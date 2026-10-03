import { afterEach, describe, expect, it } from "vitest";
import { createHash } from "node:crypto";
import { mkdtemp, readFile, writeFile, rm } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { createEmptyDesign } from "@doormes/domain";
import { FileFactoryStore } from "./store.js";

const directories: string[] = [], password = "Factory-validation-test-2026!";
afterEach(async () => {
  // Exact test-owned mkdtemp directories only; no user data is removed.
  for (const directory of directories.splice(0)) await rm(directory, { recursive: true, force: true });
});
async function fixture() {
  const directory = await mkdtemp(join(tmpdir(), "doormes-integrity-")); directories.push(directory);
  const store = new FileFactoryStore(directory, password), { actor } = await store.login("designer", password, "fixture");
  await store.mutate(actor, { expectedRevision: 0, idempotencyKey: "SAVE", command: { type: "save-design", name: "文件校验", drawingNumber: "CHECK-01", document: createEmptyDesign("CHECK") } });
  return { directory, store, path: join(directory, "workspace.json") };
}
describe("factory files fail closed without overwriting evidence", () => {
  it("rejects malformed but correctly checksummed workspace fields and dangling business references", async () => {
    const f = await fixture(), original = JSON.parse(await readFile(f.path, "utf8"));
    const corruptions = [
      (file: typeof original) => { file.workspace.designs[0].status = "invented"; },
      (file: typeof original) => { file.workspace.designs[0].baseReleaseId = "foreign"; },
      (file: typeof original) => { file.workspace.designs[0].document.windows = "invalid"; },
      (file: typeof original) => { file.workspace.receipts[0].targetId = "foreign"; },
      (file: typeof original) => { file.workspace.audit[0].workspaceRevision = 99; },
      (file: typeof original) => { file.workspace.unrecognizedOverride = true; }
    ];
    for (const corrupt of corruptions) {
      const file = structuredClone(original); corrupt(file);
      file.checksum = createHash("sha256").update(JSON.stringify(file.workspace)).digest("hex");
      const bytes = JSON.stringify(file); await writeFile(f.path, bytes);
      await expect(new FileFactoryStore(f.directory, password).read()).rejects.toMatchObject({ code: "CORRUPT_FILE", status: 500 });
      expect(await readFile(f.path, "utf8")).toBe(bytes);
    }
  });
  it("rejects mismatched history filenames on reads and startup replay without replacing head", async () => {
    const f = await fixture(), head = await readFile(f.path, "utf8");
    await writeFile(join(f.directory, "versions", "r00000000.json"), head);
    await expect(f.store.version(0)).rejects.toMatchObject({ code: "CORRUPT_FILE" });
    await writeFile(join(f.directory, "versions", "r00000002.json"), head);
    await expect(new FileFactoryStore(f.directory, password).read()).rejects.toMatchObject({ code: "CORRUPT_FILE" });
    expect(await readFile(f.path, "utf8")).toBe(head);
  });
  it("rejects ambiguous or structurally invalid accounts instead of selecting the first duplicate", async () => {
    const f = await fixture(), path = join(f.directory, "users.json"), original = JSON.parse(await readFile(path, "utf8"));
    const corruptions = [
      (file: typeof original) => { file.users[1].id = file.users[0].id; },
      (file: typeof original) => { file.users[1].username = file.users[0].username; },
      (file: typeof original) => { file.users[1].roles = "designer"; },
      (file: typeof original) => { file.users[1].roles = ["designer", "designer"]; },
      (file: typeof original) => { file.users[1].enabled = "yes"; },
      (file: typeof original) => { delete file.users[1].passwordHash; }
    ];
    for (const corrupt of corruptions) {
      const file = structuredClone(original); corrupt(file); const bytes = JSON.stringify(file); await writeFile(path, bytes);
      await expect(new FileFactoryStore(f.directory, password).read()).rejects.toMatchObject({ code: "CORRUPT_FILE" });
      expect(await readFile(path, "utf8")).toBe(bytes);
    }
  });
});
