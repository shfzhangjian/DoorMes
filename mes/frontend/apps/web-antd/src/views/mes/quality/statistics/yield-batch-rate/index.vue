<script lang="ts" setup>
import type { TableColumnsType } from 'ant-design-vue';

import type { EchartsUIType } from '@vben/plugins/echarts';

import type { MesQmsYieldAnalysisV2Api } from '#/api/mes/quality/statistics/yield-analysis-v2';

import {
  computed,
  nextTick,
  onBeforeUnmount,
  onMounted,
  reactive,
  ref,
} from 'vue';

import { Page } from '@vben/common-ui';
import { EchartsUI, useEcharts } from '@vben/plugins/echarts';

import {
  Button,
  DatePicker,
  Input,
  message,
  Select,
  Table,
} from 'ant-design-vue';
import dayjs from 'dayjs';

import {
  getYieldAnalysisV2List,
  getYieldAnalysisV2TargetModelOptions,
  syncYieldAnalysisV2,
} from '#/api/mes/quality/statistics/yield-analysis-v2';

defineOptions({ name: 'MesQmsYieldBatchRate' });

type PivotRow = MesQmsYieldAnalysisV2Api.ProcessPivotRow;
type PivotStage = MesQmsYieldAnalysisV2Api.ProcessPivotStage;

interface BatchRateRow {
  batchNo: string;
  isPlaceholder?: boolean;
  key: string;
  metricLabel: string;
  modelCode: string;
  sourceCount: number;
  stageCode: string;
  yieldRate?: number;
}

interface BatchRateAccumulator {
  batchNo: string;
  metricLabel: string;
  modelCode: string;
  sourceCount: number;
  stageCode: string;
  totalWeight: number;
  weightedRate: number;
}

const processOptions = [
  { label: '全部工序', value: 'ALL' },
  { label: '配料', value: 'FORMULA' },
  { label: '湿法', value: 'WET' },
  { label: '磨皮', value: 'ROUGH_GRINDING' },
  { label: '粘胶1', value: 'ADHESIVE1' },
  { label: '分切', value: 'SLITTING' },
  { label: '压槽', value: 'PRESS_SLOT' },
  { label: '粘胶2', value: 'ADHESIVE2' },
  { label: '裁切', value: 'CUT_ROUND' },
  { label: '终检', value: 'FINAL_INSPECTION' },
];

const TABLE_ROW_HEIGHT = 53;

function naturalWeekRange(reference = dayjs()) {
  const weekday = reference.day();
  const monday = reference
    .subtract(weekday === 0 ? 6 : weekday - 1, 'day')
    .startOf('day');
  return [
    monday.format('YYYY-MM-DD'),
    monday.add(6, 'day').format('YYYY-MM-DD'),
  ];
}

const query = reactive({
  dateRange: naturalWeekRange() as string[],
  materialKeyword: undefined as string | undefined,
  modelCode: undefined as string | undefined,
  motherRollBatchNo: undefined as string | undefined,
  motherSegmentBatchNo: undefined as string | undefined,
  pieceNo: undefined as string | undefined,
  planNo: undefined as string | undefined,
  processCode: 'ALL',
});

const loading = ref(false);
const syncing = ref(false);
const rows = ref<PivotRow[]>([]);
const modelCodes = ref<string[]>([]);
const chartPaneRef = ref<HTMLElement>();
const tablePaneRef = ref<HTMLElement>();
const tablePlaceholderRowCount = ref(0);
const tableScrollY = ref(420);
const batchRateChartRef = ref<EchartsUIType>();
const { renderEcharts: renderBatchRateChart, resize: resizeBatchRateChart } =
  useEcharts(batchRateChartRef);

let layoutResizeFrame = 0;
let layoutResizeObserver: ResizeObserver | undefined;

const modelOptions = computed(() =>
  modelCodes.value.map((value) => ({ label: value, value })),
);

const batchRateRows = computed(() => buildBatchRateRows(rows.value));

const batchRateModelCodes = computed(() => [
  ...new Set(batchRateRows.value.map((row) => row.modelCode)),
]);

