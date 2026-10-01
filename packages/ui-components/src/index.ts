import {
  createAddWindowTopologyMemberCommand,
  createDeleteDrawingTextLabelCommand,
  createDrawingTextLabelCommand,
  createDeleteWindowCommand,
  createEqualizeWindowGridCommand,
  createMergeWindowGridDividerCommand,
  createMoveOpeningMeetingMullionCommand,
  createMoveWindowGridDividerCommand,
  createMoveWindowTopologyMemberCommand,
  createRemoveLastWindowGridTrackCommand,
  createRemoveWindowTopologyMemberCommand,
  createResizeWindowCommand,
  createSetWindowCellOpeningCommand,
  createStandardSlidingConfiguration,
  createSplitWindowGridCommand,
  createUpdateFabricationAssemblyInstallationCommand,
  createUpdateWindowInstallationCommand,
  createUpdateEngineeringJointCommand,
  createUpdateDrawingTextLabelCommand,
  createUpdateFactoryDrawingElementOptionsCommand,
  findEngineeringJointCatalogSelection,
  listEngineeringJointCatalogSelections,
  requireEngineeringJointCatalogSelection,
  createUpdateWindowTopologyMemberCommand,
  createUpdateWindowSurroundCatalogSelectionCommand,
  createUpdateWindowDesignComponentRemarksCommand,
  createUpdateWindowMarkCommand,
  createUpdateWindowVisualConfigurationCommand,
  type DesignCanvasViewState,
  type DesignCanvasViewStore,
  type DesignSelectionStore,
  type DesignSession
} from "@doormes/application";
import {
  applyAppearanceCatalogPreset,
  applyHardwareModelCatalogPreset,
  createManagedGltfGeometrySnapshot,
  diagnoseWindowVisualAssets,
  listAppearanceCatalogPresets,
  listHardwareModelCatalogPresets,
  listSurroundBusinessCatalogPresets,
  replaceWindowAppearanceEditorSlot,
  replaceWindowHardwareInstanceModel,
  replaceWindowHardwareRoleModel,
  removeWindowHardwareInstanceModel,
  resolveHardwareComponentModel,
  resolveWindowAppearanceEditorSlot,
  resolveWindowVisualConfigurationForRender,
  WINDOW_APPEARANCE_EDITOR_SLOTS,
  type WindowAppearanceEditorSlot
} from "@doormes/appearance-model";
import type {
  AppearanceMaterialFamily,
  ComponentAssetAxis,
  ComponentAssetSourceUnit,
  ComponentModelMountSnapshot,
  DesignDocument,
  DesignObjectId,
  DrawingTextLabel,
  OpeningHardwareRole,
  ParametricComponentGeometrySnapshot,
  WindowCell,
  WindowTopologyMember,
  WindowUnit,
  WindowVisualConfiguration
} from "@doormes/contracts";
import {
  createWindowInstallationSurroundObjectId,
  createWindowInstallationWallObjectId,
  normalizeWindowInstallation,
  resolveFabricationAssemblyGeometry,
  resolveWindowGeometry
} from "@doormes/geometry-topology";
import { TouchCanvasGestureController } from "@doormes/interaction-touch";
import { createOpeningPanelKey } from "@doormes/opening-kinematics";
import {
  collectOpeningPreviewPanelDefinitions,
  resolveCanvasInteractionHit,
  resolveCanvasInteractionIntent,
  OpeningPreviewController,
  resolveOpeningPreviewAngleDegrees,
  resolveOpeningPreviewMaximumAngleDegrees,
  type CanvasInteractionHit,
  type OpeningPreviewState,
  type OpeningPreviewStore,
  type WindowVisualPreviewStore
} from "@doormes/interaction-core";
import { renderDesignSvg } from "@doormes/renderer-svg";
import {
  loadManagedGltfComponentAsset,
  ThreeDesignViewport,
  ThreeVisualAssetRegistry,
  type ThreeVisualAssetQuality
} from "@doormes/renderer-three";
import {
  calculateVisualAssetContentHash,
  type ManagedGltfAssetGateway,
  type ManagedGltfUploadEvidence
} from "@doormes/visual-asset-storage";

/** Identifies the shared centre-workspace projection currently visible. */
type WorkspaceView = "2d" | "3d";

/**
 * Construction actions exposed by the current desktop 2D toolbar.
 *
 * Values are presentation vocabulary only; `executeGridAction` converts each
 * one to a versioned application command before model data can change.
 *
 * @example `split-column-3` becomes a 3-part `SplitWindowGridCommand`.
 * @since 0.4.3
 * @modified 2026-09-17 - Added local members, equalization and triple splits.
 */
type GridAction =
  | "split-column"
  | "split-row"
  | "split-column-3"
  | "split-row-3"
  | "remove-column"
  | "remove-row"
  | "equalize"
  | "local-vertical"
  | "local-horizontal";

/**
 * Shared construction-tool metadata rendered into desktop and mobile shells.
 *
 * Labels live beside action IDs so the two interaction shells cannot drift or
 * silently expose different topology commands. The action is still translated
 * by `executeGridAction`; this table contains no geometry or BOM rules.
 *
 * @example `local-vertical` renders as “局部竖梃” in both tool surfaces.
 * @since 0.4.6
 * @modified 2026-09-17 - Added one source for desktop/mobile drawing tools.
 */
const GRID_TOOL_DEFINITIONS: readonly Readonly<{
  action: GridAction;
  label: string;
}>[] = [
  { action: "split-column", label: "贯通竖梃" },
  { action: "split-row", label: "贯通横梃" },
  { action: "local-vertical", label: "局部竖梃" },
  { action: "local-horizontal", label: "局部横梃" },
  { action: "split-column-3", label: "竖向三等分" },
  { action: "split-row-3", label: "横向三等分" },
  { action: "equalize", label: "全部均分" },
  { action: "remove-column", label: "删除末列" },
  { action: "remove-row", label: "删除末行" }
];

/**
 * Creates accessible construction buttons from shared, trusted metadata.
 *
 * DOM properties are assigned directly instead of interpolating HTML. Calling
 * this for the desktop toolbar and mobile drawer guarantees matching action
 * IDs while allowing each shell to arrange the buttons independently.
 *
 * @param target Empty toolbar or drawer container owned by a layout shell.
 * @example `populateGridToolButtons(sidebar)` creates all nine current tools.
 * @since 0.4.6
 * @modified 2026-09-17 - Centralized drawing-tool presentation metadata.
 */
function populateGridToolButtons(target: HTMLElement): void {
  const fragment = document.createDocumentFragment();
  for (const definition of GRID_TOOL_DEFINITIONS) {
    const button = document.createElement("button");
    button.type = "button";
    button.dataset.gridAction = definition.action;
    button.textContent = definition.label;
    fragment.append(button);
  }
  target.replaceChildren(fragment);
}

/**
 * Narrows untrusted DOM button metadata to supported construction actions.
 *
 * @param value `data-grid-action` read from an event target.
 * @returns True only for a command handled by `executeGridAction`.
 * @example `isGridAction("equalize")` is true; `isGridAction("save")` is false.
 * @since 0.4.3
 * @modified 2026-09-17 - Centralized toolbar action validation.
 */
function isGridAction(value: string | undefined): value is GridAction {
  return [
    "split-column",
    "split-row",
    "split-column-3",
    "split-row-3",
    "remove-column",
    "remove-row",
    "equalize",
    "local-vertical",
    "local-horizontal"
  ].includes(value ?? "");
}

/**
 * Presentation-only state for one in-progress through-divider drag.
 *
 * The SVG element receives a temporary transform during pointer movement; the
 * final millimetre coordinate is committed once on pointer-up and this state is
 * discarded, preserving a single undo boundary.
 *
 * @example A vertical gesture stores `axis: "column"` and its boundary index.
 * @since 0.4.2
 * @modified 2026-09-20 - Added threshold activation so a click never commits a drag command.
 */
interface DividerDragState {
  readonly pointerId: number;
  readonly element: SVGRectElement;
  readonly windowId: string;
  readonly objectId: string;
  readonly axis: "column" | "row";
  readonly index: number;
  readonly startAxisMm: number;
  currentAxisMm: number;
  readonly renderScale: number;
  readonly startClientX: number;
  readonly startClientY: number;
  activated: boolean;
}

/**
 * Presentation-only state for dragging an explicit topology member.
 *
 * Local SVG coordinates are retained because the displayed host cell is inside
 * the outer frame. On pointer-up they are converted to a host-relative ratio,
 * then to the prototype-compatible host millimetres accepted by the command.
 *
 * @example Dragging a vertical local mullion tracks its local x coordinate.
 * @since 0.4.4
 * @modified 2026-09-20 - Added threshold activation before the single-command drag preview.
 */
interface TopologyMemberDragState {
  readonly pointerId: number;
  readonly element: SVGRectElement;
  readonly windowId: string;
  readonly objectId: string;
  readonly orientation: "vertical" | "horizontal";
  readonly startAxisMm: number;
  currentAxisMm: number;
  readonly renderScale: number;
  readonly startClientX: number;
  readonly startClientY: number;
  activated: boolean;
}

/**
 * Presentation-only state for a sash-owned flying-mullion drag.
 *
 * Movement is previewed on the SVG rectangle and committed once on release as
 * a host-relative millimetre command. The model then recalculates both panel
 * envelopes, hardware, 3D and BOM from one meeting-position ratio.
 *
 * @example Dragging from 730mm to 620mm creates one command at pointer-up.
 * @since 0.5.2
 * @modified 2026-09-20 - Added threshold activation while preserving the independent selection ID.
 */
interface MeetingMullionDragState {
  readonly pointerId: number;
  readonly element: SVGRectElement;
  readonly windowId: string;
  readonly cellId: string;
  /** Stable generated member ID used consistently by SVG, Three.js and the tree. */
  readonly objectId: string;
  readonly startClientX: number;
  readonly startClientY: number;
  readonly startPositionMm: number;
  currentPositionMm: number;
  readonly pixelsPerMm: number;
  readonly renderScale: number;
  activated: boolean;
}

/** Temporary SVG state for dragging one spatial corner joint's plan-view angle. */
interface CornerAngleDragState {
  readonly pointerId: number;
  readonly group: SVGGElement;
  readonly assemblyId: string;
  readonly jointId: string;
  readonly turnDirection: "clockwise" | "counterclockwise";
  readonly axisX: number;
  readonly axisY: number;
  readonly firstRayAngleRadians: number;
  readonly radius: number;
  readonly minimumAngleDeg: number;
  readonly maximumAngleDeg: number;
  readonly startAngleDeg: number;
  currentAngleDeg: number;
  activated: boolean;
}

/** Temporary state for moving one user-authored label in its owner-local plane. */
interface DrawingTextLabelDragState {
  readonly pointerId: number;
  readonly element: SVGGElement;
  readonly labelId: string;
  readonly startClientX: number;
  readonly startClientY: number;
  readonly startXMm: number;
  readonly startYMm: number;
  readonly startSvgX: number;
  readonly startSvgY: number;
  readonly pixelsPerMm: number;
  readonly renderScale: number;
  currentXMm: number;
  currentYMm: number;
  activated: boolean;
}

/**
 * Temporary presentation state for a threshold-gated canvas pan.
 *
 * The origin is captured on pointer-down, but `activated` stays false until the
 * shared client-pixel threshold is crossed. This keeps a blank-area click from
 * moving the view.
 *
 * @example A 4px movement remains a click while a 6px movement starts panning.
 * @since 0.10.28
 * @modified 2026-09-20 - Added shared click-versus-pan arbitration.
 */
interface CanvasPanState {
  readonly pointerId: number;
  readonly startClientX: number;
  readonly startClientY: number;
  readonly originX: number;
  readonly originY: number;
  activated: boolean;
}

/**
 * Shared semantic press retained until release for threshold decisions.
 *
 * The original semantic hit is deliberately stable for the entire gesture, so
 * moving across another object cannot change a pending divider drag into canvas
 * panning or object selection.
 *
 * @example A press starting on `MULLION-1` keeps that hit after leaving its SVG rect.
 * @since 0.10.28
 * @modified 2026-09-20 - Added stable per-pointer semantic gesture state.
 */
interface CanvasPointerPressState {
  readonly hit: CanvasInteractionHit;
  readonly startClientX: number;
  readonly startClientY: number;
  activated: boolean;
}

/**
 * DOM elements plus the single semantic hit chosen by the shared resolver.
 *
 * DOM references support presentation-only previews and popovers. The semantic
 * member is the only value used for interaction arbitration.
 *
 * @example A dimension nested in a cell retains both DOM nodes but resolves as a dimension.
 * @since 0.10.28
 * @modified 2026-09-20 - Separated SVG adaptation from shared interaction meaning.
 */
interface DomCanvasInteractionHit {
  readonly semantic: CanvasInteractionHit;
  readonly dragHandle?: SVGElement;
  readonly dimension?: SVGElement;
  readonly object?: SVGElement;
  readonly contextMember?: SVGElement;
}

/**
 * Resolves a visible drag grip back to the real SVG member it controls.
 *
 * The renderer deliberately keeps the small grip separate from the member so
 * it remains legible on a narrow profile. This adapter scans only the current
 * SVG and matches the trusted stable ID; the downstream drag code therefore
 * continues to read orientation, grid index and host metadata from the member
 * rectangle. Clicking the member body follows the zero-scan fast path.
 *
 * @param target Browser event target inside the SVG.
 * @returns The grid/topology/meeting member that owns the gesture, if any.
 * @example A grip with `data-drag-owner-id="M-1"` resolves the M-1 rectangle.
 * @since 0.10.30
 * @modified 2026-09-20 - Added explicit top-right member drag affordances.
 */
function resolveDomDragOwner(target: Element): SVGElement | undefined {
  const candidate = target.closest<SVGElement>(
    "[data-drag-owner-id], .design-window__member--grid-divider, .design-window__meeting-mullion, .design-window__member--topology-member, .design-text-label"
  ) ?? undefined;
  const ownerId = candidate?.dataset.dragOwnerId;
  if (!candidate || !ownerId) return candidate;
  const svg = candidate.ownerSVGElement;
  if (!svg) return undefined;
  return [...svg.querySelectorAll<SVGElement>(
    ".design-window__member--grid-divider, .design-window__meeting-mullion, .design-window__member--topology-member, .design-text-label"
  )].find((element) => element.dataset.objectId === ownerId);
}

/**
 * Adapts overlapping SVG nodes to the renderer-neutral hit-test contract.
 *
 * The adapter only reads trusted `data-*` projection metadata. Priority and
 * activation meaning remain in `interaction-core`, so desktop and mobile use
 * the same arbitration even though their property panels are presented
 * differently.
 *
 * @param target Browser event target inside or outside the SVG surface.
 * @returns DOM references needed for previews plus one semantic shared hit.
 * @example A divider rectangle wins over its owning cell's wide hit region.
 * @since 0.10.28
 * @modified 2026-09-20 - Added grouped dimension owners and explicit drag grips.
 */
function resolveDomCanvasInteractionHit(target: EventTarget | null): DomCanvasInteractionHit {
  if (!(target instanceof Element)) {
    return { semantic: resolveCanvasInteractionHit({}) };
  }
  const cornerAngleHandle = target.closest<SVGElement>("[data-corner-angle-handle]") ?? undefined;
  const dragHandle = cornerAngleHandle ?? resolveDomDragOwner(target);
  const dimension = target.closest<SVGElement>("[data-dimension-kind]") ?? undefined;
  const object = target.closest<SVGElement>("[data-object-id]") ?? undefined;
  const contextMember = target.closest<SVGElement>("[data-member-kind]") ?? undefined;
  const contextMemberKind = contextMember?.dataset.memberKind;
  const supportsMemberContextMenu =
    contextMemberKind === "grid-divider" ||
    contextMemberKind === "topology-member" ||
    contextMemberKind === "drawing-label";
  return {
    semantic: resolveCanvasInteractionHit({
      ...(dragHandle?.dataset.objectId
        ? { dragHandleObjectId: dragHandle.dataset.objectId }
        : {}),
      dragHandleSupportsContextMenu: supportsMemberContextMenu,
      ...(dimension?.dataset.objectId
        ? { dimensionObjectId: dimension.dataset.objectId }
        : {}),
      ...(dimension?.dataset.dimensionKind
        ? { dimensionKind: dimension.dataset.dimensionKind }
        : {}),
      ...(object?.dataset.objectId ? { objectId: object.dataset.objectId } : {})
    }),
    ...(dragHandle ? { dragHandle } : {}),
    ...(dimension ? { dimension } : {}),
    ...(object ? { object } : {}),
    ...(contextMember ? { contextMember } : {})
  };
}

/**
 * Converts a horizontal client-pixel gesture into model millimetres and an SVG preview offset.
 *
 * The captured `pixelsPerMm` already includes the SVG viewBox, canvas zoom and
 * window render scale through `getScreenCTM()`. Dividing by the render scale a
 * second time would amplify movement when the window is fitted to the canvas.
 * This pure calculation is shared by mouse and touch Pointer Events.
 *
 * @param input Pointer position plus the scale snapshot captured at pointer-down.
 * @returns Signed model delta, next host-relative position and SVG translation.
 * @throws RangeError when scale values cannot describe a visible SVG element.
 * @example 20px at 0.2px/mm moves 100mm and a 0.25 render scale previews 25 SVG units.
 * @since 0.5.2
 * @modified 2026-09-17 - Extracted screen-scale-safe flying-mullion drag math.
 */
export function calculateMeetingMullionDrag(input: Readonly<{
  clientX: number;
  startClientX: number;
  startPositionMm: number;
  pixelsPerMm: number;
  renderScale: number;
}>): Readonly<{ deltaMm: number; positionMm: number; svgDelta: number }> {
  if (
    !Number.isFinite(input.clientX) ||
    !Number.isFinite(input.startClientX) ||
    !Number.isFinite(input.startPositionMm) ||
    !Number.isFinite(input.pixelsPerMm) ||
    input.pixelsPerMm <= 0 ||
    !Number.isFinite(input.renderScale) ||
    input.renderScale <= 0
  ) {
    throw new RangeError("Flying-mullion drag scales must be finite positive numbers.");
  }
  const deltaMm = (input.clientX - input.startClientX) / input.pixelsPerMm;
  return {
    deltaMm,
    positionMm: input.startPositionMm + deltaMm,
    svgDelta: deltaMm * input.renderScale
  };
}

/**
 * Converts one plan-view pointer into a catalog-clamped corner angle preview.
 *
 * SVG uses a downward-positive Y axis, so the stored business turn direction
 * deliberately controls the signed sweep instead of relying on the shortest
 * angle. This keeps a clockwise and counterclockwise corner stable while the
 * pointer crosses the 0/360-degree boundary.
 *
 * @since 0.10.90
 */
export function calculateCornerAngleDrag(input: Readonly<{
  axisX: number;
  axisY: number;
  pointerX: number;
  pointerY: number;
  firstRayAngleRadians: number;
  turnDirection: "clockwise" | "counterclockwise";
  minimumAngleDeg: number;
  maximumAngleDeg: number;
  radius: number;
}>): Readonly<{
  includedAngleDeg: number;
  endX: number;
  endY: number;
  labelX: number;
  labelY: number;
  sweepFlag: 0 | 1;
}> {
  const values = [
    input.axisX,
    input.axisY,
    input.pointerX,
    input.pointerY,
    input.firstRayAngleRadians,
    input.minimumAngleDeg,
    input.maximumAngleDeg,
    input.radius
  ];
  if (
    values.some((value) => !Number.isFinite(value)) ||
    input.minimumAngleDeg <= 0 ||
    input.maximumAngleDeg >= 180 ||
    input.minimumAngleDeg > input.maximumAngleDeg ||
    input.radius <= 0
  ) {
    throw new RangeError("Corner-angle drag requires finite geometry and a valid 0..180 degree range.");
  }
  const dx = input.pointerX - input.axisX;
  const dy = input.pointerY - input.axisY;
  if (Math.hypot(dx, dy) < 0.001) {
    throw new RangeError("Corner-angle drag pointer must differ from the joint axis.");
  }
  const fullTurn = Math.PI * 2;
  const positiveRadians = (value: number): number => ((value % fullTurn) + fullTurn) % fullTurn;
  const pointerAngle = Math.atan2(dy, dx);
  const directionalRadians = input.turnDirection === "clockwise"
    ? positiveRadians(input.firstRayAngleRadians - pointerAngle)
    : positiveRadians(pointerAngle - input.firstRayAngleRadians);
  const rawAngleDeg = directionalRadians * 180 / Math.PI;
  const includedAngleDeg = Math.round(Math.min(
    input.maximumAngleDeg,
    Math.max(input.minimumAngleDeg, rawAngleDeg)
  ) * 10) / 10;
  const signedSweep = includedAngleDeg * Math.PI / 180 *
    (input.turnDirection === "clockwise" ? -1 : 1);
  const endAngle = input.firstRayAngleRadians + signedSweep;
  const middleAngle = input.firstRayAngleRadians + signedSweep / 2;
  const labelRadius = input.radius + 13;
  return {
    includedAngleDeg,
    endX: input.axisX + Math.cos(endAngle) * input.radius,
    endY: input.axisY + Math.sin(endAngle) * input.radius,
    labelX: input.axisX + Math.cos(middleAngle) * labelRadius,
    labelY: input.axisY + Math.sin(middleAngle) * labelRadius,
    sweepFlag: signedSweep >= 0 ? 1 : 0
  };
}

/**
 * Commits one native range-input intent without losing it to a synchronous repaint.
 *
 * Algorithm: read and parse the live DOM value first, cancel any animation second,
 * then commit the captured percentage. Cancellation notifies preview subscribers
 * synchronously, so reversing the first two steps lets a rerender restore the old
 * value before it can be read.
 *
 * @param input Deferred control read plus playback and preview-store callbacks.
 * @example A 50% drag is still committed when `cancelPlayback` repaints the range to 80%.
 * @since 0.8.5
 * @modified 2026-09-17 - Added a testable ordering boundary for range input.
 */
export function commitOpeningProgressIntent(input: Readonly<{
  readValue: () => string;
  cancelPlayback: () => void;
  setProgress: (progressPercent: number) => void;
}>): void {
  const progressPercent = Number(input.readValue());
  input.cancelPlayback();
  input.setProgress(progressPercent);
}

/**
 * Commits one physical-angle input before playback cancellation can repaint it.
 *
 * Manual rotating-window controls expose degrees; normalized percentage remains
 * an internal animation coordinate. Range and numeric inputs share this pure
 * boundary so desktop and mobile commit exactly the value the user entered.
 *
 * @param input Deferred degree read plus playback and preview-store callbacks.
 * @example Entering `12.5` commits 12.5° even if cancellation redraws 18.3°.
 * @since 0.9.0
 * @modified 2026-09-17 - Replaced percentage-facing manual opening input.
 */
export function commitOpeningAngleIntent(input: Readonly<{
  readValue: () => string;
  cancelPlayback: () => void;
  setAngleDegrees: (angleDegrees: number) => void;
}>): void {
  const angleDegrees = Number(input.readValue());
  input.cancelPlayback();
  input.setAngleDegrees(angleDegrees);
}

/**
 * Commits a mode-select intent while preserving the option chosen by the user.
 *
 * Mode changes share the range bug's synchronous notification path. Capturing the
 * option before cancellation prevents a control rerender from reverting “内倒”
 * to the previous “平开” value before the store receives the new intent.
 *
 * @param input Deferred select read plus playback and preview-store callbacks.
 * @example Selecting `tilt` remains `tilt` even if cancellation repaints `primary`.
 * @since 0.8.5
 * @modified 2026-09-17 - Added a testable ordering boundary for mode selection.
 */
export function commitOpeningMotionModeIntent(input: Readonly<{
  readValue: () => string;
  cancelPlayback: () => void;
  setMotionMode: (mode: "primary" | "tilt") => void;
}>): void {
  const motionMode = input.readValue() === "tilt" ? "tilt" : "primary";
  input.cancelPlayback();
  input.setMotionMode(motionMode);
}

/**
 * Pending mobile long-press that may open a member action menu.
 *
 * The timer is presentation-only. Moving more than eight client pixels, adding
 * another finger, lifting or cancelling clears it; no design command is emitted
 * until a user chooses a menu action.
 *
 * @example Holding a topology mullion for 550ms opens edit/delete actions.
 * @since 0.4.5
 * @modified 2026-09-17 - Added touch equivalent for desktop right-click.
 */
interface MemberLongPressState {
  readonly pointerId: number;
  readonly startClientX: number;
  readonly startClientY: number;
  readonly member: SVGElement;
  readonly timer: ReturnType<typeof setTimeout>;
}

/**
 * Grid cell/window resolved from a stable cross-view selection.
 *
 * It lets a window, cell, frame, grid divider or topology member drive the same
 * toolbar commands without asking either renderer for business state.
 *
 * @example Selecting a member hosted by row 1/column 2 returns that grid cell.
 * @since 0.4.2
 * @modified 2026-09-17 - Shared selection-to-command targeting.
 */
interface GridCommandTarget {
  readonly window: WindowUnit;
  readonly row: number;
  readonly column: number;
}

/**
 * Optional shell-owned placement for the shared construction tools.
 *
 * Desktop uses the built-in horizontal toolbar. Mobile supplies a drawer body
 * and its surrounding disclosure panel; the workspace populates it and keeps
 * its availability synchronized with the same selection and command session.
 *
 * @example Mobile passes its `data-mobile-grid-tools` element and `<details>`.
 * @since 0.4.6
 * @modified 2026-09-20 - Added shell-specific presentation for shared open-properties intent.
 */
export interface DesignWorkspaceMountOptions {
  readonly constructionToolsContainer?: HTMLElement;
  readonly constructionToolsPanel?: HTMLElement;
  readonly constructionToolsStatus?: HTMLElement;
  readonly openingControlsContainer?: HTMLElement;
  readonly openingControlsPanel?: HTMLElement;
  /** Reloads hash-pinned component bytes into the shared Three registry. */
  readonly visualAssetGateway?: ManagedGltfAssetGateway;
  /** Desktop normally uses high; mobile may request low without changing design data. */
  readonly visualAssetQuality?: ThreeVisualAssetQuality;
  /**
   * Presents the shell-specific property panel for one already selected object.
   *
   * @example Desktop focuses the side panel; mobile expands its property drawer.
   * @since 0.10.28
   * @modified 2026-09-20 - Added common double-activation presentation hook.
   */
  readonly onOpenProperties?: (objectId: DesignObjectId) => void;
}

/** Exact managed model identity needed to rebuild one design's Three scene. */
export interface ManagedComponentAssetRequirement {
  readonly assetId: string;
  readonly contentHash: string;
  readonly quality: ThreeVisualAssetQuality;
}

/**
 * Lists unique managed GLB/glTF objects referenced by a design snapshot.
 *
 * The function reads only persisted visual configuration. It includes primary,
 * medium and low LOD hashes and rejects one ID being pinned to two hashes inside
 * the same design. The result contains no renderer objects and is therefore
 * shared by desktop/mobile reload orchestration and unit tests.
 *
 * @example A custom handle with three LODs returns three ID/hash requirements.
 * @since 0.10.16
 * @modified 2026-09-18 - Added deterministic persisted model reload discovery.
 */
export function listManagedComponentAssetRequirements(
  document: DesignDocument,
  preferredQuality?: ThreeVisualAssetQuality
): readonly ManagedComponentAssetRequirement[] {
  const requirements = new Map<string, ManagedComponentAssetRequirement>();
  const add = (requirement: ManagedComponentAssetRequirement): void => {
    const assetId = requirement.assetId.trim();
    const contentHash = requirement.contentHash.trim().toLowerCase();
    if (!assetId || !/^sha256:[0-9a-f]{64}$/u.test(contentHash)) {
      throw new Error("Managed component asset references require an ID and SHA-256 hash.");
    }
    const existing = requirements.get(assetId);
    if (existing && existing.contentHash !== contentHash) {
      throw new Error(`Managed component asset ${assetId} has conflicting content hashes.`);
    }
    if (!existing) requirements.set(assetId, { assetId, contentHash, quality: requirement.quality });
  };
  for (const window of document.windows) {
    for (const assignment of window.visualConfiguration?.hardwareModels ?? []) {
      const geometry = assignment.model.geometry;
      if (geometry.kind !== "gltf") continue;
      const primary = {
        assetId: geometry.assetId,
        contentHash: geometry.contentHash,
        quality: "high" as const
      };
      const lods = geometry.lodAssets ?? [];
      if (preferredQuality) {
        const medium = lods.find((asset) => asset.quality === "medium");
        const low = lods.find((asset) => asset.quality === "low");
        const selected = preferredQuality === "low"
          ? low ?? medium ?? primary
          : preferredQuality === "medium"
            ? medium ?? primary ?? low
            : primary;
        add(selected);
      } else {
        add(primary);
        for (const lod of lods) add(lod);
      }
    }
  }
  return [...requirements.values()].sort((left, right) =>
    `${left.assetId}|${left.quality}`.localeCompare(`${right.assetId}|${right.quality}`));
}

/**
 * Mounts the shared 2D/3D workspace used by desktop and mobile layout shells.
 *
 * The component owns view tabs and renderer lifecycles but not business state.
 * Both projections read the same immutable `DesignSession`, and all pointer
 * hits are reduced to one stable ID in `DesignSelectionStore`. The layout shell
 * can therefore be replaced without duplicating geometry or selection rules.
 *
 * @param container Host element owned by a desktop or mobile shell.
 * @param session Shared immutable design session.
 * @param selection Shared 2D/3D/object-tree selection coordinator.
 * @param canvasView Shared zoom, pan, dimension and plan-view state.
 * @param openingPreview Shared runtime-only per-panel progress and playback state.
 * @param visualPreview Shared runtime-only material/model draft state.
 * @param options Optional shell-owned placement for shared drawing tools.
 * @returns A disposer for DOM events, subscriptions and WebGL resources.
 * @example `mountSharedDesignWorkspace(main, session, selection)`.
 * @since 0.4.0
 * @modified 2026-09-20 - Added explicit 3D wall/package visibility for factory and installation review.
 */
