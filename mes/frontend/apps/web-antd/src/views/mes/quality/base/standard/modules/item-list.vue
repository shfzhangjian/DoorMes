<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesQualityStandardApi } from '#/api/mes/quality/base/standard';

import { nextTick, ref, watch } from 'vue';

import { IconifyIcon } from '@vben/icons';

import {
  Button,
  Input,
  InputNumber,
  Select,
  Switch,
  Tooltip,
} from 'ant-design-vue';

import { useVbenVxeGrid } from '#/adapter/vxe-table';

import {
  cloneEntryRulePresetParams,
  DEFAULT_QUALITATIVE_ENTRY_RULE_PRESET,
  DEFAULT_QUANTITATIVE_ENTRY_RULE_PRESET,
  getEntryRulePresetSummary,
  stringifyEntryRulePresetParams,
  type EntryRulePresetKey,
} from './entry-rule-presets';
import EntryRuleConfigModal from './qms-entry-rule-config-modal.vue';

defineOptions({ name: 'QualityStandardItemList' });

const props = defineProps<{ applyType?: string; disabled?: boolean }>();

const activeRuleRow = ref<MesQualityStandardApi.StandardItem>();
const entryRuleModalRef = ref<InstanceType<typeof EntryRuleConfigModal>>();

const OQC_CATEGORY_OPTIONS = [
  { value: 'OQC_COA', label: '随货 COA' },
  { value: 'OQC_LABEL', label: '包装盒及产品标签' },
  { value: 'OQC_PACKING', label: '打包' },
  { value: 'OQC_OTHER', label: '其他检查项' },
];
const OQC_CHECKLIST_OPTIONS = ['OK', 'NG', 'NA'];

type PrecisionValueField =
  | 'avgMaxLimit'
  | 'avgMinLimit'
  | 'maxValue'
  | 'minValue'
  | 'stdMaxLimit'
  | 'stdMinLimit';
type PrecisionScaleField =
  | 'avgMaxLimitScale'
  | 'avgMinLimitScale'
  | 'maxValueScale'
  | 'minValueScale'
  | 'stdMaxLimitScale'
  | 'stdMinLimitScale';

const PRECISION_FIELDS: PrecisionValueField[] = [
  'avgMinLimit',
  'avgMaxLimit',
  'stdMinLimit',
  'stdMaxLimit',
  'minValue',
  'maxValue',
];
const PRECISION_SCALE_FIELD_MAP: Record<
  PrecisionValueField,
  PrecisionScaleField
> = {
  avgMaxLimit: 'avgMaxLimitScale',
  avgMinLimit: 'avgMinLimitScale',
  maxValue: 'maxValueScale',
  minValue: 'minValueScale',
  stdMaxLimit: 'stdMaxLimitScale',
  stdMinLimit: 'stdMinLimitScale',
};
const precisionInputTextCache = new WeakMap<
  MesQualityStandardApi.StandardItem,
  Partial<Record<PrecisionValueField, string>>
>();
const precisionInputRenderKey = ref(0);

function isOqcStandard() {
  return props.applyType === 'OQC';
}

function isIqcStandard() {
  return props.applyType === 'IQC';
}

function supportsItemAttachments() {
  return isIqcStandard() || props.applyType === 'FAI';
}

function supportsActualValueRequired() {
  return props.applyType === 'FQC';
}

function itemTypeOptions() {
  return [
    { value: 'QUANTITATIVE', label: '定量' },
    { value: 'QUALITATIVE', label: '定性' },
    ...(isIqcStandard() ? [{ value: 'DATE', label: '时间' }] : []),
  ];
}

function itemTypeLabel(itemType?: string) {
  return (
    itemTypeOptions().find((item) => item.value === itemType)?.label || '-'
  );
}

function resolveOqcCategoryLabel(value?: string) {
  if (!value) return '-';
  return (
    OQC_CATEGORY_OPTIONS.find((item) => item.value === value)?.label ||
    '其他检查项'
  );
}

function ensureOqcCategory(row: MesQualityStandardApi.StandardItem) {
  if (isOqcStandard() && !row.sheetSectionCode) {
    row.sheetSectionCode = 'OQC_OTHER';
  }
}

