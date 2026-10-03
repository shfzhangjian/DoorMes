<script lang="ts" setup>
import type { EchartsUIType } from '@vben/plugins/echarts';
import type { MesQmsDefectCategoryAnalysisApi } from '#/api/mes/quality/statistics/defect-category-analysis';

import { computed, nextTick, onMounted, reactive, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';
import { EchartsUI, useEcharts } from '@vben/plugins/echarts';

import dayjs from 'dayjs';
import {
  Button,
  Form,
  Input,
  Pagination,
  RangePicker,
  Select,
  Statistic,
  Table,
  Tag,
} from 'ant-design-vue';

import {
  getDefectCategoryAnalysisDetailPage,
  getDefectCategoryAnalysisOverview,
  getDefectCategoryOptions,
} from '#/api/mes/quality/statistics/defect-category-analysis';

defineOptions({ name: 'MesQmsDefectCategoryAnalysis' });

type DetailRow = MesQmsDefectCategoryAnalysisApi.DetailRow;
type OverviewResult = MesQmsDefectCategoryAnalysisApi.OverviewResult;
type PivotRow = MesQmsDefectCategoryAnalysisApi.PivotRow;
type ViewLevel = 'DETAIL' | 'MOTHER' | 'SEGMENT';

interface DefectTableColumn {
  dataIndex?: string;
  defectCategory?: string;
  fixed?: 'left';
  key: string;
  title: string;
  width?: number;
}

interface PivotDisplayRow extends PivotRow {
  [key: string]: number | object | string | undefined;
}

const processOptions = [
  { label: '全部工序', value: 'ALL' },
  { label: '分切', value: 'SLITTING' },
  { label: '压槽', value: 'PRESS_SLOT' },
  { label: '粘胶2', value: 'ADHESIVE2' },
  { label: '裁切', value: 'CUT_ROUND' },
];

const inspectionTypeOptions = [
  { label: '全部类型', value: 'ALL' },
  { label: '自检', value: 'SELF_CHECK' },
  { label: '送检', value: 'SUBMISSION' },
  { label: '终检', value: 'FINAL_INSPECTION' },
];

const loading = ref(false);
const detailLoading = ref(false);
const paretoChartRef = ref<EchartsUIType>();
const { renderEcharts: renderParetoChart } = useEcharts(paretoChartRef);
const queryCollapsed = ref(false);
const metricsCollapsed = ref(false);
const chartCollapsed = ref(false);

const query = reactive({
  dateRange: [
    dayjs().subtract(7, 'day').format('YYYY-MM-DD'),
    dayjs().format('YYYY-MM-DD'),
  ] as string[],
  defectCategory: '',
  inspectionType: 'ALL',
  keyword: '',
  modelCode: '',
  motherRollBatchNo: '',
  processCode: 'ALL',
  segmentBatchNo: '',
});

const viewLevel = ref<ViewLevel>('MOTHER');
const overview = ref<OverviewResult>(createEmptyOverview());
const detailRows = ref<DetailRow[]>([]);
const defectCategoryOptions = ref<{ label: string; value: string }[]>([]);
const selectedMother = ref('');
const selectedSegment = ref('');
const detailPagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
});

const metricItems = computed(() => [
  {
    color: '#0f172a',
    icon: 'lucide:layers-3',
    title: '缺陷总数',
    value: overview.value.overview.totalCount,
  },
  {
    color: '#b45309',
    icon: 'lucide:scan-line',
    title: '自检异常',
    value: overview.value.overview.selfCheckCount,
  },
  {
    color: '#1d4ed8',
    icon: 'lucide:clipboard-check',
    title: '送检异常',
    value: overview.value.overview.submissionCount,
  },
  {
    color: '#166534',
    icon: 'lucide:bar-chart-3',
    subtitle: overview.value.overview.primaryDefectCategory || '-',
    title: '主要分类',
    value: overview.value.overview.primaryDefectCount,
  },
]);

const pivotRows = computed<PivotDisplayRow[]>(() =>
  overview.value.rows.map((row) => {
    const displayRow: PivotDisplayRow = { ...row };
    overview.value.defectColumns.forEach((column, index) => {
      displayRow[defectField(index)] = Number(row.defectCounts?.[column.key] || 0);
    });
    return displayRow;
  }),
);

