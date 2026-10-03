<script lang="ts" setup>
import type { EchartsUIType } from '@vben/plugins/echarts';
import { ref, onMounted, watch, nextTick, computed } from 'vue';
import { Page } from '@vben/common-ui';
import { Select, Tag, Drawer, Timeline, Spin, Descriptions, Table, Empty } from 'ant-design-vue';
import { IconifyIcon } from '@vben/icons';
import { EchartsUI, useEcharts } from '@vben/plugins/echarts';
import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { getDashboardBase, getMatrixData, getDrillDownTrace } from '#/api/mes/srm/performance/dashboard/index';

const globalLoading = ref(true);
const matrixLoading = ref(false);

const baseDict = ref<any>({ workload: {}, indicatorList: [], supplierList: [], monthList: [] });

// 💡 核心筛选器 (支持多选)
const activeIndicator = ref('KPI-D-01');
const activeMonths = ref<string[]>([]);
const activeSuppliers = ref<string[]>([]);

const indicatorDetail = ref<any>({});

const trendChartRef = ref<EchartsUIType>();
const { renderEcharts: renderTrend } = useEcharts(trendChartRef);

// 溯源抽屉状态
const traceDrawerVisible = ref(false);
const currentTraceDetail = ref<any>(null);
const traceLoading = ref(false);

onMounted(async () => {
  // 1. 初始化基础字典
  const res: any = await getDashboardBase();
  baseDict.value = res;

  // 2. 设置默认值：默认选中最近的3个月，和 TOP 5 供应商
  activeMonths.value = res.monthList.slice(-3);
  activeSuppliers.value = res.supplierList.slice(0, 5).map((s: any) => s.id);

  globalLoading.value = false;

  // 3. 加载矩阵数据
  loadMatrixData();
});

// 监听筛选器变化，自动刷新大盘
watch([activeIndicator, activeMonths, activeSuppliers], () => {
  if (activeMonths.value.length > 0 && activeSuppliers.value.length > 0) {
    loadMatrixData();
  }
}, { deep: true });

async function loadMatrixData() {
  matrixLoading.value = true;
  try {
    const res: any = await getMatrixData({
      indicatorCode: activeIndicator.value,
      supplierIds: activeSuppliers.value,
      months: activeMonths.value.sort() // 保证时间轴顺序
    });

    indicatorDetail.value = res.indicatorInfo;

    nextTick(() => {
      initTrendChart(activeMonths.value.sort(), res.seriesData, res.indicatorInfo);

      // 动态构建 VxeTable 的列
      const dynamicCols: any[] = [
        { title: '对标供应商', field: 'name', minWidth: 180, fixed: 'left' }
      ];
      activeMonths.value.sort().forEach(m => {
        dynamicCols.push({ title: `${m} 得分`, field: m, width: 100, align: 'center', slots: { default: 'monthScore' } });
      });
      dynamicCols.push({ title: '周期平均分', field: 'avgScore', width: 110, align: 'center', fixed: 'right', slots: { default: 'avgScore' } });

      gridApi.setGridOptions({
        columns: dynamicCols,
        data: res.tableData
      });
    });
  } finally {
    matrixLoading.value = false;
  }
}

// 渲染：多供应商走势对比图
function initTrendChart(months: string[], seriesData: any[], info: any) {
  renderTrend({
    tooltip: { trigger: 'axis', axisPointer: { type: 'cross' } },
    legend: {
      top: 0, // 将图例移到顶部，让底部空间留给 X 轴
      type: 'scroll'
    },
    grid: {
      left: 10,       // 左侧微调，结合 containLabel 防止 Y 轴文字被裁
      right: 15,      // 右侧留少许空间防尾部文字溢出
      bottom: 10,     // 底部给 X 轴文字留足空间即可
      top: 40,        // 顶部留出 40px 给图例 (legend) 和 tooltip 提示线
      containLabel: true // 核心：自动计算坐标轴标签的宽度并包含在网格内
    },
    xAxis: { type: 'category', data: months, boundaryGap: false },
    yAxis: { type: 'value', name: `指标得分 (满分${info.target})`, max: info.target },
    series: seriesData
  });
}

