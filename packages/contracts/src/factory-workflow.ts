import type { DesignDocument } from "./index.js";

/** Versioned JSON contract shared by the factory HTTP service and workbenches. */
export const FACTORY_SCHEMA = "doormes-factory-workspace.v1" as const;
export const FACTORY_ROLES = ["sales", "designer", "reviewer", "process", "procurement", "production", "operator", "quality", "admin"] as const;
export type FactoryRole = typeof FACTORY_ROLES[number];
export const FACTORY_ROLE_LABELS: Record<FactoryRole, string> = {
  sales: "订单销售", designer: "设计研发", reviewer: "设计审核", process: "工艺工程",
  procurement: "采购", production: "生产主管", operator: "车间执行", quality: "质量", admin: "系统管理"
};
export const DESIGN_REVIEW_ROLES = ["reviewer", "process", "procurement", "production", "quality"] as const;
export interface FactoryActor { id: string; name: string; roles: FactoryRole[]; organizationId: string }
export interface FactoryReview { actorId: string; role: FactoryRole; decision: "approve" | "reject"; note: string; at: string; revision: number }
export interface FactoryRequirement {
  widthMm: number; heightMm: number; material: string; glass: string; hardware: string;
  finish: string; dueDate: string; note: string;
}
/** Frozen summary; full geometry and per-component choices remain in the release document. */
export interface FactoryProductSpecification {
  schemaVersion: "doormes-factory-product.v1";
  widthMm: number; heightMm: number; material: string; glass: string; hardware: string; finish: string;
  units: { id: string; mark: string; kind: "window" | "assembly"; widthMm: number; heightMm: number; isCoplanar: boolean }[];
  windows: { id: string; mark: string; widthMm: number; heightMm: number; quantity: number; material: string; glass: string; hardware: string; finish: string }[];
}
export interface FactoryBomLine {
  id: string; sourceObjectId: string; category: string; materialCode: string; name: string;
  sourceMark: string; grossLengthMm: number; material: string; color: string;
  specification: string; lengthMm: number; widthMm: number; heightMm: number;
  cutLeftDeg: number; cutRightDeg: number; quantity: number; unit: string;
}
export interface FactoryRouteStep { id: string; name: string; instruction: string; sourceObjectIds: string[]; templateId?: string }
export interface FactoryCalculation {
  fingerprint: string; catalogVersion: string; basis: "reference-simulation";
  lines: FactoryBomLine[]; route: FactoryRouteStep[]; blockingCodes: string[];
  productionEligible: false;
  processingParameters: unknown[];
}
export interface FactoryDesign {
  id: string; name: string; drawingNumber: string; version: number;
  document: DesignDocument; createdBy: string; updatedAt: string; submittedBy?: string;
  status: "draft" | "in-review" | "returned" | "approved" | "released";
  reviews: FactoryReview[]; calculation?: FactoryCalculation;
  manufacturingRoute?: FactoryRouteStep[];
  baseReleaseId?: string; engineeringRequestId?: string; changeId?: string;
}
export type FactoryReleaseScope = { kind: "catalog" } | { kind: "order-line"; orderId: string; lineId: string };
export interface FactoryRelease {
  id: string; designId: string; productCode: string; name: string; drawingNumber: string;
  version: number; designVersion: number; document: DesignDocument;
  calculation: FactoryCalculation; reviews: FactoryReview[];
  publishedBy: string; publishedAt: string; supersedes?: string;
  usage: "prototype"; productionEligible: false;
  specification?: FactoryProductSpecification;
  scope?: FactoryReleaseScope;
}
export interface FactoryOrderLine {
  id: string; mark: string; kind: "standard" | "custom"; quantity: number;
  requirementRevision?: number;
  requirement: FactoryRequirement; releaseId?: string; engineeringRequestId?: string;
  customerConfirmedReleaseId?: string;
}
export interface FactoryOrder {
  id: string; number: string; customer: string; revision: number;
  status: "draft" | "confirmed" | "issued" | "in-production" | "complete";
  lines: FactoryOrderLine[]; createdBy: string; updatedAt: string;
  quantityRevisions?: { lineId: string; fromQuantity: number; toQuantity: number; fromRevision: number; toRevision: number; note: string; actorId: string; at: string }[];
  requirementRevisions?: FactoryRequirementRevision[];
}
/** Append-only demand snapshots; withdrawing a pre-issue baseline never overwrites its drawing/reviews. */
export interface FactoryRequirementRevision {
  lineId: string; fromRevision: number; toRevision: number;
  fromRequirementRevision: number; toRequirementRevision: number;
  before: FactoryRequirement; after: FactoryRequirement;
  fromKind: FactoryOrderLine["kind"]; toKind: FactoryOrderLine["kind"];
  fromReleaseId?: string; toReleaseId?: string; fromRequestId?: string; toRequestId?: string;
  cancelledPackageIds: string[]; differences: FactoryDifference[];
  note: string; actorId: string; at: string;
}
export interface FactoryEngineeringRequest {
  id: string; orderId: string; lineId: string; requirement: FactoryRequirement;
  designId: string; status: "unassigned" | "assigned" | "designing" | "in-review" | "returned" | "released";
  referenceReleaseId?: string; assigneeId?: string; assignedBy?: string; assignedAt?: string; claimedAt?: string;
  requirementRevision?: number; orderRevision?: number; previousRequestId?: string;
}
export interface FactoryManufacturingPackage {
  id: string; orderId: string; orderRevision: number; lineId: string; releaseId: string;
  version: number; quantity: number; batch: string; bom: FactoryBomLine[];
  route: FactoryRouteStep[]; pieceNumbers: string[]; productionEligible: false;
  status: "review" | "approved" | "issued" | "acknowledged" | "complete" | "superseded" | "cancelled";
  reviews: FactoryReview[]; createdAt: string; createdBy: string;
  supersedes?: string; changeId?: string;
  processingParameters: unknown[];
  requirementRevision?: number;
  cancellation?: { previousStatus: "review" | "approved"; note: string; actorId: string; at: string; orderRevision: number };
}
export interface FactoryDifference { path: string; before: unknown; after: unknown; category: "geometry" | "material" | "bom" | "route" | "other" }
/** Read-only comparison of exact published baselines, never of the editable drawing session. */
export interface FactoryReleaseComparison { before?: FactoryRelease; after: FactoryRelease; differences: FactoryDifference[] }
export type FactoryDisposition = "use-as-is" | "rework" | "scrap" | "replace" | "isolate";
/** Append-only evidence; rejected records remain visible and are replaced by new records. */
export interface FactoryDispositionRecord {
  id: string; pieceNumbers: string[]; disposition: FactoryDisposition;
  executionIds: string[]; replacementPairs: { oldPiece: string; newPiece: string }[];
  note: string; actorId: string; at: string; reviews: FactoryReview[];
}
export interface FactoryChange {
  id: string; orderId: string; lineId: string; basePackageId: string; baseReleaseId: string;
  reason: string; createdBy: string; createdAt: string; designId: string;
  status: "designing" | "impact-review" | "approved" | "issued" | "acknowledged" | "closed" | "cancelled";
  schemeId?: string;
  newReleaseId?: string; newPackageId?: string; differences: FactoryDifference[];
  impact: { executionIds: string[]; procurement: string; cost: string; delivery: string };
  effectivity: { batch: string; fromStep: string; pieceNumbers: string[] };
  disposition: FactoryDisposition;
  reviews: FactoryReview[];
  dispositionRecords?: FactoryDispositionRecord[];
  customerConfirmation?: { releaseId: string; actorId: string; at: string };
  cancellation?: { note: string; actorId: string; at: string };
  closure?: { note: string; actorId: string; at: string };
}
/** A shared design/approval transaction; each member retains its own actual baseline and execution scope.
 * The first member owns the shared drawing. Post-issue receipt/disposal/closure remain per batch.
 */
