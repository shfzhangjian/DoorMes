<script lang="ts" setup>
import type { MesFqcApi } from '#/api/mes/quality/fqc';

import { computed, onMounted, ref, watch } from 'vue';

import { IconifyIcon } from '@vben/icons';
import { downloadFileFromBlobPart } from '@vben/utils';

import { Button, Empty, Input, message, Tag } from 'ant-design-vue';

import {
  parseEntryRuleParams,
  resolveEntryRuleExpectedSampleCount,
  resolveEntryRulePositions,
  resolveEntryRuleRepeatCount,
  resolveEntryRuleSampleSize,
} from '#/api/mes/quality/entry-rule';
import {
  downloadFqcItemTemplate,
  downloadGlueBoardFqcItemTemplate,
  exportFqcItemValues,
  exportGlueBoardFqcItemValues,
  getFqcDetail,
  getFqcPage,
  getGlueBoardFqcDetail,
  getGlueBoardFqcPage,
  getPendingGlueBoardFqcTasks,
  getPendingFqcTasks,
  getStandardByMaterial,
  recalculateGlueBoardFqcProgramEntry,
  recalculateFqcProgramEntry,
  saveGlueBoardFqcProgramEntry,
  saveFqcProgramEntry,
  submitGlueBoardFqcProgramEntry,
  submitFqcProgramEntry,
} from '#/api/mes/quality/fqc';

import {
  formatReportQty,
  resolveFqcOperationLabel,
  sortFqcRecordsByStatus,
} from '../data';
import QmsFqcItemOverviewImportModal from './qms-fqc-item-overview-import-modal.vue';
import QmsFqcItemValueInputModal from './qms-fqc-item-value-input-modal.vue';
import QmsFqcScanEntryBox from './qms-fqc-scan-entry-box.vue';
import QmsFqcTemplatePreviewModal from './qms-fqc-template-preview-modal.vue';

const props = withDefaults(
  defineProps<{
    apiMode?: 'FQC';
    enableImport?: boolean;
    enableScan?: boolean;
    initialRecord?: MesFqcApi.FqcRecord;
    initialRecordId?: number;
    pageDescription?: string;
    pageTitle?: string;
  }>(),
  {
    apiMode: 'FQC',
    enableImport: true,
    enableScan: true,
    pageDescription:
      '按检验标准的位置模板填写数值或定性判定，系统重算平均值和标准差',
    pageTitle: '成品检验项明细概览',
  },
);
const emit = defineEmits(['backToLedger']);

type EditableSample = Record<string, any>;

interface DataRuleInputField {
  code: string;
  name: string;
  required?: boolean;
}

interface DataRuleResultField {
  code: string;
  name: string;
  formula: string;
  judgment?: boolean;
  precision?: number;
}

interface DataRuleConfig {
  inputFields: DataRuleInputField[];
  resultFields: DataRuleResultField[];
  judgmentMetric?: string;
}

const activeRecord = ref<MesFqcApi.FqcRecord | null>(null);
const activeItems = ref<MesFqcApi.FqcItem[]>([]);
const selectedItemId = ref<number | string>();
const modalItem = ref<MesFqcApi.FqcItem>();
const inputModalOpen = ref(false);
const importModalOpen = ref(false);
const templatePreviewOpen = ref(false);
const expandedItemKeys = ref<string[]>([]);
const pendingList = ref<MesFqcApi.FqcRecord[]>([]);
const searchKeyword = ref('');
const loading = ref(false);
const saving = ref(false);
const showTaskSidebar = false;
const showItemExcelActions = false;
const showScanEntry = false;
const isGlueBoardMode = computed(() => props.apiMode === 'GLUE_BOARD');

interface LoadRecordOptions {
  fallbackToStandard?: boolean;
  skipEmptyDetailRefetch?: boolean;
}

onMounted(async () => {
  await refreshTaskList();
  if (props.initialRecord) {
    await loadRecord(props.initialRecord, { fallbackToStandard: false });
  } else if (props.initialRecordId) {
    await loadRecordById(props.initialRecordId);
  }
});

watch(
  () => props.initialRecord,
  async (record) => {
    if (record) await loadRecord(record, { fallbackToStandard: false });
  },
);

watch(
  () => props.initialRecordId,
  async (id) => {
    if (id && id !== activeRecord.value?.id && !props.initialRecord) {
      await loadRecordById(id);
    }
  },
);

const filteredTaskList = computed(() => {
  const keyword = searchKeyword.value.trim().toLowerCase();
  if (!keyword) return pendingList.value;
  return pendingList.value.filter((item) =>
    [
      item.fqcNo,
      item.workOrderNo,
      item.materialCode,
      item.productModel,
      item.operationName,
      item.productBatchNo,
    ]
      .filter(Boolean)
      .some((value) => String(value).toLowerCase().includes(keyword)),
  );
});

const displayItems = computed(() => {
  const groupedCodes = new Set<string>();
  return activeItems.value.filter((item) => {
    const code = entryGroupCode(item);
    if (!code) return true;
    if (groupedCodes.has(code)) return false;
    groupedCodes.add(code);
    return true;
  });
});

const displayInspectorName = computed(() => {
  return (
    activeRecord.value?.inspectorName ||
    activeRecord.value?.operatorName ||
    activeItems.value.find((item) => item.operatorName)?.operatorName ||
    ''
  );
});

