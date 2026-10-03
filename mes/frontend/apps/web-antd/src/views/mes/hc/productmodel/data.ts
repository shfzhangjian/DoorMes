import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesHcProductModelApi } from '#/api/mes/hc/productmodel';

import { getModelRuleSelectOptions } from '#/api/mes/hc/modelrule';
import { getProductModelFamilyOptions } from '#/api/mes/hc/productmodel';

export const productModelStatusOptions = [
  { label: '启用', value: 'ENABLE' },
  { label: '停用', value: 'DISABLE' },
];

export const productModelLevelOptions = [
  { label: '具体型号', value: 'MODEL' },
  { label: '系列型号', value: 'FAMILY' },
];

export const prodTypeOptions = [
  { label: '量产计划', value: 'MASS' },
  { label: '研发试制', value: 'RND_TRIAL' },
];

export const materialTypeOptions = [
  { label: '通用白垫', value: 'WHITE_PAD' },
  { label: '通用黑垫', value: 'BLACK_PAD' },
];

export const sizeOptions = [
  { label: '775mm', value: '775' },
  { label: '740mm', value: '740' },
];

export const optionalSizeOptions = [
  { label: '空', value: '' },
  ...sizeOptions,
];

export const retentionPeriodUnitOptions = [
  { label: '天', value: 'DAY' },
  { label: '月', value: 'MONTH' },
];

const statusMap = Object.fromEntries(productModelStatusOptions.map((item) => [item.value, item.label]));
const prodTypeMap = Object.fromEntries(prodTypeOptions.map((item) => [item.value, item.label]));
const materialTypeMap = Object.fromEntries(materialTypeOptions.map((item) => [item.value, item.label]));
const retentionUnitMap = Object.fromEntries(retentionPeriodUnitOptions.map((item) => [item.value, item.label]));

