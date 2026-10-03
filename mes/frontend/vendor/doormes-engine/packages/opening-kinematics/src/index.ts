/**
 * Three-axis numeric vector used by the renderer-independent motion kernel.
 *
 * Translation values use millimetres and rotation values use radians. The
 * motion package deliberately defines its own structural vector instead of
 * importing Three.js so SVG, tests, mobile and future collision services can
 * consume the same result.
 *
 * @example `{ x: 0, y: 12, z: 0 }` lifts a sash twelve millimetres.
 * @since 0.8.0
 * @modified 2026-09-17 - Added the shared opening-motion coordinate value.
 */
export interface OpeningMotionVector3 {
  readonly x: number;
  readonly y: number;
  readonly z: number;
}

/** One bounded part of a mechanism's complete 0..100% motion. */
export type OpeningMotionStep =
  | Readonly<{
      id: string;
      kind: "translation";
      startProgress: number;
      endProgress: number;
      deltaMm: OpeningMotionVector3;
    }>
  | Readonly<{
      id: string;
      kind: "rotation";
      startProgress: number;
      endProgress: number;
      deltaRadians: OpeningMotionVector3;
    }>
  | Readonly<{
      id: string;
      kind: "scale";
      startProgress: number;
      endProgress: number;
      targetScale: OpeningMotionVector3;
    }>;

/**
 * Renderer-neutral definition of one panel motion mode.
 *
 * A mechanism is expressed as bounded steps instead of a switch statement in
 * each renderer. Single-axis hinges use one rotation step; lift-slide can later
 * combine an early lift and a delayed translation without changing the store,
 * UI or renderer contracts.
 *
 * @example A left inward sash has one Y rotation from 0 to 100 percent.
 * @since 0.8.0
 * @modified 2026-09-17 - Added the first extensible motion definition.
 */
export interface OpeningMotionDefinition {
  readonly motionId: string;
  readonly steps: readonly OpeningMotionStep[];
}

/**
 * Complete panel pose resolved for one instantaneous preview progress.
 *
 * Positions are offsets from the renderer's closed-state pivot. Euler rotation
 * components and scale are accumulated in declared step order. The current
 * angular mechanisms rotate around a pivot already established by geometry;
 * later linear and phased mechanisms reuse the same output.
 *
 * @example Progress 50 on a 90-degree hinge yields `rotationRadians.y = PI/4`.
 * @since 0.8.0
 * @modified 2026-09-17 - Added the shared pose snapshot result.
 */
export interface OpeningPoseSnapshot {
  readonly motionId: string;
  readonly progressPercent: number;
  readonly translationMm: OpeningMotionVector3;
  readonly rotationRadians: OpeningMotionVector3;
  readonly scale: OpeningMotionVector3;
  readonly activeStepIds: readonly string[];
}

/** Opening mechanisms currently normalized by the shared strategy factory. */
export type OpeningMotionMechanism = "tilt_turn" | "top_hung" | "bottom_hung";

/** User-selectable motion mode for mechanisms with more than one opening path. */
export type OpeningMotionMode = "primary" | "tilt";

/** Horizontal travel direction of one ordinary sliding sash. */
export type SlidingTravelDirection = "left" | "right";

/** One renderer-neutral point on an opening-angle annotation arc. */
export interface OpeningAngleArcPoint {
  readonly radialX: number;
  readonly radialY: number;
}

/** Axis-aligned local bounds of one closed panel relative to its motion pivot. */
export interface OpeningMotionLocalBoundsMm {
  readonly min: OpeningMotionVector3;
  readonly max: OpeningMotionVector3;
}

/** Sampled world-relative bounds swept by a panel during its complete motion. */
export interface OpeningMotionEnvelopeMm extends OpeningMotionLocalBoundsMm {
  readonly sampleCount: number;
}

/** One stationary, axis-aligned installation or product obstacle in world millimetres. */
export interface OpeningMotionAxisAlignedObstacleMm extends OpeningMotionLocalBoundsMm {
  readonly obstacleId: string;
}

/** First sampled physical overlap between one moving panel and one obstacle. */
export interface OpeningMotionCollisionSample {
  readonly obstacleId: string;
  readonly progressPercent: number;
  readonly pose: OpeningPoseSnapshot;
  /** Smallest separating-axis overlap; useful for audit display, not machining tolerance. */
  readonly penetrationMm: number;
}

/** Valid physical edge around which a sash may rotate. */
export type OpeningHingeEdge = "left" | "right" | "top" | "bottom";

/** Functional role of one connection shown by a motion preview. */
export type OpeningConnectionRole = "pivot" | "stay";

/**
 * One configurable sash-to-frame connection in normalized panel coordinates.
 *
 * Ratios run from top to bottom on vertical edges and from left to right on
 * horizontal edges. Both ends are declared because a stay may join the sash
 * head to a different point on the stationary frame, whereas a pivot normally
 * uses matching positions. These values describe kinematics/presentation only;
 * production hardware IDs and quantities continue to come from the BOM model.
 *
 * @example A bottom pivot at 18% uses both edges `bottom` and both ratios `0.18`.
 * @since 0.8.9
 * @modified 2026-09-17 - Added configurable preview connection anchors.
 */
