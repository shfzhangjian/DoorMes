import { randomUUID } from "node:crypto";
import type { IncomingMessage, ServerResponse } from "node:http";
import {
  InMemoryManagedGltfUploadGrantRegistry,
  inspectAndStoreAuthorizedManagedGltfAsset,
  registerManagedGltfUploadAuthorization
} from "@doormes/visual-asset-storage";
import { FileSystemVisualAssetBlobStore } from "@doormes/visual-asset-storage-node";
import type { Plugin } from "vite";

const MAX_JSON_BODY_BYTES = 64 * 1024;
const MAX_GLTF_BODY_BYTES = 25 * 1024 * 1024;

interface AuthorizationBody {
  readonly assetId?: unknown;
  readonly expectedContentHash?: unknown;
  readonly mediaType?: unknown;
  readonly byteLength?: unknown;
}

/** Writes one bounded JSON response and prevents MIME sniffing in the dev API. */
function sendJson(response: ServerResponse, status: number, value: unknown): void {
  const body = `${JSON.stringify(value)}\n`;
  response.statusCode = status;
  response.setHeader("content-type", "application/json; charset=utf-8");
  response.setHeader("x-content-type-options", "nosniff");
  response.setHeader("cache-control", "no-store");
  response.end(body);
}

/** Streams one already-verified managed object without exposing its disk path. */
function sendAssetBytes(
  response: ServerResponse,
  mediaType: string,
  bytes: ArrayBuffer
): void {
  response.statusCode = 200;
  response.setHeader("content-type", mediaType);
  response.setHeader("content-length", String(bytes.byteLength));
  response.setHeader("x-content-type-options", "nosniff");
  response.setHeader("cache-control", "no-store");
  response.end(Buffer.from(bytes));
}

/** Reads a request body with an explicit byte ceiling before concatenation. */
async function readRequestBytes(request: IncomingMessage, maximumBytes: number): Promise<Buffer> {
  const declaredLength = Number(request.headers["content-length"] ?? 0);
  if (Number.isFinite(declaredLength) && declaredLength > maximumBytes) {
    throw new RangeError(`Request body exceeds the ${maximumBytes}-byte limit.`);
  }
  const chunks: Buffer[] = [];
  let length = 0;
  for await (const chunk of request) {
    const buffer = Buffer.isBuffer(chunk) ? chunk : Buffer.from(chunk as Uint8Array);
    length += buffer.byteLength;
    if (length > maximumBytes) {
      throw new RangeError(`Request body exceeds the ${maximumBytes}-byte limit.`);
    }
    chunks.push(buffer);
  }
  return Buffer.concat(chunks, length);
}

/** Converts a Node buffer view to the detached ArrayBuffer expected by the port. */
function toArrayBuffer(value: Buffer): ArrayBuffer {
  const copy = new Uint8Array(value.byteLength);
  copy.set(value);
  return copy.buffer;
}

/** Returns a required JSON string without trusting prototype-coerced values. */
function requiredString(value: unknown, field: string): string {
  if (typeof value !== "string" || !value.trim()) throw new Error(`${field} is required.`);
  return value.trim();
}

/**
 * Mounts the development-only managed GLB/glTF API on the Vite process.
 *
 * The browser receives only an opaque five-minute grant. The configured local
 * actor is injected by this server adapter, never accepted from request JSON.
 * Uploaded files pass the common hash/URI/complexity inspection and are then
 * persisted by the filesystem port beneath `storageDirectory`.
 *
 * This plugin is deliberately not a production authentication substitute. It
 * exists so designers and workers can verify real directory persistence before
 * the formal backend implements the same HTTP contract.
 *
 * @example Start Vite, choose a GLB, then inspect `runtime-data/visual-assets`.
 * @since 0.10.14
 * @modified 2026-09-18 - Added configurable local-directory upload API.
 */
