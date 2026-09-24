import { createRectangularWindowCommand } from "@doormes/application";
import type {
  CreateRectangularWindowCommand,
  DesignDocument,
  WindowVisualConfiguration
} from "@doormes/contracts";
import {
  createOpeningPanelKey,
  openingAngleDegreesToProgress,
  openingProgressToAngleDegrees,
  resolveOpeningMaximumAngleDegrees
} from "@doormes/opening-kinematics";

/**
 * Device-neutral intent produced after a shell interprets mouse, keyboard,
 * finger or pen interaction.
 *
 * @example A desktop form and a mobile bottom sheet both create this intent.
 * @since 0.1.0
 * @modified 2026-09-17 - Added first cross-device creation intent.
 */
export interface CreateWindowIntent {
  readonly commandId: string;
  readonly windowId: string;
  readonly mark: string;
  readonly widthMm: number;
  readonly heightMm: number;
}

/**
 * Converts a device-neutral intent into the shared application command.
 *
 * Interaction adapters may differ in gesture recognition but converge here.
 * Keeping command construction in this package prevents layout shells from
 * acquiring their own model mutation rules.
 *
 * @param intent Final user intent in manufacturing units.
 * @returns One command accepted by the shared `DesignSession`.
 * @example `createWindowCommandFromIntent({ widthMm: 1200, ... })`.
 * @since 0.1.0
 * @modified 2026-09-17 - Added device-independent command translation.
 */
export function createWindowCommandFromIntent(
  intent: CreateWindowIntent
): CreateRectangularWindowCommand {
  return createRectangularWindowCommand(intent);
}

/** Runtime-only description of which formal windows currently have visual drafts. */
export interface WindowVisualPreviewState {
  readonly revision: number;
  readonly windowIds: readonly string[];
}

/** Listener used by 2D/3D projections to repaint one transient visual draft. */
export type WindowVisualPreviewListener = (state: WindowVisualPreviewState) => void;

/**
 * Owns temporary material/model edits without touching design history or BOM.
 *
 * The editor places only fully normalized `WindowVisualConfiguration` values
 * here. Renderers call `project(document)` to receive a shallow design
 * projection with those values substituted; persistence and calculation keep
 * reading the original `DesignSession.document`. Any formal design change
 * clears drafts so a preview can never survive against a different base model.
 *
 * @example Change frame color, call `set`, and repaint 2D/3D while revision stays fixed.
 * @since 0.10.22
 * @modified 2026-09-18 - Added isolated appearance/model parameter preview.
 */
export class WindowVisualPreviewStore {
  readonly #listeners = new Set<WindowVisualPreviewListener>();
  readonly #configurations = new Map<string, WindowVisualConfiguration>();
  #revision = 0;

