import { describe, expect, it } from "vitest";
import {
  createDrawingSheet,
  type DrawingLinearDimensionAnnotation,
  type DrawingView
} from "@doormes/drawing-model";
import { renderTechnicalDrawingSheetSvg } from "./index";

const view: DrawingView = {
  viewId: "VIEW-FACTORY",
  projection: "factory-sheet",
  viewKind: "elevation",
  sourceObjectIds: ["WIN-001"],
  scaleDenominator: 10,
  modelBoundsMm: { x: 0, y: 0, width: 1200, height: 1500 },
  framePaperMm: { x: 80, y: 60, width: 120, height: 150 },
  primitives: [
    {
      primitiveId: "WIN-001:frame.left",
      sourceObjectIds: ["WIN-001", "WIN-001:frame.left"],
      layer: "frame",
      kind: "rectangle",
      boundsModelMm: { x: 0, y: 0, width: 70, height: 1500 }
    },
    {
      primitiveId: "WIN-001:opening-symbol",
      sourceObjectIds: ["WIN-001:P1"],
      layer: "symbol",
      kind: "line",
      startModelMm: { x: 70, y: 70 },
      endModelMm: { x: 1130, y: 750 }
    }
  ]
};

/** Schematic connection section used to verify detail-view serialization. */
const detailView: DrawingView = {
  viewId: "VIEW-JOINT-A",
  projection: "factory-sheet",
  viewKind: "detail",
  sourceObjectIds: ["ASSEMBLY-001:J1", "FEATURE-J1-CONNECTOR"],
  scaleDenominator: 5,
  modelBoundsMm: { x: 0, y: 0, width: 170, height: 70 },
  framePaperMm: { x: 20, y: 225, width: 34, height: 14 },
  primitives: [{
    primitiveId: "ASSEMBLY-001:J1:connector-zone",
    sourceObjectIds: ["ASSEMBLY-001:J1", "FEATURE-J1-CONNECTOR"],
    layer: "joint",
    kind: "rectangle",
    boundsModelMm: { x: 70, y: 8, width: 30, height: 54 }
  }]
};

/** Creates one dimension fixture around the shared factory view. */
function dimension(
  annotationId: string,
  side: DrawingLinearDimensionAnnotation["side"],
  level: DrawingLinearDimensionAnnotation["level"],
  axis: DrawingLinearDimensionAnnotation["axis"],
  startModelMm: number,
  endModelMm: number,
  label: string
): DrawingLinearDimensionAnnotation {
  return {
    annotationId,
    viewId: view.viewId,
    sourceObjectIds: ["WIN-001"],
    layer: "dimensions",
    priority: 0,
    kind: "linear-dimension",
    axis,
    side,
    level,
    startModelMm,
    endModelMm,
    measuredValueMm: Math.abs(endModelMm - startModelMm),
    label,
    witnessOriginModelMm: axis === "horizontal" ? 0 : 1200
  };
}

