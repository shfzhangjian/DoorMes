import type {
  DesignDocument,
  FabricationAssembly,
  HardwareComponentModelSnapshot,
  SurfaceAppearanceAssignment,
  VisualAppearanceSnapshot,
  WindowInstallationSide,
  WindowUnit
} from "@doormes/contracts";
import {
  resolveAppearanceRenderStyle,
  resolveComponentAssetImportBasis,
  resolveHardwareComponentModel,
  resolveWindowVisualConfigurationForRender
} from "@doormes/appearance-model";
import {
  createWindowInstallationSurroundObjectId,
  createWindowInstallationWallObjectId,
  normalizeWindowInstallation,
  resolveFabricationAssemblyGeometry,
  resolveInstallationSectionFromFrameDepth,
  resolveInstallationSubjects,
  resolveWindowGeometry,
  resolveWindowInstallationBottomSupportMm,
  resolveWindowInstallationObstacleGeometry,
  resolveWindowInstallationSurroundGeometry,
  resolveWindowInstallationSection,
  resolveWindowSectionDimensions,
  type ResolvedFabricationAssemblyGeometry,
  type ResolvedRectangleMm
} from "@doormes/geometry-topology";
import {
  createOpeningMechanismMotion,
  createOpeningPanelKey,
  resolveOpeningConnectionLayout,
  resolveOpeningPose,
  sampleOpeningAngleArc,
  type OpeningConnectionLayoutPreset,
  type OpeningHingeEdge,
  type OpeningMotionMode
} from "@doormes/opening-kinematics";
import {
  inspectManagedGltfAsset,
  type ManagedGltfInspection
} from "@doormes/visual-asset-storage";
import {
  AmbientLight,
  Box3,
  Box3Helper,
  BoxGeometry,
  BufferGeometry,
  CanvasTexture,
  Color,
  CylinderGeometry,
  DirectionalLight,
  Float32BufferAttribute,
  Group,
  GridHelper,
  Line,
  LineBasicMaterial,
  LineDashedMaterial,
  Matrix4,
  Mesh,
  MeshStandardMaterial,
  Material,
  Object3D,
  PerspectiveCamera,
  Raycaster,
  RepeatWrapping,
  Scene,
  SphereGeometry,
  SRGBColorSpace,
  Sprite,
  SpriteMaterial,
  Texture,
  Vector2,
  Vector3,
  WebGLRenderer
} from "three";
import { OrbitControls } from "three/addons/controls/OrbitControls.js";
import type { GLTF } from "three/addons/loaders/GLTFLoader.js";

const MILLIMETRES_TO_METRES = 0.001;

interface ThreePlanPointMm {
  readonly xMm: number;
  readonly zMm: number;
}

/** Builds one convex X/Z footprint as a vertical solid without a box-shaped proxy. */
function createVerticalPlanPrismGeometry(
  points: readonly ThreePlanPointMm[],
  heightMm: number
): BufferGeometry {
  if (points.length < 3) throw new Error("A plan prism requires at least three points.");
  const halfHeight = heightMm * MILLIMETRES_TO_METRES / 2;
  const vertices: number[] = [];
  const push = (point: ThreePlanPointMm, y: number): void => {
    vertices.push(
      point.xMm * MILLIMETRES_TO_METRES,
      y,
      point.zMm * MILLIMETRES_TO_METRES
    );
  };
  for (let index = 1; index < points.length - 1; index += 1) {
    push(points[0]!, halfHeight);
    push(points[index + 1]!, halfHeight);
    push(points[index]!, halfHeight);
    push(points[0]!, -halfHeight);
    push(points[index]!, -halfHeight);
    push(points[index + 1]!, -halfHeight);
  }
  for (let index = 0; index < points.length; index += 1) {
    const next = (index + 1) % points.length;
    push(points[index]!, -halfHeight);
    push(points[index]!, halfHeight);
    push(points[next]!, halfHeight);
    push(points[index]!, -halfHeight);
    push(points[next]!, halfHeight);
    push(points[next]!, -halfHeight);
  }
  const geometry = new BufferGeometry();
  geometry.setAttribute("position", new Float32BufferAttribute(vertices, 3));
  geometry.computeVertexNormals();
  geometry.computeBoundingBox();
  geometry.computeBoundingSphere();
  return geometry;
}

/** Clips a convex plan polygon to the selected side of one exact seam line. */
function clipPlanPolygonToSeam(
  polygon: readonly ThreePlanPointMm[],
  seam: Readonly<{
    originXMm: number;
    originZMm: number;
    directionX: number;
    directionZ: number;
  }>,
  keepPoint: ThreePlanPointMm
): readonly ThreePlanPointMm[] {
  const signedDistance = (point: ThreePlanPointMm): number =>
    seam.directionX * (point.zMm - seam.originZMm) -
    seam.directionZ * (point.xMm - seam.originXMm);
  const keepPositive = signedDistance(keepPoint) >= 0;
  const inside = (distance: number): boolean => keepPositive
    ? distance >= -0.0001
    : distance <= 0.0001;
  const result: ThreePlanPointMm[] = [];
  for (let index = 0; index < polygon.length; index += 1) {
    const current = polygon[index]!;
    const previous = polygon[(index + polygon.length - 1) % polygon.length]!;
    const currentDistance = signedDistance(current);
    const previousDistance = signedDistance(previous);
    const currentInside = inside(currentDistance);
    const previousInside = inside(previousDistance);
    if (currentInside !== previousInside) {
      const ratio = previousDistance / (previousDistance - currentDistance);
      result.push({
        xMm: previous.xMm + (current.xMm - previous.xMm) * ratio,
        zMm: previous.zMm + (current.zMm - previous.zMm) * ratio
      });
    }
    if (currentInside) result.push(current);
  }
  return result;
}

/**
 * Extends and clips a member-local horizontal wall/trim rectangle at every
 * connected corner's shared angle-bisector. Adjacent members therefore meet
 * at one mathematical seam instead of overlapping coplanar boxes.
 */
function resolveMiteredAssemblyRectangle(
  geometry: ResolvedFabricationAssemblyGeometry,
  instance: ResolvedFabricationAssemblyGeometry["planInstances"][number],
  input: Readonly<{ minXMm: number; maxXMm: number; minZMm: number; maxZMm: number }>
): Readonly<{
  points: readonly ThreePlanPointMm[];
  jointIds: readonly string[];
}> {
  const radians = instance.rotationYDeg * Math.PI / 180;
  const cosine = Math.cos(radians);
  const sine = Math.sin(radians);
  const toLocalPoint = (xMm: number, zMm: number): ThreePlanPointMm => {
    const deltaX = xMm - instance.originXMm;
    const deltaZ = zMm - instance.originZMm;
    return {
      xMm: cosine * deltaX - sine * deltaZ - instance.widthMm / 2,
      zMm: sine * deltaX + cosine * deltaZ
    };
  };
  const toLocalDirection = (x: number, z: number): Readonly<{ x: number; z: number }> => ({
    x: cosine * x - sine * z,
    z: sine * x + cosine * z
  });
  const seams = geometry.assembly.joints.flatMap((joint) => {
    if (joint.jointType !== "corner_joint") return [];
    const edge = joint.firstInstanceId === instance.instanceId
      ? joint.firstEdge
      : joint.secondInstanceId === instance.instanceId
        ? joint.secondEdge
        : undefined;
    if (edge !== "left" && edge !== "right") return [];
    const resolved = geometry.planJoints.find((candidate) => candidate.jointId === joint.objectId);
    const corner = resolved?.cornerInterface;
    if (!corner) return [];
    const origin = toLocalPoint(corner.seam.originXMm, corner.seam.originZMm);
    const direction = toLocalDirection(corner.seam.directionX, corner.seam.directionZ);
    return [{
      jointId: String(joint.objectId),
      edge,
      seam: {
        originXMm: origin.xMm,
        originZMm: origin.zMm,
        directionX: direction.x,
        directionZ: direction.z
      }
    }];
  });
  if (seams.length === 0) {
    return {
      points: [
        { xMm: input.minXMm, zMm: input.minZMm },
        { xMm: input.maxXMm, zMm: input.minZMm },
        { xMm: input.maxXMm, zMm: input.maxZMm },
        { xMm: input.minXMm, zMm: input.maxZMm }
      ],
      jointIds: []
    };
  }
  let minXMm = input.minXMm;
  let maxXMm = input.maxXMm;
  for (const { edge, seam } of seams) {
    if (Math.abs(seam.directionZ) <= 0.000001) continue;
    const xAtMinZ = seam.originXMm +
      seam.directionX * (input.minZMm - seam.originZMm) / seam.directionZ;
    const xAtMaxZ = seam.originXMm +
      seam.directionX * (input.maxZMm - seam.originZMm) / seam.directionZ;
    if (edge === "left") minXMm = Math.min(minXMm, xAtMinZ, xAtMaxZ);
    else maxXMm = Math.max(maxXMm, xAtMinZ, xAtMaxZ);
  }
  let points: readonly ThreePlanPointMm[] = [
    { xMm: minXMm, zMm: input.minZMm },
    { xMm: maxXMm, zMm: input.minZMm },
    { xMm: maxXMm, zMm: input.maxZMm },
    { xMm: minXMm, zMm: input.maxZMm }
  ];
  for (const { seam } of seams) {
    points = clipPlanPolygonToSeam(points, seam, { xMm: 0, zMm: 0 });
  }
  if (points.length < 3) throw new Error(`Assembly member ${instance.instanceId} miter is degenerate.`);
  return { points, jointIds: seams.map(({ jointId }) => jointId) };
}

/** Gives coincident precision contacts a deterministic raster priority. */
function createStableContactMaterial(
  source: MeshStandardMaterial,
  priority: number
): MeshStandardMaterial {
  const material = source.clone();
  material.polygonOffset = true;
  material.polygonOffsetFactor = -0.5;
  material.polygonOffsetUnits = -Math.max(1, priority);
  return material;
}

/** Compatibility exports keep existing renderer consumers source-compatible. */
export { inspectManagedGltfAsset };
export type { ManagedGltfInspection };

/**
 * Preloaded texture bundle supplied by an asset adapter to the pure scene builder.
 *
 * Texture bytes and URLs stay outside the design document. The immutable ID and
 * hash let the adapter prove which reviewed bundle it loaded; the scene cache
 * clones maps before use so disposing a scene never damages registry prototypes.
 *
 * @example A wood texture set supplies colour, normal and roughness maps.
 * @since 0.10.4
 * @modified 2026-09-18 - Added controlled texture-set injection for APPEAR-002.
 */
export interface ThreeLoadedTextureSetAsset {
  readonly textureSetId: string;
  readonly contentHash: string;
  readonly colorMap?: Texture;
  readonly normalMap?: Texture;
  readonly roughnessMap?: Texture;
  readonly metalnessMap?: Texture;
}

/**
 * Prevalidated GLB/glTF scene supplied to the renderer by the managed loader.
 *
 * The root is a prototype and is never attached directly. Every scene build
 * creates owned geometry/material/texture clones, preserving safe GPU lifetime.
 *
 * @since 0.10.4
 * @modified 2026-09-18 - Added version/hash-bound component asset injection.
 */
export interface ThreeLoadedComponentModelAsset {
  readonly assetId: string;
  readonly contentHash: string;
  readonly root: Object3D;
}

/**
 * In-memory registry of reviewed textures and component model prototypes.
 *
 * Registration rejects an ID being rebound to different content. This prevents
 * an open historical design from silently changing because a remote file was
 * replaced under the same asset ID.
 *
 * @example A project loader registers one handle GLB before rebuilding Three.
 * @since 0.10.4
 * @modified 2026-09-18 - Added deterministic runtime asset lookup.
 */
export class ThreeVisualAssetRegistry {
  readonly #textureSets = new Map<string, ThreeLoadedTextureSetAsset>();
  readonly #componentModels = new Map<string, ThreeLoadedComponentModelAsset>();

  /** Registers or reuses one immutable texture set. */
  registerTextureSet(asset: ThreeLoadedTextureSetAsset): void {
    this.#register(this.#textureSets, asset.textureSetId, asset.contentHash, asset);
  }

  /** Registers or reuses one immutable component model. */
  registerComponentModel(asset: ThreeLoadedComponentModelAsset): void {
    this.#register(this.#componentModels, asset.assetId, asset.contentHash, asset);
  }

  /** Returns a texture set by its immutable catalog/version ID. */
  resolveTextureSet(textureSetId: string): ThreeLoadedTextureSetAsset | undefined {
    return this.#textureSets.get(textureSetId.trim());
  }

  /** Returns a component prototype by its immutable asset ID. */
  resolveComponentModel(assetId: string): ThreeLoadedComponentModelAsset | undefined {
    return this.#componentModels.get(assetId.trim());
  }

  /**
   * Releases all registry-owned prototype geometry, materials and textures.
   *
   * Scene builds clone these resources, so disposing the workspace registry is
   * independent from disposing the current rendered scene. Duplicate prototype
   * roots or texture objects are released only once.
   *
   * @example The desktop/mobile shell calls `dispose()` before replacing itself.
   * @since 0.10.16
   * @modified 2026-09-18 - Added lifecycle ownership for reloaded managed assets.
   */
  dispose(): void {
    const roots = new Set([...this.#componentModels.values()].map((asset) => asset.root));
    roots.forEach((root) => disposeObjectTree(root));
    const textures = new Set<Texture>();
    for (const textureSet of this.#textureSets.values()) {
      if (textureSet.colorMap) textures.add(textureSet.colorMap);
      if (textureSet.normalMap) textures.add(textureSet.normalMap);
      if (textureSet.roughnessMap) textures.add(textureSet.roughnessMap);
      if (textureSet.metalnessMap) textures.add(textureSet.metalnessMap);
    }
    textures.forEach((texture) => texture.dispose());
    this.#componentModels.clear();
    this.#textureSets.clear();
  }

  /**
   * Stores one immutable entry while rejecting ID/hash rebinding.
   *
   * @param target Registry map for the asset class.
   * @param id Stable managed ID.
   * @param hash Verified SHA-256 identity.
   * @param asset Entry to retain.
   * @since 0.10.4
   * @modified 2026-09-18 - Added shared registry collision protection.
   */
  #register<T extends { readonly contentHash: string }>(
    target: Map<string, T>,
    id: string,
    hash: string,
    asset: T
  ): void {
    const normalizedId = id.trim();
    const normalizedHash = hash.trim().toLowerCase();
    if (!normalizedId || !/^sha256:[0-9a-f]{64}$/.test(normalizedHash)) {
      throw new Error("Managed visual assets require a non-empty ID and sha256 content hash.");
    }
    const existing = target.get(normalizedId);
    if (existing && existing.contentHash.toLowerCase() !== normalizedHash) {
      throw new Error(`Visual asset ${normalizedId} cannot be rebound to different content.`);
    }
    if (!existing) target.set(normalizedId, asset);
  }
}

/**
 * Loads already-inspected GLB/glTF bytes into an unattached Three.js prototype.
 *
 * The loader is dynamically imported so projects without custom assets do not
 * pay for it in the main bundle. All external URIs were rejected beforehand;
 * parsing therefore cannot escape the hash-covered byte payload.
 *
 * @param input Managed ID, immutable hash and exact asset bytes.
 * @returns Registry-ready scene prototype plus inspection evidence.
 * @since 0.10.4
 * @modified 2026-09-18 - Added the first real controlled GLB/glTF loading path.
 */
export async function loadManagedGltfComponentAsset(input: Readonly<{
  assetId: string;
  contentHash: string;
  bytes: ArrayBuffer;
}>): Promise<Readonly<{
  asset: ThreeLoadedComponentModelAsset;
  inspection: ManagedGltfInspection;
}>> {
  const assetId = input.assetId.trim();
  if (!assetId) throw new Error("Managed component asset ID must not be empty.");
  const inspection = await inspectManagedGltfAsset({
    bytes: input.bytes,
    expectedContentHash: input.contentHash
  });
  const { GLTFLoader } = await import("three/addons/loaders/GLTFLoader.js");
  const gltf = await new Promise<GLTF>((resolve, reject) => {
    const loader = new GLTFLoader();
    loader.parse(input.bytes, "", resolve, reject);
  });
  return {
    asset: {
      assetId,
      contentHash: inspection.contentHash,
      root: gltf.scene
    },
    inspection
  };
}

/**
 * Reuses Three.js materials for equal versioned appearance snapshots.
 *
 * The cache is scoped to one scene build. Meshes with equal render keys share
 * a `MeshStandardMaterial`; `disposeObjectTree` later deduplicates and releases
 * those shared instances exactly once. Domain snapshots never store GPU state.
 *
 * @example Frame segments with the same outside finish share one material.
 * @since 0.10.3
 * @modified 2026-09-17 - Added APPEAR-002 PBR material reuse and trace metadata.
 */
export class ThreeAppearanceMaterialCache {
  readonly #materials = new Map<string, MeshStandardMaterial>();

  /**
   * Creates a scene-local cache optionally backed by prevalidated texture data.
   *
   * @param visualAssets Runtime registry populated by the application asset adapter.
   * @since 0.10.4
   * @modified 2026-09-18 - Added managed PBR texture injection.
   */
  constructor(readonly visualAssets?: ThreeVisualAssetRegistry) {}

  /**
   * Resolves or creates one cached PBR material.
   *
   * @param appearance Versioned renderer-neutral appearance snapshot.
   * @returns A shared Three.js material carrying traceable appearance metadata.
   * @since 0.10.3
   * @modified 2026-09-18 - Added texture-snapshot hash verification and diagnostics.
   */
  resolve(appearance: VisualAppearanceSnapshot): MeshStandardMaterial {
    const style = resolveAppearanceRenderStyle(appearance);
    const resolvedTextureSet = style.textureSetId
      ? this.visualAssets?.resolveTextureSet(style.textureSetId)
      : undefined;
    const textureHashMismatch = Boolean(
      resolvedTextureSet &&
      style.textureContentHash &&
      resolvedTextureSet.contentHash.toLowerCase() !== style.textureContentHash.toLowerCase()
    );
    const textureSet = textureHashMismatch ? undefined : resolvedTextureSet;
    const cacheKey = `${style.cacheKey}|texture-content:${textureSet?.contentHash ?? "none"}`;
    const cached = this.#materials.get(cacheKey);
    if (cached) return cached;
    const material = new MeshStandardMaterial({
      color: style.colorHex,
      metalness: style.metalness,
      roughness: style.roughness,
      opacity: style.opacity,
      transparent: style.transparent,
      depthWrite: !style.transparent
    });
    const cloneTexture = (source: Texture, colorTexture = false): Texture => {
      const owned = source.clone();
      owned.wrapS = RepeatWrapping;
      owned.wrapT = RepeatWrapping;
      owned.repeat.set(style.uvScale?.x ?? 1, style.uvScale?.y ?? 1);
      if (colorTexture) owned.colorSpace = SRGBColorSpace;
      owned.needsUpdate = true;
      return owned;
    };
    if (textureSet?.colorMap) material.map = cloneTexture(textureSet.colorMap, true);
    if (textureSet?.normalMap) material.normalMap = cloneTexture(textureSet.normalMap);
    if (textureSet?.roughnessMap) material.roughnessMap = cloneTexture(textureSet.roughnessMap);
    if (textureSet?.metalnessMap) material.metalnessMap = cloneTexture(textureSet.metalnessMap);
    material.name = `appearance:${style.appearanceId}@${style.appearanceVersion}`;
    material.userData.appearanceId = style.appearanceId;
    material.userData.appearanceVersion = style.appearanceVersion;
    material.userData.materialFamily = style.materialFamily;
    material.userData.appearanceCacheKey = cacheKey;
    material.userData.textureSetId = style.textureSetId;
    material.userData.textureContentHash = textureSet?.contentHash;
    material.userData.textureMissing = Boolean(style.textureSetId && !textureSet);
    material.userData.textureExpectedContentHash = style.textureContentHash;
    material.userData.textureHashMismatch = textureHashMismatch;
    material.userData.finishCode = style.finishCode;
    material.userData.uvScale = style.uvScale ? { ...style.uvScale } : undefined;
    this.#materials.set(cacheKey, material);
    return material;
  }

  /**
   * Resolves BoxGeometry's four edges, exterior face and interior face.
   *
   * Three.js BoxGeometry material groups are ordered +X, -X, +Y, -Y, +Z,
   * -Z. DoorMes defines +Z as outdoors, so the last two entries deliberately
   * map to outside and inside while the first four use the explicit edge finish.
   *
   * @param surface Inside/outside/edge appearance assignment.
   * @returns Six materials in BoxGeometry group order.
   * @since 0.10.3
   * @modified 2026-09-17 - Added direction-correct dual-face materials.
   */
  resolveBoxSurface(surface: SurfaceAppearanceAssignment): MeshStandardMaterial[] {
    const edge = this.resolve(surface.edge);
    return [
      edge,
      edge,
      edge,
      edge,
      this.resolve(surface.outside),
      this.resolve(surface.inside)
    ];
  }