const pivotColumns = computed<DefectTableColumn[]>(() => {
  const baseColumns: DefectTableColumn[] = [
    {
      dataIndex: 'modelSeriesCode',
      fixed: 'left',
      key: 'modelSeriesCode',
      title: '型号',
      width: 78,
    },
    {
      dataIndex: 'motherRollBatchNo',
      fixed: 'left',
      key: 'motherRollBatchNo',
      title: '母卷批号',
      width: 150,
    },
  ];
  if (viewLevel.value === 'SEGMENT') {
    baseColumns.push({
      dataIndex: 'segmentBatchNo',
      fixed: 'left',
      key: 'segmentBatchNo',
      title: '分段批号',
      width: 160,
    });
  }
  baseColumns.push({
    dataIndex: 'totalCount',
    key: 'totalCount',
    title: '合计',
    width: 84,
  });
  return [
    ...baseColumns,
    ...overview.value.defectColumns.map((column, index) => ({
      dataIndex: defectField(index),
      defectCategory: column.key,
      key: defectField(index),
      title: column.label,
      width: Math.max(82, Math.min(150, column.label.length * 18 + 42)),
    })),
  ];
});

const detailColumns = computed<DefectTableColumn[]>(() => [
  { dataIndex: 'scanConfirmPieceNo', fixed: 'left', key: 'scanConfirmPieceNo', title: '扫码确认片号', width: 156 },
  { dataIndex: 'processPieceNo', key: 'processPieceNo', title: '本工序片号', width: 156 },
  { dataIndex: 'scanConfirmTime', key: 'scanConfirmTime', title: '扫码确认时间', width: 168 },
  { dataIndex: 'eventTime', key: 'eventTime', title: '检验/确认时间', width: 168 },
  { dataIndex: 'processName', key: 'processName', title: '工序', width: 86 },
  { dataIndex: 'inspectionTypeName', key: 'inspectionTypeName', title: '检验类型', width: 92 },
  { dataIndex: 'inspectorName', key: 'inspectorName', title: '检验人', width: 112 },
  { dataIndex: 'inspectionCategory', key: 'inspectionCategory', title: '检验分类', width: 150 },
  { dataIndex: 'defectCode', key: 'defectCode', title: '缺陷代码', width: 170 },
  { dataIndex: 'defectCount', key: 'defectCount', title: '计数', width: 76 },
  { dataIndex: 'inspectionNo', key: 'inspectionNo', title: '检验单号', width: 160 },
  { dataIndex: 'remark', key: 'remark', title: '备注', width: 180 },
]);

const currentTitle = computed(() => {
  if (viewLevel.value === 'DETAIL') {
    return `${selectedMother.value || '-'} / ${selectedSegment.value || '-'} 片号明细`;
  }
  if (viewLevel.value === 'SEGMENT') {
    return `${selectedMother.value || '-'} 分段缺陷分类`;
  }
  return '母卷缺陷分类';
});

const tableBodyScrollY = computed(() => {
  let offset = viewLevel.value === 'DETAIL' ? 910 : 870;
  if (queryCollapsed.value) {
    offset -= 128;
  }
  if (metricsCollapsed.value) {
    offset -= 112;
  }
  if (chartCollapsed.value) {
    offset -= 372;
  }
  return `max(260px, calc(100vh - ${Math.max(offset, 320)}px))`;
});

const pivotScroll = computed(() => ({
  x: pivotColumns.value.reduce((total, column) => total + Number(column.width || 0), 0),
  y: tableBodyScrollY.value,
}));

const detailScroll = computed(() => ({
  x: 1680,
  y: tableBodyScrollY.value,
}));

onMounted(async () => {
  await Promise.all([loadOverview(), loadDefectOptions()]);
});

function createEmptyOverview(): OverviewResult {
  return {
    defectColumns: [],
    groupLevel: 'MOTHER',
    overview: {
      motherRollCount: 0,
      pieceCount: 0,
      primaryDefectCategory: '-',
      primaryDefectCount: 0,
      segmentCount: 0,
      selfCheckCount: 0,
      submissionCount: 0,
      totalCount: 0,
    },
    paretoRows: [],
    rows: [],
  };
}

