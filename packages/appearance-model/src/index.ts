import type {
  AppearanceMaterialFamily,
  ComponentAssetAxis,
  ComponentAssetImportSnapshot,
  ComponentAssetSourceUnit,
  ComponentGeometrySourceSnapshot,
  GltfComponentGeometrySnapshot,
  GltfComponentLodAssetSnapshot,
  GlassCatalogSelectionSnapshot,
  HardwareComponentModelSnapshot,
  OpeningHardwareRole,
  SurroundCatalogSelectionSnapshot,
  SurfaceProductionMappingSnapshot,
  SurfaceAppearanceAssignment,
  VisualAppearanceSnapshot,
  WindowAppearanceSnapshot,
  WindowHardwareModelAssignment,
  WindowInstallation,
  WindowUnit,
  WindowVisualConfiguration
} from "@doormes/contracts";

const SHA256_PATTERN = /^sha256:[0-9a-f]{64}$/;

/** Numeric unit vectors used to map one imported model into DoorMes axes. */
const COMPONENT_ASSET_AXIS_VECTORS: Readonly<Record<ComponentAssetAxis,
Readonly<{ readonly x: number; readonly y: number; readonly z: number }>>> = Object.freeze({
  "+x": { x: 1, y: 0, z: 0 },
  "-x": { x: -1, y: 0, z: 0 },
  "+y": { x: 0, y: 1, z: 0 },
  "-y": { x: 0, y: -1, z: 0 },
  "+z": { x: 0, y: 0, z: 1 },
  "-z": { x: 0, y: 0, z: -1 }
});

const COMPONENT_ASSET_UNIT_TO_METRES: Readonly<Record<ComponentAssetSourceUnit, number>> =
  Object.freeze({ millimeter: 0.001, centimeter: 0.01, meter: 1, inch: 0.0254 });

/**
 * Input committed by the final step of the PC or mobile model-import wizard.
 *
 * The UI may gather these values across several screens, but only this complete
 * value enters command history. Raw URLs and mutable files are intentionally
 * absent: callers first upload/inspect bytes and then provide managed IDs and
 * verified hashes.
 *
 * @since 0.10.5
 * @modified 2026-09-18 - Added one shared import boundary for both shells.
 */
export interface ManagedGltfGeometryImportInput {
  readonly primaryAsset: Readonly<{ readonly assetId: string; readonly contentHash: string }>;
  readonly lodAssets?: readonly GltfComponentLodAssetSnapshot[];
  readonly sourceUnit: ComponentAssetSourceUnit;
  readonly upAxis: ComponentAssetAxis;
  readonly forwardAxis: ComponentAssetAxis;
}

/** Renderer-neutral basis derived from a persisted import calibration. */
export interface ResolvedComponentAssetImportBasis {
  readonly unitScaleToMetres: number;
  readonly right: Readonly<{ readonly x: number; readonly y: number; readonly z: number }>;
  readonly up: Readonly<{ readonly x: number; readonly y: number; readonly z: number }>;
  readonly forward: Readonly<{ readonly x: number; readonly y: number; readonly z: number }>;
}

/**
 * Minimal read-only asset lookup shared by diagnostics and renderer adapters.
 *
 * Three's full registry structurally implements this contract while SVG tests,
 * editors and future API adapters can expose only immutable ID/hash records.
 * No renderer object, URL or raw byte array crosses this boundary.
 *
 * @since 0.10.6
 * @modified 2026-09-18 - Added renderer-neutral visual-asset availability lookup.
 */
export interface VisualAssetAvailability {
  resolveTextureSet(textureSetId: string): Readonly<{ readonly contentHash: string }> | undefined;
  resolveComponentModel(assetId: string): Readonly<{ readonly contentHash: string }> | undefined;
}

/** One editor-facing problem found while resolving a versioned visual asset. */
export interface VisualAssetDiagnostic {
  readonly severity: "warning" | "error";
  readonly code:
    | "TEXTURE_HASH_NOT_SNAPSHOTTED"
    | "TEXTURE_ASSET_MISSING"
    | "TEXTURE_ASSET_HASH_MISMATCH"
    | "COMPONENT_ASSET_MISSING"
    | "COMPONENT_ASSET_HASH_MISMATCH"
    | "COMPONENT_LOD_HASH_NOT_SNAPSHOTTED";
  readonly path: string;
  readonly assetId: string;
  readonly expectedContentHash?: string;
  readonly actualContentHash?: string;
  readonly message: string;
}

/** Validates and canonicalizes one managed SHA-256 identity. */
function normalizeAssetHash(value: string, field: string): string {
  const normalized = normalizeText(value, field).toLowerCase();
  if (!SHA256_PATTERN.test(normalized)) {
    throw new Error(`${field} must be a lowercase sha256:<64 hex> identity.`);
  }
  return normalized;
}

/**
 * Resolves the source unit and axes into a deterministic right-handed basis.
 *
 * Algorithm: map both signed axes to unit vectors, reject parallel/opposite
 * axes by their dot product, then calculate `right = up × forward`. Renderers
 * apply the resulting row basis before measuring and fitting the asset bounds.
 *
 * @param value Frozen import selection; missing legacy values use glTF defaults.
 * @returns Metre scale and source-space vectors for DoorMes X/Y/Z.
 * @example Z-up/-Y-forward returns right +X, up +Z and forward -Y.
 * @since 0.10.5
 * @modified 2026-09-18 - Centralized unit/axis conversion for SVG metadata and Three.
 */
export function resolveComponentAssetImportBasis(
  value: ComponentAssetImportSnapshot | undefined
): ResolvedComponentAssetImportBasis {
  const resolved = value ?? {
    schemaVersion: "doormes-component-import.v1" as const,
    sourceUnit: "meter" as const,
    upAxis: "+y" as const,
    forwardAxis: "+z" as const
  };
  if (resolved.schemaVersion !== "doormes-component-import.v1") {
    throw new Error("Unsupported component asset import schema version.");
  }
  const up = COMPONENT_ASSET_AXIS_VECTORS[resolved.upAxis];
  const forward = COMPONENT_ASSET_AXIS_VECTORS[resolved.forwardAxis];
  const unitScaleToMetres = COMPONENT_ASSET_UNIT_TO_METRES[resolved.sourceUnit];
  if (!up || !forward || !unitScaleToMetres) {
    throw new Error("Component asset import unit and axes must use supported values.");
  }
  const dot = up.x * forward.x + up.y * forward.y + up.z * forward.z;
  if (Math.abs(dot) > 1e-9) {
    throw new Error("Component asset upAxis and forwardAxis must be perpendicular.");
  }
  const canonicalZero = (component: number): number => component === 0 ? 0 : component;
  return {
    unitScaleToMetres,
    right: {
      x: canonicalZero(up.y * forward.z - up.z * forward.y),
      y: canonicalZero(up.z * forward.x - up.x * forward.z),
      z: canonicalZero(up.x * forward.y - up.y * forward.x)
    },
    up: { ...up },
    forward: { ...forward }
  };
}

/**
 * Completes the shared import wizard and creates one history-safe geometry snapshot.
 *
 * Algorithm: validate primary and LOD identities, require at most one medium
 * and one low tier, validate the axis basis, then derive the deprecated ID list
 * for old readers. The hashes stay beside every ID for deterministic replay.
 *
 * @param input Complete values gathered by a desktop or mobile wizard.
 * @returns Canonical managed glTF geometry ready for a visual-config command.
 * @example A millimetre/Z-up supplier asset can publish high and low tiers.
 * @since 0.10.5
 * @modified 2026-09-18 - Added ASSET-003 import snapshot creation.
 */
export function createManagedGltfGeometrySnapshot(
  input: ManagedGltfGeometryImportInput
): GltfComponentGeometrySnapshot {
  const importConfiguration: ComponentAssetImportSnapshot = {
    schemaVersion: "doormes-component-import.v1",
    sourceUnit: input.sourceUnit,
    upAxis: input.upAxis,
    forwardAxis: input.forwardAxis
  };
  resolveComponentAssetImportBasis(importConfiguration);
  const primaryAssetId = normalizeText(input.primaryAsset.assetId, "primaryAsset.assetId");
  const lodAssets = (input.lodAssets ?? []).map((asset, index) => ({
    quality: asset.quality,
    assetId: normalizeText(asset.assetId, `lodAssets[${index}].assetId`),
    contentHash: normalizeAssetHash(asset.contentHash, `lodAssets[${index}].contentHash`)
  } satisfies GltfComponentLodAssetSnapshot));
  if (new Set(lodAssets.map(({ quality }) => quality)).size !== lodAssets.length) {
    throw new Error("lodAssets may contain at most one asset per quality tier.");
  }
  const assetIds = [primaryAssetId, ...lodAssets.map(({ assetId }) => assetId)];
  if (new Set(assetIds).size !== assetIds.length) {
    throw new Error("Primary and LOD asset IDs must be unique.");
  }
  return {
    kind: "gltf",
    assetId: primaryAssetId,
    contentHash: normalizeAssetHash(input.primaryAsset.contentHash, "primaryAsset.contentHash"),
    lodAssetIds: lodAssets.map(({ assetId }) => assetId),
    lodAssets,
    importConfiguration
  };
}

/** Role allowed to participate in the controlled visual-asset catalog workflow. */
export type ComponentAssetCatalogRole = "asset-designer" | "catalog-approver" | "administrator";

/** Authenticated application actor evaluated by the catalog publishing rule. */
export interface ComponentAssetCatalogActor {
  readonly actorId: string;
  readonly roles: readonly ComponentAssetCatalogRole[];
}

/**
 * Reviewed draft handed from an editor to the catalog publication boundary.
 *
 * Revision is editor concurrency data, not the published version. A draft may
 * be replaced while under review; publication always creates a new immutable
 * history entry and never mutates an earlier order's snapshot.
 *
 * @since 0.10.5
 * @modified 2026-09-18 - Added controlled component-model draft metadata.
 */
export interface ComponentAssetCatalogDraft {
  readonly draftId: string;
  readonly catalogItemId: string;
  readonly revision: number;
  readonly reviewStatus: "in-review";
  readonly submittedBy: string;
  readonly model: HardwareComponentModelSnapshot;
}

/** Hash-pinned bytes retained for one published model version. */
export interface PublishedComponentAssetReference {
  readonly quality: "high" | "medium" | "low";
  readonly assetId: string;
  readonly contentHash: string;
}

/**
 * Immutable catalog version referenced by current and historical designs.
 *
 * `retainedAssets` is an explicit retention manifest for blob storage. Removing
 * a newer catalog item can therefore never make an old order silently resolve
 * another file or lose its approved LOD.
 *
 * @since 0.10.5
 * @modified 2026-09-18 - Added historical custom-asset retention snapshots.
 */
export interface PublishedComponentAssetCatalogVersion {
  readonly catalogItemId: string;
  readonly catalogVersion: number;
  readonly sourceDraftId: string;
  readonly sourceDraftRevision: number;
  readonly publishedAtIso: string;
  readonly publishedBy: string;
  readonly model: HardwareComponentModelSnapshot;
  readonly retainedAssets: readonly PublishedComponentAssetReference[];
}

/**
 * Publishes one approved custom component model by appending immutable history.
 *
 * Algorithm: authorize an approver/administrator, normalize the model, require
 * a manufacturing identity, require complete GLB import calibration and an
 * exact hash for every declared LOD, then append the next per-item version.
 * Existing history is copied and never rewritten.
 *
 * @param input Actor, reviewed draft, publication time and existing history.
 * @returns Detached history including the newly published version.
 * @throws When authorization, review state or asset retention data is invalid.
 * @example Publishing HANDLE-42 twice yields versions 1 and 2 while preserving 1.
 * @since 0.10.5
 * @modified 2026-09-18 - Added ASSET-003 catalog permission/history boundary.
 */
