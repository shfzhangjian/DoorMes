// 文件路径：src/views/mes/cost/wage/performance/data.ts

import { h } from 'vue';
import { Tag } from 'ant-design-vue';
import type { FormSchema } from '#/adapter/form';
import type { VxeGridProps } from '#/adapter/vxe-table';

export const TYPE_OPTIONS = [
  { label: '奖励 (增项)', value: 1, color: 'success' },
  { label: '扣款 (减项)', value: 2, color: 'error' },
];

export const CATEGORY_OPTIONS = [
  { label: '质量表现', value: '质量表现' },
  { label: '5S考评', value: '5S考评' },
  { label: '生产效率', value: '生产效率' },
  { label: '考勤纪律', value: '考勤纪律' },
  { label: '其它', value: '其它' },
];

export const SOURCE_OPTIONS = [
  { label: '手工录入', value: 1, color: 'blue' },
  { label: '质检系统推送', value: 2, color: 'purple' },
  { label: '考勤系统推送', value: 3, color: 'orange' },
];

export const STATUS_OPTIONS = [
  { label: '待确认', value: 0, color: 'processing' },
  { label: '已审核锁定', value: 1, color: 'success' },
];

export function useGridFormSchema(): FormSchema[] {
  return [
    { fieldName: 'empName', label: '工号/姓名', component: 'Input', componentProps: { allowClear: true } },
    { fieldName: 'type', label: '奖惩类型', component: 'Select', componentProps: { allowClear: true, options: TYPE_OPTIONS } },
    { fieldName: 'status', label: '状态', component: 'Select', componentProps: { allowClear: true, options: STATUS_OPTIONS } },
  ];
}

export function useGridColumns(): VxeGridProps['columns'] {
  return [
    { type: 'checkbox', width: 60, align: 'center' },
    { field: 'recordDate', title: '发生日期', width: 120, align: 'center' },
    { field: 'empNo', title: '员工工号', width: 120 },
    { field: 'empName', title: '员工姓名', width: 120, cellRender: ({ cellValue }) => h('span', { class: 'font-semibold' }, cellValue) },
    {
      field: 'type', title: '奖惩类型', width: 110, align: 'center',
      cellRender: ({ cellValue }) => {
        const opt = TYPE_OPTIONS.find(o => o.value === cellValue);
        return h(Tag, { color: opt?.color || 'default', class: 'm-0 border-0' }, () => opt?.label || cellValue);
      }
    },
    { field: 'category', title: '考评类目', width: 110, align: 'center' },
    {
      field: 'amount', title: '金额(元)', width: 140, align: 'right',
      cellRender: ({ cellValue, row }) => {
        const isReward = row.type === 1;
        const sign = isReward ? '+ ' : '- ';
        const cssClass = isReward ? 'text-green-600 font-bold' : 'text-red-600 font-bold';
        return h('span', { class: cssClass }, `${sign}¥ ${Number(cellValue).toFixed(2)}`);
      }
    },
    {
      field: 'source', title: '数据来源', width: 130, align: 'center',
      cellRender: ({ cellValue }) => {
        const opt = SOURCE_OPTIONS.find(o => o.value === cellValue);
        return h(Tag, { color: opt?.color || 'default', class: 'm-0' }, () => opt?.label || '未知');
      }
    },
    { field: 'status', title: '核对状态', width: 110, align: 'center', slots: { default: 'status' } },
    { field: 'reason', title: '事由说明', minWidth: 200 },
    { title: '操作', field: 'action', fixed: 'right', width: 140, slots: { default: 'actions' } },
  ];
}

export function useFormSchema(): FormSchema[] {
  return [
    { fieldName: 'id', component: 'Input', dependencies: { triggerFields: [''], show: () => false } },
    { fieldName: 'recordDate', label: '发生日期', rules: 'required', component: 'DatePicker', componentProps: { valueFormat: 'YYYY-MM-DD', class: 'w-full' } },
    { fieldName: 'empNo', label: '员工工号', rules: 'required', component: 'Input' },
    { fieldName: 'empName', label: '员工姓名', rules: 'required', component: 'Input' },
    {
      fieldName: 'type', label: '奖惩类型', rules: 'required', component: 'RadioGroup',
      defaultValue: 1, componentProps: { options: TYPE_OPTIONS }
    },
    {
      fieldName: 'category', label: '考评类目', rules: 'required', component: 'Select',
      componentProps: { options: CATEGORY_OPTIONS }
    },
    {
      fieldName: 'amount', label: '涉及金额', rules: 'required', component: 'InputNumber',
      componentProps: { min: 0, precision: 2, class: 'w-full', addonBefore: '¥' }
    },
    { fieldName: 'reason', label: '事由说明', rules: 'required', component: 'Textarea', formItemClass: 'col-span-2', componentProps: { rows: 3 } },
  ];
}
