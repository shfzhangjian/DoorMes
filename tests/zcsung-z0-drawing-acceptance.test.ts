import { readFileSync } from "node:fs";
import { resolve } from "node:path";
import { describe, expect, it } from "vitest";
import {
  DesignSession,
  planZcsungSimulationWindowCreation
} from "@doormes/application";
import { resolveWindowVisualConfigurationForRender } from "@doormes/appearance-model";
import {
  adaptLegacyManufacturingCatalog,
  calculateFormalBom
} from "@doormes/calculation-engine";
import { createEmptyDesign } from "@doormes/domain";
import { createOpeningPanelKey } from "@doormes/opening-kinematics";
import { parseLegacyV2Design } from "@doormes/persistence";
import { renderDesignSvg } from "@doormes/renderer-svg";
import { ThreeDesignSceneBuilder } from "@doormes/renderer-three";

/**
 * One target-product drawing acceptance contract independent of any shell.
 *
 * These values are review expectations for DoorMes' public-reference
 * simulations, not customer-approved dimensions or manufacturing rules. The
 * test deliberately crosses application, SVG, Three, appearance and BOM layers
 * so a template cannot appear selectable while one downstream view is broken.
 *
 * @since 0.10.78
 * @modified 2026-09-22 - Added the first Z0 target-product acceptance matrix.
 */
interface ZcsungZ0AcceptanceCase {
  readonly templateId: string;
  readonly productName: string;
  readonly widthMm: number;
  readonly heightMm: number;
  readonly opening: "right_in" | "right_out";
  readonly openPlane: "in" | "out";
  readonly panelCount: 1 | 2;
  readonly hardwareSetId: "HW-TT-STD" | "HW-TURN-STD";
  readonly hasFixedMeetingMullion: boolean;
}

const Z0_CASES: readonly ZcsungZ0AcceptanceCase[] = [
  {
    templateId: "ZCSUNG-SIM-LH-120-TT",
    productName: "120内开内倒系统窗",
    widthMm: 1200,
    heightMm: 1500,
    opening: "right_in",
    openPlane: "in",
    panelCount: 1,
    hardwareSetId: "HW-TT-STD",
    hasFixedMeetingMullion: false
  },
  {
    templateId: "ZCSUNG-SIM-YK-100-OUT",
    productName: "迎客之星100外开系统窗",
    widthMm: 1200,
    heightMm: 1500,
    opening: "right_out",
    openPlane: "out",
    panelCount: 1,
    hardwareSetId: "HW-TURN-STD",
    hasFixedMeetingMullion: false
  },
  {
    templateId: "ZCSUNG-SIM-YP-95S-DOUBLE-IN",
    productName: "玉屏95S双内开系统窗",
    widthMm: 1600,
    heightMm: 1500,
    opening: "right_in",
    openPlane: "in",
    panelCount: 2,
    hardwareSetId: "HW-TT-STD",
    hasFixedMeetingMullion: true
  }
];

/** Reads the existing reference manufacturing catalogue used by migration QA. */
function referenceManufacturingCatalog() {
  const source = parseLegacyV2Design(JSON.parse(readFileSync(
    resolve("tests/fixtures/legacy-v2/rectangular-fixed.input.json"),
    "utf8"
  )) as unknown);
  return adaptLegacyManufacturingCatalog(source.catalog);
}

