<script lang="ts" setup>
import { ref } from 'vue';
import { Button, Tabs, TabPane, Tag, message, Modal as AModal, DatePicker, InputNumber, Select, Form, FormItem } from 'ant-design-vue';
import { IconifyIcon } from '@vben/icons';
import { useVbenModal } from '@vben/common-ui';
import dayjs from 'dayjs';
import { PROCESS_OPTIONS } from '../data';

import StartDetailTab from './components/StartDetailTab.vue';
import DeviceCheckTab from './components/DeviceCheckTab.vue';
import BomScanTab from './components/BomScanTab.vue';
import QualityTab from './components/QualityTab.vue';
import BookingPanel from './components/BookingPanel.vue';

const emit = defineEmits(['refresh']);
const activeMainTab = ref('detail');
const task = ref<any>(null);
const viewMode = ref<'tabs' | 'booking'>('tabs');

// ==================== 数据状态挂载 ====================
const startForm = ref({
  process: '', operator: '张师傅 (8801)', batchingNo: '', motherBatchNo: '', motherLength: 0, planBaseQty: 0, planPieceQty: 0, batchPool: [] as any[]
});

const bookingForm = ref({
  laborHours: 0, startTime: '', endTime: '', nextProcess: '包装入库', remark: '', goodQty: 0, scrapQty: 0, lossQty: 0, scrapReason: undefined, batchSelfInspect: 'OK'
});

const startTask = () => {
  if (task.value) {
    task.value.process = startForm.value.process;
    task.value.status = 'IN_PROGRESS';
    task.value.startTime = dayjs().format('YYYY-MM-DD HH:mm:ss');
    message.success(`已按【${startForm.value.process}】工序开工！`);
  }
};

const handleBookingSubmit = ({ finalGood, finalScrap }: any) => {
  message.success(`✅ 报工成功！良品: ${finalGood}, 不良: ${finalScrap}`);
  modalApi.close();
  emit('refresh');
};

const [Modal, modalApi] = useVbenModal({
  title: '生产现场终端作业台 (SFC)',
  width: 1250,
  footer: false,
  onOpenChange(isOpen) {
    if (isOpen) {
      modalApi.setState({ fullscreen: true }); // 2. 核心代码：监听弹窗打开事件，强制将其状态设置为全屏！
      task.value = modalApi.getData<any>();
      viewMode.value = 'tabs';
      activeMainTab.value = 'detail';

      startForm.value.process = task.value.process;
      startForm.value.batchingNo = `BL-${dayjs().format('MMDD')}-01`;
      startForm.value.motherBatchNo = `MB-${dayjs().format('MMDD')}-A`;
      startForm.value.motherLength = 100;
      startForm.value.planBaseQty = task.value.planQty;
      startForm.value.planPieceQty = task.value.planQty;
      startForm.value.batchPool = [{ id: '1', batchNo: `PRE-${dayjs().format('MMDD')}-1`, qty: task.value.planQty }];

      bookingForm.value.goodQty = task.value.planQty - task.value.goodQty;
      bookingForm.value.scrapQty = 0;
      bookingForm.value.lossQty = 0;
      bookingForm.value.laborHours = 8;
      bookingForm.value.startTime = task.value.startTime || dayjs().format('YYYY-MM-DD HH:mm:ss');
      bookingForm.value.endTime = dayjs().format('YYYY-MM-DD HH:mm:ss');
      bookingForm.value.remark = '';
    }
  }
});
</script>

