import { describe, expect, it } from "vitest";
import {
  createZcsungTemplateSelectionSnapshot,
  findZcsungProductTemplate,
  listZcsungProductTemplates,
  requireZcsungSimulationPreset
} from "./index";

describe("ZCSUNG public-reference product template catalogue", () => {
  it("keeps every public reference non-production and traceable to an official source", () => {
    const templates = listZcsungProductTemplates();
    const identities = templates.map((template) => `${template.templateId}@${template.templateVersion}`);

    expect(new Set(identities).size).toBe(identities.length);
    expect(templates.length).toBeGreaterThanOrEqual(8);
    for (const template of templates) {
      expect(template.customerId).toBe("ZCSUNG");
      expect(template.sourceStatus).toBe("public-reference");
      expect(template.engineeringStatus).toBe("simulated-not-for-production");
      expect(template.productionReady).toBe(false);
      expect(template.officialSourceUrls.length).toBeGreaterThan(0);
      expect(template.officialSourceUrls.every((url) => url.startsWith("https://"))).toBe(true);
      expect(template.assumptions.length).toBeGreaterThan(0);
    }
  });

  it("provides executable neutral presets only for represented shared motions", () => {
    const resolved = requireZcsungSimulationPreset("ZCSUNG-SIM-LH-120-TT");
    const selection = createZcsungTemplateSelectionSnapshot(resolved.template);

    expect(resolved.template.publicProductNames).toContain("120内开内倒系统窗");
    expect(resolved.template.templateVersion).toBe("0.1.1");
    expect(resolved.preset.opening).toBe("right_in");
    expect(resolved.preset.maximumAngleDegreesByMode).toEqual({ primary: 90, tilt: 15 });
    expect(resolved.preset.neutralGeometry).toMatchObject({
      profileSystemId: "AL70",
      hardwareSetId: "HW-TT-STD"
    });
    expect(selection).toMatchObject({
      schemaVersion: "doormes-product-template-selection.v1",
      templateId: "ZCSUNG-SIM-LH-120-TT",
      publicProductName: "120内开内倒系统窗",
      productionReady: false
    });
  });

  it("keeps explicit obsolete versions fail-closed after catalogue corrections", () => {
    expect(findZcsungProductTemplate("ZCSUNG-SIM-LH-120-TT", "0.1.0")).toBeUndefined();
    expect(() => requireZcsungSimulationPreset(
      "ZCSUNG-SIM-LH-120-TT",
      "0.1.0"
    )).toThrow("Unknown ZCSUNG product template ZCSUNG-SIM-LH-120-TT@0.1.0");
  });

  it("fails closed instead of drawing planned mechanisms as an ordinary casement", () => {
    expect(findZcsungProductTemplate("ZCSUNG-PLAN-SLIDING")?.capabilityStatus).toBe("planned");
    expect(() => requireZcsungSimulationPreset("ZCSUNG-PLAN-SLIDING")).toThrow(
      "has no executable simulation preset"
    );
  });
});
