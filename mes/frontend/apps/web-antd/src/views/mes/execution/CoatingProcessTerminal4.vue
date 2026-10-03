<script lang="ts" setup>
import { ref, computed, reactive, onBeforeUnmount } from 'vue';
import {
  Button, Card, Tag, Input, InputNumber, Table, Tabs, Dropdown, Menu,
  message, Modal, Select, Divider, Form, DatePicker, Radio, Checkbox, Descriptions
} from 'ant-design-vue';
import type { TableColumnsType } from 'ant-design-vue';
import { IconifyIcon } from '@vben/icons';
import dayjs from 'dayjs';

const props = defineProps(['operator', 'order', 'process']);
const emit = defineEmits(['close', 'abnormal', 'handover', 'pause']);

const clone = (data: any) => JSON.parse(JSON.stringify(data));

// ==================================================================================
// 核心状态机 & 顶部流转
// ==================================================================================
const workOrderStatus = ref(props.order?.status === '完成作业' ? '完成作业' : '未开工');
const batchNo = ref(props.order?.batch || '');

const activeTabPre = ref('1');
const activeTabRun = ref('涂台工艺');
const activeTabQc = ref('首检');
const qcTabsList = ['首检', '自检', '过程抽检', '完工检'];

function jumpToStep(step: string) {
  if (workOrderStatus.value === '未开工') return message.warning('请先完成扫码投料开工！');
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
// 1. 未开工逻辑 (配料投料 & 开工确认)
// ==================================================================================
const mixingScanCode = ref('');
const scannedMaterials = ref<any[]>([]);
const startJobModalVisible = ref(false);
const startJobForm = reactive({ time: dayjs().format('YYYY-MM-DD HH:mm:ss') });

const materialColumns: TableColumnsType = [
  { title: '配料批次条码', dataIndex: 'mixBatch', width: 220 },
  { title: '物料/产品名称', dataIndex: 'product', minWidth: 200 },
  { title: '标准配重(kg)', dataIndex: 'weight', width: 120 },
  { title: '实际投料(kg)', dataIndex: 'actualWeight', width: 180, align: 'center' },
  { title: '操作人', dataIndex: 'creator', width: 120 },
  { title: '扫码时间', dataIndex: 'time', width: 150 },
  { title: '操作', dataIndex: 'action', width: 80, align: 'center' }
];

function handleScanMixing() {
  if (!mixingScanCode.value.trim()) return message.warning('请扫描配料码！');
  scannedMaterials.value.unshift({
    id: Date.now(),
    mixBatch: 'MB-' + Math.floor(Math.random()*10000),
    product: 'T01_核心聚氨酯胶液',
    weight: 500, actualWeight: 500,
    creator: props.operator?.userName || '系统',
    time: new Date().toLocaleTimeString()
  });
  mixingScanCode.value = '';
}
function removeMaterial(index: number) { scannedMaterials.value.splice(index, 1); }

function openStartJobModal() {
  if (scannedMaterials.value.length === 0) return message.warning('🚨 请至少扫描一桶配料！');
  startJobForm.time = dayjs().format('YYYY-MM-DD HH:mm:ss');
  startJobModalVisible.value = true;
}
function confirmStartJob() {
  if(!startJobForm.time) return message.warning('请填写开工时间');
  startJobModalVisible.value = false;
  setTimeout(() => {
    batchNo.value = 'COAT-' + new Date().getTime().toString().slice(-8);
    workOrderStatus.value = '作业前准备';
    message.success('开工成功！进入准备阶段。');
    startQTime();
  }, 200);
}

// ==================================================================================
// 2. 作业前准备
// ==================================================================================
const checkData = ref([
  { type: '环境点检', item: '车间温度 (℃)', req: '22 ± 2 ℃', checked: false },
  { type: '环境点检', item: '车间湿度 (%RH)', req: '50 ± 5 %RH', checked: false },
  { type: '设备点检', item: '主轴与牵引电机', req: '无异响、无卡顿', checked: false },
  { type: '安全点检', item: '急停开关测试', req: '动作灵敏有效', checked: false }
]);
const checkColumns: TableColumnsType = [
  { title: '点检类别', dataIndex: 'type', width: 120 }, { title: '确认项目', dataIndex: 'item' },
  { title: '规范要求', dataIndex: 'req' }, { title: '确认状态', dataIndex: 'checked', width: 150, align: 'center' }
];

const startupParams = ref([
  { node: '涂台', item: '刀口间隙(mm)', std: '0.15', val: null as number|null|string },
  { node: '烘干', item: '一区温度(℃)', std: '120.0', val: null as number|null|string },
]);
const startupColumns: TableColumnsType = [
  { title: '工艺节点', dataIndex: 'node', width: 120 }, { title: '参数名称', dataIndex: 'item' },
  { title: '参考标准', dataIndex: 'std', width: 150 }, { title: '实际设定', dataIndex: 'val', width: 220, align: 'center' }
];

function fetchEnvValues() { checkData.value[0].checked = true; checkData.value[1].checked = true; message.success('环境数据获取成功！'); }
function checkAllNormal() { checkData.value.forEach(d => d.checked = true); message.success('一键打钩完成！'); }

const isPreCheckComplete = computed(() => checkData.value.every(d => d.checked) && startupParams.value.every(p => p.val !== null && String(p.val).trim() !== ''));
function finishPreCheck() { workOrderStatus.value = '工艺参数'; }

// ==================================================================================
// 3. 工艺参数 (恢复完整的全节点)
// ==================================================================================
const processInputs = reactive({
  '涂台工艺': { level: null as number|null, speed: null as number|null },
  '凝固工艺': { thick: null as number|null, conc: null as number|null },
  '水洗工艺': { press: null as number|null, temp: null as number|null },
  '烘干工艺': { temp: null as number|null }
});
const runLogs = ref({
  '涂台工艺': [{ time: '08:00', level: 45.0, speed: 12.5, user: '张伟' }],
  '凝固工艺': [{ time: '08:00', thick: 0.15, conc: 15.0, user: '张伟' }],
  '水洗工艺': [{ time: '08:00', press: 0.3, temp: 30.0, user: '张伟' }],
  '烘干工艺': [{ time: '08:00', temp: 120, user: '张伟' }]
});
function submitProcessParams() {
  const inputs = processInputs[activeTabRun.value as keyof typeof processInputs];
  if (Object.values(inputs).some(v => v === null || v === '')) return message.warning('参数未填完整！');

  runLogs.value[activeTabRun.value as keyof typeof runLogs].unshift({
    time: new Date().toLocaleTimeString(),
    user: props.operator?.userName || '系统',
    ...inputs
  });

  Object.keys(inputs).forEach(key => (inputs as any)[key] = null);
  message.success('参数记录成功，已追加至台账！');
}

// ==================================================================================
// 4. 品质检验 (融合代码2的高级抽样检验)
// ==================================================================================
const inspectForm = reactive({ mode: 'segment', segmentPos: '中段', length: null as number|null, barcode: '' });
const pendingInspectItems = ref<{ id: string, detail: string }[]>([]);

const pendingInspectColumns: TableColumnsType = [
  { title: '待检对象明细', dataIndex: 'detail' },
  { title: '操作', dataIndex: 'action', width: 100, align: 'center' }
];

function addPendingItem() {
  if (inspectForm.mode === 'segment' && !inspectForm.length) return message.warning('请输入检测米数！');
  if (inspectForm.mode === 'piece' && !inspectForm.barcode) return message.warning('请扫入单件条码！');
  const detail = inspectForm.mode === 'segment' ? `按批次段长: ${inspectForm.segmentPos} ${inspectForm.length}米` : `单件序列号: ${inspectForm.barcode}`;
  pendingInspectItems.value.unshift({ id: `TSK-${Date.now()}`, detail });
  inspectForm.barcode = ''; inspectForm.length = null;
  message.success('已加入待检列表！');
}

const wbVisible = ref(false);
const wbTask = reactive({ type: '', targetStr: '', targets: [] as string[], standard: null as string | null, items: [] as any[] });
const standardOptions = [{ value: 'STD-T01', label: 'T01 光学膜通用质量标准 (V1.0)' }];

const wbColumns: TableColumnsType = [
  { title: '检测项目', dataIndex: 'inspectionItem', width: 140 },
  { title: '标准要求', dataIndex: 'standardDesc', width: 150 },
  { title: 'AQL应抽', dataIndex: 'sampleSize', width: 90, align: 'center' },
  { title: '实际记录 (N次实测数据)', dataIndex: 'operatorValues', minWidth: 260 },
  { title: '实测MAX', dataIndex: 'operatorMax', width: 90, align: 'center' },
  { title: '实测MIN', dataIndex: 'operatorMin', width: 90, align: 'center' },
  { title: '实测AVG', dataIndex: 'operatorAvg', width: 90, align: 'center' },
  { title: '单项结论', dataIndex: 'operatorResult', width: 100, align: 'center', fixed: 'right' }
];

function loadStandardItems() {
  wbTask.items = [
    { id: 1, inspectionItem: '外形尺寸宽幅', itemType: 'QUANTITATIVE', standardDesc: '1200±2mm', sampleSize: 4, minValueLimit: 1198, maxValueLimit: 1202, operatorValues: [], operatorMax: null, operatorMin: null, operatorAvg: null, operatorResult: '-' },
    { id: 2, inspectionItem: '涂层厚度', itemType: 'QUANTITATIVE', standardDesc: '0.15±0.02mm', sampleSize: 5, minValueLimit: 0.13, maxValueLimit: 0.17, operatorValues: [], operatorMax: null, operatorMin: null, operatorAvg: null, operatorResult: '-' },
    { id: 3, inspectionItem: '剥离强度', itemType: 'QUANTITATIVE', standardDesc: '≥0.8 N/cm', sampleSize: 3, minValueLimit: 0.8, maxValueLimit: 999, operatorValues: [], operatorMax: null, operatorMin: null, operatorAvg: null, operatorResult: '-' },
    { id: 4, inspectionItem: '透光率 (T%)', itemType: 'QUANTITATIVE', standardDesc: '≥92%', sampleSize: 3, minValueLimit: 92, maxValueLimit: 100, operatorValues: [], operatorMax: null, operatorMin: null, operatorAvg: null, operatorResult: '-' },
    { id: 5, inspectionItem: '表面外观缺陷', itemType: 'QUALITATIVE', standardDesc: '无划伤/气泡', sampleSize: 2, operatorValues: [], operatorResult: '-' },
    { id: 6, inspectionItem: '附着力测试', itemType: 'QUALITATIVE', standardDesc: '百格测试 5B级别', sampleSize: 2, operatorValues: [], operatorResult: '-' }
  ];
  wbTask.items.forEach(it => { it.operatorValues = new Array(it.sampleSize).fill(undefined); });
}

const sampleModalVisible = ref(false);
const currentSampleRow = ref<any>(null);

function openSampleInput(row: any) {
  currentSampleRow.value = row;
  sampleModalVisible.value = true;
}

function saveSampleInput() {
  const row = currentSampleRow.value;
  const validValues = row.operatorValues.filter((v:any) => v !== undefined && v !== null && v !== '');

  if (validValues.length < row.sampleSize) return message.warning(`必须录满 ${row.sampleSize} 个实测结果！`);

  if (row.itemType === 'QUANTITATIVE') {
    const numVals = validValues.map(Number);
    row.operatorMax = Math.max(...numVals).toFixed(3);
    row.operatorMin = Math.min(...numVals).toFixed(3);
    row.operatorAvg = (numVals.reduce((a:number, b:number) => a + b, 0) / numVals.length).toFixed(3);
    const isOk = row.operatorMax <= row.maxValueLimit && row.operatorMin >= row.minValueLimit;
    row.operatorResult = isOk ? 'OK' : 'NG';
  } else {
    const hasNg = validValues.includes('NG');
    row.operatorResult = hasNg ? 'NG' : 'OK';
  }
  sampleModalVisible.value = false;
}

function openQualityWorkbench() {
  if (pendingInspectItems.value.length === 0) return message.warning('当前待检队列为空！');
  wbTask.type = activeTabQc.value;
  wbTask.targets = pendingInspectItems.value.map(i => i.detail);
  wbTask.targetStr = wbTask.targets.join(' ; ');
  wbTask.standard = null;
  wbTask.items = [];
  wbVisible.value = true;
}

const qcHistory = ref<any[]>([
  { id: 'RPT-1001', target: '按批次: 首件3米', type: '首检(FAI)', submitTime: '08:10:00', completeTime: '08:15:00', judgment: 'OK', ngCount: 0, items: [] }
]);
const qcHistoryColumns: TableColumnsType = [
  { title: '检测对象摘要', dataIndex: 'target', minWidth: 160 },
  { title: '检测类型', dataIndex: 'type', width: 140 },
  { title: '提交时间', dataIndex: 'submitTime', width: 120, align: 'center' },
  { title: '完成时间', dataIndex: 'completeTime', width: 120, align: 'center' },
  { title: '最终结论', dataIndex: 'judgment', width: 100, align: 'center' },
  { title: '不合格数', dataIndex: 'ngCount', width: 100, align: 'center' },
  { title: '操作', dataIndex: 'action', width: 100, align: 'center', fixed: 'right' }
];

function submitWorkbench() {
  const isComplete = wbTask.items.every(i => i.operatorResult !== '-');
  if (!isComplete) return message.warning('请完成所有检测项目的录入与判定！');
  const ngCount = wbTask.items.filter(i => i.operatorResult === 'NG').length;

  qcHistory.value.unshift({
    id: `RPT-${Date.now().toString().slice(-6)}`,
    target: wbTask.targetStr, targets: clone(wbTask.targets), type: wbTask.type,
    submitTime: new Date(Date.now() - 300000).toLocaleTimeString(),
    completeTime: new Date().toLocaleTimeString(),
    judgment: ngCount > 0 ? 'NG' : 'OK', ngCount: ngCount,
    standard: wbTask.standard, items: clone(wbTask.items)
  });

  pendingInspectItems.value = [];
  wbVisible.value = false;
  message.success('检验报告已成功提交并归档！');
  activeTabQc.value = '检验历史台账';
}

const detailVisible = ref(false);
const detailRecord = ref<any>(null);
function openDetail(record: any) { detailRecord.value = record; detailVisible.value = true; }

// ==================================================================================
// 5. 完成作业 (融合代码2的单据打印机制)
// ==================================================================================
const trackMode = ref<'BATCH' | 'PIECE'>('PIECE');
const yieldInput = ref('');
const generatedBarcodes = ref<{ sn: string; time: string }[]>([]);

function generateSinglePiece() { generatedBarcodes.value.unshift({ sn: `SN${Date.now().toString().slice(-6)}${Math.floor(Math.random()*100)}`, time: new Date().toLocaleTimeString() }); }
function removePiece(index: number) { generatedBarcodes.value.splice(index, 1); }
function pressKey(key: string) { if (key === 'C') yieldInput.value = ''; else if (key === 'DEL') yieldInput.value = yieldInput.value.slice(0, -1); else yieldInput.value += key; }

const inboundModalVisible = ref(false);
const finishForm = reactive({ meters: null as number|null, waste: null as number|null });
const inboundSummary = reactive({ total: 0, good: 0, scrap: 0, yieldRate: '0%' });

function handleFinishJob(type: 'GOOD' | 'SCRAP') {
  let qty = trackMode.value === 'BATCH' ? parseInt(yieldInput.value) : generatedBarcodes.value.length;
  if (!qty || qty <= 0) return message.warning('产出数量无效！');
  if (type === 'SCRAP') {
    inboundSummary.scrap += qty;
    inboundSummary.total = inboundSummary.good + inboundSummary.scrap;
    message.success(`记录成功: ${qty} 件 不良品！`);
    yieldInput.value = ''; generatedBarcodes.value = [];
  } else {
    finishForm.meters = qty;
    inboundModalVisible.value = true;
  }
}

function submitFinish() {
  if (!finishForm.meters) return message.warning('必须输入良品数');
  inboundSummary.good += finishForm.meters;
  inboundSummary.total = inboundSummary.good + inboundSummary.scrap;
  inboundSummary.yieldRate = inboundSummary.total > 0 ? ((inboundSummary.good / inboundSummary.total) * 100).toFixed(1) + '%' : '0%';
  inboundModalVisible.value = false;
  yieldInput.value = ''; generatedBarcodes.value = [];
  previewInboundReceipt();
}

const inboundPreviewVisible = ref(false); const inboundPreviewHtml = ref('');
function previewInboundReceipt() {
  inboundPreviewHtml.value = `<html><head><style>body{font-family:sans-serif;font-size:14px;line-height:1.6;padding:20px;}h2{text-align:center;border-bottom:2px solid #000;padding-bottom:10px;}.info{display:grid;grid-template-columns:1fr 1fr;gap:10px;margin-top:20px;}.qty{font-size:24px;font-weight:bold;text-align:center;border:2px dashed #000;padding:20px;margin:20px 0;}</style></head><body><h2>成品入库交接单</h2><div class="info"><div>工单号：${props.order?.id || '测试工单'}</div><div>批次号：${batchNo.value}</div><div>交接人：${props.operator?.userName || '系统'}</div><div>时间：${new Date().toLocaleString()}</div></div><div class="qty">入库良品: ${finishForm.meters} 件</div></body></html>`;
  inboundPreviewVisible.value = true;
}
function printFromPreview() { message.success('单据已打印！'); inboundPreviewVisible.value = false; }

// ==================================================================================
// 6. 综合报告 (e-DHR，基于代码1)
// ==================================================================================
const eDhrVisible = ref(false);
const eDhrActiveTab = ref('1');

const edcHistoryFlat = computed(() => {
  let flat: any[] = [];
  for(let node in runLogs.value) {
    (runLogs.value as any)[node].forEach((log: any) => {
      flat.push({ phase: node, time: log.time, actualValue: JSON.stringify(log), result: 'OK' });
    });
  }
  return flat;
});
const edcColumns: TableColumnsType = [
  { title: '阶段', dataIndex: 'phase', width: 120 }, { title: '时间', dataIndex: 'time', width: 120 },
  { title: '参数记录包', dataIndex: 'actualValue' }, { title: '状态', dataIndex: 'result', width: 80 }
];

// 底部异常下拉操作
const abnormalModalVisible = ref(false); const abnormalForm = reactive({ reason: null, desc: '' });
function handleMachineMenuClick({ key }: { key: string }) {
  if (key === 'pause') { Modal.confirm({ title: '暂停工单', content: '确认挂起当前工单？', onOk() { emit('pause'); message.warn('工单挂起'); } }); }
  else if (key === 'abnormal') { abnormalForm.reason = null; abnormalForm.desc = ''; abnormalModalVisible.value = true; }
  else if (key === 'handover') { emit('handover'); }
}
function submitAbnormalStop() { if (!abnormalForm.reason) return message.warning('选原因！'); workOrderStatus.value = '异常停工'; message.error('🚨 已拉灯锁定！'); abnormalModalVisible.value = false; }

onBeforeUnmount(() => { if (qTimeState.timer) clearInterval(qTimeState.timer); });
</script>

<template>
  <div class="flex flex-col absolute inset-0 bg-slate-100 overflow-hidden">

    <header class="flex h-16 shrink-0 items-center justify-between border-b bg-white px-6 shadow-sm z-10">
      <div class="flex items-center gap-8">
        <div v-for="label in ['未开工', '作业前准备', '工艺参数', '品质检验', '完成作业']" :key="label"
             @click="jumpToStep(label)"
             :class="['flex items-center gap-2 font-black text-lg transition-colors cursor-pointer hover:text-indigo-500',
             workOrderStatus === label ? 'text-indigo-700 scale-105' : 'text-slate-400']">
          <IconifyIcon :icon="workOrderStatus === label ? 'lucide:circle-dot' : 'lucide:circle'" class="text-xl" />
          <span>{{ label }}</span>
        </div>
      </div>
      <div class="flex items-center gap-4">
        <span v-if="batchNo" class="text-lg font-mono font-black text-indigo-700 bg-indigo-50 px-4 py-1.5 rounded-lg border border-indigo-200">当前批次: {{ batchNo }}</span>
      </div>
    </header>

    <main class="flex-1 flex flex-col min-h-0 p-4 overflow-hidden relative">

      <div v-show="workOrderStatus === '未开工'" class="flex-1 flex flex-col h-full bg-white rounded-3xl shadow-sm border border-slate-200 p-6 overflow-hidden">
        <div class="shrink-0 mb-6 flex items-center gap-6">
          <div class="flex-1 max-w-3xl">
            <div class="text-xl font-black text-slate-800 mb-2 flex items-center gap-2"><IconifyIcon icon="lucide:scan-barcode" /> 扫码备料 (配料桶核销)</div>
            <Input.Search v-model:value="mixingScanCode" size="large" placeholder="扫入配料二维码..." class="h-14 text-xl font-mono custom-huge-input-start" @search="handleScanMixing" enter-button="确认添加" />
          </div>
        </div>
        <div class="flex-1-table-container border border-slate-200 rounded-xl">
          <Table :columns="materialColumns" :dataSource="scannedMaterials" :pagination="false" :scroll="{ y: '100%' }" class="full-height-table text-lg">
            <template #empty><div class="py-16 flex flex-col items-center text-slate-400"><IconifyIcon icon="lucide:package-open" class="text-[60px] mb-4 opacity-50" /><span class="text-xl font-bold">暂无投料数据</span></div></template>
            <template #bodyCell="{ column, record, index }">
              <template v-if="column.dataIndex === 'actualWeight'"><InputNumber v-model:value="record.actualWeight" class="w-32 h-10 text-lg font-bold text-indigo-700 text-center" /></template>
              <template v-if="column.dataIndex === 'action'"><Button type="text" danger class="bg-red-50 hover:bg-red-100" @click="removeMaterial(index)"><IconifyIcon icon="lucide:trash-2" class="text-xl" /></Button></template>
            </template>
          </Table>
        </div>
      </div>

      <div v-show="workOrderStatus === '作业前准备'" class="flex-1 flex flex-col bg-white border-2 border-slate-200 rounded-2xl shadow-md overflow-hidden">
        <div class="flex items-center bg-slate-50 border-b-2 border-slate-200 px-4 shrink-0">
          <Tabs v-model:activeKey="activeTabPre" type="capsule" class="compact-tabs py-3">
            <Tabs.TabPane key="1" tab="开机环境与设备点检" /><Tabs.TabPane key="2" tab="工艺参数打底设定" />
          </Tabs>
        </div>
        <div class="flex-1-table-container p-4">
          <Table v-if="activeTabPre === '1'" :columns="checkColumns" :dataSource="checkData" :pagination="false" :scroll="{ y: '100%' }" class="full-height-table text-lg border border-slate-200 rounded-xl">
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'checked'">
                <Checkbox v-model:checked="record.checked" class="scale-150 transform translate-x-2" />
              </template>
            </template>
          </Table>
          <Table v-if="activeTabPre === '2'" :columns="startupColumns" :dataSource="startupParams" :pagination="false" :scroll="{ y: '100%' }" class="full-height-table text-lg border border-slate-200 rounded-xl">
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'val'"><InputNumber v-model:value="record.val" class="w-48 h-10 text-xl font-black text-indigo-700" placeholder="录入实际值" /></template>
            </template>
          </Table>
        </div>
      </div>

      <div v-show="workOrderStatus === '工艺参数'" class="flex-1 flex flex-col bg-white border-2 border-slate-200 rounded-2xl shadow-md overflow-hidden">
        <Tabs v-model:activeKey="activeTabRun" tabPosition="left" class="w-full h-full custom-left-tabs">
          <Tabs.TabPane v-for="node in ['涂台工艺', '凝固工艺', '水洗工艺', '烘干工艺']" :key="node" :tab="node">
            <div class="flex h-full p-4 gap-6 bg-slate-50">
              <div class="w-[400px] shrink-0 bg-white p-6 rounded-2xl border border-slate-200 shadow-sm flex flex-col">
                <div class="text-xl font-black text-indigo-900 mb-6 border-b pb-3">录入本周期参数</div>
                <Form layout="vertical" class="flex-1 overflow-y-auto">
                  <template v-if="node === '涂台工艺'">
                    <Form.Item label="料槽液位 (CM)"><InputNumber v-model:value="processInputs['涂台工艺'].level" class="w-full h-12 text-xl font-bold" /></Form.Item>
                    <Form.Item label="下料线速 (m/min)"><InputNumber v-model:value="processInputs['涂台工艺'].speed" class="w-full h-12 text-xl font-bold" /></Form.Item>
                  </template>
                  <template v-if="node === '凝固工艺'">
                    <Form.Item label="凝固厚度 (mm)"><InputNumber v-model:value="processInputs['凝固工艺'].thick" class="w-full h-12 text-xl font-bold" /></Form.Item>
                    <Form.Item label="DMF浓度 (%)"><InputNumber v-model:value="processInputs['凝固工艺'].conc" class="w-full h-12 text-xl font-bold" /></Form.Item>
                  </template>
                  <template v-if="node === '水洗工艺'">
                    <Form.Item label="水洗压力 (MPa)"><InputNumber v-model:value="processInputs['水洗工艺'].press" class="w-full h-12 text-xl font-bold" /></Form.Item>
                    <Form.Item label="水洗温度 (℃)"><InputNumber v-model:value="processInputs['水洗工艺'].temp" class="w-full h-12 text-xl font-bold" /></Form.Item>
                  </template>
                  <template v-if="node === '烘干工艺'">
                    <Form.Item label="烘箱温度 (℃)"><InputNumber v-model:value="processInputs['烘干工艺'].temp" class="w-full h-12 text-xl font-bold" /></Form.Item>
                  </template>
                </Form>
              </div>
              <div class="flex-1 bg-white p-4 rounded-2xl border border-slate-200 shadow-sm flex flex-col min-h-0 relative">
                <div class="text-lg font-bold text-slate-700 mb-4 px-2 shrink-0">📋 {{node}} 历史记录</div>
                <div class="flex-1-table-container border border-slate-200 rounded-xl overflow-hidden">
                  <Table :dataSource="runLogs[node]" :pagination="false" :scroll="{ y: '100%' }" class="full-height-table text-base absolute inset-0">
                    <Table.Column title="时间" dataIndex="time" />
                    <template v-if="node === '涂台工艺'"><Table.Column title="液位" dataIndex="level" /><Table.Column title="线速" dataIndex="speed" /></template>
                    <template v-if="node === '凝固工艺'"><Table.Column title="厚度" dataIndex="thick" /><Table.Column title="浓度" dataIndex="conc" /></template>
                    <template v-if="node === '水洗工艺'"><Table.Column title="压力" dataIndex="press" /><Table.Column title="温度" dataIndex="temp" /></template>
                    <template v-if="node === '烘干工艺'"><Table.Column title="温度" dataIndex="temp" /></template>
                    <Table.Column title="操作人" dataIndex="user" />
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
                <div class="text-xl font-black text-slate-800 mb-6 border-b pb-4 text-indigo-600">录入 {{qcType}} 对象</div>
                <Form layout="vertical">
                  <Form.Item label="检测模式">
                    <Radio.Group v-model:value="inspectForm.mode" button-style="solid" class="w-full flex" size="large">
                      <Radio.Button value="segment" class="flex-1 text-center font-bold">按批次/段(米)</Radio.Button>
                      <Radio.Button value="piece" class="flex-1 text-center font-bold">按单件(扫码)</Radio.Button>
                    </Radio.Group>
                  </Form.Item>
                  <template v-if="inspectForm.mode === 'segment'">
                    <Form.Item label="取样段位"><Select v-model:value="inspectForm.segmentPos" size="large" :options="[{value:'前段',label:'前段'},{value:'中段',label:'中段'},{value:'后段',label:'后段'}]" /></Form.Item>
                    <Form.Item label="涉及米数"><InputNumber v-model:value="inspectForm.length" class="w-full h-12 text-xl font-bold" /></Form.Item>
                  </template>
                  <template v-if="inspectForm.mode === 'piece'">
                    <Form.Item label="扫描条码"><Input v-model:value="inspectForm.barcode" size="large" class="h-12 text-lg" /></Form.Item>
                  </template>
                </Form>
                <Button type="primary" size="large" class="w-full h-14 text-xl font-bold mt-2" @click="addPendingItem">加入当前待检列表 👉</Button>
              </div>

              <div class="flex-1 p-6 flex flex-col min-h-0 relative">
                <div class="text-xl font-black text-indigo-900 mb-4">待送检清单 (共 {{pendingInspectItems.length}} 件)</div>
                <div class="flex-1-table-container border border-slate-200 rounded-xl bg-white shadow-sm overflow-hidden">
                  <Table :columns="pendingInspectColumns" :dataSource="pendingInspectItems" :pagination="false" :scroll="{ y: '100%' }" class="full-height-table text-base absolute inset-0">
                    <template #empty><div class="py-16 text-slate-400 font-bold text-lg text-center">尚未录入，请在左侧加入或直接点击底部提交</div></template>
                    <template #bodyCell="{ column, index }">
                      <template v-if="column.dataIndex === 'action'">
                        <Button type="text" danger @click="pendingInspectItems.splice(index,1)"><IconifyIcon icon="lucide:trash-2"/></Button>
                      </template>
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
                    <template v-if="column.dataIndex === 'judgment'">
                      <Tag :color="record.judgment === 'OK' ? 'success' : 'error'" class="text-base font-bold py-1 px-4">{{record.judgment}}</Tag>
                    </template>
                    <template v-if="column.dataIndex === 'action'">
                      <Button type="link" size="large" class="font-bold text-blue-600" @click="openDetail(record)">查看详情</Button>
                    </template>
                  </template>
                </Table>
              </div>
            </div>
          </Tabs.TabPane>

        </Tabs>
      </div>

      <div v-show="workOrderStatus === '完成作业'" class="flex-1 flex flex-col bg-white border-2 border-slate-200 rounded-2xl shadow-md p-6 overflow-hidden">
        <div class="flex justify-between items-center mb-6 shrink-0">
          <h2 class="text-3xl font-black text-slate-800 m-0 border-l-8 border-indigo-600 pl-4">终端过站与报工入库</h2>
          <div class="flex bg-slate-100 p-1.5 rounded-xl border border-slate-200">
            <div @click="trackMode = 'PIECE'" class="px-6 py-2 rounded-lg font-bold cursor-pointer text-xl" :class="trackMode === 'PIECE' ? 'bg-white text-indigo-600 shadow-md' : 'text-slate-500'">单件赋码</div>
            <div @click="trackMode = 'BATCH'" class="px-6 py-2 rounded-lg font-bold cursor-pointer text-xl" :class="trackMode === 'BATCH' ? 'bg-white text-indigo-600 shadow-md' : 'text-slate-500'">批量数字键盘</div>
          </div>
        </div>

        <div class="flex-1 flex gap-8 min-h-0">
          <div class="flex-1 flex flex-col h-full min-h-0">
            <div v-if="trackMode === 'PIECE'" class="flex-1 flex flex-col bg-slate-50 border-2 border-slate-200 rounded-3xl p-6 shadow-inner overflow-hidden">
              <Button size="large" class="shrink-0 h-16 text-xl font-bold bg-indigo-50 border-dashed border-indigo-300 text-indigo-600 rounded-xl mb-6" @click="generateSinglePiece">➕ 模拟产出一件 (生成条码)</Button>
              <div class="flex-1 overflow-y-auto space-y-3 pr-2">
                <div v-if="generatedBarcodes.length===0" class="h-full flex items-center justify-center text-slate-400 font-bold text-xl">点击上方按钮或使用扫码枪</div>
                <div v-for="(code, idx) in generatedBarcodes" :key="idx" class="flex justify-between items-center bg-white p-4 rounded-xl border border-slate-200 shadow-sm">
                  <div class="flex items-center gap-4"><span class="bg-slate-800 text-white w-8 h-8 flex items-center justify-center rounded-full font-bold">{{ generatedBarcodes.length - idx }}</span><span class="text-2xl font-mono font-black text-slate-700">{{ code.sn }}</span></div>
                  <Button type="text" danger @click="removePiece(idx)"><IconifyIcon icon="lucide:trash-2" class="text-3xl" /></Button>
                </div>
              </div>
            </div>
            <div v-else class="flex-1 flex flex-col bg-slate-50 border-2 border-slate-200 rounded-3xl p-8 justify-center items-end shadow-inner relative">
              <div class="text-slate-400 text-2xl font-bold mb-4">当前录入数量</div>
              <span v-if="!yieldInput" class="text-5xl font-black text-slate-300 font-mono animate-pulse">使用数字键盘</span>
              <span v-else class="text-[150px] font-black text-slate-800 font-mono leading-none">{{ yieldInput }}</span>
            </div>
          </div>

          <div class="w-[450px] shrink-0 h-full">
            <div v-if="trackMode === 'BATCH'" class="bg-white p-6 rounded-3xl border border-slate-200 shadow-lg h-full">
              <div class="grid grid-cols-3 gap-4 h-full">
                <div v-for="key in ['1','2','3','4','5','6','7','8','9','C','0','DEL']" :key="key" @click="pressKey(key)" class="bg-slate-50 border-2 border-slate-100 rounded-2xl flex items-center justify-center text-5xl font-black text-slate-700 cursor-pointer hover:bg-indigo-50 active:bg-indigo-600 active:text-white transition-colors" :class="key === 'C' ? 'text-red-500' : (key === 'DEL' ? 'text-amber-500' : '')">{{ key === 'DEL' ? '⬅' : key }}</div>
              </div>
            </div>
            <div v-else class="bg-indigo-50 border-2 border-indigo-100 rounded-3xl p-8 flex flex-col items-center justify-center text-center h-full">
              <IconifyIcon icon="lucide:scan" class="text-[100px] text-indigo-300 mb-6" />
              <h3 class="text-2xl font-bold text-indigo-800 mb-4">单件扫码模式</h3>
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
          <Button type="primary" class="h-14 px-12 text-xl font-black rounded-xl bg-indigo-600 shadow-lg" @click="openStartJobModal">确定开工</Button>
        </template>

        <template v-if="workOrderStatus === '作业前准备'">
          <Button class="h-14 px-6 text-xl font-bold rounded-xl border-2 border-indigo-300 text-indigo-600 bg-indigo-50" v-if="activeTabPre === '1'" @click="fetchEnvValues">获取环境数据</Button>
          <Button class="h-14 px-6 text-xl font-bold rounded-xl border-2 border-emerald-300 text-emerald-600 bg-emerald-50" v-if="activeTabPre === '1'" @click="checkAllNormal">一键点检合格</Button>
          <Divider type="vertical" class="h-8 bg-slate-300 mx-2" v-if="activeTabPre === '1'" />
          <Button type="primary" class="h-14 px-12 text-xl font-black rounded-xl bg-indigo-600 shadow-lg" :disabled="!isPreCheckComplete" @click="finishPreCheck">完成开机准备</Button>
        </template>

        <template v-if="workOrderStatus === '工艺参数'">
          <Button type="primary" class="h-14 px-12 text-xl font-black rounded-xl bg-blue-600 shadow-lg" @click="submitProcessParams">提交参数</Button>
        </template>

        <template v-if="workOrderStatus === '品质检验' && activeTabQc !== '检验历史台账'">
          <Button type="primary" class="h-14 px-16 text-2xl font-black rounded-xl bg-indigo-600 shadow-xl shadow-indigo-200" @click="openQualityWorkbench">🔬 提交质检并填写报告</Button>
        </template>

        <template v-if="workOrderStatus === '完成作业'">
          <Button size="large" class="h-14 px-10 text-xl font-black rounded-xl border-4 border-blue-300 text-blue-600 bg-blue-50 shadow-md hover:bg-blue-100 mr-6" @click="eDhrVisible = true">
            <IconifyIcon icon="lucide:file-bar-chart-2" class="mr-2" /> 生产综合大报告
          </Button>
          <Button class="w-[200px] h-14 text-xl font-black rounded-xl bg-red-500 hover:bg-red-600 border-none text-white shadow-lg" @click="handleFinishJob('SCRAP')">💥 报废扣除</Button>
          <Button class="w-[200px] h-14 text-xl font-black rounded-xl bg-emerald-500 hover:bg-emerald-600 border-none text-white shadow-xl shadow-emerald-200" @click="handleFinishJob('GOOD')">✅ 记录良品入库</Button>
        </template>
      </div>
    </footer>

    <Modal v-model:open="startJobModalVisible" title="确认开工" :width="500" centered>
      <Form layout="vertical" class="mt-6">
        <div class="mb-6 bg-indigo-50 text-indigo-700 p-4 rounded-xl text-sm font-bold border border-indigo-100">已核销 {{ scannedMaterials.length }} 桶配料，请确认开工时间。</div>
        <Form.Item label="实际开工时间" required>
          <DatePicker v-model:value="startJobForm.time" show-time value-format="YYYY-MM-DD HH:mm:ss" class="w-full h-12 text-lg font-bold" />
        </Form.Item>
      </Form>
      <template #footer><Button size="large" @click="startJobModalVisible=false">取消</Button><Button size="large" type="primary" @click="confirmStartJob">确认开工</Button></template>
    </Modal>

    <Modal v-model:open="wbVisible" :title="`品质执行工作台 - ${wbTask.type}`" width="1300px" centered :footer="false" :destroyOnClose="true">
      <div class="p-4 bg-slate-50 rounded-lg">
        <div class="mb-4 bg-white p-4 rounded border shadow-sm flex flex-col gap-2">
          <div class="flex gap-2 items-center text-slate-500 font-bold">将对以下对象执行统一检验：</div>
          <div class="flex flex-wrap gap-2"><Tag v-for="t in wbTask.targets" :key="t" color="processing" class="font-mono text-sm px-2 py-1">{{t}}</Tag></div>
        </div>
        <div class="mb-4 font-bold text-slate-500">
          调取检验标准：<Select v-model:value="wbTask.standard" :options="standardOptions" class="w-[400px] text-lg font-bold h-10" @change="loadStandardItems" placeholder="请选择适用标准..." />
        </div>
        <div class="h-[450px] border border-slate-200 rounded-lg overflow-hidden bg-white mb-4 relative">
          <Table :columns="wbColumns" :dataSource="wbTask.items" :pagination="false" :scroll="{ y: '100%' }" class="full-height-table absolute inset-0">
            <template #empty><div class="py-20 text-slate-400 font-bold text-center">请在上方选择标准以加载待检指标项</div></template>
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'operatorValues'">
                <div class="flex flex-col gap-1">
                  <Button size="small" type="dashed" class="w-full text-indigo-600 bg-indigo-50 border-indigo-300" @click="openSampleInput(record)">
                    <IconifyIcon icon="lucide:keyboard" class="mr-1" /> 录入 {{record.sampleSize}} 项实测值
                  </Button>
                  <div class="flex flex-wrap gap-1 mt-1"><Tag v-for="(v,i) in record.operatorValues" :key="i" :color="v===undefined?'default':'blue'">{{ v===undefined?'-':v }}</Tag></div>
                </div>
              </template>
              <template v-if="column.dataIndex === 'operatorResult'">
                <span v-if="record.operatorResult==='OK'" class="text-emerald-600 font-black">⭕ 合格</span>
                <span v-else-if="record.operatorResult==='NG'" class="text-red-600 font-black">❌ 异常</span>
                <span v-else class="text-slate-300">-</span>
              </template>
            </template>
          </Table>
        </div>
        <div class="flex justify-end gap-4 mt-2">
          <Button size="large" @click="wbVisible=false" class="font-bold">放弃保存</Button>
          <Button size="large" type="primary" class="bg-indigo-600 font-bold px-8" @click="submitWorkbench">生成最终结论并存入台账</Button>
        </div>
      </div>
    </Modal>

    <Modal v-model:open="sampleModalVisible" :title="`录入实测值: ${currentSampleRow?.inspectionItem}`" @ok="saveSampleInput" width="450px" centered>
      <div v-if="currentSampleRow" class="py-4">
        <div class="bg-indigo-50 p-3 rounded mb-4 text-sm text-indigo-700 font-bold border border-indigo-100">
          要求: {{ currentSampleRow.standardDesc }} | 需抽样数: {{ currentSampleRow.sampleSize }} 个
        </div>
        <div class="max-h-[300px] overflow-y-auto pr-2">
          <div v-for="i in currentSampleRow.sampleSize" :key="i" class="flex items-center gap-4 mb-3">
            <span class="font-mono font-bold w-20 text-slate-500">样本 {{ i }}</span>
            <InputNumber v-if="currentSampleRow.itemType === 'QUANTITATIVE'" v-model:value="currentSampleRow.operatorValues[i-1]" class="flex-1 h-10 text-lg" placeholder="实测数值" />
            <Select v-else v-model:value="currentSampleRow.operatorValues[i-1]" class="flex-1 h-10 text-lg font-bold" :options="[{label:'合格(OK)',value:'OK'},{label:'异常(NG)',value:'NG'}]" placeholder="判断结果" />
          </div>
        </div>
      </div>
    </Modal>

    <Modal v-model:open="detailVisible" title="质量检验反馈报告单 (详情)" width="1200px" centered :footer="false">
      <div class="p-4 bg-slate-50 rounded-lg" v-if="detailRecord">
        <Descriptions bordered :column="3" class="bg-white mb-4" size="small">
          <Descriptions.Item label="报告单号" :span="1"><span class="font-mono font-bold text-indigo-700">{{detailRecord.id}}</span></Descriptions.Item>
          <Descriptions.Item label="检测类型" :span="1"><Tag color="blue" class="font-bold">{{detailRecord.type}}</Tag></Descriptions.Item>
          <Descriptions.Item label="系统判定" :span="1"><Tag :color="detailRecord.judgment==='OK'?'success':'error'" class="font-bold px-3 py-0.5">{{detailRecord.judgment}}</Tag></Descriptions.Item>
          <Descriptions.Item label="送检对象" :span="2"><div class="flex flex-wrap gap-1"><Tag v-for="t in detailRecord.targets" :key="t" color="processing">{{t}}</Tag></div></Descriptions.Item>
          <Descriptions.Item label="不合格数" :span="1">{{detailRecord.ngCount}} 项</Descriptions.Item>
          <Descriptions.Item label="执行标准" :span="3">{{detailRecord.standard || '系统默认临时标准'}}</Descriptions.Item>
        </Descriptions>
        <div class="h-[300px] border border-slate-200 rounded-lg overflow-hidden bg-white mb-4 relative">
          <Table :columns="wbColumns" :dataSource="detailRecord.items" :pagination="false" :scroll="{ y: '100%' }" class="full-height-table absolute inset-0">
            <template #empty><div class="py-10 text-slate-400 text-center">无详细指标记录</div></template>
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'operatorValues'">
                <div class="flex flex-wrap gap-1"><Tag v-for="(v,i) in record.operatorValues" :key="i" color="blue" class="!m-0 font-mono">{{ v }}</Tag></div>
              </template>
              <template v-if="column.dataIndex === 'operatorResult'">
                <span :class="record.operatorResult==='OK'?'text-emerald-600':'text-red-600'" class="font-bold">{{record.operatorResult}}</span>
              </template>
            </template>
          </Table>
        </div>
        <div class="flex justify-end"><Button size="large" @click="detailVisible=false" class="font-bold px-8">关闭窗口</Button></div>
      </div>
    </Modal>

    <Modal v-model:open="inboundModalVisible" title="✅ 湿法完工入库" @ok="submitFinish" :width="600" centered>
      <Form layout="vertical" class="mt-4 p-4"><div class="mb-6 text-sm font-bold text-indigo-700 bg-indigo-50 p-4 rounded-xl border border-indigo-200">系统将依据您录入的总数与左侧数量核对。</div><Form.Item label="入库良品总计" required><InputNumber v-model:value="finishForm.meters" class="w-full h-14 text-2xl font-black" /></Form.Item></Form>
    </Modal>
    <Modal v-model:open="inboundPreviewVisible" title="📦 生产流转入库单预览" :width="800" :footer="false" centered><div class="p-4 bg-slate-100 rounded-xl mb-4"><iframe :srcdoc="inboundPreviewHtml" class="w-full h-[400px] bg-white border border-slate-300 rounded"></iframe></div><div class="flex justify-end"><Button size="large" type="primary" class="bg-indigo-600 font-bold" @click="printFromPreview">打印单据</Button></div></Modal>

    <Modal v-model:open="eDhrVisible" title="📄 综合生产执行大报告 (e-DHR)" class="w-[1100px]" :footer="false" centered>
      <div class="flex flex-col h-[650px] overflow-hidden bg-slate-50 p-2 relative">
        <Tabs v-model:activeKey="eDhrActiveTab" class="flex-1 flex flex-col overflow-hidden custom-dhr-tabs" type="card">
          <Tabs.TabPane key="1" tab="1. 基础信息汇总">
            <div class="h-full p-6 bg-white overflow-y-auto">
              <div class="grid grid-cols-2 gap-6 text-base">
                <div class="bg-slate-50 p-4 border rounded"><div class="text-slate-400 mb-1">执行工单</div><div class="font-bold">{{ props.order?.id }}</div></div>
                <div class="bg-slate-50 p-4 border rounded"><div class="text-slate-400 mb-1">当班操作人</div><div class="font-bold">{{ props.operator?.userName || '系统' }}</div></div>
                <div class="bg-slate-50 p-4 border rounded"><div class="text-slate-400 mb-1">实际开工时间</div><div class="font-bold">{{ startJobForm.time }}</div></div>
                <div class="bg-slate-50 p-4 border rounded"><div class="text-slate-400 mb-1">合格产出总数</div><div class="font-bold text-emerald-600">{{ inboundSummary.good }}</div></div>
                <div class="bg-slate-50 p-4 border rounded"><div class="text-slate-400 mb-1">不良报废总数</div><div class="font-bold text-red-600">{{ inboundSummary.scrap }}</div></div>
              </div>
            </div>
          </Tabs.TabPane>
          <Tabs.TabPane key="2" tab="2. 物料投料核销">
            <div class="h-full p-4 bg-white relative flex-1-table-container">
              <Table :dataSource="scannedMaterials" :columns="materialColumns" size="small" bordered :pagination="false" :scroll="{ y: '100%' }" class="full-height-table" />
            </div>
          </Tabs.TabPane>
          <Tabs.TabPane key="3" tab="3. 开机点检确认">
            <div class="h-full p-4 bg-white relative flex-1-table-container">
              <Table :dataSource="checkData" :columns="checkColumns" size="small" bordered :pagination="false" :scroll="{ y: '100%' }" class="full-height-table">
                <template #bodyCell="{ column, record }"><template v-if="column.dataIndex === 'checked'">{{ record.checked ? '已确认' : '未点' }}</template></template>
              </Table>
            </div>
          </Tabs.TabPane>
          <Tabs.TabPane key="4" tab="4. 工艺参数采集 (EDC)">
            <div class="h-full p-4 bg-white relative flex-1-table-container">
              <Table :dataSource="edcHistoryFlat" :columns="edcColumns" size="small" bordered :pagination="false" :scroll="{ y: '100%' }" class="full-height-table" />
            </div>
          </Tabs.TabPane>
          <Tabs.TabPane key="5" tab="5. 品质检验台账">
            <div class="h-full p-4 bg-white relative flex-1-table-container">
              <Table :dataSource="qcHistory" :columns="qcHistoryColumns" size="small" bordered :pagination="false" :scroll="{ y: '100%' }" class="full-height-table">
                <template #bodyCell="{ column, record }"><template v-if="column.dataIndex === 'action'">-</template></template>
              </Table>
            </div>
          </Tabs.TabPane>
        </Tabs>
        <div class="shrink-0 flex justify-end gap-4 mt-4 pt-4 border-t">
          <Button size="large" @click="eDhrVisible=false">关闭预览</Button>
          <Button size="large" type="primary" class="bg-indigo-600 font-bold" @click="message.success('报告已打印出档！');eDhrVisible=false;">打印报告并归档</Button>
        </div>
      </div>
    </Modal>

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

/* eDHR 综合报告 Tabs 样式 */
:deep(.custom-dhr-tabs > .ant-tabs-content-holder) { flex: 1; overflow: hidden; display: flex; flex-direction: column;}
:deep(.custom-dhr-tabs > .ant-tabs-content-holder > .ant-tabs-content) { flex: 1; height: 100%; display: flex; flex-direction: column;}
:deep(.custom-dhr-tabs > .ant-tabs-content-holder > .ant-tabs-content > .ant-tabs-tabpane) { flex: 1; height: 100%; display: flex; flex-direction: column;}
:deep(.custom-dhr-tabs .ant-tabs-nav) { margin-bottom: 0 !important; padding-top: 12px; padding-left: 16px; background-color: #f8fafc; }
:deep(.custom-dhr-tabs .ant-tabs-tab) { border-bottom: none !important; font-weight: bold; }

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
