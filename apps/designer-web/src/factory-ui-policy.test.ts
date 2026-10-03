import { describe, expect, it } from "vitest";
import { createEmptyDesign } from "@doormes/domain";
import { createRectangularWindowCommand, DesignSession } from "@doormes/application";
import { DESIGN_REVIEW_ROLES, type FactoryActor, type FactoryCommand, type FactoryRole } from "@doormes/contracts/factory-workflow";
import { applyFactoryCommand, createFactoryWorkspace } from "../../factory-api/src/workflow.js";
import { factoryCanEditDesign, factoryOwnsDesign, factoryReviewRoles, factoryExecutionKinds, factoryExecutablePieces } from "./factory-ui-policy.js";

const actor = (role: FactoryRole): FactoryActor => ({ id: "user-" + role, name: role, organizationId: "factory-prototype", roles: [role] });
const requirement = { widthMm: 1200, heightMm: 1500, material: "AL70", glass: "GL-LOWE-24", hardware: "HW-TT-STD", finish: "内RAL9016 / 外RAL7016", dueDate: "", note: "" };
function fixture() {
  const state = createFactoryWorkspace(), session = new DesignSession(createEmptyDesign("UI-POLICY"));
  session.execute(createRectangularWindowCommand({ commandId: "CREATE", windowId: "W1", mark: "C1", widthMm: 1200, heightMm: 1500, profileSystemId: requirement.material, defaultGlassTypeId: requirement.glass, defaultHardwareSetId: requirement.hardware }));
  const run = (role: FactoryRole, command: FactoryCommand) => applyFactoryCommand(state, actor(role), command, undefined, DESIGN_REVIEW_ROLES.map(actor).concat(actor("designer")));
  const designId = run("designer", { type: "save-design", document: session.document, name: "界面门禁", drawingNumber: "UI-POLICY" });
  const publish = () => {
    run("designer", { type: "submit-design", designId });
    for (const role of DESIGN_REVIEW_ROLES) run(role, { type: "review-design", designId, role, decision: "approve", note: "已核对" });
    return run("designer", { type: "release-design", designId, productCode: "UI-PRODUCT" });
  };
  const packet = () => {
    const releaseId = publish(), orderId = run("sales", { type: "create-order", number: "UI-SO", customer: "客户", kind: "standard", quantity: 2, releaseId, requirement });
    run("sales", { type: "confirm-order", orderId });
    const lineId = state.orders.find((entry) => entry.id === orderId)!.lines[0]!.id;
    const packageId = run("production", { type: "create-package", orderId, lineId, batch: "UI-BATCH" });
    return state.packages.find((entry) => entry.id === packageId)!;
  };
  return { state, run, designId, design: state.designs[0]!, publish, packet };
}
describe("factory UI actions mirror authoritative workflow gates", () => {
  it("locks submitted, approved and released drawings while retaining ownership for release", () => {
    const f = fixture(), designer = actor("designer");
    expect(factoryCanEditDesign(designer, f.state, f.design)).toBe(true);
    expect(factoryCanEditDesign(undefined, f.state, f.design)).toBe(false);
    expect(factoryCanEditDesign(actor("admin"), f.state, f.design)).toBe(false);
    expect(factoryCanEditDesign({ ...designer, organizationId: "OTHER" }, f.state, f.design)).toBe(false);
    f.run("designer", { type: "submit-design", designId: f.designId });
    expect(factoryCanEditDesign(designer, f.state, f.design)).toBe(false);
    for (const role of DESIGN_REVIEW_ROLES) f.run(role, { type: "review-design", designId: f.designId, role, decision: "approve", note: "核对" });
    expect(factoryOwnsDesign(designer, f.state, f.design)).toBe(true);
    expect(factoryCanEditDesign(designer, f.state, f.design)).toBe(false);
    f.run("designer", { type: "release-design", designId: f.designId, productCode: "P1" });
    expect(factoryCanEditDesign(designer, f.state, f.design)).toBe(false);
    expect(() => f.run("designer", { type: "save-design", designId: f.designId, name: "覆盖", drawingNumber: f.design.drawingNumber, document: f.design.document })).toThrow("不可直接改写");
  });
  it("restores editing on return and removes self-review even for a multi-role submitter", () => {
    const f = fixture(), multi = { ...actor("designer"), roles: ["designer", "reviewer"] as FactoryRole[] };
    f.run("designer", { type: "submit-design", designId: f.designId });
    expect(factoryReviewRoles(multi, f.state, "review-design", f.designId)).toEqual([]);
    expect(() => applyFactoryCommand(f.state, multi, { type: "review-design", designId: f.designId, role: "reviewer", decision: "approve", note: "自审" })).toThrow("不能会签自己");
    expect(factoryReviewRoles(actor("reviewer"), f.state, "review-design", f.designId, true)).toEqual(["reviewer"]);
    f.run("reviewer", { type: "review-design", designId: f.designId, role: "reviewer", decision: "reject", note: "退回修改" });
    expect(factoryCanEditDesign(actor("designer"), f.state, f.design)).toBe(true);
    expect(factoryReviewRoles(actor("reviewer"), f.state, "review-design", f.designId)).toEqual([]);
  });
  it("removes completed signatures from pending tasks but allows reconsideration in the full record", () => {
    const f = fixture();
    f.run("designer", { type: "submit-design", designId: f.designId });
    f.run("reviewer", { type: "review-design", designId: f.designId, role: "reviewer", decision: "approve", note: "已通过" });
    expect(factoryReviewRoles(actor("reviewer"), f.state, "review-design", f.designId, true)).toEqual([]);
    expect(factoryReviewRoles(actor("reviewer"), f.state, "review-design", f.designId)).toEqual(["reviewer"]);
    expect(factoryReviewRoles(actor("quality"), f.state, "review-design", f.designId, true)).toEqual(["quality"]);
    expect(factoryReviewRoles(actor("sales"), f.state, "review-design", f.designId)).toEqual([]);
    expect(factoryReviewRoles({ ...actor("quality"), organizationId: "OTHER" }, f.state, "review-design", f.designId)).toEqual([]);
    expect(factoryReviewRoles(actor("quality"), f.state, "review-design", "missing")).toEqual([]);
  });
  it("requires task claim and exact assignee, fails closed on missing task or withdrawn change", () => {
    const f = fixture(), id = f.run("sales", { type: "create-order", number: "CUSTOM", customer: "客户", kind: "custom", quantity: 1, requirement });
    const task = f.state.engineeringRequests.find((entry) => entry.orderId === id)!, design = f.state.designs.find((entry) => entry.id === task.designId)!;
    expect(factoryCanEditDesign(actor("designer"), f.state, design)).toBe(false);
    f.run("designer", { type: "claim-engineering", requestId: task.id });
    expect(factoryCanEditDesign(actor("designer"), f.state, design)).toBe(true);
    expect(factoryCanEditDesign({ ...actor("designer"), id: "second-designer" }, f.state, design)).toBe(false);
    expect(factoryCanEditDesign(actor("designer"), f.state, { ...design, engineeringRequestId: "MISSING" })).toBe(false);
    expect(factoryCanEditDesign(actor("designer"), f.state, { ...f.design, changeId: "MISSING" })).toBe(false);
    const packet = f.packet();
    for (const role of ["process", "procurement", "quality"] as const) f.run(role, { type: "review-package", packageId: packet.id, role, decision: "approve", note: "核对" });
    f.run("production", { type: "issue-package", packageId: packet.id });
    const changeId = f.run("sales", { type: "create-change", packageId: packet.id, reason: "变更", batch: "CH", fromStep: "CUT", pieceNumbers: [], disposition: "use-as-is", procurement: "无", cost: "无", delivery: "无" });
    const changed = f.state.designs.find((entry) => entry.changeId === changeId)!;
    expect(factoryCanEditDesign(actor("designer"), f.state, changed)).toBe(true);
    f.run("sales", { type: "withdraw-change", changeId, note: "撤回" });
    expect(factoryCanEditDesign(actor("designer"), f.state, changed)).toBe(false);
  });
  it("matches package, change and first-work self-review exclusions without granting administrators approval", () => {
    const f = fixture(), packet = f.packet();
    const owner = { ...actor("production"), roles: ["production", "quality"] as FactoryRole[] };
    expect(factoryReviewRoles(owner, f.state, "review-package", packet.id)).toEqual([]);
    expect(factoryReviewRoles(actor("admin"), f.state, "review-package", packet.id)).toEqual([]);
    expect(factoryReviewRoles(actor("quality"), f.state, "review-package", packet.id, true)).toEqual(["quality"]);
    for (const role of ["process", "procurement", "quality"] as const) f.run(role, { type: "review-package", packageId: packet.id, role, decision: "approve", note: "核对" });
    expect(factoryReviewRoles(actor("quality"), f.state, "review-package", packet.id)).toEqual([]);
    f.run("production", { type: "issue-package", packageId: packet.id }); f.run("operator", { type: "acknowledge-package", packageId: packet.id });
    const deviationId = f.run("quality", { type: "create-deviation", packageId: packet.id, reason: "先行", actual: "待补图", pieceNumbers: packet.pieceNumbers, stepIds: ["CUT"], risk: "核对", control: "隔离" });
    expect(factoryReviewRoles(actor("quality"), f.state, "review-deviation", deviationId)).toEqual([]);
    expect(factoryReviewRoles(actor("production"), f.state, "review-deviation", deviationId)).toEqual(["production"]);
    f.run("production", { type: "review-deviation", deviationId, role: "production", decision: "approve", note: "核对" });
    expect(factoryReviewRoles(actor("production"), f.state, "review-deviation", deviationId, true)).toEqual([]);
    const changeId = f.run("process", { type: "create-change", packageId: packet.id, reason: "改图", batch: "CH", fromStep: "CUT", pieceNumbers: [], disposition: "use-as-is", procurement: "无", cost: "无", delivery: "无" });
    // Pure UI state projection: the backend full change path is covered by workflow and HTTP tests.
    f.state.changes.find((entry) => entry.id === changeId)!.status = "impact-review";
    expect(factoryReviewRoles(actor("process"), f.state, "review-change", changeId)).toEqual([]);
    expect(factoryReviewRoles(actor("reviewer"), f.state, "review-change", changeId)).toEqual(["reviewer"]);
  });
  it("offers exact per-step record kinds for operator, quality and combined accounts", () => {
    expect(factoryExecutionKinds(actor("operator"), "MATERIAL")).toEqual(["material", "rework", "scrap"]);
    expect(factoryExecutionKinds(actor("operator"), "CUT")).toEqual(["process", "rework", "scrap"]);
    expect(factoryExecutionKinds(actor("quality"), "MATERIAL")).toEqual([]);
    expect(factoryExecutionKinds(actor("quality"), "QUALITY")).toEqual(["inspection"]);
    expect(factoryExecutionKinds(actor("operator"), "QUALITY")).not.toContain("inspection");
    expect(factoryExecutionKinds({ ...actor("operator"), roles: ["operator", "quality"] }, "QUALITY")).toEqual(["inspection", "rework", "scrap"]);
    expect(factoryExecutionKinds(actor("operator"), "QUALITY", true)).toEqual(["rework", "scrap"]);
    expect(factoryExecutionKinds(actor("quality"), "QUALITY", true)).toEqual([]);
  });
  it("retains earlier-step pieces and excludes only affected cutover pieces at and after the step", () => {
    const f = fixture(), packet = f.packet();
    expect(factoryExecutablePieces(f.state, packet, "MATERIAL")).toEqual([]);
    packet.status = "acknowledged";
    const changeId = f.run("sales", { type: "create-change", packageId: packet.id, reason: "局部", batch: "CH", fromStep: "CUT", pieceNumbers: [packet.pieceNumbers[0]!], disposition: "use-as-is", procurement: "无", cost: "无", delivery: "无" });
    const change = f.state.changes.find((entry) => entry.id === changeId)!;
    for (const status of ["designing", "approved", "cancelled"] as const) {
      change.status = status; expect(factoryExecutablePieces(f.state, packet, "CUT")).toEqual(packet.pieceNumbers);
    }
    for (const status of ["issued", "acknowledged", "closed"] as const) {
      change.status = status;
      expect(factoryExecutablePieces(f.state, packet, "MATERIAL")).toEqual(packet.pieceNumbers);
      expect(factoryExecutablePieces(f.state, packet, "CUT")).toEqual([packet.pieceNumbers[1]]);
      expect(factoryExecutablePieces(f.state, packet, "QUALITY")).toEqual([packet.pieceNumbers[1]]);
    }
    expect(factoryExecutablePieces(f.state, packet, "missing")).toEqual([]);
    change.effectivity.pieceNumbers = [];
    expect(factoryExecutablePieces(f.state, packet, "CUT")).toEqual([]);
    expect(factoryExecutablePieces(f.state, packet, "MATERIAL")).toEqual(packet.pieceNumbers);
  });
});
