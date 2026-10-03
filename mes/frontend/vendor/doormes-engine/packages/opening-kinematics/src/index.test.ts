import { describe, expect, it } from "vitest";
import {
  calculateOpeningMotionEnvelope,
  createHingedOpeningMotion,
  createOpeningMechanismMotion,
  createOpeningPanelKey,
  createSlidingOpeningMotion,
  findOpeningMotionCollisions,
  openingAngleDegreesToProgress,
  openingProgressToAngleDegrees,
  resolveOpeningConnectionLayout,
  resolveOpeningMaximumAngleDegrees,
  resolveOpeningPose,
  sampleOpeningAngleArc,
  sampleOpeningMotion
} from "./index";

describe("createOpeningPanelKey", () => {
  it("combines assembly and panel identity without colliding across cells", () => {
    expect(createOpeningPanelKey("CELL-1", "P1")).toBe("CELL-1::P1");
    expect(createOpeningPanelKey("CELL-2", "P1")).not.toBe(
      createOpeningPanelKey("CELL-1", "P1")
    );
  });
});

describe("opening kinematics", () => {
  it("resolves closed, intermediate and open side-hinged poses", () => {
    const definition = createHingedOpeningMotion({
      motionId: "CELL-1:P1:primary",
      hingeEdge: "left",
      openPlane: "in",
      maxAngleRadians: Math.PI / 2
    });

    expect(resolveOpeningPose(definition, 0).rotationRadians.y).toBe(0);
    expect(resolveOpeningPose(definition, 50).rotationRadians.y).toBeCloseTo(Math.PI / 4);
    expect(resolveOpeningPose(definition, 100).rotationRadians.y).toBeCloseTo(Math.PI / 2);
  });

  it("keeps top-out direction and clamps unsafe preview progress", () => {
    const definition = createHingedOpeningMotion({
      motionId: "CELL-2:P1:primary",
      hingeEdge: "top",
      openPlane: "out",
      maxAngleRadians: 0.72
    });

    expect(resolveOpeningPose(definition, -20).rotationRadians.x).toBe(0);
    expect(resolveOpeningPose(definition, 50).rotationRadians.x).toBeCloseTo(-0.36);
    expect(resolveOpeningPose(definition, 140).rotationRadians.x).toBeCloseTo(-0.72);
  });

  it("supports overlapping phased translation without mechanism-specific branches", () => {
    const pose = resolveOpeningPose({
      motionId: "SLIDE-1:P1:primary",
      steps: [
        {
          id: "lift",
          kind: "translation",
          startProgress: 0,
          endProgress: 20,
          deltaMm: { x: 0, y: 12, z: 0 }
        },
        {
          id: "slide",
          kind: "translation",
          startProgress: 10,
          endProgress: 100,
          deltaMm: { x: 600, y: 0, z: 0 }
        }
      ]
    }, 55);

    expect(pose.translationMm.y).toBe(12);
    expect(pose.translationMm.x).toBeCloseTo(300);
    expect(pose.activeStepIds).toEqual(["slide"]);
  });

  it("resolves ordinary sliding travel in physical millimetres for both directions", () => {
    const left = createSlidingOpeningMotion({
      motionId: "SLIDE-LEFT::P1:primary",
      direction: "left",
      travelMm: 620
    });
    const right = createSlidingOpeningMotion({
      motionId: "SLIDE-RIGHT::P2:primary",
      direction: "right",
      travelMm: 480
    });

    expect(resolveOpeningPose(left, 0).translationMm.x).toBe(0);
    expect(resolveOpeningPose(left, 50).translationMm.x).toBe(-310);
    expect(resolveOpeningPose(left, 100).translationMm).toEqual({ x: -620, y: 0, z: 0 });
    expect(resolveOpeningPose(right, 25).translationMm.x).toBe(120);
    expect(resolveOpeningPose(right, 100).rotationRadians).toEqual({ x: 0, y: 0, z: 0 });
  });

  it("rejects ambiguous or non-physical ordinary sliding motion inputs", () => {
    expect(() => createSlidingOpeningMotion({
      motionId: " ",
      direction: "left",
      travelMm: 500
    })).toThrow("motion ID");
    expect(() => createSlidingOpeningMotion({
      motionId: "SLIDE-INVALID",
      direction: "right",
      travelMm: 0
    })).toThrow("positive finite");
    expect(() => createSlidingOpeningMotion({
      motionId: "SLIDE-NAN",
      direction: "right",
      travelMm: Number.NaN
    })).toThrow("positive finite");
  });

  it("uses the 90-degree side-hung reference limit and the hardware-limited tilt angle", () => {
    const primary = createOpeningMechanismMotion({
      motionId: "CELL-3::P1:primary",
      mechanism: "tilt_turn",
      motionMode: "primary",
      hingeEdge: "right",
      openPlane: "in"
    });
    const tilt = createOpeningMechanismMotion({
      motionId: "CELL-3::P1:tilt",
      mechanism: "tilt_turn",
      motionMode: "tilt",
      hingeEdge: "right",
      openPlane: "in"
    });

    expect(resolveOpeningPose(primary, 100).rotationRadians.y).toBeCloseTo(-Math.PI / 2);
    expect(resolveOpeningPose(tilt, 50).rotationRadians.x).toBeCloseTo(-0.16);
    expect(resolveOpeningPose(tilt, 100).rotationRadians.y).toBe(0);
  });

  it("samples exact endpoints and calculates a common swept envelope", () => {
    const definition = createOpeningMechanismMotion({
      motionId: "CELL-4::P1:primary",
      mechanism: "tilt_turn",
      hingeEdge: "left",
      openPlane: "in"
    });
    const path = sampleOpeningMotion(definition, 4);
    const envelope = calculateOpeningMotionEnvelope(
      definition,
      {
        min: { x: 0, y: -750, z: -30 },
        max: { x: 700, y: 750, z: 30 }
      },
      20
    );

    expect(path).toHaveLength(5);
    expect(path.map((pose) => pose.progressPercent)).toEqual([0, 25, 50, 75, 100]);
    expect(envelope.sampleCount).toBe(21);
    expect(envelope.min.x).toBeLessThan(0);
    expect(envelope.max.x).toBeGreaterThanOrEqual(700);
    expect(envelope.min.z).toBeLessThan(-600);
    expect(envelope.max.y).toBe(750);
  });

  it("detects a real rotated-box collision without using a false swept AABB", () => {
    const definition = createHingedOpeningMotion({
      motionId: "CELL-5::P1:clearance",
      hingeEdge: "left",
      openPlane: "out",
      maxAngleRadians: Math.PI / 2
    });
    const collisions = findOpeningMotionCollisions({
      definition,
      localBounds: {
        min: { x: 0, y: -50, z: -5 },
        max: { x: 100, y: 50, z: 5 }
      },
      originMm: { x: 0, y: 0, z: 0 },
      obstacles: [{
        obstacleId: "deep-wall-return",
        min: { x: -10, y: -60, z: 70 },
        max: { x: 10, y: 60, z: 90 }
      }],
      intervalCount: 180
    });

    expect(collisions).toHaveLength(1);
    expect(collisions[0]).toMatchObject({ obstacleId: "deep-wall-return" });
    expect(collisions[0]!.progressPercent).toBeGreaterThan(40);
    expect(collisions[0]!.progressPercent).toBeLessThan(100);
    expect(collisions[0]!.penetrationMm).toBeGreaterThan(0.1);
  });

  it("does not report an obstacle that only intersects the full swept AABB", () => {
    const collisions = findOpeningMotionCollisions({
      definition: createHingedOpeningMotion({
        motionId: "CELL-6::P1:clearance",
        hingeEdge: "left",
        openPlane: "out",
        maxAngleRadians: Math.PI / 2
      }),
      localBounds: {
        min: { x: 0, y: -50, z: -5 },
        max: { x: 100, y: 50, z: 5 }
      },
      originMm: { x: 0, y: 0, z: 0 },
      obstacles: [{
        obstacleId: "swept-aabb-only",
        min: { x: 78, y: -60, z: 78 },
        max: { x: 88, y: 60, z: 88 }
      }],
      intervalCount: 180
    });

    expect(collisions).toEqual([]);
  });
});

