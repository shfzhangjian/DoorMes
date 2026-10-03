<script lang="ts" setup>
import type { MesFaiApi } from '#/api/mes/quality/fai';
import type { UploadFile } from 'ant-design-vue';

import { computed, nextTick, ref, toRaw, watch } from 'vue';

import { IconifyIcon } from '@vben/icons';

import {
  Button,
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
import { FileUpload } from '#/components/upload';

defineOptions({ name: 'QmsFaiItemValueInputModal' });

const props = defineProps<{
  entryGroupItems?: MesFaiApi.FaiItem[];
  item?: MesFaiApi.FaiItem;
  itemCount?: number;
  itemIndex?: number;
  open: boolean;
  readonly?: boolean;
  record?: MesFaiApi.FaiRecord | null;
  saving?: boolean;
}>();

const emit = defineEmits<{
  recalculate: [item: MesFaiApi.FaiItem];
  save: [item: MesFaiApi.FaiItem, closeAfterSave?: boolean];
  'update:open': [open: boolean];
}>();

type EditableSample = Record<string, any>;

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

const localItem = ref<MesFaiApi.FaiItem>();
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

function getPopupContainer() {
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

function cloneFaiItem(item: MesFaiApi.FaiItem): MesFaiApi.FaiItem {
  return structuredClone(toPlainValue(item)) as MesFaiApi.FaiItem;
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
    const cloned = cloneFaiItem(props.item);
    cloned.qaValues = buildEditableValues(cloned);
    cloned.attachmentUrls ||= [];
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

function resolveDataRule(item: MesFaiApi.FaiItem): DataRuleConfig | undefined {
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

function isDataRuleItem(item: MesFaiApi.FaiItem) {
  return item.itemType === 'QUANTITATIVE' && !!resolveDataRule(item);
}

const entryGroupMinWidth = computed(() => {
  const item = localItem.value;
  if (!item) return 160;
  if (isDataRuleItem(item)) {
    const dataRule = resolveDataRule(item);
    const fieldCount =
      (dataRule?.inputFields.length || 0) +
      (dataRule?.resultFields.length || 0);
    return Math.min(560, Math.max(320, fieldCount * 132));
  }
  if (item.valueTemplate === 'DENSITY_CALC') return 360;
  if (item.valueTemplate === 'COMPRESSION_CALC') return 520;
  return 160;
});

function resolveExpectedSampleCount(item: MesFaiApi.FaiItem) {
  return resolveEntryRuleExpectedSampleCount(item);
}

function buildEditableValues(item: MesFaiApi.FaiItem): EditableSample[] {
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
    return row;
  });
}

function ensureEditableValues(item: MesFaiApi.FaiItem) {
  if (
    !Array.isArray(item.qaValues) ||
    item.qaValues.length !== resolveExpectedSampleCount(item)
  ) {
    item.qaValues = buildEditableValues(item);
  }
}

function metricName(item: MesFaiApi.FaiItem) {
  if (item.sheetMetricName) return item.sheetMetricName;
  const parts = item.inspectionItem?.split('-') || [];
  return parts.length > 1 ? parts[parts.length - 1] : item.inspectionItem;
}

function entryGroupName(item: MesFaiApi.FaiItem) {
  const params = parseEntryRuleParams(item.templateParams);
  return typeof params.entryGroupName === 'string'
    ? params.entryGroupName.trim()
    : '';
}

function entryGroupCode(item: MesFaiApi.FaiItem) {
  const params = parseEntryRuleParams(item.templateParams);
  return typeof params.entryGroupCode === 'string'
    ? params.entryGroupCode.trim()
    : '';
}

function isEntryGroupItem(item: MesFaiApi.FaiItem) {
  return !!entryGroupCode(item);
}

function metricDisplayName(item: MesFaiApi.FaiItem) {
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

function displayMetricName(item: MesFaiApi.FaiItem) {
  return entryGroupName(item) || metricName(item);
}

function sectionName(item: MesFaiApi.FaiItem) {
  if (item.sheetSectionName) return item.sheetSectionName;
  const parts = item.inspectionItem?.split('-') || [];
  return parts.length > 1 ? parts.slice(0, -1).join('-') : item.stepName || '-';
}

function processName() {
  return props.record?.operationName || props.record?.processCategory || '-';
}

function valueUnit(item: MesFaiApi.FaiItem) {
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

function countFilled(item: MesFaiApi.FaiItem) {
  ensureEditableValues(item);
  return item.qaValues!.filter((row) => isSampleFilled(item, row)).length;
}

function isSampleFilled(item: MesFaiApi.FaiItem, row: EditableSample) {
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

function hasQualitativeNgMissingRemark(item: MesFaiApi.FaiItem) {
  if (item.itemType !== 'QUALITATIVE') return false;
  ensureEditableValues(item);
  return item.qaValues!.some(
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
  item: MesFaiApi.FaiItem,
  row: EditableSample,
  result: DataRuleResultField,
) {
  const values: Record<string, any> = { ...row };
  const dataRule = resolveDataRule(item);
  if (!dataRule) return row[result.code];
  for (const output of dataRule.resultFields) {
    const value = evaluateFormula(output.formula, values);
    if (value === undefined) return row[result.code];
    // 压缩性能按原始计算精度传给后端统计；output.precision 仍只负责页面展示。
    const precision =
      item.valueTemplate === 'COMPRESSION_CALC'
        ? 6
        : Number(output.precision ?? 3);
    values[output.code] = Number(value.toFixed(Math.max(0, precision)));
    if (output.code === result.code) return values[output.code];
  }
  return row[result.code];
}

function prepareItemValues(item: MesFaiApi.FaiItem) {
  ensureEditableValues(item);
  item.qaValues = item.qaValues!.map((row) => {
    const next = { ...row };
    if (item.itemType === 'QUALITATIVE') {
      next.qualitativeValue = next.value;
      next.sampleResult = next.value || 'PENDING';
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
                    <span class="font-sans text-slate-500">{{ row.name }}</span>
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
                    <span class="font-sans text-slate-500">{{ row.name }}</span>
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

          <div
            v-if="localItem.itemType === 'QUALITATIVE'"
            class="max-h-[calc(100vh-350px)] overflow-auto overscroll-contain rounded border"
          >
            <table
              class="w-full min-w-[720px] table-fixed border-collapse text-base"
            >
              <thead
                class="sticky top-0 z-20 bg-white text-xs text-slate-500 shadow-sm"
              >
                <tr v-if="localItem.itemType === 'QUALITATIVE'">
                  <th
                    class="sticky left-0 z-30 w-24 border-b bg-white px-2 py-2"
                  >
                    位置
                  </th>
                  <th class="w-48 border-b px-2 py-2">判定</th>
                  <th class="border-b px-2 py-2">备注</th>
                </tr>
                <tr v-else-if="isDataRuleItem(localItem)">
                  <th
                    class="sticky left-0 z-30 w-24 border-b bg-white px-2 py-2"
                  >
                    位置
                  </th>
                  <th
                    v-for="n in repeatCount"
                    :key="`r-h-${n}`"
                    class="whitespace-nowrap border-b px-2 py-2"
                    :colspan="
                      (resolveDataRule(localItem)?.inputFields.length || 0) +
                      (resolveDataRule(localItem)?.resultFields.length || 0)
                    "
                  >
                    第{{ n }}组
                  </th>
                </tr>
                <tr v-else-if="localItem.valueTemplate === 'DENSITY_CALC'">
                  <th
                    class="sticky left-0 z-30 w-24 border-b bg-white px-2 py-2"
                  >
                    位置
                  </th>
                  <th
                    v-for="n in repeatCount"
                    :key="`d-h-${n}`"
                    class="whitespace-nowrap border-b px-2 py-2"
                    colspan="3"
                  >
                    第{{ n }}组
                  </th>
                </tr>
                <tr v-else-if="localItem.valueTemplate === 'COMPRESSION_CALC'">
                  <th
                    class="sticky left-0 z-30 w-24 border-b bg-white px-2 py-2"
                  >
                    位置
                  </th>
                  <th
                    v-for="n in repeatCount"
                    :key="`c-h-${n}`"
                    class="whitespace-nowrap border-b px-2 py-2"
                    colspan="5"
                  >
                    第{{ n }}组
                  </th>
                </tr>
                <tr v-else>
                  <th
                    class="sticky left-0 z-30 w-24 border-b bg-white px-2 py-2"
                  >
                    位置
                  </th>
                  <th
                    v-for="n in repeatCount"
                    :key="`s-h-${n}`"
                    class="whitespace-nowrap border-b px-2 py-2"
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
                  <td
                    class="sticky left-0 z-10 whitespace-nowrap bg-slate-50 px-2 py-2 text-center font-bold shadow-[1px_0_0_0_rgb(226_232_240)]"
                  >
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
                          <span
                            class="whitespace-nowrap text-xs font-bold text-slate-500"
                          >
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
                          <span
                            class="whitespace-nowrap text-xs font-bold text-slate-500"
                          >
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
                          <span
                            class="whitespace-nowrap text-xs font-bold text-slate-500"
                          >
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
                          <span
                            class="whitespace-nowrap text-xs font-bold text-slate-500"
                          >
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
                          <span
                            class="whitespace-nowrap text-xs font-bold text-slate-500"
                          >
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
                          <span
                            class="whitespace-nowrap text-xs font-bold text-slate-500"
                          >
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
                          <span
                            class="whitespace-nowrap text-xs font-bold text-slate-500"
                          >
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
                        <span
                          class="whitespace-nowrap text-xs font-bold text-slate-500"
                        >
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
            v-else
            class="max-h-[calc(100vh-350px)] space-y-3 overflow-auto overscroll-contain rounded border bg-slate-50 p-3"
          >
            <section
              v-for="row in positionRows"
              :key="row.code"
              class="rounded border bg-white p-3"
            >
              <div class="mb-3 flex items-center gap-2">
                <span class="text-xs font-bold text-slate-400">位置</span>
                <span class="font-bold text-slate-700">{{ row.name }}</span>
              </div>

              <div
                class="grid gap-3"
                :style="{
                  gridTemplateColumns: `repeat(auto-fill, minmax(min(${entryGroupMinWidth}px, 100%), 1fr))`,
                }"
              >
                <article
                  v-for="(sample, index) in row.values"
                  :key="`${row.code}-flow-${index}`"
                  class="min-w-0 rounded border border-slate-200 bg-slate-50 p-3"
                >
                  <div
                    class="mb-2 border-b border-slate-200 pb-2 text-center text-xs font-bold text-slate-500"
                  >
                    第{{ sample.sampleGroupNo || index + 1 }}组
                  </div>

                  <div
                    v-if="isDataRuleItem(localItem)"
                    class="grid gap-2"
                    style="
                      grid-template-columns: repeat(
                        auto-fit,
                        minmax(112px, 1fr)
                      );
                    "
                  >
                    <label
                      v-for="field in resolveDataRule(localItem)?.inputFields"
                      :key="`${row.code}-${index}-${field.code}-flow`"
                      class="min-w-0"
                    >
                      <span
                        class="mb-1 block truncate text-xs font-bold text-slate-500"
                        :title="`${field.name}${field.unit ? `(${field.unit})` : ''}`"
                      >
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
                    </label>
                    <div
                      v-for="result in resolveDataRule(localItem)?.resultFields"
                      :key="`${row.code}-${index}-${result.code}-flow`"
                      class="min-w-0 rounded bg-white px-2 py-1.5 text-center"
                    >
                      <div
                        class="truncate text-xs font-bold text-slate-500"
                        :title="`${result.name}${result.unit ? `(${result.unit})` : ''}`"
                      >
                        {{ result.name
                        }}{{ result.unit ? `(${result.unit})` : '' }}
                      </div>
                      <div class="mt-2 font-mono text-slate-700">
                        {{
                          formatNumber(
                            dataRuleResultPreview(localItem, sample, result),
                            result.precision ?? 3,
                          )
                        }}
                      </div>
                    </div>
                  </div>

                  <div
                    v-else-if="localItem.valueTemplate === 'DENSITY_CALC'"
                    class="grid grid-cols-3 gap-2"
                  >
                    <label class="min-w-0">
                      <span class="mb-1 block text-xs font-bold text-slate-500">
                        厚度(mm)
                      </span>
                      <InputNumber
                        v-model:value="sample.thicknessMm"
                        :disabled="readonly"
                        :precision="3"
                        class="w-full"
                        size="large"
                      />
                    </label>
                    <label class="min-w-0">
                      <span class="mb-1 block text-xs font-bold text-slate-500">
                        重量(g)
                      </span>
                      <InputNumber
                        v-model:value="sample.weightG"
                        :disabled="readonly"
                        :precision="3"
                        class="w-full"
                        size="large"
                      />
                    </label>
                    <div
                      class="min-w-0 rounded bg-white px-2 py-1.5 text-center"
                    >
                      <div class="text-xs font-bold text-slate-500">
                        密度(g/cm²)
                      </div>
                      <div class="mt-2 font-mono text-slate-700">
                        {{ formatNumber(densityPreview(sample)) }}
                      </div>
                    </div>
                  </div>

                  <div
                    v-else-if="localItem.valueTemplate === 'COMPRESSION_CALC'"
                    class="grid grid-cols-5 gap-2"
                  >
                    <label
                      v-for="field in [
                        { code: 't1Mm', name: 'T1(mm)' },
                        { code: 't2Mm', name: 'T2(mm)' },
                        { code: 't3Mm', name: 'T3(mm)' },
                      ]"
                      :key="`${row.code}-${index}-${field.code}-flow`"
                      class="min-w-0"
                    >
                      <span class="mb-1 block text-xs font-bold text-slate-500">
                        {{ field.name }}
                      </span>
                      <InputNumber
                        v-model:value="sample[field.code]"
                        :disabled="readonly"
                        :precision="3"
                        class="w-full"
                        size="large"
                      />
                    </label>
                    <div
                      class="min-w-0 rounded bg-white px-2 py-1.5 text-center"
                    >
                      <div class="text-xs font-bold text-slate-500">
                        压缩率(%)
                      </div>
                      <div class="mt-2 font-mono text-slate-700">
                        {{ formatNumber(compressionRatePreview(sample)) }}
                      </div>
                    </div>
                    <div
                      class="min-w-0 rounded bg-white px-2 py-1.5 text-center"
                    >
                      <div class="text-xs font-bold text-slate-500">
                        压缩弹性率(%)
                      </div>
                      <div class="mt-2 font-mono text-slate-700">
                        {{ formatNumber(compressionElasticityPreview(sample)) }}
                      </div>
                    </div>
                  </div>

                  <label v-else class="block min-w-0">
                    <span class="mb-1 block text-xs font-bold text-slate-500">
                      {{ valueUnit(localItem) || '实测值' }}
                    </span>
                    <InputNumber
                      v-model:value="sample.value"
                      :disabled="readonly"
                      :precision="3"
                      class="w-full"
                      size="large"
                    />
                  </label>
                </article>
              </div>
            </section>
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
              :directory="`mes/qms/fai/${record?.faiNo || 'draft'}`"
              list-type="picture"
              :max-number="10"
              :max-size="20"
              multiple
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
        :get-container="getPopupContainer"
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
