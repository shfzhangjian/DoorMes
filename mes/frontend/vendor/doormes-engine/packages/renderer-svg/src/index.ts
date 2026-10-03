import type {
  DesignDocument,
  DrawingTextLabel,
  HardwareComponentModelSnapshot,
  VisualAppearanceSnapshot,
  WindowUnit,
  WindowVisualConfiguration
} from "@doormes/contracts";
import {
  diagnoseWindowVisualAssets,
  resolveAppearanceRenderStyle,
  resolveHardwareComponentModel,
  resolveWindowVisualConfigurationForRender,
  type VisualAssetAvailability,
  type VisualAssetDiagnostic
} from "@doormes/appearance-model";
import {
  layoutPaperSpaceDimensions,
  type DrawingLinearDimensionAnnotation,
  type DrawingView
} from "@doormes/drawing-model";
import {
  createWindowInstallationSurroundObjectId,
  createWindowInstallationWallObjectId,
  normalizeWindowInstallation,
  partitionTopologyRegion,
  resolveFabricationAssemblyGeometry,
  resolveInstallationSectionFromFrameDepth,
  resolveInstallationSubjects,
  resolveWindowGeometry,
  resolveWindowInstallationObstacleGeometry,
  resolveWindowInstallationSection,
  resolveWindowSectionDimensions,
  type ResolvedFabricationAssemblyGeometry
} from "@doormes/geometry-topology";
import {
  createOpeningMechanismMotion,
  createOpeningPanelKey,
  createSlidingOpeningMotion,
  resolveOpeningConnectionLayout,
  resolveOpeningMaximumAngleDegrees,
  resolveOpeningPose,
  sampleOpeningAngleArc,
  transformOpeningMotionPoint,
  type OpeningConnectionLayoutPreset,
  type OpeningConnectionRole,
  type OpeningHingeEdge,
  type OpeningMotionMode,
  type OpeningMotionVector3
} from "@doormes/opening-kinematics";

/**
 * Visual-only 2D drawing presentation.
 *
 * Both values consume identical geometry, IDs, dimensions and opening poses.
 * The engineering style suppresses catalogue material paint and is not a
 * substitute for a reviewed factory drawing or manufacturing release.
 *
 * @example `renderDesignSvg(document, { renderStyle: "engineering-line" })`.
 * @since 0.10.71
 * @modified 2026-09-22 - Added UI-2D-008 dual-style SVG rendering.
 */
export type SvgRenderStyle = "material" | "engineering-line";

/**
 * Controls deterministic SVG sizing without exposing browser DOM nodes to the
 * shared design model.
 *
 * @example `{ viewportWidth: 960, viewportHeight: 640, padding: 48 }`.
 * @since 0.1.0
 * @modified 2026-09-17 - Added first shared SVG render options.
 */
export interface SvgRenderOptions {
  readonly viewportWidth?: number;
  readonly viewportHeight?: number;
  readonly padding?: number;
  readonly selectedObjectId?: string;
  readonly viewport?: Readonly<{ scale: number; x: number; y: number }>;
  readonly showDimensions?: boolean;
  readonly showPlanView?: boolean;
  readonly showOpeningState?: boolean;
  /** Presentation-only material preview or monochrome engineering linework. */
  readonly renderStyle?: SvgRenderStyle;
  readonly openingProgressPercentByPanelKey?: Readonly<Record<string, number>>;
  readonly openingMotionModeByPanelKey?: Readonly<Record<string, OpeningMotionMode>>;
  readonly openingConnectionPresetByPanelKey?: Readonly<Record<string, OpeningConnectionLayoutPreset>>;
  /** Immutable availability used for texture/model missing and hash diagnostics. */
  readonly visualAssets?: VisualAssetAvailability;
}

/**
 * Default engineering canvas contract shared by desktop and mobile shells.
 *
 * The 64-unit safety border leaves room for dimensions without reproducing the
 * prototype's excessive left/top letterbox. Callers may override it for export.
 *
 * @example `renderDesignSvg(document)` uses a 960×640 view with 64 units padding.
 * @since 0.1.0
 * @modified 2026-09-17 - Reduced interactive-view padding for the 0.5.3 canvas fix.
 */
const DEFAULT_OPTIONS = {
  viewportWidth: 960,
  viewportHeight: 640,
  padding: 64,
  showDimensions: true,
  showPlanView: false,
  showOpeningState: false,
  renderStyle: "material"
} as const;

/**
 * Self-contained linework overrides embedded in engineering SVG output.
 *
 * Algorithm: material-bearing product surfaces are normalised to white fill
 * and weighted dark outlines. Engineering symbols, angle constructions,
 * orientation labels and plan motion use monochrome line types; invalid
 * diagnostics and temporary selection remain coloured interaction overlays.
 * The original appearance snapshot is neither changed nor copied into geometry;
 * switching back to `material` immediately restores the catalogue preview.
 *
 * @example A wood-textured surround retains its object/material metadata but
 * renders as white linework in the engineering view.
 * @since 0.10.71
 * @modified 2026-09-22 - Added monochrome semantic lines for printing.
 */
const ENGINEERING_LINE_STYLE = `<style class="design-svg__engineering-line-style">
.design-svg--engineering-line .design-window__outline,
.design-svg--engineering-line .design-window__surround,
.design-svg--engineering-line .design-window__frame-segment,
.design-svg--engineering-line .design-window__meeting-mullion,
.design-svg--engineering-line .design-window__member,
.design-svg--engineering-line .design-window__opening-state,
.design-svg--engineering-line .design-window__opening-side-face,
.design-svg--engineering-line .design-assembly__joint,
.design-svg--engineering-line .design-assembly__surround {
  fill: #ffffff !important;
  fill-opacity: 1 !important;
  stroke: #111827 !important;
}
.design-svg--engineering-line .design-window__opening-state {
  stroke-width: 1.5 !important;
  vector-effect: non-scaling-stroke;
}
.design-svg--engineering-line .design-window__glass,
.design-svg--engineering-line .design-window__opening-glass,
.design-svg--engineering-line .design-window__sliding-glass,
.design-svg--engineering-line .design-plan-view__sliding-panel,
.design-svg--engineering-line .design-plan-view__wall,
.design-svg--engineering-line .design-plan-view__surround,
.design-svg--engineering-line .design-plan-view__frame,
.design-svg--engineering-line .design-assembly-plan-view__wall,
.design-svg--engineering-line .design-assembly-plan-view__wall-plane,
.design-svg--engineering-line .design-assembly-plan-view__surround,
.design-svg--engineering-line .design-assembly-plan-view__frame,
.design-svg--engineering-line .design-assembly-plan-view__glass,
.design-svg--engineering-line .design-assembly-plan-view__joint {
  fill: #ffffff !important;
  fill-opacity: 1 !important;
  stroke: #374151 !important;
}
.design-svg--engineering-line .design-window__hardware > *,
.design-svg--engineering-line .design-window__opening-moving-hardware > * {
  fill: #ffffff !important;
  fill-opacity: 1 !important;
  stroke: #111827 !important;
}
.design-svg--engineering-line .design-window__hardware,
.design-svg--engineering-line .design-window__opening-connection circle {
  fill: #ffffff !important;
  fill-opacity: 1 !important;
  stroke: #111827 !important;
}
.design-svg--engineering-line .design-assembly__joint-detail {
  fill: none !important;
  stroke: #111827 !important;
  stroke-width: 1.25 !important;
  vector-effect: non-scaling-stroke;
}
.design-svg--engineering-line .design-window__opening-sash-outline {
  fill: none !important;
  stroke: transparent !important;
  stroke-opacity: 0 !important;
  pointer-events: stroke;
}
.design-svg--engineering-line .design-window__sliding-sash-outline {
  fill: none !important;
  stroke: transparent !important;
  stroke-opacity: 0 !important;
  pointer-events: stroke;
}
.design-svg--engineering-line .design-window__opening-sash-linework {
  display: inline !important;
  fill: none !important;
  stroke: #111827 !important;
  stroke-width: 1.35 !important;
  vector-effect: non-scaling-stroke;
}
.design-svg--engineering-line .design-window__sliding-sash-linework {
  fill: none !important;
  stroke: #111827 !important;
  stroke-width: 1.35 !important;
  vector-effect: non-scaling-stroke;
}
.design-svg--engineering-line .design-window__sliding-direction,
.design-svg--engineering-line .design-plan-view__sliding-track {
  stroke: #111827 !important;
}
.design-svg--engineering-line .design-window__opening-handle,
.design-svg--engineering-line .design-window__opening-flying-mullion,
.design-svg--engineering-line .design-window__opening-transmission-rod,
.design-svg--engineering-line .design-window__opening-connection-stay {
  stroke: #111827 !important;
}
.design-svg--engineering-line .design-window__glass-symbol {
  stroke: #374151 !important;
  stroke-opacity: 1 !important;
}
.design-svg--engineering-line .design-window__member--invalid {
  fill: #fee2e2 !important;
  stroke: #dc2626 !important;
}
.design-svg--engineering-line .design-window__opening-symbol {
  stroke: #111827 !important;
}
.design-svg--engineering-line .design-window__opening-symbol-halo {
  stroke: #ffffff !important;
}
.design-svg--engineering-line .design-window__opening-plane-arrow__shaft {
  stroke: #111827 !important;
}
.design-svg--engineering-line .design-window__opening-plane-arrow__head {
  fill: #111827 !important;
  stroke: #ffffff !important;
}
.design-svg--engineering-line .design-window__opening-angle line,
.design-svg--engineering-line .design-window__opening-angle path,
.design-svg--engineering-line .design-window__opening-angle-plan-glyph line,
.design-svg--engineering-line .design-window__opening-angle-plan-glyph path,
.design-svg--engineering-line .design-plan-view__opening-angle-mark line,
.design-svg--engineering-line .design-plan-view__opening-angle-mark path,
.design-svg--engineering-line .design-assembly-plan-view__opening-angle-mark line,
.design-svg--engineering-line .design-assembly-plan-view__opening-angle-mark path {
  stroke: #111827 !important;
  fill: none !important;
}
.design-svg--engineering-line .design-window__opening-angle text,
.design-svg--engineering-line .design-window__opening-angle-plan-glyph text,
.design-svg--engineering-line .design-plan-view__opening-angle-mark text,
.design-svg--engineering-line .design-assembly-plan-view__opening-angle-mark text {
  stroke: none !important;
  fill: #111827 !important;
}
.design-svg--engineering-line .design-window__opening-angle-value rect {
  fill: #ffffff !important;
  stroke: #6b7280 !important;
}
.design-svg--engineering-line .design-window__opening-angle-value text,
.design-svg--engineering-line .design-plan-view__opening-angle {
  fill: #111827 !important;
}
.design-svg--engineering-line .design-plan-view__opening-state,
.design-svg--engineering-line .design-assembly-plan-view__opening-state {
  fill: none !important;
  stroke: #111827 !important;
}
.design-svg--engineering-line .design-plan-view__side-label,
.design-svg--engineering-line .design-window__facade-orientation text,
.design-svg--engineering-line .design-assembly__facade-orientation text {
  fill: #111827 !important;
}
.design-svg--engineering-line .design-window__facade-orientation line,
.design-svg--engineering-line .design-assembly__facade-orientation line {
  stroke: #9ca3af !important;
}
.design-svg--engineering-line .design-plan-view__glass {
  fill: #ffffff !important;
  fill-opacity: 1 !important;
  stroke: #374151 !important;
}
.design-svg--engineering-line .design-plan-view__frame-reference,
.design-svg--engineering-line .design-plan-view__frame-offset {
  stroke: #111827 !important;
}
.design-svg--engineering-line .design-plan-view__wall-centerline {
  stroke: #6b7280 !important;
}
.design-svg--engineering-line .design-plan-view__frame-position-label,
.design-svg--engineering-line .design-plan-view__wall-thickness-label {
  fill: #111827 !important;
}
.design-svg--engineering-line .design-profile-annotation__face-dimension line {
  stroke: #111827 !important;
}
.design-svg--engineering-line .design-profile-annotation__face-dimension rect,
.design-svg--engineering-line .design-profile-annotation__card {
  fill: #ffffff !important;
  stroke: #6b7280 !important;
}
.design-svg--engineering-line .design-profile-annotation__face-dimension text,
.design-svg--engineering-line .design-profile-annotation__title,
.design-svg--engineering-line .design-profile-annotation__section {
  fill: #111827 !important;
}
</style>`;

/**
 * Escapes a string before it is inserted into SVG text or attributes.
 *
 * IDs and marks can originate from imported projects, so serialization must
 * not trust their contents even though the current factory creates safe values.
 *
 * @param value Raw text value.
 * @returns XML-safe text.
 * @example `escapeXml("A&B")` returns `A&amp;B`.
 * @since 0.1.0
 * @modified 2026-09-17 - Added safe deterministic SVG serialization.
 */
function escapeXml(value: string): string {
  return value
    .replaceAll("&", "&amp;")
    .replaceAll('"', "&quot;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;");
}

type ResolvedOpening = ReturnType<typeof resolveWindowGeometry>["openings"][number];
type ResolvedOpeningHardware = ReturnType<typeof resolveWindowGeometry>["hardware"][number];
type ResolvedSlidingPanel = ReturnType<typeof resolveWindowGeometry>["slidingPanels"][number];

/** Resolves one sliding panel's runtime X offset without mutating design geometry. */
function resolveSlidingPanelOffsetMm(
  panel: ResolvedSlidingPanel,
  progressPercent: number
): number {
  if (!panel.movable || !panel.travelDirection || panel.maximumTravelMm <= 0) return 0;
  return resolveOpeningPose(createSlidingOpeningMotion({
    motionId: `${createOpeningPanelKey(panel.sourceObjectId, panel.panelId)}:sliding`,
    direction: panel.travelDirection,
    travelMm: panel.maximumTravelMm
  }), progressPercent).translationMm.x;
}

/** Resolves the persisted product limit or the mechanism's versioned default. */
function resolveRenderedOpeningMaximumAngleDegrees(
  opening: ResolvedOpening,
  motionMode: OpeningMotionMode
): number {
  const configured = motionMode === "tilt"
    ? opening.maximumAngleDegreesByMode?.tilt
    : opening.maximumAngleDegreesByMode?.primary;
  return configured ?? resolveOpeningMaximumAngleDegrees({
    mechanism: opening.type === "top_hung" ? "top_hung" : "tilt_turn",
    motionMode,
    hingeEdge: opening.hingeEdge,
    openPlane: opening.opening.endsWith("_out") ? "out" : "in"
  });
}

/** Two-axis angle annotation; plan projection interprets `y` as physical Z. */
interface SvgOpeningAngleAnnotation {
  readonly pivot: Readonly<{ x: number; y: number }>;
  readonly startRayEnd: Readonly<{ x: number; y: number }>;
  readonly currentRayEnd: Readonly<{ x: number; y: number }>;
  readonly arc: readonly Readonly<{ x: number; y: number }>[];
  readonly label: Readonly<{ x: number; y: number }>;
  readonly angleDegrees: number;
}

/** Projected 2D points resolved from the same pose consumed by Three.js. */
interface SvgOpeningMotionProjection {
  readonly previewKey: string;
  readonly progressPercent: number;
  readonly motionMode: OpeningMotionMode;
  readonly angleRadians: number;
  readonly facadePointsMm: readonly Readonly<{ x: number; y: number }>[];
  readonly planHullMm: readonly Readonly<{ x: number; z: number }>[];
  readonly planAngleAnnotation?: SvgOpeningAngleAnnotation;
}

/** Formats calculated coordinates deterministically without noisy float tails. */
function formatCoordinate(value: number): string {
  return Number(value.toFixed(3)).toString();
}

/** One deterministic symbolic texture paint used when SVG cannot reproduce PBR. */
interface SvgTextureFallbackPaint {
  readonly fill: string;
  readonly definition: string;
  readonly metadata: string;
}

/** Converts a stable model/slot ID into an XML-safe SVG definition ID. */
function createSvgDefinitionId(...parts: readonly string[]): string {
  return parts.join("-").replace(/[^A-Za-z0-9_-]/g, "-");
}

/**
 * Builds a deterministic 2D hatch for one texture-backed appearance.
 *
 * SVG deliberately does not embed the PBR bitmap bundle. Instead it preserves
 * the base colour and adds a scale-aware diagonal hatch that communicates a
 * textured finish in engineering drawings. Missing/mismatched assets use a red
 * cross hatch and expose machine-readable status; a verified bundle uses a
 * neutral hatch. Plain-colour appearances keep their direct fill.
 *
 * @param appearance Versioned semantic material snapshot.
 * @param patternId Unique local SVG definition ID.
 * @param visualAssets Immutable availability registry supplied by the shell.
 * @returns Fill expression, optional pattern definition and diagnostic metadata.
 * @example A missing wood texture renders the configured brown plus red hatching.
 * @since 0.10.6
 * @modified 2026-09-18 - Added APPEAR-002 SVG texture degradation.
 */
function createSvgTextureFallbackPaint(
  appearance: VisualAppearanceSnapshot,
  patternId: string,
  visualAssets?: VisualAssetAvailability
): SvgTextureFallbackPaint {
  const style = resolveAppearanceRenderStyle(appearance);
  if (!style.textureSetId) {
    return { fill: style.cssColor, definition: "", metadata: "" };
  }
  const available = visualAssets?.resolveTextureSet(style.textureSetId);
  const hashMismatch = Boolean(
    available &&
    style.textureContentHash &&
    available.contentHash.toLowerCase() !== style.textureContentHash.toLowerCase()
  );
  const status = !available
    ? "missing"
    : hashMismatch
      ? "hash-mismatch"
      : style.textureContentHash
        ? "verified"
        : "unversioned";
  const error = status === "missing" || status === "hash-mismatch";
  const xRepeat = Math.max(0.001, style.uvScale?.x ?? 1);
  const yRepeat = Math.max(0.001, style.uvScale?.y ?? 1);
  const patternWidth = Math.max(4, 12 / xRepeat);
  const patternHeight = Math.max(4, 12 / yRepeat);
  const stroke = error ? "#b91c1c" : "#475569";
  const cross = error
    ? `<path d="M 0 ${formatCoordinate(patternHeight)} L ${formatCoordinate(patternWidth)} 0" stroke="${stroke}" stroke-width="0.8" opacity="0.65" />`
    : "";
  const definition = [
    `<pattern id="${patternId}" patternUnits="userSpaceOnUse" width="${formatCoordinate(patternWidth)}" height="${formatCoordinate(patternHeight)}">`,
    `<rect width="100%" height="100%" fill="${style.cssColor}" />`,
    `<path d="M 0 0 L ${formatCoordinate(patternWidth)} ${formatCoordinate(patternHeight)}" stroke="${stroke}" stroke-width="0.8" opacity="0.55" />`,
    cross,
    "</pattern>"
  ].join("");
  const metadata = ` data-texture-set-id="${escapeXml(style.textureSetId)}" data-texture-status="${status}" data-texture-rendering="symbolic-fallback"${style.textureContentHash ? ` data-texture-expected-hash="${escapeXml(style.textureContentHash)}"` : ""}${available ? ` data-texture-actual-hash="${escapeXml(available.contentHash)}"` : ""}`;
  return { fill: `url(#${patternId})`, definition, metadata };
}

/** Exterior trim extents required to fit the façade and its dimensions. */
interface SvgFacadeSurroundInsetsMm {
  readonly top: number;
  readonly right: number;
  readonly bottom: number;
  readonly left: number;
}

/** Resolves exterior-visible package width per edge from shared installation boxes. */
function resolveSvgFacadeSurroundInsetsMm(window: WindowUnit): SvgFacadeSurroundInsetsMm {
  const outsideSides = new Set(
    resolveWindowInstallationObstacleGeometry(window)
      .filter((obstacle) => obstacle.kind === "surround" && obstacle.layer === "outside")
      .map(({ side }) => side)
  );
  const surround = normalizeWindowInstallation(window.installation).surround;
  return {
    top: outsideSides.has("top") ? surround.outsideWidthMm : 0,
    right: outsideSides.has("right") ? surround.outsideWidthMm : 0,
    bottom: outsideSides.has("bottom") ? surround.outsideWidthMm : 0,
    left: outsideSides.has("left") ? surround.outsideWidthMm : 0
  };
}

/**
 * Serializes one physical-angle annotation into rays, a dashed arc and text.
 *
 * The geometry is already sampled by the shared motion package. This adapter
 * only converts millimetres to SVG coordinates; plan mode reverses physical Z
 * because SVG Y grows downward. Keeping this conversion here prevents the
 * visual label from becoming a second source of opening-angle truth.
 *
 * @param annotation Shared-pose angle geometry in a two-axis millimetre plane.
 * @param scale Active pixels-per-millimetre drawing scale.
 * @param className View-specific CSS class prefix.
 * @param mapY Converts the annotation's second axis into the SVG Y coordinate.
 * @returns SVG group containing two rays, a dashed arc and degree text.
 * @example Plan view maps `z` with `baselineY - z * scale`.
 * @since 0.9.2
 * @modified 2026-09-17 - Added visible 2D opening-angle construction marks.
 */
function renderOpeningAngleAnnotationSvg(
  annotation: SvgOpeningAngleAnnotation,
  scale: number,
  className: string,
  mapY: (valueMm: number) => number
): string {
  const x = (valueMm: number): string => formatCoordinate(valueMm * scale);
  const y = (valueMm: number): string => formatCoordinate(mapY(valueMm));
  const arcPath = annotation.arc
    .map((point, index) => `${index === 0 ? "M" : "L"} ${x(point.x)} ${y(point.y)}`)
    .join(" ");
  return [
    `<g class="${className}" data-opening-angle-deg="${formatCoordinate(annotation.angleDegrees)}" pointer-events="none">`,
    `<line class="${className}__ray ${className}__ray--closed" x1="${x(annotation.pivot.x)}" y1="${y(annotation.pivot.y)}" x2="${x(annotation.startRayEnd.x)}" y2="${y(annotation.startRayEnd.y)}" stroke="#0f4c81" stroke-width="1.2" />`,
    `<line class="${className}__ray ${className}__ray--current" x1="${x(annotation.pivot.x)}" y1="${y(annotation.pivot.y)}" x2="${x(annotation.currentRayEnd.x)}" y2="${y(annotation.currentRayEnd.y)}" stroke="#1677ff" stroke-width="1.5" />`,
    `<path class="${className}__arc" d="${arcPath}" fill="none" stroke="#1677ff" stroke-width="1.4" stroke-dasharray="5 4" />`,
    `<text class="${className}__label" x="${x(annotation.label.x)}" y="${y(annotation.label.y)}" text-anchor="middle" dominant-baseline="middle" fill="#0f4c81" font-size="11">${formatCoordinate(annotation.angleDegrees)}°</text>`,
    "</g>"
  ].join("");
}

/**
 * Renders the opening plane as a pure direction glyph on the sash.
 *
 * SVG Y grows downward, therefore an exterior opening points upward and an
 * interior opening points downward. The glyph is derived only from the shared
 * `opening` plane and deliberately contains no visible “内开/外开” wording.
 * A white under-stroke keeps it readable across coloured glass/materials.
 *
 * @param opening Canonical opening geometry and interior/exterior plane.
 * @param scale Current millimetre-to-SVG scale.
 * @returns A vertical arrow group positioned inside the sash aperture.
 * @example `left_out` produces an upward brown arrow; `right_in` points down.
 * @since 0.10.66
 */
function renderFacadeOpeningPlaneArrow(
  opening: ResolvedOpening,
  scale: number
): string {
  const halfFace = opening.sashFaceMm / 2;
  const left = (opening.xMm + halfFace) * scale;
  const right = (opening.xMm + opening.widthMm - halfFace) * scale;
  const top = (opening.yMm + halfFace) * scale;
  const bottom = (opening.yMm + opening.heightMm - halfFace) * scale;
  const centerX = (left + right) / 2;
  const centerY = (top + bottom) / 2;
  const length = Math.max(24, Math.min(54, (bottom - top) * 0.22));
  const exterior = opening.opening.endsWith("_out");
  const startY = centerY + (exterior ? length / 2 : -length / 2);
  const endY = centerY + (exterior ? -length / 2 : length / 2);
  const headBaseY = endY + (exterior ? 9 : -9);
  const colour = exterior ? "#b45309" : "#0755b5";
  const plane = exterior ? "out" : "in";
  return `<g class="design-window__opening-plane-arrow design-window__opening-plane-arrow--${plane}" data-open-plane="${plane}" pointer-events="none" role="img" aria-label="${exterior ? "向上外开图示" : "向下内开图示"}"><line class="design-window__opening-plane-arrow__halo" x1="${formatCoordinate(centerX)}" y1="${formatCoordinate(startY)}" x2="${formatCoordinate(centerX)}" y2="${formatCoordinate(endY)}" stroke="#ffffff" stroke-opacity="0.9" stroke-width="5" /><line class="design-window__opening-plane-arrow__shaft" x1="${formatCoordinate(centerX)}" y1="${formatCoordinate(startY)}" x2="${formatCoordinate(centerX)}" y2="${formatCoordinate(endY)}" stroke="${colour}" stroke-width="2.2" /><polygon class="design-window__opening-plane-arrow__head" points="${formatCoordinate(centerX)},${formatCoordinate(endY)} ${formatCoordinate(centerX - 5)},${formatCoordinate(headBaseY)} ${formatCoordinate(centerX + 5)},${formatCoordinate(headBaseY)}" fill="${colour}" stroke="#ffffff" stroke-width="0.8" /></g>`;
}

/**
 * Serializes the configured full-open limit while the sash is closed.
 *
 * Open moving sashes already receive a physical arc from the kinematics
 * projection. A closed sash has no visible current sweep, so its engineering
 * annotation states the product's configured maximum instead of misleadingly
 * reporting only `0°`.
 *
 * @param opening Canonical sash rectangle used only for badge placement.
 * @param angleDegrees Product-configured or default maximum opening angle.
 * @param scale Current millimetre-to-SVG scale.
 * @returns A non-interactive maximum-angle badge inside the lower-left sash area.
 * @since 0.10.66
 */
function renderFacadeOpeningAngleValue(
  opening: ResolvedOpening,
  angleDegrees: number,
  scale: number
): string {
  const halfFace = opening.sashFaceMm / 2;
  const left = (opening.xMm + halfFace) * scale;
  const bottom = (opening.yMm + opening.heightMm - halfFace) * scale;
  return `<g class="design-window__opening-angle-value" data-opening-angle-kind="maximum" data-opening-angle-deg="${formatCoordinate(angleDegrees)}" transform="translate(${formatCoordinate(left + 7)} ${formatCoordinate(bottom - 27)})" pointer-events="none"><rect width="68" height="20" rx="4" fill="#ffffff" fill-opacity="0.94" stroke="#93c5fd" /><text x="34" y="14" text-anchor="middle" fill="#0f4c81" font-size="11" font-weight="600">最大 ${formatCoordinate(angleDegrees)}°</text></g>`;
}