export function mountSharedDesignWorkspace(
  container: HTMLElement,
  session: DesignSession,
  selection: DesignSelectionStore,
  canvasView: DesignCanvasViewStore,
  openingPreview: OpeningPreviewStore,
  visualPreview: WindowVisualPreviewStore,
  options: DesignWorkspaceMountOptions = {}
): () => void {
  container.classList.add("design-workspace");
  container.innerHTML = `
    <div class="design-workspace__toolbar" role="toolbar" aria-label="设计视图">
      <div class="design-workspace__toolbar-group design-workspace__toolbar-group--views" role="group" aria-label="工作视图">
        <span class="design-workspace__toolbar-caption">工作视图</span>
        <div class="design-workspace__view-switch">
          <button type="button" data-view="2d" aria-pressed="true">2D 工程图</button>
          <button type="button" data-view="3d" aria-pressed="false">3D 仿真</button>
        </div>
      </div>
      <span class="design-workspace__asset-status" data-runtime-asset-status hidden role="status"></span>
      <div class="design-workspace__toolbar-group design-workspace__toolbar-group--display" role="group" aria-label="图纸显示">
        <span class="design-workspace__toolbar-caption">显示与图样</span>
        <label class="design-workspace__shared-view-option"><input type="checkbox" data-show-dimensions aria-label="显示2D标尺及选中对象的3D尺寸" />尺寸</label>
        <span class="design-workspace__2d-controls" data-2d-controls>
          <label>图样<select data-2d-render-style aria-label="选择2D图样风格"><option value="material">材质图</option><option value="engineering-line">工程线稿</option></select></label>
          <label><input type="checkbox" data-show-plan aria-label="显示俯视图" />俯视图</label>
          <label><input type="checkbox" data-show-opening-state aria-label="显示开启状态" />开启状态</label>
          <button type="button" data-add-text-label aria-label="在当前窗或组合上增加文字标签">文字标签</button>
          <button type="button" data-reset-canvas>重置视图</button>
        </span>
        <span class="design-workspace__3d-controls" data-3d-controls hidden>
          <label><input type="checkbox" data-show-selection-outline aria-label="显示3D选中辅助框" />选中框</label>
          <label><input type="checkbox" data-show-installation-host aria-label="显示当前单窗或组合窗的3D参考墙体" />参考墙体</label>
          <label><input type="checkbox" data-show-installation-surround aria-label="显示3D包边和洞口衬板" />包边</label>
          <button type="button" data-reset-camera>重置视角</button>
        </span>
      </div>
      <span class="design-workspace__grid-controls" data-grid-controls aria-label="中梃与分格工具"></span>
      <span class="design-opening-controls" data-opening-controls aria-label="开关窗预览控制"></span>
    </div>
    <div class="design-workspace__stage">
      <div class="design-surface" data-design-2d aria-label="2D设计视图"></div>
      <div class="three-design-viewport" data-design-3d hidden></div>
      <form class="design-dimension-editor" data-dimension-editor hidden>
        <label><span data-dimension-label>尺寸 mm</span><input type="number" min="300" max="30000" step="any" required /></label>
        <div><button type="submit">应用</button><button type="button" data-dimension-cancel>取消</button></div>
      </form>
      <form class="design-text-label-editor" data-text-label-editor hidden>
        <label>文字<input type="text" data-text-label-text maxlength="200" required /></label>
        <label>字号(mm)<input type="number" data-text-label-size min="1.5" max="20" step="0.5" required /></label>
        <label>颜色<input type="color" data-text-label-color /></label>
        <label>视图<select data-text-label-view><option value="elevation">立视图</option><option value="plan">俯视图</option><option value="both">两者</option></select></label>
        <label>旋转(°)<input type="number" data-text-label-rotation min="-180" max="180" step="1" required /></label>
        <label>对齐<select data-text-label-align><option value="left">左</option><option value="center">中</option><option value="right">右</option></select></label>
        <label><input type="checkbox" data-text-label-print-visible />工厂/安装图可继承</label>
        <div><button type="submit">应用</button><button type="button" data-text-label-cancel>取消</button></div>
      </form>
      <div class="design-context-menu" data-member-context-menu hidden role="menu">
        <strong data-context-title>构件</strong>
        <button type="button" data-context-edit role="menuitem">编辑位置</button>
        <button type="button" data-context-delete role="menuitem">删除构件</button>
      </div>
    </div>`;

  const twoDimensional = container.querySelector<HTMLElement>("[data-design-2d]");
  const threeDimensional = container.querySelector<HTMLElement>("[data-design-3d]");
  const resetCamera = container.querySelector<HTMLButtonElement>("[data-reset-camera]");
  const resetCanvas = container.querySelector<HTMLButtonElement>("[data-reset-canvas]");
  const twoDimensionalControls = container.querySelector<HTMLElement>("[data-2d-controls]");
  const threeDimensionalControls = container.querySelector<HTMLElement>("[data-3d-controls]");
  const gridControls = container.querySelector<HTMLElement>("[data-grid-controls]");
  const showDimensions = container.querySelector<HTMLInputElement>("[data-show-dimensions]");
  const twoDimensionalRenderStyle = container.querySelector<HTMLSelectElement>(
    "[data-2d-render-style]"
  );
  const showPlan = container.querySelector<HTMLInputElement>("[data-show-plan]");
  const showOpeningState = container.querySelector<HTMLInputElement>("[data-show-opening-state]");
  const showSelectionOutline = container.querySelector<HTMLInputElement>(
    "[data-show-selection-outline]"
  );
  const showInstallationHost = container.querySelector<HTMLInputElement>(
    "[data-show-installation-host]"
  );
  const showInstallationSurround = container.querySelector<HTMLInputElement>(
    "[data-show-installation-surround]"
  );
  const runtimeAssetStatus = container.querySelector<HTMLElement>("[data-runtime-asset-status]");
  const internalOpeningControls = container.querySelector<HTMLElement>("[data-opening-controls]");
  const dimensionEditor = container.querySelector<HTMLFormElement>("[data-dimension-editor]");
  const dimensionInput = dimensionEditor?.querySelector<HTMLInputElement>("input");
  const dimensionLabel = dimensionEditor?.querySelector<HTMLElement>("[data-dimension-label]");
  const dimensionCancel = dimensionEditor?.querySelector<HTMLButtonElement>("[data-dimension-cancel]");
  const addTextLabel = container.querySelector<HTMLButtonElement>("[data-add-text-label]");
  const textLabelEditor = container.querySelector<HTMLFormElement>("[data-text-label-editor]");
  const textLabelText = textLabelEditor?.querySelector<HTMLInputElement>("[data-text-label-text]");
  const textLabelSize = textLabelEditor?.querySelector<HTMLInputElement>("[data-text-label-size]");
  const textLabelColor = textLabelEditor?.querySelector<HTMLInputElement>("[data-text-label-color]");
  const textLabelView = textLabelEditor?.querySelector<HTMLSelectElement>("[data-text-label-view]");
  const textLabelRotation = textLabelEditor?.querySelector<HTMLInputElement>("[data-text-label-rotation]");
  const textLabelAlign = textLabelEditor?.querySelector<HTMLSelectElement>("[data-text-label-align]");
  const textLabelPrintVisible = textLabelEditor?.querySelector<HTMLInputElement>("[data-text-label-print-visible]");
  const textLabelCancel = textLabelEditor?.querySelector<HTMLButtonElement>("[data-text-label-cancel]");
  const memberContextMenu = container.querySelector<HTMLElement>("[data-member-context-menu]");
  const contextTitle = memberContextMenu?.querySelector<HTMLElement>("[data-context-title]");
  const contextEdit = memberContextMenu?.querySelector<HTMLButtonElement>("[data-context-edit]");
  const contextDelete = memberContextMenu?.querySelector<HTMLButtonElement>("[data-context-delete]");
  const viewButtons = [
    ...container.querySelectorAll<HTMLButtonElement>("[data-view]")
  ];
  if (
    !twoDimensional ||
    !threeDimensional ||
    !resetCamera ||
    !resetCanvas ||
    !twoDimensionalControls ||
    !threeDimensionalControls ||
    !gridControls ||
    !showDimensions ||
    !twoDimensionalRenderStyle ||
    !showPlan ||
    !showOpeningState ||
    !showSelectionOutline ||
    !showInstallationHost ||
    !showInstallationSurround ||
    !runtimeAssetStatus ||
    !internalOpeningControls ||
    !dimensionEditor ||
    !dimensionInput ||
    !dimensionLabel ||
    !dimensionCancel ||
    !addTextLabel ||
    !textLabelEditor ||
    !textLabelText ||
    !textLabelSize ||
    !textLabelColor ||
    !textLabelView ||
    !textLabelRotation ||
    !textLabelAlign ||
    !textLabelPrintVisible ||
    !textLabelCancel ||
    !memberContextMenu ||
    !contextTitle ||
    !contextEdit ||
    !contextDelete ||
    viewButtons.length !== 2
  ) {
    throw new Error("Shared design workspace could not resolve its required regions.");
  }
  populateGridToolButtons(gridControls);
  if (options.constructionToolsContainer) {
    options.constructionToolsContainer.classList.add("design-construction-tools");
    options.constructionToolsContainer.setAttribute("role", "toolbar");
    options.constructionToolsContainer.setAttribute("aria-label", "移动中梃与分格工具");
    populateGridToolButtons(options.constructionToolsContainer);
  }
  const gridButtons = [
    ...container.querySelectorAll<HTMLButtonElement>("[data-grid-action]"),
    ...(options.constructionToolsContainer?.querySelectorAll<HTMLButtonElement>(
      "[data-grid-action]"
    ) ?? [])
  ];

  let currentDocument = session.document;
  let selectedObjectId = selection.state.objectId;
  let currentCanvasView = canvasView.state;
  let activeView: WorkspaceView = "2d";
  let threeDimensionWasShown = false;
  let viewport: ThreeDesignViewport | undefined;
  const visualAssetRegistry = new ThreeVisualAssetRegistry();
  const loadingVisualAssets = new Map<string, Promise<void>>();
  let runtimeAssetRequestVersion = 0;
  let disposed = false;
  let dimensionCommandSequence = 0;
  let gridCommandSequence = 0;
  let jointCommandSequence = 0;
  let textLabelCommandSequence = 0;
  let canvasPan: CanvasPanState | undefined;
  let dividerDrag: DividerDragState | undefined;
  let topologyMemberDrag: TopologyMemberDragState | undefined;
  let meetingMullionDrag: MeetingMullionDragState | undefined;
  let cornerAngleDrag: CornerAngleDragState | undefined;
  let drawingTextLabelDrag: DrawingTextLabelDragState | undefined;
  let suppressNextCanvasClick = false;
  let canvasClickSuppressionTimer: ReturnType<typeof setTimeout> | undefined;
  let skipNativeCanvasDoubleClick = false;
  const canvasPointerPresses = new Map<number, CanvasPointerPressState>();
  const touchCanvasGesture = new TouchCanvasGestureController();
  let memberLongPress: MemberLongPressState | undefined;
  const openingControlsHost = options.openingControlsContainer ?? internalOpeningControls;
  if (options.openingControlsContainer) internalOpeningControls.hidden = true;
  openingControlsHost.classList.add("design-opening-controls");
  openingControlsHost.setAttribute("role", "group");
  openingControlsHost.setAttribute("aria-label", "开关窗预览控制");
  const openingController = new OpeningPreviewController(openingPreview);
  let currentOpeningPreview: OpeningPreviewState = openingPreview.state;
  let openingControlsSignature = "";
  let openingAnimationFrame: number | undefined;
  let openingAnimationTimestamp: number | undefined;

  /**
   * Arms suppression for the compatibility click following a real drag/pan.
   *
   * @example Called once when a pending 2D press crosses the shared 6px threshold.
   * @since 0.10.28
   * @modified 2026-09-20 - Prevented drag release from also selecting a new target.
   */
  const suppressUpcomingCanvasClick = (): void => {
    suppressNextCanvasClick = true;
  };

  /**
   * Releases drag-click suppression after the pointer-up event has had a chance
   * to emit its synchronous compatibility click.
   *
   * Scheduling the release from pointer-up, rather than the first pointer move,
   * keeps slow drags protected for their entire duration.
   *
   * @example A ten-second drag remains protected until its eventual pointer-up click.
   * @since 0.10.28
   * @modified 2026-09-20 - Fixed early suppression expiry during slow drags.
   */
  const scheduleCanvasClickSuppressionRelease = (): void => {
    if (canvasClickSuppressionTimer !== undefined) clearTimeout(canvasClickSuppressionTimer);
    canvasClickSuppressionTimer = setTimeout(() => {
      suppressNextCanvasClick = false;
      canvasClickSuppressionTimer = undefined;
    }, 0);
  };

  /**
   * Rebuilds panel checkboxes only when the design's stable panel set changes.
   *
   * Slider frames update existing controls in place, preventing pointer capture
   * loss while a mouse or finger drags through many preview values.
   * @since 0.8.2
   * @modified 2026-09-17 - Added shared PC/mobile opening controls structure.
   */
  const synchronizeOpeningControlStructure = (): void => {
    const definitions = openingPreview.panelDefinitions;
    const signature = definitions
      .map((definition) => `${definition.mechanism}:${definition.key}`)
      .join("|");
    if (signature === openingControlsSignature) return;
    openingControlsSignature = signature;
    openingControlsHost.replaceChildren();
    const panelList = document.createElement("span");
    panelList.className = "design-opening-controls__panels";
    panelList.dataset.openingPanelList = "";
    for (const definition of definitions) {
      const label = document.createElement("label");
      const checkbox = document.createElement("input");
      checkbox.type = "checkbox";
      checkbox.dataset.previewPanelKey = definition.key;
      checkbox.setAttribute("aria-label", `选择${definition.panelId}`);
      const roleLabel = definition.mechanism === "sliding"
        ? "滑动扇"
        : definition.panelRole === "primary"
          ? "主动扇"
          : definition.panelRole === "secondary"
            ? "从动扇"
            : "独立扇";
      label.append(checkbox, `${definition.panelId} ${roleLabel}`);
      panelList.append(label);
    }
    const progressLabel = document.createElement("label");
    progressLabel.className = "design-opening-controls__progress";
    const progressCaption = document.createElement("span");
    progressCaption.dataset.openingProgressCaption = "";
    progressCaption.textContent = "开启角度 ";
    const angleRange = document.createElement("input");
    angleRange.type = "range";
    angleRange.min = "0";
    angleRange.max = "90";
    angleRange.step = "0.1";
    angleRange.dataset.openingAngleRange = "";
    angleRange.setAttribute("aria-label", "所选窗扇开启角度");
    const angleNumber = document.createElement("input");
    angleNumber.type = "number";
    angleNumber.min = "0";
    angleNumber.max = "90";
    angleNumber.step = "0.1";
    angleNumber.inputMode = "decimal";
    angleNumber.dataset.openingAngleNumber = "";
    angleNumber.setAttribute("aria-label", "输入所选窗扇开启角度");
    const progressUnit = document.createElement("span");
    progressUnit.dataset.openingProgressUnit = "";
    progressUnit.textContent = "°";
    progressLabel.append(progressCaption, angleRange, angleNumber, progressUnit);
    const mode = document.createElement("select");
    mode.dataset.openingMotionMode = "";
    mode.setAttribute("aria-label", "所选窗扇运动模式");
    mode.append(new Option("平开", "primary"), new Option("内倒", "tilt"));
    const actions = [
      ["open-selected", "开启所选"],
      ["close-selected", "关闭所选"],
      ["open-all", "全部开启"],
      ["close-all", "全部关闭"],
      ["play-pause", "播放"]
    ] as const;
    openingControlsHost.append(panelList, progressLabel, mode);
    for (const [action, label] of actions) {
      const button = document.createElement("button");
      button.type = "button";
      button.dataset.openingAction = action;
      button.textContent = label;
      openingControlsHost.append(button);
    }
  };

  /** Updates control values and disabled states from one runtime snapshot. */
  const updateOpeningControlState = (): void => {
    synchronizeOpeningControlStructure();
    const selected = new Set(currentOpeningPreview.selectedPanelKeys);
    for (const checkbox of openingControlsHost.querySelectorAll<HTMLInputElement>(
      "[data-preview-panel-key]"
    )) {
      checkbox.checked = selected.has(checkbox.dataset.previewPanelKey ?? "");
    }
    const angleRange = openingControlsHost.querySelector<HTMLInputElement>(
      "[data-opening-angle-range]"
    );
    const angleNumber = openingControlsHost.querySelector<HTMLInputElement>(
      "[data-opening-angle-number]"
    );
    const mode = openingControlsHost.querySelector<HTMLSelectElement>(
      "[data-opening-motion-mode]"
    );
    const progressCaption = openingControlsHost.querySelector<HTMLElement>(
      "[data-opening-progress-caption]"
    );
    const progressUnit = openingControlsHost.querySelector<HTMLElement>(
      "[data-opening-progress-unit]"
    );
    const selectedDefinitions = openingPreview.panelDefinitions.filter((definition) =>
      selected.has(definition.key)
    );
    const selectedMechanism = selectedDefinitions[0]?.mechanism ??
      openingPreview.panelDefinitions[0]?.mechanism ?? "hinged";
    const isSlidingSelection = selectedMechanism === "sliding";
    const selectedValues = isSlidingSelection
      ? selectedDefinitions.map((definition) =>
          currentOpeningPreview.panelProgressPercent[definition.key] ?? 0
        )
      : selectedDefinitions.map((definition) =>
          resolveOpeningPreviewAngleDegrees(definition, currentOpeningPreview)
        );
    const selectedMaximumAngles = isSlidingSelection
      ? []
      : selectedDefinitions.map((definition) => {
          const selectedMode = currentOpeningPreview.panelMotionMode[definition.key] ??
            definition.motionMode;
          return resolveOpeningPreviewMaximumAngleDegrees(definition, selectedMode);
        });
    const average = selectedValues.length
      ? selectedValues.reduce((sum, value) => sum + value, 0) / selectedValues.length
      : 0;
    const sharedMaximum = isSlidingSelection
      ? 100
      : selectedMaximumAngles.length
        ? Math.min(...selectedMaximumAngles)
        : 90;
    const formattedValue = (Math.round(average * 10) / 10).toFixed(1);
    const formattedMaximum = (Math.round(sharedMaximum * 10) / 10).toFixed(1);
    if (progressCaption) {
      progressCaption.textContent = isSlidingSelection ? "滑移开度 " : "开启角度 ";
    }
    if (progressUnit) progressUnit.textContent = isSlidingSelection ? "%" : "°";
    if (angleRange) {
      angleRange.disabled = selectedValues.length === 0;
      angleRange.max = formattedMaximum;
      angleRange.value = formattedValue;
      angleRange.setAttribute(
        "aria-label",
        isSlidingSelection ? "所选推拉扇滑移开度" : "所选窗扇开启角度"
      );
    }
    if (angleNumber) {
      angleNumber.disabled = selectedValues.length === 0;
      angleNumber.max = formattedMaximum;
      angleNumber.value = formattedValue;
      angleNumber.setAttribute(
        "aria-label",
        isSlidingSelection ? "输入所选推拉扇滑移开度" : "输入所选窗扇开启角度"
      );
    }
    if (mode) {
      mode.hidden = isSlidingSelection;
      mode.disabled =
        isSlidingSelection || selectedDefinitions.length === 0 ||
        selectedDefinitions.some((definition) => !definition.supportsTilt);
      const modes = new Set(selectedDefinitions.map(
        (definition) => currentOpeningPreview.panelMotionMode[definition.key] ?? "primary"
      ));
      mode.value = modes.size === 1 ? [...modes][0] ?? "primary" : "primary";
    }
    const hasSelection = selectedDefinitions.length > 0;
    const hasPanels = openingPreview.panelDefinitions.length > 0;
    for (const button of openingControlsHost.querySelectorAll<HTMLButtonElement>(
      "[data-opening-action]"
    )) {
      const action = button.dataset.openingAction;
      button.disabled =
        action?.endsWith("selected") || action === "play-pause"
          ? !hasSelection
          : !hasPanels;
      if (action === "play-pause") {
        button.textContent = currentOpeningPreview.playback === "idle" ? "播放" :
          currentOpeningPreview.playback === "paused" ? "继续" : "暂停";
      }
    }
    if (options.openingControlsPanel) {
      options.openingControlsPanel.hidden = !hasPanels;
    }
  };

  /** Schedules the device-owned clock that advances the shared pure controller. */
  const scheduleOpeningAnimation = (): void => {
    if (openingAnimationFrame !== undefined) return;
    if (
      currentOpeningPreview.playback === "idle" ||
      currentOpeningPreview.playback === "paused"
    ) return;
    openingAnimationFrame = requestAnimationFrame((timestamp) => {
      openingAnimationFrame = undefined;
      const delta = openingAnimationTimestamp === undefined
        ? 0
        : timestamp - openingAnimationTimestamp;
      openingAnimationTimestamp = timestamp;
      openingController.advance(delta);
      scheduleOpeningAnimation();
    });
  };

  /** Handles panel selection, manual progress, mode and playback intents. */
  const onOpeningControlsInput = (event: Event): void => {
    const target = event.target;
    if (!(target instanceof HTMLInputElement || target instanceof HTMLSelectElement)) return;
    if (
      event.type === "change" &&
      target instanceof HTMLInputElement &&
      target.dataset.previewPanelKey
    ) {
      openingPreview.toggleSelected(target.dataset.previewPanelKey);
      return;
    }
    const isAngleRangeIntent =
      event.type === "input" &&
      target instanceof HTMLInputElement &&
      "openingAngleRange" in target.dataset;
    const isAngleNumberIntent =
      event.type === "change" &&
      target instanceof HTMLInputElement &&
      "openingAngleNumber" in target.dataset;
    if (isAngleRangeIntent || isAngleNumberIntent) {
      const selectedDefinitions = openingPreview.panelDefinitions.filter((definition) =>
        currentOpeningPreview.selectedPanelKeys.includes(definition.key)
      );
      if (selectedDefinitions[0]?.mechanism === "sliding") {
        openingController.cancel();
        openingPreview.setSelectedProgress(Number(target.value));
        return;
      }
      commitOpeningAngleIntent({
        readValue: () => target.value,
        cancelPlayback: () => openingController.cancel(),
        setAngleDegrees: (angleDegrees) => openingPreview.setSelectedAngleDegrees(angleDegrees)
      });
      return;
    }
    if (
      event.type === "change" &&
      target instanceof HTMLSelectElement &&
      "openingMotionMode" in target.dataset
    ) {
      commitOpeningMotionModeIntent({
        readValue: () => target.value,
        cancelPlayback: () => openingController.cancel(),
        setMotionMode: (motionMode) => openingPreview.setSelectedMotionMode(motionMode)
      });
    }
  };
  const onOpeningControlsClick = (event: Event): void => {
    const target = event.target;
    if (!(target instanceof HTMLButtonElement)) return;
    const action = target.dataset.openingAction;
    if (action === "open-selected") openingController.openSelected();
    else if (action === "close-selected") openingController.closeSelected();
    else if (action === "open-all") openingController.openAll();
    else if (action === "close-all") openingController.closeAll();
    else if (action === "play-pause") {
      if (currentOpeningPreview.playback === "paused") openingController.resume();
      else if (currentOpeningPreview.playback !== "idle") openingController.pause();
      else openingController.playSelectedCycle();
    }
    scheduleOpeningAnimation();
  };

  try {
    viewport = new ThreeDesignViewport(threeDimensional, {
      onSelect: (objectId) => selection.select(objectId, "3d")
    });
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error);
    threeDimensional.innerHTML = `<div class="three-design-viewport__error"><strong>3D视口启动失败</strong><span></span></div>`;
    const detail = threeDimensional.querySelector("span");
    if (detail) detail.textContent = message;
  }

  /**
   * Rebuilds Three from current runtime state and all safely loaded assets.
   * Factory preview leaves the non-product host wall hidden by default; the
   * toolbar can opt into wall and package layers independently without changing
   * design, BOM or the multi-window product layout.
   *
   * @example Checking “参考墙体” shows the selected installation subject's host context.
   * @since 0.4.0
   * @modified 2026-09-20 - Separated product and installation context layers.
  */
  const updateThreeDimensional = (resetCamera = true): void => {
    const projectedDocument = visualPreview.project(currentDocument);
    const installationWindowId = selectedOwningWindow(
      projectedDocument,
      selectedObjectId
    )?.objectId ?? projectedDocument.windows[0]?.objectId;
    viewport?.update(projectedDocument, {
      openingProgressPercentByPanelKey: currentOpeningPreview.panelProgressPercent,
      openingMotionModeByPanelKey: currentOpeningPreview.panelMotionMode,
      showLinearDimensions: currentCanvasView.showDimensions,
      linearDimensionObjectId: selectedObjectId,
      showOpeningAngleAnnotations: currentCanvasView.showOpeningState,
      showInstallationHost: currentCanvasView.showThreeInstallationHost,
      installationHostWindowId: currentCanvasView.showThreeInstallationHost
        ? installationWindowId
        : undefined,
      showInstallationSurround: currentCanvasView.showThreeInstallationSurround,
      visualAssets: visualAssetRegistry,
      assetQuality: options.visualAssetQuality ?? "high"
    }, resetCamera);
  };

  /** Updates the toolbar's compact loading/result message without resizing the canvas. */
  const setRuntimeAssetStatus = (
    state: "loading" | "ready" | "error" | undefined,
    message = ""
  ): void => {
    runtimeAssetStatus.hidden = !state;
    if (state) runtimeAssetStatus.dataset.state = state;
    else delete runtimeAssetStatus.dataset.state;
    runtimeAssetStatus.textContent = message;
  };

  /** Loads and registers one exact stored model once for this workspace lifetime. */
  const loadRuntimeAsset = (
    requirement: ManagedComponentAssetRequirement
  ): Promise<void> => {
    const existing = visualAssetRegistry.resolveComponentModel(requirement.assetId);
    if (existing) {
      if (existing.contentHash.toLowerCase() !== requirement.contentHash) {
        return Promise.reject(new Error(
          `模型 ${requirement.assetId} 与当前设计保存的SHA-256不一致。`
        ));
      }
      return Promise.resolve();
    }
    const key = `${requirement.assetId}|${requirement.contentHash}`;
    const pending = loadingVisualAssets.get(key);
    if (pending) return pending;
    const load = (async () => {
      if (!options.visualAssetGateway) {
        throw new Error("当前环境没有配置本地模型读取服务。");
      }
      const stored = await options.visualAssetGateway.read(
        requirement.assetId,
        requirement.contentHash
      );
      if (!stored) {
        throw new Error(`找不到模型 ${requirement.assetId} 的指定版本。`);
      }
      const loaded = await loadManagedGltfComponentAsset({
        assetId: requirement.assetId,
        contentHash: requirement.contentHash,
        bytes: stored.bytes
      });
      visualAssetRegistry.registerComponentModel(loaded.asset);
    })().finally(() => loadingVisualAssets.delete(key));
    loadingVisualAssets.set(key, load);
    return load;
  };

  /**
   * Synchronizes every persisted model reference with the runtime Three registry.
   *
   * A scene is rendered immediately with deterministic fallback geometry, then
   * rebuilt after all available hash-pinned bytes pass preflight and parse. A
   * request version prevents an older asynchronous document from overwriting
   * the status of a newer edit or undo operation.
   */
  const synchronizeRuntimeAssets = async (document: DesignDocument): Promise<void> => {
    const requestVersion = ++runtimeAssetRequestVersion;
    let requirements: readonly ManagedComponentAssetRequirement[];
    try {
      requirements = listManagedComponentAssetRequirements(
        document,
        options.visualAssetQuality ?? "high"
      );
    } catch (error) {
      setRuntimeAssetStatus("error", error instanceof Error ? error.message : "模型引用无效。");
      return;
    }
    if (requirements.length === 0) {
      setRuntimeAssetStatus(undefined);
      return;
    }
    const unloaded = requirements.filter((requirement) =>
      visualAssetRegistry.resolveComponentModel(requirement.assetId)?.contentHash.toLowerCase() !==
      requirement.contentHash);
    if (unloaded.length === 0) {
      setRuntimeAssetStatus("ready", `已加载 ${requirements.length} 个自定义模型`);
      return;
    }
    setRuntimeAssetStatus("loading", `正在读取 ${unloaded.length} 个本地模型…`);
    const results = await Promise.allSettled(unloaded.map(loadRuntimeAsset));
    if (disposed || requestVersion !== runtimeAssetRequestVersion) return;
    const failures = results.filter((result): result is PromiseRejectedResult =>
      result.status === "rejected");
    if (failures.length > 0) {
      const first = failures[0]?.reason;
      setRuntimeAssetStatus(
        "error",
        `模型加载失败 ${failures.length}/${unloaded.length}：` +
        (first instanceof Error ? first.message : "无法解析本地模型。")
      );
    } else {
      setRuntimeAssetStatus("ready", `已加载 ${requirements.length} 个自定义模型`);
    }
    updateThreeDimensional(false);
  };

  /** Replaces only the deterministic SVG projection for current state. */
  const renderTwoDimensional = (): void => {
    twoDimensional.innerHTML = renderDesignSvg(visualPreview.project(currentDocument), {
      selectedObjectId,
      viewport: currentCanvasView,
      showDimensions: currentCanvasView.showDimensions,
      showPlanView: currentCanvasView.showPlanView,
      showOpeningState: currentCanvasView.showOpeningState,
      renderStyle: currentCanvasView.twoDimensionalRenderStyle,
      openingProgressPercentByPanelKey: currentOpeningPreview.panelProgressPercent,
      openingMotionModeByPanelKey: currentOpeningPreview.panelMotionMode
    });
  };

  /** Resolves the root product that owns a new free drawing annotation. */
  const resolveDrawingTextLabelOwner = (): Readonly<{
    objectId: DesignObjectId;
    widthMm: number;
    heightMm: number;
  }> | undefined => {
    const selectedLabel = (currentDocument.drawingTextLabels ?? []).find(
      (label) => label.objectId === selectedObjectId
    );
    const selectedId = selectedLabel?.ownerObjectId ?? selectedObjectId;
    const directlySelectedAssembly = (currentDocument.assemblies ?? []).find((assembly) => {
      const wallId = createWindowInstallationWallObjectId(assembly.objectId);
      const surroundId = createWindowInstallationSurroundObjectId(assembly.objectId);
      return selectedId === assembly.objectId ||
        assembly.instances.some((instance) => instance.objectId === selectedId) ||
        assembly.joints.some((joint) => joint.objectId === selectedId) ||
        selectedId === wallId || selectedId?.startsWith(`${wallId}.`) ||
        selectedId === surroundId || selectedId?.startsWith(`${surroundId}.`);
    });
    const selectedWindow = selectedOwningWindow(currentDocument, selectedId);
    const assembly = directlySelectedAssembly ?? (currentDocument.assemblies ?? []).find(
      (candidate) => selectedWindow && candidate.instances.some(
        (instance) => instance.windowId === selectedWindow.objectId
      )
    );
    if (assembly) {
      const bounds = resolveFabricationAssemblyGeometry(assembly, currentDocument.windows).bounds;
      return {
        objectId: assembly.objectId,
        widthMm: bounds.widthMm,
        heightMm: bounds.heightMm
      };
    }
    const window = selectedWindow ?? currentDocument.windows[0];
    if (!window) return undefined;
    return {
      objectId: window.objectId,
      widthMm: window.widthMm,
      heightMm: window.heightMm
    };
  };

  /** Hides the free-label editor and clears its stable command target. */
  const closeTextLabelEditor = (): void => {
    textLabelEditor.hidden = true;
    textLabelEditor.dataset.labelId = "";
  };

  /** Opens the free-label editor at the activation point or a safe stage inset. */
  const openTextLabelEditor = (
    label: DrawingTextLabel,
    event?: Pick<MouseEvent, "clientX" | "clientY">
  ): void => {
    textLabelEditor.dataset.labelId = label.objectId;
    textLabelText.value = label.text;
    textLabelSize.value = String(label.fontSizePaperMm);
    textLabelColor.value = label.color;
    textLabelView.value = label.view;
    textLabelRotation.value = String(label.rotationDeg);
    textLabelAlign.value = label.align;
    textLabelPrintVisible.checked = label.printVisible;
    const stage = textLabelEditor.parentElement?.getBoundingClientRect();
    if (stage) {
      const clientX = event && Number.isFinite(event.clientX)
        ? event.clientX
        : stage.left + Math.min(stage.width / 2, 320);
      const clientY = event && Number.isFinite(event.clientY)
        ? event.clientY
        : stage.top + 72;
      textLabelEditor.style.left = `${Math.max(8, Math.min(stage.width - 292, clientX - stage.left + 10))}px`;
      textLabelEditor.style.top = `${Math.max(8, Math.min(stage.height - 260, clientY - stage.top - 14))}px`;
    }
    textLabelEditor.hidden = false;
    textLabelText.focus();
    textLabelText.select();
  };

  /** Adds one user-authored label to the selected window/assembly root. */
  const onAddTextLabel = (): void => {
    const owner = resolveDrawingTextLabelOwner();
    if (!owner) return;
    textLabelCommandSequence += 1;
    let suffix = `${currentDocument.revision + 1}-${textLabelCommandSequence}`;
    let objectId = `LABEL-${suffix}` as DesignObjectId;
    const occupiedIds = new Set((currentDocument.drawingTextLabels ?? []).map((label) => label.objectId));
    while (occupiedIds.has(objectId)) {
      textLabelCommandSequence += 1;
      suffix = `${currentDocument.revision + 1}-${textLabelCommandSequence}`;
      objectId = `LABEL-${suffix}` as DesignObjectId;
    }
    const label: DrawingTextLabel = {
      kind: "drawing-text-label",
      objectId,
      ownerObjectId: owner.objectId,
      text: "文字标签",
      xMm: owner.widthMm / 2,
      yMm: owner.heightMm / 2,
      view: "elevation",
      fontSizePaperMm: 3.5,
      color: "#1f2937",
      rotationDeg: 0,
      align: "center",
      printVisible: false
    };
    session.execute(createDrawingTextLabelCommand({
      commandId: `CMD-DRAWING-TEXT-CREATE-${currentDocument.revision}-${textLabelCommandSequence}`,
      label
    }));
    selection.select(label.objectId, "2d");
    openTextLabelEditor(label);
  };

  /** Applies all editable label fields through one undoable replacement command. */
  const onTextLabelSubmit = (event: SubmitEvent): void => {
    event.preventDefault();
    const label = (currentDocument.drawingTextLabels ?? []).find(
      (candidate) => candidate.objectId === textLabelEditor.dataset.labelId
    );
    if (!label) {
      closeTextLabelEditor();
      return;
    }
    const fontSizePaperMm = Number(textLabelSize.value);
    const rotationDeg = Number(textLabelRotation.value);
    if (!Number.isFinite(fontSizePaperMm) || !Number.isFinite(rotationDeg)) return;
    textLabelCommandSequence += 1;
    try {
      session.execute(createUpdateDrawingTextLabelCommand({
        commandId: `CMD-DRAWING-TEXT-UPDATE-${currentDocument.revision}-${textLabelCommandSequence}`,
        label: {
          ...label,
          text: textLabelText.value,
          fontSizePaperMm,
          color: textLabelColor.value,
          view: textLabelView.value as DrawingTextLabel["view"],
          rotationDeg,
          align: textLabelAlign.value as DrawingTextLabel["align"],
          printVisible: textLabelPrintVisible.checked
        }
      }));
      selection.select(label.objectId, "2d");
      closeTextLabelEditor();
    } catch (error) {
      const message = error instanceof Error ? error.message : "文字标签无效。";
      if (typeof globalThis.alert === "function") globalThis.alert(message);
    }
  };

  /**
   * Switches presentation panels without reconstructing either shared renderer.
   * Keeping the WebGL instance alive preserves OrbitControls state; a resize is
   * required after the previously hidden container receives real dimensions.
   */
  const showView = (view: WorkspaceView): void => {
    activeView = view;
    twoDimensional.hidden = view !== "2d";
    threeDimensional.hidden = view !== "3d";
    resetCamera.hidden = view !== "3d";
    twoDimensionalControls.hidden = view !== "2d";
    threeDimensionalControls.hidden = view !== "3d";
    gridControls.hidden = view !== "2d";
    if (options.constructionToolsPanel) {
      options.constructionToolsPanel.hidden = view !== "2d";
    }
    for (const button of viewButtons) {
      button.setAttribute("aria-pressed", String(button.dataset.view === view));
    }
    if (view === "3d") {
      viewport?.resize();
      if (!threeDimensionWasShown) {
        viewport?.resetCamera();
        threeDimensionWasShown = true;
      }
    }
  };

  const onToolbarClick = (event: Event): void => {
    const target = event.target;
    if (!(target instanceof HTMLButtonElement)) return;
    const view = target.dataset.view;
    if (view === "2d" || view === "3d") showView(view);
    const action = target.dataset.gridAction;
    if (isGridAction(action)) executeGridAction(action);
  };
  const onResetCamera = (): void => viewport?.resetCamera();
  const onResetCanvas = (): void => canvasView.resetTransform();
  const onTwoDimensionalClick = (event: MouseEvent): void => {
    if (suppressNextCanvasClick) {
      suppressNextCanvasClick = false;
      if (canvasClickSuppressionTimer !== undefined) {
        clearTimeout(canvasClickSuppressionTimer);
        canvasClickSuppressionTimer = undefined;
      }
      return;
    }
    const hit = resolveDomCanvasInteractionHit(event.target);
    // Selection redraws the SVG after the first click, so some browsers no
    // longer emit `dblclick` against the replaced label node. `detail === 2`
    // is retained on the second click and provides a stable fallback that
    // restores dimension editing without delaying single-click selection.
    if (event.detail >= 2) {
      const label = (currentDocument.drawingTextLabels ?? []).find(
        (candidate) => candidate.objectId === hit.object?.dataset.objectId
      );
      if (label) {
        skipNativeCanvasDoubleClick = true;
        setTimeout(() => { skipNativeCanvasDoubleClick = false; }, 0);
        event.preventDefault();
        event.stopPropagation();
        selection.select(label.objectId, "2d");
        openTextLabelEditor(label, event);
        return;
      }
      const doubleIntent = resolveCanvasInteractionIntent({
        activation: "double",
        hit: hit.semantic
      });
      if (doubleIntent.kind === "edit-dimension" && hit.dimension) {
        skipNativeCanvasDoubleClick = true;
        setTimeout(() => { skipNativeCanvasDoubleClick = false; }, 0);
        event.preventDefault();
        event.stopPropagation();
        openDimensionEditor(hit.dimension, event);
        return;
      }
      if (doubleIntent.kind === "open-properties") {
        skipNativeCanvasDoubleClick = true;
        setTimeout(() => { skipNativeCanvasDoubleClick = false; }, 0);
        selection.select(doubleIntent.objectId as DesignObjectId, "2d");
        options.onOpenProperties?.(doubleIntent.objectId as DesignObjectId);
        return;
      }
    }
    const intent = resolveCanvasInteractionIntent({ activation: "single", hit: hit.semantic });
    if (intent.kind !== "select") return;
    selection.select(intent.objectId as DesignObjectId | undefined, "2d");
  };

  /** Converts a browser pointer into current SVG viewBox coordinates. */
  const pointInSvg = (event: Pick<PointerEvent, "clientX" | "clientY">): { x: number; y: number } | undefined => {
    const svg = twoDimensional.querySelector<SVGSVGElement>("svg");
    if (!svg) return undefined;
    const bounds = svg.getBoundingClientRect();
    const viewBox = svg.viewBox.baseVal;
    if (bounds.width <= 0 || bounds.height <= 0) return undefined;
    return {
      x: viewBox.x + ((event.clientX - bounds.left) * viewBox.width) / bounds.width,
      y: viewBox.y + ((event.clientY - bounds.top) * viewBox.height) / bounds.height
    };
  };

  /**
   * Converts a pointer to millimetres in one rendered window's local space.
   *
   * `getCTM().inverse()` removes canvas zoom/pan and window translation; the
   * renderer scale then converts SVG units back to manufacturing millimetres.
   * For example, dragging while zoomed to 200% produces the same divider value
   * as dragging the same physical point at 100%.
   */
  const pointInWindowMm = (
    event: Pick<PointerEvent, "clientX" | "clientY">,
    element: SVGElement
  ): { x: number; y: number } | undefined => {
    const point = pointInSvg(event);
    const group = element.closest<SVGGElement>(".design-window");
    const matrix = group?.getCTM();
    const renderScale = Number(group?.dataset.renderScale);
    if (!point || !group || !matrix || !Number.isFinite(renderScale) || renderScale <= 0) {
      return undefined;
    }
    const local = new DOMPoint(point.x, point.y).matrixTransform(matrix.inverse());
    return { x: local.x / renderScale, y: local.y / renderScale };
  };

  /** Converts a root-SVG pointer to one annotation group's local coordinates. */
  const pointInSvgGroup = (
    event: Pick<PointerEvent, "clientX" | "clientY">,
    group: SVGGElement
  ): { x: number; y: number } | undefined => {
    const point = pointInSvg(event);
    const matrix = group.getCTM();
    if (!point || !matrix) return undefined;
    const local = new DOMPoint(point.x, point.y).matrixTransform(matrix.inverse());
    return { x: local.x, y: local.y };
  };

  /** Updates only the temporary angle annotation; the design graph is untouched until release. */
  const previewCornerAngleDrag = (
    drag: CornerAngleDragState,
    preview: ReturnType<typeof calculateCornerAngleDrag>
  ): void => {
    const coordinate = (value: number): string => String(Number(value.toFixed(3)));
    const startX = drag.axisX + Math.cos(drag.firstRayAngleRadians) * drag.radius;
    const startY = drag.axisY + Math.sin(drag.firstRayAngleRadians) * drag.radius;
    const path = `M ${coordinate(startX)} ${coordinate(startY)} A ${coordinate(drag.radius)} ` +
      `${coordinate(drag.radius)} 0 0 ${preview.sweepFlag} ${coordinate(preview.endX)} ` +
      `${coordinate(preview.endY)}`;
    const secondRay = drag.group.querySelector<SVGLineElement>(
      ".design-assembly-plan-view__corner-angle-second-ray"
    );
    const visibleArc = drag.group.querySelector<SVGPathElement>(
      ".design-assembly-plan-view__corner-angle-arc-visible"
    );
    const hitArc = drag.group.querySelector<SVGPathElement>(
      ".design-assembly-plan-view__corner-angle-arc"
    );
    const label = drag.group.querySelector<SVGTextElement>(
      ".design-assembly-plan-view__corner-angle-label"
    );
    const handle = drag.group.querySelector<SVGCircleElement>(
      ".design-assembly-plan-view__corner-angle-handle"
    );
    secondRay?.setAttribute("x2", coordinate(preview.endX));
    secondRay?.setAttribute("y2", coordinate(preview.endY));
    visibleArc?.setAttribute("d", path);
    hitArc?.setAttribute("d", path);
    if (label) {
      label.setAttribute("x", coordinate(preview.labelX));
      label.setAttribute("y", coordinate(preview.labelY));
      label.textContent = `${preview.includedAngleDeg}°`;
    }
    handle?.setAttribute("cx", coordinate(preview.endX));
    handle?.setAttribute("cy", coordinate(preview.endY));
    drag.group.dataset.includedAngleDeg = String(preview.includedAngleDeg);
  };

  /**
   * Maps any selected window/cell/member/frame to the grid track it addresses.
   *
   * Cell selection is exact. A through-divider chooses the track immediately
   * before it; a local mullion chooses its host region. Window/frame selection
   * defaults to the first cell, as did the prototype when no cell was active.
   * A selected flying mullion keeps its own selection ID while commands target
   * its owning cell.
   *
   * @returns Window plus zero-based host row/column, or undefined without a window.
   * @example `CELL-1:flying-mullion` resolves the grid track that owns `CELL-1`.
   * @since 0.4.2
   * @modified 2026-09-17 - Added independent flying-mullion-to-host resolution.
   */
  const resolveGridTarget = (): GridCommandTarget | undefined => {
    for (const window of currentDocument.windows) {
      const geometry = resolveWindowGeometry(window);
      const cell = geometry.cells.find((candidate) => candidate.objectId === selectedObjectId);
      if (cell) return { window, row: cell.row, column: cell.column };
      const meeting = geometry.meetingMullions.find(
        (candidate) => candidate.objectId === selectedObjectId
      );
      if (meeting) {
        const host = geometry.cells.find(
          (candidate) => candidate.objectId === meeting.sourceObjectId
        );
        if (host) return { window, row: host.row, column: host.column };
      }
      const member = geometry.members.find((candidate) => candidate.objectId === selectedObjectId);
      if (member?.kind === "grid-divider") {
        const boundary = Number(member.sourceComponentId.split(".").at(-1));
        return {
          window,
          row: member.orientation === "horizontal" ? Math.max(0, boundary - 1) : 0,
          column: member.orientation === "vertical" ? Math.max(0, boundary - 1) : 0
        };
      }
      if (member) {
        const topologyMember = window.topology.members.find(
          (candidate) => candidate.objectId === member.sourceComponentId
        );
        const host = geometry.cells.find(
          (candidate) => candidate.objectId === topologyMember?.hostRegionId
        );
        if (host) return { window, row: host.row, column: host.column };
      }
      if (
        window.objectId === selectedObjectId ||
        geometry.frames.some((candidate) => candidate.objectId === selectedObjectId)
      ) {
        return { window, row: 0, column: 0 };
      }
    }
    const window = currentDocument.windows[0];
    return window ? { window, row: 0, column: 0 } : undefined;
  };

  /**
   * Updates construction availability for the current stable selection.
   *
   * The only row/column cannot be deleted, and an identical default local
   * member cannot be added twice. This mirrors prototype no-op behavior while
   * exposing the constraint before a user clicks.
   *
   * @since 0.4.3
   * @modified 2026-09-17 - Added grid and local-member toolbar guards.
   */
  const updateGridCommandAvailability = (): void => {
    const target = resolveGridTarget();
    if (options.constructionToolsStatus) {
      options.constructionToolsStatus.textContent = target
        ? `作用目标：${target.window.mark} · 第${target.row + 1}行第${target.column + 1}列`
        : "请先创建窗体，再选择目标窗格或构件";
    }
    for (const button of gridButtons) {
      const action = button.dataset.gridAction;
      const localOrientation = action === "local-vertical"
        ? "vertical"
        : action === "local-horizontal"
          ? "horizontal"
          : undefined;
      const hostCell = target
        ? target.window.layout.cells[
            target.row * target.window.layout.columns.length + target.column
          ]
        : undefined;
      const duplicateLocal = localOrientation && hostCell
        ? target?.window.topology.members.some(
            (member) =>
              member.hostRegionId === hostCell.objectId &&
              member.orientation === localOrientation &&
              Math.abs(member.positionRatio - 0.5) < 0.015 &&
              Math.abs(member.span.startRatio) < 0.015 &&
              Math.abs(member.span.endRatio - 1) < 0.015
          )
        : false;
      button.disabled =
        !target ||
        (action === "remove-column" && target.window.layout.columns.length <= 1) ||
        (action === "remove-row" && target.window.layout.rows.length <= 1) ||
        Boolean(duplicateLocal);
    }
  };

  /**
   * Converts a toolbar action into one formal grid command.
   *
   * Stable cell IDs contain the parent window, resulting revision and a local
   * ordinal. The domain still validates global uniqueness before committing.
   */
  const executeGridAction = (action: GridAction): void => {
    const target = resolveGridTarget();
    if (!target) return;
    gridCommandSequence += 1;
    if (action === "equalize") {
      session.execute(
        createEqualizeWindowGridCommand({
          commandId: `CMD-GRID-EQUALIZE-${gridCommandSequence}`,
          windowId: target.window.objectId
        })
      );
      selection.select(target.window.objectId, "2d");
      return;
    }
    if (action === "local-vertical" || action === "local-horizontal") {
      const hostCell = target.window.layout.cells[
        target.row * target.window.layout.columns.length + target.column
      ];
      if (!hostCell) return;
      const orientation = action === "local-vertical" ? "vertical" : "horizontal";
      const duplicate = target.window.topology.members.find(
        (member) =>
          member.hostRegionId === hostCell.objectId &&
          member.orientation === orientation &&
          Math.abs(member.positionRatio - 0.5) < 0.015 &&
          Math.abs(member.span.startRatio) < 0.015 &&
          Math.abs(member.span.endRatio - 1) < 0.015
      );
      if (duplicate) {
        selection.select(duplicate.objectId, "2d");
        return;
      }
      const memberId = `${target.window.objectId}:MEMBER-${orientation === "vertical" ? "V" : "H"}-${currentDocument.revision + 1}-${gridCommandSequence}`;
      session.execute(
        createAddWindowTopologyMemberCommand({
          commandId: `CMD-TOPOLOGY-ADD-${gridCommandSequence}`,
          windowId: target.window.objectId,
          memberId,
          hostRegionId: hostCell.objectId,
          orientation
        })
      );
      selection.select(memberId as DesignObjectId, "2d");
      return;
    }
    const axis = action.includes("column") ? "column" : "row";
    if (action.startsWith("split")) {
      const index = axis === "column" ? target.column : target.row;
      const partCount = action.endsWith("-3") ? 3 : 2;
      const perpendicularCount =
        axis === "column"
          ? target.window.layout.rows.length
          : target.window.layout.columns.length;
      const newCellIds = Array.from(
        { length: perpendicularCount * (partCount - 1) },
        (_, offset) =>
          `${target.window.objectId}:CELL-GRID-${currentDocument.revision + 1}-${gridCommandSequence}-${offset + 1}`
      );
      session.execute(
        createSplitWindowGridCommand({
          commandId: `CMD-GRID-SPLIT-${gridCommandSequence}`,
          windowId: target.window.objectId,
          axis,
          index,
          partCount,
          newCellIds
        })
      );
      const updated = session.document.windows.find(
        (candidate) => candidate.objectId === target.window.objectId
      );
      const divider = updated
        ? resolveWindowGeometry(updated).members.find(
            (candidate) =>
              candidate.kind === "grid-divider" &&
              candidate.sourceComponentId ===
                `divider.${axis === "column" ? "v" : "h"}.${index + 1}`
          )
        : undefined;
      selection.select(divider?.objectId ?? target.window.objectId, "2d");
      return;
    }
    const tracks = axis === "column" ? target.window.layout.columns : target.window.layout.rows;
    if (tracks.length <= 1) return;
    session.execute(
      createRemoveLastWindowGridTrackCommand({
        commandId: `CMD-GRID-REMOVE-${gridCommandSequence}`,
        windowId: target.window.objectId,
        axis
      })
    );
    selection.select(target.window.objectId, "2d");
  };

  /**
   * Commits the final divider coordinate after the visual drag preview.
   *
   * Geometry displays grid tracks inside the frame, while the legacy command
   * stores an outer-axis ratio. The conversion removes frame faces first, then
   * maps the resulting 0..1 ratio to outer millimetres so the rebuilt divider
   * remains under the released pointer.
   */
  const commitDividerDrag = (drag: DividerDragState): void => {
    if (Math.abs(drag.currentAxisMm - drag.startAxisMm) < 1) {
      selection.select(drag.objectId as DesignObjectId, "2d");
      return;
    }
    const window = currentDocument.windows.find((candidate) => candidate.objectId === drag.windowId);
    if (!window) return;
    const axisSize = drag.axis === "column" ? window.widthMm : window.heightMm;
    const innerSize = Math.max(1, axisSize - window.frameFaceMm * 2);
    const ratio = Math.min(
      1,
      Math.max(0, (drag.currentAxisMm - window.frameFaceMm) / innerSize)
    );
    gridCommandSequence += 1;
    session.execute(
      createMoveWindowGridDividerCommand({
        commandId: `CMD-GRID-MOVE-${gridCommandSequence}`,
        windowId: window.objectId,
        axis: drag.axis,
        index: drag.index,
        positionMm: ratio * axisSize
      })
    );
    selection.select(drag.objectId as DesignObjectId, "2d");
  };

  /**
   * Commits a flying-mullion drag as one host-relative manufacturing position.
   *
   * The pointer coordinate is clamped by the domain, not by the shell. This
   * keeps mouse, touch and numeric edits identical and guarantees that a drag
   * produces one undo step plus one geometry/BOM recomputation.
   *
   * @param drag Completed temporary SVG translation.
   * @example Releasing 620mm from a cell's left edge submits `positionMm=620`.
   * @since 0.5.2
   * @modified 2026-09-17 - Added flying-mullion drag commit.
   */
  const commitMeetingMullionDrag = (drag: MeetingMullionDragState): void => {
    if (Math.abs(drag.currentPositionMm - drag.startPositionMm) < 1) {
      selection.select(drag.objectId as DesignObjectId, "2d");
      return;
    }
    const window = currentDocument.windows.find((candidate) => candidate.objectId === drag.windowId);
    if (!window) return;
    const host = resolveWindowGeometry(window).cells.find(
      (cell) => cell.objectId === drag.cellId
    );
    if (!host) return;
    gridCommandSequence += 1;
    session.execute(
      createMoveOpeningMeetingMullionCommand({
        commandId: `CMD-OPENING-MEETING-MOVE-${gridCommandSequence}`,
        windowId: window.objectId,
        cellId: host.objectId,
        positionMm: drag.currentPositionMm
      })
    );
    selection.select(drag.objectId as DesignObjectId, "2d");
  };

  /**
   * Commits one local-member drag using its shared host-cell geometry.
   *
   * Algorithm: locate the member and resolved host rectangle, convert the
   * released inner-geometry coordinate to a 0..1 ratio, multiply by the outer
   * host-track size expected by the legacy-compatible domain command, then
   * execute one immutable update.
   *
   * @param drag Completed presentation gesture.
   * @example Releasing at 60% of a 600mm host submits `positionMm: 360`.
   * @since 0.4.4
   * @modified 2026-09-17 - Added direct local-mullion dragging.
   */
  const commitTopologyMemberDrag = (drag: TopologyMemberDragState): void => {
    if (Math.abs(drag.currentAxisMm - drag.startAxisMm) < 1) {
      selection.select(drag.objectId as DesignObjectId, "2d");
      return;
    }
    const window = currentDocument.windows.find((candidate) => candidate.objectId === drag.windowId);
    const member = window?.topology.members.find(
      (candidate) => candidate.objectId === drag.objectId
    );
    if (!window || !member) return;
    const geometry = resolveWindowGeometry(window);
    const host = geometry.cells.find((cell) => cell.objectId === member.hostRegionId);
    const hostIndex = window.layout.cells.findIndex((cell) => cell.objectId === member.hostRegionId);
    if (!host || hostIndex < 0) return;
    const row = Math.floor(hostIndex / window.layout.columns.length);
    const column = hostIndex % window.layout.columns.length;
    const vertical = drag.orientation === "vertical";
    const innerStart = vertical ? host.xMm : host.yMm;
    const innerSize = Math.max(1, vertical ? host.widthMm : host.heightMm);
    const ratio = Math.min(1, Math.max(0, (drag.currentAxisMm - innerStart) / innerSize));
    const weights = vertical ? window.layout.columns : window.layout.rows;
    const trackIndex = vertical ? column : row;
    const axisSize = vertical ? window.widthMm : window.heightMm;
    const hostOuterSize =
      (axisSize * (weights[trackIndex] ?? 0)) /
      weights.reduce((sum, weight) => sum + weight, 0);
    gridCommandSequence += 1;
    session.execute(
      createMoveWindowTopologyMemberCommand({
        commandId: `CMD-TOPOLOGY-DRAG-${gridCommandSequence}`,
        windowId: window.objectId,
        memberId: member.objectId,
        positionMm: ratio * hostOuterSize
      })
    );
    selection.select(member.objectId, "2d");
  };

  /** Commits one clamped plan-view corner angle as a single undoable joint edit. */
  const commitCornerAngleDrag = (drag: CornerAngleDragState): void => {
    const assembly = (currentDocument.assemblies ?? []).find(
      (candidate) => candidate.objectId === drag.assemblyId
    );
    const joint = assembly?.joints.find((candidate) => candidate.objectId === drag.jointId);
    if (!assembly || !joint || joint.jointType !== "corner_joint" || !joint.cornerConfiguration) {
      renderTwoDimensional();
      return;
    }
    if (Math.abs(drag.currentAngleDeg - drag.startAngleDeg) < 0.05) {
      selection.select(joint.objectId, "2d");
      renderTwoDimensional();
      return;
    }
    try {
      jointCommandSequence += 1;
      session.execute(createUpdateEngineeringJointCommand({
        commandId: `CMD-ASSEMBLY-CORNER-DRAG-${currentDocument.revision}-${jointCommandSequence}`,
        assemblyId: assembly.objectId,
        jointId: joint.objectId,
        jointType: joint.jointType,
        gapMm: joint.gapMm,
        factoryScope: joint.factoryScope,
        cornerConfiguration: {
          ...joint.cornerConfiguration,
          includedAngleDeg: drag.currentAngleDeg
        },
        ...(joint.catalogSelection ? { catalogSelection: joint.catalogSelection } : {})
      }));
      selection.select(joint.objectId, "2d");
    } catch (error) {
      renderTwoDimensional();
      const message = error instanceof Error ? error.message : "转角角度更新失败。";
      if (typeof globalThis.alert === "function") globalThis.alert(message);
    }
  };

  /**
   * Hides the desktop member context menu and clears its stable target.
   *
   * Clearing metadata prevents a later click from addressing a member removed
   * by undo, another command or a cross-view selection change.
   *
   * @since 0.4.3
   * @modified 2026-09-17 - Added stale-target protection for right-click edits.
   */
  const hideMemberContextMenu = (): void => {
    memberContextMenu.hidden = true;
    memberContextMenu.dataset.windowId = "";
    memberContextMenu.dataset.memberKind = "";
    memberContextMenu.dataset.objectId = "";
    memberContextMenu.dataset.axis = "";
    memberContextMenu.dataset.index = "";
    contextEdit.hidden = false;
    contextEdit.textContent = "编辑位置";
    contextDelete.textContent = "删除构件";
  };

  /** Places the shared context overlay inside the visible 2D stage. */
  const positionContextMenu = (clientX: number, clientY: number): void => {
    const stage = memberContextMenu.parentElement?.getBoundingClientRect();
    if (!stage) return;
    memberContextMenu.style.left = `${Math.max(8, Math.min(stage.width - 164, clientX - stage.left + 10))}px`;
    memberContextMenu.style.top = `${Math.max(8, Math.min(stage.height - 112, clientY - stage.top + 10))}px`;
  };

  /**
   * Opens the prototype-compatible right-click menu for grid/topology members.
   *
   * Only stable IDs and command metadata are stored on the overlay. No SVG node
   * is retained, so a selection-triggered rerender cannot leave a stale target.
   */
  const openMemberContextMenu = (
    member: SVGElement,
    clientX: number,
    clientY: number
  ): void => {
    const windowId = member?.dataset.windowId;
    const objectId = member?.dataset.objectId;
    const memberKind = member?.dataset.memberKind;
    if (!member || !objectId || !memberKind || (memberKind !== "drawing-label" && !windowId)) {
      hideMemberContextMenu();
      return;
    }
    memberContextMenu.dataset.windowId = windowId ?? "";
    memberContextMenu.dataset.memberKind = memberKind;
    memberContextMenu.dataset.objectId = objectId;
    memberContextMenu.dataset.axis = member.dataset.dividerAxis ?? "";
    memberContextMenu.dataset.index = member.dataset.dividerIndex ?? "";
    if (memberKind === "drawing-label") {
      contextTitle.textContent = "文字标签";
      contextEdit.textContent = "编辑文字";
      contextDelete.textContent = "删除标签";
    } else {
      const orientationLabel = member.dataset.orientation === "vertical" ? "竖梃" : "横梃";
      contextTitle.textContent = `${memberKind === "grid-divider" ? "贯通" : "局部"}${orientationLabel}`;
      contextDelete.textContent = memberKind === "grid-divider" ? "删除贯通梃" : "删除局部梃";
    }
    contextEdit.hidden = false;
    positionContextMenu(clientX, clientY);
    memberContextMenu.hidden = false;
    selection.select(objectId as DesignObjectId, "2d");
  };

  /**
   * Opens a whole-window deletion menu from any rendered child of that window.
   *
   * Geometry remains immutable here: the stable window ID is stored and the
   * application command later updates/dissolves the containing assembly and its
   * joints atomically. Aggregate outlines and joints are intentionally rejected
   * by `resolveContextDeletableWindow` so a right-click can never silently pick
   * the first member of a combination.
   *
   * @since 0.10.52
   */
  const openWindowContextMenu = (
    window: WindowUnit,
    objectId: string,
    clientX: number,
    clientY: number
  ): void => {
    memberContextMenu.dataset.windowId = window.objectId;
    memberContextMenu.dataset.memberKind = "window";
    memberContextMenu.dataset.objectId = objectId;
    memberContextMenu.dataset.axis = "";
    memberContextMenu.dataset.index = "";
    contextTitle.textContent = `窗 · ${window.mark}`;
    contextEdit.hidden = true;
    contextDelete.textContent = "删除当前窗";
    positionContextMenu(clientX, clientY);
    memberContextMenu.hidden = false;
    selection.select(window.objectId, "2d");
  };

  /** Opens the shared member menu from a desktop secondary-click event. */
  const onCanvasContextMenu = (event: MouseEvent): void => {
    const hit = resolveDomCanvasInteractionHit(event.target);
    const intent = resolveCanvasInteractionIntent({
      activation: "context",
      hit: hit.semantic
    });
    if (intent.kind !== "context-menu") {
      hideMemberContextMenu();
      return;
    }
    if (hit.contextMember) {
      event.preventDefault();
      event.stopPropagation();
      openMemberContextMenu(hit.contextMember, event.clientX, event.clientY);
      return;
    }
    const designWindow = resolveContextDeletableWindow(currentDocument, intent.objectId);
    if (!designWindow) {
      hideMemberContextMenu();
      return;
    }
    event.preventDefault();
    event.stopPropagation();
    openWindowContextMenu(designWindow, intent.objectId, event.clientX, event.clientY);
  };

  /** Cancels a pending long-press without changing design or selection state. */
  const cancelMemberLongPress = (): void => {
    if (!memberLongPress) return;
    clearTimeout(memberLongPress.timer);
    memberLongPress = undefined;
  };

  /**
   * Starts the touch equivalent of a right-click for one rendered member.
   *
   * When the delay completes, any not-yet-committed drag preview is discarded
   * and the same menu function used by desktop is opened at the touch point.
   */
  const startMemberLongPress = (
    event: PointerEvent,
    member: SVGElement
  ): void => {
    cancelMemberLongPress();
    const pointerId = event.pointerId;
    const clientX = event.clientX;
    const clientY = event.clientY;
    const timer = setTimeout(() => {
      if (!memberLongPress || memberLongPress.pointerId !== pointerId) return;
      if (dividerDrag?.pointerId === pointerId) {
        dividerDrag.element.removeAttribute("transform");
        dividerDrag = undefined;
      }
      if (topologyMemberDrag?.pointerId === pointerId) {
        topologyMemberDrag.element.removeAttribute("transform");
        topologyMemberDrag = undefined;
      }
      twoDimensional.classList.remove("is-dragging-divider");
      memberLongPress = undefined;
      const hit = resolveDomCanvasInteractionHit(member);
      const intent = resolveCanvasInteractionIntent({
        activation: "context",
        hit: hit.semantic
      });
      if (intent.kind === "context-menu" && hit.contextMember) {
        openMemberContextMenu(hit.contextMember, clientX, clientY);
      }
    }, 550);
    memberLongPress = {
      pointerId,
      startClientX: clientX,
      startClientY: clientY,
      member,
      timer
    };
  };

  /**
   * Positions and opens the shared numeric editor for a member coordinate.
   *
   * Grid limits use the selected adjacent pair; local limits use the member's
   * host track. Both reuse the prototype's feasible 120mm edge clearance.
   *
   * @example A centred 1200mm divider opens with value 600 and limits 120..1080.
   * @since 0.4.3
   * @modified 2026-09-17 - Unified total-size and member-position editing UI.
   */
  const openMemberPositionEditor = (): void => {
    const kind = memberContextMenu.dataset.memberKind;
    const objectId = memberContextMenu.dataset.objectId;
    if (kind === "drawing-label" && objectId) {
      const label = (currentDocument.drawingTextLabels ?? []).find(
        (candidate) => candidate.objectId === objectId
      );
      if (label) openTextLabelEditor(label);
      hideMemberContextMenu();
      return;
    }
    const window = currentDocument.windows.find(
      (candidate) => candidate.objectId === memberContextMenu.dataset.windowId
    );
    if (!window || !objectId) return;
    let value = 0;
    let minimum = 1;
    let maximum = 1;
    if (kind === "grid-divider") {
      const axis = memberContextMenu.dataset.axis;
      const index = Number(memberContextMenu.dataset.index);
      if ((axis !== "column" && axis !== "row") || !Number.isInteger(index)) return;
      const weights = axis === "column" ? window.layout.columns : window.layout.rows;
      const axisSize = axis === "column" ? window.widthMm : window.heightMm;
      const total = weights.reduce((sum, weight) => sum + weight, 0);
      const sizes = weights.map((weight) => (axisSize * weight) / total);
      const pairStart = sizes.slice(0, index - 1).reduce((sum, size) => sum + size, 0);
      const pairTotal = (sizes[index - 1] ?? 0) + (sizes[index] ?? 0);
      const edge = Math.min(120, Math.max(1, pairTotal / 2 - 1));
      value = sizes.slice(0, index).reduce((sum, size) => sum + size, 0);
      minimum = pairStart + edge;
      maximum = pairStart + pairTotal - edge;
      dimensionEditor.dataset.dimensionKind = "grid-divider";
      dimensionEditor.dataset.axis = axis;
      dimensionEditor.dataset.index = String(index);
      dimensionLabel.textContent = `贯通${axis === "column" ? "竖梃" : "横梃"}位置 mm`;
    } else if (kind === "topology-member") {
      const member = window.topology.members.find((candidate) => candidate.objectId === objectId);
      const cellIndex = window.layout.cells.findIndex(
        (cell) => cell.objectId === member?.hostRegionId
      );
      if (!member || cellIndex < 0) return;
      const row = Math.floor(cellIndex / window.layout.columns.length);
      const column = cellIndex % window.layout.columns.length;
      const weights = member.orientation === "vertical" ? window.layout.columns : window.layout.rows;
      const trackIndex = member.orientation === "vertical" ? column : row;
      const axisSize = member.orientation === "vertical" ? window.widthMm : window.heightMm;
      const hostSize =
        (axisSize * (weights[trackIndex] ?? 0)) /
        weights.reduce((sum, weight) => sum + weight, 0);
      const edge = Math.min(
        Math.max(120, hostSize * 0.08),
        Math.max(1, hostSize / 2 - 1)
      );
      value = hostSize * member.positionRatio;
      minimum = edge;
      maximum = hostSize - edge;
      dimensionEditor.dataset.dimensionKind = "topology-member";
      dimensionEditor.dataset.memberId = member.objectId;
      dimensionLabel.textContent = `局部${member.orientation === "vertical" ? "竖梃" : "横梃"}位置 mm`;
    } else {
      return;
    }
    dimensionEditor.dataset.windowId = window.objectId;
    dimensionInput.min = String(Math.ceil(minimum));
    dimensionInput.max = String(Math.floor(maximum));
    dimensionInput.value = String(Math.round(value));
    dimensionEditor.style.left = memberContextMenu.style.left;
    dimensionEditor.style.top = memberContextMenu.style.top;
    hideMemberContextMenu();
    dimensionEditor.hidden = false;
    dimensionInput.focus();
    dimensionInput.select();
  };

  /**
   * Deletes the context target through merge/remove domain commands.
   *
   * A grid divider merges adjacent tracks and remaps hosts; an explicit member
   * removes only itself. No SVG nodes or arrays are mutated directly.
   *
   * @since 0.4.3
   * @modified 2026-09-17 - Added prototype-compatible right-click deletion.
   */
  const deleteContextMember = (): void => {
    const kind = memberContextMenu.dataset.memberKind;
    const objectId = memberContextMenu.dataset.objectId;
    if (kind === "drawing-label" && objectId) {
      textLabelCommandSequence += 1;
      try {
        session.execute(createDeleteDrawingTextLabelCommand({
          commandId: `CMD-DRAWING-TEXT-DELETE-${currentDocument.revision}-${textLabelCommandSequence}`,
          labelId: objectId
        }));
        selection.select(undefined, "system");
        closeTextLabelEditor();
      } catch (error) {
        const message = error instanceof Error ? error.message : "删除文字标签失败。";
        if (typeof globalThis.alert === "function") globalThis.alert(message);
      }
      hideMemberContextMenu();
      return;
    }
    const window = currentDocument.windows.find(
      (candidate) => candidate.objectId === memberContextMenu.dataset.windowId
    );
    if (!window || !objectId) return;
    gridCommandSequence += 1;
    if (kind === "window") {
      const confirmed = typeof globalThis.confirm !== "function" || globalThis.confirm(
        `确认删除窗“${window.mark}”？组合关系和连接件会同步更新，此操作可撤销。`
      );
      if (!confirmed) return;
      try {
        session.execute(createDeleteWindowCommand({
          commandId: `CMD-CONTEXT-WINDOW-DELETE-${session.document.revision}-${gridCommandSequence}`,
          windowId: window.objectId
        }));
        selection.select(undefined, "system");
      } catch (error) {
        const message = error instanceof Error ? error.message : "删除窗失败。";
        if (typeof globalThis.alert === "function") globalThis.alert(message);
      }
    } else if (kind === "grid-divider") {
      const axis = memberContextMenu.dataset.axis;
      const index = Number(memberContextMenu.dataset.index);
      if ((axis !== "column" && axis !== "row") || !Number.isInteger(index)) return;
      session.execute(
        createMergeWindowGridDividerCommand({
          commandId: `CMD-GRID-MERGE-${gridCommandSequence}`,
          windowId: window.objectId,
          axis,
          index
        })
      );
      selection.select(window.objectId, "2d");
    } else if (kind === "topology-member") {
      const member = window.topology.members.find((candidate) => candidate.objectId === objectId);
      if (!member) return;
      session.execute(
        createRemoveWindowTopologyMemberCommand({
          commandId: `CMD-TOPOLOGY-REMOVE-${gridCommandSequence}`,
          windowId: window.objectId,
          memberId: member.objectId
        })
      );
      selection.select(member.hostRegionId, "2d");
    }
    hideMemberContextMenu();
  };

  /**
   * Closes the menu when pointer focus moves elsewhere in the shared workspace.
   *
   * @example Clicking the 3D tab dismisses a previously opened 2D member menu.
   * @since 0.4.3
   * @modified 2026-09-17 - Added predictable context-menu dismissal.
   */
  const onWorkspacePointerDown = (event: PointerEvent): void => {
    if (!(event.target instanceof Element) || !event.target.closest("[data-member-context-menu]")) {
      hideMemberContextMenu();
    }
  };

  /** Applies the prototype's pointer-centred 1.12 wheel zoom step. */
  const onCanvasWheel = (event: WheelEvent): void => {
    const point = pointInSvg(event);
    if (!point) return;
    event.preventDefault();
    canvasView.zoomAt(point.x, point.y, event.deltaY < 0 ? 1.12 : 1 / 1.12);
  };

  /**
   * Starts a pending drag or view-pan without sacrificing object selection.
   *
   * A press on an explicit member handle is reserved for geometry movement.
   * Every other drawing surface, including glass, sash and dimensions, may pan
   * after the shared threshold; a release below the threshold remains a normal
   * click or double-click. This lets a selected double sash be used as a large
   * navigation surface instead of forcing the user to find outside whitespace.
   *
   * @example Dragging 10px on sash glass pans; clicking it still selects the cell.
   * @since 0.4.2
   * @modified 2026-09-20 - Enabled thresholded pan from ordinary objects.
   */
  const onCanvasPointerDown = (event: PointerEvent): void => {
    // CSS disables native SVG text selection. Do not cancel an ordinary mouse
    // press here: doing so suppresses the browser's compatibility `dblclick`
    // event and makes dimension labels appear non-editable. Actual member
    // drags and touch gestures still cancel native handling in their branches.
    // @modified 2026-09-20 - Restored double-click editing on SVG labels.
    const hit = resolveDomCanvasInteractionHit(event.target);
    const beginsOnDragHandle = hit.semantic.kind === "drag-handle";
    if (event.button === 0 || event.pointerType === "touch") {
      canvasPointerPresses.set(event.pointerId, {
        hit: hit.semantic,
        startClientX: event.clientX,
        startClientY: event.clientY,
        activated: false
      });
    }
    if (event.pointerType === "touch") {
      if (memberLongPress && memberLongPress.pointerId !== event.pointerId) {
        cancelMemberLongPress();
      }
      touchCanvasGesture.pointerDown(
        event.pointerId,
        event.clientX,
        event.clientY,
        beginsOnDragHandle ? "object" : "canvas"
      );
    }
    const textLabelElement = hit.dragHandle?.matches(".design-text-label")
      ? hit.dragHandle as SVGGElement
      : undefined;
    const textLabelId = textLabelElement?.dataset.objectId;
    const textLabel = (currentDocument.drawingTextLabels ?? []).find(
      (candidate) => candidate.objectId === textLabelId
    );
    const textLabelScreenMatrix = textLabelElement?.getScreenCTM();
    const textLabelRenderScale = Number(textLabelElement?.dataset.renderScale);
    const textLabelPixelsPerSvgUnit = textLabelScreenMatrix
      ? Math.hypot(textLabelScreenMatrix.a, textLabelScreenMatrix.b)
      : Number.NaN;
    const textLabelPixelsPerMm = textLabelPixelsPerSvgUnit * textLabelRenderScale;
    const textLabelStartSvgX = Number(textLabelElement?.dataset.labelXSvg);
    const textLabelStartSvgY = Number(textLabelElement?.dataset.labelYSvg);
    if (
      event.button === 0 &&
      textLabelElement &&
      textLabel &&
      Number.isFinite(textLabelPixelsPerMm) &&
      textLabelPixelsPerMm > 0 &&
      Number.isFinite(textLabelStartSvgX) &&
      Number.isFinite(textLabelStartSvgY)
    ) {
      if (event.pointerType === "touch") {
        startMemberLongPress(event, textLabelElement);
        event.preventDefault();
      }
      event.stopPropagation();
      drawingTextLabelDrag = {
        pointerId: event.pointerId,
        element: textLabelElement,
        labelId: textLabel.objectId,
        startClientX: event.clientX,
        startClientY: event.clientY,
        startXMm: textLabel.xMm,
        startYMm: textLabel.yMm,
        startSvgX: textLabelStartSvgX,
        startSvgY: textLabelStartSvgY,
        pixelsPerMm: textLabelPixelsPerMm,
        renderScale: textLabelRenderScale,
        currentXMm: textLabel.xMm,
        currentYMm: textLabel.yMm,
        activated: false
      };
      if (event.pointerType === "touch") twoDimensional.setPointerCapture?.(event.pointerId);
      return;
    }
    const cornerHandle = hit.dragHandle?.matches("[data-corner-angle-handle]")
      ? hit.dragHandle
      : undefined;
    const cornerGroup = cornerHandle?.closest<SVGGElement>(
      ".design-assembly-plan-view__corner-angle"
    );
    const cornerTurnDirection = cornerGroup?.dataset.turnDirection;
    const cornerValues = cornerGroup
      ? {
          axisX: Number(cornerGroup.dataset.axisX),
          axisY: Number(cornerGroup.dataset.axisY),
          firstRayAngleRadians: Number(cornerGroup.dataset.firstRayAngleRad),
          radius: Number(cornerGroup.dataset.radius),
          minimumAngleDeg: Number(cornerGroup.dataset.minAngleDeg),
          maximumAngleDeg: Number(cornerGroup.dataset.maxAngleDeg),
          startAngleDeg: Number(cornerGroup.dataset.includedAngleDeg)
        }
      : undefined;
    if (
      event.button === 0 &&
      cornerHandle &&
      cornerGroup &&
      cornerGroup.dataset.assemblyId &&
      cornerGroup.dataset.jointId &&
      (cornerTurnDirection === "clockwise" || cornerTurnDirection === "counterclockwise") &&
      cornerValues &&
      Object.values(cornerValues).every(Number.isFinite)
    ) {
      event.preventDefault();
      event.stopPropagation();
      cornerAngleDrag = {
        pointerId: event.pointerId,
        group: cornerGroup,
        assemblyId: cornerGroup.dataset.assemblyId,
        jointId: cornerGroup.dataset.jointId,
        turnDirection: cornerTurnDirection,
        ...cornerValues,
        currentAngleDeg: cornerValues.startAngleDeg,
        activated: false
      };
      twoDimensional.setPointerCapture?.(event.pointerId);
      return;
    }
    const divider = hit.dragHandle?.matches(".design-window__member--grid-divider")
      ? hit.dragHandle as SVGRectElement
      : undefined;
    const dividerAxis = divider?.dataset.dividerAxis;
    const dividerIndex = Number(divider?.dataset.dividerIndex);
    const windowId = divider?.dataset.windowId;
    const objectId = divider?.dataset.objectId;
    const localPoint = divider ? pointInWindowMm(event, divider) : undefined;
    if (
      event.button === 0 &&
      divider &&
      windowId &&
      objectId &&
      (dividerAxis === "column" || dividerAxis === "row") &&
      Number.isInteger(dividerIndex) &&
      dividerIndex > 0 &&
      localPoint
    ) {
      if (event.pointerType === "touch") startMemberLongPress(event, divider);
      event.preventDefault();
      event.stopPropagation();
      const startAxisMm = dividerAxis === "column" ? localPoint.x : localPoint.y;
      const group = divider.closest<SVGGElement>(".design-window");
      dividerDrag = {
        pointerId: event.pointerId,
        element: divider,
        windowId,
        objectId,
        axis: dividerAxis,
        index: dividerIndex,
        startAxisMm,
        currentAxisMm: startAxisMm,
        renderScale: Number(group?.dataset.renderScale) || 1,
        startClientX: event.clientX,
        startClientY: event.clientY,
        activated: false
      };
      twoDimensional.setPointerCapture?.(event.pointerId);
      return;
    }
    const meetingMullion = hit.dragHandle?.matches(".design-window__meeting-mullion")
      ? hit.dragHandle as SVGRectElement
      : undefined;
    const meetingWindowId = meetingMullion?.dataset.windowId;
    const meetingCellId = meetingMullion?.dataset.cellId;
    const meetingObjectId = meetingMullion?.dataset.objectId;
    const meetingWindow = currentDocument.windows.find(
      (candidate) => candidate.objectId === meetingWindowId
    );
    const meetingGeometry = meetingWindow ? resolveWindowGeometry(meetingWindow) : undefined;
    const meetingHost = meetingGeometry?.cells.find((cell) => cell.objectId === meetingCellId);
    const resolvedMeeting = meetingGeometry?.meetingMullions.find(
      (candidate) => candidate.sourceObjectId === meetingCellId
    );
    const meetingGroup = meetingMullion?.closest<SVGGElement>(".design-window");
    const meetingScreenMatrix = meetingGroup?.getScreenCTM();
    const meetingRenderScale = Number(meetingGroup?.dataset.renderScale);
    const pixelsPerSvgUnit = meetingScreenMatrix
      ? Math.hypot(meetingScreenMatrix.a, meetingScreenMatrix.b)
      : Number.NaN;
    const meetingPixelsPerMm = meetingRenderScale * pixelsPerSvgUnit;
    if (
      event.button === 0 &&
      meetingMullion &&
      meetingWindowId &&
      meetingCellId &&
      meetingObjectId &&
      meetingHost &&
      resolvedMeeting &&
      Number.isFinite(meetingPixelsPerMm) &&
      meetingPixelsPerMm > 0
    ) {
      event.preventDefault();
      event.stopPropagation();
      meetingMullionDrag = {
        pointerId: event.pointerId,
        element: meetingMullion,
        windowId: meetingWindowId,
        cellId: meetingCellId,
        objectId: meetingObjectId,
        startClientX: event.clientX,
        startClientY: event.clientY,
        startPositionMm: resolvedMeeting.positionRatio * meetingHost.widthMm,
        currentPositionMm: resolvedMeeting.positionRatio * meetingHost.widthMm,
        pixelsPerMm: meetingPixelsPerMm,
        renderScale: meetingRenderScale,
        activated: false
      };
      twoDimensional.setPointerCapture?.(event.pointerId);
      return;
    }
    const topologyMember = hit.dragHandle?.matches(".design-window__member--topology-member")
      ? hit.dragHandle as SVGRectElement
      : undefined;
    const topologyOrientation = topologyMember?.dataset.orientation;
    const topologyWindowId = topologyMember?.dataset.windowId;
    const topologyObjectId = topologyMember?.dataset.objectId;
    const topologyPoint = topologyMember ? pointInWindowMm(event, topologyMember) : undefined;
    if (
      event.button === 0 &&
      topologyMember &&
      topologyWindowId &&
      topologyObjectId &&
      (topologyOrientation === "vertical" || topologyOrientation === "horizontal") &&
      topologyPoint
    ) {
      if (event.pointerType === "touch") startMemberLongPress(event, topologyMember);
      event.preventDefault();
      event.stopPropagation();
      const startAxisMm = topologyOrientation === "vertical" ? topologyPoint.x : topologyPoint.y;
      const group = topologyMember.closest<SVGGElement>(".design-window");
      topologyMemberDrag = {
        pointerId: event.pointerId,
        element: topologyMember,
        windowId: topologyWindowId,
        objectId: topologyObjectId,
        orientation: topologyOrientation,
        startAxisMm,
        currentAxisMm: startAxisMm,
        renderScale: Number(group?.dataset.renderScale) || 1,
        startClientX: event.clientX,
        startClientY: event.clientY,
        activated: false
      };
      twoDimensional.setPointerCapture?.(event.pointerId);
      return;
    }
    if (event.pointerType === "touch" && !beginsOnDragHandle) {
      event.preventDefault();
      canvasPan = {
        pointerId: event.pointerId,
        startClientX: event.clientX,
        startClientY: event.clientY,
        originX: currentCanvasView.x,
        originY: currentCanvasView.y,
        activated: false
      };
      twoDimensional.setPointerCapture?.(event.pointerId);
      return;
    }
    if (
      event.button !== 0 ||
      beginsOnDragHandle
    ) {
      return;
    }
    canvasPan = {
      pointerId: event.pointerId,
      startClientX: event.clientX,
      startClientY: event.clientY,
      originX: currentCanvasView.x,
      originY: currentCanvasView.y,
      activated: false
    };
  };

  /**
   * Resolves total movement for the original semantic hit and arms click suppression.
   *
   * The first movement crossing the shared threshold is the only transition
   * from pending click to drag/pan. Subsequent events keep the same activation
   * until pointer-up, even when the pointer moves back near its origin.
   *
   * @param event Current pointer move in browser client coordinates.
   * @returns Shared drag/pan intent or `none` while the gesture remains pending.
   * @example A divider moved 8px yields `drag`; the same divider moved 2px yields `none`.
   * @since 0.10.28
   * @modified 2026-09-20 - Centralized threshold crossing for all 2D drag types.
   */
  const pointerMoveIntent = (event: PointerEvent) => {
    const press = canvasPointerPresses.get(event.pointerId);
    if (!press) return { kind: "none" } as const;
    const intent = resolveCanvasInteractionIntent({
      activation: "move",
      hit: press.hit,
      movementPx: press.activated
        ? 0
        : Math.hypot(
            event.clientX - press.startClientX,
            event.clientY - press.startClientY
          ),
      // An activated gesture stays activated even if the pointer moves back
      // near its origin. This also keeps object-origin pans flowing on every
      // frame instead of producing one update and then appearing frozen.
      movementThresholdPx: press.activated ? 0 : undefined
    });
    if (intent.kind === "drag" || intent.kind === "pan") {
      press.activated = true;
      suppressUpcomingCanvasClick();
    }
    return intent;
  };

  const onCanvasPointerMove = (event: PointerEvent): void => {
    if (
      memberLongPress?.pointerId === event.pointerId &&
      Math.hypot(
        event.clientX - memberLongPress.startClientX,
        event.clientY - memberLongPress.startClientY
      ) > 8
    ) {
      cancelMemberLongPress();
    }
    if (drawingTextLabelDrag && drawingTextLabelDrag.pointerId === event.pointerId) {
      const intent = pointerMoveIntent(event);
      if (intent.kind !== "drag") return;
      if (!drawingTextLabelDrag.activated) {
        drawingTextLabelDrag.activated = true;
        cancelMemberLongPress();
        event.preventDefault();
        twoDimensional.setPointerCapture?.(event.pointerId);
        twoDimensional.classList.add("is-dragging-text-label");
      }
      const deltaXMm = (event.clientX - drawingTextLabelDrag.startClientX) /
        drawingTextLabelDrag.pixelsPerMm;
      const deltaYMm = (event.clientY - drawingTextLabelDrag.startClientY) /
        drawingTextLabelDrag.pixelsPerMm;
      drawingTextLabelDrag.currentXMm = drawingTextLabelDrag.startXMm + deltaXMm;
      drawingTextLabelDrag.currentYMm = drawingTextLabelDrag.startYMm + deltaYMm;
      drawingTextLabelDrag.element.setAttribute(
        "transform",
        `translate(${drawingTextLabelDrag.startSvgX + deltaXMm * drawingTextLabelDrag.renderScale} ` +
        `${drawingTextLabelDrag.startSvgY + deltaYMm * drawingTextLabelDrag.renderScale})`
      );
      return;
    }
    if (cornerAngleDrag && cornerAngleDrag.pointerId === event.pointerId) {
      const intent = pointerMoveIntent(event);
      if (intent.kind !== "drag") return;
      if (!cornerAngleDrag.activated) {
        cornerAngleDrag.activated = true;
        twoDimensional.classList.add("is-dragging-corner-angle");
      }
      const point = pointInSvgGroup(event, cornerAngleDrag.group);
      if (!point || Math.hypot(
        point.x - cornerAngleDrag.axisX,
        point.y - cornerAngleDrag.axisY
      ) < 0.001) return;
      const preview = calculateCornerAngleDrag({
        axisX: cornerAngleDrag.axisX,
        axisY: cornerAngleDrag.axisY,
        pointerX: point.x,
        pointerY: point.y,
        firstRayAngleRadians: cornerAngleDrag.firstRayAngleRadians,
        turnDirection: cornerAngleDrag.turnDirection,
        minimumAngleDeg: cornerAngleDrag.minimumAngleDeg,
        maximumAngleDeg: cornerAngleDrag.maximumAngleDeg,
        radius: cornerAngleDrag.radius
      });
      cornerAngleDrag.currentAngleDeg = preview.includedAngleDeg;
      previewCornerAngleDrag(cornerAngleDrag, preview);
      return;
    }
    if (dividerDrag && dividerDrag.pointerId === event.pointerId) {
      const intent = pointerMoveIntent(event);
      if (intent.kind !== "drag") return;
      if (!dividerDrag.activated) {
        dividerDrag.activated = true;
        cancelMemberLongPress();
        twoDimensional.classList.add("is-dragging-divider");
      }
      const point = pointInWindowMm(event, dividerDrag.element);
      if (!point) return;
      dividerDrag.currentAxisMm = dividerDrag.axis === "column" ? point.x : point.y;
      const delta =
        (dividerDrag.currentAxisMm - dividerDrag.startAxisMm) * dividerDrag.renderScale;
      dividerDrag.element.setAttribute(
        "transform",
        dividerDrag.axis === "column" ? `translate(${delta} 0)` : `translate(0 ${delta})`
      );
      return;
    }
    if (meetingMullionDrag && meetingMullionDrag.pointerId === event.pointerId) {
      const intent = pointerMoveIntent(event);
      if (intent.kind !== "drag") return;
      if (!meetingMullionDrag.activated) {
        meetingMullionDrag.activated = true;
        twoDimensional.classList.add("is-dragging-divider");
      }
      // Client-pixel delta is converted with the element's screen CTM captured
      // at pointer-down. Reusing `pointInWindowMm` here double-applies the SVG
      // viewBox scale and made a 20px gesture jump by several hundred mm.
      const update = calculateMeetingMullionDrag({
        clientX: event.clientX,
        startClientX: meetingMullionDrag.startClientX,
        startPositionMm: meetingMullionDrag.startPositionMm,
        pixelsPerMm: meetingMullionDrag.pixelsPerMm,
        renderScale: meetingMullionDrag.renderScale
      });
      meetingMullionDrag.currentPositionMm = update.positionMm;
      meetingMullionDrag.element.setAttribute("transform", `translate(${update.svgDelta} 0)`);
      return;
    }
    if (topologyMemberDrag && topologyMemberDrag.pointerId === event.pointerId) {
      const intent = pointerMoveIntent(event);
      if (intent.kind !== "drag") return;
      if (!topologyMemberDrag.activated) {
        topologyMemberDrag.activated = true;
        cancelMemberLongPress();
        twoDimensional.classList.add("is-dragging-divider");
      }
      const point = pointInWindowMm(event, topologyMemberDrag.element);
      if (!point) return;
      topologyMemberDrag.currentAxisMm =
        topologyMemberDrag.orientation === "vertical" ? point.x : point.y;
      const delta =
        (topologyMemberDrag.currentAxisMm - topologyMemberDrag.startAxisMm) *
        topologyMemberDrag.renderScale;
      topologyMemberDrag.element.setAttribute(
        "transform",
        topologyMemberDrag.orientation === "vertical"
          ? `translate(${delta} 0)`
          : `translate(0 ${delta})`
      );
      return;
    }
    if (event.pointerType === "touch") {
      const intent = pointerMoveIntent(event);
      const update = touchCanvasGesture.pointerMove(event.pointerId, event.clientX, event.clientY);
      if (!update || intent.kind !== "pan") return;
      if (canvasPan && !canvasPan.activated) canvasPan.activated = true;
      twoDimensional.classList.add("is-panning");
      const svg = twoDimensional.querySelector<SVGSVGElement>("svg");
      if (!svg) return;
      const bounds = svg.getBoundingClientRect();
      const viewBox = svg.viewBox.baseVal;
      canvasView.panTo(
        canvasView.state.x + (update.panDeltaX * viewBox.width) / Math.max(1, bounds.width),
        canvasView.state.y + (update.panDeltaY * viewBox.height) / Math.max(1, bounds.height)
      );
      if (Math.abs(update.zoomFactor - 1) > 0.0001) {
        const anchor = pointInSvg({
          clientX: update.anchorClientX,
          clientY: update.anchorClientY
        });
        if (anchor) canvasView.zoomAt(anchor.x, anchor.y, update.zoomFactor);
      }
      return;
    }
    if (!canvasPan || canvasPan.pointerId !== event.pointerId) return;
    const intent = pointerMoveIntent(event);
    if (intent.kind !== "pan") return;
    if (!canvasPan.activated) {
      canvasPan.activated = true;
      // Capture only after the gesture is confirmed as a pan. Capturing on
      // pointer-down retargets the later click/dblclick to the SVG root and
      // loses the dimension/object hit that must open its editor.
      twoDimensional.setPointerCapture?.(event.pointerId);
      twoDimensional.classList.add("is-panning");
    }
    const svg = twoDimensional.querySelector<SVGSVGElement>("svg");
    if (!svg) return;
    const bounds = svg.getBoundingClientRect();
    const viewBox = svg.viewBox.baseVal;
    canvasView.panTo(
      canvasPan.originX + ((event.clientX - canvasPan.startClientX) * viewBox.width) / Math.max(1, bounds.width),
      canvasPan.originY + ((event.clientY - canvasPan.startClientY) * viewBox.height) / Math.max(1, bounds.height)
    );
  };
  const onCanvasPointerEnd = (event: PointerEvent): void => {
    const completedPress = canvasPointerPresses.get(event.pointerId);
    canvasPointerPresses.delete(event.pointerId);
    if (completedPress?.activated) scheduleCanvasClickSuppressionRelease();
    if (memberLongPress?.pointerId === event.pointerId) cancelMemberLongPress();
    if (event.pointerType === "touch") touchCanvasGesture.pointerUp(event.pointerId);
    if (drawingTextLabelDrag && drawingTextLabelDrag.pointerId === event.pointerId) {
      const completed = drawingTextLabelDrag;
      drawingTextLabelDrag = undefined;
      twoDimensional.classList.remove("is-dragging-text-label");
      if (twoDimensional.hasPointerCapture?.(event.pointerId)) {
        twoDimensional.releasePointerCapture?.(event.pointerId);
      }
      const label = (currentDocument.drawingTextLabels ?? []).find(
        (candidate) => candidate.objectId === completed.labelId
      );
      if (event.type === "pointerup" && completed.activated && label) {
        textLabelCommandSequence += 1;
        try {
          session.execute(createUpdateDrawingTextLabelCommand({
            commandId: `CMD-DRAWING-TEXT-MOVE-${currentDocument.revision}-${textLabelCommandSequence}`,
            label: {
              ...label,
              xMm: completed.currentXMm,
              yMm: completed.currentYMm
            }
          }));
          selection.select(label.objectId, "2d");
        } catch (error) {
          renderTwoDimensional();
          const message = error instanceof Error ? error.message : "移动文字标签失败。";
          if (typeof globalThis.alert === "function") globalThis.alert(message);
        }
      } else if (completed.activated) {
        renderTwoDimensional();
      }
      return;
    }
    if (cornerAngleDrag && cornerAngleDrag.pointerId === event.pointerId) {
      const completed = cornerAngleDrag;
      cornerAngleDrag = undefined;
      twoDimensional.classList.remove("is-dragging-corner-angle");
      if (twoDimensional.hasPointerCapture?.(event.pointerId)) {
        twoDimensional.releasePointerCapture?.(event.pointerId);
      }
      if (event.type === "pointerup" && completed.activated) {
        commitCornerAngleDrag(completed);
      } else if (completed.activated) {
        renderTwoDimensional();
      }
      return;
    }
    if (dividerDrag && dividerDrag.pointerId === event.pointerId) {
      const completed = dividerDrag;
      dividerDrag = undefined;
      completed.element.removeAttribute("transform");
      twoDimensional.classList.remove("is-dragging-divider");
      twoDimensional.releasePointerCapture?.(event.pointerId);
      if (event.type === "pointerup" && completed.activated) commitDividerDrag(completed);
      return;
    }
    if (meetingMullionDrag && meetingMullionDrag.pointerId === event.pointerId) {
      const completed = meetingMullionDrag;
      meetingMullionDrag = undefined;
      completed.element.removeAttribute("transform");
      twoDimensional.classList.remove("is-dragging-divider");
      twoDimensional.releasePointerCapture?.(event.pointerId);
      if (event.type === "pointerup" && completed.activated) {
        commitMeetingMullionDrag(completed);
      }
      return;
    }
    if (topologyMemberDrag && topologyMemberDrag.pointerId === event.pointerId) {
      const completed = topologyMemberDrag;
      topologyMemberDrag = undefined;
      completed.element.removeAttribute("transform");
      twoDimensional.classList.remove("is-dragging-divider");
      twoDimensional.releasePointerCapture?.(event.pointerId);
      if (event.type === "pointerup" && completed.activated) {
        commitTopologyMemberDrag(completed);
      }
      return;
    }
    if (event.pointerType === "touch") {
      if (touchCanvasGesture.activePointerCount === 0) {
        canvasPan = undefined;
        twoDimensional.classList.remove("is-panning");
      }
      twoDimensional.releasePointerCapture?.(event.pointerId);
      return;
    }
    if (!canvasPan || canvasPan.pointerId !== event.pointerId) return;
    canvasPan = undefined;
    twoDimensional.classList.remove("is-panning");
    if (twoDimensional.hasPointerCapture?.(event.pointerId)) {
      twoDimensional.releasePointerCapture?.(event.pointerId);
    }
  };

  /** Hides the dimension editor and clears its command target. */
  const closeDimensionEditor = (): void => {
    dimensionEditor.hidden = true;
    dimensionEditor.dataset.windowId = "";
    dimensionEditor.dataset.dimensionKind = "";
    dimensionEditor.dataset.axis = "";
    dimensionEditor.dataset.index = "";
    dimensionEditor.dataset.memberId = "";
    dimensionEditor.dataset.cellId = "";
    dimensionEditor.dataset.measurementRole = "";
    dimensionEditor.dataset.clearSpanSide = "";
    dimensionEditor.dataset.edge = "";
    dimensionInput.min = "300";
    dimensionInput.max = "30000";
  };

  /**
   * Opens the numeric editor for a total, region or meeting-line dimension.
   *
   * Region labels retain their cell identity. A one-track region maps its inner
   * size to outer window size; a multi-track region maps the requested width or
   * height to its adjacent divider. All four dimensions around a double sash
   * map back to the same meeting line through an explicit measurement role.
   *
   * @param target Dimension group carrying renderer-authored stable metadata.
   * @param event Pointer or keyboard activation used to place the popover.
   * @example Double-clicking “右内 840 mm” edits the right side while selecting the mullion.
   * @since 0.4.1
   * @modified 2026-09-20 - Added connected dimension groups and region-size editing.
   */
  const openDimensionEditor = (target: SVGElement, event: MouseEvent | KeyboardEvent): void => {
    const windowId = target.dataset.windowId;
    const kind = target.dataset.dimensionKind;
    const window = currentDocument.windows.find((candidate) => candidate.objectId === windowId);
    if (!window) return;
    let value: number;
    if (kind === "width" || kind === "height") {
      value = kind === "width" ? window.widthMm : window.heightMm;
      dimensionInput.min = "300";
      dimensionInput.max = "30000";
      dimensionLabel.textContent = kind === "width" ? "外宽 mm" : "外高 mm";
    } else if (kind === "surround-outside-width") {
      const installation = normalizeWindowInstallation(window.installation);
      value = installation.surround.outsideWidthMm;
      dimensionInput.min = "0";
      dimensionInput.max = "500";
      dimensionLabel.textContent = "外包边宽 mm";
    } else if (kind === "surround-outer-width") {
      const installation = normalizeWindowInstallation(window.installation);
      const outsideVisible = installation.surround.enabled &&
        (installation.surround.styleId === "both_sides" ||
          installation.surround.styleId === "outside_only");
      const horizontalSideCount = outsideVisible
        ? Number(installation.surround.sides.includes("left")) +
          Number(installation.surround.sides.includes("right"))
        : 0;
      if (horizontalSideCount === 0) return;
      value = window.widthMm +
        installation.surround.outsideWidthMm * horizontalSideCount;
      dimensionInput.min = String(window.widthMm);
      dimensionInput.max = String(window.widthMm + 500 * horizontalSideCount);
      dimensionLabel.textContent = "包边外廓总宽 mm";
    } else if (kind === "cell-width" || kind === "cell-height") {
      const cellId = target.dataset.cellId;
      const axis = kind === "cell-width" ? "column" : "row";
      const edge = target.dataset.edge;
      const index = Number(target.dataset.index);
      const geometry = resolveWindowGeometry(window);
      const host = geometry.cells.find((cell) => cell.objectId === cellId);
      if (
        !cellId ||
        !host ||
        target.dataset.axis !== axis ||
        (edge !== "start" && edge !== "end") ||
        !Number.isInteger(index)
      ) return;
      value = axis === "column" ? host.widthMm : host.heightMm;
      const tracks = axis === "column" ? window.layout.columns : window.layout.rows;
      const axisSize = axis === "column" ? window.widthMm : window.heightMm;
      const innerSize = Math.max(1, axisSize - window.frameFaceMm * 2);
      if (tracks.length === 1) {
        dimensionInput.min = String(Math.max(1, 300 - window.frameFaceMm * 2));
        dimensionInput.max = String(30_000 - window.frameFaceMm * 2);
      } else {
        const totalWeight = tracks.reduce((sum, weight) => sum + weight, 0) || 1;
        const outerSizes = tracks.map((weight) => axisSize * weight / totalWeight);
        const pairTotalOuter = (outerSizes[index - 1] ?? 0) + (outerSizes[index] ?? 0);
        const minimumOuter = Math.min(120, Math.max(1, pairTotalOuter / 2 - 1));
        const minimumInner = minimumOuter / axisSize * innerSize;
        const pairTotalInner = pairTotalOuter / axisSize * innerSize;
        dimensionInput.min = String(Math.ceil(minimumInner));
        dimensionInput.max = String(Math.max(Math.ceil(minimumInner), Math.floor(pairTotalInner - minimumInner)));
      }
      dimensionEditor.dataset.cellId = cellId;
      dimensionEditor.dataset.axis = axis;
      dimensionEditor.dataset.index = String(index);
      dimensionEditor.dataset.edge = edge;
      dimensionLabel.textContent = axis === "column" ? "区域内宽 mm" : "区域内高 mm";
    } else if (kind === "topology-clear-span") {
      const memberId = target.dataset.memberId;
      const cellId = target.dataset.cellId;
      const clearSpanSide = target.dataset.clearSpanSide;
      const member = window.topology.members.find((candidate) => candidate.objectId === memberId);
      const geometry = resolveWindowGeometry(window);
      const host = geometry.cells.find((cell) => cell.objectId === cellId);
      const resolvedMember = geometry.members.find(
        (candidate) => candidate.kind === "topology-member" && candidate.objectId === memberId
      );
      const cellIndex = window.layout.cells.findIndex((cell) => cell.objectId === cellId);
      if (
        !memberId ||
        !cellId ||
        !member ||
        !host ||
        !resolvedMember ||
        cellIndex < 0 ||
        (clearSpanSide !== "before" && clearSpanSide !== "after")
      ) return;
      const vertical = member.orientation === "vertical";
      const hostStartMm = vertical ? host.xMm : host.yMm;
      const hostSizeMm = vertical ? host.widthMm : host.heightMm;
      const memberStartMm = vertical ? resolvedMember.xMm : resolvedMember.yMm;
      const memberFaceMm = vertical ? resolvedMember.widthMm : resolvedMember.heightMm;
      value = clearSpanSide === "before"
        ? memberStartMm - hostStartMm
        : hostStartMm + hostSizeMm - memberStartMm - memberFaceMm;

      // The domain stores the member centre as a ratio of the rule-grid's outer
      // host size, while the displayed clear span uses the resolved inner bay.
      // Convert the domain's edge clamp into that same inner-bay coordinate so
      // the browser validates exactly the values the command can preserve.
      const row = Math.floor(cellIndex / window.layout.columns.length);
      const column = cellIndex % window.layout.columns.length;
      const weights = vertical ? window.layout.columns : window.layout.rows;
      const trackIndex = vertical ? column : row;
      const outerAxisSizeMm = vertical ? window.widthMm : window.heightMm;
      const outerHostSizeMm = outerAxisSizeMm * (weights[trackIndex] ?? 0) /
        (weights.reduce((sum, weight) => sum + weight, 0) || 1);
      const edgeMm = Math.min(
        Math.max(120, outerHostSizeMm * 0.08),
        Math.max(1, outerHostSizeMm / 2 - 1)
      );
      const maximumClearMm = hostSizeMm * (outerHostSizeMm - edgeMm) /
        outerHostSizeMm - memberFaceMm / 2;
      dimensionInput.min = String(Math.max(1, Math.ceil(
        hostSizeMm * edgeMm / outerHostSizeMm - memberFaceMm / 2
      )));
      dimensionInput.max = String(Math.max(Number(dimensionInput.min), Math.floor(maximumClearMm)));
      dimensionEditor.dataset.memberId = memberId;
      dimensionEditor.dataset.cellId = cellId;
      dimensionEditor.dataset.clearSpanSide = clearSpanSide;
      dimensionLabel.textContent = `${vertical ? "竖梃间净宽" : "横梃间净高"} mm`;
    } else if (kind === "meeting-mullion") {
      const cellId = target.dataset.cellId;
      const geometry = resolveWindowGeometry(window);
      const host = geometry.cells.find((cell) => cell.objectId === cellId);
      const meeting = geometry.meetingMullions.find(
        (candidate) => candidate.sourceObjectId === cellId
      );
      if (!cellId || !host || !meeting) return;
      const measurementRole = target.dataset.measurementRole;
      if (
        measurementRole !== "left-inner" &&
        measurementRole !== "right-inner" &&
        measurementRole !== "left-outer" &&
        measurementRole !== "right-outer"
      ) return;
      const meetingCenterMm = meeting.xMm + meeting.widthMm / 2;
      value = measurementRole === "left-inner"
        ? meeting.leftInnerWidthMm
        : measurementRole === "right-inner"
          ? meeting.rightInnerWidthMm
          : measurementRole === "left-outer"
            ? meetingCenterMm
            : window.widthMm - meetingCenterMm;
      dimensionEditor.dataset.cellId = cellId;
      dimensionEditor.dataset.measurementRole = measurementRole;
      const fixedHalfFaceMm = meeting.kind === "fixed-mullion" ? meeting.widthMm / 2 : 0;
      const availablePanelWidthMm = Math.max(2, host.widthMm - fixedHalfFaceMm * 2);
      const minimumPanelMm = Math.min(120, Math.max(1, availablePanelWidthMm / 2 - 1));
      const minimumCenterMm = fixedHalfFaceMm + minimumPanelMm;
      const maximumCenterMm = host.widthMm - fixedHalfFaceMm - minimumPanelMm;
      const minimumValue = measurementRole === "left-inner" || measurementRole === "right-inner"
        ? minimumPanelMm
        : measurementRole === "left-outer"
          ? host.xMm + minimumCenterMm
          : window.widthMm - host.xMm - maximumCenterMm;
      const maximumValue = measurementRole === "left-inner" || measurementRole === "right-inner"
        ? host.widthMm - fixedHalfFaceMm * 2 - minimumPanelMm
        : measurementRole === "left-outer"
          ? host.xMm + maximumCenterMm
          : window.widthMm - host.xMm - minimumCenterMm;
      dimensionInput.min = String(Math.ceil(minimumValue));
      dimensionInput.max = String(Math.floor(maximumValue));
      const sideLabel = measurementRole.startsWith("left") ? "左侧" : "右侧";
      const diameterLabel = measurementRole.endsWith("inner") ? "净内径" : "外径";
      dimensionLabel.textContent = `${meeting.kind === "fixed-mullion" ? "固定中梃" : "飞梃"}${sideLabel}${diameterLabel} mm`;
    } else {
      return;
    }
    dimensionEditor.dataset.windowId = window.objectId;
    dimensionEditor.dataset.dimensionKind = kind;
    dimensionInput.value = String(Math.round(value * 10) / 10);
    const stage = dimensionEditor.parentElement?.getBoundingClientRect();
    const clientX = "clientX" in event && Number.isFinite(event.clientX) ? event.clientX : (stage?.left ?? 0) + 24;
    const clientY = "clientY" in event && Number.isFinite(event.clientY) ? event.clientY : (stage?.top ?? 0) + 24;
    if (stage) {
      dimensionEditor.style.left = `${Math.max(8, Math.min(stage.width - 210, clientX - stage.left + 10))}px`;
      dimensionEditor.style.top = `${Math.max(8, Math.min(stage.height - 116, clientY - stage.top - 14))}px`;
    }
    dimensionEditor.hidden = false;
    dimensionInput.focus();
    dimensionInput.select();
  };
  /**
   * Dispatches a native double activation through the shared canvas resolver.
   *
   * Dimensions retain their numeric editor while ordinary objects ask the
   * active shell to present its property UI after stable-ID selection.
   *
   * @example Double-clicking a sash selects it and focuses desktop properties.
   * @since 0.10.28
   * @modified 2026-09-20 - Unified dimension and object double activation.
   */
  const onCanvasDoubleClick = (event: MouseEvent): void => {
    if (skipNativeCanvasDoubleClick) {
      skipNativeCanvasDoubleClick = false;
      return;
    }
    const hit = resolveDomCanvasInteractionHit(event.target);
    const label = (currentDocument.drawingTextLabels ?? []).find(
      (candidate) => candidate.objectId === hit.object?.dataset.objectId
    );
    if (label) {
      event.preventDefault();
      event.stopPropagation();
      selection.select(label.objectId, "2d");
      openTextLabelEditor(label, event);
      return;
    }
    const intent = resolveCanvasInteractionIntent({ activation: "double", hit: hit.semantic });
    if (intent.kind === "none") return;
    event.preventDefault();
    event.stopPropagation();
    if (intent.kind === "edit-dimension" && hit.dimension) {
      openDimensionEditor(hit.dimension, event);
      return;
    }
    if (intent.kind === "open-properties") {
      selection.select(intent.objectId as DesignObjectId, "2d");
      options.onOpenProperties?.(intent.objectId as DesignObjectId);
    }
  };
  /**
   * Provides the keyboard equivalent of the shared double-activation action.
   *
   * @example Enter on a focusable dimension opens its numeric editor.
   * @since 0.10.28
   * @modified 2026-09-20 - Added keyboard access to the same intent path.
   */
  const onCanvasKeyDown = (event: KeyboardEvent): void => {
    if (event.key !== "Enter" && event.key !== " ") return;
    const hit = resolveDomCanvasInteractionHit(event.target);
    const label = (currentDocument.drawingTextLabels ?? []).find(
      (candidate) => candidate.objectId === hit.object?.dataset.objectId
    );
    if (label) {
      event.preventDefault();
      selection.select(label.objectId, "2d");
      openTextLabelEditor(label);
      return;
    }
    const intent = resolveCanvasInteractionIntent({ activation: "double", hit: hit.semantic });
    if (intent.kind === "none") return;
    event.preventDefault();
    if (intent.kind === "edit-dimension" && hit.dimension) {
      openDimensionEditor(hit.dimension, event);
      return;
    }
    if (intent.kind === "open-properties") {
      selection.select(intent.objectId as DesignObjectId, "2d");
      options.onOpenProperties?.(intent.objectId as DesignObjectId);
    }
  };
  const onDimensionSubmit = (event: SubmitEvent): void => {
    event.preventDefault();
    const windowId = dimensionEditor.dataset.windowId;
    const kind = dimensionEditor.dataset.dimensionKind;
    const window = currentDocument.windows.find((candidate) => candidate.objectId === windowId);
    const rawValue = Number(dimensionInput.value);
    if (!window || !Number.isFinite(rawValue)) return;
    const minimum = Number(dimensionInput.min) || 0;
    const maximum = Number(dimensionInput.max) || 30_000;
    const value = Math.max(minimum, Math.min(maximum, Math.round(rawValue)));
    dimensionCommandSequence += 1;
    if (kind === "width" || kind === "height") {
      session.execute(
        createResizeWindowCommand({
          commandId: `CMD-DIMENSION-${dimensionCommandSequence}`,
          windowId: window.objectId,
          widthMm: kind === "width" ? value : window.widthMm,
          heightMm: kind === "height" ? value : window.heightMm
        })
      );
      selection.select(window.objectId, "2d");
    } else if (kind === "surround-outside-width" || kind === "surround-outer-width") {
      const installation = normalizeWindowInstallation(window.installation);
      const outsideVisible = installation.surround.enabled &&
        (installation.surround.styleId === "both_sides" ||
          installation.surround.styleId === "outside_only");
      const horizontalSideCount = outsideVisible
        ? Number(installation.surround.sides.includes("left")) +
          Number(installation.surround.sides.includes("right"))
        : 0;
      const outsideWidthMm = kind === "surround-outer-width"
        ? horizontalSideCount > 0
          ? (value - window.widthMm) / horizontalSideCount
          : installation.surround.outsideWidthMm
        : value;
      session.execute(createUpdateWindowInstallationCommand({
        commandId: `CMD-SURROUND-DIMENSION-${dimensionCommandSequence}`,
        windowId: window.objectId,
        installation: {
          ...installation,
          surround: {
            ...installation.surround,
            outsideWidthMm
          }
        }
      }));
      selection.select(createWindowInstallationSurroundObjectId(window.objectId), "2d");
    } else if (kind === "cell-width" || kind === "cell-height") {
      const cellId = dimensionEditor.dataset.cellId;
      const axis = dimensionEditor.dataset.axis;
      const edge = dimensionEditor.dataset.edge;
      const index = Number(dimensionEditor.dataset.index);
      const geometry = resolveWindowGeometry(window);
      const host = geometry.cells.find((cell) => cell.objectId === cellId);
      if (
        !cellId ||
        !host ||
        (axis !== "column" && axis !== "row") ||
        (edge !== "start" && edge !== "end") ||
        !Number.isInteger(index)
      ) return;
      const tracks = axis === "column" ? window.layout.columns : window.layout.rows;
      if (tracks.length === 1) {
        session.execute(
          createResizeWindowCommand({
            commandId: `CMD-CELL-DIMENSION-${dimensionCommandSequence}`,
            windowId: window.objectId,
            widthMm: axis === "column" ? value + window.frameFaceMm * 2 : window.widthMm,
            heightMm: axis === "row" ? value + window.frameFaceMm * 2 : window.heightMm
          })
        );
      } else {
        const axisSize = axis === "column" ? window.widthMm : window.heightMm;
        const innerSize = Math.max(1, axisSize - window.frameFaceMm * 2);
        const hostStartMm = axis === "column" ? host.xMm : host.yMm;
        const hostSizeMm = axis === "column" ? host.widthMm : host.heightMm;
        const resolvedBoundaryMm = edge === "end"
          ? hostStartMm + value
          : hostStartMm + hostSizeMm - value;
        const boundaryRatio = Math.min(
          1,
          Math.max(0, (resolvedBoundaryMm - window.frameFaceMm) / innerSize)
        );
        session.execute(
          createMoveWindowGridDividerCommand({
            commandId: `CMD-CELL-DIMENSION-${dimensionCommandSequence}`,
            windowId: window.objectId,
            axis,
            index,
            positionMm: boundaryRatio * axisSize
          })
        );
      }
      selection.select(cellId as DesignObjectId, "2d");
    } else if (kind === "grid-divider") {
      const axis = dimensionEditor.dataset.axis;
      const index = Number(dimensionEditor.dataset.index);
      if ((axis !== "column" && axis !== "row") || !Number.isInteger(index)) return;
      session.execute(
        createMoveWindowGridDividerCommand({
          commandId: `CMD-GRID-DIMENSION-${dimensionCommandSequence}`,
          windowId: window.objectId,
          axis,
          index,
          positionMm: value
        })
      );
      selection.select(
        `${window.objectId}:divider.${axis === "column" ? "v" : "h"}.${index}` as DesignObjectId,
        "2d"
      );
    } else if (kind === "topology-member") {
      const memberId = dimensionEditor.dataset.memberId;
      if (!memberId) return;
      session.execute(
        createMoveWindowTopologyMemberCommand({
          commandId: `CMD-TOPOLOGY-DIMENSION-${dimensionCommandSequence}`,
          windowId: window.objectId,
          memberId,
          positionMm: value
        })
      );
      selection.select(memberId as DesignObjectId, "2d");
    } else if (kind === "topology-clear-span") {
      const memberId = dimensionEditor.dataset.memberId;
      const cellId = dimensionEditor.dataset.cellId;
      const clearSpanSide = dimensionEditor.dataset.clearSpanSide;
      const member = window.topology.members.find((candidate) => candidate.objectId === memberId);
      const geometry = resolveWindowGeometry(window);
      const host = geometry.cells.find((cell) => cell.objectId === cellId);
      const resolvedMember = geometry.members.find(
        (candidate) => candidate.kind === "topology-member" && candidate.objectId === memberId
      );
      const cellIndex = window.layout.cells.findIndex((cell) => cell.objectId === cellId);
      if (
        !memberId ||
        !cellId ||
        !member ||
        !host ||
        !resolvedMember ||
        cellIndex < 0 ||
        (clearSpanSide !== "before" && clearSpanSide !== "after")
      ) return;
      const vertical = member.orientation === "vertical";
      const hostSizeMm = vertical ? host.widthMm : host.heightMm;
      const memberFaceMm = vertical ? resolvedMember.widthMm : resolvedMember.heightMm;
      const desiredCenterMm = clearSpanSide === "before"
        ? value + memberFaceMm / 2
        : hostSizeMm - value - memberFaceMm / 2;
      const row = Math.floor(cellIndex / window.layout.columns.length);
      const column = cellIndex % window.layout.columns.length;
      const weights = vertical ? window.layout.columns : window.layout.rows;
      const trackIndex = vertical ? column : row;
      const outerAxisSizeMm = vertical ? window.widthMm : window.heightMm;
      const outerHostSizeMm = outerAxisSizeMm * (weights[trackIndex] ?? 0) /
        (weights.reduce((sum, weight) => sum + weight, 0) || 1);
      session.execute(
        createMoveWindowTopologyMemberCommand({
          commandId: `CMD-TOPOLOGY-CLEAR-SPAN-${dimensionCommandSequence}`,
          windowId: window.objectId,
          memberId,
          positionMm: desiredCenterMm / hostSizeMm * outerHostSizeMm
        })
      );
      selection.select(memberId as DesignObjectId, "2d");
    } else if (kind === "meeting-mullion") {
      const cellId = dimensionEditor.dataset.cellId;
      const measurementRole = dimensionEditor.dataset.measurementRole;
      const geometry = resolveWindowGeometry(window);
      const host = geometry.cells.find((cell) => cell.objectId === cellId);
      const meeting = geometry.meetingMullions.find(
        (candidate) => candidate.sourceObjectId === cellId
      );
      if (
        !cellId ||
        !host ||
        !meeting ||
        (measurementRole !== "left-inner" &&
          measurementRole !== "right-inner" &&
          measurementRole !== "left-outer" &&
          measurementRole !== "right-outer")
      ) return;
      const fixedHalfFaceMm = meeting.kind === "fixed-mullion" ? meeting.widthMm / 2 : 0;
      const meetingPositionMm = measurementRole === "left-inner"
        ? value + fixedHalfFaceMm
        : measurementRole === "right-inner"
          ? host.widthMm - value - fixedHalfFaceMm
          : measurementRole === "left-outer"
            ? value - host.xMm
            : window.widthMm - value - host.xMm;
      session.execute(
        createMoveOpeningMeetingMullionCommand({
          commandId: `CMD-OPENING-MEETING-DIMENSION-${dimensionCommandSequence}`,
          windowId: window.objectId,
          cellId,
          positionMm: meetingPositionMm
        })
      );
      selection.select(meeting.objectId, "2d");
    } else {
      return;
    }
    closeDimensionEditor();
  };
  const onViewOptionChange = (): void => {
    canvasView.setTwoDimensionalRenderStyle(
      twoDimensionalRenderStyle.value === "engineering-line"
        ? "engineering-line"
        : "material"
    );
    canvasView.setOption("showDimensions", showDimensions.checked);
    canvasView.setOption("showPlanView", showPlan.checked);
    canvasView.setOption("showOpeningState", showOpeningState.checked);
    canvasView.setOption("showThreeSelectionOutline", showSelectionOutline.checked);
    canvasView.setOption("showThreeInstallationHost", showInstallationHost.checked);
    canvasView.setOption(
      "showThreeInstallationSurround",
      showInstallationSurround.checked
    );
  };

  container.addEventListener("click", onToolbarClick);
  options.constructionToolsContainer?.addEventListener("click", onToolbarClick);
  container.addEventListener("pointerdown", onWorkspacePointerDown);
  resetCamera.addEventListener("click", onResetCamera);
  resetCanvas.addEventListener("click", onResetCanvas);
  addTextLabel.addEventListener("click", onAddTextLabel);
  twoDimensional.addEventListener("click", onTwoDimensionalClick);
  twoDimensional.addEventListener("wheel", onCanvasWheel, { passive: false });
  twoDimensional.addEventListener("pointerdown", onCanvasPointerDown);
  twoDimensional.addEventListener("pointermove", onCanvasPointerMove);
  twoDimensional.addEventListener("pointerup", onCanvasPointerEnd);
  twoDimensional.addEventListener("pointercancel", onCanvasPointerEnd);
  twoDimensional.addEventListener("dblclick", onCanvasDoubleClick);
  twoDimensional.addEventListener("keydown", onCanvasKeyDown);
  twoDimensional.addEventListener("contextmenu", onCanvasContextMenu);
  dimensionEditor.addEventListener("submit", onDimensionSubmit);
  dimensionCancel.addEventListener("click", closeDimensionEditor);
  textLabelEditor.addEventListener("submit", onTextLabelSubmit);
  textLabelCancel.addEventListener("click", closeTextLabelEditor);
  contextEdit.addEventListener("click", openMemberPositionEditor);
  contextDelete.addEventListener("click", deleteContextMember);
  showDimensions.addEventListener("change", onViewOptionChange);
  twoDimensionalRenderStyle.addEventListener("change", onViewOptionChange);
  showPlan.addEventListener("change", onViewOptionChange);
  showOpeningState.addEventListener("change", onViewOptionChange);
  showSelectionOutline.addEventListener("change", onViewOptionChange);
  showInstallationHost.addEventListener("change", onViewOptionChange);
  showInstallationSurround.addEventListener("change", onViewOptionChange);
  openingControlsHost.addEventListener("input", onOpeningControlsInput);
  openingControlsHost.addEventListener("change", onOpeningControlsInput);
  openingControlsHost.addEventListener("click", onOpeningControlsClick);
  const disposeDocument = session.subscribe((document) => {
    currentDocument = document;
    visualPreview.synchronize(document, false);
    openingPreview.synchronize(document, false);
    currentOpeningPreview = openingPreview.state;
    if (selectedObjectId && !documentContainsObjectId(document, selectedObjectId)) {
      selection.select(undefined, "system");
    }
    if (
      textLabelEditor.dataset.labelId &&
      !(document.drawingTextLabels ?? []).some(
        (label) => label.objectId === textLabelEditor.dataset.labelId
      )
    ) {
      closeTextLabelEditor();
    }
    renderTwoDimensional();
    updateThreeDimensional();
    void synchronizeRuntimeAssets(visualPreview.project(document));
    updateOpeningControlState();
    updateGridCommandAvailability();
  });
  const disposeOpeningPreview = openingPreview.subscribe((state) => {
    currentOpeningPreview = state;
    renderTwoDimensional();
    updateThreeDimensional(false);
    updateOpeningControlState();
    scheduleOpeningAnimation();
  });
  const disposeVisualPreview = visualPreview.subscribe(() => {
    const projected = visualPreview.project(currentDocument);
    renderTwoDimensional();
    updateThreeDimensional(false);
    void synchronizeRuntimeAssets(projected);
  });
  const disposeSelection = selection.subscribe((state) => {
    visualPreview.clearAll();
    selectedObjectId = state.objectId;
    renderTwoDimensional();
    // 3D dimension labels are selection-scoped, so selection changes rebuild
    // only scene annotations while preserving the user's camera position.
    updateThreeDimensional(false);
    viewport?.setSelectedObjectId(state.objectId);
    updateGridCommandAvailability();
  });
  const disposeCanvasView = canvasView.subscribe((state) => {
    const visibilityChanged =
      state.showDimensions !== currentCanvasView.showDimensions ||
      state.twoDimensionalRenderStyle !== currentCanvasView.twoDimensionalRenderStyle ||
      state.showPlanView !== currentCanvasView.showPlanView ||
      state.showOpeningState !== currentCanvasView.showOpeningState ||
      state.showThreeInstallationHost !== currentCanvasView.showThreeInstallationHost ||
      state.showThreeInstallationSurround !== currentCanvasView.showThreeInstallationSurround;
    currentCanvasView = state;
    showDimensions.checked = state.showDimensions;
    twoDimensionalRenderStyle.value = state.twoDimensionalRenderStyle;
    showPlan.checked = state.showPlanView;
    showOpeningState.checked = state.showOpeningState;
    showSelectionOutline.checked = state.showThreeSelectionOutline;
    showInstallationHost.checked = state.showThreeInstallationHost;
    showInstallationSurround.checked = state.showThreeInstallationSurround;
    viewport?.setSelectionOutlineVisible(state.showThreeSelectionOutline);
    if (visibilityChanged) {
      renderTwoDimensional();
      updateThreeDimensional(false);
      return;
    }
    const viewportGroup = twoDimensional.querySelector<SVGGElement>("#designCanvasViewport");
    viewportGroup?.setAttribute(
      "transform",
      `translate(${state.x} ${state.y}) scale(${state.scale})`
    );
    viewportGroup?.setAttribute("data-scale", String(state.scale));
  });
  updateGridCommandAvailability();
  updateOpeningControlState();
  showView(activeView);

  return () => {
    container.removeEventListener("click", onToolbarClick);
    options.constructionToolsContainer?.removeEventListener("click", onToolbarClick);
    container.removeEventListener("pointerdown", onWorkspacePointerDown);
    resetCamera.removeEventListener("click", onResetCamera);
    resetCanvas.removeEventListener("click", onResetCanvas);
    addTextLabel.removeEventListener("click", onAddTextLabel);
    twoDimensional.removeEventListener("click", onTwoDimensionalClick);
    twoDimensional.removeEventListener("wheel", onCanvasWheel);
    twoDimensional.removeEventListener("pointerdown", onCanvasPointerDown);
    twoDimensional.removeEventListener("pointermove", onCanvasPointerMove);
    twoDimensional.removeEventListener("pointerup", onCanvasPointerEnd);
    twoDimensional.removeEventListener("pointercancel", onCanvasPointerEnd);
    twoDimensional.removeEventListener("dblclick", onCanvasDoubleClick);
    twoDimensional.removeEventListener("keydown", onCanvasKeyDown);
    twoDimensional.removeEventListener("contextmenu", onCanvasContextMenu);
    dimensionEditor.removeEventListener("submit", onDimensionSubmit);
    dimensionCancel.removeEventListener("click", closeDimensionEditor);
    textLabelEditor.removeEventListener("submit", onTextLabelSubmit);
    textLabelCancel.removeEventListener("click", closeTextLabelEditor);
    contextEdit.removeEventListener("click", openMemberPositionEditor);
    contextDelete.removeEventListener("click", deleteContextMember);
    showDimensions.removeEventListener("change", onViewOptionChange);
    twoDimensionalRenderStyle.removeEventListener("change", onViewOptionChange);
    showPlan.removeEventListener("change", onViewOptionChange);
    showOpeningState.removeEventListener("change", onViewOptionChange);
    showSelectionOutline.removeEventListener("change", onViewOptionChange);
    showInstallationHost.removeEventListener("change", onViewOptionChange);
    showInstallationSurround.removeEventListener("change", onViewOptionChange);
    openingControlsHost.removeEventListener("input", onOpeningControlsInput);
    openingControlsHost.removeEventListener("change", onOpeningControlsInput);
    openingControlsHost.removeEventListener("click", onOpeningControlsClick);
    disposeDocument();
    disposeOpeningPreview();
    disposeVisualPreview();
    disposeSelection();
    disposeCanvasView();
    touchCanvasGesture.reset();
    canvasPointerPresses.clear();
    cancelMemberLongPress();
    if (canvasClickSuppressionTimer !== undefined) clearTimeout(canvasClickSuppressionTimer);
    options.constructionToolsContainer?.replaceChildren();
    disposed = true;
    runtimeAssetRequestVersion += 1;
    viewport?.dispose();
    visualAssetRegistry.dispose();
    if (openingAnimationFrame !== undefined) cancelAnimationFrame(openingAnimationFrame);
    container.replaceChildren();
  };
}

