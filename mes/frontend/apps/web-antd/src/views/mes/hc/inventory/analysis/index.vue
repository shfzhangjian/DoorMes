<script lang="ts" setup>
import type { EchartsUIType } from '@vben/plugins/echarts';

import type { MesHcInventoryAnalysisApi } from '#/api/mes/hc/inventory/analysis';

import { computed, nextTick, onMounted, reactive, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';
import { EchartsUI, useEcharts } from '@vben/plugins/echarts';

import {
  Button,
  DatePicker,
  Empty,
  Form,
  Input,
  InputNumber,
  message,
  Select,
  Spin,
  Statistic,
} from 'ant-design-vue';

import { getInventoryAnalysisOverview } from '#/api/mes/hc/inventory/analysis';

defineOptions({ name: 'MesInventoryAnalysis' });

const dimensionOptions: Array<{
  label: string;
  value: MesHcInventoryAnalysisApi.Dimension;
}> = [
  { label: '型号', value: 'MODEL' },
  { label: '分段批次', value: 'SEGMENT_BATCH' },
  { label: '片号', value: 'SLICE_BATCH' },
  { label: '物料', value: 'MATERIAL' },
  { label: '质量状态', value: 'QUALITY_STATUS' },
  { label: '库存状态', value: 'STOCK_STATUS' },
  { label: '仓库', value: 'WAREHOUSE' },
  { label: '库位', value: 'LOCATION' },
  { label: '货架', value: 'RACK' },
  { label: '层', value: 'LAYER' },
];

function formatDate(value: Date) {
  const year = value.getFullYear();
  const month = `${value.getMonth() + 1}`.padStart(2, '0');
  const day = `${value.getDate()}`.padStart(2, '0');
  return `${year}-${month}-${day}`;
}

function getDefaultDateRange(): [string, string] {
  const end = new Date();
  const start = new Date();
  start.setDate(end.getDate() - 29);
  return [formatDate(start), formatDate(end)];
}

const queryForm = reactive({
  warehouseCode: '',
  locationCode: '',
  modelCode: '',
  batchNo: '',
  sliceBatchNo: '',
  qualityStatus: undefined as 'NG' | 'OK' | undefined,
  stockStatus:
    undefined as MesHcInventoryAnalysisApi.OverviewReqVO['stockStatus'],
  dimension: 'MODEL' as MesHcInventoryAnalysisApi.Dimension,
  topN: 20,
  granularity: 'DAY' as 'DAY' | 'MONTH' | 'WEEK',
  dateRange: getDefaultDateRange() as string[],
});

const loading = ref(false);
const queryExpanded = ref(true);
const analysisExpanded = ref(true);
const overview = ref<MesHcInventoryAnalysisApi.Overview>();
const distributionChartRef = ref<EchartsUIType>();
const movementChartRef = ref<EchartsUIType>();
const { renderEcharts: renderDistributionEcharts } =
  useEcharts(distributionChartRef);
const { renderEcharts: renderMovementEcharts } = useEcharts(movementChartRef);

const summary = computed(() => overview.value?.summary);
const distributionRows = computed(() => overview.value?.distributionBars ?? []);
const movementRows = computed(() => overview.value?.movementTrend ?? []);
const currentDimensionLabel = computed(
  () =>
    dimensionOptions.find((item) => item.value === queryForm.dimension)
      ?.label ?? '统计维度',
);

function numberValue(value?: number | string) {
  const result = Number(value ?? 0);
  return Number.isFinite(result) ? result : 0;
}

function formatQty(value?: number | string) {
  return numberValue(value).toLocaleString('zh-CN', {
    maximumFractionDigits: 0,
  });
}

function normalizeText(value: string) {
  const result = value.trim();
  return result || undefined;
}

function buildRequest(): MesHcInventoryAnalysisApi.OverviewReqVO {
  return {
    warehouseCode: normalizeText(queryForm.warehouseCode),
    locationCode: normalizeText(queryForm.locationCode),
    modelCode: normalizeText(queryForm.modelCode),
    batchNo: normalizeText(queryForm.batchNo),
    sliceBatchNo: normalizeText(queryForm.sliceBatchNo),
    qualityStatus: queryForm.qualityStatus,
    stockStatus: queryForm.stockStatus,
    dimension: queryForm.dimension,
    topN: queryForm.topN,
    granularity: queryForm.granularity,
    startDate: queryForm.dateRange[0],
    endDate: queryForm.dateRange[1],
  };
}

async function loadData() {
  loading.value = true;
  try {
    overview.value = await getInventoryAnalysisOverview(buildRequest());
  } catch {
    message.error('成品库位库存分析数据加载失败，请稍后重试');
    return;
  } finally {
    loading.value = false;
  }
  await nextTick();
  renderCharts();
}

function resetQuery() {
  Object.assign(queryForm, {
    warehouseCode: '',
    locationCode: '',
    modelCode: '',
    batchNo: '',
    sliceBatchNo: '',
    qualityStatus: undefined,
    stockStatus: undefined,
    dimension: 'MODEL',
    topN: 20,
    granularity: 'DAY',
    dateRange: getDefaultDateRange(),
  });
  void loadData();
}

function toggleQueryExpanded() {
  queryExpanded.value = !queryExpanded.value;
}

function renderCharts() {
  renderDistributionChart();
  renderMovementChart();
}

async function toggleAnalysisExpanded() {
  analysisExpanded.value = !analysisExpanded.value;
  if (analysisExpanded.value) {
    await nextTick();
    renderCharts();
  }
}

function renderDistributionChart() {
  const rows = distributionRows.value;
  renderDistributionEcharts({
    color: ['#2563eb', '#f97316', '#94a3b8'],
    grid: { top: 44, right: 24, bottom: 72, left: 56 },
    legend: { data: ['可发货', '发货锁定/已分配', '其他在位'], top: 8 },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params: any) => {
        const firstParam = Array.isArray(params) ? params[0] : params;
        const item = rows[firstParam?.dataIndex ?? 0];
        if (!item) return '';
        return [
          `<b>${item.dimensionName}</b>`,
          `在位数量：${formatQty(item.onHandQty)} PCS`,
          `可发货：${formatQty(item.shippableQty)} PCS`,
          `发货锁定/已分配：${formatQty(item.lockedQty)} PCS`,
          `库存记录：${item.recordCount}`,
        ].join('<br/>');
      },
    },
    xAxis: {
      type: 'category',
      data: rows.map((item) => item.dimensionName),
      axisLabel: {
        interval: 0,
        rotate: rows.length > 6 ? 32 : 0,
        width: 108,
        overflow: 'truncate',
      },
    },
    yAxis: { type: 'value', name: 'PCS' },
    series: [
      {
        name: '可发货',
        type: 'bar',
        stack: '在位库存',
        barMaxWidth: 36,
        data: rows.map((item) => numberValue(item.shippableQty)),
      },
      {
        name: '发货锁定/已分配',
        type: 'bar',
        stack: '在位库存',
        barMaxWidth: 36,
        data: rows.map((item) => numberValue(item.lockedQty)),
      },
      {
        name: '其他在位',
        type: 'bar',
        stack: '在位库存',
        barMaxWidth: 36,
        data: rows.map((item) =>
          Math.max(
            numberValue(item.onHandQty) -
              numberValue(item.shippableQty) -
              numberValue(item.lockedQty),
            0,
          ),
        ),
      },
    ],
  });
}

