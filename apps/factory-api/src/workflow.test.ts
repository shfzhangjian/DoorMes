import { describe, expect, it } from "vitest";
import { createRectangularWindowCommand, createResizeWindowCommand, DesignSession } from "@doormes/application";
import { createEmptyDesign } from "@doormes/domain";
import { DESIGN_REVIEW_ROLES, FACTORY_ROLES, factoryAllocatedQuantity, factoryReleaseScope, factoryEffectivePieceNumbers, type FactoryActor, type FactoryCommand, type FactoryDisposition, type FactoryManufacturingPackage, type FactoryRole } from "@doormes/contracts/factory-workflow";
import { describeFactoryProduct } from "@doormes/application/factory-product";
import { applyFactoryCommand, compareFactoryVersions, createFactoryWorkspace } from "./workflow.js";
import { validateFactoryWorkspace } from "./workspace-validation.js";
import { factoryChangeSchemeIssueBlockers, factoryChangeSchemeStatus } from "@doormes/contracts/factory-production-status";
import { factoryReviewRoles, factoryCanEditDesign } from "../../designer-web/src/factory-ui-policy.js";
import { describeFactoryHistory } from "../../designer-web/src/factory-history.js";

const actor = (role: FactoryRole): FactoryActor => ({ id: "user-" + role, name: role, roles: [role], organizationId: "factory-prototype" });
const demand = { widthMm: 1200, heightMm: 1500, material: "AL70", glass: "GL-LOWE-24", hardware: "HW-TT-STD", finish: "内RAL9016 / 外RAL7016", dueDate: "", note: "图纸按需求设计" };
function fixture() {
  let state = createFactoryWorkspace();
  const secondDesigner = { ...actor("designer"), id: "user-designer-2", name: "第二研发人员" };
  const people = [...FACTORY_ROLES.map(actor), secondDesigner];
  const runAs = (person: FactoryActor, command: FactoryCommand) => { const next = structuredClone(state); const id = applyFactoryCommand(next, person, command, undefined, people); validateFactoryWorkspace(next); state = next; return id; };
  const run = (role: FactoryRole, command: FactoryCommand) => runAs(actor(role), command);
  const session = new DesignSession(createEmptyDesign("FACTORY-TEST"));
  session.execute(createRectangularWindowCommand({ commandId: "CREATE", windowId: "W1", mark: "C1", widthMm: 1200, heightMm: 1500, profileSystemId: "AL70", defaultGlassTypeId: "GL-LOWE-24", defaultHardwareSetId: "HW-TT-STD" }));
  const designId = run("designer", { type: "save-design", name: "标准设计", drawingNumber: "DM-TEST-01", document: session.document });
  const release = (id: string, code = "STANDARD-1") => {
    run("designer", { type: "submit-design", designId: id });
    for (const role of DESIGN_REVIEW_ROLES) run(role, { type: "review-design", designId: id, role, decision: "approve", note: "已核对" });
    return run("designer", { type: "release-design", designId: id, productCode: code });
  };
  const activate = (packageId: string) => {
    for (const role of ["process", "procurement", "quality"] as const) run(role, { type: "review-package", packageId, role, decision: "approve", note: "制造可行性已核对" });
    run("production", { type: "issue-package", packageId }); run("operator", { type: "acknowledge-package", packageId });
    return structuredClone(state.packages.find((entry) => entry.id === packageId)!);
  };
  const issue = (releaseId: string, quantity = 2) => {
    const orderId = run("sales", { type: "create-order", number: "SO-" + state.orders.length, customer: "测试客户", kind: "standard", quantity, requirement: demand, releaseId });
    run("sales", { type: "confirm-order", orderId });
    const lineId = state.orders.find((entry) => entry.id === orderId)!.lines[0]!.id;
    const packageId = run("production", { type: "create-package", orderId, lineId, batch: "BATCH-1" });
    return activate(packageId);
  };
  const executeAll = (packet: FactoryManufacturingPackage, pieceNumbers = packet.pieceNumbers) => {
    for (const step of packet.route) {
      // Pre-cutover steps may be inherited; recording them again is permitted.
      run(step.id === "QUALITY" ? "quality" : "operator", { type: "record-execution", packageId: packet.id, stepId: step.id, kind: step.id === "MATERIAL" ? "material" : step.id === "QUALITY" ? "inspection" : "process", quantity: pieceNumbers.length, pieceNumbers, result: "pass", actual: "本版本逐件核对合格" });
    }
  };
  const prepareChange = (base: FactoryManufacturingPackage, disposition: FactoryDisposition, options: { pieces?: string[]; widthMm?: number; batch?: string } = {}) => {
    const changeId = run("sales", { type: "create-change", packageId: base.id, reason: "测试逐件处置", batch: options.batch ?? "CHANGED", fromStep: "CUT", pieceNumbers: options.pieces ?? factoryEffectivePieceNumbers(state, base), disposition, procurement: "重新核对", cost: "重新核对", delivery: "重新核对" });
    const design = state.designs.find((entry) => entry.id === state.changes.at(-1)!.designId)!;
    const edit = new DesignSession(design.document);
    edit.execute(createResizeWindowCommand({ commandId: "RESIZE", windowId: "W1", widthMm: options.widthMm ?? 1250, heightMm: 1500 }));
    run("designer", { type: "save-design", designId: design.id, drawingNumber: design.drawingNumber, name: "变更尺寸", document: edit.document });
    const releaseId = release(design.id, state.releases.find((entry) => entry.id === base.releaseId)!.productCode);
    run("designer", { type: "submit-change", changeId });
    for (const role of DESIGN_REVIEW_ROLES) run(role, { type: "review-change", changeId, role, decision: "approve", note: "处置计划核对" });
    run("sales", { type: "confirm-customer", orderId: base.orderId, lineId: base.lineId, releaseId });
    return { changeId, releaseId };
  };
  const changePacket = (base: FactoryManufacturingPackage, disposition: FactoryDisposition, options: { pieces?: string[]; widthMm?: number; batch?: string } = {}) => {
    const { changeId } = prepareChange(base, disposition, options);
    run("production", { type: "issue-change", changeId });
    expect(() => run("operator", { type: "acknowledge-package", packageId: state.packages.at(-1)!.id })).toThrow("变更接收");
    run("operator", { type: "acknowledge-change", changeId });
    return { changeId, changed: structuredClone(state.packages.at(-1)!) };
  };
  const confirmDisposition = (changeId: string, recordId: string) => {
    for (const role of ["production", "quality"] as const) run(role, { type: "review-change-disposition", changeId, recordId, role, decision: "approve", note: "逐件证据与去向核对" });
  };
  return { run, runAs, secondDesigner, release, issue, activate, executeAll, prepareChange, changePacket, confirmDisposition, designId, session, get state() { return state; } };
}

