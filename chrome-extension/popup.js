const DEFAULT_BACKEND_BASE_URL = "http://localhost:8080";
const MIN_VISIBLE_TEXT_LENGTH = 200;

const backendBaseUrlInput = document.getElementById("backendBaseUrl");
const loginEmailInput = document.getElementById("loginEmail");
const loginPasswordInput = document.getElementById("loginPassword");
const loginButton = document.getElementById("loginButton");
const logoutButton = document.getElementById("logoutButton");
const jwtTokenDisplay = document.getElementById("jwtTokenDisplay");
const loginStatusBadge = document.getElementById("loginStatusBadge");
const importButton = document.getElementById("importButton");
const defaultJobStatusSelect = document.getElementById("defaultJobStatus");
const statusMessage = document.getElementById("statusMessage");
const previewTitle = document.getElementById("previewTitle");
const previewUrl = document.getElementById("previewUrl");
const previewLength = document.getElementById("previewLength");

document.addEventListener("DOMContentLoaded", restoreSettings);
backendBaseUrlInput.addEventListener("change", saveSettings);
loginEmailInput.addEventListener("change", saveSettings);
loginButton.addEventListener("click", loginToResumeTailor);
logoutButton.addEventListener("click", clearExtensionToken);
importButton.addEventListener("click", importCurrentJob);
defaultJobStatusSelect.addEventListener("change", saveSettings);

async function restoreSettings() {
  const settings = await chrome.storage.local.get(["backendBaseUrl", "loginEmail", "jwtToken", "defaultJobStatus"]);
  backendBaseUrlInput.value = settings.backendBaseUrl || DEFAULT_BACKEND_BASE_URL;
  loginEmailInput.value = settings.loginEmail || "";
  defaultJobStatusSelect.value = settings.defaultJobStatus || "1";
  updateTokenDisplay(settings.jwtToken || "");
}

async function saveSettings() {
  await chrome.storage.local.set({
    backendBaseUrl: normalizeBackendBaseUrl(backendBaseUrlInput.value),
    loginEmail: loginEmailInput.value.trim(),
    defaultJobStatus: defaultJobStatusSelect.value,
  });
}

async function loginToResumeTailor() {
  setStatus("Logging in to ResumeTailor...", "info");
  setLoginButtonLoading(true);

  try {
    await saveSettings();
    const response = await fetch(`${normalizeBackendBaseUrl(backendBaseUrlInput.value)}/api/auth/login`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "Accept": "application/json",
      },
      body: JSON.stringify({
        email: loginEmailInput.value.trim(),
        password: loginPasswordInput.value,
      }),
    });

    if (!response.ok) {
      const message = await readErrorMessage(response);
      throw new Error(message || `Login failed with status ${response.status}.`);
    }

    const data = await response.json();
    const token = normalizeJwtToken(data.token || "");
    if (!token) {
      throw new Error("Login succeeded but no token was returned.");
    }

    await chrome.storage.local.set({
      backendBaseUrl: normalizeBackendBaseUrl(backendBaseUrlInput.value),
      loginEmail: loginEmailInput.value.trim(),
      jwtToken: token,
    });
    loginPasswordInput.value = "";
    updateTokenDisplay(token);
    setStatus("Extension login successful.", "success");
  } catch (error) {
    setStatus(getReadableErrorMessage(error), "error");
  } finally {
    setLoginButtonLoading(false);
  }
}

async function clearExtensionToken() {
  await chrome.storage.local.remove("jwtToken");
  updateTokenDisplay("");
  setStatus("Extension token cleared.", "info");
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
  const settings = await chrome.storage.local.get(["jwtToken"]);
  const token = normalizeJwtToken(settings.jwtToken || "");
  if (!token) {
    throw new Error("Please login to ResumeTailor in the extension first.");
  }

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
      status: Number(defaultJobStatusSelect.value),
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

function maskToken(value) {
  const token = normalizeJwtToken(value);
  if (!token) return "";

  if (token.length <= 16) {
    return `${token.slice(0, 4)}......${token.slice(-4)}`;
  }

  return `${token.slice(0, 10)}......${token.slice(-8)}`;
}

function updateTokenDisplay(value) {
  const maskedToken = maskToken(value);
  jwtTokenDisplay.textContent = maskedToken || "No token loaded";
  jwtTokenDisplay.classList.toggle("has-token", Boolean(maskedToken));
  loginStatusBadge.textContent = maskedToken ? "Logged in" : "Not logged in";
  loginStatusBadge.classList.toggle("logged-in", Boolean(maskedToken));
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

function setLoginButtonLoading(isLoading) {
  loginButton.disabled = isLoading;
  loginButton.textContent = isLoading
    ? "Logging in..."
    : "Login to ResumeTailor";
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
