<script lang="ts" setup>
import { ref, computed, onMounted } from 'vue';
import { Page, useVbenModal } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';
import { message, Tag as ATag, Button as AButton, InputSearch as AInputSearch, Form as AForm, FormItem as AFormItem, Input as AInput, InputNumber as AInputNumber, Select as ASelect, Divider as ADivider } from 'ant-design-vue';
import { useVbenVxeGrid } from '#/adapter/vxe-table';
import dayjs from 'dayjs';

defineOptions({ name: 'MesWorkOrderBooking' });

// ==================== 1. 拟真数据引擎 (结合 index.ts 计划与 CSV 工序) ====================
// 基于 index.ts 的 W26P0101、W26P0104 等计划，拆分到具体工序的执行工单
const mockWorkOrders = ref([
  { id: 'WO260302-001', planNo: 'W26P0100', product: 'DTP0213 (775)', process: '配料', planQty: 100, goodQty: 100, scrapQty: 0, status: 'COMPLETED', operator: '张师傅', startTime: '2026-03-02 08:00', endTime: '2026-03-02 09:30' },
  { id: 'WO260302-002', planNo: 'W26P0100', product: 'DTP0213 (775)', process: '湿法', planQty: 100, goodQty: 45, scrapQty: 2, status: 'IN_PROGRESS', operator: '李师傅', startTime: '2026-03-02 10:00', endTime: null },
  { id: 'WO260302-003', planNo: 'W26P0100', product: 'DTP0213 (775)', process: '磨皮(一)', planQty: 100, goodQty: 0, scrapQty: 0, status: 'PENDING', operator: null, startTime: null, endTime: null },

  { id: 'WO260302-004', planNo: 'W26P0101', product: 'DTP0213 (775)', process: '分切', planQty: 50, goodQty: 50, scrapQty: 1, status: 'COMPLETED', operator: '王师傅', startTime: '2026-03-01 13:00', endTime: '2026-03-01 17:00' },
  { id: 'WO260302-005', planNo: 'W26P0101', product: 'DTP0213 (775)', process: '压槽', planQty: 50, goodQty: 20, scrapQty: 0, status: 'IN_PROGRESS', operator: '赵师傅', startTime: '2026-03-02 08:30', endTime: null },
  { id: 'WO260302-006', planNo: 'W26P0101', product: 'DTP0213 (775)', process: '粘胶2', planQty: 50, goodQty: 0, scrapQty: 0, status: 'PENDING', operator: null, startTime: null, endTime: null },

  { id: 'WO260302-007', planNo: 'W26P0104', product: '高阶抛光垫', process: '配料', planQty: 300, goodQty: 300, scrapQty: 5, status: 'COMPLETED', operator: '张师傅', startTime: '2026-03-01 08:00', endTime: '2026-03-01 11:00' },
  { id: 'WO260302-008', planNo: 'W26P0104', product: '高阶抛光垫', process: '湿法', planQty: 300, goodQty: 0, scrapQty: 0, status: 'PENDING', operator: null, startTime: null, endTime: null },
  { id: 'WO260302-009', planNo: 'W26P0104', product: '高阶抛光垫', process: '检验', planQty: 300, goodQty: 0, scrapQty: 0, status: 'PENDING', operator: null, startTime: null, endTime: null },
]);

// 状态映射字典
const statusMap: Record<string, { text: string; color: string }> = {
  'PENDING': { text: '待开工', color: 'default' },
  'IN_PROGRESS': { text: '生产中', color: 'processing' },
  'COMPLETED': { text: '已完工', color: 'success' },
  'PAUSED': { text: '暂停/异常', color: 'error' },
};

