<script lang="ts" setup>
import type { MesFqcApi } from '#/api/mes/quality/fqc';

import { computed, onMounted, reactive, ref, watch } from 'vue';

import { IconifyIcon } from '@vben/icons';

import {
  Alert,
  Button,
  Empty,
  Input,
  InputNumber,
  message,
  Modal,
  Progress,
  Radio,
  Segmented,
  Select,
  Tag,
} from 'ant-design-vue';

import {
  getFqcDefectCodeOptions,
  recalculateFqcProgramEntry,
  saveFqcProgramEntry,
  submitFqcProgramEntry,
} from '#/api/mes/quality/fqc';
import {
  compactEntryRulePositionNames,
  hasEntryRulePositions,
  resolveEntryRuleExpectedSampleCount,
  resolveEntryRulePositions,
  resolveEntryRuleRepeatCount,
  resolveEntryRuleSampleSize,
} from '#/api/mes/quality/entry-rule';
import { formatReportQty, resolveFqcOperationLabel } from '../data';

defineOptions({ name: 'QmsFqcProgramEntryWorkbench' });

const props = defineProps<{
  disabled?: boolean;
  items: MesFqcApi.FqcItem[];
  record: MesFqcApi.FqcRecord;
}>();

const emit = defineEmits<{
  recordUpdated: [record: MesFqcApi.FqcRecord];
  submitted: [record: MesFqcApi.FqcRecord];
}>();

interface FqcEntryRow {
  sampleGroupNo: number;
  samplePosition: string;
  samplePositionCode: string;
}

interface DefectCodeOption {
  defectCode: string;
  defectLevel?: string;
  defectName: string;
  label: string;
  value: number;
}

const activeStep = ref('BASIC_INFO');
const localItems = reactive<MesFqcApi.FqcItem[]>([]);
const saving = ref(false);
const defectCodeOptions = ref<DefectCodeOption[]>([]);

onMounted(loadDefectCodeOptions);

async function loadDefectCodeOptions() {
  const list = await getFqcDefectCodeOptions();
  defectCodeOptions.value = (list || [])
    .filter((item) => item.type === 'ITEM' && item.status === 1 && item.id)
    .map((item) => ({
      defectCode: item.code,
      defectLevel: item.level,
      defectName: item.name,
      label: `${item.code} - ${item.name}`,
      value: item.id,
    }));
}

const stepDefs = [
  { code: 'BASIC_INFO', icon: 'lucide:clipboard-list', name: '基础信息' },
  { code: 'NAP_RAW', icon: 'lucide:layers-2', name: 'Nap 未磨皮' },
  { code: 'NAP_POLISHED', icon: 'lucide:scan-line', name: 'Nap 磨皮后' },
  { code: 'EMBOSSING', icon: 'lucide:combine', name: '背胶压槽' },
  { code: 'FINAL_PRODUCTS', icon: 'lucide:badge-check', name: '成品检验' },
  { code: 'GROOVE_DEPTH', icon: 'lucide:grid-3x3', name: '沟深检验' },
  { code: 'CONFIRM', icon: 'lucide:send', name: '异常与确认' },
];

watch(
  () => props.items,
  () => {
    localItems.splice(0, localItems.length, ...props.items.map(normalizeItem));
    activeStep.value = props.record.currentStepCode || 'BASIC_INFO';
  },
  { deep: true, immediate: true },
);

const activeItems = computed(() => {
  if (activeStep.value === 'BASIC_INFO' || activeStep.value === 'CONFIRM') {
    return [];
  }
  return localItems.filter(
    (item) => resolveStepCode(item) === activeStep.value,
  );
});

const totalCount = computed(() => localItems.length);
const completedCount = computed(
  () =>
    localItems.filter(
      (item) => item.qaResult === 'OK' || item.qaResult === 'NG',
    ).length,
);
const abnormalCount = computed(
  () => localItems.filter((item) => item.qaResult === 'NG').length,
);
const progress = computed(() =>
  totalCount.value === 0
    ? 0
    : Math.round((completedCount.value / totalCount.value) * 100),
);
const abnormalItems = computed(() =>
  localItems.filter((item) => item.qaResult === 'NG'),
);

