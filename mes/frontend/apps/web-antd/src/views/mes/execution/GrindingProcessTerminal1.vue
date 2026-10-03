<script lang="ts" setup>
import { ref, watch, computed } from 'vue';
import {
  Button, Card, Tag, Input, InputNumber, Table, Tabs,
  message, Modal, Select, Divider, Drawer, Timeline, Form, DatePicker, Space, Progress
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

// 母卷信息 (通过扫码获取)
const incomingRoll = ref<any>(null);
const parentBatchNo = ref('');
const startJobTime = ref<string | null>(null);

// 历史记录
const historyDrawerVisible = ref(false);
const historyList = ref<any[]>([]);

function recordHistory(action: string, detail: string, snapshot: any = null) {
  historyList.value.unshift({ time: new Date().toLocaleString(), status: workOrderStatus.value, action, user: props.operator.userName, detail, snapshot });
}

// ==================================================================================
// 1. 未开工 (领用扫描半成品库湿法母卷)
// ==================================================================================
const rollScanCode = ref('');

watch(rollScanCode, (val) => {
  if (val && val.length >= 9) {
    message.loading('校验半成品库库存...', 0.5).then(() => {
      // 模拟扫码识别到了上一道湿法工序的母卷
      parentBatchNo.value = 'COAT-' + val.slice(-6);
      incomingRoll.value = {
        code: val,
        batch: parentBatchNo.value,
        product: 'T01_Film_湿法膜',
        totalLength: 500, // 扫码带出500米
        usedLength: 0,
        source: '湿法线'
      };
      message.success(`母卷 ${parentBatchNo.value} (500m) 扫描就绪`);
      rollScanCode.value = '';
    });
  }
});

function handleStartJob() {
  if (!incomingRoll.value) return message.warning('请先扫描领用前工序半成品母卷！');
  if (!startJobTime.value) return message.warning('请选择实际开工时间！');

  workOrderStatus.value = '作业前准备';
  activeTabPre.value = '1';
  recordHistory('粗磨开机', `领用母卷: ${parentBatchNo.value}, 长度: ${incomingRoll.value.totalLength}m`);
}

// ==================================================================================
// 2. 作业前准备 (点检 + 工艺参数记录)
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

const deviceChecks = ref([
  { device: '粗磨机', item: '设备清洁点检', req: '磨辊无残留颗粒', result: '未点', remark: '', time: '--' },
  { device: '粗磨机', item: '开机点检', req: '吸尘装置运行正常', result: '未点', remark: '', time: '--' }
]);
const deviceColumns: TableColumnsType = [
  { title: '设备名称', dataIndex: 'device' }, { title: '点检项目', dataIndex: 'item' },
  { title: '点检要求', dataIndex: 'req' }, { title: '结果', dataIndex: 'result', width: 120 },
  { title: '备注', dataIndex: 'remark' }, { title: '点检时间', dataIndex: 'time', width: 150 }
];

const startupParams = ref([
  { item: '磨皮厚度', req: '0.05-0.08', unit: 'mm', val: null },
  { item: '运行线速', req: '10-15', unit: 'm/min', val: null },
  { item: '磨辊间隙', req: '0.10', unit: 'mm', val: null },
  { item: '磨辊转速', req: '1500', unit: 'rpm', val: null }
]);
const startupColumns: TableColumnsType = [
  { title: '参数名称', dataIndex: 'item' }, { title: '工艺要求', dataIndex: 'req' },
  { title: '单位', dataIndex: 'unit', width: 80 }, { title: '实际设定/记录值', dataIndex: 'val', width: 150 }
];

const isPreCheckComplete = computed(() =>
  envParams.value.every(p => p.status !== '未检') &&
  deviceChecks.value.every(d => d.result !== '未点') &&
  startupParams.value.every(p => p.val !== null)
);

function finishPreCheck() {
  recordHistory('准备完成', '点检及参数打底完毕', { type: 'pre', env: clone(envParams.value), dev: clone(deviceChecks.value) });
  workOrderStatus.value = '作业中';
}

// ==================================================================================
// 3. 作业中 (分批产出报工列展现)
// ==================================================================================
const subBatchForm = ref({ length: 100, remark: '' });
const subBatches = ref<any[]>([]); // 存放生成的子批次

// 进度与余量计算
const remainingLength = computed(() => incomingRoll.value ? incomingRoll.value.totalLength - incomingRoll.value.usedLength : 0);
const progressPercent = computed(() => incomingRoll.value ? Number(((incomingRoll.value.usedLength / incomingRoll.value.totalLength) * 100).toFixed(1)) : 0);

const subBatchColumns: TableColumnsType = [
  { title: '子批次号', dataIndex: 'subBatchNo', width: 200 },
  { title: '产出长度 (m)', dataIndex: 'length', align: 'right', width: 120 },
  { title: '报工时间', dataIndex: 'time', width: 160 },
  { title: '备注', dataIndex: 'remark' },
  { title: '状态', dataIndex: 'status', width: 100 },
  { title: '操作 (批次打印)', dataIndex: 'action', width: 120, align: 'center' }
];

// 直接在界面录入并生成批次 (无弹窗)
function handleGenerateSubBatch() {
  if (!subBatchForm.value.length || subBatchForm.value.length <= 0) return message.warning('产出长度必须大于0');
  if (subBatchForm.value.length > remainingLength.value) return message.warning(`产出长度不能大于母卷剩余量 ${remainingLength.value}m`);

  const sequence = String(subBatches.value.length + 1).padStart(3, '0');
  const subBatchNo = `${parentBatchNo.value}-${sequence}`; // 继承母批次+流水号

  subBatches.value.unshift({
    id: Date.now(),
    subBatchNo,
    length: subBatchForm.value.length,
    remark: subBatchForm.value.remark,
    time: new Date().toLocaleTimeString(),
    status: '已报工'
  });

  incomingRoll.value.usedLength += subBatchForm.value.length;
  subBatchForm.value.remark = ''; // 重置备注

  recordHistory('分批报工', `生成子卷: ${subBatchNo}, 长度: ${subBatchForm.value.length}m`);
  message.success(`成功生成批次: ${subBatchNo}`);
}

function handlePrintSubBatch(record: any) {
  Modal.info({
    title: '子批次标签打印 (斑马打印机)',
    content: `正在打印卷标...\n批次号: ${record.subBatchNo}\n长度: ${record.length}m`
  });
  record.status = '已打印';
}

// ==================================================================================
// 4. 余料处理与完工
// ==================================================================================
const tailModalVisible = ref(false);
const tailForm = ref({ action: '生成尾卷', length: 0 });

function handleFinishJob() {
  tailForm.value.length = remainingLength.value;
  tailModalVisible.value = true;
}

function submitTailAndFinish() {
  if (tailForm.value.length > 0) {
    if (tailForm.value.action === '生成尾卷') {
      const tailBatch = `${parentBatchNo.value}-TAIL`;
      subBatches.value.unshift({ subBatchNo: tailBatch, length: tailForm.value.length, remark: '尾卷', time: new Date().toLocaleTimeString(), status: '已报工' });
      incomingRoll.value.usedLength += tailForm.value.length;
    }
    recordHistory('尾卷处理', `处理方式: ${tailForm.value.action}, 长度: ${tailForm.value.length}m`);
  }

  workOrderStatus.value = '完成作业';
  tailModalVisible.value = false;
  message.success('粗磨工序全部完工！');
  setTimeout(() => emit('close'), 1500);
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
          <Tabs.TabPane key="1" tab="设备与环境点检" />
          <Tabs.TabPane key="2" tab="工艺参数打底记录" />
        </Tabs>
      </div>
    </header>

    <main class="flex-1 flex flex-col min-h-0 p-3 overflow-hidden">

      <div v-if="workOrderStatus === '未开工'" class="flex-1 flex items-center justify-center">
        <Card class="w-[500px] border-none shadow-xl rounded-2xl p-4 bg-white">
          <div class="flex flex-col items-center mb-4">
            <div class="w-16 h-16 bg-blue-50 rounded-full flex items-center justify-center text-blue-600 mb-2"><IconifyIcon icon="lucide:layers" class="text-3xl" /></div>
            <h2 class="text-lg font-black text-slate-800">粗磨工艺开工准备</h2>
          </div>

          <div class="bg-indigo-900 p-4 rounded-xl mb-4 shadow-inner">
            <div class="text-indigo-200 text-xs font-bold mb-2">1. 领用扫描：请扫描半成品库母卷条码</div>
            <Input v-model:value="rollScanCode" size="large" placeholder="扫码提取前序批次及长度..." class="!bg-white/10 !text-white !border-indigo-700" />
            <div v-if="incomingRoll" class="mt-4 bg-white/10 p-3 rounded text-xs text-indigo-100 font-mono space-y-1">
              <div class="text-white font-bold mb-1 border-b border-white/20 pb-1">母卷加载成功</div>
              <div>母卷批次：{{ incomingRoll.batch }}</div>
              <div>卷轴长度：<span class="text-green-400 font-bold text-sm">{{ incomingRoll.totalLength }} m</span></div>
              <div>流转来源：{{ incomingRoll.source }}</div>
            </div>
          </div>

          <div class="bg-slate-50 p-4 rounded-xl border border-slate-100 mb-4">
            <div class="text-xs font-bold text-slate-600 mb-2">2. 选择开工时间</div>
            <DatePicker v-model:value="startJobTime" show-time value-format="YYYY-MM-DD HH:mm:ss" :inputReadOnly="true" size="large" class="w-full" />
          </div>

          <Button type="primary" size="large" block class="h-12 rounded-xl bg-blue-600 font-bold" @click="handleStartJob">确认母卷并开机</Button>
        </Card>
      </div>

      <div v-if="workOrderStatus === '作业前准备'" class="flex-1 flex flex-col bg-white border border-slate-200 rounded-lg overflow-hidden shadow-sm">
        <div v-if="activeTabPre === '1'" class="flex-1 flex flex-col min-h-0">
          <Table :columns="envColumns" :dataSource="envParams" :pagination="false" size="small" class="vben-schema-table">
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'val'"><InputNumber v-model:value="record.val" size="small" @change="record.status='未检'"/></template>
              <template v-else-if="column.dataIndex === 'status'"><Tag :color="record.status==='正常'?'green':'red'">{{ record.status }}</Tag></template>
            </template>
          </Table>
          <Table :columns="deviceColumns" :dataSource="deviceChecks" :pagination="false" size="small" class="vben-schema-table flex-1 mt-2">
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'result'">
                <Select v-model:value="record.result" size="small" class="w-24">
                  <Select.Option value="未点">未点</Select.Option><Select.Option value="正常">正常</Select.Option>
                </Select>
              </template>
              <template v-else-if="column.dataIndex === 'remark'"><Input v-model:value="record.remark" size="small" /></template>
            </template>
          </Table>
        </div>

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
              <div><span class="text-xs text-slate-400">当前执行母卷</span><div class="font-mono font-bold text-blue-700 text-lg">{{ parentBatchNo }}</div></div>
              <Divider type="vertical" class="h-8" />
              <div><span class="text-xs text-slate-400">总可分切长度</span><div class="font-bold text-slate-700 text-lg">{{ incomingRoll.totalLength }} <span class="text-xs">m</span></div></div>
              <div><span class="text-xs text-slate-400">已报工长度</span><div class="font-bold text-green-600 text-lg">{{ incomingRoll.usedLength }} <span class="text-xs">m</span></div></div>
              <div><span class="text-xs text-slate-400">剩余可用长度</span><div class="font-bold text-orange-500 text-lg">{{ remainingLength }} <span class="text-xs">m</span></div></div>
            </div>
            <div class="w-64">
              <div class="text-[10px] text-slate-500 flex justify-between mb-1"><span>母卷消耗进度</span><span>{{ progressPercent }}%</span></div>
              <Progress :percent="progressPercent" :showInfo="false" strokeColor="#2563eb" size="small" />
            </div>
          </div>
        </Card>

        <div class="bg-blue-50 border border-blue-200 p-3 rounded-lg flex items-end gap-4 shrink-0 shadow-sm">
          <Form.Item label="本次切卷长度 (m)" class="mb-0 font-bold text-blue-800">
            <InputNumber v-model:value="subBatchForm.length" size="large" class="w-40 shadow-sm" :min="1" :max="remainingLength" />
          </Form.Item>
          <Form.Item label="报工备注说明" class="mb-0 flex-1 font-bold text-blue-800">
            <Input v-model:value="subBatchForm.remark" size="large" placeholder="可选填..." class="shadow-sm" />
          </Form.Item>
          <Button type="primary" size="large" class="bg-blue-600 font-bold px-8 shadow-md h-[40px] mb-[1px]" :disabled="remainingLength <= 0" @click="handleGenerateSubBatch">
            <IconifyIcon icon="lucide:scissors" class="mr-2" /> 确认切卷并生成批次
          </Button>
        </div>

        <div class="flex-1 bg-white border border-slate-200 rounded-lg overflow-hidden shadow-sm flex flex-col min-h-0">
          <div class="p-2 bg-slate-50 border-b text-xs font-bold text-slate-600 flex items-center gap-2">
            <IconifyIcon icon="lucide:list-tree" /> 子卷分批产出明细账
          </div>
          <Table :columns="subBatchColumns" :dataSource="subBatches" :pagination="false" size="small" :scroll="{ y: 'calc(100vh - 380px)' }" class="vben-schema-table flex-1">
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'subBatchNo'">
                <span class="font-mono font-bold text-blue-700">{{ record.subBatchNo }}</span>
              </template>
              <template v-if="column.dataIndex === 'status'">
                <Tag :color="record.status === '已打印' ? 'green' : 'blue'">{{ record.status }}</Tag>
              </template>
              <template v-if="column.dataIndex === 'action'">
                <Button type="link" size="small" @click="handlePrintSubBatch(record)">
                  <IconifyIcon icon="lucide:printer" class="mr-1" />标签打印
                </Button>
              </template>
            </template>
            <template #emptyText><div class="py-10 text-slate-400">暂无子批次产出，请在上方录入切卷长度并生成。</div></template>
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
          <Button @click="envParams.forEach(p=>p.status='正常'); deviceChecks.forEach(d=>d.result='正常')">全部点检合格</Button>
          <Button type="primary" class="bg-blue-600 font-bold px-8" :disabled="!isPreCheckComplete" @click="finishPreCheck">确认参数并开机</Button>
        </template>

        <template v-if="workOrderStatus === '作业中'">
          <Button danger class="px-6">设备异常呼叫</Button>
          <Button type="primary" class="bg-green-600 border-none font-bold px-8 shadow-lg shadow-green-100" @click="handleFinishJob">
            完工与余料尾卷处理
          </Button>
        </template>
      </div>
    </footer>

    <Modal v-model:open="tailModalVisible" title="完工及尾卷/余料处理" @ok="submitTailAndFinish">
      <Form layout="vertical" class="mt-4">
        <div class="mb-4 text-xs text-orange-600 bg-orange-50 p-2 rounded border border-orange-200">
          系统检测到当前母卷剩余 <b>{{ remainingLength }}m</b> 未报工，请选择余料处理方式。
        </div>
        <Form.Item label="余料长度 (m)">
          <InputNumber v-model:value="tailForm.length" size="large" class="w-full font-bold" disabled />
        </Form.Item>
        <Form.Item label="处理动作">
          <Select v-model:value="tailForm.action" size="large">
            <Select.Option value="生成尾卷">生成尾卷 (打印尾卷标签并入库)</Select.Option>
            <Select.Option value="退回线边库">直接退回线边库 (不打印)</Select.Option>
            <Select.Option value="废料报废">报废处理</Select.Option>
          </Select>
        </Form.Item>
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
