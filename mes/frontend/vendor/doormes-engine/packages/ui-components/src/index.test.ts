import { describe, expect, it } from "vitest";
import {
  APPEARANCE_UV_INPUT_CONSTRAINTS,
  calculateCornerAngleDrag,
  calculateMeetingMullionDrag,
  commitOpeningAngleIntent,
  commitOpeningMotionModeIntent,
  commitOpeningProgressIntent,
  listManagedComponentAssetRequirements,
  readAppearanceEditorUvScale,
  resolveContextDeletableWindow,
  resolveProductTemplateInspectorSummary
} from "./index";
import {
  createFabricationAssemblyCommand,
  createRectangularWindowCommand,
  DesignSession,
  planZcsungSimulationWindowCreation
} from "@doormes/application";
import type { DesignDocument } from "@doormes/contracts";
import { createEmptyDesign } from "@doormes/domain";
import { REFERENCE_WINDOW_INSTALLATION } from "@doormes/geometry-topology";

describe("2D contextual window deletion targeting", () => {
  it("resolves child geometry and exact instances but rejects aggregate ambiguity", () => {
    const session = new DesignSession(createEmptyDesign("DESIGN-CONTEXT-DELETE"));
    session.execute(createRectangularWindowCommand({
      commandId: "CREATE-CONTEXT-W1",
      windowId: "W-CONTEXT-1",
      mark: "左窗",
      widthMm: 1200,
      heightMm: 1500,
      cellId: "CELL-CONTEXT-1"
    }));
    session.execute(createRectangularWindowCommand({
      commandId: "CREATE-CONTEXT-W2",
      windowId: "W-CONTEXT-2",
      mark: "右窗",
      widthMm: 900,
      heightMm: 1500,
      cellId: "CELL-CONTEXT-2"
    }));
    session.execute(createFabricationAssemblyCommand({
      commandId: "CREATE-CONTEXT-ASSEMBLY",
      assemblyId: "A-CONTEXT",
      mark: "两联窗",
      instances: [
        {
          objectId: "A-CONTEXT:I1",
          windowId: "W-CONTEXT-1",
          transform: { xMm: 0, yMm: 0, zMm: 0, rotationYDeg: 0 }
        },
        {
          objectId: "A-CONTEXT:I2",
          windowId: "W-CONTEXT-2",
          transform: { xMm: 1230, yMm: 0, zMm: 0, rotationYDeg: 0 }
        }
      ],
      joints: [{
        objectId: "A-CONTEXT:J1",
        jointType: "mullion_joint",
        firstInstanceId: "A-CONTEXT:I1",
        firstEdge: "right",
        secondInstanceId: "A-CONTEXT:I2",
        secondEdge: "left",
        gapMm: 30,
        factoryScope: "factory"
      }],
      openingClearance: { topMm: 10, rightMm: 12, bottomMm: 15, leftMm: 12 },
      installation: REFERENCE_WINDOW_INSTALLATION
    }));

    expect(resolveContextDeletableWindow(session.document, "CELL-CONTEXT-1")?.objectId)
      .toBe("W-CONTEXT-1");
    expect(resolveContextDeletableWindow(session.document, "A-CONTEXT:I2")?.objectId)
      .toBe("W-CONTEXT-2");
    expect(resolveContextDeletableWindow(session.document, "A-CONTEXT")).toBeUndefined();
    expect(resolveContextDeletableWindow(session.document, "A-CONTEXT:J1")).toBeUndefined();
  });
});

describe("target product template inspector projection", () => {
  it("shows business identity and non-production status without technical assumptions", () => {
    const session = new DesignSession(createEmptyDesign("DESIGN-TEMPLATE-INSPECTOR"));
    const plan = planZcsungSimulationWindowCreation({
      templateId: "ZCSUNG-SIM-LH-120-TT",
      commandIdPrefix: "CREATE-TEMPLATE-INSPECTOR",
      windowId: "WIN-TEMPLATE-INSPECTOR"
    });
    session.executeTransaction(plan.commands, "TX-TEMPLATE-INSPECTOR");

    expect(resolveProductTemplateInspectorSummary(session.document.windows[0]!)).toEqual({
      productName: "120内开内倒系统窗",
      statusLabel: "公开参考模拟 · 禁止投产",
      versionLabel: "模板 ZCSUNG-SIM-LH-120-TT · v0.1.1"
    });
  });

  it("does not add a template banner to an ordinary manual window", () => {
    const session = new DesignSession(createEmptyDesign("DESIGN-MANUAL-INSPECTOR"));
    session.execute(createRectangularWindowCommand({
      commandId: "CREATE-MANUAL-INSPECTOR",
      windowId: "WIN-MANUAL-INSPECTOR",
      mark: "手工窗",
      widthMm: 1200,
      heightMm: 1500,
      cellId: "CELL-MANUAL-INSPECTOR"
    }));

    expect(resolveProductTemplateInspectorSummary(session.document.windows[0]!)).toBeUndefined();
  });
});