  /** Number of distinct GPU material definitions allocated by this cache. */
  get size(): number {
    return this.#materials.size;
  }
}

/**
 * Builds a local parametric hardware model or a managed GLB/glTF instance.
 *
 * Algorithm: convert the declared physical envelope to metres, place generated
 * primitives inside that envelope, then offset the content so the declared
 * pivot becomes local origin. Reviewed external geometry is cloned, centred and
 * scaled into the same envelope, while DoorMes appearance replaces embedded
 * asset materials. Missing, mismatched or empty assets retain a visible box and
 * traceable fallback reason instead of silently dropping the component.
 *
 * @param model Versioned hardware model snapshot selected for one mount.
 * @param materialCache Scene-local material cache.
 * @returns A pivot-centred group ready to attach to a frame or moving sash.
 * @example `lever-handle` produces a backplate and projecting lever, not one Box.
 * @since 0.10.3
 * @modified 2026-09-18 - Added calibrated axes and hash-bound per-tier LOD selection.
 */
function createThreeHardwareModel(
  model: HardwareComponentModelSnapshot,
  materialCache: ThreeAppearanceMaterialCache,
  visualAssets?: ThreeVisualAssetRegistry,
  assetQuality: ThreeVisualAssetQuality = "high"
): Group {
  const root = new Group();
  const content = new Group();
  const material = materialCache.resolve(model.appearance);
  const width = model.dimensionsMm.widthMm * MILLIMETRES_TO_METRES;
  const height = model.dimensionsMm.heightMm * MILLIMETRES_TO_METRES;
  const depth = model.dimensionsMm.depthMm * MILLIMETRES_TO_METRES;
  content.position.set(
    (0.5 - model.mount.pivotRatio.x) * width,
    (0.5 - model.mount.pivotRatio.y) * height,
    (0.5 - model.mount.pivotRatio.z) * depth
  );
  root.add(content);

  /**
   * Adds one generated mesh and records its owning visual model.
   *
   * @param geometry Primitive already sized in metres.
   * @param position Optional local offset inside the declared model envelope.
   * @returns The traceable mesh added to the pivot-offset content group.
   * @example A lever adds separate backplate and arm meshes with one model ID.
   * @since 0.10.3
   * @modified 2026-09-17 - Added reusable parametric-model part construction.
   */
  const addMesh = (
    geometry: BufferGeometry,
    position: Readonly<{ x?: number; y?: number; z?: number }> = {}
  ): Mesh => {
    const mesh = new Mesh(geometry, material);
    mesh.position.set(position.x ?? 0, position.y ?? 0, position.z ?? 0);
    mesh.castShadow = true;
    mesh.receiveShadow = true;
    mesh.userData.modelId = model.modelId;
    mesh.userData.modelVersion = model.modelVersion;
    content.add(mesh);
    return mesh;
  };

  if (model.geometry.kind === "gltf") {
    const primary = {
      assetId: model.geometry.assetId,
      expectedHash: model.geometry.contentHash.trim().toLowerCase(),
      quality: "high" as const
    };
    const pinnedLods = model.geometry.lodAssets ?? [];
    const medium = pinnedLods.find(({ quality }) => quality === "medium");
    const low = pinnedLods.find(({ quality }) => quality === "low");
    const candidates = assetQuality === "low"
      ? [low, medium, primary]
      : assetQuality === "medium"
        ? [medium, primary, low]
        : [primary, medium, low];
    const selected = candidates
      .filter((candidate): candidate is NonNullable<typeof candidate> => Boolean(candidate))
      .map((candidate) => ({ candidate, asset: visualAssets?.resolveComponentModel(candidate.assetId) }))
      .find(({ candidate, asset }) =>
        Boolean(asset) &&
        asset?.contentHash.toLowerCase() === ("expectedHash" in candidate
          ? candidate.expectedHash
          : candidate.contentHash.toLowerCase()));
    if (selected?.asset) {
      const instance = selected.asset.root.clone(true);
      instance.traverse((candidate) => {
        if (!(candidate instanceof Mesh)) return;
        candidate.geometry = candidate.geometry.clone();
        const groupMaterialCount = candidate.geometry.groups.reduce(
          (count: number, group: Readonly<{ materialIndex?: number }>) =>
            Math.max(count, (group.materialIndex ?? 0) + 1),
          1
        );
        candidate.material = groupMaterialCount === 1
          ? material
          : Array.from({ length: groupMaterialCount }, () => material);
        candidate.castShadow = true;
        candidate.receiveShadow = true;
        candidate.userData.modelId = model.modelId;
        candidate.userData.modelVersion = model.modelVersion;
      });
      const basis = resolveComponentAssetImportBasis(model.geometry.importConfiguration);
      const calibrated = new Group();
      calibrated.add(instance);
      calibrated.applyMatrix4(new Matrix4().set(
        basis.right.x * basis.unitScaleToMetres,
        basis.right.y * basis.unitScaleToMetres,
        basis.right.z * basis.unitScaleToMetres,
        0,
        basis.up.x * basis.unitScaleToMetres,
        basis.up.y * basis.unitScaleToMetres,
        basis.up.z * basis.unitScaleToMetres,
        0,
        basis.forward.x * basis.unitScaleToMetres,
        basis.forward.y * basis.unitScaleToMetres,
        basis.forward.z * basis.unitScaleToMetres,
        0,
        0,
        0,
        0,
        1
      ));
      const bounds = new Box3().setFromObject(calibrated);
      const size = bounds.getSize(new Vector3());
      const center = bounds.getCenter(new Vector3());
      const usableBounds = !bounds.isEmpty() && size.x > 1e-9 && size.y > 1e-9 && size.z > 1e-9;
      if (usableBounds) {
        const fitted = new Group();
        calibrated.position.sub(center);
        fitted.scale.set(width / size.x, height / size.y, depth / size.z);
        fitted.add(calibrated);
        content.add(fitted);
        root.userData.assetFallback = false;
        root.userData.resolvedAssetId = selected.asset.assetId;
        root.userData.resolvedAssetHash = selected.asset.contentHash;
        root.userData.resolvedAssetQuality = selected.candidate.quality;
        root.userData.importSourceUnit = model.geometry.importConfiguration?.sourceUnit ?? "meter";
        root.userData.importUpAxis = model.geometry.importConfiguration?.upAxis ?? "+y";
        root.userData.importForwardAxis = model.geometry.importConfiguration?.forwardAxis ?? "+z";
      } else {
        addMesh(new BoxGeometry(Math.max(0.001, width), Math.max(0.001, height), Math.max(0.001, depth)));
        root.userData.assetFallback = true;
        root.userData.assetFallbackReason = "empty-bounds";
      }
    } else {
      addMesh(new BoxGeometry(Math.max(0.001, width), Math.max(0.001, height), Math.max(0.001, depth)));
      root.userData.assetFallback = true;
      root.userData.assetFallbackReason = "missing-or-hash-mismatch";
    }
    root.userData.requestedAssetId = model.geometry.assetId;
    root.userData.requestedAssetHash = model.geometry.contentHash;
    root.userData.assetQuality = assetQuality;
    root.userData.lodAssetIds = [...model.geometry.lodAssetIds];
    root.userData.lodAssetHashes = Object.fromEntries(
      pinnedLods.map(({ quality, contentHash }) => [quality, contentHash])
    );
    root.userData.unverifiedLegacyLodAssetIds = model.geometry.lodAssets
      ? []
      : [...model.geometry.lodAssetIds];
  } else {
    const parameters = model.geometry.parameters;
    switch (model.geometry.primitiveId) {
      case "lever-handle": {
        const plateWidth = Math.min(width, (parameters.plateWidthMm ?? 28) * MILLIMETRES_TO_METRES);
        const plateHeight = Math.min(height, (parameters.plateHeightMm ?? 132) * MILLIMETRES_TO_METRES);
        const leverLength = Math.min(
          Math.max(0.004, width - plateWidth),
          (parameters.leverLengthMm ?? 108) * MILLIMETRES_TO_METRES
        );
        const plateX = -width / 2 + plateWidth / 2;
        addMesh(
          new BoxGeometry(plateWidth, plateHeight, Math.max(0.004, depth * 0.46)),
          { x: plateX, z: -depth * 0.2 }
        );
        addMesh(
          new BoxGeometry(Math.max(0.004, leverLength), Math.max(0.008, height * 0.12), Math.max(0.006, depth * 0.34)),
          { x: plateX + plateWidth / 2 + leverLength / 2, z: depth * 0.2 }
        );
        break;
      }
      case "round-knob": {
        addMesh(new SphereGeometry(Math.max(0.003, Math.min(width, height, depth) / 2), 20, 12));
        break;
      }
      case "hinge-leaf": {
        const barrelRadius = Math.max(0.002, Math.min(width, depth) * 0.2);
        addMesh(new BoxGeometry(Math.max(0.002, width * 0.42), height, Math.max(0.002, depth * 0.35)), {
          x: -width * 0.27
        });
        addMesh(new BoxGeometry(Math.max(0.002, width * 0.42), height, Math.max(0.002, depth * 0.35)), {
          x: width * 0.27
        });
        addMesh(new CylinderGeometry(barrelRadius, barrelRadius, height, 16));
        break;
      }
      case "lock-point": {
        const pin = addMesh(new CylinderGeometry(
          Math.max(0.002, Math.min(width, height) / 2),
          Math.max(0.002, Math.min(width, height) / 2),
          Math.max(0.002, depth),
          16
        ));
        pin.rotation.x = Math.PI / 2;
        break;
      }
      case "keeper": {
        addMesh(new BoxGeometry(width, Math.max(0.002, height * 0.28), depth), { y: height * 0.36 });
        addMesh(new BoxGeometry(width, Math.max(0.002, height * 0.28), depth), { y: -height * 0.36 });
        break;
      }
      case "shoot-bolt": {
        addMesh(new BoxGeometry(width, Math.max(0.002, height * 0.62), depth), { y: -height * 0.12 });
        addMesh(new CylinderGeometry(
          Math.max(0.0015, width * 0.18),
          Math.max(0.0015, width * 0.18),
          Math.max(0.003, height * 0.38),
          12
        ), { y: height * 0.38 });
        break;
      }
      case "box":
      case "custom-parametric":
        addMesh(new BoxGeometry(Math.max(0.001, width), Math.max(0.001, height), Math.max(0.001, depth)));
        break;
    }
  }
  root.name = `hardware-model:${model.modelId}`;
  root.userData.modelId = model.modelId;
  root.userData.modelVersion = model.modelVersion;
  root.userData.geometryKind = model.geometry.kind;
  root.userData.fallbackSymbolId = model.fallbackSymbolId;
  root.userData.productionStatus = model.productionStatus;
  root.userData.materialCode = model.materialCode;
  root.userData.machiningTemplateId = model.machiningTemplateId;
  return root;
}

/**
 * Optional runtime and presentation inputs applied while projecting a design into Three.js.
 *
 * The map is keyed by `createOpeningPanelKey`. Its values override only the
 * instantaneous visual pose and never change the document, revision or BOM.
 * When absent, persisted configured targets preserve legacy preview behaviour.
 *
 * @example `{ openingProgressPercentByPanelKey: { "CELL-1::P1": 25 } }`.
 * @since 0.8.0
 * @modified 2026-09-18 - Added controlled visual assets and PC/mobile quality selection.
 */
/**
 * Presentation-only asset detail tier selected by the desktop/mobile shell.
 *
 * The tier changes which immutable geometry prototype is used; declared model
 * dimensions, pivot, collision envelope, process anchor and BOM identity remain
 * unchanged. Mobile may therefore choose `low` without changing business data.
 *
 * @since 0.10.4
 * @modified 2026-09-18 - Added shared LOD selection semantics.
 */
export type ThreeVisualAssetQuality = "high" | "medium" | "low";

export interface ThreeDesignSceneBuildOptions {
  readonly openingProgressPercentByPanelKey?: Readonly<Record<string, number>>;
  readonly openingMotionModeByPanelKey?: Readonly<Record<string, OpeningMotionMode>>;
  readonly openingConnectionPresetByPanelKey?: Readonly<Record<string, OpeningConnectionLayoutPreset>>;
  /** Defaults to true; shells may hide engineering marks without changing motion. */
  readonly showOpeningAngleAnnotations?: boolean;
  /** Adds non-manufacturing 3D dimension labels when a related object is selected. */
  readonly showLinearDimensions?: boolean;
  /** Stable selected object that scopes 3D labels to one window or component. */
  readonly linearDimensionObjectId?: string;
  /**
   * Shows the non-BOM host wall/opening generated from Installation data.
   * Defaults to false because the core workspace is a factory product preview;
   * store/installation review must opt into environmental geometry explicitly.
   */
  readonly showInstallationHost?: boolean;
  /**
   * Optional active window for installation-host review. When omitted, an
   * explicitly enabled host is shown for every window for headless scene uses.
   */
  readonly installationHostWindowId?: string;
  /** Shows enabled package/liner material pieces independently from the host wall. */
  readonly showInstallationSurround?: boolean;
  /** Supplies only prevalidated texture/model prototypes; raw URLs are never accepted here. */
  readonly visualAssets?: ThreeVisualAssetRegistry;
  /** Selects primary or lighter managed model variants without changing design/BOM identity. */
  readonly assetQuality?: ThreeVisualAssetQuality;
}

/**
 * Creates a camera-facing degree label without coupling the scene builder to UI HTML.
 *
 * Browsers receive a canvas-backed sprite that remains readable while orbiting;
 * non-DOM unit tests still receive the same sprite metadata without allocating a
 * canvas. The texture is later released by `disposeObjectTree` together with its
 * material, preventing preview animation from leaking GPU resources.
 *
 * @param text Actual opening angle or engineering dimension label.
 * @param wide Whether to reserve a wider billboard for a dimension label.
 * @returns One non-pickable sprite carrying the same text in `userData`.
 * @example `createOpeningAngleLabelSprite("90°")` produces a 3D overlay label.
 * @since 0.9.2
 * @modified 2026-09-17 - Added a wide engineering-label variant for 3D rulers.
 */
function createOpeningAngleLabelSprite(text: string, wide = false): Sprite {
  let texture: CanvasTexture | undefined;
  if (typeof document !== "undefined") {
    const canvas = document.createElement("canvas");
    canvas.width = 320;
    canvas.height = 112;
    const context = canvas.getContext("2d");
    if (context) {
      context.clearRect(0, 0, canvas.width, canvas.height);
      context.fillStyle = "rgba(248,250,252,0.92)";
      context.strokeStyle = "#1677ff";
      context.lineWidth = 5;
      context.beginPath();
      context.roundRect(4, 4, canvas.width - 8, canvas.height - 8, 18);
      context.fill();
      context.stroke();
      context.fillStyle = "#0f4c81";
      context.font = `600 ${wide ? 36 : 52}px system-ui, sans-serif`;
      context.textAlign = "center";
      context.textBaseline = "middle";
      context.fillText(text, canvas.width / 2, canvas.height / 2 + 2);
      texture = new CanvasTexture(canvas);
      texture.colorSpace = SRGBColorSpace;
    }
  }
  const spriteMaterial = new SpriteMaterial({
    transparent: true,
    depthTest: false,
    depthWrite: false
  });
  if (texture) spriteMaterial.map = texture;
  const sprite = new Sprite(spriteMaterial);
  sprite.name = "opening-angle-label";
  sprite.scale.set(wide ? 0.34 : 0.24, 0.084, 1);
  sprite.renderOrder = 1002;
  sprite.userData.objectType = "opening-angle-label";
  sprite.userData.labelText = text;
  return sprite;
}

/**
 * Builds the non-manufacturing indoor/outdoor compass shown on the 3D floor.
 *
 * DoorMes uses one coordinate convention in geometry, opening kinematics and
 * rendering: +Z is outdoors and -Z is indoors. Two floor arrows and readable
 * labels make that convention visible while orbiting the model; they carry no
 * stable design ID, cannot enter BOM/selection, and never alter sash motion.
 *
 * @param halfDepthMetres Distance from the model centre to each arrow tip.
 * @returns A disposable Three.js helper centred at X=0 on a caller-owned floor Y.
 * @example `createThreeGroundOrientationGuide(1.2)` points outside toward +Z.
 * @since 0.10.70
 * @modified 2026-09-21 - Added explicit 3D indoor/outdoor ground orientation.
 */
export function createThreeGroundOrientationGuide(halfDepthMetres = 1.1): Group {
  const extent = Math.max(0.55, halfDepthMetres);
  const group = new Group();
  group.name = "ground-indoor-outdoor-guide";
  group.userData.objectType = "ground-orientation-guide";
  group.userData.coordinateConvention = "+Z-outside/-Z-inside";
  group.userData.excludeFromBom = true;
  group.userData.excludeFromPicking = true;

  const addDirection = (input: Readonly<{
    name: "outside" | "inside";
    label: "室外" | "室内";
    directionZ: 1 | -1;
    color: number;
  }>): void => {
    const tipZ = input.directionZ * extent;
    const arrowBackZ = tipZ - input.directionZ * 0.13;
    const geometry = new BufferGeometry().setFromPoints([
      new Vector3(0, 0.004, 0),
      new Vector3(0, 0.004, tipZ),
      new Vector3(-0.075, 0.004, arrowBackZ),
      new Vector3(0, 0.004, tipZ),
      new Vector3(0.075, 0.004, arrowBackZ)
    ]);
    const arrow = new Line(geometry, new LineBasicMaterial({ color: input.color }));
    arrow.name = `ground-orientation-${input.name}-arrow`;
    arrow.userData.objectType = "ground-orientation-arrow";
    arrow.userData.directionZ = input.directionZ;
    arrow.userData.excludeFromBom = true;
    arrow.userData.excludeFromPicking = true;

    const label = createOpeningAngleLabelSprite(input.label, true);
    label.name = `ground-orientation-${input.name}-label`;
    label.position.set(0, 0.07, tipZ + input.directionZ * 0.2);
    label.scale.set(0.38, 0.105, 1);
    label.userData.objectType = "ground-orientation-label";
    label.userData.directionZ = input.directionZ;
    label.userData.excludeFromBom = true;
    label.userData.excludeFromPicking = true;
    group.add(arrow, label);
  };
  addDirection({ name: "outside", label: "室外", directionZ: 1, color: 0x1677ff });
  addDirection({ name: "inside", label: "室内", directionZ: -1, color: 0xc45d00 });
  return group;
}

/**
 * Builds stationary 3D construction marks for one moving sash.
 *
 * Algorithm: side hinges map the shared polar arc into X/Z below the sash;
 * horizontal hinges map it into Y/Z beside the sash. Start/current rays and the
 * dashed arc remain attached to the fixed assembly group while the sash rotates.
 * The numeric label uses the exact pose component, not a screen-derived angle.
 *
 * @param input Window-relative pivot, dimensions and resolved pose rotation.
 * @returns A fixed group containing two rays, a dashed arc and billboard label.
 * @example A right-hinged fully open reference sash displays a 90° quarter-circle.
 * @since 0.9.2
 * @modified 2026-09-17 - Added cross-view 3D angle construction marks.
 */
function createThreeOpeningAngleAnnotation(input: Readonly<{
  previewKey: string;
  pivotEdge: OpeningHingeEdge;
  pivotXMm: number;
  pivotYMm: number;
  openingXmm: number;
  openingYmm: number;
  openingWidthMm: number;
  openingHeightMm: number;
  windowWidthMetres: number;
  windowHeightMetres: number;
  closedSashCenterZ: number;
  rotationRadians: Readonly<{ x: number; y: number; z: number }>;
}>): Group {
  const sideHinge = input.pivotEdge === "left" || input.pivotEdge === "right";
  const startAngleRadians = sideHinge
    ? input.pivotEdge === "left" ? 0 : Math.PI
    : input.pivotEdge === "top" ? Math.PI : 0;
  const sweepAngleRadians = sideHinge
    ? -input.rotationRadians.y
    : input.rotationRadians.x;
  const radius = (sideHinge
    ? Math.max(80, Math.min(260, input.openingWidthMm * 0.28))
    : Math.max(80, Math.min(240, input.openingHeightMm * 0.18))) * MILLIMETRES_TO_METRES;
  const sampled = sampleOpeningAngleArc({ radius, startAngleRadians, sweepAngleRadians });
  const mapPoint = (point: Readonly<{ radialX: number; radialY: number }>): Vector3 =>
    sideHinge
      ? new Vector3(point.radialX, 0, point.radialY)
      : new Vector3(0, point.radialX, point.radialY);
  const points = sampled.map(mapPoint);
  const origin = new Vector3();
  const group = new Group();
  group.name = `opening-angle:${input.previewKey}`;
  group.position.set(
    sideHinge
      ? input.pivotXMm * MILLIMETRES_TO_METRES - input.windowWidthMetres / 2
      : (input.openingXmm - 60) * MILLIMETRES_TO_METRES - input.windowWidthMetres / 2,
    sideHinge
      ? input.windowHeightMetres / 2 -
        (input.openingYmm + input.openingHeightMm + 60) * MILLIMETRES_TO_METRES
      : input.windowHeightMetres / 2 - input.pivotYMm * MILLIMETRES_TO_METRES,
    input.closedSashCenterZ
  );
  const rayMaterial = new LineBasicMaterial({
    color: 0x0f4c81,
    depthTest: false,
    transparent: true,
    opacity: 0.9
  });
  const closedRay = new Line(
    new BufferGeometry().setFromPoints([origin, points[0] ?? origin]),
    rayMaterial
  );
  closedRay.name = "opening-angle-ray-closed";
  closedRay.renderOrder = 1000;
  const currentRay = new Line(
    new BufferGeometry().setFromPoints([origin, points.at(-1) ?? origin]),
    new LineBasicMaterial({ color: 0x1677ff, depthTest: false })
  );
  currentRay.name = "opening-angle-ray-current";
  currentRay.renderOrder = 1000;
  const arc = new Line(
    new BufferGeometry().setFromPoints(points),
    new LineDashedMaterial({
      color: 0x1677ff,
      dashSize: 0.025,
      gapSize: 0.014,
      depthTest: false,
      transparent: true,
      opacity: 0.95
    })
  );
  arc.name = "opening-angle-arc-dashed";
  arc.computeLineDistances();
  arc.renderOrder = 1001;
  const angleDegrees = Math.abs(sweepAngleRadians) * 180 / Math.PI;
  const label = createOpeningAngleLabelSprite(`${Number(angleDegrees.toFixed(1))}°`);
  const labelAngle = startAngleRadians + sweepAngleRadians / 2;
  const labelPoint = mapPoint({
    radialX: Math.cos(labelAngle) * (radius + 0.065),
    radialY: Math.sin(labelAngle) * (radius + 0.065)
  });
  label.position.copy(labelPoint);
  group.userData.objectType = "opening-angle-annotation";
  group.userData.previewPanelKey = input.previewKey;
  group.userData.pivotEdge = input.pivotEdge;
  group.userData.previewAngleDegrees = angleDegrees;
  group.userData.labelText = label.userData.labelText;
  group.userData.arcDashed = true;
  group.add(closedRay, currentRay, arc, label);
  return group;
}

/**
 * Creates a ground-plane angle mark for one selected spatial corner joint.
 *
 * The rays point from the physical connector axis towards the two adjacent
 * window centres. The mark therefore follows product-space geometry and never
 * derives an angle from the camera view.
 *
 * @since 0.10.88
 */
function createThreeCornerJointAngleAnnotation(input: Readonly<{
  geometry: ResolvedFabricationAssemblyGeometry;
  joint: ResolvedFabricationAssemblyGeometry["planJoints"][number];
  yMetres: number;
}>): Group {
  const first = input.geometry.planInstances.find(
    (candidate) => candidate.instanceId === input.joint.firstInstanceId
  );
  const second = input.geometry.planInstances.find(
    (candidate) => candidate.instanceId === input.joint.secondInstanceId
  );
  if (!first || !second) {
    throw new Error(`Corner joint ${input.joint.jointId} has no adjacent plan instances.`);
  }
  const centre = (
    instance: ResolvedFabricationAssemblyGeometry["planInstances"][number]
  ): { xMm: number; zMm: number } => {
    const total = instance.footprint.reduce(
      (sum, point) => ({ xMm: sum.xMm + point.xMm, zMm: sum.zMm + point.zMm }),
      { xMm: 0, zMm: 0 }
    );
    return {
      xMm: total.xMm / instance.footprint.length,
      zMm: total.zMm / instance.footprint.length
    };
  };
  const unitFromAxis = (point: { xMm: number; zMm: number }): { x: number; z: number } => {
    const dx = point.xMm - input.joint.axisXMm;
    const dz = point.zMm - input.joint.axisZMm;
    const length = Math.max(0.001, Math.hypot(dx, dz));
    return { x: dx / length, z: dz / length };
  };
  const firstUnit = unitFromAxis(centre(first));
  const secondUnit = unitFromAxis(centre(second));
  const signedSweep = Math.atan2(
    firstUnit.x * secondUnit.z - firstUnit.z * secondUnit.x,
    firstUnit.x * secondUnit.x + firstUnit.z * secondUnit.z
  );
  const radius = 0.18;
  const segmentCount = Math.max(8, Math.ceil(Math.abs(signedSweep) / (Math.PI / 24)));
  const startAngle = Math.atan2(firstUnit.z, firstUnit.x);
  const points = Array.from({ length: segmentCount + 1 }, (_, index) => {
    const angle = startAngle + signedSweep * index / segmentCount;
    return new Vector3(Math.cos(angle) * radius, 0, Math.sin(angle) * radius);
  });
  const planCenterXMm = (input.geometry.planBounds.minXMm + input.geometry.planBounds.maxXMm) / 2;
  const planCenterZMm = (input.geometry.planBounds.minZMm + input.geometry.planBounds.maxZMm) / 2;
  const group = new Group();
  group.name = `corner-joint-angle:${input.joint.jointId}`;
  group.position.set(
    (input.joint.axisXMm - planCenterXMm) * MILLIMETRES_TO_METRES,
    input.yMetres,
    (input.joint.axisZMm - planCenterZMm) * MILLIMETRES_TO_METRES
  );
  const origin = new Vector3();
  const firstRay = new Line(
    new BufferGeometry().setFromPoints([origin, points[0] ?? origin]),
    new LineBasicMaterial({ color: 0x1677ff, depthTest: false })
  );
  const secondRay = new Line(
    new BufferGeometry().setFromPoints([origin, points.at(-1) ?? origin]),
    new LineBasicMaterial({ color: 0x1677ff, depthTest: false })
  );
  const arc = new Line(
    new BufferGeometry().setFromPoints(points),
    new LineDashedMaterial({
      color: 0x1677ff,
      dashSize: 0.025,
      gapSize: 0.014,
      depthTest: false,
      transparent: true,
      opacity: 0.95
    })
  );
  firstRay.name = "corner-angle-ray-first";
  secondRay.name = "corner-angle-ray-second";
  arc.name = "corner-angle-arc-dashed";
  arc.computeLineDistances();
  firstRay.renderOrder = secondRay.renderOrder = 1000;
  arc.renderOrder = 1001;
  const middleAngle = startAngle + signedSweep / 2;
  const label = createOpeningAngleLabelSprite(
    `${Number(input.joint.includedAngleDeg.toFixed(1))}°`,
    true
  );
  label.position.set(
    Math.cos(middleAngle) * (radius + 0.085),
    0.035,
    Math.sin(middleAngle) * (radius + 0.085)
  );
  group.userData.objectType = "corner-joint-angle-annotation";
  group.userData.jointId = input.joint.jointId;
  group.userData.includedAngleDeg = input.joint.includedAngleDeg;
  group.userData.turnDirection = input.joint.turnDirection;
  group.userData.labelText = label.userData.labelText;
  group.userData.arcDashed = true;
  group.userData.excludeFromBom = true;
  group.userData.excludeFromPicking = true;
  group.add(firstRay, secondRay, arc, label);
  return group;
}

/**
 * Creates one camera-readable 3D linear dimension from shared millimetre geometry.
 *
 * Algorithm: place one billboard label at the resolved dimension midpoint.
 * The older witness/dimension/tick lines were visually confused with product
 * geometry, so 3D now presents compact selection-scoped values only. Inputs
 * still use renderer-independent millimetre resolvers and never feed values
 * back into the design graph or BOM.
 *
 * @param input Stable dimension identity, measured/dimension endpoints and value.
 * @returns A non-pickable group tagged for visibility tests and future styling.
 * @example Selecting a window places “总宽 1200 mm” below the frame without connector lines.
 * @since 0.9.4
 * @modified 2026-09-20 - Replaced 3D connector rulers with compact labels.
 */
function createThreeLinearDimensionAnnotation(input: Readonly<{
  dimensionId: string;
  kind: "overall-width" | "overall-height" | "frame-depth" |
    "assembly-overall-width" | "assembly-overall-height" | "joint-gap" |
    "assembly-plan-width" | "assembly-plan-depth" | "corner-profile-width" |
    "cell-width" | "cell-height" |
    "component-width" | "component-height" | "component-depth" |
    "opening-width" | "opening-height" | "sash-depth" |
    "hardware-width" | "hardware-height" | "hardware-depth" |
    "wall-thickness" | "frame-wall-offset" |
    "surround-outer-width" | "surround-outer-height" |
    "surround-face-width" | "surround-board-thickness";
  valueMm: number;
  label: string;
  measurementStart: Vector3;
  measurementEnd: Vector3;
  dimensionStart: Vector3;
  dimensionEnd: Vector3;
  tickDirection: Vector3;
  labelOffset: Vector3;
}>): Group {
  const group = new Group();
  group.name = `linear-dimension:${input.dimensionId}`;
  group.userData.objectType = "linear-dimension-annotation";
  group.userData.dimensionId = input.dimensionId;
  group.userData.dimensionKind = input.kind;
  group.userData.dimensionMm = input.valueMm;
  group.userData.labelText = input.label;
  group.userData.presentation = "label-only";
  const label = createOpeningAngleLabelSprite(input.label, true);
  label.name = "linear-dimension-label";
  label.userData.objectType = "linear-dimension-label";
  label.position.copy(input.dimensionStart)
    .add(input.dimensionEnd)
    .multiplyScalar(0.5)
    .add(input.labelOffset);
  group.add(label);
  return group;
}

/**
 * Creates selection-scoped 3D labels for a connected product aggregate.
 *
 * An assembly selection shows only finished overall width/height. A joint
 * selection shows only its persisted connector gap. No witness lines are added,
 * preserving the compact-label 3D rule; child dimensions remain owned by the
 * selected instance or component.
 *
 * @param geometry Shared millimetre assembly geometry.
 * @param selectedObjectId Exact aggregate or joint selected by tree/3D/2D.
 * @returns Label collection attached to the assembly-centred scene group.
 * @example Selecting J1 in a 30mm mullion zone shows only “连接宽 30 mm”.
 * @since 0.10.48
 * @modified 2026-09-21 - Added exact assembly/joint 3D dimension scope.
 */
function createThreeFabricationAssemblyDimensionAnnotations(
  geometry: ResolvedFabricationAssemblyGeometry,
  selectedObjectId: string
): Group {
  const collection = new Group();
  collection.name = `linear-dimensions:${geometry.assembly.objectId}`;
  collection.userData.objectType = "linear-dimension-collection";
  collection.userData.sourceAssemblyId = geometry.assembly.objectId;
  const centerXMm = geometry.bounds.xMm + geometry.bounds.widthMm / 2;
  const centerYMm = geometry.bounds.yMm + geometry.bounds.heightMm / 2;
  const leftX = -geometry.bounds.widthMm / 2 * MILLIMETRES_TO_METRES;
  const rightX = geometry.bounds.widthMm / 2 * MILLIMETRES_TO_METRES;
  const topY = geometry.bounds.heightMm / 2 * MILLIMETRES_TO_METRES;
  const bottomY = -geometry.bounds.heightMm / 2 * MILLIMETRES_TO_METRES;
  const frontZ = 0.12;
  const formatMm = (value: number): string => `${Number(value.toFixed(1))} mm`;
  if (selectedObjectId === geometry.assembly.objectId) {
    if (!geometry.isCoplanar) {
      const planWidth = geometry.planBounds.widthMm * MILLIMETRES_TO_METRES;
      const planDepth = geometry.planBounds.depthMm * MILLIMETRES_TO_METRES;
      collection.add(
        createThreeLinearDimensionAnnotation({
          dimensionId: `${geometry.assembly.objectId}:dimension.plan-width`,
          kind: "assembly-plan-width",
          valueMm: geometry.planBounds.widthMm,
          label: `占地宽 ${formatMm(geometry.planBounds.widthMm)}`,
          measurementStart: new Vector3(-planWidth / 2, bottomY, 0),
          measurementEnd: new Vector3(planWidth / 2, bottomY, 0),
          dimensionStart: new Vector3(-planWidth / 2, bottomY - 0.14, 0),
          dimensionEnd: new Vector3(planWidth / 2, bottomY - 0.14, 0),
          tickDirection: new Vector3(),
          labelOffset: new Vector3(0, -0.04, 0)
        }),
        createThreeLinearDimensionAnnotation({
          dimensionId: `${geometry.assembly.objectId}:dimension.plan-depth`,
          kind: "assembly-plan-depth",
          valueMm: geometry.planBounds.depthMm,
          label: `占地深 ${formatMm(geometry.planBounds.depthMm)}`,
          measurementStart: new Vector3(planWidth / 2, bottomY, -planDepth / 2),
          measurementEnd: new Vector3(planWidth / 2, bottomY, planDepth / 2),
          dimensionStart: new Vector3(planWidth / 2 + 0.16, bottomY, -planDepth / 2),
          dimensionEnd: new Vector3(planWidth / 2 + 0.16, bottomY, planDepth / 2),
          tickDirection: new Vector3(),
          labelOffset: new Vector3(0.06, 0, 0)
        }),
        createThreeLinearDimensionAnnotation({
          dimensionId: `${geometry.assembly.objectId}:dimension.overall-height`,
          kind: "assembly-overall-height",
          valueMm: geometry.bounds.heightMm,
          label: `组合总高 ${formatMm(geometry.bounds.heightMm)}`,
          measurementStart: new Vector3(planWidth / 2, bottomY, 0),
          measurementEnd: new Vector3(planWidth / 2, topY, 0),
          dimensionStart: new Vector3(planWidth / 2 + 0.16, bottomY, 0),
          dimensionEnd: new Vector3(planWidth / 2 + 0.16, topY, 0),
          tickDirection: new Vector3(),
          labelOffset: new Vector3(0.06, 0, 0)
        })
      );
      return collection;
    }
    collection.add(
      createThreeLinearDimensionAnnotation({
        dimensionId: `${geometry.assembly.objectId}:dimension.overall-width`,
        kind: "assembly-overall-width",
        valueMm: geometry.bounds.widthMm,
        label: `组合总宽 ${formatMm(geometry.bounds.widthMm)}`,
        measurementStart: new Vector3(leftX, bottomY, frontZ),
        measurementEnd: new Vector3(rightX, bottomY, frontZ),
        dimensionStart: new Vector3(leftX, bottomY - 0.14, frontZ),
        dimensionEnd: new Vector3(rightX, bottomY - 0.14, frontZ),
        tickDirection: new Vector3(),
        labelOffset: new Vector3(0, -0.06, 0)
      }),
      createThreeLinearDimensionAnnotation({
        dimensionId: `${geometry.assembly.objectId}:dimension.overall-height`,
        kind: "assembly-overall-height",
        valueMm: geometry.bounds.heightMm,
        label: `组合总高 ${formatMm(geometry.bounds.heightMm)}`,
        measurementStart: new Vector3(rightX, bottomY, frontZ),
        measurementEnd: new Vector3(rightX, topY, frontZ),
        dimensionStart: new Vector3(rightX + 0.15, bottomY, frontZ),
        dimensionEnd: new Vector3(rightX + 0.15, topY, frontZ),
        tickDirection: new Vector3(),
        labelOffset: new Vector3(0.07, 0, 0)
      })
    );
    return collection;
  }
  const joint = geometry.joints.find((candidate) => candidate.jointId === selectedObjectId);
  const persistedJoint = geometry.assembly.joints.find(
    (candidate) => candidate.objectId === selectedObjectId
  );
  if (!joint || !persistedJoint) return collection;
  const planJoint = geometry.planJoints.find(
    (candidate) => candidate.jointId === selectedObjectId
  );
  const planCenterXMm = (geometry.planBounds.minXMm + geometry.planBounds.maxXMm) / 2;
  const planCenterZMm = (geometry.planBounds.minZMm + geometry.planBounds.maxZMm) / 2;
  const localX = geometry.isCoplanar || !planJoint
    ? (joint.xMm + joint.widthMm / 2 - centerXMm) * MILLIMETRES_TO_METRES
    : (planJoint.axisXMm - planCenterXMm) * MILLIMETRES_TO_METRES;
  const localY = (centerYMm - joint.yMm - joint.heightMm / 2) * MILLIMETRES_TO_METRES;
  const localZ = geometry.isCoplanar || !planJoint
    ? frontZ
    : (planJoint.axisZMm - planCenterZMm) * MILLIMETRES_TO_METRES;
  if (persistedJoint.jointType === "corner_joint" && planJoint) {
    const angleY = (centerYMm - joint.yMm - joint.heightMm) *
      MILLIMETRES_TO_METRES - 0.04;
    collection.add(createThreeCornerJointAngleAnnotation({
      geometry,
      joint: planJoint,
      yMetres: angleY
    }));
    const profileOrigin = new Vector3(localX, localY, localZ);
    collection.add(createThreeLinearDimensionAnnotation({
      dimensionId: `${joint.jointId}:dimension.corner-profile-width`,
      kind: "corner-profile-width",
      valueMm: persistedJoint.gapMm,
      label: `角柱 ${formatMm(persistedJoint.gapMm)}`,
      measurementStart: profileOrigin,
      measurementEnd: profileOrigin,
      dimensionStart: profileOrigin,
      dimensionEnd: profileOrigin,
      tickDirection: new Vector3(),
      labelOffset: new Vector3(0, 0.14, 0)
    }));
    return collection;
  }
  const verticalZone = joint.heightMm >= joint.widthMm;
  const label = verticalZone ? "连接宽" : "连接高";
  const origin = new Vector3(localX, localY, localZ);
  collection.add(createThreeLinearDimensionAnnotation({
    dimensionId: `${joint.jointId}:dimension.gap`,
    kind: "joint-gap",
    valueMm: persistedJoint.gapMm,
    label: `${label} ${formatMm(persistedJoint.gapMm)}`,
    measurementStart: origin,
    measurementEnd: origin,
    dimensionStart: origin,
    dimensionEnd: origin,
    tickDirection: new Vector3(),
    labelOffset: new Vector3(verticalZone ? 0 : 0.12, verticalZone ? 0.1 : 0, 0)
  }));
  return collection;
}

/**
 * Builds the stationary host wall around one rectangular window opening.
 *
 * Algorithm: resolve the persisted Installation Z section, retain an exact
 * opening the size of the window, and compose top/left/right plus an optional
 * sill-height bottom wall from boxes. The host has no selectable object ID and
 * is explicitly marked `excludeFromBom`; it may occlude the product in Three,
 * but can never become a frame material or manufacturing feature.
 *
 * @param window Shared design window containing installation and sill data.
 * @param widthMetres Product width centred on local X=0.
 * @param heightMetres Product height centred on local Y=0.
 * @returns One non-manufacturing, independently selectable wall group.
 * @example A 300mm wall at custom +45mm has Z bounds -105..195mm.
 * @since 0.9.8
 * @modified 2026-09-20 - Separated wall identity from window/package selection.
 */
function createThreeInstallationHost(
  window: WindowUnit,
  widthMetres: number,
  heightMetres: number,
  materialCache: ThreeAppearanceMaterialCache,
  wallAppearance: VisualAppearanceSnapshot
): Group {
  const installation = normalizeWindowInstallation(window.installation);
  const section = resolveWindowInstallationSection(window);
  const group = new Group();
  group.name = `installation-host:${window.objectId}`;
  group.userData.objectType = "installation-host";
  group.userData.objectId = createWindowInstallationWallObjectId(window.objectId);
  group.userData.sourceWindowId = window.objectId;
  group.userData.excludeFromBom = true;
  group.userData.excludeFromWindowSelectionBounds = true;
  group.userData.coordinateConvention = "+Z exterior / -Z interior";
  group.userData.installation = {
    sillHeightMm: installation.sillHeightMm,
    mountingMode: installation.surround.mountingMode,
    frameAlignment: installation.surround.frameAlignment,
    wallThicknessMm: section.wallThicknessMm,
    wallCenterZMm: section.wallCenterZMm,
    wallOutsideZMm: section.wallOutsideZMm,
    wallInsideZMm: section.wallInsideZMm,
    wallMaterialId: installation.surround.wallMaterialId
  };

  const material = materialCache.resolve(wallAppearance);
  for (const obstacle of resolveWindowInstallationObstacleGeometry(window)) {
    if (obstacle.kind !== "wall") continue;
    const mesh = new Mesh(
      new BoxGeometry(
        Math.max(0.001, (obstacle.max.x - obstacle.min.x) * MILLIMETRES_TO_METRES),
        Math.max(0.001, (obstacle.max.y - obstacle.min.y) * MILLIMETRES_TO_METRES),
        Math.max(0.001, (obstacle.max.z - obstacle.min.z) * MILLIMETRES_TO_METRES)
      ),
      material
    );
    mesh.name = `installation-wall:${obstacle.side}`;
    mesh.position.set(
      (obstacle.min.x + obstacle.max.x) / 2 * MILLIMETRES_TO_METRES,
      (obstacle.min.y + obstacle.max.y) / 2 * MILLIMETRES_TO_METRES,
      (obstacle.min.z + obstacle.max.z) / 2 * MILLIMETRES_TO_METRES
    );
    mesh.castShadow = true;
    mesh.receiveShadow = true;
    mesh.userData.objectType = "installation-wall-part";
    mesh.userData.objectId = createWindowInstallationWallObjectId(window.objectId, obstacle.side);
    mesh.userData.sourceWindowId = window.objectId;
    mesh.userData.installationSide = obstacle.side;
    mesh.userData.obstacleId = obstacle.obstacleId;
    mesh.userData.wallThicknessMm = section.wallThicknessMm;
    mesh.userData.wallCenterZMm = section.wallCenterZMm;
    mesh.userData.wallMaterialId = installation.surround.wallMaterialId;
    mesh.userData.excludeFromBom = true;
    group.add(mesh);
  }
  return group;
}

/**
 * Builds one non-production reference wall around a connected product envelope.
 *
 * The opening is derived once from the assembly graph and its per-edge
 * installation clearances. Child windows never create their own wall inside an
 * assembly; consequently a two-, three- or N-unit straight composition has one
 * continuous host and one opening rather than overlapping wall slabs.
 *
 * @param geometry Canonical assembly placement and recommended opening.
 * @param frameDepthMm Maximum member-frame depth used for Z alignment.
 * @param materialCache Shared appearance material cache.
 * @param wallAppearance Current assembly-preview wall appearance fallback.
 * @returns One selectable, BOM-excluded reference-host group.
 * @example Two side-by-side windows produce top/left/right returns around 2130mm.
 * @since 0.10.46
 * @modified 2026-09-21 - Filled concave L/T assembly voids instead of cutting one false rectangular hole.
 */
function createThreeFabricationAssemblyHost(
  geometry: ResolvedFabricationAssemblyGeometry,
  frameDepthMm: number,
  materialCache: ThreeAppearanceMaterialCache,
  wallAppearance: VisualAppearanceSnapshot
): Group {
  const { assembly, bounds, recommendedOpening } = geometry;
  const installation = normalizeWindowInstallation(assembly.installation);
  const section = resolveInstallationSectionFromFrameDepth(
    installation,
    frameDepthMm
  );
  const centerXMm = bounds.xMm + bounds.widthMm / 2;
  const centerYMm = bounds.yMm + bounds.heightMm / 2;
  const openingLeftMm = recommendedOpening.xMm - centerXMm;
  const openingRightMm = recommendedOpening.xMm + recommendedOpening.widthMm - centerXMm;
  const openingTopMm = centerYMm - recommendedOpening.yMm;
  const openingBottomMm = centerYMm -
    (recommendedOpening.yMm + recommendedOpening.heightMm);
  const wallBandMm = Math.max(
    420,
    Math.min(900, Math.min(recommendedOpening.widthMm, recommendedOpening.heightMm) * 0.35)
  );
  const bottomSupportMm = resolveWindowInstallationBottomSupportMm(installation);
  const floorYMm = openingBottomMm - bottomSupportMm;
  const group = new Group();
  const groupId = createWindowInstallationWallObjectId(assembly.objectId);
  group.name = `installation-host:${assembly.objectId}`;
  group.userData.objectType = "installation-host";
  group.userData.objectId = groupId;
  group.userData.sourceAssemblyId = assembly.objectId;
  group.userData.sourceWindowIds = assembly.instances.map((instance) => instance.windowId);
  group.userData.excludeFromBom = true;
  group.userData.excludeFromWindowSelectionBounds = true;
  group.userData.coordinateConvention = "+Z exterior / -Z interior";
  group.userData.installation = {
    sillHeightMm: installation.sillHeightMm,
    bottomSupportMm,
    mountingMode: installation.surround.mountingMode,
    frameAlignment: installation.surround.frameAlignment,
    wallThicknessMm: section.wallThicknessMm,
    wallCenterZMm: section.wallCenterZMm,
    wallOutsideZMm: section.wallOutsideZMm,
    wallInsideZMm: section.wallInsideZMm,
    wallMaterialId: installation.surround.wallMaterialId,
    openingWidthMm: recommendedOpening.widthMm,
    openingHeightMm: recommendedOpening.heightMm
  };
  const material = materialCache.resolve(wallAppearance);
  const boxes: Array<Readonly<{
    side?: WindowInstallationSide;
    role: "return" | "infill";
    index: number;
    minX: number;
    maxX: number;
    minY: number;
    maxY: number;
  }>> = [
    {
      side: "top",
      role: "return",
      index: 0,
      minX: openingLeftMm - wallBandMm,
      maxX: openingRightMm + wallBandMm,
      minY: openingTopMm,
      maxY: openingTopMm + wallBandMm
    },
    {
      side: "left",
      role: "return",
      index: 1,
      minX: openingLeftMm - wallBandMm,
      maxX: openingLeftMm,
      minY: floorYMm,
      maxY: openingTopMm
    },
    {
      side: "right",
      role: "return",
      index: 2,
      minX: openingRightMm,
      maxX: openingRightMm + wallBandMm,
      minY: floorYMm,
      maxY: openingTopMm
    }
  ];
  if (bottomSupportMm > 0) {
    boxes.push({
      side: "bottom",
      role: "return",
      index: 3,
      minX: openingLeftMm,
      maxX: openingRightMm,
      minY: floorYMm,
      maxY: openingBottomMm
    });
  }
  for (const [index, region] of geometry.recommendedOpeningVoidRegions.entries()) {
    boxes.push({
      role: "infill",
      index,
      minX: region.xMm - centerXMm,
      maxX: region.xMm + region.widthMm - centerXMm,
      minY: centerYMm - (region.yMm + region.heightMm),
      maxY: centerYMm - region.yMm
    });
  }
  for (const box of boxes) {
    const mesh = new Mesh(
      new BoxGeometry(
        Math.max(0.001, (box.maxX - box.minX) * MILLIMETRES_TO_METRES),
        Math.max(0.001, (box.maxY - box.minY) * MILLIMETRES_TO_METRES),
        Math.max(0.001, section.wallThicknessMm * MILLIMETRES_TO_METRES)
      ),
      material
    );
    mesh.name = box.role === "return"
      ? `installation-wall:${box.side}`
      : `installation-wall:infill:${box.index + 1}`;
    mesh.position.set(
      (box.minX + box.maxX) / 2 * MILLIMETRES_TO_METRES,
      (box.minY + box.maxY) / 2 * MILLIMETRES_TO_METRES,
      section.wallCenterZMm * MILLIMETRES_TO_METRES
    );
    mesh.castShadow = true;
    mesh.receiveShadow = true;
    mesh.userData.objectType = "installation-wall-part";
    mesh.userData.objectId = box.role === "return" && box.side
      ? createWindowInstallationWallObjectId(assembly.objectId, box.side)
      : `${groupId}.infill.${box.index + 1}`;
    mesh.userData.sourceAssemblyId = assembly.objectId;
    mesh.userData.installationSide = box.side;
    mesh.userData.hostPartRole = box.role;
    mesh.userData.wallThicknessMm = section.wallThicknessMm;
    mesh.userData.wallCenterZMm = section.wallCenterZMm;
    mesh.userData.wallMaterialId = installation.surround.wallMaterialId;
    mesh.userData.excludeFromBom = true;
    group.add(mesh);
  }
  return group;
}

/**
 * Builds one assembly-owned reference host from the real planes of a corner composition.
 *
 * Every product member contributes a wall plane in its own local coordinate system.
 * Returns on connected edges are suppressed, so a corner post is not incorrectly
 * surrounded by two independent rectangular openings. The result remains a
 * presentation-only installation reference and never enters the product/BOM model.
 *
 * @since 0.10.87
 */
function createThreeSpatialFabricationAssemblyHost(
  geometry: ResolvedFabricationAssemblyGeometry,
  windowById: ReadonlyMap<string, WindowUnit>,
  materialCache: ThreeAppearanceMaterialCache,
  wallAppearance: VisualAppearanceSnapshot
): Group {
  const { assembly } = geometry;
  const installation = normalizeWindowInstallation(assembly.installation);
  const group = new Group();
  const groupId = createWindowInstallationWallObjectId(assembly.objectId);
  const centerYMm = geometry.bounds.yMm + geometry.bounds.heightMm / 2;
  const planCenterXMm = (geometry.planBounds.minXMm + geometry.planBounds.maxXMm) / 2;
  const planCenterZMm = (geometry.planBounds.minZMm + geometry.planBounds.maxZMm) / 2;
  const connectedEdges = new Set(
    assembly.joints.flatMap((joint) => [
      `${joint.firstInstanceId}:${joint.firstEdge}`,
      `${joint.secondInstanceId}:${joint.secondEdge}`
    ])
  );
  group.name = `installation-host:${assembly.objectId}`;
  group.userData.objectType = "installation-host";
  group.userData.objectId = groupId;
  group.userData.sourceAssemblyId = assembly.objectId;
  group.userData.sourceWindowIds = assembly.instances.map((instance) => instance.windowId);
  group.userData.excludeFromBom = true;
  group.userData.excludeFromWindowSelectionBounds = true;
  group.userData.coordinateConvention = "+Z exterior / -Z interior";
  group.userData.hostTopology = "multi-plane";
  group.userData.planeCount = geometry.planInstances.length;
  const material = materialCache.resolve(wallAppearance);
  const bottomSupportMm = resolveWindowInstallationBottomSupportMm(installation);
  group.userData.installation = {
    sillHeightMm: installation.sillHeightMm,
    bottomSupportMm,
    wallMaterialId: installation.surround.wallMaterialId
  };

  for (const [planeIndex, planInstance] of geometry.planInstances.entries()) {
    const window = windowById.get(planInstance.windowId);
    const elevationInstance = geometry.instances.find(
      (candidate) => candidate.instanceId === planInstance.instanceId
    );
    if (!window || !elevationInstance) {
      throw new Error(`Assembly plane ${planInstance.instanceId} has no window geometry.`);
    }
    const section = resolveInstallationSectionFromFrameDepth(
      installation,
      planInstance.frameDepthMm
    );
    const wallBandMm = Math.max(
      420,
      Math.min(900, Math.min(planInstance.widthMm, elevationInstance.heightMm) * 0.35)
    );
    const free = {
      left: !connectedEdges.has(`${planInstance.instanceId}:left`),
      right: !connectedEdges.has(`${planInstance.instanceId}:right`),
      top: !connectedEdges.has(`${planInstance.instanceId}:top`),
      bottom: !connectedEdges.has(`${planInstance.instanceId}:bottom`)
    };
    const halfWidthMm = planInstance.widthMm / 2;
    const halfHeightMm = elevationInstance.heightMm / 2;
    const bottomExtensionMm = free.bottom ? bottomSupportMm : 0;
    const topExtensionMm = free.top ? wallBandMm : 0;
    const boxes: Array<Readonly<{
      side: WindowInstallationSide;
      minX: number;
      maxX: number;
      minY: number;
      maxY: number;
    }>> = [];
    if (free.top) {
      boxes.push({
        side: "top",
        minX: -halfWidthMm - (free.left ? wallBandMm : 0),
        maxX: halfWidthMm + (free.right ? wallBandMm : 0),
        minY: halfHeightMm,
        maxY: halfHeightMm + wallBandMm
      });
    }
    if (free.left) {
      boxes.push({
        side: "left",
        minX: -halfWidthMm - wallBandMm,
        maxX: -halfWidthMm,
        minY: -halfHeightMm - bottomExtensionMm,
        maxY: halfHeightMm + topExtensionMm
      });
    }
    if (free.right) {
      boxes.push({
        side: "right",
        minX: halfWidthMm,
        maxX: halfWidthMm + wallBandMm,
        minY: -halfHeightMm - bottomExtensionMm,
        maxY: halfHeightMm + topExtensionMm
      });
    }
    if (free.bottom && bottomSupportMm > 0) {
      boxes.push({
        side: "bottom",
        minX: -halfWidthMm,
        maxX: halfWidthMm,
        minY: -halfHeightMm - bottomSupportMm,
        maxY: -halfHeightMm
      });
    }

    const planCenterTotal = planInstance.footprint.reduce(
      (sum, point) => ({ xMm: sum.xMm + point.xMm, zMm: sum.zMm + point.zMm }),
      { xMm: 0, zMm: 0 }
    );
    const plane = new Group();
    plane.name = `installation-host-plane:${planInstance.instanceId}`;
    plane.position.set(
      (planCenterTotal.xMm / planInstance.footprint.length - planCenterXMm) *
        MILLIMETRES_TO_METRES,
      (centerYMm - elevationInstance.yMm - elevationInstance.heightMm / 2) *
        MILLIMETRES_TO_METRES,
      (planCenterTotal.zMm / planInstance.footprint.length - planCenterZMm) *
        MILLIMETRES_TO_METRES
    );
    plane.rotation.y = planInstance.rotationYDeg * Math.PI / 180;
    plane.userData.objectType = "installation-host-plane";
    plane.userData.sourceAssemblyId = assembly.objectId;
    plane.userData.assemblyInstanceId = planInstance.instanceId;
    plane.userData.rotationYDeg = planInstance.rotationYDeg;
    plane.userData.wallThicknessMm = section.wallThicknessMm;
    plane.userData.wallCenterZMm = section.wallCenterZMm;

    for (const box of boxes) {
      const miter = box.side === "top" || box.side === "bottom"
        ? resolveMiteredAssemblyRectangle(geometry, planInstance, {
          minXMm: box.minX,
          maxXMm: box.maxX,
          minZMm: section.wallCenterZMm - section.wallThicknessMm / 2,
          maxZMm: section.wallCenterZMm + section.wallThicknessMm / 2
        })
        : undefined;
      const usesMiter = Boolean(miter?.jointIds.length);
      const mesh = new Mesh(
        usesMiter
          ? createVerticalPlanPrismGeometry(miter!.points, box.maxY - box.minY)
          : new BoxGeometry(
            Math.max(0.001, (box.maxX - box.minX) * MILLIMETRES_TO_METRES),
            Math.max(0.001, (box.maxY - box.minY) * MILLIMETRES_TO_METRES),
            Math.max(0.001, section.wallThicknessMm * MILLIMETRES_TO_METRES)
          ),
        usesMiter ? createStableContactMaterial(material, planeIndex + 1) : material
      );
      mesh.name = `installation-wall:${planInstance.instanceId}:${box.side}`;
      mesh.position.set(
        usesMiter ? 0 : (box.minX + box.maxX) / 2 * MILLIMETRES_TO_METRES,
        (box.minY + box.maxY) / 2 * MILLIMETRES_TO_METRES,
        usesMiter ? 0 : section.wallCenterZMm * MILLIMETRES_TO_METRES
      );
      mesh.castShadow = true;
      mesh.receiveShadow = true;
      mesh.userData.objectType = "installation-wall-part";
      mesh.userData.objectId = groupId;
      mesh.userData.hostPartId = `${groupId}.${planInstance.instanceId}.${box.side}`;
      mesh.userData.sourceAssemblyId = assembly.objectId;
      mesh.userData.assemblyInstanceId = planInstance.instanceId;
      mesh.userData.installationSide = box.side;
      mesh.userData.wallThicknessMm = section.wallThicknessMm;
      mesh.userData.wallCenterZMm = section.wallCenterZMm;
      mesh.userData.wallMaterialId = installation.surround.wallMaterialId;
      mesh.userData.excludeFromBom = true;
      if (usesMiter) {
        mesh.userData.cornerJoinTreatment = "angle-bisector-miter";
        mesh.userData.cornerJointIds = [...miter!.jointIds];
        mesh.userData.renderStability = "non-overlapping-polygon-with-depth-priority";
      }
      plane.add(mesh);
    }
    group.add(plane);
  }
  return group;
}

/**
 * Projects enabled installation trim and reveal-board pieces into Three.js.
 *
 * Geometry and source-component IDs come from the shared surround resolver, so
 * 3D and installation BOM cannot disagree about selected edges or layers. Trim
 * sits beyond the outside/inside wall finish; liner boards occupy the reveal
 * depth. Installation wall/package entries are exposed separately in the
 * object tree, so picking one trim layer no longer impersonates the product.
 *
 * @param window Window containing a normalized, optionally enabled surround.
 * @param widthMetres Opening width centred on X=0.
 * @param heightMetres Opening height centred on Y=0.
 * @returns A traceable material group, empty when the surround is disabled.
 * @example `both_sides/all` creates four outside, four inside and four liner pieces.
 * @since 0.9.9
 * @modified 2026-09-20 - Added independent package/layer selection identities.
 */
function createThreeInstallationSurround(
  window: WindowUnit,
  widthMetres: number,
  heightMetres: number,
  materialCache: ThreeAppearanceMaterialCache,
  appearances: Readonly<{
    outside: VisualAppearanceSnapshot;
    inside: VisualAppearanceSnapshot;
    liner: VisualAppearanceSnapshot;
  }>
): Group {
  const geometry = resolveWindowInstallationSurroundGeometry(window);
  const section = resolveWindowInstallationSection(window);
  const { surround } = geometry.installation;
  const group = new Group();
  group.name = `installation-surround:${window.objectId}`;
  group.userData.objectType = "installation-surround";
  group.userData.objectId = createWindowInstallationSurroundObjectId(window.objectId);
  group.userData.sourceWindowId = window.objectId;
  group.userData.excludeFromWindowSelectionBounds = true;
  group.userData.enabled = surround.enabled;
  group.userData.materialCode = surround.materialCode;
  group.userData.sides = geometry.pieces.map((piece) => piece.side);
  group.userData.perimeterMm = geometry.perimeterMm;
  group.userData.linerAreaM2 = geometry.linerAreaM2;
  if (!surround.enabled) return group;

  const outsideMaterial = materialCache.resolve(appearances.outside);
  const insideMaterial = materialCache.resolve(appearances.inside);
  const linerMaterial = materialCache.resolve(appearances.liner);
  for (const obstacle of resolveWindowInstallationObstacleGeometry(window)) {
    if (obstacle.kind !== "surround" || !obstacle.layer) continue;
    const material = obstacle.layer === "outside"
      ? outsideMaterial
      : obstacle.layer === "inside"
        ? insideMaterial
        : linerMaterial;
    const mesh = new Mesh(
      new BoxGeometry(
        Math.max(0.001, (obstacle.max.x - obstacle.min.x) * MILLIMETRES_TO_METRES),
        Math.max(0.001, (obstacle.max.y - obstacle.min.y) * MILLIMETRES_TO_METRES),
        Math.max(0.001, (obstacle.max.z - obstacle.min.z) * MILLIMETRES_TO_METRES)
      ),
      material
    );
    mesh.name = `installation-surround:${obstacle.layer}:${obstacle.side}`;
    mesh.position.set(
      (obstacle.min.x + obstacle.max.x) / 2 * MILLIMETRES_TO_METRES,
      (obstacle.min.y + obstacle.max.y) / 2 * MILLIMETRES_TO_METRES,
      (obstacle.min.z + obstacle.max.z) / 2 * MILLIMETRES_TO_METRES
    );
    mesh.castShadow = true;
    mesh.receiveShadow = true;
    mesh.userData.objectId = createWindowInstallationSurroundObjectId(
      window.objectId,
      obstacle.layer,
      obstacle.side
    );
    mesh.userData.sourceWindowId = window.objectId;
    mesh.userData.objectType = `installation-surround-${obstacle.layer}`;
    mesh.userData.sourceComponentId = obstacle.sourceComponentId;
    mesh.userData.obstacleId = obstacle.obstacleId;
    mesh.userData.materialCode = obstacle.layer === "liner"
      ? `${surround.materialCode}-LINER`
      : surround.materialCode;
    mesh.userData.installationSide = obstacle.side;
    mesh.userData.layer = obstacle.layer;
    mesh.userData.lengthMm = obstacle.side === "top" || obstacle.side === "bottom"
      ? window.widthMm
      : window.heightMm;
    mesh.userData.boardThicknessMm = surround.boardThicknessMm;
    group.add(mesh);
  }
  return group;
}

/**
 * Projects package/liner pieces once around a connected assembly envelope.
 *
 * This is the visual counterpart of the assembly-owned Installation snapshot.
 * Member-window package geometry is suppressed while the assembly is active,
 * avoiding doubled boards along internal joints. Until the assembly catalog
 * receives its own appearance snapshot, the first member supplies only the PBR
 * appearance; dimensions and business material code remain assembly-owned.
 *
 * @since 0.10.46
 * @modified 2026-09-21 - Projected package pieces along the exact L/T product outline.
 */
function createThreeFabricationAssemblySurround(
  geometry: ResolvedFabricationAssemblyGeometry,
  frameDepthMm: number,
  materialCache: ThreeAppearanceMaterialCache,
  appearances: Readonly<{
    outside: VisualAppearanceSnapshot;
    inside: VisualAppearanceSnapshot;
    liner: VisualAppearanceSnapshot;
  }>
): Group {
  const { assembly, bounds } = geometry;
  const installation = normalizeWindowInstallation(assembly.installation);
  const { surround } = installation;
  const section = resolveInstallationSectionFromFrameDepth(installation, frameDepthMm);
  const group = new Group();
  group.name = `installation-surround:${assembly.objectId}`;
  group.userData.objectType = "installation-surround";
  group.userData.objectId = createWindowInstallationSurroundObjectId(assembly.objectId);
  group.userData.sourceAssemblyId = assembly.objectId;
  group.userData.sourceWindowIds = assembly.instances.map((instance) => instance.windowId);
  group.userData.excludeFromWindowSelectionBounds = true;
  group.userData.excludeFromBom = true;
  group.userData.enabled = surround.enabled;
  group.userData.materialCode = surround.materialCode;
  group.userData.sides = [...surround.sides];
  if (!surround.enabled) return group;

  const centerXMm = bounds.xMm + bounds.widthMm / 2;
  const centerYMm = bounds.yMm + bounds.heightMm / 2;
  const outsideEnabled = surround.styleId === "both_sides" ||
    surround.styleId === "outside_only";
  const insideEnabled = surround.styleId === "both_sides" ||
    surround.styleId === "inside_only";
  const linerEnabled = surround.styleId === "both_sides" || surround.styleId === "liner";
  const materials = {
    outside: materialCache.resolve(appearances.outside),
    inside: materialCache.resolve(appearances.inside),
    liner: materialCache.resolve(appearances.liner)
  };
  const addPiece = (
    segment: ResolvedFabricationAssemblyGeometry["outline"][number],
    segmentIndex: number,
    layer: "outside" | "inside" | "liner",
    faceWidthMm: number,
    minZMm: number,
    maxZMm: number
  ): void => {
    const { side } = segment;
    const liner = layer === "liner";
    const horizontal = side === "top" || side === "bottom";
    const segmentMinX = Math.min(segment.startXMm, segment.endXMm) - centerXMm;
    const segmentMaxX = Math.max(segment.startXMm, segment.endXMm) - centerXMm;
    const segmentMinY = centerYMm - Math.max(segment.startYMm, segment.endYMm);
    const segmentMaxY = centerYMm - Math.min(segment.startYMm, segment.endYMm);
    const boundaryX = segment.startXMm - centerXMm;
    const boundaryY = centerYMm - segment.startYMm;
    const minX = horizontal
      ? segmentMinX - (liner ? 0 : faceWidthMm)
      : side === "left"
        ? boundaryX - (liner ? 0 : faceWidthMm)
        : boundaryX - (liner ? faceWidthMm : 0);
    const maxX = horizontal
      ? segmentMaxX + (liner ? 0 : faceWidthMm)
      : side === "left"
        ? boundaryX + (liner ? faceWidthMm : 0)
        : boundaryX + (liner ? 0 : faceWidthMm);
    const minY = horizontal
      ? side === "top"
        ? boundaryY - (liner ? faceWidthMm : 0)
        : boundaryY - (liner ? 0 : faceWidthMm)
      : segmentMinY;
    const maxY = horizontal
      ? side === "top"
        ? boundaryY + (liner ? 0 : faceWidthMm)
        : boundaryY + (liner ? faceWidthMm : 0)
      : segmentMaxY;
    const mesh = new Mesh(
      new BoxGeometry(
        Math.max(0.001, (maxX - minX) * MILLIMETRES_TO_METRES),
        Math.max(0.001, (maxY - minY) * MILLIMETRES_TO_METRES),
        Math.max(0.001, (maxZMm - minZMm) * MILLIMETRES_TO_METRES)
      ),
      materials[layer]
    );
    mesh.name = `installation-surround:${layer}:${side}:${segmentIndex + 1}`;
    mesh.position.set(
      (minX + maxX) / 2 * MILLIMETRES_TO_METRES,
      (minY + maxY) / 2 * MILLIMETRES_TO_METRES,
      (minZMm + maxZMm) / 2 * MILLIMETRES_TO_METRES
    );
    mesh.userData.objectId = `${createWindowInstallationSurroundObjectId(
      assembly.objectId,
      layer,
      side
    )}.segment.${segmentIndex + 1}`;
    mesh.userData.sourceAssemblyId = assembly.objectId;
    mesh.userData.objectType = `installation-surround-${layer}`;
    mesh.userData.sourceComponentId = `installation.surround.${layer}.${side}.segment.${segmentIndex + 1}`;
    mesh.userData.materialCode = layer === "liner"
      ? `${surround.materialCode}-LINER`
      : surround.materialCode;
    mesh.userData.installationSide = side;
    mesh.userData.layer = layer;
    mesh.userData.lengthMm = horizontal
      ? Math.abs(segment.endXMm - segment.startXMm)
      : Math.abs(segment.endYMm - segment.startYMm);
    mesh.userData.boardThicknessMm = surround.boardThicknessMm;
    group.add(mesh);
  };
  for (const [segmentIndex, segment] of geometry.outline.entries()) {
    if (!surround.sides.includes(segment.side)) continue;
    if (linerEnabled) {
      addPiece(
        segment,
        segmentIndex,
        "liner",
        surround.boardThicknessMm,
        section.wallInsideZMm,
        section.wallOutsideZMm
      );
    }
    if (outsideEnabled) {
      addPiece(
        segment,
        segmentIndex,
        "outside",
        surround.outsideWidthMm,
        section.wallOutsideZMm,
        section.wallOutsideZMm + surround.boardThicknessMm
      );
    }
    if (insideEnabled) {
      addPiece(
        segment,
        segmentIndex,
        "inside",
        surround.insideWidthMm,
        section.wallInsideZMm - surround.boardThicknessMm,
        section.wallInsideZMm
      );
    }
  }
  return group;
}

/**
 * Projects one assembly-owned package across the real planes of a corner product.
 *
 * Connected member edges are removed before creating outside trim, inside trim
 * or reveal-board pieces. This prevents two packages from occupying the corner
 * post while keeping every visible piece under the assembly package identity.
 *
 * @since 0.10.89
 */
function createThreeSpatialFabricationAssemblySurround(
  geometry: ResolvedFabricationAssemblyGeometry,
  windowById: ReadonlyMap<string, WindowUnit>,
  materialCache: ThreeAppearanceMaterialCache,
  appearances: Readonly<{
    outside: VisualAppearanceSnapshot;
    inside: VisualAppearanceSnapshot;
    liner: VisualAppearanceSnapshot;
  }>
): Group {
  const { assembly } = geometry;
  const installation = normalizeWindowInstallation(assembly.installation);
  const { surround } = installation;
  const group = new Group();
  const groupId = createWindowInstallationSurroundObjectId(assembly.objectId);
  group.name = `installation-surround:${assembly.objectId}`;
  group.userData.objectType = "installation-surround";
  group.userData.objectId = groupId;
  group.userData.sourceAssemblyId = assembly.objectId;
  group.userData.sourceWindowIds = assembly.instances.map((instance) => instance.windowId);
  group.userData.excludeFromWindowSelectionBounds = true;
  group.userData.excludeFromBom = true;
  group.userData.enabled = surround.enabled;
  group.userData.materialCode = surround.materialCode;
  group.userData.hostTopology = "multi-plane";
  group.userData.planeCount = geometry.planInstances.length;
  if (!surround.enabled) return group;

  const connectedEdges = new Set(
    assembly.joints.flatMap((joint) => [
      `${joint.firstInstanceId}:${joint.firstEdge}`,
      `${joint.secondInstanceId}:${joint.secondEdge}`
    ])
  );
  const centerYMm = geometry.bounds.yMm + geometry.bounds.heightMm / 2;
  const planCenterXMm = (geometry.planBounds.minXMm + geometry.planBounds.maxXMm) / 2;
  const planCenterZMm = (geometry.planBounds.minZMm + geometry.planBounds.maxZMm) / 2;
  const outsideEnabled = surround.styleId === "both_sides" ||
    surround.styleId === "outside_only";
  const insideEnabled = surround.styleId === "both_sides" ||
    surround.styleId === "inside_only";
  const linerEnabled = surround.styleId === "both_sides" || surround.styleId === "liner";
  const materials = {
    outside: materialCache.resolve(appearances.outside),
    inside: materialCache.resolve(appearances.inside),
    liner: materialCache.resolve(appearances.liner)
  };
  const renderedSides: string[] = [];

  for (const [planeIndex, planInstance] of geometry.planInstances.entries()) {
    const window = windowById.get(planInstance.windowId);
    const elevationInstance = geometry.instances.find(
      (candidate) => candidate.instanceId === planInstance.instanceId
    );
    if (!window || !elevationInstance) {
      throw new Error(`Assembly package plane ${planInstance.instanceId} has no window geometry.`);
    }
    const section = resolveInstallationSectionFromFrameDepth(
      installation,
      planInstance.frameDepthMm
    );
    const halfWidthMm = planInstance.widthMm / 2;
    const halfHeightMm = elevationInstance.heightMm / 2;
    const planCenterTotal = planInstance.footprint.reduce(
      (sum, point) => ({ xMm: sum.xMm + point.xMm, zMm: sum.zMm + point.zMm }),
      { xMm: 0, zMm: 0 }
    );
    const plane = new Group();
    plane.name = `installation-surround-plane:${planInstance.instanceId}`;
    plane.position.set(
      (planCenterTotal.xMm / planInstance.footprint.length - planCenterXMm) *
        MILLIMETRES_TO_METRES,
      (centerYMm - elevationInstance.yMm - elevationInstance.heightMm / 2) *
        MILLIMETRES_TO_METRES,
      (planCenterTotal.zMm / planInstance.footprint.length - planCenterZMm) *
        MILLIMETRES_TO_METRES
    );
    plane.rotation.y = planInstance.rotationYDeg * Math.PI / 180;
    plane.userData.objectType = "installation-surround-plane";
    plane.userData.sourceAssemblyId = assembly.objectId;
    plane.userData.assemblyInstanceId = planInstance.instanceId;
    plane.userData.rotationYDeg = planInstance.rotationYDeg;

    const addPiece = (
      side: WindowInstallationSide,
      layer: "outside" | "inside" | "liner",
      faceWidthMm: number,
      minZMm: number,
      maxZMm: number
    ): void => {
      const liner = layer === "liner";
      const horizontal = side === "top" || side === "bottom";
      const minX = horizontal
        ? liner ? -halfWidthMm : -halfWidthMm - faceWidthMm
        : side === "left"
          ? liner ? -halfWidthMm : -halfWidthMm - faceWidthMm
          : liner ? halfWidthMm - faceWidthMm : halfWidthMm;
      const maxX = horizontal
        ? liner ? halfWidthMm : halfWidthMm + faceWidthMm
        : side === "left"
          ? liner ? -halfWidthMm + faceWidthMm : -halfWidthMm
          : liner ? halfWidthMm : halfWidthMm + faceWidthMm;
      const minY = horizontal
        ? side === "top"
          ? liner ? halfHeightMm - faceWidthMm : halfHeightMm
          : liner ? -halfHeightMm : -halfHeightMm - faceWidthMm
        : -halfHeightMm;
      const maxY = horizontal
        ? side === "top"
          ? liner ? halfHeightMm : halfHeightMm + faceWidthMm
          : liner ? -halfHeightMm + faceWidthMm : -halfHeightMm
        : halfHeightMm;
      const miter = horizontal
        ? resolveMiteredAssemblyRectangle(geometry, planInstance, {
          minXMm: minX,
          maxXMm: maxX,
          minZMm,
          maxZMm
        })
        : undefined;
      const usesMiter = Boolean(miter?.jointIds.length);
      const layerPriority = layer === "liner" ? 1 : layer === "outside" ? 2 : 3;
      const mesh = new Mesh(
        usesMiter
          ? createVerticalPlanPrismGeometry(miter!.points, maxY - minY)
          : new BoxGeometry(
            Math.max(0.001, (maxX - minX) * MILLIMETRES_TO_METRES),
            Math.max(0.001, (maxY - minY) * MILLIMETRES_TO_METRES),
            Math.max(0.001, (maxZMm - minZMm) * MILLIMETRES_TO_METRES)
          ),
        usesMiter
          ? createStableContactMaterial(materials[layer], planeIndex * 3 + layerPriority)
          : materials[layer]
      );
      const sideId = createWindowInstallationSurroundObjectId(
        assembly.objectId,
        layer,
        side
      );
      mesh.name = `installation-surround:${planInstance.instanceId}:${layer}:${side}`;
      mesh.position.set(
        usesMiter ? 0 : (minX + maxX) / 2 * MILLIMETRES_TO_METRES,
        (minY + maxY) / 2 * MILLIMETRES_TO_METRES,
        usesMiter ? 0 : (minZMm + maxZMm) / 2 * MILLIMETRES_TO_METRES
      );
      mesh.castShadow = true;
      mesh.receiveShadow = true;
      mesh.userData.objectId = `${sideId}.instance.${planInstance.instanceId}`;
      mesh.userData.sourceAssemblyId = assembly.objectId;
      mesh.userData.assemblyInstanceId = planInstance.instanceId;
      mesh.userData.objectType = `installation-surround-${layer}`;
      mesh.userData.sourceComponentId =
        `installation.surround.${layer}.${planInstance.instanceId}.${side}`;
      mesh.userData.materialCode = layer === "liner"
        ? `${surround.materialCode}-LINER`
        : surround.materialCode;
      mesh.userData.installationSide = side;
      mesh.userData.layer = layer;
      mesh.userData.lengthMm = horizontal
        ? planInstance.widthMm
        : elevationInstance.heightMm;
      mesh.userData.boardThicknessMm = surround.boardThicknessMm;
      if (usesMiter) {
        const xValues = miter!.points.map((point) => point.xMm);
        mesh.userData.cornerJoinTreatment = "angle-bisector-miter";
        mesh.userData.cornerJointIds = [...miter!.jointIds];
        mesh.userData.nominalLengthMm = planInstance.widthMm;
        mesh.userData.renderedPlanLengthMm = Math.max(...xValues) - Math.min(...xValues);
        mesh.userData.renderStability = "non-overlapping-polygon-with-depth-priority";
      }
      plane.add(mesh);
      renderedSides.push(`${planInstance.instanceId}:${layer}:${side}`);
    };

    for (const side of surround.sides) {
      if (connectedEdges.has(`${planInstance.instanceId}:${side}`)) continue;
      if (linerEnabled) {
        addPiece(
          side,
          "liner",
          surround.boardThicknessMm,
          section.wallInsideZMm,
          section.wallOutsideZMm
        );
      }
      if (outsideEnabled) {
        addPiece(
          side,
          "outside",
          surround.outsideWidthMm,
          section.wallOutsideZMm,
          section.wallOutsideZMm + surround.boardThicknessMm
        );
      }
      if (insideEnabled) {
        addPiece(
          side,
          "inside",
          surround.insideWidthMm,
          section.wallInsideZMm - surround.boardThicknessMm,
          section.wallInsideZMm
        );
      }
    }
    group.add(plane);
  }
  group.userData.sides = renderedSides;
  return group;
}

/**
 * Determines whether a stable selection belongs to one resolved window.
 *
 * The scene builder lays out several windows in one root. Gating annotations
 * here prevents a selected handle in window A from causing labels on every
 * window. Generated hardware uses its own stable ID, while opening dimensions
 * are associated through `sourceObjectId` inside the annotation builder.
 *
 * @param window Candidate owner.
 * @param geometry Shared renderer-neutral geometry for that owner.
 * @param objectId Selected domain or generated-hardware identity.
 * @returns True only when the candidate window owns the selection.
 * @example `cell.1.1.hardware.handle` resolves only to its source window.
 * @since 0.10.39
 * @modified 2026-09-20 - Added exact panel and hardware selection ownership.
 */
function windowOwnsDimensionSelection(
  window: WindowUnit,
  geometry: ReturnType<typeof resolveWindowGeometry>,
  objectId: string
): boolean {
  const wallId = createWindowInstallationWallObjectId(window.objectId);
  const surroundId = createWindowInstallationSurroundObjectId(window.objectId);
  return objectId === window.objectId ||
    objectId === wallId || objectId.startsWith(`${wallId}.`) ||
    objectId === surroundId || objectId.startsWith(`${surroundId}.`) ||
    geometry.frames.some((item) => item.objectId === objectId) ||
    geometry.cells.some((item) => item.objectId === objectId) ||
    geometry.openings.some((item) => item.objectId === objectId) ||
    geometry.openings.some((item) =>
      createOpeningPanelKey(item.objectId, item.panelId) === objectId
    ) ||
    geometry.members.some((item) => item.objectId === objectId) ||
    geometry.meetingMullions.some((item) => item.objectId === objectId) ||
    geometry.hardware.some((item) => item.hardwareId === objectId);
}

/**
 * Projects the current window's principal manufacturing dimensions into 3D.
 *
 * The collection contains dimensions for only the selected semantic object:
 * product envelope, one cell, one P1/P2 sash, one frame/member, one hardware
 * instance, one installation piece, or one complete wall/package group. Values
 * come from shared resolvers; the renderer never measures display meshes.
 *
 * @param input Window model, resolved opening geometry and metre-scale section data.
 * @returns One optional overlay collection controlled entirely by view state.
 * @example Selecting P2 exposes P2 width/height/depth but no P1 labels.
 * @since 0.9.4
 * @modified 2026-09-20 - Enforced exact-object dimension scope.
 */
function createThreeWindowDimensionAnnotations(input: Readonly<{
  window: WindowUnit;
  geometry: ReturnType<typeof resolveWindowGeometry>;
  selectedObjectId: string;
  widthMetres: number;
  heightMetres: number;
  frameDepthMetres: number;
  sashDepthMetres: number;
  closedSashCenterZ: number;
  installationSection: ReturnType<typeof resolveWindowInstallationSection>;
}>): Group {
  const collection = new Group();
  collection.name = `linear-dimensions:${input.window.objectId}`;
  collection.userData.objectType = "linear-dimension-collection";
  collection.userData.sourceWindowId = input.window.objectId;
  const frontZ = input.frameDepthMetres / 2 + 0.045;
  const leftX = -input.widthMetres / 2;
  const rightX = input.widthMetres / 2;
  const topY = input.heightMetres / 2;
  const bottomY = -input.heightMetres / 2;
  const formatMm = (value: number): string => `${Number(value.toFixed(1))} mm`;
  const selectedHardware = input.geometry.hardware.find((mount) =>
    mount.hardwareId === input.selectedObjectId
  );
  const selectedPanel = input.geometry.openings.find((opening) =>
    createOpeningPanelKey(opening.objectId, opening.panelId) === input.selectedObjectId
  );
  const selectedOpening = selectedPanel;
  const selectedCell = input.geometry.cells.find((cell) =>
    cell.objectId === input.selectedObjectId
  );
  const selectedFrame = input.geometry.frames.find((frame) =>
    frame.objectId === input.selectedObjectId
  );
  const selectedMember = input.geometry.members.find((member) =>
    member.objectId === input.selectedObjectId
  ) ?? input.geometry.meetingMullions.find((member) =>
    member.objectId === input.selectedObjectId
  );
  const selectsWholeWindow = input.selectedObjectId === input.window.objectId;
  const wallSelectionId = createWindowInstallationWallObjectId(input.window.objectId);
  const surroundSelectionId = createWindowInstallationSurroundObjectId(input.window.objectId);
  const selectsWallGroup = input.selectedObjectId === wallSelectionId;
  const selectsSurroundGroup = input.selectedObjectId === surroundSelectionId;
  const selectedInstallationPiece = resolveWindowInstallationObstacleGeometry(input.window).find(
    (obstacle) => {
      const objectId = obstacle.kind === "wall"
        ? createWindowInstallationWallObjectId(input.window.objectId, obstacle.side)
        : obstacle.layer
          ? createWindowInstallationSurroundObjectId(
              input.window.objectId,
              obstacle.layer,
              obstacle.side
            )
          : undefined;
      return objectId === input.selectedObjectId;
    }
  );

  /**
   * Keeps only dimensions meaningful for the selected component.
   *
   * A whole-window selection shows only its product envelope and frame depth;
   * an opening or hardware shows that panel; wall and package selections show
   * their own installation dimensions. Frame/member hits retain product
   * envelope context. This prevents wall, package and sash values from being
   * presented as if they belonged to the same selected object.
   *
   * @example Selecting `CELL-1` keeps `CELL-1:P1:dimension.width` and height.
   * @since 0.10.39
   * @modified 2026-09-20 - Split window, wall, package and opening dimensions.
   */
  const add = (dimension: ReturnType<typeof createThreeLinearDimensionAnnotation>): void => {
    const kind = dimension.userData.dimensionKind as string;
    const openingDimensionPrefix = selectedPanel
      ? `${selectedPanel.objectId}:${selectedPanel.panelId}:`
      : undefined;
    const belongsToSelectedOpening = openingDimensionPrefix
      ? String(dimension.userData.dimensionId).startsWith(openingDimensionPrefix)
      : false;
    const isWindowEnvelope = kind === "overall-width" || kind === "overall-height";
    const isSectionDepth = kind === "frame-depth";
    const isWallDimension = kind === "wall-thickness" || kind === "frame-wall-offset";
    const isSurroundDimension = kind.startsWith("surround-");
    const isHardwareDimension = kind.startsWith("hardware-");
    const isCellDimension = kind.startsWith("cell-");
    const isComponentDimension = kind.startsWith("component-");
    if (
      (selectsWholeWindow && (isWindowEnvelope || isSectionDepth)) ||
      belongsToSelectedOpening ||
      (selectedCell !== undefined && isCellDimension) ||
      ((selectedFrame !== undefined || selectedMember !== undefined ||
        selectedInstallationPiece !== undefined) && isComponentDimension) ||
      (selectsWallGroup && isWallDimension) ||
      (selectsSurroundGroup && isSurroundDimension)
      || (selectedHardware !== undefined && isHardwareDimension)
    ) {
      collection.add(dimension);
    }
  };
  add(createThreeLinearDimensionAnnotation({
    dimensionId: `${input.window.objectId}:dimension.overall-width`,
    kind: "overall-width",
    valueMm: input.window.widthMm,
    label: `总宽 ${formatMm(input.window.widthMm)}`,
    measurementStart: new Vector3(leftX, bottomY, frontZ),
    measurementEnd: new Vector3(rightX, bottomY, frontZ),
    dimensionStart: new Vector3(leftX, bottomY - 0.14, frontZ),
    dimensionEnd: new Vector3(rightX, bottomY - 0.14, frontZ),
    tickDirection: new Vector3(0, 0.032, 0),
    labelOffset: new Vector3(0, -0.06, 0)
  }));
  add(createThreeLinearDimensionAnnotation({
    dimensionId: `${input.window.objectId}:dimension.overall-height`,
    kind: "overall-height",
    valueMm: input.window.heightMm,
    label: `总高 ${formatMm(input.window.heightMm)}`,
    measurementStart: new Vector3(rightX, bottomY, frontZ),
    measurementEnd: new Vector3(rightX, topY, frontZ),
    dimensionStart: new Vector3(rightX + 0.15, bottomY, frontZ),
    dimensionEnd: new Vector3(rightX + 0.15, topY, frontZ),
    tickDirection: new Vector3(0.032, 0, 0),
    labelOffset: new Vector3(0.09, 0, 0)
  }));
  add(createThreeLinearDimensionAnnotation({
    dimensionId: `${input.window.objectId}:dimension.frame-depth`,
    kind: "frame-depth",
    valueMm: input.frameDepthMetres / MILLIMETRES_TO_METRES,
    label: `框深 ${formatMm(input.frameDepthMetres / MILLIMETRES_TO_METRES)}`,
    measurementStart: new Vector3(leftX, topY, -input.frameDepthMetres / 2),
    measurementEnd: new Vector3(leftX, topY, input.frameDepthMetres / 2),
    dimensionStart: new Vector3(leftX - 0.15, topY, -input.frameDepthMetres / 2),
    dimensionEnd: new Vector3(leftX - 0.15, topY, input.frameDepthMetres / 2),
    tickDirection: new Vector3(0.032, 0, 0),
    labelOffset: new Vector3(-0.1, 0, 0)
  }));
  const wallInsideZ = input.installationSection.wallInsideZMm * MILLIMETRES_TO_METRES;
  const wallOutsideZ = input.installationSection.wallOutsideZMm * MILLIMETRES_TO_METRES;
  add(createThreeLinearDimensionAnnotation({
    dimensionId: `${input.window.objectId}:dimension.wall-thickness`,
    kind: "wall-thickness",
    valueMm: input.installationSection.wallThicknessMm,
    label: `墙厚 ${formatMm(input.installationSection.wallThicknessMm)}`,
    measurementStart: new Vector3(leftX, topY, wallInsideZ),
    measurementEnd: new Vector3(leftX, topY, wallOutsideZ),
    dimensionStart: new Vector3(leftX - 0.27, topY, wallInsideZ),
    dimensionEnd: new Vector3(leftX - 0.27, topY, wallOutsideZ),
    tickDirection: new Vector3(0.032, 0, 0),
    labelOffset: new Vector3(-0.11, 0, 0)
  }));
  if (Math.abs(input.installationSection.wallCenterZMm) > 0.001) {
    const frameDirection = input.installationSection.wallCenterZMm > 0 ? "框向室内" : "框向室外";
    add(createThreeLinearDimensionAnnotation({
      dimensionId: `${input.window.objectId}:dimension.frame-wall-offset`,
      kind: "frame-wall-offset",
      valueMm: Math.abs(input.installationSection.wallCenterZMm),
      label: `框位 ${formatMm(Math.abs(input.installationSection.wallCenterZMm))}（${frameDirection}）`,
      measurementStart: new Vector3(leftX, topY + 0.08, 0),
      measurementEnd: new Vector3(leftX, topY + 0.08, input.installationSection.wallCenterZMm * MILLIMETRES_TO_METRES),
      dimensionStart: new Vector3(leftX - 0.39, topY + 0.08, 0),
      dimensionEnd: new Vector3(leftX - 0.39, topY + 0.08, input.installationSection.wallCenterZMm * MILLIMETRES_TO_METRES),
      tickDirection: new Vector3(0.032, 0, 0),
      labelOffset: new Vector3(-0.13, 0.08, 0)
    }));
  }
  const installation = normalizeWindowInstallation(input.window.installation);
  if (installation.surround.enabled) {
    const { surround } = installation;
    const sides = new Set(surround.sides);
    const outerWidthMm = input.window.widthMm +
      (sides.has("left") ? surround.outsideWidthMm : 0) +
      (sides.has("right") ? surround.outsideWidthMm : 0);
    const outerHeightMm = input.window.heightMm +
      (sides.has("top") ? surround.outsideWidthMm : 0) +
      (sides.has("bottom") ? surround.outsideWidthMm : 0);
    add(createThreeLinearDimensionAnnotation({
      dimensionId: `${input.window.objectId}:dimension.surround-outer-width`,
      kind: "surround-outer-width",
      valueMm: outerWidthMm,
      label: `包边外宽 ${formatMm(outerWidthMm)}`,
      measurementStart: new Vector3(leftX, bottomY, frontZ),
      measurementEnd: new Vector3(rightX, bottomY, frontZ),
      dimensionStart: new Vector3(leftX, bottomY - 0.22, frontZ),
      dimensionEnd: new Vector3(rightX, bottomY - 0.22, frontZ),
      tickDirection: new Vector3(0, 0.032, 0),
      labelOffset: new Vector3(0, -0.05, 0)
    }));
    add(createThreeLinearDimensionAnnotation({
      dimensionId: `${input.window.objectId}:dimension.surround-outer-height`,
      kind: "surround-outer-height",
      valueMm: outerHeightMm,
      label: `包边外高 ${formatMm(outerHeightMm)}`,
      measurementStart: new Vector3(rightX, bottomY, frontZ),
      measurementEnd: new Vector3(rightX, topY, frontZ),
      dimensionStart: new Vector3(rightX + 0.22, bottomY, frontZ),
      dimensionEnd: new Vector3(rightX + 0.22, topY, frontZ),
      tickDirection: new Vector3(0.032, 0, 0),
      labelOffset: new Vector3(0.09, 0, 0)
    }));
    add(createThreeLinearDimensionAnnotation({
      dimensionId: `${input.window.objectId}:dimension.surround-face-width`,
      kind: "surround-face-width",
      valueMm: surround.outsideWidthMm,
      label: `外包边宽 ${formatMm(surround.outsideWidthMm)}`,
      measurementStart: new Vector3(leftX, topY, frontZ),
      measurementEnd: new Vector3(leftX, topY, frontZ),
      dimensionStart: new Vector3(leftX - 0.08, topY + 0.08, frontZ),
      dimensionEnd: new Vector3(leftX - 0.08, topY + 0.08, frontZ),
      tickDirection: new Vector3(0.02, 0, 0),
      labelOffset: new Vector3(-0.04, 0.03, 0)
    }));
    add(createThreeLinearDimensionAnnotation({
      dimensionId: `${input.window.objectId}:dimension.surround-board-thickness`,
      kind: "surround-board-thickness",
      valueMm: surround.boardThicknessMm,
      label: `衬板厚 ${formatMm(surround.boardThicknessMm)}`,
      measurementStart: new Vector3(leftX, topY, wallInsideZ),
      measurementEnd: new Vector3(leftX, topY, wallOutsideZ),
      dimensionStart: new Vector3(leftX - 0.12, topY + 0.14, wallInsideZ),
      dimensionEnd: new Vector3(leftX - 0.12, topY + 0.14, wallOutsideZ),
      tickDirection: new Vector3(0.02, 0, 0),
      labelOffset: new Vector3(-0.05, 0.04, 0)
    }));
  }
  if (selectedCell) {
    const cellLeftX = selectedCell.xMm * MILLIMETRES_TO_METRES - input.widthMetres / 2;
    const cellRightX = (selectedCell.xMm + selectedCell.widthMm) * MILLIMETRES_TO_METRES -
      input.widthMetres / 2;
    const cellTopY = input.heightMetres / 2 - selectedCell.yMm * MILLIMETRES_TO_METRES;
    const cellBottomY = input.heightMetres / 2 -
      (selectedCell.yMm + selectedCell.heightMm) * MILLIMETRES_TO_METRES;
    add(createThreeLinearDimensionAnnotation({
      dimensionId: `${selectedCell.objectId}:dimension.cell-width`,
      kind: "cell-width",
      valueMm: selectedCell.widthMm,
      label: `单元宽 ${formatMm(selectedCell.widthMm)}`,
      measurementStart: new Vector3(cellLeftX, cellTopY, frontZ),
      measurementEnd: new Vector3(cellRightX, cellTopY, frontZ),
      dimensionStart: new Vector3(cellLeftX, cellTopY + 0.075, frontZ),
      dimensionEnd: new Vector3(cellRightX, cellTopY + 0.075, frontZ),
      tickDirection: new Vector3(0, 0.024, 0),
      labelOffset: new Vector3(0, 0.045, 0)
    }));
    add(createThreeLinearDimensionAnnotation({
      dimensionId: `${selectedCell.objectId}:dimension.cell-height`,
      kind: "cell-height",
      valueMm: selectedCell.heightMm,
      label: `单元高 ${formatMm(selectedCell.heightMm)}`,
      measurementStart: new Vector3(cellLeftX, cellBottomY, frontZ),
      measurementEnd: new Vector3(cellLeftX, cellTopY, frontZ),
      dimensionStart: new Vector3(cellLeftX - 0.075, cellBottomY, frontZ),
      dimensionEnd: new Vector3(cellLeftX - 0.075, cellTopY, frontZ),
      tickDirection: new Vector3(0.024, 0, 0),
      labelOffset: new Vector3(-0.06, 0, 0)
    }));
  }
  const selectedComponent = selectedFrame ?? selectedMember;
  if (selectedComponent) {
    const componentCenterX = (selectedComponent.xMm + selectedComponent.widthMm / 2) *
      MILLIMETRES_TO_METRES - input.widthMetres / 2;
    const componentCenterY = input.heightMetres / 2 -
      (selectedComponent.yMm + selectedComponent.heightMm / 2) * MILLIMETRES_TO_METRES;
    const labelOrigin = new Vector3(componentCenterX, componentCenterY, frontZ + 0.05);
    const dimensions = [
      ["component-width", "构件宽", selectedComponent.widthMm, 0.08],
      ["component-height", "构件高", selectedComponent.heightMm, 0]
    ] as const;
    for (const [kind, label, valueMm, offsetY] of dimensions) {
      add(createThreeLinearDimensionAnnotation({
        dimensionId: `${selectedComponent.objectId}:dimension.${kind}`,
        kind,
        valueMm,
        label: `${label} ${formatMm(valueMm)}`,
        measurementStart: labelOrigin,
        measurementEnd: labelOrigin,
        dimensionStart: labelOrigin,
        dimensionEnd: labelOrigin,
        tickDirection: new Vector3(),
        labelOffset: new Vector3(0.11, offsetY, 0)
      }));
    }
    if (selectedFrame) {
      add(createThreeLinearDimensionAnnotation({
        dimensionId: `${selectedFrame.objectId}:dimension.component-depth`,
        kind: "component-depth",
        valueMm: input.frameDepthMetres / MILLIMETRES_TO_METRES,
        label: `构件深 ${formatMm(input.frameDepthMetres / MILLIMETRES_TO_METRES)}`,
        measurementStart: labelOrigin,
        measurementEnd: labelOrigin,
        dimensionStart: labelOrigin,
        dimensionEnd: labelOrigin,
        tickDirection: new Vector3(),
        labelOffset: new Vector3(0.11, -0.08, 0)
      }));
    }
  }
  if (selectedInstallationPiece) {
    const widthMm = selectedInstallationPiece.max.x - selectedInstallationPiece.min.x;
    const heightMm = selectedInstallationPiece.max.y - selectedInstallationPiece.min.y;
    const depthMm = selectedInstallationPiece.max.z - selectedInstallationPiece.min.z;
    const center = new Vector3(
      (selectedInstallationPiece.min.x + selectedInstallationPiece.max.x) / 2 *
        MILLIMETRES_TO_METRES,
      (selectedInstallationPiece.min.y + selectedInstallationPiece.max.y) / 2 *
        MILLIMETRES_TO_METRES,
      selectedInstallationPiece.max.z * MILLIMETRES_TO_METRES + 0.05
    );
    const dimensions = [
      ["component-width", "构件宽", widthMm, 0.08],
      ["component-height", "构件高", heightMm, 0],
      ["component-depth", "构件深", depthMm, -0.08]
    ] as const;
    for (const [kind, label, valueMm, offsetY] of dimensions) {
      add(createThreeLinearDimensionAnnotation({
        dimensionId: `${input.selectedObjectId}:dimension.${kind}`,
        kind,
        valueMm,
        label: `${label} ${formatMm(valueMm)}`,
        measurementStart: center,
        measurementEnd: center,
        dimensionStart: center,
        dimensionEnd: center,
        tickDirection: new Vector3(),
        labelOffset: new Vector3(0.11, offsetY, 0)
      }));
    }
  }
  for (const opening of input.geometry.openings) {
    const openingLeftX = opening.xMm * MILLIMETRES_TO_METRES - input.widthMetres / 2;
    const openingRightX = (opening.xMm + opening.widthMm) * MILLIMETRES_TO_METRES -
      input.widthMetres / 2;
    const openingTopY = input.heightMetres / 2 - opening.yMm * MILLIMETRES_TO_METRES;
    const openingBottomY = input.heightMetres / 2 -
      (opening.yMm + opening.heightMm) * MILLIMETRES_TO_METRES;
    const panelPrefix = `${opening.panelId}扇`;
    add(createThreeLinearDimensionAnnotation({
      dimensionId: `${opening.objectId}:${opening.panelId}:dimension.width`,
      kind: "opening-width",
      valueMm: opening.widthMm,
      label: `${panelPrefix}宽 ${formatMm(opening.widthMm)}`,
      measurementStart: new Vector3(openingLeftX, openingTopY, frontZ),
      measurementEnd: new Vector3(openingRightX, openingTopY, frontZ),
      dimensionStart: new Vector3(openingLeftX, openingTopY + 0.075, frontZ),
      dimensionEnd: new Vector3(openingRightX, openingTopY + 0.075, frontZ),
      tickDirection: new Vector3(0, 0.024, 0),
      labelOffset: new Vector3(0, 0.045, 0)
    }));
    add(createThreeLinearDimensionAnnotation({
      dimensionId: `${opening.objectId}:${opening.panelId}:dimension.height`,
      kind: "opening-height",
      valueMm: opening.heightMm,
      label: `${panelPrefix}高 ${formatMm(opening.heightMm)}`,
      measurementStart: new Vector3(openingLeftX, openingBottomY, frontZ),
      measurementEnd: new Vector3(openingLeftX, openingTopY, frontZ),
      dimensionStart: new Vector3(openingLeftX - 0.075, openingBottomY, frontZ),
      dimensionEnd: new Vector3(openingLeftX - 0.075, openingTopY, frontZ),
      tickDirection: new Vector3(0.024, 0, 0),
      labelOffset: new Vector3(-0.06, 0, 0)
    }));
  }
  const firstOpening = selectedOpening ?? input.geometry.openings[0];
  if (firstOpening) {
    const x = (firstOpening.xMm + firstOpening.widthMm) * MILLIMETRES_TO_METRES -
      input.widthMetres / 2;
    const y = input.heightMetres / 2 - firstOpening.yMm * MILLIMETRES_TO_METRES + 0.13;
    const rearZ = input.closedSashCenterZ - input.sashDepthMetres / 2;
    const sashFrontZ = input.closedSashCenterZ + input.sashDepthMetres / 2;
    add(createThreeLinearDimensionAnnotation({
      dimensionId: `${firstOpening.objectId}:${firstOpening.panelId}:dimension.sash-depth`,
      kind: "sash-depth",
      valueMm: input.sashDepthMetres / MILLIMETRES_TO_METRES,
      label: `扇深 ${formatMm(input.sashDepthMetres / MILLIMETRES_TO_METRES)}`,
      measurementStart: new Vector3(x, y - 0.06, rearZ),
      measurementEnd: new Vector3(x, y - 0.06, sashFrontZ),
      dimensionStart: new Vector3(x, y, rearZ),
      dimensionEnd: new Vector3(x, y, sashFrontZ),
      tickDirection: new Vector3(0.026, 0, 0),
      labelOffset: new Vector3(0, 0.07, 0)
    }));
  }
  if (selectedHardware) {
    const configuration = resolveWindowVisualConfigurationForRender(input.window);
    const model = resolveHardwareComponentModel(
      configuration,
      selectedHardware.role,
      selectedHardware.hardwareId
    );
    if (model) {
      const centerX = (selectedHardware.xMm + selectedHardware.widthMm / 2) *
        MILLIMETRES_TO_METRES - input.widthMetres / 2;
      const centerY = input.heightMetres / 2 -
        (selectedHardware.yMm + selectedHardware.heightMm / 2) * MILLIMETRES_TO_METRES;
      const labelOrigin = new Vector3(centerX, centerY, frontZ + 0.05);
      const hardwareDimensions = [
        ["hardware-width", "宽", model.dimensionsMm.widthMm, 0.08],
        ["hardware-height", "高", model.dimensionsMm.heightMm, 0],
        ["hardware-depth", "深", model.dimensionsMm.depthMm, -0.08]
      ] as const;
      for (const [kind, label, valueMm, offsetY] of hardwareDimensions) {
        add(createThreeLinearDimensionAnnotation({
          dimensionId: `${selectedHardware.hardwareId}:dimension.${kind}`,
          kind,
          valueMm,
          label: `${label} ${formatMm(valueMm)}`,
          measurementStart: labelOrigin,
          measurementEnd: labelOrigin,
          dimensionStart: labelOrigin,
          dimensionEnd: labelOrigin,
          tickDirection: new Vector3(),
          labelOffset: new Vector3(0.11, offsetY, 0)
        }));
      }
    }
  }
  return collection;
}

/**
 * Builds Three.js scene objects from the same domain snapshot used by SVG.
 *
 * The builder creates display geometry only. Stable IDs are copied into
 * `userData.objectId` for picking, while all dimensions continue to come from
 * the domain model. WebGL renderer, camera and device quality belong to the
 * presentation shell and are intentionally absent here.
 *
 * @example `const group = new ThreeDesignSceneBuilder().build(document)`.
 * @since 0.1.0
 * @modified 2026-09-17 - Added first shared Three.js scene projection.
 */
export class ThreeDesignSceneBuilder {
  /**
   * Creates a fresh scene subtree for a complete design snapshot.
   *
   * Algorithm: build each window at metre scale, place windows in a horizontal
   * assembly preview and attach document revision metadata to the root.
   *
   * @param document Shared immutable design snapshot.
   * @returns A Three.js group ready for a renderer-owned scene.
   * @example The first 1200mm window is positioned at x = 0.6 metres.
   * @since 0.1.0
   * @modified 2026-09-20 - Kept multi-window factory previews free of implicit host walls.
   */
  build(document: DesignDocument, options: ThreeDesignSceneBuildOptions = {}): Group {
    const root = new Group();
    const materialCache = new ThreeAppearanceMaterialCache(options.visualAssets);
    root.name = "DoorMesDesign";
    root.userData.designId = document.designId;
    root.userData.revision = document.revision;

    const assemblyById = new Map(
      (document.assemblies ?? []).map((assembly) => [assembly.objectId, assembly])
    );
    const windowById = new Map(document.windows.map((window) => [window.objectId, window]));
    let cursorMetres = 0;
    for (const subject of resolveInstallationSubjects(document)) {
      if (subject.kind === "fabrication-assembly") {
        const assembly = assemblyById.get(subject.objectId);
        if (!assembly) throw new Error(`Fabrication assembly ${subject.objectId} could not be resolved.`);
        const geometry = resolveFabricationAssemblyGeometry(assembly, document.windows);
        const assemblyGroup = this.#buildFabricationAssembly(
          assembly,
          geometry,
          windowById,
          options,
          materialCache
        );
        const widthMetres = geometry.planBounds.widthMm * MILLIMETRES_TO_METRES;
        assemblyGroup.position.x = cursorMetres + widthMetres / 2;
        cursorMetres += widthMetres + 0.2;
        root.add(assemblyGroup);
        continue;
      }
      const window = windowById.get(subject.windowIds[0] ?? subject.objectId);
      if (!window) throw new Error(`Window subject ${subject.objectId} could not be resolved.`);
      const windowGroup = this.#buildWindow(window, options, materialCache);
      const widthMetres = window.widthMm * MILLIMETRES_TO_METRES;
      windowGroup.position.x = cursorMetres + widthMetres / 2;
      cursorMetres += widthMetres + 0.2;
      root.add(windowGroup);
    }
    return root;
  }