describe("renderTechnicalDrawingSheetSvg", () => {
  it("renders physical black-and-white linework, hierarchical dimensions and a title block", () => {
    const sheet = createDrawingSheet({
      sheetId: "SHEET-WIN-001",
      drawingNumber: "DM-W1-GA",
      drawingVersion: "A.2",
      sourceDocumentId: "DESIGN-001",
      sourceRevision: 7,
      profile: "factory",
      paperFormat: "A3",
      orientation: "landscape",
      title: "W1 工厂总装图",
      pageNumber: 1,
      pageCount: 2,
      views: [view, detailView],
      annotations: [
        dimension("CELL-WIDTH", "top", "detail", "horizontal", 70, 1130, "净宽 1060 mm"),
        dimension("OVERALL-WIDTH", "bottom", "overall", "horizontal", 0, 1200, "总宽 1200 mm"),
        dimension("OVERALL-HEIGHT", "right", "overall", "vertical", 0, 1500, "总高 1500 mm"),
        {
          annotationId: "MATERIAL-NOTE",
          viewId: view.viewId,
          sourceObjectIds: ["WIN-001"],
          layer: "materials",
          priority: 0,
          kind: "material-callout",
          anchorModelMm: { x: 1200, y: 0 },
          labelOffsetPaperMm: { x: -12, y: 6 },
          text: "型材 AL70；玻璃 GLASS-5"
        },
        {
          annotationId: "PI-LOCAL-TRACE:factory-callout",
          viewId: view.viewId,
          sourceObjectIds: ["WIN-001:frame.top", "PI-LOCAL-TRACE"],
          layer: "materials",
          priority: 5,
          kind: "component-callout",
          anchorModelMm: { x: 600, y: 0 },
          labelOffsetPaperMm: { x: -18.5, y: 7.25 },
          text: "W1-FR01"
        },
        {
          annotationId: "JOINT-A-GAP",
          viewId: detailView.viewId,
          sourceObjectIds: ["ASSEMBLY-001:J1"],
          layer: "dimensions",
          priority: 0,
          kind: "linear-dimension",
          axis: "horizontal",
          side: "bottom",
          level: "detail",
          startModelMm: 70,
          endModelMm: 100,
          measuredValueMm: 30,
          witnessOriginModelMm: 70,
          label: "接缝 30 mm"
        }
      ],
      tables: [{
        tableId: "WIN-001:MATERIALS",
        kind: "manufacturing-materials",
        title: "正式MBOM明细 · r7",
        framePaperMm: { x: 285, y: 20, width: 115, height: 22 },
        layout: {
          titleHeightPaperMm: 7.5,
          headerHeightPaperMm: 7,
          rowHeightPaperMm: 7,
          titleFontSizePaperMm: 3.8,
          headerFontSizePaperMm: 3.2,
          bodyFontSizePaperMm: 3
        },
        columns: [
          { key: "code", label: "物料编码", widthPaperMm: 35 },
          { key: "name", label: "名称", widthPaperMm: 30 },
          { key: "quantity", label: "数量", widthPaperMm: 50, align: "right" }
        ],
        rows: [{
          rowId: "MBOM-0001",
          sourceObjectIds: ["WIN-001", "WIN-001:frame.top"],
          cells: ["AL70-K01", "上框", "1 pcs"],
          severity: "warning"
        }]
      }]
    });
    const svg = renderTechnicalDrawingSheetSvg(sheet);

    expect(svg).toContain('width="420mm" height="297mm" viewBox="0 0 420 297"');
    expect(svg).toContain('class="technical-primitive technical-primitive--frame"');
    expect(svg).toContain('class="technical-primitive technical-primitive--symbol"');
    expect(svg).toContain('class="technical-view technical-view--detail"');
    expect(svg).toContain('data-view-id="VIEW-JOINT-A"');
    expect(svg).toContain('data-source-object-ids="ASSEMBLY-001:J1 FEATURE-J1-CONNECTOR"');
    expect(svg).toContain("接缝 30 mm");
    expect(svg).toContain('stroke-dasharray="2 1"');
    expect(svg).toContain('data-annotation-id="CELL-WIDTH"');
    expect(svg).toContain('data-level="detail"');
    expect(svg).toContain('data-level="overall"');
    expect(svg).toContain("净宽 1060 mm");
    expect(svg).toContain("型材 AL70；玻璃 GLASS-5");
    expect(svg).toContain('data-annotation-id="PI-LOCAL-TRACE:factory-callout"');
    expect(svg).toContain('data-label-offset-x="-18.5"');
    expect(svg).toContain('data-label-offset-y="7.25"');
    expect(svg).toContain("W1-FR01");
    expect(svg).toContain('text-anchor="end"');
    expect(svg).toContain('rx="0.7" fill="white"');
    expect(svg).toContain('data-table-kind="manufacturing-materials"');
    expect(svg).toContain('data-row-id="MBOM-0001"');
    expect(svg).toContain('data-severity="warning"');
    expect(svg).toContain("正式MBOM明细 · r7");
    expect(svg).toContain("AL70-K01");
    expect(svg).toContain('font-size="3.8"');
    expect(svg).toContain('font-size="3.2"');
    expect(svg).toContain('font-size="3"');
    expect(svg).not.toContain('font-weight="bold"');
    expect(svg).toContain('font-weight="normal" stroke="none"');
    expect(svg).toContain('class="technical-title-block"');
    expect(svg).toContain("图号 DM-W1-GA");
    expect(svg).toContain("版本 A.2");
    expect(svg).toContain('data-drawing-number="DM-W1-GA"');
    expect(svg).toContain('data-drawing-version="A.2"');
    expect(svg).toContain('data-page-number="1" data-page-count="2"');
    expect(svg).toContain("第 1/2 页");
    expect(svg).not.toContain("texture");
    expect(svg).not.toContain("metalness");
  });
});
