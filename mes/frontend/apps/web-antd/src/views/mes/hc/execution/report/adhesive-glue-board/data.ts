import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesHcAdhesiveConsoleApi } from '#/api/mes/hc/execution/adhesive-console';
import type { MesHcFinishedGlueBoardMapApi } from '#/api/mes/hc/finishedglueboardmap';

import { formatDateTime } from '@vben/utils';

import { getFinishedGlueBoardMapModelOptions } from '#/api/mes/hc/finishedglueboardmap';

export const STOCK_STATUS_OPTIONS = [
  { label: '可用', value: 'ACTIVE' },
  { label: '已用完', value: 'USED_UP' },
  { label: '已锁定', value: 'LOCKED' },
  { label: '已退库', value: 'RETURNED' },
];

export interface GlueBoardModelOption {
  extra?: MesHcFinishedGlueBoardMapApi.FinishedGlueBoardMapItem;
  label: string;
  value: string;
}

interface GlueBoardModelOptionParams {
  glueBoardMaterialCode?: string;
  glueProcess?: string;
}

const FALLBACK_GLUE_BOARD_MAP_ITEMS: MesHcFinishedGlueBoardMapApi.FinishedGlueBoardMapItem[] = [
  { glueProcess: 'ADHESIVE1', glueBoardMaterialCode: '01.02.00002', glueBoardMaterialName: '胶板', glueBoardModel: 'PT-217KV', preferredFlag: true, sort: 1 },
  { glueProcess: 'ADHESIVE1', glueBoardMaterialCode: '01.02.00041', glueBoardMaterialName: '胶板', glueBoardModel: '（TTC301D-04）', preferredFlag: true, sort: 1 },
  { glueProcess: 'ADHESIVE2', glueBoardMaterialCode: '01.02.00001', glueBoardMaterialName: '胶板', glueBoardModel: 'W220', preferredFlag: true, sort: 2 },
  { glueProcess: 'ADHESIVE2', glueBoardMaterialCode: '01.02.00025', glueBoardMaterialName: '胶板', glueBoardModel: 'W250', preferredFlag: true, sort: 2 },
  { glueProcess: 'ADHESIVE2', glueBoardMaterialCode: '01.02.00020', glueBoardMaterialName: '胶板', glueBoardModel: 'SDK', preferredFlag: true, sort: 2 },
  { glueProcess: 'ADHESIVE2', glueBoardModel: 'SDK双面平纹胶', preferredFlag: true, sort: 2 },
  { glueProcess: 'ADHESIVE2', glueBoardModel: 'SDK双面网格胶', preferredFlag: true, sort: 2 },
  { glueProcess: 'ADHESIVE2', glueBoardMaterialCode: '01.02.00045', glueBoardMaterialName: '胶板', glueBoardModel: 'W250改B', preferredFlag: false, sort: 3 },
];

function normalizeText(value?: string) {
  return String(value || '').trim();
}

function normalizeProcess(value?: string) {
  const process = normalizeText(value).toUpperCase();
  if (process === 'ADHESIVE' || process === 'ADHESIVE1' || process === 'OP-ADHESIVE1') return 'ADHESIVE1';
  if (process === 'ADHESIVE2' || process === 'OP-ADHESIVE2') return 'ADHESIVE2';
  return process;
}

function matchesGlueBoardModelOption(
  item: MesHcFinishedGlueBoardMapApi.FinishedGlueBoardMapItem,
  params: GlueBoardModelOptionParams,
) {
  const model = normalizeText(item.glueBoardModel);
  if (!model) return false;
  const materialCode = normalizeText(params.glueBoardMaterialCode);
  const process = normalizeProcess(params.glueProcess);
  if (materialCode && normalizeText(item.glueBoardMaterialCode).toUpperCase() !== materialCode.toUpperCase()) {
    return false;
  }
  if (process && normalizeProcess(item.glueProcess) !== process) {
    return false;
  }
  return true;
}

function toGlueBoardModelOptions(
  items: MesHcFinishedGlueBoardMapApi.FinishedGlueBoardMapItem[],
  params: GlueBoardModelOptionParams = {},
) {
  const sortedItems = [...items]
    .filter((item) => matchesGlueBoardModelOption(item, params))
    .sort((left, right) => {
      const preferredCompare = Number(!left.preferredFlag) - Number(!right.preferredFlag);
      if (preferredCompare !== 0) return preferredCompare;
      const sortCompare = Number(left.sort || 0) - Number(right.sort || 0);
      if (sortCompare !== 0) return sortCompare;
      return normalizeText(left.glueBoardModel).localeCompare(normalizeText(right.glueBoardModel), 'zh-CN');
    });
  const dedup = new Map<string, GlueBoardModelOption>();
  sortedItems.forEach((item) => {
    const model = normalizeText(item.glueBoardModel);
    const materialCode = normalizeText(item.glueBoardMaterialCode);
    const key = model.toUpperCase();
    if (!dedup.has(key)) {
      dedup.set(key, {
        label: materialCode ? `${model}（${materialCode}）` : model,
        value: model,
        extra: item,
      });
    }
  });
  return [...dedup.values()];
}