const displayBatchRateRows = computed<BatchRateRow[]>(() => {
  if (batchRateRows.value.length === 0) return [];
  const placeholders = Array.from(
    { length: tablePlaceholderRowCount.value },
    (_, index) => ({
      batchNo: '',
      isPlaceholder: true,
      key: `__placeholder_${index}`,
      metricLabel: '',
      modelCode: '',
      sourceCount: 0,
      stageCode: '',
    }),
  );
  return [...batchRateRows.value, ...placeholders];
});

const chartTitle = computed(() => {
  const modelCode =
    query.modelCode ||
    (batchRateModelCodes.value.length === 1
      ? batchRateModelCodes.value[0]
      : '');
  return modelCode ? `${modelCode}-批次良率` : '批次良率';
});

const batchRateColumns: TableColumnsType<BatchRateRow> = [
  {
    dataIndex: 'modelCode',
    ellipsis: true,
    key: 'modelCode',
    title: '型号',
    width: 120,
  },
  {
    dataIndex: 'batchNo',
    ellipsis: true,
    key: 'batchNo',
    title: '批次',
    width: 200,
  },
  {
    customRender: ({ record }) =>
      record.isPlaceholder ? '' : formatPercentValue(record.yieldRate),
    key: 'yieldRate',
    title: '良率',
    width: 110,
  },
];

function formatNumber(value: unknown) {
  if (value === undefined || value === null || value === '') return '-';
  const numberValue = Number(value);
  if (!Number.isFinite(numberValue)) return String(value);
  return numberValue.toLocaleString('zh-CN', {
    maximumFractionDigits: 2,
    minimumFractionDigits: 0,
  });
}

function formatPercentValue(value: unknown) {
  return value === undefined || value === null
    ? '-'
    : `${formatNumber(value)}%`;
}

function toStageCode(processCode: string) {
  if (processCode === 'ROUGH_GRINDING') return 'GRINDING';
  if (processCode === 'FINAL_INSPECTION') return 'CUT_ROUND';
  return processCode;
}

function stageOf(row: PivotRow, stageCode: string): PivotStage {
  return (row.stages?.[stageCode] || {}) as PivotStage;
}

function modelText(row: PivotRow) {
  return (
    row.actualModelCode ||
    row.modelSeriesCode ||
    row.modelCode ||
    row.modelName ||
    '-'
  );
}

function toFiniteNumber(value: unknown) {
  if (value === undefined || value === null || value === '') return undefined;
  const numberValue = Number(value);
  return Number.isFinite(numberValue) ? numberValue : undefined;
}

function batchRateCandidateStageCodes() {
  if (query.processCode !== 'ALL') {
    return [toStageCode(query.processCode)];
  }
  return [
    'CUT_ROUND',
    'ADHESIVE2',
    'PRESS_SLOT',
    'SLITTING',
    'ADHESIVE1',
    'GRINDING',
    'WET',
    'FORMULA',
  ];
}

function resolveBatchRateMetric(row: PivotRow) {
  for (const stageCode of batchRateCandidateStageCodes()) {
    const stage = stageOf(row, stageCode);
    const goodYieldRate = toFiniteNumber(stage.goodYieldRate);
    if (goodYieldRate !== undefined) {
      return {
        metricLabel: '良品率',
        rate: goodYieldRate,
        stage,
        stageCode,
      };
    }
  }
}

function batchRateWeight(stage: PivotStage) {
  const candidates = [
    stage.inputQty,
    stage.theoreticalOutputQty,
    stage.motherOutputQty,
    stage.doneQty,
  ];
  for (const candidate of candidates) {
    const value = Number(candidate || 0);
    if (Number.isFinite(value) && value > 0) return value;
  }
  return 1;
}

