import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import { getRangePickerDefaultProps } from '#/utils';

// ==========================================
// 1. 工作台搜索表单
// ==========================================
export function useGridFormSchema(): VbenFormSchema[] {
  return [
    { fieldName: 'applyNo', label: '申请单号', component: 'Input', componentProps: { placeholder: '导入申请单号', allowClear: true } },
    { fieldName: 'supplierName', label: '意向供应商', component: 'Input', componentProps: { placeholder: '供方名称模糊搜索', allowClear: true } },
    { fieldName: 'materialName', label: '涉及物料', component: 'Input', componentProps: { placeholder: '物料名称/品类', allowClear: true } },
    { fieldName: 'applyReason', label: '寻源原因', component: 'Select', componentProps: { options: [{label:'产能不满足', value:'产能'}, {label:'品质需提升', value:'品质'}, {label:'降本需求', value:'降本'}, {label:'开发二供三供', value:'二供'}, {label:'客户指定', value:'客指'}], allowClear: true } },
    { fieldName: 'status', label: '单据状态', component: 'Select', componentProps: { options: [{label:'草稿', value:'DRAFT'}, {label:'审批中', value:'APPROVING'}, {label:'已通过', value:'PASSED'}, {label:'已退回', value:'REJECTED'}], allowClear: true } },
    { fieldName: 'createTime', label: '发起时间', component: 'RangePicker', componentProps: { ...getRangePickerDefaultProps(), allowClear: true } },
  ];
}

// ==========================================
// 2. 工作台列表定义
// ==========================================
export function useGridColumns(): VxeTableGridOptions['columns'] {
  return [
    { type: 'seq', title: '序号', width: 60, align: 'center', fixed: 'left' },
    { field: 'applyNo', title: '申请流水号', minWidth: 160, slots: { default: 'applyNo' }, fixed: 'left' },
    { field: 'materialName', title: '寻源物料/品类', minWidth: 160, slots: { default: 'materialName' } },
    { field: 'supplierName', title: '推荐意向供应商', minWidth: 200, slots: { default: 'supplierName' } },
    { field: 'applyReason', title: '导入原因', minWidth: 140 },
    { field: 'status', title: '单据状态', minWidth: 100, align: 'center', slots: { default: 'status' } },
    { field: 'applicantName', title: '发起人', minWidth: 100, align: 'center' },
    { field: 'applyTime', title: '发起时间', minWidth: 160, align: 'center' },
    { title: '操作', width: 140, fixed: 'right', slots: { default: 'actions' } },
  ];
}