function syncOqcChecklistTemplateParams(
  row: MesQualityStandardApi.StandardItem,
) {
  if (!isOqcStandard() || row.itemType !== 'QUALITATIVE') return;
  ensureOqcCategory(row);
  const params = parseTemplateParams(row) || {};
  params.displayLayout ||= 'OQC_CHECKLIST';
  params.options = [...OQC_CHECKLIST_OPTIONS];
  params.category = row.sheetSectionCode || 'OQC_OTHER';
  params.sampleSize ||= row.sampleSize || 1;
  row.templateParams = JSON.stringify(params);
}

function buildColumns(): VxeTableGridOptions<MesQualityStandardApi.StandardItem>['columns'] {
  return [
    { type: 'seq', width: 50, align: 'center' },
    {
      field: 'inspectionItem',
      title: '检验项目',
      minWidth: 160,
      slots: { default: 'inspectionItem' },
    },
    {
      field: 'processName',
      title: '检测工序',
      width: 110,
      visible: false,
      slots: { default: 'processName' },
    },
    {
      field: 'sheetSectionCode',
      title: '检查分类',
      width: 160,
      visible: isOqcStandard(),
      slots: { default: 'sheetSectionCode' },
    },
    {
      field: 'itemType',
      title: '类型',
      width: 110,
      slots: { default: 'itemType' },
    },
    {
      field: 'actualValueRequired',
      title: '必填实际值',
      width: 105,
      align: 'center',
      visible: supportsActualValueRequired(),
      slots: { default: 'actualValueRequired' },
    },
    {
      field: 'expiryDays',
      title: '过期天数',
      width: 105,
      visible: isIqcStandard(),
      slots: { default: 'expiryDays' },
    },
    {
      field: 'standardDesc',
      title: '标准要求',
      minWidth: 220,
      slots: { default: 'standardDesc' },
    },
    {
      field: 'unit',
      title: '单位',
      width: 120,
      slots: { default: 'unit' },
    },
    {
      field: 'inspectionMethod',
      title: '检验方法',
      minWidth: 180,
      slots: { default: 'inspectionMethod' },
    },
    {
      field: 'testFrequencyJudgement',
      title: '位置/录入规则',
      minWidth: 280,
      slots: { default: 'entryRule' },
    },
    {
      field: 'ruleDescription',
      title: '规则说明',
      minWidth: 180,
      slots: { default: 'ruleDescription' },
    },
    {
      field: 'avgLimit',
      title: '平均值内控',
      width: 180,
      slots: { default: 'avgLimit' },
    },
    {
      field: 'stdLimit',
      title: '标准差内控',
      width: 180,
      slots: { default: 'stdLimit' },
    },
    {
      field: 'minValue',
      title: '下限(Min)',
      width: 100,
      slots: { default: 'minValue' },
    },
    {
      field: 'maxValue',
      title: '上限(Max)',
      width: 100,
      slots: { default: 'maxValue' },
    },
    {
      field: 'testTool',
      title: '检验仪器',
      width: 120,
      slots: { default: 'testTool' },
    },
    {
      field: 'sampleSize',
      title: '检测数',
      width: 80,
      align: 'center',
      slots: { default: 'sampleSize' },
    },
    {
      field: 'isSpc',
      title: 'SPC管控',
      width: 80,
      align: 'center',
      slots: { default: 'isSpc' },
    },
    {
      title: '操作',
      width: 140,
      align: 'center',
      slots: { default: 'action' },
      visible: !props.disabled,
    },
  ];
}

const [BaseGrid, gridApi] = useVbenVxeGrid({
  gridOptions: {
    border: true,
    showOverflow: true,
    keepSource: true,
    height: '100%',
    rowConfig: { keyField: 'id', isCurrent: true, isHover: true },
    pagerConfig: { enabled: false },
    toolbarConfig: { enabled: false },
    columns: buildColumns(),
    data: [],
  } as VxeTableGridOptions<MesQualityStandardApi.StandardItem>,
});

watch(
  () => props.applyType,
  () => {
    gridApi.setGridOptions({ columns: buildColumns() });
  },
);

