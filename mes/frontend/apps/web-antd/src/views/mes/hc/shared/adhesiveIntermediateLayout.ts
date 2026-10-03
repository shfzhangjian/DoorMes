import type { RoughDetailColumn } from './roughGrindingFormLayout';

export const ADHESIVE_INTERMEDIATE_HEADER_FIELDS = [
  'modelCode', 'materialCode', 'batchNo', 'processLength', 'productionDate',
  'productWidthMm', 'widthStart', 'widthMiddle', 'widthEnd',
  'recorder', 'recorderTime', 'confirmer', 'confirmerTime',
  'fillInstructions', 'revisionInfo', 'copyrightNotice',
];

export const ADHESIVE_INTERMEDIATE_NOTES = [
  { key: 'fillInstructions', label: '填写要求' },
  { key: 'revisionInfo', label: '修订信息' },
  { key: 'copyrightNotice', label: '版权声明' },
];

export function isAdhesiveIntermediateTemplate(form?: { processCode?: string; formCode?: string; schemaJson?: string }) {
  if (form?.processCode !== 'ADHESIVE') return false;
  let schema: Record<string, unknown> = {};
  try { const parsed = JSON.parse(form.schemaJson || '{}'); schema = parsed && typeof parsed === 'object' ? parsed : {}; } catch { /* 使用模板编码兼容旧配置 */ }
  return form.formCode?.includes('ADHESIVE1_INTERMEDIATE_RECORD') === true
    || schema.presetTemplate === 'adhesive1-intermediate-v1'
    || schema.adhesive1FormType === 'INTERMEDIATE_RECORD';
}

export function resolveAdhesiveIntermediateColumns(labels?: unknown): RoughDetailColumn[] {
  const values = Array.isArray(labels) ? labels : [];
  const title = (index: number, fallback: string) =>
    typeof values[index] === 'string' && values[index].trim() ? values[index].trim() : fallback;
  return [
    { key: 'lengthMark', bindField: 'lengthMark', title: '长度/m', width: 120 },
    { key: 'leftThickness', bindField: 'leftThickness', title: title(0, '收卷左侧10cm含纸厚度mm（XXmm）'), width: 260 },
    { key: 'rightThickness', bindField: 'rightThickness', title: title(1, '收卷右侧10cm含纸厚度mm（XXmm）'), width: 260 },
    { key: 'remark', bindField: 'remark', title: '备注', width: 260 },
  ];
}

export function getAdhesiveIntermediateRowCount(length?: number) {
  return Number.isFinite(length) && Number(length) > 0 ? Math.min(600, Math.ceil(Number(length))) : 50;
}

/** 静态说明按短行输出，避免 Excel 合并单元格的固定行高截断长说明。 */
export function buildAdhesiveIntermediateFooterNotes(notes?: Record<string, string>) {
  return ADHESIVE_INTERMEDIATE_NOTES.flatMap(({ key, label }) => {
    const text = notes?.[key]?.trim();
    if (!text) return [];
    return `${label}：${text}`.split(/\r?\n/).flatMap((line) => {
      const chars = Array.from(line);
      return Array.from({ length: Math.ceil(chars.length / 60) }, (_, index) => chars.slice(index * 60, (index + 1) * 60).join(''));
    });
  });
}