function getNgRows(item: MesFqcApi.FqcItem) {
  return (item.qaValues || []).filter((value) => value?.sampleResult === 'NG');
}

function normalizeItem(item: MesFqcApi.FqcItem) {
  const copy = structuredClone(item);
  copy.stepCode = resolveStepCode(copy);
  copy.stepName =
    copy.stepName || stepDefs.find((step) => step.code === copy.stepCode)?.name;
  copy.inputComponent = copy.inputComponent || resolveInputComponent(copy);
  copy.requiredSampleCount = resolveExpectedCount(copy);
  copy.qaValues = buildRows(copy);
  return copy;
}

function resolveStepCode(item: MesFqcApi.FqcItem) {
  if (item.stepCode) return item.stepCode;
  const section = item.sheetSectionCode || '';
  if (section.includes('NAP_RAW')) return 'NAP_RAW';
  if (section.includes('NAP_POLISHED')) return 'NAP_POLISHED';
  if (section.includes('BEFORE') || section.includes('AFTER'))
    return 'EMBOSSING';
  if (section.includes('FINAL')) return 'FINAL_PRODUCTS';
  if ((item.sheetMetricCode || item.inspectionItem || '').includes('沟深'))
    return 'GROOVE_DEPTH';
  return 'FINAL_PRODUCTS';
}

function resolveInputComponent(item: MesFqcApi.FqcItem) {
  if (item.itemType === 'QUALITATIVE') return 'QUALITATIVE_JUDGMENT';
  if (item.valueTemplate === 'DENSITY_CALC') return 'DENSITY_GROUP';
  if (item.valueTemplate === 'COMPRESSION_CALC') return 'COMPRESSION_GROUP';
  if ((item.sheetMetricCode || item.inspectionItem || '').includes('沟深'))
    return 'GROOVE_DEPTH_MATRIX';
  return 'SINGLE_VALUE_LIST';
}

function resolveExpectedCount(item: MesFqcApi.FqcItem) {
  return resolveEntryRuleExpectedSampleCount(item);
}

function buildRows(item: MesFqcApi.FqcItem) {
  const current = item.qaValues || [];
  const expectedRows = buildExpectedRows(item);
  return expectedRows.map((expectedRow: FqcEntryRow, index: number) => {
    const value = current[index];
    const row = value && typeof value === 'object' ? { ...value } : { value };
    return {
      ...expectedRow,
      sampleSeq: index + 1,
      ...row,
      defectCodeIds: Array.isArray(row.defectCodeIds)
        ? row.defectCodeIds
        : (row.defects || [])
            .map((defect: MesFqcApi.FqcSampleDefect) => defect.defectCodeId)
            .filter(Boolean),
      defects: Array.isArray(row.defects) ? row.defects : [],
      sampleGroupNo: row.sampleGroupNo || expectedRow.sampleGroupNo,
      samplePosition: row.samplePosition || expectedRow.samplePosition,
      samplePositionCode:
        row.samplePositionCode || expectedRow.samplePositionCode,
      sampleResult: row.sampleResult || 'PENDING',
    };
  });
}

function buildExpectedRows(item: MesFqcApi.FqcItem) {
  const sampleSize = resolveEntryRuleSampleSize(item);
  const positions = resolveEntryRulePositions(item);
  const repeatCount = resolveEntryRuleRepeatCount(item);
  const rows = positions.flatMap((position) =>
    Array.from({ length: repeatCount }).map(
      (_, repeatIndex: number): FqcEntryRow => ({
        sampleGroupNo: repeatIndex + 1,
        samplePosition: position.name,
        samplePositionCode: position.code,
      }),
    ),
  );
  const expectedCount = hasEntryRulePositions(item)
    ? resolveEntryRuleExpectedSampleCount(item)
    : sampleSize;
  return Array.from({ length: expectedCount }).map(
    (_, index): FqcEntryRow =>
      rows[index] || {
        sampleGroupNo: (index % repeatCount) + 1,
        samplePosition: `${index + 1}`,
        samplePositionCode: `P${index + 1}`,
      },
  );
}

