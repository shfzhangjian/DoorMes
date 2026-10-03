import { describe, expect, it } from "vitest";
import { createEmptyDesign } from "@doormes/domain";
import { FACTORY_SCHEMA, type FactoryWorkspace, type FactoryRelease } from "@doormes/contracts/factory-workflow";
import { describeFactoryHistory, factoryComparisonCandidates } from "./factory-history.js";

function fixture(): FactoryWorkspace {
  const release = (id: string, version: number, code = "P1"): FactoryRelease => ({
    id, designId: "D-" + id, productCode: code, name: "产品", drawingNumber: "DM-" + id, version, designVersion: 1,
    document: createEmptyDesign(id), calculation: { fingerprint: id, catalogVersion: "reference", basis: "reference-simulation", lines: [], route: [], processingParameters: [], blockingCodes: [], productionEligible: false },
    reviews: [], publishedBy: "designer", publishedAt: "2026-10-02T01:00:00.000Z", usage: "prototype", productionEligible: false
  });
  const first = release("R1", 1), second = release("R2", 2), branch = release("R3", 3);
  first.scope = { kind: "catalog" }; second.supersedes = first.id; second.scope = { kind: "catalog" };
  branch.supersedes = first.id; branch.scope = { kind: "order-line", orderId: "O1", lineId: "L1" };
  return {
    schemaVersion: FACTORY_SCHEMA, organizationId: "factory-prototype", revision: 7, savedAt: "2026-10-02T02:00:00.000Z",
    designs: [], releases: [first, second, branch, release("OTHER", 1, "P2")], engineeringRequests: [], receipts: [], deviations: [], executions: [],
    orders: [{ id: "O1", number: "SO-OLD", customer: "历史客户", revision: 1, status: "in-production", createdBy: "sales", updatedAt: "2026-10-02T02:00:00.000Z", lines: [
      { id: "L1", mark: "C1", kind: "standard", quantity: 2, releaseId: first.id, customerConfirmedReleaseId: first.id, requirement: { widthMm: 1200, heightMm: 1500, material: "AL70", glass: "G1", hardware: "H1", finish: "白色", dueDate: "", note: "原需求" } }
    ] }],
    packages: [{ id: "PK1", orderId: "O1", orderRevision: 1, lineId: "L1", releaseId: first.id, version: 1, quantity: 2, batch: "B1", bom: [], route: [], pieceNumbers: ["P-01", "P-02"], productionEligible: false, status: "acknowledged", reviews: [], createdAt: "2026-10-02T02:00:00.000Z", createdBy: "production", processingParameters: [] }],
    changes: [{ id: "CH1", orderId: "O1", lineId: "L1", basePackageId: "PK1", baseReleaseId: first.id, designId: "D-R3", reason: "局部变更", createdBy: "sales", createdAt: "2026-10-02T02:00:00.000Z", status: "acknowledged", newReleaseId: branch.id, differences: [], impact: { executionIds: [], procurement: "", cost: "", delivery: "" }, effectivity: { batch: "B1-CH", fromStep: "CUT", pieceNumbers: ["P-01"] }, disposition: "use-as-is", reviews: [] }],
    audit: [
      { id: "E1", actorId: "sales", at: "2026-10-02T01:00:00.000Z", action: "create-order", targetId: "O1", workspaceRevision: 1, note: "建立需求" },
      { id: "E2", actorId: "production", at: "2026-10-02T02:00:00.000Z", action: "issue-change", targetId: "CH1", workspaceRevision: 7, note: "局部切换" }
    ]
  };
}
describe("read-only archived factory history", () => {
  it("resolves order and frozen manufacturing labels against the selected archive, not today's releases", () => {
    const archived = fixture(), today = structuredClone(archived);
    today.releases[0]!.drawingNumber = "DM-CURRENT"; today.orders[0]!.number = "SO-CURRENT";
    today.orders[0]!.lines[0]!.releaseId = "R3"; today.packages[0]!.releaseId = "R3";
    const model = describeFactoryHistory(archived);
    expect(model.orders[0]).toMatchObject({ order: "SO-OLD", release: "DM-R1 V1", confirmed: "DM-R1 V1" });
    expect(model.packages[0]).toMatchObject({ order: "SO-OLD", release: "DM-R1 V1", originalPieces: ["P-01", "P-02"], effectivePieces: ["P-02"] });
    expect(describeFactoryHistory(today).packages[0]!.release).toBe("DM-R3 V3");
  });
  it("does not mutate archives and exposes detached demand, pieces and event summaries", () => {
    const archived = fixture(), before = JSON.stringify(archived), model = describeFactoryHistory(archived);
    expect(model.events.map((entry) => entry.id)).toEqual(["E2", "E1"]);
    model.orders[0]!.requirement.note = "仅修改摘要"; model.packages[0]!.originalPieces.pop(); model.packages[0]!.effectivePieces.pop(); model.events[0]!.note = "仅修改摘要";
    expect(JSON.stringify(archived)).toBe(before);
  });
  it("does not invent a previous numeric version when an order branch inherits an earlier catalog baseline", () => {
    const archived = fixture(), before = JSON.stringify(archived);
    const candidates = factoryComparisonCandidates(archived, "R3");
    expect(candidates.map((entry) => entry.id)).toEqual(["R2", "R1"]);
    expect(archived.releases[2]!.supersedes).toBe("R1");
    expect(describeFactoryHistory(archived).releases[2]!.scope).toEqual({ kind: "order-line", orderId: "O1", lineId: "L1" });
    expect(JSON.stringify(archived)).toBe(before);
    expect(factoryComparisonCandidates(archived, "missing")).toEqual([]);
  });
  it("preserves pending or withdrawn change pieces in the old manufacturing baseline", () => {
    const archived = fixture();
    for (const status of ["designing", "impact-review", "approved", "cancelled"] as const) {
      archived.changes[0]!.status = status;
      expect(describeFactoryHistory(archived).packages[0]!.effectivePieces).toEqual(["P-01", "P-02"]);
    }
    for (const status of ["issued", "acknowledged", "closed"] as const) {
      archived.changes[0]!.status = status;
      expect(describeFactoryHistory(archived).packages[0]!.effectivePieces).toEqual(["P-02"]);
    }
  });
});
