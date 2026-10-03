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
// 1. 未开工 (工单/领料扫描)
// ==================================================================================
const batchScanCode = ref('');

watch(batchScanCode, (val) => {
  if (val && val.length >= 8) {
    message.loading('校验前序单品批次...', 0.5).then(() => {
      parentBatchNo.value = 'TAPE2-' + val.slice(-4);
      incomingBatch.value = {
        code: val,
        batch: parentBatchNo.value,
        product: 'T01_Film_背胶单片',
        qty: 500, // 继承前工序数量
        source: '半成品库'
      };
      message.success(`待裁圆批次 ${parentBatchNo.value} 扫描就绪`);
      batchScanCode.value = '';
    });
  }
});

function handleStartJob() {
  if (!incomingBatch.value) return message.warning('请先扫描领用前工序待裁切的批次条码！');
  if (!startJobTime.value) return message.warning('请选择实际开工时间！');

  workOrderStatus.value = '作业前准备';
  activeTabPre.value = '1';
  recordHistory('单片裁圆开机', `接收批次: ${parentBatchNo.value}, 投入数量: ${incomingBatch.value.qty} PCS`);
}

// ==================================================================================
// 2. 作业前准备 (产线核对)
// ==================================================================================
const checkData = ref([
  { item: '核对投入物料批次与数量', req: '与工单及流转卡一致', result: '未确认', remark: '' },
  { item: '裁切模具/刀片状态', req: '刀片锋利无缺口，安装紧固', result: '未确认', remark: '' },
  { item: '设备急停与安全光栅', req: '功能触发有效', result: '未确认', remark: '' }
]);
const checkColumns: TableColumnsType = [
  { title: '检查项目', dataIndex: 'item' }, { title: '标准要求', dataIndex: 'req' },
  { title: '确认结果', dataIndex: 'result', width: 120 }, { title: '备注说明', dataIndex: 'remark' }
];

const isPreCheckComplete = computed(() => checkData.value.every(d => d.result === '正常'));

function finishPreCheck() {
  recordHistory('作业准备完成', '物料及裁切刀具确认无误', { type: 'pre', check: clone(checkData.value) });
  workOrderStatus.value = '作业中';
  activeTabRun.value = '1';
}

// ==================================================================================
// 3. 作业中 (参数记录：线速/刀深)
// ==================================================================================
const runParamForm = ref({ speed: null, depth: null });
const runParamLogs = ref<any[]>([]);

const runParamColumns: TableColumnsType = [
  { title: '记录时间', dataIndex: 'time', width: 180 },
  { title: '运行线速 (pcs/min)', dataIndex: 'speed', align: 'right' },
  { title: '切切刀深 (mm)', dataIndex: 'depth', align: 'right' },
  { title: '操作人', dataIndex: 'user', width: 120 }
];

function handleAddRunParam() {
  if (!runParamForm.value.speed || !runParamForm.value.depth) return message.warning('线速与刀深不能为空');

  runParamLogs.value.unshift({
    id: Date.now(),
    time: new Date().toLocaleTimeString(),
    speed: runParamForm.value.speed,
    depth: runParamForm.value.depth,
    user: props.operator.userName
  });

  recordHistory('工艺参数记录', `线速: ${runParamForm.value.speed}, 刀深: ${runParamForm.value.depth}`);
  message.success('工艺参数已记录');
  runParamForm.value.speed = null;
  runParamForm.value.depth = null;
}

// ==================================================================================
// 4. 作业后 (质量报检 + 完工报工入库打印)
// ==================================================================================

// 4.1 质量报检
const qaModalVisible = ref(false);
const qaForm = ref({ qty: null, level: '常规抽检', remark: '' });

function handleQaRequest() {
  qaForm.value.qty = incomingBatch.value.qty as any; // 默认带出本批总量
  qaModalVisible.value = true;
}

function submitQaRequest() {
  recordHistory('报检品质', `发起[${qaForm.value.level}], 报检数量: ${qaForm.value.qty} PCS, 备注: ${qaForm.value.remark}`);
  qaModalVisible.value = false;
  message.success('品质报检单已生成并推送至 FQC 检验工作台');
}

// 4.2 完工报工与入库打印
const finishModalVisible = ref(false);
const finishForm = ref({ goodQty: null, badQty: null, printLabel: true, remark: '' });

function handleFinishClick() {
  finishModalVisible.value = true;
}

