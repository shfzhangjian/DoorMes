<script lang="ts" setup>
import type { MesHcLotRuleApi } from '#/api/mes/hc/lotrule';

import { computed, nextTick, ref, watch } from 'vue';

import dayjs from 'dayjs';

import { useVbenModal } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';
import {
  Button,
  DatePicker,
  Input,
  InputNumber,
  message,
  Modal as AntModal,
  Select,
  Switch,
  Tooltip,
} from 'ant-design-vue';

import { useVbenForm } from '#/adapter/form';
import { VxeColumn, VxeTable } from '#/adapter/vxe-table';
import {
  createLotRule,
  generateLotNo,
  getLotRuleDetail,
  matchLotRule,
  parseLotNo,
  updateLotRule,
} from '#/api/mes/hc/lotrule';

import { useFormSchema } from '../data';

type SegmentRow = MesHcLotRuleApi.LotRuleSegment & { _rowKey: string };

const emit = defineEmits(['success']);

const formData = ref<MesHcLotRuleApi.LotRule>();
const rowSeed = ref(1);
const openToken = ref(0);
const activePreviewKey = ref('');
const lotRuleSegments = ref<SegmentRow[]>([]);
const invalidSegmentRowKeys = ref<string[]>([]);
const currentSegmentRowKey = ref('');
const pendingSegmentScrollRowKey = ref('');
const segmentTableRef = ref<any>();
const baseExpanded = ref(true);
const previewExpanded = ref(true);
const itemsExpanded = ref(true);
const advancedExpanded = ref(false);
const configTemplate = ref<'CMP_BLACK_MASS' | 'CMP_WHITE_MASS'>('CMP_WHITE_MASS');

const generateModalVisible = ref(false);
const generateBizDate = ref(dayjs());
const generateConsumeSequence = ref(false);
const generateFormValues = ref<Record<string, string>>({});
const generatedLotNo = ref('');

const matchModalVisible = ref(false);
const matchModelCode = ref('');
const matchedRuleText = ref('');

const parseModalVisible = ref(false);
const parseLotNoText = ref('');
const parseResult = ref<Record<string, string>>({});

const segmentTypeOptions = [
  { label: '年份代码', value: 'YEAR_CODE' },
  { label: '月份代码', value: 'MONTH_CODE' },
  { label: '流水号', value: 'SEQ' },
  { label: '分段取样码', value: 'SAMPLE_CODE' },
  { label: '人工输入', value: 'INPUT' },
  { label: '上下文字段', value: 'CONTEXT_FIELD' },
  { label: '固定值', value: 'FIXED' },
];

const sampleCodeOptions = [
  { label: '1段 / P', value: '1' },
  { label: '2段 / Q', value: '2' },
  { label: '3段 / R', value: '3' },
  { label: '4段 / S', value: '4' },
  { label: '5段 / T', value: '5' },
  { label: '6段 / U', value: '6' },
  { label: '7段 / V', value: '7' },
  { label: '8段 / W', value: '8' },
  { label: '9段 / X', value: '9' },
  { label: '10段 / Y', value: '10' },
  { label: '11段 / Z', value: '11' },
];

const getTitle = computed(() => (formData.value?.id ? '编辑批号规则' : '新增批号规则'));

const [Form, formApi] = useVbenForm({
  commonConfig: {
    componentProps: { class: 'w-full' },
    formItemClass: 'col-span-1',
    labelWidth: 120,
  },
  wrapperClass: 'grid-cols-2 gap-x-6',
  layout: 'horizontal',
  schema: useFormSchema(),
  showDefaultActions: false,
});

const readableRuleSummary = computed(() => {
  const values = formApi.getValues() as MesHcLotRuleApi.LotRule;
  const prefix = values.prefix || (configTemplate.value === 'CMP_BLACK_MASS' ? 'C' : 'W');
  return `${values.ruleName || 'CMP量产主批号'}｜配方开工生成｜每张计划 1 个｜${prefix} + 两位年份 + 月码 + ${values.seqLength || 3} 位年度流水 + 产线码`;
});

const nextRowKey = () => `lot-segment-${Date.now()}-${++rowSeed.value}`;

function buildCmpMassSegments(prefix: string): SegmentRow[] {
  return [
    { segmentCode: 'PAD_PREFIX', segmentName: '固定前缀', segmentType: 'FIXED', segmentValue: prefix, segmentLength: prefix.length, sort: 1, enabled: true },
    { segmentCode: 'YEAR', segmentName: '年份', segmentType: 'YEAR_CODE', segmentValue: 'yy', segmentLength: 2, sort: 2, enabled: true },
    { segmentCode: 'MONTH', segmentName: '月份', segmentType: 'MONTH_CODE', segmentValue: 'A_TO_M', segmentLength: 1, sort: 3, enabled: true },
    { segmentCode: 'ANNUAL_SEQ', segmentName: '年度流水', segmentType: 'SEQ', segmentLength: 3, counterType: 'ANNUAL_BATCH', counterDimensionExpr: 'YEAR', valuePolicy: 'LEFT_ZERO', sort: 4, enabled: true },
    { segmentCode: 'LINE', segmentName: '产线代码', segmentType: 'CONTEXT_FIELD', segmentLength: 1, sourceField: 'batchLineCode', sort: 5, enabled: true },
  ].map((item) => normalizeSegmentRow(item));
}

