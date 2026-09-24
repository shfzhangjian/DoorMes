import { describe, expect, it } from "vitest";
import {
  createFabricationAssemblyCommand,
  createRectangularWindowCommand,
  createSetWindowCellOpeningCommand,
  createUpdateWindowDesignComponentRemarksCommand,
  DesignSession
} from "@doormes/application";
import { createEmptyDesign } from "@doormes/domain";
import { REFERENCE_WINDOW_INSTALLATION } from "@doormes/geometry-topology";
import { calculateFormalBom } from "@doormes/calculation-engine";
import type { ManufacturingCatalog } from "@doormes/manufacturing-model";
import { projectFactoryDrawingSheets, projectFactoryElevationSheet } from "./index";

/** Reviewed test catalog used to prove formal MBOM projection without UI state. */
const TEST_CATALOG = {
  profileSystems: [{
    id: "AL70",
    name: "70断桥铝系统窗",
    material: "aluminum",
    frameProfile: "AL70-K01",
    sashProfile: "AL70-S01",
    mullionProfile: "AL70-Z01",
    beadProfile: "AL70-YT01",
    gasketCode: "EPDM-70",
    faceWidthMm: 70,
    sashFaceWidthMm: 58,
    sawKerfMm: 4
  }],
  glassTypes: [{ id: "GL-LOWE-24", name: "5+14A+5 Low-E", thicknessMm: 24 }],
  hardwareSets: [{
    id: "HW-TT-STD",
    name: "内开内倒标准五金",
    handleCode: "HW-BS-01",
    hingeCode: "HW-HJ-70",
    memberName: "传动器/铰链",
    hingeQtyRule: "height>1800?3:2"
  }]
} satisfies ManufacturingCatalog;

/**
 * Creates a two-window connected product used by the factory-sheet tests.
 * @since 0.1.0
 * @modified 2026-09-21 - Added a stable projection integration fixture.
 */
function createAssemblySession(
  jointType: "mullion_joint" | "reinforced_mullion" | "stacking_joint" = "mullion_joint"
): DesignSession {
  const stacking = jointType === "stacking_joint";
  const session = new DesignSession(createEmptyDesign("DESIGN-DRAWING-PROJECTION"));
  session.execute(createRectangularWindowCommand({
    commandId: "CREATE-DRAWING-W1",
    windowId: "WIN-DRAWING-W1",
    mark: "W1",
    widthMm: 1200,
    heightMm: 1500,
    cellId: "CELL-DRAWING-W1",
    defaultGlassTypeId: "GL-LOWE-24"
  }));
  session.execute(createSetWindowCellOpeningCommand({
    commandId: "OPEN-DRAWING-W1",
    windowId: "WIN-DRAWING-W1",
    cellId: "CELL-DRAWING-W1",
    cellType: "turn_tilt",
    opening: "left_in",
    hardwareSetId: "HW-TT-STD"
  }));
  session.execute(createUpdateWindowDesignComponentRemarksCommand({
    commandId: "REMARK-DRAWING-W1",
    windowId: "WIN-DRAWING-W1",
    remarks: {
      profile: "外框转角下料复核",
      glass: "玻璃标签朝室内",
      hardware: "执手颜色随样板",
      surround: ""
    }
  }));
  session.execute(createRectangularWindowCommand({
    commandId: "CREATE-DRAWING-W2",
    windowId: "WIN-DRAWING-W2",
    mark: "W2",
    widthMm: stacking ? 1200 : 900,
    heightMm: 1500,
    cellId: "CELL-DRAWING-W2",
    defaultGlassTypeId: "GL-LOWE-24"
  }));
  session.execute(createFabricationAssemblyCommand({
    commandId: "CREATE-DRAWING-ASSEMBLY",
    assemblyId: "ASSEMBLY-DRAWING",
    mark: "A-01",
    instances: [
      {
        objectId: "ASSEMBLY-DRAWING:I1",
        windowId: "WIN-DRAWING-W1",
        transform: { xMm: 0, yMm: 0, zMm: 0, rotationYDeg: 0 }
      },
      {
        objectId: "ASSEMBLY-DRAWING:I2",
        windowId: "WIN-DRAWING-W2",
        transform: stacking
          ? { xMm: 0, yMm: 1530, zMm: 0, rotationYDeg: 0 }
          : { xMm: 1230, yMm: 0, zMm: 0, rotationYDeg: 0 }
      }
    ],
    joints: [{
      objectId: "ASSEMBLY-DRAWING:J1",
      jointType,
      firstInstanceId: "ASSEMBLY-DRAWING:I1",
      firstEdge: stacking ? "bottom" : "right",
      secondInstanceId: "ASSEMBLY-DRAWING:I2",
      secondEdge: stacking ? "top" : "left",
      gapMm: 30,
      factoryScope: "factory"
    }],
    openingClearance: { topMm: 10, rightMm: 12, bottomMm: 15, leftMm: 12 },
    installation: REFERENCE_WINDOW_INSTALLATION
  }));
  return session;
}