export interface FactoryChangeScheme {
  id: string; name: string; createdBy: string; createdAt: string; changeIds: string[];
}
export interface FactoryChangeScope {
  packageId: string; batch: string; fromStep: string; pieceNumbers: string[]; disposition: FactoryDisposition;
}
export interface FactoryExecution {
  id: string; packageId: string; orderId: string; lineId: string; batch: string;
  stepId: string; pieceNumbers: string[]; kind: "material" | "process" | "inspection" | "rework" | "scrap";
  quantity: number; result: "pass" | "fail" | "recorded"; actual: string;
  actorId: string; at: string; deviationId?: string;
}
export interface FactoryDeviation {
  id: string; packageId: string; reason: string; actual: string; createdBy: string; createdAt: string;
  status: "requested" | "returned" | "authorized" | "reconciling" | "closed" | "cancelled";
  scope: { pieceNumbers: string[]; stepIds: string[]; risk: string; control: string };
  reviews: FactoryReview[]; executionIds: string[]; changeId?: string;
  resolution?: string; closedBy?: string; closedAt?: string;
}
export interface FactoryAuditEvent { id: string; actorId: string; at: string; action: string; targetId: string; workspaceRevision: number; note: string }
export interface FactoryWorkspace {
  schemaVersion: typeof FACTORY_SCHEMA; organizationId: string; revision: number; savedAt: string;
  designs: FactoryDesign[]; releases: FactoryRelease[]; orders: FactoryOrder[];
  engineeringRequests: FactoryEngineeringRequest[]; packages: FactoryManufacturingPackage[];
  changes: FactoryChange[]; changeSchemes?: FactoryChangeScheme[]; deviations: FactoryDeviation[]; executions: FactoryExecution[];
  audit: FactoryAuditEvent[]; receipts: { key: string; actorId: string; digest: string; targetId: string }[];
}
/** Replacement packages consume already allocated pieces, not additional order quantity. */
export function factoryAllocatedQuantity(packages: readonly FactoryManufacturingPackage[], lineId: string): number {
  return packages.filter((entry) => entry.lineId === lineId && !entry.supersedes && entry.status !== "cancelled").reduce((total, entry) => total + entry.quantity, 0);
}
/** Old task snapshots remain readable, but cannot be claimed, edited, signed or published. */
export function factoryEngineeringRequestCurrent(state: Pick<FactoryWorkspace, "orders">, task: FactoryEngineeringRequest): boolean {
  const line = state.orders.find((entry) => entry.id === task.orderId)?.lines.find((entry) => entry.id === task.lineId);
  return !!line && line.engineeringRequestId === task.id && (line.requirementRevision ?? 1) === (task.requirementRevision ?? 1);
}
/** Resolve the frozen demand version without using the latest line or recalculating historical drawings. */
export function factoryRequirementSnapshot(order: FactoryOrder, lineId: string, revision: number): FactoryRequirement | undefined {
  const line = order.lines.find((entry) => entry.id === lineId);
  if (!line) return;
  if ((line.requirementRevision ?? 1) === revision) return structuredClone(line.requirement);
  const record = order.requirementRevisions?.find((entry) => entry.lineId === lineId && entry.fromRequirementRevision === revision);
  return record ? structuredClone(record.before) : undefined;
}
/** Recover scope for legacy snapshots without rewriting historical releases. */
export function factoryReleaseScope(state: Pick<FactoryWorkspace, "releases" | "designs" | "changes" | "engineeringRequests">, release: FactoryRelease, seen = new Set<string>()): FactoryReleaseScope {
  if (release.scope) return release.scope;
  if (seen.has(release.id)) throw new Error("图纸发布引用形成循环");
  seen.add(release.id);
  const change = state.changes.find((entry) => entry.newReleaseId === release.id || entry.designId === release.designId);
  const task = state.engineeringRequests.find((entry) => entry.designId === release.designId);
  if (change || task) return { kind: "order-line", orderId: (change ?? task)!.orderId, lineId: (change ?? task)!.lineId };
  const parent = state.releases.find((entry) => entry.id === release.supersedes);
  return parent ? factoryReleaseScope(state, parent, seen) : { kind: "catalog" };
}
/** Original pieces already cut over remain in history but no longer execute in this package. */
export function factoryEffectivePieceNumbers(state: Pick<FactoryWorkspace, "changes">, packet: FactoryManufacturingPackage): string[] {
  if (packet.status === "cancelled") return [];
  const transferred = new Set(state.changes.filter((entry) => entry.basePackageId === packet.id && ["issued", "acknowledged", "closed"].includes(entry.status)).flatMap((entry) => entry.effectivity.pieceNumbers.length ? entry.effectivity.pieceNumbers : packet.pieceNumbers));
  return packet.pieceNumbers.filter((piece) => !transferred.has(piece));
}
/** All writes carry the workspace baseline and a retry-stable idempotency key. */
export type FactoryCommand =
  | { type: "assign-engineering"; requestId: string; assigneeId: string; note: string }
  | { type: "claim-engineering"; requestId: string }
  | { type: "save-design"; designId?: string; name: string; drawingNumber: string; document: DesignDocument }
  | { type: "fork-release"; releaseId: string; name: string }
  | { type: "submit-design"; designId: string }
  | { type: "set-design-route"; designId: string; route: FactoryRouteStep[] }
  | { type: "review-design"; designId: string; role: FactoryRole; decision: "approve" | "reject"; note: string }
  | { type: "release-design"; designId: string; productCode: string }
  | { type: "create-order"; number: string; customer: string; kind: "standard" | "custom"; quantity: number; requirement: FactoryRequirement; releaseId?: string }
  | { type: "add-order-line"; orderId: string; kind: "standard" | "custom"; quantity: number; requirement: FactoryRequirement; releaseId?: string }
  | { type: "confirm-order"; orderId: string }
  | { type: "revise-order-quantity"; orderId: string; lineId: string; quantity: number; note: string }
  | { type: "revise-order-requirement"; orderId: string; lineId: string; kind: "standard" | "custom"; requirement: FactoryRequirement; releaseId?: string; note: string }
  | { type: "confirm-customer"; orderId: string; lineId: string; releaseId: string }
  | { type: "create-package"; orderId: string; lineId: string; batch: string; quantity?: number }
  | { type: "cancel-package"; packageId: string; note: string }
  | { type: "review-package"; packageId: string; role: FactoryRole; decision: "approve" | "reject"; note: string }
  | { type: "issue-package"; packageId: string }
  | { type: "acknowledge-package"; packageId: string }
  | { type: "record-execution"; packageId: string; stepId: string; kind: FactoryExecution["kind"]; quantity: number; pieceNumbers: string[]; result: FactoryExecution["result"]; actual: string; deviationId?: string }
  | { type: "complete-package"; packageId: string }
  | { type: "create-change"; packageId: string; reason: string; batch: string; fromStep: string; pieceNumbers: string[]; disposition: FactoryChange["disposition"]; procurement: string; cost: string; delivery: string }
  | { type: "create-change-scheme"; name: string; reason: string; procurement: string; cost: string; delivery: string; scopes: FactoryChangeScope[] }
  | { type: "submit-change-scheme" | "revise-change-scheme" | "refresh-change-scheme-impact" | "issue-change-scheme"; schemeId: string }
  | { type: "withdraw-change-scheme"; schemeId: string; note: string }
  | { type: "review-change-scheme"; schemeId: string; role: FactoryRole; decision: "approve" | "reject"; note: string }
  | { type: "submit-change"; changeId: string }
  | { type: "revise-change"; changeId: string }
  | { type: "refresh-change-impact"; changeId: string }
  | { type: "withdraw-change"; changeId: string; note: string }
  | { type: "review-change"; changeId: string; role: FactoryRole; decision: "approve" | "reject"; note: string }
  | { type: "issue-change"; changeId: string }
  | { type: "acknowledge-change"; changeId: string }
  | { type: "record-change-disposition"; changeId: string; pieceNumbers: string[]; disposition: FactoryDisposition; executionIds: string[]; replacementPairs: { oldPiece: string; newPiece: string }[]; note: string }
  | { type: "review-change-disposition"; changeId: string; recordId: string; role: FactoryRole; decision: "approve" | "reject"; note: string }
  | { type: "close-change"; changeId: string; note: string }
  | { type: "create-deviation"; packageId: string; reason: string; actual: string; pieceNumbers: string[]; stepIds: string[]; risk: string; control: string }
  | { type: "revise-deviation"; deviationId: string; reason: string; actual: string; pieceNumbers: string[]; stepIds: string[]; risk: string; control: string }
  | { type: "withdraw-deviation"; deviationId: string; note: string }
  | { type: "review-deviation"; deviationId: string; role: FactoryRole; decision: "approve" | "reject"; note: string }
  | { type: "link-deviation-change"; deviationId: string; changeId: string }
  | { type: "close-deviation"; deviationId: string; resolution: string };
export interface FactoryMutationRequest { expectedRevision: number; idempotencyKey: string; command: FactoryCommand }
export interface FactoryApiError { error: { code: string; message: string; currentRevision?: number } }
export interface FactoryMutationResponse { workspace: FactoryWorkspace; targetId: string; replayed: boolean }
