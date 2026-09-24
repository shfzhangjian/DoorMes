import { describe, expect, it } from "vitest";
import {
  createDrawingSheet,
  layoutPaperSpaceDimensions,
  resolveDrawingPaperSizeMm,
  type DrawingLinearDimensionAnnotation,
  type DrawingView
} from "./index";

const VIEW: DrawingView = {
  viewId: "VIEW-ELEVATION-1",
  projection: "factory-sheet",
  viewKind: "elevation",
  sourceObjectIds: ["WIN-001"],
  scaleDenominator: 20,
  modelBoundsMm: { x: 0, y: 0, width: 1600, height: 1500 },
  framePaperMm: { x: 50, y: 60, width: 80, height: 75 }
};

/**
 * Creates one concise linear-dimension fixture for layout tests.
 * @example `dimension("WIDTH", "overall", 0, 1600)` creates a top width.
 * @since 0.1.0
 * @modified 2026-09-21 - Added reusable semantic-annotation test data.
 */
function dimension(
  annotationId: string,
  level: DrawingLinearDimensionAnnotation["level"],
  startModelMm: number,
  endModelMm: number,
  labelWidthPaperMm = 18
): DrawingLinearDimensionAnnotation {
  return {
    annotationId,
    viewId: VIEW.viewId,
    sourceObjectIds: ["WIN-001"],
    layer: "dimensions",
    priority: 0,
    kind: "linear-dimension",
    axis: "horizontal",
    side: "top",
    level,
    startModelMm,
    endModelMm,
    measuredValueMm: Math.abs(endModelMm - startModelMm),
    label: `${Math.abs(endModelMm - startModelMm)} mm`,
    labelWidthPaperMm
  };
}