export function publishComponentAssetCatalogVersion(input: Readonly<{
  actor: ComponentAssetCatalogActor;
  draft: ComponentAssetCatalogDraft;
  publishedAtIso: string;
  history: readonly PublishedComponentAssetCatalogVersion[];
}>): readonly PublishedComponentAssetCatalogVersion[] {
  const actorId = normalizeText(input.actor.actorId, "actor.actorId");
  if (!input.actor.roles.some((role) => role === "catalog-approver" || role === "administrator")) {
    throw new Error("Publishing a component asset requires catalog-approver permission.");
  }
  if (input.draft.reviewStatus !== "in-review") {
    throw new Error("Only an in-review component asset draft may be published.");
  }
  if (!Number.isSafeInteger(input.draft.revision) || input.draft.revision < 1) {
    throw new RangeError("Component asset draft revision must be a positive integer.");
  }
  const catalogItemId = normalizeText(input.draft.catalogItemId, "draft.catalogItemId");
  const normalizedModel = normalizeHardwareModel(input.draft.model, "draft.model");
  if (normalizedModel.productionStatus !== "catalog-approved") {
    throw new Error("Published component models must be catalog-approved.");
  }
  const retainedAssets: PublishedComponentAssetReference[] = [];
  if (normalizedModel.geometry.kind === "gltf") {
    if (!normalizedModel.geometry.importConfiguration) {
      throw new Error("Published GLB/glTF models require a frozen import configuration.");
    }
    const lodAssets = normalizedModel.geometry.lodAssets ?? [];
    if (lodAssets.length !== normalizedModel.geometry.lodAssetIds.length) {
      throw new Error("Published GLB/glTF models require a content hash for every LOD asset.");
    }
    retainedAssets.push({
      quality: "high",
      assetId: normalizedModel.geometry.assetId,
      contentHash: normalizedModel.geometry.contentHash
    });
    retainedAssets.push(...lodAssets.map((asset) => ({ ...asset })));
  }
  const publishedAt = new Date(input.publishedAtIso);
  if (Number.isNaN(publishedAt.valueOf())) {
    throw new Error("publishedAtIso must be a valid date-time value.");
  }
  const existingVersions = input.history.filter((entry) => entry.catalogItemId === catalogItemId);
  const nextVersion = existingVersions.reduce(
    (maximum, entry) => Math.max(maximum, entry.catalogVersion),
    0
  ) + 1;
  return [
    ...input.history.map((entry) => ({
      ...entry,
      model: normalizeHardwareModel(entry.model, "history.model"),
      retainedAssets: entry.retainedAssets.map((asset) => ({ ...asset }))
    })),
    {
      catalogItemId,
      catalogVersion: nextVersion,
      sourceDraftId: normalizeText(input.draft.draftId, "draft.draftId"),
      sourceDraftRevision: input.draft.revision,
      publishedAtIso: publishedAt.toISOString(),
      publishedBy: actorId,
      model: normalizedModel,
      retainedAssets
    }
  ];
}

/**
 * Resolves one exact historical catalog version without falling back to latest.
 *
 * @param history Append-only published history.
 * @param catalogItemId Stable catalog item ID stored by the order.
 * @param catalogVersion Exact positive version stored by the order.
 * @returns Matching immutable snapshot, or undefined when retention is broken.
 * @since 0.10.5
 * @modified 2026-09-18 - Added deterministic historical-order resolution.
 */
export function resolvePublishedComponentAssetVersion(
  history: readonly PublishedComponentAssetCatalogVersion[],
  catalogItemId: string,
  catalogVersion: number
): PublishedComponentAssetCatalogVersion | undefined {
  return history.find((entry) =>
    entry.catalogItemId === catalogItemId.trim() && entry.catalogVersion === catalogVersion);
}

/**
 * Hardware roles currently emitted by the shared opening-geometry resolver.
 *
 * The ordered list creates deterministic legacy fallbacks and is also used by
 * validation tests to prove that every generated role can resolve a model.
 * Additions must be accompanied by an exhaustive reference geometry case.
 *
 * @example The first two roles form the two leaves of one hinge connection.
 * @since 0.10.2
 * @modified 2026-09-17 - Added the canonical hardware-model coverage order.
 */
export const OPENING_HARDWARE_ROLES: readonly OpeningHardwareRole[] = [
  "hinge-sash-leaf",
  "hinge-frame-leaf",
  "handle",
  "secondary-lever",
  "lock-point",
  "keeper",
  "shoot-bolt",
  "shoot-bolt-keeper"
];

/**
 * Legacy context used to create deterministic reference visuals for a window.
 *
 * The values are read only while migrating or creating a missing snapshot;
 * after that, `WindowVisualConfiguration` is the visual source of truth.
 *
 * @example A v2 window supplies RAL colours plus its normalized installation.
 * @since 0.10.2
 * @modified 2026-09-17 - Added non-destructive legacy appearance migration input.
 */
export interface ReferenceVisualConfigurationInput {
  readonly colorInside?: string;
  readonly colorOutside?: string;
  readonly installation?: WindowInstallation;
}

/**
 * Creates a validated visual material with explicit PBR defaults.
 *
 * The helper is kept private so every reference slot receives the same field
 * normalization. User/imported values go through `normalizeAppearance` below.
 *
 * @param id Stable semantic preset ID.
 * @param family Renderer-neutral material family.
 * @param color Catalog or CSS colour value.
 * @param options Optional PBR overrides.
 * @returns A complete immutable-compatible appearance snapshot.
 * @example `createAppearance("glass.clear", "glass", "#9fd8ef", { opacity: 0.45 })`.
 * @since 0.10.2
 * @modified 2026-09-17 - Added shared reference material construction.
 */
function createAppearance(
  id: string,
  family: AppearanceMaterialFamily,
  color: string,
  options: Readonly<Partial<Pick<VisualAppearanceSnapshot,
    "metalness" | "roughness" | "opacity" | "finishCode">>> = {}
): VisualAppearanceSnapshot {
  return {
    appearanceId: id,
    appearanceVersion: "1.0.0",
    materialFamily: family,
    baseColor: color,
    metalness: options.metalness ?? 0,
    roughness: options.roughness ?? 0.62,
    opacity: options.opacity ?? 1,
    ...(options.finishCode ? { finishCode: options.finishCode } : {})
  };
}

/**
 * Creates inside/outside/edge assignments for a coated profile family.
 *
 * Each face owns a separate snapshot so a later editor can replace one face
 * without aliasing and accidentally changing another face by reference.
 *
 * @param slot Stable semantic slot used to derive preset IDs.
 * @param insideColor Room-side finish.
 * @param outsideColor outdoor finish.
 * @returns Three independent metal appearance snapshots.
 * @since 0.10.2
 * @modified 2026-09-17 - Added dual-colour profile reference appearances.
 */
function createProfileSurface(
  slot: string,
  insideColor: string,
  outsideColor: string
): SurfaceAppearanceAssignment {
  return {
    inside: createAppearance(`${slot}.inside`, "metal", insideColor, {
      metalness: 0.72,
      roughness: 0.31,
      finishCode: "powder-coated"
    }),
    outside: createAppearance(`${slot}.outside`, "metal", outsideColor, {
      metalness: 0.72,
      roughness: 0.31,
      finishCode: "powder-coated"
    }),
    edge: createAppearance(`${slot}.edge`, "metal", "#9ca3af", {
      metalness: 0.68,
      roughness: 0.36,
      finishCode: "profile-edge"
    })
  };
}

/**
 * Maps the installation wall material to a renderer-neutral appearance preset.
 *
 * The mapping supplies a safe reference when older files contain only the wall
 * material ID. It does not convert that ID into a BOM material code.
 *
 * @param installation Optional normalized installation snapshot.
 * @returns A visual wall material matching the legacy material category.
 * @since 0.10.2
 * @modified 2026-09-17 - Added wall appearance migration defaults.
 */
function createWallAppearance(installation?: WindowInstallation): VisualAppearanceSnapshot {
  const materialId = installation?.surround.wallMaterialId ?? "plaster";
  const presets: Record<typeof materialId, readonly [AppearanceMaterialFamily, string]> = {
    plaster: ["plaster", "#e7e5e4"],
    concrete: ["stone", "#b8b8b1"],
    red_brick: ["brick", "#b4533c"],
    gray_brick: ["brick", "#7c858d"],
    stone: ["stone", "#a8a29e"]
  };
  const [family, color] = presets[materialId];
  return createAppearance(`wall.${materialId}`, family, color, { roughness: 0.86 });
}

/**
 * Creates the complete reference appearance snapshot for one window.
 *
 * Algorithm: preserve legacy inside/outside colours, derive wall and surround
 * defaults from installation data, and create independent semantic slots for
 * frame, sash, mullions, glass and hardware. No renderer object is persisted.
 *
 * @param input Legacy colours and optional installation snapshot.
 * @returns A versioned appearance snapshot ready for persistence.
 * @example A legacy RAL9016/RAL7016 window keeps those two profile faces.
 * @since 0.10.2
 * @modified 2026-09-17 - Added reference appearance snapshot generation.
 */
export function createReferenceWindowAppearance(
  input: ReferenceVisualConfigurationInput = {}
): WindowAppearanceSnapshot {
  const insideColor = input.colorInside?.trim() || "RAL9016";
  const outsideColor = input.colorOutside?.trim() || "RAL7016";
  const surroundOutside = input.installation?.surround.colorOutside?.trim() || outsideColor;
  const surroundInside = input.installation?.surround.colorInside?.trim() || insideColor;
  return {
    schemaVersion: "doormes-appearance.v1",
    frame: createProfileSurface("frame", insideColor, outsideColor),
    sash: createProfileSurface("sash", insideColor, outsideColor),
    mullion: createProfileSurface("mullion", insideColor, outsideColor),
    flyingMullion: createProfileSurface("flying-mullion", insideColor, outsideColor),
    glass: createAppearance("glass.clear-blue", "glass", "#9fd8ef", {
      roughness: 0.08,
      opacity: 0.45,
      finishCode: "clear-glazing"
    }),
    wall: createWallAppearance(input.installation),
    surroundOutside: createAppearance("surround.outside", "metal", surroundOutside, {
      metalness: 0.62,
      roughness: 0.36
    }),
    surroundInside: createAppearance("surround.inside", "metal", surroundInside, {
      metalness: 0.62,
      roughness: 0.36
    }),
    surroundLiner: createAppearance("surround.liner", "metal", surroundInside, {
      metalness: 0.58,
      roughness: 0.4
    }),
    hardwareDefault: createAppearance("hardware.dark-metal", "metal", "#1f2937", {
      metalness: 0.8,
      roughness: 0.26
    })
  };
}

/**
 * Returns a reference parametric shape and its physical preview envelope.
 *
 * The dimensions are conservative display defaults, not manufacturing facts.
 * The role switch is exhaustive so adding a new hardware role fails typecheck
 * until a deliberate fallback shape is selected.
 *
 * @param role Shared opening hardware role.
 * @returns Shape generator plus width, height and depth in millimetres.
 * @since 0.10.2
 * @modified 2026-09-17 - Added role-specific reference hardware shapes.
 */
function referenceGeometryForRole(role: OpeningHardwareRole): Readonly<{
  geometry: ComponentGeometrySourceSnapshot;
  dimensionsMm: HardwareComponentModelSnapshot["dimensionsMm"];
}> {
  switch (role) {
    case "handle":
    case "secondary-lever":
      return {
        geometry: {
          kind: "parametric",
          primitiveId: "lever-handle",
          parameters: { leverLengthMm: 108, plateWidthMm: 28, plateHeightMm: 132 }
        },
        dimensionsMm: { widthMm: 136, heightMm: 132, depthMm: 48 }
      };
    case "hinge-sash-leaf":
    case "hinge-frame-leaf":
      return {
        geometry: { kind: "parametric", primitiveId: "hinge-leaf", parameters: { knuckleDiameterMm: 12 } },
        dimensionsMm: { widthMm: 22, heightMm: 38, depthMm: 13 }
      };
    case "lock-point":
      return {
        geometry: { kind: "parametric", primitiveId: "lock-point", parameters: { pinDiameterMm: 9 } },
        dimensionsMm: { widthMm: 18, heightMm: 24, depthMm: 16 }
      };
    case "keeper":
      return {
        geometry: { kind: "parametric", primitiveId: "keeper", parameters: { slotWidthMm: 12 } },
        dimensionsMm: { widthMm: 20, heightMm: 32, depthMm: 10 }
      };
    case "shoot-bolt":
      return {
        geometry: { kind: "parametric", primitiveId: "shoot-bolt", parameters: { travelMm: 20 } },
        dimensionsMm: { widthMm: 16, heightMm: 58, depthMm: 14 }
      };
    case "shoot-bolt-keeper":
      return {
        geometry: { kind: "parametric", primitiveId: "keeper", parameters: { slotWidthMm: 10 } },
        dimensionsMm: { widthMm: 18, heightMm: 26, depthMm: 10 }
      };
  }
}

/**
 * Creates one role-level reference model assignment.
 *
 * Reference models are marked preview-only, so merely showing a hinge or lock
 * can never add an unreviewed manufacturing material to BOM results.
 *
 * @param role Hardware role to cover.
 * @param appearance Default hardware finish from the window appearance snapshot.
 * @returns A deterministic role assignment used for legacy migration.
 * @since 0.10.2
 * @modified 2026-09-17 - Added stable reference model IDs and fallbacks.
 */
