<script lang="ts" setup>
import { ref, reactive, onMounted, nextTick } from 'vue';
import { Page } from '@vben/common-ui';
import { Card, Button, Table, Tag, Statistic, Select, DatePicker, Row, Col } from 'ant-design-vue';
import { IconifyIcon } from '@vben/icons';

// 引入 Vben 内置的 Echarts 组件与 hooks (解决 Vite 编译报错)
import { EchartsUI, useEcharts } from '@vben/plugins/echarts';
import type { EchartsUIType } from '@vben/plugins/echarts';

// 引入 API 接口 (复用前文定义的 API)
import { getDailyPage, getDailyOverview } from '#/api/mes/report/daily';

defineOptions({ name: 'MesDailyReport' });

// --- 状态与数据 ---
const loading = ref(false);
const overviewData = ref<any>({});
const reportList = ref<any[]>([]);
const total = ref(0);

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  dateRange: [] as string[],
  workshopId: 'W01', // 默认选中一车间，规避不同量纲相加
  shiftId: ''
});

// --- 图表定义 ---
const chartRef = ref<EchartsUIType>();
const { renderEcharts } = useEcharts(chartRef);

// --- 表格列定义 (Ant Design Vue 规范) ---
const tableColumns = [
  { title: '生产日期', dataIndex: 'productionDate', width: 120, align: 'center' },
  { title: '班别', dataIndex: 'shiftName', width: 90, align: 'center' },
  { title: '生产工单', dataIndex: 'workOrderNo', minWidth: 160 },
  { title: '产品名称', dataIndex: 'itemName', minWidth: 180, ellipsis: true },
  { title: '排产数量', dataIndex: 'plannedQty', width: 100, align: 'right' },
  { title: '实际产出', dataIndex: 'actualQty', width: 120, align: 'right' },
  { title: '合格数量', dataIndex: 'goodQty', width: 100, align: 'right' },
  { title: '报废数量', dataIndex: 'scrapQty', width: 100, align: 'right' },
  { title: '合格率(%)', dataIndex: 'yieldRate', width: 100, align: 'right' },
];

onMounted(async () => {
  await loadData();
});

// --- 数据加载核心逻辑 ---
async function loadData() {
  loading.value = true;
  try {
    // 并发请求看板数据和表格明细数据
    const [overviewRes, pageRes] = await Promise.all([
      getDailyOverview(queryParams),
      getDailyPage(queryParams)
    ]);

    overviewData.value = overviewRes.summary;
    reportList.value = pageRes.items;
    total.value = pageRes.total;

    // 渲染图表
    nextTick(() => {
      renderTrendChart(overviewRes.trend);
    });
  } finally {
    loading.value = false;
  }
}