describe("projectFactoryElevationSheet", () => {
  it("projects one connected product without reading interactive SVG", () => {
    const session = createAssemblySession();
    const sheet = projectFactoryElevationSheet(session.document, "ASSEMBLY-DRAWING");
    const view = sheet.views[0]!;

    expect(sheet.schemaVersion).toBe("doormes-drawing-sheet.v1");
    expect(sheet.profile).toBe("factory");
    expect(sheet.sourceRevision).toBe(session.document.revision);
    expect(sheet.drawingNumber).toBe("DM-A-01-GA");
    expect(sheet.drawingVersion).toBe(`R${session.document.revision}`);
    expect(sheet.title).toBe("A-01 工厂总装图");
    expect(view.projection).toBe("factory-sheet");
    expect(view.scaleDenominator).toBe(20);
    expect(view.modelBoundsMm).toEqual({ x: 0, y: 0, width: 2130, height: 1500 });
    expect(view.primitives?.some((item) => item.layer === "joint")).toBe(true);
    expect(view.primitives?.filter((item) => item.layer === "symbol")).toHaveLength(2);
    expect(sheet.annotations.some((item) =>
      item.kind === "linear-dimension" && item.label === "组合总宽 2130 mm"
    )).toBe(true);
    expect(sheet.annotations.some((item) =>
      item.kind === "linear-dimension" && item.level === "detail"
    )).toBe(true);
    expect(sheet.annotations.filter((item) => item.kind === "material-callout")).toHaveLength(0);
    const plan = sheet.views.find((candidate) => candidate.viewKind === "plan");
    expect(plan).toBeDefined();
    expect(plan?.scaleDenominator).toBe(20);
    expect(plan?.primitives?.some((item) =>
      item.kind === "polyline" && item.primitiveId.endsWith(":actual-plan")
    )).toBe(true);
    expect(plan?.primitives?.some((item) =>
      item.kind === "polyline" && item.primitiveId.endsWith(":angle-arc")
    )).toBe(true);
    expect(sheet.annotations.some((item) =>
      item.viewId === plan?.viewId && item.kind === "opening-symbol" && item.text?.includes("72°")
    )).toBe(true);
    expect(sheet.annotations.some((item) =>
      item.viewId === plan?.viewId && item.kind === "note" && item.text === "室外 ↑"
    )).toBe(true);
    const detail = sheet.views.find((candidate) => candidate.viewKind === "detail");
    expect(detail).toBeDefined();
    expect(detail?.sourceObjectIds).toContain("ASSEMBLY-DRAWING:J1");
    expect(detail?.primitives?.some((item) =>
      item.primitiveId.endsWith(":detail:connector-zone") && item.layer === "joint"
    )).toBe(true);
    expect(sheet.annotations.some((item) =>
      item.viewId === view.viewId && item.kind === "component-callout" && item.text === "节点 A"
    )).toBe(true);
    expect(sheet.annotations.some((item) =>
      item.viewId === detail?.viewId && item.kind === "linear-dimension" &&
      item.label === "接缝 30 mm"
    )).toBe(true);
    expect(sheet.annotations.some((item) =>
      item.viewId === detail?.viewId && item.kind === "component-callout" &&
      item.text === "连接料截面待正式目录"
    )).toBe(true);
    expect(sheet.tables?.map((table) => table.kind)).toEqual(["design-selection"]);
    const selectionTable = sheet.tables?.find((table) => table.kind === "design-selection");
    expect(selectionTable?.title).toBe("门窗设计组成件");
    expect(selectionTable?.columns[0]).toMatchObject({ key: "number", label: "编号" });
    expect(selectionTable?.columns[3]).toMatchObject({
      key: "dimension", label: "下料(mm)·端角·数量"
    });
    expect(selectionTable?.columns[4]).toMatchObject({ key: "remark", label: "备注" });
    const selectionNumbers = selectionTable?.rows.map((row) => row.cells[0]) ?? [];
    expect(selectionNumbers).toEqual([
      "W1-PF", "W1-GL", "W1-HW",
      "W2-PF", "W2-GL", "W2-HW"
    ]);
    expect(new Set(selectionNumbers).size).toBe(selectionNumbers.length);
    const selectionCalloutNumbers = sheet.annotations.flatMap((item) =>
      item.viewId === view.viewId && item.kind === "component-callout" &&
      item.sourceObjectIds.some((sourceId) => sourceId.includes(":design-selection."))
        ? [item.text]
        : []
    );
    expect(selectionCalloutNumbers).toEqual(selectionNumbers);
    expect(selectionTable?.rows[0]?.cells[4]).toBe("外框转角下料复核");
    expect(JSON.stringify(sheet.tables)).not.toMatch(
      /冻结信息|生产数据状态|连接件与工序|工序信息|规则\/工序|BOM-(?:NOT-CALCULATED|STALE|CURRENT)/
    );
    expect(sheet.tables?.every((table) => table.layout.bodyFontSizePaperMm >= 3)).toBe(true);
    expect(JSON.stringify(sheet)).not.toMatch(/design-window|<svg|three/i);
  });

  it("keeps the table design-only while a current snapshot enriches connection details", () => {
    const session = createAssemblySession();
    const result = calculateFormalBom(session.document, TEST_CATALOG);
    const sheet = projectFactoryElevationSheet(session.document, "ASSEMBLY-DRAWING", {
      productionSnapshot: { sourceRevision: session.document.revision, result }
    });

    expect(sheet.tables?.map((table) => table.kind)).toEqual(["design-selection"]);
    expect(sheet.tables?.[0]?.columns.map((column) => column.label)).toEqual([
      "编号", "构件", "型号/物料编码", "下料(mm)·端角·数量", "备注"
    ]);
    const componentRows = sheet.tables?.[0]?.rows.filter((row) => row.cells[0] !== "…") ?? [];
    const componentNumbers = componentRows.map((row) => row.cells[0]);
    expect(new Set(componentNumbers).size).toBe(componentNumbers.length);
    expect(componentNumbers).not.toContain("W1-PF");
    expect(componentRows.some((row) =>
      row.cells[2] === "AL70-K01" &&
      /1200 · 45\/45° · 1件/.test(row.cells[3] ?? "")
    )).toBe(true);
    expect(componentRows.some((row) =>
      row.cells[1] === "玻璃" && /\d+×\d+×24 mm · 1件/.test(row.cells[3] ?? "")
    )).toBe(true);
    const calculatedCallouts = sheet.annotations.flatMap((annotation) =>
      annotation.kind === "component-callout" &&
      annotation.viewId === sheet.views[0]?.viewId &&
      componentNumbers.includes(annotation.text)
        ? [annotation.text ?? ""]
        : []
    );
    expect(calculatedCallouts.length).toBeGreaterThanOrEqual(5);
    expect(calculatedCallouts.every((text) =>
      !text.endsWith("-PF") &&
      !text.endsWith("-GL") &&
      !text.endsWith("-HW")
    )).toBe(true);
    const featureById = new Map(result.features.map((feature) => [feature.featureId, feature]));
    const expectedVisibleInstanceIds = result.productionInstances.flatMap((instance) => {
      const feature = featureById.get(instance.sourceFeatureId);
      if (!feature || feature.kind === "seal-path" || feature.kind === "installation-material") {
        return [];
      }
      if (feature.kind === "hardware-demand" &&
        !/\.(?:handle|hinge)$/i.test(feature.sourceComponentId)) return [];
      if (feature.kind === "engineering-joint-material" &&
        !["connector", "reinforcement", "cover"].includes(feature.role)) return [];
      return [instance.productionInstanceId];
    });
    const calledOutInstanceIds = sheet.annotations.flatMap((annotation) =>
      annotation.kind === "component-callout" &&
      annotation.viewId === sheet.views[0]?.viewId
        ? annotation.sourceObjectIds.filter((sourceId) => sourceId.startsWith("PI-LOCAL-"))
        : []
    );
    expect(new Set(calledOutInstanceIds)).toEqual(new Set(expectedVisibleInstanceIds));
    expect(calledOutInstanceIds).toHaveLength(expectedVisibleInstanceIds.length);
    expect(sheet.annotations.some((annotation) =>
      annotation.kind === "component-callout" && annotation.labelOffsetPaperMm !== undefined
    )).toBe(true);
    const detail = sheet.views.find((candidate) => candidate.viewKind === "detail");
    expect(detail?.sourceObjectIds.some((sourceId) =>
      sourceId.endsWith(".process.drill-fasteners")
    )).toBe(true);
    expect(sheet.annotations.some((item) =>
      item.viewId === detail?.viewId && item.kind === "component-callout" &&
      item.text === "连接料 JNT-MUL-CONN-01"
    )).toBe(true);
  });

  it("moves every overflow component to numbered continuation sheets", () => {
    const session = createAssemblySession();
    const result = calculateFormalBom(session.document, TEST_CATALOG);
    const sheets = projectFactoryDrawingSheets(session.document, "ASSEMBLY-DRAWING", {
      productionSnapshot: { sourceRevision: session.document.revision, result }
    });

    expect(sheets.length).toBeGreaterThan(1);
    expect(sheets.map((sheet) => sheet.pageNumber)).toEqual(
      sheets.map((_sheet, index) => index + 1)
    );
    expect(sheets.every((sheet) => sheet.pageCount === sheets.length)).toBe(true);
    expect(sheets.every((sheet) => sheet.drawingNumber === sheets[0]?.drawingNumber)).toBe(true);
    expect(sheets.every((sheet) => sheet.drawingVersion === sheets[0]?.drawingVersion)).toBe(true);
    const componentRows = sheets.flatMap((sheet) =>
      sheet.tables?.flatMap((table) => table.rows) ?? []
    );
    expect(componentRows).toHaveLength(result.productionInstances.length);
    expect(componentRows.some((row) => row.cells[0] === "…")).toBe(false);
    expect(new Set(componentRows.map((row) => row.cells[0])).size).toBe(componentRows.length);
    const scheduleNumbers = new Set(componentRows.map((row) => row.cells[0]));
    const calloutNumbers = sheets[0]!.annotations.flatMap((annotation) =>
      annotation.kind === "component-callout" &&
      annotation.sourceObjectIds.some((sourceId) => sourceId.startsWith("PI-LOCAL-"))
        ? [annotation.text ?? ""]
        : []
    );
    expect(calloutNumbers.every((number) => scheduleNumbers.has(number))).toBe(true);
    expect(sheets.slice(1).every((sheet) =>
      sheet.views.length === 0 && sheet.title.includes("续表")
    )).toBe(true);
  });

  it("maps all three straight connection semantics into traceable node details", () => {
    const cases = [
      ["mullion_joint", "拼樘连接", "JNT-MUL-CONN-01", "竖向"],
      ["reinforced_mullion", "加强拼樘", "JNT-RMUL-CONN-01", "竖向"],
      ["stacking_joint", "上下叠接", "JNT-STK-CONN-01", "横向"]
    ] as const;
    for (const [jointType, label, connectorCode, orientation] of cases) {
      const session = createAssemblySession(jointType);
      const result = calculateFormalBom(session.document, TEST_CATALOG);
      const sheet = projectFactoryElevationSheet(session.document, "ASSEMBLY-DRAWING", {
        productionSnapshot: { sourceRevision: session.document.revision, result }
      });
      const detail = sheet.views.find((candidate) => candidate.viewKind === "detail");
      const title = sheet.annotations.find((item) =>
        item.viewId === detail?.viewId && item.kind === "note" &&
        item.annotationId.endsWith(":detail:title")
      );
      const connector = sheet.annotations.find((item) =>
        item.viewId === detail?.viewId && item.kind === "component-callout"
      );

      const titleText = title?.kind === "linear-dimension" ? undefined : title?.text;
      const connectorText = connector?.kind === "linear-dimension" ? undefined : connector?.text;
      expect(titleText).toContain(label);
      expect(titleText).toContain(`${orientation}截面示意`);
      expect(connectorText).toContain(connectorCode);
      expect(sheet.tables?.map((table) => table.kind)).toEqual(["design-selection"]);
      expect(detail?.modelBoundsMm.width).toBe(170);
      expect(sheet.annotations.some((annotation) =>
        annotation.viewId === detail?.viewId && annotation.kind === "linear-dimension" &&
        annotation.label === "接缝 30 mm"
      )).toBe(true);
      expect(detail?.primitives?.some((primitive) =>
        primitive.primitiveId.endsWith(":detail:reinforcement")
      )).toBe(jointType === "reinforced_mullion");
    }
  });

  it("never adds production status rows for a stale calculation snapshot", () => {
    const session = createAssemblySession();
    const result = calculateFormalBom(session.document, TEST_CATALOG);
    const sheet = projectFactoryElevationSheet(session.document, "ASSEMBLY-DRAWING", {
      productionSnapshot: { sourceRevision: session.document.revision - 1, result }
    });

    expect(sheet.tables?.map((table) => table.kind)).toEqual(["design-selection"]);
    expect(JSON.stringify(sheet.tables)).not.toMatch(/BOM-STALE|生产数据状态/);
  });

  it("projects an independent window and rejects unknown subjects", () => {
    const session = createAssemblySession();
    const sheet = projectFactoryElevationSheet(session.document, "WIN-DRAWING-W1", {
      scaleDenominator: 20,
      paperFormat: "A4",
      orientation: "landscape"
    });

    expect(sheet.views[0]?.scaleDenominator).toBe(20);
    expect(sheet.views.some((candidate) => candidate.viewKind === "plan")).toBe(true);
    expect(sheet.annotations.some((item) => item.kind === "material-callout")).toBe(false);
    expect(() => projectFactoryElevationSheet(session.document, "MISSING"))
      .toThrow(/does not exist/);
  });
});
