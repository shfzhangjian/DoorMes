import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesHcLotRuleApi } from '#/api/mes/hc/lotrule';

const statusOptions = [
  { label: '草稿', value: 0 },
  { label: '启用', value: 1 },
  { label: '停用', value: 2 },
];

const productCategoryOptions = [
  { label: '白垫', value: 'WHITE_PAD' },
  { label: '黑垫', value: 'BLACK_PAD' },
  { label: '通用', value: 'COMMON' },
];

const prodTypeOptions = [
  { label: '量产', value: 'MASS' },
  { label: '研发试制', value: 'RND_TRIAL' },
];

const modelMatchOptions = [
  { label: '全部', value: 'ALL' },
  { label: '精确匹配', value: 'EXACT' },
  { label: '前缀匹配', value: 'PREFIX' },
  { label: '列表匹配', value: 'LIST' },
];

const generationTriggerOptions = [{ label: '配方开工', value: 'FORMULA_START' }];
const generationScopeOptions = [{ label: '每张计划一个主批', value: 'PLAN_ROOT' }];

const bizTypeOptions = [
  { label: '成品批号', value: 'FG_LOT' },
  { label: '片号', value: 'PIECE_LOT' },
  { label: '卷材批号', value: 'ROLL_LOT' },
  { label: '序列号', value: 'SERIAL_NO' },
];

const ruleModeOptions = [
  { label: '批次号', value: 'LOT' },
  { label: '序列号', value: 'SERIAL' },
];

const resetCycleOptions = [
  { label: '按月重置', value: 'MONTH' },
  { label: '按年重置', value: 'YEAR' },
  { label: '按日重置', value: 'DAY' },
  { label: '永不重置', value: 'NONE' },
];

const yearCodeModeOptions = [
  { label: '两位年份，如 25', value: 'yy' },
  { label: '四位年份，如 2025', value: 'yyyy' },
];

const monthCodeModeOptions = [
  { label: 'A,B,C,D,E,F,G,H,J,K,L,M', value: 'A_TO_M' },
];

const advancedOnlyDependencies = {
  triggerFields: ['__uiMode'],
  show: (values: Record<string, any>) => values.__uiMode === 'ADVANCED',
};

function bizTypeLabel(value?: string) {
  return bizTypeOptions.find((item) => item.value === value)?.label || value || '-';
}

function ruleModeLabel(value?: string) {
  return ruleModeOptions.find((item) => item.value === value)?.label || value || '-';
}

function statusLabel(value?: number) {
  return statusOptions.find((item) => item.value === value)?.label || '-';
}