async function loadOverview() {
  loading.value = true;
  try {
    const data = await getDefectCategoryAnalysisOverview(buildOverviewParams());
    overview.value = data || createEmptyOverview();
    await nextTick();
    renderPareto();
  } finally {
    loading.value = false;
  }
}

async function loadDetail() {
  detailLoading.value = true;
  try {
    const data = await getDefectCategoryAnalysisDetailPage({
      ...buildBaseParams(),
      motherRollBatchNo: selectedMother.value,
      pageNo: detailPagination.current,
      pageSize: detailPagination.pageSize,
      segmentBatchNo: selectedSegment.value,
    });
    detailRows.value = data?.list || [];
    detailPagination.total = Number(data?.total || 0);
  } finally {
    detailLoading.value = false;
  }
}

async function loadDefectOptions() {
  const values = await getDefectCategoryOptions({
    ...buildBaseParams(),
    defectCategory: undefined,
  });
  defectCategoryOptions.value = (values || []).map((value) => ({
    label: value,
    value,
  }));
}

function buildBaseParams(): MesQmsDefectCategoryAnalysisApi.QueryParams {
  return {
    defectCategory: query.defectCategory || undefined,
    endDate: query.dateRange?.[1],
    inspectionType: query.inspectionType,
    keyword: query.keyword || undefined,
    modelCode: query.modelCode || undefined,
    motherRollBatchNo: query.motherRollBatchNo || undefined,
    processCode: query.processCode,
    segmentBatchNo: query.segmentBatchNo || undefined,
    startDate: query.dateRange?.[0],
  };
}

function buildOverviewParams(): MesQmsDefectCategoryAnalysisApi.QueryParams {
  return {
    ...buildBaseParams(),
    groupLevel: viewLevel.value === 'MOTHER' ? 'MOTHER' : 'SEGMENT',
    motherRollBatchNo:
      viewLevel.value !== 'MOTHER'
        ? selectedMother.value
        : query.motherRollBatchNo || undefined,
    segmentBatchNo:
      viewLevel.value === 'DETAIL'
        ? selectedSegment.value
        : query.segmentBatchNo || undefined,
  };
}

async function handleSearch() {
  selectedMother.value = '';
  selectedSegment.value = '';
  viewLevel.value = 'MOTHER';
  detailPagination.current = 1;
  await Promise.all([loadOverview(), loadDefectOptions()]);
}

async function handleReset() {
  query.dateRange = [
    dayjs().subtract(7, 'day').format('YYYY-MM-DD'),
    dayjs().format('YYYY-MM-DD'),
  ];
  query.defectCategory = '';
  query.inspectionType = 'ALL';
  query.keyword = '';
  query.modelCode = '';
  query.motherRollBatchNo = '';
  query.processCode = 'ALL';
  query.segmentBatchNo = '';
  await handleSearch();
}

async function openSegment(row: PivotDisplayRow) {
  selectedMother.value = String(row.motherRollBatchNo || '');
  selectedSegment.value = '';
  viewLevel.value = 'SEGMENT';
  detailPagination.current = 1;
  await loadOverview();
}

async function openDetail(row: PivotDisplayRow) {
  selectedMother.value = String(row.motherRollBatchNo || selectedMother.value || '');
  selectedSegment.value = String(row.segmentBatchNo || '');
  viewLevel.value = 'DETAIL';
  detailPagination.current = 1;
  await Promise.all([loadDetail(), loadOverview()]);
}

async function backToMother() {
  selectedMother.value = '';
  selectedSegment.value = '';
  viewLevel.value = 'MOTHER';
  await loadOverview();
}

async function backToSegment() {
  viewLevel.value = 'SEGMENT';
  selectedSegment.value = '';
  detailPagination.current = 1;
  await loadOverview();
}

async function handleDetailPageChange(page: number, pageSize: number) {
  detailPagination.current = page;
  detailPagination.pageSize = pageSize;
  await loadDetail();
}

async function toggleChartCollapsed() {
  chartCollapsed.value = !chartCollapsed.value;
  if (!chartCollapsed.value) {
    await nextTick();
    renderPareto();
  }
}

