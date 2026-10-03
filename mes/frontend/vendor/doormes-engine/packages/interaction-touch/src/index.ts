import type { CreateRectangularWindowCommand } from "@doormes/contracts";
import { createWindowCommandFromIntent } from "@doormes/interaction-core";

/**
 * Represents final values from a touch-first mobile creation workflow.
 *
 * Gesture coordinates are intentionally absent because they are transient UI
 * state; only final manufacturing dimensions enter the shared command.
 *
 * @example A numeric mobile sheet submits 1200 × 1500 millimetres.
 * @since 0.1.0
 * @modified 2026-09-17 - Added first touch input contract.
 */
export interface TouchCreateWindowInput {
  readonly commandId: string;
  readonly windowId: string;
  readonly mark: string;
  readonly widthMm: number;
  readonly heightMm: number;
}

/**
 * Translates a completed touch workflow into the shared create-window command.
 *
 * @param input Final values collected by the mobile layout shell.
 * @returns The same command shape produced by the desktop adapter.
 * @example A tap on “创建” sends one command, not pointer-move samples.
 * @since 0.1.0
 * @modified 2026-09-17 - Added touch-to-core input translation.
 */
export function touchCreateWindowCommand(
  input: TouchCreateWindowInput
): CreateRectangularWindowCommand {
  return createWindowCommandFromIntent(input);
}

/** Identifies whether a touch began on navigable canvas or an editable object. */
export type TouchCanvasTarget = "canvas" | "object";

/**
 * Incremental presentation change emitted by the touch gesture controller.
 *
 * Deltas and anchors use browser client pixels so the DOM adapter can convert
 * them through the current SVG viewBox. A factor of one means pan only.
 *
 * @example Two fingers moving apart may emit `{ zoomFactor: 1.2, ... }`.
 * @since 0.4.5
 * @modified 2026-09-17 - Added device-independent pan/pinch output.
 */
export interface TouchCanvasGestureUpdate {
  readonly mode: "pan" | "pinch";
  readonly panDeltaX: number;
  readonly panDeltaY: number;
  readonly anchorClientX: number;
  readonly anchorClientY: number;
  readonly zoomFactor: number;
}

/** Internal immutable sample retained for each active touch pointer. */
interface TouchPointerSample {
  readonly clientX: number;
  readonly clientY: number;
  readonly target: TouchCanvasTarget;
}

/** Returns midpoint and distance for the first two ordered touch samples. */
function twoPointerMetrics(
  first: TouchPointerSample,
  second: TouchPointerSample
): { readonly x: number; readonly y: number; readonly distance: number } {
  return {
    x: (first.clientX + second.clientX) / 2,
    y: (first.clientY + second.clientY) / 2,
    distance: Math.hypot(second.clientX - first.clientX, second.clientY - first.clientY)
  };
}

/**
 * Reduces raw touch pointers to deterministic canvas pan/pinch changes.
 *
 * Algorithm: retain samples by pointer ID; ignore the whole gesture whenever
 * any active pointer started on a design object; one canvas pointer emits its
 * incremental movement; two canvas pointers emit midpoint movement plus the
 * distance ratio. When one finger lifts, the survivor's last sample becomes
 * the next pan origin, preventing the common pinch-to-pan jump.
 *
 * The controller owns no DOM, timers, design objects or manufacturing values,
 * so both shells can reuse it and unit tests can replay gestures precisely.
 *
 * @example Down(1), down(2), move(2) emits one pinch update; up(2) resumes pan.
 * @since 0.4.5
 * @modified 2026-09-17 - Added mobile single-pan and two-finger pinch state.
 */
export class TouchCanvasGestureController {
  readonly #pointers = new Map<number, TouchPointerSample>();

  /** Number of active pointers, exposed so adapters can clear visual state. */
  get activePointerCount(): number {
    return this.#pointers.size;
  }

  /** Registers a touch origin without emitting movement. */
  pointerDown(
    pointerId: number,
    clientX: number,
    clientY: number,
    target: TouchCanvasTarget
  ): void {
    this.#pointers.set(pointerId, { clientX, clientY, target });
  }

  /**
   * Updates one active pointer and returns the incremental view mutation.
   * Three-or-more touches and gestures starting on objects are ignored rather
   * than guessed, keeping member/divider dragging isolated from navigation.
   */
  pointerMove(
    pointerId: number,
    clientX: number,
    clientY: number
  ): TouchCanvasGestureUpdate | undefined {
    const previous = this.#pointers.get(pointerId);
    if (!previous) return undefined;
    const previousSamples = [...this.#pointers.values()];
    this.#pointers.set(pointerId, { ...previous, clientX, clientY });
    const currentSamples = [...this.#pointers.values()];
    if (
      currentSamples.length === 0 ||
      currentSamples.length > 2 ||
      currentSamples.some((sample) => sample.target !== "canvas")
    ) return undefined;
    if (currentSamples.length === 1) {
      return {
        mode: "pan",
        panDeltaX: clientX - previous.clientX,
        panDeltaY: clientY - previous.clientY,
        anchorClientX: clientX,
        anchorClientY: clientY,
        zoomFactor: 1
      };
    }
    const previousFirst = previousSamples[0];
    const previousSecond = previousSamples[1];
    const currentFirst = currentSamples[0];
    const currentSecond = currentSamples[1];
    if (!previousFirst || !previousSecond || !currentFirst || !currentSecond) return undefined;
    const before = twoPointerMetrics(previousFirst, previousSecond);
    const after = twoPointerMetrics(currentFirst, currentSecond);
    return {
      mode: "pinch",
      panDeltaX: after.x - before.x,
      panDeltaY: after.y - before.y,
      anchorClientX: after.x,
      anchorClientY: after.y,
      zoomFactor: before.distance > 0 ? after.distance / before.distance : 1
    };
  }

  /** Ends one pointer while retaining any survivor as the next pan origin. */
  pointerUp(pointerId: number): void {
    this.#pointers.delete(pointerId);
  }

  /** Clears interrupted pointers when a surface unmounts or loses capture. */
  reset(): void {
    this.#pointers.clear();
  }
}