function formatPositionSummary(item: MesFqcApi.FqcItem) {
  const positions = resolveEntryRulePositions(item);
  const positionText = compactEntryRulePositionNames(positions);
  const repeatCount = resolveEntryRuleRepeatCount(item);
  const summary =
    repeatCount > 1
      ? `${positionText}，每点 ${repeatCount} 组`
      : `${positionText}，共 ${positions.length} 个位置`;
  return item.itemType === 'QUALITATIVE' ? `定性判定：${summary}` : summary;
}

function getStepStatus(stepCode: string) {
  if (stepCode === 'BASIC_INFO') return 'COMPLETE';
  if (stepCode === 'CONFIRM')
    return abnormalCount.value > 0
      ? 'ABNORMAL'
      : progress.value === 100
        ? 'COMPLETE'
        : 'FILLING';
  const items = localItems.filter((item) => resolveStepCode(item) === stepCode);
  if (items.length === 0) return 'EMPTY';
  if (items.some((item) => item.qaResult === 'NG')) return 'ABNORMAL';
  if (items.every((item) => item.qaResult === 'OK')) return 'COMPLETE';
  if (items.some((item) => item.qaResult !== '-')) return 'FILLING';
  return 'EMPTY';
}

function statusColor(status: string) {
  if (status === 'COMPLETE') return 'success';
  if (status === 'ABNORMAL') return 'error';
  if (status === 'FILLING') return 'processing';
  return 'default';
}

function recalculateItem(item: MesFqcApi.FqcItem) {
  const rows = item.qaValues || [];
  let complete = true;
  let hasNg = false;
  const metricValues: number[] = [];
  rows.forEach((row: any) => {
    if (!isRowComplete(item, row)) {
      complete = false;
      row.sampleResult = 'PENDING';
      clearRowDefects(row);
      return;
    }
    const value = calculateRowMetric(item, row);
    if (item.itemType === 'QUANTITATIVE' && Number.isFinite(value)) {
      metricValues.push(value as number);
      row.resultValue = value;
      const ng = isValueNg(item, value as number);
      row.sampleResult = ng ? 'NG' : 'OK';
      if (!ng) clearRowDefects(row);
      hasNg = hasNg || ng;
    } else if (item.itemType === 'QUALITATIVE') {
      row.sampleResult = row.value;
      if (row.sampleResult !== 'NG') clearRowDefects(row);
      hasNg = hasNg || row.value === 'NG';
    }
  });

  item.completedSampleCount = rows.filter(
    (row: any) => row.sampleResult === 'OK' || row.sampleResult === 'NG',
  ).length;
  item.abnormalSampleCount = rows.filter(
    (row: any) => row.sampleResult === 'NG',
  ).length;
  if (!complete) {
    item.qaResult = '-';
    item.inputStatus = item.completedSampleCount ? 'FILLING' : 'EMPTY';
    return;
  }
  if (metricValues.length > 0) {
    const avg =
      metricValues.reduce((sum, value) => sum + value, 0) / metricValues.length;
    const variance =
      metricValues.length > 1
        ? metricValues.reduce((sum, value) => sum + (value - avg) ** 2, 0) /
          (metricValues.length - 1)
        : 0;
    const std = Math.sqrt(variance);
    item.calculatedAvg = round(avg);
    item.calculatedStd = round(std);
    item.calculatedMin = round(Math.min(...metricValues));
    item.calculatedMax = round(Math.max(...metricValues));
    hasNg = hasNg || isAggregateNg(item, avg, std);
  }
  item.qaResult = hasNg ? 'NG' : 'OK';
  item.inputStatus = hasNg ? 'ABNORMAL' : 'COMPLETE';
}

