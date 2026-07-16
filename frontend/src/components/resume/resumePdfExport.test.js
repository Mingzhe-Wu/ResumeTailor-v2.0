import test from "node:test";
import assert from "node:assert/strict";
import { isResumeElementOutOfBoundary } from "./resumePdfExport.js";

test("uses the preview's one-pixel overflow tolerance for native print", () => {
  assert.equal(
    isResumeElementOutOfBoundary({ scrollHeight: 1009, clientHeight: 1008 }),
    false
  );
  assert.equal(
    isResumeElementOutOfBoundary({ scrollHeight: 1010, clientHeight: 1008 }),
    true
  );
});
