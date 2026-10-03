import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';

import { getRangePickerDefaultProps } from '#/utils';

export function useFormSchema(formType = ''): VbenFormSchema[] {
  const isDetail = formType === 'detail';

  return [
    {
      fieldName: 'deviceCode',
      label: '设备编码',
      component: 'Input',
      rules: 'required',
      componentProps: { disabled: isDetail },
    },
    {
      fieldName: 'deviceName',
      label: '设备名称',
      component: 'Input',
      rules: 'required',
      componentProps: { disabled: isDetail },
    },
    {
      fieldName: 'exceptionLevel',
      label: '异常等级',
      component: 'Select',
      componentProps: {
        disabled: isDetail,
        options: [
          { label: '一般', value: 'MINOR' },
          { label: '重大', value: 'MAJOR' },
          { label: '紧急', value: 'CRITICAL' },
        ],
      },
    },
    {
      fieldName: 'reporter',
      label: '提报人',
      component: 'Input',
      componentProps: { disabled: isDetail },
    },
    {
      fieldName: 'reportTime',
      label: '提报时间',
      component: 'DatePicker',
      componentProps: {
        disabled: isDetail,
        showTime: true,
        valueFormat: 'YYYY-MM-DD HH:mm:ss',
        class: 'w-full',
      },
    },
    {
      fieldName: 'assignee',
      label: '维修人',
      component: 'Input',
      componentProps: { disabled: isDetail },
    },
    {
      fieldName: 'repairTime',
      label: '维修时间',
      component: 'DatePicker',
      componentProps: {
        disabled: isDetail,
        showTime: true,
        valueFormat: 'YYYY-MM-DD HH:mm:ss',
        class: 'w-full',
      },
    },
    {
      fieldName: 'status',
      label: '状态',
      component: 'Select',
      componentProps: {
        disabled: isDetail,
        options: [
          { label: '响应分派', value: 'REPORTED' },
          { label: '维修执行', value: 'DISPATCHED' },
          { label: '完成确认', value: 'PENDING_CONFIRM' },
          { label: '关闭归档', value: 'PENDING_ARCHIVE' },
          { label: '已关闭/归档', value: 'CLOSED' },
        ],
      },
    },
    {
      fieldName: 'faultDesc',
      label: '异常现象',
      component: 'Textarea',
      rules: 'required',
      componentProps: { rows: 3, disabled: isDetail },
    },
    {
      fieldName: 'faultReason',
      label: '故障原因',
      component: 'Textarea',
      componentProps: { rows: 3, disabled: isDetail },
    },
    {
      fieldName: 'repairAction',
      label: '维修措施',
      component: 'Textarea',
      componentProps: { rows: 3, disabled: isDetail },
    },
    {
      fieldName: 'rootCause',
      label: '根因分析',
      component: 'Textarea',
      componentProps: { rows: 3, disabled: isDetail },
    },
    {
      fieldName: 'preventiveAction',
      label: '预防措施',
      component: 'Textarea',
      componentProps: { rows: 3, disabled: isDetail },
    },
    {
      fieldName: 'attachments',
      label: '附件地址',
      component: 'Textarea',
      componentProps: {
        rows: 2,
        disabled: isDetail,
        placeholder: '可粘贴多个附件地址，换行分隔',
      },
    },
  ];
}

export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      fieldName: 'orderNo',
      label: '异常单号',
      component: 'Input',
      componentProps: { placeholder: '输入单号', allowClear: true },
    },
    {
      fieldName: 'deviceName',
      label: '异常设备',
      component: 'Input',
      componentProps: { placeholder: '模糊检索', allowClear: true },
    },
    {
      fieldName: 'status',
      label: '单据状态',
      component: 'Select',
      componentProps: {
        options: [
          { label: '响应分派', value: 'REPORTED' },
          { label: '维修执行', value: 'DISPATCHED' },
          { label: '完成确认', value: 'PENDING_CONFIRM' },
          { label: '关闭归档', value: 'PENDING_ARCHIVE' },
          { label: '已关闭/归档', value: 'CLOSED' },
        ],
        allowClear: true,
      },
    },
    {
      fieldName: 'createTime',
      label: '提报日期',
      component: 'RangePicker',
      componentProps: { ...getRangePickerDefaultProps(), allowClear: true },
    },
  ];
}

export function useGridColumns(): VxeTableGridOptions['columns'] {
  return [
    { type: 'seq', title: '序号', width: 60, align: 'center', fixed: 'left' },
    {
      field: 'orderNo',
      title: '异常事件单号',
      minWidth: 160,
      slots: { default: 'orderNo' },
      fixed: 'left',
    },
    {
      field: 'deviceName',
      title: '异常设备',
      minWidth: 160,
      slots: { default: 'deviceName' },
    },
    {
      field: 'faultDesc',
      title: '异常现象描述',
      minWidth: 200,
      showOverflow: 'tooltip',
    },
    {
      field: 'node',
      title: '当前进度',
      minWidth: 160,
      align: 'center',
      slots: { default: 'node' },
    },
    {
      field: 'status',
      title: '业务状态',
      minWidth: 120,
      align: 'center',
      slots: { default: 'status' },
    },
    { field: 'reportTime', title: '提报时间', minWidth: 150, align: 'center' },
    {
      title: '操作',
      width: 140,
      fixed: 'right',
      slots: { default: 'actions' },
    },
  ];
}

export function usePartGridColumns(): VxeTableGridOptions['columns'] {
  return [
    {
      field: 'sort',
      title: '序号',
      width: 60,
      align: 'center',
      slots: { default: 'sort' },
    },
    {
      field: 'partName',
      title: '消耗备件名称',
      minWidth: 160,
      slots: { default: 'partName' },
    },
    {
      field: 'partCode',
      title: '备件编码',
      width: 130,
      slots: { default: 'partCode' },
    },
    {
      field: 'quantity',
      title: '数量',
      width: 100,
      slots: { default: 'quantity' },
    },
    {
      field: 'unit',
      title: '单位',
      width: 80,
      align: 'center',
      slots: { default: 'unit' },
    },
    {
      field: 'remark',
      title: '更换说明',
      minWidth: 150,
      slots: { default: 'remark' },
    },
    {
      title: '操作',
      width: 80,
      fixed: 'right',
      align: 'center',
      slots: { default: 'actions' },
    },
  ];
}
