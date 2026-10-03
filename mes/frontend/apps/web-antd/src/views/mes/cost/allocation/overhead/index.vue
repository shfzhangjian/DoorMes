<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesCostOverheadApi } from '#/api/mes/cost/allocation/overhead';

import { computed, onMounted, ref } from 'vue';
import { confirm, Page, useVbenModal } from '@vben/common-ui';
import { isEmpty } from '@vben/utils';
// 引入 AntD 的 Input(搜索) 和 Tag
import { Input as AInput, Tag as ATag, message } from 'ant-design-vue';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  getCostOverheadPage, deleteCostOverheadList, confirmCostOverhead, getOverheadPeriods
} from '#/api/mes/cost/allocation/overhead';
import { useGridColumns, useGridFormSchema } from './data';
import FormComponent from './modules/form.vue';

defineOptions({ name: 'MesCostOverhead' });

const [FormModal, formModalApi] = useVbenModal({ connectedComponent: FormComponent, destroyOnClose: true });
const checkedIds = ref<number[]>([]);

// ================= 左侧卡片列表与搜索逻辑 =================
const periodList = ref<any[]>([]);
const searchKeyword = ref('');
const selectedPeriod = ref<string>('2026-02'); // 默认选中当月

// 动态过滤期间列表
const filteredPeriods = computed(() => {
  if (!searchKeyword.value) return periodList.value;
  const kw = searchKeyword.value.toLowerCase();
  return periodList.value.filter(item =>
    item.period.includes(kw) || item.label.includes(kw)
  );
});

async function loadPeriods() {
  periodList.value = await getOverheadPeriods();
}

function handlePeriodSelect(period: string) {
  if (selectedPeriod.value !== period) {
    selectedPeriod.value = period;
    handleRefresh(); // 切换月份时刷新右侧表格
  }
}
// =========================================================

function handleRefresh() { gridApi.query(); }

function handleCreate() {
  formModalApi.setData({ period: selectedPeriod.value }).open();
}

function handleEdit(row: MesCostOverheadApi.Overhead) {
  formModalApi.setData({ id: row.id }).open();
}

async function handleDeleteBatch() {
  await confirm('确认要删除选中的草稿制费单吗？');
  const hideLoading = message.loading({ content: '删除中...', duration: 0 });
  try {
    await deleteCostOverheadList(checkedIds.value);
    checkedIds.value = [];
    message.success('删除成功');
    handleRefresh();
  } finally { hideLoading(); }
}

async function handleConfirm(row: MesCostOverheadApi.Overhead) {
  await confirm('确认过账该笔费用吗？过账后将不可修改！');
  const hideLoading = message.loading({ content: '过账处理中...', duration: 0 });
  try {
    await confirmCostOverhead(row.id);
    message.success('单据过账成功');
    handleRefresh();
  } finally { hideLoading(); }
}

function handleRowCheckboxChange({ records }: { records: MesCostOverheadApi.Overhead[] }) {
  checkedIds.value = records.map((item) => item.id);
}

const [BaseGrid, gridApi] = useVbenVxeGrid({
  formOptions: { schema: useGridFormSchema() },
  gridOptions: {
    columns: useGridColumns(),
    height: 'auto',
    keepSource: true,
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) => {
          return await getCostOverheadPage({
            pageNo: page.currentPage,
            pageSize: page.pageSize,
            period: selectedPeriod.value, // 强注入选中期间
            ...formValues
          });
        }
      },
    },
    checkboxConfig: { checkMethod: ({ row }) => row.status === 0 },
    rowConfig: { keyField: 'id', isHover: true },
    toolbarConfig: { refresh: true, search: true },
  } as VxeTableGridOptions<MesCostOverheadApi.Overhead>,
  gridEvents: { checkboxAll: handleRowCheckboxChange, checkboxChange: handleRowCheckboxChange },
});

onMounted(() => {
  loadPeriods();
});
</script>

