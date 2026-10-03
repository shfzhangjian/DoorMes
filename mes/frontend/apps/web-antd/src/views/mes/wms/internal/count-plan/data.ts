import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import { getRangePickerDefaultProps } from '#/utils';

export function useGridFormSchema(): VbenFormSchema[] {
  return [
    { fieldName: 'planNo', label: '盘点单号', component: 'Input', componentProps: { placeholder: '单号', allowClear: true } },
    {
      fieldName: 'status', label: '单据状态', component: 'Select',
      componentProps: { options: [{ label: '草稿', value: 'DRAFT' }, { label: '作业中', value: 'COUNTING' }, { label: '已完成', value: 'COMPLETED' }], allowClear: true },
    },
    { fieldName: 'createTime', label: '创建时间', component: 'RangePicker', componentProps: { ...getRangePickerDefaultProps(), allowClear: true } },
  ];
}

export function useGridColumns(): VxeTableGridOptions['columns'] {
  return [
    { type: 'checkbox', width: 40 },
    { field: 'planNo', title: '盘点单号', minWidth: 160, slots: { default: 'planNo' } },
    { field: 'type', title: '盘点类型', minWidth: 140 },
    { field: 'scopeDesc', title: '范围策略', minWidth: 160 },
    { field: 'status', title: '状态', minWidth: 100, align: 'center', slots: { default: 'status' } },
    { field: 'progress', title: '进度', minWidth: 150, slots: { default: 'progress' } },
    { field: 'creator', title: '创建人', minWidth: 100 },
    { field: 'createTime', title: '创建时间', minWidth: 160 },
    { title: '操作', width: 180, fixed: 'right', slots: { default: 'actions' } },
  ];
}

// 💡 彻底剥离 editRender，直接使用 default 插槽挂载原生组件
export function useDetailColumns(isDraft: boolean): VxeTableGridOptions['columns'] {
  const cols: VxeTableGridOptions['columns'] = [
    { type: 'checkbox', width: 40 },
    { field: 'location', title: '物理库位', minWidth: 160, slots: { default: 'location' } },
    { field: 'productCode', title: '物料编码', minWidth: 160, slots: { default: 'productCode' } },
    { field: 'productName', title: '物料名称', minWidth: 140 },
    { field: 'sysQty', title: '账面数量(Sys)', minWidth: 100, align: 'right' },
    { field: 'actualQty', title: '实盘数(Act)', minWidth: 100, align: 'right', slots: { default: 'actualQty' } },
    { field: 'variance', title: '差异(Var)', minWidth: 90, align: 'right', slots: { default: 'variance' } },
    { field: 'status', title: '状态', minWidth: 90, align: 'center', slots: { default: 'status' } },
  ];
  if (isDraft) {
    cols.push({ title: '操作', field: 'action', width: 80, align: 'center', fixed: 'right', slots: { default: 'action' } });
  }
  return cols;
}

export function useCreateFormSchema(): VbenFormSchema[] { return []; }
