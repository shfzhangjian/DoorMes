<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import { ref, computed, nextTick } from 'vue';
import { useVbenModal } from '@vben/common-ui';
import { useVbenVxeGrid, TableAction } from '#/adapter/vxe-table';
import { Descriptions, Tag, Select, InputNumber, message, Button, Popconfirm } from 'ant-design-vue';
import { IconifyIcon } from '@vben/icons';
import { useItemColumns } from '../data';

const emit = defineEmits(['success']);
const currentOrder = ref<any>({});
const isDraft = computed(() => !currentOrder.value.status || currentOrder.value.status === 'DRAFT');

const itemList = ref<any[]>([]);

const prodList = [
  { label: '高透光学复合膜', value: 'FG-OPT-099', unit: '卷' },
  { label: '特种交联剂', value: 'RM-SOLV-05', unit: '桶' }
];

const [Grid, gridApi] = useVbenVxeGrid({
  gridOptions: {
    columns: useItemColumns(true),
    height: 'auto',
    pagerConfig: { enabled: true, pageSize: 15 },
    rowConfig: { keyField: 'id', isHover: true },
    proxyConfig: {
      ajax: {
        query: async ({ page }) => {
          const start = (page.currentPage - 1) * page.pageSize;
          const end = start + page.pageSize;
          return {
            list: itemList.value.slice(start, end),
            total: itemList.value.length
          };
        }
      }
    },
  } as VxeTableGridOptions<any>,
  gridEvents: {
    cellDblclick: ({ row }) => {
      if (!isDraft.value) return;
      const target = itemList.value.find(item => item.id === row.id);
      if (target && !target.editable) {
        target.editable = true;
        gridApi.query();
      }
    }
  }
});

const [Modal, modalApi] = useVbenModal({
  fullscreenButton: true,
  title: computed(() => currentOrder.value.isNew ? '✨ 新建销售发货单' : '📦 销售发货单详情'),

  // 💡 核心修复 1：将保存逻辑移交回 Vben 原生的确认事件中
  async onConfirm() {
    if (itemList.value.length === 0) {
      message.warning('请至少添加一行发货明细！');
      return;
    }
    // 检查是否有未保存的行
    if (itemList.value.some(item => item.editable)) {
      message.warning('存在未确认的编辑行，请先点击表格中的[确认]！');
      return;
    }

    message.success('发货单草稿保存成功！');
    emit('success');
    modalApi.close();
  },

  onOpenChange(isOpen) {
    if (isOpen) {
      const data = modalApi.getData();
      currentOrder.value = data.isNew ? { deliveryNo: 'SD-NEW-DRAFT', customerName: '-', status: 'DRAFT', deliveryDate: '-' } : { ...data };

      gridApi.setGridOptions({ columns: useItemColumns(isDraft.value) });

      // 💡 核心修复 2：动态控制 Vben 原生底部按钮的显示与文案
      modalApi.setState({
        showConfirmButton: isDraft.value, // 仅草稿状态显示确定按钮
        confirmText: '保存单据草稿',      // 自定义确认按钮文案
        cancelText: '关闭',              // 自定义取消按钮文案
      });

      if (data.isNew) {
        itemList.value = [];
      } else {
        const mockArr = [{ id: '1', productCode: 'FG-OPT-099', productName: '高透光学复合膜', unit: '卷', planQty: data.totalPlanQty || 100, actualQty: data.totalActualQty || 0, status: 'PENDING', editable: false }];
        if (isDraft.value) {
          for (let i = 2; i <= 25; i++) {
            mockArr.push({ id: String(i), productCode: 'RM-SOLV-05', productName: `配套辅料-${i}`, unit: '桶', planQty: 50, actualQty: 0, status: 'PENDING', editable: false });
          }
        }
        itemList.value = mockArr;
      }
      gridApi.query();
    }
  },
});

function handleAddLine() {
  itemList.value.unshift({ id: `new_${Date.now()}`, productCode: null, productName: '-', unit: '-', planQty: 1, actualQty: 0, status: 'PENDING', editable: true });
  gridApi.query();
}

function handleRemoveLine(row: any) {
  itemList.value = itemList.value.filter(item => item.id !== row.id);
  gridApi.query();
}

function handleSaveLine(row: any) {
  if (!row.productCode || !row.planQty) return message.warning('请完善物料和发货数量！');
  const target = itemList.value.find(item => item.id === row.id);
  if (target) {
    target.productCode = row.productCode;
    target.productName = row.productName;
    target.unit = row.unit;
    target.planQty = row.planQty;
    target.editable = false;
    gridApi.query();
  }
}

function editRow(row: any) {
  const target = itemList.value.find(item => item.id === row.id);
  if (target) { target.editable = true; gridApi.query(); }
}

function onProductChange(val: string, opt: any, row: any) {
  row.productName = opt.label;
  row.unit = opt.unit;
}
</script>

