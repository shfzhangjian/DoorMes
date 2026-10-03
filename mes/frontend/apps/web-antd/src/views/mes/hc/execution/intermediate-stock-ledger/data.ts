import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesHcIntermediateStockLedgerApi } from '#/api/mes/hc/execution/intermediate-stock-ledger';

import dayjs from 'dayjs';

export const STAGE_OPTIONS = [
  { label: '湿法', value: 'WET' },
  { label: '磨皮二磨', value: 'GRINDING_SECOND' },
  { label: '粘胶1', value: 'ADHESIVE1' },
  { label: '分切', value: 'SLITTING' },
  { label: '压槽', value: 'PRESS_SLOT' },
  { label: '粘胶2', value: 'ADHESIVE2' },
  { label: '裁切', value: 'CUT_ROUND' },
];

export const STOCK_STATUS_OPTIONS = [
  { label: '可用', value: 'AVAILABLE' },
  { label: '已锁定', value: 'LOCKED' },
  { label: '冻结', value: 'FROZEN' },
  { label: '已耗尽', value: 'CONSUMED' },
];

export const QUALITY_STATUS_OPTIONS = [
  { label: '合格', value: '合格' },
  { label: '待检', value: '待检' },
  { label: '冻结', value: '冻结' },
  { label: '在制', value: '在制' },
];

export const STOCK_STATUS_LABEL: Record<string, string> = {
  AVAILABLE: '可用',
  CONSUMED: '已耗尽',
  EMPTY: '空库存',
  FROZEN: '冻结',
  LOCKED: '已锁定',
};

export const STOCK_STATUS_COLOR: Record<string, string> = {
  AVAILABLE: 'success',
  CONSUMED: 'default',
  EMPTY: 'error',
  FROZEN: 'warning',
  LOCKED: 'processing',
};

export const STAGE_COLOR: Record<string, string> = {
  ADHESIVE1: 'purple',
  ADHESIVE2: 'magenta',
  CUT_ROUND: 'red',
  GRINDING_SECOND: 'cyan',
  PRESS_SLOT: 'orange',
  SLITTING: 'blue',
  WET: 'green',
};

export const SORTABLE_FIELD_OPTIONS = [
  { label: '计划号', value: 'sourcePlanNo' },
  { label: '工序', value: 'sourceType' },
  { label: '母卷批次号', value: 'motherBatchNo' },
  { label: '中间品批号', value: 'batchNo' },
  { label: '产品型号', value: 'modelNo' },
  { label: '在库量', value: 'onHandQty' },
  { label: '可用量', value: 'availableQty' },
  { label: '可利库量', value: 'shareableQty' },
  { label: '计划锁定量', value: 'planLockedQty' },
  { label: '最近过账时间', value: 'lastTxnTime' },
  { label: '消耗/锁定说明', value: 'businessRemark' },
];

const quantityFormatter = ({ cellValue, row }: { cellValue?: number | string; row?: MesHcIntermediateStockLedgerApi.Ledger }) => {
  if (cellValue === null || cellValue === undefined || cellValue === '') {
    return '-';
  }
  const unit = row?.uom ? ` ${row.uom}` : '';
  return `${Number(cellValue).toFixed(3)}${unit}`;
};

const dateTimeFormatter = ({ cellValue }: { cellValue?: number | string }) => {
  if (cellValue === null || cellValue === undefined || cellValue === '') {
    return '-';
  }
  const rawValue = String(cellValue).trim();
  const parsedValue = /^\d+$/.test(rawValue)
    ? dayjs(rawValue.length === 10 ? Number(rawValue) * 1000 : Number(rawValue))
    : dayjs(rawValue);
  return parsedValue.isValid() ? parsedValue.format('YYYY-MM-DD HH:mm:ss') : rawValue;
};

export function useGridColumns(): VxeTableGridOptions<MesHcIntermediateStockLedgerApi.Ledger>['columns'] {
  return [
    { fixed: 'left', title: '序号', type: 'seq', width: 60 },
    { field: 'sourcePlanNo', fixed: 'left', minWidth: 145, slots: { header: 'sortableHeader' }, sortable: true, title: '计划号' },
    {
      align: 'center',
      field: 'sourceType',
      fixed: 'left',
      slots: { default: 'sourceType', header: 'sortableHeader' },
      sortable: true,
      title: '工序',
      width: 110,
    },
    {
      field: 'motherBatchNo',
      fixed: 'left',
      minWidth: 170,
      showOverflow: 'tooltip',
      slots: { header: 'sortableHeader' },
      sortable: true,
      title: '母卷批次号',
    },
    {
      field: 'batchNo',
      fixed: 'left',
      minWidth: 190,
      showOverflow: 'tooltip',
      slots: { header: 'sortableHeader' },
      sortable: true,
      title: '中间品/片号',
    },
    {
      fixed: 'left',
      field: 'modelNo',
      minWidth: 130,
      showOverflow: 'tooltip',
      slots: { header: 'sortableHeader' },
      sortable: true,
      title: '产品型号',
    },
    {
      align: 'right',
      field: 'stockCount',
      title: '片数',
      width: 78,
    },
    {
      align: 'right',
      field: 'onHandQty',
      formatter: quantityFormatter,
      slots: { default: 'quantityCell', header: 'sortableHeader' },
      sortable: true,
      title: '在库量',
      width: 120,
    },
    {
      align: 'right',
      field: 'availableQty',
      formatter: quantityFormatter,
      slots: { default: 'quantityCell', header: 'sortableHeader' },
      sortable: true,
      title: '可用量',
      width: 120,
    },
    {
      align: 'right',
      field: 'shareableQty',
      formatter: quantityFormatter,
      slots: { default: 'quantityCell', header: 'sortableHeader' },
      sortable: true,
      title: '可利库量',
      width: 120,
    },
    {
      align: 'right',
      field: 'planLockedQty',
      formatter: quantityFormatter,
      slots: { default: 'quantityCell', header: 'sortableHeader' },
      sortable: true,
      title: '计划锁定',
      width: 120,
    },
    {
      align: 'right',
      field: 'consumedQty',
      formatter: quantityFormatter,
      slots: { default: 'quantityCell' },
      title: '已消耗',
      width: 120,
    },
    {
      align: 'right',
      field: 'releasedQty',
      formatter: quantityFormatter,
      slots: { default: 'quantityCell' },
      title: '已释放',
      width: 120,
    },
    {
      align: 'right',
      field: 'lockRemainingQty',
      formatter: quantityFormatter,
      slots: { default: 'quantityCell' },
      title: '锁定剩余',
      width: 120,
    },
    {
      align: 'center',
      field: 'stockStatus',
      slots: { default: 'stockStatus' },
      title: '库存状态',
      width: 100,
    },
    {
      align: 'center',
      field: 'lastTxnTime',
      formatter: dateTimeFormatter,
      slots: { header: 'sortableHeader' },
      sortable: true,
      title: '最近过账时间',
      width: 170,
    },
    {
      field: 'txnSummary',
      minWidth: 240,
      slots: { default: 'txnSummary' },
      title: '消耗/锁定说明',
    },
    {
      align: 'center',
      fixed: 'right',
      slots: { default: 'actions' },
      title: '操作',
      width: 110,
    },
  ];
}
