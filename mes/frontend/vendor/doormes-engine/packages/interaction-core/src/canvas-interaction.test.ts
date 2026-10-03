import { describe, expect, it } from "vitest";
import {
  CANVAS_PRIMARY_MOVEMENT_THRESHOLD_PX,
  resolveCanvasInteractionHit,
  resolveCanvasInteractionIntent
} from "./canvas-interaction";

describe("2D canvas interaction intent resolver", () => {
  it("uses handle, dimension, object and canvas hit priority", () => {
    expect(resolveCanvasInteractionHit({
      dragHandleObjectId: "DIVIDER-1",
      dragHandleSupportsContextMenu: true,
      dimensionObjectId: "WIN-1",
      dimensionKind: "width",
      objectId: "CELL-1"
    })).toEqual({
      kind: "drag-handle",
      objectId: "DIVIDER-1",
      supportsContextMenu: true
    });
    expect(resolveCanvasInteractionHit({
      dimensionObjectId: "WIN-1",
      dimensionKind: "width",
      objectId: "CELL-1"
    })).toEqual({ kind: "dimension", objectId: "WIN-1", dimensionKind: "width" });
    expect(resolveCanvasInteractionHit({ objectId: "CELL-1" }))
      .toEqual({ kind: "object", objectId: "CELL-1" });
    expect(resolveCanvasInteractionHit({})).toEqual({ kind: "canvas" });
  });

  it("keeps small handle movement as a pending click and starts drag at the threshold", () => {
    const hit = resolveCanvasInteractionHit({ dragHandleObjectId: "MULLION-1" });
    expect(resolveCanvasInteractionIntent({
      activation: "move",
      hit,
      movementPx: CANVAS_PRIMARY_MOVEMENT_THRESHOLD_PX - 0.1
    })).toEqual({ kind: "none" });
    expect(resolveCanvasInteractionIntent({
      activation: "move",
      hit,
      movementPx: CANVAS_PRIMARY_MOVEMENT_THRESHOLD_PX
    })).toEqual({ kind: "drag", objectId: "MULLION-1" });
  });

  it("selects the resolved target and clears selection from canvas whitespace", () => {
    const object = resolveCanvasInteractionHit({ objectId: "SASH-1" });
    const canvas = resolveCanvasInteractionHit({});
    expect(resolveCanvasInteractionIntent({ activation: "single", hit: object })).toEqual({
      kind: "select",
      objectId: "SASH-1"
    });
    expect(resolveCanvasInteractionIntent({ activation: "single", hit: canvas }))
      .toEqual({ kind: "select" });
  });

  it("starts pan from canvas, object or dimension after meaningful movement", () => {
    const canvas = resolveCanvasInteractionHit({});
    const object = resolveCanvasInteractionHit({ objectId: "CELL-1" });
    const dimension = resolveCanvasInteractionHit({
      dimensionObjectId: "CELL-1",
      dimensionKind: "cell-width"
    });
    expect(resolveCanvasInteractionIntent({ activation: "move", hit: canvas, movementPx: 8 }))
      .toEqual({ kind: "pan" });
    expect(resolveCanvasInteractionIntent({ activation: "move", hit: object, movementPx: 8 }))
      .toEqual({ kind: "pan" });
    expect(resolveCanvasInteractionIntent({ activation: "move", hit: dimension, movementPx: 8 }))
      .toEqual({ kind: "pan" });
    // The DOM adapter uses a zero threshold after activation so every later
    // pointer sample remains part of the same smooth pan, even near the origin.
    expect(resolveCanvasInteractionIntent({
      activation: "move",
      hit: object,
      movementPx: 0,
      movementThresholdPx: 0
    })).toEqual({ kind: "pan" });
  });

  it("separates dimension editing from ordinary object property opening", () => {
    const dimension = resolveCanvasInteractionHit({
      dimensionObjectId: "WIN-1",
      dimensionKind: "height"
    });
    const object = resolveCanvasInteractionHit({ objectId: "CELL-1" });
    expect(resolveCanvasInteractionIntent({ activation: "double", hit: dimension })).toEqual({
      kind: "edit-dimension",
      objectId: "WIN-1",
      dimensionKind: "height"
    });
    expect(resolveCanvasInteractionIntent({ activation: "double", hit: object })).toEqual({
      kind: "open-properties",
      objectId: "CELL-1"
    });
  });

  it("allows context menus on supported handles and ordinary design objects", () => {
    const supported = resolveCanvasInteractionHit({
      dragHandleObjectId: "DIVIDER-1",
      dragHandleSupportsContextMenu: true
    });
    const unsupported = resolveCanvasInteractionHit({ dragHandleObjectId: "MEETING-1" });
    const object = resolveCanvasInteractionHit({ objectId: "CELL-1" });
    expect(resolveCanvasInteractionIntent({ activation: "context", hit: supported }))
      .toEqual({ kind: "context-menu", objectId: "DIVIDER-1" });
    expect(resolveCanvasInteractionIntent({ activation: "context", hit: unsupported }))
      .toEqual({ kind: "none" });
    expect(resolveCanvasInteractionIntent({ activation: "context", hit: object }))
      .toEqual({ kind: "context-menu", objectId: "CELL-1" });
  });
});
