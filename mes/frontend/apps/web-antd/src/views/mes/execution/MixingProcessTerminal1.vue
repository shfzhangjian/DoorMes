<script lang="ts" setup>
import { ref, watch, computed } from 'vue';
import {
  Button, Card, Tag, Input, InputNumber, Table, Tabs,
  message, Modal, Select, Divider, Drawer, Timeline, Form, Pagination, DatePicker
} from 'ant-design-vue';
import type { TableColumnsType } from 'ant-design-vue';
import { IconifyIcon } from '@vben/icons';

const props = defineProps(['operator', 'order', 'process']);
const emit = defineEmits(['close']);

const clone = (data: any) => JSON.parse(JSON.stringify(data));

// ==================================================================================
// 核心状态机定义 & 切换工单监听 (修复点 1)
// ==================================================================================
const workOrderStatus = ref(props.order.status || '未开工');
const batchNo = ref(props.order.batch || '');
const activeTab = ref('1');

// 监听外部工单切换，自动控制当前面板信息
watch(() => props.order, (newOrder) => {
  workOrderStatus.value = newOrder.status || '未开工';
  batchNo.value = newOrder.batch || '';
  activeTab.value = '1';
  message.info(`已切换至工单: ${newOrder.id}，当前状态: ${workOrderStatus.value}`);
}, { deep: true });

// 需求 5: 历史记录系统
const historyDrawerVisible = ref(false);
const historyDetailVisible = ref(false);
const activeHistoryDetail = ref<any>(null);
const historyList = ref<any[]>([]);

function recordHistory(action: string, detail: string, snapshot: any = null) {
  historyList.value.unshift({
    time: new Date().toLocaleString(),
    status: workOrderStatus.value,
    action,
    user: props.operator.userName,
    detail,
    snapshot
  });
}

function viewHistoryDetail(log: any) {
  if (!log.snapshot) return message.info('该记录无详细表单快照');
  activeHistoryDetail.value = log;
  historyDetailVisible.value = true;
}

// ==================================================================================
// 1. 未开工状态逻辑
// ==================================================================================
const startJobTime = ref<string | null>(null);

function handleStartJob() {
  if (!startJobTime.value) return message.warning('请先选择实际开工时间！');
  batchNo.value = 'BATCH-' + new Date().getTime().toString().slice(-8);
  workOrderStatus.value = '作业前准备';
  activeTab.value = '1';

  recordHistory('开始作业', `实际开工时间: ${startJobTime.value}, 生成批次: ${batchNo.value}`, {
    type: 'start', time: startJobTime.value, batchNo: batchNo.value
  });
  message.success('已进入作业前准备状态');
}

// ==================================================================================
// 2. 作业前准备数据与逻辑 (修复点 2: 严格核对列)
// ==================================================================================
const envParams = ref(Array.from({ length: 8 }).map((_, i) => ({
  id: `E0${i+1}`, name: i===0?'环境温度':(i===1?'环境湿度':`监控项${i+1}`),
  ref: i===0?'22-26':(i===1?'40-60':'10-20'), unit: i===0?'℃':(i===1?'%RH':'N/A'),
  source: '传感器', val: null, status: '未检', time: '--'
})));

// 需求 2.1 表格列：参数编号, 参数名称, 参考值, 单位, 数据来源, 参数值, 状态, 最后更新时间
const envColumns: TableColumnsType = [
  { title: '参数编号', dataIndex: 'id', width: 80 },
  { title: '参数名称', dataIndex: 'name' },
  { title: '参考值', dataIndex: 'ref' },
  { title: '单位', dataIndex: 'unit', width: 60 },
  { title: '数据来源', dataIndex: 'source' },
  { title: '参数值', dataIndex: 'val', width: 120 },
  { title: '状态', dataIndex: 'status', width: 90 },
  { title: '最后更新时间', dataIndex: 'time' }
];

const deviceChecks = ref(Array.from({ length: 8 }).map((_, i) => ({
  device: '主配料罐', item: `点检项目 ${i+1}`, req: '功能正常', result: '未点', remark: '', time: '--'
})));