/**
 * Resolves facade and plan projections for one moving panel.
 *
 * Algorithm: establish the same real hinge pivot as the 3D scene, create local
 * panel corners, resolve the shared mechanism pose, then transform the corners
 * through the common kinematics helper. Elevation consumes X/Y; plan view takes
 * a convex hull of X/Z including the configured sash-section depth and closed
 * front setback from the same snapshot used by Three.js.
 *
 * @param opening Shared closed-state geometry.
 * @param progressPercent Runtime preview progress.
 * @param motionMode Primary or tilt path.
 * @returns Renderer-scaleless millimetre points plus stable preview metadata.
 * @example A right-hinged leaf keeps its right pivot while its free edge moves.
 * @since 0.8.2
 * @modified 2026-09-17 - Replaced plan-depth constants with shared section geometry.
 */
function projectOpeningMotion(
  opening: ResolvedOpening,
  progressPercent: number,
  motionMode: OpeningMotionMode
): SvgOpeningMotionProjection {
  const previewKey = createOpeningPanelKey(opening.objectId, opening.panelId);
  const tiltMode = opening.type === "turn_tilt" && motionMode === "tilt";
  const pivotEdge = tiltMode ? "bottom" : opening.hingeEdge;
  const pivotX = pivotEdge === "left"
    ? opening.xMm
    : pivotEdge === "right"
      ? opening.xMm + opening.widthMm
      : opening.xMm + opening.widthMm / 2;
  const pivotY = pivotEdge === "top"
    ? opening.yMm
    : pivotEdge === "bottom"
      ? opening.yMm + opening.heightMm
      : opening.yMm + opening.heightMm / 2;
  const xRange = pivotEdge === "left"
    ? [0, opening.widthMm] as const
    : pivotEdge === "right"
      ? [-opening.widthMm, 0] as const
      : [-opening.widthMm / 2, opening.widthMm / 2] as const;
  const yRange = pivotEdge === "top"
    ? [-opening.heightMm, 0] as const
    : pivotEdge === "bottom"
      ? [0, opening.heightMm] as const
      : [-opening.heightMm / 2, opening.heightMm / 2] as const;
  const definition = createOpeningMechanismMotion({
    motionId: `${previewKey}:${motionMode}`,
    mechanism: opening.type === "top_hung" ? "top_hung" : "tilt_turn",
    motionMode,
    hingeEdge: opening.hingeEdge,
    openPlane: opening.opening.endsWith("_out") ? "out" : "in",
    maximumAngleDegrees: resolveRenderedOpeningMaximumAngleDegrees(opening, motionMode)
  });
  const pose = resolveOpeningPose(definition, progressPercent);
  const section = opening.sectionDimensions;
  const halfSashDepthMm = section.sashDepthMm / 2;
  const closedSashCenterZMm = section.frameDepthMm / 2 - halfSashDepthMm -
    section.sashFrontSetbackMm;
  const facadeLocal: readonly OpeningMotionVector3[] = [
    { x: xRange[0], y: yRange[1], z: 0 },
    { x: xRange[1], y: yRange[1], z: 0 },
    { x: xRange[1], y: yRange[0], z: 0 },
    { x: xRange[0], y: yRange[0], z: 0 }
  ];
  const planPoints: Array<{ x: number; z: number }> = [];
  for (const localX of xRange) {
    for (const localY of yRange) {
      for (const localZ of [-halfSashDepthMm, halfSashDepthMm]) {
        const point = transformOpeningMotionPoint(
          { x: localX, y: localY, z: localZ },
          pose
        );
        planPoints.push({ x: pivotX + point.x, z: closedSashCenterZMm + point.z });
      }
    }
  }
  const planAngleAnnotation = pivotEdge === "left" || pivotEdge === "right"
    ? (() => {
        const radius = Math.max(80, Math.min(260, opening.widthMm * 0.28));
        const startAngleRadians = pivotEdge === "left" ? 0 : Math.PI;
        // Three's positive Y rotation turns a local +X ray toward physical -Z.
        const sweepAngleRadians = -pose.rotationRadians.y;
        const arc = sampleOpeningAngleArc({ radius, startAngleRadians, sweepAngleRadians });
        const toPoint = (point: Readonly<{ radialX: number; radialY: number }>) => ({
          x: pivotX + point.radialX,
          y: closedSashCenterZMm + point.radialY
        });
        const labelAngle = startAngleRadians + sweepAngleRadians / 2;
        const labelRadius = radius + 38;
        return {
          pivot: { x: pivotX, y: closedSashCenterZMm },
          startRayEnd: toPoint(arc[0]!),
          currentRayEnd: toPoint(arc.at(-1)!),
          arc: arc.map(toPoint),
          label: {
            x: pivotX + Math.cos(labelAngle) * labelRadius,
            y: closedSashCenterZMm + Math.sin(labelAngle) * labelRadius
          },
          angleDegrees: Math.abs(sweepAngleRadians) * 180 / Math.PI
        };
      })()
    : undefined;
  return {
    previewKey,
    progressPercent: pose.progressPercent,
    motionMode,
    angleRadians: Math.max(
      Math.abs(pose.rotationRadians.x),
      Math.abs(pose.rotationRadians.y),
      Math.abs(pose.rotationRadians.z)
    ),
    facadePointsMm: facadeLocal.map((local) => {
      const point = transformOpeningMotionPoint(local, pose);
      return { x: pivotX + point.x, y: pivotY - point.y };
    }),
    planHullMm: convexHull(planPoints),
    planAngleAnnotation
  };
}

/**
 * Measures a stable full-motion plan envelope for one window in millimetres.
 *
 * Algorithm: include the actual frame section, then sample every opening at
 * quarter-progress intervals. The confirmed reference side hinge ends at 90°;
 * intermediate samples also keep the envelope correct when future catalog
 * limits or multi-stage mechanisms are not endpoint-monotonic. Tilt-turn leaves
 * contribute both primary and tilt modes, keeping the canvas stable while dragging.
 *
 * @param window Shared window model whose opening geometry supplies real pivots.
 * @returns Minimum/maximum plan-depth coordinates around the frame centreline.
 * @example A 1200mm side-hung leaf reserves its complete 90° sweep depth.
 * @since 0.8.6
 * @modified 2026-09-17 - Added stable intermediate sampling for configurable limits.
 */
function resolveWindowPlanEnvelopeMm(
  window: WindowUnit
): Readonly<{ minZMm: number; maxZMm: number; spanMm: number }> {
  const installationSection = resolveWindowInstallationSection(window);
  const frameDepthMm = installationSection.frameDepthMm;
  let minZMm = Math.min(installationSection.frameInsideZMm, installationSection.wallInsideZMm);
  let maxZMm = Math.max(installationSection.frameOutsideZMm, installationSection.wallOutsideZMm);
  const geometry = resolveWindowGeometry(window);
  for (const opening of geometry.openings) {
    const modes: readonly OpeningMotionMode[] = opening.type === "turn_tilt"
      ? ["primary", "tilt"]
      : ["primary"];
    for (const mode of modes) {
      // Sample intermediate poses so future catalog limits cannot collapse the envelope.
      for (const progressPercent of [0, 25, 50, 75, 100]) {
        const projection = projectOpeningMotion(opening, progressPercent, mode);
        for (const point of projection.planHullMm) {
          minZMm = Math.min(minZMm, point.z);
          maxZMm = Math.max(maxZMm, point.z);
        }
      }
    }
  }
  return {
    minZMm,
    maxZMm,
    spanMm: Math.max(frameDepthMm, maxZMm - minZMm)
  };
}

interface SvgOpeningFacadePerspective {
  readonly angleRadians: number;
  readonly connectionPresetId: string;
  readonly outerMm: readonly Readonly<{ x: number; y: number }>[];
  readonly innerMm: readonly Readonly<{ x: number; y: number }>[];
  readonly sideFaceMm: readonly Readonly<{ x: number; y: number }>[];
  readonly freeEdgeMm: readonly [Readonly<{ x: number; y: number }>, Readonly<{ x: number; y: number }>];
  readonly hardware: readonly SvgOpeningFacadeHardwareProjection[];
  readonly transmissionRodMm: readonly Readonly<{ x: number; y: number }>[];
  readonly angleAnnotation?: SvgOpeningAngleAnnotation;
  readonly connections: readonly Readonly<{
    id: string;
    role: OpeningConnectionRole;
    sashEdge: OpeningHingeEdge;
    sashMm: Readonly<{ x: number; y: number }>;
    frameMm: Readonly<{ x: number; y: number }>;
  }>[];
}

/** One physical sash/flying-mullion hardware member projected with its owner panel. */
interface SvgOpeningFacadeHardwareProjection {
  readonly hardwareId: string;
  readonly sourceComponentId: string;
  readonly role: ResolvedOpeningHardware["role"];
  readonly mountTarget: ResolvedOpeningHardware["mountTarget"];
  readonly edge: ResolvedOpeningHardware["edge"];
  readonly connectionId?: string;
  readonly polygonMm: readonly Readonly<{ x: number; y: number }>[];
  readonly centerMm: Readonly<{ x: number; y: number }>;
  readonly leverEndMm?: Readonly<{ x: number; y: number }>;
  readonly sourceCenterYmm: number;
  readonly model: HardwareComponentModelSnapshot;
}

/**
 * Projects one physically rotating sash into an oblique engineering elevation.
 *
 * The facade cannot show Z depth with an orthographic X/Y projection alone. This
 * adapter therefore keeps the shared physical hinge pose, then maps depth into a
 * small horizontal/vertical screen offset. It produces a readable perspective
 * sash without changing the model, BOM, plan angle or Three.js transform.
 *
 * Algorithm: build the visible sash rectangle around its actual hinge, transform
 * outer/inner/depth points through the shared pose, then apply one deterministic
 * oblique depth projection. A rear free edge creates the visible side face.
 *
 * @param opening Shared closed-state opening geometry.
 * @param progressPercent Runtime preview percentage.
 * @param motionMode Primary side-hung or bottom-tilt motion.
 * @param hardware Canonical closed-state mounts used for moving-detail projection.
 * @param visualConfiguration Versioned model dimensions and material appearances.
 * @returns Perspective facade polygons and a moving handle segment in millimetres.
 * @example A right inward sash becomes a foreshortened, slightly lower panel with a visible edge.
 * @since 0.8.7
 * @modified 2026-09-17 - Added shared-pose locks, levers, bolts and display rods.
 */
function projectOpeningFacadePerspective(
  opening: ResolvedOpening,
  progressPercent: number,
  motionMode: OpeningMotionMode,
  hardware: readonly ResolvedOpeningHardware[],
  visualConfiguration: WindowVisualConfiguration,
  connectionPreset?: OpeningConnectionLayoutPreset
): SvgOpeningFacadePerspective {
  const halfFace = opening.sashFaceMm / 2;
  const left = opening.xMm + halfFace;
  const right = opening.xMm + opening.widthMm - halfFace;
  const top = opening.yMm + halfFace;
  const bottom = opening.yMm + opening.heightMm - halfFace;
  const width = Math.max(1, right - left);
  const height = Math.max(1, bottom - top);
  const tiltMode = opening.type === "turn_tilt" && motionMode === "tilt";
  const pivotEdge = tiltMode ? "bottom" : opening.hingeEdge;
  const pivotX = pivotEdge === "left" ? left : pivotEdge === "right" ? right : (left + right) / 2;
  const pivotY = pivotEdge === "top" ? top : pivotEdge === "bottom" ? bottom : (top + bottom) / 2;
  const xRange = pivotEdge === "left"
    ? [0, width] as const
    : pivotEdge === "right"
      ? [-width, 0] as const
      : [-width / 2, width / 2] as const;
  const yRange = pivotEdge === "top"
    ? [-height, 0] as const
    : pivotEdge === "bottom"
      ? [0, height] as const
      : [-height / 2, height / 2] as const;
  const definition = createOpeningMechanismMotion({
    motionId: `${createOpeningPanelKey(opening.objectId, opening.panelId)}:${motionMode}:facade`,
    mechanism: opening.type === "top_hung" ? "top_hung" : "tilt_turn",
    motionMode,
    hingeEdge: opening.hingeEdge,
    openPlane: opening.opening.endsWith("_out") ? "out" : "in",
    maximumAngleDegrees: resolveRenderedOpeningMaximumAngleDegrees(opening, motionMode)
  });
  const pose = resolveOpeningPose(definition, progressPercent);
  const hingeDirection = opening.hingeEdge === "left" ? 1 : opening.hingeEdge === "right" ? -1 : 0;
  const projectPoint = (localX: number, localY: number, localZ = 0): Readonly<{ x: number; y: number }> => {
    const point = transformOpeningMotionPoint({ x: localX, y: localY, z: localZ }, pose);
    return {
      // Keep the established oblique 2D opening effect: physical depth is
      // projected by a restrained deterministic offset so sash thickness and
      // motion remain visually readable without changing shared geometry.
      x: pivotX + point.x + point.z * 0.08 * hingeDirection,
      y: pivotY - point.y - point.z * 0.25
    };
  };
  const createQuad = (
    insetMm: number,
    localZ = 0
  ): readonly Readonly<{ x: number; y: number }>[] => {
    const minX = xRange[0] + insetMm;
    const maxX = xRange[1] - insetMm;
    const minY = yRange[0] + insetMm;
    const maxY = yRange[1] - insetMm;
    return [
      projectPoint(minX, maxY, localZ),
      projectPoint(maxX, maxY, localZ),
      projectPoint(maxX, minY, localZ),
      projectPoint(minX, minY, localZ)
    ];
  };
  const outerMm = createQuad(0);
  const insetMm = Math.min(opening.sashFaceMm, width * 0.22, height * 0.22);
  const innerMm = createQuad(insetMm);
  const rearMm = createQuad(0, -opening.sectionDimensions.sashDepthMm);
  const freeIndexes = opening.hingeEdge === "left" ? [1, 2] as const : [0, 3] as const;
  const sideFaceMm = [
    outerMm[freeIndexes[0]]!,
    rearMm[freeIndexes[0]]!,
    rearMm[freeIndexes[1]]!,
    outerMm[freeIndexes[1]]!
  ];
  const connectionLayout = resolveOpeningConnectionLayout({
    mechanism: opening.type === "top_hung" ? "top_hung" : "tilt_turn",
    motionMode,
    hingeEdge: opening.hingeEdge,
    preset: connectionPreset
  });
  const angleAnnotation = pivotEdge === "left" || pivotEdge === "right"
    ? (() => {
        // Side-hung rotation occurs in the horizontal plane, so the elevation
        // carries a compact plan-angle glyph instead of pretending that the
        // arc lies on the facade. The full plan view uses the same pose/range.
        const radius = Math.max(70, Math.min(210, width * 0.22));
        const startAngleRadians = pivotEdge === "left" ? 0 : Math.PI;
        const sweepAngleRadians = -pose.rotationRadians.y;
        const arc = sampleOpeningAngleArc({ radius, startAngleRadians, sweepAngleRadians });
        const annotationPivot = {
          x: pivotX,
          y: bottom - Math.max(120, Math.min(320, height * 0.24))
        };
        const toPoint = (point: Readonly<{ radialX: number; radialY: number }>) => ({
          x: annotationPivot.x + point.radialX,
          y: annotationPivot.y - point.radialY
        });
        const labelAngle = startAngleRadians + sweepAngleRadians / 2;
        const labelRadius = radius + 34;
        return {
          pivot: annotationPivot,
          startRayEnd: toPoint(arc[0]!),
          currentRayEnd: toPoint(arc.at(-1)!),
          arc: arc.map(toPoint),
          label: {
            x: annotationPivot.x + Math.cos(labelAngle) * labelRadius,
            y: annotationPivot.y - Math.sin(labelAngle) * labelRadius
          },
          angleDegrees: Math.abs(sweepAngleRadians) * 180 / Math.PI
        };
      })()
    : pivotEdge === "top" || pivotEdge === "bottom"
      ? (() => {
        const radius = Math.max(80, Math.min(240, height * 0.18));
        const startAngleRadians = pivotEdge === "top" ? Math.PI / 2 : -Math.PI / 2;
        const sweepAngleRadians = pose.rotationRadians.x;
        const arc = sampleOpeningAngleArc({ radius, startAngleRadians, sweepAngleRadians });
        const annotationPivot = { x: left - 32, y: pivotY };
        const toPoint = (point: Readonly<{ radialX: number; radialY: number }>) => ({
          x: annotationPivot.x + point.radialX,
          y: annotationPivot.y + point.radialY
        });
        const labelAngle = startAngleRadians + sweepAngleRadians / 2;
        const labelRadius = radius + 38;
        return {
          pivot: annotationPivot,
          startRayEnd: toPoint(arc[0]!),
          currentRayEnd: toPoint(arc.at(-1)!),
          arc: arc.map(toPoint),
          label: {
            x: annotationPivot.x + Math.cos(labelAngle) * labelRadius,
            y: annotationPivot.y + Math.sin(labelAngle) * labelRadius
          },
          angleDegrees: Math.abs(sweepAngleRadians) * 180 / Math.PI
        };
        })()
      : undefined;
  const pointOnEdge = (
    edge: OpeningHingeEdge,
    ratio: number,
    moving: boolean
  ): Readonly<{ x: number; y: number }> => {
    const localX = edge === "left"
      ? xRange[0]
      : edge === "right"
        ? xRange[1]
        : xRange[0] + (xRange[1] - xRange[0]) * ratio;
    const localY = edge === "top"
      ? yRange[1]
      : edge === "bottom"
        ? yRange[0]
        : yRange[1] - (yRange[1] - yRange[0]) * ratio;
    if (moving) return projectPoint(localX, localY, 8);
    return { x: pivotX + localX, y: pivotY - localY };
  };
  /**
   * Projects canonical closed-state hardware through the exact sash pose.
   *
   * Algorithm: choose only members physically owned by this panel, transform
   * every rectangle corner from window millimetres into hinge-local space, then
   * reuse the same oblique projection as the sash. Tilt preview suppresses the
   * side-hinge leaves because its bottom pivots/top stay come from the active
   * connection preset. A display-only rod joins existing lock/handle centres;
   * it never creates a material, machining feature or BOM line.
   *
   * @example A handle and two lock points remain aligned on a fully opened P1 sash.
   * @since 0.9.6
   * @modified 2026-09-17 - Added precise moving lock and transmission projection.
   */
  const movingHardware = hardware
    .filter((mount) =>
      mount.sourceObjectId === opening.objectId &&
      (mount.mountTarget === "sash" || mount.mountTarget === "flying-mullion") &&
      (mount.mountOwnerPanelId ?? mount.panelId) === opening.panelId &&
      !(tiltMode && mount.role === "hinge-sash-leaf")
    )
    .map((mount): SvgOpeningFacadeHardwareProjection => {
      const model = resolveHardwareComponentModel(
        visualConfiguration,
        mount.role,
        mount.hardwareId
      );
      if (!model) {
        throw new Error(`No visual hardware model resolves for ${mount.hardwareId}.`);
      }
      const projectWindowPoint = (xMm: number, yMm: number): Readonly<{ x: number; y: number }> =>
        projectPoint(xMm - pivotX, pivotY - yMm, 10);
      const centerXmm = mount.xMm + mount.widthMm / 2;
      const centerYmm = mount.yMm + mount.heightMm / 2;
      const horizontalMount = mount.edge === "top" || mount.edge === "bottom";
      const modelWidthMm = horizontalMount
        ? model.dimensionsMm.heightMm
        : model.dimensionsMm.widthMm;
      const modelHeightMm = horizontalMount
        ? model.dimensionsMm.widthMm
        : model.dimensionsMm.heightMm;
      const modelLeftMm = centerXmm - modelWidthMm / 2;
      const modelTopMm = centerYmm - modelHeightMm / 2;
      const leverLengthMm = model.geometry.kind === "parametric"
        ? model.geometry.parameters.leverLengthMm ?? Math.max(45, opening.sashFaceMm * 0.65)
        : Math.max(45, opening.sashFaceMm * 0.65);
      const leverEndWindowMm = mount.edge === "left"
        ? { x: centerXmm + leverLengthMm, y: centerYmm }
        : mount.edge === "right"
          ? { x: centerXmm - leverLengthMm, y: centerYmm }
          : mount.edge === "top"
            ? { x: centerXmm, y: centerYmm + leverLengthMm }
            : { x: centerXmm, y: centerYmm - leverLengthMm };
      const isLever = mount.role === "handle" || mount.role === "secondary-lever";
      return {
        hardwareId: mount.hardwareId,
        sourceComponentId: mount.sourceComponentId,
        role: mount.role,
        mountTarget: mount.mountTarget,
        edge: mount.edge,
        connectionId: mount.connectionId,
        polygonMm: [
          projectWindowPoint(modelLeftMm, modelTopMm),
          projectWindowPoint(modelLeftMm + modelWidthMm, modelTopMm),
          projectWindowPoint(modelLeftMm + modelWidthMm, modelTopMm + modelHeightMm),
          projectWindowPoint(modelLeftMm, modelTopMm + modelHeightMm)
        ],
        centerMm: projectWindowPoint(centerXmm, centerYmm),
        leverEndMm: isLever
          ? projectWindowPoint(leverEndWindowMm.x, leverEndWindowMm.y)
          : undefined,
        sourceCenterYmm: centerYmm,
        model
      };
    });
  const transmissionMembers = movingHardware
    .filter((mount) =>
      mount.role === "handle" ||
      mount.role === "secondary-lever" ||
      mount.role === "lock-point" ||
      mount.role === "shoot-bolt"
    )
    .sort((leftMount, rightMount) => leftMount.sourceCenterYmm - rightMount.sourceCenterYmm);
  const transmissionRodMm = transmissionMembers.length >= 2 && transmissionMembers.some(
    (mount) => mount.role === "lock-point" || mount.role === "shoot-bolt"
  )
    ? transmissionMembers.map((mount) => mount.centerMm)
    : [];
  return {
    angleRadians: Math.max(
      Math.abs(pose.rotationRadians.x),
      Math.abs(pose.rotationRadians.y),
      Math.abs(pose.rotationRadians.z)
    ),
    connectionPresetId: connectionLayout.presetId,
    outerMm,
    innerMm,
    sideFaceMm,
    freeEdgeMm: [outerMm[freeIndexes[0]]!, outerMm[freeIndexes[1]]!],
    hardware: movingHardware,
    transmissionRodMm,
    angleAnnotation,
    connections: connectionLayout.anchors.map((anchor) => ({
      id: anchor.id,
      role: anchor.role,
      sashEdge: anchor.sashEdge,
      sashMm: pointOnEdge(anchor.sashEdge, anchor.sashPositionRatio, true),
      frameMm: pointOnEdge(anchor.frameEdge, anchor.framePositionRatio, false)
    }))
  };
}

/**
 * Serializes one perspective sash with physical moving hardware and footprint.
 *
 * Geometry is already resolved in millimetres by the projection helper. This
 * function only scales and escapes it into deterministic SVG: sash-owned parts
 * receive role/hardware/source metadata, while one `data-display-derived` rod
 * visually connects existing drive members. The rod is deliberately not given
 * a manufacturing ID because it is an explanatory overlay, not a BOM item.
 *
 * @param opening Shared opening geometry and stable panel identity.
 * @param progressPercent Runtime-only normalized motion coordinate.
 * @param motionMode Active primary or tilt mechanism.
 * @param scale Current engineering pixels per millimetre.
 * @param previewKey Stable object/panel preview key.
 * @param hardware Canonical mounts shared with Three.js and manufacturing extraction.
 * @param visualConfiguration Versioned model and appearance snapshot.
 * @param connectionPreset Optional catalog/reference movement connection layout.
 * @returns SVG groups for footprint, sash, connections, angle and moving hardware.
 * @example At 90° the two lock points and handle remain on the same moving P1 edge.
 * @since 0.8.7
 * @modified 2026-09-17 - Added 0.9.6 moving lock and transmission details.
 */