export interface OpeningConnectionAnchor {
  readonly id: string;
  readonly role: OpeningConnectionRole;
  readonly sashEdge: OpeningHingeEdge;
  readonly sashPositionRatio: number;
  readonly frameEdge: OpeningHingeEdge;
  readonly framePositionRatio: number;
}

/** One mechanism/mode entry inside a connection-layout preset. */
export interface OpeningConnectionLayoutEntry {
  readonly mechanism: OpeningMotionMechanism;
  readonly motionMode: OpeningMotionMode;
  readonly anchors: readonly OpeningConnectionAnchor[];
}

/**
 * Replaceable catalog preset consumed identically by SVG and Three.js.
 *
 * A future product-series editor can persist this object after validating it;
 * the current migration supplies only the built-in reference preset. Missing
 * custom entries deliberately fall back to the reference rules so partially
 * configured series remain renderable.
 *
 * @example `{ presetId: "series-a", layouts: [{ mechanism: "tilt_turn", motionMode:
 * "tilt", anchors: [...] }] }` overrides only that series' tilt connections.
 * @since 0.8.9
 * @modified 2026-09-17 - Added future user-defined connection preset contract.
 */
export interface OpeningConnectionLayoutPreset {
  readonly presetId: string;
  readonly layouts: readonly OpeningConnectionLayoutEntry[];
}

/** Fully resolved and validated connection layout for one instantaneous mode. */
export interface ResolvedOpeningConnectionLayout {
  readonly presetId: string;
  readonly mechanism: OpeningMotionMechanism;
  readonly motionMode: OpeningMotionMode;
  readonly anchors: readonly OpeningConnectionAnchor[];
}

/**
 * Resolves the effective motion connections without modifying production BOM.
 *
 * Algorithm: prefer an exact mechanism/mode entry from the optional catalog
 * preset; otherwise create the built-in physical reference layout. Side-hung
 * primary motion uses the declared side, tilt motion uses two bottom pivots and
 * one top stay, and top/bottom hung motion uses the corresponding horizontal
 * edge. Every ratio and identity is validated before renderers receive it.
 *
 * @param input Mechanism, current mode, product hinge side and optional preset.
 * @returns A renderer-neutral layout safe for desktop and mobile projections.
 * @example Tilt-turn `tilt` returns bottom pivots plus a top stay, never side pivots.
 * @since 0.8.9
 * @modified 2026-09-17 - Added built-in presets and custom override seam.
 */
export function resolveOpeningConnectionLayout(input: Readonly<{
  mechanism: OpeningMotionMechanism;
  motionMode?: OpeningMotionMode;
  hingeEdge: OpeningHingeEdge;
  preset?: OpeningConnectionLayoutPreset;
}>): ResolvedOpeningConnectionLayout {
  const motionMode = input.motionMode ?? "primary";
  const custom = input.preset?.layouts.find(
    (entry) => entry.mechanism === input.mechanism && entry.motionMode === motionMode
  );
  if (custom && !input.preset?.presetId.trim()) {
    throw new TypeError("Opening connection preset ID must be non-empty.");
  }
  const anchors = custom?.anchors ?? createReferenceConnectionAnchors(
    input.mechanism,
    motionMode,
    input.hingeEdge
  );
  validateConnectionAnchors(anchors);
  return {
    presetId: custom ? input.preset!.presetId : "doormes-reference-v1",
    mechanism: input.mechanism,
    motionMode,
    anchors: anchors.map((anchor) => ({ ...anchor }))
  };
}

/** Creates the current product-neutral reference layout used before catalog UI exists. */
function createReferenceConnectionAnchors(
  mechanism: OpeningMotionMechanism,
  motionMode: OpeningMotionMode,
  hingeEdge: OpeningHingeEdge
): readonly OpeningConnectionAnchor[] {
  const pairedPivots = (edge: OpeningHingeEdge): readonly OpeningConnectionAnchor[] => [
    {
      id: "pivot-1",
      role: "pivot",
      sashEdge: edge,
      sashPositionRatio: 0.2,
      frameEdge: edge,
      framePositionRatio: 0.2
    },
    {
      id: "pivot-2",
      role: "pivot",
      sashEdge: edge,
      sashPositionRatio: 0.8,
      frameEdge: edge,
      framePositionRatio: 0.8
    }
  ];
  if (mechanism === "tilt_turn" && motionMode === "tilt") {
    return [
      ...pairedPivots("bottom"),
      {
        id: "head-stay",
        role: "stay",
        sashEdge: "top",
        sashPositionRatio: hingeEdge === "left" ? 0.72 : 0.28,
        frameEdge: "top",
        framePositionRatio: hingeEdge === "left" ? 0.55 : 0.45
      }
    ];
  }
  if (mechanism === "top_hung") return pairedPivots("top");
  if (mechanism === "bottom_hung") return pairedPivots("bottom");
  return pairedPivots(hingeEdge);
}

