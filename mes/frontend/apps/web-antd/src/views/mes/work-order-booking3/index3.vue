<script lang="ts" setup>
import { ref, computed } from 'vue';
import { Page } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';
import {
  message, Tag as ATag, Button as AButton, InputSearch as AInputSearch,
  Form as AForm, FormItem as AFormItem, Input as AInput, InputNumber as AInputNumber,
  Select as ASelect, Divider as ADivider, Drawer as ADrawer, Tabs as ATabs, TabPane as ATabPane,
  Timeline as ATimeline, TimelineItem as ATimelineItem, Progress as AProgress, Popconfirm as APopconfirm
} from 'ant-design-vue';
import { useVbenVxeGrid } from '#/adapter/vxe-table';
import dayjs from 'dayjs';

defineOptions({ name: 'MesWorkOrderBooking' });

// ==================== 1. 拟真工单主数据 ====================
const mockWorkOrders = ref([
  { id: 'WO260302-001', planNo: 'W26P0100', product: 'DTP0213 (775)', process: '配料', planQty: 100, goodQty: 100, scrapQty: 0, status: 'COMPLETED', operator: '张师傅', startTime: '2026-03-02 08:00', endTime: '2026-03-02 09:30' },
  { id: 'WO260302-002', planNo: 'W26P0100', product: 'DTP0213 (775)', process: '湿法', planQty: 100, goodQty: 45, scrapQty: 2, status: 'IN_PROGRESS', operator: '李师傅', startTime: '2026-03-02 10:00', endTime: null },
  { id: 'WO260302-003', planNo: 'W26P0100', product: 'DTP0213 (775)', process: '磨皮(一)', planQty: 100, goodQty: 0, scrapQty: 0, status: 'PENDING', operator: null, startTime: null, endTime: null },
  { id: 'WO260302-004', planNo: 'W26P0101', product: 'DTP0213 (775)', process: '分切', planQty: 50, goodQty: 50, scrapQty: 1, status: 'COMPLETED', operator: '王师傅', startTime: '2026-03-01 13:00', endTime: '2026-03-01 17:00' },
  { id: 'WO260302-005', planNo: 'W26P0101', product: 'DTP0213 (775)', process: '压槽', planQty: 50, goodQty: 20, scrapQty: 0, status: 'IN_PROGRESS', operator: '赵师傅', startTime: '2026-03-02 08:30', endTime: null },
]);

const statusMap: Record<string, { text: string; color: string; badge: string }> = {
  'PENDING': { text: '待开工', color: 'default', badge: 'bg-slate-300' },
  'IN_PROGRESS': { text: '生产中', color: 'processing', badge: 'bg-blue-500 animate-pulse' },
  'COMPLETED': { text: '已完工', color: 'success', badge: 'bg-emerald-500' },
  'PAUSED': { text: '暂停/异常', color: 'error', badge: 'bg-red-500' },
};

// ==================== 2. 主界面列表配置 ====================
const searchKeyword = ref('');
const [Grid, gridApi] = useVbenVxeGrid({
  gridOptions: {
    columns: [
      { field: 'id', title: '执行工单号', width: 140, fixed: 'left', slots: { default: 'idSlot' } },
      { field: 'product', title: '加工产品', minWidth: 160, slots: { default: 'productSlot' } },
      { field: 'process', title: '当前工序', width: 100, slots: { default: 'processSlot' } },
      { field: 'planQty', title: '计划量', width: 80, align: 'right' },
      { field: 'goodQty', title: '良品数', width: 80, align: 'right', slots: { default: 'goodSlot' } },
      { field: 'status', title: '状态', width: 100, align: 'center', slots: { default: 'statusSlot' } },
      { field: 'startTime', title: '实际开工时间', width: 140 },
      { field: 'action', title: '执行操作', width: 110, fixed: 'right', slots: { default: 'actionSlot' } }
    ],
    data: mockWorkOrders.value,
    pagerConfig: { enabled: true, pageSize: 20 },
    height: 'auto',
    border: true,
    rowConfig: { isHover: true, height: 50 }
  }
});

