import type { FormSchema } from '#/adapter/form';
import type { VxeGridProps } from '#/adapter/vxe-table';

import { getRangePickerDefaultProps } from '#/utils';

import { processCategoryOptions } from '../../fai/data';

export const PRODUCT_ABNORMAL_EVENT_SOURCE_OPTIONS = [
  { label: '首件检验', value: 'FAI' },
  { label: '胶板检验', value: 'GLUE_BOARD_FAI' },
  { label: '裁切成品检验', value: 'CUT_ROUND_FQC' },
  { label: '发货成品检验', value: 'FG_SHIPPING_FQC' },
  { label: '出货检验(OQC)', value: 'OQC' },
] as const;

export const NCR_GENERATION_STATUS_OPTIONS = [
  { label: '未生成NCR', value: 'PENDING' },
  { label: '已生成', value: 'GENERATED' },
] as const;

export const RECHECK_STATUS_OPTIONS = [
  { label: '全部', value: 'ALL' },
  { label: '未驳回', value: 'NONE' },
  { label: '复检中', value: 'RECHECKING' },
  { label: '复检OK', value: 'RECHECK_OK' },
  { label: '复检NG', value: 'RECHECK_NG' },
] as const;

export function useGridFormSchema(advanced = false): FormSchema[] {
  const schema: FormSchema[] = [
    {
      fieldName: 'productBatchNo',
      label: '产品批次',
      component: 'Input',
      componentProps: { placeholder: '请输入产品批次' },
    },
    {
      fieldName: 'processCategory',
      label: '工序',
      component: 'Select',
      componentProps: {
        allowClear: true,
        options: processCategoryOptions,
        placeholder: '请选择工序',
      },
    },
    {
      fieldName: 'productModel',
      label: '产品型号',
      component: 'Input',
      componentProps: { placeholder: '请输入产品型号' },
    },
  ];
  if (advanced) {
    schema.push(
      {
        fieldName: 'recheckStatus',
        label: '复检状态',
        component: 'Select',
        defaultValue: 'ALL',
        componentProps: {
          allowClear: false,
          options: RECHECK_STATUS_OPTIONS,
        },
      },
      {
        fieldName: 'sourceType',
        label: '检验类型',
        component: 'Select',
        componentProps: {
          allowClear: true,
          options: PRODUCT_ABNORMAL_EVENT_SOURCE_OPTIONS,
          placeholder: '请选择检验类型',
        },
      },
      {
        fieldName: 'inspectionNo',
        label: '检验单号',
        component: 'Input',
        componentProps: { placeholder: '请输入检验单号' },
      },
      {
        fieldName: 'inspectionTime',
        label: '检验时间',
        component: 'RangePicker',
        componentProps: { ...getRangePickerDefaultProps() },
      },
    );
  }
  return schema;
}

export function useGridColumns(): VxeGridProps['columns'] {
  return [
    { type: 'checkbox', width: 46, align: 'center', fixed: 'left' },
    { type: 'seq', width: 52, align: 'center', fixed: 'left' },
    {
      field: 'inspectionNo',
      title: '检验单号',
      minWidth: 180,
      fixed: 'left',
      slots: { default: 'inspectionNo' },
    },
    {
      field: 'inspectionType',
      title: '检验类型',
      width: 140,
      align: 'center',
      slots: { default: 'inspectionType' },
    },
    { field: 'operationName', title: '工序', width: 130 },
    { field: 'productBatchNo', title: '产品批次', minWidth: 170 },
    { field: 'productModel', title: '产品型号', minWidth: 160 },
    { field: 'specification', title: '规格', minWidth: 140 },
    {
      field: 'inspectionQty',
      title: '检验数量',
      width: 110,
      align: 'right',
      slots: { default: 'inspectionQty' },
    },
    {
      field: 'unqualifiedQty',
      title: '不合格项数量',
      width: 110,
      align: 'right',
      slots: { default: 'unqualifiedQty' },
    },
    {
      field: 'judgment',
      title: '检验判定',
      width: 100,
      align: 'center',
      slots: { default: 'judgment' },
    },
    {
      field: 'abnormalSummary',
      title: '检验不合格项总结',
      minWidth: 360,
      slots: { default: 'abnormalSummary' },
    },
    {
      field: 'ncrNo',
      title: 'NCR单号',
      minWidth: 170,
      slots: { default: 'ncrNo' },
    },
    {
      field: 'ncrStatus',
      title: 'NCR状态',
      width: 110,
      align: 'center',
      slots: { default: 'ncrStatus' },
    },
    {
      field: 'rejectNextInspectionNo',
      title: '驳回单号',
      minWidth: 170,
      slots: { default: 'rejectNextInspectionNo' },
    },
    {
      field: 'recheckResult',
      title: '复检结果',
      width: 110,
      align: 'center',
      slots: { default: 'recheckResult' },
    },
    {
      field: 'recheckCount',
      title: '复检次数',
      width: 100,
      align: 'right',
      slots: { default: 'recheckCount' },
    },
    {
      title: '操作',
      width: 150,
      align: 'center',
      fixed: 'right',
      slots: { default: 'actions' },
    },
  ];
}