export function useFormSchema(): VbenFormSchema[] {
  return [
    {
      fieldName: '__uiMode',
      component: 'Input',
      defaultValue: 'SIMPLE',
      dependencies: { triggerFields: [''], show: () => false },
    },
    {
      fieldName: 'id',
      component: 'Input',
      dependencies: { triggerFields: [''], show: () => false },
    },
    {
      fieldName: 'ruleCode',
      label: '规则编码',
      component: 'Input',
      componentProps: { placeholder: '简单模式由系统自动生成；高级模式可维护' },
      dependencies: advancedOnlyDependencies,
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
      fieldName: 'bizType',
      label: '业务对象',
      component: 'Select',
      rules: 'required',
      componentProps: {
        options: bizTypeOptions,
        allowClear: false,
        placeholder: '请选择业务对象',
      },
      dependencies: advancedOnlyDependencies,
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'ruleMode',
      label: '规则模式',
      component: 'RadioGroup',
      componentProps: {
        options: ruleModeOptions,
        optionType: 'button',
        buttonStyle: 'solid',
      },
      defaultValue: 'LOT',
      dependencies: advancedOnlyDependencies,
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'productCategoryCode',
      label: '产品分类',
      component: 'Select',
      componentProps: { options: productCategoryOptions, placeholder: '请选择适用垫型' },
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'prefix',
      label: '批号前缀',
      component: 'Input',
      rules: 'required',
      componentProps: { maxlength: 16, placeholder: '例如 W 或 C' },
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'prodType',
      label: '生产类型',
      component: 'Select',
      componentProps: { options: prodTypeOptions, placeholder: '请选择生产类型' },
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'modelMatchMode',
      label: '型号匹配方式',
      component: 'Select',
      componentProps: { options: modelMatchOptions, allowClear: false },
      defaultValue: 'ALL',
      dependencies: advancedOnlyDependencies,
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'modelMatchValue',
      label: '型号匹配值',
      component: 'Input',
      componentProps: { placeholder: '全部匹配时留空；列表以逗号分隔' },
      dependencies: advancedOnlyDependencies,
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'priority',
      label: '规则优先级',
      component: 'InputNumber',
      componentProps: { class: 'w-full', min: 0, precision: 0 },
      defaultValue: 100,
      dependencies: advancedOnlyDependencies,
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'generationTrigger',
      label: '生成时机',
      component: 'Select',
      rules: 'required',
      componentProps: { options: generationTriggerOptions, allowClear: false },
      defaultValue: 'FORMULA_START',
      dependencies: advancedOnlyDependencies,
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'generationScope',
      label: '生成粒度',
      component: 'Select',
      rules: 'required',
      componentProps: { options: generationScopeOptions, allowClear: false },
      defaultValue: 'PLAN_ROOT',
      dependencies: advancedOnlyDependencies,
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'batchCardinality',
      label: '批次数量',
      component: 'RadioGroup',
      componentProps: { options: [{ label: '一个', value: 'ONE' }], optionType: 'button', buttonStyle: 'solid' },
      defaultValue: 'ONE',
      dependencies: advancedOnlyDependencies,
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'counterGroupCode',
      label: '流水组编码',
      component: 'Input',
      componentProps: { placeholder: '留空即规则独立流水' },
      dependencies: advancedOnlyDependencies,
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'yearCodeMode',
      label: '年份代码',
      component: 'Select',
      componentProps: {
        options: yearCodeModeOptions,
        allowClear: false,
        placeholder: '请选择年份代码规则',
      },
      defaultValue: 'yy',
      dependencies: advancedOnlyDependencies,
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'monthCodeMode',
      label: '月份代码',
      component: 'Select',
      componentProps: {
        options: monthCodeModeOptions,
        allowClear: false,
        placeholder: '请选择月份代码规则',
      },
      defaultValue: 'A_TO_M',
      dependencies: advancedOnlyDependencies,
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'seqLength',
      label: '流水长度',
      component: 'InputNumber',
      rules: 'required',
      componentProps: { class: 'w-full', min: 1, precision: 0 },
      defaultValue: 3,
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'seqStart',
      label: '流水起始值',
      component: 'InputNumber',
      componentProps: { class: 'w-full', min: 1, precision: 0 },
      defaultValue: 1,
      dependencies: advancedOnlyDependencies,
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'seqStep',
      label: '流水步长',
      component: 'InputNumber',
      componentProps: { class: 'w-full', min: 1, precision: 0 },
      defaultValue: 1,
      dependencies: advancedOnlyDependencies,
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'resetCycle',
      label: '重置周期',
      component: 'Select',
      componentProps: {
        options: resetCycleOptions,
        allowClear: false,
        placeholder: '请选择重置周期',
      },
      defaultValue: 'YEAR',
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'allowPreview',
      label: '允许预览',
      component: 'RadioGroup',
      componentProps: {
        options: [
          { label: '是', value: true },
          { label: '否', value: false },
        ],
        optionType: 'button',
        buttonStyle: 'solid',
      },
      defaultValue: true,
      dependencies: advancedOnlyDependencies,
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'allowParse',
      label: '允许解析',
      component: 'RadioGroup',
      componentProps: {
        options: [
          { label: '是', value: true },
          { label: '否', value: false },
        ],
        optionType: 'button',
        buttonStyle: 'solid',
      },
      defaultValue: true,
      dependencies: advancedOnlyDependencies,
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'allowManualOverride',
      label: '允许人工改号',
      component: 'RadioGroup',
      componentProps: { options: [{ label: '是', value: true }, { label: '否', value: false }], optionType: 'button', buttonStyle: 'solid' },
      defaultValue: false,
      dependencies: advancedOnlyDependencies,
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'status',
      label: '状态',
      component: 'RadioGroup',
      rules: 'required',
      componentProps: {
        options: statusOptions,
        optionType: 'button',
        buttonStyle: 'solid',
        disabled: true,
      },
      defaultValue: 1,
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'sampleSegmentRule',
      label: '抽样段规则',
      component: 'Input',
      componentProps: { placeholder: '默认 1-11 对应 P-Z' },
      defaultValue: '1=P,2=Q,3=R,4=S,5=T,6=U,7=V,8=W,9=X,10=Y,11=Z',
      formItemClass: 'col-span-2',
      dependencies: advancedOnlyDependencies,
    },
    {
      fieldName: 'remark',
      label: '备注',
      component: 'Textarea',
      componentProps: { rows: 3, placeholder: '请输入备注' },
      formItemClass: 'col-span-2',
      dependencies: advancedOnlyDependencies,
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
      fieldName: 'bizType',
      label: '业务对象',
      component: 'Select',
      componentProps: {
        options: bizTypeOptions,
        allowClear: true,
        placeholder: '请选择业务对象',
      },
    },
    {
      fieldName: 'productCategoryCode',
      label: '产品分类',
      component: 'Select',
      componentProps: { options: productCategoryOptions, allowClear: true },
    },
    {
      fieldName: 'prodType',
      label: '生产类型',
      component: 'Select',
      componentProps: { options: prodTypeOptions, allowClear: true },
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

export function useGridColumns(): VxeTableGridOptions<MesHcLotRuleApi.LotRule>['columns'] {
  return [
    { type: 'checkbox', width: 48, fixed: 'left' },
    { field: 'ruleCode', title: '规则编码', minWidth: 140 },
    { field: 'ruleName', title: '规则名称', minWidth: 180 },
    { field: 'versionNo', title: '版本', width: 80 },
    {
      field: 'bizType',
      title: '业务对象',
      minWidth: 120,
      formatter: ({ cellValue }) => bizTypeLabel(cellValue),
    },
    { field: 'productCategoryCode', title: '适用垫型', minWidth: 100 },
    { field: 'prodType', title: '生产类型', minWidth: 100 },
    { field: 'generationTrigger', title: '生成时机', minWidth: 120 },
    { field: 'generationScope', title: '生成粒度', minWidth: 120 },
    {
      field: 'ruleMode',
      title: '规则模式',
      minWidth: 100,
      formatter: ({ cellValue }) => ruleModeLabel(cellValue),
    },
    { field: 'seqLength', title: '流水长度', minWidth: 100 },
    {
      field: 'status',
      title: '状态',
      minWidth: 100,
      formatter: ({ cellValue }) => statusLabel(cellValue),
    },
    { field: 'remark', title: '备注', minWidth: 180, showOverflow: 'tooltip' },
    { field: 'createTime', title: '创建时间', minWidth: 160 },
    { field: 'updateTime', title: '更新时间', minWidth: 160 },
    {
      title: '操作',
      width: 320,
      fixed: 'right',
      slots: { default: 'actions' },
    },
  ];
}
