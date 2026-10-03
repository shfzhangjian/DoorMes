<script lang="ts" setup>
import { ref, watch, computed } from 'vue';
import {
  Button, Card, Tag, Input, InputNumber, Table, Tabs,
  message, Modal, Select, Divider, Drawer, Timeline, Form, DatePicker, Space
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
// 1. 未开工 (领用扫描原材料)
// ==================================================================================
const rollScanCode = ref('');

watch(rollScanCode, (val) => {
  if (val && val.length >= 9) {
    message.loading('校验前序原材料...', 0.5).then(() => {
      parentBatchNo.value = 'FILM-' + val.slice(-5);
      incomingRoll.value = {
        code: val,
        batch: parentBatchNo.value,
        product: 'T01_Film_精磨膜',
        length: 100,
        source: '半成品库'
      };
      message.success(`原材料 ${parentBatchNo.value} 扫描就绪`);
      rollScanCode.value = '';
    });
  }
});

function handleStartJob() {
  if (!incomingRoll.value) return message.warning('请先扫描领用原材料卷！');
  if (!startJobTime.value) return message.warning('请选择实际开工时间！');

  workOrderStatus.value = '作业前准备';
  activeTabPre.value = '1';
  recordHistory('粘胶开机', `领用精磨原材料: ${parentBatchNo.value}, 长度: ${incomingRoll.value.length}m`);
}

// ==================================================================================
// 2. 作业前准备 (环境点检 + 粘胶参数记录)
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
  { item: '贴合间隙', req: '0.05-0.10', unit: 'mm', val: null },
  { item: '压辊温度', req: '60-80', unit: '℃', val: null },
  { item: '运行线速', req: '10-20', unit: 'm/min', val: null },
  { item: '放卷恒定电流', req: '2.5', unit: 'A', val: null },
  { item: '收卷恒定电流', req: '3.0', unit: 'A', val: null }
]);
const startupColumns: TableColumnsType = [
  { title: '参数名称', dataIndex: 'item' }, { title: '工艺要求', dataIndex: 'req' },
  { title: '单位', dataIndex: 'unit', width: 80 }, { title: '实际设定/记录值', dataIndex: 'val', width: 150 }
];

const isPreCheckComplete = computed(() => envParams.value.every(p => p.status !== '未检') && startupParams.value.every(p => p.val !== null));

function finishPreCheck() {
  recordHistory('开机准备完成', '环境确认及粘胶参数设定完毕', { type: 'pre', env: clone(envParams.value), param: clone(startupParams.value) });
  workOrderStatus.value = '作业中';
}

// ==================================================================================
// 3. 作业中 (持续质量检验：厚度录入)
// ==================================================================================
const thicknessForm = ref({ val: null, pos: '左侧' });
const thicknessLogs = ref<any[]>([]);

const thicknessColumns: TableColumnsType = [
  { title: '检验时间', dataIndex: 'time', width: 150 },
  { title: '测量位置', dataIndex: 'pos', width: 100 },
  { title: '粘胶1厚度 (mm)', dataIndex: 'val', align: 'right', width: 150 },
  { title: '判定', dataIndex: 'status', width: 100 },
  { title: '检验人', dataIndex: 'user' }
];

function handleAddThickness() {
  if (!thicknessForm.value.val) return message.warning('请输入测量厚度');

  // 假定标准厚度为 0.15mm ± 0.02
  const val = Number(thicknessForm.value.val);
  const isOk = val >= 0.13 && val <= 0.17;

  thicknessLogs.value.unshift({
    id: Date.now(),
    time: new Date().toLocaleTimeString(),
    pos: thicknessForm.value.pos,
    val: val.toFixed(3),
    status: isOk ? '合格' : '超差',
    user: props.operator.userName
  });

  if (!isOk) message.warning('厚度超差，请注意调整贴合间隙！');
  else message.success('检验记录已追加');

  thicknessForm.value.val = null;
}

// ==================================================================================
// 4. 完工报工与入库
// ==================================================================================
const finishModalVisible = ref(false);
const finishScanCode = ref('');
const finishForm = ref({ width: null, length: null, warehouse: '半成品库' });

// 模拟扫码直接带出胶板宽幅和米数
watch(finishScanCode, (val) => {
  if (val && val.length >= 6) {
    message.loading('解析胶板条码...', 0.5).then(() => {
      finishForm.value.width = 1.25 as any;
      finishForm.value.length = 98.5 as any;
      message.success('已自动解析宽幅与米数');
      finishScanCode.value = '';
    });
  }
});

function handleFinishJob() {
  finishModalVisible.value = true;
}

