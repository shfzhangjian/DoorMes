import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesHcPlanOrderApi } from '#/api/mes/hc/planorder';

export const PLAN_MODE_OPTIONS = [
  { label: '按库生产 MTS', value: 'MTS' },
  { label: '按单生产 MTO', value: 'MTO' },
];

export const SOURCE_TYPE_OPTIONS = [
  { label: '销售订单', value: 'SALES_ORDER' },
  { label: '手工创建', value: 'MANUAL' },
];

export const PLAN_STATUS_OPTIONS = [
  { label: '草稿', value: 'DRAFT' },
  { label: '下达', value: 'RELEASED' },
  { label: '暂停', value: 'PAUSED' },
  { label: '关闭', value: 'CLOSED' },
  { label: '作废取消', value: 'CANCELLED' },
];

export const OPERATION_STATUS_OPTIONS = [
  { label: '未下达', value: 'NOT_RELEASED' },
  { label: '执行中', value: 'RUNNING' },
  { label: '暂停', value: 'PAUSED' },
  { label: '完工', value: 'FINISHED' },
  { label: '取消', value: 'CANCELLED' },
];

export const LOCK_TYPE_OPTIONS = [
  { label: '成品抵扣', value: 'FG' },
  { label: '在制锁定', value: 'WIP' },
];

export const LOCK_STATUS_OPTIONS = [
  { label: '有效', value: 'ACTIVE' },
  { label: '已释放', value: 'RELEASED' },
  { label: '已取消', value: 'CANCELLED' },
];

export const PLAN_STATUS_META: Record<
  string,
  { color: string; label: string; tone: string }
> = {
  CLOSED: { color: 'blue', label: '关闭', tone: 'var(--color-blue-500)' },
  CANCELED: { color: 'error', label: '作废取消', tone: 'var(--color-red-500)' },
  CANCELLED: { color: 'error', label: '作废取消', tone: 'var(--color-red-500)' },
  DRAFT: { color: 'default', label: '草稿', tone: 'var(--color-gray-500)' },
  PAUSED: { color: 'warning', label: '暂停', tone: 'var(--color-orange-500)' },
  RELEASED: { color: 'success', label: '下达', tone: 'var(--color-green-600)' },
};

export function getPlanStatusMeta(status?: string) {
  return PLAN_STATUS_META[String(status || '').toUpperCase()] || PLAN_STATUS_META.DRAFT;
}

export function formatNumber(value?: number | string | null, fractionDigits = 2) {
  if (value == null || value === '') {
    return '-';
  }
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
  if (/^\d{13}$/.test(text)) {
    return formatLocalDate(Number(text));
  }
  const matched = text.match(/^(\d{4})[-/](\d{1,2})[-/](\d{1,2})/);
  if (matched) {
    return `${matched[1]}-${matched[2]!.padStart(2, '0')}-${matched[3]!.padStart(2, '0')}`;
  }
  return text;
}

