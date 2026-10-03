<script lang="ts" setup>
import { ref, onMounted } from 'vue';
import { Page, useVbenModal } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';
import { Button, Tabs, TabPane, Tag, Modal as AModal, Table as ATable, Input, Select } from 'ant-design-vue';
import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { getTaskList } from '#/api/mes/work-order-booking/mock';
import { taskColumns, STATUS_MAP } from './data';
import OperatorLoginModal from './modules/operator-login-modal.vue';
import TaskDetailModal from './modules/task-detail-modal.vue';
import dayjs from 'dayjs';

defineOptions({ name: 'MesWorkOrderBooking' });

// 设备大屏状态
const workstationInfo = ref({ name: '打磨压槽主控线-01', status: '运行中', temp: '24.5', humidity: '55', power: '3.2' });
const lastOperator = ref<string>('暂无');
const operatorHistoryVisible = ref(false);
const operatorHistoryList = ref([
  { time: '2026-03-05 08:00:12', type: '上线打卡', user: '李师傅(8802)' },
  { time: '2026-03-05 12:30:00', type: '下线打卡', user: '李师傅(8802)' },
  { time: '2026-03-05 12:35:10', type: '上线打卡', user: '张三(8801)' }
]);

const [LoginModal, loginModalApi] = useVbenModal({ connectedComponent: OperatorLoginModal });
const [DetailModal, detailModalApi] = useVbenModal({ connectedComponent: TaskDetailModal });

const activeTab = ref('ALL');

// 1. 定义查询条件
const queryParams = ref({
  id: '',
  product: '',
  process: undefined
});

const PROCESS_OPTIONS = [
  { label: '配料', value: '配料' }, { label: '温法', value: '温法' },
  { label: '粗磨', value: '粗磨' }, { label: '精磨', value: '精磨' },
  { label: '粘双面胶', value: '粘双面胶' }, { label: '分切', value: '分切' },
  { label: '单片压槽', value: '单片压槽' }, { label: '单片背胶', value: '单片背胶' }, { label: '裁圆', value: '裁圆' }
];

// 2. 表格初始化配置
const [Grid, gridApi] = useVbenVxeGrid({
  gridOptions: {
    columns: taskColumns,
    pagerConfig: { enabled: false }, // 终端看板不分页
    height: 'auto',
    border: true,
    rowConfig: { isHover: true, height: 46 },
    // 强制开启 Toolbar 以渲染插槽与右侧的刷新/全屏/列配置按钮
    toolbarConfig: {
      refresh: true,
      zoom: true,
      custom: true
    }
  }
});

// 3. 稳健的数据加载逻辑
const loadData = async () => {
  gridApi.setGridOptions({ loading: true });
  // 调用 Mock 接口，并传入状态 Tab 和 查询表单参数
  const data = await getTaskList(activeTab.value, queryParams.value);
  gridApi.setGridOptions({ data: data as any[], loading: false });
};

const resetQuery = () => {
  queryParams.value = { id: '', product: '', process: undefined };
  loadData();
};

const handleTabChange = () => {
  loadData();
};

const handleLoginAction = (type: 'login' | 'logout') => {
  loginModalApi.setData({ type }).open();
};

const onLoginSuccess = ({ type, user }: any) => {
  const time = dayjs().format('YYYY-MM-DD HH:mm:ss');
  operatorHistoryList.value.unshift({ time, type: type === 'login' ? '上线打卡' : '下线打卡', user });
  if (type === 'login') lastOperator.value = user;
  else if (lastOperator.value === user) lastOperator.value = '暂无 (已下线)';
};

onMounted(() => {
  loadData();
  if (operatorHistoryList.value.length > 0) lastOperator.value = operatorHistoryList.value[0].user;
});
</script>

