import { readFileSync } from "node:fs";
import { resolve } from "node:path";
import { describe, expect, it } from "vitest";
import {
  createFabricationAssemblyCommand,
  createDrawingTextLabelCommand,
  createRectangularWindowCommand,
  DesignSession,
  requireEngineeringJointCatalogSelection
} from "@doormes/application";
import { createEmptyDesign, toDesignObjectId } from "@doormes/domain";
import { REFERENCE_WINDOW_INSTALLATION } from "@doormes/geometry-topology";
import {
  LegacyDesignValidationError,
  migrateLegacyV2ToDomain,
  migrateLegacyLocalStorageDesign,
  parseLegacyV2Design
} from "./index";
import {
  LocalDesignSnapshotRepository,
  LocalDesignSnapshotValidationError,
  parseFormalDesignSnapshotText,
  parseFormalDesignDocument,
  serializeFormalDesignSnapshot,
  type DesignSnapshotKeyValueStorage
} from "./local-design";
import {
  applyHardwareModelCatalogPreset,
  replaceWindowHardwareInstanceModel,
  resolveGlassBusinessCatalogSelection,
  resolveSurroundBusinessCatalogSelection
} from "@doormes/appearance-model";

/** In-memory Storage-compatible adapter used to prove browser-independent semantics. */
class MemoryKeyValueStorage implements DesignSnapshotKeyValueStorage {
  readonly values = new Map<string, string>();

  getItem(key: string): string | null {
    return this.values.get(key) ?? null;
  }

  setItem(key: string, value: string): void {
    this.values.set(key, value);
  }

  removeItem(key: string): void {
    this.values.delete(key);
  }
}

