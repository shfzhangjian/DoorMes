<script lang="ts" setup>
import { ref, reactive } from 'vue';
import { Button, Tag, Input, InputNumber, Table, Tabs, Modal, Select, Form, Radio, Descriptions, message, Row, Col } from 'ant-design-vue';
import type { TableColumnsType } from 'ant-design-vue';
import { IconifyIcon } from '@vben/icons';

const props = defineProps({
  operator: { type: Object, default: () => ({ userName: '未知操作员' }) },
  order: { type: Object, default: () => ({ id: '无工单' }) },
});

// 左侧 Tab 状态暴露给父级，以支持底部按钮联动
const activeTabQc = ref('FAI');
defineExpose({ activeTabQc });

// ==========================================
// 1. 各检验类型的对象录入表单
// ==========================================
const inspectForm = reactive({ mode: 'segment', segmentPos: '中段', length: null as number|null, barcode: '' });

// 触发工作台入口
function triggerInspection(type: string) {
  let target = '';
  if (type === '首检(FAI)') target = '首件样板 3件';
  else if (type === '完工检(FQC)') target = '成品完工抽样 5件';
  else {
    if (inspectForm.mode === 'segment' && !inspectForm.length) return message.warning('请输入取样米数！');
    if (inspectForm.mode === 'piece' && !inspectForm.barcode) return message.warning('请扫入单件条码！');
    target = inspectForm.mode === 'segment' ? `按段长: ${inspectForm.segmentPos} ${inspectForm.length}米` : `按单件: ${inspectForm.barcode}`;
  }

  wbTask.type = type;
  wbTask.target = target;
  wbTask.standard = null;
  wbTask.items = [];
  wbVisible.value = true;
}

// ==========================================
// 2. 统一质量工作台 (Workbench Modal)
// ==========================================
const wbVisible = ref(false);
const wbTask = reactive({ type: '', target: '', standard: null as string | null, items: [] as any[] });

const standardOptions = [
  { value: 'STD-T01-01', label: 'T01 聚氨酯通用检验标准 (V1.0)' },
  { value: 'STD-T01-02', label: 'T01 高要求特种检验标准 (V2.0)' }
];

const wbColumns: TableColumnsType = [
  { title: '检测项目', dataIndex: 'name', width: 140 },
  { title: '标准要求', dataIndex: 'std', width: 150 },
  { title: 'AQL应抽', dataIndex: 'aql', width: 90, align: 'center' },
  { title: '实际记录 (多值空格隔开)', dataIndex: 'values', minWidth: 200 },
  { title: '实测MAX', dataIndex: 'max', width: 100, align: 'center' },
  { title: '实测MIN', dataIndex: 'min', width: 100, align: 'center' },
  { title: '实测AVG', dataIndex: 'avg', width: 100, align: 'center' },
  { title: '结论', dataIndex: 'result', width: 100, align: 'center' }
];

function loadStandardItems() {
  wbTask.items = [
    { id: 1, name: '表面疵点', type: 'QUAL', std: '无划伤/气泡', aql: '3件', values: '', max: '-', min: '-', avg: '-', result: null },
    { id: 2, name: '涂层厚度(mm)', type: 'QUAN', std: '0.15 ± 0.02', target: 0.15, tol: 0.02, aql: '5件', values: '', max: null, min: null, avg: null, result: null }
  ];
}

function handleValuesChange(record: any) {
  if (!record.values) {
    record.max = record.type==='QUAN'?null:'-'; record.min = record.type==='QUAN'?null:'-'; record.avg = record.type==='QUAN'?null:'-'; record.result = null; return;
  }
  if (record.type === 'QUAN') {
    const nums = record.values.split(/[,，\s]+/).map(Number).filter((n:any) => !isNaN(n));
    if (nums.length > 0) {
      const max = Math.max(...nums); const min = Math.min(...nums); const avg = nums.reduce((a:number,b:number)=>a+b,0)/nums.length;
      record.max = max.toFixed(3); record.min = min.toFixed(3); record.avg = avg.toFixed(3);
      record.result = (max <= record.target + record.tol && min >= record.target - record.tol) ? '合格' : '不合格';
    }
  } else {
    record.result = record.values.includes('异常') || record.values.includes('NG') ? '不合格' : '合格';
  }
}

