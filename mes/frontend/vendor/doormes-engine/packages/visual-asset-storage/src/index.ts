import {
  publishComponentAssetCatalogVersion,
  type ComponentAssetCatalogActor,
  type ComponentAssetCatalogDraft,
  type PublishedComponentAssetCatalogVersion
} from "@doormes/appearance-model";
import { portableSha256 } from './sha256.js';

const SHA256_PATTERN = /^sha256:[0-9a-f]{64}$/;
const MAX_VISUAL_ASSET_BYTES = 50 * 1024 * 1024;
const MAX_GLTF_BYTES = 25 * 1024 * 1024;
const MAX_GLTF_NODES = 512;
const MAX_GLTF_MESHES = 256;
const MAX_GLTF_PRIMITIVES = 2_048;
const MAX_GLTF_TRIANGLES = 500_000;
const MAX_GLTF_MATERIALS = 128;
const MAX_GLTF_IMAGES = 64;
const MAX_UPLOAD_GRANT_TTL_MS = 15 * 60 * 1000;

/**
 * Auditable evidence produced before GLB/glTF bytes enter storage or Three.js.
 *
 * Counts are read from the hash-covered JSON document. They can be attached to
 * an upload review without retaining a renderer scene, object URL or mutable
 * browser file handle.
 *
 * @since 0.10.10
 * @modified 2026-09-18 - Moved pure upload inspection out of renderer-three.
 */
export interface ManagedGltfInspection {
  readonly contentHash: string;
  readonly byteLength: number;
  readonly nodeCount: number;
  readonly meshCount: number;
  readonly primitiveCount: number;
  readonly estimatedTriangleCount: number;
  readonly materialCount: number;
  readonly imageCount: number;
}

/** Minimal JSON view needed for bounded and renderer-free glTF inspection. */
interface InspectableGltfJson {
  readonly nodes?: readonly unknown[];
  readonly meshes?: readonly Readonly<{
    readonly primitives?: readonly Readonly<{
      readonly indices?: number;
      readonly mode?: number;
      readonly attributes?: Readonly<Record<string, number>>;
    }>[];
  }>[];
  readonly accessors?: readonly Readonly<{ readonly count?: number }>[];
  readonly materials?: readonly unknown[];
  readonly images?: readonly Readonly<{ readonly uri?: string }>[];
  readonly buffers?: readonly Readonly<{ readonly uri?: string }>[];
}

/** Managed byte category; neither value is a remote URL or renderer object. */
export type VisualAssetBlobKind = "component-model" | "texture-bundle";

/** Immutable metadata returned after exact bytes pass the storage boundary. */
export interface VisualAssetBlobDescriptor {
  readonly assetId: string;
  readonly kind: VisualAssetBlobKind;
  readonly mediaType: string;
  readonly contentHash: string;
  readonly byteLength: number;
  readonly storedAtIso: string;
}

/** Detached bytes plus their verified immutable descriptor. */
export interface StoredVisualAssetBlob {
  readonly descriptor: VisualAssetBlobDescriptor;
  readonly bytes: ArrayBuffer;
}

/** Evidence that one model upload passed preflight and immutable byte storage. */
export interface ManagedGltfUploadEvidence {
  readonly asset: VisualAssetBlobDescriptor;
  readonly inspection: ManagedGltfInspection;
  readonly inspectedAtIso: string;
}

/** One backend-issued capability that authorizes an exact immutable upload. */
export interface ManagedGltfUploadGrant {
  readonly schemaVersion: "doormes-managed-gltf-upload-grant.v1";
  readonly grantId: string;
  readonly actorId: string;
  readonly assetId: string;
  readonly expectedContentHash: string;
  readonly mediaType: "model/gltf-binary" | "model/gltf+json";
  readonly byteLength: number;
  readonly issuedAtIso: string;
  readonly expiresAtIso: string;
}

/** Server-side registry resolving opaque grant IDs; clients never submit grant fields. */
export interface ManagedGltfUploadGrantRegistry {
  register(grant: ManagedGltfUploadGrant): Promise<ManagedGltfUploadGrant>;
  resolve(grantId: string): Promise<ManagedGltfUploadGrant | undefined>;
}

/**
 * Renderer/UI-neutral gateway consumed by the shared PC/mobile import editor.
 *
 * The local implementation writes IndexedDB directly. A production HTTP
 * implementation can request an opaque upload grant and call the authorized
 * backend endpoint without changing the editor, domain command or renderer.
 *
 * @since 0.10.12
 * @modified 2026-09-18 - Added the backend-ready upload/availability seam.
 */
export interface ManagedGltfAssetGateway {
  upload(input: Readonly<{
    assetId: string;
    expectedContentHash: string;
    mediaType: "model/gltf-binary" | "model/gltf+json";
    bytes: ArrayBuffer;
    inspectedAtIso: string;
  }>): Promise<ManagedGltfUploadEvidence>;
  describe(assetId: string): Promise<VisualAssetBlobDescriptor | undefined>;
  /**
   * Reads exact managed bytes only when both the stable ID and frozen hash match.
   *
   * Renderers use this after opening/reloading a design; callers never receive a
   * mutable filesystem path or remote URL. Implementations must return detached
   * bytes and must not silently substitute the latest content for a stale hash.
   *
   * @since 0.10.16
   * @modified 2026-09-18 - Added persisted model reload to the editor gateway.
   */
  read(assetId: string, expectedContentHash: string): Promise<StoredVisualAssetBlob | undefined>;
}

/** Optimistically versioned catalog state returned by the publication store. */
export interface ComponentAssetCatalogState {
  readonly catalogItemId: string;
  readonly revision: number;
  readonly history: readonly PublishedComponentAssetCatalogVersion[];
}

/**
 * Atomic persistence boundary for one reviewed catalog publication.
 *
 * A backend implementation must compare `expectedRevision`, verify and retain
 * every referenced byte descriptor, append the immutable catalog version and
 * increment revision in one database transaction. Object bytes are uploaded
 * before this call and never overwritten, so the transaction stores only their
 * exact ID/hash references. This contract prevents two approvers from both
 * publishing version N from the same stale screen.
 *
 * @since 0.10.12
 * @modified 2026-09-18 - Added review publication transaction/CAS boundary.
 */
export interface ComponentAssetCatalogPublicationStore {
  read(catalogItemId: string): Promise<ComponentAssetCatalogState>;
  commit(input: Readonly<{
    expectedRevision: number;
    version: PublishedComponentAssetCatalogVersion;
  }>): Promise<ComponentAssetCatalogState>;
}

/** Exact asset identity retained by one historical catalog/order owner. */
export interface VisualAssetRetentionReference {
  readonly assetId: string;
  readonly contentHash: string;
}

/** Append-only owner manifest used by garbage collection and audit. */
export interface VisualAssetRetentionManifest {
  readonly ownerId: string;
  readonly retainedAtIso: string;
  readonly assets: readonly VisualAssetRetentionReference[];
}

/** One missing or replaced byte object found while checking a retention owner. */
export interface VisualAssetRetentionIssue {
  readonly ownerId: string;
  readonly assetId: string;
  readonly expectedContentHash: string;
  readonly code: "ASSET_BYTES_MISSING" | "ASSET_BYTES_HASH_MISMATCH";
}

/**
 * Storage port for hash-addressed visual bytes and immutable retention owners.
 *
 * Production implementations may use IndexedDB, object storage or a backend
 * blob service. Every method remains renderer-free and exchanges copied raw
 * bytes, exact hashes and stable IDs only. `retain` must validate every asset
 * before writing its owner manifest so a partial historical version is never
 * observable.
 *
 * @example Upload all LOD blobs, then retain `component:HANDLE-42:version:3`.
 * @since 0.10.9
 * @modified 2026-09-18 - Added ASSET-003 byte-retention abstraction.
 */
