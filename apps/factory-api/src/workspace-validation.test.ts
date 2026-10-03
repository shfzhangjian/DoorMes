import { describe, expect, it } from "vitest";
import { createEmptyDesign } from "@doormes/domain";
import { createRectangularWindowCommand, DesignSession } from "@doormes/application";
import { DESIGN_REVIEW_ROLES, type FactoryRole, type FactoryCommand, type FactoryWorkspace } from "@doormes/contracts/factory-workflow";
import { applyFactoryCommand, createFactoryWorkspace } from "./workflow.js";
import { validateFactoryWorkspace } from "./workspace-validation.js";

function fixture(): FactoryWorkspace {
  const state = createFactoryWorkspace();
  const run = (role: FactoryRole, command: FactoryCommand) => {
    const id = applyFactoryCommand(state, { id: "user-" + role, name: role, roles: [role], organizationId: state.organizationId }, command);
    validateFactoryWorkspace(state); return id;
  };
  const session = new DesignSession(createEmptyDesign("VALIDATION"));
  session.execute(createRectangularWindowCommand({ commandId: "CREATE", windowId: "W1", mark: "C1", widthMm: 1200, heightMm: 1500, profileSystemId: "AL70", defaultGlassTypeId: "GL-LOWE-24", defaultHardwareSetId: "HW-TT-STD" }));
  const designId = run("designer", { type: "save-design", name: "标准窗", drawingNumber: "DM-VALIDATION", document: session.document });
  run("designer", { type: "submit-design", designId });
  for (const role of DESIGN_REVIEW_ROLES) run(role, { type: "review-design", designId, role, decision: "approve", note: "核对" });
  const releaseId = run("designer", { type: "release-design", designId, productCode: "PRODUCT-1" });
  const orderId = run("sales", { type: "create-order", number: "ORDER-1", customer: "客户", kind: "standard", quantity: 2, releaseId, requirement: { widthMm: 1200, heightMm: 1500, material: "AL70", glass: "GL-LOWE-24", hardware: "HW-TT-STD", finish: "内RAL9016 / 外RAL7016", dueDate: "", note: "" } });
  run("sales", { type: "confirm-order", orderId });
  const packageId = run("production", { type: "create-package", orderId, lineId: state.orders[0]!.lines[0]!.id, batch: "B1" });
  for (const role of ["process", "procurement", "quality"] as const) run(role, { type: "review-package", packageId, role, decision: "approve", note: "核对" });
  run("production", { type: "issue-package", packageId }); run("operator", { type: "acknowledge-package", packageId });
  const packet = state.packages[0]!;
  run("operator", { type: "record-execution", packageId, stepId: "MATERIAL", kind: "material", quantity: 2, pieceNumbers: packet.pieceNumbers, result: "recorded", actual: "齐套" });
  return state;
}
describe("factory workspace JSON integrity", () => {
  it("accepts full states and reads legacy optional release metadata without rewriting", () => {
    const state = fixture(); delete state.releases[0]!.scope; delete state.releases[0]!.specification;
    const before = JSON.stringify(state); validateFactoryWorkspace(state); expect(JSON.stringify(state)).toBe(before);
  });
  it("rejects broken quantity chains and cancelled packages with actual production", () => {
    const state = fixture(), order = state.orders[0]!, line = order.lines[0]!;
    const run = (quantity: number) => applyFactoryCommand(state, { id: "user-sales", name: "sales", roles: ["sales"], organizationId: state.organizationId }, { type: "revise-order-quantity", orderId: order.id, lineId: line.id, quantity, note: "客户确认" });
    run(3); run(4); validateFactoryWorkspace(state);
    for (const corrupt of [
      (value: FactoryWorkspace) => { value.orders[0]!.quantityRevisions![1]!.fromQuantity = 2; },
      (value: FactoryWorkspace) => { value.orders[0]!.quantityRevisions![1]!.toQuantity = 5; },
      (value: FactoryWorkspace) => { value.orders[0]!.quantityRevisions![0]!.lineId = "MISSING"; },
      (value: FactoryWorkspace) => { value.orders[0]!.quantityRevisions![0]!.toRevision = 100; },
      (value: FactoryWorkspace) => { const packet = value.packages[0]!; packet.status = "cancelled"; packet.cancellation = { previousStatus: "approved", note: "假撤销", actorId: "production", at: value.savedAt, orderRevision: order.revision }; },
      (value: FactoryWorkspace) => { value.packages[0]!.status = "cancelled"; }
    ]) {
      const corruptState = structuredClone(state); corrupt(corruptState); const before = JSON.stringify(corruptState);
      expect(() => validateFactoryWorkspace(corruptState)).toThrow("后端业务文件内容无效");
      expect(JSON.stringify(corruptState)).toBe(before);
    }
  });
  const cases: [string, (state: FactoryWorkspace) => void][] = [
    ["illegal enum", (state) => { (state.packages[0] as unknown as { status: string }).status = "published"; }],
    ["missing fields", (state) => { delete (state.executions[0] as Partial<FactoryWorkspace["executions"][number]>).pieceNumbers; }],
    ["formal drawing", (state) => { (state.releases[0]!.document.windows[0] as unknown as { widthMm: number }).widthMm = -1; }],
    ["prototype promotion", (state) => { (state.releases[0] as unknown as { productionEligible: boolean }).productionEligible = true; }],
    ["duplicate IDs", (state) => { state.designs.push(structuredClone(state.designs[0]!)); }],
    ["foreign order line", (state) => { state.packages[0]!.lineId = "missing"; }],
    ["dangling release", (state) => { state.orders[0]!.lines[0]!.releaseId = "missing"; }],
    ["wrong scope", (state) => { state.releases[0]!.scope = { kind: "order-line", orderId: "missing", lineId: "missing" }; }],
    ["release cycle", (state) => { state.releases[0]!.supersedes = state.releases[0]!.id; }],
    ["missing approval", (state) => { state.releases[0]!.reviews.pop(); }],
    ["duplicate role", (state) => { state.releases[0]!.reviews.push(structuredClone(state.releases[0]!.reviews[0]!)); }],
    ["wrong approved revision", (state) => { state.releases[0]!.reviews[0]!.revision = 99; }],
    ["self approval", (state) => { state.releases[0]!.reviews[0]!.actorId = state.designs[0]!.submittedBy!; }],
    ["modified frozen BOM", (state) => { state.packages[0]!.bom[0]!.lengthMm += 1; }],
    ["modified frozen route", (state) => { state.packages[0]!.route[0]!.instruction = "另一个版本"; }],
    ["missing package approval", (state) => { state.packages[0]!.reviews.pop(); }],
    ["piece count", (state) => { state.packages[0]!.pieceNumbers.pop(); }],
    ["over allocation", (state) => { state.orders[0]!.lines[0]!.quantity = 1; }],
    ["unknown execution step", (state) => { state.executions[0]!.stepId = "foreign"; }],
    ["foreign execution batch", (state) => { state.executions[0]!.batch = "B2"; }],
    ["false execution quantity", (state) => { state.executions[0]!.quantity = 1; }],
    ["foreign execution piece", (state) => { state.executions[0]!.pieceNumbers[0] = "foreign"; }],
    ["future audit", (state) => { state.audit[0]!.workspaceRevision = state.revision + 1; }],
    ["unknown receipt target", (state) => { state.receipts.push({ key: "retry", actorId: "user-designer", digest: "a".repeat(64), targetId: "foreign" }); }]
  ];
  it.each(cases)("rejects %s without repairing input", (_name, corrupt) => {
    const state = fixture(); corrupt(state); const before = JSON.stringify(state);
    expect(() => validateFactoryWorkspace(state)).toThrow("后端业务文件内容无效");
    expect(JSON.stringify(state)).toBe(before);
  });
});
