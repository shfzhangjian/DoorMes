<script lang="ts" setup>
import type { MesIqcApi } from '#/api/mes/quality/iqc';
import type { UploadFile } from 'ant-design-vue';

import { computed, nextTick, ref, toRaw, watch } from 'vue';

import { IconifyIcon } from '@vben/icons';

import {
  Button,
  DatePicker,
  Image,
  Input,
  InputNumber,
  message,
  Modal,
  Radio,
  Tag,
} from 'ant-design-vue';

import type { EntryRulePosition } from '#/api/mes/quality/entry-rule';

import {
  parseEntryRuleParams,
  resolveEntryRuleExpectedSampleCount,
  resolveEntryRulePositions,
  resolveEntryRuleRepeatCount,
  resolveEntryRuleSampleSize,
} from '#/api/mes/quality/entry-rule';
import { FileUpload } from '#/components/upload';

defineOptions({ name: 'QmsIqcItemValueInputModal' });

const props = defineProps<{
  item?: MesIqcApi.IqcItem;
  itemCount?: number;
  itemIndex?: number;
  open: boolean;
  readonly?: boolean;
  record?: MesIqcApi.IqcRecord | null;
  saving?: boolean;
}>();

const emit = defineEmits<{
  recalculate: [item: MesIqcApi.IqcItem];
  save: [item: MesIqcApi.IqcItem, closeAfterSave?: boolean];
  'update:open': [open: boolean];
}>();

type EditableSample = Record<string, any>;

type PositionConfig = EntryRulePosition;

interface DataRuleInputField {
  code: string;
  name: string;
  precision?: number;
  required?: boolean;
  unit?: string;
}

interface DataRuleResultField {
  code: string;
  formula?: string;
  judgment?: boolean;
  name: string;
  precision?: number;
  unit?: string;
}

interface DataRuleConfig {
  inputFields: DataRuleInputField[];
  judgmentMetric?: string;
  resultFields: DataRuleResultField[];
}

const localItem = ref<MesIqcApi.IqcItem>();
const modalRootRef = ref<HTMLElement | null>(null);
const attachmentPreviewOpen = ref(false);
const attachmentPreviewTitle = ref('');
const attachmentPreviewUrl = ref('');
const entryMessageReady = ref(false);
let attachmentPreviewObjectUrl: string | undefined;
const [entryMessage, entryMessageHolder] = message.useMessage({
  staticGetContainer: () => modalRootRef.value || document.body,
  top: '24px',
} as any);

function getDatePickerPopupContainer() {
  return modalRootRef.value || document.body;
}

function isImageUploadFile(file: UploadFile) {
  const mimeType = file.type || file.originFileObj?.type || '';
  if (mimeType.toLowerCase().startsWith('image/')) return true;
  const filePath = file.name || file.url || '';
  return /\.(?:avif|bmp|gif|ico|jpe?g|png|svg|tiff?|webp)(?:$|[?#])/i.test(
    filePath,
  );
}

function releaseAttachmentPreviewObjectUrl() {
  if (!attachmentPreviewObjectUrl) return;
  URL.revokeObjectURL(attachmentPreviewObjectUrl);
  attachmentPreviewObjectUrl = undefined;
}

function closeAttachmentPreview() {
  attachmentPreviewOpen.value = false;
  releaseAttachmentPreviewObjectUrl();
}

function handleAttachmentPreview(file: UploadFile) {
  const fileUrl = file.url || file.thumbUrl || file.preview;
  if (!isImageUploadFile(file)) {
    if (fileUrl) {
      const previewWindow = window.open(
        fileUrl,
        '_blank',
        'noopener,noreferrer',
      );
      if (previewWindow) previewWindow.opener = null;
    } else {
      entryMessage.warning('当前附件尚未生成可访问地址');
    }
    return;
  }

  releaseAttachmentPreviewObjectUrl();
  let previewUrl = fileUrl;
  if (!previewUrl && file.originFileObj) {
    attachmentPreviewObjectUrl = URL.createObjectURL(file.originFileObj);
    previewUrl = attachmentPreviewObjectUrl;
  }
  if (!previewUrl) {
    entryMessage.warning('当前图片尚未生成可预览地址');
    return;
  }
  attachmentPreviewTitle.value = file.name || '图片预览';
  attachmentPreviewUrl.value = previewUrl;
  attachmentPreviewOpen.value = true;
}

watch(
  () => props.open,
  async (isOpen) => {
    if (!isOpen) {
      closeAttachmentPreview();
      entryMessageReady.value = false;
      return;
    }
    await nextTick();
    entryMessageReady.value = true;
  },
  { flush: 'post' },
);

watch(
  () => [props.item, props.open] as const,
  () => {
    if (!props.open || !props.item) return;
    const cloned = cloneItem(props.item);
    cloned.sampleValues = buildEditableValues(cloned);
    cloned.attachmentUrls ||= [];
    localItem.value = cloned;
  },
  { immediate: true },
);

const repeatCount = computed(() =>
  localItem.value ? resolveRepeatCount(localItem.value) : 1,
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
  const itemRepeatCount = resolveRepeatCount(item);
  return resolvePositions(item).map((position, positionIndex) => ({
    ...position,
    values: Array.from({ length: itemRepeatCount }).map(
      (_, offset) =>
        item.sampleValues![positionIndex * itemRepeatCount + offset],
    ),
  }));
});

