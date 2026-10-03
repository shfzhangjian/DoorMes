// 文件路径：src/views/mes/cost/base/wage-quota/data.ts

import { h } from 'vue';
import { Tag, Badge } from 'ant-design-vue';
import type { FormSchema } from '#/adapter/form';
import type { VxeGridProps } from '#/adapter/vxe-table';

export const WAGE_TYPE_OPTIONS = [
  { label: '计件单价 (元/件)', value: 1 },
  { label: '计时费率 (元/小时)', value: 2 },
];

export const STATUS_OPTIONS = [
  { label: '草稿', value: 0, color: 'processing' },
  { label: '已生效', value: 1, color: 'success' },
  { label: '历史版本', value: 2, color: 'default' },
];

export function useGridFormSchema(): FormSchema[] {
  return [
    { fieldName: 'productName', label: '产品名称/编码', component: 'Input', componentProps: { allowClear: true } },
    { fieldName: 'processName', label: '加工工序', component: 'Input', componentProps: { allowClear: true } },
    { fieldName: 'status', label: '状态', component: 'Select', componentProps: { allowClear: true, options: STATUS_OPTIONS } },
  ];
}

export function useGridColumns(): VxeGridProps['columns'] {
  return [
    { type: 'checkbox', width: 60, align: 'center' },
    { field: 'productCode', title: '产品编码', width: 140 },
    { field: 'productName', title: '产品名称', minWidth: 160 },
    { field: 'processName', title: '核算工序', width: 130 },
    {
      field: 'wageType', title: '计薪方式', width: 150, align: 'center',
      formatter: ({ cellValue }) => WAGE_TYPE_OPTIONS.find(o => o.value === cellValue)?.label || cellValue,
    },
    {
      field: 'unitPrice', title: '定额单价', width: 130, align: 'right',
      cellRender: ({ cellValue, row }) => {
        const suffix = row.wageType === 1 ? '件' : 'H';
        return h('span', { class: 'font-bold text-blue-600' }, `¥ ${Number(cellValue).toFixed(2)} / ${suffix}`);
      }
    },
    {
      field: 'version', title: '版本号', width: 100, align: 'center',
      cellRender: ({ cellValue, row }) => {
        // 突出当前生效版本的徽标
        return row.status === 1
          ? h(Badge, { count: cellValue, numberStyle: { backgroundColor: '#52c41a' } })
          : h('span', { class: 'text-gray-500 font-mono' }, cellValue);
      }
    },
    { field: 'effectiveDate', title: '生效日期', width: 120, align: 'center' },
    { field: 'expireDate', title: '失效日期', width: 120, align: 'center', formatter: ({ cellValue }) => cellValue || '长期有效' },
    { field: 'status', title: '状态', width: 100, align: 'center', slots: { default: 'status' } },
    { title: '操作', field: 'action', fixed: 'right', width: 220, slots: { default: 'actions' } },
  ];
}

export function useFormSchema(): FormSchema[] {
  return [
    { fieldName: 'id', component: 'Input', dependencies: { triggerFields: [''], show: () => false } },
    { fieldName: 'productCode', label: '产品编码', rules: 'required', component: 'Input' },
    { fieldName: 'productName', label: '产品名称', rules: 'required', component: 'Input' },
    { fieldName: 'processCode', label: '工序编码', rules: 'required', component: 'Input' },
    { fieldName: 'processName', label: '工序名称', rules: 'required', component: 'Input' },
    {
      fieldName: 'wageType', label: '计薪方式', rules: 'required', component: 'RadioGroup',
      defaultValue: 1, componentProps: { options: WAGE_TYPE_OPTIONS }
    },
    {
      fieldName: 'unitPrice', label: '定额单价', rules: 'required', component: 'InputNumber',
      componentProps: { min: 0, precision: 4, class: 'w-full', addonBefore: '¥' }
    },
    {
      fieldName: 'version', label: '版本号', rules: 'required', component: 'Input',
      defaultValue: 'V1.0', componentProps: { placeholder: '如 V1.0, V1.1' }
    },
    { fieldName: 'effectiveDate', label: '生效日期', rules: 'required', component: 'DatePicker', componentProps: { valueFormat: 'YYYY-MM-DD', class: 'w-full' } },
    { fieldName: 'remark', label: '版本说明', component: 'Textarea', formItemClass: 'col-span-2', componentProps: { rows: 2 } },
  ];
}
