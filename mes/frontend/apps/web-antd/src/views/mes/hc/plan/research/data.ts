import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesHcResearchTaskApi } from '#/api/mes/hc/researchtask';

export const PRODUCT_CLASS_OPTIONS = [
  { label: 'RD CMP黑垫', value: 'RD' },
  { label: 'RA CMP黑垫', value: 'RA' },
];

export const FORMULA_CODE_OPTIONS = [
  { label: '33', value: '33' },
  { label: '21', value: '21' },
  { label: '12', value: '12' },
];

export const SINGLE_PROCESS_CODE_OPTIONS = Array.from({ length: 9 }, (_, index) => {
  const value = String(index + 1);
  return { label: value, value };
});

export const POST_PROCESS_CODE_OPTIONS = Array.from({ length: 19 }, (_, index) => {
  const value = String(index + 1).padStart(2, '0');
  return { label: value, value };
});

export const TASK_STATUS_OPTIONS = [
  { label: '草稿', value: 'DRAFT' },
  { label: '已确认', value: 'CONFIRMED' },
  { label: '已归档', value: 'ARCHIVED' },
  { label: '已取消', value: 'CANCELED' },
];

export const TASK_STATUS_META: Record<
  string,
  { color: string; label: string; tone: string }
> = {
  ARCHIVED: { color: 'blue', label: '已归档', tone: 'var(--color-blue-500)' },
  CANCELED: { color: 'error', label: '已取消', tone: 'var(--color-red-500)' },
  CONFIRMED: { color: 'success', label: '已确认', tone: 'var(--color-green-600)' },
  DRAFT: { color: 'default', label: '草稿', tone: 'var(--color-gray-500)' },
};

export function getTaskStatusMeta(status?: string) {
  return TASK_STATUS_META[String(status || '').toUpperCase()] || TASK_STATUS_META.DRAFT;
}

export function formatNumber(value?: number | string | null, fractionDigits = 2) {
  if (value == null || value === '') return '-';
  return Number(value).toFixed(fractionDigits);
}

export function formatLocalDate(value?: unknown) {
  if (!value) return '-';
  if (typeof value === 'number') {
    const date = new Date(value);
    if (!Number.isNaN(date.getTime())) {
      return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
    }
    return '-';
  }
  if (Array.isArray(value)) {
    const [year, month, day] = value;
    if (year && month && day) {
      return `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
    }
    return '-';
  }
  const text = String(value);
  const matched = text.match(/^(\d{4})[-/](\d{1,2})[-/](\d{1,2})/);
  if (matched) {
    return `${matched[1]}-${matched[2]!.padStart(2, '0')}-${matched[3]!.padStart(2, '0')}`;
  }
  return text;
}

export function useGridColumns(): VxeTableGridOptions<MesHcResearchTaskApi.ResearchTask>['columns'] {
  return [
    { type: 'checkbox', width: 48, fixed: 'left' },
    { field: 'taskNo', title: '研发任务号', minWidth: 190, fixed: 'left', slots: { default: 'taskNo' } },
    { field: 'taskStatus', title: '状态', width: 100, align: 'center', slots: { default: 'taskStatus' } },
    { field: 'rdModelCode', title: '研发型号', minWidth: 150, slots: { default: 'rdModelCode' } },
    { field: 'productClassName', title: '型号类型', width: 110 },
    { field: 'baseFormulaCode', title: '基准配方', width: 110 },
    { field: 'wetProcessCode', title: '湿法', width: 90, align: 'center' },
    { field: 'grindingProcessCode', title: '磨皮', width: 90, align: 'center' },
    { field: 'postProcessCode', title: '后工艺', width: 90, align: 'center' },
    { field: 'reuseSeq', title: '重复序号', width: 100, align: 'center', slots: { default: 'reuseSeq' } },
    { field: 'routeCode', title: '工艺路线', minWidth: 150 },
    {
      field: 'researchDate',
      title: '研发日期',
      width: 120,
      align: 'center',
      formatter: ({ cellValue }) => formatLocalDate(cellValue),
    },
    { field: 'targetQty', title: '目标量', width: 120, align: 'right', slots: { default: 'targetQty' } },
    { field: 'archivedModelCode', title: '归档型号', minWidth: 150, formatter: ({ row }) => row.archivedModelCode || '-' },
    {
      field: 'createTime',
      title: '创建时间',
      width: 120,
      align: 'center',
      formatter: ({ cellValue }) => formatLocalDate(cellValue),
    },
    { title: '操作', width: 260, fixed: 'right', slots: { default: 'actions' } },
  ];
}
