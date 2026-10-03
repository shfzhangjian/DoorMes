import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesHcTeamApi } from '#/api/mes/hc/team';

import { getWorkCenterSelectOptions } from '#/api/mes/hc/workcenter';

export function useFormSchema(): VbenFormSchema[] {
  return [
    { fieldName: 'id', component: 'Input', dependencies: { triggerFields: [''], show: () => false } },
    { fieldName: 'workCenterId', component: 'Input', dependencies: { triggerFields: [''], show: () => false } },
    { fieldName: 'teamCode', label: '班组编码', component: 'Input', rules: 'required', componentProps: { placeholder: '请输入班组编码' }, formItemClass: 'col-span-1' },
    { fieldName: 'teamName', label: '班组名称', component: 'Input', rules: 'required', componentProps: { placeholder: '请输入班组名称' }, formItemClass: 'col-span-1' },
    { fieldName: 'leaderUserId', label: '班组长用户ID', component: 'InputNumber', componentProps: { class: 'w-full' }, formItemClass: 'col-span-1' },
    { fieldName: 'leaderName', label: '班组长姓名', component: 'Input', componentProps: { placeholder: '请输入班组长姓名' }, formItemClass: 'col-span-1' },
    {
      fieldName: 'status',
      label: '状态',
      component: 'RadioGroup',
      rules: 'required',
      componentProps: {
        options: [
          { label: '启用', value: 0 },
          { label: '停用', value: 1 },
        ],
        buttonStyle: 'solid',
        optionType: 'button',
      },
      defaultValue: 0,
      formItemClass: 'col-span-1',
    },
    { fieldName: 'remark', label: '备注', component: 'Textarea', componentProps: { rows: 3, placeholder: '请输入备注' }, formItemClass: 'col-span-1' },
  ];
}

export function useGridFormSchema(): VbenFormSchema[] {
  return [
    { fieldName: 'teamCode', label: '班组编码', component: 'Input', componentProps: { placeholder: '请输入班组编码' } },
    { fieldName: 'teamName', label: '班组名称', component: 'Input', componentProps: { placeholder: '请输入班组名称' } },
    {
      fieldName: 'workCenterId',
      label: '默认工作中心',
      component: 'ApiSelect',
      componentProps: {
        api: getWorkCenterSelectOptions,
        labelField: 'label',
        valueField: 'value',
        placeholder: '请选择默认工作中心',
        allowClear: true,
      },
    },
    {
      fieldName: 'status',
      label: '状态',
      component: 'Select',
      componentProps: {
        options: [
          { label: '启用', value: 0 },
          { label: '停用', value: 1 },
        ],
        allowClear: true,
        placeholder: '请选择状态',
      },
    },
  ];
}

export function useGridColumns(): VxeTableGridOptions<MesHcTeamApi.Team>['columns'] {
  return [
    { type: 'checkbox', width: 48, fixed: 'left' },
    { field: 'teamCode', title: '班组编码', minWidth: 140 },
    { field: 'teamName', title: '班组名称', minWidth: 160 },
    {
      field: 'status',
      title: '状态',
      minWidth: 120,
      formatter: ({ cellValue }) =>
        ({ 0: '启用', 1: '停用' } as Record<string, string>)[String(cellValue)] || cellValue || '-',
    },
    { field: 'workCenterCode', title: '默认工作中心编码', minWidth: 140 },
    { field: 'leaderName', title: '班组长姓名', minWidth: 140 },
    { field: 'remark', title: '备注', minWidth: 220 },
    { title: '操作', width: 160, fixed: 'right', slots: { default: 'actions' } },
  ];
}
