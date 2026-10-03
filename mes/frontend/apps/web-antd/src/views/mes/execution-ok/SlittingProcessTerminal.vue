<script lang="ts" setup>
import { ref, watch, computed } from 'vue';
import {
  Button, Card, Tag, Input, InputNumber, Table, Tabs,
  message, Modal, Select, Divider, Drawer, Timeline, Form, DatePicker, Row, Col, Progress
} from 'ant-design-vue';
import type { TableColumnsType } from 'ant-design-vue';
import { IconifyIcon } from '@vben/icons';

const props = defineProps(['operator', 'order', 'process']);
const emit = defineEmits(['close']);
const clone = (data: any) => JSON.parse(JSON.stringify(data));

// ==================================================================================
// 核心状态机定义
// ==================================================================================
const workOrderStatus = ref(props.order.status || '未开工');
const activeTabRun = ref('1');

const incomingRoll = ref<any>(null);
const parentBatchNo = ref('');
const startJobTime = ref<string | null>(null);

const historyDrawerVisible = ref(false);
const historyList = ref<any[]>([]);

function recordHistory(action: string, detail: string, snapshot: any = null) {
  historyList.value.unshift({ time: new Date().toLocaleString(), status: workOrderStatus.value, action, user: props.operator.userName, detail, snapshot });
}

// ==================================================================================
// 1. 未开工 (工单/领料扫描)
// ==================================================================================
const rollScanCode = ref('');

watch(rollScanCode, (val) => {
  if (val && val.length >= 9) {
    message.loading('解析前序复合基材条码...', 0.5).then(() => {
      parentBatchNo.value = 'TAPE-' + val.slice(-5);
      incomingRoll.value = {
        code: val,
        batch: parentBatchNo.value,
        product: 'T01_Film_双面胶基材',
        totalLength: 2000,
        usedLength: 0,
        wasteLength: 0,
        source: '半成品库'
      };
      message.success(`原材料 ${parentBatchNo.value} (2000m) 扫描就绪`);
      rollScanCode.value = '';
    });
  }
});

function handleStartJob() {
  if (!incomingRoll.value) return message.warning('请先扫描领用待分切母卷！');
  if (!startJobTime.value) return message.warning('请选择实际开工时间！');

  workOrderStatus.value = '作业前准备';
  recordHistory('分切领料开机', `领用母卷: ${parentBatchNo.value}, 总长: ${incomingRoll.value.totalLength}m`);
}

// ==================================================================================
// 2. 作业前准备 (分切产线确认)
// ==================================================================================
const prepData = ref([
  { item: '核对工单规格', req: props.order.spec, result: '未确认' },
  { item: '分切刀具安装检查', req: '刀片无缺损，间距准确', result: '未确认' },
  { item: '张力控制器归零', req: '表盘显示0', result: '未确认' }
]);
const prepColumns: TableColumnsType = [
  { title: '准备项目', dataIndex: 'item' }, { title: '标准/要求', dataIndex: 'req' },
  { title: '确认状态', dataIndex: 'result', width: 150 }
];

const isPreCheckComplete = computed(() => prepData.value.every(d => d.result === '正常'));

function finishPreCheck() {
  recordHistory('作业准备完成', '刀具及设备确认完毕，开始分切', { type: 'pre', data: clone(prepData.value) });
  workOrderStatus.value = '作业中';
}

// ==================================================================================
// 3. 作业中 (单品裁切追踪与赋码 + 异常剔除)
// ==================================================================================
const remainingLength = computed(() => {
  if (!incomingRoll.value) return 0;
  return incomingRoll.value.totalLength - incomingRoll.value.usedLength - incomingRoll.value.wasteLength;
});
const progressPercent = computed(() => incomingRoll.value ? Number((((incomingRoll.value.usedLength + incomingRoll.value.wasteLength) / incomingRoll.value.totalLength) * 100).toFixed(1)) : 0);

