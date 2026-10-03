// 文件路径：src/views/mes/cost/wage/team-weight/data.ts

import { h } from 'vue';
import { Tag } from 'ant-design-vue';
import type { FormSchema } from '#/adapter/form';
import type { VxeGridProps } from '#/adapter/vxe-table';

export const STATUS_OPTIONS = [
  { label: '在位(参与分配)', value: 1, color: 'success' },
  { label: '借调/离岗(暂停分配)', value: 0, color: 'warning' },
];

export const ROLE_OPTIONS = [
  { label: '线长/班长', value: '线长' },
  { label: '核心/熟练工', value: '熟练操作工' },
  { label: '普通操作工', value: '普通操作工' },
  { label: '学徒/辅助', value: '学徒/辅助' },
  { label: '技术调试员', value: '技术员' },
];

export function useGridFormSchema(): FormSchema[] {
  return [
    { fieldName: 'empName', label: '工号/姓名', component: 'Input', componentProps: { allowClear: true, placeholder: '输入工号或姓名查询' } },
    { fieldName: 'status', label: '在岗状态', component: 'Select', componentProps: { allowClear: true, options: STATUS_OPTIONS } },
  ];
}

export function useGridColumns(): VxeGridProps['columns'] {
  return [
    { type: 'checkbox', width: 60, align: 'center' },
    { field: 'empNo', title: '员工工号', width: 120 },
    { field: 'empName', title: '员工姓名', minWidth: 120, cellRender: ({ cellValue }) => h('span', { class: 'font-semibold' }, cellValue) },
    { field: 'role', title: '班组角色', width: 140, align: 'center' },
    {
      field: 'weight',
      title: '计件分配权重系数',
      width: 180,
      align: 'center',
      slots: { default: 'weight' } // 交给 Vue 插槽实现行内编辑
    },
    { field: 'joinDate', title: '入组日期', width: 120, align: 'center' },
    {
      field: 'status',
      title: '当前状态',
      width: 140,
      align: 'center',
      cellRender: ({ cellValue }) => {
        const opt = STATUS_OPTIONS.find(o => o.value === cellValue);
        return h(Tag, { color: opt?.color || 'default', class: 'm-0 border-0' }, () => opt?.label || '未知');
      }
    },
    { title: '操作', field: 'action', fixed: 'right', width: 150, slots: { default: 'actions' } },
  ];
}

export function useFormSchema(): FormSchema[] {
  return [
    { fieldName: 'id', component: 'Input', dependencies: { triggerFields: [''], show: () => false } },
    { fieldName: 'teamId', component: 'Input', dependencies: { triggerFields: [''], show: () => false } }, // 隐式传递归属班组
    { fieldName: 'empNo', label: '员工工号', rules: 'required', component: 'Input' },
    { fieldName: 'empName', label: '员工姓名', rules: 'required', component: 'Input' },
    {
      fieldName: 'role', label: '班组角色', rules: 'required', component: 'Select',
      componentProps: { options: ROLE_OPTIONS }
    },
    {
      fieldName: 'weight', label: '分配权重', rules: 'required', component: 'InputNumber',
      defaultValue: 1.0, componentProps: { min: 0.1, max: 5.0, precision: 1, step: 0.1, class: 'w-full' },
      help: '基准系数为 1.0。大于 1 表示分得多，小于 1 表示分得少。'
    },
    {
      fieldName: 'status', label: '当前状态', rules: 'required', component: 'RadioGroup',
      defaultValue: 1, componentProps: { options: STATUS_OPTIONS }
    },
  ];
}