describe("opening connection layouts", () => {
  it("switches a tilt-turn from side pivots to bottom pivots and a head stay", () => {
    const primary = resolveOpeningConnectionLayout({
      mechanism: "tilt_turn",
      motionMode: "primary",
      hingeEdge: "right"
    });
    const tilt = resolveOpeningConnectionLayout({
      mechanism: "tilt_turn",
      motionMode: "tilt",
      hingeEdge: "right"
    });

    expect(primary.anchors.map((anchor) => anchor.sashEdge)).toEqual(["right", "right"]);
    expect(tilt.anchors.map((anchor) => [anchor.role, anchor.sashEdge])).toEqual([
      ["pivot", "bottom"],
      ["pivot", "bottom"],
      ["stay", "top"]
    ]);
  });

  it("accepts a future product-series override without changing the resolver", () => {
    const layout = resolveOpeningConnectionLayout({
      mechanism: "tilt_turn",
      motionMode: "tilt",
      hingeEdge: "left",
      preset: {
        presetId: "series-custom-a",
        layouts: [{
          mechanism: "tilt_turn",
          motionMode: "tilt",
          anchors: [{
            id: "custom-bottom-pivot",
            role: "pivot",
            sashEdge: "bottom",
            sashPositionRatio: 0.36,
            frameEdge: "bottom",
            framePositionRatio: 0.34
          }]
        }]
      }
    });

    expect(layout.presetId).toBe("series-custom-a");
    expect(layout.anchors).toEqual([
      expect.objectContaining({ id: "custom-bottom-pivot", sashPositionRatio: 0.36 })
    ]);
  });

  it("rejects unsafe custom ratios before they reach either renderer", () => {
    expect(() => resolveOpeningConnectionLayout({
      mechanism: "top_hung",
      hingeEdge: "top",
      preset: {
        presetId: "broken",
        layouts: [{
          mechanism: "top_hung",
          motionMode: "primary",
          anchors: [{
            id: "bad",
            role: "pivot",
            sashEdge: "top",
            sashPositionRatio: 1.2,
            frameEdge: "top",
            framePositionRatio: 0.5
          }]
        }]
      }
    })).toThrow(/ratios/);
  });
});