  /**
   * Projects one connected graph as a single product-space scene root.
   *
   * Each member retains its own stable window/component IDs, while the assembly
   * owns placement, joint meshes, package and reference wall. This prevents the
   * renderer from turning connected windows back into unrelated preview cards.
   *
   * @since 0.10.46
   * @modified 2026-09-20 - Added connected assembly rendering and shared host.
   */
  #buildFabricationAssembly(
    assembly: FabricationAssembly,
    geometry: ResolvedFabricationAssemblyGeometry,
    windowById: ReadonlyMap<string, WindowUnit>,
    options: ThreeDesignSceneBuildOptions,
    materialCache: ThreeAppearanceMaterialCache
  ): Group {
    const group = new Group();
    const firstWindow = windowById.get(assembly.instances[0]?.windowId ?? "");
    if (!firstWindow) throw new Error(`Assembly ${assembly.objectId} has no resolvable first window.`);
    this.#tag(group, assembly.objectId, firstWindow.objectId, "fabrication-assembly");
    group.name = assembly.mark;
    group.userData.sourceAssemblyId = assembly.objectId;
    group.userData.windowIds = assembly.instances.map((instance) => instance.windowId);
    group.userData.boundsMm = { ...geometry.bounds };
    group.userData.planBoundsMm = { ...geometry.planBounds };
    group.userData.isCoplanar = geometry.isCoplanar;
    group.userData.recommendedOpeningMm = { ...geometry.recommendedOpening };
    const centerXMm = geometry.bounds.xMm + geometry.bounds.widthMm / 2;
    const centerYMm = geometry.bounds.yMm + geometry.bounds.heightMm / 2;
    const planCenterXMm = (geometry.planBounds.minXMm + geometry.planBounds.maxXMm) / 2;
    const planCenterZMm = (geometry.planBounds.minZMm + geometry.planBounds.maxZMm) / 2;
    const frameDepthMm = Math.max(...assembly.instances.map((instance) => {
      const window = windowById.get(instance.windowId);
      if (!window) throw new Error(`Assembly window ${instance.windowId} could not be resolved.`);
      return resolveWindowSectionDimensions(window).frameDepthMm;
    }));
    const appearances = resolveWindowVisualConfigurationForRender(firstWindow).appearance;
    const assemblyPlaneZMetres = (assembly.instances[0]?.transform.zMm ?? 0) *
      MILLIMETRES_TO_METRES;
    const requestedHostId = options.installationHostWindowId;
    const ownsRequestedHost = !requestedHostId || requestedHostId === assembly.objectId ||
      assembly.instances.some((instance) => instance.windowId === requestedHostId);
    if (options.showInstallationHost === true && ownsRequestedHost) {
      const host = geometry.isCoplanar
        ? createThreeFabricationAssemblyHost(
          geometry,
          frameDepthMm,
          materialCache,
          appearances.wall
        )
        : createThreeSpatialFabricationAssemblyHost(
          geometry,
          windowById,
          materialCache,
          appearances.wall
        );
      if (geometry.isCoplanar) host.position.z = assemblyPlaneZMetres;
      group.add(host);
    }
    if (
      normalizeWindowInstallation(assembly.installation).surround.enabled &&
      options.showInstallationSurround !== false
    ) {
      const surroundAppearances = {
        outside: appearances.surroundOutside,
        inside: appearances.surroundInside,
        liner: appearances.surroundLiner
      };
      const surround = geometry.isCoplanar
        ? createThreeFabricationAssemblySurround(
          geometry,
          frameDepthMm,
          materialCache,
          surroundAppearances
        )
        : createThreeSpatialFabricationAssemblySurround(
          geometry,
          windowById,
          materialCache,
          surroundAppearances
        );
      if (geometry.isCoplanar) surround.position.z = assemblyPlaneZMetres;
      group.add(surround);
    }
    const childOptions: ThreeDesignSceneBuildOptions = {
      ...options,
      showInstallationHost: false,
      showInstallationSurround: false
    };
    for (const instance of geometry.instances) {
      const window = windowById.get(instance.windowId);
      if (!window) throw new Error(`Assembly window ${instance.windowId} could not be resolved.`);
      const persistedInstance = assembly.instances.find(
        (candidate) => candidate.objectId === instance.instanceId
      );
      if (!persistedInstance) {
        throw new Error(`Assembly instance ${instance.instanceId} could not be resolved.`);
      }
      const planInstance = geometry.planInstances.find(
        (candidate) => candidate.instanceId === instance.instanceId
      );
      if (!planInstance) {
        throw new Error(`Assembly plan instance ${instance.instanceId} could not be resolved.`);
      }
      const planCenterTotal = planInstance.footprint.reduce(
        (sum, point) => ({ xMm: sum.xMm + point.xMm, zMm: sum.zMm + point.zMm }),
        { xMm: 0, zMm: 0 }
      );
      const planCenter = {
        xMm: planCenterTotal.xMm / planInstance.footprint.length,
        zMm: planCenterTotal.zMm / planInstance.footprint.length
      };
      const selectsInstance = options.linearDimensionObjectId === instance.instanceId;
      const child = this.#buildWindow(
        window,
        selectsInstance
          ? { ...childOptions, linearDimensionObjectId: window.objectId }
          : childOptions,
        materialCache
      );
      child.position.set(
        (planCenter.xMm - planCenterXMm) * MILLIMETRES_TO_METRES,
        (centerYMm - instance.yMm - instance.heightMm / 2) * MILLIMETRES_TO_METRES,
        (planCenter.zMm - planCenterZMm) * MILLIMETRES_TO_METRES
      );
      child.rotation.y = persistedInstance.transform.rotationYDeg * Math.PI / 180;
      child.userData.sourceAssemblyId = assembly.objectId;
      child.userData.assemblyInstanceId = instance.instanceId;
      group.add(child);
    }
    const jointMaterial = materialCache.resolveBoxSurface(appearances.mullion);
    for (const [jointIndex, joint] of geometry.joints.entries()) {
      const persistedJoint = geometry.assembly.joints.find(
        (candidate) => candidate.objectId === joint.jointId
      );
      const planJoint = geometry.planJoints.find(
        (candidate) => candidate.jointId === joint.jointId
      );
      const cornerFootprint = planJoint?.footprint.map((point) => ({
        xMm: point.xMm - (planJoint?.axisXMm ?? 0),
        zMm: point.zMm - (planJoint?.axisZMm ?? 0)
      })) ?? [];
      const mesh = new Mesh(
        joint.jointType === "corner_joint" && cornerFootprint.length >= 3
          ? createVerticalPlanPrismGeometry(cornerFootprint, joint.heightMm)
          : new BoxGeometry(
            Math.max(0.001, joint.widthMm * MILLIMETRES_TO_METRES),
            Math.max(0.001, joint.heightMm * MILLIMETRES_TO_METRES),
            frameDepthMm * MILLIMETRES_TO_METRES
          ),
        joint.jointType === "corner_joint"
          ? createStableContactMaterial(
            materialCache.resolve(appearances.mullion.edge),
            geometry.planInstances.length + jointIndex + 1
          )
          : jointMaterial
      );
      mesh.position.set(
        ((planJoint?.axisXMm ?? (joint.xMm + joint.widthMm / 2)) - planCenterXMm) *
          MILLIMETRES_TO_METRES,
        (centerYMm - joint.yMm - joint.heightMm / 2) * MILLIMETRES_TO_METRES,
        ((planJoint?.axisZMm ?? 0) - planCenterZMm) * MILLIMETRES_TO_METRES
      );
      this.#tag(
        mesh,
        joint.jointId,
        firstWindow.objectId,
        "engineering-joint",
        `assembly.joint.${joint.jointType}`
      );
      mesh.userData.sourceAssemblyId = assembly.objectId;
      mesh.userData.jointType = joint.jointType;
      if (planJoint) {
        mesh.userData.includedAngleDeg = planJoint.includedAngleDeg;
        mesh.userData.turnDirection = planJoint.turnDirection;
        mesh.userData.cornerPostMode = planJoint.cornerPostMode;
        mesh.userData.profileModelId = planJoint.profileModelId;
        if (planJoint.cornerInterface) {
          mesh.userData.cornerInterfaceSchemaVersion =
            planJoint.cornerInterface.schemaVersion;
          mesh.userData.profileDepthMm = planJoint.cornerInterface.profileDepthMm;
          mesh.userData.firstMemberEndCutDeg =
            planJoint.cornerInterface.firstMemberEndCutDeg;
          mesh.userData.secondMemberEndCutDeg =
            planJoint.cornerInterface.secondMemberEndCutDeg;
          mesh.userData.firstContactFace = planJoint.cornerInterface.firstContactFace.map(
            (point) => ({ ...point })
          );
          mesh.userData.secondContactFace = planJoint.cornerInterface.secondContactFace.map(
            (point) => ({ ...point })
          );
          mesh.userData.joinSeam = { ...planJoint.cornerInterface.seam };
          mesh.userData.renderStability = "exact-contact-prism-with-depth-priority";
          mesh.renderOrder = geometry.planInstances.length + jointIndex + 1;
        }
      }
      if (persistedJoint?.catalogSelection) {
        mesh.userData.catalogItemId = persistedJoint.catalogSelection.catalogItemId;
        mesh.userData.catalogVersion = persistedJoint.catalogSelection.catalogVersion;
        mesh.userData.manufacturingRuleId =
          persistedJoint.catalogSelection.manufacturingRuleId;
      }
      mesh.userData.firstInstanceId = joint.firstInstanceId;
      mesh.userData.secondInstanceId = joint.secondInstanceId;
      group.add(mesh);
    }
    if (options.showLinearDimensions && options.linearDimensionObjectId) {
      const selectionIsAssemblyOrJoint = options.linearDimensionObjectId === assembly.objectId ||
        geometry.joints.some((joint) => joint.jointId === options.linearDimensionObjectId);
      if (selectionIsAssemblyOrJoint) {
        group.add(createThreeFabricationAssemblyDimensionAnnotations(
          geometry,
          options.linearDimensionObjectId
        ));
      }
    }
    return group;
  }

  /**
   * Creates frame segments, cell panes and grid/topology member meshes.
   *
   * Every child carries its own stable object ID plus its owning window and
   * manufacturing source component. Invalid topology members use a warning
   * material and retain `partitionValid=false` for presentation controls.
   *
   * @param window Domain window entity.
   * @returns A Three.js group representing the window.
   * @example A custom 90mm frame depth becomes a 0.09m deep frame mesh.
   * @since 0.1.0
   * @modified 2026-09-20 - Made installation hosts explicit presentation options.
   */
  #buildWindow(
    window: WindowUnit,
    options: ThreeDesignSceneBuildOptions,
    materialCache: ThreeAppearanceMaterialCache
  ): Group {
    const group = new Group();
    group.name = window.mark;
    this.#tag(group, window.objectId, window.objectId, "window");

    const width = window.widthMm * MILLIMETRES_TO_METRES;
    const height = window.heightMm * MILLIMETRES_TO_METRES;
    const section = resolveWindowSectionDimensions(window);
    const installation = normalizeWindowInstallation(window.installation);
    const installationSection = resolveWindowInstallationSection(window);
    const visualConfiguration = resolveWindowVisualConfigurationForRender(window);
    const depth = section.frameDepthMm * MILLIMETRES_TO_METRES;
    const sashProfileDepth = section.sashDepthMm * MILLIMETRES_TO_METRES;
    const glassDepth = section.glassDepthMm * MILLIMETRES_TO_METRES;
    const hardwareProjection = section.hardwareProjectionMm * MILLIMETRES_TO_METRES;
    const flyingMullionDepth = section.flyingMullionDepthMm * MILLIMETRES_TO_METRES;
    const flyingMullionFrontLocalZ = sashProfileDepth / 2 +
      section.flyingMullionFrontProjectionMm * MILLIMETRES_TO_METRES;
    const flyingMullionCenterLocalZ = flyingMullionFrontLocalZ - flyingMullionDepth / 2;
    /*
     * Seat the closed sash from the shared catalog/design section snapshot.
     * The centre equals frame-front minus configured front setback and half the
     * sash depth. Validation guarantees the rear face also remains inside the
     * frame. Hardware projection is handled independently below.
     * @since 0.9.0
     * @modified 2026-09-17 - Corrected closed-sash frame seating.
     */
    const closedSashCenterZ = depth / 2 - sashProfileDepth / 2 -
      section.sashFrontSetbackMm * MILLIMETRES_TO_METRES;
    group.userData.sectionPresetId = section.presetId;
    group.userData.sectionDimensions = { ...section };
    group.userData.installation = {
      ...installation,
      surround: { ...installation.surround, sides: [...installation.surround.sides] }
    };
    group.userData.installationSection = { ...installationSection };
    const geometry = resolveWindowGeometry(window);
    const frameMaterial = materialCache.resolveBoxSurface(visualConfiguration.appearance.frame);
    const sashMaterial = materialCache.resolveBoxSurface(visualConfiguration.appearance.sash);
    const mullionMaterial = materialCache.resolveBoxSurface(visualConfiguration.appearance.mullion);
    const invalidMaterial = new MeshStandardMaterial({ color: 0xdc2626 });
    const glassMaterial = materialCache.resolve(visualConfiguration.appearance.glass);
    const hardwareDarkMaterial = materialCache.resolve(
      visualConfiguration.appearance.hardwareDefault
    );
    const hardwareAccentMaterial = materialCache.resolve(
      resolveHardwareComponentModel(visualConfiguration, "handle")?.appearance ??
        visualConfiguration.appearance.hardwareDefault
    );
    const flyingMullionMaterial = materialCache.resolveBoxSurface(
      visualConfiguration.appearance.flyingMullion
    );

    if (
      options.showInstallationHost === true &&
      (!options.installationHostWindowId || options.installationHostWindowId === window.objectId)
    ) {
      group.add(createThreeInstallationHost(
        window,
        width,
        height,
        materialCache,
        visualConfiguration.appearance.wall
      ));
    }
    if (installation.surround.enabled && options.showInstallationSurround !== false) {
      group.add(createThreeInstallationSurround(window, width, height, materialCache, {
        outside: visualConfiguration.appearance.surroundOutside,
        inside: visualConfiguration.appearance.surroundInside,
        liner: visualConfiguration.appearance.surroundLiner
      }));
    }

    const addRectangle = (input: {
      rectangle: ResolvedRectangleMm;
      objectId: string;
      sourceComponentId: string;
      objectType: "frame-segment" | "grid-divider" | "topology-member" | "fixed-mullion" | "cell";
      material: MeshStandardMaterial | MeshStandardMaterial[];
      meshDepth: number;
      partitionValid?: boolean;
    }): void => {
      const rectangle = input.rectangle;
      const mesh = new Mesh(
        new BoxGeometry(
          Math.max(0.001, rectangle.widthMm * MILLIMETRES_TO_METRES),
          Math.max(0.001, rectangle.heightMm * MILLIMETRES_TO_METRES),
          input.meshDepth
        ),
        input.material
      );
      mesh.position.set(
        (rectangle.xMm + rectangle.widthMm / 2) * MILLIMETRES_TO_METRES - width / 2,
        height / 2 - (rectangle.yMm + rectangle.heightMm / 2) * MILLIMETRES_TO_METRES,
        input.objectType === "cell" ? -0.01 : 0
      );
      this.#tag(
        mesh,
        input.objectId,
        window.objectId,
        input.objectType,
        input.sourceComponentId,
        input.partitionValid
      );
      group.add(mesh);
    };

    for (const frame of geometry.frames) {
      addRectangle({
        rectangle: frame,
        objectId: frame.objectId,
        sourceComponentId: frame.sourceComponentId,
        objectType: "frame-segment",
        material: frameMaterial,
        meshDepth: depth
      });
    }
    const openingCellIds = new Set(geometry.openings.map((opening) => opening.objectId));
    for (const cell of geometry.cells) {
      if (openingCellIds.has(cell.objectId)) continue;
      addRectangle({
        rectangle: cell,
        objectId: cell.objectId,
        sourceComponentId: `cell.${cell.row + 1}.${cell.column + 1}`,
        objectType: "cell",
        material: glassMaterial,
        meshDepth: glassDepth
      });
    }
    const openingAssemblyGroups = new Map<string, Group>();
    const panelGroups = new Map<string, {
      group: Group;
      opening: (typeof geometry.openings)[number];
      pivotXMm: number;
      pivotYMm: number;
      motionMode: OpeningMotionMode;
      previewKey: string;
    }>();
    for (const opening of geometry.openings) {
      let assemblyGroup = openingAssemblyGroups.get(opening.objectId);
      if (!assemblyGroup) {
        assemblyGroup = new Group();
        this.#tag(
          assemblyGroup,
          opening.objectId,
          window.objectId,
          "opening-assembly",
          opening.assemblySourceComponentId
        );
        openingAssemblyGroups.set(opening.objectId, assemblyGroup);
        group.add(assemblyGroup);
      }
      const sashGroup = new Group();
      const panelWidth = opening.widthMm * MILLIMETRES_TO_METRES;
      const panelHeight = opening.heightMm * MILLIMETRES_TO_METRES;
      const face = Math.min(
        opening.sashFaceMm * MILLIMETRES_TO_METRES,
        panelWidth / 2,
        panelHeight / 2
      );
      const previewKey = createOpeningPanelKey(opening.objectId, opening.panelId);
      const motionMode = options.openingMotionModeByPanelKey?.[previewKey] ?? "primary";
      const pivotEdge = opening.type === "turn_tilt" && motionMode === "tilt"
        ? "bottom"
        : opening.hingeEdge;
      const horizontalHinge = pivotEdge === "top" || pivotEdge === "bottom";
      const direction = pivotEdge === "left" ? 1 : -1;
      const pivotXMm = pivotEdge === "left"
        ? opening.xMm
        : pivotEdge === "right"
          ? opening.xMm + opening.widthMm
          : opening.xMm + opening.widthMm / 2;
      const pivotYMm = pivotEdge === "top"
        ? opening.yMm
        : pivotEdge === "bottom"
          ? opening.yMm + opening.heightMm
          : opening.yMm + opening.heightMm / 2;
      sashGroup.position.set(
        pivotXMm * MILLIMETRES_TO_METRES - width / 2,
        height / 2 - pivotYMm * MILLIMETRES_TO_METRES,
        closedSashCenterZ
      );
      /**
       * Applies the shared renderer-neutral hinge solver after establishing the
       * physical pivot. Runtime progress wins when supplied; otherwise the v2
       * configured target keeps migrated fixtures visually unchanged.
       */
      const previewProgress =
        options.openingProgressPercentByPanelKey?.[previewKey] ?? opening.openPercent;
      const pose = resolveOpeningPose(
        createOpeningMechanismMotion({
          motionId: `${previewKey}:${motionMode}`,
          mechanism: opening.hingeEdge === "top" ? "top_hung" : "tilt_turn",
          motionMode,
          hingeEdge: opening.hingeEdge,
          openPlane: opening.opening.endsWith("_out") ? "out" : "in",
          maximumAngleDegrees: motionMode === "tilt"
            ? opening.maximumAngleDegreesByMode?.tilt
            : opening.maximumAngleDegreesByMode?.primary
        }),
        previewProgress
      );
      sashGroup.position.x += pose.translationMm.x * MILLIMETRES_TO_METRES;
      sashGroup.position.y += pose.translationMm.y * MILLIMETRES_TO_METRES;
      sashGroup.position.z += pose.translationMm.z * MILLIMETRES_TO_METRES;
      sashGroup.rotation.set(
        pose.rotationRadians.x,
        pose.rotationRadians.y,
        pose.rotationRadians.z
      );
      sashGroup.scale.set(pose.scale.x, pose.scale.y, pose.scale.z);
      this.#tag(
        sashGroup,
        previewKey,
        window.objectId,
        "opening-panel",
        opening.sourceComponentId
      );
      sashGroup.userData.panelId = opening.panelId;
      sashGroup.userData.panelRole = opening.panelRole;
      sashGroup.userData.operationOrder = opening.operationOrder;
      sashGroup.userData.previewPanelKey = previewKey;
      sashGroup.userData.previewProgressPercent = pose.progressPercent;
      sashGroup.userData.previewAngleDegrees = Math.max(
        Math.abs(pose.rotationRadians.x),
        Math.abs(pose.rotationRadians.y),
        Math.abs(pose.rotationRadians.z)
      ) * 180 / Math.PI;
      sashGroup.userData.motionMode = motionMode;
      sashGroup.userData.pivotEdge = pivotEdge;
      sashGroup.userData.openPlane = opening.opening.endsWith("_out") ? "out" : "in";

      const addSashPart = (
        partWidth: number,
        partHeight: number,
        x: number,
        y: number,
        material: MeshStandardMaterial | MeshStandardMaterial[],
        part: string,
        meshDepth: number
      ): void => {
        const mesh = new Mesh(
          new BoxGeometry(Math.max(0.001, partWidth), Math.max(0.001, partHeight), meshDepth),
          material
        );
        mesh.position.set(x, y, 0);
        this.#tag(
          mesh,
          previewKey,
          window.objectId,
          "opening-panel",
          `${opening.sourceComponentId}.${part}`
        );
        sashGroup.add(mesh);
      };
      const topPivot = pivotEdge === "top";
      const centerX = horizontalHinge ? 0 : direction * panelWidth / 2;
      const centerY = horizontalHinge ? (topPivot ? -panelHeight / 2 : panelHeight / 2) : 0;
      addSashPart(panelWidth, face, centerX, horizontalHinge ? (topPivot ? -face / 2 : panelHeight - face / 2) : panelHeight / 2 - face / 2, sashMaterial, "sash.top", sashProfileDepth);
      addSashPart(panelWidth, face, centerX, horizontalHinge ? (topPivot ? -panelHeight + face / 2 : face / 2) : -panelHeight / 2 + face / 2, sashMaterial, "sash.bottom", sashProfileDepth);
      addSashPart(face, panelHeight, horizontalHinge ? -panelWidth / 2 + face / 2 : direction * face / 2, centerY, sashMaterial, "sash.left", sashProfileDepth);
      addSashPart(face, panelHeight, horizontalHinge ? panelWidth / 2 - face / 2 : direction * (panelWidth - face / 2), centerY, sashMaterial, "sash.right", sashProfileDepth);
      addSashPart(
        Math.max(0.001, panelWidth - face * 2),
        Math.max(0.001, panelHeight - face * 2),
        centerX,
        centerY,
        glassMaterial,
        "glass",
        glassDepth
      );
      assemblyGroup.add(sashGroup);
      /**
       * Scope opening angles with the same exact-object rule as linear labels.
       *
       * A panel key keeps one angle and a cell selection keeps its complete
       * opening assembly. Hardware, window, frame, wall and package selections
       * show only their own linear dimensions, so they cannot inherit a host
       * sash's 72° label. With no selection, the opening-state overlay may still
       * show every panel because the user explicitly enabled that global view.
       *
       * @example Selecting P2 shows one angle; selecting P2's handle shows none.
       * @since 0.10.41
       * @modified 2026-09-20 - Unified angle and linear-dimension selection scope.
       */
      const selectedDimensionObjectId = options.linearDimensionObjectId;
      const angleBelongsToSelection = !selectedDimensionObjectId ||
        selectedDimensionObjectId === opening.objectId ||
        selectedDimensionObjectId === previewKey;
      if (options.showOpeningAngleAnnotations !== false && angleBelongsToSelection) {
        assemblyGroup.add(createThreeOpeningAngleAnnotation({
          previewKey,
          pivotEdge,
          pivotXMm,
          pivotYMm,
          openingXmm: opening.xMm,
          openingYmm: opening.yMm,
          openingWidthMm: opening.widthMm,
          openingHeightMm: opening.heightMm,
          windowWidthMetres: width,
          windowHeightMetres: height,
          closedSashCenterZ,
          rotationRadians: pose.rotationRadians
        }));
      }
      panelGroups.set(`${opening.objectId}:${opening.panelId}`, {
        group: sashGroup,
        opening,
        pivotXMm,
        pivotYMm,
        motionMode,
        previewKey
      });
    }
    /**
     * Adds the effective motion connections independently from production BOM.
     *
     * The moving and stationary endpoints consume the same normalized preset as
     * SVG. Tilt mode therefore shows bottom pivots plus a head stay even though
     * the complete tilt-turn hardware set still contains side transmission
     * parts. A future catalog editor can inject a per-panel preset through build
     * options without changing this projection algorithm.
     *
     * @example A right-hinged panel in tilt mode produces two bottom pivots and
     * one top stay whose bar joins the moving sash to the stationary head.
     * @since 0.8.9
     * @modified 2026-09-17 - Added configurable direction-aware connections.
     */
    for (const owner of panelGroups.values()) {
      const { opening, group: sashGroup, pivotXMm, pivotYMm, motionMode, previewKey } = owner;
      const halfFaceMm = opening.sashFaceMm / 2;
      const leftMm = opening.xMm + halfFaceMm;
      const rightMm = opening.xMm + opening.widthMm - halfFaceMm;
      const topMm = opening.yMm + halfFaceMm;
      const bottomMm = opening.yMm + opening.heightMm - halfFaceMm;
      const pointOnEdge = (edge: "left" | "right" | "top" | "bottom", ratio: number) => ({
        xMm: edge === "left"
          ? leftMm
          : edge === "right"
            ? rightMm
            : leftMm + (rightMm - leftMm) * ratio,
        yMm: edge === "top"
          ? topMm
          : edge === "bottom"
            ? bottomMm
            : topMm + (bottomMm - topMm) * ratio
      });
      const assemblyGroup = openingAssemblyGroups.get(opening.objectId);
      if (!assemblyGroup) continue;
      const connectionLayout = resolveOpeningConnectionLayout({
        mechanism: opening.type === "top_hung" ? "top_hung" : "tilt_turn",
        motionMode,
        hingeEdge: opening.hingeEdge,
        preset: options.openingConnectionPresetByPanelKey?.[previewKey]
      });
      sashGroup.userData.connectionPresetId = connectionLayout.presetId;
      sashGroup.userData.connectionAnchorIds = connectionLayout.anchors.map((anchor) => anchor.id);
      sashGroup.updateMatrix();
      for (const anchor of connectionLayout.anchors) {
        const sashPointMm = pointOnEdge(anchor.sashEdge, anchor.sashPositionRatio);
        const framePointMm = pointOnEdge(anchor.frameEdge, anchor.framePositionRatio);
        const connectionMarkerDepth = 0.016;
        const sashConnectionCenterZ = sashProfileDepth / 2 + hardwareProjection -
          connectionMarkerDepth / 2;
        const frameConnectionCenterZ = depth / 2 + hardwareProjection -
          connectionMarkerDepth / 2;
        const sashPointLocal = new Vector3(
          (sashPointMm.xMm - pivotXMm) * MILLIMETRES_TO_METRES,
          (pivotYMm - sashPointMm.yMm) * MILLIMETRES_TO_METRES,
          sashConnectionCenterZ
        );
        const sashMarker = new Mesh(
          new BoxGeometry(0.018, 0.028, connectionMarkerDepth),
          anchor.role === "stay" ? hardwareAccentMaterial : hardwareDarkMaterial
        );
        sashMarker.position.copy(sashPointLocal);
        this.#tag(
          sashMarker,
          previewKey,
          window.objectId,
          `preview-connection-${anchor.role}-sash`,
          `${opening.sourceComponentId}.connection.${anchor.id}.sash`
        );
        sashMarker.userData.connectionPresetId = connectionLayout.presetId;
        sashMarker.userData.connectionId = anchor.id;
        sashMarker.userData.connectionRole = anchor.role;
        sashMarker.userData.edge = anchor.sashEdge;
        sashGroup.add(sashMarker);

        const framePoint = new Vector3(
          framePointMm.xMm * MILLIMETRES_TO_METRES - width / 2,
          height / 2 - framePointMm.yMm * MILLIMETRES_TO_METRES,
          frameConnectionCenterZ
        );
        const frameMarker = new Mesh(
          new BoxGeometry(0.018, 0.028, connectionMarkerDepth),
          hardwareDarkMaterial
        );
        frameMarker.position.copy(framePoint);
        this.#tag(
          frameMarker,
          previewKey,
          window.objectId,
          `preview-connection-${anchor.role}-frame`,
          `${opening.sourceComponentId}.connection.${anchor.id}.frame`
        );
        frameMarker.userData.connectionPresetId = connectionLayout.presetId;
        frameMarker.userData.connectionId = anchor.id;
        frameMarker.userData.connectionRole = anchor.role;
        frameMarker.userData.edge = anchor.frameEdge;
        assemblyGroup.add(frameMarker);

        if (anchor.role === "stay") {
          const sashPointInAssembly = sashPointLocal.clone().applyMatrix4(sashGroup.matrix);
          const delta = sashPointInAssembly.clone().sub(framePoint);
          const length = Math.max(0.001, delta.length());
          const stay = new Mesh(
            new BoxGeometry(0.01, length, 0.01),
            hardwareAccentMaterial
          );
          stay.position.copy(framePoint.clone().add(sashPointInAssembly).multiplyScalar(0.5));
          stay.quaternion.setFromUnitVectors(
            new Vector3(0, 1, 0),
            delta.clone().normalize()
          );
          this.#tag(
            stay,
            previewKey,
            window.objectId,
            "preview-connection-stay-link",
            `${opening.sourceComponentId}.connection.${anchor.id}.link`
          );
          stay.userData.connectionPresetId = connectionLayout.presetId;
          stay.userData.connectionId = anchor.id;
          stay.userData.connectionRole = anchor.role;
          assemblyGroup.add(stay);
        }
      }
    }
    /**
     * Projects hardware only after every panel transform exists.
     *
     * Algorithm: stationary frame members remain under the assembly root;
     * sash and flying-mullion members are converted from shared closed-state
     * millimetres into the declared owner's hinge-local coordinates. Head/sill
     * shoot-bolt keepers remain on the stationary frame assembly.
     * This two-pass attachment is required because a P1 connection may be
     * physically mounted on P2, whose group may be created later in source order.
     *
     * @example A primary-leaf keeper targets P1's flying mullion and rotates with P1.
     * Model resolution first uses the exact generated hardware ID, then its
     * semantic role. Parametric geometry applies the saved millimetre envelope,
     * pivot and mount axis; managed GLB entries use the same bounded fallback
     * until ASSET-003 supplies reviewed bytes.
     *
     * @since 0.5.0
     * @modified 2026-09-17 - Replaced role-coloured boxes with ASSET-002 models.
     */
    for (const mount of geometry.hardware) {
      const mountOwner = panelGroups.get(
        `${mount.sourceObjectId}:${mount.mountOwnerPanelId ?? mount.panelId}`
      );
      if (
        mountOwner?.motionMode === "tilt" &&
        (mount.role === "hinge-sash-leaf" || mount.role === "hinge-frame-leaf")
      ) continue;
      const model = resolveHardwareComponentModel(
        visualConfiguration,
        mount.role,
        mount.hardwareId
      );
      if (!model) throw new Error(`No visual hardware model resolves for ${mount.hardwareId}.`);
      const visual = createThreeHardwareModel(
        model,
        materialCache,
        options.visualAssets,
        options.assetQuality ?? "high"
      );
      const modelDepth = model.dimensionsMm.depthMm * MILLIMETRES_TO_METRES;
      switch (model.mount.mountAxis) {
        case "+z":
          break;
        case "-z":
          visual.rotateX(Math.PI);
          break;
        case "+x":
          visual.rotateY(Math.PI / 2);
          break;
        case "-x":
          visual.rotateY(-Math.PI / 2);
          break;
        case "+y":
          visual.rotateX(-Math.PI / 2);
          break;
        case "-y":
          visual.rotateX(Math.PI / 2);
          break;
      }
      if (mount.edge === "top") visual.rotateZ(Math.PI / 2);
      if (mount.edge === "bottom") visual.rotateZ(-Math.PI / 2);
      if (mount.edge === "right") visual.scale.x *= -1;
      const centerXMm = mount.xMm + mount.widthMm / 2;
      const centerYMm = mount.yMm + mount.heightMm / 2;
      if (mount.mountTarget === "frame" || mount.mountTarget === "fixed-mullion") {
        const assemblyGroup = openingAssemblyGroups.get(mount.sourceObjectId);
        if (!assemblyGroup) continue;
        visual.position.set(
          centerXMm * MILLIMETRES_TO_METRES - width / 2,
          height / 2 - centerYMm * MILLIMETRES_TO_METRES,
          depth / 2 + hardwareProjection -
            (1 - model.mount.pivotRatio.z) * modelDepth
        );
        assemblyGroup.add(visual);
      } else {
        const ownerPanelId = mount.mountOwnerPanelId ?? mount.panelId;
        const owner = panelGroups.get(`${mount.sourceObjectId}:${ownerPanelId}`);
        if (!owner) continue;
        const hostFront = mount.mountTarget === "flying-mullion"
          ? flyingMullionFrontLocalZ
          : sashProfileDepth / 2;
        visual.position.set(
          (centerXMm - owner.pivotXMm) * MILLIMETRES_TO_METRES,
          (owner.pivotYMm - centerYMm) * MILLIMETRES_TO_METRES,
          hostFront + hardwareProjection -
            (1 - model.mount.pivotRatio.z) * modelDepth
        );
        owner.group.add(visual);
      }
      visual.traverse((part) => {
        this.#tag(
          part,
          mount.hardwareId,
          window.objectId,
          part === visual ? `hardware-${mount.role}` : "component-model-part",
          mount.sourceComponentId
        );
        part.userData.openingObjectId = mount.sourceObjectId;
        part.userData.hardwareId = mount.hardwareId;
        part.userData.hardwareSetId = mount.hardwareSetId;
        part.userData.mountTarget = mount.mountTarget;
        part.userData.mountOwnerPanelId = mount.mountOwnerPanelId;
        part.userData.edge = mount.edge;
        part.userData.modelId = model.modelId;
        part.userData.modelVersion = model.modelVersion;
        part.userData.modelDimensionsMm = { ...model.dimensionsMm };
        if (mount.connectionId) part.userData.connectionId = mount.connectionId;
        if (mount.matingHardwareId) part.userData.matingHardwareId = mount.matingHardwareId;
      });
    }
    /**
     * Projects fixed and flying meeting profiles according to physical ownership.
     *
     * A fixed mullion remains under the stationary window root. A flying mullion
     * is transformed into the secondary leaf's hinge-local coordinates so it
     * follows exactly the same 3D rotation as its owner.
     * @since 0.5.1
     * @modified 2026-09-17 - Added stationary fixed-mullion projection for 0.6.0.
     */
    for (const mullion of geometry.meetingMullions) {
      if (mullion.kind === "fixed-mullion") {
        addRectangle({
          rectangle: mullion,
          objectId: mullion.objectId,
          sourceComponentId: mullion.sourceComponentId,
          objectType: "fixed-mullion",
          material: mullionMaterial,
          meshDepth: depth
        });
        continue;
      }
      if (!mullion.ownerPanelId) continue;
      const owner = panelGroups.get(`${mullion.sourceObjectId}:${mullion.ownerPanelId}`);
      if (!owner) continue;
      const centerXMm = mullion.xMm + mullion.widthMm / 2;
      const centerYMm = mullion.yMm + mullion.heightMm / 2;
      const mesh = new Mesh(
        new BoxGeometry(
          Math.max(0.001, mullion.widthMm * MILLIMETRES_TO_METRES),
          Math.max(0.001, mullion.heightMm * MILLIMETRES_TO_METRES),
          flyingMullionDepth
        ),
        flyingMullionMaterial
      );
      mesh.position.set(
        (centerXMm - owner.pivotXMm) * MILLIMETRES_TO_METRES,
        (owner.pivotYMm - centerYMm) * MILLIMETRES_TO_METRES,
        flyingMullionCenterLocalZ
      );
      this.#tag(
        mesh,
        mullion.objectId,
        window.objectId,
        "flying-mullion",
        mullion.sourceComponentId
      );
      mesh.userData.ownerPanelId = mullion.ownerPanelId;
      owner.group.add(mesh);

      /**
       * Adds a visual seal face without inventing another manufactured object.
       *
       * Algorithm: derive a narrow face from the resolved flying-mullion bounds,
       * place it just in front of the sash profile, then reuse the mullion ID so
       * ray-picking either mesh selects the same physical component. This helper
       * never feeds dimensions back into BOM calculation.
       *
       * @example Selecting the dark seal highlights `CELL-1:flying-mullion`.
       * @since 0.5.3
       * @modified 2026-09-17 - Made the sash-owned flying mullion visibly distinct.
       */
      const seal = new Mesh(
        new BoxGeometry(
          Math.max(0.004, mullion.widthMm * MILLIMETRES_TO_METRES * 0.2),
          Math.max(0.001, mullion.heightMm * MILLIMETRES_TO_METRES * 0.96),
          0.012
        ),
        hardwareDarkMaterial
      );
      seal.position.set(mesh.position.x, mesh.position.y, flyingMullionFrontLocalZ + 0.006);
      this.#tag(
        seal,
        mullion.objectId,
        window.objectId,
        "flying-mullion-seal",
        `${mullion.sourceComponentId}.seal`
      );
      seal.userData.ownerPanelId = mullion.ownerPanelId;
      owner.group.add(seal);
    }
    for (const member of geometry.members) {
      addRectangle({
        rectangle: member,
        objectId: member.objectId,
        sourceComponentId: member.sourceComponentId,
        objectType: member.kind,
        material: member.partitionValid ? mullionMaterial : invalidMaterial,
        meshDepth: depth,
        partitionValid: member.partitionValid
      });
    }
    if (
      options.showLinearDimensions &&
      options.linearDimensionObjectId &&
      windowOwnsDimensionSelection(window, geometry, options.linearDimensionObjectId)
    ) {
      group.add(createThreeWindowDimensionAnnotations({
        window,
        geometry,
        selectedObjectId: options.linearDimensionObjectId,
        widthMetres: width,
        heightMetres: height,
        frameDepthMetres: depth,
        sashDepthMetres: sashProfileDepth,
        closedSashCenterZ,
        installationSection
      }));
    }
    return group;
  }

  /**
   * Attaches a stable domain identifier to a Three.js display object.
   *
   * Raycasting code reads this metadata to return a domain ID; it must never
   * return the mutable Object3D itself as application state.
   *
   * @param object Three.js display object.
   * @param objectId Stable source-domain identifier.
   * @param sourceWindowId Owning window used for selection context.
   * @param objectType Stable semantic display-object type.
   * @param sourceComponentId Optional manufacturing source component.
   * @param partitionValid Optional topology validity flag.
   * @example Tagging a member with `M-1` enables cross-view selection.
   * @since 0.1.0
   * @modified 2026-09-17 - Added scene-to-domain trace metadata.
   */
  #tag(
    object: Object3D,
    objectId: string,
    sourceWindowId: string,
    objectType: string,
    sourceComponentId?: string,
    partitionValid?: boolean
  ): void {
    object.userData.objectId = objectId;
    object.userData.sourceWindowId = sourceWindowId;
    object.userData.objectType = objectType;
    if (sourceComponentId) object.userData.sourceComponentId = sourceComponentId;
    if (partitionValid !== undefined) object.userData.partitionValid = partitionValid;
  }
}

