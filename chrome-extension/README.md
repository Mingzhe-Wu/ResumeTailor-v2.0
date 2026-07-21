# ResumeTailor Job Importer

This is a lightweight Manifest V3 Chrome extension MVP for importing the currently visible job posting page into ResumeTailor.

The extension uses plain HTML, CSS, and JavaScript. There is no React, Vite, TypeScript, or build step.

## Load the Extension Locally

1. Open Chrome.
2. Go to `chrome://extensions`.
3. Enable **Developer Mode**.
4. Click **Load unpacked**.
5. Select the `chrome-extension/` directory.

## Open the Side Panel

Click the **ResumeTailor Job Importer** extension icon to open the Chrome Side Panel.

Unlike a normal extension popup, the side panel stays open while you click and scroll the job posting page.

## Configure Local Backend

In the extension side panel, set:

```text
Backend Base URL: http://localhost:8080
```

The side panel stores this value in Chrome local extension storage.

## Extension Login

The extension logs in separately from the ResumeTailor web app.

To authenticate the extension:

1. Open the extension side panel.
2. Enter your ResumeTailor email and password.
3. Click **Login to ResumeTailor**.

The extension calls:

```text
POST http://localhost:8080/api/auth/login
```

It stores the returned JWT in Chrome local extension storage and sends imports as:

```text
Authorization: Bearer <token>
```

The side panel only displays a masked token preview. Manual token entry is intentionally disabled so the full token is not exposed in the UI. This extension token is separate from the web app's localStorage token.

## Test Importing a Job

1. Start the ResumeTailor backend locally.
2. Log in from the extension side panel.
3. Open a LinkedIn, Indeed, or Handshake job posting page in Chrome that you can already view.
4. Open the extension side panel.
5. Select the matching job source.
6. Confirm the backend URL, extension login state, and default job status.
7. Click **Import Current Job**.

The extension extracts:

- `document.title`
- `window.location.href`
- job description text from the visible page

The MVP requests LinkedIn page access through `https://www.linkedin.com/*` so the side panel can read the visible job page after the extension is reloaded.

For LinkedIn pages, the importer keeps the visible text starting after the `About the job` heading. If that heading is not found, it falls back to the full visible page text.

Then it calls:

```text
POST http://localhost:8080/api/jobs/import
```

Request body:

```json
{
  "title": "Page title",
  "company": "Company name",
  "location": "Job location",
  "salary": "Salary or hourly rate",
  "sourceUrl": "https://example.com/job",
  "description": "Visible page text",
  "status": 1
}
```

Use **Copy JD to Clipboard** when you want to copy the extracted description without importing the job into ResumeTailor. LinkedIn keeps its existing job-description extraction. Indeed Copy and Import use the same structured extraction: stop before `Explore other jobs` when present, otherwise stop before `Report job`, and otherwise keep the remaining text through the end of the page.

## Current Limitations

- LinkedIn import and copy use the existing LinkedIn extraction behavior.
- Indeed parsing requires the visible headings `Job Post Details`, `- job post`, and `Full job description`. The optional end markers are `Explore other jobs` and `Report job`.
- Indeed imports currently omit location and salary because those summary lines are not consistently present across postings.
- Handshake Copy and Import extract the title immediately before `Posted`, the company immediately after `About the employer`, and the JD between `Job description` and `What they're looking for`.
- Handshake `/job-search/{id}` URLs are converted to the public `/public/jobs/{id}` sharing URL without carrying search filters into the imported job.
- Indeed imports convert the current page's `vjk` (or `jk`) parameter into a canonical `https://www.indeed.com/viewjob?jk=...` source URL. Other onboarding and tracking parameters are discarded.
- Generic visible text extraction only.
- No AI parsing yet.
- LinkedIn search-result URLs are normalized to canonical `/jobs/view/{jobId}` URLs when `currentJobId` is available.
- LinkedIn page titles in the form `Position | Company | LinkedIn` are split into title and company.
- No deeper LinkedIn-specific or job-board-specific selectors yet.
- Does not bypass login walls, hidden content, anti-bot behavior, or unavailable page text.
- The import uses the current visible active tab only and must be triggered by the user.
- Company falls back to `Unknown` when it cannot be parsed from the page title.
- The extension stores a separate local JWT for MVP development.