/**
 * Checks selection identity against the shared domain/geometry projection.
 *
 * Undo may remove the selected window after all views have highlighted one of
 * its generated frame or divider IDs. Reconciling on document updates prevents
 * a stale object-tree status and a 3D helper pointing at a disposed scene node.
 *
 * @param document Current immutable document.
 * @param objectId Candidate selection ID.
 * @returns True when any current window or projected child owns the ID.
 * @example An empty document never contains `WIN-1:frame.left`.
 * @since 0.4.0
 * @modified 2026-09-18 - Included generated hardware IDs for exact model editing.
 */
function documentContainsObjectId(document: DesignDocument, objectId: string): boolean {
  if ((document.drawingTextLabels ?? []).some((label) => label.objectId === objectId)) return true;
  if ((document.assemblies ?? []).some((assembly) => {
    const wallId = createWindowInstallationWallObjectId(assembly.objectId);
    const surroundId = createWindowInstallationSurroundObjectId(assembly.objectId);
    return objectId === assembly.objectId ||
      assembly.instances.some((instance) => instance.objectId === objectId) ||
      assembly.joints.some((joint) => joint.objectId === objectId) ||
      objectId === wallId || objectId.startsWith(`${wallId}.`) ||
      objectId === surroundId || objectId.startsWith(`${surroundId}.`);
  })) return true;
  return document.windows.some((window) => {
    if (window.objectId === objectId) return true;
    const wallId = createWindowInstallationWallObjectId(window.objectId);
    const surroundId = createWindowInstallationSurroundObjectId(window.objectId);
    if (
      objectId === wallId || objectId.startsWith(`${wallId}.`) ||
      objectId === surroundId || objectId.startsWith(`${surroundId}.`)
    ) return true;
    const geometry = resolveWindowGeometry(window);
    return [
      ...geometry.frames,
      ...geometry.cells,
      ...geometry.openings.map((opening) => ({
        objectId: createOpeningPanelKey(opening.objectId, opening.panelId)
      })),
      ...geometry.members,
      ...geometry.meetingMullions,
      ...geometry.hardware.map((mount) => ({ objectId: mount.hardwareId }))
    ].some(
      (item) => item.objectId === objectId
    );
  });
}