// 暴露：加载数据
function loadData(data: MesQualityStandardApi.StandardItem[]) {
  gridApi.setGridOptions({ data });
}

function normalizeMetricCode(value?: string) {
  return (value || '').replace(/[^a-zA-Z0-9]/g, '').toLowerCase();
}

function isSameMetricCode(source?: string, target?: string) {
  if (!source || !target) return false;
  return (
    source === target ||
    normalizeMetricCode(source) === normalizeMetricCode(target)
  );
}

function parseTemplateParams(row: MesQualityStandardApi.StandardItem) {
  if (!row.templateParams) return undefined;
  try {
    return JSON.parse(row.templateParams) as Record<string, any>;
  } catch {
    return undefined;
  }
}

function applyEntryRulePreset(
  row: MesQualityStandardApi.StandardItem,
  presetKey: EntryRulePresetKey,
) {
  const params = cloneEntryRulePresetParams(presetKey);
  row.templateParams = JSON.stringify(params);
  row.testFrequencyJudgement = getEntryRulePresetSummary(presetKey);
  row.sampleSize = Number(params.sampleSize || row.sampleSize || 1);
  row.entryRuleType = 'PRESET';
  row.entryRuleTemplateId = undefined;
  row.entryRuleTemplateName = undefined;
  if (row.itemType === 'QUANTITATIVE') {
    row.valueTemplate = params.valueTemplate || 'SINGLE_VALUE';
    row.judgmentMetric =
      params.judgmentMetric || params.dataRule?.judgmentMetric || 'resultValue';
  }
}

function resolveJudgmentResultUnit(row: MesQualityStandardApi.StandardItem) {
  const params = parseTemplateParams(row);
  const dataRule = params?.dataRule || params;
  const resultFields = Array.isArray(dataRule?.resultFields)
    ? dataRule.resultFields
    : [];
  if (resultFields.length === 0) return row.unit || '';

  const judgmentMetric = dataRule?.judgmentMetric || row.judgmentMetric;
  const judgmentField =
    resultFields.find((item: any) =>
      isSameMetricCode(item?.code, judgmentMetric),
    ) ||
    resultFields.find((item: any) => item?.judgment) ||
    resultFields[0];

  return judgmentField?.unit || '';
}

function displayUnit(row: MesQualityStandardApi.StandardItem) {
  return resolveJudgmentResultUnit(row) || '-';
}

function systemTemplateName(row: MesQualityStandardApi.StandardItem) {
  if (row.itemType === 'QUALITATIVE') {
    return row.testFrequencyJudgement;
  }
  if (row.valueTemplate === 'DENSITY_CALC') return '密度计算模板';
  if (row.valueTemplate === 'COMPRESSION_CALC') return '压缩性能计算模板';
  if (row.valueTemplate === 'SINGLE_VALUE') return '单值实测模板';
  return '';
}

function displayEntryRule(row: MesQualityStandardApi.StandardItem) {
  return (
    row.entryRuleTemplateName ||
    systemTemplateName(row) ||
    row.testFrequencyJudgement ||
    '-'
  );
}

function displayEntryRuleTooltip(row: MesQualityStandardApi.StandardItem) {
  const title = displayEntryRule(row);
  const summary = row.testFrequencyJudgement;
  return summary && summary !== title ? `${title}：${summary}` : title;
}

function withRuleUnitSnapshot(item: MesQualityStandardApi.StandardItem) {
  return {
    ...item,
    unit: resolveJudgmentResultUnit(item),
  };
}

function getPrecisionScaleField(field: PrecisionValueField) {
  return PRECISION_SCALE_FIELD_MAP[field];
}

function getPrecisionCache(row: MesQualityStandardApi.StandardItem) {
  let cache = precisionInputTextCache.get(row);
  if (!cache) {
    cache = {};
    precisionInputTextCache.set(row, cache);
  }
  return cache;
}

function formatPrecisionValue(
  value: null | number | string | undefined,
  scale: null | number | undefined,
  emptyText = '',
) {
  if (value === undefined || value === null || value === '') return emptyText;
  const text = String(value);
  const normalizedScale =
    typeof scale === 'number' && scale >= 0 ? Math.min(scale, 30) : undefined;
  if (normalizedScale === undefined) return text;
  const numeric = Number(value);
  if (!Number.isFinite(numeric)) return text;
  return numeric.toFixed(normalizedScale);
}

