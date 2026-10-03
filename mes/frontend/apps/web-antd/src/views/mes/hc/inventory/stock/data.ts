import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesHcInvStockApi } from '#/api/mes/hc/inv/stock';

export const STOCK_TYPE_OPTIONS = [
  { label: '半成品', value: 'WIP' },
  { label: '成品', value: 'FG' },
];

export const QUALITY_STATUS_OPTIONS = [
  { label: '合格', value: '合格' },
  { label: '待检', value: '待检' },
  { label: '冻结', value: '冻结' },
  { label: '在制', value: '在制' },
];

export const BIZ_STATUS_OPTIONS = [
  { label: '量产', value: '量产' },
  { label: '研发', value: '研发' },
  { label: '客诉冻结', value: '客诉冻结' },
];

export const QUALITY_STATUS_COLOR: Record<string, string> = {
  合格: 'success',
  待检: 'warning',
  冻结: 'error',
  在制: 'processing',
};

export const BIZ_STATUS_COLOR: Record<string, string> = {
  量产: 'blue',
  研发: 'purple',
  客诉冻结: 'red',
};

export function resolveStockTypeLabel(stockType?: string) {
  return STOCK_TYPE_OPTIONS.find((item) => item.value === stockType)?.label || stockType || '-';
}

export function useGridFormSchema(showAdvanced = false): VbenFormSchema[] {
  const baseSchema: VbenFormSchema[] = [
    {
      fieldName: 'stockType',
      label: '库存类型',
      component: 'Select',
      componentProps: {
        options: STOCK_TYPE_OPTIONS,
        allowClear: true,
        placeholder: '请选择库存类型',
      },
    },
    {
      fieldName: 'keyword',
      label: '关键词',
      component: 'Input',
      componentProps: { placeholder: '批号/物料/型号' },
    },
  ];

  const advancedSchema: VbenFormSchema[] = [
    {
      fieldName: 'warehouseCode',
      label: '仓库编码',
      component: 'Input',
      componentProps: { placeholder: '请输入仓库编码' },
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
      fieldName: 'recipeCode',
      label: '配方编码',
      component: 'Input',
      componentProps: { placeholder: '请输入配方编码' },
    },
    {
      fieldName: 'specSize',
      label: '尺寸规格',
      component: 'Input',
      componentProps: { placeholder: '请输入尺寸规格' },
    },
    {
      fieldName: 'opSeq',
      label: '所在工序',
      component: 'InputNumber',
      componentProps: { class: 'w-full', min: 0, precision: 0, placeholder: '请输入工序序号' },
    },
    {
      fieldName: 'segmentCode',
      label: '段位编码',
      component: 'Input',
      componentProps: { placeholder: 'P/Q/R/S/整片' },
    },
    {
      fieldName: 'qualityStatus',
      label: '质量状态',
      component: 'Select',
      componentProps: {
        options: QUALITY_STATUS_OPTIONS,
        allowClear: true,
        placeholder: '请选择质量状态',
      },
    },
    {
      fieldName: 'bizStatus',
      label: '业务状态',
      component: 'Select',
      componentProps: {
        options: BIZ_STATUS_OPTIONS,
        allowClear: true,
        placeholder: '请选择业务状态',
      },
    },
    {
      fieldName: 'productionDateStart',
      label: '生产日期起',
      component: 'DatePicker',
      componentProps: { class: 'w-full', valueFormat: 'YYYY-MM-DD', placeholder: '开始日期' },
    },
    {
      fieldName: 'productionDateEnd',
      label: '生产日期止',
      component: 'DatePicker',
      componentProps: { class: 'w-full', valueFormat: 'YYYY-MM-DD', placeholder: '结束日期' },
    },
  ];

  return showAdvanced ? [...baseSchema, ...advancedSchema] : baseSchema;
}

export function useGridColumns(): VxeTableGridOptions<MesHcInvStockApi.Stock>['columns'] {
  return [
    { type: 'seq', width: 50, fixed: 'left' },
    {
      field: 'stockType',
      title: '库存类型',
      width: 90,
      fixed: 'left',
      slots: { default: 'stockType' },
    },
    { field: 'warehouseName', title: '仓库', width: 110, fixed: 'left', showOverflow: 'tooltip' },
    { field: 'locationName', title: '库位', width: 120, showOverflow: 'tooltip' },
    { field: 'materialCode', title: '物料编码', minWidth: 130 },
    { field: 'materialName', title: '物料名称', minWidth: 120, showOverflow: 'tooltip' },
    { field: 'recipeCode', title: '配方编码', minWidth: 110, showOverflow: 'tooltip' },
    { field: 'modelNo', title: '型号', minWidth: 150, showOverflow: 'tooltip' },
    { field: 'specSize', title: '尺寸规格', width: 90, align: 'center' },
    { field: 'batchNo', title: '批次号', minWidth: 160, showOverflow: 'tooltip' },
    { field: 'opName', title: '所在工序', minWidth: 120, showOverflow: 'tooltip' },
    { field: 'segmentName', title: '段位', width: 80, align: 'center' },
    { field: 'thickness', title: '厚度', width: 80, align: 'right' },
    { field: 'productionDate', title: '生产日期', width: 110, align: 'center' },
    {
      field: 'onHandQty',
      title: '在库数量',
      width: 100,
      align: 'right',
      formatter: ({ cellValue }) => (cellValue != null ? Number(cellValue).toFixed(3) : '-'),
    },
    {
      field: 'availableQty',
      title: '可用数量',
      width: 100,
      align: 'right',
      formatter: ({ cellValue }) => (cellValue != null ? Number(cellValue).toFixed(3) : '-'),
    },
    {
      field: 'frozenQty',
      title: '冻结数量',
      width: 100,
      align: 'right',
      formatter: ({ cellValue }) => (cellValue != null ? Number(cellValue).toFixed(3) : '-'),
    },
    {
      field: 'planLockedQty',
      title: '计划锁定',
      width: 100,
      align: 'right',
      formatter: ({ cellValue }) => (cellValue != null ? Number(cellValue).toFixed(3) : '-'),
    },
    { field: 'uom', title: '单位', width: 60, align: 'center' },
    { field: 'ownerName', title: '货主', width: 100, showOverflow: 'tooltip' },
    {
      field: 'qualityStatus',
      title: '质量状态',
      width: 90,
      align: 'center',
      slots: { default: 'qualityStatus' },
    },
    {
      field: 'bizStatus',
      title: '业务状态',
      width: 90,
      align: 'center',
      slots: { default: 'bizStatus' },
    },
    { field: 'businessRemark', title: '业务备注', minWidth: 160, showOverflow: 'tooltip' },
    { field: 'lastTxnNo', title: '最近流水号', minWidth: 170, showOverflow: 'tooltip' },
    { field: 'updateTime', title: '更新时间', width: 165, align: 'center' },
  ];
}
