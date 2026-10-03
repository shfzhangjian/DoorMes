import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesHcOwnerApi } from '#/api/mes/hc/owner';

export function useFormSchema(): VbenFormSchema[] {
  return [
    { fieldName: 'id', component: 'Input', dependencies: { triggerFields: [''], show: () => false } },
    { fieldName: 'ownerCode', label: '货主编码', component: 'Input', rules: 'required', componentProps: { placeholder: '请输入编码' }, formItemClass: 'col-span-1', },
    { fieldName: 'ownerName', label: '货主名称', component: 'Input', rules: 'required', componentProps: { placeholder: '请输入名称' }, formItemClass: 'col-span-1', },
    { fieldName: 'ownerType', label: '货主类型', component: 'Input', rules: 'required', componentProps: { placeholder: '请输入类型' }, formItemClass: 'col-span-1', },
    { fieldName: 'contactName', label: '联系人', component: 'Input', componentProps: { placeholder: '请输入名称' }, formItemClass: 'col-span-1', },
    { fieldName: 'contactPhone', label: '联系电话', component: 'Input', componentProps: { placeholder: '请输入联系电话' }, formItemClass: 'col-span-1', },
    { fieldName: 'status', label: '状态', component: 'RadioGroup', rules: 'required', componentProps: { options: [{ label: '启用', value: '启用' }, { label: '停用', value: '停用' }], buttonStyle: 'solid', optionType: 'button' }, defaultValue: '启用', formItemClass: 'col-span-1' },
  ];
}

export function useGridFormSchema(): VbenFormSchema[] {
  return [
    { fieldName: 'ownerCode', label: '货主编码', component: 'Input', componentProps: { placeholder: '请输入编码' }, },
    { fieldName: 'ownerName', label: '货主名称', component: 'Input', componentProps: { placeholder: '请输入名称' }, },
    { fieldName: 'status', label: '状态', component: 'Select', componentProps: { options: [{ label: '启用', value: '启用' }, { label: '停用', value: '停用' }], allowClear: true, placeholder: '请选择状态' } },
  ];
}

export function useGridColumns(): VxeTableGridOptions<MesHcOwnerApi.Owner>['columns'] {
  return [
    { type: 'checkbox', width: 48, fixed: 'left' },
    { field: 'ownerCode', title: '货主编码', minWidth: 140 },
    { field: 'ownerName', title: '货主名称', minWidth: 140 },
    { field: 'status', title: '状态', minWidth: 140, formatter: ({ cellValue }) => ({ '启用': '启用', '停用': '停用' } as Record<string, string>)[String(cellValue)] || cellValue || '-' },
    { field: 'ownerType', title: '货主类型', minWidth: 140 },
    { field: 'contactName', title: '联系人', minWidth: 140 },
    { field: 'contactPhone', title: '联系电话', minWidth: 140 },
    { title: '操作', width: 160, fixed: 'right', slots: { default: 'actions' } },
  ];
}
