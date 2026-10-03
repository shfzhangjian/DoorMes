import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import { getRangePickerDefaultProps } from '#/utils';

export function useGridFormSchema(): VbenFormSchema[] {
  return [
    { fieldName: 'trialNo', label: '试产单号', component: 'Input', componentProps: { placeholder: '输入单号', allowClear: true } },
    { fieldName: 'supplierName', label: '供应商名称', component: 'Input', componentProps: { placeholder: '模糊检索', allowClear: true } },
    { fieldName: 'materialName', label: '试产物料', component: 'Input', componentProps: { placeholder: '物料名称', allowClear: true } },
    { fieldName: 'status', label: '试产状态', component: 'Select', componentProps: { options: [{label:'草稿', value:'DRAFT'}, {label:'试产评估中', value:'APPROVING'}, {label:'试产失败', value:'REJECTED'}, {label:'试产合格', value:'PASSED'}], allowClear: true } },
    { fieldName: 'createTime', label: '试产日期', component: 'RangePicker', componentProps: { ...getRangePickerDefaultProps(), allowClear: true } },
  ];
}

export function useGridColumns(): VxeTableGridOptions['columns'] {
  return [
    { type: 'seq', title: '序号', width: 60, align: 'center', fixed: 'left' },
    { field: 'trialNo', title: '试产追踪单号', minWidth: 160, slots: { default: 'trialNo' }, fixed: 'left' },
    { field: 'supplierName', title: '供应商名称', minWidth: 200, slots: { default: 'supplierName' } },
    { field: 'materialName', title: '试产物料', minWidth: 160 },
    { field: 'trialQty', title: '试产批量', width: 100, align: 'center' },
    { field: 'lineYield', title: '综合良率', width: 100, align: 'center', slots: { default: 'lineYield' } },
    { field: 'status', title: '单据状态', minWidth: 100, align: 'center', slots: { default: 'status' } },
    { field: 'trialDate', title: '试产日期', minWidth: 120, align: 'center' },
    { title: '操作', width: 140, fixed: 'right', slots: { default: 'actions' } },
  ];
}
