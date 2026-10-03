import type { VbenFormSchema } from '#/adapter/form';
import type { VxeGridProps } from '#/adapter/vxe-table';

import { getRangePickerDefaultProps } from '#/utils';

export const processCategoryOptions = [
  { label: '湿法', value: 'WET' },
  { label: '磨皮', value: 'GRINDING' },
  { label: '粘胶1', value: 'GLUE_1' },
  { label: '压槽', value: 'GROOVING' },
  { label: '粘胶2', value: 'GLUE_2' },
  { label: '成品', value: 'FINAL' },
];

type FqcOperationDisplayRecord = {
  operationName?: string;
  processCategory?: string;
  sourceOperationName?: string;
};

export function resolveFqcOperationLabel(
  record?: FqcOperationDisplayRecord | null,
) {
  if (!record) return '-';
  return (
    record.operationName ||
    record.sourceOperationName ||
    processCategoryOptions.find((item) => item.value === record.processCategory)
      ?.label ||
    '-'
  );
}

type ReportQtyDisplayRecord = {
  produceQty?: number | string | null;
};

function formatQuantityValue(value: number | string) {
  const numericValue = Number(value);
  if (!Number.isFinite(numericValue)) {
    return String(value);
  }
  return new Intl.NumberFormat('zh-CN', {
    maximumFractionDigits: 6,
  }).format(numericValue);
}

export function formatReportQty(record?: ReportQtyDisplayRecord | null) {
  const quantity = record?.produceQty;
  if (quantity === undefined || quantity === null || quantity === '') {
    return '-';
  }
  return formatQuantityValue(quantity);
}

export const submissionTypeOptions = [
  { label: '研发', value: 'RND' },
  { label: '客户验证', value: 'CUSTOMER_VALIDATION' },
  { label: '量产发货', value: 'MASS_SHIPMENT' },
];

export const triggerReasonOptions = [
  { label: '新单开机', value: 'NEW_ORDER' },
  { label: '换班交接', value: 'SHIFT_CHANGE' },
  { label: '换模/换料', value: 'TOOL_CHANGE' },
  { label: '参数调整', value: 'PARAMETER_CHANGE' },
  { label: '处置复检', value: 'REWORK_RECHECK' },
];

export const FqcStatusOptions = [
  { label: '待检测', value: 'PENDING' },
  { label: '自检中', value: 'INSPECTING' },
  { label: '待审核', value: 'WAITING_QA' },
  { label: '已挂起', value: 'SUSPENDED' },
  { label: '处置中', value: 'REWORKING' },
  { label: '已完成', value: 'COMPLETED' },
  { label: '不合格', value: 'REJECTED' },
  { label: '已取消', value: 'CANCELED' },
];

const FQC_STATUS_SORT_FALLBACK = 999;
const fqcStatusSortOrder = new Map(
  FqcStatusOptions.map((item, index) => [item.value, index]),
);

export function sortFqcRecordsByStatus<T extends { status?: string }>(
  records: T[],
) {
  return records
    .map((record, index) => ({ index, record }))
    .toSorted((left, right) => {
      const leftOrder =
        fqcStatusSortOrder.get(left.record.status || '') ??
        FQC_STATUS_SORT_FALLBACK;
      const rightOrder =
        fqcStatusSortOrder.get(right.record.status || '') ??
        FQC_STATUS_SORT_FALLBACK;
      return leftOrder - rightOrder || left.index - right.index;
    })
    .map(({ record }) => record);
}

export const FqcJudgmentOptions = [
  { label: '待判定', value: '-' },
  { label: '待判定', value: 'PENDING' },
  { label: '合格', value: 'OK' },
  { label: '不合格', value: 'NG' },
];

export const recheckFlagOptions = [
  { label: '复检单', value: true },
  { label: '非复检单', value: false },
];

export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      fieldName: 'fqcNo',
      label: 'FQC单号',
      component: 'Input',
      componentProps: { placeholder: '输入单号' },
    },
    {
      fieldName: 'processCategory',
      label: '工序',
      component: 'Select',
      componentProps: { options: processCategoryOptions, allowClear: true },
    },
    {
      fieldName: 'submissionType',
      label: '送检类型',
      component: 'Select',
      componentProps: { options: submissionTypeOptions, allowClear: true },
    },
    {
      fieldName: 'materialCode',
      label: '物料编码',
      component: 'Input',
      componentProps: { placeholder: '输入物料编码' },
    },
    {
      fieldName: 'productModel',
      label: '产品型号',
      component: 'Input',
      componentProps: { placeholder: '输入产品型号' },
    },
    {
      fieldName: 'productBatchNo',
      label: '产品批次',
      component: 'Input',
      componentProps: { placeholder: '输入产品批次' },
    },
    {
      fieldName: 'status',
      label: '单据状态',
      component: 'Select',
      componentProps: { options: FqcStatusOptions, allowClear: true },
    },
    {
      fieldName: 'judgment',
      label: '判定结果',
      component: 'Select',
      componentProps: { options: FqcJudgmentOptions, allowClear: true },
    },
    {
      fieldName: 'recheckFlag',
      label: '复检标记',
      component: 'Select',
      componentProps: { options: recheckFlagOptions, allowClear: true },
    },
    {
      fieldName: 'submissionTime',
      label: '送检时间',
      component: 'RangePicker',
      componentProps: { ...getRangePickerDefaultProps() },
    },
    {
      fieldName: 'submitterName',
      label: '送检人员',
      component: 'Input',
      componentProps: { placeholder: '输入送检人员' },
    },
  ];
}

export function useGridColumns(): VxeGridProps['columns'] {
  return [
    { type: 'seq', width: 50, align: 'center', fixed: 'left' },
    {
      field: 'fqcNo',
      title: 'FQC单号',
      width: 160,
      fixed: 'left',
      slots: { default: 'fqcNo' },
    },
    {
      field: 'processCategory',
      title: '工序',
      width: 100,
      align: 'center',
      slots: { default: 'processCategory' },
    },
    {
      field: 'submissionType',
      title: '送检类型',
      width: 110,
      align: 'center',
      slots: { default: 'submissionType' },
    },
    { field: 'materialCode', title: '物料编码', minWidth: 140 },
    { field: 'productModel', title: '产品型号', minWidth: 140 },
    { field: 'productBatchNo', title: '产品批次', minWidth: 130 },
    {
      field: 'produceQty',
      title: '报检数量',
      width: 120,
      align: 'right',
      formatter: ({ row }) => formatReportQty(row),
    },
    {
      field: 'status',
      title: '单据状态',
      width: 120,
      align: 'center',
      slots: { default: 'inspectionStatus' },
    },
    {
      field: 'judgment',
      title: '判定结果',
      width: 110,
      align: 'center',
      slots: { default: 'qualifiedStatus' },
    },
    {
      field: 'recheckFlag',
      title: '复检',
      width: 100,
      align: 'center',
      slots: { default: 'recheckFlag' },
    },
    { field: 'originalInspectionNo', title: '原检验单号', minWidth: 150 },
    { field: 'submissionTime', title: '送检时间', width: 160, align: 'center' },
    { field: 'submitterName', title: '送检人员', width: 100, align: 'center' },
    { field: 'gluePlateBatchNo', title: '胶板批次', minWidth: 130 },
    { field: 'remark', title: '备注', minWidth: 180 },
    {
      title: '操作',
      field: 'action',
      fixed: 'right',
      width: 260,
      align: 'center',
      slots: { default: 'actions' },
    },
  ];
}