function schemeFixture() {
  const f = fixture(), releaseId = f.release(f.designId);
  const orderId = f.run("sales", { type: "create-order", number: "SCHEME", customer: "客户", kind: "standard", quantity: 4, requirement: demand, releaseId });
  f.run("sales", { type: "confirm-order", orderId });
  const lineId = f.state.orders[0]!.lines[0]!.id;
  const bases = ["A", "B"].map((batch) => f.activate(f.run("production", { type: "create-package", orderId, lineId, batch, quantity: 2 })));
  const input: Extract<FactoryCommand, { type: "create-change-scheme" }> = {
    type: "create-change-scheme", name: "同产品两批尺寸变更", reason: "客户修改", procurement: "材料核对", cost: "差额确认", delivery: "交期确认",
    scopes: bases.map((packet, index) => ({ packageId: packet.id, batch: packet.batch, fromStep: index ? "CUT" : "MATERIAL", pieceNumbers: index ? [packet.pieceNumbers[0]!] : [], disposition: index ? "replace" : "use-as-is" }))
  };
  const create = () => f.run("sales", input);
  const publish = (schemeId: string, widthMm = 1250) => {
    const leader = f.state.changes.find((entry) => entry.id === f.state.changeSchemes!.find((entry) => entry.id === schemeId)!.changeIds[0])!;
    const design = f.state.designs.find((entry) => entry.id === leader.designId)!, edit = new DesignSession(design.document);
    edit.execute(createResizeWindowCommand({ commandId: "GROUP-RESIZE", windowId: "W1", widthMm, heightMm: 1500 }));
    f.run("designer", { type: "save-design", designId: design.id, name: design.name, drawingNumber: design.drawingNumber, document: edit.document });
    return f.release(design.id);
  };
  const approve = (schemeId: string) => {
    for (const role of DESIGN_REVIEW_ROLES) f.run(role, { type: "review-change-scheme", schemeId, role, decision: "approve", note: "两批影响共同核对" });
  };
  return { f, bases, input, create, publish, approve, orderId, lineId, releaseId };
}
describe("versioned requirements and R&D re-handoff", () => {
  it("keeps superseded review snapshots read-only, starts a fresh unclaimed task and checks the actual drawing before review", () => {
    const f = fixture(), orderId = f.run("sales", { type: "create-order", number: "REQ-1", customer: "客户", kind: "custom", quantity: 1, requirement: demand });
    f.run("sales", { type: "confirm-order", orderId });
    const originalTask = f.state.engineeringRequests[0]!, lineId = originalTask.lineId;
    f.run("designer", { type: "claim-engineering", requestId: originalTask.id });
    f.run("designer", { type: "submit-design", designId: originalTask.designId });
    f.run("reviewer", { type: "review-design", designId: originalTask.designId, role: "reviewer", decision: "approve", note: "旧需求会签" });
    const oldTask = structuredClone(f.state.engineeringRequests[0]!), oldDesign = structuredClone(f.state.designs.find((entry) => entry.id === oldTask.designId)!);
    const prior = f.state.orders[0]!.revision;
    f.run("sales", { type: "revise-order-requirement", orderId, lineId, kind: "custom", requirement: { ...demand, widthMm: 1250, dueDate: "2026-11-01", note: "新尺寸需求" }, note: "客户调整宽度及交期" });
    const task = f.state.engineeringRequests.at(-1)!, design = f.state.designs.find((entry) => entry.id === task.designId)!;
    expect(task).toMatchObject({ previousRequestId: oldTask.id, status: "unassigned", requirementRevision: 2, orderRevision: prior + 1 });
    expect(task.assigneeId).toBeUndefined(); expect(design.reviews).toEqual([]); expect(design.calculation).toBeUndefined();
    expect(design.document).toEqual(oldDesign.document); // No silent resizing of topology, joints or materials.
    expect(f.state.engineeringRequests[0]).toEqual(oldTask); expect(f.state.designs.find((entry) => entry.id === oldDesign.id)).toEqual(oldDesign);
    expect(factoryCanEditDesign(actor("designer"), f.state, oldDesign)).toBe(false);
    expect(factoryReviewRoles(actor("quality"), f.state, "review-design", oldDesign.id)).toEqual([]);
    const stale: [FactoryRole, FactoryCommand][] = [
      ["designer", { type: "save-design", designId: oldDesign.id, name: oldDesign.name, drawingNumber: oldDesign.drawingNumber, document: oldDesign.document }],
      ["designer", { type: "submit-design", designId: oldDesign.id }],
      ["designer", { type: "release-design", designId: oldDesign.id, productCode: "REQ-P" }],
      ["designer", { type: "claim-engineering", requestId: oldTask.id }],
      ["production", { type: "assign-engineering", requestId: oldTask.id, assigneeId: f.secondDesigner.id, note: "旧任务不可交接" }],
      ["process", { type: "set-design-route", designId: oldDesign.id, route: oldDesign.calculation!.route }],
      ["quality", { type: "review-design", designId: oldDesign.id, role: "quality", decision: "approve", note: "旧需求不能继续" }]
    ];
    for (const [role, command] of stale) { const before = structuredClone(f.state); expect(() => f.run(role, command)).toThrow("需求已修订"); expect(f.state).toEqual(before); }
    expect(() => f.run("designer", { type: "submit-design", designId: design.id })).toThrow("领取");
    f.run("designer", { type: "claim-engineering", requestId: task.id });
    expect(() => f.run("designer", { type: "submit-design", designId: design.id })).toThrow("主规格尚未符合");
    const edit = new DesignSession(design.document), windowId = String(design.document.windows[0]!.objectId);
    edit.execute(createResizeWindowCommand({ commandId: "REQ-RESIZE", windowId, widthMm: 1250, heightMm: 1500 }));
    f.run("designer", { type: "save-design", designId: design.id, name: design.name, drawingNumber: design.drawingNumber, document: edit.document });
    const releaseId = f.release(design.id, "REQ-P");
    expect(() => f.run("production", { type: "create-package", orderId, lineId, batch: "NEW" })).toThrow("客户");
    f.run("sales", { type: "confirm-customer", orderId, lineId, releaseId });
    const packageId = f.run("production", { type: "create-package", orderId, lineId, batch: "NEW" });
    expect(f.state.packages.find((entry) => entry.id === packageId)!.requirementRevision).toBe(2);
    const history = describeFactoryHistory(f.state), before = JSON.stringify(f.state);
    expect(history.orders[0]!.requirementRevisions[0]!.differences.some((entry) => entry.path === "requirement.widthMm" && entry.before === 1200 && entry.after === 1250)).toBe(true);
    history.orders[0]!.requirementRevisions[0]!.before.widthMm = 10;
    expect(JSON.stringify(f.state)).toBe(before);
  });
  it("atomically invalidates all pending batches and preserves their signed BOMs when standard demand becomes custom", () => {
    const f = fixture(), releaseId = f.release(f.designId), released = structuredClone(f.state.releases[0]!);
    const orderId = f.run("sales", { type: "create-order", number: "REQ-STANDARD", customer: "客户", kind: "standard", quantity: 2, requirement: demand, releaseId });
    f.run("sales", { type: "confirm-order", orderId }); const lineId = f.state.orders[0]!.lines[0]!.id;
    const ids = ["A", "B"].map((batch) => f.run("production", { type: "create-package", orderId, lineId, batch, quantity: 1 }));
    for (const role of ["process", "procurement", "quality"] as const) f.run(role, { type: "review-package", packageId: ids[0]!, role, decision: "approve", note: "原需求已核对" });
    const old = structuredClone(f.state.packages), input: FactoryCommand = { type: "revise-order-requirement", orderId, lineId, kind: "custom", requirement: { ...demand, widthMm: 1400, finish: "内RAL9016 / 外RAL9005" }, note: "客户改尺寸和外色" };
    const baseline = JSON.stringify(f.state);
    expect(() => applyFactoryCommand(f.state, actor("designer"), input)).toThrow("岗位"); expect(JSON.stringify(f.state)).toBe(baseline);
    expect(() => applyFactoryCommand(f.state, actor("sales"), { ...input, kind: "standard", releaseId })).toThrow("完整发布规格"); expect(JSON.stringify(f.state)).toBe(baseline);
    expect(() => applyFactoryCommand(f.state, actor("sales"), { ...input, kind: "standard", requirement: demand, releaseId })).toThrow("未改变"); expect(JSON.stringify(f.state)).toBe(baseline);
    const revision = f.state.orders[0]!.revision; f.run("sales", input);
    expect(f.state.orders[0]!.revision).toBe(revision + 1);
    expect(f.state.orders[0]!.requirementRevisions![0]!.cancelledPackageIds).toEqual(ids);
    expect(f.state.orders[0]!.lines[0]).toMatchObject({ kind: "custom", requirementRevision: 2 });
    expect(f.state.orders[0]!.lines[0]!.releaseId).toBeUndefined(); expect(f.state.orders[0]!.lines[0]!.customerConfirmedReleaseId).toBeUndefined();
    f.state.packages.forEach((packet, index) => {
      expect(packet).toMatchObject({ ...old[index], status: "cancelled", cancellation: { previousStatus: old[index]!.status, orderRevision: revision + 1, actorId: "user-sales" } });
      expect(() => f.run("production", { type: "issue-package", packageId: packet.id })).toThrow("会签未通过");
    });
    expect(factoryAllocatedQuantity(f.state.packages, lineId)).toBe(0); expect(f.state.releases[0]).toEqual(released);
    expect(f.state.designs.at(-1)!.baseReleaseId).toBeUndefined(); // Catalog product remains its own immutable branch.
    expect(f.state.engineeringRequests[0]!.referenceReleaseId).toBe(releaseId);
    const corrupt = structuredClone(f.state); corrupt.packages[0]!.status = "approved"; delete corrupt.packages[0]!.cancellation;
    expect(() => validateFactoryWorkspace(corrupt)).toThrow("撤销制造包引用");
  });
  it("revises an order-owned published drawing with its real parent and never carries old customer confirmation forward", () => {
    const f = fixture(), orderId = f.run("sales", { type: "create-order", number: "REQ-PUBLISHED", customer: "客户", kind: "custom", quantity: 1, requirement: demand });
    const original = f.state.engineeringRequests[0]!; f.run("designer", { type: "claim-engineering", requestId: original.id }); f.run("sales", { type: "confirm-order", orderId });
    const releaseId = f.release(original.designId, "REQ-CUSTOM"); f.run("sales", { type: "confirm-customer", orderId, lineId: original.lineId, releaseId });
    const frozen = structuredClone(f.state.releases.at(-1)!);
    f.run("sales", { type: "revise-order-requirement", orderId, lineId: original.lineId, kind: "custom", requirement: { ...demand, widthMm: 1280 }, note: "重新调整需求" });
    const task = f.state.engineeringRequests.at(-1)!, design = f.state.designs.at(-1)!;
    expect(design).toMatchObject({ baseReleaseId: releaseId, drawingNumber: frozen.drawingNumber, status: "draft", reviews: [] });
    expect(() => f.run("sales", { type: "confirm-customer", orderId, lineId: task.lineId, releaseId })).toThrow("有效设计");
    f.run("production", { type: "assign-engineering", requestId: task.id, assigneeId: f.secondDesigner.id, note: "新需求交第二研发" });
    f.runAs(f.secondDesigner, { type: "claim-engineering", requestId: task.id });
    const edit = new DesignSession(design.document); edit.execute(createResizeWindowCommand({ commandId: "EDIT-REQ", windowId: String(edit.document.windows[0]!.objectId), widthMm: 1280, heightMm: 1500 }));
    f.runAs(f.secondDesigner, { type: "save-design", designId: design.id, name: design.name, drawingNumber: design.drawingNumber, document: edit.document });
    f.runAs(f.secondDesigner, { type: "submit-design", designId: design.id });
    for (const role of DESIGN_REVIEW_ROLES) f.run(role, { type: "review-design", designId: design.id, role, decision: "approve", note: "核对新需求" });
    const newId = f.runAs(f.secondDesigner, { type: "release-design", designId: design.id, productCode: "REQ-CUSTOM" });
    expect(f.state.releases.at(-1)).toMatchObject({ id: newId, supersedes: releaseId, version: 2, publishedBy: f.secondDesigner.id });
    expect(f.state.releases.find((entry) => entry.id === releaseId)).toEqual(frozen);
    expect(f.state.orders[0]!.lines[0]!.customerConfirmedReleaseId).toBeUndefined();
  });
  it("retains a consecutive requirement chain across custom/standard transitions, quantity amendments and legacy fields", () => {
    const f = fixture(), releaseId = f.release(f.designId);
    const orderId = f.run("sales", { type: "create-order", number: "REQ-CHAIN", customer: "客户", kind: "custom", quantity: 1, requirement: demand });
    const line = f.state.orders[0]!.lines[0]!, originalTask = f.state.engineeringRequests[0]!;
    delete line.requirementRevision; delete originalTask.requirementRevision; delete originalTask.orderRevision;
    validateFactoryWorkspace(f.state);
    f.run("sales", { type: "revise-order-requirement", orderId, lineId: line.id, kind: "standard", releaseId, requirement: demand, note: "改用现有标准产品" });
    expect(f.state.orders[0]!.lines[0]!.engineeringRequestId).toBeUndefined();
    f.run("sales", { type: "revise-order-quantity", orderId, lineId: line.id, quantity: 2, note: "增加数量" });
    f.run("sales", { type: "revise-order-requirement", orderId, lineId: line.id, kind: "custom", requirement: { ...demand, widthMm: 1250 }, note: "再次转定制" });
    const second = f.state.engineeringRequests.at(-1)!;
    expect(second).toMatchObject({ requirementRevision: 3, previousRequestId: originalTask.id });
    f.run("sales", { type: "revise-order-requirement", orderId, lineId: line.id, kind: "custom", requirement: { ...demand, widthMm: 1280 }, note: "定制尺寸再修订" });
    expect(f.state.engineeringRequests.at(-1)).toMatchObject({ requirementRevision: 4, previousRequestId: second.id });
    expect(f.state.orders[0]!.requirementRevisions!.map((entry) => [entry.fromRequirementRevision, entry.toRequirementRevision])).toEqual([[1, 2], [2, 3], [3, 4]]);
    const variants = [
      (s: typeof f.state) => { s.orders[0]!.requirementRevisions![1]!.before.widthMm = 2; },
      (s: typeof f.state) => { s.orders[0]!.requirementRevisions![0]!.differences[0]!.after = "fake"; },
      (s: typeof f.state) => { s.orders[0]!.lines[0]!.requirementRevision = 2; },
      (s: typeof f.state) => { s.engineeringRequests.at(-1)!.previousRequestId = s.engineeringRequests.at(-1)!.id; },
      (s: typeof f.state) => { s.engineeringRequests[0]!.requirement.note = "改写旧快照"; },
      (s: typeof f.state) => { s.orders[0]!.lines[0]!.engineeringRequestId = originalTask.id; }
    ];
    for (const corrupt of variants) { const state = structuredClone(f.state); corrupt(state); expect(() => validateFactoryWorkspace(state)).toThrow("后端业务文件内容无效"); }
  });
  it("blocks direct requirement changes on issued lines without stopping unissued lines in the same order", () => {
    const f = fixture(), releaseId = f.release(f.designId), orderId = f.run("sales", { type: "create-order", number: "REQ-MIXED", customer: "客户", kind: "standard", quantity: 1, requirement: demand, releaseId });
    f.run("sales", { type: "add-order-line", orderId, kind: "custom", quantity: 1, requirement: demand }); f.run("sales", { type: "confirm-order", orderId });
    const first = f.state.orders[0]!.lines[0]!, second = f.state.orders[0]!.lines[1]!;
    const packet = f.activate(f.run("production", { type: "create-package", orderId, lineId: first.id, batch: "ISSUED" }));
    const input: FactoryCommand = { type: "revise-order-requirement", orderId, lineId: first.id, kind: "custom", requirement: { ...demand, widthMm: 1250 }, note: "不允许直接改已下达需求" };
    const before = JSON.stringify(f.state); expect(() => applyFactoryCommand(f.state, actor("sales"), input)).toThrow("工程变更"); expect(JSON.stringify(f.state)).toBe(before);
    f.run("sales", { ...input, lineId: second.id });
    expect(f.state.orders[0]!.status).toBe("in-production"); expect(f.state.packages.find((entry) => entry.id === packet.id)).toEqual(packet);
    expect(f.state.orders[0]!.lines[0]).toEqual(first); expect(f.state.orders[0]!.lines[1]!.requirementRevision).toBe(2);
  });
});

