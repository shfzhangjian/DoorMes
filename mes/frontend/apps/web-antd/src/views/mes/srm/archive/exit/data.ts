import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import { getRangePickerDefaultProps } from '#/utils';

export function useGridFormSchema(): VbenFormSchema[] {
  return [
    { fieldName: 'exitNo', label: '退出单号', component: 'Input', componentProps: { placeholder: '请输入审批单号', allowClear: true } },
    { fieldName: 'supplierName', label: '供应商名称', component: 'Input', componentProps: { placeholder: '模糊检索', allowClear: true } },
    { fieldName: 'exitType', label: '退出类型', component: 'Select', componentProps: { options: [{label:'质量淘汰', value:'质量淘汰'}, {label:'绩效不达标', value:'绩效不达标'}, {label:'长期无交易', value:'长期无交易'}, {label:'主动退出', value:'主动退出'}], allowClear: true } },
    { fieldName: 'createTime', label: '发起时间', component: 'RangePicker', componentProps: { ...getRangePickerDefaultProps(), allowClear: true } },
  ];
}

export function useGridColumns(): VxeTableGridOptions['columns'] {
  return [
    { type: 'seq', title: '序号', width: 60, align: 'center', fixed: 'left' },
    { field: 'exitNo', title: '退出审批单号', minWidth: 160, slots: { default: 'exitNo' }, fixed: 'left' },
    { field: 'supplierName', title: '供应商名称', minWidth: 200, slots: { default: 'supplierName' } },
    { field: 'exitType', title: '退出类型', minWidth: 120, align: 'center', slots: { default: 'exitType' } },
    { field: 'riskLevel', title: '断供风险', width: 90, align: 'center', slots: { default: 'riskLevel' } },
    { field: 'applyDept', title: '发起部门', minWidth: 120, align: 'center' },
    { field: 'status', title: '单据状态', minWidth: 100, align: 'center', slots: { default: 'status' } },
    { field: 'applyDate', title: '申请日期', minWidth: 120, align: 'center' },
    { title: '操作', width: 140, fixed: 'right', slots: { default: 'actions' } },
  ];
}
