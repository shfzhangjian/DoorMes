<script lang="ts" setup>
import { ref, reactive, onMounted, nextTick } from 'vue';
import { Page } from '@vben/common-ui';
import { Card, Button, Table, Tag, Statistic, Select, DatePicker, Row, Col, Progress } from 'ant-design-vue';
import { IconifyIcon } from '@vben/icons';

// 引入 Vben 内置的 Echarts 组件与 hooks
import { EchartsUI, useEcharts } from '@vben/plugins/echarts';
import type { EchartsUIType } from '@vben/plugins/echarts';

// 引入 API 接口
import { getOeeOverview, getOeePage } from '#/api/mes/report/oee';

defineOptions({ name: 'MesOeeAnalysis' });

// --- 状态与数据 ---
const loading = ref(false);
const overviewData = ref<any>({});
const reportList = ref<any[]>([]);
const total = ref(0);

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  dateRange: [] as string[],
  workshopId: undefined,
  equipType: undefined
});

// --- 图表定义 ---
const chartRef = ref<EchartsUIType>();
const { renderEcharts } = useEcharts(chartRef);

// --- 表格列定义 ---
const tableColumns = [
  { title: '设备编号', dataIndex: 'equipCode', width: 120, fixed: 'left' },
  { title: '设备名称', dataIndex: 'equipName', minWidth: 160 },
  { title: '计划运行(H)', dataIndex: 'plannedTime', width: 110, align: 'right' },
  { title: '实际运行(H)', dataIndex: 'actualRunTime', width: 110, align: 'right' },
  { title: '停机耗时(H)', dataIndex: 'downTime', width: 110, align: 'right' },
  { title: '时间开动率(A)', dataIndex: 'availability', width: 120, align: 'right' },
  { title: '性能开动率(P)', dataIndex: 'performance', width: 120, align: 'right' },
  { title: '合格率(Q)', dataIndex: 'quality', width: 100, align: 'right' },
  { title: 'OEE 综合效率', dataIndex: 'oee', width: 120, align: 'right', fixed: 'right' }
];

onMounted(async () => {
  await loadData();
});

// --- 数据加载核心逻辑 ---
async function loadData() {
  loading.value = true;
  try {
    const [overviewRes, pageRes] = await Promise.all([
      getOeeOverview(queryParams),
      getOeePage(queryParams)
    ]);

    overviewData.value = overviewRes;
    reportList.value = pageRes.items;
    total.value = pageRes.total;

    nextTick(() => {
      renderTrendChart(overviewRes.trend);
    });
  } finally {
    loading.value = false;
  }
}

