import { describe, expect, it } from "vitest";
import type { WindowVisualConfiguration } from "@doormes/contracts";
import {
  applyAppearanceCatalogPreset,
  applyHardwareModelCatalogPreset,
  createManagedGltfGeometrySnapshot,
  createReferenceWindowVisualConfiguration,
  diagnoseWindowVisualAssets,
  listAppearanceCatalogPresets,
  listGlassBusinessCatalogPresets,
  listHardwareModelCatalogPresets,
  listSurroundBusinessCatalogPresets,
  normalizeGlassCatalogSelectionSnapshot,
  normalizeSurroundCatalogSelectionSnapshot,
  normalizeWindowVisualConfiguration,
  OPENING_HARDWARE_ROLES,
  publishComponentAssetCatalogVersion,
  removeWindowHardwareInstanceModel,
  replaceWindowAppearanceEditorSlot,
  replaceWindowHardwareInstanceModel,
  replaceWindowHardwareRoleModel,
  resolveAppearanceColour,
  resolveAppearanceRenderStyle,
  resolveComponentAssetImportBasis,
  resolveComponentModelBoundsMm,
  resolveHardwareComponentModel,
  resolvePublishedComponentAssetVersion,
  resolveGlassBusinessCatalogSelection,
  resolveSurroundBusinessCatalogSelection
} from "./index";

