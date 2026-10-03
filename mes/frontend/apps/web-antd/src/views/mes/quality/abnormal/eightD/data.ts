import type { FormSchema } from '#/adapter/form';
import type { VxeGridProps } from '#/adapter/vxe-table';

import { getDictOptions } from '@vben/hooks';

import { getRangePickerDefaultProps } from '#/utils';

export const QMS_8D_DICT = {
  action: 'mes_qms_8d_action',
  actionItemStatus: 'mes_qms_8d_action_item_status',
  memberRole: 'mes_qms_8d_member_role',
  rootCauseCategory: 'mes_qms_4m1e_category',
  sourceType: 'mes_qms_8d_source_type',
  status: 'mes_qms_8d_status',
  step: 'mes_qms_8d_step',
} as const;

export const QMS_8D_ACTION_ITEM_TYPE_OPTIONS = [
  { label: '围堵', value: 'CONTAINMENT' },
  { label: '纠正', value: 'CORRECTIVE' },
  { label: '预防', value: 'PREVENTIVE' },
  { label: '标准化', value: 'STANDARDIZE' },
];

export function useGridFormSchema(advanced = false): FormSchema[] {
  const schema: FormSchema[] = [
    {
      fieldName: 'reportNo',
      label: '8D报告单号',
      component: 'Input',
      componentProps: { placeholder: '请输入 8D 报告号' },
    },
    {
      fieldName: 'sourceNo',
      label: '来源单号',
      component: 'Input',
      componentProps: { placeholder: '异常/NCR/客诉/审核单号' },
    },
    {
      fieldName: 'issueDate',
      label: '发起日期',
      component: 'RangePicker',
      componentProps: { ...getRangePickerDefaultProps() },
    },
  ];
  if (advanced) {
    schema.splice(
      2,
      0,
      {
        fieldName: 'sourceType',
        label: '来源类型',
        component: 'Select',
        componentProps: {
          allowClear: true,
          options: getDictOptions(QMS_8D_DICT.sourceType, 'string'),
          placeholder: '请选择来源类型',
        },
      },
      {
        fieldName: 'currentStep',
        label: '当前阶段',
        component: 'Select',
        componentProps: {
          allowClear: true,
          options: getDictOptions(QMS_8D_DICT.step, 'string'),
          placeholder: '请选择阶段',
        },
      },
      {
        fieldName: 'status',
        label: '8D状态',
        component: 'Select',
        componentProps: {
          allowClear: true,
          options: getDictOptions(QMS_8D_DICT.status, 'string'),
          placeholder: '请选择状态',
        },
      },
      {
        fieldName: 'targetDate',
        label: '要求结案日',
        component: 'RangePicker',
        componentProps: { ...getRangePickerDefaultProps() },
      },
    );
  }
  return schema;
}

export function useGridColumns(): VxeGridProps['columns'] {
  return [
    { type: 'seq', width: 50, align: 'center', fixed: 'left' },
    {
      field: 'reportNo',
      title: '8D报告号',
      minWidth: 160,
      slots: { default: 'reportNo' },
      fixed: 'left',
    },
    {
      field: 'sourceType',
      title: '来源类型',
      width: 130,
      align: 'center',
      slots: { default: 'sourceType' },
    },
    { field: 'sourceNo', title: '追溯凭证号', width: 170 },
    { field: 'problemDesc', title: '问题简述(D2)', minWidth: 260 },
    {
      field: 'currentStep',
      title: '当前进度',
      width: 160,
      align: 'center',
      slots: { default: 'currentStep' },
    },
    { field: 'currentHandlerUserName', title: '当前处理人', width: 120 },
    { field: 'issueDate', title: '立案日期', width: 110, align: 'center' },
    {
      field: 'targetDate',
      title: '要求结案日',
      width: 130,
      align: 'center',
      slots: { default: 'targetDate' },
    },
    {
      field: 'status',
      title: '单据状态',
      width: 110,
      align: 'center',
      slots: { default: 'status' },
    },
    {
      title: '操作',
      width: 110,
      fixed: 'right',
      slots: { default: 'actions' },
    },
  ];
}
