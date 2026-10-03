import type { FormSchema } from '#/adapter/form';
import type { VxeGridProps } from '#/adapter/vxe-table';
import { getRangePickerDefaultProps } from '#/utils';

export const OQC_STATUS_LABEL_MAP: Record<string, string> = {
  CANCELED: '已取消',
  COMPLETED: '已完成',
  INSPECTING: '检验中',
  PENDING: '待检',
  REJECTED: '已拦截',
  SUSPENDED: '已挂起',
  WAITING_QA: '待审核',
};

export const OQC_JUDGMENT_LABEL_MAP: Record<string, string> = {
  '-': '待判定',
  NA: '不适用',
  NG: '异常/拦截',
  OK: '合格/允许发货',
  PENDING: '待判定',
};

export const recheckFlagOptions = [
  { label: '复检单', value: true },
  { label: '非复检单', value: false },
];

export function resolveOqcStatusLabel(status?: string) {
  return status ? OQC_STATUS_LABEL_MAP[status] || status : '-';
}

export function resolveOqcJudgmentLabel(judgment?: string) {
  return judgment ? OQC_JUDGMENT_LABEL_MAP[judgment] || judgment : '待判定';
}

export function useGridFormSchema(): FormSchema[] {
  return [
    { fieldName: 'oqcNo', label: '出货检单号', component: 'Input', componentProps: { placeholder: '输入单号' } },
    { fieldName: 'customerName', label: '客户名称', component: 'Input', componentProps: { placeholder: '输入客户名' } },
    { fieldName: 'status', label: '单据状态', component: 'Select', componentProps: { options: [{label:'待检',value:'PENDING'},{label:'检验中',value:'INSPECTING'},{label:'待审核',value:'WAITING_QA'},{label:'已完成',value:'COMPLETED'},{label:'已拦截',value:'REJECTED'},{label:'已挂起',value:'SUSPENDED'},{label:'已取消',value:'CANCELED'}], allowClear: true } },
    { fieldName: 'judgment', label: '检验判定', component: 'Select', componentProps: { options: [{label:'待判定',value:'PENDING'},{label:'合格/允许发货',value:'OK'},{label:'异常/拦截',value:'NG'}], allowClear: true } },
    { fieldName: 'recheckFlag', label: '复检标记', component: 'Select', componentProps: { options: recheckFlagOptions, allowClear: true } },
    { fieldName: 'inspectionTime', label: '检验时间', component: 'RangePicker', componentProps: { ...getRangePickerDefaultProps() } },
  ];
}

export function useGridColumns(): VxeGridProps['columns'] {
  return [
    { type: 'seq', width: 50, align: 'center', fixed: 'left' },
    { field: 'oqcNo', title: '出货检验单号', width: 160, fixed: 'left', slots: { default: 'oqcNo' } },
    { field: 'customerName', formatter: ({ row, cellValue }) => row.productType === 'SAMPLE' ? '' : cellValue, title: '收货客户', minWidth: 160 },
    { field: 'materialCode', title: '产品料号', minWidth: 150 },
    { field: 'batchNo', title: '出货批次', width: 130 },
    { field: 'shippingPieceQty', title: '片，总计', width: 90, align: 'right' },
    { field: 'status', title: '状态', width: 100, align: 'center', slots: { default: 'status' } },
    { field: 'recheckFlag', title: '复检', width: 100, align: 'center', slots: { default: 'recheckFlag' } },
    { field: 'originalInspectionNo', title: '原检验单号', minWidth: 150 },
    { field: 'inspectorName', title: '检验员', width: 90, align: 'center' },
    { field: 'inspectionTime', title: '检验时间', width: 150, align: 'center' },
    { field: 'qaInspectorName', title: '审核员', width: 90, align: 'center' },
    { field: 'qaTime', title: '审核时间', width: 150, align: 'center' },
    { field: 'judgment', title: '判定结果', width: 100, align: 'center', slots: { default: 'judgment' } },
    { title: '操作', field: 'action', fixed: 'right', width: 180, align: 'center', slots: { default: 'actions' } },
  ];
}