/**
 * Browser callbacks supplied by the shared UI composition layer.
 *
 * The viewport reports stable IDs only; consumers never receive a mutable
 * Three.js mesh or renderer instance.
 *
 * @example `{ onSelect: id => selection.select(id, "3d") }`.
 * @since 0.4.0
 * @modified 2026-09-17 - Added renderer-to-application selection boundary.
 */
export interface ThreeDesignViewportOptions {
  readonly onSelect?: (objectId: string | undefined) => void;
  readonly pixelRatioLimit?: number;
}

/**
 * Calculates a camera distance that fits an object box in both viewport axes.
 *
 * This pure function is separated from WebGL so framing can be unit tested.
 * The larger of vertical and horizontal requirements is used, with padding for
 * labels, helpers and future opening animations.
 *
 * @param size World-space bounds of the design.
 * @param verticalFieldOfViewDegrees Perspective camera vertical field of view.
 * @param aspect Viewport width divided by height.
 * @returns Positive camera distance from the design centre.
 * @example A 1.2×1.5m window at aspect 1.5 fits at roughly two metres.
 * @since 0.4.0
 * @modified 2026-09-17 - Added deterministic 3D camera framing.
 */
export function calculateCameraFitDistance(
  size: Readonly<{ x: number; y: number; z: number }>,
  verticalFieldOfViewDegrees: number,
  aspect: number
): number {
  const verticalRadians = (verticalFieldOfViewDegrees * Math.PI) / 180;
  const safeAspect = Math.max(0.1, aspect);
  const verticalDistance = Math.max(0.1, size.y) / (2 * Math.tan(verticalRadians / 2));
  const horizontalFieldOfView = 2 * Math.atan(Math.tan(verticalRadians / 2) * safeAspect);
  const horizontalDistance = Math.max(0.1, size.x) / (2 * Math.tan(horizontalFieldOfView / 2));
  return Math.max(verticalDistance, horizontalDistance, size.z * 2, 0.6) * 1.35;
}

