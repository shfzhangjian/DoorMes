/**
 * Identifies why one shared design snapshot is being projected.
 *
 * The value controls presentation rules only. It must never change product
 * geometry, material identity, opening kinematics or BOM calculations.
 *
 * @example A factory sheet and an installation sheet can reference the same
 * `WIN-001` revision while using different annotations and title blocks.
 * @since 0.1.0
 * @modified 2026-09-21 - Added the first shared three-projection contract.
 */
export type DrawingProjectionKind =
  | "design-2d"
  | "simulation-3d"
  | "factory-sheet"
  | "installation-sheet";

/**
 * Names the first supported ISO paper formats.
 *
 * Sizes are resolved in physical millimetres so browser zoom, SVG pixels and
 * device pixel ratio cannot silently move engineering annotations.
 *
 * @example `A3` landscape resolves to 420 × 297 paper millimetres.
 * @since 0.1.0
 * @modified 2026-09-21 - Added A4/A3 technical-sheet foundations.
 */
export type DrawingPaperFormat = "A4" | "A3";

/** Describes a paper orientation without coupling the model to a renderer. */
export type DrawingPaperOrientation = "portrait" | "landscape";

/**
 * One point measured in model millimetres or paper millimetres by its owner.
 * @example `{ x: 25, y: 40 }` can be a paper anchor inside one view frame.
 * @since 0.1.0
 * @modified 2026-09-21 - Added renderer-neutral two-axis coordinates.
 */
export interface DrawingPointMm {
  readonly x: number;
  readonly y: number;
}

/**
 * One rectangle measured in model millimetres or paper millimetres by its owner.
 * @example `{ x: 0, y: 0, width: 1200, height: 1500 }` describes a window model envelope.
 * @since 0.1.0
 * @modified 2026-09-21 - Added shared model/paper envelopes.
 */
export interface DrawingRectMm extends DrawingPointMm {
  readonly width: number;
  readonly height: number;
}

/**
 * Shared ownership and linework layer metadata for one technical drawing primitive.
 * @example A frame rectangle retains the exact frame-segment object ID as its source.
 * @since 0.2.0
 * @modified 2026-09-21 - Added renderer-neutral technical linework.
 */
export interface DrawingPrimitiveBase {
  readonly primitiveId: string;
  readonly sourceObjectIds: readonly string[];
  readonly layer: "outline" | "frame" | "sash" | "glass" | "mullion" | "joint" | "symbol";
  /**
   * Print-line convention independent of SVG/CAD adapters.
   *
   * Dashed geometry is used for closed/reference positions and angle arcs;
   * solid remains the default for manufactured edges.
   * @example A plan-view closed sash reference uses `dashed` while the actual
   * configured sash footprint uses `solid`.
   * @since 0.4.0
   * @modified 2026-09-21 - Added renderer-neutral plan/reference line styles.
   */
  readonly lineStyle?: "solid" | "dashed" | "center";
}

/**
 * Axis-aligned model-space rectangle projected as technical linework.
 * @example One outer-frame segment uses layer `frame` and its resolved mm bounds.
 * @since 0.2.0
 * @modified 2026-09-21 - Added frame/cell/member elevation primitives.
 */
export interface DrawingRectanglePrimitive extends DrawingPrimitiveBase {
  readonly kind: "rectangle";
  readonly boundsModelMm: DrawingRectMm;
}

/**
 * One model-space line, commonly used by conventional opening symbols.
 * @example A side-hung symbol connects one hinge corner to the opposite midpoint.
 * @since 0.2.0
 * @modified 2026-09-21 - Added semantic opening-symbol linework.
 */
export interface DrawingLinePrimitive extends DrawingPrimitiveBase {
  readonly kind: "line";
  readonly startModelMm: DrawingPointMm;
  readonly endModelMm: DrawingPointMm;
}

/**
 * Connected model-space outline used by non-rectangular fabrication assemblies.
 * @example An L-shaped assembly exterior is stored as one closed polyline.
 * @since 0.2.0
 * @modified 2026-09-21 - Added exact assembly-outline linework.
 */
export interface DrawingPolylinePrimitive extends DrawingPrimitiveBase {
  readonly kind: "polyline";
  readonly pointsModelMm: readonly DrawingPointMm[];
  readonly closed: boolean;
}

/** Technical geometry supported by the first black-and-white SVG adapter. */
export type DrawingPrimitive =
  | DrawingRectanglePrimitive
  | DrawingLinePrimitive
  | DrawingPolylinePrimitive;