export function createLocalVisualAssetApiPlugin(options: Readonly<{
  storageDirectory: string;
  actorId: string;
}>): Plugin {
  const store = new FileSystemVisualAssetBlobStore(options.storageDirectory);
  const grants = new InMemoryManagedGltfUploadGrantRegistry();
  const actor = {
    actorId: options.actorId.trim() || "local-worker",
    roles: ["asset-designer" as const]
  };

  return {
    name: "doormes-local-visual-asset-api",
    apply: "serve",
    configureServer(server) {
      server.config.logger.info(`DoorMes local model uploads: ${store.rootDirectory}`);
      server.middlewares.use(async (request, response, next) => {
        const method = request.method ?? "GET";
        const requestUrl = new URL(request.url ?? "/", "http://127.0.0.1");
        const pathname = requestUrl.pathname;
        const authorizationPath = "/api/visual-assets/model-uploads/authorize";
        const uploadMatch = pathname.match(/^\/api\/visual-assets\/model-uploads\/([^/]+)$/u);
        const contentMatch = pathname.match(/^\/api\/visual-assets\/([^/]+)\/content$/u);
        const descriptorMatch = pathname.match(/^\/api\/visual-assets\/([^/]+)$/u);
        const matchesApi = pathname.startsWith("/api/visual-assets/");
        if (!matchesApi) {
          next();
          return;
        }

        try {
          if (method === "POST" && pathname === authorizationPath) {
            const raw = await readRequestBytes(request, MAX_JSON_BODY_BYTES);
            const body = JSON.parse(raw.toString("utf8")) as AuthorizationBody;
            const issuedAt = new Date();
            const mediaType = requiredString(body.mediaType, "mediaType");
            if (mediaType !== "model/gltf-binary" && mediaType !== "model/gltf+json") {
              throw new Error("mediaType must be model/gltf-binary or model/gltf+json.");
            }
            const authorization = await registerManagedGltfUploadAuthorization(grants, {
              grantId: randomUUID(),
              actor,
              assetId: requiredString(body.assetId, "assetId"),
              expectedContentHash: requiredString(body.expectedContentHash, "expectedContentHash"),
              mediaType,
              byteLength: Number(body.byteLength),
              issuedAtIso: issuedAt.toISOString(),
              expiresAtIso: new Date(issuedAt.valueOf() + 5 * 60 * 1000).toISOString()
            });
            sendJson(response, 201, authorization);
            return;
          }

          if (method === "PUT" && uploadMatch?.[1]) {
            const grantId = decodeURIComponent(uploadMatch[1]);
            const grant = await grants.resolve(grantId);
            if (!grant) throw new Error("Managed model upload grant is unavailable.");
            const requestMediaType = request.headers["content-type"]?.split(";", 1)[0]?.trim();
            if (requestMediaType !== grant.mediaType) {
              throw new Error("Managed model upload Content-Type does not match its grant.");
            }
            const bytes = await readRequestBytes(request, MAX_GLTF_BODY_BYTES);
            const evidence = await inspectAndStoreAuthorizedManagedGltfAsset(store, grants, {
              actorId: actor.actorId,
              grantId,
              bytes: toArrayBuffer(bytes),
              uploadedAtIso: new Date().toISOString()
            });
            sendJson(response, 201, evidence);
            return;
          }

          if (method === "GET" && contentMatch?.[1]) {
            const assetId = decodeURIComponent(contentMatch[1]);
            const expectedContentHash = requiredString(
              requestUrl.searchParams.get("contentHash"),
              "contentHash"
            );
            const stored = await store.read(assetId, expectedContentHash);
            if (!stored) {
              sendJson(response, 404, { message: "Exact asset content not found." });
              return;
            }
            sendAssetBytes(response, stored.descriptor.mediaType, stored.bytes);
            return;
          }

          if (method === "GET" && descriptorMatch?.[1]) {
            const descriptor = await store.describe(decodeURIComponent(descriptorMatch[1]));
            sendJson(response, descriptor ? 200 : 404, descriptor ?? { message: "Asset not found." });
            return;
          }

          sendJson(response, 405, { message: "Method or local visual-asset route is not supported." });
        } catch (error) {
          const status = error instanceof RangeError ? 413 : 400;
          const message = error instanceof Error ? error.message : "Local visual-asset request failed.";
          sendJson(response, status, { message: message.slice(0, 500) });
        }
      });
    }
  };
}
