import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';

// ==========================================
// 1. 搜索表单
// ==========================================
export function useGridFormSchema(): VbenFormSchema[] {
  return [
    { fieldName: 'customerName', label: '客户名称', component: 'Input', componentProps: { placeholder: '输入客户名称或编码', allowClear: true } },
    { fieldName: 'ruleName', label: '规则名称', component: 'Input', componentProps: { placeholder: '输入规则名称', allowClear: true } },
    { fieldName: 'status', label: '状态', component: 'Select', componentProps: { options: [{ label: '启用', value: 0 }, { label: '禁用', value: 1 }], allowClear: true } },
  ];
}

// ==========================================
// 2. 列表字段定义
// ==========================================
export function useGridColumns(): VxeTableGridOptions['columns'] {
  return [
    { type: 'seq', title: '序号', width: 60, align: 'center' },
    { field: 'customerName', title: '客户信息', minWidth: 160, slots: { default: 'customer' } },
    { field: 'ruleName', title: '规则名称', minWidth: 160, slots: { default: 'ruleName' } },
    { field: 'barcodeType', title: '条码载体', minWidth: 100, align: 'center', slots: { default: 'barcodeType' } },
    { field: 'preview', title: '生成效果预览', minWidth: 220, slots: { default: 'preview' } },
    { field: 'printTemplate', title: '打印模板', minWidth: 140 },
    { field: 'status', title: '状态', minWidth: 80, align: 'center', slots: { default: 'status' } },
    { title: '操作', width: 140, fixed: 'right', slots: { default: 'actions' } },
  ];
}
