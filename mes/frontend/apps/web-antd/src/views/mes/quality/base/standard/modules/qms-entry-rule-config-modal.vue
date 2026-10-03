<script lang="ts" setup>
import type { MesQualityStandardApi } from '#/api/mes/quality/base/standard';

import { computed, nextTick, reactive, ref, watch } from 'vue';

import { IconifyIcon } from '@vben/icons';

import {
  createEntryRuleTemplate,
  deleteEntryRuleTemplate,
  getEntryRuleTemplateList,
  updateEntryRuleTemplate,
} from '#/api/mes/quality/base/standard';

import {
  Button,
  Collapse,
  Input,
  InputNumber,
  message,
  Modal,
  Radio,
  RadioGroup,
  Select,
  Switch,
  Tag,
  Textarea,
} from 'ant-design-vue';

import {
  DEFAULT_QUALITATIVE_ENTRY_RULE_PRESET,
  DEFAULT_QUANTITATIVE_ENTRY_RULE_PRESET,
  DEFAULT_STD_RULE,
  entryRulePresetOptions,
  entryRulePresets,
  isEntryRulePresetKey,
  type EntryRulePresetKey,
} from './entry-rule-presets';

defineOptions({ name: 'QmsEntryRuleConfigModal' });

type EntryRuleType = 'CUSTOM' | 'PRESET';

type PositionRule = {
  code: string;
  name: string;
  required?: boolean;
  sort: number;
};

type InputFieldRule = Required<
  Pick<MesQualityStandardApi.EntryRuleInputField, 'code' | 'name'>
> &
  MesQualityStandardApi.EntryRuleInputField;

type ResultFieldRule = Required<
  Pick<MesQualityStandardApi.EntryRuleResultField, 'code' | 'formula' | 'name'>
> &
  MesQualityStandardApi.EntryRuleResultField;

type EntryRulePayload = Pick<
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
>;

const emit = defineEmits<{
  confirm: [payload: EntryRulePayload];
}>();

const openState = ref(false);
const fullscreenState = ref(false);
const readonlyState = ref(false);
const customTemplates = ref<MesQualityStandardApi.EntryRuleTemplate[]>([]);
const itemType =
  ref<MesQualityStandardApi.StandardItem['itemType']>('QUANTITATIVE');
const activeItemRow = ref<MesQualityStandardApi.StandardItem>();
const FORMULA_FUNCTIONS = new Set(['avg', 'std', 'round']);
const UNSUPPORTED_FORMULA_KEYWORDS = new Set(['else', 'if']);
const advancedRuleActiveKeys = ref<string[]>([]);
const isManualSummary = ref(false);
const summaryAutoSyncPaused = ref(false);

type FormulaToken = {
  type: 'comma' | 'identifier' | 'number' | 'operator' | 'paren';
  value: string;
};

type FormulaTrialResult = {
  code: string;
  name: string;
  value: string;
};

const form = reactive({
  entryRuleType: 'PRESET' as EntryRuleType,
  presetRule: DEFAULT_QUANTITATIVE_ENTRY_RULE_PRESET as EntryRulePresetKey,
  selectedTemplateValue: undefined as string | undefined,
  positions: [] as PositionRule[],
  repeatCount: 1,
  displayLayout: 'THREE_POINT_GRID',
  stdRule: DEFAULT_STD_RULE,
  optionsText: 'OK,NG',
  ngRemarkRequired: true,
  inputFields: [] as InputFieldRule[],
  resultFields: [] as ResultFieldRule[],
  judgmentMetric: '',
  savedTemplateId: undefined as number | undefined,
  templateName: '',
  ruleDescription: '',
  sampleSize: 1,
  testFrequencyJudgement: '',
});

const formulaTrialValues = reactive<Record<string, null | number | undefined>>(
  {},
);
const formulaTrialResults = ref<FormulaTrialResult[]>([]);
const formulaTrialError = ref('');
const modalWidth = computed(() =>
  fullscreenState.value ? 'calc(100vw - 32px)' : '1120px',
);
const modalStyle = computed(() =>
  fullscreenState.value
    ? {
        paddingBottom: '16px',
        top: '16px',
      }
    : undefined,
);

const isQualitative = computed(() => itemType.value === 'QUALITATIVE');
const quantitativePositionCount = computed(
  () => getNormalizedPositions().length,
);
const quantitativeRepeatCount = computed(() =>
  Math.max(1, Number(form.repeatCount || 1)),
);
const quantitativeSampleSize = computed(
  () => quantitativePositionCount.value * quantitativeRepeatCount.value,
);
const formulaTrialInputFields = computed(() => getNormalizedInputFields());

const availablePresetRuleOptions = computed(() =>
  entryRulePresetOptions.filter((item) =>
    isQualitative.value
      ? item.value === DEFAULT_QUALITATIVE_ENTRY_RULE_PRESET
      : item.value !== DEFAULT_QUALITATIVE_ENTRY_RULE_PRESET,
  ),
);

const ruleTemplateOptions = computed(() => {
  const options = [
    {
      label: '预设模板',
      options: availablePresetRuleOptions.value.map((item) => ({
        label: item.label,
        value: `PRESET:${item.value}`,
      })),
    },
  ];
  if (customTemplates.value.length > 0) {
    options.push({
      label: '已保存模板',
      options: customTemplates.value.map((item) => ({
        label: item.templateName,
        value: `CUSTOM:${item.id}`,
      })),
    });
  }
  return options;
});

const presetTemplateValue = computed(
  () => `PRESET:${form.presetRule || DEFAULT_QUANTITATIVE_ENTRY_RULE_PRESET}`,
);

const selectedSavedTemplate = computed(() =>
  form.savedTemplateId
    ? customTemplates.value.find((item) => item.id === form.savedTemplateId)
    : undefined,
);

const summaryAutoSource = computed(() =>
  JSON.stringify({
    itemType: itemType.value,
    positions: form.positions.map((item) => ({
      code: item.code,
      name: item.name,
      required: item.required,
    })),
    repeatCount: form.repeatCount,
    optionsText: form.optionsText,
    ngRemarkRequired: form.ngRemarkRequired,
    inputFields: form.inputFields.map((item) => ({ ...item })),
    resultFields: form.resultFields.map((item) => ({ ...item })),
    judgmentMetric: form.judgmentMetric,
  }),
);

watch(
  summaryAutoSource,
  () => {
    syncGeneratedSummary();
  },
  { flush: 'post' },
);

watch(
  () => form.inputFields.map((item) => item.code.trim()).join('|'),
  () => {
    syncFormulaTrialValues();
    resetFormulaTrialResult();
  },
  { flush: 'post' },
);

watch(
  () =>
    form.resultFields
      .map(
        (item) =>
          `${item.code.trim()}:${item.name.trim()}:${item.formula.trim()}:${
            item.precision ?? ''
          }`,
      )
      .join('|'),
  () => {
    resetFormulaTrialResult();
  },
  { flush: 'post' },
);

async function loadTemplateOptions(type = itemType.value) {
  customTemplates.value = await getEntryRuleTemplateList({
    itemType: type,
  }).catch(() => []);
}

function clonePositions(positions?: PositionRule[]) {
  return (positions || []).map((item, index) => ({
    code: item.code || '',
    name: item.name || '',
    required: item.required !== false,
    sort: Number(item.sort || index + 1),
  }));
}

function cloneInputFields(
  fields?: MesQualityStandardApi.EntryRuleInputField[],
) {
  return (fields || []).map((item) => ({
    code: item.code || '',
    name: item.name || '',
    unit: item.unit || '',
    type: item.type || 'NUMBER',
    required: item.required !== false,
    precision: Number(item.precision ?? 3),
  }));
}

function cloneResultFields(
  fields?: MesQualityStandardApi.EntryRuleResultField[],
) {
  return (fields || []).map((item) => ({
    code: item.code || '',
    name: item.name || '',
    formula: item.formula || '',
    unit: item.unit || '',
    judgment: !!item.judgment,
    precision: Number(item.precision ?? 3),
  }));
}

function pauseSummaryAutoSync() {
  summaryAutoSyncPaused.value = true;
  void nextTick(() => {
    summaryAutoSyncPaused.value = false;
  });
}

function setSummaryValue(summary: string, manual = false) {
  form.testFrequencyJudgement = summary;
  isManualSummary.value = manual;
}