async function applyTemplate(template: 'CMP_BLACK_MASS' | 'CMP_WHITE_MASS', forceRuleName = false) {
  configTemplate.value = template;
  const isBlack = template === 'CMP_BLACK_MASS';
  const prefix = isBlack ? 'C' : 'W';
  const current = (await formApi.getValues()) as MesHcLotRuleApi.LotRule;
  await formApi.setValues({
    __uiMode: 'SIMPLE',
    ruleName: forceRuleName || !current.ruleName ? (isBlack ? '黑垫量产主批号' : '白垫量产主批号') : current.ruleName,
    ruleCode: current.ruleCode,
    bizType: 'FG_LOT',
    ruleMode: 'LOT',
    productCategoryCode: isBlack ? 'BLACK_PAD' : 'WHITE_PAD',
    prodType: 'MASS',
    modelMatchMode: 'ALL',
    modelMatchValue: undefined,
    priority: 100,
    generationTrigger: 'FORMULA_START',
    generationScope: 'PLAN_ROOT',
    batchCardinality: 'ONE',
    yearCodeMode: 'yy',
    monthCodeMode: 'A_TO_M',
    prefix,
    seqLength: 3,
    seqStart: 1,
    seqStep: 1,
    resetCycle: 'YEAR',
    allowPreview: true,
    allowParse: true,
    allowManualOverride: false,
  });
  lotRuleSegments.value = buildCmpMassSegments(prefix);
  advancedExpanded.value = false;
  recalculateSegmentTable();
}

async function toggleAdvancedSettings() {
  advancedExpanded.value = !advancedExpanded.value;
  await formApi.setValues({ __uiMode: advancedExpanded.value ? 'ADVANCED' : 'SIMPLE' });
  if (advancedExpanded.value) {
    nextTick(() => recalculateSegmentTable());
  }
}

const normalizeSegmentRow = (row?: Partial<MesHcLotRuleApi.LotRuleSegment>): SegmentRow =>
  ({
    ...(row || {}),
    _rowKey: (row as any)?._rowKey || nextRowKey(),
    enabled: row?.enabled ?? true,
    sort: row?.sort || lotRuleSegments.value.length + 1,
  }) as SegmentRow;

const orderedSegments = computed(() =>
  [...lotRuleSegments.value].sort((a, b) => (a.sort || 0) - (b.sort || 0)),
);

const previewSegments = computed(() =>
  orderedSegments.value
    .filter((item) => item.enabled !== false)
    .map((item, index) => ({
      key: item._rowKey,
      segmentName: item.segmentName || `分段${index + 1}`,
      lengthLabel: buildSegmentLengthLabel(item),
      sampleValue: buildPreviewValue(item),
      segmentTypeLabel:
        segmentTypeOptions.find((option) => option.value === item.segmentType)?.label || '未定义',
      flexValue: Math.max(resolvePreviewFlex(item), 1),
    })),
);

const previewLotNo = computed(() =>
  previewSegments.value.length
    ? previewSegments.value.map((item) => item.sampleValue).join('')
    : '暂无规则段，请先维护批号规则分段',
);

const dynamicGenerateFields = computed(() =>
  orderedSegments.value.filter((item) => ['INPUT', 'SAMPLE_CODE', 'CONTEXT_FIELD'].includes((item.segmentType || '').toUpperCase())),
);

function resolvePreviewFlex(row: SegmentRow) {
  if (row.segmentType === 'YEAR_CODE') {
    return buildPreviewValue(row).length;
  }
  if (row.segmentType === 'SEQ') {
    return Number((formApi.getValues() as any).seqLength || 3);
  }
  if (row.segmentType === 'FIXED') {
    return Math.max((row.segmentValue || '').length, 1);
  }
  if (row.segmentType === 'INPUT' && /^\d+$/.test(row.segmentValue || '')) {
    return Number(row.segmentValue);
  }
  return 1;
}

function buildSegmentLengthLabel(row: SegmentRow) {
  const type = (row.segmentType || '').toUpperCase();
  if (type === 'YEAR_CODE') {
    return `${buildPreviewValue(row).length} 位`;
  }
  if (type === 'MONTH_CODE' || type === 'SAMPLE_CODE') {
    return '1 位';
  }
  if (type === 'SEQ') {
    const formValues = formApi.getValues() as any;
    return `${formValues.seqLength || 3} 位`;
  }
  if (type === 'FIXED') {
    return `${(row.segmentValue || '').length || 1} 位`;
  }
  if (/^\d+$/.test(row.segmentValue || '')) {
    return `${row.segmentValue} 位`;
  }
  return '变长';
}

function buildPreviewValue(row: SegmentRow) {
  const formValues = formApi.getValues() as any;
  const type = (row.segmentType || '').toUpperCase();
  switch (type) {
    case 'YEAR_CODE':
      return resolveYearPreview(formValues.yearCodeMode || 'yy');
    case 'MONTH_CODE':
      return 'A';
    case 'SEQ':
      return String(formValues.seqStart || 1).padStart(formValues.seqLength || 3, '0');
    case 'SAMPLE_CODE':
      return 'P';
    case 'FIXED':
      return row.segmentValue || '-';
    default:
      return row.segmentName || '输入值';
  }
}

function resolveYearPreview(mode: string) {
  const year = dayjs().year();
  const yy = String(year).slice(-2);
  const actualMode = mode || 'yy';
  return actualMode.replaceAll('yyyy', String(year)).replaceAll('yy', yy);
}

function setActivePreview(rowKey?: string) {
  activePreviewKey.value = rowKey || '';
}

function recalculateSegmentTable() {
  nextTick(() => segmentTableRef.value?.recalculate?.(true));
}

function applyCurrentSegmentRow(rowKey?: string) {
  currentSegmentRowKey.value = rowKey || '';
  nextTick(() => {
    const row = lotRuleSegments.value.find((item) => item._rowKey === rowKey);
    if (!row) return;
    segmentTableRef.value?.setCurrentRow?.(row);
  });
}

