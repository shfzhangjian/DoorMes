import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesHcMaterialApi } from '#/api/mes/hc/material';

import { getMaterialCategoryTree } from '#/api/mes/hc/materialcategory';
import { getLotRuleSelectOptions } from '#/api/mes/hc/lotrule';
import { getUnitSelectOptions } from '#/api/mes/base/unit';

const materialTypeOptions = [
  { label: '成品', value: 1 },
  { label: '半成品', value: 2 },
  { label: '原料', value: 3 },
  { label: '辅料', value: 4 },
  { label: '包材', value: 5 },
  { label: '备件', value: 6 },
];

const materialTypeMap = Object.fromEntries(
  materialTypeOptions.map((item) => [String(item.value), item.label]),
);

export function useFormSchema(): VbenFormSchema[] {
  return [
    {
      fieldName: 'id',
      component: 'Input',
      dependencies: {
        triggerFields: [''],
        show: () => false,
      },
    },
    {
      fieldName: 'materialCode',
      label: '物料编码',
      component: 'Input',
      componentProps: { placeholder: '可手工填写，留空则按物料编号规则自动生成' },
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'materialCodeRuleId',
      label: '物料编号规则',
      component: 'ApiSelect',
      componentProps: {
        api: getLotRuleSelectOptions,
        labelField: 'label',
        valueField: 'value',
        placeholder: '请选择物料编号规则',
        allowClear: true,
      },
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'materialName',
      label: '物料名称',
      component: 'Input',
      rules: 'required',
      componentProps: { placeholder: '请输入物料名称' },
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'materialShortName',
      label: '物料简称',
      component: 'Input',
      componentProps: { placeholder: '请输入物料简称' },
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'materialType',
      label: '物料类型',
      component: 'Select',
      rules: 'required',
      componentProps: {
        options: materialTypeOptions,
        placeholder: '请选择物料类型',
        allowClear: true,
      },
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'materialCategoryId',
      label: '物料分类',
      component: 'ApiTreeSelect',
      rules: 'selectRequired',
      componentProps: {
        api: getMaterialCategoryTree,
        labelField: 'categoryName',
        valueField: 'id',
        childrenField: 'children',
        placeholder: '请选择物料分类',
        treeDefaultExpandAll: true,
        allowClear: true,
        showSearch: true,
        treeLine: true,
        popupClassName: 'hc-material-category-tree-select',
      },
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'specModel',
      label: '规格型号描述',
      component: 'Input',
      componentProps: { placeholder: '请输入规格型号描述' },
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'modelCodeRuleId',
      component: 'Input',
      dependencies: {
        triggerFields: [''],
        show: () => false,
      },
    },
    {
      fieldName: 'productModelId',
      component: 'Input',
      dependencies: {
        triggerFields: [''],
        show: () => false,
      },
    },
    {
      fieldName: 'modelCode',
      component: 'Input',
      dependencies: {
        triggerFields: [''],
        show: () => false,
      },
    },
    {
      fieldName: 'productModelName',
      component: 'Input',
      dependencies: {
        triggerFields: [''],
        show: () => false,
      },
    },
    {
      fieldName: 'modelSegmentsJson',
      component: 'Input',
      dependencies: {
        triggerFields: [''],
        show: () => false,
      },
    },
    {
      fieldName: 'baseUnitId',
      label: '基础单位',
      component: 'ApiSelect',
      rules: 'selectRequired',
      componentProps: {
        api: getUnitSelectOptions,
        labelField: 'label',
        valueField: 'value',
        placeholder: '请选择基础单位',
        allowClear: true,
      },
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'baseUnitCode',
      component: 'Input',
      dependencies: { triggerFields: [''], show: () => false },
    },
    {
      fieldName: 'baseUnitName',
      component: 'Input',
      dependencies: { triggerFields: [''], show: () => false },
    },
    {
      fieldName: 'baseUom',
      component: 'Input',
      dependencies: { triggerFields: [''], show: () => false },
    },
    {
      fieldName: 'stockUnitId',
      label: '库存单位',
      component: 'ApiSelect',
      componentProps: {
        api: getUnitSelectOptions,
        labelField: 'label',
        valueField: 'value',
        placeholder: '请选择库存单位',
        allowClear: true,
      },
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'stockUnitCode',
      component: 'Input',
      dependencies: { triggerFields: [''], show: () => false },
    },
    {
      fieldName: 'stockUnitName',
      component: 'Input',
      dependencies: { triggerFields: [''], show: () => false },
    },
    {
      fieldName: 'stockUom',
      component: 'Input',
      dependencies: { triggerFields: [''], show: () => false },
    },
    {
      fieldName: 'produceUnitId',
      label: '生产单位',
      component: 'ApiSelect',
      componentProps: {
        api: getUnitSelectOptions,
        labelField: 'label',
        valueField: 'value',
        placeholder: '请选择生产单位',
        allowClear: true,
      },
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'produceUnitCode',
      component: 'Input',
      dependencies: { triggerFields: [''], show: () => false },
    },
    {
      fieldName: 'produceUnitName',
      component: 'Input',
      dependencies: { triggerFields: [''], show: () => false },
    },
    {
      fieldName: 'produceUom',
      component: 'Input',
      dependencies: { triggerFields: [''], show: () => false },
    },
    {
      fieldName: 'defaultRouteId',
      component: 'Input',
      dependencies: {
        triggerFields: [''],
        show: () => false,
      },
    },
    {
      fieldName: 'defaultBomId',
      component: 'Input',
      dependencies: {
        triggerFields: [''],
        show: () => false,
      },
    },
    {
      fieldName: 'defaultRecipeId',
      component: 'Input',
      dependencies: {
        triggerFields: [''],
        show: () => false,
      },
    },
    {
      fieldName: 'materialStatus',
      label: '物料状态',
      component: 'RadioGroup',
      rules: 'required',
      componentProps: {
        options: [
          { label: '草稿', value: 0 },
          { label: '启用', value: 1 },
          { label: '停用', value: 2 },
        ],
        buttonStyle: 'solid',
        optionType: 'button',
      },
      defaultValue: 1,
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'mesSelectVisible',
      label: 'MES产品',
      component: 'RadioGroup',
      componentProps: {
        options: [
          { label: '是', value: true },
          { label: '否', value: false },
        ],
        buttonStyle: 'solid',
        optionType: 'button',
      },
      defaultValue: false,
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
      fieldName: 'materialCode',
      label: '物料编码',
      component: 'Input',
      componentProps: { placeholder: '请输入物料编码' },
    },
    {
      fieldName: 'materialName',
      label: '物料名称',
      component: 'Input',
      componentProps: { placeholder: '请输入物料名称' },
    },
    {
      fieldName: 'specModel',
      label: '规格型号描述',
      component: 'Input',
      componentProps: { placeholder: '请输入规格型号描述' },
    },
    {
      fieldName: 'modelCode',
      label: '型号编码',
      component: 'Input',
      componentProps: { placeholder: '请输入型号编码' },
    },
    {
      fieldName: 'defaultRecipeCode',
      label: '配方',
      component: 'Input',
      componentProps: { placeholder: '请输入配方编码/型号编码' },
    },
    {
      fieldName: 'materialType',
      label: '物料类型',
      component: 'Select',
      componentProps: {
        options: materialTypeOptions,
        allowClear: true,
        placeholder: '请选择物料类型',
      },
    },
    {
      fieldName: 'materialStatus',
      label: '物料状态',
      component: 'Select',
      componentProps: {
        options: [
          { label: '草稿', value: 0 },
          { label: '启用', value: 1 },
          { label: '停用', value: 2 },
        ],
        allowClear: true,
        placeholder: '请选择物料状态',
      },
    },
    {
      fieldName: 'mesSelectVisible',
      label: 'MES产品',
      component: 'Select',
      componentProps: {
        options: [
          { label: '是', value: true },
          { label: '否', value: false },
        ],
        allowClear: true,
        placeholder: '请选择MES产品',
      },
    },
  ];
}