/**
 * Backward-compatible 2D-only surface used by early migration callers.
 *
 * New layout shells should call `mountSharedDesignWorkspace`; this adapter is
 * retained so isolated integrations can continue consuming deterministic SVG.
 *
 * @param container Host element for SVG.
 * @param session Shared document session.
 * @returns Subscription disposer.
 * @example `mountSharedDesignSurface(exportPreview, session)`.
 * @since 0.1.0
 * @modified 2026-09-17 - Retained as a compatibility wrapper after 3D integration.
 */
export function mountSharedDesignSurface(
  container: HTMLElement,
  session: DesignSession
): () => void {
  container.classList.add("design-surface");
  return session.subscribe((document) => {
    container.innerHTML = renderDesignSvg(document);
  });
}

/**
 * Mounts a domain-derived object tree linked to both visual projections.
 *
 * Tree rows are derived from shared window geometry rather than scanning SVG
 * or Three.js nodes. Generated grid dividers and window-scoped frame IDs are
 * therefore visible and selectable with the same identifiers as both renderers.
 *
 * @param container Sidebar or drawer region that receives the tree.
 * @param session Shared design source.
 * @param selection Shared selection coordinator.
 * @param presentation Hides program-only identities in the factory workspace.
 * @returns A disposer for events and subscriptions.
 * @example `mountDesignObjectTree(sidebar, session, selection)`.
 * @since 0.4.0
 * @modified 2026-10-01 - Added a factory presentation without internal IDs.
 */
