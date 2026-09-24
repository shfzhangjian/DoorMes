import type {
  OpeningPreviewPanelDefinition,
  OpeningPreviewPanelKey,
  OpeningPreviewState
} from "./index";

/** Direction of one user-requested preview transition. */
export type OpeningPreviewTransitionDirection = "opening" | "closing";

/** One panel interpolation inside an ordered or parallel transition phase. */
export interface OpeningPreviewTransitionItem {
  readonly panelKey: OpeningPreviewPanelKey;
  readonly fromProgressPercent: number;
  readonly toProgressPercent: number;
}

/** Panels in one phase move together; later phases wait for earlier phases. */
export interface OpeningPreviewTransitionPhase {
  readonly phaseIndex: number;
  readonly items: readonly OpeningPreviewTransitionItem[];
}

/** Complete deterministic transition plan produced before animation starts. */
export interface OpeningPreviewTransitionPlan {
  readonly direction: OpeningPreviewTransitionDirection;
  readonly phases: readonly OpeningPreviewTransitionPhase[];
}

/** Minimal store boundary required by the DOM/renderer-independent controller. */
export interface OpeningPreviewControllerStore {
  readonly state: OpeningPreviewState;
  readonly panelDefinitions: readonly OpeningPreviewPanelDefinition[];
  applyPlaybackFrame(progressByPanelKey: Readonly<Record<string, number>>): void;
  setPlayback(playback: OpeningPreviewState["playback"]): void;
}

/**
 * Plans opening/closing phases with physical order and interlock expansion.
 *
 * Algorithm: group requested panels by opening assembly. Independent leaves
 * share phase zero. For ordered flying-mullion assemblies, opening a later leaf
 * automatically includes earlier prerequisites; closing an earlier leaf first
 * includes every later dependent leaf. Opening sorts ascending by operation
 * order and closing sorts descending. Assemblies may advance in parallel at
 * the same local phase index.
 *
 * @param definitions Static design-derived panel metadata.
 * @param state Current runtime progress.
 * @param requestedKeys User-selected target leaves.
 * @param direction Whether the request opens or closes.
 * @returns A pure transition plan; empty phases mean no movement is necessary.
 * @example Closing flying-mullion P2 while P1 is open yields P1 then P2.
 * @since 0.8.2
 * @modified 2026-09-17 - Added shared sequence and interlock planning.
 */
export function planOpeningPreviewTransition(
  definitions: readonly OpeningPreviewPanelDefinition[],
  state: OpeningPreviewState,
  requestedKeys: readonly OpeningPreviewPanelKey[],
  direction: OpeningPreviewTransitionDirection
): OpeningPreviewTransitionPlan {
  const definitionsByKey = new Map(definitions.map((definition) => [definition.key, definition]));
  const requested = new Set(requestedKeys);
  for (const key of requested) {
    if (!definitionsByKey.has(key)) throw new RangeError(`Unknown opening preview panel: ${key}`);
  }
  const definitionsByAssembly = new Map<string, OpeningPreviewPanelDefinition[]>();
  for (const definition of definitions) {
    const siblings = definitionsByAssembly.get(definition.objectId) ?? [];
    siblings.push(definition);
    definitionsByAssembly.set(definition.objectId, siblings);
  }

  const phaseItems: OpeningPreviewTransitionItem[][] = [];
  for (const siblings of definitionsByAssembly.values()) {
    const directlyRequested = siblings.filter((definition) => requested.has(definition.key));
    if (directlyRequested.length === 0) continue;
    const ordered = siblings.some((definition) => definition.operationMode === "ordered");
    let moving: OpeningPreviewPanelDefinition[];
    if (!ordered) {
      moving = directlyRequested;
    } else if (direction === "opening") {
      const lastRequiredOrder = Math.max(
        ...directlyRequested.map((definition) => definition.operationOrder)
      );
      moving = siblings
        .filter((definition) => definition.operationOrder <= lastRequiredOrder)
        .sort((left, right) => left.operationOrder - right.operationOrder);
    } else {
      const firstRequiredOrder = Math.min(
        ...directlyRequested.map((definition) => definition.operationOrder)
      );
      moving = siblings
        .filter((definition) => definition.operationOrder >= firstRequiredOrder)
        .sort((left, right) => right.operationOrder - left.operationOrder);
    }

    const necessary = moving.filter((definition) => {
      const current = state.panelProgressPercent[definition.key] ?? 0;
      const target = direction === "opening" ? 100 : 0;
      return Math.abs(current - target) > 1e-9;
    });
    necessary.forEach((definition, localIndex) => {
      const phaseIndex = ordered ? localIndex : 0;
      const items = phaseItems[phaseIndex] ?? [];
      items.push({
        panelKey: definition.key,
        fromProgressPercent: state.panelProgressPercent[definition.key] ?? 0,
        toProgressPercent:
          direction === "opening" ? 100 : 0
      });
      phaseItems[phaseIndex] = items;
    });
  }
  return {
    direction,
    phases: phaseItems.map((items, phaseIndex) => ({ phaseIndex, items }))
  };
}