export function useGridColumns(): VxeTableGridOptions<MesHcMaterialApi.Material>['columns'] {
  return [
    { type: 'checkbox', width: 48, fixed: 'left' },
    { field: 'materialCode', title: '物料编码', minWidth: 140 },
    { field: 'materialName', title: '物料名称', minWidth: 180 },
    { field: 'materialShortName', title: '物料简称', minWidth: 140 },
    { field: 'materialCategoryName', title: '物料分类', minWidth: 160 },
    {
      field: 'materialType',
      title: '物料类型',
      minWidth: 120,
      formatter: ({ cellValue }) => materialTypeMap[String(cellValue)] || cellValue || '-',
    },
    {
      field: 'specModel',
      title: '规格型号描述',
      minWidth: 200,
      showOverflow: 'tooltip',
    },
    {
      field: 'modelCode',
      title: '型号编码',
      minWidth: 160,
      showOverflow: 'tooltip',
    },
    {
      field: 'defaultRecipeCode',
      title: '配方编码',
      minWidth: 160,
      showOverflow: 'tooltip',
    },
    {
      field: 'defaultRecipeName',
      title: '配方型号编码',
      minWidth: 180,
      showOverflow: 'tooltip',
    },
    {
      field: 'baseUnitCode',
      title: '基础单位',
      minWidth: 140,
      formatter: ({ row }) => [row.baseUnitCode || row.baseUom, row.baseUnitName].filter(Boolean).join('/') || '-',
    },
    {
      field: 'stockUnitCode',
      title: '库存单位',
      minWidth: 140,
      formatter: ({ row }) => [row.stockUnitCode || row.stockUom, row.stockUnitName].filter(Boolean).join('/') || '-',
    },
    {
      field: 'produceUnitCode',
      title: '生产单位',
      minWidth: 140,
      formatter: ({ row }) => [row.produceUnitCode || row.produceUom, row.produceUnitName].filter(Boolean).join('/') || '-',
    },
    {
      field: 'materialStatus',
      title: '物料状态',
      minWidth: 120,
      formatter: ({ cellValue }) =>
        ({ 0: '草稿', 1: '启用', 2: '停用' } as Record<string, string>)[
          String(cellValue)
        ] ||
        cellValue ||
        '-',
    },
    {
      field: 'mesSelectVisible',
      title: 'MES产品',
      minWidth: 120,
      align: 'center',
      formatter: ({ cellValue }) => (cellValue ? '是' : '否'),
    },
    { title: '操作', width: 160, fixed: 'right', slots: { default: 'actions' } },
  ];
}





