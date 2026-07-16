const WEB_URL_PATTERN = /^(?:https?:\/\/|www\.)/i;
const BARE_WEB_URL_PATTERN = /^(?:[\w-]+\.)+[a-z]{2,}(?:[/:?#].*)?$/i;
const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export function getContactHref(field, value) {
  const text = String(value || "").trim();
  if (!text) return undefined;

  if (field === "email" || EMAIL_PATTERN.test(text)) {
    return `mailto:${text.replace(/^mailto:/i, "")}`;
  }

  if (field === "phone") {
    const phone = text.replace(/[^\d+]/g, "");
    return phone ? `tel:${phone}` : undefined;
  }

  if (
    ["linkedin", "github"].includes(field) ||
    WEB_URL_PATTERN.test(text) ||
    BARE_WEB_URL_PATTERN.test(text)
  ) {
    return normalizeWebHref(text);
  }

  return undefined;
}

export function getProjectHref(item) {
  const value = [
    item?.url,
    item?.projectUrl,
    item?.project_url,
    item?.demoUrl,
    item?.demo_url,
    item?.githubUrl,
    item?.github_url,
    item?.link,
  ].find((candidate) => String(candidate || "").trim());

  return value ? normalizeWebHref(value) : undefined;
}

function normalizeWebHref(value) {
  const text = String(value || "").trim();
  if (!text) return undefined;
  return /^https?:\/\//i.test(text) ? text : `https://${text}`;
}
