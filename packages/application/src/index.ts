import type {
  AddFabricationAssemblyInstanceCommand,
  AssemblyOpeningClearance,
  CommandExecutionResult,
  AddWindowTopologyMemberCommand,
  CreateDrawingTextLabelCommand,
  CreateFabricationAssemblyCommand,
  CreateRectangularWindowCommand,
  DeleteDrawingTextLabelCommand,
  DeleteWindowCommand,
  DesignCommand,
  DesignDocument,
  DesignObjectId,
  DrawingTextLabel,
  EngineeringJoint,
  EngineeringJointCatalogSelectionSnapshot,
  EngineeringJointType,
  EqualizeWindowGridCommand,
  GlassCatalogSelectionSnapshot,
  MergeWindowGridDividerCommand,
  MoveOpeningMeetingMullionCommand,
  MoveWindowGridDividerCommand,
  MoveWindowTopologyMemberCommand,
  ProductTemplateSelectionSnapshot,
  RemoveLastWindowGridTrackCommand,
  RemoveWindowTopologyMemberCommand,
  ResizeWindowCommand,
  SetWindowCellOpeningCommand,
  SplitWindowGridCommand,
  SurroundCatalogSelectionSnapshot,
  UpdateFabricationAssemblyInstallationCommand,
  UpdateDrawingTextLabelCommand,
  UpdateWindowInstallationCommand,
  UpdateEngineeringJointCommand,
  UpdateWindowGlassCatalogSelectionCommand,
  UpdateWindowDesignComponentRemarksCommand,
  UpdateWindowMarkCommand,
  UpdateWindowSurroundCatalogSelectionCommand,
  UpdateWindowProfileGeometryCommand,
  UpdateWindowVisualConfigurationCommand,
  UpdateWindowTopologyMemberCommand,
  WindowGridAxis,
  WindowDesignComponentRemarks,
  WindowGridLayout,
  WindowInstallation,
  WindowSectionDimensions,
  WindowTopology,
  WindowUnit,
  WindowUnitInstance,
  WindowVisualConfiguration
} from "@doormes/contracts";
import { applyDesignCommand, toDesignObjectId } from "@doormes/domain";
import {
  findEngineeringJointCatalogSelection,
  listEngineeringJointCatalogSelections,
  requireEngineeringJointCatalogSelection
} from "@doormes/engineering-joint-catalog";
import {
  createWindowInstallationSurroundObjectId,
  createWindowInstallationWallObjectId,
  resolveWindowGeometry,
  REFERENCE_WINDOW_INSTALLATION
} from "@doormes/geometry-topology";
import {
  createZcsungTemplateSelectionSnapshot,
  listZcsungProductTemplates,
  requireZcsungSimulationPreset
} from "@doormes/product-template-catalog";

export {
  findEngineeringJointCatalogSelection,
  listEngineeringJointCatalogSelections,
  requireEngineeringJointCatalogSelection
};

/**
 * Callback used by renderers and shells to observe the current design snapshot.
 *
 * @example `session.subscribe(document => render(document))`.
 * @since 0.1.0
 * @modified 2026-09-17 - Added shared-session observation.
 */
export type DesignSessionListener = (document: DesignDocument) => void;

/**
 * Identifies the interaction surface that most recently changed selection.
 *
 * The source is presentation metadata only: manufacturing and geometry code
 * consume the selected stable ID but never branch on the input device.
 *
 * @example Selecting a mullion in the WebGL viewport publishes source `3d`.
 * @since 0.4.0
 * @modified 2026-09-17 - Added cross-view selection provenance.
 */
export type DesignSelectionSource = "2d" | "3d" | "tree" | "system";

/**
 * Immutable snapshot shared by the 2D view, 3D view and object tree.
 *
 * @example `{ objectId: "M-1", source: "tree", revision: 3 }`.
 * @since 0.4.0
 * @modified 2026-09-17 - Added the first shared presentation-state contract.
 */
export interface DesignSelectionState {
  readonly objectId?: DesignObjectId;
  readonly source: DesignSelectionSource;
  readonly revision: number;
}

/** Callback notified immediately and after every effective selection change. */
export type DesignSelectionListener = (state: DesignSelectionState) => void;

/**
 * Coordinates one stable selection across independent presentation adapters.
 *
 * Selection deliberately lives outside `DesignDocument`: selecting an object
 * must not create an undo entry, change a BOM revision or dirty a saved design.
 * Views publish only stable domain IDs, which prevents SVG elements or mutable
 * Three.js objects from leaking into application state.
 *
 * @example `selection.select(toDesignObjectId("CELL-1"), "2d")` highlights the
 * same cell in SVG, Three.js and the object tree.
 * @since 0.4.0
 * @modified 2026-09-17 - Added shared 2D/3D/tree selection state.
 */
export class DesignSelectionStore {
  readonly #listeners = new Set<DesignSelectionListener>();
  #state: DesignSelectionState = { source: "system", revision: 0 };

  /** Returns the latest immutable selection snapshot. */
  get state(): DesignSelectionState {
    return this.#state;
  }

