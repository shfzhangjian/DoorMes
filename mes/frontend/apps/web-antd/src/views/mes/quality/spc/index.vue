<script lang="ts" setup>
import { ref, onMounted, nextTick } from 'vue';
import { Page, useVbenModal } from '@vben/common-ui';
import { Segmented, Select, Card, Button, Table, Tag, Statistic, Alert } from 'ant-design-vue';
import { IconifyIcon } from '@vben/icons';
import { getSpcData, type MesSpcApi } from '#/api/mes/quality/spc';
import ExceptionModalVue from '../abnormal/exception/modules/detail-modal.vue';

// 引入 Vben 内置的 Echarts 组件与 hooks
import { EchartsUI, useEcharts } from '@vben/plugins/echarts';
import type { EchartsUIType } from '@vben/plugins/echarts';

defineOptions({ name: 'MesQualitySpc' });

const viewMode = ref<'MONITOR' | 'ANALYSIS'>('MONITOR');
const spcResult = ref<MesSpcApi.SpcAnalysisResult | null>(null);

// 图表 Ref 定义
const xBarChartRef = ref<EchartsUIType>();
const rChartRef = ref<EchartsUIType>();
const histChartRef = ref<EchartsUIType>();

// 提取 render 函数
const { renderEcharts: renderXBar } = useEcharts(xBarChartRef);
const { renderEcharts: renderR } = useEcharts(rChartRef);
const { renderEcharts: renderHist } = useEcharts(histChartRef);

const [ExceptionModal, exceptionModalApi] = useVbenModal({ connectedComponent: ExceptionModalVue });

onMounted(async () => {
  await loadData();
});

async function loadData() {
  spcResult.value = await getSpcData('Coater-01', 'M-PET-005');
  nextTick(() => {
    renderXBarChart();
    renderRChart();
    if (viewMode.value === 'ANALYSIS') {
      renderHistogram();
    }
  });
}

function handleModeChange() {
  nextTick(() => {
    // 切换到分析视图时，由于使用了 v-show，需触发一次渲染以适配宽高
    if (viewMode.value === 'ANALYSIS') {
      renderHistogram();
    }
  });
}

// 绘制 X-bar 均值控制图
function renderXBarChart() {
  if (!spcResult.value) return;

  const data = spcResult.value.data;
  const cfg = spcResult.value.config;
  const xData = data.map(d => d.time);
  const yData = data.map(d => d.xBar);
  const oocPoints = data.map((d, i) => d.isOoc ? { coord: [i, d.xBar], itemStyle: { color: '#ef4444' } } : null).filter(Boolean);

  renderXBar({
    title: { text: 'X-Bar 均值控制图', textStyle: { fontSize: 14 } },
    tooltip: { trigger: 'axis' },
    grid: { left: '40px', right: '80px', top: '40px', bottom: '30px' },
    xAxis: { type: 'category', data: xData, boundaryGap: false },
    yAxis: { type: 'value', min: cfg.lsl - 2, max: cfg.usl + 2 },
    series: [{
      name: '均值 (X-bar)', type: 'line', data: yData,
      itemStyle: { color: '#0ea5e9' },
      markPoint: { data: oocPoints },
      markLine: {
        silent: true, symbol: 'none',
        data: [
          { yAxis: cfg.uclX, name: 'UCL', label: { formatter: 'UCL {c}', position: 'end' }, lineStyle: { color: '#ef4444', type: 'dashed' } },
          { yAxis: cfg.clX, name: 'CL', label: { formatter: 'CL {c}', position: 'end' }, lineStyle: { color: '#10b981', type: 'solid' } },
          { yAxis: cfg.lclX, name: 'LCL', label: { formatter: 'LCL {c}', position: 'end' }, lineStyle: { color: '#ef4444', type: 'dashed' } },
          { yAxis: cfg.usl, name: 'USL', label: { formatter: 'USL {c}', position: 'end' }, lineStyle: { color: '#64748b', type: 'solid', width: 2 } },
          { yAxis: cfg.lsl, name: 'LSL', label: { formatter: 'LSL {c}', position: 'end' }, lineStyle: { color: '#64748b', type: 'solid', width: 2 } }
        ]
      }
    }]
  });
}

