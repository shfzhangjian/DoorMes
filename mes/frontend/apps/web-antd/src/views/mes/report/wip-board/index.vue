<script lang="ts" setup>
import { ref, reactive, onMounted, nextTick } from 'vue';
import { Page } from '@vben/common-ui';
import { Card, Button, Table, Tag, Statistic, Select, Input, Row, Col, Popconfirm, message } from 'ant-design-vue';
import { IconifyIcon } from '@vben/icons';

// 引入 Vben 内置的 Echarts 组件与 hooks
import { EchartsUI, useEcharts } from '@vben/plugins/echarts';
import type { EchartsUIType } from '@vben/plugins/echarts';

// 引入 API 接口 (需确保此 API 已按前文定义)
import { getWipPage, getWipOverview } from '#/api/mes/report/wip';

defineOptions({ name: 'MesWipBoard' });

// --- 状态与数据 ---
const loading = ref(false);
const overviewData = ref<any>({});
const reportList = ref<any[]>([]);
const total = ref(0);

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  batchNo: '',
  materialName: '',
  processName: undefined,
  status: undefined
});

// --- 图表定义 ---
const chartRef = ref<EchartsUIType>();
const { renderEcharts } = useEcharts(chartRef);

// --- 表格列定义 (Ant Design Vue 规范) ---
const tableColumns = [
  { title: '流转批号', dataIndex: 'batchNo', minWidth: 160, fixed: 'left' },
  { title: '物料品名', dataIndex: 'materialName', minWidth: 160 },
  { title: '当前工序', dataIndex: 'processName', width: 120, align: 'center' },
  { title: '机台资源', dataIndex: 'equipCode', width: 120, align: 'center' },
  { title: '状态', dataIndex: 'status', width: 100, align: 'center' },
  { title: '当前数量', dataIndex: 'qty', width: 100, align: 'right' },
  { title: '滞留时长(H)', dataIndex: 'queueTime', width: 110, align: 'right' },
  { title: '操作', dataIndex: 'actions', width: 120, align: 'center', fixed: 'right' }
];

onMounted(async () => {
  await loadData();
});

// --- 数据加载核心逻辑 ---
async function loadData() {
  loading.value = true;
  try {
    const [overviewRes, pageRes] = await Promise.all([
      getWipOverview(), // 实际业务中可传入过滤条件
      getWipPage(queryParams)
    ]);

    overviewData.value = overviewRes.overview;
    reportList.value = pageRes.items;
    total.value = pageRes.total;

    // 渲染图表
    nextTick(() => {
      renderDistributionChart(overviewRes.distribution);
    });
  } finally {
    loading.value = false;
  }
}

// --- Echarts 渲染逻辑 ---
function renderDistributionChart(data: any[]) {
  if (!data || data.length === 0) return;

  renderEcharts({
    title: { text: '实时在制品 (WIP) 工序分布', textStyle: { fontSize: 14, fontWeight: 'normal' }, left: '0' },
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    legend: { data: ['等待加工 (WAIT)', '正在加工 (RUN)'], right: 0 },
    grid: { left: '1%', right: '2%', bottom: '2%', containLabel: true },
    xAxis: { type: 'category', data: data.map(d => d.processName) },
    yAxis: { type: 'value', name: '批次数量 (单位)' },
    series: [
      { name: '正在加工 (RUN)', type: 'bar', stack: 'total', barWidth: 40, itemStyle: { color: '#10b981' }, data: data.map(d => d.runQty) },
      { name: '等待加工 (WAIT)', type: 'bar', stack: 'total', barWidth: 40, itemStyle: { color: '#f59e0b' }, data: data.map(d => d.waitQty) }
    ]
  });
}

// --- 事件处理 ---
function handleQuery() {
  queryParams.pageNum = 1;
  loadData();
}

function resetQuery() {
  queryParams.batchNo = '';
  queryParams.materialName = '';
  queryParams.processName = undefined;
  queryParams.status = undefined;
  handleQuery();
}

function handleTableChange(pagination: any) {
  queryParams.pageNum = pagination.current;
  queryParams.pageSize = pagination.pageSize;
  loadData();
}

// 模拟解冻操作
function handleUnhold(record: any) {
  message.success(`批次 ${record.batchNo} 已成功解除冻结！`);
  loadData();
}

// 状态标签颜色映射
function getStatusColor(status: string) {
  const map: Record<string, string> = { 'RUN': 'success', 'WAIT': 'processing', 'HOLD': 'error' };
  return map[status] || 'default';
}
</script>

