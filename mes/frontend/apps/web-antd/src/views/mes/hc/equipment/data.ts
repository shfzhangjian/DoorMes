import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesHcEquipmentApi } from '#/api/mes/hc/equipment';

import { getWorkCenterSelectOptions } from '#/api/mes/hc/workcenter';

const WORK_STATUS_OPTIONS = [
  { label: '待机', value: 'IDLE' },
  { label: '生产中', value: 'PRODUCING' },
  { label: '检修', value: 'MAINTENANCE' },
  { label: '故障', value: 'FAULT' },
];

export const applicablePadTypeOptions = [
  { label: '白垫', value: 'WHITE_PAD' },
  { label: '黑垫', value: 'BLACK_PAD' },
  { label: '通用', value: 'COMMON' },
];

const applicablePadTypeMap = Object.fromEntries(applicablePadTypeOptions.map((item) => [item.value, item.label]));

export function useFormSchema(): VbenFormSchema[] {
  return [
    { fieldName: 'id', component: 'Input', dependencies: { triggerFields: [''], show: () => false } },
    { fieldName: 'workCenterId', component: 'Input', dependencies: { triggerFields: [''], show: () => false } },
    { fieldName: 'applicablePadTypeName', component: 'Input', dependencies: { triggerFields: [''], show: () => false } },
    { fieldName: 'equipmentCode', label: '设备编码', component: 'Input', rules: 'required', componentProps: { placeholder: '请输入设备编码' }, formItemClass: 'col-span-1' },
    { fieldName: 'equipmentName', label: '设备名称', component: 'Input', rules: 'required', componentProps: { placeholder: '请输入设备名称' }, formItemClass: 'col-span-1' },
    { fieldName: 'equipmentType', label: '设备类型', component: 'Input', componentProps: { placeholder: '请输入设备类型' }, formItemClass: 'col-span-1' },
    {
      fieldName: 'applicablePadType',
      label: '适用垫型',
      component: 'Select',
      componentProps: { options: applicablePadTypeOptions, allowClear: true, placeholder: '请选择适用垫型' },
      formItemClass: 'col-span-1',
    },
    { fieldName: 'assetNo', label: '资产编号', component: 'Input', componentProps: { placeholder: '请输入资产编号' }, formItemClass: 'col-span-1' },
    {
      fieldName: 'enableQcChecklist',
      label: '是否启用点检',
      component: 'RadioGroup',
      componentProps: {
        options: [
          { label: '是', value: true },
          { label: '否', value: false },
        ],
        buttonStyle: 'solid',
        optionType: 'button',
      },
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'enableCleanChecklist',
      label: '是否启用清洁点检',
      component: 'RadioGroup',
      componentProps: {
        options: [
          { label: '是', value: true },
          { label: '否', value: false },
        ],
        buttonStyle: 'solid',
        optionType: 'button',
      },
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'status',
      label: '状态',
      component: 'RadioGroup',
      rules: 'required',
      componentProps: {
        options: [
          { label: '启用', value: 0 },
          { label: '停用', value: 1 },
        ],
        buttonStyle: 'solid',
        optionType: 'button',
      },
      defaultValue: 0,
      formItemClass: 'col-span-1',
    },
    { fieldName: 'remark', label: '备注', component: 'Textarea', componentProps: { rows: 3, placeholder: '请输入备注' }, formItemClass: 'col-span-1' },
  ];
}

export function useGridFormSchema(): VbenFormSchema[] {
  return [
    { fieldName: 'equipmentCode', label: '设备编码', component: 'Input', componentProps: { placeholder: '请输入设备编码' } },
    { fieldName: 'equipmentName', label: '设备名称', component: 'Input', componentProps: { placeholder: '请输入设备名称' } },
    {
      fieldName: 'workCenterId',
      label: '所属工作中心',
      component: 'ApiSelect',
      componentProps: {
        api: getWorkCenterSelectOptions,
        labelField: 'label',
        valueField: 'value',
        placeholder: '请选择所属工作中心',
        allowClear: true,
      },
    },
    {
      fieldName: 'workStatus',
      label: '运行状态',
      component: 'Select',
      componentProps: {
        options: WORK_STATUS_OPTIONS,
        allowClear: true,
        placeholder: '请选择运行状态',
      },
    },
    {
      fieldName: 'applicablePadType',
      label: '适用垫型',
      component: 'Select',
      componentProps: {
        options: applicablePadTypeOptions,
        allowClear: true,
        placeholder: '请选择适用垫型',
      },
    },
    {
      fieldName: 'status',
      label: '状态',
      component: 'Select',
      componentProps: {
        options: [
          { label: '启用', value: 0 },
          { label: '停用', value: 1 },
        ],
        allowClear: true,
        placeholder: '请选择状态',
      },
    },
  ];
}

export function useGridColumns(): VxeTableGridOptions<MesHcEquipmentApi.Equipment>['columns'] {
  return [
    { type: 'checkbox', width: 48, fixed: 'left' },
    { field: 'equipmentCode', title: '设备编码', minWidth: 140 },
    { field: 'equipmentName', title: '设备名称', minWidth: 160 },
    {
      field: 'workStatus',
      title: '运行状态',
      minWidth: 120,
      formatter: ({ cellValue }) =>
        ({ IDLE: '待机', PRODUCING: '生产中', MAINTENANCE: '检修', FAULT: '故障' } as Record<string, string>)[
          String(cellValue)
        ] || cellValue || '-',
    },
    {
      field: 'status',
      title: '状态',
      minWidth: 120,
      formatter: ({ cellValue }) =>
        ({ 0: '启用', 1: '停用' } as Record<string, string>)[String(cellValue)] || cellValue || '-',
    },
    { field: 'workCenterName', title: '所属工作中心名称', minWidth: 160 },
    { field: 'workCenterCode', title: '所属工作中心编码', minWidth: 140 },
    { field: 'equipmentType', title: '设备类型', minWidth: 140 },
    {
      field: 'applicablePadType',
      title: '适用垫型',
      minWidth: 120,
      formatter: ({ cellValue, row }) => row.applicablePadTypeName || applicablePadTypeMap[String(cellValue)] || cellValue || '-',
    },
    { field: 'currentPlanNo', title: '当前计划号', minWidth: 160 },
    { field: 'currentOperationCode', title: '当前工序编号', minWidth: 140 },
    { field: 'currentOperationName', title: '当前工序名称', minWidth: 160 },
    { field: 'currentStartTime', title: '开工时间', minWidth: 180 },
    { field: 'currentEndTime', title: '完工时间', minWidth: 180 },
    { field: 'currentOperatorName', title: '操作人', minWidth: 120 },
    { field: 'currentRecordTime', title: '回写时间', minWidth: 180 },
    { field: 'remark', title: '备注', minWidth: 220 },
    { title: '操作', width: 220, fixed: 'right', slots: { default: 'actions' } },
  ];
}
