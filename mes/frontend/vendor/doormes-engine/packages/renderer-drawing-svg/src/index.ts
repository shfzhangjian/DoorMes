import {
  layoutPaperSpaceDimensions,
  resolveDrawingPaperSizeMm,
  type DrawingAnnotation,
  type DrawingLinearDimensionAnnotation,
  type DrawingPointMm,
  type DrawingPrimitive,
  type DrawingSheet,
  type DrawingTable,
  type DrawingView,
  type PaperDimensionPlacement
} from "@doormes/drawing-model";

/**
 * Escapes user/catalog text before it enters a technical SVG document.
 * @example `A&B` becomes `A&amp;B` inside a title block or material note.
 * @since 0.1.0
 * @modified 2026-09-21 - Added safe technical-drawing serialization.
 */
function escapeXml(value: string): string {
  return value
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&apos;");
}

/**
 * Formats paper coordinates without browser-dependent floating-point noise.
 * @example `12.3400001` becomes `12.34`.
 * @since 0.1.0
 * @modified 2026-09-21 - Added deterministic SVG numeric output.
 */
function format(value: number): string {
  return String(Math.round(value * 1000) / 1000);
}

/**
 * Maps one model-millimetre point into its placed technical-sheet view.
 * @example Model x=1200 at 1:20 maps 60 paper millimetres from the view origin.
 * @since 0.1.0
 * @modified 2026-09-21 - Added shared model-to-paper transform.
 */
function mapPoint(view: DrawingView, point: DrawingPointMm): DrawingPointMm {
  return {
    x: view.framePaperMm.x + (point.x - view.modelBoundsMm.x) / view.scaleDenominator,
    y: view.framePaperMm.y + (point.y - view.modelBoundsMm.y) / view.scaleDenominator
  };
}

/** Technical line weights in physical paper millimetres. */
const LAYER_STROKE_WIDTH_MM: Readonly<Record<DrawingPrimitive["layer"], number>> = {
  outline: 0.7,
  frame: 0.5,
  sash: 0.35,
  glass: 0.18,
  mullion: 0.5,
  joint: 0.5,
  symbol: 0.25
};

/**
 * Serializes one semantic primitive into black-and-white technical SVG linework.
 *
 * Geometry is transformed from model millimetres using the view scale; material
 * colors and PBR textures are intentionally ignored. Source IDs remain in data
 * attributes for traceability and future selection/issue markup.
 *
 * @since 0.1.0
 * @modified 2026-09-21 - Added rectangle, line and polyline sheet primitives.
 */
function renderPrimitive(view: DrawingView, primitive: DrawingPrimitive): string {
  const sourceIds = escapeXml(primitive.sourceObjectIds.join(" "));
  const lineStyle = primitive.lineStyle ?? (primitive.layer === "symbol" ? "dashed" : "solid");
  const dash = lineStyle === "dashed"
    ? ' stroke-dasharray="2 1"'
    : lineStyle === "center"
      ? ' stroke-dasharray="6 1.5 1.2 1.5"'
      : "";
  const common = `class="technical-primitive technical-primitive--${primitive.layer}" data-primitive-id="${escapeXml(primitive.primitiveId)}" data-source-object-ids="${sourceIds}" data-line-style="${lineStyle}" fill="none" stroke="#111827" stroke-width="${format(LAYER_STROKE_WIDTH_MM[primitive.layer])}" vector-effect="non-scaling-stroke"${dash}`;
  if (primitive.kind === "rectangle") {
    const topLeft = mapPoint(view, {
      x: primitive.boundsModelMm.x,
      y: primitive.boundsModelMm.y
    });
    const width = primitive.boundsModelMm.width / view.scaleDenominator;
    const height = primitive.boundsModelMm.height / view.scaleDenominator;
    return `<rect ${common} x="${format(topLeft.x)}" y="${format(topLeft.y)}" width="${format(width)}" height="${format(height)}" />`;
  }
  if (primitive.kind === "line") {
    const start = mapPoint(view, primitive.startModelMm);
    const end = mapPoint(view, primitive.endModelMm);
    return `<line ${common} x1="${format(start.x)}" y1="${format(start.y)}" x2="${format(end.x)}" y2="${format(end.y)}" />`;
  }
  const points = primitive.pointsModelMm.map((point) => {
    const mapped = mapPoint(view, point);
    return `${format(mapped.x)},${format(mapped.y)}`;
  }).join(" ");
  return `<polyline ${common} points="${points}"${primitive.closed ? ' data-closed="true"' : ""} />`;
}

