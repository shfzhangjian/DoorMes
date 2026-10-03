<script lang="ts" setup>
import { ref, watch, computed, reactive, onBeforeUnmount } from 'vue';
import {
  Button, Card, Tag, Input, InputNumber, Table, Tabs, Dropdown, Menu,
  message, Modal, Select, Divider, Form, DatePicker, Progress
} from 'ant-design-vue';
import type { TableColumnsType } from 'ant-design-vue';
import { IconifyIcon } from '@vben/icons';
import dayjs, { Dayjs } from 'dayjs';

const props = defineProps(['operator', 'order', 'process']);
const emit = defineEmits(['close', 'abnormal', 'handover', 'pause']);

// ==================================================================================
// 核心状态机定义
// ==================================================================================
const workOrderStatus = ref(props.order?.status === '完成作业' ? '完成作业' : '未开工');
const batchNo = ref(props.order?.batch || '');

// 各视图的子 Tab 状态
const activeTabPre = ref('1');
const activeTabRun = ref('涂台工艺');
const activeTabQc = ref('FAI');

const historyList = ref<any[]>([]);

function recordHistory(action: string, detail: string, snapshot: any = null) {
  historyList.value.unshift({ time: new Date().toLocaleString(), status: workOrderStatus.value, action, user: props.operator?.userName || '系统', detail, snapshot });
}

