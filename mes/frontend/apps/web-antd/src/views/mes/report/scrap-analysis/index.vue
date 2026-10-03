<script lang="ts" setup>
import { ref, reactive, onMounted, nextTick } from 'vue';
import { Page } from '@vben/common-ui';
import { Card, Button, Table, Tag, Statistic, Select, DatePicker, Row, Col, Input } from 'ant-design-vue';
import { IconifyIcon } from '@vben/icons';

// 引入 Vben 内置的 Echarts 组件与 hooks
import { EchartsUI, useEcharts } from '@vben/plugins/echarts';
import type { EchartsUIType } from '@vben/plugins/echarts';

// 引入 API 接口
import { getScrapOverview, getScrapPage } from '#/api/mes/report/scrap';

defineOptions({ name: 'MesScrapAnalysis' });

// --- 状态与数据 ---
const loading = ref(false);
const overviewData = ref<any>({});
const reportList = ref<any[]>([]);
const total = ref(0);

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  dateRange: [] as string[],
  workshopId: 'W01', // 默认指定车间，避免不同产品量纲汇总失去意义
  materialName: '',
  processName: undefined
});

// --- 图表定义 ---
const trendChartRef = ref<EchartsUIType>();
const paretoChartRef = ref<EchartsUIType>();
const { renderEcharts: renderTrendChart } = useEcharts(trendChartRef);
const { renderEcharts: renderParetoChart } = useEcharts(paretoChartRef);

// --- 表格列定义 ---
const tableColumns = [
  { title: '生产日期', dataIndex: 'productionDate', width: 110, align: 'center', fixed: 'left' },
  { title: '生产工单', dataIndex: 'workOrderNo', minWidth: 150 },
  { title: '物料品名', dataIndex: 'materialName', minWidth: 160 },
  { title: '发生工序', dataIndex: 'processName', width: 120, align: 'center' },
  { title: '检验总数', dataIndex: 'totalQty', width: 100, align: 'right' },
  { title: '报废数量', dataIndex: 'scrapQty', width: 100, align: 'right' },
  { title: '废品率(%)', dataIndex: 'scrapRate', width: 100, align: 'right' },
  { title: '主要缺陷', dataIndex: 'mainDefect', width: 140, align: 'center' }
];

onMounted(async () => {
  await loadData();
});

// --- 数据加载核心逻辑 ---
async function loadData() {
  loading.value = true;
  try {
    const [overviewRes, pageRes] = await Promise.all([
      getScrapOverview(queryParams),
      getScrapPage(queryParams)
    ]);

    overviewData.value = overviewRes;
    reportList.value = pageRes.items;
    total.value = pageRes.total;

    nextTick(() => {
      drawTrendChart(overviewRes.trend);
      drawParetoChart(overviewRes.pareto);
    });
  } finally {
    loading.value = false;
  }
}

// --- 绘制废品率趋势图 ---
function drawTrendChart(trendData: any) {
  if (!trendData) return;

  renderTrendChart({
    title: { text: '废品数量与不良率趋势', textStyle: { fontSize: 14, fontWeight: 'normal' } },
    tooltip: { trigger: 'axis', axisPointer: { type: 'cross' } },
    legend: { data: ['报废数量', '废品率'], right: 0 },
    grid: { left: '1%', right: '2%', bottom: '2%', top: '20%', containLabel: true },
    xAxis: { type: 'category', data: trendData.dates },
    yAxis: [
      { type: 'value', name: '数量 (PCS)', position: 'left' },
      { type: 'value', name: '废品率(%)', position: 'right', axisLabel: { formatter: '{value}%' } }
    ],
    series: [
      { name: '报废数量', type: 'bar', barWidth: '35%', itemStyle: { color: '#ef4444' }, data: trendData.scrapQty },
      { name: '废品率', type: 'line', yAxisIndex: 1, smooth: true, itemStyle: { color: '#f59e0b' }, symbolSize: 8, data: trendData.scrapRate }
    ]
  });
}

// --- 绘制缺陷原因柏拉图 (Pareto) ---
function drawParetoChart(paretoData: any[]) {
  if (!paretoData || paretoData.length === 0) return;

  renderParetoChart({
    title: { text: '不良原因柏拉图 (TOP 5)', textStyle: { fontSize: 14, fontWeight: 'normal' } },
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    legend: { data: ['不良频次', '累计占比'], right: 0 },
    grid: { left: '1%', right: '2%', bottom: '2%', top: '20%', containLabel: true },
    xAxis: { type: 'category', data: paretoData.map(d => d.reason), axisLabel: { interval: 0, rotate: 30 } },
    yAxis: [
      { type: 'value', name: '频次', position: 'left' },
      { type: 'value', name: '累计(%)', min: 0, max: 100, position: 'right', axisLabel: { formatter: '{value}%' } }
    ],
    series: [
      { name: '不良频次', type: 'bar', barWidth: '40%', itemStyle: { color: '#6366f1' }, data: paretoData.map(d => d.qty) },
      { name: '累计占比', type: 'line', yAxisIndex: 1, itemStyle: { color: '#10b981' }, lineStyle: { width: 2 }, data: paretoData.map(d => d.cumulativeRate) }
    ]
  });
}

// --- 事件处理 ---
function handleQuery() {
  queryParams.pageNum = 1;
  loadData();
}

function handleTableChange(pagination: any) {
  queryParams.pageNum = pagination.current;
  queryParams.pageSize = pagination.pageSize;
  loadData();
}
</script>

