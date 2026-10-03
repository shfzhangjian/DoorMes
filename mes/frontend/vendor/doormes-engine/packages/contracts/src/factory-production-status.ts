import { factoryAllocatedQuantity, factoryEffectivePieceNumbers, type FactoryChange, type FactoryDeviation, type FactoryExecution, type FactoryManufacturingPackage, type FactoryOrder, type FactoryWorkspace } from "@doormes/contracts/factory-workflow";

/** Read-only prerequisites shared by the workbench and service. These do not grant role permissions. */
export interface FactoryProductionBlocker {
  message: string; code: string; status: number; pieceNumbers: string[]; referenceIds: string[];
}
function blocker(message: string, pieceNumbers: string[] = [], referenceIds: string[] = [], code = "INVALID_STATE", status = 422): FactoryProductionBlocker {
  return { message, code, status, pieceNumbers, referenceIds };
}
const unfinished = (status: string) => !["closed", "cancelled"].includes(status);
const accepted = (reviews: { role: string; decision: string }[]) => ["production", "quality"].every((role) => reviews.some((entry) => entry.role === role && entry.decision === "approve"));
function uninspected(state: FactoryWorkspace, packet: FactoryManufacturingPackage, pieces: string[]): string[] {
  return pieces.filter((piece) => state.executions.filter((entry) => entry.packageId === packet.id && entry.kind === "inspection" && entry.pieceNumbers.includes(piece)).at(-1)?.result !== "pass");
}
export function factoryLastStepResult(state: FactoryWorkspace, packet: FactoryManufacturingPackage, piece: string, stepId: string, seen = new Set<string>()): FactoryExecution["result"] | undefined {
  if (seen.has(packet.id)) return undefined;
  seen.add(packet.id);
  const record = state.executions.filter((entry) => entry.packageId === packet.id && entry.pieceNumbers.includes(piece) && entry.stepId === stepId && !["scrap", "rework"].includes(entry.kind)).at(-1);
  if (record) return record.result;
  if (packet.changeId && packet.supersedes) {
    const change = state.changes.find((entry) => entry.id === packet.changeId), base = state.packages.find((entry) => entry.id === packet.supersedes);
    if (!change || !base) return undefined;
    const index = base.route.findIndex((entry) => entry.id === stepId), cutover = base.route.findIndex((entry) => entry.id === change.effectivity.fromStep);
    if (index >= 0 && index < cutover && !["replace", "scrap"].includes(change.disposition)) return factoryLastStepResult(state, base, piece, stepId, seen);
  }
  return undefined;
}
/** Step-local range retains pre-cutover work on the original package, even after piece transfer. */
export function factoryStepPieceNumbers(state: FactoryWorkspace, packet: FactoryManufacturingPackage, stepId: string): string[] {
  if (packet.status === "cancelled") return [];
  const stepIndex = packet.route.findIndex((entry) => entry.id === stepId);
  if (stepIndex < 0) return [];
  const transferred = new Set(state.changes.filter((entry) => entry.basePackageId === packet.id && ["issued", "acknowledged", "closed"].includes(entry.status) && stepIndex >= packet.route.findIndex((step) => step.id === entry.effectivity.fromStep)).flatMap((entry) => entry.effectivity.pieceNumbers.length ? entry.effectivity.pieceNumbers : packet.pieceNumbers));
  return packet.pieceNumbers.filter((piece) => !transferred.has(piece));
}
/** Quantity-only amendments never mutate frozen drawings, packages or execution facts. */
export function factoryOrderProductionStatus(state: FactoryWorkspace, order: FactoryOrder): FactoryOrder["status"] {
  if (order.status === "draft") return "draft";
  const active = state.packages.filter((entry) => entry.orderId === order.id && !["cancelled", "superseded"].includes(entry.status));
  if (order.lines.every((line) => factoryAllocatedQuantity(state.packages, line.id) === line.quantity) && active.length && active.every((entry) => entry.status === "complete")) return "complete";
  if (active.some((entry) => entry.status === "acknowledged")) return "in-production";
  if (active.some((entry) => entry.status === "issued")) return "issued";
  return "confirmed";
}
export function factoryPackageCancellationBlockers(state: FactoryWorkspace, packet: FactoryManufacturingPackage): FactoryProductionBlocker[] {
  const result: FactoryProductionBlocker[] = [];
  if (packet.supersedes || packet.changeId || !["review", "approved"].includes(packet.status)) result.push(blocker("仅未下达的原始制造批次可撤销；已下达或变更批次须走受控变更"));
  if (state.executions.some((entry) => entry.packageId === packet.id) || state.deviations.some((entry) => entry.packageId === packet.id) || state.changes.some((entry) => entry.basePackageId === packet.id || entry.newPackageId === packet.id)) result.push(blocker("存在实际执行、先行或变更关联的批次不能直接撤销"));
  return result;
}
export function factoryPackageCompletionBlockers(state: FactoryWorkspace, packet: FactoryManufacturingPackage): FactoryProductionBlocker[] {
  const result: FactoryProductionBlocker[] = [];
  if (packet.status !== "acknowledged") result.push(blocker("制造包尚未接收或已经结束"));
  const deviations = state.deviations.filter((entry) => entry.packageId === packet.id && unfinished(entry.status));
  if (deviations.length) result.push(blocker("存在未闭环偏差", [], deviations.map((entry) => entry.id)));
  const pieces = uninspected(state, packet, factoryEffectivePieceNumbers(state, packet));
  if (pieces.length) result.push(blocker("全部有效生产件须有最新合格终检记录", pieces));
  const changes = state.changes.filter((entry) => entry.basePackageId === packet.id && unfinished(entry.status));
  if (changes.length) result.push(blocker("关联工程变更尚未闭环", [], changes.map((entry) => entry.id)));
  return result;
}
export function factoryChangeIssueBlockers(state: FactoryWorkspace, change: FactoryChange): FactoryProductionBlocker[] {
  const result: FactoryProductionBlocker[] = [], line = state.orders.find((entry) => entry.id === change.orderId)?.lines.find((entry) => entry.id === change.lineId);
  if (change.status !== "approved" || !change.newReleaseId || (change.customerConfirmation?.releaseId ?? line?.customerConfirmedReleaseId) !== change.newReleaseId) result.push(blocker("变更需会签通过且客户确认新版本后下达"));
  const base = state.packages.find((entry) => entry.id === change.basePackageId), release = state.releases.find((entry) => entry.id === change.newReleaseId);
  if (!base || !release) { result.push(blocker("指定业务对象不存在", [], [], "NOT_FOUND", 404)); return result; }
  if (!release.calculation.route.some((entry) => entry.id === change.effectivity.fromStep)) result.push(blocker("新工艺路线须保留生效工序编号，请修订并重新会签"));
  const pieces = change.effectivity.pieceNumbers.length ? change.effectivity.pieceNumbers : base.pieceNumbers;
  const current = state.executions.filter((entry) => entry.packageId === base.id && entry.pieceNumbers.some((piece) => pieces.includes(piece))).map((entry) => entry.id);
  if (JSON.stringify(current) !== JSON.stringify(change.impact.executionIds)) result.push(blocker("会签后在制品记录有变化，请重新评估并会签", [], current, "STALE_IMPACT", 409));
  if (base.releaseId !== change.baseReleaseId) result.push(blocker("基础制造包版本已变化，请重新评估基线", [], [], "CONFLICT", 409));
  const effective = factoryEffectivePieceNumbers(state, base), unavailable = pieces.filter((piece) => !effective.includes(piece));
  if (!pieces.length || unavailable.length) result.push(blocker("涉及件号已切换其他版本或不属于当前有效范围，请从生效制造包发起变更", unavailable, [], "CONFLICT", 409));
  const overlaps = state.changes.filter((entry) => entry.id !== change.id && entry.basePackageId === base.id && unfinished(entry.status) && (entry.effectivity.pieceNumbers.length ? entry.effectivity.pieceNumbers : base.pieceNumbers).some((piece) => pieces.includes(piece)));
  if (overlaps.length) result.push(blocker("涉及件号已有未结束变更，请修订或撤回原变更", pieces, overlaps.map((entry) => entry.id), "CONFLICT", 409));
  return result;
}
export function factoryChangeClosureBlockers(state: FactoryWorkspace, change: FactoryChange): FactoryProductionBlocker[] {
  if (change.status !== "acknowledged" || !change.newPackageId) return [blocker("变更尚未执行接收")];
  const packet = state.packages.find((entry) => entry.id === change.newPackageId), base = state.packages.find((entry) => entry.id === change.basePackageId);
  if (!packet || !base) return [blocker("指定业务对象不存在", [], [], "NOT_FOUND", 404)];
  const result: FactoryProductionBlocker[] = [], deviations = state.deviations.filter((entry) => entry.packageId === packet.id && unfinished(entry.status));
  if (deviations.length) result.push(blocker("新版本仍有未闭环先行偏差", [], deviations.map((entry) => entry.id)));
  const pieces = uninspected(state, packet, packet.pieceNumbers);
  if (pieces.length) result.push(blocker("新版本须完成最新逐件质量验证", pieces));
  const confirmed = (change.dispositionRecords ?? []).filter((entry) => entry.disposition !== "isolate" && accepted(entry.reviews));
  const missing = (change.effectivity.pieceNumbers.length ? change.effectivity.pieceNumbers : base.pieceNumbers).filter((piece) => !confirmed.some((entry) => entry.pieceNumbers.includes(piece)));
  if (missing.length) result.push(blocker("旧件须逐件完成最终处置并由生产、质量分别确认；隔离记录不能作为闭环", missing));
  return result;
}
/** All members are preflighted before any package is created; one stale batch blocks the whole issue. */
export function factoryChangeSchemeIssueBlockers(state: FactoryWorkspace, schemeId: string): FactoryProductionBlocker[] {
  const scheme = state.changeSchemes?.find((entry) => entry.id === schemeId);
  if (!scheme || scheme.changeIds.length < 2) return [blocker("统一变更方案不存在或范围不足", [], [], "NOT_FOUND", 404)];
  return scheme.changeIds.flatMap((id) => {
    const change = state.changes.find((entry) => entry.id === id);
    if (!change || change.schemeId !== schemeId) return [blocker("统一变更成员不存在", [], [id], "NOT_FOUND", 404)];
    const batch = state.packages.find((entry) => entry.id === change.basePackageId)?.batch ?? change.basePackageId;
    return factoryChangeIssueBlockers(state, change).map((entry) => ({ ...entry, message: "批次 " + batch + "：" + entry.message, referenceIds: [change.id, ...entry.referenceIds] }));
  });
}
/** Derived overview only: a member's closure never silently closes the other batches. */
export function factoryChangeSchemeStatus(state: FactoryWorkspace, schemeId: string): string {
  const scheme = state.changeSchemes?.find((entry) => entry.id === schemeId);
  const members = scheme?.changeIds.map((id) => state.changes.find((entry) => entry.id === id));
  if (!members?.length || members.some((entry) => !entry)) return "invalid";
  const statuses = members.map((entry) => entry!.status);
  if (statuses.every((status) => status === statuses[0])) return statuses[0]!;
  if (statuses.includes("closed")) return "partially-closed";
  if (statuses.includes("acknowledged")) return "partially-acknowledged";
  return "invalid";
}
export function factoryDeviationClosureBlockers(state: FactoryWorkspace, deviation: FactoryDeviation): FactoryProductionBlocker[] {
  const result: FactoryProductionBlocker[] = [];
  if (deviation.status !== "reconciling" || !deviation.changeId || !state.changes.some((entry) => entry.id === deviation.changeId && entry.status === "closed") || !deviation.executionIds.length) result.push(blocker("先行执行记录、补图变更与质量对账尚未闭环"));
  const missing = deviation.scope?.pieceNumbers.filter((piece) => deviation.scope.stepIds.some((stepId) => !state.executions.some((entry) => entry.deviationId === deviation.id && entry.stepId === stepId && entry.pieceNumbers.includes(piece))));
  if (!deviation.scope || missing?.length) result.push(blocker("先行授权范围还有生产件或工序缺少实际记录，不能整体闭环", missing ?? []));
  return result;
}
