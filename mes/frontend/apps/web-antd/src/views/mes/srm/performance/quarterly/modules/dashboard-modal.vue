<script lang="ts" setup>
import type { EchartsUIType } from '@vben/plugins/echarts';

import { ref, computed, watch } from 'vue';
import { useVbenModal } from '@vben/common-ui';
import { Steps, Input, InputNumber, Radio, Tag, message, Drawer, Descriptions, Timeline } from 'ant-design-vue';
import { IconifyIcon } from '@vben/icons';
import { EchartsUI, useEcharts } from '@vben/plugins/echarts';
import { useVbenVxeGrid } from '#/adapter/vxe-table';

const emit = defineEmits(['success']);
const reportData = ref<any>(null);
const approvalForm = ref({ result: 'PASS', opinion: '', finalScore: 0 });

const chartRef = ref<EchartsUIType>();
const { renderEcharts } = useEcharts(chartRef);

// ======= 单项指标月度数据钻取控制 =======
const metricDrawerVisible = ref(false);
const currentMetricTrace = ref<any>({});

const finalGradeObj = computed(() => {
  const s = approvalForm.value.finalScore || 0;
  if (s >= 90) return { grade: 'A', color: 'success' };
  if (s >= 80) return { grade: 'B', color: 'processing' };
  if (s >= 70) return { grade: 'C', color: 'warning' };
  return { grade: 'D', color: 'error' };
});

const [Modal, modalApi] = useVbenModal({
  title: '📈 供应商季度品质与绩效综合看板',
  class: 'w-[1250px]',
  onOpenChange: async (isOpen) => {
    if (isOpen) {
      const { record, isReadOnly } = modalApi.getData();
      reportData.value = record;
      approvalForm.value = { result: 'PASS', opinion: '', finalScore: record.totalScore };
      modalApi.setState({ showConfirmButton: !isReadOnly, confirmText: '提交核准并归档' });

      setTimeout(() => { renderTrendChart(); }, 300);
    }
  },
  onConfirm: async () => {
    if (approvalForm.value.result === 'REJECT' && !approvalForm.value.opinion) {
      return message.warning('驳回必须填写审批意见！');
    }
    message.success(`季度评定已核准！最终季度得分为：${approvalForm.value.finalScore}分，评级为：${finalGradeObj.value.grade}级。`);
    emit('success');
    modalApi.close();
  }
});

function renderTrendChart() {
  if (!reportData.value || !reportData.value.months) return;
  const rd = reportData.value;
  const series: any[] = rd.indicatorTrends.map((ind: any) => ({
    name: ind.metric, type: 'line', smooth: true, symbolSize: 6, data: [ind.m1, ind.m2, ind.m3]
  }));
  series.push({
    name: '🌟 月度综合总分', type: 'bar', yAxisIndex: 1, barWidth: '25%',
    itemStyle: { borderRadius: [4, 4, 0, 0], color: 'rgba(99, 102, 241, 0.2)', borderColor: '#6366f1', borderWidth: 1 },
    data: rd.totalScores
  });
  renderEcharts({
    tooltip: { trigger: 'axis', axisPointer: { type: 'cross' } },
    legend: { type: 'scroll', bottom: 0, data: [...rd.indicatorTrends.map((i: any) => i.metric), '🌟 月度综合总分'] },
    grid: { left: '3%', right: '3%', bottom: '15%', top: '15%', containLabel: true },
    xAxis: { type: 'category', boundaryGap: true, data: rd.months },
    yAxis: [
      { type: 'value', name: '单项得分', max: 20, splitLine: { lineStyle: { type: 'dashed' } } },
      { type: 'value', name: '总分(100)', min: 60, max: 100, position: 'right', splitLine: { show: false } }
    ],
    series
  });
}

const tableColumns = computed(() => {
  if (!reportData.value) return [];
  const cols: any[] = [
    { title: '考核指标维度', field: 'metric', minWidth: 160, slots: { default: 'metric' } },
    { title: '满分标准', field: 'target', width: 80, align: 'center' }
  ];
  (reportData.value.months || []).forEach((m: string, idx: number) => {
    cols.push({ title: `${m}得分`, field: `m${idx+1}`, width: 90, align: 'center', slots: { default: 'monthScore' } });
  });
  cols.push({ title: '季度本项分', field: 'qScore', width: 110, align: 'center', slots: { default: 'qScore' } });
  return cols;
});

