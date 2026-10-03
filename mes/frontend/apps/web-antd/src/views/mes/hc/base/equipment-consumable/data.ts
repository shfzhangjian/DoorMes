import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesHcEquipmentConsumableApi } from '#/api/mes/hc/equipment-consumable';

import { formatDateTime } from '@vben/utils';

import { getEquipmentSelectOptions } from '#/api/mes/hc/equipment';

import { normalizeMesDateTime } from '../shared/date-time';

export const CONSUMABLE_TYPE_OPTIONS = [
  { label: '砂纸', value: 'SANDPAPER' },
  { label: '导布', value: 'GUIDE_CLOTH' },
];

export const CONSUMABLE_STATUS_OPTIONS = [
  { label: '使用中', value: 'IN_USE' },
  { label: '使用中', value: 'ACTIVE' },
  { label: '停用', value: 'STOPPED' },
];

export const EVENT_TYPE_OPTIONS = [
  { label: '使用累计', value: 'USE' },
  { label: '更换', value: 'REPLACE' },
  { label: '调整', value: 'ADJUST' },
  { label: '研发样品手工消耗', value: 'RND_MANUAL_USE' },
];

const typeText = (value?: string) =>
  CONSUMABLE_TYPE_OPTIONS.find((item) => item.value === value)?.label || value || '-';

const statusText = (value?: string) =>
  CONSUMABLE_STATUS_OPTIONS.find((item) => item.value === value)?.label || value || '-';

const eventText = (value?: string) =>
  EVENT_TYPE_OPTIONS.find((item) => item.value === value)?.label || value || '-';

const recordSourceText = (value?: string) =>
  value === 'RND_MANUAL' ? '研发手工登记' : value ? value : '量产报工';

const dateText = (value?: unknown) => {
  const normalized = normalizeMesDateTime(value);
  return normalized ? (formatDateTime(normalized) as string) : '-';
};

export function useStateGridFormSchema(): VbenFormSchema[] {
  return [
    { fieldName: 'equipmentCode', label: '设备编码', component: 'Input', componentProps: { placeholder: '请输入设备编码' } },
    { fieldName: 'equipmentName', label: '设备名称', component: 'Input', componentProps: { placeholder: '请输入设备名称' } },
    {
      fieldName: 'consumableType',
      label: '耗材类型',
      component: 'Select',
      componentProps: { options: CONSUMABLE_TYPE_OPTIONS, allowClear: true, placeholder: '请选择耗材类型' },
    },
    { fieldName: 'batchNo', label: '批号', component: 'Input', componentProps: { placeholder: '请输入批号' } },
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
    {
      fieldName: 'status',
      label: '状态',
      component: 'Select',
      componentProps: { options: CONSUMABLE_STATUS_OPTIONS, allowClear: true, placeholder: '请选择状态' },
    },
  ];
}

export function useEventGridFormSchema(): VbenFormSchema[] {
  return [
    { fieldName: 'equipmentCode', label: '设备编码', component: 'Input', componentProps: { placeholder: '请输入设备编码' } },
    {
      fieldName: 'consumableType',
      label: '耗材类型',
      component: 'Select',
      componentProps: { options: CONSUMABLE_TYPE_OPTIONS, allowClear: true, placeholder: '请选择耗材类型' },
    },
    {
      fieldName: 'eventType',
      label: '事件类型',
      component: 'Select',
      componentProps: { options: EVENT_TYPE_OPTIONS, allowClear: true, placeholder: '请选择事件类型' },
    },
    { fieldName: 'batchNo', label: '批号', component: 'Input', componentProps: { placeholder: '输入更换前/后批号' } },
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
        placeholder: '请选择设备',
      },
      rules: 'selectRequired',
    },
    {
      fieldName: 'consumableType',
      label: '耗材类型',
      component: 'RadioGroup',
      componentProps: { options: CONSUMABLE_TYPE_OPTIONS, buttonStyle: 'solid', optionType: 'button' },
      rules: 'required',
    },
    {
      fieldName: 'eventType',
      label: '操作类型',
      component: 'RadioGroup',
      componentProps: { options: EVENT_TYPE_OPTIONS, buttonStyle: 'solid', optionType: 'button' },
      rules: 'required',
    },
    { fieldName: 'batchNo', label: '当前批号', component: 'Input', componentProps: { placeholder: '请输入当前批号' }, rules: 'required' },
    { fieldName: 'processCode', label: '工序编号', component: 'Input', componentProps: { placeholder: '默认 ROUGH_GRINDING' } },
    { fieldName: 'processName', label: '工序名称', component: 'Input', componentProps: { placeholder: '默认 磨皮' } },
    { fieldName: 'usedLength', label: '累计米数', component: 'InputNumber', componentProps: { min: 0, precision: 3, class: 'w-full' } },
    { fieldName: 'useCount', label: '累计次数', component: 'InputNumber', componentProps: { min: 0, precision: 0, class: 'w-full' } },
    { fieldName: 'limitLength', label: '米数上限', component: 'InputNumber', componentProps: { min: 0, precision: 3, class: 'w-full' } },
    { fieldName: 'limitCount', label: '次数上限', component: 'InputNumber', componentProps: { min: 0, precision: 0, class: 'w-full' } },
    {
      fieldName: 'status',
      label: '状态',
      component: 'Select',
      componentProps: { options: CONSUMABLE_STATUS_OPTIONS, allowClear: false },
      rules: 'selectRequired',
    },
    {
      fieldName: 'eventTime',
      label: '操作时间',
      component: 'DatePicker',
      componentProps: { showTime: true, valueFormat: 'YYYY-MM-DD HH:mm:ss', class: 'w-full' },
      rules: 'required',
    },
    { fieldName: 'lastReplacePlanNo', label: '关联计划号', component: 'Input', componentProps: { placeholder: '可选' } },
    { fieldName: 'operatorName', label: '操作人', component: 'Input', componentProps: { placeholder: '默认当前登录人' } },
    { fieldName: 'replaceReason', label: '更换/调整原因', component: 'Input', componentProps: { placeholder: '请输入原因' }, formItemClass: 'col-span-2' },
    { fieldName: 'remark', label: '备注', component: 'InputTextArea', componentProps: { rows: 3 }, formItemClass: 'col-span-2' },
  ];
}

