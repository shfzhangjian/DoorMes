// 文件路径：src/views/mes/cost/allocation/equivalent/data.ts

import { h } from 'vue';
import type { FormSchema } from '#/adapter/form';
import type { VxeGridProps } from '#/adapter/vxe-table';

export function useGridFormSchema(): FormSchema[] {
  return [
    { fieldName: 'workOrderNo', label: '生产工单号', component: 'Input', componentProps: { allowClear: true } },
    { fieldName: 'productName', label: '产品名称', component: 'Input', componentProps: { allowClear: true } },
  ];
}

export function useGridColumns(): VxeGridProps['columns'] {
  return [
    { field: 'workOrderNo', title: '生产工单号', width: 140 },
    { field: 'productCode', title: '产品编码', width: 130 },
    { field: 'productName', title: '产品名称', minWidth: 160 },
    {
      field: 'wipQuantity',
      title: '月末WIP数量 (A)',
      width: 140,
      align: 'right',
      formatter: ({ cellValue }) => `${cellValue} 件/单位`,
    },
    {
      field: 'completionRate',
      title: '完工程度 (B)',
      width: 180,
      align: 'center',
      slots: { default: 'completionRate' }
    },
    {
      field: 'equivalentQty',
      title: '约当产量 (C = A×B)', // 明确标出变量 C 和公式
      width: 160,
      align: 'right',
      slots: { default: 'equivalentQty' }
    },
    {
      field: 'status',
      title: '状态',
      width: 100,
      align: 'center',
      slots: { default: 'status' } // 使用 Vue 插槽渲染 Tag
    },
    { field: 'updateTime', title: '折算时间', width: 160, align: 'center' },
  ];
}
