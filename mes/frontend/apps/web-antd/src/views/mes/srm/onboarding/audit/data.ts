import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import { getRangePickerDefaultProps } from '#/utils';

export function useGridFormSchema(): VbenFormSchema[] {
  return [
    { fieldName: 'selectionNo', label: '定标单号', component: 'Input', componentProps: { placeholder: '输入定标单号', allowClear: true } },
    { fieldName: 'supplierName', label: '供应商名称', component: 'Input', componentProps: { placeholder: '模糊检索', allowClear: true } },
    { fieldName: 'materialName', label: '采购项目', component: 'Input', componentProps: { placeholder: '物料名称或品类', allowClear: true } },
    { fieldName: 'status', label: '审批状态', component: 'Select', componentProps: { options: [{label:'草稿', value:'DRAFT'}, {label:'审批中', value:'APPROVING'}, {label:'已退回', value:'REJECTED'}, {label:'已通过', value:'PASSED'}], allowClear: true } },
    { fieldName: 'createTime', label: '发起日期', component: 'RangePicker', componentProps: { ...getRangePickerDefaultProps(), allowClear: true } },
  ];
}

export function useGridColumns(): VxeTableGridOptions['columns'] {
  return [
    { type: 'seq', title: '序号', width: 60, align: 'center', fixed: 'left' },
    { field: 'selectionNo', title: '定标单号', minWidth: 160, slots: { default: 'selectionNo' }, fixed: 'left' },
    { field: 'supplierName', title: '供应商名称', minWidth: 200, slots: { default: 'supplierName' } },
    { field: 'materialName', title: '采购项目/物料', minWidth: 160 },
    { field: 'finalScore', title: '定标综合得分', minWidth: 120, align: 'center', slots: { default: 'finalScore' } },
    { field: 'status', title: '单据状态', minWidth: 100, align: 'center', slots: { default: 'status' } },
    { field: 'applicant', title: '发起人', minWidth: 100, align: 'center' },
    { title: '操作', width: 140, fixed: 'right', slots: { default: 'actions' } },
  ];
}
