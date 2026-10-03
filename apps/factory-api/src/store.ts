import { createHash, randomUUID, scryptSync, timingSafeEqual } from "node:crypto";
import { mkdir, readFile, rename, writeFile, readdir } from "node:fs/promises";
import { resolve, join } from "node:path";
import Ajv from "ajv";
import { FACTORY_MUTATION_JSON_SCHEMA } from "@doormes/contracts/factory-command-schema";
import { FACTORY_USERS_JSON_SCHEMA } from "@doormes/contracts/factory-workspace-schema";
import { FACTORY_ROLES, FACTORY_ROLE_LABELS, FACTORY_SCHEMA, type FactoryActor, type FactoryRole, type FactoryWorkspace, type FactoryMutationRequest, type FactoryMutationResponse } from "@doormes/contracts/factory-workflow";
import { validateFactoryWorkspace } from "./workspace-validation.js";
import { applyFactoryCommand, check, createFactoryWorkspace, FactoryError, requiredText } from "./workflow.js";

interface StoredUser extends FactoryActor { username: string; salt: string; passwordHash: string; enabled: boolean }
interface FileEnvelope { schemaVersion: "doormes-factory-file.v1"; checksum: string; workspace: FactoryWorkspace }
export interface FactoryPublicUser extends FactoryActor { username: string; enabled: boolean }
function hash(value: unknown): string { return createHash("sha256").update(JSON.stringify(value)).digest("hex"); }
function userView(user: StoredUser): FactoryPublicUser { return { id: user.id, name: user.name, roles: [...user.roles], organizationId: user.organizationId, username: user.username, enabled: user.enabled }; }
const validateMutation = new Ajv({ allErrors: false }).compile(FACTORY_MUTATION_JSON_SCHEMA);
const validateUsers = new Ajv({ allErrors: false }).compile(FACTORY_USERS_JSON_SCHEMA);
/** Sibling temp + rename prevents half-written JSON from replacing the baseline. */
async function atomicJson(path: string, value: unknown): Promise<void> {
  const temporary = `${path}.${randomUUID()}.tmp`;
  await writeFile(temporary, `${JSON.stringify(value, null, 2)}\n`, { encoding: "utf8", flag: "wx", mode: 0o600 });
  await rename(temporary, path);
}
function decodeFile(value: unknown): FactoryWorkspace {
  check(value && typeof value === "object" && !Array.isArray(value), "后端文件格式无效，原文件未被覆盖", "CORRUPT_FILE", 500);
  const envelope = value as FileEnvelope;
  check(envelope.schemaVersion === "doormes-factory-file.v1" && envelope.workspace?.schemaVersion === FACTORY_SCHEMA && envelope.checksum === hash(envelope.workspace), "后端文件校验失败，原文件未被覆盖", "CORRUPT_FILE", 500);
  validateFactoryWorkspace(envelope.workspace);
  return envelope.workspace;
}

