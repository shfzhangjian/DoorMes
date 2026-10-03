import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesHcPressSlotSpareApi } from '#/api/mes/hc/press-slot-spare';

import { formatDateTime } from '@vben/utils';

import { getEquipmentSelectOptions } from '#/api/mes/hc/equipment';

import { normalizeMesDateTime } from '../shared/date-time';

export const PRESS_SLOT_SPARE_TYPE_OPTIONS = [
  { label: '压槽辊', value: 'PRESS_ROLLER' },
  { label: '轴承', value: 'BEARING' },
];

export const PRESS_SLOT_SPARE_STATUS_OPTIONS = [
  { label: '使用中', value: 'ACTIVE' },
  { label: '未使用（停用）', value: 'STOPPED' },
];

export const PRESS_SLOT_SPARE_EVENT_OPTIONS = [
  { label: '基础配置', value: 'CONFIG' },
  { label: '更换', value: 'REPLACE' },
  { label: '清洗复位', value: 'CLEAN_RESET' },
  { label: '使用累计', value: 'USE' },
  { label: '研发样品手工消耗', value: 'RND_MANUAL_USE' },
];

const typeText = (value?: string) =>
  PRESS_SLOT_SPARE_TYPE_OPTIONS.find((item) => item.value === value)?.label || value || '-';

const statusText = (value?: string) =>
  PRESS_SLOT_SPARE_STATUS_OPTIONS.find((item) => item.value === value)?.label || value || '-';

const eventText = (value?: string) =>
  PRESS_SLOT_SPARE_EVENT_OPTIONS.find((item) => item.value === value)?.label || value || '-';

const dateText = (value?: unknown) => {
  const normalized = normalizeMesDateTime(value);
  return normalized ? (formatDateTime(normalized) as string) : '-';
};

export function useStateGridFormSchema(): VbenFormSchema[] {
  return [
    { fieldName: 'equipmentCode', label: '设备编码', component: 'Input', componentProps: { placeholder: '请输入设备编码' } },
    { fieldName: 'equipmentName', label: '设备名称', component: 'Input', componentProps: { placeholder: '请输入设备名称' } },
    {
      fieldName: 'spareType',
      label: '备件类型',
      component: 'Select',
      componentProps: { options: PRESS_SLOT_SPARE_TYPE_OPTIONS, allowClear: true, placeholder: '请选择备件类型' },
    },
    { fieldName: 'materialCode', label: '料号', component: 'Input', componentProps: { placeholder: '请输入料号' } },
    { fieldName: 'batchNo', label: '批号/编码', component: 'Input', componentProps: { placeholder: '请输入批号或编码' } },
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
    { fieldName: 'equipmentCode', label: '设备编码', component: 'Input', componentProps: { placeholder: '请输入设备编码' } },
    {
      fieldName: 'spareType',
      label: '备件类型',
      component: 'Select',
      componentProps: { options: PRESS_SLOT_SPARE_TYPE_OPTIONS, allowClear: true, placeholder: '请选择备件类型' },
    },
    {
      fieldName: 'eventType',
      label: '事件类型',
      component: 'Select',
      componentProps: { options: PRESS_SLOT_SPARE_EVENT_OPTIONS, allowClear: true, placeholder: '请选择事件类型' },
    },
    { fieldName: 'batchNo', label: '批号/编码', component: 'Input', componentProps: { placeholder: '输入更换前/后批号' } },
    { fieldName: 'planNo', label: '计划号', component: 'Input', componentProps: { placeholder: '请输入计划号' } },
    { fieldName: 'operatorName', label: '操作人', component: 'Input', componentProps: { placeholder: '请输入操作人' } },
  ];
}

