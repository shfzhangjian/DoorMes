import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import { h } from 'vue';
import { Tag } from 'ant-design-vue';

export function useGridFormSchema(): VbenFormSchema[] {
  return [
    { fieldName: 'supplierName', label: '供应商名称', component: 'Input', componentProps: { placeholder: '模糊搜索' } },
    { fieldName: 'planTitle', label: '所属考核计划', component: 'Input', componentProps: { placeholder: '计划标题' } },
  ];
}

export function useGridColumns(): VxeTableGridOptions['columns'] {
  return [
    { type: 'seq', title: '序号', width: 60, align: 'center' },
    { field: 'recordNo', title: '评分单号', width: 140, slots: { default: 'recordNo' } },
    { field: 'supplierName', title: '被考评供应商', minWidth: 200, align: 'left', slots: { default: 'supplierName' } },
    { field: 'planTitle', title: '所属计划名称', minWidth: 220, align: 'left' },
    { field: 'status', title: '阅卷进度', width: 100, align: 'center', slots: { default: 'status' } },
    { field: 'totalScore', title: '最终总分', width: 100, align: 'center', slots: { default: 'totalScore' } },
    { field: 'evalGrade', title: '评级', width: 80, align: 'center', slots: { default: 'evalGrade' } },
    { title: '操作', width: 120, fixed: 'right', slots: { default: 'actions' } },
  ];
}