/**
 * Returns the paper point at which a dimension witness touches its source feature.
 * @example A top horizontal cell dimension starts its witness at the cell-top Y coordinate.
 * @since 0.1.0
 * @modified 2026-09-21 - Added feature-aware witness origins.
 */
function resolveWitnessPoint(
  view: DrawingView,
  annotation: DrawingLinearDimensionAnnotation,
  axisValueModelMm: number
): DrawingPointMm {
  const fallbackPerpendicular = annotation.axis === "horizontal"
    ? annotation.side === "top"
      ? view.modelBoundsMm.y
      : view.modelBoundsMm.y + view.modelBoundsMm.height
    : annotation.side === "left"
      ? view.modelBoundsMm.x
      : view.modelBoundsMm.x + view.modelBoundsMm.width;
  const perpendicular = annotation.witnessOriginModelMm ?? fallbackPerpendicular;
  return annotation.axis === "horizontal"
    ? mapPoint(view, { x: axisValueModelMm, y: perpendicular })
    : mapPoint(view, { x: perpendicular, y: axisValueModelMm });
}

/**
 * Serializes one laid-out engineering dimension in physical paper coordinates.
 *
 * The adapter consumes the semantic annotation and layout placement directly;
 * it does not measure SVG text or infer meaning from the localized label.
 * @since 0.1.0
 * @modified 2026-09-21 - Added witness lines, ticks and hierarchy metadata.
 */
function renderDimension(
  view: DrawingView,
  annotation: DrawingLinearDimensionAnnotation,
  placement: PaperDimensionPlacement
): string {
  const startWitness = resolveWitnessPoint(view, annotation, annotation.startModelMm);
  const endWitness = resolveWitnessPoint(view, annotation, annotation.endModelMm);
  const coordinate = placement.lineCoordinatePaperMm;
  const start = annotation.axis === "horizontal"
    ? { x: placement.intervalStartPaperMm, y: coordinate }
    : { x: coordinate, y: placement.intervalStartPaperMm };
  const end = annotation.axis === "horizontal"
    ? { x: placement.intervalEndPaperMm, y: coordinate }
    : { x: coordinate, y: placement.intervalEndPaperMm };
  const firstWitnessEnd = annotation.axis === "horizontal"
    ? { x: start.x, y: coordinate }
    : { x: coordinate, y: start.y };
  const secondWitnessEnd = annotation.axis === "horizontal"
    ? { x: end.x, y: coordinate }
    : { x: coordinate, y: end.y };
  const middle = { x: (start.x + end.x) / 2, y: (start.y + end.y) / 2 };
  const labelWidth = annotation.labelWidthPaperMm ?? Math.max(10, annotation.label.length * 2.1 + 5);
  const label = escapeXml(annotation.label);
  const text = annotation.axis === "horizontal"
    ? `<rect x="${format(middle.x - labelWidth / 2)}" y="${format(middle.y - 2.5)}" width="${format(labelWidth)}" height="5" fill="white" /><text x="${format(middle.x)}" y="${format(middle.y + 1)}" text-anchor="middle">${label}</text>`
    : `<g transform="translate(${format(middle.x)} ${format(middle.y)}) rotate(-90)"><rect x="${format(-labelWidth / 2)}" y="-2.5" width="${format(labelWidth)}" height="5" fill="white" /><text x="0" y="1" text-anchor="middle">${label}</text></g>`;
  return `<g class="technical-dimension technical-dimension--${annotation.level}" data-annotation-id="${escapeXml(annotation.annotationId)}" data-source-object-ids="${escapeXml(annotation.sourceObjectIds.join(" "))}" data-side="${annotation.side}" data-level="${annotation.level}" data-lane-index="${placement.laneIndex}" fill="none" stroke="#334155" stroke-width="0.18" font-family="Arial, 'Microsoft YaHei', sans-serif" font-size="3" font-weight="500" vector-effect="non-scaling-stroke"><line x1="${format(startWitness.x)}" y1="${format(startWitness.y)}" x2="${format(firstWitnessEnd.x)}" y2="${format(firstWitnessEnd.y)}" /><line x1="${format(endWitness.x)}" y1="${format(endWitness.y)}" x2="${format(secondWitnessEnd.x)}" y2="${format(secondWitnessEnd.y)}" /><line x1="${format(start.x)}" y1="${format(start.y)}" x2="${format(end.x)}" y2="${format(end.y)}" /><line x1="${format(start.x - 1.2)}" y1="${format(start.y + 1.2)}" x2="${format(start.x + 1.2)}" y2="${format(start.y - 1.2)}" /><line x1="${format(end.x - 1.2)}" y1="${format(end.y + 1.2)}" x2="${format(end.x + 1.2)}" y2="${format(end.y - 1.2)}" />${text}</g>`;
}