// ==========================================
// 3. 检验历史台账 (History Ledger)
// ==========================================
const qcHistory = ref<any[]>([
  { id: 'RPT-001', target: '首件样板 3件', type: '首件检验(FAI)', submitTime: '08:10:00', completeTime: '08:15:00', result: '合格', ngCount: 0, items: [] },
  { id: 'RPT-002', target: '按段长: 中段 500米', type: '生产自检(SELF)', submitTime: '09:25:00', completeTime: '09:26:00', result: '合格', ngCount: 0, items: [] },
  { id: 'RPT-003', target: '按单件: SN2026022401', type: '过程抽检(IPQC)', submitTime: '10:40:00', completeTime: '10:45:00', result: '不合格', ngCount: 1, items: [] }
]);
const qcHistoryColumns: TableColumnsType = [
  { title: '检测对象', dataIndex: 'target', minWidth: 160 },
  { title: '检测类型', dataIndex: 'type', width: 140 },
  { title: '提交检测时间', dataIndex: 'submitTime', width: 130, align: 'center' },
  { title: '完成检测时间', dataIndex: 'completeTime', width: 130, align: 'center' },
  { title: '结论', dataIndex: 'result', width: 100, align: 'center' },
  { title: '不合格项数', dataIndex: 'ngCount', width: 120, align: 'center' },
  { title: '操作', dataIndex: 'action', width: 100, align: 'center' }
];

function submitWorkbench() {
  const ngCount = wbTask.items.filter(i => i.result === '不合格').length;
  const isComplete = wbTask.items.every(i => i.result !== null);
  if (!isComplete) return message.warning('请完成所有检测项目的录入判定！');

  qcHistory.value.unshift({
    id: `RPT-${Date.now().toString().slice(-6)}`,
    target: wbTask.target, type: wbTask.type,
    submitTime: new Date(Date.now() - 120000).toLocaleTimeString(),
    completeTime: new Date().toLocaleTimeString(),
    result: ngCount > 0 ? '不合格' : '合格', ngCount: ngCount,
    standard: wbTask.standard, items: JSON.parse(JSON.stringify(wbTask.items))
  });

  wbVisible.value = false;
  message.success('检验报告已成功归档！');
  activeTabQc.value = 'HISTORY';

  // 清理输入框
  inspectForm.barcode = ''; inspectForm.length = null;
}

// 报告详情弹窗
const detailVisible = ref(false);
const detailRecord = ref<any>(null);
function openDetail(record: any) { detailRecord.value = record; detailVisible.value = true; }
</script>

