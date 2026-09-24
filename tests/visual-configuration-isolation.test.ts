import { readFileSync } from "node:fs";
import { resolve } from "node:path";
import { describe, expect, it } from "vitest";
import {
  createUpdateWindowGlassCatalogSelectionCommand,
  createUpdateWindowInstallationCommand,
  createUpdateWindowSurroundCatalogSelectionCommand,
  createUpdateWindowVisualConfigurationCommand,
  DesignSession
} from "@doormes/application";
import {
  applyAppearanceCatalogPreset,
  resolveGlassBusinessCatalogSelection,
  resolveSurroundBusinessCatalogSelection
} from "@doormes/appearance-model";
import {
  adaptLegacyManufacturingCatalog,
  calculateFormalBom,
  FormalBomCalculationStore
} from "@doormes/calculation-engine";
import type { WindowVisualConfiguration } from "@doormes/contracts";
import { migrateLegacyV2ToDomain, parseLegacyV2Design } from "@doormes/persistence";

/**
 * Loads the frozen single-opening fixture used to prove visual/BOM isolation.
 *
 * @returns Parsed, still-untrusted JSON ready for the legacy schema adapter.
 * @since 0.10.2
 * @modified 2026-09-17 - Added APPEAR-001 manufacturing-boundary coverage.
 */
function readFixture(): unknown {
  return JSON.parse(readFileSync(resolve(
    "tests/fixtures/legacy-v2/single-tilt-turn.input.json"
  ), "utf8")) as unknown;
}