// 需求 2.2 表格列：设备名称, 点检项目, 点检要求, 点检结果, 备注, 点检时间
const deviceColumns: TableColumnsType = [
  { title: '设备名称', dataIndex: 'device' },
  { title: '点检项目', dataIndex: 'item' },
  { title: '点检要求', dataIndex: 'req' },
  { title: '点检结果', dataIndex: 'result', width: 120 },
  { title: '备注', dataIndex: 'remark' },
  { title: '点检时间', dataIndex: 'time', width: 140 }
];

function fetchEnvValues() {
  envParams.value.forEach(p => p.val = parseFloat((Math.random()*10 + 20).toFixed(1)));
  message.success('已获取最新环境参数');
}
function confirmEnvNormal() {
  envParams.value.forEach(p => { if(p.val) { p.status = '正常'; p.time = new Date().toLocaleTimeString(); } });
}
function handleQuickCheck() {
  deviceChecks.value.forEach(d => {
    if(d.result === '未点') { d.result = '正常'; d.time = new Date().toLocaleTimeString(); }
  });
  message.success('设备一键点检完成');
}

const isPreCheckComplete = computed(() => {
  return envParams.value.every(p => p.status !== '未检') && deviceChecks.value.every(d => d.result !== '未点');
});

function finishPreCheck() {
  recordHistory('完成班前检', '环境及设备检查全部确认正常', {
    type: 'pre_check', envData: clone(envParams.value), deviceData: clone(deviceChecks.value)
  });
  workOrderStatus.value = '配方投料';
  activeTab.value = '1';
}

// ==================================================================================
// 3. 配方投料数据与逻辑 (修复点 2: 严格核对列)
// ==================================================================================
const recipePages = ref([{ id: 1, code: '', verifier: '', time: '--', status: '未核对', remark: '', items: [] }]);
const currentRecipePage = ref(1);
const curRecipe = computed(() => recipePages.value[currentRecipePage.value - 1]);
const unverifiedRecipeCount = computed(() => recipePages.value.filter(r => r.status !== '已核对').length);

// 需求 3.1 表格列：配方编码, 配方名称, 标准用量, 计量单位, 允差%, 实际用量, 备注说明
const recipeColumns: TableColumnsType = [
  { title: '配方编码', dataIndex: 'code' },
  { title: '配方名称', dataIndex: 'name' },
  { title: '标准用量', dataIndex: 'std', align: 'right' },
  { title: '计量单位', dataIndex: 'unit', width: 80 },
  { title: '允差%', dataIndex: 'tol' },
  { title: '实际用量', dataIndex: 'actual', width: 120 },
  { title: '备注说明', dataIndex: 'remark' }
];

watch(() => curRecipe.value.code, (val) => {
  if (val && val.length === 9) {
    curRecipe.value.items = Array.from({ length: 6 }).map((_, i) => ({
      code: `M${val.slice(-3)}-${i}`, name: `仿真物料${i+1}`, std: 100, unit: 'kg', tol: '1%', actual: 0, remark: ''
    }));
    curRecipe.value.status = '未核对';
    message.success('已查找到配方清单并初始化');
  }
});

function verifyRecipe() {
  curRecipe.value.status = '已核对';
  curRecipe.value.verifier = props.operator.userName;
  curRecipe.value.time = new Date().toLocaleTimeString();
}
function addRecipe() {
  recipePages.value.push({ id: recipePages.value.length + 1, code: '', verifier: '', time: '--', status: '未核对', remark: '', items: [] });
  currentRecipePage.value = recipePages.value.length;
}
function deleteRecipe() {
  Modal.confirm({
    title: '删除确认', content: '确认删除当前配方页吗？',
    onOk: () => {
      if (recipePages.value.length === 1) return message.warning('必须保留至少一个配方页');
      recipePages.value.splice(currentRecipePage.value - 1, 1);
      currentRecipePage.value = Math.max(1, currentRecipePage.value - 1);
    }
  });
}