function renderPareto() {
  if (chartCollapsed.value) {
    return;
  }
  const rows = overview.value.paretoRows || [];
  const maxLabelLength = Math.max(
    0,
    ...rows.map((row) => String(row.defectCategory || '-').length),
  );
  const maxLabelLines = Math.ceil(maxLabelLength / (maxLabelLength > 18 ? 6 : 8));
  const labelBottom = Math.min(220, Math.max(92, maxLabelLines * 18 + 48));
  renderParetoChart({
    color: ['#2563eb', '#dc2626'],
    grid: {
      bottom: labelBottom,
      containLabel: true,
      left: 56,
      right: 56,
      top: 48,
    },
    legend: {
      data: ['缺陷计数', '累计占比'],
      top: 4,
    },
    series: [
      {
        barMaxWidth: 34,
        data: rows.map((row) => ({
          ratio: row.ratio || 0,
          value: row.defectCount || 0,
        })),
        label: {
          color: '#334155',
          formatter: (params: { data?: { ratio?: number } }) =>
            formatChartPercent(params.data?.ratio),
          position: 'top',
          show: true,
        },
        name: '缺陷计数',
        type: 'bar',
      },
      {
        data: rows.map((row) => ({
          value: row.cumulativeRatio || 0,
        })),
        label: {
          color: '#dc2626',
          formatter: (params: { value?: number }) => formatChartPercent(params.value),
          position: 'top',
          show: true,
        },
        name: '累计占比',
        smooth: true,
        symbol: 'circle',
        symbolSize: 7,
        type: 'line',
        yAxisIndex: 1,
      },
    ],
    tooltip: {
      trigger: 'axis',
    },
    xAxis: {
      axisLabel: {
        formatter: wrapAxisLabel,
        hideOverlap: false,
        interval: 0,
        lineHeight: 16,
        margin: 14,
        rotate: rows.length > 10 ? 28 : 0,
      },
      data: rows.map((row) => row.defectCategory || '-'),
      type: 'category',
    },
    yAxis: [
      {
        minInterval: 1,
        name: '计数',
        type: 'value',
      },
      {
        max: 100,
        name: '累计%',
        type: 'value',
      },
    ],
  });
}

function defectField(index: number) {
  return `defect_${index}`;
}

function getColumnDataIndex(column: DefectTableColumn) {
  return String(column.dataIndex || column.key);
}

function getCountClass(value: unknown) {
  return Number(value || 0) > 0 ? 'defect-count defect-count--warn' : 'defect-count';
}

function getInspectionTypeColor(inspectionType: string) {
  if (inspectionType === 'FINAL_INSPECTION') {
    return 'purple';
  }
  return inspectionType === 'SUBMISSION' ? 'blue' : 'orange';
}

function wrapAxisLabel(value: string) {
  const text = String(value || '-');
  const chars = Array.from(text);
  const lineSize = chars.length > 18 ? 6 : 8;
  const lines: string[] = [];
  for (let index = 0; index < chars.length; index += lineSize) {
    lines.push(chars.slice(index, index + lineSize).join(''));
  }
  return lines.join('\n');
}

function formatChartPercent(value: unknown) {
  const percent = Number(value || 0);
  return `${percent.toFixed(1)}%`;
}

function formatCount(value: unknown) {
  const count = Number(value || 0);
  return count > 0 ? count : '-';
}

function formatPlain(value: unknown) {
  const text = String(value ?? '').trim();
  return text && text !== '-' ? text : '-';
}

function formatTotal(total?: number) {
  return `共 ${total || 0} 条`;
}
</script>

