import {
  createFabricationAssemblyCommand,
  createRectangularWindowCommand,
  createSetWindowCellOpeningCommand,
  createUpdateWindowInstallationCommand,
  DesignCanvasViewStore,
  DesignSelectionStore,
  DesignSession,
  planConnectedWindowCreation,
  planZcsungSimulationWindowCreation
} from "@doormes/application";
import { createEmptyDesign, toDesignObjectId } from "@doormes/domain";
import { requireEngineeringJointCatalogSelection } from "@doormes/engineering-joint-catalog";
import {
  normalizeWindowTopology,
  REFERENCE_WINDOW_INSTALLATION
} from "@doormes/geometry-topology";
import {
  OpeningPreviewStore,
  WindowVisualPreviewStore
} from "@doormes/interaction-core";
import { mountDesktopLayoutShell } from "@doormes/layout-shell-desktop";
import { mountMobileLayoutShell } from "@doormes/layout-shell-mobile";
import { LocalDesignSnapshotRepository } from "@doormes/persistence/local-design";
import { LocalWindowVisualHistoryRepository } from "@doormes/persistence/local-visual-history";
import {
  HttpManagedGltfAssetGateway,
  IndexedDbVisualAssetBlobStore,
  LocalManagedGltfAssetGateway
} from "@doormes/visual-asset-storage";
import { mountFactoryDrawingControls } from "./factory-drawing-controls";
import { mountProjectFileControls } from "./project-file-controls";
import { mountFactoryWorkbench } from "./factory-workbench";
import {
  REFERENCE_SIMULATION_MANUFACTURING_CATALOG,
  REFERENCE_SIMULATION_NUMBER_POLICY
} from "./reference-manufacturing-catalog";
import "./style.css";

const MOBILE_SHELL_QUERY = "(max-width: 900px), (pointer: coarse)";

/**
 * Boots the migration verification app with one shared session and one of two
 * replaceable layout shells.
 *
 * The session is intentionally created outside either shell. When the viewport
 * changes, the shell is destroyed and rebuilt while the domain document and
 * undo history remain unchanged. This is the first executable proof that PC
 * and mobile are interaction shells over the same model.
 *
 * @example Resize the browser across 900px; the layout changes but created
 * windows remain in the same `DesignSession`.
 * @since 0.1.0
 * @modified 2026-09-18 - Deferred legacy migration code until matching prototype data exists.
 */