// ==================== 3. 鼎华风格 - 沉浸式工作台控制逻辑 ====================
const isStationVisible = ref(false);
const activeTab = ref('1'); // 1:报工作业 2:投料防错 3:SOP指导
const currentWO = ref<any>(null);
const scanBarcode = ref('');

// 动态工艺参数字典 (源自上传的CSV)
const processParamConfig: Record<string, any[]> = {
  '配料': [{ key: 'temp', label: '车间温度', unit: '℃' }, { key: 'humidity', label: '湿度', unit: '%RH' }],
  '湿法': [{ key: 'level', label: '料槽液位高度', unit: 'CM' }, { key: 'gap', label: '刀口间隙', unit: 'mm' }],
  '分切': [{ key: 'length', label: '分切米数', unit: 'm' }],
  '压槽': [{ key: 'gap', label: '压辊间隙', unit: 'mm' }, { key: 'temp', label: '压辊温度', unit: '℃' }, { key: 'laser', label: '激光定位值', unit: 'mm' }]
};

// 模拟防错投料清单 (BOM)
const materialList = ref<any[]>([]);

// 模拟操作日志
const actionLogs = ref<{time: string; action: string; type: string}[]>([]);

// 报工表单模型
const bookingForm = ref<any>({ goodQty: 0, scrapQty: 0, params: {} });

// 打开执行工作台
const openWorkstation = (row: any) => {
  currentWO.value = row;
  activeTab.value = '1';

  // 初始化报工表单
  bookingForm.value = {
    goodQty: row.planQty - row.goodQty > 0 ? row.planQty - row.goodQty : 0,
    scrapQty: 0,
    params: {}
  };

  // 生成模拟投料清单
  materialList.value = [
    { id: 'RM-001', name: '特种基材', spec: '卷料', requireQty: row.planQty * 1.5, issuedQty: row.status==='IN_PROGRESS'? row.planQty*1.5 : 0, unit: 'm' },
    { id: 'RM-002', name: '处理液A', spec: '桶装', requireQty: row.planQty * 0.2, issuedQty: 0, unit: 'L' }
  ];

  // 生成履历
  actionLogs.value = [];
  if (row.startTime) {
    actionLogs.value.push({ time: row.startTime, action: '工单扫码开工', type: 'success' });
  } else {
    actionLogs.value.push({ time: dayjs().format('HH:mm:ss'), action: '进入工作台，待开工', type: 'info' });
  }

  isStationVisible.value = true;
};

// 模拟投料扫码
const handleScanMaterial = () => {
  if (!scanBarcode.value) return;
  const target = materialList.value.find(m => m.issuedQty < m.requireQty);
  if (target) {
    target.issuedQty = target.requireQty; // 模拟直接满足
    actionLogs.value.unshift({ time: dayjs().format('HH:mm:ss'), action: `物料防错通过: [${target.name}] 已扫码投料`, type: 'success' });
    message.success('条码验证成功，准许投料！');
  } else {
    message.error('未找到匹配物料或已投满！');
  }
  scanBarcode.value = '';
};

// 动作执行函数
const execAction = (action: string) => {
  const time = dayjs().format('HH:mm:ss');

  if (action === 'start') {
    currentWO.value.status = 'IN_PROGRESS';
    currentWO.value.startTime = dayjs().format('YYYY-MM-DD HH:mm');
    actionLogs.value.unshift({ time, action: '触发开工指令，设备联机正常', type: 'success' });
    message.success('工单已开工');
  }
  else if (action === 'pause') {
    currentWO.value.status = 'PAUSED';
    actionLogs.value.unshift({ time, action: '触发暂停指令，等待异常解除', type: 'error' });
    message.warning('工单已挂起');
  }
  else if (action === 'submit') {
    // 防呆拦截：检查投料是否齐套
    const isMaterialReady = materialList.value.every(m => m.issuedQty >= m.requireQty);
    if (!isMaterialReady && currentWO.value.process !== '包装') {
      activeTab.value = '2';
      return message.error('防呆拦截：请先完成扫码投料与防错验证！');
    }

    if (bookingForm.value.goodQty + bookingForm.value.scrapQty <= 0) {
      return message.warning('报工数量不能为0');
    }

    currentWO.value.goodQty += bookingForm.value.goodQty;
    currentWO.value.scrapQty += bookingForm.value.scrapQty;

    if (currentWO.value.goodQty >= currentWO.value.planQty) {
      currentWO.value.status = 'COMPLETED';
      currentWO.value.endTime = dayjs().format('YYYY-MM-DD HH:mm');
    }

    actionLogs.value.unshift({ time, action: `产出登记：良品+${bookingForm.value.goodQty}，不良+${bookingForm.value.scrapQty}`, type: 'success' });
    message.success('过站报工成功！数据已上传ERP。');

    // 更新外部网格
    gridApi.setGridOptions({ data: [...mockWorkOrders.value] });
    if (currentWO.value.status === 'COMPLETED') {
      setTimeout(() => { isStationVisible.value = false; }, 800);
    }
  }
};
</script>

