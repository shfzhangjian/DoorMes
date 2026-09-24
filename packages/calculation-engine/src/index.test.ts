import { describe, expect, it } from "vitest";
import { LegacyBomCalculator, normalizeLegacyBomForComparison } from "./index";

describe("LegacyBomCalculator", () => {
  it("isolates legacy input and normalizes volatile result fields", () => {
    const input = { windows: [{ windowId: "W-1" }] };
    const adapter = new LegacyBomCalculator((legacyInput) => {
      const mutable = legacyInput as { windows: Array<{ windowId: string }> };
      mutable.windows[0]!.windowId = "MUTATED";
      return { mbom: { generatedAt: "2026-09-17T01:02:03.000Z", lines: [] } };
    });
    const result = adapter.calculate(input);

    expect(input.windows[0]?.windowId).toBe("W-1");
    expect(normalizeLegacyBomForComparison(result.value)).toEqual({
      mbom: { generatedAt: "<generated-at>", lines: [] }
    });
  });
});