export interface VisualAssetBlobStore {
  put(input: Readonly<{
    assetId: string;
    kind: VisualAssetBlobKind;
    mediaType: string;
    expectedContentHash: string;
    bytes: ArrayBuffer;
    storedAtIso: string;
  }>): Promise<VisualAssetBlobDescriptor>;
  read(assetId: string, expectedContentHash: string): Promise<StoredVisualAssetBlob | undefined>;
  describe(assetId: string): Promise<VisualAssetBlobDescriptor | undefined>;
  retain(manifest: VisualAssetRetentionManifest): Promise<VisualAssetRetentionManifest>;
  readRetention(ownerId: string): Promise<VisualAssetRetentionManifest | undefined>;
  auditRetention(ownerId: string): Promise<readonly VisualAssetRetentionIssue[]>;
}

/** Validates and canonicalizes one public storage identifier. */
function normalizeText(value: string, field: string): string {
  const normalized = value.trim();
  if (!normalized) throw new Error(`${field} must not be empty.`);
  return normalized;
}

/** Validates the content-addressed identity shared with domain snapshots. */
function normalizeHash(value: string, field: string): string {
  const normalized = normalizeText(value, field).toLowerCase();
  if (!SHA256_PATTERN.test(normalized)) {
    throw new Error(`${field} must be a lowercase sha256:<64 hex> identity.`);
  }
  return normalized;
}

/** Deep-copies one upload grant so registry callers cannot mutate authority. */
function cloneUploadGrant(value: ManagedGltfUploadGrant): ManagedGltfUploadGrant {
  return { ...value };
}

/**
 * Issues one short-lived capability for an exact actor, ID, hash, MIME and size.
 *
 * Algorithm: authorize an asset designer/approver/administrator, canonicalize
 * every immutable identity field, enforce the same 25MB gate as preflight, and
 * limit the lifetime to fifteen minutes. The API persists the returned value in
 * a server-side registry and sends only `grantId` to the client; therefore a
 * caller cannot increase size, change hash or impersonate another actor.
 *
 * @param input Authenticated actor plus server-validated upload declaration.
 * @returns Canonical capability ready for a grant registry.
 * @throws On role, identity, size, MIME or lifetime violations.
 * @example A designer receives a five-minute grant for one 3.6MB GLB hash.
 * @since 0.10.12
 * @modified 2026-09-18 - Added backend upload authorization policy.
 */
export function issueManagedGltfUploadGrant(input: Readonly<{
  grantId: string;
  actor: ComponentAssetCatalogActor;
  assetId: string;
  expectedContentHash: string;
  mediaType: "model/gltf-binary" | "model/gltf+json";
  byteLength: number;
  issuedAtIso: string;
  expiresAtIso: string;
}>): ManagedGltfUploadGrant {
  const actorId = normalizeText(input.actor.actorId, "actor.actorId");
  if (!input.actor.roles.some((role) =>
    role === "asset-designer" || role === "catalog-approver" || role === "administrator")) {
    throw new Error("Managed model upload requires an asset catalog role.");
  }
  if (
    !Number.isSafeInteger(input.byteLength) ||
    input.byteLength < 2 ||
    input.byteLength > MAX_GLTF_BYTES
  ) {
    throw new RangeError(`Managed model upload must contain 2..${MAX_GLTF_BYTES} bytes.`);
  }
  if (input.mediaType !== "model/gltf-binary" && input.mediaType !== "model/gltf+json") {
    throw new Error("Managed model upload MIME type is not supported.");
  }
  const issuedAt = new Date(input.issuedAtIso);
  const expiresAt = new Date(input.expiresAtIso);
  const lifetimeMs = expiresAt.valueOf() - issuedAt.valueOf();
  if (
    Number.isNaN(issuedAt.valueOf()) ||
    Number.isNaN(expiresAt.valueOf()) ||
    lifetimeMs <= 0 ||
    lifetimeMs > MAX_UPLOAD_GRANT_TTL_MS
  ) {
    throw new Error("Managed model upload grant lifetime must be 1..15 minutes.");
  }
  return {
    schemaVersion: "doormes-managed-gltf-upload-grant.v1",
    grantId: normalizeText(input.grantId, "grantId"),
    actorId,
    assetId: normalizeText(input.assetId, "assetId"),
    expectedContentHash: normalizeHash(input.expectedContentHash, "expectedContentHash"),
    mediaType: input.mediaType,
    byteLength: input.byteLength,
    issuedAtIso: issuedAt.toISOString(),
    expiresAtIso: expiresAt.toISOString()
  };
}

/**
 * Implements the authorization endpoint's framework-neutral application core.
 *
 * The HTTP adapter must derive `actor` from its authenticated request context
 * and generate an unpredictable `grantId`; neither value is accepted from JSON.
 * This function validates the declaration, registers immutable server state and
 * returns only the opaque ID/expiry that the browser needs for its PUT request.
 *
 * @since 0.10.13
 * @modified 2026-09-18 - Added concrete server authorization orchestration.
 */
export async function registerManagedGltfUploadAuthorization(
  registry: ManagedGltfUploadGrantRegistry,
  input: Readonly<{
    grantId: string;
    actor: ComponentAssetCatalogActor;
    assetId: string;
    expectedContentHash: string;
    mediaType: "model/gltf-binary" | "model/gltf+json";
    byteLength: number;
    issuedAtIso: string;
    expiresAtIso: string;
  }>
): Promise<ManagedGltfUploadAuthorizationResponse> {
  const grant = await registry.register(issueManagedGltfUploadGrant(input));
  return { grantId: grant.grantId, expiresAtIso: grant.expiresAtIso };
}

/**
 * Calculates the canonical SHA-256 identity of an exact visual-asset payload.
 *
 * The helper uses Web Crypto in browsers and Node 22, allowing the file picker,
 * upload API, persistence adapter and renderer to display/verify the same hash.
 *
 * @param bytes Detached exact file bytes.
 * @returns Lowercase `sha256:<64 hex>` identity.
 * @example A selected GLB is hashed before its managed asset ID is generated.
 * @since 0.10.11
 * @modified 2026-09-18 - Exposed shared client/server upload hashing.
 */
export async function calculateVisualAssetContentHash(bytes: ArrayBuffer): Promise<string> {
  if (!globalThis.crypto?.subtle) return portableSha256(bytes);
  const digest = await globalThis.crypto.subtle.digest("SHA-256", bytes);
  return `sha256:${[...new Uint8Array(digest)]
    .map((value) => value.toString(16).padStart(2, "0"))
    .join("")}`;
}

/**
 * Extracts the first JSON document from JSON glTF or a complete GLB v2 file.
 *
 * @param bytes Candidate bytes already bounded by the public preflight gate.
 * @returns Parsed structural view for URI and complexity checks.
 * @throws On invalid GLB header/chunk lengths or a non-object JSON root.
 * @example A GLB must start with its JSON chunk before embedded binary data.
 * @since 0.10.10
 * @modified 2026-09-18 - Centralized format parsing in asset infrastructure.
 */