// 💡 3.1 单品裁切记录 (生成批次与单片打印)
const pieceForm = ref({ length: null });
const pieceLogs = ref<any[]>([]);
const pieceColumns: TableColumnsType = [
  { title: '单品追溯码 (片/垛)', dataIndex: 'pieceCode', width: 220 },
  { title: '消耗母卷长 (m)', dataIndex: 'length', align: 'right', width: 120 },
  { title: '产出时间', dataIndex: 'time', width: 160 },
  { title: '打印状态', dataIndex: 'status', width: 100, align: 'center' },
  { title: '操作人', dataIndex: 'user', width: 100 },
  { title: '操作 (单件打印)', dataIndex: 'action', width: 120, align: 'center' }
];

function handleGeneratePiece() {
  if (!pieceForm.value.length || pieceForm.value.length <= 0) return message.warning('消耗米数必须大于0');
  if (pieceForm.value.length > remainingLength.value) return message.warning(`不能大于剩余量 ${remainingLength.value}m`);

  // 继承母批次并生成单片流水号 P0001, P0002...
  const seq = String(pieceLogs.value.length + 1).padStart(4, '0');
  const pieceCode = `${parentBatchNo.value}-P${seq}`;

  pieceLogs.value.unshift({
    id: Date.now(),
    pieceCode,
    length: pieceForm.value.length,
    time: new Date().toLocaleTimeString(),
    status: '待打印',
    user: props.operator.userName
  });

  incomingRoll.value.usedLength += pieceForm.value.length;
  recordHistory('单片产出', `生成单品追溯码: ${pieceCode}`);
  pieceForm.value.length = null;
}

function handlePrintPiece(record: any) {
  Modal.info({
    title: '单品条码打印 (Zebra USB)',
    content: `正在输出单品标签...\n\n单品追溯码: ${record.pieceCode}\n规格: ${props.order.spec}`,
    onOk: () => {
      record.status = '已打印';
      message.success(`${record.pieceCode} 标签已打印`);
    }
  });
}

// 3.2 异常切除 (中间废料剔除)
const wasteForm = ref({ length: null, reason: '涂布不均/破损' });
const wasteLogs = ref<any[]>([]);
const wasteColumns: TableColumnsType = [
  { title: '异常剔除米数 (m)', dataIndex: 'length', align: 'right', width: 150 },
  { title: '异常原因', dataIndex: 'reason' },
  { title: '剔除时间', dataIndex: 'time', width: 160 },
  { title: '操作人', dataIndex: 'user', width: 100 }
];

function handleAddWasteLog() {
  if (!wasteForm.value.length || wasteForm.value.length <= 0) return message.warning('剔除米数必须大于0');
  if (wasteForm.value.length > remainingLength.value) return message.warning(`不能大于剩余量 ${remainingLength.value}m`);

  wasteLogs.value.unshift({ id: Date.now(), length: wasteForm.value.length, reason: wasteForm.value.reason, time: new Date().toLocaleTimeString(), user: props.operator.userName });
  incomingRoll.value.wasteLength += wasteForm.value.length;
  recordHistory('异常切除', `剔除中间废料: ${wasteForm.value.length}m, 原因: ${wasteForm.value.reason}`);
  wasteForm.value.length = null;
}

// ==================================================================================
// 4. 完工报工与余料退库
// ==================================================================================
const finishModalVisible = ref(false);
const finishForm = ref({ goodQty: 0, badQty: null, returnRoll: false, returnLength: 0 });

function handleFinishClick() {
  // 自动汇总已生成的单品数量
  finishForm.value.goodQty = pieceLogs.value.length;
  finishForm.value.returnLength = remainingLength.value;
  finishForm.value.returnRoll = remainingLength.value > 0;
  finishModalVisible.value = true;
}

function submitFinish() {
  const detailStr = `分切汇总: 合格单品 ${finishForm.value.goodQty} PCS, 不良 ${finishForm.value.badQty || 0} PCS`
    + (finishForm.value.returnRoll ? `, 余卷退料回库: ${finishForm.value.returnLength}m` : '');

  recordHistory('完工报工与结单', detailStr, { type: 'finish', data: clone(finishForm.value) });

  if (finishForm.value.returnRoll) {
    Modal.success({
      title: '余卷退库条码打印',
      content: `检测到余料回库动作。\n已发送退库条码至打印机：${parentBatchNo.value}-RET (长度: ${finishForm.value.returnLength}m)`,
      onOk: closeTerminal
    });
  } else {
    message.success('完工报工成功');
    closeTerminal();
  }
}

