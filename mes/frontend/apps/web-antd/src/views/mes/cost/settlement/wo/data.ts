// 文件路径：src/views/mes/cost/settlement/wo/data.ts

import type { FormSchema } from '#/adapter/form';
import type { VxeGridProps } from '#/adapter/vxe-table';

export function useGridFormSchema(): FormSchema[] {
  return [
    { fieldName: 'workOrderNo', label: '工单号', component: 'Input', componentProps: { allowClear: true } },
    { fieldName: 'productName', label: '产品名称', component: 'Input', componentProps: { allowClear: true } },
  ];
}

export function useGridColumns(): VxeGridProps['columns'] {
  return [
    { field: 'workOrderNo', title: '生产工单号', width: 140, fixed: 'left' },
    { field: 'productName', title: '产品名称', minWidth: 150 },
    { field: 'costCenterName', title: '归属中心', width: 140 },
    {
      field: 'completedQty',
      title: '结算数量',
      width: 100,
      align: 'right',
      formatter: ({ cellValue }) => `${cellValue} 件`,
    },
    // ---- 成本明细区 (全部交由 Vue Slot 渲染) ----
    { field: 'materialCost', title: '直接材料成本 (A)', width: 160, align: 'right', slots: { default: 'materialCost' } },
    { field: 'overheadCost', title: '分摊制造费用 (B)', width: 160, align: 'right', slots: { default: 'overheadCost' } },
    { field: 'totalCost', title: '工单总成本 (C)', width: 150, align: 'right', slots: { default: 'totalCost' } },
    { field: 'unitCost', title: '单位成本 (D)', width: 130, align: 'right', slots: { default: 'unitCost' } },
    // -------------------
    { field: 'status', title: '结转状态', width: 100, align: 'center', slots: { default: 'status' } },
    { field: 'settleTime', title: '结转时间', width: 160, align: 'center' },
  ];
}
