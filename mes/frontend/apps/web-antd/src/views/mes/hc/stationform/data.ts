import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesHcStationFormApi } from '#/api/mes/hc/stationform';

import { formatDateTime } from '@vben/utils';

import { getStationFormProcessOptions } from '#/api/mes/hc/stationform';

export const PROCESS_OPTIONS: MesHcStationFormApi.ProcessOption[] = [
  { label: '配料', value: 'FORMULA' },
  { label: '湿法', value: 'WET' },
  { label: '磨皮', value: 'ROUGH_GRINDING' },
  { label: '粘胶1', value: 'ADHESIVE' },
  { label: '粘胶2', value: 'ADHESIVE2' },
  { label: '分切', value: 'SLITTING' },
  { label: '压槽', value: 'PRESS_SLOT' },
  { label: '裁切', value: 'CUT_ROUND' },
  { label: '包装', value: 'PACKAGING' },
];

let processOptionsCache: MesHcStationFormApi.ProcessOption[] | undefined;

export async function loadProcessOptions() {
  if (processOptionsCache) return processOptionsCache;
  try {
    const options = await getStationFormProcessOptions();
    processOptionsCache = options?.length ? options : PROCESS_OPTIONS;
  } catch {
    processOptionsCache = PROCESS_OPTIONS;
  }
  return processOptionsCache;
}

export function resolveProcessName(
  processCode?: string,
  options = processOptionsCache || PROCESS_OPTIONS,
) {
  if (!processCode) return '';
  const legacyMap: Record<string, string> = {
    ADHESIVE1: '粘胶1',
    BACK_GLUE: '粘胶2',
    FINE_GRINDING: '精磨',
    TAPE: '粘胶1',
  };
  return options.find((item) => item.value === processCode)?.label || legacyMap[processCode] || processCode;
}

export const TRIGGER_TIMING_OPTIONS = [
  { label: '开工前', value: 'BEFORE_START' },
  { label: '首检', value: 'BEFORE_PROCESS_CHECK' },
  { label: '生产中', value: 'IN_PROCESS' },
  { label: '完工前', value: 'BEFORE_FINISH' },
];

export const TRIGGER_TIMING_NAME_MAP = Object.fromEntries(
  TRIGGER_TIMING_OPTIONS.map((item) => [item.value, item.label]),
);

export const VALUE_MODE_OPTIONS = [
  { label: '文本', value: 'TEXT' },
  { label: 'OK/NG', value: 'OK_NG' },
  { label: '双标签', value: 'DUAL_LABEL' },
  { label: '只读', value: 'READONLY' },
];

export const HEADER_LAYOUT_OPTIONS = [
  { label: '四列表头网格', value: 'GRID_4' },
  { label: '三列表头网格', value: 'GRID_3' },
  { label: '两列表头网格', value: 'GRID_2' },
  { label: '顶部摘要字段', value: 'TOP_FIELDS' },
];

export const PRESET_TEMPLATE_OPTIONS = [
  { label: '湿法开机点检模板', value: 'wet-startup-v1' },
  { label: '湿法清洁点检模板', value: 'wet-cleaning-v1' },
  { label: '湿法生产点检模板', value: 'wet-process-v1' },
  { label: '湿法凝固半成品模板', value: 'wet-solid-semi-v1' },
  { label: '湿法烘箱半成品模板', value: 'wet-oven-semi-v1' },
  { label: '粘胶1中间品记录模板', value: 'adhesive1-intermediate-v1' },
];

export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      fieldName: 'formCode',
      label: '表单编码',
      component: 'Input',
      componentProps: { placeholder: '请输入表单编码' },
    },
    {
      fieldName: 'formName',
      label: '表单名称',
      component: 'Input',
      componentProps: { placeholder: '请输入表单名称' },
    },
    {
      fieldName: 'processCode',
      label: '业务工序',
      component: 'ApiSelect',
      componentProps: {
        api: getStationFormProcessOptions,
        labelField: 'label',
        valueField: 'value',
        options: PROCESS_OPTIONS,
        allowClear: true,
        placeholder: '请选择业务工序',
      },
    },
    {
      fieldName: 'triggerTimingCode',
      label: '触发时机',
      component: 'Select',
      componentProps: {
        options: TRIGGER_TIMING_OPTIONS,
        allowClear: true,
        placeholder: '请选择触发时机',
      },
    },
    {
      fieldName: 'status',
      label: '状态',
      component: 'Select',
      componentProps: {
        options: [
          { label: '启用', value: 1 },
          { label: '停用', value: 0 },
        ],
        allowClear: true,
        placeholder: '请选择状态',
      },
    },
  ];
}

export function useGridColumns(): VxeTableGridOptions<MesHcStationFormApi.StationForm>['columns'] {
  return [
    { type: 'checkbox', width: 48, fixed: 'left' },
    { field: 'formCode', title: '表单编码', minWidth: 160 },
    { field: 'formName', title: '表单名称', minWidth: 220 },
    { field: 'processName', title: '业务工序', minWidth: 120 },
    { field: 'triggerTimingName', title: '触发时机', minWidth: 120 },
    {
      field: 'needConfirm',
      title: '需确认',
      minWidth: 100,
      formatter: ({ cellValue }) => (cellValue ? '是' : '否'),
    },
    { field: 'sortNo', title: '排序', minWidth: 80 },
    {
      field: 'status',
      title: '状态',
      minWidth: 100,
      formatter: ({ cellValue }) =>
        ({ 1: '启用', 0: '停用' } as Record<string, string>)[String(cellValue)] || '-',
    },
    { field: 'remark', title: '备注', minWidth: 220 },
    {
      field: 'createTime',
      title: '创建时间',
      minWidth: 180,
      formatter: ({ cellValue }) => (cellValue ? (formatDateTime(cellValue) as string) : '-'),
    },
    { title: '操作', width: 210, fixed: 'right', slots: { default: 'actions' } },
  ];
}