/** Rejects malformed custom catalog entries at the shared boundary. */
function validateConnectionAnchors(anchors: readonly OpeningConnectionAnchor[]): void {
  const ids = new Set<string>();
  if (anchors.length === 0) {
    throw new RangeError("Opening connection layout requires at least one anchor.");
  }
  for (const anchor of anchors) {
    if (!anchor.id || ids.has(anchor.id)) {
      throw new TypeError("Opening connection anchor IDs must be non-empty and unique.");
    }
    ids.add(anchor.id);
    for (const ratio of [anchor.sashPositionRatio, anchor.framePositionRatio]) {
      if (!Number.isFinite(ratio) || ratio < 0 || ratio > 1) {
        throw new RangeError("Opening connection ratios must be finite values from 0 to 1.");
      }
    }
  }
}

/**
 * Creates the stable cross-renderer identity of one movable panel.
 *
 * The containing cell/object ID is paired with the panel ID because `P1` and
 * `P2` are intentionally reused by every opening assembly. The same key is
 * consumed by the preview store, SVG, Three.js and later collision checks.
 *
 * @param objectId Stable opening-cell object ID.
 * @param panelId Stable panel-local ID such as `P1`.
 * @returns A document-wide panel key.
 * @example `createOpeningPanelKey("CELL-1", "P1")` returns `CELL-1::P1`.
 * @since 0.8.0
 * @modified 2026-09-17 - Added shared per-panel preview identity.
 */
export function createOpeningPanelKey(objectId: string, panelId: string): string {
  if (!objectId || !panelId) {
    throw new TypeError("Opening panel identity requires both objectId and panelId.");
  }
  return `${objectId}::${panelId}`;
}

/**
 * Builds a single-step hinge definition for side- or horizontal-axis panels.
 *
 * Algorithm: determine the Euler axis from the physical hinge edge, then apply
 * the plane sign used by the prototype: left/right inward panels rotate toward
 * the room, outward panels reverse; top/bottom axes use the corresponding
 * vertical edge sign. The caller supplies the product-specific maximum angle.
 *
 * @param input Stable panel identity, hinge edge, open plane and maximum angle.
 * @returns One validated rotation definition whose pivot stays renderer-owned.
 * @example A top-out sash with 0.72 radians produces a negative X rotation.
 * @since 0.8.0
 * @modified 2026-09-17 - Extracted current hinge signs from Three.js projection.
 */
export function createHingedOpeningMotion(input: Readonly<{
  motionId: string;
  hingeEdge: OpeningHingeEdge;
  openPlane: "in" | "out";
  maxAngleRadians: number;
}>): OpeningMotionDefinition {
  if (!Number.isFinite(input.maxAngleRadians) || input.maxAngleRadians < 0) {
    throw new RangeError("Opening maximum angle must be a finite non-negative value.");
  }
  const outward = input.openPlane === "out";
  let deltaRadians: OpeningMotionVector3;
  if (input.hingeEdge === "top" || input.hingeEdge === "bottom") {
    const edgeSign = input.hingeEdge === "top" ? -1 : 1;
    const direction = edgeSign * (outward ? 1 : -1);
    deltaRadians = { x: direction * input.maxAngleRadians, y: 0, z: 0 };
  } else {
    const edgeSign = input.hingeEdge === "left" ? 1 : -1;
    const direction = edgeSign * (outward ? -1 : 1);
    deltaRadians = { x: 0, y: direction * input.maxAngleRadians, z: 0 };
  }
  return {
    motionId: input.motionId,
    steps: [{
      id: `${input.motionId}:hinge`,
      kind: "rotation",
      startProgress: 0,
      endProgress: 100,
      deltaRadians
    }]
  };
}

/**
 * Builds the renderer-neutral translation used by an ordinary sliding sash.
 *
 * The stored travel is a positive physical distance in millimetres; direction
 * supplies its sign in the shared local X axis (left negative, right positive).
 * Track assignment, overlap, interlock profiles and usable clear opening remain
 * product geometry concerns and are deliberately not inferred here. Keeping the
 * motion this small lets SVG, Three.js, collision checks and mobile playback
 * consume exactly the same pose without treating a sliding sash as a zero-angle
 * hinged leaf.
 *
 * @param input Stable motion identity, horizontal direction and full travel.
 * @returns One validated translation step spanning the normalized preview path.
 * @throws When the identity is blank or travel is not a positive finite value.
 * @example A left-moving sash with 620mm travel ends at `x = -620`.
 * @since 0.11.2
 */
export function createSlidingOpeningMotion(input: Readonly<{
  motionId: string;
  direction: SlidingTravelDirection;
  travelMm: number;
}>): OpeningMotionDefinition {
  if (!input.motionId.trim()) {
    throw new TypeError("Sliding opening motion ID must be non-empty.");
  }
  if (!Number.isFinite(input.travelMm) || input.travelMm <= 0) {
    throw new RangeError("Sliding opening travel must be a positive finite millimetre value.");
  }
  const signedTravelMm = input.direction === "left" ? -input.travelMm : input.travelMm;
  return {
    motionId: input.motionId,
    steps: [{
      id: `${input.motionId}:slide`,
      kind: "translation",
      startProgress: 0,
      endProgress: 100,
      deltaMm: { x: signedTravelMm, y: 0, z: 0 }
    }]
  };
}

