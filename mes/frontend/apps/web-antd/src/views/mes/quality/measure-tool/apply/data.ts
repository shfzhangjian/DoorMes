import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { QmsMeasureToolApi } from '#/api/mes/quality/measure-tool';

import { APPLY_STATUS_OPTIONS, optionLabel } from '../shared';

type Option = { label?: string; value?: number };

export function useFormSchema(categoryOptions: Option[] = []): VbenFormSchema[] {
  return [
    { component: 'Input', dependencies: { show: () => false, triggerFields: [''] }, fieldName: 'id' },
    {
      component: 'Input',
      componentProps: { placeholder: '请输入量检具名称' },
      fieldName: 'toolName',
      label: '量检具名称',
      rules: 'required',
    },
    {
      component: 'Select',
      componentProps: { allowClear: true, options: categoryOptions, showSearch: true },
      fieldName: 'categoryId',
      label: '分类',
    },
    { component: 'Input', componentProps: { placeholder: '请输入型号' }, fieldName: 'model', label: '型号' },
    { component: 'Input', componentProps: { placeholder: '请输入规格' }, fieldName: 'specification', label: '规格' },
    { component: 'Input', componentProps: { placeholder: '请输入精度' }, fieldName: 'accuracy', label: '精度' },
    { component: 'Input', componentProps: { placeholder: '请输入量程' }, fieldName: 'measureRange', label: '量程' },
    { component: 'Input', componentProps: { placeholder: '请输入制造商/品牌' }, fieldName: 'manufacturer', label: '制造商' },
    {
      component: 'DatePicker',
      componentProps: { class: 'w-full', valueFormat: 'YYYY-MM-DD' },
      fieldName: 'purchaseDate',
      label: '采购日期',
    },
    {
      component: 'InputNumber',
      componentProps: { class: 'w-full', min: 1, precision: 0 },
      defaultValue: 12,
      fieldName: 'calibrationCycleMonths',
      label: '校准周期(月)',
    },
    {
      component: 'InputNumber',
      componentProps: { class: 'w-full', min: 0, precision: 0 },
      defaultValue: 30,
      fieldName: 'warningDays',
      label: '预警天数',
    },
    { component: 'Input', componentProps: { placeholder: '请输入使用部门' }, fieldName: 'usingDepartment', label: '使用部门' },
    { component: 'Input', componentProps: { placeholder: '请输入保管人' }, fieldName: 'keeperName', label: '保管人' },
    { component: 'Input', componentProps: { placeholder: '请输入存放位置' }, fieldName: 'storageLocation', label: '存放位置' },
    { component: 'Input', componentProps: { placeholder: '请输入申请部门' }, fieldName: 'applyDepartment', label: '申请部门' },
    { component: 'Input', componentProps: { placeholder: '请输入申请人' }, fieldName: 'applicantName', label: '申请人' },
    {
      component: 'Textarea',
      componentProps: { placeholder: '请输入申请原因', rows: 3 },
      fieldName: 'applyReason',
      formItemClass: 'col-span-2',
      label: '申请原因',
    },
    {
      component: 'Textarea',
      componentProps: { placeholder: '请输入备注', rows: 3 },
      fieldName: 'remark',
      formItemClass: 'col-span-2',
      label: '备注',
    },
  ];
}

export function useGridFormSchema(categoryOptions: Option[] = []): VbenFormSchema[] {
  return [
    { component: 'Input', componentProps: { allowClear: true }, fieldName: 'applyNo', label: '申请单号' },
    { component: 'Input', componentProps: { allowClear: true }, fieldName: 'toolName', label: '量检具名称' },
    {
      component: 'Select',
      componentProps: { allowClear: true, options: categoryOptions, showSearch: true },
      fieldName: 'categoryId',
      label: '分类',
    },
    { component: 'Input', componentProps: { allowClear: true }, fieldName: 'applyDepartment', label: '申请部门' },
    { component: 'Input', componentProps: { allowClear: true }, fieldName: 'applicantName', label: '申请人' },
    {
      component: 'Select',
      componentProps: { allowClear: true, options: APPLY_STATUS_OPTIONS },
      fieldName: 'status',
      label: '状态',
    },
  ];
}

export function useGridColumns(): VxeTableGridOptions<QmsMeasureToolApi.Apply>['columns'] {
  return [
    { fixed: 'left', type: 'checkbox', width: 40 },
    { field: 'applyNo', fixed: 'left', minWidth: 170, title: '申请单号' },
    { field: 'toolName', fixed: 'left', minWidth: 180, title: '量检具名称' },
    { field: 'categoryName', minWidth: 140, title: '分类' },
    { field: 'model', minWidth: 120, title: '型号' },
    { field: 'specification', minWidth: 120, title: '规格' },
    { field: 'accuracy', minWidth: 100, title: '精度' },
    { field: 'measureRange', minWidth: 140, title: '量程' },
    { align: 'center', field: 'purchaseDate', title: '采购日期', width: 120 },
    { align: 'center', field: 'calibrationCycleMonths', title: '周期(月)', width: 90 },
    { field: 'usingDepartment', minWidth: 130, title: '使用部门' },
    { field: 'keeperName', minWidth: 100, title: '保管人' },
    { field: 'applyDepartment', minWidth: 130, title: '申请部门' },
    { field: 'applicantName', minWidth: 100, title: '申请人' },
    {
      align: 'center',
      field: 'status',
      formatter: ({ cellValue }) => optionLabel(APPLY_STATUS_OPTIONS, cellValue),
      title: '状态',
      width: 100,
    },
    { field: 'assignedToolCode', minWidth: 150, title: '分配编码' },
    { fixed: 'right', slots: { default: 'actions' }, title: '操作', width: 260 },
  ];
}