describe("cross-batch shared engineering change", () => {
  it("shares one drawing and review, blocks child commands and atomically issues only after every impact is fresh", () => {
    const s = schemeFixture(), { f } = s, schemeId = s.create(), newReleaseId = s.publish(schemeId);
    const scheme = f.state.changeSchemes![0]!, leaderId = scheme.changeIds[0]!;
    expect(new Set(f.state.changes.map((entry) => entry.designId)).size).toBe(1);
    expect(f.state.changes.map((entry) => entry.effectivity.pieceNumbers.length)).toEqual([2, 1]);
    expect(f.state.changes.every((entry) => entry.newReleaseId === newReleaseId)).toBe(true);
    f.run("designer", { type: "submit-change-scheme", schemeId });
    expect(factoryReviewRoles(actor("reviewer"), f.state, "review-change", leaderId)).toEqual([]);
    expect(factoryReviewRoles(actor("reviewer"), f.state, "review-change-scheme", schemeId)).toEqual(["reviewer"]);
    expect(factoryReviewRoles({ ...actor("sales"), roles: ["sales", "reviewer"] }, f.state, "review-change-scheme", schemeId)).toEqual([]);
    for (const type of ["submit-change", "refresh-change-impact", "revise-change", "issue-change", "withdraw-change", "review-change"] as const) {
      const before = JSON.stringify(f.state);
      expect(() => f.run(type === "issue-change" ? "production" : type === "review-change" ? "reviewer" : "designer", { type, changeId: leaderId, note: "测试", role: "reviewer", decision: "approve" } as FactoryCommand)).toThrow("统一变更方案");
      expect(JSON.stringify(f.state)).toBe(before);
    }
    s.approve(schemeId);
    expect(factoryChangeSchemeIssueBlockers(f.state, schemeId)[0]!.message).toContain("批次 A");
    f.run("sales", { type: "confirm-customer", orderId: s.orderId, lineId: s.lineId, releaseId: newReleaseId });
    expect(f.state.changes[0]!.customerConfirmation).toEqual(f.state.changes[1]!.customerConfirmation);
    const oldPackets = JSON.stringify(f.state.packages);
    f.run("operator", { type: "record-execution", packageId: s.bases[1]!.id, stepId: "MATERIAL", kind: "material", pieceNumbers: [s.bases[1]!.pieceNumbers[0]!], quantity: 1, result: "pass", actual: "B批新领料" });
    expect(factoryChangeSchemeIssueBlockers(f.state, schemeId).some((entry) => entry.code === "STALE_IMPACT" && entry.message.includes("批次 B"))).toBe(true);
    const detached = structuredClone(f.state), before = JSON.stringify(detached);
    expect(() => applyFactoryCommand(detached, actor("production"), { type: "issue-change-scheme", schemeId })).toThrow("批次 B");
    expect(JSON.stringify(detached)).toBe(before); // even detached application must preflight all batches
    f.run("production", { type: "refresh-change-scheme-impact", schemeId });
    expect(f.state.changes.every((entry) => entry.reviews.length === 0 && entry.status === "impact-review")).toBe(true);
    s.approve(schemeId);
    expect(factoryChangeSchemeIssueBlockers(f.state, schemeId)).toEqual([]);
    const revision = f.state.orders[0]!.revision, fact = JSON.stringify(f.state.executions);
    f.run("production", { type: "issue-change-scheme", schemeId });
    const packets = f.state.changes.map((entry) => f.state.packages.find((packet) => packet.id === entry.newPackageId)!);
    expect(f.state.orders[0]!.revision).toBe(revision + 1);
    expect(packets.every((entry) => entry.orderRevision === revision + 1 && entry.releaseId === newReleaseId)).toBe(true);
    expect(JSON.stringify(f.state.packages.slice(0, 2))).toBe(oldPackets);
    expect(JSON.stringify(f.state.executions)).toBe(fact);
    expect(factoryAllocatedQuantity(f.state.packages, s.lineId)).toBe(4);
    expect(f.state.orders[0]!.lines[0]!.releaseId).toBe(s.releaseId);
    expect(packets[0]!.pieceNumbers).toEqual(s.bases[0]!.pieceNumbers);
    expect(packets[1]!.pieceNumbers).not.toContain(s.bases[1]!.pieceNumbers[0]);
    expect(factoryEffectivePieceNumbers(f.state, s.bases[1]!)).toEqual([s.bases[1]!.pieceNumbers[1]]);
    expect(f.state.audit.at(-1)).toMatchObject({ action: "issue-change-scheme", targetId: schemeId });
    expect(factoryChangeSchemeStatus(f.state, schemeId)).toBe("issued");
    for (const corrupt of [
      (value: typeof f.state) => { value.changes[1]!.status = "approved"; delete value.changes[1]!.newPackageId; },
      (value: typeof f.state) => { value.packages[3]!.orderRevision--; }
    ]) { const bad = structuredClone(f.state); corrupt(bad); expect(() => validateFactoryWorkspace(bad)).toThrow("后端业务文件内容无效"); }
    for (let index = 0; index < 2; index++) {
      const change = f.state.changes[index]!, packet = packets[index]!;
      f.run("operator", { type: "acknowledge-change", changeId: change.id });
      if (!index) expect(factoryChangeSchemeStatus(f.state, schemeId)).toBe("partially-acknowledged");
      f.executeAll(packet);
      const recordId = f.run("operator", { type: "record-change-disposition", changeId: change.id, pieceNumbers: change.effectivity.pieceNumbers, disposition: change.disposition, executionIds: [], replacementPairs: index ? [{ oldPiece: change.effectivity.pieceNumbers[0]!, newPiece: packet.pieceNumbers[0]! }] : [], note: "旧件去向核对" });
      f.confirmDisposition(change.id, recordId); f.run("quality", { type: "close-change", changeId: change.id, note: "逐件验证合格" });
      expect(factoryChangeSchemeStatus(f.state, schemeId)).toBe(index ? "closed" : "partially-closed");
    }
    expect(describeFactoryHistory(f.state).changeSchemes[0]).toMatchObject({ id: schemeId, status: "closed", changeIds: scheme.changeIds });
  });
  it("rejects shared reviews as a unit, forks once on revision and withdraws without deleting authorized facts", () => {
    const s = schemeFixture(), { f } = s, schemeId = s.create(), releaseId = s.publish(schemeId);
    const firstDesign = f.state.changes[0]!.designId;
    f.run("sales", { type: "confirm-customer", orderId: s.orderId, lineId: s.lineId, releaseId });
    f.run("designer", { type: "submit-change-scheme", schemeId });
    f.run("process", { type: "review-change-scheme", schemeId, role: "process", decision: "reject", note: "两批都需重画" });
    expect(f.state.changes.every((entry) => entry.status === "designing" && entry.reviews[0]!.decision === "reject")).toBe(true);
    const count = f.state.designs.length;
    f.run("designer", { type: "revise-change-scheme", schemeId });
    expect(f.state.designs.length).toBe(count + 1);
    expect(f.state.changes.every((entry) => entry.designId !== firstDesign && !entry.newReleaseId && !entry.customerConfirmation)).toBe(true);
    expect(new Set(f.state.changes.map((entry) => entry.designId)).size).toBe(1);
    const deviationId = f.run("operator", { type: "create-deviation", packageId: s.bases[0]!.id, reason: "先行", actual: "实际做法", pieceNumbers: [s.bases[0]!.pieceNumbers[0]!], stepIds: ["MATERIAL"], risk: "风险", control: "隔离核对" });
    for (const role of ["production", "quality"] as const) f.run(role, { type: "review-deviation", deviationId, role, decision: "approve", note: "限范围" });
    f.run("operator", { type: "record-execution", packageId: s.bases[0]!.id, stepId: "MATERIAL", kind: "material", quantity: 1, pieceNumbers: [s.bases[0]!.pieceNumbers[0]!], result: "pass", actual: "实况", deviationId });
    f.run("designer", { type: "link-deviation-change", deviationId, changeId: f.state.changes[0]!.id });
    const facts = JSON.stringify(f.state.executions);
    expect(() => f.run("designer", { type: "withdraw-change-scheme", schemeId, note: "非发起人" })).toThrow("仅发起人");
    f.run("sales", { type: "withdraw-change-scheme", schemeId, note: "客户撤回" });
    expect(factoryChangeSchemeStatus(f.state, schemeId)).toBe("cancelled");
    expect(JSON.stringify(f.state.executions)).toBe(facts);
    expect(f.state.deviations[0]).toMatchObject({ status: "authorized" });
    expect(f.state.deviations[0]!.changeId).toBeUndefined();
    expect(factoryCanEditDesign(actor("designer"), f.state, f.state.designs.at(-1)!)).toBe(false);
    expect(f.state.releases.some((entry) => entry.id === releaseId)).toBe(true);
    expect(() => f.run("designer", { type: "submit-change-scheme", schemeId })).toThrow("请先完成");
    expect(s.create()).not.toBe(schemeId); // cancellation releases reservations, not history
  });
  it("preflights invalid scopes and rejects corrupted group references, approvals or partial issue", () => {
    const s = schemeFixture(), { f } = s;
    const before = JSON.stringify(f.state);
    for (const command of [
      { ...s.input, scopes: [s.input.scopes[0]!] },
      { ...s.input, scopes: [s.input.scopes[0]!, s.input.scopes[0]!] },
      { ...s.input, scopes: [s.input.scopes[0]!, { ...s.input.scopes[1]!, fromStep: "UNKNOWN" }] },
      { ...s.input, scopes: [s.input.scopes[0]!, { ...s.input.scopes[1]!, pieceNumbers: ["UNKNOWN"] }] }
    ]) {
      const detached = structuredClone(f.state);
      expect(() => applyFactoryCommand(detached, actor("sales"), command)).toThrow();
      expect(JSON.stringify(detached)).toBe(before);
    }
    expect(() => f.run("operator", s.input)).toThrow("当前岗位");
    const schemeId = s.create(); s.publish(schemeId); f.run("designer", { type: "submit-change-scheme", schemeId }); s.approve(schemeId);
    for (const corrupt of [
      (value: typeof f.state) => { value.changeSchemes![0]!.changeIds.reverse(); },
      (value: typeof f.state) => { value.changes[1]!.schemeId = "UNKNOWN"; },
      (value: typeof f.state) => { value.changes[1]!.reviews = []; },
      (value: typeof f.state) => { value.changes[1]!.status = "cancelled"; },
      (value: typeof f.state) => { value.changeSchemes!.push({ ...value.changeSchemes![0]!, id: "DUPLICATE" }); },
      (value: typeof f.state) => { delete value.changeSchemes; }
    ]) { const bad = structuredClone(f.state); corrupt(bad); expect(() => validateFactoryWorkspace(bad)).toThrow("后端业务文件内容无效"); }
  });
  it("cannot reserve scopes across orders or reuse a pending group's pieces", () => {
    const s = schemeFixture(), { f } = s, other = f.issue(s.releaseId, 1);
    const before = JSON.stringify(f.state), detached = structuredClone(f.state);
    expect(() => applyFactoryCommand(detached, actor("sales"), { ...s.input, scopes: [s.input.scopes[0]!, { ...s.input.scopes[1]!, packageId: other.id, pieceNumbers: other.pieceNumbers }] })).toThrow("同一订单行");
    expect(JSON.stringify(detached)).toBe(before);
    s.create(); const reserved = JSON.stringify(f.state);
    expect(s.create).toThrow("未结束变更");
    expect(JSON.stringify(f.state)).toBe(reserved);
  });
  it.each([1250, 1300])("compares every real baseline to shared width %s, allowing one batch to have no changes", (widthMm) => {
    const s = schemeFixture(), { f } = s;
    const { changeId, changed } = f.changePacket(s.bases[0]!, "use-as-is", { widthMm: 1250 });
    f.executeAll(changed);
    const recordId = f.run("operator", { type: "record-change-disposition", changeId, pieceNumbers: s.bases[0]!.pieceNumbers, disposition: "use-as-is", executionIds: [], replacementPairs: [], note: "沿用" });
    f.confirmDisposition(changeId, recordId); f.run("quality", { type: "close-change", changeId, note: "完成" });
    const schemeId = f.run("sales", { ...s.input, scopes: [{ ...s.input.scopes[0]!, packageId: changed.id }, s.input.scopes[1]!] });
    const targetId = s.publish(schemeId, widthMm);
    f.run("designer", { type: "submit-change-scheme", schemeId });
    const members = f.state.changes.filter((entry) => entry.schemeId === schemeId);
    expect(members.map((entry) => entry.baseReleaseId)).toEqual([changed.releaseId, s.releaseId]);
    expect(members.map((entry) => entry.differences.find((item) => item.path.endsWith("widthMm") && item.before === (entry.baseReleaseId === s.releaseId ? 1200 : 1250))?.after)).toEqual([widthMm === 1250 ? undefined : widthMm, widthMm]);
    if (widthMm === 1250) expect(members[0]!.differences).toEqual([]);
    expect(f.state.releases.find((entry) => entry.id === targetId)!.supersedes).toBe(changed.releaseId);
    s.approve(schemeId); f.run("sales", { type: "confirm-customer", orderId: s.orderId, lineId: s.lineId, releaseId: targetId });
    f.run("production", { type: "issue-change-scheme", schemeId });
    expect(factoryChangeSchemeStatus(f.state, schemeId)).toBe("issued");
  });
});