<template>
  <Modal>
    <div class="flex flex-col bg-slate-50 relative -m-4" style="height: 80vh; min-height: 650px;">

      <div class="h-16 bg-white flex items-center justify-between px-6 shrink-0 border-b border-slate-200 z-10 shadow-sm">
        <div class="flex items-center gap-6">
          <div class="flex flex-col leading-tight">
            <span class="text-[11px] text-slate-400 font-bold uppercase">执行工单 / Work Order</span>
            <span class="text-lg font-black text-indigo-700 font-mono tracking-wide">{{ task?.id }}</span>
          </div>
          <div class="h-8 w-px bg-slate-200"></div>
          <div class="flex flex-col leading-tight">
            <span class="text-[11px] text-slate-400 font-bold uppercase">加工产品 | 当前工序</span>
            <span class="text-base font-bold text-slate-800">{{ task?.product }} <Tag color="blue" class="ml-2 !m-0 font-bold">{{ startForm.process }}</Tag></span>
          </div>
        </div>

        <div class="flex items-center gap-3">
          <template v-if="viewMode === 'tabs'">
            <Button size="large" type="primary" class="bg-blue-600 hover:bg-blue-500 font-bold px-6 shadow-sm border-none" @click="startTask" :disabled="task?.status === 'IN_PROGRESS'">
              <IconifyIcon icon="lucide:play" class="mr-1"/> {{ task?.status === 'IN_PROGRESS' ? '已开工生产中' : '执行开工确认' }}
            </Button>
            <Button size="large" type="primary" class="bg-emerald-600 hover:bg-emerald-500 font-bold px-6 shadow-md border-none" @click="viewMode = 'booking'">
              <IconifyIcon icon="lucide:check-square" class="mr-1"/> 结束完工与报工
            </Button>
          </template>
        </div>
      </div>

      <div v-show="viewMode === 'tabs'" class="flex-1 flex flex-col p-4 min-h-0 bg-slate-100 animate-fade-in">
        <div class="bg-white flex-1 min-h-0 rounded-xl shadow-sm border border-slate-200 flex flex-col overflow-hidden p-2">
          <Tabs v-model:activeKey="activeMainTab" class="h-full custom-main-tabs flex flex-col">
            <TabPane key="detail" tab="📄 工单详情与开工设置" class="h-full">
              <StartDetailTab :task="task" :startForm="startForm" />
            </TabPane>
            <TabPane key="check" tab="🛠️ 设备点检与工艺参数" class="h-full"><DeviceCheckTab /></TabPane>
            <TabPane key="bom" tab="📦 投料防错与核对" class="h-full"><BomScanTab /></TabPane>
            <TabPane key="quality" tab="🔬 质量管控提报" class="h-full"><QualityTab /></TabPane>
          </Tabs>
        </div>
      </div>

      <div v-if="viewMode === 'booking'" class="flex-1 flex flex-col overflow-hidden bg-slate-100 animate-fade-in z-20">

        <div class="bg-white p-5 shrink-0 border-b border-slate-200 shadow-sm grid grid-cols-4 gap-6 items-end z-10">
          <FormItem label="投入人工工时 (H)" class="mb-0 font-bold"><InputNumber v-model:value="bookingForm.laborHours" :min="0" :step="0.5" class="w-full text-indigo-600 font-black text-xl" size="large" placeholder="必填"/></FormItem>
          <FormItem label="实际开工时间" class="mb-0 font-bold"><DatePicker v-model:value="bookingForm.startTime" show-time valueFormat="YYYY-MM-DD HH:mm:ss" class="w-full bg-slate-50" size="large"/></FormItem>
          <FormItem label="实际完工时间" class="mb-0 font-bold"><DatePicker v-model:value="bookingForm.endTime" show-time valueFormat="YYYY-MM-DD HH:mm:ss" class="w-full bg-slate-50" size="large"/></FormItem>
          <FormItem label="转入下道工序" class="mb-0 font-bold"><Select v-model:value="bookingForm.nextProcess" class="w-full font-bold text-indigo-600" size="large" :options="PROCESS_OPTIONS.map(p=>({label:p, value:p}))"/></FormItem>
        </div>

        <div class="flex-1 min-h-0 flex bg-slate-50 p-6 gap-6">
          <div class="w-[280px] shrink-0 bg-white rounded-xl p-5 border border-slate-200 shadow-sm flex flex-col h-full">
            <h3 class="font-black text-slate-800 border-b border-slate-100 pb-3 mb-5 flex items-center shrink-0"><IconifyIcon icon="lucide:file-bar-chart-2" class="mr-2 text-indigo-500" /> 计划达成看板</h3>
            <div class="space-y-5 flex-1 min-h-0 overflow-y-auto pr-1">
              <div class="bg-slate-50 p-3 rounded-lg border border-slate-100"><div class="text-slate-400 text-xs font-bold mb-1">目标指令总数</div><div class="text-2xl font-black font-mono text-slate-700">{{ task?.planQty }} <span class="text-sm font-normal text-slate-500">PCS</span></div></div>
              <div class="bg-emerald-50 p-3 rounded-lg border border-emerald-100"><div class="text-emerald-600/70 text-xs font-bold mb-1">本工单已流转良品</div><div class="text-2xl font-black font-mono text-emerald-600">{{ task?.goodQty }} <span class="text-sm font-normal text-emerald-500">PCS</span></div></div>
              <div class="bg-slate-50 p-3 rounded-lg border border-slate-100 mt-auto"><div class="text-slate-500 font-bold mb-1 text-xs">当前状态</div><div class="font-bold">{{ task?.status === 'IN_PROGRESS' ? '生产中' : '已暂停/未开工' }}</div></div>
            </div>
          </div>

          <BookingPanel :task="task" :startForm="startForm" :bookingForm="bookingForm" @cancel="viewMode='tabs'" @submit="handleBookingSubmit" />
        </div>

      </div>
    </div>
  </Modal>
</template>

<style scoped>
:deep(.custom-main-tabs .ant-tabs-nav) { padding: 0 16px; margin-bottom: 0; background-color: #ffffff; border-radius: 8px 8px 0 0; }
:deep(.custom-main-tabs .ant-tabs-tab) { font-size: 15px; font-weight: bold; padding: 12px 16px !important; }
:deep(.custom-main-tabs .ant-tabs-content-holder) { flex: 1; overflow: hidden; display: flex; flex-direction: column; background: #f8fafc; }
:deep(.custom-main-tabs .ant-tabs-content) { flex: 1; display: flex; flex-direction: column; overflow: hidden; }
:deep(.custom-main-tabs .ant-tabs-tabpane) { flex: 1; overflow: hidden; display: flex; flex-direction: column; }
.animate-fade-in { animation: fadeIn 0.2s ease-out; }
@keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }
</style>
