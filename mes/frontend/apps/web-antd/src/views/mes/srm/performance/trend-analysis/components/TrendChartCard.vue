<script lang="ts" setup>
import type { EchartsUIType } from '@vben/plugins/echarts';

import type { SrmQuarterlyPerformanceApi } from '#/api/mes/srm/assessment/quarterly-performance';

import { nextTick, ref, watch } from 'vue';

import { EchartsUI, useEcharts } from '@vben/plugins/echarts';

const props = defineProps<{
  panel: SrmQuarterlyPerformanceApi.TrendPanel;
}>();

const chartRef = ref<EchartsUIType>();
const { renderEcharts } = useEcharts(chartRef);

const chartColors = ['#ed7d31', '#2f75b5', '#a6a6a6', '#70ad47', '#ffc000'];

watch(
  () => props.panel,
  async () => {
    await nextTick();
    await renderEcharts(buildChartOption(), true);
  },
  { deep: true, immediate: true },
);

function buildChartOption() {
  const months = props.panel.months?.length
    ? props.panel.months
    : ['01', '02', '03', '04', '05', '06', '07', '08', '09', '10', '11', '12'];
  const hasPercent = props.panel.series?.some((series) => series.percent);
  const hasPlain = props.panel.series?.some((series) => !series.percent);
  const percentAxisIndex = hasPlain ? 1 : 0;
  const yAxis: Record<string, any>[] = [];
  if (hasPlain || !hasPercent) {
    yAxis.push({
      axisLabel: { color: '#2f5597' },
      axisLine: { show: false },
      splitLine: { lineStyle: { color: '#d9e2ef' } },
      type: 'value',
    });
  }
  if (hasPercent) {
    yAxis.push({
      axisLabel: {
        color: '#2f5597',
        formatter: '{value}%',
      },
      axisLine: { show: false },
      max: 100,
      min: 0,
      splitLine: { show: !hasPlain, lineStyle: { color: '#d9e2ef' } },
      type: 'value',
    });
  }
  if (props.panel.chartType === 'SCORE') {
    yAxis[0] = {
      axisLabel: { color: '#2f5597' },
      axisLine: { show: false },
      max: 100,
      min: 90,
      splitLine: { lineStyle: { color: '#d9e2ef' } },
      type: 'value',
    };
  }
  return {
    color: chartColors,
    grid: {
      bottom: 44,
      containLabel: true,
      left: 40,
      right: hasPercent && hasPlain ? 56 : 28,
      top: 36,
    },
    legend: {
      data: (props.panel.series || []).map((series) => series.name),
      itemHeight: 8,
      itemWidth: 28,
      right: 56,
      textStyle: { color: '#2f5597', fontSize: 12 },
      top: 4,
    },
    series: (props.panel.series || []).map((series, index) => ({
      barMaxWidth: 24,
      connectNulls: false,
      data: series.values || [],
      name: series.name,
      smooth: series.type === 'line',
      symbolSize: series.type === 'line' ? 6 : 0,
      type: series.type === 'bar' ? 'bar' : 'line',
      yAxisIndex: series.percent ? percentAxisIndex : 0,
      z: index + 1,
    })),
    tooltip: {
      appendToBody: true,
      confine: true,
      trigger: 'axis',
      valueFormatter: (value: unknown) => {
        if (value === null || value === undefined || value === '') {
          return '-';
        }
        return String(value);
      },
    },
    xAxis: {
      axisLabel: { color: '#2f5597' },
      axisLine: { lineStyle: { color: '#c9d5e4' } },
      axisTick: {
        show: true,
        alignWithLabel: true,
        lineStyle: { color: '#c9d5e4' },
      },
      boundaryGap: true,
      data: months,
      type: 'category',
    },
    yAxis,
  };
}
</script>

<template>
  <section class="srm-trend-card">
    <div class="srm-trend-card__title">{{ panel.title }}</div>
    <div class="srm-trend-card__chart">
      <EchartsUI ref="chartRef" class="srm-trend-chart" />
    </div>
    <div class="srm-trend-card__table-wrap">
      <table class="srm-trend-table">
        <thead>
          <tr>
            <th>月份</th>
            <th v-for="month in panel.months" :key="month">{{ month }}</th>
            <th>{{ panel.summaryLabel || '综合' }}</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in panel.rows" :key="row.label">
            <th>{{ row.label }}</th>
            <td v-for="(value, index) in row.values" :key="index">
              {{ value || '-' }}
            </td>
            <td>{{ row.summary || '-' }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>
</template>

<style scoped>
.srm-trend-card {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.srm-trend-card + .srm-trend-card {
  margin-top: 18px;
}

.srm-trend-card__title {
  margin-bottom: 6px;
  color: #111827;
  font-size: 15px;
  font-weight: 800;
  line-height: 24px;
}

.srm-trend-card__chart {
  position: relative;
  z-index: 0;
  flex: 0 0 228px;
  height: 228px;
  overflow: hidden;
  border: 2px solid #5b9bd5;
  border-bottom: 0;
  background: #fff;
}

.srm-trend-chart {
  display: block;
  height: 100%;
  width: 100%;
}

.srm-trend-card__table-wrap {
  position: relative;
  z-index: 1;
  background: #fff;
  overflow-x: auto;
}

.srm-trend-table {
  width: 100%;
  min-width: 980px;
  border-collapse: collapse;
  table-layout: fixed;
  background: #fff;
}

.srm-trend-table th,
.srm-trend-table td {
  height: 32px;
  border: 1px solid #1f2937;
  color: #111827;
  font-size: 13px;
  line-height: 20px;
  padding: 4px 6px;
  text-align: center;
  white-space: nowrap;
}

.srm-trend-table thead th,
.srm-trend-table tbody th {
  background: #d9d9d9;
  font-weight: 800;
}

.srm-trend-table thead th:first-child,
.srm-trend-table tbody th {
  width: 118px;
}
</style>