function closeTerminal() {
  workOrderStatus.value = '完成作业';
  finishModalVisible.value = false;
  setTimeout(() => emit('close'), 500);
}
</script>

<template>
  <div class="flex flex-col absolute inset-0 bg-[#f4f6f8] overflow-hidden">

    <header class="flex h-14 shrink-0 items-center justify-between border-b bg-white px-4 shadow-sm z-10">
      <div class="flex items-center gap-6">
        <div v-for="label in ['未开工', '作业前准备', '作业中', '完成作业']" :key="label"
             :class="['flex items-center gap-2 font-bold text-xs transition-colors', workOrderStatus === label ? 'text-blue-700 scale-105' : 'text-slate-400']">
          <IconifyIcon :icon="workOrderStatus === label ? 'lucide:circle-dot' : 'lucide:circle'" />
          <span>{{ label }}</span>
        </div>
      </div>
      <div class="flex items-center gap-4">
        <span v-if="parentBatchNo" class="text-xs font-mono font-bold text-blue-700 bg-blue-50 px-3 py-1 rounded-full border border-blue-100">母卷: {{ parentBatchNo }}</span>
        <Tabs v-if="workOrderStatus === '作业中'" v-model:activeKey="activeTabRun" type="capsule" size="small" class="compact-tabs">
          <Tabs.TabPane key="1" tab="单品分切与赋码" />
          <Tabs.TabPane key="2" tab="【异常】中间废料切除" />
        </Tabs>
      </div>
    </header>

    <main class="flex-1 flex flex-col min-h-0 p-3 overflow-hidden">

      <div v-if="workOrderStatus === '未开工'" class="flex-1 flex items-center justify-center">
        <Card class="w-[500px] border-none shadow-xl rounded-2xl p-4 bg-white">
          <div class="flex flex-col items-center mb-4">
            <div class="w-16 h-16 bg-blue-50 rounded-full flex items-center justify-center text-blue-600 mb-2"><IconifyIcon icon="lucide:scissors" class="text-3xl" /></div>
            <h2 class="text-lg font-black text-slate-800">裁切分片 开工准备</h2>
          </div>

          <div class="bg-indigo-900 p-4 rounded-xl mb-4 shadow-inner">
            <div class="text-indigo-200 text-xs font-bold mb-2">1. 领用扫描：扫描待裁切母卷条码</div>
            <Input v-model:value="rollScanCode" size="large" placeholder="扫码提取上游母卷..." class="!bg-white/10 !text-white !border-indigo-700" />
            <div v-if="incomingRoll" class="mt-4 bg-white/10 p-3 rounded text-xs text-indigo-100 font-mono space-y-1">
              <div class="text-white font-bold mb-1 border-b border-white/20 pb-1">母卷加载成功</div>
              <div>母卷批次：{{ incomingRoll.batch }}</div>
              <div>额定卷长：<span class="text-green-400 font-bold text-sm">{{ incomingRoll.totalLength }} m</span></div>
            </div>
          </div>

          <div class="bg-slate-50 p-4 rounded-xl border border-slate-100 mb-4">
            <div class="text-xs font-bold text-slate-600 mb-2">2. 选择开工时间</div>
            <DatePicker v-model:value="startJobTime" show-time value-format="YYYY-MM-DD HH:mm:ss" :inputReadOnly="true" size="large" class="w-full" />
          </div>
          <Button type="primary" size="large" block class="h-12 rounded-xl bg-blue-600 font-bold" @click="handleStartJob">确认领用并开机</Button>
        </Card>
      </div>

      <div v-if="workOrderStatus === '作业前准备'" class="flex-1 flex flex-col bg-white border border-slate-200 rounded-lg overflow-hidden shadow-sm">
        <div class="p-3 bg-slate-50 border-b text-xs font-bold text-slate-600 flex items-center gap-2 shrink-0">
          <IconifyIcon icon="lucide:check-square" /> 裁切机开机前确认项
        </div>
        <Table :columns="prepColumns" :dataSource="prepData" :pagination="false" size="small" class="vben-schema-table flex-1">
          <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'result'">
              <Select v-model:value="record.result" size="small" class="w-24">
                <Select.Option value="未确认">未确认</Select.Option><Select.Option value="正常">正常</Select.Option>
              </Select>
            </template>
          </template>
        </Table>
      </div>

      <div v-if="workOrderStatus === '作业中'" class="flex-1 flex flex-col gap-3">
        <Card size="small" class="bg-white border-slate-200 shadow-sm shrink-0">
          <div class="flex items-center justify-between">
            <div class="flex items-center gap-6">
              <div><span class="text-xs text-slate-400">分切母卷</span><div class="font-mono font-bold text-blue-700 text-lg">{{ parentBatchNo }}</div></div>
              <Divider type="vertical" class="h-8" />
              <div><span class="text-xs text-slate-400">初始总长</span><div class="font-bold text-slate-700 text-lg">{{ incomingRoll.totalLength }} <span class="text-xs">m</span></div></div>
              <div><span class="text-xs text-slate-400">单品消耗分摊</span><div class="font-bold text-green-600 text-lg">{{ incomingRoll.usedLength }} <span class="text-xs">m</span></div></div>
              <div><span class="text-xs text-slate-400">异常剔除废料</span><div class="font-bold text-red-500 text-lg">{{ incomingRoll.wasteLength }} <span class="text-xs">m</span></div></div>
              <div><span class="text-xs text-slate-400">当前剩余卷长</span><div class="font-bold text-orange-500 text-lg">{{ remainingLength }} <span class="text-xs">m</span></div></div>
            </div>
            <div class="w-64">
              <div class="text-[10px] text-slate-500 flex justify-between mb-1"><span>卷材消耗进度</span><span>{{ progressPercent }}%</span></div>
              <Progress :percent="progressPercent" :showInfo="false" strokeColor="#2563eb" size="small" />
            </div>
          </div>
        </Card>

        <div v-if="activeTabRun === '1'" class="flex-1 flex flex-col min-h-0 bg-white border border-slate-200 rounded-lg overflow-hidden shadow-sm">
          <div class="bg-indigo-50 p-3 border-b border-indigo-200 flex items-end gap-3 shrink-0">
            <Form.Item label="本次切片消耗母卷长度 (m)" class="mb-0 font-bold text-indigo-800">
              <InputNumber v-model:value="pieceForm.length" size="large" class="w-48 shadow-sm" :min="1" :max="remainingLength" />
            </Form.Item>
            <div class="flex-1 text-xs text-indigo-500 mb-2">填写长度后生成独立单品追溯码，并继承母卷批次。</div>
            <Button type="primary" size="large" class="bg-indigo-600 font-bold px-6 shadow-md h-[40px] mb-[1px]" :disabled="remainingLength <= 0" @click="handleGeneratePiece">
              <IconifyIcon icon="lucide:split-square-vertical" class="mr-2" /> 生成单品记录
            </Button>
          </div>
          <Table :columns="pieceColumns" :dataSource="pieceLogs" :pagination="false" size="small" :scroll="{ y: 'calc(100vh - 350px)' }" class="vben-schema-table flex-1">
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'pieceCode'">
                <span class="font-mono font-bold text-indigo-700">{{ record.pieceCode }}</span>
              </template>
              <template v-if="column.dataIndex === 'status'">
                <Tag :color="record.status === '已打印' ? 'green' : 'orange'">{{ record.status }}</Tag>
              </template>
              <template v-if="column.dataIndex === 'action'">
                <Button type="link" size="small" @click="handlePrintPiece(record)">
                  <IconifyIcon icon="lucide:printer" class="mr-1" />单品打印
                </Button>
              </template>
            </template>
            <template #emptyText><div class="py-10 text-slate-400">暂无单品记录，请在上侧录入消耗长度生成。</div></template>
          </Table>
        </div>

        <div v-if="activeTabRun === '2'" class="flex-1 flex flex-col min-h-0 bg-white border border-red-200 rounded-lg overflow-hidden shadow-sm">
          <div class="bg-red-50 p-3 border-b border-red-200 flex items-end gap-3 shrink-0">
            <Form.Item label="中间废料剔除长度 (m)" class="mb-0 font-bold text-red-800">
              <InputNumber v-model:value="wasteForm.length" size="large" class="w-48 shadow-sm" :min="1" :max="remainingLength" />
            </Form.Item>
            <Form.Item label="异常原因分类" class="mb-0 font-bold text-red-800 flex-1">
              <Select v-model:value="wasteForm.reason" size="large" class="w-full shadow-sm">
                <Select.Option value="涂布不均/破损">涂布不均/破损</Select.Option>
                <Select.Option value="接头部分">接头部分剔除</Select.Option>
                <Select.Option value="起皱/折痕">起皱/折痕</Select.Option>
              </Select>
            </Form.Item>
            <Button danger type="primary" size="large" class="font-bold px-6 shadow-md h-[40px] mb-[1px]" :disabled="remainingLength <= 0" @click="handleAddWasteLog">执行异常切除记账</Button>
          </div>
          <Table :columns="wasteColumns" :dataSource="wasteLogs" :pagination="false" size="small" :scroll="{ y: 'calc(100vh - 350px)' }" class="vben-schema-table flex-1" />
        </div>
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
          <Button @click="prepData.forEach(p=>p.result='正常')" class="border-blue-200 text-blue-600">一键确认设备就绪</Button>
          <Button type="primary" class="bg-blue-600 font-bold px-8" :disabled="!isPreCheckComplete" @click="finishPreCheck">开始执行分切</Button>
        </template>

        <template v-if="workOrderStatus === '作业中'">
          <Button type="primary" class="bg-green-600 border-none font-bold px-8 shadow-lg shadow-green-100" @click="handleFinishClick">
            裁切完工报工汇总
          </Button>
        </template>
      </div>
    </footer>

    <Modal v-model:open="finishModalVisible" title="完工报工 / 退库处理" @ok="submitFinish" width="600px">
      <Form layout="vertical" class="mt-4">
        <div class="bg-blue-50 text-blue-600 p-2 rounded mb-4 text-xs">
          系统已自动将本次作业生成的单品记录汇总至“合格产出”。
        </div>
        <Row :gutter="16">
          <Col :span="12">
            <Form.Item label="合格单片总数 (PCS)">
              <InputNumber v-model:value="finishForm.goodQty" size="large" class="w-full font-bold text-green-600" />
            </Form.Item>
          </Col>
          <Col :span="12">
            <Form.Item label="报废/不良数量 (PCS)">
              <InputNumber v-model:value="finishForm.badQty" size="large" class="w-full text-red-500" />
            </Form.Item>
          </Col>
        </Row>

        <div class="mt-4 p-4 border border-orange-200 bg-orange-50 rounded-xl">
          <div class="flex items-center justify-between mb-2">
            <span class="font-bold text-orange-800">【异常】退料 / 余卷回库</span>
            <Tag color="orange">剩余未切长度: {{ remainingLength }}m</Tag>
          </div>
          <div class="text-xs text-orange-600 mb-4">母卷尚未完全消耗完毕。如果提前结单完工，请确认余卷退料长度，系统将打印退料标签并将其退回线边库。</div>
          <Form.Item label="退回库房长度 (m)" class="mb-0">
            <InputNumber v-model:value="finishForm.returnLength" class="w-full" :disabled="!finishForm.returnRoll" />
          </Form.Item>
        </div>
      </Form>
    </Modal>

    <Drawer v-model:open="historyDrawerVisible" title="工序追踪体系" placement="right" width="400">
      <Timeline>
        <Timeline.Item v-for="(log, idx) in historyList" :key="idx" :color="idx === 0 ? 'blue' : 'gray'">
          <div class="font-bold mb-1 text-slate-700">{{ log.action }}</div>
          <div class="text-xs text-slate-500 mb-1">{{ log.time }} / {{ log.user }}</div>
          <div class="text-xs bg-slate-50 p-2 rounded border border-slate-100">{{ log.detail }}</div>
        </Timeline.Item>
      </Timeline>
    </Drawer>
  </div>
</template>

<style scoped>
/* 样式部分保持一致 */
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
