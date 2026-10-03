import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesHcBomApi } from '#/api/mes/hc/bom';

export function useFormSchema(): VbenFormSchema[] {
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
      fieldName: 'productModelId',
      component: 'Input',
      dependencies: { triggerFields: [''], show: () => false },
    },
    {
      fieldName: 'bomCode',
      label: 'BOM编码',
      component: 'Input',
      rules: 'required',
      componentProps: { placeholder: '请输入BOM编码' },
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'bomName',
      label: 'BOM名称',
      component: 'Input',
      rules: 'required',
      componentProps: { disabled: true, placeholder: '选择制成品物料后自动回填型号描述' },
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'productModelCode',
      component: 'Input',
      dependencies: { triggerFields: [''], show: () => false },
    },
    {
      fieldName: 'productModelName',
      component: 'Input',
      dependencies: { triggerFields: [''], show: () => false },
    },
    {
      fieldName: 'productSpec',
      component: 'Input',
      dependencies: { triggerFields: [''], show: () => false },
    },
    {
      fieldName: 'recipeId',
      component: 'Input',
      dependencies: { triggerFields: [''], show: () => false },
    },
    {
      fieldName: 'recipeCode',
      component: 'Input',
      dependencies: { triggerFields: [''], show: () => false },
    },
    {
      fieldName: 'recipeName',
      component: 'Input',
      dependencies: { triggerFields: [''], show: () => false },
    },
    {
      fieldName: 'versionNo',
      label: '版本号',
      component: 'Input',
      rules: 'required',
      defaultValue: 'V1',
      componentProps: { placeholder: '请输入版本号' },
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'bomType',
      label: 'BOM类型',
      component: 'Select',
      rules: 'selectRequired',
      defaultValue: '研发样',
      componentProps: {
        allowClear: true,
        placeholder: '请选择BOM类型',
        options: [
          { label: '量产', value: '量产' },
          { label: '研发样', value: '研发样' },
        ],
      },
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'yieldRate',
      label: '标准良率',
      component: 'InputNumber',
      defaultValue: 1,
      componentProps: { class: 'w-full', min: 0, precision: 4 },
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'pressSlotContinuousCheckCount',
      label: '压槽连续作业加检数',
      component: 'InputNumber',
      defaultValue: 20,
      componentProps: { class: 'w-full', min: 1, precision: 0 },
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
      fieldName: 'productModelCode',
      label: '型号编码',
      component: 'Input',
      componentProps: { placeholder: '请输入型号编码' },
    },
    {
      fieldName: 'recipeCode',
      label: '配方编码',
      component: 'Input',
      componentProps: { placeholder: '请输入配方编码' },
    },
  ];
}

export function useGridColumns(): VxeTableGridOptions<MesHcBomApi.Bom>['columns'] {
  return [
    { type: 'checkbox', width: 48, fixed: 'left' },
    { field: 'bomCode', title: 'BOM编码', minWidth: 160 },
    { field: 'bomName', title: 'BOM名称', minWidth: 180 },
    { field: 'productModelCode', title: '型号编码', minWidth: 130 },
    { field: 'productSpec', title: '尺寸规格', width: 110 },
    { field: 'recipeCode', title: '配方编码', minWidth: 150 },
    { field: 'productMaterialCode', title: '产品物料编码', minWidth: 160 },
    { field: 'productMaterialName', title: '产品物料名称', minWidth: 200 },
    { field: 'versionNo', title: '版本号', width: 120 },
    { field: 'bomType', title: 'BOM类型', width: 140 },
    { field: 'pressSlotContinuousCheckCount', title: '压槽加检数', width: 130 },
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