const modalItemIndex = computed(() => {
  if (!modalItem.value) return 0;
  const currentKey = resolveItemKey(modalItem.value);
  const index = displayItems.value.findIndex((item) =>
    isSameItemKey(resolveItemKey(item), currentKey),
  );
  return index >= 0 ? index + 1 : 0;
});

async function refreshTaskList() {
  const [pending, page] = await Promise.all([
    isGlueBoardMode.value
      ? getPendingGlueBoardFqcTasks()
      : getPendingFqcTasks(),
    isGlueBoardMode.value
      ? getGlueBoardFqcPage({ pageNo: 1, pageSize: 20 } as MesFqcApi.FqcPageReq)
      : getFqcPage({ pageNo: 1, pageSize: 20 } as MesFqcApi.FqcPageReq),
  ]);
  const map = new Map<string, MesFqcApi.FqcRecord>();
  [...pending, ...(page.list || [])].forEach((item) =>
    map.set(item.fqcNo, item),
  );
  pendingList.value = sortSidebarTasks([...map.values()]);
}

function sortSidebarTasks(records: MesFqcApi.FqcRecord[]) {
  return sortFqcRecordsByStatus(records);
}

function syncSidebarTask(record: MesFqcApi.FqcRecord) {
  const next = new Map(pendingList.value.map((item) => [item.fqcNo, item]));
  next.set(record.fqcNo, record);
  pendingList.value = sortSidebarTasks([...next.values()]);
}

async function loadRecordById(id: number) {
  loading.value = true;
  try {
    const detail = await (isGlueBoardMode.value
      ? getGlueBoardFqcDetail(id)
      : getFqcDetail(id));
    await loadRecord(detail, { skipEmptyDetailRefetch: true });
  } finally {
    loading.value = false;
  }
}

async function handleSelectTask(row: MesFqcApi.FqcRecord) {
  if (!row.id) return;
  await loadRecord(row, { fallbackToStandard: false });
}

async function handleScanResolved(resp: MesFqcApi.FqcScanResp) {
  if (!resp.record?.id) {
    if (resp.message) message.info(resp.message);
    return;
  }
  await loadRecord(resp.record);
  syncSidebarTask(resp.record);
  if (resp.openTarget === 'ITEM_MODAL' && resp.matchedFqcItemId) {
    const target = activeItems.value.find(
      (item) => item.id === resp.matchedFqcItemId,
    );
    if (target) {
      handleOpenItemModal(target);
    }
  }
}

async function loadRecord(
  detail: MesFqcApi.FqcRecord,
  options: LoadRecordOptions = {},
) {
  activeRecord.value = { ...detail };
  let items = detail.items || [];
  if (items.length === 0 && detail.id && !options.skipEmptyDetailRefetch) {
    loading.value = true;
    try {
      const fullDetail = await (isGlueBoardMode.value
        ? getGlueBoardFqcDetail(detail.id)
        : getFqcDetail(detail.id));
      await loadRecord(fullDetail, {
        ...options,
        skipEmptyDetailRefetch: true,
      });
      return;
    } finally {
      loading.value = false;
    }
  }
  if (
    !isGlueBoardMode.value &&
    items.length === 0 &&
    detail.materialCode &&
    options.fallbackToStandard
  ) {
    items = (await getStandardByMaterial(
      detail.materialCode,
      detail.operationCode,
      detail.operationName,
    )) as MesFqcApi.FqcItem[];
  }
  items = filterRecheckItems(detail, items);
  activeItems.value = items.map((item) => {
    const normalized = { ...item };
    normalized.qaValues = buildEditableValues(normalized);
    return normalized;
  });
  activeRecord.value.items = activeItems.value;
  selectedItemId.value = activeItems.value[0]
    ? resolveItemKey(activeItems.value[0])
    : undefined;
  expandedItemKeys.value = [];
}

function filterRecheckItems(
  detail: MesFqcApi.FqcRecord,
  items: MesFqcApi.FqcItem[],
) {
  if (!detail.recheckFlag) return items;
  const recheckItems = items.filter(
    (item) => item.recheckItemFlag || item.samples?.some((sample) => sample.recheckItemFlag),
  );
  return recheckItems.length ? recheckItems : items;
}

function resolveItemKey(item: MesFqcApi.FqcItem) {
  return item.id ?? item.standardItemId ?? item.inspectionItem;
}

function isSameItemKey(left?: number | string, right?: number | string) {
  return (
    left !== undefined && right !== undefined && String(left) === String(right)
  );
}

function resolveItemTreeKey(item: MesFqcApi.FqcItem) {
  return String(resolveItemKey(item) ?? metricName(item));
}

function isItemExpanded(item: MesFqcApi.FqcItem) {
  return expandedItemKeys.value.includes(resolveItemTreeKey(item));
}

