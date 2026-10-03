// 文件路径：src/views/mes/cost/wage/distribution/data.ts

import { h } from 'vue';
import type { FormSchema } from '#/adapter/form';
import type { VxeGridProps } from '#/adapter/vxe-table';

export function useGridFormSchema(): FormSchema[] {
  return [
    { fieldName: 'empName', label: '工号/姓名', component: 'Input', componentProps: { allowClear: true } },
  ];
}

export function useGridColumns(): VxeGridProps['columns'] {
  return [
    { field: 'empNo', title: '员工工号', width: 110, fixed: 'left' },
    { field: 'empName', title: '员工姓名', width: 100, cellRender: ({ cellValue }) => h('span', { class: 'font-semibold' }, cellValue) },
    { field: 'role', title: '岗位角色', width: 100, align: 'center' },

    { title: '基准数据', align: 'center', children: [
        { field: 'baseHours', title: '有效工时 (A)', width: 100, align: 'right', formatter: ({ cellValue }) => `${cellValue} H` },
        { field: 'weight', title: '岗位权重 (B)', width: 100, align: 'right' },
        { field: 'weightedHours', title: '加权工时 (C=A×B)', width: 130, align: 'right', cellRender: ({ cellValue }) => h('span', { class: 'text-blue-600 font-bold' }, `${cellValue.toFixed(1)} H`) },
      ]},

    { title: '金额分配结果 (元)', align: 'center', children: [
        { field: 'calcAmount', title: '系统分配 (D)', width: 120, align: 'right', cellRender: ({ cellValue }) => h('span', { class: 'text-gray-700' }, `¥ ${cellValue.toFixed(2)}`) },
        { field: 'adjustAmount', title: '手工微调 (E)', width: 120, align: 'right', slots: { default: 'adjustAmount' } }, // 插槽编辑
        { field: 'finalAmount', title: '最终分配 (F=D+E)', width: 140, align: 'right', cellRender: ({ cellValue }) => h('span', { class: 'text-green-600 font-bold text-[15px]' }, `¥ ${cellValue.toFixed(2)}`) },
      ]},

    { field: 'remark', title: '调整备注', minWidth: 150, align: 'left', slots: { default: 'remark' } },
  ];
}