<template>
  <Page auto-content-height>
    <div class="h-full flex flex-col bg-slate-100 gap-3 p-3">

      <div class="shrink-0 bg-white rounded-xl p-4 shadow-sm border border-slate-200 flex justify-between items-center text-slate-700">
        <div class="flex items-center gap-4">
          <div class="w-12 h-12 bg-indigo-50 rounded-xl border border-indigo-200 flex items-center justify-center"><IconifyIcon icon="lucide:monitor-play" class="text-2xl text-indigo-600" /></div>
          <div><div class="text-[11px] text-slate-400 uppercase tracking-wider mb-0.5">当前工作站</div><div class="text-lg font-black text-slate-800 tracking-wide">{{ workstationInfo.name }}</div></div>
          <Tag color="success" class="ml-2 !border-none bg-emerald-100 text-emerald-700 font-bold">正在运行</Tag>
        </div>

        <div class="flex gap-8 px-8 border-x border-slate-200">
          <div class="text-center"><div class="text-[11px] text-slate-400 font-bold mb-1">环境温度</div><div class="text-lg font-mono font-black text-indigo-600">{{ workstationInfo.temp }} <span class="text-xs">℃</span></div></div>
          <div class="text-center"><div class="text-[11px] text-slate-400 font-bold mb-1">环境湿度</div><div class="text-lg font-mono font-black text-blue-500">{{ workstationInfo.humidity }} <span class="text-xs">%RH</span></div></div>
          <div class="text-center"><div class="text-[11px] text-slate-400 font-bold mb-1">实时能耗</div><div class="text-lg font-mono font-black text-amber-500">{{ workstationInfo.power }} <span class="text-xs">kW</span></div></div>
        </div>

        <div class="flex items-center gap-4">
          <div class="flex flex-col items-end mr-2 cursor-pointer group p-2 hover:bg-slate-50 rounded-lg transition-colors" @click="operatorHistoryVisible = true" title="点击查看所有操作人履历">
            <span class="text-[11px] text-slate-400 font-bold mb-0.5 flex items-center gap-1">最后操作人 <IconifyIcon icon="lucide:history" class="text-indigo-400 group-hover:text-indigo-600"/></span>
            <span class="font-black text-slate-800">{{ lastOperator }}</span>
          </div>
          <div class="flex gap-2">
            <Button type="primary" class="bg-indigo-600 font-bold" @click="handleLoginAction('login')">刷卡上线</Button>
            <Button class="font-bold text-slate-500" @click="handleLoginAction('logout')">下线退出</Button>
          </div>
        </div>
      </div>

      <div class="flex-1 min-h-0 bg-white rounded-xl shadow-sm border border-slate-200 flex flex-col overflow-hidden">

        <Tabs v-model:activeKey="activeTab" @change="handleTabChange" class="custom-list-tabs px-4 pt-2 shrink-0">
          <TabPane key="ALL" tab="全部排产任务" />
          <TabPane key="PENDING" tab="待开工" />
          <TabPane key="IN_PROGRESS" tab="生产中" />
          <TabPane key="COMPLETED" tab="已完工" />
        </Tabs>

        <div class="flex-1 relative p-2 pt-0 min-h-0 bg-slate-50/50 custom-toolbar-grid">
          <Grid class="h-full">

            <template #toolbar-tools>
              <div class="flex items-center gap-2 pr-4 border-r border-slate-200">
                <Input v-model:value="queryParams.id" allow-clear placeholder="工单号" class="w-36" />
                <Input v-model:value="queryParams.product" allow-clear placeholder="加工产品" class="w-36" />
                <Select v-model:value="queryParams.process" allow-clear placeholder="全部工序" :options="PROCESS_OPTIONS" class="w-28" />
                <Button type="primary" @click="loadData" class="bg-indigo-600 shadow-sm font-bold ml-1"><IconifyIcon icon="lucide:search" class="mr-1"/>检索</Button>
                <Button @click="resetQuery">重置</Button>
              </div>
            </template>

            <template #goodSlot="{ row }"><span class="text-emerald-600 font-bold">{{ row.goodQty }}</span></template>
            <template #statusSlot="{ row }">
              <div class="flex items-center justify-center gap-1.5">
                <span class="w-2 h-2 rounded-full shadow-sm" :class="STATUS_MAP[row.status]?.badge"></span>
                <span class="text-xs font-bold" :class="row.status === 'IN_PROGRESS' ? 'text-blue-600' : 'text-slate-600'">{{ STATUS_MAP[row.status]?.text }}</span>
              </div>
            </template>
            <template #actionSlot="{ row }">
              <Button type="primary" size="small" class="bg-indigo-600 text-xs font-bold shadow-sm" @click="detailModalApi.setData(row).open()">
                <IconifyIcon icon="lucide:log-in" class="mr-1"/> 进站执行
              </Button>
            </template>
          </Grid>
        </div>
      </div>

      <LoginModal @success="onLoginSuccess" />
      <DetailModal @refresh="loadData" />

      <AModal v-model:open="operatorHistoryVisible" title="👨‍🔧 当天工位人员操作履历" :footer="null" :width="500" centered>
        <div class="pt-4 pb-2">
          <ATable :dataSource="operatorHistoryList" :pagination="false" size="small" bordered>
            <ATable.Column title="发生时间" dataIndex="time" />
            <ATable.Column title="动作类型" dataIndex="type">
              <template #default="{ text }"><Tag :color="text.includes('上线') ? 'blue' : 'default'">{{ text }}</Tag></template>
            </ATable.Column>
            <ATable.Column title="操作员工" dataIndex="user" />
          </ATable>
        </div>
      </AModal>

    </div>
  </Page>
</template>

<style scoped>
:deep(.vxe-body--column) { padding: 6px 0 !important; }
:deep(.custom-list-tabs .ant-tabs-nav) { margin-bottom: 0 !important; border-bottom: 1px solid #f1f5f9; }

/* 调整 Toolbar 内边距，使其与我们自定义的紧凑查询控件融合对齐 */
.custom-toolbar-grid :deep(.vxe-toolbar) { padding: 8px 16px; background-color: #ffffff; border-bottom: 1px solid #f1f5f9; border-radius: 8px 8px 0 0; }
</style>
