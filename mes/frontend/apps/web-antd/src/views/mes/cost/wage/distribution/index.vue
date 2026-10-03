<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesCostWageDistApi } from '#/api/mes/cost/wage/distribution';

import { onMounted, ref, computed } from 'vue';
import { Page, confirm } from '@vben/common-ui';
import { message, Tag as ATag, Alert as AAlert, InputNumber as AInputNumber, DatePicker as ADatePicker, Input as AInput } from 'ant-design-vue';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import { getDistPage, getDistTeams, executeDistribution, updateAdjustAmount } from '#/api/mes/cost/wage/distribution';
import { useGridColumns, useGridFormSchema } from './data';

defineOptions({ name: 'MesCostWageDistribution' });

// ================= 左侧周期与班组逻辑 =================
const period = ref<string>(new Date().toISOString().slice(0, 7));
const teamList = ref<any[]>([]);
const selectedTeamId = ref<string>('');

// 右侧看板数据
const currentPoolAmount = ref<number>(0);
const isDescExpanded = ref(true);

// VxeTable 内部数据引用，用于实时计算合计差额
const tableData = ref<MesCostWageDistApi.DistRecord[]>([]);

async function loadTeams() {
  teamList.value = await getDistTeams();
  if (teamList.value.length > 0 && !selectedTeamId.value) {
    handleTeamSelect(teamList.value[0]);
  }
}

function handleTeamSelect(team: any) {
  selectedTeamId.value = team.teamId;
  currentPoolAmount.value = team.poolAmount;
  handleRefresh();
}

// 监听月份变更
function onPeriodChange(val: string) {
  if (val) {
    period.value = val;
    loadTeams();
  }
}
// =========================================================

function handleRefresh() { gridApi.query(); }

// 计算属性：监控分配总额差异
const distributedTotal = computed(() => {
  return tableData.value.reduce((sum, item) => sum + (Number(item.finalAmount) || 0), 0);
});
const difference = computed(() => {
  return currentPoolAmount.value - distributedTotal.value;
});

// 行内修改微调金额
async function handleAdjustChange(row: MesCostWageDistApi.DistRecord) {
  if (row.adjustAmount === null || row.adjustAmount === undefined) {
    row.adjustAmount = 0;
  }
  try {
    await updateAdjustAmount(row.id, row.adjustAmount);
    // 前端静默重算行总额
    row.finalAmount = row.calcAmount + row.adjustAmount;
    // 触发 VxeTable 表尾更新
    gridApi.grid?.updateFooter();
  } catch (e) {
    message.error('微调更新失败');
  }
}

async function handleExecuteCalc() {
  await confirm(`确认要将奖金池 ¥${currentPoolAmount.value} 分配给【${teamList.value.find(t=>t.teamId===selectedTeamId.value)?.teamName}】的成员吗？这将覆盖目前的系统分配金额。`);
  const hideLoading = message.loading({ content: '分配引擎计算中...', duration: 0 });
  try {
    const res = await executeDistribution(period.value, selectedTeamId.value, currentPoolAmount.value);
    message.success(res.msg);
    // 标记为已计算并刷新
    const team = teamList.value.find(t=>t.teamId === selectedTeamId.value);
    if(team) team.isCalculated = true;
    handleRefresh();
  } finally { hideLoading(); }
}

const [BaseGrid, gridApi] = useVbenVxeGrid({
  formOptions: { schema: useGridFormSchema() },
  gridOptions: {
    columns: useGridColumns(),
    height: 'auto',
    keepSource: true,
    pagerConfig: { enabled: false }, // 分配表通常一页展示整个班组
    showFooter: true,
    footerMethod: ({ columns, data }) => {
      // 同步数据供外层计算差异看板使用
      tableData.value = data as MesCostWageDistApi.DistRecord[];

      return [
        columns.map((column, columnIndex) => {
          if (columnIndex === 1) return '班组合计';
          if (['baseHours', 'weightedHours'].includes(column.field)) {
            const sum = data.reduce((prev, curr) => prev + (Number(curr[column.field]) || 0), 0);
            return `${sum.toFixed(1)} H`;
          }
          if (['calcAmount', 'adjustAmount', 'finalAmount'].includes(column.field)) {
            const sum = data.reduce((prev, curr) => prev + (Number(curr[column.field]) || 0), 0);
            return `¥ ${sum.toFixed(2)}`;
          }
          return '-';
        })
      ];
    },
    proxyConfig: {
      ajax: {
        query: async (_, formValues) => {
          return await getDistPage({ period: period.value, teamId: selectedTeamId.value, ...formValues });
        }
      },
    },
    rowConfig: { keyField: 'id', isHover: true },
    toolbarConfig: { refresh: true, search: true },
  } as VxeTableGridOptions<MesCostWageDistApi.DistRecord>,
});

