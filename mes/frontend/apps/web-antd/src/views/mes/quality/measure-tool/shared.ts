import type { QmsMeasureToolApi } from '#/api/mes/quality/measure-tool';

export const TOOL_STATUS_OPTIONS = [
  { color: 'success', label: '正在使用', value: 'IN_USE' },
  { color: 'default', label: '闲置', value: 'IDLE' },
  { color: 'processing', label: '校准中', value: 'CALIBRATING' },
  { color: 'warning', label: '维修', value: 'REPAIRING' },
  { color: 'warning', label: '停用', value: 'STOPPED' },
  { color: 'warning', label: '报废', value: 'SCRAPPED' },
];

export const WARNING_STATUS_OPTIONS = [
  { color: 'success', label: '正常', value: 'NORMAL' },
  { color: 'warning', label: '即将到期', value: 'DUE_SOON' },
  { color: 'error', label: '已超期', value: 'OVERDUE' },
  { color: 'default', label: '未校准', value: 'NOT_CALIBRATED' },
  { color: 'default', label: '不适用', value: 'NOT_REQUIRED' },
];

export const APPLY_STATUS_OPTIONS = [
  { color: 'default', label: '草稿', value: 'DRAFT' },
  { color: 'processing', label: '审批中', value: 'APPROVING' },
  { color: 'success', label: '已通过', value: 'APPROVED' },
  { color: 'error', label: '已驳回', value: 'REJECTED' },
  { color: 'default', label: '已取消', value: 'CANCELLED' },
];

export const TASK_STATUS_OPTIONS = [
  { color: 'warning', label: '待执行', value: 'PENDING' },
  { color: 'processing', label: '待确认', value: 'IN_PROGRESS' },
  { color: 'success', label: '已完成', value: 'COMPLETED' },
  { color: 'error', label: '已逾期', value: 'OVERDUE' },
  { color: 'default', label: '已取消', value: 'CANCELLED' },
];

export const CALIBRATION_RESULT_OPTIONS = [
  { color: 'success', label: '合格', value: 'QUALIFIED' },
  { color: 'error', label: '不合格', value: 'UNQUALIFIED' },
  { color: 'warning', label: '限用', value: 'LIMITED' },
];

export const CALIBRATION_TYPE_OPTIONS = [
  { color: 'processing', label: '内校', value: 'INTERNAL' },
  { color: 'default', label: '外校', value: 'EXTERNAL' },
];

export const MSA_RESULT_OPTIONS = [
  { color: 'success', label: '合格', value: 'QUALIFIED' },
  { color: 'error', label: '不合格', value: 'UNQUALIFIED' },
];

export const RECORD_SOURCE_OPTIONS = [
  { color: 'processing', label: '预警任务完成', value: 'TASK' },
  { color: 'default', label: '人工新建', value: 'MANUAL' },
  { color: 'default', label: '历史导入', value: 'IMPORT' },
];

export const CATEGORY_STATUS_OPTIONS = [
  { color: 'success', label: '启用', value: 1 },
  { color: 'error', label: '停用', value: 0 },
];

export function optionLabel(
  options: Array<{ label: string; value: number | string }>,
  value?: number | string,
) {
  return options.find((item) => item.value === value)?.label || value || '-';
}

export function optionColor(
  options: Array<{ color: string; value: number | string }>,
  value?: number | string,
) {
  return options.find((item) => item.value === value)?.color || 'default';
}

export function formatCycleMonths(value?: number | string) {
  const months = Number(value);
  if (!Number.isFinite(months) || months <= 0) {
    return '-';
  }
  if (months % 12 === 0) {
    return `${months / 12}年（${months}个月）`;
  }
  if (months % 3 === 0) {
    return `${months / 3}季（${months}个月）`;
  }
  return `${months}个月`;
}

export function mapCategoryOptions(rows: QmsMeasureToolApi.Category[] = []) {
  return (rows || [])
    .filter((item) => item.status !== 0)
    .map((item) => ({
      label: item.categoryName || item.categoryCode,
      value: item.id,
    }));
}

export async function loadCategoryOptions(
  loader: () => Promise<QmsMeasureToolApi.Category[]>,
) {
  return mapCategoryOptions(await loader());
}