<template>
  <Page auto-content-height class="defect-page">
    <div class="defect-toolbar">
      <section class="defect-panel defect-query-panel">
        <button
          class="defect-section-toggle"
          type="button"
          @click="queryCollapsed = !queryCollapsed"
        >
          <span class="defect-title">
            <IconifyIcon icon="lucide:sliders-horizontal" />
            <span>查询条件</span>
          </span>
          <IconifyIcon :icon="queryCollapsed ? 'lucide:chevron-down' : 'lucide:chevron-up'" />
        </button>
        <Form v-show="!queryCollapsed" class="defect-query-form" layout="inline">
          <div class="defect-query-grid">
            <Form.Item class="defect-query-item defect-query-item--date" label="日期">
              <RangePicker
                v-model:value="query.dateRange"
                allow-clear
                value-format="YYYY-MM-DD"
              />
            </Form.Item>
            <Form.Item class="defect-query-item" label="母卷">
              <Input
                v-model:value="query.motherRollBatchNo"
                allow-clear
                placeholder="母卷批号"
              />
            </Form.Item>
            <Form.Item class="defect-query-item" label="型号">
              <Input
                v-model:value="query.modelCode"
                allow-clear
                placeholder="型号前三位"
              />
            </Form.Item>
            <Form.Item class="defect-query-item" label="工序">
              <Select v-model:value="query.processCode" :options="processOptions" />
            </Form.Item>
            <Form.Item class="defect-query-item" label="类型">
              <Select
                v-model:value="query.inspectionType"
                :options="inspectionTypeOptions"
              />
            </Form.Item>
            <Form.Item class="defect-query-item" label="分类">
              <Select
                v-model:value="query.defectCategory"
                allow-clear
                show-search
                :filter-option="true"
                :options="defectCategoryOptions"
                placeholder="缺陷分类"
              />
            </Form.Item>
            <Form.Item class="defect-query-item" label="关键字">
              <Input v-model:value="query.keyword" allow-clear placeholder="片号/人员/缺陷" />
            </Form.Item>
            <Form.Item class="defect-query-actions">
              <Button :loading="loading" type="primary" @click="handleSearch">
                <IconifyIcon icon="lucide:search" />
                查询
              </Button>
              <Button @click="handleReset">
                <IconifyIcon icon="lucide:rotate-ccw" />
                重置
              </Button>
            </Form.Item>
          </div>
        </Form>
      </section>
    </div>

    <div class="defect-content">
      <section class="defect-panel defect-metric-panel">
        <button
          class="defect-section-toggle"
          type="button"
          @click="metricsCollapsed = !metricsCollapsed"
        >
          <span class="defect-title">
            <IconifyIcon icon="lucide:panel-top-close" />
            <span>指标卡</span>
          </span>
          <IconifyIcon :icon="metricsCollapsed ? 'lucide:chevron-down' : 'lucide:chevron-up'" />
        </button>
        <div v-show="!metricsCollapsed" class="defect-metrics">
          <div
            v-for="item in metricItems"
            :key="item.title"
            class="metric-panel"
            :style="{ '--metric-color': item.color }"
          >
            <div class="metric-panel__icon">
              <IconifyIcon :icon="item.icon" />
            </div>
            <Statistic :title="item.title" :value="item.value" />
            <span v-if="item.subtitle" class="metric-panel__sub">{{ item.subtitle }}</span>
          </div>
        </div>
      </section>

      <section class="defect-panel defect-table-panel">
        <div class="defect-panel__head">
          <div class="defect-title">
            <IconifyIcon icon="lucide:table-2" />
            <span>{{ currentTitle }}</span>
          </div>
          <div class="defect-nav">
            <Tag v-if="viewLevel !== 'MOTHER'" color="blue">
              {{ selectedMother }}
            </Tag>
            <Tag v-if="viewLevel === 'DETAIL'" color="green">
              {{ selectedSegment }}
            </Tag>
            <Button v-if="viewLevel !== 'MOTHER'" size="small" @click="backToMother">
              <IconifyIcon icon="lucide:chevrons-left" />
              母卷
            </Button>
            <Button v-if="viewLevel === 'DETAIL'" size="small" @click="backToSegment">
              <IconifyIcon icon="lucide:chevron-left" />
              分段
            </Button>
          </div>
        </div>

        <Table
          v-if="viewLevel !== 'DETAIL'"
          bordered
          class="defect-table"
          :columns="pivotColumns"
          :data-source="pivotRows"
          :loading="loading"
          :locale="{ emptyText: '暂无缺陷分类数据' }"
          :pagination="false"
          row-key="groupKey"
          :scroll="pivotScroll"
          size="small"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="getColumnDataIndex(column) === 'motherRollBatchNo'">
              <button class="defect-link" type="button" @click="openSegment(record)">
                {{ formatPlain(record.motherRollBatchNo) }}
              </button>
            </template>
            <template v-else-if="getColumnDataIndex(column) === 'segmentBatchNo'">
              <button class="defect-link" type="button" @click="openDetail(record)">
                {{ formatPlain(record.segmentBatchNo) }}
              </button>
            </template>
            <template v-else-if="column.defectCategory || getColumnDataIndex(column) === 'totalCount'">
              <span :class="getCountClass(record[getColumnDataIndex(column)])">
                {{ formatCount(record[getColumnDataIndex(column)]) }}
              </span>
            </template>
            <template v-else>
              {{ formatPlain(record[getColumnDataIndex(column)]) }}
            </template>
          </template>
        </Table>

        <div v-else class="defect-detail-host">
          <Table
            bordered
            class="defect-table"
            :columns="detailColumns"
            :data-source="detailRows"
            :loading="detailLoading"
            :locale="{ emptyText: '暂无片号缺陷明细' }"
            :pagination="false"
            row-key="eventKey"
            :scroll="detailScroll"
            size="small"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="getColumnDataIndex(column) === 'inspectionTypeName'">
                <Tag :color="getInspectionTypeColor(record.inspectionType)">
                  {{ record.inspectionTypeName }}
                </Tag>
              </template>
              <template v-else-if="getColumnDataIndex(column) === 'defectCount'">
                <span :class="getCountClass(record.defectCount)">
                  {{ formatCount(record.defectCount) }}
                </span>
              </template>
              <template v-else>
                {{ formatPlain(record[getColumnDataIndex(column)]) }}
              </template>
            </template>
          </Table>
          <div class="defect-detail-pagination">
            <Pagination
              :current="detailPagination.current"
              :page-size="detailPagination.pageSize"
              :page-size-options="['10', '20', '50', '100']"
              :show-total="formatTotal"
              :total="detailPagination.total"
              show-size-changer
              size="small"
              @change="handleDetailPageChange"
            />
          </div>
        </div>
      </section>

      <section
        class="defect-panel defect-chart-panel"
        :class="{ 'defect-chart-panel--collapsed': chartCollapsed }"
      >
        <div class="defect-panel__head">
          <div class="defect-title">
            <IconifyIcon icon="lucide:chart-no-axes-combined" />
            <span>柏拉图</span>
          </div>
          <Button size="small" type="text" @click="toggleChartCollapsed">
            <IconifyIcon :icon="chartCollapsed ? 'lucide:chevron-down' : 'lucide:chevron-up'" />
          </Button>
        </div>
        <EchartsUI v-show="!chartCollapsed" ref="paretoChartRef" class="defect-chart" />
      </section>
    </div>
  </Page>