function getPrecisionInputValue(
  row: MesQualityStandardApi.StandardItem,
  field: PrecisionValueField,
) {
  void precisionInputRenderKey.value;
  const cachedValue = precisionInputTextCache.get(row)?.[field];
  if (cachedValue !== undefined) return cachedValue;
  return formatPrecisionValue(
    row[field],
    row[getPrecisionScaleField(field)] as number | undefined,
  );
}

function formatPrecisionDisplay(
  row: MesQualityStandardApi.StandardItem,
  field: PrecisionValueField,
) {
  return formatPrecisionValue(
    row[field],
    row[getPrecisionScaleField(field)] as number | undefined,
    '-',
  );
}

function isDecimalDraft(value: string) {
  return /^[+-]?(?:\d+|\d*\.\d*)?$/.test(value);
}

function isCompleteDecimal(value: string) {
  return /^[+-]?(?:\d+\.?\d*|\.\d+)$/.test(value);
}

function resolveInputScale(value: string) {
  const dotIndex = value.indexOf('.');
  return dotIndex < 0 ? 0 : value.length - dotIndex - 1;
}

function handlePrecisionInput(
  row: MesQualityStandardApi.StandardItem,
  field: PrecisionValueField,
  value: string,
) {
  const text = String(value ?? '');
  const trimmed = text.trim();
  if (trimmed && !isDecimalDraft(trimmed)) {
    precisionInputRenderKey.value++;
    return;
  }

  const cache = getPrecisionCache(row);
  cache[field] = text;
  const scaleField = getPrecisionScaleField(field);
  if (!trimmed || !isCompleteDecimal(trimmed)) {
    row[field] = undefined;
    (row as any)[scaleField] = undefined;
    precisionInputRenderKey.value++;
    return;
  }

  const numeric = Number(trimmed);
  row[field] = Number.isFinite(numeric) ? numeric : undefined;
  (row as any)[scaleField] = Number.isFinite(numeric)
    ? resolveInputScale(trimmed)
    : undefined;
  precisionInputRenderKey.value++;
}

function handlePrecisionBlur(
  row: MesQualityStandardApi.StandardItem,
  field: PrecisionValueField,
) {
  const cache = precisionInputTextCache.get(row);
  if (cache) delete cache[field];
  precisionInputRenderKey.value++;
}

function clearPrecisionFields(row: MesQualityStandardApi.StandardItem) {
  const cache = precisionInputTextCache.get(row);
  PRECISION_FIELDS.forEach((field) => {
    row[field] = undefined;
    (row as any)[getPrecisionScaleField(field)] = undefined;
    if (cache) delete cache[field];
  });
}

function normalizePrecisionFieldsForSave(
  item: MesQualityStandardApi.StandardItem,
) {
  if (item.itemType !== 'QUANTITATIVE') {
    clearPrecisionFields(item);
    return;
  }
  PRECISION_FIELDS.forEach((field) => {
    const scaleField = getPrecisionScaleField(field);
    if (item[field] === undefined || item[field] === null) {
      (item as any)[scaleField] = undefined;
    }
  });
}

// 暴露：获取保存时的数据
function getData(): MesQualityStandardApi.StandardItem[] {
  if (!gridApi.grid) return [];
  // 获取当前表格所有数据
  const { fullData } = gridApi.grid.getTableData();
  // 过滤出新增时用的临时时间戳ID
  return fullData.map((item: any) => {
    const nextItem = withRuleUnitSnapshot(item);
    if (nextItem.itemType === 'QUALITATIVE' || nextItem.itemType === 'DATE') {
      nextItem.valueTemplate = undefined;
      nextItem.judgmentMetric = undefined;
      if (nextItem.itemType === 'DATE') {
        nextItem.templateParams = undefined;
        nextItem.testFrequencyJudgement = '按日期与过期天数自动判定';
      } else {
        syncOqcChecklistTemplateParams(nextItem);
      }
    }
    // IQC、FAI 的检验项统一允许上传附件，标准定义页不再提供人工开关。
    nextItem.attachmentEnabled = supportsItemAttachments()
      ? true
      : !!nextItem.attachmentEnabled;
    nextItem.actualValueRequired = supportsActualValueRequired()
      ? !!nextItem.actualValueRequired
      : false;
    normalizePrecisionFieldsForSave(nextItem);
    if (typeof item.id === 'string' || item.id > 10_000_000_000) {
      return { ...nextItem, id: undefined };
    }
    return nextItem;
  });
}