function applyParams(params: Record<string, any>, summary?: string) {
  pauseSummaryAutoSync();
  form.positions = clonePositions(params.positions);
  form.repeatCount = Number(params.repeatCount || 1);
  form.displayLayout = params.displayLayout || 'POSITION_REPEAT_GRID';
  form.stdRule = DEFAULT_STD_RULE;
  form.optionsText = Array.isArray(params.options)
    ? params.options.join(',')
    : 'OK,NG';
  form.ngRemarkRequired = params.ngRemarkRequired !== false;
  form.sampleSize =
    Number(params.sampleSize) ||
    Math.max(1, form.positions.length * form.repeatCount);
  const dataRule = params.dataRule || params;
  form.inputFields = cloneInputFields(dataRule.inputFields);
  form.resultFields = cloneResultFields(dataRule.resultFields);
  form.judgmentMetric =
    dataRule.judgmentMetric ||
    form.resultFields.find((item) => item.judgment)?.code ||
    '';
  setSummaryValue(summary || generateSummary(), false);
}

function applySavedTemplate(value: unknown) {
  const id = Number(value);
  const template = customTemplates.value.find((item) => item.id === id);
  if (!template) return;
  form.entryRuleType = 'CUSTOM';
  form.savedTemplateId = template.id;
  form.templateName = template.templateName;
  form.ruleDescription = template.ruleDescription || '';
  try {
    applyParams(
      JSON.parse(template.templateParams),
      template.testFrequencyJudgement,
    );
  } catch {
    setSummaryValue(template.testFrequencyJudgement, false);
  }
  form.sampleSize = template.sampleSize || form.sampleSize;
}

function clearAppliedTemplateSnapshot(clearName = false) {
  form.savedTemplateId = undefined;
  if (clearName) {
    form.templateName = '';
  }
}

function applyPreset(rule: string) {
  if (!isEntryRulePresetKey(rule)) return;
  const preset = entryRulePresets[rule];
  if (!preset) return;
  form.presetRule = rule;
  applyParams(
    rule === 'COMPRESSION_CALC'
      ? buildCompressionTemplateParams(activeItemRow.value, preset.params)
      : JSON.parse(JSON.stringify(preset.params)),
    preset.summary,
  );
}

function handleRuleTemplateChange(value: unknown) {
  advancedRuleActiveKeys.value = [];
  if (typeof value !== 'string') {
    form.selectedTemplateValue = undefined;
    clearAppliedTemplateSnapshot(true);
    return;
  }

  form.selectedTemplateValue = value;
  if (value.startsWith('PRESET:')) {
    const rule = value.replace('PRESET:', '');
    form.entryRuleType = 'PRESET';
    clearAppliedTemplateSnapshot(true);
    applyPreset(rule);
    return;
  }

  if (value.startsWith('CUSTOM:')) {
    const id = Number(value.replace('CUSTOM:', ''));
    form.entryRuleType = 'CUSTOM';
    form.savedTemplateId = id;
    applySavedTemplate(id);
  }
}

function parseTemplateParams(row: MesQualityStandardApi.StandardItem) {
  if (!row.templateParams) {
    applyPreset(
      row.itemType === 'QUALITATIVE'
        ? DEFAULT_QUALITATIVE_ENTRY_RULE_PRESET
        : DEFAULT_QUANTITATIVE_ENTRY_RULE_PRESET,
    );
    return;
  }
  try {
    const params = JSON.parse(row.templateParams);
    applyParams(normalizeTemplateParams(row, params), row.testFrequencyJudgement);
  } catch {
    applyPreset(
      row.itemType === 'QUALITATIVE'
        ? DEFAULT_QUALITATIVE_ENTRY_RULE_PRESET
        : DEFAULT_QUANTITATIVE_ENTRY_RULE_PRESET,
    );
  }
}

function hasCompleteDataRule(params: Record<string, any>) {
  const dataRule = params.dataRule || params;
  const inputFields = Array.isArray(dataRule.inputFields)
    ? dataRule.inputFields
    : [];
  const resultFields = Array.isArray(dataRule.resultFields)
    ? dataRule.resultFields
    : [];
  const hasJudgment =
    !!dataRule.judgmentMetric || resultFields.some((item: any) => item?.judgment);
  return (
    inputFields.length > 0 &&
    resultFields.length > 0 &&
    resultFields.every((item: any) => item?.code && item?.formula) &&
    hasJudgment
  );
}

function hasCompleteCompressionDataRule(params: Record<string, any>) {
  const dataRule = params.dataRule || {};
  const inputCodes = Array.isArray(dataRule.inputFields)
    ? dataRule.inputFields.map((item: any) => item?.code).filter(Boolean)
    : [];
  const resultCodes = Array.isArray(dataRule.resultFields)
    ? dataRule.resultFields.map((item: any) => item?.code).filter(Boolean)
    : [];
  return (
    inputCodes.includes('t1Mm') &&
    inputCodes.includes('t2Mm') &&
    inputCodes.includes('t3Mm') &&
    resultCodes.includes('compressionRate') &&
    resultCodes.includes('compressionElasticityRate')
  );
}

function normalizeCompressionMetric(metric?: string) {
  const value = metric?.trim();
  if (!value) return '';
  const normalized = value.replaceAll('-', '_').toUpperCase();
  if (normalized === 'COMPRESSION_ELASTICITY_RATE') {
    return 'compressionElasticityRate';
  }
  if (normalized === 'COMPRESSION_RATE') {
    return 'compressionRate';
  }
  if (value === 'compressionElasticityRate' || value === 'compressionRate') {
    return value;
  }
  return '';
}

function resolveCompressionJudgmentMetric(
  row?: MesQualityStandardApi.StandardItem,
) {
  const explicit = normalizeCompressionMetric(row?.judgmentMetric);
  if (explicit) return explicit;
  const text = `${row?.inspectionItem || ''}${row?.standardDesc || ''}`;
  return text.includes('弹性') || text.includes('回弹')
    ? 'compressionElasticityRate'
    : 'compressionRate';
}

function normalizeCodeSegment(value: string) {
  return value
    .replaceAll(/[^\da-z]+/gi, '_')
    .replaceAll(/^_+|_+$/g, '')
    .toUpperCase();
}

function resolveCompressionEntryGroupCode(
  row?: MesQualityStandardApi.StandardItem,
) {
  const rowContext = (row || {}) as Record<string, any>;
  const source = `${row?.inspectionItem || ''} ${rowContext.processName || ''} ${
    rowContext.processCode || ''
  }`;
  const upperSource = source.toUpperCase();
  if (source.includes('成品') || upperSource.includes('FINAL')) {
    return 'COMPRESSION_FINAL_PRODUCTS';
  }
  if (
    source.includes('磨皮后') ||
    source.includes('磨后') ||
    upperSource.includes('NAP_POLISHED')
  ) {
    return 'COMPRESSION_NAP_POLISHED';
  }
  if (source.includes('未磨') || upperSource.includes('NAP_RAW')) {
    return 'COMPRESSION_NAP_RAW';
  }

  const context = (row?.inspectionItem || '').split('-').slice(0, -1).join('-');
  const codeSegment = normalizeCodeSegment(context);
  return codeSegment ? `COMPRESSION_${codeSegment}` : 'COMPRESSION_PERFORMANCE';
}

function buildCompressionTemplateParams(
  row: MesQualityStandardApi.StandardItem | undefined,
  params: Record<string, any>,
) {
  const base = hasCompleteCompressionDataRule(params)
    ? { ...params }
    : {
        ...entryRulePresets.COMPRESSION_CALC.params,
        ...params,
      };
  const judgmentMetric = resolveCompressionJudgmentMetric(row);
  const dataRule = base.dataRule ? { ...base.dataRule } : {};
  const resultFields = Array.isArray(dataRule.resultFields)
    ? dataRule.resultFields.map((item: any) => ({
        ...item,
        judgment: item?.code === judgmentMetric,
      }))
    : dataRule.resultFields;
  const entryGroupCode =
    !base.entryGroupCode || base.entryGroupCode === 'COMPRESSION_PERFORMANCE'
      ? resolveCompressionEntryGroupCode(row)
      : base.entryGroupCode;
  return {
    ...base,
    valueTemplate: 'COMPRESSION_CALC',
    judgmentMetric,
    entryGroupCode,
    entryGroupName: base.entryGroupName || '压缩性能',
    dataRule: {
      ...dataRule,
      resultFields,
      judgmentMetric,
    },
  };
}

