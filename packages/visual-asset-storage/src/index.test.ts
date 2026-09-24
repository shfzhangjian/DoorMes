import { readFile } from "node:fs/promises";
import { resolve } from "node:path";
import { describe, expect, it } from "vitest";
import {
  createManagedGltfGeometrySnapshot,
  createReferenceWindowVisualConfiguration
} from "@doormes/appearance-model";
import {
  InMemoryComponentAssetCatalogPublicationStore,
  InMemoryManagedGltfUploadGrantRegistry,
  InMemoryVisualAssetBlobStore,
  HttpManagedGltfAssetGateway,
  LocalManagedGltfAssetGateway,
  inspectManagedGltfAsset,
  inspectAndStoreManagedGltfAsset,
  inspectAndStoreAuthorizedManagedGltfAsset,
  issueManagedGltfUploadGrant,
  publishRetainedComponentAssetCatalogVersion,
  registerManagedGltfUploadAuthorization,
  retainPublishedComponentAssetVersion
} from "./index";

const ABC_HASH = "sha256:ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad";
const XYZ_HASH = "sha256:3608bca1e44ea6c4d268eb6db02260269892c0b42b86bbf1e77a6fa16c3c9282";

function bytes(value: string): ArrayBuffer {
  return new TextEncoder().encode(value).buffer as ArrayBuffer;
}

async function hash(value: ArrayBuffer): Promise<string> {
  const digest = await crypto.subtle.digest("SHA-256", value);
  return `sha256:${[...new Uint8Array(digest)]
    .map((octet) => octet.toString(16).padStart(2, "0"))
    .join("")}`;
}