// 新增一行
async function handleAddRow() {
  if (!props.disabled && gridApi.grid) {
    const defaultRuleParams = cloneEntryRulePresetParams(
      DEFAULT_QUANTITATIVE_ENTRY_RULE_PRESET,
    );
    const defaultData: MesQualityStandardApi.StandardItem = {
      id: Date.now(), // 临时ID
      inspectionItem: '',
      itemType: 'QUANTITATIVE',
      standardDesc: '',
      unit: '',
      inspectionMethod: '',
      testFrequencyJudgement: getEntryRulePresetSummary(
        DEFAULT_QUANTITATIVE_ENTRY_RULE_PRESET,
      ),
      valueTemplate: defaultRuleParams.valueTemplate || 'SINGLE_VALUE',
      judgmentMetric:
        defaultRuleParams.judgmentMetric ||
        defaultRuleParams.dataRule?.judgmentMetric ||
        'resultValue',
      templateParams: JSON.stringify(defaultRuleParams),
      entryRuleType: 'PRESET',
      testTool: '',
      sampleSize: Number(defaultRuleParams.sampleSize || 5),
      isSpc: false,
      attachmentEnabled: supportsItemAttachments(),
      actualValueRequired: false,
      ...(isOqcStandard() ? { sheetSectionCode: 'OQC_OTHER' } : {}),
    };
    const { row } = await gridApi.grid.insertAt(defaultData, -1);
    await gridApi.grid.scrollToRow(row);
    await gridApi.grid.setCurrentRow(row);
  }
}

// 删除当前行
async function handleRemoveRow(row: any) {
  if (!props.disabled && gridApi.grid) await gridApi.grid.remove(row);
}

function getRowIndex(row: MesQualityStandardApi.StandardItem) {
  if (!gridApi.grid) return -1;
  const { fullData } = gridApi.grid.getTableData();
  return fullData.findIndex((item: any) => item === row);
}

function canMoveRow(
  row: MesQualityStandardApi.StandardItem,
  direction: 'down' | 'up',
) {
  if (props.disabled || !gridApi.grid) return false;
  const { fullData } = gridApi.grid.getTableData();
  const rowIndex = getRowIndex(row);
  if (rowIndex < 0) return false;
  return direction === 'up' ? rowIndex > 0 : rowIndex < fullData.length - 1;
}

async function handleMoveRow(
  row: MesQualityStandardApi.StandardItem,
  direction: 'down' | 'up',
) {
  if (!canMoveRow(row, direction) || !gridApi.grid) return;

  const { fullData } = gridApi.grid.getTableData();
  const rowIndex = getRowIndex(row);
  const targetIndex = direction === 'up' ? rowIndex - 1 : rowIndex + 1;
  const nextRows = [...fullData];

  [nextRows[rowIndex], nextRows[targetIndex]] = [
    nextRows[targetIndex],
    nextRows[rowIndex],
  ];
  gridApi.setGridOptions({ data: nextRows });

  await nextTick();
  await gridApi.grid.setCurrentRow(row);
  await gridApi.grid.scrollToRow(row);
}