  /** Returns a small immutable-compatible summary for UI status rendering. */
  get state(): WindowVisualPreviewState {
    return {
      revision: this.#revision,
      windowIds: [...this.#configurations.keys()]
    };
  }

  /** Replaces one window's normalized visual draft and notifies both projections. */
  set(windowId: string, configuration: WindowVisualConfiguration): void {
    if (!windowId.trim()) throw new Error("Visual preview requires a window ID.");
    this.#configurations.set(windowId, structuredClone(configuration));
    this.#notify();
  }

  /** Clears one draft, normally after submit, selection change or cancellation. */
  clear(windowId: string): void {
    if (!this.#configurations.delete(windowId)) return;
    this.#notify();
  }

  /** Clears every draft when the formal document or active shell changes. */
  clearAll(notify = true): void {
    if (this.#configurations.size === 0) return;
    this.#configurations.clear();
    if (notify) this.#notify();
  }

  /**
   * Reconciles against a new formal snapshot by discarding all old-base drafts.
   * @param _document Documents are accepted for the same lifecycle API as opening preview.
   */
  synchronize(_document: DesignDocument, notify = true): void {
    this.clearAll(notify);
  }

  /** Returns a renderer-only document projection; the source document is untouched. */
  project(document: DesignDocument): DesignDocument {
    if (this.#configurations.size === 0) return document;
    return {
      ...document,
      windows: document.windows.map((window) => {
        const configuration = this.#configurations.get(window.objectId);
        return configuration ? { ...window, visualConfiguration: configuration } : window;
      })
    };
  }

  /** Registers a listener and immediately publishes the current summary. */
  subscribe(listener: WindowVisualPreviewListener): () => void {
    this.#listeners.add(listener);
    listener(this.state);
    return () => this.#listeners.delete(listener);
  }

  /** Advances only runtime revision; no domain revision or command is generated. */
  #notify(): void {
    this.#revision += 1;
    const state = this.state;
    for (const listener of [...this.#listeners]) listener(state);
  }
}

/** Stable identifier used to address one movable leaf in runtime preview state. */
export type OpeningPreviewPanelKey = string;

/**
 * Design-derived metadata required by preview controls and sequence policies.
 *
 * This value contains no mutable runtime state. `configuredOpenPercent` is the
 * persisted prototype-compatible target, while the store keeps the current
 * instantaneous progress separately for each panel.
 *
 * @example A flying-mullion P2 definition has `operationOrder: 0`.
 * @since 0.8.0
 * @modified 2026-09-17 - Added the device-neutral preview panel descriptor.
 */
export interface OpeningPreviewPanelDefinition {
  readonly key: OpeningPreviewPanelKey;
  readonly objectId: string;
  readonly windowId: string;
  readonly panelId: "P1" | "P2";
  readonly panelRole: "primary" | "secondary" | "independent";
  readonly operationOrder: 0 | 1;
  readonly operationMode: "ordered" | "independent";
  readonly configuredOpenPercent: number;
  readonly motionMode: "primary" | "tilt";
  readonly supportsTilt: boolean;
  /** Physical full-open limits used by degree-facing controls and projections. */
  readonly maximumAngleDegreesByMode: Readonly<{
    primary: number;
    tilt?: number;
  }>;
}

/**
 * Resolves the active physical angle limit for one design-derived panel.
 *
 * @param definition Immutable panel/mechanism metadata.
 * @param motionMode Current preview path.
 * @returns Maximum angle in degrees for that path.
 * @example A tilt-turn tilt path resolves to about 18.3°.
 * @since 0.9.0
 * @modified 2026-09-17 - Added a common PC/mobile angle-control adapter.
 */
export function resolveOpeningPreviewMaximumAngleDegrees(
  definition: OpeningPreviewPanelDefinition,
  motionMode: "primary" | "tilt"
): number {
  if (motionMode === "tilt") {
    const tiltMaximum = definition.maximumAngleDegreesByMode.tilt;
    if (tiltMaximum === undefined) {
      throw new RangeError(`Opening preview panel does not support a tilt angle: ${definition.key}`);
    }
    return tiltMaximum;
  }
  return definition.maximumAngleDegreesByMode.primary;
}

/** Converts one panel's normalized runtime progress into its current degrees. */
export function resolveOpeningPreviewAngleDegrees(
  definition: OpeningPreviewPanelDefinition,
  state: OpeningPreviewState
): number {
  const mode = state.panelMotionMode[definition.key] ?? definition.motionMode;
  return openingProgressToAngleDegrees(
    state.panelProgressPercent[definition.key] ?? 0,
    resolveOpeningPreviewMaximumAngleDegrees(definition, mode)
  );
}

/** Runtime-only panel progress and selection shared by PC and mobile shells. */
export interface OpeningPreviewState {
  readonly revision: number;
  readonly panelProgressPercent: Readonly<Record<OpeningPreviewPanelKey, number>>;
  readonly panelMotionMode: Readonly<Record<OpeningPreviewPanelKey, "primary" | "tilt">>;
  readonly selectedPanelKeys: readonly OpeningPreviewPanelKey[];
  readonly playback: "idle" | "opening" | "closing" | "paused";
}

/** Listener notified after a preview-only state transition. */
export type OpeningPreviewListener = (state: OpeningPreviewState) => void;

/**
 * Extracts movable-panel preview definitions from an immutable design snapshot.
 *
 * Algorithm: walk windows and layout cells, skip fixed glazing, then expand
 * each opening assembly into one definition per stable panel. The legacy
 * `openPercent` field is normalized only as a configured target; it is never
 * changed here. Flying mullions default to ordered operation, while fixed
 * mullions explicitly remain independent.
 *
 * @param document Shared design snapshot.
 * @returns Definitions in document/window/cell/panel order.
 * @example A double sash produces `CELL-1::P1` and `CELL-1::P2`.
 * @since 0.8.0
 * @modified 2026-09-17 - Added formal design-to-preview projection.
 */
export function collectOpeningPreviewPanelDefinitions(
  document: DesignDocument
): readonly OpeningPreviewPanelDefinition[] {
  const result: OpeningPreviewPanelDefinition[] = [];
  for (const window of document.windows) {
    for (const cell of window.layout.cells) {
      if (cell.type === "fixed_glass") continue;
      const assembly = cell.openingAssembly;
      const operationMode = cell.type === "turn_tilt"
        ? cell.openingAssembly.operationMode ?? (cell.openingAssembly.mullionMode === "fixed_mullion"
            ? "independent"
            : "ordered")
        : "ordered";
      for (const panel of assembly.panels) {
        const hingeEdge = "hingeEdge" in panel ? panel.hingeEdge : panel.hingeSide;
        const mechanism = cell.type === "top_hung" ? "top_hung" : "tilt_turn";
        const maximumAngleDegreesByMode = {
          primary: assembly.maximumAngleDegreesByMode?.primary ??
            resolveOpeningMaximumAngleDegrees({
              mechanism,
              motionMode: "primary",
              hingeEdge,
              openPlane: assembly.openPlane
            }),
          ...(cell.type === "turn_tilt"
            ? {
                tilt: assembly.maximumAngleDegreesByMode?.tilt ??
                  resolveOpeningMaximumAngleDegrees({
                    mechanism,
                    motionMode: "tilt",
                    hingeEdge,
                    openPlane: assembly.openPlane
                  })
              }
            : {})
        };
        result.push({
          key: createOpeningPanelKey(cell.objectId, panel.id),
          objectId: cell.objectId,
          windowId: window.objectId,
          panelId: panel.id,
          panelRole: panel.role,
          operationOrder: panel.operationOrder,
          operationMode,
          configuredOpenPercent: clampOpeningPreviewProgress(assembly.openPercent),
          motionMode:
            cell.type === "turn_tilt" && assembly.operationPriority === "tilt_first"
              ? "tilt"
              : "primary",
          // Outward side-hung products share the panel/hinge graph but do not
          // expose the inward tilt mode. Keeping this capability in the shared
          // definition prevents PC/mobile controls from diverging.
          // @since 0.10.51
          // @modified 2026-09-21 - Disable tilt for outward-opening sashes.
          supportsTilt: cell.type === "turn_tilt" && assembly.openPlane === "in",
          maximumAngleDegreesByMode
        });
      }
    }
  }
  return result;
}

/**
 * Owns transient opening preview state without mutating design or BOM inputs.
 *
 * PC and mobile controls call this same store after translating their mouse,
 * keyboard or touch gestures. A newly discovered panel starts at the design's
 * configured target so current migrated 3D previews remain visually compatible.
 * Existing progress survives ordinary document revisions; removed panels and
 * stale selections are discarded during `synchronize`.
 *
 * @example Select P1, call `setSelectedProgress(50)`, and render the returned
 * progress map while the design document revision remains unchanged.
 * @since 0.8.0
 * @modified 2026-09-17 - Added MOT-002 preview/design isolation foundation.
 */
export class OpeningPreviewStore {
  readonly #listeners = new Set<OpeningPreviewListener>();
  #definitions = new Map<OpeningPreviewPanelKey, OpeningPreviewPanelDefinition>();
  #state: OpeningPreviewState = {
    revision: 0,
    panelProgressPercent: {},
    panelMotionMode: {},
    selectedPanelKeys: [],
    playback: "idle"
  };

  constructor(document: DesignDocument) {
    this.synchronize(document, false);
  }

  /** Returns the latest immutable-compatible runtime snapshot. */
  get state(): OpeningPreviewState {
    return this.#state;
  }

  /** Returns definitions in deterministic document order. */
  get panelDefinitions(): readonly OpeningPreviewPanelDefinition[] {
    return [...this.#definitions.values()];
  }

  /**
   * Reconciles runtime state after a design revision.
   *
   * Existing panel progress is retained by stable key. New panels start at the
   * configured target and deleted panels disappear. This operation never writes
   * back to `document` and therefore never enters undo, persistence or BOM.
   */
  synchronize(document: DesignDocument, notify = true): void {
    const hadDefinitions = this.#definitions.size > 0;
    const nextDefinitions = new Map(
      collectOpeningPreviewPanelDefinitions(document).map((definition) => [
        definition.key,
        definition
      ])
    );
    const nextProgress: Record<string, number> = {};
    const nextMotionMode: Record<string, "primary" | "tilt"> = {};
    for (const definition of nextDefinitions.values()) {
      nextProgress[definition.key] =
        this.#state.panelProgressPercent[definition.key] ??
        definition.configuredOpenPercent;
      nextMotionMode[definition.key] =
        this.#state.panelMotionMode[definition.key] ?? definition.motionMode;
    }
    const retainedSelection = this.#state.selectedPanelKeys.filter((key) =>
      nextDefinitions.has(key)
    );
    /**
     * Keep the angle toolbar truthful on first load.
     *
     * The persisted design may intentionally open every leaf at a non-zero
     * target (the migrated double-sash fixture uses 80%). Leaving the transient
     * control selection empty made the canvas show 72 degrees while the toolbar
     * showed a disabled 0 degrees. Select every newly discovered leaf only when
     * transitioning from no preview definitions; later user-cleared selections
     * remain cleared across ordinary document revisions.
     *
     * @example Loading two 80% leaves checks P1/P2 and reports their real 72°.
     * @since 0.10.40
     * @modified 2026-09-20 - Aligned initial controls with rendered sash state.
     */
    const nextSelection = !hadDefinitions && retainedSelection.length === 0
      ? [...nextDefinitions.keys()]
      : retainedSelection;
    this.#definitions = nextDefinitions;
    this.#replaceState({
      panelProgressPercent: nextProgress,
      panelMotionMode: nextMotionMode,
      selectedPanelKeys: nextSelection,
      playback: "idle"
    }, notify);
  }

  /** Selects exactly one panel, or clears selection when omitted. */
  selectOnly(key?: OpeningPreviewPanelKey): void {
    if (key !== undefined) this.#requireDefinition(key);
    this.#replaceState({ selectedPanelKeys: key ? [key] : [] });
  }

  /** Adds/removes one panel from the multi-selection set. */
  toggleSelected(key: OpeningPreviewPanelKey): void {
    this.#requireDefinition(key);
    const selected = new Set(this.#state.selectedPanelKeys);
    if (selected.has(key)) selected.delete(key);
    else selected.add(key);
    this.#replaceState({ selectedPanelKeys: [...selected] });
  }

  /** Sets one panel's instantaneous preview progress in the inclusive 0..100 range. */
  setPanelProgress(key: OpeningPreviewPanelKey, progressPercent: number): void {
    this.#requireDefinition(key);
    this.#setProgressForKeys([key], progressPercent);
  }

  /**
   * Applies one controller animation frame without stopping active playback.
   *
   * The controller may update multiple independent panels atomically so PC and
   * mobile renderers never observe half of one logical frame.
   * @param progressPercentByPanelKey Bounded progress updates by stable key.
   * @example Two fixed-mullion leaves can advance together in one store revision.
   * @since 0.8.2
   * @modified 2026-09-17 - Added controller-owned atomic preview frames.
   */
  applyPlaybackFrame(
    progressPercentByPanelKey: Readonly<Record<OpeningPreviewPanelKey, number>>
  ): void {
    const entries = Object.entries(progressPercentByPanelKey);
    if (entries.length === 0) return;
    const next = { ...this.#state.panelProgressPercent };
    for (const [key, value] of entries) {
      this.#requireDefinition(key);
      next[key] = clampOpeningPreviewProgress(value);
    }
    this.#replaceState({ panelProgressPercent: next });
  }

  /** Sets all selected panels to one instantaneous preview progress. */
  setSelectedProgress(progressPercent: number): void {
    this.#setProgressForKeys(this.#state.selectedPanelKeys, progressPercent);
  }

  /**
   * Sets selected rotating panels to one real physical angle in degrees.
   *
   * Each panel converts the same requested angle against its current mode's
   * own mechanical limit. This is intentionally not one shared percentage:
   * 9° means 9° for both a side-hung sash and an 18° tilt sash even though
   * their normalized animation progress differs.
   *
   * @param angleDegrees Requested instantaneous physical rotation.
   * @example 9° becomes about 13.5% primary progress but 49.1% tilt progress.
   * @since 0.9.0
   * @modified 2026-09-17 - Replaced percentage-facing manual controls.
   */
  setSelectedAngleDegrees(angleDegrees: number): void {
    if (this.#state.selectedPanelKeys.length === 0) return;
    const next = { ...this.#state.panelProgressPercent };
    for (const key of this.#state.selectedPanelKeys) {
      const definition = this.#requireDefinition(key);
      const mode = this.#state.panelMotionMode[key] ?? definition.motionMode;
      const maximum = resolveOpeningPreviewMaximumAngleDegrees(definition, mode);
      next[key] = openingAngleDegreesToProgress(angleDegrees, maximum);
    }
    this.#replaceState({ panelProgressPercent: next, playback: "idle" });
  }

  /**
   * Selects a motion path while preserving the current physical angle.
   *
   * A normalized percentage cannot be copied between a 90° primary path and
   * an 18.3° tilt path. The old angle is therefore converted to the new mode's
   * progress and clamped at its mechanical limit.
   */
  setSelectedMotionMode(motionMode: "primary" | "tilt"): void {
    if (this.#state.selectedPanelKeys.length === 0) return;
    const next = { ...this.#state.panelMotionMode };
    const nextProgress = { ...this.#state.panelProgressPercent };
    for (const key of this.#state.selectedPanelKeys) {
      const definition = this.#requireDefinition(key);
      if (motionMode === "tilt" && !definition.supportsTilt) {
        throw new RangeError(`Opening preview panel does not support tilt mode: ${key}`);
      }
      const previousMode = this.#state.panelMotionMode[key] ?? definition.motionMode;
      const previousAngle = openingProgressToAngleDegrees(
        this.#state.panelProgressPercent[key] ?? 0,
        resolveOpeningPreviewMaximumAngleDegrees(definition, previousMode)
      );
      next[key] = motionMode;
      nextProgress[key] = openingAngleDegreesToProgress(
        previousAngle,
        resolveOpeningPreviewMaximumAngleDegrees(definition, motionMode)
      );
    }
    this.#replaceState({ panelMotionMode: next, panelProgressPercent: nextProgress });
  }

  /** Opens selected panels fully; configured targets only define initial preview. */
  openSelected(): void {
    this.#setProgressForKeys(this.#state.selectedPanelKeys, 100);
  }

  /** Closes selected panels without changing their configured targets. */
  closeSelected(): void {
    this.#setProgressForKeys(this.#state.selectedPanelKeys, 0);
  }

  /** Opens every panel fully, matching the prototype's “全部开启” action. */
  openAll(): void {
    this.#setProgressForKeys([...this.#definitions.keys()], 100);
  }

  /** Closes every panel while preserving persisted design configuration. */
  closeAll(): void {
    this.#setProgressForKeys([...this.#definitions.keys()], 0);
  }

  /** Updates playback intent; animation timing remains owned by the shell/controller. */
  setPlayback(playback: OpeningPreviewState["playback"]): void {
    this.#replaceState({ playback });
  }

  /** Subscribes to preview changes and returns an idempotent unsubscriber. */
  subscribe(listener: OpeningPreviewListener): () => void {
    this.#listeners.add(listener);
    return () => this.#listeners.delete(listener);
  }

  /** Applies the same bounded progress to a key set in one store revision. */
  #setProgressForKeys(
    keys: readonly OpeningPreviewPanelKey[],
    progressPercent: number
  ): void {
    if (keys.length === 0) return;
    const next = { ...this.#state.panelProgressPercent };
    const progress = clampOpeningPreviewProgress(progressPercent);
    for (const key of keys) {
      this.#requireDefinition(key);
      next[key] = progress;
    }
    this.#replaceState({ panelProgressPercent: next, playback: "idle" });
  }

  /** Rejects stale shell keys before they can silently create orphan state. */
  #requireDefinition(key: OpeningPreviewPanelKey): OpeningPreviewPanelDefinition {
    const definition = this.#definitions.get(key);
    if (!definition) throw new RangeError(`Unknown opening preview panel: ${key}`);
    return definition;
  }

  /** Creates a fresh snapshot, increments only runtime revision and notifies listeners. */
  #replaceState(
    change: Partial<Omit<OpeningPreviewState, "revision">>,
    notify = true
  ): void {
    this.#state = {
      ...this.#state,
      ...change,
      revision: this.#state.revision + 1
    };
    if (notify) {
      for (const listener of this.#listeners) listener(this.#state);
    }
  }
}

/** Bounds shell/runtime input and prevents NaN from reaching render transforms. */
function clampOpeningPreviewProgress(value: number): number {
  if (!Number.isFinite(value)) return 0;
  return Math.min(100, Math.max(0, value));
}

export * from "./opening-preview-controller";
export * from "./canvas-interaction";