export function mountDesignObjectTree(
  container: HTMLElement,
  session: DesignSession,
  selection: DesignSelectionStore,
  presentation: "technical" | "factory-workbench" = "technical"
): () => void {
  container.classList.add("object-tree");
  let currentDocument = session.document;
  let selectedObjectId = selection.state.objectId;

  const appendRow = (
    parent: HTMLElement,
    objectId: string,
    label: string,
    kind: string
  ): void => {
    const item = document.createElement("li");
    const button = document.createElement("button");
    button.type = "button";
    button.dataset.objectId = objectId;
    button.dataset.kind = kind;
    button.setAttribute("aria-pressed", String(objectId === selectedObjectId));
    button.textContent = label;
    item.append(button);
    parent.append(item);
  };

  /** Rebuilds the small tree projection from immutable domain geometry. */
  const render = (): void => {
    const fragment = document.createDocumentFragment();
    const status = document.createElement("p");
    status.className = "object-tree__selection";
    status.textContent = selectedObjectId
      ? presentation === "factory-workbench"
        ? "当前已选中构件"
        : `当前选择：${selectedObjectId}`
      : "当前未选择构件";
    fragment.append(status);
    if (currentDocument.windows.length === 0) {
      const empty = document.createElement("p");
      empty.className = "object-tree__empty";
      empty.textContent = "创建窗体后显示对象结构";
      fragment.append(empty);
    }
    for (const assembly of currentDocument.assemblies ?? []) {
      const geometry = resolveFabricationAssemblyGeometry(assembly, currentDocument.windows);
      const section = document.createElement("section");
      section.className = "object-tree__window object-tree__assembly";
      const list = document.createElement("ul");
      appendRow(
        list,
        assembly.objectId,
        `${assembly.mark} · 组合外形 ${geometry.bounds.widthMm}×${geometry.bounds.heightMm}`,
        "fabrication-assembly"
      );
      appendRow(
        list,
        createWindowInstallationWallObjectId(assembly.objectId),
        geometry.recommendedOpeningVoidRegions.length > 0
          ? `组合参考墙 · 折线洞口（包络 ${geometry.recommendedOpening.widthMm}×${geometry.recommendedOpening.heightMm}）`
          : `组合参考墙 · 推荐洞口 ${geometry.recommendedOpening.widthMm}×${geometry.recommendedOpening.heightMm}`,
        "installation-wall"
      );
      const installation = normalizeWindowInstallation(assembly.installation);
      appendRow(
        list,
        createWindowInstallationSurroundObjectId(assembly.objectId),
        installation.surround.enabled
          ? `组合包边/衬板 · ${installation.surround.materialCode}`
          : "组合包边/衬板 · 未启用",
        "installation-surround"
      );
      for (const instance of assembly.instances) {
        const window = currentDocument.windows.find(
          (candidate) => candidate.objectId === instance.windowId
        );
        appendRow(
          list,
          instance.objectId,
          `窗单元实例 · ${window?.mark ?? instance.windowId} · (${instance.transform.xMm}, ${instance.transform.yMm})`,
          "window-instance"
        );
      }
      for (const joint of assembly.joints) {
        const typeLabel = joint.jointType === "reinforced_mullion"
          ? "加强拼樘"
          : joint.jointType === "stacking_joint"
            ? "上下叠接"
            : "拼樘连接";
        appendRow(
          list,
          joint.objectId,
          joint.catalogSelection
            ? `${joint.catalogSelection.businessName} · ${joint.factoryScope === "factory" ? "工厂连接" : "现场连接"}`
            : `${typeLabel} · ${joint.gapMm}mm · ${joint.factoryScope === "factory" ? "工厂连接" : "现场连接"}`,
          "engineering-joint"
        );
      }
      for (const label of currentDocument.drawingTextLabels ?? []) {
        if (label.ownerObjectId !== assembly.objectId) continue;
        appendRow(
          list,
          label.objectId,
          `文字标签 · ${label.text}`,
          "drawing-text-label"
        );
      }
      section.append(list);
      fragment.append(section);
    }
    const assembledWindowIds = new Set(
      (currentDocument.assemblies ?? []).flatMap((assembly) =>
        assembly.instances.map((instance) => instance.windowId))
    );
    for (const window of currentDocument.windows) {
      const section = document.createElement("section");
      section.className = "object-tree__window";
      const list = document.createElement("ul");
      appendRow(list, window.objectId, `${window.mark} · ${window.widthMm}×${window.heightMm}`, "window");
      const installation = normalizeWindowInstallation(window.installation);
      if (!assembledWindowIds.has(window.objectId)) {
        appendRow(
          list,
          createWindowInstallationWallObjectId(window.objectId),
          `安装墙体 · ${installation.surround.wallThicknessMm}mm · ${installation.surround.wallMaterialId}`,
          "installation-wall"
        );
        appendRow(
          list,
          createWindowInstallationSurroundObjectId(window.objectId),
          installation.surround.enabled
            ? `包边/衬板 · ${installation.surround.materialCode} · 外宽${installation.surround.outsideWidthMm}mm`
            : "包边/衬板 · 未启用",
          "installation-surround"
        );
      }
      const geometry = resolveWindowGeometry(window);
      for (const frame of geometry.frames) {
        appendRow(list, frame.objectId, `框 · ${frame.side}`, "frame-segment");
      }
      for (const cell of geometry.cells) {
        const sourceCell = window.layout.cells.find(
          (candidate) => candidate.objectId === cell.objectId
        );
        const cellLabel = sourceCell?.type === "turn_tilt"
          ? sourceCell.openingAssembly.panelCount === 2
            ? sourceCell.openingAssembly.mullionMode === "fixed_mullion"
              ? `双扇固定中梃 · 左右独立${sourceCell.openingAssembly.openPlane === "out" ? "外开" : "内开/内倒"}`
              : `双扇飞梃 · ${sourceCell.openingAssembly.openPlane === "out" ? "外开" : "内开/内倒"} · ${sourceCell.openingAssembly.primarySide === "right" ? "右主动扇" : "左主动扇"}`
            : sourceCell.openingAssembly.openPlane === "out"
              ? "单扇外平开"
              : "单扇内开/内倒"
          : sourceCell?.type === "top_hung"
            ? sourceCell.opening === "top_out" ? "上悬外开" : "上悬内开"
          : sourceCell?.type === "sliding"
            ? `普通推拉 · ${sourceCell.openingAssembly.panelCount}扇${sourceCell.openingAssembly.trackCount}轨 · ${sourceCell.opening === "slide_left" ? "活动扇向左收拢" : "活动扇向右收拢"}`
          : "固定玻璃";
        appendRow(
          list,
          cell.objectId,
          `单元 · ${cell.row + 1}-${cell.column + 1} · ${cellLabel}`,
          "cell"
        );
        for (const panel of geometry.openings.filter(
          (candidate) => candidate.objectId === cell.objectId
        )) {
          const panelRole = panel.panelCount === 1
            ? "活动扇"
            : panel.mullionMode === "fixed_mullion"
              ? panel.panelId === "P1" ? "左独立扇" : "右独立扇"
              : panel.panelRole === "primary" ? "主动扇" : "从动扇";
          appendRow(
            list,
            createOpeningPanelKey(panel.objectId, panel.panelId),
            `窗扇 · ${panel.panelId} · ${panelRole}`,
            "opening-panel"
          );
        }
        for (const panel of geometry.slidingPanels.filter(
          (candidate) => candidate.sourceObjectId === cell.objectId
        )) {
          appendRow(
            list,
            createOpeningPanelKey(panel.sourceObjectId, panel.panelId),
            `推拉扇 · ${panel.panelId} · ${panel.movable ? "活动" : "固定"} · 第${panel.trackIndex + 1}轨`,
            "sliding-panel"
          );
        }
        for (const track of geometry.slidingTracks.filter(
          (candidate) => candidate.sourceObjectId === cell.objectId
        )) {
          appendRow(
            list,
            track.objectId,
            `轨道 · 第${track.trackIndex + 1}轨（中性中心线）`,
            "sliding-track"
          );
        }
      }
      /**
       * Keeps the assembly summary while exposing every generated hardware ID.
       * Position remains catalog/rule-owned, but exact model selection is a
       * legitimate design override and therefore needs a selectable identity.
       */
      for (const opening of geometry.openings) {
        if (opening.panelIndex !== 0) continue;
        const openingHardware = geometry.hardware.filter(
          (mount) => mount.sourceObjectId === opening.objectId
        );
        const hingeCount = openingHardware.filter(
          (mount) => mount.role === "hinge-sash-leaf"
        ).length;
        const lockCount = openingHardware.filter((mount) => mount.role === "lock-point").length;
        const handleCount = openingHardware.filter((mount) => mount.role === "handle").length;
        const secondaryLeverCount = openingHardware.filter(
          (mount) => mount.role === "secondary-lever"
        ).length;
        const shootBoltCount = openingHardware.filter(
          (mount) => mount.role === "shoot-bolt"
        ).length;
        const meetingHardware = shootBoltCount > 0
          ? `${lockCount}组交汇锁点/锁座 · ${shootBoltCount}组天地插销/框锁座 · ${handleCount}执手 · ${secondaryLeverCount}从动扇操作杆`
          : `${lockCount}组锁点/锁座 · ${handleCount}执手`;
        const hardwareLabel = opening.type === "top_hung"
          ? `${hingeCount}组摩擦铰链/风撑 · ${handleCount}底部执手`
          : `${hingeCount}组合页 · ${meetingHardware}`;
        appendRow(
          list,
          opening.objectId,
          `五金连接 · ${hardwareLabel}`,
          "opening-hardware"
        );
        for (const mount of openingHardware) {
          appendRow(
            list,
            mount.hardwareId,
            `${HARDWARE_ROLE_LABELS[mount.role]} · ${mount.panelId} · ${mount.edge}`,
            `hardware-${mount.role}`
          );
        }
      }
      for (const mullion of geometry.meetingMullions) {
        appendRow(
          list,
          mullion.objectId,
          mullion.kind === "fixed-mullion"
            ? "固定中梃 · 归属外框"
            : `飞梃 · 归属${mullion.ownerPanelId}从动扇`,
          mullion.kind
        );
      }
      for (const member of geometry.members) {
        const validity = member.partitionValid ? "" : " · 无效拓扑";
        appendRow(
          list,
          member.objectId,
          `${member.kind === "grid-divider" ? "规则梃" : "拓扑梃"} · ${member.orientation}${validity}`,
          member.kind
        );
      }
      for (const label of currentDocument.drawingTextLabels ?? []) {
        if (label.ownerObjectId !== window.objectId) continue;
        appendRow(
          list,
          label.objectId,
          `文字标签 · ${label.text}`,
          "drawing-text-label"
        );
      }
      section.append(list);
      fragment.append(section);
    }
    container.replaceChildren(fragment);
  };

  const onClick = (event: MouseEvent): void => {
    const target = event.target;
    const button = target instanceof Element
      ? target.closest<HTMLButtonElement>("button[data-object-id]")
      : null;
    if (button?.dataset.objectId) selection.select(button.dataset.objectId, "tree");
  };
  container.addEventListener("click", onClick);
  const disposeDocument = session.subscribe((document) => {
    currentDocument = document;
    render();
  });
  const disposeSelection = selection.subscribe((state) => {
    selectedObjectId = state.objectId;
    render();
  });

  return () => {
    container.removeEventListener("click", onClick);
    disposeDocument();
    disposeSelection();
    container.replaceChildren();
  };
}

/**
 * Domain context required to edit one explicitly stored topology member.
 *
 * Generated rule-grid dividers deliberately do not resolve to this structure:
 * their dimensions are edited by divider commands, while this inspector owns
 * only members that carry profile, joint and note manufacturing properties.
 *
 * @example Selecting `MEMBER-V-1` resolves its parent window and host indexes.
 * @since 0.4.5
 * @modified 2026-09-17 - Added shared member-inspector context.
 */
interface SelectedTopologyMember {
  readonly window: WindowUnit;
  readonly member: WindowTopologyMember;
  readonly row: number;
  readonly column: number;
}

/** Stable cell plus its owning window for opening-property editing. */
interface SelectedWindowCell {
  readonly window: WindowUnit;
  readonly cell: WindowCell;
}

/**
 * Resolves a persisted cell directly or through its generated flying mullion.
 *
 * The inspector continues editing the host opening assembly while the shared
 * selection coordinator retains the flying mullion's independent object ID.
 *
 * @param design Current shared document.
 * @param objectId Cross-view stable selection.
 * @returns Owning window and cell when the selected ID is a layout cell.
 * @example Selecting `CELL-1:flying-mullion` resolves persisted cell `CELL-1`.
 * @since 0.4.9
 * @modified 2026-09-20 - Added P1/P2 stable-panel host lookup.
 */
function selectedWindowCell(
  design: DesignDocument,
  objectId: DesignObjectId | undefined
): SelectedWindowCell | undefined {
  if (!objectId) return undefined;
  for (const window of design.windows) {
    const cell = window.layout.cells.find((candidate) => candidate.objectId === objectId);
    if (cell) return { window, cell };
    const panelCell = window.layout.cells.find((candidate) =>
      candidate.type !== "fixed_glass" && candidate.openingAssembly.panels.some((panel) =>
        createOpeningPanelKey(candidate.objectId, panel.id) === objectId
      )
    );
    if (panelCell) return { window, cell: panelCell };
    const geometry = resolveWindowGeometry(window);
    const slidingTrack = geometry.slidingTracks.find(
      (candidate) => candidate.objectId === objectId
    );
    if (slidingTrack) {
      const host = window.layout.cells.find(
        (candidate) => candidate.objectId === slidingTrack.sourceObjectId
      );
      if (host) return { window, cell: host };
    }
    const meeting = geometry.meetingMullions.find(
      (candidate) => candidate.objectId === objectId
    );
    if (meeting) {
      const host = window.layout.cells.find(
        (candidate) => candidate.objectId === meeting.sourceObjectId
      );
      if (host) return { window, cell: host };
    }
  }
  return undefined;
}

/**
 * Resolves one exact generated hardware identity without widening to the window.
 *
 * The previous inspector fallback treated every generated handle/lock/hinge as
 * its owning window. That made a correct 3D hit display total window width and
 * height, which looked like the hit itself had failed. Keeping the exact mount
 * here lets the property card and 3D dimensions describe only the picked part.
 *
 * @param design Current immutable design snapshot.
 * @param objectId Candidate exact hardware ID.
 * @returns Owning window and resolved mount, or undefined for other objects.
 * @example Selecting `cell.1.1.panel.P2.hardware.handle` returns that handle.
 * @since 0.10.41
 * @modified 2026-09-20 - Added exact hardware property resolution.
 */
function selectedWindowHardware(
  design: DesignDocument,
  objectId: DesignObjectId | undefined
): Readonly<{
  window: WindowUnit;
  mount: ReturnType<typeof resolveWindowGeometry>["hardware"][number];
}> | undefined {
  if (!objectId) return undefined;
  for (const window of design.windows) {
    const mount = resolveWindowGeometry(window).hardware.find(
      (candidate) => candidate.hardwareId === objectId
    );
    if (mount) return { window, mount };
  }
  return undefined;
}

/**
 * Resolves a selected stable ID to an explicit topology member and host cell.
 *
 * @param design Immutable document currently shown by both shells.
 * @param objectId Selection shared by 2D, 3D and the object tree.
 * @returns Parent/member/host context, or undefined for any other object kind.
 * @example A generated `divider.v.1` returns undefined; a stored member resolves.
 * @since 0.4.5
 * @modified 2026-09-17 - Centralized inspector selection resolution.
 */
function selectedTopologyMember(
  design: DesignDocument,
  objectId: DesignObjectId | undefined
): SelectedTopologyMember | undefined {
  if (!objectId) return undefined;
  for (const window of design.windows) {
    const member = window.topology.members.find((candidate) => candidate.objectId === objectId);
    if (!member) continue;
    const cellIndex = window.layout.cells.findIndex(
      (cell) => cell.objectId === member.hostRegionId
    );
    if (cellIndex < 0 || window.layout.columns.length === 0) return undefined;
    return {
      window,
      member,
      row: Math.floor(cellIndex / window.layout.columns.length),
      column: cellIndex % window.layout.columns.length
    };
  }
  return undefined;
}

/**
 * Resolves a selected visual/domain object to its owning window.
 *
 * The lookup uses shared resolved geometry, not SVG ancestry, so the same rule
 * works for a window, cell, frame segment, generated grid divider or topology
 * member or flying mullion selected from 2D, 3D or the object tree.
 *
 * @param design Current immutable document.
 * @param objectId Stable cross-view selected object ID.
 * @returns Owning window, or undefined when no current object is selected.
 * @example Selecting `WIN-1:frame.left` resolves `WIN-1`.
 * @since 0.4.7
 * @modified 2026-09-18 - Included exact generated hardware ownership.
 */
function selectedOwningWindow(
  design: DesignDocument,
  objectId: DesignObjectId | undefined
): WindowUnit | undefined {
  if (!objectId) return undefined;
  for (const assembly of design.assemblies ?? []) {
    const selectedInstance = assembly.instances.find(
      (instance) => instance.objectId === objectId
    );
    if (selectedInstance) {
      return design.windows.find(
        (window) => window.objectId === selectedInstance.windowId
      );
    }
    const wallId = createWindowInstallationWallObjectId(assembly.objectId);
    const surroundId = createWindowInstallationSurroundObjectId(assembly.objectId);
    const belongsToAssembly = objectId === assembly.objectId ||
      assembly.joints.some((joint) => joint.objectId === objectId) ||
      objectId === wallId || objectId.startsWith(`${wallId}.`) ||
      objectId === surroundId || objectId.startsWith(`${surroundId}.`);
    if (!belongsToAssembly) continue;
    const firstWindowId = assembly.instances[0]?.windowId;
    return design.windows.find((window) => window.objectId === firstWindowId);
  }
  return design.windows.find((window) => {
    if (window.objectId === objectId) return true;
    const wallId = createWindowInstallationWallObjectId(window.objectId);
    const surroundId = createWindowInstallationSurroundObjectId(window.objectId);
    if (
      objectId === wallId || objectId.startsWith(`${wallId}.`) ||
      objectId === surroundId || objectId.startsWith(`${surroundId}.`)
    ) return true;
    const geometry = resolveWindowGeometry(window);
    return [
      ...geometry.frames,
      ...geometry.cells,
      ...geometry.openings.map((opening) => ({
        objectId: createOpeningPanelKey(opening.objectId, opening.panelId)
      })),
      ...geometry.slidingTracks,
      ...geometry.slidingPanels.map((panel) => ({
        objectId: createOpeningPanelKey(panel.sourceObjectId, panel.panelId)
      })),
      ...geometry.members,
      ...geometry.meetingMullions,
      ...geometry.hardware.map((mount) => ({ objectId: mount.hardwareId }))
    ].some(
      (item) => item.objectId === objectId
    );
  });
}

/**
 * Resolves a 2D secondary-click to an unambiguous deletable window product.
 *
 * An exact assembly instance maps to its referenced window. Assembly outlines,
 * joints, walls and surround pieces deliberately return `undefined`: those
 * aggregate objects do not identify which member the user intends to remove.
 * All remaining child geometry reuses the cross-view ownership resolver.
 *
 * @param design Current immutable design graph.
 * @param objectId Stable ID reported by the SVG hit adapter.
 * @returns The exact window that a contextual delete may remove.
 * @example Right-clicking `CELL-1` resolves its window; right-clicking `A-1` does not.
 * @since 0.10.52
 */
export function resolveContextDeletableWindow(
  design: DesignDocument,
  objectId: string
): WindowUnit | undefined {
  for (const assembly of design.assemblies ?? []) {
    const instance = assembly.instances.find((candidate) => candidate.objectId === objectId);
    if (instance) {
      return design.windows.find((window) => window.objectId === instance.windowId);
    }
    const wallId = createWindowInstallationWallObjectId(assembly.objectId);
    const surroundId = createWindowInstallationSurroundObjectId(assembly.objectId);
    if (
      objectId === assembly.objectId ||
      assembly.joints.some((joint) => joint.objectId === objectId) ||
      objectId === wallId || objectId.startsWith(`${wallId}.`) ||
      objectId === surroundId || objectId.startsWith(`${surroundId}.`)
    ) {
      return undefined;
    }
  }
  return selectedOwningWindow(design, objectId as DesignObjectId);
}

/**
 * Converts one host grid track from proportional weight to outer millimetres.
 *
 * This mirrors the domain command's conversion so the inspector displays the
 * same unit that will be validated on submit. Render geometry remains free to
 * deduct frame faces independently.
 *
 * @param context Selected member plus its stable host row and column.
 * @param axis Horizontal resolves host width; vertical resolves host height.
 * @returns Host track size in millimetres.
 * @example Column 0 of two equal columns in a 1200mm window resolves to 600.
 * @since 0.4.5
 * @modified 2026-09-17 - Added millimetre display conversion for the inspector.
 */
function memberHostAxisSizeMm(
  context: SelectedTopologyMember,
  axis: "horizontal" | "vertical"
): number {
  const weights = axis === "horizontal"
    ? context.window.layout.columns
    : context.window.layout.rows;
  const trackIndex = axis === "horizontal" ? context.column : context.row;
  const outerSize = axis === "horizontal" ? context.window.widthMm : context.window.heightMm;
  const total = weights.reduce((sum, value) => sum + value, 0);
  return (outerSize * (weights[trackIndex] ?? 0)) / total;
}

/** Rounds calculated form values without discarding useful tenths of a millimetre. */
function inspectorMillimetres(value: number): string {
  return String(Math.round(value * 10) / 10);
}

/** Non-technical product identity shown beside a selected simulated template. */
export interface ProductTemplateInspectorSummary {
  readonly productName: string;
  readonly statusLabel: "公开参考模拟 · 禁止投产";
  readonly versionLabel: string;
}

/**
 * Projects persisted template provenance into a compact designer-facing label.
 *
 * Algorithm: return no label for a hand-drawn/legacy window; otherwise expose
 * only the business product name, review status and exact template version.
 * Public-source URLs, geometry assumptions and rendering parameters remain in
 * the saved engineering snapshot and technical audit instead of cluttering the
 * daily PC/mobile property panel.
 *
 * @param window Selected window or the owning window of a selected component.
 * @returns Safe display strings, or undefined when no template was selected.
 * @example A simulated 120-system window shows its product name and
 * `ZCSUNG-SIM-LH-120-TT · v1.0.0`, while a manual rectangle shows no banner.
 * @since 0.10.77
 * @modified 2026-09-22 - Added shared target-template identity projection.
 */
export function resolveProductTemplateInspectorSummary(
  window: WindowUnit
): ProductTemplateInspectorSummary | undefined {
  const selection = window.productTemplateSelection;
  if (!selection) return undefined;
  return {
    productName: selection.publicProductName,
    statusLabel: "公开参考模拟 · 禁止投产",
    versionLabel: `模板 ${selection.templateId} · v${selection.templateVersion}`
  };
}

/**
 * Mounts the shared window/member manufacturing property inspector.
 *
 * Algorithm: subscribe to the same document and selection stores as both
 * renderers; prefer explicit member properties and otherwise resolve the owning
 * window for total-size editing; project normalized ratios to host millimetres;
 * and submit each form through one shared command. Static markup is used only
 * for known controls, while all domain strings are assigned through DOM values
 * or textContent to prevent imported notes/profile IDs becoming executable HTML.
 *
 * @param container Desktop sidebar or mobile details panel.
 * @param session Shared command session and undo history.
 * @param selection Cross-view stable selection coordinator.
 * @returns A disposer for DOM events, subscriptions and rendered controls.
 * @example `mountDesignObjectInspector(panel, session, selection)`.
 * @since 0.4.5
 * @modified 2026-09-17 - Exposed 0.5.4 double-sash keeper host relationships.
 */
