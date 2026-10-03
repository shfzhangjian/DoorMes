import {
  nextAvailableWindowSequence,
  planConnectedWindowCreation,
  listEngineeringJointCatalogSelections,
  listNeutralSlidingWindowOptions,
  listZcsungSimulationWindowOptions,
  NEUTRAL_ORDINARY_SLIDING_TEMPLATE_ID,
  planNeutralSlidingWindowCreation,
  planZcsungSimulationWindowCreation,
  requireEngineeringJointCatalogSelection,
  resolveSelectedOwningWindowId,
  type FabricationConnectionDirection,
  type DesignCanvasViewStore,
  type DesignSelectionStore,
  type DesignSession
} from "@doormes/application";
import type { WindowUnit } from "@doormes/contracts";
import { desktopCreateWindowCommand } from "@doormes/interaction-desktop";
import type {
  OpeningPreviewStore,
  WindowVisualPreviewStore
} from "@doormes/interaction-core";
import {
  createHistoryControls,
  mountWindowAppearanceEditor,
  mountDesignObjectInspector,
  mountDesignObjectTree,
  mountSharedDesignWorkspace,
  type AppearanceEditorAssetServices
} from "@doormes/ui-components";
import {
  instantiateReusableWindowTemplate,
  readReusableWindowTemplates,
  writeReusableWindowTemplates,
  type ReusableWindowTemplate
} from "./reusable-window-template.js";
import { mountWallPlanWorkspace } from "./wall-plan.js";

/**
 * Mounts the PC-specific workspace around the shared application session.
 *
 * The shell owns the three-column layout, text-field parsing and mouse/keyboard
 * presentation. It delegates command construction to the desktop adapter and
 * SVG output to the shared component, so no model, geometry or BOM logic is
 * duplicated here.
 *
 * @param root Browser element that receives the desktop shell.
 * @param session Session shared with any replacement mobile shell.
 * @param selection Selection shared by 2D, 3D, object tree and replacement shells.
 * @param canvasView Shared 2D navigation and engineering-overlay state.
 * @param openingPreview Shared non-persistent panel preview state.
 * @param visualPreview Shared non-persistent material/model draft state.
 * @param assetServices Shared local or authenticated upload gateway from the app root.
 * @returns A disposer for subscriptions and shell-owned DOM.
 * @example `mountDesktopLayoutShell(root, session)` on a wide viewport.
 * @since 0.1.0
 * @modified 2026-10-01 - Reorganized the PC shell as a factory design workbench.
 */