function scrollToSegmentTableRow(rowKey?: string) {
  if (!rowKey) return;
  const doScroll = () => {
    const row = lotRuleSegments.value.find((item) => item._rowKey === rowKey);
    if (!row) return;
    segmentTableRef.value?.scrollToRow?.(row);
  };
  nextTick(() => {
    doScroll();
    setTimeout(doScroll, 60);
  });
}

function focusSegmentRow(rowKey?: string) {
  if (!rowKey) return;
  nextTick(() => {
    const holder = document.querySelector(
      `.hc-lot-rule-modal [data-segment-focus-key="${rowKey}"]`,
    ) as HTMLElement | null;
    if (!holder) return;
    holder.scrollIntoView({ block: 'center', behavior: 'smooth' });
    (holder.querySelector('input') as HTMLElement | null)?.focus?.();
  });
}

function scrollToSegmentRow(rowKey?: string) {
  if (!rowKey) return;
  setActivePreview(rowKey);
  applyCurrentSegmentRow(rowKey);
  nextTick(() => {
    const row = lotRuleSegments.value.find((item) => item._rowKey === rowKey);
    if (row) {
      segmentTableRef.value?.scrollToRow?.(row);
    }
    focusSegmentRow(rowKey);
  });
}

function isEmptySegmentRow(row: Partial<MesHcLotRuleApi.LotRuleSegment>) {
  return ![
    row.segmentCode,
    row.segmentName,
    row.segmentType,
    row.segmentValue,
    row.delimiter,
  ].some((value) => value !== undefined && value !== null && String(value).trim() !== '');
}

function validateSegmentRows(rows: SegmentRow[]) {
  const emptyIndex = rows.findIndex((row) => isEmptySegmentRow(row));
  if (emptyIndex >= 0) {
    invalidSegmentRowKeys.value = [rows[emptyIndex]!._rowKey];
    scrollToSegmentRow(rows[emptyIndex]!._rowKey);
    message.warning(`批号规则段第 ${emptyIndex + 1} 行为空，请填写后提交或删除空行`);
    return false;
  }
  const invalidIndex = rows.findIndex((row) => {
    const type = (row.segmentType || '').toUpperCase();
    if (!row.segmentCode?.trim() || !row.segmentName?.trim() || !type) {
      return true;
    }
    if (type === 'FIXED') {
      return !row.segmentValue?.trim();
    }
    if (type === 'INPUT') {
      return !/^\d+$/.test(row.segmentValue || '');
    }
    return false;
  });
  if (invalidIndex >= 0) {
    invalidSegmentRowKeys.value = [rows[invalidIndex]!._rowKey];
    scrollToSegmentRow(rows[invalidIndex]!._rowKey);
    message.warning(`批号规则段第 ${invalidIndex + 1} 行请完善规则段编码、规则段名称、规则段类型和长度/固定值`);
    return false;
  }
  invalidSegmentRowKeys.value = [];
  return true;
}

function addSegmentRow() {
  const row = normalizeSegmentRow();
  lotRuleSegments.value.push(row);
  setActivePreview(row._rowKey);
  applyCurrentSegmentRow(row._rowKey);
  invalidSegmentRowKeys.value = [];
  itemsExpanded.value = true;
  pendingSegmentScrollRowKey.value = row._rowKey;
  recalculateSegmentTable();
  scrollToSegmentTableRow(row._rowKey);
  focusSegmentRow(row._rowKey);
}

function copySegmentRow(index: number) {
  const source = lotRuleSegments.value[index];
  if (!source) return;
  const cloned = normalizeSegmentRow({ ...source, id: 0, _rowKey: undefined });
  lotRuleSegments.value.splice(index + 1, 0, cloned);
  resequenceSegments();
  invalidSegmentRowKeys.value = [];
  applyCurrentSegmentRow(cloned._rowKey);
  pendingSegmentScrollRowKey.value = cloned._rowKey;
  recalculateSegmentTable();
  scrollToSegmentRow(cloned._rowKey);
}

function removeSegmentRow(index: number) {
  const removed = lotRuleSegments.value[index];
  lotRuleSegments.value.splice(index, 1);
  resequenceSegments();
  invalidSegmentRowKeys.value = [];
  if (currentSegmentRowKey.value === removed?._rowKey) {
    applyCurrentSegmentRow(lotRuleSegments.value[Math.min(index, lotRuleSegments.value.length - 1)]?._rowKey || '');
  }
  if (activePreviewKey.value === removed?._rowKey) {
    activePreviewKey.value = lotRuleSegments.value[0]?._rowKey || '';
  }
  recalculateSegmentTable();
}

function moveSegmentRow(index: number, direction: 'up' | 'down') {
  const target = direction === 'up' ? index - 1 : index + 1;
  if (target < 0 || target >= lotRuleSegments.value.length) return;
  const rows = [...lotRuleSegments.value];
  const current = rows[index];
  rows[index] = rows[target]!;
  rows[target] = current!;
  lotRuleSegments.value = rows;
  resequenceSegments();
  applyCurrentSegmentRow(rows[target]?._rowKey || '');
  recalculateSegmentTable();
}

function resequenceSegments() {
  lotRuleSegments.value.forEach((row, rowIndex) => {
    row.sort = rowIndex + 1;
  });
}

async function resetFormState() {
  formData.value = undefined;
  lotRuleSegments.value = [];
  invalidSegmentRowKeys.value = [];
  activePreviewKey.value = '';
  generateModalVisible.value = false;
  parseModalVisible.value = false;
  generateBizDate.value = dayjs();
  generateConsumeSequence.value = false;
  generateFormValues.value = {};
  generatedLotNo.value = '';
  parseLotNoText.value = '';
  parseResult.value = {};
  pendingSegmentScrollRowKey.value = '';
  advancedExpanded.value = false;
  configTemplate.value = 'CMP_WHITE_MASS';
  await formApi.resetForm();
  await applyTemplate('CMP_WHITE_MASS', true);
}

