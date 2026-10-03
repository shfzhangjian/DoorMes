import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesHcOcapTemplateApi } from '#/api/mes/hc/ocaptemplate';

export function useFormSchema(): VbenFormSchema[] {
  return [
    { fieldName: 'id', component: 'Input', dependencies: { triggerFields: [''], show: () => false } },
    { fieldName: 'ocapCode', label: 'OCAP编码', component: 'Input', rules: 'required', componentProps: { placeholder: '请输入编码' }, formItemClass: 'col-span-1', },
    { fieldName: 'ocapName', label: 'OCAP名称', component: 'Input', rules: 'required', componentProps: { placeholder: '请输入名称' }, formItemClass: 'col-span-1', },
    { fieldName: 'businessStage', label: '业务工序', component: 'Input', rules: 'required', componentProps: { placeholder: '请输入业务工序' }, formItemClass: 'col-span-1', },
    { fieldName: 'triggerItemCode', label: '触发项目编码', component: 'Input', rules: 'required', componentProps: { placeholder: '请输入编码' }, formItemClass: 'col-span-1', },
    { fieldName: 'triggerCondition', label: '触发条件', component: 'Textarea', rules: 'required', componentProps: { rows: 3, placeholder: '请输入' }, formItemClass: 'col-span-1', },
    { fieldName: 'actionSteps', label: '处置步骤', component: 'Textarea', rules: 'required', componentProps: { rows: 3, placeholder: '请输入' }, formItemClass: 'col-span-1', },
    { fieldName: 'status', label: '状态', component: 'RadioGroup', rules: 'required', componentProps: { options: [{ label: '启用', value: '启用' }, { label: '停用', value: '停用' }], buttonStyle: 'solid', optionType: 'button' }, defaultValue: '启用', formItemClass: 'col-span-1' },
  ];
}

export function useGridFormSchema(): VbenFormSchema[] {
  return [
    { fieldName: 'ocapCode', label: 'OCAP编码', component: 'Input', componentProps: { placeholder: '请输入编码' }, },
    { fieldName: 'ocapName', label: 'OCAP名称', component: 'Input', componentProps: { placeholder: '请输入名称' }, },
    { fieldName: 'status', label: '状态', component: 'Select', componentProps: { options: [{ label: '启用', value: '启用' }, { label: '停用', value: '停用' }], allowClear: true, placeholder: '请选择状态' } },
  ];
}

export function useGridColumns(): VxeTableGridOptions<MesHcOcapTemplateApi.OcapTemplate>['columns'] {
  return [
    { type: 'checkbox', width: 48, fixed: 'left' },
    { field: 'ocapCode', title: 'OCAP编码', minWidth: 140 },
    { field: 'ocapName', title: 'OCAP名称', minWidth: 140 },
    { field: 'status', title: '状态', minWidth: 140, formatter: ({ cellValue }) => ({ '启用': '启用', '停用': '停用' } as Record<string, string>)[String(cellValue)] || cellValue || '-' },
    { field: 'businessStage', title: '业务工序', minWidth: 140 },
    { field: 'triggerItemCode', title: '触发项目编码', minWidth: 140 },
    { field: 'triggerCondition', title: '触发条件', minWidth: 140 },
    { field: 'actionSteps', title: '处置步骤', minWidth: 220 },
    { title: '操作', width: 160, fixed: 'right', slots: { default: 'actions' } },
  ];
}
