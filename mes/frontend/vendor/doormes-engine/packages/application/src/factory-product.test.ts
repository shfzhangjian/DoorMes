import { describe, expect, it } from "vitest";
import { createEmptyDesign } from "@doormes/domain";
import { REFERENCE_WINDOW_INSTALLATION } from "@doormes/geometry-topology";
import { DesignSession, createRectangularWindowCommand, createFabricationAssemblyCommand } from "./index.js";
import { describeFactoryProduct, factoryRequirementChecks, normalizeFactorySpecificationText } from "./factory-product.js";

describe("shared factory product specification", () => {
  it("counts connected envelopes once and includes connector widths", () => {
    const session = new DesignSession(createEmptyDesign("PRODUCT-SPEC"));
    for (const [index, widthMm] of [1200, 900].entries()) session.execute(createRectangularWindowCommand({ commandId: "CREATE-" + index, windowId: "W" + index, mark: "C" + index, widthMm, heightMm: 1500 }));
    session.execute(createFabricationAssemblyCommand({
      commandId: "CONNECT", assemblyId: "A1", mark: "A1",
      instances: [
        { objectId: "I1", windowId: "W0", transform: { xMm: 0, yMm: 0, zMm: 0, rotationYDeg: 0 } },
        { objectId: "I2", windowId: "W1", transform: { xMm: 1230, yMm: 0, zMm: 0, rotationYDeg: 0 } }
      ],
      joints: [{ objectId: "J1", jointType: "mullion_joint", firstInstanceId: "I1", firstEdge: "right", secondInstanceId: "I2", secondEdge: "left", gapMm: 30, factoryScope: "factory" }],
      openingClearance: { topMm: 10, rightMm: 12, bottomMm: 10, leftMm: 12 }, installation: REFERENCE_WINDOW_INSTALLATION
    }));
    expect(describeFactoryProduct(session.document)).toMatchObject({ widthMm: 2130, heightMm: 1500, units: [{ id: "A1", kind: "assembly", isCoplanar: true }] });
    expect(describeFactoryProduct(session.document).windows).toHaveLength(2);
    const requirement = { ...describeFactoryProduct(session.document), dueDate: "", note: "" };
    expect(factoryRequirementChecks(session.document, requirement).every((check) => check.matches)).toBe(true);
    expect(factoryRequirementChecks(session.document, { ...requirement, widthMm: 2100 }).find((check) => check.field === "widthMm")!.matches).toBe(false);
  });
  it("normalizes only casing and whitespace, not unlike material or finish codes", () => {
    expect(normalizeFactorySpecificationText("内 ral9016 / 外 RAL7016")).toBe(normalizeFactorySpecificationText("内RAL9016/外RAL7016"));
    expect(normalizeFactorySpecificationText("RAL9016")).not.toBe(normalizeFactorySpecificationText("#FFFFFF"));
    expect(describeFactoryProduct(createEmptyDesign("EMPTY")).units).toEqual([]);
  });
  it("checks six actual drawing specifications without modifying the graph or treating schedule and comments as geometry", () => {
    const session = new DesignSession(createEmptyDesign("REQUIREMENT-CHECK"));
    session.execute(createRectangularWindowCommand({ commandId: "CREATE", windowId: "W1", mark: "C1", widthMm: 1200, heightMm: 1500 }));
    const before = structuredClone(session.document), specification = describeFactoryProduct(session.document);
    const requirement = { ...specification, material: specification.material.toLowerCase(), finish: specification.finish.replaceAll(" ", ""), dueDate: "2026-11-01", note: "交期和说明由人工会签" };
    const checks = factoryRequirementChecks(session.document, requirement);
    expect(checks.map((check) => check.field)).toEqual(["widthMm", "heightMm", "material", "glass", "hardware", "finish"]);
    expect(checks.every((check) => check.matches)).toBe(true);
    const changed = factoryRequirementChecks(session.document, { ...requirement, widthMm: 1250, heightMm: 1600, material: "AL90", glass: "NEW-GLASS", hardware: "NEW-HARDWARE", finish: "#FFFFFF" });
    expect(changed.every((check) => !check.matches)).toBe(true);
    expect(changed[0]).toMatchObject({ required: 1250, actual: 1200 });
    expect(session.document).toEqual(before);
  });
  it("does not accept an empty drawing even when empty specification values match", () => {
    const document = createEmptyDesign("EMPTY-REQUIREMENT"), specification = describeFactoryProduct(document);
    expect(factoryRequirementChecks(document, { ...specification, dueDate: "", note: "" }).every((check) => !check.matches)).toBe(true);
  });
});