export function useFormSchema(): VbenFormSchema[] {
  return [
    { fieldName: 'id', component: 'Input', dependencies: { triggerFields: [''], show: () => false } },
    { fieldName: 'operatorId', component: 'Input', dependencies: { triggerFields: [''], show: () => false } },
    {
      fieldName: 'equipmentId',
      label: '设备',
      component: 'ApiSelect',
      componentProps: {
        api: getEquipmentSelectOptions,
        labelField: 'label',
        valueField: 'value',
        allowClear: false,
        placeholder: '请选择压槽设备',
      },
      rules: 'selectRequired',
    },
    {
      fieldName: 'spareType',
      label: '备件类型',
      component: 'RadioGroup',
      componentProps: { options: PRESS_SLOT_SPARE_TYPE_OPTIONS, buttonStyle: 'solid', optionType: 'button' },
      rules: 'required',
    },
    { fieldName: 'materialCode', label: '料号', component: 'Input', componentProps: { placeholder: '压槽辊可为空，轴承必填' } },
    { fieldName: 'materialName', label: '名称', component: 'Input', componentProps: { placeholder: '请输入备件名称' } },
    { fieldName: 'batchNo', label: '批号/编码', component: 'Input', componentProps: { placeholder: '压槽辊编码或轴承批号' }, rules: 'required' },
    { fieldName: 'onlineQuantity', label: '在线数量', component: 'InputNumber', componentProps: { min: 0, precision: 0, class: 'w-full' } },
    { fieldName: 'availableQuantity', label: '可用量', component: 'InputNumber', componentProps: { min: 0, precision: 0, class: 'w-full' } },
    { fieldName: 'useCount', label: '累计片数', component: 'InputNumber', componentProps: { min: 0, precision: 0, class: 'w-full' } },
    { fieldName: 'limitCount', label: '片数上限', component: 'InputNumber', componentProps: { min: 0, precision: 0, class: 'w-full' } },
    { fieldName: 'limitDays', label: '天数上限', component: 'InputNumber', componentProps: { min: 0, precision: 0, class: 'w-full' } },
    {
      fieldName: 'status',
      label: '状态',
      component: 'Select',
      componentProps: { options: PRESS_SLOT_SPARE_STATUS_OPTIONS, allowClear: false },
      rules: 'selectRequired',
    },
    { fieldName: 'operatorName', label: '操作人', component: 'Input', componentProps: { placeholder: '默认当前登录人' } },
    { fieldName: 'replaceReason', label: '配置/更换说明', component: 'Input', componentProps: { placeholder: '请输入说明' }, formItemClass: 'col-span-2' },
    { fieldName: 'lastCleanRemark', label: '清洗备注', component: 'Input', componentProps: { placeholder: '压槽辊清洗备注' }, formItemClass: 'col-span-2' },
    { fieldName: 'remark', label: '备注', component: 'InputTextArea', componentProps: { rows: 3 }, formItemClass: 'col-span-2' },
  ];
}

export function useStateGridColumns(): VxeTableGridOptions<MesHcPressSlotSpareApi.Spare>['columns'] {
  return [
    { field: 'equipmentCode', title: '设备编码', minWidth: 130, fixed: 'left' },
    { field: 'equipmentName', title: '设备名称', minWidth: 150, fixed: 'left' },
    { field: 'spareType', title: '备件类型', minWidth: 110, formatter: ({ cellValue }) => typeText(cellValue) },
    { field: 'materialCode', title: '料号', minWidth: 130 },
    { field: 'batchNo', title: '批号/编码', minWidth: 160 },
    { field: 'availableQuantity', title: '可用量', minWidth: 90 },
    { field: 'useCount', title: '累计片数', minWidth: 100 },
    { field: 'limitCount', title: '片数上限', minWidth: 100 },
    { field: 'limitDays', title: '天数上限', minWidth: 100 },
    { field: 'warningFlag', title: '预警', minWidth: 90, formatter: ({ cellValue }) => (Number(cellValue) === 1 ? '预警' : '正常') },
    { field: 'status', title: '状态', minWidth: 90, formatter: ({ cellValue }) => statusText(cellValue) },
    { field: 'lastReplaceTime', title: '上次更换时间', minWidth: 180, formatter: ({ cellValue }) => dateText(cellValue) },
    { field: 'lastOperatorName', title: '最后操作人', minWidth: 120 },
    { title: '操作', width: 250, minWidth: 250, fixed: 'right', slots: { default: 'actions' } },
  ];
}

export function useRecordGridColumns(): VxeTableGridOptions<MesHcPressSlotSpareApi.Record>['columns'] {
  return [
    { field: 'eventTime', title: '事件时间', minWidth: 180, fixed: 'left', formatter: ({ cellValue }) => dateText(cellValue) },
    { field: 'eventType', title: '事件类型', minWidth: 110, formatter: ({ cellValue }) => eventText(cellValue) },
    { field: 'equipmentCode', title: '设备编码', minWidth: 130 },
    { field: 'equipmentName', title: '设备名称', minWidth: 150 },
    { field: 'spareType', title: '备件类型', minWidth: 110, formatter: ({ cellValue }) => typeText(cellValue) },
    { field: 'beforeBatchNo', title: '前批号/编码', minWidth: 150 },
    { field: 'afterBatchNo', title: '后批号/编码', minWidth: 150 },
    { field: 'beforeUseCount', title: '前片数', minWidth: 90 },
    { field: 'afterUseCount', title: '后片数', minWidth: 90 },
    { field: 'changeUseCount', title: '变化片数', minWidth: 100 },
    { field: 'beforeAvailableQuantity', title: '前可用量', minWidth: 100 },
    { field: 'afterAvailableQuantity', title: '后可用量', minWidth: 100 },
    { field: 'changeQuantity', title: '变化量', minWidth: 90 },
    { field: 'planNo', title: '计划号', minWidth: 150 },
    { field: 'productionBatchNo', title: '研发生产批号', minWidth: 160 },
    { field: 'pressSlotInputPcs', title: '压槽投入片数', minWidth: 120 },
    { field: 'pressSlotOutputPcs', title: '压槽产出片数', minWidth: 120 },
    { field: 'operatorName', title: '操作人', minWidth: 120 },
    { field: 'recordSource', title: '来源', minWidth: 110 },
    { field: 'replaceReason', title: '原因', minWidth: 180 },
  ];
}