<template>
  <Page auto-content-height>
    <div class="h-full flex flex-col bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
      <div class="h-14 shrink-0 bg-slate-50 border-b border-slate-200 flex items-center justify-between px-4">
        <div class="flex items-center gap-3">
          <IconifyIcon icon="lucide:monitor-play" class="text-indigo-600 text-xl" />
          <span class="font-black text-slate-800 text-[15px]">生产现场执行大厅 (HMI)</span>
        </div>
        <div class="flex items-center gap-3">
          <a-input-search v-model:value="searchKeyword" placeholder="请扫描工单条码..." class="w-[280px]" />
        </div>
      </div>
      <div class="flex-1 min-h-0 relative p-2">
        <Grid class="h-full">
          <template #idSlot="{ row }"><span class="font-mono font-bold text-slate-700">{{ row.id }}</span></template>
          <template #productSlot="{ row }">
            <div class="font-bold text-indigo-900">{{ row.product }}</div>
            <div class="text-[10px] text-slate-400">计划号: {{ row.planNo }}</div>
          </template>
          <template #processSlot="{ row }">
            <span class="bg-indigo-100 text-indigo-700 px-2 py-0.5 rounded text-[12px] font-bold">{{ row.process }}</span>
          </template>
          <template #goodSlot="{ row }"><span class="text-emerald-600 font-bold text-[14px]">{{ row.goodQty }}</span></template>
          <template #statusSlot="{ row }">
            <div class="flex items-center justify-center gap-1.5">
              <span class="w-2 h-2 rounded-full shadow-sm" :class="statusMap[row.status]?.badge"></span>
              <span class="text-xs font-bold" :class="row.status === 'IN_PROGRESS' ? 'text-blue-600' : 'text-slate-600'">{{ statusMap[row.status]?.text }}</span>
            </div>
          </template>
          <template #actionSlot="{ row }">
            <a-button type="primary" size="small" class="bg-indigo-600 shadow-sm" @click="openWorkstation(row)">
              <IconifyIcon icon="lucide:monitor" class="mr-1"/> 进站
            </a-button>
          </template>
        </Grid>
      </div>
    </div>

    <a-drawer
      v-model:open="isStationVisible"
      placement="right"
      width="100%"
      :closable="false"
      :bodyStyle="{ padding: 0, backgroundColor: '#f1f5f9', display: 'flex', flexDirection: 'column' }"
      destroyOnClose
    >
      <div v-if="currentWO" class="w-full h-full flex flex-col">

        <div class="h-16 bg-slate-800 text-white flex items-center justify-between px-6 shrink-0 shadow-md z-10">
          <div class="flex items-center gap-6">
            <a-button type="text" class="text-slate-300 hover:text-white p-0 flex items-center" @click="isStationVisible = false">
              <IconifyIcon icon="lucide:arrow-left" class="text-2xl" />
            </a-button>
            <div class="h-8 w-px bg-slate-600"></div>
            <div>
              <div class="text-[11px] text-slate-400 uppercase tracking-widest mb-0.5">Work Order / 工单号</div>
              <div class="text-lg font-mono font-bold leading-none tracking-wide text-blue-300">{{ currentWO.id }}</div>
            </div>
            <div>
              <div class="text-[11px] text-slate-400 uppercase tracking-widest mb-0.5">Product / 加工产品</div>
              <div class="text-base font-bold leading-none">{{ currentWO.product }}</div>
            </div>
            <div>
              <div class="text-[11px] text-slate-400 uppercase tracking-widest mb-0.5">Process / 当前工序</div>
              <div class="text-base font-bold leading-none text-emerald-400 border border-emerald-500/50 bg-emerald-500/10 px-2 py-0.5 rounded">{{ currentWO.process }}</div>
            </div>
          </div>

          <div class="flex items-center gap-6 w-[350px]">
            <div class="flex-1">
              <div class="flex justify-between text-[11px] text-slate-300 mb-1">
                <span>良品进度 ({{ currentWO.goodQty }}/{{ currentWO.planQty }})</span>
                <span class="text-emerald-400 font-bold">{{ Math.round((currentWO.goodQty/currentWO.planQty)*100) }}%</span>
              </div>
              <a-progress :percent="(currentWO.goodQty/currentWO.planQty)*100" :showInfo="false" strokeColor="#34d399" trailColor="#475569" size="small" />
            </div>
            <div class="text-center">
              <div class="text-[11px] text-slate-400 uppercase tracking-widest mb-0.5">Status</div>
              <div class="flex items-center gap-1.5">
                <span class="w-2.5 h-2.5 rounded-full shadow-[0_0_8px_currentColor]" :class="statusMap[currentWO.status]?.badge"></span>
                <span class="text-sm font-bold">{{ statusMap[currentWO.status]?.text }}</span>
              </div>
            </div>
          </div>
        </div>

        <div class="flex-1 flex min-h-0 overflow-hidden p-4 gap-4">

          <div class="flex-[2] bg-white rounded-xl shadow-sm border border-slate-200 flex flex-col overflow-hidden">
            <a-tabs v-model:activeKey="activeTab" class="custom-hmi-tabs px-2">

              <a-tab-pane key="1" tab="🎯 报工作业与工艺收集">
                <div class="p-6 overflow-y-auto h-full">
                  <div class="grid grid-cols-2 gap-8">

                    <div class="bg-slate-50 border border-slate-200 rounded-xl p-5">
                      <div class="text-sm font-black text-slate-700 border-b border-slate-200 pb-3 mb-5 flex items-center">
                        <IconifyIcon icon="lucide:package-check" class="text-emerald-500 mr-2 text-lg"/> 产出登记面板
                      </div>
                      <div class="mb-6">
                        <div class="text-slate-500 mb-2 font-bold">✅ 合格良品数量 (PCS)</div>
                        <a-input-number v-model:value="bookingForm.goodQty" :min="0" class="w-full text-2xl text-emerald-600 font-mono font-black h-12 custom-hmi-input" />
                      </div>
                      <div>
                        <div class="text-slate-500 mb-2 font-bold flex justify-between">
                          <span>❌ 不良报废数量 (PCS)</span>
                          <span v-if="bookingForm.scrapQty > 0" class="text-red-500 text-xs animate-bounce">请在右下角记录不良代码</span>
                        </div>
                        <a-input-number v-model:value="bookingForm.scrapQty" :min="0" class="w-full text-2xl text-red-500 font-mono font-black h-12 custom-hmi-input" />
                      </div>
                    </div>

                    <div class="bg-slate-50 border border-slate-200 rounded-xl p-5 relative">
                      <div v-if="currentWO.status === 'PENDING'" class="absolute inset-0 bg-white/60 backdrop-blur-[1px] z-10 flex flex-col items-center justify-center rounded-xl">
                        <IconifyIcon icon="lucide:lock" class="text-4xl text-slate-400 mb-2"/>
                        <span class="font-bold text-slate-500">请先点击右侧【开工】解锁工艺表单</span>
                      </div>

                      <div class="text-sm font-black text-slate-700 border-b border-slate-200 pb-3 mb-5 flex items-center">
                        <IconifyIcon icon="lucide:settings-2" class="text-blue-500 mr-2 text-lg"/> 现场工艺点检记录
                      </div>

                      <a-form layout="vertical">
                        <template v-if="processParamConfig[currentWO.process]">
                          <a-form-item v-for="param in processParamConfig[currentWO.process]" :key="param.key" class="mb-4">
                            <template #label><span class="font-bold text-slate-600">{{ param.label }}</span></template>
                            <a-input-number v-model:value="bookingForm.params[param.key]" class="w-full h-10 custom-hmi-input text-lg" :addon-after="param.unit" placeholder="点检录入" />
                          </a-form-item>
                        </template>
                        <div v-else class="text-center text-slate-400 py-10">当前工序无强制工艺参数收集要求</div>
                      </a-form>
                    </div>
                  </div>
                </div>
              </a-tab-pane>

              <a-tab-pane key="2" tab="🔍 投料扫码防错">
                <div class="p-6 h-full flex flex-col">
                  <div class="bg-indigo-50 border border-indigo-200 rounded-lg p-4 flex gap-4 mb-4">
                    <div class="flex-1">
                      <div class="text-indigo-800 font-bold mb-2">扫描物料批次条码：</div>
                      <a-input-search v-model:value="scanBarcode" size="large" enter-button="确认投料" @search="handleScanMaterial" placeholder="请使用扫码枪扫描物料二维码..." />
                    </div>
                    <div class="w-64 text-xs text-indigo-600/70 bg-indigo-100/50 p-2 rounded">
                      <IconifyIcon icon="lucide:info" class="inline mb-0.5" /> 防呆说明：系统将自动校验扫描批次是否符合工艺BOM，数量不足禁止后续报工过站。
                    </div>
                  </div>

                  <div class="flex-1 border border-slate-200 rounded-lg overflow-hidden">
                    <table class="w-full text-left text-[13px] border-collapse">
                      <thead class="bg-slate-100 text-slate-600 border-b border-slate-200">
                      <tr>
                        <th class="p-3">物料编码 / 名称</th>
                        <th class="p-3 text-right">标准需领量</th>
                        <th class="p-3 text-right">已扫码投入</th>
                        <th class="p-3 text-center">防错状态</th>
                      </tr>
                      </thead>
                      <tbody class="divide-y divide-slate-100">
                      <tr v-for="mat in materialList" :key="mat.id" class="hover:bg-slate-50 transition-colors">
                        <td class="p-3 font-bold text-slate-700">{{ mat.id }} <span class="font-normal text-slate-500 block text-xs mt-0.5">{{ mat.name }} ({{ mat.spec }})</span></td>
                        <td class="p-3 text-right font-mono">{{ mat.requireQty }}{{ mat.unit }}</td>
                        <td class="p-3 text-right font-mono text-blue-600 font-bold">{{ mat.issuedQty }}{{ mat.unit }}</td>
                        <td class="p-3 text-center">
                          <IconifyIcon v-if="mat.issuedQty >= mat.requireQty" icon="lucide:check-circle-2" class="text-emerald-500 text-xl mx-auto" />
                          <IconifyIcon v-else icon="lucide:clock-4" class="text-orange-400 text-xl mx-auto" />
                        </td>
                      </tr>
                      </tbody>
                    </table>
                  </div>
                </div>
              </a-tab-pane>

              <a-tab-pane key="3" tab="📖 ESOP电子作业书">
                <div class="p-6 h-full flex items-center justify-center bg-slate-50">
                  <div class="text-center text-slate-400 border-2 border-dashed border-slate-300 rounded-xl p-10 bg-white">
                    <IconifyIcon icon="lucide:file-image" class="text-6xl mx-auto mb-4 text-slate-300" />
                    <div class="font-bold text-lg text-slate-600 mb-1">《{{ currentWO.process }} - 标准作业指导书》</div>
                    <div>受控图纸渲染区，支持双指缩放</div>
                  </div>
                </div>
              </a-tab-pane>

            </a-tabs>
          </div>

          <div class="flex-1 flex flex-col gap-4">

            <div class="bg-slate-800 rounded-xl shadow-sm p-4 grid grid-cols-2 gap-3 shrink-0">
              <button
                class="h-16 rounded-lg font-black text-lg transition-all shadow-md active:scale-95 flex items-center justify-center gap-2"
                :class="currentWO.status === 'PENDING' ? 'bg-emerald-500 text-white hover:bg-emerald-400' : 'bg-slate-700 text-slate-500 cursor-not-allowed'"
                @click="currentWO.status === 'PENDING' && execAction('start')"
              >
                <IconifyIcon icon="lucide:play" /> 开始作业
              </button>

              <button
                class="h-16 rounded-lg font-black text-lg transition-all shadow-md active:scale-95 flex items-center justify-center gap-2"
                :class="currentWO.status === 'IN_PROGRESS' ? 'bg-amber-500 text-white hover:bg-amber-400' : 'bg-slate-700 text-slate-500 cursor-not-allowed'"
                @click="currentWO.status === 'IN_PROGRESS' && execAction('pause')"
              >
                <IconifyIcon icon="lucide:pause" /> 异常挂起
              </button>

              <a-popconfirm title="确认产出与参数无误，执行过站报工？" ok-text="确认" cancel-text="取消" @confirm="execAction('submit')">
                <button
                  class="col-span-2 h-20 rounded-lg font-black text-2xl transition-all shadow-lg flex items-center justify-center gap-2"
                  :class="(currentWO.status === 'IN_PROGRESS' || currentWO.status === 'PAUSED') ? 'bg-blue-600 text-white hover:bg-blue-500 hover:shadow-blue-500/50' : 'bg-slate-700 text-slate-500 cursor-not-allowed'"
                  :disabled="currentWO.status === 'PENDING' || currentWO.status === 'COMPLETED'"
                >
                  <IconifyIcon icon="lucide:check-circle" /> 完工 / 过站登记
                </button>
              </a-popconfirm>
            </div>

            <div class="flex-1 bg-white rounded-xl shadow-sm border border-slate-200 p-4 flex flex-col overflow-hidden">
              <div class="font-bold text-slate-700 border-b border-slate-100 pb-2 mb-4 flex items-center">
                <IconifyIcon icon="lucide:history" class="text-indigo-500 mr-2" /> 执行追溯履历
              </div>
              <div class="flex-1 overflow-y-auto pr-2 custom-scrollbar">
                <a-timeline>
                  <a-timeline-item v-for="(log, i) in actionLogs" :key="i" :color="log.type === 'success' ? 'green' : log.type === 'error' ? 'red' : 'blue'">
                    <div class="text-[11px] text-slate-400 mb-0.5">{{ log.time }}</div>
                    <div class="text-[13px] font-medium text-slate-700">{{ log.action }}</div>
                  </a-timeline-item>
                </a-timeline>
              </div>
            </div>

          </div>

        </div>
      </div>
    </a-drawer>
  </Page>
</template>

<style scoped>
/* 穿透 VxeTable 的默认单元格间距 */
:deep(.vxe-body--column) { padding: 6px 0 !important; }

/* 定制化 Ant Design Tabs 样式以符合工业 HMI 风格 */
:deep(.custom-hmi-tabs .ant-tabs-nav) {
  margin-bottom: 0 !important;
}
:deep(.custom-hmi-tabs .ant-tabs-tab) {
  padding: 16px 24px !important;
  font-size: 15px;
  font-weight: bold;
  color: #64748b;
}
:deep(.custom-hmi-tabs .ant-tabs-tab-active) {
  color: #4f46e5 !important;
}

/* 定制巨型输入框样式 */
:deep(.custom-hmi-input .ant-input-number-input) {
  height: 100% !important;
  text-align: center;
}

.custom-scrollbar::-webkit-scrollbar { width: 6px; }
.custom-scrollbar::-webkit-scrollbar-track { background: transparent; }
.custom-scrollbar::-webkit-scrollbar-thumb { background: #cbd5e1; border-radius: 4px; }
</style>
