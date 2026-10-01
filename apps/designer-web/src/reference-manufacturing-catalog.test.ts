import { describe, expect, it } from "vitest";
import type { ProductionIdentityContext } from "@doormes/manufacturing-model";
import { toDesignObjectId } from "@doormes/domain";
import { REFERENCE_SIMULATION_NUMBER_POLICY } from "./reference-manufacturing-catalog";

function context(productionInstanceId: string): ProductionIdentityContext {
  return {
    productionInstanceId,
    sourceFeatureId: "ASSEMBLY:CORNERS:connector",
    sourceObjectIds: [toDesignObjectId("ASSEMBLY"), toDesignObjectId("JOINT")],
    sourceWindowId: toDesignObjectId("WINDOW-1"),
    sourceMark: "A1",
    sourceComponentId: "joint.connector",
    catalogIdentity: {
      catalogItemId: "REFERENCE",
      catalogVersion: "1.0.0",
      modelCode: "JNT-CORNER",
      source: "catalog"
    },
    materialCode: "JNT-CORNER",
    assemblyPath: [],
    positionCode: "INSTALLATION-RIGHT",
    trackingMode: "piece",
    sequence: 1
  };
}

describe("REFERENCE_SIMULATION_NUMBER_POLICY", () => {
  it("keeps equal-position corner-joint instances unique and deterministic", () => {
    const first = REFERENCE_SIMULATION_NUMBER_POLICY.createNumber(context("PI-LOCAL-CORNER-A"));
    const second = REFERENCE_SIMULATION_NUMBER_POLICY.createNumber(context("PI-LOCAL-CORNER-B"));

    expect(first.productionNumber).not.toBe(second.productionNumber);
    expect(REFERENCE_SIMULATION_NUMBER_POLICY.createNumber(
      context("PI-LOCAL-CORNER-A")
    ).productionNumber).toBe(first.productionNumber);
    expect(first.productionNumber).toMatch(/^A1-IN-R-/);
  });
});
