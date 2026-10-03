import { mkdtemp, readFile, readdir, rm } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join, resolve } from "node:path";
import { afterEach, describe, expect, it } from "vitest";
import { calculateVisualAssetContentHash } from "@doormes/visual-asset-storage";
import { FileSystemVisualAssetBlobStore } from "./index";

const temporaryDirectories: string[] = [];

/** Creates an isolated store root that is safe to recursively remove after a test. */
async function createTemporaryStore(): Promise<{
  directory: string;
  store: FileSystemVisualAssetBlobStore;
}> {
  const directory = await mkdtemp(join(tmpdir(), "doormes-visual-assets-"));
  temporaryDirectories.push(directory);
  return { directory, store: new FileSystemVisualAssetBlobStore(directory) };
}

afterEach(async () => {
  const operatingSystemTemp = `${resolve(tmpdir()).toLowerCase()}\\`;
  for (const directory of temporaryDirectories.splice(0)) {
    const target = resolve(directory);
    if (!`${target.toLowerCase()}\\`.startsWith(operatingSystemTemp)) {
      throw new Error(`Refusing to remove non-temporary test directory: ${target}`);
    }
    await rm(target, { recursive: true, force: true });
  }
});

describe("FileSystemVisualAssetBlobStore", () => {
  it("persists immutable bytes and descriptors across store instances", async () => {
    const { directory, store } = await createTemporaryStore();
    const bytes = new TextEncoder().encode('{"asset":{"version":"2.0"}}').buffer;
    const contentHash = await calculateVisualAssetContentHash(bytes);

    const descriptor = await store.put({
      assetId: "HANDLE-DEMO-HIGH",
      kind: "component-model",
      mediaType: "model/gltf+json",
      expectedContentHash: contentHash,
      bytes,
      storedAtIso: "2026-09-18T04:00:00.000Z"
    });

    const reopened = new FileSystemVisualAssetBlobStore(directory);
    expect(await reopened.describe("HANDLE-DEMO-HIGH")).toEqual(descriptor);
    expect(new Uint8Array((await reopened.read("HANDLE-DEMO-HIGH", contentHash))!.bytes))
      .toEqual(new Uint8Array(bytes));
    expect(await readdir(join(directory, "objects"))).toHaveLength(1);
    expect(await readdir(join(directory, "assets"))).toHaveLength(1);
  });

  it("deduplicates content while refusing to rebind an asset ID", async () => {
    const { directory, store } = await createTemporaryStore();
    const sharedBytes = new TextEncoder().encode('{"asset":{"version":"2.0"}}').buffer;
    const sharedHash = await calculateVisualAssetContentHash(sharedBytes);
    for (const assetId of ["HANDLE-A", "HANDLE-B"]) {
      await store.put({
        assetId,
        kind: "component-model",
        mediaType: "model/gltf+json",
        expectedContentHash: sharedHash,
        bytes: sharedBytes,
        storedAtIso: "2026-09-18T04:01:00.000Z"
      });
    }
    expect(await readdir(join(directory, "objects"))).toHaveLength(1);

    const replacement = new TextEncoder().encode('{"asset":{"version":"2.0"},"scene":0}').buffer;
    await expect(store.put({
      assetId: "HANDLE-A",
      kind: "component-model",
      mediaType: "model/gltf+json",
      expectedContentHash: await calculateVisualAssetContentHash(replacement),
      bytes: replacement,
      storedAtIso: "2026-09-18T04:02:00.000Z"
    })).rejects.toThrow("cannot be rebound");
  });

  it("retains exact asset hashes in an immutable owner manifest", async () => {
    const { directory, store } = await createTemporaryStore();
    const bytes = new TextEncoder().encode('{"asset":{"version":"2.0"}}').buffer;
    const contentHash = await calculateVisualAssetContentHash(bytes);
    await store.put({
      assetId: "LOCK-42-HIGH",
      kind: "component-model",
      mediaType: "model/gltf+json",
      expectedContentHash: contentHash,
      bytes,
      storedAtIso: "2026-09-18T04:03:00.000Z"
    });

    const manifest = await store.retain({
      ownerId: "component-catalog:LOCK-42:version:1",
      retainedAtIso: "2026-09-18T04:04:00.000Z",
      assets: [{ assetId: "LOCK-42-HIGH", contentHash }]
    });
    expect(await new FileSystemVisualAssetBlobStore(directory).readRetention(manifest.ownerId))
      .toEqual(manifest);
    expect(await store.auditRetention(manifest.ownerId)).toEqual([]);

    const retentionFiles = await readdir(join(directory, "retentions"));
    expect(retentionFiles).toHaveLength(1);
    expect(await readFile(join(directory, "retentions", retentionFiles[0]!), "utf8"))
      .toContain("LOCK-42-HIGH");
  });
});
