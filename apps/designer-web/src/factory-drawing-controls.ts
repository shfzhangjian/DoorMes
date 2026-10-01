import {
  createUpdateFactoryDrawingAnnotationLayoutCommand,
  type DesignSelectionStore,
  type DesignSession
} from "@doormes/application";
import type {
  DesignDocument,
  WindowUnit
} from "@doormes/contracts";
import type {
  FactoryComponentCalloutMode,
  FactoryDrawingProductionSnapshot
} from "@doormes/drawing-projection";
import { resolveWindowGeometry } from "@doormes/geometry-topology";
import type { ProjectFileStatusReporter } from "./project-file-controls";

/**
 * Boundary through which a catalog supplies a revision-bound formal
 * calculation to technical drawing export. The prototype composition root
 * injects an explicitly named simulation catalog; a factory deployment must
 * replace it with its reviewed enterprise catalog before release.
 *
 * @example A factory workspace can calculate and return `{ sourceRevision,
 * result }`; the existing drawing pipeline then prints a formal MBOM without
 * changing SVG code.
 * @since 0.10.56
 * @modified 2026-09-23 - Connected the reference simulation calculation provider.
 */
export type FactoryDrawingProductionSnapshotProvider = (
  document: DesignDocument,
  subjectId: string
) => FactoryDrawingProductionSnapshot | undefined |
  Promise<FactoryDrawingProductionSnapshot | undefined>;

/**
 * Determines whether one stable selection ID belongs to a window or its child geometry.
 *
 * The resolver uses shared geometry IDs rather than querying DOM/SVG nodes. This
 * keeps factory export selection identical across object tree, 2D and 3D views.
 *
 * @example Selecting one handle or sash panel returns its owning `WindowUnit`.
 * @since 0.10.55
 * @modified 2026-09-21 - Added object-to-factory-subject resolution.
 */
function windowOwnsSelection(window: WindowUnit, selectedObjectId: string): boolean {
  if (selectedObjectId === window.objectId || selectedObjectId.startsWith(`${window.objectId}:`)) {
    return true;
  }
  const geometry = resolveWindowGeometry(window);
  return [
    ...geometry.frames.map((item) => item.objectId),
    ...geometry.cells.map((item) => item.objectId),
    ...geometry.members.map((item) => item.objectId),
    ...geometry.meetingMullions.map((item) => item.objectId),
    ...geometry.openings.flatMap((item) => [item.objectId, `${item.objectId}::${item.panelId}`]),
    ...geometry.hardware.map((item) => item.hardwareId)
  ].some((objectId) =>
    selectedObjectId === objectId ||
    selectedObjectId.startsWith(`${objectId}:`) ||
    selectedObjectId.startsWith(`${objectId}::`) ||
    selectedObjectId.startsWith(`${objectId}.`)
  );
}

/**
 * Resolves the product scope exported by the header's factory-drawing action.
 *
 * Algorithm: an explicitly selected assembly/instance/joint wins; otherwise
 * resolve the selected window or child. A window inside exactly one connected
 * product exports that entire assembly. With no selection, automatic export is
 * allowed only when the document has exactly one unambiguous product subject.
 *
 * @param document Current immutable design snapshot.
 * @param selectedObjectId Shared 2D/3D/tree selection, when present.
 * @returns Window or fabrication-assembly object ID; undefined when ambiguous.
 * @example Selecting `ASSEMBLY-1:J1` returns `ASSEMBLY-1`.
 * @since 0.10.55
 * @modified 2026-09-21 - Added safe factory drawing subject selection.
 */
