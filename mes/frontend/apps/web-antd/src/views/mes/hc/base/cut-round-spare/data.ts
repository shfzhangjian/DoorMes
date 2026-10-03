import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesHcCutRoundSpareApi } from '#/api/mes/hc/cut-round-spare';

import { formatDateTime } from '@vben/utils';

import { getEquipmentSelectOptions } from '#/api/mes/hc/equipment';

import { normalizeMesDateTime } from '../shared/date-time';

export const CUT_ROUND_SPARE_TYPE_OPTIONS = [
  { label: '刀片', value: 'CUTTING_BLADE' },
  { label: '毛毡', value: 'CUTTING_FELT' },
];

export const CUT_ROUND_SPARE_STATUS_OPTIONS = [
  { label: '使用中', value: 'ACTIVE' },
  { label: '未使用（停用）', value: 'STOPPED' },
];

export const CUT_ROUND_SPARE_EVENT_OPTIONS = [
  { label: '基础配置', value: 'CONFIG' },
  { label: '历史纠错', value: 'ADJUST' },
  { label: '更换', value: 'REPLACE' },
  { label: '使用累计', value: 'USE' },
  { label: '研发样品手工消耗', value: 'RND_MANUAL_USE' },
];

const typeText = (value?: string) =>
  CUT_ROUND_SPARE_TYPE_OPTIONS.find((item) => item.value === value)?.label ||
  value ||
  '-';

const statusText = (value?: string) =>
  CUT_ROUND_SPARE_STATUS_OPTIONS.find((item) => item.value === value)?.label ||
  value ||
  '-';

const eventText = (value?: string) =>
  CUT_ROUND_SPARE_EVENT_OPTIONS.find((item) => item.value === value)?.label ||
  value ||
  '-';

const dateText = (value?: unknown) => {
  const normalized = normalizeMesDateTime(value);
  return normalized ? (formatDateTime(normalized) as string) : '-';
};

export function useStateGridFormSchema(): VbenFormSchema[] {
  return [
    {
      fieldName: 'equipmentCode',
      label: '设备编码',
      component: 'Input',
      componentProps: { placeholder: '请输入设备编码' },
    },
    {
      fieldName: 'equipmentName',
      label: '设备名称',
      component: 'Input',
      componentProps: { placeholder: '请输入设备名称' },
    },
    {
      fieldName: 'spareType',
      label: '备件类型',
      component: 'Select',
      componentProps: {
        options: CUT_ROUND_SPARE_TYPE_OPTIONS,
        allowClear: true,
        placeholder: '请选择备件类型',
      },
    },
    {
      fieldName: 'warningFlag',
      label: '预警',
      component: 'Select',
      componentProps: {
        options: [
          { label: '正常', value: 0 },
          { label: '预警', value: 1 },
        ],
        allowClear: true,
        placeholder: '请选择预警状态',
      },
    },
  ];
}

export function useRecordGridFormSchema(): VbenFormSchema[] {
  return [
    {
      fieldName: 'equipmentCode',
      label: '设备编码',
      component: 'Input',
      componentProps: { placeholder: '请输入设备编码' },
    },
    {
      fieldName: 'spareType',
      label: '备件类型',
      component: 'Select',
      componentProps: {
        options: CUT_ROUND_SPARE_TYPE_OPTIONS,
        allowClear: true,
        placeholder: '请选择备件类型',
      },
    },
    {
      fieldName: 'eventType',
      label: '事件类型',
      component: 'Select',
      componentProps: {
        options: CUT_ROUND_SPARE_EVENT_OPTIONS,
        allowClear: true,
        placeholder: '请选择事件类型',
      },
    },
    {
      fieldName: 'planNo',
      label: '计划号',
      component: 'Input',
      componentProps: { placeholder: '请输入计划号' },
    },
    {
      fieldName: 'operatorName',
      label: '操作人',
      component: 'Input',
      componentProps: { placeholder: '请输入操作人' },
    },
  ];
}