<template>
  <Page auto-content-height>
    <FormModal @success="handleRefresh" />

    <div class="flex h-full gap-4">

      <div class="w-72 bg-background p-4 rounded-md shadow-sm border border-border flex flex-col">
        <div class="mb-3 text-base font-semibold border-b border-border pb-2 flex items-center">
          <span class="i-ep:calendar mr-2"></span> 核算期间
        </div>

        <div class="mb-3">
          <a-input
            v-model:value="searchKeyword"
            placeholder="搜索期间 (如 2026或12月)"
            allow-clear
          >
            <template #prefix>
              <span class="i-ep:search text-gray-400"></span>
            </template>
          </a-input>
        </div>

        <div class="flex-1 overflow-y-auto space-y-2 pr-1 custom-scrollbar">
          <div
            v-for="item in filteredPeriods"
            :key="item.period"
            @click="handlePeriodSelect(item.period)"
            :class="[
              'p-3 rounded-md border cursor-pointer transition-all duration-200 relative overflow-hidden select-none',
              selectedPeriod === item.period
                ? 'border-primary bg-primary/10 shadow-sm'
                : 'border-border bg-layout hover:border-primary/50 hover:bg-layout-hover'
            ]"
          >
            <div class="flex justify-between items-center mb-1">
              <span class="font-medium text-[15px]" :class="selectedPeriod === item.period ? 'text-primary' : ''">
                {{ item.label }}
              </span>
              <a-tag :color="item.isClosed ? 'default' : 'blue'" class="mr-0 border-0">
                {{ item.tag }}
              </a-tag>
            </div>
            <div class="text-xs text-gray-500 flex items-center mt-1">
              <span class="i-ep:price-tag mr-1"></span> 期间代码: {{ item.period }}
            </div>

            <div
              v-if="selectedPeriod === item.period"
              class="absolute left-0 top-0 bottom-0 w-1 bg-primary"
            ></div>
          </div>

          <div v-if="filteredPeriods.length === 0" class="text-center text-gray-400 mt-10 text-sm">
            <span class="i-ep:document-delete text-3xl mb-2 opacity-50 block mx-auto"></span>
            未找到匹配的核算期间
          </div>
        </div>
      </div>

      <div class="flex-1 overflow-hidden">
        <BaseGrid :table-title="`【${selectedPeriod}】期 制费归集录入`">
          <template #toolbar-tools>
            <TableAction
              :actions="[
                { label: '手工录入', type: 'primary', icon: ACTION_ICON.ADD, auth: ['mes:cost-overhead:create'], onClick: handleCreate },
                { label: 'ERP同步', type: 'default', icon: ACTION_ICON.DOWNLOAD },
                { label: '批量删除', type: 'primary', danger: true, icon: ACTION_ICON.DELETE, disabled: isEmpty(checkedIds), onClick: handleDeleteBatch },
              ]"
            />
          </template>

          <template #actions="{ row }">
            <TableAction
              :actions="[
                { label: '过账', type: 'link', icon: 'ep:finished', ifShow: row.status === 0, onClick: handleConfirm.bind(null, row) },
                { label: '编辑', type: 'link', icon: ACTION_ICON.EDIT, ifShow: row.status === 0, onClick: handleEdit.bind(null, row) },
                { label: '删除', type: 'link', danger: true, icon: ACTION_ICON.DELETE, ifShow: row.status === 0, popConfirm: { title: '确认删除?', confirm: () => { checkedIds.value=[row.id]; handleDeleteBatch(); } } },
              ]"
            />
          </template>
        </BaseGrid>
      </div>

    </div>
  </Page>
</template>

<style scoped>
/* 优化滚动条样式，使其在卡片列表中更优雅 */
.custom-scrollbar::-webkit-scrollbar {
  width: 4px;
}
.custom-scrollbar::-webkit-scrollbar-track {
  background: transparent;
}
.custom-scrollbar::-webkit-scrollbar-thumb {
  background: #e2e8f0;
  border-radius: 4px;
}
.custom-scrollbar:hover::-webkit-scrollbar-thumb {
  background: #cbd5e1;
}
</style>