export function resolveFactoryDrawingSubjectId(
  document: DesignDocument,
  selectedObjectId?: string
): string | undefined {
  const assemblies = document.assemblies ?? [];
  if (selectedObjectId) {
    const directAssembly = assemblies.find((assembly) =>
      selectedObjectId === assembly.objectId ||
      assembly.instances.some((instance) => instance.objectId === selectedObjectId) ||
      assembly.joints.some((joint) => joint.objectId === selectedObjectId)
    );
    if (directAssembly) return directAssembly.objectId;
    const ownerWindow = document.windows.find((window) =>
      windowOwnsSelection(window, selectedObjectId)
    );
    if (ownerWindow) {
      const containingAssemblies = assemblies.filter((assembly) =>
        assembly.instances.some((instance) => instance.windowId === ownerWindow.objectId)
      );
      if (containingAssemblies.length === 1) return containingAssemblies[0]!.objectId;
      if (containingAssemblies.length > 1) return undefined;
      return ownerWindow.objectId;
    }
  }
  const connectedWindowIds = new Set(
    assemblies.flatMap((assembly) => assembly.instances.map((instance) => instance.windowId))
  );
  const standaloneWindows = document.windows.filter((window) =>
    !connectedWindowIds.has(window.objectId)
  );
  const subjects = [
    ...assemblies.map((assembly) => assembly.objectId),
    ...standaloneWindows.map((window) => window.objectId)
  ];
  return subjects.length === 1 ? subjects[0] : undefined;
}

/**
 * Converts a product ID into a safe deterministic download filename fragment.
 * @example `A 01/总装` becomes `A-01-` plus the retained safe characters.
 * @since 0.10.55
 * @modified 2026-09-21 - Added technical SVG filenames.
 */
function drawingFilenamePart(value: string): string {
  return value.trim().replace(/[^a-zA-Z0-9._-]+/g, "-").replace(/^-+|-+$/g, "") || "product";
}

/** Immutable browser-preview artifact shared by preview, SVG download and print. */
export interface FactoryDrawingPreviewArtifact {
  /** First page retained for single-page download/backward compatibility. */
  readonly source: string;
  /** Ordered physical pages; absent only for older single-page callers. */
  readonly sources?: readonly string[];
  readonly filename: string;
  readonly title: string;
  readonly paperFormat: "A4" | "A3";
  readonly orientation: "portrait" | "landscape";
  readonly sourceRevision: number;
  readonly drawingNumber: string;
  readonly drawingVersion: string;
}

/** Escapes sheet identity before it enters the preview document's HTML title. */
function escapePreviewHtml(value: string): string {
  return value
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}

/**
 * Wraps one already-rendered SVG snapshot in a physical-paper print document.
 *
 * The browser print dialog can print directly or save to PDF. `@page`, body and
 * SVG all use the same ISO dimensions, preventing browser margins or responsive
 * CSS from scaling an A3 sheet after it has already been laid out in paper mm.
 * No application script or live design session enters the iframe document.
 *
 * @example An A3 landscape artifact produces `size: 420mm 297mm` and reuses the
 * exact SVG source shown in the page preview.
 * @since 0.10.59
 * @modified 2026-09-21 - Added shared preview/print/PDF document composition.
 */
export function buildFactoryDrawingPrintDocument(
  artifact: Pick<
    FactoryDrawingPreviewArtifact,
    "source" | "sources" | "title" | "paperFormat" | "orientation" | "sourceRevision" |
    "drawingNumber" | "drawingVersion"
  >
): string {
  const portrait = artifact.paperFormat === "A3"
    ? { width: 297, height: 420 }
    : { width: 210, height: 297 };
  const page = artifact.orientation === "portrait"
    ? portrait
    : { width: portrait.height, height: portrait.width };
  const title = escapePreviewHtml(
    `${artifact.drawingNumber} ${artifact.drawingVersion} · ${artifact.title}`
  );
  const sources = "sources" in artifact && Array.isArray(artifact.sources) && artifact.sources.length
    ? artifact.sources
    : [artifact.source];
  const pages = sources.map((source, index) =>
    `<main class="factory-print-sheet" data-page-number="${index + 1}">${source}</main>`
  ).join("");
  return `<!doctype html><html lang="zh-CN"><head><meta charset="utf-8" /><title>${title}</title><style>@page{size:${page.width}mm ${page.height}mm;margin:0}html,body{width:${page.width}mm;min-height:${page.height}mm;height:auto;margin:0;padding:0;background:#dbe3ec}body{-webkit-print-color-adjust:exact;print-color-adjust:exact}.factory-print-sheet{width:${page.width}mm;height:${page.height}mm;margin:0 auto 8mm;background:#fff;break-after:page;page-break-after:always}.factory-print-sheet:last-child{break-after:auto;page-break-after:auto}.factory-print-sheet>svg{display:block;width:${page.width}mm;height:${page.height}mm}@media print{body{background:#fff}.factory-print-sheet{margin:0}}@media screen{html,body{width:100%;height:auto}body{overflow:auto}.factory-print-sheet{max-width:100%;height:auto}.factory-print-sheet>svg{width:100%;height:auto}}</style></head><body>${pages}</body></html>`;
}