describe("physical opening angle controls", () => {
  it("derives user-facing limits from the same primary and tilt strategies", () => {
    const primaryMaximum = resolveOpeningMaximumAngleDegrees({
      mechanism: "tilt_turn",
      motionMode: "primary",
      hingeEdge: "right",
      openPlane: "in"
    });
    const tiltMaximum = resolveOpeningMaximumAngleDegrees({
      mechanism: "tilt_turn",
      motionMode: "tilt",
      hingeEdge: "right",
      openPlane: "in"
    });

    expect(primaryMaximum).toBe(90);
    expect(tiltMaximum).toBeCloseTo(18.335, 3);
    expect(openingProgressToAngleDegrees(50, tiltMaximum)).toBeCloseTo(9.167, 3);
    expect(openingAngleDegreesToProgress(9.167, tiltMaximum)).toBeCloseTo(50, 1);
  });

  it("clamps entered physical angles to the selected mechanism limit", () => {
    expect(openingAngleDegreesToProgress(90, 18)).toBe(100);
    expect(openingAngleDegreesToProgress(-5, 18)).toBe(0);
    expect(openingProgressToAngleDegrees(50, 180)).toBe(90);
    expect(() => openingProgressToAngleDegrees(50, 0)).toThrow(/maximum angle/);
    expect(() => openingProgressToAngleDegrees(50, 181)).toThrow(/at most 180/);
  });

  it("samples a renderer-neutral dashed arc including exact endpoints", () => {
    const points = sampleOpeningAngleArc({
      radius: 200,
      startAngleRadians: 0,
      sweepAngleRadians: Math.PI / 2
    });

    expect(points.length).toBeGreaterThan(10);
    expect(points[0]).toEqual({ radialX: 200, radialY: 0 });
    expect(points.at(-1)?.radialX).toBeCloseTo(0, 8);
    expect(points.at(-1)?.radialY).toBeCloseTo(200, 8);
  });
});