function toggleItemTree(item: MesFqcApi.FqcItem) {
  const key = resolveItemTreeKey(item);
  expandedItemKeys.value = isItemExpanded(item)
    ? expandedItemKeys.value.filter((itemKey) => itemKey !== key)
    : [...expandedItemKeys.value, key];
}

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
      required: field.required !== false,
    })),
    resultFields: resultFields.map((field: any) => ({
      code: field.code,
      name: field.name || field.code,
      formula: field.formula || '',
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

function entryGroupParams(item: MesFqcApi.FqcItem) {
  return parseEntryRuleParams(item.templateParams);
}

function entryGroupCode(item: MesFqcApi.FqcItem) {
  const params = entryGroupParams(item);
  return typeof params.entryGroupCode === 'string'
    ? params.entryGroupCode.trim()
    : '';
}

function entryGroupName(item: MesFqcApi.FqcItem) {
  const params = entryGroupParams(item);
  return typeof params.entryGroupName === 'string'
    ? params.entryGroupName.trim()
    : '';
}

function entryGroupItems(item: MesFqcApi.FqcItem) {
  const code = entryGroupCode(item);
  if (!code) return [item];
  return activeItems.value.filter(
    (activeItem) => entryGroupCode(activeItem) === code,
  );
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

function groupMetricRows(item: MesFqcApi.FqcItem) {
  return entryGroupItems(item).map((groupItem) => ({
    key: resolveItemKey(groupItem) ?? metricDisplayName(groupItem),
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
    avgValue: formatNumber(groupItem.calculatedAvg ?? groupItem.qaAvg),
    stdValue: formatNumber(groupItem.calculatedStd),
  }));
}

function displayMetricName(item: MesFqcApi.FqcItem) {
  return entryGroupName(item) || metricName(item);
}

function entryInputFields(item: MesFqcApi.FqcItem): DataRuleInputField[] {
  const dataRule = resolveDataRule(item);
  if (dataRule) return dataRule.inputFields;
  if (item.valueTemplate === 'COMPRESSION_CALC') {
    return [
      { code: 't1Mm', name: 'T1', required: true },
      { code: 't2Mm', name: 'T2', required: true },
      { code: 't3Mm', name: 'T3', required: true },
    ];
  }
  return [];
}

function buildEditableValues(item: MesFqcApi.FqcItem): EditableSample[] {
  const source = Array.isArray(item.qaValues) ? item.qaValues : [];
  const expectedCount = resolveEntryRuleExpectedSampleCount(item);
  const positions = resolveEntryRulePositions(item);
  const repeatCount = resolveEntryRuleRepeatCount(item);
  return Array.from({ length: expectedCount }).map((_, index) => {
    const value = source[index];
    const row: EditableSample =
      value && typeof value === 'object' ? { ...value } : { value };
    const position =
      positions[Math.floor(index / repeatCount)] || positions[index];
    row.samplePosition =
      row.samplePosition || position?.name || `点位${index + 1}`;
    row.sampleGroupNo = row.sampleGroupNo || (index % repeatCount) + 1;
    return row;
  });
}

function ensureEditableValues(item: MesFqcApi.FqcItem) {
  if (
    !Array.isArray(item.qaValues) ||
    item.qaValues.length !== resolveEntryRuleExpectedSampleCount(item)
  ) {
    item.qaValues = buildEditableValues(item);
  }
}

function isReadOnly() {
  return (
    activeRecord.value?.status === 'COMPLETED' ||
    activeRecord.value?.status === 'CANCELED' ||
    activeRecord.value?.sheetLocked === true
  );
}

function hasActiveItems() {
  return activeItems.value.length > 0;
}

function canMutateItems() {
  return !!activeRecord.value && hasActiveItems() && !isReadOnly();
}

function processLabel(record?: MesFqcApi.FqcRecord | null) {
  return resolveFqcOperationLabel(record);
}

function statusLabel(status?: string) {
  if (status === 'PENDING') return '待检验';
  if (status === 'INSPECTING') return '检验中';
  if (status === 'WAITING_QA') return '待审核';
  if (status === 'COMPLETED') return '已完成';
  if (status === 'REJECTED') return '已驳回';
  if (status === 'SUSPENDED') return '已挂起';
  if (status === 'REWORKING') return '处置中';
  return status || '-';
}

function statusColor(status?: string) {
  if (status === 'COMPLETED') return 'success';
  if (status === 'REJECTED' || status === 'REWORKING') return 'error';
  if (status === 'WAITING_QA') return 'purple';
  if (status === 'INSPECTING') return 'processing';
  if (status === 'PENDING') return 'warning';
  return 'default';
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

function itemReviewLabel(item: MesFqcApi.FqcItem) {
  return resultLabel(item.qaResult);
}

function itemReviewColor(item: MesFqcApi.FqcItem) {
  return resultColor(item.qaResult);
}

function metricName(item: MesFqcApi.FqcItem) {
  if (item.sheetMetricName) return item.sheetMetricName;
  const parts = item.inspectionItem?.split('-') || [];
  return parts.length > 1 ? parts[parts.length - 1] : item.inspectionItem;
}

function controlText(min?: number, max?: number, unit = '') {
  if (min === undefined && max === undefined) return '待定';
  return `${min ?? '-'}~${max ?? '-'}${unit}`;
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

function formatNumber(value?: number, digits = 3) {
  if (value === undefined || value === null || Number.isNaN(Number(value)))
    return '-';
  return Number(value).toFixed(digits);
}

function countFilled(item: MesFqcApi.FqcItem) {
  ensureEditableValues(item);
  return item.qaValues!.filter((row) => isSampleFilled(item, row)).length;
}

function canSubmitConfirm() {
  return canMutateItems();
}

function buildFillTreeRows(item: MesFqcApi.FqcItem) {
  ensureEditableValues(item);
  const itemRepeatCount = resolveEntryRuleRepeatCount(item);
  return resolveEntryRulePositions(item).map((position, positionIndex) => ({
    ...position,
    samples: Array.from({ length: itemRepeatCount }).map((_, offset) => {
      const sample =
        item.qaValues?.[positionIndex * itemRepeatCount + offset] || {};
      return {
        filled: isSampleFilled(item, sample),
        groupNo: offset + 1,
        result: sampleResultLabel(sample),
        text: sampleValueText(item, sample),
      };
    }),
  }));
}

function sampleResultLabel(sample: EditableSample) {
  return resultLabel(sample.sampleResult || sample.result || sample.value);
}

function sampleValueText(item: MesFqcApi.FqcItem, sample: EditableSample) {
  if (!isSampleFilled(item, sample)) return '未填写';
  if (item.itemType === 'QUALITATIVE') {
    return resultLabel(sample.value || sample.qualitativeValue);
  }
  const dataRule = resolveDataRule(item);
  if (dataRule) {
    return dataRule.inputFields
      .map((field) => `${field.name} ${formatNumber(sample[field.code])}`)
      .join(' / ');
  }
  if (item.valueTemplate === 'DENSITY_CALC') {
    return `厚度 ${formatNumber(sample.thicknessMm)} / 重量 ${formatNumber(
      sample.weightG,
    )} / 密度 ${formatNumber(densityPreview(sample))}`;
  }
  if (item.valueTemplate === 'COMPRESSION_CALC') {
    return `T1 ${formatNumber(sample.t1Mm)} / T2 ${formatNumber(
      sample.t2Mm,
    )} / T3 ${formatNumber(sample.t3Mm)} / 压缩率 ${formatNumber(
      compressionRatePreview(sample),
    )}% / 弹性率 ${formatNumber(compressionElasticityPreview(sample))}%`;
  }
  return `${formatNumber(sample.value ?? sample.resultValue)}${valueUnit(item)}`;
}

function positionSummary(item: MesFqcApi.FqcItem) {
  return `${resolveEntryRulePositions(item)
    .map((position) => position.name)
    .join(' / ')}，每位置 ${resolveEntryRuleRepeatCount(item)} 组`;
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

function applyDataRuleResults(item: MesFqcApi.FqcItem, row: EditableSample) {
  const dataRule = resolveDataRule(item);
  if (!dataRule) return row;
  const values: Record<string, any> = { ...row };
  for (const result of dataRule.resultFields) {
    const value = evaluateFormula(result.formula, values);
    if (value === undefined) continue;
    const precision = Number(result.precision ?? 3);
    values[result.code] = Number(value.toFixed(Math.max(0, precision)));
    row[result.code] = values[result.code];
  }
  const metric =
    dataRule.judgmentMetric ||
    dataRule.resultFields.find((result) => result.judgment)?.code;
  row.resultValue = metric ? row[metric] : undefined;
  row.measuredValue = row.resultValue;
  row.rawValuesJson = JSON.stringify(
    Object.fromEntries(
      dataRule.inputFields
        .map((field) => [field.code, row[field.code]])
        .filter(([, value]) => value !== undefined && value !== null),
    ),
  );
  return row;
}

function prepareItemValues(item: MesFqcApi.FqcItem) {
  ensureEditableValues(item);
  item.qaValues = item.qaValues!.map((row) => {
    const next = { ...row };
    if (item.itemType === 'QUALITATIVE') {
      next.qualitativeValue = next.value;
      next.sampleResult = next.value || 'PENDING';
    } else if (isDataRuleItem(item)) {
      applyDataRuleResults(item, next);
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

function prepareItemDraft(item: MesFqcApi.FqcItem) {
  const prepared = { ...prepareItemValues(item) };
  if (countFilled(prepared) < resolveEntryRuleSampleSize(prepared)) {
    prepared.qaValues = (prepared.qaValues || []).filter((row) =>
      isSampleFilled(prepared, row),
    );
  }
  return prepared;
}

function copyEntryGroupInputs(
  source: MesFqcApi.FqcItem,
  target: MesFqcApi.FqcItem,
) {
  const inputFields = entryInputFields(target);
  const fallbackFields =
    inputFields.length > 0 ? inputFields : entryInputFields(source);
  const sourceRows = source.qaValues || [];
  const qaValues = buildEditableValues(target).map((row, index) => {
    const sourceRow = sourceRows[index] || {};
    const nextRow = { ...row };
    for (const field of fallbackFields) {
      nextRow[field.code] = sourceRow[field.code];
    }
    return nextRow;
  });
  return {
    ...target,
    qaValues,
  };
}

function prepareEntryGroupItemDrafts(item: MesFqcApi.FqcItem) {
  const groupItems = entryGroupItems(item);
  if (groupItems.length <= 1) return [prepareItemDraft(item)];

  const sourceDraft = prepareItemDraft(item);
  const currentKey = resolveItemKey(item);
  return groupItems.map((groupItem) => {
    if (isSameItemKey(resolveItemKey(groupItem), currentKey)) {
      return sourceDraft;
    }
    return prepareItemDraft(copyEntryGroupInputs(sourceDraft, groupItem));
  });
}

function prepareMergedItemDrafts(item: MesFqcApi.FqcItem) {
  const currentKey = resolveItemKey(item);
  const currentDrafts = prepareEntryGroupItemDrafts(item);
  const currentDraftMap = new Map(
    currentDrafts.map((draft) => [String(resolveItemKey(draft)), draft]),
  );
  return activeItems.value
    .map((activeItem) => {
      const draft = currentDraftMap.get(String(resolveItemKey(activeItem)));
      if (draft) return draft;
      return resolveItemKey(activeItem) === currentKey
        ? currentDrafts[0]
        : prepareItemDraft(activeItem);
    })
    .filter(
      (draft) =>
        currentDraftMap.has(String(resolveItemKey(draft))) ||
        resolveItemKey(draft) === currentKey ||
        countFilled(draft) > 0,
    );
}

function recordPayload(items: MesFqcApi.FqcItem[]) {
  return {
    ...activeRecord.value!,
    items,
  };
}

function handleOpenItemModal(item: MesFqcApi.FqcItem) {
  selectedItemId.value = resolveItemKey(item);
  modalItem.value = item;
  inputModalOpen.value = true;
}

function findItemByKey(key?: number | string) {
  return activeItems.value.find((item) =>
    isSameItemKey(resolveItemKey(item), key),
  );
}

function resolveNextItemKey(item: MesFqcApi.FqcItem) {
  const currentKey = resolveItemKey(item);
  const currentIndex = displayItems.value.findIndex((activeItem) =>
    isSameItemKey(resolveItemKey(activeItem), currentKey),
  );
  const nextItem =
    currentIndex >= 0 ? displayItems.value[currentIndex + 1] : undefined;
  return nextItem ? resolveItemKey(nextItem) : undefined;
}

async function handleSaveModalItem(
  item: MesFqcApi.FqcItem,
  closeAfterSave = false,
) {
  if (!activeRecord.value) return;
  const currentKey = resolveItemKey(item);
  const nextKey = closeAfterSave ? resolveNextItemKey(item) : undefined;
  saving.value = true;
  try {
    const saveProgramEntry = isGlueBoardMode.value
      ? saveGlueBoardFqcProgramEntry
      : saveFqcProgramEntry;
    const resp = await saveProgramEntry(
      recordPayload(prepareMergedItemDrafts(item)),
    );
    await loadRecord(resp);
    if (closeAfterSave) {
      const nextItem = findItemByKey(nextKey);
      if (nextItem) {
        handleOpenItemModal(nextItem);
        message.success('当前检验项已保存，已自动跳转下一项');
      } else {
        const currentItem = findItemByKey(currentKey);
        if (currentItem) selectedItemId.value = resolveItemKey(currentItem);
        inputModalOpen.value = false;
        message.success('当前检验项已保存并关闭，已到最后一项');
      }
    } else {
      const currentItem = findItemByKey(currentKey);
      if (currentItem) {
        selectedItemId.value = resolveItemKey(currentItem);
        modalItem.value = currentItem;
      }
      message.success('当前检验项已保存');
    }
  } finally {
    saving.value = false;
  }
}

async function handleRecalculateModalItem(item: MesFqcApi.FqcItem) {
  if (!activeRecord.value || !item) return;
  saving.value = true;
  try {
    const recalculateProgramEntry = isGlueBoardMode.value
      ? recalculateGlueBoardFqcProgramEntry
      : recalculateFqcProgramEntry;
    const resp = await recalculateProgramEntry(
      recordPayload(prepareMergedItemDrafts(item)),
    );
    await loadRecord(resp);
    message.success('当前检验项已重算');
  } finally {
    saving.value = false;
  }
}

async function handleSubmit() {
  if (!activeRecord.value) return;
  const incomplete = activeItems.value.find(
    (item) => countFilled(item) < resolveEntryRuleSampleSize(item),
  );
  if (incomplete) {
    message.warning(
      `检验项“${incomplete.inspectionItem}”尚未填满 ${resolveEntryRuleSampleSize(incomplete)} 组数据`,
    );
    selectedItemId.value = resolveItemKey(incomplete);
    return;
  }
  saving.value = true;
  try {
    const submitProgramEntry = isGlueBoardMode.value
      ? submitGlueBoardFqcProgramEntry
      : submitFqcProgramEntry;
    const resp = await submitProgramEntry(
      recordPayload(activeItems.value.map((item) => prepareItemValues(item))),
    );
    await loadRecord(resp);
    syncSidebarTask(resp);
    message.success('成品检验数据已提交，等待质检主管审核');
  } finally {
    saving.value = false;
  }
}

async function handleDownloadItemTemplate() {
  if (!activeRecord.value?.id) return;
  const data = await (isGlueBoardMode.value
    ? downloadGlueBoardFqcItemTemplate(activeRecord.value.id)
    : downloadFqcItemTemplate(activeRecord.value.id));
  downloadFileFromBlobPart({
    fileName: `${isGlueBoardMode.value ? '胶板检验' : 'FQC'}检验项明细导入模板_${activeRecord.value.fqcNo}.xlsx`,
    source: data,
  });
}

async function handleExportItemValues() {
  if (!activeRecord.value?.id) return;
  const data = await (isGlueBoardMode.value
    ? exportGlueBoardFqcItemValues(activeRecord.value.id)
    : exportFqcItemValues(activeRecord.value.id));
  downloadFileFromBlobPart({
    fileName: `${isGlueBoardMode.value ? '胶板检验' : 'FQC'}检验项明细_${activeRecord.value.fqcNo}.xlsx`,
    source: data,
  });
}

async function handleImportSuccess(record?: MesFqcApi.FqcRecord) {
  if (record) {
    await loadRecord(record);
    syncSidebarTask(record);
    return;
  }
  if (activeRecord.value?.id) {
    await loadRecordById(activeRecord.value.id);
  }
}
</script>

<template>
  <div class="flex h-full flex-col overflow-hidden bg-slate-100">
    <header
      class="flex h-14 shrink-0 items-center justify-between border-b bg-white px-4"
    >
      <div class="flex min-w-0 items-center gap-3">
        <Button size="small" @click="emit('backToLedger')">
          <IconifyIcon icon="lucide:arrow-left" class="mr-1" /> 返回台账
        </Button>
        <div class="min-w-0">
          <div class="text-base font-bold text-slate-800">
            {{ props.pageTitle }}
          </div>
          <div class="text-xs text-slate-500">
            {{ props.pageDescription }}
          </div>
        </div>
      </div>
      <div class="flex items-center gap-2">
        <Button
          size="small"
          :disabled="!activeRecord || !hasActiveItems()"
          @click="templatePreviewOpen = true"
        >
          <IconifyIcon icon="lucide:eye" class="mr-1" /> 查看录入表
        </Button>
        <Button
          v-if="showItemExcelActions"
          size="small"
          :disabled="!canMutateItems()"
          @click="handleDownloadItemTemplate"
        >
          <IconifyIcon icon="lucide:download" class="mr-1" /> 下载模板
        </Button>
        <Button
          v-if="showItemExcelActions && props.enableImport"
          size="small"
          :disabled="!canMutateItems()"
          @click="importModalOpen = true"
        >
          <IconifyIcon icon="lucide:upload" class="mr-1" /> 导入
        </Button>
        <Button
          v-if="showItemExcelActions"
          size="small"
          :disabled="!activeRecord || !hasActiveItems()"
          @click="handleExportItemValues"
        >
          <IconifyIcon icon="lucide:download" class="mr-1" /> 导出
        </Button>
        <QmsFqcScanEntryBox
          v-if="showScanEntry && props.enableScan"
          :current-fqc-id="activeRecord?.id"
          scene="WORKBENCH_HEADER"
          @resolved="handleScanResolved"
        />
        <Button size="small" :loading="loading" @click="refreshTaskList">
          <IconifyIcon icon="lucide:refresh-cw" class="mr-1" /> 刷新
        </Button>
      </div>
    </header>

    <main class="flex min-h-0 flex-1 gap-3 p-3">
      <aside
        v-if="showTaskSidebar"
        class="flex w-[320px] shrink-0 flex-col overflow-hidden rounded border bg-white"
      >
        <div class="border-b p-3">
          <Input
            v-model:value="searchKeyword"
            allow-clear
            placeholder="搜索单号、工单、物料、型号、批次"
          />
        </div>
        <div class="min-h-0 flex-1 space-y-2 overflow-y-auto p-3">
          <button
            v-for="item in filteredTaskList"
            :key="item.fqcNo"
            type="button"
            class="w-full rounded border bg-white p-3 text-left transition hover:border-indigo-400 hover:bg-indigo-50"
            :class="
              activeRecord?.fqcNo === item.fqcNo
                ? 'border-indigo-500 bg-indigo-50'
                : 'border-slate-200'
            "
            @click="handleSelectTask(item)"
          >
            <div class="flex items-center justify-between gap-2">
              <span
                class="truncate font-mono text-sm font-bold text-indigo-700"
              >
                {{ item.fqcNo }}
              </span>
              <Tag :color="statusColor(item.status)" class="!m-0">
                {{ statusLabel(item.status) }}
              </Tag>
            </div>
            <div class="mt-2 grid grid-cols-2 gap-2 text-xs text-slate-500">
              <span class="truncate">{{ item.workOrderNo }}</span>
              <span class="truncate text-right">{{
                item.productBatchNo || '-'
              }}</span>
              <span class="truncate">{{ item.materialCode }}</span>
              <span class="truncate text-right">{{
                item.productModel || '-'
              }}</span>
              <span class="col-span-2 truncate">{{ processLabel(item) }}</span>
            </div>
          </button>
        </div>
      </aside>

      <section
        class="flex min-w-0 flex-1 flex-col overflow-hidden rounded border bg-white"
      >
        <div
          v-if="!activeRecord"
          class="flex h-full flex-col items-center justify-center"
        >
          <Empty description="请从台账中选择成品检验单" />
        </div>

        <template v-else>
          <div class="overflow-x-auto border-b px-4 py-3">
            <div class="flex min-w-[960px] items-center gap-8">
              <div class="min-w-[150px]">
                <div class="text-xs font-bold text-slate-400">FQC单号</div>
                <div class="font-mono text-sm font-bold text-slate-800">
                  {{ activeRecord.fqcNo }}
                </div>
              </div>
              <div class="min-w-[150px]">
                <div class="text-xs font-bold text-slate-400">工单号</div>
                <div class="font-mono text-sm font-bold text-slate-800">
                  {{ activeRecord.workOrderNo || '-' }}
                </div>
              </div>
              <div class="min-w-[150px]">
                <div class="text-xs font-bold text-slate-400">物料编码</div>
                <div class="font-mono text-sm font-bold text-slate-800">
                  {{ activeRecord.materialCode || '-' }}
                </div>
              </div>
              <div class="min-w-[150px]">
                <div class="text-xs font-bold text-slate-400">产品型号</div>
                <div class="font-mono text-sm font-bold text-slate-800">
                  {{ activeRecord.productModel || '-' }}
                </div>
              </div>
              <div class="min-w-[130px]">
                <div class="text-xs font-bold text-slate-400">检测工序</div>
                <div class="text-sm font-bold text-slate-800">
                  {{ processLabel(activeRecord) }}
                </div>
              </div>
              <div class="min-w-[150px]">
                <div class="text-xs font-bold text-slate-400">产品批次</div>
                <div class="font-mono text-sm font-bold text-slate-800">
                  {{ activeRecord.productBatchNo || '-' }}
                </div>
              </div>
              <div class="min-w-[120px]">
                <div class="text-xs font-bold text-slate-400">报检数量</div>
                <div class="text-sm font-bold text-slate-800">
                  {{ formatReportQty(activeRecord) }}
                </div>
              </div>
              <div class="min-w-[120px]">
                <div class="text-xs font-bold text-slate-400">检验人</div>
                <div class="text-sm font-bold text-slate-800">
                  {{ displayInspectorName || '-' }}
                </div>
              </div>
              <div class="min-w-[120px]">
                <div class="text-xs font-bold text-slate-400">审核人</div>
                <div class="text-sm font-bold text-slate-800">
                  {{ activeRecord.qaInspectorName || '-' }}
                </div>
              </div>
              <div class="flex min-w-[170px] items-center gap-2">
                <Tag :color="statusColor(activeRecord.status)" class="!m-0">
                  {{ statusLabel(activeRecord.status) }}
                </Tag>
                <Tag :color="resultColor(activeRecord.judgment)" class="!m-0">
                  {{ resultLabel(activeRecord.judgment) }}
                </Tag>
              </div>
            </div>
            <div
              v-if="activeRecord.lastReturnReason"
              class="mt-3 min-w-[960px] rounded border border-amber-200 bg-amber-50 px-3 py-2 text-sm text-amber-800"
            >
              <span class="font-bold">驳回原因：</span>
              <span>{{ activeRecord.lastReturnReason }}</span>
            </div>
          </div>

          <div class="min-h-0 flex-1 overflow-auto p-3">
            <div class="overflow-x-auto rounded border">
              <table
                class="w-full min-w-[1400px] table-fixed border-collapse text-sm"
              >
                <thead class="bg-slate-50 text-xs text-slate-500">
                  <tr>
                    <th class="w-10 border-b px-2 py-2 text-center"></th>
                    <th class="w-12 border-b px-2 py-2 text-center">序号</th>
                    <th class="w-28 border-b px-2 py-2 text-left">检验项目</th>
                    <th class="w-36 border-b px-2 py-2 text-left">
                      平均值内控
                    </th>
                    <th class="w-36 border-b px-2 py-2 text-left">
                      标准差内控
                    </th>
                    <th class="w-48 border-b px-2 py-2 text-left">位置配置</th>
                    <th class="w-24 border-b px-2 py-2 text-center">填值</th>
                    <th class="w-24 border-b px-2 py-2 text-right">平均值</th>
                    <th class="w-24 border-b px-2 py-2 text-right">标准差</th>
                    <th class="w-24 border-b px-2 py-2 text-center">判定</th>
                    <th class="w-24 border-b px-2 py-2 text-center">检验人</th>
                    <th class="w-28 border-b px-2 py-2 text-center">操作</th>
                  </tr>
                </thead>
                <tbody>
                  <template
                    v-for="(item, index) in displayItems"
                    :key="resolveItemKey(item)"
                  >
                    <tr
                      class="cursor-pointer border-b hover:bg-indigo-50"
                      :class="
                        selectedItemId === resolveItemKey(item)
                          ? 'bg-indigo-50'
                          : ''
                      "
                      @click="selectedItemId = resolveItemKey(item)"
                      @dblclick="handleOpenItemModal(item)"
                    >
                      <td class="px-2 py-2 text-center">
                        <button
                          type="button"
                          class="inline-flex h-6 w-6 items-center justify-center rounded text-slate-500 hover:bg-indigo-100 hover:text-indigo-700"
                          :title="
                            isItemExpanded(item)
                              ? '收起填报内容'
                              : '展开填报内容'
                          "
                          @click.stop="toggleItemTree(item)"
                        >
                          <IconifyIcon
                            :icon="
                              isItemExpanded(item)
                                ? 'lucide:chevron-down'
                                : 'lucide:chevron-right'
                            "
                            class="text-base"
                          />
                        </button>
                      </td>
                      <td class="px-2 py-2 text-center text-slate-500">
                        {{ index + 1 }}
                      </td>
                      <td class="px-2 py-2 font-bold text-slate-800">
                        {{ displayMetricName(item) }}
                      </td>
                      <td class="px-2 py-2 font-mono">
                        <template v-if="isEntryGroupItem(item)">
                          <div
                            v-for="row in groupMetricRows(item)"
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
                              item.avgMinLimit,
                              item.avgMaxLimit,
                              valueUnit(item),
                            )
                          }}
                        </template>
                      </td>
                      <td class="px-2 py-2 font-mono">
                        <template v-if="isEntryGroupItem(item)">
                          <div
                            v-for="row in groupMetricRows(item)"
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
                              item.stdMinLimit,
                              item.stdMaxLimit,
                              valueUnit(item),
                            )
                          }}
                        </template>
                      </td>
                      <td class="px-2 py-2 text-xs text-slate-500">
                        {{ positionSummary(item) }}
                      </td>
                      <td class="px-2 py-2 text-center">
                        {{ countFilled(item) }}/{{
                          resolveEntryRuleSampleSize(item)
                        }}
                      </td>
                      <td class="px-2 py-2 text-right font-mono">
                        <template v-if="isEntryGroupItem(item)">
                          <div
                            v-for="row in groupMetricRows(item)"
                            :key="`avg-value-${row.key}`"
                            class="flex justify-between gap-2 text-xs leading-5"
                          >
                            <span class="font-sans text-slate-500">{{
                              row.name
                            }}</span>
                            <span>{{ row.avgValue }}</span>
                          </div>
                        </template>
                        <template v-else>
                          {{ formatNumber(item.calculatedAvg ?? item.qaAvg) }}
                        </template>
                      </td>
                      <td class="px-2 py-2 text-right font-mono">
                        <template v-if="isEntryGroupItem(item)">
                          <div
                            v-for="row in groupMetricRows(item)"
                            :key="`std-value-${row.key}`"
                            class="flex justify-between gap-2 text-xs leading-5"
                          >
                            <span class="font-sans text-slate-500">{{
                              row.name
                            }}</span>
                            <span>{{ row.stdValue }}</span>
                          </div>
                        </template>
                        <template v-else>
                          {{ formatNumber(item.calculatedStd) }}
                        </template>
                      </td>
                      <td class="px-2 py-2 text-center">
                        <Tag :color="itemReviewColor(item)" class="!m-0">
                          {{ itemReviewLabel(item) }}
                        </Tag>
                      </td>
                      <td class="px-2 py-2 text-center text-slate-700">
                        {{ item.operatorName || '-' }}
                      </td>
                      <td class="px-2 py-2 text-center">
                        <button
                          type="button"
                          class="inline-flex h-7 cursor-pointer items-center justify-center rounded border border-indigo-200 bg-indigo-50 px-3 text-xs font-bold text-indigo-700 hover:bg-indigo-100"
                          @click.stop="handleOpenItemModal(item)"
                          @mousedown.stop.prevent="handleOpenItemModal(item)"
                        >
                          {{ isReadOnly() ? '查看' : '填写' }}
                        </button>
                      </td>
                    </tr>
                    <tr
                      v-if="isItemExpanded(item)"
                      class="border-b bg-slate-50"
                    >
                      <td colspan="12" class="px-4 py-3">
                        <div class="rounded border bg-white p-3 text-xs">
                          <div
                            class="mb-2 flex items-center justify-between gap-2"
                          >
                            <div class="font-bold text-slate-700">
                              {{ displayMetricName(item) }} 填报内容
                            </div>
                            <Tag :color="itemReviewColor(item)" class="!m-0">
                              {{ itemReviewLabel(item) }}
                            </Tag>
                          </div>
                          <div
                            v-if="item.ruleDescription"
                            class="mb-2 rounded border border-amber-100 bg-amber-50 px-3 py-2 text-xs text-amber-800"
                          >
                            <span class="font-bold">填写说明：</span>
                            <span>{{ item.ruleDescription }}</span>
                          </div>
                          <div class="space-y-2">
                            <div
                              v-for="position in buildFillTreeRows(item)"
                              :key="position.code"
                              class="border-l-2 border-indigo-100 pl-3"
                            >
                              <div
                                class="flex items-center gap-2 font-bold text-slate-700"
                              >
                                <IconifyIcon
                                  icon="lucide:map-pin"
                                  class="text-indigo-500"
                                />
                                <span>{{ position.name }}</span>
                              </div>
                              <div class="mt-1 grid gap-1 pl-5">
                                <div
                                  v-for="sample in position.samples"
                                  :key="`${position.code}-${sample.groupNo}`"
                                  class="flex min-w-0 items-center gap-2 rounded bg-slate-50 px-2 py-1"
                                >
                                  <span
                                    class="w-14 shrink-0 font-mono text-slate-500"
                                  >
                                    第{{ sample.groupNo }}组
                                  </span>
                                  <Tag
                                    :color="
                                      sample.filled ? 'success' : 'default'
                                    "
                                    class="!m-0 shrink-0"
                                  >
                                    {{ sample.filled ? '已填' : '未填' }}
                                  </Tag>
                                  <span
                                    class="min-w-0 flex-1 truncate font-mono text-slate-700"
                                  >
                                    {{ sample.text }}
                                  </span>
                                  <span
                                    v-if="sample.filled"
                                    class="shrink-0 text-slate-500"
                                  >
                                    {{ sample.result }}
                                  </span>
                                </div>
                              </div>
                            </div>
                          </div>
                        </div>
                      </td>
                    </tr>
                  </template>
                </tbody>
              </table>
            </div>
          </div>

          <footer
            class="flex shrink-0 items-center justify-between border-t bg-white px-4 py-3"
          >
            <div class="text-xs text-slate-500">
              已完成
              {{
                activeRecord.completedItemCount ??
                activeItems.filter(
                  (item) => item.qaResult === 'OK' || item.qaResult === 'NG',
                ).length
              }}
              / {{ activeRecord.requiredItemCount ?? activeItems.length }} 项
            </div>
            <div class="flex items-center gap-2">
              <Button
                type="primary"
                :disabled="!canSubmitConfirm()"
                :loading="saving"
                @click="handleSubmit"
              >
                <IconifyIcon icon="lucide:send" class="mr-1" /> 提交检测结果
              </Button>
            </div>
          </footer>
        </template>
      </section>
    </main>
    <QmsFqcItemValueInputModal
      v-model:open="inputModalOpen"
      :item="modalItem"
      :item-count="displayItems.length"
      :item-index="modalItemIndex"
      :entry-group-items="modalItem ? entryGroupItems(modalItem) : []"
      :readonly="isReadOnly()"
      :record="activeRecord"
      :saving="saving"
      @recalculate="handleRecalculateModalItem"
      @save="handleSaveModalItem"
    />
    <QmsFqcItemOverviewImportModal
      v-if="props.enableImport"
      v-model:open="importModalOpen"
      :fqc-id="activeRecord?.id"
      :record="activeRecord"
      @success="handleImportSuccess"
    />
    <QmsFqcTemplatePreviewModal
      v-model:open="templatePreviewOpen"
      :items="activeItems"
      :record="activeRecord"
    />
  </div>
</template>
