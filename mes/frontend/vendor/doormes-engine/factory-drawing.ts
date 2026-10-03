import type { DesignDocument } from '@doormes/contracts';
import { projectFactoryDrawingSheets, type FactoryDrawingDesignComponent, type FactoryDrawingDesignComponentSnapshot } from '@doormes/drawing-projection';
import { createDrawingSheet, resolveDrawingPaperSizeMm, type DrawingTableRow } from '@doormes/drawing-model';
import { renderTechnicalDrawingSheetSvg } from '@doormes/renderer-drawing-svg';
import { parseFormalDesignDocument } from '@doormes/persistence/local-design';
import { deriveDrawingBom, type DrawingBomLine } from './bom';

export interface FactoryDrawingSubject {
  id: string;
  mark: string;
  kind: 'window' | 'assembly';
  defaultSelected: boolean;
}

export interface FactoryDrawingPage {
  id: string;
  title: string;
  kind: 'drawing' | 'components';
  subjectId: string;
  subjectMark: string;
  pageNumber: number;
  pageCount: number;
  svg: string;
  widthMm: number;
  heightMm: number;
}

export interface FactoryDrawingPreview {
  drawingNumber: string;
  name: string;
  revision: number;
  revisionLabel: string;
  subjects: FactoryDrawingSubject[];
  pages: FactoryDrawingPage[];
  warnings: string[];
}

export interface FactoryDrawingPreviewOptions {
  drawingNumber: string;
  name: string;
  /** Exact saved MES business revision, independent of document.revision. */
  revision: number;
  remark?: string;
  /** Omitted selects each assembly and all independent windows exactly once. */
  subjectIds?: readonly string[];
}

function subjectsFor(document: DesignDocument): FactoryDrawingSubject[] {
  const assemblies = document.assemblies ?? [];
  const assembled = new Set(assemblies.flatMap((assembly) => assembly.instances.map((instance) => instance.windowId)));
  return [
    ...assemblies.map((assembly) => ({ id: String(assembly.objectId), mark: assembly.mark, kind: 'assembly' as const, defaultSelected: true })),
    ...document.windows.map((window) => ({ id: String(window.objectId), mark: window.mark, kind: 'window' as const, defaultSelected: !assembled.has(window.objectId) }))
  ];
}

export function listFactoryDrawingSubjects(input: unknown): FactoryDrawingSubject[] {
  return subjectsFor(parseFormalDesignDocument(input));
}

const number = (value: number) => String(Math.round(value * 1000) / 1000);
function dimensions(line: DrawingBomLine): string {
  const d = line.dimensions;
  const quantity = `${number(line.quantity)} ${line.unit === 'pcs' ? '件' : line.unit === 'set' ? '套' : 'm'}`;
  if (d.basis === 'design-envelope') return `包络 ${number(d.envelopeWidthMm ?? 0)}×${number(d.envelopeHeightMm ?? 0)}（非下料）\n${quantity}`;
  if (d.basis === 'unspecified') return `加工尺寸未提供\n${quantity}`;
  const values: string[] = [];
  if (d.widthMm !== undefined || d.heightMm !== undefined) values.push(`参考 ${[d.widthMm, d.heightMm, d.thicknessMm].filter((v): v is number => v !== undefined).map(number).join('×')}`);
  else if (d.lengthMm !== undefined) values.push(`参考长 ${number(d.lengthMm)}`);
  if (d.grossLengthMm && d.grossLengthMm !== d.lengthMm) values.push(`参考毛坯 ${number(d.grossLengthMm)}`);
  if (d.cutLeftDeg !== undefined && d.cutRightDeg !== undefined && (d.cutLeftDeg || d.cutRightDeg)) values.push(`参考端角 ${number(d.cutLeftDeg)}/${number(d.cutRightDeg)}°`);
  return `${values.join('；') || '加工尺寸未提供'}\n${quantity}`;
}

/** Bound text in physical paper space, preserving all characters across continuation rows. */
function wrap(value: string, widthMm: number): string[] {
  const lines: string[] = []; let line = '', used = 0;
  for (const character of value.replaceAll('\r\n', '\n').replaceAll('\r', '\n')) {
    const width = character.charCodeAt(0) < 128 ? 1.8 : 3;
    if (character === '\n' || used + width > widthMm - 2.4) {
      lines.push(line); line = ''; used = 0;
      if (character === '\n') continue;
    }
    line += character; used += width;
  }
  lines.push(line); return lines;
}

const tableColumnWidths = [19, 14, 18, 26, 23].map((ratio) => 392 * ratio / 100);
function paginatedRow(row: DrawingTableRow): DrawingTableRow[] {
  const cells = row.cells.map((cell, index) => wrap(cell, tableColumnWidths[index]!));
  const count = Math.max(1, ...cells.map((cell) => Math.ceil(cell.length / 3)));
  return Array.from({ length: count }, (_, index) => ({
    rowId: index ? `${row.rowId}:text-continuation:${index}` : row.rowId,
    sourceObjectIds: row.sourceObjectIds,
    cells: cells.map((cell, column) => {
      const value = cell.slice(index * 3, index * 3 + 3).join('\n');
      // Repeat only the short reference on note continuations; never repeat demand quantity.
      if (index && !value && column === 0) return row.cells[0]!;
      if (index && !value && column === 1) return `文字续行 ${index}/${count - 1}`;
      return value;
    })
  }));
}

