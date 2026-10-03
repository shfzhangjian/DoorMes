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
import { touchCreateWindowCommand } from "@doormes/interaction-touch";
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
 * Mounts the touch-first phone and tablet layout around the shared session.
 *
 * This shell uses a full-screen drawing surface and bottom sheet instead of the
 * PC three-column composition. It may own gesture and drawer state, but it can
 * only mutate the design through the same application commands as the PC shell.
 *
 * @param root Browser element that receives the mobile shell.
 * @param session Session preserved when switching between shell layouts.
 * @param selection Cross-view selection preserved when layouts switch.
 * @param canvasView Shared 2D navigation and engineering-overlay state.
 * @param openingPreview Shared non-persistent panel preview state.
 * @param visualPreview Shared non-persistent material/model draft state.
 * @param assetServices Shared local or authenticated upload gateway from the app root.
 * @returns A disposer for subscriptions and shell-owned DOM.
 * @example `mountMobileLayoutShell(root, session)` on a coarse pointer device.
 * @since 0.1.0
 * @modified 2026-09-18 - Forwarded durable model-upload services to the shared drawer.
 */
export function mountMobileLayoutShell(
  root: HTMLElement,
  session: DesignSession,
  selection: DesignSelectionStore,
  canvasView: DesignCanvasViewStore,
  openingPreview: OpeningPreviewStore,
  visualPreview: WindowVisualPreviewStore,
  assetServices: AppearanceEditorAssetServices = {}
): () => void {
  const shell = document.createElement("div");
  shell.className = "shell mobile-shell";
  shell.innerHTML = `
    <header class="shell__header">
      <h1 class="shell__title">DoorMes · 移动交互壳</h1>
      <span class="shell__badge">共享核心</span>
    </header>
    <div class="mobile-shell__body">
      <main data-design-workspace></main>
      <section class="mobile-shell__sheet" aria-label="创建窗体">
        <form data-create-window-form>
          <div class="mobile-shell__fields">
            <select name="productTemplate" data-product-template aria-label="产品模板">
              <option value="">手工矩形窗</option>
            </select>
            <input name="width" type="number" inputmode="decimal" value="1200" min="100" aria-label="宽度毫米" />
            <input name="height" type="number" inputmode="decimal" value="1500" min="100" aria-label="高度毫米" />
            <select name="creationMode" data-connection-direction aria-label="放置方式">
              <option value="independent">独立窗</option>
              <option value="right">接当前窗右侧</option>
              <option value="left">接当前窗左侧</option>
              <option value="bottom">接当前窗下方</option>
              <option value="top">接当前窗上方</option>
              <option value="corner">从当前窗右侧转角</option>
            </select>
            <select name="jointCatalog" data-connection-catalog aria-label="连接件型号" disabled></select>
            <input name="cornerAngle" data-corner-angle type="number" inputmode="decimal" value="90" min="60" max="150" step="0.1" aria-label="转角内夹角" hidden disabled />
            <select name="cornerTurn" data-corner-turn aria-label="俯视转向" hidden disabled>
              <option value="clockwise">顺时针</option>
              <option value="counterclockwise">逆时针</option>
            </select>
            <button class="shell-button" type="submit">创建矩形窗</button>
          </div>
          <small data-product-template-note>手工创建通用矩形窗。</small>
          <small data-connection-catalog-note>连接时选择参考目录型号。</small>
          <output class="mobile-shell__creation-status" data-creation-status>首窗独立创建；后续可连接当前选中窗。</output>
        </form>
        <details class="mobile-shell__tools" data-mobile-tools-panel data-mobile-panel>
          <summary>2D绘制工具</summary>
          <p class="mobile-shell__tool-target" data-mobile-grid-tool-status></p>
          <div data-mobile-grid-tools></div>
        </details>
        <details class="mobile-shell__opening" data-mobile-opening-panel data-mobile-panel>
          <summary>开关窗预览</summary>
          <div data-mobile-opening-controls></div>
        </details>
        <div data-history></div>
        <details class="mobile-shell__tree" data-mobile-panel>
          <summary>对象树与当前选择</summary>
          <div data-object-tree></div>
        </details>
        <details class="mobile-shell__inspector" data-mobile-inspector-panel data-mobile-panel>
          <summary>窗体与构件属性</summary>
          <div data-object-inspector></div>
        </details>
        <details class="mobile-shell__appearance" data-mobile-panel>
          <summary>材质与五金型号</summary>
          <div data-appearance-editor></div>
        </details>
      </section>
    </div>`;
  root.append(shell);

  const form = shell.querySelector<HTMLFormElement>("[data-create-window-form]");
  const workspace = shell.querySelector<HTMLElement>("[data-design-workspace]");
  const objectTree = shell.querySelector<HTMLElement>("[data-object-tree]");
  const history = shell.querySelector<HTMLElement>("[data-history]");
  const objectInspector = shell.querySelector<HTMLElement>("[data-object-inspector]");
  const objectInspectorPanel = shell.querySelector<HTMLDetailsElement>(
    "[data-mobile-inspector-panel]"
  );
  const appearanceEditor = shell.querySelector<HTMLElement>("[data-appearance-editor]");
  const constructionTools = shell.querySelector<HTMLElement>("[data-mobile-grid-tools]");
  const constructionToolsPanel = shell.querySelector<HTMLElement>("[data-mobile-tools-panel]");
  const constructionToolsStatus = shell.querySelector<HTMLElement>(
    "[data-mobile-grid-tool-status]"
  );
  const openingControls = shell.querySelector<HTMLElement>("[data-mobile-opening-controls]");
  const openingControlsPanel = shell.querySelector<HTMLElement>("[data-mobile-opening-panel]");
  const directionControl = form?.querySelector<HTMLSelectElement>("[data-connection-direction]");
  const productTemplateControl = form?.querySelector<HTMLSelectElement>("[data-product-template]");
  const productTemplateNote = form?.querySelector<HTMLElement>("[data-product-template-note]");
  const widthControl = form?.elements.namedItem("width");
  const heightControl = form?.elements.namedItem("height");
  const jointCatalogControl = form?.querySelector<HTMLSelectElement>("[data-connection-catalog]");
  const jointCatalogNote = form?.querySelector<HTMLElement>("[data-connection-catalog-note]");
  const cornerAngleControl = form?.querySelector<HTMLInputElement>("[data-corner-angle]");
  const cornerTurnControl = form?.querySelector<HTMLSelectElement>("[data-corner-turn]");
  const creationStatus = form?.querySelector<HTMLOutputElement>("[data-creation-status]");
  const mobilePanels = [
    ...shell.querySelectorAll<HTMLDetailsElement>("details[data-mobile-panel]")
  ];
  if (
    !form ||
    !workspace ||
    !objectTree ||
    !objectInspector ||
    !objectInspectorPanel ||
    !appearanceEditor ||
    !constructionTools ||
    !constructionToolsPanel ||
    !constructionToolsStatus ||
    !openingControls ||
    !openingControlsPanel ||
    !directionControl ||
    !productTemplateControl ||
    !productTemplateNote ||
    !(widthControl instanceof HTMLInputElement) ||
    !(heightControl instanceof HTMLInputElement) ||
    !jointCatalogControl ||
    !jointCatalogNote ||
    !cornerAngleControl ||
    !cornerTurnControl ||
    !creationStatus ||
    mobilePanels.length !== 5 ||
    !history
  ) {
    throw new Error("Mobile layout shell could not resolve its required regions.");
  }

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
   * Projects the shared target-product choice into touch-friendly form defaults.
   *
   * Only name, status and editable dimensions enter the mobile sheet; profile
   * and rendering internals remain inside the shared application plan. This
   * keeps the mobile shell a compact interaction wrapper over the same model.
   *
   * @since 0.10.77
   * @modified 2026-09-22 - Added mobile target-product template selection.
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
      ? "公开参考模拟（部分中性假设），可改尺寸，禁止投产。"
      : "公开参考模拟，可改尺寸；工程数据审核前禁止投产。";
  };
  productTemplateControl.addEventListener("change", synchronizeProductTemplate);

  /** Projects direction-compatible business connection models into the sheet. */
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
      option.textContent = selection.businessName;
      return option;
    }));
    jointCatalogControl.disabled = independent;
    jointCatalogControl.value = choices.some((choice) => choice.catalogItemId === previous)
      ? previous
      : choices[0]?.catalogItemId ?? "";
    const selected = choices.find(
      (choice) => choice.catalogItemId === jointCatalogControl.value
    );
    cornerAngleControl.hidden = !corner;
    cornerAngleControl.disabled = !corner;
    cornerTurnControl.hidden = !corner;
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
        ? `${selected.specification}；尚非企业审核数据。`
        : "当前方向没有可用连接件型号。";
  };
  directionControl.addEventListener("change", synchronizeConnectionFields);
  jointCatalogControl.addEventListener("change", synchronizeConnectionFields);
  synchronizeConnectionFields();

  const onSubmit = (event: SubmitEvent): void => {
    event.preventDefault();
    const values = new FormData(form);
    const sequence = nextAvailableWindowSequence(session.document);
    const transactionId = `CMD-TOUCH-${sequence}`;
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
      const createCommand = creationPlan?.createCommand ?? touchCreateWindowCommand({
          commandId: `CMD-TOUCH-${sequence}`,
          windowId: `WIN-${sequence}`,
          mark: `C${sequence}`,
          widthMm: Number(values.get("width")),
          heightMm: Number(values.get("height"))
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
          : `已创建 ${createCommand.mark}；下一窗默认连接其右侧。`;
        creationStatus.dataset.state = "success";
        directionControl.value = "right";
        synchronizeConnectionFields();
        return;
      }
      const anchorWindowId = resolveSelectedOwningWindowId(
        session.document,
        selection.state.objectId
      ) ?? session.document.windows.at(-1)?.objectId;
      if (!anchorWindowId) throw new Error("请先创建或选择一个基准窗。");
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
      creationStatus.value = `已连接 ${createCommand.mark}，可继续从当前窗追加。`;
      creationStatus.dataset.state = "success";
    } catch (error) {
      creationStatus.value = error instanceof Error ? error.message : "创建或连接失败。";
      creationStatus.dataset.state = "error";
    }
  };
  form.addEventListener("submit", onSubmit);

  /**
   * Keeps the phone bottom sheet as a one-panel accordion.
   *
   * Native `<details>` supplies keyboard/accessibility behavior; this shell-only
   * coordinator closes siblings after one panel opens so tools, tree and full
   * properties cannot jointly consume the entire drawing viewport. The
   * appearance entry may expand to a full-screen form, but still participates
   * in the same accordion and never owns model or command state.
   *
   * @example Opening “构件属性” closes an already open “2D绘制工具”.
   * @since 0.4.6
   * @modified 2026-09-18 - Included the appearance drawer in panel exclusivity.
   */
  const onPanelToggle = (event: Event): void => {
    const opened = event.currentTarget;
    if (!(opened instanceof HTMLDetailsElement) || !opened.open) return;
    for (const panel of mobilePanels) {
      if (panel !== opened) panel.open = false;
    }
  };
  for (const panel of mobilePanels) panel.addEventListener("toggle", onPanelToggle);
  history.append(createHistoryControls(session));
  const disposeWorkspace = mountSharedDesignWorkspace(
    workspace,
    session,
    selection,
    canvasView,
    openingPreview,
    visualPreview,
    {
      constructionToolsContainer: constructionTools,
      constructionToolsPanel,
      constructionToolsStatus,
      openingControlsContainer: openingControls,
      openingControlsPanel,
      visualAssetGateway: assetServices.gateway,
      visualAssetQuality: "low",
      onOpenProperties: () => {
        objectInspectorPanel.open = true;
        objectInspectorPanel.scrollIntoView({ block: "nearest" });
        requestAnimationFrame(() => objectInspectorPanel.querySelector("summary")?.focus());
      }
    }
  );
  const disposeObjectTree = mountDesignObjectTree(objectTree, session, selection);
  const disposeObjectInspector = mountDesignObjectInspector(objectInspector, session, selection);
  /*
   * Only the container presentation is mobile-specific. The editor receives
   * the same session/selection and emits the same complete command as desktop.
   * @since 0.10.7
   * @modified 2026-09-18 - Mounted the touch-safe full-screen editor.
   */
  const disposeAppearanceEditor = mountWindowAppearanceEditor(
    appearanceEditor,
    session,
    selection,
    "mobile-steps",
    visualPreview,
    assetServices
  );

  return () => {
    form.removeEventListener("submit", onSubmit);
    directionControl.removeEventListener("change", synchronizeConnectionFields);
    productTemplateControl.removeEventListener("change", synchronizeProductTemplate);
    for (const panel of mobilePanels) panel.removeEventListener("toggle", onPanelToggle);
    disposeWorkspace();
    disposeObjectTree();
    disposeObjectInspector();
    disposeAppearanceEditor();
    shell.remove();
  };
}