export function mountDesignObjectInspector(
  container: HTMLElement,
  session: DesignSession,
  selection: DesignSelectionStore,
  presentation: "technical" | "factory-workbench" = "technical"
): () => void {
  container.classList.add("object-inspector");
  container.dataset.inspectorPresentation = presentation;
  let currentDocument = session.document;
  let selectedObjectId = selection.state.objectId;
  let commandSequence = 0;

  /**
   * Places one safe business template banner immediately after object identity.
   *
   * All persisted values use `textContent`; imported project strings can never
   * become markup. The same helper is called for a window, cell, sash, hardware
   * or topology member so 2D, 3D, object tree, PC and mobile keep one identity.
   *
   * @param target Inspector card or form receiving the notice.
   * @param window Window that owns the currently selected object.
   * @since 0.10.77
   * @modified 2026-09-22 - Added target-template status to shared inspectors.
   */
  const insertProductTemplateNotice = (
    target: HTMLElement,
    window: WindowUnit
  ): void => {
    const summary = resolveProductTemplateInspectorSummary(window);
    if (!summary) return;
    const notice = document.createElement("aside");
    notice.className = "object-inspector__product-template";
    const productName = document.createElement("strong");
    const status = document.createElement("span");
    const version = document.createElement("small");
    productName.textContent = summary.productName;
    status.textContent = summary.statusLabel;
    version.textContent = summary.versionLabel;
    notice.append(productName, status, version);
    const identity = target.querySelector(".object-inspector__identity");
    if (identity) identity.insertAdjacentElement("afterend", notice);
    else target.prepend(notice);
  };

  /** Adds the two independent factory-output switches to the selected element. */
  const replaceInspectorContent = (
    target: HTMLElement,
    includeFactoryOptions = true
  ): void => {
    if (presentation === "factory-workbench") {
      // Keep stable object IDs in command datasets, but not in business-facing
      // identity labels. The one exception is the plain-language "用户维护" tag.
      target.querySelectorAll<HTMLElement>(
        ".object-inspector__identity > span:not([data-business-label])"
      ).forEach((identity) => {
        identity.textContent = "";
        identity.hidden = true;
      });
    }
    const objectId = selectedObjectId ? String(selectedObjectId) : "";
    if (includeFactoryOptions && objectId && !objectId.includes(":installation.wall")) {
      const saved = currentDocument.factoryDrawingElementOptions?.find(
        (item) => item.objectId === objectId
      );
      const group = document.createElement("fieldset");
      group.className = "object-inspector__factory-output";
      group.dataset.factoryDrawingObjectId = objectId;
      const legend = document.createElement("legend");
      legend.textContent = "工厂图输出";
      const number = document.createElement("label");
      number.textContent = "图面短编号";
      const numberInput = document.createElement("input");
      numberInput.type = "text";
      numberInput.maxLength = 32;
      numberInput.placeholder = "自动生成";
      numberInput.value = saved?.factoryDrawingNumber ?? "";
      numberInput.dataset.factoryDrawingNumber = "true";
      number.append(numberInput);
      const dimensions = document.createElement("label");
      const dimensionsInput = document.createElement("input");
      dimensionsInput.type = "checkbox";
      dimensionsInput.checked = saved?.showDimensions ?? true;
      dimensionsInput.dataset.factoryDrawingOption = "dimensions";
      dimensions.append(dimensionsInput, " 在图面显示该元件尺寸");
      const table = document.createElement("label");
      const tableInput = document.createElement("input");
      tableInput.type = "checkbox";
      tableInput.checked = saved?.showInComponentTable ?? true;
      tableInput.dataset.factoryDrawingOption = "table";
      table.append(tableInput, " 收录到独立组成件表");
      const hint = document.createElement("p");
      hint.className = "object-inspector__hint";
      hint.textContent = "短编号只用于图纸；内部图元ID保持不变。两项显隐互不绑定。";
      group.append(legend, number, dimensions, table, hint);
      target.append(group);
    }
    container.replaceChildren(target);
  };

  /** Refreshes orientation-dependent millimetres without mutating domain state. */
  const refreshDependentFields = (form: HTMLFormElement, useMemberRatios: boolean): void => {
    const context = selectedTopologyMember(currentDocument, selectedObjectId);
    if (!context) return;
    const orientation = form.elements.namedItem("orientation");
    const throughMode = form.elements.namedItem("throughMode");
    const position = form.elements.namedItem("positionMm");
    const spanStart = form.elements.namedItem("spanStartMm");
    const spanEnd = form.elements.namedItem("spanEndMm");
    if (
      !(orientation instanceof HTMLSelectElement) ||
      !(throughMode instanceof HTMLSelectElement) ||
      !(position instanceof HTMLInputElement) ||
      !(spanStart instanceof HTMLInputElement) ||
      !(spanEnd instanceof HTMLInputElement)
    ) return;
    const direction = orientation.value === "horizontal" ? "horizontal" : "vertical";
    const positionHostSize = memberHostAxisSizeMm(
      context,
      direction === "vertical" ? "horizontal" : "vertical"
    );
    const spanHostSize = memberHostAxisSizeMm(
      context,
      direction === "vertical" ? "vertical" : "horizontal"
    );
    const edgeClearance = Math.min(
      Math.max(120, positionHostSize * 0.08),
      Math.max(1, positionHostSize / 2 - 1)
    );
    position.min = inspectorMillimetres(edgeClearance);
    position.max = inspectorMillimetres(positionHostSize - edgeClearance);
    spanStart.max = inspectorMillimetres(spanHostSize * 0.95);
    spanEnd.max = inspectorMillimetres(spanHostSize);
    if (useMemberRatios) {
      position.value = inspectorMillimetres(context.member.positionRatio * positionHostSize);
      spanStart.value = inspectorMillimetres(context.member.span.startRatio * spanHostSize);
      spanEnd.value = inspectorMillimetres(context.member.span.endRatio * spanHostSize);
    }
    const continuous = throughMode.value === "continuous";
    spanStart.disabled = continuous;
    spanEnd.disabled = continuous;
    if (continuous) {
      spanStart.value = "0";
      spanEnd.value = inspectorMillimetres(spanHostSize);
    }
  };

  /**
   * Synchronizes the cell inspector after a construction-type change.
   *
   * Algorithm: fixed glass disables opening inputs; top-hung forces the
   * one-panel assembly and exposes only top-in/top-out; tilt-turn restores the
   * side directions and single/double variants. One form definition is shared
   * by desktop and mobile, so both shells emit identical domain commands.
   *
   * @param form Active cell inspector form.
   * @param preferredOpening Optional persisted value to preserve on render.
   * @example Selecting `top_hung` chooses `top_out` and locks assembly to single.
   * @since 0.7.0
   * @modified 2026-09-17 - Added top-hung inspector synchronization.
   */
  const syncCellOpeningControls = (
    form: HTMLFormElement,
    preferredOpening?: string,
    preserveConfiguredAngles = false
  ): void => {
    const cellType = form.elements.namedItem("cellType");
    const assemblyMode = form.elements.namedItem("assemblyMode");
    const opening = form.elements.namedItem("opening");
    const hardware = form.elements.namedItem("hardwareSetId");
    const primaryMaximum = form.elements.namedItem("primaryMaximumAngleDegrees");
    const tiltMaximum = form.elements.namedItem("tiltMaximumAngleDegrees");
    const slidingOverlap = form.elements.namedItem("slidingOverlapMm");
    const openingLabel = form.querySelector<HTMLElement>("[data-opening-label]");
    const openingHint = form.querySelector<HTMLElement>("[data-opening-hint]");
    const primaryMaximumLabel = form.querySelector<HTMLElement>("[data-primary-maximum-label]");
    const tiltMaximumLabel = form.querySelector<HTMLElement>("[data-tilt-maximum-label]");
    const slidingOverlapLabel = form.querySelector<HTMLElement>("[data-sliding-overlap-label]");
    if (
      !(cellType instanceof HTMLSelectElement) ||
      !(assemblyMode instanceof HTMLSelectElement) ||
      !(opening instanceof HTMLSelectElement)
    ) return;
    const fixed = cellType.value === "fixed_glass";
    const topHung = cellType.value === "top_hung";
    const sliding = cellType.value === "sliding";
    if (topHung) assemblyMode.value = "single";
    if (fixed) assemblyMode.value = "single";
    if (sliding) assemblyMode.value = "sliding_standard";
    for (const option of [...assemblyMode.options]) {
      option.hidden = sliding
        ? option.value !== "sliding_standard"
        : option.value === "sliding_standard";
      option.disabled = option.hidden;
    }
    assemblyMode.disabled = fixed || topHung || sliding;
    const fixedDouble = !fixed && !topHung && !sliding &&
      assemblyMode.value === "double_fixed";
    const flyingDouble = !fixed && !topHung && !sliding &&
      assemblyMode.value === "double_flying";
    const openingTextByValue: Readonly<Record<string, string>> = flyingDouble
      ? {
          left_in: "左主动扇 · 内开（左右扇各自铰接）",
          right_in: "右主动扇 · 内开（左右扇各自铰接）",
          left_out: "左主动扇 · 外开（左右扇各自铰接）",
          right_out: "右主动扇 · 外开（左右扇各自铰接）",
          independent: "左右独立内平开（左扇左铰，右扇右铰）",
          top_out: "上悬外开（上铰）",
          top_in: "上悬内开（上铰，特殊构造）",
          slide_left: "活动扇向左收拢（室内视）",
          slide_right: "活动扇向右收拢（室内视）"
        }
      : {
          left_in: "左开内平开（室内视，左铰，可内倒）",
          right_in: "右开内平开（室内视，右铰，可内倒）",
          left_out: "左开外平开（室内视，左铰）",
          right_out: "右开外平开（室内视，右铰）",
          independent: "左右独立内平开（左扇左铰，右扇右铰）",
          top_out: "上悬外开（上铰）",
          top_in: "上悬内开（上铰，特殊构造）",
          slide_left: "活动扇向左收拢（室内视）",
          slide_right: "活动扇向右收拢（室内视）"
        };
    for (const option of [...opening.options]) {
      option.textContent = openingTextByValue[option.value] ?? option.textContent;
      const valid = sliding
        ? option.value === "slide_left" || option.value === "slide_right"
        : topHung
        ? option.value === "top_in" || option.value === "top_out"
        : option.value === "left_in" || option.value === "right_in" ||
          option.value === "left_out" || option.value === "right_out" ||
          option.value === "independent";
      option.hidden = !valid;
      option.disabled = !valid;
    }
    const requested = preferredOpening ?? opening.value;
    opening.value = sliding
      ? requested === "slide_left" ? "slide_left" : "slide_right"
      : topHung
      ? requested === "top_in" ? "top_in" : "top_out"
      : fixedDouble
        ? "independent"
        : requested === "left_in" || requested === "right_in" ||
            requested === "left_out" || requested === "right_out"
          ? requested
          : "right_in";
    opening.disabled = fixed || fixedDouble;
    if (openingLabel) {
      openingLabel.firstChild!.textContent = topHung
        ? "开启形式"
        : sliding
          ? "推拉方向"
        : fixedDouble
          ? "双扇开向"
          : flyingDouble
            ? "主动扇与开启面"
            : "开启方向";
    }
    if (openingHint) {
      openingHint.textContent = sliding
        ? "方向采用室内视：向左/向右表示活动扇关闭后开启时的收拢方向。当前为中性两扇两轨分配；真实轨槽、滚轮和排水构造由审核目录确定。"
        : flyingDouble
        ? "开向采用室内标志面；左右表示主动扇位置，两个窗扇仍分别以各自左/右侧为铰轴，内/外表示运动面。"
        : fixedDouble
          ? "固定中梃双扇没有单一左/右开向：左扇左铰、右扇右铰，当前分别独立内开。"
          : "开向采用室内标志面：左开/右开表示铰轴在左/右，内开/外开表示窗扇运动到室内/室外；不使用含糊的“左手/右手”叫法。";
    }
    if (hardware instanceof HTMLInputElement) hardware.disabled = fixed;
    const supportsTilt = !fixed && !topHung && !sliding && opening.value.endsWith("_in");
    if (primaryMaximum instanceof HTMLInputElement) {
      primaryMaximum.disabled = fixed || sliding;
      if (!preserveConfiguredAngles) {
        primaryMaximum.value = topHung ? "41.3" : "90";
      }
    }
    if (primaryMaximumLabel) {
      primaryMaximumLabel.hidden = fixed || sliding;
      const title = primaryMaximumLabel.querySelector<HTMLElement>("[data-angle-title]");
      if (title) title.textContent = topHung ? "上悬最大开启角度（°）" : "平开最大开启角度（°）";
    }
    if (tiltMaximum instanceof HTMLInputElement) {
      tiltMaximum.disabled = !supportsTilt;
      if (!preserveConfiguredAngles) tiltMaximum.value = "18.3";
    }
    if (tiltMaximumLabel) tiltMaximumLabel.hidden = !supportsTilt;
    if (slidingOverlap instanceof HTMLInputElement) {
      slidingOverlap.disabled = !sliding;
      if (sliding && !preserveConfiguredAngles && !slidingOverlap.value) {
        slidingOverlap.value = "35";
      }
    }
    if (slidingOverlapLabel) slidingOverlapLabel.hidden = !sliding;
  };

  /** Rebuilds the small form when document revision or stable selection changes. */
  const render = (): void => {
    const context = selectedTopologyMember(currentDocument, selectedObjectId);
    if (!context) {
      const hardwareContext = selectedWindowHardware(currentDocument, selectedObjectId);
      if (hardwareContext) {
        const { window, mount } = hardwareContext;
        const configuration = resolveWindowVisualConfigurationForRender(window);
        const model = resolveHardwareComponentModel(
          configuration,
          mount.role,
          mount.hardwareId
        );
        const card = document.createElement("section");
        card.className = "object-inspector__form";
        card.dataset.inspectorKind = "hardware";
        card.innerHTML = `
          <p class="object-inspector__identity"><strong data-hardware-name></strong><span data-hardware-id></span></p>
          <p class="object-inspector__hint">当前选中单件五金；此处显示对应型号、外形尺寸和安装位置。</p>
          <p data-hardware-model></p>
          <p data-hardware-size></p>
          <p data-hardware-mount></p>`;
        const name = card.querySelector<HTMLElement>("[data-hardware-name]");
        const identity = card.querySelector<HTMLElement>("[data-hardware-id]");
        const modelLine = card.querySelector<HTMLElement>("[data-hardware-model]");
        const sizeLine = card.querySelector<HTMLElement>("[data-hardware-size]");
        const mountLine = card.querySelector<HTMLElement>("[data-hardware-mount]");
        if (name) name.textContent = `${HARDWARE_ROLE_LABELS[mount.role]} · ${mount.panelId}`;
        if (identity) identity.textContent = mount.hardwareId;
        if (modelLine) {
          modelLine.textContent = model
            ? `型号：${model.businessName ?? (presentation === "factory-workbench" ? "已关联目录型号" : model.modelId)}` +
              `${presentation === "factory-workbench" ? "" : ` · ${model.modelVersion}`}`
            : "型号：未关联";
        }
        if (sizeLine) {
          sizeLine.textContent = model
            ? `实际尺寸：${model.dimensionsMm.widthMm} × ${model.dimensionsMm.heightMm} × ${model.dimensionsMm.depthMm} mm`
            : "实际尺寸：未解析";
        }
        if (mountLine) {
          const targetLabel = mount.mountTarget === "frame" ? "窗框" : "窗扇";
          const edgeLabel = ({ left: "左侧", right: "右侧", top: "上侧", bottom: "下侧" } as const)[mount.edge];
          mountLine.textContent = presentation === "factory-workbench"
            ? `安装位置：${targetLabel} · ${edgeLabel}`
            : `安装：${mount.mountTarget} · ${mount.edge} · 归属${mount.mountOwnerPanelId ?? mount.panelId}`;
        }
        insertProductTemplateNotice(card, window);
        replaceInspectorContent(card);
        return;
      }
      const cellContext = selectedWindowCell(currentDocument, selectedObjectId);
      if (cellContext) {
        const form = document.createElement("form");
        form.className = "object-inspector__form";
        form.dataset.inspectorKind = "cell";
        form.dataset.windowId = cellContext.window.objectId;
        form.dataset.cellId = cellContext.cell.objectId;
        form.innerHTML = `
          <p class="object-inspector__identity"><strong data-cell-name></strong><span data-cell-id></span></p>
          <label>构件类型<select name="cellType"><option value="fixed_glass">固定玻璃</option><option value="turn_tilt">平开/内倒窗</option><option value="top_hung">上悬窗</option><option value="sliding">普通推拉窗</option></select></label>
          <label>扇型组合<select name="assemblyMode"><option value="single">单扇</option><option value="double_flying">双扇（飞梃）</option><option value="double_fixed">双扇（固定中梃）</option><option value="sliding_standard">两扇两轨（标准）</option></select></label>
          <label data-opening-label>开启方向<select name="opening"><option value="left_in">左开内平开（室内视，左铰，可内倒）</option><option value="right_in">右开内平开（室内视，右铰，可内倒）</option><option value="left_out">左开外平开（室内视，左铰）</option><option value="right_out">右开外平开（室内视，右铰）</option><option value="independent">左右独立内平开（左扇左铰，右扇右铰）</option><option value="top_out">上悬外开（上铰）</option><option value="top_in">上悬内开（上铰，特殊构造）</option><option value="slide_left">活动扇向左收拢（室内视）</option><option value="slide_right">活动扇向右收拢（室内视）</option></select></label>
          <p class="object-inspector__hint" data-opening-hint>开向采用室内标志面：左开/右开表示铰轴在左/右，内开/外开表示窗扇运动到室内/室外；不使用含糊的“左手/右手”叫法。</p>
          <label data-primary-maximum-label><span data-angle-title>平开最大开启角度（°）</span><input name="primaryMaximumAngleDegrees" type="number" min="1" max="180" step="0.1" required /></label>
          <label data-tilt-maximum-label><span>内倒最大开启角度（°）</span><input name="tiltMaximumAngleDegrees" type="number" min="1" max="90" step="0.1" required /></label>
          <label data-sliding-overlap-label hidden>相邻扇搭接（mm）<input name="slidingOverlapMm" type="number" min="0" max="300" step="0.1" required disabled /></label>
          <label>五金套系<input name="hardwareSetId" type="text" /></label>
          <p class="object-inspector__hint" data-hardware-summary></p>
          <p class="object-inspector__hint">同一构件模型同时驱动2D、3D、玻璃净尺寸、扇料和五金BOM。</p>
          <button class="shell-button" type="submit">应用构件类型</button>
          <button class="shell-button" type="button" data-delete-window>删除当前窗</button>`;
        const cellName = form.querySelector<HTMLElement>("[data-cell-name]");
        const cellId = form.querySelector<HTMLElement>("[data-cell-id]");
        const cellType = form.elements.namedItem("cellType");
        const assemblyMode = form.elements.namedItem("assemblyMode");
        const opening = form.elements.namedItem("opening");
        const hardware = form.elements.namedItem("hardwareSetId");
        const primaryMaximum = form.elements.namedItem("primaryMaximumAngleDegrees");
        const tiltMaximum = form.elements.namedItem("tiltMaximumAngleDegrees");
        const slidingOverlap = form.elements.namedItem("slidingOverlapMm");
        const hardwareSummary = form.querySelector<HTMLElement>("[data-hardware-summary]");
        const selectedPanel = cellContext.cell.type === "fixed_glass"
          ? undefined
          : cellContext.cell.openingAssembly.panels.find((panel) =>
              createOpeningPanelKey(cellContext.cell.objectId, panel.id) === selectedObjectId
            );
        if (cellName) {
          cellName.textContent = selectedPanel
            ? `${cellContext.window.mark} · ${selectedPanel.id}窗扇（所属单元）`
            : `${cellContext.window.mark} · 单元构件`;
        }
        if (cellId) {
          cellId.textContent = selectedPanel
            ? createOpeningPanelKey(cellContext.cell.objectId, selectedPanel.id)
            : cellContext.cell.objectId;
        }
        if (cellType instanceof HTMLSelectElement) cellType.value = cellContext.cell.type;
        if (assemblyMode instanceof HTMLSelectElement) {
          assemblyMode.value = cellContext.cell.type === "sliding"
            ? "sliding_standard"
            : cellContext.cell.type === "turn_tilt" &&
            cellContext.cell.openingAssembly.panelCount === 2
            ? cellContext.cell.openingAssembly.mullionMode === "fixed_mullion"
              ? "double_fixed"
              : "double_flying"
            : "single";
          assemblyMode.disabled = cellContext.cell.type === "fixed_glass" ||
            cellContext.cell.type === "top_hung" || cellContext.cell.type === "sliding";
        }
        if (opening instanceof HTMLSelectElement) {
          const fixedDouble = cellContext.cell.type === "turn_tilt" &&
            cellContext.cell.openingAssembly.panelCount === 2 &&
            cellContext.cell.openingAssembly.mullionMode === "fixed_mullion";
          opening.value = fixedDouble
            ? "independent"
            : cellContext.cell.type === "turn_tilt" || cellContext.cell.type === "top_hung" ||
                cellContext.cell.type === "sliding"
              ? cellContext.cell.opening
              : "left_in";
          opening.disabled = cellContext.cell.type === "fixed_glass" || fixedDouble;
        }
        if (hardware instanceof HTMLInputElement) {
          hardware.value = cellContext.cell.type !== "fixed_glass"
            ? cellContext.cell.hardwareSetId
            : cellContext.window.defaultHardwareSetId;
          hardware.disabled = cellContext.cell.type === "fixed_glass";
        }
        const previewDefinition = collectOpeningPreviewPanelDefinitions(currentDocument).find(
          (definition) => definition.objectId === cellContext.cell.objectId
        );
        if (primaryMaximum instanceof HTMLInputElement) {
          primaryMaximum.value = previewDefinition
            ? String(Math.round((previewDefinition.maximumAngleDegreesByMode?.primary ?? 90) * 10) / 10)
            : "90";
        }
        if (tiltMaximum instanceof HTMLInputElement) {
          tiltMaximum.value = previewDefinition?.maximumAngleDegreesByMode?.tilt === undefined
            ? "18.3"
            : String(Math.round(previewDefinition.maximumAngleDegreesByMode!.tilt! * 10) / 10);
        }
        if (slidingOverlap instanceof HTMLInputElement) {
          slidingOverlap.value = cellContext.cell.type === "sliding"
            ? String(cellContext.cell.openingAssembly.overlapMm)
            : "35";
        }
        if (hardwareSummary) {
          if (cellContext.cell.type === "fixed_glass") {
            hardwareSummary.textContent = "连接结构：固定构件无开启五金。";
          } else if (cellContext.cell.type === "sliding") {
            const activePanel = cellContext.cell.openingAssembly.panels.find(
              (panel) => panel.movable
            );
            hardwareSummary.textContent =
              `结构：${cellContext.cell.openingAssembly.panelCount}扇${cellContext.cell.openingAssembly.trackCount}轨，` +
              `${activePanel?.id ?? "活动扇"}${activePanel?.travelDirection === "left" ? "向左" : "向右"}收拢，` +
              `设计搭接${cellContext.cell.openingAssembly.overlapMm}mm；轨槽、滚轮、密封与排水构造待审核目录。`;
          } else {
            const geometry = resolveWindowGeometry(cellContext.window);
            const mounts = geometry.hardware.filter(
              (mount) => mount.sourceObjectId === cellContext.cell.objectId
            );
            const hingeCount = mounts.filter((mount) => mount.role === "hinge-sash-leaf").length;
            const lockCount = mounts.filter((mount) => mount.role === "lock-point").length;
            const handleCount = mounts.filter((mount) => mount.role === "handle").length;
            const secondaryLeverCount = mounts.filter(
              (mount) => mount.role === "secondary-lever"
            ).length;
            const shootBoltCount = mounts.filter((mount) => mount.role === "shoot-bolt").length;
            const shootBoltKeeperCount = mounts.filter(
              (mount) => mount.role === "shoot-bolt-keeper" && mount.mountTarget === "frame"
            ).length;
            const flyingKeeperCount = mounts.filter(
              (mount) => mount.role === "keeper" && mount.mountTarget === "flying-mullion"
            ).length;
            const fixedKeeperCount = mounts.filter(
              (mount) => mount.role === "keeper" && mount.mountTarget === "fixed-mullion"
            ).length;
            const meeting = geometry.meetingMullions.find(
              (candidate) => candidate.sourceObjectId === cellContext.cell.objectId
            );
            const meetingCenterMm = meeting ? meeting.xMm + meeting.widthMm / 2 : 0;
            const flying = meeting
              ? meeting.kind === "fixed-mullion"
                ? `，固定中梃归属外框；左右扇独立启闭，${fixedKeeperCount}个锁座安装在固定中梃；净内径${Math.round(meeting.leftInnerWidthMm)}/${Math.round(meeting.rightInnerWidthMm)}mm，外径${Math.round(meetingCenterMm)}/${Math.round(cellContext.window.widthMm - meetingCenterMm)}mm`
                : `，飞梃归属${meeting.ownerPanelId}从动扇；主动扇${flyingKeeperCount}个锁座安装在飞梃上，从动扇由${secondaryLeverCount}个操作杆驱动${shootBoltCount}个上下天地插销并锁入外框${shootBoltKeeperCount}个锁座；内径${Math.round(meeting.leftInnerWidthMm)}/${Math.round(meeting.rightInnerWidthMm)}mm，外径${Math.round(meetingCenterMm)}/${Math.round(cellContext.window.widthMm - meetingCenterMm)}mm`
              : "";
            hardwareSummary.textContent = cellContext.cell.type === "top_hung"
              ? `连接结构：${hingeCount}组摩擦铰链/风撑沿顶部连接窗扇与窗框，${handleCount}个底部执手；供应商锁闭与加工模板待配置。`
              : `连接结构：${hingeCount}组合页连接窗扇与窗框，${lockCount}组锁点/锁座，${handleCount}个执手${flying}。`;
          }
        }
        syncCellOpeningControls(
          form,
          cellContext.cell.type === "fixed_glass" ? undefined : cellContext.cell.opening,
          true
        );
        insertProductTemplateNotice(form, cellContext.window);
        replaceInspectorContent(form);
        return;
      }
      const selectedId = selectedObjectId ? String(selectedObjectId) : "";
      const installationAssembly = (currentDocument.assemblies ?? []).find((assembly) => {
        const wallId = createWindowInstallationWallObjectId(assembly.objectId);
        const surroundId = createWindowInstallationSurroundObjectId(assembly.objectId);
        return selectedId === wallId || selectedId.startsWith(`${wallId}.`) ||
          selectedId === surroundId || selectedId.startsWith(`${surroundId}.`);
      });
      const installationWindow = selectedOwningWindow(currentDocument, selectedObjectId);
      if (installationWindow && selectedId.includes(":installation.wall")) {
        const installation = normalizeWindowInstallation(
          installationAssembly?.installation ?? installationWindow.installation
        );
        const card = document.createElement("section");
        card.className = "object-inspector__form";
        card.innerHTML = `
          <p class="object-inspector__identity"><strong>安装墙体（环境对象）</strong><span></span></p>
          <p class="object-inspector__hint">墙体不属于窗体和包边，不进入门窗产品BOM。</p>
          <p>墙厚：${installation.surround.wallThicknessMm} mm</p>
          <p>墙体类型：${installation.surround.wallMaterialId}</p>
          <p>框位：${installation.surround.frameAlignment} / ${installation.surround.frameOffsetMm} mm</p>`;
        const identity = card.querySelector("span");
        if (identity && presentation === "technical") identity.textContent = selectedId;
        else if (identity) identity.hidden = true;
        container.replaceChildren(card);
        return;
      }
      if (installationWindow && selectedId.includes(":installation.surround")) {
        const installation = normalizeWindowInstallation(
          installationAssembly?.installation ?? installationWindow.installation
        );
        const baseWidthMm = installationAssembly
          ? resolveFabricationAssemblyGeometry(
              installationAssembly,
              currentDocument.windows
            ).bounds.widthMm
          : installationWindow.widthMm;
        const outsideVisible = installation.surround.enabled &&
          (installation.surround.styleId === "both_sides" ||
            installation.surround.styleId === "outside_only");
        const horizontalSideCount = outsideVisible
          ? Number(installation.surround.sides.includes("left")) +
            Number(installation.surround.sides.includes("right"))
          : 0;
        const outerWidthMm = baseWidthMm +
          installation.surround.outsideWidthMm * horizontalSideCount;
        const form = document.createElement("form");
        form.className = "object-inspector__form";
        form.dataset.inspectorKind = "surround";
        form.dataset.windowId = installationAssembly ? "" : installationWindow.objectId;
        form.dataset.assemblyId = installationAssembly?.objectId ?? "";
        form.dataset.surroundBaseWidthMm = String(baseWidthMm);
        form.dataset.surroundHorizontalSideCount = String(horizontalSideCount);
        form.innerHTML = `
          <p class="object-inspector__identity"><strong>安装包边/衬板（材料对象）</strong><span></span></p>
          <p class="object-inspector__hint">包边依附洞口外周，但材质、尺寸和BOM均与墙体分离。</p>
          <p>状态：${installation.surround.enabled ? "已启用" : "未启用"}</p>
          <p>材料：${installation.surround.materialCode}</p>
          <label>外包边宽（单侧，mm）<input name="outsideWidthMm" type="number" min="0" max="500" step="0.1" required /></label>
          <label>包边外宽（总外廓，mm）<input name="outerWidthMm" type="number" min="${baseWidthMm}" max="${baseWidthMm + horizontalSideCount * 500}" step="0.1" ${horizontalSideCount > 0 ? "required" : "disabled"} /></label>
          <p class="object-inspector__hint">总外廓宽由窗/组合宽度与左右已启用包边推导；修改任一宽度会同步另一项。衬板厚：${installation.surround.boardThicknessMm} mm。</p>
          <button class="shell-button" type="submit">应用包边尺寸</button>`;
        const identity = form.querySelector("span");
        if (identity) identity.textContent = selectedId;
        const outsideWidth = form.elements.namedItem("outsideWidthMm");
        const outerWidth = form.elements.namedItem("outerWidthMm");
        if (outsideWidth instanceof HTMLInputElement) {
          outsideWidth.value = inspectorMillimetres(installation.surround.outsideWidthMm);
        }
        if (outerWidth instanceof HTMLInputElement) {
          outerWidth.value = inspectorMillimetres(outerWidthMm);
        }
        replaceInspectorContent(form);
        return;
      }
      /**
       * Keeps aggregate and joint selections from falling through to the first
       * member window's resize/delete form. An assembly root is not a window;
       * a selected joint receives its own connection editor instead of making
       * an arbitrary first member appear selected.
       * @since 0.10.51
       * @modified 2026-09-22 - Added the dedicated engineering-joint editor.
       */
      const assemblySelection = (currentDocument.assemblies ?? []).find(
        (assembly) => assembly.objectId === selectedObjectId ||
          assembly.joints.some((joint) => joint.objectId === selectedObjectId)
      );
      if (assemblySelection) {
        const joint = assemblySelection.joints.find(
          (candidate) => candidate.objectId === selectedObjectId
        );
        if (joint) {
          const verticalConnector =
            (joint.firstEdge === "left" || joint.firstEdge === "right") &&
            (joint.secondEdge === "left" || joint.secondEdge === "right");
          const catalogOptions = listEngineeringJointCatalogSelections(
            joint.jointType === "corner_joint"
              ? "corner"
              : verticalConnector ? "left-right" : "top-bottom"
          );
          const inferredCatalogSelection = joint.catalogSelection ??
            findEngineeringJointCatalogSelection({
              jointType: joint.jointType,
              finishedWidthMm: joint.gapMm
            });
          const selectedCatalogItemId = joint.catalogSelection?.catalogItemId ??
            inferredCatalogSelection?.catalogItemId ??
            "legacy-custom";
          const catalogOptionMarkup = [
            ...(selectedCatalogItemId === "legacy-custom"
              ? [`<option value="legacy-custom">旧项目自定义连接 · ${joint.gapMm}mm</option>`]
              : []),
            ...catalogOptions.map((selection) =>
              `<option value="${selection.catalogItemId}"${selection.catalogItemId === selectedCatalogItemId ? " selected" : ""}>${selection.businessName} · ${selection.specification}</option>`
            )
          ].join("");
          const form = document.createElement("form");
          form.className = "object-inspector__form";
          form.dataset.inspectorKind = "joint";
          form.dataset.assemblyId = assemblySelection.objectId;
          form.dataset.jointId = joint.objectId;
          form.innerHTML = `
            <p class="object-inspector__identity"><strong data-joint-name></strong><span data-joint-id></span></p>
            <label>连接件型号<select name="jointCatalog" required>${catalogOptionMarkup}</select></label>
            <p class="object-inspector__hint" data-joint-catalog-summary></p>
            ${joint.jointType === "corner_joint" ? `
              <div class="object-inspector__span">
                <label>转角内夹角（°）<input name="cornerAngle" type="number" min="1" max="179.9" step="0.1" required /></label>
                <label>俯视转向<select name="cornerTurn"><option value="clockwise">顺时针</option><option value="counterclockwise">逆时针</option></select></label>
              </div>` : ""}
            <label>连接范围<select name="factoryScope"><option value="factory">工厂连接</option><option value="site">现场连接</option></select></label>
            <p class="object-inspector__hint">${presentation === "factory-workbench"
              ? "选择连接件目录型号后会带入对应规格；当前为参考选型，需企业正式型号确认后才能用于生产。"
              : "设计人员选择连接件型号；类型、成品宽度和制造规则由该目录版本带入并冻结。当前为DoorMes参考目录，企业正式目录导入前不得视为审核生产数据。"}</p>
            <button class="shell-button" type="submit">应用连接设置</button>`;
          const name = form.querySelector<HTMLElement>("[data-joint-name]");
          const id = form.querySelector<HTMLElement>("[data-joint-id]");
          const scope = form.elements.namedItem("factoryScope");
          const summary = form.querySelector<HTMLElement>("[data-joint-catalog-summary]");
          const cornerAngle = form.elements.namedItem("cornerAngle");
          const cornerTurn = form.elements.namedItem("cornerTurn");
          const typeLabel = joint.jointType === "reinforced_mullion"
            ? "加强拼樘"
            : joint.jointType === "stacking_joint"
              ? "上下叠接"
              : joint.jointType === "corner_joint"
                ? "转角连接"
                : "普通拼樘";
          if (name) name.textContent = `${typeLabel} · 连接件属性`;
          if (id) id.textContent = joint.objectId;
          if (scope instanceof HTMLSelectElement) scope.value = joint.factoryScope;
          if (cornerAngle instanceof HTMLInputElement && joint.cornerConfiguration) {
            cornerAngle.value = inspectorMillimetres(joint.cornerConfiguration.includedAngleDeg);
            if (inferredCatalogSelection?.cornerCapability) {
              cornerAngle.min = String(
                inferredCatalogSelection.cornerCapability.minimumIncludedAngleDeg
              );
              cornerAngle.max = String(
                inferredCatalogSelection.cornerCapability.maximumIncludedAngleDeg
              );
            }
          }
          if (cornerTurn instanceof HTMLSelectElement && joint.cornerConfiguration) {
            cornerTurn.value = joint.cornerConfiguration.turnDirection;
          }
          if (summary) {
            summary.textContent = inferredCatalogSelection
              ? presentation === "factory-workbench"
                ? `${inferredCatalogSelection.businessName} · ${inferredCatalogSelection.specification}；成品宽度 ${inspectorMillimetres(joint.gapMm)} mm`
                : `${inferredCatalogSelection.specification}；制造映射 ${inferredCatalogSelection.manufacturingRuleId}@${inferredCatalogSelection.manufacturingRuleVersion}`
              : presentation === "factory-workbench"
                ? `旧项目自定义连接：${typeLabel}，成品宽度 ${inspectorMillimetres(joint.gapMm)} mm；请选择目录中的连接件型号。`
                : `旧项目自定义连接：${typeLabel}，成品宽度 ${inspectorMillimetres(joint.gapMm)}mm；请选择目录型号完成版本冻结。`;
          }
          replaceInspectorContent(form);
          return;
        }
        const card = document.createElement("section");
        card.className = "object-inspector__form";
        const identity = document.createElement("p");
        identity.className = "object-inspector__identity";
        const name = document.createElement("strong");
        const id = document.createElement("span");
        name.textContent = `${assemblySelection.mark} · 制造组合`;
        id.textContent = assemblySelection.objectId;
        identity.append(name, id);
        const hint = document.createElement("p");
        hint.className = "object-inspector__hint";
        hint.textContent = `当前组合包含${assemblySelection.instances.length}樘窗；请选择具体窗单元实例后再修改尺寸或删除窗。`;
        card.append(identity, hint);
        replaceInspectorContent(card);
        return;
      }
      const window = installationWindow;
      if (window) {
        const form = document.createElement("form");
        form.className = "object-inspector__form";
        form.dataset.inspectorKind = "window";
        form.dataset.windowId = window.objectId;
        form.innerHTML = `
          <p class="object-inspector__identity"><strong data-window-name></strong><span data-window-id></span></p>
          <label>窗编号<input name="mark" type="text" maxlength="64" autocomplete="off" required /></label>
          <div class="object-inspector__span">
            <label>总宽（mm）<input name="widthMm" type="number" min="100" max="20000" step="0.1" required /></label>
            <label>总高（mm）<input name="heightMm" type="number" min="100" max="20000" step="0.1" required /></label>
          </div>
          <p class="object-inspector__hint">编号用于图面、工厂表格和业务追溯；产品模板与型号另行维护。同一设计内编号不可重复。</p>
          <p class="object-inspector__identity"><strong>工厂图组成件备注</strong><span data-business-label>用户维护</span></p>
          <label>型材备注<textarea name="profileRemark" rows="2" maxlength="500"></textarea></label>
          <label>玻璃备注<textarea name="glassRemark" rows="2" maxlength="500"></textarea></label>
          <label>五金备注<textarea name="hardwareRemark" rows="2" maxlength="500"></textarea></label>
          <label>包边/衬板备注<textarea name="surroundRemark" rows="2" maxlength="500"></textarea></label>
          <p class="object-inspector__hint">备注仅作为设计说明进入工厂图组成件表，不改变目录型号、渲染参数或生产计算。</p>
          <button class="shell-button" type="submit">应用窗体信息</button>
          <button class="shell-button" type="button" data-delete-window>删除当前窗</button>`;
        const windowName = form.querySelector<HTMLElement>("[data-window-name]");
        const windowId = form.querySelector<HTMLElement>("[data-window-id]");
        const mark = form.elements.namedItem("mark");
        const width = form.elements.namedItem("widthMm");
        const height = form.elements.namedItem("heightMm");
        const profileRemark = form.elements.namedItem("profileRemark");
        const glassRemark = form.elements.namedItem("glassRemark");
        const hardwareRemark = form.elements.namedItem("hardwareRemark");
        const surroundRemark = form.elements.namedItem("surroundRemark");
        const remarks = window.designComponentRemarks ?? {
          profile: "", glass: "", hardware: "", surround: ""
        };
        if (windowName) windowName.textContent = `${window.mark} · 窗体属性`;
        if (windowId) windowId.textContent = window.objectId;
        if (mark instanceof HTMLInputElement) mark.value = window.mark;
        if (width instanceof HTMLInputElement) {
          width.value = inspectorMillimetres(window.widthMm);
        }
        if (height instanceof HTMLInputElement) {
          height.value = inspectorMillimetres(window.heightMm);
        }
        if (profileRemark instanceof HTMLTextAreaElement) profileRemark.value = remarks.profile;
        if (glassRemark instanceof HTMLTextAreaElement) glassRemark.value = remarks.glass;
        if (hardwareRemark instanceof HTMLTextAreaElement) hardwareRemark.value = remarks.hardware;
        if (surroundRemark instanceof HTMLTextAreaElement) surroundRemark.value = remarks.surround;
        insertProductTemplateNotice(form, window);
        replaceInspectorContent(form);
        return;
      }
      const status = document.createElement("p");
      status.className = "object-inspector__empty";
      status.textContent = selectedObjectId
        ? "当前对象没有可编辑的拓扑梃属性"
        : "选择窗体或构件后编辑总尺寸与构件属性";
      container.replaceChildren(status);
      return;
    }
    const form = document.createElement("form");
    form.className = "object-inspector__form";
    form.dataset.inspectorKind = "member";
    form.innerHTML = `
      <p class="object-inspector__identity"><strong data-member-name></strong><span data-host-name></span></p>
      <label>方向<select name="orientation"><option value="vertical">竖向</option><option value="horizontal">横向</option></select></label>
      <label>贯通方式<select name="throughMode"><option value="local">局部</option><option value="continuous">连续</option></select></label>
      <label>位置（mm）<input name="positionMm" type="number" step="0.1" required /></label>
      <div class="object-inspector__span">
        <label>跨度起点（mm）<input name="spanStartMm" type="number" min="0" step="0.1" required /></label>
        <label>跨度终点（mm）<input name="spanEndMm" type="number" min="0" step="0.1" required /></label>
      </div>
      <label>型材编码<input name="profileId" type="text" /></label>
      <div class="object-inspector__span">
        <label>起点连接<select name="connectionStart"><option value="butt">对接</option><option value="through">贯通</option></select></label>
        <label>终点连接<select name="connectionEnd"><option value="butt">对接</option><option value="through">贯通</option></select></label>
      </div>
      <label>备注<textarea name="note" rows="2"></textarea></label>
      <button class="shell-button" type="submit">应用构件属性</button>`;
    const memberName = form.querySelector<HTMLElement>("[data-member-name]");
    const hostName = form.querySelector<HTMLElement>("[data-host-name]");
    const orientation = form.elements.namedItem("orientation");
    const throughMode = form.elements.namedItem("throughMode");
    const profileId = form.elements.namedItem("profileId");
    const connectionStart = form.elements.namedItem("connectionStart");
    const connectionEnd = form.elements.namedItem("connectionEnd");
    const note = form.elements.namedItem("note");
    if (memberName) {
      memberName.textContent = presentation === "factory-workbench"
        ? `${context.window.mark} · ${context.member.orientation === "horizontal" ? "横向中梃" : "竖向中梃"}`
        : context.member.objectId;
    }
    if (hostName) {
      hostName.textContent = presentation === "factory-workbench"
        ? "所属窗体构件"
        : `宿主：${context.member.hostRegionId}`;
    }
    if (orientation instanceof HTMLSelectElement) orientation.value = context.member.orientation;
    if (throughMode instanceof HTMLSelectElement) throughMode.value = context.member.throughMode;
    if (profileId instanceof HTMLInputElement) profileId.value = context.member.profileId;
    if (connectionStart instanceof HTMLSelectElement) {
      connectionStart.value = context.member.connectionStart;
    }
    if (connectionEnd instanceof HTMLSelectElement) connectionEnd.value = context.member.connectionEnd;
    if (note instanceof HTMLTextAreaElement) note.value = context.member.note;
    insertProductTemplateNotice(form, context.window);
    replaceInspectorContent(form);
    refreshDependentFields(form, true);
  };

  /** Reprojects member ratios when orientation or through mode changes in-place. */
  const onChange = (event: Event): void => {
    const target = event.target;
    if (target instanceof HTMLInputElement && (
      target.dataset.factoryDrawingOption || target.dataset.factoryDrawingNumber
    )) {
      const group = target.closest<HTMLElement>("[data-factory-drawing-object-id]");
      const objectId = group?.dataset.factoryDrawingObjectId;
      const number = group?.querySelector<HTMLInputElement>(
        "[data-factory-drawing-number]"
      );
      const dimensions = group?.querySelector<HTMLInputElement>(
        '[data-factory-drawing-option="dimensions"]'
      );
      const table = group?.querySelector<HTMLInputElement>(
        '[data-factory-drawing-option="table"]'
      );
      if (!objectId || !number || !dimensions || !table) return;
      commandSequence += 1;
      try {
        number.setCustomValidity("");
        session.execute(createUpdateFactoryDrawingElementOptionsCommand({
          commandId: `CMD-FACTORY-DRAWING-OPTIONS-${session.document.revision}-${commandSequence}`,
          objectId,
          factoryDrawingNumber: number.value,
          showDimensions: dimensions.checked,
          showInComponentTable: table.checked
        }));
      } catch (error) {
        number.setCustomValidity(error instanceof Error ? error.message : "工厂图编号无效");
        number.reportValidity();
      }
      return;
    }
    const form = target instanceof Element ? target.closest<HTMLFormElement>("form") : null;
    if (!form) return;
    if (
      form.dataset.inspectorKind === "surround" &&
      target instanceof HTMLInputElement &&
      (target.name === "outsideWidthMm" || target.name === "outerWidthMm")
    ) {
      const outsideWidth = form.elements.namedItem("outsideWidthMm");
      const outerWidth = form.elements.namedItem("outerWidthMm");
      const baseWidthMm = Number(form.dataset.surroundBaseWidthMm);
      const sideCount = Number(form.dataset.surroundHorizontalSideCount);
      if (
        outsideWidth instanceof HTMLInputElement &&
        outerWidth instanceof HTMLInputElement &&
        Number.isFinite(baseWidthMm) &&
        sideCount > 0
      ) {
        if (target.name === "outerWidthMm") {
          outsideWidth.value = inspectorMillimetres(
            Math.max(0, (Number(target.value) - baseWidthMm) / sideCount)
          );
        } else {
          outerWidth.value = inspectorMillimetres(
            baseWidthMm + Math.max(0, Number(target.value)) * sideCount
          );
        }
      }
    } else if (target instanceof HTMLInputElement && target.name === "mark") {
      target.setCustomValidity("");
    } else if (target instanceof HTMLSelectElement && target.name === "jointCatalog") {
      target.setCustomValidity("");
      if (target.value === "legacy-custom") return;
      const selection = requireEngineeringJointCatalogSelection(target.value);
      const summary = form.querySelector<HTMLElement>("[data-joint-catalog-summary]");
      if (summary) {
        summary.textContent = presentation === "factory-workbench"
          ? `${selection.businessName} · ${selection.specification}`
          : `${selection.specification}；制造映射 ${selection.manufacturingRuleId}@${selection.manufacturingRuleVersion}`;
      }
      const scope = form.elements.namedItem("factoryScope");
      if (scope instanceof HTMLSelectElement) {
        const previous = scope.value;
        scope.replaceChildren(...selection.allowedFactoryScopes.map((value) => {
          const option = document.createElement("option");
          option.value = value;
          option.textContent = value === "factory" ? "工厂连接" : "现场连接";
          return option;
        }));
        scope.value = selection.allowedFactoryScopes.includes(
          previous as "factory" | "site"
        ) ? previous : selection.defaultFactoryScope;
      }
    } else if (target instanceof HTMLSelectElement && target.name === "orientation") {
      refreshDependentFields(form, true);
    } else if (target instanceof HTMLSelectElement && target.name === "throughMode") {
      refreshDependentFields(form, false);
    } else if (target instanceof HTMLSelectElement && target.name === "cellType") {
      const hardware = form.elements.namedItem("hardwareSetId");
      if (target.value === "top_hung" && hardware instanceof HTMLInputElement) {
        hardware.value = "HW-HUNG-STD";
      } else if (target.value === "sliding" && hardware instanceof HTMLInputElement) {
        hardware.value = "HW-SLIDE-STD";
      }
      syncCellOpeningControls(form);
    } else if (target instanceof HTMLSelectElement && target.name === "assemblyMode") {
      syncCellOpeningControls(form, undefined, true);
    } else if (target instanceof HTMLSelectElement && target.name === "opening") {
      syncCellOpeningControls(form, target.value, true);
    }
  };
  /**
   * Converts the visible inspector into exactly one shared application command.
   *
   * Window forms emit one mark/resize command or an atomic two-command
   * transaction; member and engineering-joint forms emit their complete
   * property command. Every path uses stable IDs and session history, so
   * PC/mobile produce one undo step and immediately invalidate all shared
   * render/BOM projections.
   *
   * @example Submitting 1400×1600 from mobile uses the same resize command as PC.
   * @since 0.4.7
   * @modified 2026-09-22 - Added undoable engineering-joint property editing.
   */
  const onSubmit = (event: SubmitEvent): void => {
    const form = event.target;
    if (!(form instanceof HTMLFormElement)) return;
    event.preventDefault();
    if (form.dataset.inspectorKind === "surround") {
      const assembly = (currentDocument.assemblies ?? []).find(
        (candidate) => candidate.objectId === form.dataset.assemblyId
      );
      const window = currentDocument.windows.find(
        (candidate) => candidate.objectId === form.dataset.windowId
      );
      if (!assembly && !window) return;
      const installation = normalizeWindowInstallation(
        assembly?.installation ?? window!.installation
      );
      const outsideWidthMm = Number(new FormData(form).get("outsideWidthMm"));
      if (!Number.isFinite(outsideWidthMm)) return;
      const nextInstallation = {
        ...installation,
        surround: {
          ...installation.surround,
          outsideWidthMm
        }
      };
      commandSequence += 1;
      if (assembly) {
        session.execute(createUpdateFabricationAssemblyInstallationCommand({
          commandId: `CMD-ASSEMBLY-SURROUND-${session.document.revision}-${commandSequence}`,
          assemblyId: assembly.objectId,
          installation: nextInstallation
        }));
        selection.select(createWindowInstallationSurroundObjectId(assembly.objectId), "2d");
      } else if (window) {
        session.execute(createUpdateWindowInstallationCommand({
          commandId: `CMD-WINDOW-SURROUND-${session.document.revision}-${commandSequence}`,
          windowId: window.objectId,
          installation: nextInstallation
        }));
        selection.select(createWindowInstallationSurroundObjectId(window.objectId), "2d");
      }
      return;
    }
    if (form.dataset.inspectorKind === "joint") {
      const assembly = (currentDocument.assemblies ?? []).find(
        (candidate) => candidate.objectId === form.dataset.assemblyId
      );
      const joint = assembly?.joints.find(
        (candidate) => candidate.objectId === form.dataset.jointId
      );
      if (!assembly || !joint) return;
      const values = new FormData(form);
      const catalogItemId = String(values.get("jointCatalog") ?? "");
      const catalogSelection = catalogItemId && catalogItemId !== "legacy-custom"
        ? requireEngineeringJointCatalogSelection(catalogItemId)
        : undefined;
      const requestedJointType = catalogSelection?.jointType ?? joint.jointType;
      const requestedGapMm = catalogSelection?.finishedWidthMm ?? joint.gapMm;
      const requestedScope = values.get("factoryScope") === "site" ? "site" : "factory";
      const requestedCornerConfiguration = requestedJointType === "corner_joint"
        ? {
            schemaVersion: "doormes-engineering-corner-joint.v1" as const,
            includedAngleDeg: Number(values.get("cornerAngle")),
            turnDirection: values.get("cornerTurn") === "counterclockwise"
              ? "counterclockwise" as const
              : "clockwise" as const
          }
        : undefined;
      if (
        requestedJointType === joint.jointType &&
        requestedGapMm === joint.gapMm &&
        requestedScope === joint.factoryScope &&
        requestedCornerConfiguration?.includedAngleDeg ===
          joint.cornerConfiguration?.includedAngleDeg &&
        requestedCornerConfiguration?.turnDirection ===
          joint.cornerConfiguration?.turnDirection &&
        catalogSelection?.catalogItemId === joint.catalogSelection?.catalogItemId &&
        catalogSelection?.catalogVersion === joint.catalogSelection?.catalogVersion
      ) return;
      const catalogInput = form.elements.namedItem("jointCatalog");
      try {
        commandSequence += 1;
        session.execute(createUpdateEngineeringJointCommand({
          commandId: `CMD-ASSEMBLY-JOINT-${session.document.revision}-${commandSequence}`,
          assemblyId: assembly.objectId,
          jointId: joint.objectId,
          jointType: requestedJointType,
          gapMm: requestedGapMm,
          factoryScope: requestedScope,
          ...(requestedCornerConfiguration ? {
            cornerConfiguration: requestedCornerConfiguration
          } : {}),
          ...(catalogSelection ? { catalogSelection } : {})
        }));
        if (catalogInput instanceof HTMLSelectElement) catalogInput.setCustomValidity("");
      } catch (error) {
        const message = error instanceof Error ? error.message : "连接设置更新失败。";
        if (catalogInput instanceof HTMLSelectElement) {
          catalogInput.setCustomValidity(message);
          catalogInput.reportValidity();
        } else if (typeof globalThis.alert === "function") {
          globalThis.alert(message);
        }
      }
      return;
    }
    if (form.dataset.inspectorKind === "window") {
      const window = currentDocument.windows.find(
        (candidate) => candidate.objectId === form.dataset.windowId
      );
      if (!window) return;
      const values = new FormData(form);
      const markInput = form.elements.namedItem("mark");
      const requestedMark = String(values.get("mark") ?? "").trim();
      const requestedWidthMm = Number(values.get("widthMm"));
      const requestedHeightMm = Number(values.get("heightMm"));
      const requestedRemarks = {
        profile: String(values.get("profileRemark") ?? "").trim(),
        glass: String(values.get("glassRemark") ?? "").trim(),
        hardware: String(values.get("hardwareRemark") ?? "").trim(),
        surround: String(values.get("surroundRemark") ?? "").trim()
      };
      const currentRemarks = window.designComponentRemarks ?? {
        profile: "", glass: "", hardware: "", surround: ""
      };
      commandSequence += 1;
      const transactionId = `CMD-WINDOW-PROPERTIES-${session.document.revision}-${commandSequence}`;
      const commands = [
        ...(requestedMark !== window.mark
          ? [createUpdateWindowMarkCommand({
              commandId: `${transactionId}:MARK`,
              windowId: window.objectId,
              mark: requestedMark
            })]
          : []),
        ...(requestedWidthMm !== window.widthMm || requestedHeightMm !== window.heightMm
          ? [createResizeWindowCommand({
              commandId: `${transactionId}:SIZE`,
              windowId: window.objectId,
              widthMm: requestedWidthMm,
              heightMm: requestedHeightMm
            })]
          : []),
        ...(Object.keys(requestedRemarks).some((key) =>
          requestedRemarks[key as keyof typeof requestedRemarks] !==
            currentRemarks[key as keyof typeof currentRemarks]
        )
          ? [createUpdateWindowDesignComponentRemarksCommand({
              commandId: `${transactionId}:REMARKS`,
              windowId: window.objectId,
              remarks: requestedRemarks
            })]
          : [])
      ];
      if (commands.length === 0) return;
      try {
        if (commands.length === 1) {
          session.execute(commands[0]!);
        } else {
          session.executeTransaction(commands, transactionId);
        }
        if (markInput instanceof HTMLInputElement) markInput.setCustomValidity("");
      } catch (error) {
        const message = error instanceof Error ? error.message : "窗体信息更新失败。";
        if (markInput instanceof HTMLInputElement) {
          markInput.setCustomValidity(message);
          markInput.reportValidity();
        } else if (typeof globalThis.alert === "function") {
          globalThis.alert(message);
        }
      }
      return;
    }
    if (form.dataset.inspectorKind === "cell") {
      const cellContext = selectedWindowCell(currentDocument, selectedObjectId);
      if (!cellContext || cellContext.cell.objectId !== form.dataset.cellId) return;
      const values = new FormData(form);
      const cellType = values.get("cellType") === "turn_tilt"
        ? "turn_tilt"
        : values.get("cellType") === "top_hung"
          ? "top_hung"
          : values.get("cellType") === "sliding"
            ? "sliding"
          : "fixed_glass";
      const requestedOpening = values.get("opening");
      const slidingOpening = requestedOpening === "slide_left"
        ? "slide_left" as const
        : "slide_right" as const;
      const opening = cellType === "fixed_glass"
        ? "fixed"
        : cellType === "sliding"
          ? slidingOpening
        : cellType === "top_hung"
          ? requestedOpening === "top_in" ? "top_in" : "top_out"
        : requestedOpening === "right_in"
          ? "right_in"
          : requestedOpening === "left_in"
            ? "left_in"
            : requestedOpening === "right_out"
              ? "right_out"
              : requestedOpening === "left_out"
                ? "left_out"
            : cellContext.cell.type === "turn_tilt"
              ? cellContext.cell.opening
              : "left_in";
      const assemblyMode = values.get("assemblyMode");
      const panelCount = cellType === "turn_tilt" &&
        (assemblyMode === "double_flying" || assemblyMode === "double_fixed") ? 2 : 1;
      const primaryMaximumAngleDegrees = Number(values.get("primaryMaximumAngleDegrees"));
      const tiltMaximumAngleDegrees = Number(values.get("tiltMaximumAngleDegrees"));
      const slidingOverlapMm = Number(values.get("slidingOverlapMm"));
      const slidingOpenPercent = cellContext.cell.type === "sliding"
        ? cellContext.cell.openingAssembly.openPercent
        : 80;
      commandSequence += 1;
      session.execute(
        createSetWindowCellOpeningCommand({
          commandId: `CMD-CELL-OPENING-${session.document.revision}-${commandSequence}`,
          windowId: cellContext.window.objectId,
          cellId: cellContext.cell.objectId,
          cellType,
          opening,
          panelCount,
          mullionMode: assemblyMode === "double_flying" ? "flying_mullion" : "fixed_mullion",
          hardwareSetId: String(values.get("hardwareSetId") ?? ""),
          maximumAngleDegreesByMode: cellType === "fixed_glass" || cellType === "sliding"
            ? undefined
            : {
                primary: primaryMaximumAngleDegrees,
                ...(cellType === "turn_tilt" && opening.endsWith("_in")
                  ? { tilt: tiltMaximumAngleDegrees }
                  : {})
              },
          slidingConfiguration: cellType === "sliding"
            ? createStandardSlidingConfiguration(
                slidingOpening,
                slidingOverlapMm,
                slidingOpenPercent
              )
            : undefined
        })
      );
      return;
    }
    const context = selectedTopologyMember(currentDocument, selectedObjectId);
    if (!context) return;
    const values = new FormData(form);
    const orientation = values.get("orientation") === "horizontal" ? "horizontal" : "vertical";
    const throughMode = values.get("throughMode") === "continuous" ? "continuous" : "local";
    commandSequence += 1;
    session.execute(
      createUpdateWindowTopologyMemberCommand({
        commandId: `CMD-TOPOLOGY-PROPERTIES-${session.document.revision}-${commandSequence}`,
        windowId: context.window.objectId,
        memberId: context.member.objectId,
        orientation,
        positionMm: Number(values.get("positionMm")),
        spanStartMm: throughMode === "continuous" ? 0 : Number(values.get("spanStartMm")),
        spanEndMm: throughMode === "continuous"
          ? memberHostAxisSizeMm(
              context,
              orientation === "vertical" ? "vertical" : "horizontal"
            )
          : Number(values.get("spanEndMm")),
        profileId: String(values.get("profileId") ?? ""),
        throughMode,
        connectionStart: values.get("connectionStart") === "through" ? "through" : "butt",
        connectionEnd: values.get("connectionEnd") === "through" ? "through" : "butt",
        note: String(values.get("note") ?? "")
      })
    );
  };
  /**
   * Removes the exact window represented by the active inspector form.
   *
   * Cell and panel selections resolve to their owning product; the domain then
   * decides whether an enclosing assembly is updated, dissolved or rejected as
   * disconnected. Selection is cleared only after a successful command so a
   * validation failure leaves the user's context intact.
   *
   * @example Deleting one member of a two-window pair leaves the survivor independent.
   * @since 0.10.51
   * @modified 2026-09-21 - Added shared desktop/mobile window deletion action.
   */
  const onClick = (event: MouseEvent): void => {
    const target = event.target;
    if (!(target instanceof Element)) return;
    const button = target.closest<HTMLButtonElement>("[data-delete-window]");
    if (!button || !container.contains(button)) return;
    const form = button.closest<HTMLFormElement>("form[data-window-id]");
    const windowId = form?.dataset.windowId;
    const designWindow = currentDocument.windows.find(
      (candidate) => candidate.objectId === windowId
    );
    if (!designWindow) return;
    const confirmed = typeof globalThis.confirm !== "function" || globalThis.confirm(
      `确认删除窗“${designWindow.mark}”？组合关系和连接件会同步更新，此操作可撤销。`
    );
    if (!confirmed) return;
    try {
      commandSequence += 1;
      session.execute(createDeleteWindowCommand({
        commandId: `CMD-WINDOW-DELETE-${session.document.revision}-${commandSequence}`,
        windowId: designWindow.objectId
      }));
      selection.select(undefined, "system");
    } catch (error) {
      const message = error instanceof Error ? error.message : "删除窗失败。";
      if (typeof globalThis.alert === "function") globalThis.alert(message);
    }
  };
  container.addEventListener("click", onClick);
  container.addEventListener("change", onChange);
  container.addEventListener("submit", onSubmit);
  const disposeDocument = session.subscribe((document) => {
    currentDocument = document;
    render();
  });
  const disposeSelection = selection.subscribe((state) => {
    selectedObjectId = state.objectId;
    render();
  });

  return () => {
    container.removeEventListener("click", onClick);
    container.removeEventListener("change", onChange);
    container.removeEventListener("submit", onSubmit);
    disposeDocument();
    disposeSelection();
    container.replaceChildren();
  };
}