function renderOpeningFacadePerspective(
  opening: ResolvedOpening,
  progressPercent: number,
  motionMode: OpeningMotionMode,
  scale: number,
  previewKey: string,
  hardware: readonly ResolvedOpeningHardware[],
  visualConfiguration: WindowVisualConfiguration,
  connectionPreset?: OpeningConnectionLayoutPreset
): string {
  const perspective = projectOpeningFacadePerspective(
    opening,
    progressPercent,
    motionMode,
    hardware,
    visualConfiguration,
    connectionPreset
  );
  const sashOutside = resolveAppearanceRenderStyle(visualConfiguration.appearance.sash.outside);
  const sashEdge = resolveAppearanceRenderStyle(visualConfiguration.appearance.sash.edge);
  const glass = resolveAppearanceRenderStyle(visualConfiguration.appearance.glass);
  const flyingMullion = resolveAppearanceRenderStyle(
    visualConfiguration.appearance.flyingMullion.outside
  );
  const points = (values: readonly Readonly<{ x: number; y: number }>[]): string =>
    values.map((point) => `${formatCoordinate(point.x * scale)},${formatCoordinate(point.y * scale)}`).join(" ");
  const halfFace = opening.sashFaceMm / 2;
  const left = (opening.xMm + halfFace) * scale;
  const top = (opening.yMm + halfFace) * scale;
  const width = Math.max(0, (opening.widthMm - opening.sashFaceMm) * scale);
  const height = Math.max(0, (opening.heightMm - opening.sashFaceMm) * scale);
  const profileStroke = Math.max(4, opening.sashFaceMm * scale * 0.72);
  const angleDegrees = perspective.angleRadians * 180 / Math.PI;
  const flyingEdge = opening.flyingMullionOwnerPanelId === opening.panelId
    ? `<line class="design-window__opening-flying-mullion" x1="${formatCoordinate(perspective.freeEdgeMm[0].x * scale)}" y1="${formatCoordinate(perspective.freeEdgeMm[0].y * scale)}" x2="${formatCoordinate(perspective.freeEdgeMm[1].x * scale)}" y2="${formatCoordinate(perspective.freeEdgeMm[1].y * scale)}" stroke="${flyingMullion.cssColor}" stroke-opacity="${formatCoordinate(flyingMullion.opacity)}" stroke-width="${Math.max(5, profileStroke * 0.65)}" />`
    : "";
  const connections = perspective.connections.map((connection) => {
    const frameX = formatCoordinate(connection.frameMm.x * scale);
    const frameY = formatCoordinate(connection.frameMm.y * scale);
    const sashX = formatCoordinate(connection.sashMm.x * scale);
    const sashY = formatCoordinate(connection.sashMm.y * scale);
    const connector = connection.role === "stay"
      ? `<line class="design-window__opening-connection-stay" x1="${frameX}" y1="${frameY}" x2="${sashX}" y2="${sashY}" stroke="#0f766e" stroke-width="3" stroke-linecap="round" />`
      : "";
    return [
      `<g class="design-window__opening-connection" data-connection-id="${escapeXml(connection.id)}" data-connection-role="${connection.role}" data-connection-edge="${connection.sashEdge}">`,
      connector,
      `<circle cx="${frameX}" cy="${frameY}" r="4" fill="#0f172a" />`,
      `<circle cx="${sashX}" cy="${sashY}" r="4" fill="${connection.role === "stay" ? "#0f766e" : "#d97706"}" />`,
      "</g>"
    ].join("");
  }).join("");
  const sideHungPlanGlyph = motionMode === "primary" &&
    (opening.hingeEdge === "left" || opening.hingeEdge === "right");
  const angleAnnotation = perspective.angleAnnotation
    ? renderOpeningAngleAnnotationSvg(
        perspective.angleAnnotation,
        scale,
        sideHungPlanGlyph
          ? "design-window__opening-angle-plan-glyph"
          : "design-window__opening-angle",
        (valueMm) => valueMm * scale
      )
    : "";
  const transmissionRod = perspective.transmissionRodMm.length >= 2
    ? `<polyline class="design-window__opening-transmission-rod" data-display-derived="true" points="${points(perspective.transmissionRodMm)}" fill="none" stroke="#92400e" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" />`
    : "";
  const movingHardware = perspective.hardware.map((mount) => {
    const style = resolveAppearanceRenderStyle(mount.model.appearance);
    const connection = mount.connectionId
      ? ` data-connection-id="${escapeXml(mount.connectionId)}"`
      : "";
    const lever = mount.leverEndMm
      ? `<line class="design-window__opening-handle" x1="${formatCoordinate(mount.centerMm.x * scale)}" y1="${formatCoordinate(mount.centerMm.y * scale)}" x2="${formatCoordinate(mount.leverEndMm.x * scale)}" y2="${formatCoordinate(mount.leverEndMm.y * scale)}" stroke="#78350f" stroke-width="3" stroke-linecap="round" />`
      : "";
    return [
      `<g class="design-window__opening-moving-hardware design-window__opening-moving-hardware--${mount.role}" data-hardware-id="${escapeXml(mount.hardwareId)}" data-source-component-id="${escapeXml(mount.sourceComponentId)}" data-model-id="${escapeXml(mount.model.modelId)}" data-model-version="${escapeXml(mount.model.modelVersion)}" data-geometry-kind="${mount.model.geometry.kind}" data-mount-target="${mount.mountTarget}" data-edge="${mount.edge}"${connection}>`,
      `<polygon points="${points(mount.polygonMm)}" fill="${style.cssColor}" fill-opacity="${formatCoordinate(style.opacity)}" stroke="#1e293b" stroke-width="1" stroke-linejoin="round" />`,
      lever,
      "</g>"
    ].join("");
  }).join("");
  return [
    `<g class="design-window__opening-perspective" data-preview-panel-key="${escapeXml(previewKey)}" data-open-plane="${opening.opening.endsWith("_out") ? "out" : "in"}" data-opening-angle-deg="${formatCoordinate(angleDegrees)}" data-connection-preset-id="${escapeXml(perspective.connectionPresetId)}">`,
    `<rect class="design-window__opening-closed-footprint" x="${formatCoordinate(left)}" y="${formatCoordinate(top)}" width="${formatCoordinate(width)}" height="${formatCoordinate(height)}" fill="none" stroke="#94a3b8" stroke-width="1.2" stroke-dasharray="5 4" />`,
    `<polygon class="design-window__opening-side-face" points="${points(perspective.sideFaceMm)}" fill="${sashEdge.cssColor}" fill-opacity="${formatCoordinate(Math.min(0.75, sashEdge.opacity))}" stroke="#334155" stroke-width="1.2" />`,
    `<polygon class="design-window__opening-state" data-preview-panel-key="${escapeXml(previewKey)}" points="${points(perspective.outerMm)}" fill="${sashOutside.cssColor}" fill-opacity="${formatCoordinate(sashOutside.opacity)}" stroke="#475569" stroke-width="${formatCoordinate(profileStroke)}" stroke-linejoin="round" />`,
    `<polygon class="design-window__opening-glass" points="${points(perspective.innerMm)}" fill="${glass.cssColor}" fill-opacity="${formatCoordinate(glass.opacity)}" stroke="#7aa7bd" stroke-width="1" />`,
    flyingEdge,
    connections,
    angleAnnotation,
    transmissionRod,
    movingHardware,
    "</g>"
  ].join("");
}

/** Computes a deterministic monotonic-chain hull for SVG plan footprints. */
function convexHull(
  points: readonly Readonly<{ x: number; z: number }>[]
): readonly Readonly<{ x: number; z: number }>[] {
  const sorted = [...points]
    .sort((left, right) => left.x - right.x || left.z - right.z)
    .filter((point, index, values) =>
      index === 0 || point.x !== values[index - 1]?.x || point.z !== values[index - 1]?.z
    );
  if (sorted.length <= 2) return sorted;
  const cross = (
    origin: Readonly<{ x: number; z: number }>,
    left: Readonly<{ x: number; z: number }>,
    right: Readonly<{ x: number; z: number }>
  ): number => (left.x - origin.x) * (right.z - origin.z) -
    (left.z - origin.z) * (right.x - origin.x);
  const lower: typeof sorted = [];
  for (const point of sorted) {
    while (lower.length >= 2 && cross(lower[lower.length - 2]!, lower[lower.length - 1]!, point) <= 0) {
      lower.pop();
    }
    lower.push(point);
  }
  const upper: typeof sorted = [];
  for (const point of [...sorted].reverse()) {
    while (upper.length >= 2 && cross(upper[upper.length - 2]!, upper[upper.length - 1]!, point) <= 0) {
      upper.pop();
    }
    upper.push(point);
  }
  lower.pop();
  upper.pop();
  return [...lower, ...upper];
}

/**
 * Serializes one engineering dimension with object/group edit metadata.
 *
 * Every visible label owns a transparent label-sized hit area and a stable
 * target object ID. Related labels (for example the left/right inner and outer
 * dimensions around one meeting mullion) share one group ID but retain a
 * measurement role, allowing the UI to translate the clicked value back to the
 * same physical split without inferring meaning from localized text.
 *
 * @param input Screen-space dimension endpoints, label and optional edit target.
 * @returns One SVG group containing witness lines, dimension line and label.
 * @example A 1200mm width uses kind `width` and window ID `WIN-1`.
 * @since 0.4.1
 * @modified 2026-09-20 - Added grouped label hits and exact selection targets.
 * @modified 2026-09-21 - Added explicit interior-bay placement and source-member metadata.
 */
function renderDimensionLine(input: {
  x1: number;
  y1: number;
  x2: number;
  y2: number;
  label: string;
  vertical?: boolean;
  windowId?: string;
  objectId?: string;
  groupId?: string;
  kind?: "width" | "height" | "cell-width" | "cell-height" |
    "meeting-mullion" | "surround-outside-width" | "surround-outer-width" |
    "topology-clear-span";
  cellId?: string;
  measurementRole?: "left-inner" | "right-inner" | "left-outer" | "right-outer";
  memberId?: string;
  clearSpanSide?: "before" | "after";
  axis?: "column" | "row";
  index?: number;
  edge?: "start" | "end";
  selected?: boolean;
  placement?: "outside" | "cell-interior" | "member-interior";
  sourceObjectId?: string;
}): string {
  const cellMetadata = input.cellId
    ? ` data-cell-id="${escapeXml(input.cellId)}"`
    : "";
  const objectMetadata = input.objectId
    ? ` data-object-id="${escapeXml(input.objectId)}"`
    : "";
  const groupMetadata = input.groupId
    ? ` data-dimension-group-id="${escapeXml(input.groupId)}"`
    : "";
  const roleMetadata = input.measurementRole
    ? ` data-measurement-role="${input.measurementRole}"`
    : "";
  const memberMetadata = input.memberId
    ? ` data-member-id="${escapeXml(input.memberId)}"`
    : "";
  const clearSpanMetadata = input.clearSpanSide
    ? ` data-clear-span-side="${input.clearSpanSide}"`
    : "";
  const gridMetadata = input.axis && input.index !== undefined && input.edge
    ? ` data-axis="${input.axis}" data-index="${input.index}" data-edge="${input.edge}"`
    : "";
  const selected = input.selected ? ' data-selected="true" aria-current="true"' : "";
  const placement = input.placement
    ? ` data-dimension-placement="${input.placement}"`
    : "";
  const sourceObject = input.sourceObjectId
    ? ` data-dimension-source-object-id="${escapeXml(input.sourceObjectId)}"`
    : "";
  const editable = input.windowId && input.kind && input.objectId
    ? ` data-window-id="${escapeXml(input.windowId)}" data-dimension-kind="${input.kind}"${objectMetadata}${groupMetadata}${cellMetadata}${roleMetadata}${memberMetadata}${clearSpanMetadata}${gridMetadata}${selected} tabindex="0" role="button" aria-label="双击修改${escapeXml(input.label)}"`
    : "";
  const labelHitWidth = Math.max(48, input.label.length * 7 + 12);
  if (input.vertical) {
    const middle = (input.y1 + input.y2) / 2;
    return `<g class="design-dimension${editable ? " design-dimension--editable" : ""}"${editable}${placement}${sourceObject}><line class="design-dimension__hit" x1="${input.x1}" y1="${input.y1}" x2="${input.x2}" y2="${input.y2}" /><line x1="${input.x1}" y1="${input.y1}" x2="${input.x2}" y2="${input.y2}" /><line x1="${input.x1 - 7}" y1="${input.y1}" x2="${input.x1 + 7}" y2="${input.y1}" /><line x1="${input.x1 - 7}" y1="${input.y2}" x2="${input.x1 + 7}" y2="${input.y2}" /><g class="design-dimension__label" transform="translate(${input.x1 - 14} ${middle}) rotate(-90)"><rect class="design-dimension__label-hit" x="${-labelHitWidth / 2}" y="-14" width="${labelHitWidth}" height="18" rx="4" /><text x="0" y="0">${escapeXml(input.label)}</text></g></g>`;
  }
  const middle = (input.x1 + input.x2) / 2;
  return `<g class="design-dimension${editable ? " design-dimension--editable" : ""}"${editable}${placement}${sourceObject}><line class="design-dimension__hit" x1="${input.x1}" y1="${input.y1}" x2="${input.x2}" y2="${input.y2}" /><line x1="${input.x1}" y1="${input.y1}" x2="${input.x2}" y2="${input.y2}" /><line x1="${input.x1}" y1="${input.y1 - 7}" x2="${input.x1}" y2="${input.y1 + 7}" /><line x1="${input.x2}" y1="${input.y2 - 7}" x2="${input.x2}" y2="${input.y2 + 7}" /><rect class="design-dimension__label-hit" x="${middle - labelHitWidth / 2}" y="${input.y1 + 5}" width="${labelHitWidth}" height="18" rx="4" /><text x="${middle}" y="${input.y1 + 18}">${escapeXml(input.label)}</text></g>`;
}

/**
 * Renders one selection-scoped profile callout without crowding the elevation.
 *
 * Algorithm: the selected physical profile keeps its normal stable object ID;
 * this read-only overlay measures the visible face perpendicular to the member
 * and leads to a two-line card in the reserved white band above the product.
 * Section depth is reported as catalogue/preset data rather than being falsely
 * projected into the facade. The overlay never creates a new selectable object
 * and therefore cannot diverge from Three.js, BOM or the object tree.
 *
 * @param input Resolved profile rectangle, orientation and section metadata.
 * @returns A non-interactive SVG group for the selected profile only.
 * @example A selected sash reports `面宽 58 mm` and `截面预设 ... · 深 55 mm`.
 * @since 0.10.74
 * @modified 2026-09-22 - Added selection-scoped frame/sash/mullion annotation.
 */
function renderSelectedProfileAnnotation(input: Readonly<{
  objectId: string;
  roleKind: "sash" | "sliding-sash" | "frame" | "flying-mullion" | "fixed-mullion" |
    "grid-mullion" | "topology-mullion";
  roleLabel: string;
  presetId: string;
  faceWidthMm: number;
  depthMm: number;
  orientation: "horizontal" | "vertical";
  xMm: number;
  yMm: number;
  widthMm: number;
  heightMm: number;
  scale: number;
  calloutY: number;
  windowWidth: number;
}>): string {
  const x = input.xMm * input.scale;
  const y = input.yMm * input.scale;
  const width = input.widthMm * input.scale;
  const height = input.heightMm * input.scale;
  const anchorX = x + width / 2;
  const anchorY = y + height / 2;
  const boxWidth = 252;
  const boxHeight = 38;
  const boxCenterX = Math.max(
    boxWidth / 2,
    Math.min(Math.max(boxWidth / 2, input.windowWidth - boxWidth / 2), anchorX)
  );
  const boxX = boxCenterX - boxWidth / 2;
  const boxY = input.calloutY - boxHeight;
  const faceLabel = `${formatCoordinate(input.faceWidthMm)} mm`;
  const faceLabelWidth = Math.max(48, faceLabel.length * 7 + 12);
  const faceDimension = input.orientation === "horizontal"
    ? (() => {
        const dimensionX = x + Math.min(Math.max(16, width * 0.16), Math.max(16, width - 16));
        const middleY = y + height / 2;
        return `<g class="design-profile-annotation__face-dimension" transform="translate(${formatCoordinate(dimensionX)} ${formatCoordinate(middleY)}) rotate(-90)"><line x1="${formatCoordinate(-height / 2)}" y1="0" x2="${formatCoordinate(height / 2)}" y2="0" /><line x1="${formatCoordinate(-height / 2)}" y1="-5" x2="${formatCoordinate(-height / 2)}" y2="5" /><line x1="${formatCoordinate(height / 2)}" y1="-5" x2="${formatCoordinate(height / 2)}" y2="5" /><rect x="${formatCoordinate(-faceLabelWidth / 2)}" y="4" width="${formatCoordinate(faceLabelWidth)}" height="17" rx="4" /><text x="0" y="16">${faceLabel}</text></g>`;
      })()
    : (() => {
        const dimensionY = y + Math.min(Math.max(16, height * 0.16), Math.max(16, height - 16));
        const middleX = x + width / 2;
        return `<g class="design-profile-annotation__face-dimension"><line x1="${formatCoordinate(x)}" y1="${formatCoordinate(dimensionY)}" x2="${formatCoordinate(x + width)}" y2="${formatCoordinate(dimensionY)}" /><line x1="${formatCoordinate(x)}" y1="${formatCoordinate(dimensionY - 5)}" x2="${formatCoordinate(x)}" y2="${formatCoordinate(dimensionY + 5)}" /><line x1="${formatCoordinate(x + width)}" y1="${formatCoordinate(dimensionY - 5)}" x2="${formatCoordinate(x + width)}" y2="${formatCoordinate(dimensionY + 5)}" /><rect x="${formatCoordinate(middleX - faceLabelWidth / 2)}" y="${formatCoordinate(dimensionY + 4)}" width="${formatCoordinate(faceLabelWidth)}" height="17" rx="4" /><text x="${formatCoordinate(middleX)}" y="${formatCoordinate(dimensionY + 16)}">${faceLabel}</text></g>`;
      })();
  return [
    `<g class="design-profile-annotation" data-profile-source-object-id="${escapeXml(input.objectId)}" data-profile-role="${input.roleKind}" data-profile-label="${escapeXml(input.roleLabel)}" data-profile-face-mm="${formatCoordinate(input.faceWidthMm)}" data-profile-depth-mm="${formatCoordinate(input.depthMm)}" data-section-preset-id="${escapeXml(input.presetId)}" pointer-events="none">`,
    `<path class="design-profile-annotation__leader" d="M ${formatCoordinate(anchorX)} ${formatCoordinate(anchorY)} L ${formatCoordinate(anchorX)} ${formatCoordinate(input.calloutY + 8)} L ${formatCoordinate(boxCenterX)} ${formatCoordinate(input.calloutY + 8)}" fill="none" stroke="#475569" stroke-width="1.2" vector-effect="non-scaling-stroke" />`,
    `<rect class="design-profile-annotation__card" x="${formatCoordinate(boxX)}" y="${formatCoordinate(boxY)}" width="${boxWidth}" height="${boxHeight}" rx="5" fill="#ffffff" fill-opacity="0.97" stroke="#94a3b8" stroke-width="1" vector-effect="non-scaling-stroke" />`,
    `<text class="design-profile-annotation__title" x="${formatCoordinate(boxCenterX)}" y="${formatCoordinate(boxY + 15)}" text-anchor="middle" fill="#0f172a" font-size="11" font-weight="600">${escapeXml(input.roleLabel)} · 面宽 ${formatCoordinate(input.faceWidthMm)} mm</text>`,
    `<text class="design-profile-annotation__section" x="${formatCoordinate(boxCenterX)}" y="${formatCoordinate(boxY + 30)}" text-anchor="middle" fill="#475569" font-size="10">截面预设 ${escapeXml(input.presetId)} · 深 ${formatCoordinate(input.depthMm)} mm</text>`,
    faceDimension,
    "</g>"
  ].join("");
}

/**
 * Renders read-only dimensions owned by one connected product aggregate.
 *
 * Child windows already render editable product sizes. This layer adds only
 * aggregate width/height and the physical connector-zone gap, distinguishing a
 * single-window size from the finished combination without pretending that an
 * assembly label edits one child window.
 *
 * @param geometry Canonical assembly rectangles and connector zones in mm.
 * @param baseX Screen X of the assembly bounds origin.
 * @param baseY Screen Y of the assembly bounds origin.
 * @param scale Shared millimetre-to-SVG scale.
 * @returns Non-interactive group controlled by the common dimension toggle.
 * @example A 1200+30+900 row shows total 2130mm and connection width 30mm.
 * @since 0.10.48
 * @modified 2026-09-21 - Added aggregate and connection dimensions.
 */
function renderFabricationAssemblyDimensions(
  geometry: ResolvedFabricationAssemblyGeometry,
  baseX: number,
  baseY: number,
  scale: number,
  laneLayout: AssemblyDimensionLaneLayout,
  selectedObjectId?: string
): string {
  const width = geometry.bounds.widthMm * scale;
  const height = geometry.bounds.heightMm * scale;
  // Member dimensions occupy the first lanes. Member captions, aggregate
  // dimensions and the aggregate title then receive separate lower bands.
  const overallBottom = baseY + height + 72 + laneLayout.maximumBottomLane * 34;
  const overallRight = baseX + width + 78 + laneLayout.maximumRightLane * 66;
  const assemblyWallObjectId = createWindowInstallationWallObjectId(
    geometry.assembly.objectId
  );
  const assemblySurroundObjectId = createWindowInstallationSurroundObjectId(
    geometry.assembly.objectId
  );
  const showOverall = !selectedObjectId ||
    selectedObjectId === geometry.assembly.objectId ||
    selectedObjectId === assemblyWallObjectId ||
    selectedObjectId.startsWith(`${assemblyWallObjectId}.`) ||
    selectedObjectId === assemblySurroundObjectId ||
    selectedObjectId.startsWith(`${assemblySurroundObjectId}.`);
  const overall = showOverall ? [
    renderDimensionLine({
      x1: baseX,
      y1: overallBottom,
      x2: baseX + width,
      y2: overallBottom,
      label: `组合总宽 ${formatCoordinate(geometry.bounds.widthMm)} mm`
    }),
    renderDimensionLine({
      x1: overallRight,
      y1: baseY,
      x2: overallRight,
      y2: baseY + height,
      label: `组合总高 ${formatCoordinate(geometry.bounds.heightMm)} mm`,
      vertical: true
    })
  ].join("") : "";
  const joints = geometry.joints.filter((joint) =>
    selectedObjectId === joint.jointId
  ).map((joint) => {
    const source = geometry.assembly.joints.find(
      (candidate) => candidate.objectId === joint.jointId
    );
    const gapMm = source?.gapMm ?? Math.min(joint.widthMm, joint.heightMm);
    const verticalZone = joint.heightMm >= joint.widthMm;
    const dimensionLabel = source?.jointType === "corner_joint"
      ? "角柱"
      : verticalZone ? "连接宽" : "连接高";
    const x = baseX + (joint.xMm - geometry.bounds.xMm) * scale;
    const y = baseY + (joint.yMm - geometry.bounds.yMm) * scale;
    return verticalZone
      ? renderDimensionLine({
          x1: x,
          y1: baseY - 18,
          x2: x + joint.widthMm * scale,
          y2: baseY - 18,
          label: `${dimensionLabel} ${formatCoordinate(gapMm)} mm`
        })
      : renderDimensionLine({
          x1: baseX + width + 18,
          y1: y,
          x2: baseX + width + 18,
          y2: y + joint.heightMm * scale,
          label: `${dimensionLabel} ${formatCoordinate(gapMm)} mm`,
          vertical: true
        });
  }).join("");
  return `<g class="design-assembly__dimensions" data-assembly-id="${escapeXml(geometry.assembly.objectId)}" pointer-events="none">${overall}${joints}</g>`;
}

/** Screen-space annotation lanes allocated to one assembly member instance. */
interface AssemblyDimensionLaneLayout {
  readonly byInstanceId: ReadonlyMap<string, Readonly<{
    top: number;
    right: number;
    bottom: number;
    /** True for the right-most owner of one identical vertical span. */
    showHeightDimensions: boolean;
  }>>;
  readonly maximumTopLane: number;
  readonly maximumRightLane: number;
  readonly maximumBottomLane: number;
}

/**
 * Allocates the three facade dimension directions for a connected assembly.
 *
 * Top and bottom dimensions measure horizontal spans; right dimensions measure
 * vertical spans. The SVG adapter now delegates collision and lane ownership to
 * the renderer-neutral paper-space annotation model. It consumes only the
 * returned lane index here, keeping the existing interactive SVG spacing while
 * factory/installation exports can consume physical paper-millimetre offsets.
 *
 * @since 0.10.52
 * @modified 2026-09-21 - Deduplicated equal-height member chains for the design canvas.
 */
function resolveAssemblyDimensionLaneLayout(
  geometry: ResolvedFabricationAssemblyGeometry
): AssemblyDimensionLaneLayout {
  const view: DrawingView = {
    viewId: `${geometry.assembly.objectId}:facade`,
    projection: "design-2d",
    viewKind: "elevation",
    sourceObjectIds: [geometry.assembly.objectId],
    // A unit scale preserves the previous span-overlap behaviour. Physical
    // export views supply their actual denominator and still use the same core.
    scaleDenominator: 1,
    modelBoundsMm: {
      x: geometry.bounds.xMm,
      y: geometry.bounds.yMm,
      width: geometry.bounds.widthMm,
      height: geometry.bounds.heightMm
    },
    framePaperMm: {
      x: geometry.bounds.xMm,
      y: geometry.bounds.yMm,
      width: geometry.bounds.widthMm,
      height: geometry.bounds.heightMm
    }
  };
  const createDimension = (
    instance: ResolvedFabricationAssemblyGeometry["instances"][number],
    side: DrawingLinearDimensionAnnotation["side"]
  ): DrawingLinearDimensionAnnotation => {
    const horizontal = side === "top" || side === "bottom";
    const startModelMm = horizontal ? instance.xMm : instance.yMm;
    const endModelMm = startModelMm + (horizontal ? instance.widthMm : instance.heightMm);
    return {
      annotationId: `${instance.instanceId}:dimension:${side}`,
      viewId: view.viewId,
      sourceObjectIds: [instance.instanceId, instance.windowId],
      layer: "dimensions",
      priority: 0,
      kind: "linear-dimension",
      axis: horizontal ? "horizontal" : "vertical",
      side,
      level: "detail",
      startModelMm,
      endModelMm,
      measuredValueMm: endModelMm - startModelMm,
      label: horizontal
        ? `${Math.round(instance.widthMm)} mm`
        : `${Math.round(instance.heightMm)} mm`
    };
  };
  /**
   * Choose one visible owner for every coincident vertical span.
   *
   * Side-by-side windows frequently have the same Y origin and height. Drawing
   * both inner-height chains plus both outer heights and the assembly height
   * produces five visually identical rulers. The right-most instance is the
   * conventional owner because its rulers sit nearest the assembly's right
   * dimension band. Stacked or offset members retain separate rulers because
   * their span keys differ.
   *
   * @example Two 1500mm-high windows at y=0 create one member-height chain;
   * the assembly still creates one separate overall-height ruler.
   * @since 0.10.63
   */
  const heightOwnerBySpan = new Map<string, string>();
  for (const instance of [...geometry.instances].sort((left, right) =>
    (right.xMm + right.widthMm) - (left.xMm + left.widthMm) ||
    left.instanceId.localeCompare(right.instanceId)
  )) {
    const spanKey = `${instance.yMm.toFixed(6)}:${instance.heightMm.toFixed(6)}`;
    if (!heightOwnerBySpan.has(spanKey)) {
      heightOwnerBySpan.set(spanKey, instance.instanceId);
    }
  }
  const heightOwnerIds = new Set(heightOwnerBySpan.values());
  const annotations = geometry.instances.flatMap((instance) => [
    createDimension(instance, "top"),
    ...(heightOwnerIds.has(instance.instanceId)
      ? [createDimension(instance, "right")]
      : []),
    createDimension(instance, "bottom")
  ]);
  const placements = layoutPaperSpaceDimensions(view, annotations);
  const placementById = new Map(placements.map((placement) => [
    placement.annotationId,
    placement
  ]));
  const byInstanceId = new Map<string, Readonly<{
    top: number;
    right: number;
    bottom: number;
    showHeightDimensions: boolean;
  }>>();
  for (const instance of geometry.instances) {
    byInstanceId.set(instance.instanceId, {
      top: placementById.get(`${instance.instanceId}:dimension:top`)?.laneIndex ?? 0,
      right: placementById.get(`${instance.instanceId}:dimension:right`)?.laneIndex ?? 0,
      bottom: placementById.get(`${instance.instanceId}:dimension:bottom`)?.laneIndex ?? 0,
      showHeightDimensions: heightOwnerIds.has(instance.instanceId)
    });
  }
  const maximumLane = (side: DrawingLinearDimensionAnnotation["side"]): number => Math.max(
    0,
    ...placements.filter((placement) => placement.side === side).map(
      (placement) => placement.laneIndex
    )
  );
  return {
    byInstanceId,
    maximumTopLane: maximumLane("top"),
    maximumRightLane: maximumLane("right"),
    maximumBottomLane: maximumLane("bottom")
  };
}