function buildBatchRateRows(sourceRows: PivotRow[]) {
  const grouped = new Map<string, BatchRateAccumulator>();
  for (const row of sourceRows) {
    const metric = resolveBatchRateMetric(row);
    if (!metric) continue;
    const modelCode = modelText(row);
    const batchNo = row.segmentBatchNo || row.motherRollBatchNo || '-';
    if (modelCode === '-' || batchNo === '-') continue;
    const key = `${modelCode}|${batchNo}`;
    const weight = batchRateWeight(metric.stage);
    const current =
      grouped.get(key) ||
      ({
        batchNo,
        metricLabel: metric.metricLabel,
        modelCode,
        sourceCount: 0,
        stageCode: metric.stageCode,
        totalWeight: 0,
        weightedRate: 0,
      } satisfies BatchRateAccumulator);
    current.sourceCount += 1;
    current.totalWeight += weight;
    current.weightedRate += metric.rate * weight;
    grouped.set(key, current);
  }
  return [...grouped.values()]
    .map((item) => ({
      batchNo: item.batchNo,
      key: `${item.modelCode}|${item.batchNo}`,
      metricLabel: item.metricLabel,
      modelCode: item.modelCode,
      sourceCount: item.sourceCount,
      stageCode: item.stageCode,
      yieldRate:
        item.totalWeight > 0
          ? Number((item.weightedRate / item.totalWeight).toFixed(2))
          : undefined,
    }))
    .toSorted((first, second) =>
      `${first.modelCode}|${first.batchNo}`.localeCompare(
        `${second.modelCode}|${second.batchNo}`,
        'zh-CN',
      ),
    );
}

function buildParams(): MesQmsYieldAnalysisV2Api.QueryParams {
  return {
    actualReportDateEnd: query.dateRange?.[1],
    actualReportDateStart: query.dateRange?.[0],
    materialKeyword: query.materialKeyword,
    modelCode: query.modelCode,
    motherRollBatchNo: query.motherRollBatchNo,
    motherSegmentBatchNo: query.motherSegmentBatchNo,
    pageNo: 1,
    pageSize: 20,
    pieceNo: query.pieceNo,
    planNo: query.planNo,
    processCode: query.processCode,
  };
}

function getBatchRateRowKey(record: BatchRateRow) {
  return record.key;
}

function getBatchRateRowClass(record: BatchRateRow) {
  if (record.isPlaceholder) return 'batch-rate-row--placeholder';
  return '';
}

function updateLayoutSize() {
  const paneHeight = tablePaneRef.value?.clientHeight || 0;
  const headerHeight =
    tablePaneRef.value
      ?.querySelector('.ant-table-thead')
      ?.getBoundingClientRect().height || 42;
  if (paneHeight > 0) {
    tableScrollY.value = Math.max(180, paneHeight - headerHeight - 2);
    tablePlaceholderRowCount.value = Math.max(
      0,
      Math.ceil(tableScrollY.value / TABLE_ROW_HEIGHT) -
        batchRateRows.value.length,
    );
  }
}

function scheduleLayoutResize() {
  if (layoutResizeFrame) {
    cancelAnimationFrame(layoutResizeFrame);
  }
  layoutResizeFrame = requestAnimationFrame(() => {
    layoutResizeFrame = 0;
    updateLayoutSize();
    void nextTick(() => resizeBatchRateChart());
  });
}

function setupLayoutObserver() {
  layoutResizeObserver?.disconnect();
  layoutResizeObserver = new ResizeObserver(scheduleLayoutResize);
  for (const target of [chartPaneRef.value, tablePaneRef.value]) {
    if (target) layoutResizeObserver.observe(target);
  }
  window.addEventListener('resize', scheduleLayoutResize);
  scheduleLayoutResize();
}

