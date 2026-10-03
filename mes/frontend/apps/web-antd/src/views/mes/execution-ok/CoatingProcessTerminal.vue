<script lang="ts" setup>
import { ref, watch, computed, onMounted } from 'vue';
import {
  Button, Card, Tag, Input, InputNumber, Table, Tabs,
  message, Modal, Select, Divider, Drawer, Timeline, Form, Space, DatePicker, Row, Col, Descriptions
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
const batchNo = ref(props.order.batch || '');
const activeTabPre = ref('1');
const activeTabRun = ref('涂台工艺');

const historyDrawerVisible = ref(false);
const historyList = ref<any[]>([]);

function recordHistory(action: string, detail: string, snapshot: any = null) {
  historyList.value.unshift({ time: new Date().toLocaleString(), status: workOrderStatus.value, action, user: props.operator.userName, detail, snapshot });
}

// ==================================================================================
// 1. 未开工状态逻辑 (前工序扫码溯源 & 工单切换)
// ==================================================================================
const startJobTime = ref<string | null>(null);
const mixingScanCode = ref('');
const prevMixingInfo = ref<any>(null); // 前工序配料打印详情

// 需求: 扫描配料打印二维码, 显示配料详情并切换工单
watch(mixingScanCode, (val) => {
  if (val && val.length === 9) {
    message.loading('正在溯源前工序配料数据...', 0.5).then(() => {
      // 模拟扫码获取到的配料信息
      prevMixingInfo.value = {
        code: val,
        mixBatch: 'MB-' + val.slice(-4),
        product: 'T01_Film_胶液',
        weight: '500kg',
        creator: '张配料',
        time: new Date(Date.now() - 3600000).toLocaleString()
      };
      message.success(`成功关联前工序配料: ${prevMixingInfo.value.mixBatch}`);
      // 模拟切换工单：这里实际上应该 emit 给父组件让父组件切换，为演示终端独立性，这里做展示更新
      message.info('已自动切换至对应的涂布作业工单');
    });
  }
});

function handleStartJob() {
  if (!prevMixingInfo.value) return message.warning('请先扫描领用前工序配料桶！');
  if (!startJobTime.value) return message.warning('请选择实际开工时间！');

  batchNo.value = 'COAT-' + new Date().getTime().toString().slice(-8);
  workOrderStatus.value = '作业前准备';

  recordHistory('湿法线开机', `关联配料: ${prevMixingInfo.value.mixBatch}, 批次: ${batchNo.value}`);
  message.success('已进入作业前点检与参数设定状态');
}

// ==================================================================================
// 2. 作业前准备 (点检 + 工艺设定)
// ==================================================================================
const checkData = ref([
  { type: '环境点检', item: '温、湿度(℃，%RH)', req: '符合工艺卡', result: '未点' },
  { type: '设备点检', item: '设备清洁点检、开机点检', req: '各导辊无异物/急停有效', result: '未点' }
]);
const checkColumns: TableColumnsType = [
  { title: '类型', dataIndex: 'type', width: 120 }, { title: '点检项目', dataIndex: 'item' },
  { title: '要求', dataIndex: 'req' }, { title: '结果', dataIndex: 'result', width: 150 }
];

// 作业前触发的工艺参数设定
const startupParams = ref([
  { node: '涂台工艺', item: '刀口间隙', unit: 'mm', std: '0.15', val: null },
  { node: '涂台工艺', item: '涂料宽幅', unit: 'm', std: '1.20', val: null },
  { node: '涂台工艺', item: '浆料温度', unit: '℃', std: '45.0', val: null },
  { node: '凝固工艺', item: '三区DMF浓度', unit: '%', std: '15.0', val: null },
  { node: '凝固工艺', item: '三区水温', unit: '℃', std: '30.0', val: null },
  { node: '凝固工艺', item: '三区电导率', unit: 'μS/cm', std: '500', val: null },
  { node: '水洗工艺', item: '三区DMF/水温/电导率', unit: '混合', std: '标准要求', val: null },
  { node: '烘干工艺', item: '三区温度', unit: '℃', std: '120.0', val: null },
]);
const startupColumns: TableColumnsType = [
  { title: '工艺节点', dataIndex: 'node', width: 120 }, { title: '工艺参数', dataIndex: 'item' },
  { title: '单位', dataIndex: 'unit', width: 80 }, { title: '参考标准', dataIndex: 'std', width: 100 },
  { title: '实际设定值', dataIndex: 'val', width: 150 }
];

const isPreCheckComplete = computed(() => checkData.value.every(d => d.result !== '未点') && startupParams.value.every(p => p.val !== null));

function finishPreCheck() {
  recordHistory('作业前准备完成', '开机点检与工艺参数设定完毕');
  workOrderStatus.value = '作业中';
}

// ==================================================================================
// 3. 作业中逻辑 (工艺节点周期监控报工)
// ==================================================================================
// 模拟作业中周期采集的记录表
const runLogs = ref({
  '涂台工艺': [{ time: '14:00', level: '45.2', speed: '12.5', length: '1500', user: '张伟' }],
  '凝固工艺': [{ time: '14:00', width: '1.18', thickness: '0.145', spc: '已推送', user: '张伟' }],
  '水洗工艺': [{ time: '14:00', peelPos: '区2', width: '1.18', user: '张伟' }],
  '烘干工艺': [{ time: '14:00', speed: '12.5', thickness: '0.14', width: '1.17', user: '张伟' }]
});

function recordRunLog() {
  const now = new Date().toLocaleTimeString('en-US', { hour12: false, hour: 'numeric', minute: 'numeric' });
  message.success(`已记录 ${activeTabRun.value} 最新参数`);
  // 仅做UI模拟追加
  runLogs.value[activeTabRun.value].unshift({ time: now, user: props.operator.userName, level: '45.0', speed: '12.5', length: '1600', width: '1.18', thickness: '0.145', peelPos: '区2', spc: '已推送' });
}

// 3.1 品质交互表单
const firstArticleVisible = ref(false);
const firstArticleForm = ref({ remark: '' });
function submitFirstArticle() {
  recordHistory('首件检验', `首样跟踪单已提交: ${firstArticleForm.value.remark}`);
  firstArticleVisible.value = false;
  message.success('打样检验单已生成并推送品管');
}

const samplingVisible = ref(false);
const samplingForm = ref({ pos: '中段', qty: 1 });
function submitSampling() {
  recordHistory('取样送检', `取样位置: ${samplingForm.value.pos}, 数量: ${samplingForm.value.qty}`);
  samplingVisible.value = false;
  Modal.info({ title: '打印预览', content: '【NAP检验标签】已通过斑马打印机输出，请贴于样卷上。' });
}

// ==================================================================================
// 4. 完成作业 (完工报工入库)
// ==================================================================================
const finishModalVisible = ref(false);
const finishForm = ref({ meters: null, waste: null, endTime: '' });

function handleFinishJob() { finishModalVisible.value = true; }

function submitFinish() {
  if (!finishForm.value.meters) return message.warning('必须输入收卷米数');
  const whCode = 'WH-IN-' + new Date().getTime().toString().slice(-6); // 生成边库入库编号

  recordHistory('完工报工入库', `收卷: ${finishForm.value.meters}m, 入库码: ${whCode}`);
  workOrderStatus.value = '完成作业';
  finishModalVisible.value = false;

  Modal.success({
    title: '半成品库标签打印',
    content: `半成品库标签已打印！\n线边库入库编号: ${whCode}`,
    onOk: () => { emit('close'); }
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
        <span v-if="batchNo" class="text-xs font-mono font-bold text-blue-700 bg-blue-50 px-3 py-1 rounded-full border border-blue-100">批次: {{ batchNo }}</span>
      </div>
    </header>

    <main class="flex-1 flex flex-col min-h-0 p-3 overflow-hidden relative">

      <div v-if="workOrderStatus === '未开工'" class="flex-1 flex items-center justify-center">
        <Card class="w-[500px] border-none shadow-xl rounded-2xl p-2 bg-white">
          <div class="flex flex-col items-center mb-6">
            <div class="w-16 h-16 bg-blue-50 rounded-full flex items-center justify-center text-blue-600 mb-2"><IconifyIcon icon="lucide:waves" class="text-3xl" /></div>
            <h2 class="text-lg font-black text-slate-800">湿法线开工准备</h2>
          </div>

          <div class="bg-indigo-900 p-4 rounded-xl mb-4 shadow-inner">
            <div class="text-indigo-200 text-xs font-bold mb-2">步骤1：扫描领用前工序配料打印码</div>
            <Input v-model:value="mixingScanCode" size="large" placeholder="请使用扫码枪扫描配料二维码..." class="!bg-white/10 !text-white !border-indigo-700" />
            <div v-if="prevMixingInfo" class="mt-4 bg-white/10 p-3 rounded text-xs text-indigo-100 font-mono space-y-1">
              <div class="text-white font-bold mb-1 border-b border-white/20 pb-1">配料详情确认</div>
              <div>源配料批次：{{ prevMixingInfo.mixBatch }}</div>
              <div>胶液重量：{{ prevMixingInfo.weight }} ({{ prevMixingInfo.product }})</div>
              <div>配料时间：{{ prevMixingInfo.time }}</div>
            </div>
          </div>

          <div class="bg-slate-50 p-4 rounded-xl border border-slate-100 mb-6">
            <div class="text-xs font-bold text-slate-600 mb-2">步骤2：选择当前产线实际开工时间</div>
            <DatePicker v-model:value="startJobTime" show-time value-format="YYYY-MM-DD HH:mm:ss" :inputReadOnly="true" size="large" class="w-full" />
          </div>

          <Button type="primary" size="large" block class="h-12 rounded-xl bg-blue-600 font-bold" @click="handleStartJob">关联胶液并开机运行</Button>
        </Card>
      </div>

      <div v-if="workOrderStatus === '作业前准备'" class="flex-1 flex flex-col bg-white border border-slate-200 rounded-lg overflow-hidden shadow-sm">
        <div class="flex items-center bg-slate-50 border-b px-2 shrink-0">
          <Tabs v-model:activeKey="activeTabPre" type="capsule" size="small" class="compact-tabs py-2">
            <Tabs.TabPane key="1" tab="开机点检(环境/设备)" />
            <Tabs.TabPane key="2" tab="开机工艺参数打底设定" />
          </Tabs>
        </div>

        <div class="flex-1 overflow-hidden p-2">
          <Table v-if="activeTabPre === '1'" :columns="checkColumns" :dataSource="checkData" :pagination="false" size="small" :scroll="{ y: 'calc(100vh - 250px)' }" class="vben-schema-table h-full">
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'result'">
                <Select v-model:value="record.result" size="small" class="w-24">
                  <Select.Option value="未点">未点</Select.Option><Select.Option value="正常">正常</Select.Option><Select.Option value="异常">异常</Select.Option>
                </Select>
              </template>
            </template>
          </Table>

          <Table v-if="activeTabPre === '2'" :columns="startupColumns" :dataSource="startupParams" :pagination="false" size="small" :scroll="{ y: 'calc(100vh - 250px)' }" class="vben-schema-table h-full">
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'val'"><InputNumber v-model:value="record.val" size="small" class="w-32" placeholder="填写实际设定" /></template>
            </template>
          </Table>
        </div>
      </div>

      <div v-if="workOrderStatus === '作业中'" class="flex-1 flex flex-col overflow-hidden gap-2">
        <div class="flex items-center gap-2 mb-1 shrink-0">
          <IconifyIcon icon="lucide:activity" class="text-blue-600" />
          <span class="text-sm font-bold text-slate-700">连续制造过程参数监控台</span>
        </div>

        <div class="flex-1 flex bg-white border border-slate-200 rounded-lg overflow-hidden shadow-sm">
          <Tabs v-model:activeKey="activeTabRun" tabPosition="left" class="w-full h-full custom-left-tabs">
            <Tabs.TabPane v-for="node in ['涂台工艺', '凝固工艺', '水洗工艺', '烘干工艺']" :key="node" :tab="node">

              <div class="p-3 h-full flex flex-col">
                <div class="flex items-center justify-between bg-blue-50 border border-blue-100 p-3 rounded-lg mb-3 shrink-0">
                  <div class="flex items-center gap-6">
                    <div class="font-bold text-blue-800">{{ node }} - 最新参数</div>
                    <div v-if="node === '涂台工艺'" class="flex gap-4 text-xs"><span>料槽液位: <b class="text-blue-600">45.2 CM</b></span><span>下料线速: <b class="text-blue-600">12.5 m/min</b></span></div>
                    <div v-if="node === '凝固工艺'" class="flex gap-4 text-xs"><span>出槽厚度: <b class="text-blue-600">0.145 mm</b></span><span>SPC参数: <Tag color="blue" size="small">生成中</Tag></span></div>
                    <div v-if="node === '水洗工艺'" class="flex gap-4 text-xs"><span>剥离位置: <b class="text-blue-600">正常</b></span><span>出水宽幅: <b class="text-blue-600">1.18 m</b></span></div>
                    <div v-if="node === '烘干工艺'" class="flex gap-4 text-xs"><span>线速: <b class="text-blue-600">12.5</b></span><span>出箱厚度: <b class="text-blue-600">0.14 mm</b></span></div>
                  </div>
                  <Button type="primary" size="small" @click="recordRunLog">录入当前巡检周期值</Button>
                </div>

                <Table :dataSource="runLogs[node]" :pagination="false" size="small" :scroll="{ y: 'calc(100vh - 350px)' }" class="vben-schema-table flex-1 border rounded-lg">
                  <Table.Column title="记录时间" dataIndex="time" width="100" />
                  <template v-if="node === '涂台工艺'">
                    <Table.Column title="料槽液位高度 (CM)" dataIndex="level" /><Table.Column title="下料线速 (m/min)" dataIndex="speed" /><Table.Column title="涂料米数 (m)" dataIndex="length" />
                  </template>
                  <template v-if="node === '凝固工艺'">
                    <Table.Column title="出槽宽幅 (m)" dataIndex="width" /><Table.Column title="出槽厚度 (mm)" dataIndex="thickness" /><Table.Column title="SPC推送状态" dataIndex="spc" />
                  </template>
                  <template v-if="node === '水洗工艺'">
                    <Table.Column title="剥离位置" dataIndex="peelPos" /><Table.Column title="出水宽幅" dataIndex="width" />
                  </template>
                  <template v-if="node === '烘干工艺'">
                    <Table.Column title="线速" dataIndex="speed" /><Table.Column title="出箱厚度" dataIndex="thickness" /><Table.Column title="出箱宽幅" dataIndex="width" />
                  </template>
                  <Table.Column title="操作人" dataIndex="user" width="80" />
                </Table>
              </div>

            </Tabs.TabPane>
          </Tabs>
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
          <Button v-if="activeTabPre === '1'" @click="checkData.forEach(d=>d.result='正常')">一键点检合格</Button>
          <Button type="primary" class="bg-blue-600 font-bold px-8" :disabled="!isPreCheckComplete" @click="finishPreCheck">完成开机准备</Button>
        </template>

        <template v-if="workOrderStatus === '作业中'">
          <Button class="border-orange-300 text-orange-600" @click="firstArticleVisible = true">首件检验(首样单)</Button>
          <Button class="border-blue-300 text-blue-600" @click="samplingVisible = true">取样送检(打印NAP)</Button>
          <Divider type="vertical" />
          <Button danger>停机中断</Button>
          <Button type="primary" class="bg-green-600 border-none font-bold px-8" @click="handleFinishJob">完工报工并推送</Button>
        </template>
      </div>
    </footer>

    <Modal v-model:open="firstArticleVisible" title="填写首样跟踪单" @ok="submitFirstArticle">
      <Form layout="vertical" class="mt-4"><Form.Item label="首件情况备注"><Input.TextArea v-model:value="firstArticleForm.remark" rows="3" /></Form.Item></Form>
    </Modal>
    <Modal v-model:open="samplingVisible" title="填写取样NAP信息" @ok="submitSampling">
      <Form layout="vertical" class="mt-4">
        <Form.Item label="取样位置"><Input v-model:value="samplingForm.pos" /></Form.Item>
        <Form.Item label="取样数量"><InputNumber v-model:value="samplingForm.qty" class="w-full" /></Form.Item>
      </Form>
    </Modal>
    <Modal v-model:open="finishModalVisible" title="湿法完工报工入库" @ok="submitFinish">
      <Form layout="vertical" class="mt-4">
        <div class="mb-4 text-xs text-blue-600 bg-blue-50 p-2 rounded">确认报工后将打印半成品库入库标签并生成线边库编码。</div>
        <Form.Item label="收卷总米数 (m)"><InputNumber v-model:value="finishForm.meters" size="large" class="w-full font-bold" /></Form.Item>
        <Form.Item label="废品米数 (m)"><InputNumber v-model:value="finishForm.waste" class="w-full" /></Form.Item>
        <Form.Item label="实际完工时间"><DatePicker v-model:value="finishForm.endTime" show-time :inputReadOnly="true" class="w-full" /></Form.Item>
      </Form>
    </Modal>

    <Drawer v-model:open="historyDrawerVisible" title="工序追踪体系" placement="right" width="400">
      <Timeline>
        <Timeline.Item v-for="(log, idx) in historyList" :key="idx" :color="idx === 0 ? 'blue' : 'gray'">
          <div class="font-bold mb-1 text-slate-700">{{ log.action }} <Tag color="blue" size="small" class="ml-2">{{ log.status }}</Tag></div>
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

/* 侧边工艺节点 Tab 样式覆盖 */
.custom-left-tabs :deep(.ant-tabs-nav-list) { padding-top: 10px; width: 120px; }
.custom-left-tabs :deep(.ant-tabs-tab) { padding: 12px 16px !important; font-weight: bold; color: #64748b; }
.custom-left-tabs :deep(.ant-tabs-tab-active) { background: #f8fafc; color: #2563eb; border-right: 3px solid #2563eb; }

.vben-schema-table :deep(.ant-table-wrapper), .vben-schema-table :deep(.ant-spin-nested-loading), .vben-schema-table :deep(.ant-spin-container), .vben-schema-table :deep(.ant-table) { height: 100%; display: flex; flex-direction: column; }
.vben-schema-table :deep(.ant-table-thead > tr > th) { background: #f8fafc !important; color: #64748b !important; font-size: 11px; font-weight: bold; padding: 6px 8px !important; }
.vben-schema-table :deep(.ant-table-cell) { padding: 6px 8px !important; font-size: 12px; }

::-webkit-scrollbar { width: 6px; height: 6px; }
::-webkit-scrollbar-thumb { background: #cbd5e1; border-radius: 4px; }
</style>
