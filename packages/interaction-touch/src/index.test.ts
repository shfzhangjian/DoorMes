import { describe, expect, it } from "vitest";
import { TouchCanvasGestureController } from "./index";

describe("TouchCanvasGestureController", () => {
  it("emits incremental one-finger canvas pan", () => {
    const gesture = new TouchCanvasGestureController();
    gesture.pointerDown(1, 100, 80, "canvas");
    expect(gesture.pointerMove(1, 118, 71)).toMatchObject({
      mode: "pan",
      panDeltaX: 18,
      panDeltaY: -9,
      zoomFactor: 1
    });
  });

  it("emits midpoint pan and distance-ratio zoom for two fingers", () => {
    const gesture = new TouchCanvasGestureController();
    gesture.pointerDown(1, 100, 100, "canvas");
    gesture.pointerDown(2, 200, 100, "canvas");
    const update = gesture.pointerMove(2, 220, 120);
    expect(update).toMatchObject({ mode: "pinch", panDeltaX: 10, panDeltaY: 10 });
    expect(update?.zoomFactor).toBeCloseTo(Math.hypot(120, 20) / 100, 8);
    expect(update?.anchorClientX).toBe(160);
    expect(update?.anchorClientY).toBe(110);
  });

  it("resumes pan from the survivor without a transition jump", () => {
    const gesture = new TouchCanvasGestureController();
    gesture.pointerDown(1, 10, 10, "canvas");
    gesture.pointerDown(2, 30, 10, "canvas");
    gesture.pointerMove(1, 8, 10);
    gesture.pointerUp(2);
    expect(gesture.pointerMove(1, 9, 13)).toMatchObject({
      mode: "pan",
      panDeltaX: 1,
      panDeltaY: 3
    });
  });

  it("does not navigate when any active touch started on a design object", () => {
    const gesture = new TouchCanvasGestureController();
    gesture.pointerDown(1, 10, 10, "object");
    gesture.pointerDown(2, 30, 10, "canvas");
    expect(gesture.pointerMove(2, 40, 10)).toBeUndefined();
    gesture.pointerUp(1);
    expect(gesture.pointerMove(2, 42, 12)).toMatchObject({
      mode: "pan",
      panDeltaX: 2,
      panDeltaY: 2
    });
  });
});