<template>
  <Page auto-content-height class="bg-[#f4f6f8] relative">

    <div class="bg-white px-4 py-3 border-b flex gap-4 items-center shrink-0 shadow-sm z-10 relative flex-wrap">
      <div class="flex items-center gap-2">
        <span class="text-xs font-bold text-slate-500">统计区间:</span>
        <DatePicker.RangePicker v-model:value="queryParams.dateRange" valueFormat="YYYY-MM-DD" class="w-64" />
      </div>
      <div class="flex items-center gap-2">
        <span class="text-xs font-bold text-slate-500">车间:</span>
        <Select v-model:value="queryParams.workshopId" :options="[{label:'抛光材料一车间', value:'W01'}, {label:'配液二车间', value:'W02'}]" class="w-40" />
      </div>
      <div class="flex items-center gap-2">
        <span class="text-xs font-bold text-slate-500">物料品名:</span>
        <Input v-model:value="queryParams.materialName" placeholder="模糊搜索" class="w-40" allowClear />
      </div>
      <div class="flex items-center gap-2">
        <span class="text-xs font-bold text-slate-500">工序:</span>
        <Select v-model:value="queryParams.processName" :options="[{label:'全部工序', value:''}, {label:'表面精抛', value:'OP30'}, {label:'精密分切', value:'OP40'}]" allowClear class="w-32" placeholder="全部" />
      </div>
      <Button type="primary" @click="handleQuery" :loading="loading">
        <template #icon><IconifyIcon icon="lucide:search" /></template>查询
      </Button>
    </div>

    <div class="flex-1 flex flex-col gap-4 p-4 min-h-0 overflow-y-auto">

      <Row :gutter="16" class="shrink-0">
        <Col :span="6">
          <Card size="small" class="bg-red-50 border-red-100 shadow-sm">
            <Statistic title="全局平均废品率" :value="overviewData.avgScrapRate || 0" suffix="%" :value-style="{ color: '#dc2626', fontWeight: 'bold' }" />
            <div class="text-xs text-slate-500 mt-2 flex items-center gap-1">
              <IconifyIcon icon="lucide:pie-chart" class="text-red-500" /> 总报废量 ÷ (合格量 + 报废量)
            </div>
          </Card>
        </Col>
        <Col :span="6">
          <Card size="small" class="bg-orange-50 border-orange-100 shadow-sm">
            <Statistic title="累计报废数量" :value="overviewData.totalScrapQty || 0" :value-style="{ color: '#ea580c', fontWeight: 'bold' }" />
            <div class="text-xs text-slate-500 mt-2 flex items-center gap-1">
              <IconifyIcon icon="lucide:trash-2" class="text-orange-500" /> 区间内确认报废总数
            </div>
          </Card>
        </Col>
        <Col :span="6">
          <Card size="small" class="bg-indigo-50 border-indigo-100 shadow-sm">
            <Statistic title="预估报废成本(元)" :value="overviewData.estScrapCost || 0" :value-style="{ color: '#4338ca', fontWeight: 'bold' }" />
            <div class="text-xs text-slate-500 mt-2 flex items-center gap-1">
              <IconifyIcon icon="lucide:calculator" class="text-indigo-500" /> 基于物料标准成本估算
            </div>
          </Card>
        </Col>
        <Col :span="6">
          <Card size="small" class="bg-slate-50 border-slate-200 shadow-sm">
            <div class="text-xs text-slate-500 mb-1">首要不良原因 (Top 1)</div>
            <div class="font-black text-lg text-slate-800 h-[32px] flex items-center">
              {{ overviewData.topDefectReason || '-' }}
            </div>
            <div class="text-xs text-slate-500 mt-2 flex items-center gap-1">
              <IconifyIcon icon="lucide:alert-triangle" class="text-slate-500" /> 占比最高的缺陷项
            </div>
          </Card>
        </Col>
      </Row>

      <Row :gutter="16" class="shrink-0 h-[280px]">
        <Col :span="14">
          <div class="bg-white p-4 rounded-lg shadow-sm border border-slate-200 h-full relative">
            <EchartsUI ref="trendChartRef" class="w-full h-full" />
          </div>
        </Col>
        <Col :span="10">
          <div class="bg-white p-4 rounded-lg shadow-sm border border-slate-200 h-full relative">
            <EchartsUI ref="paretoChartRef" class="w-full h-full" />
          </div>
        </Col>
      </Row>

      <div class="bg-white rounded-lg shadow-sm border border-slate-200 flex-1 min-h-[300px] flex flex-col overflow-hidden mt-4">
        <div class="p-3 border-b font-bold text-slate-700 bg-slate-50 flex items-center gap-2">
          <IconifyIcon icon="lucide:file-x-2" /> 报废明细记录
        </div>
        <Table
          :columns="tableColumns"
          :dataSource="reportList"
          :loading="loading"
          size="small"
          :pagination="{
            total: total,
            current: queryParams.pageNum,
            pageSize: queryParams.pageSize,
            showSizeChanger: true,
            showTotal: (t) => `共 ${t} 条记录`
          }"
          @change="handleTableChange"
          class="p-2"
        >
          <template #bodyCell="{ column, record }">

            <template v-if="column.dataIndex === 'workOrderNo'">
              <span class="font-mono text-indigo-700 font-bold">{{ record.workOrderNo }}</span>
            </template>

            <template v-if="column.dataIndex === 'scrapRate'">
              <span :class="record.scrapRate >= 2.0 ? 'text-red-600 font-bold' : ''">{{ record.scrapRate }}%</span>
            </template>

            <template v-if="column.dataIndex === 'mainDefect'">
              <Tag color="error" class="!m-0 border-none">{{ record.mainDefect }}</Tag>
            </template>

          </template>
        </Table>
      </div>

    </div>
  </Page>
</template>
