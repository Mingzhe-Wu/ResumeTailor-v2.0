import assert from "node:assert/strict";
import test from "node:test";

import { sortResumeSections } from "./resumeUtils.js";

test("sortResumeSections uses the fixed resume display order", () => {
  const sections = [
    { id: "skills", type: "skills", order: 1 },
    { id: "projects", type: "projects", order: 2 },
    { id: "education", type: "education", order: 4 },
    { id: "experience", type: "experience", order: 3 },
  ];

  assert.deepEqual(
    sortResumeSections(sections).map((section) => section.id),
    ["education", "experience", "projects", "skills"]
  );
});

test("sortResumeSections recognizes section id and keeps unknown sections last", () => {
  const sections = [
    { id: "certifications", order: 2 },
    { id: "project", order: 4 },
    { id: "experience", order: 1 },
    { id: "education", order: 3 },
    { id: "awards", order: 1 },
    { id: "skill", order: 2 },
  ];

  assert.deepEqual(
    sortResumeSections(sections).map((section) => section.id),
    ["education", "experience", "project", "skill", "awards", "certifications"]
  );
});