/**
 * Renders non-dimensional semantic notes and callouts.
 * @example A material callout prints the reviewed profile and glass codes near its anchor.
 * @since 0.1.0
 * @modified 2026-09-21 - Added initial material/note text output.
 */
function renderReferenceAnnotation(view: DrawingView, annotation: Exclude<
  DrawingAnnotation,
  DrawingLinearDimensionAnnotation
>): string {
  if (!annotation.text) return "";
  const anchor = mapPoint(view, annotation.anchorModelMm);
  if (annotation.kind === "note" || annotation.kind === "opening-symbol") {
    return `<g class="technical-callout technical-callout--${annotation.kind}" data-annotation-id="${escapeXml(annotation.annotationId)}" data-source-object-ids="${escapeXml(annotation.sourceObjectIds.join(" "))}" font-family="Arial, 'Microsoft YaHei', sans-serif" font-size="3.2" font-weight="500"><text x="${format(anchor.x)}" y="${format(anchor.y)}" text-anchor="middle">${escapeXml(annotation.text)}</text></g>`;
  }
  const offset = annotation.labelOffsetPaperMm ?? { x: 8, y: -8 };
  const labelPoint = { x: anchor.x + offset.x, y: anchor.y + offset.y };
  const pointsLeft = offset.x < 0;
  const textAnchor = pointsLeft ? "end" : "start";
  const textX = labelPoint.x + (pointsLeft ? -1 : 1);
  const lines = wrapTitle(annotation.text, 30, 3.2).split('\n');
  const labelWidth = Math.min(30, Math.max(8, annotation.text.length * 1.85 + 3));
  const extraHeight = (lines.length - 1) * 3.84;
  const textY = labelPoint.y - extraHeight / 2;
  const content = lines.length === 1 ? escapeXml(annotation.text) : lines.map((line, index) =>
    `<tspan x="${format(textX)}" dy="${index ? '3.84' : '0'}">${escapeXml(line)}</tspan>`).join('');
  const rectX = pointsLeft ? textX - labelWidth - 1 : textX - 1;
  const leaderEndX = pointsLeft ? labelPoint.x + 0.8 : labelPoint.x - 0.8;
  return `<g class="technical-callout technical-callout--${annotation.kind}" data-annotation-id="${escapeXml(annotation.annotationId)}" data-source-object-ids="${escapeXml(annotation.sourceObjectIds.join(" "))}" data-label-offset-x="${format(offset.x)}" data-label-offset-y="${format(offset.y)}" font-family="Arial, 'Microsoft YaHei', sans-serif" font-size="3.2"><title>${escapeXml(annotation.text)}</title><line x1="${format(anchor.x)}" y1="${format(anchor.y)}" x2="${format(leaderEndX)}" y2="${format(labelPoint.y)}" stroke="#334155" stroke-width="0.18" /><rect x="${format(rectX)}" y="${format(labelPoint.y - 3.8 - extraHeight / 2)}" width="${format(labelWidth + 2)}" height="${format(5.4 + extraHeight)}" rx="0.7" fill="white" stroke="#94a3b8" stroke-width="0.15" /><text x="${format(textX)}" y="${format(textY)}" text-anchor="${textAnchor}" font-weight="600">${content}</text></g>`;
}

