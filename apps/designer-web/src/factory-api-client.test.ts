import { describe, expect, it, vi } from "vitest";
import { FactoryApiClient, FactoryClientError } from "./factory-api-client.js";
describe("factory browser transport contract", () => {
  it("keeps the global receiver for default native-style fetch and injected transports", async () => {
    const transport = vi.fn(function (this: unknown) {
      if (this !== globalThis) throw new TypeError("Illegal invocation");
      return Promise.resolve(new Response("{}", { status: 200 }));
    });
    vi.stubGlobal("fetch", transport);
    try {
      const api = new FactoryApiClient();
      await api.session();
      await api.login("designer", "secret");
      await api.workspace();
      await new FactoryApiClient(transport).workspace();
      expect(transport.mock.contexts).toEqual([globalThis, globalThis, globalThis, globalThis]);
      expect(transport.mock.calls).toHaveLength(4);
    } finally {
      vi.unstubAllGlobals();
    }
  });
  it("reads archives and explicitly selected release baselines using authenticated GETs only", async () => {
    const transport = vi.fn().mockImplementation(async () => new Response("{}", { status: 200 }));
    const api = new FactoryApiClient(transport);
    await api.versions(); await api.version(12);
    await api.releaseComparison("release /目标", "base &?版本");
    await api.releaseComparison("release /目标");
    expect(transport.mock.calls.map(([url]) => url)).toEqual([
      "/api/factory/versions", "/api/factory/versions/12",
      "/api/factory/releases/" + encodeURIComponent("release /目标") + "/diff?against=" + encodeURIComponent("base &?版本"),
      "/api/factory/releases/" + encodeURIComponent("release /目标") + "/diff"
    ]);
    for (const [, options] of transport.mock.calls) expect(options).toMatchObject({ method: "GET", credentials: "same-origin", body: undefined });
  });
  it("uses same-origin HttpOnly sessions, no client actor spoofing, and reports structured conflicts", async () => {
    const transport = vi.fn().mockResolvedValue(new Response(JSON.stringify({ actor: { id: "user-designer" } }), { status: 200 }));
    const api = new FactoryApiClient(transport);
    await api.login("designer", "secret");
    expect(transport).toHaveBeenCalledWith("/api/factory/login", expect.objectContaining({ credentials: "same-origin", method: "POST", body: '{"username":"designer","password":"secret"}' }));
    transport.mockImplementation(async () => new Response(JSON.stringify({ error: { code: "REVISION_CONFLICT", message: "请刷新" } }), { status: 409 }));
    await expect(api.workspace()).rejects.toMatchObject({ code: "REVISION_CONFLICT", status: 409 });
    await expect(api.workspace()).rejects.toBeInstanceOf(FactoryClientError);
  });
});