function buildSingleValueDataRuleParams(
  row: MesQualityStandardApi.StandardItem,
  params: Record<string, any>,
) {
  const unit = row.unit || '';
  return {
    ...params,
    displayLayout: params.displayLayout || 'POSITION_REPEAT_GRID',
    valueTemplate: 'SINGLE_VALUE',
    judgmentMetric: 'resultValue',
    dataRule: {
      inputFields: [
        {
          code: 'value',
          name: '实测值',
          unit,
          type: 'NUMBER',
          required: true,
          precision: 3,
        },
      ],
      resultFields: [
        {
          code: 'resultValue',
          name: '结果值',
          formula: 'value',
          unit,
          judgment: true,
          precision: 3,
        },
      ],
      judgmentMetric: 'resultValue',
    },
  };
}

function normalizeTemplateParams(
  row: MesQualityStandardApi.StandardItem,
  params: Record<string, any>,
) {
  if (
    row.valueTemplate === 'SINGLE_VALUE' &&
    !hasCompleteDataRule(params)
  ) {
    return buildSingleValueDataRuleParams(row, params);
  }
  if (row.valueTemplate === 'COMPRESSION_CALC') {
    return hasCompleteDataRule(params)
      ? params
      : buildCompressionTemplateParams(row, params);
  }
  if (row.valueTemplate !== 'DENSITY_CALC') {
    return params;
  }
  if (hasCompleteDataRule(params)) {
    return params;
  }
  return {
    ...entryRulePresets.DENSITY_CALC.params,
    stdRule: DEFAULT_STD_RULE,
  };
}

function resolvePresetRule(
  row: MesQualityStandardApi.StandardItem,
): EntryRulePresetKey {
  if (row.entryRuleType === 'CUSTOM')
    return DEFAULT_QUANTITATIVE_ENTRY_RULE_PRESET;
  if (row.itemType === 'QUALITATIVE')
    return DEFAULT_QUALITATIVE_ENTRY_RULE_PRESET;
  if (row.valueTemplate === 'DENSITY_CALC') return 'DENSITY_CALC';
  if (row.valueTemplate === 'COMPRESSION_CALC') return 'COMPRESSION_CALC';
  if (!row.templateParams) return DEFAULT_QUANTITATIVE_ENTRY_RULE_PRESET;
  try {
    const params = JSON.parse(row.templateParams);
    const layout = params.displayLayout;
    const positions = Array.isArray(params.positions) ? params.positions : [];
    const repeatCount = Number(params.repeatCount || 1);
    if (
      row.valueTemplate === 'SINGLE_VALUE' &&
      positions.length === 1 &&
      repeatCount === 5
    ) {
      return DEFAULT_QUANTITATIVE_ENTRY_RULE_PRESET;
    }
    if (row.valueTemplate === 'SINGLE_VALUE' && row.sampleSize === 1) {
      return 'SINGLE_VALUE';
    }
    if (layout === 'THREE_POINT_GRID' || layout === 'DENSITY_THREE_POINT_GRID')
      return 'THREE_POINT';
    if (layout === 'THREE_POINT_FIVE_REPEAT_GRID')
      return 'THREE_POINT_FIVE_REPEAT';
    if (
      (layout === 'POSITION_REPEAT_GRID' ||
        layout === 'DENSITY_POSITION_REPEAT_GRID') &&
      positions.length === 5 &&
      repeatCount === 3
    ) {
      return 'FIVE_POINT_THREE_REPEAT';
    }
    if (
      layout === 'SEQUENCE_15_GRID' ||
      layout === 'COMPRESSION_SEQUENCE_15_GRID'
    )
      return 'SEQUENCE_15';
    if (layout === 'QUALITATIVE_JUDGMENT')
      return DEFAULT_QUALITATIVE_ENTRY_RULE_PRESET;
  } catch {}
  return DEFAULT_QUANTITATIVE_ENTRY_RULE_PRESET;
}

function open(
  row: MesQualityStandardApi.StandardItem,
  disabled = false,
  initialEntryRuleType?: EntryRuleType,
) {
  activeItemRow.value = row;
  fullscreenState.value = false;
  readonlyState.value = disabled;
  itemType.value = row.itemType;
  void loadTemplateOptions(row.itemType);
  form.entryRuleType = initialEntryRuleType || row.entryRuleType || 'PRESET';
  form.savedTemplateId = row.entryRuleTemplateId;
  form.selectedTemplateValue = undefined;
  form.templateName = row.entryRuleTemplateName || '';
  form.presetRule = resolvePresetRule(row);
  form.ruleDescription = row.ruleDescription || '';
  advancedRuleActiveKeys.value = [];
  parseTemplateParams(row);
  if (row.testFrequencyJudgement && row.valueTemplate !== 'DENSITY_CALC') {
    setSummaryValue(row.testFrequencyJudgement, false);
  }
  if (row.sampleSize) {
    form.sampleSize = row.sampleSize;
  }
  if (form.entryRuleType === 'PRESET') {
    form.selectedTemplateValue = presetTemplateValue.value;
  } else if (form.savedTemplateId) {
    form.selectedTemplateValue = `CUSTOM:${form.savedTemplateId}`;
  }
  isManualSummary.value = isCurrentSummaryManual(form.testFrequencyJudgement);
  openState.value = true;
}

function toggleFullscreen() {
  fullscreenState.value = !fullscreenState.value;
}

function handleEntryRuleTypeChange() {
  advancedRuleActiveKeys.value = [];
  if (form.entryRuleType === 'PRESET') {
    applyPreset(
      isQualitative.value
        ? DEFAULT_QUALITATIVE_ENTRY_RULE_PRESET
        : form.presetRule,
    );
    clearAppliedTemplateSnapshot(true);
    form.selectedTemplateValue = presetTemplateValue.value;
  } else {
    form.selectedTemplateValue = form.savedTemplateId
      ? `CUSTOM:${form.savedTemplateId}`
      : undefined;
  }
}

function handleAddPosition() {
  const positionIndex = form.positions.length + 1;
  form.positions.push({
    code: `P${String(positionIndex).padStart(2, '0')}`,
    name: '',
    required: true,
    sort: positionIndex,
  });
}

function handleRemovePosition(index: number) {
  form.positions.splice(index, 1);
  form.positions.forEach((item, currentIndex) => {
    item.sort = currentIndex + 1;
  });
}

function handleAddInputField() {
  const index = form.inputFields.length + 1;
  form.inputFields.push({
    code: `field${index}`,
    name: '',
    unit: '',
    type: 'NUMBER',
    required: true,
    precision: 3,
  });
}

function handleRemoveInputField(index: number) {
  form.inputFields.splice(index, 1);
}

function handleAddResultField() {
  const index = form.resultFields.length + 1;
  form.resultFields.push({
    code: `result${index}`,
    name: '',
    formula: '',
    unit: '',
    judgment: form.resultFields.length === 0,
    precision: 3,
  });
  if (!form.judgmentMetric) {
    form.judgmentMetric = form.resultFields[0]?.code || '';
  }
}

function handleRemoveResultField(index: number) {
  const removed = form.resultFields[index]?.code;
  form.resultFields.splice(index, 1);
  if (form.judgmentMetric === removed) {
    form.judgmentMetric = form.resultFields[0]?.code || '';
  }
}

function getQualitativeOptions() {
  return form.optionsText
    .split(/[,，\s]+/)
    .map((item) => item.trim())
    .filter(Boolean);
}

function validateFieldCode(code: string) {
  return /^[A-Za-z_]\w*$/.test(code);
}

function resetFormulaTrialResult() {
  formulaTrialResults.value = [];
  formulaTrialError.value = '';
}

function syncFormulaTrialValues() {
  const codes = new Set(
    form.inputFields.map((item) => item.code.trim()).filter(Boolean),
  );
  for (const code of codes) {
    if (!(code in formulaTrialValues)) {
      formulaTrialValues[code] = undefined;
    }
  }
  for (const code of Object.keys(formulaTrialValues)) {
    if (!codes.has(code)) {
      delete formulaTrialValues[code];
    }
  }
}