function getGenerateInputLabel(row: SegmentRow) {
  return row.segmentName || row.segmentCode || '未命名分段';
}

async function openGenerateModal() {
  const rows = orderedSegments.value.map((row, index) => normalizeSegmentRow({ ...row, sort: index + 1 }));
  if (!rows.length) {
    message.warning('请先维护批号规则分段后再测试生成');
    return;
  }
  if (!validateSegmentRows(rows)) return;
  const values: Record<string, string> = {};
  dynamicGenerateFields.value.forEach((row) => {
    values[row.sourceField || row.segmentCode || ''] = row.segmentType === 'SAMPLE_CODE' ? '1' : '';
  });
  generateFormValues.value = values;
  generatedLotNo.value = '';
  generateBizDate.value = dayjs();
  generateConsumeSequence.value = false;
  generateModalVisible.value = true;
}

async function openMatchModal() {
  const values = (await formApi.getValues()) as MesHcLotRuleApi.LotRule;
  if (!values.bizType || !values.generationTrigger || !values.generationScope) {
    message.warning('请先完善业务对象、生成时机和生成粒度');
    return;
  }
  matchModelCode.value = values.modelMatchMode === 'EXACT' ? values.modelMatchValue || '' : '';
  matchedRuleText.value = '';
  matchModalVisible.value = true;
}

async function handleMatchRule() {
  const values = (await formApi.getValues()) as MesHcLotRuleApi.LotRule;
  const result = await matchLotRule({
    bizType: values.bizType || 'FG_LOT',
    productCategoryCode: values.productCategoryCode,
    prodType: values.prodType,
    generationTrigger: values.generationTrigger || 'FORMULA_START',
    generationScope: values.generationScope || 'PLAN_ROOT',
    modelCode: matchModelCode.value,
  });
  matchedRuleText.value = `${result.ruleCode}（V${result.versionNo || 1}）- ${result.ruleName}`;
}

async function handleGenerateLotNo() {
  const formValues = (await formApi.getValues()) as MesHcLotRuleApi.LotRule;
  const payload: MesHcLotRuleApi.GenerateReq = {
    ruleId: formData.value?.id,
    ruleCode: formValues.ruleCode,
    ruleName: formValues.ruleName,
    ruleMode: formValues.ruleMode,
    yearCodeMode: formValues.yearCodeMode,
    monthCodeMode: formValues.monthCodeMode,
    seqLength: formValues.seqLength,
    seqStart: formValues.seqStart,
    seqStep: formValues.seqStep,
    resetCycle: formValues.resetCycle,
    sampleSegmentRule: formValues.sampleSegmentRule,
    bizDate: (generateBizDate.value || dayjs()).format('YYYY-MM-DD'),
    consumeSequence: generateConsumeSequence.value,
    lotRuleSegments: orderedSegments.value.map(({ _rowKey, ...rest }) => ({ ...rest })),
    inputValues: generateFormValues.value,
  };
  const resp = await generateLotNo(payload);
  generatedLotNo.value = resp.lotNo || '';
  message.success('批号生成成功');
}

async function openParseModal() {
  const rows = orderedSegments.value.map((row, index) => normalizeSegmentRow({ ...row, sort: index + 1 }));
  if (!rows.length) {
    message.warning('请先维护批号规则分段后再测试解析');
    return;
  }
  if (!validateSegmentRows(rows)) return;
  parseLotNoText.value = previewLotNo.value === '暂无规则段，请先维护批号规则分段' ? '' : previewLotNo.value;
  parseResult.value = {};
  parseModalVisible.value = true;
}

async function handleParseLotNo() {
  if (!parseLotNoText.value.trim()) {
    message.warning('请输入待解析批号');
    return;
  }
  const formValues = (await formApi.getValues()) as MesHcLotRuleApi.LotRule;
  const resp = await parseLotNo({
    ruleId: formData.value?.id,
    ruleCode: formValues.ruleCode,
    ruleName: formValues.ruleName,
    seqLength: formValues.seqLength,
    yearCodeMode: formValues.yearCodeMode,
    monthCodeMode: formValues.monthCodeMode,
    lotRuleSegments: orderedSegments.value.map(({ _rowKey, ...rest }) => ({ ...rest })),
    lotNo: parseLotNoText.value.trim(),
  });
  parseResult.value = resp.segmentValues || {};
  message.success('批号解析成功');
}

watch(
  previewSegments,
  (segments) => {
    if (!segments.length) {
      activePreviewKey.value = '';
      return;
    }
    if (!activePreviewKey.value || !segments.some((item) => item.key === activePreviewKey.value)) {
      activePreviewKey.value = segments[0]!.key;
    }
  },
  { immediate: true },
);