// 绘制 R 极差控制图
function renderRChart() {
  if (!spcResult.value) return;

  const data = spcResult.value.data;
  const cfg = spcResult.value.config;

  renderR({
    title: { text: 'R 极差控制图', textStyle: { fontSize: 14 } },
    tooltip: { trigger: 'axis' },
    grid: { left: '40px', right: '80px', top: '40px', bottom: '30px' },
    xAxis: { type: 'category', data: data.map(d => d.time), boundaryGap: false },
    yAxis: { type: 'value' },
    series: [{
      name: '极差 (R)', type: 'line', data: data.map(d => d.rValue),
      itemStyle: { color: '#8b5cf6' },
      markLine: {
        silent: true, symbol: 'none',
        data: [
          { yAxis: cfg.uclR, name: 'UCL', label: { formatter: 'UCL {c}' }, lineStyle: { color: '#ef4444', type: 'dashed' } },
          { yAxis: cfg.clR, name: 'CL', label: { formatter: 'CL {c}' }, lineStyle: { color: '#10b981', type: 'solid' } }
        ]
      }
    }]
  });
}

// 绘制直方图与正态分布曲线
function renderHistogram() {
  if (!spcResult.value) return;

  renderHist({
    title: { text: '测量值频数直方图与分布曲线', textStyle: { fontSize: 14 } },
    tooltip: { trigger: 'axis' },
    grid: { left: '40px', right: '40px', top: '40px', bottom: '30px' },
    xAxis: { type: 'category', data: ['46','47','48','49','50','51','52','53','54'] },
    yAxis: { type: 'value' },
    series: [
      { name: '频数', type: 'bar', data: [2, 5, 12, 25, 40, 20, 10, 4, 1], itemStyle: { color: '#cbd5e1' } },
      { name: '正态拟合', type: 'line', smooth: true, data: [1, 6, 15, 28, 42, 28, 15, 6, 1], itemStyle: { color: '#3b82f6', width: 3 } }
    ]
  });
}

const tableColumns = [
  { title: '子组编号', dataIndex: 'groupId', width: 80, align: 'center' },
  { title: '采集时间', dataIndex: 'time', width: 120 },
  { title: '实测数据 (X1-X5)', dataIndex: 'values', customRender: ({ text }: any) => text.join(', ') },
  { title: '均值 (X-bar)', dataIndex: 'xBar', width: 90, align: 'right' },
  { title: '极差 (R)', dataIndex: 'rValue', width: 90, align: 'right' },
  { title: '状态', dataIndex: 'isOoc', width: 80, align: 'center' }
];

function triggerException() {
  exceptionModalApi.setData({
    isNew: true, exceptionType: 'PROCESS', exceptionLevel: 'MAJOR', isRelateProduct: true,
    discoverDept: '工程部', discoverer: 'SPC 系统自动监控', description: `SPC 控制图报警：机台 Coater-01 生产批次涂布厚度触发【准则1: 单点超出 3σ 控制限】。最新 X-bar 均值为 52.8，控制上限为 52.5。过程能力指数 Cpk 降至 ${spcResult.value?.config.cpk}。`
  }).open();
}
</script>