/**
 * Defines one placed drawing view on a technical sheet.
 *
 * `modelBoundsMm` supplies the model-space origin and envelope. `framePaperMm`
 * is its paper-space placement. A scale denominator of 20 means 20 model
 * millimetres are represented by one paper millimetre.
 *
 * @example A 1200 × 1500 window at 1:20 occupies 60 × 75 mm before labels.
 * @since 0.1.0
 * @modified 2026-09-21 - Added renderer-neutral view placement.
 */
export interface DrawingView {
  readonly viewId: string;
  readonly projection: DrawingProjectionKind;
  readonly viewKind: "elevation" | "plan" | "section" | "detail" | "perspective";
  readonly sourceObjectIds: readonly string[];
  readonly scaleDenominator: number;
  readonly modelBoundsMm: DrawingRectMm;
  readonly framePaperMm: DrawingRectMm;
  /** Optional semantic linework; later raster/model views may intentionally omit it. */
  readonly primitives?: readonly DrawingPrimitive[];
}

/**
 * Shared metadata carried by every semantic drawing annotation.
 * @example A material callout links its stable ID to one frame source object.
 * @since 0.1.0
 * @modified 2026-09-21 - Added stable annotation ownership and layers.
 */
export interface DrawingAnnotationBase {
  readonly annotationId: string;
  readonly viewId: string;
  readonly sourceObjectIds: readonly string[];
  readonly layer: "dimensions" | "symbols" | "materials" | "notes" | "diagnostics";
  readonly priority: number;
}

/**
 * Distinguishes engineering dimension hierarchy before layout.
 *
 * Layout always places detail dimensions nearest the view, segment dimensions
 * after every detail lane and overall dimensions farthest away. This preserves
 * reading order even when collision avoidance creates extra lanes.
 */
export type DrawingDimensionLevel = "detail" | "segment" | "overall";

/** The four legal sides on which a linear dimension can be placed. */
export type DrawingDimensionSide = "top" | "right" | "bottom" | "left";

/**
 * Semantic linear dimension independent of SVG text and line nodes.
 *
 * The measured endpoints are coordinates along the view axis in model
 * millimetres. The source object IDs and stable annotation ID allow selection,
 * editing and audit without parsing localized labels.
 *
 * @example A window overall width may span 0..1200 on the bottom side with
 * level `overall` and label `总宽 1200 mm`.
 * @since 0.1.0
 * @modified 2026-09-21 - Added paper-space dimension semantics.
 */
export interface DrawingLinearDimensionAnnotation extends DrawingAnnotationBase {
  readonly kind: "linear-dimension";
  readonly axis: "horizontal" | "vertical";
  readonly side: DrawingDimensionSide;
  readonly level: DrawingDimensionLevel;
  readonly startModelMm: number;
  readonly endModelMm: number;
  readonly measuredValueMm: number;
  readonly label: string;
  /** Perpendicular model coordinate from which both witness lines originate. */
  readonly witnessOriginModelMm?: number;
  /** Optional measured label width; otherwise the pure layout engine estimates it. */
  readonly labelWidthPaperMm?: number;
}

/**
 * Semantic non-dimensional mark used for opening symbols, material callouts and notes.
 *
 * The annotation stores meaning and a model anchor. A renderer chooses the
 * actual standard glyph or leader style for the requested sheet profile.
 */
export interface DrawingReferenceAnnotation extends DrawingAnnotationBase {
  readonly kind: "opening-symbol" | "material-callout" | "component-callout" | "note";
  readonly anchorModelMm: DrawingPointMm;
  /**
   * Explicit paper-space displacement for the label end of a leader.
   *
   * Manufacturing marks use physical paper millimetres so their spacing stays
   * legible at every model scale. The model anchor remains attached to the
   * physical part while this value only controls annotation layout.
   */
  readonly labelOffsetPaperMm?: DrawingPointMm;
  readonly text?: string;
  readonly symbolCode?: string;
}

/** Every annotation supported by the first technical-sheet schema. */
export type DrawingAnnotation = DrawingLinearDimensionAnnotation | DrawingReferenceAnnotation;

/**
 * Stable semantic purpose of a tabular block printed on a technical sheet.
 *
 * The kinds deliberately separate design selections from calculated material
 * demand. A renderer must not relabel a `design-selection` table as an MBOM,
 * because a selected product model is not yet a released purchasing quantity.
 *
 * @example A sheet without a current formal BOM still prints its reviewed
 * profile/glass/hardware selections and a production-data warning.
 * @since 0.3.0
 * @modified 2026-09-21 - Added SHEET-002 traceable schedule semantics.
 */