const materialForm = ref({ stockCode: '', verifier: '', time: '--', status: '未核对', remark: '' });
const materialLogs = ref<any[]>([]);

// 需求 3.2 表格列：物料编码, 物料名称, 标准配比(%), 计量单位, 规格型号, 材料库存号, 库存量, 领用量, 备注说明
const materialColumns: TableColumnsType = [
  { title: '物料编码', dataIndex: 'code' },
  { title: '物料名称', dataIndex: 'name' },
  { title: '标准配比(%)', dataIndex: 'ratio', align: 'center' },
  { title: '计量单位', dataIndex: 'unit', width: 80 },
  { title: '规格型号', dataIndex: 'spec' },
  { title: '材料库存号', dataIndex: 'stockId' },
  { title: '库存量', dataIndex: 'stockQty', align: 'right' },
  { title: '领用量', dataIndex: 'pickQty', width: 120 },
  { title: '备注说明', dataIndex: 'remark' }
];

watch(() => materialForm.value.stockCode, (val) => {
  if (val && val.length === 9) {
    materialLogs.value.unshift({
      code: 'MT-'+val.slice(-4), name: '扫码原料', ratio: '100%', unit: 'kg', spec: 'Standard',
      stockId: val, stockQty: 500, pickQty: 0, remark: ''
    });
    materialForm.value.status = '未核对';
    materialForm.value.stockCode = '';
    message.success('成功扫描库存追加物料');
  }
});

function verifyMaterial() {
  materialForm.value.status = '已核对';
  materialForm.value.verifier = props.operator.userName;
  materialForm.value.time = new Date().toLocaleTimeString();
}

const isRecipeMixComplete = computed(() => unverifiedRecipeCount.value === 0 && materialForm.value.status === '已核对');

function finishRecipeMix() {
  recordHistory('配方投料完成', '配方及物料领用已全部核对', {
    type: 'recipe_mix', recipes: clone(recipePages.value), materials: clone(materialLogs.value)
  });
  workOrderStatus.value = '作业中';
}

// ==================================================================================
// 4. 作业中及完成作业状态逻辑
// ==================================================================================
const printModalVisible = ref(false);

function handlePrint() { printModalVisible.value = true; }
function submitPrint() {
  message.success('打印指令已发送');
  printModalVisible.value = false;
  recordHistory('标签打印', '成功调用 Zebra 打印机输出标签', { type: 'print', batch: batchNo.value });
}

const stopModalVisible = ref(false);
const stopForm = ref({ reason: '', person: props.operator.userName, time: new Date().toLocaleString() });

function submitStop() {
  recordHistory('停止作业', `${props.process.name}工序因 [${stopForm.value.reason}] 停止`, {
    type: 'stop', reason: stopForm.value.reason, time: stopForm.value.time
  });
  workOrderStatus.value = '未开工';
  stopModalVisible.value = false;
  message.warning('工单已标记为停止作业');
}

const finishModalVisible = ref(false);
// 统一使用 DatePicker 选择实际完工时间
const finishForm = ref({ time: '', hours: 2, remark: '' });

function submitFinish() {
  if (!finishForm.value.time) return message.warning('请选择实际完工时间');
  recordHistory('完成工序', `实际工时: ${finishForm.value.hours}H, 标记待开工`, {
    type: 'finish', time: finishForm.value.time, hours: finishForm.value.hours, remark: finishForm.value.remark
  });
  workOrderStatus.value = '完成作业';
  finishModalVisible.value = false;
  message.success('完成工序并推送下一步');
  setTimeout(() => emit('close'), 1500);
}
</script>

