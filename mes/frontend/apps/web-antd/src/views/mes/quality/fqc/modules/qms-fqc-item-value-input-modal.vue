<script lang="ts" setup>
import type { MesFqcApi } from '#/api/mes/quality/fqc';

import { computed, nextTick, onMounted, ref, toRaw, watch } from 'vue';

import { IconifyIcon } from '@vben/icons';

import {
  Button,
  Checkbox,
  Image,
  Input,
  InputNumber,
  message,
  Modal,
  Radio,
  Tag,
} from 'ant-design-vue';

import {
  parseEntryRuleParams,
  resolveEntryRuleExpectedSampleCount,
  resolveEntryRulePositions,
  resolveEntryRuleRepeatCount,
  resolveEntryRuleSampleSize,
} from '#/api/mes/quality/entry-rule';
import { getFqcDefectCodeOptions } from '#/api/mes/quality/fqc';

defineOptions({ name: 'QmsFqcItemValueInputModal' });

const props = defineProps<{
  entryGroupItems?: MesFqcApi.FqcItem[];
  item?: MesFqcApi.FqcItem;
  itemCount?: number;
  itemIndex?: number;
  open: boolean;
  readonly?: boolean;
  record?: MesFqcApi.FqcRecord | null;
  saving?: boolean;
}>();

const emit = defineEmits<{
  recalculate: [item: MesFqcApi.FqcItem];
  save: [item: MesFqcApi.FqcItem, closeAfterSave?: boolean];
  'update:open': [open: boolean];
}>();

type EditableSample = Record<string, any>;

interface DefectCodeOption {
  causes?: MesFqcApi.FqcDefectCodeOption['causes'];
  defectCode: string;
  defectLevel?: string;
  defectName: string;
  label: string;
  referencePicUrls?: string[];
  remark?: string;
  value: number;
}

interface DataRuleInputField {
  code: string;
  name: string;
  unit?: string;
  required?: boolean;
  precision?: number;
}

interface DataRuleResultField {
  code: string;
  name: string;
  formula: string;
  unit?: string;
  judgment?: boolean;
  precision?: number;
}

interface DataRuleConfig {
  inputFields: DataRuleInputField[];
  resultFields: DataRuleResultField[];
  judgmentMetric?: string;
}

const localItem = ref<MesFqcApi.FqcItem>();
const defectCodeOptions = ref<DefectCodeOption[]>([]);
const defectPickerOpen = ref(false);
const defectPickerKeyword = ref('');
const defectPickerRow = ref<EditableSample | null>(null);
const defectPickerSelectedIds = ref<number[]>([]);
const highlightedDefectCodeId = ref<number>();
const modalRootRef = ref<HTMLElement | null>(null);
const entryMessageReady = ref(false);
const [entryMessage, entryMessageHolder] = message.useMessage({
  staticGetContainer: () => modalRootRef.value || document.body,
  top: '24px',
} as any);

onMounted(loadDefectCodeOptions);

watch(
  () => props.open,
  async (isOpen) => {
    if (!isOpen) {
      entryMessageReady.value = false;
      return;
    }
    await nextTick();
    entryMessageReady.value = true;
  },
  { flush: 'post' },
);

async function loadDefectCodeOptions() {
  const list = await getFqcDefectCodeOptions();
  defectCodeOptions.value = (list || [])
    .filter((item) => item.type === 'ITEM' && item.status === 1 && item.id)
    .map((item) => ({
      causes: item.causes,
      defectCode: item.code,
      defectLevel: item.level,
      defectName: item.name,
      label: `${item.code} - ${item.name}`,
      referencePicUrls: item.referencePicUrls,
      remark: item.remark,
      value: item.id,
    }));
}

function cloneFqcItem(item: MesFqcApi.FqcItem): MesFqcApi.FqcItem {
  return structuredClone(toPlainValue(item)) as MesFqcApi.FqcItem;
}

function toPlainValue(value: unknown): unknown {
  if (value === null || typeof value !== 'object') return value;

  const raw = toRaw(value);
  if (raw instanceof Date) return raw.toISOString();
  if (Array.isArray(raw)) return raw.map((item) => toPlainValue(item));

  const prototype = Object.getPrototypeOf(raw);
  if (prototype !== Object.prototype && prototype !== null) return undefined;

  const plain: Record<string, unknown> = {};
  for (const [key, child] of Object.entries(raw as Record<string, unknown>)) {
    if (typeof child !== 'function' && typeof child !== 'symbol') {
      plain[key] = toPlainValue(child);
    }
  }
  return plain;
}

watch(
  () => [props.item, props.open] as const,
  () => {
    if (!props.open || !props.item) return;
    const cloned = cloneFqcItem(props.item);
    cloned.qaValues = buildEditableValues(cloned);
    localItem.value = cloned;
  },
  { immediate: true },
);

const repeatCount = computed(() =>
  localItem.value ? resolveEntryRuleRepeatCount(localItem.value) : 1,
);

const currentItemIndex = computed(() =>
  props.itemIndex && props.itemIndex > 0 ? props.itemIndex : 1,
);

const currentItemCount = computed(() =>
  props.itemCount && props.itemCount > 0 ? props.itemCount : 1,
);

const positionRows = computed(() => {
  const item = localItem.value;
  if (!item) return [];
  ensureEditableValues(item);
  const itemRepeatCount = resolveEntryRuleRepeatCount(item);
  return resolveEntryRulePositions(item).map((position, positionIndex) => ({
    ...position,
    values: Array.from({ length: itemRepeatCount }).map(
      (_, offset) => item.qaValues![positionIndex * itemRepeatCount + offset],
    ),
  }));
});