describe.each(Z0_CASES)("ZCSUNG Z0 drawing acceptance: $templateId", (fixture) => {
  it("keeps 2D, plan view, 3D, appearance and preview BOM on one formal object graph", () => {
    const session = new DesignSession(createEmptyDesign(`DESIGN-${fixture.templateId}`));
    const windowId = `WIN-${fixture.templateId}`;
    const plan = planZcsungSimulationWindowCreation({
      templateId: fixture.templateId,
      commandIdPrefix: `CMD-${fixture.templateId}`,
      windowId
    });
    session.executeTransaction(plan.commands, `TX-${fixture.templateId}`);

    const window = session.document.windows[0];
    if (!window) throw new Error(`${fixture.templateId} did not create a window.`);
    const cell = window.layout.cells[0];
    if (!cell || cell.type === "fixed_glass") {
      throw new Error(`${fixture.templateId} did not create an opening cell.`);
    }
    expect(window).toMatchObject({
      widthMm: fixture.widthMm,
      heightMm: fixture.heightMm,
      profileSystemId: "AL70",
      defaultHardwareSetId: fixture.hardwareSetId,
      productTemplateSelection: {
        templateId: fixture.templateId,
        publicProductName: fixture.productName,
        productionReady: false
      }
    });
    expect(cell).toMatchObject({
      opening: fixture.opening,
      hardwareSetId: fixture.hardwareSetId,
      openingAssembly: {
        panelCount: fixture.panelCount,
        openPlane: fixture.openPlane
      }
    });

    const previewProgress = Object.fromEntries(
      cell.openingAssembly.panels.map((panel) => [
        createOpeningPanelKey(cell.objectId, panel.id),
        50
      ])
    );
    const svg = renderDesignSvg(session.document, {
      selectedObjectId: window.objectId,
      showDimensions: true,
      showPlanView: true,
      showOpeningState: true,
      openingProgressPercentByPanelKey: previewProgress
    });
    expect(svg).toContain(`data-window-id="${windowId}"`);
    expect(svg).toContain(`data-opening="${fixture.opening}"`);
    expect(svg).toContain(`data-open-plane="${fixture.openPlane}"`);
    expect(svg).toContain(`data-opening-angle-deg="45"`);
    expect(svg).toContain('data-dimension-kind="width"');
    expect(svg).toContain('data-dimension-kind="height"');
    expect(svg).toContain("室外 ↑");
    expect(svg).toContain("室内 ↓");
    expect(svg).not.toMatch(/>\s*(?:主动扇|从动扇)\s*</);
    expect(svg.includes('data-meeting-kind="fixed-mullion"')).toBe(
      fixture.hasFixedMeetingMullion
    );

    const scene = new ThreeDesignSceneBuilder().build(session.document, {
      openingProgressPercentByPanelKey: previewProgress
    });
    const frameSegments: unknown[] = [];
    const openingPanels: Array<{ openPlane?: string; previewAngleDegrees?: number }> = [];
    const fixedMeetingMullions: unknown[] = [];
    scene.traverse((object) => {
      if (object.userData.objectType === "frame-segment") frameSegments.push(object);
      if (
        object.userData.objectType === "opening-panel" &&
        object.userData.previewPanelKey
      ) openingPanels.push(object.userData);
      if (object.userData.objectType === "fixed-mullion") fixedMeetingMullions.push(object);
    });
    expect(frameSegments).toHaveLength(4);
    expect(openingPanels).toHaveLength(fixture.panelCount);
    expect(openingPanels.every((panel) => panel.openPlane === fixture.openPlane)).toBe(true);
    expect(openingPanels.every((panel) => panel.previewAngleDegrees === 45)).toBe(true);
    expect(fixedMeetingMullions.length > 0).toBe(fixture.hasFixedMeetingMullion);

    const appearance = resolveWindowVisualConfigurationForRender(window);
    expect(appearance.schemaVersion).toBe("doormes-visual-config.v1");
    expect(appearance.appearance.frame).toMatchObject({
      inside: expect.objectContaining({ materialFamily: "metal" }),
      outside: expect.objectContaining({ materialFamily: "metal" })
    });
    expect(appearance.appearance.glass.materialFamily).toBe("glass");
    expect(appearance.hardwareModels.every(
      (assignment) => assignment.model.productionStatus === "preview-only"
    )).toBe(true);

    const bom = calculateFormalBom(session.document, referenceManufacturingCatalog());
    expect(bom.mbom.lines.length).toBeGreaterThan(0);
    expect(bom.diagnostics).toContainEqual(expect.objectContaining({
      code: "PRODUCT_TEMPLATE_NOT_PRODUCTION_APPROVED",
      sourceWindowId: window.objectId,
      blocksConfirmation: true
    }));
    expect(bom.confirmation.allowed).toBe(false);
  });
});