<template>
  <Modal class="w-[1200px]">
    <div class="flex flex-col h-[75vh] min-h-[500px] w-full bg-[#f4f6f8] p-4">

      <Descriptions size="small" :column="4" class="bg-white p-4 rounded-lg border border-slate-200 shadow-sm shrink-0 mb-4">
        <Descriptions.Item label="发货单号"><span class="font-mono font-bold text-indigo-700 bg-indigo-50 px-2 py-0.5 rounded">{{ currentOrder.deliveryNo }}</span></Descriptions.Item>
        <Descriptions.Item label="状态">
          <Tag :color="isDraft ? 'default' : (currentOrder.status === 'PICKING' ? 'processing' : 'success')" class="!m-0 font-bold border-transparent">
            {{ isDraft ? '草稿' : (currentOrder.status === 'PICKING' ? '作业中' : '已出库') }}
          </Tag>
        </Descriptions.Item>
        <Descriptions.Item label="收货客户"><span class="font-bold text-slate-800">{{ currentOrder.customerName }}</span></Descriptions.Item>
        <Descriptions.Item label="要求日期">{{ currentOrder.deliveryDate }}</Descriptions.Item>
      </Descriptions>

      <div class="flex-1 min-h-0 bg-white border border-slate-200 rounded-lg overflow-hidden shadow-sm flex flex-col">
        <Grid table-title="发货明细子表 (Delivery Lines)">
          <template #toolbar-tools>
            <div v-if="isDraft" class="flex gap-4 items-center">
              <span class="text-xs text-slate-500"><IconifyIcon icon="lucide:info" class="mr-1 text-indigo-500"/>双击表格行即可编辑</span>
              <Button type="primary" class="bg-indigo-600 font-bold" @click="handleAddLine"><IconifyIcon icon="lucide:plus" class="mr-1"/>追加物料</Button>
            </div>
          </template>

          <template #productCode="{ row }">
            <Select v-if="row.editable" v-model:value="row.productCode" :options="prodList" placeholder="搜索物料" size="small" class="w-full shadow-sm" @change="(val, opt) => onProductChange(val, opt, row)" />
            <span v-else class="text-indigo-700 font-mono font-bold text-xs">{{ row.productCode || '待指定' }}</span>
          </template>

          <template #planQty="{ row }">
            <InputNumber v-if="row.editable" v-model:value="row.planQty" :min="1" size="small" class="w-full shadow-sm" />
            <span v-else class="font-mono font-bold text-slate-600">{{ row.planQty }}</span>
          </template>

          <template #actualQty="{ row }">
            <span :class="row.actualQty >= row.planQty ? 'text-green-600' : 'text-orange-500'" class="font-mono font-bold">{{ row.actualQty }}</span>
          </template>

          <template #status="{ row }">
            <Tag :color="row.status === 'PENDING' ? 'default' : 'green'" class="!m-0">{{ row.status === 'PENDING' ? '待拣货' : '已齐套' }}</Tag>
          </template>

          <template #action="{ row }">
            <div class="flex justify-center gap-1">
              <Button v-if="row.editable" type="primary" size="small" class="bg-indigo-600 border-none h-6 px-3" @click.stop="handleSaveLine(row)">确认</Button>
              <template v-else>
                <Button type="link" size="small" class="px-1" @click.stop="editRow(row)">编辑</Button>
                <Popconfirm title="确定剔除此行？" @confirm="handleRemoveLine(row)">
                  <Button type="text" danger size="small" class="px-1" @click.stop>剔除</Button>
                </Popconfirm>
              </template>
            </div>
          </template>
        </Grid>
      </div>

      <div class="bg-indigo-50 mt-4 p-3 border border-indigo-100 flex items-center shrink-0 rounded shadow-sm text-indigo-700 text-xs">
        <IconifyIcon icon="lucide:info" class="mr-2 text-lg shrink-0"/>
        <span>{{ isDraft ? '请在上方完善发货明细行。确认无误后，请点击下方【保存单据草稿】。' : '当前单据已下发执行，仓库作业人员正在现场按此明细扫码拣货，当前仅提供只读浏览。' }}</span>
      </div>

    </div>
  </Modal>
</template>

<style scoped>
:deep(.vben-vxe-grid) {
  height: 100%;
  display: flex;
  flex-direction: column;
}
:deep(.ant-table-wrapper),
:deep(.ant-spin-nested-loading),
:deep(.ant-spin-container) {
  height: 100%;
  display: flex;
  flex-direction: column;
}
:deep(.ant-table) {
  flex: 1;
  overflow: hidden;
}
:deep(.ant-table-thead > tr > th) { background: #f8fafc !important; font-size: 13px; font-weight: bold; border-bottom: 1px solid #e2e8f0; }
:deep(.ant-table-cell) { font-size: 13px; padding: 8px 12px !important; border-bottom: 1px solid #f1f5f9; }
:deep(.ant-pagination) {
  margin: 0 !important;
  padding: 12px 16px;
  border-top: 1px solid #e2e8f0;
  background: #fff;
  flex-shrink: 0;
}
</style>
