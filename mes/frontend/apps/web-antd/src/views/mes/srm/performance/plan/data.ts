import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import { h } from 'vue';
import { Tag } from 'ant-design-vue';

export function useGridFormSchema(): VbenFormSchema[] {
  return [
    { fieldName: 'planNo', label: '计划单号', component: 'Input', componentProps: { placeholder: '请输入单号' } },
    { fieldName: 'title', label: '计划标题', component: 'Input', componentProps: { placeholder: '模糊搜索' } },
    {
      fieldName: 'status', label: '计划状态', component: 'Select',
      componentProps: { options: [{label: '草稿', value: 0}, {label: '进行中', value: 1}, {label: '已完结', value: 2}], allowClear: true }
    },
  ];
}

export function useGridColumns(): VxeTableGridOptions['columns'] {
  return [
    { type: 'seq', title: '序号', width: 60, align: 'center' },
    { field: 'planNo', title: '计划单号', width: 150, slots: { default: 'planNo' } },
    { field: 'title', title: '计划标题', minWidth: 220, align: 'left', slots: { default: 'title' } },
    { field: 'templateName', title: '考核采用模板', minWidth: 200, align: 'left' },
    {
      field: 'periodType', title: '考核周期', width: 120, align: 'center',
      slots: {
        default: ({ row }) => {
          const text = row.periodType === 'QUARTER' ? `${row.evalYear}年 Q${row.evalQuarter}` : row.periodType === 'YEAR' ? `${row.evalYear}年度` : '专项稽核';
          return h(Tag, { color: 'blue', class: 'font-bold !m-0 border-none' }, () => text);
        }
      }
    },
    { field: 'deadline', title: '打分截止日期', width: 120, align: 'center' },
    { field: 'status', title: '状态', width: 100, align: 'center', slots: { default: 'status' } },
    { title: '操作', width: 180, fixed: 'right', slots: { default: 'actions' } },
  ];
}