/**
 * Creates the canonical strategy for the migrated rotating mechanisms.
 *
 * The reference side-hung path supports a 90-degree rotation. Tilt and
 * horizontal-hung paths retain their smaller hardware-limited reference angles
 * (0.32rad, 0.72rad and 0.42rad). A renderer establishes the declared physical
 * pivot; this function supplies only device-neutral motion. A persisted product
 * limit overrides the reference value without changing the normalized runtime
 * progress contract.
 *
 * @param input Mechanism, requested mode and physical opening direction.
 * @returns One normalized motion definition suitable for every renderer.
 * @example A tilt-turn `tilt` mode resolves to a bottom/inward 0.32rad hinge.
 * @since 0.8.1
 * @modified 2026-09-17 - Set the confirmed reference side-hung limit to 90 degrees.
 */
export function createOpeningMechanismMotion(input: Readonly<{
  motionId: string;
  mechanism: OpeningMotionMechanism;
  motionMode?: OpeningMotionMode;
  hingeEdge: OpeningHingeEdge;
  openPlane: "in" | "out";
  maximumAngleDegrees?: number;
}>): OpeningMotionDefinition {
  if (input.maximumAngleDegrees !== undefined) {
    validateMaximumOpeningAngle(input.maximumAngleDegrees);
  }
  const configuredRadians = input.maximumAngleDegrees === undefined
    ? undefined
    : input.maximumAngleDegrees * Math.PI / 180;
  if (input.mechanism === "tilt_turn" && input.motionMode === "tilt") {
    return createHingedOpeningMotion({
      motionId: input.motionId,
      hingeEdge: "bottom",
      openPlane: "in",
      maxAngleRadians: configuredRadians ?? 0.32
    });
  }
  if (input.mechanism === "top_hung") {
    return createHingedOpeningMotion({
      motionId: input.motionId,
      hingeEdge: "top",
      openPlane: input.openPlane,
      maxAngleRadians: configuredRadians ?? 0.72
    });
  }
  if (input.mechanism === "bottom_hung") {
    return createHingedOpeningMotion({
      motionId: input.motionId,
      hingeEdge: "bottom",
      openPlane: input.openPlane,
      maxAngleRadians: configuredRadians ?? 0.42
    });
  }
  return createHingedOpeningMotion({
    motionId: input.motionId,
    hingeEdge: input.hingeEdge,
    openPlane: input.openPlane,
    maxAngleRadians: configuredRadians ?? Math.PI / 2
  });
}

/**
 * Samples a dashed-angle arc shared by SVG and Three.js annotations.
 *
 * Algorithm: subdivide the signed sweep into no more than 7.5-degree chords,
 * while always returning both endpoints. Renderers map the generic radial
 * plane into X/Z for side hinges or Y/Z for horizontal hinges, so the measured
 * angle and visible arc cannot drift apart between 2D and 3D.
 *
 * @param input Radius, starting polar angle and signed sweep in radians.
 * @returns Ordered radial-plane points including the start and current ray.
 * @example A `Math.PI / 2` sweep yields a complete 90-degree quarter-circle.
 * @since 0.9.2
 * @modified 2026-09-17 - Added cross-renderer opening-angle annotation geometry.
 */
export function sampleOpeningAngleArc(input: Readonly<{
  radius: number;
  startAngleRadians: number;
  sweepAngleRadians: number;
}>): readonly OpeningAngleArcPoint[] {
  if (!Number.isFinite(input.radius) || input.radius <= 0) {
    throw new RangeError("Opening angle arc radius must be a finite positive value.");
  }
  if (!Number.isFinite(input.startAngleRadians) || !Number.isFinite(input.sweepAngleRadians)) {
    throw new RangeError("Opening angle arc angles must be finite values.");
  }
  const segmentCount = Math.max(1, Math.ceil(Math.abs(input.sweepAngleRadians) / (Math.PI / 24)));
  return Array.from({ length: segmentCount + 1 }, (_, index) => {
    const ratio = index / segmentCount;
    const angle = input.startAngleRadians + input.sweepAngleRadians * ratio;
    return {
      radialX: Math.cos(angle) * input.radius,
      radialY: Math.sin(angle) * input.radius
    };
  });
}

/**
 * Returns the physical full-open angle of one configured rotation strategy.
 *
 * The UI must expose degrees instead of the animation kernel's normalized
 * percentage. This helper resolves the same strategy used by SVG/Three and
 * measures its fully applied Euler rotation, preventing labels and geometry
 * from acquiring separate hard-coded limits.
 *
 * @param input Mechanism, mode and physical direction used by the renderer.
 * @returns Full-open physical rotation in degrees.
 * @example Tilt-turn `tilt` returns about `18.335` degrees (`0.32rad`).
 * @since 0.9.0
 * @modified 2026-09-17 - Added the shared degree-facing control boundary.
 */
