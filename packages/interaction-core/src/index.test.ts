import { describe, expect, it, vi } from "vitest";
import {
  createRectangularWindowCommand,
  createSetWindowCellOpeningCommand,
  DesignSession
} from "@doormes/application";
import { createEmptyDesign } from "@doormes/domain";
import {
  collectOpeningPreviewPanelDefinitions,
  OpeningPreviewController,
  OpeningPreviewStore,
  resolveOpeningPreviewAngleDegrees,
  WindowVisualPreviewStore
} from "./index";

/**
 * Builds a representative two-leaf design for preview-state tests.
 *
 * @returns A session whose flying-mullion assembly has stable P1/P2 identities.
 * @example Tests call this helper and then construct `OpeningPreviewStore`.
 * @since 0.8.0
 * @modified 2026-09-17 - Added MOT-002 interaction test fixture.
 */
function createDoubleOpeningSession(
  mullionMode: "flying_mullion" | "fixed_mullion" = "flying_mullion"
): DesignSession {
  const session = new DesignSession(createEmptyDesign("DESIGN-PREVIEW-STORE"));
  session.execute(
    createRectangularWindowCommand({
      commandId: "CREATE-PREVIEW-STORE",
      windowId: "WIN-PREVIEW-STORE",
      mark: "C-PREVIEW",
      widthMm: 1600,
      heightMm: 1500,
      cellId: "CELL-PREVIEW-STORE"
    })
  );
  session.execute(
    createSetWindowCellOpeningCommand({
      commandId: "SET-PREVIEW-STORE",
      windowId: "WIN-PREVIEW-STORE",
      cellId: "CELL-PREVIEW-STORE",
      cellType: "turn_tilt",
      opening: "right_in",
      panelCount: 2,
      mullionMode
    })
  );
  return session;
}

describe("WindowVisualPreviewStore", () => {
  it("projects a normalized visual draft without mutating revision or formal state", () => {
    const session = createDoubleOpeningSession();
    const window = session.document.windows[0]!;
    const configuration = window.visualConfiguration!;
    const draft = {
      ...configuration,
      appearance: {
        ...configuration.appearance,
        frame: {
          ...configuration.appearance.frame,
          outside: {
            ...configuration.appearance.frame.outside,
            baseColor: "#ff0000"
          }
        }
      }
    };
    const store = new WindowVisualPreviewStore();
    const revisionBefore = session.document.revision;
    const notifications: number[] = [];
    const dispose = store.subscribe((state) => notifications.push(state.revision));

    store.set(window.objectId, draft);
    const projected = store.project(session.document);

    expect(projected.revision).toBe(revisionBefore);
    expect(projected.windows[0]?.visualConfiguration?.appearance.frame.outside.baseColor)
      .toBe("#ff0000");
    expect(session.document.windows[0]?.visualConfiguration?.appearance.frame.outside.baseColor)
      .not.toBe("#ff0000");
    expect(session.undo()).toBe(true);
    store.synchronize(session.document);
    expect(store.project(session.document)).toBe(session.document);
    expect(notifications).toEqual([0, 1, 2]);
    dispose();
  });
});