describe("prototype v2 compatibility", () => {
  it("validates and explicitly reports partial rectangular-window migration", () => {
    const fixture = JSON.parse(
      readFileSync(resolve("tests/fixtures/legacy-v2/rectangular-fixed.input.json"), "utf8")
    ) as unknown;
    const source = parseLegacyV2Design(fixture);
    const result = migrateLegacyV2ToDomain(source);

    expect(result.status).toBe("partial");
    expect(result.document.windows).toHaveLength(1);
    expect(result.document.windows[0]).toMatchObject({
      objectId: "W-RECT-001",
      mark: "C1",
      widthMm: 1200,
      heightMm: 1500,
      sectionDimensions: {
        presetId: "AL70-REFERENCE-V1",
        frameDepthMm: 70,
        sashDepthMm: 55,
        sashFrontSetbackMm: 2
      },
      installation: {
        sillHeightMm: 0,
        surround: {
          mountingMode: "opening",
          frameAlignment: "center",
          wallThicknessMm: 200
        }
      }
    });
    expect(result.issues.map((issue) => issue.code)).toContain(
      "LEGACY_CATALOG_NOT_MIGRATED"
    );
  });

  it("preserves validated prototype installation geometry during migration", () => {
    const fixture = JSON.parse(
      readFileSync(resolve("tests/fixtures/legacy-v2/rectangular-fixed.input.json"), "utf8")
    ) as { windows: Array<Record<string, unknown>> };
    fixture.windows[0]!.installation = {
      sillHeightMm: 880,
      surround: {
        enabled: true,
        mountingMode: "opening",
        frameAlignment: "custom",
        styleId: "both_sides",
        edgeMode: "three_without_bottom",
        sides: ["top", "right", "bottom", "left"],
        wallThicknessMm: 280,
        wallMaterialId: "red_brick",
        wallCornerMode: "structural_pier",
        cornerPierWidthMm: 240,
        frameOffsetMm: -35,
        exteriorMountGapMm: 0,
        outsideWidthMm: 80,
        insideWidthMm: 80,
        boardThicknessMm: 18,
        materialCode: "SURROUND-AL-02",
        colorOutside: "RAL7016",
        colorInside: "RAL9016",
        note: "migration fixture"
      }
    };
    const result = migrateLegacyV2ToDomain(parseLegacyV2Design(fixture));

    expect(result.document.windows[0]?.installation).toMatchObject({
      sillHeightMm: 880,
      surround: {
        frameAlignment: "custom",
        sides: ["top", "right", "left"],
        wallThicknessMm: 280,
        wallMaterialId: "red_brick",
        frameOffsetMm: -35
      }
    });
  });

  it("expands a schema-valid partial legacy installation before strict domain validation", () => {
    const fixture = JSON.parse(
      readFileSync(resolve("tests/fixtures/legacy-v2/rectangular-fixed.input.json"), "utf8")
    ) as { windows: Array<Record<string, unknown>> };
    fixture.windows[0]!.installation = {};

    const result = migrateLegacyV2ToDomain(parseLegacyV2Design(fixture));
    expect(result.document.windows[0]?.installation).toMatchObject({
      sillHeightMm: 0,
      surround: {
        mountingMode: "opening",
        frameAlignment: "center",
        wallThicknessMm: 200
      }
    });
  });

  it("rejects malformed input before migration", () => {
    expect(() => parseLegacyV2Design({ schemaVersion: "cn-door-window-design.v2" })).toThrow(
      LegacyDesignValidationError
    );
  });

  it("blocks unsupported opening cells instead of silently converting them to fixed glass", () => {
    const fixture = JSON.parse(
      readFileSync(resolve("tests/fixtures/legacy-v2/rectangular-fixed.input.json"), "utf8")
    ) as { windows: Array<{ layout: { cells: Array<{ type: string; opening: string }> } }> };
    fixture.windows[0]!.layout.cells[0] = {
      ...fixture.windows[0]!.layout.cells[0]!,
      type: "turn",
      opening: "left_in"
    };
    const result = migrateLegacyV2ToDomain(parseLegacyV2Design(fixture));

    expect(result.status).toBe("blocked");
    expect(result.document.windows).toHaveLength(0);
    expect(result.issues.map((issue) => issue.code)).toContain("LEGACY_CELL_TYPE_NOT_MIGRATED");
  });

  it("migrates the frozen tilt-turn assembly without flattening it to fixed glass", () => {
    const fixture = JSON.parse(
      readFileSync(resolve("tests/fixtures/legacy-v2/single-tilt-turn.input.json"), "utf8")
    ) as unknown;
    const result = migrateLegacyV2ToDomain(parseLegacyV2Design(fixture));
    const cell = result.document.windows[0]?.layout.cells[0];

    expect(result.status).toBe("partial");
    expect(cell).toMatchObject({
      objectId: "CELL-TT-001",
      type: "turn_tilt",
      opening: "right_in",
      hardwareSetId: "HW-TT-STD",
      openingAssembly: {
        mechanism: "tilt_turn",
        primarySide: "right",
        operationSequence: ["P1"]
      }
    });
    expect(result.issues.map((issue) => issue.code)).not.toContain(
      "LEGACY_CELL_TYPE_NOT_MIGRATED"
    );
  });

  it("migrates prototype top-hung data to an explicit top-edge hinge", () => {
    const fixture = JSON.parse(
      readFileSync(resolve("tests/fixtures/legacy-v2/top-hung.input.json"), "utf8")
    ) as unknown;
    const result = migrateLegacyV2ToDomain(parseLegacyV2Design(fixture));
    const cell = result.document.windows[0]?.layout.cells[0];

    expect(result.status).toBe("partial");
    expect(cell).toMatchObject({
      objectId: "CELL-TOP-HUNG-001",
      type: "top_hung",
      opening: "top_out",
      hardwareSetId: "HW-HUNG-STD",
      openingAssembly: {
        mechanism: "top_hung",
        openPlane: "out",
        panels: [{ id: "P1", hingeEdge: "top", operationOrder: 0 }],
        operationSequence: ["P1"]
      }
    });
    expect(result.issues.map((issue) => issue.code)).not.toContain(
      "LEGACY_TOP_HUNG_ASSEMBLY_NOT_MIGRATED"
    );
  });

  it("migrates the frozen double sash with physical panel order and flying ownership", () => {
    const fixture = JSON.parse(
      readFileSync(
        resolve("tests/fixtures/legacy-v2/double-tilt-turn-flying-mullion.input.json"),
        "utf8"
      )
    ) as unknown;
    const result = migrateLegacyV2ToDomain(parseLegacyV2Design(fixture));
    const cell = result.document.windows[0]?.layout.cells[0];

    expect(result.status).toBe("partial");
    expect(cell).toMatchObject({
      objectId: "CELL-TT-DOUBLE-001",
      type: "turn_tilt",
      opening: "right_in",
      openingAssembly: {
        panelCount: 2,
        activePanelCount: 2,
        primarySide: "right",
        mullionMode: "flying_mullion",
        panels: [
          { id: "P1", role: "secondary", hingeSide: "left", operationOrder: 1 },
          { id: "P2", role: "primary", hingeSide: "right", operationOrder: 0 }
        ],
        operationSequence: ["P2", "P1"]
      }
    });
    expect(result.issues.map((issue) => issue.code)).not.toContain(
      "LEGACY_TILT_TURN_ASSEMBLY_NOT_MIGRATED"
    );
  });

  it("blocks a double sash whose panel roles disagree with its primary side", () => {
    const fixture = JSON.parse(
      readFileSync(
        resolve("tests/fixtures/legacy-v2/double-tilt-turn-flying-mullion.input.json"),
        "utf8"
      )
    ) as {
      windows: Array<{
        layout: { cells: Array<{ openingAssembly: { panels: Array<{ role: string }> } }> };
      }>;
    };
    fixture.windows[0]!.layout.cells[0]!.openingAssembly.panels[0]!.role = "primary";
    const result = migrateLegacyV2ToDomain(parseLegacyV2Design(fixture));

    expect(result.status).toBe("blocked");
    expect(result.document.windows).toHaveLength(0);
    expect(result.issues.map((issue) => issue.code)).toContain(
      "LEGACY_TILT_TURN_ASSEMBLY_NOT_MIGRATED"
    );
  });
});