/**
 * Renders one cell through a nested clipping viewport while preserving full text.
 *
 * The visible text cannot overwrite neighbouring columns. A `<title>` retains
 * the complete value for browser inspection and future evidence extraction;
 * no data is shortened in the drawing model.
 *
 * @since 0.2.0
 * @modified 2026-09-21 - Added deterministic schedule cell clipping.
 */
function renderTableCell(
  value: string,
  x: number,
  y: number,
  width: number,
  height: number,
  align: "left" | "center" | "right",
  fontSize: number,
  fontWeight = "normal"
): string {
  const padding = 1.2;
  const anchor = align === "center" ? "middle" : align === "right" ? "end" : "start";
  const textX = align === "center" ? width / 2 : align === "right" ? width - padding : padding;
  const lines = value.split('\n');
  const textY = height / 2 + fontSize * 0.34 - (lines.length - 1) * fontSize * 0.6;
  const content = lines.length === 1 ? escapeXml(value) : lines.map((line, index) =>
    `<tspan x="${format(textX)}" dy="${index ? format(fontSize * 1.2) : '0'}">${escapeXml(line)}</tspan>`).join('');
  return `<svg x="${format(x)}" y="${format(y)}" width="${format(width)}" height="${format(height)}" overflow="hidden"><title>${escapeXml(value)}</title><text x="${format(textX)}" y="${format(textY)}" text-anchor="${anchor}" font-size="${format(fontSize)}" font-weight="${fontWeight}" stroke="none">${content}</text></svg>`;
}

function wrapTitle(value: string, widthMm: number, fontSizeMm: number): string {
  const lines: string[] = []; let line = '', used = 0;
  for (const character of value) {
    const width = (character.charCodeAt(0) < 128 ? 0.6 : 1) * fontSizeMm;
    if (character === '\n' || used + width > widthMm - 2.4) {
      lines.push(line); line = ''; used = 0;
      if (character === '\n') continue;
    }
    line += character; used += width;
  }
  lines.push(line); return lines.join('\n');
}

export interface TechnicalDrawingSvgOptions {
  readonly purposeLabel?: string;
  /** Presentation label only; data-source-revision still preserves the true engine revision. */
  readonly sourceRevisionLabel?: string;
}

/**
 * Serializes one semantic schedule with traceable rows in physical paper space.
 *
 * Design selections, formal MBOM demand, connection routing and diagnostics
 * keep distinct `data-table-kind` values. Warning/error rows use monochrome
 * shading so printed sheets remain meaningful without colour.
 *
 * @example A material row exposes its MBOM and design IDs in
 * `data-source-object-ids` while displaying code/specification/quantity.
 * @since 0.2.0
 * @modified 2026-09-21 - Added production-information table rendering.
 */
