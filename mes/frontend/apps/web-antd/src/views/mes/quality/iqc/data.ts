import type { VbenFormSchema } from '#/adapter/form';
import type { VxeGridProps } from '#/adapter/vxe-table';

import { getRangePickerDefaultProps } from '#/utils';

interface GridFormSchemaOptions {
  includeMaterialBatch?: boolean;
}

export const retentionStatusOptions = [
  { label: '已留样', value: 'RETAINED' },
  { label: '未留样', value: 'NOT_RETAINED' },
];

export function retentionStatusLabel(value?: string) {
  return value === 'RETAINED' ? '已留样' : '未留样';
}

export function retentionStatusColor(value?: string) {
  return value === 'RETAINED' ? 'success' : 'default';
}

export function useGridFormSchema(
  options: GridFormSchemaOptions = {},
): VbenFormSchema[] {
  const { includeMaterialBatch = true } = options;
  const materialBatchSchema: VbenFormSchema[] = includeMaterialBatch
    ? [
        {
          fieldName: 'materialCode',
          label: '料号',
          component: 'Input',
          componentProps: { placeholder: '输入料号' },
        },
        {
          fieldName: 'batchNo',
          label: '批号',
          component: 'Input',
          componentProps: { placeholder: '输入批号' },
        },
      ]
    : [];
  const schema: VbenFormSchema[] = [
    ...materialBatchSchema,
    {
      fieldName: 'iqcNo',
      label: '检验单号',
      component: 'Input',
      componentProps: { placeholder: '输入单号' },
    },
    {
      fieldName: 'receiptNo',
      label: '收料单号',
      component: 'Input',
      componentProps: { placeholder: '输入收料单' },
    },
    {
      fieldName: 'supplierName',
      label: '供应商',
      component: 'Input',
      componentProps: { placeholder: '供应商名称' },
    },
    {
      fieldName: 'status',
      label: '单据状态',
      component: 'Select',
      componentProps: {
        options: [
          { label: '待检验', value: 'PENDING' },
          { label: '检验中', value: 'INSPECTING' },
          { label: '待审核', value: 'WAITING_CONFIRM' },
          { label: '已挂起', value: 'SUSPENDED' },
          { label: '已完成', value: 'COMPLETED' },
          { label: '已完成（历史）', value: 'FINISHED' },
          { label: '已拒收', value: 'REJECTED' },
          { label: '已取消', value: 'CANCELED' },
        ],
        allowClear: true,
      },
    },
    {
      fieldName: 'judgment',
      label: '判定结果',
      component: 'Select',
      componentProps: {
        options: [
          { label: '合格(OK)', value: 'OK' },
          { label: '拒收(NG)', value: 'NG' },
          { label: '不判定', value: 'SKIP' },
        ],
        allowClear: true,
      },
    },
    {
      fieldName: 'retentionStatus',
      label: '留样状态',
      component: 'Select',
      componentProps: {
        options: retentionStatusOptions,
        allowClear: true,
      },
    },
    {
      fieldName: 'standardNo',
      label: '标准编号',
      component: 'Input',
      componentProps: { placeholder: '输入标准编号' },
    },
    {
      fieldName: 'standardMatchMode',
      label: '匹配方式',
      component: 'Select',
      componentProps: {
        options: [
          { label: '物料专用', value: 'MATERIAL' },
          { label: '通用标准', value: 'UNIVERSAL' },
        ],
        allowClear: true,
      },
    },
    {
      fieldName: 'inspectionTime',
      label: '检验时间',
      component: 'RangePicker',
      componentProps: { ...getRangePickerDefaultProps() },
    },
  ];
  return schema;
}

export function useGridColumns(): VxeGridProps['columns'] {
  return [
    { type: 'seq', width: 50, align: 'center', fixed: 'left' },
    {
      field: 'iqcNo',
      title: 'IQC检验单号',
      width: 160,
      fixed: 'left',
      slots: { default: 'iqcNo' },
    },
    {
      field: 'materialCode',
      title: '物料编码',
      minWidth: 140,
      formatter: ({ cellValue }) => cellValue || '-',
    },
    { field: 'supplierName', title: '供应商名称', minWidth: 160 },
    { field: 'receiverName', title: '收件人', width: 100 },
    { field: 'materialName', title: '物料品名', minWidth: 150 },
    { field: 'batchNo', title: '批次号', width: 130 },
    { field: 'arrivalDate', title: '来料日期', width: 110, align: 'center' },
    {
      field: 'productionDate',
      title: '生产日期',
      width: 110,
      align: 'center',
    },
    { field: 'expiryDate', title: '失效日期', width: 110, align: 'center' },
    { field: 'receiveQty', title: '到货量', width: 90, align: 'right' },
    { field: 'unit', title: '单位', width: 80, align: 'center' },
    {
      field: 'inspectionApplyTime',
      title: '报检时间',
      width: 150,
      align: 'center',
    },
    {
      field: 'inspectionApplyAttachmentUrls',
      title: '送检附件',
      width: 100,
      align: 'center',
      slots: { default: 'attachments' },
    },
    { field: 'purchaseContractNo', title: '采购合同号', width: 150 },
    {
      field: 'standardNo',
      title: '检验标准',
      width: 170,
      slots: { default: 'standardNo' },
    },
    {
      field: 'status',
      title: '单据状态',
      width: 90,
      align: 'center',
      slots: { default: 'status' },
    },
    {
      field: 'judgment',
      title: '最终判定',
      width: 90,
      align: 'center',
      slots: { default: 'judgment' },
    },
    {
      field: 'retentionStatus',
      title: '留样状态',
      width: 100,
      align: 'center',
      slots: { default: 'retentionStatus' },
    },
    { field: 'inspectorName', title: '检验员', width: 90, align: 'center' },
    { field: 'inspectionTime', title: '检验时间', width: 150, align: 'center' },
    { field: 'qaInspectorName', title: '确认人', width: 90, align: 'center' },
    { field: 'qaTime', title: '确认时间', width: 150, align: 'center' },
    // 💡 这里是补上的操作列配置！绑定了 index.vue 中的 #actions 插槽
    {
      title: '操作',
      field: 'action',
      fixed: 'right',
      width: 380,
      align: 'center',
      slots: { default: 'actions' },
    },
  ];
}
