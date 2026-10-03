<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesCostWageAttendApi } from '#/api/mes/cost/wage/attendance';

import { onMounted, ref } from 'vue';
import { confirm, Page } from '@vben/common-ui';
import { isEmpty } from '@vben/utils';
import { message, Tag as ATag, Alert as AAlert, InputNumber as AInputNumber, Button as AButton } from 'ant-design-vue';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import { getAttendPage, updateAttendHours, verifyAttendanceList, unverifyAttendanceList } from '#/api/mes/cost/wage/attendance';
import { useGridColumns, useGridFormSchema, STATUS_OPTIONS } from './data';

defineOptions({ name: 'MesCostWageAttend' });

const checkedIds = ref<number[]>([]);
const isDescExpanded = ref(true);

function handleRefresh() { gridApi.query(); }

function handleRowCheckboxChange({ records }: { records: MesCostWageAttendApi.AttendanceRecord[] }) {
  checkedIds.value = records.map((item) => item.id);
}

// 行内修改工时或补贴触发 (状态必须为待核对)
async function handleHoursChange(row: MesCostWageAttendApi.AttendanceRecord, field: string) {
  if (row.status === 1) return; // 防御性判断
  const val = (row as any)[field];
  if (val === null || val === undefined || val < 0) {
    (row as any)[field] = 0;
  }
  try {
    await updateAttendHours(row.id, field, (row as any)[field]);
    // 更新表尾合计工时
    gridApi.grid?.updateFooter();
  } catch (e) {
    message.error('工时修正失败');
  }
}

// 核心功能：批量审核工时
async function handleVerifyBatch() {
  await confirm('确认要核对并锁定选中的工时记录吗？锁定后将不可再修改工时。');
  const hideLoading = message.loading({ content: '工时核对中...', duration: 0 });
  try {
    const res = await verifyAttendanceList(checkedIds.value);
    checkedIds.value = [];
    message.success(res.msg);
    handleRefresh();
  } finally { hideLoading(); }
}

async function handleUnverifyBatch() {
  await confirm('确认要撤销核对吗？将恢复为可编辑状态。');
  try {
    await unverifyAttendanceList(checkedIds.value);
    checkedIds.value = [];
    message.success('撤销成功');
    handleRefresh();
  } catch(e) {}
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
          if (columnIndex === 1) return '当页汇总工时:';
          if (['adjRegularHours', 'adjOvertimeHours', 'nightShiftSubsidy'].includes(column.field)) {
            const sum = data.reduce((prev, curr) => prev + (Number(curr[column.field]) || 0), 0);
            return column.field === 'nightShiftSubsidy' ? `${sum} 次` : `${sum.toFixed(1)} H`;
          }
          return '-';
        })
      ];
    },
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) => await getAttendPage({
          pageNo: page.currentPage, pageSize: page.pageSize, ...formValues
        })
      },
    },
    rowConfig: { keyField: 'id', isHover: true },
    toolbarConfig: { refresh: true, search: true },
  } as VxeTableGridOptions<MesCostWageAttendApi.AttendanceRecord>,
  gridEvents: { checkboxAll: handleRowCheckboxChange, checkboxChange: handleRowCheckboxChange },
});

onMounted(() => {
  // 默认带入今日日期作为查询条件
  // const today = new Date().toISOString().slice(0, 10);
  // gridApi.formApi.setValues({ attendDate: today, status: 0 });
  gridApi.formApi.setValues({ status: 0 });
});
</script>

