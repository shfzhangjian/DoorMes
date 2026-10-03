// 文件路径：src/views/mes/cost/wage/monthly/data.ts

import { h } from 'vue';
import type { FormSchema } from '#/adapter/form';
import type { VxeGridProps } from '#/adapter/vxe-table';

export const STATUS_OPTIONS = [
  { label: '预结算(可重算)', value: 0, color: 'processing' },
  { label: '已审核封账', value: 1, color: 'success' },
];

export function useGridFormSchema(): FormSchema[] {
  return [
    { fieldName: 'period', label: '核算期间', component: 'DatePicker', componentProps: { picker: 'month', valueFormat: 'YYYY-MM', allowClear: false } },
    { fieldName: 'teamName', label: '班组名称', component: 'Input', componentProps: { allowClear: true } },
    { fieldName: 'empName', label: '工号/姓名', component: 'Input', componentProps: { allowClear: true } },
    { fieldName: 'status', label: '封账状态', component: 'Select', componentProps: { allowClear: true, options: STATUS_OPTIONS } },
  ];
}

const formatMoney = (val: number, isNegative = false, isPositive = false) => {
  const num = Number(val) || 0;
  if (num === 0) return h('span', { class: 'text-gray-400' }, '-');

  let cssClass = 'text-gray-700';
  let prefix = '';
  if (isNegative) { cssClass = 'text-red-600'; prefix = '- '; }
  else if (isPositive) { cssClass = 'text-green-600'; prefix = '+ '; }

  return h('span', { class: cssClass }, `${prefix}¥ ${num.toFixed(2)}`);
};

export function useGridColumns(): VxeGridProps['columns'] {
  return [
    { type: 'checkbox', width: 60, align: 'center', fixed: 'left' },
    { field: 'empNo', title: '工号', width: 100, fixed: 'left' },
    { field: 'empName', title: '姓名', width: 100, fixed: 'left', cellRender: ({ cellValue }) => h('span', { class: 'font-bold text-gray-800' }, cellValue) },
    { field: 'teamName', title: '归属班组', width: 120 },

    { title: '计件收益类 (元) [自动读取]', align: 'center', children: [
        { field: 'directPiece', title: '个人直接计件', width: 120, align: 'right', cellRender: ({ cellValue }) => formatMoney(cellValue) },
        { field: 'teamPiece', title: '班组二次分配', width: 120, align: 'right', cellRender: ({ cellValue }) => formatMoney(cellValue) },
      ]},

    { title: '计时与补贴类 (元)', align: 'center', children: [
        { field: 'timeBased', title: '标准计时底薪', width: 120, align: 'right', cellRender: ({ cellValue }) => formatMoney(cellValue) },
        { field: 'overtime', title: '加班费', width: 100, align: 'right', cellRender: ({ cellValue }) => formatMoney(cellValue) },
        { field: 'subsidy', title: '补贴 (支持修改)', width: 130, align: 'right', slots: { default: 'subsidy' } },
      ]},

    { title: '绩效奖惩明细 (元)', align: 'center', children: [
        { field: 'reward', title: '奖励 (支持修改)', width: 130, align: 'right', slots: { default: 'reward' } },
        { field: 'punish', title: '扣款 (支持修改)', width: 130, align: 'right', slots: { default: 'punish' } },
      ]},

    {
      field: 'grossWage', title: '税前生产总薪资', width: 150, align: 'right', fixed: 'right',
      cellRender: ({ cellValue }) => h('span', { class: 'text-blue-700 font-bold text-[16px]' }, `¥ ${Number(cellValue).toFixed(2)}`)
    },
    { field: 'status', title: '封账状态', width: 120, align: 'center', fixed: 'right', slots: { default: 'status' } },
  ];
}
