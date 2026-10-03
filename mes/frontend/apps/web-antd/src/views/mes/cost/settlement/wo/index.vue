<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesCostSettlementApi } from '#/api/mes/cost/settlement/wo';

import { computed, onMounted, ref } from 'vue';
import { Page, confirm } from '@vben/common-ui';
import { downloadFileFromBlobPart } from '@vben/utils';
// 引入 InputNumber 供行内编辑，Button 用于展开折叠
import { Input as AInput, Tag as ATag, message, Alert as AAlert, InputNumber as AInputNumber, Button as AButton } from 'ant-design-vue';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  getSettlementPage, getSettlementPeriods, executePeriodSettlement, exportSettlement, updateSettlementCost
} from '#/api/mes/cost/settlement/wo';
import { useGridColumns, useGridFormSchema } from './data';

defineOptions({ name: 'MesCostSettlement' });

// 左侧期间逻辑
const periodList = ref<any[]>([]);
const searchKeyword = ref('');
const selectedPeriod = ref<string>('2026-02');

// 说明面板折叠控制
const isDescExpanded = ref(true);

const filteredPeriods = computed(() => {
  if (!searchKeyword.value) return periodList.value;
  const kw = searchKeyword.value.toLowerCase();
  return periodList.value.filter(item => item.period.includes(kw) || item.label.includes(kw));
});

async function loadPeriods() {
  periodList.value = await getSettlementPeriods();
}

function handlePeriodSelect(period: string) {
  if (selectedPeriod.value !== period) {
    selectedPeriod.value = period;
    handleRefresh();
  }
}

function handleRefresh() { gridApi.query(); }

// 核心手工调整逻辑 (触发后端更新和本地重算)
async function handleCostChange(row: MesCostSettlementApi.Settlement, field: 'materialCost' | 'overheadCost') {
  try {
    await updateSettlementCost(row.id, field, row[field]);
    // 前端无感静默更新对应行的结果，不刷新整个表格以保持体验
    row.totalCost = row.materialCost + row.overheadCost;
    row.unitCost = row.totalCost / (row.completedQty || 1);
  } catch (e) {
    message.error('成本更新失败');
  }
}

async function handleExecuteSettlement() {
  await confirm(`警告：确认要对【${selectedPeriod.value}】期间的工单执行成本结转吗？结转后数据将被锁定！`);
  const hideLoading = message.loading({ content: '底层计算引擎运转中...', duration: 0 });
  try {
    const res = await executePeriodSettlement(selectedPeriod.value);
    message.success(res.msg);
    handleRefresh();
    loadPeriods();
  } finally { hideLoading(); }
}

async function handleExport() {
  const data = await exportSettlement({});
  downloadFileFromBlobPart({ fileName: `成本结转表_${selectedPeriod.value}.xls`, source: data });
}

// VxeGrid 实例化 (加入底部汇总逻辑)
const [BaseGrid, gridApi] = useVbenVxeGrid({
  formOptions: { schema: useGridFormSchema() },
  gridOptions: {
    columns: useGridColumns(),
    height: 'auto', // 配合外层 flex-1 min-h-0 实现自适应和固定底部分页
    keepSource: true,
    pagerConfig: { enabled: true },
    // 开启表尾汇总行
    showFooter: true,
    footerMethod: ({ columns, data }) => {
      return [
        columns.map((column, columnIndex) => {
          if (columnIndex === 0) return '当页合计';
          // 需要汇总的列
          if (['completedQty', 'materialCost', 'overheadCost', 'totalCost'].includes(column.field)) {
            const sum = data.reduce((prev, curr) => prev + (Number(curr[column.field]) || 0), 0);
            return column.field === 'completedQty' ? sum : `¥ ${sum.toFixed(2)}`;
          }
          return '-';
        })
      ];
    },
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) => {
          return await getSettlementPage({
            pageNo: page.currentPage, pageSize: page.pageSize,
            period: selectedPeriod.value, ...formValues
          });
        }
      },
    },
    rowConfig: { keyField: 'id', isHover: true },
    toolbarConfig: { refresh: true, search: true },
  } as VxeTableGridOptions<MesCostSettlementApi.Settlement>,
});

onMounted(() => { loadPeriods(); });
</script>

