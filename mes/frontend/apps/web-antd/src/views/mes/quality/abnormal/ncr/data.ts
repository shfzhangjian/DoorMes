import type { FormSchema } from '#/adapter/form';
import type { VxeGridProps } from '#/adapter/vxe-table';

import { getDictOptions } from '@vben/hooks';

import { getRangePickerDefaultProps } from '#/utils';

export const QMS_NCR_DICT = {
  action: 'mes_qms_ncr_action',
  disposition: 'mes_qms_ncr_disposition',
  level: 'mes_qms_exception_level',
  mrbReviewStatus: 'mes_qms_mrb_review_status',
  responsibilityDept: 'mes_qms_ncr_responsibility_dept',
  rootCauseCategory: 'mes_qms_4m1e_category',
  sourceBizType: 'mes_qms_ncr_source_biz_type',
  sourceType: 'mes_qms_ncr_source_type',
  status: 'mes_qms_ncr_status',
} as const;

export function useGridFormSchema(advanced = false): FormSchema[] {
  const schema: FormSchema[] = [
    {
      fieldName: 'ncNo',
      label: 'NCR单号',
      component: 'Input',
      componentProps: { placeholder: '请输入 NCR 单号' },
    },
    {
      fieldName: 'lotNo',
      label: '异常批号',
      component: 'Input',
      componentProps: { placeholder: '请输入批号' },
    },
    {
      fieldName: 'happenTime',
      label: '发生时间',
      component: 'RangePicker',
      componentProps: { ...getRangePickerDefaultProps() },
    },
  ];
  if (advanced) {
    schema.splice(
      2,
      0,
      {
        fieldName: 'ncLevel',
        label: '不合格等级',
        component: 'Select',
        componentProps: {
          allowClear: true,
          options: getDictOptions(QMS_NCR_DICT.level, 'string'),
          placeholder: '请选择等级',
        },
      },
      {
        fieldName: 'status',
        label: 'NCR状态',
        component: 'Select',
        componentProps: {
          allowClear: true,
          options: getDictOptions(QMS_NCR_DICT.status, 'string'),
          placeholder: '请选择状态',
        },
      },
      {
        fieldName: 'finalDisposition',
        label: '处置结论',
        component: 'Select',
        componentProps: {
          allowClear: true,
          options: getDictOptions(QMS_NCR_DICT.disposition, 'string'),
          placeholder: '请选择处置结论',
        },
      },
    );
  }
  return schema;
}

export function useGridColumns(): VxeGridProps['columns'] {
  return [
    { type: 'seq', width: 50, align: 'center', fixed: 'left' },
    {
      field: 'ncNo',
      title: 'NCR单号',
      minWidth: 170,
      fixed: 'left',
      slots: { default: 'ncNo' },
    },
    {
      field: 'sourceType',
      title: '类型',
      width: 110,
      align: 'center',
      slots: { default: 'sourceType' },
    },
    {
      field: 'materialName',
      title: '物料',
      minWidth: 170,
      slots: { default: 'materialName' },
    },
    { field: 'lotNo', title: '异常批号', width: 140 },
    { field: 'defectCode', title: '缺陷代码', width: 120 },
    {
      field: 'defectQty',
      title: '不良数',
      width: 90,
      align: 'right',
    },
    {
      field: 'ncLevel',
      title: '等级',
      width: 90,
      align: 'center',
      slots: { default: 'ncLevel' },
    },
    {
      field: 'currentNodeName',
      title: '当前环节',
      minWidth: 150,
      align: 'center',
      slots: { default: 'currentNodeName' },
    },
    {
      field: 'status',
      title: '状态',
      width: 110,
      align: 'center',
      slots: { default: 'status' },
    },
    {
      field: 'finalDisposition',
      title: '处置结论',
      width: 110,
      align: 'center',
      slots: { default: 'finalDisposition' },
    },
    {
      field: 'applicantUserName',
      title: '发起人',
      width: 100,
      align: 'center',
    },
    { field: 'happenTime', title: '发生时间', width: 170, align: 'center' },
    {
      title: '操作',
      width: 128,
      fixed: 'right',
      slots: { default: 'actions' },
    },
  ];
}