function renderTable(table: DrawingTable): string {
  const frame = table.framePaperMm;
  const layout = table.layout;
  let cursorX = frame.x;
  const columnStarts = table.columns.map((column) => {
    const start = cursorX;
    cursorX += column.widthPaperMm;
    return start;
  });
  const headerY = frame.y + layout.titleHeightPaperMm;
  const bodyY = headerY + layout.headerHeightPaperMm;
  const verticals = table.columns.slice(0, -1).map((column, index) => {
    const x = columnStarts[index]! + column.widthPaperMm;
    return `<line x1="${format(x)}" y1="${format(headerY)}" x2="${format(x)}" y2="${format(frame.y + frame.height)}" />`;
  }).join("");
  const headers = table.columns.map((column, index) =>
    renderTableCell(
      column.label,
      columnStarts[index]!,
      headerY,
      column.widthPaperMm,
      layout.headerHeightPaperMm,
      column.align ?? "center",
      layout.headerFontSizePaperMm,
      "normal"
    )
  ).join("");
  const rows = table.rows.map((row, rowIndex) => {
    const y = bodyY + rowIndex * layout.rowHeightPaperMm;
    const fill = row.severity === "error"
      ? "#d1d5db"
      : row.severity === "warning"
        ? "#f1f5f9"
        : "white";
    const cells = row.cells.map((cell, columnIndex) => {
      const column = table.columns[columnIndex]!;
      return renderTableCell(
        cell,
        columnStarts[columnIndex]!,
        y,
        column.widthPaperMm,
        layout.rowHeightPaperMm,
        column.align ?? "left",
        layout.bodyFontSizePaperMm
      );
    }).join("");
    return `<g class="technical-table-row" data-row-id="${escapeXml(row.rowId)}" data-source-object-ids="${escapeXml(row.sourceObjectIds.join(" "))}"${row.severity ? ` data-severity="${row.severity}"` : ""}><rect x="${format(frame.x)}" y="${format(y)}" width="${format(frame.width)}" height="${format(layout.rowHeightPaperMm)}" fill="${fill}" />${cells}<line x1="${format(frame.x)}" y1="${format(y + layout.rowHeightPaperMm)}" x2="${format(frame.x + frame.width)}" y2="${format(y + layout.rowHeightPaperMm)}" /></g>`;
  }).join("");
  return `<g class="technical-table technical-table--${table.kind}" data-table-id="${escapeXml(table.tableId)}" data-table-kind="${table.kind}" font-family="Arial, 'Microsoft YaHei', sans-serif" fill="#111827" stroke="#111827" stroke-width="0.18"><rect x="${format(frame.x)}" y="${format(frame.y)}" width="${format(frame.width)}" height="${format(frame.height)}" fill="white" /><rect x="${format(frame.x)}" y="${format(frame.y)}" width="${format(frame.width)}" height="${format(layout.titleHeightPaperMm)}" fill="#e5e7eb" />${renderTableCell(table.title, frame.x, frame.y, frame.width, layout.titleHeightPaperMm, "left", layout.titleFontSizePaperMm, "normal")}<rect x="${format(frame.x)}" y="${format(headerY)}" width="${format(frame.width)}" height="${format(layout.headerHeightPaperMm)}" fill="#f8fafc" />${headers}${rows}${verticals}<rect x="${format(frame.x)}" y="${format(frame.y)}" width="${format(frame.width)}" height="${format(frame.height)}" fill="none" stroke-width="0.35" /><line x1="${format(frame.x)}" y1="${format(headerY)}" x2="${format(frame.x + frame.width)}" y2="${format(headerY)}" /><line x1="${format(frame.x)}" y1="${format(bodyY)}" x2="${format(frame.x + frame.width)}" y2="${format(bodyY)}" /></g>`;
}

/**
 * Serializes one immutable technical sheet as printable vector SVG.
 *
 * Algorithm: size the root in physical millimetres, render semantic linework
 * per view, lay out dimensions using the shared paper-space engine, render
 * reviewed callouts, then add a deterministic title block containing the
 * source revision, sheet ID, profile and scale. The function never consumes
 * interactive SVG output or PBR color.
 *
 * @param sheet Validated drawing snapshot from `createDrawingSheet` or a projector.
 * @returns Standalone SVG text suitable for browser print or later PDF conversion.
 * @example An A3 landscape factory sheet has viewBox `0 0 420 297`.
 * @since 0.1.0
 * @modified 2026-09-21 - Added SHEET-002 first black-and-white vector adapter.
 */