export function useGridFormSchema(showAdvanced = false): VbenFormSchema[] {
  const baseSchema: VbenFormSchema[] = [
    {
      fieldName: 'planNo',
      label: '计划号',
      component: 'Input',
      componentProps: { placeholder: '请输入计划号' },
    },
    {
      fieldName: 'modelCode',
      label: '产品型号',
      component: 'Input',
      componentProps: { placeholder: '请输入产品型号' },
    },
    {
      fieldName: 'productionEndDateEnd',
      label: '计划结束止',
      component: 'DatePicker',
      componentProps: {
        class: 'w-full',
        format: 'YYYY-MM-DD',
        valueFormat: 'YYYY-MM-DD',
        placeholder: '结束日期',
      },
    },
    {
      fieldName: 'materialKeyword',
      label: '产品料号',
      component: 'Input',
      componentProps: { placeholder: '编码或名称' },
    },
  ];

  const advancedSchema: VbenFormSchema[] = [
    {
      fieldName: 'productionStartDateStart',
      label: '计划开始起',
      component: 'DatePicker',
      componentProps: {
        class: 'w-full',
        format: 'YYYY-MM-DD',
        valueFormat: 'YYYY-MM-DD',
        placeholder: '开始日期',
      },
    },
    {
      fieldName: 'productionStartDateEnd',
      label: '计划开始止',
      component: 'DatePicker',
      componentProps: {
        class: 'w-full',
        format: 'YYYY-MM-DD',
        valueFormat: 'YYYY-MM-DD',
        placeholder: '开始日期止',
      },
    },
    {
      fieldName: 'productionEndDateStart',
      label: '计划结束起',
      component: 'DatePicker',
      componentProps: {
        class: 'w-full',
        format: 'YYYY-MM-DD',
        valueFormat: 'YYYY-MM-DD',
        placeholder: '结束日期起',
      },
    },
    {
      fieldName: 'planStatuses',
      label: '状态',
      component: 'Select',
      componentProps: {
        allowClear: true,
        mode: 'multiple',
        maxTagCount: 2,
        options: PLAN_STATUS_OPTIONS,
        placeholder: '请选择状态',
      },
    },
    {
      fieldName: 'sizeSpec',
      label: '尺寸规格',
      component: 'Input',
      componentProps: { placeholder: '请输入尺寸规格' },
    },
    {
      fieldName: 'prodType',
      label: '生产类型',
      component: 'Input',
      componentProps: { placeholder: '请输入生产类型编码' },
    },
    {
      fieldName: 'categoryCode',
      label: '物料类型',
      component: 'Input',
      componentProps: { placeholder: '请输入物料类型编码' },
    },
  ];

  return showAdvanced ? [...baseSchema, ...advancedSchema] : baseSchema;
}

export function useGridColumns(): VxeTableGridOptions<MesHcPlanOrderApi.PlanOrder>['columns'] {
  return [
    { type: 'checkbox', width: 48, fixed: 'left' },
    { field: 'planNo', title: '计划单号', minWidth: 170, fixed: 'left', slots: { default: 'planNo' } },
    { field: 'planStatus', title: '状态', width: 110, align: 'center', slots: { default: 'planStatus' } },
    { field: 'modelCode', title: '产品型号', minWidth: 140 },
    {
      field: 'productionBatchNo',
      title: '产品批号',
      minWidth: 140,
      formatter: ({ row }) => row.productionBatchNo || row.batchNo || '后加工',
    },
    { field: 'materialCode', title: '产品料号', minWidth: 150 },
    {
      field: 'sizeName',
      title: '尺寸规格',
      minWidth: 110,
      formatter: ({ row }) => row.sizeName || row.sizeSpec || '-',
    },
    { field: 'prodTypeName', title: '生产类型', minWidth: 120, formatter: ({ row }) => row.prodTypeName || row.prodType || '-' },
    { field: 'categoryName', title: '物料类型', minWidth: 120, formatter: ({ row }) => row.categoryName || row.categoryCode || '-' },
    {
      field: 'productionStartDate',
      title: '计划开始日期',
      width: 120,
      align: 'center',
      formatter: ({ cellValue }) => formatLocalDate(cellValue),
    },
    {
      field: 'productionEndDate',
      title: '计划结束日期',
      width: 120,
      align: 'center',
      formatter: ({ cellValue }) => formatLocalDate(cellValue),
    },
    { field: 'targetQty', title: '目标量', width: 140, align: 'right', slots: { default: 'targetQty' } },
    { field: 'creator', title: '创建人', width: 100, align: 'center', slots: { default: 'creator' } },
    {
      field: 'createTime',
      title: '创建时间',
      width: 120,
      align: 'center',
      formatter: ({ cellValue }) => formatLocalDate(cellValue),
    },
    { title: '操作', width: 360, fixed: 'right', slots: { default: 'actions' } },
  ];
}