/**
 * Advances planned opening phases using elapsed milliseconds, without DOM or RAF.
 *
 * Shells own the clock: desktop can call `advance` from requestAnimationFrame,
 * tests can pass exact durations, and mobile may lower frame frequency without
 * changing results. Pause/resume retains phase and elapsed time. Starting a new
 * request replaces the previous plan deterministically.
 *
 * @example `controller.openSelected(); controller.advance(300)` advances half
 * of a 600ms first phase.
 * @since 0.8.2
 * @modified 2026-09-17 - Added shared playback/pause controller.
 */
export class OpeningPreviewController {
  readonly #store: OpeningPreviewControllerStore;
  readonly #phaseDurationMs: number;
  #plan?: OpeningPreviewTransitionPlan;
  #phaseIndex = 0;
  #phaseElapsedMs = 0;
  #pausedDirection?: OpeningPreviewTransitionDirection;
  #cycleRequestedKeys?: readonly OpeningPreviewPanelKey[];

  constructor(store: OpeningPreviewControllerStore, phaseDurationMs = 600) {
    if (!Number.isFinite(phaseDurationMs) || phaseDurationMs <= 0) {
      throw new RangeError("Opening preview phase duration must be positive.");
    }
    this.#store = store;
    this.#phaseDurationMs = phaseDurationMs;
  }

  /** Starts or replaces playback for the current selected panel set. */
  openSelected(): OpeningPreviewTransitionPlan {
    return this.#start("opening", this.#store.state.selectedPanelKeys);
  }

  /** Starts reverse-order closing for the current selected panel set. */
  closeSelected(): OpeningPreviewTransitionPlan {
    return this.#start("closing", this.#store.state.selectedPanelKeys);
  }

  /** Starts opening all known panels, respecting each assembly's interlocks. */
  openAll(): OpeningPreviewTransitionPlan {
    return this.#start("opening", this.#store.panelDefinitions.map((item) => item.key));
  }

  /** Starts reverse-order closing for every known panel. */
  closeAll(): OpeningPreviewTransitionPlan {
    return this.#start("closing", this.#store.panelDefinitions.map((item) => item.key));
  }

  /**
   * Reproduces the prototype demonstration: reset selected/interlocked panels,
   * open them in operation order, then close them in reverse order.
   *
   * @returns The opening half of the cycle, or an empty plan without selection.
   * @example Selecting a flying-mullion secondary leaf animates P2→P1→P1→P2.
   * @since 0.8.2
   * @modified 2026-09-17 - Added exact prototype selected-cycle playback.
   */
  playSelectedCycle(): OpeningPreviewTransitionPlan {
    const requestedKeys = [...this.#store.state.selectedPanelKeys];
    if (requestedKeys.length === 0) {
      this.cancel();
      return { direction: "opening", phases: [] };
    }
    const zeroProgress = Object.fromEntries(
      this.#store.panelDefinitions.map((definition) => [definition.key, 0])
    );
    const zeroState: OpeningPreviewState = {
      ...this.#store.state,
      panelProgressPercent: zeroProgress
    };
    const openingPlan = planOpeningPreviewTransition(
      this.#store.panelDefinitions,
      zeroState,
      requestedKeys,
      "opening"
    );
    const resetFrame: Record<string, number> = {};
    for (const phase of openingPlan.phases) {
      for (const item of phase.items) resetFrame[item.panelKey] = 0;
    }
    const requestedDefinitions = this.#store.panelDefinitions.filter((definition) =>
      requestedKeys.includes(definition.key)
    );
    for (const requested of requestedDefinitions) {
      if (requested.operationMode !== "ordered") continue;
      for (const sibling of this.#store.panelDefinitions) {
        if (sibling.objectId === requested.objectId) resetFrame[sibling.key] = 0;
      }
    }
    this.#store.applyPlaybackFrame(resetFrame);
    this.#cycleRequestedKeys = Object.keys(resetFrame);
    return this.#start("opening", requestedKeys, true);
  }