export function resolveOpeningMaximumAngleDegrees(input: Readonly<{
  mechanism: OpeningMotionMechanism;
  motionMode?: OpeningMotionMode;
  hingeEdge: OpeningHingeEdge;
  openPlane: "in" | "out";
  maximumAngleDegrees?: number;
}>): number {
  const definition = createOpeningMechanismMotion({
    motionId: "opening-angle-limit",
    ...input
  });
  const pose = resolveOpeningPose(definition, 100);
  const radians = Math.max(
    Math.abs(pose.rotationRadians.x),
    Math.abs(pose.rotationRadians.y),
    Math.abs(pose.rotationRadians.z)
  );
  return radians * 180 / Math.PI;
}

/**
 * Converts renderer/controller progress into a user-facing physical angle.
 *
 * @param progressPercent Normalized animation progress in the inclusive 0..100 range.
 * @param maximumAngleDegrees Physical full-open limit for the selected mechanism mode.
 * @returns Clamped instantaneous angle in degrees.
 * @example `50, 18.335` returns about `9.168` degrees.
 * @since 0.9.0
 * @modified 2026-09-17 - Added percentage-to-angle conversion for controls.
 */
export function openingProgressToAngleDegrees(
  progressPercent: number,
  maximumAngleDegrees: number
): number {
  validateMaximumOpeningAngle(maximumAngleDegrees);
  return clampProgress(progressPercent) / 100 * maximumAngleDegrees;
}

/**
 * Converts a requested physical angle back to normalized controller progress.
 *
 * Rotation views consume the resulting progress through the existing shared
 * pose solver, so an entered 12° value produces exactly 12° in SVG and Three.
 * Values outside the mechanism limit are clamped instead of destabilizing the
 * preview; design validation can apply stricter catalog rules when persisted.
 *
 * @param angleDegrees User-entered current angle.
 * @param maximumAngleDegrees Physical limit of the current mode.
 * @returns Normalized progress accepted by the preview store.
 * @example `9, 18` returns `50`.
 * @since 0.9.0
 * @modified 2026-09-17 - Added angle-to-percentage preview adaptation.
 */
export function openingAngleDegreesToProgress(
  angleDegrees: number,
  maximumAngleDegrees: number
): number {
  validateMaximumOpeningAngle(maximumAngleDegrees);
  if (!Number.isFinite(angleDegrees)) return 0;
  return Math.min(maximumAngleDegrees, Math.max(0, angleDegrees)) /
    maximumAngleDegrees * 100;
}

/** Prevents invalid product limits from being mistaken for a closed sash. */
function validateMaximumOpeningAngle(maximumAngleDegrees: number): void {
  if (!Number.isFinite(maximumAngleDegrees) || maximumAngleDegrees <= 0 || maximumAngleDegrees > 180) {
    throw new RangeError("Opening maximum angle must be a finite value above 0 and at most 180 degrees.");
  }
}

/**
 * Resolves a mechanism definition into a deterministic instantaneous pose.
 *
 * Algorithm: clamp requested progress to 0..100, map it into every step's own
 * interval, then add translations/rotations and interpolate scale from one.
 * Earlier completed steps remain fully applied while later steps remain at
 * zero. This supports lift-then-slide and release-then-slide without renderer
 * conditionals; overlapping intervals intentionally allow coordinated motion.
 *
 * @param definition Pure motion steps for one panel and mode.
 * @param progressPercent Requested runtime preview progress.
 * @returns New immutable-compatible pose with no mutation of design data.
 * @example A step spanning 10..20% is half applied at global progress 15%.
 * @since 0.8.0
 * @modified 2026-09-17 - Added first shared pose-solving algorithm.
 */
export function resolveOpeningPose(
  definition: OpeningMotionDefinition,
  progressPercent: number
): OpeningPoseSnapshot {
  const progress = clampProgress(progressPercent);
  const translation = { x: 0, y: 0, z: 0 };
  const rotation = { x: 0, y: 0, z: 0 };
  const scale = { x: 1, y: 1, z: 1 };
  const activeStepIds: string[] = [];
  for (const step of definition.steps) {
    validateStep(step);
    const local = stepProgress(step.startProgress, step.endProgress, progress);
    if (local > 0 && local < 1) activeStepIds.push(step.id);
    if (step.kind === "translation") {
      translation.x += step.deltaMm.x * local;
      translation.y += step.deltaMm.y * local;
      translation.z += step.deltaMm.z * local;
    } else if (step.kind === "rotation") {
      rotation.x += step.deltaRadians.x * local;
      rotation.y += step.deltaRadians.y * local;
      rotation.z += step.deltaRadians.z * local;
    } else {
      scale.x *= 1 + (step.targetScale.x - 1) * local;
      scale.y *= 1 + (step.targetScale.y - 1) * local;
      scale.z *= 1 + (step.targetScale.z - 1) * local;
    }
  }
  return {
    motionId: definition.motionId,
    progressPercent: progress,
    translationMm: translation,
    rotationRadians: rotation,
    scale,
    activeStepIds
  };
}