async function bootstrap(): Promise<void> {
  const root = document.querySelector<HTMLDivElement>("#app");
  if (!root) {
    throw new Error("DoorMes cannot start because the #app mount element is missing.");
  }

  const search = new URLSearchParams(window.location.search);
  const demo = search.get("demo")?.trim() || "";
  const projectScope = demo || "default";
  const storageStatus = document.createElement("output");
  storageStatus.className = "project-storage-status";
  storageStatus.setAttribute("aria-live", "polite");
  document.body.append(storageStatus);
  const showStorageStatus = (
    state: "ready" | "warning" | "error",
    message: string
  ): void => {
    storageStatus.dataset.state = state;
    storageStatus.textContent = message;
  };
  let snapshotRepository: LocalDesignSnapshotRepository | undefined;
  let initialDocument = createEmptyDesign("DESIGN-MIGRATION-001");
  let restored = false;
  let persistenceEnabled = true;
  let initialStorageMessage = "本地设计自动保存已启用";
  try {
    snapshotRepository = new LocalDesignSnapshotRepository(
      window.localStorage,
      `doormes-formal-design-v1:${projectScope}`
    );
    const resetRequested = search.get("resetProject") === "1";
    if (resetRequested) {
      snapshotRepository.clear();
      search.delete("resetProject");
      const query = search.toString();
      window.history.replaceState(null, "", `${window.location.pathname}${query ? `?${query}` : ""}`);
      initialStorageMessage = "已清除当前场景的正式设计快照";
    } else {
      const saved = snapshotRepository.load();
      if (saved) {
        initialDocument = saved.document;
        restored = true;
        initialStorageMessage = `已恢复本地设计 · r${saved.document.revision}`;
      } else if (!demo && window.localStorage.getItem("doormes-designer-v1") !== null) {
        // The Ajv-backed legacy adapter is intentionally loaded only when the
        // prototype's real storage key exists. Normal startup therefore pays
        // only for the lightweight formal-snapshot decoder.
        const { migrateLegacyLocalStorageDesign } = await import("@doormes/persistence");
        const migration = migrateLegacyLocalStorageDesign(window.localStorage);
        if (migration) {
          const blocking = migration.issues.filter((issue) => issue.severity === "blocking");
          if (blocking.length === 0 && migration.document.windows.length > 0) {
            initialDocument = migration.document;
            restored = true;
            initialStorageMessage =
              `已迁移原型本地设计 · ${migration.issues.length}项说明`;
          } else {
            persistenceEnabled = false;
            initialStorageMessage =
              `原型本地设计含${blocking.length}项未迁移功能，已保留原数据且未自动覆盖`;
          }
        }
      }
    }
  } catch (error) {
    persistenceEnabled = false;
    initialStorageMessage = error instanceof Error
      ? `本地设计恢复失败：${error.message}`
      : "本地设计恢复失败，原存储未被覆盖";
  }
  showStorageStatus(persistenceEnabled ? "ready" : "error", initialStorageMessage);

  const session = new DesignSession(initialDocument);
  const selection = new DesignSelectionStore();
  const canvasView = new DesignCanvasViewStore();
  const configuredVisualAssetApiBaseUrl =
    import.meta.env.VITE_VISUAL_ASSET_API_BASE_URL?.trim();
  // Development defaults to the Vite-hosted filesystem API. A deployed build
  // uses an explicitly configured backend, or IndexedDB as an offline fallback.
  const visualAssetApiBaseUrl = configuredVisualAssetApiBaseUrl ||
    (import.meta.env.DEV ? "/api" : "");
  const visualAssetStore = visualAssetApiBaseUrl
    ? undefined
    : new IndexedDbVisualAssetBlobStore({
        databaseName: "doormes-designer-visual-assets-v1"
      });
  const visualAssetGateway = visualAssetApiBaseUrl
    ? new HttpManagedGltfAssetGateway({
        baseUrl: visualAssetApiBaseUrl,
        credentials: "include"
      })
    : new LocalManagedGltfAssetGateway(visualAssetStore!);
  if (!restored && search.get("demo") === "topology") {
    seedTopologyPreview(session);
  } else if (!restored && search.get("demo") === "assembly") {
    seedFabricationAssemblyPreview(session);
  } else if (!restored && search.get("demo") === "corner-assembly") {
    seedCornerFabricationAssemblyPreview(session);
  } else if (!restored && search.get("demo") === "multi-corner-assembly") {
    seedMultiCornerFabricationAssemblyPreview(session);
  } else if (!restored && search.get("demo") === "double-opening") {
    seedOpeningPreview(session, "flying");
  } else if (!restored && (search.get("demo") === "fixed-mullion" || search.get("demo") === "double-fixed")) {
    seedOpeningPreview(session, "fixed");
  } else if (!restored && search.get("demo") === "opening") {
    seedOpeningPreview(session, "single");
  } else if (!restored && search.get("demo") === "top-hung") {
    seedTopHungPreview(session);
  } else if (!restored && search.get("demo") === "zcsung-120-inward-tilt") {
    seedZcsungProductTemplate(session, "ZCSUNG-SIM-LH-120-TT");
  } else if (!restored && search.get("demo") === "zcsung-100-outward") {
    seedZcsungProductTemplate(session, "ZCSUNG-SIM-YK-100-OUT");
  } else if (!restored && search.get("demo") === "zcsung-95s-double-inward") {
    seedZcsungProductTemplate(session, "ZCSUNG-SIM-YP-95S-DOUBLE-IN");
  }
  let persistenceFailed = !persistenceEnabled;
  const disposePersistence = session.subscribe((document) => {
    if (!snapshotRepository || persistenceFailed) return;
    try {
      snapshotRepository.save(document);
      showStorageStatus(
        "ready",
        restored
          ? `${initialStorageMessage} · 自动保存已启用`
          : `本地设计已自动保存 · r${document.revision}`
      );
      restored = false;
    } catch (error) {
      persistenceFailed = true;
      showStorageStatus(
        "error",
        error instanceof Error ? `本地设计保存失败：${error.message}` : "本地设计保存失败"
      );
    }
  });
  let visualHistoryRepository: LocalWindowVisualHistoryRepository | undefined;
  let visualHistoryFailed = false;
  try {
    visualHistoryRepository = new LocalWindowVisualHistoryRepository(
      window.localStorage,
      `doormes-window-visual-history-v1:${projectScope}`
    );
  } catch (error) {
    visualHistoryFailed = true;
    showStorageStatus(
      "warning",
      error instanceof Error ? `本地型号历史不可用：${error.message}` : "本地型号历史不可用"
    );
  }
  const disposeVisualHistory = session.subscribe((document) => {
    if (!visualHistoryRepository || visualHistoryFailed) return;
    try {
      visualHistoryRepository.captureDocument(document);
    } catch (error) {
      visualHistoryFailed = true;
      showStorageStatus(
        "warning",
        error instanceof Error
          ? `本地型号历史停止记录，原数据未覆盖：${error.message}`
          : "本地型号历史停止记录，原数据未覆盖"
      );
    }
  });
  const openingPreview = new OpeningPreviewStore(session.document);
  const visualPreview = new WindowVisualPreviewStore();
  const shellOverride = search.get("shell");
  const media = window.matchMedia(MOBILE_SHELL_QUERY);
  let disposeShell: (() => void) | undefined;
  let disposeProjectFileControls: (() => void) | undefined;
  let disposeFactoryDrawingControls: (() => void) | undefined;

  const mountCurrentShell = (): void => {
    disposeFactoryDrawingControls?.();
    disposeProjectFileControls?.();
    disposeShell?.();
    root.replaceChildren();
    const useMobileShell = shellOverride === "mobile"
      ? true
      : shellOverride === "desktop"
        ? false
        : media.matches;
    disposeShell = useMobileShell
      ? mountMobileLayoutShell(
          root,
          session,
          selection,
          canvasView,
          openingPreview,
          visualPreview,
          { gateway: visualAssetGateway, history: visualHistoryRepository }
        )
      : mountDesktopLayoutShell(
          root,
          session,
          selection,
          canvasView,
          openingPreview,
          visualPreview,
          { gateway: visualAssetGateway, history: visualHistoryRepository }
        );
    const header = root.querySelector<HTMLElement>(".shell__header");
    if (!header) {
      throw new Error("DoorMes shell is missing its project-file action region.");
    }
    disposeProjectFileControls = mountProjectFileControls(
      header,
      session,
      showStorageStatus
    );
    disposeFactoryDrawingControls = mountFactoryDrawingControls(
      header,
      session,
      selection,
      showStorageStatus,
      async (document) => {
        const { calculateFormalBom } = await import("@doormes/calculation-engine");
        return {
          sourceRevision: document.revision,
          result: calculateFormalBom(document, REFERENCE_SIMULATION_MANUFACTURING_CATALOG, {
            plannedNumberPolicy: REFERENCE_SIMULATION_NUMBER_POLICY
          })
        };
      }
    );
  };

  // Manual QA may pin one shell with `?shell=desktop|mobile`; ordinary users
  // retain automatic switching and shared session preservation at breakpoints.
  if (shellOverride !== "desktop" && shellOverride !== "mobile") {
    media.addEventListener("change", mountCurrentShell);
  }
  mountCurrentShell();
  const disposeFactoryWorkbench = mountFactoryWorkbench(root, session, () => {
    selection.select(undefined, "system");
    canvasView.resetTransform();
  });
  window.addEventListener("pagehide", () => {
    disposeFactoryWorkbench();
    disposeFactoryDrawingControls?.();
    disposeProjectFileControls?.();
    disposeShell?.();
    disposePersistence();
    disposeVisualHistory();
    storageStatus.remove();
    void visualAssetStore?.dispose();
  }, { once: true });
}

