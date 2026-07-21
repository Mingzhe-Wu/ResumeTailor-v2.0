const DEFAULT_BACKEND_BASE_URL = "http://localhost:8080";
const DEFAULT_JOB_SOURCE = "linkedin";
const MIN_VISIBLE_TEXT_LENGTH = 200;

const backendBaseUrlInput = document.getElementById("backendBaseUrl");
const loginEmailInput = document.getElementById("loginEmail");
const loginPasswordInput = document.getElementById("loginPassword");
const loginButton = document.getElementById("loginButton");
const logoutButton = document.getElementById("logoutButton");
const jwtTokenDisplay = document.getElementById("jwtTokenDisplay");
const loginStatusBadge = document.getElementById("loginStatusBadge");
const importButton = document.getElementById("importButton");
const copyJdButton = document.getElementById("copyJdButton");
const jobSourceSelect = document.getElementById("jobSource");
const defaultJobStatusSelect = document.getElementById("defaultJobStatus");
const statusMessage = document.getElementById("statusMessage");
const previewTitle = document.getElementById("previewTitle");
const previewCompany = document.getElementById("previewCompany");
const previewLength = document.getElementById("previewLength");

document.addEventListener("DOMContentLoaded", restoreSettings);
backendBaseUrlInput.addEventListener("change", saveSettings);
loginEmailInput.addEventListener("change", saveSettings);
loginButton.addEventListener("click", loginToResumeTailor);
logoutButton.addEventListener("click", clearExtensionToken);
importButton.addEventListener("click", importCurrentJob);
copyJdButton.addEventListener("click", copyCurrentJobDescription);
jobSourceSelect.addEventListener("change", handleJobSourceChange);
defaultJobStatusSelect.addEventListener("change", saveSettings);

async function restoreSettings() {
  const settings = await chrome.storage.local.get([
    "backendBaseUrl",
    "loginEmail",
    "jwtToken",
    "defaultJobStatus",
    "jobSource",
  ]);
  backendBaseUrlInput.value = settings.backendBaseUrl || DEFAULT_BACKEND_BASE_URL;
  loginEmailInput.value = settings.loginEmail || "";
  defaultJobStatusSelect.value = settings.defaultJobStatus || "1";
  jobSourceSelect.value = normalizeJobSource(settings.jobSource);
  updateTokenDisplay(settings.jwtToken || "");
}

async function saveSettings() {
  await chrome.storage.local.set({
    backendBaseUrl: normalizeBackendBaseUrl(backendBaseUrlInput.value),
    loginEmail: loginEmailInput.value.trim(),
    defaultJobStatus: defaultJobStatusSelect.value,
    jobSource: normalizeJobSource(jobSourceSelect.value),
  });
}