describe("window visual configuration", () => {
  it("creates independent semantic materials and a model fallback for every hardware role", () => {
    const configuration = createReferenceWindowVisualConfiguration({
      colorInside: "RAL9016",
      colorOutside: "RAL7016"
    });

    expect(configuration.appearance.frame.inside.baseColor).toBe("RAL9016");
    expect(configuration.appearance.frame.outside.baseColor).toBe("RAL7016");
    expect(configuration.appearance.glass.materialFamily).toBe("glass");
    expect(configuration.hardwareModels.map(({ role }) => role)).toEqual(OPENING_HARDWARE_ROLES);
    expect(configuration.hardwareModels.every(({ model }) =>
      model.productionStatus === "preview-only")).toBe(true);
  });

  it("resolves one exact custom lock before the role fallback", () => {
    const reference = createReferenceWindowVisualConfiguration();
    const hardwareId = "WIN-1:CELL-1:P1:LOCK-1";
    const customModel = {
      ...reference.hardwareModels.find(({ role }) => role === "lock-point")!.model,
      modelId: "customer.lock.oval-42",
      geometry: {
        kind: "gltf" as const,
        assetId: "ASSET-LOCK-OVAL-42",
        contentHash: `sha256:${"1".repeat(64)}`,
        lodAssetIds: ["ASSET-LOCK-OVAL-42-LOD1"]
      }
    };
    const partial = {
      ...reference,
      hardwareModels: [{ role: "lock-point" as const, hardwareId, model: customModel }]
    } satisfies WindowVisualConfiguration;
    const normalized = normalizeWindowVisualConfiguration(partial);

    expect(resolveHardwareComponentModel(normalized, "lock-point", hardwareId)?.modelId)
      .toBe("customer.lock.oval-42");
    expect(resolveHardwareComponentModel(normalized, "lock-point")?.modelId)
      .toBe("doormes.reference.lock-point");
    expect(normalized.hardwareModels).toHaveLength(OPENING_HARDWARE_ROLES.length + 1);
  });

  it("rejects duplicate assignments, unbound production models and invalid mounts", () => {
    const reference = createReferenceWindowVisualConfiguration();
    const handle = reference.hardwareModels.find(({ role }) => role === "handle")!;

    expect(() => normalizeWindowVisualConfiguration({
      ...reference,
      hardwareModels: [handle, handle]
    })).toThrow(/duplicate assignment/);
    expect(() => normalizeWindowVisualConfiguration({
      ...reference,
      hardwareModels: [{
        ...handle,
        model: { ...handle.model, productionStatus: "catalog-approved" }
      }]
    })).toThrow(/materialCode is required/);
    expect(() => normalizeWindowVisualConfiguration({
      ...reference,
      hardwareModels: [{
        ...handle,
        model: {
          ...handle.model,
          mount: { ...handle.model.mount, mountAxis: "sideways" }
        }
      }]
    } as unknown as WindowVisualConfiguration)).toThrow(/signed X, Y or Z axis/);
    expect(() => normalizeWindowVisualConfiguration({
      ...reference,
      hardwareModels: [{
        ...handle,
        model: {
          ...handle.model,
          mount: { ...handle.model.mount, pivotRatio: { x: 1.1, y: 0.5, z: 0 } }
        }
      }]
    })).toThrow(/pivotRatio.x/);
  });

  it("resolves RAL, CSS shorthand and RGB into one deterministic renderer colour", () => {
    expect(resolveAppearanceColour("RAL 7016")).toEqual({
      cssColor: "#383e42",
      colorHex: 0x383e42
    });
    expect(resolveAppearanceColour("#abc").cssColor).toBe("#aabbcc");
    expect(resolveAppearanceColour("rgb(56, 62, 66)").colorHex).toBe(0x383e42);
    expect(resolveAppearanceColour("unsupported-customer-colour").cssColor).toBe("#808080");
  });

  it("creates equal cache keys only for equal renderable appearance values", () => {
    const appearance = createReferenceWindowVisualConfiguration().appearance.glass;
    const first = resolveAppearanceRenderStyle(appearance);
    const second = resolveAppearanceRenderStyle({ ...appearance });
    const changed = resolveAppearanceRenderStyle({ ...appearance, roughness: 0.5 });

    expect(first.cacheKey).toBe(second.cacheKey);
    expect(changed.cacheKey).not.toBe(first.cacheKey);
    expect(first.transparent).toBe(true);
  });

  it("resolves physical model bounds around the pivot for edge and mount rotations", () => {
    const model = {
      ...createReferenceWindowVisualConfiguration().hardwareModels.find(
        ({ role }) => role === "handle"
      )!.model,
      dimensionsMm: { widthMm: 20, heightMm: 100, depthMm: 40 },
      mount: {
        pivotRatio: { x: 0.25, y: 0.5, z: 0 },
        mountAxis: "+x" as const
      }
    };

    expect(resolveComponentModelBoundsMm(model, "left")).toEqual({
      min: { x: 0, y: -50, z: -15 },
      max: { x: 40, y: 50, z: 5 }
    });
    expect(resolveComponentModelBoundsMm(model, "top")).toEqual({
      min: { x: 0, y: -5, z: -50 },
      max: { x: 40, y: 15, z: 50 }
    });
  });

  it("freezes import units, axes and a separate hash for every LOD tier", () => {
    const geometry = createManagedGltfGeometrySnapshot({
      primaryAsset: {
        assetId: "ASSET-HANDLE-42-HIGH",
        contentHash: `sha256:${"a".repeat(64)}`
      },
      lodAssets: [
        {
          quality: "medium",
          assetId: "ASSET-HANDLE-42-MEDIUM",
          contentHash: `sha256:${"b".repeat(64)}`
        },
        {
          quality: "low",
          assetId: "ASSET-HANDLE-42-LOW",
          contentHash: `sha256:${"c".repeat(64)}`
        }
      ],
      sourceUnit: "millimeter",
      upAxis: "+z",
      forwardAxis: "-y"
    });

    expect(geometry).toMatchObject({
      assetId: "ASSET-HANDLE-42-HIGH",
      lodAssetIds: ["ASSET-HANDLE-42-MEDIUM", "ASSET-HANDLE-42-LOW"],
      importConfiguration: {
        sourceUnit: "millimeter",
        upAxis: "+z",
        forwardAxis: "-y"
      }
    });
    expect(geometry.lodAssets?.map(({ quality, contentHash }) => [quality, contentHash])).toEqual([
      ["medium", `sha256:${"b".repeat(64)}`],
      ["low", `sha256:${"c".repeat(64)}`]
    ]);
    expect(resolveComponentAssetImportBasis(geometry.importConfiguration)).toEqual({
      unitScaleToMetres: 0.001,
      right: { x: 1, y: 0, z: 0 },
      up: { x: 0, y: 0, z: 1 },
      forward: { x: 0, y: -1, z: 0 }
    });
  });

  it("rejects ambiguous import axes and duplicate LOD quality tiers", () => {
    const base = {
      primaryAsset: {
        assetId: "ASSET-HANDLE-42-HIGH",
        contentHash: `sha256:${"a".repeat(64)}`
      },
      sourceUnit: "meter" as const,
      upAxis: "+y" as const,
      forwardAxis: "+z" as const
    };
    expect(() => createManagedGltfGeometrySnapshot({
      ...base,
      forwardAxis: "-y"
    })).toThrow(/perpendicular/i);
    expect(() => createManagedGltfGeometrySnapshot({
      ...base,
      lodAssets: [
        { quality: "low", assetId: "LOW-A", contentHash: `sha256:${"b".repeat(64)}` },
        { quality: "low", assetId: "LOW-B", contentHash: `sha256:${"c".repeat(64)}` }
      ]
    })).toThrow(/one asset per quality/i);
  });

  it("allows only approvers to append catalog versions and retains exact historical assets", () => {
    const referenceModel = createReferenceWindowVisualConfiguration().hardwareModels.find(
      ({ role }) => role === "handle"
    )!.model;
    const geometry = createManagedGltfGeometrySnapshot({
      primaryAsset: {
        assetId: "ASSET-HANDLE-42-HIGH",
        contentHash: `sha256:${"a".repeat(64)}`
      },
      lodAssets: [{
        quality: "low",
        assetId: "ASSET-HANDLE-42-LOW",
        contentHash: `sha256:${"b".repeat(64)}`
      }],
      sourceUnit: "millimeter",
      upAxis: "+z",
      forwardAxis: "-y"
    });
    const draft = {
      draftId: "DRAFT-HANDLE-42-1",
      catalogItemId: "HANDLE-42",
      revision: 3,
      reviewStatus: "in-review" as const,
      submittedBy: "designer-1",
      model: {
        ...referenceModel,
        modelId: "customer.handle.42",
        modelVersion: "1.0.0",
        geometry,
        productionStatus: "catalog-approved" as const,
        materialCode: "HW-HANDLE-42"
      }
    };

    expect(() => publishComponentAssetCatalogVersion({
      actor: { actorId: "designer-1", roles: ["asset-designer"] },
      draft,
      publishedAtIso: "2026-09-18T04:00:00.000Z",
      history: []
    })).toThrow(/catalog-approver permission/i);

    const firstHistory = publishComponentAssetCatalogVersion({
      actor: { actorId: "approver-1", roles: ["catalog-approver"] },
      draft,
      publishedAtIso: "2026-09-18T04:00:00.000Z",
      history: []
    });
    const secondHistory = publishComponentAssetCatalogVersion({
      actor: { actorId: "admin-1", roles: ["administrator"] },
      draft: {
        ...draft,
        draftId: "DRAFT-HANDLE-42-2",
        revision: 4,
        model: { ...draft.model, modelVersion: "1.1.0" }
      },
      publishedAtIso: "2026-09-18T05:00:00.000Z",
      history: firstHistory
    });

    expect(secondHistory.map(({ catalogVersion }) => catalogVersion)).toEqual([1, 2]);
    expect(resolvePublishedComponentAssetVersion(secondHistory, "HANDLE-42", 1)).toMatchObject({
      sourceDraftId: "DRAFT-HANDLE-42-1",
      model: { modelVersion: "1.0.0" },
      retainedAssets: [
        { quality: "high", assetId: "ASSET-HANDLE-42-HIGH" },
        { quality: "low", assetId: "ASSET-HANDLE-42-LOW" }
      ]
    });
    expect(resolvePublishedComponentAssetVersion(secondHistory, "HANDLE-42", 2)?.model.modelVersion)
      .toBe("1.1.0");
  });

  it("blocks catalog publication when legacy LOD IDs have no retained hashes", () => {
    const referenceModel = createReferenceWindowVisualConfiguration().hardwareModels.find(
      ({ role }) => role === "handle"
    )!.model;
    expect(() => publishComponentAssetCatalogVersion({
      actor: { actorId: "approver-1", roles: ["catalog-approver"] },
      draft: {
        draftId: "DRAFT-LEGACY",
        catalogItemId: "HANDLE-LEGACY",
        revision: 1,
        reviewStatus: "in-review",
        submittedBy: "designer-1",
        model: {
          ...referenceModel,
          productionStatus: "catalog-approved",
          materialCode: "HW-LEGACY",
          geometry: {
            kind: "gltf",
            assetId: "ASSET-LEGACY-HIGH",
            contentHash: `sha256:${"d".repeat(64)}`,
            lodAssetIds: ["ASSET-LEGACY-LOW"],
            importConfiguration: {
              schemaVersion: "doormes-component-import.v1",
              sourceUnit: "meter",
              upAxis: "+y",
              forwardAxis: "+z"
            }
          }
        }
      },
      publishedAtIso: "2026-09-18T06:00:00.000Z",
      history: []
    })).toThrow(/content hash for every LOD/i);
  });

  it("reports exact texture paths for missing and hash-mismatched bundles", () => {
    const reference = createReferenceWindowVisualConfiguration();
    const expectedHash = `sha256:${"1".repeat(64)}`;
    const configuration = normalizeWindowVisualConfiguration({
      ...reference,
      appearance: {
        ...reference.appearance,
        surroundOutside: {
          ...reference.appearance.surroundOutside,
          textureSetId: "TEXTURE-SURROUND-OUTSIDE",
          textureContentHash: expectedHash
        },
        surroundInside: {
          ...reference.appearance.surroundInside,
          textureSetId: "TEXTURE-SURROUND-INSIDE",
          textureContentHash: expectedHash
        },
        surroundLiner: {
          ...reference.appearance.surroundLiner,
          textureSetId: "TEXTURE-SURROUND-LINER"
        }
      }
    });
    const diagnostics = diagnoseWindowVisualAssets(configuration, {
      resolveTextureSet: (assetId) => assetId === "TEXTURE-SURROUND-OUTSIDE"
        ? { contentHash: `sha256:${"2".repeat(64)}` }
        : undefined,
      resolveComponentModel: () => undefined
    });

    expect(diagnostics).toEqual(expect.arrayContaining([
      expect.objectContaining({
        code: "TEXTURE_ASSET_HASH_MISMATCH",
        path: "appearance.surroundOutside",
        assetId: "TEXTURE-SURROUND-OUTSIDE"
      }),
      expect.objectContaining({
        code: "TEXTURE_ASSET_MISSING",
        path: "appearance.surroundInside",
        assetId: "TEXTURE-SURROUND-INSIDE"
      }),
      expect.objectContaining({
        code: "TEXTURE_HASH_NOT_SNAPSHOTTED",
        path: "appearance.surroundLiner",
        assetId: "TEXTURE-SURROUND-LINER"
      })
    ]));
  });

  it("replaces one semantic face without changing another face", () => {
    const reference = createReferenceWindowVisualConfiguration();
    const originalInside = reference.appearance.frame.inside;
    const nextOutside = {
      ...reference.appearance.frame.outside,
      appearanceId: "customer.frame.outside.blue",
      appearanceVersion: "2.0.0+design-r7",
      baseColor: "RAL5011",
      textureSetId: "TEXTURE-POWDER-5011",
      textureContentHash: `sha256:${"3".repeat(64)}`
    };

    const changed = replaceWindowAppearanceEditorSlot(
      reference,
      "frame.outside",
      nextOutside
    );

    expect(changed.appearance.frame.outside).toEqual(nextOutside);
    expect(changed.appearance.frame.inside).toEqual(originalInside);
    expect(changed.appearance.sash).toEqual(reference.appearance.sash);
  });

  it("replaces one role fallback while retaining exact hardware overrides", () => {
    const reference = createReferenceWindowVisualConfiguration();
    const fallback = reference.hardwareModels.find(({ role }) => role === "handle")!;
    const exactOverride = {
      ...fallback,
      hardwareId: "WIN-1:CELL-1:P1:HANDLE",
      model: { ...fallback.model, modelId: "customer.handle.exact" }
    };
    const configuration = normalizeWindowVisualConfiguration({
      ...reference,
      hardwareModels: [...reference.hardwareModels, exactOverride]
    });
    const nextFallback = {
      ...fallback.model,
      modelId: "customer.handle.default",
      modelVersion: "2.1.0",
      productionStatus: "preview-only" as const
    };

    const changed = replaceWindowHardwareRoleModel(configuration, "handle", nextFallback);

    expect(resolveHardwareComponentModel(changed, "handle")?.modelId)
      .toBe("customer.handle.default");
    expect(resolveHardwareComponentModel(
      changed,
      "handle",
      "WIN-1:CELL-1:P1:HANDLE"
    )?.modelId).toBe("customer.handle.exact");
    expect(resolveHardwareComponentModel(changed, "keeper"))
      .toEqual(resolveHardwareComponentModel(configuration, "keeper"));
  });

  it("adds and replaces one exact hardware model without changing its role fallback", () => {
    const reference = createReferenceWindowVisualConfiguration();
    const fallback = resolveHardwareComponentModel(reference, "handle")!;
    const hardwareId = "cell.1.1.hardware.handle";
    const first = replaceWindowHardwareInstanceModel(reference, "handle", hardwareId, {
      ...fallback,
      modelId: "customer.handle.instance-a",
      productionStatus: "preview-only"
    });
    const changed = replaceWindowHardwareInstanceModel(first, "handle", hardwareId, {
      ...fallback,
      modelId: "customer.handle.instance-b",
      productionStatus: "preview-only"
    });

    expect(resolveHardwareComponentModel(changed, "handle")?.modelId).toBe(fallback.modelId);
    expect(resolveHardwareComponentModel(changed, "handle", hardwareId)?.modelId)
      .toBe("customer.handle.instance-b");
    expect(changed.hardwareModels.filter((assignment) =>
      assignment.role === "handle" && assignment.hardwareId === hardwareId)).toHaveLength(1);
    const restored = removeWindowHardwareInstanceModel(changed, "handle", hardwareId);
    expect(resolveHardwareComponentModel(restored, "handle", hardwareId)?.modelId)
      .toBe(fallback.modelId);
    expect(restored.hardwareModels.some((assignment) =>
      assignment.role === "handle" && assignment.hardwareId === hardwareId)).toBe(false);
    expect(() => replaceWindowHardwareInstanceModel(
      reference,
      "handle",
      "   ",
      fallback
    )).toThrow(/hardwareId/);
  });

  it("filters and applies exact appearance presets only to compatible slots", () => {
    const reference = createReferenceWindowVisualConfiguration();
    const glassPresets = listAppearanceCatalogPresets("glass");
    const framePresets = listAppearanceCatalogPresets("frame.outside");

    expect(glassPresets.map(({ presetId }) => presetId)).toEqual(["GLASS-CLEAR-V1"]);
    expect(framePresets.map(({ presetId }) => presetId)).toContain(
      "FINISH-POWDER-RAL7016-V1"
    );

    const changed = applyAppearanceCatalogPreset(
      reference,
      "frame.outside",
      "FINISH-POWDER-RAL7016-V1"
    );
    expect(changed.appearance.frame.outside).toMatchObject({
      appearanceId: "catalog.finish.powder.ral7016",
      appearanceVersion: "1.0.0",
      baseColor: "RAL7016",
      finishCode: "POWDER-RAL7016",
      productionMapping: {
        schemaVersion: "doormes-surface-production.v1",
        productionStatus: "catalog-approved",
        treatmentCode: "ST-POWDER-RAL7016",
        processTemplateId: "PROC-POWDER-AL-V1"
      }
    });
    expect(changed.appearance.frame.inside).toEqual(reference.appearance.frame.inside);
    expect(() => applyAppearanceCatalogPreset(
      reference,
      "glass",
      "FINISH-POWDER-RAL7016-V1"
    )).toThrow(/not compatible/i);
  });

  it("resolves exact glass business versions and validates production snapshots", () => {
    const presets = listGlassBusinessCatalogPresets("AL70");
    expect(presets.map(({ selection }) => selection.catalogItemId)).toEqual([
      "GL-LOWE-24",
      "GL-TEMP-27"
    ]);
    const selection = resolveGlassBusinessCatalogSelection(
      "GL-TEMP-27",
      "1.0.0",
      "AL70"
    );
    expect(selection).toMatchObject({
      materialCode: "GL-TEMP-27",
      specification: "6+15A+6 钢化中空",
      thicknessMm: 27,
      appearance: {
        appearanceId: "catalog.glass.tempered.aqua",
        materialFamily: "glass"
      }
    });
    expect(() => resolveGlassBusinessCatalogSelection(
      "GL-TEMP-27",
      "2.0.0",
      "AL70"
    )).toThrow(/does not exist/i);
    expect(() => normalizeGlassCatalogSelectionSnapshot({
      ...selection,
      appearance: { ...selection.appearance, materialFamily: "metal" }
    })).toThrow(/must be glass/i);
  });

  it("resolves exact surround packages without deriving production data from colour", () => {
    const presets = listSurroundBusinessCatalogPresets("AL70");
    expect(presets.map(({ selection }) => selection.catalogItemId)).toEqual([
      "SUR-AL-BOARD-18",
      "SUR-STONE-GRAY-18"
    ]);
    const selection = resolveSurroundBusinessCatalogSelection(
      "SUR-STONE-GRAY-18",
      "1.0.0",
      "AL70"
    );
    expect(selection).toMatchObject({
      trimMaterialCode: "SURROUND-STONE-GRAY-18",
      linerMaterialCode: "LINER-COMPOSITE-GRAY-18",
      trimCutProcessTemplateId: "PROC-SURROUND-STONE-CUT-V1",
      boardThicknessMm: 18,
      outsideAppearance: {
        appearanceId: "catalog.surround.stone.gray.outside",
        materialFamily: "stone"
      }
    });
    expect(() => resolveSurroundBusinessCatalogSelection(
      "SUR-STONE-GRAY-18",
      "2.0.0",
      "AL70"
    )).toThrow(/does not exist/i);
    expect(() => normalizeSurroundCatalogSelectionSnapshot({
      ...selection,
      sealProcessTemplateId: ""
    })).toThrow(/sealProcessTemplateId/i);
  });

  it("rejects incomplete approved surface mappings and preserves reviewed ones", () => {
    const reference = createReferenceWindowVisualConfiguration();
    expect(() => normalizeWindowVisualConfiguration({
      ...reference,
      appearance: {
        ...reference.appearance,
        frame: {
          ...reference.appearance.frame,
          outside: {
            ...reference.appearance.frame.outside,
            productionMapping: {
              schemaVersion: "doormes-surface-production.v1",
              productionStatus: "catalog-approved",
              treatmentCode: "ST-INCOMPLETE"
            }
          }
        }
      }
    })).toThrow(/treatmentCode and processTemplateId/);

    const reviewed = applyAppearanceCatalogPreset(
      reference,
      "frame.outside",
      "FINISH-POWDER-RAL9016-V1"
    );
    expect(normalizeWindowVisualConfiguration(reviewed).appearance.frame.outside.productionMapping)
      .toEqual({
        schemaVersion: "doormes-surface-production.v1",
        productionStatus: "catalog-approved",
        treatmentCode: "ST-POWDER-RAL9016",
        processTemplateId: "PROC-POWDER-AL-V1"
      });
  });

  it("applies approved hardware presets without replacing exact-instance models", () => {
    const reference = createReferenceWindowVisualConfiguration();
    const fallback = reference.hardwareModels.find(({ role }) => role === "handle")!;
    const exactHardwareId = "WIN-1:CELL-1:P1:HANDLE";
    const configuration = normalizeWindowVisualConfiguration({
      ...reference,
      hardwareModels: [
        ...reference.hardwareModels,
        {
          ...fallback,
          hardwareId: exactHardwareId,
          model: { ...fallback.model, modelId: "customer.handle.exact" }
        }
      ]
    });

    expect(listHardwareModelCatalogPresets("handle")).toHaveLength(3);
    expect(listHardwareModelCatalogPresets("keeper")).toHaveLength(0);
    const changed = applyHardwareModelCatalogPreset(
      configuration,
      "handle",
      "HANDLE-KNOB-ROUND-V1"
    );

    expect(resolveHardwareComponentModel(changed, "handle")).toMatchObject({
      modelId: "catalog.handle.knob.round",
      catalogItemId: "HANDLE-KNOB-ROUND-V1",
      catalogVersion: "1.0.0",
      businessName: "执手 · 圆形旋钮",
      specification: "圆形旋钮执手 52×52×58 mm",
      productionStatus: "catalog-approved",
      materialCode: "HW-HANDLE-KNOB-ROUND",
      machiningTemplateId: "MACH-HANDLE-KNOB-ROUND",
      geometry: { kind: "parametric", primitiveId: "round-knob" }
    });
    expect(resolveHardwareComponentModel(changed, "handle", exactHardwareId)?.modelId)
      .toBe("customer.handle.exact");
    expect(() => applyHardwareModelCatalogPreset(
      configuration,
      "keeper",
      "HANDLE-KNOB-ROUND-V1"
    )).toThrow(/not compatible/i);
  });
});
