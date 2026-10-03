<script lang="ts" setup>
import { ref, watch, computed, reactive, onBeforeUnmount } from 'vue';
import {
  Button, Card, Tag, Input, InputNumber, Table, Tabs, Dropdown, Menu,
  message, Modal, Select, Divider, Form, DatePicker, Row, Col, Statistic, Radio, Descriptions
} from 'ant-design-vue';
import type { TableColumnsType } from 'ant-design-vue';
import { IconifyIcon } from '@vben/icons';
import dayjs from 'dayjs';

// 🌟 引入分离的大报告组件
import ComprehensiveReport from './components/ComprehensiveReport.vue';

const props = defineProps(['operator', 'order', 'process']);
const emit = defineEmits(['close', 'abnormal', 'handover', 'pause']);
const clone = (data: any) => JSON.parse(JSON.stringify(data));

// ==================================================================================
// 核心状态机定义
// ==================================================================================
const workOrderStatus = ref(props.order?.status === '完成作业' ? '完成作业' : '未开工');
const activeTabPre = ref('1');
const activeTabQc = ref('首检(FAI)');
const qcTabsList = ['首检(FAI)', '过程抽检(IPQC)', '完工检(FQC)'];

const parentBatchNo = ref('');

function jumpToStep(step: string) {
  if (workOrderStatus.value === '未开工') return message.warning('请先完成扫码领料并开机！');
  workOrderStatus.value = step;
}

const qTimeState = reactive({ active: false, remaining: 1800, timer: null as any, isExpired: false });
function startQTime() {
  if (qTimeState.active) return;
  qTimeState.active = true; qTimeState.remaining = 1800;
  qTimeState.timer = setInterval(() => {
    if (qTimeState.remaining > 0) qTimeState.remaining--;
    else {
      clearInterval(qTimeState.timer); qTimeState.isExpired = true;
      Modal.error({ title: '🚨 Q-Time 超期拦截', content: '物料超时风险，已自动锁机！请呼叫 IPQC 解锁。' });
    }
  }, 1000);
}

// 历史快照
const historyDrawerVisible = ref(false);
const historyList = ref<any[]>([]);

function recordHistory(action: string, detail: string, snapshot: any = null) {
  historyList.value.unshift({ time: new Date().toLocaleString(), status: workOrderStatus.value, action, user: props.operator?.userName || '系统', detail, snapshot });
}

// ==================================================================================
// 1. 未开工 (领用批量单品扫描 - 🌟标准大屏表格化)
// ==================================================================================
const batchScanCode = ref('');
const scannedMaterials = ref<any[]>([]);
const startJobModalVisible = ref(false);
const startJobForm = reactive({ time: dayjs().format('YYYY-MM-DD HH:mm:ss') });

const materialColumns: TableColumnsType = [
  { title: '单品批次/周转垛条码', dataIndex: 'batch', width: 250 },
  { title: '物料/产品名称', dataIndex: 'product', minWidth: 200 },
  { title: '来源工序', dataIndex: 'source', width: 150 },
  { title: '预估加工数量 (PCS)', dataIndex: 'qty', width: 180, align: 'center' },
  { title: '扫码载入时间', dataIndex: 'time', width: 180 },
  { title: '操作', dataIndex: 'action', width: 100, align: 'center' }
];

const totalIncomingQty = computed(() => scannedMaterials.value.reduce((sum, item) => sum + item.qty, 0));

function handleScanBatch() {
  if (!batchScanCode.value.trim()) return message.warning('请扫描待裁圆周转垛/批次条码！');

  parentBatchNo.value = 'TAPE2-' + batchScanCode.value.slice(-4);
  scannedMaterials.value.unshift({
    id: Date.now(),
    code: batchScanCode.value,
    batch: parentBatchNo.value,
    product: 'T01_Film_背胶单片',
    qty: 500, // 假设这一垛有 500 片
    source: '背胶站(周转区)',
    time: new Date().toLocaleTimeString()
  });

  message.success(`待裁切批次 ${parentBatchNo.value} (500 PCS) 扫描就绪`);
  batchScanCode.value = '';
}

function removeMaterial(index: number) {
  scannedMaterials.value.splice(index, 1);
  if(scannedMaterials.value.length === 0) parentBatchNo.value = '';
}

function openStartJobModal() {
  if (scannedMaterials.value.length === 0) return message.warning('🚨 请先扫描领用前序待加工的单品批次！');
  startJobForm.time = dayjs().format('YYYY-MM-DD HH:mm:ss');
  startJobModalVisible.value = true;
}

function confirmStartJob() {
  if(!startJobForm.time) return message.warning('请填写开工时间');
  startJobModalVisible.value = false;
  setTimeout(() => {
    workOrderStatus.value = '作业前准备';
    activeTabPre.value = '1';
    recordHistory('单片裁圆开机', `接收批次: ${parentBatchNo.value}, 投入总量: ${totalIncomingQty.value} PCS`, { type: 'start', time: startJobForm.time, batch: parentBatchNo.value });
    message.success('开机成功！进入准备阶段。');
    startQTime();
  }, 200);
}

// ==================================================================================
// 2. 作业前准备 (大表格化产线核对)
// ==================================================================================
const checkData = ref([
  { item: '核对投入物料批次与数量', req: '与工单及流转卡一致', result: '未确认', remark: '' },
  { item: '裁切模具/刀片状态', req: '刀片锋利无缺口，安装紧固', result: '未确认', remark: '' },
  { item: '设备急停与安全光栅', req: '功能触发有效', result: '未确认', remark: '' }
]);
const checkColumns: TableColumnsType = [
  { title: '检查项目', dataIndex: 'item', width: '30%' }, { title: '标准要求', dataIndex: 'req', width: '30%' },
  { title: '确认结果', dataIndex: 'result', width: '20%', align: 'center' }, { title: '备注说明', dataIndex: 'remark', width: '20%' }
];

const isPreCheckComplete = computed(() => checkData.value.every(d => d.result === '正常'));

function finishPreCheck() {
  recordHistory('作业准备完成', '物料及裁切刀具确认无误', { type: 'pre', check: clone(checkData.value) });
  workOrderStatus.value = '作业中';
}