const tableData = computed(() => {
  if (!reportData.value || !reportData.value.indicatorTrends) return [];
  const rd = reportData.value;
  const rows = rd.indicatorTrends.map((ind: any) => ({
    metric: ind.metric, target: ind.target, m1: ind.m1.toFixed(1), m2: ind.m2.toFixed(1), m3: ind.m3.toFixed(1),
    qScore: ((ind.m1 + ind.m2 + ind.m3) / 3).toFixed(1)
  }));
  if (rd.totalScores && rd.totalScores.length === 3) {
    rows.push({
      metric: '⭐ 综合绩效总得分', target: '100分',
      m1: rd.totalScores[0].toFixed(1), m2: rd.totalScores[1].toFixed(1), m3: rd.totalScores[2].toFixed(1),
      qScore: rd.totalScore
    });
  }
  return rows;
});

const [Grid, gridApi] = useVbenVxeGrid({
  gridOptions: { columns: [], data: [], showOverflow: true, border: true, size: 'small', pagerConfig: { enabled: false } }
});

watch(() => tableData.value, (newData) => {
  if (newData.length > 0) gridApi.setGridOptions({ columns: tableColumns.value, data: newData });
}, { deep: true });

// 💡 核心：具体指标的月度数据钻取方法
function openMonthlyTrace(row: any, field: string) {
  const monthIdx = parseInt(field.replace('m', '')) - 1;
  const monthName = reportData.value.months[monthIdx];

  // 模拟推导实际达成率 (实际开发中这部分由后端接口原样返回)
  const score = parseFloat(row[field]);
  const isQuality = row.metric.includes('合格');
  const evalDept = isQuality ? '品质部' : (row.metric.includes('交期') ? 'PMC计划部' : '采购部');

  // 组装钻取详情的数据源
  currentMetricTrace.value = {
    supplierName: reportData.value.supplierName,
    month: monthName,
    metric: row.metric,
    target: row.target,
    score: row[field],
    // 模拟达成率
    actualRate: score >= 20 ? '100%' : (score >= 18 ? '95.2%' : '82.5%'),
    evalDept: evalDept,
    type: isQuality ? 'quality' : 'delivery',
    evidenceDesc: isQuality ? 'WMS质检台账系统自动抓取，存在1批次退货。' : 'ERP交货明细统计，逾期2次。',
    files: [
      { name: `${monthName}_${row.metric.substring(0,4)}明细台账.xlsx`, size: '128KB' },
      { name: score < 18 ? '客诉异常8D改善报告.pdf' : null, size: '2.1MB' }
    ].filter(f => f.name !== null)
  };

  metricDrawerVisible.value = true;
}
</script>

