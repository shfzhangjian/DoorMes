import {
  nextAvailableWindowSequence,
  planConnectedWindowCreation,
  listEngineeringJointCatalogSelections,
  listZcsungSimulationWindowOptions,
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
 * @modified 2026-09-18 - Bounded the object tree and inspector within the desktop viewport.
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
      <h1 class="shell__title">DoorMes · PC交互壳</h1>
      <span class="shell__badge">共享模型 / 共享SVG / 共享算法</span>
    </header>
    <div class="desktop-shell__workspace">
      <aside class="shell-panel desktop-shell__left-panel">
        <h2>门窗创建与组合</h2>
        <form class="shell-form" data-create-window-form>
          <label>产品模板
            <select name="productTemplate" data-product-template>
              <option value="">手工矩形窗</option>
            </select>
          </label>
          <small data-product-template-note>手工创建通用矩形窗。</small>
          <label>宽度（mm）<input name="width" type="number" value="1200" min="100" /></label>
          <label>高度（mm）<input name="height" type="number" value="1500" min="100" /></label>
          <label>放置方式
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
          <button class="shell-button" type="submit">创建矩形窗</button>
          <output class="shell-form__status" data-creation-status>首窗默认独立创建；后续可连接到当前选中窗。</output>
        </form>
      </aside>
      <main data-design-workspace></main>
      <aside class="shell-panel desktop-shell__right-panel">
        <section class="desktop-shell__panel-section desktop-shell__panel-section--tree">
          <h2>对象树</h2>
          <p>点击树节点、2D构件或3D构件，三个视图同步选择。</p>
          <div data-object-tree></div>
        </section>
        <section class="desktop-shell__panel-section" data-object-inspector-panel tabindex="-1">
          <h2>窗体与构件属性</h2>
          <div data-object-inspector></div>
        </section>
        <section class="desktop-shell__panel-section">
          <h2>材质与五金型号</h2>
          <div data-appearance-editor></div>
        </section>
        <div data-history></div>
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

  const productTemplateOptions = listZcsungSimulationWindowOptions();
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
    const selected = productTemplateOptions.find(
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
      const templatePlan = selectedTemplateId
        ? planZcsungSimulationWindowCreation({
            templateId: selectedTemplateId,
            commandIdPrefix: `${transactionId}:TEMPLATE`,
            windowId: `WIN-${sequence}`,
            instanceMark: `C${sequence}`,
            widthMm: Number(values.get("width")),
            heightMm: Number(values.get("height"))
          })
        : undefined;
      const createCommand = templatePlan?.createCommand ?? desktopCreateWindowCommand({
          commandId: `CMD-DESKTOP-${sequence}`,
          windowId: `WIN-${sequence}`,
          mark: `C${sequence}`,
          widthText: String(values.get("width") ?? ""),
          heightText: String(values.get("height") ?? "")
        });
      const direction = String(values.get("creationMode") ?? "independent");
      if (direction === "independent") {
        if (templatePlan) {
          session.executeTransaction(templatePlan.commands, transactionId);
        } else {
          session.execute(createCommand);
        }
        selection.select(createCommand.windowId, "system");
        creationStatus.value = templatePlan
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
        afterCreateCommands: templatePlan ? [templatePlan.openingCommand] : undefined
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
        objectInspectorPanel.scrollIntoView({ block: "nearest" });
        objectInspectorPanel.focus({ preventScroll: true });
      }
    }
  );
  const disposeObjectTree = mountDesignObjectTree(objectTree, session, selection);
  const disposeObjectInspector = mountDesignObjectInspector(objectInspector, session, selection);
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
    "desktop-panel",
    visualPreview,
    assetServices
  );

  return () => {
    form.removeEventListener("submit", onSubmit);
    directionControl.removeEventListener("change", synchronizeConnectionFields);
    productTemplateControl.removeEventListener("change", synchronizeProductTemplate);
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