// ==================================================================================
// 3. 作业中 (大屏参数记录：线速/刀深)
// ==================================================================================
const runParamForm = ref({ speed: null as number|null, depth: null as number|null });
const runParamLogs = ref<any[]>([]);

const runParamColumns: TableColumnsType = [
  { title: '记录时间', dataIndex: 'time', width: 200, align: 'center' },
  { title: '运行线速 (pcs/min)', dataIndex: 'speed', align: 'center' },
  { title: '裁切刀深 (mm)', dataIndex: 'depth', align: 'center' },
  { title: '操作人', dataIndex: 'user', width: 150, align: 'center' }
];

function handleAddRunParam() {
  if (!runParamForm.value.speed || !runParamForm.value.depth) return message.warning('线速与刀深不能为空');

  runParamLogs.value.unshift({
    id: Date.now(),
    time: new Date().toLocaleTimeString(),
    speed: runParamForm.value.speed,
    depth: runParamForm.value.depth,
    user: props.operator?.userName || '系统'
  });

  recordHistory('工艺参数记录', `线速: ${runParamForm.value.speed}, 刀深: ${runParamForm.value.depth}`);
  message.success('工艺参数监控记录已追加！');
  runParamForm.value.speed = null;
  runParamForm.value.depth = null;
}

// ==================================================================================
// 4. 品质检验 (动态列 + 单选判定)
// ==================================================================================
const inspectForm = reactive({ mode: 'piece', segmentPos: '', length: null as number|null, barcode: '' });
const pendingInspectItems = ref<{ id: string, detail: string }[]>([]);

const pendingInspectColumns: TableColumnsType = [
  { title: '待检单品明细', dataIndex: 'detail' },
  { title: '操作', dataIndex: 'action', width: 100, align: 'center' }
];

function addPendingItem() {
  if (!inspectForm.barcode) return message.warning('请扫入单件条码！');
  const detail = `裁圆单品序列号: ${inspectForm.barcode}`;
  pendingInspectItems.value.unshift({ id: `TSK-${Date.now()}`, detail });
  inspectForm.barcode = '';
  message.success('已加入待检队列！');
}

const wbVisible = ref(false);
const wbTask = reactive({ type: '', targetStr: '', targets: [] as string[], standard: null as string | null, summaryResult: 'A级', summaryRemark: '', items: [] as any[] });
const standardOptions = [{ value: 'STD-CUT-01', label: '裁圆成品出厂检验标准 (V1.0)' }];

const getDynamicColumns = (targets: string[]): TableColumnsType => {
  const baseColumns: TableColumnsType = [
    { title: '序号', dataIndex: 'id', width: 60, align: 'center', fixed: 'left' },
    { title: '检测项目', dataIndex: 'inspectionItem', width: 140, fixed: 'left' },
    { title: '标准要求', dataIndex: 'standardDesc', width: 150 },
    { title: '抽样数', dataIndex: 'sampleSize', width: 90, align: 'center' },
  ];
  const dynamicColumns: TableColumnsType = targets.map((target, idx) => ({ title: `实测值-${idx + 1}`, dataIndex: `val_${idx}`, minWidth: 140, align: 'center' }));
  const endColumns: TableColumnsType = [
    { title: 'MAX', dataIndex: 'operatorMax', width: 80, align: 'center' }, { title: 'MIN', dataIndex: 'operatorMin', width: 80, align: 'center' },
    { title: 'AVG', dataIndex: 'operatorAvg', width: 80, align: 'center' }, { title: '单项结论', dataIndex: 'operatorResult', width: 100, align: 'center', fixed: 'right' }
  ];
  return [...baseColumns, ...dynamicColumns, ...endColumns];
};

function loadStandardItems() {
  const templateItems = [
    { id: 1, inspectionItem: '裁圆直径公差', itemType: 'QUANTITATIVE', standardDesc: '±0.1 mm', sampleSize: 3, minValueLimit: -0.1, maxValueLimit: 0.1, operatorValues: [], operatorMax: null, operatorMin: null, operatorAvg: null, operatorResult: '-' },
    { id: 2, inspectionItem: '边缘毛刺', itemType: 'QUALITATIVE', standardDesc: '无毛刺/不沾边', sampleSize: 2, operatorValues: [], operatorResult: '-' },
    { id: 3, inspectionItem: '离型膜完整性', itemType: 'QUALITATIVE', standardDesc: '无划伤/破损', sampleSize: 1, operatorValues: [], operatorResult: '-' }
  ];
  wbTask.items = clone(templateItems);
  wbTask.items.forEach((it: any) => { it.operatorValues = new Array(wbTask.targets.length).fill(null); });
}

function handleValueChange(row: any) {
  const validValues = row.operatorValues.filter((v: any) => v !== undefined && v !== null && v !== '');
  if (validValues.length === 0) { row.operatorMax = null; row.operatorMin = null; row.operatorAvg = null; row.operatorResult = '-'; return; }
  if (row.itemType === 'QUANTITATIVE') {
    const numVals = validValues.map(Number).filter((n:any) => !isNaN(n));
    if(numVals.length > 0) {
      row.operatorMax = Math.max(...numVals).toFixed(3); row.operatorMin = Math.min(...numVals).toFixed(3); row.operatorAvg = (numVals.reduce((a:number, b:number) => a + b, 0) / numVals.length).toFixed(3);
      row.operatorResult = (row.operatorMax <= row.maxValueLimit && row.operatorMin >= row.minValueLimit) ? 'OK' : 'NG';
    } else { row.operatorResult = '-'; }
  } else { row.operatorResult = (validValues.includes('NG') || validValues.includes('异常')) ? 'NG' : 'OK'; }
}

function openQualityWorkbench() {
  if (pendingInspectItems.value.length === 0) return message.warning('当前待检队列为空！');
  wbTask.type = activeTabQc.value; wbTask.targets = pendingInspectItems.value.map(i => i.detail); wbTask.targetStr = wbTask.targets.join(' ; ');
  wbTask.standard = null; wbTask.summaryResult = 'A级'; wbTask.summaryRemark = ''; wbTask.items = []; wbVisible.value = true;
}