// 注册动态表格
// 注册动态表格
const [Grid, gridApi] = useVbenVxeGrid({
  gridOptions: {
    height: 'auto', // 💡 核心新增：开启高度自动填满父容器
    columns: [],
    data: [],
    showOverflow: true,
    border: true,
    size: 'small',
    pagerConfig: { enabled: false }
  }
});

// 💡 核心钻取：点击表格格子，加载具体底层凭证
async function handleDrillDown(row: any, month: string) {
  traceDrawerVisible.value = true;
  traceLoading.value = true;
  try {
    const res = await getDrillDownTrace({
      indicatorCode: activeIndicator.value,
      supplierId: row.id,
      month: month
    });
    currentTraceDetail.value = res;
  } finally {
    traceLoading.value = false;
  }
}
</script>

<!--<template>-->
<!--  <Page auto-content-height class="bg-[#f4f6f8]">-->
<!--    <div class="h-full flex flex-col p-4 gap-4 overflow-y-auto">-->

<!--      <div class="shrink-0 bg-white rounded-xl shadow-sm border border-slate-200 p-5">-->
<!--        <div class="font-black text-slate-800 text-lg mb-4 flex items-center">-->
<!--          <IconifyIcon icon="lucide:globe" class="mr-2 text-indigo-600" /> SRM 核心业务运营工作量统计-->
<!--        </div>-->
<!--        <Spin :spinning="globalLoading">-->
<!--          <div class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-4">-->
<!--            <div class="bg-blue-50 p-4 rounded-lg flex flex-col items-center justify-center border border-blue-100">-->
<!--              <IconifyIcon icon="lucide:flask-conical" class="text-3xl text-blue-500 mb-1" />-->
<!--              <div class="text-3xl font-black text-slate-800">{{ baseDict.workload?.sampleEvalCount }}</div>-->
<!--              <div class="text-xs text-slate-500 font-bold">样品评价 (批)</div>-->
<!--            </div>-->
<!--            <div class="bg-indigo-50 p-4 rounded-lg flex flex-col items-center justify-center border border-indigo-100">-->
<!--              <IconifyIcon icon="lucide:search-check" class="text-3xl text-indigo-500 mb-1" />-->
<!--              <div class="text-3xl font-black text-slate-800">{{ baseDict.workload?.sourcingCount }}</div>-->
<!--              <div class="text-xs text-slate-500 font-bold">发起寻源任务 (次)</div>-->
<!--            </div>-->
<!--            <div class="bg-cyan-50 p-4 rounded-lg flex flex-col items-center justify-center border border-cyan-100">-->
<!--              <IconifyIcon icon="lucide:clipboard-list" class="text-3xl text-cyan-500 mb-1" />-->
<!--              <div class="text-3xl font-black text-slate-800">{{ baseDict.workload?.requirementCount }}</div>-->
<!--              <div class="text-xs text-slate-500 font-bold">处理采购需求 (项)</div>-->
<!--            </div>-->
<!--            <div class="bg-emerald-50 p-4 rounded-lg flex flex-col items-center justify-center border border-emerald-100">-->
<!--              <IconifyIcon icon="lucide:database" class="text-3xl text-emerald-500 mb-1" />-->
<!--              <div class="text-3xl font-black text-slate-800">{{ baseDict.workload?.supplierInfoCount }}</div>-->
<!--              <div class="text-xs text-slate-500 font-bold">收录供应商档案 (家)</div>-->
<!--            </div>-->
<!--            <div class="bg-green-50 p-4 rounded-lg flex flex-col items-center justify-center border border-green-100">-->
<!--              <IconifyIcon icon="lucide:user-plus" class="text-3xl text-green-500 mb-1" />-->
<!--              <div class="text-3xl font-black text-slate-800">{{ baseDict.workload?.newSupplierCount }}</div>-->
<!--              <div class="text-xs text-slate-500 font-bold">新进准入供应商 (家)</div>-->
<!--            </div>-->
<!--            <div class="bg-red-50 p-4 rounded-lg flex flex-col items-center justify-center border border-red-100">-->
<!--              <IconifyIcon icon="lucide:user-minus" class="text-3xl text-red-500 mb-1" />-->
<!--              <div class="text-3xl font-black text-slate-800">{{ baseDict.workload?.outSupplierCount }}</div>-->
<!--              <div class="text-xs text-slate-500 font-bold">淘汰冻结供应商 (家)</div>-->
<!--            </div>-->
<!--          </div>-->
<!--        </Spin>-->
<!--      </div> min-h-[500px]-->