function resolveDataRule(item: MesFqcApi.FqcItem): DataRuleConfig | undefined {
  const params = parseEntryRuleParams(item.templateParams);
  const dataRule = params.dataRule || params;
  const inputFields = Array.isArray(dataRule.inputFields)
    ? dataRule.inputFields
    : [];
  const resultFields = Array.isArray(dataRule.resultFields)
    ? dataRule.resultFields
    : [];
  if (inputFields.length === 0 || resultFields.length === 0) return undefined;
  return {
    inputFields: inputFields.map((field: any) => ({
      code: field.code,
      name: field.name || field.code,
      unit: field.unit || '',
      required: field.required !== false,
      precision: Number(field.precision ?? 3),
    })),
    resultFields: resultFields.map((field: any) => ({
      code: field.code,
      name: field.name || field.code,
      formula: field.formula || '',
      unit: field.unit || '',
      judgment: !!field.judgment,
      precision: Number(field.precision ?? 3),
    })),
    judgmentMetric:
      dataRule.judgmentMetric ||
      resultFields.find((field: any) => field.judgment)?.code,
  };
}

function isDataRuleItem(item: MesFqcApi.FqcItem) {
  return item.itemType === 'QUANTITATIVE' && !!resolveDataRule(item);
}

function resolveExpectedSampleCount(item: MesFqcApi.FqcItem) {
  return resolveEntryRuleExpectedSampleCount(item);
}

function buildEditableValues(item: MesFqcApi.FqcItem): EditableSample[] {
  const source = Array.isArray(item.qaValues) ? item.qaValues : [];
  const positions = resolveEntryRulePositions(item);
  const itemRepeatCount = resolveEntryRuleRepeatCount(item);
  const expectedSampleCount = resolveEntryRuleExpectedSampleCount(item);
  return Array.from({ length: expectedSampleCount }).map((_, index) => {
    const value = source[index];
    const row: EditableSample =
      value && typeof value === 'object' ? { ...value } : { value };
    const position =
      positions[Math.floor(index / itemRepeatCount)] || positions[index];
    row.samplePosition =
      row.samplePosition || position?.name || `点位${index + 1}`;
    row.samplePositionCode =
      row.samplePositionCode || position?.code || `P${index + 1}`;
    row.sampleGroupNo = row.sampleGroupNo || (index % itemRepeatCount) + 1;
    normalizeRowDefects(row);
    return row;
  });
}

function normalizeRowDefects(row: EditableSample) {
  row.defects = Array.isArray(row.defects) ? row.defects : [];
  row.defectCodeIds = Array.isArray(row.defectCodeIds)
    ? row.defectCodeIds
    : row.defects
        .map((defect: MesFqcApi.FqcSampleDefect) => defect.defectCodeId)
        .filter(Boolean);
}

function handleDefectChange(row: EditableSample, values: any) {
  const selectedIds = Array.isArray(values)
    ? values.map(Number).filter((value) => Number.isFinite(value))
    : [];
  const selected = new Set(selectedIds);
  const options = defectCodeOptions.value
    .filter((option) => selected.has(option.value));
  row.defectCodeIds = options.map((option) => option.value);
  row.defects = options
    .map((option, index) => ({
      defectCodeId: option.value,
      defectCode: option.defectCode,
      defectName: option.defectName,
      defectLevel: option.defectLevel,
      sort: (index + 1) * 10,
    }));
  row.defectCode = row.defects[0]?.defectCode;
  row.defectName = row.defects[0]?.defectName;
}

const filteredDefectCodeOptions = computed(() => {
  const keyword = defectPickerKeyword.value.trim().toLowerCase();
  if (!keyword) return defectCodeOptions.value;
  return defectCodeOptions.value.filter((option) =>
    [option.defectCode, option.defectName, option.remark]
      .filter(Boolean)
      .some((text) => String(text).toLowerCase().includes(keyword)),
  );
});

const activeDefectOption = computed(() => {
  return (
    defectCodeOptions.value.find(
      (option) => option.value === highlightedDefectCodeId.value,
    ) ||
    defectCodeOptions.value.find((option) =>
      defectPickerSelectedIds.value.includes(option.value),
    ) ||
    filteredDefectCodeOptions.value[0]
  );
});

watch(filteredDefectCodeOptions, (list) => {
  if (!defectPickerOpen.value) return;
  if (
    highlightedDefectCodeId.value &&
    list.some((option) => option.value === highlightedDefectCodeId.value)
  ) {
    return;
  }
  highlightedDefectCodeId.value = list[0]?.value;
});

function openDefectPicker(row: EditableSample) {
  if (props.readonly) return;
  normalizeRowDefects(row);
  defectPickerKeyword.value = '';
  defectPickerRow.value = row;
  defectPickerSelectedIds.value = Array.isArray(row.defectCodeIds)
    ? row.defectCodeIds
        .map(Number)
        .filter((value: number) => Number.isFinite(value))
    : [];
  highlightedDefectCodeId.value =
    defectPickerSelectedIds.value[0] || defectCodeOptions.value[0]?.value;
  defectPickerOpen.value = true;
}

function closeDefectPicker() {
  defectPickerOpen.value = false;
  defectPickerRow.value = null;
  defectPickerSelectedIds.value = [];
  highlightedDefectCodeId.value = undefined;
}

function confirmDefectPicker() {
  if (defectPickerRow.value) {
    handleDefectChange(defectPickerRow.value, defectPickerSelectedIds.value);
  }
  closeDefectPicker();
}

function isDefectPickerSelected(value: number) {
  return defectPickerSelectedIds.value.includes(value);
}

function toggleDefectPickerSelection(value: number, checked: boolean) {
  highlightedDefectCodeId.value = value;
  if (checked) {
    if (!defectPickerSelectedIds.value.includes(value)) {
      defectPickerSelectedIds.value = [...defectPickerSelectedIds.value, value];
    }
    return;
  }
  defectPickerSelectedIds.value = defectPickerSelectedIds.value.filter(
    (item) => item !== value,
  );
}

