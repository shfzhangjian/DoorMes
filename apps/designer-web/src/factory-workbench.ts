import type { DesignSession } from "@doormes/application";
import type { DesignDocument } from "@doormes/contracts";
import { describeFactoryProduct, factoryRequirementChecks } from "@doormes/application/factory-product";
import { FACTORY_ROLES, FACTORY_ROLE_LABELS, DESIGN_REVIEW_ROLES, factoryAllocatedQuantity, factoryReleaseScope, factoryEffectivePieceNumbers, factoryEngineeringRequestCurrent, type FactoryActor, type FactoryCalculation, type FactoryCommand, type FactoryDesign, type FactoryDeviation, type FactoryDisposition, type FactoryChange, type FactoryManufacturingPackage, type FactoryOrder, type FactoryOrderLine, type FactoryRequirement, type FactoryRole, type FactoryWorkspace, type FactoryMutationRequest, type FactoryMutationResponse, type FactoryDifference } from "@doormes/contracts/factory-workflow";
import { renderDesignSvg } from "@doormes/renderer-svg";
import { factoryLastStepResult, factoryStepPieceNumbers, factoryPackageCompletionBlockers, factoryChangeIssueBlockers, factoryChangeSchemeIssueBlockers, factoryChangeSchemeStatus, factoryChangeClosureBlockers, factoryDeviationClosureBlockers, factoryPackageCancellationBlockers, type FactoryProductionBlocker } from "@doormes/contracts/factory-production-status";
import { FactoryApiClient, FactoryClientError, type FactoryAccount } from "./factory-api-client.js";
import { describeFactoryHistory, factoryComparisonCandidates } from "./factory-history.js";
import { factoryCanEditDesign, factoryOwnsDesign, factoryReviewRoles, factoryExecutionKinds, factoryExecutablePieces, type FactoryReviewType } from "./factory-ui-policy.js";
import "./factory-workbench.css";

export const FACTORY_PAGES = ["orders", "tasks", "designs", "reviews", "packages", "changes", "deviations", "history", "users"] as const;
type Page = typeof FACTORY_PAGES[number];
const PAGE_LABELS: Record<Page, string> = { orders: "订单与设计需求", tasks: "研发任务", designs: "设计图纸库", reviews: "待办会签", packages: "制造包与执行", changes: "工程变更", deviations: "先行处置 / 后补图", history: "历史版本", users: "账号与岗位" };
export function factoryPagesFor(actor: FactoryActor): Page[] {
  if (actor.roles.includes("admin") && actor.roles.length === 1) return ["users", "history"];
  const first: Page = actor.roles.includes("sales") ? "orders" : actor.roles.includes("designer") ? "tasks" : actor.roles.includes("operator") ? "packages" : "reviews";
  return [first, ...FACTORY_PAGES.filter((page) => page !== first && (page !== "users" || actor.roles.includes("admin")))];
}
const STATUS: Record<string, string> = { draft: "草稿", "in-review": "会签中", returned: "已退回", approved: "已通过", released: "已发布", confirmed: "已确认", issued: "已下达", "in-production": "生产中", complete: "已完成", review: "制造包会签", acknowledged: "已接收", superseded: "已替代", cancelled: "已撤销 / 撤回", designing: "设计中", "impact-review": "影响会签", closed: "已闭环", requested: "待授权", authorized: "已授权", reconciling: "补图对账", approve: "通过", reject: "退回" };
function node<K extends keyof HTMLElementTagNameMap>(tag: K, text = "", className = ""): HTMLElementTagNameMap[K] {
  const element = document.createElement(tag); element.textContent = text; element.className = className; return element;
}
function button(label: string, action: () => void | Promise<void>, className = ""): HTMLButtonElement {
  const element = node("button", label, className); element.type = "button"; element.addEventListener("click", () => { void action(); }); return element;
}
function table(headers: string[], rows: (string | HTMLElement)[][]): HTMLElement {
  const wrap = node("div", "", "factory-table-wrap"), result = node("table", "", "factory-table"), head = node("thead"), line = node("tr"), body = node("tbody");
  for (const text of headers) line.append(node("th", text)); head.append(line); result.append(head);
  for (const values of rows) { const tr = node("tr"); for (const value of values) { const td = node("td"); if (typeof value === "string") td.textContent = value; else td.append(value); tr.append(td); } body.append(tr); }
  result.append(body); wrap.append(result); return wrap;
}
function guardedAction(action: HTMLButtonElement, blockers: FactoryProductionBlocker[]): HTMLElement {
  const wrap = node("div", "", "factory-action-gate");
  action.disabled = blockers.length > 0;
  wrap.append(action);
  if (blockers.length) {
    const list = node("ul", "", "factory-blockers");
    for (const entry of blockers) list.append(node("li", entry.message + (entry.pieceNumbers.length ? "；件号：" + entry.pieceNumbers.join("、") : "") + (entry.referenceIds.length ? "；关联记录：" + entry.referenceIds.join("、") : "")));
    wrap.append(list);
  }
  return wrap;
}
interface Field { key: string; label: string; value?: string | number; type?: "text" | "number" | "date" | "password" | "textarea"; options?: [string, string][]; required?: boolean }
type Values = Record<string, string>;
const text = (key: string, label: string, value = "", required = true): Field => ({ key, label, value, required });
const area = (key: string, label: string, value = "", required = true): Field => ({ key, label, value, required, type: "textarea" });
const number = (key: string, label: string, value: number): Field => ({ key, label, value, type: "number" });
const select = (key: string, label: string, options: [string, string][], value?: string): Field => ({ key, label, options, value });
const pieces = (value: string) => value.split(/\r?\n/u).map((item) => item.trim()).filter(Boolean);
const DISPOSITION_LABELS: Record<FactoryDisposition, string> = { "use-as-is": "沿用", rework: "返工", scrap: "报废", replace: "替换", isolate: "隔离（非最终处置）" };

