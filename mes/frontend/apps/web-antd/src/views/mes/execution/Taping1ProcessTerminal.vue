<script lang="ts" setup>
import { ref, computed, reactive, onBeforeUnmount, watch } from 'vue';
import {
  Button, Card, Tag, Input, InputNumber, Table, Tabs, Dropdown, Menu,
  message, Modal, Select, Divider, Form, DatePicker, Radio, Checkbox, Descriptions, Switch
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
const activeTabPre = ref('1');
const activeTabQc = ref('首检(FAI)');
const qcTabsList = ['首检(FAI)', '自检(SELF)', '过程抽检(IPQC)', '完工检(FQC)'];

function jumpToStep(step: string) {
  if (workOrderStatus.value === '未开工') return message.warning('请先完成扫码领用并开工！');
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
// 1. 未开工逻辑 (扫码领用精磨膜)
// ==================================================================================
const rollScanCode = ref('');
const incomingRoll = ref<any>(null);
const parentBatchNo = ref('');
const scannedMaterials = ref<any[]>([]);
const startJobModalVisible = ref(false);
const startJobForm = reactive({ time: dayjs().format('YYYY-MM-DD HH:mm:ss') });

const materialColumns: TableColumnsType = [
  { title: '原材料批次条码', dataIndex: 'batch', width: 220 },
  { title: '物料/产品名称', dataIndex: 'product', minWidth: 200 },
  { title: '来源', dataIndex: 'source', width: 150 },
  { title: '母卷长度 (m)', dataIndex: 'length', width: 150, align: 'center' },
  { title: '扫码时间', dataIndex: 'time', width: 150 },
  { title: '操作', dataIndex: 'action', width: 80, align: 'center' }
];

function handleScanMixing() {
  if (!rollScanCode.value.trim()) return message.warning('请扫描前序原材料(精磨膜)条码！');

  parentBatchNo.value = 'FILM-' + rollScanCode.value.slice(-5);
  incomingRoll.value = {
    id: Date.now(),
    code: rollScanCode.value,
    batch: parentBatchNo.value,
    product: 'T01_Film_精磨膜',
    length: 100,
    source: '半成品库',
    time: new Date().toLocaleTimeString()
  };
  scannedMaterials.value = [incomingRoll.value];
  rollScanCode.value = '';
  message.success(`原材料 ${parentBatchNo.value} 扫描就绪`);
}

function removeMaterial() {
  scannedMaterials.value = [];
  incomingRoll.value = null;
  parentBatchNo.value = '';
}

function openStartJobModal() {
  if (scannedMaterials.value.length === 0) return message.warning('🚨 请先扫描领用前序原材料卷！');
  startJobForm.time = dayjs().format('YYYY-MM-DD HH:mm:ss');
  startJobModalVisible.value = true;
}

function confirmStartJob() {
  if(!startJobForm.time) return message.warning('请填写开工时间');
  startJobModalVisible.value = false;
  setTimeout(() => {
    workOrderStatus.value = '作业前准备';
    message.success('粘胶开机成功！进入准备阶段。');
    startQTime();
  }, 200);
}

// ==================================================================================
// 2. 作业前准备 (环境 + 粘胶参数)
// ==================================================================================
const envParams = ref([
  { id: 'E01', name: '环境温度 (℃)', ref: '20-25', val: null as number|null|string, checked: false },
  { id: 'E02', name: '环境湿度 (%RH)', ref: '40-60', val: null as number|null|string, checked: false }
]);
const envColumns: TableColumnsType = [
  { title: '编号', dataIndex: 'id', width: 100 }, { title: '参数名称', dataIndex: 'name' },
  { title: '参考标准', dataIndex: 'ref', width: 150 }, { title: '实际值录入', dataIndex: 'val', width: 200, align: 'center' },
  { title: '确认状态', dataIndex: 'checked', width: 150, align: 'center' }
];

const startupParams = ref([
  { item: '贴合间隙 (mm)', req: '0.05-0.10', val: null as number|null|string },
  { item: '压辊温度 (℃)', req: '60-80', val: null as number|null|string },
  { item: '运行线速 (m/min)', req: '10-20', val: null as number|null|string },
  { item: '放卷恒定电流 (A)', req: '2.5', val: null as number|null|string },
  { item: '收卷恒定电流 (A)', req: '3.0', val: null as number|null|string }
]);
const startupColumns: TableColumnsType = [
  { title: '粘胶工艺参数', dataIndex: 'item' }, { title: '规范要求', dataIndex: 'req', width: 200 },
  { title: '实际设定/记录值', dataIndex: 'val', width: 250, align: 'center' }
];

function fetchEnvValues() {
  envParams.value[0].val = 23; envParams.value[0].checked = true;
  envParams.value[1].val = 52; envParams.value[1].checked = true;
  message.success('环境数据获取成功！');
}
function checkAllNormal() { envParams.value.forEach(d => d.checked = true); message.success('一键确认完毕！'); }

const isPreCheckComplete = computed(() => envParams.value.every(d => d.checked) && startupParams.value.every(p => p.val !== null && String(p.val).trim() !== ''));
function finishPreCheck() { workOrderStatus.value = '作业中'; }

// ==================================================================================
// 3. 作业中 (粘胶特色：持续质量厚度检验)
// ==================================================================================
const thicknessForm = ref({ val: null as number|null, pos: '左侧' });
const thicknessLogs = ref<any[]>([]);

const thicknessColumns: TableColumnsType = [
  { title: '检验时间', dataIndex: 'time', width: 180 },
  { title: '测量位置', dataIndex: 'pos', width: 120 },
  { title: '粘胶层厚度 (mm)', dataIndex: 'val', align: 'center', width: 200 },
  { title: '判定结论', dataIndex: 'status', width: 150, align: 'center' },
  { title: '操作人', dataIndex: 'user' }
];

function handleAddThickness() {
  if (!thicknessForm.value.val) return message.warning('请输入测量厚度');

  const val = Number(thicknessForm.value.val);
  const isOk = val >= 0.13 && val <= 0.17;

  thicknessLogs.value.unshift({
    id: Date.now(),
    time: new Date().toLocaleTimeString(),
    pos: thicknessForm.value.pos,
    val: val.toFixed(3),
    status: isOk ? '合格' : '超差',
    user: props.operator.userName || '系统'
  });

  if (!isOk) message.error('🚨 厚度超差异常！请立即注意调整贴合间隙！');
  else message.success('抽检厚度记录已追加');

  thicknessForm.value.val = null;
}

// ==================================================================================
// 4. 品质检验 (标准化动态列 + 单选判定)
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
const wbTask = reactive({
  type: '', targetStr: '', targets: [] as string[], standard: null as string | null,
  summaryResult: 'A级', summaryRemark: '', items: [] as any[]
});
const standardOptions = [{ value: 'STD-TAPE-01', label: '粘胶工艺检验规范 (V2.0)' }];

const getDynamicColumns = (targets: string[]): TableColumnsType => {
  const baseColumns: TableColumnsType = [
    { title: '序号', dataIndex: 'id', width: 60, align: 'center', fixed: 'left' },
    { title: '检测项目', dataIndex: 'inspectionItem', width: 140, fixed: 'left' },
    { title: '标准要求', dataIndex: 'standardDesc', width: 150 },
    { title: 'AQL样本数', dataIndex: 'sampleSize', width: 110, align: 'center' },
  ];
  const dynamicColumns: TableColumnsType = targets.map((target, idx) => ({ title: `实际值-${idx + 1}`, dataIndex: `val_${idx}`, minWidth: 140, align: 'center' }));
  const endColumns: TableColumnsType = [
    { title: 'MAX', dataIndex: 'operatorMax', width: 80, align: 'center' },
    { title: 'MIN', dataIndex: 'operatorMin', width: 80, align: 'center' },
    { title: 'AVG', dataIndex: 'operatorAvg', width: 80, align: 'center' },
    { title: '单项结论', dataIndex: 'operatorResult', width: 100, align: 'center', fixed: 'right' }
  ];
  return [...baseColumns, ...dynamicColumns, ...endColumns];
};

function loadStandardItems() {
  const templateItems = [
    { id: 1, inspectionItem: '粘胶外观', itemType: 'QUALITATIVE', standardDesc: '无气泡/褶皱/溢胶', sampleSize: 2, operatorValues: [], operatorResult: '-' },
    { id: 2, inspectionItem: '初粘力测试', itemType: 'QUANTITATIVE', standardDesc: '≥8.0 N/25mm', sampleSize: 3, minValueLimit: 8.0, maxValueLimit: 999, operatorValues: [], operatorMax: null, operatorMin: null, operatorAvg: null, operatorResult: '-' },
    { id: 3, inspectionItem: '剥离强度', itemType: 'QUANTITATIVE', standardDesc: '10-15 N/cm', sampleSize: 3, minValueLimit: 10.0, maxValueLimit: 15.0, operatorValues: [], operatorMax: null, operatorMin: null, operatorAvg: null, operatorResult: '-' }
  ];
  wbTask.items = clone(templateItems);
  wbTask.items.forEach((it: any) => { it.operatorValues = new Array(wbTask.targets.length).fill(null); });
}

function handleValueChange(row: any) {
  const validValues = row.operatorValues.filter((v: any) => v !== undefined && v !== null && v !== '');
  if (validValues.length === 0) {
    row.operatorMax = null; row.operatorMin = null; row.operatorAvg = null; row.operatorResult = '-'; return;
  }
  if (row.itemType === 'QUANTITATIVE') {
    const numVals = validValues.map(Number).filter((n:any) => !isNaN(n));
    if(numVals.length > 0) {
      row.operatorMax = Math.max(...numVals).toFixed(3);
      row.operatorMin = Math.min(...numVals).toFixed(3);
      row.operatorAvg = (numVals.reduce((a:number, b:number) => a + b, 0) / numVals.length).toFixed(3);
      const isOk = row.operatorMax <= row.maxValueLimit && row.operatorMin >= row.minValueLimit;
      row.operatorResult = isOk ? 'OK' : 'NG';
    } else { row.operatorResult = '-'; }
  } else {
    const hasNg = validValues.includes('NG') || validValues.includes('异常');
    row.operatorResult = hasNg ? 'NG' : 'OK';
  }
}

function openQualityWorkbench() {
  if (pendingInspectItems.value.length === 0) return message.warning('当前待检队列为空！');
  wbTask.type = activeTabQc.value;
  wbTask.targets = pendingInspectItems.value.map(i => i.detail);
  wbTask.targetStr = wbTask.targets.join(' ; ');
  wbTask.standard = null;
  wbTask.summaryResult = 'A级';
  wbTask.summaryRemark = '';
  wbTask.items = [];
  wbVisible.value = true;
}

const qcHistory = ref<any[]>([]);
const qcHistoryColumns: TableColumnsType = [
  { title: '报告单号/批次', dataIndex: 'id', width: 140 }, { title: '检测类型', dataIndex: 'type', width: 140 },
  { title: '提交时间', dataIndex: 'submitTime', width: 120, align: 'center' }, { title: '完成时间', dataIndex: 'completeTime', width: 120, align: 'center' },
  { title: '包含样本列数', key: 'sampleCount', width: 120, align: 'center' }, { title: '综合结论', dataIndex: 'judgment', width: 100, align: 'center' },
  { title: '操作', dataIndex: 'action', width: 120, align: 'center', fixed: 'right' }
];

function submitWorkbench() {
  const isComplete = wbTask.items.every(i => i.operatorResult !== '-');
  if (!isComplete) return message.warning('请完成表格中所有检测指标的录入与判定！');
  const ngCount = wbTask.items.filter(i => i.operatorResult === 'NG').length;
  qcHistory.value.unshift({
    id: `RPT-${Date.now().toString().slice(-6)}`, target: wbTask.targetStr, targets: clone(wbTask.targets), type: wbTask.type,
    submitTime: new Date(Date.now() - 300000).toLocaleTimeString(), completeTime: new Date().toLocaleTimeString(),
    judgment: wbTask.summaryResult, ngCount: ngCount, standard: wbTask.standard, remark: wbTask.summaryRemark, items: clone(wbTask.items)
  });
  pendingInspectItems.value = [];
  wbVisible.value = false;
  message.success('检验报告已成功提交并生成综合单据存入台账！');
  activeTabQc.value = '检验历史台账';
}

const detailVisible = ref(false);
const detailRecord = ref<any>(null);
function openDetail(record: any) { detailRecord.value = record; detailVisible.value = true; }

// ==================================================================================
// 5. 完成作业 (大尺寸键盘 + 粘胶特色弹窗确认宽幅/米数)
// ==================================================================================
const trackMode = ref<'BATCH' | 'PIECE'>('BATCH');
const yieldInput = ref('');
const generatedBarcodes = ref<{ sn: string; time: string }[]>([]);

function generateSinglePiece() { generatedBarcodes.value.unshift({ sn: `SN${Date.now().toString().slice(-6)}${Math.floor(Math.random()*100)}`, time: new Date().toLocaleTimeString() }); }
function removePiece(index: number) { generatedBarcodes.value.splice(index, 1); }
function pressKey(key: string) { if (key === 'C') yieldInput.value = ''; else if (key === 'DEL') yieldInput.value = yieldInput.value.slice(0, -1); else yieldInput.value += key; }

const finishModalVisible = ref(false);
const finishScanCode = ref('');
const finishForm = ref({ width: null as number|null, length: null as number|null, warehouse: '半成品库' });
const inboundSummary = reactive({ total: 0, good: 0, scrap: 0, yieldRate: '0%' });

// 模拟扫码直接带出胶板宽幅和米数
watch(finishScanCode, (val) => {
  if (val && val.length >= 6) {
    message.loading('解析胶板条码...', 0.5).then(() => {
      finishForm.value.width = 1.25;
      finishForm.value.length = 98.5;
      message.success('已自动解析宽幅与米数');
      finishScanCode.value = '';
    });
  }
});

function handleFinishJob(type: 'GOOD' | 'SCRAP') {
  let qty = trackMode.value === 'BATCH' ? parseInt(yieldInput.value) : generatedBarcodes.value.length;
  if (!qty || qty <= 0) return message.warning('录入数量无效！');

  if (type === 'SCRAP') {
    inboundSummary.scrap += qty;
    inboundSummary.total = inboundSummary.good + inboundSummary.scrap;
    message.success(`记录成功: 报废 ${qty} m/件！`);
    yieldInput.value = ''; generatedBarcodes.value = [];
  } else {
    finishForm.value.length = qty; // 默认把主界面的数量赋给米数
    finishModalVisible.value = true;
  }
}

function submitFinish() {
  if (!finishForm.value.width || !finishForm.value.length) return message.warning('宽幅与米数不能为空');

  // 将确定的米数计入良品台账
  inboundSummary.good += finishForm.value.length;
  inboundSummary.total = inboundSummary.good + inboundSummary.scrap;
  inboundSummary.yieldRate = inboundSummary.total > 0 ? ((inboundSummary.good / inboundSummary.total) * 100).toFixed(1) + '%' : '0%';

  finishModalVisible.value = false;
  yieldInput.value = ''; generatedBarcodes.value = [];
  previewInboundReceipt();
}

const inboundPreviewVisible = ref(false); const inboundPreviewHtml = ref('');
function previewInboundReceipt() {
  inboundPreviewHtml.value = `<html><head><style>body{font-family:sans-serif;font-size:14px;line-height:1.6;padding:20px;}h2{text-align:center;border-bottom:2px solid #000;padding-bottom:10px;}.info{display:grid;grid-template-columns:1fr 1fr;gap:10px;margin-top:20px;}.qty{font-size:24px;font-weight:bold;text-align:center;border:2px dashed #000;padding:20px;margin:20px 0;}</style></head><body><h2>粘胶(1) 完工入库单</h2><div class="info"><div>工单号：${props.order?.id || '测试工单'}</div><div>目标仓库：${finishForm.value.warehouse}</div><div>交接人：${props.operator?.userName || '系统'}</div><div>时间：${new Date().toLocaleString()}</div></div><div class="qty">入库胶板规格: 宽 ${finishForm.value.width}m × 长 ${finishForm.value.length}m</div></body></html>`;
  inboundPreviewVisible.value = true;
}
function printFromPreview() { message.success('流转单据已打印！'); inboundPreviewVisible.value = false; }

// ==================================================================================
// 6. 综合大报告 eDHR
// ==================================================================================
const eDhrVisible = ref(false);
const eDhrData = computed(() => ({
  order: props.order, process: props.process, operator: props.operator, startTime: startJobForm.time,
  summary: inboundSummary, materials: scannedMaterials.value, checks: envParams.value,
  qc: qcHistory.value.map(q => ({ time: q.completeTime, type: q.type, mode: '系统/人工', detail: q.target, result: q.judgment }))
}));

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
        <div v-for="label in ['未开工', '作业前准备', '作业中', '品质检验', '完成作业']" :key="label"
             @click="jumpToStep(label)"
             :class="['flex items-center gap-2 font-black text-lg transition-colors cursor-pointer hover:text-indigo-500',
             workOrderStatus === label ? 'text-indigo-700 scale-105' : 'text-slate-400']">
          <IconifyIcon :icon="workOrderStatus === label ? 'lucide:circle-dot' : 'lucide:circle'" class="text-xl" />
          <span>{{ label }}</span>
        </div>
      </div>
      <div class="flex items-center gap-4">
        <span v-if="parentBatchNo" class="text-lg font-mono font-black text-indigo-700 bg-indigo-50 px-4 py-1.5 rounded-lg border border-indigo-200">当前母卷: {{ parentBatchNo }}</span>
      </div>
    </header>

    <main class="flex-1 flex flex-col min-h-0 p-4 overflow-hidden relative">

      <div v-show="workOrderStatus === '未开工'" class="flex-1 flex flex-col h-full bg-white rounded-3xl shadow-sm border border-slate-200 p-6 overflow-hidden">
        <div class="shrink-0 mb-6 flex items-center gap-6">
          <div class="flex-1 max-w-3xl">
            <div class="text-xl font-black text-slate-800 mb-2 flex items-center gap-2"><IconifyIcon icon="lucide:scan-face" /> 扫码领用 (前序精磨产出卷)</div>
            <Input.Search v-model:value="rollScanCode" size="large" placeholder="扫入前序流转原材料条码..." class="h-14 text-xl font-mono custom-huge-input-start" @search="handleScanMixing" enter-button="确认装载" />
          </div>
        </div>
        <div class="flex-1-table-container border border-slate-200 rounded-xl">
          <Table :columns="materialColumns" :dataSource="scannedMaterials" :pagination="false" :scroll="{ y: '100%' }" class="full-height-table text-lg">
            <template #empty><div class="py-16 flex flex-col items-center text-slate-400"><IconifyIcon icon="lucide:layers" class="text-[60px] mb-4 opacity-50" /><span class="text-xl font-bold">暂无原材料数据</span></div></template>
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'length'"><span class="text-emerald-600 font-bold">{{ record.length }} m</span></template>
              <template v-if="column.dataIndex === 'action'"><Button type="text" danger class="bg-red-50 hover:bg-red-100" @click="removeMaterial"><IconifyIcon icon="lucide:trash-2" class="text-xl" /></Button></template>
            </template>
          </Table>
        </div>
      </div>

      <div v-show="workOrderStatus === '作业前准备'" class="flex-1 flex flex-col bg-white border-2 border-slate-200 rounded-2xl shadow-md overflow-hidden">
        <div class="flex items-center bg-slate-50 border-b-2 border-slate-200 px-4 shrink-0">
          <Tabs v-model:activeKey="activeTabPre" type="capsule" class="compact-tabs py-3">
            <Tabs.TabPane key="1" tab="环境点检" /><Tabs.TabPane key="2" tab="粘胶工艺参数设定" />
          </Tabs>
        </div>
        <div class="flex-1-table-container p-4">
          <Table v-if="activeTabPre === '1'" :columns="envColumns" :dataSource="envParams" :pagination="false" :scroll="{ y: '100%' }" class="full-height-table text-lg border border-slate-200 rounded-xl">
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'val'"><InputNumber v-model:value="record.val" size="large" class="w-40 font-bold" @change="record.checked=false" placeholder="录入实测值"/></template>
              <template v-if="column.dataIndex === 'checked'"><Checkbox v-model:checked="record.checked" class="scale-150 transform translate-x-2" /></template>
            </template>
          </Table>
          <Table v-if="activeTabPre === '2'" :columns="startupColumns" :dataSource="startupParams" :pagination="false" :scroll="{ y: '100%' }" class="full-height-table text-lg border border-slate-200 rounded-xl">
            <template #bodyCell="{ column, record }"><template v-if="column.dataIndex === 'val'"><InputNumber v-model:value="record.val" class="w-48 h-10 text-xl font-black text-indigo-700" placeholder="录入参数值" /></template></template>
          </Table>
        </div>
      </div>

      <div v-show="workOrderStatus === '作业中'" class="flex-1 flex flex-col gap-4 min-h-0">
        <Card class="bg-white border-slate-200 shadow-sm shrink-0 rounded-2xl">
          <div class="flex items-center gap-8">
            <div><span class="text-sm text-slate-400 font-bold">正处理母卷</span><div class="font-mono font-black text-blue-700 text-3xl">{{ parentBatchNo || '未载入' }}</div></div>
            <Divider type="vertical" class="h-12 bg-slate-200" />
            <div><span class="text-sm text-slate-400 font-bold">工序要求</span><div class="font-black text-slate-700 text-2xl">定时记录粘胶层厚度分布</div></div>
          </div>
        </Card>

        <div class="bg-indigo-50 border border-indigo-100 p-5 rounded-2xl flex items-end gap-6 shrink-0 shadow-sm">
          <Form.Item label="测量位置选择" class="mb-0 font-bold text-indigo-900 text-lg">
            <Select v-model:value="thicknessForm.pos" size="large" class="w-48 h-12 text-xl font-bold shadow-sm">
              <Select.Option value="左侧">左侧</Select.Option>
              <Select.Option value="中段">中段</Select.Option>
              <Select.Option value="右侧">右侧</Select.Option>
            </Select>
          </Form.Item>
          <Form.Item label="实测厚度 (mm) [标准: 0.15 ± 0.02]" class="mb-0 flex-1 font-bold text-indigo-900 text-lg max-w-[400px]">
            <InputNumber v-model:value="thicknessForm.val" size="large" class="w-full h-12 text-2xl font-black shadow-sm" placeholder="如: 0.151" :step="0.001" />
          </Form.Item>
          <Button type="primary" size="large" class="bg-indigo-600 font-black px-10 shadow-md h-12 text-xl mb-[1px]" @click="handleAddThickness">
            <IconifyIcon icon="lucide:file-check" class="mr-2" /> 记录品质数据
          </Button>
        </div>

        <div class="flex-1 bg-white border border-slate-200 rounded-2xl overflow-hidden shadow-sm flex flex-col min-h-0 relative">
          <div class="p-3 bg-slate-50 border-b border-slate-200 text-lg font-black text-slate-700 flex items-center gap-2 shrink-0">
            <IconifyIcon icon="lucide:ruler" class="text-indigo-600"/> 粘胶层厚度抽检历史记录表
          </div>
          <div class="flex-1-table-container">
            <Table :columns="thicknessColumns" :dataSource="thicknessLogs" :pagination="false" :scroll="{ y: '100%' }" class="full-height-table text-lg">
              <template #empty><div class="py-16 text-slate-400 font-bold text-lg">暂无厚度检验记录，请定时抽检录入</div></template>
              <template #bodyCell="{ column, record }">
                <template v-if="column.dataIndex === 'val'"><span class="font-mono font-bold text-indigo-700 text-xl">{{ record.val }}</span></template>
                <template v-if="column.dataIndex === 'status'">
                  <Tag :color="record.status === '合格' ? 'success' : 'error'" class="text-lg py-1 px-3 font-bold">{{ record.status }}</Tag>
                </template>
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
                <div class="text-xl font-black text-slate-800 mb-6 border-b pb-4 text-indigo-600">录入 {{qcType}} 对象</div>
                <Form layout="vertical">
                  <Form.Item label="检测模式">
                    <Radio.Group v-model:value="inspectForm.mode" button-style="solid" class="w-full flex" size="large">
                      <Radio.Button value="segment" class="flex-1 text-center font-bold">按批次/段</Radio.Button>
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
                <Button type="primary" size="large" class="w-full h-14 text-xl font-bold mt-2" @click="addPendingItem">加入待检列表 👉</Button>
              </div>

              <div class="flex-1 p-6 flex flex-col min-h-0 relative">
                <div class="text-xl font-black text-indigo-900 mb-4">待送检清单 (共 {{pendingInspectItems.length}} 件)</div>
                <div class="flex-1-table-container border border-slate-200 rounded-xl bg-white shadow-sm overflow-hidden">
                  <Table :columns="pendingInspectColumns" :dataSource="pendingInspectItems" :pagination="false" :scroll="{ y: '100%' }" class="full-height-table text-base absolute inset-0">
                    <template #empty><div class="py-16 text-slate-400 font-bold text-lg text-center">尚未录入，请在左侧加入</div></template>
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
          <h2 class="text-3xl font-black text-slate-800 m-0 border-l-8 border-indigo-600 pl-4">终端过站与报工入库</h2>
          <div class="flex bg-slate-100 p-1.5 rounded-xl border border-slate-200">
            <div @click="trackMode = 'PIECE'" class="px-6 py-2 rounded-lg font-bold cursor-pointer text-xl" :class="trackMode === 'PIECE' ? 'bg-white text-indigo-600 shadow-md' : 'text-slate-500'">扫码模式</div>
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
              <div class="text-slate-400 text-2xl font-bold mb-4">当前录入数量 (件/m)</div>
              <span v-if="!yieldInput" class="text-5xl font-black text-slate-300 font-mono animate-pulse">使用数字键盘</span>
              <span v-else class="text-[150px] font-black text-slate-800 font-mono leading-none">{{ yieldInput }}</span>
            </div>
          </div>

          <div class="w-[450px] shrink-0 h-full">
            <div v-if="trackMode === 'BATCH'" class="bg-white p-6 rounded-3xl border border-slate-200 shadow-lg h-full flex flex-col">
              <div class="grid grid-cols-3 gap-4 flex-1">
                <div v-for="key in ['1','2','3','4','5','6','7','8','9','C','0','DEL']" :key="key" @click="pressKey(key)" class="bg-slate-50 border-2 border-slate-100 rounded-2xl flex items-center justify-center text-5xl font-black text-slate-700 cursor-pointer hover:bg-indigo-50 active:bg-indigo-600 active:text-white transition-colors" :class="key === 'C' ? 'text-red-500' : (key === 'DEL' ? 'text-amber-500' : '')">{{ key === 'DEL' ? '⬅' : key }}</div>
              </div>
            </div>
            <div v-else class="bg-indigo-50 border-2 border-indigo-100 rounded-3xl p-8 flex flex-col items-center justify-center text-center h-full">
              <IconifyIcon icon="lucide:scan" class="text-[100px] text-indigo-300 mb-6" />
              <h3 class="text-2xl font-bold text-indigo-800 mb-4">单件扫码核验入库</h3>
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
          <Button class="h-14 px-6 text-xl font-bold rounded-xl border-2 border-indigo-300 text-indigo-600 bg-indigo-50" v-if="activeTabPre === '1'" @click="fetchEnvValues">获取环境数据</Button>
          <Button class="h-14 px-6 text-xl font-bold rounded-xl border-2 border-emerald-300 text-emerald-600 bg-emerald-50" v-if="activeTabPre === '1'" @click="checkAllNormal">一键确认正常</Button>
          <Divider type="vertical" class="h-8 bg-slate-300 mx-2" v-if="activeTabPre === '1'" />
          <Button type="primary" class="h-14 px-12 text-xl font-black rounded-xl bg-indigo-600 shadow-lg" :disabled="!isPreCheckComplete" @click="finishPreCheck">流转入库</Button>
        </template>

        <template v-if="workOrderStatus === '作业中'">
          <Button type="primary" class="h-14 px-12 text-xl font-black rounded-xl bg-blue-600 shadow-lg" @click="workOrderStatus = '品质检验'">进入送检环节</Button>
        </template>

        <template v-if="workOrderStatus === '品质检验' && activeTabQc !== '检验历史台账'">
          <Button type="primary" class="h-14 px-16 text-2xl font-black rounded-xl bg-indigo-600 shadow-xl shadow-indigo-200" @click="openQualityWorkbench">🔬 弹出质检并填写综合报告</Button>
        </template>

        <template v-if="workOrderStatus === '完成作业'">
          <Button type="primary" class="h-14 px-10 text-xl font-black rounded-xl bg-blue-600 shadow-lg mr-6" @click="eDhrVisible = true">
            <IconifyIcon icon="lucide:file-bar-chart-2" class="mr-2" /> 综合大报告
          </Button>
          <Button class="w-[200px] h-14 text-xl font-black rounded-xl bg-red-500 hover:bg-red-600 border-none text-white shadow-lg" @click="handleFinishJob('SCRAP')">💥 报废扣除</Button>
          <Button class="w-[200px] h-14 text-xl font-black rounded-xl bg-emerald-500 hover:bg-emerald-600 border-none text-white shadow-xl shadow-emerald-200" @click="handleFinishJob('GOOD')">✅ 完工交接入库</Button>
        </template>
      </div>
    </footer>

    <Modal v-model:open="startJobModalVisible" title="确认开机" :width="500" centered>
      <Form layout="vertical" class="mt-6">
        <div class="mb-6 bg-indigo-50 text-indigo-700 p-4 rounded-xl text-sm font-bold border border-indigo-100">已载入精磨原材料 {{ incomingRoll?.batch }}，请确认粘胶开机时间。</div>
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

    <Modal v-model:open="inboundModalVisible" title="✅ 粘胶完工与入库确认" @ok="submitFinish" :width="700" centered>
      <Form layout="vertical" class="mt-4 p-4">
        <div class="mb-6 text-sm font-bold text-indigo-700 bg-indigo-50 p-4 rounded-xl border border-indigo-200">
          <div class="flex items-center gap-3 mb-2 text-lg text-indigo-900"><IconifyIcon icon="lucide:scan-barcode" /> 快速录入</div>
          <Input v-model:value="finishScanCode" placeholder="请扫描产出胶板上的二维码以自动提取规格参数..." size="large" class="w-full font-mono text-lg h-12" />
        </div>
        <div class="grid grid-cols-2 gap-6">
          <Form.Item label="胶板宽幅 (m)" required>
            <InputNumber v-model:value="finishForm.width" size="large" class="w-full h-14 text-2xl font-black" placeholder="手动录入" />
          </Form.Item>
          <Form.Item label="收卷总米数 (m)" required>
            <InputNumber v-model:value="finishForm.length" size="large" class="w-full h-14 text-2xl font-black" placeholder="手动录入" />
          </Form.Item>
        </div>
        <Form.Item label="流向目标仓库" class="mt-4">
          <Select v-model:value="finishForm.warehouse" size="large" class="h-12 text-lg font-bold" disabled>
            <Select.Option value="半成品库">半成品库</Select.Option>
          </Select>
        </Form.Item>
      </Form>
    </Modal>

    <Modal v-model:open="inboundPreviewVisible" title="📦 生产流转入库单预览" :width="800" :footer="false" centered><div class="p-4 bg-slate-100 rounded-xl mb-4"><iframe :srcdoc="inboundPreviewHtml" class="w-full h-[400px] bg-white border border-slate-300 rounded"></iframe></div><div class="flex justify-end"><Button size="large" type="primary" class="bg-indigo-600 font-bold" @click="printFromPreview">打印单据</Button></div></Modal>

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
