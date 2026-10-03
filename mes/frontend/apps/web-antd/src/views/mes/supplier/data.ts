import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesSupplierApi } from '#/api/mes/supplier';

export const SUPPLIER_STATUS_OPTIONS = [
  { label: '合格', value: 'QUALIFIED' },
  { label: '不合格', value: 'UNQUALIFIED' },
];

export const SUPPLIER_MATERIAL_CATEGORY_OPTIONS = [
  { label: 'A', value: 'A' },
  { label: 'B', value: 'B' },
  { label: 'C', value: 'C' },
  { label: 'D', value: 'D' },
];

export function useFormSchema(hiddenFields: string[] = []): VbenFormSchema[] {
  const hiddenFieldSet = new Set(hiddenFields);

  return [
    {
      component: 'Input',
      dependencies: { show: () => false, triggerFields: [''] },
      fieldName: 'id',
    },
    {
      component: 'Input',
      componentProps: { placeholder: '请输入使用部门' },
      fieldName: 'usingDepartment',
      label: '使用部门',
    },
    {
      component: 'Input',
      componentProps: { placeholder: '请输入供应商名称' },
      fieldName: 'supplierName',
      label: '供应商名称',
      rules: 'required',
    },
    {
      component: 'Input',
      componentProps: { placeholder: '请输入供应商代码' },
      fieldName: 'supplierCode',
      label: '供应商代码',
      rules: 'required',
    },
    {
      component: 'Input',
      componentProps: { placeholder: '请输入联系人' },
      fieldName: 'contactPerson',
      label: '联系人',
    },
    {
      component: 'Input',
      componentProps: { placeholder: '请输入联系电话' },
      fieldName: 'contactPhone',
      label: '联系电话',
    },
    {
      component: 'Input',
      componentProps: { placeholder: '请输入供应商地址' },
      fieldName: 'address',
      formItemClass: 'col-span-2',
      label: '供应商地址',
    },
    {
      component: 'Input',
      componentProps: { placeholder: '请输入企业性质' },
      fieldName: 'companyNature',
      label: '企业性质',
    },
    {
      component: 'Input',
      componentProps: { placeholder: '请输入产地' },
      fieldName: 'originPlace',
      label: '产地',
    },
    {
      component: 'Input',
      componentProps: { placeholder: '请输入原厂信息' },
      fieldName: 'originalFactoryInfo',
      formItemClass: 'col-span-2',
      label: '原厂信息',
    },
    {
      component: 'Input',
      componentProps: { placeholder: '请输入提供/协作产品' },
      fieldName: 'providedProduct',
      label: '提供/协作产品',
    },
    {
      component: 'Input',
      componentProps: { placeholder: '请输入型号' },
      fieldName: 'model',
      label: '型号',
    },
    {
      component: 'Input',
      componentProps: { placeholder: '请输入物料代码' },
      fieldName: 'materialCode',
      label: '物料代码',
    },
    {
      component: 'Input',
      componentProps: { placeholder: '请输入适用产品' },
      fieldName: 'applicableProduct',
      label: '适用产品',
    },
    {
      component: 'DatePicker',
      componentProps: { class: 'w-full', valueFormat: 'YYYY-MM-DD' },
      fieldName: 'importDate',
      label: '导入日期',
    },
    {
      component: 'Select',
      componentProps: { options: SUPPLIER_MATERIAL_CATEGORY_OPTIONS },
      fieldName: 'materialCategory',
      label: '物料类别',
    },
    {
      component: 'Select',
      componentProps: { options: SUPPLIER_STATUS_OPTIONS },
      defaultValue: 'QUALIFIED',
      fieldName: 'status',
      label: '供应商状态',
      rules: 'required',
    },
    {
      component: 'InputNumber',
      componentProps: { class: 'w-full', min: 0, precision: 0 },
      defaultValue: 0,
      fieldName: 'sort',
      hide: hiddenFieldSet.has('sort'),
      label: '排序',
    },
  ];
}

export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      component: 'Input',
      componentProps: { allowClear: true },
      fieldName: 'usingDepartment',
      label: '使用部门',
    },
    {
      component: 'Input',
      componentProps: { allowClear: true },
      fieldName: 'supplierName',
      label: '供应商名称',
    },
    {
      component: 'Input',
      componentProps: { allowClear: true },
      fieldName: 'supplierCode',
      label: '供应商代码',
    },
    {
      component: 'Input',
      componentProps: { allowClear: true },
      fieldName: 'materialCode',
      label: '物料代码',
    },
    {
      component: 'Select',
      componentProps: {
        allowClear: true,
        options: SUPPLIER_MATERIAL_CATEGORY_OPTIONS,
      },
      fieldName: 'materialCategory',
      label: '物料类别',
    },
    {
      component: 'Select',
      componentProps: { allowClear: true, options: SUPPLIER_STATUS_OPTIONS },
      fieldName: 'status',
      label: '供应商状态',
    },
    {
      component: 'RangePicker',
      componentProps: {
        allowClear: true,
        format: 'YYYY-MM-DD',
        valueFormat: 'YYYY-MM-DD',
      },
      fieldName: 'importDate',
      label: '导入日期',
    },
  ];
}

export function useGridColumns(): VxeTableGridOptions<MesSupplierApi.Supplier>['columns'] {
  return [
    { fixed: 'left', type: 'checkbox', width: 40 },
    {
      field: 'usingDepartment',
      fixed: 'left',
      minWidth: 120,
      title: '使用部门',
    },
    {
      field: 'supplierName',
      fixed: 'left',
      minWidth: 180,
      title: '供应商名称',
    },
    {
      field: 'supplierCode',
      fixed: 'left',
      minWidth: 140,
      title: '供应商代码',
    },
    { field: 'contactPerson', minWidth: 100, title: '联系人' },
    { field: 'contactPhone', minWidth: 130, title: '联系电话' },
    { field: 'address', minWidth: 220, title: '供应商地址' },
    { field: 'companyNature', minWidth: 130, title: '企业性质' },
    { field: 'originPlace', minWidth: 120, title: '产地' },
    { field: 'originalFactoryInfo', minWidth: 180, title: '原厂信息' },
    { field: 'providedProduct', minWidth: 180, title: '提供/协作产品' },
    { field: 'model', minWidth: 140, title: '型号' },
    { field: 'materialCode', minWidth: 140, title: '物料代码' },
    { field: 'applicableProduct', minWidth: 180, title: '适用产品' },
    { align: 'center', field: 'importDate', title: '导入日期', width: 120 },
    { field: 'materialCategory', minWidth: 120, title: '物料类别' },
    {
      align: 'center',
      field: 'status',
      formatter: ({ cellValue }) =>
        SUPPLIER_STATUS_OPTIONS.find((item) => item.value === cellValue)
          ?.label ||
        cellValue ||
        '-',
      title: '供应商状态',
      width: 110,
    },
    {
      fixed: 'right',
      slots: { default: 'actions' },
      title: '操作',
      width: 150,
    },
  ];
}