const [Modal, modalApi] = useVbenModal({
  closeOnClickModal: false,
  closable: true,
  showCancelButton: false,
  closeOnPressEscape: false,
  fullscreen: true,
  fullscreenButton: false,
  class: 'hc-lot-rule-modal',
  async onConfirm() {
    const { valid } = await formApi.validate();
    if (!valid) {
      return;
    }
    const rows = orderedSegments.value.map((row, index) => normalizeSegmentRow({ ...row, sort: index + 1 }));
    if (!validateSegmentRows(rows)) return;
    modalApi.lock();
    try {
      const rawData = (await formApi.getValues()) as MesHcLotRuleApi.LotRule & { __uiMode?: string };
      const { __uiMode, ...data } = rawData;
      if (__uiMode !== 'ADVANCED') {
        const prefix = data.prefix?.trim();
        if (!prefix) {
          message.warning('请填写批号前缀');
          return;
        }
        lotRuleSegments.value = buildCmpMassSegments(prefix);
      }
      const finalRows = orderedSegments.value.map((row, index) => normalizeSegmentRow({ ...row, sort: index + 1 }));
      data.lotRuleSegments = finalRows
        .filter((row) => !isEmptySegmentRow(row))
        .map((row, index) => {
          const { _rowKey, ...rest } = row as any;
          return {
            ...rest,
            ruleCode: data.ruleCode,
            sort: row.sort || index + 1,
          };
        });
      await (formData.value?.id ? updateLotRule(data) : createLotRule(data));
      await modalApi.close();
      emit('success');
      message.success('保存成功');
    } finally {
      modalApi.unlock();
    }
  },
  async onOpenChange(isOpen: boolean) {
    if (!isOpen) {
      openToken.value += 1;
      return;
    }
    const currentToken = ++openToken.value;
    const data = modalApi.getData<MesHcLotRuleApi.LotRule>();
    if (!data?.id) {
      await resetFormState();
      return;
    }
    modalApi.lock();
    try {
      const detail = await getLotRuleDetail(data.id);
      if (currentToken !== openToken.value) return;
      formData.value = detail;
      lotRuleSegments.value = (detail.lotRuleSegments || []).map((item) => normalizeSegmentRow(item));
      const prefix = lotRuleSegments.value.find((item) => item.segmentCode === 'PAD_PREFIX' && item.segmentType === 'FIXED')?.segmentValue
        || lotRuleSegments.value.find((item) => item.segmentType === 'FIXED')?.segmentValue
        || detail.prefix
        || (detail.productCategoryCode === 'BLACK_PAD' ? 'C' : 'W');
      configTemplate.value = detail.productCategoryCode === 'BLACK_PAD' ? 'CMP_BLACK_MASS' : 'CMP_WHITE_MASS';
      advancedExpanded.value = false;
      await formApi.setValues({ ...detail, prefix, __uiMode: 'SIMPLE' });
      invalidSegmentRowKeys.value = [];
      recalculateSegmentTable();
    } finally {
      modalApi.unlock();
    }
  },
  async onClosed() {
    await resetFormState();
  },
});

function toggleBaseExpanded() {
  baseExpanded.value = !baseExpanded.value;
}

function togglePreviewExpanded() {
  previewExpanded.value = !previewExpanded.value;
}

function toggleItemsExpanded() {
  itemsExpanded.value = !itemsExpanded.value;
  if (itemsExpanded.value) {
    nextTick(() => {
      recalculateSegmentTable();
      if (pendingSegmentScrollRowKey.value) {
        const rowKey = pendingSegmentScrollRowKey.value;
        pendingSegmentScrollRowKey.value = '';
        scrollToSegmentTableRow(rowKey);
        focusSegmentRow(rowKey);
      }
    });
  }
}
</script>