/** Returns true when one stable 2D/3D selection belongs to a child window. */
function windowOwnsRenderedObjectId(window: WindowUnit, objectId: string | undefined): boolean {
  if (!objectId) return false;
  if (window.objectId === objectId) return true;
  const wallId = createWindowInstallationWallObjectId(window.objectId);
  const surroundId = createWindowInstallationSurroundObjectId(window.objectId);
  if (
    objectId === wallId || objectId.startsWith(`${wallId}.`) ||
    objectId === surroundId || objectId.startsWith(`${surroundId}.`)
  ) return true;
  const geometry = resolveWindowGeometry(window);
  return [
    ...geometry.frames,
    ...geometry.cells,
    ...geometry.members,
    ...geometry.meetingMullions,
    ...geometry.openings.map((opening) => ({
      objectId: createOpeningPanelKey(opening.objectId, opening.panelId)
    })),
    ...geometry.slidingTracks,
    ...geometry.slidingPanels.map((panel) => ({
      objectId: createOpeningPanelKey(panel.sourceObjectId, panel.panelId)
    })),
    ...geometry.hardware.map((mount) => ({ objectId: mount.hardwareId }))
  ].some((item) => item.objectId === objectId);
}

/** Projects a non-coplanar assembly in its actual X/Z plan coordinates. */
function renderSpatialFabricationAssemblyPlan(input: Readonly<{
  geometry: ResolvedFabricationAssemblyGeometry;
  windowById: ReadonlyMap<string, WindowUnit>;
  scale: number;
  showDimensions: boolean;
  showOpeningState: boolean;
  selectedObjectId?: string;
  openingProgressPercentByPanelKey?: Readonly<Record<string, number>>;
  openingMotionModeByPanelKey?: Readonly<Record<string, OpeningMotionMode>>;
  visualAssets?: VisualAssetAvailability;
  showPlanOrientation?: boolean;
  showPlanLabel?: boolean;
  planOrientationX?: number;
}>): string {
  const { geometry, scale } = input;
  const padding = 16;
  const bounds = geometry.planBounds;
  const installation = normalizeWindowInstallation(geometry.assembly.installation);
  const maximumFrameDepthMm = Math.max(
    1,
    ...geometry.planInstances.map((instance) => instance.frameDepthMm)
  );
  const planMarginMm = Math.max(
    0,
    (installation.surround.wallThicknessMm - maximumFrameDepthMm) / 2 +
      installation.surround.boardThicknessMm,
    installation.surround.enabled
      ? Math.max(
          installation.surround.outsideWidthMm,
          installation.surround.insideWidthMm
        ) + installation.surround.boardThicknessMm
      : 0
  );
  const renderBounds = {
    minXMm: bounds.minXMm - planMarginMm,
    maxXMm: bounds.maxXMm + planMarginMm,
    minZMm: bounds.minZMm - planMarginMm,
    maxZMm: bounds.maxZMm + planMarginMm
  };
  const width = (renderBounds.maxXMm - renderBounds.minXMm) * scale;
  const height = (renderBounds.maxZMm - renderBounds.minZMm) * scale;
  const mapPoint = (point: { xMm: number; zMm: number }): { x: number; y: number } => ({
    x: padding + (point.xMm - renderBounds.minXMm) * scale,
    y: padding + (renderBounds.maxZMm - point.zMm) * scale
  });
  const transformPoint = (
    instance: ResolvedFabricationAssemblyGeometry["planInstances"][number],
    localXMm: number,
    localZMm: number
  ): { xMm: number; zMm: number } => {
    const radians = instance.rotationYDeg * Math.PI / 180;
    return {
      xMm: instance.originXMm + Math.cos(radians) * localXMm + Math.sin(radians) * localZMm,
      zMm: instance.originZMm - Math.sin(radians) * localXMm + Math.cos(radians) * localZMm
    };
  };
  const polygonPoints = (points: readonly { xMm: number; zMm: number }[]): string =>
    points.map((point) => {
      const mapped = mapPoint(point);
      return `${formatCoordinate(mapped.x)},${formatCoordinate(mapped.y)}`;
    }).join(" ");
  const selectedAttribute = (objectId: string): string =>
    input.selectedObjectId === objectId ? ' data-selected="true" aria-current="true"' : "";
  const firstPlanInstance = geometry.planInstances[0];
  const firstWindow = firstPlanInstance
    ? input.windowById.get(firstPlanInstance.windowId)
    : undefined;
  if (!firstPlanInstance || !firstWindow) {
    throw new Error(`Assembly ${geometry.assembly.objectId} has no spatial plan source.`);
  }
  const firstAppearance = resolveWindowVisualConfigurationForRender(firstWindow).appearance;
  const wallStyle = resolveAppearanceRenderStyle(firstAppearance.wall);
  const jointStyle = resolveAppearanceRenderStyle(firstAppearance.mullion.outside);
  const surroundStyles = {
    outside: resolveAppearanceRenderStyle(firstAppearance.surroundOutside),
    inside: resolveAppearanceRenderStyle(firstAppearance.surroundInside),
    liner: resolveAppearanceRenderStyle(firstAppearance.surroundLiner)
  };
  const wallPolygons = geometry.planInstances.map((instance) => {
    const wallSection = resolveInstallationSectionFromFrameDepth(
      geometry.assembly.installation,
      instance.frameDepthMm
    );
    const points = [
      transformPoint(instance, 0, wallSection.wallInsideZMm),
      transformPoint(instance, instance.widthMm, wallSection.wallInsideZMm),
      transformPoint(instance, instance.widthMm, wallSection.wallOutsideZMm),
      transformPoint(instance, 0, wallSection.wallOutsideZMm)
    ];
    const wallId = createWindowInstallationWallObjectId(geometry.assembly.objectId);
    return `<polygon class="design-assembly-plan-view__wall-plane" data-object-id="${escapeXml(wallId)}"${selectedAttribute(wallId)} data-instance-id="${escapeXml(instance.instanceId)}" points="${polygonPoints(points)}" fill="${wallStyle.cssColor}" fill-opacity="${formatCoordinate(Math.min(0.38, wallStyle.opacity))}" stroke="#a68a64" />`;
  }).join("");
  const connectedEdges = new Set(
    geometry.assembly.joints.flatMap((joint) => [
      `${joint.firstInstanceId}:${joint.firstEdge}`,
      `${joint.secondInstanceId}:${joint.secondEdge}`
    ])
  );
  const outsideEnabled = installation.surround.styleId === "both_sides" ||
    installation.surround.styleId === "outside_only";
  const insideEnabled = installation.surround.styleId === "both_sides" ||
    installation.surround.styleId === "inside_only";
  const linerEnabled = installation.surround.styleId === "both_sides" ||
    installation.surround.styleId === "liner";
  const surroundPolygons = installation.surround.enabled
    ? geometry.planInstances.flatMap((instance) => {
        const wallSection = resolveInstallationSectionFromFrameDepth(
          geometry.assembly.installation,
          instance.frameDepthMm
        );
        return (["left", "right"] as const).flatMap((side) => {
          if (
            !installation.surround.sides.includes(side) ||
            connectedEdges.has(`${instance.instanceId}:${side}`)
          ) return [];
          const boundaryX = side === "left" ? 0 : instance.widthMm;
          const createPiece = (
            layer: "outside" | "inside" | "liner",
            faceWidthMm: number,
            minZMm: number,
            maxZMm: number
          ): string => {
            const liner = layer === "liner";
            const minXMm = side === "left"
              ? liner ? boundaryX : boundaryX - faceWidthMm
              : liner ? boundaryX - faceWidthMm : boundaryX;
            const maxXMm = side === "left"
              ? liner ? boundaryX + faceWidthMm : boundaryX
              : liner ? boundaryX : boundaryX + faceWidthMm;
            const objectId = `${createWindowInstallationSurroundObjectId(
              geometry.assembly.objectId,
              layer,
              side
            )}.instance.${instance.instanceId}`;
            const points = [
              transformPoint(instance, minXMm, minZMm),
              transformPoint(instance, maxXMm, minZMm),
              transformPoint(instance, maxXMm, maxZMm),
              transformPoint(instance, minXMm, maxZMm)
            ];
            const style = surroundStyles[layer];
            return `<polygon class="design-assembly-plan-view__surround design-assembly-plan-view__surround--${layer}" data-object-id="${escapeXml(objectId)}"${selectedAttribute(objectId)} data-instance-id="${escapeXml(instance.instanceId)}" data-installation-side="${side}" data-layer="${layer}" data-material-code="${escapeXml(layer === "liner" ? `${installation.surround.materialCode}-LINER` : installation.surround.materialCode)}" points="${polygonPoints(points)}" fill="${style.cssColor}" fill-opacity="${formatCoordinate(style.opacity)}" stroke="${style.cssColor}" />`;
          };
          const pieces: string[] = [];
          if (linerEnabled) {
            pieces.push(createPiece(
              "liner",
              installation.surround.boardThicknessMm,
              wallSection.wallInsideZMm,
              wallSection.wallOutsideZMm
            ));
          }
          if (outsideEnabled) {
            pieces.push(createPiece(
              "outside",
              installation.surround.outsideWidthMm,
              wallSection.wallOutsideZMm,
              wallSection.wallOutsideZMm + installation.surround.boardThicknessMm
            ));
          }
          if (insideEnabled) {
            pieces.push(createPiece(
              "inside",
              installation.surround.insideWidthMm,
              wallSection.wallInsideZMm - installation.surround.boardThicknessMm,
              wallSection.wallInsideZMm
            ));
          }
          return pieces;
        });
      }).join("")
    : "";
  const frames = geometry.planInstances.map((instance) => {
    const window = input.windowById.get(instance.windowId);
    if (!window) return "";
    const appearance = resolveWindowVisualConfigurationForRender(window).appearance;
    const frameStyle = resolveAppearanceRenderStyle(appearance.frame.edge);
    const glassStyle = resolveAppearanceRenderStyle(appearance.glass);
    const innerStart = Math.min(window.frameFaceMm, instance.widthMm / 2);
    const innerEnd = Math.max(innerStart, instance.widthMm - window.frameFaceMm);
    const innerDepth = Math.max(1, instance.frameDepthMm - 10);
    const glassFootprint = [
      transformPoint(instance, innerStart, -innerDepth / 2),
      transformPoint(instance, innerEnd, -innerDepth / 2),
      transformPoint(instance, innerEnd, innerDepth / 2),
      transformPoint(instance, innerStart, innerDepth / 2)
    ];
    return `<g class="design-assembly-plan-view__spatial-instance" data-object-id="${escapeXml(instance.instanceId)}"${selectedAttribute(instance.instanceId)} data-window-id="${escapeXml(window.objectId)}" data-rotation-y-deg="${formatCoordinate(instance.rotationYDeg)}"><polygon class="design-assembly-plan-view__frame" points="${polygonPoints(instance.footprint)}" fill="${frameStyle.cssColor}" fill-opacity="${formatCoordinate(frameStyle.opacity)}" stroke="#475569" /><polygon class="design-assembly-plan-view__glass" points="${polygonPoints(glassFootprint)}" fill="${glassStyle.cssColor}" fill-opacity="${formatCoordinate(glassStyle.opacity)}" stroke="#7aa7bd" /></g>`;
  }).join("");
  const joints = geometry.planJoints.map((joint) => {
    const persisted = geometry.assembly.joints.find((item) => item.objectId === joint.jointId);
    const catalogAttributes = persisted?.catalogSelection
      ? ` data-catalog-item-id="${escapeXml(persisted.catalogSelection.catalogItemId)}" data-catalog-version="${escapeXml(persisted.catalogSelection.catalogVersion)}"`
      : "";
    return `<polygon class="design-assembly-plan-view__joint design-assembly-plan-view__joint--${joint.jointType}" data-object-id="${escapeXml(joint.jointId)}"${selectedAttribute(joint.jointId)} data-joint-type="${joint.jointType}" data-included-angle-deg="${formatCoordinate(joint.includedAngleDeg)}"${catalogAttributes} points="${polygonPoints(joint.footprint)}" fill="${jointStyle.cssColor}" fill-opacity="${formatCoordinate(jointStyle.opacity)}" stroke="#1e293b" />`;
  }).join("");
  const angleMarks = geometry.planJoints.filter(
    (joint) => input.showDimensions && joint.jointType === "corner_joint" &&
      (!input.selectedObjectId ||
        input.selectedObjectId === geometry.assembly.objectId ||
        input.selectedObjectId === joint.jointId)
  ).map((joint) => {
    const persisted = geometry.assembly.joints.find((item) => item.objectId === joint.jointId);
    const first = geometry.planInstances.find((item) => item.instanceId === joint.firstInstanceId);
    const second = geometry.planInstances.find((item) => item.instanceId === joint.secondInstanceId);
    if (!first || !second) return "";
    const centerOf = (instance: typeof first): { xMm: number; zMm: number } =>
      transformPoint(instance, instance.widthMm / 2, 0);
    const axis = mapPoint({ xMm: joint.axisXMm, zMm: joint.axisZMm });
    const firstCenter = mapPoint(centerOf(first));
    const secondCenter = mapPoint(centerOf(second));
    const unit = (point: { x: number; y: number }): { x: number; y: number } => {
      const dx = point.x - axis.x;
      const dy = point.y - axis.y;
      const length = Math.max(0.001, Math.hypot(dx, dy));
      return { x: dx / length, y: dy / length };
    };
    const firstUnit = unit(firstCenter);
    const secondUnit = unit(secondCenter);
    const radius = 28;
    const start = { x: axis.x + firstUnit.x * radius, y: axis.y + firstUnit.y * radius };
    const end = { x: axis.x + secondUnit.x * radius, y: axis.y + secondUnit.y * radius };
    const cross = firstUnit.x * secondUnit.y - firstUnit.y * secondUnit.x;
    const labelDirection = {
      x: firstUnit.x + secondUnit.x,
      y: firstUnit.y + secondUnit.y
    };
    const labelLength = Math.max(0.001, Math.hypot(labelDirection.x, labelDirection.y));
    const labelX = axis.x + labelDirection.x / labelLength * (radius + 13);
    const labelY = axis.y + labelDirection.y / labelLength * (radius + 13);
    const capability = persisted?.catalogSelection?.cornerCapability;
    const minimumAngleDeg = capability?.minimumIncludedAngleDeg ?? 1;
    const maximumAngleDeg = capability?.maximumIncludedAngleDeg ?? 179.9;
    const selected = input.selectedObjectId === joint.jointId;
    const handle = selected
      ? `<circle class="design-assembly-plan-view__corner-angle-handle" data-corner-angle-handle="true" data-object-id="${escapeXml(joint.jointId)}" cx="${formatCoordinate(end.x)}" cy="${formatCoordinate(end.y)}" r="6" fill="#ffffff" stroke="#1677ff" stroke-width="2" pointer-events="all" style="cursor:grab"><title>拖动修改转角夹角</title></circle>`
      : "";
    return `<g class="design-assembly-plan-view__corner-angle" data-object-id="${escapeXml(joint.jointId)}" data-assembly-id="${escapeXml(geometry.assembly.objectId)}" data-joint-id="${escapeXml(joint.jointId)}" data-included-angle-deg="${formatCoordinate(joint.includedAngleDeg)}" data-turn-direction="${joint.turnDirection ?? "clockwise"}" data-axis-x="${formatCoordinate(axis.x)}" data-axis-y="${formatCoordinate(axis.y)}" data-first-ray-angle-rad="${formatCoordinate(Math.atan2(firstUnit.y, firstUnit.x))}" data-radius="${radius}" data-min-angle-deg="${formatCoordinate(minimumAngleDeg)}" data-max-angle-deg="${formatCoordinate(maximumAngleDeg)}"><line class="design-assembly-plan-view__corner-angle-first-ray" x1="${formatCoordinate(axis.x)}" y1="${formatCoordinate(axis.y)}" x2="${formatCoordinate(start.x)}" y2="${formatCoordinate(start.y)}" stroke="#1677ff" pointer-events="none" /><line class="design-assembly-plan-view__corner-angle-second-ray" x1="${formatCoordinate(axis.x)}" y1="${formatCoordinate(axis.y)}" x2="${formatCoordinate(end.x)}" y2="${formatCoordinate(end.y)}" stroke="#1677ff" pointer-events="none" /><path class="design-assembly-plan-view__corner-angle-arc" d="M ${formatCoordinate(start.x)} ${formatCoordinate(start.y)} A ${radius} ${radius} 0 0 ${cross >= 0 ? 1 : 0} ${formatCoordinate(end.x)} ${formatCoordinate(end.y)}" fill="none" stroke="#1677ff" stroke-width="6" stroke-opacity="0" pointer-events="stroke" /><path class="design-assembly-plan-view__corner-angle-arc-visible" d="M ${formatCoordinate(start.x)} ${formatCoordinate(start.y)} A ${radius} ${radius} 0 0 ${cross >= 0 ? 1 : 0} ${formatCoordinate(end.x)} ${formatCoordinate(end.y)}" fill="none" stroke="#1677ff" stroke-dasharray="4 3" pointer-events="none" /><text class="design-assembly-plan-view__corner-angle-label" x="${formatCoordinate(labelX)}" y="${formatCoordinate(labelY)}" text-anchor="middle" fill="#0f4c81" font-size="11" pointer-events="none">${formatCoordinate(joint.includedAngleDeg)}°</text>${handle}</g>`;
  }).join("");
  const motion = input.showOpeningState
    ? geometry.planInstances.map((instance) => {
        const window = input.windowById.get(instance.windowId);
        if (!window) return "";
        return resolveWindowGeometry(window).openings.map((opening) => {
          const previewKey = createOpeningPanelKey(opening.objectId, opening.panelId);
          const mode = input.openingMotionModeByPanelKey?.[previewKey] ?? "primary";
          const projection = projectOpeningMotion(
            opening,
            input.openingProgressPercentByPanelKey?.[previewKey] ?? opening.openPercent,
            mode
          );
          const points = projection.planHullMm.map((point) =>
            transformPoint(instance, point.x, point.z));
          return `<polygon class="design-assembly-plan-view__opening-state" data-preview-panel-key="${escapeXml(previewKey)}" data-opening-angle-deg="${formatCoordinate(projection.angleRadians * 180 / Math.PI)}" points="${polygonPoints(points)}" fill="#1677ff" fill-opacity="0.22" stroke="#1677ff" stroke-width="1.5" />`;
        }).join("");
      }).join("")
    : "";
  const orientationX = input.planOrientationX ?? width + padding * 2 + 18;
  const orientation = input.showPlanOrientation === false
    ? ""
    : `<g class="design-plan-view__shared-orientation" data-placement="outside-right" data-aligned-with="facade-orientation" transform="translate(${formatCoordinate(orientationX)} 8)" pointer-events="none"><rect width="68" height="42" rx="5" fill="#ffffff" fill-opacity="0.96" stroke="#cbd5e1" /><text x="34" y="16" text-anchor="middle" fill="#075985" font-size="11" font-weight="600">室外 ↑</text><line x1="9" y1="22" x2="59" y2="22" stroke="#e2e8f0" /><text x="34" y="35" text-anchor="middle" fill="#9a3412" font-size="11" font-weight="600">室内 ↓</text></g>`;
  const label = input.showPlanLabel === false
    ? ""
    : `<text class="design-plan-view__shared-label" x="${formatCoordinate(padding + width / 2)}" y="${formatCoordinate(padding * 2 + height + 18)}" text-anchor="middle">俯视图</text>`;
  return `<g class="design-assembly-plan-view design-assembly-plan-view--spatial" data-object-id="${escapeXml(geometry.assembly.objectId)}" data-plan-projection="spatial" data-plan-width-mm="${formatCoordinate(bounds.widthMm)}" data-plan-depth-mm="${formatCoordinate(bounds.depthMm)}">${orientation}${wallPolygons}${surroundPolygons}${frames}${joints}${motion}${angleMarks}${label}</g>`;
}

/**
 * Projects one horizontal section through a connected assembly.
 *
 * The section is cut through the first instance centre. Every instance and
 * connector crossing that elevation is projected at its real assembly X/Z
 * transform, while the assembly owns one wall/opening and one indoor/outdoor
 * orientation. This removes the incorrect “one wall per child window” result.
 * A later section-line editor may move `sectionYMm`; the current deterministic
 * cut already handles straight N-window rows and documents which stacked tier
 * is visible.
 *
 * @param geometry Canonical connected-product placement.
 * @param windowById Product roots referenced by assembly instances.
 * @param scale Shared millimetre-to-SVG scale.
 * @param showDimensions Whether installation labels are visible.
 * @param showOpeningState Whether current sash poses are projected.
 * @returns One assembly-owned plan group using local assembly X coordinates.
 * @example A two-window row produces one 2130mm wall opening, two frames and J1.
 * @since 0.10.48
 * @modified 2026-09-21 - Replaced repeated child plans with one assembly section.
 */