<template>
  <Page auto-content-height>
    <div class="flex h-full gap-4">

      <div class="w-72 bg-background p-4 rounded-md shadow-sm border border-border flex flex-col h-full">
        <div class="mb-3 text-base font-semibold border-b border-border pb-2 flex items-center">
          <span class="i-ep:wallet mr-2"></span> 结算期间
        </div>
        <div class="mb-3">
          <a-input v-model:value="searchKeyword" placeholder="搜索期间" allow-clear>
            <template #prefix><span class="i-ep:search text-gray-400"></span></template>
          </a-input>
        </div>
        <div class="flex-1 overflow-y-auto space-y-2 pr-1 custom-scrollbar">
          <div
            v-for="item in filteredPeriods" :key="item.period" @click="handlePeriodSelect(item.period)"
            :class="[
              'p-3 rounded-md border cursor-pointer transition-all duration-200 relative overflow-hidden select-none',
              selectedPeriod === item.period ? 'border-primary bg-primary/10 shadow-sm' : 'border-border bg-layout hover:border-primary/50'
            ]"
          >
            <div class="flex justify-between items-center mb-1">
              <span class="font-medium text-[15px]" :class="selectedPeriod === item.period ? 'text-primary' : ''">{{ item.label }}</span>
              <a-tag :color="item.isClosed ? 'default' : 'blue'" class="mr-0 border-0">{{ item.isClosed ? '已封账' : '处理中' }}</a-tag>
            </div>
            <div class="text-xs text-gray-500 flex items-center mt-1"><span class="i-ep:price-tag mr-1"></span> 期间: {{ item.period }}</div>
            <div v-if="selectedPeriod === item.period" class="absolute left-0 top-0 bottom-0 w-1 bg-primary"></div>
          </div>
        </div>
      </div>

      <div class="flex-1 flex flex-col min-w-0 h-full">

        <div class="mb-3 border border-green-200 rounded-md bg-green-50/30 overflow-hidden transition-all duration-300 shadow-sm">
          <div
            class="px-4 py-3 flex justify-between items-center cursor-pointer hover:bg-green-50/60"
            @click="isDescExpanded = !isDescExpanded"
          >
            <span class="font-bold text-[15px] text-green-800 flex items-center">
              <span class="i-ep:info-filled mr-2 text-lg"></span> 期末工单成本结转运算引擎
            </span>
            <a-button type="link" class="p-0 h-auto text-green-700">
              {{ isDescExpanded ? '收起说明' : '展开说明' }}
              <span :class="isDescExpanded ? 'i-ep:arrow-up' : 'i-ep:arrow-down'" class="ml-1"></span>
            </a-button>
          </div>

          <div v-show="isDescExpanded" class="px-4 pb-4 text-sm leading-relaxed text-gray-700 border-t border-green-100/50">
            <p class="mb-2 mt-2">
              系统将汇总领退料流水形成【直接材料成本】，依据《分摊规则》计算【分摊制费】。<br/>
              <span class="text-orange-600">※ 提示：在状态为"待运算"时，允许财务人员根据实际账务微调(人工调整)各项成本，结转后数据将被锁定。</span>
            </p>
            <div class="bg-green-100/50 p-3 rounded border border-green-200 text-green-900 mt-2 flex gap-4 font-mono text-[13px] items-center">
              <span class="font-bold whitespace-nowrap"><span class="i-ep:money mr-1"></span>计算模型：</span>
              <span class="bg-white px-2 py-1 rounded shadow-sm">A材料费(可调)</span>
              <span class="text-gray-400">+</span>
              <span class="bg-white px-2 py-1 rounded shadow-sm">B制造费(可调)</span>
              <span class="text-gray-400">=</span>
              <span class="bg-green-200 px-2 py-1 rounded shadow-sm text-green-800 font-bold">C总成本</span>
              <span class="text-gray-400">➡️</span>
              <span class="bg-green-600 px-2 py-1 rounded shadow-sm text-white font-bold">D单价(C÷数量)</span>
            </div>
          </div>
        </div>

        <div class="flex-1 overflow-hidden bg-background rounded-md shadow-sm border border-border">
          <BaseGrid :table-title="`【${selectedPeriod}】成本结转台账`">
            <template #toolbar-tools>
              <TableAction
                :actions="[
                  { label: '执行期末结转', type: 'primary', icon: 'ep:money', auth: ['mes:cost-settlement:exec'], onClick: handleExecuteSettlement },
                  { label: '导出台账', type: 'default', icon: ACTION_ICON.DOWNLOAD, onClick: handleExport },
                ]"
              />
            </template>

            <template #materialCost="{ row }">
              <div v-if="row.status === 0" class="flex items-center justify-end w-full gap-1">
                <span class="text-gray-400 text-xs">¥</span>
                <a-input-number v-model:value="row.materialCost" :min="0" :precision="2" size="small" class="w-24 text-right" @change="handleCostChange(row, 'materialCost')" />
              </div>
              <span v-else class="text-gray-700">¥ {{ Number(row.materialCost).toFixed(2) }}</span>
            </template>

            <template #overheadCost="{ row }">
              <div v-if="row.status === 0" class="flex items-center justify-end w-full gap-1">
                <span class="text-gray-400 text-xs">¥</span>
                <a-input-number v-model:value="row.overheadCost" :min="0" :precision="2" size="small" class="w-24 text-right" @change="handleCostChange(row, 'overheadCost')" />
              </div>
              <span v-else class="text-orange-600">¥ {{ Number(row.overheadCost).toFixed(2) }}</span>
            </template>

            <template #totalCost="{ row }">
              <span class="text-blue-600 font-bold text-[14px]">¥ {{ Number(row.totalCost).toFixed(2) }}</span>
            </template>

            <template #unitCost="{ row }">
              <span class="text-green-600 font-bold text-[14px]">¥ {{ Number(row.unitCost).toFixed(2) }}</span>
            </template>

            <template #status="{ row }">
              <a-tag :color="row.status === 1 ? 'success' : 'processing'" class="m-0 border-0">
                <template #icon v-if="row.status === 0"><span class="i-ep:loading animate-spin mr-1"></span></template>
                {{ row.status === 1 ? '已结转' : '待运算' }}
              </a-tag>
            </template>

          </BaseGrid>
        </div>

      </div>
    </div>
  </Page>
</template>

<style scoped>
.custom-scrollbar::-webkit-scrollbar { width: 4px; }
.custom-scrollbar::-webkit-scrollbar-track { background: transparent; }
.custom-scrollbar::-webkit-scrollbar-thumb { background: #e2e8f0; border-radius: 4px; }
.custom-scrollbar:hover::-webkit-scrollbar-thumb { background: #cbd5e1; }

/* 覆盖 Ant Design InputNumber 文字右对齐 */
:deep(.ant-input-number-input) {
  text-align: right;
}
</style>
