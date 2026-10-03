import type { VbenFormSchema } from '#/adapter/form';
import type { VxeGridProps } from '#/adapter/vxe-table';

import { getRangePickerDefaultProps } from '#/utils';

export const CutRoundFqcStatusOptions = [
  { color: 'warning', label: '待检测', value: 'PENDING' },
  { color: 'processing', label: '检测中', value: 'INSPECTING' },
  { color: 'purple', label: '待审核', value: 'WAITING_QA' },
  { color: 'success', label: '已完成', value: 'COMPLETED' },
  { color: 'error', label: '已驳回', value: 'REJECTED' },
  { color: 'error', label: '已取消', value: 'CANCELED' },
];

export const CutRoundFqcJudgmentOptions = [
  { color: 'default', label: '待判定', value: 'PENDING' },
  { color: 'success', label: '合格', value: 'OK' },
  { color: 'error', label: '片级管控', value: 'NG' },
];

export const recheckFlagOptions = [
  { label: '复检单', value: true },
  { label: '非复检单', value: false },
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
      componentProps: { placeholder: '输入FQC单号' },
      fieldName: 'fqcNo',
      label: 'FQC单号',
    },
    {
      component: 'Input',
      componentProps: { placeholder: '输入计划号' },
      fieldName: 'planNo',
      label: '计划号',
    },
    {
      component: 'Input',
      componentProps: { placeholder: '输入裁切片号' },
      fieldName: 'productionBatchNo',
      label: '裁切片号',
    },
    {
      component: 'Input',
      componentProps: { placeholder: '输入物料编码' },
      fieldName: 'materialCode',
      label: '物料编码',
    },
    {
      component: 'Input',
      componentProps: { placeholder: '输入产品型号' },
      fieldName: 'productModel',
      label: '产品型号',
    },
    {
      component: 'Select',
      componentProps: { allowClear: true, options: CutRoundFqcStatusOptions },
      fieldName: 'status',
      label: '单据状态',
    },
    {
      component: 'Select',
      componentProps: { allowClear: true, options: CutRoundFqcJudgmentOptions },
      fieldName: 'judgment',
      label: '判定结果',
    },
    {
      component: 'Select',
      componentProps: { allowClear: true, options: recheckFlagOptions },
      fieldName: 'recheckFlag',
      label: '复检标记',
    },
    {
      component: 'RangePicker',
      componentProps: { ...getRangePickerDefaultProps() },
      fieldName: 'submissionTime',
      label: '送检时间',
    },
  ];
}

export function useGridColumns(): VxeGridProps['columns'] {
  return [
    { align: 'center', fixed: 'left', type: 'seq', width: 50 },
    {
      field: 'fqcNo',
      fixed: 'left',
      minWidth: 170,
      slots: { default: 'fqcNo' },
      title: 'FQC单号',
    },
    { field: 'workOrderNo', minWidth: 150, title: '计划号' },
    { field: 'materialCode', minWidth: 140, title: '物料编码' },
    { field: 'productModel', minWidth: 140, title: '产品型号' },
    { field: 'productBatchNo', minWidth: 160, title: '产品批次' },
    {
      align: 'right',
      field: 'submissionDetailCount',
      title: '送检片数',
      width: 100,
    },
    { align: 'right', field: 'okQty', title: 'OK', width: 80 },
    { align: 'right', field: 'ngQty', title: 'NG', width: 80 },
    {
      align: 'center',
      field: 'status',
      slots: { default: 'status' },
      title: '状态',
      width: 110,
    },
    {
      align: 'center',
      field: 'judgment',
      slots: { default: 'judgment' },
      title: '判定',
      width: 110,
    },
    {
      align: 'center',
      field: 'recheckFlag',
      slots: { default: 'recheckFlag' },
      title: '复检',
      width: 100,
    },
    { field: 'originalInspectionNo', minWidth: 150, title: '原检验单号' },
    {
      align: 'center',
      field: 'submissionTime',
      title: '送检时间',
      width: 170,
    },
    { field: 'submitterName', title: '送检人', width: 110 },
    {
      align: 'center',
      field: 'inspectionTime',
      title: '检验时间',
      width: 170,
    },
    { field: 'inspectorName', title: '检验人', width: 110 },
    {
      align: 'center',
      field: 'qaTime',
      title: '审核时间',
      width: 170,
    },
    { field: 'qaInspectorName', title: '审核人', width: 110 },
    {
      align: 'center',
      field: 'action',
      fixed: 'right',
      slots: { default: 'actions' },
      title: '操作',
      width: 260,
    },
  ];
}