/**
 * Seeds a connected two-unit factory product for ASSEMBLY-001 visual review.
 *
 * The 30mm centre zone is an engineering joint, not a blank wall strip. Both
 * units therefore render under one assembly root and one optional reference
 * wall opening. This fixture enters through the same public commands used by
 * persistence and future creation tools.
 *
 * @example Open `/?demo=assembly&shell=desktop&resetProject=1`.
 * @since 0.10.46
 * @modified 2026-09-20 - Added human-reviewable connected-product projection.
 */
function seedFabricationAssemblyPreview(session: DesignSession): void {
  session.execute(createRectangularWindowCommand({
    commandId: "CMD-PREVIEW-ASSEMBLY-WINDOW-1",
    windowId: "W-PREVIEW-ASSEMBLY-1",
    mark: "组合左窗",
    widthMm: 1200,
    heightMm: 1500
  }));
  session.execute(createRectangularWindowCommand({
    commandId: "CMD-PREVIEW-ASSEMBLY-WINDOW-2",
    windowId: "W-PREVIEW-ASSEMBLY-2",
    mark: "组合右窗",
    widthMm: 900,
    heightMm: 1500
  }));
  session.execute(createFabricationAssemblyCommand({
    commandId: "CMD-PREVIEW-ASSEMBLY-CREATE",
    assemblyId: "A-PREVIEW-CONNECTED",
    mark: "两联组合窗预览",
    instances: [
      {
        objectId: "A-PREVIEW-CONNECTED:I1",
        windowId: "W-PREVIEW-ASSEMBLY-1",
        transform: { xMm: 0, yMm: 0, zMm: 0, rotationYDeg: 0 }
      },
      {
        objectId: "A-PREVIEW-CONNECTED:I2",
        windowId: "W-PREVIEW-ASSEMBLY-2",
        transform: { xMm: 1230, yMm: 0, zMm: 0, rotationYDeg: 0 }
      }
    ],
    joints: [{
      objectId: "A-PREVIEW-CONNECTED:J1",
      jointType: "mullion_joint",
      firstInstanceId: "A-PREVIEW-CONNECTED:I1",
      firstEdge: "right",
      secondInstanceId: "A-PREVIEW-CONNECTED:I2",
      secondEdge: "left",
      gapMm: 30,
      factoryScope: "factory"
    }],
    openingClearance: { topMm: 10, rightMm: 12, bottomMm: 15, leftMm: 12 },
    installation: {
      ...REFERENCE_WINDOW_INSTALLATION,
      sillHeightMm: 900,
      surround: {
        ...REFERENCE_WINDOW_INSTALLATION.surround,
        enabled: true,
        wallMaterialId: "red_brick",
        materialCode: "SURROUND-AL-01"
      }
    }
  }));
}

