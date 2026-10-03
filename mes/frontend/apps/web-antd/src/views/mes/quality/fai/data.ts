import type { VbenFormSchema } from '#/adapter/form';
import type { VxeGridProps } from '#/adapter/vxe-table';

import { getRangePickerDefaultProps } from '#/utils';

export const processCategoryOptions = [
  { label: '湿法', value: 'WET' },
  { label: '磨皮', value: 'ROUGH_GRINDING' },
  { label: '粘胶1', value: 'ADHESIVE' },
  { label: '压槽', value: 'PRESS_SLOT' },
  { label: '粘胶2', value: 'ADHESIVE2' },
  { label: '成品', value: 'FINAL_PRODUCTS' },
];

const processCategoryLabelMap = new Map([
  ...processCategoryOptions.map((item) => [item.value, item.label] as const),
  ['GRINDING', '磨皮'],
  ['ADHESIVE1', '粘胶1'],
  ['GLUE_1', '粘胶1'],
  ['GROOVING', '压槽'],
  ['GLUE_2', '粘胶2'],
  ['FINAL', '成品'],
]);

type FaiOperationDisplayRecord = {
  operationName?: string;
  processCategory?: string;
  sourceOperationName?: string;
};

export function resolveFaiOperationLabel(
  record?: FaiOperationDisplayRecord | null,
) {
  if (!record) return '-';
  return (
    record.operationName ||
    record.sourceOperationName ||
    processCategoryLabelMap.get(
      String(record.processCategory || '').toUpperCase(),
    ) ||
    '-'
  );
}

type InspectionQtyDisplayRecord = {
  inspectionQty?: number | string | null;
};

