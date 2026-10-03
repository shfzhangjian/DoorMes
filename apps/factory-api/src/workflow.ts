import { randomUUID, createHash } from "node:crypto";
import { factoryLastStepResult as lastStepResult, factoryPackageCompletionBlockers, factoryChangeIssueBlockers, factoryChangeSchemeIssueBlockers, factoryChangeClosureBlockers, factoryDeviationClosureBlockers, factoryOrderProductionStatus, factoryPackageCancellationBlockers, type FactoryProductionBlocker } from "@doormes/contracts/factory-production-status";
import { createEmptyDesign } from "@doormes/domain";
import { createRectangularWindowCommand, DesignSession } from "@doormes/application";
import { describeFactoryProduct, normalizeFactorySpecificationText, factoryRequirementChecks } from "@doormes/application/factory-product";
import { parseFormalDesignDocument } from "@doormes/persistence/local-design";
import { calculateFormalBom, createFormalBomInputFingerprint } from "@doormes/calculation-engine";
import { REFERENCE_SIMULATION_MANUFACTURING_CATALOG as catalog } from "../../../packages/manufacturing-model/src/reference-catalog.js";
import {
  DESIGN_REVIEW_ROLES, FACTORY_SCHEMA, factoryAllocatedQuantity, factoryReleaseScope, factoryEffectivePieceNumbers, factoryEngineeringRequestCurrent,
  type FactoryActor, type FactoryCalculation, type FactoryCommand, type FactoryDesign,
  type FactoryDifference, type FactoryManufacturingPackage, type FactoryOrder,
  type FactoryOrderLine, type FactoryRequirement, type FactoryReview, type FactoryRole,
  type FactoryWorkspace, type FactoryChange, type FactoryReleaseScope
} from "@doormes/contracts/factory-workflow";