function submitFinish() {
  if (!finishForm.value.width || !finishForm.value.length) return message.warning('宽幅与米数不能为空');

  Modal.confirm({
    title: '完工入库确认',
    content: `将生成完工批次并转入【${finishForm.value.warehouse}】，宽幅: ${finishForm.value.width}m, 长度: ${finishForm.value.length}m`,
    onOk: () => {
      recordHistory('完工报工入库', `宽幅: ${finishForm.value.width}m, 米数: ${finishForm.value.length}m, 入库至半成品库`);
      workOrderStatus.value = '完成作业';
      finishModalVisible.value = false;
      message.success('粘胶完工报工成功！');
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
        <span v-if="parentBatchNo" class="text-xs font-mono font-bold text-blue-700 bg-blue-50 px-3 py-1 rounded-full border border-blue-100">母卷: {{ parentBatchNo }}</span>
        <Tabs v-if="workOrderStatus === '作业前准备'" v-model:activeKey="activeTabPre" type="capsule" size="small" class="compact-tabs">
          <Tabs.TabPane key="1" tab="环境点检" />
          <Tabs.TabPane key="2" tab="粘胶参数设定" />
        </Tabs>
      </div>
    </header>

    <main class="flex-1 flex flex-col min-h-0 p-3 overflow-hidden">

      <div v-if="workOrderStatus === '未开工'" class="flex-1 flex items-center justify-center">
        <Card class="w-[500px] border-none shadow-xl rounded-2xl p-4 bg-white">
          <div class="flex flex-col items-center mb-4">
            <div class="w-16 h-16 bg-blue-50 rounded-full flex items-center justify-center text-blue-600 mb-2"><IconifyIcon icon="lucide:component" class="text-3xl" /></div>
            <h2 class="text-lg font-black text-slate-800">粘胶(1) 开工准备</h2>
          </div>

          <div class="bg-indigo-900 p-4 rounded-xl mb-4 shadow-inner">
            <div class="text-indigo-200 text-xs font-bold mb-2">1. 领用扫描：请扫描原材料条码</div>
            <Input v-model:value="rollScanCode" size="large" placeholder="扫码提取前序基材批次..." class="!bg-white/10 !text-white !border-indigo-700" />
            <div v-if="incomingRoll" class="mt-4 bg-white/10 p-3 rounded text-xs text-indigo-100 font-mono space-y-1">
              <div class="text-white font-bold mb-1 border-b border-white/20 pb-1">原材料加载成功</div>
              <div>原料批次：{{ incomingRoll.batch }}</div>
              <div>来源工序：{{ incomingRoll.source }}</div>
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
            <template v-if="column.dataIndex === 'val'">
              <InputNumber v-model:value="record.val" size="small" class="w-32" placeholder="录入参数" />
            </template>
          </template>
        </Table>
      </div>

      <div v-if="workOrderStatus === '作业中'" class="flex-1 flex flex-col gap-3">
        <div class="bg-blue-50 border border-blue-200 p-3 rounded-lg flex items-end gap-4 shrink-0 shadow-sm">
          <Form.Item label="测量位置" class="mb-0 font-bold text-blue-800">
            <Select v-model:value="thicknessForm.pos" size="large" class="w-32 shadow-sm">
              <Select.Option value="左侧">左侧</Select.Option>
              <Select.Option value="中段">中段</Select.Option>
              <Select.Option value="右侧">右侧</Select.Option>
            </Select>
          </Form.Item>
          <Form.Item label="实测厚度 (mm)" class="mb-0 flex-1 font-bold text-blue-800 max-w-[250px]">
            <InputNumber v-model:value="thicknessForm.val" size="large" class="w-full shadow-sm" placeholder="如: 0.15" :step="0.001" />
          </Form.Item>
          <Button type="primary" size="large" class="bg-blue-600 font-bold px-8 shadow-md h-[40px] mb-[1px]" @click="handleAddThickness">
            <IconifyIcon icon="lucide:file-check" class="mr-2" /> 记录品质检验数据
          </Button>
        </div>

        <div class="flex-1 bg-white border border-slate-200 rounded-lg overflow-hidden shadow-sm flex flex-col min-h-0">
          <div class="p-2 bg-slate-50 border-b text-xs font-bold text-slate-600 flex items-center gap-2">
            <IconifyIcon icon="lucide:ruler" /> 粘胶层厚度抽检记录表 (标准: 0.15 ± 0.02)
          </div>
          <Table :columns="thicknessColumns" :dataSource="thicknessLogs" :pagination="false" size="small" :scroll="{ y: 'calc(100vh - 310px)' }" class="vben-schema-table flex-1">
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'status'">
                <Tag :color="record.status === '合格' ? 'green' : 'red'">{{ record.status }}</Tag>
              </template>
            </template>
            <template #emptyText><div class="py-10 text-slate-400">暂无厚度检验记录，请在作业过程中定时抽检。</div></template>
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
          <Button type="primary" class="bg-blue-600 font-bold px-8" :disabled="!isPreCheckComplete" @click="finishPreCheck">确认参数并运行</Button>
        </template>

        <template v-if="workOrderStatus === '作业中'">
          <Button type="primary" class="bg-green-600 border-none font-bold px-8 shadow-lg shadow-green-100" @click="handleFinishJob">
            完工报工入库
          </Button>
        </template>
      </div>
    </footer>

    <Modal v-model:open="finishModalVisible" title="完工报工及半成品入库" @ok="submitFinish">
      <Form layout="vertical" class="mt-4">
        <div class="mb-4 text-xs text-blue-600 bg-blue-50 p-2 rounded border border-blue-200 flex gap-2 items-center">
          <IconifyIcon icon="lucide:scan-barcode" class="text-lg" />
          <Input v-model:value="finishScanCode" placeholder="扫描胶板二维码快速带出宽幅/米数" size="small" class="w-64" />
        </div>
        <Row :gutter="16">
          <Col :span="12">
            <Form.Item label="胶板宽幅 (m)">
              <InputNumber v-model:value="finishForm.width" size="large" class="w-full font-bold" />
            </Form.Item>
          </Col>
          <Col :span="12">
            <Form.Item label="收卷米数 (m)">
              <InputNumber v-model:value="finishForm.length" size="large" class="w-full font-bold" />
            </Form.Item>
          </Col>
        </Row>
        <Form.Item label="入库目标">
          <Select v-model:value="finishForm.warehouse" size="large" disabled>
            <Select.Option value="半成品库">半成品库</Select.Option>
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