function selectedDefectTags(row: EditableSample) {
  const defects = Array.isArray(row.defects) ? row.defects : [];
  if (defects.length > 0) {
    return defects
      .filter((defect) => defect?.defectCode || defect?.defectName)
      .map((defect, index) => ({
        key: defect.defectCodeId || `${defect.defectCode}-${index}`,
        label: `${defect.defectCode || '-'} - ${defect.defectName || '-'}`,
      }));
  }
  const ids = Array.isArray(row.defectCodeIds) ? row.defectCodeIds : [];
  const tags = ids
    .map((id) => defectCodeOptions.value.find((option) => option.value === id))
    .filter(Boolean)
    .map((option) => ({
      key: option!.value,
      label: `${option!.defectCode} - ${option!.defectName}`,
    }));
  if (tags.length > 0) return tags;
  if (row.defectCode || row.defectName) {
    return [
      {
        key: row.defectCode || row.defectName,
        label: `${row.defectCode || '-'} - ${row.defectName || '-'}`,
      },
    ];
  }
  return [];
}

function hasReferenceImages(option: DefectCodeOption) {
  return (
    Array.isArray(option.referencePicUrls) &&
    option.referencePicUrls.length > 0
  );
}

function clearRowDefects(row: EditableSample) {
  row.defectCodeIds = [];
  row.defects = [];
  row.defectCode = undefined;
  row.defectName = undefined;
}

function handleQualitativeChange(row: EditableSample) {
  if (row.value !== 'NG') {
    clearRowDefects(row);
  }
}

function ensureEditableValues(item: MesFqcApi.FqcItem) {
  if (
    !Array.isArray(item.qaValues) ||
    item.qaValues.length !== resolveExpectedSampleCount(item)
  ) {
    item.qaValues = buildEditableValues(item);
  }
}

function metricName(item: MesFqcApi.FqcItem) {
  if (item.sheetMetricName) return item.sheetMetricName;
  const parts = item.inspectionItem?.split('-') || [];
  return parts.length > 1 ? parts[parts.length - 1] : item.inspectionItem;
}

function entryGroupName(item: MesFqcApi.FqcItem) {
  const params = parseEntryRuleParams(item.templateParams);
  return typeof params.entryGroupName === 'string'
    ? params.entryGroupName.trim()
    : '';
}

function entryGroupCode(item: MesFqcApi.FqcItem) {
  const params = parseEntryRuleParams(item.templateParams);
  return typeof params.entryGroupCode === 'string'
    ? params.entryGroupCode.trim()
    : '';
}

function isEntryGroupItem(item: MesFqcApi.FqcItem) {
  return !!entryGroupCode(item);
}

function metricDisplayName(item: MesFqcApi.FqcItem) {
  const text = [
    item.inspectionItem,
    item.sheetMetricName,
    item.valueTemplateName,
    item.standardDesc,
  ]
    .filter(Boolean)
    .join(' ');
  const dataRule = resolveDataRule(item);
  const judgmentMetric = String(
    item.judgmentMetric || dataRule?.judgmentMetric || '',
  ).toLowerCase();
  if (
    text.includes('弹性') ||
    text.includes('回弹') ||
    judgmentMetric.includes('elastic')
  ) {
    return '压缩弹性率';
  }
  if (text.includes('压缩') || judgmentMetric.includes('compression')) {
    return '压缩率';
  }
  return metricName(item) || '-';
}

const metricRows = computed(() => {
  const item = localItem.value;
  if (!item) return [];
  const groupItems =
    isEntryGroupItem(item) && props.entryGroupItems?.length
      ? props.entryGroupItems
      : [item];
  return groupItems.map((groupItem) => ({
    key: groupItem.id ?? metricDisplayName(groupItem),
    name: metricDisplayName(groupItem),
    avgLimitText: controlText(
      groupItem.avgMinLimit,
      groupItem.avgMaxLimit,
      valueUnit(groupItem),
    ),
    stdLimitText: controlText(
      groupItem.stdMinLimit,
      groupItem.stdMaxLimit,
      valueUnit(groupItem),
    ),
  }));
});

function displayMetricName(item: MesFqcApi.FqcItem) {
  return entryGroupName(item) || metricName(item);
}

function sectionName(item: MesFqcApi.FqcItem) {
  if (item.sheetSectionName) return item.sheetSectionName;
  const parts = item.inspectionItem?.split('-') || [];
  return parts.length > 1 ? parts.slice(0, -1).join('-') : item.stepName || '-';
}

function processName() {
  return props.record?.operationName || props.record?.processCategory || '-';
}

function valueUnit(item: MesFqcApi.FqcItem) {
  if (item.unit) return item.unit;
  const desc = item.standardDesc || '';
  if (desc.includes('g/cm')) return 'g/cm2';
  if (desc.includes('%')) return '%';
  if (desc.includes('HA')) return 'HA';
  if (desc.includes('um')) return 'um';
  if (desc.includes('mm')) return 'mm';
  return '';
}

function controlText(min?: number, max?: number, unit = '') {
  if (min === undefined && max === undefined) return '待定';
  return `${min ?? '-'}~${max ?? '-'}${unit}`;
}

function formatNumber(value?: number, digits = 3) {
  if (value === undefined || value === null || Number.isNaN(Number(value)))
    return '-';
  return Number(value).toFixed(digits);
}

function resultColor(result?: string) {
  if (result === 'OK') return 'success';
  if (result === 'NG') return 'error';
  return 'default';
}

function resultLabel(result?: string) {
  if (result === 'OK') return '合格';
  if (result === 'NG') return '不合格';
  if (result === '-' || result === 'PENDING' || !result) return '待判定';
  return result;
}

