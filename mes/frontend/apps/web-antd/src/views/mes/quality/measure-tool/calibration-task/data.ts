import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { QmsMeasureToolApi } from '#/api/mes/quality/measure-tool';

import {
  TASK_STATUS_OPTIONS,
  WARNING_STATUS_OPTIONS,
  optionLabel,
} from '../shared';

type Option = { label?: string; value?: number };

export function useGridFormSchema(categoryOptions: Option[] = []): VbenFormSchema[] {
  return [
    { component: 'Input', componentProps: { allowClear: true }, fieldName: 'taskNo', label: '任务号' },
    { component: 'Input', componentProps: { allowClear: true }, fieldName: 'toolCode', label: '量检具编码' },
    { component: 'Input', componentProps: { allowClear: true }, fieldName: 'toolName', label: '量检具名称' },
    {
      component: 'Select',
      componentProps: { allowClear: true, options: categoryOptions, showSearch: true },
      fieldName: 'categoryId',
      label: '分类',
    },
    { component: 'Input', componentProps: { allowClear: true }, fieldName: 'usingDepartment', label: '使用部门' },
    {
      component: 'Select',
      componentProps: { allowClear: true, options: WARNING_STATUS_OPTIONS },
      fieldName: 'warningStatus',
      label: '预警状态',
    },
    {
      component: 'Select',
      componentProps: { allowClear: true, options: TASK_STATUS_OPTIONS },
      fieldName: 'taskStatus',
      label: '任务状态',
    },
    {
      component: 'RangePicker',
      componentProps: { allowClear: true, format: 'YYYY-MM-DD', valueFormat: 'YYYY-MM-DD' },
      fieldName: 'dueDate',
      label: '应校准日期',
    },
  ];
}

export function useGridColumns(): VxeTableGridOptions<QmsMeasureToolApi.CalibrationTask>['columns'] {
  return [
    { field: 'taskNo', fixed: 'left', minWidth: 170, title: '任务号' },
    { field: 'toolCode', fixed: 'left', minWidth: 150, title: '量检具编码' },
    { field: 'toolName', fixed: 'left', minWidth: 180, title: '量检具名称' },
    { field: 'categoryName', minWidth: 130, title: '分类' },
    { field: 'usingDepartment', minWidth: 130, title: '使用部门' },
    { field: 'keeperName', minWidth: 100, title: '保管人' },
    { align: 'center', field: 'dueDate', title: '应校准日期', width: 120 },
    { align: 'center', field: 'warningDays', title: '预警天数', width: 90 },
    {
      align: 'center',
      field: 'warningStatus',
      formatter: ({ cellValue }) => optionLabel(WARNING_STATUS_OPTIONS, cellValue),
      title: '预警状态',
      width: 110,
    },
    {
      align: 'center',
      field: 'taskStatus',
      formatter: ({ cellValue }) => optionLabel(TASK_STATUS_OPTIONS, cellValue),
      title: '任务状态',
      width: 110,
    },
    { align: 'center', field: 'generatedTime', title: '生成时间', width: 170 },
    { align: 'center', field: 'completedTime', title: '完成时间', width: 170 },
    { field: 'handlerName', minWidth: 100, title: '处理人' },
    { fixed: 'right', slots: { default: 'actions' }, title: '操作', width: 180 },
  ];
}