function createReferenceHardwareAssignment(
  role: OpeningHardwareRole,
  appearance: VisualAppearanceSnapshot
): WindowHardwareModelAssignment {
  const reference = referenceGeometryForRole(role);
  const usesAccentFinish = role === "handle" || role === "secondary-lever" ||
    role === "lock-point" || role === "shoot-bolt";
  const modelAppearance = usesAccentFinish
    ? {
        ...appearance,
        appearanceId: "hardware.accent-metal",
        baseColor: "#d97706",
        metalness: 0.65,
        roughness: 0.28
      }
    : { ...appearance };
  return {
    role,
    model: {
      modelId: `doormes.reference.${role}`,
      modelVersion: "1.0.0",
      geometry: reference.geometry,
      dimensionsMm: reference.dimensionsMm,
      mount: {
        pivotRatio: { x: 0.5, y: 0.5, z: 0 },
        mountAxis: "+z"
      },
      appearance: modelAppearance,
      fallbackSymbolId: `hardware.${role}`,
      productionStatus: "preview-only"
    }
  };
}

/**
 * Creates a complete visual configuration for a new or migrated window.
 *
 * All known hardware roles receive role-level models. Future exact-hardware
 * overrides can be appended while retaining these deterministic fallbacks.
 *
 * @param input Legacy colour and installation context.
 * @returns Versioned appearance plus reference hardware model assignments.
 * @since 0.10.2
 * @modified 2026-09-17 - Added default visual-configuration creation.
 */
export function createReferenceWindowVisualConfiguration(
  input: ReferenceVisualConfigurationInput = {}
): WindowVisualConfiguration {
  const appearance = createReferenceWindowAppearance(input);
  return {
    schemaVersion: "doormes-visual-config.v1",
    appearance,
    hardwareModels: OPENING_HARDWARE_ROLES.map((role) =>
      createReferenceHardwareAssignment(role, appearance.hardwareDefault))
  };
}

/**
 * Validates and trims a persisted identifier or colour string.
 *
 * @param value Candidate string.
 * @param field Human-readable field path for deterministic errors.
 * @returns Trimmed non-empty text.
 * @throws When text is empty or implausibly large for a snapshot identifier.
 * @since 0.10.2
 * @modified 2026-09-17 - Added shared textual snapshot validation.
 */
function normalizeText(value: string, field: string): string {
  const normalized = value.trim();
  if (!normalized || normalized.length > 256) {
    throw new Error(`${field} must contain between 1 and 256 characters.`);
  }
  return normalized;
}

/**
 * Validates a finite number within an inclusive interval.
 *
 * @param value Candidate numeric value.
 * @param field Human-readable field path.
 * @param minimum Inclusive lower bound.
 * @param maximum Inclusive upper bound.
 * @returns The original finite value.
 * @since 0.10.2
 * @modified 2026-09-17 - Added deterministic numeric validation.
 */
function normalizeRange(
  value: number,
  field: string,
  minimum: number,
  maximum: number
): number {
  if (!Number.isFinite(value) || value < minimum || value > maximum) {
    throw new RangeError(`${field} must be between ${minimum} and ${maximum}.`);
  }
  return value;
}

/**
 * Deep-copies and validates one appearance snapshot.
 *
 * PBR fields stay in 0..1 and UV scales stay positive. Optional strings are
 * omitted when blank, producing stable JSON for history and persistence.
 *
 * @param value Candidate appearance from an editor or importer.
 * @param field Semantic field path.
 * @returns A normalized snapshot with no shared nested objects.
 * @since 0.10.2
 * @modified 2026-09-17 - Added imported/custom appearance validation.
 */
function normalizeAppearance(
  value: VisualAppearanceSnapshot,
  field: string
): VisualAppearanceSnapshot {
  const textureSetId = value.textureSetId?.trim();
  const textureContentHash = value.textureContentHash?.trim();
  if (textureContentHash && !textureSetId) {
    throw new Error(`${field}.textureContentHash requires textureSetId.`);
  }
  const finishCode = value.finishCode?.trim();
  const productionMapping = value.productionMapping
    ? normalizeSurfaceProductionMapping(value.productionMapping, `${field}.productionMapping`)
    : undefined;
  const uvScale = value.uvScale
    ? {
        x: normalizeRange(value.uvScale.x, `${field}.uvScale.x`, 0.001, 1_000),
        y: normalizeRange(value.uvScale.y, `${field}.uvScale.y`, 0.001, 1_000)
      }
    : undefined;
  return {
    appearanceId: normalizeText(value.appearanceId, `${field}.appearanceId`),
    appearanceVersion: normalizeText(value.appearanceVersion, `${field}.appearanceVersion`),
    materialFamily: value.materialFamily,
    baseColor: normalizeText(value.baseColor, `${field}.baseColor`),
    metalness: normalizeRange(value.metalness, `${field}.metalness`, 0, 1),
    roughness: normalizeRange(value.roughness, `${field}.roughness`, 0, 1),
    opacity: normalizeRange(value.opacity, `${field}.opacity`, 0, 1),
    ...(textureSetId ? { textureSetId: normalizeText(textureSetId, `${field}.textureSetId`) } : {}),
    ...(textureContentHash
      ? { textureContentHash: normalizeAssetHash(textureContentHash, `${field}.textureContentHash`) }
      : {}),
    ...(finishCode ? { finishCode: normalizeText(finishCode, `${field}.finishCode`) } : {}),
    ...(uvScale ? { uvScale } : {}),
    ...(productionMapping ? { productionMapping } : {})
  };
}

/**
 * Validates the sole appearance-to-manufacturing bridge.
 *
 * Algorithm: normalize the version/status first, then require both the stable
 * treatment code and process-template identity for a reviewed mapping. Draft
 * mappings may retain no production identity and are ignored by calculation.
 * Keeping this invariant here means project import, local history, PC and mobile
 * editing all enforce the same boundary.
 *
 * @param value Candidate mapping persisted with one appearance face.
 * @param field Full semantic path used in deterministic validation messages.
 * @returns Detached, normalized production mapping.
 * @example A reviewed powder finish must provide `ST-...` and `PROC-...`.
 * @since 0.10.24
 * @modified 2026-09-18 - Implemented APPEAR-004 mapping validation.
 */
function normalizeSurfaceProductionMapping(
  value: SurfaceProductionMappingSnapshot,
  field: string
): SurfaceProductionMappingSnapshot {
  if (value.schemaVersion !== "doormes-surface-production.v1") {
    throw new Error(`${field}.schemaVersion is unsupported.`);
  }
  if (!["preview-only", "catalog-approved"].includes(value.productionStatus)) {
    throw new Error(`${field}.productionStatus is unsupported.`);
  }
  const treatmentCode = value.treatmentCode?.trim();
  const processTemplateId = value.processTemplateId?.trim();
  if (value.productionStatus === "catalog-approved" && (!treatmentCode || !processTemplateId)) {
    throw new Error(
      `${field} requires treatmentCode and processTemplateId when catalog-approved.`
    );
  }
  return {
    schemaVersion: "doormes-surface-production.v1",
    productionStatus: value.productionStatus,
    ...(treatmentCode
      ? { treatmentCode: normalizeText(treatmentCode, `${field}.treatmentCode`) }
      : {}),
    ...(processTemplateId
      ? { processTemplateId: normalizeText(processTemplateId, `${field}.processTemplateId`) }
      : {})
  };
}

/**
 * Normalizes the three material faces of one profile slot.
 *
 * @param value Candidate face assignment.
 * @param field Semantic field path.
 * @returns A deep-copied assignment.
 * @since 0.10.2
 * @modified 2026-09-17 - Added profile-face snapshot normalization.
 */
function normalizeSurface(
  value: SurfaceAppearanceAssignment,
  field: string
): SurfaceAppearanceAssignment {
  return {
    inside: normalizeAppearance(value.inside, `${field}.inside`),
    outside: normalizeAppearance(value.outside, `${field}.outside`),
    edge: normalizeAppearance(value.edge, `${field}.edge`)
  };
}

/**
 * Deep-copies and validates a complete window appearance snapshot.
 *
 * @param value Candidate semantic appearance slots.
 * @returns A normalized v1 snapshot.
 * @since 0.10.2
 * @modified 2026-09-17 - Added appearance-root normalization.
 */
function normalizeWindowAppearance(value: WindowAppearanceSnapshot): WindowAppearanceSnapshot {
  if (value.schemaVersion !== "doormes-appearance.v1") {
    throw new Error("Unsupported window appearance schema version.");
  }
  return {
    schemaVersion: "doormes-appearance.v1",
    frame: normalizeSurface(value.frame, "appearance.frame"),
    sash: normalizeSurface(value.sash, "appearance.sash"),
    mullion: normalizeSurface(value.mullion, "appearance.mullion"),
    flyingMullion: normalizeSurface(value.flyingMullion, "appearance.flyingMullion"),
    glass: normalizeAppearance(value.glass, "appearance.glass"),
    wall: normalizeAppearance(value.wall, "appearance.wall"),
    surroundOutside: normalizeAppearance(value.surroundOutside, "appearance.surroundOutside"),
    surroundInside: normalizeAppearance(value.surroundInside, "appearance.surroundInside"),
    surroundLiner: normalizeAppearance(value.surroundLiner, "appearance.surroundLiner"),
    hardwareDefault: normalizeAppearance(value.hardwareDefault, "appearance.hardwareDefault")
  };
}

/**
 * Deep-copies and validates a parametric or managed GLB/glTF geometry source.
 *
 * Parametric values must be finite; managed assets require a frozen content
 * hash and unique LOD IDs. New snapshots additionally freeze every LOD hash
 * and source unit/axis calibration. Raw URLs are never accepted.
 *
 * @param value Candidate geometry source.
 * @param field Semantic field path.
 * @returns A normalized discriminated geometry snapshot.
 * @since 0.10.2
 * @modified 2026-09-18 - Added per-LOD hashes and import calibration validation.
 */
function normalizeGeometry(
  value: ComponentGeometrySourceSnapshot,
  field: string
): ComponentGeometrySourceSnapshot {
  if (value.kind === "parametric") {
    const parameters = Object.fromEntries(
      Object.entries(value.parameters).map(([key, parameter]) => [
        normalizeText(key, `${field}.parameters key`),
        normalizeRange(parameter, `${field}.parameters.${key}`, -100_000, 100_000)
      ])
    );
    return { kind: "parametric", primitiveId: value.primitiveId, parameters };
  }
  const lodAssetIds = value.lodAssetIds.map((assetId, index) =>
    normalizeText(assetId, `${field}.lodAssetIds[${index}]`));
  if (new Set(lodAssetIds).size !== lodAssetIds.length) {
    throw new Error(`${field}.lodAssetIds must be unique.`);
  }
  const lodAssets = value.lodAssets?.map((asset, index) => ({
    quality: asset.quality,
    assetId: normalizeText(asset.assetId, `${field}.lodAssets[${index}].assetId`),
    contentHash: normalizeAssetHash(
      asset.contentHash,
      `${field}.lodAssets[${index}].contentHash`
    )
  } satisfies GltfComponentLodAssetSnapshot)) ?? [];
  if (new Set(lodAssets.map(({ quality }) => quality)).size !== lodAssets.length) {
    throw new Error(`${field}.lodAssets may contain at most one asset per quality tier.`);
  }
  const canonicalLodIds = lodAssets.length > 0
    ? lodAssets.map(({ assetId }) => assetId)
    : lodAssetIds;
  const primaryAssetId = normalizeText(value.assetId, `${field}.assetId`);
  if (new Set([primaryAssetId, ...canonicalLodIds]).size !== canonicalLodIds.length + 1) {
    throw new Error(`${field} primary and LOD asset IDs must be unique.`);
  }
  const importConfiguration: ComponentAssetImportSnapshot = value.importConfiguration ?? {
    schemaVersion: "doormes-component-import.v1",
    sourceUnit: "meter",
    upAxis: "+y",
    forwardAxis: "+z"
  };
  resolveComponentAssetImportBasis(importConfiguration);
  return {
    kind: "gltf",
    assetId: primaryAssetId,
    contentHash: normalizeAssetHash(value.contentHash, `${field}.contentHash`),
    lodAssetIds: canonicalLodIds,
    lodAssets,
    importConfiguration: {
      schemaVersion: "doormes-component-import.v1",
      sourceUnit: importConfiguration.sourceUnit,
      upAxis: importConfiguration.upAxis,
      forwardAxis: importConfiguration.forwardAxis
    }
  };
}