function parseInspectableGltf(bytes: ArrayBuffer): InspectableGltfJson {
  const view = new DataView(bytes);
  const octets = new Uint8Array(bytes);
  let jsonBytes: Uint8Array;
  if (bytes.byteLength >= 12 && view.getUint32(0, true) === 0x46546c67) {
    const version = view.getUint32(4, true);
    const declaredLength = view.getUint32(8, true);
    if (version !== 2 || declaredLength !== bytes.byteLength || bytes.byteLength < 20) {
      throw new Error("Managed GLB must be a complete version-2 binary container.");
    }
    const chunkLength = view.getUint32(12, true);
    const chunkType = view.getUint32(16, true);
    if (chunkType !== 0x4e4f534a || chunkLength < 2 || chunkLength%4!==0 || 20 + chunkLength > bytes.byteLength) {
      throw new Error("Managed GLB must begin with one valid JSON chunk.");
    }
    jsonBytes = octets.slice(20, 20 + chunkLength);
    let offset=20+chunkLength;
    while(offset<bytes.byteLength){
      if(offset+8>bytes.byteLength)throw Error('Truncated GLB chunk.');
      const length=view.getUint32(offset,true),type=view.getUint32(offset+4,true);
      if(length%4!==0||offset+8+length>bytes.byteLength||type===0x4e4f534a)throw Error('Invalid GLB chunk.');
      offset+=8+length;
    }
  } else {
    jsonBytes = octets;
  }
  const jsonText = new TextDecoder().decode(jsonBytes).replace(/\u0000+$/g, "").trim();
  const parsed = JSON.parse(jsonText) as unknown;
  if (!parsed || typeof parsed !== "object" || Array.isArray(parsed)) {
    throw new Error("Managed glTF JSON root must be an object.");
  }
  if ((parsed as { asset?: { version?: string } }).asset?.version !== '2.0') {
    throw new Error('Managed glTF must declare asset.version 2.0.');
  }
  // No URI anywhere may escape the hash-covered payload, including extension objects.
  const visit = (value: unknown, depth = 0): void => {
    if(depth>64) throw new Error('Managed glTF nesting exceeds the runtime budget.');
    if(value && typeof value==='object')for(const [key,child] of Object.entries(value)){
      if(key==='uri' && (typeof child!=='string'||!child.toLowerCase().startsWith('data:')))throw new Error('Managed glTF external URI is not allowed.');
      visit(child,depth+1);
    }
  };
  visit(parsed);
  return parsed as InspectableGltfJson;
}

/**
 * Performs the renderer-free security and complexity preflight for GLB/glTF.
 *
 * Algorithm: enforce the 25MB budget, calculate and compare SHA-256, parse the
 * embedded JSON, reject all non-data external buffer/image URIs, count scene
 * structures, and estimate triangles from index/POSITION accessors. No Three.js
 * object is allocated, so API and mobile upload paths can run the same gate.
 *
 * @param input Exact upload bytes plus their expected immutable identity.
 * @returns Review evidence safe to retain with an upload draft.
 * @throws On hash, format, external-reference or complexity violations.
 * @example A 20k-triangle embedded GLB returns counts before byte persistence.
 * @since 0.10.10
 * @modified 2026-09-18 - Extracted ASSET-003 preflight from the renderer.
 */
export async function inspectManagedGltfAsset(input: Readonly<{
  bytes: ArrayBuffer;
  expectedContentHash: string;
}>): Promise<ManagedGltfInspection> {
  if (input.bytes.byteLength < 2 || input.bytes.byteLength > MAX_GLTF_BYTES) {
    throw new RangeError(`Managed GLB/glTF must contain 2..${MAX_GLTF_BYTES} bytes.`);
  }
  const expectedContentHash = normalizeHash(input.expectedContentHash, "expectedContentHash");
  const contentHash = await calculateVisualAssetContentHash(input.bytes);
  if (contentHash !== expectedContentHash) {
    throw new Error("Managed GLB/glTF content hash does not match its snapshot.");
  }
  const document = parseInspectableGltf(input.bytes);
  const nodes = document.nodes ?? [];
  const meshes = document.meshes ?? [];
  const materials = document.materials ?? [];
  const images = document.images ?? [];
  const buffers = document.buffers ?? [];
  const externalUris = [...buffers, ...images]
    .flatMap((entry) => entry.uri ? [entry.uri.trim()] : [])
    .filter((uri) => !uri.toLowerCase().startsWith("data:"));
  if (externalUris.length > 0) {
    throw new Error("Managed GLB/glTF must not reference external buffer or image URIs.");
  }
  const primitives = meshes.flatMap((mesh) => mesh.primitives ?? []);
  const accessors = document.accessors ?? [];
  for(const accessor of accessors)if(!Number.isSafeInteger(accessor.count)||Number(accessor.count)<0)throw Error('Invalid glTF accessor count.');
  const estimatedTriangleCount = primitives.reduce((total, primitive) => {
    const accessorIndex = primitive.indices ?? primitive.attributes?.POSITION;
    const vertexCount = accessorIndex === undefined
      ? 0
      : Number(accessors[accessorIndex]?.count ?? 0);
    if(accessorIndex!==undefined&&(!Number.isSafeInteger(accessorIndex)||accessorIndex<0||accessorIndex>=accessors.length))throw Error('Invalid glTF primitive accessor.');
    const mode = primitive.mode ?? 4;
    const triangles = mode === 4
      ? Math.floor(vertexCount / 3)
      : mode === 5 || mode === 6
        ? Math.max(0, vertexCount - 2)
        : 0;
    return total + triangles;
  }, 0);
  if (
    nodes.length > MAX_GLTF_NODES ||
    meshes.length > MAX_GLTF_MESHES ||
    primitives.length > MAX_GLTF_PRIMITIVES ||
    estimatedTriangleCount > MAX_GLTF_TRIANGLES ||
    materials.length > MAX_GLTF_MATERIALS ||
    images.length > MAX_GLTF_IMAGES
  ) {
    throw new RangeError("Managed GLB/glTF exceeds the reviewed runtime complexity budget.");
  }
  return {
    contentHash,
    byteLength: input.bytes.byteLength,
    nodeCount: nodes.length,
    meshCount: meshes.length,
    primitiveCount: primitives.length,
    estimatedTriangleCount,
    materialCount: materials.length,
    imageCount: images.length
  };
}

/** Copies a descriptor so callers cannot mutate adapter-owned records. */
function cloneDescriptor(value: VisualAssetBlobDescriptor): VisualAssetBlobDescriptor {
  return {
    assetId: value.assetId,
    kind: value.kind,
    mediaType: value.mediaType,
    contentHash: value.contentHash,
    byteLength: value.byteLength,
    storedAtIso: value.storedAtIso
  };
}

/** Copies and canonicalizes a retention manifest in deterministic asset order. */
function normalizeManifest(value: VisualAssetRetentionManifest): VisualAssetRetentionManifest {
  const ownerId = normalizeText(value.ownerId, "manifest.ownerId");
  const retainedAt = new Date(value.retainedAtIso);
  if (Number.isNaN(retainedAt.valueOf())) {
    throw new Error("manifest.retainedAtIso must be a valid date-time value.");
  }
  const keys = new Set<string>();
  const assets = value.assets.map((asset, index) => {
    const assetId = normalizeText(asset.assetId, `manifest.assets[${index}].assetId`);
    const contentHash = normalizeHash(
      asset.contentHash,
      `manifest.assets[${index}].contentHash`
    );
    const key = `${assetId}|${contentHash}`;
    if (keys.has(key)) throw new Error(`Retention manifest contains duplicate asset ${assetId}.`);
    keys.add(key);
    return { assetId, contentHash };
  }).sort((left, right) =>
    `${left.assetId}|${left.contentHash}`.localeCompare(`${right.assetId}|${right.contentHash}`));
  return { ownerId, retainedAtIso: retainedAt.toISOString(), assets };
}

/**
 * In-memory reference adapter for tests, local prototypes and port conformance.
 *
 * Algorithm: hash a detached byte copy, reject mismatches and same-ID rebinding,
 * then store one immutable record. Retention first audits every requested blob
 * and only afterwards commits an owner manifest. Existing owner IDs are
 * idempotent for an identical asset set and immutable for a different set.
 *
 * This adapter intentionally does not claim durable browser persistence; the
 * IndexedDB/backend implementations must satisfy this exact port and conformance
 * suite before production use.
 *
 * @since 0.10.9
 * @modified 2026-09-18 - Added executable asset-storage contract reference.
 */
export class InMemoryVisualAssetBlobStore implements VisualAssetBlobStore {
  readonly #blobs = new Map<string, StoredVisualAssetBlob>();
  readonly #retentions = new Map<string, VisualAssetRetentionManifest>();

