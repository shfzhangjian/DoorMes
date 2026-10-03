import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesHcTerminalApi } from '#/api/mes/hc/terminal';

import { getWorkCenterSelectOptions } from '#/api/mes/hc/workcenter';

export function useFormSchema(): VbenFormSchema[] {
  return [
    { fieldName: 'id', component: 'Input', dependencies: { triggerFields: [''], show: () => false } },
    { fieldName: 'terminalCode', label: '终端编码', component: 'Input', rules: 'required', componentProps: { placeholder: '请输入编码' }, formItemClass: 'col-span-1', },
    { fieldName: 'terminalName', label: '终端名称', component: 'Input', rules: 'required', componentProps: { placeholder: '请输入名称' }, formItemClass: 'col-span-1', },
    { fieldName: 'workCenterId', label: '工作中心', component: 'ApiSelect', rules: 'selectRequired', componentProps: { api: getWorkCenterSelectOptions, labelField: 'label', valueField: 'value', placeholder: '请选择工作中心', allowClear: true }, formItemClass: 'col-span-1' },
    { fieldName: 'terminalMode', label: '终端模式', component: 'Input', rules: 'required', componentProps: { placeholder: '请输入模式' }, formItemClass: 'col-span-1', },
    { fieldName: 'status', label: '状态', component: 'RadioGroup', rules: 'required', componentProps: { options: [{ label: '启用', value: 0 }, { label: '停用', value: 1 }], buttonStyle: 'solid', optionType: 'button' }, defaultValue: 0, formItemClass: 'col-span-1' },
  ];
}

export function useGridFormSchema(): VbenFormSchema[] {
  return [
    { fieldName: 'terminalCode', label: '终端编码', component: 'Input', componentProps: { placeholder: '请输入编码' }, },
    { fieldName: 'terminalName', label: '终端名称', component: 'Input', componentProps: { placeholder: '请输入名称' }, },
    { fieldName: 'workCenterId', label: '工作中心', component: 'ApiSelect', componentProps: { api: getWorkCenterSelectOptions, labelField: 'label', valueField: 'value', placeholder: '请选择工作中心', allowClear: true } },
    { fieldName: 'status', label: '状态', component: 'Select', componentProps: { options: [{ label: '启用', value: 0 }, { label: '停用', value: 1 }], allowClear: true, placeholder: '请选择状态' } },
  ];
}

export function useGridColumns(): VxeTableGridOptions<MesHcTerminalApi.Terminal>['columns'] {
  return [
    { type: 'checkbox', width: 48, fixed: 'left' },
    { field: 'terminalCode', title: '终端编码', minWidth: 140 },
    { field: 'terminalName', title: '终端名称', minWidth: 140 },
    { field: 'status', title: '状态', minWidth: 120, formatter: ({ cellValue }) => ({ 0: '启用', 1: '停用' } as Record<string, string>)[String(cellValue)] || cellValue || '-' },
    { field: 'workCenterCode', title: '工作中心编码', minWidth: 140 },
    { field: 'terminalMode', title: '终端模式', minWidth: 140 },
    { title: '操作', width: 160, fixed: 'right', slots: { default: 'actions' } },
  ];
}