/**
 * Owns one real WebGL renderer, camera, lights, OrbitControls and picking loop.
 *
 * `ThreeDesignSceneBuilder` remains the deterministic model projection, while
 * this adapter manages browser resources and interaction. Rendering is
 * event-driven rather than continuous: document, camera, resize and selection
 * changes request one animation frame, reducing idle CPU/GPU use and preparing
 * the same adapter for mobile quality policies.
 *
 * @example `const view = new ThreeDesignViewport(host, { onSelect })`.
 * @since 0.4.0
 * @modified 2026-09-17 - Added the first interactive Three.js viewport.
 */
export class ThreeDesignViewport {
  readonly #container: HTMLElement;
  readonly #options: ThreeDesignViewportOptions;
  readonly #scene = new Scene();
  readonly #camera = new PerspectiveCamera(38, 1, 0.01, 100);
  readonly #renderer: WebGLRenderer;
  readonly #controls: OrbitControls;
  readonly #raycaster = new Raycaster();
  readonly #pointer = new Vector2();
  readonly #builder = new ThreeDesignSceneBuilder();
  readonly #resizeObserver?: ResizeObserver;
  #designRoot = new Group();
  #groundOrientationGuide = createThreeGroundOrientationGuide();
  #selectionHelper?: Box3Helper;
  #selectionOutlineVisible = true;
  #selectedObjectId?: string;
  #renderFrame?: number;
  #pointerDown?: Readonly<{ x: number; y: number }>;
  #disposed = false;