export const GLUE_BOARD_MODEL_OPTIONS: GlueBoardModelOption[] = toGlueBoardModelOptions(FALLBACK_GLUE_BOARD_MAP_ITEMS);

export async function loadGlueBoardModelOptions(params: GlueBoardModelOptionParams = {}) {
  let items: MesHcFinishedGlueBoardMapApi.FinishedGlueBoardMapItem[];
  try {
    const requestParams: GlueBoardModelOptionParams = {};
    const materialCode = normalizeText(params.glueBoardMaterialCode);
    const process = normalizeProcess(params.glueProcess);
    if (materialCode) {
      requestParams.glueBoardMaterialCode = materialCode;
    }
    if (process) {
      requestParams.glueProcess = process;
    }
    items = await getFinishedGlueBoardMapModelOptions(requestParams);
  } catch {
    items = FALLBACK_GLUE_BOARD_MAP_ITEMS;
  }
  const options = toGlueBoardModelOptions(items, params);
  if (!normalizeText(params.glueBoardMaterialCode) && !normalizeText(params.glueProcess)) {
    GLUE_BOARD_MODEL_OPTIONS.splice(0, GLUE_BOARD_MODEL_OPTIONS.length, ...options);
  }
  return options;
}

export function getGlueBoardModelOption(value?: string, options: GlueBoardModelOption[] = GLUE_BOARD_MODEL_OPTIONS) {
  const model = normalizeText(value);
  return options.find((item) => item.value === model);
}

export const STOCK_MEASURE_MODE_OPTIONS = [
  { label: '按米', value: 'LENGTH' },
  { label: '按个', value: 'COUNT' },
];

export const LIFETIME_MODE_OPTIONS = [
  { label: '不判断', value: 'NONE' },
  { label: '按米', value: 'LENGTH' },
  { label: '按次', value: 'COUNT' },
  { label: '米或次数先到', value: 'LENGTH_OR_COUNT' },
];

export const QUALITY_STATUS_OPTIONS = [
  { label: '正常', value: 'NORMAL' },
  { label: '已送检', value: 'WAITING' },
  { label: '质量异常', value: 'ABNORMAL' },
  { label: '已锁定', value: 'LOCKED' },
];

export const ERP_TRANSFER_STATUS_OPTIONS = [
  { label: '待同步', value: 'NOT_SYNCED' },
  { label: '已同步', value: 'SYNCED' },
  { label: '同步失败', value: 'FAILED' },
];

function formatMeter(value: any) {
  if (value === null || value === undefined || value === '') return '-';
  const numberValue = Number(value);
  return Number.isFinite(numberValue) ? `${numberValue.toFixed(3)} m` : String(value);
}

function formatCount(value: any) {
  if (value === null || value === undefined || value === '') return '-';
  const numberValue = Number(value);
  return Number.isFinite(numberValue) ? `${numberValue.toFixed(3)} 个` : String(value);
}

function formatStockQty(row: MesHcAdhesiveConsoleApi.GlueBoardStock) {
  return row?.stockMeasureMode === 'COUNT' ? formatCount(row.availableCount) : formatMeter(row.availableLength);
}

function formatLifeLimit(row: MesHcAdhesiveConsoleApi.GlueBoardStock) {
  const mode = row?.lifetimeMode || 'NONE';
  if (mode === 'LENGTH') return formatMeter(row.lifetimeLimitLength);
  if (mode === 'COUNT') return `${Number(row.lifetimeLimitCount || 0).toFixed(0)} 次`;
  if (mode === 'LENGTH_OR_COUNT') {
    return `${formatMeter(row.lifetimeLimitLength)} / ${Number(row.lifetimeLimitCount || 0).toFixed(0)} 次`;
  }
  return '-';
}

function formatLifeUsed(row: MesHcAdhesiveConsoleApi.GlueBoardStock) {
  const mode = row?.lifetimeMode || 'NONE';
  if (mode === 'LENGTH') return formatMeter(row.lifeUsedLength);
  if (mode === 'COUNT') return `${Number(row.lifeUsedCount || 0).toFixed(0)} 次`;
  if (mode === 'LENGTH_OR_COUNT') {
    return `${formatMeter(row.lifeUsedLength)} / ${Number(row.lifeUsedCount || 0).toFixed(0)} 次`;
  }
  return '-';
}

function formatStatus(options: Array<{ label: string; value: string }>, value: any) {
  return options.find((item) => item.value === value)?.label || value || '-';
}

