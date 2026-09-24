import { describe, expect, it } from "vitest";
import {
  createFabricationAssemblyCommand,
  createRectangularWindowCommand,
  DesignSession
} from "@doormes/application";
import { createEmptyDesign } from "@doormes/domain";
import { REFERENCE_WINDOW_INSTALLATION } from "@doormes/geometry-topology";
import {
  buildFactoryDrawingPrintDocument,
  resolveFactoryDrawingSubjectId
} from "./factory-drawing-controls";

/**
 * Creates two connected windows plus one standalone product for export-scope tests.
 * @since 0.10.55
 * @modified 2026-09-21 - Added factory drawing selection fixtures.
 */
function createSelectionSession(): DesignSession {
  const session = new DesignSession(createEmptyDesign("DESIGN-DRAWING-SELECTION"));
  for (const [suffix, widthMm] of [["1", 1200], ["2", 900], ["3", 700]] as const) {
    session.execute(createRectangularWindowCommand({
      commandId: `CREATE-W${suffix}`,
      windowId: `WIN-${suffix}`,
      mark: `W${suffix}`,
      widthMm,
      heightMm: 1500,
      cellId: `CELL-${suffix}`
    }));
  }
  session.execute(createFabricationAssemblyCommand({
    commandId: "CREATE-A1",
    assemblyId: "ASSEMBLY-1",
    mark: "A1",
    instances: [
      {
        objectId: "ASSEMBLY-1:I1",
        windowId: "WIN-1",
        transform: { xMm: 0, yMm: 0, zMm: 0, rotationYDeg: 0 }
      },
      {
        objectId: "ASSEMBLY-1:I2",
        windowId: "WIN-2",
        transform: { xMm: 1230, yMm: 0, zMm: 0, rotationYDeg: 0 }
      }
    ],
    joints: [{
      objectId: "ASSEMBLY-1:J1",
      jointType: "mullion_joint",
      firstInstanceId: "ASSEMBLY-1:I1",
      firstEdge: "right",
      secondInstanceId: "ASSEMBLY-1:I2",
      secondEdge: "left",
      gapMm: 30,
      factoryScope: "factory"
    }],
    openingClearance: { topMm: 10, rightMm: 10, bottomMm: 10, leftMm: 10 },
    installation: REFERENCE_WINDOW_INSTALLATION
  }));
  return session;
}

describe("resolveFactoryDrawingSubjectId", () => {
  it("promotes connected child selections to the factory assembly", () => {
    const session = createSelectionSession();
    expect(resolveFactoryDrawingSubjectId(session.document, "ASSEMBLY-1:J1")).toBe("ASSEMBLY-1");
    expect(resolveFactoryDrawingSubjectId(session.document, "ASSEMBLY-1:I2")).toBe("ASSEMBLY-1");
    expect(resolveFactoryDrawingSubjectId(session.document, "CELL-1")).toBe("ASSEMBLY-1");
    expect(resolveFactoryDrawingSubjectId(session.document, "WIN-1:frame.left")).toBe("ASSEMBLY-1");
  });

  it("keeps a selected standalone window independent and refuses an ambiguous blank selection", () => {
    const session = createSelectionSession();
    expect(resolveFactoryDrawingSubjectId(session.document, "CELL-3")).toBe("WIN-3");
    expect(resolveFactoryDrawingSubjectId(session.document)).toBeUndefined();
    expect(resolveFactoryDrawingSubjectId(session.document, "UNKNOWN")).toBeUndefined();
  });
});

describe("buildFactoryDrawingPrintDocument", () => {
  it("keeps one A3 landscape snapshot at exact physical print dimensions", () => {
    const source = '<svg width="420mm" height="297mm"><text>工厂图</text></svg>';
    const output = buildFactoryDrawingPrintDocument({
      source,
      title: "A1 工厂总装图",
      paperFormat: "A3",
      orientation: "landscape",
      sourceRevision: 12,
      drawingNumber: "DM-A1-GA",
      drawingVersion: "R12"
    });

    expect(output).toContain("<title>DM-A1-GA R12 · A1 工厂总装图</title>");
    expect(output).toContain("@page{size:420mm 297mm;margin:0}");
    expect(output).toContain("width:420mm;height:297mm");
    expect(output).toContain(source);
  });

  it("rotates A4 only from paper metadata and escapes the browser title", () => {
    const output = buildFactoryDrawingPrintDocument({
      source: '<svg width="297mm" height="210mm"></svg>',
      title: "W1 <选型>",
      paperFormat: "A4",
      orientation: "landscape",
      sourceRevision: 3,
      drawingNumber: "DM-W1-GA",
      drawingVersion: "A<3"
    });

    expect(output).toContain("<title>DM-W1-GA A&lt;3 · W1 &lt;选型&gt;</title>");
    expect(output).toContain("@page{size:297mm 210mm;margin:0}");
  });

  it("stacks continuation SVG pages for preview and page-broken printing", () => {
    const first = '<svg data-page-number="1"></svg>';
    const second = '<svg data-page-number="2"></svg>';
    const output = buildFactoryDrawingPrintDocument({
      source: first,
      sources: [first, second],
      title: "A1 工厂总装图",
      paperFormat: "A3",
      orientation: "landscape",
      sourceRevision: 13,
      drawingNumber: "DM-A1-GA",
      drawingVersion: "R13"
    });

    expect(output).toContain('class="factory-print-sheet" data-page-number="1"');
    expect(output).toContain('class="factory-print-sheet" data-page-number="2"');
    expect(output).toContain("page-break-after:always");
    expect(output).toContain(first);
    expect(output).toContain(second);
  });
});