  /** Pauses an active transition without discarding its exact progress. */
  pause(): void {
    if (!this.#plan || this.#store.state.playback === "paused") return;
    this.#pausedDirection = this.#plan.direction;
    this.#store.setPlayback("paused");
  }

  /** Resumes a paused transition from the same phase and elapsed time. */
  resume(): void {
    if (!this.#plan || !this.#pausedDirection) return;
    this.#store.setPlayback(this.#pausedDirection);
    this.#pausedDirection = undefined;
    this.#cycleRequestedKeys = undefined;
  }

  /** Cancels playback while keeping the current instantaneous panel poses. */
  cancel(): void {
    this.#plan = undefined;
    this.#phaseIndex = 0;
    this.#phaseElapsedMs = 0;
    this.#pausedDirection = undefined;
    this.#store.setPlayback("idle");
  }

  /**
   * Advances one or more phases and applies one atomic store frame per phase.
   * Extra elapsed time carries into the next phase, making the result independent
   * of frame rate and background-tab scheduling.
   */
  advance(deltaMilliseconds: number): void {
    if (
      !this.#plan ||
      this.#store.state.playback === "paused" ||
      this.#store.state.playback === "idle"
    ) return;
    if (!Number.isFinite(deltaMilliseconds) || deltaMilliseconds < 0) {
      throw new RangeError("Opening preview delta time must be finite and non-negative.");
    }
    let remaining = deltaMilliseconds;
    while (this.#plan && this.#phaseIndex < this.#plan.phases.length) {
      const phase = this.#plan.phases[this.#phaseIndex];
      if (!phase) break;
      const available = this.#phaseDurationMs - this.#phaseElapsedMs;
      const consumed = Math.min(available, remaining);
      this.#phaseElapsedMs += consumed;
      remaining -= consumed;
      const ratio = Math.min(1, this.#phaseElapsedMs / this.#phaseDurationMs);
      const frame: Record<string, number> = {};
      for (const item of phase.items) {
        frame[item.panelKey] =
          item.fromProgressPercent +
          (item.toProgressPercent - item.fromProgressPercent) * ratio;
      }
      this.#store.applyPlaybackFrame(frame);
      if (ratio < 1) break;
      this.#phaseIndex += 1;
      this.#phaseElapsedMs = 0;
      if (this.#phaseIndex >= this.#plan.phases.length) {
        if (this.#plan.direction === "opening" && this.#cycleRequestedKeys) {
          const cycleKeys = this.#cycleRequestedKeys;
          this.#start("closing", cycleKeys, true);
          if (!this.#plan) {
            this.cancel();
            break;
          }
          if (remaining <= 0) break;
          continue;
        }
        this.cancel();
        break;
      }
      if (remaining <= 0) break;
    }
  }

  /** Builds a fresh plan and updates playback state only when work exists. */
  #start(
    direction: OpeningPreviewTransitionDirection,
    requestedKeys: readonly OpeningPreviewPanelKey[],
    preserveCycle = false
  ): OpeningPreviewTransitionPlan {
    const plan = planOpeningPreviewTransition(
      this.#store.panelDefinitions,
      this.#store.state,
      requestedKeys,
      direction
    );
    this.#plan = plan.phases.length > 0 ? plan : undefined;
    this.#phaseIndex = 0;
    this.#phaseElapsedMs = 0;
    this.#pausedDirection = undefined;
    if (!preserveCycle) this.#cycleRequestedKeys = undefined;
    this.#store.setPlayback(this.#plan ? direction : "idle");
    return plan;
  }
}