/** Authenticated business workbench wraps, but does not replace, the graph engine. */
export function mountFactoryWorkbench(root: HTMLElement, session: DesignSession, onLoad: () => void): () => void {
  const api = new FactoryApiClient(), bar = node("header", "", "factory-global-bar"), overlay = node("section", "", "factory-workbench"), notice = node("output", "", "factory-notice");
  notice.setAttribute("aria-live", "polite"); document.body.append(bar, overlay, notice); document.body.classList.add("factory-mode"); root.hidden = true;
  let actor: FactoryActor | undefined, state: FactoryWorkspace | undefined, page: Page = "designs", activeDesignId: string | undefined;
  let people: FactoryActor[] = [];
  let baseline = "", dirty = false, busy = false, disposed = false, drawing = false, pending: FactoryMutationRequest | undefined;
  const dialogs = new Set<HTMLDialogElement>(), has = (...roles: FactoryRole[]) => !!actor?.roles.some((role) => roles.includes(role));
  function message(text: string, error = false) { notice.textContent = text; notice.dataset.error = String(error); }
  function fail(error: unknown) {
    message(error instanceof Error ? error.message : "操作失败", true);
    if (error instanceof FactoryClientError && error.status === 401) { actor = undefined; state = undefined; drawing = false; renderLogin(); }
  }
  async function safely(action: () => Promise<void>) { if (busy || disposed) return; busy = true; try { await action(); } catch (error) { fail(error); } finally { busy = false; } }
  function form(title: string, fields: Field[], apply: (values: Values) => Promise<void>, help = "", after?: (element: HTMLFormElement) => void) {
    const dialog = node("dialog", "", "factory-form-dialog"), element = node("form"), inputs = node("div", "", "factory-form-grid"), feedback = node("output"), submit = node("button", "确认", "primary");
    element.append(node("h2", title)); if (help) element.append(node("p", help));
    for (const field of fields) {
      const label = node("label", field.label), input = field.options ? node("select") : field.type === "textarea" ? node("textarea") : node("input");
      input.name = field.key; input.required = field.required !== false;
      if (input instanceof HTMLInputElement) { input.type = field.type === "textarea" ? "text" : field.type ?? "text"; if (field.type === "number") { input.step = "any"; input.min = "0.001"; } }
      if (field.options && input instanceof HTMLSelectElement) for (const [value, name] of field.options) { const option = node("option", name); option.value = value; input.append(option); }
      input.value = String(field.value ?? field.options?.[0]?.[0] ?? ""); label.append(input); inputs.append(label);
    }
    const close = () => { dialog.close(); dialog.remove(); dialogs.delete(dialog); }; submit.type = "submit";
    element.append(inputs, feedback, actions(submit, button("取消", close))); dialog.append(element); document.body.append(dialog); dialogs.add(dialog); after?.(element);
    element.addEventListener("submit", (event) => { event.preventDefault(); if (submit.disabled) return; submit.disabled = true; feedback.textContent = "处理中…";
      const values = Object.fromEntries([...new FormData(element)].map(([key, value]) => [key, String(value)]));
      void apply(values).then(close).catch((error: unknown) => { feedback.textContent = error instanceof Error ? error.message : "操作失败"; fail(error); }).finally(() => { submit.disabled = false; });
    });
    dialog.addEventListener("cancel", () => { dialogs.delete(dialog); dialog.remove(); }); dialog.showModal();
  }
  function acceptMutation(request: FactoryMutationRequest, result: FactoryMutationResponse) {
    state = result.workspace;
    if (request.command.type === "save-design") {
      activeDesignId = result.targetId;
      baseline = JSON.stringify(request.command.document);
      dirty = JSON.stringify(session.document) !== baseline;
    }
    pending = undefined; render();
  }
  async function command(value: FactoryCommand): Promise<string> {
    if (!state || !actor) throw new Error("请登录并加载后端数据");
    if (pending) throw new Error("上一操作结果未确认，请先重试或刷新核对");
    pending = api.transaction(value, state.revision);
    const request = pending;
    try { const result = await api.mutate(request); acceptMutation(request, result); message("已保存到后端 · R" + state.revision); return result.targetId; }
    catch (error) { if (error instanceof FactoryClientError && error.status < 500) pending = undefined; throw error; }
  }
  function actions(...entries: (HTMLElement | undefined)[]): HTMLElement { const result = node("div", "", "factory-actions"); for (const entry of entries) if (entry) result.append(entry); return result; }
  function cmd(label: string, value: FactoryCommand) { return button(label, () => safely(async () => { await command(value); })); }
  function reviewButtons(type: FactoryReviewType, id: string, roles: readonly FactoryRole[]) {
    const available = factoryReviewRoles(actor, state!, type, id).filter((role) => roles.includes(role));
    if (!available.length && roles.some((role) => has(role))) return actions(node("span", "提交人不能会签自己的方案，请由对应岗位的其他人员办理。"));
    return actions(...available.map((role) => button(FACTORY_ROLE_LABELS[role] + "会签 / 调整结论", () => form("会签结论", [select("decision", "结论", [["approve", "通过"], ["reject", "退回修改"]]), area("note", "审核说明")], async (value) => {
      const key = type === "review-design" ? "designId" : type === "review-package" ? "packageId" : type === "review-change" ? "changeId" : type === "review-change-scheme" ? "schemeId" : "deviationId";
      await command({ type, [key]: id, role, decision: value.decision, note: value.note } as FactoryCommand);
    }))));
  }
  function reviews(entries: { actorId: string; role: FactoryRole; decision: string; note: string; at: string }[]) {
    return table(["岗位", "会签人 / 身份ID", "结论", "说明", "时间"], entries.map((entry) => [FACTORY_ROLE_LABELS[entry.role], (people.find((person) => person.id === entry.actorId)?.name ?? "历史人员") + " / " + entry.actorId, STATUS[entry.decision] ?? entry.decision, entry.note, new Date(entry.at).toLocaleString()]));
  }
  function detail(title: string, ...parts: HTMLElement[]) { const item = node("details", "", "factory-card"); item.append(node("summary", title), ...parts); return item; }
  function preview(value: DesignDocument) { const holder = node("div", "", "factory-drawing-thumbnail"); holder.innerHTML = renderDesignSvg(value, { renderStyle: "engineering-line" }); return holder; }
  function bom(packet: Pick<FactoryManufacturingPackage, "bom" | "route">) {
    return detail("组成件 BOM 与工艺路线", table(["来源窗", "材料编码", "名称", "规格/颜色", "净长×宽×高 mm", "毛料长度 mm", "端切角", "数量"], packet.bom.map((line) => [line.sourceMark, line.materialCode, line.name, line.specification + " / " + line.color, [line.lengthMm, line.widthMm, line.heightMm].join("×"), String(line.grossLengthMm), line.cutLeftDeg + "° / " + line.cutRightDeg + "°", line.quantity + " " + line.unit])), table(["顺序", "工序", "说明"], packet.route.map((step, index) => [String(index + 1), step.name, step.instruction])));
  }
  function renderBar() {
    const active = state?.designs.find((entry) => entry.id === activeDesignId);
    const editable = !!actor && has("designer") && (!activeDesignId || !!active && !!state && factoryCanEditDesign(actor, state, active));
    root.inert = !editable;
    bar.replaceChildren(node("strong", "DoorMes · 工厂协同原型")); if (!actor) return;
    bar.append(node("span", actor.name + " · " + actor.roles.map((role) => FACTORY_ROLE_LABELS[role]).join(" / ")), button("岗位工作台", () => { drawing = false; render(); }));
    if (has("designer")) {
      const save = button(drawing ? editable ? "保存设计到后端" : "当前图纸只读" : "绘图工作台", () => { if (!drawing) { drawing = true; render(); } else saveDrawing(); });
      save.disabled = drawing && !editable; bar.append(save);
    }
    bar.append(button("退出登录", () => {
      if (dirty && !window.confirm("画布有未保存到后端的修改，确认退出？本地恢复副本不会删除。")) return;
      void safely(async () => { await api.logout(); actor = undefined; state = undefined; activeDesignId = undefined; baseline = ""; dirty = false; drawing = false; renderLogin(); });
    }));
    if (drawing) bar.append(node("span", (active?.drawingNumber ?? (activeDesignId ? "挂接图纸不可用" : "未挂接后端设计")) + (dirty ? " · 未保存" : "") + (!editable ? " · 已锁定，请领取任务或建立新修订" : "")));
  }
  function renderLogin() {
    for (const dialog of dialogs) { dialog.close(); dialog.remove(); } dialogs.clear();
    root.hidden = true; overlay.hidden = false; renderBar(); overlay.replaceChildren();
    const panel = node("form", "", "factory-login"), username = node("input"), password = node("input"), submit = node("button", "登录工厂工作台", "primary"), feedback = node("output");
    username.autocomplete = "username"; username.required = true; username.placeholder = "岗位账号，例如 designer";
    password.type = "password"; password.autocomplete = "current-password"; password.required = true;
    const userLabel = node("label", "账号"), passLabel = node("label", "密码"); userLabel.append(username); passLabel.append(password);
    panel.append(node("h1", "工厂设计与生产协同"), node("p", "标准图纸复用 · 定制设计 · 会签发布 · 版本变更 · 车间执行"), userLabel, passLabel, feedback, submit, node("small", "原型账号：sales / designer / reviewer / process / procurement / production / operator / quality / admin。初始密码见本地配置说明。"));
    panel.addEventListener("submit", (event) => { event.preventDefault(); submit.disabled = true; feedback.textContent = "正在登录…";
      void (async () => { try { actor = (await api.login(username.value, password.value)).actor; state = await api.workspace(); people = (await api.people()).people; if (disposed) return; page = factoryPagesFor(actor)[0]!; render(); message("已加载后端数据。参考BOM/工艺仅供流程演练。"); } catch (error) { feedback.textContent = error instanceof Error ? error.message : "登录失败"; fail(error); } finally { submit.disabled = false; } })();
    }); overlay.append(panel);
  }
  function openDesign(design: FactoryDesign) {
    if (!has("designer") || !["draft", "returned"].includes(design.status)) return;
    if (!canWorkOn(design)) { message("请先领取属于自己的研发任务。", true); return; }
    if (dirty && !window.confirm("画布尚未保存到后端，确认切换？")) return;
    activeDesignId = design.id; session.replaceDocument(structuredClone(design.document)); baseline = JSON.stringify(session.document); dirty = false; onLoad(); drawing = true; render();
  }
  function saveDrawing(asNew = false) {
    const designId = asNew ? undefined : activeDesignId, design = state?.designs.find((entry) => entry.id === designId);
    if (!actor || !has("designer") || !state || designId && (!design || !factoryCanEditDesign(actor, state, design))) { message("此图纸已锁定或任务不属于你，请领取任务或建立新修订。", true); return; }
    form("保存设计图纸", [text("name", "设计名称", design?.name), text("drawingNumber", "图纸编号", design?.drawingNumber ?? "DM-" + Date.now())], async (value) => {
      const current = state?.designs.find((entry) => entry.id === designId);
      if (designId && (!current || !state || !factoryCanEditDesign(actor, state, current))) throw new Error("图纸状态或任务归属已变化，请刷新后重新办理。");
      const captured = structuredClone(session.document), json = JSON.stringify(captured);
      activeDesignId = await command({ type: "save-design", designId, name: value.name!, drawingNumber: value.drawingNumber!, document: captured });
      baseline = json; dirty = JSON.stringify(session.document) !== baseline; renderBar();
    }, "仅保存草稿。会签中与已发布图纸不可覆盖；通过退回修改或发布库修订创建下一版本。");
  }
  function requirementFields(releaseId?: string, demand?: FactoryRequirement): Field[] {
    const release = state?.releases.find((entry) => entry.id === releaseId), specification = release ? describeFactoryProduct(release.document) : undefined;
    return [number("widthMm", "产品展开总宽 mm", demand?.widthMm ?? specification?.widthMm ?? 1200), number("heightMm", "产品总高 mm", demand?.heightMm ?? specification?.heightMm ?? 1500),
      text("material", "型材系统", demand?.material ?? specification?.material ?? "AL70"), text("glass", "玻璃型号", demand?.glass ?? specification?.glass ?? "GL-LOWE-24"),
      text("hardware", "五金系统", demand?.hardware ?? specification?.hardware ?? "HW-TT-STD"), text("finish", "表面/颜色要求", demand?.finish ?? specification?.finish ?? "内RAL9016 / 外RAL7016"),
      { key: "dueDate", label: "要求日期", type: "date", required: false, value: demand?.dueDate }, area("note", "需求备注", demand?.note ?? "", false)];
  }
  function requirementRevisionForm(order: FactoryOrder, line: FactoryOrderLine) {
    const options: [string, string][] = state!.releases.map((entry) => [entry.id, (factoryReleaseScope(state!, entry).kind === "catalog" ? "标准产品 · " : "定制参考 · ") + entry.productCode + " V" + entry.version]);
    const packets = state!.packages.filter((entry) => entry.lineId === line.id && ["review", "approved"].includes(entry.status));
    form("修订需求 · " + order.number + " / " + line.mark + " · 需求 V" + (line.requirementRevision ?? 1), [
      select("kind", "修订后类型", [["custom", "定制：新建研发修订任务"], ["standard", "标准：选择明确批准版本"]], line.kind),
      { ...select("releaseId", "标准版本 / 可选定制参考", [["", "沿用当前图纸构造（由研发调整）"], ...options], line.kind === "standard" ? line.releaseId : ""), required: false },
      ...requirementFields(undefined, line.requirement), area("revisionNote", "修订原因与客户需求确认说明")
    ], async (value) => {
      await command({ type: "revise-order-requirement", orderId: order.id, lineId: line.id, kind: value.kind as "custom" | "standard", releaseId: value.releaseId || undefined, note: value.revisionNote!,
        requirement: { widthMm: Number(value.widthMm), heightMm: Number(value.heightMm), material: value.material!, glass: value.glass!, hardware: value.hardware!, finish: value.finish!, dueDate: value.dueDate ?? "", note: value.note ?? "" } });
    }, "新需求不会自动拉伸旧构造或替换材料。研发须重新领取、绘图核对并会签；客户须重新确认。旧图纸和意见保留，只读。将撤销未下达批次：" + (packets.map((entry) => entry.batch).join("、") || "无") + "。已下达批次必须走工程变更。", (element) => {
      const kind = element.elements.namedItem("kind") as HTMLSelectElement, release = element.elements.namedItem("releaseId") as HTMLSelectElement;
      const update = () => {
        const selected = release.value; release.replaceChildren();
        for (const [id, label] of [["", kind.value === "standard" ? "请选择批准的标准版本" : "沿用当前图纸构造（由研发调整）"], ...options] as [string, string][]) {
          const item = state!.releases.find((entry) => entry.id === id);
          if (kind.value === "standard" && item && factoryReleaseScope(state!, item).kind !== "catalog") continue;
          const option = node("option", label); option.value = id; release.append(option);
        }
        release.value = [...release.options].some((entry) => entry.value === selected) ? selected : "";
        release.required = kind.value === "standard";
      };
      update(); kind.addEventListener("change", update);
      release.addEventListener("change", () => {
        if (!release.value) return;
        for (const field of requirementFields(release.value)) if (field.value !== undefined && !["dueDate", "note"].includes(field.key)) (element.elements.namedItem(field.key) as HTMLInputElement).value = String(field.value);
      });
    });
  }
  function orderForm(order?: FactoryOrder) {
    const releaseOptions: [string, string][] = state!.releases.map((entry) => [entry.id, (factoryReleaseScope(state!, entry).kind === "catalog" ? "标准产品 · " : "订单专用 · ") + entry.productCode + " V" + entry.version + " · " + entry.name]);
    form(order ? "增加订单行" : "新增生产订单", [
      ...(!order ? [text("number", "订单号"), text("customer", "客户")] : []),
      select("kind", "产品类型", [["custom", "定制：生成设计需求"], ["standard", "标准：引用已发布图纸"]]),
      { ...select("releaseId", "标准版本 / 定制参考图纸", [["", "不引用历史图纸"], ...releaseOptions]), required: false }, number("quantity", "产品套数", 1), ...requirementFields()
    ], async (value) => {
      const common = { kind: value.kind as "standard" | "custom", quantity: Number(value.quantity), releaseId: value.releaseId || undefined,
        requirement: { widthMm: Number(value.widthMm), heightMm: Number(value.heightMm), material: value.material!, glass: value.glass!, hardware: value.hardware!, finish: value.finish!, dueDate: value.dueDate!, note: value.note! } };
      await command(order ? { type: "add-order-line", orderId: order.id, ...common } : { type: "create-order", number: value.number!, customer: value.customer!, ...common });
    }, "标准订单锁定完整发布产品。组合宽使用展开外廓，独立套件宽为各单元展开宽之和，不含安装间隔。规格/内外颜色变动请选择定制；引用历史图纸的定制保留原构造，由研发按需求调整。", (element) => {
      element.querySelector("select[name=kind]")?.addEventListener("change", () => {
        const input = element.elements.namedItem("releaseId") as HTMLSelectElement, selected = input.value, kind = (element.elements.namedItem("kind") as HTMLSelectElement).value;
        input.replaceChildren();
        for (const [id, label] of [["", "不引用历史图纸"], ...releaseOptions] as [string, string][]) {
          const release = state!.releases.find((entry) => entry.id === id);
          if (kind === "standard" && release && factoryReleaseScope(state!, release).kind !== "catalog") continue;
          const option = node("option", label); option.value = id; input.append(option);
        }
        input.value = [...input.options].some((entry) => entry.value === selected) ? selected : "";
      });
      element.querySelector("select[name=releaseId]")?.addEventListener("change", () => {
        const id = (element.elements.namedItem("releaseId") as HTMLSelectElement).value;
        for (const field of requirementFields(id)) if (field.value !== undefined && !["dueDate", "note"].includes(field.key)) (element.elements.namedItem(field.key) as HTMLInputElement).value = String(field.value);
      });
    });
  }
  function renderOrders(content: HTMLElement) {
    if (has("sales")) content.append(button("新增订单 / 设计需求", () => orderForm(), "primary"));
    for (const order of state!.orders) {
      const controls = actions(has("sales") && order.status === "draft" ? button("增加订单行", () => orderForm(order)) : undefined,
        has("sales") && order.status === "draft" ? cmd("确认订单", { type: "confirm-order", orderId: order.id }) : undefined);
      const rows = order.lines.map((line) => {
        const release = state!.releases.find((entry) => entry.id === line.releaseId), options = actions();
        const allocated = factoryAllocatedQuantity(state!.packages, line.id), remaining = line.quantity - allocated;
        if (has("sales")) {
          const issued = state!.packages.some((entry) => entry.lineId === line.id && !["review", "approved", "cancelled"].includes(entry.status));
          options.append(issued ? node("span", "需求变动请从生效批次发起工程变更") : button("修订尺寸 / 材料需求", () => requirementRevisionForm(order, line)));
        }
        if (has("sales", "designer", "process", "procurement", "production", "quality") && schemeCandidates(line.id).length >= 2) options.append(button("跨批统一变更", () => schemeForm(line.id)));
        if (has("sales")) options.append(button("修订订单数量", () => form("数量修订 · " + line.mark, [number("quantity", "新数量（整套产品）", line.quantity), area("note", "数量调整与客户确认说明")], async (value) => { await command({ type: "revise-order-quantity", orderId: order.id, lineId: line.id, quantity: Number(value.quantity), note: value.note! }); }, "最低不能小于已分配有效批次的 " + allocated + " 套。图纸、材料和已建批次不随数量修订改变；需先由生产撤销未下达批次才能释放其数量。")));
        const batches = state!.packages.filter((entry) => entry.lineId === line.id).map((entry) => entry.batch + "：V" + state!.releases.find((item) => item.id === entry.releaseId)!.version + " / 有效" + factoryEffectivePieceNumbers(state!, entry).length + "套 / " + STATUS[entry.status]);
        if (has("sales") && release && line.customerConfirmedReleaseId !== release.id) options.append(cmd("客户确认图纸版本", { type: "confirm-customer", orderId: order.id, lineId: line.id, releaseId: release.id }));
        if (has("production") && line.releaseId && order.status !== "draft" && line.customerConfirmedReleaseId === line.releaseId && remaining > 0) options.append(button("分批建立制造包", () => form("制造包批次", [text("batch", "生产批次"), number("quantity", "本批数量（整套产品）", remaining)], async (value) => { await command({ type: "create-package", orderId: order.id, lineId: line.id, batch: value.batch!, quantity: Number(value.quantity) }); }, "尚未分配 " + remaining + " 套；本批独立审核、下达和执行。")));
        if (has("production") && line.releaseId && line.customerConfirmedReleaseId !== line.releaseId && remaining > 0) options.append(node("span", "等待订单销售确认客户图纸版本"));
        return [line.mark + " · 需求 V" + (line.requirementRevision ?? 1), line.kind === "standard" ? "标准件" : "定制", line.requirement.widthMm + "×" + line.requirement.heightMm,
          [line.requirement.material, line.requirement.glass, line.requirement.hardware].join(" / "), line.requirement.finish + "\n" + line.requirement.note, line.quantity + "（已分配 " + allocated + " / 待分配 " + remaining + "）", (release ? "订单原基线：" + release.drawingNumber + " V" + release.version : "待设计") + "\n" + batches.join("\n"), options];
      });
      content.append(detail(order.number + " · " + order.customer + " · " + STATUS[order.status] + " · R" + order.revision, controls, table(["行号", "类型", "尺寸 mm", "材料需求", "颜色/说明", "数量", "图纸版本", "操作"], rows),
        table(["数量修订", "订单行", "原数量 → 新数量", "人员 / 时间", "调整说明"], (order.quantityRevisions ?? []).map((entry) => ["R" + entry.fromRevision + " → R" + entry.toRevision, order.lines.find((line) => line.id === entry.lineId)?.mark ?? entry.lineId, entry.fromQuantity + " → " + entry.toQuantity, (people.find((person) => person.id === entry.actorId)?.name ?? entry.actorId) + " / " + new Date(entry.at).toLocaleString(), entry.note])),
        ...[...(order.requirementRevisions ?? [])].reverse().map((entry) => detail("需求 V" + entry.fromRequirementRevision + " → V" + entry.toRequirementRevision + " · 订单 R" + entry.toRevision + " · " + entry.note,
          node("p", "修订人：" + (people.find((person) => person.id === entry.actorId)?.name ?? entry.actorId) + " · " + new Date(entry.at).toLocaleString() + "；撤销批次：" + (entry.cancelledPackageIds.map((id) => state!.packages.find((packet) => packet.id === id)?.batch ?? id).join("、") || "无")), differences(entry.differences)))));
    }
  }
  function canWorkOn(design: FactoryDesign): boolean {
    return !!state && factoryOwnsDesign(actor, state, design);
  }
  function renderTasks(content: HTMLElement) {
    for (const task of state!.engineeringRequests) {
      const design = state!.designs.find((entry) => entry.id === task.designId)!;
      const order = state!.orders.find((entry) => entry.id === task.orderId)!;
      const current = factoryEngineeringRequestCurrent(state!, task);
      const controls = actions();
      if (current && has("reviewer", "production") && ["draft", "returned"].includes(design.status)) controls.append(button(task.assigneeId ? "交接研发任务" : "分派研发任务", () => form("分派 / 交接研发任务", [
        select("assigneeId", "设计研发接收人", people.filter((person) => person.roles.includes("designer")).map((person) => [person.id, person.name]), task.assigneeId),
        area("note", "分派或交接说明")
      ], async (value) => { await command({ type: "assign-engineering", requestId: task.id, assigneeId: value.assigneeId!, note: value.note! }); })));
      if (current && has("designer") && !task.claimedAt && (!task.assigneeId || task.assigneeId === actor!.id) && ["draft", "returned"].includes(design.status)) controls.append(cmd("领取研发任务", { type: "claim-engineering", requestId: task.id }));
      if (canWorkOn(design) && ["draft", "returned"].includes(design.status)) controls.append(button("进入任务绘图", () => openDesign(design)));
      content.append(detail(order.number + " · " + order.lines.find((line) => line.id === task.lineId)!.mark + " · 需求 V" + (task.requirementRevision ?? 1) + " · " + (!current ? "历史任务（已失效，只读）" : task.status === "unassigned" ? "待分派" : task.status === "assigned" ? "待领取" : STATUS[task.status] ?? "设计中"),
        controls, node("p", "负责人：" + (people.find((person) => person.id === task.assigneeId)?.name ?? "未分派") + "；领取时间：" + (task.claimedAt ? new Date(task.claimedAt).toLocaleString() : "未领取")),
        node("p", "需求：" + task.requirement.widthMm + "×" + task.requirement.heightMm + " mm / " + [task.requirement.material, task.requirement.glass, task.requirement.hardware, task.requirement.finish].join(" / ") + "；交期：" + (task.requirement.dueDate || "未指定") + "；说明：" + task.requirement.note),
        designCard(design)));
    }
  }
  function designCard(design: FactoryDesign) {
    const controls = actions();
    const request = state!.engineeringRequests.find((entry) => entry.id === design.engineeringRequestId);
    const change = state!.changes.find((entry) => entry.id === design.changeId);
    const changeActive = (!design.changeId || change?.status === "designing" && change.designId === design.id) && (!design.engineeringRequestId || !!request && factoryEngineeringRequestCurrent(state!, request));
    const requirementChecks = request ? factoryRequirementChecks(design.document, request.requirement) : [];
    if (canWorkOn(design) && ["draft", "returned"].includes(design.status)) {
      const submit = cmd("计算BOM并提交会签", { type: "submit-design", designId: design.id });
      submit.disabled = requirementChecks.some((entry) => !entry.matches);
      controls.append(button("进入绘图", () => openDesign(design)), submit);
      if (submit.disabled) controls.append(node("span", "主规格与需求不一致，请进入绘图调整并保存后提交。"));
    }
    if (canWorkOn(design) && design.status === "approved") controls.append(button("发布设计版本", () => form("发布图纸", [text("productCode", "产品编号", state!.releases.find((entry) => entry.id === design.baseReleaseId)?.productCode)], async (value) => { await command({ type: "release-design", designId: design.id, productCode: value.productCode! }); })));
    if (changeActive && design.status === "in-review") controls.append(reviewButtons("review-design", design.id, DESIGN_REVIEW_ROLES));
    if (changeActive && has("process") && ["draft", "returned", "in-review"].includes(design.status)) controls.append(button("维护工艺路线", () => safely(async () => {
      const calculation = await api.request<FactoryCalculation>("/designs/" + design.id + "/calculation");
      const route = design.manufacturingRoute ?? calculation.route;
      const fields = route.flatMap((step, index) => [
        number("order-" + index, step.name + "：顺序", index + 1),
        text("name-" + index, "工序名称 · " + (index + 1), step.name),
        area("instruction-" + index, "加工/设备/检查说明", step.instruction),
        text("template-" + index, "加工模板编号（可选）", step.templateId, false)
      ]);
      fields.push(text("newId", "增加工序编号（可选）", "", false), text("newName", "新增工序名称", "", false), area("newInstruction", "新增工序说明", "", false), number("newOrder", "新增工序顺序", route.length - 0.5));
      form("调整设计加工路线", fields, async (value) => {
        const ordered = route.map((step, index) => ({ order: Number(value["order-" + index]), step: { ...step, name: value["name-" + index]!, instruction: value["instruction-" + index]!, templateId: value["template-" + index] || undefined } }));
        if (value.newId) ordered.push({ order: Number(value.newOrder), step: { id: value.newId, name: value.newName!, instruction: value.newInstruction!, sourceObjectIds: design.document.windows.map((window) => String(window.objectId)), templateId: undefined } });
        if (ordered.some((entry) => !Number.isFinite(entry.order))) throw new Error("工序顺序无效");
        await command({ type: "set-design-route", designId: design.id, route: ordered.sort((a, b) => a.order - b.order).map((entry) => entry.step) });
      }, "按顺序维护工序、说明和模板编号。首末须保留领料与终检。修改清除会签并须重新提交；企业设备/模板验证待接入。");
    })));
    const card = detail(design.drawingNumber + " · " + design.name + " · V" + design.version + " · " + STATUS[design.status], controls, preview(design.document));
    if (design.changeId && !changeActive) card.append(node("p", "此图纸为变更历史快照或已提交方案，只读；请从当前变更任务继续，或从生效制造包发起下一轮修订。"));
    if (request && !factoryEngineeringRequestCurrent(state!, request)) card.append(node("p", "订单需求已修订，本图纸和原会签只读保留，不再用于新需求发布。请进入最新研发任务。"));
    if (request) card.append(node("p", "订单需求：" + request.requirement.widthMm + "×" + request.requirement.heightMm + " mm · " + [request.requirement.material, request.requirement.glass, request.requirement.hardware, request.requirement.finish, request.requirement.note].join(" / ")));
    if (request) card.append(table(["主规格核对", "任务需求 V" + (request.requirementRevision ?? 1), "已保存图纸", "结果"], requirementChecks.map((entry) => [entry.label, String(entry.required), String(entry.actual), entry.matches ? "一致" : "不符，须调整"])), node("p", "交期与备注由各岗会签核对；此处校验整套主规格，不替代逐构件材料和加工规则验证。"));
    if (design.calculation) card.append(bom({ bom: design.calculation.lines, route: design.calculation.route }));
    card.append(reviews(design.reviews)); return card;
  }
  function renderDesigns(content: HTMLElement) {
    if (has("designer")) content.append(button("当前画布另存新设计", () => { if (dirty && !window.confirm("另存成功后会挂接新设计，原图纸保留，确认？")) return; saveDrawing(true); }, "primary"));
    for (const design of state!.designs) content.append(designCard(design));
    content.append(node("h2", "已发布图纸（标准目录 / 订单专用）"));
    for (const release of state!.releases) {
      const scope = factoryReleaseScope(state!, release);
      const scopeLabel = scope.kind === "catalog" ? "标准产品" : "订单专用：" + (state!.orders.find((entry) => entry.id === scope.orderId)?.number ?? scope.orderId);
      content.append(detail(scopeLabel + " · " + release.productCode + " · " + release.drawingNumber + " V" + release.version, preview(release.document),
        has("designer") && scope.kind === "catalog" ? button("从此版本新建标准修订", () => form("创建修订草稿", [text("name", "设计名称", release.name)], async (value) => { await command({ type: "fork-release", releaseId: release.id, name: value.name! }); })) : node("p", scope.kind === "catalog" ? "发布版本只读" : "订单修订只用于所属订单；后续变更从生效制造包发起。历史图纸可作为新的定制需求参考。")));
    }
  }
  function executionForm(packet: FactoryManufacturingPackage, disposalOnly = false) {
    const deviations: [string, string][] = state!.deviations.filter((entry) => entry.packageId === packet.id && entry.scope && ["authorized", "reconciling"].includes(entry.status)).map((entry) => [entry.id, entry.reason + "（" + entry.scope.pieceNumbers.join("、") + " / " + entry.scope.stepIds.join("、") + "）"]);
    const disposalPieces = state!.changes.filter((entry) => entry.basePackageId === packet.id && entry.status === "acknowledged" && ["rework", "scrap", "isolate"].includes(entry.disposition)).flatMap((entry) => entry.effectivity.pieceNumbers.length ? entry.effectivity.pieceNumbers : packet.pieceNumbers);
    const steps = packet.route.filter((entry) => factoryExecutionKinds(actor!, entry.id, disposalOnly).length > 0);
    const initialStep = has("quality") && !has("operator") ? "QUALITY" : steps[0]?.id;
    const kindLabels = { material: "领料", process: "加工", inspection: "质量终检", rework: "返工", scrap: "报废" };
    const kinds = (stepId: string): [string, string][] => factoryExecutionKinds(actor!, stepId, disposalOnly).map((kind) => [kind, kindLabels[kind]]);
    form("记录实际执行", [
      select("stepId", "当前版本工序", steps.map((entry) => [entry.id, entry.name]), initialStep),
      select("kind", "记录类型", kinds(initialStep ?? "")),
      area("pieceNumbers", "件号（每行一个）", (disposalOnly ? [...new Set(disposalPieces)] : factoryExecutablePieces(state!, packet, initialStep ?? "")).join("\n")), select("result", "结果", [["pass", "合格"], ["fail", "不合格"], ["recorded", "已记录"]]),
      area("actual", "实际尺寸/材料/加工/检查说明"), { ...select("deviationId", "先行授权", [["", "正常制造包执行"], ...deviations]), required: false }
    ], async (value) => { const ids = pieces(value.pieceNumbers!); await command({ type: "record-execution", packageId: packet.id, stepId: value.stepId!, kind: value.kind as "inspection", quantity: ids.length, pieceNumbers: ids, result: value.result as "pass", actual: value.actual!, deviationId: value.deviationId || undefined }); },
    "工序切换会同步记录类型和可按旧版执行的件号。先授权再记录先行实际执行；终检由质量岗位填写。真实时间由后端记录；后补图不能覆盖原始执行记录。", (element) => {
      const step = element.elements.namedItem("stepId") as HTMLSelectElement, kind = element.elements.namedItem("kind") as HTMLSelectElement, authorization = element.elements.namedItem("deviationId") as HTMLSelectElement;
      const updatePieces = () => {
        const disposal = disposalOnly || ["rework", "scrap"].includes(kind.value);
        let eligible = disposal ? [...new Set(disposalPieces)] : factoryExecutablePieces(state!, packet, step.value);
        const deviation = state!.deviations.find((entry) => entry.id === authorization.value);
        if (deviation) eligible = deviation.scope?.stepIds.includes(step.value) ? eligible.filter((piece) => deviation.scope.pieceNumbers.includes(piece)) : [];
        (element.elements.namedItem("pieceNumbers") as HTMLTextAreaElement).value = eligible.join("\n");
      };
      step.addEventListener("change", () => {
        kind.replaceChildren();
        for (const [id, label] of kinds(step.value)) { const option = node("option", label); option.value = id; kind.append(option); }
        updatePieces();
      });
      kind.addEventListener("change", updatePieces);
      authorization.addEventListener("change", updatePieces);
    });
  }
  function schemeCandidates(lineId: string) {
    return state!.packages.filter((packet) => packet.lineId === lineId && ["issued", "acknowledged", "complete"].includes(packet.status) && factoryEffectivePieceNumbers(state!, packet).length > 0
      && (!packet.changeId || state!.changes.some((entry) => entry.id === packet.changeId && entry.status === "closed")));
  }
  function schemeForm(lineId: string) {
    const candidates = schemeCandidates(lineId);
    if (candidates.length > 200) { message("超过200个候选批次，请按业务范围拆分方案。", true); return; }
    const fields: Field[] = [text("name", "统一变更方案名称"), area("reason", "客户/采购/工艺变更原因"), area("procurement", "采购影响"), area("cost", "成本影响"), area("delivery", "交期影响")];
    candidates.forEach((packet, index) => {
      const prefix = "scope-" + index, release = state!.releases.find((entry) => entry.id === packet.releaseId)!;
      fields.push(select(prefix + "-include", packet.batch + " · " + release.drawingNumber + " V" + release.version, [["no", "不纳入"], ["yes", "纳入本方案"]], index < 2 ? "yes" : "no"),
        text(prefix + "-batch", packet.batch + " · 生效批次", packet.batch), select(prefix + "-step", packet.batch + " · 生效工序", packet.route.map((entry) => [entry.id, entry.name])),
        area(prefix + "-pieces", packet.batch + " · 涉及件号（每行一个）", factoryEffectivePieceNumbers(state!, packet).join("\n")),
        select(prefix + "-disposition", packet.batch + " · 旧件处置", Object.entries(DISPOSITION_LABELS)));
    });
    form("跨批统一变更", fields, async (value) => {
      const scopes = candidates.flatMap((packet, index) => {
        const prefix = "scope-" + index;
        return value[prefix + "-include"] === "yes" ? [{ packageId: packet.id, batch: value[prefix + "-batch"]!, fromStep: value[prefix + "-step"]!, pieceNumbers: pieces(value[prefix + "-pieces"]!), disposition: value[prefix + "-disposition"] as FactoryDisposition }] : [];
      });
      await command({ type: "create-change-scheme", name: value.name!, reason: value.reason!, procurement: value.procurement!, cost: value.cost!, delivery: value.delivery!, scopes }); page = "changes"; render();
    }, "至少选择两个同一订单行的有效批次。首个纳入批次提供修订图纸基线；各批次分别计算差异和保留件号、工序、旧件处置。范围有重叠申请时后端会拒绝。", (element) => {
      candidates.forEach((_, index) => {
        const prefix = "scope-" + index, include = element.elements.namedItem(prefix + "-include") as HTMLSelectElement;
        const update = () => { for (const suffix of ["batch", "step", "pieces", "disposition"]) (element.elements.namedItem(prefix + "-" + suffix) as HTMLInputElement).disabled = include.value !== "yes"; };
        include.addEventListener("change", update); update();
      });
    });
  }
  function changeForm(packet: FactoryManufacturingPackage) {
    form("发起工程变更", [area("reason", "客户/采购/工艺变更原因"), text("batch", "生效批次", packet.batch),
      select("fromStep", "从哪道工序切换", packet.route.map((entry) => [entry.id, entry.name])), area("pieceNumbers", "涉及件号（留空表示整包）", "", false),
      select("disposition", "旧料/在制品处置", [["use-as-is", "评估后沿用"], ["rework", "返工"], ["scrap", "报废"], ["replace", "替换新件"], ["isolate", "隔离"]]),
      area("procurement", "采购影响"), area("cost", "成本影响"), area("delivery", "交期影响")
    ], async (value) => { await command({ type: "create-change", packageId: packet.id, reason: value.reason!, batch: value.batch!, fromStep: value.fromStep!, pieceNumbers: pieces(value.pieceNumbers!), disposition: value.disposition as "rework", procurement: value.procurement!, cost: value.cost!, delivery: value.delivery! }); });
  }
  function packageCard(packet: FactoryManufacturingPackage) {
    const controls = actions();
    if (packet.status === "review") controls.append(reviewButtons("review-package", packet.id, ["process", "procurement", "quality"]));
    if (has("production") && packet.status === "approved" && !packet.changeId) controls.append(cmd("下达制造包", { type: "issue-package", packageId: packet.id }));
    if (has("production") && ["review", "approved"].includes(packet.status) && !packet.changeId && !packet.supersedes) controls.append(guardedAction(button("撤销未下达批次", () => form("撤销制造批次 · " + packet.batch, [area("note", "撤销原因")], async (value) => { await command({ type: "cancel-package", packageId: packet.id, note: value.note! }); }, "仅释放未下达批次的数量。冻结图纸、BOM、件号和会签仍保留；原批次名与件号不复用。")), factoryPackageCancellationBlockers(state!, packet)));
    if (has("operator") && packet.status === "issued" && !packet.changeId) controls.append(cmd("车间接收版本", { type: "acknowledge-package", packageId: packet.id }));
    if (packet.status === "acknowledged" && has("operator", "quality")) controls.append(button("记录执行 / 终检", () => executionForm(packet)));
    if (has("operator") && ["acknowledged", "complete"].includes(packet.status) && state!.changes.some((entry) => entry.basePackageId === packet.id && entry.status === "acknowledged" && ["rework", "scrap", "isolate"].includes(entry.disposition))) controls.append(button("旧件处置报工", () => executionForm(packet, true)));
    if (packet.status === "acknowledged" && has("quality")) controls.append(guardedAction(cmd("质量完成制造包", { type: "complete-package", packageId: packet.id }), factoryPackageCompletionBlockers(state!, packet)));
    if (["issued", "acknowledged", "complete"].includes(packet.status) && has("sales", "designer", "process", "procurement", "production", "quality")) controls.append(button("发起工程变更", () => changeForm(packet)));
    if (packet.status === "acknowledged" && has("operator", "production", "quality")) controls.append(button("申请先行处置", () => deviationForm(packet)));
    return detail((state!.orders.find((entry) => entry.id === packet.orderId)?.number ?? "") + " · " + packet.batch + " · 制造包 V" + packet.version + " · " + STATUS[packet.status], controls,
      preview(state!.releases.find((entry) => entry.id === packet.releaseId)!.document),
      node("p", "图纸基线：" + (state!.releases.find((entry) => entry.id === packet.releaseId)?.drawingNumber ?? "") + " · 件号：" + packet.pieceNumbers.join("、")), bom(packet), reviews(packet.reviews),
      node("p", packet.cancellation ? "撤销：" + packet.cancellation.note + " / " + (people.find((person) => person.id === packet.cancellation!.actorId)?.name ?? packet.cancellation.actorId) + " / " + new Date(packet.cancellation.at).toLocaleString() + " / 订单 R" + packet.cancellation.orderRevision + "；原件号已作废，不复用" : "当前有效生产件：" + (factoryEffectivePieceNumbers(state!, packet).join("、") || "已全部转入变更制造包；旧记录保留")),
      table(["工序", "本版本工序范围", "合格 / 已记录", "未记录 / 不合格"], packet.route.map((step) => {
        const eligible = factoryStepPieceNumbers(state!, packet, step.id), passed = eligible.filter((piece) => ["pass", "recorded"].includes(factoryLastStepResult(state!, packet, piece, step.id) ?? ""));
        return [step.id + " · " + step.name, eligible.join("、") || "无（已移交或不适用）", passed.join("、") || "无", eligible.filter((piece) => !passed.includes(piece)).join("、") || "无"];
      })),
      table(["记录ID", "实际时间", "工序", "件号", "结果", "实际记录", "授权"], state!.executions.filter((entry) => entry.packageId === packet.id).map((entry) => [entry.id, new Date(entry.at).toLocaleString(), entry.stepId, entry.pieceNumbers.join("、"), entry.result, entry.actual, entry.deviationId ?? "正常执行"])));
  }
  function deviationForm(packet: FactoryManufacturingPackage, entry?: FactoryDeviation) {
    form(entry ? "修订先行申请并重新会签" : "先行申请", [
      area("reason", "先行原因", entry?.reason), area("actual", "拟先行/已发生的真实做法", entry?.actual),
      area("pieceNumbers", "明确生产件号（每行一个，不能留空）", entry?.scope?.pieceNumbers.join("\n") ?? packet.pieceNumbers.join("\n")),
      area("stepIds", "允许先行的工序编号（每行一个）", entry?.scope?.stepIds.join("\n") ?? packet.route[0]?.id),
      area("risk", "风险与影响", entry?.scope?.risk), area("control", "风险控制、隔离及质量措施", entry?.scope?.control)
    ], async (value) => {
      const fields = { reason: value.reason!, actual: value.actual!, pieceNumbers: pieces(value.pieceNumbers!), stepIds: pieces(value.stepIds!), risk: value.risk!, control: value.control! };
      await command(entry ? { type: "revise-deviation", deviationId: entry.id, ...fields } : { type: "create-deviation", packageId: packet.id, ...fields });
    }, "先行授权只对所列件号及工序有效，不得省略授权作为正常执行绕过。可选工序：" + packet.route.map((step) => step.id + "＝" + step.name).join("；"));
  }
  function differences(value: FactoryDifference[]) {
    const labels = { geometry: "几何尺寸", material: "材质选型", bom: "组成件 BOM", route: "工艺路线", other: "其他参数" };
    return table(["差异类别", "参数位置", "原版本", "新版本"], value.map((entry) => [labels[entry.category], entry.path, entry.before === undefined ? "未设置" : JSON.stringify(entry.before), entry.after === undefined ? "未设置" : JSON.stringify(entry.after)]));
  }
  function dispositionForm(change: FactoryChange) {
    const base = state!.packages.find((entry) => entry.id === change.basePackageId)!;
    const affected = change.effectivity.pieceNumbers.length ? change.effectivity.pieceNumbers : base.pieceNumbers;
    const options = (Object.entries(DISPOSITION_LABELS) as [FactoryDisposition, string][]).filter(([key]) => key === change.disposition || change.disposition === "isolate" && key !== "replace");
    const evidence = state!.executions.filter((entry) => [base.id, change.newPackageId].includes(entry.packageId));
    form("记录旧件实际处置", [area("pieceNumbers", "本次处置的原生产件号（每行一个）", affected.join("\n")),
      select("disposition", "实际处置", options, change.disposition), area("executionIds", "关联实际记录ID（返工/报废必须填写，每行一个）", "", false),
      area("replacementPairs", "替换对应：原件号=新件号（非替换留空）", "", false), area("note", "实际处置、旧件去向和证据说明")], async (value) => {
        const replacementPairs = pieces(value.replacementPairs!).map((line) => {
          const pair = line.split("=");
          if (pair.length !== 2 || !pair[0]?.trim() || !pair[1]?.trim()) throw new Error("替换对应请使用：原件号=新件号");
          return { oldPiece: pair[0].trim(), newPiece: pair[1].trim() };
        });
        await command({ type: "record-change-disposition", changeId: change.id, pieceNumbers: pieces(value.pieceNumbers!), disposition: value.disposition as FactoryDisposition, executionIds: pieces(value.executionIds!), replacementPairs, note: value.note! });
      }, "记录不会覆盖旧记录，须由生产与质量分别确认。隔离不能闭环。新件号：" + (state!.packages.find((entry) => entry.id === change.newPackageId)?.pieceNumbers.join("、") ?? "") + "；可关联实际记录：" + evidence.map((entry) => entry.id + "（" + entry.kind + " / " + entry.pieceNumbers.join("、") + "）").join("；"));
  }
  function dispositionTable(change: FactoryChange): HTMLElement {
    return table(["时间/记录人", "原件号", "实际处置", "实际记录 / 新旧件对应", "实况与旧件去向", "确认"], (change.dispositionRecords ?? []).map((record) => {
      const controls = actions(), final = record.reviews.some((entry) => entry.decision === "reject") || ["production", "quality"].every((role) => record.reviews.some((entry) => entry.role === role && entry.decision === "approve"));
      if (change.status === "acknowledged" && !final && record.actorId !== actor!.id) {
        for (const role of ["production", "quality"] as const) if (has(role) && !record.reviews.some((entry) => entry.role === role || entry.actorId === actor!.id)) controls.append(button(FACTORY_ROLE_LABELS[role] + "确认", () => form("确认逐件处置", [select("decision", "结论", [["approve", "通过"], ["reject", "退回（保留记录，另补新记录）"]]), area("note", "核对证据与处置说明")], async (value) => { await command({ type: "review-change-disposition", changeId: change.id, recordId: record.id, role, decision: value.decision as "approve" | "reject", note: value.note! }); })));
      }
      controls.append(reviews(record.reviews));
      return [new Date(record.at).toLocaleString() + " / " + (people.find((entry) => entry.id === record.actorId)?.name ?? record.actorId), record.pieceNumbers.join("、"), DISPOSITION_LABELS[record.disposition], record.executionIds.join("、") + "\n" + record.replacementPairs.map((pair) => pair.oldPiece + " → " + pair.newPiece).join("\n"), record.note, controls];
    }));
  }
  function renderChanges(content: HTMLElement) {
    for (const scheme of state!.changeSchemes ?? []) {
      const members = scheme.changeIds.map((id) => state!.changes.find((entry) => entry.id === id)!);
      const leader = members[0]!, design = state!.designs.find((entry) => entry.id === leader.designId)!, controls = actions();
      const status = factoryChangeSchemeStatus(state!, scheme.id), statusLabel = status === "partially-closed" ? "部分批次已闭环" : status === "partially-acknowledged" ? "部分批次已接收" : STATUS[status] ?? status;
      if (has("designer") && status === "designing") {
        if (factoryCanEditDesign(actor, state!, design)) controls.append(button("绘制共享修订图纸", () => openDesign(design)));
        if (leader.newReleaseId) controls.append(cmd("整组生成差异并提交会签", { type: "submit-change-scheme", schemeId: scheme.id }), cmd("重新修订共享图纸", { type: "revise-change-scheme", schemeId: scheme.id }));
      }
      if (status === "impact-review") controls.append(reviewButtons("review-change-scheme", scheme.id, DESIGN_REVIEW_ROLES));
      if (has("designer", "production") && ["impact-review", "approved"].includes(status)) controls.append(cmd("整组更新在制品影响并重审", { type: "refresh-change-scheme-impact", schemeId: scheme.id }));
      if (has("sales") && leader.newReleaseId && ["designing", "impact-review", "approved"].includes(status)) controls.append(cmd("客户确认共享新图纸", { type: "confirm-customer", orderId: leader.orderId, lineId: leader.lineId, releaseId: leader.newReleaseId }));
      if (scheme.createdBy === actor!.id && ["designing", "impact-review", "approved"].includes(status)) controls.append(button("撤回整个方案", () => form("撤回统一变更", [area("note", "撤回原因")], async (value) => { await command({ type: "withdraw-change-scheme", schemeId: scheme.id, note: value.note! }); }, "整组释放预留范围，保留图纸/会签/客户确认历史；先行授权和实况不删除。")));
      if (has("production") && status === "approved") controls.append(guardedAction(cmd("整组下达变更", { type: "issue-change-scheme", schemeId: scheme.id }), factoryChangeSchemeIssueBlockers(state!, scheme.id)));
      content.append(detail("统一方案 · " + scheme.name + " · " + statusLabel, controls,
        node("p", "共用修订图纸：" + design.drawingNumber + "；共用会签和客户确认。整组下达必须全部通过，逐批接收、处置和闭环见下方批次详情。"),
        table(["原批次", "原图版本 → 新图版本", "件号范围", "切换工序", "旧件处置", "差异数", "状态"], members.map((change) => {
          const base = state!.releases.find((entry) => entry.id === change.baseReleaseId)!, target = state!.releases.find((entry) => entry.id === change.newReleaseId);
          return [state!.packages.find((entry) => entry.id === change.basePackageId)!.batch, "V" + base.version + " → " + (target ? "V" + target.version : "待修订"), change.effectivity.pieceNumbers.join("、"), change.effectivity.fromStep, DISPOSITION_LABELS[change.disposition], String(change.differences.length), STATUS[change.status] ?? change.status];
        })), reviews(leader.reviews)));
    }
    for (const change of state!.changes) {
      const controls = actions(), design = state!.designs.find((entry) => entry.id === change.designId)!;
      if (!change.schemeId && has("designer") && change.status === "designing") {
        if (factoryCanEditDesign(actor, state!, design)) controls.append(button("绘制变更图纸", () => openDesign(design)));
        if (change.newReleaseId) controls.append(cmd("生成差异并提交影响会签", { type: "submit-change", changeId: change.id }), cmd("重新修订变更图纸", { type: "revise-change", changeId: change.id }));
      }
      if (!change.schemeId && change.status === "impact-review") controls.append(reviewButtons("review-change", change.id, DESIGN_REVIEW_ROLES));
      if (!change.schemeId && has("designer", "production") && ["impact-review", "approved"].includes(change.status)) controls.append(cmd("更新在制品影响并重新会签", { type: "refresh-change-impact", changeId: change.id }));
      if (!change.schemeId && has("sales") && change.newReleaseId && ["designing", "impact-review", "approved"].includes(change.status)) controls.append(cmd("客户确认新图纸", { type: "confirm-customer", orderId: change.orderId, lineId: change.lineId, releaseId: change.newReleaseId }));
      if (!change.schemeId && change.createdBy === actor!.id && ["designing", "impact-review", "approved"].includes(change.status)) controls.append(button("撤回未下达变更", () => form("撤回工程变更", [area("note", "撤回原因")], async (value) => { await command({ type: "withdraw-change", changeId: change.id, note: value.note! }); }, "保留图纸和审核历史；释放件号范围。已下达变更不能撤回。")));
      if (!change.schemeId && has("production") && change.status === "approved") controls.append(guardedAction(cmd("下达变更执行", { type: "issue-change", changeId: change.id }), factoryChangeIssueBlockers(state!, change)));
      if (has("operator") && change.status === "issued") controls.append(cmd("车间接收变更", { type: "acknowledge-change", changeId: change.id }));
      if (has("operator", "production") && change.status === "acknowledged") controls.append(button("记录旧件实际处置", () => dispositionForm(change)));
      if (has("quality") && change.status === "acknowledged") controls.append(guardedAction(button("质量验证与闭环", () => form("工程变更闭环", [area("note", "旧件处置与新版本质量验证结论")], async (value) => { await command({ type: "close-change", changeId: change.id, note: value.note! }); })), factoryChangeClosureBlockers(state!, change)));
      content.append(detail((change.schemeId ? "统一方案批次 · " : "") + change.effectivity.batch + " · " + change.reason + " · " + (change.status === "cancelled" ? "已撤回" : STATUS[change.status]), controls, node("p", "生效：" + change.effectivity.batch + " / " + change.effectivity.fromStep + " / " + (change.effectivity.pieceNumbers.join("、") || "整包") + "；旧件处置：" + change.disposition),
        node("p", "采购：" + change.impact.procurement + "；成本：" + change.impact.cost + "；交期：" + change.impact.delivery + "；评估实际记录：" + change.impact.executionIds.length + "条"),
        node("p", change.customerConfirmation ? "客户确认：图纸发布ID " + change.customerConfirmation.releaseId + " / " + new Date(change.customerConfirmation.at).toLocaleString() : "客户尚未确认本变更版本"),
        node("p", change.cancellation ? "撤回：" + change.cancellation.note + " / " + new Date(change.cancellation.at).toLocaleString() : "只影响上述批次与件号，不改写订单原基线和其他批次"),
        differences(change.differences), reviews(change.reviews), dispositionTable(change), node("p", change.closure?.note ?? "图纸发布、变更批准、车间接收和逐件处置确认是独立环节。")));
    }
  }
  function renderDeviations(content: HTMLElement) {
    for (const entry of state!.deviations) {
      const controls = actions();
      if (entry.createdBy === actor!.id && ["requested", "returned"].includes(entry.status)) {
        controls.append(button("修订申请", () => deviationForm(state!.packages.find((packet) => packet.id === entry.packageId)!, entry)),
          button("撤回未执行申请", () => form("撤回先行申请", [area("note", "撤回原因")], async (value) => { await command({ type: "withdraw-deviation", deviationId: entry.id, note: value.note! }); })));
      }
      if (entry.status === "requested") controls.append(reviewButtons("review-deviation", entry.id, ["production", "quality"]));
      if (has("designer") && entry.status === "authorized") controls.append(button("关联后补图变更", () => form("后补设计对账", [select("changeId", "同基础制造包的变更", state!.changes.filter((change) => change.basePackageId === entry.packageId && change.status !== "cancelled").map((change) => [change.id, change.reason]))], async (value) => { await command({ type: "link-deviation-change", deviationId: entry.id, changeId: value.changeId! }); })));
      if (has("quality") && entry.status === "reconciling") controls.append(guardedAction(button("关闭先行偏差", () => form("实际/计划/设计对账", [area("resolution", "对账与处置结论")], async (value) => { await command({ type: "close-deviation", deviationId: entry.id, resolution: value.resolution! }); })), factoryDeviationClosureBlockers(state!, entry)));
      content.append(detail(entry.reason + " · " + (entry.status === "cancelled" ? "已撤回" : STATUS[entry.status]), controls, node("p", "申请真实时间：" + new Date(entry.createdAt).toLocaleString() + "。实际做法：" + entry.actual + "。实际执行记录：" + entry.executionIds.length + " 条"),
        node("p", entry.scope ? "授权范围：件号 " + entry.scope.pieceNumbers.join("、") + "；工序 " + entry.scope.stepIds.join("、") + "；风险：" + entry.scope.risk + "；控制措施：" + entry.scope.control : "旧申请缺少明确范围，不能执行；请先修订并重新会签。"),
        reviews(entry.reviews), node("p", entry.resolution ?? "授权后记录实际执行，再关联补图变更和质量对账。")));
    }
  }
  async function renderUsers(content: HTMLElement) {
    const accounts = (await api.users()).users; if (disposed || page !== "users" || !content.isConnected) return;
    const accountForm = (account?: FactoryAccount) => form("维护工厂账号", [text("username", "登录账号", account?.username), text("name", "姓名", account?.name),
      text("roles", "岗位（多个用英文逗号分隔）", account?.roles.join(",") ?? "designer"), { key: "password", label: "新密码（至少10位，修改可留空）", type: "password", required: !account },
      select("enabled", "状态", [["true", "启用"], ["false", "停用"]], account?.enabled === false ? "false" : "true")], async (value) => {
        await api.saveUser({ id: account?.id, username: value.username!, name: value.name!, roles: value.roles!.split(",").map((role) => role.trim()) as FactoryRole[], password: value.password || undefined, enabled: value.enabled === "true" }); render(); message("账号已保存；被修改账号的旧登录已失效。");
      }, FACTORY_ROLES.map((role) => role + "：" + FACTORY_ROLE_LABELS[role]).join("；"));
    content.append(button("新增账号", () => accountForm(), "primary"), table(["账号", "姓名", "岗位", "状态", "操作"], accounts.map((entry) => [entry.username, entry.name, entry.roles.map((role) => FACTORY_ROLE_LABELS[role]).join("、"), entry.enabled ? "启用" : "停用", button("编辑", () => accountForm(entry))])));
  }
  function readOnlyDialog(title: string, ...parts: HTMLElement[]) {
    if (disposed || !actor) return;
    const dialog = node("dialog", "", "factory-form-dialog factory-history-dialog");
    const close = () => { dialog.close(); dialog.remove(); dialogs.delete(dialog); };
    dialog.append(actions(node("h2", title), button("关闭", close)),
      node("p", "只读历史：不会加载到编辑画布，不覆盖当前设计、订单或制造包。", "factory-prototype-note"), ...parts);
    dialog.addEventListener("cancel", () => { dialogs.delete(dialog); dialog.remove(); });
    document.body.append(dialog); dialogs.add(dialog); dialog.showModal();
  }
  function scopeLabel(snapshot: FactoryWorkspace, releaseId: string) {
    const entry = snapshot.releases.find((release) => release.id === releaseId)!;
    const scope = factoryReleaseScope(snapshot, entry);
    return scope.kind === "catalog" ? "标准目录" : "订单专用 · " + (snapshot.orders.find((order) => order.id === scope.orderId)?.number ?? scope.orderId) + " / " + (snapshot.orders.flatMap((order) => order.lines).find((line) => line.id === scope.lineId)?.mark ?? scope.lineId);
  }
  async function showComparison(snapshot: FactoryWorkspace, releaseId: string, against?: string) {
    const result = await api.releaseComparison(releaseId, against);
    if (disposed || !actor) return;
    const columns = node("div", "", "factory-history-comparison");
    for (const [heading, release] of [["对比基线", result.before], ["目标发布版", result.after]] as const) {
      const column = node("section"); column.append(node("h3", heading));
      if (release) column.append(node("p", release.drawingNumber + " V" + release.version + " · " + scopeLabel(snapshot, release.id)),
        node("p", "发布： " + new Date(release.publishedAt).toLocaleString() + " · 发布人：" + (people.find((person) => person.id === release.publishedBy)?.name ?? release.publishedBy)),
        preview(release.document), bom({ bom: release.calculation.lines, route: release.calculation.route }), reviews(release.reviews));
      else column.append(node("p", "首次发布，没有父版本；这不表示与其他发布版无差异。"));
      columns.append(column);
    }
    readOnlyDialog("发布图纸版本对比", columns,
      node("p", result.before ? "差异 " + result.differences.length + " 项，包含冻结图纸、BOM 和工艺路线参数。" : "请选择同一产品的另一发布版，可进行指定版本对比。"),
      differences(result.differences));
  }
  function downloadSnapshot(snapshot: FactoryWorkspace) {
    const url = URL.createObjectURL(new Blob([JSON.stringify(snapshot, null, 2)], { type: "application/json" })), link = node("a");
    link.href = url; link.download = "factory-history-R" + snapshot.revision + ".json"; link.click(); URL.revokeObjectURL(url);
  }
  function showHistorySnapshot(snapshot: FactoryWorkspace) {
    const history = describeFactoryHistory(snapshot);
    const names = (id: string) => people.find((person) => person.id === id)?.name ?? id;
    const parts: HTMLElement[] = [
      node("p", "后端提交 R" + history.revision + " · " + new Date(history.savedAt).toLocaleString()),
      button("下载此修订 JSON（只读导出）", () => downloadSnapshot(snapshot)),
      table(["统一变更方案", "方案状态", "成员变更ID", "发起人 / 时间"], history.changeSchemes.map((entry) => [entry.name, STATUS[entry.status] ?? (entry.status === "partially-closed" ? "部分批次已闭环" : entry.status === "partially-acknowledged" ? "部分批次已接收" : entry.status), entry.changeIds.join("、"), names(entry.createdBy) + " / " + new Date(entry.createdAt).toLocaleString()])),
      node("h3", "订单与设计需求"),
      table(["订单 / 行", "类别 / 数量", "尺寸 mm", "型材 / 玻璃 / 五金", "颜色 / 交期 / 备注", "订单基线", "客户确认版", "状态"], history.orders.map((entry) => [entry.order + " / " + entry.mark, (entry.kind === "standard" ? "标准" : "定制") + " / " + entry.quantity, entry.requirement.widthMm + "×" + entry.requirement.heightMm, [entry.requirement.material, entry.requirement.glass, entry.requirement.hardware].join(" / "), [entry.requirement.finish, entry.requirement.dueDate, entry.requirement.note].join("\n"), entry.release, entry.confirmed, STATUS[entry.status] ?? entry.status])),
      table(["订单 / 行", "数量修订", "原数量 → 新数量", "人员 / 时间", "调整说明"], history.orders.flatMap((entry) => entry.quantityRevisions.map((record) => [entry.order + " / " + entry.mark, "R" + record.fromRevision + " → R" + record.toRevision, record.fromQuantity + " → " + record.toQuantity, names(record.actorId) + " / " + new Date(record.at).toLocaleString(), record.note]))),
      node("h3", "研发任务与当时设计"),
      ...history.orders.flatMap((entry) => entry.requirementRevisions.map((record) => detail(entry.order + " / " + entry.mark + " · 需求 V" + record.fromRequirementRevision + " → V" + record.toRequirementRevision + " · 订单 R" + record.toRevision,
        node("p", names(record.actorId) + " / " + new Date(record.at).toLocaleString() + " · " + record.note + "；撤销包：" + record.cancelledPackageIds.join("、")), differences(record.differences)))),
      table(["订单 / 行", "任务", "负责人", "图纸 / 修订", "状态"], snapshot.engineeringRequests.map((entry) => [snapshot.orders.find((order) => order.id === entry.orderId)!.number + " / " + snapshot.orders.flatMap((order) => order.lines).find((line) => line.id === entry.lineId)!.mark, entry.id, entry.assigneeId ? names(entry.assigneeId) : "未分派", snapshot.designs.find((design) => design.id === entry.designId)!.drawingNumber, STATUS[entry.status] ?? entry.status])),
      ...snapshot.designs.map((design) => detail(design.drawingNumber + " · 草稿修订 " + design.version + " · " + (STATUS[design.status] ?? design.status), preview(design.document), reviews(design.reviews), ...(design.calculation ? [bom({ bom: design.calculation.lines, route: design.calculation.route })] : []))),
      node("h3", "当时已发布图纸基线"),
      ...snapshot.releases.map((release) => detail(release.productCode + " · " + release.drawingNumber + " V" + release.version + " · " + scopeLabel(snapshot, release.id),
        node("p", "发布人：" + names(release.publishedBy) + " / " + release.publishedBy + "；发布时间：" + new Date(release.publishedAt).toLocaleString() + "；父发布版：" + (history.releases.find((entry) => entry.id === release.supersedes)?.label ?? "首次发布")),
        preview(release.document), bom({ bom: release.calculation.lines, route: release.calculation.route }), reviews(release.reviews))),
      node("h3", "制造包与实际生产版本"),
      table(["订单 / 批次", "制造包版本", "图纸基线", "状态 / 数量", "原始件号", "此修订时有效件号", "替代来源 / 撤销"], history.packages.map((entry) => [entry.order + " / " + entry.batch, "V" + entry.version, entry.release, (STATUS[entry.status] ?? entry.status) + " / " + entry.quantity, entry.originalPieces.join("、"), entry.effectivePieces.join("、") || (entry.cancellation ? "已撤销，原件号作废" : "已全部移交变更"), entry.cancellation ? entry.cancellation.note + " / " + names(entry.cancellation.actorId) + " / " + new Date(entry.cancellation.at).toLocaleString() + " / 订单 R" + entry.cancellation.orderRevision : entry.supersedes ?? "原始制造包"])),
      ...snapshot.packages.map((packet) => detail(packet.batch + " · 制造包 V" + packet.version + " 冻结图纸 / BOM", preview(snapshot.releases.find((release) => release.id === packet.releaseId)!.document), bom(packet), reviews(packet.reviews),
        table(["实况时间", "件号", "工序 / 结果", "实际做法", "记录人 / 先行授权"], snapshot.executions.filter((entry) => entry.packageId === packet.id).map((entry) => [new Date(entry.at).toLocaleString(), entry.pieceNumbers.join("、"), entry.stepId + " / " + entry.result, entry.actual, names(entry.actorId) + " / " + (entry.deviationId ?? "正常执行")])))),
      node("h3", "工程变更 / 先行后补"),
      ...snapshot.changes.map((entry) => detail(entry.reason + " · " + (STATUS[entry.status] ?? entry.status),
        node("p", "图纸：" + history.releases.find((release) => release.id === entry.baseReleaseId)!.label + " → " + (history.releases.find((release) => release.id === entry.newReleaseId)?.label ?? "未发布新版本")),
        node("p", "批次：" + entry.effectivity.batch + "；生效工序：" + entry.effectivity.fromStep + "；件号：" + (entry.effectivity.pieceNumbers.join("、") || "整包") + "；处置：" + DISPOSITION_LABELS[entry.disposition]), differences(entry.differences), reviews(entry.reviews),
        ...(entry.dispositionRecords ?? []).map((record) => detail("逐件处置 · " + new Date(record.at).toLocaleString() + " · " + names(record.actorId), node("p", "原件：" + record.pieceNumbers.join("、") + "；处置：" + DISPOSITION_LABELS[record.disposition] + "；实况证据：" + record.executionIds.join("、") + "；替换关系：" + record.replacementPairs.map((pair) => pair.oldPiece + " → " + pair.newPiece).join("、")), node("p", record.note), reviews(record.reviews))),
        node("p", entry.closure?.note ?? entry.cancellation?.note ?? "未闭环"))),
      ...snapshot.deviations.map((entry) => detail(entry.reason + " · " + (STATUS[entry.status] ?? entry.status), node("p", entry.actual), node("p", "件号：" + (entry.scope?.pieceNumbers.join("、") ?? "旧记录无明确范围") + "；工序：" + (entry.scope?.stepIds.join("、") ?? "未授权") + "；实际记录：" + entry.executionIds.join("、")), reviews(entry.reviews), node("p", entry.resolution ?? "未对账闭环"))),
      node("h3", "提交记录"),
      table(["修订 / 时间", "记录人", "动作", "对象", "说明"], history.events.map((entry) => ["R" + entry.workspaceRevision + " / " + new Date(entry.at).toLocaleString(), names(entry.actorId), entry.action, entry.targetId, entry.note])),
      detail("原始 JSON", node("pre", JSON.stringify(snapshot, null, 2)))
    ];
    readOnlyDialog("R" + snapshot.revision + " 历史工作区", ...parts);
  }
  async function renderHistory(content: HTMLElement) {
    const snapshot = structuredClone(state!);
    const { revisions } = await api.versions(); if (disposed || page !== "history" || !content.isConnected) return;
    content.append(node("p", "发布版本、设计草稿修订和后端提交修订分别保留。默认对比真实父发布版，不按版本号猜测；订单专用分支不会替代标准目录。"),
      table(["发布图纸", "用途", "时间", "只读对比"], snapshot.releases.map((entry) => {
        const candidates = factoryComparisonCandidates(snapshot, entry.id), chooser = node("select");
        chooser.setAttribute("aria-label", entry.drawingNumber + " V" + entry.version + " 对比基线");
        for (const baseline of candidates) { const option = node("option", baseline.drawingNumber + " V" + baseline.version + " · " + scopeLabel(snapshot, baseline.id)); option.value = baseline.id; chooser.append(option); }
        if (entry.supersedes) chooser.value = entry.supersedes;
        const controls = actions(button(entry.supersedes ? "与父发布版对比" : "查看首次发布", () => safely(() => showComparison(snapshot, entry.id))));
        if (candidates.length) controls.append(chooser, button("与所选发布版对比", () => safely(() => showComparison(snapshot, entry.id, chooser.value))));
        return [entry.productCode + " · " + entry.drawingNumber + " V" + entry.version, scopeLabel(snapshot, entry.id), new Date(entry.publishedAt).toLocaleString(), controls];
      })),
      node("h2", "后端提交历史（只读）"),
      table(["提交修订", "操作"], revisions.map((revision) => ["R" + revision, button("查看图纸、订单与生产记录", () => safely(async () => { const value = await api.version(revision); if (!disposed && actor) showHistorySnapshot(value); }))])));
  }
  function renderPage(content: HTMLElement) {
    if (page === "orders") renderOrders(content);
    if (page === "tasks") renderTasks(content);
    if (page === "designs") renderDesigns(content);
    if (page === "packages") for (const packet of state!.packages) content.append(packageCard(packet));
    if (page === "changes") renderChanges(content);
    if (page === "deviations") renderDeviations(content);
    if (page === "reviews") {
      for (const design of state!.designs.filter((entry) => factoryReviewRoles(actor, state!, "review-design", entry.id, true).length > 0)) content.append(designCard(design));
      for (const packet of state!.packages.filter((entry) => factoryReviewRoles(actor, state!, "review-package", entry.id, true).length > 0)) content.append(packageCard(packet));
      if (state!.changes.some((entry) => factoryReviewRoles(actor, state!, "review-change", entry.id, true).length > 0)) content.append(button("工程变更会签", () => { page = "changes"; render(); }));
      if ((state!.changeSchemes ?? []).some((entry) => factoryReviewRoles(actor, state!, "review-change-scheme", entry.id, true).length > 0)) content.append(button("跨批统一变更会签", () => { page = "changes"; render(); }));
      if (state!.deviations.some((entry) => factoryReviewRoles(actor, state!, "review-deviation", entry.id, true).length > 0)) content.append(button("先行授权待办", () => { page = "deviations"; render(); }));
    }
    if (page === "users") void renderUsers(content).catch(fail);
    if (page === "history") void renderHistory(content).catch(fail);
    if (content.children.length === 3 && !["users", "history"].includes(page)) content.append(node("p", page === "reviews" ? "当前岗位暂无待会签项。自己提交的方案不能自审；已通过事项可在对应业务页查看，在仍处于会签阶段时调整结论。" : "暂无记录。按岗位创建订单或保存设计后开始流程演练。"));
  }
  function render() {
    if (disposed) return; if (!actor || !state) { renderLogin(); return; }
    renderBar(); root.hidden = !drawing || !has("designer"); overlay.hidden = drawing && has("designer"); if (overlay.hidden) return;
    const nav = node("nav", "", "factory-navigation"), content = node("main", "", "factory-content");
    for (const item of factoryPagesFor(actor)) nav.append(button(PAGE_LABELS[item], () => { page = item; render(); }, item === page ? "active" : ""));
    content.append(node("h1", PAGE_LABELS[page]), node("p", "后端 R" + state.revision + " · 原型演练：参考BOM/工艺待企业目录确认，不是实际投产凭据。", "factory-prototype-note"));
    content.append(actions(button("刷新后端数据", () => safely(async () => { state = await api.workspace(); people = (await api.people()).people; render(); message("已刷新。画布未保存内容保持不变。"); })),
      pending ? button("重试上一操作", () => safely(async () => { const request = pending!; const result = await api.mutate(request); acceptMutation(request, result); message(result.replayed ? "上次操作已成功，没有重复执行。" : "操作已确认。"); })) : undefined,
      pending ? button("核对后解除待确认操作", () => { if (window.confirm("确认已核对后端记录？解除不会自动重发操作。")) { pending = undefined; render(); } }) : undefined));
    overlay.replaceChildren(nav, content);
    renderPage(content);
  }
  const unsubscribe = session.subscribe((value) => { dirty = baseline ? JSON.stringify(value) !== baseline : value.windows.length > 0; if (actor && drawing) renderBar(); });
  renderLogin(); void (async () => { try { const result = await api.session(); const workspace = await api.workspace(); const directory = await api.people(); if (disposed) return; actor = result.actor; state = workspace; people = directory.people; page = factoryPagesFor(actor)[0]!; render(); } catch (error) { if (!(error instanceof FactoryClientError && error.status === 401)) fail(error); } })();
  return () => { disposed = true; unsubscribe(); for (const dialog of dialogs) dialog.remove(); bar.remove(); overlay.remove(); notice.remove(); document.body.classList.remove("factory-mode"); root.hidden = false; root.inert = false; };
}