function countFilled(item: MesFqcApi.FqcItem) {
  ensureEditableValues(item);
  return item.qaValues!.filter((row) => isSampleFilled(item, row)).length;
}

function isSampleFilled(item: MesFqcApi.FqcItem, row: EditableSample) {
  if (item.itemType === 'QUALITATIVE') {
    return row.value === 'OK' || (row.value === 'NG' && !!row.remark);
  }
  const dataRule = resolveDataRule(item);
  if (dataRule) {
    return dataRule.inputFields
      .filter((field) => field.required !== false)
      .every(
        (field) =>
          row[field.code] !== undefined &&
          row[field.code] !== null &&
          row[field.code] !== '',
      );
  }
  if (item.valueTemplate === 'DENSITY_CALC') {
    return (
      row.thicknessMm !== undefined &&
      row.thicknessMm !== null &&
      row.weightG !== undefined &&
      row.weightG !== null
    );
  }
  if (item.valueTemplate === 'COMPRESSION_CALC') {
    return (
      row.t1Mm !== undefined &&
      row.t1Mm !== null &&
      row.t2Mm !== undefined &&
      row.t2Mm !== null &&
      row.t3Mm !== undefined &&
      row.t3Mm !== null
    );
  }
  return row.value !== undefined && row.value !== null && row.value !== '';
}

function hasQualitativeNgMissingRemark(item: MesFqcApi.FqcItem) {
  if (item.itemType !== 'QUALITATIVE') return false;
  ensureEditableValues(item);
  return item.qaValues!.some(
    (row) => row.value === 'NG' && !String(row.remark || '').trim(),
  );
}

function findQualitativeNgMissingDefect(item: MesFqcApi.FqcItem) {
  if (item.itemType !== 'QUALITATIVE') return undefined;
  ensureEditableValues(item);
  return item.qaValues!.find(
    (row) =>
      row.value === 'NG' &&
      (!Array.isArray(row.defects) || row.defects.length === 0),
  );
}

function densityPreview(row: EditableSample) {
  const thickness = Number(row.thicknessMm);
  const weight = Number(row.weightG);
  if (
    !Number.isFinite(thickness) ||
    !Number.isFinite(weight) ||
    thickness === 0
  )
    return row.densityValue;
  return weight / (((thickness / 10) * 3.14 * 39 * 39) / 4);
}

function compressionRatePreview(row: EditableSample) {
  const t1 = Number(row.t1Mm);
  const t2 = Number(row.t2Mm);
  if (!Number.isFinite(t1) || !Number.isFinite(t2) || t1 === 0)
    return row.compressionRate;
  return ((t1 - t2) / t1) * 100;
}

function compressionElasticityPreview(row: EditableSample) {
  const t1 = Number(row.t1Mm);
  const t2 = Number(row.t2Mm);
  const t3 = Number(row.t3Mm);
  if (
    !Number.isFinite(t1) ||
    !Number.isFinite(t2) ||
    !Number.isFinite(t3) ||
    t1 === t2
  )
    return row.compressionElasticityRate;
  return ((t3 - t2) / (t1 - t2)) * 100;
}

function evaluateFormula(formula: string, values: Record<string, any>) {
  if (!/^[\w\s+\-*/().,]+$/.test(formula)) return undefined;
  const identifiers = formula.match(/[A-Za-z_]\w*/g) || [];
  const allowedFunctions = new Set(['avg', 'std', 'round']);
  const scope: Record<string, number | ((...args: number[]) => number)> = {
    avg: (...args: number[]) =>
      args.reduce((total, value) => total + value, 0) / args.length,
    round: (value: number, precision = 2) =>
      Number(value.toFixed(Math.max(0, precision))),
    std: (...args: number[]) => {
      const avg = args.reduce((total, value) => total + value, 0) / args.length;
      const variance =
        args.reduce((total, value) => total + (value - avg) ** 2, 0) /
        args.length;
      return Math.sqrt(variance);
    },
  };
  for (const identifier of identifiers) {
    if (allowedFunctions.has(identifier)) continue;
    const value = Number(values[identifier]);
    if (!Number.isFinite(value)) return undefined;
    scope[identifier] = value;
  }
  try {
    const names = Object.keys(scope);
    const args = Object.values(scope);
    const result = Function(
      ...names,
      `"use strict"; return (${formula});`,
    )(...args);
    return Number.isFinite(Number(result)) ? Number(result) : undefined;
  } catch {
    return undefined;
  }
}

function dataRuleResultPreview(
  item: MesFqcApi.FqcItem,
  row: EditableSample,
  result: DataRuleResultField,
) {
  const values: Record<string, any> = { ...row };
  const dataRule = resolveDataRule(item);
  if (!dataRule) return row[result.code];
  for (const output of dataRule.resultFields) {
    const value = evaluateFormula(output.formula, values);
    if (value === undefined) return row[result.code];
    const precision = Number(output.precision ?? 3);
    values[output.code] = Number(value.toFixed(Math.max(0, precision)));
    if (output.code === result.code) return values[output.code];
  }
  return row[result.code];
}