/**
 * Samples a complete motion path including its exact closed and open poses.
 *
 * The result is deterministic and timing-independent. Animation controllers
 * may interpolate time separately, while SVG plan view and collision auditing
 * can inspect identical progress points.
 *
 * @param definition Pure motion definition.
 * @param intervalCount Number of equal intervals; produces one extra endpoint.
 * @returns Pose snapshots from 0 through 100 percent.
 * @example `sampleOpeningMotion(definition, 4)` returns five snapshots.
 * @since 0.8.1
 * @modified 2026-09-17 - Added renderer-neutral path sampling.
 */
export function sampleOpeningMotion(
  definition: OpeningMotionDefinition,
  intervalCount = 20
): readonly OpeningPoseSnapshot[] {
  if (!Number.isInteger(intervalCount) || intervalCount < 1 || intervalCount > 1000) {
    throw new RangeError("Opening motion intervalCount must be an integer from 1 to 1000.");
  }
  return Array.from({ length: intervalCount + 1 }, (_, index) =>
    resolveOpeningPose(definition, (index / intervalCount) * 100)
  );
}

/**
 * Finds the first sampled collision with every stationary axis-aligned obstacle.
 *
 * Algorithm: build the moving sash as an oriented bounding box from its real
 * pivot-local bounds, apply the exact shared pose at each sample, then run the
 * complete 15-axis OBB/AABB separating-axis test (three sash axes, three world
 * axes and nine cross products). This is materially tighter than comparing a
 * swept AABB, which would report false wall strikes throughout a 90-degree arc.
 * Touching faces are not treated as penetration; `minimumPenetrationMm` absorbs
 * floating-point noise and may later be raised by a product clearance policy.
 *
 * The function knows nothing about walls, BOM or Three.js. Callers provide the
 * same pivot origin and stationary volumes used by their geometry model, so
 * side-hung, tilt, top-hung and future multi-stage motions share one audit path.
 *
 * @param input Motion definition, real sash bounds/origin and obstacle volumes.
 * @returns At most one record per obstacle, ordered like the input obstacles.
 * @example A side-hung sash can first meet a deep reveal at 63% of its path.
 * @since 0.10.0
 * @modified 2026-09-17 - Added renderer-independent installation collision sampling.
 */
export function findOpeningMotionCollisions(input: Readonly<{
  definition: OpeningMotionDefinition;
  localBounds: OpeningMotionLocalBoundsMm;
  originMm: OpeningMotionVector3;
  obstacles: readonly OpeningMotionAxisAlignedObstacleMm[];
  intervalCount?: number;
  minimumPenetrationMm?: number;
}>): readonly OpeningMotionCollisionSample[] {
  validateBounds(input.localBounds);
  validateVector(input.originMm, "Opening motion collision origin");
  const minimumPenetrationMm = input.minimumPenetrationMm ?? 0.1;
  if (!Number.isFinite(minimumPenetrationMm) || minimumPenetrationMm < 0) {
    throw new RangeError("Opening collision minimum penetration must be finite and non-negative.");
  }
  const obstacleIds = new Set<string>();
  for (const obstacle of input.obstacles) {
    if (!obstacle.obstacleId.trim() || obstacleIds.has(obstacle.obstacleId)) {
      throw new TypeError("Opening collision obstacle IDs must be non-empty and unique.");
    }
    obstacleIds.add(obstacle.obstacleId);
    validateBounds(obstacle);
  }

  const firstByObstacleId = new Map<string, OpeningMotionCollisionSample>();
  for (const pose of sampleOpeningMotion(input.definition, input.intervalCount ?? 180)) {
    if (firstByObstacleId.size === input.obstacles.length) break;
    for (const obstacle of input.obstacles) {
      if (firstByObstacleId.has(obstacle.obstacleId)) continue;
      const penetrationMm = orientedBoundsPenetrationMm(
        input.localBounds,
        input.originMm,
        pose,
        obstacle,
        minimumPenetrationMm
      );
      if (penetrationMm === undefined) continue;
      firstByObstacleId.set(obstacle.obstacleId, {
        obstacleId: obstacle.obstacleId,
        progressPercent: pose.progressPercent,
        pose,
        penetrationMm
      });
    }
  }
  return input.obstacles.flatMap((obstacle) => {
    const collision = firstByObstacleId.get(obstacle.obstacleId);
    return collision ? [collision] : [];
  });
}

