import { getStationFormSimpleList } from '#/api/mes/hc/stationform';
import type { MesHcProcessFormApi } from '#/api/mes/hc/processform';

export type RoughFormLayoutMode =
  | 'cleaning-check'
  | 'middle-product'
  | 'process-check'
  | 'startup-check';

export type RoughFormIdentity = {
  formCode?: string;
  formName?: string;
};

export type RoughDetailColumn = {
  bindField?: string;
  key: string;
  title: string;
  width?: number;
};

export type RoughMiddleProductColumnKey =
  | 'innerThickness'
  | 'lengthMeter'
  | 'outerThickness'
  | 'remark';

export type RoughMiddleProductHeaderField = {
  bindKey?: string;
  editable?: boolean;
  key: string;
  label: string;
  type?: 'input' | 'number' | 'text';
  value?: null | number | string;
};

export type RoughMiddleProductRowLike = Partial<Record<RoughMiddleProductColumnKey, null | number | string>>;

export const ROUGH_MIDDLE_PRODUCT_COLUMNS: Array<RoughDetailColumn & { key: RoughMiddleProductColumnKey }> = [
  { bindField: 'lengthMeter', key: 'lengthMeter', title: '长度/m', width: 120 },
  { bindField: 'innerThickness', key: 'innerThickness', title: '磨皮左侧10cm厚度(XXmm)', width: 230 },
  { bindField: 'outerThickness', key: 'outerThickness', title: '磨皮右侧10cm厚度(XXmm)', width: 230 },
  { bindField: 'remark', key: 'remark', title: '备注', width: 260 },
];

export function resolveRoughMiddleProductColumns(labels?: unknown) {
  const titles = Array.isArray(labels) ? labels : [];
  return ROUGH_MIDDLE_PRODUCT_COLUMNS.map((column) => {
    const index = column.key === 'innerThickness' ? 0 : column.key === 'outerThickness' ? 1 : -1;
    const title = index >= 0 && typeof titles[index] === 'string' ? titles[index].trim() : '';
    return { ...column, title: title || column.title };
  });
}

export async function loadRoughMiddleProductThicknessLabels(): Promise<string[]> {
  const forms = await getStationFormSimpleList('ROUGH_GRINDING');
  const form = forms.find((item) => item.formCode === 'ROUGH_MIDDLE_PRODUCT_RECORD');
  if (!form?.schemaJson) return [];
  try {
    const schema = JSON.parse(form.schemaJson);
    return Array.isArray(schema?.thicknessLabels)
      ? schema.thicknessLabels.slice(0, 2).map((label: unknown) => typeof label === 'string' ? label : '')
      : [];
  } catch {
    return [];
  }
}

export const ROUGH_MIDDLE_PRODUCT_SIGNATURE_KEYS = [
  'recorder',
  'recorderTime',
  'confirmer',
  'confirmerTime',
] as const;

export const ROUGH_DETAIL_LAYOUTS: Record<RoughFormLayoutMode, {
  detailTitle: string;
  displayName: string;
  excelSheetName: string;
  tableColumns: RoughDetailColumn[];
  visualMode: string;
}> = {
  'cleaning-check': {
    detailTitle: '明细项目',
    displayName: '磨皮设备清洁点检表',
    excelSheetName: '磨皮过站记录',
    tableColumns: [
      { key: 'itemCategory', title: '工序', width: 140 },
      { key: 'itemName', title: '点检项目', width: 220 },
      { key: 'standardText', title: '检查标准', width: 420 },
      { bindField: 'actualValue', key: 'actualValue', title: '实际/记录', width: 220 },
      { bindField: 'abnormalRemark', key: 'abnormalRemark', title: '备注', width: 260 },
    ],
    visualMode: 'rough-pass-work',
  },
  'middle-product': {
    detailTitle: '中间品记录明细',
    displayName: '磨皮中间品记录表',
    excelSheetName: '磨皮中间品记录',
    tableColumns: ROUGH_MIDDLE_PRODUCT_COLUMNS,
    visualMode: 'wet-semi',
  },
  'process-check': {
    detailTitle: '明细项目',
    displayName: '磨皮工艺参数点检表',
    excelSheetName: '磨皮过站记录',
    tableColumns: [
      { key: 'itemCategory', title: '物料/生产环节', width: 140 },
      { key: 'stepNode', title: '确认节点', width: 140 },
      { key: 'itemName', title: '点检项目', width: 220 },
      { key: 'standardText', title: '点检标准', width: 220 },
      { bindField: 'actualValue', key: 'actualValue', title: '实际/记录', width: 180 },
      { bindField: 'abnormalRemark', key: 'abnormalRemark', title: '异常备注', width: 240 },
    ],
    visualMode: 'rough-pass-work',
  },
  'startup-check': {
    detailTitle: '明细项目',
    displayName: '磨皮开机点检表',
    excelSheetName: '磨皮过站记录',
    tableColumns: [
      { key: 'itemSeq', title: '序号', width: 90 },
      { key: 'itemName', title: '点检项目', width: 260 },
      { key: 'standardText', title: '标准', width: 360 },
      { bindField: 'actualValue', key: 'actualValue', title: '实际/记录', width: 220 },
      { bindField: 'abnormalRemark', key: 'abnormalRemark', title: '备注', width: 260 },
    ],
    visualMode: 'rough-pass-work',
  },
};