function handleItemTypeChange(row: MesQualityStandardApi.StandardItem) {
  if (row.itemType === 'DATE') {
    row.valueTemplate = undefined;
    row.judgmentMetric = undefined;
    row.templateParams = undefined;
    row.testFrequencyJudgement = '按日期与过期天数自动判定';
    row.entryRuleType = 'PRESET';
    row.entryRuleTemplateId = undefined;
    row.entryRuleTemplateName = undefined;
    row.ruleDescription = '所选日期早于当前日期超过设定天数时自动判定NG';
    row.expiryDays ??= 0;
    row.sampleSize = 1;
    row.isSpc = false;
    clearPrecisionFields(row);
    row.sheetTemplateId = undefined;
    row.sheetSectionCode = undefined;
    row.sheetMetricCode = undefined;
    row.sheetFieldCode = undefined;
    return;
  }
  row.expiryDays = undefined;
  if (row.itemType !== 'QUANTITATIVE') {
    row.valueTemplate = undefined;
    row.judgmentMetric = undefined;
    row.templateParams = isOqcStandard()
      ? ''
      : stringifyEntryRulePresetParams(DEFAULT_QUALITATIVE_ENTRY_RULE_PRESET);
    row.testFrequencyJudgement = getEntryRulePresetSummary(
      DEFAULT_QUALITATIVE_ENTRY_RULE_PRESET,
    );
    row.entryRuleType = 'PRESET';
    row.entryRuleTemplateId = undefined;
    row.entryRuleTemplateName = undefined;
    row.sampleSize = 1;
    row.avgMinLimit = undefined;
    row.avgMaxLimit = undefined;
    row.stdMinLimit = undefined;
    row.stdMaxLimit = undefined;
    clearPrecisionFields(row);
    row.sheetTemplateId = undefined;
    row.sheetSectionCode = isOqcStandard() ? row.sheetSectionCode : undefined;
    row.sheetMetricCode = undefined;
    row.sheetFieldCode = undefined;
    ensureOqcCategory(row);
    syncOqcChecklistTemplateParams(row);
    return;
  }
  applyEntryRulePreset(row, DEFAULT_QUANTITATIVE_ENTRY_RULE_PRESET);
  ensureOqcCategory(row);
}

function handleOpenRuleConfig(
  row: MesQualityStandardApi.StandardItem,
  forceCustom = false,
) {
  activeRuleRow.value = row;
  entryRuleModalRef.value?.open(
    row,
    !!props.disabled,
    forceCustom ? 'CUSTOM' : undefined,
  );
}

function handleRuleConfigured(
  payload: Pick<
    MesQualityStandardApi.StandardItem,
    | 'entryRuleType'
    | 'entryRuleTemplateId'
    | 'entryRuleTemplateName'
    | 'ruleDescription'
    | 'sampleSize'
    | 'templateParams'
    | 'testFrequencyJudgement'
    | 'valueTemplate'
    | 'judgmentMetric'
  >,
) {
  if (!activeRuleRow.value) return;
  Object.assign(activeRuleRow.value, payload);
  activeRuleRow.value.unit = resolveJudgmentResultUnit(activeRuleRow.value);
}

defineExpose({ loadData, getData });
</script>