/**
 * Presentation selected by the shell while catalog application and commands
 * stay shared. The factory workbench exposes business selections only; the
 * advanced variants retain developer-facing render/model configuration tools.
 */
export type AppearanceEditorPresentation =
  | "desktop-panel"
  | "mobile-steps"
  | "factory-workbench";

/** Structural history entry supplied by a local or future remote adapter. */
export interface AppearanceEditorVisualHistoryEntry {
  readonly entryId: string;
  readonly designId: string;
  readonly windowId: string;
  readonly windowMark: string;
  readonly sourceRevision: number;
  readonly capturedAtIso: string;
  readonly configuration: WindowVisualConfiguration;
}

/** Read-only history port; restoring still goes through the shared command session. */
export interface AppearanceEditorVisualHistoryGateway {
  list(
    designId: string,
    windowId: string
  ): readonly AppearanceEditorVisualHistoryEntry[];
}

/**
 * Asset infrastructure injected by the application composition root.
 *
 * The shared editor depends on an upload gateway, never IndexedDB or HTTP
 * directly, so desktop/mobile shells and tests use the same workflow with a
 * durable local adapter or an authenticated backend client.
 *
 * @since 0.10.11
 * @modified 2026-09-18 - Replaced direct storage access with an upload gateway.
 */
export interface AppearanceEditorAssetServices {
  readonly gateway?: ManagedGltfAssetGateway;
  readonly history?: AppearanceEditorVisualHistoryGateway;
  readonly now?: () => Date;
}

const APPEARANCE_SLOT_LABELS: Readonly<Record<WindowAppearanceEditorSlot, string>> = {
  "frame.outside": "窗框·室外面",
  "frame.inside": "窗框·室内面",
  "frame.edge": "窗框·侧面",
  "sash.outside": "窗扇·室外面",
  "sash.inside": "窗扇·室内面",
  "sash.edge": "窗扇·侧面",
  "mullion.outside": "固定中梃·室外面",
  "mullion.inside": "固定中梃·室内面",
  "mullion.edge": "固定中梃·侧面",
  "flyingMullion.outside": "飞梃·室外面",
  "flyingMullion.inside": "飞梃·室内面",
  "flyingMullion.edge": "飞梃·侧面",
  glass: "玻璃",
  wall: "墙体",
  surroundOutside: "室外包边",
  surroundInside: "室内包边",
  surroundLiner: "洞口衬板",
  hardwareDefault: "默认五金外观"
};

const APPEARANCE_MATERIAL_FAMILIES: readonly AppearanceMaterialFamily[] = [
  "metal",
  "glass",
  "wood",
  "stone",
  "plaster",
  "brick",
  "plastic",
  "rubber",
  "custom"
];

/**
 * Browser constraints for UV repeat inputs in the shared technical editor.
 *
 * `step` must be `any`: combining `min=0.001` with `step=0.1` makes the HTML
 * constraint grid 0.001, 0.101, 0.201... and incorrectly rejects ordinary
 * values such as 0.5, 1 and 2 before domain validation runs. The domain still
 * enforces the authoritative inclusive 0.001..1000 interval.
 *
 * @example A catalog author may enter `1`, `0.5` or `2.25`.
 * @since 0.10.25
 * @modified 2026-09-18 - Fixed native UV step-mismatch validation.
 */
export const APPEARANCE_UV_INPUT_CONSTRAINTS = Object.freeze({
  min: 0.001,
  max: 1_000,
  step: "any"
} as const);

/**
 * Converts the two visible UV repeat controls into a persistent appearance value.
 *
 * UV scale is an authored material property, so it is retained even when the
 * current draft has no texture bundle. This lets a catalog author set tiling
 * before assigning an asset and prevents a form redraw from silently restoring
 * `1 × 1`. Renderers naturally ignore the value until a texture is resolved.
 *
 * @param horizontal Raw horizontal repeat value from the appearance form.
 * @param vertical Raw vertical repeat value from the appearance form.
 * @returns A detached UV scale object for the versioned appearance snapshot.
 * @example `readAppearanceEditorUvScale("0.5", "2")` returns `{ x: 0.5, y: 2 }`.
 * @since 0.10.26
 * @modified 2026-09-18 - Preserved UV values independently of texture assignment.
 */
export function readAppearanceEditorUvScale(
  horizontal: FormDataEntryValue | null,
  vertical: FormDataEntryValue | null
): Readonly<{ readonly x: number; readonly y: number }> {
  const x = Number(horizontal);
  const y = Number(vertical);
  if (
    !Number.isFinite(x) ||
    !Number.isFinite(y) ||
    x < APPEARANCE_UV_INPUT_CONSTRAINTS.min ||
    x > APPEARANCE_UV_INPUT_CONSTRAINTS.max ||
    y < APPEARANCE_UV_INPUT_CONSTRAINTS.min ||
    y > APPEARANCE_UV_INPUT_CONSTRAINTS.max
  ) {
    throw new Error(
      `UV repeat must be between ${APPEARANCE_UV_INPUT_CONSTRAINTS.min} and ${APPEARANCE_UV_INPUT_CONSTRAINTS.max}.`
    );
  }
  return { x, y };
}

const HARDWARE_EDITOR_ROLES: readonly OpeningHardwareRole[] = [
  "handle",
  "secondary-lever",
  "hinge-sash-leaf",
  "hinge-frame-leaf",
  "lock-point",
  "keeper",
  "shoot-bolt",
  "shoot-bolt-keeper"
];

/** Import choices persisted into every managed GLB/glTF geometry snapshot. */
const COMPONENT_ASSET_SOURCE_UNITS: readonly ComponentAssetSourceUnit[] = [
  "millimeter",
  "centimeter",
  "meter",
  "inch"
];

const COMPONENT_ASSET_AXES: readonly ComponentAssetAxis[] = [
  "+x",
  "-x",
  "+y",
  "-y",
  "+z",
  "-z"
];

const HARDWARE_ROLE_LABELS: Readonly<Record<OpeningHardwareRole, string>> = {
  handle: "执手/锁具操作件",
  "secondary-lever": "从动扇操作杆",
  "hinge-sash-leaf": "扇侧合页叶",
  "hinge-frame-leaf": "框侧合页叶",
  "lock-point": "锁点",
  keeper: "锁座",
  "shoot-bolt": "天地插销",
  "shoot-bolt-keeper": "天地插销锁座"
};

/** Returns shape choices that preserve one hardware role's business meaning. */
function hardwarePrimitiveChoices(
  role: OpeningHardwareRole
): readonly ParametricComponentGeometrySnapshot["primitiveId"][] {
  switch (role) {
    case "handle":
    case "secondary-lever":
      return ["lever-handle", "round-knob", "box", "custom-parametric"];
    case "hinge-sash-leaf":
    case "hinge-frame-leaf":
      return ["hinge-leaf", "box", "custom-parametric"];
    case "lock-point":
      return ["lock-point", "box", "custom-parametric"];
    case "keeper":
    case "shoot-bolt-keeper":
      return ["keeper", "box", "custom-parametric"];
    case "shoot-bolt":
      return ["shoot-bolt", "box", "custom-parametric"];
  }
}

/**
 * Converts shared editor controls into one validated model mount snapshot.
 *
 * The axis is checked before the visual-configuration normalizer so PC and
 * mobile receive the same localized form error. Pivot values are normalized
 * later with the complete model and must remain within 0..1, where 0 is the
 * minimum edge and 1 the maximum edge of the declared millimetre envelope.
 *
 * @param values Submitted hardware or managed-import form values.
 * @returns Renderer-neutral mount axis and normalized-envelope pivot ratios.
 * @example `+z, 0.5/0.5/0` mounts the centre of a model's back face.
 * @since 0.10.18
 * @modified 2026-09-18 - Exposed model placement in the shared editor.
 */
function readComponentModelMount(values: FormData): ComponentModelMountSnapshot {
  const mountAxis = String(values.get("mountAxis") ?? "");
  if (!COMPONENT_ASSET_AXES.includes(mountAxis as ComponentAssetAxis)) {
    throw new Error("请选择受支持的安装朝向轴。");
  }
  return {
    mountAxis: mountAxis as ComponentAssetAxis,
    pivotRatio: {
      x: Number(values.get("pivotX")),
      y: Number(values.get("pivotY")),
      z: Number(values.get("pivotZ"))
    }
  };
}

/** Creates a traceable editor revision without mutating the catalog version. */
function nextEditorAppearanceVersion(current: string, documentRevision: number): string {
  return `${current.split("+")[0]}+design-r${documentRevision + 1}`;
}

/**
 * Mounts one shared appearance/hardware editor into either interaction shell.
 *
 * Desktop presents the three stages in its property panel; mobile presents the
 * same stages inside a full-screen drawer. Form changes remain local drafts.
 * Only the submit button creates one complete visual-configuration command,
 * guaranteeing one undo step and preventing partial PBR/texture/model states.
 * Manual hardware edits intentionally reset the model to `preview-only`: a new
 * shape or dimension cannot retain an old material/machining approval. The
 * import stage accepts managed IDs/hashes, not URLs or mutable local paths.
 *
 * @param container Shell-owned panel or mobile drawer body.
 * @param session Shared design session and undo history.
 * @param selection Shared stable selection coordinator.
 * @param presentation Layout hint only; no domain behavior depends on it.
 * @param visualPreview Runtime-only draft store consumed by both renderers.
 * @param assetServices Optional hash-pinned upload/availability gateway.
 * @returns Disposer for subscriptions and delegated DOM events.
 * @example Both shells edit `frame.outside` through the same replacement helper.
 * @since 0.10.7
 * @modified 2026-09-18 - Isolated durable upload behind a backend-ready gateway.
 */
