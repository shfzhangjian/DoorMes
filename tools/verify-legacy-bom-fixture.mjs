import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import { fileURLToPath } from "node:url";
import { calculateProjectBom } from "../../luck_door/dist/assets/calculation.js";

const FIXTURE_DIRECTORY = fileURLToPath(
  new URL("../tests/fixtures/legacy-v2/", import.meta.url)
);

/**
 * Reads a UTF-8 JSON fixture from the committed legacy baseline directory.
 *
 * @param name Fixture filename without directory traversal.
 * @returns Parsed JSON value.
 * @example `readFixture("rectangular-fixed.input.json")`.
 * @since 0.1.0
 * @modified 2026-09-17 - Added fixture-loading support for legacy verification.
 */
async function readFixture(name) {
  return JSON.parse(await readFile(`${FIXTURE_DIRECTORY}/${name}`, "utf8"));
}

/**
 * Normalizes only the prototype-generated timestamp before exact comparison.
 *
 * @param value Raw legacy BOM result.
 * @returns Deterministic result retaining all business values.
 * @example Two verification runs compare equal despite different wall clocks.
 * @since 0.1.0
 * @modified 2026-09-17 - Added deterministic legacy verification.
 */
function normalizeLegacyBom(value) {
  const clone = structuredClone(value);
  if (clone?.mbom) clone.mbom.generatedAt = "<generated-at>";
  return clone;
}

/**
 * Replays the frozen prototype algorithm and compares every result field with
 * the committed expectation.
 *
 * This command depends on the sibling `luck_door` baseline by design and is a
 * migration-time audit, not a production runtime dependency.
 *
 * @returns A promise completed when the parity check passes.
 * @example Run `npm run verify:legacy-fixtures` from the formal repository.
 * @since 0.1.0
 * @modified 2026-09-17 - Added first real legacy-algorithm replay.
 */
async function main() {
  for (const name of [
    "rectangular-fixed",
    "two-column-fixed",
    "local-mullions-fixed",
    "tee-mullions-fixed",
    "single-tilt-turn",
    "double-tilt-turn-flying-mullion",
    "top-hung"
  ]) {
    const input = await readFixture(`${name}.input.json`);
    const expected = await readFixture(`${name}.legacy-bom.json`);
    const actual = normalizeLegacyBom(calculateProjectBom(structuredClone(input)));
    assert.deepEqual(actual, expected, `The frozen ${name} BOM baseline changed.`);
  }
  console.log(
    "Legacy fixed-window, topology, tilt-turn and top-hung BOM fixtures match the frozen prototype engine."
  );
}

await main();