/** Seeds a real 90-degree factory corner with unfolded and spatial projections. */
function seedCornerFabricationAssemblyPreview(session: DesignSession): void {
  session.execute(createRectangularWindowCommand({
    commandId: "CMD-PREVIEW-CORNER-WINDOW-1",
    windowId: "W-PREVIEW-CORNER-1",
    mark: "K1",
    widthMm: 1200,
    heightMm: 1500
  }));
  session.execute(createRectangularWindowCommand({
    commandId: "CMD-PREVIEW-CORNER-WINDOW-2",
    windowId: "W-PREVIEW-CORNER-2",
    mark: "K2",
    widthMm: 900,
    heightMm: 1500
  }));
  const catalogSelection = requireEngineeringJointCatalogSelection(
    "DM-JOINT-CORNER-70",
    "2026.09-r1"
  );
  session.execute(createFabricationAssemblyCommand({
    commandId: "CMD-PREVIEW-CORNER-CREATE",
    assemblyId: "A-PREVIEW-CORNER",
    mark: "90度转角组合窗",
    instances: [
      {
        objectId: "A-PREVIEW-CORNER:I1",
        windowId: "W-PREVIEW-CORNER-1",
        transform: { xMm: 0, yMm: 0, zMm: 0, rotationYDeg: 0 }
      },
      {
        objectId: "A-PREVIEW-CORNER:I2",
        windowId: "W-PREVIEW-CORNER-2",
        transform: { xMm: 1235, yMm: 0, zMm: -35, rotationYDeg: 90 }
      }
    ],
    joints: [{
      objectId: "A-PREVIEW-CORNER:J1",
      jointType: "corner_joint",
      firstInstanceId: "A-PREVIEW-CORNER:I1",
      firstEdge: "right",
      secondInstanceId: "A-PREVIEW-CORNER:I2",
      secondEdge: "left",
      gapMm: 70,
      factoryScope: "factory",
      cornerConfiguration: {
        schemaVersion: "doormes-engineering-corner-joint.v1",
        includedAngleDeg: 90,
        turnDirection: "clockwise"
      },
      catalogSelection
    }],
    openingClearance: { topMm: 10, rightMm: 12, bottomMm: 15, leftMm: 12 },
    installation: REFERENCE_WINDOW_INSTALLATION
  }));
}

