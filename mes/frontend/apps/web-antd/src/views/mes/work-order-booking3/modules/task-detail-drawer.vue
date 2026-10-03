<script lang="ts" setup>
import { ref } from 'vue';
import { Drawer, Button, Tabs, TabPane, Tag, Dropdown, Menu, message } from 'ant-design-vue';
import { IconifyIcon } from '@vben/icons';
import DeviceCheckModal from './device-check-modal.vue';
import FinishBookingModal from './finish-booking-modal.vue';
import { useVbenModal } from '@vben/common-ui';

const emit = defineEmits(['refresh']);
const activeTab = ref('sop');
const task = ref<any>(null);
const isOpen = ref(false);

const [CheckModal, checkModalApi] = useVbenModal({ connectedComponent: DeviceCheckModal });
const [BookingModal, bookingModalApi] = useVbenModal({ connectedComponent: FinishBookingModal });

const openDrawer = (record: any) => {
  task.value = record;
  isOpen.value = true;
  activeTab.value = 'sop';
};

const handleQualityInspect = ({ key }: any) => {
  message.success(`已向质检部下发【${key}】申请！`);
};

const handleBookingSuccess = () => {
  emit('refresh');
  isOpen.value = false;
};

defineExpose({ openDrawer });
</script>

<template>
  <Drawer v-model:open="isOpen" placement="right" width="100%" :closable="false" :bodyStyle="{ padding: 0, display: 'flex', flexDirection: 'column', backgroundColor: '#f8fafc' }">

    <div class="h-16 bg-slate-800 text-white flex items-center justify-between px-6 shrink-0 shadow-md">
      <div class="flex items-center gap-6">
        <Button type="text" class="text-slate-300 hover:text-white p-0" @click="isOpen = false"><IconifyIcon icon="lucide:arrow-left" class="text-2xl" /></Button>
        <div class="h-8 w-px bg-slate-600"></div>
        <div class="flex flex-col leading-tight">
          <span class="text-xs text-slate-400">执行工单</span>
          <span class="text-lg font-bold text-blue-300">{{ task?.id }}</span>
        </div>
        <div class="flex flex-col leading-tight">
          <span class="text-xs text-slate-400">加工产品 | 工序</span>
          <span class="text-base font-bold">{{ task?.product }} <Tag color="blue" class="ml-2 border-none bg-blue-500/20 text-blue-300">{{ task?.process }}</Tag></span>
        </div>
      </div>

      <div class="flex items-center gap-3">
        <Button type="primary" ghost class="border-slate-500 text-slate-200 hover:text-white" @click="checkModalApi.open()"><IconifyIcon icon="lucide:clipboard-check" class="mr-1"/> 设备点检/参数</Button>

        <Dropdown>
          <Button type="primary" ghost class="border-slate-500 text-slate-200 hover:text-white">
            <IconifyIcon icon="lucide:microscope" class="mr-1"/> 质量检验 <IconifyIcon icon="lucide:chevron-down" class="ml-1"/>
          </Button>
          <template #overlay>
            <Menu @click="handleQualityInspect">
              <Menu.Item key="首件检验 (FAI)">首件检验 (FAI)</Menu.Item>
              <Menu.Item key="过程抽检 (IPQC)">过程抽检 (IPQC)</Menu.Item>
              <Menu.Item key="成品检验 (FQC)">成品检验 (FQC)</Menu.Item>
            </Menu>
          </template>
        </Dropdown>

        <div class="w-px h-6 bg-slate-600 mx-1"></div>
        <Button type="primary" class="bg-emerald-600 hover:bg-emerald-500 font-bold px-6" @click="bookingModalApi.setData(task).open()">
          <IconifyIcon icon="lucide:check-square" class="mr-1"/> 完工报工
        </Button>
      </div>
    </div>

    <div class="flex-1 p-6 overflow-hidden flex flex-col">
      <div class="bg-white flex-1 rounded-xl shadow-sm border border-slate-200 flex flex-col overflow-hidden p-2">
        <Tabs v-model:activeKey="activeTab" class="h-full custom-tabs">
          <TabPane key="sop" tab="📖 ESOP 电子图纸">
            <div class="p-10 flex justify-center items-center h-full bg-slate-50">
              <div class="text-center text-slate-400 border-2 border-dashed border-slate-300 rounded-xl p-10 bg-white w-full max-w-3xl">
                <IconifyIcon icon="lucide:image" class="text-6xl mx-auto mb-4" />
                <div class="font-bold text-lg mb-1">《{{ task?.process }} - 标准作业指导书》</div>
                <div>支持鼠标滚轮缩放查看图纸明细</div>
              </div>
            </div>
          </TabPane>
          <TabPane key="bom" tab="📦 物料防错清单">
            <div class="p-6">
              <div class="bg-indigo-50 p-4 rounded-lg border border-indigo-100 text-indigo-700 font-bold mb-4 flex items-center">
                <IconifyIcon icon="lucide:scan-barcode" class="mr-2 text-xl" /> 请扫描下方要求的组件条码进行投料匹配
              </div>
              <table class="w-full text-left border-collapse border border-slate-200">
                <thead class="bg-slate-100"><tr><th class="p-3 border-b">物料名称</th><th class="p-3 border-b">需求数量</th><th class="p-3 border-b">已扫入数量</th><th class="p-3 border-b">状态</th></tr></thead>
                <tbody>
                <tr><td class="p-3 border-b">特种基材</td><td class="p-3 border-b">100 m</td><td class="p-3 border-b">0 m</td><td class="p-3 border-b"><Tag color="warning">待投料</Tag></td></tr>
                </tbody>
              </table>
            </div>
          </TabPane>
          <TabPane key="log" tab="⏱️ 操作履历">
            <div class="p-6 text-slate-500">记录该工单在当前工位的所有行为...</div>
          </TabPane>
        </Tabs>
      </div>
    </div>

    <CheckModal />
    <BookingModal @success="handleBookingSuccess" />
  </Drawer>
</template>

<style scoped>
:deep(.custom-tabs .ant-tabs-nav) { padding: 0 16px; margin-bottom: 0; }
:deep(.custom-tabs .ant-tabs-content-holder) { flex: 1; overflow: hidden; display: flex; flex-direction: column; }
:deep(.custom-tabs .ant-tabs-tabpane) { flex: 1; overflow: auto; }
</style>
