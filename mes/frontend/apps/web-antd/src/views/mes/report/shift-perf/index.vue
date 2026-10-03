<script lang="ts" setup>
import { ref, reactive, onMounted, nextTick } from 'vue';
import { Page } from '@vben/common-ui';
import { Card, Button, Table, Tag, Statistic, Select, DatePicker, Row, Col } from 'ant-design-vue';
import { IconifyIcon } from '@vben/icons';

// 引入 Vben 内置的 Echarts 组件与 hooks
import { EchartsUI, useEcharts } from '@vben/plugins/echarts';
import type { EchartsUIType } from '@vben/plugins/echarts';

// 引入 API 接口
import { getShiftPerfPage, getShiftPerfOverview } from '#/api/mes/report/shift-perf';

defineOptions({ name: 'MesShiftPerf' });

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
  shiftId: undefined,
  teamId: undefined
});

// --- 图表定义 ---
const chartRef = ref<EchartsUIType>();
const { renderEcharts } = useEcharts(chartRef);

// --- 表格列定义 ---
const tableColumns = [
  { title: '生产日期', dataIndex: 'productionDate', width: 110, align: 'center', fixed: 'left' },
  { title: '班别', dataIndex: 'shiftName', width: 90, align: 'center' },
  { title: '班组', dataIndex: 'teamName', width: 100, align: 'center' },
  { title: '班组长', dataIndex: 'teamLeader', width: 100, align: 'center' },
  { title: '目标产量', dataIndex: 'targetQty', width: 100, align: 'right' },
  { title: '实际产量', dataIndex: 'actualQty', width: 100, align: 'right' },
  { title: '合格数量', dataIndex: 'goodQty', width: 100, align: 'right' },
  { title: '不良数量', dataIndex: 'defectQty', width: 100, align: 'right' },
  { title: '达成率(%)', dataIndex: 'achieveRate', width: 110, align: 'right' },
  { title: '合格率(%)', dataIndex: 'yieldRate', width: 110, align: 'right' }
];

onMounted(async () => {
  await loadData();
});

// --- 数据加载核心逻辑 ---
async function loadData() {
  loading.value = true;
  try {
    const [overviewRes, pageRes] = await Promise.all([
      getShiftPerfOverview(queryParams),
      getShiftPerfPage(queryParams)
    ]);

    overviewData.value = overviewRes;
    reportList.value = pageRes.items;
    total.value = pageRes.total;

    nextTick(() => {
      renderCompareChart(overviewRes.trend);
    });
  } finally {
    loading.value = false;
  }
}