  /**
   * Creates browser resources and attaches one accessible canvas to the host.
   *
   * @param container Element whose content box defines renderer dimensions.
   * @param options Selection and quality callbacks.
   * @example The desktop workspace constructs one viewport and disposes it on unmount.
   * @since 0.4.0
   * @modified 2026-09-17 - Added WebGL/camera/control initialization.
   */
  constructor(container: HTMLElement, options: ThreeDesignViewportOptions = {}) {
    this.#container = container;
    this.#options = options;
    this.#scene.background = new Color(0xf2f5f9);
    this.#renderer = new WebGLRenderer({ antialias: true, alpha: false });
    this.#renderer.outputColorSpace = SRGBColorSpace;
    this.#renderer.setPixelRatio(
      Math.min(window.devicePixelRatio || 1, options.pixelRatioLimit ?? 2)
    );
    this.#renderer.domElement.className = "three-design-viewport__canvas";
    this.#renderer.domElement.setAttribute("aria-label", "DoorMes 3D design viewport");
    this.#renderer.domElement.setAttribute("role", "img");
    this.#container.replaceChildren(this.#renderer.domElement);

    this.#camera.position.set(2.4, 1.7, 3.2);
    this.#controls = new OrbitControls(this.#camera, this.#renderer.domElement);
    this.#controls.enableDamping = false;
    this.#controls.minDistance = 0.25;
    this.#controls.maxDistance = 30;
    this.#controls.addEventListener("change", this.#requestRender);