export function renderTechnicalDrawingSheetSvg(sheet: DrawingSheet, options: TechnicalDrawingSvgOptions = {}): string {
  const page = resolveDrawingPaperSizeMm(sheet.paperFormat, sheet.orientation);
  const viewMarkup = sheet.views.map((view) => {
    const annotations = sheet.annotations.filter((annotation) => annotation.viewId === view.viewId);
    const linear = annotations.filter(
      (annotation): annotation is DrawingLinearDimensionAnnotation =>
        annotation.kind === "linear-dimension"
    );
    const placements = layoutPaperSpaceDimensions(view, linear);
    const placementById = new Map(placements.map((placement) => [
      placement.annotationId,
      placement
    ]));
    const primitives = (view.primitives ?? []).map((primitive) =>
      renderPrimitive(view, primitive)
    ).join("");
    const dimensions = linear.map((annotation) => {
      const placement = placementById.get(annotation.annotationId);
      if (!placement) throw new Error(`Dimension ${annotation.annotationId} was not laid out.`);
      return renderDimension(view, annotation, placement);
    }).join("");
    const references = annotations.filter(
      (annotation): annotation is Exclude<DrawingAnnotation, DrawingLinearDimensionAnnotation> =>
        annotation.kind !== "linear-dimension"
    ).map((annotation) => renderReferenceAnnotation(view, annotation)).join("");
    return `<g class="technical-view technical-view--${view.viewKind}" data-view-id="${escapeXml(view.viewId)}" data-scale="1:${format(view.scaleDenominator)}" data-source-object-ids="${escapeXml(view.sourceObjectIds.join(" "))}">${primitives}${dimensions}${references}</g>`;
  }).join("");
  const tableMarkup = (sheet.tables ?? []).map((table) => renderTable(table)).join("");
  const titleY = page.height - 35;
  const titleWidth = page.width - 20;
  const primaryScale = sheet.views[0]?.scaleDenominator;
  const pageLabel = sheet.pageNumber && sheet.pageCount
    ? ` · 第 ${sheet.pageNumber}/${sheet.pageCount} 页`
    : "";
  const titleCellWidth = page.width - 138;
  const titleFontSize = sheet.title.length > 55 ? 3.2 : 4.2;
  const titleCell = renderTableCell(wrapTitle(sheet.title, titleCellWidth, titleFontSize), 14, titleY + 1, titleCellWidth, 14, 'left', titleFontSize);
  const issueCell = renderTableCell(wrapTitle(`图号 ${sheet.drawingNumber}　版本 ${sheet.drawingVersion}${pageLabel}`, titleCellWidth, 2.8), 14, titleY + 15, titleCellWidth, 9, 'left', 2.8);
  const titleBlock = `<g class="technical-title-block" font-family="Arial, 'Microsoft YaHei', sans-serif" fill="#111827" stroke="#111827" stroke-width="0.25"><rect x="10" y="${format(titleY)}" width="${format(titleWidth)}" height="25" fill="white" /><line x1="${format(page.width - 120)}" y1="${format(titleY)}" x2="${format(page.width - 120)}" y2="${format(titleY + 25)}" /><line x1="${format(page.width - 60)}" y1="${format(titleY)}" x2="${format(page.width - 60)}" y2="${format(titleY + 25)}" />${titleCell}${issueCell}<text x="${format(page.width - 115)}" y="${format(titleY + 10)}" font-size="3" stroke="none">比例 ${primaryScale ? `1:${format(primaryScale)}` : "—"}</text><text x="${format(page.width - 115)}" y="${format(titleY + 18)}" font-size="3" stroke="none">图幅 ${sheet.paperFormat} ${sheet.orientation === "landscape" ? "横向" : "纵向"}</text><text x="${format(page.width - 55)}" y="${format(titleY + 10)}" font-size="3" stroke="none">用途 ${escapeXml(options.purposeLabel ?? (sheet.profile === "factory" ? "工厂" : "安装"))}</text><text x="${format(page.width - 55)}" y="${format(titleY + 18)}" font-size="3" stroke="none">${escapeXml(options.sourceRevisionLabel ?? `设计修订 r${sheet.sourceRevision} · mm`)}</text></g>`;
  const pageAttributes = sheet.pageNumber && sheet.pageCount
    ? ` data-page-number="${sheet.pageNumber}" data-page-count="${sheet.pageCount}"`
    : "";
  return `<svg xmlns="http://www.w3.org/2000/svg" class="doormes-technical-sheet" data-sheet-id="${escapeXml(sheet.sheetId)}" data-drawing-number="${escapeXml(sheet.drawingNumber)}" data-drawing-version="${escapeXml(sheet.drawingVersion)}" data-source-revision="${sheet.sourceRevision}"${pageAttributes} width="${format(page.width)}mm" height="${format(page.height)}mm" viewBox="0 0 ${format(page.width)} ${format(page.height)}"><rect class="technical-page" x="0" y="0" width="${format(page.width)}" height="${format(page.height)}" fill="white" /><rect class="technical-border" x="10" y="10" width="${format(page.width - 20)}" height="${format(page.height - 20)}" fill="none" stroke="#111827" stroke-width="0.5" />${viewMarkup}${tableMarkup}${titleBlock}</svg>`;
}
