import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import { getRangePickerDefaultProps } from '#/utils';

export function useGridFormSchema(): VbenFormSchema[] {
  return [
    { fieldName: 'sampleNo', label: '送样单号', component: 'Input', componentProps: { placeholder: '输入送样单号', allowClear: true } },
    { fieldName: 'supplierName', label: '供应商名称', component: 'Input', componentProps: { placeholder: '模糊检索', allowClear: true } },
    { fieldName: 'materialName', label: '样品名称', component: 'Input', componentProps: { placeholder: '品名或规格型号', allowClear: true } },
    { fieldName: 'verifyType', label: '验证类别', component: 'Select', componentProps: { options: [{label:'新品开发', value:'新品开发'}, {label:'产品改进', value:'产品改进'}, {label:'增加供应商', value:'增加供应商'}], allowClear: true } },
    { fieldName: 'createTime', label: '送样日期', component: 'RangePicker', componentProps: { ...getRangePickerDefaultProps(), allowClear: true } },
  ];
}

export function useGridColumns(): VxeTableGridOptions['columns'] {
  return [
    { type: 'seq', title: '序号', width: 60, align: 'center', fixed: 'left' },
    { field: 'sampleNo', title: '送样评估单号', minWidth: 160, slots: { default: 'sampleNo' }, fixed: 'left' },
    { field: 'supplierName', title: '供应商名称', minWidth: 200, slots: { default: 'supplierName' } },
    { field: 'materialName', title: '样品品名及规格', minWidth: 180, slots: { default: 'materialName' } },
    { field: 'sampleQty', title: '送样数量', minWidth: 100, align: 'center' },
    { field: 'verifyType', title: '验证类别', minWidth: 120, align: 'center' },
    { field: 'status', title: '综合结论', minWidth: 120, align: 'center', slots: { default: 'status' } },
    { title: '操作', width: 120, fixed: 'right', slots: { default: 'actions' } },
  ];
}
