import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import { getRangePickerDefaultProps } from '#/utils';

// ==========================================
// 1. 发货单搜索表单
// ==========================================
export function useGridFormSchema(): VbenFormSchema[] {
  return [
    { fieldName: 'deliveryNo', label: '发货单号', component: 'Input', componentProps: { placeholder: '输入发货单号', allowClear: true } },
    { fieldName: 'customerName', label: '收货客户', component: 'Input', componentProps: { placeholder: '输入客户名称或编码', allowClear: true } },
    {
      fieldName: 'status', label: '单据状态', component: 'Select',
      componentProps: {
        options: [
          { label: '草稿 (待下发)', value: 'DRAFT' },
          { label: '拣货装车中', value: 'PICKING' },
          { label: '已发货 (过账)', value: 'SHIPPED' }
        ],
        allowClear: true,
      }
    },
    { fieldName: 'createTime', label: '创建时间', component: 'RangePicker', componentProps: { ...getRangePickerDefaultProps(), allowClear: true } },
  ];
}

// ==========================================
// 2. 发货单主表列定义
// ==========================================
export function useGridColumns(): VxeTableGridOptions['columns'] {
  return [
    { type: 'checkbox', width: 40 },
    { field: 'deliveryNo', title: '发货单号', minWidth: 160, slots: { default: 'deliveryNo' }, fixed: 'left' },
    { field: 'customerName', title: '收货客户', minWidth: 180, slots: { default: 'customer' } },
    { field: 'totalPlanQty', title: '计划发货总量', minWidth: 120, align: 'right', slots: { default: 'totalPlanQty' } },
    { field: 'totalActualQty', title: '实际拣货总量', minWidth: 120, align: 'right', slots: { default: 'totalActualQty' } },
    { field: 'status', title: '发货状态', minWidth: 120, align: 'center', slots: { default: 'status' } },
    { field: 'contactName', title: '联系人', minWidth: 100 },
    { field: 'deliveryDate', title: '要求发货日期', minWidth: 140, align: 'center' },
    { title: '操作', width: 160, fixed: 'right', slots: { default: 'actions' } },
  ];
}

// ==========================================
// 3. 发货明细子表列定义 (弹窗内使用)
// ==========================================
export function useItemColumns(isDraft: boolean): VxeTableGridOptions['columns'] {
  const cols: VxeTableGridOptions['columns'] = [
    { type: 'seq', title: '行号', width: 60, align: 'center' },
    { field: 'productCode', title: '物料编码', minWidth: 140, slots: { default: 'productCode' } },
    { field: 'productName', title: '物料名称', minWidth: 160 },
    { field: 'unit', title: '单位', minWidth: 80, align: 'center' },
    { field: 'planQty', title: '计划发货量', minWidth: 120, align: 'right', slots: { default: 'planQty' } },
    { field: 'actualQty', title: '实拣量', minWidth: 120, align: 'right', slots: { default: 'actualQty' } },
    { field: 'status', title: '行状态', minWidth: 100, align: 'center', slots: { default: 'status' } },
  ];
  if (isDraft) {
    cols.push({ title: '操作', field: 'action', width: 80, align: 'center', fixed: 'right', slots: { default: 'action' } });
  }
  return cols;
}