/** Seeds a three-window bay-style chain with two independently editable corners. */
function seedMultiCornerFabricationAssemblyPreview(session: DesignSession): void {
  session.execute(createRectangularWindowCommand({
    commandId: "CMD-PREVIEW-MULTI-CORNER-WINDOW-1",
    windowId: "W-PREVIEW-MULTI-CORNER-1",
    mark: "B1",
    widthMm: 1200,
    heightMm: 1500
  }));
  session.execute(createUpdateWindowInstallationCommand({
    commandId: "CMD-PREVIEW-MULTI-CORNER-INSTALLATION",
    windowId: "W-PREVIEW-MULTI-CORNER-1",
    installation: {
      ...REFERENCE_WINDOW_INSTALLATION,
      surround: {
        ...REFERENCE_WINDOW_INSTALLATION.surround,
        enabled: true,
        styleId: "both_sides",
        edgeMode: "all",
        sides: ["top", "right", "bottom", "left"]
      }
    }
  }));
  const catalogSelection = requireEngineeringJointCatalogSelection(
    "DM-JOINT-CORNER-70",
    "2026.09-r1"
  );
  const second = planConnectedWindowCreation({
    document: session.document,
    createWindowCommand: createRectangularWindowCommand({
      commandId: "CMD-PREVIEW-MULTI-CORNER-WINDOW-2",
      windowId: "W-PREVIEW-MULTI-CORNER-2",
      mark: "B2",
      widthMm: 900,
      heightMm: 1500
    }),
    anchorWindowId: "W-PREVIEW-MULTI-CORNER-1",
    direction: "corner",
    gapMm: 70,
    catalogSelection,
    cornerIncludedAngleDeg: 120,
    cornerTurnDirection: "clockwise",
    transactionId: "CMD-PREVIEW-MULTI-CORNER-CONNECT-2"
  });
  session.executeTransaction(second.commands, "CMD-PREVIEW-MULTI-CORNER-CONNECT-2");
  const third = planConnectedWindowCreation({
    document: session.document,
    createWindowCommand: createRectangularWindowCommand({
      commandId: "CMD-PREVIEW-MULTI-CORNER-WINDOW-3",
      windowId: "W-PREVIEW-MULTI-CORNER-3",
      mark: "B3",
      widthMm: 1000,
      heightMm: 1500
    }),
    anchorWindowId: "W-PREVIEW-MULTI-CORNER-2",
    direction: "corner",
    gapMm: 70,
    catalogSelection,
    cornerIncludedAngleDeg: 120,
    cornerTurnDirection: "clockwise",
    transactionId: "CMD-PREVIEW-MULTI-CORNER-CONNECT-3"
  });
  session.executeTransaction(third.commands, "CMD-PREVIEW-MULTI-CORNER-CONNECT-3");
}