<template>
  <Page auto-content-height>
    <div class="h-full flex flex-col min-h-0 gap-3">

      <div class="border border-green-200 rounded-md bg-green-50/30 overflow-hidden shadow-sm">
        <div class="px-4 py-3 flex justify-between items-center cursor-pointer hover:bg-green-50/60" @click="isDescExpanded = !isDescExpanded">
          <span class="font-bold text-[15px] text-green-800 flex items-center">
            <span class="i-ep:clock mr-2 text-lg"></span> 车间考勤与有效结算工时核对
          </span>
          <a-button type="link" class="p-0 h-auto text-green-700">
            {{ isDescExpanded ? '收起说明' : '展开说明' }}
            <span :class="isDescExpanded ? 'i-ep:arrow-up' : 'i-ep:arrow-down'" class="ml-1"></span>
          </a-button>
        </div>
        <div v-show="isDescExpanded" class="px-4 pb-4 text-sm leading-relaxed text-gray-700 border-t border-green-100/50">
          <p class="mb-2 mt-2">
            系统已根据员工当天的打卡流水自动推算出初始考勤工时。由于存在漏打卡、调休等异常，车间主任需每日在此台账中进行确认微调。
          </p>
          <div class="bg-white p-3 rounded border border-green-100 text-green-900 mt-2 font-mono text-[13px] shadow-sm inline-block">
            <span class="i-ep:warning text-orange-500 mr-1"></span>
            <span class="font-bold">注意：</span> 只有状态被标记为 <span class="bg-green-100 text-green-800 px-1 py-0.5 rounded">已核对锁定</span> 的记录，其工时数据才会被月末【计件二次分配】和【计时工资计算】模块抓取提取。
          </div>
        </div>
      </div>

      <div class="flex-1 overflow-hidden bg-background rounded-md shadow-sm border border-border flex flex-col">
        <BaseGrid table-title="每日车间工时台账" class="flex-1">
          <template #toolbar-tools>
            <TableAction
              :actions="[
                { label: '批量核对锁定', type: 'primary', icon: 'ep:check', disabled: isEmpty(checkedIds), auth: ['mes:cost-wage:attend:update'], onClick: handleVerifyBatch },
                { label: '撤销核对', type: 'default', danger: true, icon: 'ep:refresh-left', disabled: isEmpty(checkedIds), auth: ['mes:cost-wage:attend:update'], onClick: handleUnverifyBatch },
                { label: '导出明细', type: 'default', icon: ACTION_ICON.DOWNLOAD },
              ]"
            />
          </template>

          <template #adjRegularHours="{ row }">
            <div v-if="row.status === 0" class="flex items-center justify-center gap-1">
              <a-input-number v-model:value="row.adjRegularHours" :min="0" :max="24" :step="0.5" :precision="1" size="small" class="w-16 text-center" @change="handleHoursChange(row, 'adjRegularHours')" />
            </div>
            <span v-else class="text-blue-600 font-bold">{{ Number(row.adjRegularHours).toFixed(1) }}</span>
          </template>

          <template #adjOvertimeHours="{ row }">
            <div v-if="row.status === 0" class="flex items-center justify-center gap-1">
              <a-input-number v-model:value="row.adjOvertimeHours" :min="0" :max="24" :step="0.5" :precision="1" size="small" class="w-16 text-center" @change="handleHoursChange(row, 'adjOvertimeHours')" />
            </div>
            <span v-else :class="row.adjOvertimeHours > 0 ? 'text-orange-600 font-bold' : 'text-gray-400'">{{ Number(row.adjOvertimeHours).toFixed(1) }}</span>
          </template>

          <template #nightShiftSubsidy="{ row }">
            <div v-if="row.status === 0" class="flex items-center justify-center gap-1">
              <a-input-number v-model:value="row.nightShiftSubsidy" :min="0" :max="3" :step="1" :precision="0" size="small" class="w-14 text-center" @change="handleHoursChange(row, 'nightShiftSubsidy')" />
            </div>
            <span v-else class="text-gray-700">{{ row.nightShiftSubsidy }}</span>
          </template>

          <template #status="{ row }">
            <a-tag :color="STATUS_OPTIONS.find(o => o.value === row.status)?.color" class="m-0 border-0">
              <template #icon v-if="row.status === 1"><span class="i-ep:success-filled mr-1"></span></template>
              {{ STATUS_OPTIONS.find(o => o.value === row.status)?.label }}
            </a-tag>
          </template>
        </BaseGrid>
      </div>

    </div>
  </Page>
</template>

<style scoped>
:deep(.ant-input-number-input) { text-align: center; }
</style>