export function mountWindowAppearanceEditor(
  container: HTMLElement,
  session: DesignSession,
  selection: DesignSelectionStore,
  presentation: AppearanceEditorPresentation,
  visualPreview: WindowVisualPreviewStore,
  assetServices: AppearanceEditorAssetServices = {}
): () => void {
  container.classList.add("appearance-editor");
  container.dataset.appearanceEditorPresentation = presentation;
  let currentDocument = session.document;
  let selectedObjectId = selection.state.objectId;
  let activeStep: "appearance" | "hardware" | "import" = "appearance";
  let activeSlot: WindowAppearanceEditorSlot = "frame.outside";
  let activeRole: OpeningHardwareRole = "handle";
  let hardwareTargetScope: "role" | "instance" = "role";
  let commandSequence = 0;
  const availableAssetHashes = new Map<string, string>();
  let availabilityRequestKey = "";
  let disposed = false;

  const editingWindow = (): WindowUnit | undefined =>
    selectedOwningWindow(currentDocument, selectedObjectId) ??
    (!selectedObjectId && currentDocument.windows.length === 1
      ? currentDocument.windows[0]
      : undefined);

  /**
   * Resolves the currently selected generated hardware item, when applicable.
   *
   * Geometry remains the source of truth: the editor does not parse role or
   * panel meaning back out of the ID. Selecting ordinary window geometry
   * returns undefined and therefore keeps role-wide editing available.
   *
   * @returns Exact selected mount plus its stable hardware ID, or undefined.
   * @example Selecting a 3D handle exposes a single-instance editing target.
   * @since 0.10.17
   * @modified 2026-09-18 - Connected cross-view hardware selection to editor scope.
   */
  const selectedHardware = () => {
    const window = editingWindow();
    return window
      ? resolveWindowGeometry(window).hardware.find(
          (mount) => mount.hardwareId === selectedObjectId
        )
      : undefined;
  };

  /** Shows a validation problem without discarding the user's local form draft. */
  const showError = (message: string): void => {
    const target = container.querySelector<HTMLElement>("[data-appearance-editor-error]");
    if (target) {
      target.hidden = false;
      target.textContent = message;
    }
  };

  /**
   * Hydrates the synchronous diagnostic view from an asynchronous asset gateway.
   *
   * IDs are deduplicated and queried once per configuration identity. A hash
   * change triggers exactly one re-render; missing records remain visible as
   * diagnostics. This cache is presentation state only and never enters the
   * design/BOM snapshot.
   *
   * @param configuration Current window's immutable visual snapshot.
   * @since 0.10.11
   * @modified 2026-09-18 - Connected persisted upload availability to diagnostics.
   */
  const refreshAssetAvailability = async (
    configuration: WindowVisualConfiguration
  ): Promise<void> => {
    if (!assetServices.gateway) return;
    const ids = [...new Set(configuration.hardwareModels.flatMap(({ model }) =>
      model.geometry.kind === "gltf"
        ? [model.geometry.assetId, ...model.geometry.lodAssetIds]
        : []))].sort();
    const requestKey = ids.join("|");
    if (requestKey === availabilityRequestKey) return;
    availabilityRequestKey = requestKey;
    const descriptors = await Promise.all(ids.map((assetId) =>
      assetServices.gateway!.describe(assetId)));
    if (disposed) return;
    let changed = false;
    ids.forEach((assetId, index) => {
      const contentHash = descriptors[index]?.contentHash;
      if (contentHash && availableAssetHashes.get(assetId) !== contentHash) {
        availableAssetHashes.set(assetId, contentHash);
        changed = true;
      }
    });
    if (changed) render();
  };

  /** Rebuilds forms from the latest immutable document and selected window. */
  const render = (): void => {
    const isFactoryWorkbench = presentation === "factory-workbench";
    const window = editingWindow();
    if (!window) {
      const empty = document.createElement("p");
      empty.className = "appearance-editor__empty";
      empty.textContent = "请选择窗体或窗体构件后编辑材质与五金型号。";
      container.replaceChildren(empty);
      return;
    }
    const configuration = window.visualConfiguration;
    if (!configuration) {
      const empty = document.createElement("p");
      empty.className = "appearance-editor__empty";
      empty.textContent = "当前窗体尚未生成外观快照。";
      container.replaceChildren(empty);
      return;
    }
    let historyEntries: readonly AppearanceEditorVisualHistoryEntry[] = [];
    let historyError = "";
    try {
      historyEntries = assetServices.history?.list(
        currentDocument.designId,
        window.objectId
      ) ?? [];
    } catch (error) {
      historyError = error instanceof Error ? error.message : "本地版本读取失败。";
    }
    const appearance = resolveWindowAppearanceEditorSlot(configuration, activeSlot);
    const selectedMount = selectedHardware();
    const targetHardwareId = hardwareTargetScope === "instance" &&
      selectedMount?.role === activeRole
      ? selectedMount.hardwareId
      : undefined;
    const hasExactHardwareOverride = targetHardwareId !== undefined &&
      configuration.hardwareModels.some((assignment) =>
        assignment.role === activeRole && assignment.hardwareId === targetHardwareId);
    const model = resolveHardwareComponentModel(configuration, activeRole, targetHardwareId);
    if (!model) throw new Error(`No role-level model resolves for ${activeRole}.`);
    const appearancePresets = listAppearanceCatalogPresets(activeSlot);
    const hardwarePresets = listHardwareModelCatalogPresets(activeRole);
    const selectedAppearancePresetId = appearancePresets.find((preset) =>
      preset.appearance.appearanceId === appearance.appearanceId &&
      preset.appearance.appearanceVersion === appearance.appearanceVersion
    )?.presetId;
    const selectedHardwarePresetId = hardwarePresets.find((preset) =>
      preset.model.modelId === model.modelId &&
      preset.model.modelVersion === model.modelVersion
    )?.presetId;
    const surroundPresets = listSurroundBusinessCatalogPresets(window.profileSystemId);
    const selectedSurroundCatalogValue = window.installationSurroundSelection
      ? `${window.installationSurroundSelection.catalogItemId}@${window.installationSurroundSelection.catalogVersion}`
      : "";
    const diagnostics = diagnoseWindowVisualAssets(configuration, {
      resolveTextureSet: (assetId) => {
        const contentHash = availableAssetHashes.get(assetId);
        return contentHash ? { contentHash } : undefined;
      },
      resolveComponentModel: (assetId) => {
        const contentHash = availableAssetHashes.get(assetId);
        return contentHash ? { contentHash } : undefined;
      }
    });
    const root = document.createElement("section");
    root.className = "appearance-editor__content";
    if (isFactoryWorkbench) root.classList.add("appearance-editor__content--factory");
    root.innerHTML = `
      <header class="appearance-editor__header">
        <strong data-editor-window-name></strong>
        <span data-editor-window-id${isFactoryWorkbench ? " hidden" : ""}></span>
      </header>
      <nav class="appearance-editor__steps" aria-label="材质与型号编辑步骤">
        <button type="button" data-editor-step="appearance" aria-pressed="${activeStep === "appearance"}">${isFactoryWorkbench ? "材质 / 玻璃" : "1 材质外观"}</button>
        <button type="button" data-editor-step="hardware" aria-pressed="${activeStep === "hardware"}">${isFactoryWorkbench ? "五金型号" : "2 五金型号"}</button>
        ${isFactoryWorkbench ? "" : `<button type="button" data-editor-step="import" aria-pressed="${activeStep === "import"}">3 模型导入</button>`}
      </nav>
      ${isFactoryWorkbench
        ? `<p class="appearance-editor__factory-guidance">在此选择设计使用的材质、玻璃和五金型号；选择结果会同步预览2D/3D。渲染参数和模型资产维护不属于工厂设计操作。</p>`
        : `<p class="appearance-editor__hint">当前为迁移期高级目录/渲染参数工具。正式设计选型将只显示业务材质、玻璃和五金型号；PBR、UV、哈希、模型轴向及生产映射由独立目录维护功能管理。</p>`}
      ${activeSlot === "surroundOutside" || activeSlot === "surroundInside" || activeSlot === "surroundLiner" ? `
      <section class="appearance-editor__history" data-surround-business-selector>
        <strong>安装包边/衬板型号（不修改墙体）</strong>
        <select data-surround-catalog-selection aria-label="选择包边和衬板材料型号">
          <option value="">请选择业务材料型号</option>
          ${surroundPresets.map(({ selection }) =>
            `<option value="${selection.catalogItemId}@${selection.catalogVersion}"${`${selection.catalogItemId}@${selection.catalogVersion}` === selectedSurroundCatalogValue ? " selected" : ""}>${selection.businessName} · ${selection.specification} · ${selection.catalogItemId}</option>`
          ).join("")}
        </select>
        <span>选择后立即启用并更新2D、3D、包边、衬板、转角件和密封材料；宽度与安装边仍按当前绘图参数。</span>
      </section>
      ` : ""}
      <div class="appearance-editor__preview-status" role="status"${isFactoryWorkbench ? " hidden" : ""}>
        <span data-visual-preview-status>修改有效参数时同步预览2D/3D；保存前不写入设计、BOM或撤销历史。</span>
        <button type="button" data-clear-visual-preview>还原草稿</button>
      </div>${isFactoryWorkbench ? "" : `
      <section class="appearance-editor__history" data-visual-history>
        <strong>本地历史版本</strong>
        <select data-visual-history-entry aria-label="选择本地材质与型号版本"></select>
        <div>
          <button type="button" data-preview-visual-history>预览版本</button>
          <button type="button" data-restore-visual-history>恢复为当前版本</button>
        </div>
        <span data-visual-history-status></span>
      </section>`}
      ${isFactoryWorkbench ? `
      <form class="appearance-editor__form appearance-editor__factory-form" data-factory-appearance-form${activeStep === "appearance" ? "" : " hidden"}>
        <label>应用部位<select name="slot">${WINDOW_APPEARANCE_EDITOR_SLOTS.map((slot) =>
          `<option value="${slot}"${slot === activeSlot ? " selected" : ""}>${APPEARANCE_SLOT_LABELS[slot]}</option>`).join("")}</select></label>
        <label>业务材质 / 玻璃型号<select name="appearancePresetId">
          <option value="">当前选型未关联目录型号</option>
          ${appearancePresets.map((preset) =>
            `<option value="${preset.presetId}"${preset.presetId === selectedAppearancePresetId ? " selected" : ""}>${preset.label}</option>`).join("")}
        </select></label>
        <p class="appearance-editor__factory-guidance">${selectedAppearancePresetId
          ? `当前目录选型：${appearancePresets.find((preset) => preset.presetId === selectedAppearancePresetId)?.label ?? "已关联"}`
          : "当前外观尚未关联到目录型号；可继续设计预览，不能据此认定为正式物料。"}</p>
      </form>
      <form class="appearance-editor__form appearance-editor__factory-form" data-factory-hardware-form${activeStep === "hardware" ? "" : " hidden"}>
        <label>五金类别<select name="role">${HARDWARE_EDITOR_ROLES.map((role) =>
          `<option value="${role}"${role === activeRole ? " selected" : ""}>${HARDWARE_ROLE_LABELS[role]}</option>`).join("")}</select></label>
        <label>应用范围<select name="hardwareTargetScope">
          <option value="role"${targetHardwareId ? "" : " selected"}>该类别全部构件</option>
          ${selectedMount?.role === activeRole
            ? `<option value="instance"${targetHardwareId ? " selected" : ""}>仅当前五金构件</option>`
            : ""}
        </select></label>
        <label>五金业务型号<select name="hardwarePresetId">
          <option value="">当前型号未关联目录</option>
          ${hardwarePresets.map((preset) =>
            `<option value="${preset.presetId}"${preset.presetId === selectedHardwarePresetId ? " selected" : ""}>${preset.label} · ${preset.model.materialCode ?? "仅预览"}</option>`).join("")}
        </select></label>
        <p class="appearance-editor__factory-guidance">选择目录型号后会更新设计预览；仅预览或待审核选型不代表可下单生产。</p>
      </form>` : ""}
      <form class="appearance-editor__form" data-appearance-form${isFactoryWorkbench || activeStep !== "appearance" ? " hidden" : ""}>
        <label>编辑对象<select name="slot">${WINDOW_APPEARANCE_EDITOR_SLOTS.map((slot) =>
          `<option value="${slot}">${APPEARANCE_SLOT_LABELS[slot]}</option>`).join("")}</select></label>
        <label>受控目录预设<select name="appearancePresetId">
          <option value="">自定义草稿</option>
          ${appearancePresets.map((preset) =>
            `<option value="${preset.presetId}"${preset.presetId === selectedAppearancePresetId ? " selected" : ""}>${preset.label}</option>`).join("")}
        </select></label>
        <div class="appearance-editor__span">
          <label>外观ID<input name="appearanceId" type="text" required /></label>
          <label>材质族<select name="materialFamily">${APPEARANCE_MATERIAL_FAMILIES.map((family) =>
            `<option value="${family}">${family}</option>`).join("")}</select></label>
        </div>
        <label>颜色 / RAL<input name="baseColor" type="text" required /></label>
        <div class="appearance-editor__span appearance-editor__span--three">
          <label>金属度<input name="metalness" type="number" min="0" max="1" step="0.01" required /></label>
          <label>粗糙度<input name="roughness" type="number" min="0" max="1" step="0.01" required /></label>
          <label>透明度<input name="opacity" type="number" min="0" max="1" step="0.01" required /></label>
        </div>
        <label>纹理集ID<input name="textureSetId" type="text" placeholder="留空表示纯色" /></label>
        <label>纹理SHA-256<input name="textureContentHash" type="text" placeholder="sha256:…" /></label>
        <div class="appearance-editor__span">
          <label>纹理横向重复<input name="uvX" type="number" min="${APPEARANCE_UV_INPUT_CONSTRAINTS.min}" max="${APPEARANCE_UV_INPUT_CONSTRAINTS.max}" step="${APPEARANCE_UV_INPUT_CONSTRAINTS.step}" /></label>
          <label>纹理纵向重复<input name="uvY" type="number" min="${APPEARANCE_UV_INPUT_CONSTRAINTS.min}" max="${APPEARANCE_UV_INPUT_CONSTRAINTS.max}" step="${APPEARANCE_UV_INPUT_CONSTRAINTS.step}" /></label>
        </div>
        <p class="appearance-editor__hint">UV重复值会独立保存；当前未绑定纹理时不改变画面，绑定纹理后按保存倍率生效。</p>
        <label>显示用表面处理编码<input name="finishCode" type="text" /></label>
        <p class="appearance-editor__hint" data-surface-production-status></p>
        <button class="shell-button" type="submit">应用材质外观</button>
      </form>
      <form class="appearance-editor__form" data-hardware-model-form${isFactoryWorkbench || activeStep !== "hardware" ? " hidden" : ""}>
        <label>五金构件类型<select name="role">${HARDWARE_EDITOR_ROLES.map((role) =>
          `<option value="${role}">${HARDWARE_ROLE_LABELS[role]}</option>`).join("")}</select></label>
        <label>应用范围<select name="hardwareTargetScope">
          <option value="role">该类型全部构件（默认型号）</option>
          ${selectedMount?.role === activeRole
            ? '<option value="instance">仅当前构件</option>'
            : ""}
        </select></label>
        <label>已审核目录型号<select name="hardwarePresetId">
          <option value="">手工草稿（仅预览）</option>
          ${hardwarePresets.map((preset) =>
            `<option value="${preset.presetId}"${preset.presetId === selectedHardwarePresetId ? " selected" : ""}>${preset.label} · ${preset.model.materialCode ?? "仅预览"}</option>`).join("")}
        </select></label>
        <div class="appearance-editor__span">
          <label>型号ID<input name="modelId" type="text" required /></label>
          <label>型号版本<input name="modelVersion" type="text" required /></label>
        </div>
        <label>预览形状<select name="primitiveId"></select></label>
        <div class="appearance-editor__span appearance-editor__span--three">
          <label>宽mm<input name="widthMm" type="number" min="0.1" max="5000" step="0.1" required /></label>
          <label>高mm<input name="heightMm" type="number" min="0.1" max="5000" step="0.1" required /></label>
          <label>深mm<input name="depthMm" type="number" min="0.1" max="5000" step="0.1" required /></label>
        </div>
        <label>安装朝向轴<select name="mountAxis">${COMPONENT_ASSET_AXES.map((axis) =>
          `<option value="${axis}">${axis}</option>`).join("")}</select></label>
        <div class="appearance-editor__span appearance-editor__span--three">
          <label>枢轴X比例<input name="pivotX" type="number" min="0" max="1" step="0.01" required /></label>
          <label>枢轴Y比例<input name="pivotY" type="number" min="0" max="1" step="0.01" required /></label>
          <label>枢轴Z比例<input name="pivotZ" type="number" min="0" max="1" step="0.01" required /></label>
        </div>
        <label>型号颜色<input name="modelColor" type="text" required /></label>
        <p class="appearance-editor__hint">枢轴比例0/1表示模型包络的最小/最大边；安装朝向轴决定模型贴向构件的方向。手工修改后先保存为“仅预览”，物资编码和加工模板须重新确认。</p>
        ${hasExactHardwareOverride
          ? '<button class="shell-button shell-button--secondary" type="button" data-reset-hardware-instance>恢复该类型默认型号</button>'
          : ""}
        <button class="shell-button" type="submit">应用五金型号</button>
      </form>
      <form class="appearance-editor__form" data-managed-model-import-form${isFactoryWorkbench || activeStep !== "import" ? " hidden" : ""}>
        <p class="appearance-editor__hint">此处只接收资产服务返回的受控ID与SHA-256，不接收网址。导入结果为“仅预览”，审核发布后才能取得物资编码和加工模板。</p>
        <label>五金构件类型<select name="role">${HARDWARE_EDITOR_ROLES.map((role) =>
          `<option value="${role}">${HARDWARE_ROLE_LABELS[role]}</option>`).join("")}</select></label>
        <label>应用范围<select name="hardwareTargetScope">
          <option value="role">该类型全部构件（默认型号）</option>
          ${selectedMount?.role === activeRole
            ? '<option value="instance">仅当前构件</option>'
            : ""}
        </select></label>
        <div class="appearance-editor__span">
          <label>型号ID<input name="modelId" type="text" value="customer.model.preview" required /></label>
          <label>型号版本<input name="modelVersion" type="text" value="0.1.0" required /></label>
        </div>
        <label>高清资产ID<input name="primaryAssetId" type="text" placeholder="ASSET-HANDLE-42-HIGH" required /></label>
        <label>高清资产SHA-256<input name="primaryContentHash" type="text" placeholder="sha256:…" required /></label>
        <label>选择高清GLB/glTF<input name="primaryFile" type="file" accept=".glb,.gltf,model/gltf-binary,model/gltf+json"${assetServices.gateway ? "" : " disabled"} /></label>
        <div class="appearance-editor__span">
          <label>中清资产ID<input name="mediumAssetId" type="text" /></label>
          <label>中清SHA-256<input name="mediumContentHash" type="text" placeholder="可选，须与ID同时填写" /></label>
        </div>
        <label>选择中清GLB/glTF<input name="mediumFile" type="file" accept=".glb,.gltf,model/gltf-binary,model/gltf+json"${assetServices.gateway ? "" : " disabled"} /></label>
        <div class="appearance-editor__span">
          <label>低清资产ID<input name="lowAssetId" type="text" /></label>
          <label>低清SHA-256<input name="lowContentHash" type="text" placeholder="可选，须与ID同时填写" /></label>
        </div>
        <label>选择低清GLB/glTF<input name="lowFile" type="file" accept=".glb,.gltf,model/gltf-binary,model/gltf+json"${assetServices.gateway ? "" : " disabled"} /></label>
        <div class="appearance-editor__span appearance-editor__span--three">
          <label>源单位<select name="sourceUnit">${COMPONENT_ASSET_SOURCE_UNITS.map((unit) =>
            `<option value="${unit}">${unit}</option>`).join("")}</select></label>
          <label>向上轴<select name="upAxis">${COMPONENT_ASSET_AXES.map((axis) =>
            `<option value="${axis}">${axis}</option>`).join("")}</select></label>
          <label>朝外轴<select name="forwardAxis">${COMPONENT_ASSET_AXES.map((axis) =>
            `<option value="${axis}">${axis}</option>`).join("")}</select></label>
        </div>
        <div class="appearance-editor__span appearance-editor__span--three">
          <label>宽mm<input name="widthMm" type="number" min="0.1" max="5000" step="0.1" required /></label>
          <label>高mm<input name="heightMm" type="number" min="0.1" max="5000" step="0.1" required /></label>
          <label>深mm<input name="depthMm" type="number" min="0.1" max="5000" step="0.1" required /></label>
        </div>
        <label>安装朝向轴<select name="mountAxis">${COMPONENT_ASSET_AXES.map((axis) =>
          `<option value="${axis}">${axis}</option>`).join("")}</select></label>
        <div class="appearance-editor__span appearance-editor__span--three">
          <label>枢轴X比例<input name="pivotX" type="number" min="0" max="1" step="0.01" required /></label>
          <label>枢轴Y比例<input name="pivotY" type="number" min="0" max="1" step="0.01" required /></label>
          <label>枢轴Z比例<input name="pivotZ" type="number" min="0" max="1" step="0.01" required /></label>
        </div>
        <label>预览颜色<input name="modelColor" type="text" required /></label>
        <section class="appearance-editor__upload-status" aria-live="polite">
          <strong>文件预检与保存</strong>
          <progress data-managed-upload-progress max="1" value="0"></progress>
          <span data-managed-upload-status>${assetServices.gateway
            ? "选择文件后自动计算哈希；提交时执行安全预检并写入本地持久存储。"
            : "当前环境未配置资产存储，只能填写已有受控资产ID与哈希。"}</span>
          <ul data-managed-upload-evidence></ul>
        </section>
        ${hasExactHardwareOverride
          ? '<button class="shell-button shell-button--secondary" type="button" data-reset-hardware-instance>恢复该类型默认型号</button>'
          : ""}
        <button class="shell-button" type="submit">预检、保存并导入</button>
      </form>
      <section class="appearance-editor__diagnostics" aria-live="polite"${isFactoryWorkbench ? " hidden" : ""}>
        <strong>资产诊断</strong>
        <ul data-asset-diagnostic-list></ul>
      </section>
      <p class="appearance-editor__error" data-appearance-editor-error hidden></p>`;
    const windowName = root.querySelector<HTMLElement>("[data-editor-window-name]");
    const windowId = root.querySelector<HTMLElement>("[data-editor-window-id]");
    if (windowName) windowName.textContent = `${window.mark} · 外观与型号`;
    if (windowId) windowId.textContent = window.objectId;
    const surroundBusinessSelect = root.querySelector<HTMLSelectElement>(
      "[data-surround-catalog-selection]"
    );
    if (surroundBusinessSelect && window.installationSurroundSelection) {
      surroundBusinessSelect.value =
        `${window.installationSurroundSelection.catalogItemId}@` +
        window.installationSurroundSelection.catalogVersion;
    }
    root.querySelectorAll<HTMLOptionElement>(
      'select[name="hardwareTargetScope"] option[value="instance"]'
    ).forEach((option) => {
      option.textContent = `仅当前构件 · ${selectedMount?.hardwareId ?? ""}`;
    });
    const historySelect = root.querySelector<HTMLSelectElement>("[data-visual-history-entry]");
    const historyStatus = root.querySelector<HTMLElement>("[data-visual-history-status]");
    const historyButtons = root.querySelectorAll<HTMLButtonElement>(
      "[data-preview-visual-history], [data-restore-visual-history]"
    );
    if (historySelect) {
      if (historyEntries.length === 0) {
        historySelect.append(new Option("尚无本地版本", ""));
      } else {
        historyEntries.forEach((entry, index) => {
          const captured = new Date(entry.capturedAtIso).toLocaleString("zh-CN", {
            month: "2-digit",
            day: "2-digit",
            hour: "2-digit",
            minute: "2-digit",
            second: "2-digit",
            hour12: false
          });
          const frameColor = entry.configuration.appearance.frame.outside.baseColor;
          const exactCount = entry.configuration.hardwareModels.filter(
            (assignment) => assignment.hardwareId !== undefined
          ).length;
          const label = `${index === 0 ? "最新 · " : ""}r${entry.sourceRevision} · ${captured} · 框外${frameColor} · 单件${exactCount}`;
          historySelect.append(new Option(label, entry.entryId));
        });
      }
      historySelect.disabled = historyEntries.length === 0 || Boolean(historyError);
    }
    historyButtons.forEach((button) => {
      button.disabled = historyEntries.length === 0 || Boolean(historyError);
    });
    if (historyStatus) {
      historyStatus.textContent = historyError ||
        (historyEntries.length > 0
          ? `保留最近${historyEntries.length}个去重快照；预览不写入设计。`
          : "首次正式外观快照将在本地记录。 ");
      if (historyError) historyStatus.dataset.state = "error";
    }

    const appearanceForm = root.querySelector<HTMLFormElement>("[data-appearance-form]")!;
    const setAppearanceValue = (name: string, value: string): void => {
      const control = appearanceForm.elements.namedItem(name);
      if (control instanceof HTMLInputElement || control instanceof HTMLSelectElement) {
        control.value = value;
      }
    };
    setAppearanceValue("slot", activeSlot);
    setAppearanceValue("appearanceId", appearance.appearanceId);
    setAppearanceValue("materialFamily", appearance.materialFamily);
    setAppearanceValue("baseColor", appearance.baseColor);
    setAppearanceValue("metalness", String(appearance.metalness));
    setAppearanceValue("roughness", String(appearance.roughness));
    setAppearanceValue("opacity", String(appearance.opacity));
    setAppearanceValue("textureSetId", appearance.textureSetId ?? "");
    setAppearanceValue("textureContentHash", appearance.textureContentHash ?? "");
    setAppearanceValue("uvX", String(appearance.uvScale?.x ?? 1));
    setAppearanceValue("uvY", String(appearance.uvScale?.y ?? 1));
    setAppearanceValue("finishCode", appearance.finishCode ?? "");
    const surfaceProductionStatus = root.querySelector<HTMLElement>(
      "[data-surface-production-status]"
    );
    if (surfaceProductionStatus) {
      const mapping = appearance.productionMapping;
      surfaceProductionStatus.textContent = mapping?.productionStatus === "catalog-approved"
        ? `生产映射：${mapping.treatmentCode} · 工艺模板 ${mapping.processTemplateId}`
        : "当前为展示外观；手工颜色、纹理和显示编码不会改变BOM或工艺。";
    }
    setAppearanceValue(
      "appearancePresetId",
      appearancePresets.find((preset) =>
        preset.appearance.appearanceId === appearance.appearanceId &&
        preset.appearance.appearanceVersion === appearance.appearanceVersion
      )?.presetId ?? ""
    );

    const hardwareForm = root.querySelector<HTMLFormElement>("[data-hardware-model-form]")!;
    const setHardwareValue = (name: string, value: string): void => {
      const control = hardwareForm.elements.namedItem(name);
      if (control instanceof HTMLInputElement || control instanceof HTMLSelectElement) {
        control.value = value;
      }
    };
    setHardwareValue("role", activeRole);
    setHardwareValue("hardwareTargetScope", targetHardwareId ? "instance" : "role");
    setHardwareValue("modelId", model.modelId);
    setHardwareValue("modelVersion", model.modelVersion);
    setHardwareValue("widthMm", String(model.dimensionsMm.widthMm));
    setHardwareValue("heightMm", String(model.dimensionsMm.heightMm));
    setHardwareValue("depthMm", String(model.dimensionsMm.depthMm));
    setHardwareValue("mountAxis", model.mount.mountAxis);
    setHardwareValue("pivotX", String(model.mount.pivotRatio.x));
    setHardwareValue("pivotY", String(model.mount.pivotRatio.y));
    setHardwareValue("pivotZ", String(model.mount.pivotRatio.z));
    setHardwareValue("modelColor", model.appearance.baseColor);
    setHardwareValue(
      "hardwarePresetId",
      hardwarePresets.find((preset) =>
        preset.model.modelId === model.modelId && preset.model.modelVersion === model.modelVersion
      )?.presetId ?? ""
    );
    const primitive = hardwareForm.elements.namedItem("primitiveId");
    if (primitive instanceof HTMLSelectElement) {
      const choices = hardwarePrimitiveChoices(activeRole);
      primitive.replaceChildren(...[
        ...(model.geometry.kind === "gltf"
          ? [{ value: "keep-current", label: `保持受控模型 ${model.geometry.assetId}` }]
          : []),
        ...choices.map((value) => ({ value, label: value }))
      ].map(({ value, label }) => {
        const option = document.createElement("option");
        option.value = value;
        option.textContent = label;
        return option;
      }));
      primitive.value = model.geometry.kind === "gltf"
        ? "keep-current"
        : choices.includes(model.geometry.primitiveId)
          ? model.geometry.primitiveId
          : choices[0]!;
    }

    const importForm = root.querySelector<HTMLFormElement>("[data-managed-model-import-form]")!;
    const setImportValue = (name: string, value: string): void => {
      const control = importForm.elements.namedItem(name);
      if (control instanceof HTMLInputElement || control instanceof HTMLSelectElement) {
        control.value = value;
      }
    };
    setImportValue("role", activeRole);
    setImportValue("hardwareTargetScope", targetHardwareId ? "instance" : "role");
    setImportValue("sourceUnit", "millimeter");
    setImportValue("upAxis", "+z");
    setImportValue("forwardAxis", "-y");
    setImportValue("widthMm", String(model.dimensionsMm.widthMm));
    setImportValue("heightMm", String(model.dimensionsMm.heightMm));
    setImportValue("depthMm", String(model.dimensionsMm.depthMm));
    setImportValue("mountAxis", model.mount.mountAxis);
    setImportValue("pivotX", String(model.mount.pivotRatio.x));
    setImportValue("pivotY", String(model.mount.pivotRatio.y));
    setImportValue("pivotZ", String(model.mount.pivotRatio.z));
    setImportValue("modelColor", model.appearance.baseColor);

    const diagnosticList = root.querySelector<HTMLUListElement>("[data-asset-diagnostic-list]");
    if (diagnosticList) {
      if (diagnostics.length === 0) {
        const item = document.createElement("li");
        item.textContent = "当前快照没有外部资产问题。";
        diagnosticList.append(item);
      } else {
        diagnostics.forEach((diagnostic) => {
          const item = document.createElement("li");
          item.dataset.severity = diagnostic.severity;
          item.textContent = `${diagnostic.path}：${diagnostic.message}`;
          diagnosticList.append(item);
        });
      }
    }
    container.replaceChildren(root);
    void refreshAssetAvailability(configuration).catch((error: unknown) => {
      if (!disposed) showError(error instanceof Error ? error.message : "资产可用性查询失败。");
    });
  };

  /** Switches editor step/slot/role without writing domain state. */
  const onClick = (event: Event): void => {
    const target = event.target;
    if (!(target instanceof Element)) return;
    const previewHistory = target.closest<HTMLButtonElement>("[data-preview-visual-history]");
    const restoreHistory = target.closest<HTMLButtonElement>("[data-restore-visual-history]");
    if (previewHistory || restoreHistory) {
      const window = editingWindow();
      const select = container.querySelector<HTMLSelectElement>("[data-visual-history-entry]");
      if (!window || !select?.value || !assetServices.history) return;
      try {
        const entry = assetServices.history.list(
          currentDocument.designId,
          window.objectId
        ).find((candidate) => candidate.entryId === select.value);
        if (!entry) throw new Error("选择的本地版本已不存在，请重新打开版本列表。");
        if (previewHistory) {
          visualPreview.set(window.objectId, entry.configuration);
          const status = container.querySelector<HTMLElement>("[data-visual-preview-status]");
          if (status) {
            status.textContent = `正在预览历史 r${entry.sourceRevision}；尚未修改正式设计和BOM。`;
          }
        } else {
          visualPreview.clear(window.objectId);
          commandSequence += 1;
          session.execute(createUpdateWindowVisualConfigurationCommand({
            commandId: `CMD-VISUAL-HISTORY-${session.document.revision}-${commandSequence}`,
            windowId: window.objectId,
            visualConfiguration: entry.configuration
          }));
        }
      } catch (error) {
        showError(error instanceof Error ? error.message : "本地版本操作失败。 ");
      }
      return;
    }
    const clearPreview = target.closest<HTMLButtonElement>("[data-clear-visual-preview]");
    if (clearPreview) {
      const window = editingWindow();
      if (window) visualPreview.clear(window.objectId);
      render();
      return;
    }
    const resetInstance = target.closest<HTMLButtonElement>("[data-reset-hardware-instance]");
    if (resetInstance) {
      const window = editingWindow();
      const configuration = window?.visualConfiguration;
      const mount = selectedHardware();
      if (!window || !configuration || !mount || mount.role !== activeRole) return;
      commandSequence += 1;
      session.execute(createUpdateWindowVisualConfigurationCommand({
        commandId: `CMD-VISUAL-EDITOR-${session.document.revision}-${commandSequence}`,
        windowId: window.objectId,
        visualConfiguration: removeWindowHardwareInstanceModel(
          configuration,
          mount.role,
          mount.hardwareId
        )
      }));
      return;
    }
    const button = target.closest<HTMLButtonElement>("[data-editor-step]");
    if (!button) return;
    const window = editingWindow();
    if (window) visualPreview.clear(window.objectId);
    activeStep = button.dataset.editorStep === "hardware"
      ? "hardware"
      : button.dataset.editorStep === "import"
        ? "import"
        : "appearance";
    render();
  };

  const managedFileFields = {
    primaryFile: { assetId: "primaryAssetId", hash: "primaryContentHash", quality: "高清" },
    mediumFile: { assetId: "mediumAssetId", hash: "mediumContentHash", quality: "中清" },
    lowFile: { assetId: "lowAssetId", hash: "lowContentHash", quality: "低清" }
  } as const;

  /** Returns a named text input from one import form or fails visibly. */
  const importTextInput = (form: HTMLFormElement, name: string): HTMLInputElement => {
    const control = form.elements.namedItem(name);
    if (!(control instanceof HTMLInputElement)) {
      throw new Error(`模型导入表单缺少 ${name} 字段。`);
    }
    return control;
  };

  /**
   * Hashes a newly selected local file and stages its managed identity fields.
   *
   * This does not persist bytes yet. Submit subsequently runs the authoritative
   * security preflight and gateway write, keeping file selection reversible and
   * ensuring one design command is emitted only after every selected LOD passes.
   *
   * @param input Changed primary/medium/low file control.
   * @example `handle.glb` receives a deterministic ID containing its hash prefix.
   * @since 0.10.11
   * @modified 2026-09-18 - Added shared PC/mobile file staging.
   */
  const stageManagedFile = async (input: HTMLInputElement): Promise<void> => {
    const mapping = managedFileFields[input.name as keyof typeof managedFileFields];
    const form = input.closest<HTMLFormElement>("[data-managed-model-import-form]");
    const file = input.files?.[0];
    if (!mapping || !form || !file) return;
    const requestId = `${performance.now()}-${file.name}-${file.size}`;
    input.dataset.stagingRequestId = requestId;
    const status = form.querySelector<HTMLElement>("[data-managed-upload-status]");
    const progress = form.querySelector<HTMLProgressElement>("[data-managed-upload-progress]");
    const submitButton = form.querySelector<HTMLButtonElement>('button[type="submit"]');
    if (submitButton) submitButton.disabled = true;
    if (status) status.textContent = `正在读取并计算${mapping.quality}文件哈希：${file.name}`;
    progress?.removeAttribute("value");
    try {
      const contentHash = await calculateVisualAssetContentHash(await file.arrayBuffer());
      if (
        disposed ||
        !input.isConnected ||
        input.dataset.stagingRequestId !== requestId ||
        input.files?.[0] !== file
      ) return;
      const assetId = importTextInput(form, mapping.assetId);
      const hashInput = importTextInput(form, mapping.hash);
      const baseName = file.name.replace(/\.[^.]+$/u, "")
        .replace(/[^a-zA-Z0-9_-]+/gu, "-")
        .replace(/^-+|-+$/gu, "")
        .slice(0, 48) || "MODEL";
      const generatedAssetId = `ASSET-${baseName}-${contentHash.slice(7, 19)}`.toUpperCase();
      if (
        !assetId.value.trim() ||
        assetId.value === assetId.dataset.autoGeneratedAssetId
      ) {
        assetId.value = generatedAssetId;
        assetId.dataset.autoGeneratedAssetId = generatedAssetId;
      }
      hashInput.value = contentHash;
      if (progress) {
        progress.max = 1;
        progress.value = 0;
      }
      if (status) status.textContent = `${mapping.quality}文件已暂存；提交后执行格式、外链和复杂度预检。`;
    } catch (error) {
      if (progress) {
        progress.max = 1;
        progress.value = 0;
      }
      showError(error instanceof Error ? error.message : "无法读取所选模型文件。 ");
    } finally {
      if (input.dataset.stagingRequestId === requestId) {
        delete input.dataset.stagingRequestId;
      }
      if (submitButton && !form.querySelector("[data-staging-request-id]")) {
        submitButton.disabled = false;
      }
    }
  };

  /**
   * Preflights and stores every selected LOD before a model snapshot is committed.
   *
   * @param form Active shared import form.
   * @returns Hash-covered evidence in high/medium/low field order.
   * @throws If storage is absent or any selected file fails the common gate.
   * @since 0.10.11
   * @modified 2026-09-18 - Added durable upload orchestration and progress evidence.
   */
  const uploadManagedFiles = async (
    form: HTMLFormElement
  ): Promise<readonly ManagedGltfUploadEvidence[]> => {
    const selected = Object.entries(managedFileFields).flatMap(([fileField, mapping]) => {
      const control = form.elements.namedItem(fileField);
      const file = control instanceof HTMLInputElement ? control.files?.[0] : undefined;
      return file ? [{ file, mapping }] : [];
    });
    if (selected.length === 0) return [];
    if (!assetServices.gateway) {
      throw new Error("当前环境未配置模型上传服务，不能上传本地模型文件。");
    }
    const status = form.querySelector<HTMLElement>("[data-managed-upload-status]");
    const progress = form.querySelector<HTMLProgressElement>("[data-managed-upload-progress]");
    const evidenceList = form.querySelector<HTMLUListElement>("[data-managed-upload-evidence]");
    evidenceList?.replaceChildren();
    if (progress) {
      progress.max = selected.length;
      progress.value = 0;
    }
    const evidence: ManagedGltfUploadEvidence[] = [];
    for (const [index, item] of selected.entries()) {
      if (status) {
        status.textContent = `正在预检并保存${item.mapping.quality}文件 ${index + 1}/${selected.length}：${item.file.name}`;
      }
      const result = await assetServices.gateway.upload({
        assetId: importTextInput(form, item.mapping.assetId).value.trim(),
        expectedContentHash: importTextInput(form, item.mapping.hash).value.trim(),
        mediaType: item.file.name.toLowerCase().endsWith(".gltf")
          ? "model/gltf+json"
          : "model/gltf-binary",
        bytes: await item.file.arrayBuffer(),
        inspectedAtIso: (assetServices.now?.() ?? new Date()).toISOString()
      });
      evidence.push(result);
      availableAssetHashes.set(result.asset.assetId, result.asset.contentHash);
      if (progress) progress.value = index + 1;
      if (evidenceList) {
        const row = document.createElement("li");
        row.textContent = `${item.mapping.quality}：${result.inspection.byteLength}B，${result.inspection.estimatedTriangleCount}三角面，${result.inspection.nodeCount}节点`;
        evidenceList.append(row);
      }
    }
    if (status) status.textContent = `${selected.length}个文件预检与本地持久保存完成。`;
    return evidence;
  };

  /** Builds one validated appearance draft from the complete visible form. */
  const appearanceDraftFromForm = (
    form: HTMLFormElement,
    configuration: WindowVisualConfiguration
  ): WindowVisualConfiguration => {
    const values = new FormData(form);
    const presetId = String(values.get("appearancePresetId") ?? "").trim();
    if (presetId) {
      return applyAppearanceCatalogPreset(configuration, activeSlot, presetId);
    }
    const current = resolveWindowAppearanceEditorSlot(configuration, activeSlot);
    const textureSetId = String(values.get("textureSetId") ?? "").trim();
    const textureContentHash = String(values.get("textureContentHash") ?? "").trim();
    const finishCode = String(values.get("finishCode") ?? "").trim();
    return replaceWindowAppearanceEditorSlot(configuration, activeSlot, {
      appearanceId: String(values.get("appearanceId") ?? "").trim(),
      appearanceVersion: nextEditorAppearanceVersion(
        current.appearanceVersion,
        currentDocument.revision
      ),
      materialFamily: APPEARANCE_MATERIAL_FAMILIES.includes(
        values.get("materialFamily") as AppearanceMaterialFamily
      ) ? values.get("materialFamily") as AppearanceMaterialFamily : "custom",
      baseColor: String(values.get("baseColor") ?? "").trim(),
      metalness: Number(values.get("metalness")),
      roughness: Number(values.get("roughness")),
      opacity: Number(values.get("opacity")),
      ...(textureSetId ? { textureSetId } : {}),
      ...(textureContentHash ? { textureContentHash } : {}),
      uvScale: readAppearanceEditorUvScale(values.get("uvX"), values.get("uvY")),
      ...(finishCode ? { finishCode } : {})
    });
  };

  /** Builds one validated role/instance hardware draft from the visible form. */
  const hardwareDraftFromForm = (
    form: HTMLFormElement,
    configuration: WindowVisualConfiguration
  ): WindowVisualConfiguration => {
    const values = new FormData(form);
    const selectedMount = selectedHardware();
    const targetHardwareId = hardwareTargetScope === "instance" &&
      selectedMount?.role === activeRole
      ? selectedMount.hardwareId
      : undefined;
    const current = resolveHardwareComponentModel(
      configuration,
      activeRole,
      targetHardwareId
    );
    if (!current) throw new Error(`No role-level model resolves for ${activeRole}.`);
    const presetId = String(values.get("hardwarePresetId") ?? "").trim();
    if (presetId) {
      if (targetHardwareId) {
        const preset = listHardwareModelCatalogPresets(activeRole).find(
          (candidate) => candidate.presetId === presetId
        );
        if (!preset) throw new Error(`Hardware preset ${presetId} is not compatible.`);
        return replaceWindowHardwareInstanceModel(
          configuration,
          activeRole,
          targetHardwareId,
          preset.model
        );
      }
      return applyHardwareModelCatalogPreset(configuration, activeRole, presetId);
    }
    const primitiveValue = String(values.get("primitiveId") ?? "keep-current");
    const geometry = primitiveValue === "keep-current" && current.geometry.kind === "gltf"
      ? current.geometry
      : {
          kind: "parametric" as const,
          primitiveId: hardwarePrimitiveChoices(activeRole).includes(
            primitiveValue as ParametricComponentGeometrySnapshot["primitiveId"]
          )
            ? primitiveValue as ParametricComponentGeometrySnapshot["primitiveId"]
            : hardwarePrimitiveChoices(activeRole)[0]!,
          parameters: current.geometry.kind === "parametric" &&
            current.geometry.primitiveId === primitiveValue
            ? { ...current.geometry.parameters }
            : {}
        };
    const {
      materialCode: _materialCode,
      machiningTemplateId: _template,
      ...previewModel
    } = current;
    const nextModel = {
      ...previewModel,
      modelId: String(values.get("modelId") ?? "").trim(),
      modelVersion: String(values.get("modelVersion") ?? "").trim(),
      geometry,
      dimensionsMm: {
        widthMm: Number(values.get("widthMm")),
        heightMm: Number(values.get("heightMm")),
        depthMm: Number(values.get("depthMm"))
      },
      mount: readComponentModelMount(values),
      appearance: {
        ...current.appearance,
        appearanceVersion: nextEditorAppearanceVersion(
          current.appearance.appearanceVersion,
          currentDocument.revision
        ),
        baseColor: String(values.get("modelColor") ?? "").trim()
      },
      productionStatus: "preview-only"
    } as const;
    return targetHardwareId
      ? replaceWindowHardwareInstanceModel(
          configuration,
          activeRole,
          targetHardwareId,
          nextModel
        )
      : replaceWindowHardwareRoleModel(configuration, activeRole, nextModel);
  };

  let visualPreviewFrame: number | undefined;

  /** Applies a valid local form draft only to the renderer-facing preview store. */
  const updateVisualPreview = (form: HTMLFormElement): void => {
    const window = editingWindow();
    const configuration = window?.visualConfiguration;
    if (!window || !configuration || !form.checkValidity()) {
      if (window) visualPreview.clear(window.objectId);
      return;
    }
    try {
      const nextConfiguration = form.matches("[data-appearance-form]")
        ? appearanceDraftFromForm(form, configuration)
        : form.matches("[data-hardware-model-form]")
          ? hardwareDraftFromForm(form, configuration)
          : undefined;
      if (!nextConfiguration) return;
      visualPreview.set(window.objectId, nextConfiguration);
      const status = container.querySelector<HTMLElement>("[data-visual-preview-status]");
      if (status) status.textContent = "正在预览未保存草稿；BOM、revision和撤销历史保持不变。";
      const error = container.querySelector<HTMLElement>("[data-appearance-editor-error]");
      if (error) error.hidden = true;
    } catch (error) {
      visualPreview.clear(window.objectId);
      const status = container.querySelector<HTMLElement>("[data-visual-preview-status]");
      if (status) {
        status.textContent = error instanceof Error
          ? `当前草稿暂不能预览：${error.message}`
          : "当前草稿暂不能预览。";
      }
    }
  };

  /** Coalesces rapid keyboard/range input to one 2D/3D repaint per frame. */
  const scheduleVisualPreview = (form: HTMLFormElement): void => {
    if (visualPreviewFrame !== undefined) cancelAnimationFrame(visualPreviewFrame);
    visualPreviewFrame = requestAnimationFrame(() => {
      visualPreviewFrame = undefined;
      if (form.isConnected) updateVisualPreview(form);
    });
  };

  /**
   * Applies business catalog choices and maintains advanced technical drafts.
   *
   * Reviewed appearance, surround and hardware selections commit their exact
   * versioned snapshot immediately, so the drawing responds to one business
   * choice. Subsequent manual PBR/model field edits clear the preset selector
   * and remain preview-only until the explicit technical submit action.
   *
   * @example Selecting “圆形旋钮” replaces the model immediately; changing its
   * width afterwards turns the form into an unapproved technical draft.
   * @since 0.10.8
   * @modified 2026-09-20 - Made reviewed directory choices apply immediately.
   */
  const onChange = (event: Event): void => {
    const target = event.target;
    if (!(target instanceof HTMLInputElement || target instanceof HTMLSelectElement)) return;
    if (target instanceof HTMLInputElement && target.type === "file") {
      void stageManagedFile(target);
      return;
    }
    if (
      target instanceof HTMLSelectElement &&
      target.matches("[data-surround-catalog-selection]")
    ) {
      const window = editingWindow();
      if (!window || !target.value) return;
      const preset = listSurroundBusinessCatalogPresets(window.profileSystemId)
        .find(({ selection }) =>
          `${selection.catalogItemId}@${selection.catalogVersion}` === target.value);
      if (!preset) {
        showError("所选包边/衬板型号已不存在或不适用于当前系列。");
        return;
      }
      // Business selections commit immediately. A designer choosing a package
      // expects the perimeter to appear, not a hidden draft that needs a second
      // technical button. The command remains one undoable history operation.
      // @since 0.10.39
      // @modified 2026-09-20 - Made surround catalog selection visible immediately.
      visualPreview.clear(window.objectId);
      commandSequence += 1;
      session.execute(createUpdateWindowSurroundCatalogSelectionCommand({
        commandId: `CMD-SURROUND-CATALOG-${session.document.revision}-${commandSequence}`,
        windowId: window.objectId,
        selection: preset.selection
      }));
      return;
    }
    if (target instanceof HTMLSelectElement && target.name === "slot" && WINDOW_APPEARANCE_EDITOR_SLOTS.includes(
      target.value as WindowAppearanceEditorSlot
    )) {
      const window = editingWindow();
      if (window) visualPreview.clear(window.objectId);
      activeSlot = target.value as WindowAppearanceEditorSlot;
      render();
    } else if (target instanceof HTMLSelectElement && target.name === "role" && HARDWARE_EDITOR_ROLES.includes(
      target.value as OpeningHardwareRole
    )) {
      const window = editingWindow();
      if (window) visualPreview.clear(window.objectId);
      activeRole = target.value as OpeningHardwareRole;
      if (selectedHardware()?.role !== activeRole) hardwareTargetScope = "role";
      render();
    } else if (
      target instanceof HTMLSelectElement &&
      target.name === "hardwareTargetScope" &&
      (target.value === "role" || target.value === "instance")
    ) {
      const window = editingWindow();
      if (window) visualPreview.clear(window.objectId);
      hardwareTargetScope = target.value;
      render();
    } else if (target instanceof HTMLSelectElement && target.name === "appearancePresetId") {
      const preset = listAppearanceCatalogPresets(activeSlot).find((candidate) =>
        candidate.presetId === target.value);
      if (!preset) return;
      const window = editingWindow();
      const configuration = window?.visualConfiguration;
      if (!window || !configuration) return;
      // Reviewed wall/profile/glass presets are user-facing choices. Commit the
      // exact snapshot now so choosing “红砖墙” changes both 2D and 3D without
      // exposing the later developer-only PBR form workflow.
      visualPreview.clear(window.objectId);
      commandSequence += 1;
      session.execute(createUpdateWindowVisualConfigurationCommand({
        commandId: `CMD-APPEARANCE-CATALOG-${session.document.revision}-${commandSequence}`,
        windowId: window.objectId,
        visualConfiguration: applyAppearanceCatalogPreset(
          configuration,
          activeSlot,
          preset.presetId
        )
      }));
      return;
    } else if (target instanceof HTMLSelectElement && target.name === "hardwarePresetId") {
      const preset = listHardwareModelCatalogPresets(activeRole).find((candidate) =>
        candidate.presetId === target.value);
      if (!preset) return;
      const window = editingWindow();
      const configuration = window?.visualConfiguration;
      if (!window || !configuration) return;
      const mount = selectedHardware();
      const exactHardwareId = hardwareTargetScope === "instance" && mount?.role === activeRole
        ? mount.hardwareId
        : undefined;
      // A reviewed handle/hinge choice is a real design-model replacement, not
      // merely a form fill. Parametric shapes and managed GLB models therefore
      // change in both renderers as soon as the user selects their business ID.
      visualPreview.clear(window.objectId);
      commandSequence += 1;
      session.execute(createUpdateWindowVisualConfigurationCommand({
        commandId: `CMD-HARDWARE-CATALOG-${session.document.revision}-${commandSequence}`,
        windowId: window.objectId,
        visualConfiguration: exactHardwareId
          ? replaceWindowHardwareInstanceModel(
              configuration,
              activeRole,
              exactHardwareId,
              preset.model
            )
          : applyHardwareModelCatalogPreset(
              configuration,
              activeRole,
              preset.presetId
            )
      }));
      return;
    } else {
      const appearanceForm = target.closest<HTMLFormElement>("[data-appearance-form]");
      const appearancePreset = appearanceForm?.elements.namedItem("appearancePresetId");
      if (appearancePreset instanceof HTMLSelectElement) appearancePreset.value = "";
      const hardwareForm = target.closest<HTMLFormElement>("[data-hardware-model-form]");
      const hardwarePreset = hardwareForm?.elements.namedItem("hardwarePresetId");
      if (hardwarePreset instanceof HTMLSelectElement) hardwarePreset.value = "";
    }
    const changedForm = target.closest<HTMLFormElement>(
      "[data-appearance-form], [data-hardware-model-form]"
    );
    if (changedForm?.isConnected) scheduleVisualPreview(changedForm);
  };

  /** Keeps text/range edits visually live before native change/blur fires. */
  const onInput = (event: Event): void => {
    const target = event.target;
    if (!(target instanceof HTMLInputElement || target instanceof HTMLSelectElement)) return;
    if (target instanceof HTMLInputElement && target.type === "file") return;
    const appearanceForm = target.closest<HTMLFormElement>("[data-appearance-form]");
    if (appearanceForm && target.name !== "appearancePresetId" && target.name !== "slot") {
      const preset = appearanceForm.elements.namedItem("appearancePresetId");
      if (preset instanceof HTMLSelectElement) preset.value = "";
      scheduleVisualPreview(appearanceForm);
      return;
    }
    const hardwareForm = target.closest<HTMLFormElement>("[data-hardware-model-form]");
    if (
      hardwareForm &&
      target.name !== "hardwarePresetId" &&
      target.name !== "role" &&
      target.name !== "hardwareTargetScope"
    ) {
      const preset = hardwareForm.elements.namedItem("hardwarePresetId");
      if (preset instanceof HTMLSelectElement) preset.value = "";
      scheduleVisualPreview(hardwareForm);
    }
  };

  /** Converts one completed stage into exactly one shared history command. */
  const onSubmit = async (event: SubmitEvent): Promise<void> => {
    const form = event.target;
    if (!(form instanceof HTMLFormElement)) return;
    event.preventDefault();
    const window = editingWindow();
    const configuration = window?.visualConfiguration;
    if (!window || !configuration) return;
    const values = new FormData(form);
    const submitButton = form.querySelector<HTMLButtonElement>('button[type="submit"]');
    if (submitButton) submitButton.disabled = true;
    try {
      let nextConfiguration;
      if (form.matches("[data-appearance-form]")) {
        nextConfiguration = appearanceDraftFromForm(form, configuration);
      } else if (form.matches("[data-hardware-model-form]")) {
        nextConfiguration = hardwareDraftFromForm(form, configuration);
      } else if (form.matches("[data-managed-model-import-form]")) {
        await uploadManagedFiles(form);
        const selectedMount = selectedHardware();
        const targetHardwareId = hardwareTargetScope === "instance" &&
          selectedMount?.role === activeRole
          ? selectedMount.hardwareId
          : undefined;
        const current = resolveHardwareComponentModel(
          configuration,
          activeRole,
          targetHardwareId
        );
        if (!current) return;
        const sourceUnitValue = String(values.get("sourceUnit") ?? "");
        const upAxisValue = String(values.get("upAxis") ?? "");
        const forwardAxisValue = String(values.get("forwardAxis") ?? "");
        if (!COMPONENT_ASSET_SOURCE_UNITS.includes(sourceUnitValue as ComponentAssetSourceUnit)) {
          throw new Error("请选择受支持的模型源单位。");
        }
        if (
          !COMPONENT_ASSET_AXES.includes(upAxisValue as ComponentAssetAxis) ||
          !COMPONENT_ASSET_AXES.includes(forwardAxisValue as ComponentAssetAxis)
        ) {
          throw new Error("请选择受支持的向上轴和朝外轴。");
        }
        const mediumAssetId = String(values.get("mediumAssetId") ?? "").trim();
        const mediumContentHash = String(values.get("mediumContentHash") ?? "").trim();
        const lowAssetId = String(values.get("lowAssetId") ?? "").trim();
        const lowContentHash = String(values.get("lowContentHash") ?? "").trim();
        if (Boolean(mediumAssetId) !== Boolean(mediumContentHash)) {
          throw new Error("中清资产ID和SHA-256必须同时填写。");
        }
        if (Boolean(lowAssetId) !== Boolean(lowContentHash)) {
          throw new Error("低清资产ID和SHA-256必须同时填写。");
        }
        const geometry = createManagedGltfGeometrySnapshot({
          primaryAsset: {
            assetId: String(values.get("primaryAssetId") ?? "").trim(),
            contentHash: String(values.get("primaryContentHash") ?? "").trim()
          },
          lodAssets: [
            ...(mediumAssetId
              ? [{ quality: "medium" as const, assetId: mediumAssetId,
                  contentHash: mediumContentHash }]
              : []),
            ...(lowAssetId
              ? [{ quality: "low" as const, assetId: lowAssetId,
                  contentHash: lowContentHash }]
              : [])
          ],
          sourceUnit: sourceUnitValue as ComponentAssetSourceUnit,
          upAxis: upAxisValue as ComponentAssetAxis,
          forwardAxis: forwardAxisValue as ComponentAssetAxis
        });
        const {
          materialCode: _materialCode,
          machiningTemplateId: _template,
          ...previewModel
        } = current;
        const nextModel = {
          ...previewModel,
          modelId: String(values.get("modelId") ?? "").trim(),
          modelVersion: String(values.get("modelVersion") ?? "").trim(),
          geometry,
          dimensionsMm: {
            widthMm: Number(values.get("widthMm")),
            heightMm: Number(values.get("heightMm")),
            depthMm: Number(values.get("depthMm"))
          },
          mount: readComponentModelMount(values),
          appearance: {
            ...current.appearance,
            appearanceVersion: nextEditorAppearanceVersion(
              current.appearance.appearanceVersion,
              currentDocument.revision
            ),
            baseColor: String(values.get("modelColor") ?? "").trim()
          },
          productionStatus: "preview-only"
        } as const;
        nextConfiguration = targetHardwareId
          ? replaceWindowHardwareInstanceModel(
              configuration,
              activeRole,
              targetHardwareId,
              nextModel
            )
          : replaceWindowHardwareRoleModel(configuration, activeRole, nextModel);
      } else {
        return;
      }
      commandSequence += 1;
      session.execute(createUpdateWindowVisualConfigurationCommand({
        commandId: `CMD-VISUAL-EDITOR-${session.document.revision}-${commandSequence}`,
        windowId: window.objectId,
        visualConfiguration: nextConfiguration
      }));
    } catch (error) {
      showError(error instanceof Error ? error.message : "外观配置无效。请检查输入。 ");
    } finally {
      if (submitButton?.isConnected) submitButton.disabled = false;
    }
  };

  container.addEventListener("click", onClick);
  container.addEventListener("input", onInput);
  container.addEventListener("change", onChange);
  container.addEventListener("submit", onSubmit);
  const disposeDocument = session.subscribe((document) => {
    currentDocument = document;
    render();
  });
  const disposeSelection = selection.subscribe((state) => {
    selectedObjectId = state.objectId;
    const selectedId = state.objectId ? String(state.objectId) : "";
    if (selectedId.includes(":installation.wall")) {
      activeSlot = "wall";
      activeStep = "appearance";
    } else if (selectedId.includes(":installation.surround")) {
      activeSlot = selectedId.includes(":installation.surround.inside")
        ? "surroundInside"
        : selectedId.includes(":installation.surround.liner")
          ? "surroundLiner"
          : "surroundOutside";
      activeStep = "appearance";
    }
    const mount = selectedHardware();
    if (mount) {
      activeRole = mount.role;
      hardwareTargetScope = "instance";
      activeStep = "hardware";
    } else {
      hardwareTargetScope = "role";
    }
    render();
  });
  return () => {
    disposed = true;
    container.removeEventListener("click", onClick);
    container.removeEventListener("input", onInput);
    container.removeEventListener("change", onChange);
    container.removeEventListener("submit", onSubmit);
    disposeDocument();
    disposeSelection();
    if (visualPreviewFrame !== undefined) cancelAnimationFrame(visualPreviewFrame);
    visualPreview.clearAll();
    container.replaceChildren();
  };
}

/**
 * Creates undo and redo controls that work identically in either shell.
 *
 * @param session Shared session whose history should be navigated.
 * @returns A DOM element ready for placement by a layout shell.
 * @example `panel.append(createHistoryControls(session))`.
 * @since 0.1.0
 * @modified 2026-09-17 - Kept history controls shared across both layouts.
 */
export function createHistoryControls(session: DesignSession): HTMLElement {
  const controls = document.createElement("div");
  controls.className = "shell-history";
  const undo = document.createElement("button");
  undo.type = "button";
  undo.textContent = "撤销";
  undo.addEventListener("click", () => session.undo());
  const redo = document.createElement("button");
  redo.type = "button";
  redo.textContent = "重做";
  redo.addEventListener("click", () => session.redo());
  controls.append(undo, redo);
  return controls;
}