describe("appearance UV input constraints", () => {
  it("accepts ordinary positive decimal repeats without a native step grid", () => {
    expect(APPEARANCE_UV_INPUT_CONSTRAINTS).toEqual({
      min: 0.001,
      max: 1_000,
      step: "any"
    });
    for (const value of [0.001, 0.5, 1, 2, 2.25, 1_000]) {
      expect(value).toBeGreaterThanOrEqual(APPEARANCE_UV_INPUT_CONSTRAINTS.min);
      expect(value).toBeLessThanOrEqual(APPEARANCE_UV_INPUT_CONSTRAINTS.max);
    }
  });

  it("persists authored repeats before a texture bundle is assigned", () => {
    expect(readAppearanceEditorUvScale("0.5", "0.5")).toEqual({ x: 0.5, y: 0.5 });
    expect(readAppearanceEditorUvScale("2.25", "3")).toEqual({ x: 2.25, y: 3 });
  });

  it("rejects non-finite or out-of-range repeats at the shared form boundary", () => {
    expect(() => readAppearanceEditorUvScale("0", "1")).toThrow(/between 0\.001 and 1000/);
    expect(() => readAppearanceEditorUvScale("1", "Infinity")).toThrow(/between 0\.001 and 1000/);
  });
});

describe("flying-mullion pointer conversion", () => {
  it("keeps the gesture direction and converts client pixels exactly once", () => {
    const movedRight = calculateMeetingMullionDrag({
      clientX: 520,
      startClientX: 500,
      startPositionMm: 620,
      pixelsPerMm: 0.2,
      renderScale: 0.25
    });
    const movedLeft = calculateMeetingMullionDrag({
      clientX: 480,
      startClientX: 500,
      startPositionMm: 620,
      pixelsPerMm: 0.2,
      renderScale: 0.25
    });

    expect(movedRight).toEqual({ deltaMm: 100, positionMm: 720, svgDelta: 25 });
    expect(movedLeft).toEqual({ deltaMm: -100, positionMm: 520, svgDelta: -25 });
  });

  it("rejects a missing screen scale instead of committing an unbounded jump", () => {
    expect(() =>
      calculateMeetingMullionDrag({
        clientX: 520,
        startClientX: 500,
        startPositionMm: 620,
        pixelsPerMm: 0,
        renderScale: 0.25
      })
    ).toThrow(RangeError);
  });
});

describe("spatial corner angle pointer conversion", () => {
  it("preserves business turn direction across the SVG angle boundary", () => {
    const clockwise = calculateCornerAngleDrag({
      axisX: 100,
      axisY: 100,
      pointerX: 100,
      pointerY: 150,
      firstRayAngleRadians: Math.PI,
      turnDirection: "clockwise",
      minimumAngleDeg: 60,
      maximumAngleDeg: 150,
      radius: 28
    });
    const counterclockwise = calculateCornerAngleDrag({
      axisX: 100,
      axisY: 100,
      pointerX: 100,
      pointerY: 50,
      firstRayAngleRadians: Math.PI,
      turnDirection: "counterclockwise",
      minimumAngleDeg: 60,
      maximumAngleDeg: 150,
      radius: 28
    });

    expect(clockwise.includedAngleDeg).toBe(90);
    expect(clockwise.sweepFlag).toBe(0);
    expect(clockwise.endX).toBeCloseTo(100, 6);
    expect(clockwise.endY).toBeCloseTo(128, 6);
    expect(counterclockwise.includedAngleDeg).toBe(90);
    expect(counterclockwise.sweepFlag).toBe(1);
    expect(counterclockwise.endY).toBeCloseTo(72, 6);
  });

  it("clamps the preview to the frozen connector catalog capability", () => {
    const belowMinimum = calculateCornerAngleDrag({
      axisX: 0,
      axisY: 0,
      pointerX: -10,
      pointerY: 1,
      firstRayAngleRadians: Math.PI,
      turnDirection: "clockwise",
      minimumAngleDeg: 60,
      maximumAngleDeg: 150,
      radius: 28
    });
    const aboveMaximum = calculateCornerAngleDrag({
      axisX: 0,
      axisY: 0,
      pointerX: 10,
      pointerY: -1,
      firstRayAngleRadians: Math.PI,
      turnDirection: "clockwise",
      minimumAngleDeg: 60,
      maximumAngleDeg: 150,
      radius: 28
    });

    expect(belowMinimum.includedAngleDeg).toBe(60);
    expect(aboveMaximum.includedAngleDeg).toBe(150);
  });
});