describe("visual asset byte retention", () => {
  it("verifies bytes, copies reads and prevents one managed ID from rebinding", async () => {
    const store = new InMemoryVisualAssetBlobStore();
    const descriptor = await store.put({
      assetId: "ASSET-HANDLE-HIGH",
      kind: "component-model",
      mediaType: "model/gltf-binary",
      expectedContentHash: ABC_HASH,
      bytes: bytes("abc"),
      storedAtIso: "2026-09-18T08:00:00.000Z"
    });

    expect(descriptor).toMatchObject({
      assetId: "ASSET-HANDLE-HIGH",
      contentHash: ABC_HASH,
      byteLength: 3
    });
    const firstRead = await store.read("ASSET-HANDLE-HIGH", ABC_HASH);
    expect(new TextDecoder().decode(firstRead?.bytes)).toBe("abc");
    if (firstRead) new Uint8Array(firstRead.bytes)[0] = 0;
    expect(new TextDecoder().decode((await store.read("ASSET-HANDLE-HIGH", ABC_HASH))?.bytes))
      .toBe("abc");
    await expect(store.put({
      assetId: "ASSET-HANDLE-HIGH",
      kind: "component-model",
      mediaType: "model/gltf-binary",
      expectedContentHash: XYZ_HASH,
      bytes: bytes("xyz"),
      storedAtIso: "2026-09-18T08:01:00.000Z"
    })).rejects.toThrow(/cannot be rebound/i);
    await expect(store.put({
      assetId: "ASSET-BAD-HASH",
      kind: "component-model",
      mediaType: "model/gltf-binary",
      expectedContentHash: ABC_HASH,
      bytes: bytes("xyz"),
      storedAtIso: "2026-09-18T08:02:00.000Z"
    })).rejects.toThrow(/do not match/i);
  });

  it("retains a published version only after every hash-pinned blob exists", async () => {
    const store = new InMemoryVisualAssetBlobStore();
    const referenceModel = createReferenceWindowVisualConfiguration().hardwareModels.find(
      ({ role }) => role === "handle"
    )!.model;
    await store.put({
      assetId: "ASSET-HANDLE-HIGH",
      kind: "component-model",
      mediaType: "model/gltf-binary",
      expectedContentHash: ABC_HASH,
      bytes: bytes("abc"),
      storedAtIso: "2026-09-18T08:00:00.000Z"
    });
    const publication = {
      catalogItemId: "HANDLE-42",
      catalogVersion: 3,
      sourceDraftId: "DRAFT-HANDLE-42",
      sourceDraftRevision: 5,
      publishedAtIso: "2026-09-18T09:00:00.000Z",
      publishedBy: "approver-1",
      model: referenceModel,
      retainedAssets: [
        { quality: "high" as const, assetId: "ASSET-HANDLE-HIGH", contentHash: ABC_HASH },
        { quality: "low" as const, assetId: "ASSET-HANDLE-LOW", contentHash: XYZ_HASH }
      ]
    };

    await expect(retainPublishedComponentAssetVersion(store, publication))
      .rejects.toThrow(/unavailable asset bytes/i);
    expect(await store.readRetention("component-catalog:HANDLE-42:version:3")).toBeUndefined();

    await store.put({
      assetId: "ASSET-HANDLE-LOW",
      kind: "component-model",
      mediaType: "model/gltf-binary",
      expectedContentHash: XYZ_HASH,
      bytes: bytes("xyz"),
      storedAtIso: "2026-09-18T08:10:00.000Z"
    });
    const manifest = await retainPublishedComponentAssetVersion(store, publication);

    expect(manifest.ownerId).toBe("component-catalog:HANDLE-42:version:3");
    expect(manifest.assets).toEqual([
      { assetId: "ASSET-HANDLE-HIGH", contentHash: ABC_HASH },
      { assetId: "ASSET-HANDLE-LOW", contentHash: XYZ_HASH }
    ]);
    expect(await store.auditRetention(manifest.ownerId)).toEqual([]);
    await expect(store.retain({
      ...manifest,
      assets: [{ assetId: "ASSET-HANDLE-HIGH", contentHash: ABC_HASH }]
    })).rejects.toThrow(/immutable/i);
  });

  it("stores hash-covered preflight evidence and leaves no bytes after rejection", async () => {
    const store = new InMemoryVisualAssetBlobStore();
    const validBytes = bytes(JSON.stringify({
      asset: { version: "2.0" },
      nodes: [{}],
      accessors: [{ count: 12 }],
      meshes: [{ primitives: [{ attributes: { POSITION: 0 } }] }],
      buffers: [{ uri: "data:application/octet-stream;base64,AA==" }]
    }));
    const evidence = await inspectAndStoreManagedGltfAsset(store, {
      assetId: "ASSET-PREFLIGHT-VALID",
      expectedContentHash: await hash(validBytes),
      mediaType: "model/gltf+json",
      bytes: validBytes,
      inspectedAtIso: "2026-09-18T10:00:00.000Z"
    });

    expect(evidence).toMatchObject({
      asset: { assetId: "ASSET-PREFLIGHT-VALID", byteLength: validBytes.byteLength },
      inspection: {
        nodeCount: 1,
        meshCount: 1,
        primitiveCount: 1,
        estimatedTriangleCount: 4
      },
      inspectedAtIso: "2026-09-18T10:00:00.000Z"
    });

    const externalBytes = bytes(JSON.stringify({
      asset: { version: "2.0" },
      buffers: [{ uri: "https://untrusted.example/model.bin" }]
    }));
    await expect(inspectAndStoreManagedGltfAsset(store, {
      assetId: "ASSET-PREFLIGHT-REJECTED",
      expectedContentHash: await hash(externalBytes),
      mediaType: "model/gltf+json",
      bytes: externalBytes,
      inspectedAtIso: "2026-09-18T10:01:00.000Z"
    })).rejects.toThrow(/external buffer or image URIs/i);
    expect(await store.describe("ASSET-PREFLIGHT-REJECTED")).toBeUndefined();
  });

  it("keeps the editor on a gateway that can be replaced by an HTTP adapter", async () => {
    const store = new InMemoryVisualAssetBlobStore();
    const gateway = new LocalManagedGltfAssetGateway(store);
    const validBytes = bytes(JSON.stringify({
      asset: { version: "2.0" },
      nodes: [{}],
      meshes: [{ primitives: [] }]
    }));
    const contentHash = await hash(validBytes);

    const evidence = await gateway.upload({
      assetId: "ASSET-GATEWAY-HIGH",
      expectedContentHash: contentHash,
      mediaType: "model/gltf+json",
      bytes: validBytes,
      inspectedAtIso: "2026-09-18T10:05:00.000Z"
    });

    expect(evidence.asset.assetId).toBe("ASSET-GATEWAY-HIGH");
    expect(await gateway.describe("ASSET-GATEWAY-HIGH")).toMatchObject({ contentHash });
    const reloaded = await gateway.read("ASSET-GATEWAY-HIGH", contentHash);
    expect(new Uint8Array(reloaded!.bytes)).toEqual(new Uint8Array(validBytes));
    expect(await gateway.read("ASSET-GATEWAY-HIGH", ABC_HASH)).toBeUndefined();
  });

  it("reloads exact HTTP bytes and verifies them against immutable metadata", async () => {
    const validBytes = bytes(JSON.stringify({
      asset: { version: "2.0" },
      nodes: [{}],
      meshes: [{ primitives: [] }]
    }));
    const contentHash = await hash(validBytes);
    const descriptor = {
      assetId: "ASSET-HTTP-RELOAD",
      kind: "component-model" as const,
      mediaType: "model/gltf+json",
      contentHash,
      byteLength: validBytes.byteLength,
      storedAtIso: "2026-09-18T12:10:00.000Z"
    };
    const requests: string[] = [];
    const fetchStub = (async (input: URL | RequestInfo) => {
      const url = String(input);
      requests.push(url);
      if (url.endsWith("/visual-assets/ASSET-HTTP-RELOAD")) {
        return new Response(JSON.stringify(descriptor), {
          status: 200,
          headers: { "content-type": "application/json" }
        });
      }
      if (url.includes("/visual-assets/ASSET-HTTP-RELOAD/content?")) {
        return new Response(validBytes, {
          status: 200,
          headers: { "content-type": "model/gltf+json" }
        });
      }
      return new Response("not found", { status: 404 });
    }) as typeof fetch;
    const gateway = new HttpManagedGltfAssetGateway({
      baseUrl: "https://mes.example.test/api",
      fetch: fetchStub
    });

    const reloaded = await gateway.read(descriptor.assetId, contentHash);

    expect(reloaded?.descriptor).toEqual(descriptor);
    expect(new Uint8Array(reloaded!.bytes)).toEqual(new Uint8Array(validBytes));
    expect(requests[1]).toContain(`contentHash=${encodeURIComponent(contentHash)}`);
    expect(await gateway.read(descriptor.assetId, ABC_HASH)).toBeUndefined();
    expect(requests).toHaveLength(3);
  });

  it("binds an authorized upload to its actor, exact bytes and expiration", async () => {
    const store = new InMemoryVisualAssetBlobStore();
    const grants = new InMemoryManagedGltfUploadGrantRegistry();
    const validBytes = bytes(JSON.stringify({
      asset: { version: "2.0" },
      nodes: [{}],
      meshes: [{ primitives: [] }]
    }));
    const contentHash = await hash(validBytes);
    const grant = issueManagedGltfUploadGrant({
      grantId: "GRANT-DESIGNER-1",
      actor: { actorId: "designer-1", roles: ["asset-designer"] },
      assetId: "ASSET-AUTHORIZED-HIGH",
      expectedContentHash: contentHash,
      mediaType: "model/gltf+json",
      byteLength: validBytes.byteLength,
      issuedAtIso: "2026-09-18T10:00:00.000Z",
      expiresAtIso: "2026-09-18T10:05:00.000Z"
    });
    await grants.register(grant);

    await expect(inspectAndStoreAuthorizedManagedGltfAsset(store, grants, {
      grantId: grant.grantId,
      actorId: "another-designer",
      bytes: validBytes,
      uploadedAtIso: "2026-09-18T10:01:00.000Z"
    })).rejects.toThrow(/another actor/i);
    await expect(inspectAndStoreAuthorizedManagedGltfAsset(store, grants, {
      grantId: grant.grantId,
      actorId: grant.actorId,
      bytes: validBytes.slice(0, validBytes.byteLength - 1),
      uploadedAtIso: "2026-09-18T10:01:00.000Z"
    })).rejects.toThrow(/byte length/i);
    await expect(inspectAndStoreAuthorizedManagedGltfAsset(store, grants, {
      grantId: grant.grantId,
      actorId: grant.actorId,
      bytes: validBytes,
      uploadedAtIso: "2026-09-18T10:05:00.001Z"
    })).rejects.toThrow(/expired/i);
    expect(await store.describe(grant.assetId)).toBeUndefined();

    const evidence = await inspectAndStoreAuthorizedManagedGltfAsset(store, grants, {
      grantId: grant.grantId,
      actorId: grant.actorId,
      bytes: validBytes,
      uploadedAtIso: "2026-09-18T10:02:00.000Z"
    });
    expect(evidence.asset).toMatchObject({
      assetId: grant.assetId,
      contentHash,
      byteLength: validBytes.byteLength
    });
  });

  it("uses the two-step HTTP protocol without accepting actor identity from JSON", async () => {
    const validBytes = bytes(JSON.stringify({
      asset: { version: "2.0" },
      nodes: [{}],
      meshes: [{ primitives: [] }]
    }));
    const contentHash = await hash(validBytes);
    const requests: Array<Readonly<{ url: string; init?: RequestInit }>> = [];
    const fetchStub = (async (input: URL | RequestInfo, init?: RequestInit) => {
      const url = String(input);
      requests.push({ url, init });
      if (url.endsWith("/authorize")) {
        return new Response(JSON.stringify({
          grantId: "opaque/grant-1",
          expiresAtIso: "2026-09-18T12:05:00.000Z"
        }), { status: 200, headers: { "content-type": "application/json" } });
      }
      if (url.includes("/model-uploads/")) {
        return new Response(JSON.stringify({
          asset: {
            assetId: "ASSET-HTTP-HIGH",
            kind: "component-model",
            mediaType: "model/gltf+json",
            contentHash,
            byteLength: validBytes.byteLength,
            storedAtIso: "2026-09-18T12:01:00.000Z"
          },
          inspection: {
            contentHash,
            byteLength: validBytes.byteLength,
            nodeCount: 1,
            meshCount: 1,
            primitiveCount: 0,
            estimatedTriangleCount: 0,
            materialCount: 0,
            imageCount: 0
          },
          inspectedAtIso: "2026-09-18T12:01:00.000Z"
        }), { status: 200, headers: { "content-type": "application/json" } });
      }
      return new Response("not found", { status: 404 });
    }) as typeof fetch;
    const gateway = new HttpManagedGltfAssetGateway({
      baseUrl: "https://mes.example.test/api/",
      fetch: fetchStub,
      credentials: "include"
    });

    const evidence = await gateway.upload({
      assetId: "ASSET-HTTP-HIGH",
      expectedContentHash: contentHash,
      mediaType: "model/gltf+json",
      bytes: validBytes,
      inspectedAtIso: "1999-01-01T00:00:00.000Z"
    });

    expect(evidence.asset.assetId).toBe("ASSET-HTTP-HIGH");
    expect(requests).toHaveLength(2);
    expect(requests[0]?.url).toBe(
      "https://mes.example.test/api/visual-assets/model-uploads/authorize"
    );
    expect(JSON.parse(String(requests[0]?.init?.body))).toEqual({
      assetId: "ASSET-HTTP-HIGH",
      expectedContentHash: contentHash,
      mediaType: "model/gltf+json",
      byteLength: validBytes.byteLength
    });
    expect(requests[1]?.url).toContain("opaque%2Fgrant-1");
    expect(requests[1]?.init?.body).toBe(validBytes);
    expect(await gateway.describe("ASSET-MISSING")).toBeUndefined();
  });

  it("registers only an opaque upload authorization response", async () => {
    const grants = new InMemoryManagedGltfUploadGrantRegistry();
    const response = await registerManagedGltfUploadAuthorization(grants, {
      grantId: "server-random-grant",
      actor: { actorId: "designer-7", roles: ["asset-designer"] },
      assetId: "ASSET-AUTH-7",
      expectedContentHash: ABC_HASH,
      mediaType: "model/gltf-binary",
      byteLength: 3,
      issuedAtIso: "2026-09-18T12:00:00.000Z",
      expiresAtIso: "2026-09-18T12:05:00.000Z"
    });

    expect(response).toEqual({
      grantId: "server-random-grant",
      expiresAtIso: "2026-09-18T12:05:00.000Z"
    });
    expect(await grants.resolve(response.grantId)).toMatchObject({
      actorId: "designer-7",
      assetId: "ASSET-AUTH-7",
      expectedContentHash: ABC_HASH
    });
  });

  it("rejects upload grants without a catalog role or with an excessive lifetime", async () => {
    const declaration = {
      grantId: "GRANT-INVALID",
      actor: { actorId: "viewer-1", roles: [] },
      assetId: "ASSET-INVALID",
      expectedContentHash: ABC_HASH,
      mediaType: "model/gltf-binary" as const,
      byteLength: 3,
      issuedAtIso: "2026-09-18T10:00:00.000Z",
      expiresAtIso: "2026-09-18T10:05:00.000Z"
    };
    expect(() => issueManagedGltfUploadGrant(declaration)).toThrow(/catalog role/i);
    expect(() => issueManagedGltfUploadGrant({
      ...declaration,
      actor: { actorId: "designer-1", roles: ["asset-designer"] },
      expiresAtIso: "2026-09-18T10:15:00.001Z"
    })).toThrow(/1\.\.15 minutes/i);
  });

  it("publishes and retains one catalog version under optimistic concurrency", async () => {
    const blobs = new InMemoryVisualAssetBlobStore();
    const publications = new InMemoryComponentAssetCatalogPublicationStore(blobs);
    const highBytes = bytes(JSON.stringify({ asset: { version: "2.0" }, nodes: [{}] }));
    const lowBytes = bytes(JSON.stringify({ asset: { version: "2.0" }, nodes: [{}, {}] }));
    const highHash = await hash(highBytes);
    const lowHash = await hash(lowBytes);
    await blobs.put({
      assetId: "ASSET-CAS-HIGH",
      kind: "component-model",
      mediaType: "model/gltf+json",
      expectedContentHash: highHash,
      bytes: highBytes,
      storedAtIso: "2026-09-18T11:00:00.000Z"
    });
    await blobs.put({
      assetId: "ASSET-CAS-LOW",
      kind: "component-model",
      mediaType: "model/gltf+json",
      expectedContentHash: lowHash,
      bytes: lowBytes,
      storedAtIso: "2026-09-18T11:00:01.000Z"
    });
    const referenceModel = createReferenceWindowVisualConfiguration().hardwareModels.find(
      ({ role }) => role === "handle"
    )!.model;
    const draft = {
      draftId: "DRAFT-CAS-1",
      catalogItemId: "HANDLE-CAS",
      revision: 7,
      reviewStatus: "in-review" as const,
      submittedBy: "designer-1",
      model: {
        ...referenceModel,
        modelId: "customer.handle.cas",
        modelVersion: "1.0.0",
        geometry: createManagedGltfGeometrySnapshot({
          primaryAsset: { assetId: "ASSET-CAS-HIGH", contentHash: highHash },
          lodAssets: [{
            quality: "low",
            assetId: "ASSET-CAS-LOW",
            contentHash: lowHash
          }],
          sourceUnit: "meter",
          upAxis: "+y",
          forwardAxis: "+z"
        }),
        productionStatus: "catalog-approved" as const,
        materialCode: "HW-HANDLE-CAS"
      }
    };
    const request = {
      actor: { actorId: "approver-1", roles: ["catalog-approver"] as const },
      draft,
      publishedAtIso: "2026-09-18T11:10:00.000Z",
      expectedRevision: 0
    };

    const attempts = await Promise.allSettled([
      publishRetainedComponentAssetCatalogVersion(publications, request),
      publishRetainedComponentAssetCatalogVersion(publications, request)
    ]);

    expect(attempts.filter(({ status }) => status === "fulfilled")).toHaveLength(1);
    expect(attempts.filter(({ status }) => status === "rejected")).toHaveLength(1);
    const state = await publications.read("HANDLE-CAS");
    expect(state).toMatchObject({ revision: 1 });
    expect(state.history).toHaveLength(1);
    expect(state.history[0]).toMatchObject({ catalogVersion: 1, sourceDraftRevision: 7 });
    expect(await blobs.readRetention("component-catalog:HANDLE-CAS:version:1"))
      .toMatchObject({
        assets: [
          { assetId: "ASSET-CAS-HIGH", contentHash: highHash },
          { assetId: "ASSET-CAS-LOW", contentHash: lowHash }
        ]
      });
  });
});