function prepareItemValues(item: MesFqcApi.FqcItem) {
  ensureEditableValues(item);
  item.qaValues = item.qaValues!.map((row) => {
    const next = { ...row };
    if (item.itemType === 'QUALITATIVE') {
      next.qualitativeValue = next.value;
      next.sampleResult = next.value || 'PENDING';
      if (next.sampleResult !== 'NG') {
        clearRowDefects(next);
      }
    } else if (isDataRuleItem(item)) {
      const dataRule = resolveDataRule(item)!;
      for (const result of dataRule.resultFields) {
        const value = dataRuleResultPreview(item, next, result);
        next[result.code] = value;
      }
      const metric =
        dataRule.judgmentMetric ||
        dataRule.resultFields.find((result) => result.judgment)?.code;
      next.resultValue = metric ? next[metric] : undefined;
      next.measuredValue = next.resultValue;
      next.rawValuesJson = JSON.stringify(
        Object.fromEntries(
          dataRule.inputFields
            .map((field) => [field.code, next[field.code]])
            .filter(([, value]) => value !== undefined && value !== null),
        ),
      );
    } else if (item.valueTemplate === 'DENSITY_CALC') {
      const density = densityPreview(next);
      next.densityValue = density;
      next.resultValue = density;
    } else if (item.valueTemplate === 'COMPRESSION_CALC') {
      const rate = compressionRatePreview(next);
      const elasticity = compressionElasticityPreview(next);
      next.compressionRate = rate;
      next.compressionElasticityRate = elasticity;
      next.resultValue =
        item.judgmentMetric === 'COMPRESSION_ELASTICITY_RATE'
          ? elasticity
          : rate;
    } else {
      next.resultValue = next.value;
    }
    return next;
  });
  return item;
}

function prepareItemDraft() {
  if (!localItem.value) return;
  const prepared = { ...prepareItemValues(localItem.value) };
  if (hasQualitativeNgMissingRemark(prepared)) {
    entryMessage.warning('定性检测判定不合格时必须填写备注');
    return;
  }
  const missingDefect = findQualitativeNgMissingDefect(prepared);
  if (missingDefect) {
    entryMessage.warning(
      `【${prepared.inspectionItem}】样本 ${missingDefect.sampleSeq} 判定不合格时必须选择缺陷码`,
    );
    return;
  }
  if (countFilled(prepared) < resolveEntryRuleSampleSize(prepared)) {
    prepared.qaValues = (prepared.qaValues || []).filter((row) =>
      isSampleFilled(prepared, row),
    );
  }
  return prepared;
}

function handleSave(closeAfterSave = false) {
  const prepared = prepareItemDraft();
  if (prepared) emit('save', prepared, closeAfterSave);
}

function handleRecalculate() {
  const prepared = prepareItemDraft();
  if (prepared) emit('recalculate', prepared);
}

function handleClose() {
  emit('update:open', false);
}

function getPopupContainer() {
  return modalRootRef.value || document.body;
}
</script>