  /**
   * Hashes and stores one detached byte object under a stable managed ID.
   *
   * @param input Bytes plus caller-declared identity and audit timestamp.
   * @returns Verified immutable descriptor; identical retry is idempotent.
   * @throws On size/hash failure, malformed metadata or same-ID rebinding.
   * @example Store a reviewed high-detail GLB before catalog publication.
   * @since 0.10.9
   * @modified 2026-09-18 - Added reference byte insertion behavior.
   */
  async put(input: Readonly<{
    assetId: string;
    kind: VisualAssetBlobKind;
    mediaType: string;
    expectedContentHash: string;
    bytes: ArrayBuffer;
    storedAtIso: string;
  }>): Promise<VisualAssetBlobDescriptor> {
    const assetId = normalizeText(input.assetId, "asset.assetId");
    const mediaType = normalizeText(input.mediaType, "asset.mediaType");
    const expectedContentHash = normalizeHash(input.expectedContentHash, "asset.contentHash");
    if (input.bytes.byteLength < 1 || input.bytes.byteLength > MAX_VISUAL_ASSET_BYTES) {
      throw new RangeError(`Visual asset bytes must contain 1..${MAX_VISUAL_ASSET_BYTES} bytes.`);
    }
    const storedAt = new Date(input.storedAtIso);
    if (Number.isNaN(storedAt.valueOf())) {
      throw new Error("asset.storedAtIso must be a valid date-time value.");
    }
    const bytes = input.bytes.slice(0);
    const actualContentHash = await calculateVisualAssetContentHash(bytes);
    if (actualContentHash !== expectedContentHash) {
      throw new Error(`Visual asset ${assetId} bytes do not match the expected content hash.`);
    }
    const existing = this.#blobs.get(assetId);
    if (existing && existing.descriptor.contentHash !== expectedContentHash) {
      throw new Error(`Visual asset ${assetId} cannot be rebound to different content.`);
    }
    if (existing) return cloneDescriptor(existing.descriptor);
    const descriptor: VisualAssetBlobDescriptor = {
      assetId,
      kind: input.kind,
      mediaType,
      contentHash: expectedContentHash,
      byteLength: bytes.byteLength,
      storedAtIso: storedAt.toISOString()
    };
    this.#blobs.set(assetId, { descriptor, bytes });
    return cloneDescriptor(descriptor);
  }

  /**
   * Reads one asset only when both its ID and frozen hash match.
   *
   * @param assetId Managed ID from a visual configuration.
   * @param expectedContentHash Exact historical hash.
   * @returns Copied bytes and descriptor, or undefined for a non-match.
   * @example A historical order cannot read the latest bytes by ID alone.
   * @since 0.10.9
   * @modified 2026-09-18 - Added hash-pinned defensive reads.
   */
  async read(assetId: string, expectedContentHash: string): Promise<StoredVisualAssetBlob | undefined> {
    const normalizedId = normalizeText(assetId, "assetId");
    const normalizedHash = normalizeHash(expectedContentHash, "expectedContentHash");
    const stored = this.#blobs.get(normalizedId);
    if (!stored || stored.descriptor.contentHash !== normalizedHash) return undefined;
    return { descriptor: cloneDescriptor(stored.descriptor), bytes: stored.bytes.slice(0) };
  }

  /**
   * Describes the bytes currently bound to a managed ID without returning data.
   *
   * @param assetId Stable managed identifier.
   * @returns Detached descriptor or undefined when the ID is unavailable.
   * @example Diagnostics compare this hash with a design snapshot.
   * @since 0.10.9
   * @modified 2026-09-18 - Added lightweight availability lookup.
   */
  async describe(assetId: string): Promise<VisualAssetBlobDescriptor | undefined> {
    const descriptor = this.#blobs.get(normalizeText(assetId, "assetId"))?.descriptor;
    return descriptor ? cloneDescriptor(descriptor) : undefined;
  }

  /**
   * Atomically records an immutable historical owner after validating all bytes.
   *
   * @param manifest Owner/version and exact asset references.
   * @returns Canonical copied manifest.
   * @throws When any bytes are missing/mismatched or an owner is rewritten.
   * @example Retain all three LODs for catalog version 3 in one operation.
   * @since 0.10.9
   * @modified 2026-09-18 - Added all-or-nothing retention behavior.
   */
  async retain(manifest: VisualAssetRetentionManifest): Promise<VisualAssetRetentionManifest> {
    const normalized = normalizeManifest(manifest);
    const issues = await this.#auditManifest(normalized);
    if (issues.length > 0) {
      throw new Error(`Retention ${normalized.ownerId} references unavailable asset bytes.`);
    }
    const existing = this.#retentions.get(normalized.ownerId);
    if (existing) {
      const existingIdentity = JSON.stringify(existing.assets);
      const requestedIdentity = JSON.stringify(normalized.assets);
      if (existingIdentity !== requestedIdentity) {
        throw new Error(`Retention owner ${normalized.ownerId} is immutable.`);
      }
      return { ...existing, assets: existing.assets.map((asset) => ({ ...asset })) };
    }
    this.#retentions.set(normalized.ownerId, normalized);
    return { ...normalized, assets: normalized.assets.map((asset) => ({ ...asset })) };
  }

  /**
   * Reads one immutable owner manifest.
   *
   * @param ownerId Catalog/order version retention identity.
   * @returns Detached manifest or undefined when not retained.
   * @since 0.10.9
   * @modified 2026-09-18 - Added retention evidence lookup.
   */
  async readRetention(ownerId: string): Promise<VisualAssetRetentionManifest | undefined> {
    const manifest = this.#retentions.get(normalizeText(ownerId, "ownerId"));
    return manifest
      ? { ...manifest, assets: manifest.assets.map((asset) => ({ ...asset })) }
      : undefined;
  }

  /**
   * Rechecks an existing retention owner against current stored descriptors.
   *
   * @param ownerId Existing immutable retention owner.
   * @returns Missing/replaced byte evidence; empty means current references pass.
   * @example A maintenance job audits historical catalog versions before GC.
   * @since 0.10.9
   * @modified 2026-09-18 - Added retention-integrity audit.
   */
  async auditRetention(ownerId: string): Promise<readonly VisualAssetRetentionIssue[]> {
    const normalizedOwnerId = normalizeText(ownerId, "ownerId");
    const manifest = this.#retentions.get(normalizedOwnerId);
    return manifest ? this.#auditManifest(manifest) : [];
  }

  /** Audits without mutating blobs or owner manifests. */
  async #auditManifest(
    manifest: VisualAssetRetentionManifest
  ): Promise<readonly VisualAssetRetentionIssue[]> {
    const issues: VisualAssetRetentionIssue[] = [];
    for (const reference of manifest.assets) {
      const descriptor = this.#blobs.get(reference.assetId)?.descriptor;
      if (!descriptor) {
        issues.push({
          ownerId: manifest.ownerId,
          assetId: reference.assetId,
          expectedContentHash: reference.contentHash,
          code: "ASSET_BYTES_MISSING"
        });
      } else if (descriptor.contentHash !== reference.contentHash) {
        issues.push({
          ownerId: manifest.ownerId,
          assetId: reference.assetId,
          expectedContentHash: reference.contentHash,
          code: "ASSET_BYTES_HASH_MISMATCH"
        });
      }
    }
    return issues;
  }
}

/**
 * Reference registry for upload-grant contract tests and local API prototypes.
 *
 * Production must replace this with a database/Redis implementation scoped to
 * the authenticated backend. Registration is idempotent for the exact same
 * grant and rejects any attempt to rebind an opaque grant ID to new authority.
 *
 * @since 0.10.12
 * @modified 2026-09-18 - Added executable upload-authorization registry contract.
 */