function renderFabricationAssemblyPlan(input: Readonly<{
  geometry: ResolvedFabricationAssemblyGeometry;
  windowById: ReadonlyMap<string, WindowUnit>;
  scale: number;
  showDimensions: boolean;
  showOpeningState: boolean;
  selectedObjectId?: string;
  openingProgressPercentByPanelKey?: Readonly<Record<string, number>>;
  openingMotionModeByPanelKey?: Readonly<Record<string, OpeningMotionMode>>;
  visualAssets?: VisualAssetAvailability;
  /** Emits the canvas-level indoor/outdoor plan legend for this subject. */
  showPlanOrientation?: boolean;
  /** Emits the single visible `俯视图` caption for this subject. */
  showPlanLabel?: boolean;
  /**
   * Local X coordinate shared with the facade orientation card. Supplying the
   * same coordinate keeps elevation and plan observation legends in one
   * vertical annotation column without coupling either label to wall geometry.
   */
  planOrientationX?: number;
}>): string {
  const { geometry, scale } = input;
  if (!geometry.isCoplanar) return renderSpatialFabricationAssemblyPlan(input);
  const firstInstance = geometry.instances[0];
  const firstWindow = firstInstance
    ? input.windowById.get(firstInstance.windowId)
    : undefined;
  if (!firstInstance || !firstWindow) {
    throw new Error(`Assembly ${geometry.assembly.objectId} has no plan-section source.`);
  }
  const sectionYMm = firstInstance.yMm + firstInstance.heightMm / 2;
  const activeInstances = geometry.instances.filter((instance) =>
    sectionYMm >= instance.yMm && sectionYMm <= instance.yMm + instance.heightMm
  );
  const activeJoints = geometry.joints.filter((joint) =>
    sectionYMm >= joint.yMm && sectionYMm <= joint.yMm + joint.heightMm
  );
  const persistedByInstanceId = new Map(
    geometry.assembly.instances.map((instance) => [instance.objectId, instance])
  );
  const maximumFrameDepthMm = Math.max(...activeInstances.map((instance) => {
    const window = input.windowById.get(instance.windowId);
    if (!window) throw new Error(`Assembly window ${instance.windowId} could not be resolved.`);
    return resolveWindowSectionDimensions(window).frameDepthMm;
  }));
  const installation = normalizeWindowInstallation(geometry.assembly.installation);
  const installationSection = resolveInstallationSectionFromFrameDepth(
    installation,
    maximumFrameDepthMm
  );
  const planEnvelopes = activeInstances.map((instance) => {
    const window = input.windowById.get(instance.windowId);
    const persisted = persistedByInstanceId.get(instance.instanceId);
    if (!window || !persisted) {
      throw new Error(`Assembly instance ${instance.instanceId} could not resolve plan data.`);
    }
    const envelope = resolveWindowPlanEnvelopeMm(window);
    return {
      minZMm: envelope.minZMm + persisted.transform.zMm,
      maxZMm: envelope.maxZMm + persisted.transform.zMm
    };
  });
  const minZMm = Math.min(
    installationSection.wallInsideZMm,
    ...planEnvelopes.map((envelope) => envelope.minZMm)
  );
  const maxZMm = Math.max(
    installationSection.wallOutsideZMm,
    ...planEnvelopes.map((envelope) => envelope.maxZMm)
  );
  const planPadding = 16;
  const baselineY = planPadding + maxZMm * scale;
  const wallTopY = baselineY - installationSection.wallOutsideZMm * scale;
  const wallBottomY = baselineY - installationSection.wallInsideZMm * scale;
  const wallHeight = wallBottomY - wallTopY;
  const planHeight = (maxZMm - minZMm) * scale + planPadding * 2;
  const width = geometry.bounds.widthMm * scale;
  const firstAppearance = resolveWindowVisualConfigurationForRender(firstWindow).appearance;
  const wallStyle = resolveAppearanceRenderStyle(firstAppearance.wall);
  const wallPatternId = `assembly-plan-wall-${geometry.assembly.objectId.replace(/[^a-zA-Z0-9_-]/g, "-")}`;
  const wallPaint = createSvgTextureFallbackPaint(
    firstAppearance.wall,
    wallPatternId,
    input.visualAssets
  );
  const jointStyle = resolveAppearanceRenderStyle(firstAppearance.mullion.outside);
  const selectedAttribute = (objectId: string): string =>
    input.selectedObjectId === objectId ? ' data-selected="true" aria-current="true"' : "";
  const frames = activeInstances.map((instance) => {
    const window = input.windowById.get(instance.windowId);
    const persisted = persistedByInstanceId.get(instance.instanceId);
    if (!window || !persisted) return "";
    const section = resolveWindowSectionDimensions(window);
    const appearance = resolveWindowVisualConfigurationForRender(window).appearance;
    const frameStyle = resolveAppearanceRenderStyle(appearance.frame.edge);
    const glassStyle = resolveAppearanceRenderStyle(appearance.glass);
    const x = (instance.xMm - geometry.bounds.xMm) * scale;
    const frameTopY = baselineY -
      (persisted.transform.zMm + section.frameDepthMm / 2) * scale;
    const frameDepth = section.frameDepthMm * scale;
    const innerX = x + window.frameFaceMm * scale;
    const innerWidth = Math.max(0, instance.widthMm * scale - window.frameFaceMm * scale * 2);
    return [
      `<rect class="design-assembly-plan-view__frame" data-object-id="${escapeXml(instance.instanceId)}"${selectedAttribute(instance.instanceId)} data-window-id="${escapeXml(window.objectId)}" x="${formatCoordinate(x)}" y="${formatCoordinate(frameTopY)}" width="${formatCoordinate(instance.widthMm * scale)}" height="${formatCoordinate(frameDepth)}" fill="${frameStyle.cssColor}" fill-opacity="${formatCoordinate(frameStyle.opacity)}" stroke="#475569" />`,
      `<rect class="design-assembly-plan-view__glass" x="${formatCoordinate(innerX)}" y="${formatCoordinate(frameTopY + 5)}" width="${formatCoordinate(innerWidth)}" height="${formatCoordinate(Math.max(1, frameDepth - 10))}" fill="${glassStyle.cssColor}" fill-opacity="${formatCoordinate(glassStyle.opacity)}" stroke="#7aa7bd" />`
    ].join("");
  }).join("");
  const joints = activeJoints.map((joint) => {
    const persisted = geometry.assembly.joints.find((item) => item.objectId === joint.jointId);
    const first = persisted
      ? persistedByInstanceId.get(persisted.firstInstanceId)
      : undefined;
    const zMm = first?.transform.zMm ?? 0;
    const x = (joint.xMm - geometry.bounds.xMm) * scale;
    const topY = baselineY - (zMm + maximumFrameDepthMm / 2) * scale;
    const catalogAttributes = persisted?.catalogSelection
      ? ` data-catalog-item-id="${escapeXml(persisted.catalogSelection.catalogItemId)}" data-catalog-version="${escapeXml(persisted.catalogSelection.catalogVersion)}"`
      : "";
    return `<rect class="design-assembly-plan-view__joint" data-object-id="${escapeXml(joint.jointId)}"${selectedAttribute(joint.jointId)} data-joint-type="${joint.jointType}"${catalogAttributes} x="${formatCoordinate(x)}" y="${formatCoordinate(topY)}" width="${formatCoordinate(joint.widthMm * scale)}" height="${formatCoordinate(maximumFrameDepthMm * scale)}" fill="${jointStyle.cssColor}" fill-opacity="${formatCoordinate(jointStyle.opacity)}" stroke="#1e293b" />`;
  }).join("");
  const motion = input.showOpeningState
    ? activeInstances.map((instance) => {
        const window = input.windowById.get(instance.windowId);
        const persisted = persistedByInstanceId.get(instance.instanceId);
        if (!window || !persisted) return "";
        const offsetXMm = instance.xMm - geometry.bounds.xMm;
        return resolveWindowGeometry(window).openings.map((opening) => {
          const previewKey = createOpeningPanelKey(opening.objectId, opening.panelId);
          const motionMode = input.openingMotionModeByPanelKey?.[previewKey] ?? "primary";
          const projection = projectOpeningMotion(
            opening,
            input.openingProgressPercentByPanelKey?.[previewKey] ?? opening.openPercent,
            motionMode
          );
          const closed = projectOpeningMotion(opening, 0, motionMode);
          const points = projection.planHullMm.map((point) =>
            `${formatCoordinate((offsetXMm + point.x) * scale)},${formatCoordinate(baselineY - (persisted.transform.zMm + point.z) * scale)}`
          ).join(" ");
          const closedPoints = closed.planHullMm.map((point) =>
            `${formatCoordinate((offsetXMm + point.x) * scale)},${formatCoordinate(baselineY - (persisted.transform.zMm + point.z) * scale)}`
          ).join(" ");
          const angleDegrees = projection.angleRadians * 180 / Math.PI;
          const closedFootprint = projection.progressPercent > 0.001
            ? `<polygon class="design-assembly-plan-view__opening-closed" points="${closedPoints}" fill="none" stroke="#64748b" stroke-width="1.25" stroke-dasharray="5 4" />`
            : "";
          const angle = projection.planAngleAnnotation
            ? `<g transform="translate(${formatCoordinate(offsetXMm * scale)} 0)">${renderOpeningAngleAnnotationSvg(
                projection.planAngleAnnotation,
                scale,
                "design-assembly-plan-view__opening-angle-mark",
                (valueMm) => baselineY - (persisted.transform.zMm + valueMm) * scale
              )}</g>`
            : "";
          return `<g class="design-assembly-plan-view__opening" data-preview-panel-key="${escapeXml(previewKey)}" data-preview-progress="${projection.progressPercent}" data-motion-mode="${motionMode}" data-opening-angle-deg="${formatCoordinate(angleDegrees)}">${closedFootprint}<polygon class="design-assembly-plan-view__opening-state" points="${points}" fill="#1677ff" fill-opacity="0.25" stroke="#1677ff" stroke-width="1.5" />${angle}</g>`;
        }).join("");
      }).join("")
    : "";
  const dimensions = input.showDimensions
    ? `<g class="design-assembly-plan-view__dimensions" pointer-events="none"><line x1="${formatCoordinate(width + 18)}" y1="${formatCoordinate(wallTopY)}" x2="${formatCoordinate(width + 18)}" y2="${formatCoordinate(wallBottomY)}" stroke="#64748b" /></g>`
    : "";
  const wallId = createWindowInstallationWallObjectId(geometry.assembly.objectId);
  const orientationX = input.planOrientationX ?? width + 18;
  const orientation = input.showPlanOrientation === false
    ? ""
    : `<g class="design-plan-view__shared-orientation" data-placement="outside-right" data-aligned-with="facade-orientation" transform="translate(${formatCoordinate(orientationX)} 8)" pointer-events="none"><rect width="68" height="42" rx="5" fill="#ffffff" fill-opacity="0.96" stroke="#cbd5e1" /><text class="design-plan-view__side-label design-plan-view__side-label--outside" x="34" y="16" text-anchor="middle" fill="#075985" font-size="11" font-weight="600">室外 ↑</text><line x1="9" y1="22" x2="59" y2="22" stroke="#e2e8f0" /><text class="design-plan-view__side-label design-plan-view__side-label--inside" x="34" y="35" text-anchor="middle" fill="#9a3412" font-size="11" font-weight="600">室内 ↓</text></g>`;
  const label = input.showPlanLabel === false
    ? ""
    : `<text class="design-plan-view__shared-label" x="${formatCoordinate(width / 2)}" y="${formatCoordinate(planHeight + 18)}" text-anchor="middle">俯视图</text>`;
  return `<g class="design-assembly-plan-view" data-object-id="${escapeXml(geometry.assembly.objectId)}" data-plan-wall-owner-id="${escapeXml(wallId)}" data-section-y-mm="${formatCoordinate(sectionYMm)}" data-plan-wall-thickness-mm="${formatCoordinate(installationSection.wallThicknessMm)}" data-plan-mm-scale="${formatCoordinate(scale)}" aria-label="${escapeXml(geometry.assembly.mark)}组合俯视截面">${wallPaint.definition ? `<defs>${wallPaint.definition}</defs>` : ""}${orientation}<rect class="design-assembly-plan-view__wall" data-object-id="${escapeXml(wallId)}"${selectedAttribute(wallId)}${wallPaint.metadata} width="${formatCoordinate(width)}" y="${formatCoordinate(wallTopY)}" height="${formatCoordinate(wallHeight)}" fill="${wallPaint.fill}" fill-opacity="${formatCoordinate(wallStyle.opacity)}" stroke="#a68a64" />${frames}${joints}${motion}${dimensions}${label}</g>`;
}

/**
 * Renders one rectangular window at a shared engineering scale.
 *
 * The function reads millimetre dimensions from the domain object and writes
 * the stable ID to `data-object-id`. It never feeds SVG dimensions back into
 * the model or BOM calculation.
 *
 * @param window Domain window to render.
 * @param x Left screen coordinate.
 * @param y Top screen coordinate.
 * @param scale Pixels per design millimetre.
 * @returns An SVG group string.
 * Grid cells, implicit dividers and explicit topology members all come from the
 * shared geometry resolver. Invalid local partitions remain visible with a red
 * dashed treatment instead of being silently hidden.
 *
 * @example A T-junction becomes three visible regions separated by two members.
 * @since 0.1.0
 * @modified 2026-09-21 - Kept complete 2D dimensions for the selected window and moved them outside the assembly envelope.
 */
