<script lang="ts" setup>
import { ref, watch, computed } from 'vue';
import {
  Button, Card, Tag, Input, InputNumber, Table, Tabs,
  message, Modal, Select, Divider, Drawer, Timeline, Form, DatePicker, Progress
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
const activeTabPre = ref('1');

const incomingRoll = ref<any>(null);
const parentBatchNo = ref('');
const startJobTime = ref<string | null>(null);

const historyDrawerVisible = ref(false);
const historyList = ref<any[]>([]);

function recordHistory(action: string, detail: string, snapshot: any = null) {
  historyList.value.unshift({ time: new Date().toLocaleString(), status: workOrderStatus.value, action, user: props.operator.userName, detail, snapshot });
}

// ==================================================================================
// 1. 未开工 (领用扫描：从粗磨下来的原材料)
// ==================================================================================
const rollScanCode = ref('');

watch(rollScanCode, (val) => {
  if (val && val.length >= 9) {
    message.loading('校验前序原材料...', 0.5).then(() => {
      // 模拟扫码识别到了粗磨工序的产出卷
      parentBatchNo.value = 'R-GRIND-' + val.slice(-4);
      incomingRoll.value = {
        code: val,
        batch: parentBatchNo.value,
        product: 'T01_Film_粗磨膜',
        totalLength: 100, // 精磨输入卷通常较短
        usedLength: 0,
        source: '粗磨工序(半成品库)'
      };
      message.success(`原材料卷 ${parentBatchNo.value} 扫描就绪`);
      rollScanCode.value = '';
    });
  }
});

function handleStartJob() {
  if (!incomingRoll.value) return message.warning('请先扫描领用原材料卷！');
  if (!startJobTime.value) return message.warning('请选择实际开工时间！');

  workOrderStatus.value = '作业前准备';
  activeTabPre.value = '1';
  recordHistory('精磨开机', `领用原材料: ${parentBatchNo.value}, 长度: ${incomingRoll.value.totalLength}m`);
}

// ==================================================================================
// 2. 作业前准备 (环境点检 + 参数记录)
// 注意：按需求描述，精磨没有设备清洁点检，只有环境和参数。
// ==================================================================================
const envParams = ref([
  { id: 'E01', name: '环境温度', ref: '20-25', unit: '℃', val: null, status: '未检', time: '--' },
  { id: 'E02', name: '环境湿度', ref: '40-60', unit: '%RH', val: null, status: '未检', time: '--' }
]);
const envColumns: TableColumnsType = [
  { title: '编号', dataIndex: 'id', width: 80 }, { title: '参数名称', dataIndex: 'name' },
  { title: '参考值', dataIndex: 'ref' }, { title: '单位', dataIndex: 'unit', width: 80 },
  { title: '参数值', dataIndex: 'val', width: 120 }, { title: '状态', dataIndex: 'status', width: 100 },
  { title: '最后更新', dataIndex: 'time' }
];

const startupParams = ref([
  { item: '精磨厚度', req: '0.02-0.03', unit: 'mm', val: null },
  { item: '运行线速', req: '5-8', unit: 'm/min', val: null },
  { item: '精磨间隙', req: '0.05', unit: 'mm', val: null },
  { item: '抛光转速', req: '2000', unit: 'rpm', val: null }
]);
const startupColumns: TableColumnsType = [
  { title: '参数名称', dataIndex: 'item' }, { title: '工艺要求', dataIndex: 'req' },
  { title: '单位', dataIndex: 'unit', width: 80 }, { title: '实际记录值', dataIndex: 'val', width: 150 }
];

const isPreCheckComplete = computed(() => envParams.value.every(p => p.status !== '未检') && startupParams.value.every(p => p.val !== null));

function finishPreCheck() {
  recordHistory('准备完成', '环境确认及参数打底完毕', { type: 'pre', env: clone(envParams.value), param: clone(startupParams.value) });
  workOrderStatus.value = '作业中';
}

// ==================================================================================
// 3. 作业中 (拆分不同规格 + 单件追溯赋码 + 库存转移分流)
// ==================================================================================
const subBatchForm = ref({ length: null, spec: 'T01-A (高透)', routing: '半成品库' });
const subBatches = ref<any[]>([]);

const remainingLength = computed(() => incomingRoll.value ? incomingRoll.value.totalLength - incomingRoll.value.usedLength : 0);
const progressPercent = computed(() => incomingRoll.value ? Number(((incomingRoll.value.usedLength / incomingRoll.value.totalLength) * 100).toFixed(1)) : 0);

const subBatchColumns: TableColumnsType = [
  { title: '单件追溯码 (条码)', dataIndex: 'subBatchNo', width: 220 },
  { title: '分卷长度 (m)', dataIndex: 'length', align: 'right', width: 120 },
  { title: '产出规格', dataIndex: 'spec', width: 140 },
  { title: '库存流向', dataIndex: 'routing', width: 120 },
  { title: '报工时间', dataIndex: 'time', width: 150 },
  { title: '操作 (单件打印)', dataIndex: 'action', width: 120, align: 'center' }
];

function handleGenerateSubBatch() {
  if (!subBatchForm.value.length || subBatchForm.value.length <= 0) return message.warning('产出长度必须大于0');
  if (subBatchForm.value.length > remainingLength.value) return message.warning(`不能大于剩余量 ${remainingLength.value}m`);

  const sequence = String(subBatches.value.length + 1).padStart(3, '0');
  const routingSuffix = subBatchForm.value.routing === '研发样品库' ? '-RD' : '-WIP';
  const subBatchNo = `${parentBatchNo.value}-F${sequence}${routingSuffix}`; // F代表Fine, 带流向后缀

  subBatches.value.unshift({
    id: Date.now(),
    subBatchNo,
    length: subBatchForm.value.length,
    spec: subBatchForm.value.spec,
    routing: subBatchForm.value.routing,
    time: new Date().toLocaleTimeString(),
    status: '已报工'
  });

  incomingRoll.value.usedLength += subBatchForm.value.length;
  recordHistory('分卷拆分', `生成: ${subBatchNo}, 规格: ${subBatchForm.value.spec}, 流向: ${subBatchForm.value.routing}`);
  message.success(`成功生成单件批次: ${subBatchNo}`);
}

function handlePrintSubBatch(record: any) {
  Modal.info({
    title: '单件追溯标签打印 (入库标签)',
    content: `[Zebra USB]\n追溯码: ${record.subBatchNo}\n规格: ${record.spec}\n长度: ${record.length}m\n流向: ${record.routing}`
  });
  record.status = '已打印';
}

// ==================================================================================
// 4. 库存转移与完工
// ==================================================================================
function handleFinishJob() {
  if (remainingLength.value > 0) {
    return message.warning(`当前母卷还有 ${remainingLength.value}m 未进行分段拆分处理，请先处理余料！`);
  }

  Modal.confirm({
    title: '精磨完工与库存转移',
    content: `确认将已产出的 ${subBatches.value.length} 个单件按流向（半成品库/研发库）执行库存转移并结单？`,
    onOk: () => {
      recordHistory('完工与转移', `产出 ${subBatches.value.length} 卷，已触发系统库存转移指令`);
      workOrderStatus.value = '完成作业';
      message.success('精磨完工，库存转移已执行！');
      setTimeout(() => emit('close'), 1500);
    }
  });
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
        <span v-if="parentBatchNo" class="text-xs font-mono font-bold text-blue-700 bg-blue-50 px-3 py-1 rounded-full border border-blue-100">母批次: {{ parentBatchNo }}</span>
        <Tabs v-if="workOrderStatus === '作业前准备'" v-model:activeKey="activeTabPre" type="capsule" size="small" class="compact-tabs">
          <Tabs.TabPane key="1" tab="环境点检" />
          <Tabs.TabPane key="2" tab="工艺参数记录" />
        </Tabs>
      </div>
    </header>

    <main class="flex-1 flex flex-col min-h-0 p-3 overflow-hidden">

      <div v-if="workOrderStatus === '未开工'" class="flex-1 flex items-center justify-center">
        <Card class="w-[500px] border-none shadow-xl rounded-2xl p-4 bg-white">
          <div class="flex flex-col items-center mb-4">
            <div class="w-16 h-16 bg-blue-50 rounded-full flex items-center justify-center text-blue-600 mb-2"><IconifyIcon icon="lucide:scan-face" class="text-3xl" /></div>
            <h2 class="text-lg font-black text-slate-800">精磨工艺开工准备</h2>
          </div>

          <div class="bg-indigo-900 p-4 rounded-xl mb-4 shadow-inner">
            <div class="text-indigo-200 text-xs font-bold mb-2">1. 领用扫描：请扫描原材料(粗磨产出)</div>
            <Input v-model:value="rollScanCode" size="large" placeholder="扫码提取前序批次..." class="!bg-white/10 !text-white !border-indigo-700" />
            <div v-if="incomingRoll" class="mt-4 bg-white/10 p-3 rounded text-xs text-indigo-100 font-mono space-y-1">
              <div class="text-white font-bold mb-1 border-b border-white/20 pb-1">原材料加载成功</div>
              <div>原料批次：{{ incomingRoll.batch }}</div>
              <div>原料长度：<span class="text-green-400 font-bold text-sm">{{ incomingRoll.totalLength }} m</span></div>
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
        <Table v-if="activeTabPre === '1'" :columns="envColumns" :dataSource="envParams" :pagination="false" size="small" class="vben-schema-table h-full">
          <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'val'"><InputNumber v-model:value="record.val" size="small" @change="record.status='未检'"/></template>
            <template v-else-if="column.dataIndex === 'status'"><Tag :color="record.status==='正常'?'green':'red'">{{ record.status }}</Tag></template>
          </template>
        </Table>
        <Table v-if="activeTabPre === '2'" :columns="startupColumns" :dataSource="startupParams" :pagination="false" size="small" class="vben-schema-table h-full">
          <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'val'"><InputNumber v-model:value="record.val" size="small" class="w-32" placeholder="录入参数" /></template>
          </template>
        </Table>
      </div>

      <div v-if="workOrderStatus === '作业中'" class="flex-1 flex flex-col gap-3">
        <Card size="small" class="bg-white border-slate-200 shadow-sm shrink-0">
          <div class="flex items-center justify-between">
            <div class="flex items-center gap-6">
              <div><span class="text-xs text-slate-400">处理中原料</span><div class="font-mono font-bold text-blue-700 text-lg">{{ parentBatchNo }}</div></div>
              <Divider type="vertical" class="h-8" />
              <div><span class="text-xs text-slate-400">总长度</span><div class="font-bold text-slate-700 text-lg">{{ incomingRoll.totalLength }} <span class="text-xs">m</span></div></div>
              <div><span class="text-xs text-slate-400">剩余可用</span><div class="font-bold text-orange-500 text-lg">{{ remainingLength }} <span class="text-xs">m</span></div></div>
            </div>
            <div class="w-64">
              <div class="text-[10px] text-slate-500 flex justify-between mb-1"><span>原料消化进度</span><span>{{ progressPercent }}%</span></div>
              <Progress :percent="progressPercent" :showInfo="false" strokeColor="#2563eb" size="small" />
            </div>
          </div>
        </Card>

        <div class="bg-indigo-50 border border-indigo-200 p-3 rounded-lg flex items-end gap-3 shrink-0 shadow-sm">
          <Form.Item label="本次分段长度 (m)" class="mb-0 font-bold text-indigo-900">
            <InputNumber v-model:value="subBatchForm.length" size="large" class="w-32 shadow-sm" :min="1" :max="remainingLength" />
          </Form.Item>
          <Form.Item label="产出规格型号" class="mb-0 font-bold text-indigo-900">
            <Select v-model:value="subBatchForm.spec" size="large" class="w-40 shadow-sm">
              <Select.Option value="T01-A (高透)">T01-A (高透)</Select.Option>
              <Select.Option value="T01-B (雾面)">T01-B (雾面)</Select.Option>
              <Select.Option value="T01-C (试验)">T01-C (试验)</Select.Option>
            </Select>
          </Form.Item>
          <Form.Item label="库存转移流向" class="mb-0 flex-1 font-bold text-indigo-900">
            <Select v-model:value="subBatchForm.routing" size="large" class="w-48 shadow-sm">
              <Select.Option value="半成品库">半成品库 (入库码 WIP)</Select.Option>
              <Select.Option value="研发样品库">研发样品库 (入库码 RD)</Select.Option>
            </Select>
          </Form.Item>
          <Button type="primary" size="large" class="bg-indigo-600 font-bold px-6 shadow-md h-[40px] mb-[1px]" :disabled="remainingLength <= 0" @click="handleGenerateSubBatch">
            <IconifyIcon icon="lucide:split-square-horizontal" class="mr-2" /> 拆分生成单件
          </Button>
        </div>

        <div class="flex-1 bg-white border border-slate-200 rounded-lg overflow-hidden shadow-sm flex flex-col min-h-0">
          <div class="p-2 bg-slate-50 border-b text-xs font-bold text-slate-600 flex items-center gap-2">
            <IconifyIcon icon="lucide:network" /> 单件追溯与库存转移明细
          </div>
          <Table :columns="subBatchColumns" :dataSource="subBatches" :pagination="false" size="small" :scroll="{ y: 'calc(100vh - 380px)' }" class="vben-schema-table flex-1">
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'subBatchNo'">
                <span class="font-mono font-bold text-indigo-700">{{ record.subBatchNo }}</span>
              </template>
              <template v-if="column.dataIndex === 'routing'">
                <Tag :color="record.routing === '半成品库' ? 'blue' : 'purple'">{{ record.routing }}</Tag>
              </template>
              <template v-if="column.dataIndex === 'action'">
                <Button type="link" size="small" @click="handlePrintSubBatch(record)">打印入库签</Button>
              </template>
            </template>
            <template #emptyText><div class="py-10 text-slate-400">暂无单件产出，请在上方录入规格与流向并生成。</div></template>
          </Table>
        </div>
      </div>

    </main>

    <footer class="flex h-14 shrink-0 items-center justify-between border-t bg-white px-4 shadow-[0_-2px_8px_rgba(0,0,0,0.05)] z-20">
      <div class="flex items-center gap-4">
        <Button @click="emit('close')" class="font-bold text-slate-600 border-slate-300">退出终端</Button>
        <Divider type="vertical" />
        <Button type="link" class="p-0 font-bold text-blue-600" @click="historyDrawerVisible = true">工序执行历史</Button>
      </div>

      <div class="flex gap-3 items-center">
        <template v-if="workOrderStatus === '作业前准备'">
          <Button @click="envParams.forEach(p=>p.status='正常')" class="border-blue-200 text-blue-600">环境点检正常</Button>
          <Button type="primary" class="bg-blue-600 font-bold px-8" :disabled="!isPreCheckComplete" @click="finishPreCheck">确认参数并开机</Button>
        </template>

        <template v-if="workOrderStatus === '作业中'">
          <Button type="primary" class="bg-green-600 border-none font-bold px-8 shadow-lg shadow-green-100" @click="handleFinishJob">
            执行转移并结单
          </Button>
        </template>
      </div>
    </footer>

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
