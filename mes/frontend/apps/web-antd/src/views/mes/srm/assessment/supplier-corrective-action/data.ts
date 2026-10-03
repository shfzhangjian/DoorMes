import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';

import { getRangePickerDefaultProps } from '#/utils';

const statusOptions = [
  { label: '待供方回复', value: 'WAIT_SUPPLIER' },
  { label: '已整改关闭', value: 'CLOSED' },
];

export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      component: 'Input',
      componentProps: { allowClear: true, placeholder: '请输入台账编号' },
      fieldName: 'scarNo',
      label: '台账编号',
    },
    {
      component: 'Input',
      componentProps: { allowClear: true, placeholder: '供应商名称模糊搜索' },
      fieldName: 'supplierName',
      label: '供应商名称',
    },
    {
      component: 'Input',
      componentProps: { allowClear: true, placeholder: '物料名称模糊搜索' },
      fieldName: 'materialName',
      label: '物料名称',
    },
    {
      component: 'Select',
      componentProps: { allowClear: true, options: statusOptions },
      fieldName: 'status',
      label: '状态',
    },
    {
      component: 'RangePicker',
      componentProps: {
        ...getRangePickerDefaultProps(),
        allowClear: true,
        format: 'YYYY-MM-DD',
        valueFormat: 'YYYY-MM-DD',
      },
      fieldName: 'issueDate',
      label: '异常发生日期',
    },
  ];
}

export function useGridColumns(): VxeTableGridOptions['columns'] {
  return [
    { align: 'center', fixed: 'left', title: '序号', type: 'seq', width: 60 },
    {
      field: 'scarNo',
      fixed: 'left',
      minWidth: 180,
      slots: { default: 'scarNo' },
      title: '台账编号',
    },
    {
      align: 'center',
      field: 'issueDate',
      minWidth: 120,
      title: '异常发生日期',
    },
    { field: 'supplierCode', minWidth: 180, title: '供应商代码' },
    {
      field: 'supplierName',
      minWidth: 240,
      slots: { default: 'supplierName' },
      title: '供应商名称',
    },
    { field: 'materialCode', minWidth: 160, title: '物料代码' },
    {
      field: 'materialName',
      minWidth: 200,
      slots: { default: 'materialName' },
      title: '物料名称',
    },
    { field: 'materialModel', minWidth: 180, title: '物料型号' },
    { field: 'batchNo', minWidth: 160, title: '物料批次' },
    { align: 'right', field: 'quantity', minWidth: 120, title: '数量' },
    { field: 'issueDesc', minWidth: 300, title: '异常描述' },
    {
      align: 'center',
      field: 'replyDate',
      minWidth: 120,
      title: '异常回复日期',
    },
    { field: 'replyDesc', minWidth: 300, title: '异常回复说明' },
    {
      align: 'center',
      field: 'status',
      minWidth: 120,
      slots: { default: 'status' },
      title: '状态',
    },
    { align: 'center', field: 'applicantName', minWidth: 110, title: '登记人' },
    {
      align: 'center',
      field: 'applyTime',
      minWidth: 170,
      title: '登记时间',
    },
    {
      fixed: 'right',
      slots: { default: 'actions' },
      title: '操作',
      width: 140,
    },
  ];
}