async function handleJobSourceChange() {
  await saveSettings();

  if (jobSourceSelect.value === "indeed") {
    setStatus("Indeed extraction is selected.", "info");
  } else if (jobSourceSelect.value === "handshake") {
    setStatus("Handshake extraction is selected.", "info");
  } else {
    setStatus("LinkedIn extraction is selected.", "info");
  }
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

async function copyCurrentJobDescription() {
  setStatus("Extracting job description...", "info");
  setCopyButtonLoading(true);

  try {
    const [tab] = await chrome.tabs.query({ active: true, currentWindow: true });
    if (!tab?.id) {
      throw new Error("No active tab found.");
    }

    const source = normalizeJobSource(jobSourceSelect.value);
    const extracted = await extractVisiblePageContent(tab.id);
    updatePreview(extracted);

    if (extracted.description.length < MIN_VISIBLE_TEXT_LENGTH) {
      setStatus(
        "The visible page text is too short to copy. Open the full job posting and try again.",
        "warning"
      );
      return;
    }

    await copyJobDescriptionToClipboard(extracted.description);
    setStatus(
      source === "indeed"
        ? `Indeed job description copied (${extracted.description.length.toLocaleString()} characters).`
        : source === "handshake"
          ? `Handshake job description copied (${extracted.description.length.toLocaleString()} characters).`
        : "Job description copied to clipboard.",
      "success"
    );
  } catch (error) {
    setStatus(getReadableErrorMessage(error), "error");
  } finally {
    setCopyButtonLoading(false);
  }
}

async function copyJobDescriptionToClipboard(description) {
  await navigator.clipboard.writeText(description);
}

async function extractFullVisiblePageText(tabId) {
  const [result] = await chrome.scripting.executeScript({
    target: { tabId },
    func: () => document.body?.innerText || "",
  });

  const description = normalizeVisibleText(result?.result);
  if (!description) {
    throw new Error("Could not read the active page.");
  }

  return { description };
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

  if (normalizeJobSource(jobSourceSelect.value) === "indeed") {
    const details = parseIndeedJobPostDetails(extractIndeedJobPostDetails(page.description));
    return {
      title: details.title,
      company: details.company,
      description: details.description,
      sourceUrl: normalizeIndeedSourceUrl(page.sourceUrl),
    };
  }

  if (normalizeJobSource(jobSourceSelect.value) === "handshake") {
    const details = parseHandshakePage(page.description);
    return {
      ...details,
      sourceUrl: normalizeHandshakeSourceUrl(page.sourceUrl),
    };
  }

  return {
    ...parseLinkedInTitle(page.title),
    sourceUrl: normalizeSourceUrl(page.sourceUrl),
    description: normalizeVisibleText(extractJobDescriptionFromVisibleText(page.description)),
  };
}

function normalizeJobSource(value) {
  if (value === "indeed" || value === "handshake") {
    return value;
  }

  return DEFAULT_JOB_SOURCE;
}

function extractIndeedJobPostDetails(value) {
  const text = String(value || "");
  const startIndex = findSectionMarkerIndex(text, "Job Post Details");
  if (startIndex < 0) {
    throw new Error("Could not find the Indeed 'Job Post Details' section on this page.");
  }

  const detailsText = text.slice(startIndex);
  const exploreOtherJobsIndex = findSectionMarkerIndex(detailsText, "Explore other jobs");
  const reportJobIndex = findSectionMarkerIndex(detailsText, "Report job");
  const endIndex = exploreOtherJobsIndex >= 0
    ? exploreOtherJobsIndex
    : reportJobIndex >= 0
      ? reportJobIndex
      : detailsText.length;

  return detailsText.slice(0, endIndex);
}

function parseIndeedJobPostDetails(value) {
  const lines = String(value || "")
    .replace(/\r/g, "")
    .split("\n")
    .map((line) => line.replace(/\u00a0/g, " ").trim())
    .filter((line) => line && line.toLowerCase() !== "&nbsp;");

  const detailsIndex = findExactLineIndex(lines, "Job Post Details");
  const jobPostIndex = findExactLineIndex(lines, "- job post", detailsIndex + 1);
  const descriptionIndex = findExactLineIndex(lines, "Full job description", jobPostIndex + 1);

  if (detailsIndex < 0 || jobPostIndex < 0 || descriptionIndex < 0) {
    throw new Error("Could not parse the Indeed job headings from the visible page.");
  }

  const title = lines[detailsIndex + 1] || "";
  const company = lines[jobPostIndex + 1] || "";
  const location = lines[jobPostIndex + 2] || "";
  const salary = normalizeIndeedSalary(lines[jobPostIndex + 3] || "");
  const description = normalizeVisibleText(lines.slice(descriptionIndex + 1).join("\n"));

  if (!title || !company || !location || !description) {
    throw new Error("The Indeed job page is missing a title, company, location, or full job description.");
  }

  return { title, company, location, salary, description };
}

function findExactLineIndex(lines, marker, startIndex = 0) {
  const normalizedMarker = marker.toLowerCase();
  return lines.findIndex(
    (line, index) => index >= startIndex && line.toLowerCase() === normalizedMarker
  );
}

function normalizeIndeedSalary(value) {
  return String(value || "")
    .replace(
      /\s+-\s+(?:full-time|part-time|contract|temporary|internship|per diem|seasonal)(?:\s*,.*)?$/i,
      ""
    )
    .trim();
}

function parseHandshakePage(value) {
  const lines = String(value || "")
    .replace(/\r/g, "")
    .split("\n")
    .map((line) => line.replace(/\u00a0/g, " ").trim())
    .filter(Boolean);

  const descriptionIndex = findExactLineIndex(lines, "Job description");
  const qualificationsIndex = findExactLineIndex(
    lines,
    "What they're looking for",
    descriptionIndex + 1
  );
  const aboutEmployerIndex = findExactLineIndex(
    lines,
    "About the employer",
    qualificationsIndex + 1
  );
  let postedIndex = -1;
  for (let index = descriptionIndex - 1; index >= 0; index -= 1) {
    if (/^Posted\b/i.test(lines[index])) {
      postedIndex = index;
      break;
    }
  }

  if (
    descriptionIndex < 0 ||
    qualificationsIndex < 0 ||
    aboutEmployerIndex < 0 ||
    postedIndex < 1
  ) {
    throw new Error("Could not parse the Handshake job headings from the visible page.");
  }

  const title = lines[postedIndex - 1] || "";
  const company = lines[aboutEmployerIndex + 1] || "";
  const description = normalizeVisibleText(
    lines.slice(descriptionIndex + 1, qualificationsIndex).join("\n")
  );

  if (!title || !company || !description) {
    throw new Error("The Handshake job page is missing a title, company, or job description.");
  }

  return { title, company, description };
}

function extractJobDescriptionFromVisibleText(value) {
  const text = String(value || "");
  const markerMatch = text.match(/(^|\n)\s*About the job\s*(\n|$)/i);

  if (!markerMatch || markerMatch.index === undefined) {
    return text;
  }

  const descriptionText = text.slice(markerMatch.index);
  const premiumMarkerIndex = findSectionMarkerIndex(
    descriptionText,
    "Job search faster with Premium"
  );
  if (premiumMarkerIndex >= 0) {
    return descriptionText.slice(0, premiumMarkerIndex);
  }

  const companyMarkerIndex = findSectionMarkerIndex(descriptionText, "About the company");
  return companyMarkerIndex >= 0
    ? descriptionText.slice(0, companyMarkerIndex)
    : descriptionText;
}

function findSectionMarkerIndex(text, marker) {
  const escapedMarker = marker.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
  const match = text.match(new RegExp(`(^|\\n)\\s*${escapedMarker}\\s*(\\n|$)`, "i"));

  return match?.index ?? -1;
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
      ...(extracted.location ? { location: extracted.location } : {}),
      ...(extracted.salary ? { salary: extracted.salary } : {}),
      ...(extracted.sourceUrl ? { sourceUrl: extracted.sourceUrl } : {}),
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

function normalizeIndeedSourceUrl(value) {
  try {
    const url = new URL(String(value || "").trim());
    const jobKey = (url.searchParams.get("vjk") || url.searchParams.get("jk") || "").trim();

    if (!/^[a-zA-Z0-9]+$/.test(jobKey)) {
      return "";
    }

    return `https://www.indeed.com/viewjob?jk=${encodeURIComponent(jobKey)}`;
  } catch {
    return "";
  }
}

function normalizeHandshakeSourceUrl(value) {
  try {
    const url = new URL(String(value || "").trim());
    const jobIdMatch = url.pathname.match(/^\/(?:job-search|jobs|public\/jobs)\/(\d+)(?:\/|$)/i);
    const jobId = jobIdMatch?.[1] || "";

    if (!jobId) {
      return "";
    }

    return `https://app.joinhandshake.com/public/jobs/${jobId}?utm_source=web&utm_campaign=job_share&utm_medium=copy_link&utm_content=stu-copy_link-job_page`;
  } catch {
    return "";
  }
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
  previewCompany.textContent = extracted.company || "Unknown";
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

function setCopyButtonLoading(isLoading) {
  copyJdButton.disabled = isLoading;
  copyJdButton.textContent = isLoading
    ? "Copying..."
    : "Copy JD to Clipboard";
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
    const source = normalizeJobSource(jobSourceSelect.value);
    const sourceName = source === "indeed"
      ? "Indeed"
      : source === "handshake"
        ? "Handshake"
        : "LinkedIn";
    return `Chrome blocked access to this page. Reload the extension, then try again on a ${sourceName} job page.`;
  }

  return error.message;
}
