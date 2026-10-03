import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesHcFifoPolicyApi } from '#/api/mes/hc/fifopolicy';

export function useFormSchema(): VbenFormSchema[] {
  return [
    { fieldName: 'id', component: 'Input', dependencies: { triggerFields: [''], show: () => false } },
    { fieldName: 'policyCode', label: '策略编码', component: 'Input', rules: 'required', componentProps: { placeholder: '请输入编码' }, formItemClass: 'col-span-1', },
    { fieldName: 'policyName', label: '策略名称', component: 'Input', rules: 'required', componentProps: { placeholder: '请输入名称' }, formItemClass: 'col-span-1', },
    { fieldName: 'warehouseCode', label: '仓库编码', component: 'Input', rules: 'required', componentProps: { placeholder: '请输入编码' }, formItemClass: 'col-span-1', },
    { fieldName: 'warehouseName', label: '仓库名称', component: 'Input', rules: 'required', componentProps: { placeholder: '请输入名称' }, formItemClass: 'col-span-1', },
    { fieldName: 'ownerCode', label: '货主编码', component: 'Input', componentProps: { placeholder: '请输入编码' }, formItemClass: 'col-span-1', },
    { fieldName: 'matchScope', label: '适用范围', component: 'Input', rules: 'required', componentProps: { placeholder: '请输入适用范围' }, formItemClass: 'col-span-1', },
    { fieldName: 'issueRule', label: '出库规则', component: 'Input', rules: 'required', componentProps: { placeholder: '请输入出库规则' }, formItemClass: 'col-span-1', },
    { fieldName: 'priorityFields', label: '优先字段', component: 'Input', rules: 'required', componentProps: { placeholder: '请输入优先字段' }, formItemClass: 'col-span-1', },
    { fieldName: 'status', label: '状态', component: 'RadioGroup', rules: 'required', componentProps: { options: [{ label: '启用', value: '启用' }, { label: '停用', value: '停用' }], buttonStyle: 'solid', optionType: 'button' }, defaultValue: '启用', formItemClass: 'col-span-1' },
  ];
}

export function useGridFormSchema(): VbenFormSchema[] {
  return [
    { fieldName: 'policyCode', label: '策略编码', component: 'Input', componentProps: { placeholder: '请输入编码' }, },
    { fieldName: 'policyName', label: '策略名称', component: 'Input', componentProps: { placeholder: '请输入名称' }, },
    { fieldName: 'status', label: '状态', component: 'Select', componentProps: { options: [{ label: '启用', value: '启用' }, { label: '停用', value: '停用' }], allowClear: true, placeholder: '请选择状态' } },
  ];
}

export function useGridColumns(): VxeTableGridOptions<MesHcFifoPolicyApi.FifoPolicy>['columns'] {
  return [
    { type: 'checkbox', width: 48, fixed: 'left' },
    { field: 'policyCode', title: '策略编码', minWidth: 140 },
    { field: 'policyName', title: '策略名称', minWidth: 140 },
    { field: 'status', title: '状态', minWidth: 140, formatter: ({ cellValue }) => ({ '启用': '启用', '停用': '停用' } as Record<string, string>)[String(cellValue)] || cellValue || '-' },
    { field: 'warehouseName', title: '仓库名称', minWidth: 140 },
    { field: 'ownerCode', title: '货主编码', minWidth: 140 },
    { field: 'warehouseCode', title: '仓库编码', minWidth: 140 },
    { field: 'matchScope', title: '适用范围', minWidth: 140 },
    { title: '操作', width: 160, fixed: 'right', slots: { default: 'actions' } },
  ];
}