export type DrawingTableKind =
  | "design-selection"
  | "manufacturing-materials"
  | "engineering-joints"
  | "production-diagnostics";

/**
 * One physical-paper column in a technical-sheet table.
 * @example A 25mm `物料编码` column remains 25mm in SVG and later PDF output.
 * @since 0.3.0
 * @modified 2026-09-21 - Added renderer-neutral table columns.
 */
export interface DrawingTableColumn {
  readonly key: string;
  readonly label: string;
  readonly widthPaperMm: number;
  readonly align?: "left" | "center" | "right";
}

/**
 * One auditable schedule row linked back to design, catalog or BOM identities.
 *
 * Cells follow the owning table's column order. `sourceObjectIds` may contain
 * several IDs when equivalent BOM lines are aggregated for printing; this
 * retains drill-down capability without duplicating rows on paper.
 *
 * @example A grouped AL70 frame row can reference the window plus four frame
 * component IDs while displaying one summed quantity.
 * @since 0.3.0
 * @modified 2026-09-21 - Added source-linked technical table rows.
 */
export interface DrawingTableRow {
  readonly rowId: string;
  readonly sourceObjectIds: readonly string[];
  readonly cells: readonly string[];
  readonly severity?: "info" | "warning" | "error";
}

/**
 * Physical-paper typography and row rhythm for one technical table.
 *
 * Storing these values in the drawing snapshot keeps SVG/PDF output identical
 * and prevents a renderer from silently shrinking production information to
 * fit. Values are millimetres on paper, not browser pixels.
 *
 * @example A 3mm body font is about 8.5pt on the printed sheet.
 * @since 0.4.0
 * @modified 2026-09-21 - Added readable, adapter-independent table typography.
 */
export interface DrawingTableLayout {
  readonly titleHeightPaperMm: number;
  readonly headerHeightPaperMm: number;
  readonly rowHeightPaperMm: number;
  readonly titleFontSizePaperMm: number;
  readonly headerFontSizePaperMm: number;
  readonly bodyFontSizePaperMm: number;
}

/**
 * One placed table measured entirely in physical paper millimetres.
 *
 * Tables are sheet-level projections, not view annotations: their values may
 * combine geometry, catalog and calculation results while every row retains
 * its own source identities. The frame is validated against the selected page.
 *
 * @example An A3 factory sheet places its material schedule in a 116mm panel
 * to the right of the scaled elevation.
 * @since 0.3.0
 * @modified 2026-09-21 - Added schedules shared by SVG/PDF adapters.
 */
export interface DrawingTable {
  readonly tableId: string;
  readonly kind: DrawingTableKind;
  readonly title: string;
  readonly framePaperMm: DrawingRectMm;
  readonly layout: DrawingTableLayout;
  readonly columns: readonly DrawingTableColumn[];
  readonly rows: readonly DrawingTableRow[];
}

/**
 * Immutable, renderer-neutral technical drawing snapshot.
 *
 * The snapshot references the exact design revision and owns views plus
 * semantic annotations. It deliberately excludes SVG/Canvas/Three objects.
 */
export interface DrawingSheet {
  readonly schemaVersion: "doormes-drawing-sheet.v1";
  readonly sheetId: string;
  /** Business-facing drawing number printed in the title block. */
  readonly drawingNumber: string;
  /** Issued drawing version, independent from the internal design revision. */
  readonly drawingVersion: string;
  readonly sourceDocumentId: string;
  readonly sourceRevision: number;
  readonly profile: "factory" | "installation";
  readonly paperFormat: DrawingPaperFormat;
  readonly orientation: DrawingPaperOrientation;
  readonly title: string;
  /** One-based page position within one drawing issue. */
  readonly pageNumber?: number;
  /** Total physical pages emitted for the same drawing number and version. */
  readonly pageCount?: number;
  readonly views: readonly DrawingView[];
  readonly annotations: readonly DrawingAnnotation[];
  /** Optional selection/BOM/joint/diagnostic schedules placed in paper space. */
  readonly tables?: readonly DrawingTable[];
}

/**
 * Physical paper width and height after orientation is applied.
 * @example A3 landscape is `{ width: 420, height: 297 }`.
 * @since 0.1.0
 * @modified 2026-09-21 - Added physical page sizing.
 */
export interface DrawingPaperSizeMm {
  readonly width: number;
  readonly height: number;
}