<template>
  <div class="flex h-full flex-col bg-slate-50">
    <div
      class="flex items-center justify-between border-b border-slate-200 bg-white px-3 py-2"
    >
      <div class="flex items-center gap-2">
        <div class="h-3.5 w-1 rounded-sm bg-indigo-600"></div>
        <span class="text-sm font-bold text-slate-700">检验项明细</span>
      </div>
      <Button
        v-if="!disabled"
        type="primary"
        size="small"
        @click="handleAddRow"
      >
        <IconifyIcon icon="lucide:plus" class="mr-1" /> 添加检验项
      </Button>
    </div>

    <div class="min-h-0 flex-1 bg-white p-2">
      <BaseGrid>
        <template #inspectionItem="{ row }">
          <Input
            v-if="!disabled"
            v-model:value="row.inspectionItem"
            placeholder="如: 厚度"
            size="small"
          />
          <span v-else>{{ row.inspectionItem }}</span>
        </template>

        <template #processName="{ row }">
          <span>{{ row.processName || row.processCode || '-' }}</span>
        </template>

        <template #sheetSectionCode="{ row }">
          <Select
            v-if="!disabled"
            v-model:value="row.sheetSectionCode"
            size="small"
            class="w-full"
            placeholder="请选择检查分类"
            :options="OQC_CATEGORY_OPTIONS"
          />
          <span v-else>{{
            resolveOqcCategoryLabel(row.sheetSectionCode)
          }}</span>
        </template>

        <template #itemType="{ row }">
          <Select
            v-if="!disabled"
            v-model:value="row.itemType"
            size="small"
            class="w-full"
            @change="() => handleItemTypeChange(row)"
            :options="itemTypeOptions()"
          />
          <span v-else>{{ itemTypeLabel(row.itemType) }}</span>
        </template>

        <template #expiryDays="{ row }">
          <InputNumber
            v-if="!disabled && row.itemType === 'DATE'"
            v-model:value="row.expiryDays"
            :min="0"
            :precision="0"
            size="small"
            class="w-full"
            placeholder="天数"
          />
          <span v-else-if="row.itemType === 'DATE'">{{
            row.expiryDays ?? '-'
          }}</span>
          <span v-else class="text-slate-400">-</span>
        </template>

        <template #actualValueRequired="{ row }">
          <Switch
            v-if="!disabled"
            v-model:checked="row.actualValueRequired"
            size="small"
          />
          <span v-else>{{ row.actualValueRequired ? '是' : '否' }}</span>
        </template>

        <template #standardDesc="{ row }">
          <Input
            v-if="!disabled"
            v-model:value="row.standardDesc"
            placeholder="如: 0.05±0.003mm"
            size="small"
          />
          <span v-else>{{ row.standardDesc }}</span>
        </template>

        <template #unit="{ row }">
          <span class="text-slate-500">{{ displayUnit(row) }}</span>
        </template>

        <template #inspectionMethod="{ row }">
          <Input
            v-if="!disabled"
            v-model:value="row.inspectionMethod"
            placeholder="如: 核对COA"
            size="small"
          />
          <span v-else>{{ row.inspectionMethod }}</span>
        </template>

        <template #entryRule="{ row }">
          <span v-if="row.itemType === 'DATE'">
            日期超过 {{ row.expiryDays ?? '-' }} 天自动判NG
          </span>
          <div v-else-if="!disabled" class="flex items-center gap-1">
            <Input
              :value="displayEntryRule(row)"
              size="small"
              class="min-w-0 flex-1"
              readonly
              placeholder="请在规则配置中维护"
            />
            <Button size="small" @click="handleOpenRuleConfig(row)">
              <IconifyIcon icon="lucide:sliders-horizontal" />
            </Button>
          </div>
          <div v-else class="flex items-center gap-1">
            <Tooltip :title="displayEntryRuleTooltip(row)">
              <span class="min-w-0 flex-1 truncate">{{
                displayEntryRule(row)
              }}</span>
            </Tooltip>
            <Button size="small" @click="handleOpenRuleConfig(row)">
              查看
            </Button>
          </div>
        </template>

        <template #ruleDescription="{ row }">
          <Tooltip :title="row.ruleDescription || '未填写规则说明'">
            <span class="block truncate">{{ row.ruleDescription || '-' }}</span>
          </Tooltip>
        </template>

        <template #avgLimit="{ row }">
          <div
            v-if="!disabled && row.itemType === 'QUANTITATIVE'"
            class="grid grid-cols-2 gap-1"
          >
            <Input
              :value="getPrecisionInputValue(row, 'avgMinLimit')"
              size="small"
              class="w-full"
              placeholder="下限"
              inputmode="decimal"
              @update:value="
                (value) => handlePrecisionInput(row, 'avgMinLimit', value)
              "
              @blur="() => handlePrecisionBlur(row, 'avgMinLimit')"
            />
            <Input
              :value="getPrecisionInputValue(row, 'avgMaxLimit')"
              size="small"
              class="w-full"
              placeholder="上限"
              inputmode="decimal"
              @update:value="
                (value) => handlePrecisionInput(row, 'avgMaxLimit', value)
              "
              @blur="() => handlePrecisionBlur(row, 'avgMaxLimit')"
            />
          </div>
          <span v-else-if="row.itemType === 'QUANTITATIVE'"
            >{{ formatPrecisionDisplay(row, 'avgMinLimit') }} ~
            {{ formatPrecisionDisplay(row, 'avgMaxLimit') }}</span
          >
          <span v-else class="text-slate-400">-</span>
        </template>

        <template #stdLimit="{ row }">
          <div
            v-if="!disabled && row.itemType === 'QUANTITATIVE'"
            class="grid grid-cols-2 gap-1"
          >
            <Input
              :value="getPrecisionInputValue(row, 'stdMinLimit')"
              size="small"
              class="w-full"
              placeholder="下限"
              inputmode="decimal"
              @update:value="
                (value) => handlePrecisionInput(row, 'stdMinLimit', value)
              "
              @blur="() => handlePrecisionBlur(row, 'stdMinLimit')"
            />
            <Input
              :value="getPrecisionInputValue(row, 'stdMaxLimit')"
              size="small"
              class="w-full"
              placeholder="上限"
              inputmode="decimal"
              @update:value="
                (value) => handlePrecisionInput(row, 'stdMaxLimit', value)
              "
              @blur="() => handlePrecisionBlur(row, 'stdMaxLimit')"
            />
          </div>
          <span v-else-if="row.itemType === 'QUANTITATIVE'"
            >{{ formatPrecisionDisplay(row, 'stdMinLimit') }} ~
            {{ formatPrecisionDisplay(row, 'stdMaxLimit') }}</span
          >
          <span v-else class="text-slate-400">-</span>
        </template>

        <template #minValue="{ row }">
          <Input
            v-if="!disabled && row.itemType === 'QUANTITATIVE'"
            :value="getPrecisionInputValue(row, 'minValue')"
            size="small"
            class="w-full"
            inputmode="decimal"
            @update:value="
              (value) => handlePrecisionInput(row, 'minValue', value)
            "
            @blur="() => handlePrecisionBlur(row, 'minValue')"
          />
          <span v-else-if="row.itemType === 'QUANTITATIVE'">{{
            formatPrecisionDisplay(row, 'minValue')
          }}</span>
          <span v-else class="text-slate-400">-</span>
        </template>

        <template #maxValue="{ row }">
          <Input
            v-if="!disabled && row.itemType === 'QUANTITATIVE'"
            :value="getPrecisionInputValue(row, 'maxValue')"
            size="small"
            class="w-full"
            inputmode="decimal"
            @update:value="
              (value) => handlePrecisionInput(row, 'maxValue', value)
            "
            @blur="() => handlePrecisionBlur(row, 'maxValue')"
          />
          <span v-else-if="row.itemType === 'QUANTITATIVE'">{{
            formatPrecisionDisplay(row, 'maxValue')
          }}</span>
          <span v-else class="text-slate-400">-</span>
        </template>

        <template #testTool="{ row }">
          <Input v-if="!disabled" v-model:value="row.testTool" size="small" />
          <span v-else>{{ row.testTool }}</span>
        </template>

        <template #sampleSize="{ row }">
          <span>{{ row.sampleSize || '-' }}</span>
        </template>

        <template #isSpc="{ row }">
          <Switch v-if="!disabled" v-model:checked="row.isSpc" size="small" />
          <span v-else>{{ row.isSpc ? '是' : '否' }}</span>
        </template>

        <template #action="{ row }">
          <div
            v-if="!disabled"
            class="flex items-center justify-center gap-1 whitespace-nowrap"
          >
            <Tooltip title="上移">
              <Button
                type="text"
                size="small"
                :disabled="!canMoveRow(row, 'up')"
                @click="handleMoveRow(row, 'up')"
              >
                <IconifyIcon icon="lucide:arrow-up" />
              </Button>
            </Tooltip>
            <Tooltip title="下移">
              <Button
                type="text"
                size="small"
                :disabled="!canMoveRow(row, 'down')"
                @click="handleMoveRow(row, 'down')"
              >
                <IconifyIcon icon="lucide:arrow-down" />
              </Button>
            </Tooltip>
            <Button
              type="link"
              danger
              size="small"
              @click="handleRemoveRow(row)"
            >
              移除
            </Button>
          </div>
          <span v-else>-</span>
        </template>
      </BaseGrid>
    </div>

    <EntryRuleConfigModal
      ref="entryRuleModalRef"
      @confirm="handleRuleConfigured"
    />
  </div>
</template>