export function useFormSchema(): VbenFormSchema[] {
  return [
    { fieldName: 'id', component: 'Input', dependencies: { triggerFields: [''], show: () => false } },
    { fieldName: 'modelRuleCode', component: 'Input', dependencies: { triggerFields: [''], show: () => false } },
    { fieldName: 'modelRuleName', component: 'Input', dependencies: { triggerFields: [''], show: () => false } },
    { fieldName: 'prodTypeName', component: 'Input', dependencies: { triggerFields: [''], show: () => false } },
    { fieldName: 'categoryName', component: 'Input', dependencies: { triggerFields: [''], show: () => false } },
    { fieldName: 'sizeName', component: 'Input', dependencies: { triggerFields: [''], show: () => false } },
    { fieldName: 'recipeCode', component: 'Input', dependencies: { triggerFields: [''], show: () => false } },
    { fieldName: 'recipeName', component: 'Input', dependencies: { triggerFields: [''], show: () => false } },
    { fieldName: 'defaultRouteCode', component: 'Input', dependencies: { triggerFields: [''], show: () => false } },
    { fieldName: 'defaultRouteName', component: 'Input', dependencies: { triggerFields: [''], show: () => false } },
    {
      fieldName: 'modelLevel',
      label: '型号层级',
      component: 'RadioGroup',
      defaultValue: 'MODEL',
      rules: 'required',
      componentProps: {
        options: productModelLevelOptions,
        buttonStyle: 'solid',
        optionType: 'button',
      },
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'parentModelId',
      label: '所属系列',
      component: 'ApiSelect',
      componentProps: {
        api: getProductModelFamilyOptions,
        labelField: 'label',
        valueField: 'value',
        allowClear: true,
        placeholder: '请选择所属系列，如 W33P',
      },
      dependencies: {
        triggerFields: ['modelLevel'],
        show: (values: any) => values.modelLevel === 'MODEL',
      },
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'modelRuleId',
      label: '型号规则',
      component: 'ApiSelect',
      rules: 'required',
      componentProps: {
        api: getModelRuleSelectOptions,
        labelField: 'label',
        valueField: 'value',
        placeholder: '请选择型号规则',
      },
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'modelCode',
      label: '产品型号编码',
      component: 'Input',
      rules: 'required',
      componentProps: { placeholder: '维护规则段值后生成或手工录入' },
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'modelName',
      label: '产品型号名称',
      component: 'Input',
      componentProps: { placeholder: '请输入产品型号名称' },
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'prodType',
      label: '生产类型',
      component: 'Select',
      componentProps: { options: prodTypeOptions, allowClear: true },
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'categoryCode',
      label: '物料类型',
      component: 'Select',
      componentProps: { options: materialTypeOptions, allowClear: true },
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'sizeSpec',
      label: '尺寸规格',
      component: 'Select',
      componentProps: { options: optionalSizeOptions, allowClear: true, placeholder: '可为空' },
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'retentionPeriodValue',
      label: '留样时长',
      component: 'InputNumber',
      componentProps: { min: 1, precision: 0, placeholder: '留空则手工设置' },
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'retentionPeriodUnit',
      label: '留样单位',
      component: 'Select',
      componentProps: { options: retentionPeriodUnitOptions, allowClear: true, placeholder: '请选择天或月' },
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'recipeId',
      label: '默认配方',
      component: 'Select',
      componentProps: { options: [], allowClear: true, placeholder: '请选择默认配方' },
      formItemClass: 'col-span-1',
    },
    { fieldName: 'defaultRouteId', component: 'Input', dependencies: { triggerFields: [''], show: () => false } },
    {
      fieldName: 'status',
      label: '状态',
      component: 'RadioGroup',
      defaultValue: 'ENABLE',
      componentProps: {
        options: productModelStatusOptions,
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
      fieldName: 'modelCode',
      label: '型号编码',
      component: 'Input',
      componentProps: { placeholder: '请输入型号编码' },
    },
    {
      fieldName: 'modelName',
      label: '型号名称',
      component: 'Input',
      componentProps: { placeholder: '请输入型号名称' },
    },
    {
      fieldName: 'status',
      label: '状态',
      component: 'Select',
      componentProps: { options: productModelStatusOptions, allowClear: true },
    },
  ];
}

export function useGridColumns(): VxeTableGridOptions<MesHcProductModelApi.ProductModel>['columns'] {
  return [
    { type: 'checkbox', width: 48, fixed: 'left' },
    { field: 'modelCode', title: '产品型号编码', minWidth: 160 },
    { field: 'modelName', title: '产品型号名称', minWidth: 180 },
    {
      field: 'modelLevel',
      title: '型号层级',
      minWidth: 110,
      formatter: ({ cellValue }) =>
        cellValue === 'FAMILY' ? '系列型号' : '具体型号',
    },
    { field: 'parentModelCode', title: '所属系列', minWidth: 120 },
    { field: 'modelRuleName', title: '型号规则', minWidth: 160 },
    {
      field: 'prodType',
      title: '生产类型',
      minWidth: 120,
      formatter: ({ cellValue }) => prodTypeMap[String(cellValue)] || cellValue || '-',
    },
    {
      field: 'categoryCode',
      title: '物料类型',
      minWidth: 120,
      formatter: ({ cellValue, row }) => row.categoryName || materialTypeMap[String(cellValue)] || cellValue || '-',
    },
    { field: 'sizeName', title: '尺寸规格', minWidth: 100 },
    {
      field: 'retentionPeriodValue',
      title: '留样时长',
      minWidth: 110,
      formatter: ({ row }) =>
        row.retentionPeriodValue
          ? `${row.retentionPeriodValue}${retentionUnitMap[String(row.retentionPeriodUnit)] || ''}`
          : '-',
    },
    { field: 'recipeCode', title: '默认配方', minWidth: 140 },
    { field: 'defaultRouteName', title: '默认路线', minWidth: 150 },
    {
      field: 'status',
      title: '状态',
      minWidth: 100,
      formatter: ({ cellValue }) => statusMap[String(cellValue)] || cellValue || '-',
    },
    {
      field: 'referencedFlag',
      title: '已引用',
      width: 90,
      align: 'center',
      formatter: ({ cellValue }) => (cellValue ? '是' : '否'),
    },
    { title: '操作', width: 160, fixed: 'right', slots: { default: 'actions' } },
  ];
}