function renderWindow(
  window: WindowUnit,
  x: number,
  y: number,
  scale: number,
  selectedObjectId: string | undefined,
  showDimensions: boolean,
  showPlanView: boolean,
  showOpeningState: boolean,
  openingProgressPercentByPanelKey: Readonly<Record<string, number>> | undefined,
  openingMotionModeByPanelKey: Readonly<Record<string, OpeningMotionMode>> | undefined,
  openingConnectionPresetByPanelKey: Readonly<Record<string, OpeningConnectionLayoutPreset>> | undefined,
  visualAssets: VisualAssetAvailability | undefined,
  presentation: Readonly<{
    dimensionEnvelopeInsetsMm?: Readonly<{
      top: number;
      right: number;
      bottom: number;
      left: number;
    }>;
    showFacadeOrientation?: boolean;
    /**
     * Zero-based annotation lanes assigned by the assembly layout. Two windows
     * may reuse one lane only when their measured spans do not overlap.
     *
     * @example Side-by-side windows share a top lane but use separate right lanes.
     * @since 0.10.52
     */
    dimensionLanes?: Readonly<{
      top: number;
      right: number;
      bottom: number;
    }>;
    /** Fine-grained dimension visibility used to remove assembly duplicates. */
    dimensionVisibility?: Readonly<{
      cellHeights?: boolean;
      outerHeight?: boolean;
    }>;
    /** Assembly rendering owns member captions, so child captions can be suppressed. */
    showMark?: boolean;
    /**
     * Whether this subject owns the canvas-level indoor/outdoor legend beside
     * the plan projection. Independent windows keep separate plan geometry,
     * but only the final visible subject emits this shared orientation legend.
     *
     * @example Two independent windows render two plan sections and one pair
     * of `室外 ↑` / `室内 ↓` labels.
     * @since 0.10.83
     */
    showPlanOrientation?: boolean;
    /**
     * Whether this subject owns the single visible `俯视图` caption.
     * Accessibility labels and model metadata remain available on every plan.
     *
     * @since 0.10.83
     */
    showPlanLabel?: boolean;
  }> = {}
): string {
  const width = window.widthMm * scale;
  const height = window.heightMm * scale;
  const geometry = resolveWindowGeometry(window);
  const dimensionEnvelopeInsetsMm = presentation.dimensionEnvelopeInsetsMm ?? {
    top: 0,
    right: 0,
    bottom: 0,
    left: 0
  };
  const dimensionLanes = presentation.dimensionLanes ?? { top: 0, right: 0, bottom: 0 };
  const section = resolveWindowSectionDimensions(window);
  const installation = normalizeWindowInstallation(window.installation);
  const installationSection = resolveWindowInstallationSection(window);
  const visualConfiguration = resolveWindowVisualConfigurationForRender(window);
  const assetDiagnostics: readonly VisualAssetDiagnostic[] = diagnoseWindowVisualAssets(
    visualConfiguration,
    visualAssets
  );
  const frameOutside = resolveAppearanceRenderStyle(
    visualConfiguration.appearance.frame.outside
  );
  const frameEdge = resolveAppearanceRenderStyle(
    visualConfiguration.appearance.frame.edge
  );
  const sashOutside = resolveAppearanceRenderStyle(
    visualConfiguration.appearance.sash.outside
  );
  const mullionOutside = resolveAppearanceRenderStyle(
    visualConfiguration.appearance.mullion.outside
  );
  const flyingMullionOutside = resolveAppearanceRenderStyle(
    visualConfiguration.appearance.flyingMullion.outside
  );
  const glassStyle = resolveAppearanceRenderStyle(visualConfiguration.appearance.glass);
  const wallStyle = resolveAppearanceRenderStyle(visualConfiguration.appearance.wall);
  const surroundOutsideStyle = resolveAppearanceRenderStyle(
    visualConfiguration.appearance.surroundOutside
  );
  const surroundInsideStyle = resolveAppearanceRenderStyle(
    visualConfiguration.appearance.surroundInside
  );
  const surroundLinerStyle = resolveAppearanceRenderStyle(
    visualConfiguration.appearance.surroundLiner
  );
  const id = escapeXml(window.objectId);
  const mark = escapeXml(window.mark);
  const selectedAttribute = (objectId: string): string =>
    objectId === selectedObjectId || Boolean(
      selectedObjectId && (
        objectId.startsWith(`${selectedObjectId}.`) ||
        objectId.startsWith(`${selectedObjectId}::`)
      )
    )
      ? ' data-selected="true" aria-current="true"'
      : "";
  const wallPaint = createSvgTextureFallbackPaint(
    visualConfiguration.appearance.wall,
    createSvgDefinitionId("texture", window.objectId, "wall"),
    visualAssets
  );
  const surroundOutsidePaint = createSvgTextureFallbackPaint(
    visualConfiguration.appearance.surroundOutside,
    createSvgDefinitionId("texture", window.objectId, "surround-outside"),
    visualAssets
  );
  const surroundInsidePaint = createSvgTextureFallbackPaint(
    visualConfiguration.appearance.surroundInside,
    createSvgDefinitionId("texture", window.objectId, "surround-inside"),
    visualAssets
  );
  const surroundLinerPaint = createSvgTextureFallbackPaint(
    visualConfiguration.appearance.surroundLiner,
    createSvgDefinitionId("texture", window.objectId, "surround-liner"),
    visualAssets
  );
  const textureDefinitions = [
    wallPaint.definition,
    surroundOutsidePaint.definition,
    surroundInsidePaint.definition,
    surroundLinerPaint.definition
  ].filter(Boolean).join("");
  const installationObstacles = resolveWindowInstallationObstacleGeometry(window);
  const facadeInsets = resolveSvgFacadeSurroundInsetsMm(window);
  const facadeSurround = installationObstacles
    .filter((obstacle) => obstacle.kind === "surround" && obstacle.layer === "outside")
    .map((obstacle) => {
      const objectId = createWindowInstallationSurroundObjectId(
        window.objectId,
        "outside",
        obstacle.side
      );
      const x = (obstacle.min.x + window.widthMm / 2) * scale;
      const y = (window.heightMm / 2 - obstacle.max.y) * scale;
      const obstacleWidth = (obstacle.max.x - obstacle.min.x) * scale;
      const obstacleHeight = (obstacle.max.y - obstacle.min.y) * scale;
      return `<rect class="design-window__surround design-window__surround--outside" data-object-id="${escapeXml(objectId)}"${selectedAttribute(objectId)} data-source-component-id="${escapeXml(obstacle.sourceComponentId ?? obstacle.obstacleId)}" data-installation-side="${obstacle.side}" data-surround-layer="outside"${surroundOutsidePaint.metadata} x="${formatCoordinate(x)}" y="${formatCoordinate(y)}" width="${formatCoordinate(obstacleWidth)}" height="${formatCoordinate(obstacleHeight)}" fill="${surroundOutsidePaint.fill}" fill-opacity="${formatCoordinate(surroundOutsideStyle.opacity)}" stroke="${surroundOutsideStyle.cssColor}" />`;
    }).join("");
  const diagnosticCodes = [...new Set(assetDiagnostics.map(({ code }) => code))].join(",");
  const diagnosticMetadata = assetDiagnostics.length > 0
    ? `<metadata class="design-window__asset-diagnostics">${escapeXml(JSON.stringify(assetDiagnostics))}</metadata>`
    : "";
  const resolvePanelRuntime = (
    sourceObjectId: string,
    panelId: "P1" | "P2"
  ): Readonly<{ opening: ResolvedOpening; progressPercent: number; motionMode: OpeningMotionMode }> | undefined => {
    const opening = geometry.openings.find(
      (candidate) => candidate.objectId === sourceObjectId && candidate.panelId === panelId
    );
    if (!opening) return undefined;
    const previewKey = createOpeningPanelKey(opening.objectId, opening.panelId);
    return {
      opening,
      progressPercent: openingProgressPercentByPanelKey?.[previewKey] ?? opening.openPercent,
      motionMode: openingMotionModeByPanelKey?.[previewKey] ?? "primary"
    };
  };
  /**
   * The three short parallel slashes are a symbolic glass mark, not texture.
   * They remain visible across material colours and future engineering-line
   * mode while the backing rectangle continues to carry the actual catalogue
   * appearance and stable selection identity. A valid local-member topology
   * receives one mark per resulting rectangular pane; the marks remain purely
   * graphical and do not invent additional selectable design objects.
   * @since 0.10.66
   */
  const cells = geometry.cells.map((cell) => {
    const left = cell.xMm * scale;
    const top = cell.yMm * scale;
    const cellWidth = cell.widthMm * scale;
    const cellHeight = cell.heightMm * scale;
    const topologyMembers = window.topology.members.filter(
      (member) => member.hostRegionId === cell.objectId
    );
    const partition = partitionTopologyRegion(topologyMembers);
    const glassRegions = topologyMembers.length > 0 && partition.valid
      ? partition.regions
      : [{ xStart: 0, xEnd: 1, yStart: 0, yEnd: 1 }];
    const glassSymbols = glassRegions.map((region, regionIndex) => {
      const regionLeft = left + cellWidth * region.xStart;
      const regionTop = top + cellHeight * region.yStart;
      const regionWidth = cellWidth * (region.xEnd - region.xStart);
      const regionHeight = cellHeight * (region.yEnd - region.yStart);
      const regionRight = regionLeft + regionWidth;
      const regionBottom = regionTop + regionHeight;
      const slashLength = Math.max(6, Math.min(18, regionWidth / 5, regionHeight / 5));
      const slashGap = slashLength * 0.42;
      const slashStartX = Math.max(
        regionLeft + 7,
        regionRight - 7 - slashLength - slashGap * 2
      );
      const slashStartY = Math.max(regionTop + slashLength + 7, regionBottom - 7);
      const slashes = [0, 1, 2].map((index) => {
        const offset = slashGap * index;
        return `<line x1="${formatCoordinate(slashStartX + offset)}" y1="${formatCoordinate(slashStartY)}" x2="${formatCoordinate(slashStartX + slashLength + offset)}" y2="${formatCoordinate(slashStartY - slashLength)}" />`;
      }).join("");
      return `<g class="design-window__glass-symbol" data-cell-id="${escapeXml(cell.objectId)}" data-glass-region-index="${regionIndex}" data-glass-symbol="three-slash" fill="none" stroke="#0f4c81" stroke-opacity="0.62" stroke-width="1.25" pointer-events="none">${slashes}</g>`;
    }).join("");
    return [
      `<rect class="design-window__glass" data-object-id="${escapeXml(cell.objectId)}"${selectedAttribute(cell.objectId)} data-row="${cell.row}" data-column="${cell.column}" x="${formatCoordinate(left)}" y="${formatCoordinate(top)}" width="${formatCoordinate(cellWidth)}" height="${formatCoordinate(cellHeight)}" fill="${glassStyle.cssColor}" fill-opacity="${formatCoordinate(glassStyle.opacity)}" stroke="#7aa7bd" />`,
      glassSymbols
    ].join("");
  });
  const foregroundOpenings: string[] = [];
  const slidingPanels = [...geometry.slidingPanels]
    // Interior rails are painted first so the exterior rail remains visually in front.
    .sort((leftPanel, rightPanel) => rightPanel.trackIndex - leftPanel.trackIndex)
    .map((panel) => {
      const previewKey = createOpeningPanelKey(panel.sourceObjectId, panel.panelId);
      const progressPercent = showOpeningState
        ? openingProgressPercentByPanelKey?.[previewKey] ?? panel.openPercent
        : 0;
      const offsetXMm = resolveSlidingPanelOffsetMm(panel, progressPercent);
      const panelX = panel.xMm + offsetXMm;
      const faceMm = Math.min(window.sashFaceMm, panel.widthMm / 2, panel.heightMm / 2);
      const outerX = panelX * scale;
      const outerY = panel.yMm * scale;
      const panelWidth = panel.widthMm * scale;
      const panelHeight = panel.heightMm * scale;
      const innerX = (panelX + faceMm) * scale;
      const innerY = (panel.yMm + faceMm) * scale;
      const innerWidth = Math.max(0, (panel.widthMm - faceMm * 2) * scale);
      const innerHeight = Math.max(0, (panel.heightMm - faceMm * 2) * scale);
      const direction = panel.travelDirection;
      const arrow = !showOpeningState || !direction
        ? ""
        : (() => {
            const centerY = (panel.yMm + panel.heightMm / 2) * scale;
            const startX = (panelX + panel.widthMm * (direction === "left" ? 0.62 : 0.38)) * scale;
            const endX = (panelX + panel.widthMm * (direction === "left" ? 0.38 : 0.62)) * scale;
            const sign = direction === "left" ? -1 : 1;
            return `<path class="design-window__sliding-direction" d="M ${formatCoordinate(startX)} ${formatCoordinate(centerY)} L ${formatCoordinate(endX)} ${formatCoordinate(centerY)} M ${formatCoordinate(endX)} ${formatCoordinate(centerY)} L ${formatCoordinate(endX - sign * 10)} ${formatCoordinate(centerY - 7)} M ${formatCoordinate(endX)} ${formatCoordinate(centerY)} L ${formatCoordinate(endX - sign * 10)} ${formatCoordinate(centerY + 7)}" fill="none" stroke="#0755b5" stroke-width="2" pointer-events="none" />`;
          })();
      return [
        `<g class="design-window__sliding-panel" data-object-id="${escapeXml(previewKey)}"${selectedAttribute(previewKey)} data-opening-object-id="${escapeXml(panel.sourceObjectId)}" data-window-id="${id}" data-source-component-id="${escapeXml(panel.sourceComponentId)}" data-mechanism="sliding" data-panel-id="${escapeXml(panel.panelId)}" data-panel-role="${panel.role}" data-track-index="${panel.trackIndex}" data-closed-x-mm="${formatCoordinate(panel.xMm)}" data-panel-pitch-mm="${formatCoordinate(panel.panelPitchMm)}" data-maximum-travel-mm="${formatCoordinate(panel.maximumTravelMm)}" data-preview-progress="${formatCoordinate(progressPercent)}" data-preview-translation-x-mm="${formatCoordinate(offsetXMm)}"${direction ? ` data-travel-direction="${direction}"` : ""}>`,
        `<rect class="design-window__sliding-glass" x="${formatCoordinate(innerX)}" y="${formatCoordinate(innerY)}" width="${formatCoordinate(innerWidth)}" height="${formatCoordinate(innerHeight)}" fill="${glassStyle.cssColor}" fill-opacity="${formatCoordinate(glassStyle.opacity)}" stroke="#7aa7bd" />`,
        `<rect class="design-window__sliding-sash-outline" data-profile-face-mm="${formatCoordinate(faceMm)}" x="${formatCoordinate((panelX + faceMm / 2) * scale)}" y="${formatCoordinate((panel.yMm + faceMm / 2) * scale)}" width="${formatCoordinate(Math.max(0, (panel.widthMm - faceMm) * scale))}" height="${formatCoordinate(Math.max(0, (panel.heightMm - faceMm) * scale))}" fill="none" stroke="${sashOutside.cssColor}" stroke-opacity="${formatCoordinate(sashOutside.opacity)}" stroke-width="${formatCoordinate(Math.max(3, faceMm * scale))}" />`,
        `<rect class="design-window__sliding-sash-linework" x="${formatCoordinate(outerX)}" y="${formatCoordinate(outerY)}" width="${formatCoordinate(panelWidth)}" height="${formatCoordinate(panelHeight)}" fill="none" stroke="#334155" stroke-width="1.25" pointer-events="none" />`,
        arrow,
        "</g>"
      ].join("");
    });
  const openings = geometry.openings.map((opening) => {
    const halfFace = opening.sashFaceMm / 2;
    const left = opening.xMm + halfFace;
    const right = opening.xMm + opening.widthMm - halfFace;
    const top = opening.yMm + halfFace;
    const bottom = opening.yMm + opening.heightMm - halfFace;
    const middleY = (top + bottom) / 2;
    const previewKey = createOpeningPanelKey(opening.objectId, opening.panelId);
    const progressPercent =
      openingProgressPercentByPanelKey?.[previewKey] ?? opening.openPercent;
    const motionMode = openingMotionModeByPanelKey?.[previewKey] ?? "primary";
    const projection = projectOpeningMotion(opening, progressPercent, motionMode);
    const maximumAngleDegrees = resolveRenderedOpeningMaximumAngleDegrees(opening, motionMode);
    const motionMetadata = ` data-preview-panel-key="${escapeXml(previewKey)}" data-preview-progress="${projection.progressPercent}" data-motion-mode="${motionMode}"`;
    const openingPlane = opening.opening.endsWith("_out") ? "out" : "in";
    const openingSymbolClass = `design-window__opening-symbol design-window__opening-symbol--${openingPlane}`;
    const openingSymbolDash = openingPlane === "out" ? ' stroke-dasharray="7 5"' : "";
    const openingSymbolLabel = openingPlane === "out" ? "外开图示" : "内开图示";
    /**
     * Material view keeps the catalogue-coloured wide centre-line stroke. The
     * engineering view hides that paint but retains it as the generous hit
     * target, then reveals exact outer/inner profile boundaries. This prevents
     * a sash rail from looking like a solid section cut while preserving the
     * same physical face width and panel selection identity.
     * @since 0.10.74
     */
    const sashOutline = `<rect class="design-window__opening-sash-outline" data-profile-face-mm="${formatCoordinate(opening.sashFaceMm)}" x="${left * scale}" y="${top * scale}" width="${Math.max(0, (right - left) * scale)}" height="${Math.max(0, (bottom - top) * scale)}" fill="none" stroke="${sashOutside.cssColor}" stroke-opacity="${formatCoordinate(sashOutside.opacity)}" stroke-width="${Math.max(3, opening.sashFaceMm * scale)}" />`;
    const sashLineworkInnerWidth = Math.max(0, opening.widthMm - opening.sashFaceMm * 2);
    const sashLineworkInnerHeight = Math.max(0, opening.heightMm - opening.sashFaceMm * 2);
    const sashLinework = `<g class="design-window__opening-sash-linework" data-profile-face-mm="${formatCoordinate(opening.sashFaceMm)}" display="none" pointer-events="none"><rect x="${formatCoordinate(opening.xMm * scale)}" y="${formatCoordinate(opening.yMm * scale)}" width="${formatCoordinate(opening.widthMm * scale)}" height="${formatCoordinate(opening.heightMm * scale)}" /><rect x="${formatCoordinate((opening.xMm + opening.sashFaceMm) * scale)}" y="${formatCoordinate((opening.yMm + opening.sashFaceMm) * scale)}" width="${formatCoordinate(sashLineworkInnerWidth * scale)}" height="${formatCoordinate(sashLineworkInnerHeight * scale)}" /></g>`;
    if (
      showOpeningState &&
      (projection.progressPercent > 0.001 || motionMode === "tilt")
    ) {
      const typeClass = opening.type === "top_hung"
        ? "design-window__opening--top-hung"
        : "design-window__opening--tilt-turn";
      const movingOpening = [
        `<g class="design-window__opening ${typeClass}" data-object-id="${escapeXml(previewKey)}"${selectedAttribute(previewKey)} data-opening-object-id="${escapeXml(opening.objectId)}" data-window-id="${id}" data-source-component-id="${escapeXml(opening.sourceComponentId)}" data-opening="${opening.opening}" data-hinge-edge="${opening.hingeEdge}" data-panel-id="${opening.panelId}" data-panel-role="${opening.panelRole}" data-operation-order="${opening.operationOrder}"${motionMetadata}>`,
        renderOpeningFacadePerspective(
          opening,
          projection.progressPercent,
          motionMode,
          scale,
          previewKey,
          geometry.hardware,
          visualConfiguration,
          openingConnectionPresetByPanelKey?.[previewKey]
        ),
        "</g>"
      ].join("");
      // Opening-direction symbols describe the closed elevation and would be
      // misleading if left behind on the stationary aperture. They are hidden
      // while the actual sash geometry moves. Both opening planes use the same
      // orthographic facade layer; physical occlusion belongs to Three.js.
      foregroundOpenings.push(movingOpening);
      return "";
    }
    if (opening.type === "top_hung") {
      return [
        `<g class="design-window__opening design-window__opening--top-hung" data-object-id="${escapeXml(previewKey)}"${selectedAttribute(previewKey)} data-opening-object-id="${escapeXml(opening.objectId)}" data-window-id="${id}" data-source-component-id="${escapeXml(opening.sourceComponentId)}" data-opening="${opening.opening}" data-hinge-edge="top" data-panel-id="${opening.panelId}" data-panel-role="${opening.panelRole}" data-operation-order="${opening.operationOrder}"${motionMetadata}>`,
        sashOutline,
        sashLinework,
        `<path class="design-window__opening-symbol-halo" d="M ${left * scale} ${bottom * scale} L ${(left + right) / 2 * scale} ${top * scale} L ${right * scale} ${bottom * scale}" fill="none" stroke="#ffffff" stroke-opacity="0.88" stroke-width="4"${openingSymbolDash} />`,
        `<path class="${openingSymbolClass}" data-open-plane="${openingPlane}" d="M ${left * scale} ${bottom * scale} L ${(left + right) / 2 * scale} ${top * scale} L ${right * scale} ${bottom * scale}" fill="none" stroke="#0755b5" stroke-width="2"${openingSymbolDash} role="img" aria-label="${openingSymbolLabel}" />`,
        renderFacadeOpeningPlaneArrow(opening, scale),
        renderFacadeOpeningAngleValue(opening, maximumAngleDegrees, scale),
        "</g>"
      ].join("");
    }
    const hingeX = opening.hingeEdge === "left" ? left : right;
    const handleX = opening.hingeEdge === "left" ? right : left;
    /**
     * Panel role stays available as stable SVG metadata for selection,
     * opening order and manufacturing mapping, but is deliberately not
     * rendered as facade text. The drawing already expresses hinge side,
     * opening plane and movement with symbols; repeating "primary/secondary"
     * or "left/right independent leaf" inside the glass obscures dimensions
     * and incorrectly turns an internal operation role into a product label.
     *
     * Example: a flying-mullion pair still serializes
     * `data-panel-role="primary"` / `secondary`, while its visible facade only
     * shows the two opening symbols and configured maximum angles.
     *
     * @since 0.10.75
     * @change 0.10.75 Removed visible panel-role wording without changing IDs,
     * opening sequence, hardware ownership or BOM semantics.
     */
    return [
      `<g class="design-window__opening design-window__opening--tilt-turn" data-object-id="${escapeXml(previewKey)}"${selectedAttribute(previewKey)} data-opening-object-id="${escapeXml(opening.objectId)}" data-window-id="${id}" data-source-component-id="${escapeXml(opening.sourceComponentId)}" data-opening="${opening.opening}" data-hinge-edge="${opening.hingeEdge}" data-panel-id="${opening.panelId}" data-panel-role="${opening.panelRole}" data-operation-order="${opening.operationOrder}"${motionMetadata}>`,
      sashOutline,
      sashLinework,
      `<path class="design-window__opening-symbol-halo" d="M ${handleX * scale} ${top * scale} L ${hingeX * scale} ${middleY * scale} L ${handleX * scale} ${bottom * scale} M ${left * scale} ${bottom * scale} L ${(left + right) / 2 * scale} ${top * scale} L ${right * scale} ${bottom * scale}" fill="none" stroke="#ffffff" stroke-opacity="0.88" stroke-width="4"${openingSymbolDash} />`,
      `<path class="${openingSymbolClass}" data-open-plane="${openingPlane}" d="M ${handleX * scale} ${top * scale} L ${hingeX * scale} ${middleY * scale} L ${handleX * scale} ${bottom * scale} M ${left * scale} ${bottom * scale} L ${(left + right) / 2 * scale} ${top * scale} L ${right * scale} ${bottom * scale}" fill="none" stroke="#0755b5" stroke-width="2"${openingSymbolDash} role="img" aria-label="${openingSymbolLabel}" />`,
      renderFacadeOpeningPlaneArrow(opening, scale),
      renderFacadeOpeningAngleValue(opening, maximumAngleDegrees, scale),
      "</g>"
    ].join("");
  });
  const meetingMullions = geometry.meetingMullions.map((mullion) => {
    const ownerRuntime = mullion.ownerPanelId
      ? resolvePanelRuntime(mullion.sourceObjectId, mullion.ownerPanelId)
      : undefined;
    if (
      showOpeningState &&
      mullion.kind === "flying-mullion" &&
      ownerRuntime &&
      ownerRuntime.progressPercent > 0.001
    ) return "";
    const owner = mullion.ownerPanelId
      ? ` data-owner-panel-id="${mullion.ownerPanelId}"`
      : "";
    const style = mullion.kind === "fixed-mullion"
      ? mullionOutside
      : flyingMullionOutside;
    const renderedWidth = Math.max(3, mullion.widthMm * scale);
    const renderedX = mullion.xMm * scale;
    const renderedY = mullion.yMm * scale;
    return [
      `<rect class="design-window__meeting-mullion design-window__meeting-mullion--${mullion.kind === "fixed-mullion" ? "fixed" : "flying"}" data-object-id="${escapeXml(mullion.objectId)}"${selectedAttribute(mullion.objectId)} data-drag-owner-id="${escapeXml(mullion.objectId)}" data-window-id="${id}" data-cell-id="${escapeXml(mullion.sourceObjectId)}" data-member-kind="meeting-mullion" data-meeting-kind="${mullion.kind}" data-orientation="vertical" data-source-component-id="${escapeXml(mullion.sourceComponentId)}"${owner} data-position-ratio="${mullion.positionRatio}" x="${renderedX}" y="${renderedY}" width="${renderedWidth}" height="${mullion.heightMm * scale}" fill="${style.cssColor}" fill-opacity="${formatCoordinate(style.opacity)}" stroke="#0f172a" stroke-width="1.5" />`
    ].join("");
  });
  const frames = geometry.frames.map(
    (frame) =>
      `<rect class="design-window__frame-segment design-window__frame-segment--${frame.side}" data-object-id="${escapeXml(frame.objectId)}"${selectedAttribute(frame.objectId)} data-source-component-id="${escapeXml(frame.sourceComponentId)}" x="${frame.xMm * scale}" y="${frame.yMm * scale}" width="${frame.widthMm * scale}" height="${frame.heightMm * scale}" fill="${frameOutside.cssColor}" fill-opacity="${formatCoordinate(frameOutside.opacity)}" stroke="#64748b" />`
  );
  /**
   * Serializes shared closed-state hardware after frame/member shapes so small
   * hinge, lock and keeper parts remain visible. Stable cell IDs drive ordinary
   * selection; derived hardware, mate IDs, physical workpieces and moving-panel
   * owners remain available for inspection and 3D/process parity checks.
   * Secondary-leaf shoot bolts are vertical moving members; their top/bottom
   * keepers are horizontal stationary members on the outer frame.
   * @since 0.5.0
   * @modified 2026-09-17 - Added 0.5.5 secondary-leaf shoot-bolt roles.
   */
  const hardware = geometry.hardware.map((mount) => {
    const ownerRuntime = resolvePanelRuntime(
      mount.sourceObjectId,
      mount.mountOwnerPanelId ?? mount.panelId
    );
    if (
      showOpeningState &&
      ownerRuntime &&
      ownerRuntime.progressPercent > 0.001 &&
      (mount.mountTarget === "sash" || mount.mountTarget === "flying-mullion")
    ) return "";
    if (
      showOpeningState &&
      ownerRuntime?.motionMode === "tilt" &&
      (mount.role === "hinge-sash-leaf" || mount.role === "hinge-frame-leaf")
    ) return "";
    const selected = selectedAttribute(mount.hardwareId);
    const connection = mount.connectionId
      ? ` data-connection-id="${escapeXml(mount.connectionId)}"`
      : "";
    const mate = mount.matingHardwareId
      ? ` data-mating-hardware-id="${escapeXml(mount.matingHardwareId)}"`
      : "";
    const mountOwner = mount.mountOwnerPanelId
      ? ` data-mount-owner-panel-id="${mount.mountOwnerPanelId}"`
      : "";
    const model = resolveHardwareComponentModel(
      visualConfiguration,
      mount.role,
      mount.hardwareId
    );
    if (!model) throw new Error(`No visual hardware model resolves for ${mount.hardwareId}.`);
    const style = resolveAppearanceRenderStyle(model.appearance);
    const geometrySource = model.geometry;
    const horizontalMount = mount.edge === "top" || mount.edge === "bottom";
    const modelWidthMm = horizontalMount
      ? model.dimensionsMm.heightMm
      : model.dimensionsMm.widthMm;
    const modelHeightMm = horizontalMount
      ? model.dimensionsMm.widthMm
      : model.dimensionsMm.heightMm;
    const centerXMm = mount.xMm + mount.widthMm / 2;
    const centerYMm = mount.yMm + mount.heightMm / 2;
    const width = Math.max(3, modelWidthMm * scale);
    const height = Math.max(3, modelHeightMm * scale);
    const x = centerXMm * scale - width / 2;
    const y = centerYMm * scale - height / 2;
    const asset = geometrySource.kind === "gltf"
      ? ` data-asset-id="${escapeXml(geometrySource.assetId)}" data-asset-hash="${escapeXml(geometrySource.contentHash)}"`
      : ` data-primitive-id="${geometrySource.primitiveId}"`;
    const common = `class="design-window__hardware design-window__hardware--${mount.role}" data-object-id="${escapeXml(mount.hardwareId)}"${selected} data-opening-object-id="${escapeXml(mount.sourceObjectId)}" data-hardware-id="${escapeXml(mount.hardwareId)}" data-source-component-id="${escapeXml(mount.sourceComponentId)}" data-model-id="${escapeXml(model.modelId)}" data-model-version="${escapeXml(model.modelVersion)}" data-geometry-kind="${geometrySource.kind}"${asset} data-mount-target="${mount.mountTarget}"${mountOwner} data-edge="${mount.edge}"${connection}${mate}`;
    if (mount.role === "handle" || mount.role === "secondary-lever") {
      const centerX = x + width / 2;
      const centerY = y + height / 2;
      const leverLengthMm = geometrySource.kind === "parametric"
        ? geometrySource.parameters.leverLengthMm ?? 45
        : 45;
      const leverLength = Math.max(9, leverLengthMm * scale);
      const plateWidth = geometrySource.kind === "parametric"
        ? Math.max(3, Math.min(width, (geometrySource.parameters.plateWidthMm ?? modelWidthMm) * scale))
        : width;
      const plateHeight = geometrySource.kind === "parametric"
        ? Math.max(3, Math.min(height, (geometrySource.parameters.plateHeightMm ?? modelHeightMm) * scale))
        : height;
      const plateX = centerX - plateWidth / 2;
      const plateY = centerY - plateHeight / 2;
      const direction = mount.edge === "left" ? 1 : -1;
      const label = mount.role === "secondary-lever" ? "从动扇操作杆" : "执手";
      const lever = mount.edge === "bottom" || mount.edge === "top"
        ? `<line x1="${centerX}" y1="${centerY}" x2="${centerX}" y2="${centerY - leverLength}" stroke="#78350f" stroke-width="3" stroke-linecap="round" />`
        : `<line x1="${centerX}" y1="${centerY}" x2="${centerX + direction * leverLength}" y2="${centerY}" stroke="#78350f" stroke-width="3" stroke-linecap="round" />`;
      const plate = geometrySource.kind === "parametric" && geometrySource.primitiveId === "round-knob"
        ? `<ellipse cx="${centerX}" cy="${centerY}" rx="${width / 2}" ry="${height / 2}" fill="${style.cssColor}" fill-opacity="${formatCoordinate(style.opacity)}" stroke="#1e293b" />`
        : `<rect x="${plateX}" y="${plateY}" width="${plateWidth}" height="${plateHeight}" rx="${Math.min(4, plateWidth / 2)}" fill="${style.cssColor}" fill-opacity="${formatCoordinate(style.opacity)}" stroke="#1e293b" />`;
      return `<g ${common} role="img" aria-label="${label}">${plate}${geometrySource.kind === "parametric" && geometrySource.primitiveId === "round-knob" ? "" : lever}</g>`;
    }
    if (geometrySource.kind === "parametric" && geometrySource.primitiveId === "lock-point") {
      return `<ellipse ${common} role="img" aria-label="${mount.role}" cx="${x + width / 2}" cy="${y + height / 2}" rx="${width / 2}" ry="${height / 2}" fill="${style.cssColor}" fill-opacity="${formatCoordinate(style.opacity)}" stroke="#1e293b" stroke-width="1" />`;
    }
    return `<rect ${common} role="img" aria-label="${mount.role}" x="${x}" y="${y}" width="${width}" height="${height}" rx="2" fill="${style.cssColor}" fill-opacity="${formatCoordinate(style.opacity)}" stroke="#1e293b" stroke-width="1" />`;
  });
  const members = geometry.members.map((member) => {
    const invalid = member.partitionValid ? "" : " design-window__member--invalid";
    const dash = member.partitionValid ? "" : ' stroke-dasharray="7 5"';
    const dividerMatch = /^divider\.(v|h)\.(\d+)$/.exec(member.sourceComponentId);
    const dividerMetadata = member.kind === "grid-divider" && dividerMatch
      ? ` data-divider-axis="${dividerMatch[1] === "v" ? "column" : "row"}" data-divider-index="${dividerMatch[2]}"`
      : "";
    const renderedWidth = Math.max(2, member.widthMm * scale);
    const renderedX = member.xMm * scale;
    const renderedY = member.yMm * scale;
    return [
      `<rect class="design-window__member design-window__member--${member.kind}${invalid}" data-object-id="${escapeXml(member.objectId)}"${selectedAttribute(member.objectId)} data-drag-owner-id="${escapeXml(member.objectId)}" data-window-id="${id}" data-member-kind="${member.kind}" data-source-component-id="${escapeXml(member.sourceComponentId)}" data-orientation="${member.orientation}" data-partition-valid="${member.partitionValid}"${dividerMetadata} x="${renderedX}" y="${renderedY}" width="${renderedWidth}" height="${Math.max(2, member.heightMm * scale)}" fill="${member.partitionValid ? mullionOutside.cssColor : "#fee2e2"}" fill-opacity="${member.partitionValid ? formatCoordinate(mullionOutside.opacity) : "1"}" stroke="${member.partitionValid ? "#475569" : "#dc2626"}"${dash} />`
    ].join("");
  });
  const dimensions: string[] = [];
  if (showDimensions) {
    // Each top/right lane owns a 66px block because one window can render both
    // near (cell/inner) and far (outer/meeting) measurements. Bottom lanes need
    // only 34px. Fixed screen-space spacing keeps text readable at every zoom.
    const dimensionTopNear = -(facadeInsets.top + dimensionEnvelopeInsetsMm.top) * scale -
      28 - dimensionLanes.top * 66;
    const dimensionTopFar = -(facadeInsets.top + dimensionEnvelopeInsetsMm.top) * scale -
      58 - dimensionLanes.top * 66;
    const dimensionRightNear = width +
      (facadeInsets.right + dimensionEnvelopeInsetsMm.right) * scale + 28 +
      dimensionLanes.right * 66;
    const dimensionRightFar = width +
      (facadeInsets.right + dimensionEnvelopeInsetsMm.right) * scale + 58 +
      dimensionLanes.right * 66;
    const dimensionBottom = height +
      (facadeInsets.bottom + dimensionEnvelopeInsetsMm.bottom) * scale + 30 +
      dimensionLanes.bottom * 34;
    const columnCells = geometry.cells.filter((cell) => cell.row === 0);
    const rowCells = geometry.cells.filter((cell) => cell.column === 0);
    const hasVerticalGridDivision = window.layout.columns.length > 1;
    const hasHorizontalGridDivision = window.layout.rows.length > 1;
    for (const cell of columnCells) {
      // Inter-mullion clear widths belong inside the bay they measure. This
      // keeps the internal manufacturing value visually attached to its two
      // bounding mullions while outer frame/assembly sizes stay outside.
      const interiorWidthDimensionY = cell.yMm * scale + Math.min(
        20,
        Math.max(12, cell.heightMm * scale * 0.08)
      );
      const cellWidthDimensionY = hasVerticalGridDivision
        ? interiorWidthDimensionY
        : dimensionTopNear;
      const meeting = geometry.meetingMullions.find(
        (candidate) => candidate.sourceObjectId === cell.objectId
      );
      if (!meeting) {
        dimensions.push(
          renderDimensionLine({
            x1: cell.xMm * scale,
            y1: cellWidthDimensionY,
            x2: (cell.xMm + cell.widthMm) * scale,
            y2: cellWidthDimensionY,
            label: `${Math.round(cell.widthMm)} mm`,
            windowId: window.objectId,
            objectId: cell.objectId,
            groupId: `${cell.objectId}:width`,
            cellId: cell.objectId,
            kind: "cell-width",
            axis: "column",
            index: window.layout.columns.length === 1
              ? 0
              : cell.column < window.layout.columns.length - 1
                ? cell.column + 1
                : cell.column,
            edge: cell.column < window.layout.columns.length - 1 ? "end" : "start",
            selected: selectedObjectId === cell.objectId,
            placement: hasVerticalGridDivision ? "cell-interior" : "outside"
          })
        );
        continue;
      }
      const meetingCenterMm = meeting.xMm + meeting.widthMm / 2;
      const leftInnerEdgeMm = meeting.kind === "fixed-mullion"
        ? meeting.xMm
        : meetingCenterMm;
      const rightInnerEdgeMm = meeting.kind === "fixed-mullion"
        ? meeting.xMm + meeting.widthMm
        : meetingCenterMm;
      dimensions.push(
        renderDimensionLine({
          x1: cell.xMm * scale,
          y1: interiorWidthDimensionY,
          x2: leftInnerEdgeMm * scale,
          y2: interiorWidthDimensionY,
           label: `内 ${Math.round(meeting.leftInnerWidthMm)} mm`,
           windowId: window.objectId,
           objectId: meeting.objectId,
           groupId: meeting.objectId,
           cellId: cell.objectId,
           kind: "meeting-mullion",
           measurementRole: "left-inner",
           selected: selectedObjectId === meeting.objectId,
           placement: "cell-interior"
        }),
        renderDimensionLine({
          x1: rightInnerEdgeMm * scale,
          y1: interiorWidthDimensionY,
          x2: (cell.xMm + cell.widthMm) * scale,
          y2: interiorWidthDimensionY,
          label: `内 ${Math.round(meeting.rightInnerWidthMm)} mm`,
          windowId: window.objectId,
          objectId: meeting.objectId,
          groupId: meeting.objectId,
          cellId: cell.objectId,
          kind: "meeting-mullion",
          measurementRole: "right-inner",
          selected: selectedObjectId === meeting.objectId,
          placement: "cell-interior"
        }),
        renderDimensionLine({
          x1: 0,
          y1: dimensionTopFar,
          x2: meetingCenterMm * scale,
          y2: dimensionTopFar,
          label: `外 ${Math.round(meetingCenterMm)} mm`,
          windowId: window.objectId,
          objectId: meeting.objectId,
          groupId: meeting.objectId,
          cellId: cell.objectId,
          kind: "meeting-mullion",
          measurementRole: "left-outer",
          selected: selectedObjectId === meeting.objectId
        }),
        renderDimensionLine({
          x1: meetingCenterMm * scale,
          y1: dimensionTopFar,
          x2: width,
          y2: dimensionTopFar,
          label: `外 ${Math.round(window.widthMm - meetingCenterMm)} mm`,
          windowId: window.objectId,
          objectId: meeting.objectId,
          groupId: meeting.objectId,
          cellId: cell.objectId,
          kind: "meeting-mullion",
          measurementRole: "right-outer",
          selected: selectedObjectId === meeting.objectId
        })
      );
    }
    if (presentation.dimensionVisibility?.cellHeights !== false) {
      for (const cell of rowCells) {
        const interiorHeightDimensionX = cell.xMm * scale + Math.min(
          38,
          Math.max(24, cell.widthMm * scale * 0.1)
        );
        const cellHeightDimensionX = hasHorizontalGridDivision
          ? interiorHeightDimensionX
          : dimensionRightNear;
        dimensions.push(
          renderDimensionLine({
            x1: cellHeightDimensionX,
            y1: cell.yMm * scale,
            x2: cellHeightDimensionX,
            y2: (cell.yMm + cell.heightMm) * scale,
            label: `${Math.round(cell.heightMm)} mm`,
            vertical: true,
            windowId: window.objectId,
            objectId: cell.objectId,
            groupId: `${cell.objectId}:height`,
            cellId: cell.objectId,
            kind: "cell-height",
            axis: "row",
            index: window.layout.rows.length === 1
              ? 0
              : cell.row < window.layout.rows.length - 1
                ? cell.row + 1
                : cell.row,
            edge: cell.row < window.layout.rows.length - 1 ? "end" : "start",
            selected: selectedObjectId === cell.objectId,
            placement: hasHorizontalGridDivision ? "cell-interior" : "outside"
          })
        );
      }
    }
    /**
     * Local topology members do not alter the legacy rule-grid row/column
     * arrays. Their clear spans therefore have to come from the topology
     * partition itself rather than from `geometry.cells`. For every valid
     * rectangular partition, member-centre boundaries are moved to the
     * corresponding profile face so the displayed value is the actual clear
     * opening between profiles, not a centre-line distance.
     *
     * Widths are shown only when vertical topology members exist; heights are
     * shown only when horizontal members exist. This prevents a horizontal
     * transom from redundantly repeating the host width while still showing
     * the upper and lower clear heights inside their respective panes.
     *
     * @example One full-width horizontal member creates two in-pane height
     * dimensions, each excluding half of the member face at its boundary.
     * @since 0.10.66
     */
    for (const host of geometry.cells) {
      const topologyMembers = window.topology.members.filter(
        (member) => member.hostRegionId === host.objectId
      );
      if (topologyMembers.length === 0) continue;
      const partition = partitionTopologyRegion(topologyMembers);
      if (!partition.valid) continue;
      const resolvedMemberById = new Map(
        geometry.members
          .filter((member) => member.kind === "topology-member")
          .map((member) => [member.objectId, member] as const)
      );
      const verticalMembers = topologyMembers.filter(
        (member) => member.orientation === "vertical"
      );
      const horizontalMembers = topologyMembers.filter(
        (member) => member.orientation === "horizontal"
      );
      const epsilon = 0.00001;
      for (const region of partition.regions) {
        const regionMiddleX = (region.xStart + region.xEnd) / 2;
        const regionMiddleY = (region.yStart + region.yEnd) / 2;
        const leftBoundary = verticalMembers.find(
          (member) =>
            Math.abs(member.positionRatio - region.xStart) < epsilon &&
            regionMiddleY >= member.span.startRatio - epsilon &&
            regionMiddleY <= member.span.endRatio + epsilon
        );
        const rightBoundary = verticalMembers.find(
          (member) =>
            Math.abs(member.positionRatio - region.xEnd) < epsilon &&
            regionMiddleY >= member.span.startRatio - epsilon &&
            regionMiddleY <= member.span.endRatio + epsilon
        );
        const topBoundary = horizontalMembers.find(
          (member) =>
            Math.abs(member.positionRatio - region.yStart) < epsilon &&
            regionMiddleX >= member.span.startRatio - epsilon &&
            regionMiddleX <= member.span.endRatio + epsilon
        );
        const bottomBoundary = horizontalMembers.find(
          (member) =>
            Math.abs(member.positionRatio - region.yEnd) < epsilon &&
            regionMiddleX >= member.span.startRatio - epsilon &&
            regionMiddleX <= member.span.endRatio + epsilon
        );
        const leftMm = leftBoundary
          ? (resolvedMemberById.get(leftBoundary.objectId)?.xMm ??
            host.xMm + host.widthMm * region.xStart) +
            (resolvedMemberById.get(leftBoundary.objectId)?.widthMm ?? 0)
          : host.xMm + host.widthMm * region.xStart;
        const rightMm = rightBoundary
          ? resolvedMemberById.get(rightBoundary.objectId)?.xMm ??
            host.xMm + host.widthMm * region.xEnd
          : host.xMm + host.widthMm * region.xEnd;
        const topMm = topBoundary
          ? (resolvedMemberById.get(topBoundary.objectId)?.yMm ??
            host.yMm + host.heightMm * region.yStart) +
            (resolvedMemberById.get(topBoundary.objectId)?.heightMm ?? 0)
          : host.yMm + host.heightMm * region.yStart;
        const bottomMm = bottomBoundary
          ? resolvedMemberById.get(bottomBoundary.objectId)?.yMm ??
            host.yMm + host.heightMm * region.yEnd
          : host.yMm + host.heightMm * region.yEnd;
        if (verticalMembers.length > 0 && rightMm - leftMm > 0.5) {
          // Prefer the region's trailing boundary. Editing a clear span then
          // moves the member at the end of that span; the final region falls
          // back to its leading boundary and moves it from the opposite side.
          const owner = rightBoundary ?? leftBoundary ?? verticalMembers[0];
          const clearSpanSide = rightBoundary ? "before" as const : "after" as const;
          dimensions.push(renderDimensionLine({
            x1: leftMm * scale,
            y1: ((topMm + bottomMm) / 2) * scale,
            x2: rightMm * scale,
            y2: ((topMm + bottomMm) / 2) * scale,
            label: `${Math.round(rightMm - leftMm)} mm`,
            windowId: window.objectId,
            objectId: owner?.objectId,
            groupId: `${owner?.objectId ?? host.objectId}:clear-width`,
            cellId: host.objectId,
            kind: "topology-clear-span",
            memberId: owner?.objectId,
            clearSpanSide,
            placement: "member-interior",
            sourceObjectId: owner?.objectId,
            selected: owner?.objectId === selectedObjectId
          }));
        }
        if (horizontalMembers.length > 0 && bottomMm - topMm > 0.5) {
          const owner = bottomBoundary ?? topBoundary ?? horizontalMembers[0];
          const clearSpanSide = bottomBoundary ? "before" as const : "after" as const;
          dimensions.push(renderDimensionLine({
            x1: ((leftMm + rightMm) / 2) * scale,
            y1: topMm * scale,
            x2: ((leftMm + rightMm) / 2) * scale,
            y2: bottomMm * scale,
            label: `${Math.round(bottomMm - topMm)} mm`,
            vertical: true,
            windowId: window.objectId,
            objectId: owner?.objectId,
            groupId: `${owner?.objectId ?? host.objectId}:clear-height`,
            cellId: host.objectId,
            kind: "topology-clear-span",
            memberId: owner?.objectId,
            clearSpanSide,
            placement: "member-interior",
            sourceObjectId: owner?.objectId,
            selected: owner?.objectId === selectedObjectId
          }));
        }
      }
    }
    dimensions.push(renderDimensionLine({
        x1: 0,
        y1: dimensionBottom,
        x2: width,
        y2: dimensionBottom,
        label: `${Math.round(window.widthMm)} mm`,
        windowId: window.objectId,
        objectId: window.objectId,
        groupId: `${window.objectId}:outer-size`,
        kind: "width",
        selected: selectedObjectId === window.objectId
      }));
    if (presentation.dimensionVisibility?.outerHeight !== false) {
      dimensions.push(renderDimensionLine({
        x1: dimensionRightFar,
        y1: 0,
        x2: dimensionRightFar,
        y2: height,
        label: `${Math.round(window.heightMm)} mm`,
        vertical: true,
        windowId: window.objectId,
        objectId: window.objectId,
        groupId: `${window.objectId}:outer-size`,
        kind: "height",
        selected: selectedObjectId === window.objectId
      }));
    }
    const hasFacadeSurround = facadeInsets.top > 0 || facadeInsets.right > 0 ||
      facadeInsets.bottom > 0 || facadeInsets.left > 0;
    if (hasFacadeSurround) {
      const outerLeft = -facadeInsets.left * scale;
      const outerRight = width + facadeInsets.right * scale;
      const outerTop = -facadeInsets.top * scale;
      const outerBottom = height + facadeInsets.bottom * scale;
      const outerWidthMm = window.widthMm + facadeInsets.left + facadeInsets.right;
      const outerHeightMm = window.heightMm + facadeInsets.top + facadeInsets.bottom;
      const surroundObjectId = createWindowInstallationSurroundObjectId(window.objectId);
      if (facadeInsets.left > 0 || facadeInsets.right > 0) {
        dimensions.push(renderDimensionLine({
          x1: outerLeft,
          y1: dimensionBottom + 14,
          x2: outerRight,
          y2: dimensionBottom + 14,
          label: `包边外宽 ${Math.round(outerWidthMm)} mm`,
          windowId: window.objectId,
          objectId: surroundObjectId,
          groupId: `${window.objectId}:surround-size`,
          kind: "surround-outer-width",
          selected: selectedObjectId === surroundObjectId ||
            selectedObjectId?.startsWith(`${surroundObjectId}.`) === true
        }));
      }
      if (presentation.dimensionVisibility?.outerHeight !== false) {
        dimensions.push(renderDimensionLine({
          x1: dimensionRightFar + 14,
          y1: outerTop,
          x2: dimensionRightFar + 14,
          y2: outerBottom,
          label: `包边外高 ${Math.round(outerHeightMm)} mm`,
          vertical: true
        }));
      }
      const firstHorizontalInset = facadeInsets.left > 0
        ? { start: outerLeft, end: 0 }
        : { start: width, end: outerRight };
      dimensions.push(renderDimensionLine({
        x1: firstHorizontalInset.start,
        y1: dimensionTopFar,
        x2: firstHorizontalInset.end,
        y2: dimensionTopFar,
        label: `外包边宽 ${Math.round(installation.surround.outsideWidthMm)} mm`,
        windowId: window.objectId,
        objectId: surroundObjectId,
        groupId: `${window.objectId}:surround-size`,
        kind: "surround-outside-width",
        selected: selectedObjectId === surroundObjectId ||
          selectedObjectId?.startsWith(`${surroundObjectId}.`) === true
      }));
    }
  }
  /**
   * Profile dimensions are contextual, not another permanent dimension chain.
   * A selected sash panel uses its top rail as the facade measurement sample;
   * all four rails still belong to the same panel until the manufacturing model
   * introduces separately identified cut pieces. Existing frame/member/mullion
   * IDs remain exact and receive their own role and section depth.
   * @since 0.10.74
   */
  const profileCalloutY = -(
    facadeInsets.top + dimensionEnvelopeInsetsMm.top
  ) * scale - 92 - dimensionLanes.top * 66;
  const selectedOpening = selectedObjectId
    ? geometry.openings.find(
        (opening) => createOpeningPanelKey(opening.objectId, opening.panelId) === selectedObjectId
      )
    : undefined;
  const selectedSlidingPanel = selectedObjectId
    ? geometry.slidingPanels.find(
        (panel) => createOpeningPanelKey(panel.sourceObjectId, panel.panelId) === selectedObjectId
      )
    : undefined;
  const selectedFrame = selectedObjectId
    ? geometry.frames.find((frame) => frame.objectId === selectedObjectId)
    : undefined;
  const selectedMeetingMullion = selectedObjectId
    ? geometry.meetingMullions.find((mullion) => mullion.objectId === selectedObjectId)
    : undefined;
  const selectedMember = selectedObjectId
    ? geometry.members.find((member) => member.objectId === selectedObjectId)
    : undefined;
  const profileAnnotation = !showDimensions || !selectedObjectId
    ? ""
    : selectedOpening
      ? renderSelectedProfileAnnotation({
          objectId: selectedObjectId,
          roleKind: "sash",
          roleLabel: "扇框型材",
          presetId: section.presetId,
          faceWidthMm: selectedOpening.sashFaceMm,
          depthMm: section.sashDepthMm,
          orientation: "horizontal",
          xMm: selectedOpening.xMm,
          yMm: selectedOpening.yMm,
          widthMm: selectedOpening.widthMm,
          heightMm: selectedOpening.sashFaceMm,
          scale,
          calloutY: profileCalloutY,
          windowWidth: width
        })
      : selectedSlidingPanel
        ? renderSelectedProfileAnnotation({
            objectId: selectedObjectId,
            roleKind: "sliding-sash",
            roleLabel: "推拉扇框型材",
            presetId: section.presetId,
            faceWidthMm: window.sashFaceMm,
            depthMm: section.sashDepthMm,
            orientation: "horizontal",
            xMm: selectedSlidingPanel.xMm,
            yMm: selectedSlidingPanel.yMm,
            widthMm: selectedSlidingPanel.widthMm,
            heightMm: window.sashFaceMm,
            scale,
            calloutY: profileCalloutY,
            windowWidth: width
          })
      : selectedFrame
        ? renderSelectedProfileAnnotation({
            objectId: selectedObjectId,
            roleKind: "frame",
            roleLabel: `外框${selectedFrame.side === "top" || selectedFrame.side === "bottom" ? "横料" : "竖料"}`,
            presetId: section.presetId,
            faceWidthMm: window.frameFaceMm,
            depthMm: section.frameDepthMm,
            orientation: selectedFrame.side === "top" || selectedFrame.side === "bottom"
              ? "horizontal"
              : "vertical",
            xMm: selectedFrame.xMm,
            yMm: selectedFrame.yMm,
            widthMm: selectedFrame.widthMm,
            heightMm: selectedFrame.heightMm,
            scale,
            calloutY: profileCalloutY,
            windowWidth: width
          })
        : selectedMeetingMullion
          ? renderSelectedProfileAnnotation({
              objectId: selectedObjectId,
              roleKind: selectedMeetingMullion.kind,
              roleLabel: selectedMeetingMullion.kind === "flying-mullion"
                ? "飞梃型材"
                : "固定中梃型材",
              presetId: section.presetId,
              faceWidthMm: selectedMeetingMullion.widthMm,
              depthMm: selectedMeetingMullion.kind === "flying-mullion"
                ? section.flyingMullionDepthMm
                : section.frameDepthMm,
              orientation: "vertical",
              xMm: selectedMeetingMullion.xMm,
              yMm: selectedMeetingMullion.yMm,
              widthMm: selectedMeetingMullion.widthMm,
              heightMm: selectedMeetingMullion.heightMm,
              scale,
              calloutY: profileCalloutY,
              windowWidth: width
            })
          : selectedMember
            ? renderSelectedProfileAnnotation({
                objectId: selectedObjectId,
                roleKind: selectedMember.kind === "grid-divider"
                  ? "grid-mullion"
                  : "topology-mullion",
                roleLabel: `${selectedMember.kind === "grid-divider" ? "贯通" : "局部"}${selectedMember.orientation === "horizontal" ? "横梃" : "竖梃"}型材`,
                presetId: section.presetId,
                faceWidthMm: selectedMember.orientation === "horizontal"
                  ? selectedMember.heightMm
                  : selectedMember.widthMm,
                depthMm: section.frameDepthMm,
                orientation: selectedMember.orientation,
                xMm: selectedMember.xMm,
                yMm: selectedMember.yMm,
                widthMm: selectedMember.widthMm,
                heightMm: selectedMember.heightMm,
                scale,
                calloutY: profileCalloutY,
                windowWidth: width
              })
            : "";
  const facadeBottom = height + facadeInsets.bottom * scale;
  const planY = facadeBottom + (showDimensions ? 82 : 44);
  const planEnvelope = resolveWindowPlanEnvelopeMm(window);
  const planPadding = 16;
  const planFrameDepthMm = section.frameDepthMm;
  const planFrameDepth = planFrameDepthMm * scale;
  // Screen Y grows downward while physical +Z is the exterior direction.
  // Inverting Z here fixes the engineering convention: exterior above, room below.
  const planBaselineY = planPadding + planEnvelope.maxZMm * scale;
  const planHeight = planEnvelope.spanMm * scale + planPadding * 2;
  const planFrameTopY = planBaselineY - planFrameDepth / 2;
  const planFrameBottomY = planBaselineY + planFrameDepth / 2;
  const planWallTopY = planBaselineY - installationSection.wallOutsideZMm * scale;
  const planWallBottomY = planBaselineY - installationSection.wallInsideZMm * scale;
  const planWallHeight = planWallBottomY - planWallTopY;
  const planWallCenterY = planBaselineY - installationSection.wallCenterZMm * scale;
  const planMotion = showOpeningState
    ? geometry.openings.map((opening) => {
        const previewKey = createOpeningPanelKey(opening.objectId, opening.panelId);
        const motionMode = openingMotionModeByPanelKey?.[previewKey] ?? "primary";
        const projection = projectOpeningMotion(
          opening,
          openingProgressPercentByPanelKey?.[previewKey] ?? opening.openPercent,
          motionMode
        );
        const closedProjection = projectOpeningMotion(opening, 0, motionMode);
        const closedPoints = closedProjection.planHullMm.map((point) =>
          `${formatCoordinate(point.x * scale)},${formatCoordinate(planBaselineY - point.z * scale)}`
        ).join(" ");
        const points = projection.planHullMm.map((point) =>
          `${formatCoordinate(point.x * scale)},${formatCoordinate(planBaselineY - point.z * scale)}`
        ).join(" ");
        const centerX = projection.planHullMm.reduce((total, point) => total + point.x, 0) /
          Math.max(1, projection.planHullMm.length) * scale;
        const centerY = planBaselineY - projection.planHullMm.reduce((total, point) => total + point.z, 0) /
          Math.max(1, projection.planHullMm.length) * scale;
        const angleDegrees = projection.angleRadians * 180 / Math.PI;
        const closedFootprint = projection.progressPercent > 0.001
          ? `<polygon class="design-plan-view__opening-closed" data-preview-panel-key="${escapeXml(previewKey)}" points="${closedPoints}" fill="none" stroke="#64748b" stroke-width="1.25" stroke-dasharray="5 4" />`
          : "";
        const angleAnnotation = projection.planAngleAnnotation
          ? renderOpeningAngleAnnotationSvg(
              projection.planAngleAnnotation,
              scale,
              "design-plan-view__opening-angle-mark",
              (valueMm) => planBaselineY - valueMm * scale
            )
          : `<text class="design-plan-view__opening-angle" x="${formatCoordinate(centerX)}" y="${formatCoordinate(centerY - 7)}" text-anchor="middle" fill="#0f4c81" font-size="11">${formatCoordinate(angleDegrees)}°</text>`;
        return [
          `<g class="design-plan-view__opening" data-preview-panel-key="${escapeXml(previewKey)}" data-preview-progress="${projection.progressPercent}" data-motion-mode="${motionMode}" data-open-plane="${opening.opening.endsWith("_out") ? "out" : "in"}" data-opening-angle-deg="${formatCoordinate(angleDegrees)}">`,
          closedFootprint,
          `<polygon class="design-plan-view__opening-state" points="${points}" fill="#1677ff" fill-opacity="0.25" stroke="#1677ff" stroke-width="1.5" />`,
          angleAnnotation,
          "</g>"
        ].join("");
      }).join("")
    : "";
  const planSliding = geometry.slidingTracks.length === 0
    ? ""
    : [
        ...geometry.slidingTracks.map((track) => {
          const trackY = planBaselineY - track.centerOffsetZMm * scale;
          return `<line class="design-plan-view__sliding-track" data-object-id="${escapeXml(track.objectId)}"${selectedAttribute(track.objectId)} data-source-component-id="${escapeXml(track.sourceComponentId)}" data-track-index="${track.trackIndex}" x1="${formatCoordinate(track.startXMm * scale)}" y1="${formatCoordinate(trackY)}" x2="${formatCoordinate(track.endXMm * scale)}" y2="${formatCoordinate(trackY)}" stroke="#64748b" stroke-width="1" stroke-dasharray="4 3" />`;
        }),
        ...[...geometry.slidingPanels]
          .sort((leftPanel, rightPanel) => rightPanel.trackIndex - leftPanel.trackIndex)
          .map((panel) => {
            const previewKey = createOpeningPanelKey(panel.sourceObjectId, panel.panelId);
            const progressPercent = showOpeningState
              ? openingProgressPercentByPanelKey?.[previewKey] ?? panel.openPercent
              : 0;
            const offsetXMm = resolveSlidingPanelOffsetMm(panel, progressPercent);
            const track = geometry.slidingTracks.find(
              (candidate) => candidate.sourceObjectId === panel.sourceObjectId &&
                candidate.trackIndex === panel.trackIndex
            );
            if (!track) return "";
            const panelDepthMm = track.allocatedDepthMm;
            const panelY = planBaselineY -
              (panel.trackCenterOffsetZMm + panelDepthMm / 2) * scale;
            return `<rect class="design-plan-view__sliding-panel" data-object-id="${escapeXml(previewKey)}"${selectedAttribute(previewKey)} data-opening-object-id="${escapeXml(panel.sourceObjectId)}" data-source-component-id="${escapeXml(panel.sourceComponentId)}" data-track-index="${panel.trackIndex}" data-preview-progress="${formatCoordinate(progressPercent)}" data-preview-translation-x-mm="${formatCoordinate(offsetXMm)}" x="${formatCoordinate((panel.xMm + offsetXMm) * scale)}" y="${formatCoordinate(panelY)}" width="${formatCoordinate(panel.widthMm * scale)}" height="${formatCoordinate(panelDepthMm * scale)}" fill="${sashOutside.cssColor}" fill-opacity="0.32" stroke="${sashOutside.cssColor}" stroke-width="1.25" />`;
          })
      ].join("");
  /*
   * Installation mode/alignment remain structured model properties and are
   * editable in the inspector. They are intentionally not serialized as free
   * prose on the drawing: automatic phrases such as “洞口安装 · 框居中” collide
   * when several independent plan sections share one row. A future explicit
   * annotation object will own user-maintained drawing notes. The geometric
   * wall-thickness and frame-offset indicators below remain authoritative.
   *
   * @since 0.10.83
   * @modified 2026-09-22 - Removed generated installation prose from 2D plans.
   */
  const planInstallationDimensions = showDimensions
    ? `<g class="design-plan-view__installation-dimensions" pointer-events="none"><line class="design-plan-view__wall-centerline" x1="0" y1="${formatCoordinate(planWallCenterY)}" x2="${formatCoordinate(width)}" y2="${formatCoordinate(planWallCenterY)}" stroke="#9a6b3a" stroke-width="1" stroke-dasharray="5 4" /><line x1="${formatCoordinate(width + 18)}" y1="${formatCoordinate(planWallTopY)}" x2="${formatCoordinate(width + 18)}" y2="${formatCoordinate(planWallBottomY)}" stroke="#64748b" /><line x1="${formatCoordinate(width + 13)}" y1="${formatCoordinate(planWallTopY)}" x2="${formatCoordinate(width + 23)}" y2="${formatCoordinate(planWallTopY)}" stroke="#64748b" /><line x1="${formatCoordinate(width + 13)}" y1="${formatCoordinate(planWallBottomY)}" x2="${formatCoordinate(width + 23)}" y2="${formatCoordinate(planWallBottomY)}" stroke="#64748b" /><line class="design-plan-view__frame-offset" x1="${formatCoordinate(width / 2)}" y1="${formatCoordinate(planBaselineY)}" x2="${formatCoordinate(width / 2)}" y2="${formatCoordinate(planWallCenterY)}" stroke="#a16207" stroke-width="1.25" /></g>`
    : "";
  const planSurround = installationObstacles
    .filter((obstacle) =>
      obstacle.kind === "surround" &&
      obstacle.layer !== undefined &&
      (obstacle.side === "left" || obstacle.side === "right"))
    .map((obstacle) => {
      const layer = obstacle.layer!;
      const objectId = createWindowInstallationSurroundObjectId(
        window.objectId,
        layer,
        obstacle.side
      );
      const paint = layer === "outside"
        ? surroundOutsidePaint
        : layer === "inside"
          ? surroundInsidePaint
          : surroundLinerPaint;
      const style = layer === "outside"
        ? surroundOutsideStyle
        : layer === "inside"
          ? surroundInsideStyle
          : surroundLinerStyle;
      const obstacleX = (obstacle.min.x + window.widthMm / 2) * scale;
      const obstacleY = planBaselineY - obstacle.max.z * scale;
      return `<rect class="design-plan-view__surround design-plan-view__surround--${layer}" data-object-id="${escapeXml(objectId)}"${selectedAttribute(objectId)} data-source-component-id="${escapeXml(obstacle.sourceComponentId ?? obstacle.obstacleId)}" data-installation-side="${obstacle.side}" data-surround-layer="${layer}"${paint.metadata} x="${formatCoordinate(obstacleX)}" y="${formatCoordinate(obstacleY)}" width="${formatCoordinate((obstacle.max.x - obstacle.min.x) * scale)}" height="${formatCoordinate((obstacle.max.z - obstacle.min.z) * scale)}" fill="${paint.fill}" fill-opacity="${formatCoordinate(style.opacity)}" stroke="${style.cssColor}" />`;
    }).join("");
  const orientationColumnX = width + (showDimensions
    ? 92 + dimensionLanes.right * 66
    : 18);
  const planOrientation = presentation.showPlanOrientation === false
    ? ""
    : `<g class="design-plan-view__shared-orientation" data-placement="outside-right" data-aligned-with="facade-orientation" transform="translate(${formatCoordinate(orientationColumnX)} 8)" pointer-events="none"><rect width="68" height="42" rx="5" fill="#ffffff" fill-opacity="0.96" stroke="#cbd5e1" /><text class="design-plan-view__side-label design-plan-view__side-label--outside" x="34" y="16" text-anchor="middle" fill="#075985" font-size="11" font-weight="600">室外 ↑</text><line x1="9" y1="22" x2="59" y2="22" stroke="#e2e8f0" /><text class="design-plan-view__side-label design-plan-view__side-label--inside" x="34" y="35" text-anchor="middle" fill="#9a3412" font-size="11" font-weight="600">室内 ↓</text></g>`;
  const planLabel = presentation.showPlanLabel === false
    ? ""
    : `<text class="design-plan-view__shared-label" x="${width / 2}" y="${formatCoordinate(planHeight + 18)}" text-anchor="middle">俯视图</text>`;
  const plan = showPlanView
    ? `<g class="design-plan-view" data-section-preset-id="${escapeXml(section.presetId)}" data-plan-frame-depth-mm="${formatCoordinate(section.frameDepthMm)}" data-plan-sash-depth-mm="${formatCoordinate(section.sashDepthMm)}" data-plan-wall-thickness-mm="${formatCoordinate(installationSection.wallThicknessMm)}" data-plan-wall-center-z-mm="${formatCoordinate(installationSection.wallCenterZMm)}" data-plan-mounting-mode="${installation.surround.mountingMode}" data-plan-frame-alignment="${installation.surround.frameAlignment}" data-plan-mm-scale="${formatCoordinate(scale)}" data-plan-baseline-y="${formatCoordinate(planBaselineY)}" transform="translate(0 ${planY})" aria-label="${mark}俯视图">${planOrientation}<rect class="design-plan-view__wall" data-object-id="${escapeXml(createWindowInstallationWallObjectId(window.objectId))}"${selectedAttribute(createWindowInstallationWallObjectId(window.objectId))} data-plan-wall="true"${wallPaint.metadata} width="${width}" y="${formatCoordinate(planWallTopY)}" height="${formatCoordinate(planWallHeight)}" fill="${wallPaint.fill}" fill-opacity="${formatCoordinate(wallStyle.opacity)}" stroke="#a68a64" />${planSurround}<rect class="design-plan-view__frame" data-plan-frame="true" width="${width}" y="${formatCoordinate(planFrameTopY)}" height="${formatCoordinate(planFrameDepth)}" fill="${frameEdge.cssColor}" fill-opacity="${formatCoordinate(frameEdge.opacity)}" stroke="#64748b" /><rect class="design-plan-view__glass" x="${window.frameFaceMm * scale}" y="${formatCoordinate(planFrameTopY + 5)}" width="${Math.max(0, width - window.frameFaceMm * scale * 2)}" height="${Math.max(1, planFrameDepth - 10)}" fill="${glassStyle.cssColor}" fill-opacity="${formatCoordinate(glassStyle.opacity)}" stroke="#7aa7bd" /><line class="design-plan-view__frame-reference" x1="${window.frameFaceMm * scale}" y1="${formatCoordinate(planBaselineY)}" x2="${Math.max(window.frameFaceMm * scale, width - window.frameFaceMm * scale)}" y2="${formatCoordinate(planBaselineY)}" stroke="#1677ff" />${planInstallationDimensions}${planSliding}${planMotion}${planLabel}</g>`
    : "";
  const markY = facadeBottom + (showDimensions ? 68 + dimensionLanes.bottom * 34 : 24);
  const facadeOrientationX = orientationColumnX;
  const facadeOrientation = presentation.showFacadeOrientation === false
    ? ""
    : `<g class="design-window__facade-orientation" data-placement="outside-right" transform="translate(${formatCoordinate(facadeOrientationX)} 8)" pointer-events="none"><rect width="68" height="42" rx="5" fill="#ffffff" fill-opacity="0.96" stroke="#cbd5e1" /><text x="34" y="16" text-anchor="middle" fill="#075985" font-size="11" font-weight="600">室外 ↑</text><line x1="9" y1="22" x2="59" y2="22" stroke="#e2e8f0" /><text x="34" y="35" text-anchor="middle" fill="#9a3412" font-size="11" font-weight="600">室内 ↓</text></g>`;
  // The elevation uses only the short business number. Size belongs to the
  // dimension system and product/template wording belongs to the inspector.
  const markLabel = mark;
  return [
    `<g class="design-window" data-object-id="${id}" data-window-id="${id}" data-render-scale="${scale}" data-visual-asset-diagnostic-count="${assetDiagnostics.length}"${diagnosticCodes ? ` data-visual-asset-diagnostic-codes="${diagnosticCodes}"` : ""}${selectedAttribute(window.objectId)} transform="translate(${x} ${y})">`,
    textureDefinitions ? `<defs>${textureDefinitions}</defs>` : "",
    diagnosticMetadata,
    facadeOrientation,
    facadeSurround,
    `<rect class="design-window__outline" width="${width}" height="${height}" fill="${glassStyle.cssColor}" fill-opacity="${formatCoordinate(Math.min(0.18, glassStyle.opacity))}" stroke="#475569" stroke-width="2" pointer-events="none" />`,
    ...cells,
    ...slidingPanels,
    ...openings,
    ...meetingMullions,
    ...frames,
    ...members,
    ...foregroundOpenings,
    ...hardware,
    ...dimensions,
    profileAnnotation,
    presentation.showMark === false
      ? ""
      : `<text class="design-window__mark" x="${width / 2}" y="${markY}" text-anchor="middle" fill="#334155" font-size="14">${markLabel}</text>`,
    plan,
    "</g>"
  ].join("");
}