const WORKER_SAMPLE_ROOT = resolve(process.cwd(), "test-assets/gltf-import");

/** Reads one checked-in worker fixture without exposing Node Buffer semantics to the preflight. */
async function workerFixture(fileName: string): Promise<ArrayBuffer> {
  const file = await readFile(resolve(WORKER_SAMPLE_ROOT, fileName));
  return file.buffer.slice(file.byteOffset, file.byteOffset + file.byteLength) as ArrayBuffer;
}

describe("worker GLB/glTF import pack", () => {
  /**
   * Keeps every distributed positive fixture inside the same production gate.
   *
   * @example If an upstream binary is accidentally replaced or a limit changes,
   * this test fails before workers receive a sample that the editor will reject.
   * @since 0.10.12
   * @modified 2026-09-18 - Added pinned Khronos import-pack verification.
   */
  it.each([
    ["01-box-basic.glb", "sha256:ed52f7192b8311d700ac0ce80644e3852cd01537e4d62241b9acba023da3d54e"],
    ["02-box-vertex-colors.glb", "sha256:9c48227f33b0ba2fbcf23b98ebf60d1c8ae0c6e6c5281e0aa3cc58affee10382"],
    ["03-simple-meshes-embedded.gltf", "sha256:021cef90f29927aa7881daeff6acc84465bfb715dd1c9e4ab6f051bbe4b35828"],
    ["04-damaged-helmet-textured.glb", "sha256:a1e3b04de97b11de564ce6e53b95f02954a297f0008183ac63a4f5974f6b32d8"]
  ])("accepts distributed sample %s with its pinned hash", async (fileName, contentHash) => {
    const inspection = await inspectManagedGltfAsset({
      bytes: await workerFixture(fileName),
      expectedContentHash: contentHash
    });

    expect(inspection.contentHash).toBe(contentHash);
    expect(inspection.byteLength).toBeGreaterThan(0);
    expect(inspection.nodeCount).toBeGreaterThan(0);
    expect(inspection.meshCount).toBeGreaterThan(0);
  });

  /** Verifies that worker-facing negative examples fail for their documented reason. */
  it.each([
    [
      "05-external-uri-rejected.gltf",
      "sha256:0f1d19382e349a4cc454ba2eee408fbdcc99d0f0ccd085d5a150fadf386d6fc9",
      /external buffer or image URIs/i
    ],
    [
      "06-invalid-json-rejected.gltf",
      "sha256:dc2a9995cfe0f8936d2f797cf450d2a1b35000463bedee415a3f4379269a7f6c",
      /json/i
    ]
  ])("rejects distributed negative sample %s", async (fileName, contentHash, message) => {
    await expect(inspectManagedGltfAsset({
      bytes: await workerFixture(fileName),
      expectedContentHash: contentHash
    })).rejects.toThrow(message);
  });
});
