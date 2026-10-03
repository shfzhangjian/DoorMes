import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesMaintTaskApi } from '#/api/mes/resource/device/maint-task';

import dayjs from 'dayjs';

import { getRangePickerDefaultProps } from '#/utils';

import { optionLabel } from '../shared';

type Option = { label?: string; value?: number };

export const MAINT_TYPE_OPTIONS = [
  { color: 'blue', label: '日常巡检', value: '日常巡检' },
  { color: 'green', label: '一级保养', value: '一级保养' },
  { color: 'orange', label: '二级保养', value: '二级保养' },
  { color: 'red', label: '三级大修', value: '三级大修' },
];

export const MAINT_RESULT_OPTIONS = [
  { color: 'default', label: '待执行', value: 10 },
  { color: 'success', label: '正常完成', value: 20 },
  { color: 'error', label: '异常完成', value: 30 },
];

export function getMaintRecordMonthRange(month = dayjs().format('YYYY-MM')) {
  const targetMonth = dayjs(`${month}-01`);
  return [
    targetMonth.startOf('month').format('YYYY-MM-DD HH:mm:ss'),
    targetMonth.endOf('month').format('YYYY-MM-DD HH:mm:ss'),
  ];
}

export function useFormSchema(_formType: string): VbenFormSchema[] {
  return [
    { component: 'Input', dependencies: { show: () => false, triggerFields: [''] }, fieldName: 'id' },
    { component: 'Input', componentProps: { disabled: true }, fieldName: 'recordNo', label: '记录编号' },
    { component: 'Input', componentProps: { disabled: true }, fieldName: 'taskNo', label: '任务单号' },
    { component: 'Input', componentProps: { disabled: true }, fieldName: 'deviceCode', label: '设备编码' },
    { component: 'Input', componentProps: { disabled: true }, fieldName: 'deviceName', label: '设备名称' },
    { component: 'Input', componentProps: { disabled: true }, fieldName: 'categoryName', label: '设备分类' },
    { component: 'Input', componentProps: { disabled: true }, fieldName: 'standardName', label: '应用标准' },
    { component: 'Input', componentProps: { disabled: true }, fieldName: 'maintType', label: '维保类型' },
    { component: 'Input', componentProps: { disabled: true }, fieldName: 'operator', label: '执行人' },
    { component: 'Input', componentProps: { disabled: true }, fieldName: 'dueDate', label: '应检日期' },
    { component: 'Input', componentProps: { disabled: true }, fieldName: 'planTime', label: '计划时间' },
    { component: 'Input', componentProps: { disabled: true }, fieldName: 'actualTime', label: '完成时间' },
    {
      component: 'Select',
      componentProps: { disabled: true, options: MAINT_RESULT_OPTIONS },
      fieldName: 'status',
      label: '执行结果',
    },
    { component: 'Textarea', componentProps: { disabled: true, rows: 3 }, fieldName: 'remark', formItemClass: 'col-span-2', label: '异常/备注' },
  ];
}

export function useGridFormSchema(categoryOptions: Option[] = [], advanced = false): VbenFormSchema[] {
  const schema: VbenFormSchema[] = [
    { component: 'Input', componentProps: { allowClear: true }, fieldName: 'deviceCode', label: '设备编码' },
    { component: 'Input', componentProps: { allowClear: true }, fieldName: 'deviceName', label: '设备名称' },
    {
      component: 'Select',
      componentProps: { allowClear: true, optionFilterProp: 'label', options: categoryOptions, showSearch: true },
      fieldName: 'categoryId',
      label: '设备分类',
    },
    {
      component: 'RangePicker',
      componentProps: { ...getRangePickerDefaultProps(), allowClear: true },
      defaultValue: getMaintRecordMonthRange(),
      fieldName: 'actualTime',
      label: '完成时间',
    },
  ];

  if (advanced) {
    schema.push(
      { component: 'Input', componentProps: { allowClear: true }, fieldName: 'taskNo', label: '任务单号' },
      { component: 'Input', componentProps: { allowClear: true }, fieldName: 'recordNo', label: '记录编号' },
      {
        component: 'Select',
        componentProps: { allowClear: true, options: MAINT_TYPE_OPTIONS },
        fieldName: 'maintType',
        label: '维保类型',
      },
      {
        component: 'Select',
        componentProps: { allowClear: true, options: MAINT_RESULT_OPTIONS },
        fieldName: 'status',
        label: '执行结果',
      },
    );
  }

  return schema;
}

export function useGridColumns(): VxeTableGridOptions<MesMaintTaskApi.Task>['columns'] {
  return [
    { align: 'center', fixed: 'left', title: '序号', type: 'seq', width: 60 },
    { field: 'recordNo', fixed: 'left', minWidth: 170, title: '记录编号' },
    { field: 'taskNo', fixed: 'left', minWidth: 160, title: '任务单号' },
    { field: 'deviceCode', minWidth: 140, title: '设备编码' },
    { field: 'deviceName', minWidth: 170, title: '设备名称' },
    { field: 'categoryName', minWidth: 130, title: '设备分类' },
    { field: 'standardName', minWidth: 180, title: '应用标准' },
    {
      align: 'center',
      field: 'maintType',
      formatter: ({ cellValue }) => optionLabel(MAINT_TYPE_OPTIONS, cellValue),
      title: '维保类型',
      width: 110,
    },
    { field: 'operator', title: '执行人', width: 100 },
    { align: 'center', field: 'dueDate', title: '应检日期', width: 120 },
    {
      align: 'center',
      field: 'status',
      formatter: ({ cellValue }) => optionLabel(MAINT_RESULT_OPTIONS, cellValue),
      slots: { default: 'status' },
      title: '执行结果',
      width: 110,
    },
    { align: 'center', field: 'planTime', title: '计划时间', width: 170 },
    { align: 'center', field: 'actualTime', title: '完成时间', width: 170 },
    { field: 'remark', minWidth: 220, title: '备注' },
    { align: 'center', fixed: 'right', slots: { default: 'actions' }, title: '操作', width: 120 },
  ];
}

export function useItemGridColumns(): VxeTableGridOptions<MesMaintTaskApi.TaskItem>['columns'] {
  return [
    { align: 'center', field: 'sort', title: '序号', width: 60 },
    { field: 'itemName', minWidth: 180, title: '检查/清扫项目' },
    { field: 'method', title: '作业方法', width: 100 },
    { field: 'requirement', minWidth: 220, title: '合格标准/要求' },
    { align: 'center', field: 'result', slots: { default: 'result' }, title: '点检结果', width: 100 },
    { field: 'remark', minWidth: 150, title: '异常说明' },
  ];
}
