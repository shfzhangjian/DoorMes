import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import { getRangePickerDefaultProps } from '#/utils';

export function useGridFormSchema(): VbenFormSchema[] {
  return [
    { fieldName: 'scarNo', label: 'SCAR单号', component: 'Input', componentProps: { placeholder: '请输入整改单号', allowClear: true } },
    { fieldName: 'supplierName', label: '供应商名称', component: 'Input', componentProps: { placeholder: '模糊检索', allowClear: true } },
    { fieldName: 'source', label: '异常来源', component: 'Select', componentProps: { options: [{label:'绩效C/D级', value:'绩效C/D级'}, {label:'IQC拒收', value:'IQC拒收'}, {label:'产线不良', value:'产线不良'}, {label:'重大客诉', value:'重大客诉'}], allowClear: true } },
    { fieldName: 'createTime', label: '发起时间', component: 'RangePicker', componentProps: { ...getRangePickerDefaultProps(), allowClear: true } },
  ];
}

export function useGridColumns(): VxeTableGridOptions['columns'] {
  return [
    { type: 'seq', title: '序号', width: 60, align: 'center', fixed: 'left' },
    { field: 'scarNo', title: 'SCAR 整改单号', minWidth: 160, slots: { default: 'scarNo' }, fixed: 'left' },
    { field: 'supplierName', title: '供应商名称', minWidth: 200, slots: { default: 'supplierName' } },
    { field: 'source', title: '异常来源', minWidth: 120, align: 'center', slots: { default: 'source' } },
    { field: 'level', title: '严重等级', width: 100, align: 'center', slots: { default: 'level' } },
    { field: 'applyDept', title: '发起部门', minWidth: 120, align: 'center' },
    { field: 'status', title: '单据状态', minWidth: 100, align: 'center', slots: { default: 'status' } },
    { field: 'dueDate', title: '要求回复期限', minWidth: 120, align: 'center', slots: { default: 'dueDate' } },
    { title: '操作', width: 140, fixed: 'right', slots: { default: 'actions' } },
  ];
}