// ==================== 2. 表格网格配置 ====================
const searchKeyword = ref('');
const [Grid, gridApi] = useVbenVxeGrid({
  gridOptions: {
    columns: [
      { field: 'id', title: '工单编号', width: 140, fixed: 'left' },
      { field: 'planNo', title: '关联计划/订单', width: 120 },
      { field: 'product', title: '生产产品', minWidth: 160 },
      { field: 'process', title: '执行工序', width: 100, slots: { default: 'processSlot' } },
      { field: 'planQty', title: '计划量', width: 80, align: 'right' },
      { field: 'goodQty', title: '合格数', width: 80, align: 'right', slots: { default: 'goodSlot' } },
      { field: 'scrapQty', title: '报废数', width: 80, align: 'right', slots: { default: 'scrapSlot' } },
      { field: 'status', title: '状态', width: 100, align: 'center', slots: { default: 'statusSlot' } },
      { field: 'operator', title: '作业员', width: 100 },
      { field: 'startTime', title: '开工时间', width: 150 },
      { field: 'action', title: '操作', width: 120, fixed: 'right', slots: { default: 'actionSlot' } }
    ],
    data: mockWorkOrders.value,
    pagerConfig: { enabled: true, pageSize: 20 },
    height: 'auto',
    border: true,
    rowConfig: { isHover: true }
  }
});

// 搜索过滤逻辑
const handleSearch = () => {
  const keyword = searchKeyword.value.trim().toLowerCase();
  if (!keyword) {
    gridApi.setGridOptions({ data: mockWorkOrders.value });
    return;
  }
  const filtered = mockWorkOrders.value.filter(item =>
    item.id.toLowerCase().includes(keyword) ||
    item.product.toLowerCase().includes(keyword) ||
    item.planNo.toLowerCase().includes(keyword)
  );
  gridApi.setGridOptions({ data: filtered });
};

// ==================== 3. 动态报工与防呆验证逻辑 ====================
// 🌟 核心：解析 CSV 提取的工序特征，不同工序报工时弹出的收集字段不同
const processParamConfig: Record<string, any[]> = {
  '配料': [
    { key: 'temp', label: '车间温度', type: 'number', unit: '℃', required: true, desc: '环境点检' },
    { key: 'humidity', label: '车间湿度', type: 'number', unit: '%RH', required: true, desc: '环境点检' },
    { key: 'confirm', label: '配方核对', type: 'select', options: ['核对无误', '配方异常'], required: true, desc: '投料防错' }
  ],
  '湿法': [
    { key: 'level', label: '料槽液位高度', type: 'number', unit: 'CM', required: true, desc: '参数记录' },
    { key: 'gap', label: '刀口间隙', type: 'number', unit: 'mm', required: true, desc: '参数记录' },
    { key: 'dmf', label: '三区DMF浓度', type: 'number', unit: '%', required: true, desc: '设备采集' }
  ],
  '磨皮(一)': [
    { key: 'thickness', label: '磨后厚度', type: 'number', unit: 'mm', required: true, desc: '参数记录' },
    { key: 'speed', label: '磨皮线速', type: 'number', unit: 'm/min', required: true, desc: '参数记录' }
  ],
  '分切': [
    { key: 'length', label: '分切米数', type: 'number', unit: 'm', required: true, desc: '参数记录' },
    { key: 'barcode', label: '生成流水码段', type: 'text', unit: '', required: false, desc: '单品赋码' }
  ],
  '压槽': [
    { key: 'gap', label: '压辊间隙', type: 'number', unit: 'mm', required: true, desc: '参数记录' },
    { key: 'temp', label: '压辊温度', type: 'number', unit: '℃', required: true, desc: '参数记录' },
    { key: 'laser', label: '激光定位值', type: 'number', unit: 'mm', required: true, desc: '参数记录' }
  ]
};

const isBookingModalVisible = ref(false);
const currentWO = ref<any>(null);
const bookingForm = ref<any>({
  goodQty: 0,
  scrapQty: 0,
  params: {}
});