const PAPER_PORTRAIT_SIZE_MM: Readonly<Record<DrawingPaperFormat, DrawingPaperSizeMm>> = {
  A4: { width: 210, height: 297 },
  A3: { width: 297, height: 420 }
};

/**
 * Resolves one ISO sheet size in physical millimetres.
 *
 * Algorithm: read the canonical portrait dimensions, then swap width and
 * height for landscape. Returning a fresh object prevents callers from
 * mutating the built-in reference table.
 *
 * @example `resolveDrawingPaperSizeMm("A3", "landscape")` returns `{ width: 420, height: 297 }`.
 * @since 0.1.0
 * @modified 2026-09-21 - Added the first A4/A3 paper-size resolver.
 */
export function resolveDrawingPaperSizeMm(
  format: DrawingPaperFormat,
  orientation: DrawingPaperOrientation
): DrawingPaperSizeMm {
  const portrait = PAPER_PORTRAIT_SIZE_MM[format];
  return orientation === "portrait"
    ? { ...portrait }
    : { width: portrait.height, height: portrait.width };
}

/**
 * Paper-space spacing used by the deterministic dimension layout algorithm.
 * @example `{ firstOffsetPaperMm: 10, laneGapPaperMm: 8 }` keeps fixed print spacing.
 * @since 0.1.0
 * @modified 2026-09-21 - Added configurable, device-independent spacing.
 */
export interface PaperDimensionLayoutOptions {
  /** Distance between the view outline and its nearest dimension line. */
  readonly firstOffsetPaperMm?: number;
  /** Distance between neighbouring dimension lanes. */
  readonly laneGapPaperMm?: number;
  /** Minimum clear paper distance retained between neighbouring labels. */
  readonly minimumLabelGapPaperMm?: number;
}

/**
 * Resolved paper-space placement for one semantic linear dimension.
 * @example A top detail dimension in lane 0 may have a 10mm offset from the view.
 * @since 0.1.0
 * @modified 2026-09-21 - Added serializable output for SVG/PDF adapters.
 */
export interface PaperDimensionPlacement {
  readonly annotationId: string;
  readonly side: DrawingDimensionSide;
  readonly level: DrawingDimensionLevel;
  readonly laneIndex: number;
  readonly offsetFromViewPaperMm: number;
  readonly lineCoordinatePaperMm: number;
  readonly intervalStartPaperMm: number;
  readonly intervalEndPaperMm: number;
}

const DIMENSION_LEVEL_ORDER: Readonly<Record<DrawingDimensionLevel, number>> = {
  detail: 0,
  segment: 1,
  overall: 2
};

/**
 * Rejects non-finite drawing inputs before they can corrupt a complete sheet.
 * @example `assertFiniteNumber(Number.NaN, "scale")` throws a focused error.
 * @since 0.1.0
 * @modified 2026-09-21 - Added shared numeric validation.
 */
function assertFiniteNumber(value: number, field: string): void {
  if (!Number.isFinite(value)) {
    throw new Error(`${field} must be a finite number.`);
  }
}

/**
 * Resolves the collision width of one dimension label in paper millimetres.
 *
 * Explicit font metrics win; otherwise a deterministic conservative estimate
 * avoids DOM/canvas text measurement and keeps Node/PDF/browser results equal.
 * @example `1200 mm` receives an estimated width when the caller omits metrics.
 * @since 0.1.0
 * @modified 2026-09-21 - Added DOM-free label collision sizing.
 */
function estimateLabelWidthPaperMm(annotation: DrawingLinearDimensionAnnotation): number {
  if (annotation.labelWidthPaperMm !== undefined) {
    assertFiniteNumber(annotation.labelWidthPaperMm, `${annotation.annotationId}.labelWidthPaperMm`);
    if (annotation.labelWidthPaperMm <= 0) {
      throw new Error(`${annotation.annotationId}.labelWidthPaperMm must be greater than zero.`);
    }
    return annotation.labelWidthPaperMm;
  }
  return Math.max(10, annotation.label.length * 2.1 + 5);
}

/**
 * Validates view ownership, numeric range and axis/side compatibility.
 * @example A vertical dimension placed on `top` is rejected before layout.
 * @since 0.1.0
 * @modified 2026-09-21 - Added semantic dimension guardrails.
 */