const qcHistory = ref<any[]>([]);
const qcHistoryColumns: TableColumnsType = [
  { title: '报告批次', dataIndex: 'id', width: 140 }, { title: '检测类型', dataIndex: 'type', width: 140 },
  { title: '提交时间', dataIndex: 'submitTime', width: 120, align: 'center' }, { title: '包含样本', key: 'sampleCount', width: 100, align: 'center' },
  { title: '综合结论', dataIndex: 'judgment', width: 100, align: 'center' }, { title: '操作', dataIndex: 'action', width: 120, align: 'center', fixed: 'right' }
];

function submitWorkbench() {
  const isComplete = wbTask.items.every(i => i.operatorResult !== '-');
  if (!isComplete) return message.warning('请完成表格中所有检测指标录入！');
  qcHistory.value.unshift({
    id: `RPT-${Date.now().toString().slice(-6)}`, target: wbTask.targetStr, targets: clone(wbTask.targets), type: wbTask.type,
    submitTime: new Date(Date.now() - 300000).toLocaleTimeString(), completeTime: new Date().toLocaleTimeString(),
    judgment: wbTask.summaryResult, ngCount: wbTask.items.filter(i => i.operatorResult === 'NG').length, standard: wbTask.standard, remark: wbTask.summaryRemark, items: clone(wbTask.items)
  });
  pendingInspectItems.value = []; wbVisible.value = false; activeTabQc.value = '检验历史台账';
  message.success('质检单生成并存入台账！');
}

const detailVisible = ref(false); const detailRecord = ref<any>(null);
function openDetail(record: any) { detailRecord.value = record; detailVisible.value = true; }

// ==================================================================================
// 5. 完成作业 (键盘输入不良数，自动倒算合格数)
// ==================================================================================
const finishForm = reactive({ remark: '', printLabel: true });
const inboundSummary = reactive({ total: 0, good: 0, scrap: 0, yieldRate: '0%' });
const keypadInput = ref('');

const computedBadQty = computed(() => parseInt(keypadInput.value) || 0);
const computedGoodQty = computed(() => totalIncomingQty.value - computedBadQty.value);

function pressKey(key: string) { if (key === 'C') keypadInput.value = ''; else if (key === 'DEL') keypadInput.value = keypadInput.value.slice(0, -1); else keypadInput.value += key; }

const finishModalVisible = ref(false);

function handleFinishClick() {
  finishModalVisible.value = true;
}

function submitFinish() {
  inboundSummary.total = totalIncomingQty.value;
  inboundSummary.scrap = computedBadQty.value;
  inboundSummary.good = computedGoodQty.value;
  inboundSummary.yieldRate = inboundSummary.total > 0 ? ((inboundSummary.good / inboundSummary.total) * 100).toFixed(1) + '%' : '0%';

  const detailStr = `裁切完工汇总: 合格 ${inboundSummary.good} PCS, 不良 ${inboundSummary.scrap} PCS`;
  recordHistory('完工报工入库', detailStr, { type: 'finish', data: clone(inboundSummary) });

  if (finishForm.printLabel) {
    Modal.success({
      title: '入库标签打印 (Zebra USB)',
      content: `裁切批次入库单已自动发送。\n批次号: ${parentBatchNo.value}-CUT\n数量: ${inboundSummary.good} PCS\n流向: 成品周转库`,
      onOk: () => { workOrderStatus.value = '完成作业'; finishModalVisible.value = false; setTimeout(() => emit('close'), 500); }
    });
  } else {
    workOrderStatus.value = '完成作业';
    finishModalVisible.value = false;
    message.success('完工报工成功！');
    setTimeout(() => emit('close'), 500);
  }
}

// ==================================================================================
// 6. 综合大报告 eDHR
// ==================================================================================
const eDhrVisible = ref(false);
const eDhrData = computed(() => ({
  order: props.order, process: props.process, operator: props.operator, startTime: startJobForm.time,
  summary: inboundSummary, materials: scannedMaterials.value, checks: checkData.value,
  qc: qcHistory.value.map(q => ({ time: q.completeTime, type: q.type, mode: '系统/人工', detail: q.target, result: q.judgment }))
}));

const abnormalModalVisible = ref(false); const abnormalForm = reactive({ reason: null, desc: '' });
function handleMachineMenuClick({ key }: { key: string }) {
  if (key === 'pause') { Modal.confirm({ title: '暂停工单', content: '确认挂起当前工单？', onOk() { emit('pause'); } }); }
  else if (key === 'abnormal') { abnormalModalVisible.value = true; }
  else if (key === 'handover') { emit('handover'); }
}
function submitAbnormalStop() { workOrderStatus.value = '异常停工'; abnormalModalVisible.value = false; }
</script>