// --- Echarts 渲染逻辑 (OEE 与三大子项趋势图) ---
function renderTrendChart(trendData: any) {
  if (!trendData) return;

  renderEcharts({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    legend: { data: ['时间开动率(A)', '性能开动率(P)', '合格率(Q)', 'OEE'], right: 0 },
    grid: { left: '1%', right: '2%', bottom: '2%', top: '15%', containLabel: true },
    xAxis: { type: 'category', data: trendData.dates },
    yAxis: { type: 'value', name: '百分比(%)', min: 70, max: 100 },
    series: [
      { name: '时间开动率(A)', type: 'bar', barWidth: '15%', itemStyle: { color: '#3b82f6' }, data: trendData.availability },
      { name: '性能开动率(P)', type: 'bar', barWidth: '15%', itemStyle: { color: '#8b5cf6' }, data: trendData.performance },
      { name: '合格率(Q)', type: 'bar', barWidth: '15%', itemStyle: { color: '#10b981' }, data: trendData.quality },
      { name: 'OEE', type: 'line', smooth: true, itemStyle: { color: '#f59e0b' }, symbolSize: 8, lineStyle: { width: 3 }, data: trendData.oee }
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
        <Select v-model:value="queryParams.workshopId" :options="[{label:'抛光材料一车间', value:'W01'}, {label:'配液二车间', value:'W02'}]" allowClear placeholder="全部车间" class="w-40" />
      </div>
      <div class="flex items-center gap-2">
        <span class="text-xs font-bold text-slate-500">设备类型:</span>
        <Select v-model:value="queryParams.equipType" :options="[{label:'精密涂布机', value:'COAT'}, {label:'双面精抛机', value:'POL'}, {label:'高剪切配料釜', value:'MIX'}]" allowClear placeholder="全部类型" class="w-40" />
      </div>
      <Button type="primary" @click="handleQuery" :loading="loading">
        <template #icon><IconifyIcon icon="lucide:search" /></template>查询
      </Button>
    </div>

    <div class="flex-1 flex flex-col gap-4 p-4 min-h-0 overflow-y-auto">

      <Row :gutter="16" class="shrink-0">
        <Col :span="6">
          <Card size="small" class="bg-slate-800 border-slate-700 shadow-sm text-white">
            <Statistic title="全局设备综合效率 (OEE)" :value="overviewData.avgOee || 0" suffix="%" :value-style="{ color: '#f59e0b', fontWeight: 'bold' }" />
            <div class="mt-3">
              <Progress :percent="overviewData.avgOee" :show-info="false" stroke-color="#f59e0b" trail-color="rgba(255,255,255,0.2)" size="small" />
            </div>
            <div class="text-xs text-slate-400 mt-2 flex items-center justify-between">
              <span>A × P × Q 的综合乘积</span>
              <IconifyIcon icon="lucide:activity" class="text-amber-500" />
            </div>
          </Card>
        </Col>
        <Col :span="6">
          <Card size="small" class="bg-blue-50 border-blue-100 shadow-sm">
            <Statistic title="平均时间开动率 (Availability)" :value="overviewData.avgAvailability || 0" suffix="%" :value-style="{ color: '#1d4ed8', fontWeight: 'bold' }" />
            <div class="mt-3">
              <Progress :percent="overviewData.avgAvailability" :show-info="false" stroke-color="#3b82f6" size="small" />
            </div>
            <div class="text-xs text-slate-500 mt-2 flex items-center justify-between">
              <span>实际运行时间 ÷ 计划运行时间</span>
              <IconifyIcon icon="lucide:clock" class="text-blue-500" />
            </div>
          </Card>
        </Col>
        <Col :span="6">
          <Card size="small" class="bg-indigo-50 border-indigo-100 shadow-sm">
            <Statistic title="平均性能开动率 (Performance)" :value="overviewData.avgPerformance || 0" suffix="%" :value-style="{ color: '#4338ca', fontWeight: 'bold' }" />
            <div class="mt-3">
              <Progress :percent="overviewData.avgPerformance" :show-info="false" stroke-color="#8b5cf6" size="small" />
            </div>
            <div class="text-xs text-slate-500 mt-2 flex items-center justify-between">
              <span>实际加工节拍 ÷ 理论设计节拍</span>
              <IconifyIcon icon="lucide:zap" class="text-indigo-500" />
            </div>
          </Card>
        </Col>
        <Col :span="6">
          <Card size="small" class="bg-green-50 border-green-100 shadow-sm">
            <Statistic title="平均合格率 (Quality)" :value="overviewData.avgQuality || 0" suffix="%" :value-style="{ color: '#15803d', fontWeight: 'bold' }" />
            <div class="mt-3">
              <Progress :percent="overviewData.avgQuality" :show-info="false" stroke-color="#10b981" size="small" />
            </div>
            <div class="text-xs text-slate-500 mt-2 flex items-center justify-between">
              <span>合格品数量 ÷ 总加工数量</span>
              <IconifyIcon icon="lucide:check-circle" class="text-green-500" />
            </div>
          </Card>
        </Col>
      </Row>

      <div class="bg-white p-4 rounded-lg shadow-sm border border-slate-200 h-[260px] shrink-0 relative">
        <div class="absolute top-3 left-4 text-sm font-bold text-slate-700 z-10">OEE 与三大子指标趋势</div>
        <EchartsUI ref="chartRef" class="w-full h-full pt-6" />
      </div>

      <div class="bg-white rounded-lg shadow-sm border border-slate-200 flex-1 min-h-[300px] flex flex-col overflow-hidden">
        <div class="p-3 border-b font-bold text-slate-700 bg-slate-50 flex items-center gap-2">
          <IconifyIcon icon="lucide:cpu" /> 单机设备运行效率明细
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
            showTotal: (t) => `共 ${t} 台设备`
          }"
          @change="handleTableChange"
          class="p-2"
        >
          <template #bodyCell="{ column, record }">

            <template v-if="column.dataIndex === 'equipCode'">
              <span class="font-mono font-bold text-slate-700">{{ record.equipCode }}</span>
            </template>

            <template v-if="column.dataIndex === 'downTime'">
              <span :class="record.downTime > 2 ? 'text-red-500 font-bold' : ''">{{ record.downTime }}</span>
            </template>

            <template v-if="column.dataIndex === 'oee'">
              <Tag :color="record.oee >= 85 ? 'success' : (record.oee < 80 ? 'warning' : 'processing')" class="!m-0 font-bold border-none">
                {{ record.oee }}%
              </Tag>
            </template>

            <template v-if="['availability', 'performance', 'quality'].includes(column.dataIndex as string)">
              <span>{{ record[column.dataIndex] }}%</span>
            </template>

          </template>
        </Table>
      </div>

    </div>
  </Page>
</template>