/** Exact separating-axis overlap for one transformed local box and world AABB. */
function orientedBoundsPenetrationMm(
  localBounds: OpeningMotionLocalBoundsMm,
  originMm: OpeningMotionVector3,
  pose: OpeningPoseSnapshot,
  obstacle: OpeningMotionAxisAlignedObstacleMm,
  minimumPenetrationMm: number
): number | undefined {
  const localCenter = {
    x: (localBounds.min.x + localBounds.max.x) / 2,
    y: (localBounds.min.y + localBounds.max.y) / 2,
    z: (localBounds.min.z + localBounds.max.z) / 2
  };
  const transformedCenter = transformOpeningMotionPoint(localCenter, pose);
  const movingCenter = addVectors(originMm, transformedCenter);
  const movingAxes = [
    rotateOpeningMotionVector({ x: 1, y: 0, z: 0 }, pose.rotationRadians),
    rotateOpeningMotionVector({ x: 0, y: 1, z: 0 }, pose.rotationRadians),
    rotateOpeningMotionVector({ x: 0, y: 0, z: 1 }, pose.rotationRadians)
  ] as const;
  const movingHalfExtents = [
    (localBounds.max.x - localBounds.min.x) / 2 * Math.abs(pose.scale.x),
    (localBounds.max.y - localBounds.min.y) / 2 * Math.abs(pose.scale.y),
    (localBounds.max.z - localBounds.min.z) / 2 * Math.abs(pose.scale.z)
  ] as const;
  const obstacleCenter = {
    x: (obstacle.min.x + obstacle.max.x) / 2,
    y: (obstacle.min.y + obstacle.max.y) / 2,
    z: (obstacle.min.z + obstacle.max.z) / 2
  };
  const obstacleHalfExtents = [
    (obstacle.max.x - obstacle.min.x) / 2,
    (obstacle.max.y - obstacle.min.y) / 2,
    (obstacle.max.z - obstacle.min.z) / 2
  ] as const;
  const worldAxes = [
    { x: 1, y: 0, z: 0 },
    { x: 0, y: 1, z: 0 },
    { x: 0, y: 0, z: 1 }
  ] as const;
  const separatingAxes: OpeningMotionVector3[] = [...movingAxes, ...worldAxes];
  for (const movingAxis of movingAxes) {
    for (const worldAxis of worldAxes) separatingAxes.push(cross(movingAxis, worldAxis));
  }
  const centreDelta = subtractVectors(obstacleCenter, movingCenter);
  let minimumOverlap = Infinity;
  for (const rawAxis of separatingAxes) {
    const axis = normalizeVector(rawAxis);
    if (!axis) continue;
    const movingRadius = movingAxes.reduce(
      (sum, movingAxis, index) => sum + movingHalfExtents[index]! * Math.abs(dot(axis, movingAxis)),
      0
    );
    const obstacleRadius = worldAxes.reduce(
      (sum, worldAxis, index) => sum + obstacleHalfExtents[index]! * Math.abs(dot(axis, worldAxis)),
      0
    );
    const overlap = movingRadius + obstacleRadius - Math.abs(dot(centreDelta, axis));
    if (overlap <= minimumPenetrationMm) return undefined;
    minimumOverlap = Math.min(minimumOverlap, overlap);
  }
  return Math.round(minimumOverlap * 1000) / 1000;
}

/** Rotates a direction using the same XYZ Euler order as point transformation. */
function rotateOpeningMotionVector(
  vector: OpeningMotionVector3,
  rotation: OpeningMotionVector3
): OpeningMotionVector3 {
  let { x, y, z } = vector;
  const cosX = Math.cos(rotation.x);
  const sinX = Math.sin(rotation.x);
  [y, z] = [y * cosX - z * sinX, y * sinX + z * cosX];
  const cosY = Math.cos(rotation.y);
  const sinY = Math.sin(rotation.y);
  [x, z] = [x * cosY + z * sinY, -x * sinY + z * cosY];
  const cosZ = Math.cos(rotation.z);
  const sinZ = Math.sin(rotation.z);
  [x, y] = [x * cosZ - y * sinZ, x * sinZ + y * cosZ];
  return { x, y, z };
}

/** Minimal vector helpers keep the motion package independent from Three.js. */
function addVectors(a: OpeningMotionVector3, b: OpeningMotionVector3): OpeningMotionVector3 {
  return { x: a.x + b.x, y: a.y + b.y, z: a.z + b.z };
}

function subtractVectors(a: OpeningMotionVector3, b: OpeningMotionVector3): OpeningMotionVector3 {
  return { x: a.x - b.x, y: a.y - b.y, z: a.z - b.z };
}

function dot(a: OpeningMotionVector3, b: OpeningMotionVector3): number {
  return a.x * b.x + a.y * b.y + a.z * b.z;
}

function cross(a: OpeningMotionVector3, b: OpeningMotionVector3): OpeningMotionVector3 {
  return {
    x: a.y * b.z - a.z * b.y,
    y: a.z * b.x - a.x * b.z,
    z: a.x * b.y - a.y * b.x
  };
}

function normalizeVector(vector: OpeningMotionVector3): OpeningMotionVector3 | undefined {
  const length = Math.hypot(vector.x, vector.y, vector.z);
  if (length < 1e-9) return undefined;
  return { x: vector.x / length, y: vector.y / length, z: vector.z / length };
}