<template>
  <Modal>
    <div v-if="reportData" class="bg-[#f4f6f8] min-h-[70vh] p-6 -mx-6 -mt-4 flex flex-col gap-6">

      <div class="bg-white rounded-xl shadow-sm border border-slate-200 p-6 flex justify-between items-center relative overflow-hidden shrink-0">
        <div class="z-10">
          <div class="text-sm text-slate-500 mb-2 flex items-center gap-2">
            <Tag color="cyan" class="border-none">{{ reportData.evalYear }}年 Q{{ reportData.evalQuarter }}</Tag>
            季度综合评定报告 | 单号：{{ reportData.id }}
          </div>
          <h2 class="text-3xl font-black text-slate-800 m-0">{{ reportData.supplierName }}</h2>
        </div>
        <div class="text-right flex gap-8 items-center z-10">
          <div>
            <div class="text-xs text-slate-400 font-bold uppercase tracking-widest">System Avg Score</div>
            <div class="text-4xl font-black text-slate-400">{{ reportData.totalScore }} <span class="text-lg">分</span></div>
          </div>
          <div class="h-12 w-px bg-slate-200"></div>
          <div>
            <div class="text-xs text-slate-400 font-bold uppercase tracking-widest">Final Approved Score</div>
            <div class="text-4xl font-black text-indigo-600">{{ approvalForm.finalScore }} <span class="text-lg">分</span></div>
          </div>
        </div>
        <IconifyIcon icon="lucide:award" class="absolute -right-10 -top-10 text-[180px] text-slate-50 opacity-80 z-0" />
      </div>

      <div class="grid grid-cols-1 xl:grid-cols-2 gap-4 shrink-0">
        <div class="bg-white rounded-xl shadow-sm border border-slate-200 p-5 flex flex-col">
          <div class="font-bold text-slate-700 mb-4 flex items-center">
            <IconifyIcon icon="lucide:trending-up" class="mr-2 text-indigo-500"/> 各单项指标与月度总分趋势图
          </div>
          <div class="w-full h-[320px]">
            <EchartsUI ref="chartRef" class="w-full h-full" />
          </div>
        </div>

        <div class="bg-white rounded-xl shadow-sm border border-slate-200 p-5 flex flex-col">
          <div class="font-bold text-slate-700 mb-4 flex items-center justify-between">
            <div><IconifyIcon icon="lucide:table" class="mr-2 text-indigo-500"/> 月度考核分数明细表</div>
            <div class="text-xs font-normal text-slate-400 bg-slate-100 px-2 py-1 rounded">提示：点击带下划线分数可钻取打分证据链</div>
          </div>
          <div class="h-[320px]">
            <Grid>
              <template #metric="{ row }">
                <span :class="row.metric.includes('总得分') ? 'font-black text-indigo-700' : 'font-bold text-slate-600'">{{ row.metric }}</span>
              </template>

              <template #monthScore="{ row, column }">
                <span v-if="row.metric.includes('总得分')" class="font-bold text-slate-400">
                  {{ row[column.field] }}
                </span>
                <span
                  v-else
                  class="font-black text-blue-600 cursor-pointer hover:underline border-b border-blue-200 pb-0.5"
                  @click="openMonthlyTrace(row, column.field)"
                  title="点击钻取该指标当月的底层评分明细与附件凭证"
                >
                  {{ row[column.field] }} <IconifyIcon icon="lucide:external-link" class="inline text-[10px]"/>
                </span>
              </template>

              <template #qScore="{ row }">
                <span class="font-black text-indigo-600 bg-indigo-50 px-2 py-0.5 rounded">{{ row.qScore }}</span>
              </template>
            </Grid>
          </div>
        </div>
      </div>

      <div class="bg-indigo-50 rounded-xl shadow-sm border border-indigo-100 p-6 shrink-0">
        <div class="font-black text-indigo-800 mb-6 flex items-center gap-2 text-lg">
          <IconifyIcon icon="lucide:pen-tool"/> 报告核准与季度定级
        </div>
        <div class="grid grid-cols-1 md:grid-cols-3 gap-8">
          <div class="col-span-1 border-r border-indigo-200 pr-4">
            <Steps direction="vertical" :current="1" size="small">
              <Steps.Step title="采购经理发起" description="系统汇总推送" status="finish" />
              <Steps.Step title="品质/技术会签" description="当前节点" status="process" />
              <Steps.Step title="采购总监最终核准" description="等待流转" status="wait" />
            </Steps>
          </div>
          <div class="col-span-2 flex flex-col gap-4">
            <div class="flex items-center gap-4 bg-white p-3 rounded-lg border border-indigo-100 shadow-sm">
              <div class="font-bold text-slate-700">最终季度核定得分：</div>
              <InputNumber v-model:value="approvalForm.finalScore" :min="0" :max="100" :precision="1" class="w-32 text-center font-bold text-xl text-indigo-700 bg-indigo-50 border-indigo-200" />
              <div class="text-xs text-slate-400 -ml-2">(默认取三个月平均分，主管可微调)</div>
              <div class="font-bold text-slate-700 ml-4 border-l pl-4">最终定级：</div>
              <Tag :color="finalGradeObj.color" class="font-black text-lg px-3 py-1">{{ finalGradeObj.grade }} 级</Tag>
            </div>
            <div class="font-bold text-slate-700 mt-2">办理结论：</div>
            <Radio.Group v-model:value="approvalForm.result" button-style="solid" class="w-full flex">
              <Radio.Button value="PASS" class="flex-1 text-center bg-green-50 border-green-200 !text-green-700 font-bold">同意核定成绩，签字流转</Radio.Button>
              <Radio.Button value="REJECT" class="flex-1 text-center bg-red-50 border-red-200 !text-red-700 font-bold">成绩有异议，驳回重批</Radio.Button>
            </Radio.Group>
            <div class="font-bold text-slate-700">审批意见/评语：</div>
            <Input.TextArea v-model:value="approvalForm.opinion" :rows="2" placeholder="填写最终评审评语..." />
          </div>
        </div>
      </div>
    </div>

    <Drawer v-model:open="metricDrawerVisible" title="🔎 考核指标评定溯源" placement="right" :width="650">

      <div class="bg-gradient-to-r from-blue-50 to-indigo-50 border border-blue-100 p-5 rounded-xl mb-6 relative overflow-hidden">
        <IconifyIcon icon="lucide:crosshair" class="absolute -right-4 -bottom-4 text-[100px] text-blue-500 opacity-5" />
        <div class="flex items-center gap-2 mb-4 relative z-10">
          <Tag color="cyan" class="!m-0 border-none font-bold">{{ currentMetricTrace.month }}</Tag>
          <span class="text-slate-600 font-bold">{{ currentMetricTrace.supplierName }}</span>
        </div>
        <div class="text-2xl font-black text-slate-800 relative z-10">{{ currentMetricTrace.metric }}</div>
      </div>

      <div class="mb-6">
        <div class="font-black text-slate-700 mb-3 flex items-center gap-2">
          <IconifyIcon icon="lucide:target" class="text-indigo-500"/> 目标与得分判定
        </div>
        <Descriptions bordered size="small" :column="2">
          <Descriptions.Item label="满分/目标要求"><span class="font-bold text-slate-600">{{ currentMetricTrace.target }}</span></Descriptions.Item>
          <Descriptions.Item label="实际达成率 (系统抓取)">
            <span class="font-bold" :class="parseFloat(currentMetricTrace.score) >= 20 ? 'text-green-600' : 'text-orange-500'">{{ currentMetricTrace.actualRate }}</span>
          </Descriptions.Item>
          <Descriptions.Item label="系统建议/人工评分" :span="2">
            <span class="text-2xl font-black text-blue-600">{{ currentMetricTrace.score }} 分</span>
          </Descriptions.Item>
          <Descriptions.Item label="考核/复核部门" :span="2">
            <Tag color="processing" class="border-none">{{ currentMetricTrace.evalDept }}</Tag>
          </Descriptions.Item>
        </Descriptions>
      </div>

      <div class="mb-6">
        <div class="font-black text-slate-700 mb-3 flex items-center gap-2">
          <IconifyIcon icon="lucide:file-search" class="text-indigo-500"/> 评分依据与举证附件
        </div>
        <div class="bg-slate-50 border border-slate-200 p-4 rounded text-slate-600 text-sm mb-3">
          {{ currentMetricTrace.evidenceDesc }}
        </div>
        <div class="flex flex-col gap-2">
          <a v-for="file in currentMetricTrace.files" :key="file.name" class="flex items-center justify-between bg-white border border-slate-200 p-2 rounded hover:border-blue-400 hover:shadow-sm cursor-pointer transition-all">
            <span class="flex items-center gap-2 text-blue-600 font-medium">
              <IconifyIcon icon="lucide:file-text" /> {{ file.name }}
            </span>
            <span class="text-xs text-slate-400">{{ file.size }}</span>
          </a>
        </div>
      </div>

      <div>
        <div class="font-black text-slate-700 mb-3 flex items-center gap-2">
          <IconifyIcon icon="lucide:database" class="text-indigo-500"/> 关联系统底层单据
        </div>

        <Timeline v-if="currentMetricTrace.type === 'quality'" class="mt-4 pl-2">
          <Timeline.Item color="red">
            <div class="font-bold text-slate-600">2026-02-18 <Tag color="error" class="ml-2 border-none">IQC 拒收</Tag></div>
            <div class="text-xs text-slate-400 mt-1">关联异常单：NCR-260218-001 | 缺陷描述：尺寸超差</div>
          </Timeline.Item>
          <Timeline.Item color="green">
            <div class="font-bold text-slate-600">2026-02-15 <Tag color="success" class="ml-2 border-none">IQC 合格</Tag></div>
            <div class="text-xs text-slate-400 mt-1">关联入库单：IN-260215-089</div>
          </Timeline.Item>
        </Timeline>

        <Table
          v-else
          size="small" :pagination="false"
          :columns="[{title:'采购订单', dataIndex:'po'}, {title:'要求交期', dataIndex:'d1'}, {title:'实际入库', dataIndex:'d2'}, {title:'状态', dataIndex:'status'}]"
          :dataSource="[
            {po: 'PO-2602-085', d1: '02-25', d2: '02-28', status: '逾期3天'},
            {po: 'PO-2602-032', d1: '02-15', d2: '02-15', status: '按时交货'}
          ]"
        >
          <template #bodyCell="{ column, record }">
            <span v-if="column.dataIndex === 'status'" :class="record.status.includes('逾期') ? 'text-red-500 font-bold' : 'text-green-600'">{{ record.status }}</span>
          </template>
        </Table>
      </div>

    </Drawer>

  </Modal>
</template>
