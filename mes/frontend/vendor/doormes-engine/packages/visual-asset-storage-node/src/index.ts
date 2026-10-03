import { createHash, randomUUID } from "node:crypto";
import {
  mkdir,
  readFile,
  rename,
  stat,
  unlink,
  writeFile
} from "node:fs/promises";
import { dirname, join, resolve } from "node:path";
import {
  calculateVisualAssetContentHash,
  type StoredVisualAssetBlob,
  type VisualAssetBlobDescriptor,
  type VisualAssetBlobKind,
  type VisualAssetBlobStore,
  type VisualAssetRetentionIssue,
  type VisualAssetRetentionManifest
} from "@doormes/visual-asset-storage";

const MAX_VISUAL_ASSET_BYTES = 50 * 1024 * 1024;
const SHA256_PATTERN = /^sha256:[0-9a-f]{64}$/u;

/** Returns a validated non-empty identity without interpreting it as a path. */
function normalizeText(value: string, field: string): string {
  const normalized = value.trim();
  if (!normalized) throw new Error(`${field} must not be empty.`);
  return normalized;
}

/** Returns one canonical content hash used by descriptors and object filenames. */
function normalizeHash(value: string, field: string): string {
  const normalized = normalizeText(value, field).toLowerCase();
  if (!SHA256_PATTERN.test(normalized)) {
    throw new Error(`${field} must be a lowercase sha256:<64 hex> identity.`);
  }
  return normalized;
}

/** Converts an external ID to a fixed filesystem-safe key, preventing traversal. */
function pathKey(value: string): string {
  return createHash("sha256").update(value, "utf8").digest("hex");
}

/** Copies a descriptor so callers cannot mutate storage state by reference. */
function cloneDescriptor(value: VisualAssetBlobDescriptor): VisualAssetBlobDescriptor {
  return { ...value };
}

/** Canonicalizes an immutable retention manifest before it reaches disk. */
function normalizeManifest(value: VisualAssetRetentionManifest): VisualAssetRetentionManifest {
  const ownerId = normalizeText(value.ownerId, "manifest.ownerId");
  const retainedAt = new Date(value.retainedAtIso);
  if (Number.isNaN(retainedAt.valueOf())) {
    throw new Error("manifest.retainedAtIso must be a valid date-time value.");
  }
  const identities = new Set<string>();
  const assets = value.assets.map((asset, index) => {
    const assetId = normalizeText(asset.assetId, `manifest.assets[${index}].assetId`);
    const contentHash = normalizeHash(
      asset.contentHash,
      `manifest.assets[${index}].contentHash`
    );
    const identity = `${assetId}|${contentHash}`;
    if (identities.has(identity)) {
      throw new Error(`Retention manifest contains duplicate asset ${assetId}.`);
    }
    identities.add(identity);
    return { assetId, contentHash };
  }).sort((left, right) =>
    `${left.assetId}|${left.contentHash}`.localeCompare(`${right.assetId}|${right.contentHash}`));
  return { ownerId, retainedAtIso: retainedAt.toISOString(), assets };
}

/** Reads JSON when present and rejects directory/corrupt-record surprises. */
async function readJsonFile<T>(path: string): Promise<T | undefined> {
  try {
    return JSON.parse(await readFile(path, "utf8")) as T;
  } catch (error) {
    if ((error as NodeJS.ErrnoException).code === "ENOENT") return undefined;
    throw error;
  }
}

/** Writes a complete file through a sibling temporary path and atomic rename. */
async function writeAtomic(path: string, bytes: string | Uint8Array): Promise<void> {
  await mkdir(dirname(path), { recursive: true });
  const temporaryPath = `${path}.${process.pid}.${randomUUID()}.tmp`;
  try {
    await writeFile(temporaryPath, bytes, { flag: "wx" });
    await rename(temporaryPath, path);
  } catch (error) {
    await unlink(temporaryPath).catch(() => undefined);
    throw error;
  }
}

/** Validates a descriptor loaded from disk before any bytes are exposed. */
function normalizeDescriptor(value: VisualAssetBlobDescriptor): VisualAssetBlobDescriptor {
  const storedAt = new Date(value.storedAtIso);
  if (Number.isNaN(storedAt.valueOf())) throw new Error("Stored asset timestamp is invalid.");
  if (value.kind !== "component-model" && value.kind !== "texture-bundle") {
    throw new Error("Stored asset kind is invalid.");
  }
  if (!Number.isSafeInteger(value.byteLength) || value.byteLength < 1) {
    throw new Error("Stored asset byte length is invalid.");
  }
  return {
    assetId: normalizeText(value.assetId, "stored assetId"),
    kind: value.kind,
    mediaType: normalizeText(value.mediaType, "stored mediaType"),
    contentHash: normalizeHash(value.contentHash, "stored contentHash"),
    byteLength: value.byteLength,
    storedAtIso: storedAt.toISOString()
  };
}

