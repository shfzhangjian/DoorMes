import type { FactoryActor, FactoryCommand, FactoryMutationRequest, FactoryMutationResponse, FactoryWorkspace, FactoryRole, FactoryReleaseComparison } from "@doormes/contracts/factory-workflow";

export interface FactoryAccount extends FactoryActor { username: string; enabled: boolean }
export class FactoryClientError extends Error {
  constructor(readonly status: number, readonly code: string, message: string) { super(message); }
}
/** Session cookies stay HttpOnly. UI never supplies an actor or approval timestamp. */
export class FactoryApiClient {
  private readonly transport: typeof fetch;
  constructor(transport: typeof fetch = globalThis.fetch, private readonly base = "/api/factory") {
    // Native Window.fetch requires its global receiver, not this client instance.
    this.transport = transport.bind(globalThis);
  }
  async request<T>(path: string, body?: unknown): Promise<T> {
    const response = await this.transport(this.base + path, {
      method: body === undefined ? "GET" : "POST", credentials: "same-origin",
      headers: body === undefined ? {} : { "content-type": "application/json" },
      body: body === undefined ? undefined : JSON.stringify(body)
    });
    const value = await response.json() as { error?: { code: string; message: string } };
    if (!response.ok) throw new FactoryClientError(response.status, value.error?.code ?? "HTTP_ERROR", value.error?.message ?? "工厂服务访问失败");
    return value as T;
  }
  session(): Promise<{ actor: FactoryActor }> { return this.request("/session"); }
  login(username: string, password: string): Promise<{ actor: FactoryActor }> { return this.request("/login", { username, password }); }
  logout(): Promise<{ ok: boolean }> { return this.request("/logout", {}); }
  workspace(): Promise<FactoryWorkspace> { return this.request("/workspace"); }
  versions(): Promise<{ revisions: number[] }> { return this.request("/versions"); }
  version(revision: number): Promise<FactoryWorkspace> { return this.request("/versions/" + revision); }
  releaseComparison(releaseId: string, against?: string): Promise<FactoryReleaseComparison> {
    return this.request("/releases/" + encodeURIComponent(releaseId) + "/diff" + (against === undefined ? "" : "?against=" + encodeURIComponent(against)));
  }
  people(): Promise<{ people: FactoryActor[] }> { return this.request("/people"); }
  transaction(command: FactoryCommand, revision: number): FactoryMutationRequest {
    return { command, expectedRevision: revision, idempotencyKey: crypto.randomUUID() };
  }
  mutate(request: FactoryMutationRequest): Promise<FactoryMutationResponse> { return this.request("/commands", request); }
  users(): Promise<{ users: FactoryAccount[] }> { return this.request("/users"); }
  saveUser(input: { id?: string; username: string; name: string; roles: FactoryRole[]; password?: string; enabled: boolean }): Promise<{ users: FactoryAccount[] }> { return this.request("/users", input); }
}