// --- Echarts 渲染逻辑 ---
function renderTrendChart(trendData: any) {
  if (!trendData) return;

  renderEcharts({
    tooltip: { trigger: 'axis', axisPointer: { type: 'cross' } },
    legend: { data: ['实际产出', '合格率'], right: 0 },
    grid: { left: '1%', right: '2%', bottom: '2%', top: '15%', containLabel: true },
    xAxis: { type: 'category', data: trendData.dates },
    yAxis: [
      { type: 'value', name: '产量', position: 'left' },
      { type: 'value', name: '合格率(%)', min: 95, max: 100, position: 'right', axisLabel: { formatter: '{value}' } }
    ],
    series: [
      { name: '实际产出', type: 'bar', barWidth: 30, itemStyle: { color: '#0960bd' }, data: trendData.actualOutput },
      { name: '合格率', type: 'line', yAxisIndex: 1, smooth: true, itemStyle: { color: '#eab308' }, data: trendData.yieldRates }
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

    <div class="bg-white px-4 py-3 border-b flex gap-4 items-center shrink-0 shadow-sm z-10 relative">
      <div class="flex items-center gap-2">
        <span class="text-xs font-bold text-slate-500">生产日期:</span>
        <DatePicker.RangePicker v-model:value="queryParams.dateRange" valueFormat="YYYY-MM-DD" class="w-64" />
      </div>
      <div class="flex items-center gap-2">
        <span class="text-xs font-bold text-slate-500">生产车间:</span>
        <Select v-model:value="queryParams.workshopId" :options="[{label:'抛光材料一车间 (PCS)', value:'W01'}, {label:'配液二车间 (KG)', value:'W02'}]" class="w-48" />
      </div>
      <div class="flex items-center gap-2">
        <span class="text-xs font-bold text-slate-500">班别:</span>
        <Select v-model:value="queryParams.shiftId" :options="[{label:'全部', value:''}, {label:'早班', value:'S1'}, {label:'晚班', value:'S2'}]" class="w-28" />
      </div>
      <Button type="primary" @click="handleQuery" :loading="loading">
        <template #icon><IconifyIcon icon="lucide:search" /></template>查询
      </Button>
    </div>

    <div class="flex-1 flex flex-col gap-4 p-4 min-h-0 overflow-y-auto">

      <Row :gutter="16" class="shrink-0">
        <Col :span="6">
          <Card size="small" class="bg-blue-50 border-blue-100 shadow-sm">
            <Statistic title="计划总产量" :value="overviewData.plannedQty?.total || 0" :value-style="{ fontWeight: 'bold' }" />
            <div class="text-xs text-slate-500 mt-2">
              较昨日 <span :class="overviewData.plannedQty?.value > 0 ? 'text-green-600 font-bold' : 'text-red-600 font-bold'">{{ overviewData.plannedQty?.value > 0 ? '+' : '' }}{{ overviewData.plannedQty?.value || 0 }}%</span>
            </div>
          </Card>
        </Col>
        <Col :span="6">
          <Card size="small" class="bg-indigo-50 border-indigo-100 shadow-sm">
            <Statistic title="实际总产出" :value="overviewData.actualQty?.total || 0" :value-style="{ color: '#4338ca', fontWeight: 'bold' }" />
            <div class="text-xs text-slate-500 mt-2">
              较昨日 <span :class="overviewData.actualQty?.value > 0 ? 'text-green-600 font-bold' : 'text-red-600 font-bold'">{{ overviewData.actualQty?.value > 0 ? '+' : '' }}{{ overviewData.actualQty?.value || 0 }}%</span>
            </div>
          </Card>
        </Col>
        <Col :span="6">
          <Card size="small" class="bg-red-50 border-red-100 shadow-sm">
            <Statistic title="报废总计" :value="overviewData.scrapQty?.total || 0" :value-style="{ color: '#dc2626', fontWeight: 'bold' }" />
            <div class="text-xs text-slate-500 mt-2">
              较昨日 <span :class="overviewData.scrapQty?.value < 0 ? 'text-green-600 font-bold' : 'text-red-600 font-bold'">{{ overviewData.scrapQty?.value > 0 ? '+' : '' }}{{ overviewData.scrapQty?.value || 0 }}%</span>
            </div>
          </Card>
        </Col>
        <Col :span="6">
          <Card size="small" class="bg-orange-50 border-orange-100 shadow-sm">
            <Statistic title="综合一次合格率(FTT)" :value="overviewData.yieldRate?.total || '0%'" :value-style="{ color: '#ea580c', fontWeight: 'bold' }" />
            <div class="text-xs text-slate-500 mt-2">
              较昨日 <span :class="overviewData.yieldRate?.value > 0 ? 'text-green-600 font-bold' : 'text-red-600 font-bold'">{{ overviewData.yieldRate?.value > 0 ? '+' : '' }}{{ overviewData.yieldRate?.value || 0 }}%</span>
            </div>
          </Card>
        </Col>
      </Row>

      <div class="bg-white p-4 rounded-lg shadow-sm border border-slate-200 h-[280px] shrink-0 relative">
        <EchartsUI ref="chartRef" class="w-full h-full" />
      </div>

      <div class="bg-white rounded-lg shadow-sm border border-slate-200 flex-1 min-h-[300px] flex flex-col overflow-hidden">
        <div class="p-3 border-b font-bold text-slate-700 bg-slate-50 flex items-center gap-2">
          <IconifyIcon icon="lucide:list" /> 产出明细记录
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
            <template v-if="column.dataIndex === 'actualQty'">
              <span class="font-bold text-slate-800">{{ record.actualQty }}</span>
              <span class="text-[10px] text-slate-400 ml-1">{{ record.unit }}</span>
            </template>

            <template v-if="column.dataIndex === 'scrapQty'">
              <span :class="record.scrapQty > 0 ? 'text-red-500 font-bold' : ''">{{ record.scrapQty }}</span>
            </template>

            <template v-if="column.dataIndex === 'yieldRate'">
              <Tag :color="record.yieldRate >= 99 ? 'success' : 'error'" class="!m-0 font-bold border-none">
                {{ record.yieldRate }}%
              </Tag>
            </template>
          </template>
        </Table>
      </div>

    </div>
  </Page>
</template>