function isRowComplete(item: MesFqcApi.FqcItem, row: any) {
  if (item.itemType === 'QUALITATIVE')
    return row.value === 'OK' || (row.value === 'NG' && !!row.remark);
  if (item.inputComponent === 'DENSITY_GROUP')
    return filled(row.thicknessMm) && filled(row.weightG);
  if (item.inputComponent === 'COMPRESSION_GROUP')
    return filled(row.t1Mm) && filled(row.t2Mm) && filled(row.t3Mm);
  return filled(row.value);
}

function filled(value: any) {
  return value !== undefined && value !== null && value !== '';
}

function calculateRowMetric(item: MesFqcApi.FqcItem, row: any) {
  if (item.inputComponent === 'DENSITY_GROUP') {
    const thickness = Number(row.thicknessMm);
    const weight = Number(row.weightG);
    const diameter = Number(row.diameterMm || 39);
    if (thickness > 0 && weight >= 0 && diameter > 0) {
      row.densityValue = round(
        weight / (((thickness / 10) * 3.14 * diameter * diameter) / 4),
      );
      return row.densityValue;
    }
    return undefined;
  }
  if (item.inputComponent === 'COMPRESSION_GROUP') {
    const t1 = Number(row.t1Mm);
    const t2 = Number(row.t2Mm);
    const t3 = Number(row.t3Mm);
    if (
      Number.isFinite(t1) &&
      Number.isFinite(t2) &&
      Number.isFinite(t3) &&
      t1 !== 0 &&
      t1 !== t2
    ) {
      row.compressionRate = round(((t1 - t2) / t1) * 100);
      row.compressionElasticityRate = round(((t3 - t2) / (t1 - t2)) * 100);
      return item.judgmentMetric === 'COMPRESSION_ELASTICITY_RATE'
        ? row.compressionElasticityRate
        : row.compressionRate;
    }
    return undefined;
  }
  return Number(row.value);
}

function isValueNg(item: MesFqcApi.FqcItem, value: number) {
  return (
    (item.minValueLimit !== undefined && value < item.minValueLimit) ||
    (item.maxValueLimit !== undefined && value > item.maxValueLimit)
  );
}

function isAggregateNg(item: MesFqcApi.FqcItem, avg: number, std: number) {
  return (
    (item.avgMinLimit !== undefined && avg < item.avgMinLimit) ||
    (item.avgMaxLimit !== undefined && avg > item.avgMaxLimit) ||
    (item.stdMinLimit !== undefined && std < item.stdMinLimit) ||
    (item.stdMaxLimit !== undefined && std > item.stdMaxLimit)
  );
}

function round(value: number) {
  return Number.isFinite(value) ? Number(value.toFixed(6)) : undefined;
}

function filterDefectOption(input: string, option?: any) {
  const keyword = String(input || '').trim().toLowerCase();
  if (!keyword) return true;
  return [option?.label, option?.defectCode, option?.defectName]
    .filter(Boolean)
    .some((text) => String(text).toLowerCase().includes(keyword));
}