type FaiQTimeDisplayRecord = {
  inspectionTime?: string | null;
  qTime?: string | null;
  submissionTime?: string | null;
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

export function formatInspectionQty(
  record?: InspectionQtyDisplayRecord | null,
) {
  const quantity = record?.inspectionQty;
  if (quantity === undefined || quantity === null || quantity === '') {
    return '1';
  }
  return formatQuantityValue(quantity);
}

function parseDateTimeMillis(value?: string | null) {
  if (!value) return undefined;
  const normalizedValue = value.trim().replace(' ', 'T');
  const millis = new Date(normalizedValue).getTime();
  return Number.isFinite(millis) ? millis : undefined;
}

function formatDurationMillis(durationMillis: number) {
  const totalSeconds = Math.floor(Math.abs(durationMillis) / 1000);
  const days = Math.floor(totalSeconds / 86_400);
  const hours = Math.floor((totalSeconds % 86_400) / 3600);
  const minutes = Math.floor((totalSeconds % 3600) / 60);
  const seconds = totalSeconds % 60;
  const timeText = [hours, minutes, seconds]
    .map((part) => String(part).padStart(2, '0'))
    .join(':');
  return `${durationMillis < 0 ? '-' : ''}${days > 0 ? `${days}天 ` : ''}${timeText}`;
}

export function formatFaiQTime(record?: FaiQTimeDisplayRecord | null) {
  if (!record) return '-';
  if (record.qTime) return record.qTime;
  const submissionMillis = parseDateTimeMillis(record.submissionTime);
  const inspectionMillis = parseDateTimeMillis(record.inspectionTime);
  if (submissionMillis === undefined || inspectionMillis === undefined) {
    return '-';
  }
  return formatDurationMillis(inspectionMillis - submissionMillis);
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
  { label: '调机复检', value: 'REWORK_RECHECK' },
];

export const faiStatusOptions = [
  { label: '待检测', value: 'PENDING' },
  { label: '自检中', value: 'INSPECTING' },
  { label: '待审核', value: 'WAITING_QA' },
  { label: '已挂起', value: 'SUSPENDED' },
  { label: '调机中', value: 'REWORKING' },
  { label: '已完成', value: 'COMPLETED' },
  { label: '不合格', value: 'REJECTED' },
  { label: '已取消', value: 'CANCELED' },
];

const FAI_STATUS_SORT_FALLBACK = 999;
const faiStatusSortOrder = new Map(
  faiStatusOptions.map((item, index) => [item.value, index]),
);

export function sortFaiRecordsByStatus<T extends { status?: string }>(
  records: T[],
) {
  return records
    .map((record, index) => ({ index, record }))
    .toSorted((left, right) => {
      const leftOrder =
        faiStatusSortOrder.get(left.record.status || '') ??
        FAI_STATUS_SORT_FALLBACK;
      const rightOrder =
        faiStatusSortOrder.get(right.record.status || '') ??
        FAI_STATUS_SORT_FALLBACK;
      return leftOrder - rightOrder || left.index - right.index;
    })
    .map(({ record }) => record);
}

export const faiJudgmentOptions = [
  { label: '待判定', value: '-' },
  { label: '待判定', value: 'PENDING' },
  { label: '合格', value: 'OK' },
  { label: '不合格', value: 'NG' },
];

export const retentionStatusOptions = [
  { label: '未确认', value: 'UNCONFIRMED' },
  { label: '已留样', value: 'RETAINED' },
  { label: '未留样', value: 'NOT_RETAINED' },
];

export const retentionDestroyStatusOptions = [
  { label: '待报废', value: 'WAIT_DESTROY' },
  { label: '已报废', value: 'DESTROYED' },
];

export const recheckFlagOptions = [
  { label: '复检单', value: true },
  { label: '原检/加检单', value: false },
];

type FaiInspectionTypeDisplayRecord = {
  recheckFlag?: boolean | null;
  triggerReason?: string | null;
  sourceModule?: string | null;
  sourceReportNo?: string | null;
  remark?: string | null;
};

export function isAdditionalFaiInspection(
  record?: FaiInspectionTypeDisplayRecord | null,
) {
  if (!record) return false;
  const triggerReason = String(record.triggerReason || '');
  if (
    ['ADDITIONAL_FIRST_INSPECTION', 'PROCESS_CHECK_NG_RESTART'].includes(
      triggerReason,
    )
  ) {
    return true;
  }
  const text = [
    record.sourceModule,
    record.sourceReportNo,
    record.triggerReason,
    record.remark,
  ]
    .filter(Boolean)
    .join(' ');
  return (
    text.toUpperCase().includes('PROCESS_CHECK') ||
    text.includes('过程加检') ||
    text.includes('加检')
  );
}

export function resolveFaiInspectionTypeMeta(
  record?: FaiInspectionTypeDisplayRecord | null,
) {
  if (isAdditionalFaiInspection(record)) {
    return { color: 'blue', label: '加检' };
  }
  if (record?.recheckFlag) {
    return { color: 'orange', label: '复检' };
  }
  return { color: 'default', label: '原检' };
}

export const itemRecheckStatusFilterOptions = [
  { label: '待复检', value: 'WAIT_RECHECK' },
  { label: '复检待审', value: 'WAIT_AUDIT' },
];

export function retentionStatusLabel(value?: string) {
  return (
    retentionStatusOptions.find((item) => item.value === value)?.label ||
    value ||
    '-'
  );
}

export function retentionStatusColor(value?: string) {
  if (value === 'RETAINED') return 'success';
  if (value === 'NOT_RETAINED') return 'warning';
  return 'default';
}

export function retentionDestroyStatusLabel(value?: string) {
  return (
    retentionDestroyStatusOptions.find((item) => item.value === value)?.label ||
    value ||
    '待报废'
  );
}

export function retentionDestroyStatusColor(value?: string) {
  if (value === 'DESTROYED') return 'success';
  return 'warning';
}

export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      fieldName: 'faiNo',
      label: '首检单号',
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
      fieldName: 'glueBoardMaterialCode',
      label: '胶板料号',
      component: 'Input',
      componentProps: { placeholder: '输入胶板料号' },
    },
    {
      fieldName: 'gluePlateBatchNo',
      label: '胶板批号',
      component: 'Input',
      componentProps: { placeholder: '输入胶板批号' },
    },
    {
      fieldName: 'status',
      label: '单据状态',
      component: 'Select',
      componentProps: { options: faiStatusOptions, allowClear: true },
    },
    {
      fieldName: 'judgment',
      label: '判定结果',
      component: 'Select',
      componentProps: { options: faiJudgmentOptions, allowClear: true },
    },
    {
      fieldName: 'recheckFlag',
      label: '复检标记',
      component: 'Select',
      componentProps: { options: recheckFlagOptions, allowClear: true },
    },
    {
      fieldName: 'itemRecheckStatusFilter',
      label: '复检审核',
      component: 'Select',
      componentProps: {
        options: itemRecheckStatusFilterOptions,
        allowClear: true,
      },
    },
    {
      fieldName: 'retentionStatus',
      label: '留样状态',
      component: 'Select',
      componentProps: { options: retentionStatusOptions, allowClear: true },
    },
    {
      fieldName: 'submissionTime',
      label: '送检时间',
      component: 'RangePicker',
      componentProps: { ...getRangePickerDefaultProps() },
    },
    {
      fieldName: 'inspectionTime',
      label: '检验时间',
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
      field: 'faiNo',
      title: '首检单号',
      width: 160,
      fixed: 'left',
      slots: { default: 'faiNo' },
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
    {
      field: 'productBatchNo',
      title: '产品批次',
      minWidth: 130,
      slots: { default: 'productBatchNo' },
    },
    { field: 'glueBoardMaterialCode', title: '胶板料号', minWidth: 130 },
    { field: 'gluePlateBatchNo', title: '胶板批号', minWidth: 130 },
    {
      field: 'inspectionQty',
      title: '送检数量',
      width: 120,
      align: 'right',
      formatter: ({ row }) => formatInspectionQty(row),
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
      title: '检验类型',
      width: 110,
      align: 'center',
      slots: { default: 'recheckFlag' },
    },
    {
      field: 'latestRecheckApplyStatus',
      title: '复检申请',
      width: 120,
      align: 'center',
      slots: { default: 'recheckApplyStatus' },
    },
    {
      field: 'retentionStatus',
      title: '留样状态',
      width: 110,
      align: 'center',
      slots: { default: 'retentionStatus' },
    },
    { field: 'submissionTime', title: '送检时间', width: 160, align: 'center' },
    { field: 'inspectionTime', title: '检验时间', width: 160, align: 'center' },
    {
      field: 'qTime',
      title: 'Q-time',
      width: 120,
      align: 'center',
      formatter: ({ row }) => formatFaiQTime(row),
    },
    { field: 'submitterName', title: '送检人员', width: 100, align: 'center' },
    { field: 'remark', title: '备注', minWidth: 180 },
    {
      title: '操作',
      field: 'action',
      fixed: 'right',
      width: 320,
      align: 'center',
      slots: { default: 'actions' },
    },
  ];
}