function renderMovementChart() {
  const rows = movementRows.value;
  renderMovementEcharts({
    color: ['#16a34a', '#dc2626', '#f59e0b', '#2563eb'],
    grid: { top: 44, right: 24, bottom: 52, left: 56 },
    legend: { data: ['入库', '实际出库', '退回重包', '净变化'], top: 8 },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: rows.map((item) => item.periodLabel) },
    yAxis: { type: 'value', name: 'PCS' },
    series: [
      {
        name: '入库',
        type: 'line',
        smooth: true,
        data: rows.map((item) => numberValue(item.inboundQty)),
      },
      {
        name: '实际出库',
        type: 'line',
        smooth: true,
        data: rows.map((item) => numberValue(item.outboundQty)),
      },
      {
        name: '退回重包',
        type: 'line',
        smooth: true,
        data: rows.map((item) => numberValue(item.repackReturnQty)),
      },
      {
        name: '净变化',
        type: 'line',
        smooth: true,
        lineStyle: { type: 'dashed' },
        data: rows.map((item) => numberValue(item.netChangeQty)),
      },
    ],
  });
}

onMounted(() => {
  void loadData();
});
</script>

<template>
  <Page auto-content-height class="inventory-analysis-page">
    <Spin :spinning="loading">
      <div class="inventory-toolbar">
        <button
          class="inventory-query-toggle"
          type="button"
          :aria-expanded="queryExpanded"
          @click="toggleQueryExpanded"
        >
          <span class="inventory-query-toggle__title">
            <IconifyIcon icon="lucide:search" />
            <span>整体搜索</span>
          </span>
          <span class="inventory-query-toggle__summary">
            入库日期：{{ queryForm.dateRange[0] }} 至
            {{ queryForm.dateRange[1] }}
          </span>
          <IconifyIcon
            :icon="queryExpanded ? 'lucide:chevron-up' : 'lucide:chevron-down'"
          />
        </button>
        <Form
          v-show="queryExpanded"
          class="inventory-query-form"
          layout="inline"
        >
          <div class="inventory-query-grid">
            <Form.Item
              class="inventory-query-item inventory-query-item--date"
              label="入库日期"
            >
              <DatePicker.RangePicker
                v-model:value="queryForm.dateRange"
                value-format="YYYY-MM-DD"
              />
            </Form.Item>
            <Form.Item class="inventory-query-item" label="型号">
              <Input
                v-model:value="queryForm.modelCode"
                allow-clear
                placeholder="请输入型号"
              />
            </Form.Item>
            <Form.Item class="inventory-query-item" label="仓库">
              <Input
                v-model:value="queryForm.warehouseCode"
                allow-clear
                placeholder="请输入仓库编码"
              />
            </Form.Item>
            <Form.Item class="inventory-query-item" label="库位">
              <Input
                v-model:value="queryForm.locationCode"
                allow-clear
                placeholder="请输入库位编码"
              />
            </Form.Item>
            <Form.Item class="inventory-query-item" label="分段批次">
              <Input
                v-model:value="queryForm.batchNo"
                allow-clear
                placeholder="请输入分段批次"
              />
            </Form.Item>
            <Form.Item class="inventory-query-item" label="片号">
              <Input
                v-model:value="queryForm.sliceBatchNo"
                allow-clear
                placeholder="请输入片号"
              />
            </Form.Item>
            <Form.Item class="inventory-query-item" label="质量状态">
              <Select
                v-model:value="queryForm.qualityStatus"
                allow-clear
                placeholder="全部"
                :options="[
                  { label: '合格', value: 'OK' },
                  { label: '不合格', value: 'NG' },
                ]"
              />
            </Form.Item>
            <Form.Item class="inventory-query-item" label="库存状态">
              <Select
                v-model:value="queryForm.stockStatus"
                allow-clear
                placeholder="全部"
                :options="[
                  { label: '待上架', value: 'INBOUND_LOCKED' },
                  { label: '已上架', value: 'INBOUNDED' },
                  { label: '可用', value: 'AVAILABLE' },
                  { label: '发货锁定', value: 'OUTBOUND_LOCKED' },
                  { label: '已分配', value: 'ALLOCATED' },
                ]"
              />
            </Form.Item>
            <Form.Item class="inventory-query-item" label="柱状图维度">
              <Select
                v-model:value="queryForm.dimension"
                :options="dimensionOptions"
              />
            </Form.Item>
            <Form.Item class="inventory-query-item" label="趋势粒度">
              <Select
                v-model:value="queryForm.granularity"
                :options="[
                  { label: '按日', value: 'DAY' },
                  { label: '按周', value: 'WEEK' },
                  { label: '按月', value: 'MONTH' },
                ]"
              />
            </Form.Item>
            <Form.Item class="inventory-query-item" label="展示数量">
              <InputNumber
                v-model:value="queryForm.topN"
                :min="5"
                :max="50"
                class="inventory-top-n"
              />
            </Form.Item>
            <Form.Item class="inventory-query-actions">
              <Button type="primary" :loading="loading" @click="loadData">
                <template #icon>
                  <IconifyIcon icon="lucide:search" />
                </template>
                查询
              </Button>
              <Button @click="resetQuery">
                <template #icon>
                  <IconifyIcon icon="lucide:rotate-ccw" />
                </template>
                重置
              </Button>
            </Form.Item>
          </div>
        </Form>
      </div>

      <div class="inventory-content">
        <section class="inventory-page-heading">
          <div class="inventory-page-heading__title">
            <IconifyIcon icon="lucide:package-search" />
            <div>
              <h2>成品库位库存分析</h2>
              <p>
                当前柱状图按{{
                  currentDimensionLabel
                }}汇总，可在筛选区切换统计维度。
              </p>
            </div>
          </div>
          <span class="inventory-scope-chip">仅限成品库位在位库存</span>
        </section>

        <section class="inventory-insight-panel">
          <button
            class="inventory-insight-toggle"
            type="button"
            @click="toggleAnalysisExpanded"
          >
            <span class="inventory-insight-toggle__title">
              <IconifyIcon icon="lucide:chart-column" />
              <span>指标卡与图表</span>
            </span>
            <span class="inventory-insight-toggle__metrics">
              全部 {{ formatQty(summary?.onHandQty) }} PCS / 合格
              {{ formatQty(summary?.qualifiedQty) }} PCS / 不合格
              {{ formatQty(summary?.unqualifiedQty) }} PCS
            </span>
            <IconifyIcon
              :icon="
                analysisExpanded ? 'lucide:chevron-up' : 'lucide:chevron-down'
              "
            />
          </button>

          <div v-show="analysisExpanded" class="inventory-insight-body">
            <div class="inventory-metrics">
              <div class="inventory-metric-panel">
                <div
                  class="inventory-metric-panel__icon inventory-metric-panel__icon--blue"
                >
                  <IconifyIcon icon="lucide:boxes" />
                </div>
                <div>
                  <Statistic
                    title="全部在位成品"
                    :value="numberValue(summary?.onHandQty)"
                    suffix="PCS"
                    :value-style="{
                      color: '#2563eb',
                      fontSize: '22px',
                      fontWeight: 700,
                    }"
                  />
                  <div class="inventory-metric-panel__sub">
                    当前成品库位中的全部在位成品
                  </div>
                </div>
              </div>
              <div class="inventory-metric-panel">
                <div
                  class="inventory-metric-panel__icon inventory-metric-panel__icon--green"
                >
                  <IconifyIcon icon="lucide:circle-check" />
                </div>
                <div>
                  <Statistic
                    title="合格成品"
                    :value="numberValue(summary?.qualifiedQty)"
                    suffix="PCS"
                    :value-style="{
                      color: '#15803d',
                      fontSize: '22px',
                      fontWeight: 700,
                    }"
                  />
                  <div class="inventory-metric-panel__sub">质量状态为合格</div>
                </div>
              </div>
              <div class="inventory-metric-panel">
                <div
                  class="inventory-metric-panel__icon inventory-metric-panel__icon--red"
                >
                  <IconifyIcon icon="lucide:circle-x" />
                </div>
                <div>
                  <Statistic
                    title="不合格成品"
                    :value="numberValue(summary?.unqualifiedQty)"
                    suffix="PCS"
                    :value-style="{
                      color: '#dc2626',
                      fontSize: '22px',
                      fontWeight: 700,
                    }"
                  />
                  <div class="inventory-metric-panel__sub">
                    质量状态为不合格
                  </div>
                </div>
              </div>
            </div>

            <div class="inventory-charts">
              <section class="inventory-panel inventory-panel--distribution">
                <div class="inventory-panel__head">
                  <IconifyIcon icon="lucide:bar-chart-3" />
                  <span>{{ currentDimensionLabel }}库存分布</span>
                  <em>默认按型号汇总</em>
                </div>
                <EchartsUI
                  v-if="distributionRows.length > 0"
                  ref="distributionChartRef"
                  class="inventory-chart"
                />
                <Empty
                  v-else
                  class="inventory-empty"
                  description="当前筛选条件下暂无成品库位库存"
                />
              </section>
              <section class="inventory-panel">
                <div class="inventory-panel__head">
                  <IconifyIcon icon="lucide:chart-line" />
                  <span>成品库位出入库变化趋势</span>
                  <em>退回重包不计入实际出库</em>
                </div>
                <EchartsUI
                  v-if="movementRows.length > 0"
                  ref="movementChartRef"
                  class="inventory-chart"
                />
                <Empty
                  v-else
                  class="inventory-empty"
                  description="选定日期范围内暂无成品库位出入库流水"
                />
              </section>
            </div>
          </div>
        </section>

        <section class="inventory-scope-note">
          <IconifyIcon icon="lucide:circle-info" />
          <span>
            统计范围：仅统计“成品库位管理”中有效 PACKAGE_FG
            库位的在位成品；汇总卡与柱状图按所选入库日期筛选，趋势图展示该期间的入库、实际出库和退回重包事实。
          </span>
        </section>
      </div>
    </Spin>
  </Page>