function getAvailableFormulaCodes(resultIndex: number) {
  const codes = [
    ...form.inputFields.map((item) => item.code.trim()),
    ...form.resultFields
      .slice(0, resultIndex)
      .map((item) => item.code.trim()),
  ].filter(Boolean);
  return [...new Set(codes)];
}

function getFormulaIdentifiers(formula: string) {
  return formula.match(/[A-Za-z_]\w*/g) || [];
}

function getFormulaFunctionNames(formula: string) {
  const names = [...formula.matchAll(/([A-Za-z_]\w*)\s*\(/g)].map(
    (item) => item[1]!,
  );
  for (const identifier of getFormulaIdentifiers(formula)) {
    if (UNSUPPORTED_FORMULA_KEYWORDS.has(identifier)) {
      names.push(identifier);
    }
  }
  return [...new Set(names)];
}

function hasChineseText(value: string) {
  return /[\u3400-\u9fff]/.test(value);
}

function getFormulaFieldNameUsage(
  formula: string,
  inputFields: MesQualityStandardApi.EntryRuleInputField[],
  resultFields: MesQualityStandardApi.EntryRuleResultField[],
) {
  const names = [...inputFields, ...resultFields]
    .map((item) => item.name?.trim())
    .filter((name): name is string => !!name && hasChineseText(name))
    .sort((a, b) => b.length - a.length);
  return names.find((name) => formula.includes(name));
}

function getUnsupportedFormulaChars(formula: string) {
  const chars = [...formula.matchAll(/[^A-Za-z0-9_\s+\-*/().,]/g)].map(
    (item) => item[0],
  );
  return [...new Set(chars)];
}

function validateFormulaDetail(
  result: MesQualityStandardApi.EntryRuleResultField,
  resultIndex: number,
  inputFields: MesQualityStandardApi.EntryRuleInputField[],
  resultFields: MesQualityStandardApi.EntryRuleResultField[],
) {
  const formula = result.formula || '';
  const resultName = result.name || result.code || `第 ${resultIndex + 1} 个结果`;
  const usedFieldName = getFormulaFieldNameUsage(
    formula,
    inputFields,
    resultFields,
  );
  if (usedFieldName) {
    return `结果“${resultName}”公式只能使用字段编码，不能使用字段名称：${usedFieldName}`;
  }

  const unsupportedFunction = getFormulaFunctionNames(formula).find(
    (name) => !FORMULA_FUNCTIONS.has(name),
  );
  if (unsupportedFunction) {
    return `结果“${resultName}”公式使用了不支持函数：${unsupportedFunction}`;
  }

  const unsupportedChars = getUnsupportedFormulaChars(formula);
  if (unsupportedChars.length > 0) {
    return `结果“${resultName}”存在不支持的公式字符：${unsupportedChars.join(' ')}`;
  }

  const inputCodes = new Set(inputFields.map((item) => item.code));
  const resultCodes = new Set(resultFields.map((item) => item.code));
  const availableCodes = new Set([
    ...inputCodes,
    ...resultFields.slice(0, resultIndex).map((item) => item.code),
  ]);
  for (const identifier of getFormulaIdentifiers(formula)) {
    if (FORMULA_FUNCTIONS.has(identifier)) continue;
    if (availableCodes.has(identifier)) continue;
    if (resultCodes.has(identifier)) {
      return `结果“${resultName}”公式引用了尚未定义的结果：${identifier}`;
    }
    return `结果“${resultName}”公式引用了不存在的编码：${identifier}`;
  }
  return '';
}

function getReferencedOptionalFieldError(
  inputFields: MesQualityStandardApi.EntryRuleInputField[],
  resultFields: MesQualityStandardApi.EntryRuleResultField[],
) {
  const referencedInputCodes = collectReferencedInputCodes(
    inputFields,
    resultFields,
  );
  const referencedOptionalField = inputFields.find(
    (field) => field.required === false && referencedInputCodes.has(field.code),
  );
  if (!referencedOptionalField) return '';
  return `字段“${referencedOptionalField.name}(${referencedOptionalField.code})”被结果公式引用，必须设为必填`;
}

function tokenizeFormula(formula: string) {
  const tokens: FormulaToken[] = [];
  let index = 0;
  while (index < formula.length) {
    const char = formula[index]!;
    if (/\s/.test(char)) {
      index += 1;
      continue;
    }
    if (/[+\-*/]/.test(char)) {
      tokens.push({ type: 'operator', value: char });
      index += 1;
      continue;
    }
    if (char === '(' || char === ')') {
      tokens.push({ type: 'paren', value: char });
      index += 1;
      continue;
    }
    if (char === ',') {
      tokens.push({ type: 'comma', value: char });
      index += 1;
      continue;
    }
    if (/[A-Za-z_]/.test(char)) {
      const start = index;
      index += 1;
      while (index < formula.length && /[A-Za-z0-9_]/.test(formula[index]!)) {
        index += 1;
      }
      tokens.push({
        type: 'identifier',
        value: formula.slice(start, index),
      });
      continue;
    }
    if (/[\d.]/.test(char)) {
      const start = index;
      index += 1;
      while (index < formula.length && /[\d.]/.test(formula[index]!)) {
        index += 1;
      }
      const value = formula.slice(start, index);
      if (!/^(?:\d+\.?\d*|\.\d+)$/.test(value)) {
        throw new Error(`公式语法错误：数字格式不合法 ${value}`);
      }
      tokens.push({ type: 'number', value });
      continue;
    }
    throw new Error(`不支持的公式字符：${char}`);
  }
  return tokens;
}

function applyFormulaFunction(name: string, args: number[]) {
  if (!FORMULA_FUNCTIONS.has(name)) {
    throw new Error(`公式使用了不支持函数：${name}`);
  }
  if ((name === 'avg' || name === 'std') && args.length === 0) {
    throw new Error(`函数 ${name} 至少需要一个参数`);
  }
  if (name === 'avg') {
    return args.reduce((sum, value) => sum + value, 0) / args.length;
  }
  if (name === 'std') {
    const avg = args.reduce((sum, value) => sum + value, 0) / args.length;
    const variance =
      args.reduce((sum, value) => sum + (value - avg) ** 2, 0) / args.length;
    return Math.sqrt(variance);
  }
  const [value, precisionValue = 0] = args;
  if (args.length < 1 || args.length > 2 || value === undefined) {
    throw new Error('函数 round 需要 1 到 2 个参数');
  }
  const precision = Math.trunc(precisionValue);
  if (precision < 0 || precision > 12) {
    throw new Error('函数 round 的精度需在 0 到 12 之间');
  }
  const factor = 10 ** precision;
  return Math.round(value * factor) / factor;
}

function evaluateFormulaExpression(
  formula: string,
  values: Record<string, number>,
  inputFields: MesQualityStandardApi.EntryRuleInputField[],
) {
  const tokens = tokenizeFormula(formula);
  let index = 0;
  const inputNames = new Map(
    inputFields.map((field) => [field.code, field.name] as const),
  );
  const peek = () => tokens[index];
  const consume = () => tokens[index++];

  const parseExpression = (): number => {
    let value = parseTerm();
    while (peek()?.value === '+' || peek()?.value === '-') {
      const operator = consume()!.value;
      const right = parseTerm();
      value = operator === '+' ? value + right : value - right;
    }
    return value;
  };

  const parseTerm = (): number => {
    let value = parseUnary();
    while (peek()?.value === '*' || peek()?.value === '/') {
      const operator = consume()!.value;
      const right = parseUnary();
      if (operator === '/' && right === 0) {
        throw new Error('公式试算失败：除数不能为 0');
      }
      value = operator === '*' ? value * right : value / right;
    }
    return value;
  };

  const parseUnary = (): number => {
    if (peek()?.value === '+') {
      consume();
      return parseUnary();
    }
    if (peek()?.value === '-') {
      consume();
      return -parseUnary();
    }
    return parsePrimary();
  };

  const parsePrimary = (): number => {
    const token = consume();
    if (!token) {
      throw new Error('公式语法错误：缺少运算项');
    }
    if (token.type === 'number') {
      return Number(token.value);
    }
    if (token.value === '(') {
      const value = parseExpression();
      if (consume()?.value !== ')') {
        throw new Error('公式语法错误：缺少右括号 )');
      }
      return value;
    }
    if (token.type !== 'identifier') {
      throw new Error(`公式语法错误：缺少运算项 ${token.value}`);
    }
    if (peek()?.value === '(') {
      consume();
      const args: number[] = [];
      if (peek()?.value !== ')') {
        while (true) {
          args.push(parseExpression());
          if (peek()?.value !== ',') break;
          consume();
        }
      }
      if (consume()?.value !== ')') {
        throw new Error(`函数 ${token.value} 缺少右括号 )`);
      }
      return applyFormulaFunction(token.value, args);
    }
    if (!Object.prototype.hasOwnProperty.call(values, token.value)) {
      const fieldName = inputNames.get(token.value);
      if (fieldName) {
        throw new Error(`请填写测试值：${fieldName}(${token.value})`);
      }
      throw new Error(`公式引用了不存在的编码：${token.value}`);
    }
    const value = values[token.value];
    if (!Number.isFinite(value)) {
      throw new Error(`编码 ${token.value} 的试算值不是有效数字`);
    }
    return value;
  };

  const value = parseExpression();
  if (index < tokens.length) {
    throw new Error(`公式语法错误：多余内容 ${tokens[index]!.value}`);
  }
  if (!Number.isFinite(value)) {
    throw new Error('公式试算结果不是有效数字');
  }
  return value;
}

function formatFormulaTrialValue(value: number, precision?: number) {
  const normalizedPrecision = Math.min(
    8,
    Math.max(0, Math.trunc(Number(precision ?? 3))),
  );
  return Number(value.toFixed(normalizedPrecision)).toString();
}

function resolveFormulaError(error: unknown) {
  return error instanceof Error ? error.message : '公式试算失败';
}

function handleRunFormulaTrial() {
  const inputFields = getNormalizedInputFields();
  const resultFields = getNormalizedResultFields();
  formulaTrialResults.value = [];
  formulaTrialError.value = '';

  if (inputFields.length === 0 || resultFields.length === 0) {
    formulaTrialError.value = '请先维护输入字段和结果定义';
    message.warning(formulaTrialError.value);
    return;
  }

  const values: Record<string, number> = {};
  for (const field of inputFields) {
    const rawValue = formulaTrialValues[field.code];
    if (rawValue === undefined || rawValue === null) continue;
    const value = Number(rawValue);
    if (!Number.isFinite(value)) {
      formulaTrialError.value = `测试值不是有效数字：${field.name}(${field.code})`;
      message.warning(formulaTrialError.value);
      return;
    }
    values[field.code] = value;
  }

  const optionalFieldError = getReferencedOptionalFieldError(
    inputFields,
    resultFields,
  );
  if (optionalFieldError) {
    formulaTrialError.value = optionalFieldError;
    message.warning(formulaTrialError.value);
    return;
  }

  try {
    const results: FormulaTrialResult[] = [];
    for (const [index, result] of resultFields.entries()) {
      const formulaError = validateFormulaDetail(
        result,
        index,
        inputFields,
        resultFields,
      );
      if (formulaError) {
        throw new Error(formulaError);
      }
      const value = evaluateFormulaExpression(
        result.formula,
        values,
        inputFields,
      );
      values[result.code] = value;
      results.push({
        code: result.code,
        name: result.name,
        value: formatFormulaTrialValue(value, result.precision),
      });
    }
    formulaTrialResults.value = results;
    message.success('公式试算完成');
  } catch (error) {
    formulaTrialError.value = resolveFormulaError(error);
    message.warning(formulaTrialError.value);
  }
}

function collectReferencedInputCodes(
  inputFields: MesQualityStandardApi.EntryRuleInputField[],
  resultFields: MesQualityStandardApi.EntryRuleResultField[],
) {
  const inputCodes = new Set(inputFields.map((item) => item.code));
  const referenced = new Set<string>();
  for (const result of resultFields) {
    for (const identifier of getFormulaIdentifiers(result.formula || '')) {
      if (inputCodes.has(identifier)) {
        referenced.add(identifier);
      }
    }
  }
  return referenced;
}

function isInputFieldReferencedByFormula(code?: string) {
  const normalizedCode = code?.trim();
  if (!normalizedCode) return false;
  return collectReferencedInputCodes(
    getNormalizedInputFields(),
    getNormalizedResultFields(),
  ).has(normalizedCode);
}

function isInputRequiredLocked(field: InputFieldRule) {
  return (
    isInputFieldReferencedByFormula(field.code) && field.required !== false
  );
}

function resolvePositionCode(position: PositionRule, index: number) {
  const code = position.code?.trim();
  if (code) return code;
  return `P${String(index + 1).padStart(2, '0')}`;
}

function getNormalizedPositions() {
  return clonePositions(form.positions)
    .map((item, index) => ({
      ...item,
      code: resolvePositionCode(item, index),
      name: item.name.trim(),
      required: true,
      sort: index + 1,
    }))
    .filter((item) => item.name)
    .sort((a, b) => a.sort - b.sort);
}

function getNormalizedInputFields() {
  return form.inputFields
    .map((item) => ({
      code: item.code.trim(),
      name: item.name.trim(),
      unit: item.unit?.trim() || '',
      type: item.type || 'NUMBER',
      required: item.required !== false,
      precision: Number(item.precision ?? 3),
    }))
    .filter((item) => item.code && item.name);
}

function getNormalizedResultFields() {
  return form.resultFields
    .map((item) => ({
      code: item.code.trim(),
      name: item.name.trim(),
      formula: item.formula.trim(),
      unit: item.unit?.trim() || '',
      judgment: form.judgmentMetric
        ? item.code.trim() === form.judgmentMetric
        : !!item.judgment,
      precision: Number(item.precision ?? 3),
    }))
    .filter((item) => item.code && item.name && item.formula);
}

function isSequentialPositionLayout(
  positions: PositionRule[],
  repeatCount: number,
) {
  if (repeatCount !== 1 || positions.length < 2) return false;
  const values = positions.map((item) => Number(item.name));
  return values.every((value, index) => {
    if (!Number.isInteger(value) || value <= 0) return false;
    return index === 0 || value === values[index - 1]! + 1;
  });
}

function resolveDisplayLayout(positions: PositionRule[], repeatCount: number) {
  if (isSequentialPositionLayout(positions, repeatCount))
    return 'SEQUENCE_15_GRID';
  if (positions.length === 3 && repeatCount === 1) return 'THREE_POINT_GRID';
  if (positions.length === 3 && repeatCount === 5)
    return 'THREE_POINT_FIVE_REPEAT_GRID';
  return 'POSITION_REPEAT_GRID';
}

function buildTemplateParams() {
  if (isQualitative.value) {
    return {
      displayLayout: 'QUALITATIVE_JUDGMENT',
      options: getQualitativeOptions(),
      ngRemarkRequired: form.ngRemarkRequired,
      sampleSize: Math.max(1, Number(form.sampleSize || 1)),
    };
  }
  const positions = getNormalizedPositions();
  const repeatCount = Math.max(1, Number(form.repeatCount || 1));
  const displayLayout = resolveDisplayLayout(positions, repeatCount);
  const inputFields = getNormalizedInputFields();
  const resultFields = getNormalizedResultFields();
  const judgmentMetric =
    form.judgmentMetric || resultFields.find((item) => item.judgment)?.code;
  const valueTemplate = resolveValueTemplate(inputFields, resultFields);
  const params = {
    positions,
    repeatCount,
    sampleSize: positions.length * repeatCount,
    stdRule: DEFAULT_STD_RULE,
    displayLayout,
    valueTemplate,
    judgmentMetric,
    dataRule: {
      inputFields,
      resultFields,
      judgmentMetric,
    },
  };
  return valueTemplate === 'COMPRESSION_CALC'
    ? buildCompressionTemplateParams(activeItemRow.value, params)
    : params;
}

function resolveValueTemplate(
  inputFields: MesQualityStandardApi.EntryRuleInputField[],
  resultFields: MesQualityStandardApi.EntryRuleResultField[],
) {
  const inputCodes = inputFields.map((item) => item.code).join(',');
  const resultCodes = resultFields.map((item) => item.code).join(',');
  if (
    inputCodes.includes('thicknessMm') &&
    inputCodes.includes('weightG') &&
    resultCodes.includes('densityValue')
  ) {
    return 'DENSITY_CALC';
  }
  if (
    inputCodes.includes('t1Mm') &&
    inputCodes.includes('t2Mm') &&
    resultCodes.includes('compressionRate')
  ) {
    return 'COMPRESSION_CALC';
  }
  return 'SINGLE_VALUE';
}

function generateSummary() {
  if (isQualitative.value) {
    const options = getQualitativeOptions();
    const optionText = options.length > 0 ? options.join('/') : 'OK/NG';
    return `定性 ${optionText}${form.ngRemarkRequired ? '，NG 必填备注' : ''}`;
  }
  const positionSummary = generatePositionSummary();
  const inputFields = getNormalizedInputFields();
  const resultFields = getNormalizedResultFields();
  if (inputFields.length > 0 && resultFields.length > 0) {
    const repeatCount = Math.max(1, Number(form.repeatCount || 1));
    const isSingleMeasuredValue =
      getNormalizedPositions().length === 1 &&
      getNormalizedPositions()[0]?.name === '实测' &&
      inputFields.length === 1 &&
      inputFields[0]?.code === 'value' &&
      resultFields.length === 1 &&
      resultFields[0]?.code === 'resultValue' &&
      form.judgmentMetric === 'resultValue';
    if (isSingleMeasuredValue) {
      return `单值实测，${repeatCount} 组`;
    }
    const inputText = formatRuleFieldNames(
      inputFields.map((item) => item.name),
    );
    const resultText = formatRuleFieldNames(
      resultFields.map((item) => item.name),
    );
    const judgmentField =
      resultFields.find((item) => item.code === form.judgmentMetric) ||
      resultFields.find((item) => item.judgment) ||
      resultFields[0];
    return `${positionSummary}录入${inputText}，系统计算${resultText}，按${judgmentField.name}判定`;
  }
  return `${positionSummary}录入`;
}

function generatePositionSummary() {
  const positions = getNormalizedPositions();
  if (positions.length === 0) return '';
  const repeatCount = Math.max(1, Number(form.repeatCount || 1));
  if (resolveDisplayLayout(positions, repeatCount) === 'SEQUENCE_15_GRID') {
    return `按 ${positions[0]?.name || '1'}-${
      positions[positions.length - 1]?.name || positions.length
    } 序号位置`;
  }
  const positionText = positions.map((item) => item.name).join('/');
  if (repeatCount === 1) {
    return `按 ${positionText} ${positions.length} 点`;
  }
  return `按 ${positionText} ${positions.length} 点，每个位置 ${repeatCount} 组`;
}

function formatRuleFieldNames(names: string[]) {
  const values = names.map((item) => item?.trim()).filter(Boolean);
  if (values.length <= 1) {
    return values[0] || '';
  }
  if (values.length === 2) {
    return values.join('和');
  }
  return `${values.slice(0, -1).join('、')}和${values[values.length - 1]}`;
}

function normalizeSummaryText(value?: string) {
  return (value || '').trim();
}

function isCurrentSummaryManual(value?: string) {
  const current = normalizeSummaryText(value);
  const generated = normalizeSummaryText(generateSummary());
  const presetSummary = normalizeSummaryText(
    entryRulePresets[form.presetRule]?.summary,
  );
  return !!current && current !== generated && current !== presetSummary;
}

function syncGeneratedSummary(force = false) {
  if (!force && (summaryAutoSyncPaused.value || isManualSummary.value)) return;
  setSummaryValue(generateSummary(), false);
}

function handleSummaryInput(value: string) {
  form.testFrequencyJudgement = value;
  isManualSummary.value =
    normalizeSummaryText(value) !== normalizeSummaryText(generateSummary());
}

function handleRegenerateSummary() {
  syncGeneratedSummary(true);
}

function validateConfig() {
  if (isQualitative.value) {
    if (getQualitativeOptions().length === 0) {
      message.warning('请至少维护一个判定选项');
      return false;
    }
    return true;
  }

  const positions = getNormalizedPositions();
  if (positions.length === 0) {
    message.warning('请至少维护一个位置');
    return false;
  }
  const names = new Set<string>();
  for (const position of positions) {
    if (names.has(position.name)) {
      message.warning('位置名称不能重复');
      return false;
    }
    names.add(position.name);
  }
  if (Number(form.repeatCount || 0) <= 0) {
    message.warning('每个位置录入组数必须大于 0');
    return false;
  }
  const inputFields = getNormalizedInputFields();
  const resultFields = getNormalizedResultFields();
  if (inputFields.length === 0) {
    message.warning('请至少维护一个输入字段');
    return false;
  }
  if (resultFields.length === 0) {
    message.warning('请至少维护一个结果定义');
    return false;
  }
  const fieldCodes = new Set<string>();
  for (const field of inputFields) {
    if (!validateFieldCode(field.code)) {
      message.warning(`输入字段编码不合法：${field.code}`);
      return false;
    }
    if (fieldCodes.has(field.code)) {
      message.warning(`输入字段编码重复：${field.code}`);
      return false;
    }
    fieldCodes.add(field.code);
  }
  let hasJudgment = false;
  for (const [index, result] of resultFields.entries()) {
    if (!validateFieldCode(result.code)) {
      message.warning(`结果编码不合法：${result.code}`);
      return false;
    }
    if (fieldCodes.has(result.code)) {
      message.warning(`字段或结果编码重复：${result.code}`);
      return false;
    }
    const formulaError = validateFormulaDetail(
      result,
      index,
      inputFields,
      resultFields,
    );
    if (formulaError) {
      message.warning(formulaError);
      return false;
    }
    fieldCodes.add(result.code);
    hasJudgment = hasJudgment || !!result.judgment;
  }
  const optionalFieldError = getReferencedOptionalFieldError(
    inputFields,
    resultFields,
  );
  if (optionalFieldError) {
    message.warning(optionalFieldError);
    return false;
  }
  if (!hasJudgment) {
    message.warning('请选择一个结果作为判定指标');
    return false;
  }
  return true;
}

function handleConfirm() {
  if (readonlyState.value) {
    openState.value = false;
    return;
  }
  if (!validateConfig()) return;

  const params = buildTemplateParams();
  const sampleSize = Number(params.sampleSize || 1);
  const summary = form.testFrequencyJudgement;
  const valueTemplate = isQualitative.value
    ? undefined
    : (params as any).valueTemplate || 'SINGLE_VALUE';
  const judgmentMetric = isQualitative.value
    ? undefined
    : (params as any).judgmentMetric || 'resultValue';
  const selectedTemplate = selectedSavedTemplate.value;
  emit('confirm', {
    entryRuleType: form.entryRuleType,
    entryRuleTemplateId: form.savedTemplateId,
    entryRuleTemplateName:
      form.savedTemplateId && selectedTemplate
        ? selectedTemplate.templateName
        : form.savedTemplateId
          ? form.templateName.trim()
          : undefined,
    testFrequencyJudgement: summary,
    templateParams: JSON.stringify(params),
    ruleDescription: form.ruleDescription,
    sampleSize,
    valueTemplate,
    judgmentMetric,
  });
  openState.value = false;
}

async function handleSaveTemplate() {
  if (readonlyState.value) return;
  if (!validateConfig()) return;
  if (!form.templateName.trim()) {
    message.warning('请填写模板名称');
    return;
  }
  const params = buildTemplateParams();
  const sampleSize = Number(params.sampleSize || 1);
  const summary = form.testFrequencyJudgement;
  const id = await createEntryRuleTemplate({
    templateName: form.templateName.trim(),
    itemType: itemType.value,
    testFrequencyJudgement: summary,
    templateParams: JSON.stringify(params),
    ruleDescription: form.ruleDescription,
    sampleSize,
  });
  await loadTemplateOptions();
  form.savedTemplateId = id;
  form.selectedTemplateValue = id ? `CUSTOM:${id}` : undefined;
  form.entryRuleType = 'CUSTOM';
  message.success('规则模板已保存；点击“确定并应用”后会写入当前检验项');
}

async function handleUpdateTemplate() {
  if (readonlyState.value || !form.savedTemplateId) return;
  if (!validateConfig()) return;
  if (!form.templateName.trim()) {
    message.warning('请填写模板名称');
    return;
  }
  const params = buildTemplateParams();
  const sampleSize = Number(params.sampleSize || 1);
  const summary = form.testFrequencyJudgement;
  await updateEntryRuleTemplate({
    id: form.savedTemplateId,
    templateName: form.templateName.trim(),
    itemType: itemType.value,
    testFrequencyJudgement: summary,
    templateParams: JSON.stringify(params),
    ruleDescription: form.ruleDescription,
    sampleSize,
  });
  await loadTemplateOptions();
  form.selectedTemplateValue = `CUSTOM:${form.savedTemplateId}`;
  message.success('规则模板已更新');
}

function handleDeleteTemplate() {
  const template = selectedSavedTemplate.value;
  if (readonlyState.value || !template?.id) return;
  Modal.confirm({
    title: '删除规则模板',
    content: `确认删除模板“${template.templateName}”吗？已使用该模板的检验项不会被清空。`,
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      await deleteEntryRuleTemplate(template.id!);
      await loadTemplateOptions();
      form.savedTemplateId = undefined;
      form.selectedTemplateValue = undefined;
      form.templateName = '';
      message.success('规则模板已删除');
    },
  });
}