export class InMemoryManagedGltfUploadGrantRegistry
implements ManagedGltfUploadGrantRegistry {
  readonly #grants = new Map<string, ManagedGltfUploadGrant>();

  /** Registers one immutable capability or returns the identical existing grant. */
  async register(grant: ManagedGltfUploadGrant): Promise<ManagedGltfUploadGrant> {
    if (grant.schemaVersion !== "doormes-managed-gltf-upload-grant.v1") {
      throw new Error("Unsupported managed model upload grant schema.");
    }
    const grantId = normalizeText(grant.grantId, "grant.grantId");
    const normalized = issueManagedGltfUploadGrant({
      grantId,
      actor: { actorId: grant.actorId, roles: ["asset-designer"] },
      assetId: grant.assetId,
      expectedContentHash: grant.expectedContentHash,
      mediaType: grant.mediaType,
      byteLength: grant.byteLength,
      issuedAtIso: grant.issuedAtIso,
      expiresAtIso: grant.expiresAtIso
    });
    const existing = this.#grants.get(grantId);
    if (existing && JSON.stringify(existing) !== JSON.stringify(normalized)) {
      throw new Error(`Managed model upload grant ${grantId} is immutable.`);
    }
    if (!existing) this.#grants.set(grantId, normalized);
    return cloneUploadGrant(existing ?? normalized);
  }

  /** Resolves one detached capability by opaque ID without accepting client fields. */
  async resolve(grantId: string): Promise<ManagedGltfUploadGrant | undefined> {
    const grant = this.#grants.get(normalizeText(grantId, "grantId"));
    return grant ? cloneUploadGrant(grant) : undefined;
  }
}

/**
 * Transactional reference catalog store backed by the blob-retention port.
 *
 * The internal promise queue models a serial database transaction for tests:
 * each commit compares revision before retaining bytes, and no second commit
 * can interleave between that comparison and history append. Production API
 * code replaces this class with its database transaction implementation.
 *
 * @since 0.10.12
 * @modified 2026-09-18 - Added executable publication CAS/retention contract.
 */
export class InMemoryComponentAssetCatalogPublicationStore
implements ComponentAssetCatalogPublicationStore {
  readonly #blobStore: VisualAssetBlobStore;
  readonly #states = new Map<string, ComponentAssetCatalogState>();
  #commitQueue: Promise<void> = Promise.resolve();

  constructor(blobStore: VisualAssetBlobStore) {
    this.#blobStore = blobStore;
  }

  /** Returns a detached append-only history or an empty revision-zero state. */
  async read(catalogItemId: string): Promise<ComponentAssetCatalogState> {
    const normalizedId = normalizeText(catalogItemId, "catalogItemId");
    const state = this.#states.get(normalizedId) ?? {
      catalogItemId: normalizedId,
      revision: 0,
      history: []
    };
    return structuredClone(state);
  }

  /** Serializes one compare-and-swap publication and exact byte retention. */
  commit(input: Readonly<{
    expectedRevision: number;
    version: PublishedComponentAssetCatalogVersion;
  }>): Promise<ComponentAssetCatalogState> {
    const operation = this.#commitQueue.then(async () => {
      const catalogItemId = normalizeText(input.version.catalogItemId, "version.catalogItemId");
      const current = this.#states.get(catalogItemId) ?? {
        catalogItemId,
        revision: 0,
        history: []
      };
      if (!Number.isSafeInteger(input.expectedRevision) || input.expectedRevision < 0) {
        throw new RangeError("Expected catalog revision must be a non-negative integer.");
      }
      if (current.revision !== input.expectedRevision) {
        throw new Error(
          `Component asset catalog revision conflict: expected ${input.expectedRevision}, ` +
          `actual ${current.revision}.`
        );
      }
      const expectedVersion = current.history.reduce(
        (maximum, entry) => Math.max(maximum, entry.catalogVersion),
        0
      ) + 1;
      if (input.version.catalogVersion !== expectedVersion) {
        throw new Error(`Component asset catalog version must append as ${expectedVersion}.`);
      }
      await retainPublishedComponentAssetVersion(this.#blobStore, input.version);
      const next: ComponentAssetCatalogState = {
        catalogItemId,
        revision: current.revision + 1,
        history: [...current.history, structuredClone(input.version)]
      };
      this.#states.set(catalogItemId, next);
      return structuredClone(next);
    });
    this.#commitQueue = operation.then(() => undefined, () => undefined);
    return operation;
  }
}

interface IndexedDbVisualAssetRecord extends VisualAssetBlobDescriptor {
  readonly bytes: ArrayBuffer;
}

const INDEXED_DB_BLOB_STORE = "visual-assets";
const INDEXED_DB_RETENTION_STORE = "visual-asset-retentions";

/** Resolves one IndexedDB request without leaking event callbacks to adapters. */
function indexedDbRequest<T>(request: IDBRequest<T>): Promise<T> {
  return new Promise((resolve, reject) => {
    request.addEventListener("success", () => resolve(request.result), { once: true });
    request.addEventListener("error", () => reject(
      request.error ?? new Error("IndexedDB request failed.")), { once: true });
  });
}

/** Waits for an IndexedDB transaction's atomic commit or abort result. */
function indexedDbTransaction(transaction: IDBTransaction): Promise<void> {
  return new Promise((resolve, reject) => {
    transaction.addEventListener("complete", () => resolve(), { once: true });
    transaction.addEventListener("abort", () => reject(
      transaction.error ?? new Error("IndexedDB transaction was aborted.")), { once: true });
    transaction.addEventListener("error", () => reject(
      transaction.error ?? new Error("IndexedDB transaction failed.")), { once: true });
  });
}

/**
 * Durable browser implementation of the visual-asset storage port.
 *
 * Two object stores keep hash-verified blobs and immutable owner manifests.
 * Blob insertion and manifest retention use read/write transactions; retention
 * checks every referenced record inside the same transaction that writes the
 * owner, so a browser interruption cannot expose a partial historical version.
 * The factory is injectable for worker/test environments while production uses
 * `globalThis.indexedDB` from the composition root.
 *
 * @example Create once at app startup and share it across desktop/mobile shells.
 * @since 0.10.11
 * @modified 2026-09-18 - Added production browser persistence adapter.
 */
export class IndexedDbVisualAssetBlobStore implements VisualAssetBlobStore {
  readonly #databaseName: string;
  readonly #factory: IDBFactory;
  readonly #database: Promise<IDBDatabase>;

