// 文件路径：src/views/mes/cost/report/analysis/data.ts

import type { FormSchema } from '#/adapter/form';
import type { VxeGridProps } from '#/adapter/vxe-table';

export function useGridFormSchema(): FormSchema[] {
  return [
    { fieldName: 'period', label: '核算期间', component: 'DatePicker', componentProps: { picker: 'month', valueFormat: 'YYYY-MM', allowClear: false } },
    { fieldName: 'costCenterId', label: '成本中心', component: 'Input', componentProps: { placeholder: '请输入成本中心', allowClear: true } },
  ];
}

export function useGridColumns(): VxeGridProps['columns'] {
  return [
    {
      field: 'itemName',
      title: '成本中心 / 工单 / 费用要素',
      minWidth: 260,
      treeNode: true, // 保持开启树节点控制
      align: 'left',
      slots: { default: 'itemName' } // 交给插槽处理层级颜色
    },
    { field: 'method', title: '分摊方法', width: 140, align: 'center' },
    { field: 'qty', title: '产出/约当量', width: 120, align: 'right' },
    {
      field: 'materialCost',
      title: '直接材料(元)',
      width: 130,
      align: 'right',
      slots: { default: 'materialCost' } // 交给插槽处理颜色和格式
    },
    {
      field: 'overheadCost',
      title: '制造费用(元)',
      width: 130,
      align: 'right',
      slots: { default: 'overheadCost' }
    },
    {
      field: 'totalCost',
      title: '总成本(元)',
      width: 140,
      align: 'right',
      slots: { default: 'totalCost' }
    },
    {
      field: 'unitCost',
      title: '单位成本(元)',
      width: 120,
      align: 'right',
      slots: { default: 'unitCost' }
    },
    { field: 'remark', title: '占比/说明', minWidth: 150, align: 'left' },
  ];
}