export function useStateGridColumns(): VxeTableGridOptions<MesHcEquipmentConsumableApi.State>['columns'] {
  return [
    { field: 'equipmentCode', title: '设备编码', minWidth: 130, fixed: 'left' },
    { field: 'equipmentName', title: '设备名称', minWidth: 150, fixed: 'left' },
    { field: 'processName', title: '工序', minWidth: 110 },
    { field: 'consumableType', title: '耗材类型', minWidth: 100, formatter: ({ cellValue }) => typeText(cellValue) },
    { field: 'batchNo', title: '当前批号', minWidth: 160 },
    { field: 'usedLength', title: '累计米数(m)', minWidth: 120 },
    { field: 'useCount', title: '累计次数', minWidth: 100 },
    { field: 'limitLength', title: '米数上限', minWidth: 110 },
    { field: 'limitCount', title: '次数上限', minWidth: 110 },
    {
      field: 'warningFlag',
      title: '预警',
      minWidth: 90,
      formatter: ({ cellValue }) => (Number(cellValue) === 1 ? '预警' : '正常'),
    },
    { field: 'status', title: '状态', minWidth: 90, formatter: ({ cellValue }) => statusText(cellValue) },
    { field: 'lastReplaceTime', title: '上次更换时间', minWidth: 180, formatter: ({ cellValue }) => dateText(cellValue) },
    { field: 'lastReplacePlanNo', title: '上次计划号', minWidth: 150 },
    { field: 'lastOperatorName', title: '最后操作人', minWidth: 120 },
    { field: 'lastEventTime', title: '最后操作时间', minWidth: 180, formatter: ({ cellValue }) => dateText(cellValue) },
    { title: '操作', width: 190, minWidth: 190, fixed: 'right', slots: { default: 'actions' } },
  ];
}

export function useEventGridColumns(): VxeTableGridOptions<MesHcEquipmentConsumableApi.Event>['columns'] {
  return [
    { field: 'eventTime', title: '事件时间', minWidth: 180, fixed: 'left', formatter: ({ cellValue }) => dateText(cellValue) },
    { field: 'eventType', title: '事件类型', minWidth: 100, formatter: ({ cellValue }) => eventText(cellValue) },
    { field: 'recordSource', title: '数据来源', minWidth: 120, formatter: ({ cellValue }) => recordSourceText(cellValue) },
    { field: 'equipmentCode', title: '设备编码', minWidth: 130 },
    { field: 'equipmentName', title: '设备名称', minWidth: 150 },
    { field: 'processName', title: '工序', minWidth: 110 },
    { field: 'consumableType', title: '耗材类型', minWidth: 100, formatter: ({ cellValue }) => typeText(cellValue) },
    { field: 'beforeBatchNo', title: '更换前批号', minWidth: 150 },
    { field: 'afterBatchNo', title: '更换后批号', minWidth: 150 },
    { field: 'beforeUsedLength', title: '前累计米数', minWidth: 120 },
    { field: 'afterUsedLength', title: '后累计米数', minWidth: 120 },
    { field: 'changeLength', title: '变化米数', minWidth: 110 },
    { field: 'beforeUseCount', title: '前次数', minWidth: 90 },
    { field: 'afterUseCount', title: '后次数', minWidth: 90 },
    { field: 'changeUseCount', title: '变化次数', minWidth: 100 },
    { field: 'productModelCode', title: '产品型号', minWidth: 130 },
    { field: 'productMaterialCode', title: '产品料号', minWidth: 130 },
    { field: 'productBatchNo', title: '产品批次', minWidth: 140 },
    { field: 'petModel', title: 'PET型号', minWidth: 120 },
    { field: 'petBatchNo', title: 'PET批号', minWidth: 140 },
    { field: 'wetInputKg', title: '湿法投入(kg)', minWidth: 120 },
    { field: 'wetOutputMeter', title: '湿法产出(m)', minWidth: 120 },
    { field: 'recordGroupNo', title: '登记分组号', minWidth: 170 },
    { field: 'planNo', title: '计划号', minWidth: 150 },
    { field: 'operatorName', title: '操作人', minWidth: 120 },
    { field: 'replaceReason', title: '原因', minWidth: 180 },
    { field: 'remark', title: '备注', minWidth: 180 },
  ];
}