defineExpose({ open });
</script>

<template>
  <Modal
    v-model:open="openState"
    :width="modalWidth"
    :style="modalStyle"
    :ok-text="readonlyState ? '关闭' : '确定并应用'"
    @ok="handleConfirm"
  >
    <template #title>
      <div class="qms-entry-rule-modal__title">
        <span>{{ readonlyState ? '查看位置/录入规则' : '配置位置/录入规则' }}</span>
        <Button
          type="text"
          size="small"
          :title="fullscreenState ? '还原窗口' : '全屏展开'"
          @click.stop="toggleFullscreen"
        >
          <IconifyIcon
            :icon="fullscreenState ? 'lucide:minimize-2' : 'lucide:maximize-2'"
          />
        </Button>
      </div>
    </template>
    <div
      class="qms-entry-rule-modal__body space-y-4"
      :class="{ 'is-fullscreen': fullscreenState }"
    >
      <div class="grid grid-cols-2 gap-4">
        <div>
          <div class="mb-1 text-xs text-slate-500">规则来源</div>
          <RadioGroup
            v-model:value="form.entryRuleType"
            :disabled="readonlyState"
            :options="[
              { label: '预设规则', value: 'PRESET' },
              { label: '自定义规则', value: 'CUSTOM' },
            ]"
            @change="handleEntryRuleTypeChange"
          />
        </div>
        <div>
          <div class="mb-1 text-xs text-slate-500">规则模板</div>
          <Select
            v-model:value="form.selectedTemplateValue"
            class="w-full"
            allow-clear
            show-search
            :disabled="readonlyState"
            :options="ruleTemplateOptions"
            placeholder="选择预设模板或已保存模板"
            @change="handleRuleTemplateChange"
          />
        </div>
      </div>

      <div class="grid grid-cols-[1fr_auto_auto_auto] items-end gap-3">
        <div>
          <div class="mb-1 text-xs text-slate-500">复用模板名称</div>
          <Input
            v-model:value="form.templateName"
            :disabled="readonlyState"
            placeholder="如：硬度五点三组"
          />
        </div>
        <Button
          v-if="!readonlyState"
          :disabled="form.entryRuleType !== 'CUSTOM'"
          @click="handleSaveTemplate"
        >
          <IconifyIcon icon="lucide:save" class="mr-1" />
          保存为复用模板
        </Button>
        <Button
          v-if="!readonlyState"
          :disabled="form.entryRuleType !== 'CUSTOM' || !form.savedTemplateId"
          @click="handleUpdateTemplate"
        >
          <IconifyIcon icon="lucide:save-all" class="mr-1" />
          更新模板
        </Button>
        <Button
          v-if="!readonlyState"
          danger
          :disabled="form.entryRuleType !== 'CUSTOM' || !form.savedTemplateId"
          @click="handleDeleteTemplate"
        >
          <IconifyIcon icon="lucide:trash-2" class="mr-1" />
          删除模板
        </Button>
      </div>

      <div v-if="!isQualitative" class="space-y-3">
        <div class="flex items-center justify-between">
          <div class="text-sm font-semibold text-slate-700">位置列表</div>
          <Button
            v-if="!readonlyState && form.entryRuleType === 'CUSTOM'"
            size="small"
            @click="handleAddPosition"
          >
            <IconifyIcon icon="lucide:plus" class="mr-1" />
            添加位置
          </Button>
        </div>
        <div class="space-y-2">
          <div class="px-1 text-xs text-slate-500">位置名称</div>
          <div
            v-for="(position, index) in form.positions"
            :key="index"
            class="grid grid-cols-[1fr_auto] items-center gap-2"
          >
            <Input
              v-model:value="position.name"
              :disabled="readonlyState || form.entryRuleType === 'PRESET'"
              placeholder="位置名称"
            />
            <Button
              v-if="!readonlyState && form.entryRuleType === 'CUSTOM'"
              danger
              type="text"
              size="small"
              title="移除位置"
              @click="handleRemovePosition(index)"
            >
              <IconifyIcon icon="lucide:trash-2" />
            </Button>
          </div>
        </div>

        <div class="grid grid-cols-[220px_1fr] gap-4">
          <div>
            <div class="mb-1 text-xs text-slate-500">每个位置录入组数</div>
            <InputNumber
              v-model:value="form.repeatCount"
              class="w-full"
              :disabled="readonlyState || form.entryRuleType === 'PRESET'"
              :min="1"
            />
          </div>
          <div>
            <div class="mb-1 text-xs text-slate-500">自动检测数</div>
            <div
              class="flex h-8 items-center rounded border border-slate-200 bg-slate-50 px-3 text-sm text-slate-700"
            >
              检测数 = {{ quantitativePositionCount }} 个位置 ×
              {{ quantitativeRepeatCount }} 组/位置 =
              {{ quantitativeSampleSize }} 组
            </div>
          </div>
        </div>
      </div>

      <div v-else class="grid grid-cols-2 gap-4">
        <div>
          <div class="mb-1 text-xs text-slate-500">判定选项</div>
          <Input
            v-model:value="form.optionsText"
            :disabled="readonlyState || form.entryRuleType === 'PRESET'"
            placeholder="例如 OK,NG,NA"
          />
        </div>
        <div>
          <div class="mb-1 text-xs text-slate-500">NG 备注必填</div>
          <Switch
            v-model:checked="form.ngRemarkRequired"
            :disabled="readonlyState || form.entryRuleType === 'PRESET'"
            checked-children="是"
            un-checked-children="否"
          />
        </div>
      </div>

      <Collapse
        v-if="!isQualitative"
        v-model:active-key="advancedRuleActiveKeys"
        class="rounded border bg-white"
        :bordered="false"
      >
        <Collapse.Panel key="dataRule">
          <template #header>
            <div class="flex items-center justify-between pr-3">
              <span class="text-sm font-semibold text-slate-700">
                高级数据处理规则
              </span>
              <span class="text-xs text-slate-500">
                字段/结果编码公式，可校验试算
              </span>
            </div>
          </template>

          <div class="space-y-3">
            <div class="space-y-2">
              <div class="flex items-center justify-between">
                <div class="text-xs font-bold text-slate-500">字段定义表</div>
                <Button
                  v-if="!readonlyState && form.entryRuleType === 'CUSTOM'"
                  size="small"
                  @click="handleAddInputField"
                >
                  <IconifyIcon icon="lucide:plus" class="mr-1" />
                  添加字段
                </Button>
              </div>
              <div class="px-1 text-xs text-slate-500">
                字段编码用于公式计算，字段名称用于检验员录入展示。
              </div>
              <div
                class="grid grid-cols-[1.2fr_1.2fr_.8fr_.8fr_.8fr_.7fr_auto] gap-2 px-1 text-xs text-slate-500"
              >
                <span>字段编码</span>
                <span>字段名称</span>
                <span>单位</span>
                <span>类型</span>
                <span>必填</span>
                <span>精度</span>
                <span></span>
              </div>
              <div
                v-for="(field, index) in form.inputFields"
                :key="`input-${index}`"
                class="grid grid-cols-[1.2fr_1.2fr_.8fr_.8fr_.8fr_.7fr_auto] items-center gap-2"
              >
                <Input
                  v-model:value="field.code"
                  :disabled="readonlyState || form.entryRuleType === 'PRESET'"
                  size="small"
                />
                <Input
                  v-model:value="field.name"
                  :disabled="readonlyState || form.entryRuleType === 'PRESET'"
                  size="small"
                />
                <Input
                  v-model:value="field.unit"
                  :disabled="readonlyState || form.entryRuleType === 'PRESET'"
                  size="small"
                />
                <Select
                  v-model:value="field.type"
                  :disabled="readonlyState || form.entryRuleType === 'PRESET'"
                  :options="[{ label: '数值', value: 'NUMBER' }]"
                  size="small"
                />
                <Switch
                  v-model:checked="field.required"
                  :disabled="
                    readonlyState ||
                    form.entryRuleType === 'PRESET' ||
                    isInputRequiredLocked(field)
                  "
                  size="small"
                />
                <InputNumber
                  v-model:value="field.precision"
                  :disabled="readonlyState || form.entryRuleType === 'PRESET'"
                  :min="0"
                  :max="8"
                  size="small"
                  class="w-full"
                />
                <Button
                  v-if="!readonlyState && form.entryRuleType === 'CUSTOM'"
                  danger
                  type="text"
                  size="small"
                  @click="handleRemoveInputField(index)"
                >
                  <IconifyIcon icon="lucide:trash-2" />
                </Button>
              </div>
            </div>

            <div class="space-y-2">
              <div class="flex items-center justify-between">
                <div class="text-xs font-bold text-slate-500">结果定义表</div>
                <Button
                  v-if="!readonlyState && form.entryRuleType === 'CUSTOM'"
                  size="small"
                  @click="handleAddResultField"
                >
                  <IconifyIcon icon="lucide:plus" class="mr-1" />
                  添加结果
                </Button>
              </div>
              <div
                class="flex flex-wrap gap-x-4 gap-y-1 px-1 text-xs text-slate-500"
              >
                <span>
                  支持：字段编码、已定义结果编码、数字、+ - * / ( )、avg、std、round。
                </span>
                <span>
                  不支持：中文字段名、= 号、比较符、if/else、百分号。
                </span>
              </div>
              <div
                class="grid grid-cols-[1fr_1fr_2fr_.8fr_.9fr_.7fr_auto] gap-2 px-1 text-xs text-slate-500"
              >
                <span>结果编码</span>
                <span>结果名称</span>
                <span>公式</span>
                <span>单位</span>
                <span>判定指标</span>
                <span>精度</span>
                <span></span>
              </div>
              <div
                v-for="(result, index) in form.resultFields"
                :key="`result-${index}`"
                class="grid grid-cols-[1fr_1fr_2fr_.8fr_.9fr_.7fr_auto] items-start gap-2"
              >
                <Input
                  v-model:value="result.code"
                  :disabled="readonlyState || form.entryRuleType === 'PRESET'"
                  size="small"
                />
                <Input
                  v-model:value="result.name"
                  :disabled="readonlyState || form.entryRuleType === 'PRESET'"
                  size="small"
                />
                <div class="min-w-0 space-y-1">
                  <Input
                    v-model:value="result.formula"
                    :disabled="readonlyState || form.entryRuleType === 'PRESET'"
                    size="small"
                  />
                  <div
                    class="flex flex-wrap items-center gap-1 text-[11px] text-slate-500"
                  >
                    <span>当前可引用编码</span>
                    <template v-if="getAvailableFormulaCodes(index).length > 0">
                      <Tag
                        v-for="code in getAvailableFormulaCodes(index)"
                        :key="`${index}-${code}`"
                        class="m-0"
                      >
                        {{ code }}
                      </Tag>
                    </template>
                    <span v-else>暂无</span>
                  </div>
                </div>
                <Input
                  v-model:value="result.unit"
                  :disabled="readonlyState || form.entryRuleType === 'PRESET'"
                  size="small"
                />
                <Radio
                  :checked="form.judgmentMetric === result.code"
                  :disabled="
                    readonlyState ||
                    form.entryRuleType === 'PRESET' ||
                    !result.code?.trim()
                  "
                  @change="form.judgmentMetric = result.code"
                />
                <InputNumber
                  v-model:value="result.precision"
                  :disabled="readonlyState || form.entryRuleType === 'PRESET'"
                  :min="0"
                  :max="8"
                  size="small"
                  class="w-full"
                />
                <Button
                  v-if="!readonlyState && form.entryRuleType === 'CUSTOM'"
                  danger
                  type="text"
                  size="small"
                  @click="handleRemoveResultField(index)"
                >
                  <IconifyIcon icon="lucide:trash-2" />
                </Button>
              </div>
            </div>

            <div class="space-y-2 border-t border-slate-100 pt-2">
              <div class="flex items-center justify-between">
                <div class="text-xs font-bold text-slate-500">公式试算</div>
                <Button
                  size="small"
                  :disabled="
                    formulaTrialInputFields.length === 0 ||
                    form.resultFields.length === 0
                  "
                  @click="handleRunFormulaTrial"
                >
                  <IconifyIcon icon="lucide:calculator" class="mr-1" />
                  试算
                </Button>
              </div>
              <div
                v-if="formulaTrialInputFields.length > 0"
                class="grid grid-cols-3 gap-2"
              >
                <div
                  v-for="field in formulaTrialInputFields"
                  :key="`trial-${field.code}`"
                  class="min-w-0"
                >
                  <div class="mb-1 truncate text-[11px] text-slate-500">
                    {{ field.name }}（{{ field.code }}）
                  </div>
                  <InputNumber
                    v-model:value="formulaTrialValues[field.code]"
                    class="w-full"
                    size="small"
                    placeholder="测试值"
                  />
                </div>
              </div>
              <div v-else class="px-1 text-xs text-slate-400">
                先维护输入字段后可试算。
              </div>
              <div
                v-if="formulaTrialError"
                class="rounded border border-red-100 bg-red-50 px-2 py-1 text-xs text-red-600"
              >
                {{ formulaTrialError }}
              </div>
              <div
                v-if="formulaTrialResults.length > 0"
                class="flex flex-wrap gap-2 text-xs"
              >
                <span
                  v-for="result in formulaTrialResults"
                  :key="`trial-result-${result.code}`"
                  class="rounded border border-slate-200 bg-slate-50 px-2 py-1 text-slate-700"
                >
                  {{ result.name }}（{{ result.code }}）：{{ result.value }}
                </span>
              </div>
            </div>
          </div>
        </Collapse.Panel>
      </Collapse>

      <div
        :class="
          isQualitative
            ? 'grid grid-cols-[1fr_140px] gap-4'
            : 'grid grid-cols-1 gap-4'
        "
      >
        <div>
          <div class="mb-1 flex items-center justify-between gap-2">
            <div class="text-xs text-slate-500">位置/录入规则摘要</div>
            <div class="flex items-center gap-2">
              <Tag v-if="isManualSummary" color="orange" class="m-0">
                手动摘要
              </Tag>
              <Button
                v-if="!readonlyState"
                type="link"
                size="small"
                class="px-0"
                @click="handleRegenerateSummary"
              >
                <IconifyIcon icon="lucide:refresh-cw" class="mr-1" />
                重新生成摘要
              </Button>
            </div>
          </div>
          <Input
            :value="form.testFrequencyJudgement"
            :disabled="readonlyState"
            :placeholder="generateSummary() || '系统将根据配置自动生成摘要'"
            @update:value="handleSummaryInput"
          />
        </div>
        <div v-if="isQualitative">
          <div class="mb-1 text-xs text-slate-500">检测数</div>
          <InputNumber
            v-model:value="form.sampleSize"
            class="w-full"
            :disabled="readonlyState"
            :min="1"
          />
        </div>
      </div>

      <div>
        <div class="mb-1 text-xs text-slate-500">规则说明</div>
        <Textarea
          v-model:value="form.ruleDescription"
          :disabled="readonlyState"
          :maxlength="1000"
          :rows="4"
          show-count
          placeholder="填写给检验员查看的现场执行说明"
        />
      </div>
    </div>
  </Modal>
</template>

<style scoped>
.qms-entry-rule-modal__title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-right: 28px;
}

.qms-entry-rule-modal__body {
  max-height: min(720px, calc(100vh - 220px));
  overflow-x: hidden;
  overflow-y: auto;
  padding-right: 4px;
}

.qms-entry-rule-modal__body.is-fullscreen {
  height: calc(100vh - 170px);
  max-height: calc(100vh - 170px);
}
</style>
