import { readFileSync } from "node:fs";
import { resolve } from "node:path";
import { describe, expect, it } from "vitest";

const FORBIDDEN_SHELL_IMPORTS = [
  "@doormes/domain",
  "@doormes/geometry",
  "@doormes/geometry-topology",
  "@doormes/manufacturing-model",
  "@doormes/rule-engine",
  "@doormes/calculation-engine",
  "@doormes/renderer-svg",
  "@doormes/renderer-three",
  "three"
];

/**
 * Reads a repository source file for architecture-boundary assertions.
 *
 * The test intentionally inspects imports rather than runtime behaviour: an
 * illegal shell dependency should fail immediately even when unused in a test
 * scenario. A future lint rule may replace this helper without changing the
 * dependency policy.
 *
 * @param relativePath Repository-relative TypeScript source path.
 * @returns UTF-8 source contents.
 * @example `readSource("packages/layout-shell-mobile/src/index.ts")`.
 * @since 0.1.0
 * @modified 2026-09-17 - Added executable shell-boundary enforcement.
 */
function readSource(relativePath: string): string {
  return readFileSync(resolve(process.cwd(), relativePath), "utf8");
}

describe("layout shell dependency boundaries", () => {
  for (const shellPath of [
    "packages/layout-shell-desktop/src/index.ts",
    "packages/layout-shell-mobile/src/index.ts"
  ]) {
    it(`${shellPath} depends only on application, interaction and shared UI layers`, () => {
      const source = readSource(shellPath);
      for (const forbiddenImport of FORBIDDEN_SHELL_IMPORTS) {
        expect(source).not.toContain(`from "${forbiddenImport}"`);
      }
    });
  }
});