describe("factory design → production state machine", () => {
  it("retains quantity revisions without changing standard baselines and rejects occupied reductions", () => {
    const f = fixture(), releaseId = f.release(f.designId);
    const orderId = f.run("sales", { type: "create-order", number: "AMEND", customer: "测试客户", kind: "standard", quantity: 3, requirement: demand, releaseId });
    const lineId = f.state.orders[0]!.lines[0]!.id;
    const originalRelease = JSON.stringify(f.state.releases);
    f.run("sales", { type: "revise-order-quantity", orderId, lineId, quantity: 4, note: "客户增加一套" });
    expect(f.state.orders[0]).toMatchObject({ status: "draft", revision: 2 });
    f.run("sales", { type: "confirm-order", orderId });
    const packageId = f.run("production", { type: "create-package", orderId, lineId, batch: "A", quantity: 2 });
    const frozen = JSON.stringify(f.state.packages);
    f.run("sales", { type: "revise-order-quantity", orderId, lineId, quantity: 2, note: "客户确认只做两套" });
    expect(f.state.orders[0]!.quantityRevisions!.map((entry) => [entry.fromQuantity, entry.toQuantity])).toEqual([[3, 4], [4, 2]]);
    expect(JSON.stringify(f.state.packages)).toBe(frozen);
    expect(JSON.stringify(f.state.releases)).toBe(originalRelease);
    expect(f.state.orders[0]!.lines[0]!.customerConfirmedReleaseId).toBe(releaseId);
    for (const [role, quantity, message] of [["sales", 1, "不能小于"], ["sales", 2, "未改变"], ["sales", 2.5, "无效"], ["production", 3, "当前岗位"], ["admin", 3, "当前岗位"]] as const) {
      const before = JSON.stringify(f.state);
      expect(() => f.run(role, { type: "revise-order-quantity", orderId, lineId, quantity, note: "测试" })).toThrow(message);
      expect(JSON.stringify(f.state)).toBe(before);
    }
    f.activate(packageId);
    f.run("sales", { type: "revise-order-quantity", orderId, lineId, quantity: 3, note: "已生产批次不动，再增加一套" });
    expect(f.state.orders[0]!.status).toBe("in-production");
    const nextId = f.run("production", { type: "create-package", orderId, lineId, batch: "NEXT" });
    for (const role of ["process", "procurement", "quality"] as const) f.run(role, { type: "review-package", packageId: nextId, role, decision: "approve", note: "审核" });
    f.run("production", { type: "issue-package", packageId: nextId });
    expect(f.state.orders[0]!.status).toBe("in-production"); // a newly issued second batch cannot erase ongoing first-batch work
  });
  it("cancels only unissued root allocations, keeps signatures and never reuses old identity", () => {
    const f = fixture(), releaseId = f.release(f.designId);
    const orderId = f.run("sales", { type: "create-order", number: "CANCEL", customer: "客户", kind: "standard", quantity: 2, requirement: demand, releaseId });
    f.run("sales", { type: "confirm-order", orderId }); const lineId = f.state.orders[0]!.lines[0]!.id;
    const packageId = f.run("production", { type: "create-package", orderId, lineId, batch: "OLD" });
    for (const role of ["process", "procurement", "quality"] as const) f.run(role, { type: "review-package", packageId, role, decision: "approve", note: "已核对" });
    const old = structuredClone(f.state.packages[0]!);
    expect(() => f.run("sales", { type: "cancel-package", packageId, note: "不能替生产撤销" })).toThrow("当前岗位");
    f.run("production", { type: "cancel-package", packageId, note: "计划批次调整" });
    const cancelled = f.state.packages[0]!;
    expect(cancelled).toMatchObject({ status: "cancelled", cancellation: { previousStatus: "approved", actorId: "user-production", note: "计划批次调整" } });
    expect(cancelled.bom).toEqual(old.bom); expect(cancelled.reviews).toEqual(old.reviews); expect(cancelled.pieceNumbers).toEqual(old.pieceNumbers);
    expect(factoryAllocatedQuantity(f.state.packages, lineId)).toBe(0); expect(factoryEffectivePieceNumbers(f.state, cancelled)).toEqual([]);
    expect(() => f.run("production", { type: "cancel-package", packageId, note: "重复撤销" })).toThrow("仅未下达");
    expect(() => f.run("production", { type: "issue-package", packageId })).toThrow("会签未通过");
    expect(() => f.run("process", { type: "review-package", packageId, role: "process", decision: "approve", note: "重用" })).toThrow("审核阶段");
    expect(() => f.run("production", { type: "create-package", orderId, lineId, batch: "OLD" })).toThrow("同名");
    f.run("sales", { type: "revise-order-quantity", orderId, lineId, quantity: 1, note: "客户减量" });
    const newId = f.run("production", { type: "create-package", orderId, lineId, batch: "NEW" });
    const current = f.activate(newId); expect(current.pieceNumbers.some((piece) => old.pieceNumbers.includes(piece))).toBe(false);
    expect(() => f.run("production", { type: "cancel-package", packageId: newId, note: "已下达不能直接撤销" })).toThrow("仅未下达");
    f.executeAll(current); f.run("quality", { type: "complete-package", packageId: newId });
    expect(f.state.orders[0]!.status).toBe("complete"); // cancelled batch does not block order completion
  });
  it("reopens completed orders only for additional unallocated demand and preserves completed facts", () => {
    const f = fixture(), releaseId = f.release(f.designId), packet = f.issue(releaseId);
    f.executeAll(packet); f.run("quality", { type: "complete-package", packageId: packet.id });
    const execution = JSON.stringify(f.state.executions), originalPacket = JSON.stringify(f.state.packages[0]);
    f.run("sales", { type: "revise-order-quantity", orderId: packet.orderId, lineId: packet.lineId, quantity: 3, note: "客户追加一套" });
    expect(f.state.orders[0]!.status).toBe("confirmed");
    const extraId = f.run("production", { type: "create-package", orderId: packet.orderId, lineId: packet.lineId, batch: "EXTRA" });
    f.run("production", { type: "cancel-package", packageId: extraId, note: "追加批次未下达，客户取消追加" });
    f.run("sales", { type: "revise-order-quantity", orderId: packet.orderId, lineId: packet.lineId, quantity: 2, note: "客户确认恢复原数量" });
    expect(f.state.orders[0]!.status).toBe("complete");
    expect(JSON.stringify(f.state.executions)).toBe(execution); expect(JSON.stringify(f.state.packages[0])).toBe(originalPacket);
    const { changed } = f.changePacket(packet, "use-as-is");
    expect(() => f.run("production", { type: "cancel-package", packageId: changed.id, note: "不能撤销变更新包" })).toThrow("仅未下达");
  });
  it("validates whole standard bundles including colors and freezes independent order identities", () => {
    const f = fixture();
    f.session.execute(createRectangularWindowCommand({ commandId: "CREATE-SECOND", windowId: "W2", mark: "C2", widthMm: 900, heightMm: 1600, profileSystemId: "AL70", defaultGlassTypeId: "GL-LOWE-24", defaultHardwareSetId: "HW-TURN-STD", colorOutside: "RAL9005" }));
    f.run("designer", { type: "save-design", designId: f.designId, drawingNumber: "DM-TEST-01", name: "独立双窗套件", document: f.session.document });
    const releaseId = f.release(f.designId), pinned = structuredClone(f.state.releases[0]!);
    const specification = describeFactoryProduct(pinned.document);
    expect(specification).toMatchObject({ widthMm: 2100, heightMm: 1600, hardware: "HW-TT-STD / HW-TURN-STD" });
    expect(specification.windows).toHaveLength(2);
    expect(pinned.specification).toEqual(specification);
    const requirement = { ...demand, ...specification };
    // Only the transport demand fields are sent; the full released document stays locked.
    const wanted = { widthMm: requirement.widthMm, heightMm: requirement.heightMm, material: requirement.material, glass: requirement.glass, hardware: requirement.hardware, finish: requirement.finish, dueDate: "", note: "" };
    expect(() => f.run("sales", { type: "create-order", number: "WRONG-FIRST", customer: "客户", kind: "standard", quantity: 1, releaseId, requirement: demand })).toThrow("完整发布规格");
    expect(() => f.run("sales", { type: "create-order", number: "WRONG-COLOR", customer: "客户", kind: "standard", quantity: 1, releaseId, requirement: { ...wanted, finish: demand.finish } })).toThrow("内外颜色");
    for (const [number, quantity] of [["ORDER-A", 2], ["ORDER-B", 3]] as const) {
      const orderId = f.run("sales", { type: "create-order", number, customer: "客户", kind: "standard", quantity, releaseId, requirement: wanted });
      f.run("sales", { type: "confirm-order", orderId });
      f.run("production", { type: "create-package", orderId, lineId: f.state.orders.at(-1)!.lines[0]!.id, batch: "SAME-BATCH" });
    }
    expect(new Set(f.state.packages.flatMap((packet) => packet.pieceNumbers)).size).toBe(5);
    expect(f.state.packages[1]!.bom[0]!.quantity).toBe(pinned.calculation.lines[0]!.quantity * 3);
    expect(f.state.releases[0]).toEqual(pinned);
    f.run("sales", { type: "create-order", number: "CUSTOM-REF", customer: "客户", kind: "custom", quantity: 1, releaseId, requirement: { ...wanted, widthMm: 2300 } });
    const task = f.state.engineeringRequests[0]!, draft = f.state.designs.find((design) => design.id === task.designId)!;
    expect(task).toMatchObject({ referenceReleaseId: releaseId, status: "unassigned" });
    expect(draft.document).toEqual(pinned.document);
  });
  it("assigns, claims and hands over custom R&D tasks without allowing other designers to overwrite them", () => {
    const f = fixture();
    f.run("sales", { type: "create-order", number: "TASK-1", customer: "客户", kind: "custom", quantity: 1, requirement: demand });
    const task = f.state.engineeringRequests[0]!, design = f.state.designs.find((entry) => entry.id === task.designId)!;
    const save = { type: "save-design" as const, designId: design.id, name: "需求图", drawingNumber: design.drawingNumber, document: design.document };
    expect(() => f.run("designer", save)).toThrow("领取");
    expect(() => f.run("sales", { type: "assign-engineering", requestId: task.id, assigneeId: "user-designer", note: "不能越权" })).toThrow("岗位");
    expect(() => f.run("reviewer", { type: "assign-engineering", requestId: task.id, assigneeId: "user-procurement", note: "非研发账号" })).toThrow("研发账号");
    f.run("reviewer", { type: "assign-engineering", requestId: task.id, assigneeId: "user-designer", note: "首轮分派" });
    expect(() => f.runAs(f.secondDesigner, { type: "claim-engineering", requestId: task.id })).toThrow("其他研发");
    f.run("designer", { type: "claim-engineering", requestId: task.id }); f.run("designer", save);
    f.run("designer", { type: "submit-design", designId: design.id });
    expect(() => f.run("production", { type: "assign-engineering", requestId: task.id, assigneeId: f.secondDesigner.id, note: "会签中不能交接" })).toThrow("不可交接");
    f.run("reviewer", { type: "review-design", designId: design.id, role: "reviewer", decision: "reject", note: "调整后交接" });
    f.run("production", { type: "assign-engineering", requestId: task.id, assigneeId: f.secondDesigner.id, note: "交第二研发" });
    expect(() => f.run("designer", save)).toThrow("领取");
    f.runAs(f.secondDesigner, { type: "claim-engineering", requestId: task.id });
    f.runAs(f.secondDesigner, save);
    f.runAs(f.secondDesigner, { type: "submit-design", designId: design.id });
    expect(f.state.designs.find((entry) => entry.id === design.id)!.submittedBy).toBe(f.secondDesigner.id);
    expect(f.state.engineeringRequests[0]).toMatchObject({ status: "in-review", assignedBy: "user-production", assigneeId: f.secondDesigner.id });
    expect(f.state.audit.filter((entry) => entry.targetId === task.id).map((entry) => entry.action)).toEqual(["assign-engineering", "claim-engineering", "assign-engineering", "claim-engineering"]);
  });
  it("enforces roles, unique drawing numbers, review resubmission and immutable released versions", () => {
    const f = fixture(), before = structuredClone(f.state);
    expect(() => f.run("sales", { type: "submit-design", designId: f.designId })).toThrow("岗位");
    expect(f.state).toEqual(before);
    expect(() => f.run("designer", { type: "save-design", name: "重复", drawingNumber: "DM-TEST-01", document: f.session.document })).toThrow("编号");
    f.run("designer", { type: "submit-design", designId: f.designId });
    const multi = { ...actor("designer"), roles: ["designer", "reviewer"] as FactoryRole[] };
    expect(() => applyFactoryCommand(structuredClone(f.state), multi, { type: "review-design", designId: f.designId, role: "reviewer", decision: "approve", note: "自行审批" })).toThrow("提交人");
    f.run("reviewer", { type: "review-design", designId: f.designId, role: "reviewer", decision: "reject", note: "修改后重算" });
    expect(f.state.designs[0]!.status).toBe("returned");
    f.run("designer", { type: "save-design", designId: f.designId, name: "已修改", drawingNumber: "DM-TEST-01", document: f.session.document });
    const releaseId = f.release(f.designId), pinned = structuredClone(f.state.releases[0]);
    expect(() => f.run("designer", { type: "save-design", designId: f.designId, name: "覆盖", drawingNumber: "DM-TEST-01", document: f.session.document })).toThrow("不可直接");
    const packet = f.issue(releaseId);
    expect(packet.bom.some((line) => line.lengthMm > 0 && line.quantity > 0)).toBe(true);
    expect(packet.productionEligible).toBe(false);
    expect(() => f.run("quality", { type: "record-execution", packageId: packet.id, stepId: "QUALITY", kind: "inspection", quantity: packet.quantity, pieceNumbers: packet.pieceNumbers, result: "pass", actual: "越过工序" })).toThrow("前序");
    f.executeAll(packet);
    f.run("quality", { type: "complete-package", packageId: packet.id });
    expect(f.state.orders[0]!.status).toBe("complete");
    expect(f.state.releases[0]).toEqual(pinned);
  });
  it("creates custom engineering requests with order dimensions/materials and requires customer confirmation", () => {
    const f = fixture();
    const orderId = f.run("sales", { type: "create-order", number: "CUSTOM-01", customer: "客户", kind: "custom", quantity: 1, requirement: { ...demand, widthMm: 1300, hardware: "HW-TURN-STD" } });
    const task = f.state.engineeringRequests[0]!, document = f.state.designs.find((entry) => entry.id === task.designId)!.document;
    expect(document.windows[0]!.widthMm).toBe(1300);
    expect(document.windows[0]!.defaultHardwareSetId).toBe("HW-TURN-STD");
    f.run("designer", { type: "claim-engineering", requestId: task.id });
    f.run("sales", { type: "confirm-order", orderId });
    const releaseId = f.release(task.designId, "CUSTOM-PRODUCT-01"), lineId = task.lineId;
    expect(() => f.run("production", { type: "create-package", orderId, lineId, batch: "B1" })).toThrow("客户");
    f.run("sales", { type: "confirm-customer", orderId, lineId, releaseId });
    f.run("production", { type: "create-package", orderId, lineId, batch: "B1" });
    expect(f.state.engineeringRequests[0]!.status).toBe("released");
  });
  it("retains original records, authorizes first-work, publishes compared change, cuts over and reconciles retrospective design", () => {
    const f = fixture(), releaseId = f.release(f.designId), base = f.issue(releaseId);
    const original = structuredClone(f.state.releases[0]);
    const deviationId = f.run("operator", { type: "create-deviation", packageId: base.id, reason: "客户现场需调整", actual: "申请先行领料，不倒填设计日期", pieceNumbers: base.pieceNumbers, stepIds: ["MATERIAL"], risk: "补图前用料偏差", control: "单独标识并待质量对账" });
    expect(() => f.run("operator", { type: "record-execution", packageId: base.id, stepId: "MATERIAL", kind: "material", quantity: base.quantity, pieceNumbers: base.pieceNumbers, result: "pass", actual: "尚未授权", deviationId })).toThrow("获准");
    for (const role of ["production", "quality"] as const) f.run(role, { type: "review-deviation", deviationId, role, decision: "approve", note: "先行范围已确认" });
    const actualId = f.run("operator", { type: "record-execution", packageId: base.id, stepId: "MATERIAL", kind: "material", quantity: 1, pieceNumbers: [base.pieceNumbers[0]!], result: "pass", actual: "真实领料记录", deviationId });
    const actual = structuredClone(f.state.executions[0]);
    const changeId = f.run("sales", { type: "create-change", packageId: base.id, reason: "后补尺寸调整", batch: base.batch, fromStep: "CUT", pieceNumbers: base.pieceNumbers, disposition: "rework", procurement: "材料可复用", cost: "增加切割", delivery: "延后一天" });
    f.run("designer", { type: "link-deviation-change", deviationId, changeId });
    const designId = f.state.changes[0]!.designId, design = f.state.designs.find((entry) => entry.id === designId)!;
    const session = new DesignSession(design.document);
    session.execute(createResizeWindowCommand({ commandId: "RESIZE", windowId: "W1", widthMm: 1250, heightMm: 1500 }));
    f.run("designer", { type: "save-design", designId, drawingNumber: design.drawingNumber, name: "调整宽度", document: session.document });
    const newReleaseId = f.release(designId);
    f.run("designer", { type: "submit-change", changeId });
    expect(f.state.changes[0]!.differences.some((entry) => entry.path.endsWith("widthMm"))).toBe(true);
    expect(f.state.changes[0]!.impact.executionIds).toContain(actualId);
    for (const role of DESIGN_REVIEW_ROLES) f.run(role, { type: "review-change", changeId, role, decision: "approve", note: "影响及旧件返工已核对" });
    expect(() => f.run("production", { type: "issue-change", changeId })).toThrow("客户");
    f.run("sales", { type: "confirm-customer", orderId: base.orderId, lineId: base.lineId, releaseId: newReleaseId });
    f.run("production", { type: "issue-change", changeId });
    expect(() => f.run("operator", { type: "record-execution", packageId: base.id, stepId: "CUT", kind: "process", quantity: 1, pieceNumbers: [base.pieceNumbers[0]!], result: "pass", actual: "继续按旧图" })).toThrow("旧图");
    f.run("operator", { type: "acknowledge-change", changeId });
    const changed = f.state.packages.at(-1)!;
    f.executeAll(changed);
    expect(() => f.run("quality", { type: "close-change", changeId, note: "只有一句说明不能关闭" })).toThrow("旧件须逐件");
    const evidenceId = f.run("operator", { type: "record-execution", packageId: base.id, stepId: "CUT", kind: "rework", quantity: base.quantity, pieceNumbers: base.pieceNumbers, result: "recorded", actual: "旧料已返工，并逐件标识" });
    const recordId = f.run("operator", { type: "record-change-disposition", changeId, pieceNumbers: base.pieceNumbers, disposition: "rework", executionIds: [evidenceId], replacementPairs: [], note: "原件返工后转入新版本制造包" });
    f.confirmDisposition(changeId, recordId);
    f.run("quality", { type: "close-change", changeId, note: "旧件已返工，新版本逐件验证通过" });
    expect(() => f.run("quality", { type: "close-deviation", deviationId, resolution: "不能遗漏另一件实际记录" })).toThrow("缺少实际记录");
    f.run("operator", { type: "record-execution", packageId: base.id, stepId: "MATERIAL", kind: "material", quantity: 1, pieceNumbers: [base.pieceNumbers[1]!], result: "pass", actual: "另一件领料实际记录", deviationId });
    f.run("quality", { type: "close-deviation", deviationId, resolution: "设计、计划、实际对账一致；原执行真实保留" });
    f.run("quality", { type: "complete-package", packageId: base.id });
    f.run("quality", { type: "complete-package", packageId: changed.id });
    expect(f.state.orders[0]!.status).toBe("complete");
    expect(f.state.executions[0]).toEqual(actual);
    expect(f.state.releases[0]).toEqual(original);
    expect(f.state.deviations[0]!.status).toBe("closed");
    expect(f.state.changes[0]!.closure?.note).toContain("逐件");
  });
  it("compares stable object identities instead of falsely reporting array reordering", () => {
    expect(compareFactoryVersions({ windows: [{ objectId: "A", widthMm: 12 }, { objectId: "B", widthMm: 22 }] }, { windows: [{ objectId: "B", widthMm: 22 }, { objectId: "A", widthMm: 12 }] })).toEqual([]);
    expect(compareFactoryVersions({ revision: 1, widthMm: 12 }, { revision: 2, widthMm: 13 })[0]).toMatchObject({ category: "geometry", before: 12, after: 13 });
  });
  it("splits quantities into separately reviewed batches without prematurely completing the order", () => {
    const f = fixture(), releaseId = f.release(f.designId);
    const orderId = f.run("sales", { type: "create-order", number: "SPLIT", customer: "客户", kind: "standard", quantity: 3, releaseId, requirement: demand });
    f.run("sales", { type: "confirm-order", orderId });
    const lineId = f.state.orders[0]!.lines[0]!.id;
    expect(() => f.run("production", { type: "create-package", orderId, lineId, batch: "ONE", quantity: 4 })).toThrow("尚未分配");
    expect(() => f.run("production", { type: "create-package", orderId, lineId, batch: "ONE", quantity: 0.5 })).toThrow("无效");
    const first = f.run("production", { type: "create-package", orderId, lineId, batch: "ONE", quantity: 1 });
    expect(() => f.run("production", { type: "create-package", orderId, lineId, batch: " ONE ", quantity: 1 })).toThrow("同名");
    for (const role of ["process", "procurement", "quality"] as const) f.run(role, { type: "review-package", packageId: first, role, decision: "approve", note: "第一批" });
    f.run("production", { type: "issue-package", packageId: first }); f.run("operator", { type: "acknowledge-package", packageId: first });
    f.executeAll(f.state.packages[0]!); f.run("quality", { type: "complete-package", packageId: first });
    expect(f.state.orders[0]!.status).not.toBe("complete");
    const second = f.run("production", { type: "create-package", orderId, lineId, batch: "TWO" }); // Default remaining quantity.
    expect(f.state.packages[1]!.quantity).toBe(2);
    expect(f.state.packages[1]!.bom[0]!.quantity).toBe(f.state.packages[0]!.bom[0]!.quantity * 2);
    expect(factoryAllocatedQuantity(f.state.packages, lineId)).toBe(3);
    expect(new Set(f.state.packages.flatMap((entry) => entry.pieceNumbers)).size).toBe(3);
    expect(() => f.run("production", { type: "create-package", orderId, lineId, batch: "THREE", quantity: 1 })).toThrow("全部分配");
    for (const role of ["process", "procurement", "quality"] as const) f.run(role, { type: "review-package", packageId: second, role, decision: "approve", note: "第二批" });
    f.run("production", { type: "issue-package", packageId: second }); f.run("operator", { type: "acknowledge-package", packageId: second });
    f.executeAll(f.state.packages[1]!); f.run("quality", { type: "complete-package", packageId: second });
    expect(f.state.orders[0]!.status).toBe("complete");
  });
  it.each(["use-as-is", "rework", "scrap", "replace"] as const)("requires per-piece %s evidence, separate production/quality confirmation and final inspection", (disposition) => {
    const f = fixture(), base = f.issue(f.release(f.designId)), { changeId, changed } = f.changePacket(base, disposition);
    const input = { type: "record-change-disposition" as const, changeId, pieceNumbers: base.pieceNumbers, disposition, executionIds: [] as string[], replacementPairs: disposition === "replace" ? base.pieceNumbers.map((oldPiece, index) => ({ oldPiece, newPiece: changed.pieceNumbers[index]! })) : [], note: "逐件处置实况与旧件去向" };
    expect(() => f.run("sales", input)).toThrow("岗位");
    expect(() => f.run("operator", { ...input, pieceNumbers: ["OTHER"] })).toThrow("生效范围");
    if (disposition === "rework" || disposition === "scrap") {
      expect(() => f.run("operator", input)).toThrow("成功实际记录");
      input.executionIds.push(f.run("operator", { type: "record-execution", packageId: base.id, stepId: "CUT", kind: disposition, quantity: base.quantity, pieceNumbers: base.pieceNumbers, result: "recorded", actual: "真实处置记录" }));
    }
    if (disposition === "replace") {
      expect(() => f.run("operator", { ...input, replacementPairs: [] })).toThrow("一对一");
      expect(() => f.run("operator", { ...input, replacementPairs: input.replacementPairs.map((entry) => ({ ...entry, newPiece: changed.pieceNumbers[0]! })) })).toThrow("一对一");
    }
    const recordId = f.run("operator", input);
    const self = { ...actor("operator"), roles: ["operator", "production"] as FactoryRole[] };
    expect(() => f.runAs(self, { type: "review-change-disposition", changeId, recordId, role: "production", decision: "approve", note: "自行确认" })).toThrow("提交人");
    f.run("production", { type: "review-change-disposition", changeId, recordId, role: "production", decision: "approve", note: "生产核对" });
    const dual = { ...actor("production"), roles: ["production", "quality"] as FactoryRole[] };
    expect(() => f.runAs(dual, { type: "review-change-disposition", changeId, recordId, role: "quality", decision: "approve", note: "跨岗位自行确认" })).toThrow("跨岗位");
    f.executeAll(changed);
    expect(() => f.run("quality", { type: "close-change", changeId, note: "只有生产确认" })).toThrow("旧件须逐件");
    f.run("quality", { type: "review-change-disposition", changeId, recordId, role: "quality", decision: "approve", note: "质量核对" });
    const frozen = structuredClone(f.state.changes[0]!.dispositionRecords);
    expect(() => f.run("operator", input)).toThrow("不可覆盖");
    f.run("quality", { type: "close-change", changeId, note: "新版本终检与旧件处置逐件闭环" });
    expect(f.state.changes[0]!.dispositionRecords).toEqual(frozen);
    expect(factoryAllocatedQuantity(f.state.packages, base.lineId)).toBe(base.quantity);
    expect(() => f.run("operator", input)).toThrow("接收变更");
  });
  it("keeps isolation and rejected evidence as history, requiring a separately confirmed final outcome", () => {
    const f = fixture(), base = f.issue(f.release(f.designId)), { changeId, changed } = f.changePacket(base, "isolate");
    f.executeAll(changed);
    const input = { type: "record-change-disposition" as const, changeId, pieceNumbers: base.pieceNumbers, disposition: "isolate" as FactoryDisposition, executionIds: [], replacementPairs: [], note: "隔离专柜，未放行" };
    const isolated = f.run("operator", input); f.confirmDisposition(changeId, isolated);
    expect(() => f.run("quality", { type: "close-change", changeId, note: "隔离不算最终结论" })).toThrow("隔离记录");
    const rejected = f.run("operator", { ...input, disposition: "use-as-is", note: "待核对" });
    f.run("quality", { type: "review-change-disposition", changeId, recordId: rejected, role: "quality", decision: "reject", note: "缺少去向说明" });
    expect(() => f.run("production", { type: "review-change-disposition", changeId, recordId: rejected, role: "production", decision: "approve", note: "不能覆盖退回" })).toThrow("补充新记录");
    const final = f.run("operator", { ...input, disposition: "use-as-is", note: "逐件尺寸核对，旧件沿用并转入新版本" }); f.confirmDisposition(changeId, final);
    f.run("quality", { type: "close-change", changeId, note: "隔离放行与质量核对完成" });
    expect(f.state.changes[0]!.dispositionRecords).toHaveLength(3);
    expect(f.state.changes[0]!.dispositionRecords![1]!.reviews[0]!.decision).toBe("reject");
  });
  it("appends approved-scope disposal to a previously completed package without rewriting its production history", () => {
    const f = fixture(), base = f.issue(f.release(f.designId)); f.executeAll(base);
    f.run("quality", { type: "complete-package", packageId: base.id });
    const original = structuredClone(f.state.executions);
    const { changeId, changed } = f.changePacket(base, "rework");
    const execution = { type: "record-execution" as const, packageId: base.id, stepId: "CUT", kind: "rework" as const, quantity: base.quantity, pieceNumbers: base.pieceNumbers, result: "recorded" as const, actual: "对已完工原件按批准变更返工" };
    expect(() => f.run("operator", { ...execution, kind: "process" })).toThrow("已完工包");
    const evidenceId = f.run("operator", execution);
    const recordId = f.run("operator", { type: "record-change-disposition", changeId, pieceNumbers: base.pieceNumbers, disposition: "rework", executionIds: [evidenceId], replacementPairs: [], note: "旧件返工移交" });
    f.confirmDisposition(changeId, recordId);
    expect(() => f.run("operator", execution)).toThrow("最终处置已确认");
    f.executeAll(changed); f.run("quality", { type: "close-change", changeId, note: "逐件闭环" });
    f.run("quality", { type: "complete-package", packageId: changed.id });
    expect(f.state.orders[0]!.status).toBe("complete");
    expect(f.state.executions.slice(0, original.length)).toEqual(original);
  });
  it("keeps independent batch confirmations and order defaults pinned while publishing order-only branches", () => {
    const f = fixture(), catalogId = f.release(f.designId), pinnedCatalog = structuredClone(f.state.releases[0]);
    const orderId = f.run("sales", { type: "create-order", number: "MULTI-BATCH", customer: "客户", kind: "standard", quantity: 3, releaseId: catalogId, requirement: demand });
    f.run("sales", { type: "confirm-order", orderId });
    const lineId = f.state.orders[0]!.lines[0]!.id;
    const first = f.activate(f.run("production", { type: "create-package", orderId, lineId, batch: "FIRST", quantity: 1 }));
    const second = f.activate(f.run("production", { type: "create-package", orderId, lineId, batch: "SECOND", quantity: 1 }));
    const a = f.prepareChange(first, "use-as-is", { widthMm: 1250, batch: "FIRST-REV" });
    const b = f.prepareChange(second, "use-as-is", { widthMm: 1300, batch: "SECOND-REV" });
    f.run("production", { type: "issue-change", changeId: a.changeId });
    expect(f.state.packages.find((entry) => entry.id === second.id)).toEqual(second);
    f.run("production", { type: "issue-change", changeId: b.changeId });
    expect(f.state.orders[0]!.lines[0]).toMatchObject({ releaseId: catalogId, customerConfirmedReleaseId: catalogId });
    expect(f.state.changes.map((entry) => entry.customerConfirmation!.releaseId)).toEqual([a.releaseId, b.releaseId]);
    const remainingId = f.run("production", { type: "create-package", orderId, lineId, batch: "THIRD" });
    expect(f.state.packages.find((entry) => entry.id === remainingId)).toMatchObject({ quantity: 1, releaseId: catalogId });
    const orderRelease = f.state.releases.find((entry) => entry.id === a.releaseId)!;
    expect(factoryReleaseScope(f.state, orderRelease)).toEqual({ kind: "order-line", orderId, lineId });
    expect(() => f.run("sales", { type: "create-order", number: "CANNOT-STANDARDIZE", customer: "客户", kind: "standard", quantity: 1, releaseId: a.releaseId, requirement: { ...demand, widthMm: 1250 } })).toThrow("订单专用");
    expect(() => f.run("designer", { type: "fork-release", releaseId: a.releaseId, name: "不能绕过制造包" })).toThrow("生效制造包");
    const catalogDraft = f.run("designer", { type: "fork-release", releaseId: catalogId, name: "独立标准产品修订" });
    const nextCatalog = f.release(catalogDraft);
    expect(factoryReleaseScope(f.state, f.state.releases.find((entry) => entry.id === nextCatalog)!)).toEqual({ kind: "catalog" });
    const stale = f.run("designer", { type: "fork-release", releaseId: catalogId, name: "过期标准基线" });
    expect(() => f.release(stale)).toThrow("更新版本");
    expect(f.state.releases[0]).toEqual(pinnedCatalog);
    const legacy = structuredClone(f.state);
    for (const release of legacy.releases) delete release.scope;
    expect(factoryReleaseScope(legacy, legacy.releases.find((entry) => entry.id === a.releaseId)!)).toEqual({ kind: "order-line", orderId, lineId });
    expect(() => applyFactoryCommand(legacy, actor("sales"), { type: "create-order", number: "LEGACY-NO-PROMOTION", customer: "客户", kind: "standard", quantity: 1, releaseId: a.releaseId, requirement: { ...demand, widthMm: 1250 } })).toThrow("订单专用");
  });
  it("reserves disjoint pieces, ignores unrelated actuals, rejects stale affected actuals and completes every lineage", () => {
    const f = fixture(), base = f.issue(f.release(f.designId), 3);
    const first = [base.pieceNumbers[0]!], second = [base.pieceNumbers[1]!], residual = [base.pieceNumbers[2]!];
    const a = f.prepareChange(base, "use-as-is", { pieces: first, widthMm: 1250, batch: "A" });
    expect(() => f.prepareChange(base, "use-as-is", { pieces: first, widthMm: 1350 })).toThrow("未结束变更");
    const b = f.prepareChange(base, "use-as-is", { pieces: second, widthMm: 1300, batch: "B" });
    f.executeAll(base, residual);
    const actual = f.run("operator", { type: "record-execution", packageId: base.id, stepId: "MATERIAL", kind: "material", quantity: 1, pieceNumbers: first, result: "pass", actual: "会签后本范围领料" });
    expect(() => f.run("production", { type: "issue-change", changeId: a.changeId })).toThrow("重新评估");
    f.run("production", { type: "refresh-change-impact", changeId: a.changeId });
    expect(f.state.changes[0]!.impact.executionIds).toEqual([actual]);
    for (const role of DESIGN_REVIEW_ROLES) f.run(role, { type: "review-change", changeId: a.changeId, role, decision: "approve", note: "新增领料重新核对" });
    for (const change of [a, b]) {
      f.run("production", { type: "issue-change", changeId: change.changeId });
      f.run("operator", { type: "acknowledge-change", changeId: change.changeId });
      const changed = f.state.packages.at(-1)!;
      f.executeAll(changed);
      const recordId = f.run("operator", { type: "record-change-disposition", changeId: change.changeId, pieceNumbers: changed.pieceNumbers, disposition: "use-as-is", executionIds: [], replacementPairs: [], note: "本件旧料逐项复核后沿用" });
      f.confirmDisposition(change.changeId, recordId);
      f.run("quality", { type: "close-change", changeId: change.changeId, note: "本件闭环" });
      f.run("quality", { type: "complete-package", packageId: changed.id });
    }
    expect(factoryEffectivePieceNumbers(f.state, base)).toEqual(residual);
    expect(() => f.prepareChange(base, "use-as-is", { pieces: first, widthMm: 1400 })).toThrow("已切换");
    f.run("quality", { type: "complete-package", packageId: base.id });
    expect(factoryAllocatedQuantity(f.state.packages, base.lineId)).toBe(3);
    expect(f.state.orders[0]!.status).toBe("complete");
  });
  it("continues a second rework from the new package rather than inheriting the first package's disposal lock", () => {
    const f = fixture(), base = f.issue(f.release(f.designId), 1);
    let current = base;
    for (const widthMm of [1250, 1300]) {
      const { changeId, changed } = f.changePacket(current, "rework", { widthMm });
      expect(() => f.prepareChange(changed, "rework", { widthMm: 1400 })).toThrow("先闭环来源");
      const evidenceId = f.run("operator", { type: "record-execution", packageId: current.id, stepId: "CUT", kind: "rework", quantity: 1, pieceNumbers: current.pieceNumbers, result: "recorded", actual: "本版本旧件按新修订返工" });
      const recordId = f.run("operator", { type: "record-change-disposition", changeId, pieceNumbers: current.pieceNumbers, disposition: "rework", executionIds: [evidenceId], replacementPairs: [], note: "本轮返工实际交接" });
      f.confirmDisposition(changeId, recordId); f.executeAll(changed);
      f.run("quality", { type: "close-change", changeId, note: "本轮质量验证完成" });
      if (f.state.packages.find((entry) => entry.id === current.id)!.status !== "complete") f.run("quality", { type: "complete-package", packageId: current.id });
      f.run("quality", { type: "complete-package", packageId: changed.id });
      current = changed;
    }
    expect(f.state.packages).toHaveLength(3);
    expect(f.state.changes.map((entry) => entry.status)).toEqual(["closed", "closed"]);
    expect(factoryAllocatedQuantity(f.state.packages, base.lineId)).toBe(1);
    expect(f.state.orders[0]!.status).toBe("complete");
  });
  it("withdraws unissued reservations, preserves review history and releases linked retrospective tasks", () => {
    const f = fixture(), base = f.issue(f.release(f.designId));
    const deviationId = f.run("operator", { type: "create-deviation", packageId: base.id, reason: "先行申请", actual: "需补图", pieceNumbers: base.pieceNumbers, stepIds: ["MATERIAL"], risk: "待核对", control: "隔离" });
    for (const role of ["production", "quality"] as const) f.run(role, { type: "review-deviation", deviationId, role, decision: "approve", note: "范围已确认" });
    const first = f.prepareChange(base, "use-as-is");
    f.run("designer", { type: "link-deviation-change", deviationId, changeId: first.changeId });
    const archived = structuredClone(f.state.changes[0]!), design = f.state.designs.find((entry) => entry.id === archived.designId)!;
    expect(() => f.run("designer", { type: "withdraw-change", changeId: first.changeId, note: "非发起人" })).toThrow("发起人");
    f.run("sales", { type: "withdraw-change", changeId: first.changeId, note: "客户取消此方案，保留会签证据" });
    expect(f.state.changes[0]!.reviews).toEqual(archived.reviews);
    expect(f.state.deviations[0]).toMatchObject({ status: "authorized" });
    expect(f.state.deviations[0]!.changeId).toBeUndefined();
    expect(() => f.run("sales", { type: "confirm-customer", orderId: base.orderId, lineId: base.lineId, releaseId: first.releaseId })).toThrow("有效设计");
    expect(() => f.run("production", { type: "issue-change", changeId: first.changeId })).toThrow("会签");
    expect(() => f.run("designer", { type: "save-design", designId: design.id, name: "不能覆盖撤回方案", drawingNumber: design.drawingNumber, document: design.document })).toThrow("已撤回");
    const next = f.prepareChange(base, "use-as-is", { widthMm: 1300 });
    f.run("production", { type: "issue-change", changeId: next.changeId });
    expect(() => f.run("sales", { type: "withdraw-change", changeId: next.changeId, note: "已下达不可撤回" })).toThrow("尚未下达");
    expect(f.state.changes[0]!.cancellation?.note).toContain("保留");
  });
  it("limits first-work to explicit pieces and steps, rejects bypass, and resets signatures on resubmission", () => {
    const f = fixture(), packet = f.issue(f.release(f.designId));
    const scope = { pieceNumbers: [packet.pieceNumbers[0]!], stepIds: ["MATERIAL"], risk: "规格待追认", control: "独立标识与检验" };
    const input = { reason: "紧急调整", actual: "只先领料", ...scope };
    expect(() => f.run("operator", { type: "create-deviation", packageId: packet.id, ...input, pieceNumbers: [] })).toThrow("件号");
    expect(() => f.run("operator", { type: "create-deviation", packageId: packet.id, ...input, stepIds: ["UNKNOWN"] })).toThrow("工序");
    const id = f.run("operator", { type: "create-deviation", packageId: packet.id, ...input });
    expect(() => f.run("operator", { type: "create-deviation", packageId: packet.id, ...input })).toThrow("未闭环");
    const execution = { type: "record-execution" as const, packageId: packet.id, stepId: "MATERIAL", kind: "material" as const, quantity: 1, pieceNumbers: scope.pieceNumbers, result: "pass" as const, actual: "真实领料" };
    expect(() => f.run("operator", execution)).toThrow("正常执行绕过");
    f.run("operator", { ...execution, pieceNumbers: [packet.pieceNumbers[1]!] });
    f.run("production", { type: "review-deviation", deviationId: id, role: "production", decision: "approve", note: "主管通过" });
    f.run("quality", { type: "review-deviation", deviationId: id, role: "quality", decision: "reject", note: "补充隔离措施" });
    expect(f.state.deviations[0]!.status).toBe("returned");
    expect(() => f.run("production", { type: "revise-deviation", deviationId: id, ...input })).toThrow("申请人");
    const createdAt = f.state.deviations[0]!.createdAt;
    f.run("operator", { type: "revise-deviation", deviationId: id, ...input, control: "隔离专柜，逐件复核" });
    expect(f.state.deviations[0]!.reviews).toEqual([]);
    expect(f.state.deviations[0]!.createdAt).toBe(createdAt);
    for (const role of ["production", "quality"] as const) f.run(role, { type: "review-deviation", deviationId: id, role, decision: "approve", note: "明确范围批准" });
    expect(() => f.run("operator", { ...execution, deviationId: id, pieceNumbers: [packet.pieceNumbers[1]!] })).toThrow("授权件号");
    expect(() => f.run("operator", execution)).toThrow("正常执行绕过");
    expect(() => f.run("operator", { type: "revise-deviation", deviationId: id, ...input })).toThrow("申请人");
    expect(() => f.run("operator", { type: "withdraw-deviation", deviationId: id, note: "不能撤回已授权" })).toThrow("未授权");
    f.run("operator", { ...execution, deviationId: id });
    const after = structuredClone(f.state);
    expect(() => f.run("operator", { ...execution, stepId: "CUT", kind: "process", deviationId: id })).toThrow("工序范围");
    expect(f.state).toEqual(after);
    const changeId = f.run("sales", { type: "create-change", packageId: packet.id, reason: "部分件修改", batch: packet.batch, fromStep: "CUT", pieceNumbers: [packet.pieceNumbers[1]!], disposition: "rework", procurement: "无", cost: "无", delivery: "无" });
    expect(() => f.run("designer", { type: "link-deviation-change", deviationId: id, changeId })).toThrow("覆盖全部");
  });
  it("withdraws only untouched unapproved applications and retains their historical evidence", () => {
    const f = fixture(), packet = f.issue(f.release(f.designId));
    const id = f.run("operator", { type: "create-deviation", packageId: packet.id, reason: "可撤回申请", actual: "未执行", pieceNumbers: packet.pieceNumbers, stepIds: ["MATERIAL"], risk: "风险待确认", control: "暂停执行" });
    expect(() => f.run("production", { type: "withdraw-deviation", deviationId: id, note: "越权撤回" })).toThrow("申请人");
    f.run("quality", { type: "review-deviation", deviationId: id, role: "quality", decision: "reject", note: "不允许先行" });
    f.run("operator", { type: "withdraw-deviation", deviationId: id, note: "按原设计执行，不再先行" });
    expect(f.state.deviations[0]).toMatchObject({ status: "cancelled", resolution: "按原设计执行，不再先行" });
    expect(f.state.deviations[0]!.reviews[0]!.note).toBe("不允许先行");
    f.executeAll(packet); f.run("quality", { type: "complete-package", packageId: packet.id });
    expect(f.state.orders[0]!.status).toBe("complete");
  });
  it("invalidates prior signatures when process engineering edits a route and freezes it on publication", () => {
    const f = fixture();
    f.run("designer", { type: "submit-design", designId: f.designId });
    f.run("reviewer", { type: "review-design", designId: f.designId, role: "reviewer", decision: "approve", note: "首轮" });
    const route = structuredClone(f.state.designs[0]!.calculation!.route);
    route.splice(route.length - 1, 0, { id: "INTERMEDIATE-CHECK", name: "中间检验", instruction: "装配后检查对角线", sourceObjectIds: ["W1"] });
    expect(() => f.run("sales", { type: "set-design-route", designId: f.designId, route })).toThrow("岗位");
    f.run("process", { type: "set-design-route", designId: f.designId, route });
    expect(f.state.designs[0]!.status).toBe("returned");
    expect(f.state.designs[0]!.reviews).toEqual([]);
    const releaseId = f.release(f.designId);
    expect(f.state.releases.find((entry) => entry.id === releaseId)!.calculation.route).toEqual(route);
    expect(() => f.run("process", { type: "set-design-route", designId: f.designId, route })).toThrow("不可覆盖");
  });
});