</template>

<style scoped>
.inventory-analysis-page {
  height: 100%;
  background: #f6f8fb;
}

.inventory-toolbar {
  border-bottom: 1px solid #dbe3ef;
  background: #ffffff;
  padding: 0 16px;
}

.inventory-query-toggle {
  display: flex;
  width: 100%;
  min-height: 44px;
  align-items: center;
  gap: 12px;
  border: 0;
  background: #ffffff;
  color: #1e293b;
  cursor: pointer;
  font: inherit;
  padding: 0;
  text-align: left;
}

.inventory-query-toggle:hover {
  background: #f8fbff;
}

.inventory-query-toggle__title {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: #1e293b;
  font-weight: 600;
}

.inventory-query-toggle__title :deep(svg) {
  color: #2563eb;
  font-size: 16px;
}

.inventory-query-toggle__summary {
  margin-left: auto;
  color: #64748b;
  font-size: 12px;
}

.inventory-query-form {
  border-top: 1px solid #edf1f7;
  padding: 12px 0 10px;
  width: 100%;
}

.inventory-query-form :deep(.ant-form-item) {
  margin: 0;
}

.inventory-query-form :deep(.ant-form-item-label) {
  min-width: 68px;
  padding-right: 8px;
  text-align: right;
}

.inventory-query-form :deep(.ant-form-item-label > label) {
  height: 32px;
  color: #475569;
  font-size: 13px;
}

