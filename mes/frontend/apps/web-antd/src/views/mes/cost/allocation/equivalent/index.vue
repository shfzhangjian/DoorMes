<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesCostEquivalentApi } from '#/api/mes/cost/allocation/equivalent';

import { computed, onMounted, ref } from 'vue';
import { Page, confirm } from '@vben/common-ui';
import { downloadFileFromBlobPart } from '@vben/utils';
// 引入 Button 控制折叠
import { Input as AInput, Tag as ATag, message, Progress as AProgress, InputNumber as AInputNumber, Button as AButton } from 'ant-design-vue';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  getEquivalentPage, getEquivalentPeriods, executeEquivalentCalc, updateCompletionRate
} from '#/api/mes/cost/allocation/equivalent';
import { useGridColumns, useGridFormSchema } from './data';

defineOptions({ name: 'MesCostEquivalent' });

const periodList = ref<any[]>([]);
const searchKeyword = ref('');
const selectedPeriod = ref<string>('2026-02');

// 说明面板折叠控制
const isDescExpanded = ref(true);

const filteredPeriods = computed(() => {
  if (!searchKeyword.value) return periodList.value;
  const kw = searchKeyword.value.toLowerCase();
  return periodList.value.filter(item =>
    item.period.includes(kw) || item.label.includes(kw)
  );
});

async function loadPeriods() {
  periodList.value = await getEquivalentPeriods();
}

function handlePeriodSelect(period: string) {
  if (selectedPeriod.value !== period) {
    selectedPeriod.value = period;
    handleRefresh();
  }
}

function handleRefresh() { gridApi.query(); }

async function handleRateChange(row: MesCostEquivalentApi.EquivalentRecord) {
  if (row.completionRate === null || row.completionRate === undefined) {
    row.completionRate = 0;
  }
  try {
    await updateCompletionRate(row.id, row.completionRate);
    // 强制触发 VxeTable 表尾合计重算，确保联动实时性
    gridApi.grid?.updateFooter();
  } catch (e) {
    message.error('完工率更新失败');
  }
}

async function handleExecuteCalc() {
  await confirm(`确定要对【${selectedPeriod.value}】期间的在制品执行约当产量折算吗？`);
  const hideLoading = message.loading({ content: '系统正在计算约当产量，请稍候...', duration: 0 });
  try {
    const res = await executeEquivalentCalc(selectedPeriod.value);
    message.success(res.msg);
    handleRefresh();
    loadPeriods();
  } finally {
    hideLoading();
  }
}

const [BaseGrid, gridApi] = useVbenVxeGrid({
  formOptions: { schema: useGridFormSchema() },
  gridOptions: {
    columns: useGridColumns(),
    height: 'auto', // 配合外层 flex 布局实现自适应
    keepSource: true,
    pagerConfig: { enabled: true },
    showFooter: true, // 开启表尾汇总行
    footerMethod: ({ columns, data }) => {
      return [
        columns.map((column, columnIndex) => {
          if (columnIndex === 0) return '当页合计';

          if (['wipQuantity', 'equivalentQty'].includes(column.field)) {
            const sum = data.reduce((prev, curr) => {
              if (column.field === 'wipQuantity') {
                return prev + (Number(curr.wipQuantity) || 0);
              }
              if (column.field === 'equivalentQty') {
                // 若状态为待折算(0)，则按完工率计算预估值；否则取实际值
                const val = curr.status === 0
                  ? Math.round(curr.wipQuantity * ((curr.completionRate || 0) / 100))
                  : curr.equivalentQty;
                return prev + (Number(val) || 0);
              }
              return prev;
            }, 0);
            return `${sum} 件`;
          }
          return '-';
        })
      ];
    },
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) => {
          return await getEquivalentPage({
            pageNo: page.currentPage,
            pageSize: page.pageSize,
            period: selectedPeriod.value,
            ...formValues
          });
        }
      },
    },
    rowConfig: { keyField: 'id', isHover: true },
    toolbarConfig: { refresh: true, search: true },
  } as VxeTableGridOptions<MesCostEquivalentApi.EquivalentRecord>,
});

onMounted(() => {
  loadPeriods();
});
</script>

