const DEFAULT_BACKEND_BASE_URL = "http://localhost:8080";
const RESUMETAILOR_FRONTEND_URL_PATTERN = "http://localhost:5173/*";
const MIN_VISIBLE_TEXT_LENGTH = 200;

const backendBaseUrlInput = document.getElementById("backendBaseUrl");
const jwtTokenInput = document.getElementById("jwtToken");
const loadTokenButton = document.getElementById("loadTokenButton");
const importButton = document.getElementById("importButton");
const statusMessage = document.getElementById("statusMessage");
const previewTitle = document.getElementById("previewTitle");
const previewUrl = document.getElementById("previewUrl");
const previewLength = document.getElementById("previewLength");

document.addEventListener("DOMContentLoaded", restoreSettings);
backendBaseUrlInput.addEventListener("change", saveSettings);
jwtTokenInput.addEventListener("change", saveSettings);
loadTokenButton.addEventListener("click", loadTokenFromResumeTailorTab);
importButton.addEventListener("click", importCurrentJob);

async function restoreSettings() {
  const settings = await chrome.storage.local.get(["backendBaseUrl", "jwtToken"]);
  backendBaseUrlInput.value = settings.backendBaseUrl || DEFAULT_BACKEND_BASE_URL;
  jwtTokenInput.value = normalizeJwtToken(settings.jwtToken || "");
}

async function saveSettings() {
  await chrome.storage.local.set({
    backendBaseUrl: normalizeBackendBaseUrl(backendBaseUrlInput.value),
    jwtToken: normalizeJwtToken(jwtTokenInput.value),
  });
}

async function loadTokenFromResumeTailorTab() {
  setStatus("Looking for an open ResumeTailor tab...", "info");
  setLoadTokenButtonLoading(true);

  try {
    const [resumeTailorTab] = await chrome.tabs.query({
      url: RESUMETAILOR_FRONTEND_URL_PATTERN,
    });

    if (!resumeTailorTab?.id) {
      throw new Error("Open ResumeTailor at http://localhost:5173 first, then try again.");
    }

    const [result] = await chrome.scripting.executeScript({
      target: { tabId: resumeTailorTab.id },
      func: () => localStorage.getItem("token") || "",
    });

    const token = normalizeJwtToken(result?.result || "");
    if (!token) {
      throw new Error("No token found in the ResumeTailor tab. Please log in first.");
    }

    jwtTokenInput.value = token;
    await saveSettings();
    setStatus("Token loaded from ResumeTailor tab.", "success");
  } catch (error) {
    setStatus(getReadableErrorMessage(error), "error");
  } finally {
    setLoadTokenButtonLoading(false);
  }
}

async function importCurrentJob() {
  setStatus("Extracting current page...", "info");
  setButtonLoading(true);

  try {
    await saveSettings();
    const [tab] = await chrome.tabs.query({ active: true, currentWindow: true });
    if (!tab?.id) {
      throw new Error("No active tab found.");
    }

    const extracted = await extractVisiblePageContent(tab.id);
    updatePreview(extracted);

    if (extracted.description.length < MIN_VISIBLE_TEXT_LENGTH) {
      setStatus(
        "The visible page text is too short to import. Open the full job posting and try again.",
        "warning"
      );
      return;
    }

    setStatus("Sending job to ResumeTailor...", "info");
    const importedJob = await sendImportRequest(extracted);
    setStatus(`Imported "${importedJob.title || extracted.title}" successfully.`, "success");
  } catch (error) {
    setStatus(getReadableErrorMessage(error), "error");
  } finally {
    setButtonLoading(false);
  }
}

async function extractVisiblePageContent(tabId) {
  const [result] = await chrome.scripting.executeScript({
    target: { tabId },
    func: () => ({
      title: document.title || "",
      sourceUrl: window.location.href || "",
      description: document.body?.innerText || "",
    }),
  });

  const page = result?.result;
  if (!page) {
    throw new Error("Could not read the active page.");
  }

  return {
    ...parseLinkedInTitle(page.title),
    sourceUrl: normalizeSourceUrl(page.sourceUrl),
    description: normalizeVisibleText(extractJobDescriptionFromVisibleText(page.description)),
  };
}

