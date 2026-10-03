import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesHcInvTxnApi } from '#/api/mes/hc/inv/txn';

export const TXN_TYPE_OPTIONS = [
  { label: '生产入库', value: 'IN_PROD' },
  { label: '退料入库', value: 'IN_RETURN' },
  { label: '盘盈入库', value: 'IN_ADJUST' },
  { label: '出货出库', value: 'OUT_SHIP' },
  { label: '盘亏出库', value: 'OUT_ADJUST' },
  { label: '冻结', value: 'FREEZE' },
  { label: '解冻', value: 'UNFREEZE' },
  { label: '调拨入', value: 'TRANSFER_IN' },
  { label: '调拨出', value: 'TRANSFER_OUT' },
  { label: '计划锁定', value: 'PLAN_LOCK' },
  { label: '计划解锁', value: 'PLAN_UNLOCK' },
];

export const TXN_TYPE_COLOR: Record<string, string> = {
  IN_PROD: 'green',
  IN_RETURN: 'cyan',
  IN_ADJUST: 'blue',
  OUT_SHIP: 'orange',
  OUT_ADJUST: 'red',
  FREEZE: 'purple',
  UNFREEZE: 'geekblue',
  TRANSFER_IN: 'teal',
  TRANSFER_OUT: 'volcano',
  PLAN_LOCK: 'gold',
  PLAN_UNLOCK: 'lime',
};

export const TXN_TYPE_LABEL: Record<string, string> = Object.fromEntries(
  TXN_TYPE_OPTIONS.map((o) => [o.value, o.label]),
);

export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      fieldName: 'warehouseCode',
      label: '仓库',
      component: 'Select',
      componentProps: {
        options: [{ label: '成品仓', value: 'FG' }],
        allowClear: true,
        placeholder: '请选择仓库',
      },
    },
    {
      fieldName: 'materialCode',
      label: '物料编码',
      component: 'Input',
      componentProps: { placeholder: '请输入物料编码' },
    },
    {
      fieldName: 'modelNo',
      label: '型号',
      component: 'Input',
      componentProps: { placeholder: '请输入型号' },
    },
    {
      fieldName: 'batchNo',
      label: '批次号',
      component: 'Input',
      componentProps: { placeholder: '请输入批次号' },
    },
    {
      fieldName: 'txnTypes',
      label: '交易类型',
      component: 'Select',
      componentProps: {
        options: TXN_TYPE_OPTIONS,
        mode: 'multiple',
        allowClear: true,
        placeholder: '请选择交易类型',
      },
    },
    {
      fieldName: 'txnNo',
      label: '单据号',
      component: 'Input',
      componentProps: { placeholder: '请输入单据号' },
    },
    {
      fieldName: 'txnTimeStart',
      label: '交易时间起',
      component: 'DatePicker',
      componentProps: {
        class: 'w-full',
        showTime: true,
        valueFormat: 'YYYY-MM-DD HH:mm:ss',
        placeholder: '开始时间',
      },
    },
    {
      fieldName: 'txnTimeEnd',
      label: '交易时间止',
      component: 'DatePicker',
      componentProps: {
        class: 'w-full',
        showTime: true,
        valueFormat: 'YYYY-MM-DD HH:mm:ss',
        placeholder: '结束时间',
      },
    },
  ];
}

export function useGridColumns(): VxeTableGridOptions<MesHcInvTxnApi.TxnLog>['columns'] {
  return [
    { type: 'seq', width: 50, fixed: 'left' },
    {
      field: 'txnTime',
      title: '交易时间',
      width: 160,
      align: 'center',
      fixed: 'left',
      formatter: ({ cellValue }) => {
        if (!cellValue) return '-';
        const d = new Date(typeof cellValue === 'number' ? cellValue : cellValue);
        return d.toLocaleString('zh-CN', { hour12: false }).replace(/\//g, '-');
      },
    },
    {
      field: 'txnType',
      title: '交易类型',
      width: 100,
      align: 'center',
      slots: { default: 'txnType' },
    },
    { field: 'txnNo', title: '单据号', minWidth: 180, showOverflow: 'tooltip', slots: { default: 'txnNo' } },
    { field: 'warehouseName', title: '仓库', width: 80 },
    { field: 'materialCode', title: '物料编码', minWidth: 120 },
    { field: 'modelNo', title: '型号', minWidth: 140, showOverflow: 'tooltip' },
    { field: 'batchNo', title: '批次号', minWidth: 160, showOverflow: 'tooltip' },
    {
      field: 'txnQty',
      title: '交易数量',
      width: 90,
      align: 'right',
      slots: { default: 'txnQty' },
    },
    {
      field: 'beforeQty',
      title: '交易前在库',
      width: 100,
      align: 'right',
      formatter: ({ cellValue }) => (cellValue != null ? Number(cellValue).toFixed(2) : '-'),
    },
    {
      field: 'afterQty',
      title: '交易后在库',
      width: 100,
      align: 'right',
      formatter: ({ cellValue }) => (cellValue != null ? Number(cellValue).toFixed(2) : '-'),
    },
    { field: 'creatorName', title: '操作人', width: 80, align: 'center' },
    { field: 'remark', title: '备注', minWidth: 200, showOverflow: 'tooltip' },
  ];
}