function validateLinearDimension(
  view: DrawingView,
  annotation: DrawingLinearDimensionAnnotation
): void {
  if (annotation.viewId !== view.viewId) {
    throw new Error(`Annotation ${annotation.annotationId} does not belong to view ${view.viewId}.`);
  }
  assertFiniteNumber(annotation.startModelMm, `${annotation.annotationId}.startModelMm`);
  assertFiniteNumber(annotation.endModelMm, `${annotation.annotationId}.endModelMm`);
  assertFiniteNumber(annotation.measuredValueMm, `${annotation.annotationId}.measuredValueMm`);
  if (annotation.witnessOriginModelMm !== undefined) {
    assertFiniteNumber(
      annotation.witnessOriginModelMm,
      `${annotation.annotationId}.witnessOriginModelMm`
    );
  }
  if (annotation.startModelMm === annotation.endModelMm) {
    throw new Error(`Annotation ${annotation.annotationId} must measure a non-zero interval.`);
  }
  if (annotation.measuredValueMm <= 0) {
    throw new Error(`Annotation ${annotation.annotationId}.measuredValueMm must be greater than zero.`);
  }
  const sideRequiresHorizontal = annotation.side === "top" || annotation.side === "bottom";
  if (sideRequiresHorizontal !== (annotation.axis === "horizontal")) {
    throw new Error(`Annotation ${annotation.annotationId} axis does not match side ${annotation.side}.`);
  }
}

/**
 * Internal normalized interval used by the lane-colouring algorithm.
 * @since 0.1.0
 * @modified 2026-09-21 - Added expanded collision intervals for label clearance.
 */
interface PreparedDimension {
  readonly annotation: DrawingLinearDimensionAnnotation;
  readonly intervalStartPaperMm: number;
  readonly intervalEndPaperMm: number;
  readonly collisionStartPaperMm: number;
  readonly collisionEndPaperMm: number;
}

/**
 * Converts one model-space dimension into paper-space measurement and collision intervals.
 * @example At 1:20, model span 0..1200 becomes a 60mm paper interval.
 * @since 0.1.0
 * @modified 2026-09-21 - Added scale-aware interval preparation.
 */
function prepareDimension(
  view: DrawingView,
  annotation: DrawingLinearDimensionAnnotation,
  minimumLabelGapPaperMm: number
): PreparedDimension {
  validateLinearDimension(view, annotation);
  const modelOrigin = annotation.axis === "horizontal"
    ? view.modelBoundsMm.x
    : view.modelBoundsMm.y;
  const paperOrigin = annotation.axis === "horizontal"
    ? view.framePaperMm.x
    : view.framePaperMm.y;
  const start = paperOrigin + (Math.min(annotation.startModelMm, annotation.endModelMm) - modelOrigin) /
    view.scaleDenominator;
  const end = paperOrigin + (Math.max(annotation.startModelMm, annotation.endModelMm) - modelOrigin) /
    view.scaleDenominator;
  const center = (start + end) / 2;
  const labelHalfWidth = estimateLabelWidthPaperMm(annotation) / 2 + minimumLabelGapPaperMm / 2;
  const intervalHalfWidth = (end - start) / 2;
  const collisionHalfWidth = Math.max(labelHalfWidth, intervalHalfWidth);
  return {
    annotation,
    intervalStartPaperMm: start,
    intervalEndPaperMm: end,
    collisionStartPaperMm: center - collisionHalfWidth,
    collisionEndPaperMm: center + collisionHalfWidth
  };
}

/**
 * Places linear dimensions into deterministic paper-space lanes.
 *
 * Algorithm:
 * 1. validate the view, scale and stable annotation IDs;
 * 2. group annotations by side and semantic level;
 * 3. reserve entire inner bands for detail, then segment, then overall values;
 * 4. inside each band, greedily reuse a lane only when the expanded label
 *    intervals no longer overlap;
 * 5. calculate the final line coordinate from a fixed paper-mm offset.
 *
 * This makes dimension spacing independent of browser pixels and drawing
 * scale. The returned placements contain no SVG nodes and can be consumed by
 * SVG, PDF or a future DXF adapter.
 *
 * @param view Technical-sheet view with model and paper envelopes.
 * @param annotations Semantic linear dimensions belonging to that view.
 * @param options Optional paper-mm spacing overrides.
 * @returns Stable placements sorted by side, semantic level, interval and ID.
 * @example Two overlapping detail widths occupy lanes 0 and 1; a segment width
 * then starts at lane 2 and an overall width starts farther out at lane 3.
 * @since 0.1.0
 * @modified 2026-09-21 - Added collision-aware hierarchical paper layout.
 */
