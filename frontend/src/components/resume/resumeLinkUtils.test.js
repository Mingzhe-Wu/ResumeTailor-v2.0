import test from "node:test";
import assert from "node:assert/strict";
import { getContactHref, getProjectHref } from "./resumeLinkUtils.js";

test("builds printable contact href values without changing display text", () => {
  assert.equal(getContactHref("email", "person@example.com"), "mailto:person@example.com");
  assert.equal(getContactHref("phone", "+1 (555) 123-4567"), "tel:+15551234567");
  assert.equal(
    getContactHref("linkedin", "linkedin.com/in/person"),
    "https://linkedin.com/in/person"
  );
  assert.equal(getContactHref("additional-info-0", "Seattle, WA"), undefined);
  assert.equal(
    getContactHref("additional-info-0", "portfolio.example.com"),
    "https://portfolio.example.com"
  );
  assert.equal(
    getContactHref("additional-info-0", "https://portfolio.example.com"),
    "https://portfolio.example.com"
  );
});

test("uses an existing project or demo URL for the rendered project name", () => {
  assert.equal(
    getProjectHref({ projectUrl: "example.com/project" }),
    "https://example.com/project"
  );
  assert.equal(
    getProjectHref({ demo_url: "https://demo.example.com" }),
    "https://demo.example.com"
  );
  assert.equal(getProjectHref({ name: "No link" }), undefined);
});