export function mountDesktopLayoutShell(
  root: HTMLElement,
  session: DesignSession,
  selection: DesignSelectionStore,
  canvasView: DesignCanvasViewStore,
  openingPreview: OpeningPreviewStore,
  visualPreview: WindowVisualPreviewStore,
  assetServices: AppearanceEditorAssetServices = {},
  templateStorage?: Pick<Storage, "getItem" | "setItem">
): () => void {
  const shell = document.createElement("div");
  shell.className = "shell desktop-shell";
  shell.innerHTML = `
    <header class="shell__header">
      <div class="desktop-shell__identity">
        <span class="desktop-shell__brand-mark" aria-hidden="true">DM</span>
        <div class="desktop-shell__brand-copy">
          <h1 class="shell__title">DoorMes</h1>
          <span>WINDOW DESIGN STUDIO</span>
        </div>
      </div>
      <div class="desktop-shell__workspace-title">
        <span>工厂设计</span>
        <strong>门窗产品工作台</strong>
      </div>
      <output class="desktop-shell__document-status" data-document-status aria-live="polite"></output>
    </header>
    <div class="desktop-shell__workflow" aria-label="门窗图纸设计流程">
      <span class="desktop-shell__workflow-title">图纸设计</span>
      <span class="desktop-shell__workflow-step">1 选择图型</span>
      <span class="desktop-shell__workflow-step">2 绘制构造</span>
      <span class="desktop-shell__workflow-step">3 设置材料</span>
      <span class="desktop-shell__workflow-step">4 预览出图</span>
      <span class="desktop-shell__workflow-hint">图纸、2D、3D 共用同一设计</span>
    </div>
    <div class="desktop-shell__workspace">
      <aside class="shell-panel desktop-shell__left-panel">
        <div class="desktop-shell__panel-heading">
          <span>图形与构造</span>
          <h2>设计资源库</h2>
        </div>
        <nav class="desktop-shell__library-tabs" data-library-tabs role="tablist" aria-label="设计资源">
          <button type="button" role="tab" data-library-tab="windows" aria-selected="true">整窗模板</button>
          <button type="button" role="tab" data-library-tab="components" aria-selected="false">构造工具</button>
          <button type="button" role="tab" data-library-tab="mine" aria-selected="false">我的模板</button>
        </nav>
        <section class="desktop-shell__library-pane" data-library-pane="windows" aria-label="预制整窗">
          <p class="desktop-shell__library-caption">选择图型后可修改尺寸，再放入画布。</p>
          <div class="desktop-shell__template-gallery" data-template-gallery></div>
        </section>
        <section class="desktop-shell__library-pane" data-library-pane="components" aria-label="构造与分格工具" hidden>
          <p class="desktop-shell__library-caption">先选中窗或分格，再选构造方式；构件仍可在右侧设置型号。</p>
          <div data-component-tools></div>
          <p class="desktop-shell__library-footnote">窗框、扇、玻璃与五金由当前图型及构件属性驱动，不能脱离宿主窗直接放置。</p>
        </section>
        <section class="desktop-shell__library-pane" data-library-pane="mine" aria-label="我的复用模板" hidden>
          <p class="desktop-shell__library-caption">将当前选中窗保存为本机模板，后续可重新放入设计。</p>
          <div class="desktop-shell__save-template">
            <label>模板名称<input type="text" data-template-name maxlength="48" placeholder="例如：客厅三分格窗" /></label>
            <button type="button" data-save-template>保存选中窗</button>
          </div>
          <div class="desktop-shell__template-gallery" data-saved-template-gallery></div>
          <output class="desktop-shell__template-status" data-template-status aria-live="polite"></output>
        </section>
        <div class="desktop-shell__create-heading"><span>放入设计</span><strong data-selected-template-name>手工矩形窗</strong></div>
        <form class="shell-form" data-create-window-form>
          <input name="productTemplate" data-product-template type="hidden" value="" />
          <small class="desktop-shell__template-note" data-product-template-note>手工创建通用矩形窗。</small>
          <details class="desktop-shell__form-group" open>
            <summary>基本尺寸</summary>
            <label>总宽（mm）<input name="width" type="number" value="1200" min="100" /></label>
            <label>总高（mm）<input name="height" type="number" value="1500" min="100" /></label>
          </details>
          <details class="desktop-shell__form-group">
            <summary>组合与连接</summary>
            <label>加入方式
            <select name="creationMode" data-connection-direction>
              <option value="independent">独立窗</option>
              <option value="right">连接到当前窗右侧</option>
              <option value="left">连接到当前窗左侧</option>
              <option value="bottom">连接到当前窗下方</option>
              <option value="top">连接到当前窗上方</option>
              <option value="corner">从当前窗右侧转角连接</option>
            </select>
            </label>
            <label>连接件型号<select name="jointCatalog" data-connection-catalog disabled></select></label>
            <small data-connection-catalog-note>连接时选择参考目录型号。</small>
            <label data-corner-field hidden>转角内夹角（°）
              <input name="cornerAngle" data-corner-angle type="number" value="90" min="60" max="150" step="0.1" disabled />
            </label>
            <label data-corner-field hidden>俯视转向
              <select name="cornerTurn" data-corner-turn disabled>
                <option value="clockwise">顺时针</option>
                <option value="counterclockwise">逆时针</option>
              </select>
            </label>
          </details>
          <button class="shell-button" type="submit">放入画布</button>
          <output class="shell-form__status" data-creation-status>首窗默认独立创建；后续可连接到当前选中窗。</output>
        </form>
      </aside>
      <main class="desktop-shell__drawing-area">
        <div class="desktop-shell__drawing-header">
          <strong>设计画布</strong>
          <button type="button" data-drawing-mode="product" aria-pressed="true">门窗产品</button>
          <button type="button" data-drawing-mode="wall" aria-pressed="false">墙体平面</button>
          <span data-drawing-hint>主视绘制 / 俯视校核 / 三维仿真</span>
        </div>
        <div data-design-workspace></div>
        <div data-wall-plan-workspace hidden></div>
      </main>
      <aside class="shell-panel desktop-shell__right-panel">
        <div class="desktop-shell__inspector-heading"><span>设计参数</span><strong>构件与材料</strong></div>
        <section class="desktop-shell__panel-section desktop-shell__panel-section--tree">
          <div class="desktop-shell__panel-heading desktop-shell__panel-heading--compact">
            <span>02 · 结构导航</span>
            <h2>构件树</h2>
          </div>
          <p>窗体、组合连接及其构件；选择后可查看和编辑。</p>
          <div data-object-tree></div>
        </section>
        <section class="desktop-shell__editor-panel" data-object-inspector-panel tabindex="-1">
          <nav class="desktop-shell__editor-tabs" data-editor-tabs role="tablist" aria-label="设计属性编辑">
            <button id="desktop-editor-tab-properties" type="button" role="tab" data-editor-tab="properties" aria-selected="true" aria-controls="desktop-editor-pane-properties" tabindex="0">构件属性</button>
            <button id="desktop-editor-tab-appearance" type="button" role="tab" data-editor-tab="appearance" aria-selected="false" aria-controls="desktop-editor-pane-appearance" tabindex="-1">材质与型号</button>
          </nav>
          <div class="desktop-shell__editor-panes">
            <div class="desktop-shell__editor-pane" id="desktop-editor-pane-properties" role="tabpanel" data-editor-pane="properties" aria-labelledby="desktop-editor-tab-properties">
              <h2>构件属性</h2>
              <div data-object-inspector></div>
            </div>
            <div class="desktop-shell__editor-pane" id="desktop-editor-pane-appearance" role="tabpanel" data-editor-pane="appearance" aria-labelledby="desktop-editor-tab-appearance" hidden>
              <h2>材质与型号</h2>
              <div data-appearance-editor></div>
            </div>
          </div>
          <div class="desktop-shell__history" data-history></div>
        </section>
      </aside>
    </div>`;
  root.append(shell);

  const form = shell.querySelector<HTMLFormElement>("[data-create-window-form]");
  const workspace = shell.querySelector<HTMLElement>("[data-design-workspace]");
  const wallWorkspace = shell.querySelector<HTMLElement>("[data-wall-plan-workspace]");
  const drawingHeader = shell.querySelector<HTMLElement>(".desktop-shell__drawing-header");
  const objectTree = shell.querySelector<HTMLElement>("[data-object-tree]");
  const history = shell.querySelector<HTMLElement>("[data-history]");
  const objectInspector = shell.querySelector<HTMLElement>("[data-object-inspector]");
  const objectInspectorPanel = shell.querySelector<HTMLElement>("[data-object-inspector-panel]");
  const appearanceEditor = shell.querySelector<HTMLElement>("[data-appearance-editor]");
  const documentStatus = shell.querySelector<HTMLOutputElement>("[data-document-status]");
  const editorTabList = shell.querySelector<HTMLElement>("[data-editor-tabs]");
  const libraryTabs = shell.querySelector<HTMLElement>("[data-library-tabs]");
  const templateGallery = shell.querySelector<HTMLElement>("[data-template-gallery]");
  const savedTemplateGallery = shell.querySelector<HTMLElement>("[data-saved-template-gallery]");
  const componentTools = shell.querySelector<HTMLElement>("[data-component-tools]");
  const templateName = shell.querySelector<HTMLInputElement>("[data-template-name]");
  const saveTemplateButton = shell.querySelector<HTMLButtonElement>("[data-save-template]");
  const templateStatus = shell.querySelector<HTMLOutputElement>("[data-template-status]");
  const selectedTemplateName = shell.querySelector<HTMLElement>("[data-selected-template-name]");
  const directionControl = form?.querySelector<HTMLSelectElement>("[data-connection-direction]");
  const productTemplateControl = form?.querySelector<HTMLInputElement>("[data-product-template]");
  const productTemplateNote = form?.querySelector<HTMLElement>("[data-product-template-note]");
  const widthControl = form?.elements.namedItem("width");
  const heightControl = form?.elements.namedItem("height");
  const jointCatalogControl = form?.querySelector<HTMLSelectElement>("[data-connection-catalog]");
  const jointCatalogNote = form?.querySelector<HTMLElement>("[data-connection-catalog-note]");
  const cornerFields = form?.querySelectorAll<HTMLElement>("[data-corner-field]");
  const cornerAngleControl = form?.querySelector<HTMLInputElement>("[data-corner-angle]");
  const cornerTurnControl = form?.querySelector<HTMLSelectElement>("[data-corner-turn]");
  const creationStatus = form?.querySelector<HTMLOutputElement>("[data-creation-status]");
  if (
    !form ||
    !workspace ||
    !wallWorkspace ||
    !drawingHeader ||
    !objectTree ||
    !objectInspector ||
    !objectInspectorPanel ||
    !appearanceEditor ||
    !documentStatus ||
    !editorTabList ||
    !libraryTabs ||
    !templateGallery ||
    !savedTemplateGallery ||
    !componentTools ||
    !templateName ||
    !saveTemplateButton ||
    !templateStatus ||
    !selectedTemplateName ||
    !directionControl ||
    !productTemplateControl ||
    !productTemplateNote ||
    !(widthControl instanceof HTMLInputElement) ||
    !(heightControl instanceof HTMLInputElement) ||
    !jointCatalogControl ||
    !jointCatalogNote ||
    !cornerFields ||
    !cornerAngleControl ||
    !cornerTurnControl ||
    !creationStatus ||
    !history
  ) {
    throw new Error("Desktop layout shell could not resolve its required regions.");
  }

  const editorTabs = [...editorTabList.querySelectorAll<HTMLButtonElement>("[data-editor-tab]")];
  const editorPanes = [...shell.querySelectorAll<HTMLElement>("[data-editor-pane]")];
  const setEditorTab = (tab: "properties" | "appearance"): void => {
    for (const editorTab of editorTabs) {
      const active = editorTab.dataset.editorTab === tab;
      editorTab.setAttribute("aria-selected", String(active));
      editorTab.tabIndex = active ? 0 : -1;
    }
    for (const pane of editorPanes) pane.hidden = pane.dataset.editorPane !== tab;
  };
  const onEditorTabClick = (event: MouseEvent): void => {
    const target = event.target;
    if (!(target instanceof Element)) return;
    const tab = target.closest<HTMLButtonElement>("[data-editor-tab]")?.dataset.editorTab;
    if (tab === "properties" || tab === "appearance") setEditorTab(tab);
  };
  const onEditorTabKeyDown = (event: KeyboardEvent): void => {
    const current = editorTabs.findIndex((tab) => tab === (root.getRootNode() as Document | ShadowRoot).activeElement);
    if (current < 0 || editorTabs.length === 0) return;
    const nextIndex = event.key === "ArrowRight"
      ? (current + 1) % editorTabs.length
      : event.key === "ArrowLeft"
        ? (current - 1 + editorTabs.length) % editorTabs.length
        : event.key === "Home"
          ? 0
          : event.key === "End"
            ? editorTabs.length - 1
            : -1;
    if (nextIndex < 0) return;
    event.preventDefault();
    const tab = editorTabs[nextIndex];
    if (tab?.dataset.editorTab === "properties" || tab?.dataset.editorTab === "appearance") {
      setEditorTab(tab.dataset.editorTab);
      tab.focus();
    }
  };
  editorTabList.addEventListener("click", onEditorTabClick);
  editorTabList.addEventListener("keydown", onEditorTabKeyDown);
  setEditorTab("properties");

  const disposeDocumentStatus = session.subscribe((design) => {
    documentStatus.textContent = `设计 r${design.revision} · ${design.windows.length} 樘窗`;
  });
  const disposeSelectionTabs = selection.subscribe((state) => {
    if (state.objectId) setEditorTab("properties");
  });

  const libraryPanes = [...shell.querySelectorAll<HTMLElement>("[data-library-pane]")];
  const setLibraryTab = (tab: "windows" | "components" | "mine"): void => {
    for (const button of libraryTabs.querySelectorAll<HTMLButtonElement>("[data-library-tab]")) {
      const active = button.dataset.libraryTab === tab;
      button.setAttribute("aria-selected", String(active));
      button.tabIndex = active ? 0 : -1;
    }
    for (const pane of libraryPanes) pane.hidden = pane.dataset.libraryPane !== tab;
  };
  const onLibraryTabClick = (event: MouseEvent): void => {
    const target = event.target;
    if (!(target instanceof Element)) return;
    const tab = target.closest<HTMLButtonElement>("[data-library-tab]")?.dataset.libraryTab;
    if (tab === "windows" || tab === "components" || tab === "mine") setLibraryTab(tab);
  };
  const onLibraryTabKeyDown = (event: KeyboardEvent): void => {
    const tabs = [...libraryTabs.querySelectorAll<HTMLButtonElement>("[data-library-tab]")];
    const current = tabs.findIndex((tab) => tab === (root.getRootNode() as Document | ShadowRoot).activeElement);
    if (current < 0) return;
    const next = event.key === "ArrowRight" ? (current + 1) % tabs.length
      : event.key === "ArrowLeft" ? (current - 1 + tabs.length) % tabs.length
      : -1;
    if (next < 0) return;
    event.preventDefault();
    const tab = tabs[next];
    if (tab?.dataset.libraryTab === "windows" ||
      tab?.dataset.libraryTab === "components" || tab?.dataset.libraryTab === "mine") {
      setLibraryTab(tab.dataset.libraryTab);
      tab.focus();
    }
  };
  libraryTabs.addEventListener("click", onLibraryTabClick);
  libraryTabs.addEventListener("keydown", onLibraryTabKeyDown);

  const neutralSlidingOptions = listNeutralSlidingWindowOptions();
  const zcsungProductTemplateOptions = listZcsungSimulationWindowOptions();
  const productTemplateOptions = [
    ...neutralSlidingOptions,
    ...zcsungProductTemplateOptions
  ];
  let localStorage: Pick<Storage, "getItem" | "setItem"> | undefined = templateStorage;
  if (!templateStorage) try { localStorage = window.localStorage; } catch { /* private mode may deny storage */ }
  let savedTemplates = readReusableWindowTemplates(localStorage);

  const createTemplateCard = (input: {
    id: string;
    title: string;
    detail: string;
    kind: "blank" | "inward" | "outward" | "double" | "sliding" | "custom";
  }): HTMLButtonElement => {
    const card = document.createElement("button");
    card.type = "button";
    card.className = "desktop-shell__template-card";
    card.dataset.templateId = input.id;
    const icon = document.createElement("span");
    icon.className = `desktop-shell__template-icon desktop-shell__template-icon--${input.kind}`;
    icon.setAttribute("aria-hidden", "true");
    const copy = document.createElement("span");
    copy.className = "desktop-shell__template-copy";
    const title = document.createElement("strong");
    title.textContent = input.title;
    const detail = document.createElement("small");
    detail.textContent = input.detail;
    copy.append(title, detail);
    card.append(icon, copy);
    return card;
  };
  const presetCards = [
    createTemplateCard({ id: "", title: "手工矩形窗", detail: "空白图型 · 自行绘制分格", kind: "blank" }),
    ...productTemplateOptions.map((template) => createTemplateCard({
      id: template.templateId,
      title: template.label.replace(/（.*$/, "").replace(/ · 两扇两轨.*$/, ""),
      detail: `${template.defaultWidthMm} × ${template.defaultHeightMm} mm · ${template.templateId === NEUTRAL_ORDINARY_SLIDING_TEMPLATE_ID ? "中性验证" : "公开参考"}`,
      kind: template.templateId === NEUTRAL_ORDINARY_SLIDING_TEMPLATE_ID
        ? "sliding"
        : template.templateId.includes("DOUBLE") ? "double"
          : template.templateId.includes("OUT") ? "outward" : "inward"
    }))
  ];
  templateGallery.replaceChildren(...presetCards);
  const renderSavedTemplates = (): void => {
    const cards = savedTemplates.map((template) => createTemplateCard({
      id: `saved:${template.id}`,
      title: template.name,
      detail: `${template.source.widthMm} × ${template.source.heightMm} mm · 本机复用`,
      kind: "custom"
    }));
    if (cards.length === 0) {
      const empty = document.createElement("p");
      empty.className = "desktop-shell__library-empty";
      empty.textContent = "还没有模板。选中已绘制的窗，输入名称后保存。";
      savedTemplateGallery.replaceChildren(empty);
    } else {
      savedTemplateGallery.replaceChildren(...cards);
    }
  };
  renderSavedTemplates();

  const selectTemplate = (id: string): void => {
    productTemplateControl.value = id;
    const card = [...templateGallery.querySelectorAll<HTMLButtonElement>("[data-template-id]"),
      ...savedTemplateGallery.querySelectorAll<HTMLButtonElement>("[data-template-id]")]
      .find((candidate) => candidate.dataset.templateId === id);
    selectedTemplateName.textContent = card?.querySelector("strong")?.textContent ?? "手工矩形窗";
    for (const candidate of [...templateGallery.querySelectorAll<HTMLButtonElement>("[data-template-id]"),
      ...savedTemplateGallery.querySelectorAll<HTMLButtonElement>("[data-template-id]")]) {
      candidate.setAttribute("aria-pressed", String(candidate.dataset.templateId === id));
    }
    synchronizeProductTemplate();
  };
  const onTemplateCardClick = (event: MouseEvent): void => {
    const target = event.target;
    if (!(target instanceof Element)) return;
    const id = target.closest<HTMLButtonElement>("[data-template-id]")?.dataset.templateId;
    if (id !== undefined) selectTemplate(id);
  };
  templateGallery.addEventListener("click", onTemplateCardClick);
  savedTemplateGallery.addEventListener("click", onTemplateCardClick);

  /**
   * Applies a template's neutral starting size without locking user dimensions.
   *
   * Selecting a template is business intent, while width/height remain final
   * design inputs. The hint makes its public-reference/non-production status
   * visible before creation and does not expose profile/PBR implementation data.
   *
   * @since 0.10.77
   * @modified 2026-09-22 - Added the desktop target-product picker behavior.
   */
  const synchronizeProductTemplate = (): void => {
    const neutralSliding = neutralSlidingOptions.find(
      (template) => template.templateId === productTemplateControl.value
    );
    if (neutralSliding) {
      widthControl.value = String(neutralSliding.defaultWidthMm);
      heightControl.value = String(neutralSliding.defaultHeightMm);
      productTemplateNote.textContent =
        "中性两扇两轨设计模板，可改尺寸与搭接；供应商轨槽、滚轮和加工模板尚未审核。";
      return;
    }
    const selected = zcsungProductTemplateOptions.find(
      (template) => template.templateId === productTemplateControl.value
    );
    const saved = savedTemplates.find((template) => `saved:${template.id}` === productTemplateControl.value);
    if (saved) {
      widthControl.value = String(saved.source.widthMm);
      heightControl.value = String(saved.source.heightMm);
      productTemplateNote.textContent = "本机自定义图型；会复制构造与选型，生成新的窗体编号。";
      return;
    }
    if (!selected) {
      widthControl.value = "1200";
      heightControl.value = "1500";
      productTemplateNote.textContent = "手工创建通用矩形窗。";
      return;
    }
    widthControl.value = String(selected.defaultWidthMm);
    heightControl.value = String(selected.defaultHeightMm);
    productTemplateNote.textContent = selected.capabilityStatus === "partial"
      ? "公开参考模拟（部分结构为中性假设），可改尺寸，禁止投产。"
      : "公开参考模拟，可改尺寸；企业工程数据未审核前禁止投产。";
  };
  selectTemplate("");

  const onSaveTemplate = (): void => {
    const name = templateName.value.trim();
    if (!name) {
      templateStatus.value = "请先填写模板名称。";
      return;
    }
    const selectedWindowId = resolveSelectedOwningWindowId(
      session.document,
      selection.state.objectId
    );
    const selectedWindow = session.document.windows.find((candidate) => candidate.objectId === selectedWindowId);
    if (!selectedWindow) {
      templateStatus.value = "请先在画布或构件树中选中一樘窗。";
      return;
    }
    const template: ReusableWindowTemplate = {
      schemaVersion: "doormes-local-window-template.v1",
      id: globalThis.crypto?.randomUUID?.() ?? `${Date.now()}-${Math.random().toString(36).slice(2)}`,
      name,
      savedAt: new Date().toISOString(),
      source: structuredClone(selectedWindow) as WindowUnit
    };
    try {
      const next = [template, ...savedTemplates].slice(0, 100);
      writeReusableWindowTemplates(localStorage, next);
      savedTemplates = next;
      renderSavedTemplates();
      selectTemplate(`saved:${template.id}`);
      templateStatus.value = `已保存“${name}”；现在可重复放入设计。`;
      templateName.value = "";
    } catch (error) {
      templateStatus.value = error instanceof Error ? error.message : "保存模板失败。";
    }
  };
  saveTemplateButton.addEventListener("click", onSaveTemplate);

  /** Projects direction-compatible business connection models into the form. */
  const synchronizeConnectionFields = (): void => {
    const direction = directionControl.value;
    const independent = direction === "independent";
    const vertical = direction === "top" || direction === "bottom";
    const corner = direction === "corner";
    const previous = jointCatalogControl.value;
    const choices = listEngineeringJointCatalogSelections(
      corner ? "corner" : vertical ? "top-bottom" : "left-right"
    );
    jointCatalogControl.replaceChildren(...choices.map((selection) => {
      const option = document.createElement("option");
      option.value = selection.catalogItemId;
      option.textContent = `${selection.businessName} · ${selection.specification}`;
      return option;
    }));
    jointCatalogControl.disabled = independent;
    jointCatalogControl.value = choices.some((choice) => choice.catalogItemId === previous)
      ? previous
      : choices[0]?.catalogItemId ?? "";
    const selected = choices.find(
      (choice) => choice.catalogItemId === jointCatalogControl.value
    );
    for (const field of cornerFields) field.hidden = !corner;
    cornerAngleControl.disabled = !corner;
    cornerTurnControl.disabled = !corner;
    if (corner && selected?.cornerCapability) {
      cornerAngleControl.min = String(selected.cornerCapability.minimumIncludedAngleDeg);
      cornerAngleControl.max = String(selected.cornerCapability.maximumIncludedAngleDeg);
      const currentAngle = Number(cornerAngleControl.value);
      if (!Number.isFinite(currentAngle) ||
        currentAngle < selected.cornerCapability.minimumIncludedAngleDeg ||
        currentAngle > selected.cornerCapability.maximumIncludedAngleDeg) {
        cornerAngleControl.value = String(selected.cornerCapability.defaultIncludedAngleDeg);
      }
    }
    jointCatalogNote.textContent = independent
      ? "连接时选择参考目录型号。"
      : selected
        ? `${selected.specification}；参考目录，尚非企业审核数据。`
        : "当前方向没有可用连接件型号。";
  };
  directionControl.addEventListener("change", synchronizeConnectionFields);
  jointCatalogControl.addEventListener("change", synchronizeConnectionFields);
  synchronizeConnectionFields();

  const onSubmit = (event: SubmitEvent): void => {
    event.preventDefault();
    const values = new FormData(form);
    const sequence = nextAvailableWindowSequence(session.document);
    const transactionId = `CMD-DESKTOP-${sequence}`;
    try {
      const selectedTemplateId = String(values.get("productTemplate") ?? "").trim();
      const selectedSavedTemplate = savedTemplates.find(
        (template) => `saved:${template.id}` === selectedTemplateId
      );
      if (selectedTemplateId.startsWith("saved:") && !selectedSavedTemplate) {
        throw new Error("该本机模板已不可用，请重新选择。");
      }
      const neutralSlidingPlan = selectedTemplateId === NEUTRAL_ORDINARY_SLIDING_TEMPLATE_ID
        ? planNeutralSlidingWindowCreation({
            templateId: selectedTemplateId,
            commandIdPrefix: `${transactionId}:TEMPLATE`,
            windowId: `WIN-${sequence}`,
            instanceMark: `C${sequence}`,
            widthMm: Number(values.get("width")),
            heightMm: Number(values.get("height"))
          })
        : undefined;
      const templatePlan = selectedTemplateId && !neutralSlidingPlan && !selectedSavedTemplate
        ? planZcsungSimulationWindowCreation({
            templateId: selectedTemplateId,
            commandIdPrefix: `${transactionId}:TEMPLATE`,
            windowId: `WIN-${sequence}`,
            instanceMark: `C${sequence}`,
            widthMm: Number(values.get("width")),
            heightMm: Number(values.get("height"))
          })
        : undefined;
      const creationPlan = neutralSlidingPlan ?? templatePlan;
      const createCommand = selectedSavedTemplate
        ? instantiateReusableWindowTemplate({
            template: selectedSavedTemplate,
            commandId: transactionId,
            windowId: `WIN-${sequence}`,
            mark: `C${sequence}`,
            widthMm: Number(values.get("width")),
            heightMm: Number(values.get("height"))
          })
        : creationPlan?.createCommand ?? desktopCreateWindowCommand({
          commandId: `CMD-DESKTOP-${sequence}`,
          windowId: `WIN-${sequence}`,
          mark: `C${sequence}`,
          widthText: String(values.get("width") ?? ""),
          heightText: String(values.get("height") ?? "")
        });
      const direction = String(values.get("creationMode") ?? "independent");
      if (direction === "independent") {
        if (creationPlan) {
          session.executeTransaction(creationPlan.commands, transactionId);
        } else {
          session.execute(createCommand);
        }
        selection.select(createCommand.windowId, "system");
        creationStatus.value = neutralSlidingPlan
          ? "已创建中性普通推拉窗（两扇两轨）；供应商截面与推拉五金目录待审核。"
          : templatePlan
            ? `已创建 ${templatePlan.productName} 公开参考模拟；当前禁止投产。`
          : selectedSavedTemplate
            ? `已从“${selectedSavedTemplate.name}”创建独立窗 ${createCommand.mark}。`
          : `已创建独立窗 ${createCommand.mark}；后续默认从其右侧连接。`;
        creationStatus.dataset.state = "success";
        directionControl.value = "right";
        synchronizeConnectionFields();
        return;
      }
      const anchorWindowId = resolveSelectedOwningWindowId(
        session.document,
        selection.state.objectId
      ) ?? session.document.windows.at(-1)?.objectId;
      if (!anchorWindowId) throw new Error("请先创建或选择一个基准窗，再增加连接窗。");
      const jointCatalogSelection = requireEngineeringJointCatalogSelection(
        String(values.get("jointCatalog") ?? "")
      );
      const plan = planConnectedWindowCreation({
        document: session.document,
        createWindowCommand: createCommand,
        anchorWindowId,
        direction: direction as FabricationConnectionDirection,
        jointType: jointCatalogSelection.jointType,
        gapMm: jointCatalogSelection.finishedWidthMm,
        catalogSelection: jointCatalogSelection,
        ...(direction === "corner" ? {
          cornerIncludedAngleDeg: Number(values.get("cornerAngle")),
          cornerTurnDirection: values.get("cornerTurn") === "counterclockwise"
            ? "counterclockwise" as const
            : "clockwise" as const
        } : {}),
        transactionId,
        afterCreateCommands: creationPlan ? [creationPlan.openingCommand] : undefined
      });
      session.executeTransaction(plan.commands, transactionId);
      selection.select(plan.newWindowId, "system");
      creationStatus.value = `已将 ${createCommand.mark} 连接到 ${anchorWindowId} 的${connectionDirectionLabel(direction)}。`;
      creationStatus.dataset.state = "success";
    } catch (error) {
      creationStatus.value = error instanceof Error ? error.message : "创建或连接失败。";
      creationStatus.dataset.state = "error";
    }
  };
  form.addEventListener("submit", onSubmit);
  history.append(createHistoryControls(session));
  const disposeWorkspace = mountSharedDesignWorkspace(
    workspace,
    session,
    selection,
    canvasView,
    openingPreview,
    visualPreview,
    {
      visualAssetGateway: assetServices.gateway,
      visualAssetQuality: "high",
      constructionToolsContainer: componentTools,
      onOpenProperties: () => {
        setEditorTab("properties");
        objectInspectorPanel.scrollIntoView({ block: "nearest" });
        objectInspectorPanel.focus({ preventScroll: true });
      }
    }
  );
  const disposeWallWorkspace = mountWallPlanWorkspace(wallWorkspace, session);
  const onDrawingModeClick = (event: MouseEvent): void => {
    const target = event.target;
    if (!(target instanceof Element)) return;
    const mode = target.closest<HTMLButtonElement>("[data-drawing-mode]")?.dataset.drawingMode;
    if (mode !== "wall" && mode !== "product") return;
    workspace.hidden = mode === "wall";
    wallWorkspace.hidden = mode !== "wall";
    for (const button of drawingHeader.querySelectorAll<HTMLButtonElement>("[data-drawing-mode]")) {
      button.setAttribute("aria-pressed", String(button.dataset.drawingMode === mode));
    }
    const hint = drawingHeader.querySelector<HTMLElement>("[data-drawing-hint]");
    if (hint) hint.textContent = mode === "wall"
      ? "可选墙体场景 · 墙段、转角、门窗洞口"
      : "主视绘制 / 俯视校核 / 三维仿真";
  };
  drawingHeader.addEventListener("click", onDrawingModeClick);
  const disposeObjectTree = mountDesignObjectTree(
    objectTree,
    session,
    selection,
    "factory-workbench"
  );
  const disposeObjectInspector = mountDesignObjectInspector(
    objectInspector,
    session,
    selection,
    "factory-workbench"
  );
  /*
   * Presentation differs from mobile, but draft validation, slot replacement
   * and the resulting undo command remain inside the shared editor component.
   * @since 0.10.7
   * @modified 2026-09-18 - Mounted the desktop appearance editor slice.
   */
  const disposeAppearanceEditor = mountWindowAppearanceEditor(
    appearanceEditor,
    session,
    selection,
    "factory-workbench",
    visualPreview,
    assetServices
  );

  return () => {
    form.removeEventListener("submit", onSubmit);
    saveTemplateButton.removeEventListener("click", onSaveTemplate);
    libraryTabs.removeEventListener("click", onLibraryTabClick);
    libraryTabs.removeEventListener("keydown", onLibraryTabKeyDown);
    templateGallery.removeEventListener("click", onTemplateCardClick);
    savedTemplateGallery.removeEventListener("click", onTemplateCardClick);
    directionControl.removeEventListener("change", synchronizeConnectionFields);
    editorTabList.removeEventListener("click", onEditorTabClick);
    editorTabList.removeEventListener("keydown", onEditorTabKeyDown);
    disposeDocumentStatus();
    disposeSelectionTabs();
    disposeWorkspace();
    disposeWallWorkspace();
    drawingHeader.removeEventListener("click", onDrawingModeClick);
    disposeObjectTree();
    disposeObjectInspector();
    disposeAppearanceEditor();
    shell.remove();
  };
}

/** Supplies a short localized direction phrase for creation feedback. */
function connectionDirectionLabel(direction: string): string {
  return ({
    left: "左侧",
    right: "右侧",
    top: "上方",
    bottom: "下方",
    corner: "右侧转角"
  } as Record<string, string>)[direction]
    ?? "指定方向";
}
