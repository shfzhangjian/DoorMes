import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import { getRangePickerDefaultProps } from '#/utils';

// ==========================================
// 1. 台账查询表单 (支持多维度复合搜索)
// ==========================================
export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      fieldName: 'productCode',
      label: '物料编码/名称',
      component: 'Input',
      componentProps: { placeholder: '请输入编码或名称', allowClear: true },
    },
    {
      fieldName: 'trxType',
      label: '事务类型',
      component: 'Select',
      componentProps: {
        options: [
          { label: '采购入库 (PO_RECEIPT)', value: 'PO_RECEIPT' },
          { label: '生产领料 (WO_ISSUE)', value: 'WO_ISSUE' },
          { label: '余料退库 (WO_RETURN)', value: 'WO_RETURN' },
          { label: '库内调拨 (TRANSFER)', value: 'TRANSFER' },
          { label: '盘点调整 (CYCLE_COUNT)', value: 'CYCLE_COUNT' },
          { label: '杂项入账 (MISC_RECEIPT)', value: 'MISC_RECEIPT' },
          { label: '杂项发料 (MISC_ISSUE)', value: 'MISC_ISSUE' },
        ],
        allowClear: true,
        placeholder: '全部类型',
      },
    },
    {
      fieldName: 'refOrderNo',
      label: '关联源单据',
      component: 'Input',
      componentProps: { placeholder: '如入库单、工单号等', allowClear: true },
    },
    {
      fieldName: 'location',
      label: '操作库位',
      component: 'Input',
      componentProps: { placeholder: '输入库位编码', allowClear: true },
    },
    {
      fieldName: 'trxTime',
      label: '发生时间',
      component: 'RangePicker',
      componentProps: { ...getRangePickerDefaultProps(), allowClear: true },
    },
  ];
}

// ==========================================
// 2. 台账数据列定义
// ==========================================
export function useGridColumns(): VxeTableGridOptions['columns'] {
  return [
    { type: 'seq', title: '序号', width: 60, align: 'center' },
    { field: 'trxNo', title: '台账流水号', minWidth: 180, slots: { default: 'trxNo' } },
    { field: 'trxType', title: '事务类型', minWidth: 140, slots: { default: 'trxType' } },
    { field: 'refOrderNo', title: '源单据凭证', minWidth: 160 },
    { field: 'productCode', title: '物料编码', minWidth: 140, slots: { default: 'productCode' } },
    { field: 'productName', title: '物料名称', minWidth: 160 },
    { field: 'location', title: '操作库位', minWidth: 120, slots: { default: 'location' } },
    // 💡 核心数据列：变动数和结存数
    { field: 'qty', title: '变动数量', minWidth: 120, align: 'right', slots: { default: 'qty' } },
    { field: 'balanceQty', title: '变动后结存', minWidth: 120, align: 'right', slots: { default: 'balanceQty' } },

    { field: 'operator', title: '操作人员', minWidth: 100, align: 'center' },
    { field: 'trxTime', title: '过账时间', minWidth: 160, align: 'center' },
  ];
}