/**
 * Seeds the verification application with a deterministic T-junction window.
 *
 * This is an opt-in migration preview selected by `?demo=topology`; it is not a
 * production default or a second design model. The sample enters through the
 * same command pipeline as user-created PC/mobile windows, so switching shells
 * preserves exactly the same topology IDs and SVG projection.
 *
 * @param session Shared application session created by the composition root.
 * @example Open `/?demo=topology` to inspect the current M4 topology rendering.
 * @since 0.3.2
 * @modified 2026-09-17 - Added an auditable manual topology preview.
 */
function seedTopologyPreview(session: DesignSession): void {
  const cellId = toDesignObjectId("R-PREVIEW-TEE");
  const layout = {
    columns: [1],
    rows: [1],
    cells: [{ objectId: cellId, type: "fixed_glass" as const, opening: "fixed" as const }]
  };
  const topology = normalizeWindowTopology(undefined, layout);
  session.execute(
    createRectangularWindowCommand({
      commandId: "CMD-PREVIEW-TEE",
      windowId: "W-PREVIEW-TEE",
      mark: "T形拓扑预览",
      widthMm: 1200,
      heightMm: 1500,
      geometryMode: "topology",
      layout,
      topology: {
        ...topology,
        members: [
          {
            objectId: toDesignObjectId("M-PREVIEW-V"),
            role: "mullion",
            orientation: "vertical",
            hostRegionId: cellId,
            positionRatio: 0.5,
            span: { startRatio: 0, endRatio: 0.5 },
            profileId: "AL70-Z01",
            throughMode: "local",
            connectionStart: "butt",
            connectionEnd: "through",
            note: "T形上半竖梃"
          },
          {
            objectId: toDesignObjectId("M-PREVIEW-H"),
            role: "mullion",
            orientation: "horizontal",
            hostRegionId: cellId,
            positionRatio: 0.5,
            span: { startRatio: 0, endRatio: 1 },
            profileId: "AL70-Z02",
            throughMode: "continuous",
            connectionStart: "butt",
            connectionEnd: "butt",
            note: "T形贯通横梃"
          }
        ]
      }
    })
  );
}

/**
 * Seeds the first complete opening/sash/glass/hardware verification scenario.
 *
 * The sample deliberately executes the same two commands exposed by desktop
 * and mobile: create a fixed window, then change its stable cell to a right-in
 * tilt-turn assembly. This keeps the preview honest—undo returns to fixed glass,
 * while the current revision drives shared 2D, 3D and formal BOM rules.
 *
 * @param session Shared application session created by the composition root.
 * @param mode Single, ordered flying-mullion, or independent fixed-mullion variant.
 * @example Open `/?demo=fixed-mullion` to inspect the frame-owned central member.
 * @since 0.4.9
 * @modified 2026-09-17 - Added the human-reviewable fixed-mullion product variant.
 */
