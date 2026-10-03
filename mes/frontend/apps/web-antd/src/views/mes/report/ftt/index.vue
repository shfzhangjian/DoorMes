<script lang="ts" setup>
import { ref, reactive, onMounted, nextTick } from 'vue';
import { Page } from '@vben/common-ui';
import { Card, Button, Table, Tag, Statistic, Select, DatePicker, Row, Col, Input, Progress } from 'ant-design-vue';
import { IconifyIcon } from '@vben/icons';

// 引入 Vben 内置的 Echarts 组件与 hooks
import { EchartsUI, useEcharts } from '@vben/plugins/echarts';
import type { EchartsUIType } from '@vben/plugins/echarts';

// 引入 API 接口
import { getFttOverview, getFttPage } from '#/api/mes/report/ftt';

defineOptions({ name: 'MesFttAnalysis' });

// --- 状态与数据 ---
const loading = ref(false);
const overviewData = ref<any>({});
const reportList = ref<any[]>([]);
const total = ref(0);

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  dateRange: [] as string[],
  workshopId: 'W01',
  materialName: '',
  processName: undefined
});

// --- 图表定义 ---
const trendChartRef = ref<EchartsUIType>();
const { renderEcharts } = useEcharts(trendChartRef);

// --- 表格列定义 ---
const tableColumns = [
  { title: '生产日期', dataIndex: 'productionDate', width: 110, align: 'center', fixed: 'left' },
  { title: '生产工单', dataIndex: 'workOrderNo', minWidth: 160 },
  { title: '物料品名', dataIndex: 'materialName', minWidth: 180 },
  { title: '发生工序', dataIndex: 'processName', width: 120, align: 'center' },
  { title: '总投入数量', dataIndex: 'inputQty', width: 100, align: 'right' },
  { title: '一次合格数', dataIndex: 'firstPassQty', width: 100, align: 'right' },
  { title: '返工数量', dataIndex: 'reworkQty', width: 90, align: 'right' },
  { title: '报废数量', dataIndex: 'scrapQty', width: 90, align: 'right' },
  { title: '一次合格率(%)', dataIndex: 'fttRate', width: 120, align: 'right', fixed: 'right' }
];

onMounted(async () => {
  await loadData();
});

// --- 数据加载核心逻辑 ---
async function loadData() {
  loading.value = true;
  try {
    const [overviewRes, pageRes] = await Promise.all([
      getFttOverview(queryParams),
      getFttPage(queryParams)
    ]);

    overviewData.value = overviewRes;
    reportList.value = pageRes.items;
    total.value = pageRes.total;

    nextTick(() => {
      drawTrendChart(overviewRes.trend);
    });
  } finally {
    loading.value = false;
  }
}