function componentSnapshot(document: DesignDocument, lines: readonly DrawingBomLine[], remark: string): FactoryDrawingDesignComponentSnapshot {
  const continuationRows: Record<string, DrawingTableRow[]> = {};
  const components: FactoryDrawingDesignComponent[] = lines.map((line) => {
    const assembly = document.assemblies?.find((candidate) => candidate.joints.some((joint) => line.sourceObjectIds.includes(joint.objectId)));
    const joint = assembly?.joints.find((candidate) => line.sourceObjectIds.includes(candidate.objectId));
    const uuid = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
    const model = uuid.test(line.modelCode) ? '型号未映射' : line.modelCode || '型号未提供';
    const rows = paginatedRow({ rowId: line.objectId, sourceObjectIds: line.sourceObjectIds,
      cells: [line.displayCode, line.name, [model, line.specification].filter(Boolean).join('\n'), dimensions(line), line.remark] });
    if (rows.length > 1) continuationRows[line.objectId] = rows.slice(1);
    return { row: rows[0]!, sourceWindowId: line.sourceWindowId, sourceComponentId: line.sourceComponentId,
      category: line.category, ...(assembly && joint ? { assemblyId: assembly.objectId, jointId: joint.objectId } : {}) };
  });
  const preambleRows = remark ? paginatedRow({ rowId: `drawing-note:${document.designId}`, sourceObjectIds: [document.designId],
    cells: ['', '图纸备注', '', '', remark] }) : [];
  return { sourceRevision: document.revision, components, continuationRows, preambleRows };
}

/** Pure saved-document projection. It never mounts a canvas or changes product geometry. */
export function createFactoryDrawingPreview(input: unknown, options: FactoryDrawingPreviewOptions): FactoryDrawingPreview {
  const document = parseFormalDesignDocument(input);
  if (!options || typeof options.drawingNumber !== 'string' || !options.drawingNumber.trim() || options.drawingNumber.length > 60 ||
    typeof options.name !== 'string' || !options.name.trim() || options.name.length > 200 ||
    !Number.isSafeInteger(options.revision) || options.revision < 1 ||
    (options.remark !== undefined && (typeof options.remark !== 'string' || options.remark.length > 2000))) {
    throw new Error('图号、名称或保存版本无效，无法生成工厂图。');
  }
  const subjects = subjectsFor(document);
  const subjectIds = options.subjectIds ?? subjects.filter((subject) => subject.defaultSelected).map((subject) => subject.id);
  if (!Array.isArray(subjectIds) || !subjectIds.length || new Set(subjectIds).size !== subjectIds.length ||
    subjectIds.some((id) => !subjects.some((subject) => subject.id === id))) throw new Error('请选择当前图纸内有效且不重复的门窗或组合件。');
  const drawingNumber = options.drawingNumber.trim(), name = options.name.trim(), revisionLabel = `R${options.revision}`;
  const bom = deriveDrawingBom(document);
  const snapshot = componentSnapshot(document, bom.lines, options.remark?.trim() ?? '');
  const pending: { subject: FactoryDrawingSubject; sheet: ReturnType<typeof createDrawingSheet> }[] = [];
  for (const subjectId of subjectIds) {
    const subject = subjects.find((item) => item.id === subjectId)!;
    const sheets = projectFactoryDrawingSheets(document, subjectId, { paperFormat: 'A3', orientation: 'landscape',
      sheetId: `mes:${document.designId}:${revisionLabel}:${subjectId}:factory`,
      drawingNumber, drawingVersion: revisionLabel, designComponentSnapshot: snapshot });
    for (const projected of sheets) {
      const { schemaVersion: _schemaVersion, ...sheetInput } = projected;
      const title = `${name} · ${projected.title.replace('工厂总装图', '设计参考图')}`;
      const sheet = createDrawingSheet({ ...sheetInput, title });
      pending.push({ subject, sheet });
    }
  }
  const pages: FactoryDrawingPage[] = pending.map(({ subject, sheet: original }, index) => {
    const sheet = { ...original, pageNumber: index + 1, pageCount: pending.length };
    const paper = resolveDrawingPaperSizeMm(sheet.paperFormat, sheet.orientation);
    return { id: sheet.sheetId, title: sheet.title, kind: sheet.tables?.length ? 'components' : 'drawing',
      subjectId: subject.id, subjectMark: subject.mark, pageNumber: index + 1, pageCount: pending.length,
      svg: renderTechnicalDrawingSheetSvg(sheet, { purposeLabel: '设计参考', sourceRevisionLabel: `业务版本 ${revisionLabel} · mm` }),
      widthMm: paper.width, heightMm: paper.height };
  });
  return { drawingNumber, name, revision: options.revision, revisionLabel, subjects, pages,
    warnings: ['图中尺寸与组成件参数供设计核对，尚未经过正式截面、扣减及加工校核。',
      ...(bom.lines.some((line) => line.dimensions.basis !== 'reference-rule') ? ['部分构件仅提供设计包络或型号，未提供加工尺寸。'] : [])] };
}