<template>
  <div class="flex-1 flex flex-col bg-white border-2 border-slate-200 rounded-2xl overflow-hidden shadow-md">
    <Tabs v-model:activeKey="activeTabQc" tabPosition="left" class="w-full h-full custom-left-tabs">

      <Tabs.TabPane key="FAI" tab="首检(FAI)">
        <div class="flex-1 h-full flex flex-col items-center justify-center p-8 bg-slate-50 relative">
          <IconifyIcon icon="lucide:microscope" class="text-[120px] text-orange-400 mb-8 drop-shadow-md" />
          <h2 class="text-3xl font-black text-slate-800 mb-4">首件检验大厅</h2>
          <p class="text-xl text-slate-500 mb-10">工单开机后的首件必须接受全面核验。请点击底部大按钮调出首检报告表单。</p>
        </div>
      </Tabs.TabPane>

      <Tabs.TabPane key="SELF" tab="生产自检(SELF)">
        <div class="flex h-full bg-slate-50 items-center justify-center">
          <div class="w-[500px] p-8 flex flex-col border border-slate-200 bg-white rounded-3xl shadow-lg">
            <div class="text-2xl font-black text-slate-800 mb-6 border-b pb-4 text-emerald-600">🧑‍🔧 员工自检对象录入</div>
            <Form layout="vertical">
              <Form.Item label="检验模式">
                <Radio.Group v-model:value="inspectForm.mode" button-style="solid" class="w-full flex" size="large">
                  <Radio.Button value="segment" class="flex-1 text-center font-bold">按段长(米)</Radio.Button>
                  <Radio.Button value="piece" class="flex-1 text-center font-bold">单件(扫码)</Radio.Button>
                </Radio.Group>
              </Form.Item>
              <template v-if="inspectForm.mode === 'segment'">
                <Form.Item label="取样段位"><Select v-model:value="inspectForm.segmentPos" size="large" :options="[{value:'前段',label:'前段'},{value:'中段',label:'中段'},{value:'后段',label:'后段'}]" /></Form.Item>
                <Form.Item label="涉及米数 (m)"><InputNumber v-model:value="inspectForm.length" class="w-full h-12 text-xl font-bold" /></Form.Item>
              </template>
              <template v-if="inspectForm.mode === 'piece'">
                <Form.Item label="扫描条码"><Input v-model:value="inspectForm.barcode" size="large" class="h-12 text-lg" /></Form.Item>
              </template>
            </Form>
            <div class="mt-4 text-slate-400 font-bold text-center">录入完成后，请点击底部固定按钮弹出填写报告。</div>
          </div>
        </div>
      </Tabs.TabPane>

      <Tabs.TabPane key="IPQC" tab="过程抽检(IPQC)">
        <div class="flex h-full bg-slate-50 items-center justify-center">
          <div class="w-[500px] p-8 flex flex-col border border-slate-200 bg-white rounded-3xl shadow-lg">
            <div class="text-2xl font-black text-slate-800 mb-6 border-b pb-4 text-blue-600">🔬 过程抽检送检录入</div>
            <Form layout="vertical">
              <Form.Item label="检验模式">
                <Radio.Group v-model:value="inspectForm.mode" button-style="solid" class="w-full flex" size="large">
                  <Radio.Button value="segment" class="flex-1 text-center font-bold">按段长(米)</Radio.Button>
                  <Radio.Button value="piece" class="flex-1 text-center font-bold">单件(扫码)</Radio.Button>
                </Radio.Group>
              </Form.Item>
              <template v-if="inspectForm.mode === 'segment'">
                <Form.Item label="取样段位"><Select v-model:value="inspectForm.segmentPos" size="large" :options="[{value:'前段',label:'前段'},{value:'中段',label:'中段'},{value:'后段',label:'后段'}]" /></Form.Item>
                <Form.Item label="涉及米数 (m)"><InputNumber v-model:value="inspectForm.length" class="w-full h-12 text-xl font-bold" /></Form.Item>
              </template>
              <template v-if="inspectForm.mode === 'piece'">
                <Form.Item label="扫描条码"><Input v-model:value="inspectForm.barcode" size="large" class="h-12 text-lg" /></Form.Item>
              </template>
            </Form>
            <div class="mt-4 text-slate-400 font-bold text-center">录入完成后，请点击底部固定按钮弹出填写报告。</div>
          </div>
        </div>
      </Tabs.TabPane>

      <Tabs.TabPane key="FQC" tab="完工检(FQC)">
        <div class="flex-1 h-full flex flex-col items-center justify-center p-8 bg-slate-50 relative">
          <IconifyIcon icon="lucide:package-check" class="text-[120px] text-indigo-400 mb-8 drop-shadow-md" />
          <h2 class="text-3xl font-black text-slate-800 mb-4">完工出厂检验</h2>
          <p class="text-xl text-slate-500 mb-10">结单前需进行全项核验出 COA。请点击底部按钮调出质检工作台。</p>
        </div>
      </Tabs.TabPane>

      <Tabs.TabPane key="HISTORY" tab="检验历史台账">
        <div class="p-6 h-full flex flex-col bg-slate-50 relative">
          <div class="text-2xl font-black text-slate-800 mb-4 px-2 flex items-center shrink-0"><IconifyIcon icon="lucide:book-open-check" class="mr-2 text-indigo-600"/> 品质检测综合台账</div>
          <div class="flex-1-table-container border border-slate-200 rounded-xl overflow-hidden bg-white shadow-inner">
            <Table :columns="qcHistoryColumns" :dataSource="qcHistory" :pagination="false" :scroll="{y: '100%'}" class="full-height-table text-lg">
              <template #bodyCell="{ column, record }">
                <template v-if="column.dataIndex === 'result'">
                  <Tag :color="record.result === '合格' ? 'success' : 'error'" class="text-base font-bold py-1 px-4">{{record.result}}</Tag>
                </template>
                <template v-if="column.dataIndex === 'action'">
                  <Button type="link" size="large" class="font-bold text-blue-600" @click="openDetail(record)">查看详情</Button>
                </template>
              </template>
            </Table>
          </div>
        </div>
      </Tabs.TabPane>

    </Tabs>

    <Modal v-model:open="wbVisible" :title="`品质执行工作台 - ${wbTask.type}`" width="1200px" centered :footer="false">
      <div class="p-4 bg-slate-50 rounded-lg">
        <Row :gutter="16" class="mb-4">
          <Col :span="12"><div class="font-bold text-slate-500 mb-1">当前检测对象</div><div class="text-lg font-black text-indigo-700 bg-indigo-50 px-3 py-1 rounded">{{wbTask.target}}</div></Col>
          <Col :span="12"><div class="font-bold text-slate-500 mb-1">调取检验判定标准</div>
            <Select v-model:value="wbTask.standard" :options="standardOptions" class="w-full text-lg font-bold" @change="loadStandardItems" placeholder="请选择适用标准自动加载表单..." />
          </Col>
        </Row>
        <div class="h-[400px] border border-slate-200 rounded-lg overflow-hidden bg-white mb-4 relative">
          <Table :columns="wbColumns" :dataSource="wbTask.items" :pagination="false" :scroll="{ y: '100%' }" class="full-height-table absolute inset-0">
            <template #empty><div class="py-20 text-slate-400 font-bold">请在上方选择标准以加载待检指标项</div></template>
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'values'">
                <Input v-model:value="record.values" placeholder="输入值(空格分隔)" @blur="handleValuesChange(record)" class="w-full font-bold" />
              </template>
              <template v-if="column.dataIndex === 'result'">
                <Select v-model:value="record.result" :options="[{value:'合格',label:'合格'},{value:'不合格',label:'不合格'}]" class="w-full font-bold" :class="record.result==='合格'?'text-emerald-600':'text-red-600'" />
              </template>
            </template>
          </Table>
        </div>
        <div class="flex justify-end gap-4 mt-2">
          <Button size="large" @click="wbVisible=false" class="font-bold">放弃保存</Button>
          <Button size="large" type="primary" class="bg-indigo-600 font-bold px-8" @click="submitWorkbench">生成结论并写入台账</Button>
        </div>
      </div>
    </Modal>

    <Modal v-model:open="detailVisible" title="质量检验反馈报告单" width="1100px" centered :footer="false">
      <div class="p-4 bg-slate-50 rounded-lg" v-if="detailRecord">
        <Descriptions bordered :column="3" class="bg-white mb-4">
          <Descriptions.Item label="报告单号" :span="1">{{detailRecord.id}}</Descriptions.Item>
          <Descriptions.Item label="检测类型" :span="1"><Tag color="blue" class="font-bold">{{detailRecord.type}}</Tag></Descriptions.Item>
          <Descriptions.Item label="系统判定结论" :span="1"><Tag :color="detailRecord.result==='合格'?'success':'error'" class="font-bold text-base px-3 py-1">{{detailRecord.result}}</Tag></Descriptions.Item>
          <Descriptions.Item label="送检对象明细" :span="2">{{detailRecord.target}}</Descriptions.Item>
          <Descriptions.Item label="不合格项总计" :span="1">{{detailRecord.ngCount}} 项</Descriptions.Item>
          <Descriptions.Item label="依据执行标准" :span="3">{{detailRecord.standard || '系统默认临时标准'}}</Descriptions.Item>
        </Descriptions>
        <div class="h-[250px] border border-slate-200 rounded-lg overflow-hidden bg-white mb-4 relative">
          <Table :columns="wbColumns" :dataSource="detailRecord.items" :pagination="false" :scroll="{ y: '100%' }" class="full-height-table absolute inset-0">
            <template #empty><div class="py-10 text-slate-400">无详细指标记录数据</div></template>
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'result'">
                <span :class="record.result==='合格'?'text-emerald-600':'text-red-600'" class="font-bold">{{record.result}}</span>
              </template>
            </template>
          </Table>
        </div>
        <div class="flex justify-end"><Button size="large" @click="detailVisible=false" class="font-bold px-8">关闭窗口</Button></div>
      </div>
    </Modal>

    <span class="hidden" :data-trigger-fai="() => triggerInspection('首检(FAI)')"></span>
    <span class="hidden" :data-trigger-self="() => triggerInspection('生产自检(SELF)')"></span>
    <span class="hidden" :data-trigger-ipqc="() => triggerInspection('过程抽检(IPQC)')"></span>
    <span class="hidden" :data-trigger-fqc="() => triggerInspection('完工检(FQC)')"></span>
  </div>
