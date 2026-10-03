<script lang="ts" setup>
import { ref, watch, computed } from 'vue';
import {
  Button, Card, Tag, Input, InputNumber, Table, Tabs,
  message, Modal, Select, Divider, Drawer, Timeline, Form, DatePicker, Row, Col, Statistic, Space
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
const activeTabRun = ref('1');

const incomingBatch = ref<any>(null);
const parentBatchNo = ref('');
const startJobTime = ref<string | null>(null);

const historyDrawerVisible = ref(false);
const historyList = ref<any[]>([]);

function recordHistory(action: string, detail: string, snapshot: any = null) {
  historyList.value.unshift({ time: new Date().toLocaleString(), status: workOrderStatus.value, action, user: props.operator.userName, detail, snapshot });
}

// ==================================================================================
// 1. 未开工 (领用扫描原材料)
// ==================================================================================
const batchScanCode = ref('');

watch(batchScanCode, (val) => {
  if (val && val.length >= 8) {
    message.loading('校验前序压槽单品批次...', 0.5).then(() => {
      parentBatchNo.value = 'STAMP-' + val.slice(-4);
      incomingBatch.value = {
        code: val,
        batch: parentBatchNo.value,
        product: 'T01_Film_压槽单片',
        qty: 500, // 继承前工序数量
        source: '压槽工序流转区'
      };
      message.success(`待背胶原材料 ${parentBatchNo.value} 扫描就绪`);
      batchScanCode.value = '';
    });
  }
});

function handleStartJob() {
  if (!incomingBatch.value) return message.warning('请先扫描领用前工序原材料条码！');
  if (!startJobTime.value) return message.warning('请选择实际开工时间！');

  workOrderStatus.value = '作业前准备';
  activeTabPre.value = '1';
  recordHistory('单片背胶开机', `接收批次: ${parentBatchNo.value}, 数量: ${incomingBatch.value.qty} PCS`);
}

// ==================================================================================
// 2. 作业前准备 (环境/设备点检 + 粘胶参数记录)
// ==================================================================================
const checkData = ref([
  { type: '环境点检', item: '温、湿度(℃,%RH)', req: '符合工艺卡要求', result: '未点', remark: '' },
  { type: '设备点检', item: '设备清洁点检', req: '各导辊及工作台无异物', result: '未点', remark: '' },
  { type: '设备点检', item: '开机点检', req: '气压、急停功能正常', result: '未点', remark: '' }
]);
const checkColumns: TableColumnsType = [
  { title: '类型', dataIndex: 'type', width: 100 }, { title: '检查项目', dataIndex: 'item' },
  { title: '标准要求', dataIndex: 'req' }, { title: '确认结果', dataIndex: 'result', width: 120 },
  { title: '备注说明', dataIndex: 'remark' }
];

const startupParams = ref([
  { item: '贴合间隙', req: '0.05-0.10', unit: 'mm', val: null },
  { item: '压辊温度', req: '60-80', unit: '℃', val: null },
  { item: '运行线速', req: '15-25', unit: 'pcs/min', val: null }
]);
const startupColumns: TableColumnsType = [
  { title: '工艺参数', dataIndex: 'item' }, { title: '工艺标准', dataIndex: 'req' },
  { title: '单位', dataIndex: 'unit', width: 80 }, { title: '实际设定值', dataIndex: 'val', width: 150 }
];

const isPreCheckComplete = computed(() => checkData.value.every(d => d.result !== '未点') && startupParams.value.every(p => p.val !== null));

function finishPreCheck() {
  recordHistory('作业准备完成', '设备环境正常，背胶参数已确认', { type: 'pre', check: clone(checkData.value), param: clone(startupParams.value) });
  workOrderStatus.value = '作业中';
  activeTabRun.value = '1';
}

// ==================================================================================
// 3. 作业中 (单片扫码上线、自检报工、胶板宽幅、质量三片抽检)
// ==================================================================================
const currentWidth = ref<number | null>(1.25); // 实时记录的胶板宽幅
const pieceScanCode = ref('');
const scannedPieces = ref<any[]>([]);

const pieceColumns: TableColumnsType = [
  { title: '序号', dataIndex: 'index', width: 60, align: 'center' },
  { title: '单品追溯条码', dataIndex: 'code' },
  { title: '胶板宽幅(m)', dataIndex: 'width', width: 120, align: 'right' },
  { title: '上线/自检时间', dataIndex: 'time', width: 180 },
  { title: '自检结果', dataIndex: 'status', width: 120, align: 'center' },
  { title: '操作', dataIndex: 'action', width: 100, align: 'center' }
];

const stats = computed(() => {
  const total = scannedPieces.value.length;
  const bad = scannedPieces.value.filter(p => p.status === '不良剔除').length;
  return { total, good: total - bad, bad };
});

// 需求: 单片上线 / 扫码生产自检报工
watch(pieceScanCode, (val) => {
  if (val && val.length >= 10) {
    if (!currentWidth.value) {
      pieceScanCode.value = '';
      return message.warning('请先确认或录入当前的胶板宽幅！');
    }
    const idx = scannedPieces.value.length + 1;
    scannedPieces.value.unshift({
      id: Date.now(),
      index: idx,
      code: val,
      width: currentWidth.value,
      time: new Date().toLocaleTimeString(),
      status: '背胶合格'
    });
    pieceScanCode.value = '';
    message.success(`单片 ${val} 已完成背胶并自检报工`);
  }
});

function markAsDefect(record: any) {
  record.status = '不良剔除';
  message.warning(`已将 ${record.code} 标记为背胶不良品`);
  recordHistory('生产自检异常', `单品 ${record.code} 背胶不合格，已剔除`);
}

// 需求: 质量检验 - 粘胶2中间品厚度(三片)
const qcForm = ref({ val1: null, val2: null, val3: null });
const qcLogs = ref<any[]>([]);
const qcColumns: TableColumnsType = [
  { title: '抽检时间', dataIndex: 'time', width: 160 },
  { title: '片1厚度(mm)', dataIndex: 'val1', align: 'center' },
  { title: '片2厚度(mm)', dataIndex: 'val2', align: 'center' },
  { title: '片3厚度(mm)', dataIndex: 'val3', align: 'center' },
  { title: '平均厚度(mm)', dataIndex: 'avg', align: 'center' },
  { title: '综合判定', dataIndex: 'status', align: 'center', width: 100 },
  { title: '检验人', dataIndex: 'user', width: 100 }
];

function submitQcCheck() {
  const { val1, val2, val3 } = qcForm.value;
  if (!val1 || !val2 || !val3) return message.warning('必须完整输入连续3片的厚度！');

  const v1 = Number(val1), v2 = Number(val2), v3 = Number(val3);
  const avg = ((v1 + v2 + v3) / 3).toFixed(3);

  // 假定工艺要求背胶后厚度为 0.20 ± 0.02
  const isOk = Number(avg) >= 0.18 && Number(avg) <= 0.22;

  qcLogs.value.unshift({
    id: Date.now(),
    time: new Date().toLocaleTimeString(),
    val1: v1.toFixed(3), val2: v2.toFixed(3), val3: v3.toFixed(3),
    avg,
    status: isOk ? '合格' : '不合格',
    user: props.operator.userName
  });

  if (isOk) message.success('三片厚度抽检合格');
  else message.error('平均厚度超差，请立即调整压辊间隙！');

  recordHistory('中间品厚度抽检', `抽检三片，均值: ${avg}mm, 判定: ${isOk ? '合格' : '不合格'}`);
  qcForm.value = { val1: null, val2: null, val3: null };
}

// ==================================================================================
// 4. 完工报工入库 (半成品库)
// ==================================================================================
const finishModalVisible = ref(false);
const finishForm = ref({ goodQty: 0, badQty: 0, warehouse: '半成品库', remark: '' });

function handleFinishClick() {
  finishForm.value.goodQty = stats.value.good;
  finishForm.value.badQty = stats.value.bad;
  finishModalVisible.value = true;
}

function submitFinish() {
  const detailStr = `背胶完工汇总: 合格 ${finishForm.value.goodQty} PCS, 剔除 ${finishForm.value.badQty} PCS, 入库至[${finishForm.value.warehouse}]`;
  recordHistory('完工报工入库', detailStr, { type: 'finish', data: clone(finishForm.value) });

  workOrderStatus.value = '完成作业';
  finishModalVisible.value = false;
  message.success('背胶作业完工，已完成半成品库入库过账');
  setTimeout(() => emit('close'), 1000);
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
        <span v-if="parentBatchNo" class="text-xs font-mono font-bold text-blue-700 bg-blue-50 px-3 py-1 rounded-full border border-blue-100">加工批次: {{ parentBatchNo }}</span>
        <Tabs v-if="workOrderStatus === '作业前准备'" v-model:activeKey="activeTabPre" type="capsule" size="small" class="compact-tabs">
          <Tabs.TabPane key="1" tab="设备与环境点检" />
          <Tabs.TabPane key="2" tab="粘胶参数记录" />
        </Tabs>
        <Tabs v-if="workOrderStatus === '作业中'" v-model:activeKey="activeTabRun" type="capsule" size="small" class="compact-tabs">
          <Tabs.TabPane key="1" tab="单片扫码上线与自检" />
          <Tabs.TabPane key="2" tab="质量检验(三片抽检)" />
        </Tabs>
      </div>
    </header>

    <main class="flex-1 flex flex-col min-h-0 p-3 overflow-hidden">

      <div v-if="workOrderStatus === '未开工'" class="flex-1 flex items-center justify-center">
        <Card class="w-[500px] border-none shadow-xl rounded-2xl p-4 bg-white">
          <div class="flex flex-col items-center mb-4">
            <div class="w-16 h-16 bg-blue-50 rounded-full flex items-center justify-center text-blue-600 mb-2"><IconifyIcon icon="lucide:layers" class="text-3xl" /></div>
            <h2 class="text-lg font-black text-slate-800">单片背胶 (粘胶2) 开工</h2>
          </div>

          <div class="bg-indigo-900 p-4 rounded-xl mb-4 shadow-inner">
            <div class="text-indigo-200 text-xs font-bold mb-2">1. 领用扫描：扫描待背胶原材料批次条码</div>
            <Input v-model:value="batchScanCode" size="large" placeholder="扫码加载前序单品批次..." class="!bg-white/10 !text-white !border-indigo-700" />
            <div v-if="incomingBatch" class="mt-4 bg-white/10 p-3 rounded text-xs text-indigo-100 font-mono space-y-1">
              <div class="text-white font-bold mb-1 border-b border-white/20 pb-1">单品原材料就绪</div>
              <div>来料批次：{{ incomingBatch.batch }}</div>
              <div>预估总数：<span class="text-green-400 font-bold text-sm">{{ incomingBatch.qty }} PCS</span></div>
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
        <Table v-if="activeTabPre === '1'" :columns="checkColumns" :dataSource="checkData" :pagination="false" size="small" class="vben-schema-table flex-1">
          <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'result'">
              <Select v-model:value="record.result" size="small" class="w-24">
                <Select.Option value="未点">未点</Select.Option><Select.Option value="正常">正常</Select.Option><Select.Option value="异常">异常</Select.Option>
              </Select>
            </template>
            <template v-else-if="column.dataIndex === 'remark'"><Input v-model:value="record.remark" size="small" /></template>
          </template>
        </Table>

        <Table v-if="activeTabPre === '2'" :columns="startupColumns" :dataSource="startupParams" :pagination="false" size="small" class="vben-schema-table flex-1">
          <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'val'"><InputNumber v-model:value="record.val" size="small" class="w-32" placeholder="录入设备参数" /></template>
          </template>
        </Table>
      </div>

      <div v-if="workOrderStatus === '作业中'" class="flex-1 flex flex-col gap-3">
        <Row :gutter="12" class="shrink-0">
          <Col :span="6"><Card size="small" class="bg-blue-50 border-blue-100 shadow-sm"><Statistic title="当前批次总量" :value="incomingBatch.qty" /></Card></Col>
          <Col :span="6"><Card size="small" class="bg-green-50 border-green-100 shadow-sm"><Statistic title="合格自检量 (PCS)" :value="stats.good" valueStyle="color: #16a34a" /></Card></Col>
          <Col :span="6"><Card size="small" class="bg-red-50 border-red-100 shadow-sm"><Statistic title="自检不良量 (PCS)" :value="stats.bad" valueStyle="color: #dc2626" /></Card></Col>
          <Col :span="6">
            <Card size="small" class="bg-indigo-50 border-indigo-100 shadow-sm h-full flex flex-col justify-center">
              <div class="text-xs text-indigo-500 mb-1">当前胶板宽幅设定 (m)</div>
              <InputNumber v-model:value="currentWidth" size="large" class="w-full font-bold text-indigo-700" :step="0.01" />
            </Card>
          </Col>
        </Row>

        <div v-if="activeTabRun === '1'" class="flex-1 flex flex-col min-h-0 bg-white border border-slate-200 rounded-lg overflow-hidden shadow-sm">
          <div class="bg-indigo-50 p-4 border-b border-indigo-200 flex flex-col gap-2 shrink-0">
            <div class="text-xs font-bold text-indigo-800">生产自检扫码区 (扫码即代表单片上线背胶完成，且操作员确认外观自检合格)</div>
            <Input v-model:value="pieceScanCode" size="large" placeholder="扫描单品条码进行报工自检..." class="font-mono text-lg shadow-inner" autofocus />
          </div>
          <Table :columns="pieceColumns" :dataSource="scannedPieces" :pagination="false" size="small" :scroll="{ y: 'calc(100vh - 380px)' }" class="vben-schema-table flex-1">
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'code'"><span class="font-mono font-bold text-slate-700">{{ record.code }}</span></template>
              <template v-if="column.dataIndex === 'status'"><Tag :color="record.status === '背胶合格' ? 'green' : 'red'">{{ record.status }}</Tag></template>
              <template v-if="column.dataIndex === 'action'">
                <Button v-if="record.status !== '不良剔除'" type="link" danger size="small" @click="markAsDefect(record)">自检异常</Button>
                <span v-else class="text-xs text-slate-400">已剔除</span>
              </template>
            </template>
            <template #emptyText><div class="py-10 text-slate-400">请使用扫码枪进行单片上线自检报工。</div></template>
          </Table>
        </div>

        <div v-if="activeTabRun === '2'" class="flex-1 flex flex-col min-h-0 bg-white border border-slate-200 rounded-lg overflow-hidden shadow-sm">
          <div class="bg-blue-50 p-4 border-b border-blue-200 flex items-end gap-4 shrink-0 shadow-sm">
            <Form.Item label="抽检片 1 厚度 (mm)" class="mb-0 font-bold text-blue-800"><InputNumber v-model:value="qcForm.val1" class="w-32 shadow-sm" :step="0.001" /></Form.Item>
            <Form.Item label="抽检片 2 厚度 (mm)" class="mb-0 font-bold text-blue-800"><InputNumber v-model:value="qcForm.val2" class="w-32 shadow-sm" :step="0.001" /></Form.Item>
            <Form.Item label="抽检片 3 厚度 (mm)" class="mb-0 font-bold text-blue-800"><InputNumber v-model:value="qcForm.val3" class="w-32 shadow-sm" :step="0.001" /></Form.Item>
            <Button type="primary" size="large" class="bg-blue-600 font-bold px-8 shadow-md h-[40px] mb-[1px]" @click="submitQcCheck">
              <IconifyIcon icon="lucide:ruler" class="mr-2" /> 记录三片厚度并判定
            </Button>
          </div>
          <Table :columns="qcColumns" :dataSource="qcLogs" :pagination="false" size="small" :scroll="{ y: 'calc(100vh - 350px)' }" class="vben-schema-table flex-1">
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'status'"><Tag :color="record.status === '合格' ? 'green' : 'red'">{{ record.status }}</Tag></template>
            </template>
            <template #emptyText><div class="py-10 text-slate-400">请按工艺要求进行连续三片厚度抽检记录。</div></template>
          </Table>
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
          <Button @click="checkData.forEach(p=>p.result='正常')" class="border-blue-200 text-blue-600">一键点检通过</Button>
          <Button type="primary" class="bg-blue-600 font-bold px-8" :disabled="!isPreCheckComplete" @click="finishPreCheck">开始背胶作业</Button>
        </template>

        <template v-if="workOrderStatus === '作业中'">
          <Button type="primary" class="bg-green-600 border-none font-bold px-8 shadow-lg shadow-green-100" @click="handleFinishClick">
            结束作业并完工入库
          </Button>
        </template>
      </div>
    </footer>

    <Modal v-model:open="finishModalVisible" title="完工报工及半成品入库确认" @ok="submitFinish">
      <Form layout="vertical" class="mt-4">
        <div class="bg-blue-50 text-blue-600 p-2 rounded mb-4 text-xs border border-blue-100">
          系统已自动汇总当前批次的背胶自检数据，入库动作将同步更新 WMS 库存。
        </div>
        <Row :gutter="16">
          <Col :span="12">
            <Form.Item label="合格产出数量 (PCS)">
              <InputNumber v-model:value="finishForm.goodQty" size="large" class="w-full font-bold text-green-600" disabled />
            </Form.Item>
          </Col>
          <Col :span="12">
            <Form.Item label="异常剔除数量 (PCS)">
              <InputNumber v-model:value="finishForm.badQty" size="large" class="w-full font-bold text-red-500" disabled />
            </Form.Item>
          </Col>
        </Row>
        <Form.Item label="系统目标仓库">
          <Select v-model:value="finishForm.warehouse" disabled size="large">
            <Select.Option value="半成品库">半成品库</Select.Option>
          </Select>
        </Form.Item>
        <Form.Item label="报工备注说明">
          <Input.TextArea v-model:value="finishForm.remark" rows="2" />
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
