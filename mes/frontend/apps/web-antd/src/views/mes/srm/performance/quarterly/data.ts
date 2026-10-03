import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';

export function useGridFormSchema(): VbenFormSchema[] {
  return [
    { fieldName: 'supplierName', label: '供应商名称', component: 'Input', componentProps: { placeholder: '模糊搜索' } },
    { fieldName: 'evalYear', label: '考核年份', component: 'Select', componentProps: { options: [{label:'2025',value:2025},{label:'2026',value:2026}] } },
    { fieldName: 'evalQuarter', label: '考核季度', component: 'Select', componentProps: { options: [{label:'Q1',value:1},{label:'Q2',value:2},{label:'Q3',value:3},{label:'Q4',value:4}] } },
  ];
}

export function useGridColumns(): VxeTableGridOptions['columns'] {
  return [
    { type: 'seq', title: '序号', width: 60, align: 'center' },
    { field: 'reportNo', title: '报告编号', width: 160, slots: { default: 'reportNo' } },
    { field: 'supplierName', title: '供应商名称', minWidth: 200 },
    { field: 'evalYear', title: '年份', width: 80, align: 'center' },
    { field: 'evalQuarter', title: '季度', width: 80, align: 'center', formatter: ({cellValue}) => `Q${cellValue}` },
    { field: 'totalScore', title: '季度综合得分', width: 120, align: 'center', slots: { default: 'totalScore' } },
    { field: 'evalGrade', title: '综合评级', width: 100, align: 'center', slots: { default: 'evalGrade' } },
    { field: 'status', title: '审批状态', width: 120, align: 'center', slots: { default: 'status' } },
    { title: '操作', width: 120, fixed: 'right', slots: { default: 'actions' } },
  ];
}
