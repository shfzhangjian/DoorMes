<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesCostWageMonthApi } from '#/api/mes/cost/wage/monthly';

import { onMounted, ref } from 'vue';
import { confirm, Page } from '@vben/common-ui';
import { isEmpty } from '@vben/utils';
import { message, Tag as ATag, Button as AButton, InputNumber as AInputNumber } from 'ant-design-vue';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import { getMonthlyWagePage, calculateMonthlyWage, auditMonthlyWage, updateMonthlyWageItem } from '#/api/mes/cost/wage/monthly';
import { useGridColumns, useGridFormSchema, STATUS_OPTIONS } from './data';

defineOptions({ name: 'MesCostWageMonthly' });

const checkedIds = ref<number[]>([]);
const isDescExpanded = ref(true);

function handleRefresh() { gridApi.query(); }

function handleRowCheckboxChange({ records }: { records: MesCostWageMonthApi.MonthlyWage[] }) {
  checkedIds.value = records.map((item) => item.id);
}

const getCurrentPeriod = () => {
  const formVals = gridApi.formApi.getValues();
  return formVals.period || new Date().toISOString().slice(0, 7);
};

async function handleCalculateAll() {
  const period = getCurrentPeriod();
  await confirm(`将重算全车间 ${period} 期间的薪酬。已封账记录将被跳过。确认执行吗？`);
  const hideLoading = message.loading({ content: '聚合计算中...', duration: 0 });
  try {
    const res = await calculateMonthlyWage(period);
    message.success(res.msg);
    handleRefresh();
  } finally { hideLoading(); }
}

async function handleAuditBatch() {
  const period = getCurrentPeriod();
  await confirm(`确认封账选定的 ${checkedIds.value.length} 条记录吗？封账后不可再修改和重算。`);
  const hideLoading = message.loading({ content: '封账处理中...', duration: 0 });
  try {
    const res = await auditMonthlyWage(period, checkedIds.value);
    checkedIds.value = [];
    message.success(res.msg);
    handleRefresh();
  } finally { hideLoading(); }
}

// 行内修改数值触发
async function handleItemChange(row: MesCostWageMonthApi.MonthlyWage, field: string) {
  if (row.status === 1) return;
  const val = (row as any)[field];
  if (val === null || val === undefined || val < 0) {
    (row as any)[field] = 0;
  }
  try {
    await updateMonthlyWageItem(row.id, field, (row as any)[field]);
    // 前端同步重算总工资
    row.grossWage = row.directPiece + row.teamPiece + row.timeBased + row.overtime + row.subsidy + row.reward - row.punish;
    gridApi.grid?.updateFooter();
  } catch (e) {
    message.error('数据修改保存失败');
  }
}

const [BaseGrid, gridApi] = useVbenVxeGrid({
  formOptions: {
    schema: useGridFormSchema(),
    baseColProps: { span: 6 },
  },
  gridOptions: {
    columns: useGridColumns(),
    height: 'auto',
    keepSource: true,
    pagerConfig: { enabled: true },
    showFooter: true,
    footerMethod: ({ columns, data }) => {
      return [
        columns.map((column, columnIndex) => {
          if (columnIndex === 1) return '当页核算总计:';
          if (['directPiece', 'teamPiece', 'timeBased', 'overtime', 'subsidy', 'reward', 'punish', 'grossWage'].includes(column.field)) {
            const sum = data.reduce((prev, curr) => prev + (Number(curr[column.field]) || 0), 0);
            return `¥ ${sum.toFixed(2)}`;
          }
          return '-';
        })
      ];
    },
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) => await getMonthlyWagePage({
          pageNo: page.currentPage, pageSize: page.pageSize, ...formValues
        })
      },
    },
    checkboxConfig: { checkMethod: ({ row }) => row.status === 0 },
    rowConfig: { keyField: 'id', isHover: true },
    toolbarConfig: { refresh: true, search: true },
  } as VxeTableGridOptions<MesCostWageMonthApi.MonthlyWage>,
  gridEvents: { checkboxAll: handleRowCheckboxChange, checkboxChange: handleRowCheckboxChange },
});

onMounted(() => {
  const currentMonth = new Date().toISOString().slice(0, 7);
  gridApi.formApi.setValues({ period: currentMonth });
});
</script>

