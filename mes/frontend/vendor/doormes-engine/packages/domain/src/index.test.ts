import { describe, expect, it } from "vitest";
import { createSlidingOpeningAssembly } from "./index";

describe("createSlidingOpeningAssembly", () => {
  it("normalizes an ordinary two-panel two-track sliding window", () => {
    const assembly = createSlidingOpeningAssembly({
      trackCount: 2,
      overlapMm: 35,
      panels: [
        { trackIndex: 0, movable: true, travelDirection: "right" },
        { trackIndex: 1, movable: false }
      ]
    });

    expect(assembly).toMatchObject({
      mechanism: "sliding",
      panelCount: 2,
      activePanelCount: 1,
      trackCount: 2,
      stackSide: "right",
      overlapMm: 35,
      openPercent: 80,
      operationSequence: ["P1"]
    });
    expect(assembly.panels).toEqual([
      {
        id: "P1",
        label: "1号扇",
        role: "active",
        movable: true,
        trackIndex: 0,
        closedPositionIndex: 0,
        operationOrder: 0,
        travelDirection: "right"
      },
      {
        id: "P2",
        label: "2号扇",
        role: "passive",
        movable: false,
        trackIndex: 1,
        closedPositionIndex: 1
      }
    ]);
  });

  it("preserves explicit multi-panel rail allocation and mixed travel", () => {
    const assembly = createSlidingOpeningAssembly({
      trackCount: 3,
      overlapMm: 42,
      openPercent: 65,
      panels: [
        { trackIndex: 0, movable: true, travelDirection: "right" },
        { trackIndex: 1, movable: false },
        { trackIndex: 2, movable: true, travelDirection: "left" }
      ]
    });

    expect(assembly.activePanelCount).toBe(2);
    expect(assembly.stackSide).toBe("both");
    expect(assembly.openPercent).toBe(65);
    expect(assembly.operationSequence).toEqual(["P1", "P3"]);
    expect(assembly.panels[2]).toMatchObject({
      id: "P3",
      operationOrder: 1,
      travelDirection: "left"
    });
  });

  it("rejects ambiguous or physically incomplete rail configuration", () => {
    expect(() => createSlidingOpeningAssembly({
      trackCount: 1 as 2,
      overlapMm: 30,
      panels: [
        { trackIndex: 0, movable: true, travelDirection: "right" },
        { trackIndex: 0, movable: false }
      ]
    })).toThrow("between 2 and 4 tracks");
    expect(() => createSlidingOpeningAssembly({
      trackCount: 2,
      overlapMm: 30,
      panels: [{ trackIndex: 0, movable: true, travelDirection: "right" }]
    })).toThrow("between 2 and 6 panels");
    expect(() => createSlidingOpeningAssembly({
      trackCount: 2,
      overlapMm: 30,
      panels: [
        { trackIndex: 0, movable: true },
        { trackIndex: 1, movable: false }
      ]
    })).toThrow("require a travel direction");
    expect(() => createSlidingOpeningAssembly({
      trackCount: 2,
      overlapMm: 30,
      panels: [
        { trackIndex: 0, movable: true, travelDirection: "right" },
        { trackIndex: 0, movable: false }
      ]
    })).toThrow("Every declared sliding rail");
    expect(() => createSlidingOpeningAssembly({
      trackCount: 2,
      overlapMm: -1,
      panels: [
        { trackIndex: 0, movable: true, travelDirection: "right" },
        { trackIndex: 1, movable: false }
      ]
    })).toThrow("non-negative finite");
  });
});
