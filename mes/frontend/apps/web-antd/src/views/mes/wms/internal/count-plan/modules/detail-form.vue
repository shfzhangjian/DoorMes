<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import { ref, computed } from 'vue';
import { useVbenModal, confirm } from '@vben/common-ui';
import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { Descriptions, Tag, TreeSelect, Select, message, Button, Popconfirm } from 'ant-design-vue';
import { IconifyIcon } from '@vben/icons';
import { useDetailColumns } from '../data';

const emit = defineEmits(['success']);
const currentPlan = ref<any>({});
const isDraft = computed(() => currentPlan.value?.status === 'DRAFT');

// 💡 彻底解决报错：纯 Vue 响应式数据驱动
const lineData = ref<any[]>([]);

const locTree = [{ title: 'A区-01货架-01储位', value: 'BIN-A01-01' }, { title: 'A区-01货架-02储位', value: 'BIN-A01-02' }];
const prodList = [{ label: '光学级PET树脂', value: 'RM-RESIN-01', sysQty: 1500 }, { label: '特种交联剂', value: 'RM-SOLV-05', sysQty: 200 }];

// 🌟 内部 Grid 配置：完全符合 Vben5 规范
const [Grid, gridApi] = useVbenVxeGrid({
  gridOptions: {
    columns: useDetailColumns(true),
    height: 'auto', // 撑满弹窗剩余高度，实现底部分页
    pagerConfig: { enabled: true, pageSize: 15 },
    rowConfig: { keyField: 'id', isHover: true },
    proxyConfig: {
      ajax: {
        query: async ({ page }) => {
          const start = (page.currentPage - 1) * page.pageSize;
          const end = start + page.pageSize;
          // 💡 必须返回 list，切片实现前端分页
          return {
            list: lineData.value.slice(start, end),
            total: lineData.value.length
          };
        }
      }
    },
  } as VxeTableGridOptions<any>,
  gridEvents: {
    // 💡 监听双击行事件：直接修改源数据后重载，绝不报错
    cellDblclick: ({ row }) => {
      if (!isDraft.value) return;
      const target = lineData.value.find(item => item.id === row.id);
      if (target && !target.editable) {
        target.editable = true;
        gridApi.query();
      }
    }
  }
});

const [Modal, modalApi] = useVbenModal({
  fullscreenButton: true,
  title: '📦 盘点单据主从详情',
  onOpenChange(isOpen) {
    if (isOpen) {
      currentPlan.value = modalApi.getData();
      gridApi.setGridOptions({ columns: useDetailColumns(isDraft.value) });

      // 造数据测试分页与滚动
      const mockArr = [{ id: '1', location: 'BIN-A01-01', productCode: 'RM-RESIN-01', productName: '光学级PET树脂', sysQty: 1500, actualQty: null, variance: null, status: 'PENDING', editable: false }];
      if (isDraft.value) {
        for (let i = 2; i <= 25; i++) {
          mockArr.push({ id: String(i), location: 'BIN-A01-02', productCode: 'RM-SOLV-05', productName: `测试物料-${i}`, sysQty: 200, actualQty: null, variance: null, status: 'PENDING', editable: false });
        }
      }
      lineData.value = mockArr;
      gridApi.query(); // 初始化加载
    }
  },
});

// 💡 追加行逻辑：直接插入数组最前面，并激活编辑状态
function handleAddLine() {
  lineData.value.unshift({ id: `new_${Date.now()}`, location: null, productCode: null, productName: '-', sysQty: 0, actualQty: null, variance: null, status: 'PENDING', editable: true });
  gridApi.query();
  message.success('已在顶部追加明细行！');
}

function handleRemoveLine(row: any) {
  lineData.value = lineData.value.filter(item => item.id !== row.id);
  gridApi.query();
}

// 编辑保存：同步回本地数组并刷新表格
function handleSaveLine(row: any) {
  if (!row.location || !row.productCode) return message.warning('请选择完整的库位和物料！');
  const target = lineData.value.find(item => item.id === row.id);
  if (target) {
    target.location = row.location;
    target.productCode = row.productCode;
    target.productName = row.productName;
    target.sysQty = row.sysQty;
    target.editable = false;
    gridApi.query();
    message.success('保存成功');
  }
}

// 激活单行编辑 (兼容点击按钮)
function editRow(row: any) {
  const target = lineData.value.find(item => item.id === row.id);
  if (target) { target.editable = true; gridApi.query(); }
}