  /**
   * Opens or upgrades the private DoorMes asset database.
   *
   * @param options Stable database name and optional injected IndexedDB factory.
   * @throws When IndexedDB is unavailable or database opening is blocked/fails.
   * @since 0.10.11
   * @modified 2026-09-18 - Added schema-v1 browser store initialization.
   */
  constructor(options: Readonly<{
    databaseName?: string;
    indexedDbFactory?: IDBFactory;
  }> = {}) {
    this.#databaseName = normalizeText(
      options.databaseName ?? "doormes-visual-assets-v1",
      "databaseName"
    );
    const factory = options.indexedDbFactory ?? globalThis.indexedDB;
    if (!factory) throw new Error("IndexedDB is unavailable in this runtime.");
    this.#factory = factory;
    this.#database = this.#open();
  }

  /** Opens schema version one and creates stores only during upgrade. */
  #open(): Promise<IDBDatabase> {
    return new Promise((resolve, reject) => {
      const request = this.#factory.open(this.#databaseName, 1);
      request.addEventListener("upgradeneeded", () => {
        const database = request.result;
        if (!database.objectStoreNames.contains(INDEXED_DB_BLOB_STORE)) {
          database.createObjectStore(INDEXED_DB_BLOB_STORE, { keyPath: "assetId" });
        }
        if (!database.objectStoreNames.contains(INDEXED_DB_RETENTION_STORE)) {
          database.createObjectStore(INDEXED_DB_RETENTION_STORE, { keyPath: "ownerId" });
        }
      });
      request.addEventListener("success", () => resolve(request.result), { once: true });
      request.addEventListener("blocked", () => reject(
        new Error(`IndexedDB ${this.#databaseName} upgrade is blocked.`)), { once: true });
      request.addEventListener("error", () => reject(
        request.error ?? new Error(`IndexedDB ${this.#databaseName} could not be opened.`)),
      { once: true });
    });
  }

  /**
   * Hashes and persists one immutable asset record.
   *
   * @param input Managed identity, exact bytes and storage audit metadata.
   * @returns Persisted descriptor; identical retries are idempotent.
   * @throws On invalid bytes or an existing ID bound to another hash/type.
   * @since 0.10.11
   * @modified 2026-09-18 - Added transactional IndexedDB byte insertion.
   */
  async put(input: Readonly<{
    assetId: string;
    kind: VisualAssetBlobKind;
    mediaType: string;
    expectedContentHash: string;
    bytes: ArrayBuffer;
    storedAtIso: string;
  }>): Promise<VisualAssetBlobDescriptor> {
    const assetId = normalizeText(input.assetId, "asset.assetId");
    const mediaType = normalizeText(input.mediaType, "asset.mediaType");
    const expectedContentHash = normalizeHash(input.expectedContentHash, "asset.contentHash");
    if (input.bytes.byteLength < 1 || input.bytes.byteLength > MAX_VISUAL_ASSET_BYTES) {
      throw new RangeError(`Visual asset bytes must contain 1..${MAX_VISUAL_ASSET_BYTES} bytes.`);
    }
    const storedAt = new Date(input.storedAtIso);
    if (Number.isNaN(storedAt.valueOf())) {
      throw new Error("asset.storedAtIso must be a valid date-time value.");
    }
    const bytes = input.bytes.slice(0);
    if (await calculateVisualAssetContentHash(bytes) !== expectedContentHash) {
      throw new Error(`Visual asset ${assetId} bytes do not match the expected content hash.`);
    }
    const database = await this.#database;
    const transaction = database.transaction(INDEXED_DB_BLOB_STORE, "readwrite");
    const complete = indexedDbTransaction(transaction);
    const store = transaction.objectStore(INDEXED_DB_BLOB_STORE);
    try {
      const existing = await indexedDbRequest(
        store.get(assetId) as IDBRequest<IndexedDbVisualAssetRecord | undefined>
      );
      if (existing && existing.contentHash !== expectedContentHash) {
        throw new Error(`Visual asset ${assetId} cannot be rebound to different content.`);
      }
      if (existing && (existing.kind !== input.kind || existing.mediaType !== mediaType)) {
        throw new Error(`Visual asset ${assetId} cannot be rebound to different metadata.`);
      }
      if (existing) {
        await complete;
        return cloneDescriptor(existing);
      }
      const record: IndexedDbVisualAssetRecord = {
        assetId,
        kind: input.kind,
        mediaType,
        contentHash: expectedContentHash,
        byteLength: bytes.byteLength,
        storedAtIso: storedAt.toISOString(),
        bytes
      };
      await indexedDbRequest(store.add(record));
      await complete;
      return cloneDescriptor(record);
    } catch (error) {
      try {
        transaction.abort();
      } catch {
        // The transaction may already have auto-aborted after a failed request.
      }
      await complete.catch(() => undefined);
      throw error;
    }
  }

  /** Reads copied bytes only for an exact ID/hash pair. */
  async read(assetId: string, expectedContentHash: string): Promise<StoredVisualAssetBlob | undefined> {
    const normalizedId = normalizeText(assetId, "assetId");
    const normalizedHash = normalizeHash(expectedContentHash, "expectedContentHash");
    const database = await this.#database;
    const transaction = database.transaction(INDEXED_DB_BLOB_STORE, "readonly");
    const complete = indexedDbTransaction(transaction);
    const record = await indexedDbRequest(
      transaction.objectStore(INDEXED_DB_BLOB_STORE).get(normalizedId) as
        IDBRequest<IndexedDbVisualAssetRecord | undefined>
    );
    await complete;
    if (!record || record.contentHash !== normalizedHash) return undefined;
    return { descriptor: cloneDescriptor(record), bytes: record.bytes.slice(0) };
  }

  /** Returns immutable metadata without loading it into a renderer. */
  async describe(assetId: string): Promise<VisualAssetBlobDescriptor | undefined> {
    const database = await this.#database;
    const transaction = database.transaction(INDEXED_DB_BLOB_STORE, "readonly");
    const complete = indexedDbTransaction(transaction);
    const record = await indexedDbRequest(
      transaction.objectStore(INDEXED_DB_BLOB_STORE).get(normalizeText(assetId, "assetId")) as
        IDBRequest<IndexedDbVisualAssetRecord | undefined>
    );
    await complete;
    return record ? cloneDescriptor(record) : undefined;
  }

  /** Atomically validates all bytes and writes one immutable owner manifest. */
  async retain(manifest: VisualAssetRetentionManifest): Promise<VisualAssetRetentionManifest> {
    const normalized = normalizeManifest(manifest);
    const database = await this.#database;
    const transaction = database.transaction(
      [INDEXED_DB_BLOB_STORE, INDEXED_DB_RETENTION_STORE],
      "readwrite"
    );
    const complete = indexedDbTransaction(transaction);
    const blobs = transaction.objectStore(INDEXED_DB_BLOB_STORE);
    const retentions = transaction.objectStore(INDEXED_DB_RETENTION_STORE);
    try {
      for (const reference of normalized.assets) {
        const record = await indexedDbRequest(
          blobs.get(reference.assetId) as IDBRequest<IndexedDbVisualAssetRecord | undefined>
        );
        if (!record || record.contentHash !== reference.contentHash) {
          throw new Error(`Retention ${normalized.ownerId} references unavailable asset bytes.`);
        }
      }
      const existing = await indexedDbRequest(
        retentions.get(normalized.ownerId) as IDBRequest<VisualAssetRetentionManifest | undefined>
      );
      if (existing && JSON.stringify(existing.assets) !== JSON.stringify(normalized.assets)) {
        throw new Error(`Retention owner ${normalized.ownerId} is immutable.`);
      }
      if (!existing) await indexedDbRequest(retentions.add(normalized));
      await complete;
      const result = existing ?? normalized;
      return { ...result, assets: result.assets.map((asset) => ({ ...asset })) };
    } catch (error) {
      try {
        transaction.abort();
      } catch {
        // The transaction may already have auto-aborted after a failed request.
      }
      await complete.catch(() => undefined);
      throw error;
    }
  }

  /** Returns a detached historical owner manifest, when present. */
  async readRetention(ownerId: string): Promise<VisualAssetRetentionManifest | undefined> {
    const database = await this.#database;
    const transaction = database.transaction(INDEXED_DB_RETENTION_STORE, "readonly");
    const complete = indexedDbTransaction(transaction);
    const result = await indexedDbRequest(
      transaction.objectStore(INDEXED_DB_RETENTION_STORE).get(
        normalizeText(ownerId, "ownerId")
      ) as IDBRequest<VisualAssetRetentionManifest | undefined>
    );
    await complete;
    return result ? { ...result, assets: result.assets.map((asset) => ({ ...asset })) } : undefined;
  }

  /** Rechecks one retained owner against persisted blob records. */
  async auditRetention(ownerId: string): Promise<readonly VisualAssetRetentionIssue[]> {
    const normalizedOwnerId = normalizeText(ownerId, "ownerId");
    const database = await this.#database;
    const transaction = database.transaction(
      [INDEXED_DB_BLOB_STORE, INDEXED_DB_RETENTION_STORE],
      "readonly"
    );
    const complete = indexedDbTransaction(transaction);
    const manifest = await indexedDbRequest(
      transaction.objectStore(INDEXED_DB_RETENTION_STORE).get(normalizedOwnerId) as
        IDBRequest<VisualAssetRetentionManifest | undefined>
    );
    if (!manifest) {
      await complete;
      return [];
    }
    const issues: VisualAssetRetentionIssue[] = [];
    const blobs = transaction.objectStore(INDEXED_DB_BLOB_STORE);
    for (const reference of manifest.assets) {
      const record = await indexedDbRequest(
        blobs.get(reference.assetId) as IDBRequest<IndexedDbVisualAssetRecord | undefined>
      );
      if (!record) {
        issues.push({
          ownerId: normalizedOwnerId,
          assetId: reference.assetId,
          expectedContentHash: reference.contentHash,
          code: "ASSET_BYTES_MISSING"
        });
      } else if (record.contentHash !== reference.contentHash) {
        issues.push({
          ownerId: normalizedOwnerId,
          assetId: reference.assetId,
          expectedContentHash: reference.contentHash,
          code: "ASSET_BYTES_HASH_MISMATCH"
        });
      }
    }
    await complete;
    return issues;
  }

  /** Closes this adapter's database connection during application disposal. */
  async dispose(): Promise<void> {
    (await this.#database).close();
  }
}

