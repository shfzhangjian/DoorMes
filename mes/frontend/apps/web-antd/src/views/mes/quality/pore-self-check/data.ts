import type { VbenFormSchema } from '#/adapter/form';
import type { VxeGridProps } from '#/adapter/vxe-table';

import { getRangePickerDefaultProps } from '#/utils';

export const PoreSelfCheckResultOptions = [
  { color: 'success', label: '合格', value: 'OK' },
  { color: 'error', label: '异常', value: 'NG' },
];

export const PoreSelfCheckImageStatusOptions = [
  { color: 'success', label: '已上传', value: 'UPLOADED' },
  { color: 'warning', label: '未上传', value: 'MISSING' },
];

export function optionMeta(
  options: Array<{ color?: string; label: string; value: string }>,
  value?: string,
) {
  return options.find((item) => item.value === value) || {
    color: 'default',
    label: value || '-',
    value: value || '',
  };
}

export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      component: 'Input',
      componentProps: { placeholder: '计划号/料号/批号/检验人' },
      fieldName: 'keyword',
      label: '关键字',
    },
    {
      component: 'Input',
      componentProps: { placeholder: '输入计划号' },
      fieldName: 'planNo',
      label: '计划号',
    },
    {
      component: 'Input',
      componentProps: { placeholder: '输入产品型号' },
      fieldName: 'productModel',
      label: '产品型号',
    },
    {
      component: 'Input',
      componentProps: { placeholder: '输入母批批号' },
      fieldName: 'motherBatchNo',
      label: '母批批号',
    },
    {
      component: 'Input',
      componentProps: { placeholder: '输入产品料号' },
      fieldName: 'productMaterialCode',
      label: '产品料号',
    },
    {
      component: 'Select',
      componentProps: {
        allowClear: true,
        options: PoreSelfCheckResultOptions,
      },
      fieldName: 'selfCheckResult',
      hide: true,
      label: '自检结果',
    },
    {
      component: 'Select',
      componentProps: {
        allowClear: true,
        options: PoreSelfCheckImageStatusOptions,
      },
      fieldName: 'imageStatus',
      label: '图片状态',
    },
    {
      component: 'RangePicker',
      componentProps: { ...getRangePickerDefaultProps() },
      fieldName: 'selfCheckTime',
      label: '自检时间',
    },
  ];
}

export function useGridColumns(): VxeGridProps['columns'] {
  return [
    { align: 'center', fixed: 'left', type: 'seq', width: 50 },
    {
      field: 'planNo',
      fixed: 'left',
      minWidth: 160,
      slots: { default: 'planNo' },
      title: '计划号',
    },
    { field: 'productModel', minWidth: 140, title: '产品型号' },
    { field: 'motherBatchNo', minWidth: 160, title: '母批批号' },
    { field: 'productMaterialCode', minWidth: 150, title: '产品料号' },
    { field: 'operationName', minWidth: 130, title: '工序' },
    {
      align: 'center',
      field: 'selfCheckTime',
      title: '自检时间',
      width: 170,
    },
    {
      align: 'center',
      field: 'selfCheckResult',
      slots: { default: 'selfCheckResult' },
      title: '自检结果',
      visible: false,
      width: 110,
    },
    { field: 'inspector', title: '检验人', width: 110 },
    {
      align: 'center',
      field: 'imageStatus',
      slots: { default: 'imageStatus' },
      title: '图片状态',
      width: 110,
    },
    {
      align: 'center',
      field: 'photo',
      slots: { default: 'photo' },
      title: '泡孔图片',
      width: 120,
    },
    { field: 'remark', minWidth: 180, title: '备注' },
    {
      align: 'center',
      field: 'recordTime',
      title: '湿法记录时间',
      width: 170,
    },
    { field: 'recordUserName', title: '记录人', width: 110 },
    {
      align: 'center',
      field: 'action',
      fixed: 'right',
      slots: { default: 'actions' },
      title: '操作',
      width: 270,
    },
  ];
}