describe("formal local design snapshots", () => {
  it("round-trips user-authored drawing labels without generating system prose", () => {
    const session = new DesignSession(createEmptyDesign("DESIGN-PERSIST-LABEL"));
    session.execute(createRectangularWindowCommand({
      commandId: "PERSIST-LABEL-WINDOW",
      windowId: "PERSIST-LABEL-W1",
      mark: "C1",
      widthMm: 1200,
      heightMm: 1500
    }));
    session.execute(createDrawingTextLabelCommand({
      commandId: "PERSIST-LABEL-CREATE",
      label: {
        kind: "drawing-text-label",
        objectId: toDesignObjectId("PERSIST-LABEL-1"),
        ownerObjectId: toDesignObjectId("PERSIST-LABEL-W1"),
        text: "洞口尺寸以复测为准",
        xMm: 110,
        yMm: 90,
        view: "both",
        fontSizePaperMm: 4,
        color: "#334155",
        rotationDeg: -8,
        align: "left",
        printVisible: true
      }
    }));

    const serialized = serializeFormalDesignSnapshot(
      session.document,
      "2026-09-24T10:00:00.000Z"
    );
    const restored = parseFormalDesignSnapshotText(serialized);

    expect(restored.document.drawingTextLabels).toEqual(session.document.drawingTextLabels);
    expect(serialized).toContain("洞口尺寸以复测为准");
    expect(serialized).not.toContain("墙洞安装");
  });

  it("round-trips connected fabrication assemblies after restoring their windows", () => {
    const session = new DesignSession(createEmptyDesign("DESIGN-PERSIST-ASSEMBLY"));
    const jointCatalogSelection = requireEngineeringJointCatalogSelection(
      "DM-JOINT-MUL-30",
      "2026.09-r1"
    );
    session.execute(createRectangularWindowCommand({
      commandId: "PERSIST-ASSEMBLY-W1",
      windowId: "PERSIST-W1",
      mark: "P1",
      widthMm: 1000,
      heightMm: 1400
    }));
    session.execute(createRectangularWindowCommand({
      commandId: "PERSIST-ASSEMBLY-W2",
      windowId: "PERSIST-W2",
      mark: "P2",
      widthMm: 800,
      heightMm: 1400
    }));
    session.execute(createFabricationAssemblyCommand({
      commandId: "PERSIST-ASSEMBLY-CREATE",
      assemblyId: "PERSIST-ASSEMBLY",
      mark: "PA-1",
      instances: [
        { objectId: "PERSIST-ASSEMBLY:I1", windowId: "PERSIST-W1",
          transform: { xMm: 0, yMm: 0, zMm: 0, rotationYDeg: 0 } },
        { objectId: "PERSIST-ASSEMBLY:I2", windowId: "PERSIST-W2",
          transform: { xMm: 1030, yMm: 0, zMm: 0, rotationYDeg: 0 } }
      ],
      joints: [{
        objectId: "PERSIST-ASSEMBLY:J1",
        jointType: "mullion_joint",
        firstInstanceId: "PERSIST-ASSEMBLY:I1",
        firstEdge: "right",
        secondInstanceId: "PERSIST-ASSEMBLY:I2",
        secondEdge: "left",
        gapMm: 30,
        factoryScope: "site",
        catalogSelection: jointCatalogSelection
      }],
      openingClearance: { topMm: 8, rightMm: 10, bottomMm: 8, leftMm: 10 },
      installation: REFERENCE_WINDOW_INSTALLATION
    }));
    const serialized = serializeFormalDesignSnapshot(
      session.document,
      "2026-09-20T09:00:00.000Z"
    );
    const restored = parseFormalDesignSnapshotText(serialized);

    expect(restored.document.revision).toBe(3);
    expect(restored.document.assemblies).toEqual(session.document.assemblies);
    expect(restored.document.assemblies?.[0]?.joints[0]).toMatchObject({
      objectId: "PERSIST-ASSEMBLY:J1",
      gapMm: 30,
      factoryScope: "site",
      catalogSelection: expect.objectContaining({
        catalogItemId: "DM-JOINT-MUL-30",
        catalogVersion: "2026.09-r1"
      })
    });
  });

  it("round-trips exact hardware overrides and all normalized project data", () => {
    const fixture = JSON.parse(
      readFileSync(resolve("tests/fixtures/legacy-v2/single-tilt-turn.input.json"), "utf8")
    ) as unknown;
    const migrated = migrateLegacyV2ToDomain(parseLegacyV2Design(fixture)).document;
    const window = migrated.windows[0]!;
    const visual = window.visualConfiguration!;
    const glassSelection = resolveGlassBusinessCatalogSelection(
      "GL-TEMP-27",
      "1.0.0",
      "AL70"
    );
    const surroundSelection = resolveSurroundBusinessCatalogSelection(
      "SUR-STONE-GRAY-18",
      "1.0.0",
      "AL70"
    );
    const fallback = visual.hardwareModels.find((assignment) =>
      assignment.role === "handle" && assignment.hardwareId === undefined)!.model;
    const document = {
      ...migrated,
      revision: 42,
      windows: [{
        ...window,
        mark: "C1 · 【公开参考模拟】120内开内倒系统窗",
        defaultGlassTypeId: glassSelection.catalogItemId,
        defaultGlassSelection: glassSelection,
        installationSurroundSelection: surroundSelection,
        designComponentRemarks: {
          profile: "型材端部去毛刺",
          glass: "玻璃标签朝室内",
          hardware: "执手随确认样板",
          surround: "石材纹理连续"
        },
        productTemplateSelection: {
          schemaVersion: "doormes-product-template-selection.v1" as const,
          customerId: "ZCSUNG",
          customerName: "智宬轩系统门窗",
          templateId: "ZCSUNG-SIM-LH-120-TT",
          templateVersion: "0.1.0",
          publicProductName: "120内开内倒系统窗",
          sourceStatus: "public-reference" as const,
          engineeringStatus: "simulated-not-for-production" as const,
          productionReady: false as const,
          officialSourceUrls: ["https://www.shzcsung.com/product-200009.html"],
          assumptions: ["中性演示尺寸，不是企业审核参数。"]
        },
        installation: {
          ...window.installation!,
          surround: {
            ...window.installation!.surround,
            boardThicknessMm: surroundSelection.boardThicknessMm,
            materialCode: surroundSelection.trimMaterialCode,
            colorOutside: surroundSelection.outsideAppearance.baseColor,
            colorInside: surroundSelection.insideAppearance.baseColor
          }
        },
        visualConfiguration: replaceWindowHardwareInstanceModel(
          applyHardwareModelCatalogPreset({
            ...visual,
            appearance: {
              ...visual.appearance,
              glass: glassSelection.appearance,
              surroundOutside: surroundSelection.outsideAppearance,
              surroundInside: surroundSelection.insideAppearance,
              surroundLiner: surroundSelection.linerAppearance
            }
          }, "handle", "HANDLE-KNOB-ROUND-V1"),
          "handle",
          "cell.1.1.hardware.handle",
          {
            ...fallback,
            modelId: "customer.persisted.handle",
            mount: {
              mountAxis: "-x",
              pivotRatio: { x: 0.25, y: 0.75, z: 1 }
            },
            productionStatus: "preview-only"
          }
        )
      }]
    };
    const storage = new MemoryKeyValueStorage();
    const repository = new LocalDesignSnapshotRepository(storage, "project:opening");

    repository.save(document, "2026-09-18T08:00:00.000Z");
    const restored = repository.load();

    expect(restored?.savedAtIso).toBe("2026-09-18T08:00:00.000Z");
    expect(restored?.document.revision).toBe(42);
    expect(restored?.document.windows[0]?.mark).toBe("C1");
    expect(restored?.document.windows[0]?.defaultGlassSelection).toMatchObject({
      catalogItemId: "GL-TEMP-27",
      catalogVersion: "1.0.0",
      materialCode: "GL-TEMP-27",
      thicknessMm: 27
    });
    expect(restored?.document.windows[0]?.visualConfiguration?.appearance.glass)
      .toEqual(glassSelection.appearance);
    expect(restored?.document.windows[0]?.installationSurroundSelection).toMatchObject({
      catalogItemId: "SUR-STONE-GRAY-18",
      linerMaterialCode: "LINER-COMPOSITE-GRAY-18",
      trimCutProcessTemplateId: "PROC-SURROUND-STONE-CUT-V1"
    });
    expect(restored?.document.windows[0]?.designComponentRemarks).toEqual({
      profile: "型材端部去毛刺",
      glass: "玻璃标签朝室内",
      hardware: "执手随确认样板",
      surround: "石材纹理连续"
    });
    expect(restored?.document.windows[0]?.productTemplateSelection).toEqual({
      schemaVersion: "doormes-product-template-selection.v1",
      customerId: "ZCSUNG",
      customerName: "智宬轩系统门窗",
      templateId: "ZCSUNG-SIM-LH-120-TT",
      templateVersion: "0.1.0",
      publicProductName: "120内开内倒系统窗",
      sourceStatus: "public-reference",
      engineeringStatus: "simulated-not-for-production",
      productionReady: false,
      officialSourceUrls: ["https://www.shzcsung.com/product-200009.html"],
      assumptions: ["中性演示尺寸，不是企业审核参数。"]
    });
    expect(restored?.document.windows[0]?.visualConfiguration?.appearance.surroundOutside)
      .toEqual(surroundSelection.outsideAppearance);
    expect(restored?.document.windows[0]?.visualConfiguration?.hardwareModels).toContainEqual(
      expect.objectContaining({
        role: "handle",
        hardwareId: "cell.1.1.hardware.handle",
        model: expect.objectContaining({
          modelId: "customer.persisted.handle",
          mount: {
            mountAxis: "-x",
            pivotRatio: { x: 0.25, y: 0.75, z: 1 }
          }
        })
      })
    );
    expect(restored?.document.windows[0]?.visualConfiguration?.hardwareModels).toContainEqual(
      expect.objectContaining({
        role: "handle",
        model: expect.objectContaining({
          catalogItemId: "HANDLE-KNOB-ROUND-V1",
          catalogVersion: "1.0.0",
          businessName: "执手 · 圆形旋钮",
          materialCode: "HW-HANDLE-KNOB-ROUND"
        })
      })
    );
  });

  it("repairs only the broken ZCSUNG 0.1.0 placeholder material mapping", () => {
    const fixture = JSON.parse(
      readFileSync(resolve("tests/fixtures/legacy-v2/single-tilt-turn.input.json"), "utf8")
    ) as unknown;
    const document = migrateLegacyV2ToDomain(parseLegacyV2Design(fixture)).document;
    const sourceWindow = document.windows[0]!;
    const brokenSnapshot = {
      ...document,
      windows: [{
        ...sourceWindow,
        profileSystemId: "DM-ZCSUNG-PUBLIC-SIM",
        defaultHardwareSetId: "HW-ZCSUNG-PUBLIC-SIM-TT",
        productTemplateSelection: {
          schemaVersion: "doormes-product-template-selection.v1" as const,
          customerId: "ZCSUNG",
          customerName: "智宬轩系统门窗",
          templateId: "ZCSUNG-SIM-LH-120-TT",
          templateVersion: "0.1.0",
          publicProductName: "120内开内倒系统窗",
          sourceStatus: "public-reference" as const,
          engineeringStatus: "simulated-not-for-production" as const,
          productionReady: false as const,
          officialSourceUrls: ["https://www.shzcsung.com/product-200009.html"],
          assumptions: ["旧版占位目录。"]
        },
        layout: {
          ...sourceWindow.layout,
          cells: sourceWindow.layout.cells.map((cell) => cell.type === "fixed_glass"
            ? cell
            : { ...cell, hardwareSetId: "HW-ZCSUNG-PUBLIC-SIM-TT" })
        }
      }]
    };

    const restored = parseFormalDesignDocument(brokenSnapshot);
    const restoredWindow = restored.windows[0]!;

    expect(restoredWindow).toMatchObject({
      profileSystemId: "AL70",
      defaultHardwareSetId: "HW-TT-STD",
      productTemplateSelection: {
        templateId: "ZCSUNG-SIM-LH-120-TT",
        templateVersion: "0.1.1",
        productionReady: false
      }
    });
    expect(restoredWindow.layout.cells.every((cell) =>
      cell.type === "fixed_glass" || cell.hardwareSetId === "HW-TT-STD"
    )).toBe(true);
    expect(restoredWindow.productTemplateSelection?.assumptions.at(-1)).toContain(
      "0.1.0占位物料映射迁移"
    );
  });

  it("retains malformed stored text and never converts it into a trusted design", () => {
    const storage = new MemoryKeyValueStorage();
    storage.setItem("project:broken", "{not-json");
    const repository = new LocalDesignSnapshotRepository(storage, "project:broken");

    expect(() => repository.load()).toThrow(LocalDesignSnapshotValidationError);
    expect(storage.getItem("project:broken")).toBe("{not-json");
    expect(() => parseFormalDesignDocument({
      schemaVersion: "doormes-domain.v1",
      designId: "D-1",
      revision: -1,
      windows: []
    })).toThrow(/revision/);
  });

  it("reads the prototype's real LocalStorage key through the v2 migration adapter", () => {
    const fixture = readFileSync(
      resolve("tests/fixtures/legacy-v2/rectangular-fixed.input.json"),
      "utf8"
    );
    const storage = new MemoryKeyValueStorage();
    storage.setItem("doormes-designer-v1", fixture);

    const migration = migrateLegacyLocalStorageDesign(storage);

    expect(migration?.status).toBe("partial");
    expect(migration?.document.windows[0]).toMatchObject({
      objectId: "W-RECT-001",
      widthMm: 1200,
      heightMm: 1500
    });
    expect(migration?.issues).not.toHaveLength(0);
  });

  it("uses the same strict envelope for downloadable project files", () => {
    const fixture = JSON.parse(
      readFileSync(resolve("tests/fixtures/legacy-v2/rectangular-fixed.input.json"), "utf8")
    ) as unknown;
    const document = {
      ...migrateLegacyV2ToDomain(parseLegacyV2Design(fixture)).document,
      revision: 17
    };

    const source = serializeFormalDesignSnapshot(document, "2026-09-18T09:30:00.000Z");
    const restored = parseFormalDesignSnapshotText(source);

    expect(source).toContain("\n  \"schemaVersion\"");
    expect(restored.savedAtIso).toBe("2026-09-18T09:30:00.000Z");
    expect(restored.document).toMatchObject({
      designId: document.designId,
      revision: 17,
      windows: [{ objectId: "W-RECT-001", widthMm: 1200, heightMm: 1500 }]
    });
    expect(() => parseFormalDesignSnapshotText("not-json")).toThrow(
      /Project file is not valid JSON/
    );
  });
});
