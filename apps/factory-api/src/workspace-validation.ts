import Ajv from "ajv";
import { FACTORY_WORKSPACE_JSON_SCHEMA } from "@doormes/contracts/factory-workspace-schema";
import { DESIGN_REVIEW_ROLES, factoryAllocatedQuantity, factoryReleaseScope, factoryRequirementSnapshot, factoryEngineeringRequestCurrent, type FactoryWorkspace, type FactoryReview } from "@doormes/contracts/factory-workflow";
import { parseFormalDesignDocument } from "@doormes/persistence/local-design";
import { check, compareFactoryVersions } from "./workflow.js";
import { isDeepStrictEqual } from "node:util";

const validate = new Ajv({ allErrors: false }).compile(FACTORY_WORKSPACE_JSON_SCHEMA);
function valid(condition: unknown, location: string): asserts condition {
  check(condition, "后端业务文件内容无效：" + location + "；原文件未被覆盖", "CORRUPT_FILE", 500);
}
function unique(values: readonly string[], location: string): void { valid(values.length === new Set(values).size, location + "编号重复"); }
function reviews(entries: FactoryReview[], location: string, required: readonly string[] = []): void {
  unique(entries.map((entry) => entry.role), location + "会签岗位");
  valid(required.every((role) => entries.some((entry) => entry.role === role && entry.decision === "approve")), location + "缺少批准会签");
}
/** Validate stored shape and referential integrity without recalculating history with today's rules.
 * Called before commit, head recovery and historical reads. Never repairs business evidence silently.
 * Legacy optional fields stay absent; they do not imply new authorization or catalog promotion.
 */
