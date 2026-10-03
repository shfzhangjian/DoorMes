import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesHcFormTemplateApi } from '#/api/mes/hc/formtemplate';

export function useFormSchema(): VbenFormSchema[] {
  return [
    { fieldName: 'id', component: 'Input', dependencies: { triggerFields: [''], show: () => false } },
    { fieldName: 'templateCode', label: '模板编码', component: 'Input', rules: 'required', componentProps: { placeholder: '请输入编码' }, formItemClass: 'col-span-1', },
    { fieldName: 'templateName', label: '模板名称', component: 'Input', rules: 'required', componentProps: { placeholder: '请输入名称' }, formItemClass: 'col-span-1', },
    { fieldName: 'templateType', label: '模板类型', component: 'Input', rules: 'required', componentProps: { placeholder: '请输入类型' }, formItemClass: 'col-span-1', },
    { fieldName: 'businessStage', label: '业务工序', component: 'Input', rules: 'required', componentProps: { placeholder: '请输入业务工序' }, formItemClass: 'col-span-1', },
    { fieldName: 'formStyle', label: '表单样式', component: 'Input', rules: 'required', componentProps: { placeholder: '请输入表单样式' }, formItemClass: 'col-span-1', },
    { fieldName: 'status', label: '状态', component: 'RadioGroup', rules: 'required', componentProps: { options: [{ label: '启用', value: '启用' }, { label: '停用', value: '停用' }], buttonStyle: 'solid', optionType: 'button' }, defaultValue: '启用', formItemClass: 'col-span-1' },
    { fieldName: 'remark', label: '备注', component: 'Textarea', componentProps: { rows: 3, placeholder: '请输入' }, formItemClass: 'col-span-1', },
  ];
}

export function useGridFormSchema(): VbenFormSchema[] {
  return [
    { fieldName: 'templateCode', label: '模板编码', component: 'Input', componentProps: { placeholder: '请输入编码' }, },
    { fieldName: 'templateName', label: '模板名称', component: 'Input', componentProps: { placeholder: '请输入名称' }, },
    { fieldName: 'status', label: '状态', component: 'Select', componentProps: { options: [{ label: '启用', value: '启用' }, { label: '停用', value: '停用' }], allowClear: true, placeholder: '请选择状态' } },
  ];
}

export function useGridColumns(): VxeTableGridOptions<MesHcFormTemplateApi.FormTemplate>['columns'] {
  return [
    { type: 'checkbox', width: 48, fixed: 'left' },
    { field: 'templateCode', title: '模板编码', minWidth: 140 },
    { field: 'templateName', title: '模板名称', minWidth: 140 },
    { field: 'status', title: '状态', minWidth: 140, formatter: ({ cellValue }) => ({ '启用': '启用', '停用': '停用' } as Record<string, string>)[String(cellValue)] || cellValue || '-' },
    { field: 'businessStage', title: '业务工序', minWidth: 140 },
    { field: 'remark', title: '备注', minWidth: 220 },
    { field: 'templateType', title: '模板类型', minWidth: 140 },
    { field: 'formStyle', title: '表单样式', minWidth: 140 },
    { title: '操作', width: 160, fixed: 'right', slots: { default: 'actions' } },
  ];
}
