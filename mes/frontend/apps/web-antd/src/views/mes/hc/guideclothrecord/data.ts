import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesHcGuideClothRecordApi } from '#/api/mes/hc/guideclothrecord';

import { formatDateTime } from '@vben/utils';

export const LINE_OPTIONS = [
  { label: '白垫线', value: 'WHITE' },
  { label: '黑垫线', value: 'BLACK' },
];

export const CURRENT_FLAG_OPTIONS = [
  { label: '当前', value: 0 },
  { label: '历史', value: 1 },
];

export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      fieldName: 'lineCode',
      label: '产线编号',
      component: 'Select',
      componentProps: {
        options: LINE_OPTIONS,
        allowClear: true,
        placeholder: '请选择产线编号',
      },
    },
    {
      fieldName: 'replacePlanNo',
      label: '计划号',
      component: 'Input',
      componentProps: { placeholder: '请输入计划号' },
    },
    {
      fieldName: 'petBatchNo',
      label: 'PET批号',
      component: 'Input',
      componentProps: { placeholder: '请输入PET批号' },
    },
    {
      fieldName: 'guideClothBatchNo',
      label: '导布批号',
      component: 'Input',
      componentProps: { placeholder: '请输入导布批号' },
    },
    {
      fieldName: 'currentFlag',
      label: '当前标记',
      component: 'Select',
      componentProps: {
        options: CURRENT_FLAG_OPTIONS,
        allowClear: true,
        placeholder: '请选择当前标记',
      },
    },
  ];
}

export function useFormSchema(): VbenFormSchema[] {
  return [
    {
      fieldName: 'id',
      component: 'Input',
      dependencies: { show: false },
    },
    {
      fieldName: 'lineCode',
      label: '产线编号',
      component: 'Select',
      componentProps: { options: LINE_OPTIONS, allowClear: false },
      rules: 'required',
    },
    {
      fieldName: 'lineName',
      label: '产线名',
      component: 'Input',
      componentProps: { placeholder: '请输入产线名' },
      rules: 'required',
    },
    {
      fieldName: 'replaceTime',
      label: '上次更换时间',
      component: 'DatePicker',
      componentProps: {
        showTime: true,
        valueFormat: 'YYYY-MM-DD HH:mm:ss',
      },
      rules: 'required',
    },
    {
      fieldName: 'replacePlanNo',
      label: '上次更换计划号',
      component: 'Input',
    },
    {
      fieldName: 'petBatchNo',
      label: '上次PET批号',
      component: 'Input',
    },
    {
      fieldName: 'guideClothBatchNo',
      label: '上次导布批号',
      component: 'Input',
    },
    {
      fieldName: 'petModel',
      label: '上次PET型号',
      component: 'Input',
    },
    {
      fieldName: 'replaceReason',
      label: '上次更换原因',
      component: 'Input',
    },
    {
      fieldName: 'useCount',
      label: '累计使用次数',
      component: 'InputNumber',
      componentProps: { min: 0, precision: 0 },
      rules: 'required',
    },
    {
      fieldName: 'currentFlag',
      label: '当前标记',
      component: 'Select',
      componentProps: { options: CURRENT_FLAG_OPTIONS, allowClear: false },
      rules: 'required',
    },
    {
      fieldName: 'remark',
      label: '备注',
      component: 'InputTextArea',
      componentProps: { rows: 3 },
      formItemClass: 'col-span-2',
    },
  ];
}

export function useGridColumns(): VxeTableGridOptions<MesHcGuideClothRecordApi.GuideClothRecord>['columns'] {
  return [
    { type: 'checkbox', width: 48, fixed: 'left' },
    { field: 'lineName', title: '产线名', minWidth: 100 },
    { field: 'lineCode', title: '产线编号', minWidth: 100 },
    {
      field: 'currentFlag',
      title: '当前标记',
      minWidth: 100,
      formatter: ({ cellValue }) => (Number(cellValue) === 0 ? '当前' : '历史'),
    },
    {
      field: 'replaceTime',
      title: '上次更换时间',
      minWidth: 180,
      formatter: ({ cellValue }) => (cellValue ? (formatDateTime(cellValue) as string) : '-'),
    },
    { field: 'replacePlanNo', title: '上次更换计划号', minWidth: 160 },
    { field: 'petBatchNo', title: '上次PET批号', minWidth: 140 },
    { field: 'guideClothBatchNo', title: '上次导布批号', minWidth: 140 },
    { field: 'petModel', title: '上次PET型号', minWidth: 140 },
    { field: 'replaceReason', title: '上次更换原因', minWidth: 160 },
    { field: 'useCount', title: '累计使用次数', minWidth: 120 },
    {
      field: 'createTime',
      title: '创建时间',
      minWidth: 180,
      formatter: ({ cellValue }) => (cellValue ? (formatDateTime(cellValue) as string) : '-'),
    },
    { title: '操作', width: 300, fixed: 'right', slots: { default: 'actions' } },
  ];
}
