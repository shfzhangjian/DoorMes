import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesHcProcessOutputBalanceApi } from '#/api/mes/hc/execution/output-balance';

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

export const BALANCE_STATUS_OPTIONS = [
  { label: '有结存', value: 'AVAILABLE' },
  { label: '部分消耗', value: 'PARTIAL' },
  { label: '已耗尽', value: 'CONSUMED' },
  { label: '空产出', value: 'EMPTY' },
];

export const BALANCE_STATUS_LABEL: Record<string, string> = {
  AVAILABLE: '有结存',
  CONSUMED: '已耗尽',
  EMPTY: '空产出',
  PARTIAL: '部分消耗',
};

export const BALANCE_STATUS_COLOR: Record<string, string> = {
  AVAILABLE: 'success',
  CONSUMED: 'default',
  EMPTY: 'error',
  PARTIAL: 'processing',
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
  { label: '计划号', value: 'planNo' },
  { label: '工序', value: 'stageCode' },
  { label: '产出物批号（产品批号）', value: 'outputBatchNo' },
  { label: '产品型号', value: 'modelCode' },
  { label: '报工量（产出数量）', value: 'outputQty' },
  { label: '已加工', value: 'consumedQty' },
  { label: '剩余量', value: 'remainingQty' },
  { label: '结存状态', value: 'balanceStatus' },
  { label: '报工时间（确认时间）', value: 'reportTime' },
  { label: '消耗说明', value: 'consumeSummary' },
];

const quantityFormatter = ({ cellValue, row }: { cellValue?: number | string; row?: MesHcProcessOutputBalanceApi.Balance }) => {
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

export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      component: 'Input',
      componentProps: { placeholder: '计划号/批号/物料/型号/工序' },
      fieldName: 'keyword',
      label: '关键词',
    },
    {
      component: 'Input',
      componentProps: { placeholder: '请输入计划号' },
      fieldName: 'planNo',
      label: '计划号',
    },
    {
      component: 'Input',
      componentProps: { placeholder: '请输入产出批号' },
      fieldName: 'outputBatchNo',
      label: '产出批号',
    },
    {
      component: 'Select',
      componentProps: {
        allowClear: true,
        options: STAGE_OPTIONS,
        placeholder: '请选择工序',
      },
      fieldName: 'stageCode',
      label: '工序',
    },
    {
      component: 'Select',
      componentProps: {
        allowClear: true,
        options: BALANCE_STATUS_OPTIONS,
        placeholder: '请选择结存状态',
      },
      fieldName: 'balanceStatus',
      label: '结存状态',
    },
    {
      component: 'Switch',
      componentProps: {
        checkedChildren: '是',
        unCheckedChildren: '否',
      },
      defaultValue: false,
      fieldName: 'onlyRemaining',
      label: '只看有剩余',
    },
    {
      component: 'Input',
      componentProps: { placeholder: '请输入来源批号' },
      fieldName: 'sourceBatchNo',
      label: '来源批号',
    },
    {
      component: 'Input',
      componentProps: { placeholder: '请输入母批/上游批号' },
      fieldName: 'parentBatchNo',
      label: '母批/上游',
    },
    {
      component: 'Input',
      componentProps: { placeholder: '请输入物料编码' },
      fieldName: 'materialCode',
      label: '物料编码',
    },
    {
      component: 'Input',
      componentProps: { placeholder: '请输入型号' },
      fieldName: 'modelCode',
      label: '型号',
    },
    {
      component: 'DatePicker',
      componentProps: { class: 'w-full', placeholder: '开始日期', valueFormat: 'YYYY-MM-DD' },
      fieldName: 'reportDateStart',
      label: '确认日期起',
    },
    {
      component: 'DatePicker',
      componentProps: { class: 'w-full', placeholder: '结束日期', valueFormat: 'YYYY-MM-DD' },
      fieldName: 'reportDateEnd',
      label: '确认日期止',
    },
  ];
}

export function useGridColumns(): VxeTableGridOptions<MesHcProcessOutputBalanceApi.Balance>['columns'] {
  return [
    { fixed: 'left', title: '序号', type: 'seq', width: 60 },
    { field: 'planNo', fixed: 'left', minWidth: 145, slots: { header: 'sortableHeader' }, sortable: true, title: '计划号' },
    {
      align: 'center',
      field: 'stageCode',
      fixed: 'left',
      slots: { default: 'stageCode', header: 'sortableHeader' },
      sortable: true,
      title: '工序',
      width: 110,
    },
    {
      field: 'outputBatchNo',
      fixed: 'left',
      minWidth: 190,
      showOverflow: 'tooltip',
      slots: { header: 'sortableHeader' },
      sortable: true,
      title: '批次号',
    },
    {
      field: 'modelCode',
      minWidth: 130,
      showOverflow: 'tooltip',
      slots: { header: 'sortableHeader' },
      sortable: true,
      title: '产品型号',
    },
    {
      align: 'right',
      field: 'outputQty',
      formatter: quantityFormatter,
      slots: { header: 'sortableHeader' },
      sortable: true,
      title: '报工量（产出数量）',
      width: 150,
    },
    {
      align: 'right',
      field: 'consumedQty',
      formatter: quantityFormatter,
      slots: { header: 'sortableHeader' },
      sortable: true,
      title: '已加工',
      width: 120,
    },
    {
      align: 'right',
      field: 'remainingQty',
      formatter: quantityFormatter,
      slots: { header: 'sortableHeader' },
      sortable: true,
      title: '剩余量',
      width: 120,
    },
    {
      align: 'center',
      field: 'balanceStatus',
      slots: { default: 'balanceStatus', header: 'sortableHeader' },
      sortable: true,
      title: '结存状态',
      width: 100,
    },
    {
      align: 'center',
      field: 'reportTime',
      formatter: dateTimeFormatter,
      slots: { header: 'sortableHeader' },
      sortable: true,
      title: '报工时间',
      width: 170,
    },
    {
      field: 'consumeSummary',
      minWidth: 240,
      showOverflow: 'tooltip',
      slots: { header: 'sortableHeader' },
      sortable: true,
      title: '消耗说明',
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