// 打开报工弹窗
const openBooking = (row: any) => {
  currentWO.value = row;
  bookingForm.value = {
    goodQty: row.planQty - row.goodQty, // 默认带出剩余数量
    scrapQty: 0,
    params: {} // 动态参数池
  };
  isBookingModalVisible.value = true;
};

// 提交报工数据
const submitBooking = () => {
  if (bookingForm.value.goodQty + bookingForm.value.scrapQty <= 0) {
    return message.warning('报工总数量必须大于 0');
  }

  // 模拟提交更新
  const index = mockWorkOrders.value.findIndex(w => w.id === currentWO.value.id);
  if (index > -1) {
    mockWorkOrders.value[index].goodQty += bookingForm.value.goodQty;
    mockWorkOrders.value[index].scrapQty += bookingForm.value.scrapQty;
    mockWorkOrders.value[index].status = mockWorkOrders.value[index].goodQty >= mockWorkOrders.value[index].planQty ? 'COMPLETED' : 'IN_PROGRESS';
    if (!mockWorkOrders.value[index].startTime) {
      mockWorkOrders.value[index].startTime = dayjs().format('YYYY-MM-DD HH:mm');
    }
  }

  gridApi.setGridOptions({ data: [...mockWorkOrders.value] }); // 触发响应式刷新
  isBookingModalVisible.value = false;
  message.success(`工单 ${currentWO.value.id} 报工登记成功！`);
};

const openStart = (row: any) => {
  const index = mockWorkOrders.value.findIndex(w => w.id === row.id);
  if (index > -1) {
    mockWorkOrders.value[index].status = 'IN_PROGRESS';
    mockWorkOrders.value[index].startTime = dayjs().format('YYYY-MM-DD HH:mm');
    mockWorkOrders.value[index].operator = '当前账号';
    gridApi.setGridOptions({ data: [...mockWorkOrders.value] });
    message.success('工单已开工！');
  }
}
</script>

