import { defineConfig } from "vitest/config";

/**
 * Defines the single test entry point for every workspace package.
 *
 * The root configuration deliberately runs tests from shared packages and
 * architecture tests together so a shell change cannot bypass core boundary
 * checks by using a package-local test command.
 *
 * @example Run all tests with `npm test` from the repository root.
 * @since 0.1.0
 * @modified 2026-09-21 - Included composition-root control tests beside shared packages.
 */
export default defineConfig({
  test: {
    environment: "node",
    include: ["apps/**/*.test.ts", "packages/**/*.test.ts", "tests/**/*.test.ts"],
    coverage: {
      reporter: ["text", "html"]
    }
  }
});