<template>
  <Modal :title="getTitle">
    <div class="hc-master-modal">
      <div class="hc-master-modal__scroll">
        <div class="hc-master-panel hc-master-panel--base">
          <div class="hc-master-panel__header">
            <span class="hc-master-panel__title-chip">基本信息</span>
            <div class="hc-master-panel__header-spacer" />
            <Button type="text" size="small" class="hc-master-panel__toggle-btn" @click="toggleBaseExpanded">
              <IconifyIcon :icon="baseExpanded ? 'lucide:chevron-up' : 'lucide:chevron-down'" />
            </Button>
          </div>
          <div v-show="baseExpanded" class="hc-master-panel__body hc-master-panel__body--base">
            <div class="hc-master-panel__base-box">
              <div class="hc-lot-rule-template-bar">
                <div>
                  <div class="hc-lot-rule-template-bar__label">配置模板</div>
                  <div class="hc-lot-rule-template-bar__hint">普通用户只需选择模板并维护业务字段，规则段和流水维度由系统生成。</div>
                </div>
                <Select
                  v-model:value="configTemplate"
                  class="hc-lot-rule-template-bar__select"
                  :options="[
                    { label: 'CMP量产主批号 - 白垫', value: 'CMP_WHITE_MASS' },
                    { label: 'CMP量产主批号 - 黑垫', value: 'CMP_BLACK_MASS' },
                  ]"
                  @change="applyTemplate(configTemplate, true)"
                />
              </div>
              <div class="hc-lot-rule-readable-summary">{{ readableRuleSummary }}</div>
              <Form />
              <div class="hc-lot-rule-advanced-toggle">
                <Button type="link" @click="toggleAdvancedSettings">
                  {{ advancedExpanded ? '收起高级设置' : '展开高级设置' }}
                </Button>
                <span>高级设置包含业务对象、型号匹配、优先级、规则段和人工改号开关。</span>
              </div>
            </div>
          </div>
        </div>

        <div class="hc-master-panel hc-master-panel--preview">
          <div class="hc-master-panel__header">
            <span class="hc-master-panel__title-chip">编码预览</span>
            <div class="hc-master-panel__header-spacer" />
            <div class="hc-master-panel__header-actions">
              <Button size="small" @click="openParseModal">测试解析</Button>
              <Button size="small" @click="openMatchModal">测试匹配</Button>
              <Button type="primary" ghost size="small" @click="openGenerateModal">测试生成</Button>
              <Button type="text" size="small" class="hc-master-panel__toggle-btn" @click="togglePreviewExpanded">
                <IconifyIcon :icon="previewExpanded ? 'lucide:chevron-up' : 'lucide:chevron-down'" />
              </Button>
            </div>
          </div>
          <div v-show="previewExpanded" class="hc-master-panel__body hc-master-panel__body--preview">
            <div v-if="!previewSegments.length" class="hc-lot-rule-preview__empty">暂无规则段，请先新增规则段</div>
            <div v-else class="hc-lot-rule-preview__segments">
              <div
                v-for="segment in previewSegments"
                :key="segment.key"
                class="hc-lot-rule-preview__segment"
                :class="{ 'is-active': activePreviewKey === segment.key }"
                :style="{ flex: `${segment.flexValue} 1 0` }"
                @click="scrollToSegmentRow(segment.key)"
                @mouseenter="setActivePreview(segment.key)"
              >
                <div class="hc-lot-rule-preview__segment-length">{{ segment.lengthLabel }}</div>
                <div class="hc-lot-rule-preview__segment-name">{{ segment.segmentName }}</div>
                <div class="hc-lot-rule-preview__segment-value">{{ segment.sampleValue }}</div>
                <div class="hc-lot-rule-preview__segment-type">{{ segment.segmentTypeLabel }}</div>
              </div>
            </div>
          </div>
        </div>

        <div v-show="advancedExpanded" class="hc-master-panel hc-master-panel--items">
          <div class="hc-master-panel__header">
            <span class="hc-master-panel__title-chip">规则段</span>
            <div class="hc-master-panel__header-spacer" />
            <div v-show="itemsExpanded" class="hc-master-panel__header-actions">
              <Button type="primary" size="small" @click="addSegmentRow">新增规则段</Button>
            </div>
            <Button type="text" size="small" class="hc-master-panel__toggle-btn" @click="toggleItemsExpanded">
              <IconifyIcon :icon="itemsExpanded ? 'lucide:chevron-up' : 'lucide:chevron-down'" />
            </Button>
          </div>
          <div v-show="itemsExpanded" class="hc-master-panel__body hc-master-panel__body--items">
            <div class="hc-master-modal__table-wrap">
              <VxeTable
                ref="segmentTableRef"
                :data="lotRuleSegments"
                :row-class-name="({ row }) => invalidSegmentRowKeys.includes(row._rowKey) ? 'is-invalid-row' : currentSegmentRowKey === row._rowKey ? 'is-current-row' : ''"
                auto-resize
                border
                stripe
                :round="false"
                size="small"
                height="100%"
                show-overflow
                row-id="_rowKey"
                @cell-click="({ row }) => applyCurrentSegmentRow(row._rowKey)"
              >
                <VxeColumn field="sort" title="顺序" width="80" align="center" header-align="center">
                  <template #default="{ row }">
                    <InputNumber v-model:value="row.sort" :min="1" :precision="0" class="w-full" />
                  </template>
                </VxeColumn>
                <VxeColumn field="segmentCode" title="*规则段编码" min-width="160" header-align="center">
                  <template #default="{ row }">
                    <div :data-segment-focus-key="row._rowKey">
                      <Input v-model:value="row.segmentCode" placeholder="请输入规则段编码" @focus="setActivePreview(row._rowKey)" />
                    </div>
                  </template>
                </VxeColumn>
                <VxeColumn field="segmentName" title="*规则段名称" min-width="180" header-align="center">
                  <template #default="{ row }">
                    <Input v-model:value="row.segmentName" placeholder="请输入规则段名称" @focus="setActivePreview(row._rowKey)" />
                  </template>
                </VxeColumn>
                <VxeColumn field="segmentType" title="*规则段类型" width="160" header-align="center">
                  <template #default="{ row }">
                    <Select
                      v-model:value="row.segmentType"
                      :options="segmentTypeOptions"
                      class="w-full"
                      placeholder="请选择规则段类型"
                      @focus="setActivePreview(row._rowKey)"
                    />
                  </template>
                </VxeColumn>
                <VxeColumn field="segmentValue" title="长度/固定值" min-width="140" header-align="center">
                  <template #default="{ row }">
                    <Input
                      v-model:value="row.segmentValue"
                      :placeholder="row.segmentType === 'FIXED' ? '请输入固定值' : row.segmentType === 'INPUT' ? '请输入长度，如 3' : '可留空'"
                      @focus="setActivePreview(row._rowKey)"
                    />
                  </template>
                </VxeColumn>
                <VxeColumn field="segmentLength" title="长度" width="96" header-align="center">
                  <template #default="{ row }">
                    <InputNumber v-model:value="row.segmentLength" :min="1" :precision="0" class="w-full" />
                  </template>
                </VxeColumn>
                <VxeColumn field="counterType" title="流水类型" min-width="130" header-align="center">
                  <template #default="{ row }">
                    <Input v-model:value="row.counterType" :disabled="row.segmentType !== 'SEQ'" placeholder="ANNUAL_BATCH" />
                  </template>
                </VxeColumn>
                <VxeColumn field="counterDimensionExpr" title="重置维度" min-width="120" header-align="center">
                  <template #default="{ row }">
                    <Input v-model:value="row.counterDimensionExpr" :disabled="row.segmentType !== 'SEQ'" placeholder="YEAR" />
                  </template>
                </VxeColumn>
                <VxeColumn field="sourceField" title="上下文字段" min-width="150" header-align="center">
                  <template #default="{ row }">
                    <Input v-model:value="row.sourceField" placeholder="如 batchLineCode" @focus="setActivePreview(row._rowKey)" />
                  </template>
                </VxeColumn>
                <VxeColumn field="valuePolicy" title="补位方式" min-width="110" header-align="center">
                  <template #default="{ row }">
                    <Input v-model:value="row.valuePolicy" placeholder="LEFT_ZERO" />
                  </template>
                </VxeColumn>
                <VxeColumn field="delimiter" title="分隔符" width="100" header-align="center">
                  <template #default="{ row }">
                    <Input v-model:value="row.delimiter" placeholder="无" @focus="setActivePreview(row._rowKey)" />
                  </template>
                </VxeColumn>
                <VxeColumn field="enabled" title="启用" width="90" align="center" header-align="center">
                  <template #default="{ row }">
                    <Switch v-model:checked="row.enabled" checked-children="是" un-checked-children="否" />
                  </template>
                </VxeColumn>
                <VxeColumn title="操作" width="156" fixed="right" align="center" header-align="center">
                  <template #default="{ $rowIndex, row }">
                    <div class="hc-row-actions" @mouseenter="setActivePreview(row._rowKey)">
                      <Tooltip title="复制">
                        <Button type="text" class="hc-icon-btn" @click="copySegmentRow($rowIndex)">
                          <IconifyIcon icon="carbon:copy" class="hc-action-icon" />
                        </Button>
                      </Tooltip>
                      <Tooltip title="上移">
                        <Button type="text" class="hc-icon-btn" @click="moveSegmentRow($rowIndex, 'up')">
                          <IconifyIcon icon="carbon:arrow-up" class="hc-action-icon" />
                        </Button>
                      </Tooltip>
                      <Tooltip title="下移">
                        <Button type="text" class="hc-icon-btn" @click="moveSegmentRow($rowIndex, 'down')">
                          <IconifyIcon icon="carbon:arrow-down" class="hc-action-icon" />
                        </Button>
                      </Tooltip>
                      <Tooltip title="删除">
                        <Button danger type="text" class="hc-icon-btn hc-icon-btn--danger" @click="removeSegmentRow($rowIndex)">
                          <IconifyIcon icon="carbon:trash-can" class="hc-action-icon" />
                        </Button>
                      </Tooltip>
                    </div>
                  </template>
                </VxeColumn>
              </VxeTable>
            </div>
          </div>
        </div>
      </div>
    </div>
    <AntModal
      v-model:open="matchModalVisible"
      :mask-closable="false"
      :width="560"
      title="批号规则测试匹配"
      ok-text="调用规则匹配器"
      cancel-text="关闭"
      @ok="handleMatchRule"
      @cancel="matchModalVisible = false"
    >
      <div class="hc-test-panel">
        <div class="hc-test-panel__result">
          <div class="hc-test-panel__label">产品型号（可选）</div>
          <Input v-model:value="matchModelCode" placeholder="用于精确、前缀或列表匹配测试" />
        </div>
        <div class="hc-test-panel__result">
          <div class="hc-test-panel__label">匹配结果</div>
          <Input :value="matchedRuleText" readonly />
        </div>
      </div>
    </AntModal>

    <AntModal
      v-model:open="generateModalVisible"
      :mask-closable="false"
      :keyboard="false"
      :width="760"
      title="批号测试生成"
      ok-text="调用后台生成"
      cancel-text="关闭"
      @ok="handleGenerateLotNo"
      @cancel="generateModalVisible = false"
    >
      <div class="hc-test-panel">
        <div class="hc-test-panel__head">
          <div class="hc-test-panel__field">
            <div class="hc-test-panel__label">业务日期</div>
            <DatePicker v-model:value="generateBizDate" class="w-full" />
          </div>
          <div class="hc-test-panel__field">
            <div class="hc-test-panel__label">占用流水号</div>
            <Switch v-model:checked="generateConsumeSequence" checked-children="是" un-checked-children="否" />
          </div>
        </div>
        <div class="hc-test-panel__body">
          <div v-for="row in dynamicGenerateFields" :key="row._rowKey" class="hc-test-panel__row">
            <div class="hc-test-panel__row-label">{{ getGenerateInputLabel(row) }}</div>
            <div class="hc-test-panel__row-field">
              <Select
                v-if="row.segmentType === 'SAMPLE_CODE'"
                v-model:value="generateFormValues[row.sourceField || row.segmentCode || '']"
                :options="sampleCodeOptions"
                class="w-full"
                placeholder="请选择取样段次"
              />
              <Input
                v-else
                v-model:value="generateFormValues[row.sourceField || row.segmentCode || '']"
                :placeholder="`请输入${getGenerateInputLabel(row)}`"
              />
            </div>
          </div>
        </div>
        <div class="hc-test-panel__result">
          <div class="hc-test-panel__label">生成结果</div>
          <Input :value="generatedLotNo" readonly />
        </div>
      </div>
    </AntModal>

    <AntModal
      v-model:open="parseModalVisible"
      :mask-closable="false"
      :keyboard="false"
      :width="760"
      title="批号测试解析"
      ok-text="调用后台解析"
      cancel-text="关闭"
      @ok="handleParseLotNo"
      @cancel="parseModalVisible = false"
    >
      <div class="hc-test-panel">
        <div class="hc-test-panel__result">
          <div class="hc-test-panel__label">待解析批号</div>
          <Input v-model:value="parseLotNoText" placeholder="请输入待解析批号" />
        </div>
        <div class="hc-parse-result">
          <div v-for="row in orderedSegments" :key="row._rowKey" class="hc-parse-result__row">
            <div class="hc-parse-result__label">{{ row.segmentName || row.segmentCode }}</div>
            <div class="hc-parse-result__value">{{ parseResult[row.segmentCode || ''] || '-' }}</div>
          </div>
        </div>
      </div>
    </AntModal>
  </Modal>