/** Structured errors are identical in HTTP, state-machine tests and UI. */
export class FactoryError extends Error {
  constructor(readonly status: number, readonly code: string, message: string) { super(message); }
}
export function check(condition: unknown, message: string, code = "INVALID_STATE", status = 422): asserts condition {
  if (!condition) throw new FactoryError(status, code, message);
}
export function requiredText(value: unknown, label: string, limit = 2000): string {
  check(typeof value === "string" && value.trim().length > 0 && value.length <= limit, `${label}不能为空，且不能超过${limit}字`, "INVALID_INPUT", 400);
  return value.trim();
}
function designDocument(value: unknown): FactoryDesign["document"] {
  try { return parseFormalDesignDocument(value); }
  catch (error) { throw new FactoryError(400, "INVALID_DESIGN", error instanceof Error ? error.message : "设计文档无效"); }
}
function positive(value: unknown, label: string, maximum = 100000, integer = false): number {
  check(typeof value === "number" && Number.isFinite(value) && value > 0 && value <= maximum && (!integer || Number.isInteger(value)), `${label}无效`, "INVALID_INPUT", 400);
  return value;
}
function choice<T extends string>(value: unknown, choices: readonly T[], label: string): T {
  check(typeof value === "string" && choices.includes(value as T), `${label}无效`, "INVALID_INPUT", 400);
  return value as T;
}
function stringList(value: unknown): string[] {
  check(Array.isArray(value) && value.length <= 10000 && value.every((item) => typeof item === "string" && item.length < 200), "编号列表无效", "INVALID_INPUT", 400);
  return [...new Set(value as string[])];
}
function get<T extends { id: string }>(items: readonly T[], id: unknown): T {
  const item = items.find((entry) => entry.id === id);
  check(item, "指定业务对象不存在", "NOT_FOUND", 404);
  return item;
}
function checkProductionBlockers(blockers: FactoryProductionBlocker[]): void {
  const first = blockers[0];
  if (first) throw new FactoryError(first.status, first.code, first.message + (first.pieceNumbers.length ? "：" + first.pieceNumbers.join("、") : ""));
}
function authorize(actor: FactoryActor, ...roles: FactoryRole[]): void {
  check(actor.roles.some((role) => roles.includes(role)), "当前岗位不能执行此操作", "FORBIDDEN", 403);
}
function review(actor: FactoryActor, role: FactoryRole, owner: string, decision: unknown, note: unknown, revision: number, now: string): FactoryReview {
  authorize(actor, role);
  check(actor.id !== owner, "提交人不能会签自己提交的版本", "SELF_APPROVAL", 403);
  return { actorId: actor.id, role, decision: choice(decision, ["approve", "reject"], "审核结论"), note: requiredText(note, "审核说明"), revision, at: now };
}
function accepted(reviews: FactoryReview[], roles: readonly FactoryRole[]): boolean {
  return roles.every((role) => reviews.some((entry) => entry.role === role && entry.decision === "approve"));
}
function upsertReview(reviews: FactoryReview[], entry: FactoryReview): FactoryReview[] {
  return [...reviews.filter((item) => item.role !== entry.role), entry];
}
function requirement(value: FactoryRequirement): FactoryRequirement {
  check(value && typeof value === "object", "订单需求格式无效", "INVALID_INPUT", 400);
  return {
    widthMm: positive(value.widthMm, "总宽", 50000), heightMm: positive(value.heightMm, "总高", 50000),
    material: requiredText(value.material, "型材系统", 200), glass: requiredText(value.glass, "玻璃", 200),
    hardware: requiredText(value.hardware, "五金", 200), finish: requiredText(value.finish, "表面要求", 200),
    dueDate: typeof value.dueDate === "string" ? value.dueDate.slice(0, 40) : "",
    note: typeof value.note === "string" ? value.note.slice(0, 2000) : ""
  };
}
export function createFactoryWorkspace(): FactoryWorkspace {
  return { schemaVersion: FACTORY_SCHEMA, organizationId: "factory-prototype", revision: 0, savedAt: new Date().toISOString(), designs: [], releases: [], orders: [], engineeringRequests: [], packages: [], changes: [], deviations: [], executions: [], audit: [], receipts: [] };
}
/** Uses the existing graph calculator, retaining simulation provenance. */
export function calculateFactoryDesign(document: FactoryDesign["document"]): FactoryCalculation {
  const result = calculateFormalBom(document, catalog);
  const processNames = { "hardware-mount": "五金安装定位", "hardware-machining": "五金孔槽加工", "surface-treatment": "表面处理", "engineering-joint-process": "连接件加工装配" };
  const route = [
    { id: "MATERIAL", name: "领料与齐套", instruction: "按本制造包的材料编码、数量和批次领料", sourceObjectIds: document.windows.map((window) => String(window.objectId)) },
    { id: "CUT", name: "型材切割", instruction: "核对本版本切长和端切角；参考规则需企业确认", sourceObjectIds: document.windows.map((window) => String(window.objectId)) },
    ...result.processFeatures.map((feature) => ({ id: feature.featureId, name: processNames[feature.kind], instruction: feature.kind === "hardware-mount" || feature.kind === "hardware-machining" ? "定位 X/Y/Z：" + [feature.xMm, feature.yMm, feature.zMm].join(" / ") + " mm；型号：" + feature.componentModelId + "。按企业确认模板加工与检查。" : "按本版本连接与表面处理定义核对工序，企业参数待确认。", sourceObjectIds: [...feature.sourceObjectIds].map(String) })),
    { id: "ASSEMBLY", name: "框扇、玻璃与五金装配", instruction: "按图纸与组成件表核对装配关系", sourceObjectIds: document.windows.map((window) => String(window.objectId)) },
    { id: "QUALITY", name: "终检", instruction: "尺寸、对角线、外观、开合与齐套检验", sourceObjectIds: document.windows.map((window) => String(window.objectId)) }
  ];
  return {
    fingerprint: createHash("sha256").update(createFormalBomInputFingerprint(document, catalog)).digest("hex"),
    catalogVersion: "doormes-reference-simulation.2026-10-02", basis: "reference-simulation", productionEligible: false,
    lines: result.mbom.lines.map((line) => ({ id: line.lineId, sourceObjectId: line.sourceComponentId, sourceMark: line.sourceMark, grossLengthMm: line.grossLengthMm, material: line.material, color: line.color, category: line.category, materialCode: line.materialCode, name: line.name, specification: line.spec, lengthMm: line.lengthMm, widthMm: line.widthMm, heightMm: line.heightMm, cutLeftDeg: line.cutLeftDeg, cutRightDeg: line.cutRightDeg, quantity: line.quantity, unit: line.unit })),
    route, blockingCodes: [...result.confirmation.blockingDiagnosticCodes], processingParameters: structuredClone([...result.processFeatures])
  };
}
/** Stable-object keyed comparison; revisions/selection do not create geometry diffs. */
export function compareFactoryVersions(before: unknown, after: unknown, path = ""): FactoryDifference[] {
  if (JSON.stringify(before) === JSON.stringify(after)) return [];
  const category: FactoryDifference["category"] = path.startsWith("bom") ? "bom" : path.startsWith("route") ? "route" : /material|profile|glass|hardware|visual|catalog/i.test(path) ? "material" : /width|height|position|transform|layout|topology|opening|installation/i.test(path) ? "geometry" : "other";
  if (Array.isArray(before) && Array.isArray(after)) {
    const key = (item: unknown, index: number): string => item && typeof item === "object" ? String((item as Record<string, unknown>).objectId ?? (item as Record<string, unknown>).id ?? index) : String(index);
    const left = new Map(before.map((item, index) => [key(item, index), item]));
    const right = new Map(after.map((item, index) => [key(item, index), item]));
    return [...new Set([...left.keys(), ...right.keys()])].flatMap((id) => compareFactoryVersions(left.get(id), right.get(id), `${path}[${id}]`));
  }
  if (before && after && typeof before === "object" && typeof after === "object") {
    const left = before as Record<string, unknown>, right = after as Record<string, unknown>;
    return [...new Set([...Object.keys(left), ...Object.keys(right)])].filter((field) => field !== "revision").flatMap((field) => compareFactoryVersions(left[field], right[field], path ? `${path}.${field}` : field));
  }
  return [{ path, before: before ?? null, after: after ?? null, category }];
}
function newDesign(state: FactoryWorkspace, actor: FactoryActor, name: string, document: unknown, now: string): FactoryDesign {
  const design: FactoryDesign = { id: randomUUID(), name: requiredText(name, "设计名称", 200), drawingNumber: `DM-${String(state.designs.length + 1).padStart(4, "0")}`, version: 1, document: designDocument(document), createdBy: actor.id, updatedAt: now, status: "draft", reviews: [] };
  state.designs.push(design);
  return design;
}
function standardRequirementRelease(state: FactoryWorkspace, releaseId: unknown, demand: FactoryRequirement) {
  const release = get(state.releases, releaseId), specification = describeFactoryProduct(release.document);
  check(factoryReleaseScope(state, release).kind === "catalog", "订单专用图纸不能作为标准产品直接下单，请建立定制需求或发布独立标准产品");
  check(specification.units.length && specification.widthMm === demand.widthMm && specification.heightMm === demand.heightMm
    && (["material", "glass", "hardware", "finish"] as const).every((field) => normalizeFactorySpecificationText(specification[field]) === normalizeFactorySpecificationText(demand[field])),
  "标准件尺寸/型号/内外颜色超出完整发布规格，请转为定制工程任务");
  return release;
}
function makeOrderLine(state: FactoryWorkspace, actor: FactoryActor, order: FactoryOrder, input: Extract<FactoryCommand, { type: "create-order" | "add-order-line" }>, now: string): FactoryOrderLine {
  const kind = choice(input.kind, ["standard", "custom"], "订单类型");
  const demand = requirement(input.requirement);
  const line: FactoryOrderLine = { id: randomUUID(), mark: `L${order.lines.length + 1}`, kind, quantity: positive(input.quantity, "数量", 1000, true), requirement: demand, requirementRevision: 1 };
  if (kind === "standard") {
    const release = standardRequirementRelease(state, input.releaseId, demand);
    line.releaseId = release.id; line.customerConfirmedReleaseId = release.id;
  } else {
    const session = new DesignSession(createEmptyDesign(`CUSTOM-${line.id}`));
    session.execute(createRectangularWindowCommand({ commandId: randomUUID(), windowId: `WINDOW-${line.id}`, mark: "C1", widthMm: demand.widthMm, heightMm: demand.heightMm, profileSystemId: demand.material, defaultGlassTypeId: demand.glass, defaultHardwareSetId: demand.hardware }));
    const design = newDesign(state, actor, `${order.number}-${line.mark} 定制设计`, session.document, now);
    const reference = input.releaseId ? get(state.releases, input.releaseId) : undefined;
    if (reference) { design.document = designDocument(reference.document); design.manufacturingRoute = structuredClone(reference.calculation.route); }
    const request = { id: randomUUID(), orderId: order.id, lineId: line.id, requirement: structuredClone(demand), requirementRevision: 1, orderRevision: order.revision + (input.type === "add-order-line" ? 1 : 0), designId: design.id, status: "unassigned" as const, ...(reference ? { referenceReleaseId: reference.id } : {}) };
    design.engineeringRequestId = request.id; line.engineeringRequestId = request.id; state.engineeringRequests.push(request);
  }
  order.lines.push(line); return line;
}
function makePackage(state: FactoryWorkspace, actor: FactoryActor, order: FactoryOrder, line: FactoryOrderLine, releaseId: string, batch: string, now: string, quantity = line.quantity): FactoryManufacturingPackage {
  const release = get(state.releases, releaseId);
  const version = state.packages.filter((entry) => entry.lineId === line.id).length + 1;
  const packet: FactoryManufacturingPackage = {
    id: randomUUID(), orderId: order.id, orderRevision: order.revision, requirementRevision: line.requirementRevision ?? 1, lineId: line.id, releaseId,
    version, quantity, batch: requiredText(batch, "生产批次", 100), productionEligible: false, status: "review", reviews: [], createdBy: actor.id, createdAt: now,
    bom: release.calculation.lines.map((entry) => ({ ...entry, quantity: entry.quantity * quantity })),
    processingParameters: structuredClone(release.calculation.processingParameters ?? []),
    route: structuredClone(release.calculation.route), pieceNumbers: Array.from({ length: quantity }, (_, index) => `${order.number}-${line.mark}-${batch}-V${version}-${String(index + 1).padStart(4, "0")}`)
  };
  state.packages.push(packet); return packet;
}
function deviationScope(packet: FactoryManufacturingPackage, input: Extract<FactoryCommand, { type: "create-deviation" | "revise-deviation" }>) {
  const pieceNumbers = stringList(input.pieceNumbers), stepIds = stringList(input.stepIds);
  check(pieceNumbers.length && pieceNumbers.every((piece) => packet.pieceNumbers.includes(piece)), "先行件号必须明确且属于制造包");
  check(stepIds.length && stepIds.every((stepId) => packet.route.some((step) => step.id === stepId)), "先行工序必须明确且属于制造包");
  return { pieceNumbers, stepIds, risk: requiredText(input.risk, "先行风险"), control: requiredText(input.control, "风险控制与质量措施") };
}