/**
 * Calculates the axis-aligned volume swept by local panel bounds.
 *
 * Algorithm: sample the same pose path used by renderers, transform all eight
 * corners by scale, XYZ Euler rotation and translation, then accumulate minimum
 * and maximum coordinates. Callers choose a denser sample count for production
 * collision audits; preview guides can use the default twenty intervals.
 *
 * @param definition Shared motion definition.
 * @param localBounds Closed panel bounds relative to its real pivot, in mm.
 * @param intervalCount Equal motion intervals used for conservative sampling.
 * @returns Sampled swept bounds in the same pivot-relative coordinate system.
 * @example A side-hung sash sweeps from its closed X span into the Z axis.
 * @since 0.8.1
 * @modified 2026-09-17 - Added first common motion-envelope calculation.
 */
export function calculateOpeningMotionEnvelope(
  definition: OpeningMotionDefinition,
  localBounds: OpeningMotionLocalBoundsMm,
  intervalCount = 20
): OpeningMotionEnvelopeMm {
  validateBounds(localBounds);
  const minimum = { x: Infinity, y: Infinity, z: Infinity };
  const maximum = { x: -Infinity, y: -Infinity, z: -Infinity };
  const corners = createBoundsCorners(localBounds);
  const poses = sampleOpeningMotion(definition, intervalCount);
  for (const pose of poses) {
    for (const corner of corners) {
      const transformed = transformOpeningMotionPoint(corner, pose);
      minimum.x = Math.min(minimum.x, transformed.x);
      minimum.y = Math.min(minimum.y, transformed.y);
      minimum.z = Math.min(minimum.z, transformed.z);
      maximum.x = Math.max(maximum.x, transformed.x);
      maximum.y = Math.max(maximum.y, transformed.y);
      maximum.z = Math.max(maximum.z, transformed.z);
    }
  }
  return { min: minimum, max: maximum, sampleCount: poses.length };
}

/** Transforms one pivot-local point with the same XYZ Euler convention as Three.js. */
export function transformOpeningMotionPoint(
  point: OpeningMotionVector3,
  pose: OpeningPoseSnapshot
): OpeningMotionVector3 {
  let x = point.x * pose.scale.x;
  let y = point.y * pose.scale.y;
  let z = point.z * pose.scale.z;
  const cosX = Math.cos(pose.rotationRadians.x);
  const sinX = Math.sin(pose.rotationRadians.x);
  [y, z] = [y * cosX - z * sinX, y * sinX + z * cosX];
  const cosY = Math.cos(pose.rotationRadians.y);
  const sinY = Math.sin(pose.rotationRadians.y);
  [x, z] = [x * cosY + z * sinY, -x * sinY + z * cosY];
  const cosZ = Math.cos(pose.rotationRadians.z);
  const sinZ = Math.sin(pose.rotationRadians.z);
  [x, y] = [x * cosZ - y * sinZ, x * sinZ + y * cosZ];
  return {
    x: x + pose.translationMm.x,
    y: y + pose.translationMm.y,
    z: z + pose.translationMm.z
  };
}

/** Enumerates all corner combinations of a validated axis-aligned box. */
function createBoundsCorners(bounds: OpeningMotionLocalBoundsMm): OpeningMotionVector3[] {
  const result: OpeningMotionVector3[] = [];
  for (const x of [bounds.min.x, bounds.max.x]) {
    for (const y of [bounds.min.y, bounds.max.y]) {
      for (const z of [bounds.min.z, bounds.max.z]) result.push({ x, y, z });
    }
  }
  return result;
}

/** Prevents inverted or non-finite panel geometry from poisoning envelope results. */
function validateBounds(bounds: OpeningMotionLocalBoundsMm): void {
  for (const axis of ["x", "y", "z"] as const) {
    if (
      !Number.isFinite(bounds.min[axis]) ||
      !Number.isFinite(bounds.max[axis]) ||
      bounds.min[axis] > bounds.max[axis]
    ) {
      throw new RangeError(`Opening local ${axis} bounds must be finite and ordered.`);
    }
  }
}

/** Rejects non-finite origins before they can make every SAT comparison false. */
function validateVector(vector: OpeningMotionVector3, label: string): void {
  if (![vector.x, vector.y, vector.z].every(Number.isFinite)) {
    throw new RangeError(`${label} must contain finite coordinates.`);
  }
}

/** Clamps untrusted preview input without allowing NaN into render transforms. */
function clampProgress(value: number): number {
  if (!Number.isFinite(value)) return 0;
  return Math.min(100, Math.max(0, value));
}

/** Maps global progress into one inclusive step interval. */
function stepProgress(start: number, end: number, progress: number): number {
  if (progress <= start) return 0;
  if (progress >= end) return 1;
  return (progress - start) / (end - start);
}

/** Rejects malformed steps before a renderer can receive unstable transforms. */
function validateStep(step: OpeningMotionStep): void {
  if (
    !Number.isFinite(step.startProgress) ||
    !Number.isFinite(step.endProgress) ||
    step.startProgress < 0 ||
    step.endProgress > 100 ||
    step.endProgress <= step.startProgress
  ) {
    throw new RangeError(`Motion step ${step.id} must span an increasing interval within 0..100.`);
  }
}