export function layoutPaperSpaceDimensions(
  view: DrawingView,
  annotations: readonly DrawingLinearDimensionAnnotation[],
  options: PaperDimensionLayoutOptions = {}
): readonly PaperDimensionPlacement[] {
  assertFiniteNumber(view.scaleDenominator, `${view.viewId}.scaleDenominator`);
  if (view.scaleDenominator <= 0) {
    throw new Error(`${view.viewId}.scaleDenominator must be greater than zero.`);
  }
  const firstOffsetPaperMm = options.firstOffsetPaperMm ?? 10;
  const laneGapPaperMm = options.laneGapPaperMm ?? 8;
  const minimumLabelGapPaperMm = options.minimumLabelGapPaperMm ?? 2;
  for (const [field, value] of [
    ["firstOffsetPaperMm", firstOffsetPaperMm],
    ["laneGapPaperMm", laneGapPaperMm],
    ["minimumLabelGapPaperMm", minimumLabelGapPaperMm]
  ] as const) {
    assertFiniteNumber(value, field);
    if (value < 0 || (field !== "minimumLabelGapPaperMm" && value === 0)) {
      throw new Error(`${field} must be ${field === "minimumLabelGapPaperMm" ? "non-negative" : "greater than zero"}.`);
    }
  }
  const seenIds = new Set<string>();
  const prepared = annotations.map((annotation) => {
    if (!annotation.annotationId || seenIds.has(annotation.annotationId)) {
      throw new Error(`Drawing annotation IDs must be non-empty and unique: ${annotation.annotationId}.`);
    }
    seenIds.add(annotation.annotationId);
    return prepareDimension(view, annotation, minimumLabelGapPaperMm);
  });
  const placements: PaperDimensionPlacement[] = [];
  const sides: readonly DrawingDimensionSide[] = ["top", "right", "bottom", "left"];
  const levels: readonly DrawingDimensionLevel[] = ["detail", "segment", "overall"];
  for (const side of sides) {
    let reservedLaneCount = 0;
    for (const level of levels) {
      const band = prepared.filter((item) =>
        item.annotation.side === side && item.annotation.level === level
      ).sort((left, right) =>
        left.collisionStartPaperMm - right.collisionStartPaperMm ||
        left.collisionEndPaperMm - right.collisionEndPaperMm ||
        left.annotation.priority - right.annotation.priority ||
        left.annotation.annotationId.localeCompare(right.annotation.annotationId)
      );
      const laneEnds: number[] = [];
      for (const item of band) {
        let localLane = laneEnds.findIndex((end) => end <= item.collisionStartPaperMm + 0.001);
        if (localLane < 0) localLane = laneEnds.length;
        laneEnds[localLane] = item.collisionEndPaperMm;
        const laneIndex = reservedLaneCount + localLane;
        const offsetFromViewPaperMm = firstOffsetPaperMm + laneIndex * laneGapPaperMm;
        const lineCoordinatePaperMm = side === "top"
          ? view.framePaperMm.y - offsetFromViewPaperMm
          : side === "bottom"
            ? view.framePaperMm.y + view.framePaperMm.height + offsetFromViewPaperMm
            : side === "left"
              ? view.framePaperMm.x - offsetFromViewPaperMm
              : view.framePaperMm.x + view.framePaperMm.width + offsetFromViewPaperMm;
        placements.push({
          annotationId: item.annotation.annotationId,
          side,
          level,
          laneIndex,
          offsetFromViewPaperMm,
          lineCoordinatePaperMm,
          intervalStartPaperMm: item.intervalStartPaperMm,
          intervalEndPaperMm: item.intervalEndPaperMm
        });
      }
      reservedLaneCount += laneEnds.length;
    }
  }
  return placements.sort((left, right) =>
    sides.indexOf(left.side) - sides.indexOf(right.side) ||
    DIMENSION_LEVEL_ORDER[left.level] - DIMENSION_LEVEL_ORDER[right.level] ||
    left.intervalStartPaperMm - right.intervalStartPaperMm ||
    left.annotationId.localeCompare(right.annotationId)
  );
}

/**
 * Validates and snapshots a technical drawing sheet.
 *
 * Algorithm: verify stable sheet/view/annotation IDs, ensure every annotation
 * references an existing view, validate page placement and linear-dimension
 * semantics, then clone arrays so callers cannot later append hidden content.
 *
 * @example Factory output can snapshot one elevation view and its material
 * callouts before an SVG/PDF adapter serializes the sheet.
 * @since 0.1.0
 * @modified 2026-09-21 - Added the initial immutable sheet constructor.
 */
