import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesHcModelRuleApi } from '#/api/mes/hc/modelrule';

export const statusOptions = [
  { label: '启用', value: 1 },
  { label: '停用', value: 0 },
];

export const ruleCategoryOptions = [
  { label: '成品型号', value: 'FG_MODEL' },
  { label: '半成品型号', value: 'WIP_MODEL' },
  { label: '包装标签', value: 'PK_LABEL' },
];

export const targetLevelOptions = [
  { label: '成品', value: 'FG' },
  { label: '在制/半成品', value: 'WIP' },
  { label: '原材料', value: 'RM' },
  { label: '包材', value: 'PM' },
];

const statusMap = Object.fromEntries(statusOptions.map((item) => [String(item.value), item.label]));
const ruleCategoryMap = Object.fromEntries(ruleCategoryOptions.map((item) => [String(item.value), item.label]));
const targetLevelMap = Object.fromEntries(targetLevelOptions.map((item) => [String(item.value), item.label]));

export function useFormSchema(): VbenFormSchema[] {
  return [
    {
      fieldName: 'id',
      component: 'Input',
      dependencies: { triggerFields: [''], show: () => false },
    },
    {
      fieldName: 'ruleCode',
      label: '规则编码',
      component: 'Input',
      rules: 'required',
      componentProps: { placeholder: '请输入规则编码' },
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'ruleName',
      label: '规则名称',
      component: 'Input',
      rules: 'required',
      componentProps: { placeholder: '请输入规则名称' },
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'ruleCategory',
      label: '规则分类',
      component: 'Select',
      rules: 'required',
      componentProps: {
        options: ruleCategoryOptions,
        allowClear: true,
        placeholder: '请选择规则分类',
      },
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'targetLevel',
      label: '产品层次',
      component: 'Select',
      rules: 'required',
      componentProps: {
        options: targetLevelOptions,
        allowClear: true,
        placeholder: '请选择产品层次',
      },
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'effectiveVersion',
      label: '当前生效版本',
      component: 'Input',
      componentProps: { placeholder: '请输入当前生效版本' },
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'status',
      label: '状态',
      component: 'RadioGroup',
      rules: 'required',
      componentProps: {
        options: statusOptions,
        buttonStyle: 'solid',
        optionType: 'button',
      },
      defaultValue: 1,
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'ruleDesc',
      label: '规则说明',
      component: 'Textarea',
      componentProps: { rows: 3, placeholder: '请输入规则说明' },
      formItemClass: 'col-span-2',
    },
  ];
}

export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      fieldName: 'ruleCode',
      label: '规则编码',
      component: 'Input',
      componentProps: { placeholder: '请输入规则编码' },
    },
    {
      fieldName: 'ruleName',
      label: '规则名称',
      component: 'Input',
      componentProps: { placeholder: '请输入规则名称' },
    },
    {
      fieldName: 'status',
      label: '状态',
      component: 'Select',
      componentProps: {
        options: statusOptions,
        allowClear: true,
        placeholder: '请选择状态',
      },
    },
  ];
}

export function useGridColumns(): VxeTableGridOptions<MesHcModelRuleApi.ModelRule>['columns'] {
  return [
    { type: 'checkbox', width: 48, fixed: 'left' },
    { field: 'ruleCode', title: '规则编码', minWidth: 140 },
    { field: 'ruleName', title: '规则名称', minWidth: 180 },
    {
      field: 'status',
      title: '状态',
      minWidth: 100,
      formatter: ({ cellValue }) => statusMap[String(cellValue)] || cellValue || '-',
    },
    {
      field: 'ruleCategory',
      title: '规则分类',
      minWidth: 140,
      formatter: ({ cellValue }) => ruleCategoryMap[String(cellValue)] || cellValue || '-',
    },
    {
      field: 'targetLevel',
      title: '产品层次',
      minWidth: 140,
      formatter: ({ cellValue }) => targetLevelMap[String(cellValue)] || cellValue || '-',
    },
    { field: 'effectiveVersion', title: '当前生效版本', minWidth: 120 },
    { field: 'ruleDesc', title: '规则说明', minWidth: 220 },
    { title: '操作', width: 160, fixed: 'right', slots: { default: 'actions' } },
  ];
}