<template>
  <Page auto-content-height class="bg-[#f4f6f8] relative">

    <div class="bg-white px-4 py-3 border-b flex gap-4 items-center shrink-0 shadow-sm z-10 relative flex-wrap">
      <div class="flex items-center gap-2">
        <span class="text-xs font-bold text-slate-500">流转批号:</span>
        <Input v-model:value="queryParams.batchNo" placeholder="请输入批号" class="w-40" allow-clear />
      </div>
      <div class="flex items-center gap-2">
        <span class="text-xs font-bold text-slate-500">物料品名:</span>
        <Input v-model:value="queryParams.materialName" placeholder="产品名称" class="w-40" allow-clear />
      </div>
      <div class="flex items-center gap-2">
        <span class="text-xs font-bold text-slate-500">当前工序:</span>
        <Select v-model:value="queryParams.processName" placeholder="全部工序" class="w-32" allow-clear :options="[{label:'原料配制', value:'OP10'}, {label:'基材涂布', value:'OP20'}, {label:'表面精抛', value:'OP30'}]" />
      </div>
      <div class="flex items-center gap-2">
        <span class="text-xs font-bold text-slate-500">批次状态:</span>
        <Select v-model:value="queryParams.status" placeholder="全部状态" class="w-32" allow-clear :options="[{label:'加工中(RUN)', value:'RUN'}, {label:'排队中(WAIT)', value:'WAIT'}, {label:'冻结(HOLD)', value:'HOLD'}]" />
      </div>
      <Button type="primary" @click="handleQuery" :loading="loading">查询</Button>
      <Button @click="resetQuery">重置</Button>
    </div>

    <div class="flex-1 flex flex-col gap-4 p-4 min-h-0 overflow-y-auto">

      <Row :gutter="16" class="shrink-0">
        <Col :span="6">
          <Card size="small" class="bg-blue-50 border-blue-100 shadow-sm">
            <Statistic title="车间总 WIP" :value="overviewData.totalQty || 0" :value-style="{ color: '#1d4ed8', fontWeight: 'bold' }" />
            <div class="text-xs text-slate-500 mt-2 flex items-center gap-1">
              <IconifyIcon icon="lucide:layers" class="text-blue-500" /> 当前存量总计
            </div>
          </Card>
        </Col>
        <Col :span="6">
          <Card size="small" class="bg-orange-50 border-orange-100 shadow-sm">
            <Statistic title="积压瓶颈工序" :value="overviewData.bottleneckQty || 0" :value-style="{ color: '#ea580c', fontWeight: 'bold' }">
              <template #prefix>
                <span class="text-lg mr-2">{{ overviewData.bottleneckProcess || '-' }}</span>
              </template>
            </Statistic>
            <div class="text-xs text-slate-500 mt-2 flex items-center gap-1">
              <IconifyIcon icon="lucide:alert-triangle" class="text-orange-500" /> 识别到最大积压点
            </div>
          </Card>
        </Col>
        <Col :span="6">
          <Card size="small" class="bg-red-50 border-red-100 shadow-sm">
            <Statistic title="异常冻结 (HOLD)" :value="overviewData.holdLotCount || 0" suffix="批" :value-style="{ color: '#dc2626', fontWeight: 'bold' }" />
            <div class="text-xs text-slate-500 mt-2 flex items-center gap-1">
              <IconifyIcon icon="lucide:lock" class="text-red-500" /> 待品质MRB判定单据
            </div>
          </Card>
        </Col>
        <Col :span="6">
          <Card size="small" class="bg-green-50 border-green-100 shadow-sm">
            <Statistic title="平均排队滞留" :value="overviewData.avgQueueTime || 0" suffix="H" :value-style="{ color: '#15803d', fontWeight: 'bold' }" />
            <div class="text-xs text-slate-500 mt-2 flex items-center gap-1">
              <IconifyIcon icon="lucide:clock" class="text-green-500" /> 批次工序间平均周转
            </div>
          </Card>
        </Col>
      </Row>

      <div class="bg-white p-4 rounded-lg shadow-sm border border-slate-200 h-[240px] shrink-0 relative">
        <EchartsUI ref="chartRef" class="w-full h-full" />
      </div>

      <div class="bg-white rounded-lg shadow-sm border border-slate-200 flex-1 min-h-[300px] flex flex-col overflow-hidden">
        <div class="p-3 border-b font-bold text-slate-700 bg-slate-50 flex items-center gap-2">
          <IconifyIcon icon="lucide:list" /> 批次流转实时明细
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
            showTotal: (t) => `共 ${t} 个流转批次`
          }"
          @change="handleTableChange"
          class="p-2"
        >
          <template #bodyCell="{ column, record }">

            <template v-if="column.dataIndex === 'batchNo'">
              <a class="font-mono font-bold text-indigo-700 hover:underline">{{ record.batchNo }}</a>
            </template>

            <template v-if="column.dataIndex === 'qty'">
              <span class="font-bold text-slate-800">{{ record.qty }}</span>
              <span class="text-[10px] text-slate-400 ml-1">{{ record.unit }}</span>
            </template>

            <template v-if="column.dataIndex === 'status'">
              <Tag :color="getStatusColor(record.status)" class="!m-0 font-bold border-none">
                {{ record.status }}
              </Tag>
            </template>

            <template v-if="column.dataIndex === 'queueTime'">
              <span :class="{'text-red-500 font-bold': record.queueTime > 12}">{{ record.queueTime }}</span>
            </template>

            <template v-if="column.dataIndex === 'actions'">
              <div class="flex gap-2 justify-center">
                <Button type="link" size="small" class="!px-0">追溯</Button>
                <Popconfirm
                  title="确认要解除该批次的品质冻结吗？"
                  @confirm="handleUnhold(record)"
                  placement="topLeft"
                >
                  <Button type="link" size="small" class="!px-0 text-red-600" :disabled="record.status !== 'HOLD'">解冻</Button>
                </Popconfirm>
              </div>
            </template>

          </template>
        </Table>
      </div>

    </div>
  </Page>
</template>
