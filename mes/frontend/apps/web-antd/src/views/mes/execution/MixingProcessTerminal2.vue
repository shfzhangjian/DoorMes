<script lang="ts" setup>
import { ref, watch, computed, reactive, onBeforeUnmount } from 'vue';
import {
  Button, Card, Tag, Input, InputNumber, Table, Tabs, Dropdown, Menu,
  message, Modal, Select, Divider, Drawer, Timeline, Form, Pagination, DatePicker, Radio, Descriptions
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
// 核心状态机 & 切换工单监听 (完美保留特色逻辑)
// ==================================================================================
const workOrderStatus = ref(props.order?.status || '未开工');
const batchNo = ref(props.order?.batch || '');

const activeTabPre = ref('1');
const activeTabMix = ref('1');
const activeTabQc = ref('首检(FAI)');
const qcTabsList = ['首检(FAI)', '过程抽检(IPQC)', '完工检(FQC)'];

// 监听外部工单切换，自动控制当前面板信息
watch(() => props.order, (newOrder) => {
  workOrderStatus.value = newOrder.status || '未开工';
  batchNo.value = newOrder.batch || '';
  activeTabPre.value = '1';
  activeTabMix.value = '1';
  message.info(`已切换至工单: ${newOrder.id}，当前状态: ${workOrderStatus.value}`);
}, { deep: true });

function jumpToStep(step: string) {
  if (workOrderStatus.value === '未开工') return message.warning('请先完成开工确认！');
  workOrderStatus.value = step;
}

// 🌟 历史记录系统 (保留并升级样式)
const historyDrawerVisible = ref(false);
const historyDetailVisible = ref(false);
const activeHistoryDetail = ref<any>(null);
const historyList = ref<any[]>([]);

function recordHistory(action: string, detail: string, snapshot: any = null) {
  historyList.value.unshift({ time: new Date().toLocaleString(), status: workOrderStatus.value, action, user: props.operator?.userName || '系统', detail, snapshot });
}

function viewHistoryDetail(log: any) {
  if (!log.snapshot) return message.info('该记录无详细表单快照');
  activeHistoryDetail.value = log;
  historyDetailVisible.value = true;
}

// ==================================================================================
// 1. 未开工逻辑 (大屏 UI 升级)
// ==================================================================================
const startJobTime = ref<string | null>(null);

function handleStartJob() {
  if (!startJobTime.value) return message.warning('请先选择实际开工时间！');
  batchNo.value = 'MIX-' + new Date().getTime().toString().slice(-8);

  recordHistory('开始作业', `实际开工时间: ${startJobTime.value}, 生成批次: ${batchNo.value}`, {
    type: 'start', time: startJobTime.value, batchNo: batchNo.value
  });

  workOrderStatus.value = '作业前准备';
  activeTabPre.value = '1';
  message.success('已进入作业前准备状态！');
}

// ==================================================================================
// 2. 作业前准备 (环境 + 设备点检)
// ==================================================================================
const envParams = ref(Array.from({ length: 4 }).map((_, i) => ({
  id: `E0${i+1}`, name: i===0?'环境温度':(i===1?'环境湿度':`辅料仓温${i+1}`),
  ref: i===0?'22-26':(i===1?'40-60':'10-20'), unit: i===0?'℃':(i===1?'%RH':'℃'),
  source: '传感器直采', val: null as number|null, status: '未检', time: '--'
})));

const envColumns: TableColumnsType = [
  { title: '参数编号', dataIndex: 'id', width: 100 }, { title: '参数名称', dataIndex: 'name', width: 150 },
  { title: '参考标准', dataIndex: 'ref', width: 120 }, { title: '单位', dataIndex: 'unit', width: 80, align: 'center' },
  { title: '数据来源', dataIndex: 'source', width: 120 }, { title: '实际参数值', dataIndex: 'val', width: 200, align: 'center' },
  { title: '状态', dataIndex: 'status', width: 100, align: 'center' }, { title: '更新时间', dataIndex: 'time', width: 150 }
];

const deviceChecks = ref(Array.from({ length: 5 }).map((_, i) => ({
  device: '主配料搅拌罐', item: i===0?'搅拌轴清洁':(i===1?'真空泵压力':'阀门气密性'), req: '运行正常/无残留', result: '未点', remark: '', time: '--'
})));

const deviceColumns: TableColumnsType = [
  { title: '设备名称', dataIndex: 'device', width: 150 }, { title: '点检项目', dataIndex: 'item', width: 200 },
  { title: '规范要求', dataIndex: 'req', width: 200 }, { title: '点检结果', dataIndex: 'result', width: 150, align: 'center' },
  { title: '备注', dataIndex: 'remark' }, { title: '确认时间', dataIndex: 'time', width: 150 }
];

function fetchEnvValues() {
  envParams.value.forEach(p => p.val = parseFloat((Math.random()*10 + 20).toFixed(1)));
  message.success('已自动获取最新环境参数！');
}
function confirmEnvNormal() {
  envParams.value.forEach(p => { if(p.val) { p.status = '正常'; p.time = new Date().toLocaleTimeString(); } });
}
function handleQuickCheck() {
  deviceChecks.value.forEach(d => { if(d.result === '未点') { d.result = '正常'; d.time = new Date().toLocaleTimeString(); } });
  message.success('设备一键点检完成！');
}

const isPreCheckComplete = computed(() => envParams.value.every(p => p.status !== '未检') && deviceChecks.value.every(d => d.result !== '未点'));

function finishPreCheck() {
  recordHistory('完成开机准备', '环境及设备检查全部确认正常', { type: 'pre_check', envData: clone(envParams.value), deviceData: clone(deviceChecks.value) });
  workOrderStatus.value = '配方投料';
  activeTabMix.value = '1';
}

// ==================================================================================
// 3. 配方投料核对 (大尺寸改造)
// ==================================================================================
const recipePages = ref([{ id: 1, code: '', verifier: '', time: '--', status: '未核对', remark: '', items: [] as any[] }]);
const currentRecipePage = ref(1);
const curRecipe = computed(() => recipePages.value[currentRecipePage.value - 1]);
const unverifiedRecipeCount = computed(() => recipePages.value.filter(r => r.status !== '已核对').length);

const recipeColumns: TableColumnsType = [
  { title: '配方编码', dataIndex: 'code', width: 120 }, { title: '配方名称', dataIndex: 'name', width: 180 },
  { title: '标准用量', dataIndex: 'std', align: 'center', width: 120 }, { title: '单位', dataIndex: 'unit', width: 80, align: 'center' },
  { title: '允差', dataIndex: 'tol', width: 100, align: 'center' }, { title: '实际用量', dataIndex: 'actual', width: 180, align: 'center' },
  { title: '备注说明', dataIndex: 'remark' }
];

watch(() => curRecipe.value.code, (val) => {
  if (val && val.length >= 6) {
    curRecipe.value.items = Array.from({ length: 4 }).map((_, i) => ({ code: `M${val.slice(-3)}-${i}`, name: `核心聚氨酯树脂${i+1}`, std: 100, unit: 'kg', tol: '±1%', actual: null, remark: '' }));
    curRecipe.value.status = '未核对';
    message.success('配方清单加载成功！');
  }
});

function verifyRecipe() { curRecipe.value.status = '已核对'; curRecipe.value.verifier = props.operator?.userName || '系统'; curRecipe.value.time = new Date().toLocaleTimeString(); }
function addRecipe() { recipePages.value.push({ id: recipePages.value.length + 1, code: '', verifier: '', time: '--', status: '未核对', remark: '', items: [] }); currentRecipePage.value = recipePages.value.length; }
function deleteRecipe() {
  if (recipePages.value.length === 1) return message.warning('必须保留至少一个配方！');
  recipePages.value.splice(currentRecipePage.value - 1, 1);
  currentRecipePage.value = Math.max(1, currentRecipePage.value - 1);
}

const materialForm = ref({ stockCode: '', verifier: '', time: '--', status: '未核对', remark: '' });
const materialLogs = ref<any[]>([]);

const materialColumns: TableColumnsType = [
  { title: '物料编码', dataIndex: 'code', width: 120 }, { title: '物料名称', dataIndex: 'name', width: 150 },
  { title: '配比(%)', dataIndex: 'ratio', align: 'center', width: 100 }, { title: '单位', dataIndex: 'unit', width: 80, align: 'center' },
  { title: '库存条码', dataIndex: 'stockId', width: 180 }, { title: '可领余量', dataIndex: 'stockQty', align: 'center', width: 120 },
  { title: '实际投料量', dataIndex: 'pickQty', width: 180, align: 'center' }, { title: '备注', dataIndex: 'remark' }
];

watch(() => materialForm.value.stockCode, (val) => {
  if (val && val.length >= 6) {
    materialLogs.value.unshift({ code: 'MT-'+val.slice(-4), name: '溶剂DMF', ratio: '40%', unit: 'kg', stockId: val, stockQty: 500, pickQty: null, remark: '' });
    materialForm.value.status = '未核对';
    materialForm.value.stockCode = '';
    message.success('扫码成功，投料记录已追加！');
  }
});

function verifyMaterial() { materialForm.value.status = '已核对'; materialForm.value.verifier = props.operator?.userName || '系统'; materialForm.value.time = new Date().toLocaleTimeString(); }
const isRecipeMixComplete = computed(() => unverifiedRecipeCount.value === 0 && materialForm.value.status === '已核对');

function finishRecipeMix() {
  recordHistory('配方投料完成', '配方及物料领用已全部核对完毕', { type: 'recipe_mix', recipes: clone(recipePages.value), materials: clone(materialLogs.value) });
  workOrderStatus.value = '作业中';
}

// ==================================================================================
// 4. 品质检验 (新增：动态列质检台)
// ==================================================================================
const inspectForm = reactive({ mode: 'segment', segmentPos: '整缸提取', length: null as number|null, barcode: '' });
const pendingInspectItems = ref<{ id: string, detail: string }[]>([]);
const pendingInspectColumns: TableColumnsType = [ { title: '待检胶液对象', dataIndex: 'detail' }, { title: '操作', dataIndex: 'action', width: 100, align: 'center' } ];

function addPendingItem() {
  const detail = `批次抽样: ${batchNo.value} - 搅拌提取`;
  pendingInspectItems.value.unshift({ id: `TSK-${Date.now()}`, detail });
  message.success('已生成待检样本！');
}

const wbVisible = ref(false);
const wbTask = reactive({ type: '', targetStr: '', targets: [] as string[], standard: null as string | null, summaryResult: 'A级', summaryRemark: '', items: [] as any[] });
const standardOptions = [{ value: 'STD-MIX-01', label: '聚氨酯浆料通用检验标准 (V1.0)' }];

const getDynamicColumns = (targets: string[]): TableColumnsType => {
  const baseColumns: TableColumnsType = [
    { title: '序号', dataIndex: 'id', width: 60, align: 'center', fixed: 'left' }, { title: '检验项目', dataIndex: 'inspectionItem', width: 140, fixed: 'left' },
    { title: '标准要求', dataIndex: 'standardDesc', width: 150 }, { title: '取样数', dataIndex: 'sampleSize', width: 90, align: 'center' }
  ];
  const dynamicColumns: TableColumnsType = targets.map((t, idx) => ({ title: `实测值-${idx + 1}`, dataIndex: `val_${idx}`, minWidth: 140, align: 'center' }));
  const endColumns: TableColumnsType = [
    { title: 'MAX', dataIndex: 'operatorMax', width: 80, align: 'center' }, { title: 'MIN', dataIndex: 'operatorMin', width: 80, align: 'center' },
    { title: 'AVG', dataIndex: 'operatorAvg', width: 80, align: 'center' }, { title: '结论', dataIndex: 'operatorResult', width: 100, align: 'center', fixed: 'right' }
  ];
  return [...baseColumns, ...dynamicColumns, ...endColumns];
};

function loadStandardItems() {
  const templateItems = [
    { id: 1, inspectionItem: '浆料黏度', itemType: 'QUANTITATIVE', standardDesc: '2000-3000 cps', sampleSize: 3, minValueLimit: 2000, maxValueLimit: 3000, operatorValues: [], operatorMax: null, operatorMin: null, operatorAvg: null, operatorResult: '-' },
    { id: 2, inspectionItem: '固含量', itemType: 'QUANTITATIVE', standardDesc: '25-30 %', sampleSize: 2, minValueLimit: 25, maxValueLimit: 30, operatorValues: [], operatorMax: null, operatorMin: null, operatorAvg: null, operatorResult: '-' },
    { id: 3, inspectionItem: '外观状态', itemType: 'QUALITATIVE', standardDesc: '均匀无结块/气泡', sampleSize: 1, operatorValues: [], operatorResult: '-' }
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
  { title: '检验批号', dataIndex: 'id', width: 140 }, { title: '类型', dataIndex: 'type', width: 140 }, { title: '提交时间', dataIndex: 'completeTime', width: 160, align: 'center' },
  { title: '综合结论', dataIndex: 'judgment', width: 100, align: 'center' }, { title: '操作', dataIndex: 'action', width: 120, align: 'center', fixed: 'right' }
];

function submitWorkbench() {
  const isComplete = wbTask.items.every(i => i.operatorResult !== '-');
  if (!isComplete) return message.warning('请完成所有检测指标录入！');
  qcHistory.value.unshift({
    id: `RPT-${Date.now().toString().slice(-6)}`, target: wbTask.targetStr, targets: clone(wbTask.targets), type: wbTask.type,
    completeTime: new Date().toLocaleTimeString(), judgment: wbTask.summaryResult, ngCount: wbTask.items.filter(i => i.operatorResult === 'NG').length,
    standard: wbTask.standard, remark: wbTask.summaryRemark, items: clone(wbTask.items)
  });
  pendingInspectItems.value = []; wbVisible.value = false; activeTabQc.value = '检验历史台账';
  message.success('胶料质检报告生成并归档！');
}

const detailVisible = ref(false); const detailRecord = ref<any>(null);
function openDetail(record: any) { detailRecord.value = record; detailVisible.value = true; }

// ==================================================================================
// 5. 完工作业 (重量键盘录入)
// ==================================================================================
const yieldInput = ref('');
function pressKey(key: string) { if (key === 'C') yieldInput.value = ''; else if (key === 'DEL') yieldInput.value = yieldInput.value.slice(0, -1); else yieldInput.value += key; }

const inboundModalVisible = ref(false);
const finishForm = reactive({ weight: null as number|null });
const inboundSummary = reactive({ total: 0, good: 0, scrap: 0, yieldRate: '0%' });

function handleFinishJob() {
  let qty = parseInt(yieldInput.value);
  if (!qty || qty <= 0) return message.warning('请输入有效的产出重量！');
  finishForm.weight = qty;
  inboundModalVisible.value = true;
}

function submitFinish() {
  if (!finishForm.weight) return message.warning('必须输入重量');
  inboundSummary.good += finishForm.weight;
  inboundSummary.total = inboundSummary.good;
  inboundSummary.yieldRate = '100%'; // 简化配料良率展示
  inboundModalVisible.value = false; yieldInput.value = '';
  previewInboundReceipt();
}

const inboundPreviewVisible = ref(false); const inboundPreviewHtml = ref('');
function previewInboundReceipt() {
  inboundPreviewHtml.value = `<html><head><style>body{font-family:sans-serif;font-size:14px;line-height:1.6;padding:20px;}h2{text-align:center;border-bottom:2px solid #000;padding-bottom:10px;}.info{display:grid;grid-template-columns:1fr 1fr;gap:10px;margin-top:20px;}.qty{font-size:24px;font-weight:bold;text-align:center;border:2px dashed #000;padding:20px;margin:20px 0;}</style></head><body><h2>配料/混料产出流转单</h2><div class="info"><div>工单号：${props.order?.id || '测试工单'}</div><div>胶料批次：${batchNo.value}</div><div>交接人：${props.operator?.userName || '系统'}</div><div>时间：${new Date().toLocaleString()}</div></div><div class="qty">产出净重: ${finishForm.weight} kg</div></body></html>`;
  inboundPreviewVisible.value = true;
}
function printFromPreview() { message.success('胶液桶条码及流转单已发送打印！'); inboundPreviewVisible.value = false; }

// ==================================================================================
// 6. e-DHR 综合大报告
// ==================================================================================
const eDhrVisible = ref(false);
const eDhrData = computed(() => ({
  order: props.order, process: props.process, operator: props.operator, startTime: startJobTime.value,
  summary: inboundSummary, materials: materialLogs.value, checks: [...envParams.value, ...deviceChecks.value],
  qc: qcHistory.value.map(q => ({ time: q.completeTime, type: q.type, mode: '系统/人工', detail: q.target, result: q.judgment }))
}));

const abnormalModalVisible = ref(false); const abnormalForm = reactive({ reason: null, desc: '' });
function handleMachineMenuClick({ key }: { key: string }) {
  if (key === 'pause') { Modal.confirm({ title: '暂停作业', content: '确认挂起搅拌罐？', onOk() { emit('pause'); } }); }
  else if (key === 'abnormal') { abnormalModalVisible.value = true; }
  else if (key === 'handover') { emit('handover'); }
}
function submitAbnormalStop() { workOrderStatus.value = '异常停工'; abnormalModalVisible.value = false; }

</script>

<template>
  <div class="flex flex-col absolute inset-0 bg-slate-100 overflow-hidden">

    <header class="flex h-16 shrink-0 items-center justify-between border-b bg-white px-6 shadow-sm z-10">
      <div class="flex items-center gap-8">
        <div v-for="label in ['未开工', '作业前准备', '配方投料', '作业中', '品质检验', '完成作业']" :key="label"
             @click="jumpToStep(label)"
             :class="['flex items-center gap-2 font-black text-lg transition-colors cursor-pointer hover:text-indigo-500',
             workOrderStatus === label ? 'text-indigo-700 scale-105' : 'text-slate-400']">
          <IconifyIcon :icon="workOrderStatus === label ? 'lucide:circle-dot' : 'lucide:circle'" class="text-xl" />
          <span>{{ label }}</span>
        </div>
      </div>
      <div class="flex items-center gap-4">
        <span v-if="batchNo" class="text-lg font-mono font-black text-indigo-700 bg-indigo-50 px-4 py-1.5 rounded-lg border border-indigo-200">混料缸批次: {{ batchNo }}</span>
      </div>
    </header>

    <main class="flex-1 flex flex-col min-h-0 p-4 overflow-hidden relative">

      <div v-show="workOrderStatus === '未开工'" class="flex-1 flex items-center justify-center">
        <Card class="w-[600px] border-none shadow-xl rounded-3xl p-8 bg-white text-center">
          <div class="mb-6 flex h-24 w-24 items-center justify-center rounded-full bg-indigo-50 text-indigo-600 mx-auto shadow-inner">
            <IconifyIcon icon="lucide:play-circle" class="text-6xl" />
          </div>
          <h2 class="text-3xl font-black text-slate-800 mb-4">等待配料开工确认</h2>
          <p class="mb-8 text-lg text-slate-500 font-bold">操作员: {{ operator?.userName || '系统' }} | 执行工单: {{ order?.id || '测试单' }}</p>
          <div class="text-left bg-slate-50 p-6 rounded-2xl border border-slate-200 mb-8 shadow-sm">
            <div class="text-lg font-black text-slate-700 mb-4">选择实际开缸时间：</div>
            <DatePicker v-model:value="startJobTime" show-time value-format="YYYY-MM-DD HH:mm:ss" :inputReadOnly="true" size="large" class="w-full h-14 text-xl font-bold" />
          </div>
          <Button type="primary" size="large" block class="h-16 rounded-xl bg-indigo-600 font-black text-2xl shadow-lg" @click="handleStartJob">确认接单并开缸</Button>
        </Card>
      </div>

      <div v-show="workOrderStatus === '作业前准备'" class="flex-1 flex flex-col bg-white border-2 border-slate-200 rounded-2xl shadow-md overflow-hidden">
        <div class="flex items-center bg-slate-50 border-b-2 border-slate-200 px-4 shrink-0">
          <Tabs v-model:activeKey="activeTabPre" type="capsule" class="compact-tabs py-3">
            <Tabs.TabPane key="1" tab="温湿度等环境点检" /><Tabs.TabPane key="2" tab="主配料缸设备确认" />
          </Tabs>
        </div>
        <div class="flex-1-table-container p-4">
          <Table v-if="activeTabPre === '1'" :columns="envColumns" :dataSource="envParams" :pagination="false" :scroll="{ y: '100%' }" class="full-height-table text-lg border border-slate-200 rounded-xl">
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'val'"><InputNumber v-model:value="record.val" size="large" class="w-32 font-bold" @change="record.status='未检'" placeholder="数值"/></template>
              <template v-if="column.dataIndex === 'status'"><Tag :color="record.status==='正常'?'success':'error'" class="text-base py-1 px-3 font-bold">{{ record.status }}</Tag></template>
            </template>
          </Table>
          <Table v-if="activeTabPre === '2'" :columns="deviceColumns" :dataSource="deviceChecks" :pagination="false" :scroll="{ y: '100%' }" class="full-height-table text-lg border border-slate-200 rounded-xl">
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'result'">
                <Select v-model:value="record.result" size="large" class="w-32 font-bold" @change="record.time=new Date().toLocaleTimeString()"><Select.Option value="未点">未点</Select.Option><Select.Option value="正常">正常</Select.Option></Select>
              </template>
              <template v-if="column.dataIndex === 'remark'"><Input v-model:value="record.remark" size="large" /></template>
            </template>
          </Table>
        </div>
      </div>

      <div v-show="workOrderStatus === '配方投料'" class="flex-1 flex flex-col bg-white border-2 border-slate-200 rounded-2xl shadow-md overflow-hidden">
        <Tabs v-model:activeKey="activeTabMix" tabPosition="left" class="w-full h-full custom-left-tabs">
          <Tabs.TabPane key="1" tab="1. 配方单核对">
            <div class="flex flex-col h-full bg-slate-50 p-4 gap-4">
              <div class="flex items-center gap-6 p-4 bg-white border border-slate-200 rounded-xl shadow-sm shrink-0">
                <div class="flex items-center text-lg"><span class="font-black text-slate-600 mr-4">调取配方编码:</span><Input.Search v-model:value="curRecipe.code" size="large" placeholder="扫入配方..." class="w-64 h-12 custom-huge-input-start" enter-button="查询" /></div>
                <Divider type="vertical" class="h-10 bg-slate-200" />
                <div class="text-lg"><span class="font-bold text-slate-500 mr-2">复核人:</span><span class="font-black">{{ curRecipe.verifier || '--' }}</span></div>
                <div class="text-lg"><span class="font-bold text-slate-500 mr-2">状态:</span><Tag :color="curRecipe.status==='已核对'?'success':'processing'" class="text-base py-1 px-3 font-bold">{{ curRecipe.status }}</Tag></div>
              </div>
              <div class="flex-1 bg-white border border-slate-200 rounded-xl shadow-sm flex flex-col min-h-0 relative">
                <div class="flex items-center justify-between p-3 border-b shrink-0 bg-slate-50">
                  <div class="text-lg font-black text-red-600 flex items-center gap-2"><IconifyIcon icon="lucide:alert-circle" /> 尚有 {{ unverifiedRecipeCount }} 页未核对</div>
                  <Pagination v-model:current="currentRecipePage" :total="recipePages.length" :pageSize="1" />
                </div>
                <div class="flex-1-table-container">
                  <Table :columns="recipeColumns" :dataSource="curRecipe.items" :pagination="false" :scroll="{ y: '100%' }" class="full-height-table text-lg">
                    <template #empty><div class="py-16 text-slate-400 font-bold text-center">暂无配方明细，请在上方扫码调取</div></template>
                    <template #bodyCell="{ column, record }">
                      <template v-if="column.dataIndex === 'actual'"><InputNumber v-model:value="record.actual" size="large" class="w-32 font-bold text-center" placeholder="实投量" /></template>
                      <template v-if="column.dataIndex === 'remark'"><Input v-model:value="record.remark" size="large" placeholder="备注" /></template>
                    </template>
                  </Table>
                </div>
              </div>
            </div>
          </Tabs.TabPane>
          <Tabs.TabPane key="2" tab="2. 物料防错核销">
            <div class="flex flex-col h-full bg-slate-50 p-4 gap-4">
              <div class="flex items-center gap-6 p-4 bg-white border border-slate-200 rounded-xl shadow-sm shrink-0">
                <div class="flex-1 flex items-center text-lg"><span class="font-black text-slate-600 mr-4">扫描物料防错:</span><Input.Search v-model:value="materialForm.stockCode" size="large" placeholder="扫入实物桶条码核销防错..." class="max-w-2xl h-12 custom-huge-input-start" enter-button="确认加料" /></div>
                <div class="text-lg"><span class="font-bold text-slate-500 mr-2">整体防错状态:</span><Tag :color="materialForm.status==='已核对'?'success':'error'" class="text-base py-1 px-4 font-bold">{{ materialForm.status }}</Tag></div>
              </div>
              <div class="flex-1 bg-white border border-slate-200 rounded-xl shadow-sm flex flex-col min-h-0 relative">
                <div class="flex-1-table-container">
                  <Table :columns="materialColumns" :dataSource="materialLogs" :pagination="false" :scroll="{ y: '100%' }" class="full-height-table text-lg">
                    <template #empty><div class="py-16 text-slate-400 font-bold text-center">请扫码投入需要消耗的辅料及树脂</div></template>
                    <template #bodyCell="{ column, record }">
                      <template v-if="column.dataIndex === 'pickQty'"><InputNumber v-model:value="record.pickQty" :max="record.stockQty" size="large" class="w-32 font-bold text-center text-indigo-700" placeholder="实扣量"/></template>
                      <template v-if="column.dataIndex === 'remark'"><Input v-model:value="record.remark" size="large" /></template>
                    </template>
                  </Table>
                </div>
              </div>
            </div>
          </Tabs.TabPane>
        </Tabs>
      </div>

      <div v-show="workOrderStatus === '作业中'" class="flex-1 flex flex-col items-center justify-center bg-white rounded-3xl border border-slate-200 shadow-inner">
        <div class="relative flex items-center justify-center w-64 h-64 mb-8">
          <IconifyIcon icon="lucide:loader-circle" class="text-indigo-600 text-[180px] animate-spin absolute opacity-20" />
          <IconifyIcon icon="lucide:flask-conical" class="text-indigo-600 text-[100px] z-10" />
        </div>
        <h2 class="text-4xl font-black text-slate-800 mb-4 tracking-widest">搅拌缸正在自动配料混料中...</h2>
        <p class="text-xl text-slate-500 font-bold">在此期间您可以前往「品质检验」录入抽检数据</p>
      </div>

      <div v-show="workOrderStatus === '品质检验'" class="flex-1 flex flex-col bg-white border-2 border-slate-200 rounded-2xl overflow-hidden shadow-md">
        <Tabs v-model:activeKey="activeTabQc" tabPosition="left" class="w-full h-full custom-left-tabs">
          <Tabs.TabPane v-for="qcType in qcTabsList" :key="qcType" :tab="qcType">
            <div class="flex h-full bg-slate-50">
              <div class="w-[450px] p-6 flex flex-col border-r border-slate-200 bg-white shrink-0">
                <div class="text-xl font-black text-slate-800 mb-6 border-b pb-4 text-indigo-600">录入 {{qcType}} 胶液样本</div>
                <Form layout="vertical">
                  <Form.Item label="抽取模式">
                    <Radio.Group v-model:value="inspectForm.mode" button-style="solid" class="w-full flex" size="large">
                      <Radio.Button value="segment" class="flex-1 text-center font-bold">缸内取样</Radio.Button>
                    </Radio.Group>
                  </Form.Item>
                </Form>
                <Button type="primary" size="large" class="w-full h-14 text-xl font-bold mt-2" @click="addPendingItem">确认提取检验项 👉</Button>
              </div>
              <div class="flex-1 p-6 flex flex-col min-h-0 relative">
                <div class="text-xl font-black text-indigo-900 mb-4">待送检样本区</div>
                <div class="flex-1-table-container border border-slate-200 rounded-xl bg-white shadow-sm overflow-hidden">
                  <Table :columns="pendingInspectColumns" :dataSource="pendingInspectItems" :pagination="false" :scroll="{ y: '100%' }" class="full-height-table text-base absolute inset-0">
                    <template #empty><div class="py-16 text-slate-400 font-bold text-lg text-center">尚未提取胶液</div></template>
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
          <h2 class="text-3xl font-black text-slate-800 m-0 border-l-8 border-indigo-600 pl-4">缸体出料与称重报工</h2>
          <div class="flex bg-slate-100 p-1.5 rounded-xl border border-slate-200">
            <div class="px-6 py-2 rounded-lg font-bold cursor-pointer text-xl bg-white text-indigo-600 shadow-md">数字键盘录入重量</div>
          </div>
        </div>

        <div class="flex-1 flex gap-8 min-h-0">
          <div class="flex-1 flex flex-col bg-slate-50 border-2 border-slate-200 rounded-3xl p-8 justify-center items-end shadow-inner relative">
            <div class="text-slate-400 text-2xl font-bold mb-4">当前出缸净重 (kg)</div>
            <span v-if="!yieldInput" class="text-5xl font-black text-slate-300 font-mono animate-pulse">使用数字键盘</span>
            <span v-else class="text-[150px] font-black text-slate-800 font-mono leading-none">{{ yieldInput }}</span>
          </div>

          <div class="w-[450px] shrink-0 h-full">
            <div class="bg-white p-6 rounded-3xl border border-slate-200 shadow-lg h-full">
              <div class="grid grid-cols-3 gap-4 h-full">
                <div v-for="key in ['1','2','3','4','5','6','7','8','9','C','0','DEL']" :key="key" @click="pressKey(key)" class="bg-slate-50 border-2 border-slate-100 rounded-2xl flex items-center justify-center text-5xl font-black text-slate-700 cursor-pointer hover:bg-indigo-50 active:bg-indigo-600 active:text-white transition-colors" :class="key === 'C' ? 'text-red-500' : (key === 'DEL' ? 'text-amber-500' : '')">{{ key === 'DEL' ? '⬅' : key }}</div>
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
        <Button class="h-14 px-6 font-black text-lg text-indigo-600 bg-indigo-50 border-2 border-indigo-200 rounded-xl hover:bg-indigo-100 shadow-sm" @click="historyDrawerVisible = true"><IconifyIcon icon="lucide:history" class="mr-2 text-2xl" /> 工序追踪快照系统</Button>
        <Dropdown placement="topRight">
          <Button class="h-14 px-6 font-black text-lg text-slate-600 border-2 border-slate-300 rounded-xl hover:text-indigo-600 shadow-sm ml-2"><IconifyIcon icon="lucide:settings-2" class="mr-2 text-2xl" /> 机台操作</Button>
          <template #overlay>
            <Menu @click="handleMachineMenuClick" class="w-56 p-2 rounded-xl border-2 border-slate-200 shadow-xl">
              <Menu.Item key="pause"><div class="flex items-center text-amber-600 font-bold text-lg py-2"><IconifyIcon icon="lucide:pause-circle" class="mr-3 text-2xl"/> 挂起搅拌缸</div></Menu.Item>
              <Menu.Item key="abnormal"><div class="flex items-center text-red-600 font-bold text-lg py-2"><IconifyIcon icon="lucide:siren" class="mr-3 text-2xl"/> 异常拉灯提报</div></Menu.Item>
              <Menu.Divider class="my-2" />
              <Menu.Item key="handover"><div class="flex items-center text-emerald-600 font-bold text-lg py-2"><IconifyIcon icon="lucide:users" class="mr-3 text-2xl"/> 发起交接班</div></Menu.Item>
            </Menu>
          </template>
        </Dropdown>
      </div>

      <div class="flex gap-4 items-center">
        <template v-if="workOrderStatus === '作业前准备'">
          <template v-if="activeTabPre === '1'"><Button class="h-14 px-6 text-xl font-bold rounded-xl border-2 border-indigo-300 text-indigo-600 bg-indigo-50" @click="fetchEnvValues">自动抓取</Button><Button class="h-14 px-6 text-xl font-bold rounded-xl border-2 border-emerald-300 text-emerald-600 bg-emerald-50" @click="confirmEnvNormal">一键确认正常</Button></template>
          <template v-if="activeTabPre === '2'"><Button class="h-14 px-6 text-xl font-bold rounded-xl border-2 border-indigo-300 text-indigo-600 bg-indigo-50" @click="handleQuickCheck">一键设备点检</Button></template>
          <Divider type="vertical" class="h-8 bg-slate-300 mx-2" />
          <Button type="primary" class="h-14 px-12 text-xl font-black rounded-xl bg-indigo-600 shadow-lg" :disabled="!isPreCheckComplete" @click="finishPreCheck">确认参数并流转配方</Button>
        </template>

        <template v-if="workOrderStatus === '配方投料'">
          <template v-if="activeTabMix === '1'"><Button danger size="large" class="h-14 font-bold rounded-xl" @click="deleteRecipe">删页</Button><Button class="h-14 px-6 text-xl font-bold rounded-xl border-2 border-indigo-300 text-indigo-600 bg-indigo-50" @click="addRecipe">加页</Button><Button class="h-14 px-6 text-xl font-bold rounded-xl border-2 border-emerald-300 text-emerald-600 bg-emerald-50" @click="verifyRecipe">单页核对完毕</Button></template>
          <template v-if="activeTabMix === '2'"><Button class="h-14 px-6 text-xl font-bold rounded-xl border-2 border-emerald-300 text-emerald-600 bg-emerald-50" @click="verifyMaterial">物料核对无误</Button></template>
          <Divider type="vertical" class="h-8 bg-slate-300 mx-2" />
          <Button type="primary" class="h-14 px-12 text-xl font-black rounded-xl bg-indigo-600 shadow-lg" :disabled="!isRecipeMixComplete" @click="finishRecipeMix">启动搅拌缸执行</Button>
        </template>

        <template v-if="workOrderStatus === '作业中'">
          <Button type="primary" class="h-14 px-12 text-xl font-black rounded-xl bg-blue-600 shadow-lg" @click="workOrderStatus = '品质检验'">提取胶液送检</Button>
          <Button type="primary" class="h-14 px-12 text-xl font-black rounded-xl bg-emerald-600 shadow-lg border-none shadow-emerald-200" @click="workOrderStatus = '完成作业'">执行出缸报工</Button>
        </template>

        <template v-if="workOrderStatus === '品质检验' && activeTabQc !== '检验历史台账'">
          <Button type="primary" class="h-14 px-16 text-2xl font-black rounded-xl bg-indigo-600 shadow-xl shadow-indigo-200" @click="openQualityWorkbench">🔬 弹出质检并填写综合报告</Button>
        </template>

        <template v-if="workOrderStatus === '完成作业'">
          <Button type="primary" class="h-14 px-10 text-xl font-black rounded-xl bg-blue-600 shadow-lg mr-6" @click="eDhrVisible = true">
            <IconifyIcon icon="lucide:file-bar-chart-2" class="mr-2" /> 综合大报告
          </Button>
          <Button class="w-[200px] h-14 text-xl font-black rounded-xl bg-emerald-500 hover:bg-emerald-600 border-none text-white shadow-xl shadow-emerald-200" @click="handleFinishJob">✅ 胶液重量确认出缸</Button>
        </template>
      </div>
    </footer>

    <Modal v-model:open="wbVisible" :title="`胶液质检执行工作台 - ${wbTask.type}`" width="1350px" centered :footer="false" :destroyOnClose="true">
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
          <div class="font-bold text-slate-700 mr-4 text-base">本次胶液综合判定：</div>
          <Radio.Group v-model:value="wbTask.summaryResult" button-style="solid" size="large" class="font-bold">
            <Radio.Button value="A级">A级 (优)</Radio.Button>
            <Radio.Button value="B级">B级 (良)</Radio.Button>
            <Radio.Button value="C级">C级 (中)</Radio.Button>
            <Radio.Button value="不合格">不合格 (NG)</Radio.Button>
          </Radio.Group>
          <Divider type="vertical" class="h-8 mx-6 bg-slate-200" />
          <div class="font-bold text-slate-700 mr-4 text-base">说明/备注：</div>
          <Input v-model:value="wbTask.summaryRemark" size="large" placeholder="请输入检验说明..." class="flex-1 h-10 text-base" />
        </div>

        <div class="flex justify-end gap-4 mt-4 pt-4 border-t shrink-0">
          <Button size="large" @click="wbVisible=false" class="font-bold">放弃保存</Button>
          <Button size="large" type="primary" class="bg-indigo-600 font-bold px-8" @click="submitWorkbench">生成胶液判定单并归档</Button>
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

    <Modal v-model:open="inboundModalVisible" title="✅ 混料完工与入库确认" @ok="submitFinish" :width="500" centered>
      <Form layout="vertical" class="mt-4 p-4"><div class="mb-6 text-sm font-bold text-indigo-700 bg-indigo-50 p-4 rounded-xl border border-indigo-200">系统将依据您在左侧键盘录入的重量进行入库。</div><Form.Item label="产出胶液净重 (kg)" required><InputNumber v-model:value="finishForm.weight" class="w-full h-14 text-2xl font-black" /></Form.Item></Form>
    </Modal>
    <Modal v-model:open="inboundPreviewVisible" title="📦 生产流转出缸单预览" :width="800" :footer="false" centered><div class="p-4 bg-slate-100 rounded-xl mb-4"><iframe :srcdoc="inboundPreviewHtml" class="w-full h-[400px] bg-white border border-slate-300 rounded"></iframe></div><div class="flex justify-end"><Button size="large" type="primary" class="bg-indigo-600 font-bold" @click="printFromPreview">打印入库条码</Button></div></Modal>
    <ComprehensiveReport v-model:visible="eDhrVisible" :reportData="eDhrData" />

    <Drawer v-model:open="historyDrawerVisible" title="工序追踪体系快照" placement="right" width="900">
      <div class="flex h-full gap-6">
        <div class="w-80 border-r border-slate-200 pr-4 overflow-y-auto">
          <Timeline class="mt-4">
            <Timeline.Item v-for="(log, idx) in historyList" :key="idx" :color="idx === 0 ? 'blue' : 'gray'">
              <div class="font-black mb-1 text-slate-700 text-lg cursor-pointer hover:text-indigo-600 transition-colors" @click="viewHistoryDetail(log)">
                {{ log.action }} <IconifyIcon icon="lucide:external-link" class="text-indigo-400" v-if="log.snapshot" />
              </div>
              <div class="text-sm font-bold text-slate-500 mb-1">{{ log.time }}</div>
              <div class="text-sm text-slate-400">{{ log.user }}</div>
            </Timeline.Item>
          </Timeline>
        </div>
        <div class="flex-1 overflow-y-auto bg-slate-50 p-6 rounded-2xl border border-slate-200 shadow-inner">
          <div v-if="!activeHistoryDetail" class="text-center text-slate-400 font-bold text-lg mt-32">点击左侧时间轴节点查看历史数据快照</div>
          <div v-else>
            <h3 class="text-2xl font-black text-indigo-900 border-b-2 border-indigo-100 pb-3 mb-6">{{ activeHistoryDetail.action }} - 数据快照</h3>
            <div class="text-base font-bold text-indigo-700 mb-6 bg-indigo-50 p-4 border border-indigo-100 rounded-xl">{{ activeHistoryDetail.detail }}</div>

            <template v-if="activeHistoryDetail.snapshot?.type === 'pre_check'">
              <div class="font-black text-lg text-slate-700 mb-3 border-l-4 border-emerald-500 pl-3">环境参数快照</div>
              <Table :columns="envColumns" :dataSource="activeHistoryDetail.snapshot.envData" :pagination="false" size="small" class="vben-schema-table mb-6 border border-slate-200 bg-white" bordered>
                <template #bodyCell="{ column, record }"><template v-if="column.dataIndex === 'status'"><Tag :color="record.status==='正常'?'success':'error'" class="font-bold">{{ record.status }}</Tag></template></template>
              </Table>
              <div class="font-black text-lg text-slate-700 mb-3 border-l-4 border-emerald-500 pl-3">设备点检快照</div>
              <Table :columns="deviceColumns" :dataSource="activeHistoryDetail.snapshot.deviceData" :pagination="false" size="small" class="vben-schema-table border border-slate-200 bg-white" bordered />
            </template>

            <template v-if="activeHistoryDetail.snapshot?.type === 'recipe_mix'">
              <div class="font-black text-lg text-slate-700 mb-3 border-l-4 border-indigo-500 pl-3">已核对配方列表</div>
              <div v-for="page in activeHistoryDetail.snapshot.recipes" :key="page.id" class="mb-6">
                <Table :columns="recipeColumns" :dataSource="page.items" :pagination="false" size="small" class="vben-schema-table border border-slate-200 bg-white" bordered />
              </div>
              <div class="font-black text-lg text-slate-700 mb-3 border-l-4 border-indigo-500 pl-3">物料领用核销记录</div>
              <Table :columns="materialColumns" :dataSource="activeHistoryDetail.snapshot.materials" :pagination="false" size="small" class="vben-schema-table border border-slate-200 bg-white" bordered />
            </template>
          </div>
        </div>
      </div>
    </Drawer>

    <Modal v-model:open="abnormalModalVisible" title="🚨 停机异常反馈" :width="500" :footer="false" centered>
      <div class="p-6"><Select v-model:value="abnormalForm.reason" class="w-full h-12 text-lg mb-4" :options="[{label: '设备机械故障', value: '设备机械故障'}, {label: '品质异常', value: '品质异常'}]" placeholder="停机原因" /><Input.TextArea v-model:value="abnormalForm.desc" :rows="4" placeholder="异常描述..." class="mb-6 text-lg p-3" /><Button type="primary" danger block class="h-14 text-xl font-bold" @click="submitAbnormalStop">确认挂起</Button></div>
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
