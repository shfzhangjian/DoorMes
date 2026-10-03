import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesHcRouteApi } from '#/api/mes/hc/route';

export function useFormSchema(onApplicableScopeChange?: (value: string) => void): VbenFormSchema[] {
  return [
    {
      fieldName: 'id',
      component: 'Input',
      dependencies: { triggerFields: [''], show: () => false },
    },
    {
      fieldName: 'productMaterialId',
      component: 'Input',
      dependencies: { triggerFields: [''], show: () => false },
    },
    {
      fieldName: 'productMaterialCode',
      component: 'Input',
      dependencies: { triggerFields: [''], show: () => false },
    },
    {
      fieldName: 'productMaterialName',
      component: 'Input',
      dependencies: { triggerFields: [''], show: () => false },
    },
    {
      fieldName: 'applicableScope',
      label: '适用范围',
      component: 'RadioGroup',
      rules: 'required',
      defaultValue: 'MATERIAL',
      componentProps: {
        options: [
          { label: '绑定物料', value: 'MATERIAL' },
          { label: '通用路线', value: 'GLOBAL' },
        ],
        buttonStyle: 'solid',
        optionType: 'button',
        onChange: (event: any) => onApplicableScopeChange?.(String(event?.target?.value || event || 'MATERIAL')),
      },
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'routeCode',
      label: '路线编码',
      component: 'Input',
      rules: 'required',
      componentProps: { placeholder: '请输入路线编码' },
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'routeName',
      label: '路线名称',
      component: 'Input',
      rules: 'required',
      componentProps: { placeholder: '请输入路线名称' },
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'productLevel',
      label: '适用层次',
      component: 'Select',
      rules: 'selectRequired',
      componentProps: {
        allowClear: true,
        placeholder: '请选择适用层次',
        options: [
          { label: '产成品', value: 'FG' },
          { label: '半成品', value: 'WIP' },
          { label: '原材料', value: 'RM' },
          { label: '包材', value: 'PM' },
        ],
      },
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'versionNo',
      label: '版本号',
      component: 'Input',
      rules: 'required',
      componentProps: { placeholder: '请输入版本号' },
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'routeType',
      label: '路线类型',
      component: 'Select',
      componentProps: {
        allowClear: true,
        placeholder: '请选择路线类型',
        options: [
          { label: '标准路线', value: '标准路线' },
          { label: '试产路线', value: '试产路线' },
          { label: '包装路线', value: '包装路线' },
          { label: '返工路线', value: '返工路线' },
        ],
      },
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'status',
      label: '状态',
      component: 'RadioGroup',
      rules: 'required',
      defaultValue: 1,
      componentProps: {
        options: [
          { label: '启用', value: 1 },
          { label: '停用', value: 0 },
        ],
        buttonStyle: 'solid',
        optionType: 'button',
      },
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'remark',
      label: '备注',
      component: 'Textarea',
      componentProps: { rows: 3, placeholder: '请输入备注' },
      formItemClass: 'col-span-2',
    },
  ];
}

export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      fieldName: 'routeCode',
      label: '路线编码',
      component: 'Input',
      componentProps: { placeholder: '请输入路线编码' },
    },
    {
      fieldName: 'routeName',
      label: '路线名称',
      component: 'Input',
      componentProps: { placeholder: '请输入路线名称' },
    },
    {
      fieldName: 'productMaterialKeyword',
      label: '适用物料',
      component: 'Input',
      componentProps: { placeholder: '请输入适用物料编码或名称' },
    },
    {
      fieldName: 'applicableScope',
      label: '适用范围',
      component: 'Select',
      componentProps: {
        allowClear: true,
        placeholder: '请选择适用范围',
        options: [
          { label: '绑定物料', value: 'MATERIAL' },
          { label: '通用路线', value: 'GLOBAL' },
        ],
      },
    },
    {
      fieldName: 'status',
      label: '状态',
      component: 'Select',
      componentProps: {
        allowClear: true,
        placeholder: '请选择状态',
        options: [
          { label: '启用', value: 1 },
          { label: '停用', value: 0 },
        ],
      },
    },
  ];
}

export function useGridColumns(): VxeTableGridOptions<MesHcRouteApi.Route>['columns'] {
  return [
    { type: 'checkbox', width: 48, fixed: 'left' },
    { field: 'routeCode', title: '路线编码', minWidth: 160 },
    { field: 'routeName', title: '路线名称', minWidth: 180 },
    {
      field: 'applicableScope',
      title: '适用范围',
      width: 120,
      formatter: ({ cellValue }) => ({ MATERIAL: '绑定物料', GLOBAL: '通用路线' } as Record<string, string>)[String(cellValue)] || '-',
    },
    { field: 'productMaterialCode', title: '适用物料编码', minWidth: 160 },
    { field: 'productMaterialName', title: '适用物料名称', minWidth: 200 },
    {
      field: 'productLevel',
      title: '适用层次',
      width: 120,
      formatter: ({ cellValue }) => ({ FG: '产成品', WIP: '半成品', RM: '原材料', PM: '包材' } as Record<string, string>)[String(cellValue)] || '-',
    },
    { field: 'versionNo', title: '版本号', width: 120 },
    { field: 'routeType', title: '路线类型', width: 140 },
    {
      field: 'status',
      title: '状态',
      width: 100,
      formatter: ({ cellValue }) => ({ 1: '启用', 0: '停用' } as Record<string, string>)[String(cellValue)] || '-',
    },
    { field: 'remark', title: '备注', minWidth: 220 },
    { title: '操作', width: 180, fixed: 'right', slots: { default: 'actions' } },
  ];
}