onMounted(() => {
  loadTeams();
});
</script>

<template>
  <Page auto-content-height>
    <div class="h-full flex gap-4 min-h-0">

      <div class="w-72 bg-background p-4 rounded-md shadow-sm border border-border flex flex-col h-full min-h-0">
        <div class="mb-4">
          <div class="text-sm text-gray-500 mb-1">核算期间</div>
          <a-date-picker v-model:value="period" picker="month" valueFormat="YYYY-MM" @change="onPeriodChange" class="w-full" :allowClear="false" />
        </div>

        <div class="mb-3 text-base font-semibold border-b border-border pb-2 flex items-center mt-2">
          <span class="i-ep:menu mr-2"></span> 产线班组清单
        </div>

        <div class="flex-1 overflow-y-auto space-y-2 pr-1 custom-scrollbar">
          <div
            v-for="item in teamList" :key="item.teamId" @click="handleTeamSelect(item)"
            :class="[
              'p-3 rounded-md border cursor-pointer transition-all duration-200 relative overflow-hidden select-none',
              selectedTeamId === item.teamId ? 'border-primary bg-primary/10 shadow-sm' : 'border-border bg-layout hover:border-primary/50'
            ]"
          >
            <div class="flex justify-between items-center mb-1">
              <span class="font-medium text-[15px]" :class="selectedTeamId === item.teamId ? 'text-primary' : ''">{{ item.teamName }}</span>
              <a-tag :color="item.isCalculated ? 'success' : 'default'" class="mr-0 border-0">{{ item.isCalculated ? '已分配' : '待处理' }}</a-tag>
            </div>
            <div class="text-xs text-gray-500 flex items-center mt-1">
              计件池: ¥ {{ Number(item.poolAmount).toFixed(2) }}
            </div>
            <div v-if="selectedTeamId === item.teamId" class="absolute left-0 top-0 bottom-0 w-1 bg-primary"></div>
          </div>
        </div>
      </div>

      <div class="flex-1 flex flex-col min-w-0 h-full">

        <div class="mb-3 bg-white border border-border rounded-md shadow-sm flex items-center p-4 justify-between">
          <div class="flex gap-8">
            <div>
              <div class="text-gray-500 text-sm mb-1">班组可分配计件奖金池总量</div>
              <div class="text-2xl font-bold text-gray-800">¥ {{ currentPoolAmount.toFixed(2) }}</div>
            </div>
            <div class="w-px bg-gray-200"></div>
            <div>
              <div class="text-gray-500 text-sm mb-1">当前已分配总额 (含微调)</div>
              <div class="text-2xl font-bold text-blue-600">¥ {{ distributedTotal.toFixed(2) }}</div>
            </div>
            <div class="w-px bg-gray-200"></div>
            <div>
              <div class="text-gray-500 text-sm mb-1">分配差额 (需确保平账为0)</div>
              <div class="text-2xl font-bold" :class="Math.abs(difference) > 0.01 ? 'text-red-500' : 'text-green-500'">
                {{ difference > 0 ? '+' : '' }}¥ {{ difference.toFixed(2) }}
              </div>
            </div>
          </div>
          <div class="text-right">
            <a-button type="primary" size="large" @click="handleExecuteCalc">
              <template #icon><span class="i-ep:cpu"></span></template>
              一键执行二次分配
            </a-button>
          </div>
        </div>

        <div class="flex-1 overflow-hidden bg-background rounded-md shadow-sm border border-border flex flex-col">
          <BaseGrid :table-title="`【${teamList.find(t=>t.teamId===selectedTeamId)?.teamName}】人员计件分配明细`" class="flex-1">
            <template #toolbar-tools>
              <TableAction :actions="[{ label: '导出分配表', type: 'default', icon: ACTION_ICON.DOWNLOAD }]" />
            </template>

            <template #adjustAmount="{ row }">
              <div class="flex items-center justify-end w-full gap-1">
                <a-input-number v-model:value="row.adjustAmount" :precision="2" size="small" class="w-24 text-right" @change="handleAdjustChange(row)" />
              </div>
            </template>

            <template #remark="{ row }">
              <a-input v-model:value="row.remark" size="small" placeholder="微调原因" class="w-full bg-transparent border-transparent hover:border-gray-300 focus:border-primary" />
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

:deep(.ant-input-number-input) { text-align: right; }
</style>