    const ambient = new AmbientLight(0xffffff, 1.6);
    const keyLight = new DirectionalLight(0xffffff, 2.4);
    keyLight.position.set(3, 4, 5);
    const fillLight = new DirectionalLight(0xbfd8ff, 1.1);
    fillLight.position.set(-4, 1, 3);
    const floorGrid = new GridHelper(12, 24, 0xcbd5e1, 0xe2e8f0);
    floorGrid.position.y = -0.9;
    this.#groundOrientationGuide.position.y = -0.895;
    this.#scene.add(
      ambient,
      keyLight,
      fillLight,
      floorGrid,
      this.#groundOrientationGuide,
      this.#designRoot
    );

    this.#renderer.domElement.addEventListener("pointerdown", this.#onPointerDown);
    this.#renderer.domElement.addEventListener("pointerup", this.#onPointerUp);
    if (typeof ResizeObserver !== "undefined") {
      this.#resizeObserver = new ResizeObserver(() => this.resize());
      this.#resizeObserver.observe(this.#container);
    } else {
      window.addEventListener("resize", this.#onWindowResize);
    }
    this.resize();
  }

  /**
   * Rebuilds the scene subtree from one immutable domain snapshot.
   *
   * The previous subtree is removed and all geometries/materials are disposed
   * before replacement. Camera framing then uses the new world bounds, while a
   * matching selected ID restores the cross-view selection helper.
   *
   * @param document Current design document.
   * @param options Runtime-only panel pose plus angle/ruler overlay visibility.
   * @param resetCamera Whether a design change should reframe the scene; preview
   * frames pass false so user OrbitControls orientation remains stable.
   * @example Calling after resize-command revision 2 displays new dimensions.
   * @since 0.4.0
   * @modified 2026-09-17 - Added camera-stable opening and engineering-overlay projection.
   */
  update(
    document: DesignDocument,
    options: ThreeDesignSceneBuildOptions = {},
    resetCamera = true
  ): void {
    const previous = this.#designRoot;
    this.#scene.remove(previous);
    disposeObjectTree(previous);
    this.#designRoot = this.#builder.build(document, options);
    this.#scene.remove(this.#groundOrientationGuide);
    disposeObjectTree(this.#groundOrientationGuide);
    const designBounds = new Box3().setFromObject(this.#designRoot);
    const designCenter = new Vector3();
    const designSize = new Vector3();
    if (!designBounds.isEmpty()) {
      designBounds.getCenter(designCenter);
      designBounds.getSize(designSize);
    }
    this.#groundOrientationGuide = createThreeGroundOrientationGuide(
      Math.max(0.75, designSize.z / 2 + 0.55)
    );
    this.#groundOrientationGuide.position.set(designCenter.x, -0.895, designCenter.z);
    this.#scene.add(this.#groundOrientationGuide);
    this.#scene.add(this.#designRoot);
    if (resetCamera) this.resetCamera();
    this.setSelectedObjectId(this.#selectedObjectId);
    this.#requestRender();
  }

  /**
   * Fits the complete design in view and resets the OrbitControls target.
   *
   * @example The toolbar reset button calls this after a user rotates the view.
   * @since 0.4.0
   * @modified 2026-09-17 - Added deterministic user-visible camera reset.
   */
  resetCamera(): void {
    const bounds = new Box3().setFromObject(this.#designRoot);
    const center = new Vector3(0, 0, 0);
    const size = new Vector3(1, 1, 0.1);
    if (!bounds.isEmpty()) {
      bounds.getCenter(center);
      bounds.getSize(size);
    }
    const distance = calculateCameraFitDistance(size, this.#camera.fov, this.#camera.aspect);
    this.#camera.position.set(
      center.x + distance * 0.62,
      center.y + distance * 0.34,
      center.z + distance
    );
    this.#camera.near = Math.max(0.01, distance / 100);
    this.#camera.far = Math.max(100, distance * 20);
    this.#camera.updateProjectionMatrix();
    this.#controls.target.copy(center);
    this.#controls.update();
    this.#requestRender();
  }

  /**
   * Updates canvas dimensions from its host without changing domain geometry.
   *
   * @example Called after switching from the hidden 2D tab to the 3D tab.
   * @since 0.4.0
   * @modified 2026-09-17 - Added responsive renderer sizing.
   */
  resize(): void {
    const width = Math.max(1, this.#container.clientWidth);
    const height = Math.max(1, this.#container.clientHeight);
    this.#camera.aspect = width / height;
    this.#camera.updateProjectionMatrix();
    this.#renderer.setSize(width, height, false);
    this.#requestRender();
  }

  /**
   * Draws a non-destructive box helper around the matching stable object ID.
   *
   * A helper is used instead of mutating shared materials, so highlighting one
   * frame segment cannot accidentally recolour every mesh using that material.
   *
   * @param objectId Selected domain/display ID, or undefined to clear.
   * @example Selecting `M-PREVIEW-H` outlines the same mullion picked in SVG.
   * @since 0.4.0
   * @modified 2026-09-17 - Added cross-view 3D highlighting.
   */
  setSelectedObjectId(objectId: string | undefined): void {
    this.#selectedObjectId = objectId;
    if (this.#selectionHelper) {
      this.#scene.remove(this.#selectionHelper);
      this.#selectionHelper.geometry.dispose();
      const helperMaterials = Array.isArray(this.#selectionHelper.material)
        ? this.#selectionHelper.material
        : [this.#selectionHelper.material];
      helperMaterials.forEach((material) => material.dispose());
      this.#selectionHelper = undefined;
    }
    if (!objectId || !this.#selectionOutlineVisible) {
      this.#requestRender();
      return;
    }
    const bounds = calculateStableSelectionBounds(this.#designRoot, objectId);
    if (bounds) {
      this.#selectionHelper = new Box3Helper(bounds, 0xff8a00);
      this.#scene.add(this.#selectionHelper);
    }
    this.#requestRender();
  }

  /**
   * Shows or removes the orange selected-object bounding helper independently.
   *
   * The stable selected object ID remains untouched, so hiding this display aid
   * does not clear the object tree selection, property panel or editing target.
   * Re-enabling it rebuilds the helper from the latest scene subtree, which also
   * keeps the box correct after an opening-animation scene replacement.
   *
   * @param visible Whether the Three.js `BoxHelper` should be present.
   * @example `viewport.setSelectionOutlineVisible(false)` hides only the orange box.
   * @since 0.9.5
   * @modified 2026-09-17 - Added user-controlled 3D selection-helper visibility.
   */
  setSelectionOutlineVisible(visible: boolean): void {
    if (this.#selectionOutlineVisible === visible) return;
    this.#selectionOutlineVisible = visible;
    this.setSelectedObjectId(this.#selectedObjectId);
  }

  /**
   * Releases listeners, WebGL resources and all scene allocations.
   *
   * @example Layout-shell replacement calls `dispose()` before removing its DOM.
   * @since 0.4.0
   * @modified 2026-09-17 - Added explicit GPU and observer cleanup.
   */
  dispose(): void {
    if (this.#disposed) return;
    this.#disposed = true;
    if (this.#renderFrame !== undefined) cancelAnimationFrame(this.#renderFrame);
    this.#resizeObserver?.disconnect();
    window.removeEventListener("resize", this.#onWindowResize);
    this.#renderer.domElement.removeEventListener("pointerdown", this.#onPointerDown);
    this.#renderer.domElement.removeEventListener("pointerup", this.#onPointerUp);
    this.#controls.removeEventListener("change", this.#requestRender);
    this.#controls.dispose();
    disposeObjectTree(this.#scene);
    this.#renderer.dispose();
    this.#renderer.domElement.remove();
  }

  /** Coalesces multiple state changes into one browser render frame. */
  readonly #requestRender = (): void => {
    if (this.#disposed || this.#renderFrame !== undefined) return;
    this.#renderFrame = requestAnimationFrame(() => {
      this.#renderFrame = undefined;
      if (!this.#disposed) this.#renderer.render(this.#scene, this.#camera);
    });
  };

  /** Stores a click origin so OrbitControls drags are not mistaken for picks. */
  readonly #onPointerDown = (event: PointerEvent): void => {
    this.#pointerDown = { x: event.clientX, y: event.clientY };
  };

  /** Raycasts short pointer taps and returns the nearest stable object ID. */
  readonly #onPointerUp = (event: PointerEvent): void => {
    const start = this.#pointerDown;
    this.#pointerDown = undefined;
    if (!start || Math.hypot(event.clientX - start.x, event.clientY - start.y) > 5) return;
    const bounds = this.#renderer.domElement.getBoundingClientRect();
    if (bounds.width <= 0 || bounds.height <= 0) return;
    this.#pointer.set(
      ((event.clientX - bounds.left) / bounds.width) * 2 - 1,
      -((event.clientY - bounds.top) / bounds.height) * 2 + 1
    );
    this.#raycaster.setFromCamera(this.#pointer, this.#camera);
    const hit = this.#raycaster
      .intersectObject(this.#designRoot, true)
      .find((intersection) => typeof intersection.object.userData.objectId === "string");
    this.#options.onSelect?.(hit?.object.userData.objectId as string | undefined);
  };

  /** Fallback resize listener for browsers without ResizeObserver. */
  readonly #onWindowResize = (): void => this.resize();
}

/** Locates a scene node by the stable application ID copied into userData. */
function findObjectByStableId(root: Object3D, objectId: string): Object3D | undefined {
  let result: Object3D | undefined;
  root.traverse((candidate) => {
    if (!result && candidate.userData.objectId === objectId) result = candidate;
  });
  return result;
}

/**
 * Calculates a selection box without leaking installation hosts into products.
 *
 * A window scene root owns its frame, sashes and also non-product wall/package
 * previews. `BoxHelper(windowRoot)` therefore outlined the entire wall opening
 * and made a window selection look like a wall selection. This resolver walks
 * rendered mesh leaves, skips explicitly separated installation subtrees only
 * for a window-root selection, and keeps those same subtrees measurable when
 * their own wall/package IDs are selected.
 *
 * @param root Current design scene subtree.
 * @param objectId Stable selection identity.
 * @returns Detached world-space bounds, or undefined when no mesh resolves.
 * @example Selecting `WIN-1` excludes `WIN-1:installation.wall` from the box.
 * @since 0.10.40
 * @modified 2026-09-20 - Corrected product versus installation selection bounds.
 */
export function calculateStableSelectionBounds(
  root: Object3D,
  objectId: string
): Box3 | undefined {
  const target = findObjectByStableId(root, objectId);
  if (!target) return undefined;
  root.updateMatrixWorld(true);
  const bounds = new Box3();
  let foundMesh = false;
  const excludesInstallation = target.userData.objectType === "window";
  const visit = (candidate: Object3D, isTarget: boolean): void => {
    if (
      !isTarget &&
      excludesInstallation &&
      candidate.userData.excludeFromWindowSelectionBounds === true
    ) return;
    if (candidate instanceof Mesh) {
      bounds.expandByObject(candidate, true);
      foundMesh = true;
    }
    for (const child of candidate.children) visit(child, false);
  };
  visit(target, true);
  return foundMesh && !bounds.isEmpty() ? bounds : undefined;
}

/**
 * Disposes unique mesh resources below a removed projection subtree.
 *
 * Materials can be shared by many frame meshes, so Sets guarantee each GPU
 * allocation is released once even when the tree contains repeated references.
 */
function disposeObjectTree(root: Object3D): void {
  const geometries = new Set<BufferGeometry>();
  const materials = new Set<Material>();
  const textures = new Set<{ dispose(): void }>();
  root.traverse((candidate) => {
    const renderable = candidate as Object3D & {
      geometry?: BufferGeometry;
      material?: Material | Material[];
    };
    if (renderable.geometry instanceof BufferGeometry) geometries.add(renderable.geometry);
    const materialList = renderable.material
      ? Array.isArray(renderable.material)
        ? renderable.material
        : [renderable.material]
      : [];
    for (const material of materialList) {
      materials.add(material);
      for (const value of Object.values(material)) {
        if (value instanceof Texture) textures.add(value);
      }
    }
  });
  geometries.forEach((geometry) => geometry.dispose());
  textures.forEach((texture) => texture.dispose());
  materials.forEach((material) => material.dispose());
}