<template>
  <Teleport to="body">
    <div
      v-if="open && localItem"
      ref="modalRootRef"
      class="fixed inset-0 z-[3000] bg-white"
      style="z-index: 10000"
    >
      <component :is="entryMessageHolder" v-if="entryMessageReady" />
      <section
        class="flex h-screen w-screen flex-col overflow-hidden bg-white shadow-2xl"
      >
        <header
          class="flex shrink-0 items-center justify-between border-b px-6 py-4"
        >
          <div class="text-lg font-bold text-slate-800">
            填写检验项 - {{ displayMetricName(localItem) }}
          </div>
          <button
            type="button"
            class="rounded px-3 py-2 text-xl leading-none text-slate-500 hover:bg-slate-100"
            :disabled="saving"
            @click="handleClose"
          >
            ×
          </button>
        </header>

        <div class="min-h-0 flex-1 space-y-4 overflow-auto p-6">
          <div
            v-if="record?.lastReturnReason"
            class="rounded border border-amber-200 bg-amber-50 px-3 py-2 text-sm text-amber-800"
          >
            <span class="font-bold">驳回原因：</span>
            <span>{{ record.lastReturnReason }}</span>
          </div>

          <div
            class="grid grid-cols-4 gap-2 rounded border bg-slate-50 p-3 text-xs"
          >
            <div class="rounded bg-white p-2">
              <div class="font-bold text-slate-400">检验项目</div>
              <div class="mt-1 font-bold text-slate-800">
                {{ displayMetricName(localItem) }}
              </div>
            </div>
            <div class="rounded bg-white p-2">
              <div class="font-bold text-slate-400">工序阶段</div>
              <div class="mt-1 text-slate-700">{{ processName() }}</div>
            </div>
            <div class="rounded bg-white p-2">
              <div class="font-bold text-slate-400">检测对象</div>
              <div class="mt-1 text-slate-700">
                {{ sectionName(localItem) }}
              </div>
            </div>
            <div class="rounded bg-white p-2">
              <div class="font-bold text-slate-400">填写说明</div>
              <div class="mt-1 break-words text-slate-700">
                {{ localItem.ruleDescription || '-' }}
              </div>
            </div>
            <div class="rounded bg-white p-2">
              <div class="font-bold text-slate-400">当前判定</div>
              <Tag :color="resultColor(localItem.qaResult)" class="!mb-0 !mt-1">
                {{ resultLabel(localItem.qaResult) }}
              </Tag>
            </div>
            <div class="rounded bg-white p-2">
              <div class="font-bold text-slate-400">平均值内控</div>
              <div class="mt-1 font-mono text-slate-700">
                <template v-if="isEntryGroupItem(localItem)">
                  <div
                    v-for="row in metricRows"
                    :key="`avg-limit-${row.key}`"
                    class="flex justify-between gap-2 text-xs leading-5"
                  >
                    <span class="font-sans text-slate-500">{{
                      row.name
                    }}</span>
                    <span>{{ row.avgLimitText }}</span>
                  </div>
                </template>
                <template v-else>
                  {{
                    controlText(
                      localItem.avgMinLimit,
                      localItem.avgMaxLimit,
                      valueUnit(localItem),
                    )
                  }}
                </template>
              </div>
            </div>
            <div class="rounded bg-white p-2">
              <div class="font-bold text-slate-400">标准差内控</div>
              <div class="mt-1 font-mono text-slate-700">
                <template v-if="isEntryGroupItem(localItem)">
                  <div
                    v-for="row in metricRows"
                    :key="`std-limit-${row.key}`"
                    class="flex justify-between gap-2 text-xs leading-5"
                  >
                    <span class="font-sans text-slate-500">{{
                      row.name
                    }}</span>
                    <span>{{ row.stdLimitText }}</span>
                  </div>
                </template>
                <template v-else>
                  {{
                    controlText(
                      localItem.stdMinLimit,
                      localItem.stdMaxLimit,
                      valueUnit(localItem),
                    )
                  }}
                </template>
              </div>
            </div>
            <div class="rounded bg-white p-2">
              <div class="font-bold text-slate-400">填值进度</div>
              <div class="mt-1 font-mono text-slate-700">
                {{ countFilled(localItem) }}/{{
                  resolveExpectedSampleCount(localItem)
                }}
              </div>
            </div>
          </div>

          <div class="max-h-[calc(100vh-350px)] overflow-auto rounded border">
            <table
              class="w-full min-w-[1200px] table-fixed border-collapse text-base"
            >
              <thead class="bg-white text-xs text-slate-500">
                <tr v-if="localItem.itemType === 'QUALITATIVE'">
                  <th class="w-24 border-b px-2 py-2">位置</th>
                  <th class="w-48 border-b px-2 py-2">判定</th>
                  <th class="w-80 border-b px-2 py-2">缺陷码</th>
                  <th class="border-b px-2 py-2">备注</th>
                </tr>
                <tr v-else-if="isDataRuleItem(localItem)">
                  <th class="w-24 border-b px-2 py-2">位置</th>
                  <th
                    v-for="n in repeatCount"
                    :key="`r-h-${n}`"
                    class="border-b px-2 py-2"
                    :colspan="
                      (resolveDataRule(localItem)?.inputFields.length || 0) +
                      (resolveDataRule(localItem)?.resultFields.length || 0)
                    "
                  >
                    第{{ n }}组
                  </th>
                </tr>
                <tr v-else-if="localItem.valueTemplate === 'DENSITY_CALC'">
                  <th class="w-24 border-b px-2 py-2">位置</th>
                  <th
                    v-for="n in repeatCount"
                    :key="`d-h-${n}`"
                    class="border-b px-2 py-2"
                    colspan="3"
                  >
                    第{{ n }}组
                  </th>
                </tr>
                <tr v-else-if="localItem.valueTemplate === 'COMPRESSION_CALC'">
                  <th class="w-24 border-b px-2 py-2">位置</th>
                  <th
                    v-for="n in repeatCount"
                    :key="`c-h-${n}`"
                    class="border-b px-2 py-2"
                    colspan="5"
                  >
                    第{{ n }}组
                  </th>
                </tr>
                <tr v-else>
                  <th class="w-24 border-b px-2 py-2">位置</th>
                  <th
                    v-for="n in repeatCount"
                    :key="`s-h-${n}`"
                    class="border-b px-2 py-2"
                  >
                    第{{ n }}组
                  </th>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="row in positionRows"
                  :key="row.code"
                  class="border-b last:border-b-0"
                >
                  <td class="bg-slate-50 px-2 py-2 text-center font-bold">
                    {{ row.name }}
                  </td>
                  <template v-if="localItem.itemType === 'QUALITATIVE'">
                    <td class="px-2 py-2">
                      <Radio.Group
                        v-model:value="row.values[0].value"
                        :disabled="readonly"
                        button-style="solid"
                        size="large"
                        @change="handleQualitativeChange(row.values[0])"
                      >
                        <Radio.Button value="OK">合格</Radio.Button>
                        <Radio.Button value="NG">不合格</Radio.Button>
                      </Radio.Group>
                    </td>
                    <td class="px-2 py-2">
                      <div
                        v-if="row.values[0].value === 'NG'"
                        class="flex flex-col items-start gap-2"
                      >
                        <Button
                          :disabled="readonly"
                          size="large"
                          @click="openDefectPicker(row.values[0])"
                        >
                          选择缺陷码
                        </Button>
                        <div
                          v-if="selectedDefectTags(row.values[0]).length > 0"
                          class="flex flex-wrap gap-1"
                        >
                          <Tag
                            v-for="tag in selectedDefectTags(row.values[0])"
                            :key="tag.key"
                            color="error"
                            class="!m-0"
                          >
                            {{ tag.label }}
                          </Tag>
                        </div>
                        <span v-else class="text-xs text-slate-400">
                          未选择缺陷码
                        </span>
                      </div>
                      <span v-else class="text-xs text-slate-400">-</span>
                    </td>
                    <td class="px-2 py-2">
                      <Input
                        v-model:value="row.values[0].remark"
                        :disabled="readonly"
                        placeholder="不合格时必填备注"
                        size="large"
                      />
                    </td>
                  </template>
                  <template v-else-if="isDataRuleItem(localItem)">
                    <template
                      v-for="(sample, index) in row.values"
                      :key="`${row.code}-r-${index}`"
                    >
                      <td
                        v-for="field in resolveDataRule(localItem)?.inputFields"
                        :key="`${row.code}-${index}-${field.code}`"
                        class="px-2 py-2"
                      >
                        <div class="flex flex-col items-stretch gap-1">
                          <span class="text-xs font-bold text-slate-500">
                            {{ field.name
                            }}{{ field.unit ? `(${field.unit})` : '' }}
                          </span>
                          <InputNumber
                            v-model:value="sample[field.code]"
                            :disabled="readonly"
                            :precision="field.precision ?? 3"
                            class="w-full"
                            size="large"
                          />
                        </div>
                      </td>
                      <td
                        v-for="result in resolveDataRule(localItem)
                          ?.resultFields"
                        :key="`${row.code}-${index}-${result.code}`"
                        class="px-2 py-2 text-center font-mono text-slate-500"
                      >
                        <div
                          class="flex flex-col items-center gap-1 text-center"
                        >
                          <span class="text-xs font-bold text-slate-500">
                            {{ result.name
                            }}{{ result.unit ? `(${result.unit})` : '' }}
                          </span>
                          <span>
                            {{
                              formatNumber(
                                dataRuleResultPreview(
                                  localItem,
                                  sample,
                                  result,
                                ),
                                result.precision ?? 3,
                              )
                            }}
                          </span>
                        </div>
                      </td>
                    </template>
                  </template>
                  <template
                    v-else-if="localItem.valueTemplate === 'DENSITY_CALC'"
                  >
                    <template
                      v-for="(sample, index) in row.values"
                      :key="`${row.code}-d-${index}`"
                    >
                      <td class="px-2 py-2">
                        <div class="flex flex-col items-stretch gap-1">
                          <span class="text-xs font-bold text-slate-500">
                            厚度(mm)
                          </span>
                          <InputNumber
                            v-model:value="sample.thicknessMm"
                            :disabled="readonly"
                            :precision="3"
                            class="w-full"
                            size="large"
                          />
                        </div>
                      </td>
                      <td class="px-2 py-2">
                        <div class="flex flex-col items-stretch gap-1">
                          <span class="text-xs font-bold text-slate-500">
                            重量(g)
                          </span>
                          <InputNumber
                            v-model:value="sample.weightG"
                            :disabled="readonly"
                            :precision="3"
                            class="w-full"
                            size="large"
                          />
                        </div>
                      </td>
                      <td class="px-2 py-2 text-right font-mono text-slate-500">
                        {{ formatNumber(densityPreview(sample)) }}
                      </td>
                    </template>
                  </template>
                  <template
                    v-else-if="localItem.valueTemplate === 'COMPRESSION_CALC'"
                  >
                    <template
                      v-for="(sample, index) in row.values"
                      :key="`${row.code}-c-${index}`"
                    >
                      <td class="px-2 py-2">
                        <div class="flex flex-col items-stretch gap-1">
                          <span class="text-xs font-bold text-slate-500">
                            T1(mm)
                          </span>
                          <InputNumber
                            v-model:value="sample.t1Mm"
                            :disabled="readonly"
                            :precision="3"
                            class="w-full"
                            size="large"
                          />
                        </div>
                      </td>
                      <td class="px-2 py-2">
                        <div class="flex flex-col items-stretch gap-1">
                          <span class="text-xs font-bold text-slate-500">
                            T2(mm)
                          </span>
                          <InputNumber
                            v-model:value="sample.t2Mm"
                            :disabled="readonly"
                            :precision="3"
                            class="w-full"
                            size="large"
                          />
                        </div>
                      </td>
                      <td class="px-2 py-2">
                        <div class="flex flex-col items-stretch gap-1">
                          <span class="text-xs font-bold text-slate-500">
                            T3(mm)
                          </span>
                          <InputNumber
                            v-model:value="sample.t3Mm"
                            :disabled="readonly"
                            :precision="3"
                            class="w-full"
                            size="large"
                          />
                        </div>
                      </td>
                      <td class="px-2 py-2 text-right font-mono text-slate-500">
                        {{ formatNumber(compressionRatePreview(sample)) }}
                      </td>
                      <td class="px-2 py-2 text-right font-mono text-slate-500">
                        {{ formatNumber(compressionElasticityPreview(sample)) }}
                      </td>
                    </template>
                  </template>
                  <template v-else>
                    <td
                      v-for="(sample, index) in row.values"
                      :key="`${row.code}-s-${index}`"
                      class="px-2 py-2"
                    >
                      <div class="flex flex-col items-stretch gap-1">
                        <span class="text-xs font-bold text-slate-500">
                          {{ valueUnit(localItem) || '实测值' }}
                        </span>
                        <InputNumber
                          v-model:value="sample.value"
                          :disabled="readonly"
                          :precision="3"
                          class="w-full"
                          size="large"
                        />
                      </div>
                    </td>
                  </template>
                </tr>
              </tbody>
            </table>
          </div>

          <div class="flex items-center justify-between gap-3 border-t pt-3">
            <div class="text-xs font-bold text-slate-500">
              当前检测第
              <span class="font-mono text-indigo-600">
                {{ currentItemIndex }}
              </span>
              / {{ currentItemCount }} 项
            </div>
            <div class="flex items-center justify-end gap-2">
              <Button :disabled="saving" @click="handleClose">关闭</Button>
              <Button
                v-if="!readonly"
                :loading="saving"
                @click="handleRecalculate"
              >
                <IconifyIcon icon="lucide:calculator" class="mr-1" />重算当前项
              </Button>
              <Button
                v-if="!readonly"
                :loading="saving"
                @click="handleSave(false)"
              >
                <IconifyIcon icon="lucide:save" class="mr-1" />保存当前项
              </Button>
              <Button
                v-if="!readonly"
                type="primary"
                :loading="saving"
                @click="handleSave(true)"
              >
                <IconifyIcon icon="lucide:save" class="mr-1" />保存
              </Button>
            </div>
          </div>
        </div>
      </section>
      <Modal
        v-model:open="defectPickerOpen"
        cancel-text="取消"
        ok-text="确认选择"
        title="选择缺陷码"
        width="1120px"
        :get-container="getPopupContainer"
        :mask-closable="false"
        :z-index="10050"
        @cancel="closeDefectPicker"
        @ok="confirmDefectPicker"
      >
        <div class="space-y-3">
          <Input
            v-model:value="defectPickerKeyword"
            allow-clear
            placeholder="按缺陷代码、缺陷名称、备注搜索"
            size="large"
          />

          <div class="grid grid-cols-[minmax(0,1fr)_360px] gap-4">
            <div class="max-h-[520px] overflow-auto rounded border">
              <table class="w-full min-w-[680px] border-collapse text-sm">
                <thead class="sticky top-0 bg-slate-50 text-xs text-slate-500">
                  <tr>
                    <th class="w-12 border-b px-2 py-2 text-center">选择</th>
                    <th class="w-28 border-b px-2 py-2 text-left">
                      缺陷代码
                    </th>
                    <th class="w-36 border-b px-2 py-2 text-left">
                      缺陷名称
                    </th>
                    <th class="w-24 border-b px-2 py-2 text-left">
                      严重等级
                    </th>
                    <th class="border-b px-2 py-2 text-left">备注</th>
                    <th class="w-20 border-b px-2 py-2 text-center">
                      图片
                    </th>
                  </tr>
                </thead>
                <tbody>
                  <tr
                    v-for="option in filteredDefectCodeOptions"
                    :key="option.value"
                    class="cursor-pointer border-b last:border-b-0 hover:bg-slate-50"
                    :class="{
                      'bg-blue-50':
                        option.value === highlightedDefectCodeId,
                    }"
                    @click="highlightedDefectCodeId = option.value"
                  >
                    <td class="px-2 py-2 text-center">
                      <Checkbox
                        :checked="isDefectPickerSelected(option.value)"
                        @click.stop
                        @change="
                          (event) =>
                            toggleDefectPickerSelection(
                              option.value,
                              !!event?.target?.checked,
                            )
                        "
                      />
                    </td>
                    <td class="px-2 py-2 font-mono text-slate-700">
                      {{ option.defectCode }}
                    </td>
                    <td class="px-2 py-2 text-slate-700">
                      {{ option.defectName }}
                    </td>
                    <td class="px-2 py-2">
                      <Tag class="!m-0">{{ option.defectLevel || '-' }}</Tag>
                    </td>
                    <td class="px-2 py-2 text-slate-600">
                      {{ option.remark || '-' }}
                    </td>
                    <td class="px-2 py-2 text-center">
                      <Tag
                        :color="
                          hasReferenceImages(option) ? 'success' : 'default'
                        "
                        class="!m-0"
                      >
                        {{ hasReferenceImages(option) ? '有' : '无' }}
                      </Tag>
                    </td>
                  </tr>
                  <tr v-if="filteredDefectCodeOptions.length === 0">
                    <td
                      class="px-2 py-10 text-center text-slate-400"
                      colspan="6"
                    >
                      暂无匹配缺陷码
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>

            <aside class="max-h-[520px] overflow-auto rounded border p-3">
              <template v-if="activeDefectOption">
                <div class="space-y-3">
                  <div>
                    <div class="text-xs font-bold text-slate-400">
                      缺陷代码
                    </div>
                    <div class="mt-1 font-mono text-base text-slate-800">
                      {{ activeDefectOption.defectCode }}
                    </div>
                  </div>
                  <div>
                    <div class="text-xs font-bold text-slate-400">
                      缺陷名称
                    </div>
                    <div class="mt-1 text-base font-bold text-slate-800">
                      {{ activeDefectOption.defectName }}
                    </div>
                  </div>
                  <div>
                    <div class="text-xs font-bold text-slate-400">
                      严重等级
                    </div>
                    <Tag class="!mb-0 !mt-1">
                      {{ activeDefectOption.defectLevel || '-' }}
                    </Tag>
                  </div>
                  <div>
                    <div class="text-xs font-bold text-slate-400">备注</div>
                    <div class="mt-1 whitespace-pre-wrap text-sm text-slate-700">
                      {{ activeDefectOption.remark || '-' }}
                    </div>
                  </div>

                  <div>
                    <div class="mb-2 text-xs font-bold text-slate-400">
                      发生原因
                    </div>
                    <div
                      v-if="activeDefectOption.causes?.length"
                      class="space-y-2"
                    >
                      <div
                        v-for="(cause, index) in activeDefectOption.causes"
                        :key="
                          cause.reasonCode || cause.reasonName || index
                        "
                        class="rounded border bg-slate-50 p-2 text-xs text-slate-700"
                      >
                        <div class="flex items-center justify-between gap-2">
                          <span class="font-bold">
                            {{ cause.reasonCode || '-' }} -
                            {{ cause.reasonName || '-' }}
                          </span>
                          <Tag class="!m-0">
                            {{ cause.reasonType || '-' }}
                          </Tag>
                        </div>
                        <div
                          v-if="cause.reasonDesc"
                          class="mt-1 whitespace-pre-wrap"
                        >
                          {{ cause.reasonDesc }}
                        </div>
                        <div
                          v-if="cause.remark"
                          class="mt-1 whitespace-pre-wrap text-slate-500"
                        >
                          {{ cause.remark }}
                        </div>
                      </div>
                    </div>
                    <div v-else class="text-sm text-slate-400">
                      暂无发生原因
                    </div>
                  </div>

                  <div>
                    <div class="mb-2 text-xs font-bold text-slate-400">
                      参考缺陷图片
                    </div>
                    <div
                      v-if="activeDefectOption.referencePicUrls?.length"
                      class="flex flex-wrap gap-2"
                    >
                      <Image
                        v-for="url in activeDefectOption.referencePicUrls"
                        :key="url"
                        :height="72"
                        :src="url"
                        :width="96"
                        class="rounded border object-cover"
                        :preview="{
                          getContainer: getPopupContainer,
                          rootClassName: 'qms-fqc-defect-image-preview',
                          zIndex: 10080,
                        }"
                      />
                    </div>
                    <div v-else class="text-sm text-slate-400">
                      暂无参考图片
                    </div>
                  </div>
                </div>
              </template>
              <div v-else class="py-12 text-center text-sm text-slate-400">
                请选择左侧缺陷项查看详情
              </div>
            </aside>
          </div>

          <div class="text-xs text-slate-500">
            已选择 {{ defectPickerSelectedIds.length }} 个缺陷码
          </div>
        </div>
      </Modal>
    </div>
  </Teleport>
</template>