async function renderBatchRateBars() {
  const chartRows = batchRateRows.value;
  await renderBatchRateChart({
    color: ['#3f73d8'],
    grid: { bottom: 96, containLabel: true, left: 56, right: 28, top: 58 },
    series: [
      {
        barMaxWidth: 24,
        data: chartRows.map((item) => item.yieldRate ?? 0),
        label: {
          color: '#334155',
          formatter: ({ value }: { value: number }) =>
            `${Number(value || 0).toFixed(2)}%`,
          show: true,
          position: 'top',
        },
        name: '良率',
        type: 'bar',
      },
    ],
    title: {
      left: 'center',
      text: chartTitle.value,
      textStyle: { color: '#334155', fontSize: 16, fontWeight: 700 },
      top: 10,
    },
    tooltip: {
      formatter: ({
        dataIndex,
        value,
      }: {
        dataIndex: number;
        value: number;
      }) => {
        const row = chartRows[dataIndex];
        return row
          ? `${row.modelCode}<br/>${row.batchNo}<br/>${row.metricLabel}: ${Number(value || 0).toFixed(2)}%`
          : '';
      },
      trigger: 'item',
    },
    xAxis: {
      axisLabel: { color: '#64748b', fontSize: 10, interval: 0, rotate: 45 },
      axisTick: { alignWithLabel: true },
      data: chartRows.map((item) => item.batchNo),
      type: 'category',
    },
    yAxis: {
      axisLabel: { formatter: '{value}%' },
      max: 100,
      min: 0,
      splitLine: { lineStyle: { color: '#e5e7eb' } },
      type: 'value',
    },
  });
}

async function loadData() {
  loading.value = true;
  try {
    rows.value = (await getYieldAnalysisV2List(buildParams())) || [];
    await nextTick();
    updateLayoutSize();
    await renderBatchRateBars();
    resizeBatchRateChart();
  } catch (error) {
    console.error(error);
    message.error('加载批次良率统计失败');
  } finally {
    loading.value = false;
  }
}

async function loadModelOptions() {
  try {
    modelCodes.value = (await getYieldAnalysisV2TargetModelOptions()) || [];
  } catch (error) {
    console.warn('加载型号下拉失败', error);
  }
}

function handleQuery() {
  void loadData();
}

function handleShiftWeek(offset: number) {
  const [startDate, endDate] = query.dateRange?.length
    ? query.dateRange
    : naturalWeekRange();
  query.dateRange = [
    dayjs(startDate).add(offset, 'week').format('YYYY-MM-DD'),
    dayjs(endDate).add(offset, 'week').format('YYYY-MM-DD'),
  ];
  handleQuery();
}

function handleReset() {
  Object.assign(query, {
    dateRange: naturalWeekRange(),
    materialKeyword: undefined,
    modelCode: undefined,
    motherRollBatchNo: undefined,
    motherSegmentBatchNo: undefined,
    pieceNo: undefined,
    planNo: undefined,
    processCode: 'ALL',
  });
  void loadData();
}

async function handleSync() {
  syncing.value = true;
  try {
    const result = await syncYieldAnalysisV2();
    message.success(
      `最新状态同步完成：新增 ${result.insertedSourceCount} 条，更新 ${result.updatedSourceCount} 条，移除 ${result.removedSourceCount} 条，未变 ${result.alreadySettledRecordCount} 条`,
    );
    await loadData();
  } catch (error) {
    console.error(error);
    message.error('日结同步失败，请检查服务端日志');
  } finally {
    syncing.value = false;
  }
}

onMounted(async () => {
  await nextTick();
  setupLayoutObserver();
  void Promise.all([loadModelOptions(), loadData()]);
});

onBeforeUnmount(() => {
  layoutResizeObserver?.disconnect();
  window.removeEventListener('resize', scheduleLayoutResize);
  if (layoutResizeFrame) {
    cancelAnimationFrame(layoutResizeFrame);
  }
});
</script>