<template>
  <div class="flex flex-col absolute inset-0 bg-slate-100 overflow-hidden">

    <header class="flex h-16 shrink-0 items-center justify-between border-b bg-white px-6 shadow-sm z-10">
      <div class="flex items-center gap-8">
        <div v-for="label in ['未开工', '作业前准备', '作业中', '品质检验', '完成作业']" :key="label"
             @click="jumpToStep(label)"
             :class="['flex items-center gap-2 font-black text-lg transition-colors cursor-pointer hover:text-indigo-500',
             workOrderStatus === label ? 'text-indigo-700 scale-105' : 'text-slate-400']">
          <IconifyIcon :icon="workOrderStatus === label ? 'lucide:circle-dot' : 'lucide:circle'" class="text-xl" />
          <span>{{ label }}</span>
        </div>
      </div>
      <div class="flex items-center gap-4">
        <span v-if="parentBatchNo" class="text-lg font-mono font-black text-indigo-700 bg-indigo-50 px-4 py-1.5 rounded-lg border border-indigo-200">当前裁切批次: {{ parentBatchNo }}</span>
      </div>
    </header>

    <main class="flex-1 flex flex-col min-h-0 p-4 overflow-hidden relative">

      <div v-show="workOrderStatus === '未开工'" class="flex-1 flex flex-col h-full bg-white rounded-3xl shadow-sm border border-slate-200 p-6 overflow-hidden">
        <div class="shrink-0 mb-6 flex items-center gap-6">
          <div class="flex-1 max-w-3xl">
            <div class="text-xl font-black text-slate-800 mb-2 flex items-center gap-2"><IconifyIcon icon="lucide:scan-barcode" /> 扫码领用 (前序背胶单品)</div>
            <Input.Search v-model:value="batchScanCode" size="large" placeholder="扫入待裁圆周转垛/批次条码..." class="h-14 text-xl font-mono custom-huge-input-start" @search="handleScanBatch" enter-button="确认载入" />
          </div>
        </div>
        <div class="flex-1-table-container border border-slate-200 rounded-xl">
          <Table :columns="materialColumns" :dataSource="scannedMaterials" :pagination="false" :scroll="{ y: '100%' }" class="full-height-table text-lg">
            <template #empty><div class="py-16 flex flex-col items-center text-slate-400"><IconifyIcon icon="lucide:layers" class="text-[60px] mb-4 opacity-50" /><span class="text-xl font-bold">暂无待加工批次数据</span></div></template>
            <template #bodyCell="{ column, record, index }">
              <template v-if="column.dataIndex === 'qty'"><span class="text-emerald-600 font-bold">{{ record.qty }} PCS</span></template>
              <template v-if="column.dataIndex === 'action'"><Button type="text" danger class="bg-red-50 hover:bg-red-100" @click="removeMaterial(index)"><IconifyIcon icon="lucide:trash-2" class="text-xl" /></Button></template>
            </template>
          </Table>
        </div>
      </div>

      <div v-show="workOrderStatus === '作业前准备'" class="flex-1 flex flex-col bg-white border-2 border-slate-200 rounded-2xl shadow-md overflow-hidden">
        <div class="p-4 bg-slate-50 border-b border-slate-200 text-xl font-black text-slate-700 flex items-center gap-2 shrink-0">
          <IconifyIcon icon="lucide:check-square" class="text-indigo-600" /> 裁圆设备开机前确认项
        </div>
        <div class="flex-1-table-container p-4">
          <Table :columns="checkColumns" :dataSource="checkData" :pagination="false" :scroll="{ y: '100%' }" class="full-height-table text-lg border border-slate-200 rounded-xl">
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'result'">
                <Select v-model:value="record.result" size="large" class="w-40 font-bold text-center">
                  <Select.Option value="未确认">未确认</Select.Option><Select.Option value="正常">正常</Select.Option><Select.Option value="异常">异常</Select.Option>
                </Select>
              </template>
              <template v-if="column.dataIndex === 'remark'"><Input v-model:value="record.remark" size="large" /></template>
            </template>
          </Table>
        </div>
      </div>

      <div v-show="workOrderStatus === '作业中'" class="flex-1 flex flex-col gap-4 min-h-0">
        <Row :gutter="16" class="shrink-0">
          <Col :span="8">
            <Card class="bg-blue-50 border-blue-200 shadow-sm rounded-2xl p-2 text-center">
              <div class="text-blue-600 font-bold text-lg mb-2">当前批次加工目标 (PCS)</div>
              <div class="text-5xl font-black text-blue-700 font-mono">{{ totalIncomingQty }}</div>
            </Card>
          </Col>
          <Col :span="8">
            <Card class="bg-indigo-50 border-indigo-200 shadow-sm rounded-2xl p-2 text-center">
              <div class="text-indigo-600 font-bold text-lg mb-2">运行线速参考 (pcs/min)</div>
              <div class="text-4xl font-black text-indigo-700">25 - 35</div>
            </Card>
          </Col>
          <Col :span="8">
            <Card class="bg-purple-50 border-purple-200 shadow-sm rounded-2xl p-2 text-center">
              <div class="text-purple-600 font-bold text-lg mb-2">裁切刀深基准 (mm)</div>
              <div class="text-4xl font-black text-purple-700">1.50 ± 0.05</div>
            </Card>
          </Col>
        </Row>

        <div class="flex-1 bg-white border border-slate-200 rounded-2xl overflow-hidden shadow-sm flex flex-col min-h-0 relative">
          <div class="bg-slate-50 p-5 border-b border-slate-200 flex items-end gap-6 shrink-0 shadow-sm">
            <Form.Item label="当前线速 (pcs/min)" class="mb-0 font-bold text-slate-800 text-lg">
              <InputNumber v-model:value="runParamForm.speed" size="large" class="w-48 h-12 text-2xl font-black shadow-sm" />
            </Form.Item>
            <Form.Item label="当前刀深 (mm)" class="mb-0 font-bold text-slate-800 text-lg flex-1 max-w-[300px]">
              <InputNumber v-model:value="runParamForm.depth" size="large" class="w-full h-12 text-2xl font-black shadow-sm" :step="0.01" />
            </Form.Item>
            <Button type="primary" size="large" class="bg-indigo-600 font-black px-10 shadow-md h-12 text-xl mb-[1px]" @click="handleAddRunParam">
              <IconifyIcon icon="lucide:activity" class="mr-2" /> 记录过程工艺参数
            </Button>
          </div>

          <div class="flex-1-table-container">
            <Table :columns="runParamColumns" :dataSource="runParamLogs" :pagination="false" :scroll="{ y: '100%' }" class="full-height-table text-lg">
              <template #empty><div class="py-16 text-slate-400 font-bold text-xl text-center">请定时录入裁切线速与刀深参数监控记录。</div></template>
              <template #bodyCell="{ column, record }">
                <template v-if="column.dataIndex === 'speed'"><span class="font-bold text-indigo-600">{{ record.speed }}</span></template>
                <template v-if="column.dataIndex === 'depth'"><span class="font-bold text-purple-600">{{ record.depth }}</span></template>
              </template>
            </Table>
          </div>
        </div>
      </div>

      <div v-show="workOrderStatus === '品质检验'" class="flex-1 flex flex-col bg-white border-2 border-slate-200 rounded-2xl overflow-hidden shadow-md">
        <Tabs v-model:activeKey="activeTabQc" tabPosition="left" class="w-full h-full custom-left-tabs">
          <Tabs.TabPane v-for="qcType in qcTabsList" :key="qcType" :tab="qcType">
            <div class="flex h-full bg-slate-50">
              <div class="w-[450px] p-6 flex flex-col border-r border-slate-200 bg-white shrink-0">
                <div class="text-xl font-black text-slate-800 mb-6 border-b pb-4 text-indigo-600">录入 {{qcType}} 抽检单品</div>
                <Form layout="vertical">
                  <Form.Item label="检测模式">
                    <Radio.Group v-model:value="inspectForm.mode" button-style="solid" class="w-full flex" size="large">
                      <Radio.Button value="piece" class="flex-1 text-center font-bold">按单件(扫单品码)</Radio.Button>
                    </Radio.Group>
                  </Form.Item>
                  <Form.Item label="扫描单品条码">
                    <Input v-model:value="inspectForm.barcode" size="large" class="h-12 text-lg font-bold font-mono" placeholder="如: CUT-12345-P001" />
                  </Form.Item>
                </Form>
                <Button type="primary" size="large" class="w-full h-14 text-xl font-bold mt-2" @click="addPendingItem">加入待检队列 👉</Button>
              </div>

              <div class="flex-1 p-6 flex flex-col min-h-0 relative">
                <div class="text-xl font-black text-indigo-900 mb-4">待送检单品清单 (共 {{pendingInspectItems.length}} 件)</div>
                <div class="flex-1-table-container border border-slate-200 rounded-xl bg-white shadow-sm overflow-hidden">
                  <Table :columns="pendingInspectColumns" :dataSource="pendingInspectItems" :pagination="false" :scroll="{ y: '100%' }" class="full-height-table text-base absolute inset-0">
                    <template #empty><div class="py-16 text-slate-400 font-bold text-lg text-center">尚未扫描录入</div></template>
                    <template #bodyCell="{ column, index }">
                      <template v-if="column.dataIndex === 'action'"><Button type="text" danger @click="pendingInspectItems.splice(index,1)"><IconifyIcon icon="lucide:trash-2"/></Button></template>
                    </template>
                  </Table>
                </div>
              </div>
            </div>
          </Tabs.TabPane>

          <Tabs.TabPane key="检验历史台账" tab="检验历史台账">
            <div class="p-6 h-full flex flex-col bg-slate-50 relative">
              <div class="text-2xl font-black text-slate-800 mb-4 px-2 flex items-center shrink-0"><IconifyIcon icon="lucide:book-open-check" class="mr-2 text-indigo-600"/> 品质检测总台账</div>
              <div class="flex-1-table-container border border-slate-200 rounded-xl bg-white shadow-inner overflow-hidden">
                <Table :columns="qcHistoryColumns" :dataSource="qcHistory" :pagination="false" :scroll="{y: '100%'}" class="full-height-table text-lg absolute inset-0">
                  <template #bodyCell="{ column, record }">
                    <template v-if="column.key === 'sampleCount'"><Tag color="processing" class="font-bold">{{record.targets?.length || 0}} 列</Tag></template>
                    <template v-if="column.dataIndex === 'judgment'"><Tag :color="record.judgment === '不合格' ? 'error' : 'success'" class="text-base font-bold py-1 px-4">{{record.judgment}}</Tag></template>
                    <template v-if="column.dataIndex === 'action'"><Button type="link" size="large" class="font-bold text-blue-600" @click="openDetail(record)">查看动态详表</Button></template>
                  </template>
                </Table>
              </div>
            </div>
          </Tabs.TabPane>
        </Tabs>
      </div>

      <div v-show="workOrderStatus === '完成作业'" class="flex-1 flex flex-col bg-white border-2 border-slate-200 rounded-2xl shadow-md p-6 overflow-hidden">
        <div class="flex justify-between items-center mb-6 shrink-0">
          <h2 class="text-3xl font-black text-slate-800 m-0 border-l-8 border-indigo-600 pl-4">裁切批次完工与入库</h2>
        </div>

        <div class="flex-1 flex gap-8 min-h-0">
          <div class="flex-1 flex flex-col h-full min-h-0 gap-6">
            <div class="bg-slate-50 border border-slate-200 p-8 rounded-3xl shadow-inner flex flex-col justify-center">
              <div class="text-slate-500 text-xl font-bold mb-2">批次总投料 (PCS)</div>
              <div class="text-5xl font-black text-slate-700">{{ totalIncomingQty }}</div>
            </div>
            <div class="bg-emerald-50 border border-emerald-200 p-8 rounded-3xl shadow-inner flex-1 flex flex-col justify-center relative overflow-hidden">
              <div class="text-emerald-800 text-2xl font-black mb-4 relative z-10"><IconifyIcon icon="lucide:check-circle" class="mr-2" />自动倒算合格产出 (PCS)</div>
              <div class="text-7xl font-black text-emerald-600 mb-2 relative z-10">{{ computedGoodQty }}</div>
              <div class="text-lg text-emerald-700 font-bold bg-white/50 p-3 rounded-lg inline-block self-start relative z-10 mt-2">公式: 投料总数 - 录入不良数</div>
            </div>
          </div>

          <div class="w-[450px] shrink-0 h-full flex flex-col">
            <div class="bg-white p-6 rounded-3xl border border-slate-200 shadow-lg flex-1 flex flex-col">
              <div class="text-slate-500 text-xl font-bold mb-4">录入裁圆产生的不良数 (PCS)</div>
              <span v-if="!keypadInput" class="text-5xl font-black text-slate-300 font-mono animate-pulse mb-6 flex-1">选填: 数字键盘</span>
              <span v-else class="text-[100px] font-black text-red-600 font-mono leading-none mb-6 flex-1 flex items-center">{{ keypadInput }}</span>
              <div class="grid grid-cols-3 gap-4 h-[350px]">
                <div v-for="key in ['1','2','3','4','5','6','7','8','9','C','0','DEL']" :key="key" @click="pressKey(key)" class="bg-slate-50 border-2 border-slate-100 rounded-2xl flex items-center justify-center text-4xl font-black text-slate-700 cursor-pointer hover:bg-indigo-50 active:bg-indigo-600 active:text-white transition-colors" :class="key === 'C' ? 'text-red-500' : (key === 'DEL' ? 'text-amber-500' : '')">{{ key === 'DEL' ? '⬅' : key }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>

    </main>

    <footer class="flex h-[80px] shrink-0 items-center justify-between border-t border-slate-200 bg-white px-8 shadow-[0_-8px_20px_rgba(0,0,0,0.03)] z-50 relative">
      <div class="flex items-center gap-3">
        <Button class="h-14 px-6 font-black text-lg text-slate-600 border-2 border-slate-300 rounded-xl hover:text-indigo-600 hover:border-indigo-400 shadow-sm" @click="emit('close')"><IconifyIcon icon="lucide:layout-dashboard" class="mr-2 text-2xl" /> 返回主板</Button>
        <Divider type="vertical" class="h-8 bg-slate-300 mx-2" />
        <Dropdown placement="topRight">
          <Button class="h-14 px-6 font-black text-lg text-slate-600 border-2 border-slate-300 rounded-xl hover:text-indigo-600 shadow-sm"><IconifyIcon icon="lucide:settings-2" class="mr-2 text-2xl" /> 机台操作</Button>
          <template #overlay>
            <Menu @click="handleMachineMenuClick" class="w-56 p-2 rounded-xl border-2 border-slate-200 shadow-xl">
              <Menu.Item key="pause"><div class="flex items-center text-amber-600 font-bold text-lg py-2"><IconifyIcon icon="lucide:pause-circle" class="mr-3 text-2xl"/> 暂停当前工单</div></Menu.Item>
              <Menu.Item key="abnormal"><div class="flex items-center text-red-600 font-bold text-lg py-2"><IconifyIcon icon="lucide:siren" class="mr-3 text-2xl"/> 异常拉灯提报</div></Menu.Item>
              <Menu.Divider class="my-2" />
              <Menu.Item key="handover"><div class="flex items-center text-emerald-600 font-bold text-lg py-2"><IconifyIcon icon="lucide:users" class="mr-3 text-2xl"/> 发起交接班</div></Menu.Item>
            </Menu>
          </template>
        </Dropdown>
      </div>

      <div class="flex gap-4 items-center">
        <template v-if="workOrderStatus === '未开工'">
          <Button type="primary" class="h-14 px-12 text-xl font-black rounded-xl bg-indigo-600 shadow-lg" @click="openStartJobModal">确定开机</Button>
        </template>

        <template v-if="workOrderStatus === '作业前准备'">
          <Button class="h-14 px-6 text-xl font-bold rounded-xl border-2 border-emerald-300 text-emerald-600 bg-emerald-50" @click="checkData.forEach(p=>p.result='正常')">一键确认全正常</Button>
          <Divider type="vertical" class="h-8 bg-slate-300 mx-2" />
          <Button type="primary" class="h-14 px-12 text-xl font-black rounded-xl bg-indigo-600 shadow-lg" :disabled="!isPreCheckComplete" @click="finishPreCheck">开始裁切作业</Button>
        </template>

        <template v-if="workOrderStatus === '作业中'">
          <Button type="primary" class="h-14 px-10 text-xl font-black rounded-xl bg-blue-600 shadow-lg" @click="workOrderStatus = '品质检验'">单品抽样送检</Button>
          <Button type="primary" class="h-14 px-10 text-xl font-black rounded-xl bg-emerald-600 shadow-lg border-none shadow-emerald-200" @click="workOrderStatus = '完成作业'">进入结单报工</Button>
        </template>

        <template v-if="workOrderStatus === '品质检验' && activeTabQc !== '检验历史台账'">
          <Button type="primary" class="h-14 px-16 text-2xl font-black rounded-xl bg-indigo-600 shadow-xl shadow-indigo-200" @click="openQualityWorkbench">🔬 弹出质检并填写综合报告</Button>
        </template>

        <template v-if="workOrderStatus === '完成作业'">
          <Button type="primary" class="h-14 px-10 text-xl font-black rounded-xl bg-blue-600 shadow-lg mr-6" @click="eDhrVisible = true">
            <IconifyIcon icon="lucide:file-bar-chart-2" class="mr-2" /> 综合大报告
          </Button>
          <Button class="w-[250px] h-14 text-xl font-black rounded-xl bg-emerald-500 hover:bg-emerald-600 border-none text-white shadow-xl shadow-emerald-200" @click="handleFinishClick">✅ 确认并结单入库</Button>
        </template>
      </div>
    </footer>

    <Modal v-model:open="startJobModalVisible" title="确认开机" :width="500" centered>
      <Form layout="vertical" class="mt-6">
        <div class="mb-6 bg-indigo-50 text-indigo-700 p-4 rounded-xl text-sm font-bold border border-indigo-100">已载入前序单品批次，请确认裁圆开机时间。</div>
        <Form.Item label="实际开工时间" required>
          <DatePicker v-model:value="startJobForm.time" show-time value-format="YYYY-MM-DD HH:mm:ss" class="w-full h-12 text-lg font-bold" />
        </Form.Item>
      </Form>
      <template #footer><Button size="large" @click="startJobModalVisible=false">取消</Button><Button size="large" type="primary" @click="confirmStartJob">确认开机</Button></template>
    </Modal>

    <Modal v-model:open="finishModalVisible" title="裁切完工报工与打印" @ok="submitFinish" width="600px" centered>
      <Form layout="vertical" class="mt-4 p-2">
        <div class="bg-indigo-50 text-indigo-700 p-3 rounded-lg mb-6 text-sm font-bold border border-indigo-200">
          请确认最终合格产出与报废数量。勾选下方选项将自动连接斑马打印机输出成品箱贴。
        </div>
        <Row :gutter="16">
          <Col :span="12">
            <Form.Item label="最终合格数量 (PCS)">
              <InputNumber :value="computedGoodQty" size="large" class="w-full h-12 font-black text-2xl text-emerald-600" disabled />
            </Form.Item>
          </Col>
          <Col :span="12">
            <Form.Item label="不良报废数量 (PCS)">
              <InputNumber :value="computedBadQty" size="large" class="w-full h-12 font-black text-2xl text-red-500" disabled />
            </Form.Item>
          </Col>
        </Row>
        <div class="mt-4 p-4 border border-blue-200 bg-blue-50 rounded-xl flex items-center">
          <label class="flex items-center gap-3 cursor-pointer font-bold text-slate-700 text-lg">
            <input type="checkbox" v-model="finishForm.printLabel" class="w-6 h-6 text-blue-600 rounded border-gray-300" />
            同步打印入库流转标签 (半成品库)
          </label>
        </div>
      </Form>
    </Modal>

    <Modal v-model:open="wbVisible" :title="`品质执行工作台 - ${wbTask.type}`" width="1350px" centered :footer="false" :destroyOnClose="true">
      <div class="p-4 bg-slate-50 rounded-lg flex flex-col h-[750px]">
        <div class="mb-4 bg-white p-4 rounded border shadow-sm flex flex-col gap-2 shrink-0">
          <div class="flex gap-2 items-center text-slate-500 font-bold">待检对象明细（每项对应表格中一列）：</div>
          <div class="flex flex-wrap gap-2">
            <Tag v-for="(t, idx) in wbTask.targets" :key="t" color="processing" class="font-mono text-sm px-2 py-1">列 {{idx + 1}}: {{t}}</Tag>
          </div>
        </div>
        <div class="mb-4 font-bold text-slate-500 shrink-0">
          调取检验标准：<Select v-model:value="wbTask.standard" :options="standardOptions" class="w-[400px] text-lg font-bold h-10" @change="loadStandardItems" placeholder="请选择适用标准..." />
        </div>

        <div class="flex-1 border border-slate-200 rounded-lg overflow-hidden bg-white relative">
          <Table :columns="getDynamicColumns(wbTask.targets)" :dataSource="wbTask.items" :pagination="false" :scroll="{ x: 'max-content', y: '100%' }" class="full-height-table absolute inset-0">
            <template #empty><div class="py-20 text-slate-400 font-bold text-center">请在上方选择标准以加载该样本的待检指标项</div></template>
            <template #bodyCell="{ column, record }">
              <template v-if="typeof column.dataIndex === 'string' && column.dataIndex.startsWith('val_')">
                <InputNumber v-if="record.itemType === 'QUANTITATIVE'" v-model:value="record.operatorValues[parseInt(column.dataIndex.split('_')[1])]" @change="handleValueChange(record)" class="w-full font-bold h-10 text-lg" placeholder="录入数值" />
                <Select v-else v-model:value="record.operatorValues[parseInt(column.dataIndex.split('_')[1])]" :options="[{label:'正常',value:'OK'},{label:'异常',value:'NG'}]" @change="handleValueChange(record)" class="w-full font-bold h-10 text-lg" placeholder="判定" />
              </template>

              <template v-if="column.dataIndex === 'operatorResult'">
                <span v-if="record.operatorResult==='OK'" class="text-emerald-600 font-black">⭕ 合格</span>
                <span v-else-if="record.operatorResult==='NG'" class="text-red-600 font-black">❌ 异常</span>
                <span v-else class="text-slate-300">-</span>
              </template>
            </template>
          </Table>
        </div>

        <div class="mt-4 p-4 bg-white border border-slate-200 rounded-lg shrink-0 flex items-center shadow-sm">
          <div class="font-bold text-slate-700 mr-4 text-base">本次送检综合判定：</div>
          <Radio.Group v-model:value="wbTask.summaryResult" button-style="solid" size="large" class="font-bold">
            <Radio.Button value="A级">A级 (优)</Radio.Button>
            <Radio.Button value="B级">B级 (良)</Radio.Button>
            <Radio.Button value="C级">C级 (中)</Radio.Button>
            <Radio.Button value="不合格">不合格 (NG)</Radio.Button>
          </Radio.Group>
          <Divider type="vertical" class="h-8 mx-6 bg-slate-200" />
          <div class="font-bold text-slate-700 mr-4 text-base">质检说明/备注：</div>
          <Input v-model:value="wbTask.summaryRemark" size="large" placeholder="请输入综合检验说明、异常描述或特采备注..." class="flex-1 h-10 text-base" />
        </div>

        <div class="flex justify-end gap-4 mt-4 pt-4 border-t shrink-0">
          <Button size="large" @click="wbVisible=false" class="font-bold">放弃保存</Button>
          <Button size="large" type="primary" class="bg-indigo-600 font-bold px-8" @click="submitWorkbench">生成结论单并存入历史台账</Button>
        </div>
      </div>
    </Modal>

    <Modal v-model:open="detailVisible" title="质量检验反馈单据 (动态列详情)" width="1350px" centered :footer="false">
      <div class="p-4 bg-slate-50 rounded-lg flex flex-col h-[700px]" v-if="detailRecord">
        <Descriptions bordered :column="3" class="bg-white mb-4 shrink-0" size="small">
          <Descriptions.Item label="单据号" :span="1"><span class="font-mono font-bold text-indigo-700">{{detailRecord.id}}</span></Descriptions.Item>
          <Descriptions.Item label="检测类型" :span="1"><Tag color="blue" class="font-bold">{{detailRecord.type}}</Tag></Descriptions.Item>
          <Descriptions.Item label="综合判定" :span="1"><Tag :color="detailRecord.judgment==='不合格'?'error':'success'" class="font-bold px-3 py-0.5 text-base">{{detailRecord.judgment}}</Tag></Descriptions.Item>
          <Descriptions.Item label="送检对象群 (对应表格列)" :span="2">
            <div class="flex flex-wrap gap-1"><Tag v-for="(t, idx) in detailRecord.targets" :key="t" color="processing">列 {{idx + 1}}: {{t}}</Tag></div>
          </Descriptions.Item>
          <Descriptions.Item label="异常数" :span="1">{{detailRecord.ngCount}} 项</Descriptions.Item>
          <Descriptions.Item label="执行标准" :span="3">{{detailRecord.standard || '系统默认临时标准'}}</Descriptions.Item>
          <Descriptions.Item label="质检备注/说明" :span="3"><span class="font-bold text-slate-700">{{detailRecord.remark || '无'}}</span></Descriptions.Item>
        </Descriptions>

        <div class="flex-1 border border-slate-200 rounded-lg overflow-hidden bg-white relative">
          <Table :columns="getDynamicColumns(detailRecord.targets || [])" :dataSource="detailRecord.items" :pagination="false" :scroll="{ x: 'max-content', y: '100%' }" class="full-height-table absolute inset-0">
            <template #empty><div class="py-10 text-slate-400 text-center">无详细指标记录</div></template>
            <template #bodyCell="{ column, record }">
              <template v-if="typeof column.dataIndex === 'string' && column.dataIndex.startsWith('val_')">
                <span class="font-bold text-indigo-700 text-lg">{{ record.operatorValues[parseInt(column.dataIndex.split('_')[1])] || '-' }}</span>
              </template>
              <template v-if="column.dataIndex === 'operatorResult'">
                <span :class="record.operatorResult==='OK'?'text-emerald-600':'text-red-600'" class="font-bold">{{record.operatorResult}}</span>
              </template>
            </template>
          </Table>
        </div>
        <div class="flex justify-end mt-4 shrink-0"><Button size="large" @click="detailVisible=false" class="font-bold px-8">关闭窗口</Button></div>
      </div>
    </Modal>

    <ComprehensiveReport v-model:visible="eDhrVisible" :reportData="eDhrData" />

    <Modal v-model:open="abnormalModalVisible" title="🚨 设备异常提报" :width="500" :footer="false" centered>
      <div class="p-6"><Select v-model:value="abnormalForm.reason" class="w-full h-12 text-lg mb-4" :options="[{label: '设备机械故障', value: '设备机械故障'}, {label: '品质异常', value: '品质异常'}]" placeholder="停机原因" /><Input.TextArea v-model:value="abnormalForm.desc" :rows="4" placeholder="异常描述..." class="mb-6 text-lg p-3" /><Button type="primary" danger block class="h-14 text-xl font-bold" @click="submitAbnormalStop">确认拉灯</Button></div>
    </Modal>

  </div>
</template>

<style scoped>
/* 扫码框放大 */
:deep(.custom-huge-input-start .ant-input) { height: 100% !important; font-size: 1.4rem !important; border-radius: 1rem 0 0 1rem !important; padding-left: 1.5rem !important; }
:deep(.custom-huge-input-start .ant-input-search-button) { height: 100% !important; width: 140px !important; font-size: 1.2rem !important; font-weight: bold !important; border-radius: 0 1rem 1rem 0 !important; background-color: #4f46e5; border-color: #4f46e5; }

/* Tabs 放大重写 */
.compact-tabs :deep(.ant-tabs-nav) { margin-bottom: 0 !important; }
.compact-tabs :deep(.ant-tabs-nav-list) { background: #f1f5f9; padding: 4px; border-radius: 12px; }
.compact-tabs :deep(.ant-tabs-tab) { padding: 10px 24px !important; margin: 0 !important; border-radius: 8px !important; transition: all 0.2s; font-size: 18px; font-weight: bold; color: #64748b;}
.compact-tabs :deep(.ant-tabs-tab-active) { background: #ffffff !important; color: #4338ca !important; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.1); }
.compact-tabs :deep(.ant-tabs-ink-bar) { display: none; }

/* 左侧Tab高度拉满 */
.custom-left-tabs { height: 100%; display: flex; }
:deep(.custom-left-tabs > .ant-tabs-nav) { height: 100%; }
:deep(.custom-left-tabs > .ant-tabs-content-holder) { flex: 1; height: 100%; display: flex; flex-direction: column; overflow: hidden; }
:deep(.custom-left-tabs > .ant-tabs-content-holder > .ant-tabs-content) { flex: 1; height: 100%; display: flex; flex-direction: column; }
:deep(.custom-left-tabs > .ant-tabs-content-holder > .ant-tabs-content > .ant-tabs-tabpane) { flex: 1; height: 100%; display: flex; flex-direction: column; }
.custom-left-tabs :deep(.ant-tabs-nav-list) { padding-top: 16px; width: 220px; }
.custom-left-tabs :deep(.ant-tabs-tab) { padding: 24px 24px !important; font-size: 18px; font-weight: 900; color: #94a3b8; transition: all 0.2s;}
.custom-left-tabs :deep(.ant-tabs-tab-active) { background: #eef2ff; color: #4338ca; border-right: 4px solid #4338ca; }

/* Table 大屏自适应防塌陷 */
.flex-1-table-container { flex: 1; min-height: 0; position: relative; display: flex; flex-direction: column; }
.full-height-table { position: absolute; top: 0; left: 0; right: 0; bottom: 0; }
.full-height-table :deep(.ant-table-wrapper), .full-height-table :deep(.ant-spin-nested-loading), .full-height-table :deep(.ant-spin-container), .full-height-table :deep(.ant-table), .full-height-table :deep(.ant-table-container) { height: 100%; display: flex; flex-direction: column; min-height: 0; }
.full-height-table :deep(.ant-table-body) { flex: 1; overflow-y: auto !important; min-height: 0; }
.full-height-table :deep(.ant-table-placeholder) { flex: 1; display: flex; align-items: center; justify-content: center; }
.full-height-table :deep(.ant-table-thead > tr > th) { background: #f8fafc !important; color: #64748b !important; font-size: 16px; font-weight: 900; padding: 16px 16px !important; border-bottom: 2px solid #e2e8f0; position: sticky; top: 0; z-index: 10;}
.full-height-table :deep(.ant-table-cell) { padding: 12px 16px !important; font-size: 16px; color: #334155; }
.full-height-table :deep(.ant-table-row:hover > td) { background: #f1f5f9 !important; }
.full-height-table :deep(.ant-table-body)::-webkit-scrollbar { width: 12px; height: 12px; }
.full-height-table :deep(.ant-table-body)::-webkit-scrollbar-thumb { background: #94a3b8; border-radius: 6px; border: 2px solid #f1f5f9; }
.full-height-table :deep(.ant-table-body)::-webkit-scrollbar-track { background: #f1f5f9; border-radius: 6px; }
::-webkit-scrollbar { width: 8px; height: 8px; }
::-webkit-scrollbar-thumb { background: #cbd5e1; border-radius: 4px; }
</style>