function onProductChange(val: string, opt: any, row: any) {
  row.productName = opt.label; row.sysQty = opt.sysQty;
}

function handleApproveVariance() {
  confirm({
    title: '确认审核盘点差异？', content: '审核通过后，系统将自动调整库存底账以平抑盈亏。',
    onOk: () => { currentPlan.value.status = 'COMPLETED'; message.success('差异已审核！'); emit('success'); modalApi.close(); }
  });
}
</script>

<template>
  <Modal class="w-[1200px]">
    <div class="flex flex-col h-[75vh] min-h-[500px] w-full bg-[#f4f6f8] p-4">

      <Descriptions size="small" :column="4" class="bg-white p-4 rounded-lg border border-slate-200 shadow-sm shrink-0 mb-4">
        <Descriptions.Item label="单据号"><span class="font-mono font-bold text-indigo-600">{{ currentPlan.planNo }}</span></Descriptions.Item>
        <Descriptions.Item label="状态"><Tag :color="isDraft ? 'default' : 'processing'">{{ currentPlan.status }}</Tag></Descriptions.Item>
        <Descriptions.Item label="盘点类型">{{ currentPlan.type }}</Descriptions.Item>
        <Descriptions.Item label="范围策略">{{ currentPlan.scopeDesc }}</Descriptions.Item>
      </Descriptions>

      <div class="flex-1 min-h-0 bg-white border border-slate-200 rounded-lg overflow-hidden shadow-sm flex flex-col">
        <Grid table-title="明细子表 (Lines)">

          <template #toolbar-tools>
            <div v-if="isDraft" class="flex gap-4 items-center">
              <span class="text-xs text-slate-500"><IconifyIcon icon="lucide:info" class="mr-1 text-indigo-500"/>双击表格行即可编辑</span>
              <Button type="primary" class="bg-indigo-600" @click="handleAddLine"><IconifyIcon icon="lucide:plus" class="mr-1"/>追加遗漏行</Button>
            </div>
          </template>

          <template #location="{ row }">
            <TreeSelect v-if="row.editable" v-model:value="row.location" :tree-data="locTree" placeholder="选择目标库位" size="small" class="w-full shadow-sm" tree-default-expand-all />
            <span v-else class="text-orange-600 font-mono font-bold text-xs">{{ row.location || '待指定' }}</span>
          </template>

          <template #productCode="{ row }">
            <Select v-if="row.editable" v-model:value="row.productCode" :options="prodList" placeholder="搜索物料" size="small" class="w-full shadow-sm" @change="(val, opt) => onProductChange(val, opt, row)" />
            <span v-else class="text-slate-700 font-mono text-xs">{{ row.productCode || '待指定' }}</span>
          </template>

          <template #actualQty="{ row }"><span class="font-mono font-bold">{{ row.actualQty ?? '-' }}</span></template>
          <template #variance="{ row }">
            <span v-if="row.variance === null">-</span>
            <span v-else :class="row.variance === 0 ? 'text-green-600 font-bold' : 'text-red-600 font-bold'">{{ row.variance > 0 ? '+'+row.variance : row.variance }}</span>
          </template>
          <template #status="{ row }">
            <Tag :color="row.status === 'PENDING' ? 'default' : 'green'" class="!m-0">{{ row.status === 'PENDING' ? '待盘点' : '相符' }}</Tag>
          </template>

          <template #action="{ row }">
            <div class="flex justify-center gap-1">
              <Button v-if="row.editable" type="primary" size="small" class="bg-indigo-600 border-none h-6 px-3" @click.stop="handleSaveLine(row)">保存</Button>
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

      <div class="bg-white mt-4 p-4 border border-slate-200 flex justify-between items-center shrink-0 rounded-lg shadow-sm">
        <span class="text-xs text-slate-500">
           <IconifyIcon icon="lucide:info" class="text-indigo-500 mr-1"/>
           {{ currentPlan?.status === 'DRAFT' ? '确认无误后，请在外部列表页点击【下发任务】以冻结账务并指导前线作业。' : '盘点结束后须由主管复核差异并执行强制平账。' }}
        </span>
        <div class="flex gap-3">
          <Button @click="modalApi.close()" size="large">关闭窗口</Button>
          <Button v-if="currentPlan?.status === 'COUNTING'" type="primary" class="bg-rose-600 border-none font-bold" size="large" @click="handleApproveVariance">审核差异并强制平账</Button>
        </div>
      </div>
    </div>
  </Modal>
</template>