</template>

<style scoped>
.defect-page {
  display: flex;
  height: 100%;
  min-height: 0;
  flex-direction: column;
  overflow: hidden;
  background: #f6f8fb;
}

.defect-toolbar {
  flex: 0 0 auto;
  border-bottom: 1px solid #dbe3ef;
  background: #f6f8fb;
  padding: 12px 12px 0;
}

.defect-query-panel,
.defect-metric-panel {
  flex: 0 0 auto;
}

.defect-section-toggle {
  display: flex;
  width: 100%;
  min-height: 42px;
  align-items: center;
  justify-content: space-between;
  border: 0;
  background: #ffffff;
  color: #334155;
  cursor: pointer;
  font: inherit;
  padding: 0 12px;
}

.defect-section-toggle:hover {
  background: #f8fbff;
}

.defect-query-form,
.defect-query-grid {
  width: 100%;
}

.defect-query-form {
  border-top: 1px solid #edf1f7;
  padding: 12px;
}

.defect-query-grid {
  display: grid;
  align-items: center;
  gap: 10px 12px;
  grid-template-columns: repeat(4, minmax(220px, 1fr));
}

.defect-query-item {
  min-width: 0;
  margin: 0;
}

.defect-query-item :deep(.ant-form-item-control),
.defect-query-item :deep(.ant-form-item-control-input-content),
.defect-query-item :deep(.ant-input),
.defect-query-item :deep(.ant-picker),
.defect-query-item :deep(.ant-select) {
  width: 100%;
}

.defect-query-actions {
  margin: 0;
}

