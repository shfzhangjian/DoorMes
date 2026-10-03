// src/views/mes/report/wip-board/data.ts
import type { FormSchema } from '#/adapter/form';
import type { VxeGridProps } from '#/adapter/vxe-table';

export function useGridFormSchema(): FormSchema[] {
  return [
    { fieldName: 'batchNo', label: '批号', component: 'Input', componentProps: { placeholder: '输入批次号模糊查询' } },
    { fieldName: 'materialName', label: '品名', component: 'Input', componentProps: { placeholder: '产品名称' } },
    {
      fieldName: 'processName', label: '当前工序', component: 'Select',
      componentProps: { options: [{ label: '原料配制', value: 'OP10' }, { label: '基材涂布', value: 'OP20' }, { label: '表面精抛', value: 'OP30' }], allowClear: true }
    },
    {
      fieldName: 'status', label: '批次状态', component: 'Select',
      componentProps: { options: [{ label: '加工中 (RUN)', value: 'RUN' }, { label: '排队中 (WAIT)', value: 'WAIT' }, { label: '已冻结 (HOLD)', value: 'HOLD' }], allowClear: true }
    },
  ];
}

export function useGridColumns(): VxeGridProps['columns'] {
  return [
    { type: 'seq', width: 50, align: 'center', fixed: 'left' },
    { field: 'batchNo', title: '流转批号', minWidth: 160, fixed: 'left', slots: { default: 'batchNo' } },
    { field: 'materialName', title: '物料品名', minWidth: 160 },
    { field: 'processName', title: '当前工序', width: 120, align: 'center' },
    { field: 'equipCode', title: '机台资源', width: 120, align: 'center' },
    { field: 'status', title: '状态', width: 100, align: 'center', slots: { default: 'status' } },
    { field: 'qty', title: '当前数量', width: 100, align: 'right', slots: { default: 'qty' } },
    { field: 'queueTime', title: '滞留时长(H)', width: 100, align: 'right', slots: { default: 'queueTime' } },
    { title: '操作', width: 120, fixed: 'right', slots: { default: 'actions' } },
  ];
}