<template>
  <Page auto-content-height>
    <div class="h-full flex flex-col bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">

      <div class="h-14 shrink-0 bg-slate-50 border-b border-slate-200 flex items-center justify-between px-4">
        <div class="flex items-center gap-3">
          <IconifyIcon icon="lucide:edit-pen" class="text-indigo-600 text-xl" />
          <span class="font-black text-slate-800 text-[15px]">工单现场报工台 (SFC)</span>
        </div>

        <div class="flex items-center gap-3">
          <a-input-search
            v-model:value="searchKeyword"
            placeholder="扫描或输入工单号/产品/计划单"
            allow-clear
            @search="handleSearch"
            class="w-[300px]"
          />
          <a-divider type="vertical" />
          <a-button><IconifyIcon icon="lucide:printer" class="mr-1"/> 补打流转卡</a-button>
          <a-button type="primary" class="bg-indigo-600"><IconifyIcon icon="lucide:scan-line" class="mr-1"/> 扫码极速报工</a-button>
        </div>
      </div>

      <div class="flex-1 min-h-0 relative p-2">
        <Grid class="h-full">

          <template #processSlot="{ row }">
            <span class="font-bold text-slate-700 bg-slate-100 px-2 py-1 rounded border border-slate-200">{{ row.process }}</span>
          </template>

          <template #goodSlot="{ row }">
            <span class="text-emerald-600 font-bold">{{ row.goodQty }}</span>
          </template>

          <template #scrapSlot="{ row }">
            <span :class="row.scrapQty > 0 ? 'text-red-500 font-bold' : 'text-slate-400'">{{ row.scrapQty }}</span>
          </template>

          <template #statusSlot="{ row }">
            <a-tag :color="statusMap[row.status]?.color" class="!m-0">{{ statusMap[row.status]?.text }}</a-tag>
          </template>

          <template #actionSlot="{ row }">
            <div class="flex items-center gap-2">
              <a-button v-if="row.status === 'PENDING'" type="link" size="small" class="p-0 text-emerald-600" @click="openStart(row)">开工</a-button>
              <a-button v-if="row.status === 'IN_PROGRESS'" type="link" size="small" class="p-0 text-indigo-600 font-bold" @click="openBooking(row)">登记报工</a-button>
              <a-button v-if="row.status === 'COMPLETED'" type="link" size="small" class="p-0 text-slate-400">查看履历</a-button>
            </div>
          </template>

        </Grid>
      </div>
    </div>

    <a-modal v-model:open="isBookingModalVisible" title="工序执行与报工登记" @ok="submitBooking" okText="确认过站" cancelText="取消" :width="650" destroyOnClose>
      <div class="pt-4 pb-2" v-if="currentWO">

        <div class="bg-indigo-50 border border-indigo-100 rounded-lg p-3 mb-5 flex justify-between items-center">
          <div>
            <div class="text-xs text-indigo-400 mb-1">执行工单号</div>
            <div class="font-mono font-bold text-indigo-900 text-base">{{ currentWO.id }}</div>
          </div>
          <div class="text-right">
            <div class="text-xs text-indigo-400 mb-1">加工产品 | 当前工序</div>
            <div class="font-bold text-indigo-900">{{ currentWO.product }} <span class="mx-1 text-indigo-300">|</span> <span class="bg-indigo-600 text-white px-1.5 py-0.5 rounded text-xs">{{ currentWO.process }}</span></div>
          </div>
        </div>

        <a-form layout="vertical" class="grid grid-cols-2 gap-x-6">
          <div class="col-span-2 text-[13px] font-bold text-slate-700 border-b border-slate-100 pb-2 mb-3 flex items-center">
            <IconifyIcon icon="lucide:package-check" class="text-emerald-500 mr-1.5 text-lg"/> 本次产出登记
            <span class="ml-auto text-xs text-slate-400 font-normal">目标剩余量: {{ currentWO.planQty - currentWO.goodQty }}</span>
          </div>

          <a-form-item label="合格数量 (良品)">
            <a-input-number v-model:value="bookingForm.goodQty" :min="0" class="w-full text-emerald-600 font-bold" size="large" />
          </a-form-item>
          <a-form-item label="报废数量 (不良)">
            <a-input-number v-model:value="bookingForm.scrapQty" :min="0" class="w-full text-red-500 font-bold" size="large" />
          </a-form-item>

          <template v-if="processParamConfig[currentWO.process]">
            <div class="col-span-2 text-[13px] font-bold text-slate-700 border-b border-slate-100 pb-2 mb-3 mt-2 flex items-center">
              <IconifyIcon icon="lucide:data-line" class="text-blue-500 mr-1.5 text-lg"/> 过程工艺参数 (SOP要求必填)
            </div>

            <a-form-item v-for="param in processParamConfig[currentWO.process]" :key="param.key" :required="param.required">
              <template #label>
                {{ param.label }} <span class="text-[10px] text-slate-400 ml-1">({{ param.desc }})</span>
              </template>

              <a-input-number v-if="param.type === 'number'" v-model:value="bookingForm.params[param.key]" class="w-full" :addon-after="param.unit" placeholder="读取设备或手工录入" />

              <a-select v-else-if="param.type === 'select'" v-model:value="bookingForm.params[param.key]" :options="param.options.map(o => ({label: o, value: o}))" placeholder="点检确认" />

              <a-input v-else v-model:value="bookingForm.params[param.key]" placeholder="文本记录" />
            </a-form-item>
          </template>

          <a-form-item label="异常及备注说明" class="col-span-2 mb-0">
            <a-textarea placeholder="如有报废请填写异常原因..." :rows="2" />
          </a-form-item>
        </a-form>
      </div>
    </a-modal>

  </Page>
</template>

<style scoped>
/* 穿透 VxeTable 的默认单元格间距 */
:deep(.vxe-body--column) {
  padding: 6px 0 !important;
}
</style>