<!--      <div class="flex-1 bg-white rounded-xl shadow-sm border border-slate-200 p-5 flex flex-col">-->
<!--        <div class="font-black text-slate-800 text-lg mb-4 flex items-center">-->
<!--          <IconifyIcon icon="lucide:layout-template" class="mr-2 text-indigo-600" /> 多维指标横向对比与溯源引擎-->
<!--        </div>-->

<!--        <div class="flex items-start gap-6 bg-slate-50 p-4 rounded-xl border border-slate-200 mb-4 shrink-0">-->
<!--          <div class="flex-1">-->
<!--            <div class="text-xs text-slate-500 font-bold mb-1">1. 选择分析指标 (单选)</div>-->
<!--            <Select v-model:value="activeIndicator" class="w-full font-bold" :options="baseDict.indicatorList.map((i:any) => ({label: `【${i.category}】${i.name}`, value: i.code}))" />-->
<!--          </div>-->
<!--          <div class="flex-1">-->
<!--            <div class="text-xs text-slate-500 font-bold mb-1">2. 选择评价周期 (多选)</div>-->
<!--            <Select v-model:value="activeMonths" mode="multiple" class="w-full" :options="baseDict.monthList.map((m:string) => ({label: m, value: m}))" placeholder="请选择月份" />-->
<!--          </div>-->
<!--          <div class="w-1/2">-->
<!--            <div class="text-xs text-slate-500 font-bold mb-1">3. 选择对标供应商 (多选，默认选TOP5)</div>-->
<!--            <Select v-model:value="activeSuppliers" mode="multiple" class="w-full" :options="baseDict.supplierList.map((s:any) => ({label: s.name, value: s.id}))" placeholder="请选择要加入对比的供应商" :maxTagCount="3" />-->
<!--          </div>-->
<!--        </div>-->

<!--        <div class="flex-1 flex gap-6">-->
<!--          <div class="w-1/2 flex flex-col">-->
<!--            <div class="font-bold text-slate-700 mb-2"><IconifyIcon icon="lucide:trending-up" class="text-indigo-500"/> 多供应商月度横向跑道对比走势</div>-->
<!--            <div class="flex-1 w-full relative border border-slate-100 rounded-lg">-->
<!--              <div v-show="matrixLoading" class="absolute inset-0 z-10 flex items-center justify-center bg-white/60 backdrop-blur-sm"><Spin /></div>-->

<!--              <EchartsUI v-if="activeMonths.length && activeSuppliers.length" ref="trendChartRef" class="w-full h-full" />-->
<!--              <Empty v-else class="mt-20" description="请至少选择一个月份和一个供应商" />-->
<!--            </div>-->
<!--          </div>-->

<!--          <div class="w-1/2 flex flex-col">-->
<!--            <div class="font-bold text-slate-700 mb-2 flex justify-between items-center">-->
<!--              <span><IconifyIcon icon="lucide:table" class="text-indigo-500"/> 动态指标得分矩阵表</span>-->
<!--              <Tag color="blue" class="border-none !m-0">提示：点击月份分数可下钻溯源</Tag>-->
<!--            </div>-->
<!--            <div class="flex-1 relative">-->
<!--              <div v-show="matrixLoading" class="absolute inset-0 z-10 flex items-center justify-center bg-white/60 backdrop-blur-sm"><Spin /></div>-->

