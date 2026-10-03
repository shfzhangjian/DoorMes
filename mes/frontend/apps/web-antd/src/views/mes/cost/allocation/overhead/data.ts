// 文件路径：src/views/mes/cost/allocation/overhead/data.ts

import { h } from 'vue';
import { Tag } from 'ant-design-vue';
import type { FormSchema } from '#/adapter/form';
import type { VxeGridProps } from '#/adapter/vxe-table';
import { getCostCenterList } from '#/api/mes/cost/base/cost-center';
import { handleTree } from '@vben/utils';

// 常量字典模拟
export const COST_ELEMENT_OPTIONS = [
  { label: '机器折旧费', value: 1 },
  { label: '水电动力费', value: 2 },
  { label: '间接人工费', value: 3 },
  { label: '机物料消耗', value: 4 },
  { label: '其他制造费', value: 5 },
];

export const DATA_SOURCE_OPTIONS = [
  { label: '手工录入', value: 1, color: 'blue' },
  { label: 'ERP同步', value: 2, color: 'purple' },
];

export const STATUS_OPTIONS = [
  { label: '草稿', value: 0, color: 'default' },
  { label: '已过账', value: 1, color: 'success' },
];

export function useGridFormSchema(): FormSchema[] {
  return [
    // 移除了 period 字段，由左侧树接管
    {
      fieldName: 'costCenterId',
      label: '成本中心',
      component: 'ApiTreeSelect',
      componentProps: {
        allowClear: true,
        api: async () => handleTree(await getCostCenterList({})),
        labelField: 'name', valueField: 'id',
      },
    },
    {
      fieldName: 'elementCode',
      label: '费用要素',
      component: 'Select',
      componentProps: { allowClear: true, options: COST_ELEMENT_OPTIONS },
    },
    {
      fieldName: 'status',
      label: '状态',
      component: 'Select',
      componentProps: { allowClear: true, options: STATUS_OPTIONS },
    },
  ];
}

export function useGridColumns(): VxeGridProps['columns'] {
  return [
    { type: 'checkbox', width: 60, align: 'center' },
    { field: 'period', title: '核算期间', width: 120, align: 'center' },
    { field: 'costCenterId', title: '成本中心', minWidth: 180 }, // 真实开发中应显示名称
    {
      field: 'elementCode', title: '费用要素', width: 140,
      formatter: ({ cellValue }) => COST_ELEMENT_OPTIONS.find(o => o.value === cellValue)?.label || cellValue,
    },
    {
      field: 'amount', title: '归集金额(元)', width: 140, align: 'right',
      formatter: ({ cellValue }) => Number(cellValue).toFixed(2),
    },
    {
      field: 'source', title: '数据来源', width: 100, align: 'center',
      cellRender: ({ cellValue }) => {
        const opt = DATA_SOURCE_OPTIONS.find(o => o.value === cellValue);
        return h(Tag, { color: opt?.color || 'default' }, () => opt?.label || cellValue);
      }
    },
    {
      field: 'status', title: '状态', width: 100, align: 'center',
      cellRender: ({ cellValue }) => {
        const opt = STATUS_OPTIONS.find(o => o.value === cellValue);
        return h(Tag, { color: opt?.color || 'default' }, () => opt?.label || '未知');
      }
    },
    { field: 'remark', title: '备注说明', minWidth: 200 },
    { title: '操作', field: 'action', fixed: 'right', width: 220, slots: { default: 'actions' } },
  ];
}

export function useFormSchema(): FormSchema[] {
  return [
    { fieldName: 'id', component: 'Input', dependencies: { triggerFields: [''], show: () => false } },
    {
      fieldName: 'period',
      label: '核算期间',
      rules: 'required',
      component: 'DatePicker',
      componentProps: { picker: 'month', valueFormat: 'YYYY-MM', class: 'w-full' },
    },
    {
      fieldName: 'costCenterId',
      label: '成本中心',
      rules: 'required',
      component: 'ApiTreeSelect',
      componentProps: {
        api: async () => handleTree(await getCostCenterList({})),
        labelField: 'name', valueField: 'id', childrenField: 'children', treeDefaultExpandAll: true,
      },
    },
    {
      fieldName: 'elementCode',
      label: '费用要素',
      rules: 'required',
      component: 'Select',
      componentProps: { options: COST_ELEMENT_OPTIONS },
    },
    {
      fieldName: 'amount',
      label: '金额(元)',
      rules: 'required',
      component: 'InputNumber',
      componentProps: { min: 0, precision: 2, class: 'w-full', addonAfter: '¥' },
    },
    {
      fieldName: 'remark',
      label: '备注说明',
      component: 'Textarea',
      formItemClass: 'col-span-2',
      componentProps: { rows: 3 },
    },
  ];
}