describe("opening native-control intent ordering", () => {
  /**
   * Reproduces the real failure mode without a browser DOM: cancellation acts as
   * the synchronous subscriber repaint and overwrites the live control value.
   * The committed value must remain the one captured before that repaint.
   *
   * @example Dragging 80→50 commits 50 even though cancellation restores 80.
   * @since 0.8.5
   * @modified 2026-09-17 - Guards range/select ordering after the manual defect report.
   */
  it("captures range progress before cancellation repaints the control", () => {
    let liveValue = "50";
    let committed = -1;

    commitOpeningProgressIntent({
      readValue: () => liveValue,
      cancelPlayback: () => { liveValue = "80"; },
      setProgress: (progressPercent) => { committed = progressPercent; }
    });

    expect(liveValue).toBe("80");
    expect(committed).toBe(50);
  });

  it("captures tilt mode before cancellation repaints the select", () => {
    let liveValue = "tilt";
    let committed: "primary" | "tilt" = "primary";

    commitOpeningMotionModeIntent({
      readValue: () => liveValue,
      cancelPlayback: () => { liveValue = "primary"; },
      setMotionMode: (motionMode) => { committed = motionMode; }
    });

    expect(liveValue).toBe("primary");
    expect(committed).toBe("tilt");
  });

  it("captures a physical angle before cancellation repaints either degree input", () => {
    let liveValue = "12.5";
    let committed = -1;

    commitOpeningAngleIntent({
      readValue: () => liveValue,
      cancelPlayback: () => { liveValue = "18.3"; },
      setAngleDegrees: (angleDegrees) => { committed = angleDegrees; }
    });

    expect(liveValue).toBe("18.3");
    expect(committed).toBe(12.5);
  });
});

describe("managed component model reload discovery", () => {
  it("deduplicates primary/LOD references and preserves their quality tiers", () => {
    const highHash = `sha256:${"a".repeat(64)}`;
    const mediumHash = `sha256:${"b".repeat(64)}`;
    const lowHash = `sha256:${"c".repeat(64)}`;
    const document = {
      windows: [{
        visualConfiguration: {
          hardwareModels: [{
            model: {
              geometry: {
                kind: "gltf",
                assetId: "HANDLE-HIGH",
                contentHash: highHash,
                lodAssets: [
                  { quality: "medium", assetId: "HANDLE-MEDIUM", contentHash: mediumHash },
                  { quality: "low", assetId: "HANDLE-LOW", contentHash: lowHash }
                ]
              }
            }
          }, {
            model: {
              geometry: {
                kind: "gltf",
                assetId: "HANDLE-HIGH",
                contentHash: highHash,
                lodAssets: []
              }
            }
          }]
        }
      }]
    } as unknown as DesignDocument;

    expect(listManagedComponentAssetRequirements(document)).toEqual([
      { assetId: "HANDLE-HIGH", contentHash: highHash, quality: "high" },
      { assetId: "HANDLE-LOW", contentHash: lowHash, quality: "low" },
      { assetId: "HANDLE-MEDIUM", contentHash: mediumHash, quality: "medium" }
    ]);
    expect(listManagedComponentAssetRequirements(document, "low")).toEqual([
      { assetId: "HANDLE-HIGH", contentHash: highHash, quality: "high" },
      { assetId: "HANDLE-LOW", contentHash: lowHash, quality: "low" }
    ]);
    expect(listManagedComponentAssetRequirements(document, "high")).toEqual([
      { assetId: "HANDLE-HIGH", contentHash: highHash, quality: "high" }
    ]);
  });

  it("rejects one persisted asset ID pinned to conflicting hashes", () => {
    const document = {
      windows: [{
        visualConfiguration: {
          hardwareModels: [{ model: { geometry: {
            kind: "gltf",
            assetId: "CONFLICTING-ASSET",
            contentHash: `sha256:${"a".repeat(64)}`,
            lodAssets: []
          } } }, { model: { geometry: {
            kind: "gltf",
            assetId: "CONFLICTING-ASSET",
            contentHash: `sha256:${"b".repeat(64)}`,
            lodAssets: []
          } } }]
        }
      }]
    } as unknown as DesignDocument;

    expect(() => listManagedComponentAssetRequirements(document)).toThrow(/conflicting/i);
  });
});