.inventory-query-grid {
  display: grid;
  width: 100%;
  align-items: center;
  gap: 10px 12px;
  grid-template-columns: repeat(3, minmax(250px, 1fr));
}

.inventory-query-item {
  min-width: 0;
}

.inventory-query-item :deep(.ant-form-item-control),
.inventory-query-item :deep(.ant-form-item-control-input),
.inventory-query-item :deep(.ant-form-item-control-input-content) {
  min-width: 0;
  width: 100%;
}

.inventory-query-item :deep(.ant-input),
.inventory-query-item :deep(.ant-picker),
.inventory-query-item :deep(.ant-select),
.inventory-top-n {
  width: 100%;
}

.inventory-query-actions {
  justify-self: stretch;
}

.inventory-query-actions :deep(.ant-form-item-control-input-content) {
  display: flex;
  width: 100%;
  align-items: center;
  gap: 8px;
  justify-content: flex-end;
}

.inventory-content {
  display: flex;
  min-height: 0;
  flex-direction: column;
  gap: 12px;
  padding: 12px;
}

.inventory-page-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 0 2px;
}

.inventory-page-heading__title {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #2563eb;
}

.inventory-page-heading__title :deep(svg) {
  font-size: 22px;
}

.inventory-page-heading h2 {
  margin: 0;
  color: #1e293b;
  font-size: 16px;
  font-weight: 600;
}

