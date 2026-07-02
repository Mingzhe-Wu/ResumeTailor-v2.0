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

## JWT Token for Local Testing

For the MVP, click **Load Token from ResumeTailor Tab** to load the current local ResumeTailor JWT.

To load the token automatically:

1. Open ResumeTailor at `http://localhost:5173`.
2. Log in.
3. Keep that tab open.
4. Open the extension side panel from any tab.
5. Click **Load Token from ResumeTailor Tab**.

The extension looks for an open `http://localhost:5173/*` tab and reads:

```js
localStorage.getItem("token")
```

The extension stores the loaded token in Chrome local extension storage and sends it as:

```text
Authorization: Bearer <token>
```

The side panel only displays a masked token preview. Manual token entry is intentionally disabled so the full token is not exposed in the UI. LocalStorage token loading is temporary and intended only for local development; a safer auth flow should replace it later.

## Test Importing a Job

1. Start the ResumeTailor backend locally.
2. Log in to ResumeTailor and load the JWT token from the open ResumeTailor tab.
3. Open a job posting page in Chrome, such as a LinkedIn job page that you can already view.
4. If the LinkedIn job description is collapsed, click **more** in the JD before importing.
5. Open the extension side panel.
6. Confirm the backend URL and token.
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
  "sourceUrl": "https://example.com/job",
  "description": "Visible page text"
}
```

## Current Limitations

- Generic visible text extraction only.
- No AI parsing yet.
- LinkedIn search-result URLs are normalized to canonical `/jobs/view/{jobId}` URLs when `currentJobId` is available.
- LinkedIn page titles in the form `Position | Company | LinkedIn` are split into title and company.
- No deeper LinkedIn-specific or job-board-specific selectors yet.
- Does not bypass login walls, hidden content, anti-bot behavior, or unavailable page text.
- The import uses the current visible active tab only and must be triggered by the user.
- Company falls back to `Unknown` when it cannot be parsed from the page title.
- LocalStorage JWT token loading is temporary and should be replaced by a safer authentication flow later.