function cloneItem(item: MesIqcApi.IqcItem): MesIqcApi.IqcItem {
  return structuredClone(toPlainValue(item)) as MesIqcApi.IqcItem;
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

function resolvePositions(item: MesIqcApi.IqcItem): PositionConfig[] {
  return resolveEntryRulePositions(item);
}

function resolveRepeatCount(item: MesIqcApi.IqcItem) {
  return resolveEntryRuleRepeatCount(item);
}

function resolveSampleSize(item: MesIqcApi.IqcItem) {
  return resolveEntryRuleSampleSize(item);
}

function resolveExpectedSampleCount(item: MesIqcApi.IqcItem) {
  return resolveEntryRuleExpectedSampleCount(item);
}

function resolveDataRule(item: MesIqcApi.IqcItem): DataRuleConfig | undefined {
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
      precision: Number(field.precision ?? 3),
      required: field.required !== false,
      unit: field.unit || '',
    })),
    judgmentMetric:
      dataRule.judgmentMetric ||
      resultFields.find((field: any) => field.judgment)?.code,
    resultFields: resultFields.map((field: any) => ({
      code: field.code,
      formula: field.formula || '',
      judgment: !!field.judgment,
      name: field.name || field.code,
      precision: Number(field.precision ?? 3),
      unit: field.unit || '',
    })),
  };
}

function isDataRuleItem(item: MesIqcApi.IqcItem) {
  return item.itemType === 'QUANTITATIVE' && !!resolveDataRule(item);
}

function normalizeEditableRow(
  item: MesIqcApi.IqcItem,
  source: unknown,
  index: number,
  position?: PositionConfig,
) {
  const row: EditableSample =
    source && typeof source === 'object' ? { ...(source as object) } : {};
  if (source !== undefined && source !== null && typeof source !== 'object') {
    row.value = source;
  }
  const sample = item.samples?.[index];
  if (sample) {
    row.sampleId = row.sampleId || sample.id;
    row.sampleBarcode = row.sampleBarcode || sample.sampleBarcode;
    row.value =
      row.value ??
      (item.itemType === 'QUALITATIVE'
        ? sample.qualitativeValue
        : item.itemType === 'DATE'
          ? sample.dateValue
          : sample.measuredValue);
    row.remark = row.remark || sample.remark;
    row.sampleResult = row.sampleResult || sample.sampleResult;
  }
  row.samplePosition =
    row.samplePosition || position?.name || `点位${index + 1}`;
  row.samplePositionCode =
    row.samplePositionCode || position?.code || `P${index + 1}`;
  row.sampleGroupNo =
    row.sampleGroupNo || (index % resolveRepeatCount(item)) + 1;
  return row;
}