export function stripRoughModelPrefix(name?: string) {
  const text = String(name || '').trim();
  if (!text) return '';
  const stripped = text.replace(/^.*?[（(][^）)]*[）)]\s*/u, '').trim();
  return stripped || text;
}

export function isRoughStartupForm(record?: RoughFormIdentity | null) {
  const code = String(record?.formCode || '').toUpperCase();
  const name = String(record?.formName || '');
  return code === 'ROUGH_STARTUP_CHECK' || name.includes('开机点检');
}

export function isRoughCleaningForm(record?: RoughFormIdentity | null) {
  const code = String(record?.formCode || '').toUpperCase();
  const name = String(record?.formName || '');
  return code === 'ROUGH_CLEANING_CHECK' || name.includes('清洁点检');
}

export function isRoughMiddleProductForm(record?: RoughFormIdentity | null) {
  const code = String(record?.formCode || '').toUpperCase();
  const name = String(record?.formName || '');
  return code === 'ROUGH_MIDDLE_PRODUCT_RECORD' || name.includes('中间品记录');
}

export function isRoughProcessCheckForm(record?: RoughFormIdentity | null) {
  const code = String(record?.formCode || '').toUpperCase();
  const name = String(record?.formName || '');
  return code.startsWith('ROUGH_PROCESS_CHECK') || name.includes('工艺参数点检');
}

export function getRoughFormLayoutMode(record?: RoughFormIdentity | null): RoughFormLayoutMode {
  if (isRoughCleaningForm(record)) return 'cleaning-check';
  if (isRoughMiddleProductForm(record)) return 'middle-product';
  if (isRoughProcessCheckForm(record)) return 'process-check';
  return 'startup-check';
}

export function getRoughRecordLayoutConfig(record?: RoughFormIdentity | null) {
  return ROUGH_DETAIL_LAYOUTS[getRoughFormLayoutMode(record)];
}

export function getRoughDisplayFormName(record?: RoughFormIdentity | null) {
  if (isRoughStartupForm(record)) return ROUGH_DETAIL_LAYOUTS['startup-check'].displayName;
  if (isRoughCleaningForm(record)) return ROUGH_DETAIL_LAYOUTS['cleaning-check'].displayName;
  if (isRoughMiddleProductForm(record)) return ROUGH_DETAIL_LAYOUTS['middle-product'].displayName;
  if (isRoughProcessCheckForm(record)) {
    const displayName = stripRoughModelPrefix(record?.formName);
    if (displayName.includes('一次磨皮') || displayName.includes('二次磨皮')) return displayName;
    return ROUGH_DETAIL_LAYOUTS['process-check'].displayName;
  }
  return stripRoughModelPrefix(record?.formName);
}

export function createRoughLayoutCell(
  colIndex: number,
  text?: null | number | string,
  options: Partial<MesHcProcessFormApi.LayoutCell> = {},
): MesHcProcessFormApi.LayoutCell {
  return {
    bindField: options.bindField,
    bindKey: options.bindKey,
    colIndex,
    colSpan: options.colSpan ?? 1,
    editable: options.editable ?? false,
    rowSpan: options.rowSpan ?? 1,
    text: String(text ?? ''),
  };
}

export function toRoughExcelColumns(columns: RoughDetailColumn[]): MesHcProcessFormApi.LayoutColumn[] {
  return columns.map((column) => ({
    title: column.title,
    width: column.width,
  }));
}

export function buildRoughMiddleProductExcelRows(
  rows: RoughMiddleProductRowLike[],
  editable = true,
): MesHcProcessFormApi.LayoutRow[] {
  return rows.map((row, index) => ({
    cells: ROUGH_MIDDLE_PRODUCT_COLUMNS.map((column, colIndex) =>
      createRoughLayoutCell(colIndex, row[column.key], {
        bindField: column.bindField || column.key,
        bindKey: String(index),
        editable,
      }),
    ),
  }));
}

export function sanitizeRoughExcelFileName(name?: string, fallback = '磨皮过站记录') {
  return `${String(name || fallback).trim() || fallback}.xlsx`.replace(/[\\/:*?"<>|]/gu, '_');
}

export function buildRoughMiddleProductExcelLayout(options: {
  thicknessLabels?: string[];
  editable?: boolean;
  filePrefix?: null | number | string;
  formName?: string;
  headerItems: MesHcProcessFormApi.LayoutHeaderItem[];
  rows: RoughMiddleProductRowLike[];
}): MesHcProcessFormApi.LayoutExcelReq {
  const formName = options.formName || ROUGH_DETAIL_LAYOUTS['middle-product'].displayName;
  const filePrefix = String(options.filePrefix || '磨皮');
  return {
    columns: toRoughExcelColumns(resolveRoughMiddleProductColumns(options.thicknessLabels)),
    detailTitle: ROUGH_DETAIL_LAYOUTS['middle-product'].detailTitle,
    fileName: sanitizeRoughExcelFileName(`${filePrefix}_${formName}`, '磨皮中间品记录表'),
    headerItems: options.headerItems,
    rows: buildRoughMiddleProductExcelRows(options.rows, options.editable ?? true),
    sheetName: ROUGH_DETAIL_LAYOUTS['middle-product'].excelSheetName,
    title: formName,
    visualMode: ROUGH_DETAIL_LAYOUTS['middle-product'].visualMode,
  };
}