<!--              <Grid v-if="activeMonths.length && activeSuppliers.length">-->
<!--                <template #monthScore="{ row, column }">-->
<!--                  <span-->
<!--                    class="font-black text-blue-600 cursor-pointer hover:underline border-b border-blue-200 pb-[1px]"-->
<!--                    @click="handleDrillDown(row, column.field)"-->
<!--                    title="点击追溯当月底层打分凭证"-->
<!--                  >-->
<!--                    {{ row[column.field] }} <IconifyIcon icon="lucide:external-link" class="inline text-[10px]"/>-->
<!--                  </span>-->
<!--                </template>-->
<!--                <template #avgScore="{ row }">-->
<!--                  <span class="font-black text-indigo-700 bg-indigo-50 px-2 py-0.5 rounded">{{ row.avgScore }}</span>-->
<!--                </template>-->
<!--              </Grid>-->
<!--            </div>-->
<!--          </div>-->
<!--        </div>-->
<!--      </div>-->
<!--    </div>-->

<!--    <Drawer v-model:open="traceDrawerVisible" :title="`🔎 [${currentTraceDetail?.month}] - 单项指标评定溯源`" placement="right" :width="650">-->
<!--      <Spin :spinning="traceLoading">-->
<!--        <template v-if="currentTraceDetail">-->
<!--          <div class="bg-gradient-to-r from-blue-50 to-indigo-50 border border-blue-100 p-5 rounded-xl mb-6 relative overflow-hidden">-->
<!--            <IconifyIcon icon="lucide:crosshair" class="absolute -right-4 -bottom-4 text-[100px] text-blue-500 opacity-5" />-->
<!--            <div class="flex items-center gap-2 mb-3 relative z-10">-->
<!--              <Tag color="cyan" class="!m-0 border-none font-bold px-3 text-sm">{{ currentTraceDetail.month }}</Tag>-->
<!--              <span class="text-slate-600 font-bold text-lg">{{ currentTraceDetail.supplierName }}</span>-->
<!--            </div>-->
<!--            <div class="text-3xl font-black text-slate-800 relative z-10">{{ currentTraceDetail.metric }}</div>-->
<!--          </div>-->

<!--          <div class="mb-6">-->
<!--            <div class="font-black text-slate-700 mb-3 flex items-center gap-2">-->
<!--              <IconifyIcon icon="lucide:target" class="text-indigo-500"/> 目标与当月实际得分-->
<!--            </div>-->
<!--            <Descriptions bordered size="small" :column="2">-->
<!--              <Descriptions.Item label="满分/基准"><span class="font-bold text-slate-600">{{ currentTraceDetail.target }} 分</span></Descriptions.Item>-->
<!--              <Descriptions.Item label="当月实绩抓取">-->
<!--                <span class="font-bold" :class="parseFloat(currentTraceDetail.score) >= 18 ? 'text-green-600' : 'text-red-500'">{{ currentTraceDetail.actualRate }}</span>-->
<!--              </Descriptions.Item>-->
<!--              <Descriptions.Item label="系统最终得分" :span="2">-->
<!--                <span class="text-3xl font-black text-blue-600">{{ currentTraceDetail.score }} <span class="text-sm font-normal text-slate-400">分</span></span>-->
<!--              </Descriptions.Item>-->
<!--            </Descriptions>-->
<!--          </div>-->

<!--          <div class="mb-6">-->
<!--            <div class="font-black text-slate-700 mb-3 flex items-center gap-2">-->
<!--              <IconifyIcon icon="lucide:file-search" class="text-indigo-500"/> 底层业务台账判定与凭证-->
<!--            </div>-->
<!--            <div class="bg-slate-50 border border-slate-200 p-4 rounded text-slate-600 text-sm mb-3">-->
<!--              <span class="font-bold text-slate-700">系统判定摘要：</span><br/>-->
<!--              {{ currentTraceDetail.evidenceDesc }}-->
<!--            </div>-->
<!--            <div class="flex flex-col gap-2">-->
<!--              <a v-for="file in currentTraceDetail.files" :key="file.name" class="flex items-center justify-between bg-white border border-slate-200 p-3 rounded hover:border-blue-400 cursor-pointer shadow-sm transition-all">-->
<!--                <span class="flex items-center gap-2 text-blue-600 font-medium"><IconifyIcon icon="lucide:paperclip" /> {{ file.name }}</span>-->
<!--                <span class="text-xs text-slate-400 bg-slate-100 px-2 py-0.5 rounded">{{ file.size }}</span>-->
<!--              </a>-->
<!--            </div>-->
<!--          </div>-->