.inventory-page-heading p {
  margin: 3px 0 0;
  color: #64748b;
  font-size: 12px;
}

.inventory-scope-chip {
  border: 1px solid #bfdbfe;
  background: #eff6ff;
  color: #1d4ed8;
  font-size: 12px;
  padding: 4px 8px;
  white-space: nowrap;
}

.inventory-insight-panel,
.inventory-panel,
.inventory-scope-note {
  border: 1px solid #dbe3ef;
  background: #ffffff;
}

.inventory-insight-toggle {
  display: flex;
  width: 100%;
  min-height: 42px;
  align-items: center;
  gap: 12px;
  border: 0;
  background: #ffffff;
  color: #1e293b;
  cursor: pointer;
  font: inherit;
  padding: 0 12px;
  text-align: left;
}

.inventory-insight-toggle:hover {
  background: #f8fbff;
}

.inventory-insight-toggle__title {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
}

.inventory-insight-toggle__metrics {
  margin-left: auto;
  color: #64748b;
  font-size: 12px;
}

.inventory-insight-body {
  display: grid;
  gap: 12px;
  border-top: 1px solid #edf1f7;
  padding: 12px;
}

.inventory-metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.inventory-metric-panel {
  display: flex;
  min-height: 86px;
  align-items: center;
  gap: 12px;
  border: 1px solid #dbe3ef;
  background: #ffffff;
  padding: 14px;
}

