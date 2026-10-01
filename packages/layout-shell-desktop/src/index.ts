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
  assetServices: AppearanceEditorAssetServices = {}
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
    <div class="desktop-shell__workspace">
      <aside class="shell-panel desktop-shell__left-panel">
        <div class="desktop-shell__panel-heading">
          <span>01 · 产品定义</span>
          <h2>新建设计</h2>
        </div>
        <p class="desktop-shell__panel-intro">先定义窗体和组合关系，再在画布中完成分格与构件设计。</p>
        <form class="shell-form" data-create-window-form>
          <details class="desktop-shell__form-group" open>
            <summary>产品模板</summary>
            <label>产品系列
            <select name="productTemplate" data-product-template>
              <option value="">手工矩形窗</option>
            </select>
            </label>
            <small data-product-template-note>手工创建通用矩形窗。</small>
          </details>
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
          <button class="shell-button" type="submit">创建窗体并加入设计</button>
          <output class="shell-form__status" data-creation-status>首窗默认独立创建；后续可连接到当前选中窗。</output>
        </form>
      </aside>
      <main data-design-workspace></main>
      <aside class="shell-panel desktop-shell__right-panel">
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
  const objectTree = shell.querySelector<HTMLElement>("[data-object-tree]");
  const history = shell.querySelector<HTMLElement>("[data-history]");
  const objectInspector = shell.querySelector<HTMLElement>("[data-object-inspector]");
  const objectInspectorPanel = shell.querySelector<HTMLElement>("[data-object-inspector-panel]");
  const appearanceEditor = shell.querySelector<HTMLElement>("[data-appearance-editor]");
  const documentStatus = shell.querySelector<HTMLOutputElement>("[data-document-status]");
  const editorTabList = shell.querySelector<HTMLElement>("[data-editor-tabs]");
  const directionControl = form?.querySelector<HTMLSelectElement>("[data-connection-direction]");
  const productTemplateControl = form?.querySelector<HTMLSelectElement>("[data-product-template]");
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
    !objectTree ||
    !objectInspector ||
    !objectInspectorPanel ||
    !appearanceEditor ||
    !documentStatus ||
    !editorTabList ||
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
    const current = editorTabs.findIndex((tab) => tab === document.activeElement);
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

  const neutralSlidingOptions = listNeutralSlidingWindowOptions();
  const zcsungProductTemplateOptions = listZcsungSimulationWindowOptions();
  const productTemplateOptions = [
    ...neutralSlidingOptions,
    ...zcsungProductTemplateOptions
  ];
  for (const template of productTemplateOptions) {
    const option = document.createElement("option");
    option.value = template.templateId;
    option.textContent = template.label;
    productTemplateControl.append(option);
  }

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
    if (!selected) {
      productTemplateNote.textContent = "手工创建通用矩形窗。";
      return;
    }
    widthControl.value = String(selected.defaultWidthMm);
    heightControl.value = String(selected.defaultHeightMm);
    productTemplateNote.textContent = selected.capabilityStatus === "partial"
      ? "公开参考模拟（部分结构为中性假设），可改尺寸，禁止投产。"
      : "公开参考模拟，可改尺寸；企业工程数据未审核前禁止投产。";
  };
  productTemplateControl.addEventListener("change", synchronizeProductTemplate);

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
      const templatePlan = selectedTemplateId && !neutralSlidingPlan
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
      const createCommand = creationPlan?.createCommand ?? desktopCreateWindowCommand({
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
      onOpenProperties: () => {
        setEditorTab("properties");
        objectInspectorPanel.scrollIntoView({ block: "nearest" });
        objectInspectorPanel.focus({ preventScroll: true });
      }
    }
  );
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
    directionControl.removeEventListener("change", synchronizeConnectionFields);
    productTemplateControl.removeEventListener("change", synchronizeProductTemplate);
    editorTabList.removeEventListener("click", onEditorTabClick);
    editorTabList.removeEventListener("keydown", onEditorTabKeyDown);
    disposeDocumentStatus();
    disposeSelectionTabs();
    disposeWorkspace();
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
