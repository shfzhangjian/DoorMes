import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesHcRecipeApi } from '#/api/mes/hc/recipe';

const statusOptions = [
  { label: '启用', value: 1 },
  { label: '停用', value: 0 },
];

const recipeTypeOptions = [
  { label: '量产', value: 'mass' },
  { label: '研发', value: 'rd' },
];

function formatRecipeType(value?: string) {
  return (
    recipeTypeOptions.find((item) => item.value === value)?.label ||
    (value ? String(value) : '-')
  );
}

export function useFormSchema(): VbenFormSchema[] {
  return [
    {
      fieldName: 'id',
      component: 'Input',
      dependencies: { triggerFields: [''], show: () => false },
    },
    {
      fieldName: 'recipeCode',
      label: '配方编码',
      component: 'Input',
      rules: 'required',
      componentProps: { placeholder: '请输入配方编码' },
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'recipeName',
      label: '配方名称',
      component: 'Input',
      rules: 'required',
      componentProps: { placeholder: '请输入配方名称' },
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'recipeType',
      label: '配方类型',
      component: 'Select',
      componentProps: {
        options: recipeTypeOptions,
        allowClear: true,
        placeholder: '请选择配方类型',
      },
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'modelCode',
      label: '型号编号',
      component: 'Input',
      componentProps: { placeholder: '请输入型号编号' },
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'usageCount',
      label: '累计使用次数',
      component: 'InputNumber',
      componentProps: { class: 'w-full', min: 0, precision: 0 },
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'productMaterialId',
      component: 'Input',
      dependencies: { triggerFields: [''], show: () => false },
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
      fieldName: 'solidContentStd',
      label: '标准固含',
      component: 'InputNumber',
      componentProps: { class: 'w-full', precision: 2 },
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'viscosityStd',
      label: '标准粘度',
      component: 'InputNumber',
      componentProps: { class: 'w-full', precision: 2 },
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'yieldRate',
      label: '理论得率',
      component: 'InputNumber',
      componentProps: { class: 'w-full', precision: 2 },
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'effectiveDate',
      label: '生效日期',
      component: 'DatePicker',
      componentProps: { class: 'w-full' },
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'expireDate',
      label: '失效日期',
      component: 'DatePicker',
      componentProps: { class: 'w-full' },
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'status',
      label: '状态',
      component: 'RadioGroup',
      rules: 'required',
      componentProps: {
        options: statusOptions,
        buttonStyle: 'solid',
        optionType: 'button',
      },
      defaultValue: 1,
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
      fieldName: 'recipeCode',
      label: '配方编码',
      component: 'Input',
      componentProps: { placeholder: '请输入配方编码' },
    },
    {
      fieldName: 'recipeName',
      label: '配方名称',
      component: 'Input',
      componentProps: { placeholder: '请输入配方名称' },
    },
    {
      fieldName: 'recipeType',
      label: '配方类型',
      component: 'Select',
      componentProps: {
        options: recipeTypeOptions,
        allowClear: true,
        placeholder: '请选择配方类型',
      },
    },
    {
      fieldName: 'modelCode',
      label: '型号编号',
      component: 'Input',
      componentProps: { placeholder: '请输入型号编号' },
    },
    {
      fieldName: 'status',
      label: '状态',
      component: 'Select',
      componentProps: {
        options: statusOptions,
        allowClear: true,
        placeholder: '请选择状态',
      },
    },
  ];
}

export function useGridColumns(): VxeTableGridOptions<MesHcRecipeApi.Recipe>['columns'] {
  return [
    { type: 'checkbox', width: 48, fixed: 'left' },
    { field: 'recipeCode', title: '配方编码', minWidth: 140 },
    { field: 'recipeName', title: '配方名称', minWidth: 160 },
    {
      field: 'recipeType',
      title: '配方类型',
      minWidth: 120,
      formatter: ({ cellValue }) => formatRecipeType(cellValue),
    },
    { field: 'modelCode', title: '型号编号', minWidth: 160 },
    {
      field: 'status',
      title: '状态',
      minWidth: 100,
      formatter: ({ cellValue }) =>
        ({ 1: '启用', 0: '停用' } as Record<string, string>)[String(cellValue)] || cellValue || '-',
    },
    { field: 'usageCount', title: '累计使用次数', minWidth: 140 },
    { field: 'versionNo', title: '版本号', minWidth: 120 },
    { field: 'remark', title: '备注', minWidth: 220 },
    { title: '操作', width: 160, fixed: 'right', slots: { default: 'actions' } },
  ];
}

export { recipeTypeOptions, formatRecipeType };
