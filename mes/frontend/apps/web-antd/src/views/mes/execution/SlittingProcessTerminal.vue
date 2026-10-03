<script lang="ts" setup>
import { ref, watch, computed, reactive, onBeforeUnmount } from 'vue';
import {
  Button, Card, Tag, Input, InputNumber, Table, Tabs, Dropdown, Menu,
  message, Modal, Select, Divider, Form, DatePicker, Row, Col, Progress, Radio, Descriptions, Switch
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
// 核心状态机 & 顶部流转
// ==================================================================================
const workOrderStatus = ref(props.order?.status === '完成作业' ? '完成作业' : '未开工');
const activeTabRun = ref('1'); // 作业中的左侧Tab
const activeTabQc = ref('首检(FAI)');
const qcTabsList = ['首检(FAI)', '过程抽检(IPQC)', '完工检(FQC)'];

function jumpToStep(step: string) {
  if (workOrderStatus.value === '未开工') return message.warning('请先完成扫码领料并开工！');
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

// ==================================================================================
// 1. 未开工 (工单/领料扫码 - 大屏化)
// ==================================================================================
const rollScanCode = ref('');
const incomingRoll = ref<any>(null);
const parentBatchNo = ref('');
const scannedMaterials = ref<any[]>([]);
const startJobModalVisible = ref(false);
const startJobForm = reactive({ time: dayjs().format('YYYY-MM-DD HH:mm:ss') });

const materialColumns: TableColumnsType = [
  { title: '复合基材母卷条码', dataIndex: 'batch', width: 220 },
  { title: '物料/产品名称', dataIndex: 'product', minWidth: 200 },
  { title: '来源', dataIndex: 'source', width: 150 },
  { title: '额定卷长 (m)', dataIndex: 'totalLength', width: 180, align: 'center' },
  { title: '扫码时间', dataIndex: 'time', width: 150 },
  { title: '操作', dataIndex: 'action', width: 80, align: 'center' }
];

watch(rollScanCode, (val) => {
  if (val && val.length >= 6) {
    message.loading('解析前序复合基材条码...', 0.5).then(() => {
      parentBatchNo.value = 'TAPE-' + val.slice(-5);
      incomingRoll.value = {
        id: Date.now(),
        code: val,
        batch: parentBatchNo.value,
        product: 'T01_Film_双面胶基材',
        totalLength: 2000,
        usedLength: 0,
        wasteLength: 0,
        source: '半成品库',
        time: new Date().toLocaleTimeString()
      };
      scannedMaterials.value = [incomingRoll.value];
      message.success(`原材料 ${parentBatchNo.value} (2000m) 扫描就绪`);
      rollScanCode.value = '';
    });
  }
});

function removeMaterial() {
  scannedMaterials.value = [];
  incomingRoll.value = null;
  parentBatchNo.value = '';
}

function openStartJobModal() {
  if (scannedMaterials.value.length === 0) return message.warning('🚨 请先扫描领用待分切母卷！');
  startJobForm.time = dayjs().format('YYYY-MM-DD HH:mm:ss');
  startJobModalVisible.value = true;
}

function confirmStartJob() {
  if(!startJobForm.time) return message.warning('请填写开机时间');
  startJobModalVisible.value = false;
  setTimeout(() => {
    workOrderStatus.value = '作业前准备';
    message.success('分切开机成功！进入准备阶段。');
    startQTime();
  }, 200);
}

// ==================================================================================
// 2. 作业前准备 (大表格化产线确认)
// ==================================================================================
const prepData = ref([
  { item: '核对工单规格', req: props.order?.spec || '标准规格', result: '未确认' },
  { item: '分切刀具安装检查', req: '刀片无缺损，间距准确', result: '未确认' },
  { item: '张力控制器归零', req: '表盘显示0', result: '未确认' }
]);
const prepColumns: TableColumnsType = [
  { title: '准备项目', dataIndex: 'item', width: '30%' }, { title: '标准/要求', dataIndex: 'req', width: '40%' },
  { title: '确认状态', dataIndex: 'result', width: '30%', align: 'center' }
];

const isPreCheckComplete = computed(() => prepData.value.every(d => d.result === '正常'));

function finishPreCheck() { workOrderStatus.value = '作业中'; }

// ==================================================================================
// 3. 作业中 (单品追踪 + 异常剔除)
// ==================================================================================
const remainingLength = computed(() => {
  if (!incomingRoll.value) return 0;
  return incomingRoll.value.totalLength - incomingRoll.value.usedLength - incomingRoll.value.wasteLength;
});
const progressPercent = computed(() => incomingRoll.value ? Number((((incomingRoll.value.usedLength + incomingRoll.value.wasteLength) / incomingRoll.value.totalLength) * 100).toFixed(1)) : 0);

// 3.1 单片生成
const pieceForm = ref({ length: null as number|null });
const pieceLogs = ref<any[]>([]);
const pieceColumns: TableColumnsType = [
  { title: '单品追溯码 (片/卷)', dataIndex: 'pieceCode', width: 250 },
  { title: '消耗母卷长 (m)', dataIndex: 'length', align: 'center', width: 180 },
  { title: '产出时间', dataIndex: 'time', width: 180 },
  { title: '打印状态', dataIndex: 'status', width: 120, align: 'center' },
  { title: '操作人', dataIndex: 'user', width: 120 },
  { title: '操作', dataIndex: 'action', width: 150, align: 'center' }
];

function handleGeneratePiece() {
  if (!pieceForm.value.length || pieceForm.value.length <= 0) return message.warning('消耗米数必须大于0');
  if (pieceForm.value.length > remainingLength.value) return message.warning(`不能大于剩余量 ${remainingLength.value}m`);

  const seq = String(pieceLogs.value.length + 1).padStart(4, '0');
  const pieceCode = `${parentBatchNo.value}-P${seq}`;

  pieceLogs.value.unshift({ id: Date.now(), pieceCode, length: pieceForm.value.length, time: new Date().toLocaleTimeString(), status: '待打印', user: props.operator?.userName || '系统' });
  incomingRoll.value.usedLength += pieceForm.value.length;
  pieceForm.value.length = null;
  message.success(`成功生成单品记录: ${pieceCode}`);
}

function handlePrintPiece(record: any) {
  Modal.info({
    title: '单品条码打印 (Zebra USB)',
    content: `正在输出单品标签...\n\n单品追溯码: ${record.pieceCode}\n规格: ${props.order?.spec || '标准'}`,
    onOk: () => { record.status = '已打印'; message.success(`${record.pieceCode} 标签已打印`); }
  });
}

// 3.2 异常切除
const wasteForm = ref({ length: null as number|null, reason: '涂布不均/破损' });
const wasteLogs = ref<any[]>([]);
const wasteColumns: TableColumnsType = [
  { title: '异常剔除米数 (m)', dataIndex: 'length', align: 'center', width: 200 },
  { title: '异常原因分类', dataIndex: 'reason' },
  { title: '剔除登记时间', dataIndex: 'time', width: 180 },
  { title: '操作人', dataIndex: 'user', width: 150 }
];

function handleAddWasteLog() {
  if (!wasteForm.value.length || wasteForm.value.length <= 0) return message.warning('剔除米数必须大于0');
  if (wasteForm.value.length > remainingLength.value) return message.warning(`不能大于剩余量 ${remainingLength.value}m`);

  wasteLogs.value.unshift({ id: Date.now(), length: wasteForm.value.length, reason: wasteForm.value.reason, time: new Date().toLocaleTimeString(), user: props.operator?.userName || '系统' });
  incomingRoll.value.wasteLength += wasteForm.value.length;
  message.success(`成功登记异常切除: ${wasteForm.value.length}m`);
  wasteForm.value.length = null;
}

// ==================================================================================
// 4. 品质检验 (动态列 + 单选判定)
// ==================================================================================
const inspectForm = reactive({ mode: 'piece', segmentPos: '中段', length: null as number|null, barcode: '' });
const pendingInspectItems = ref<{ id: string, detail: string }[]>([]);

const pendingInspectColumns: TableColumnsType = [
  { title: '待检对象明细', dataIndex: 'detail' },
  { title: '操作', dataIndex: 'action', width: 100, align: 'center' }
];

function addPendingItem() {
  if (inspectForm.mode === 'segment' && !inspectForm.length) return message.warning('请输入检测米数！');
  if (inspectForm.mode === 'piece' && !inspectForm.barcode) return message.warning('请扫入单件条码！');
  const detail = inspectForm.mode === 'segment' ? `按批次段长: ${inspectForm.segmentPos} ${inspectForm.length}米` : `单品序列号: ${inspectForm.barcode}`;
  pendingInspectItems.value.unshift({ id: `TSK-${Date.now()}`, detail });
  inspectForm.barcode = ''; inspectForm.length = null;
  message.success('已加入待检列表！');
}

const wbVisible = ref(false);
const wbTask = reactive({
  type: '', targetStr: '', targets: [] as string[], standard: null as string | null,
  summaryResult: 'A级', summaryRemark: '', items: [] as any[]
});
const standardOptions = [{ value: 'STD-SLIT-01', label: '分切单品出货检验标准 (V1.0)' }];

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
    { id: 1, inspectionItem: '分切宽度公差', itemType: 'QUANTITATIVE', standardDesc: '依工单 ±0.5mm', sampleSize: 3, minValueLimit: 119.5, maxValueLimit: 120.5, operatorValues: [], operatorMax: null, operatorMin: null, operatorAvg: null, operatorResult: '-' },
    { id: 2, inspectionItem: '分切长度公差', itemType: 'QUANTITATIVE', standardDesc: '依工单 ±1mm', sampleSize: 2, minValueLimit: 499, maxValueLimit: 501, operatorValues: [], operatorMax: null, operatorMin: null, operatorAvg: null, operatorResult: '-' },
    { id: 3, inspectionItem: '端面平整度', itemType: 'QUALITATIVE', standardDesc: '平齐无毛刺/波浪边', sampleSize: 1, operatorValues: [], operatorResult: '-' }
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
      row.operatorMax = Math.max(...numVals).toFixed(2); row.operatorMin = Math.min(...numVals).toFixed(2); row.operatorAvg = (numVals.reduce((a:number, b:number) => a + b, 0) / numVals.length).toFixed(2);
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
// 5. 完成作业 (键盘输入不良数，确认余卷退库)
// ==================================================================================
const trackMode = ref<'BATCH' | 'PIECE'>('BATCH');
const finishForm = reactive({ badQty: null as number|null });
const inboundSummary = reactive({ total: 0, good: 0, scrap: 0, yieldRate: '0%' });
const keypadInput = ref('');

function pressKey(key: string) { if (key === 'C') keypadInput.value = ''; else if (key === 'DEL') keypadInput.value = keypadInput.value.slice(0, -1); else keypadInput.value += key; }

function handleFinishSubmit() {
  const badQty = parseInt(keypadInput.value) || 0;

  inboundSummary.good = pieceLogs.value.length;
  inboundSummary.scrap = badQty;
  inboundSummary.total = inboundSummary.good + inboundSummary.scrap;
  inboundSummary.yieldRate = inboundSummary.total > 0 ? ((inboundSummary.good / inboundSummary.total) * 100).toFixed(1) + '%' : '0%';

  if (remainingLength.value > 0) {
    Modal.success({
      title: '完工结单成功与余卷退库',
      content: `检测到母卷剩余 ${remainingLength.value}m 未切完。\n已自动发送退库条码 [${parentBatchNo.value}-RET] 至打印机，请贴签后退回线边库。`,
      onOk: () => { message.success('分切工单全部完工！'); setTimeout(() => emit('close'), 500); }
    });
  } else {
    message.success('分切工单全部完工！');
    setTimeout(() => emit('close'), 500);
  }
}

// ==================================================================================
// 6. 综合大报告 eDHR
// ==================================================================================
const eDhrVisible = ref(false);
const eDhrData = computed(() => ({
  order: props.order, process: props.process, operator: props.operator, startTime: startJobForm.time,
  summary: inboundSummary, materials: scannedMaterials.value, checks: prepData.value,
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
        <span v-if="parentBatchNo" class="text-lg font-mono font-black text-indigo-700 bg-indigo-50 px-4 py-1.5 rounded-lg border border-indigo-200">当前执行母卷: {{ parentBatchNo }}</span>
      </div>
    </header>

    <main class="flex-1 flex flex-col min-h-0 p-4 overflow-hidden relative">

      <div v-show="workOrderStatus === '未开工'" class="flex-1 flex flex-col h-full bg-white rounded-3xl shadow-sm border border-slate-200 p-6 overflow-hidden">
        <div class="shrink-0 mb-6 flex items-center gap-6">
          <div class="flex-1 max-w-3xl">
            <div class="text-xl font-black text-slate-800 mb-2 flex items-center gap-2"><IconifyIcon icon="lucide:scan-face" /> 扫码领用 (前序复合基材母卷)</div>
            <Input.Search v-model:value="rollScanCode" size="large" placeholder="扫入待分切母卷条码..." class="h-14 text-xl font-mono custom-huge-input-start" @search="handleScanMixing" enter-button="确认装载" />
          </div>
        </div>
        <div class="flex-1-table-container border border-slate-200 rounded-xl">
          <Table :columns="materialColumns" :dataSource="scannedMaterials" :pagination="false" :scroll="{ y: '100%' }" class="full-height-table text-lg">
            <template #empty><div class="py-16 flex flex-col items-center text-slate-400"><IconifyIcon icon="lucide:layers" class="text-[60px] mb-4 opacity-50" /><span class="text-xl font-bold">暂无母卷数据</span></div></template>
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'totalLength'"><span class="text-emerald-600 font-bold">{{ record.totalLength }} m</span></template>
              <template v-if="column.dataIndex === 'action'"><Button type="text" danger class="bg-red-50 hover:bg-red-100" @click="removeMaterial"><IconifyIcon icon="lucide:trash-2" class="text-xl" /></Button></template>
            </template>
          </Table>
        </div>
      </div>

      <div v-show="workOrderStatus === '作业前准备'" class="flex-1 flex flex-col bg-white border-2 border-slate-200 rounded-2xl shadow-md overflow-hidden">
        <div class="p-4 bg-slate-50 border-b border-slate-200 text-xl font-black text-slate-700 flex items-center gap-2 shrink-0">
          <IconifyIcon icon="lucide:check-square" class="text-indigo-600" /> 分切设备开机前确认项
        </div>
        <div class="flex-1-table-container p-4">
          <Table :columns="prepColumns" :dataSource="prepData" :pagination="false" :scroll="{ y: '100%' }" class="full-height-table text-lg border border-slate-200 rounded-xl">
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'result'">
                <Select v-model:value="record.result" size="large" class="w-40 font-bold text-center">
                  <Select.Option value="未确认">未确认</Select.Option><Select.Option value="正常">正常</Select.Option>
                </Select>
              </template>
            </template>
          </Table>
        </div>
      </div>

      <div v-show="workOrderStatus === '作业中'" class="flex-1 flex flex-col bg-white border-2 border-slate-200 rounded-2xl shadow-md overflow-hidden">
        <Card class="bg-slate-50 border-b border-slate-200 shadow-sm shrink-0 rounded-none rounded-t-xl p-4">
          <div class="flex items-center justify-between">
            <div class="flex items-center gap-8">
              <div><span class="text-sm text-slate-400 font-bold">待切母卷</span><div class="font-mono font-black text-blue-700 text-3xl">{{ parentBatchNo || '未载入' }}</div></div>
              <Divider type="vertical" class="h-12 bg-slate-200" />
              <div><span class="text-sm text-slate-400 font-bold">母卷总长</span><div class="font-black text-slate-700 text-3xl">{{ incomingRoll?.totalLength || 0 }} <span class="text-lg">m</span></div></div>
              <div><span class="text-sm text-slate-400 font-bold">单品消耗汇总</span><div class="font-black text-green-600 text-3xl">{{ incomingRoll?.usedLength || 0 }} <span class="text-lg">m</span></div></div>
              <div><span class="text-sm text-slate-400 font-bold">废料切除汇总</span><div class="font-black text-red-500 text-3xl">{{ incomingRoll?.wasteLength || 0 }} <span class="text-lg">m</span></div></div>
              <div><span class="text-sm text-slate-400 font-bold">当前剩余长</span><div class="font-black text-orange-500 text-4xl">{{ remainingLength }} <span class="text-lg">m</span></div></div>
            </div>
            <div class="w-80">
              <div class="text-sm font-bold text-slate-500 flex justify-between mb-2"><span>母卷整体消耗进度</span><span>{{ progressPercent }}%</span></div>
              <Progress :percent="progressPercent" :showInfo="false" strokeColor="#2563eb" :strokeWidth="14" />
            </div>
          </div>
        </Card>

        <Tabs v-model:activeKey="activeTabRun" tabPosition="left" class="w-full h-full custom-left-tabs">
          <Tabs.TabPane key="1" tab="1. 单品分切追踪与赋码">
            <div class="flex flex-col h-full bg-slate-50 p-4 gap-4">
              <div class="flex items-center gap-6 p-5 bg-white border border-slate-200 rounded-xl shadow-sm shrink-0">
                <Form.Item label="本次切片消耗长度 (m)" class="mb-0 font-bold text-indigo-900 text-lg">
                  <InputNumber v-model:value="pieceForm.length" size="large" class="w-48 h-12 text-2xl font-black shadow-sm" :min="1" :max="remainingLength" />
                </Form.Item>
                <div class="flex-1 text-sm font-bold text-indigo-500">录入消耗长度后，系统自动生成以 "-Pxxxx" 结尾的独立单品追溯流水号。</div>
                <Button type="primary" size="large" class="bg-indigo-600 font-black px-10 shadow-md h-12 text-xl mb-[1px]" :disabled="remainingLength <= 0" @click="handleGeneratePiece">
                  <IconifyIcon icon="lucide:split-square-vertical" class="mr-2" /> 生成单品记录
                </Button>
              </div>
              <div class="flex-1 bg-white border border-slate-200 rounded-xl shadow-sm flex flex-col min-h-0 relative">
                <div class="flex-1-table-container">
                  <Table :columns="pieceColumns" :dataSource="pieceLogs" :pagination="false" :scroll="{ y: '100%' }" class="full-height-table text-lg">
                    <template #empty><div class="py-16 text-slate-400 font-bold text-center text-lg">暂无单品分切记录</div></template>
                    <template #bodyCell="{ column, record }">
                      <template v-if="column.dataIndex === 'pieceCode'"><span class="font-mono font-bold text-indigo-700">{{ record.pieceCode }}</span></template>
                      <template v-if="column.dataIndex === 'length'"><span class="font-bold">{{ record.length }}</span></template>
                      <template v-if="column.dataIndex === 'status'"><Tag :color="record.status === '已打印' ? 'success' : 'processing'" class="text-base py-1 px-3">{{ record.status }}</Tag></template>
                      <template v-if="column.dataIndex === 'action'"><Button type="primary" ghost size="large" class="font-bold" @click="handlePrintPiece(record)"><IconifyIcon icon="lucide:printer" class="mr-1" />打印</Button></template>
                    </template>
                  </Table>
                </div>
              </div>
            </div>
          </Tabs.TabPane>

          <Tabs.TabPane key="2" tab="2. 异常废料切除记账">
            <div class="flex flex-col h-full bg-red-50 p-4 gap-4 border-l border-red-100">
              <div class="flex items-center gap-6 p-5 bg-white border border-red-200 rounded-xl shadow-sm shrink-0">
                <Form.Item label="中间废料剔除长度 (m)" class="mb-0 font-bold text-red-900 text-lg">
                  <InputNumber v-model:value="wasteForm.length" size="large" class="w-48 h-12 text-2xl font-black shadow-sm" :min="1" :max="remainingLength" />
                </Form.Item>
                <Form.Item label="异常原因分类" class="mb-0 flex-1 font-bold text-red-900 text-lg">
                  <Select v-model:value="wasteForm.reason" size="large" class="w-full h-12 text-xl font-bold shadow-sm">
                    <Select.Option value="涂布不均/破损">涂布不均/破损</Select.Option>
                    <Select.Option value="接头部分剔除">接头部分剔除</Select.Option>
                    <Select.Option value="起皱/折痕">起皱/折痕</Select.Option>
                  </Select>
                </Form.Item>
                <Button danger type="primary" size="large" class="font-black px-10 shadow-md h-12 text-xl mb-[1px]" :disabled="remainingLength <= 0" @click="handleAddWasteLog">
                  执行废料切除记账
                </Button>
              </div>
              <div class="flex-1 bg-white border border-red-200 rounded-xl shadow-sm flex flex-col min-h-0 relative">
                <div class="flex-1-table-container">
                  <Table :columns="wasteColumns" :dataSource="wasteLogs" :pagination="false" :scroll="{ y: '100%' }" class="full-height-table text-lg">
                    <template #empty><div class="py-16 text-slate-400 font-bold text-center text-lg">暂无废料切除记录</div></template>
                    <template #bodyCell="{ column, record }">
                      <template v-if="column.dataIndex === 'length'"><span class="font-bold text-red-600">{{ record.length }}</span></template>
                    </template>
                  </Table>
                </div>
              </div>
            </div>
          </Tabs.TabPane>
        </Tabs>
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
                    <Input v-model:value="inspectForm.barcode" size="large" class="h-12 text-lg font-bold font-mono" placeholder="如: TAPE-12345-P0001" />
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
          <h2 class="text-3xl font-black text-slate-800 m-0 border-l-8 border-indigo-600 pl-4">分切结单报工与余卷处理</h2>
        </div>

        <div class="flex-1 flex gap-8 min-h-0">
          <div class="flex-1 flex flex-col h-full min-h-0 gap-6">
            <div class="bg-indigo-50 border border-indigo-200 p-6 rounded-3xl shadow-inner">
              <div class="text-indigo-800 text-2xl font-black mb-4"><IconifyIcon icon="lucide:check-circle" class="mr-2" />本工单合格产出自动汇总</div>
              <div class="text-6xl font-black text-indigo-600 mb-2">{{ pieceLogs.length }} <span class="text-2xl text-indigo-400">PCS (单品数)</span></div>
              <div class="text-lg text-indigo-500 font-bold mt-4">数据来源于“作业中”分切生成的所有追溯单品记录。</div>
            </div>
            <div class="bg-orange-50 border border-orange-200 p-6 rounded-3xl shadow-inner flex-1 flex flex-col justify-center">
              <div class="text-orange-800 text-2xl font-black mb-4"><IconifyIcon icon="lucide:package-open" class="mr-2" />检测到母卷剩余未切余量</div>
              <div class="text-5xl font-black text-orange-600 mb-4">{{ remainingLength }} <span class="text-2xl text-orange-400">m (米)</span></div>
              <div class="text-lg text-orange-700 font-bold bg-white/50 p-3 rounded-lg inline-block self-start">结单时系统将自动为您发送该余卷的 [退库条码] 至打印机。</div>
            </div>
          </div>

          <div class="w-[450px] shrink-0 h-full flex flex-col">
            <div class="bg-white p-6 rounded-3xl border border-slate-200 shadow-lg flex-1 flex flex-col">
              <div class="text-slate-400 text-2xl font-bold mb-4">录入本次不良单品数 (PCS)</div>
              <span v-if="!keypadInput" class="text-5xl font-black text-slate-300 font-mono animate-pulse mb-6 flex-1">使用键盘输入</span>
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
          <Button class="h-14 px-6 text-xl font-bold rounded-xl border-2 border-emerald-300 text-emerald-600 bg-emerald-50" @click="prepData.forEach(p=>p.result='正常')">一键确认设备就绪</Button>
          <Divider type="vertical" class="h-8 bg-slate-300 mx-2" />
          <Button type="primary" class="h-14 px-12 text-xl font-black rounded-xl bg-indigo-600 shadow-lg" :disabled="!isPreCheckComplete" @click="finishPreCheck">开始分切裁片</Button>
        </template>

        <template v-if="workOrderStatus === '作业中'">
          <Button type="primary" class="h-14 px-12 text-xl font-black rounded-xl bg-blue-600 shadow-lg" @click="workOrderStatus = '品质检验'">单品抽样送检</Button>
          <Button type="primary" class="h-14 px-12 text-xl font-black rounded-xl bg-emerald-600 shadow-lg border-none shadow-emerald-200" @click="workOrderStatus = '完成作业'">进入结单报工</Button>
        </template>

        <template v-if="workOrderStatus === '品质检验' && activeTabQc !== '检验历史台账'">
          <Button type="primary" class="h-14 px-16 text-2xl font-black rounded-xl bg-indigo-600 shadow-xl shadow-indigo-200" @click="openQualityWorkbench">🔬 弹出质检并填写综合报告</Button>
        </template>

        <template v-if="workOrderStatus === '完成作业'">
          <Button type="primary" class="h-14 px-10 text-xl font-black rounded-xl bg-blue-600 shadow-lg mr-6" @click="eDhrVisible = true">
            <IconifyIcon icon="lucide:file-bar-chart-2" class="mr-2" /> 综合大报告
          </Button>
          <Button class="w-[250px] h-14 text-xl font-black rounded-xl bg-emerald-500 hover:bg-emerald-600 border-none text-white shadow-xl shadow-emerald-200" @click="handleFinishSubmit">✅ 确认并结单入库</Button>
        </template>
      </div>
    </footer>

    <Modal v-model:open="startJobModalVisible" title="确认开机" :width="500" centered>
      <Form layout="vertical" class="mt-6">
        <div class="mb-6 bg-indigo-50 text-indigo-700 p-4 rounded-xl text-sm font-bold border border-indigo-100">已载入复合母卷 {{ incomingRoll?.batch }}，请确认分切开机时间。</div>
        <Form.Item label="实际开工时间" required>
          <DatePicker v-model:value="startJobForm.time" show-time value-format="YYYY-MM-DD HH:mm:ss" class="w-full h-12 text-lg font-bold" />
        </Form.Item>
      </Form>
      <template #footer><Button size="large" @click="startJobModalVisible=false">取消</Button><Button size="large" type="primary" @click="confirmStartJob">确认开机</Button></template>
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