/**
 * Node-only durable visual-asset store used by the local development API.
 *
 * Layout and algorithm:
 * - `objects/<sha256>.blob` stores deduplicated immutable content bytes;
 * - `assets/<hash(assetId)>.json` binds a stable managed ID to that hash;
 * - `retentions/<hash(ownerId)>.json` freezes catalog/order ownership.
 *
 * External IDs are never used as path fragments. Mutations are serialized in
 * this process and metadata is written through atomic rename, so interrupted
 * uploads cannot expose half-written JSON. This adapter is intended for one
 * local Vite process; production multi-node storage must implement the same
 * port with database/object-store transactions.
 *
 * @example `new FileSystemVisualAssetBlobStore("./runtime-data/visual-assets")`.
 * @since 0.10.14
 * @modified 2026-09-18 - Added configurable project-local upload persistence.
 */
export class FileSystemVisualAssetBlobStore implements VisualAssetBlobStore {
  readonly #rootDirectory: string;
  #mutationTail: Promise<void> = Promise.resolve();

  constructor(rootDirectory: string) {
    this.#rootDirectory = resolve(normalizeText(rootDirectory, "rootDirectory"));
  }

  /** Absolute directory containing objects, descriptors and retention manifests. */
  get rootDirectory(): string {
    return this.#rootDirectory;
  }