/** One process serializes every write, checks optimistic revisions, and archives all commits. */
export class FileFactoryStore {
  readonly directory: string;
  #state = createFactoryWorkspace();
  #users: StoredUser[] = [];
  #ready: Promise<void>;
  #queue: Promise<unknown> = Promise.resolve();
  #sessions = new Map<string, { userId: string; expiresAt: number }>();
  #loginAttempts = new Map<string, { count: number; expiresAt: number }>();
  constructor(directory: string, initialPassword = "DoorMes-demo-2026!") {
    this.directory = resolve(directory);
    this.#ready = this.#initialize(initialPassword);
    // Keep failures visible through API calls without an unhandled startup rejection.
    void this.#ready.catch(() => undefined);
  }
  async #initialize(initialPassword: string): Promise<void> {
    await mkdir(join(this.directory, "versions"), { recursive: true });
    try { this.#state = decodeFile(JSON.parse(await readFile(join(this.directory, "workspace.json"), "utf8"))); }
    catch (error) {
      if ((error as NodeJS.ErrnoException).code !== "ENOENT") throw error;
      await this.#persist(this.#state);
    }
    await this.#recoverArchive();
    try {
      const saved = JSON.parse(await readFile(join(this.directory, "users.json"), "utf8")) as { schemaVersion: string; users: StoredUser[] };
      check(validateUsers(saved) && saved.users.some((user) => user.enabled && user.roles.includes("admin")), "账号文件无效", "CORRUPT_FILE", 500);
      check(saved.users.every((user) => user.organizationId === this.#state.organizationId) && new Set(saved.users.map((user) => user.id)).size === saved.users.length && new Set(saved.users.map((user) => user.username)).size === saved.users.length, "账号文件组织或编号冲突", "CORRUPT_FILE", 500);
      this.#users = saved.users;
    } catch (error) {
      if ((error as NodeJS.ErrnoException).code !== "ENOENT") throw error;
      check(initialPassword.length >= 10, "原型初始密码至少10位", "INVALID_CONFIG", 500);
      this.#users = FACTORY_ROLES.map((role) => {
        const salt = randomUUID();
        return { id: `user-${role}`, username: role, name: FACTORY_ROLE_LABELS[role], roles: [role], organizationId: this.#state.organizationId, salt, passwordHash: scryptSync(initialPassword, salt, 64).toString("hex"), enabled: true };
      });
      await this.#persistUsers();
    }
  }
  async #persist(state: FactoryWorkspace): Promise<void> {
    validateFactoryWorkspace(state);
    const envelope: FileEnvelope = { schemaVersion: "doormes-factory-file.v1", checksum: hash(state), workspace: state };
    const versionPath = join(this.directory, "versions", `r${String(state.revision).padStart(8, "0")}.json`);
    // Archives are immutable. A retry after a failed head write must have identical bytes.
    try { await writeFile(versionPath, `${JSON.stringify(envelope, null, 2)}\n`, { encoding: "utf8", flag: "wx", mode: 0o600 }); }
    catch (error) {
      if ((error as NodeJS.ErrnoException).code !== "EEXIST") throw error;
      const previous = JSON.parse(await readFile(versionPath, "utf8")) as FileEnvelope;
      check(previous.checksum === envelope.checksum, "历史版本不可覆盖", "VERSION_COLLISION", 500);
    }
    await atomicJson(join(this.directory, "workspace.json"), envelope);
  }
  /** A complete immutable journal commit survives interruption before head rename. */
  async #recoverArchive(): Promise<void> {
    const names = (await readdir(join(this.directory, "versions"))).filter((name) => /^r\d{8}\.json$/u.test(name)).sort();
    for (const name of names) {
      const revision = Number(name.slice(1, -5));
      if (revision <= this.#state.revision) continue;
      check(revision === this.#state.revision + 1, "历史提交不连续，请保留文件并检查", "CORRUPT_FILE", 500);
      const recovered = decodeFile(JSON.parse(await readFile(join(this.directory, "versions", name), "utf8")));
      check(recovered.revision === revision, "历史文件名与内容修订不一致", "CORRUPT_FILE", 500);
      this.#state = recovered;
    }
    await atomicJson(join(this.directory, "workspace.json"), { schemaVersion: "doormes-factory-file.v1", checksum: hash(this.#state), workspace: this.#state });
  }
  async #persistUsers(): Promise<void> { await atomicJson(join(this.directory, "users.json"), { schemaVersion: "doormes-factory-users.v1", users: this.#users }); }
  async read(): Promise<FactoryWorkspace> { await this.#ready; await this.#queue; return structuredClone(this.#state); }
  async versions(): Promise<number[]> {
    await this.#ready;
    return (await readdir(join(this.directory, "versions"))).flatMap((name) => /^r\d{8}\.json$/u.test(name) ? [Number(name.slice(1, -5))] : []).sort((a, b) => b - a);
  }
  async version(revision: number): Promise<FactoryWorkspace> {
    await this.#ready; check(Number.isSafeInteger(revision) && revision >= 0, "历史版本无效", "INVALID_INPUT", 400);
    try {
      const state = decodeFile(JSON.parse(await readFile(join(this.directory, "versions", `r${String(revision).padStart(8, "0")}.json`), "utf8")));
      check(state.revision === revision, "历史文件名与内容修订不一致", "CORRUPT_FILE", 500); return state;
    }
    catch (error) { if ((error as NodeJS.ErrnoException).code === "ENOENT") throw new FactoryError(404, "NOT_FOUND", "历史版本不存在"); throw error; }
  }
  async mutate(actor: FactoryActor, request: FactoryMutationRequest): Promise<FactoryMutationResponse> {
    await this.#ready;
    check(validateMutation(request), "命令JSON字段格式无效，请按 /api/factory/schema 提交", "INVALID_INPUT", 400);
    const task = this.#queue.then(async () => {
      const current = this.#users.find((entry) => entry.id === actor.id && entry.enabled);
      check(current, "账号已停用", "FORBIDDEN", 403); actor = userView(current);
      check(actor.organizationId === this.#state.organizationId, "不能访问其他组织", "FORBIDDEN", 403);
      check(request && Number.isSafeInteger(request.expectedRevision), "缺少有效基础修订", "INVALID_INPUT", 400);
      const key = requiredText(request.idempotencyKey, "幂等键", 100), digest = hash(request.command);
      const receipt = this.#state.receipts.find((entry) => entry.actorId === actor.id && entry.key === key);
      if (receipt) {
        check(receipt.digest === digest, "相同幂等键不能执行不同命令", "IDEMPOTENCY_CONFLICT", 409);
        return { workspace: structuredClone(this.#state), targetId: receipt.targetId, replayed: true };
      }
      check(request.expectedRevision === this.#state.revision, "数据已被其他岗位更新，请刷新后重试", "REVISION_CONFLICT", 409);
      const next = structuredClone(this.#state);
      const targetId = applyFactoryCommand(next, actor, request.command, undefined, this.#users.filter((user) => user.enabled).map(userView));
      next.receipts.push({ key, actorId: actor.id, digest, targetId });
      try { await this.#persist(next); this.#state = next; }
      catch (error) { await this.#recoverArchive(); throw error; }
      return { workspace: structuredClone(next), targetId, replayed: false };
    });
    this.#queue = task.catch(() => undefined); return task;
  }
  async login(username: unknown, password: unknown, remoteAddress: string): Promise<{ actor: FactoryActor; token: string }> {
    await this.#ready;
    const normalized = requiredText(username, "用户名", 100), value = requiredText(password, "密码", 256);
    const attempt = this.#loginAttempts.get(remoteAddress);
    if (attempt && attempt.expiresAt > Date.now()) check(attempt.count < 25, "登录尝试过多，请稍后再试", "RATE_LIMIT", 429);
    else this.#loginAttempts.set(remoteAddress, { count: 0, expiresAt: Date.now() + 60000 });
    const user = this.#users.find((entry) => entry.username === normalized && entry.enabled);
    const candidate = scryptSync(value, user?.salt ?? "unknown-factory-user", 64);
    if (!user || !timingSafeEqual(candidate, Buffer.from(user.passwordHash, "hex"))) {
      this.#loginAttempts.get(remoteAddress)!.count++; throw new FactoryError(401, "LOGIN_FAILED", "用户名或密码错误");
    }
    const token = `${randomUUID()}${randomUUID()}`;
    this.#sessions.set(token, { userId: user.id, expiresAt: Date.now() + 8 * 3600000 }); return { actor: userView(user), token };
  }
  async actor(token: string | undefined): Promise<FactoryActor> {
    await this.#ready; const session = token ? this.#sessions.get(token) : undefined;
    const user = session ? this.#users.find((entry) => entry.id === session.userId && entry.enabled) : undefined;
    check(session && user && session.expiresAt > Date.now(), "请登录工厂工作台", "UNAUTHENTICATED", 401); return userView(user);
  }
  logout(token: string | undefined): void { if (token) this.#sessions.delete(token); }
  async users(actor: FactoryActor): Promise<FactoryPublicUser[]> {
    await this.#ready; check(actor.roles.includes("admin"), "仅管理员可维护账号", "FORBIDDEN", 403); return this.#users.map(userView);
  }
  async people(actor: FactoryActor): Promise<FactoryActor[]> {
    await this.#ready;
    check(actor.organizationId === this.#state.organizationId, "不能访问其他组织", "FORBIDDEN", 403);
    return this.#users.filter((user) => user.enabled).map((user) => ({ id: user.id, name: user.name, organizationId: user.organizationId, roles: [...user.roles] }));
  }
  async saveUser(actor: FactoryActor, input: { id?: string; username: string; name: string; roles: FactoryRole[]; password?: string; enabled: boolean }): Promise<FactoryPublicUser[]> {
    await this.#ready; check(actor.roles.includes("admin"), "仅管理员可维护账号", "FORBIDDEN", 403);
    const task = this.#queue.then(async () => {
      check(this.#users.some((entry) => entry.id === actor.id && entry.enabled && entry.roles.includes("admin")), "管理员权限已失效", "FORBIDDEN", 403);
      const username = requiredText(input.username, "用户名", 100), name = requiredText(input.name, "姓名", 100);
      check(Array.isArray(input.roles) && input.roles.length > 0 && input.roles.every((role) => FACTORY_ROLES.includes(role)) && typeof input.enabled === "boolean", "账号角色或状态无效", "INVALID_INPUT", 400);
      check(!this.#users.some((entry) => entry.username === username && entry.id !== input.id), "用户名重复");
      const existing = input.id ? this.#users.find((entry) => entry.id === input.id) : undefined; check(!input.id || existing, "账号不存在", "NOT_FOUND", 404);
      check(existing || input.password, "新账号必须设置密码", "INVALID_INPUT", 400);
      if (input.password) check(input.password.length >= 10 && input.password.length <= 256, "密码长度应为10至256位", "INVALID_INPUT", 400);
      const salt = input.password ? randomUUID() : existing!.salt;
      const user: StoredUser = { id: existing?.id ?? randomUUID(), username, name, organizationId: this.#state.organizationId, roles: [...new Set(input.roles)], enabled: input.enabled, salt, passwordHash: input.password ? scryptSync(input.password, salt, 64).toString("hex") : existing!.passwordHash };
      const previous = this.#users;
      const next = [...previous.filter((entry) => entry.id !== user.id), user];
      check(next.some((entry) => entry.enabled && entry.roles.includes("admin")), "至少保留一个启用的管理员");
      this.#users = next;
      try { await this.#persistUsers(); } catch (error) { this.#users = previous; throw error; }
      for (const [token, session] of this.#sessions) if (session.userId === user.id) this.#sessions.delete(token);
      return this.#users.map(userView);
    }); this.#queue = task.catch(() => undefined); return task;
  }
}