/**
 * Runs model preflight and persists the exact verified bytes as one operation.
 *
 * The inspection executes before the storage write. Therefore invalid hashes,
 * external URIs and excessive complexity cannot leave a managed descriptor.
 * The returned evidence may be attached to a review draft; catalog publication
 * still requires its independent permission and retention checks.
 *
 * @param store Selected browser/backend storage adapter.
 * @param input Managed identity, bytes and audit metadata from the upload API.
 * @returns Descriptor plus hash-covered complexity evidence.
 * @example Upload a high-detail handle GLB, then reference its descriptor ID/hash.
 * @since 0.10.10
 * @modified 2026-09-18 - Added preflight-to-storage orchestration.
 */
export async function inspectAndStoreManagedGltfAsset(
  store: VisualAssetBlobStore,
  input: Readonly<{
    assetId: string;
    expectedContentHash: string;
    mediaType: "model/gltf-binary" | "model/gltf+json";
    bytes: ArrayBuffer;
    inspectedAtIso: string;
  }>
): Promise<ManagedGltfUploadEvidence> {
  const inspectedAt = new Date(input.inspectedAtIso);
  if (Number.isNaN(inspectedAt.valueOf())) {
    throw new Error("inspectedAtIso must be a valid date-time value.");
  }
  const inspection = await inspectManagedGltfAsset({
    bytes: input.bytes,
    expectedContentHash: input.expectedContentHash
  });
  const asset = await store.put({
    assetId: input.assetId,
    kind: "component-model",
    mediaType: input.mediaType,
    expectedContentHash: inspection.contentHash,
    bytes: input.bytes,
    storedAtIso: inspectedAt.toISOString()
  });
  return { asset, inspection, inspectedAtIso: inspectedAt.toISOString() };
}

/**
 * Local gateway used by the desktop app while no backend upload API is mounted.
 *
 * It deliberately implements the same editor-facing interface as a future HTTP
 * client but delegates to one injected IndexedDB/in-memory store. This keeps
 * local preview useful without teaching the shared UI about browser databases.
 *
 * @since 0.10.12
 * @modified 2026-09-18 - Isolated local persistence behind the upload gateway.
 */
export class LocalManagedGltfAssetGateway implements ManagedGltfAssetGateway {
  readonly #store: VisualAssetBlobStore;

  constructor(store: VisualAssetBlobStore) {
    this.#store = store;
  }

  /** Runs the common preflight and writes exact bytes to the local store. */
  upload(input: Readonly<{
    assetId: string;
    expectedContentHash: string;
    mediaType: "model/gltf-binary" | "model/gltf+json";
    bytes: ArrayBuffer;
    inspectedAtIso: string;
  }>): Promise<ManagedGltfUploadEvidence> {
    return inspectAndStoreManagedGltfAsset(this.#store, input);
  }

  /** Resolves persisted metadata for editor diagnostics after page reload. */
  describe(assetId: string): Promise<VisualAssetBlobDescriptor | undefined> {
    return this.#store.describe(assetId);
  }

  /** Reloads hash-pinned bytes for the shared Three.js runtime registry. */
  read(assetId: string, expectedContentHash: string): Promise<StoredVisualAssetBlob | undefined> {
    return this.#store.read(assetId, expectedContentHash);
  }
}

/** HTTP response returned after the authenticated API registers one grant. */
export interface ManagedGltfUploadAuthorizationResponse {
  readonly grantId: string;
  readonly expiresAtIso: string;
}

/**
 * Browser HTTP gateway for the production managed-model upload protocol.
 *
 * Protocol:
 * 1. POST `/visual-assets/model-uploads/authorize` with exact public metadata.
 * 2. PUT raw bytes to `/visual-assets/model-uploads/{opaqueGrantId}`.
 * 3. GET `/visual-assets/{assetId}` for hash-pinned availability diagnostics.
 *
 * Cookies/session headers are handled by `fetch` credentials. The client never
 * sends an actor ID or role; the server derives identity from its authenticated
 * request context. Returned evidence is checked against the requested ID/hash
 * before the editor may commit a design command.
 *
 * @since 0.10.13
 * @modified 2026-09-18 - Added concrete authenticated HTTP client adapter.
 */
export class HttpManagedGltfAssetGateway implements ManagedGltfAssetGateway {
  readonly #baseUrl: string;
  readonly #fetch: typeof fetch;
  readonly #credentials: RequestCredentials;

  constructor(options: Readonly<{
    baseUrl?: string;
    fetch?: typeof fetch;
    credentials?: RequestCredentials;
  }> = {}) {
    const baseUrl = (options.baseUrl ?? "/api").trim().replace(/\/+$/u, "");
    if (!baseUrl) throw new Error("Managed model HTTP gateway baseUrl must not be empty.");
    const fetchImplementation = options.fetch ?? globalThis.fetch;
    if (!fetchImplementation) throw new Error("Fetch is unavailable in this runtime.");
    this.#baseUrl = baseUrl;
    this.#fetch = fetchImplementation.bind(globalThis);
    this.#credentials = options.credentials ?? "same-origin";
  }

  /** Requests a short grant, uploads raw bytes, then validates returned evidence. */
  async upload(input: Readonly<{
    assetId: string;
    expectedContentHash: string;
    mediaType: "model/gltf-binary" | "model/gltf+json";
    bytes: ArrayBuffer;
    inspectedAtIso: string;
  }>): Promise<ManagedGltfUploadEvidence> {
    const assetId = normalizeText(input.assetId, "assetId");
    const expectedContentHash = normalizeHash(input.expectedContentHash, "expectedContentHash");
    const authorizationResponse = await this.#fetch(
      `${this.#baseUrl}/visual-assets/model-uploads/authorize`,
      {
        method: "POST",
        credentials: this.#credentials,
        headers: { "content-type": "application/json" },
        body: JSON.stringify({
          assetId,
          expectedContentHash,
          mediaType: input.mediaType,
          byteLength: input.bytes.byteLength
        })
      }
    );
    const authorization = await this.#json<ManagedGltfUploadAuthorizationResponse>(
      authorizationResponse,
      "Managed model upload authorization failed."
    );
    const grantId = normalizeText(authorization.grantId, "authorization.grantId");
    if (Number.isNaN(new Date(authorization.expiresAtIso).valueOf())) {
      throw new Error("Managed model upload authorization returned an invalid expiry.");
    }
    const uploadResponse = await this.#fetch(
      `${this.#baseUrl}/visual-assets/model-uploads/${encodeURIComponent(grantId)}`,
      {
        method: "PUT",
        credentials: this.#credentials,
        headers: { "content-type": input.mediaType },
        body: input.bytes
      }
    );
    const evidence = await this.#json<ManagedGltfUploadEvidence>(
      uploadResponse,
      "Managed model upload failed."
    );
    if (
      evidence.asset?.assetId !== assetId ||
      evidence.asset?.contentHash?.toLowerCase() !== expectedContentHash
    ) {
      throw new Error("Managed model upload response does not match the requested asset identity.");
    }
    if (
      evidence.inspection?.contentHash?.toLowerCase() !== expectedContentHash ||
      evidence.inspection?.byteLength !== input.bytes.byteLength
    ) {
      throw new Error("Managed model upload evidence does not match the uploaded bytes.");
    }
    return structuredClone(evidence);
  }

  /** Reads immutable descriptor metadata; HTTP 404 means the asset is unavailable. */
  async describe(assetId: string): Promise<VisualAssetBlobDescriptor | undefined> {
    const normalizedId = normalizeText(assetId, "assetId");
    const response = await this.#fetch(
      `${this.#baseUrl}/visual-assets/${encodeURIComponent(normalizedId)}`,
      { method: "GET", credentials: this.#credentials, cache: "no-store" }
    );
    if (response.status === 404) return undefined;
    const descriptor = await this.#json<VisualAssetBlobDescriptor>(
      response,
      "Managed model asset lookup failed."
    );
    if (descriptor.assetId !== normalizedId) {
      throw new Error("Managed model asset lookup returned another asset identity.");
    }
    return cloneDescriptor(descriptor);
  }

  /**
   * Reloads exact bytes through the managed content endpoint.
   *
   * Algorithm: resolve immutable descriptor metadata first, reject a missing or
   * different hash without downloading bytes, fetch the raw object, then verify
   * length and SHA-256 again in the browser. This makes a local-directory or
   * future object-store response equivalent to IndexedDB for the renderer.
   *
   * @since 0.10.16
   * @modified 2026-09-18 - Added HTTP-backed persisted GLB/glTF reload.
   */
  async read(
    assetId: string,
    expectedContentHash: string
  ): Promise<StoredVisualAssetBlob | undefined> {
    const normalizedId = normalizeText(assetId, "assetId");
    const normalizedHash = normalizeHash(expectedContentHash, "expectedContentHash");
    const descriptor = await this.describe(normalizedId);
    if (!descriptor || descriptor.contentHash.toLowerCase() !== normalizedHash) return undefined;
    const response = await this.#fetch(
      `${this.#baseUrl}/visual-assets/${encodeURIComponent(normalizedId)}/content?` +
      `contentHash=${encodeURIComponent(normalizedHash)}`,
      { method: "GET", credentials: this.#credentials, cache: "no-store" }
    );
    if (response.status === 404) return undefined;
    if (!response.ok) {
      await this.#json<unknown>(response, "Managed model asset content reload failed.");
      return undefined;
    }
    const bytes = await response.arrayBuffer();
    if (
      bytes.byteLength !== descriptor.byteLength ||
      await calculateVisualAssetContentHash(bytes) !== normalizedHash
    ) {
      throw new Error("Managed model asset content does not match its immutable descriptor.");
    }
    return { descriptor: cloneDescriptor(descriptor), bytes };
  }

  /** Parses one JSON response and preserves a bounded server error message. */
  async #json<T>(response: Response, fallbackMessage: string): Promise<T> {
    const text = await response.text();
    if (!response.ok) {
      let message = fallbackMessage;
      try {
        const body = JSON.parse(text) as { readonly message?: unknown };
        if (typeof body.message === "string" && body.message.trim()) {
          message = body.message.trim().slice(0, 500);
        }
      } catch {
        // Non-JSON proxy errors intentionally use the stable client fallback.
      }
      throw new Error(message);
    }
    try {
      return JSON.parse(text) as T;
    } catch {
      throw new Error(`${fallbackMessage} The server returned invalid JSON.`);
    }
  }
}