/**
 * Mounts factory drawing preview, SVG download and browser print/PDF actions.
 *
 * Projection and SVG serialization are loaded only after activation, so the
 * existing design/3D startup bundle does not eagerly include drawing exporters.
 * The current immutable revision is projected directly; interactive SVG nodes,
 * browser zoom and current 3D camera are never read.
 *
 * @param header Active PC or mobile shell header.
 * @param session Shared formal design session.
 * @param selection Shared cross-view selection store.
 * @param report Non-blocking status reporter.
 * @param productionSnapshotProvider Optional reviewed catalog/calculation boundary.
 * @returns Listener/control disposer used when the responsive shell is rebuilt.
 * @example `mountFactoryDrawingControls(header, session, selection, report)`.
 * @since 0.10.55
 * @modified 2026-09-21 - Added same-snapshot preview, SVG and print/PDF actions.
 */
export function mountFactoryDrawingControls(
  header: HTMLElement,
  session: DesignSession,
  selection: DesignSelectionStore,
  report: ProjectFileStatusReporter,
  productionSnapshotProvider?: FactoryDrawingProductionSnapshotProvider
): () => void {
  const controls = document.createElement("div");
  controls.className = "drawing-export-controls";
  const exportButton = document.createElement("button");
  exportButton.type = "button";
  exportButton.textContent = "预览工厂图";
  controls.append(exportButton);
  header.append(controls);

  const dialog = document.createElement("dialog");
  dialog.className = "factory-drawing-dialog";
  dialog.setAttribute("aria-label", "工厂技术图纸预览");
  const dialogHeader = document.createElement("header");
  dialogHeader.className = "factory-drawing-dialog__header";
  const dialogTitle = document.createElement("div");
  const dialogHeading = document.createElement("strong");
  dialogHeading.textContent = "工厂技术图纸";
  const dialogMeta = document.createElement("span");
  dialogMeta.textContent = "请选择产品后生成预览";
  dialogTitle.append(dialogHeading, dialogMeta);
  const closeButton = document.createElement("button");
  closeButton.type = "button";
  closeButton.textContent = "关闭";
  dialogHeader.append(dialogTitle, closeButton);
  const previewStage = document.createElement("div");
  previewStage.className = "factory-drawing-dialog__stage";
  const previewFrame = document.createElement("iframe");
  previewFrame.className = "factory-drawing-dialog__frame";
  previewFrame.title = "工厂图纸页面预览";
  previewFrame.setAttribute("sandbox", "allow-same-origin allow-modals");
  previewStage.append(previewFrame);
  const dialogActions = document.createElement("footer");
  dialogActions.className = "factory-drawing-dialog__actions";
  const statusGroup = document.createElement("div");
  statusGroup.className = "factory-drawing-dialog__status";
  const snapshotStatus = document.createElement("span");
  snapshotStatus.textContent = "尚未生成图纸";
  const interactionHint = document.createElement("span");
  interactionHint.textContent = "编号自动避让；可在图上拖动后锁定";
  statusGroup.append(snapshotStatus, interactionHint);
  const actionGroup = document.createElement("div");
  const calloutModeLabel = document.createElement("label");
  calloutModeLabel.textContent = "图面编号";
  const calloutModeSelect = document.createElement("select");
  const calloutModeOptions: readonly [FactoryComponentCalloutMode, string][] = [
    ["all", "全部"],
    ["profiles", "型材/连接"],
    ["glass-hardware", "玻璃/五金"],
    ["selected", "仅当前选择"]
  ];
  calloutModeOptions.forEach(([value, label]) => {
    const option = document.createElement("option");
    option.value = value;
    option.textContent = label;
    calloutModeSelect.append(option);
  });
  calloutModeLabel.append(calloutModeSelect);
  const resetLayoutButton = document.createElement("button");
  resetLayoutButton.type = "button";
  resetLayoutButton.textContent = "自动重排编号";
  const downloadButton = document.createElement("button");
  downloadButton.type = "button";
  downloadButton.textContent = "下载SVG";
  const printButton = document.createElement("button");
  printButton.type = "button";
  printButton.textContent = "打印 / 另存PDF";
  actionGroup.append(calloutModeLabel, resetLayoutButton, downloadButton, printButton);
  dialogActions.append(statusGroup, actionGroup);
  dialog.append(dialogHeader, previewStage, dialogActions);
  document.body.append(dialog);

  let selectedObjectId = selection.state.objectId;
  let exporting = false;
  let previewLoaded = false;
  let artifact: FactoryDrawingPreviewArtifact | undefined;
  let commandSequence = 0;
  let disposePreviewInteractions = (): void => undefined;
  const updateState = (): void => {
    const subjectId = resolveFactoryDrawingSubjectId(session.document, selectedObjectId);
    exportButton.disabled = exporting || !subjectId;
    exportButton.title = subjectId
      ? `预览${subjectId}的A3黑白工厂总装图（可下载SVG或打印为PDF）`
      : "请先选择一樘独立窗或一个连接组合";
    downloadButton.disabled = !artifact;
    printButton.disabled = !artifact || !previewLoaded;
    resetLayoutButton.disabled = exporting ||
      !(session.document.factoryDrawingAnnotationLayouts?.some((layout) => layout.locked));
  };
  const disposeSelection = selection.subscribe((state) => {
    selectedObjectId = state.objectId;
    updateState();
  });
  const disposeSession = session.subscribe(() => updateState());

  const onPreview = async (): Promise<void> => {
    const subjectId = resolveFactoryDrawingSubjectId(session.document, selectedObjectId);
    if (!subjectId || exporting) {
      report("warning", "请选择一樘独立窗或一个连接组合后再预览工厂图");
      return;
    }
    exporting = true;
    artifact = undefined;
    previewLoaded = false;
    updateState();
    report("warning", `正在生成工厂总装图：${subjectId}`);
    try {
      const [{ projectFactoryDrawingSheets }, { renderTechnicalDrawingSheetSvg }] =
        await Promise.all([
          import("@doormes/drawing-projection"),
          import("@doormes/renderer-drawing-svg")
        ]);
      const productionSnapshot = await productionSnapshotProvider?.(
        session.document,
        subjectId
      );
      const sheets = projectFactoryDrawingSheets(session.document, subjectId, {
        ...(productionSnapshot ? { productionSnapshot } : {}),
        componentCalloutMode: calloutModeSelect.value as FactoryComponentCalloutMode,
        selectedObjectIds: selectedObjectId ? [selectedObjectId] : []
      });
      const sheet = sheets[0]!;
      const sources = sheets.map((candidate) => renderTechnicalDrawingSheetSvg(candidate));
      const source = sources[0]!;
      artifact = {
        source,
        sources,
        filename: `DoorMes-${drawingFilenamePart(sheet.drawingNumber)}-${drawingFilenamePart(sheet.drawingVersion)}.svg`,
        title: sheet.title,
        paperFormat: sheet.paperFormat,
        orientation: sheet.orientation,
        sourceRevision: sheet.sourceRevision,
        drawingNumber: sheet.drawingNumber,
        drawingVersion: sheet.drawingVersion
      };
      dialogHeading.textContent = sheet.title;
      dialogMeta.textContent = `图号 ${sheet.drawingNumber} · 版本 ${sheet.drawingVersion} · ${sheet.paperFormat} · ${sheet.orientation === "landscape" ? "横向" : "纵向"} · 共${sheets.length}页`;
      snapshotStatus.textContent = `门窗设计组成件表 · ${sheets.length}页 · 设计修订 r${sheet.sourceRevision}`;
      previewFrame.srcdoc = buildFactoryDrawingPrintDocument(artifact);
      if (!dialog.open) dialog.showModal();
      report(
        "ready",
        `工厂总装图预览已生成 · 图号 ${sheet.drawingNumber} · 版本 ${sheet.drawingVersion} · ${sheets.length}页`
      );
    } catch (error) {
      report(
        "error",
        error instanceof Error ? `工厂图生成失败：${error.message}` : "工厂图生成失败"
      );
    } finally {
      exporting = false;
      updateState();
    }
  };

  const onPreviewLoaded = (): void => {
    previewLoaded = true;
    disposePreviewInteractions();
    const frameDocument = previewFrame.contentDocument;
    const frameWindow = previewFrame.contentWindow;
    if (frameDocument && frameWindow) {
      type ActiveDrag = {
        readonly group: SVGGElement;
        readonly svg: SVGSVGElement;
        readonly annotationId: string;
        readonly pointerId: number;
        readonly startClientX: number;
        readonly startClientY: number;
        readonly baseOffsetX: number;
        readonly baseOffsetY: number;
        readonly originalTransform: string | null;
        moved: boolean;
      };
      let active: ActiveDrag | undefined;
      const paperDelta = (
        svg: SVGSVGElement,
        clientX: number,
        clientY: number,
        startClientX: number,
        startClientY: number
      ): Readonly<{ x: number; y: number }> => {
        const bounds = svg.getBoundingClientRect();
        const viewBox = svg.viewBox.baseVal;
        return {
          x: bounds.width > 0 ? (clientX - startClientX) * viewBox.width / bounds.width : 0,
          y: bounds.height > 0 ? (clientY - startClientY) * viewBox.height / bounds.height : 0
        };
      };
      const onPointerDown = (event: PointerEvent): void => {
        const target = event.target as Element | null;
        const group = typeof target?.closest === "function" ? target.closest<SVGGElement>(
          "g.technical-callout--component-callout[data-annotation-id][data-label-offset-x][data-label-offset-y]"
        ) : null;
        if (!group || !group.dataset.sourceObjectIds?.includes("PI-LOCAL-")) return;
        const svg = group.ownerSVGElement;
        const annotationId = group.dataset.annotationId;
        const baseOffsetX = Number(group.dataset.labelOffsetX);
        const baseOffsetY = Number(group.dataset.labelOffsetY);
        if (!svg || !annotationId || !Number.isFinite(baseOffsetX) || !Number.isFinite(baseOffsetY)) {
          return;
        }
        active = {
          group,
          svg,
          annotationId,
          pointerId: event.pointerId,
          startClientX: event.clientX,
          startClientY: event.clientY,
          baseOffsetX,
          baseOffsetY,
          originalTransform: group.getAttribute("transform"),
          moved: false
        };
        group.setPointerCapture?.(event.pointerId);
        event.preventDefault();
      };
      const onPointerMove = (event: PointerEvent): void => {
        if (!active || active.pointerId !== event.pointerId) return;
        const delta = paperDelta(
          active.svg,
          event.clientX,
          event.clientY,
          active.startClientX,
          active.startClientY
        );
        active.moved = active.moved || Math.hypot(delta.x, delta.y) >= 0.5;
        active.group.setAttribute("transform", `translate(${delta.x} ${delta.y})`);
        event.preventDefault();
      };
      const finishDrag = (event: PointerEvent): void => {
        if (!active || active.pointerId !== event.pointerId) return;
        const completed = active;
        active = undefined;
        const delta = paperDelta(
          completed.svg,
          event.clientX,
          event.clientY,
          completed.startClientX,
          completed.startClientY
        );
        if (completed.originalTransform === null) completed.group.removeAttribute("transform");
        else completed.group.setAttribute("transform", completed.originalTransform);
        if (!completed.moved) return;
        commandSequence += 1;
        session.execute(createUpdateFactoryDrawingAnnotationLayoutCommand({
          commandId: `CMD-FACTORY-CALLOUT-LAYOUT-${commandSequence}`,
          annotationId: completed.annotationId,
          offsetPaperMm: {
            x: completed.baseOffsetX + delta.x,
            y: completed.baseOffsetY + delta.y
          },
          locked: true
        }));
        void onPreview();
      };
      frameDocument.addEventListener("pointerdown", onPointerDown);
      frameDocument.addEventListener("pointermove", onPointerMove);
      frameDocument.addEventListener("pointerup", finishDrag);
      frameDocument.addEventListener("pointercancel", finishDrag);
      frameDocument.querySelectorAll<SVGGElement>(
        "g.technical-callout--component-callout[data-source-object-ids*='PI-LOCAL-']"
      ).forEach((group) => {
        group.style.cursor = "grab";
        group.style.touchAction = "none";
      });
      disposePreviewInteractions = () => {
        frameDocument.removeEventListener("pointerdown", onPointerDown);
        frameDocument.removeEventListener("pointermove", onPointerMove);
        frameDocument.removeEventListener("pointerup", finishDrag);
        frameDocument.removeEventListener("pointercancel", finishDrag);
      };
    }
    updateState();
  };
  const onResetLayout = (): void => {
    const locked = session.document.factoryDrawingAnnotationLayouts?.filter(
      (layout) => layout.locked
    ) ?? [];
    if (!locked.length) return;
    const commands = locked.map((layout) => {
      commandSequence += 1;
      return createUpdateFactoryDrawingAnnotationLayoutCommand({
        commandId: `CMD-FACTORY-CALLOUT-RESET-${commandSequence}`,
        annotationId: layout.annotationId,
        offsetPaperMm: { x: 0, y: 0 },
        locked: false
      });
    });
    session.executeTransaction(commands, `TX-FACTORY-CALLOUT-RESET-${commandSequence}`);
    void onPreview();
  };
  const onDownload = (): void => {
    if (!artifact) return;
    const currentArtifact = artifact;
    const sources = currentArtifact.sources?.length
      ? currentArtifact.sources
      : [currentArtifact.source];
    const extensionIndex = currentArtifact.filename.toLowerCase().lastIndexOf(".svg");
    const filenameBase = extensionIndex >= 0
      ? currentArtifact.filename.slice(0, extensionIndex)
      : currentArtifact.filename;
    sources.forEach((source, index) => {
      const url = URL.createObjectURL(new Blob(
        [source],
        { type: "image/svg+xml;charset=utf-8" }
      ));
      const anchor = document.createElement("a");
      anchor.href = url;
      anchor.download = sources.length === 1
        ? currentArtifact.filename
        : `${filenameBase}-P${index + 1}.svg`;
      anchor.click();
      setTimeout(() => URL.revokeObjectURL(url), 0);
    });
    report("ready", `已下载工厂图SVG · ${sources.length}页 · r${currentArtifact.sourceRevision}`);
  };
  const onPrint = (): void => {
    if (!artifact || !previewLoaded || !previewFrame.contentWindow) {
      report("warning", "图纸页面仍在载入，请稍后再打印");
      return;
    }
    previewFrame.contentWindow.focus();
    previewFrame.contentWindow.print();
    report("ready", `已打开打印窗口；可选择打印机或“另存为PDF” · r${artifact.sourceRevision}`);
  };
  const onClose = (): void => dialog.close();
  const onCalloutModeChange = (): void => {
    if (dialog.open) void onPreview();
  };

  exportButton.addEventListener("click", onPreview);
  previewFrame.addEventListener("load", onPreviewLoaded);
  calloutModeSelect.addEventListener("change", onCalloutModeChange);
  resetLayoutButton.addEventListener("click", onResetLayout);
  downloadButton.addEventListener("click", onDownload);
  printButton.addEventListener("click", onPrint);
  closeButton.addEventListener("click", onClose);
  updateState();
  return () => {
    exportButton.removeEventListener("click", onPreview);
    previewFrame.removeEventListener("load", onPreviewLoaded);
    calloutModeSelect.removeEventListener("change", onCalloutModeChange);
    resetLayoutButton.removeEventListener("click", onResetLayout);
    downloadButton.removeEventListener("click", onDownload);
    printButton.removeEventListener("click", onPrint);
    closeButton.removeEventListener("click", onClose);
    disposeSelection();
    disposeSession();
    disposePreviewInteractions();
    if (dialog.open) dialog.close();
    dialog.remove();
    controls.remove();
  };
}