/**
 * Projects the complete design snapshot into a deterministic SVG document.
 *
 * Algorithm: compute one scale that fits every window into a horizontal layout,
 * then serialize each object with its stable ID. The result is shared by PC and
 * mobile; CSS controls only how the same SVG is placed inside each shell.
 *
 * @param document Shared immutable design snapshot.
 * @param options Optional viewport and padding values for export or preview.
 * @returns A self-contained SVG string.
 * @example `renderDesignSvg(session.document)`.
 * @since 0.1.0
 * @modified 2026-09-17 - Added the shared SVG renderer vertical slice.
 */
export function renderDesignSvg(
  document: DesignDocument,
  options: SvgRenderOptions = {}
): string {
  const settings = {
    viewportWidth: options.viewportWidth ?? DEFAULT_OPTIONS.viewportWidth,
    viewportHeight: options.viewportHeight ?? DEFAULT_OPTIONS.viewportHeight,
    padding: options.padding ?? DEFAULT_OPTIONS.padding,
    showDimensions: options.showDimensions ?? DEFAULT_OPTIONS.showDimensions,
    showPlanView: options.showPlanView ?? DEFAULT_OPTIONS.showPlanView,
    showOpeningState: options.showOpeningState ?? DEFAULT_OPTIONS.showOpeningState,
    renderStyle: options.renderStyle ?? DEFAULT_OPTIONS.renderStyle
  };
  const viewport = options.viewport ?? { scale: 1, x: 0, y: 0 };
  const gap = 48;
  const windowById = new Map(document.windows.map((window) => [window.objectId, window]));
  const assemblyById = new Map(
    (document.assemblies ?? []).map((assembly) => [assembly.objectId, assembly])
  );
  const facadeInsetsByWindow = new Map(
    document.windows.map((window) => [window.objectId, resolveSvgFacadeSurroundInsetsMm(window)])
  );
  /**
   * Converts the domain installation subjects into one 2D layout list.
   *
   * Connected instances keep their exact assembly transforms and connector
   * zones. Only independent subjects receive the presentation gap, so rendering
   * cannot accidentally imply that an engineering joint is empty wall space.
   */
  const subjects = resolveInstallationSubjects(document).map((subject) => {
    if (subject.kind === "fabrication-assembly") {
      const assembly = assemblyById.get(subject.objectId);
      if (!assembly) throw new Error(`Fabrication assembly ${subject.objectId} could not be resolved.`);
      const geometry = resolveFabricationAssemblyGeometry(assembly, document.windows);
      const installation = normalizeWindowInstallation(assembly.installation);
      const outsideEnabled = installation.surround.enabled &&
        (installation.surround.styleId === "both_sides" ||
          installation.surround.styleId === "outside_only");
      const activeSides = new Set(installation.surround.sides);
      const assemblyInsets = {
        top: outsideEnabled && activeSides.has("top") ? installation.surround.outsideWidthMm : 0,
        right: outsideEnabled && activeSides.has("right") ? installation.surround.outsideWidthMm : 0,
        bottom: outsideEnabled && activeSides.has("bottom") ? installation.surround.outsideWidthMm : 0,
        left: outsideEnabled && activeSides.has("left") ? installation.surround.outsideWidthMm : 0
      };
      return {
        kind: subject.kind,
        objectId: subject.objectId,
        widthMm: assemblyInsets.left + geometry.bounds.widthMm + assemblyInsets.right,
        heightMm: assemblyInsets.top + geometry.bounds.heightMm + assemblyInsets.bottom,
        geometry,
        assemblyInsets,
        window: undefined
      } as const;
    }
    const window = windowById.get(subject.windowIds[0] ?? subject.objectId);
    if (!window) throw new Error(`Window subject ${subject.objectId} could not be resolved.`);
    const insets = facadeInsetsByWindow.get(window.objectId)!;
    return {
      kind: subject.kind,
      objectId: subject.objectId,
      widthMm: insets.left + window.widthMm + insets.right,
      heightMm: insets.top + window.heightMm + insets.bottom,
      geometry: undefined,
      assemblyInsets: undefined,
      window
    } as const;
  });
  const assemblyDimensionLanesById = new Map(
    subjects.flatMap((subject) =>
      subject.kind === "fabrication-assembly" && subject.geometry
        ? [[subject.objectId, resolveAssemblyDimensionLaneLayout(subject.geometry)] as const]
        : []
    )
  );
  const maximumTopLane = Math.max(
    0,
    ...Array.from(assemblyDimensionLanesById.values(), (layout) => layout.maximumTopLane)
  );
  const maximumRightLane = Math.max(
    0,
    ...Array.from(assemblyDimensionLanesById.values(), (layout) => layout.maximumRightLane)
  );
  const maximumBottomLane = Math.max(
    0,
    ...Array.from(assemblyDimensionLanesById.values(), (layout) => layout.maximumBottomLane)
  );
  const totalWidthMm = subjects.reduce((total, subject) => total + subject.widthMm, 0);
  const maxHeightMm = Math.max(1, ...subjects.map((subject) => subject.heightMm));
  // Annotation reserves are screen-space bands. They are deliberately outside
  // the engineering scale so zooming a large product cannot collapse labels
  // back over profiles, opening symbols or neighbouring dimensions.
  const topAnnotationReserve = settings.showDimensions
    ? 70 + maximumTopLane * 66
    : 24;
  const dimensionRightReserve = settings.showDimensions
    ? 96 + maximumRightLane * 66
    : 0;
  const orientationRightReserve = settings.showDimensions
    ? 166 + maximumRightLane * 66
    : 98;
  const rightAnnotationReserve = Math.max(
    dimensionRightReserve,
    orientationRightReserve
  );
  const bottomAnnotationReserve = settings.showDimensions
    ? (settings.showPlanView ? 112 : 100) + maximumBottomLane * 34
    : settings.showPlanView ? 44 : 24;
  const widthForWindows = settings.viewportWidth - settings.padding * 2 -
    rightAnnotationReserve * Math.max(1, subjects.length) -
    gap * Math.max(0, subjects.length - 1);
  const maxPlanSpanMm = settings.showPlanView
    ? Math.max(
        1,
        ...document.windows.map((window) => resolveWindowPlanEnvelopeMm(window).spanMm),
        ...subjects.flatMap((subject) =>
          subject.kind === "fabrication-assembly" && subject.geometry
            ? (() => {
                const installation = normalizeWindowInstallation(
                  subject.geometry.assembly.installation
                );
                const maximumFrameDepthMm = Math.max(
                  1,
                  ...subject.geometry.planInstances.map((instance) => instance.frameDepthMm)
                );
                const marginMm = Math.max(
                  0,
                  (installation.surround.wallThicknessMm - maximumFrameDepthMm) / 2 +
                    installation.surround.boardThicknessMm,
                  installation.surround.enabled
                    ? Math.max(
                        installation.surround.outsideWidthMm,
                        installation.surround.insideWidthMm
                      ) + installation.surround.boardThicknessMm
                    : 0
                );
                return [subject.geometry.planBounds.depthMm + marginMm * 2];
              })()
            : [])
      )
    : 0;
  const planDecorationReserve = settings.showPlanView ? 32 : 0;
  const heightForGeometry = settings.viewportHeight - settings.padding * 2 -
    topAnnotationReserve - bottomAnnotationReserve - planDecorationReserve;
  const heightGeometryMm = maxHeightMm + maxPlanSpanMm;
  const scale = subjects.length
    ? Math.max(0.01, Math.min(
        widthForWindows / Math.max(1, totalWidthMm),
        heightForGeometry / Math.max(1, heightGeometryMm)
      ))
    : 1;

  type LabelOwnerPlacement = Readonly<{
    elevationX: number;
    elevationY: number;
    planX: number;
    planY: number;
    scale: number;
  }>;
  const labelOwnerPlacements = new Map<string, LabelOwnerPlacement>();

  let cursorX = settings.padding;
  const groups = subjects.map((subject, subjectIndex) => {
    const showSharedFacadeOrientation = subjectIndex === subjects.length - 1;
    if (subject.kind === "window") {
      const window = subject.window;
      const insets = facadeInsetsByWindow.get(window.objectId)!;
      const elevationX = cursorX + insets.left * scale;
      const elevationY = settings.padding + topAnnotationReserve + insets.top * scale;
      labelOwnerPlacements.set(window.objectId, {
        elevationX,
        elevationY,
        planX: elevationX,
        planY: elevationY + (window.heightMm + insets.bottom) * scale +
          (settings.showDimensions ? 82 : 44),
        scale
      });
      const group = renderWindow(
        window,
        elevationX,
        elevationY,
        scale,
        options.selectedObjectId,
        settings.showDimensions,
        settings.showPlanView,
        settings.showOpeningState,
        options.openingProgressPercentByPanelKey,
        options.openingMotionModeByPanelKey,
        options.openingConnectionPresetByPanelKey,
        options.visualAssets,
        {
          showFacadeOrientation: showSharedFacadeOrientation,
          showPlanOrientation: showSharedFacadeOrientation,
          showPlanLabel: showSharedFacadeOrientation
        }
      );
      cursorX += subject.widthMm * scale + gap;
      return group;
    }
    const geometry = subject.geometry;
    const laneLayout = assemblyDimensionLanesById.get(subject.objectId) ??
      resolveAssemblyDimensionLaneLayout(geometry);
    const baseX = cursorX + subject.assemblyInsets.left * scale;
    const baseY = settings.padding + topAnnotationReserve + subject.assemblyInsets.top * scale;
    const selected = options.selectedObjectId === geometry.assembly.objectId
      ? ' data-selected="true" aria-current="true"'
      : "";
    const assemblyWallObjectId = createWindowInstallationWallObjectId(
      geometry.assembly.objectId
    );
    const assemblySurroundObjectId = createWindowInstallationSurroundObjectId(
      geometry.assembly.objectId
    );
    const selectedAssemblyInstallation = options.selectedObjectId !== undefined && (
      options.selectedObjectId === assemblyWallObjectId ||
      options.selectedObjectId.startsWith(`${assemblyWallObjectId}.`) ||
      options.selectedObjectId === assemblySurroundObjectId ||
      options.selectedObjectId.startsWith(`${assemblySurroundObjectId}.`)
    );
    const windows = geometry.instances.map((instance) => {
      const window = windowById.get(instance.windowId);
      if (!window) throw new Error(`Assembly window ${instance.windowId} could not be resolved.`);
      const dimensionLane = laneLayout.byInstanceId.get(instance.instanceId);
      const showAssemblyOverview = !options.selectedObjectId ||
        options.selectedObjectId === geometry.assembly.objectId ||
        selectedAssemblyInstallation;
      const showChildDimensions = settings.showDimensions && (
        !options.selectedObjectId ||
        options.selectedObjectId === geometry.assembly.objectId ||
        selectedAssemblyInstallation ||
        options.selectedObjectId === instance.instanceId ||
        windowOwnsRenderedObjectId(window, options.selectedObjectId)
      );
      return renderWindow(
        window,
        baseX + (instance.xMm - geometry.bounds.xMm) * scale,
        baseY + (instance.yMm - geometry.bounds.yMm) * scale,
        scale,
        options.selectedObjectId,
        showChildDimensions,
        false,
        settings.showOpeningState,
        options.openingProgressPercentByPanelKey,
        options.openingMotionModeByPanelKey,
        options.openingConnectionPresetByPanelKey,
        options.visualAssets,
        {
          dimensionEnvelopeInsetsMm: {
            top: instance.yMm - geometry.bounds.yMm,
            right: geometry.bounds.xMm + geometry.bounds.widthMm -
              (instance.xMm + instance.widthMm),
            bottom: geometry.bounds.yMm + geometry.bounds.heightMm -
              (instance.yMm + instance.heightMm),
            left: instance.xMm - geometry.bounds.xMm
          },
          showFacadeOrientation: false,
          showPlanOrientation: false,
          showPlanLabel: false,
          dimensionLanes: dimensionLane,
          dimensionVisibility: showAssemblyOverview
            ? {
                cellHeights: dimensionLane?.showHeightDimensions ?? true,
                outerHeight: false
              }
            : undefined,
          showMark: false
        }
      );
    }).join("");
    const joints = geometry.joints.map((joint) => {
      const jointSelected = options.selectedObjectId === joint.jointId
        ? ' data-selected="true" aria-current="true"'
        : "";
      const x = baseX + (joint.xMm - geometry.bounds.xMm) * scale;
      const y = baseY + (joint.yMm - geometry.bounds.yMm) * scale;
      const width = Math.max(1, joint.widthMm * scale);
      const height = Math.max(1, joint.heightMm * scale);
      const catalogSelection = geometry.assembly.joints.find(
        (candidate) => candidate.objectId === joint.jointId
      )?.catalogSelection;
      const catalogAttributes = catalogSelection
        ? ` data-catalog-item-id="${escapeXml(catalogSelection.catalogItemId)}" data-catalog-version="${escapeXml(catalogSelection.catalogVersion)}"`
        : "";
      const vertical = height >= width;
      const detail = joint.jointType === "reinforced_mullion"
        ? vertical
          ? `<line class="design-assembly__joint-detail design-assembly__joint-detail--reinforcement" x1="${formatCoordinate(x + width * 0.35)}" y1="${formatCoordinate(y)}" x2="${formatCoordinate(x + width * 0.35)}" y2="${formatCoordinate(y + height)}" stroke="#cbd5e1" /><line class="design-assembly__joint-detail design-assembly__joint-detail--reinforcement" x1="${formatCoordinate(x + width * 0.65)}" y1="${formatCoordinate(y)}" x2="${formatCoordinate(x + width * 0.65)}" y2="${formatCoordinate(y + height)}" stroke="#cbd5e1" />`
          : `<line class="design-assembly__joint-detail design-assembly__joint-detail--reinforcement" x1="${formatCoordinate(x)}" y1="${formatCoordinate(y + height * 0.35)}" x2="${formatCoordinate(x + width)}" y2="${formatCoordinate(y + height * 0.35)}" stroke="#cbd5e1" /><line class="design-assembly__joint-detail design-assembly__joint-detail--reinforcement" x1="${formatCoordinate(x)}" y1="${formatCoordinate(y + height * 0.65)}" x2="${formatCoordinate(x + width)}" y2="${formatCoordinate(y + height * 0.65)}" stroke="#cbd5e1" />`
        : vertical
          ? `<line class="design-assembly__joint-detail design-assembly__joint-detail--centre" x1="${formatCoordinate(x + width / 2)}" y1="${formatCoordinate(y)}" x2="${formatCoordinate(x + width / 2)}" y2="${formatCoordinate(y + height)}" stroke="#cbd5e1" />`
          : `<line class="design-assembly__joint-detail design-assembly__joint-detail--centre" x1="${formatCoordinate(x)}" y1="${formatCoordinate(y + height / 2)}" x2="${formatCoordinate(x + width)}" y2="${formatCoordinate(y + height / 2)}" stroke="#cbd5e1" />`;
      return `<g class="design-assembly__joint-group design-assembly__joint-group--${joint.jointType}" data-object-id="${escapeXml(joint.jointId)}"${jointSelected} data-assembly-id="${escapeXml(geometry.assembly.objectId)}" data-joint-type="${joint.jointType}"${catalogAttributes} data-first-instance-id="${escapeXml(joint.firstInstanceId)}" data-second-instance-id="${escapeXml(joint.secondInstanceId)}"><rect class="design-assembly__joint"${jointSelected} x="${formatCoordinate(x)}" y="${formatCoordinate(y)}" width="${formatCoordinate(width)}" height="${formatCoordinate(height)}" fill="#475569" stroke="#1e293b" />${detail}</g>`;
    }).join("");
    const assemblyInstallation = normalizeWindowInstallation(geometry.assembly.installation);
    const firstAssemblyWindowId = geometry.instances[0]?.windowId;
    const firstAssemblyWindow = firstAssemblyWindowId
      ? windowById.get(firstAssemblyWindowId)
      : undefined;
    if (!firstAssemblyWindow) {
      throw new Error(`Assembly ${geometry.assembly.objectId} has no renderable member window.`);
    }
    const surroundStyle = resolveAppearanceRenderStyle(
      resolveWindowVisualConfigurationForRender(firstAssemblyWindow).appearance.surroundOutside
    );
    const surround = assemblyInstallation.surround.enabled &&
      (assemblyInstallation.surround.styleId === "both_sides" ||
        assemblyInstallation.surround.styleId === "outside_only")
      ? geometry.outline.flatMap((segment, segmentIndex) => {
          const side = segment.side;
          if (!assemblyInstallation.surround.sides.includes(side)) return [];
          const face = assemblyInstallation.surround.outsideWidthMm * scale;
          const horizontal = side === "top" || side === "bottom";
          const startX = baseX + (Math.min(segment.startXMm, segment.endXMm) - geometry.bounds.xMm) * scale;
          const endX = baseX + (Math.max(segment.startXMm, segment.endXMm) - geometry.bounds.xMm) * scale;
          const startY = baseY + (Math.min(segment.startYMm, segment.endYMm) - geometry.bounds.yMm) * scale;
          const endY = baseY + (Math.max(segment.startYMm, segment.endYMm) - geometry.bounds.yMm) * scale;
          const x = horizontal
            ? startX - face
            : side === "left" ? startX - face : startX;
          const y = horizontal
            ? side === "top" ? startY - face : startY
            : startY;
          const pieceWidth = horizontal ? endX - startX + face * 2 : face;
          const pieceHeight = horizontal ? face : endY - startY;
          const sideObjectId = createWindowInstallationSurroundObjectId(
            geometry.assembly.objectId,
            "outside",
            side
          );
          const objectId = `${sideObjectId}.segment.${segmentIndex + 1}`;
          const pieceSelected = options.selectedObjectId === objectId ||
            options.selectedObjectId === sideObjectId ||
            options.selectedObjectId === createWindowInstallationSurroundObjectId(
              geometry.assembly.objectId
            )
            ? ' data-selected="true" aria-current="true"'
            : "";
          return [`<rect class="design-assembly__surround design-assembly__surround--outside" data-object-id="${escapeXml(objectId)}"${pieceSelected} data-assembly-id="${escapeXml(geometry.assembly.objectId)}" data-installation-side="${side}" data-outline-segment-index="${segmentIndex}" data-material-code="${escapeXml(assemblyInstallation.surround.materialCode)}" x="${formatCoordinate(x)}" y="${formatCoordinate(y)}" width="${formatCoordinate(pieceWidth)}" height="${formatCoordinate(pieceHeight)}" fill="${surroundStyle.cssColor}" fill-opacity="${formatCoordinate(surroundStyle.opacity)}" stroke="${surroundStyle.cssColor}" />`];
        }).join("")
      : "";
    /**
     * Keeps the assembly outline selectable without emitting a permanent line.
     *
     * The former blue dashed contour was an interaction helper, not a product
     * edge or manufacturing line. It became visually dominant on the white
     * engineering canvas. Every real contour segment now has a wide transparent
     * hit stroke; a solid orange overlay is serialized only while the assembly
     * itself is selected. Child windows, joints and dimensions keep their own
     * independent selection identities.
     *
     * @example An unselected two-window assembly has four transparent hit lines
     * and no visible `design-assembly__outline-selection` nodes.
     * @since 0.10.72
     * @modified 2026-09-22 - Removed the permanent dashed assembly helper.
     */
    const outlineLines = geometry.outline.map((segment) => {
      const x1 = formatCoordinate(baseX + (segment.startXMm - geometry.bounds.xMm) * scale);
      const y1 = formatCoordinate(baseY + (segment.startYMm - geometry.bounds.yMm) * scale);
      const x2 = formatCoordinate(baseX + (segment.endXMm - geometry.bounds.xMm) * scale);
      const y2 = formatCoordinate(baseY + (segment.endYMm - geometry.bounds.yMm) * scale);
      return `<line class="design-assembly__outline-hit" x1="${x1}" y1="${y1}" x2="${x2}" y2="${y2}" fill="none" stroke="transparent" stroke-width="14" vector-effect="non-scaling-stroke" pointer-events="stroke" />` +
        (options.selectedObjectId === geometry.assembly.objectId
          ? `<line class="design-assembly__outline-selection" x1="${x1}" y1="${y1}" x2="${x2}" y2="${y2}" fill="none" stroke="#ff8a00" stroke-width="2.5" vector-effect="non-scaling-stroke" pointer-events="none" />`
          : "");
    }).join("");
    const outline = `<g class="design-assembly__outline" data-object-id="${escapeXml(geometry.assembly.objectId)}"${selected} data-fills-bounding-rectangle="${geometry.fillsBoundingRectangle}" data-outline-visibility="${options.selectedObjectId === geometry.assembly.objectId ? "selection-only" : "hit-only"}">${outlineLines}</g>`;
    const assemblyDimensions = settings.showDimensions
      ? renderFabricationAssemblyDimensions(
          geometry,
          baseX,
          baseY,
          scale,
          laneLayout,
          options.selectedObjectId
        )
      : "";
    const assemblyPlanY = baseY + geometry.bounds.heightMm * scale +
      (settings.showDimensions ? 112 + laneLayout.maximumBottomLane * 34 : 34);
    labelOwnerPlacements.set(geometry.assembly.objectId, {
      elevationX: baseX,
      elevationY: baseY,
      planX: baseX,
      planY: assemblyPlanY,
      scale
    });
    const assemblyFacadeWidth = geometry.bounds.widthMm * scale;
    const assemblyPlan = settings.showPlanView
      ? `<g transform="translate(${formatCoordinate(baseX)} ${formatCoordinate(assemblyPlanY)})">${renderFabricationAssemblyPlan({
          geometry,
          windowById,
          scale,
          showDimensions: settings.showDimensions,
          showOpeningState: settings.showOpeningState,
          selectedObjectId: options.selectedObjectId,
          openingProgressPercentByPanelKey: options.openingProgressPercentByPanelKey,
          openingMotionModeByPanelKey: options.openingMotionModeByPanelKey,
          visualAssets: options.visualAssets,
          showPlanOrientation: showSharedFacadeOrientation,
          showPlanLabel: showSharedFacadeOrientation,
          planOrientationX: assemblyFacadeWidth + (
            settings.showDimensions ? 92 + laneLayout.maximumRightLane * 66 : 18
          )
        })}</g>`
      : "";
    const assemblyOrientationX = baseX + assemblyFacadeWidth + (
      settings.showDimensions ? 92 + laneLayout.maximumRightLane * 66 : 18
    );
    const assemblyOrientation = showSharedFacadeOrientation
      ? `<g class="design-assembly__facade-orientation" data-placement="outside-right" transform="translate(${formatCoordinate(assemblyOrientationX)} ${formatCoordinate(baseY + 8)})" pointer-events="none"><rect width="68" height="42" rx="5" fill="#ffffff" fill-opacity="0.96" stroke="#cbd5e1" /><text x="34" y="16" text-anchor="middle" fill="#075985" font-size="11" font-weight="600">室外 ↑</text><line x1="9" y1="22" x2="59" y2="22" stroke="#e2e8f0" /><text x="34" y="35" text-anchor="middle" fill="#9a3412" font-size="11" font-weight="600">室内 ↓</text></g>`
      : "";
    cursorX += subject.widthMm * scale + gap;
    return `<g class="design-fabrication-assembly" data-object-id="${escapeXml(geometry.assembly.objectId)}" data-assembly-window-count="${geometry.instances.length}" data-recommended-opening-width-mm="${formatCoordinate(geometry.recommendedOpening.widthMm)}" data-recommended-opening-height-mm="${formatCoordinate(geometry.recommendedOpening.heightMm)}">${surround}${joints}${windows}${outline}${assemblyOrientation}${assemblyDimensions}${assemblyPlan}</g>`;
  });
  /** Renders one label occurrence without allowing it to affect product fitting. */
  const renderLabelOccurrence = (
    label: DrawingTextLabel,
    placement: LabelOwnerPlacement,
    view: "elevation" | "plan"
  ): string => {
    const originX = view === "plan" ? placement.planX : placement.elevationX;
    const originY = view === "plan" ? placement.planY : placement.elevationY;
    const x = originX + label.xMm * placement.scale;
    const y = originY + label.yMm * placement.scale;
    const fontSize = Math.max(8, label.fontSizePaperMm * 4);
    const textWidth = Math.max(fontSize, Array.from(label.text).length * fontSize * 0.62);
    const boxX = label.align === "center"
      ? -textWidth / 2 - 4
      : label.align === "right"
        ? -textWidth - 4
        : -4;
    const textAnchor = label.align === "center" ? "middle" : label.align === "right" ? "end" : "start";
    const selected = options.selectedObjectId === label.objectId;
    const selectedAttributes = selected ? ' data-selected="true" aria-current="true"' : "";
    return `<g class="design-text-label design-text-label--${view}" tabindex="0" role="button" aria-label="文字标签：${escapeXml(label.text)}" data-object-id="${escapeXml(label.objectId)}" data-owner-object-id="${escapeXml(label.ownerObjectId)}" data-member-kind="drawing-label" data-drag-owner-id="${escapeXml(label.objectId)}" data-render-scale="${formatCoordinate(placement.scale)}" data-label-view="${view}" data-label-x-svg="${formatCoordinate(x)}" data-label-y-svg="${formatCoordinate(y)}" data-print-visible="${label.printVisible}"${selectedAttributes} transform="translate(${formatCoordinate(x)} ${formatCoordinate(y)})"><g transform="rotate(${formatCoordinate(label.rotationDeg)})"><rect class="design-text-label__hit" x="${formatCoordinate(boxX)}" y="${formatCoordinate(-fontSize)}" width="${formatCoordinate(textWidth + 8)}" height="${formatCoordinate(fontSize + 6)}" rx="3" fill="transparent" stroke="${selected ? "#ff8a00" : "transparent"}" stroke-width="${selected ? "1.5" : "8"}" vector-effect="non-scaling-stroke" pointer-events="all" /><text class="design-text-label__text" x="0" y="0" text-anchor="${textAnchor}" fill="${label.color}" font-size="${formatCoordinate(fontSize)}" font-weight="500" pointer-events="none">${escapeXml(label.text)}</text></g></g>`;
  };
  const textLabels = (document.drawingTextLabels ?? []).flatMap((label) => {
    const placement = labelOwnerPlacements.get(label.ownerObjectId);
    if (!placement) return [];
    const occurrences: string[] = [];
    if (label.view === "elevation" || label.view === "both") {
      occurrences.push(renderLabelOccurrence(label, placement, "elevation"));
    }
    if (settings.showPlanView && (label.view === "plan" || label.view === "both")) {
      occurrences.push(renderLabelOccurrence(label, placement, "plan"));
    }
    return occurrences;
  });
  const emptyState = document.windows.length
    ? ""
    : `<text x="50%" y="50%" text-anchor="middle" fill="#64748b" font-size="18">共享设计核心：请选择一个布局壳创建窗体</text>`;

  return [
    `<svg xmlns="http://www.w3.org/2000/svg" class="design-svg design-svg--${settings.renderStyle}" data-render-style="${settings.renderStyle}" data-material-rendering="${settings.renderStyle === "engineering-line" ? "suppressed" : "catalogue-preview"}" viewBox="0 0 ${settings.viewportWidth} ${settings.viewportHeight}" role="img" aria-label="DoorMes 2D design">`,
    settings.renderStyle === "engineering-line" ? ENGINEERING_LINE_STYLE : "",
    `<g id="designCanvasViewport" class="design-canvas-viewport" data-design-id="${escapeXml(document.designId)}" data-revision="${document.revision}" data-scale="${viewport.scale}" transform="translate(${viewport.x} ${viewport.y}) scale(${viewport.scale})">`,
    emptyState,
    ...groups,
    ...textLabels,
    "</g>",
    "</svg>"
  ].join("");
}