// --- 绘制一次合格率与投入量双轴趋势图 ---
function drawTrendChart(trendData: any) {
  if (!trendData) return;

  renderEcharts({
    title: { text: '投入产量与一次合格率趋势', textStyle: { fontSize: 14, fontWeight: 'normal', color: '#334155' } },
    tooltip: { trigger: 'axis', axisPointer: { type: 'cross' } },
    legend: { data: ['投入数量', '一次合格率(FTT)'], right: 0 },
    grid: { left: '1%', right: '2%', bottom: '2%', top: '20%', containLabel: true },
    xAxis: { type: 'category', data: trendData.dates },
    yAxis: [
      { type: 'value', name: '数量 (PCS/KG)', position: 'left' },
      { type: 'value', name: 'FTT(%)', min: 90, max: 100, position: 'right', axisLabel: { formatter: '{value}%' } }
    ],
    series: [
      { name: '投入数量', type: 'bar', barWidth: '35%', itemStyle: { color: '#93c5fd' }, data: trendData.inputQty },
      { name: '一次合格率(FTT)', type: 'line', yAxisIndex: 1, smooth: true, itemStyle: { color: '#10b981' }, symbolSize: 8, lineStyle: { width: 3 }, data: trendData.fttRate }
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
        <Select v-model:value="queryParams.processName" :options="[{label:'基材涂布', value:'OP20'}, {label:'表面精抛', value:'OP30'}, {label:'压延成型', value:'OP40'}]" allowClear class="w-32" placeholder="全部" />
      </div>
      <Button type="primary" @click="handleQuery" :loading="loading">
        <template #icon><IconifyIcon icon="lucide:search" /></template>查询
      </Button>
    </div>

    <div class="flex-1 flex flex-col gap-4 p-4 min-h-0 overflow-y-auto">

      <Row :gutter="16" class="shrink-0">
        <Col :span="6">
          <Card size="small" class="bg-green-50 border-green-100 shadow-sm">
            <Statistic title="全局平均一次合格率 (FTT)" :value="overviewData.avgFtt || 0" suffix="%" :value-style="{ color: '#15803d', fontWeight: 'bold' }" />
            <div class="mt-2">
              <Progress :percent="overviewData.avgFtt" :show-info="false" stroke-color="#10b981" size="small" />
            </div>
            <div class="text-xs text-slate-500 mt-2 flex items-center justify-between">
              <span>零缺陷直通率指标</span>
              <IconifyIcon icon="lucide:check-circle" class="text-green-500" />
            </div>
          </Card>
        </Col>
        <Col :span="6">
          <Card size="small" class="bg-blue-50 border-blue-100 shadow-sm">
            <Statistic title="总投入加工数量" :value="overviewData.totalInputQty || 0" :value-style="{ color: '#1d4ed8', fontWeight: 'bold' }" />
            <div class="text-xs text-slate-500 mt-6 flex items-center justify-between">
              <span>区间内投产总量 (作为分母)</span>
              <IconifyIcon icon="lucide:arrow-down-to-line" class="text-blue-500" />
            </div>
          </Card>
        </Col>
        <Col :span="6">
          <Card size="small" class="bg-indigo-50 border-indigo-100 shadow-sm">
            <Statistic title="一次合格总数" :value="overviewData.firstPassQty || 0" :value-style="{ color: '#4338ca', fontWeight: 'bold' }" />
            <div class="text-xs text-slate-500 mt-6 flex items-center justify-between">
              <span>未触发返工/报废的直接良品</span>
              <IconifyIcon icon="lucide:medal" class="text-indigo-500" />
            </div>
          </Card>
        </Col>
        <Col :span="6">
          <Card size="small" class="bg-orange-50 border-orange-100 shadow-sm">
            <Statistic title="返工与报废拦截总数" :value="overviewData.reworkScrapQty || 0" :value-style="{ color: '#ea580c', fontWeight: 'bold' }" />
            <div class="text-xs text-slate-500 mt-6 flex items-center justify-between">
              <span>造成 FTT 折损的异常品合计</span>
              <IconifyIcon icon="lucide:alert-triangle" class="text-orange-500" />
            </div>
          </Card>
        </Col>
      </Row>

      <div class="bg-white p-4 rounded-lg shadow-sm border border-slate-200 h-[280px] shrink-0 relative">
        <EchartsUI ref="trendChartRef" class="w-full h-full" />
      </div>

      <div class="bg-white rounded-lg shadow-sm border border-slate-200 flex-1 min-h-[300px] flex flex-col overflow-hidden mt-2">
        <div class="p-3 border-b font-bold text-slate-700 bg-slate-50 flex items-center gap-2">
          <IconifyIcon icon="lucide:file-bar-chart-2" /> 直通率(FTT)工单明细记录
        </div>
        <Table
          :columns="tableColumns"
          :dataSource="reportList"
          :loading="loading"
          size="small"
          :scroll="{ x: 'max-content' }"
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
              <span class="font-mono text-indigo-700 font-bold hover:underline cursor-pointer">{{ record.workOrderNo }}</span>
            </template>

            <template v-if="column.dataIndex === 'inputQty'">
              <span class="font-bold text-slate-700">{{ record.inputQty }}</span>
            </template>

            <template v-if="column.dataIndex === 'firstPassQty'">
              <span class="font-bold text-green-600">{{ record.firstPassQty }}</span>
            </template>

            <template v-if="column.dataIndex === 'reworkQty'">
              <span :class="record.reworkQty > 0 ? 'text-orange-500 font-bold' : 'text-slate-400'">{{ record.reworkQty }}</span>
            </template>
            <template v-if="column.dataIndex === 'scrapQty'">
              <span :class="record.scrapQty > 0 ? 'text-red-500 font-bold' : 'text-slate-400'">{{ record.scrapQty }}</span>
            </template>

            <template v-if="column.dataIndex === 'fttRate'">
              <Tag :color="record.fttRate >= 98.0 ? 'success' : 'error'" class="!m-0 border-none font-bold">
                {{ record.fttRate }}%
              </Tag>
            </template>

          </template>
        </Table>
      </div>

    </div>
  </Page>
</template>
