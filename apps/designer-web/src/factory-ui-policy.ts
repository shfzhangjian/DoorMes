import { DESIGN_REVIEW_ROLES, factoryEngineeringRequestCurrent, type FactoryActor, type FactoryDesign, type FactoryExecution, type FactoryManufacturingPackage, type FactoryReview, type FactoryRole, type FactoryWorkspace } from "@doormes/contracts/factory-workflow";
import { factoryStepPieceNumbers } from "@doormes/contracts/factory-production-status";

export type FactoryReviewType = "review-design" | "review-package" | "review-change" | "review-change-scheme" | "review-deviation";
/** UI affordances only; HTTP permissions and transitions remain authoritative. */
export function factoryOwnsDesign(actor: FactoryActor | undefined, state: FactoryWorkspace, design: FactoryDesign): boolean {
  if (!actor || actor.organizationId !== state.organizationId || !actor.roles.includes("designer")) return false;
  if (design.engineeringRequestId) {
    const task = state.engineeringRequests.find((entry) => entry.id === design.engineeringRequestId);
    if (!task || !factoryEngineeringRequestCurrent(state, task) || task.assigneeId !== actor.id || !task.claimedAt) return false;
  }
  if (design.changeId) {
    const change = state.changes.find((entry) => entry.id === design.changeId);
    if (!change || change.status !== "designing" || change.designId !== design.id) return false;
  }
  return true;
}
export function factoryCanEditDesign(actor: FactoryActor | undefined, state: FactoryWorkspace, design: FactoryDesign): boolean {
  return ["draft", "returned"].includes(design.status) && factoryOwnsDesign(actor, state, design);
}
function reviewContext(state: FactoryWorkspace, type: FactoryReviewType, id: string): { owner: string; roles: readonly FactoryRole[]; reviews: FactoryReview[] } | undefined {
  if (type === "review-design") {
    const design = state.designs.find((entry) => entry.id === id);
    if (!design || design.status !== "in-review") return;
    if (design.engineeringRequestId && !state.engineeringRequests.some((entry) => entry.id === design.engineeringRequestId && factoryEngineeringRequestCurrent(state, entry))) return;
    if (design.changeId && !state.changes.some((entry) => entry.id === design.changeId && entry.status === "designing" && entry.designId === design.id)) return;
    return { owner: design.submittedBy ?? design.createdBy, roles: DESIGN_REVIEW_ROLES, reviews: design.reviews };
  }
  if (type === "review-package") {
    const packet = state.packages.find((entry) => entry.id === id);
    if (packet?.status === "review") return { owner: packet.createdBy, roles: ["process", "procurement", "quality"], reviews: packet.reviews };
  }
  if (type === "review-change") {
    const change = state.changes.find((entry) => entry.id === id);
    if (change?.status === "impact-review" && !change.schemeId) return { owner: change.createdBy, roles: DESIGN_REVIEW_ROLES, reviews: change.reviews };
  }
  if (type === "review-change-scheme") {
    const scheme = state.changeSchemes?.find((entry) => entry.id === id);
    const change = state.changes.find((entry) => entry.id === scheme?.changeIds[0]);
    if (scheme && change?.status === "impact-review") return { owner: scheme.createdBy, roles: DESIGN_REVIEW_ROLES, reviews: change.reviews };
  }
  if (type === "review-deviation") {
    const deviation = state.deviations.find((entry) => entry.id === id);
    if (deviation?.status === "requested") return { owner: deviation.createdBy, roles: ["production", "quality"], reviews: deviation.reviews };
  }
}
export function factoryReviewRoles(actor: FactoryActor | undefined, state: FactoryWorkspace, type: FactoryReviewType, id: string, pendingOnly = false): FactoryRole[] {
  if (!actor || actor.organizationId !== state.organizationId) return [];
  const target = reviewContext(state, type, id);
  if (!target || target.owner === actor.id) return [];
  // Signed roles can reconsider during review but no longer count as pending.
  return target.roles.filter((role) => actor.roles.includes(role) && (!pendingOnly || !target.reviews.some((entry) => entry.role === role && entry.decision === "approve")));
}
export function factoryExecutionKinds(actor: FactoryActor, stepId: string, disposalOnly = false): FactoryExecution["kind"][] {
  const kinds: FactoryExecution["kind"][] = [];
  if (!disposalOnly) {
    if (stepId === "QUALITY") { if (actor.roles.includes("quality")) kinds.push("inspection"); }
    else if (actor.roles.includes("operator")) kinds.push(stepId === "MATERIAL" ? "material" : "process");
  }
  if (actor.roles.includes("operator")) kinds.push("rework", "scrap");
  return kinds;
}
/** Only affected pieces at/after cutover are removed; earlier steps remain executable. */
export function factoryExecutablePieces(state: FactoryWorkspace, packet: FactoryManufacturingPackage, stepId: string): string[] {
  return packet.status === "acknowledged" ? factoryStepPieceNumbers(state, packet, stepId) : [];
}