describe("visual configuration manufacturing isolation", () => {
  it("migrates defaults, updates atomically, preserves BOM and supports undo/redo", () => {
    const source = parseLegacyV2Design(readFixture());
    const migration = migrateLegacyV2ToDomain(source);
    const catalog = adaptLegacyManufacturingCatalog(source.catalog);
    const session = new DesignSession(migration.document);
    const window = session.document.windows[0]!;
    const original = window.visualConfiguration!;
    const bomBefore = calculateFormalBom(session.document, catalog);
    const updated = {
      ...original,
      appearance: {
        ...original.appearance,
        wall: {
          ...original.appearance.wall,
          appearanceId: "customer.wall.warm-white",
          baseColor: "#f3eee5",
          textureSetId: "TEXTURE-PLASTER-FINE-01"
        }
      }
    } satisfies WindowVisualConfiguration;

    session.execute(createUpdateWindowVisualConfigurationCommand({
      commandId: "UPDATE-VISUAL-001",
      windowId: window.objectId,
      visualConfiguration: updated
    }));

    expect(session.document.windows[0]?.visualConfiguration?.appearance.wall.baseColor)
      .toBe("#f3eee5");
    expect(calculateFormalBom(session.document, catalog)).toEqual(bomBefore);
    expect(session.undo()).toBe(true);
    expect(session.document.windows[0]?.visualConfiguration).toEqual(original);
    expect(session.redo()).toBe(true);
    expect(session.document.windows[0]?.visualConfiguration?.appearance.wall.textureSetId)
      .toBe("TEXTURE-PLASTER-FINE-01");
    expect(calculateFormalBom(session.document, catalog)).toEqual(bomBefore);
  });

  it("emits a reviewed frame-face process without mutating profile BOM", () => {
    const source = parseLegacyV2Design(readFixture());
    const migration = migrateLegacyV2ToDomain(source);
    const catalog = adaptLegacyManufacturingCatalog(source.catalog);
    const session = new DesignSession(migration.document);
    const window = session.document.windows[0]!;
    const original = window.visualConfiguration!;
    const before = calculateFormalBom(session.document, catalog);
    const reviewed = applyAppearanceCatalogPreset(
      original,
      "frame.outside",
      "FINISH-POWDER-RAL7016-V1"
    );

    session.execute(createUpdateWindowVisualConfigurationCommand({
      commandId: "UPDATE-SURFACE-PRODUCTION-001",
      windowId: window.objectId,
      visualConfiguration: reviewed
    }));
    const after = calculateFormalBom(session.document, catalog);
    const surface = after.processFeatures.find((feature) =>
      feature.kind === "surface-treatment" &&
      feature.surfaceRole === "frame" &&
      feature.face === "outside"
    );
    const expectedLengthMm = before.features.reduce((total, feature) =>
      feature.kind === "profile-cut" && feature.sourceComponentId.startsWith("frame.")
        ? total + feature.grossLengthMm * feature.quantity
        : total, 0);

    expect(after.features).toEqual(before.features);
    expect(after.mbom).toEqual(before.mbom);
    expect(after.summary).toEqual(before.summary);
    expect(surface).toMatchObject({
      treatmentCode: "ST-POWDER-RAL7016",
      processTemplateId: "PROC-POWDER-AL-V1",
      appearanceId: "catalog.finish.powder.ral7016",
      applicationLengthMm: expectedLengthMm,
      quantityMetres: Math.round(expectedLengthMm / 10) / 100,
      status: "ready"
    });
    expect(surface?.sourceFeatureIds).toHaveLength(4);
    expect(after.processFeatures).toHaveLength(before.processFeatures.length + 1);
  });

  it("maps an exact glass business version to rendering, BOM and stale state", () => {
    const source = parseLegacyV2Design(readFixture());
    const migration = migrateLegacyV2ToDomain(source);
    const catalog = adaptLegacyManufacturingCatalog(source.catalog);
    const session = new DesignSession(migration.document);
    const window = session.document.windows[0]!;
    const store = new FormalBomCalculationStore(session.document, catalog);
    store.recalculate(session.document, catalog);

    session.execute(createUpdateWindowGlassCatalogSelectionCommand({
      commandId: "SELECT-GLASS-TEMP-27",
      windowId: window.objectId,
      selection: resolveGlassBusinessCatalogSelection("GL-TEMP-27", "1.0.0", "AL70")
    }));

    expect(store.synchronize(session.document, catalog).status).toBe("stale");
    expect(session.document.windows[0]).toMatchObject({
      defaultGlassTypeId: "GL-TEMP-27",
      defaultGlassSelection: {
        catalogVersion: "1.0.0",
        materialCode: "GL-TEMP-27",
        specification: "6+15A+6 钢化中空"
      },
      visualConfiguration: {
        appearance: {
          glass: { appearanceId: "catalog.glass.tempered.aqua" }
        }
      }
    });
    const result = calculateFormalBom(session.document, {
      ...catalog,
      // Historical designs calculate from their frozen selection even if the
      // live query no longer returns that exact version.
      glassTypes: []
    });
    expect(result.features.filter((feature) => feature.kind === "glass-panel"))
      .toEqual(expect.arrayContaining([
        expect.objectContaining({
          materialCode: "GL-TEMP-27",
          name: "钢化中空玻璃",
          thicknessMm: 27,
          catalogItemId: "GL-TEMP-27",
          catalogVersion: "1.0.0",
          specification: "6+15A+6 钢化中空"
        })
      ]));
    expect(result.mbom.lines.filter((line) => line.category === "glass"))
      .toEqual(expect.arrayContaining([
        expect.objectContaining({
          materialCode: "GL-TEMP-27",
          name: "钢化中空玻璃",
          spec: expect.stringMatching(/x27$/)
        })
      ]));
    expect(session.undo()).toBe(true);
    expect(session.document.windows[0]?.defaultGlassSelection).toBeUndefined();
    expect(store.synchronize(session.document, catalog).status).toBe("current");
  });

  it("maps one surround package to 2D/3D appearance, material, process and stale state", () => {
    const source = parseLegacyV2Design(readFixture());
    const migration = migrateLegacyV2ToDomain(source);
    const catalog = adaptLegacyManufacturingCatalog(source.catalog);
    const session = new DesignSession(migration.document);
    const window = session.document.windows[0]!;
    const installation = window.installation!;
    session.execute(createUpdateWindowInstallationCommand({
      commandId: "ENABLE-SURROUND",
      windowId: window.objectId,
      installation: {
        ...installation,
        surround: {
          ...installation.surround,
          enabled: true,
          styleId: "both_sides",
          edgeMode: "all",
          sides: ["top", "right", "bottom", "left"]
        }
      }
    }));
    const store = new FormalBomCalculationStore(session.document, catalog);
    store.recalculate(session.document, catalog);

    session.execute(createUpdateWindowSurroundCatalogSelectionCommand({
      commandId: "SELECT-SURROUND-STONE",
      windowId: window.objectId,
      selection: resolveSurroundBusinessCatalogSelection(
        "SUR-STONE-GRAY-18",
        "1.0.0",
        "AL70"
      )
    }));

    expect(store.synchronize(session.document, catalog).status).toBe("stale");
    expect(session.document.windows[0]).toMatchObject({
      installationSurroundSelection: {
        catalogItemId: "SUR-STONE-GRAY-18",
        catalogVersion: "1.0.0"
      },
      visualConfiguration: {
        appearance: {
          surroundOutside: { appearanceId: "catalog.surround.stone.gray.outside" },
          surroundLiner: { appearanceId: "catalog.surround.composite.gray.liner" }
        }
      }
    });
    const result = calculateFormalBom(session.document, catalog);
    expect(result.features).toEqual(expect.arrayContaining([
      expect.objectContaining({
        sourceComponentId: "installation.surround.outside.top",
        materialCode: "SURROUND-STONE-GRAY-18",
        processTemplateId: "PROC-SURROUND-STONE-CUT-V1"
      }),
      expect.objectContaining({
        sourceComponentId: "installation.surround.liner.top",
        materialCode: "LINER-COMPOSITE-GRAY-18",
        processTemplateId: "PROC-SURROUND-COMPOSITE-LINER-CUT-V1"
      }),
      expect.objectContaining({
        sourceComponentId: "installation.surround.seal",
        materialCode: "SEAL-STONE-NEUTRAL-GRAY",
        processTemplateId: "PROC-SURROUND-STONE-SEAL-V1"
      })
    ]));
    expect(result.mbom.lines).toEqual(expect.arrayContaining([
      expect.objectContaining({
        materialCode: "SURROUND-STONE-GRAY-18",
        processTemplateId: "PROC-SURROUND-STONE-CUT-V1"
      }),
      expect.objectContaining({
        materialCode: "LINER-COMPOSITE-GRAY-18",
        processTemplateId: "PROC-SURROUND-COMPOSITE-LINER-CUT-V1"
      })
    ]));
    expect(session.undo()).toBe(true);
    expect(store.synchronize(session.document, catalog).status).toBe("current");
  });

  it("keeps pure display edits current and marks reviewed production changes stale", () => {
    const source = parseLegacyV2Design(readFixture());
    const migration = migrateLegacyV2ToDomain(source);
    const catalog = adaptLegacyManufacturingCatalog(source.catalog);
    const session = new DesignSession(migration.document);
    const store = new FormalBomCalculationStore(session.document, catalog);
    const statuses: string[] = [];
    store.subscribe(({ status }) => statuses.push(status));
    const calculated = store.recalculate(session.document, catalog);
    const originalFingerprint = calculated.calculatedInputFingerprint;
    const window = session.document.windows[0]!;
    const original = window.visualConfiguration!;

    session.execute(createUpdateWindowVisualConfigurationCommand({
      commandId: "UPDATE-PURE-DISPLAY-001",
      windowId: window.objectId,
      visualConfiguration: {
        ...original,
        appearance: {
          ...original.appearance,
          frame: {
            ...original.appearance.frame,
            outside: {
              ...original.appearance.frame.outside,
              appearanceId: "customer.frame.display-only",
              baseColor: "#efe8dc",
              roughness: 0.73,
              finishCode: "CUSTOM-DISPLAY-LABEL"
            }
          }
        }
      }
    }));
    expect(store.synchronize(session.document, catalog)).toMatchObject({
      status: "current",
      calculatedInputFingerprint: originalFingerprint
    });

    const displayOnly = session.document.windows[0]!.visualConfiguration!;
    session.execute(createUpdateWindowVisualConfigurationCommand({
      commandId: "UPDATE-REVIEWED-FINISH-001",
      windowId: window.objectId,
      visualConfiguration: applyAppearanceCatalogPreset(
        displayOnly,
        "frame.outside",
        "FINISH-POWDER-RAL9016-V1"
      )
    }));
    expect(store.synchronize(session.document, catalog).status).toBe("stale");
    expect(store.state.result).toBe(calculated.result);
    expect(session.undo()).toBe(true);
    expect(store.synchronize(session.document, catalog).status).toBe("current");
    expect(session.redo()).toBe(true);
    expect(store.synchronize(session.document, catalog).status).toBe("stale");
    expect(store.recalculate(session.document, catalog)).toMatchObject({
      status: "current",
      calculatedDocumentRevision: session.document.revision
    });
    expect(store.state.result?.processFeatures.some((feature) =>
      feature.kind === "surface-treatment")).toBe(true);
    expect(statuses).toEqual([
      "not-calculated",
      "current",
      "current",
      "stale",
      "current",
      "stale",
      "current"
    ]);
  });
});