export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      fieldName: 'glueBoardMaterialCode',
      label: '辅料料号',
      component: 'Input',
      componentProps: { placeholder: '请输入辅料料号' },
    },
    {
      fieldName: 'glueBoardModel',
      label: '胶板型号',
      component: 'Select',
      componentProps: {
        allowClear: true,
        options: GLUE_BOARD_MODEL_OPTIONS,
        placeholder: '请选择胶板型号',
      },
    },
    {
      fieldName: 'glueBoardBatchNo',
      label: '胶板批号',
      component: 'Input',
      componentProps: { placeholder: '请输入胶板批号' },
    },
    {
      fieldName: 'stockStatus',
      label: '库存状态',
      component: 'Select',
      componentProps: {
        allowClear: true,
        options: STOCK_STATUS_OPTIONS,
        placeholder: '请选择库存状态',
      },
    },
    {
      fieldName: 'qualityStatus',
      label: '质量状态',
      component: 'Select',
      componentProps: {
        allowClear: true,
        options: QUALITY_STATUS_OPTIONS,
        placeholder: '请选择质量状态',
      },
    },
    {
      fieldName: 'receiveDate',
      label: '领料日期',
      component: 'DatePicker',
      componentProps: {
        valueFormat: 'YYYY-MM-DD',
      },
    },
    {
      fieldName: 'erpTransferStatus',
      label: 'ERP同步',
      component: 'Select',
      componentProps: {
        allowClear: true,
        options: ERP_TRANSFER_STATUS_OPTIONS,
        placeholder: '请选择ERP同步状态',
      },
    },
  ];
}

export function useGridColumns(): VxeTableGridOptions<MesHcAdhesiveConsoleApi.GlueBoardStock>['columns'] {
  return [
    { type: 'seq', width: 60, fixed: 'left', title: '序号' },
    { field: 'glueBoardMaterialCode', title: '辅料料号', minWidth: 150, fixed: 'left' },
    { field: 'glueBoardModel', title: '胶板型号', minWidth: 110, fixed: 'left' },
    { field: 'glueBoardMaterialName', title: '辅料名称', minWidth: 160 },
    { field: 'glueBoardBatchNo', title: '胶板批号', minWidth: 170 },
    { field: 'stockMeasureMode', title: '库存口径', minWidth: 100, formatter: ({ cellValue }) => formatStatus(STOCK_MEASURE_MODE_OPTIONS, cellValue) },
    { field: 'erpTransferNo', title: 'ERP移库单号', minWidth: 150 },
    { field: 'transferQty', title: '移库数量', minWidth: 110 },
    { field: 'transferUnit', title: '移库单位', minWidth: 90 },
    { field: 'unpackQty', title: '拆包量', minWidth: 110 },
    { field: 'unpackUnit', title: '拆包单位', minWidth: 100 },
    { field: 'erpTransferStatus', title: 'ERP同步', minWidth: 110, formatter: ({ cellValue }) => formatStatus(ERP_TRANSFER_STATUS_OPTIONS, cellValue) },
    { field: 'edgeWarehouseName', title: '边库', minWidth: 130 },
    { field: 'usedLength', title: '已分配', minWidth: 120, formatter: ({ row }) => row.stockMeasureMode === 'COUNT' ? formatCount(row.usedCount) : formatMeter(row.usedLength) },
    { field: 'lossLength', title: '损耗量', minWidth: 120, formatter: ({ row }) => row.stockMeasureMode === 'COUNT' ? formatCount(row.lossCount) : formatMeter(row.lossLength) },
    { field: 'availableStartPosition', title: '可用起位置', minWidth: 130, formatter: ({ cellValue }) => formatMeter(cellValue) },
    { field: 'availableLength', title: '当前可用', minWidth: 120, formatter: ({ row }) => formatStockQty(row) },
    { field: 'lifetimeMode', title: '寿命口径', minWidth: 130, formatter: ({ cellValue }) => formatStatus(LIFETIME_MODE_OPTIONS, cellValue) },
    { field: 'lifetimeLimitLength', title: '寿命上限', minWidth: 150, formatter: ({ row }) => formatLifeLimit(row) },
    { field: 'lifeUsedLength', title: '寿命已用', minWidth: 150, formatter: ({ row }) => formatLifeUsed(row) },
    { field: 'stockStatus', title: '库存状态', minWidth: 110, formatter: ({ cellValue }) => formatStatus(STOCK_STATUS_OPTIONS, cellValue) },
    { field: 'qualityStatus', title: '质量状态', minWidth: 110, formatter: ({ cellValue }) => formatStatus(QUALITY_STATUS_OPTIONS, cellValue) },
    { field: 'latestInspectionNo', title: '最新检验单', minWidth: 150 },
    { field: 'latestInspectionResult', title: '最新判定', minWidth: 100 },
    {
      field: 'inspectionSubmitTime',
      title: '送检时间',
      minWidth: 180,
      formatter: ({ cellValue }) => (cellValue ? (formatDateTime(cellValue) as string) : '-'),
    },
    { field: 'receiverName', title: '领料人', minWidth: 110 },
    { field: 'receiveDate', title: '领料日期', minWidth: 120 },
    {
      field: 'receiveTime',
      title: '领料时间',
      minWidth: 180,
      formatter: ({ cellValue }) => (cellValue ? (formatDateTime(cellValue) as string) : '-'),
    },
    { field: 'printCount', title: '打印次数', minWidth: 100 },
    {
      field: 'printTime',
      title: '最近打印时间',
      minWidth: 180,
      formatter: ({ cellValue }) => (cellValue ? (formatDateTime(cellValue) as string) : '-'),
    },
    { field: 'remark', title: '备注', minWidth: 180 },
    { title: '操作', width: 130, minWidth: 130, fixed: 'right', slots: { default: 'actions' } },
  ];
}
