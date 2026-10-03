import { describe, expect, it } from "vitest";
import type { WallPlan } from "@doormes/contracts";
import { applyDesignCommand, createEmptyDesign, toDesignObjectId } from "./index";

const wallPlan: WallPlan = {
  segments: [
    {
      objectId: toDesignObjectId("WALL-1"),
      startMm: { x: 0, y: 0 }, endMm: { x: 3000, y: 0 },
      thicknessMm: 200, heightMm: 3000
    },
    {
      objectId: toDesignObjectId("WALL-2"),
      startMm: { x: 3000, y: 0 }, endMm: { x: 3000, y: 2400 },
      thicknessMm: 200, heightMm: 3000,
      connectsToWallId: toDesignObjectId("WALL-1")
    }
  ],
  openings: [{
    objectId: toDesignObjectId("OPENING-1"), wallId: toDesignObjectId("WALL-1"),
    kind: "window", offsetMm: 500, widthMm: 1200, sillMm: 900, heightMm: 1500
  }]
};

describe("authored wall plan", () => {
  it("stores connected corner walls and an opening as one immutable revision", () => {
    const original = createEmptyDesign("DESIGN-WALL");
    const result = applyDesignCommand(original, {
      type: "wall-plan.update", commandId: "CMD-WALL-1", wallPlan
    });
    expect(original.wallPlan?.segments).toEqual([]);
    expect(result.document.revision).toBe(1);
    expect(result.document.wallPlan).toEqual(wallPlan);
    expect(result.changes.createdObjectIds).toEqual([
      toDesignObjectId("WALL-1"), toDesignObjectId("WALL-2"), toDesignObjectId("OPENING-1")
    ]);
  });

  it("rejects disconnected joins, overlapping openings and duplicate IDs", () => {
    const document = createEmptyDesign("DESIGN-WALL");
    const update = (plan: WallPlan) => applyDesignCommand(document, {
      type: "wall-plan.update", commandId: "CMD-BAD", wallPlan: plan
    });
    expect(() => update({ ...wallPlan, segments: [wallPlan.segments[0]!, {
      ...wallPlan.segments[1]!, startMm: { x: 2900, y: 0 }
    }] })).toThrow("终点");
    expect(() => update({ ...wallPlan, openings: [...wallPlan.openings, {
      ...wallPlan.openings[0]!, objectId: toDesignObjectId("OPENING-2"), offsetMm: 1000
    }] })).toThrow("重叠");
    expect(() => update({ ...wallPlan, openings: [{
      ...wallPlan.openings[0]!, objectId: toDesignObjectId("WALL-1")
    }] })).toThrow("重复");
  });
});