/**
 * Deep-copies and validates a hardware component model.
 *
 * Preview envelopes are limited to 0.1..5000 mm, pivots remain inside the
 * envelope, and approved catalog models require an explicit material code.
 * These rules stop visual assets from becoming accidental BOM identities.
 *
 * @param value Candidate model snapshot.
 * @param field Semantic field path.
 * @returns A normalized component model.
 * @since 0.10.2
 * @modified 2026-09-17 - Added hardware-model invariant enforcement.
 */
function normalizeHardwareModel(
  value: HardwareComponentModelSnapshot,
  field: string
): HardwareComponentModelSnapshot {
  const materialCode = value.materialCode?.trim();
  const machiningTemplateId = value.machiningTemplateId?.trim();
  const catalogItemId = value.catalogItemId?.trim();
  const catalogVersion = value.catalogVersion?.trim();
  const businessName = value.businessName?.trim();
  const specification = value.specification?.trim();
  const hasBusinessIdentity = Boolean(
    catalogItemId || catalogVersion || businessName || specification
  );
  if (value.productionStatus === "catalog-approved" && !materialCode) {
    throw new Error(`${field}.materialCode is required for catalog-approved models.`);
  }
  if (hasBusinessIdentity &&
    (!catalogItemId || !catalogVersion || !businessName || !specification)) {
    throw new Error(`${field} business catalog identity must be complete.`);
  }
  const mountAxes: readonly HardwareComponentModelSnapshot["mount"]["mountAxis"][] = [
    "+x",
    "-x",
    "+y",
    "-y",
    "+z",
    "-z"
  ];
  if (!mountAxes.includes(value.mount.mountAxis)) {
    throw new Error(`${field}.mount.mountAxis must be a signed X, Y or Z axis.`);
  }
  return {
    modelId: normalizeText(value.modelId, `${field}.modelId`),
    modelVersion: normalizeText(value.modelVersion, `${field}.modelVersion`),
    ...(hasBusinessIdentity ? {
      catalogItemId: normalizeText(catalogItemId!, `${field}.catalogItemId`),
      catalogVersion: normalizeText(catalogVersion!, `${field}.catalogVersion`),
      businessName: normalizeText(businessName!, `${field}.businessName`),
      specification: normalizeText(specification!, `${field}.specification`)
    } : {}),
    geometry: normalizeGeometry(value.geometry, `${field}.geometry`),
    dimensionsMm: {
      widthMm: normalizeRange(value.dimensionsMm.widthMm, `${field}.dimensionsMm.widthMm`, 0.1, 5_000),
      heightMm: normalizeRange(value.dimensionsMm.heightMm, `${field}.dimensionsMm.heightMm`, 0.1, 5_000),
      depthMm: normalizeRange(value.dimensionsMm.depthMm, `${field}.dimensionsMm.depthMm`, 0.1, 5_000)
    },
    mount: {
      pivotRatio: {
        x: normalizeRange(value.mount.pivotRatio.x, `${field}.mount.pivotRatio.x`, 0, 1),
        y: normalizeRange(value.mount.pivotRatio.y, `${field}.mount.pivotRatio.y`, 0, 1),
        z: normalizeRange(value.mount.pivotRatio.z, `${field}.mount.pivotRatio.z`, 0, 1)
      },
      mountAxis: value.mount.mountAxis
    },
    appearance: normalizeAppearance(value.appearance, `${field}.appearance`),
    fallbackSymbolId: normalizeText(value.fallbackSymbolId, `${field}.fallbackSymbolId`),
    productionStatus: value.productionStatus,
    ...(materialCode ? { materialCode: normalizeText(materialCode, `${field}.materialCode`) } : {}),
    ...(machiningTemplateId
      ? { machiningTemplateId: normalizeText(machiningTemplateId, `${field}.machiningTemplateId`) }
      : {})
  };
}

/**
 * Validates and deep-copies a complete visual configuration.
 *
 * Algorithm: validate all semantic appearance slots, reject duplicate role or
 * exact-hardware assignments, then append missing role fallbacks from the
 * current reference set. Consequently every known geometry role resolves even
 * when a future editor stores only one custom handle override.
 *
 * @param value Candidate configuration; omitted values create reference defaults.
 * @param context Legacy colours and installation used only for missing defaults.
 * @returns A normalized snapshot safe for design history and persistence.
 * @since 0.10.2
 * @modified 2026-09-17 - Implemented APPEAR-001 and ASSET-001 normalization.
 */
export function normalizeWindowVisualConfiguration(
  value: WindowVisualConfiguration | undefined,
  context: ReferenceVisualConfigurationInput = {}
): WindowVisualConfiguration {
  const reference = createReferenceWindowVisualConfiguration(context);
  if (!value) {
    return reference;
  }
  if (value.schemaVersion !== "doormes-visual-config.v1") {
    throw new Error("Unsupported window visual-configuration schema version.");
  }
  const keys = new Set<string>();
  const normalizedAssignments = value.hardwareModels.map((assignment, index) => {
    const hardwareId = assignment.hardwareId?.trim();
    const key = `${assignment.role}|${hardwareId ?? ""}`;
    if (keys.has(key)) {
      throw new Error(`hardwareModels contains duplicate assignment ${key}.`);
    }
    keys.add(key);
    return {
      role: assignment.role,
      ...(hardwareId
        ? { hardwareId: normalizeText(hardwareId, `hardwareModels[${index}].hardwareId`) }
        : {}),
      model: normalizeHardwareModel(assignment.model, `hardwareModels[${index}].model`)
    } satisfies WindowHardwareModelAssignment;
  });
  for (const fallback of reference.hardwareModels) {
    const key = `${fallback.role}|`;
    if (!keys.has(key)) {
      normalizedAssignments.push(fallback);
      keys.add(key);
    }
  }
  return {
    schemaVersion: "doormes-visual-config.v1",
    appearance: normalizeWindowAppearance(value.appearance),
    hardwareModels: normalizedAssignments
  };
}

/**
 * Renderer-neutral material values resolved from one appearance snapshot.
 *
 * SVG consumes `cssColor`; Three.js consumes `colorHex` plus the PBR fields.
 * The stable `cacheKey` lets a renderer reuse one material for equal snapshots
 * without storing renderer objects in the domain model.
 *
 * @example RAL7016 resolves to CSS `#383e42` and numeric `0x383e42`.
 * @since 0.10.3
 * @modified 2026-09-17 - Added the shared APPEAR-002 render projection.
 */
export interface ResolvedAppearanceRenderStyle {
  readonly appearanceId: string;
  readonly appearanceVersion: string;
  readonly materialFamily: AppearanceMaterialFamily;
  readonly cssColor: string;
  readonly colorHex: number;
  readonly metalness: number;
  readonly roughness: number;
  readonly opacity: number;
  readonly transparent: boolean;
  readonly textureSetId?: string;
  readonly textureContentHash?: string;
  readonly finishCode?: string;
  readonly uvScale?: Readonly<{ readonly x: number; readonly y: number }>;
  readonly cacheKey: string;
}

/**
 * Built-in RAL values needed by migrated prototype and reference catalog data.
 *
 * The bounded table makes Node tests, SVG and Three.js resolve the same colour
 * without browser-dependent parsing. Catalog expansion will version this data.
 *
 * @example `RAL7016` maps to anthracite gray `#383e42`.
 * @since 0.10.3
 * @modified 2026-09-17 - Added deterministic reference RAL conversion.
 */
const RAL_COLOURS: Readonly<Record<string, string>> = Object.freeze({
  RAL1013: "#e9e5ce",
  RAL3005: "#5e2028",
  RAL6005: "#0f4336",
  RAL7016: "#383e42",
  RAL7035: "#c5c7c4",
  RAL8017: "#442f29",
  RAL9005: "#0a0a0d",
  RAL9006: "#a5a5a5",
  RAL9016: "#f1f0ea"
});

/**
 * CSS named colours accepted without relying on a browser canvas parser.
 *
 * This intentionally small compatibility table covers controlled catalog and
 * import values; unknown text uses the resolver's visible neutral fallback.
 *
 * @example `silver` resolves identically in server-side export and WebGL.
 * @since 0.10.3
 * @modified 2026-09-17 - Added cross-runtime named-colour compatibility.
 */
const CSS_NAMED_COLOURS: Readonly<Record<string, string>> = Object.freeze({
  black: "#000000",
  white: "#ffffff",
  red: "#ff0000",
  green: "#008000",
  blue: "#0000ff",
  gray: "#808080",
  grey: "#808080",
  silver: "#c0c0c0",
  transparent: "#000000"
});

/**
 * Resolves catalog, hexadecimal, RGB and common named colours deterministically.
 *
 * Algorithm: normalize whitespace/case, consult the RAL table, expand `#RGB`,
 * accept `#RRGGBB`, parse integer `rgb(r,g,b)`, then consult a bounded named
 * colour table. Unsupported values fall back to neutral gray in every renderer.
 *
 * @param value Persisted colour string.
 * @returns Canonical CSS hex and its 24-bit numeric representation.
 * @example `resolveAppearanceColour("rgb(56,62,66)")` equals RAL7016.
 * @since 0.10.3
 * @modified 2026-09-17 - Centralized SVG/Three colour interpretation.
 */