function submitFinish() {
  if (!finishForm.value.goodQty) return message.warning('合格裁切数量不能为空');

  const detailStr = `裁切完工汇总: 合格 ${finishForm.value.goodQty} PCS, 不良 ${finishForm.value.badQty || 0} PCS`;
  recordHistory('完工报工', detailStr, { type: 'finish', data: clone(finishForm.value) });

  if (finishForm.value.printLabel) {
    Modal.success({
      title: '入库标签打印 (斑马打印机)',
      content: `裁切批次入库单已打印。\n批次号: ${parentBatchNo.value}-CUT\n数量: ${finishForm.value.goodQty} PCS\n流向: 半成品库`,
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
        <span v-if="parentBatchNo" class="text-xs font-mono font-bold text-blue-700 bg-blue-50 px-3 py-1 rounded-full border border-blue-100">加工批次: {{ parentBatchNo }}</span>
        <Tabs v-if="workOrderStatus === '作业前准备'" v-model:activeKey="activeTabPre" type="capsule" size="small" class="compact-tabs">
          <Tabs.TabPane key="1" tab="产线核对与点检" />
        </Tabs>
        <Tabs v-if="workOrderStatus === '作业中'" v-model:activeKey="activeTabRun" type="capsule" size="small" class="compact-tabs">
          <Tabs.TabPane key="1" tab="工艺参数监控记录" />
        </Tabs>
      </div>
    </header>

    <main class="flex-1 flex flex-col min-h-0 p-3 overflow-hidden">

      <div v-if="workOrderStatus === '未开工'" class="flex-1 flex items-center justify-center">
        <Card class="w-[500px] border-none shadow-xl rounded-2xl p-4 bg-white">
          <div class="flex flex-col items-center mb-4">
            <div class="w-16 h-16 bg-blue-50 rounded-full flex items-center justify-center text-blue-600 mb-2"><IconifyIcon icon="lucide:circle-dashed" class="text-3xl" /></div>
            <h2 class="text-lg font-black text-slate-800">单片裁圆 开工准备</h2>
          </div>

          <div class="bg-indigo-900 p-4 rounded-xl mb-4 shadow-inner">
            <div class="text-indigo-200 text-xs font-bold mb-2">1. 领料扫描：扫描前序半成品批次条码</div>
            <Input v-model:value="batchScanCode" size="large" placeholder="扫码加载待裁切批次..." class="!bg-white/10 !text-white !border-indigo-700" />
            <div v-if="incomingBatch" class="mt-4 bg-white/10 p-3 rounded text-xs text-indigo-100 font-mono space-y-1">
              <div class="text-white font-bold mb-1 border-b border-white/20 pb-1">工序过站投入确认</div>
              <div>来料批次：{{ incomingBatch.batch }}</div>
              <div>投入数量：<span class="text-green-400 font-bold text-sm">{{ incomingBatch.qty }} PCS</span></div>
              <div>来源库位：{{ incomingBatch.source }}</div>
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
                <Select.Option value="未确认">未确认</Select.Option><Select.Option value="正常">正常</Select.Option><Select.Option value="异常">异常</Select.Option>
              </Select>
            </template>
            <template v-else-if="column.dataIndex === 'remark'"><Input v-model:value="record.remark" size="small" /></template>
          </template>
        </Table>
      </div>

      <div v-if="workOrderStatus === '作业中'" class="flex-1 flex flex-col gap-3">
        <Row :gutter="12" class="shrink-0">
          <Col :span="8"><Card size="small" class="bg-blue-50 border-blue-100 shadow-sm"><Statistic title="当前批次目标 (PCS)" :value="incomingBatch.qty" /></Card></Col>
          <Col :span="8"><Card size="small" class="bg-indigo-50 border-indigo-100 shadow-sm"><Statistic title="运行线速参考 (pcs/min)" value="25 - 35" valueStyle="font-size:16px; font-weight:bold; color:#4f46e5;" /></Card></Col>
          <Col :span="8"><Card size="small" class="bg-purple-50 border-purple-100 shadow-sm"><Statistic title="裁切刀深参考 (mm)" value="1.50 ± 0.05" valueStyle="font-size:16px; font-weight:bold; color:#7e22ce;" /></Card></Col>
        </Row>

        <div v-if="activeTabRun === '1'" class="flex-1 flex flex-col min-h-0 bg-white border border-slate-200 rounded-lg overflow-hidden shadow-sm">
          <div class="bg-blue-50 p-4 border-b border-blue-200 flex items-end gap-4 shrink-0 shadow-sm">
            <Form.Item label="当前线速 (pcs/min)" class="mb-0 font-bold text-blue-800">
              <InputNumber v-model:value="runParamForm.speed" size="large" class="w-40 shadow-sm" />
            </Form.Item>
            <Form.Item label="当前刀深 (mm)" class="mb-0 font-bold text-blue-800">
              <InputNumber v-model:value="runParamForm.depth" size="large" class="w-40 shadow-sm" :step="0.01" />
            </Form.Item>
            <Button type="primary" size="large" class="bg-blue-600 font-bold px-8 shadow-md h-[40px] mb-[1px]" @click="handleAddRunParam">
              <IconifyIcon icon="lucide:activity" class="mr-2" /> 记录过程工艺参数
            </Button>
          </div>

          <Table :columns="runParamColumns" :dataSource="runParamLogs" :pagination="false" size="small" :scroll="{ y: 'calc(100vh - 360px)' }" class="vben-schema-table flex-1">
            <template #emptyText><div class="py-10 text-slate-400">请定时录入裁切线速与刀深参数。</div></template>
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
          <Button @click="checkData.forEach(p=>p.result='正常')" class="border-blue-200 text-blue-600">一键点检正常</Button>
          <Button type="primary" class="bg-blue-600 font-bold px-8" :disabled="!isPreCheckComplete" @click="finishPreCheck">开始裁圆作业</Button>
        </template>

        <template v-if="workOrderStatus === '作业中'">
          <Button class="border-orange-300 text-orange-600 font-bold px-6" @click="handleQaRequest">
            <IconifyIcon icon="lucide:shield-check" class="mr-1" /> 触发品质报检
          </Button>
          <Divider type="vertical" />
          <Button type="primary" class="bg-green-600 border-none font-bold px-8 shadow-lg shadow-green-100" @click="handleFinishClick">
            裁切完工报工入库
          </Button>
        </template>
      </div>
    </footer>

    <Modal v-model:open="qaModalVisible" title="触发品质报检申请" @ok="submitQaRequest">
      <Form layout="vertical" class="mt-4">
        <Form.Item label="报检检验级别">
          <Select v-model:value="qaForm.level">
            <Select.Option value="常规抽检">常规抽检</Select.Option>
            <Select.Option value="全数全检">全数全检</Select.Option>
          </Select>
        </Form.Item>
        <Form.Item label="送检数量 (PCS)"><InputNumber v-model:value="qaForm.qty" class="w-full" disabled /></Form.Item>
        <Form.Item label="报检备注"><Input.TextArea v-model:value="qaForm.remark" rows="2" placeholder="可选填异常现象..." /></Form.Item>
      </Form>
    </Modal>

    <Modal v-model:open="finishModalVisible" title="裁切完工报工与入库确认" @ok="submitFinish" width="600px">
      <Form layout="vertical" class="mt-4">
        <Row :gutter="16">
          <Col :span="12">
            <Form.Item label="裁切合格数量 (PCS) *必填">
              <InputNumber v-model:value="finishForm.goodQty" size="large" class="w-full font-bold text-green-600" />
            </Form.Item>
          </Col>
          <Col :span="12">
            <Form.Item label="裁切不良数量 (PCS)">
              <InputNumber v-model:value="finishForm.badQty" size="large" class="w-full font-bold text-red-500" />
            </Form.Item>
          </Col>
        </Row>
        <div class="mt-2 p-4 border border-blue-200 bg-blue-50 rounded-xl">
          <div class="flex items-center justify-between mb-2">
            <span class="font-bold text-blue-800">入库与打印指令</span>
          </div>
          <div class="text-xs text-blue-600 mb-4">确认报工后，合格产出品将流转至半成品库。勾选下方选项将同步触发斑马打印机输出装箱/周转标签。</div>
          <Form.Item class="mb-0">
            <label class="flex items-center gap-2 cursor-pointer font-bold text-slate-700">
              <input type="checkbox" v-model="finishForm.printLabel" class="w-4 h-4 text-blue-600 rounded border-gray-300 focus:ring-blue-500" />
              同步打印入库流转标签 (自动扫码入库)
            </label>
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
