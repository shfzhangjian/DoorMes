import type { FormSchema } from '#/adapter/form';
import type { VxeGridProps } from '#/adapter/vxe-table';

import { getDictOptions } from '@vben/hooks';

import { getRangePickerDefaultProps } from '#/utils';

export const QMS_EXCEPTION_DICT = {
  action: 'mes_qms_exception_action',
  level: 'mes_qms_exception_level',
  relationType: 'mes_qms_relation_type',
  responsibilityDept: 'mes_srm_apply_department',
  rootCauseCategory: 'mes_qms_4m1e_category',
  status: 'mes_qms_exception_status',
  type: 'mes_qms_exception_type',
} as const;

export function useGridFormSchema(advanced = false): FormSchema[] {
  const schema: FormSchema[] = [
    {
      fieldName: 'exceptionNo',
      label: '异常单号',
      component: 'Input',
      componentProps: { placeholder: '请输入异常单号' },
    },
    {
      fieldName: 'exceptionType',
      label: '异常类别',
      component: 'Select',
      componentProps: {
        allowClear: true,
        options: getDictOptions(QMS_EXCEPTION_DICT.type, 'string'),
        placeholder: '请选择异常类别',
      },
    },
    {
      fieldName: 'discoverTime',
      label: '发现时间',
      component: 'RangePicker',
      componentProps: { ...getRangePickerDefaultProps() },
    },
  ];
  if (advanced) {
    schema.splice(
      2,
      0,
      {
        fieldName: 'exceptionLevel',
        label: '异常等级',
        component: 'Select',
        componentProps: {
          allowClear: true,
          options: getDictOptions(QMS_EXCEPTION_DICT.level, 'string'),
          placeholder: '请选择异常等级',
        },
      },
      {
        fieldName: 'status',
        label: '异常状态',
        component: 'Select',
        componentProps: {
          allowClear: true,
          options: getDictOptions(QMS_EXCEPTION_DICT.status, 'string'),
          placeholder: '请选择异常状态',
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
      field: 'exceptionNo',
      title: '异常单号',
      minWidth: 150,
      slots: { default: 'exceptionNo' },
      fixed: 'left',
    },
    {
      field: 'exceptionType',
      title: '异常类别',
      width: 110,
      slots: { default: 'exceptionType' },
    },
    {
      field: 'exceptionLevel',
      title: '等级',
      width: 90,
      align: 'center',
      slots: { default: 'exceptionLevel' },
    },
    { field: 'discoverDeptName', title: '发现部门', width: 120 },
    { field: 'discovererName', title: '发现人', width: 100, align: 'center' },
    { field: 'discoverTime', title: '发现时间', width: 170, align: 'center' },
    {
      field: 'slaStatus',
      title: '48H围堵时效',
      width: 150,
      align: 'center',
      slots: { default: 'slaStatus' },
    },
    {
      field: 'currentNodeName',
      title: '当前环节',
      minWidth: 160,
      align: 'center',
      slots: { default: 'currentNodeName' },
    },
    {
      field: 'currentUserTaskTodoLabel',
      title: '我的任务',
      width: 140,
      align: 'center',
      slots: { default: 'currentUserTaskTodo' },
    },
    {
      field: 'status',
      title: '状态',
      width: 110,
      align: 'center',
      slots: { default: 'status' },
    },
    {
      title: '操作',
      width: 220,
      fixed: 'right',
      slots: { default: 'actions' },
    },
  ];
}