<!--          <div>-->
<!--            <div class="font-black text-slate-700 mb-3 flex items-center gap-2">-->
<!--              <IconifyIcon icon="lucide:database" class="text-indigo-500"/> MES/WMS/ERP 关联溯源流-->
<!--            </div>-->
<!--            <Timeline class="mt-4 pl-2" v-if="currentTraceDetail.type === 'quality'">-->
<!--              <Timeline.Item :color="currentTraceDetail.score < 18 ? 'red' : 'green'">-->
<!--                <div class="font-bold text-slate-600 text-base">{{ currentTraceDetail.score < 18 ? 'IQC 发生严重退货 (拒收)' : 'IQC 抽样全部合格' }}</div>-->
<!--                <div class="text-sm text-slate-400 mt-1">关联批次流水号：BATCH-{{ currentTraceDetail.month }}-001</div>-->
<!--                <div v-if="currentTraceDetail.score < 18" class="text-xs text-red-500 mt-1 mt-2 cursor-pointer hover:underline"><IconifyIcon icon="lucide:link"/> 穿透至 WMS 不良品处理单 (NCR-9921)</div>-->
<!--              </Timeline.Item>-->
<!--            </Timeline>-->
<!--            <Table v-else size="small" :pagination="false" :columns="[{title:'关联采购单号', dataIndex:'po'}, {title:'系统判定状态', dataIndex:'status'}]" :dataSource="[{po: `PO-2603-${Math.floor(Math.random()*1000)}`, status: currentTraceDetail.score < 18 ? '逾期多次' : '按时入库'}]">-->
<!--              <template #bodyCell="{ column, record }">-->
<!--                <span v-if="column.dataIndex === 'status'" :class="record.status.includes('逾期') ? 'text-red-500 font-bold' : 'text-green-600'">{{ record.status }}</span>-->
<!--              </template>-->
<!--            </Table>-->
<!--          </div>-->
<!--        </template>-->
<!--      </Spin>-->
<!--    </Drawer>-->
<!--  </Page>-->
<!--</template>-->
<template>
  <Page auto-content-height class="bg-[#f4f6f8]">
    <div class="h-full flex flex-col p-4">

      <div class="flex-1 bg-white rounded-xl shadow-sm border border-slate-200 p-5 flex flex-col min-h-0">

        <div class="font-black text-slate-800 text-lg mb-4 flex items-center shrink-0">
          <IconifyIcon icon="lucide:layout-template" class="mr-2 text-indigo-600" /> 多维指标横向对比与溯源引擎
        </div>

        <div class="flex items-start gap-6 bg-slate-50 p-4 rounded-xl border border-slate-200 mb-4 shrink-0">
          <div class="flex-1">
            <div class="text-xs text-slate-500 font-bold mb-1">1. 选择分析指标 (单选)</div>
            <Select v-model:value="activeIndicator" class="w-full font-bold" :options="baseDict.indicatorList.map((i:any) => ({label: `【${i.category}】${i.name}`, value: i.code}))" />
          </div>
          <div class="flex-1">
            <div class="text-xs text-slate-500 font-bold mb-1">2. 选择评价周期 (多选)</div>
            <Select v-model:value="activeMonths" mode="multiple" class="w-full" :options="baseDict.monthList.map((m:string) => ({label: m, value: m}))" placeholder="请选择月份" />
          </div>
          <div class="w-1/2">
            <div class="text-xs text-slate-500 font-bold mb-1">3. 选择对标供应商 (多选，默认选TOP5)</div>
            <Select v-model:value="activeSuppliers" mode="multiple" class="w-full" :options="baseDict.supplierList.map((s:any) => ({label: s.name, value: s.id}))" placeholder="请选择要加入对比的供应商" :maxTagCount="3" />
          </div>
        </div>


        <div class="flex-1 flex gap-6 min-h-0">

          <div class="w-1/2 flex flex-col min-h-0 border border-slate-200 rounded-lg bg-slate-50/50 p-3">
            <div class="font-bold text-slate-700 mb-2 shrink-0 flex items-center h-8">
              <IconifyIcon icon="lucide:trending-up" class="text-indigo-500 mr-2"/> 多供应商月度横向跑道对比走势
            </div>

            <div class="flex-1 relative bg-white rounded-md border border-slate-200 min-h-0 overflow-hidden">
              <div class="absolute inset-0 p-4 box-border flex flex-col">
                <EchartsUI ref="trendChartRef" class="flex-1 w-full h-full min-h-0" />
              </div>

              <div v-if="!(activeMonths.length && activeSuppliers.length)" class="absolute inset-0 z-10 bg-white flex items-center justify-center">
                <Empty description="请至少选择一个月份和一个供应商" />
              </div>

              <div v-show="matrixLoading" class="absolute inset-0 z-20 flex items-center justify-center bg-white/70 backdrop-blur-sm">
                <Spin />
              </div>
            </div>
          </div>

          <div class="w-1/2 flex flex-col min-h-0 border border-slate-200 rounded-lg bg-slate-50/50 p-3">
            <div class="font-bold text-slate-700 mb-2 shrink-0 flex justify-between items-center h-8">
              <span class="flex items-center"><IconifyIcon icon="lucide:table" class="text-indigo-500 mr-2"/> 动态指标得分矩阵表</span>
              <Tag color="blue" class="border-none !m-0">提示：点击月份分数可下钻溯源</Tag>
            </div>

            <div class="flex-1 relative bg-white rounded-md border border-slate-200 min-h-0 overflow-hidden">
              <div class="absolute inset-0">
                <Grid class="h-full w-full">
                  <template #monthScore="{ row, column }">
                    <span
                      class="font-black text-blue-600 cursor-pointer hover:underline border-b border-blue-200 pb-[1px]"
                      @click="handleDrillDown(row, column.field)"
                      title="点击追溯当月底层打分凭证"
                    >
                      {{ row[column.field] }} <IconifyIcon icon="lucide:external-link" class="inline text-[10px]"/>
                    </span>
                  </template>
                  <template #avgScore="{ row }">
                    <span class="font-black text-indigo-700 bg-indigo-50 px-2 py-0.5 rounded">{{ row.avgScore }}</span>
                  </template>
                </Grid>
              </div>

              <div v-if="!(activeMonths.length && activeSuppliers.length)" class="absolute inset-0 z-10 bg-white flex items-center justify-center">
                <Empty description="暂无数据对比" />
              </div>

              <div v-show="matrixLoading" class="absolute inset-0 z-20 flex items-center justify-center bg-white/70 backdrop-blur-sm">
                <Spin />
              </div>
            </div>
          </div>

        </div>

      </div>
    </div>

    <Drawer v-model:open="traceDrawerVisible" :title="`🔎 [${currentTraceDetail?.month}] - 单项指标评定溯源`" placement="right" :width="650">
      <Spin :spinning="traceLoading">
        <template v-if="currentTraceDetail">
          <div class="bg-gradient-to-r from-blue-50 to-indigo-50 border border-blue-100 p-5 rounded-xl mb-6 relative overflow-hidden">
            <IconifyIcon icon="lucide:crosshair" class="absolute -right-4 -bottom-4 text-[100px] text-blue-500 opacity-5" />
            <div class="flex items-center gap-2 mb-3 relative z-10">
              <Tag color="cyan" class="!m-0 border-none font-bold px-3 text-sm">{{ currentTraceDetail.month }}</Tag>
              <span class="text-slate-600 font-bold text-lg">{{ currentTraceDetail.supplierName }}</span>
            </div>
            <div class="text-3xl font-black text-slate-800 relative z-10">{{ currentTraceDetail.metric }}</div>
          </div>

          <div class="mb-6">
            <div class="font-black text-slate-700 mb-3 flex items-center gap-2">
              <IconifyIcon icon="lucide:target" class="text-indigo-500"/> 目标与当月实际得分
            </div>
            <Descriptions bordered size="small" :column="2">
              <Descriptions.Item label="满分/基准"><span class="font-bold text-slate-600">{{ currentTraceDetail.target }} 分</span></Descriptions.Item>
              <Descriptions.Item label="当月实绩抓取">
                <span class="font-bold" :class="parseFloat(currentTraceDetail.score) >= 18 ? 'text-green-600' : 'text-red-500'">{{ currentTraceDetail.actualRate }}</span>
              </Descriptions.Item>
              <Descriptions.Item label="系统最终得分" :span="2">
                <span class="text-3xl font-black text-blue-600">{{ currentTraceDetail.score }} <span class="text-sm font-normal text-slate-400">分</span></span>
              </Descriptions.Item>
            </Descriptions>
          </div>

          <div class="mb-6">
            <div class="font-black text-slate-700 mb-3 flex items-center gap-2">
              <IconifyIcon icon="lucide:file-search" class="text-indigo-500"/> 底层业务台账判定与凭证
            </div>
            <div class="bg-slate-50 border border-slate-200 p-4 rounded text-slate-600 text-sm mb-3">
              <span class="font-bold text-slate-700">系统判定摘要：</span><br/>
              {{ currentTraceDetail.evidenceDesc }}
            </div>
            <div class="flex flex-col gap-2">
              <a v-for="file in currentTraceDetail.files" :key="file.name" class="flex items-center justify-between bg-white border border-slate-200 p-3 rounded hover:border-blue-400 cursor-pointer shadow-sm transition-all">
                <span class="flex items-center gap-2 text-blue-600 font-medium"><IconifyIcon icon="lucide:paperclip" /> {{ file.name }}</span>
                <span class="text-xs text-slate-400 bg-slate-100 px-2 py-0.5 rounded">{{ file.size }}</span>
              </a>
            </div>
          </div>

          <div>
            <div class="font-black text-slate-700 mb-3 flex items-center gap-2">
              <IconifyIcon icon="lucide:database" class="text-indigo-500"/> MES/WMS/ERP 关联溯源流
            </div>
            <Timeline class="mt-4 pl-2" v-if="currentTraceDetail.type === 'quality'">
              <Timeline.Item :color="currentTraceDetail.score < 18 ? 'red' : 'green'">
                <div class="font-bold text-slate-600 text-base">{{ currentTraceDetail.score < 18 ? 'IQC 发生严重退货 (拒收)' : 'IQC 抽样全部合格' }}</div>
                <div class="text-sm text-slate-400 mt-1">关联批次流水号：BATCH-{{ currentTraceDetail.month }}-001</div>
                <div v-if="currentTraceDetail.score < 18" class="text-xs text-red-500 mt-1 mt-2 cursor-pointer hover:underline"><IconifyIcon icon="lucide:link"/> 穿透至 WMS 不良品处理单 (NCR-9921)</div>
              </Timeline.Item>
            </Timeline>
            <Table v-else size="small" :pagination="false" :columns="[{title:'关联采购单号', dataIndex:'po'}, {title:'系统判定状态', dataIndex:'status'}]" :dataSource="[{po: `PO-2603-${Math.floor(Math.random()*1000)}`, status: currentTraceDetail.score < 18 ? '逾期多次' : '按时入库'}]">
              <template #bodyCell="{ column, record }">
                <span v-if="column.dataIndex === 'status'" :class="record.status.includes('逾期') ? 'text-red-500 font-bold' : 'text-green-600'">{{ record.status }}</span>
              </template>
            </Table>
          </div>
        </template>
      </Spin>
    </Drawer>
  </Page>
</template>
