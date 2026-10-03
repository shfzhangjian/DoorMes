import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';

import { getRangePickerDefaultProps } from '#/utils';

const yesNoOptions = [
  { label: '是', value: true },
  { label: '否', value: false },
];

export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      component: 'Input',
      componentProps: { allowClear: true, placeholder: '请输入调查表编号' },
      fieldName: 'surveyNo',
      label: '调查表编号',
    },
    {
      component: 'Input',
      componentProps: { allowClear: true, placeholder: '供应商名称模糊搜索' },
      fieldName: 'supplierName',
      label: '供应商名称',
    },
    {
      component: 'Select',
      componentProps: { allowClear: true, options: yesNoOptions },
      fieldName: 'unregisteredSupplier',
      label: '未入库供应商',
    },
    {
      component: 'RangePicker',
      componentProps: {
        ...getRangePickerDefaultProps(),
        allowClear: true,
        format: 'YYYY-MM-DD',
        valueFormat: 'YYYY-MM-DD',
      },
      fieldName: 'surveyDate',
      label: '调查日期',
    },
  ];
}

export function useGridColumns(): VxeTableGridOptions['columns'] {
  return [
    { align: 'center', fixed: 'left', title: '序号', type: 'seq', width: 60 },
    {
      field: 'surveyNo',
      fixed: 'left',
      minWidth: 180,
      slots: { default: 'surveyNo' },
      title: '调查表编号',
    },
    { field: 'supplierCode', minWidth: 180, title: '供应商代码' },
    {
      field: 'supplierName',
      minWidth: 240,
      slots: { default: 'supplierName' },
      title: '供应商名称',
    },
    {
      align: 'center',
      field: 'unregisteredSupplier',
      minWidth: 130,
      slots: { default: 'unregisteredSupplier' },
      title: '未入库供应商',
    },
    { align: 'center', field: 'surveyDate', minWidth: 120, title: '调查日期' },
    { field: 'conclusion', minWidth: 300, title: '调查结论说明' },
    { align: 'center', field: 'applicantName', minWidth: 110, title: '登记人' },
    {
      align: 'center',
      field: 'applyTime',
      minWidth: 170,
      title: '调查表登记时间',
    },
    {
      align: 'center',
      field: 'updateTime',
      minWidth: 170,
      title: '更新时间',
    },
    {
      fixed: 'right',
      slots: { default: 'actions' },
      title: '操作',
      width: 140,
    },
  ];
}
