import { describe, expect, it } from "vitest";
import { createEmptyDesign } from "@doormes/domain";
import { createRectangularWindowCommand, DesignSession } from "@doormes/application";
import { DESIGN_REVIEW_ROLES, type FactoryActor, type FactoryChange, type FactoryCommand, type FactoryExecution, type FactoryRole } from "@doormes/contracts/factory-workflow";
import { factoryLastStepResult, factoryStepPieceNumbers, factoryPackageCompletionBlockers, factoryChangeIssueBlockers, factoryChangeClosureBlockers, factoryDeviationClosureBlockers } from "@doormes/contracts/factory-production-status";
import { applyFactoryCommand, createFactoryWorkspace, FactoryError } from "./workflow.js";

function fixture() {
  const state = createFactoryWorkspace(), session = new DesignSession(createEmptyDesign("GATES"));
  const actor = (role: FactoryRole): FactoryActor => ({ id: role, name: role, roles: [role], organizationId: state.organizationId });
  const run = (role: FactoryRole, command: FactoryCommand) => applyFactoryCommand(state, actor(role), command);
  const requirement = { widthMm: 1200, heightMm: 1500, material: "AL70", glass: "GL-LOWE-24", hardware: "HW-TT-STD", finish: "内RAL9016 / 外RAL7016", dueDate: "", note: "" };
  session.execute(createRectangularWindowCommand({ commandId: "CREATE", windowId: "W1", mark: "C1", widthMm: 1200, heightMm: 1500, profileSystemId: requirement.material, defaultGlassTypeId: requirement.glass, defaultHardwareSetId: requirement.hardware }));
  const designId = run("designer", { type: "save-design", document: session.document, name: "门禁", drawingNumber: "GATES" });
  run("designer", { type: "submit-design", designId });
  for (const role of DESIGN_REVIEW_ROLES) run(role, { type: "review-design", designId, role, decision: "approve", note: "核对" });
  const releaseId = run("designer", { type: "release-design", designId, productCode: "GATES" });
  const orderId = run("sales", { type: "create-order", number: "GATES-SO", customer: "测试", kind: "standard", quantity: 2, releaseId, requirement });
  run("sales", { type: "confirm-order", orderId });
  const lineId = state.orders[0]!.lines[0]!.id;
  const id = run("production", { type: "create-package", orderId, lineId, batch: "A" });
  for (const role of ["process", "procurement", "quality"] as const) run(role, { type: "review-package", packageId: id, role, decision: "approve", note: "确认" });
  run("production", { type: "issue-package", packageId: id }); run("operator", { type: "acknowledge-package", packageId: id });
  const packet = state.packages[0]!;
  const execution = (stepId: string, ids = packet.pieceNumbers, result: FactoryExecution["result"] = "pass", kind?: FactoryExecution["kind"]) => run(stepId === "QUALITY" ? "quality" : "operator", { type: "record-execution", packageId: id, stepId, pieceNumbers: ids, quantity: ids.length, kind: kind ?? (stepId === "QUALITY" ? "inspection" : stepId === "MATERIAL" ? "material" : "process"), result, actual: "已核对" });
  const change = () => {
    const changeId = run("production", { type: "create-change", packageId: id, reason: "尺寸修订", batch: "B", fromStep: packet.route[1]!.id, pieceNumbers: [packet.pieceNumbers[0]!], disposition: "use-as-is", procurement: "核对", cost: "核对", delivery: "核对" });
    return state.changes.find((entry) => entry.id === changeId)!;
  };
  return { state, run, packet, execution, change, releaseId };
}
describe("shared production prerequisites", () => {
  it("reports exact missing pieces and the service enforces the same prerequisite", () => {
    const f = fixture(), gate = factoryPackageCompletionBlockers(f.state, f.packet)[0]!;
    expect(gate.pieceNumbers).toEqual(f.packet.pieceNumbers);
    expect(() => f.run("quality", { type: "complete-package", packageId: f.packet.id })).toThrow(gate.message);
    for (const step of f.packet.route) f.execution(step.id);
    expect(factoryPackageCompletionBlockers(f.state, f.packet)).toEqual([]);
    f.execution("QUALITY", [f.packet.pieceNumbers[0]!], "fail");
    expect(factoryPackageCompletionBlockers(f.state, f.packet)[0]!.pieceNumbers).toEqual([f.packet.pieceNumbers[0]]);
    f.execution("QUALITY", [f.packet.pieceNumbers[0]!]);
    f.run("quality", { type: "complete-package", packageId: f.packet.id });
    expect(factoryPackageCompletionBlockers(f.state, f.packet)[0]!.message).toContain("已经结束");
    expect(factoryStepPieceNumbers(f.state, f.packet, "QUALITY")).toEqual(f.packet.pieceNumbers);
  });
  it("does not count disposal as normal progress or override a latest failed step", () => {
    const f = fixture(); f.execution("MATERIAL"); f.execution("MATERIAL", [f.packet.pieceNumbers[0]!], "fail");
    f.execution("MATERIAL", [f.packet.pieceNumbers[0]!], "pass", "rework");
    expect(factoryLastStepResult(f.state, f.packet, f.packet.pieceNumbers[0]!, "MATERIAL")).toBe("fail");
  });
  it("keeps pre-cutover ranges and inherits only allowed prior records", () => {
    const f = fixture(); f.execution("MATERIAL"); const change = f.change();
    // Explicit projection fixture; full legal issuance paths are covered by workflow/HTTP tests.
    change.status = "issued";
    const next = { ...structuredClone(f.packet), id: "NEXT", supersedes: f.packet.id, changeId: change.id, pieceNumbers: [...change.effectivity.pieceNumbers] };
    f.state.packages.push(next);
    expect(factoryStepPieceNumbers(f.state, f.packet, "MATERIAL")).toEqual(f.packet.pieceNumbers);
    expect(factoryStepPieceNumbers(f.state, f.packet, change.effectivity.fromStep)).toEqual([f.packet.pieceNumbers[1]]);
    expect(factoryLastStepResult(f.state, next, next.pieceNumbers[0]!, "MATERIAL")).toBe("pass");
    change.disposition = "replace";
    expect(factoryLastStepResult(f.state, next, next.pieceNumbers[0]!, "MATERIAL")).toBeUndefined();
    next.supersedes = next.id;
    expect(factoryLastStepResult(f.state, next, next.pieceNumbers[0]!, "MATERIAL")).toBeUndefined();
  });
  it("shows stale affected work, confirmation and conflict with matching HTTP error codes", () => {
    const f = fixture(), change = f.change(); change.status = "approved"; change.newReleaseId = f.releaseId;
    const next = structuredClone(f.state.releases[0]!); next.id = "NEW"; f.state.releases.push(next); change.newReleaseId = next.id;
    expect(factoryChangeIssueBlockers(f.state, change)[0]!.message).toContain("客户确认");
    change.customerConfirmation = { releaseId: next.id, actorId: "sales", at: f.state.savedAt };
    f.execution("MATERIAL", [f.packet.pieceNumbers[1]!]);
    expect(factoryChangeIssueBlockers(f.state, change)).toEqual([]);
    f.execution("MATERIAL", [f.packet.pieceNumbers[0]!]);
    expect(factoryChangeIssueBlockers(f.state, change)[0]!.code).toBe("STALE_IMPACT");
    try { f.run("production", { type: "issue-change", changeId: change.id }); throw new Error("should reject"); }
    catch (error) { expect(error).toBeInstanceOf(FactoryError); expect((error as FactoryError).status).toBe(409); }
    change.impact.executionIds = f.state.executions.filter((entry) => entry.pieceNumbers.includes(f.packet.pieceNumbers[0]!)).map((entry) => entry.id);
    const other: FactoryChange = { ...structuredClone(change), id: "OTHER", status: "designing" }; f.state.changes.push(other);
    expect(factoryChangeIssueBlockers(f.state, change)[0]!.code).toBe("CONFLICT");
  });
  it("requires final two-role old-piece disposition and new-piece latest inspection", () => {
    const f = fixture(); for (const step of f.packet.route) f.execution(step.id);
    const change = f.change(); change.status = "acknowledged"; change.newPackageId = f.packet.id;
    const affected = change.effectivity.pieceNumbers;
    const record = { id: "D", pieceNumbers: affected, disposition: "isolate" as const, executionIds: [], replacementPairs: [], note: "隔离", actorId: "operator", at: f.state.savedAt, reviews: ["production", "quality"].map((role) => ({ actorId: role, role: role as "quality", decision: "approve" as const, note: "确认", at: f.state.savedAt, revision: f.state.revision })) };
    change.dispositionRecords = [record];
    expect(factoryChangeClosureBlockers(f.state, change)[0]!.pieceNumbers).toEqual(affected);
    change.dispositionRecords[0]!.disposition = "use-as-is";
    expect(factoryChangeClosureBlockers(f.state, change)).toEqual([]);
    f.state.executions.push({ ...structuredClone(f.state.executions.at(-1)!), id: "LATEST-FAIL", pieceNumbers: affected, result: "fail" });
    expect(factoryChangeClosureBlockers(f.state, change)[0]!.message).toContain("逐件质量验证");
  });
  it("keeps every authorized piece/step actual record mandatory after retrospective change", () => {
    const f = fixture(), deviationId = f.run("operator", { type: "create-deviation", packageId: f.packet.id, reason: "先行", actual: "计划", pieceNumbers: f.packet.pieceNumbers, stepIds: ["MATERIAL"], risk: "风险", control: "控制" });
    const deviation = f.state.deviations[0]!, change = f.change();
    for (const role of ["production", "quality"] as const) f.run(role, { type: "review-deviation", deviationId, role, decision: "approve", note: "授权" });
    f.run("operator", { type: "record-execution", packageId: f.packet.id, stepId: "MATERIAL", kind: "material", pieceNumbers: [f.packet.pieceNumbers[0]!], quantity: 1, result: "pass", actual: "实际先行记录", deviationId });
    deviation.status = "reconciling"; deviation.changeId = change.id; change.status = "closed";
    const actual = f.state.executions.at(-1)!;
    expect(factoryDeviationClosureBlockers(f.state, deviation)[0]!.pieceNumbers).toEqual([f.packet.pieceNumbers[1]]);
    expect(() => f.run("quality", { type: "close-deviation", deviationId, resolution: "关闭" })).toThrow("缺少实际记录");
    const second = { ...structuredClone(actual), id: "SECOND", pieceNumbers: [f.packet.pieceNumbers[1]!], result: "fail" as const }; f.state.executions.push(second); deviation.executionIds.push(second.id);
    expect(factoryDeviationClosureBlockers(f.state, deviation)).toEqual([]); // Failed actual work is still a fact, not a pass.
  });
});