function buildEditableValues(item: MesIqcApi.IqcItem): EditableSample[] {
  const source = Array.isArray(item.sampleValues) ? item.sampleValues : [];
  const positions = resolvePositions(item);
  const itemRepeatCount = resolveRepeatCount(item);
  const expectedSampleCount = Math.max(
    resolveSampleSize(item),
    positions.length * itemRepeatCount,
  );
  return Array.from({ length: expectedSampleCount }).map((_, index) => {
    const position =
      positions[Math.floor(index / itemRepeatCount)] || positions[index];
    return normalizeEditableRow(item, source[index], index, position);
  });
}

function ensureEditableValues(item: MesIqcApi.IqcItem) {
  if (
    !Array.isArray(item.sampleValues) ||
    item.sampleValues.length !== resolveExpectedSampleCount(item)
  ) {
    item.sampleValues = buildEditableValues(item);
  }
}

function metricName(item: MesIqcApi.IqcItem) {
  return item.inspectionItem;
}

function sectionName(item: MesIqcApi.IqcItem) {
  return item.standardDesc || item.ruleDescription || '-';
}

function processName() {
  return props.record?.receiptNo || props.record?.supplierName || '-';
}

function valueUnit(item: MesIqcApi.IqcItem) {
  if (item.unit) return item.unit;
  const desc = item.standardDesc || '';
  if (desc.includes('mPa')) return 'mPa.s';
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

function businessToday() {
  const parts = new Intl.DateTimeFormat('en-US', {
    day: '2-digit',
    month: '2-digit',
    timeZone: 'Asia/Shanghai',
    year: 'numeric',
  }).formatToParts(new Date());
  const values = Object.fromEntries(
    parts.map((part) => [part.type, part.value]),
  );
  return `${values.year}-${values.month}-${values.day}`;
}

function dateOrdinal(value?: string) {
  if (!value) return undefined;
  const match = value.match(/^(\d{4})-(\d{2})-(\d{2})$/);
  if (!match) return undefined;
  return (
    Date.UTC(Number(match[1]), Number(match[2]) - 1, Number(match[3])) /
    86_400_000
  );
}

function dateSampleResult(item: MesIqcApi.IqcItem, value?: string) {
  const selectedOrdinal = dateOrdinal(value);
  const todayOrdinal = dateOrdinal(businessToday());
  if (
    selectedOrdinal === undefined ||
    todayOrdinal === undefined ||
    item.expiryDays === undefined
  ) {
    return 'PENDING';
  }
  if (selectedOrdinal > todayOrdinal) return 'NG';
  return todayOrdinal - selectedOrdinal > item.expiryDays ? 'NG' : 'OK';
}

function disabledFutureDate(current: any) {
  return !!current && current.format('YYYY-MM-DD') > businessToday();
}

function hasFutureDate(item: MesIqcApi.IqcItem) {
  if (item.itemType !== 'DATE') return false;
  const todayOrdinal = dateOrdinal(businessToday())!;
  return (item.sampleValues || []).some((row) => {
    const selectedOrdinal = dateOrdinal(row?.value);
    return selectedOrdinal !== undefined && selectedOrdinal > todayOrdinal;
  });
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
  if (result === 'SKIP') return '不判定';
  if (result === 'OK') return '合格';
  if (result === 'NG') return '不合格';
  if (result === '-' || result === 'PENDING' || !result) return '待判定';
  return result;
}

function countFilled(item: MesIqcApi.IqcItem) {
  ensureEditableValues(item);
  return item.sampleValues!.filter((row) => isSampleFilled(item, row)).length;
}

function valueTemplateLabel(item: MesIqcApi.IqcItem) {
  if (item.itemType === 'DATE') {
    return `日期有效期判定（${item.expiryDays ?? '-'}天）`;
  }
  if (item.valueTemplateName) return item.valueTemplateName;
  if (item.valueTemplate === 'DENSITY_CALC') return '密度计算模板';
  if (item.valueTemplate === 'COMPRESSION_CALC') return '压缩性能计算模板';
  if (item.valueTemplate === 'SINGLE_VALUE') return '单值实测模板';
  return item.testFrequencyJudgement || '-';
}

function isSampleFilled(item: MesIqcApi.IqcItem, row: EditableSample) {
  if (item.itemType === 'QUALITATIVE') {
    return row.value === 'OK' || (row.value === 'NG' && !!row.remark);
  }
  if (item.itemType === 'DATE') {
    return !!dateOrdinal(row.value);
  }
  if (isDataRuleItem(item)) {
    const dataRule = resolveDataRule(item)!;
    return dataRule.inputFields.every((field) => {
      if (!field.required) return true;
      return row[field.code] !== undefined && row[field.code] !== null;
    });
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

function hasQualitativeNgMissingRemark(item: MesIqcApi.IqcItem) {
  if (item.itemType !== 'QUALITATIVE') return false;
  ensureEditableValues(item);
  return item.sampleValues!.some(
    (row) => row.value === 'NG' && !String(row.remark || '').trim(),
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

function evaluateFormula(
  formula: string | undefined,
  values: Record<string, any>,
): number | undefined {
  if (!formula) return undefined;
  const identifiers = Array.from(
    new Set(formula.match(/[A-Za-z_][\dA-Za-z_]*/g) || []),
  );
  const allowedFunctions = new Set(['avg', 'round', 'std']);
  const scope: Record<string, any> = {
    avg: (...args: number[]) =>
      args.length
        ? args.reduce((total, value) => total + value, 0) / args.length
        : undefined,
    round: (value: number, digits = 0) => {
      const factor = 10 ** Number(digits || 0);
      return Math.round(value * factor) / factor;
    },
    std: (...args: number[]) => {
      if (!args.length) return undefined;
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
  item: MesIqcApi.IqcItem,
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

function prepareItemValues(item: MesIqcApi.IqcItem) {
  ensureEditableValues(item);
  item.sampleValues = item.sampleValues!.map((row) => {
    const next = { ...row };
    if (item.itemType === 'QUALITATIVE') {
      next.qualitativeValue = next.value;
      next.sampleResult = next.value || 'PENDING';
    } else if (item.itemType === 'DATE') {
      next.dateValue = next.value;
      next.sampleResult = dateSampleResult(item, next.value);
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
  if (item.itemType === 'DATE') {
    const results = item.sampleValues.map((row) => row.sampleResult);
    item.itemResult = results.includes('NG')
      ? 'NG'
      : results.every((result) => result === 'OK')
        ? 'OK'
        : 'PENDING';
  }
  return item;
}

function prepareItemDraft() {
  if (!localItem.value) return;
  const prepared = { ...prepareItemValues(localItem.value) };
  if (hasQualitativeNgMissingRemark(prepared)) {
    entryMessage.warning('定性检测判定不合格时必须填写备注');
    return;
  }
  if (hasFutureDate(prepared)) {
    entryMessage.warning('时间检验项不能选择晚于当前日期的日期');
    return;
  }
  if (countFilled(prepared) < resolveSampleSize(prepared)) {
    prepared.sampleValues = (prepared.sampleValues || []).filter((row) =>
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
            填写检验项 - {{ metricName(localItem) }}
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
            class="grid grid-cols-4 gap-2 rounded border bg-slate-50 p-3 text-xs"
          >
            <div class="rounded bg-white p-2">
              <div class="font-bold text-slate-400">检验项目</div>
              <div class="mt-1 font-bold text-slate-800">
                {{ metricName(localItem) }}
              </div>
            </div>
            <div class="rounded bg-white p-2">
              <div class="font-bold text-slate-400">收料单号</div>
              <div class="mt-1 text-slate-700">{{ processName() }}</div>
            </div>
            <div class="rounded bg-white p-2">
              <div class="font-bold text-slate-400">检测对象</div>
              <div class="mt-1 text-slate-700">
                {{ record?.materialName || '-' }}
              </div>
            </div>
            <div class="rounded bg-white p-2">
              <div class="font-bold text-slate-400">当前判定</div>
              <Tag
                :color="resultColor(localItem.itemResult)"
                class="!mb-0 !mt-1"
              >
                {{ resultLabel(localItem.itemResult) }}
              </Tag>
              <div v-if="localItem.judgmentReason" class="mt-1 text-amber-700">
                {{ localItem.judgmentReason }}
              </div>
            </div>
            <div class="rounded bg-white p-2">
              <div class="font-bold text-slate-400">标准范围</div>
              <div class="mt-1 font-mono text-slate-700">
                <template v-if="localItem.itemType === 'DATE'">
                  距当前日期不超过 {{ localItem.expiryDays ?? '-' }} 天
                </template>
                <template v-else>{{
                  controlText(
                    localItem.minValueLimit,
                    localItem.maxValueLimit,
                    valueUnit(localItem),
                  )
                }}</template>
              </div>
            </div>
            <div class="rounded bg-white p-2">
              <div class="font-bold text-slate-400">平均值内控</div>
              <div class="mt-1 font-mono text-slate-700">
                {{
                  controlText(
                    localItem.avgMinLimit,
                    localItem.avgMaxLimit,
                    valueUnit(localItem),
                  )
                }}
              </div>
            </div>
            <div class="rounded bg-white p-2">
              <div class="font-bold text-slate-400">录入模板</div>
              <div class="mt-1 text-slate-700">
                {{ valueTemplateLabel(localItem) }}
              </div>
            </div>
            <div class="rounded bg-white p-2">
              <div class="font-bold text-slate-400">检验方法</div>
              <div class="mt-1 text-slate-700">
                {{ localItem.inspectionMethod || '-' }}
              </div>
            </div>
            <div class="rounded bg-white p-2">
              <div class="font-bold text-slate-400">标准要求</div>
              <div class="mt-1 text-slate-700">
                {{ sectionName(localItem) }}
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
                  <th class="border-b px-2 py-2">备注</th>
                </tr>
                <tr v-else-if="localItem.itemType === 'DATE'">
                  <th class="w-24 border-b px-2 py-2">位置</th>
                  <th class="border-b px-2 py-2">日期</th>
                  <th class="w-40 border-b px-2 py-2">自动判定</th>
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
                      >
                        <Radio.Button value="OK">合格</Radio.Button>
                        <Radio.Button value="NG">不合格</Radio.Button>
                      </Radio.Group>
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
                  <template v-else-if="localItem.itemType === 'DATE'">
                    <td class="px-2 py-2">
                      <DatePicker
                        v-model:value="row.values[0].value"
                        :disabled="readonly"
                        :disabled-date="disabledFutureDate"
                        :get-popup-container="getDatePickerPopupContainer"
                        class="w-full"
                        size="large"
                        value-format="YYYY-MM-DD"
                        placeholder="请选择日期"
                      />
                    </td>
                    <td class="px-2 py-2 text-center">
                      <Tag
                        :color="
                          resultColor(
                            dateSampleResult(localItem, row.values[0].value),
                          )
                        "
                      >
                        {{
                          resultLabel(
                            dateSampleResult(localItem, row.values[0].value),
                          )
                        }}
                      </Tag>
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

          <div
            v-if="localItem.attachmentEnabled"
            class="rounded border bg-slate-50 p-4"
          >
            <div class="mb-2 flex items-center justify-between">
              <div class="font-bold text-slate-700">检验项附件</div>
              <div class="text-xs text-slate-500">
                可选，最多10个，单文件不超过20MB
              </div>
            </div>
            <FileUpload
              v-model="localItem.attachmentUrls"
              :disabled="readonly"
              :directory="`mes/qms/iqc/${record?.iqcNo || 'draft'}`"
              list-type="picture"
              :max-number="10"
              :max-size="20"
              multiple
              show-description
              @preview="handleAttachmentPreview"
            />
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
        :footer="null"
        :get-container="getDatePickerPopupContainer"
        :open="attachmentPreviewOpen"
        :title="attachmentPreviewTitle"
        :z-index="10080"
        width="860px"
        @cancel="closeAttachmentPreview"
      >
        <div class="flex max-h-[72vh] justify-center overflow-auto bg-slate-50">
          <Image
            :preview="false"
            :src="attachmentPreviewUrl"
            class="max-h-[70vh] max-w-full object-contain"
          />
        </div>
      </Modal>
    </div>
  </Teleport>
</template>