.inventory-metric-panel__icon {
  display: grid;
  width: 38px;
  height: 38px;
  flex: 0 0 auto;
  place-items: center;
  border: 1px solid currentcolor;
  background: #f8fafc;
  font-size: 20px;
}

.inventory-metric-panel__icon--blue {
  color: #2563eb;
}

.inventory-metric-panel__icon--green {
  color: #15803d;
}

.inventory-metric-panel__icon--red {
  color: #dc2626;
}

.inventory-metric-panel__sub {
  margin-top: 2px;
  color: #64748b;
  font-size: 12px;
}

.inventory-charts {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 12px;
}

.inventory-panel {
  min-width: 0;
}

.inventory-panel__head {
  display: flex;
  min-height: 42px;
  align-items: center;
  gap: 8px;
  border-bottom: 1px solid #edf1f7;
  padding: 0 12px;
  color: #1e293b;
  font-weight: 600;
}

.inventory-panel__head em {
  margin-left: auto;
  color: #64748b;
  font-size: 12px;
  font-style: normal;
  font-weight: 400;
}

.inventory-chart {
  height: 288px;
  width: 100%;
}

.inventory-empty {
  display: grid;
  height: 288px;
  place-content: center;
}

.inventory-scope-note {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  background: #f8fbff;
  color: #64748b;
  font-size: 12px;
  line-height: 20px;
  padding: 10px 12px;
}

.inventory-scope-note :deep(svg) {
  margin-top: 2px;
  flex: 0 0 auto;
  color: #2563eb;
  font-size: 15px;
}

@media (max-width: 1440px) {
  .inventory-query-grid {
    grid-template-columns: repeat(3, minmax(220px, 1fr));
  }
}

@media (max-width: 1280px) {
  .inventory-query-grid {
    grid-template-columns: repeat(2, minmax(250px, 1fr));
  }

  .inventory-query-actions {
    grid-column: 1 / -1;
  }

  .inventory-metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 960px) {
  .inventory-query-grid,
  .inventory-metrics,
  .inventory-charts {
    grid-template-columns: 1fr;
  }

  .inventory-query-actions :deep(.ant-form-item-control-input-content) {
    justify-content: flex-start;
  }

  .inventory-insight-toggle {
    align-items: flex-start;
    flex-direction: column;
    padding: 10px 12px;
  }

  .inventory-insight-toggle__metrics {
    margin-left: 0;
  }

  .inventory-query-toggle {
    align-items: flex-start;
    flex-wrap: wrap;
    gap: 4px 8px;
    padding: 10px 0;
  }

  .inventory-query-toggle__summary {
    width: calc(100% - 24px);
    margin-left: 24px;
  }

  .inventory-page-heading {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
