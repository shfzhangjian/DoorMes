import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesHcInvStockApi } from '#/api/mes/hc/inv/stock';

export const SOURCE_TYPE_OPTIONS = [
  { label: '磨皮二磨产出', value: 'GRINDING_SECOND' },
  { label: '计划挂接中间品', value: 'PLAN_WIP_LOCK' },
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

export const SOURCE_TYPE_LABEL: Record<string, string> = {
  GRINDING_SECOND: '磨皮二磨产出',
  PLAN_WIP_LOCK: '计划挂接中间品',
};

export const SOURCE_TYPE_COLOR: Record<string, string> = {
  GRINDING_SECOND: 'cyan',
  PLAN_WIP_LOCK: 'blue',
};

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

const quantityFormatter = ({ cellValue }: { cellValue?: number | string }) =>
  cellValue === null || cellValue === undefined || cellValue === ''
    ? '-'
    : Number(cellValue).toFixed(3);

export function useGridFormSchema(showAdvanced = false): VbenFormSchema[] {
  const baseSchema: VbenFormSchema[] = [
    {
      component: 'Input',
      componentProps: { placeholder: '批号/计划号/物料/型号' },
      fieldName: 'keyword',
      label: '关键词',
    },
    {
      component: 'Input',
      componentProps: { placeholder: '请输入来源计划号' },
      fieldName: 'sourcePlanNo',
      label: '来源计划',
    },
    {
      component: 'Input',
      componentProps: { placeholder: '请输入半成品批号' },
      fieldName: 'batchNo',
      label: '半成品批号',
    },
  ];

  const advancedSchema: VbenFormSchema[] = [
    {
      component: 'Input',
      componentProps: { placeholder: '请输入来源批号' },
      fieldName: 'sourceBatchNo',
      label: '来源批号',
    },
    {
      component: 'Input',
      componentProps: { placeholder: '请输入母批号' },
      fieldName: 'sourceParentBatchNo',
      label: '母批号',
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
      fieldName: 'modelNo',
      label: '型号',
    },
    {
      component: 'InputNumber',
      componentProps: { class: 'w-full', min: 0, precision: 0, placeholder: '请输入工序序号' },
      fieldName: 'opSeq',
      label: '所在工序',
    },
    {
      component: 'Input',
      componentProps: { placeholder: '请输入段位编码' },
      fieldName: 'segmentCode',
      label: '段位编码',
    },
    {
      component: 'Select',
      componentProps: {
        allowClear: true,
        options: SOURCE_TYPE_OPTIONS,
        placeholder: '请选择来源类型',
      },
      fieldName: 'sourceType',
      label: '来源类型',
    },
    {
      component: 'Select',
      componentProps: {
        allowClear: true,
        options: QUALITY_STATUS_OPTIONS,
        placeholder: '请选择质量状态',
      },
      fieldName: 'qualityStatus',
      label: '质量状态',
    },
    {
      component: 'Select',
      componentProps: {
        allowClear: true,
        options: BIZ_STATUS_OPTIONS,
        placeholder: '请选择业务状态',
      },
      fieldName: 'bizStatus',
      label: '业务状态',
    },
    {
      component: 'DatePicker',
      componentProps: { class: 'w-full', placeholder: '开始日期', valueFormat: 'YYYY-MM-DD' },
      fieldName: 'productionDateStart',
      label: '生产日期起',
    },
    {
      component: 'DatePicker',
      componentProps: { class: 'w-full', placeholder: '结束日期', valueFormat: 'YYYY-MM-DD' },
      fieldName: 'productionDateEnd',
      label: '生产日期止',
    },
  ];

  return showAdvanced ? [...baseSchema, ...advancedSchema] : baseSchema;
}

export function useGridColumns(): VxeTableGridOptions<MesHcInvStockApi.Stock>['columns'] {
  return [
    { fixed: 'left', type: 'seq', width: 50 },
    { field: 'sourcePlanNo', fixed: 'left', minWidth: 150, title: '来源计划号' },
    { field: 'batchNo', fixed: 'left', minWidth: 170, showOverflow: 'tooltip', title: '半成品批号' },
    {
      align: 'center',
      field: 'sourceType',
      slots: { default: 'sourceType' },
      title: '来源类型',
      width: 130,
    },
    { field: 'opName', minWidth: 120, showOverflow: 'tooltip', title: '残留工序' },
    { align: 'center', field: 'segmentName', title: '段位', width: 90 },
    { field: 'sourceParentBatchNo', minWidth: 160, showOverflow: 'tooltip', title: '母批号' },
    { field: 'sourceBatchNo', minWidth: 170, showOverflow: 'tooltip', title: '来源批号' },
    { field: 'materialCode', minWidth: 130, title: '物料编码' },
    { field: 'materialName', minWidth: 150, showOverflow: 'tooltip', title: '物料名称' },
    { field: 'modelNo', minWidth: 130, showOverflow: 'tooltip', title: '型号' },
    { align: 'center', field: 'specSize', title: '尺寸', width: 90 },
    {
      align: 'right',
      field: 'onHandQty',
      formatter: quantityFormatter,
      title: '在库量',
      width: 100,
    },
    {
      align: 'right',
      field: 'availableQty',
      formatter: quantityFormatter,
      title: '可用量',
      width: 100,
    },
    {
      align: 'right',
      field: 'frozenQty',
      formatter: quantityFormatter,
      title: '冻结量',
      width: 100,
    },
    {
      align: 'right',
      field: 'planLockedQty',
      formatter: quantityFormatter,
      title: '计划锁定',
      width: 105,
    },
    { align: 'center', field: 'uom', title: '单位', width: 70 },
    {
      align: 'center',
      field: 'qualityStatus',
      slots: { default: 'qualityStatus' },
      title: '质量状态',
      width: 90,
    },
    {
      align: 'center',
      field: 'bizStatus',
      slots: { default: 'bizStatus' },
      title: '业务状态',
      width: 90,
    },
    { field: 'warehouseName', minWidth: 130, showOverflow: 'tooltip', title: '边库' },
    { field: 'locationName', minWidth: 110, showOverflow: 'tooltip', title: '库位' },
    { align: 'center', field: 'productionDate', title: '生产日期', width: 110 },
    { field: 'lastTxnNo', minWidth: 170, showOverflow: 'tooltip', title: '最近流水号' },
    { align: 'center', field: 'lastTxnTime', title: '最近过账时间', width: 165 },
    { field: 'businessRemark', minWidth: 220, showOverflow: 'tooltip', title: '业务备注' },
  ];
}