export function validateFactoryWorkspace(value: unknown): asserts value is FactoryWorkspace {
  valid(validate(value), "JSON字段" + (validate.errors?.[0]?.instancePath ?? ""));
  const state = value as FactoryWorkspace;
  valid(state.organizationId === "factory-prototype", "组织");
  const collections = [state.designs, state.releases, state.orders, state.engineeringRequests, state.packages, state.changes, state.changeSchemes ?? [], state.deviations, state.executions, state.audit];
  for (const entries of collections) unique(entries.map((entry) => entry.id), "集合");
  const lines = state.orders.flatMap((entry) => entry.lines); unique(lines.map((entry) => entry.id), "订单行");
  unique(state.orders.map((entry) => entry.number), "订单号");
  unique(state.receipts.map((entry) => JSON.stringify([entry.actorId, entry.key])), "幂等回执");
  const designs = new Map(state.designs.map((entry) => [entry.id, entry]));
  const releases = new Map(state.releases.map((entry) => [entry.id, entry]));
  const orders = new Map(state.orders.map((entry) => [entry.id, entry]));
  const packages = new Map(state.packages.map((entry) => [entry.id, entry]));
  const changes = new Map(state.changes.map((entry) => [entry.id, entry]));
  const schemes = new Map((state.changeSchemes ?? []).map((entry) => [entry.id, entry]));
  const deviations = new Map(state.deviations.map((entry) => [entry.id, entry]));
  const executions = new Map(state.executions.map((entry) => [entry.id, entry]));
  const tasks = new Map(state.engineeringRequests.map((entry) => [entry.id, entry]));
  const lineFor = (orderId: string, lineId: string) => {
    const line = orders.get(orderId)?.lines.find((entry) => entry.id === lineId); valid(line, "订单/行引用"); return line;
  };
  const route = (entries: { id: string }[], location: string) => {
    unique(entries.map((entry) => entry.id), location + "工序");
    valid(entries.length >= 3 && entries[0]!.id === "MATERIAL" && entries.at(-1)!.id === "QUALITY", location + "首末工序");
  };
  for (const entry of [...state.designs, ...state.releases]) {
    try { parseFormalDesignDocument(entry.document); } catch { valid(false, "绘图文档"); }
    reviews(entry.reviews, "图纸");
    if (entry.calculation) { route(entry.calculation.route, "计算"); unique(entry.calculation.lines.map((line) => line.id), "BOM"); }
  }
  for (const design of state.designs) {
    if (design.baseReleaseId) valid(releases.has(design.baseReleaseId), "设计基线引用");
    if (design.engineeringRequestId) valid(tasks.get(design.engineeringRequestId)?.designId === design.id, "设计任务引用");
    if (design.changeId) valid(changes.has(design.changeId), "设计变更引用");
    if (design.manufacturingRoute) route(design.manufacturingRoute, "设计");
    if (["in-review", "approved", "released"].includes(design.status)) valid(design.calculation && design.submittedBy, "提交设计计算/人员");
    valid(design.reviews.every((entry) => entry.revision === design.version && entry.actorId !== (design.submittedBy ?? design.createdBy) && (DESIGN_REVIEW_ROLES as readonly string[]).includes(entry.role)), "设计会签修订/职责分离");
    if (["approved", "released"].includes(design.status)) reviews(design.reviews, "通过设计", DESIGN_REVIEW_ROLES);
  }
  unique(state.releases.map((entry) => JSON.stringify([entry.productCode, entry.version])), "产品发布版本");
  for (const release of state.releases) {
    valid(designs.has(release.designId), "发布设计引用");
    reviews(release.reviews, "发布图纸", DESIGN_REVIEW_ROLES);
    valid(release.reviews.every((entry) => entry.revision === release.designVersion && entry.actorId !== (designs.get(release.designId)!.submittedBy ?? designs.get(release.designId)!.createdBy)), "发布会签修订/职责分离");
    if (release.supersedes) {
      const base = releases.get(release.supersedes);
      valid(base && base.productCode === release.productCode && base.version < release.version, "发布父链/产品版本");
    }
    const scope = factoryReleaseScope(state, release);
    if (scope.kind === "order-line") lineFor(scope.orderId, scope.lineId);
  }
  for (const order of state.orders) {
    unique(order.lines.map((entry) => entry.mark), "订单行标记");
    unique((order.quantityRevisions ?? []).map((entry) => String(entry.toRevision)), "数量修订");
    unique([...(order.quantityRevisions ?? []), ...(order.requirementRevisions ?? [])].map((entry) => String(entry.toRevision)), "订单修订事务");
    let previousDemandOrderRevision = 0;
    const demandHeads = new Map<string, NonNullable<FactoryWorkspace["orders"][number]["requirementRevisions"]>[number]>();
    for (const entry of order.requirementRevisions ?? []) {
      const line = lineFor(order.id, entry.lineId), head = demandHeads.get(entry.lineId);
      valid(entry.toRevision === entry.fromRevision + 1 && entry.toRevision <= order.revision && entry.fromRevision >= previousDemandOrderRevision
        && entry.fromRequirementRevision === (head?.toRequirementRevision ?? 1) && entry.toRequirementRevision === entry.fromRequirementRevision + 1, "订单需求修订引用/版本");
      if (head) valid(isDeepStrictEqual(head.after, entry.before) && head.toKind === entry.fromKind && head.toRequestId === entry.fromRequestId, "订单需求修订断链");
      for (const releaseId of [entry.fromReleaseId, entry.toReleaseId].filter(Boolean) as string[]) valid(releases.has(releaseId), "需求修订发布引用");
      const fromTask = tasks.get(entry.fromRequestId ?? ""), toTask = tasks.get(entry.toRequestId ?? "");
      valid(entry.fromKind === "custom" ? fromTask && fromTask.orderId === order.id && fromTask.lineId === line.id && (fromTask.requirementRevision ?? 1) === entry.fromRequirementRevision : !entry.fromRequestId, "需求修订原任务引用");
      valid(entry.toKind === "custom" ? toTask && toTask.orderId === order.id && toTask.lineId === line.id && toTask.requirementRevision === entry.toRequirementRevision && toTask.orderRevision === entry.toRevision && !entry.toReleaseId : !entry.toRequestId && entry.toReleaseId && factoryReleaseScope(state, releases.get(entry.toReleaseId)!).kind === "catalog", "需求修订新任务/标准版本引用");
      const differences = compareFactoryVersions(
        { requirement: entry.before, kind: entry.fromKind, releaseId: entry.fromReleaseId ?? null, referenceReleaseId: fromTask?.referenceReleaseId ?? null },
        { requirement: entry.after, kind: entry.toKind, releaseId: entry.toReleaseId ?? null, referenceReleaseId: toTask?.referenceReleaseId ?? null });
      valid(differences.length && isDeepStrictEqual(differences, entry.differences), "需求修订差异与快照不一致");
      for (const id of entry.cancelledPackageIds) {
        const packet = packages.get(id);
        valid(packet && packet.orderId === order.id && packet.lineId === line.id && packet.status === "cancelled" && packet.cancellation?.orderRevision === entry.toRevision
          && packet.cancellation.actorId === entry.actorId && packet.cancellation.at === entry.at && (packet.requirementRevision ?? 1) === entry.fromRequirementRevision, "需求修订撤销制造包引用");
      }
      demandHeads.set(line.id, entry); previousDemandOrderRevision = entry.toRevision;
    }
    let previousRevision = 0;
    const quantities = new Map<string, number>();
    for (const entry of order.quantityRevisions ?? []) {
      valid(order.lines.some((line) => line.id === entry.lineId) && entry.toRevision === entry.fromRevision + 1 && entry.toRevision <= order.revision && entry.fromRevision >= previousRevision && entry.fromQuantity !== entry.toQuantity, "订单数量修订引用/版本");
      valid(!quantities.has(entry.lineId) || quantities.get(entry.lineId) === entry.fromQuantity, "订单数量修订断链");
      quantities.set(entry.lineId, entry.toQuantity); previousRevision = entry.toRevision;
    }
    for (const line of order.lines) {
      const demandHead = demandHeads.get(line.id);
      valid((line.requirementRevision ?? 1) === (demandHead?.toRequirementRevision ?? 1), "需求修订与现行需求版本不一致");
      if (demandHead) valid(isDeepStrictEqual(line.requirement, demandHead.after) && line.kind === demandHead.toKind && line.engineeringRequestId === demandHead.toRequestId
        && (line.kind !== "standard" || line.releaseId === demandHead.toReleaseId), "需求修订与现值/任务不一致");
      valid(!quantities.has(line.id) || quantities.get(line.id) === line.quantity, "订单数量修订与现值不一致");
      if (line.releaseId) valid(releases.has(line.releaseId), "订单图纸引用");
      if (line.customerConfirmedReleaseId) valid(releases.has(line.customerConfirmedReleaseId), "客户确认图纸引用");
      if (line.kind === "standard") valid(line.releaseId, "标准订单发布基线");
      if (line.engineeringRequestId) {
        const task = tasks.get(line.engineeringRequestId); valid(task && task.orderId === order.id && task.lineId === line.id, "订单设计任务引用");
      }
      valid(factoryAllocatedQuantity(state.packages, line.id) <= line.quantity, "订单超量分批");
    }
  }
  for (const task of state.engineeringRequests) {
    const line = lineFor(task.orderId, task.lineId), order = orders.get(task.orderId)!;
    valid(designs.get(task.designId)?.engineeringRequestId === task.id, "研发任务双向引用");
    valid(factoryEngineeringRequestCurrent(state, task) || (order.requirementRevisions ?? []).some((entry) => entry.fromRequestId === task.id || entry.toRequestId === task.id), "历史研发任务缺少需求修订来源");
    valid(isDeepStrictEqual(task.requirement, factoryRequirementSnapshot(order, line.id, task.requirementRevision ?? 1)), "研发任务需求与冻结需求版本不一致");
    if (task.orderRevision) valid(task.orderRevision <= order.revision, "研发任务未来订单修订");
    if ((task.requirementRevision ?? 1) > 1) valid(order.requirementRevisions?.some((entry) => entry.toRequestId === task.id && entry.toRevision === task.orderRevision), "研发修订任务来源");
    if (task.previousRequestId) {
      const previous = tasks.get(task.previousRequestId);
      valid(previous && previous.orderId === task.orderId && previous.lineId === task.lineId && (previous.requirementRevision ?? 1) < (task.requirementRevision ?? 1), "研发任务交接父链");
    }
    if (task.referenceReleaseId) valid(releases.has(task.referenceReleaseId), "定制参考发布");
    if (task.assigneeId) valid(task.claimedAt || task.assignedBy && task.assignedAt, "任务分派/自主领取记录");
  }
  // A cutover revises the same physical batch; only separate root allocations need distinct names.
  unique(state.packages.filter((entry) => !entry.supersedes).map((entry) => JSON.stringify([entry.lineId, entry.batch])), "原始生产批次");
  unique(state.packages.map((entry) => JSON.stringify([entry.lineId, entry.version])), "制造包版本");
  unique(state.packages.filter((entry) => !entry.supersedes).flatMap((entry) => entry.pieceNumbers), "原始生产件号");
  for (const packet of state.packages) {
    const line = lineFor(packet.orderId, packet.lineId), order = orders.get(packet.orderId)!;
    const release = releases.get(packet.releaseId); valid(release, "制造包图纸引用");
    valid(packet.orderRevision <= orders.get(packet.orderId)!.revision, "制造包订单修订");
    const demandVersion = (order.requirementRevisions ?? []).filter((entry) => entry.lineId === line.id && entry.toRevision <= packet.orderRevision).at(-1)?.toRequirementRevision ?? 1;
    valid((packet.requirementRevision ?? 1) === demandVersion && factoryRequirementSnapshot(order, line.id, demandVersion), "制造包冻结需求版本");
    if (packet.status !== "cancelled") valid((packet.requirementRevision ?? 1) === (line.requirementRevision ?? 1), "旧需求制造包未撤销");
    valid(packet.pieceNumbers.length === packet.quantity, "制造包件号数量");
    if (packet.status === "cancelled") {
      valid(packet.cancellation && !packet.supersedes && !packet.changeId && packet.cancellation.orderRevision <= orders.get(packet.orderId)!.revision && packet.cancellation.orderRevision > packet.orderRevision, "撤销制造包记录/修订");
      valid(!state.executions.some((entry) => entry.packageId === packet.id) && !state.deviations.some((entry) => entry.packageId === packet.id) && !state.changes.some((entry) => entry.basePackageId === packet.id || entry.newPackageId === packet.id), "撤销制造包关联实况/变更");
      if (packet.cancellation.previousStatus === "approved") reviews(packet.reviews, "撤销前批准制造包", ["process", "procurement", "quality"]);
    } else valid(!packet.cancellation, "非撤销制造包存在撤销记录");
    route(packet.route, "制造包"); unique(packet.bom.map((entry) => entry.id), "制造包BOM"); reviews(packet.reviews, "制造包");
    valid(isDeepStrictEqual(packet.bom, release.calculation.lines.map((entry) => ({ ...entry, quantity: entry.quantity * packet.quantity }))) && isDeepStrictEqual(packet.route, release.calculation.route) && isDeepStrictEqual(packet.processingParameters, release.calculation.processingParameters), "制造包/BOM/路线与冻结发布不一致");
    if (["approved", "issued", "acknowledged", "complete", "superseded"].includes(packet.status)) reviews(packet.reviews, "已批准制造包", packet.changeId ? DESIGN_REVIEW_ROLES : ["process", "procurement", "quality"]);
    const scope = factoryReleaseScope(state, release);
    if (scope.kind === "order-line") valid(scope.orderId === packet.orderId && scope.lineId === packet.lineId, "制造包专用发布范围");
    if (packet.supersedes || packet.changeId) {
      const base = packages.get(packet.supersedes ?? ""), change = changes.get(packet.changeId ?? "");
      valid(base && change && base.orderId === packet.orderId && base.lineId === packet.lineId && base.version < packet.version && change.newPackageId === packet.id && change.basePackageId === base.id, "制造包变更父链");
    }
  }
  unique((state.changeSchemes ?? []).flatMap((entry) => entry.changeIds), "统一变更成员");
  for (const scheme of state.changeSchemes ?? []) {
    const members = scheme.changeIds.map((id) => changes.get(id));
    valid(members.length >= 2 && members.every((entry) => entry?.schemeId === scheme.id), "统一变更双向引用");
    const leader = members[0]!;
    unique(members.map((entry) => entry!.basePackageId), "统一变更原批");
    valid(leader.createdBy === scheme.createdBy && leader.createdAt === scheme.createdAt && designs.get(leader.designId)?.changeId === leader.id, "统一变更图纸/发起人");
    const productCode = releases.get(leader.baseReleaseId)?.productCode;
    valid(members.every((entry) => entry!.orderId === leader.orderId && entry!.lineId === leader.lineId && releases.get(entry!.baseReleaseId)?.productCode === productCode
      && entry!.designId === leader.designId && entry!.newReleaseId === leader.newReleaseId && entry!.reason === leader.reason && entry!.createdBy === scheme.createdBy && entry!.createdAt === scheme.createdAt
      && isDeepStrictEqual(entry!.reviews, leader.reviews) && isDeepStrictEqual(entry!.customerConfirmation, leader.customerConfirmation) && isDeepStrictEqual(entry!.cancellation, leader.cancellation)
      && entry!.impact.procurement === leader.impact.procurement && entry!.impact.cost === leader.impact.cost && entry!.impact.delivery === leader.impact.delivery), "统一变更共同基线/会签不一致");
    const issued = ["issued", "acknowledged", "closed"];
    valid(members.every((entry) => issued.includes(leader.status) ? issued.includes(entry!.status) && entry!.newPackageId : entry!.status === leader.status && !entry!.newPackageId), "统一变更部分下达/撤回");
    if (issued.includes(leader.status)) {
      const packets = members.map((entry) => packages.get(entry!.newPackageId!));
      valid(packets.every((entry) => entry && entry.orderRevision === packets[0]!.orderRevision && entry.createdAt === packets[0]!.createdAt), "统一变更下达修订不一致");
    }
  }
  for (const change of state.changes) {
    lineFor(change.orderId, change.lineId);
    const base = packages.get(change.basePackageId);
    if (change.schemeId) valid(schemes.get(change.schemeId)?.changeIds.includes(change.id), "变更统一方案引用");
    const designOwnerId = change.schemeId ? schemes.get(change.schemeId)!.changeIds[0] : change.id;
    valid(base && base.releaseId === change.baseReleaseId && base.orderId === change.orderId && base.lineId === change.lineId && designs.get(change.designId)?.changeId === designOwnerId, "变更基线引用");
    valid(base.route.some((step) => step.id === change.effectivity.fromStep), "变更生效工序");
    valid(change.effectivity.pieceNumbers.every((piece) => base.pieceNumbers.includes(piece)), "变更生效件号");
    valid(change.impact.executionIds.every((id) => executions.get(id)?.packageId === base.id), "变更实际影响引用");
    reviews(change.reviews, "变更");
    if (change.newReleaseId) valid(releases.has(change.newReleaseId), "变更新发布引用");
    if (change.customerConfirmation) valid(releases.has(change.customerConfirmation.releaseId), "变更客户确认引用");
    if (["impact-review", "approved", "issued", "acknowledged", "closed"].includes(change.status)) valid(change.newReleaseId, "已提交变更新发布");
    if (["approved", "issued", "acknowledged", "closed"].includes(change.status)) reviews(change.reviews, "批准变更", DESIGN_REVIEW_ROLES);
    if (["issued", "acknowledged", "closed"].includes(change.status)) valid(packages.get(change.newPackageId ?? "")?.changeId === change.id, "已下达变更新包");
    if (change.newPackageId) valid(packages.get(change.newPackageId)?.changeId === change.id, "变更新包引用");
    if (change.status === "cancelled") valid(change.cancellation && !change.newPackageId, "撤回记录");
    if (change.status === "closed") valid(change.closure, "变更闭环记录");
    unique((change.dispositionRecords ?? []).map((entry) => entry.id), "处置记录");
    for (const record of change.dispositionRecords ?? []) {
      reviews(record.reviews, "处置");
      valid(record.pieceNumbers.every((piece) => (change.effectivity.pieceNumbers.length ? change.effectivity.pieceNumbers : base.pieceNumbers).includes(piece)), "处置件号范围");
      valid(record.executionIds.every((id) => { const actual = executions.get(id); return actual && [base.id, change.newPackageId].includes(actual.packageId) && actual.pieceNumbers.every((piece) => record.pieceNumbers.includes(piece) || record.replacementPairs.some((pair) => pair.newPiece === piece)); }), "处置实际引用");
      valid(record.replacementPairs.every((pair) => record.pieceNumbers.includes(pair.oldPiece) && packages.get(change.newPackageId ?? "")?.pieceNumbers.includes(pair.newPiece)), "处置替换件引用");
    }
  }
  for (const deviation of state.deviations) {
    const packet = packages.get(deviation.packageId); valid(packet, "先行制造包引用"); reviews(deviation.reviews, "先行");
    if (deviation.scope) valid(deviation.scope.pieceNumbers.every((piece) => packet.pieceNumbers.includes(piece)) && deviation.scope.stepIds.every((id) => packet.route.some((step) => step.id === id)), "先行授权范围");
    valid(deviation.executionIds.every((id) => executions.get(id)?.deviationId === deviation.id), "先行实际关联");
    if (deviation.changeId) valid(changes.get(deviation.changeId)?.basePackageId === packet.id, "先行补图变更引用");
    if (["reconciling", "closed"].includes(deviation.status)) valid(deviation.changeId, "先行追认引用");
    if (["authorized", "reconciling", "closed"].includes(deviation.status)) reviews(deviation.reviews, "先行批准", ["production", "quality"]);
    valid(deviation.reviews.every((entry) => entry.actorId !== deviation.createdBy), "先行自审");
  }
  for (const execution of state.executions) {
    const packet = packages.get(execution.packageId);
    valid(packet && packet.orderId === execution.orderId && packet.lineId === execution.lineId && packet.batch === execution.batch, "实况制造包/订单/批次引用");
    valid(execution.quantity === execution.pieceNumbers.length && execution.pieceNumbers.every((piece) => packet.pieceNumbers.includes(piece)) && packet.route.some((step) => step.id === execution.stepId), "实况件号/工序/数量");
    if (execution.deviationId) {
      const deviation = deviations.get(execution.deviationId);
      valid(deviation && deviation.packageId === packet.id && deviation.executionIds.includes(execution.id), "实况先行引用");
      if (deviation.scope) valid(deviation.scope.stepIds.includes(execution.stepId) && execution.pieceNumbers.every((piece) => deviation.scope.pieceNumbers.includes(piece)), "实况超出先行授权范围");
    }
  }
  unique(state.audit.map((entry) => String(entry.workspaceRevision)), "审计修订");
  valid(state.audit.every((entry) => entry.workspaceRevision <= state.revision), "审计未来修订");
  const targets = new Set([...collections.flatMap((entries) => entries.map((entry) => entry.id)), ...lines.map((entry) => entry.id), ...state.changes.flatMap((entry) => (entry.dispositionRecords ?? []).map((record) => record.id))]);
  valid([...state.audit, ...state.receipts].every((entry) => targets.has(entry.targetId)), "事件/回执目标引用");
}