export function createDrawingSheet(
  input: Omit<DrawingSheet, "schemaVersion">
): DrawingSheet {
  if (!input.sheetId || !input.drawingNumber || !input.drawingVersion ||
    !input.sourceDocumentId || !input.title) {
    throw new Error(
      "Drawing sheet identity, drawing number/version, source document and title are required."
    );
  }
  if (!Number.isInteger(input.sourceRevision) || input.sourceRevision < 0) {
    throw new Error("Drawing sheet sourceRevision must be a non-negative integer.");
  }
  if ((input.pageNumber === undefined) !== (input.pageCount === undefined) ||
    (input.pageNumber !== undefined && (!Number.isInteger(input.pageNumber) ||
      !Number.isInteger(input.pageCount) || input.pageNumber < 1 || input.pageCount! < 1 ||
      input.pageNumber > input.pageCount!))) {
    throw new Error("Drawing sheet pageNumber/pageCount must be a valid one-based pair.");
  }
  const paperSize = resolveDrawingPaperSizeMm(input.paperFormat, input.orientation);
  const viewIds = new Set<string>();
  for (const view of input.views) {
    if (!view.viewId || viewIds.has(view.viewId)) {
      throw new Error(`Drawing view IDs must be non-empty and unique: ${view.viewId}.`);
    }
    viewIds.add(view.viewId);
    if (view.scaleDenominator <= 0 || !Number.isFinite(view.scaleDenominator)) {
      throw new Error(`${view.viewId}.scaleDenominator must be greater than zero.`);
    }
    for (const [field, value] of Object.entries(view.framePaperMm)) {
      assertFiniteNumber(value, `${view.viewId}.framePaperMm.${field}`);
    }
    for (const [field, value] of Object.entries(view.modelBoundsMm)) {
      assertFiniteNumber(value, `${view.viewId}.modelBoundsMm.${field}`);
    }
    if (view.framePaperMm.width <= 0 || view.framePaperMm.height <= 0) {
      throw new Error(`${view.viewId}.framePaperMm must have positive width and height.`);
    }
    if (view.modelBoundsMm.width <= 0 || view.modelBoundsMm.height <= 0) {
      throw new Error(`${view.viewId}.modelBoundsMm must have positive width and height.`);
    }
    if (view.framePaperMm.x < 0 || view.framePaperMm.y < 0 ||
      view.framePaperMm.x + view.framePaperMm.width > paperSize.width ||
      view.framePaperMm.y + view.framePaperMm.height > paperSize.height) {
      throw new Error(`${view.viewId}.framePaperMm must fit within the selected paper.`);
    }
    const expectedProjection: DrawingProjectionKind = input.profile === "factory"
      ? "factory-sheet"
      : "installation-sheet";
    if (view.projection !== expectedProjection) {
      throw new Error(`${view.viewId}.projection must be ${expectedProjection} for this sheet profile.`);
    }
  }
  const annotationIds = new Set<string>();
  for (const annotation of input.annotations) {
    if (!annotation.annotationId || annotationIds.has(annotation.annotationId)) {
      throw new Error(`Drawing annotation IDs must be non-empty and unique: ${annotation.annotationId}.`);
    }
    annotationIds.add(annotation.annotationId);
    if (!viewIds.has(annotation.viewId)) {
      throw new Error(`Annotation ${annotation.annotationId} references unknown view ${annotation.viewId}.`);
    }
    if (annotation.kind === "linear-dimension") {
      const view = input.views.find((candidate) => candidate.viewId === annotation.viewId)!;
      validateLinearDimension(view, annotation);
    }
  }
  const tableIds = new Set<string>();
  const tableRowIds = new Set<string>();
  for (const table of input.tables ?? []) {
    if (!table.tableId || tableIds.has(table.tableId)) {
      throw new Error(`Drawing table IDs must be non-empty and unique: ${table.tableId}.`);
    }
    tableIds.add(table.tableId);
    if (!table.title || table.columns.length === 0) {
      throw new Error(`Drawing table ${table.tableId} requires a title and at least one column.`);
    }
    for (const [field, value] of Object.entries(table.layout)) {
      assertFiniteNumber(value, `${table.tableId}.layout.${field}`);
      if (value <= 0) {
        throw new Error(`Drawing table ${table.tableId} layout values must be greater than zero.`);
      }
    }
    for (const [field, value] of Object.entries(table.framePaperMm)) {
      assertFiniteNumber(value, `${table.tableId}.framePaperMm.${field}`);
    }
    if (table.framePaperMm.width <= 0 || table.framePaperMm.height <= 0 ||
      table.framePaperMm.x < 0 || table.framePaperMm.y < 0 ||
      table.framePaperMm.x + table.framePaperMm.width > paperSize.width ||
      table.framePaperMm.y + table.framePaperMm.height > paperSize.height) {
      throw new Error(`Drawing table ${table.tableId} must fit within the selected paper.`);
    }
    const columnKeys = new Set<string>();
    let columnWidthPaperMm = 0;
    for (const column of table.columns) {
      if (!column.key || !column.label || columnKeys.has(column.key)) {
        throw new Error(`Drawing table ${table.tableId} column keys must be non-empty and unique.`);
      }
      columnKeys.add(column.key);
      assertFiniteNumber(column.widthPaperMm, `${table.tableId}.${column.key}.widthPaperMm`);
      if (column.widthPaperMm <= 0) {
        throw new Error(`Drawing table ${table.tableId} column widths must be greater than zero.`);
      }
      columnWidthPaperMm += column.widthPaperMm;
    }
    if (columnWidthPaperMm > table.framePaperMm.width + 0.001) {
      throw new Error(`Drawing table ${table.tableId} columns exceed its paper frame.`);
    }
    const requiredTableHeightPaperMm = table.layout.titleHeightPaperMm +
      table.layout.headerHeightPaperMm + table.rows.length * table.layout.rowHeightPaperMm;
    if (requiredTableHeightPaperMm > table.framePaperMm.height + 0.001) {
      throw new Error(`Drawing table ${table.tableId} rows exceed its paper frame.`);
    }
    for (const row of table.rows) {
      if (!row.rowId || tableRowIds.has(row.rowId)) {
        throw new Error(`Drawing table row IDs must be non-empty and globally unique: ${row.rowId}.`);
      }
      tableRowIds.add(row.rowId);
      if (row.cells.length !== table.columns.length) {
        throw new Error(
          `Drawing table row ${row.rowId} must contain ${table.columns.length} cells.`
        );
      }
    }
  }
  const views = input.views.map((view) => Object.freeze({
    ...view,
    sourceObjectIds: Object.freeze([...view.sourceObjectIds]),
    modelBoundsMm: Object.freeze({ ...view.modelBoundsMm }),
    framePaperMm: Object.freeze({ ...view.framePaperMm }),
    primitives: view.primitives === undefined
      ? undefined
      : Object.freeze(view.primitives.map((primitive) => Object.freeze({
          ...primitive,
          sourceObjectIds: Object.freeze([...primitive.sourceObjectIds]),
          ...(primitive.kind === "rectangle"
            ? { boundsModelMm: Object.freeze({ ...primitive.boundsModelMm }) }
            : primitive.kind === "line"
              ? {
                  startModelMm: Object.freeze({ ...primitive.startModelMm }),
                  endModelMm: Object.freeze({ ...primitive.endModelMm })
                }
              : {
                  pointsModelMm: Object.freeze(
                    primitive.pointsModelMm.map((point) => Object.freeze({ ...point }))
                  )
                })
        })))
  }));
  const annotations = input.annotations.map((annotation) => Object.freeze({
    ...annotation,
    sourceObjectIds: Object.freeze([...annotation.sourceObjectIds]),
    ...(annotation.kind === "linear-dimension"
      ? {}
      : {
          anchorModelMm: Object.freeze({ ...annotation.anchorModelMm }),
          ...(annotation.labelOffsetPaperMm
            ? { labelOffsetPaperMm: Object.freeze({ ...annotation.labelOffsetPaperMm }) }
            : {})
        })
  }));
  const tables = input.tables?.map((table) => Object.freeze({
    ...table,
    framePaperMm: Object.freeze({ ...table.framePaperMm }),
    layout: Object.freeze({ ...table.layout }),
    columns: Object.freeze(table.columns.map((column) => Object.freeze({ ...column }))),
    rows: Object.freeze(table.rows.map((row) => Object.freeze({
      ...row,
      sourceObjectIds: Object.freeze([...row.sourceObjectIds]),
      cells: Object.freeze([...row.cells])
    })))
  }));
  return Object.freeze({
    ...input,
    schemaVersion: "doormes-drawing-sheet.v1",
    views: Object.freeze(views),
    annotations: Object.freeze(annotations),
    ...(tables ? { tables: Object.freeze(tables) } : {})
  });
}