<template>
  <Page
    auto-content-height
    class="yield-batch-rate-page"
    content-class="yield-batch-rate-content"
  >
    <section class="query-panel">
      <div class="query-grid">
        <div class="query-item query-date">
          <label>实际报工日期</label>
          <div class="week-range-control">
            <Button @click="handleShiftWeek(-1)">上周</Button>
            <DatePicker.RangePicker
              v-model:value="query.dateRange"
              :allow-clear="false"
              value-format="YYYY-MM-DD"
            />
            <Button @click="handleShiftWeek(1)">下周</Button>
          </div>
        </div>
        <div class="query-item">
          <label>工序</label>
          <Select
            v-model:value="query.processCode"
            :options="processOptions"
            @change="handleQuery"
          />
        </div>
        <div class="query-item">
          <label>型号</label>
          <Select
            v-model:value="query.modelCode"
            allow-clear
            :options="modelOptions"
            placeholder="全部型号"
            show-search
          />
        </div>
        <div class="query-item">
          <label>母卷批号</label>
          <Input
            v-model:value="query.motherRollBatchNo"
            allow-clear
            placeholder="母批/生产/主批号"
            @press-enter="handleQuery"
          />
        </div>
        <div class="query-item">
          <label>分段批号</label>
          <Input
            v-model:value="query.motherSegmentBatchNo"
            allow-clear
            placeholder="分段批号"
            @press-enter="handleQuery"
          />
        </div>
        <div class="query-item">
          <label>片号</label>
          <Input
            v-model:value="query.pieceNo"
            allow-clear
            placeholder="片号"
            @press-enter="handleQuery"
          />
        </div>
        <div class="query-item">
          <label>计划号</label>
          <Input
            v-model:value="query.planNo"
            allow-clear
            placeholder="计划号"
            @press-enter="handleQuery"
          />
        </div>
        <div class="query-item">
          <label>物料/型号</label>
          <Input
            v-model:value="query.materialKeyword"
            allow-clear
            placeholder="物料编码、名称或型号"
            @press-enter="handleQuery"
          />
        </div>
        <div class="query-actions">
          <Button type="primary" :loading="loading" @click="handleQuery">
            查询
          </Button>
          <Button :disabled="syncing" @click="handleReset">重置</Button>
          <Button :loading="syncing" @click="handleSync">同步最新数据</Button>
        </div>
      </div>
    </section>

    <section class="batch-rate-panel">
      <div class="batch-rate-layout">
        <div ref="tablePaneRef" class="batch-rate-table-pane">
          <Table
            bordered
            :columns="batchRateColumns"
            :data-source="displayBatchRateRows"
            :loading="loading"
            :locale="{ emptyText: '当前条件下暂无批次良率数据' }"
            :pagination="false"
            :row-class-name="getBatchRateRowClass"
            :row-key="getBatchRateRowKey"
            :scroll="{ y: tableScrollY }"
            size="small"
          />
        </div>
        <div ref="chartPaneRef" class="batch-rate-chart-pane">
          <EchartsUI
            ref="batchRateChartRef"
            class="batch-rate-chart"
            height="100%"
            width="100%"
          />
        </div>
      </div>
    </section>
  </Page>
</template>

<style scoped>
.yield-batch-rate-page {
  --panel-border: #d7e2ef;
  --table-border: #111;
  --table-row-height: 53px;
  --table-row-bg: #f8fafc;
}

.yield-batch-rate-page :deep(.yield-batch-rate-content) {
  display: flex;
  min-height: 0;
  flex-direction: column;
  overflow: hidden;
}

.query-panel,
.batch-rate-panel {
  border: 1px solid var(--panel-border);
  border-radius: 6px;
  background: #fff;
  box-shadow: 0 1px 2px rgb(15 23 42 / 4%);
}

.query-panel {
  flex: 0 0 auto;
  padding: 12px;
}

.query-grid {
  display: grid;
  align-items: center;
  grid-template-columns: minmax(420px, 1.2fr) repeat(3, minmax(220px, 1fr));
  gap: 10px 12px;
}

.query-item {
  display: grid;
  min-width: 0;
  align-items: center;
  grid-template-columns: 84px minmax(0, 1fr);
}

.query-date {
  grid-template-columns: 96px minmax(0, 1fr);
}

.query-item label {
  display: flex;
  min-height: 34px;
  align-items: center;
  justify-content: flex-end;
  padding-right: 10px;
  color: #334155;
  font-size: 13px;
  font-weight: 500;
  white-space: nowrap;
}

.query-item :deep(.ant-input),
.query-item :deep(.ant-input-affix-wrapper),
.query-item :deep(.ant-picker),
.query-item :deep(.ant-select),
.query-item :deep(.ant-select-selector) {
  width: 100%;
  min-height: 34px;
}