export function resolveAppearanceColour(value: string): Readonly<{
  cssColor: string;
  colorHex: number;
}> {
  const compact = value.trim().replace(/\s+/g, "");
  const upper = compact.toUpperCase();
  let cssColor = RAL_COLOURS[upper];
  if (!cssColor && /^#[0-9A-F]{3}$/i.test(compact)) {
    cssColor = `#${compact[1]}${compact[1]}${compact[2]}${compact[2]}${compact[3]}${compact[3]}`
      .toLowerCase();
  }
  if (!cssColor && /^#[0-9A-F]{6}$/i.test(compact)) cssColor = compact.toLowerCase();
  if (!cssColor) {
    const rgb = /^rgb\((\d{1,3}),(\d{1,3}),(\d{1,3})\)$/i.exec(compact);
    if (rgb) {
      const channels = rgb.slice(1).map((channel) => Math.min(255, Number(channel)));
      cssColor = `#${channels.map((channel) => channel.toString(16).padStart(2, "0")).join("")}`;
    }
  }
  cssColor ??= CSS_NAMED_COLOURS[compact.toLowerCase()] ?? "#808080";
  return { cssColor, colorHex: Number.parseInt(cssColor.slice(1), 16) };
}

/**
 * Resolves one domain appearance into stable SVG/Three material inputs.
 *
 * The cache key includes every currently renderable field plus texture and
 * finish identity. Future texture loading can extend the renderer without
 * changing how equal appearances are detected or persisted.
 *
 * @param appearance Versioned domain appearance snapshot.
 * @returns Renderer-neutral colour, PBR, alpha and cache metadata.
 * @since 0.10.3
 * @modified 2026-09-17 - Implemented shared APPEAR-002 style resolution.
 */
export function resolveAppearanceRenderStyle(
  appearance: VisualAppearanceSnapshot
): ResolvedAppearanceRenderStyle {
  const { cssColor, colorHex } = resolveAppearanceColour(appearance.baseColor);
  const cacheKey = [
    appearance.appearanceId,
    appearance.appearanceVersion,
    appearance.materialFamily,
    cssColor,
    appearance.metalness,
    appearance.roughness,
    appearance.opacity,
    appearance.textureSetId ?? "",
    appearance.textureContentHash ?? "",
    appearance.finishCode ?? "",
    appearance.uvScale?.x ?? "",
    appearance.uvScale?.y ?? ""
  ].join("|");
  return {
    appearanceId: appearance.appearanceId,
    appearanceVersion: appearance.appearanceVersion,
    materialFamily: appearance.materialFamily,
    cssColor,
    colorHex,
    metalness: appearance.metalness,
    roughness: appearance.roughness,
    opacity: appearance.opacity,
    transparent: appearance.opacity < 0.999,
    ...(appearance.textureSetId ? { textureSetId: appearance.textureSetId } : {}),
    ...(appearance.textureContentHash
      ? { textureContentHash: appearance.textureContentHash }
      : {}),
    ...(appearance.finishCode ? { finishCode: appearance.finishCode } : {}),
    ...(appearance.uvScale ? { uvScale: { ...appearance.uvScale } } : {}),
    cacheKey
  };
}

/**
 * Returns the canonical render snapshot for a window, including legacy files.
 *
 * New documents already contain a validated snapshot; normalization still
 * performs a defensive deep copy. Older/imported documents receive defaults
 * derived from their persisted RAL colours and installation material choices.
 *
 * @param window Domain window consumed by SVG or Three.js.
 * @returns Complete appearance and hardware-model configuration.
 * @since 0.10.3
 * @modified 2026-09-17 - Added one compatibility entry point for both renderers.
 */
export function resolveWindowVisualConfigurationForRender(
  window: WindowUnit
): WindowVisualConfiguration {
  return normalizeWindowVisualConfiguration(window.visualConfiguration, {
    colorInside: window.colorInside,
    colorOutside: window.colorOutside,
    installation: window.installation
  });
}

/**
 * Diagnoses missing, replaced or insufficiently versioned visual assets.
 *
 * Algorithm: enumerate every semantic appearance slot and GLB model assignment,
 * resolve each managed ID through the caller's immutable registry view, then
 * compare normalized hashes. Equal IDs are checked per path so an editor can
 * focus the exact face/model field that needs repair. Parametric geometry and
 * appearances without textures create no asset requirement.
 *
 * @param configuration Complete versioned window visual configuration.
 * @param availability Registry/API availability; absent means referenced bytes are unavailable.
 * @returns Stable editor-facing diagnostics in appearance/model declaration order.
 * @example A missing surround wood texture reports `appearance.surroundOutside`.
 * @since 0.10.6
 * @modified 2026-09-18 - Added APPEAR-002 missing-asset and hash diagnostics.
 */
export function diagnoseWindowVisualAssets(
  configuration: WindowVisualConfiguration,
  availability?: VisualAssetAvailability
): readonly VisualAssetDiagnostic[] {
  const diagnostics: VisualAssetDiagnostic[] = [];
  const appearances: readonly [string, VisualAppearanceSnapshot][] = [
    ["appearance.frame.inside", configuration.appearance.frame.inside],
    ["appearance.frame.outside", configuration.appearance.frame.outside],
    ["appearance.frame.edge", configuration.appearance.frame.edge],
    ["appearance.sash.inside", configuration.appearance.sash.inside],
    ["appearance.sash.outside", configuration.appearance.sash.outside],
    ["appearance.sash.edge", configuration.appearance.sash.edge],
    ["appearance.mullion.inside", configuration.appearance.mullion.inside],
    ["appearance.mullion.outside", configuration.appearance.mullion.outside],
    ["appearance.mullion.edge", configuration.appearance.mullion.edge],
    ["appearance.flyingMullion.inside", configuration.appearance.flyingMullion.inside],
    ["appearance.flyingMullion.outside", configuration.appearance.flyingMullion.outside],
    ["appearance.flyingMullion.edge", configuration.appearance.flyingMullion.edge],
    ["appearance.glass", configuration.appearance.glass],
    ["appearance.wall", configuration.appearance.wall],
    ["appearance.surroundOutside", configuration.appearance.surroundOutside],
    ["appearance.surroundInside", configuration.appearance.surroundInside],
    ["appearance.surroundLiner", configuration.appearance.surroundLiner],
    ["appearance.hardwareDefault", configuration.appearance.hardwareDefault]
  ];
  for (const [path, appearance] of appearances) {
    if (!appearance.textureSetId) continue;
    const available = availability?.resolveTextureSet(appearance.textureSetId);
    if (!appearance.textureContentHash) {
      diagnostics.push({
        severity: "warning",
        code: "TEXTURE_HASH_NOT_SNAPSHOTTED",
        path,
        assetId: appearance.textureSetId,
        ...(available ? { actualContentHash: available.contentHash } : {}),
        message: `Texture ${appearance.textureSetId} has no frozen content hash.`
      });
    }
    if (!available) {
      diagnostics.push({
        severity: "error",
        code: "TEXTURE_ASSET_MISSING",
        path,
        assetId: appearance.textureSetId,
        ...(appearance.textureContentHash
          ? { expectedContentHash: appearance.textureContentHash }
          : {}),
        message: `Texture ${appearance.textureSetId} is not available.`
      });
    } else if (
      appearance.textureContentHash &&
      available.contentHash.toLowerCase() !== appearance.textureContentHash.toLowerCase()
    ) {
      diagnostics.push({
        severity: "error",
        code: "TEXTURE_ASSET_HASH_MISMATCH",
        path,
        assetId: appearance.textureSetId,
        expectedContentHash: appearance.textureContentHash,
        actualContentHash: available.contentHash,
        message: `Texture ${appearance.textureSetId} content does not match its snapshot.`
      });
    }
  }
  configuration.hardwareModels.forEach((assignment, index) => {
    if (assignment.model.geometry.kind !== "gltf") return;
    const path = `hardwareModels[${index}].model.geometry`;
    const geometry = assignment.model.geometry;
    const required = [
      { assetId: geometry.assetId, contentHash: geometry.contentHash, path },
      ...(geometry.lodAssets ?? []).map((asset, lodIndex) => ({
        assetId: asset.assetId,
        contentHash: asset.contentHash,
        path: `${path}.lodAssets[${lodIndex}]`
      }))
    ];
    for (const requirement of required) {
      const available = availability?.resolveComponentModel(requirement.assetId);
      if (!available) {
        diagnostics.push({
          severity: "error",
          code: "COMPONENT_ASSET_MISSING",
          path: requirement.path,
          assetId: requirement.assetId,
          expectedContentHash: requirement.contentHash,
          message: `Component asset ${requirement.assetId} is not available.`
        });
      } else if (available.contentHash.toLowerCase() !== requirement.contentHash.toLowerCase()) {
        diagnostics.push({
          severity: "error",
          code: "COMPONENT_ASSET_HASH_MISMATCH",
          path: requirement.path,
          assetId: requirement.assetId,
          expectedContentHash: requirement.contentHash,
          actualContentHash: available.contentHash,
          message: `Component asset ${requirement.assetId} content does not match its snapshot.`
        });
      }
    }
    if (!geometry.lodAssets && geometry.lodAssetIds.length > 0) {
      geometry.lodAssetIds.forEach((assetId, lodIndex) => diagnostics.push({
        severity: "warning",
        code: "COMPONENT_LOD_HASH_NOT_SNAPSHOTTED",
        path: `${path}.lodAssetIds[${lodIndex}]`,
        assetId,
        message: `Legacy LOD ${assetId} has no frozen content hash.`
      }));
    }
  });
  return diagnostics;
}

/** Appearance slots exposed by the shared PC/mobile editor. */
export type WindowAppearanceEditorSlot =
  | "frame.inside"
  | "frame.outside"
  | "frame.edge"
  | "sash.inside"
  | "sash.outside"
  | "sash.edge"
  | "mullion.inside"
  | "mullion.outside"
  | "mullion.edge"
  | "flyingMullion.inside"
  | "flyingMullion.outside"
  | "flyingMullion.edge"
  | "glass"
  | "wall"
  | "surroundOutside"
  | "surroundInside"
  | "surroundLiner"
  | "hardwareDefault";

/** Canonical ordered slots keep desktop and mobile menus identical. */
export const WINDOW_APPEARANCE_EDITOR_SLOTS: readonly WindowAppearanceEditorSlot[] = [
  "frame.outside",
  "frame.inside",
  "frame.edge",
  "sash.outside",
  "sash.inside",
  "sash.edge",
  "mullion.outside",
  "mullion.inside",
  "mullion.edge",
  "flyingMullion.outside",
  "flyingMullion.inside",
  "flyingMullion.edge",
  "glass",
  "wall",
  "surroundOutside",
  "surroundInside",
  "surroundLiner",
  "hardwareDefault"
];

/** Returns the current appearance snapshot for one editor slot. */
export function resolveWindowAppearanceEditorSlot(
  configuration: WindowVisualConfiguration,
  slot: WindowAppearanceEditorSlot
): VisualAppearanceSnapshot {
  const [surface, face] = slot.split(".") as [string, string | undefined];
  if (face) {
    const assignment = configuration.appearance[
      surface as "frame" | "sash" | "mullion" | "flyingMullion"
    ];
    return assignment[face as "inside" | "outside" | "edge"];
  }
  return configuration.appearance[
    slot as "glass" | "wall" | "surroundOutside" | "surroundInside" |
      "surroundLiner" | "hardwareDefault"
  ];
}

/**
 * Replaces exactly one semantic appearance while preserving all other fields.
 *
 * Algorithm: branch only on the typed slot, construct a complete appearance
 * root, and run the same normalizer used by imports/domain commands. The output
 * is a detached whole configuration, so one desktop or mobile submit becomes
 * one atomic undo entry rather than a stream of partial colour/texture edits.
 *
 * @param configuration Current complete visual snapshot.
 * @param slot Semantic face/material selected by the editor.
 * @param appearance Complete replacement appearance for that slot.
 * @returns Normalized complete visual configuration.
 * @since 0.10.7
 * @modified 2026-09-18 - Added shared APPEAR-003 appearance edit projection.
 */
export function replaceWindowAppearanceEditorSlot(
  configuration: WindowVisualConfiguration,
  slot: WindowAppearanceEditorSlot,
  appearance: VisualAppearanceSnapshot
): WindowVisualConfiguration {
  const [surface, face] = slot.split(".") as [string, string | undefined];
  const nextAppearance = face
    ? {
        ...configuration.appearance,
        [surface]: {
          ...configuration.appearance[
            surface as "frame" | "sash" | "mullion" | "flyingMullion"
          ],
          [face]: appearance
        }
      }
    : { ...configuration.appearance, [slot]: appearance };
  return normalizeWindowVisualConfiguration({
    ...configuration,
    appearance: nextAppearance as WindowAppearanceSnapshot
  });
}

/**
 * Replaces one role-level hardware model used as the window's default.
 *
 * Exact hardware-ID overrides remain untouched. The model is normalized with
 * the complete configuration, preserving fallback coverage for every role and
 * making PC/mobile submissions behaviorally identical.
 *
 * @param configuration Current complete visual snapshot.
 * @param role Semantic hardware role to update.
 * @param model Complete replacement model snapshot.
 * @returns Normalized visual configuration with one role fallback replaced.
 * @since 0.10.7
 * @modified 2026-09-18 - Added shared hardware-model editor projection.
 */
export function replaceWindowHardwareRoleModel(
  configuration: WindowVisualConfiguration,
  role: OpeningHardwareRole,
  model: HardwareComponentModelSnapshot
): WindowVisualConfiguration {
  let replaced = false;
  const hardwareModels = configuration.hardwareModels.map((assignment) => {
    if (assignment.role !== role || assignment.hardwareId !== undefined) return assignment;
    replaced = true;
    return { ...assignment, model };
  });
  if (!replaced) hardwareModels.push({ role, model });
  return normalizeWindowVisualConfiguration({ ...configuration, hardwareModels });
}

/**
 * Replaces the visual model of one generated hardware instance.
 *
 * The stable `hardwareId` is stored beside the semantic role so the override
 * survives 2D/3D view changes and document persistence. The role fallback is
 * deliberately left unchanged: for example, one damaged-hinge replacement may
 * use a different catalog model while every other hinge still inherits the
 * window-wide hinge model. Replacing an existing exact assignment is idempotent
 * and adding a new one is normalized by the same validation path as imports.
 *
 * @param configuration Current complete visual snapshot.
 * @param role Semantic role emitted by shared opening geometry.
 * @param hardwareId Stable generated hardware instance ID.
 * @param model Complete replacement model snapshot for that instance.
 * @returns Normalized visual configuration with one exact override replaced.
 * @example Override `cell.1.1.hardware.handle` without changing other handles.
 * @since 0.10.17
 * @modified 2026-09-18 - Added exact-instance model editing for shared shells.
 */
export function replaceWindowHardwareInstanceModel(
  configuration: WindowVisualConfiguration,
  role: OpeningHardwareRole,
  hardwareId: string,
  model: HardwareComponentModelSnapshot
): WindowVisualConfiguration {
  const normalizedHardwareId = normalizeText(hardwareId, "hardwareId");
  let replaced = false;
  const hardwareModels = configuration.hardwareModels.map((assignment) => {
    if (assignment.role !== role || assignment.hardwareId !== normalizedHardwareId) {
      return assignment;
    }
    replaced = true;
    return { ...assignment, model };
  });
  if (!replaced) hardwareModels.push({ role, hardwareId: normalizedHardwareId, model });
  return normalizeWindowVisualConfiguration({ ...configuration, hardwareModels });
}

/**
 * Removes one exact hardware model so the instance inherits its role fallback.
 *
 * This is the inverse of `replaceWindowHardwareInstanceModel` and is safe to
 * expose as one undoable design command. Removing a missing assignment is
 * idempotent; normalization still verifies that every role fallback remains.
 *
 * @param configuration Current complete visual snapshot.
 * @param role Semantic role of the generated hardware instance.
 * @param hardwareId Stable generated hardware instance ID.
 * @returns Normalized configuration without that exact assignment.
 * @example Restore one custom hinge to the window-wide hinge catalog model.
 * @since 0.10.19
 * @modified 2026-09-18 - Added reversible exact-instance inheritance.
 */
export function removeWindowHardwareInstanceModel(
  configuration: WindowVisualConfiguration,
  role: OpeningHardwareRole,
  hardwareId: string
): WindowVisualConfiguration {
  const normalizedHardwareId = normalizeText(hardwareId, "hardwareId");
  return normalizeWindowVisualConfiguration({
    ...configuration,
    hardwareModels: configuration.hardwareModels.filter((assignment) =>
      assignment.role !== role || assignment.hardwareId !== normalizedHardwareId)
  });
}

/** One reviewed material/finish choice exposed by the shared editor catalog. */
export interface AppearanceCatalogPreset {
  readonly presetId: string;
  readonly label: string;
  readonly compatibleSlots: readonly WindowAppearanceEditorSlot[];
  readonly appearance: VisualAppearanceSnapshot;
}

/**
 * Reviewed business-facing glass choice used by the future design selector.
 *
 * The complete selection is copied into a design rather than retained as a
 * pointer to this in-memory list. A catalog upgrade therefore cannot silently
 * change the visual pane, thickness or material code of a historical order.
 *
 * @example The AL70 list contains exact Low-E and tempered insulating choices.
 * @since 0.10.29
 * @modified 2026-09-20 - Added the first CAT-001 glass catalog projection.
 */
export interface GlassBusinessCatalogPreset {
  readonly selection: GlassCatalogSelectionSnapshot;
}

/** Reviewed business-facing package/liner choice copied into one design. */
export interface SurroundBusinessCatalogPreset {
  readonly selection: SurroundCatalogSelectionSnapshot;
}

/** One reviewed hardware model choice exposed for compatible generated roles. */
export interface HardwareModelCatalogPreset {
  readonly presetId: string;
  readonly label: string;
  readonly compatibleRoles: readonly OpeningHardwareRole[];
  readonly model: HardwareComponentModelSnapshot;
}

const PROFILE_APPEARANCE_SLOTS: readonly WindowAppearanceEditorSlot[] =
  WINDOW_APPEARANCE_EDITOR_SLOTS.filter((slot) =>
    slot.startsWith("frame.") ||
    slot.startsWith("sash.") ||
    slot.startsWith("mullion.") ||
    slot.startsWith("flyingMullion.")
  );

/**
 * Creates one detached, validated hardware preset from a reference role model.
 *
 * The preset is explicitly catalog-approved and therefore must carry a stable
 * material code. It is never inferred from the display label or geometry ID.
 * Model snapshots are copied into a design when selected, so later catalog UI
 * changes cannot mutate a historical order.
 *
 * @param role Role whose reference mount/pivot and fallback symbol are reused.
 * @param input Reviewed catalog identity, shape, envelope and production link.
 * @returns Immutable editor-facing preset with a normalized model snapshot.
 * @since 0.10.8
 * @modified 2026-09-18 - Added APPEAR-003 controlled hardware presets.
 */
function createReferenceHardwareCatalogPreset(
  role: OpeningHardwareRole,
  input: Readonly<{
    presetId: string;
    label: string;
    specification: string;
    modelId: string;
    modelVersion: string;
    primitiveId: Extract<ComponentGeometrySourceSnapshot, { kind: "parametric" }>["primitiveId"];
    dimensionsMm: HardwareComponentModelSnapshot["dimensionsMm"];
    color: string;
    materialCode: string;
    machiningTemplateId: string;
  }>
): HardwareModelCatalogPreset {
  const reference = resolveHardwareComponentModel(createReferenceWindowVisualConfiguration(), role);
  if (!reference) throw new Error(`Reference catalog has no ${role} model.`);
  const model = normalizeHardwareModel({
    ...reference,
    modelId: input.modelId,
    modelVersion: input.modelVersion,
    catalogItemId: input.presetId,
    catalogVersion: input.modelVersion,
    businessName: input.label,
    specification: input.specification,
    geometry: { kind: "parametric", primitiveId: input.primitiveId, parameters: {} },
    dimensionsMm: { ...input.dimensionsMm },
    appearance: {
      ...reference.appearance,
      appearanceId: `${input.modelId}.finish`,
      appearanceVersion: input.modelVersion,
      baseColor: input.color
    },
    productionStatus: "catalog-approved",
    materialCode: input.materialCode,
    machiningTemplateId: input.machiningTemplateId
  }, `catalog.${input.presetId}.model`);
  return {
    presetId: input.presetId,
    label: input.label,
    compatibleRoles: [role],
    model
  };
}

/**
 * Reference appearance presets used until the remote catalog adapter lands.
 *
 * Every option is already a complete, versioned snapshot and declares exactly
 * where it is valid. The brick entry intentionally references hash-pinned
 * managed texture bytes; diagnostics remain visible until an asset adapter can
 * prove those bytes are available instead of silently substituting an image.
 *
 * @example `listAppearanceCatalogPresets("glass")` returns only glass finishes.
 * @since 0.10.8
 * @modified 2026-09-18 - Added the first controlled material preset catalog.
 */
export const REFERENCE_APPEARANCE_CATALOG_PRESETS: readonly AppearanceCatalogPreset[] = [
  {
    presetId: "FINISH-POWDER-RAL9016-V1",
    label: "粉末喷涂 · RAL9016",
    compatibleSlots: [...PROFILE_APPEARANCE_SLOTS],
    appearance: {
      appearanceId: "catalog.finish.powder.ral9016",
      appearanceVersion: "1.0.0",
      materialFamily: "metal",
      baseColor: "RAL9016",
      metalness: 0.58,
      roughness: 0.36,
      opacity: 1,
      finishCode: "POWDER-RAL9016",
      productionMapping: {
        schemaVersion: "doormes-surface-production.v1",
        productionStatus: "catalog-approved",
        treatmentCode: "ST-POWDER-RAL9016",
        processTemplateId: "PROC-POWDER-AL-V1"
      }
    }
  },
  {
    presetId: "FINISH-POWDER-RAL7016-V1",
    label: "粉末喷涂 · RAL7016",
    compatibleSlots: [...PROFILE_APPEARANCE_SLOTS],
    appearance: {
      appearanceId: "catalog.finish.powder.ral7016",
      appearanceVersion: "1.0.0",
      materialFamily: "metal",
      baseColor: "RAL7016",
      metalness: 0.62,
      roughness: 0.32,
      opacity: 1,
      finishCode: "POWDER-RAL7016",
      productionMapping: {
        schemaVersion: "doormes-surface-production.v1",
        productionStatus: "catalog-approved",
        treatmentCode: "ST-POWDER-RAL7016",
        processTemplateId: "PROC-POWDER-AL-V1"
      }
    }
  },
  {
    presetId: "GLASS-CLEAR-V1",
    label: "透明中空玻璃 · 参考",
    compatibleSlots: ["glass"],
    appearance: {
      appearanceId: "catalog.glass.clear",
      appearanceVersion: "1.0.0",
      materialFamily: "glass",
      baseColor: "#bfe3f2",
      metalness: 0,
      roughness: 0.08,
      opacity: 0.42,
      finishCode: "GLASS-CLEAR"
    }
  },
  {
    presetId: "WALL-PLASTER-WHITE-V1",
    label: "墙体 · 白色抹灰",
    compatibleSlots: ["wall"],
    appearance: {
      appearanceId: "catalog.wall.plaster.white",
      appearanceVersion: "1.0.0",
      materialFamily: "plaster",
      baseColor: "#d8dde6",
      metalness: 0,
      roughness: 0.92,
      opacity: 1,
      finishCode: "WALL-PLASTER-WHITE"
    }
  },
  {
    presetId: "WALL-BRICK-RED-V1",
    label: "墙体 · 红砖纹理（需受控资产）",
    compatibleSlots: ["wall"],
    appearance: {
      appearanceId: "catalog.wall.brick.red",
      appearanceVersion: "1.0.0",
      materialFamily: "brick",
      baseColor: "#9b5546",
      metalness: 0,
      roughness: 0.88,
      opacity: 1,
      textureSetId: "TEXTURE-WALL-BRICK-RED-V1",
      textureContentHash: `sha256:${"b".repeat(64)}`,
      uvScale: { x: 4, y: 3 },
      finishCode: "WALL-BRICK-RED"
    }
  },
  {
    presetId: "SURROUND-STONE-GRAY-V1",
    label: "包边/衬板 · 灰色石材",
    compatibleSlots: ["surroundOutside", "surroundInside", "surroundLiner"],
    appearance: {
      appearanceId: "catalog.surround.stone.gray",
      appearanceVersion: "1.0.0",
      materialFamily: "stone",
      baseColor: "#9ca3af",
      metalness: 0,
      roughness: 0.7,
      opacity: 1,
      finishCode: "SURROUND-STONE-GRAY"
    }
  }
];

/**
 * Local reviewed glass catalog used until the remote catalog adapter is added.
 *
 * Values deliberately match the legacy calculation catalog where possible, so
 * choosing the reference Low-E item keeps frozen BOM output unchanged while
 * adding exact version, applicability and visual identity to the design.
 *
 * @since 0.10.29
 * @modified 2026-09-20 - Added reference glass SKU-to-render mappings.
 */
export const REFERENCE_GLASS_BUSINESS_CATALOG_PRESETS:
readonly GlassBusinessCatalogPreset[] = [
  {
    selection: {
      schemaVersion: "doormes-glass-selection.v1",
      productionStatus: "catalog-approved",
      catalogItemId: "GL-LOWE-24",
      catalogVersion: "1.0.0",
      businessName: "Low-E中空玻璃",
      materialCode: "GL-LOWE-24",
      specification: "5+14A+5 Low-E",
      thicknessMm: 24,
      compatibleProfileSystemIds: ["AL70"],
      appearance: {
        appearanceId: "catalog.glass.lowe.clear",
        appearanceVersion: "1.0.0",
        materialFamily: "glass",
        baseColor: "#bfe3f2",
        metalness: 0,
        roughness: 0.08,
        opacity: 0.42,
        finishCode: "GLASS-LOWE-CLEAR"
      }
    }
  },
  {
    selection: {
      schemaVersion: "doormes-glass-selection.v1",
      productionStatus: "catalog-approved",
      catalogItemId: "GL-TEMP-27",
      catalogVersion: "1.0.0",
      businessName: "钢化中空玻璃",
      materialCode: "GL-TEMP-27",
      specification: "6+15A+6 钢化中空",
      thicknessMm: 27,
      compatibleProfileSystemIds: ["AL70"],
      appearance: {
        appearanceId: "catalog.glass.tempered.aqua",
        appearanceVersion: "1.0.0",
        materialFamily: "glass",
        baseColor: "#a9d7d1",
        metalness: 0,
        roughness: 0.12,
        opacity: 0.48,
        finishCode: "GLASS-TEMPERED-AQUA"
      }
    }
  }
];

/**
 * Local reviewed surround catalog used until the remote business adapter lands.
 *
 * Every record freezes display identity, physical board thickness, the four
 * production material identities, process templates and all renderer-facing
 * appearances. Keeping both aluminium and stone examples proves that business
 * users can choose a named product without seeing PBR fields.
 *
 * @since 0.10.30
 * @modified 2026-09-20 - Added the MS-01 package/liner reference catalog.
 */
export const REFERENCE_SURROUND_BUSINESS_CATALOG_PRESETS:
readonly SurroundBusinessCatalogPreset[] = [
  {
    selection: {
      schemaVersion: "doormes-surround-selection.v1",
      productionStatus: "catalog-approved",
      catalogItemId: "SUR-AL-BOARD-18",
      catalogVersion: "1.0.0",
      businessName: "铝合金包套与衬板套装",
      specification: "铝合金包套 / 18mm铝蜂窝衬板",
      compatibleProfileSystemIds: ["AL70"],
      boardThicknessMm: 18,
      trimMaterialCode: "SURROUND-AL-01",
      linerMaterialCode: "SURROUND-AL-01-LINER",
      cornerConnectorMaterialCode: "SURROUND-AL-01-CORNER",
      sealMaterialCode: "SEAL-INSTALL-SURROUND",
      trimCutProcessTemplateId: "PROC-SURROUND-AL-TRIM-CUT-V1",
      linerCutProcessTemplateId: "PROC-SURROUND-AL-LINER-CUT-V1",
      cornerAssemblyProcessTemplateId: "PROC-SURROUND-AL-CORNER-V1",
      sealProcessTemplateId: "PROC-SURROUND-SEAL-V1",
      outsideAppearance: {
        appearanceId: "catalog.surround.aluminium.ral7016.outside",
        appearanceVersion: "1.0.0",
        materialFamily: "metal",
        baseColor: "RAL7016",
        metalness: 0.58,
        roughness: 0.36,
        opacity: 1,
        finishCode: "POWDER-RAL7016"
      },
      insideAppearance: {
        appearanceId: "catalog.surround.aluminium.ral9016.inside",
        appearanceVersion: "1.0.0",
        materialFamily: "metal",
        baseColor: "RAL9016",
        metalness: 0.58,
        roughness: 0.36,
        opacity: 1,
        finishCode: "POWDER-RAL9016"
      },
      linerAppearance: {
        appearanceId: "catalog.surround.aluminium.ral9016.liner",
        appearanceVersion: "1.0.0",
        materialFamily: "metal",
        baseColor: "RAL9016",
        metalness: 0.42,
        roughness: 0.44,
        opacity: 1,
        finishCode: "POWDER-RAL9016"
      }
    }
  },
  {
    selection: {
      schemaVersion: "doormes-surround-selection.v1",
      productionStatus: "catalog-approved",
      catalogItemId: "SUR-STONE-GRAY-18",
      catalogVersion: "1.0.0",
      businessName: "灰色石材包套与衬板套装",
      specification: "灰色石材饰面 / 18mm复合衬板",
      compatibleProfileSystemIds: ["AL70"],
      boardThicknessMm: 18,
      trimMaterialCode: "SURROUND-STONE-GRAY-18",
      linerMaterialCode: "LINER-COMPOSITE-GRAY-18",
      cornerConnectorMaterialCode: "SURROUND-STONE-CORNER-GRAY",
      sealMaterialCode: "SEAL-STONE-NEUTRAL-GRAY",
      trimCutProcessTemplateId: "PROC-SURROUND-STONE-CUT-V1",
      linerCutProcessTemplateId: "PROC-SURROUND-COMPOSITE-LINER-CUT-V1",
      cornerAssemblyProcessTemplateId: "PROC-SURROUND-STONE-CORNER-V1",
      sealProcessTemplateId: "PROC-SURROUND-STONE-SEAL-V1",
      outsideAppearance: {
        appearanceId: "catalog.surround.stone.gray.outside",
        appearanceVersion: "1.0.0",
        materialFamily: "stone",
        baseColor: "#9ca3af",
        metalness: 0,
        roughness: 0.7,
        opacity: 1,
        finishCode: "SURROUND-STONE-GRAY"
      },
      insideAppearance: {
        appearanceId: "catalog.surround.stone.gray.inside",
        appearanceVersion: "1.0.0",
        materialFamily: "stone",
        baseColor: "#a8adb5",
        metalness: 0,
        roughness: 0.72,
        opacity: 1,
        finishCode: "SURROUND-STONE-GRAY"
      },
      linerAppearance: {
        appearanceId: "catalog.surround.composite.gray.liner",
        appearanceVersion: "1.0.0",
        materialFamily: "stone",
        baseColor: "#adb3bb",
        metalness: 0,
        roughness: 0.74,
        opacity: 1,
        finishCode: "LINER-COMPOSITE-GRAY"
      }
    }
  }
];

/**
 * Reference hardware presets, including visibly distinct handle/lock controls.
 *
 * These entries exercise catalog-approved selection without introducing a
 * remote service dependency. A later API adapter may append server snapshots,
 * but it must preserve IDs, versions and manufacturing mappings verbatim.
 *
 * @since 0.10.8
 * @modified 2026-09-18 - Added initial reviewed hardware choices.
 */
export const REFERENCE_HARDWARE_MODEL_CATALOG_PRESETS: readonly HardwareModelCatalogPreset[] = [
  createReferenceHardwareCatalogPreset("handle", {
    presetId: "HANDLE-LEVER-STD-V1",
    label: "执手 · 标准直柄",
    specification: "标准直柄执手 28×132×48 mm",
    modelId: "catalog.handle.lever.standard",
    modelVersion: "1.0.0",
    primitiveId: "lever-handle",
    dimensionsMm: { widthMm: 28, heightMm: 132, depthMm: 48 },
    color: "RAL9005",
    materialCode: "HW-HANDLE-LEVER-STD",
    machiningTemplateId: "MACH-HANDLE-LEVER-STD"
  }),
  createReferenceHardwareCatalogPreset("handle", {
    presetId: "HANDLE-KNOB-ROUND-V1",
    label: "执手 · 圆形旋钮",
    specification: "圆形旋钮执手 52×52×58 mm",
    modelId: "catalog.handle.knob.round",
    modelVersion: "1.0.0",
    primitiveId: "round-knob",
    dimensionsMm: { widthMm: 52, heightMm: 52, depthMm: 58 },
    color: "RAL9006",
    materialCode: "HW-HANDLE-KNOB-ROUND",
    machiningTemplateId: "MACH-HANDLE-KNOB-ROUND"
  }),
  createReferenceHardwareCatalogPreset("handle", {
    presetId: "HANDLE-LEVER-SLIM-V1",
    label: "执手 · 窄边长柄",
    specification: "窄边长柄执手 22×158×42 mm",
    modelId: "catalog.handle.lever.slim",
    modelVersion: "1.0.0",
    primitiveId: "lever-handle",
    dimensionsMm: { widthMm: 22, heightMm: 158, depthMm: 42 },
    color: "RAL7016",
    materialCode: "HW-HANDLE-LEVER-SLIM",
    machiningTemplateId: "MACH-HANDLE-LEVER-SLIM"
  })
];

/**
 * Lists reviewed material presets compatible with one semantic design slot.
 *
 * Filtering occurs in the shared model rather than a shell so desktop, mobile
 * and future API validation expose the same legal choices.
 *
 * @param slot Wall/profile/glass/surround slot being edited.
 * @returns Catalog entries in stable reference display order.
 * @example The glass slot returns `GLASS-CLEAR-V1`, never a powder finish.
 * @since 0.10.8
 * @modified 2026-09-18 - Added shared catalog option projection.
 */
export function listAppearanceCatalogPresets(
  slot: WindowAppearanceEditorSlot
): readonly AppearanceCatalogPreset[] {
  return REFERENCE_APPEARANCE_CATALOG_PRESETS.filter((preset) =>
    preset.compatibleSlots.includes(slot));
}

/**
 * Validates and detaches one immutable glass business selection.
 *
 * Algorithm: verify the exact schema/status, normalize all business identities,
 * require a practical positive glass thickness, deduplicate compatible systems
 * and run the embedded appearance through the same visual normalizer used by
 * renderers. The material family must remain `glass`.
 *
 * @param value Catalog or persisted selection to normalize.
 * @param field Diagnostic path included in validation failures.
 * @returns A detached snapshot safe for command history and persistence.
 * @example Normalizing `GL-LOWE-24` copies its compatibility list and appearance.
 * @since 0.10.29
 * @modified 2026-09-20 - Added CAT-001 immutable glass selection validation.
 */
export function normalizeGlassCatalogSelectionSnapshot(
  value: GlassCatalogSelectionSnapshot,
  field = "glassSelection"
): GlassCatalogSelectionSnapshot {
  if (value.schemaVersion !== "doormes-glass-selection.v1") {
    throw new Error(`${field}.schemaVersion is not supported.`);
  }
  if (value.productionStatus !== "catalog-approved") {
    throw new Error(`${field}.productionStatus must be catalog-approved.`);
  }
  if (!Number.isFinite(value.thicknessMm) || value.thicknessMm <= 0 || value.thicknessMm > 200) {
    throw new RangeError(`${field}.thicknessMm must be greater than 0 and no more than 200.`);
  }
  if (!Array.isArray(value.compatibleProfileSystemIds)) {
    throw new Error(`${field}.compatibleProfileSystemIds must be an array.`);
  }
  const compatibleProfileSystemIds = [...new Set(
    value.compatibleProfileSystemIds.map((id, index) =>
      normalizeText(id, `${field}.compatibleProfileSystemIds[${index}]`))
  )];
  if (compatibleProfileSystemIds.length === 0) {
    throw new Error(`${field}.compatibleProfileSystemIds must not be empty.`);
  }
  const appearance = normalizeAppearance(value.appearance, `${field}.appearance`);
  if (appearance.materialFamily !== "glass") {
    throw new Error(`${field}.appearance.materialFamily must be glass.`);
  }
  return {
    schemaVersion: "doormes-glass-selection.v1",
    productionStatus: "catalog-approved",
    catalogItemId: normalizeText(value.catalogItemId, `${field}.catalogItemId`),
    catalogVersion: normalizeText(value.catalogVersion, `${field}.catalogVersion`),
    businessName: normalizeText(value.businessName, `${field}.businessName`),
    materialCode: normalizeText(value.materialCode, `${field}.materialCode`),
    specification: normalizeText(value.specification, `${field}.specification`),
    thicknessMm: Math.round(value.thicknessMm * 10) / 10,
    compatibleProfileSystemIds,
    appearance
  };
}

/**
 * Lists exact reviewed glass versions compatible with a profile system.
 *
 * @param profileSystemId Window product system being designed.
 * @returns Detached business selections in stable catalog order.
 * @example `listGlassBusinessCatalogPresets("AL70")` returns 24mm and 27mm items.
 * @since 0.10.29
 * @modified 2026-09-20 - Added the first design-safe glass catalog query.
 */
export function listGlassBusinessCatalogPresets(
  profileSystemId: string
): readonly GlassBusinessCatalogPreset[] {
  const normalizedProfileSystemId = normalizeText(profileSystemId, "profileSystemId");
  return REFERENCE_GLASS_BUSINESS_CATALOG_PRESETS
    .filter(({ selection }) =>
      selection.compatibleProfileSystemIds.includes(normalizedProfileSystemId))
    .map(({ selection }) => ({
      selection: normalizeGlassCatalogSelectionSnapshot(selection)
    }));
}

/**
 * Resolves one exact glass catalog version without falling forward to latest.
 *
 * @param catalogItemId Stable business item ID selected by the designer.
 * @param catalogVersion Exact version displayed to the designer.
 * @param profileSystemId Current window product system.
 * @returns A detached immutable selection snapshot.
 * @throws When identity, version or applicability does not match exactly.
 * @example Resolve `GL-TEMP-27` version `1.0.0` for `AL70`.
 * @since 0.10.29
 * @modified 2026-09-20 - Prevented historical glass selection drift.
 */
export function resolveGlassBusinessCatalogSelection(
  catalogItemId: string,
  catalogVersion: string,
  profileSystemId: string
): GlassCatalogSelectionSnapshot {
  const normalizedItemId = normalizeText(catalogItemId, "catalogItemId");
  const normalizedVersion = normalizeText(catalogVersion, "catalogVersion");
  const normalizedProfileSystemId = normalizeText(profileSystemId, "profileSystemId");
  const preset = REFERENCE_GLASS_BUSINESS_CATALOG_PRESETS.find(({ selection }) =>
    selection.catalogItemId === normalizedItemId &&
    selection.catalogVersion === normalizedVersion
  );
  if (!preset) {
    throw new Error(`Glass catalog item ${normalizedItemId}@${normalizedVersion} does not exist.`);
  }
  if (!preset.selection.compatibleProfileSystemIds.includes(normalizedProfileSystemId)) {
    throw new Error(
      `Glass catalog item ${normalizedItemId}@${normalizedVersion} is not compatible with ` +
      `${normalizedProfileSystemId}.`
    );
  }
  return normalizeGlassCatalogSelectionSnapshot(preset.selection);
}

/**
 * Validates and detaches one exact package/liner business selection.
 *
 * Algorithm: validate the version/status, normalize every catalog, material
 * and process identity, constrain board thickness to practical installation
 * dimensions, deduplicate compatible profile systems and normalize the three
 * visual snapshots. No material code is ever inferred from an appearance.
 *
 * @param value Catalog or persisted selection to normalize.
 * @param field Diagnostic path included in validation failures.
 * @returns Detached selection safe for history, rendering and calculation.
 * @example Normalizing a stone package preserves distinct trim/liner SKUs.
 * @since 0.10.30
 * @modified 2026-09-20 - Added MS-01 surround catalog validation.
 */
export function normalizeSurroundCatalogSelectionSnapshot(
  value: SurroundCatalogSelectionSnapshot,
  field = "surroundSelection"
): SurroundCatalogSelectionSnapshot {
  if (value.schemaVersion !== "doormes-surround-selection.v1") {
    throw new Error(`${field}.schemaVersion is not supported.`);
  }
  if (value.productionStatus !== "catalog-approved") {
    throw new Error(`${field}.productionStatus must be catalog-approved.`);
  }
  if (!Number.isFinite(value.boardThicknessMm) ||
    value.boardThicknessMm < 5 || value.boardThicknessMm > 100) {
    throw new RangeError(`${field}.boardThicknessMm must be from 5 to 100.`);
  }
  if (!Array.isArray(value.compatibleProfileSystemIds)) {
    throw new Error(`${field}.compatibleProfileSystemIds must be an array.`);
  }
  const compatibleProfileSystemIds = [...new Set(
    value.compatibleProfileSystemIds.map((id, index) =>
      normalizeText(id, `${field}.compatibleProfileSystemIds[${index}]`))
  )];
  if (compatibleProfileSystemIds.length === 0) {
    throw new Error(`${field}.compatibleProfileSystemIds must not be empty.`);
  }
  return {
    schemaVersion: "doormes-surround-selection.v1",
    productionStatus: "catalog-approved",
    catalogItemId: normalizeText(value.catalogItemId, `${field}.catalogItemId`),
    catalogVersion: normalizeText(value.catalogVersion, `${field}.catalogVersion`),
    businessName: normalizeText(value.businessName, `${field}.businessName`),
    specification: normalizeText(value.specification, `${field}.specification`),
    compatibleProfileSystemIds,
    boardThicknessMm: Math.round(value.boardThicknessMm * 10) / 10,
    trimMaterialCode: normalizeText(value.trimMaterialCode, `${field}.trimMaterialCode`),
    linerMaterialCode: normalizeText(value.linerMaterialCode, `${field}.linerMaterialCode`),
    cornerConnectorMaterialCode: normalizeText(
      value.cornerConnectorMaterialCode,
      `${field}.cornerConnectorMaterialCode`
    ),
    sealMaterialCode: normalizeText(value.sealMaterialCode, `${field}.sealMaterialCode`),
    trimCutProcessTemplateId: normalizeText(
      value.trimCutProcessTemplateId,
      `${field}.trimCutProcessTemplateId`
    ),
    linerCutProcessTemplateId: normalizeText(
      value.linerCutProcessTemplateId,
      `${field}.linerCutProcessTemplateId`
    ),
    cornerAssemblyProcessTemplateId: normalizeText(
      value.cornerAssemblyProcessTemplateId,
      `${field}.cornerAssemblyProcessTemplateId`
    ),
    sealProcessTemplateId: normalizeText(
      value.sealProcessTemplateId,
      `${field}.sealProcessTemplateId`
    ),
    outsideAppearance: normalizeAppearance(
      value.outsideAppearance,
      `${field}.outsideAppearance`
    ),
    insideAppearance: normalizeAppearance(
      value.insideAppearance,
      `${field}.insideAppearance`
    ),
    linerAppearance: normalizeAppearance(value.linerAppearance, `${field}.linerAppearance`)
  };
}

/**
 * Lists exact reviewed package/liner versions compatible with a profile system.
 *
 * @param profileSystemId Current product system selected for the window.
 * @returns Detached business choices in stable reference order.
 * @example `listSurroundBusinessCatalogPresets("AL70")` returns aluminium and stone.
 * @since 0.10.30
 * @modified 2026-09-20 - Added the MS-01 business selector projection.
 */
export function listSurroundBusinessCatalogPresets(
  profileSystemId: string
): readonly SurroundBusinessCatalogPreset[] {
  const normalizedProfileSystemId = normalizeText(profileSystemId, "profileSystemId");
  return REFERENCE_SURROUND_BUSINESS_CATALOG_PRESETS
    .filter(({ selection }) =>
      selection.compatibleProfileSystemIds.includes(normalizedProfileSystemId))
    .map(({ selection }) => ({
      selection: normalizeSurroundCatalogSelectionSnapshot(selection)
    }));
}

/**
 * Resolves one exact package/liner version without falling forward to latest.
 *
 * @param catalogItemId Stable business item selected by the designer.
 * @param catalogVersion Exact version shown at selection time.
 * @param profileSystemId Current window product system.
 * @returns Detached immutable catalog selection.
 * @throws When identity, version or applicability does not match exactly.
 * @since 0.10.30
 * @modified 2026-09-20 - Prevented surround catalog drift.
 */
export function resolveSurroundBusinessCatalogSelection(
  catalogItemId: string,
  catalogVersion: string,
  profileSystemId: string
): SurroundCatalogSelectionSnapshot {
  const normalizedItemId = normalizeText(catalogItemId, "catalogItemId");
  const normalizedVersion = normalizeText(catalogVersion, "catalogVersion");
  const normalizedProfileSystemId = normalizeText(profileSystemId, "profileSystemId");
  const preset = REFERENCE_SURROUND_BUSINESS_CATALOG_PRESETS.find(({ selection }) =>
    selection.catalogItemId === normalizedItemId &&
    selection.catalogVersion === normalizedVersion
  );
  if (!preset) {
    throw new Error(
      `Surround catalog item ${normalizedItemId}@${normalizedVersion} does not exist.`
    );
  }
  if (!preset.selection.compatibleProfileSystemIds.includes(normalizedProfileSystemId)) {
    throw new Error(
      `Surround catalog item ${normalizedItemId}@${normalizedVersion} is not compatible with ` +
      `${normalizedProfileSystemId}.`
    );
  }
  return normalizeSurroundCatalogSelectionSnapshot(preset.selection);
}

/**
 * Lists reviewed model presets compatible with one generated hardware role.
 *
 * @param role Geometry role resolved by the opening assembly.
 * @returns Exact versioned models valid for the role, in catalog order.
 * @example The handle role returns lever/knob choices while keeper returns none.
 * @since 0.10.8
 * @modified 2026-09-18 - Added role-safe model option projection.
 */
export function listHardwareModelCatalogPresets(
  role: OpeningHardwareRole
): readonly HardwareModelCatalogPreset[] {
  return REFERENCE_HARDWARE_MODEL_CATALOG_PRESETS.filter((preset) =>
    preset.compatibleRoles.includes(role));
}

/**
 * Applies an exact appearance catalog snapshot to one compatible design slot.
 *
 * @throws When the preset is missing or not valid for the requested slot.
 * @example A frame preset cannot be assigned to the glass slot.
 * @since 0.10.8
 * @modified 2026-09-18 - Added guarded preset selection for both shells.
 */
export function applyAppearanceCatalogPreset(
  configuration: WindowVisualConfiguration,
  slot: WindowAppearanceEditorSlot,
  presetId: string
): WindowVisualConfiguration {
  const preset = REFERENCE_APPEARANCE_CATALOG_PRESETS.find((candidate) =>
    candidate.presetId === presetId.trim());
  if (!preset) throw new Error(`Appearance catalog preset ${presetId} does not exist.`);
  if (!preset.compatibleSlots.includes(slot)) {
    throw new Error(`Appearance catalog preset ${presetId} is not compatible with ${slot}.`);
  }
  return replaceWindowAppearanceEditorSlot(configuration, slot, preset.appearance);
}

/**
 * Applies an exact reviewed model while preserving per-instance overrides.
 *
 * @throws When the preset is missing or incompatible with the selected role.
 * @example Choosing a handle catalog item cannot replace hinge assignments.
 * @since 0.10.8
 * @modified 2026-09-18 - Added guarded hardware preset selection.
 */
export function applyHardwareModelCatalogPreset(
  configuration: WindowVisualConfiguration,
  role: OpeningHardwareRole,
  presetId: string
): WindowVisualConfiguration {
  const preset = REFERENCE_HARDWARE_MODEL_CATALOG_PRESETS.find((candidate) =>
    candidate.presetId === presetId.trim());
  if (!preset) throw new Error(`Hardware catalog preset ${presetId} does not exist.`);
  if (!preset.compatibleRoles.includes(role)) {
    throw new Error(`Hardware catalog preset ${presetId} is not compatible with ${role}.`);
  }
  return replaceWindowHardwareRoleModel(configuration, role, preset.model);
}

/** Axis-aligned millimetre bounds of a component after its fixed mount rotation. */
export interface ResolvedComponentModelBoundsMm {
  readonly min: Readonly<{ readonly x: number; readonly y: number; readonly z: number }>;
  readonly max: Readonly<{ readonly x: number; readonly y: number; readonly z: number }>;
}

/**
 * Resolves a component envelope around its declared mounting pivot.
 *
 * Algorithm: create the eight corners from real model dimensions and pivot,
 * mirror right-edge instances, apply the same top/bottom Z rotation used by
 * Three, then rotate the declared mount axis onto its host. The resulting AABB
 * remains renderer-free and can therefore drive installation collision and MES
 * process evidence without measuring a display mesh.
 *
 * @param model Versioned component model with physical dimensions and pivot.
 * @param edge Physical window edge used by the resolved hardware placement.
 * @returns Fixed-orientation bounds relative to the component mount point.
 * @example A 28×132×48 handle with rear-face pivot extends 0..48mm on local Z.
 * @since 0.10.4
 * @modified 2026-09-18 - Added shared model envelope for rendering/process audits.
 */
export function resolveComponentModelBoundsMm(
  model: HardwareComponentModelSnapshot,
  edge: "left" | "right" | "top" | "bottom"
): ResolvedComponentModelBoundsMm {
  const { widthMm, heightMm, depthMm } = model.dimensionsMm;
  const pivot = model.mount.pivotRatio;
  const xValues = [-pivot.x * widthMm, (1 - pivot.x) * widthMm];
  const yValues = [-pivot.y * heightMm, (1 - pivot.y) * heightMm];
  const zValues = [-pivot.z * depthMm, (1 - pivot.z) * depthMm];
  const minimum = { x: Infinity, y: Infinity, z: Infinity };
  const maximum = { x: -Infinity, y: -Infinity, z: -Infinity };
  for (const sourceX of xValues) {
    for (const sourceY of yValues) {
      for (const sourceZ of zValues) {
        let x = edge === "right" ? -sourceX : sourceX;
        let y = sourceY;
        let z = sourceZ;
        if (edge === "top") [x, y] = [-y, x];
        if (edge === "bottom") [x, y] = [y, -x];
        switch (model.mount.mountAxis) {
          case "+z":
            break;
          case "-z":
            [y, z] = [-y, -z];
            break;
          case "+x":
            [x, z] = [z, -x];
            break;
          case "-x":
            [x, z] = [-z, x];
            break;
          case "+y":
            [y, z] = [z, -y];
            break;
          case "-y":
            [y, z] = [-z, y];
            break;
        }
        minimum.x = Math.min(minimum.x, x);
        minimum.y = Math.min(minimum.y, y);
        minimum.z = Math.min(minimum.z, z);
        maximum.x = Math.max(maximum.x, x);
        maximum.y = Math.max(maximum.y, y);
        maximum.z = Math.max(maximum.z, z);
      }
    }
  }
  const canonicalize = (value: number): number => Object.is(value, -0) ? 0 : value;
  return {
    min: {
      x: canonicalize(minimum.x),
      y: canonicalize(minimum.y),
      z: canonicalize(minimum.z)
    },
    max: {
      x: canonicalize(maximum.x),
      y: canonicalize(maximum.y),
      z: canonicalize(maximum.z)
    }
  };
}

/**
 * Resolves the model for a generated hardware component.
 *
 * Resolution is deterministic: exact hardware ID wins, followed by the role
 * fallback. The normalizer guarantees a role fallback for every current role,
 * so normalized configurations always return a model.
 *
 * @param configuration Window visual snapshot.
 * @param role Semantic hardware role from shared geometry.
 * @param hardwareId Optional exact generated hardware ID.
 * @returns The assigned model, or undefined only for unnormalized legacy input.
 * @example A custom lock override wins over the generic `lock-point` model.
 * @since 0.10.2
 * @modified 2026-09-17 - Added exact-then-role component model resolution.
 */
export function resolveHardwareComponentModel(
  configuration: WindowVisualConfiguration,
  role: OpeningHardwareRole,
  hardwareId?: string
): HardwareComponentModelSnapshot | undefined {
  return configuration.hardwareModels.find((assignment) =>
    assignment.role === role &&
    hardwareId !== undefined &&
    assignment.hardwareId === hardwareId)?.model ??
    configuration.hardwareModels.find((assignment) =>
      assignment.role === role && assignment.hardwareId === undefined)?.model;
}