</template>

<style>
.hc-lot-rule-modal .ant-modal-body {
  height: 100%;
  overflow: hidden;
}
</style>

<style scoped>
.hc-master-modal {
  height: 100%;
  min-height: 0;
  overflow: hidden;
}
.hc-master-modal__scroll {
  height: 100%;
  overflow: auto;
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding-right: 4px;
}
.hc-master-panel {
  border: 1px solid var(--ant-color-border-secondary);
  border-radius: 8px;
  background: #fff;
  overflow: visible;
  min-width: 0;
  padding: 0;
}
.hc-master-panel__header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 12px 0;
}
.hc-master-panel__header-spacer {
  flex: 1;
}
.hc-master-panel__header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}
.hc-master-panel__toggle-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 28px;
  width: 28px;
  height: 28px;
  padding: 0;
  color: #6b7280;
}
.hc-master-panel__title-chip {
  display: inline-flex;
  align-items: center;
  min-width: 76px;
  height: 24px;
  padding: 0 14px;
  border-radius: 3px;
  background: #8b8b8b;
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  line-height: 24px;
}
.hc-master-panel__body {
  padding: 2px 12px 12px;
}
.hc-master-panel__body--base {
  overflow: hidden;
}
.hc-master-panel__base-box {
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  padding: 12px;
  background: #fff;
}
.hc-master-panel__body--preview {
  min-height: 96px;
  overflow: hidden;
}
.hc-master-panel__body--items {
  height: 500px;
  min-height: 500px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}