// 🌟 Q-Time 逻辑机制
const qTimeState = reactive({ active: false, remaining: 1800, timer: null as any, isExpired: false });
function startQTime() {
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
// 1. 未开工状态逻辑
// ==================================================================================
const mixingScanCode = ref('');
const scannedMaterials = ref<any[]>([]);
const startJobModalVisible = ref(false);

// 🌟 [修复点1]：使用原生的 dayjs 对象，不使用 value-format 转换成字符串
const startJobForm = reactive({ time: dayjs() as Dayjs | null });

const materialColumns: TableColumnsType = [
  { title: '配料批次条码', dataIndex: 'mixBatch', width: 220 },
  { title: '物料/产品名称', dataIndex: 'product', minWidth: 200 },
  { title: '标准配重 (kg)', dataIndex: 'weight', width: 150 },
  { title: '配料人员', dataIndex: 'creator', width: 120 },
  { title: '扫码核销时间', dataIndex: 'time', width: 180 },
  { title: '实际投料量 (kg)', dataIndex: 'actualWeight', width: 200, align: 'center' },
  { title: '操作', dataIndex: 'action', width: 100, align: 'center' }
];

function handleScanMixing() {
  const val = mixingScanCode.value.trim();
  if (!val) return message.warning('请先输入或扫描配料码！');

  message.loading('正在溯源配料数据...', 0.5).then(() => {
    scannedMaterials.value.push({
      id: Date.now(),
      mixBatch: 'MB-' + (val.length > 4 ? val.slice(-4) : val) + Math.floor(Math.random()*100),
      product: 'T01_Film_核心聚氨酯胶液',
      weight: 500,
      actualWeight: 500,
      creator: '张配料',
      time: new Date().toLocaleTimeString()
    });
    message.success(`配料桶核销成功！`);
    mixingScanCode.value = '';
  });
}

function removeMaterial(index: number) { scannedMaterials.value.splice(index, 1); }

function openStartJobModal() {
  if (scannedMaterials.value.length === 0) return message.warning('🚨 请至少扫描一桶配料后再开工！');
  startJobForm.time = dayjs(); // 每次打开弹窗刷新时间
  startJobModalVisible.value = true;
}

function confirmStartJob() {
  if (!startJobForm.time) return message.warning('请选择实际开工时间！');

  // 🌟 [修复点2]：先关弹窗，让弹窗动画走完。避开弹窗卸载和底层大表格卸载的冲突
  startJobModalVisible.value = false;

  // 延迟 400ms (大于 Antd 弹窗 300ms 动画)，安全进行底层 DOM 的重构
  setTimeout(() => {
    batchNo.value = 'COAT-' + new Date().getTime().toString().slice(-8);
    workOrderStatus.value = '作业前准备';

    const totalWeight = scannedMaterials.value.reduce((sum, item) => sum + (item.actualWeight || 0), 0);
    recordHistory('湿法线开机', `投料总计: ${totalWeight}kg, 批次: ${batchNo.value}`);
    message.success('开工成功！请执行作业前设备点检与参数设定。');
  }, 400);
}

// ==================================================================================
// 2. 作业前准备
// ==================================================================================
const checkData = ref([
  { type: '环境点检', item: '车间温度 (℃)', req: '22 ± 2 ℃', result: '未点' },
  { type: '环境点检', item: '车间湿度 (%RH)', req: '50 ± 5 %RH', result: '未点' },
  { type: '环境点检', item: '局部洁净度', req: '10000级标准', result: '未点' },
  { type: '设备点检', item: '主轴与牵引电机', req: '无异响、无卡顿', result: '未点' },
  { type: '设备点检', item: '各级导辊表面', req: '无结块、无异物划伤', result: '未点' },
  { type: '安全点检', item: '急停开关测试', req: '动作灵敏有效', result: '未点' }
]);
const checkColumns: TableColumnsType = [
  { title: '类型', dataIndex: 'type', width: 150 }, { title: '点检项目', dataIndex: 'item' },
  { title: '工艺/规范要求', dataIndex: 'req' }, { title: '核对结果', dataIndex: 'result', width: 220 }
];

const startupParams = ref([
  { node: '涂台工艺', item: '刀口间隙', unit: 'mm', std: '0.15 ~ 0.18', val: null as number | null | string },
  { node: '涂台工艺', item: '涂料宽幅设定', unit: 'm', std: '1.20', val: null as number | null | string },
  { node: '涂台工艺', item: '浆料保温温度', unit: '℃', std: '45.0 ± 1', val: null as number | null | string },
  { node: '凝固工艺', item: '一区DMF浓度', unit: '%', std: '15.0', val: null as number | null | string },
  { node: '凝固工艺', item: '一区纯水温度', unit: '℃', std: '30.0', val: null as number | null | string },
  { node: '水洗工艺', item: '喷淋水压', unit: 'MPa', std: '0.3', val: null as number | null | string },
  { node: '烘干工艺', item: '一区烘箱温度', unit: '℃', std: '120.0', val: null as number | null | string },
]);
const startupColumns: TableColumnsType = [
  { title: '工艺节点', dataIndex: 'node', width: 150 }, { title: '工艺参数名称', dataIndex: 'item' },
  { title: '单位', dataIndex: 'unit', width: 100 }, { title: '参考标准', dataIndex: 'std', width: 150 },
  { title: '实际设定值', dataIndex: 'val', width: 220 }
];

const isPreCheckComplete = computed(() => {
  const isCheckDone = checkData.value.every(d => d.result !== '未点');
  const isParamDone = startupParams.value.every(p => p.val !== null && p.val !== undefined && String(p.val).trim() !== '');
  return isCheckDone && isParamDone;
});

function finishPreCheck() {
  workOrderStatus.value = '工艺参数';
  startQTime(); // 开始正式工艺的Q-Time
}

// ==================================================================================
// 3. 工艺参数
// ==================================================================================
const processInputs = reactive({
  '涂台工艺': { level: null as number|null, speed: null as number|null },
  '凝固工艺': { thickness: null as number|null, width: null as number|null },
  '水洗工艺': { peelPos: '区2', waterPress: null as number|null },
  '烘干工艺': { speed: null as number|null, thickness: null as number|null, temp: null as number|null }
});

const runLogs = ref({
  '涂台工艺': [{ time: '08:00', level: 45.0, speed: 12.5, user: '张伟' }],
  '凝固工艺': [{ time: '08:00', thickness: 0.145, width: 1.18, user: '张伟' }],
  '水洗工艺': [{ time: '08:00', peelPos: '区2', waterPress: 0.3, user: '张伟' }],
  '烘干工艺': [{ time: '08:00', speed: 12.5, thickness: 0.140, temp: 120, user: '张伟' }]
});

function submitProcessParams() {
  const currentTab = activeTabRun.value;
  const inputs = processInputs[currentTab as keyof typeof processInputs];

  if (Object.values(inputs).some(v => v === null || v === '')) {
    return message.warning('请将当前节点的所有参数填写完整！');
  }

  const now = new Date().toLocaleTimeString('en-US', { hour12: false, hour: 'numeric', minute: 'numeric' });
  const logEntry = { time: now, user: props.operator?.userName || '张伟', ...inputs };

  runLogs.value[currentTab as keyof typeof runLogs].unshift(logEntry);
  message.success(`${currentTab} 周期参数已记录归档！`);

  Object.keys(inputs).forEach(key => (inputs as any)[key] = null);
  if (currentTab === '水洗工艺') processInputs['水洗工艺'].peelPos = '区2';
}

function finishProcessParams() {
  workOrderStatus.value = '品质检验'; // 流转到新增的检验节点
}

// ==================================================================================
// 4. 品质检验 (全屏大视图 + 动态倒计时交互)
// ==================================================================================
const qcStatus = reactive({
  fai: { state: 'IDLE', countdown: 5 },
  ipqc: { state: 'IDLE', countdown: 5, pos: '前段', qty: 3, records: [] as any[] },
  fqc: { state: 'IDLE', countdown: 5 }
});
let qcInterval: any = null;

// FAI 首检申请
function requestFAI() {
  qcStatus.fai.state = 'INSPECTING';
  qcStatus.fai.countdown = 5;
  qcInterval = setInterval(() => {
    qcStatus.fai.countdown--;
    if (qcStatus.fai.countdown <= 0) {
      clearInterval(qcInterval); qcStatus.fai.state = 'DONE'; message.success('首检判定合格！');
    }
  }, 1000);
}

// IPQC 取样送检
function requestIPQC() {
  qcStatus.ipqc.state = 'INSPECTING';
  qcStatus.ipqc.countdown = 5;
  qcInterval = setInterval(() => {
    qcStatus.ipqc.countdown--;
    if (qcStatus.ipqc.countdown <= 0) {
      clearInterval(qcInterval);
      qcStatus.ipqc.state = 'DONE';
      message.success('抽检判定合格！');
      qcStatus.ipqc.records.unshift({ sn: `NAP-${Date.now().toString().slice(-6)}`, time: new Date().toLocaleTimeString() });
    }
  }, 1000);
}

// FQC 完工检验申请
function requestFQC() {
  qcStatus.fqc.state = 'INSPECTING';
  qcStatus.fqc.countdown = 5;
  qcInterval = setInterval(() => {
    qcStatus.fqc.countdown--;
    if (qcStatus.fqc.countdown <= 0) {
      clearInterval(qcInterval); qcStatus.fqc.state = 'DONE'; message.success('成品质检已完成！');
    }
  }, 1000);
}

function resetIpqc() { qcStatus.ipqc.state = 'IDLE'; }

// ==================================================================================
// 5. 完工作业与报表打印
// ==================================================================================
const finishModalVisible = ref(false);
const finishForm = reactive({ meters: null as number|null, waste: null as number|null });
const inboundSummary = reactive({ total: 0, yieldRate: '0%' });

function handleFinishJob() { finishModalVisible.value = true; }

function submitFinish() {
  if (!finishForm.meters) return message.warning('必须输入收卷良品米数');

  const total = finishForm.meters + (finishForm.waste || 0);
  const yRate = total > 0 ? ((finishForm.meters / total) * 100).toFixed(1) + '%' : '0%';
  inboundSummary.total = total;
  inboundSummary.yieldRate = yRate;

  workOrderStatus.value = '完成作业';
  finishModalVisible.value = false;
  if(qTimeState.timer) clearInterval(qTimeState.timer);

  previewInboundReceipt(); // 直接弹出包含总结的入库单预览
}

// ==================================================================================
// 打印预览 HTML 生成
// ==================================================================================
const inboundPreviewVisible = ref(false); const inboundPreviewHtml = ref('');
const faiPreviewVisible = ref(false); const faiPreviewHtml = ref('');
const ipqcPreviewVisible = ref(false); const ipqcPreviewHtml = ref('');
const fqcPreviewVisible = ref(false); const fqcPreviewHtml = ref('');

function executePrint() { message.success('🖨️ 打印指令已成功下发至默认打印机'); }
function printFromPreview() { executePrint(); }

function previewInboundReceipt() {
  inboundPreviewHtml.value = `
    <html>
      <head><style>@page { size: A5 landscape; margin: 10mm; } body { font-family: sans-serif; font-size: 14px; } h2 { text-align: center; border-bottom: 2px solid #000; padding-bottom: 10px; margin-bottom: 20px;} .info { display: flex; flex-wrap: wrap; justify-content: space-between; margin-bottom: 20px; line-height: 2;} .info div { width: 48%; } .qty { font-size: 24px; font-weight: bold; text-align: center; border: 2px dashed #000; padding: 20px; margin: 20px 0; } .sign { display: flex; justify-content: space-between; margin-top: 50px; font-weight: bold; }</style></head>
      <body><h2>[内部流转] 成品入库交接单</h2><div class="info"><div><strong>工单号码：</strong>${props.order?.id}</div><div><strong>生产机台：</strong>${props.process?.station}</div><div><strong>产品名称：</strong>${props.order?.product}</div><div><strong>交接人员：</strong>${props.operator?.userName || '系统'}</div><div><strong>完工时间：</strong>${new Date().toLocaleString()}</div></div><div class="qty">入库良品: ${finishForm.meters} m</div><div style="border: 2px solid #d97706; background-color: #fffbeb; padding: 15px; margin-bottom: 20px; color: #b45309;"><strong>⚠️ Q-Time 品质管控提醒：</strong><br/><br/>本批次产品必须在 <strong>30分钟内</strong> 送达下一工序投料！</div><div class="sign"><span>产线签核：_________</span><span>仓管签核：_________</span></div></body>
    </html>
  `;
  inboundPreviewVisible.value = true;
}

function previewFAIReport() {
  faiPreviewHtml.value = `<html><head><style>body { font-family: sans-serif; font-size: 13px; line-height: 1.5; } h2{text-align:center;} table { width: 100%; border-collapse: collapse; margin-bottom: 20px; } th, td { border: 1px solid #000; padding: 10px 8px; text-align: center; } th { background-color: #f2f2f2; font-weight: bold; }</style></head><body><h2>首件检验记录报告 (FAI)</h2><p>工单号: ${props.order?.id}</p><table><tr><th>检验项目</th><th>标准要求</th><th>实测状态</th><th>判定</th></tr><tr><td>产品外观</td><td>无气泡、无划伤</td><td>符合要求</td><td style="font-weight:bold;">OK</td></tr><tr><td>涂层厚度</td><td>0.15±0.02mm</td><td>0.155mm</td><td style="font-weight:bold;">OK</td></tr></table></body></html>`;
  faiPreviewVisible.value = true;
}

function previewIPQCReport() {
  ipqcPreviewHtml.value = `<html><head><style>body { font-family: sans-serif; font-size: 13px; line-height: 1.5; } h2{text-align:center;} table { width: 100%; border-collapse: collapse; margin-bottom: 20px; } th, td { border: 1px solid #000; padding: 10px 8px; text-align: center; } th { background-color: #f2f2f2; font-weight: bold; }</style></head><body><h2>过程抽检报告 (IPQC) / NAP标签</h2><p>工单号: ${props.order?.id}</p><table><tr><th>抽检条码 (SN)</th><th>检验项目</th><th>判定</th></tr><tr><td>NAP-${new Date().getTime().toString().slice(-6)}</td><td>常规抽检项全面达标</td><td style="font-weight:bold;">OK</td></tr></table></body></html>`;
  ipqcPreviewVisible.value = true;
}

function previewFQCReport() {
  fqcPreviewHtml.value = `<html><head><style>body { font-family: sans-serif; font-size: 13px; line-height: 1.5; } h2{text-align:center;} table { width: 100%; border-collapse: collapse; margin-bottom: 20px; } th, td { border: 1px solid #000; padding: 10px 8px; text-align: center; } th { background-color: #f2f2f2; font-weight: bold; }</style></head><body><h2>成品出厂质量检验报告 (FQC / COA)</h2><p>工单号: ${props.order?.id}</p><table><tr><th>抽样数</th><th>判定</th></tr><tr><td>5 PCS</td><td style="font-weight:bold;">合格允收</td></tr></table></body></html>`;
  fqcPreviewVisible.value = true;
}

// 底部机台操作
function handleMachineMenuClick({ key }: { key: string }) {
  if (key === 'pause') {
    Modal.confirm({ title: '暂停工单', content: '确认暂停当前工单任务吗？', onOk() { emit('pause'); message.warn('工单已暂停挂起'); } });
  } else if (key === 'abnormal') { emit('abnormal');
  } else if (key === 'handover') { emit('handover'); }
}

onBeforeUnmount(() => { if (qTimeState.timer) clearInterval(qTimeState.timer); if(qcInterval) clearInterval(qcInterval); });
</script>

<template>
  <div class="flex flex-col absolute inset-0 bg-slate-100 overflow-hidden">

    <header class="flex h-16 shrink-0 items-center justify-between border-b bg-white px-6 shadow-sm z-10">
      <div class="flex items-center gap-8">
        <div v-for="label in ['未开工', '作业前准备', '工艺参数', '品质检验', '完成作业']" :key="label"
             :class="['flex items-center gap-2 font-black text-lg transition-colors', workOrderStatus === label ? 'text-indigo-700 scale-105' : 'text-slate-300']">
          <IconifyIcon :icon="workOrderStatus === label ? 'lucide:circle-dot' : 'lucide:circle'" class="text-xl" />
          <span>{{ label }}</span>
        </div>
      </div>
      <div class="flex items-center gap-4">
        <span v-if="batchNo" class="text-lg font-mono font-black text-indigo-700 bg-indigo-50 px-4 py-1.5 rounded-lg border border-indigo-200">批次: {{ batchNo }}</span>
      </div>
    </header>

    <main class="flex-1 flex flex-col min-h-0 p-6 overflow-hidden relative">

      <div v-if="workOrderStatus === '未开工'" class="flex-1 flex flex-col h-full bg-white rounded-3xl shadow-sm border border-slate-200 overflow-hidden p-6">
        <div class="shrink-0 mb-6 border-b border-slate-100 pb-6 flex items-center gap-6">
          <div class="flex-1 max-w-3xl">
            <div class="text-xl font-black text-slate-800 mb-2 flex items-center gap-2"><IconifyIcon icon="lucide:scan-barcode" /> 扫码备料 (配料桶核销)</div>
            <Input.Search v-model:value="mixingScanCode" size="large" placeholder="请使用扫码枪扫入或输入配料桶二维码..." enter-button="添加投料" @search="handleScanMixing" class="h-14 text-xl font-mono custom-huge-input-start" />
          </div>
          <div class="flex-1 bg-indigo-50 border border-indigo-100 rounded-2xl p-4 text-indigo-800 text-sm font-bold">
            💡 操作提示：请扫描线边仓送达的所有配料桶二维码进行物料追溯防错。支持在下方表格中修改每桶的真实投料量。
          </div>
        </div>
        <div class="flex-1 min-h-0 overflow-hidden relative border border-slate-200 rounded-xl">
          <Table :columns="materialColumns" :dataSource="scannedMaterials" :pagination="false" :scroll="{ y: '100%' }" class="vben-schema-table h-full text-lg absolute inset-0">
            <template #empty>
              <div class="py-12 flex flex-col items-center justify-center text-slate-400">
                <IconifyIcon icon="lucide:package-open" class="text-[60px] mb-4 opacity-50" />
                <span class="text-xl font-bold">暂无投料数据，请在上方扫码核销配料桶</span>
              </div>
            </template>
            <template #bodyCell="{ column, record, index }">
              <template v-if="column.dataIndex === 'actualWeight'"><InputNumber v-model:value="record.actualWeight" :min="0" class="w-32 h-10 text-lg font-bold text-indigo-700 text-center" /></template>
              <template v-if="column.dataIndex === 'action'"><Button type="text" danger class="bg-red-50 hover:bg-red-100" @click="removeMaterial(index)"><IconifyIcon icon="lucide:trash-2" class="text-xl" /></Button></template>
            </template>
          </Table>
        </div>
      </div>

      <div v-if="workOrderStatus === '作业前准备'" class="flex-1 flex flex-col bg-white border-2 border-slate-200 rounded-2xl overflow-hidden shadow-md">
        <div class="flex items-center bg-slate-50 border-b-2 border-slate-200 px-4 shrink-0">
          <Tabs v-model:activeKey="activeTabPre" type="capsule" class="compact-tabs py-3">
            <Tabs.TabPane key="1" tab="开机环境与设备点检" />
            <Tabs.TabPane key="2" tab="开机工艺参数打底设定" />
          </Tabs>
        </div>
        <div class="flex-1 overflow-hidden p-4 min-h-0 relative">
          <Table v-if="activeTabPre === '1'" :columns="checkColumns" :dataSource="checkData" :pagination="false" :scroll="{ y: '100%' }" class="vben-schema-table h-full text-lg absolute inset-0 p-4">
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'result'">
                <Select v-model:value="record.result" class="w-32 h-10 text-lg font-bold" :class="record.result === '正常' ? 'text-emerald-600' : (record.result === '未点' ? 'text-slate-400' : 'text-red-600')">
                  <Select.Option value="未点">未点</Select.Option><Select.Option value="正常">正常</Select.Option><Select.Option value="异常">异常</Select.Option>
                </Select>
              </template>
            </template>
          </Table>

          <Table v-if="activeTabPre === '2'" :columns="startupColumns" :dataSource="startupParams" :pagination="false" :scroll="{ y: '100%' }" class="vben-schema-table h-full text-lg absolute inset-0 p-4">
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'val'">
                <InputNumber v-model:value="record.val" class="w-40 h-10 text-xl font-black text-indigo-700" placeholder="录入实际值" />
              </template>
            </template>
          </Table>
        </div>
      </div>

      <div v-if="workOrderStatus === '工艺参数'" class="flex-1 flex flex-col bg-white border-2 border-slate-200 rounded-2xl overflow-hidden shadow-md">
        <Tabs v-model:activeKey="activeTabRun" tabPosition="left" class="w-full h-full custom-left-tabs">
          <Tabs.TabPane v-for="node in ['涂台工艺', '凝固工艺', '水洗工艺', '烘干工艺']" :key="node" :tab="node">
            <div class="flex h-full p-4 gap-6 bg-slate-50">
              <div class="w-1/3 bg-white p-6 rounded-2xl border border-slate-200 shadow-sm flex flex-col">
                <div class="text-xl font-black text-indigo-900 mb-6 border-b pb-3 flex items-center gap-2"><IconifyIcon icon="lucide:edit-3"/> 录入本周期 {{node}} 参数</div>
                <Form layout="vertical" class="flex-1 overflow-y-auto">
                  <template v-if="node === '涂台工艺'">
                    <Form.Item label="料槽液位高度 (CM)"><InputNumber v-model:value="processInputs['涂台工艺'].level" class="w-full h-12 text-xl font-bold" /></Form.Item>
                    <Form.Item label="下料线速 (m/min)"><InputNumber v-model:value="processInputs['涂台工艺'].speed" class="w-full h-12 text-xl font-bold" /></Form.Item>
                  </template>
                  <template v-if="node === '凝固工艺'">
                    <Form.Item label="出槽厚度 (mm)"><InputNumber v-model:value="processInputs['凝固工艺'].thickness" class="w-full h-12 text-xl font-bold" /></Form.Item>
                    <Form.Item label="出槽宽幅 (m)"><InputNumber v-model:value="processInputs['凝固工艺'].width" class="w-full h-12 text-xl font-bold" /></Form.Item>
                  </template>
                  <template v-if="node === '水洗工艺'">
                    <Form.Item label="薄膜剥离位置">
                      <Select v-model:value="processInputs['水洗工艺'].peelPos" class="w-full h-12 text-xl font-bold">
                        <Select.Option value="区1">区1</Select.Option><Select.Option value="区2">区2</Select.Option><Select.Option value="区3">区3</Select.Option>
                      </Select>
                    </Form.Item>
                    <Form.Item label="水洗喷淋水压 (MPa)"><InputNumber v-model:value="processInputs['水洗工艺'].waterPress" class="w-full h-12 text-xl font-bold" /></Form.Item>
                  </template>
                  <template v-if="node === '烘干工艺'">
                    <Form.Item label="烘箱内线速 (m/min)"><InputNumber v-model:value="processInputs['烘干工艺'].speed" class="w-full h-12 text-xl font-bold" /></Form.Item>
                    <Form.Item label="出箱测量厚度 (mm)"><InputNumber v-model:value="processInputs['烘干工艺'].thickness" class="w-full h-12 text-xl font-bold" /></Form.Item>
                    <Form.Item label="出箱测量宽幅 (m)"><InputNumber v-model:value="processInputs['烘干工艺'].width" class="w-full h-12 text-xl font-bold" /></Form.Item>
                  </template>
                </Form>
                <Button type="primary" size="large" class="h-14 text-xl font-bold rounded-xl bg-indigo-600 shadow-md mt-4 shrink-0" @click="submitProcessParams">提交并追加到右侧台账</Button>
              </div>

              <div class="w-2/3 bg-white p-4 rounded-2xl border border-slate-200 shadow-sm flex flex-col min-h-0">
                <div class="text-lg font-bold text-slate-700 mb-4 px-2 shrink-0">📋 {{node}} 历史采集台账</div>
                <div class="flex-1 min-h-0 relative overflow-hidden rounded-xl border border-slate-200">
                  <Table :dataSource="runLogs[node]" :pagination="false" :scroll="{ y: '100%' }" class="vben-schema-table h-full text-base absolute inset-0">
                    <Table.Column title="记录时间" dataIndex="time" width="120" />
                    <template v-if="node === '涂台工艺'"><Table.Column title="液位(CM)" dataIndex="level" /><Table.Column title="线速(m/min)" dataIndex="speed" /></template>
                    <template v-if="node === '凝固工艺'"><Table.Column title="出槽厚度(mm)" dataIndex="thickness" /><Table.Column title="宽幅(m)" dataIndex="width" /></template>
                    <template v-if="node === '水洗工艺'"><Table.Column title="剥离位置" dataIndex="peelPos" /><Table.Column title="水压(MPa)" dataIndex="waterPress" /></template>
                    <template v-if="node === '烘干工艺'"><Table.Column title="线速(m/min)" dataIndex="speed" /><Table.Column title="厚度(mm)" dataIndex="thickness" /><Table.Column title="宽幅(m)" dataIndex="width" /></template>
                    <Table.Column title="操作人" dataIndex="user" width="100" />
                  </Table>
                </div>
              </div>
            </div>
          </Tabs.TabPane>
        </Tabs>
      </div>

      <div v-if="workOrderStatus === '品质检验'" class="flex-1 flex flex-col bg-white border-2 border-slate-200 rounded-2xl overflow-hidden shadow-md">
        <Tabs v-model:activeKey="activeTabQc" tabPosition="left" class="w-full h-full custom-left-tabs">

          <Tabs.TabPane key="FAI" tab="首件检验(FAI)">
            <div class="flex-1 h-full flex flex-col items-center justify-center p-8 bg-slate-50 relative">
              <div v-if="qcStatus.fai.state === 'IDLE'" class="text-center max-w-xl">
                <IconifyIcon icon="lucide:microscope" class="text-[120px] text-orange-400 mb-8 drop-shadow-md" />
                <h2 class="text-3xl font-black text-slate-800 mb-4">触发质量防线：首件检验</h2>
                <p class="text-xl text-slate-500 mb-10 leading-relaxed">工单开机后的前3件产品必须通过品管部首检判定，请点击下方按钮下发指令并将实物送检。</p>
                <Button size="large" type="primary" class="w-full h-16 text-2xl font-bold rounded-2xl bg-orange-500 hover:bg-orange-600 border-none shadow-lg" @click="requestFAI">📡 提交 FAI 首检申请</Button>
              </div>
              <div v-else-if="qcStatus.fai.state === 'INSPECTING'" class="flex flex-col items-center bg-white p-16 rounded-3xl border-2 border-orange-200 shadow-xl w-[500px]">
                <Progress type="circle" :percent="Math.floor(((5 - qcStatus.fai.countdown)/5)*100)" :width="180" strokeColor="#f97316">
                  <template #format><span class="text-5xl font-mono text-orange-600 font-black">{{ qcStatus.fai.countdown }}s</span></template>
                </Progress>
                <div class="text-2xl font-bold text-orange-800 mt-8">品管部首检判定中...</div>
              </div>
              <div v-else-if="qcStatus.fai.state === 'DONE'" class="flex flex-col items-center bg-emerald-50 p-16 rounded-3xl border-2 border-emerald-200 shadow-xl w-[500px]">
                <div class="text-[100px] mb-6 animate-bounce">✅</div>
                <div class="text-4xl font-black text-emerald-700 mb-10">首检合格 (PASS)</div>
                <Button size="large" class="h-16 w-full text-2xl font-bold border-emerald-500 text-emerald-600 rounded-2xl bg-white" @click="previewFAIReport"><IconifyIcon icon="lucide:file-text" class="mr-2" /> 查看并打印质检电子报告</Button>
              </div>
            </div>
          </Tabs.TabPane>

          <Tabs.TabPane key="IPQC" tab="过程抽检(IPQC)">
            <div class="flex-1 h-full flex flex-col items-center justify-center p-8 bg-slate-50 relative">
              <div v-if="qcStatus.ipqc.state === 'IDLE'" class="w-[600px] bg-white p-8 rounded-3xl border-2 border-blue-200 shadow-xl">
                <div class="text-center mb-8">
                  <IconifyIcon icon="lucide:search-check" class="text-[80px] text-blue-400 mb-4 drop-shadow-md" />
                  <h2 class="text-2xl font-black text-slate-800">提报生产过程品质抽检</h2>
                </div>
                <Form layout="vertical">
                  <Form.Item label="取样位置规范"><Input v-model:value="qcStatus.ipqc.pos" class="h-14 text-xl font-bold" /></Form.Item>
                  <Form.Item label="送检数量 (PCS)"><InputNumber v-model:value="qcStatus.ipqc.qty" class="w-full h-14 text-xl font-bold flex items-center" /></Form.Item>
                </Form>
                <Button size="large" type="primary" class="w-full h-16 text-2xl font-bold rounded-2xl bg-blue-600 hover:bg-blue-500 border-none shadow-lg mt-4" @click="requestIPQC">🚀 提交抽检任务至质检室</Button>
                <div v-if="qcStatus.ipqc.records.length > 0" class="mt-6 pt-4 border-t border-slate-200">
                  <div class="text-sm font-bold text-slate-500 mb-2">本班次已抽检记录:</div>
                  <div v-for="rec in qcStatus.ipqc.records" :key="rec.sn" class="flex justify-between bg-slate-50 p-2 rounded mb-2 text-sm font-mono border border-slate-200"><span class="text-blue-700 font-bold">{{rec.sn}}</span> <span class="text-emerald-600 font-bold">合格 ({{rec.time}})</span></div>
                </div>
              </div>
              <div v-else-if="qcStatus.ipqc.state === 'INSPECTING'" class="flex flex-col items-center bg-white p-16 rounded-3xl border-2 border-blue-200 shadow-xl w-[500px]">
                <Progress type="circle" :percent="Math.floor(((5 - qcStatus.ipqc.countdown)/5)*100)" :width="180" strokeColor="#2563eb">
                  <template #format><span class="text-5xl font-mono text-blue-600 font-black">{{ qcStatus.ipqc.countdown }}s</span></template>
                </Progress>
                <div class="text-2xl font-bold text-blue-800 mt-8">IPQC 核验仪器检测中...</div>
              </div>
              <div v-else-if="qcStatus.ipqc.state === 'DONE'" class="flex flex-col items-center bg-emerald-50 p-16 rounded-3xl border-2 border-emerald-200 shadow-xl w-[500px]">
                <div class="text-[100px] mb-6 animate-bounce">✅</div>
                <div class="text-4xl font-black text-emerald-700 mb-10">抽检全部合格</div>
                <Button size="large" class="h-16 w-full text-2xl font-bold border-emerald-500 text-emerald-600 rounded-2xl bg-white mb-4" @click="previewIPQCReport"><IconifyIcon icon="lucide:tag" class="mr-2" /> 查看并打印 NAP 标签</Button>
                <Button type="link" class="text-slate-500 text-lg" @click="resetIpqc">继续下一次抽检发起</Button>
              </div>
            </div>
          </Tabs.TabPane>

          <Tabs.TabPane key="FQC" tab="完工质检(FQC)">
            <div class="flex-1 h-full flex flex-col items-center justify-center p-8 bg-slate-50 relative">
              <div v-if="qcStatus.fqc.state === 'IDLE'" class="text-center max-w-xl">
                <IconifyIcon icon="lucide:package-check" class="text-[120px] text-indigo-400 mb-8 drop-shadow-md" />
                <h2 class="text-3xl font-black text-slate-800 mb-4">本工单即将结束生产</h2>
                <p class="text-xl text-slate-500 mb-10 leading-relaxed">系统将下发 FQC 抽样指令，进行成品入库前的最终质量核验与 COA 报告生成。</p>
                <Button size="large" type="primary" class="w-full h-16 text-2xl font-bold rounded-2xl bg-indigo-600 hover:bg-indigo-500 border-none shadow-lg" @click="requestFQC">📡 提交完工质检 (FQC) 申请</Button>
              </div>
              <div v-else-if="qcStatus.fqc.state === 'INSPECTING'" class="flex flex-col items-center bg-white p-16 rounded-3xl border-2 border-indigo-200 shadow-xl w-[500px]">
                <Progress type="circle" :percent="Math.floor(((5 - qcStatus.fqc.countdown)/5)*100)" :width="180" strokeColor="#4f46e5">
                  <template #format><span class="text-5xl font-mono text-indigo-600 font-black">{{ qcStatus.fqc.countdown }}s</span></template>
                </Progress>
                <div class="text-2xl font-bold text-indigo-800 mt-8">FQC 全面检验中...</div>
              </div>
              <div v-else-if="qcStatus.fqc.state === 'DONE'" class="flex flex-col items-center bg-emerald-50 p-16 rounded-3xl border-2 border-emerald-200 shadow-xl w-[500px]">
                <div class="text-[100px] mb-6 animate-bounce">✅</div>
                <div class="text-4xl font-black text-emerald-700 mb-10">检验合格 准许入库</div>
                <Button size="large" class="h-16 w-full text-2xl font-bold border-emerald-500 text-emerald-600 rounded-2xl bg-white" @click="previewFQCReport"><IconifyIcon icon="lucide:file-badge" class="mr-2" /> 查看并打印出厂报告 (COA)</Button>
              </div>
            </div>
          </Tabs.TabPane>
        </Tabs>
      </div>

    </main>

    <footer class="flex h-[80px] shrink-0 items-center justify-between border-t border-slate-200 bg-white px-8 shadow-[0_-8px_20px_rgba(0,0,0,0.03)] z-20">
      <div class="flex items-center gap-3">
        <Button class="h-14 px-6 font-black text-lg text-slate-600 border-2 border-slate-300 rounded-xl hover:text-indigo-600 hover:border-indigo-400 shadow-sm" @click="emit('close')">
          <IconifyIcon icon="lucide:layout-dashboard" class="mr-2 text-2xl" /> 返回主板
        </Button>
        <Divider type="vertical" class="h-8 bg-slate-300 mx-2" />
        <Dropdown placement="topRight">
          <Button class="h-14 px-6 font-black text-lg text-slate-600 border-2 border-slate-300 rounded-xl hover:text-indigo-600 shadow-sm">
            <IconifyIcon icon="lucide:settings-2" class="mr-2 text-2xl" /> 机台操作
          </Button>
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
          <Button class="h-14 px-6 text-xl font-bold rounded-xl border-2" v-if="activeTabPre === '1'" @click="checkData.forEach(d=>d.result='正常')">一键点检合格</Button>
          <Button type="primary" class="h-14 px-12 text-xl font-black rounded-xl bg-indigo-600 shadow-lg" :disabled="!isPreCheckComplete" @click="finishPreCheck">完成开机准备</Button>
        </template>
        <template v-if="workOrderStatus === '工艺参数'">
          <Button type="primary" class="h-14 px-12 text-xl font-black rounded-xl bg-blue-600 shadow-lg" @click="finishProcessParams">工艺设定完成，进入质检</Button>
        </template>
        <template v-if="workOrderStatus === '品质检验'">
          <Button danger class="h-14 px-8 text-xl font-black rounded-xl border-2" @click="emit('abnormal')">品质异常报警</Button>
          <Button type="primary" class="h-14 px-12 text-xl font-black rounded-xl bg-emerald-500 hover:bg-emerald-400 border-none shadow-xl shadow-emerald-200" @click="handleFinishJob">完工报工入库</Button>
        </template>
      </div>
    </footer>

    <Modal v-model:open="startJobModalVisible" title="确认开工信息" :width="500" centered>
      <Form layout="vertical" class="mt-6">
        <div class="mb-6 bg-indigo-50 text-indigo-700 p-4 rounded-xl text-sm font-bold border border-indigo-100">
          已成功核销 {{ scannedMaterials.length }} 桶配料，请确认实际投入生产的时间。
        </div>
        <Form.Item label="实际开工时间" required>
          <DatePicker v-model:value="startJobForm.time" show-time class="w-full h-12 text-lg font-bold" />
        </Form.Item>
      </Form>
      <template #footer>
        <Button size="large" @click="startJobModalVisible = false">取消</Button>
        <Button size="large" type="primary" @click="confirmStartJob">确认开工</Button>
      </template>
    </Modal>

    <Modal v-model:open="finishModalVisible" title="✅ 湿法完工报工入库" @ok="submitFinish" :width="600" centered>
      <Form layout="vertical" class="mt-4 p-4">
        <div class="mb-6 text-sm font-bold text-indigo-700 bg-indigo-50 p-4 rounded-xl border border-indigo-200">确认报工后将打印半成品库入库标签并生成线边库编码。</div>
        <Form.Item label="收卷良品总米数 (m)" required><InputNumber v-model:value="finishForm.meters" class="w-full h-14 text-2xl font-black flex items-center" /></Form.Item>
        <Form.Item label="过程废品米数 (m)"><InputNumber v-model:value="finishForm.waste" class="w-full h-12 text-lg flex items-center" /></Form.Item>
      </Form>
    </Modal>

    <Modal v-model:open="inboundPreviewVisible" title="📦 生产流转入库成功" :width="700" :footer="false" centered>
      <div class="flex flex-col items-center py-6 px-4 text-center">
        <IconifyIcon icon="lucide:check-circle-2" class="text-[80px] text-emerald-500 mb-4" />
        <h3 class="text-3xl font-black text-slate-800 mb-6">成功入库 {{ finishForm.meters }} m</h3>
        <div class="w-full grid grid-cols-3 gap-4 mb-6">
          <div class="bg-slate-50 p-4 rounded-xl border border-slate-200"><div class="text-sm text-slate-500 font-bold mb-1">总产出量</div><div class="text-2xl font-black text-slate-800">{{ inboundSummary.total }} m</div></div>
          <div class="bg-red-50 p-4 rounded-xl border border-red-200"><div class="text-sm text-red-500 font-bold mb-1">废品合计</div><div class="text-2xl font-black text-red-600">{{ finishForm.waste || 0 }} m</div></div>
          <div class="bg-emerald-50 p-4 rounded-xl border border-emerald-200"><div class="text-sm text-emerald-600 font-bold mb-1">良率表现</div><div class="text-2xl font-black text-emerald-600">{{ inboundSummary.yieldRate }}</div></div>
        </div>
        <div class="bg-amber-50 border border-amber-200 p-4 rounded-xl mb-6 w-full text-left shadow-inner">
          <div class="text-amber-800 font-bold mb-2 text-lg"><IconifyIcon icon="lucide:timer" class="mr-1 inline" />下一工序 Q-Time 倒计时已启动！</div>
          <div class="text-amber-700 font-mono mb-1">本站完工时间：{{ new Date().toLocaleTimeString() }}</div>
          <div class="text-amber-700">工艺流转要求：请于 <span class="font-black text-red-600 text-lg">30分钟内</span> 将此批产品流转至下工序投料！</div>
        </div>
        <div class="flex gap-4 w-full">
          <Button size="large" class="flex-1 h-14 text-lg font-bold rounded-xl" @click="inboundPreviewVisible = false">关闭返回</Button>
          <Button size="large" type="primary" class="flex-1 h-14 text-lg font-bold bg-indigo-600 rounded-xl shadow-md" @click="printFromPreview"><IconifyIcon icon="lucide:printer" class="mr-2" /> 打印交接单</Button>
        </div>
      </div>
    </Modal>

    <Modal v-model:open="faiPreviewVisible" title="📄 FAI首检单预览" :width="800" :footer="false" centered><div class="p-4 bg-slate-100 rounded-xl mb-4"><iframe :srcdoc="faiPreviewHtml" class="w-full h-[300px] bg-white border border-slate-300"></iframe></div><div class="flex justify-end"><Button size="large" type="primary" class="bg-indigo-600 font-bold" @click="executePrint()">确认打印</Button></div></Modal>
    <Modal v-model:open="ipqcPreviewVisible" title="📄 IPQC抽检单预览" :width="800" :footer="false" centered><div class="p-4 bg-slate-100 rounded-xl mb-4"><iframe :srcdoc="ipqcPreviewHtml" class="w-full h-[300px] bg-white border border-slate-300"></iframe></div><div class="flex justify-end"><Button size="large" type="primary" class="bg-indigo-600 font-bold" @click="executePrint()">确认打印</Button></div></Modal>
    <Modal v-model:open="fqcPreviewVisible" title="📄 成品出厂报告 (COA) 预览" :width="900" :footer="false" centered><div class="p-4 bg-slate-100 rounded-xl mb-4"><iframe :srcdoc="fqcPreviewHtml" class="w-full h-[350px] bg-white border border-slate-300"></iframe></div><div class="flex justify-end"><Button size="large" type="primary" class="bg-indigo-600 font-bold" @click="executePrint()">确认打印</Button></div></Modal>

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

.custom-left-tabs :deep(.ant-tabs-nav-list) { padding-top: 16px; width: 180px; }
.custom-left-tabs :deep(.ant-tabs-tab) { padding: 20px 24px !important; font-size: 18px; font-weight: 900; color: #94a3b8; transition: all 0.2s;}
.custom-left-tabs :deep(.ant-tabs-tab-active) { background: #eef2ff; color: #4338ca; border-right: 4px solid #4338ca; }

/* Table 大屏放大与自适应内部滚动 */
.vben-schema-table { height: 100%; display: flex; flex-direction: column; }
.vben-schema-table :deep(.ant-spin-nested-loading),
.vben-schema-table :deep(.ant-spin-container),
.vben-schema-table :deep(.ant-table),
.vben-schema-table :deep(.ant-table-container) { height: 100%; display: flex; flex-direction: column; min-height: 0; }
.vben-schema-table :deep(.ant-table-body) { flex: 1; overflow-y: auto !important; min-height: 0; }
.vben-schema-table :deep(.ant-table-placeholder) { flex: 1; display: flex; align-items: center; justify-content: center; }
.vben-schema-table :deep(.ant-table-thead > tr > th) { background: #f8fafc !important; color: #64748b !important; font-size: 16px; font-weight: 900; padding: 16px 16px !important; border-bottom: 2px solid #e2e8f0;}
.vben-schema-table :deep(.ant-table-cell) { padding: 12px 16px !important; font-size: 16px; color: #334155; }
.vben-schema-table :deep(.ant-table-row:hover > td) { background: #f1f5f9 !important; }

/* 滚动条美化 */
.vben-schema-table :deep(.ant-table-body)::-webkit-scrollbar { width: 12px; height: 12px; }
.vben-schema-table :deep(.ant-table-body)::-webkit-scrollbar-thumb { background: #94a3b8; border-radius: 6px; }
.vben-schema-table :deep(.ant-table-body)::-webkit-scrollbar-track { background: #f1f5f9; border-radius: 6px; }
::-webkit-scrollbar { width: 8px; height: 8px; }
::-webkit-scrollbar-thumb { background: #cbd5e1; border-radius: 4px; }
</style>