<template>
  <Page auto-content-height class="bg-[#f4f6f8] relative">
    <ExceptionModal />

    <template #extra>
      <Segmented v-model:value="viewMode" :options="[{label:'车间实时预警大屏', value:'MONITOR'}, {label:'工程分析看板', value:'ANALYSIS'}]" class="font-bold shadow-sm" @change="handleModeChange" />
    </template>

    <div class="bg-white px-4 py-3 border-b flex gap-4 items-center shrink-0 shadow-sm z-10 relative">
      <div class="flex items-center gap-2">
        <span class="text-xs font-bold text-slate-500">监控机台:</span>
        <Select value="Coater-01" :options="[{label:'Coater-01 (一号涂布机)', value:'Coater-01'}]" class="w-48" />
      </div>
      <div class="flex items-center gap-2">
        <span class="text-xs font-bold text-slate-500">检验特征:</span>
        <Select value="thickness" :options="[{label:'涂布厚度 (规格: 50±5um)', value:'thickness'}]" class="w-56" />
      </div>
      <Button type="primary" @click="loadData">查询数据</Button>
    </div>

    <div v-show="viewMode === 'MONITOR'" class="flex-1 flex gap-4 p-4 min-h-0">
      <div class="flex-1 flex flex-col gap-4 min-h-0">
        <div class="bg-white p-4 rounded-lg shadow-sm flex-1 min-h-0 relative border border-slate-200">
          <EchartsUI ref="xBarChartRef" class="w-full h-full" />
        </div>
        <div class="bg-white p-4 rounded-lg shadow-sm flex-1 min-h-0 relative border border-slate-200">
          <EchartsUI ref="rChartRef" class="w-full h-full" />
        </div>
      </div>

      <div class="w-[320px] bg-white rounded-lg shadow-sm border border-slate-200 flex flex-col min-h-0">
        <div class="p-3 bg-slate-800 text-white font-bold rounded-t-lg flex items-center gap-2">
          <IconifyIcon icon="lucide:bell-ring" /> SPC 实时预警播报
        </div>
        <div class="flex-1 overflow-y-auto p-3 flex flex-col gap-3">
          <template v-for="d in spcResult?.data.filter(i => i.isOoc)" :key="d.groupId">
            <Alert type="error" show-icon class="border-red-300 bg-red-50">
              <template #message><span class="font-bold">过程失控报警 (OOC)</span></template>
              <template #description>
                <div class="text-xs mt-1">时间：{{ d.time }}</div>
                <div class="text-xs font-bold text-red-600 mt-1">{{ d.oocRule }}</div>
                <div class="text-xs mt-1">实测均值: {{ d.xBar }} (UCL: {{ spcResult?.config.uclX }})</div>
                <Button type="primary" danger size="small" class="mt-2 w-full" @click="triggerException">
                  立即发起 OCAP 异常单
                </Button>
              </template>
            </Alert>
          </template>
          <div v-if="!spcResult?.data.some(i => i.isOoc)" class="text-center text-slate-400 mt-10">
            <IconifyIcon icon="lucide:shield-check" class="text-4xl mb-2 opacity-30"/>
            <div>当前制程稳定，未触发判异规则</div>
          </div>
        </div>
      </div>
    </div>

    <div v-show="viewMode === 'ANALYSIS'" class="flex-1 flex flex-col gap-4 p-4 min-h-0 overflow-y-auto">
      <div class="grid grid-cols-4 gap-4 shrink-0">
        <Card size="small" class="bg-indigo-50 border-indigo-100 shadow-sm">
          <Statistic title="过程准确度 (Ca)" :value="spcResult?.config.cpk" :precision="2" :value-style="{ color: '#4338ca', fontWeight: 'bold' }" />
        </Card>
        <Card size="small" class="bg-blue-50 border-blue-100 shadow-sm">
          <Statistic title="过程精密度 (Cp)" :value="spcResult?.config.cp" :precision="2" :value-style="{ color: '#1d4ed8', fontWeight: 'bold' }" />
        </Card>
        <Card size="small" class="bg-green-50 border-green-100 shadow-sm">
          <Statistic title="子组数量 (K)" :value="spcResult?.data.length" :value-style="{ color: '#15803d', fontWeight: 'bold' }" />
        </Card>
        <Card size="small" :class="spcResult?.config.cpk && spcResult.config.cpk >= 1.33 ? 'bg-green-100 border-green-300' : 'bg-orange-100 border-orange-300'">
          <div class="text-xs text-slate-500 mb-1">制程能力综合评价</div>
          <div class="font-black text-lg" :class="spcResult?.config.cpk && spcResult.config.cpk >= 1.33 ? 'text-green-700' : 'text-orange-700'">
            {{ spcResult?.config.cpk && spcResult.config.cpk >= 1.33 ? '制程能力充足 (A级)' : '制程能力不足，需改善' }}
          </div>
        </Card>
      </div>

      <div class="flex gap-4 min-h-[350px]">
        <div class="w-1/3 bg-white p-4 rounded-lg shadow-sm border border-slate-200">
          <EchartsUI ref="histChartRef" class="w-full h-full" />
        </div>
        <div class="w-2/3 bg-white rounded-lg shadow-sm border border-slate-200 flex flex-col">
          <div class="p-3 border-b font-bold text-slate-700 bg-slate-50">基础采样数据追溯 (Raw Data)</div>
          <Table :columns="tableColumns" :dataSource="spcResult?.data" size="small" :pagination="{ pageSize: 5 }" class="p-2">
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'isOoc'">
                <Tag :color="record.isOoc ? 'error' : 'success'">{{ record.isOoc ? '失控(OOC)' : '受控' }}</Tag>
              </template>
            </template>
          </Table>
        </div>
      </div>
    </div>
  </Page>
</template>