.hc-lot-rule-template-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 12px;
  padding: 12px;
  border: 1px solid #d6e4ff;
  border-radius: 8px;
  background: #f7fbff;
}
.hc-lot-rule-template-bar__label {
  color: var(--ant-color-text);
  font-weight: 600;
}
.hc-lot-rule-template-bar__hint {
  margin-top: 4px;
  color: var(--ant-color-text-secondary);
  font-size: 12px;
}
.hc-lot-rule-template-bar__select {
  min-width: 250px;
}
.hc-lot-rule-readable-summary {
  margin-bottom: 14px;
  padding: 10px 12px;
  border-left: 3px solid var(--ant-color-primary);
  background: var(--ant-color-fill-quaternary);
  color: var(--ant-color-text);
  font-size: 13px;
}
.hc-lot-rule-advanced-toggle {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 6px;
  color: var(--ant-color-text-secondary);
  font-size: 12px;
}
.hc-master-modal__table-wrap {
  flex: 1;
  overflow: hidden;
}
.hc-master-modal__table-wrap :deep(.vxe-table),
.hc-master-modal__table-wrap :deep(.vxe-table--border-wrapper),
.hc-master-modal__table-wrap :deep(.vxe-table--main-wrapper) {
  height: 100%;
  border-radius: 0;
}
.hc-master-modal__table-wrap :deep(.is-invalid-row) {
  background-color: #fff1f0;
}
.hc-master-modal__table-wrap :deep(.is-current-row) {
  background-color: #e6f4ff;
}
.hc-row-actions {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 2px;
  flex-wrap: nowrap;
  white-space: nowrap;
}
.hc-icon-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 2px;
  min-width: 24px;
  padding-inline: 2px;
  color: #1677ff;
}
.hc-icon-btn :deep(svg) {
  font-size: 14px;
}
.hc-icon-btn:hover {
  color: #4096ff;
}
.hc-icon-btn--danger {
  color: #ff4d4f;
}
.hc-action-icon {
  font-size: 16px;
  line-height: 1;
}
.hc-lot-rule-preview__empty {
  display: flex;
  min-height: 84px;
  align-items: center;
  justify-content: center;
  color: var(--ant-color-text-secondary);
}
.hc-lot-rule-preview__segments {
  display: flex;
  gap: 8px;
  overflow-x: auto;
}
.hc-lot-rule-preview__segment {
  display: grid;
  min-width: 120px;
  grid-template-rows: 16px 20px 1fr 14px;
  cursor: pointer;
  border: 1px solid #d9d9d9;
  border-radius: 8px;
  background: #fff;
  padding: 4px 10px;
  transition: all 0.2s ease;
}
.hc-lot-rule-preview__segment.is-active {
  border-color: var(--ant-color-primary);
  box-shadow: 0 0 0 2px rgb(22 119 255 / 12%);
  background: #f0f7ff;
}
.hc-lot-rule-preview__segment-length {
  font-size: 12px;
  color: var(--ant-color-primary);
  font-weight: 600;
}
.hc-lot-rule-preview__segment-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 14px;
  font-weight: 600;
}
.hc-lot-rule-preview__segment-value {
  display: flex;
  align-items: center;
  font-size: 12px;
  color: var(--ant-color-text);
}
.hc-lot-rule-preview__segment-type {
  font-size: 12px;
  color: var(--ant-color-text-secondary);
}
.hc-test-panel {
  display: grid;
  row-gap: 16px;
}
.hc-test-panel__head {
  display: grid;
  grid-template-columns: 1fr 180px;
  gap: 12px;
}
.hc-test-panel__field,
.hc-test-panel__result {
  display: grid;
  row-gap: 8px;
}
.hc-test-panel__label {
  font-size: 13px;
  font-weight: 600;
  color: var(--ant-color-text);
}
.hc-test-panel__body {
  display: grid;
  row-gap: 12px;
}
.hc-test-panel__row {
  display: grid;
  grid-template-columns: 180px minmax(0, 1fr);
  gap: 12px;
  align-items: center;
}
.hc-test-panel__row-label {
  color: var(--ant-color-text);
}
.hc-parse-result {
  display: grid;
  max-height: 360px;
  row-gap: 8px;
  overflow: auto;
}
.hc-parse-result__row {
  display: grid;
  grid-template-columns: 180px minmax(0, 1fr);
  gap: 12px;
  align-items: center;
  border-bottom: 1px dashed var(--ant-color-border-secondary);
  padding-bottom: 6px;
}
.hc-parse-result__label {
  font-weight: 600;
  color: var(--ant-color-text);
}
.hc-parse-result__value {
  color: var(--ant-color-text-secondary);
  word-break: break-all;
}
</style>