<template>
  <div class="flex flex-col absolute inset-0 bg-[#f4f6f8] overflow-hidden">

    <header class="flex h-14 shrink-0 items-center justify-between border-b bg-white px-4 shadow-sm z-10">
      <div class="flex items-center gap-6">
        <div v-for="label in ['未开工', '作业前准备', '配方投料', '作业中', '完成作业']" :key="label"
             :class="['flex items-center gap-2 font-bold text-xs transition-colors', workOrderStatus === label ? 'text-blue-600' : 'text-slate-400']">
          <IconifyIcon :icon="workOrderStatus === label ? 'lucide:circle-dot' : 'lucide:circle'" />
          <span>{{ label }}</span>
        </div>
      </div>
      <div class="flex items-center gap-4">
        <span v-if="batchNo" class="text-xs font-mono font-bold text-blue-700 bg-blue-50 px-3 py-1 rounded-full border border-blue-100">批次: {{ batchNo }}</span>
        <Tabs v-if="['作业前准备', '配方投料'].includes(workOrderStatus)" v-model:activeKey="activeTab" type="capsule" size="small" class="compact-tabs">
          <template v-if="workOrderStatus === '作业前准备'"><Tabs.TabPane key="1" tab="环境参数" /><Tabs.TabPane key="2" tab="设备点检" /></template>
          <template v-else-if="workOrderStatus === '配方投料'"><Tabs.TabPane key="1" tab="配方核对" /><Tabs.TabPane key="2" tab="物料领用核对" /></template>
        </Tabs>
      </div>
    </header>

    <main class="flex-1 flex flex-col min-h-0 p-3 overflow-hidden">

      <div v-if="workOrderStatus === '未开工'" class="flex-1 flex items-center justify-center">
        <Card class="w-[450px] text-center border-none shadow-xl rounded-2xl p-6">
          <div class="mb-6 flex h-20 w-20 items-center justify-center rounded-full bg-blue-50 text-blue-600 mx-auto shadow-inner">
            <IconifyIcon icon="lucide:play-circle" class="text-5xl" />
          </div>
          <h2 class="text-xl font-black text-slate-800 mb-2">等待开工确认</h2>
          <p class="mb-6 text-xs text-slate-400">操作员: {{ operator.userName }} | 工单: {{ order.id }}</p>

          <div class="text-left bg-slate-50 p-4 rounded-xl border border-slate-100 mb-6">
            <div class="text-xs font-bold text-slate-600 mb-2">选择实际开工时间 (必填)：</div>
            <DatePicker
              v-model:value="startJobTime" show-time value-format="YYYY-MM-DD HH:mm:ss"
              :inputReadOnly="true" size="large" placeholder="请点击此处选择时间..." style="width: 100%"
            />
          </div>
          <Button type="primary" size="large" block class="h-12 rounded-xl bg-blue-600 font-bold shadow-lg" :disabled="!startJobTime" @click="handleStartJob">确认并开始作业</Button>
        </Card>
      </div>

      <div v-if="workOrderStatus === '作业前准备'" class="flex-1 flex flex-col bg-white border border-slate-200 rounded-lg overflow-hidden shadow-sm">
        <Table v-if="activeTab === '1'" :columns="envColumns" :dataSource="envParams" :pagination="false" size="small" :scroll="{ y: 'calc(100vh - 210px)' }" class="vben-schema-table flex-1">
          <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'val'"><InputNumber v-model:value="record.val" size="small" class="w-20" @change="record.status='未检'"/></template>
            <template v-else-if="column.dataIndex === 'status'"><Tag :color="record.status==='正常'?'green':'red'">{{ record.status }}</Tag></template>
          </template>
        </Table>
        <Table v-if="activeTab === '2'" :columns="deviceColumns" :dataSource="deviceChecks" :pagination="false" size="small" :scroll="{ y: 'calc(100vh - 210px)' }" class="vben-schema-table flex-1">
          <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'result'">
              <Select v-model:value="record.result" size="small" class="w-24" @change="record.time=new Date().toLocaleTimeString()">
                <Select.Option value="未点">未点</Select.Option><Select.Option value="正常">正常</Select.Option><Select.Option value="异常">异常</Select.Option>
              </Select>
            </template>
            <template v-else-if="column.dataIndex === 'remark'"><Input v-model:value="record.remark" size="small" /></template>
          </template>
        </Table>
      </div>

      <div v-if="workOrderStatus === '配方投料'" class="flex-1 flex flex-col overflow-hidden gap-2">
        <div v-if="activeTab === '1'" class="flex-1 flex flex-col min-h-0 bg-white border border-slate-200 rounded-lg overflow-hidden shadow-sm">
          <div class="flex items-center gap-6 p-2 bg-slate-50 border-b overflow-x-auto whitespace-nowrap shrink-0">
            <div class="flex items-center"><span class="text-xs font-bold text-slate-500 mr-2 shrink-0">配方编码:</span> <Input v-model:value="curRecipe.code" size="small" placeholder="扫码枪模拟(9位)" class="w-40" /></div>
            <div class="flex items-center"><span class="text-xs font-bold text-slate-500 mr-2 shrink-0">复核人:</span> <span class="text-xs font-bold">{{ curRecipe.verifier || '--' }}</span></div>
            <div class="flex items-center"><span class="text-xs font-bold text-slate-500 mr-2 shrink-0">核对状态:</span> <Tag :color="curRecipe.status==='已核对'?'green':'orange'">{{ curRecipe.status }}</Tag></div>
            <div class="flex items-center flex-1"><span class="text-xs font-bold text-slate-500 mr-2 shrink-0">备注:</span> <Input v-model:value="curRecipe.remark" size="small" class="w-full" /></div>
          </div>
          <div class="flex items-center justify-between px-3 py-1.5 bg-white border-b shrink-0">
            <div class="text-xs font-bold text-red-500 flex items-center gap-1"><IconifyIcon icon="lucide:alert-circle" /> 未核对数: {{ unverifiedRecipeCount }}</div>
            <Pagination v-model:current="currentRecipePage" :total="recipePages.length" :pageSize="1" size="small" />
          </div>
          <Table :columns="recipeColumns" :dataSource="curRecipe.items" :pagination="false" size="small" :scroll="{ y: 'calc(100vh - 310px)' }" class="vben-schema-table flex-1">
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'actual'"><InputNumber v-model:value="record.actual" size="small" class="w-full" /></template>
              <template v-else-if="column.dataIndex === 'remark'"><Input v-model:value="record.remark" size="small" /></template>
            </template>
          </Table>
        </div>
        <div v-if="activeTab === '2'" class="flex-1 flex flex-col min-h-0 bg-white border border-slate-200 rounded-lg overflow-hidden shadow-sm">
          <div class="flex items-center gap-6 p-2 bg-slate-50 border-b overflow-x-auto whitespace-nowrap shrink-0">
            <div class="flex items-center"><span class="text-xs font-bold text-slate-500 mr-2 shrink-0">库存条码:</span> <Input v-model:value="materialForm.stockCode" size="small" placeholder="扫码入库(9位)" class="w-48" /></div>
            <div class="flex items-center"><span class="text-xs font-bold text-slate-500 mr-2 shrink-0">状态:</span> <Tag :color="materialForm.status==='已核对'?'green':'orange'">{{ materialForm.status }}</Tag></div>
          </div>
          <Table :columns="materialColumns" :dataSource="materialLogs" :pagination="false" size="small" :scroll="{ y: 'calc(100vh - 270px)' }" class="vben-schema-table flex-1">
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'pickQty'"><InputNumber v-model:value="record.pickQty" :max="record.stockQty" size="small" class="w-full" /></template>
              <template v-else-if="column.dataIndex === 'remark'"><Input v-model:value="record.remark" size="small" /></template>
            </template>
          </Table>
        </div>
      </div>

      <div v-if="workOrderStatus === '作业中'" class="flex-1 flex flex-col items-center justify-center">
        <IconifyIcon icon="lucide:loader-circle" class="text-blue-600 text-6xl animate-spin mb-4" />
        <h2 class="text-2xl font-black text-slate-800">正在作业中...</h2>
      </div>
    </main>

    <footer class="flex h-14 shrink-0 items-center justify-between border-t bg-white px-4 shadow-[0_-2px_8px_rgba(0,0,0,0.05)] z-20">
      <div class="flex items-center gap-4">
        <Button @click="emit('close')" class="font-bold text-slate-600 border-slate-300">退出终端</Button>
        <Divider type="vertical" />
        <Button type="link" class="p-0 font-bold text-blue-600" @click="historyDrawerVisible = true">工序执行历史记录</Button>
      </div>

      <div class="flex gap-3 items-center">
        <template v-if="workOrderStatus === '作业前准备'">
          <template v-if="activeTab === '1'"><Button @click="fetchEnvValues" class="border-blue-200 text-blue-700">获取值</Button><Button @click="confirmEnvNormal" class="border-blue-200 text-blue-700">确认正常</Button></template>
          <template v-if="activeTab === '2'"><Button @click="handleQuickCheck" class="border-blue-200 text-blue-700">一键点检</Button></template>
          <Button type="primary" class="bg-blue-600 font-bold px-8" :disabled="!isPreCheckComplete" @click="finishPreCheck">完成作业前准备</Button>
        </template>
        <template v-if="workOrderStatus === '配方投料'">
          <template v-if="activeTab === '1'"><Button danger @click="deleteRecipe">删除当前配方</Button><Button @click="addRecipe" class="border-blue-200 text-blue-700">追加配方</Button><Button @click="verifyRecipe" class="border-blue-200 text-blue-700">一键核对</Button></template>
          <template v-if="activeTab === '2'"><Button @click="verifyMaterial" class="border-blue-200 text-blue-700">一键核对</Button></template>
          <Button type="primary" class="bg-blue-600 font-bold px-8" :disabled="!isRecipeMixComplete" @click="finishRecipeMix">完成作业 (进入作业中)</Button>
        </template>
        <template v-if="workOrderStatus === '作业中'">
          <Button @click="handlePrint" class="border-blue-200 text-blue-700">打印标签</Button>
          <Button danger @click="stopModalVisible = true">停止作业</Button>
          <Button type="primary" class="bg-green-600 border-none font-bold px-8" @click="finishModalVisible = true">完成工序推送下步</Button>
        </template>
      </div>
    </footer>

    <Modal v-model:open="printModalVisible" title="斑马打印机 USB 预览" @ok="submitPrint">
      <div style="text-align:center; padding: 20px; border: 1px solid #e2e8f0; border-radius: 8px; background: #fff;">
        <h3 style="margin-bottom:10px; color: #1e293b; font-weight: bold;">产出批次: {{ batchNo }}</h3>
        <IconifyIcon icon="lucide:qr-code" style="font-size: 80px; color: #334155;" />
      </div>
    </Modal>
    <Modal v-model:open="stopModalVisible" title="停止作业反馈" @ok="submitStop">
      <Form layout="vertical" class="mt-4">
        <Form.Item label="停止原因"><Input v-model:value="stopForm.reason" placeholder="如设备故障" /></Form.Item>
        <Form.Item label="负责人"><Input v-model:value="stopForm.person" disabled /></Form.Item>
      </Form>
    </Modal>
    <Modal v-model:open="finishModalVisible" title="完工推送确认" @ok="submitFinish">
      <Form layout="vertical" class="mt-4">
        <Form.Item label="实际完工时间">
          <DatePicker v-model:value="finishForm.time" show-time value-format="YYYY-MM-DD HH:mm:ss" :inputReadOnly="true" class="w-full" />
        </Form.Item>
        <Form.Item label="实际工时(H)"><InputNumber v-model:value="finishForm.hours" class="w-full" /></Form.Item>
        <Form.Item label="备注"><Input v-model:value="finishForm.remark" /></Form.Item>
      </Form>
    </Modal>

    <Drawer v-model:open="historyDrawerVisible" title="工序执行与追溯系统" placement="right" width="800">
      <div class="flex h-full gap-4">
        <div class="w-64 border-r border-slate-100 pr-4 overflow-y-auto">
          <Timeline>
            <Timeline.Item v-for="(log, idx) in historyList" :key="idx" :color="idx === 0 ? 'blue' : 'gray'">
              <div class="font-bold mb-1 text-slate-700 cursor-pointer hover:text-blue-600 transition-colors" @click="viewHistoryDetail(log)">
                {{ log.action }} <IconifyIcon icon="lucide:external-link" class="text-blue-400" v-if="log.snapshot" />
              </div>
              <div class="text-xs text-slate-500 mb-1">{{ log.time }}</div>
              <div class="text-xs text-slate-400">{{ log.user }}</div>
            </Timeline.Item>
          </Timeline>
        </div>
        <div class="flex-1 overflow-y-auto bg-slate-50 p-4 rounded-lg">
          <div v-if="!activeHistoryDetail" class="text-center text-slate-400 mt-20">点击左侧节点查看该时刻的数据快照</div>
          <div v-else>
            <h3 class="font-bold text-slate-800 border-b pb-2 mb-4">{{ activeHistoryDetail.action }} - 数据快照</h3>
            <div class="text-xs text-slate-500 mb-4 bg-white p-3 border rounded shadow-sm">{{ activeHistoryDetail.detail }}</div>
            <template v-if="activeHistoryDetail.snapshot?.type === 'pre_check'">
              <div class="font-bold text-xs text-slate-600 mb-2">环境参数记录</div>
              <Table :columns="envColumns" :dataSource="activeHistoryDetail.snapshot.envData" :pagination="false" size="small" class="vben-schema-table mb-4" bordered>
                <template #bodyCell="{ column, record }"><template v-if="column.dataIndex === 'status'"><Tag :color="record.status==='正常'?'green':'red'">{{ record.status }}</Tag></template></template>
              </Table>
              <div class="font-bold text-xs text-slate-600 mb-2">设备点检记录</div>
              <Table :columns="deviceColumns" :dataSource="activeHistoryDetail.snapshot.deviceData" :pagination="false" size="small" class="vben-schema-table" bordered />
            </template>
            <template v-if="activeHistoryDetail.snapshot?.type === 'recipe_mix'">
              <div class="font-bold text-xs text-slate-600 mb-2">已核对配方列表</div>
              <div v-for="page in activeHistoryDetail.snapshot.recipes" :key="page.id" class="mb-4">
                <Table :columns="recipeColumns" :dataSource="page.items" :pagination="false" size="small" class="vben-schema-table" bordered />
              </div>
              <div class="font-bold text-xs text-slate-600 mb-2">物料领用记录</div>
              <Table :columns="materialColumns" :dataSource="activeHistoryDetail.snapshot.materials" :pagination="false" size="small" class="vben-schema-table" bordered />
            </template>
          </div>
        </div>
      </div>
    </Drawer>
  </div>