  /** Hashes, validates and persistently binds exact bytes to one managed ID. */
  put(input: Readonly<{
    assetId: string;
    kind: VisualAssetBlobKind;
    mediaType: string;
    expectedContentHash: string;
    bytes: ArrayBuffer;
    storedAtIso: string;
  }>): Promise<VisualAssetBlobDescriptor> {
    return this.#serialize(async () => {
      const assetId = normalizeText(input.assetId, "asset.assetId");
      const mediaType = normalizeText(input.mediaType, "asset.mediaType");
      const expectedContentHash = normalizeHash(input.expectedContentHash, "asset.contentHash");
      if (input.kind !== "component-model" && input.kind !== "texture-bundle") {
        throw new Error("Visual asset kind is not supported.");
      }
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
      const descriptorPath = this.#descriptorPath(assetId);
      const existing = await this.#readDescriptor(assetId);
      if (existing && existing.contentHash !== expectedContentHash) {
        throw new Error(`Visual asset ${assetId} cannot be rebound to different content.`);
      }
      if (existing) return cloneDescriptor(existing);

      const objectPath = this.#objectPath(expectedContentHash);
      try {
        const objectStats = await stat(objectPath);
        if (!objectStats.isFile() || objectStats.size !== bytes.byteLength) {
          throw new Error(`Stored content object ${expectedContentHash} has an invalid size.`);
        }
        const existingObject = await readFile(objectPath);
        const existingBytes = existingObject.buffer.slice(
          existingObject.byteOffset,
          existingObject.byteOffset + existingObject.byteLength
        ) as ArrayBuffer;
        if (await calculateVisualAssetContentHash(existingBytes) !== expectedContentHash) {
          throw new Error(`Stored content object ${expectedContentHash} failed hash verification.`);
        }
      } catch (error) {
        if ((error as NodeJS.ErrnoException).code !== "ENOENT") throw error;
        await writeAtomic(objectPath, new Uint8Array(bytes));
      }

      const descriptor: VisualAssetBlobDescriptor = {
        assetId,
        kind: input.kind,
        mediaType,
        contentHash: expectedContentHash,
        byteLength: bytes.byteLength,
        storedAtIso: storedAt.toISOString()
      };
      await writeAtomic(descriptorPath, `${JSON.stringify(descriptor, null, 2)}\n`);
      return cloneDescriptor(descriptor);
    });
  }

  /** Reads bytes only when the stable ID and historical hash both match. */
  async read(
    assetId: string,
    expectedContentHash: string
  ): Promise<StoredVisualAssetBlob | undefined> {
    const normalizedId = normalizeText(assetId, "assetId");
    const normalizedHash = normalizeHash(expectedContentHash, "expectedContentHash");
    const descriptor = await this.#readDescriptor(normalizedId);
    if (!descriptor || descriptor.contentHash !== normalizedHash) return undefined;
    try {
      const data = await readFile(this.#objectPath(normalizedHash));
      const bytes = data.buffer.slice(data.byteOffset, data.byteOffset + data.byteLength);
      if (
        bytes.byteLength !== descriptor.byteLength ||
        await calculateVisualAssetContentHash(bytes) !== normalizedHash
      ) {
        return undefined;
      }
      return { descriptor: cloneDescriptor(descriptor), bytes };
    } catch (error) {
      if ((error as NodeJS.ErrnoException).code === "ENOENT") return undefined;
      throw error;
    }
  }

  /** Reads immutable descriptor metadata without loading the model bytes. */
  describe(assetId: string): Promise<VisualAssetBlobDescriptor | undefined> {
    return this.#readDescriptor(normalizeText(assetId, "assetId"));
  }

  /** Validates every exact asset, then atomically writes one immutable owner. */
  retain(manifest: VisualAssetRetentionManifest): Promise<VisualAssetRetentionManifest> {
    return this.#serialize(async () => {
      const normalized = normalizeManifest(manifest);
      const issues = await this.#auditManifest(normalized);
      if (issues.length > 0) {
        throw new Error(`Retention ${normalized.ownerId} references unavailable asset bytes.`);
      }
      const path = this.#retentionPath(normalized.ownerId);
      const existingValue = await readJsonFile<VisualAssetRetentionManifest>(path);
      if (existingValue) {
        const existing = normalizeManifest(existingValue);
        if (JSON.stringify(existing.assets) !== JSON.stringify(normalized.assets)) {
          throw new Error(`Retention owner ${normalized.ownerId} is immutable.`);
        }
        return structuredClone(existing);
      }
      await writeAtomic(path, `${JSON.stringify(normalized, null, 2)}\n`);
      return structuredClone(normalized);
    });
  }

  /** Reads a retained catalog/order owner, if it exists. */
  async readRetention(ownerId: string): Promise<VisualAssetRetentionManifest | undefined> {
    const normalizedId = normalizeText(ownerId, "ownerId");
    const value = await readJsonFile<VisualAssetRetentionManifest>(
      this.#retentionPath(normalizedId)
    );
    if (!value) return undefined;
    const normalized = normalizeManifest(value);
    if (normalized.ownerId !== normalizedId) throw new Error("Retention owner identity is corrupt.");
    return structuredClone(normalized);
  }

  /** Reports missing or replaced bytes for one retained owner. */
  async auditRetention(ownerId: string): Promise<readonly VisualAssetRetentionIssue[]> {
    const manifest = await this.readRetention(ownerId);
    return manifest ? this.#auditManifest(manifest) : [];
  }

  /** Serializes writes so same-process retries cannot interleave metadata. */
  #serialize<T>(operation: () => Promise<T>): Promise<T> {
    const result = this.#mutationTail.then(operation, operation);
    this.#mutationTail = result.then(() => undefined, () => undefined);
    return result;
  }

  /** Resolves and validates the descriptor bound to an exact managed ID. */
  async #readDescriptor(assetId: string): Promise<VisualAssetBlobDescriptor | undefined> {
    const value = await readJsonFile<VisualAssetBlobDescriptor>(this.#descriptorPath(assetId));
    if (!value) return undefined;
    const descriptor = normalizeDescriptor(value);
    if (descriptor.assetId !== assetId) throw new Error("Stored asset identity is corrupt.");
    return descriptor;
  }

  /** Audits the exact hash identity for every reference in one manifest. */
  async #auditManifest(
    manifest: VisualAssetRetentionManifest
  ): Promise<readonly VisualAssetRetentionIssue[]> {
    const issues: VisualAssetRetentionIssue[] = [];
    for (const asset of manifest.assets) {
      const descriptor = await this.#readDescriptor(asset.assetId);
      if (!descriptor) {
        issues.push({
          ownerId: manifest.ownerId,
          assetId: asset.assetId,
          expectedContentHash: asset.contentHash,
          code: "ASSET_BYTES_MISSING"
        });
      } else if (
        descriptor.contentHash !== asset.contentHash ||
        !(await this.read(asset.assetId, asset.contentHash))
      ) {
        issues.push({
          ownerId: manifest.ownerId,
          assetId: asset.assetId,
          expectedContentHash: asset.contentHash,
          code: "ASSET_BYTES_HASH_MISMATCH"
        });
      }
    }
    return issues;
  }

  /** Hash-addressed immutable object path. */
  #objectPath(contentHash: string): string {
    return join(this.#rootDirectory, "objects", `${contentHash.slice("sha256:".length)}.blob`);
  }

  /** Stable-ID descriptor path; its filename cannot escape the configured root. */
  #descriptorPath(assetId: string): string {
    return join(this.#rootDirectory, "assets", `${pathKey(assetId)}.json`);
  }

  /** Immutable owner manifest path; owner text remains inside JSON for auditing. */
  #retentionPath(ownerId: string): string {
    return join(this.#rootDirectory, "retentions", `${pathKey(ownerId)}.json`);
  }
}