/**
 * Executes the backend upload endpoint using only an opaque registered grant.
 *
 * Algorithm: resolve server-side authority by ID, bind it to the authenticated
 * actor, reject expiry or byte-length tampering, then reuse the exact common
 * preflight/storage function. The request contains no client-controlled asset
 * ID, hash or MIME fields, preventing them from diverging from authorization.
 * Identical retries before expiry remain safe because the blob store is
 * content-addressed and idempotent for the same ID/hash.
 *
 * @param store Backend object/blob storage adapter.
 * @param grants Server-side opaque authorization registry.
 * @param input Authenticated actor, opaque grant ID, bytes and server time.
 * @returns Hash-covered upload and inspection evidence.
 * @throws On unknown, expired, foreign or size-mismatched authorization.
 * @example An HTTP handler derives actorId from its session, never request JSON.
 * @since 0.10.12
 * @modified 2026-09-18 - Added authorized upload endpoint orchestration.
 */
export async function inspectAndStoreAuthorizedManagedGltfAsset(
  store: VisualAssetBlobStore,
  grants: ManagedGltfUploadGrantRegistry,
  input: Readonly<{
    grantId: string;
    actorId: string;
    bytes: ArrayBuffer;
    uploadedAtIso: string;
  }>
): Promise<ManagedGltfUploadEvidence> {
  const grant = await grants.resolve(normalizeText(input.grantId, "grantId"));
  if (!grant) throw new Error("Managed model upload grant is unknown.");
  const actorId = normalizeText(input.actorId, "actorId");
  if (grant.actorId !== actorId) {
    throw new Error("Managed model upload grant belongs to another actor.");
  }
  const uploadedAt = new Date(input.uploadedAtIso);
  if (Number.isNaN(uploadedAt.valueOf())) {
    throw new Error("uploadedAtIso must be a valid date-time value.");
  }
  if (uploadedAt.valueOf() < new Date(grant.issuedAtIso).valueOf()) {
    throw new Error("Managed model upload grant is not active yet.");
  }
  if (uploadedAt.valueOf() > new Date(grant.expiresAtIso).valueOf()) {
    throw new Error("Managed model upload grant has expired.");
  }
  if (input.bytes.byteLength !== grant.byteLength) {
    throw new Error("Managed model upload byte length does not match its grant.");
  }
  return inspectAndStoreManagedGltfAsset(store, {
    assetId: grant.assetId,
    expectedContentHash: grant.expectedContentHash,
    mediaType: grant.mediaType,
    bytes: input.bytes,
    inspectedAtIso: uploadedAt.toISOString()
  });
}

/**
 * Retains every exact byte object referenced by one published catalog version.
 *
 * This adapter function bridges the pure catalog publication result to blob
 * storage without making the appearance model depend on persistence. Backend
 * code should commit the catalog row and returned manifest in one transaction;
 * until that API exists, failure here means the caller must not persist the new
 * history entry.
 *
 * @param store Concrete storage port implementation.
 * @param version Newly published immutable catalog version.
 * @returns Stored manifest keyed by catalog item and exact version.
 * @example Version 3 retains high/medium/low hashes under one immutable owner.
 * @since 0.10.9
 * @modified 2026-09-18 - Connected catalog retention lists to byte storage.
 */
export async function retainPublishedComponentAssetVersion(
  store: VisualAssetBlobStore,
  version: PublishedComponentAssetCatalogVersion
): Promise<VisualAssetRetentionManifest> {
  return store.retain({
    ownerId: `component-catalog:${version.catalogItemId}:version:${version.catalogVersion}`,
    retainedAtIso: version.publishedAtIso,
    assets: version.retainedAssets.map(({ assetId, contentHash }) => ({ assetId, contentHash }))
  });
}

/**
 * Publishes a reviewed model through optimistic concurrency and byte retention.
 *
 * Algorithm: read the current item revision, reject a stale review immediately,
 * use the pure appearance-model rule to authorize and construct the next exact
 * version, then ask the publication store to compare-and-swap history together
 * with its retention manifest. A concurrent approver can win only once; the
 * loser receives a revision conflict and must reload the reviewed draft.
 *
 * @param store Database-facing atomic publication adapter.
 * @param input Authenticated actor, reviewed draft, server time and UI revision.
 * @returns Updated detached item history and incremented repository revision.
 * @throws On permission, missing bytes, stale revision or invalid model data.
 * @example Two requests with expected revision 0 produce one version 1 and one conflict.
 * @since 0.10.12
 * @modified 2026-09-18 - Added review publication transaction orchestration.
 */
export async function publishRetainedComponentAssetCatalogVersion(
  store: ComponentAssetCatalogPublicationStore,
  input: Readonly<{
    actor: ComponentAssetCatalogActor;
    draft: ComponentAssetCatalogDraft;
    publishedAtIso: string;
    expectedRevision: number;
  }>
): Promise<ComponentAssetCatalogState> {
  const current = await store.read(input.draft.catalogItemId);
  if (current.revision !== input.expectedRevision) {
    throw new Error(
      `Component asset catalog revision conflict: expected ${input.expectedRevision}, ` +
      `actual ${current.revision}.`
    );
  }
  const nextHistory = publishComponentAssetCatalogVersion({
    actor: input.actor,
    draft: input.draft,
    publishedAtIso: input.publishedAtIso,
    history: current.history
  });
  const nextVersion = nextHistory[nextHistory.length - 1];
  if (!nextVersion) throw new Error("Component asset publication did not produce a version.");
  return store.commit({ expectedRevision: input.expectedRevision, version: nextVersion });
}