</template>

<style scoped>
.compact-tabs :deep(.ant-tabs-nav) { margin-bottom: 0 !important; }
.compact-tabs :deep(.ant-tabs-nav-list) { background: #f1f5f9; padding: 2px; border-radius: 6px; }
.compact-tabs :deep(.ant-tabs-tab) { padding: 4px 16px !important; margin: 0 !important; border-radius: 4px !important; transition: all 0.2s; font-size: 11px; }
.compact-tabs :deep(.ant-tabs-tab-active) { background: #ffffff !important; box-shadow: 0 1px 3px rgba(0,0,0,0.1); }
.compact-tabs :deep(.ant-tabs-ink-bar) { display: none; }

.vben-schema-table :deep(.ant-table-wrapper), .vben-schema-table :deep(.ant-spin-nested-loading), .vben-schema-table :deep(.ant-spin-container), .vben-schema-table :deep(.ant-table) { height: 100%; display: flex; flex-direction: column; }
.vben-schema-table :deep(.ant-table-thead > tr > th) { background: #f8fafc !important; color: #64748b !important; font-size: 11px; font-weight: bold; padding: 6px 8px !important; }
.vben-schema-table :deep(.ant-table-cell) { padding: 6px 8px !important; font-size: 12px; }

::-webkit-scrollbar { width: 6px; height: 6px; }
::-webkit-scrollbar-thumb { background: #cbd5e1; border-radius: 4px; }
</style>