export function useFormSchema(): VbenFormSchema[] {
  return [
    {
      fieldName: 'correctHistory', label: '历史纠错（不换刀）', component: 'Switch',
      dependencies: { triggerFields: ['id', 'spareType'], show: (values) => !!values.id && values.spareType === 'CUTTING_BLADE' },
      help: '仅用于修正录入错误，须填写原因；实际更换刀片必须在报工页面选择库存并扣减。',
    },
    {
      fieldName: 'id',
      component: 'Input',
      dependencies: { triggerFields: [''], show: () => false },
    },
    {
      fieldName: 'operatorId',
      component: 'Input',
      dependencies: { triggerFields: [''], show: () => false },
    },
    {
      fieldName: 'equipmentId',
      dependencies: { triggerFields: ['id'], disabled: (values) => !!values.id },
      label: '设备',
      component: 'ApiSelect',
      componentProps: {
        api: getEquipmentSelectOptions,
        labelField: 'label',
        valueField: 'value',
        allowClear: false,
        placeholder: '请选择裁切设备',
      },
      rules: 'selectRequired',
    },
    {
      fieldName: 'spareType',
      dependencies: { triggerFields: ['id'], disabled: (values) => !!values.id },
      label: '备件类型',
      component: 'RadioGroup',
      componentProps: {
        options: CUT_ROUND_SPARE_TYPE_OPTIONS,
        buttonStyle: 'solid',
        optionType: 'button',
      },
      rules: 'required',
    },
    {
      fieldName: 'useCount',
      dependencies: { triggerFields: ['id', 'spareType', 'correctHistory'], disabled: (values) => !!values.id && values.spareType === 'CUTTING_BLADE' && !values.correctHistory },
      label: '累计片数',
      component: 'InputNumber',
      componentProps: { min: 0, precision: 0, class: 'w-full' },
    },
    {
      fieldName: 'limitCount',
      label: '片数上限',
      component: 'InputNumber',
      componentProps: { min: 0, precision: 0, class: 'w-full' },
    },
    {
      fieldName: 'limitDays',
      label: '天数上限',
      component: 'InputNumber',
      componentProps: { min: 0, precision: 0, class: 'w-full' },
      dependencies: {
        triggerFields: ['spareType'],
        show: (values) => values.spareType === 'CUTTING_FELT',
      },
    },
    {
      fieldName: 'lastReplaceTime',
      dependencies: { triggerFields: ['id', 'spareType', 'correctHistory'], disabled: (values) => !!values.id && values.spareType === 'CUTTING_BLADE' && !values.correctHistory },
      label: '上次更换时间',
      component: 'DatePicker',
      componentProps: {
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
        options: CUT_ROUND_SPARE_STATUS_OPTIONS,
        allowClear: false,
      },
      rules: 'selectRequired',
    },
    {
      fieldName: 'operatorName',
      label: '操作人',
      component: 'Input',
      componentProps: { placeholder: '默认当前登录人' },
    },
    {
      fieldName: 'replaceReason',
      label: '配置/纠错原因',
      component: 'Input',
      componentProps: { placeholder: '请输入说明' },
      formItemClass: 'col-span-2',
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

export function useStateGridColumns(): VxeTableGridOptions<MesHcCutRoundSpareApi.Spare>['columns'] {
  return [
    { field: 'equipmentCode', title: '设备编码', minWidth: 130, fixed: 'left' },
    { field: 'equipmentName', title: '设备名称', minWidth: 150, fixed: 'left' },
    {
      field: 'spareType',
      title: '备件类型',
      minWidth: 110,
      formatter: ({ cellValue }) => typeText(cellValue),
    },
    { field: 'useCount', title: '累计片数', minWidth: 100 },
    { field: 'limitCount', title: '片数上限', minWidth: 100 },
    {
      field: 'limitDays',
      title: '天数上限',
      minWidth: 100,
      formatter: ({ cellValue, row }) =>
        row.spareType === 'CUTTING_BLADE' ? '-' : cellValue,
    },
    {
      field: 'warningFlag',
      title: '预警',
      minWidth: 90,
      formatter: ({ cellValue }) => (Number(cellValue) === 1 ? '预警' : '正常'),
    },
    {
      field: 'status',
      title: '状态',
      minWidth: 90,
      formatter: ({ cellValue }) => statusText(cellValue),
    },
    {
      field: 'lastReplaceTime',
      title: '上次更换时间',
      minWidth: 180,
      formatter: ({ cellValue }) => dateText(cellValue),
    },
    { field: 'lastOperatorName', title: '最后操作人', minWidth: 120 },
    {
      title: '操作',
      width: 170,
      minWidth: 170,
      fixed: 'right',
      slots: { default: 'actions' },
    },
  ];
}

export function useRecordGridColumns(): VxeTableGridOptions<MesHcCutRoundSpareApi.Record>['columns'] {
  return [
    {
      field: 'eventTime',
      title: '事件时间',
      minWidth: 180,
      fixed: 'left',
      formatter: ({ cellValue }) => dateText(cellValue),
    },
    {
      field: 'eventType',
      title: '事件类型',
      minWidth: 110,
      formatter: ({ cellValue }) => eventText(cellValue),
    },
    { field: 'equipmentCode', title: '设备编码', minWidth: 130 },
    { field: 'equipmentName', title: '设备名称', minWidth: 150 },
    {
      field: 'spareType',
      title: '备件类型',
      minWidth: 110,
      formatter: ({ cellValue }) => typeText(cellValue),
    },
    { field: 'ledgerId', title: '领用台账ID', minWidth: 120 },
    { field: 'consumeId', title: '消耗明细ID', minWidth: 120 },
    { field: 'replaceQuantity', title: '更换数量', minWidth: 100 },
    { field: 'afterBatchNo', title: '领用批号', minWidth: 130 },
    { field: 'beforeUseCount', title: '前片数', minWidth: 90 },
    { field: 'afterUseCount', title: '后片数', minWidth: 90 },
    { field: 'changeUseCount', title: '变化片数', minWidth: 100 },
    { field: 'modelCode', title: '产品型号', minWidth: 130 },
    { field: 'productionBatchNo', title: '产品批次', minWidth: 160 },
    { field: 'cutSizeMm', title: '裁切尺寸(mm)', minWidth: 110 },
    { field: 'cutInputPcs', title: '研发投入(pcs)', minWidth: 120 },
    { field: 'cutOutputPcs', title: '研发产出(pcs)', minWidth: 120 },
    { field: 'feltUseDays', title: '毛毡使用天数', minWidth: 120 },
    { field: 'planNo', title: '计划号', minWidth: 150 },
    { field: 'operatorName', title: '操作人', minWidth: 120 },
    { field: 'replaceReason', title: '原因', minWidth: 180 },
    { field: 'remark', title: '审计说明', minWidth: 220 },
  ];
}