</template>

<style scoped>
/* 继承左侧菜单和表格自适应样式 */
.custom-left-tabs { height: 100%; display: flex; }
:deep(.custom-left-tabs > .ant-tabs-nav) { height: 100%; }
:deep(.custom-left-tabs > .ant-tabs-content-holder) { flex: 1; height: 100%; display: flex; flex-direction: column; overflow: hidden; }
:deep(.custom-left-tabs > .ant-tabs-content-holder > .ant-tabs-content) { flex: 1; height: 100%; display: flex; flex-direction: column; }
:deep(.custom-left-tabs > .ant-tabs-content-holder > .ant-tabs-content > .ant-tabs-tabpane) { flex: 1; height: 100%; display: flex; flex-direction: column; }
.custom-left-tabs :deep(.ant-tabs-nav-list) { padding-top: 16px; width: 220px; }
.custom-left-tabs :deep(.ant-tabs-tab) { padding: 24px 24px !important; font-size: 18px; font-weight: 900; color: #94a3b8; transition: all 0.2s;}
.custom-left-tabs :deep(.ant-tabs-tab-active) { background: #eef2ff; color: #4338ca; border-right: 4px solid #4338ca; }

.flex-1-table-container { flex: 1; min-height: 0; position: relative; display: flex; flex-direction: column; }
.full-height-table { position: absolute; top: 0; left: 0; right: 0; bottom: 0; }
.full-height-table :deep(.ant-table-wrapper), .full-height-table :deep(.ant-spin-nested-loading), .full-height-table :deep(.ant-spin-container), .full-height-table :deep(.ant-table), .full-height-table :deep(.ant-table-container) { height: 100%; display: flex; flex-direction: column; min-height: 0; }
.full-height-table :deep(.ant-table-body) { flex: 1; overflow-y: auto !important; min-height: 0; }
.full-height-table :deep(.ant-table-thead > tr > th) { background: #f8fafc !important; color: #64748b !important; font-size: 16px; font-weight: 900; padding: 16px 16px !important; border-bottom: 2px solid #e2e8f0; position: sticky; top: 0; z-index: 10;}
.full-height-table :deep(.ant-table-cell) { padding: 12px 16px !important; font-size: 16px; color: #334155; }
.full-height-table :deep(.ant-table-body)::-webkit-scrollbar { width: 12px; height: 12px; }
.full-height-table :deep(.ant-table-body)::-webkit-scrollbar-thumb { background: #94a3b8; border-radius: 6px; }
.full-height-table :deep(.ant-table-body)::-webkit-scrollbar-track { background: #f1f5f9; border-radius: 6px; }
</style>