function handleDefectChange(row: any, values: any) {
  const selected = new Set(values || []);
  row.defects = defectCodeOptions.value
    .filter((option) => selected.has(option.value))
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

function clearRowDefects(row: any) {
  row.defectCodeIds = [];
  row.defects = [];
  row.defectCode = undefined;
  row.defectName = undefined;
}

function findNgRowWithoutDefect() {
  for (const item of localItems) {
    const row = (item.qaValues || []).find(
      (sample: any) =>
        sample.sampleResult === 'NG' &&
        (!Array.isArray(sample.defects) || sample.defects.length === 0),
    );
    if (row) {
      return { item, row };
    }
  }
}

function recalculateAll() {
  localItems.forEach((item) => recalculateItem(item));
}

function buildRecord() {
  recalculateAll();
  return {
    ...props.record,
    currentStepCode: activeStep.value,
    entryLayout: 'PROGRAM_FORM' as const,
    entryMode: 'MANUAL' as const,
    items: localItems.map((item) => ({
      ...item,
      qaValues: item.qaValues || [],
    })),
  };
}

async function handleSave() {
  saving.value = true;
  try {
    const draft = buildRecord();
    const missingDefect = findNgRowWithoutDefect();
    if (missingDefect) {
      message.warning(
        `【${missingDefect.item.inspectionItem}】样本 ${missingDefect.row.sampleSeq} 判定不合格时必须选择缺陷码`,
      );
      return;
    }
    const record = await saveFqcProgramEntry(draft);
    emit('recordUpdated', record);
    message.success('程序录入草稿已保存');
  } finally {
    saving.value = false;
  }
}

async function handleRecalculate() {
  saving.value = true;
  try {
    const draft = buildRecord();
    const missingDefect = findNgRowWithoutDefect();
    if (missingDefect) {
      message.warning(
        `【${missingDefect.item.inspectionItem}】样本 ${missingDefect.row.sampleSeq} 判定不合格时必须选择缺陷码`,
      );
      return;
    }
    const record = await recalculateFqcProgramEntry(draft);
    emit('recordUpdated', record);
    message.success('已按服务端规则重算');
  } finally {
    saving.value = false;
  }
}

async function handleSubmit() {
  const record = buildRecord();
  const incomplete = localItems.find(
    (item) => item.qaResult === '-' || item.qaResult === 'PENDING',
  );
  if (incomplete) {
    message.warning(`请先完成【${incomplete.inspectionItem}】后再提交`);
    activeStep.value = resolveStepCode(incomplete);
    return;
  }
  const ngWithoutRemark = localItems.some((item) =>
    (item.qaValues || []).some(
      (row: any) => row.sampleResult === 'NG' && !row.remark,
    ),
  );
  if (ngWithoutRemark) {
    message.warning('NG 样本必须填写备注或异常说明');
    return;
  }
  const missingDefect = findNgRowWithoutDefect();
  if (missingDefect) {
    message.warning(
      `【${missingDefect.item.inspectionItem}】样本 ${missingDefect.row.sampleSeq} 判定不合格时必须选择缺陷码`,
    );
    activeStep.value = resolveStepCode(missingDefect.item);
    return;
  }
  Modal.confirm({
    title: '提交成品检验审核',
    content: '提交后将通知质检主管审核，审核前单据锁定只读。',
    onOk: async () => {
      const updated = await submitFqcProgramEntry(record);
      emit('submitted', updated);
      message.success('已提交成品检验审核，等待质检主管审核');
    },
  });
}

function nextStep() {
  const index = stepDefs.findIndex((step) => step.code === activeStep.value);
  activeStep.value = stepDefs[Math.min(index + 1, stepDefs.length - 1)]!.code;
}

function previousStep() {
  const index = stepDefs.findIndex((step) => step.code === activeStep.value);
  activeStep.value = stepDefs[Math.max(index - 1, 0)]!.code;
}
</script>

<template>
  <div class="flex h-full min-h-0 overflow-hidden bg-slate-50">
    <aside class="flex w-[230px] shrink-0 flex-col border-r bg-white">
      <div class="border-b p-4">
        <div class="text-xs font-bold text-slate-400">程序录入进度</div>
        <Progress
          :percent="progress"
          size="small"
          :status="abnormalCount ? 'exception' : 'active'"
        />
        <div class="mt-2 flex justify-between text-xs text-slate-500">
          <span>{{ completedCount }}/{{ totalCount }} 项完成</span>
          <span v-if="abnormalCount" class="font-bold text-red-600"
            >{{ abnormalCount }} 异常</span
          >
        </div>
      </div>
      <button
        v-for="step in stepDefs"
        :key="step.code"
        class="flex items-center justify-between border-b px-4 py-3 text-left transition hover:bg-slate-50"
        :class="
          activeStep === step.code
            ? 'bg-indigo-50 text-indigo-700'
            : 'text-slate-600'
        "
        @click="activeStep = step.code"
      >
        <span class="flex items-center gap-2 text-sm font-bold">
          <IconifyIcon :icon="step.icon" />
          {{ step.name }}
        </span>
        <Tag :color="statusColor(getStepStatus(step.code))" class="!m-0">
          {{
            getStepStatus(step.code) === 'COMPLETE'
              ? '完成'
              : getStepStatus(step.code) === 'ABNORMAL'
                ? '异常'
                : getStepStatus(step.code) === 'FILLING'
                  ? '填写中'
                  : '未填'
          }}
        </Tag>
      </button>
    </aside>

    <section class="flex min-w-0 flex-1 flex-col">
      <header
        class="flex shrink-0 items-center justify-between border-b bg-white px-4 py-3"
      >
        <div>
          <div class="text-base font-black text-slate-800">
            {{ stepDefs.find((step) => step.code === activeStep)?.name }}
          </div>
          <div class="mt-1 text-xs text-slate-500">
            {{ props.record.fqcNo }} /
            {{ props.record.materialCode || props.record.materialName }} /
            {{ props.record.productBatchNo }}
          </div>
        </div>
        <div class="flex gap-2">
          <Button :loading="saving" @click="handleSave">
            <IconifyIcon icon="lucide:save" class="mr-1" />保存草稿
          </Button>
          <Button :loading="saving" @click="handleRecalculate">
            <IconifyIcon icon="lucide:calculator" class="mr-1" />重算
          </Button>
          <Button
            type="primary"
            :disabled="props.disabled"
            @click="handleSubmit"
          >
            <IconifyIcon icon="lucide:send" class="mr-1" />提交判定
          </Button>
        </div>
      </header>

      <main class="custom-scrollbar min-h-0 flex-1 overflow-y-auto p-4">
        <div v-if="activeStep === 'BASIC_INFO'" class="grid grid-cols-4 gap-3">
          <div
            v-for="field in [
              ['FQC单号', props.record.fqcNo],
              ['工单号', props.record.workOrderNo],
              [
                '物料编码',
                props.record.materialCode || props.record.materialName,
              ],
              ['产品批次', props.record.productBatchNo],
              ['报检数量', formatReportQty(props.record)],
              ['规格', props.record.specification],
              ['工序', resolveFqcOperationLabel(props.record)],
            ]"
            :key="field[0]"
            class="rounded border bg-white p-3"
          >
            <div class="text-xs font-bold text-slate-400">{{ field[0] }}</div>
            <div
              class="mt-2 min-h-6 break-words text-sm font-bold text-slate-700"
            >
              {{ field[1] || '-' }}
            </div>
          </div>
        </div>

        <div v-else-if="activeStep === 'CONFIRM'" class="space-y-3">
          <Alert
            :type="
              abnormalItems.length
                ? 'error'
                : progress === 100
                  ? 'success'
                  : 'warning'
            "
            show-icon
            :message="
              abnormalItems.length
                ? '存在异常项，提交后将通知质检主管审核'
                : progress === 100
                  ? '所有项目已完成，可提交审核'
                  : '仍有项目未完成'
            "
          />
          <div class="rounded border bg-white">
            <div class="border-b px-4 py-3 text-sm font-black text-slate-700">
              异常汇总
            </div>
            <div
              v-if="!abnormalItems.length"
              class="p-4 text-sm text-slate-500"
            >
              暂无异常项
            </div>
            <div
              v-for="item in abnormalItems"
              :key="item.id || item.inspectionItem"
              class="border-b p-4 last:border-b-0"
            >
              <div class="font-bold text-red-600">
                {{ item.inspectionItem }}
              </div>
              <div class="mt-2 grid grid-cols-3 gap-2">
                <div
                  v-for="row in getNgRows(item)"
                  :key="row.sampleSeq"
                  class="rounded bg-red-50 p-2 text-xs"
                >
                  <div>
                    样本 {{ row.sampleSeq }}：{{
                      row.resultValue ?? row.value ?? row.densityValue ?? '-'
                    }}
                  </div>
                  <Input
                    v-model:value="row.remark"
                    class="mt-2"
                    placeholder="填写异常说明"
                  />
                </div>
              </div>
            </div>
          </div>
        </div>

        <Empty
          v-else-if="!activeItems.length"
          description="当前步骤暂无配置项目"
        />

        <div v-else class="space-y-3">
          <div
            v-for="item in activeItems"
            :key="item.id || item.inspectionItem"
            class="rounded border bg-white"
          >
            <div class="flex items-center justify-between border-b px-4 py-3">
              <div>
                <div class="font-black text-slate-800">
                  {{ item.inspectionItem }}
                </div>
                <div class="mt-1 text-xs text-slate-500">
                  {{ item.standardDesc || '未配置标准说明' }}
                </div>
                <div
                  v-if="item.ruleDescription"
                  class="mt-1 text-xs font-bold text-amber-700"
                >
                  填写说明：{{ item.ruleDescription }}
                </div>
                <div class="mt-1 text-xs font-bold text-slate-500">
                  {{ formatPositionSummary(item) }}
                </div>
              </div>
              <div class="flex items-center gap-2">
                <Tag color="blue">{{ item.inputComponent }}</Tag>
                <Tag
                  :color="
                    item.qaResult === 'OK'
                      ? 'success'
                      : item.qaResult === 'NG'
                        ? 'error'
                        : 'default'
                  "
                >
                  {{ item.qaResult === '-' ? '待填写' : item.qaResult }}
                </Tag>
              </div>
            </div>
            <div class="p-4">
              <div class="mb-3 grid grid-cols-4 gap-2 text-xs">
                <div class="rounded bg-slate-50 p-2">
                  下限：{{ item.minValueLimit ?? '-' }}
                </div>
                <div class="rounded bg-slate-50 p-2">
                  上限：{{ item.maxValueLimit ?? '-' }}
                </div>
                <div class="rounded bg-slate-50 p-2">
                  平均：{{ item.calculatedAvg ?? '-' }}
                </div>
                <div class="rounded bg-slate-50 p-2">
                  标准差：{{ item.calculatedStd ?? '-' }}
                </div>
              </div>

              <div v-if="item.itemType === 'QUALITATIVE'" class="space-y-2">
                <div
                  class="grid grid-cols-[64px_120px_180px_280px_1fr] gap-3 text-xs font-bold text-slate-500"
                >
                  <span>序号</span><span>位置</span><span>判定</span
                  ><span>缺陷码</span><span>备注</span>
                </div>
                <div
                  v-for="row in item.qaValues"
                  :key="row.sampleSeq"
                  class="grid grid-cols-[64px_120px_180px_280px_1fr] items-center gap-3"
                >
                  <span class="text-sm font-bold text-slate-500"
                    >#{{ row.sampleSeq }}</span
                  >
                  <Input v-model:value="row.samplePosition" size="small" />
                  <Radio.Group
                    v-model:value="row.value"
                    button-style="solid"
                    @change="recalculateItem(item)"
                  >
                    <Radio.Button value="OK">合格</Radio.Button>
                    <Radio.Button value="NG">不合格</Radio.Button>
                  </Radio.Group>
                  <Select
                    v-if="row.value === 'NG' || row.sampleResult === 'NG'"
                    v-model:value="row.defectCodeIds"
                    :options="defectCodeOptions"
                    mode="multiple"
                    show-search
                    option-filter-prop="label"
                    :filter-option="filterDefectOption"
                    placeholder="请选择缺陷码"
                    class="w-full"
                    @change="(values) => handleDefectChange(row, values)"
                  />
                  <span v-else class="text-xs text-slate-400">-</span>
                  <Input
                    v-model:value="row.remark"
                    placeholder="不合格时必填备注"
                    @change="recalculateItem(item)"
                  />
                </div>
              </div>

              <div v-else class="space-y-2">
                <div
                  class="grid grid-cols-[64px_96px_64px_1fr_90px_280px_1fr] gap-2 text-xs font-bold text-slate-500"
                >
                  <span>序号</span><span>点位</span><span>组次</span
                  ><span>输入</span><span>判定</span><span>缺陷码</span
                  ><span>备注</span>
                </div>
                <div
                  v-for="row in item.qaValues"
                  :key="row.sampleSeq"
                  class="grid grid-cols-[64px_96px_64px_1fr_90px_280px_1fr] items-center gap-2"
                >
                  <span class="text-sm font-bold text-slate-500"
                    >#{{ row.sampleSeq }}</span
                  >
                  <Input v-model:value="row.samplePosition" size="small" />
                  <Tag class="!m-0 text-center"
                    >第{{ row.sampleGroupNo || 1 }}组</Tag
                  >
                  <div
                    v-if="item.inputComponent === 'DENSITY_GROUP'"
                    class="grid grid-cols-3 gap-2"
                  >
                    <InputNumber
                      v-model:value="row.thicknessMm"
                      class="w-full"
                      size="small"
                      placeholder="厚度mm"
                      @change="recalculateItem(item)"
                    />
                    <InputNumber
                      v-model:value="row.weightG"
                      class="w-full"
                      size="small"
                      placeholder="重量g"
                      @change="recalculateItem(item)"
                    />
                    <InputNumber
                      :value="row.densityValue"
                      class="w-full"
                      size="small"
                      disabled
                      placeholder="密度"
                    />
                  </div>
                  <div
                    v-else-if="item.inputComponent === 'COMPRESSION_GROUP'"
                    class="grid grid-cols-5 gap-2"
                  >
                    <InputNumber
                      v-model:value="row.t1Mm"
                      class="w-full"
                      size="small"
                      placeholder="T1"
                      @change="recalculateItem(item)"
                    />
                    <InputNumber
                      v-model:value="row.t2Mm"
                      class="w-full"
                      size="small"
                      placeholder="T2"
                      @change="recalculateItem(item)"
                    />
                    <InputNumber
                      v-model:value="row.t3Mm"
                      class="w-full"
                      size="small"
                      placeholder="T3"
                      @change="recalculateItem(item)"
                    />
                    <InputNumber
                      :value="row.compressionRate"
                      class="w-full"
                      size="small"
                      disabled
                      placeholder="压缩率"
                    />
                    <InputNumber
                      :value="row.compressionElasticityRate"
                      class="w-full"
                      size="small"
                      disabled
                      placeholder="弹性率"
                    />
                  </div>
                  <div v-else class="flex gap-2">
                    <Segmented
                      v-if="item.inputComponent === 'GROOVE_DEPTH_MATRIX'"
                      v-model:value="row.sampleAxis"
                      :options="['X', 'Y']"
                      size="small"
                    />
                    <InputNumber
                      v-model:value="row.value"
                      class="w-full"
                      size="small"
                      placeholder="实测值"
                      @change="recalculateItem(item)"
                    />
                  </div>
                  <Tag
                    :color="
                      row.sampleResult === 'OK'
                        ? 'success'
                        : row.sampleResult === 'NG'
                          ? 'error'
                          : 'default'
                    "
                    class="!m-0 text-center"
                  >
                    {{ row.sampleResult || '-' }}
                  </Tag>
                  <Select
                    v-if="row.sampleResult === 'NG'"
                    v-model:value="row.defectCodeIds"
                    :options="defectCodeOptions"
                    mode="multiple"
                    show-search
                    option-filter-prop="label"
                    :filter-option="filterDefectOption"
                    placeholder="请选择缺陷码"
                    class="w-full"
                    @change="(values) => handleDefectChange(row, values)"
                  />
                  <span v-else class="text-xs text-slate-400">-</span>
                  <Input
                    v-model:value="row.remark"
                    size="small"
                    placeholder="备注"
                  />
                </div>
              </div>
            </div>
          </div>
        </div>
      </main>

      <footer class="flex shrink-0 justify-between border-t bg-white px-4 py-3">
        <Button @click="previousStep"
          ><IconifyIcon icon="lucide:arrow-left" class="mr-1" />上一步</Button
        >
        <Button type="primary" ghost @click="nextStep"
          >下一步<IconifyIcon icon="lucide:arrow-right" class="ml-1"
        /></Button>
      </footer>
    </section>
  </div>
</template>

<style scoped>
.custom-scrollbar::-webkit-scrollbar {
  width: 6px;
}
.custom-scrollbar::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 4px;
}
</style>