describe("OpeningPreviewStore", () => {
  it("exposes a movable sliding sash through translation progress, not hinge angles", () => {
    const session = new DesignSession(createEmptyDesign("DESIGN-SLIDING-PREVIEW"));
    session.execute(createRectangularWindowCommand({
      commandId: "CREATE-SLIDING-PREVIEW",
      windowId: "WIN-SLIDING-PREVIEW",
      mark: "S1",
      widthMm: 1800,
      heightMm: 1500,
      cellId: "CELL-SLIDING-PREVIEW"
    }));
    session.execute(createSetWindowCellOpeningCommand({
      commandId: "SET-SLIDING-PREVIEW",
      windowId: "WIN-SLIDING-PREVIEW",
      cellId: "CELL-SLIDING-PREVIEW",
      cellType: "sliding",
      opening: "slide_right"
    }));

    const definitions = collectOpeningPreviewPanelDefinitions(session.document);
    expect(definitions).toEqual([
      expect.objectContaining({
        key: "CELL-SLIDING-PREVIEW::P1",
        mechanism: "sliding",
        panelId: "P1",
        operationMode: "independent",
        configuredOpenPercent: 80,
        supportsTilt: false
      })
    ]);

    const store = new OpeningPreviewStore(session.document);
    expect(store.state.selectedPanelKeys).toEqual(["CELL-SLIDING-PREVIEW::P1"]);
    store.setSelectedProgress(35);
    expect(store.state.panelProgressPercent["CELL-SLIDING-PREVIEW::P1"]).toBe(35);
    expect(() => store.setSelectedAngleDegrees(35)).toThrow(/travel percentage/);
    expect(() => store.setSelectedMotionMode("tilt")).toThrow(/hinge motion modes/);
    const controller = new OpeningPreviewController(store, 600);
    controller.openSelected();
    controller.advance(600);
    expect(store.state.panelProgressPercent["CELL-SLIDING-PREVIEW::P1"]).toBe(100);
  });

  it("keeps hinged-angle and sliding-progress panel selections homogeneous", () => {
    const session = createDoubleOpeningSession();
    session.execute(createRectangularWindowCommand({
      commandId: "CREATE-MIXED-SLIDING",
      windowId: "WIN-MIXED-SLIDING",
      mark: "S1",
      widthMm: 1800,
      heightMm: 1500,
      cellId: "CELL-MIXED-SLIDING"
    }));
    session.execute(createSetWindowCellOpeningCommand({
      commandId: "SET-MIXED-SLIDING",
      windowId: "WIN-MIXED-SLIDING",
      cellId: "CELL-MIXED-SLIDING",
      cellType: "sliding",
      opening: "slide_right"
    }));

    const store = new OpeningPreviewStore(session.document);
    expect(store.state.selectedPanelKeys).toEqual([
      "CELL-PREVIEW-STORE::P1",
      "CELL-PREVIEW-STORE::P2"
    ]);
    store.toggleSelected("CELL-MIXED-SLIDING::P1");
    expect(store.state.selectedPanelKeys).toEqual(["CELL-MIXED-SLIDING::P1"]);
    store.toggleSelected("CELL-PREVIEW-STORE::P1");
    expect(store.state.selectedPanelKeys).toEqual(["CELL-PREVIEW-STORE::P1"]);
  });

  it("offers primary rotation but no tilt mode for an outward side-hung sash", () => {
    const session = new DesignSession(createEmptyDesign("DESIGN-OUTWARD-PREVIEW"));
    session.execute(createRectangularWindowCommand({
      commandId: "CREATE-OUTWARD-PREVIEW",
      windowId: "WIN-OUTWARD-PREVIEW",
      mark: "OUT",
      widthMm: 1200,
      heightMm: 1500,
      cellId: "CELL-OUTWARD-PREVIEW"
    }));
    session.execute(createSetWindowCellOpeningCommand({
      commandId: "SET-OUTWARD-PREVIEW",
      windowId: "WIN-OUTWARD-PREVIEW",
      cellId: "CELL-OUTWARD-PREVIEW",
      cellType: "turn_tilt",
      opening: "right_out",
      maximumAngleDegreesByMode: { primary: 135 }
    }));

    expect(collectOpeningPreviewPanelDefinitions(session.document)[0]).toMatchObject({
      key: "CELL-OUTWARD-PREVIEW::P1",
      supportsTilt: false,
      motionMode: "primary",
      maximumAngleDegreesByMode: { primary: 135 }
    });
  });

  it("extracts ordered per-panel definitions with document-wide keys", () => {
    const session = createDoubleOpeningSession();
    expect(collectOpeningPreviewPanelDefinitions(session.document)).toEqual([
      expect.objectContaining({
        key: "CELL-PREVIEW-STORE::P1",
        mechanism: "hinged",
        panelId: "P1",
        operationMode: "ordered",
        operationOrder: 1
      }),
      expect.objectContaining({
        key: "CELL-PREVIEW-STORE::P2",
        mechanism: "hinged",
        panelId: "P2",
        operationMode: "ordered",
        operationOrder: 0
      })
    ]);
  });

  it("changes two leaf previews independently without mutating the design", () => {
    const session = createDoubleOpeningSession();
    const designBefore = JSON.stringify(session.document);
    const revisionBefore = session.document.revision;
    const store = new OpeningPreviewStore(session.document);
    const listener = vi.fn();
    store.subscribe(listener);

    store.selectOnly("CELL-PREVIEW-STORE::P1");
    store.setSelectedProgress(25);
    expect(store.state.panelProgressPercent).toMatchObject({
      "CELL-PREVIEW-STORE::P1": 25,
      "CELL-PREVIEW-STORE::P2": 80
    });
    store.toggleSelected("CELL-PREVIEW-STORE::P2");
    store.setSelectedMotionMode("tilt");
    expect(store.state.panelMotionMode).toEqual({
      "CELL-PREVIEW-STORE::P1": "tilt",
      "CELL-PREVIEW-STORE::P2": "tilt"
    });
    store.closeSelected();
    expect(store.state.panelProgressPercent).toEqual({
      "CELL-PREVIEW-STORE::P1": 0,
      "CELL-PREVIEW-STORE::P2": 0
    });
    store.openAll();
    expect(store.state.panelProgressPercent).toEqual({
      "CELL-PREVIEW-STORE::P1": 100,
      "CELL-PREVIEW-STORE::P2": 100
    });
    expect(session.document.revision).toBe(revisionBefore);
    expect(JSON.stringify(session.document)).toBe(designBefore);
    expect(listener).toHaveBeenCalled();
  });

  it("initially selects rendered leaves so the angle controls cannot report a stale zero", () => {
    const store = new OpeningPreviewStore(createDoubleOpeningSession().document);

    expect(store.state.selectedPanelKeys).toEqual([
      "CELL-PREVIEW-STORE::P1",
      "CELL-PREVIEW-STORE::P2"
    ]);
    expect(store.panelDefinitions.map((definition) =>
      resolveOpeningPreviewAngleDegrees(definition, store.state)
    )).toEqual([72, 72]);

    store.toggleSelected("CELL-PREVIEW-STORE::P1");
    store.toggleSelected("CELL-PREVIEW-STORE::P2");
    store.synchronize(createDoubleOpeningSession().document);
    expect(store.state.selectedPanelKeys).toEqual([]);
  });

  it("sets the same real angle with mode-specific normalized progress", () => {
    const store = new OpeningPreviewStore(createDoubleOpeningSession().document);
    store.selectOnly("CELL-PREVIEW-STORE::P1");
    store.setSelectedAngleDegrees(9);
    const primaryProgress = store.state.panelProgressPercent["CELL-PREVIEW-STORE::P1"] ?? 0;

    store.setSelectedMotionMode("tilt");
    store.setSelectedAngleDegrees(9);
    const tiltProgress = store.state.panelProgressPercent["CELL-PREVIEW-STORE::P1"] ?? 0;

    expect(primaryProgress).toBeCloseTo(10, 2);
    expect(tiltProgress).toBeCloseTo(49.09, 2);
    expect(tiltProgress).toBeGreaterThan(primaryProgress);
  });

  it("clamps unsafe input and rejects stale panel identities", () => {
    const store = new OpeningPreviewStore(createDoubleOpeningSession().document);
    store.setPanelProgress("CELL-PREVIEW-STORE::P1", 130);
    store.setPanelProgress("CELL-PREVIEW-STORE::P2", Number.NaN);
    expect(store.state.panelProgressPercent).toEqual({
      "CELL-PREVIEW-STORE::P1": 100,
      "CELL-PREVIEW-STORE::P2": 0
    });
    expect(() => store.selectOnly("REMOVED::P1")).toThrow(/Unknown opening preview panel/);
  });

  it("opens flying-mullion prerequisites first and closes dependants first", () => {
    const store = new OpeningPreviewStore(createDoubleOpeningSession().document);
    const controller = new OpeningPreviewController(store, 600);
    store.closeAll();
    store.selectOnly("CELL-PREVIEW-STORE::P1");

    const openingPlan = controller.openSelected();
    expect(openingPlan.phases.map((phase) => phase.items.map((item) => item.panelKey))).toEqual([
      ["CELL-PREVIEW-STORE::P2"],
      ["CELL-PREVIEW-STORE::P1"]
    ]);
    controller.advance(300);
    expect(store.state.panelProgressPercent).toEqual({
      "CELL-PREVIEW-STORE::P1": 0,
      "CELL-PREVIEW-STORE::P2": 50
    });
    controller.advance(300);
    controller.advance(600);
    expect(store.state.panelProgressPercent).toEqual({
      "CELL-PREVIEW-STORE::P1": 100,
      "CELL-PREVIEW-STORE::P2": 100
    });
    expect(store.state.playback).toBe("idle");

    store.selectOnly("CELL-PREVIEW-STORE::P2");
    const closingPlan = controller.closeSelected();
    expect(closingPlan.phases.map((phase) => phase.items.map((item) => item.panelKey))).toEqual([
      ["CELL-PREVIEW-STORE::P1"],
      ["CELL-PREVIEW-STORE::P2"]
    ]);
  });

  it("animates fixed-mullion independent leaves together and resumes after pause", () => {
    const store = new OpeningPreviewStore(
      createDoubleOpeningSession("fixed_mullion").document
    );
    const controller = new OpeningPreviewController(store, 400);
    store.closeAll();
    const plan = controller.openAll();
    expect(plan.phases).toHaveLength(1);
    expect(plan.phases[0]?.items.map((item) => item.panelKey)).toEqual([
      "CELL-PREVIEW-STORE::P1",
      "CELL-PREVIEW-STORE::P2"
    ]);

    controller.advance(100);
    controller.pause();
    expect(store.state.playback).toBe("paused");
    const paused = store.state.panelProgressPercent;
    controller.advance(200);
    expect(store.state.panelProgressPercent).toBe(paused);
    controller.resume();
    controller.advance(300);
    expect(store.state.panelProgressPercent).toEqual({
      "CELL-PREVIEW-STORE::P1": 100,
      "CELL-PREVIEW-STORE::P2": 100
    });
    expect(store.state.playback).toBe("idle");
  });

  it("plays the prototype cycle by opening in order and closing in reverse", () => {
    const store = new OpeningPreviewStore(createDoubleOpeningSession().document);
    const controller = new OpeningPreviewController(store, 200);
    store.selectOnly("CELL-PREVIEW-STORE::P1");

    const openingHalf = controller.playSelectedCycle();
    expect(openingHalf.phases.map((phase) => phase.items[0]?.panelKey)).toEqual([
      "CELL-PREVIEW-STORE::P2",
      "CELL-PREVIEW-STORE::P1"
    ]);
    expect(store.state.panelProgressPercent).toEqual({
      "CELL-PREVIEW-STORE::P1": 0,
      "CELL-PREVIEW-STORE::P2": 0
    });
    controller.advance(800);
    expect(store.state.panelProgressPercent).toEqual({
      "CELL-PREVIEW-STORE::P1": 0,
      "CELL-PREVIEW-STORE::P2": 0
    });
    expect(store.state.playback).toBe("idle");
  });
});