.defect-query-actions :deep(.ant-form-item-control-input-content) {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.defect-query-actions :deep(.ant-btn) {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.defect-content {
  display: flex;
  height: auto;
  min-height: 0;
  flex: 1 1 auto;
  flex-direction: column;
  gap: 12px;
  overflow: hidden;
  padding: 12px;
}

.defect-metrics {
  display: grid;
  flex: 0 0 auto;
  gap: 12px;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  padding: 12px;
}

.metric-panel {
  display: grid;
  min-height: 82px;
  align-items: center;
  border: 1px solid #dbe3ef;
  background: #ffffff;
  grid-template-columns: 42px minmax(0, 1fr);
  padding: 12px;
}

.metric-panel__icon {
  display: grid;
  width: 36px;
  height: 36px;
  place-items: center;
  border: 1px solid #dbe3ef;
  background: #f8fafc;
  color: var(--metric-color);
  font-size: 20px;
}

.metric-panel__sub {
  overflow: hidden;
  color: #64748b;
  font-size: 12px;
  grid-column: 2;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.defect-panel {
  min-width: 0;
  border: 1px solid #dbe3ef;
  background: #ffffff;
  overflow: hidden;
}

.defect-table-panel {
  display: flex;
  flex: 1 1 auto;
  min-height: 260px;
  flex-direction: column;
}

.defect-panel__head {
  display: flex;
  min-height: 42px;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #edf1f7;
  padding: 0 12px;
}

.defect-title,
.defect-nav {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.defect-nav :deep(.ant-btn) {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.defect-panel__head :deep(.ant-btn) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.defect-table {
  display: flex;
  min-height: 0;
  flex: 1 1 auto;
  flex-direction: column;
  overflow: hidden;
}

.defect-table :deep(.ant-spin-container),
.defect-table :deep(.ant-spin-nested-loading) {
  display: flex;
  height: 100%;
  min-height: 0;
  flex: 1 1 auto;
  flex-direction: column;
}

.defect-table :deep(.ant-table) {
  display: flex;
  height: 100%;
  min-height: 0;
  flex: 1 1 auto;
  flex-direction: column;
  color: #334155;
  font-size: 12px;
}

.defect-table :deep(.ant-table-container) {
  display: flex;
  height: 100%;
  min-height: 0;
  flex: 1 1 auto;
  flex-direction: column;
  border-color: #dbe3ef;
  overflow: hidden;
}

.defect-table :deep(.ant-table-header) {
  position: relative;
  z-index: 2;
  flex: 0 0 auto;
  min-height: 40px;
  overflow: hidden !important;
}

.defect-table :deep(.ant-table-body) {
  height: auto !important;
  min-height: 0;
  flex: 1 1 auto;
  overflow: auto !important;
}

.defect-table :deep(.ant-table-cell) {
  height: 36px;
  border-color: #e5edf6 !important;
  padding: 0 8px !important;
  text-align: center;
  white-space: nowrap;
}

.defect-table :deep(.ant-table-thead > tr > th) {
  height: 40px;
  background: #f8fbff;
  color: #475569;
  font-weight: 600;
  line-height: 18px;
  padding: 6px 8px !important;
  text-align: center !important;
  vertical-align: middle;
}

.defect-table :deep(.ant-table-cell-fix-left),
.defect-table :deep(.ant-table-thead .ant-table-cell-fix-left) {
  background: #ffffff;
}

.defect-link {
  width: 100%;
  height: 34px;
  border: 0;
  background: transparent;
  color: #1d4ed8;
  cursor: pointer;
  font: inherit;
}

.defect-link:hover {
  background: #eff6ff;
}

.defect-count {
  color: #94a3b8;
}

.defect-count--warn {
  color: #dc2626;
  font-weight: 700;
}

.defect-detail-host {
  display: flex;
  min-height: 0;
  flex: 1 1 auto;
  flex-direction: column;
}

.defect-detail-pagination {
  display: flex;
  min-height: 40px;
  align-items: center;
  justify-content: flex-end;
  border-top: 1px solid #edf1f7;
  padding: 6px 10px;
}

.defect-chart-panel {
  flex: 0 0 380px;
}

.defect-chart-panel--collapsed {
  flex-basis: auto;
}

.defect-chart {
  height: 336px;
  width: 100%;
}

@media (max-width: 1280px) {
  .defect-query-grid {
    grid-template-columns: repeat(2, minmax(240px, 1fr));
  }

  .defect-metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .defect-query-grid,
  .defect-metrics {
    grid-template-columns: 1fr;
  }

  .defect-query-actions :deep(.ant-form-item-control-input-content) {
    justify-content: flex-start;
  }
}
</style>