.week-range-control {
  display: grid;
  min-width: 0;
  align-items: center;
  grid-template-columns: auto minmax(230px, 1fr) auto;
  gap: 6px;
}

.week-range-control :deep(.ant-picker) {
  width: 100%;
}

.query-actions {
  display: flex;
  grid-column: 1 / -1;
  min-width: 0;
  align-items: center;
  justify-content: flex-end;
  justify-self: end;
  gap: 8px;
}

.batch-rate-panel {
  display: flex;
  min-height: 0;
  flex: 1 1 auto;
  margin-top: 12px;
  overflow: hidden;
}

.batch-rate-layout {
  display: grid;
  width: 100%;
  height: 100%;
  min-height: 0;
  flex: 1 1 auto;
  grid-template-columns: clamp(430px, 27vw, 520px) minmax(0, 1fr);
}

.batch-rate-table-pane {
  height: 100%;
  min-width: 0;
  min-height: 0;
  border-right: 1px solid var(--table-border);
  overflow: hidden;
}

.batch-rate-chart-pane {
  height: 100%;
  min-width: 0;
  min-height: 0;
  padding: 12px 14px 8px;
}

.batch-rate-chart {
  width: 100%;
  height: 100%;
  min-height: 0;
}

.batch-rate-table-pane :deep(.ant-table-wrapper),
.batch-rate-table-pane :deep(.ant-spin-nested-loading),
.batch-rate-table-pane :deep(.ant-spin-container),
.batch-rate-table-pane :deep(.ant-table),
.batch-rate-table-pane :deep(.ant-table-container) {
  height: 100%;
  min-height: 0;
}

.batch-rate-table-pane :deep(.ant-spin-container),
.batch-rate-table-pane :deep(.ant-table),
.batch-rate-table-pane :deep(.ant-table-container) {
  display: flex;
  background: var(--table-row-bg);
  flex-direction: column;
}

.batch-rate-table-pane :deep(.ant-table table) {
  width: 100% !important;
  background: var(--table-row-bg);
  table-layout: fixed !important;
}

.batch-rate-table-pane :deep(.ant-table-header) {
  flex: 0 0 auto;
}

.batch-rate-table-pane :deep(.ant-table-body) {
  position: relative;
  height: auto !important;
  max-height: none !important;
  background: var(--table-row-bg);
  flex: 1 1 auto;
  overflow-y: auto !important;
}

.batch-rate-table-pane :deep(.ant-table-body table) {
  background: transparent;
}

.batch-rate-table-pane :deep(.ant-table-placeholder > td) {
  background: var(--table-row-bg) !important;
}

.batch-rate-table-pane :deep(.ant-table-thead > tr > th) {
  padding: 7px 6px;
  border-color: var(--table-border) !important;
  background: #e5f3df;
  color: #334155;
  font-size: 13px;
  font-weight: 700;
  text-align: center;
}

.batch-rate-table-pane :deep(.ant-table-tbody > tr > td) {
  height: var(--table-row-height);
  padding: 5px 6px;
  border-color: var(--table-border) !important;
  background: var(--table-row-bg);
  color: #111827;
  font-size: 12px;
  font-weight: 600;
  overflow: hidden;
  text-align: center;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.batch-rate-table-pane :deep(.batch-rate-row--placeholder) {
  cursor: default;
  pointer-events: none;
}

@media (max-width: 1300px) {
  .query-grid {
    grid-template-columns: repeat(2, minmax(280px, 1fr));
  }

  .query-date {
    grid-column: 1 / -1;
  }
}

@media (max-width: 900px) {
  .query-grid,
  .batch-rate-layout {
    grid-template-columns: 1fr;
  }

  .batch-rate-table-pane {
    border-right: 0;
    border-bottom: 1px solid #d9d9d9;
  }

  .batch-rate-layout {
    grid-template-rows: minmax(240px, 0.45fr) minmax(260px, 0.55fr);
  }

  .query-actions {
    justify-content: flex-end;
  }
}
</style>