<template>
  <Page auto-content-height>
    <div class="flex h-full gap-4">

      <div class="w-72 bg-background p-4 rounded-md shadow-sm border border-border flex flex-col h-full">
        <div class="mb-3 text-base font-semibold border-b border-border pb-2 flex items-center">
          <span class="i-ep:calendar mr-2"></span> 折算期间
        </div>
        <div class="mb-3">
          <a-input v-model:value="searchKeyword" placeholder="搜索期间" allow-clear>
            <template #prefix><span class="i-ep:search text-gray-400"></span></template>
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
              <a-tag :color="item.isCalculated ? 'success' : 'warning'" class="mr-0 border-0">
                {{ item.isCalculated ? '已结转' : '待处理' }}
              </a-tag>
            </div>
            <div class="text-xs text-gray-500 flex items-center mt-1">
              <span class="i-ep:price-tag mr-1"></span> 期间: {{ item.period }}
            </div>

            <div v-if="selectedPeriod === item.period" class="absolute left-0 top-0 bottom-0 w-1 bg-primary"></div>
          </div>
        </div>
      </div>

      <div class="flex-1 flex flex-col min-w-0 h-full">

        <div class="mb-3 border border-blue-200 rounded-md bg-blue-50/30 overflow-hidden transition-all duration-300 shadow-sm">
          <div
            class="px-4 py-3 flex justify-between items-center cursor-pointer hover:bg-blue-50/60"
            @click="isDescExpanded = !isDescExpanded"
          >
            <span class="font-bold text-[15px] text-blue-800 flex items-center">
              <span class="i-ep:info-filled mr-2 text-lg"></span> 约当产量折算模型
            </span>
            <a-button type="link" class="p-0 h-auto text-blue-700">
              {{ isDescExpanded ? '收起说明' : '展开说明' }}
              <span :class="isDescExpanded ? 'i-ep:arrow-up' : 'i-ep:arrow-down'" class="ml-1"></span>
            </a-button>
          </div>

          <div v-show="isDescExpanded" class="px-4 pb-4 text-sm leading-relaxed text-gray-700 border-t border-blue-100/50">
            <p class="mb-2 mt-2">
              系统基于月末车间工单在制品（WIP）的所在工序，结合《工艺路线》预设的完工权重自动推算完工程度。
              <span class="text-blue-600 font-medium">此数据将作为月底人工与机器制费摊销的重要分母。</span>
            </p>
            <div class="bg-blue-50/50 p-3 rounded-md border border-blue-100 mt-2 text-blue-800">
              <div class="font-bold mb-2 flex items-center">
                <span class="i-ep:data-analysis mr-1 text-lg"></span> 🧮 变量映射与计算公式：
              </div>
              <div class="grid grid-cols-3 gap-4 font-mono text-[13px]">
                <div class="bg-white p-2 rounded border border-blue-50 shadow-sm">
                  <div class="text-gray-500 mb-1">变量 A</div>
                  <div class="font-semibold">月末WIP物理数量</div>
                </div>
                <div class="bg-white p-2 rounded border border-blue-50 shadow-sm">
                  <div class="text-gray-500 mb-1">变量 B</div>
                  <div class="font-semibold">完工程度 % <span class="text-xs font-normal text-orange-500">(待处理时可人工修正)</span></div>
                </div>
                <div class="bg-blue-100 p-2 rounded border border-blue-200 shadow-sm">
                  <div class="text-blue-600 mb-1">结果 C</div>
                  <div class="font-bold text-base">C = A × B</div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="flex-1 overflow-hidden bg-background rounded-md shadow-sm border border-border">
          <BaseGrid :table-title="`【${selectedPeriod}】在制工单折算台账`" class="flex-1">
            <template #toolbar-tools>
              <TableAction
                :actions="[
                  { label: '执行本期折算', type: 'primary', icon: 'ep:cpu', auth: ['mes:cost-equivalent:calc'], onClick: handleExecuteCalc },
                  { label: '导出明细', type: 'default', icon: ACTION_ICON.DOWNLOAD },
                ]"
              />
            </template>

            <template #completionRate="{ row }">
              <div v-if="row.status === 0" class="flex items-center justify-center gap-2">
                <a-input-number
                  v-model:value="row.completionRate"
                  :min="0"
                  :max="100"
                  :precision="0"
                  size="small"
                  class="w-16 text-center"
                  @change="handleRateChange(row)"
                />
                <span class="text-gray-500">%</span>
              </div>
              <a-progress
                v-else
                :percent="row.completionRate"
                size="small"
                :status="row.completionRate === 100 ? 'success' : 'normal'"
                :stroke-color="row.completionRate > 50 ? '#108ee9' : '#f50'"
                class="m-0"
              />
            </template>

            <template #equivalentQty="{ row }">
              <span v-if="row.status === 0" class="text-gray-400">
                预估: {{ Math.round(row.wipQuantity * ((row.completionRate || 0) / 100)) }}
              </span>
              <span v-else class="font-bold text-blue-600 text-base">
                {{ row.equivalentQty }}
              </span>
            </template>

            <template #status="{ row }">
              <a-tag
                :color="row.status === 1 ? 'success' : 'warning'"
                class="m-0 border-0"
              >
                {{ row.status === 1 ? '已折算' : '待处理' }}
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

/* 修正 InputNumber 的默认样式使之更协调 */
:deep(.ant-input-number-input) {
  text-align: center;
}
</style>
