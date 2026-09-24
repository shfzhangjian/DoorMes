import { describe, expect, it } from "vitest";
import {
  BUNDLED_ENGINEERING_JOINT_CATALOG,
  findEngineeringJointCatalogSelection,
  listEngineeringJointCatalogSelections,
  requireEngineeringJointCatalogSelection
} from "./index";

describe("engineering joint business catalog", () => {
  it("publishes versioned direction-compatible reference models", () => {
    expect(listEngineeringJointCatalogSelections("left-right").map((item) => item.jointType))
      .toEqual(["mullion_joint", "reinforced_mullion"]);
    expect(listEngineeringJointCatalogSelections("top-bottom").map((item) => item.jointType))
      .toEqual(["stacking_joint"]);
    expect(listEngineeringJointCatalogSelections("corner")).toEqual([
      expect.objectContaining({
        catalogItemId: "DM-JOINT-CORNER-70",
        jointType: "corner_joint",
        cornerCapability: expect.objectContaining({
          defaultIncludedAngleDeg: 90,
          allowedTurnDirections: ["clockwise", "counterclockwise"]
        })
      })
    ]);
    expect(new Set(BUNDLED_ENGINEERING_JOINT_CATALOG.map(
      (item) => `${item.catalogItemId}@${item.catalogVersion}`
    )).size).toBe(BUNDLED_ENGINEERING_JOINT_CATALOG.length);
  });

  it("returns detached exact-version snapshots and maps legacy geometry", () => {
    const selected = requireEngineeringJointCatalogSelection(
      "DM-JOINT-RMUL-40",
      "2026.09-r1"
    );
    expect(selected).toMatchObject({
      jointType: "reinforced_mullion",
      finishedWidthMm: 40,
      manufacturingRuleId: "JOINT-MULLION-REINFORCED"
    });
    expect(findEngineeringJointCatalogSelection({
      jointType: "mullion_joint",
      finishedWidthMm: 30
    })?.catalogItemId).toBe("DM-JOINT-MUL-30");
    expect(() => requireEngineeringJointCatalogSelection("MISSING"))
      .toThrow(/does not exist/);
  });
});
