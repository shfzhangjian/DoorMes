import { readFileSync } from "node:fs";
import { resolve } from "node:path";
import { describe, expect, it } from "vitest";
import {
  createRectangularWindowCommand,
  createSetWindowCellOpeningCommand,
  DesignSession
} from "@doormes/application";
import {
  adaptLegacyManufacturingCatalog,
  calculateFormalBom
} from "@doormes/calculation-engine";
import { createEmptyDesign } from "@doormes/domain";
import {
  OpeningPreviewController,
  OpeningPreviewStore
} from "@doormes/interaction-core";
import { migrateLegacyV2ToDomain, parseLegacyV2Design } from "@doormes/persistence";

/** Reads one frozen prototype-compatible fixture for cross-layer isolation tests. */
function readFixture(name: string): unknown {
  return JSON.parse(
    readFileSync(resolve(`tests/fixtures/legacy-v2/${name}`), "utf8")
  ) as unknown;
}

describe("opening preview manufacturing isolation", () => {
  it("does not change design JSON, revision or formal BOM while leaves move", () => {
    const source = parseLegacyV2Design(
      readFixture("double-tilt-turn-flying-mullion.input.json")
    );
    const migration = migrateLegacyV2ToDomain(source);
    const catalog = adaptLegacyManufacturingCatalog(source.catalog);
    const designBefore = JSON.stringify(migration.document);
    const revisionBefore = migration.document.revision;
    const bomBefore = calculateFormalBom(migration.document, catalog);
    const preview = new OpeningPreviewStore(migration.document);

    preview.selectOnly("CELL-TT-DOUBLE-001::P1");
    preview.setSelectedProgress(15);
    preview.toggleSelected("CELL-TT-DOUBLE-001::P2");
    preview.closeSelected();
    preview.openAll();

    expect(JSON.stringify(migration.document)).toBe(designBefore);
    expect(migration.document.revision).toBe(revisionBefore);
    expect(calculateFormalBom(migration.document, catalog)).toEqual(bomBefore);
  });

  /**
   * Runs the same manufacturing-boundary assertion for every migrated hinged
   * mechanism that is allowed through the current compatibility gate.
   *
   * Algorithm: load the frozen prototype document and catalog, capture the
   * formal BOM, then drive the runtime-only controller through a precise 50%
   * frame, pause/resume, 100% completion and a full return-to-zero cycle. The
   * document JSON, revision and calculated BOM must remain byte-for-byte/deep
   * equal at both the middle and final checkpoints.
   *
   * @example The single tilt-turn fixture switches to `tilt` mode before the
   * 50% frame; that changes SVG/Three pose but cannot change sash material.
   * @since 0.8.3
   * @modified 2026-09-17 - Added MOT-008 cross-mechanism BOM isolation gate.
   */
  it.each([
    ["single-tilt-turn.input.json", true],
    ["double-tilt-turn-flying-mullion.input.json", true],
    ["top-hung.input.json", false]
  ] as const)(
    "keeps %s design and BOM stable at 50%%, pause/resume and cycle completion",
    (fixtureName, supportsTilt) => {
      const source = parseLegacyV2Design(readFixture(fixtureName));
      const migration = migrateLegacyV2ToDomain(source);
      const catalog = adaptLegacyManufacturingCatalog(source.catalog);
      const designBefore = JSON.stringify(migration.document);
      const revisionBefore = migration.document.revision;
      const bomBefore = calculateFormalBom(migration.document, catalog);
      const preview = new OpeningPreviewStore(migration.document);
      const controller = new OpeningPreviewController(preview, 200);
      const firstPanel = [...preview.panelDefinitions].sort(
        (left, right) => left.operationOrder - right.operationOrder
      )[0];
      if (!firstPanel) throw new Error(`Fixture ${fixtureName} has no preview panel.`);

      preview.selectOnly(firstPanel.key);
      if (supportsTilt) preview.setSelectedMotionMode("tilt");
      preview.closeAll();
      controller.openSelected();
      controller.advance(100);
      controller.pause();

      expect(preview.state.panelProgressPercent[firstPanel.key]).toBe(50);
      expect(preview.state.playback).toBe("paused");
      expect(JSON.stringify(migration.document)).toBe(designBefore);
      expect(migration.document.revision).toBe(revisionBefore);
      expect(calculateFormalBom(migration.document, catalog)).toEqual(bomBefore);

      controller.resume();
      controller.advance(500);
      controller.playSelectedCycle();
      controller.advance(2_000);

      expect(preview.state.playback).toBe("idle");
      expect(preview.state.panelProgressPercent[firstPanel.key]).toBe(0);
      expect(JSON.stringify(migration.document)).toBe(designBefore);
      expect(migration.document.revision).toBe(revisionBefore);
      expect(calculateFormalBom(migration.document, catalog)).toEqual(bomBefore);
    }
  );

  /**
   * Covers the audited fixed-mullion rule separately from legacy fixture
   * parity because the prototype omitted its centre-profile BOM line.
   *
   * Algorithm: build the corrected formal double-leaf design, reuse the frozen
   * compatible material catalog, advance both independent leaves to exactly
   * 50% in one parallel phase, then assert that preview state never leaks into
   * the corrected formal BOM or immutable design document.
   *
   * @example Both P1 and P2 equal 50 after half of a 200ms phase while the
   * stationary centre mullion remains a calculated frame-owned workpiece.
   * @since 0.8.4
   * @modified 2026-09-17 - Added fixed-mullion MOT-008 BOM isolation coverage.
   */
  it("keeps the corrected fixed-mullion BOM stable at the parallel 50% frame", () => {
    const source = parseLegacyV2Design(
      readFixture("double-tilt-turn-flying-mullion.input.json")
    );
    const catalog = adaptLegacyManufacturingCatalog(source.catalog);
    const session = new DesignSession(createEmptyDesign("DESIGN-FIXED-MOTION-ISOLATION"));
    session.execute(
      createRectangularWindowCommand({
        commandId: "CREATE-FIXED-MOTION-ISOLATION",
        windowId: "WIN-FIXED-MOTION-ISOLATION",
        mark: "C-FIXED-MOTION",
        widthMm: 1600,
        heightMm: 1500,
        cellId: "CELL-FIXED-MOTION-ISOLATION"
      })
    );
    session.execute(
      createSetWindowCellOpeningCommand({
        commandId: "SET-FIXED-MOTION-ISOLATION",
        windowId: "WIN-FIXED-MOTION-ISOLATION",
        cellId: "CELL-FIXED-MOTION-ISOLATION",
        cellType: "turn_tilt",
        opening: "right_in",
        panelCount: 2,
        mullionMode: "fixed_mullion"
      })
    );
    const designBefore = JSON.stringify(session.document);
    const revisionBefore = session.document.revision;
    const bomBefore = calculateFormalBom(session.document, catalog);
    const preview = new OpeningPreviewStore(session.document);
    const controller = new OpeningPreviewController(preview, 200);

    preview.closeAll();
    controller.openAll();
    controller.advance(100);
    controller.pause();

    expect(preview.state.panelProgressPercent).toEqual({
      "CELL-FIXED-MOTION-ISOLATION::P1": 50,
      "CELL-FIXED-MOTION-ISOLATION::P2": 50
    });
    expect(JSON.stringify(session.document)).toBe(designBefore);
    expect(session.document.revision).toBe(revisionBefore);
    expect(calculateFormalBom(session.document, catalog)).toEqual(bomBefore);

    controller.resume();
    controller.advance(100);
    expect(preview.state.panelProgressPercent).toEqual({
      "CELL-FIXED-MOTION-ISOLATION::P1": 100,
      "CELL-FIXED-MOTION-ISOLATION::P2": 100
    });
    expect(calculateFormalBom(session.document, catalog)).toEqual(bomBefore);
  });
});