function seedOpeningPreview(
  session: DesignSession,
  mode: "single" | "flying" | "fixed"
): void {
  const doubleSash = mode !== "single";
  session.execute(
    createRectangularWindowCommand({
      commandId: "CMD-PREVIEW-OPENING-CREATE",
      windowId: "W-PREVIEW-OPENING",
      mark: "C1",
      widthMm: doubleSash ? 1600 : 1200,
      heightMm: 1500,
      cellId: "CELL-PREVIEW-OPENING",
      profileSystemId: "AL70",
      frameFaceMm: 70,
      sashFaceMm: 58,
      defaultHardwareSetId: "HW-TT-STD"
    })
  );
  session.execute(
    createSetWindowCellOpeningCommand({
      commandId: "CMD-PREVIEW-OPENING-SET",
      windowId: "W-PREVIEW-OPENING",
      cellId: "CELL-PREVIEW-OPENING",
      cellType: "turn_tilt",
      opening: "right_in",
      panelCount: doubleSash ? 2 : 1,
      mullionMode: mode === "flying" ? "flying_mullion" : "fixed_mullion",
      hardwareSetId: "HW-TT-STD"
    })
  );
}

/**
 * Instantiates one target-customer public-reference simulation through shared commands.
 *
 * The catalogue owns product/source identity and neutral assumptions; this
 * composition root only translates its executable preset into the same create
 * and opening commands used by normal PC/mobile interaction. The short visible
 * mark remains an editable business number; non-production provenance and the
 * public product name stay in the separate template snapshot and notice panel.
 *
 * Algorithm: resolve an exact fail-closed catalogue preset, create a neutral
 * rectangular window, then configure its cell motion and maximum angle. A
 * planned sliding/lift/special-motion template throws before any design change.
 *
 * @param session Shared session used by both desktop and mobile shells.
 * @param templateId Stable ZCSUNG public-reference template identity.
 * @example Open `/?demo=zcsung-120-inward-tilt&resetProject=1`.
 * @since 0.10.76
 * @modified 2026-09-22 - Added the first target-customer simulation entry points.
 */
function seedZcsungProductTemplate(session: DesignSession, templateId: string): void {
  const stableSuffix = templateId.replace(/^ZCSUNG-(?:SIM|PLAN)-/, "");
  const windowId = `W-ZCSUNG-SIM-${stableSuffix}`;
  const plan = planZcsungSimulationWindowCreation({
    templateId,
    commandIdPrefix: `CMD-${windowId}`,
    windowId
  });
  session.executeTransaction(
    plan.commands,
    `CMD-${windowId}:CREATE-FROM-TEMPLATE`
  );
}

/**
 * Seeds the first horizontal-axis opening for human 2D/3D/BOM review.
 *
 * The sample is created through the same fixed-window and cell-construction
 * commands used by both interaction shells. Its 1200×1500 dimensions match the
 * frozen prototype fixture, while `top_out` makes the free bottom rail move
 * toward exterior +Z in Three.js.
 *
 * @param session Shared application session used by the selected shell.
 * @example Open `/?demo=top-hung&shell=desktop` to inspect the full slice.
 * @since 0.7.0
 * @modified 2026-09-17 - Added the top-hung manual acceptance scenario.
 */
function seedTopHungPreview(session: DesignSession): void {
  session.execute(
    createRectangularWindowCommand({
      commandId: "CMD-PREVIEW-TOP-HUNG-CREATE",
      windowId: "W-PREVIEW-TOP-HUNG",
      mark: "上悬外开预览",
      widthMm: 1200,
      heightMm: 1500,
      cellId: "CELL-PREVIEW-TOP-HUNG",
      profileSystemId: "AL70",
      frameFaceMm: 70,
      sashFaceMm: 58,
      defaultHardwareSetId: "HW-HUNG-STD"
    })
  );
  session.execute(
    createSetWindowCellOpeningCommand({
      commandId: "CMD-PREVIEW-TOP-HUNG-SET",
      windowId: "W-PREVIEW-TOP-HUNG",
      cellId: "CELL-PREVIEW-TOP-HUNG",
      cellType: "top_hung",
      opening: "top_out",
      panelCount: 1,
      mullionMode: "fixed_mullion",
      hardwareSetId: "HW-HUNG-STD"
    })
  );
}

void bootstrap();
