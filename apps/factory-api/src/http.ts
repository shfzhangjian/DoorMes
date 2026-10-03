import type { IncomingMessage, ServerResponse } from "node:http";
import { type FactoryMutationRequest } from "@doormes/contracts/factory-workflow";
import { FACTORY_MUTATION_JSON_SCHEMA } from "@doormes/contracts/factory-command-schema";
import { FACTORY_WORKSPACE_JSON_SCHEMA } from "@doormes/contracts/factory-workspace-schema";
import { FileFactoryStore } from "./store.js";
import { check, calculateFactoryDesign, compareFactoryVersions, FactoryError } from "./workflow.js";

function json(response: ServerResponse, status: number, value: unknown): void {
  response.writeHead(status, { "content-type": "application/json; charset=utf-8", "cache-control": "no-store", "x-content-type-options": "nosniff" }); response.end(JSON.stringify(value));
}
async function body(request: IncomingMessage): Promise<Record<string, unknown>> {
  check(request.headers["content-type"]?.split(";")[0]?.trim() === "application/json", "请使用application/json", "CONTENT_TYPE", 415);
  let size = 0; const chunks: Buffer[] = [];
  for await (const chunk of request) { const bytes = Buffer.from(chunk); size += bytes.length; check(size <= 20 * 1024 * 1024, "请求超过20 MiB", "BODY_TOO_LARGE", 413); chunks.push(bytes); }
  try { const value: unknown = JSON.parse(Buffer.concat(chunks).toString("utf8")); check(value && typeof value === "object" && !Array.isArray(value), "JSON对象格式无效", "INVALID_JSON", 400); return value as Record<string, unknown>; }
  catch (error) { if (error instanceof FactoryError) throw error; throw new FactoryError(400, "INVALID_JSON", "JSON无法解析"); }
}
function sessionToken(request: IncomingMessage): string | undefined {
  return request.headers.cookie?.split(";").map((part) => part.trim()).find((part) => part.startsWith("doormes_factory_session="))?.slice("doormes_factory_session=".length);
}
/** Shared Node HTTP handler used by Vite development and the standalone backend. */
export function createFactoryHttpHandler(store: FileFactoryStore) {
  return async (request: IncomingMessage, response: ServerResponse, next?: () => void): Promise<void> => {
    const url = new URL(request.url ?? "/", "http://localhost"), path = url.pathname;
    if (!path.startsWith("/api/factory/")) { if (next) next(); else json(response, 404, { error: { code: "NOT_FOUND", message: "接口不存在" } }); return; }
    try {
      const method = request.method ?? "GET";
      if (!["GET", "HEAD"].includes(method)) {
        const origin = request.headers.origin;
        check(!origin || new URL(origin).host === request.headers.host, "拒绝跨来源写入", "ORIGIN_REJECTED", 403);
        check(request.headers["sec-fetch-site"] !== "cross-site", "拒绝跨站请求", "ORIGIN_REJECTED", 403);
      }
      if (method === "GET" && path === "/api/factory/health") { const state = await store.read(); json(response, 200, { schemaVersion: state.schemaVersion, revision: state.revision, mode: "prototype" }); return; }
      if (method === "GET" && path === "/api/factory/schema") { json(response, 200, FACTORY_MUTATION_JSON_SCHEMA); return; }
      if (method === "GET" && path === "/api/factory/schema/workspace") { json(response, 200, FACTORY_WORKSPACE_JSON_SCHEMA); return; }
      if (method === "POST" && path === "/api/factory/login") {
        const input = await body(request), result = await store.login(input.username, input.password, request.socket.remoteAddress ?? "local");
        response.setHeader("set-cookie", `doormes_factory_session=${result.token}; HttpOnly; SameSite=Strict; Path=/api/factory; Max-Age=28800`);
        json(response, 200, { actor: result.actor }); return;
      }
      const actor = await store.actor(sessionToken(request));
      if (method === "GET" && path === "/api/factory/session") { json(response, 200, { actor }); return; }
      if (method === "POST" && path === "/api/factory/logout") { store.logout(sessionToken(request)); response.setHeader("set-cookie", "doormes_factory_session=; HttpOnly; SameSite=Strict; Path=/api/factory; Max-Age=0"); json(response, 200, { ok: true }); return; }
      if (method === "GET" && path === "/api/factory/workspace") { json(response, 200, await store.read()); return; }
      if (method === "GET" && path === "/api/factory/people") { json(response, 200, { people: await store.people(actor) }); return; }
      const calculation = path.match(/^\/api\/factory\/designs\/([^/]+)\/calculation$/u);
      if (method === "GET" && calculation) {
        const design = (await store.read()).designs.find((entry) => entry.id === calculation[1]);
        check(design, "设计不存在", "NOT_FOUND", 404); json(response, 200, calculateFactoryDesign(design.document)); return;
      }
      if (method === "POST" && path === "/api/factory/commands") {
        const input = await body(request); json(response, 200, await store.mutate(actor, input as unknown as FactoryMutationRequest)); return;
      }
      if (method === "GET" && path === "/api/factory/versions") { json(response, 200, { revisions: await store.versions() }); return; }
      const releaseDiff = path.match(/^\/api\/factory\/releases\/([^/]+)\/diff$/u);
      if (method === "GET" && releaseDiff) {
        const state = await store.read(), release = state.releases.find((entry) => entry.id === releaseDiff[1]);
        check(release, "发布版本不存在", "NOT_FOUND", 404);
        const against = url.searchParams.get("against");
        const before = state.releases.find((entry) => entry.id === (against ?? release.supersedes));
        if (against !== null) check(before, "对比基线不存在", "NOT_FOUND", 404);
        check(!before || before.productCode === release.productCode, "请选择同一产品的发布版本进行对比", "INVALID_COMPARISON", 422);
        json(response, 200, { before, after: release, differences: before ? compareFactoryVersions({ document: before.document, bom: before.calculation.lines, route: before.calculation.route }, { document: release.document, bom: release.calculation.lines, route: release.calculation.route }) : [] }); return;
      }
      const version = path.match(/^\/api\/factory\/versions\/(\d+)$/u);
      if (method === "GET" && version) { json(response, 200, await store.version(Number(version[1]))); return; }
      if (method === "GET" && path === "/api/factory/users") { json(response, 200, { users: await store.users(actor) }); return; }
      if (method === "POST" && path === "/api/factory/users") { json(response, 200, { users: await store.saveUser(actor, await body(request) as unknown as Parameters<FileFactoryStore["saveUser"]>[1]) }); return; }
      throw new FactoryError(404, "NOT_FOUND", "接口不存在");
    } catch (error) {
      const known = error instanceof FactoryError;
      json(response, known ? error.status : 500, { error: { code: known ? error.code : "SERVER_ERROR", message: known ? error.message : "服务端处理失败，请检查后端日志" } });
      if (!known) console.error("DoorMes factory API:", error);
    }
  };
}