<template>
  <Page auto-content-height>
    <div class="h-full flex flex-col min-h-0 gap-3">

      <div class="border border-red-200 rounded-md bg-red-50/30 overflow-hidden shadow-sm">
        <div class="px-4 py-3 flex justify-between items-center cursor-pointer hover:bg-red-50/60" @click="isDescExpanded = !isDescExpanded">
          <span class="font-bold text-[15px] text-red-800 flex items-center">
            <span class="i-ep:document-checked mr-2 text-lg"></span> 车间月度薪酬核算总览 (封账引擎)
          </span>
          <a-button type="link" class="p-0 h-auto text-red-700">
            {{ isDescExpanded ? '收起说明' : '展开说明' }}
            <span :class="isDescExpanded ? 'i-ep:arrow-up' : 'i-ep:arrow-down'" class="ml-1"></span>
          </a-button>
        </div>
        <div v-show="isDescExpanded" class="px-4 pb-4 text-sm leading-relaxed text-gray-700 border-t border-red-100/50">
          <p class="mb-2 mt-2">
            此表将自动汇总当期计件、工时与奖惩台账。针对特殊漏算项，支持在封账前进行 <span class="text-blue-600 font-bold">行内直接修改 (补贴/奖励/扣款)</span>。
          </p>
          <div class="bg-white p-3 rounded border border-red-100 text-red-900 mt-2 font-mono text-[13px] shadow-sm inline-block w-full">
            <span class="font-bold">底栏公式引擎校验：</span><br/>
            <span class="text-blue-700 font-bold">税前总薪资</span> =
            (直接计件 + 二次分配) + 计时底薪 + 加班费 + 补贴 + <span class="text-green-600">正向奖励</span> - <span class="text-red-600">违规扣款</span>
          </div>
        </div>
      </div>

      <div class="flex-1 overflow-hidden bg-background rounded-md shadow-sm border border-border flex flex-col">
        <BaseGrid table-title="个人月度薪酬台账明细" class="flex-1">
          <template #toolbar-tools>
            <TableAction
              :actions="[
                { label: '一键核算工资(重算刷新)', type: 'primary', icon: 'ep:cpu', auth: ['mes:cost-wage:month:calc'], onClick: handleCalculateAll },
                { label: '审核封账锁定', type: 'primary', danger: true, icon: 'ep:lock', disabled: isEmpty(checkedIds), auth: ['mes:cost-wage:month:audit'], onClick: handleAuditBatch },
                { label: '导出对接表', type: 'default', icon: ACTION_ICON.DOWNLOAD, auth: ['mes:cost-wage:month:export'] },
              ]"
            />
          </template>

          <template #subsidy="{ row }">
            <div v-if="row.status === 0" class="flex items-center justify-end w-full">
              <a-input-number v-model:value="row.subsidy" :min="0" :precision="2" size="small" class="w-24 text-right" @change="handleItemChange(row, 'subsidy')" />
            </div>
            <span v-else class="text-gray-700">¥ {{ Number(row.subsidy).toFixed(2) }}</span>
          </template>

          <template #reward="{ row }">
            <div v-if="row.status === 0" class="flex items-center justify-end w-full">
              <a-input-number v-model:value="row.reward" :min="0" :precision="2" size="small" class="w-24 text-right text-green-600" @change="handleItemChange(row, 'reward')" />
            </div>
            <span v-else class="text-green-600 font-bold" v-show="row.reward > 0">+ ¥ {{ Number(row.reward).toFixed(2) }}</span>
          </template>

          <template #punish="{ row }">
            <div v-if="row.status === 0" class="flex items-center justify-end w-full">
              <a-input-number v-model:value="row.punish" :min="0" :precision="2" size="small" class="w-24 text-right text-red-600" @change="handleItemChange(row, 'punish')" />
            </div>
            <span v-else class="text-red-600 font-bold" v-show="row.punish > 0">- ¥ {{ Number(row.punish).toFixed(2) }}</span>
          </template>

          <template #status="{ row }">
            <a-tag :color="STATUS_OPTIONS.find(o => o.value === row.status)?.color" class="m-0 border-0">
              <template #icon v-if="row.status === 1"><span class="i-ep:success-filled mr-1"></span></template>
              {{ STATUS_OPTIONS.find(o => o.value === row.status)?.label || '未知' }}
            </a-tag>
          </template>

        </BaseGrid>
      </div>

    </div>
  </Page>
</template>

<style scoped>
:deep(.ant-input-number-input) { text-align: right; }
</style>