// --- Echarts 渲染逻辑 (班组横向对比图) ---
function renderCompareChart(trendData: any) {
  if (!trendData) return;

  renderEcharts({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    legend: { data: ['实际产出', '目标达成率'], right: 0 },
    grid: { left: '1%', right: '2%', bottom: '2%', top: '15%', containLabel: true },
    xAxis: { type: 'category', data: trendData.teams, axisTick: { alignWithLabel: true } },
    yAxis: [
      { type: 'value', name: '总产量 (PCS)', position: 'left' },
      { type: 'value', name: '达成率(%)', position: 'right', axisLabel: { formatter: '{value}' } }
    ],
    series: [
      { name: '实际产出', type: 'bar', barWidth: '40%', itemStyle: { color: '#3b82f6' }, data: trendData.actualOutput },
      { name: '目标达成率', type: 'line', yAxisIndex: 1, smooth: true, itemStyle: { color: '#10b981' }, symbolSize: 8, data: trendData.achieveRates }
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
        <Select v-model:value="queryParams.workshopId" :options="[{label:'抛光材料一车间', value:'W01'}]" class="w-40" />
      </div>
      <div class="flex items-center gap-2">
        <span class="text-xs font-bold text-slate-500">班别:</span>
        <Select v-model:value="queryParams.shiftId" :options="[{label:'全部', value:''}, {label:'早班', value:'S1'}, {label:'晚班', value:'S2'}]" allowClear placeholder="全部班别" class="w-28" />
      </div>
      <div class="flex items-center gap-2">
        <span class="text-xs font-bold text-slate-500">班组:</span>
        <Select v-model:value="queryParams.teamId" :options="[{label:'A班组', value:'T1'}, {label:'B班组', value:'T2'}, {label:'C班组', value:'T3'}]" allowClear placeholder="全部班组" class="w-28" />
      </div>
      <Button type="primary" @click="handleQuery" :loading="loading">
        <template #icon><IconifyIcon icon="lucide:search" /></template>查询
      </Button>
    </div>

    <div class="flex-1 flex flex-col gap-4 p-4 min-h-0 overflow-y-auto">

      <Row :gutter="16" class="shrink-0">
        <Col :span="6">
          <Card size="small" class="bg-indigo-50 border-indigo-100 shadow-sm">
            <Statistic title="最佳产出班组" :value="overviewData.topTeam || '-'" :value-style="{ color: '#4338ca', fontWeight: 'bold' }" />
            <div class="text-xs text-slate-500 mt-2 flex items-center gap-1">
              <IconifyIcon icon="lucide:trophy" class="text-indigo-500" /> 综合产出排名第一
            </div>
          </Card>
        </Col>
        <Col :span="6">
          <Card size="small" class="bg-blue-50 border-blue-100 shadow-sm">
            <Statistic title="平均计划达成率" :value="overviewData.avgAchieveRate || 0" suffix="%" :value-style="{ fontWeight: 'bold' }" />
            <div class="text-xs text-slate-500 mt-2 flex items-center gap-1">
              <IconifyIcon icon="lucide:target" class="text-blue-500" /> 全局目标完成评估
            </div>
          </Card>
        </Col>
        <Col :span="6">
          <Card size="small" class="bg-green-50 border-green-100 shadow-sm">
            <Statistic title="全局平均合格率" :value="overviewData.avgYieldRate || 0" suffix="%" :value-style="{ color: '#15803d', fontWeight: 'bold' }" />
            <div class="text-xs text-slate-500 mt-2 flex items-center gap-1">
              <IconifyIcon icon="lucide:check-circle" class="text-green-500" /> 良率指标监控
            </div>
          </Card>
        </Col>
        <Col :span="6">
          <Card size="small" class="bg-orange-50 border-orange-100 shadow-sm">
            <Statistic title="累计不良总数" :value="overviewData.totalDefectQty || 0" :value-style="{ color: '#c2410c', fontWeight: 'bold' }" />
            <div class="text-xs text-slate-500 mt-2 flex items-center gap-1">
              <IconifyIcon icon="lucide:alert-circle" class="text-orange-500" /> 报废与不良汇总
            </div>
          </Card>
        </Col>
      </Row>

      <div class="bg-white p-4 rounded-lg shadow-sm border border-slate-200 h-[280px] shrink-0 relative">
        <div class="absolute top-3 left-4 text-sm font-bold text-slate-700 z-10">班组产出与达成率横向对比</div>
        <EchartsUI ref="chartRef" class="w-full h-full pt-4" />
      </div>

      <div class="bg-white rounded-lg shadow-sm border border-slate-200 flex-1 min-h-[300px] flex flex-col overflow-hidden">
        <div class="p-3 border-b font-bold text-slate-700 bg-slate-50 flex items-center gap-2">
          <IconifyIcon icon="lucide:users" /> 班组绩效明细记录
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

            <template v-if="column.dataIndex === 'teamName'">
              <span class="font-bold text-slate-700">{{ record.teamName }}</span>
            </template>

            <template v-if="column.dataIndex === 'achieveRate'">
              <Tag :color="record.achieveRate >= 100 ? 'success' : 'error'" class="!m-0 font-bold border-none">
                {{ record.achieveRate }}%
              </Tag>
            </template>

            <template v-if="column.dataIndex === 'yieldRate'">
              <span :class="record.yieldRate >= 99 ? 'text-green-600 font-bold' : 'text-orange-500 font-bold'">
                {{ record.yieldRate }}%
              </span>
            </template>

            <template v-if="column.dataIndex === 'defectQty'">
              <span :class="record.defectQty > 50 ? 'text-red-500 font-bold' : ''">{{ record.defectQty }}</span>
            </template>

          </template>
        </Table>
      </div>

    </div>
  </Page>
</template>
