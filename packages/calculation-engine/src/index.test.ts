import { describe, expect, it } from "vitest";
import {
  adaptLegacyManufacturingCatalog,
  LegacyBomCalculator,
  normalizeLegacyBomForComparison
} from "./index";

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

describe("sliding manufacturing catalog adapter", () => {
  const formula = (source: string, offsetMm = 0) => ({ source, scale: 1, offsetMm });
  const material = (materialCode: string) => ({
    materialCode,
    name: materialCode,
    specification: "factory snapshot",
    material: "aluminum",
    color: "RAL 9016"
  });
  const orientation = (source: string, quantity: number) => ({
    length: formula(source), quantity, cutLeftDeg: 45, cutRightDeg: 45
  });
  const catalog = (slidingRule: Record<string, unknown>) => ({
    profileSystems: [{
      id: "AL-SLIDE-01", name: "Sliding profile", material: "aluminum",
      frameProfile: "F", sashProfile: "S", mullionProfile: "M", beadProfile: "B",
      gasketCode: "G", faceWidthMm: 70, sashFaceWidthMm: 50, sawKerfMm: 4
    }],
    glassTypes: [{ id: "GL-01", name: "IGU", thicknessMm: 24 }],
    hardwareSets: [{
      id: "HW-SLIDE-01", name: "Sliding hardware", handleCode: "H", hingeCode: "-",
      memberName: "Sliding set", hingeQtyRule: "not-used"
    }],
    slidingRules: [slidingRule]
  });
  const validRule = () => ({
    ruleId: "SLIDE-AL-01", ruleVersion: "r1", profileSystemId: "AL-SLIDE-01",
    hardwareSetId: "HW-SLIDE-01", applicablePanelCounts: [2, 3], applicableTrackCounts: [2],
    provenance: {
      status: "reference-only", sourceType: "public-reference-simulation",
      sourceId: "simulation-brief-01", sourceRevision: "2026-09"
    },
    frame: {
      material: material("FRAME"), horizontal: orientation("window-width", 2),
      vertical: orientation("window-height", 2)
    },
    sash: {
      material: material("SASH"), horizontal: orientation("panel-width", 2),
      vertical: orientation("panel-height", 2)
    },
    rail: {
      material: material("RAIL"), horizontal: orientation("window-width", 2),
      vertical: orientation("window-height", 1)
    },
    glass: { width: formula("panel-width", -20), height: formula("panel-height", -30) },
    hardware: [{
      material: material("ROLLER"), quantityBasis: "movable-panels",
      quantityPerBasis: 2, unit: "pcs"
    }]
  });

  it("adapts versioned formulas and keeps reference-only provenance explicit", () => {
    const adapted = adaptLegacyManufacturingCatalog(catalog(validRule()));

    expect(adapted.slidingRules?.[0]).toMatchObject({
      ruleId: "SLIDE-AL-01",
      ruleVersion: "r1",
      applicablePanelCounts: [2, 3],
      provenance: { status: "reference-only", sourceType: "public-reference-simulation" },
      glass: { width: { source: "panel-width", scale: 1, offsetMm: -20 } }
    });
  });

  it("rejects unapproved provenance, duplicate versions, dangling references, and malformed formulas", () => {
    const falseApproval = validRule();
    falseApproval.provenance = {
      status: "factory-approved", sourceType: "public-reference-simulation",
      sourceId: "simulation-brief-01", sourceRevision: "2026-09"
    };
    expect(() => adaptLegacyManufacturingCatalog(catalog(falseApproval)))
      .toThrow(/cannot mark a public reference simulation as factory-approved/);

    const badFormula = validRule();
    badFormula.glass = {
      width: { source: "arbitrary-code", scale: 1, offsetMm: 0 },
      height: formula("panel-height")
    };
    expect(() => adaptLegacyManufacturingCatalog(catalog(badFormula)))
      .toThrow(/glass\/width\/source must be one of/);

    const duplicateRule = validRule();
    const duplicateCatalog = catalog(duplicateRule);
    duplicateCatalog.slidingRules.push(validRule());
    expect(() => adaptLegacyManufacturingCatalog(duplicateCatalog))
      .toThrow(/duplicates rule SLIDE-AL-01@r1/);

    const danglingReference = validRule();
    danglingReference.profileSystemId = "UNKNOWN";
    expect(() => adaptLegacyManufacturingCatalog(catalog(danglingReference)))
      .toThrow(/references an unknown profile system/);
  });
});
