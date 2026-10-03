import { describe, expect, it } from "vitest";
import { createEmptyDesign, toDesignObjectId } from "@doormes/domain";
import {
  createDeleteWindowCommand, createRectangularWindowCommand,
  createResizeWindowCommand, DesignSession
} from "./index";

describe("optional wall plan placement", () => {
  it("creates a new product from an opening and places it in one undo step", () => {
    const session = new DesignSession(createEmptyDesign("DESIGN-WALL-CREATE"));
    session.execute({ type: "wall-plan.update", commandId: "CMD-WALL",
      wallPlan: {
        segments: [{ objectId: toDesignObjectId("WALL-1"),
          startMm: { x: 0, y: 0 }, endMm: { x: 3000, y: 0 },
          thicknessMm: 200, heightMm: 3000 }],
        openings: [{ objectId: toDesignObjectId("OPENING-1"),
          wallId: toDesignObjectId("WALL-1"), kind: "window",
          offsetMm: 500, widthMm: 1220, sillMm: 900, heightMm: 1520 }]
      }
    });
    session.executeTransaction([
      createRectangularWindowCommand({
        commandId: "CMD-CREATE", windowId: "WIN-1", mark: "C1",
        widthMm: 1200, heightMm: 1500
      }),
      { type: "wall-plan.update", commandId: "CMD-PLACE",
        wallPlan: {
          segments: session.document.wallPlan!.segments,
          openings: [{ ...session.document.wallPlan!.openings[0]!,
            placedWindowId: toDesignObjectId("WIN-1") }]
        }
      }
    ], "TX-CREATE-PLACE");
    expect(session.document.windows).toHaveLength(1);
    expect(session.document.wallPlan?.openings[0]?.placedWindowId).toBe("WIN-1");
    session.undo();
    expect(session.document.windows).toHaveLength(0);
    expect(session.document.wallPlan?.openings[0]?.placedWindowId).toBeUndefined();
  });

  it("places an existing window, protects opening fit, and shares undo history", () => {
    const session = new DesignSession(createEmptyDesign("DESIGN-WALL-PLACEMENT"));
    session.execute(createRectangularWindowCommand({
      commandId: "CMD-WINDOW", windowId: "WIN-1", mark: "C1",
      widthMm: 1200, heightMm: 1500
    }));
    const plan = {
      segments: [{ objectId: toDesignObjectId("WALL-1"),
        startMm: { x: 0, y: 0 }, endMm: { x: 3000, y: 0 },
        thicknessMm: 200, heightMm: 3000 }],
      openings: [{ objectId: toDesignObjectId("OPENING-1"),
        wallId: toDesignObjectId("WALL-1"), kind: "window" as const,
        offsetMm: 500, widthMm: 1250, sillMm: 900, heightMm: 1550,
        placedWindowId: toDesignObjectId("WIN-1") }]
    };
    session.execute({ type: "wall-plan.update", commandId: "CMD-PLACE", wallPlan: plan });
    expect(session.document.wallPlan?.openings[0]?.placedWindowId).toBe("WIN-1");
    expect(() => session.execute(createResizeWindowCommand({
      commandId: "CMD-TOO-WIDE", windowId: "WIN-1", widthMm: 1300, heightMm: 1500
    }))).toThrow("no longer fits opening");
    expect(() => session.execute(createDeleteWindowCommand({
      commandId: "CMD-DELETE", windowId: "WIN-1"
    }))).toThrow("移除");
    expect(session.undo()).toBe(true);
    expect(session.document.wallPlan?.openings).toEqual([]);
    expect(session.redo()).toBe(true);
    expect(session.document.wallPlan?.openings[0]?.placedWindowId).toBe("WIN-1");
  });
});