function extractJobDescriptionFromVisibleText(value) {
  const text = String(value || "");
  const markerMatch = text.match(/(^|\n)\s*About the job\s*(\n|$)/i);

  if (!markerMatch || markerMatch.index === undefined) {
    return text;
  }

  return text.slice(markerMatch.index);
}

async function sendImportRequest(extracted) {
  const backendBaseUrl = normalizeBackendBaseUrl(backendBaseUrlInput.value);
  const token = normalizeJwtToken(jwtTokenInput.value);
  const headers = {
    "Content-Type": "application/json",
    "Accept": "application/json",
  };

  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  const response = await fetch(`${backendBaseUrl}/api/jobs/import`, {
    method: "POST",
    headers,
    body: JSON.stringify({
      title: extracted.title,
      company: extracted.company,
      sourceUrl: extracted.sourceUrl,
      description: extracted.description,
    }),
  });

  if (!response.ok) {
    const message = await readErrorMessage(response);
    throw new Error(message || `Import failed with status ${response.status}.`);
  }

  return response.json();
}

function normalizeBackendBaseUrl(value) {
  const trimmed = (value || DEFAULT_BACKEND_BASE_URL).trim();
  return trimmed.replace(/\/+$/, "");
}

function normalizeSourceUrl(value) {
  const url = String(value || "").trim();
  const match = url.match(/^(https:\/\/www\.linkedin\.com\/jobs\/search-results\/\?currentJobId=\d+)/i);

  return match ? match[1].replace("/jobs/search-results/?currentJobId=", "/jobs/view/") : url;
}

function parseLinkedInTitle(value) {
  const parts = String(value || "")
    .split("|")
    .map((part) => part.trim())
    .filter(Boolean);

  return {
    title: parts[0] || "Imported Job",
    company: parts[1] || "Unknown",
  };
}

function normalizeJwtToken(value) {
  let token = String(value || "").trim();

  if (
    (token.startsWith("\"") && token.endsWith("\"")) ||
    (token.startsWith("'") && token.endsWith("'"))
  ) {
    token = token.slice(1, -1).trim();
  }

  if (token.toLowerCase().startsWith("bearer ")) {
    token = token.slice(7).trim();
  }

  return token;
}

function normalizeVisibleText(value) {
  return String(value || "")
    .replace(/\r/g, "")
    .replace(/[ \t]+\n/g, "\n")
    .replace(/\n{3,}/g, "\n\n")
    .trim();
}

function updatePreview(extracted) {
  previewTitle.textContent = extracted.title || "Untitled page";
  previewUrl.textContent = extracted.sourceUrl || "No URL";
  previewLength.textContent = `${extracted.description.length.toLocaleString()} characters`;
}

function setStatus(message, type) {
  statusMessage.textContent = message;
  statusMessage.className = `status-message ${type || "info"}`;
}

function setButtonLoading(isLoading) {
  importButton.disabled = isLoading;
  importButton.textContent = isLoading ? "Importing..." : "Import Current Job";
}

function setLoadTokenButtonLoading(isLoading) {
  loadTokenButton.disabled = isLoading;
  loadTokenButton.textContent = isLoading
    ? "Loading Token..."
    : "Load Token from ResumeTailor Tab";
}

async function readErrorMessage(response) {
  try {
    const data = await response.json();
    return data.message || data.errorMessage || data.error || data.detail || "";
  } catch {
    return "";
  }
}

function getReadableErrorMessage(error) {
  if (!error?.message) {
    return "Import failed. Please try again.";
  }

  if (error.message.includes("Cannot access")) {
    return "Chrome blocked access to this page. Reload the extension, then try again on a LinkedIn job page.";
  }

  return error.message;
}