  /**
   * Selects one object or clears selection when the ID is omitted.
   *
   * Duplicate object/source pairs are ignored so reciprocal view updates never
   * form a notification loop. A real change increments the local revision and
   * notifies a copied listener set, allowing safe unsubscription in callbacks.
   *
   * @param objectId Stable object ID, or `undefined` to clear selection.
   * @param source Surface that initiated the change.
   * @example `store.select(undefined, "system")` clears all highlights.
   * @since 0.4.0
   * @modified 2026-09-17 - Added deterministic selection publication.
   */
  select(objectId: string | DesignObjectId | undefined, source: DesignSelectionSource): void {
    const stableId = objectId as DesignObjectId | undefined;
    if (this.#state.objectId === stableId && this.#state.source === source) return;
    this.#state = { objectId: stableId, source, revision: this.#state.revision + 1 };
    for (const listener of [...this.#listeners]) listener(this.#state);
  }

  /**
   * Subscribes a view and immediately supplies the current selection.
   *
   * @param listener 2D, 3D or tree projection callback.
   * @returns A disposer that removes only this listener.
   * @example `const dispose = store.subscribe(renderHighlight)`.
   * @since 0.4.0
   * @modified 2026-09-17 - Added lifecycle-safe selection observation.
   */
  subscribe(listener: DesignSelectionListener): () => void {
    this.#listeners.add(listener);
    listener(this.#state);
    return () => this.#listeners.delete(listener);
  }
}

/**
 * Immutable shared state for the 2D engineering viewport and visibility flags.
 *
 * Transform values use SVG engineering-view coordinates. Visibility flags are
 * shared presentation preferences consumed by SVG and Three.js; changing them
 * never dirties the design or invalidates BOM results.
 *
 * @example `{ scale: 1.12, x: -30, y: 12, showDimensions: true }`.
 * @since 0.4.1
 * @modified 2026-09-20 - Added independent 3D wall/package visibility for factory preview.
 */
export interface DesignCanvasViewState {
  readonly scale: number;
  readonly x: number;
  readonly y: number;
  /**
   * Selects the interactive 2D presentation without changing design data.
   * `material` previews catalogue colours/textures; `engineering-line` keeps
   * the same geometry and annotations but suppresses material rendering.
   */
  readonly twoDimensionalRenderStyle: "material" | "engineering-line";
  readonly showDimensions: boolean;
  readonly showPlanView: boolean;
  readonly showOpeningState: boolean;
  readonly showThreeSelectionOutline: boolean;
  /** Shows the site wall only while reviewing installation context in 3D. */
  readonly showThreeInstallationHost: boolean;
  /** Shows enabled surround/package parts independently from the site wall. */
  readonly showThreeInstallationSurround: boolean;
  readonly revision: number;
}

/** Callback notified after an effective canvas-view change. */
export type DesignCanvasViewListener = (state: DesignCanvasViewState) => void;

const MIN_CANVAS_SCALE = 0.35;
const MAX_CANVAS_SCALE = 5;

/**
 * Coordinates 2D zoom, pan and display options across layout shells.
 *
 * Algorithm and bounds intentionally match the prototype: wheel steps use a
 * factor of 1.12 and scale is clamped to 0.35..5. The store accepts a viewport
 * anchor so zooming keeps the design point beneath the pointer stationary.
 *
 * @example `view.zoomAt(480, 320, 1.12)` zooms in around the canvas centre.
 * @since 0.4.1
 * @modified 2026-09-17 - Migrated prototype canvas navigation semantics.
 */
export class DesignCanvasViewStore {
  readonly #listeners = new Set<DesignCanvasViewListener>();
  #state: DesignCanvasViewState = {
    scale: 1,
    x: 0,
    y: 0,
    twoDimensionalRenderStyle: "material",
    showDimensions: true,
    showPlanView: false,
    showOpeningState: true,
    showThreeSelectionOutline: true,
    showThreeInstallationHost: false,
    showThreeInstallationSurround: true,
    revision: 0
  };

  /** Returns the latest immutable view-state snapshot. */
  get state(): DesignCanvasViewState {
    return this.#state;
  }

  /**
   * Zooms around one SVG coordinate using the prototype's anchored formula.
   *
   * @param anchorX Pointer x-coordinate in the SVG viewBox.
   * @param anchorY Pointer y-coordinate in the SVG viewBox.
   * @param factor Multiplicative zoom step, normally 1.12 or its reciprocal.
   * @example Zooming at a selected mullion keeps it under the cursor.
   * @since 0.4.1
   * @modified 2026-09-17 - Added anchored deterministic zoom.
   */
  zoomAt(anchorX: number, anchorY: number, factor: number): void {
    const previousScale = this.#state.scale;
    const nextScale = Math.max(
      MIN_CANVAS_SCALE,
      Math.min(MAX_CANVAS_SCALE, previousScale * (Number(factor) || 1))
    );
    if (Math.abs(nextScale - previousScale) < 0.0001) return;
    const ratio = nextScale / previousScale;
    this.#publish({
      ...this.#state,
      scale: nextScale,
      x: anchorX - (anchorX - this.#state.x) * ratio,
      y: anchorY - (anchorY - this.#state.y) * ratio
    });
  }

  /**
   * Applies an absolute translation produced by one pointer-pan gesture.
   *
   * @param x Horizontal SVG translation.
   * @param y Vertical SVG translation.
   * @example A 20-unit right drag calls `panTo(originX + 20, originY)`.
   * @since 0.4.1
   * @modified 2026-09-17 - Added cross-shell pan state.
   */
  panTo(x: number, y: number): void {
    if (!Number.isFinite(x) || !Number.isFinite(y)) return;
    if (x === this.#state.x && y === this.#state.y) return;
    this.#publish({ ...this.#state, x, y });
  }

  /** Restores the prototype's scale-one, zero-translation canvas view. */
  resetTransform(): void {
    if (this.#state.scale === 1 && this.#state.x === 0 && this.#state.y === 0) return;
    this.#publish({ ...this.#state, scale: 1, x: 0, y: 0 });
  }

  /**
   * Switches between the material preview and factory-oriented linework view.
   *
   * This is presentation state only: object IDs, geometry, dimensions,
   * selection, BOM inputs and the document revision remain unchanged. A shell
   * can therefore switch styles while editing the same mullion or window.
   *
   * @param style Requested shared 2D render style.
   * @example `view.setTwoDimensionalRenderStyle("engineering-line")` prints linework.
   * @since 0.10.71
   * @modified 2026-09-22 - Added UI-2D-008 dual-style drawing presentation.
   */
  setTwoDimensionalRenderStyle(style: "material" | "engineering-line"): void {
    if (this.#state.twoDimensionalRenderStyle === style) return;
    this.#publish({ ...this.#state, twoDimensionalRenderStyle: style });
  }

  /**
   * Toggles shared engineering overlays without mutating design geometry.
   *
   * @param option Engineering overlay or selection-helper visibility flag.
   * @param value Requested visibility.
   * @example `setOption("showPlanView", true)` adds the shared plan projection.
   * @since 0.4.1
   * @modified 2026-09-20 - Separated factory product preview from optional 3D installation context.
   */
  setOption(
    option: "showDimensions" | "showPlanView" | "showOpeningState" |
      "showThreeSelectionOutline" | "showThreeInstallationHost" |
      "showThreeInstallationSurround",
    value: boolean
  ): void {
    if (this.#state[option] === value) return;
    this.#publish({ ...this.#state, [option]: value });
  }

  /** Subscribes a renderer and immediately publishes the current view state. */
  subscribe(listener: DesignCanvasViewListener): () => void {
    this.#listeners.add(listener);
    listener(this.#state);
    return () => this.#listeners.delete(listener);
  }

  /** Stores one revised immutable snapshot and notifies copied listeners. */
  #publish(next: Omit<DesignCanvasViewState, "revision"> & { revision?: number }): void {
    this.#state = { ...next, revision: this.#state.revision + 1 };
    for (const listener of [...this.#listeners]) listener(this.#state);
  }
}

/**
 * Owns the current design snapshot and command-level undo/redo history.
 *
 * Both PC and mobile shells share one instance. The class delegates all domain
 * changes to `applyDesignCommand`; it never contains geometry, rendering or BOM
 * rules. One command creates one history entry, so a drag gesture must emit a
 * final command rather than hundreds of pointer samples.
 *
 * @example `const session = new DesignSession(createEmptyDesign("D-1"))`.
 * @since 0.1.0
 * @modified 2026-09-17 - Implemented execute, undo, redo and subscriptions.
 */
export class DesignSession {
  readonly #listeners = new Set<DesignSessionListener>();
  readonly #undoStack: DesignDocument[] = [];
  readonly #redoStack: DesignDocument[] = [];
  #document: DesignDocument;

  /**
   * Creates a session around an immutable design snapshot.
   *
   * @param initialDocument Snapshot loaded from a repository or new-design factory.
   * @example `new DesignSession(initialDocument)`.
   * @since 0.1.0
   * @modified 2026-09-17 - Added initial session construction.
   */
  constructor(initialDocument: DesignDocument) {
    this.#document = initialDocument;
  }

  /**
   * Returns the current immutable snapshot without exposing mutable internals.
   *
   * @returns The snapshot consumed by renderers and view models.
   * @example `renderDesignSvg(session.document)`.
   * @since 0.1.0
   * @modified 2026-09-17 - Added current-document access.
   */
  get document(): DesignDocument {
    return this.#document;
  }

  /**
   * Opens a complete validated project and starts a fresh editing history.
   *
   * Project import is not an ordinary component command: undo must never jump
   * back into the previously open file. The caller validates and migrates the
   * document first; this method detaches the snapshot, clears both history
   * stacks, and publishes exactly one replacement notification so renderers,
   * preview state, selection reconciliation and auto-save update together.
   *
   * @param document Complete formal design returned by a repository/parser.
   * @example `session.replaceDocument(importedSnapshot.document)`.
   * @since 0.10.21
   * @modified 2026-09-18 - Added portable project-file opening semantics.
   */
  replaceDocument(document: DesignDocument): void {
    this.#document = structuredClone(document);
    this.#undoStack.length = 0;
    this.#redoStack.length = 0;
    this.#notify();
  }

  /**
   * Executes one shared command and records a single undo boundary.
   *
   * Algorithm: run the domain reducer, push the previous snapshot, clear redo,
   * publish the new snapshot and return the domain change set for invalidation.
   *
   * @param command Device-independent design command.
   * @returns New document plus object-level changes.
   * @example `session.execute(createRectangularWindowCommand(...))`.
   * @since 0.1.0
   * @modified 2026-09-17 - Added command execution and history management.
   */
  execute(command: DesignCommand): CommandExecutionResult {
    const result = applyDesignCommand(this.#document, command);
    this.#undoStack.push(this.#document);
    this.#redoStack.length = 0;
    this.#document = result.document;
    this.#notify();
    return result;
  }

  /**
   * Commits several dependent commands as one visible edit and one undo step.
   *
   * Algorithm: reduce every command against a local snapshot first. If any
   * reducer throws, the live document, history and subscribers are untouched.
   * After all commands succeed, merge their change identities, push the
   * original snapshot once and publish only the final document once.
   *
   * @param commands Ordered commands whose later entries may use earlier output.
   * @param transactionId Audit/invalidation identity for the combined user action.
   * @returns Final document plus de-duplicated aggregate changes.
   * @example Window creation followed by assembly append undoes in one click.
   * @since 0.10.47
   * @modified 2026-09-21 - Added atomic multi-command application transactions.
   */
  executeTransaction(
    commands: readonly DesignCommand[],
    transactionId: string
  ): CommandExecutionResult {
    if (commands.length === 0) {
      throw new Error("A design transaction requires at least one command.");
    }
    if (!transactionId.trim()) {
      throw new Error("A design transaction ID must not be empty.");
    }

    let candidate = this.#document;
    const created = new Set<DesignObjectId>();
    const updated = new Set<DesignObjectId>();
    const removed = new Set<DesignObjectId>();
    for (const command of commands) {
      const result = applyDesignCommand(candidate, command);
      candidate = result.document;
      for (const objectId of result.changes.createdObjectIds) {
        removed.delete(objectId);
        updated.delete(objectId);
        created.add(objectId);
      }
      for (const objectId of result.changes.updatedObjectIds) {
        if (!created.has(objectId) && !removed.has(objectId)) updated.add(objectId);
      }
      for (const objectId of result.changes.removedObjectIds) {
        created.delete(objectId);
        updated.delete(objectId);
        removed.add(objectId);
      }
    }

    this.#undoStack.push(this.#document);
    this.#redoStack.length = 0;
    this.#document = candidate;
    this.#notify();
    return {
      document: candidate,
      changes: {
        commandId: transactionId.trim(),
        createdObjectIds: [...created],
        updatedObjectIds: [...updated],
        removedObjectIds: [...removed]
      }
    };
  }

  /**
   * Restores the previous immutable document snapshot when one exists.
   *
   * @returns `true` when a snapshot was restored, otherwise `false`.
   * @example A create command followed by `undo()` returns to an empty design.
   * @since 0.1.0
   * @modified 2026-09-17 - Added command-level undo.
   */
  undo(): boolean {
    const previous = this.#undoStack.pop();
    if (!previous) return false;
    this.#redoStack.push(this.#document);
    this.#document = previous;
    this.#notify();
    return true;
  }

  /**
   * Reapplies the most recently undone immutable document snapshot.
   *
   * @returns `true` when a snapshot was restored, otherwise `false`.
   * @example Call after `undo()` to restore the created window.
   * @since 0.1.0
   * @modified 2026-09-17 - Added command-level redo.
   */
  redo(): boolean {
    const next = this.#redoStack.pop();
    if (!next) return false;
    this.#undoStack.push(this.#document);
    this.#document = next;
    this.#notify();
    return true;
  }

  /**
   * Registers a snapshot observer and immediately sends the current document.
   *
   * @param listener Renderer or view-model callback.
   * @returns A disposer that removes only this listener.
   * @example `const dispose = session.subscribe(render); dispose()`.
   * @since 0.1.0
   * @modified 2026-09-17 - Added lifecycle-safe subscriptions.
   */
  subscribe(listener: DesignSessionListener): () => void {
    this.#listeners.add(listener);
    listener(this.#document);
    return () => this.#listeners.delete(listener);
  }

  /**
   * Notifies every active observer after a successful state transition.
   *
   * A copied listener set makes unsubscription during callbacks safe.
   *
   * @example Called internally by execute, undo and redo.
   * @since 0.1.0
   * @modified 2026-09-17 - Added deterministic observer dispatch.
   */
  #notify(): void {
    for (const listener of [...this.#listeners]) {
      listener(this.#document);
    }
  }
}

/**
 * Creates the device-independent command for one connected factory assembly.
 *
 * Every nested array/object is detached before dispatch so a desktop/mobile
 * form cannot mutate command history after execution. IDs are converted at the
 * application boundary and physical graph/placement validation remains in the
 * shared domain/geometry layer.
 *
 * @param input Stable assembly identity, instances, joints and reference-host input.
 * @returns A complete command accepted by `DesignSession`.
 * @example Two side-by-side windows use one right/left joint and a 30mm gap.
 * @since 0.10.45
 * @modified 2026-09-20 - Added the ASSEMBLY-001 command factory.
 */
export function createFabricationAssemblyCommand(input: {
  commandId: string;
  assemblyId: string;
  mark: string;
  instances: readonly (Omit<WindowUnitInstance, "objectId" | "windowId"> & {
    readonly objectId: string;
    readonly windowId: string;
  })[];
  joints: readonly (Omit<
    EngineeringJoint,
    "objectId" | "firstInstanceId" | "secondInstanceId"
  > & {
    readonly objectId: string;
    readonly firstInstanceId: string;
    readonly secondInstanceId: string;
  })[];
  openingClearance: AssemblyOpeningClearance;
  installation: WindowInstallation;
}): CreateFabricationAssemblyCommand {
  return {
    type: "assembly.create",
    commandId: input.commandId,
    assemblyId: toDesignObjectId(input.assemblyId),
    mark: input.mark,
    instances: input.instances.map((instance) => ({
      objectId: toDesignObjectId(instance.objectId),
      windowId: toDesignObjectId(instance.windowId),
      transform: { ...instance.transform }
    })),
    joints: input.joints.map((joint) => ({
      ...joint,
      objectId: toDesignObjectId(joint.objectId),
      firstInstanceId: toDesignObjectId(joint.firstInstanceId),
      secondInstanceId: toDesignObjectId(joint.secondInstanceId),
      ...(joint.cornerConfiguration ? {
        cornerConfiguration: { ...joint.cornerConfiguration }
      } : {}),
      ...(joint.catalogSelection ? {
        catalogSelection: cloneEngineeringJointCatalogSelection(joint.catalogSelection)
      } : {})
    })),
    openingClearance: { ...input.openingClearance },
    installation: cloneWindowInstallation(input.installation)
  };
}

/**
 * Creates the command that introduces one instance into an existing assembly.
 *
 * Inputs are detached so later form edits cannot mutate undo/redo history; the
 * domain remains responsible for full-graph port, placement and envelope checks.
 *
 * @param input Existing assembly identity plus one new instance/joint pair.
 * @returns A device-independent append command.
 * @example Append `WIN-3` to `ASSEMBLY-1` through `ASSEMBLY-1:J2`.
 * @since 0.10.47
 * @modified 2026-09-21 - Added shared PC/mobile append command construction.
 */
export function createAddFabricationAssemblyInstanceCommand(input: {
  commandId: string;
  assemblyId: string;
  instance: Omit<WindowUnitInstance, "objectId" | "windowId"> & {
    readonly objectId: string;
    readonly windowId: string;
  };
  joint: Omit<EngineeringJoint, "objectId" | "firstInstanceId" | "secondInstanceId"> & {
    readonly objectId: string;
    readonly firstInstanceId: string;
    readonly secondInstanceId: string;
  };
}): AddFabricationAssemblyInstanceCommand {
  return {
    type: "assembly.add-instance",
    commandId: input.commandId,
    assemblyId: toDesignObjectId(input.assemblyId),
    instance: {
      objectId: toDesignObjectId(input.instance.objectId),
      windowId: toDesignObjectId(input.instance.windowId),
      transform: { ...input.instance.transform }
    },
    joint: {
      ...input.joint,
      objectId: toDesignObjectId(input.joint.objectId),
      firstInstanceId: toDesignObjectId(input.joint.firstInstanceId),
      secondInstanceId: toDesignObjectId(input.joint.secondInstanceId),
      ...(input.joint.cornerConfiguration ? {
        cornerConfiguration: { ...input.joint.cornerConfiguration }
      } : {}),
      ...(input.joint.catalogSelection ? {
        catalogSelection: cloneEngineeringJointCatalogSelection(input.joint.catalogSelection)
      } : {})
    }
  };
}

/**
 * Creates one device-independent engineering-joint edit command.
 *
 * The helper converts external string IDs to formal design IDs while leaving
 * graph placement to the domain reducer. Desktop and mobile therefore submit
 * the same business intent and cannot persist different connector positions.
 *
 * @example Set `ASSEMBLY-1:J1` to a 45mm reinforced factory connection.
 * @since 0.10.85
 * @modified 2026-09-22 - Added exact business-catalog selection snapshots.
 */
export function createUpdateEngineeringJointCommand(input: {
  commandId: string;
  assemblyId: string | DesignObjectId;
  jointId: string | DesignObjectId;
  jointType: EngineeringJointType;
  gapMm: number;
  factoryScope: "factory" | "site";
  cornerConfiguration?: EngineeringJoint["cornerConfiguration"];
  catalogSelection?: EngineeringJointCatalogSelectionSnapshot;
}): UpdateEngineeringJointCommand {
  return {
    type: "assembly.update-joint",
    commandId: input.commandId,
    assemblyId: toDesignObjectId(input.assemblyId),
    jointId: toDesignObjectId(input.jointId),
    jointType: input.jointType,
    gapMm: input.gapMm,
    factoryScope: input.factoryScope,
    ...(input.cornerConfiguration ? {
      cornerConfiguration: { ...input.cornerConfiguration }
    } : {}),
    ...(input.catalogSelection ? {
      catalogSelection: cloneEngineeringJointCatalogSelection(input.catalogSelection)
    } : {})
  };
}

/** Direction in which a newly created product is joined to the anchor product. */
export type FabricationConnectionDirection = "left" | "right" | "top" | "bottom" | "corner";

/** Result of planning one create-and-connect action before atomic execution. */
export interface ConnectedWindowCreationPlan {
  readonly commands: readonly DesignCommand[];
  readonly assemblyId: DesignObjectId;
  readonly anchorWindowId: DesignObjectId;
  readonly newWindowId: DesignObjectId;
  readonly newInstanceId: DesignObjectId;
  readonly jointId: DesignObjectId;
}

/**
 * Plans one PC/mobile “create and connect” action without mutating the design.
 *
 * Algorithm: resolve the anchor's assembly transform (or origin for an
 * independent anchor), enforce equal heights for left/right or equal widths for
 * top/bottom first-slice joins, place the new rectangle exactly one connector
 * gap away, then return either assembly-create or assembly-append after the
 * window-create command. `DesignSession.executeTransaction` validates and
 * commits the returned list atomically.
 *
 * @param input Current document, prepared window command and connection intent.
 * @returns Ordered commands and stable IDs for selection/status updates.
 * @example A 1200mm anchor plus 30mm joint places a right product at x=1230.
 * @since 0.10.47
 * @modified 2026-09-22 - Added frozen connection-catalog selection support.
 */
export function planConnectedWindowCreation(input: {
  document: DesignDocument;
  createWindowCommand: CreateRectangularWindowCommand;
  anchorWindowId: string | DesignObjectId;
  direction: FabricationConnectionDirection;
  jointType?: EngineeringJointType;
  gapMm: number;
  catalogSelection?: EngineeringJointCatalogSelectionSnapshot;
  /** Actual interior angle for a non-coplanar corner connection. */
  cornerIncludedAngleDeg?: number;
  /** Top-view turn from the selected anchor plane to the new window plane. */
  cornerTurnDirection?: "clockwise" | "counterclockwise";
  transactionId: string;
  /** Commands such as template opening configuration applied before assembly. */
  afterCreateCommands?: readonly DesignCommand[];
}): ConnectedWindowCreationPlan {
  const anchorWindowId = toDesignObjectId(input.anchorWindowId);
  const anchorWindow = input.document.windows.find(
    (window) => window.objectId === anchorWindowId
  );
  if (!anchorWindow) throw new Error(`连接基准窗 ${anchorWindowId} 不存在。`);
  if (input.document.windows.some(
    (window) => window.objectId === input.createWindowCommand.windowId
  )) {
    throw new Error(`新窗编号 ${input.createWindowCommand.windowId} 已存在。`);
  }
  const catalogSelection = input.catalogSelection
    ? cloneEngineeringJointCatalogSelection(input.catalogSelection)
    : undefined;
  const requestedGapMm = catalogSelection?.finishedWidthMm ?? input.gapMm;
  if (!Number.isFinite(requestedGapMm) || requestedGapMm < 0 || requestedGapMm > 300) {
    throw new RangeError("连接间隙必须是 0 至 300mm 的有效数值。");
  }

  const corner = input.direction === "corner";
  const horizontal = input.direction === "left" || input.direction === "right" || corner;
  if (horizontal && input.createWindowCommand.heightMm !== anchorWindow.heightMm) {
    throw new Error("左右连接要求新窗与基准窗等高；请先统一高度。");
  }
  if (!horizontal && input.createWindowCommand.widthMm !== anchorWindow.widthMm) {
    throw new Error("上下连接要求新窗与基准窗等宽；请先统一宽度。");
  }
  const jointType = catalogSelection?.jointType ??
    input.jointType ??
    (corner ? "corner_joint" : horizontal ? "mullion_joint" : "stacking_joint");
  if (catalogSelection && input.jointType && input.jointType !== catalogSelection.jointType) {
    throw new Error("连接目录型号与提交的连接类型不一致。");
  }
  if (catalogSelection && Math.abs(input.gapMm - catalogSelection.finishedWidthMm) >= 0.05) {
    throw new Error("连接目录型号与提交的成品宽度不一致。");
  }
  if (corner && jointType !== "corner_joint") {
    throw new Error("转角连接必须使用转角连接件型号。");
  }
  if (!corner && jointType === "corner_joint") {
    throw new Error("转角连接件只能用于非共面转角放置方式。");
  }
  if (horizontal && !corner && jointType === "stacking_joint") {
    throw new Error("左右连接不能使用叠接件，请选择拼樘或加强拼樘。");
  }
  if (!horizontal && jointType !== "stacking_joint") {
    throw new Error("上下连接必须使用叠接件。");
  }
  const cornerCapability = catalogSelection?.cornerCapability;
  const cornerIncludedAngleDeg = corner
    ? input.cornerIncludedAngleDeg ?? cornerCapability?.defaultIncludedAngleDeg ?? 90
    : undefined;
  const cornerTurnDirection = corner
    ? input.cornerTurnDirection ?? "clockwise"
    : undefined;
  if (corner) {
    if (!Number.isFinite(cornerIncludedAngleDeg) ||
      cornerIncludedAngleDeg! <= 0 || cornerIncludedAngleDeg! >= 180) {
      throw new RangeError("转角内夹角必须大于0°且小于180°。");
    }
    if (cornerTurnDirection !== "clockwise" && cornerTurnDirection !== "counterclockwise") {
      throw new TypeError("转角方向必须是顺时针或逆时针。");
    }
    if (cornerCapability && (
      cornerIncludedAngleDeg! < cornerCapability.minimumIncludedAngleDeg ||
      cornerIncludedAngleDeg! > cornerCapability.maximumIncludedAngleDeg ||
      !cornerCapability.allowedTurnDirections.includes(cornerTurnDirection)
    )) {
      throw new RangeError(
        `当前转角连接件允许${cornerCapability.minimumIncludedAngleDeg}°至` +
        `${cornerCapability.maximumIncludedAngleDeg}°。`
      );
    }
  }

  const existingAssembly = (input.document.assemblies ?? []).find((assembly) =>
    assembly.instances.some((instance) => instance.windowId === anchorWindowId)
  );
  const anchorInstance = existingAssembly?.instances.find(
    (instance) => instance.windowId === anchorWindowId
  );
  const anchorTransform = anchorInstance?.transform ?? {
    xMm: 0,
    yMm: 0,
    zMm: 0,
    rotationYDeg: 0
  };
  const assemblyId = existingAssembly?.objectId ?? uniqueDesignObjectId(
    input.document,
    `ASSEMBLY:${anchorWindowId}:${input.createWindowCommand.windowId}`
  );
  const anchorInstanceId = anchorInstance?.objectId ?? uniqueDesignObjectId(
    input.document,
    `${assemblyId}:INSTANCE:${anchorWindowId}`
  );
  const newInstanceId = uniqueDesignObjectId(
    input.document,
    `${assemblyId}:INSTANCE:${input.createWindowCommand.windowId}`
  );
  const jointId = uniqueDesignObjectId(
    input.document,
    `${assemblyId}:JOINT:${anchorWindowId}:${input.createWindowCommand.windowId}`
  );
  const gapMm = Math.round(requestedGapMm * 10) / 10;
  const anchorRadians = anchorTransform.rotationYDeg * Math.PI / 180;
  const anchorTangent = { x: Math.cos(anchorRadians), z: -Math.sin(anchorRadians) };
  const rawCornerRotationYDeg = corner
    ? anchorTransform.rotationYDeg + (180 - cornerIncludedAngleDeg!) *
      (cornerTurnDirection === "clockwise" ? 1 : -1)
    : anchorTransform.rotationYDeg;
  const cornerRotationYDeg = ((rawCornerRotationYDeg + 180) % 360 + 360) % 360 - 180;
  const cornerRadians = cornerRotationYDeg * Math.PI / 180;
  const cornerTangent = { x: Math.cos(cornerRadians), z: -Math.sin(cornerRadians) };
  const cornerAxis = {
    x: anchorTransform.xMm + anchorTangent.x * (anchorWindow.widthMm + gapMm / 2),
    z: anchorTransform.zMm + anchorTangent.z * (anchorWindow.widthMm + gapMm / 2)
  };
  const transform = corner
    ? {
        ...anchorTransform,
        xMm: cornerAxis.x + cornerTangent.x * gapMm / 2,
        zMm: cornerAxis.z + cornerTangent.z * gapMm / 2,
        rotationYDeg: cornerRotationYDeg
      }
    : {
        ...anchorTransform,
        xMm: input.direction === "right"
          ? anchorTransform.xMm + anchorTangent.x * (anchorWindow.widthMm + gapMm)
          : input.direction === "left"
            ? anchorTransform.xMm - anchorTangent.x *
              (input.createWindowCommand.widthMm + gapMm)
            : anchorTransform.xMm,
        yMm: input.direction === "bottom"
          ? anchorTransform.yMm + anchorWindow.heightMm + gapMm
          : input.direction === "top"
            ? anchorTransform.yMm - input.createWindowCommand.heightMm - gapMm
            : anchorTransform.yMm,
        zMm: input.direction === "right"
          ? anchorTransform.zMm + anchorTangent.z * (anchorWindow.widthMm + gapMm)
          : input.direction === "left"
            ? anchorTransform.zMm - anchorTangent.z *
              (input.createWindowCommand.widthMm + gapMm)
            : anchorTransform.zMm
      };
  const edges = connectionEdges(corner ? "right" : input.direction);
  const joint = {
    objectId: jointId,
    jointType,
    firstInstanceId: anchorInstanceId,
    firstEdge: edges.anchor,
    secondInstanceId: newInstanceId,
    secondEdge: edges.created,
    gapMm,
    factoryScope: catalogSelection?.defaultFactoryScope ?? "factory" as const,
    ...(corner ? {
      cornerConfiguration: {
        schemaVersion: "doormes-engineering-corner-joint.v1" as const,
        includedAngleDeg: cornerIncludedAngleDeg!,
        turnDirection: cornerTurnDirection!
      }
    } : {}),
    ...(catalogSelection ? { catalogSelection } : {})
  };

  const assemblyCommand = existingAssembly
    ? createAddFabricationAssemblyInstanceCommand({
        commandId: `${input.transactionId}:APPEND`,
        assemblyId,
        instance: {
          objectId: newInstanceId,
          windowId: input.createWindowCommand.windowId,
          transform
        },
        joint
      })
    : createFabricationAssemblyCommand({
        commandId: `${input.transactionId}:ASSEMBLE`,
        assemblyId,
        mark: `组合-${anchorWindow.mark}-${input.createWindowCommand.mark}`,
        instances: [
          {
            objectId: anchorInstanceId,
            windowId: anchorWindowId,
            transform: anchorTransform
          },
          {
            objectId: newInstanceId,
            windowId: input.createWindowCommand.windowId,
            transform
          }
        ],
        joints: [joint],
        openingClearance: { topMm: 10, rightMm: 12, bottomMm: 15, leftMm: 12 },
        installation: anchorWindow.installation ?? REFERENCE_WINDOW_INSTALLATION
      });

  return {
    commands: [
      input.createWindowCommand,
      ...(input.afterCreateCommands ?? []),
      assemblyCommand
    ],
    assemblyId,
    anchorWindowId,
    newWindowId: input.createWindowCommand.windowId,
    newInstanceId,
    jointId
  };
}

/** Maps relative placement to the two opposite physical connection ports. */
function connectionEdges(direction: FabricationConnectionDirection): {
  readonly anchor: EngineeringJoint["firstEdge"];
  readonly created: EngineeringJoint["secondEdge"];
} {
  switch (direction) {
    case "left": return { anchor: "left", created: "right" };
    case "right": return { anchor: "right", created: "left" };
    case "corner": return { anchor: "right", created: "left" };
    case "top": return { anchor: "top", created: "bottom" };
    case "bottom": return { anchor: "bottom", created: "top" };
  }
}

/** Collects formal IDs and returns a deterministic unused variant. */
function uniqueDesignObjectId(document: DesignDocument, preferred: string): DesignObjectId {
  const occupied = new Set<string>();
  for (const window of document.windows) {
    occupied.add(window.objectId);
    for (const cell of window.layout.cells) occupied.add(cell.objectId);
    for (const member of window.topology.members) occupied.add(member.objectId);
  }
  for (const assembly of document.assemblies ?? []) {
    occupied.add(assembly.objectId);
    for (const instance of assembly.instances) occupied.add(instance.objectId);
    for (const joint of assembly.joints) occupied.add(joint.objectId);
  }
  let candidate = preferred;
  let suffix = 2;
  while (occupied.has(candidate)) candidate = `${preferred}:${suffix++}`;
  return toDesignObjectId(candidate);
}

/**
 * Finds the first unused conventional `WIN-n` sequence in a document.
 *
 * Counting array length is unsafe after import/deletion because `WIN-2` may be
 * occupied while only one root remains. Both shells use this helper so PC and
 * mobile generate the same collision-free identity policy.
 *
 * @param document Current shared design snapshot.
 * @returns Positive integer whose `WIN-n` ID is not present.
 * @example Existing WIN-1 and WIN-3 produce sequence 2.
 * @since 0.10.47
 * @modified 2026-09-21 - Replaced length-based shell identity generation.
 */
export function nextAvailableWindowSequence(document: DesignDocument): number {
  const occupied = new Set(document.windows.map((window) => window.objectId));
  let sequence = 1;
  while (occupied.has(`WIN-${sequence}` as DesignObjectId)) sequence += 1;
  return sequence;
}

/** One non-technical target-product choice rendered by PC and mobile shells. */
export interface ZcsungSimulationWindowOption {
  readonly templateId: string;
  readonly label: string;
  readonly capabilityStatus: "available" | "partial";
  readonly defaultWidthMm: number;
  readonly defaultHeightMm: number;
}

/**
 * Lists only target-customer templates that the shared model can instantiate.
 *
 * Planned sliding, compound-motion, door and spatial products are deliberately
 * excluded from the ordinary designer picker. They remain visible in the
 * technical catalogue/plan but cannot tempt a user to create a false casement
 * approximation. PC and mobile consume this same projection.
 *
 * @returns Stable business labels and editable neutral default dimensions.
 * @example The first slice exposes 120内开内倒、100外开 and 95S双内开.
 * @since 0.10.77
 * @modified 2026-09-22 - Added shared target-template picker projection.
 */
export function listZcsungSimulationWindowOptions(): readonly ZcsungSimulationWindowOption[] {
  return listZcsungProductTemplates().flatMap((template) => {
    const preset = template.simulationPreset;
    const publicName = template.publicProductNames[0];
    if (!preset || !publicName) return [];
    return [{
      templateId: template.templateId,
      label: `${publicName}（公开参考模拟${template.capabilityStatus === "partial" ? "·部分" : ""}）`,
      capabilityStatus: template.capabilityStatus === "partial" ? "partial" : "available",
      defaultWidthMm: preset.defaultSizeMm.width,
      defaultHeightMm: preset.defaultSizeMm.height
    }];
  });
}

/** Result of translating one target-product choice into shared formal commands. */
export interface ZcsungSimulationWindowCreationPlan {
  readonly templateId: string;
  readonly productName: string;
  readonly createCommand: CreateRectangularWindowCommand;
  readonly openingCommand: SetWindowCellOpeningCommand;
  readonly commands: readonly [CreateRectangularWindowCommand, SetWindowCellOpeningCommand];
}

/**
 * Plans one target-customer simulation without mutating the session.
 *
 * Algorithm: fail-closed resolve the exact catalogue template, freeze its
 * provenance into the new window, create neutral rectangular geometry with the
 * user's final width/height, then apply the shared opening command. The result
 * is a two-command atomic transaction; connected-window planning inserts the
 * opening command between creation and assembly without duplicating geometry.
 *
 * @param input Exact template, stable IDs, optional business mark and final size.
 * @returns Ordered create/opening commands plus display identity.
 * @example A 1250×1500 override keeps the 120-system simulation provenance.
 * @since 0.10.77
 * @modified 2026-09-22 - Added PC/mobile shared target-template instantiation.
 */
export function planZcsungSimulationWindowCreation(input: {
  readonly templateId: string;
  readonly commandIdPrefix: string;
  readonly windowId: string;
  readonly instanceMark?: string;
  readonly widthMm?: number;
  readonly heightMm?: number;
}): ZcsungSimulationWindowCreationPlan {
  const { template, preset } = requireZcsungSimulationPreset(input.templateId);
  const productName = template.publicProductNames[0];
  if (!productName) {
    throw new Error(`ZCSUNG product template ${template.templateId} has no public product name.`);
  }
  const cellId = `${input.windowId}:CELL-1`;
  const trailingSequence = input.windowId.match(/(\d+)$/)?.[1];
  const businessMark = input.instanceMark?.trim() || `C${trailingSequence ?? "1"}`;
  const createCommand = createRectangularWindowCommand({
    commandId: `${input.commandIdPrefix}:CREATE`,
    windowId: input.windowId,
    mark: businessMark,
    widthMm: input.widthMm ?? preset.defaultSizeMm.width,
    heightMm: input.heightMm ?? preset.defaultSizeMm.height,
    cellId,
    profileSystemId: preset.neutralGeometry.profileSystemId,
    frameFaceMm: preset.neutralGeometry.frameFaceMm,
    sashFaceMm: preset.neutralGeometry.sashFaceMm,
    defaultHardwareSetId: preset.neutralGeometry.hardwareSetId,
    productTemplateSelection: createZcsungTemplateSelectionSnapshot(template)
  });
  const openingCommand = createSetWindowCellOpeningCommand({
    commandId: `${input.commandIdPrefix}:OPENING`,
    windowId: input.windowId,
    cellId,
    cellType: preset.cellType,
    opening: preset.opening,
    panelCount: preset.panelCount,
    mullionMode: preset.mullionMode,
    maximumAngleDegreesByMode: preset.maximumAngleDegreesByMode,
    hardwareSetId: preset.neutralGeometry.hardwareSetId
  });
  return {
    templateId: template.templateId,
    productName,
    createCommand,
    openingCommand,
    commands: [createCommand, openingCommand]
  };
}

/**
 * Resolves a cross-view selection to the product window that owns it.
 *
 * The search uses formal/resolved identities instead of DOM or Three.js parent
 * references. Consequently a selected frame, cell, sash, mullion, hardware,
 * reference wall or assembly instance identifies the same anchor on PC/mobile.
 * An assembly/joint itself falls back to its first product because no single
 * child is semantically preferred.
 *
 * @param document Current shared design snapshot.
 * @param objectId Selected stable identity from 2D, 3D or the object tree.
 * @returns Owning window ID, or undefined when the object is not window-owned.
 * @example Selecting `CELL-1::P1` resolves the window containing that panel.
 * @since 0.10.47
 * @modified 2026-09-21 - Shared combination-anchor selection across shells.
 */
export function resolveSelectedOwningWindowId(
  document: DesignDocument,
  objectId: DesignObjectId | undefined
): DesignObjectId | undefined {
  if (!objectId) return undefined;
  for (const assembly of document.assemblies ?? []) {
    const exactInstance = assembly.instances.find(
      (instance) => instance.objectId === objectId
    );
    if (exactInstance) return exactInstance.windowId;
    const assemblyWallId = createWindowInstallationWallObjectId(assembly.objectId);
    const assemblySurroundId = createWindowInstallationSurroundObjectId(assembly.objectId);
    if (
      objectId === assembly.objectId ||
      assembly.joints.some((joint) => joint.objectId === objectId) ||
      objectId === assemblyWallId || objectId.startsWith(`${assemblyWallId}.`) ||
      objectId === assemblySurroundId || objectId.startsWith(`${assemblySurroundId}.`)
    ) return assembly.instances[0]?.windowId;
  }
  return document.windows.find((window) => selectionBelongsToWindow(window, objectId))?.objectId;
}

/** Tests every renderer-neutral identity currently generated for one product. */
function selectionBelongsToWindow(window: WindowUnit, objectId: DesignObjectId): boolean {
  if (window.objectId === objectId) return true;
  const wallId = createWindowInstallationWallObjectId(window.objectId);
  const surroundId = createWindowInstallationSurroundObjectId(window.objectId);
  if (
    objectId === wallId || objectId.startsWith(`${wallId}.`) ||
    objectId === surroundId || objectId.startsWith(`${surroundId}.`)
  ) return true;
  const geometry = resolveWindowGeometry(window);
  return [
    ...geometry.frames.map((item) => item.objectId),
    ...geometry.cells.map((item) => item.objectId),
    ...geometry.openings.map((item) => `${item.objectId}::${item.panelId}`),
    ...geometry.members.map((item) => item.objectId),
    ...geometry.meetingMullions.map((item) => item.objectId),
    ...geometry.hardware.map((item) => item.hardwareId)
  ].some((candidate) => candidate === objectId);
}

/**
 * Creates a normalized device-independent rectangular-window command.
 *
 * Desktop forms and touch workflows call the same factory. ID generation is
 * injected by callers for deterministic tests and future audit correlation.
 *
 * @param input User intent already converted to millimetres.
 * @returns A shared command accepted by `DesignSession`.
 * @example `createRectangularWindowCommand({ commandId: "C1", ... })`.
 * @since 0.1.0
 * @modified 2026-09-20 - Added immutable glass-selection input and defensive copying.
 */
export function createRectangularWindowCommand(input: {
  commandId: string;
  windowId: string;
  mark: string;
  widthMm: number;
  heightMm: number;
  quantity?: number;
  frameFaceMm?: number;
  sashFaceMm?: number;
  sectionDimensions?: WindowSectionDimensions;
  installation?: WindowInstallation;
  visualConfiguration?: WindowVisualConfiguration;
  profileSystemId?: string;
  colorInside?: string;
  colorOutside?: string;
  defaultGlassTypeId?: string;
  defaultGlassSelection?: GlassCatalogSelectionSnapshot;
  installationSurroundSelection?: SurroundCatalogSelectionSnapshot;
  productTemplateSelection?: ProductTemplateSelectionSnapshot;
  defaultHardwareSetId?: string;
  designComponentRemarks?: WindowDesignComponentRemarks;
  cellId?: string;
  layout?: WindowGridLayout;
  geometryMode?: "grid" | "topology";
  topology?: WindowTopology;
}): CreateRectangularWindowCommand {
  return {
    type: "window.create-rectangular",
    commandId: input.commandId,
    windowId: toDesignObjectId(input.windowId),
    mark: input.mark,
    widthMm: input.widthMm,
    heightMm: input.heightMm,
    quantity: input.quantity,
    frameFaceMm: input.frameFaceMm,
    sashFaceMm: input.sashFaceMm,
    sectionDimensions: input.sectionDimensions,
    installation: input.installation
      ? cloneWindowInstallation(input.installation)
      : undefined,
    visualConfiguration: input.visualConfiguration
      ? cloneWindowVisualConfiguration(input.visualConfiguration)
      : undefined,
    profileSystemId: input.profileSystemId,
    colorInside: input.colorInside,
    colorOutside: input.colorOutside,
    defaultGlassTypeId: input.defaultGlassTypeId,
    defaultGlassSelection: input.defaultGlassSelection
      ? cloneGlassCatalogSelection(input.defaultGlassSelection)
      : undefined,
    installationSurroundSelection: input.installationSurroundSelection
      ? cloneSurroundCatalogSelection(input.installationSurroundSelection)
      : undefined,
    productTemplateSelection: input.productTemplateSelection
      ? cloneProductTemplateSelection(input.productTemplateSelection)
      : undefined,
    defaultHardwareSetId: input.defaultHardwareSetId,
    designComponentRemarks: input.designComponentRemarks
      ? { ...input.designComponentRemarks }
      : undefined,
    cellId: input.cellId ? toDesignObjectId(input.cellId) : undefined,
    layout: input.layout,
    geometryMode: input.geometryMode,
    topology: input.topology
  };
}

/**
 * Deep-copies a visual configuration before it enters command history.
 *
 * Algorithm: copy every appearance face, optional UV tuple, parametric map,
 * GLB LOD/hash/import snapshots, dimensions and pivot. This prevents a mutable
 * PC/mobile form from changing a command after it is queued or later redone.
 *
 * @param value Visual configuration owned by an interaction adapter.
 * @returns An immutable-compatible copy with no shared arrays or nested maps.
 * @example Editing a form's `lodAssets` after command creation is harmless.
 * @since 0.10.2
 * @modified 2026-09-18 - Added LOD hash and axis-calibration snapshot isolation.
 */
function cloneWindowVisualConfiguration(
  value: WindowVisualConfiguration
): WindowVisualConfiguration {
  /**
   * Copies one appearance and its optional UV pair.
   *
   * @param appearance Source appearance owned by the calling form.
   * @returns A detached appearance snapshot.
   * @since 0.10.2
   * @modified 2026-09-17 - Added nested appearance isolation.
   */
  const cloneAppearance = (
    appearance: WindowVisualConfiguration["appearance"]["glass"]
  ): WindowVisualConfiguration["appearance"]["glass"] => ({
    ...appearance,
    ...(appearance.uvScale ? { uvScale: { ...appearance.uvScale } } : {})
  });
  /**
   * Copies all three explicit faces of one frame-like surface.
   *
   * @param surface Source inside/outside/edge assignment.
   * @returns Three detached appearance snapshots.
   * @since 0.10.2
   * @modified 2026-09-17 - Added dual-face and edge command isolation.
   */
  const cloneSurface = (
    surface: WindowVisualConfiguration["appearance"]["frame"]
  ): WindowVisualConfiguration["appearance"]["frame"] => ({
    inside: cloneAppearance(surface.inside),
    outside: cloneAppearance(surface.outside),
    edge: cloneAppearance(surface.edge)
  });
  return {
    schemaVersion: "doormes-visual-config.v1",
    appearance: {
      schemaVersion: "doormes-appearance.v1",
      frame: cloneSurface(value.appearance.frame),
      sash: cloneSurface(value.appearance.sash),
      mullion: cloneSurface(value.appearance.mullion),
      flyingMullion: cloneSurface(value.appearance.flyingMullion),
      glass: cloneAppearance(value.appearance.glass),
      wall: cloneAppearance(value.appearance.wall),
      surroundOutside: cloneAppearance(value.appearance.surroundOutside),
      surroundInside: cloneAppearance(value.appearance.surroundInside),
      surroundLiner: cloneAppearance(value.appearance.surroundLiner),
      hardwareDefault: cloneAppearance(value.appearance.hardwareDefault)
    },
    hardwareModels: value.hardwareModels.map((assignment) => ({
      ...assignment,
      model: {
        ...assignment.model,
        geometry: assignment.model.geometry.kind === "parametric"
          ? {
              ...assignment.model.geometry,
              parameters: { ...assignment.model.geometry.parameters }
            }
          : {
              ...assignment.model.geometry,
              lodAssetIds: [...assignment.model.geometry.lodAssetIds],
              ...(assignment.model.geometry.lodAssets
                ? { lodAssets: assignment.model.geometry.lodAssets.map((asset) => ({ ...asset })) }
                : {}),
              ...(assignment.model.geometry.importConfiguration
                ? { importConfiguration: { ...assignment.model.geometry.importConfiguration } }
                : {})
            },
        dimensionsMm: { ...assignment.model.dimensionsMm },
        mount: {
          ...assignment.model.mount,
          pivotRatio: { ...assignment.model.mount.pivotRatio }
        },
        appearance: cloneAppearance(assignment.model.appearance)
      }
    }))
  };
}

/**
 * Deep-copies one exact glass catalog selection before command storage.
 *
 * @param value Business snapshot supplied by a catalog query or importer.
 * @returns Detached compatibility list, appearance and production identity.
 * @example Later mutation of a picker result cannot change an undoable command.
 * @since 0.10.29
 * @modified 2026-09-20 - Added immutable CAT-001 glass command input.
 */
function cloneGlassCatalogSelection(
  value: GlassCatalogSelectionSnapshot
): GlassCatalogSelectionSnapshot {
  return {
    ...value,
    compatibleProfileSystemIds: [...value.compatibleProfileSystemIds],
    appearance: {
      ...value.appearance,
      ...(value.appearance.uvScale
        ? { uvScale: { ...value.appearance.uvScale } }
        : {}),
      ...(value.appearance.productionMapping
        ? { productionMapping: { ...value.appearance.productionMapping } }
        : {})
    }
  };
}

/**
 * Deep-copies one installation snapshot before it enters command history.
 *
 * @param value Mutable form-owned installation value.
 * @returns Detached installation and edge list.
 * @example Adding a side to the form array after submit cannot change undo data.
 * @since 0.10.30
 * @modified 2026-09-20 - Shared installation isolation across command factories.
 */
function cloneWindowInstallation(value: WindowInstallation): WindowInstallation {
  return {
    ...value,
    surround: {
      ...value.surround,
      sides: [...value.surround.sides]
    }
  };
}

/** Detaches one versioned engineering-joint choice from picker/form ownership. */
function cloneEngineeringJointCatalogSelection(
  value: EngineeringJointCatalogSelectionSnapshot
): EngineeringJointCatalogSelectionSnapshot {
  return {
    ...value,
    allowedFactoryScopes: [...value.allowedFactoryScopes],
    ...(value.cornerCapability ? {
      cornerCapability: {
        ...value.cornerCapability,
        allowedTurnDirections: [...value.cornerCapability.allowedTurnDirections]
      }
    } : {})
  };
}

/**
 * Deep-copies one exact surround business selection for command isolation.
 *
 * Algorithm: detach compatibility and every nested appearance/UV/production
 * mapping so a catalog picker cannot mutate a queued command or redo snapshot.
 *
 * @param value Reviewed package/liner selection.
 * @returns Fully detached selection snapshot.
 * @since 0.10.30
 * @modified 2026-09-20 - Added MS-01 surround command isolation.
 */
function cloneSurroundCatalogSelection(
  value: SurroundCatalogSelectionSnapshot
): SurroundCatalogSelectionSnapshot {
  const cloneAppearance = (
    appearance: SurroundCatalogSelectionSnapshot["outsideAppearance"]
  ): SurroundCatalogSelectionSnapshot["outsideAppearance"] => ({
    ...appearance,
    ...(appearance.uvScale ? { uvScale: { ...appearance.uvScale } } : {}),
    ...(appearance.productionMapping
      ? { productionMapping: { ...appearance.productionMapping } }
      : {})
  });
  return {
    ...value,
    compatibleProfileSystemIds: [...value.compatibleProfileSystemIds],
    outsideAppearance: cloneAppearance(value.outsideAppearance),
    insideAppearance: cloneAppearance(value.insideAppearance),
    linerAppearance: cloneAppearance(value.linerAppearance)
  };
}

/**
 * Deep-copies a product-template provenance snapshot before command storage.
 *
 * Source URLs and assumptions are detached because catalogue adapters may
 * build them from mutable JSON arrays. A queued command must remain identical
 * across execute, undo and redo even if the picker refreshes its live result.
 *
 * @param value Exact selected product-template identity and evidence.
 * @returns A detached non-production provenance snapshot.
 * @example Mutating a catalogue result after submit does not alter the design.
 * @since 0.10.76
 * @modified 2026-09-22 - Added target-template command isolation.
 */
function cloneProductTemplateSelection(
  value: ProductTemplateSelectionSnapshot
): ProductTemplateSelectionSnapshot {
  return {
    ...value,
    officialSourceUrls: [...value.officialSourceUrls],
    assumptions: [...value.assumptions]
  };
}

/**
 * Creates the shared command used by future profile-section editors/importers.
 *
 * @param input Complete face and section snapshot in millimetres.
 * @returns One device-neutral, undoable design command.
 * @example A mobile form and desktop inspector submit the same custom 90mm section.
 * @since 0.9.1
 * @modified 2026-09-17 - Added the configurable profile geometry command factory.
 */
export function createUpdateWindowProfileGeometryCommand(input: {
  commandId: string;
  windowId: string;
  frameFaceMm: number;
  sashFaceMm: number;
  sectionDimensions: WindowSectionDimensions;
}): UpdateWindowProfileGeometryCommand {
  return {
    type: "window.update-profile-geometry",
    commandId: input.commandId,
    windowId: toDesignObjectId(input.windowId),
    frameFaceMm: input.frameFaceMm,
    sashFaceMm: input.sashFaceMm,
    sectionDimensions: { ...input.sectionDimensions }
  };
}

/**
 * Creates the shared command used by installation inspectors and importers.
 *
 * The nested surround and sides are copied so later form mutations cannot
 * alter a command already present in session history. Desktop and mobile use
 * this identical factory; only their input controls differ.
 *
 * @param input Complete persisted installation snapshot.
 * @returns One device-neutral and undoable update command.
 * @example Submit a 300mm wall with a custom +45mm room-side frame offset.
 * @since 0.9.7
 * @modified 2026-09-17 - Added formal installation command construction.
 */
export function createUpdateWindowInstallationCommand(input: {
  commandId: string;
  windowId: string;
  installation: WindowInstallation;
}): UpdateWindowInstallationCommand {
  return {
    type: "window.update-installation",
    commandId: input.commandId,
    windowId: toDesignObjectId(input.windowId),
    installation: cloneWindowInstallation(input.installation)
  };
}

/**
 * Creates the aggregate-owned installation update used by connected windows.
 *
 * The assembly keeps one reference wall and one surround package, so this
 * factory deliberately accepts an assembly ID rather than a child window ID.
 *
 * @since 0.10.93
 */
export function createUpdateFabricationAssemblyInstallationCommand(input: {
  commandId: string;
  assemblyId: string;
  installation: WindowInstallation;
}): UpdateFabricationAssemblyInstallationCommand {
  return {
    type: "assembly.update-installation",
    commandId: input.commandId,
    assemblyId: toDesignObjectId(input.assemblyId),
    installation: cloneWindowInstallation(input.installation)
  };
}

/**
 * Creates the shared appearance/hardware-model replacement command.
 *
 * PC and mobile editors submit the same complete snapshot. A defensive deep
 * copy freezes the user's intent until the domain validates it during execute.
 *
 * @param input Stable target ID plus complete visual configuration.
 * @returns One device-neutral and undoable visual update command.
 * @example Replace a handle model and outside finish in the same transaction.
 * @since 0.10.2
 * @modified 2026-09-17 - Added APPEAR-001/ASSET-001 application factory.
 */
export function createUpdateWindowVisualConfigurationCommand(input: {
  commandId: string;
  windowId: string;
  visualConfiguration: WindowVisualConfiguration;
}): UpdateWindowVisualConfigurationCommand {
  return {
    type: "window.update-visual-configuration",
    commandId: input.commandId,
    windowId: toDesignObjectId(input.windowId),
    visualConfiguration: cloneWindowVisualConfiguration(input.visualConfiguration)
  };
}

/**
 * Creates the shared exact-version glass business selection command.
 *
 * PC, mobile and future template application call this same factory. The whole
 * reviewed snapshot is copied into command history; the domain later checks
 * profile-system applicability and atomically updates render/BOM inputs.
 *
 * @param input Stable target and exact reviewed catalog snapshot.
 * @returns One device-neutral, undoable glass selection command.
 * @example Select `GL-TEMP-27@1.0.0` without exposing PBR values to the designer.
 * @since 0.10.29
 * @modified 2026-09-20 - Added the CAT-001 application command factory.
 */
export function createUpdateWindowGlassCatalogSelectionCommand(input: {
  commandId: string;
  windowId: string;
  selection: GlassCatalogSelectionSnapshot;
}): UpdateWindowGlassCatalogSelectionCommand {
  return {
    type: "window.update-glass-catalog-selection",
    commandId: input.commandId,
    windowId: toDesignObjectId(input.windowId),
    selection: cloneGlassCatalogSelection(input.selection)
  };
}

/**
 * Creates the shared exact-version package/liner selection command.
 *
 * PC, mobile and template application all submit the same business snapshot.
 * The domain then updates installation, render and manufacturing fields in one
 * revision; adapters never assign PBR or material-code fields independently.
 *
 * @param input Stable window target and reviewed catalog selection.
 * @returns Detached device-neutral command.
 * @example Select `SUR-STONE-GRAY-18@1.0.0` from a business-only picker.
 * @since 0.10.30
 * @modified 2026-09-20 - Added the MS-01 application command factory.
 */
export function createUpdateWindowSurroundCatalogSelectionCommand(input: {
  commandId: string;
  windowId: string;
  selection: SurroundCatalogSelectionSnapshot;
}): UpdateWindowSurroundCatalogSelectionCommand {
  return {
    type: "window.update-surround-catalog-selection",
    commandId: input.commandId,
    windowId: toDesignObjectId(input.windowId),
    selection: cloneSurroundCatalogSelection(input.selection)
  };
}

/**
 * Creates a normalized resize command for one stable window object.
 *
 * @param input Target ID and final manufacturing dimensions in millimetres.
 * @returns A shared resize command.
 * @example `createResizeWindowCommand({ windowId: "WIN-1", widthMm: 1400, ... })`.
 * @since 0.1.0
 * @modified 2026-09-17 - Added resize command construction.
 */
export function createResizeWindowCommand(input: {
  commandId: string;
  windowId: string | DesignObjectId;
  widthMm: number;
  heightMm: number;
}): ResizeWindowCommand {
  return {
    type: "window.resize",
    commandId: input.commandId,
    windowId: toDesignObjectId(input.windowId),
    widthMm: input.widthMm,
    heightMm: input.heightMm
  };
}

/**
 * Creates the shared command for one editable window business number.
 *
 * The factory deliberately accepts the complete user value without embedding
 * a product/template name. Domain validation owns trimming and uniqueness so
 * desktop, touch and future API adapters cannot diverge.
 *
 * @param input Stable window identity and complete replacement number.
 * @returns Device-independent update command.
 * @example `createUpdateWindowMarkCommand({ windowId: "WIN-2", mark: "C2" })`.
 * @since 0.10.82
 * @modified 2026-09-22 - Added business-number editing support.
 */
export function createUpdateWindowMarkCommand(input: {
  commandId: string;
  windowId: string | DesignObjectId;
  mark: string;
}): UpdateWindowMarkCommand {
  return {
    type: "window.update-mark",
    commandId: input.commandId,
    windowId: toDesignObjectId(input.windowId),
    mark: input.mark
  };
}

/** Creates one undoable replacement of the notes printed in the component table. */
export function createUpdateWindowDesignComponentRemarksCommand(input: {
  commandId: string;
  windowId: string | DesignObjectId;
  remarks: WindowDesignComponentRemarks;
}): UpdateWindowDesignComponentRemarksCommand {
  return {
    type: "window.update-design-component-remarks",
    commandId: input.commandId,
    windowId: toDesignObjectId(input.windowId),
    remarks: { ...input.remarks }
  };
}

/** Creates one undoable user-authored 2D text label. */
export function createDrawingTextLabelCommand(input: {
  commandId: string;
  label: DrawingTextLabel;
}): CreateDrawingTextLabelCommand {
  return {
    type: "drawing-text-label.create",
    commandId: input.commandId,
    label: { ...input.label }
  };
}

/** Replaces the complete editable snapshot of one 2D text label. */
export function createUpdateDrawingTextLabelCommand(input: {
  commandId: string;
  label: DrawingTextLabel;
}): UpdateDrawingTextLabelCommand {
  return {
    type: "drawing-text-label.update",
    commandId: input.commandId,
    label: { ...input.label }
  };
}

/** Deletes one 2D text label by stable object ID. */
export function createDeleteDrawingTextLabelCommand(input: {
  commandId: string;
  labelId: string | DesignObjectId;
}): DeleteDrawingTextLabelCommand {
  return {
    type: "drawing-text-label.delete",
    commandId: input.commandId,
    labelId: toDesignObjectId(input.labelId)
  };
}

/**
 * Creates the shared command that changes one cell's opening construction.
 *
 * Both desktop and touch inspectors call this factory; direction labels and
 * controls may differ, but the command contains only stable IDs and production
 * intent. The domain supplies complete panel/hinge metadata.
 *
 * @param input Target cell plus fixed or tilt-turn selection.
 * @returns A normalized command accepted by `DesignSession`.
 * @example Select `turn_tilt/right_in` to generate sash, glass and hardware BOM.
 * @since 0.4.9
 * @modified 2026-09-17 - Added the first shared opening-cell command factory.
 */
export function createSetWindowCellOpeningCommand(input: {
  commandId: string;
  windowId: string | DesignObjectId;
  cellId: string | DesignObjectId;
  cellType: "fixed_glass" | "turn_tilt" | "top_hung";
  opening:
    | "fixed"
    | "left_in"
    | "right_in"
    | "left_out"
    | "right_out"
    | "top_in"
    | "top_out";
  hardwareSetId?: string;
  panelCount?: 1 | 2;
  mullionMode?: "fixed_mullion" | "flying_mullion";
  maximumAngleDegreesByMode?: Readonly<{ primary: number; tilt?: number }>;
}): SetWindowCellOpeningCommand {
  return {
    type: "window.cell-set-opening",
    commandId: input.commandId,
    windowId: toDesignObjectId(input.windowId),
    cellId: toDesignObjectId(input.cellId),
    cellType: input.cellType,
    opening: input.opening,
    hardwareSetId: input.hardwareSetId,
    panelCount: input.panelCount,
    mullionMode: input.mullionMode,
    maximumAngleDegreesByMode: input.maximumAngleDegreesByMode
  };
}

/**
 * Creates the shared, undoable command for removing one manufactured window.
 *
 * The shell submits only the stable window identity. Assembly-instance/joint
 * cleanup is deliberately owned by the domain reducer so desktop and mobile
 * cannot disagree about whether a residual combination remains valid.
 *
 * @param input Audit command ID and selected window root.
 * @returns A normalized command accepted by `DesignSession`.
 * @example Deleting `WIN-2` from a pair dissolves its two-window assembly.
 * @since 0.10.51
 * @modified 2026-09-21 - Added the shared delete-window command factory.
 */
export function createDeleteWindowCommand(input: {
  commandId: string;
  windowId: string | DesignObjectId;
}): DeleteWindowCommand {
  return {
    type: "window.delete",
    commandId: input.commandId,
    windowId: toDesignObjectId(input.windowId)
  };
}

/**
 * Creates the shared final-position command for a flying-mullion drag or
 * numeric dimension edit.
 *
 * Pointer previews remain inside the shell; desktop and touch submit exactly
 * one millimetre value on completion, producing one undo/BOM invalidation.
 *
 * @param input Stable window/cell IDs and host-relative position in millimetres.
 * @returns A normalized command accepted by `DesignSession`.
 * @example `positionMm: 620` moves the meeting line 620mm from the cell's left.
 * @since 0.5.2
 * @modified 2026-09-17 - Added cross-shell flying-mullion movement factory.
 */
export function createMoveOpeningMeetingMullionCommand(input: {
  commandId: string;
  windowId: string | DesignObjectId;
  cellId: string | DesignObjectId;
  positionMm: number;
}): MoveOpeningMeetingMullionCommand {
  return {
    type: "window.opening-move-meeting-mullion",
    commandId: input.commandId,
    windowId: toDesignObjectId(input.windowId),
    cellId: toDesignObjectId(input.cellId),
    positionMm: input.positionMm
  };
}

/**
 * Creates a deterministic equal-grid split used by mouse and touch shells.
 *
 * The UI owns ID allocation while the domain owns structural validation. For
 * example, splitting one column across two rows passes two new cell IDs and
 * creates a single through mullion visible in SVG, Three.js and BOM results.
 *
 * @since 0.4.2
 * @modified 2026-09-17 - Added shared through-divider construction factory.
 */
export function createSplitWindowGridCommand(input: {
  commandId: string;
  windowId: string | DesignObjectId;
  axis: WindowGridAxis;
  index: number;
  partCount?: number;
  newCellIds: readonly (string | DesignObjectId)[];
}): SplitWindowGridCommand {
  return {
    type: "window.grid-split",
    commandId: input.commandId,
    windowId: toDesignObjectId(input.windowId),
    axis: input.axis,
    index: input.index,
    partCount: input.partCount ?? 2,
    newCellIds: input.newCellIds.map((id) => toDesignObjectId(id))
  };
}

/**
 * Creates the prototype-compatible remove-last-track command.
 *
 * @example A desktop delete-column button and mobile action sheet both call
 * this factory with `axis: "column"`.
 * @since 0.4.2
 * @modified 2026-09-17 - Added cross-shell grid removal factory.
 */
export function createRemoveLastWindowGridTrackCommand(input: {
  commandId: string;
  windowId: string | DesignObjectId;
  axis: WindowGridAxis;
}): RemoveLastWindowGridTrackCommand {
  return {
    type: "window.grid-remove-last",
    commandId: input.commandId,
    windowId: toDesignObjectId(input.windowId),
    axis: input.axis
  };
}

/**
 * Creates one final divider-position command after a drag gesture completes.
 *
 * Pointer-move previews remain presentation state; only pointer-up calls this
 * factory, so one gesture creates one undo step and one BOM invalidation.
 *
 * @since 0.4.2
 * @modified 2026-09-17 - Added shared divider-drag command factory.
 */
export function createMoveWindowGridDividerCommand(input: {
  commandId: string;
  windowId: string | DesignObjectId;
  axis: WindowGridAxis;
  index: number;
  positionMm: number;
}): MoveWindowGridDividerCommand {
  return {
    type: "window.grid-move-divider",
    commandId: input.commandId,
    windowId: toDesignObjectId(input.windowId),
    axis: input.axis,
    index: input.index,
    positionMm: input.positionMm
  };
}

/**
 * Creates a shared equalize-grid command for desktop and touch tool surfaces.
 *
 * @param input Audit command ID and target window ID.
 * @returns A normalized command that preserves cells while resetting weights.
 * @example `createEqualizeWindowGridCommand({ commandId: "C1", windowId: "W1" })`.
 * @since 0.4.3
 * @modified 2026-09-17 - Added cross-shell whole-grid equalization.
 */
export function createEqualizeWindowGridCommand(input: {
  commandId: string;
  windowId: string | DesignObjectId;
}): EqualizeWindowGridCommand {
  return {
    type: "window.grid-equalize",
    commandId: input.commandId,
    windowId: toDesignObjectId(input.windowId)
  };
}

/**
 * Creates a selected-divider merge command used by the context menu.
 *
 * @param input Target axis and one-based rendered boundary index.
 * @returns A command distinct from the prototype's remove-last-track action.
 * @example Boundary 1 merges the first and second columns.
 * @since 0.4.3
 * @modified 2026-09-17 - Added selected through-divider deletion semantics.
 */
export function createMergeWindowGridDividerCommand(input: {
  commandId: string;
  windowId: string | DesignObjectId;
  axis: WindowGridAxis;
  index: number;
}): MergeWindowGridDividerCommand {
  return {
    type: "window.grid-merge-divider",
    commandId: input.commandId,
    windowId: toDesignObjectId(input.windowId),
    axis: input.axis,
    index: input.index
  };
}

/**
 * Creates the default full-span local mullion used by the prototype tool.
 *
 * Advanced inspectors may override position/span/connection fields later, but
 * both desktop and mobile start with the same 50% local member semantics.
 *
 * @example Add a vertical member at the centre of `CELL-1`.
 * @since 0.4.3
 * @modified 2026-09-17 - Added shared local-mullion creation.
 */
export function createAddWindowTopologyMemberCommand(input: {
  commandId: string;
  windowId: string | DesignObjectId;
  memberId: string | DesignObjectId;
  hostRegionId: string | DesignObjectId;
  orientation: "vertical" | "horizontal";
  profileId?: string;
  positionRatio?: number;
  span?: Readonly<{ startRatio: number; endRatio: number }>;
  throughMode?: "local" | "continuous";
  connectionStart?: "butt" | "through";
  connectionEnd?: "butt" | "through";
  note?: string;
}): AddWindowTopologyMemberCommand {
  return {
    type: "window.topology-member-add",
    commandId: input.commandId,
    windowId: toDesignObjectId(input.windowId),
    member: {
      objectId: toDesignObjectId(input.memberId),
      role: "mullion",
      orientation: input.orientation,
      hostRegionId: toDesignObjectId(input.hostRegionId),
      positionRatio: input.positionRatio ?? 0.5,
      span: input.span ?? { startRatio: 0, endRatio: 1 },
      profileId: input.profileId?.trim() ?? "",
      throughMode: input.throughMode ?? "local",
      connectionStart: input.connectionStart ?? "butt",
      connectionEnd: input.connectionEnd ?? "butt",
      note: input.note ?? ""
    }
  };
}

/**
 * Creates one final local-mullion position command after edit or drag.
 *
 * The position is expressed in host-cell millimetres; domain logic applies
 * clearance and converts it to a resize-stable ratio.
 *
 * @example Move `MEMBER-V-1` to 700mm inside its host cell.
 * @since 0.4.3
 * @modified 2026-09-17 - Added shared local-member position editing.
 */
export function createMoveWindowTopologyMemberCommand(input: {
  commandId: string;
  windowId: string | DesignObjectId;
  memberId: string | DesignObjectId;
  positionMm: number;
}): MoveWindowTopologyMemberCommand {
  return {
    type: "window.topology-member-move",
    commandId: input.commandId,
    windowId: toDesignObjectId(input.windowId),
    memberId: toDesignObjectId(input.memberId),
    positionMm: input.positionMm
  };
}

/**
 * Creates a complete, replayable topology-member property command.
 *
 * The form adapter passes host-cell millimetres and explicit enum values. No
 * ratio conversion or construction rule is duplicated here; those decisions
 * remain in the domain reducer used by desktop, mobile, import and automation.
 *
 * @param input Final values from a member inspector or future import adapter.
 * @returns A device-independent command with branded stable identifiers.
 * @example Updating a 1200mm-hosted member may use position 600 and span 100..900.
 * @since 0.4.5
 * @modified 2026-09-17 - Added the shared full-property command factory.
 */
export function createUpdateWindowTopologyMemberCommand(input: {
  commandId: string;
  windowId: string | DesignObjectId;
  memberId: string | DesignObjectId;
  orientation: "vertical" | "horizontal";
  positionMm: number;
  spanStartMm: number;
  spanEndMm: number;
  profileId: string;
  throughMode: "local" | "continuous";
  connectionStart: "butt" | "through";
  connectionEnd: "butt" | "through";
  note: string;
}): UpdateWindowTopologyMemberCommand {
  return {
    type: "window.topology-member-update",
    commandId: input.commandId,
    windowId: toDesignObjectId(input.windowId),
    memberId: toDesignObjectId(input.memberId),
    orientation: input.orientation,
    positionMm: input.positionMm,
    spanStartMm: input.spanStartMm,
    spanEndMm: input.spanEndMm,
    profileId: input.profileId,
    throughMode: input.throughMode,
    connectionStart: input.connectionStart,
    connectionEnd: input.connectionEnd,
    note: input.note
  };
}

/**
 * Creates an undoable local-mullion removal command.
 *
 * @param input Parent window and stable explicit member IDs.
 * @returns A command that removes no host cell or implicit grid divider.
 * @example Remove `MEMBER-H-2` selected from either renderer or the object tree.
 * @since 0.4.3
 * @modified 2026-09-17 - Added cross-view topology-member deletion.
 */
export function createRemoveWindowTopologyMemberCommand(input: {
  commandId: string;
  windowId: string | DesignObjectId;
  memberId: string | DesignObjectId;
}): RemoveWindowTopologyMemberCommand {
  return {
    type: "window.topology-member-remove",
    commandId: input.commandId,
    windowId: toDesignObjectId(input.windowId),
    memberId: toDesignObjectId(input.memberId)
  };
}