describe("drawing-model", () => {
  it("resolves physical A-series paper dimensions", () => {
    expect(resolveDrawingPaperSizeMm("A4", "portrait")).toEqual({ width: 210, height: 297 });
    expect(resolveDrawingPaperSizeMm("A3", "landscape")).toEqual({ width: 420, height: 297 });
  });

  it("keeps detail dimensions inside segment and overall dimensions", () => {
    const placements = layoutPaperSpaceDimensions(VIEW, [
      dimension("DETAIL-LEFT", "detail", 0, 800, 46),
      dimension("DETAIL-RIGHT", "detail", 800, 1600, 46),
      dimension("SEGMENT", "segment", 0, 1200),
      dimension("OVERALL", "overall", 0, 1600)
    ]);
    const byId = new Map(placements.map((placement) => [placement.annotationId, placement]));

    expect(byId.get("DETAIL-LEFT")?.laneIndex).toBe(0);
    expect(byId.get("DETAIL-RIGHT")?.laneIndex).toBe(1);
    expect(byId.get("SEGMENT")?.laneIndex).toBe(2);
    expect(byId.get("OVERALL")?.laneIndex).toBe(3);
    expect(byId.get("DETAIL-LEFT")?.offsetFromViewPaperMm).toBe(10);
    expect(byId.get("OVERALL")?.offsetFromViewPaperMm).toBe(34);
    expect(byId.get("OVERALL")?.lineCoordinatePaperMm).toBe(26);
  });

  it("reuses one lane for separated spans and keeps paper offsets independent of scale", () => {
    const annotations = [
      dimension("LEFT", "detail", 0, 500, 12),
      dimension("RIGHT", "detail", 1100, 1600, 12)
    ];
    const first = layoutPaperSpaceDimensions(VIEW, annotations);
    const second = layoutPaperSpaceDimensions({ ...VIEW, scaleDenominator: 10 }, annotations);

    expect(first.map((placement) => placement.laneIndex)).toEqual([0, 0]);
    expect(second.map((placement) => placement.laneIndex)).toEqual([0, 0]);
    expect(first.map((placement) => placement.offsetFromViewPaperMm)).toEqual([10, 10]);
    expect(second.map((placement) => placement.offsetFromViewPaperMm)).toEqual([10, 10]);
    expect(first[0]?.intervalEndPaperMm).toBe(75);
    expect(second[0]?.intervalEndPaperMm).toBe(100);
  });

  it("validates stable identities, view ownership and dimension orientation", () => {
    expect(() => layoutPaperSpaceDimensions(VIEW, [
      dimension("DUPLICATE", "detail", 0, 500),
      dimension("DUPLICATE", "detail", 600, 1000)
    ])).toThrow(/unique/);
    expect(() => layoutPaperSpaceDimensions(VIEW, [{
      ...dimension("WRONG-AXIS", "detail", 0, 500),
      axis: "vertical"
    }])).toThrow(/axis/);
  });

  it("creates an immutable-format sheet snapshot without renderer nodes", () => {
    const annotation = dimension("WIDTH", "overall", 0, 1600);
    const sheet = createDrawingSheet({
      sheetId: "SHEET-WIN-001-FACTORY",
      drawingNumber: "DM-WIN-001-GA",
      drawingVersion: "R12",
      sourceDocumentId: "DESIGN-001",
      sourceRevision: 12,
      profile: "factory",
      paperFormat: "A3",
      orientation: "landscape",
      title: "WIN-001 工厂总装图",
      views: [VIEW],
      annotations: [annotation],
      tables: [{
        tableId: "MATERIALS",
        kind: "design-selection",
        title: "设计选型",
        framePaperMm: { x: 280, y: 20, width: 120, height: 25 },
        layout: {
          titleHeightPaperMm: 7,
          headerHeightPaperMm: 6,
          rowHeightPaperMm: 7,
          titleFontSizePaperMm: 3.6,
          headerFontSizePaperMm: 3.1,
          bodyFontSizePaperMm: 3
        },
        columns: [
          { key: "code", label: "编码", widthPaperMm: 50 },
          { key: "name", label: "名称", widthPaperMm: 70 }
        ],
        rows: [{
          rowId: "MATERIALS:1",
          sourceObjectIds: ["WIN-001"],
          cells: ["AL70", "70系统"]
        }]
      }]
    });

    expect(sheet.schemaVersion).toBe("doormes-drawing-sheet.v1");
    expect(sheet.views[0]).not.toBe(VIEW);
    expect(sheet.annotations[0]).not.toBe(annotation);
    expect(Object.isFrozen(sheet)).toBe(true);
    expect(Object.isFrozen(sheet.views)).toBe(true);
    expect(Object.isFrozen(sheet.tables?.[0]?.rows[0]?.cells)).toBe(true);
    expect(JSON.stringify(sheet)).not.toMatch(/<svg|three|canvas/i);
  });

  it("rejects views outside the page or using the wrong technical-sheet projection", () => {
    const base = {
      sheetId: "SHEET-INVALID",
      drawingNumber: "DM-INVALID-GA",
      drawingVersion: "R1",
      sourceDocumentId: "DESIGN-001",
      sourceRevision: 1,
      profile: "factory" as const,
      paperFormat: "A4" as const,
      orientation: "portrait" as const,
      title: "Invalid",
      annotations: []
    };
    expect(() => createDrawingSheet({
      ...base,
      views: [{ ...VIEW, framePaperMm: { x: 190, y: 0, width: 80, height: 75 } }]
    })).toThrow(/fit within/);
    expect(() => createDrawingSheet({
      ...base,
      views: [{ ...VIEW, projection: "installation-sheet" }]
    })).toThrow(/factory-sheet/);
    expect(() => createDrawingSheet({
      ...base,
      views: [VIEW],
      tables: [{
        tableId: "TOO-WIDE",
        kind: "manufacturing-materials",
        title: "Invalid",
        framePaperMm: { x: 10, y: 10, width: 30, height: 20 },
        layout: {
          titleHeightPaperMm: 6,
          headerHeightPaperMm: 6,
          rowHeightPaperMm: 6,
          titleFontSizePaperMm: 3,
          headerFontSizePaperMm: 3,
          bodyFontSizePaperMm: 3
        },
        columns: [{ key: "code", label: "编码", widthPaperMm: 40 }],
        rows: []
      }]
    })).toThrow(/columns exceed/);
  });
});
