import type { VbenFormSchema } from '#/adapter/form';
import type { VxeGridProps } from '#/adapter/vxe-table';

export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      fieldName: 'keyword',
      label: '关键字',
      component: 'Input',
      componentProps: {
        allowClear: true,
        placeholder: '发货单、客户、ERP订单、物料编码、型号',
      },
    },
  ];
}

export function useGridColumns(): VxeGridProps['columns'] {
  return [
    { type: 'seq', width: 56, align: 'center', fixed: 'left' },
    {
      field: 'shippingNoticeNo',
      title: '发货通知单',
      width: 180,
      fixed: 'left',
      slots: { default: 'shippingNoticeNo' },
    },
    { field: 'customerName', title: '客户', minWidth: 150 },
    { field: 'erpOrderNo', title: 'ERP订单', minWidth: 170 },
    { field: 'materialCode', title: '物料编码', minWidth: 140 },
    { field: 'materialName', title: '成品名称', minWidth: 180 },
    { field: 'modelCode', title: '产品型号', minWidth: 150 },
    {
      field: 'alignmentProgress',
      title: '对齐进度',
      width: 180,
      align: 'center',
      slots: { default: 'alignmentProgress' },
    },
    {
      field: 'alignmentStatus',
      title: '对齐状态',
      width: 160,
      align: 'center',
      slots: { default: 'alignmentStatus' },
    },
    {
      field: 'action',
      title: '操作',
      width: 130,
      fixed: 'right',
      align: 'center',
      slots: { default: 'actions' },
    },
  ];
}
