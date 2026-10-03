import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesHcWorkCenterApi } from '#/api/mes/hc/workcenter';

import { getWorkCenterLineLabel } from '#/api/mes/hc/workcenter';

export const LINE_CODE_OPTIONS = [
  { label: '白垫线', value: 'WHITE' },
  { label: '黑垫线', value: 'BLACK' },
];

export function useFormSchema(): VbenFormSchema[] {
  return [
    { fieldName: 'id', component: 'Input', dependencies: { triggerFields: [''], show: () => false } },
    { fieldName: 'processId', component: 'Input', dependencies: { triggerFields: [''], show: () => false } },
    { fieldName: 'processName', component: 'Input', dependencies: { triggerFields: [''], show: () => false } },
    { fieldName: 'processStage', component: 'Input', dependencies: { triggerFields: [''], show: () => false } },
    { fieldName: 'wcCode', label: '工作中心编码', component: 'Input', rules: 'required', componentProps: { placeholder: '请输入工作中心编码' }, formItemClass: 'col-span-1' },
    { fieldName: 'wcName', label: '工作中心名称', component: 'Input', rules: 'required', componentProps: { placeholder: '请输入工作中心名称' }, formItemClass: 'col-span-1' },
    {
      fieldName: 'lineCode',
      label: '业务产线编码',
      component: 'Select',
      componentProps: {
        options: LINE_CODE_OPTIONS,
        placeholder: '请选择业务产线',
        allowClear: true,
      },
      formItemClass: 'col-span-1',
    },
    { fieldName: 'lineName', label: '产线名称', component: 'Input', componentProps: { placeholder: '如：白垫线' }, formItemClass: 'col-span-1' },
    {
      fieldName: 'processCode',
      label: '工序',
      component: 'Input',
      slot: 'processCode',
      rules: 'required',
      formItemClass: 'col-span-1',
    },
    { fieldName: 'lineShortCode', label: '产线短码', component: 'Input', componentProps: { placeholder: '如：W/B' }, formItemClass: 'col-span-1' },
    { fieldName: 'batchLineCode', label: '批次产线码', component: 'Input', componentProps: { placeholder: '如：A/B' }, formItemClass: 'col-span-1' },
    { fieldName: 'lineSort', label: '产线排序', component: 'InputNumber', componentProps: { class: 'w-full', precision: 0 }, formItemClass: 'col-span-1' },
    { fieldName: 'terminalIps', label: '工位 IP', component: 'Textarea', componentProps: { rows: 2, placeholder: '可配置多个 IP，支持逗号、分号或换行分隔' }, formItemClass: 'col-span-2' },
    { fieldName: 'capacityPerHour', label: '标准小时产能', component: 'InputNumber', componentProps: { class: 'w-full', precision: 2 }, formItemClass: 'col-span-1' },
    { fieldName: 'capacityUom', label: '产能单位', component: 'Input', componentProps: { placeholder: '请输入产能单位' }, formItemClass: 'col-span-1' },
    { fieldName: 'defaultShiftMode', label: '默认班制', component: 'Input', componentProps: { placeholder: '请输入默认班制' }, formItemClass: 'col-span-1' },
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
    { fieldName: 'wcCode', label: '工作中心编码', component: 'Input', componentProps: { placeholder: '请输入工作中心编码' } },
    { fieldName: 'wcName', label: '工作中心名称', component: 'Input', componentProps: { placeholder: '请输入工作中心名称' } },
    { fieldName: 'processName', label: '工序', component: 'Input', componentProps: { placeholder: '请输入工序名称' } },
    {
      fieldName: 'lineCode',
      label: '业务产线',
      component: 'Select',
      componentProps: {
        options: LINE_CODE_OPTIONS,
        allowClear: true,
        placeholder: '请选择所属线路',
      },
    },
    { fieldName: 'lineName', label: '产线名称', component: 'Input', componentProps: { placeholder: '请输入产线名称' } },
    { fieldName: 'terminalIps', label: '工位 IP', component: 'Input', componentProps: { placeholder: '请输入工位 IP' } },
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

export function useGridColumns(): VxeTableGridOptions<MesHcWorkCenterApi.WorkCenter>['columns'] {
  return [
    { type: 'checkbox', width: 48, fixed: 'left' },
    { field: 'lineName', title: '所属线路', minWidth: 140, treeNode: true },
    { field: 'lineCode', title: '业务产线编码', minWidth: 130 },
    { field: 'lineShortCode', title: '产线短码', minWidth: 100 },
    { field: 'batchLineCode', title: '批次产线码', minWidth: 120 },
    { field: 'wcCode', title: '工作中心编码', minWidth: 140 },
    { field: 'wcName', title: '工作中心名称', minWidth: 160 },
    { field: 'terminalIps', title: '工位 IP', minWidth: 180 },
    {
      field: 'processName',
      title: '工序',
      minWidth: 140,
      formatter: ({ cellValue, row }) => cellValue || row.processStage || '-',
    },
    { field: 'capacityPerHour', title: '标准小时产能', minWidth: 140 },
    { field: 'capacityUom', title: '产能单位', minWidth: 120 },
    {
      field: 'status',
      title: '状态',
      minWidth: 120,
      formatter: ({ cellValue, row }) =>
        (row as any).isGroup
          ? '-'
          : ({ 0: '启用', 1: '停用' } as Record<string, string>)[String(cellValue)] || cellValue || '-',
    },
    { field: 'remark', title: '备注', minWidth: 220 },
    { title: '操作', width: 160, fixed: 'right', slots: { default: 'actions' } },
  ];
}

export function buildWorkCenterGroupRows(list: MesHcWorkCenterApi.WorkCenter[]) {
  const groups = new Map<string, any>();
  for (const item of list || []) {
    const lineCode = String(item.lineCode || 'UNKNOWN').toUpperCase();
    if (!groups.has(lineCode)) {
      groups.set(lineCode, {
        id: `line-${lineCode}`,
        lineCode,
        lineName: getWorkCenterLineLabel(lineCode, item.lineName),
        isGroup: true,
        children: [],
      });
    }
    groups.get(lineCode).children.push({
      ...item,
      lineName: getWorkCenterLineLabel(lineCode, item.lineName),
    });
  }
  return Array.from(groups.values());
}
