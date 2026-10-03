import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesProcessApi } from '#/api/mes/base/process';

export function useFormSchema(): VbenFormSchema[] {
  return [
    {
      fieldName: 'id',
      component: 'Input',
      dependencies: { triggerFields: [''], show: () => false },
    },
    {
      fieldName: 'code',
      label: '工序编码',
      rules: 'required',
      component: 'Input',
      componentProps: { placeholder: '请输入工序编码' },
    },
    {
      fieldName: 'name',
      label: '工序名称',
      rules: 'required',
      component: 'Input',
      componentProps: { placeholder: '请输入工序名称' },
    },
  ];
}

/** 列表的搜索表单 */
export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      fieldName: 'code',
      label: '工序编码',
      component: 'Input',
      componentProps: { allowClear: true, placeholder: '请输入工序编码' },
    },
    {
      fieldName: 'name',
      label: '工序名称',
      component: 'Input',
      componentProps: { allowClear: true, placeholder: '请输入工序名称' },
    },
  ];
}

/** 列表的字段 */
export function useGridColumns(): VxeTableGridOptions<MesProcessApi.Process>['columns'] {
  return [
    { type: 'checkbox', width: 40 },
    { field: 'id', title: '主键ID', minWidth: 80, visible: false },
    { field: 'code', title: '工序编码', minWidth: 120, fixed: 'left' },
    { field: 'name', title: '工序名称', minWidth: 140, fixed: 'left' },
    {
      title: '操作',
      width: 200,
      fixed: 'right',
      slots: { default: 'actions' },
    },
  ];
}