function assertEngineeringCurrent(state: FactoryWorkspace, design: FactoryDesign): void {
  if (!design.engineeringRequestId) return;
  check(factoryEngineeringRequestCurrent(state, get(state.engineeringRequests, design.engineeringRequestId)), "订单需求已修订，此任务图纸仅供历史查看，请办理最新需求任务", "STALE_REQUIREMENT", 409);
}
function assertEngineeringOwner(state: FactoryWorkspace, design: FactoryDesign, actor: FactoryActor): void {
  assertEngineeringCurrent(state, design);
  if (design.changeId) {
    const change = get(state.changes, design.changeId);
    check(change.status === "designing" && change.designId === design.id, "变更已撤回、提交或此图纸不是当前修订，不能继续编辑或发布");
  }
  if (!design.engineeringRequestId) return;
  const request = get(state.engineeringRequests, design.engineeringRequestId);
  check(request.assigneeId === actor.id && request.claimedAt, "请先领取分派给自己的研发任务；其他人员不能改写任务图纸", "FORBIDDEN", 403);
}
function assertEngineeringRequirement(state: FactoryWorkspace, design: FactoryDesign): void {
  if (!design.engineeringRequestId) return;
  const request = get(state.engineeringRequests, design.engineeringRequestId);
  const mismatches = factoryRequirementChecks(design.document, request.requirement).filter((entry) => !entry.matches);
  check(!mismatches.length, "图纸主规格尚未符合任务需求，请调整后再提交：" + mismatches.map((entry) => entry.label + "（需求 " + entry.required + " / 图纸 " + entry.actual + "）").join("；"), "REQUIREMENT_MISMATCH", 422);
}
function changePieces(state: FactoryWorkspace, change: FactoryChange): string[] {
  return change.effectivity.pieceNumbers.length ? change.effectivity.pieceNumbers : get(state.packages, change.basePackageId).pieceNumbers;
}
function changeImpactExecutions(state: FactoryWorkspace, change: FactoryChange): string[] {
  const affected = changePieces(state, change);
  return state.executions.filter((entry) => entry.packageId === change.basePackageId && entry.pieceNumbers.some((piece) => affected.includes(piece))).map((entry) => entry.id);
}
function assertChangeScopeAvailable(state: FactoryWorkspace, packet: FactoryManufacturingPackage, pieces: string[], excludeId?: string): void {
  const effective = factoryEffectivePieceNumbers(state, packet);
  check(pieces.length && pieces.every((piece) => effective.includes(piece)), "涉及件号已切换其他版本或不属于当前有效范围，请从生效制造包发起变更", "CONFLICT", 409);
  check(!state.changes.some((entry) => entry.id !== excludeId && entry.basePackageId === packet.id && !["closed", "cancelled"].includes(entry.status) && changePieces(state, entry).some((piece) => pieces.includes(piece))), "涉及件号已有未结束变更，请修订或撤回原变更", "CONFLICT", 409);
}
function sharedChanges(state: FactoryWorkspace, change: FactoryChange): FactoryChange[] {
  return change.schemeId ? get(state.changeSchemes ?? [], change.schemeId).changeIds.map((id) => get(state.changes, id)) : [change];
}
function commandChanges(state: FactoryWorkspace, command: { changeId: string } | { schemeId: string }): { members: FactoryChange[]; targetId: string } {
  if ("schemeId" in command) {
    const scheme = get(state.changeSchemes ?? [], command.schemeId);
    return { members: scheme.changeIds.map((id) => get(state.changes, id)), targetId: scheme.id };
  }
  const change = get(state.changes, command.changeId);
  check(!change.schemeId, "此批次属于统一变更方案，请操作整个方案，不能单独会签或下达");
  return { members: [change], targetId: change.id };
}
/** Applies one authorized transaction to a detached state; only the store commits. */
export function applyFactoryCommand(state: FactoryWorkspace, actor: FactoryActor, command: FactoryCommand, now = new Date().toISOString(), people: readonly FactoryActor[] = []): string {
  check(actor.organizationId === state.organizationId, "不能访问其他组织数据", "FORBIDDEN", 403);
  check(command && typeof command === "object", "命令格式无效", "INVALID_INPUT", 400);
  let targetId = "";
  switch (command.type) {
    case "assign-engineering": {
      authorize(actor, "reviewer", "production"); const request = get(state.engineeringRequests, command.requestId), design = get(state.designs, request.designId);
      assertEngineeringCurrent(state, design);
      check(["draft", "returned"].includes(design.status), "会签中或发布任务不可交接，请先退回设计");
      const assignee = people.find((person) => person.id === command.assigneeId && person.organizationId === state.organizationId && person.roles.includes("designer"));
      check(assignee, "接收人必须是本工厂启用的设计研发账号", "INVALID_ASSIGNEE", 422);
      requiredText(command.note, "分派或交接说明");
      request.assigneeId = assignee.id; request.assignedBy = actor.id; request.assignedAt = now; delete request.claimedAt; request.status = "assigned";
      targetId = request.id; break;
    }
    case "claim-engineering": {
      authorize(actor, "designer"); const request = get(state.engineeringRequests, command.requestId), design = get(state.designs, request.designId);
      assertEngineeringCurrent(state, design);
      check(["draft", "returned"].includes(design.status) && ["unassigned", "assigned", "returned", "designing"].includes(request.status), "任务不能在此阶段领取");
      check(!request.assigneeId || request.assigneeId === actor.id, "任务已分派给其他研发人员", "FORBIDDEN", 403);
      check(!request.claimedAt, "任务已经领取");
      request.assigneeId = actor.id; request.claimedAt = now; request.status = "designing"; targetId = request.id; break;
    }
    case "save-design": {
      authorize(actor, "designer");
      const design = command.designId ? get(state.designs, command.designId) : newDesign(state, actor, command.name, command.document, now);
      assertEngineeringOwner(state, design, actor);
      check(["draft", "returned"].includes(design.status), "审核中和已发布图纸不可直接改写，请退回或复制新版本");
      design.document = designDocument(command.document); design.name = requiredText(command.name, "设计名称", 200);
      const number = requiredText(command.drawingNumber, "图纸编号", 100);
      const base = design.baseReleaseId ? get(state.releases, design.baseReleaseId) : undefined;
      check(!base || number === base.drawingNumber, "修订必须保留基础图号");
      check(base || !state.designs.some((entry) => entry.id !== design.id && entry.drawingNumber === number), "图纸编号已存在");
      design.drawingNumber = number; if (command.designId) design.version++; design.updatedAt = now;
      design.status = "draft"; design.reviews = []; delete design.calculation; delete design.submittedBy;
      if (design.engineeringRequestId) get(state.engineeringRequests, design.engineeringRequestId).status = "designing";
      targetId = design.id; break;
    }
    case "fork-release": {
      authorize(actor, "designer"); const release = get(state.releases, command.releaseId);
      check(factoryReleaseScope(state, release).kind === "catalog", "订单专用图纸的修订须从生效制造包发起；复用请建立定制需求");
      const design = newDesign(state, actor, command.name, release.document, now);
      design.manufacturingRoute = structuredClone(release.calculation.route);
      design.baseReleaseId = release.id; design.drawingNumber = release.drawingNumber; targetId = design.id; break;
    }
    case "submit-design": {
      authorize(actor, "designer"); const design = get(state.designs, command.designId);
      assertEngineeringOwner(state, design, actor);
      check(["draft", "returned"].includes(design.status), "当前设计不能重复提交");
      check(design.document.windows.length > 0, "空图纸不能提交审核");
      assertEngineeringRequirement(state, design);
      design.calculation = calculateFactoryDesign(design.document); design.reviews = []; design.status = "in-review";
      if (design.manufacturingRoute) {
        const sources = new Set(design.calculation.route.flatMap((entry) => entry.sourceObjectIds));
        check(design.manufacturingRoute.every((entry) => entry.sourceObjectIds.every((id) => sources.has(id))), "工艺路线引用了过期构件，请工艺岗位更新");
        design.calculation.route = structuredClone(design.manufacturingRoute);
      }
      design.submittedBy = actor.id;
      if (design.engineeringRequestId) get(state.engineeringRequests, design.engineeringRequestId).status = "in-review";
      targetId = design.id; break;
    }
    case "review-design": {
      const design = get(state.designs, command.designId); assertEngineeringCurrent(state, design); check(design.status === "in-review", "图纸不在会签阶段");
      if (design.changeId) check(get(state.changes, design.changeId).status === "designing" && get(state.changes, design.changeId).designId === design.id, "变更已撤回或此图纸不是当前修订，不能继续会签");
      choice(command.role, DESIGN_REVIEW_ROLES, "会签岗位");
      design.reviews = upsertReview(design.reviews, review(actor, command.role, design.submittedBy ?? design.createdBy, command.decision, command.note, design.version, now));
      design.status = command.decision === "reject" ? "returned" : accepted(design.reviews, DESIGN_REVIEW_ROLES) ? "approved" : "in-review";
      if (design.engineeringRequestId && design.status === "returned") get(state.engineeringRequests, design.engineeringRequestId).status = "returned";
      targetId = design.id; break;
    }
    case "set-design-route": {
      authorize(actor, "process"); const design = get(state.designs, command.designId);
      assertEngineeringCurrent(state, design);
      if (design.changeId) check(get(state.changes, design.changeId).status === "designing" && get(state.changes, design.changeId).designId === design.id, "变更已撤回或提交，不能修改路线");
      check(["draft", "returned", "in-review"].includes(design.status), "已批准/已发布路线不可覆盖，请先建立修订");
      check(Array.isArray(command.route) && command.route.length >= 3 && command.route.length <= 200, "工艺路线格式无效", "INVALID_INPUT", 400);
      const route = command.route.map((entry) => ({ id: requiredText(entry.id, "工序编号", 200), name: requiredText(entry.name, "工序名称", 200), instruction: requiredText(entry.instruction, "工序说明"), sourceObjectIds: stringList(entry.sourceObjectIds), ...(entry.templateId ? { templateId: requiredText(entry.templateId, "加工模板编号", 200) } : {}) }));
      check(new Set(route.map((entry) => entry.id)).size === route.length && route[0]!.id === "MATERIAL" && route.at(-1)!.id === "QUALITY", "路线编号须唯一，首末工序须为MATERIAL和QUALITY");
      design.manufacturingRoute = route; design.version++; design.updatedAt = now; design.reviews = []; delete design.calculation;
      if (design.status === "in-review") design.status = "returned";
      if (design.engineeringRequestId) {
        const task = get(state.engineeringRequests, design.engineeringRequestId);
        task.status = task.claimedAt ? design.status === "returned" ? "returned" : "designing" : task.assigneeId ? "assigned" : "unassigned";
      }
      targetId = design.id; break;
    }
    case "release-design": {
      authorize(actor, "designer"); const design = get(state.designs, command.designId);
      assertEngineeringOwner(state, design, actor);
      check(design.status === "approved" && design.calculation && accepted(design.reviews, DESIGN_REVIEW_ROLES), "全部会签通过后才可发布");
      assertEngineeringRequirement(state, design);
      const productCode = requiredText(command.productCode, "产品编号", 100);
      const change = design.changeId ? get(state.changes, design.changeId) : undefined;
      const task = design.engineeringRequestId ? get(state.engineeringRequests, design.engineeringRequestId) : undefined;
      const base = design.baseReleaseId ? get(state.releases, design.baseReleaseId) : undefined;
      const scope: FactoryReleaseScope = change || task ? { kind: "order-line", orderId: (change ?? task)!.orderId, lineId: (change ?? task)!.lineId } : base ? factoryReleaseScope(state, base) : { kind: "catalog" };
      if (design.baseReleaseId) check(get(state.releases, design.baseReleaseId).productCode === productCode, "修订产品编号必须与基础发布一致");
      else check(!state.releases.some((entry) => entry.productCode === productCode), "已有产品请从历史发布复制修订");
      if (base && scope.kind === "catalog") check(state.releases.filter((entry) => entry.productCode === productCode && factoryReleaseScope(state, entry).kind === "catalog").at(-1)?.id === base.id, "该产品已发布更新版本，请重新建立修订基线", "CONFLICT", 409);
      if (change) for (const member of sharedChanges(state, change)) assertChangeScopeAvailable(state, get(state.packages, member.basePackageId), changePieces(state, member), member.id);
      const release = { id: randomUUID(), designId: design.id, productCode, name: design.name, drawingNumber: design.drawingNumber, version: state.releases.filter((entry) => entry.productCode === productCode).length + 1, designVersion: design.version, document: structuredClone(design.document), calculation: structuredClone(design.calculation), reviews: structuredClone(design.reviews), publishedBy: actor.id, publishedAt: now, supersedes: design.baseReleaseId, usage: "prototype" as const, productionEligible: false as const };
      state.releases.push({ ...release, scope, specification: describeFactoryProduct(release.document) }); design.status = "released";
      if (design.engineeringRequestId) {
        const request = get(state.engineeringRequests, design.engineeringRequestId); request.status = "released";
        const order = get(state.orders, request.orderId); get(order.lines, request.lineId).releaseId = release.id; order.revision++; order.updatedAt = now;
      }
      if (change) for (const member of sharedChanges(state, change)) member.newReleaseId = release.id;
      targetId = release.id; break;
    }
    case "create-order": {
      authorize(actor, "sales"); const number = requiredText(command.number, "订单号", 100);
      check(!state.orders.some((entry) => entry.number === number), "订单号必须唯一");
      const order: FactoryOrder = { id: randomUUID(), number, customer: requiredText(command.customer, "客户", 200), revision: 1, status: "draft", lines: [], createdBy: actor.id, updatedAt: now };
      state.orders.push(order); makeOrderLine(state, actor, order, command, now); targetId = order.id; break;
    }
    case "add-order-line": {
      authorize(actor, "sales"); const order = get(state.orders, command.orderId); check(order.status === "draft", "确认后请通过工程变更修改订单");
      makeOrderLine(state, actor, order, command, now); order.revision++; order.updatedAt = now; targetId = order.id; break;
    }
    case "confirm-order": {
      authorize(actor, "sales"); const order = get(state.orders, command.orderId); check(order.status === "draft", "订单已经确认");
      order.status = "confirmed"; order.revision++; order.updatedAt = now; targetId = order.id; break;
    }
    case "revise-order-quantity": {
      authorize(actor, "sales"); const order = get(state.orders, command.orderId), line = get(order.lines, command.lineId);
      const quantity = positive(command.quantity, "订单数量", 1000, true), note = requiredText(command.note, "数量调整与客户确认说明");
      check(quantity !== line.quantity, "订单数量未改变");
      check(quantity >= factoryAllocatedQuantity(state.packages, line.id), "新数量不能小于已分配有效批次数量；先撤销未下达批次，已下达件须走受控处置");
      (order.quantityRevisions ??= []).push({ lineId: line.id, fromQuantity: line.quantity, toQuantity: quantity, fromRevision: order.revision, toRevision: order.revision + 1, note, actorId: actor.id, at: now });
      line.quantity = quantity; order.revision++; order.updatedAt = now; order.status = factoryOrderProductionStatus(state, order); targetId = line.id; break;
    }
    case "revise-order-requirement": {
      authorize(actor, "sales"); const order = get(state.orders, command.orderId), line = get(order.lines, command.lineId);
      const kind = choice(command.kind, ["standard", "custom"], "订单类型"), demand = requirement(command.requirement), note = requiredText(command.note, "需求调整与客户确认说明");
      const packets = state.packages.filter((entry) => entry.lineId === line.id && entry.status !== "cancelled");
      check(packets.every((entry) => ["review", "approved"].includes(entry.status)), "此订单行已有下达批次，需求变动必须从实际生效制造包发起工程变更，不能改写原需求", "REQUIRES_ENGINEERING_CHANGE", 409);
      for (const packet of packets) checkProductionBlockers(factoryPackageCancellationBlockers(state, packet));
      const reference = kind === "standard" ? standardRequirementRelease(state, command.releaseId, demand) : command.releaseId ? get(state.releases, command.releaseId) : undefined;
      const previous = state.engineeringRequests.filter((entry) => entry.orderId === order.id && entry.lineId === line.id).at(-1);
      const previousDesign = previous ? get(state.designs, previous.designId) : undefined;
      const base = line.releaseId ? get(state.releases, line.releaseId) : undefined;
      check(compareFactoryVersions(line.requirement, demand).length || line.kind !== kind || kind === "standard" && line.releaseId !== reference!.id
        || kind === "custom" && command.releaseId && previous?.referenceReleaseId !== reference?.id, "订单需求和引用版本未改变");
      const fromRevision = order.revision, fromRequirementRevision = line.requirementRevision ?? 1;
      const fromReleaseId = line.releaseId, fromRequestId = line.engineeringRequestId;
      const before = structuredClone(line.requirement), fromKind = line.kind;
      // Every input, old packet and reference is checked before creating any new task or cancelling any package.
      let nextRequestId: string | undefined;
      if (kind === "custom") {
        const document = reference?.document ?? base?.document ?? previousDesign?.document;
        check(document, "未找到可修订的设计基线");
        const design = newDesign(state, actor, order.number + "-" + line.mark + " 需求修订 " + (fromRequirementRevision + 1), document, now);
        const route = reference?.calculation.route ?? base?.calculation.route ?? previousDesign?.manufacturingRoute;
        if (route) design.manufacturingRoute = structuredClone(route);
        // Only an order-owned release is a publication parent. A catalog reference is not promoted into an order branch silently.
        const parent = base && factoryReleaseScope(state, base).kind === "order-line" ? base : line.kind === "custom" && previousDesign?.baseReleaseId ? get(state.releases, previousDesign.baseReleaseId) : undefined;
        if (parent) { design.baseReleaseId = parent.id; design.drawingNumber = parent.drawingNumber; }
        nextRequestId = randomUUID(); design.engineeringRequestId = nextRequestId;
        state.engineeringRequests.push({ id: nextRequestId, orderId: order.id, lineId: line.id, requirement: structuredClone(demand), requirementRevision: fromRequirementRevision + 1,
          orderRevision: fromRevision + 1, designId: design.id, status: "unassigned", ...(previous ? { previousRequestId: previous.id } : {}),
          ...((reference ?? base) ? { referenceReleaseId: (reference ?? base)!.id } : {}) });
      }
      order.revision++;
      for (const packet of packets) {
        packet.cancellation = { previousStatus: packet.status as "review" | "approved", note: "需求修订撤销未下达基线：" + note, actorId: actor.id, at: now, orderRevision: order.revision };
        packet.status = "cancelled";
      }
      line.requirement = demand; line.kind = kind; line.requirementRevision = fromRequirementRevision + 1;
      delete line.releaseId; delete line.customerConfirmedReleaseId; delete line.engineeringRequestId;
      if (kind === "standard") line.releaseId = reference!.id;
      if (nextRequestId) line.engineeringRequestId = nextRequestId;
      (order.requirementRevisions ??= []).push({ lineId: line.id, fromRevision, toRevision: order.revision, fromRequirementRevision, toRequirementRevision: line.requirementRevision,
        before, after: structuredClone(demand), fromKind, toKind: kind,
        ...(fromReleaseId ? { fromReleaseId } : {}), ...(line.releaseId ? { toReleaseId: line.releaseId } : {}),
        ...(fromRequestId ? { fromRequestId } : {}), ...(nextRequestId ? { toRequestId: nextRequestId } : {}),
        cancelledPackageIds: packets.map((entry) => entry.id), differences: compareFactoryVersions(
          { requirement: before, kind: fromKind, releaseId: fromReleaseId ?? null, referenceReleaseId: fromKind === "custom" ? previous?.referenceReleaseId ?? null : null },
          { requirement: demand, kind, releaseId: line.releaseId ?? null, referenceReleaseId: kind === "custom" ? (reference ?? base)?.id ?? null : null }),
        note, actorId: actor.id, at: now });
      order.updatedAt = now; order.status = factoryOrderProductionStatus(state, order); targetId = line.id; break;
    }
    case "confirm-customer": {
      authorize(actor, "sales"); const order = get(state.orders, command.orderId), line = get(order.lines, command.lineId);
      const change = state.changes.find((entry) => entry.lineId === line.id && entry.newReleaseId === command.releaseId);
      check(line.releaseId === command.releaseId || change && !["cancelled", "closed"].includes(change.status), "客户确认版本必须对应此订单行的有效设计或变更"); get(state.releases, command.releaseId);
      if (change) {
        check(["designing", "impact-review", "approved"].includes(change.status), "变更已下达或撤回，不能改写客户确认");
        for (const member of sharedChanges(state, change)) member.customerConfirmation = { releaseId: command.releaseId, actorId: actor.id, at: now };
      }
      else line.customerConfirmedReleaseId = command.releaseId;
      order.revision++; order.updatedAt = now; targetId = line.id; break;
    }
    case "create-package": {
      authorize(actor, "production"); const order = get(state.orders, command.orderId), line = get(order.lines, command.lineId);
      check(order.status !== "draft" && line.releaseId && line.customerConfirmedReleaseId === line.releaseId, "需确认订单及客户设计版本后生成制造包");
      const remaining = line.quantity - factoryAllocatedQuantity(state.packages, line.id);
      check(remaining > 0, "订单行数量已全部分配，修订请走变更流程");
      const quantity = positive(command.quantity ?? remaining, "本批数量", 1000, true), batch = requiredText(command.batch, "生产批次", 100);
      check(quantity <= remaining, "本批数量超过订单行尚未分配数量");
      check(!state.packages.some((entry) => entry.lineId === line.id && !entry.supersedes && entry.batch === batch), "此订单行已有同名生产批次，请使用不同批次");
      targetId = makePackage(state, actor, order, line, line.releaseId, batch, now, quantity).id; break;
    }
    case "review-package": {
      const packet = get(state.packages, command.packageId); check(packet.status === "review", "制造包不在审核阶段");
      choice(command.role, ["process", "procurement", "quality"], "制造包会签岗位");
      packet.reviews = upsertReview(packet.reviews, review(actor, command.role, packet.createdBy, command.decision, command.note, packet.version, now));
      if (command.decision === "reject") packet.reviews = packet.reviews.filter((entry) => entry.role === command.role);
      packet.status = accepted(packet.reviews, ["process", "procurement", "quality"]) ? "approved" : "review"; targetId = packet.id; break;
    }
    case "cancel-package": {
      authorize(actor, "production"); const packet = get(state.packages, command.packageId);
      checkProductionBlockers(factoryPackageCancellationBlockers(state, packet));
      const note = requiredText(command.note, "批次撤销原因"), order = get(state.orders, packet.orderId);
      order.revision++; order.updatedAt = now;
      packet.cancellation = { previousStatus: packet.status as "review" | "approved", note, actorId: actor.id, at: now, orderRevision: order.revision };
      packet.status = "cancelled"; order.status = factoryOrderProductionStatus(state, order); targetId = packet.id; break;
    }
    case "issue-package": {
      authorize(actor, "production"); const packet = get(state.packages, command.packageId); check(packet.status === "approved", "制造包会签未通过");
      check(!packet.changeId, "变更制造包必须通过变更下达"); packet.status = "issued";
      const order = get(state.orders, packet.orderId); order.status = factoryOrderProductionStatus(state, order); targetId = packet.id; break;
    }
    case "acknowledge-package": {
      authorize(actor, "operator"); const packet = get(state.packages, command.packageId); check(packet.status === "issued", "制造包尚未下达或已经接收");
      check(!packet.changeId, "变更制造包必须通过变更接收");
      packet.status = "acknowledged"; const order = get(state.orders, packet.orderId); order.status = factoryOrderProductionStatus(state, order); targetId = packet.id; break;
    }
    case "record-execution": {
      const kind = choice(command.kind, ["material", "process", "inspection", "rework", "scrap"], "记录类型");
      authorize(actor, ...(kind === "inspection" ? ["quality" as const] : ["operator" as const]));
      const packet = get(state.packages, command.packageId), pieces = stringList(command.pieceNumbers);
      const disposal = ["scrap", "rework"].includes(kind);
      const activeDisposal = state.changes.some((entry) => entry.basePackageId === packet.id && entry.status === "acknowledged" && (entry.disposition === kind || entry.disposition === "isolate") && pieces.every((piece) => (entry.effectivity.pieceNumbers.length ? entry.effectivity.pieceNumbers : packet.pieceNumbers).includes(piece)));
      check(packet.status === "acknowledged" || packet.status === "complete" && disposal && activeDisposal, "需先接收有效制造包；已完工包仅能追加已接收变更范围的实际处置");
      const step = get(packet.route, command.stepId);
      check(pieces.length && pieces.every((entry) => packet.pieceNumbers.includes(entry)), "生产件号不属于此制造包");
      check(!disposal || !state.changes.some((change) => change.basePackageId === packet.id && (change.dispositionRecords ?? []).some((entry) => entry.disposition !== "isolate" && accepted(entry.reviews, ["production", "quality"]) && entry.pieceNumbers.some((piece) => pieces.includes(piece)))), "这些件号的最终处置已确认，不能继续追加返工/报废；请重新发起变更");
      const quantity = positive(command.quantity, "完成数量", packet.quantity, true); check(quantity === pieces.length, "完成数量必须等于所选生产件数");
      check(kind !== "inspection" || step.id === "QUALITY", "终检记录必须对应终检工序");
      if (!["scrap", "rework"].includes(kind)) {
        check(step.id === "MATERIAL" ? kind === "material" : step.id === "QUALITY" ? kind === "inspection" : kind === "process", "记录类型与工序不一致");
        const prior = packet.route.slice(0, packet.route.findIndex((entry) => entry.id === step.id));
        check(pieces.every((piece) => prior.every((entry) => ["pass", "recorded"].includes(lastStepResult(state, packet, piece, entry.id) ?? ""))), "前序工序尚未完成，不能越过执行或终检");
      }
      const cutover = state.changes.find((change) => change.basePackageId === packet.id && ["issued", "acknowledged", "closed"].includes(change.status) && (!change.effectivity.pieceNumbers.length || pieces.some((entry) => change.effectivity.pieceNumbers.includes(entry))) && packet.route.findIndex((entry) => entry.id === step.id) >= packet.route.findIndex((entry) => entry.id === change.effectivity.fromStep));
      check(!cutover || kind === "scrap" || kind === "rework", "这些生产件在此工序已切换新版本，不能继续按旧图加工");
      const deviation = command.deviationId ? get(state.deviations, command.deviationId) : undefined;
      if (deviation) {
        check(deviation.packageId === packet.id && ["authorized", "reconciling"].includes(deviation.status), "先行处置尚未获准或不属于当前包");
        check(deviation.scope && deviation.scope.stepIds.includes(step.id) && pieces.every((piece) => deviation.scope.pieceNumbers.includes(piece)), "执行超出先行授权件号或工序范围");
      }
      const outstanding = state.deviations.filter((entry) => entry.packageId === packet.id && !["closed", "cancelled"].includes(entry.status) && (!entry.scope || entry.scope.stepIds.includes(step.id) && pieces.some((piece) => entry.scope.pieceNumbers.includes(piece))));
      check(outstanding.every((entry) => entry.id === deviation?.id), "此范围存在未闭环先行申请，须关联明确授权，不能作为正常执行绕过");
      const execution = { id: randomUUID(), packageId: packet.id, orderId: packet.orderId, lineId: packet.lineId, batch: packet.batch, stepId: step.id, pieceNumbers: pieces, kind, quantity, result: choice(command.result, ["pass", "fail", "recorded"], "执行结果"), actual: requiredText(command.actual, "实际执行说明"), actorId: actor.id, at: now, deviationId: deviation?.id };
      state.executions.push(execution); deviation?.executionIds.push(execution.id); targetId = execution.id; break;
    }
    case "complete-package": {
      authorize(actor, "quality"); const packet = get(state.packages, command.packageId);
      checkProductionBlockers(factoryPackageCompletionBlockers(state, packet));
      packet.status = "complete";
      const order = get(state.orders, packet.orderId);
      order.status = factoryOrderProductionStatus(state, order);
      targetId = packet.id; break;
    }
    case "create-change": {
      authorize(actor, "sales", "designer", "process", "procurement", "production", "quality");
      const packet = get(state.packages, command.packageId); check(["issued", "acknowledged", "complete"].includes(packet.status), "工程变更须引用已下达制造包");
      if (packet.changeId) check(get(state.changes, packet.changeId).status === "closed", "请先闭环来源变更，再从生效制造包发起后续修订");
      const release = get(state.releases, packet.releaseId); const design = newDesign(state, actor, `${release.name} 变更`, release.document, now);
      design.manufacturingRoute = structuredClone(release.calculation.route);
      const requested = stringList(command.pieceNumbers), pieces = requested.length ? requested : factoryEffectivePieceNumbers(state, packet);
      check(pieces.every((entry) => packet.pieceNumbers.includes(entry)), "变更件号不属于基础包");
      assertChangeScopeAvailable(state, packet, pieces); get(packet.route, command.fromStep);
      const change = { id: randomUUID(), orderId: packet.orderId, lineId: packet.lineId, basePackageId: packet.id, baseReleaseId: release.id, reason: requiredText(command.reason, "变更原因"), createdBy: actor.id, createdAt: now, designId: design.id, status: "designing" as const, differences: [], impact: { executionIds: state.executions.filter((entry) => entry.packageId === packet.id).map((entry) => entry.id), procurement: requiredText(command.procurement, "采购影响"), cost: requiredText(command.cost, "成本影响"), delivery: requiredText(command.delivery, "交期影响") }, effectivity: { batch: requiredText(command.batch, "生效批次", 100), fromStep: command.fromStep, pieceNumbers: pieces }, disposition: choice(command.disposition, ["use-as-is", "rework", "scrap", "replace", "isolate"], "旧件处置"), reviews: [] };
      design.baseReleaseId = release.id; design.drawingNumber = release.drawingNumber; design.changeId = change.id; state.changes.push(change); targetId = change.id; break;
    }
    case "create-change-scheme": {
      authorize(actor, "sales", "designer", "process", "procurement", "production", "quality");
      check(Array.isArray(command.scopes) && command.scopes.length >= 2 && command.scopes.length <= 200, "统一变更须包含2至200个批次", "INVALID_INPUT", 400);
      check(new Set(command.scopes.map((entry) => entry.packageId)).size === command.scopes.length, "同一制造包不能重复纳入统一变更", "INVALID_INPUT", 400);
      const name = requiredText(command.name, "方案名称", 200), reason = requiredText(command.reason, "变更原因");
      const procurement = requiredText(command.procurement, "采购影响"), cost = requiredText(command.cost, "成本影响"), delivery = requiredText(command.delivery, "交期影响");
      const scopes = command.scopes.map((entry) => {
        const packet = get(state.packages, entry.packageId);
        check(["issued", "acknowledged", "complete"].includes(packet.status), "工程变更须引用已下达制造包");
        if (packet.changeId) check(get(state.changes, packet.changeId).status === "closed", "请先闭环来源变更，再从生效制造包发起后续修订");
        const requested = stringList(entry.pieceNumbers), pieces = requested.length ? requested : factoryEffectivePieceNumbers(state, packet);
        assertChangeScopeAvailable(state, packet, pieces); get(packet.route, entry.fromStep);
        return { packet, release: get(state.releases, packet.releaseId), pieces, fromStep: entry.fromStep, batch: requiredText(entry.batch, "生效批次", 100), disposition: choice(entry.disposition, ["use-as-is", "rework", "scrap", "replace", "isolate"], "旧件处置") };
      });
      const first = scopes[0]!;
      check(scopes.every((entry) => entry.packet.orderId === first.packet.orderId && entry.packet.lineId === first.packet.lineId && entry.release.productCode === first.release.productCode), "统一变更必须属于同一订单行和产品，跨产品请分别建立方案");
      const schemeId = randomUUID(), design = newDesign(state, actor, name, first.release.document, now);
      design.baseReleaseId = first.release.id; design.drawingNumber = first.release.drawingNumber; design.manufacturingRoute = structuredClone(first.release.calculation.route);
      const members: FactoryChange[] = scopes.map((entry) => ({
        id: randomUUID(), schemeId, orderId: entry.packet.orderId, lineId: entry.packet.lineId,
        basePackageId: entry.packet.id, baseReleaseId: entry.release.id, reason, createdBy: actor.id, createdAt: now, designId: design.id,
        status: "designing", differences: [], impact: { executionIds: [], procurement, cost, delivery },
        effectivity: { batch: entry.batch, fromStep: entry.fromStep, pieceNumbers: entry.pieces }, disposition: entry.disposition, reviews: []
      }));
      design.changeId = members[0]!.id;
      for (const member of members) member.impact.executionIds = changeImpactExecutions(state, member);
      state.changes.push(...members); (state.changeSchemes ??= []).push({ id: schemeId, name, createdBy: actor.id, createdAt: now, changeIds: members.map((entry) => entry.id) });
      targetId = schemeId; break;
    }
    case "submit-change":
    case "submit-change-scheme": {
      authorize(actor, "designer"); const group = commandChanges(state, command);
      check(group.members.every((entry) => entry.status === "designing" && entry.newReleaseId), "请先完成变更设计会签并发布新图纸");
      const differences = group.members.map((change) => {
        const before = get(state.releases, change.baseReleaseId), after = get(state.releases, change.newReleaseId);
        return compareFactoryVersions({ document: before.document, bom: before.calculation.lines, route: before.calculation.route }, { document: after.document, bom: after.calculation.lines, route: after.calculation.route });
      });
      check(differences.some((entries) => entries.length > 0), "新旧版本没有设计或制造差异");
      group.members.forEach((change, index) => { change.differences = differences[index]!; change.impact.executionIds = changeImpactExecutions(state, change); change.status = "impact-review"; change.reviews = []; });
      targetId = group.targetId; break;
    }
    case "refresh-change-impact":
    case "refresh-change-scheme-impact": {
      authorize(actor, "designer", "production"); const group = commandChanges(state, command);
      check(group.members.every((entry) => ["impact-review", "approved"].includes(entry.status)), "当前变更不需要重新会签");
      for (const change of group.members) { change.impact.executionIds = changeImpactExecutions(state, change); change.reviews = []; change.status = "impact-review"; }
      targetId = group.targetId; break;
    }
    case "revise-change":
    case "revise-change-scheme": {
      authorize(actor, "designer"); const group = commandChanges(state, command), change = group.members[0]!;
      check(group.members.every((entry) => entry.status === "designing" && entry.newReleaseId), "仅退回且已有发布修订的变更可以重新设计");
      check(change.newReleaseId, "请先发布变更图纸");
      const design = newDesign(state, actor, `${get(state.releases, change.newReleaseId).name} 再修订`, get(state.releases, change.newReleaseId).document, now);
      design.baseReleaseId = change.newReleaseId; design.drawingNumber = get(state.releases, change.newReleaseId).drawingNumber; design.changeId = change.id;
      design.manufacturingRoute = structuredClone(get(state.releases, change.newReleaseId).calculation.route);
      for (const member of group.members) { member.designId = design.id; delete member.newReleaseId; delete member.customerConfirmation; member.reviews = []; }
      targetId = design.id; break;
    }
    case "withdraw-change":
    case "withdraw-change-scheme": {
      authorize(actor, "sales", "designer", "process", "procurement", "production", "quality");
      const group = commandChanges(state, command);
      check(group.members.every((entry) => entry.createdBy === actor.id && ["designing", "impact-review", "approved"].includes(entry.status)), "仅发起人可以撤回尚未下达的变更", "FORBIDDEN", 403);
      const cancellation = { note: requiredText(command.note, "撤回原因"), actorId: actor.id, at: now };
      for (const change of group.members) {
        change.cancellation = { ...cancellation }; change.status = "cancelled";
        for (const deviation of state.deviations.filter((entry) => entry.changeId === change.id && entry.status === "reconciling")) { delete deviation.changeId; deviation.status = "authorized"; }
      }
      targetId = group.targetId; break;
    }
    case "review-change":
    case "review-change-scheme": {
      const group = commandChanges(state, command), leader = group.members[0]!;
      check(group.members.every((entry) => entry.status === "impact-review"), "变更不在影响会签阶段"); choice(command.role, DESIGN_REVIEW_ROLES, "变更会签岗位");
      const entries = upsertReview(leader.reviews, review(actor, command.role, leader.createdBy, command.decision, command.note, state.revision, now));
      for (const change of group.members) { change.reviews = structuredClone(entries); change.status = command.decision === "reject" ? "designing" : accepted(entries, DESIGN_REVIEW_ROLES) ? "approved" : "impact-review"; }
      targetId = group.targetId; break;
    }
    case "issue-change":
    case "issue-change-scheme": {
      authorize(actor, "production"); const group = commandChanges(state, command), leader = group.members[0]!, order = get(state.orders, leader.orderId), line = get(order.lines, leader.lineId);
      // Preflight the entire group before mutating any effective piece or order revision.
      checkProductionBlockers("schemeId" in command ? factoryChangeSchemeIssueBlockers(state, command.schemeId) : factoryChangeIssueBlockers(state, leader));
      order.revision++; order.updatedAt = now;
      for (const change of group.members) {
        const base = get(state.packages, change.basePackageId);
        const packet = makePackage(state, actor, order, line, change.newReleaseId!, change.effectivity.batch, now, change.effectivity.pieceNumbers.length || base.quantity);
        packet.changeId = change.id; packet.supersedes = base.id; packet.reviews = structuredClone(change.reviews); packet.status = "issued";
        if (change.disposition !== "replace") packet.pieceNumbers = [...(change.effectivity.pieceNumbers.length ? change.effectivity.pieceNumbers : base.pieceNumbers)];
        change.newPackageId = packet.id; change.status = "issued";
      }
      order.status = factoryOrderProductionStatus(state, order); targetId = group.targetId; break;
    }
    case "acknowledge-change": {
      authorize(actor, "operator"); const change = get(state.changes, command.changeId); check(change.status === "issued" && change.newPackageId, "变更尚未下达");
      get(state.packages, change.newPackageId).status = "acknowledged"; change.status = "acknowledged";
      const order = get(state.orders, change.orderId); order.status = factoryOrderProductionStatus(state, order); targetId = change.id; break;
    }
    case "record-change-disposition": {
      authorize(actor, "operator", "production"); const change = get(state.changes, command.changeId);
      check(change.status === "acknowledged" && change.newPackageId, "需先接收变更才能记录旧件处置");
      const base = get(state.packages, change.basePackageId), packet = get(state.packages, change.newPackageId);
      const affected = change.effectivity.pieceNumbers.length ? change.effectivity.pieceNumbers : base.pieceNumbers;
      const pieces = stringList(command.pieceNumbers), executionIds = stringList(command.executionIds);
      check(pieces.length && pieces.every((piece) => affected.includes(piece)), "处置件号必须属于变更生效范围");
      const disposition = choice(command.disposition, ["use-as-is", "rework", "scrap", "replace", "isolate"], "实际处置");
      // Isolation is an interim hold. Final release requires two independent confirmations.
      check(disposition === change.disposition || change.disposition === "isolate" && disposition !== "replace", "实际处置不符合已批准方案，须重新发起工程变更");
      const records = change.dispositionRecords ?? [];
      check(!records.some((entry) => entry.disposition !== "isolate" && accepted(entry.reviews, ["production", "quality"]) && entry.pieceNumbers.some((piece) => pieces.includes(piece))), "这些件号已有已确认的最终处置，不可覆盖");
      const evidence = executionIds.map((id) => get(state.executions, id));
      check(evidence.every((entry) => [base.id, packet.id].includes(entry.packageId) && entry.pieceNumbers.every((piece) => pieces.includes(piece))), "处置实际记录必须对应当前变更与所选件号");
      if (disposition === "rework" || disposition === "scrap") {
        check(pieces.every((piece) => evidence.some((entry) => entry.kind === disposition && entry.result !== "fail" && entry.pieceNumbers.includes(piece))), "返工或报废必须逐件关联对应的成功实际记录");
      }
      check(Array.isArray(command.replacementPairs) && command.replacementPairs.length <= 1000, "替换件对应关系无效");
      const pairs = command.replacementPairs.map((entry) => ({ oldPiece: requiredText(entry.oldPiece, "原件号", 200), newPiece: requiredText(entry.newPiece, "新件号", 200) }));
      if (disposition === "replace") {
        check(pairs.length === pieces.length && new Set(pairs.map((entry) => entry.oldPiece)).size === pieces.length && new Set(pairs.map((entry) => entry.newPiece)).size === pieces.length && pairs.every((entry) => pieces.includes(entry.oldPiece) && packet.pieceNumbers.includes(entry.newPiece)), "替换须建立逐件一对一的新旧件号关系");
        check(!records.some((entry) => accepted(entry.reviews, ["production", "quality"]) && entry.replacementPairs.some((pair) => pairs.some((other) => other.newPiece === pair.newPiece))), "替换新件号已用于其他已确认处置");
      } else check(!pairs.length, "非替换处置不能填写新旧件号关系");
      const record = { id: randomUUID(), pieceNumbers: pieces, disposition, executionIds, replacementPairs: pairs, note: requiredText(command.note, "处置实况及旧件去向"), actorId: actor.id, at: now, reviews: [] };
      (change.dispositionRecords ??= []).push(record); targetId = record.id; break;
    }
    case "review-change-disposition": {
      const change = get(state.changes, command.changeId);
      check(change.status === "acknowledged", "仅执行中的变更可以确认处置");
      const record = get(change.dispositionRecords ?? [], command.recordId);
      choice(command.role, ["production", "quality"], "处置确认岗位");
      check(!record.reviews.some((entry) => entry.decision === "reject") && !accepted(record.reviews, ["production", "quality"]), "此处置记录已定稿或退回，请补充新记录而非覆盖");
      check(!record.reviews.some((entry) => entry.role === command.role || entry.actorId === actor.id), "同一人员不能重复或跨岗位确认同一处置");
      const other = (change.dispositionRecords ?? []).filter((entry) => entry.id !== record.id && entry.disposition !== "isolate" && accepted(entry.reviews, ["production", "quality"]));
      check(!other.some((entry) => entry.pieceNumbers.some((piece) => record.pieceNumbers.includes(piece)) || entry.replacementPairs.some((pair) => record.replacementPairs.some((otherPair) => pair.newPiece === otherPair.newPiece))), "已有已确认处置占用同一件号，请补充新记录");
      record.reviews.push(review(actor, command.role, record.actorId, command.decision, command.note, state.revision, now)); targetId = record.id; break;
    }
    case "close-change": {
      authorize(actor, "quality"); const change = get(state.changes, command.changeId);
      checkProductionBlockers(factoryChangeClosureBlockers(state, change));
      change.closure = { note: requiredText(command.note, "旧件处置与新版本验证结论"), actorId: actor.id, at: now }; change.status = "closed"; targetId = change.id; break;
    }
    case "create-deviation": {
      authorize(actor, "operator", "production", "quality"); const packet = get(state.packages, command.packageId); check(packet.status === "acknowledged", "先行处置必须关联车间已接收制造包");
      const scope = deviationScope(packet, command);
      check(!state.deviations.some((entry) => entry.packageId === packet.id && !["closed", "cancelled"].includes(entry.status) && (!entry.scope || entry.scope.pieceNumbers.some((piece) => scope.pieceNumbers.includes(piece)) && entry.scope.stepIds.some((stepId) => scope.stepIds.includes(stepId)))), "此件号与工序已存在未闭环先行申请，请修订原申请");
      const deviation = { id: randomUUID(), packageId: packet.id, reason: requiredText(command.reason, "先行原因"), actual: requiredText(command.actual, "拟先行/实际做法"), scope, createdBy: actor.id, createdAt: now, status: "requested" as const, reviews: [], executionIds: [] };
      state.deviations.push(deviation); targetId = deviation.id; break;
    }
    case "revise-deviation": {
      authorize(actor, "operator", "production", "quality"); const deviation = get(state.deviations, command.deviationId);
      check(deviation.createdBy === actor.id && ["requested", "returned"].includes(deviation.status), "仅申请人能修订尚未授权或已退回的先行申请", "FORBIDDEN", 403);
      const packet = get(state.packages, deviation.packageId); check(packet.status === "acknowledged", "制造包已经结束，不能修订先行申请");
      const scope = deviationScope(packet, command);
      check(!state.deviations.some((entry) => entry.id !== deviation.id && entry.packageId === packet.id && !["closed", "cancelled"].includes(entry.status) && (!entry.scope || entry.scope.pieceNumbers.some((piece) => scope.pieceNumbers.includes(piece)) && entry.scope.stepIds.some((stepId) => scope.stepIds.includes(stepId)))), "修订范围与其他未闭环申请重叠");
      deviation.reason = requiredText(command.reason, "先行原因"); deviation.actual = requiredText(command.actual, "拟先行/实际做法"); deviation.scope = scope;
      deviation.reviews = []; deviation.status = "requested"; targetId = deviation.id; break;
    }
    case "withdraw-deviation": {
      const deviation = get(state.deviations, command.deviationId);
      check(deviation.createdBy === actor.id && ["requested", "returned"].includes(deviation.status) && !deviation.executionIds.length, "仅申请人能撤回未授权且未发生实际执行的申请", "FORBIDDEN", 403);
      deviation.resolution = requiredText(command.note, "撤回原因"); deviation.status = "cancelled"; deviation.closedBy = actor.id; deviation.closedAt = now; targetId = deviation.id; break;
    }
    case "review-deviation": {
      const deviation = get(state.deviations, command.deviationId); check(deviation.status === "requested", "先行处置已审批"); choice(command.role, ["production", "quality"], "先行批准岗位");
      deviation.reviews = upsertReview(deviation.reviews, review(actor, command.role, deviation.createdBy, command.decision, command.note, state.revision, now));
      if (command.decision === "reject") deviation.status = "returned";
      if (accepted(deviation.reviews, ["production", "quality"])) deviation.status = "authorized"; targetId = deviation.id; break;
    }
    case "link-deviation-change": {
      authorize(actor, "designer"); const deviation = get(state.deviations, command.deviationId), change = get(state.changes, command.changeId);
      check(deviation.status === "authorized" && change.basePackageId === deviation.packageId, "请先授权先行处置，并关联同一制造包的补图变更");
      check(change.status !== "cancelled", "已撤回变更不能用于补图对账");
      check(deviation.scope && (!change.effectivity.pieceNumbers.length || deviation.scope.pieceNumbers.every((piece) => change.effectivity.pieceNumbers.includes(piece))), "后补图变更必须覆盖全部先行生产件");
      deviation.changeId = change.id; deviation.status = "reconciling"; targetId = deviation.id; break;
    }
    case "close-deviation": {
      authorize(actor, "quality"); const deviation = get(state.deviations, command.deviationId);
      checkProductionBlockers(factoryDeviationClosureBlockers(state, deviation));
      deviation.resolution = requiredText(command.resolution, "设计/计划/实际差异处置结论"); deviation.closedBy = actor.id; deviation.closedAt = now; deviation.status = "closed"; targetId = deviation.id; break;
    }
    default: throw new FactoryError(400, "UNKNOWN_COMMAND", "不支持的业务命令");
  }
  state.revision++; state.savedAt = now;
  state.audit.push({ id: randomUUID(), actorId: actor.id, at: now, action: command.type, targetId, workspaceRevision: state.revision, note: "note" in command ? String(command.note) : "reason" in command ? String(command.reason) : "resolution" in command ? String(command.resolution) : "" });
  return targetId;
}
