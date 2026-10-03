<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesCostWagePerfApi } from '#/api/mes/cost/wage/performance';

import { onMounted, ref } from 'vue';
import { confirm, Page, useVbenModal } from '@vben/common-ui';
import { isEmpty } from '@vben/utils';
import { message, Tag as ATag, Alert as AAlert, Button as AButton } from 'ant-design-vue';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import { getPerfPage, deletePerfRecords, approvePerfRecords } from '#/api/mes/cost/wage/performance';
import { useGridColumns, useGridFormSchema, STATUS_OPTIONS } from './data';
import FormComponent from './modules/form.vue';

defineOptions({ name: 'MesCostWagePerf' });

const [FormModal, formModalApi] = useVbenModal({ connectedComponent: FormComponent, destroyOnClose: true });
const checkedIds = ref<number[]>([]);
const isDescExpanded = ref(true);

function handleRefresh() { gridApi.query(); }

function handleCreate() { formModalApi.setData({}).open(); }

function handleEdit(row: MesCostWagePerfApi.PerfRecord) {
  formModalApi.setData({ id: row.id }).open();
}

function handleRowCheckboxChange({ records }: { records: MesCostWagePerfApi.PerfRecord[] }) {
  checkedIds.value = records.map((item) => item.id);
}

async function handleDeleteBatch() {
  await confirm('确认要删除选中的草稿记录吗？（已锁定或系统生成的记录无法删除）');
  const hideLoading = message.loading({ content: '删除中...', duration: 0 });
  try {
    await deletePerfRecords(checkedIds.value);
    checkedIds.value = [];
    message.success('删除成功');
    handleRefresh();
  } finally { hideLoading(); }
}

async function handleApproveBatch() {
  await confirm('确认要审核并锁定选中的奖惩记录吗？锁定后将直接汇入本月工资计算引擎，不可撤销。');
  const hideLoading = message.loading({ content: '审核锁定中...', duration: 0 });
  try {
    const res = await approvePerfRecords(checkedIds.value);
    checkedIds.value = [];
    message.success(res.msg);
    handleRefresh();
  } finally { hideLoading(); }
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
          if (columnIndex === 1) return '当页合计:';
          if (column.field === 'amount') {
            // 分别统计奖励(type=1)和扣款(type=2)
            const rewards = data.filter(d => d.type === 1).reduce((prev, curr) => prev + (Number(curr.amount) || 0), 0);
            const punishes = data.filter(d => d.type === 2).reduce((prev, curr) => prev + (Number(curr.amount) || 0), 0);
            return `总奖励: +¥${rewards.toFixed(2)} | 总扣款: -¥${punishes.toFixed(2)}`;
          }
          return '-';
        })
      ];
    },
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) => await getPerfPage({
          pageNo: page.currentPage, pageSize: page.pageSize, ...formValues
        })
      },
    },
    // 复选框拦截：仅草稿可选
    checkboxConfig: { checkMethod: ({ row }) => row.status === 0 },
    rowConfig: { keyField: 'id', isHover: true },
    toolbarConfig: { refresh: true, search: true },
  } as VxeTableGridOptions<MesCostWagePerfApi.PerfRecord>,
  gridEvents: { checkboxAll: handleRowCheckboxChange, checkboxChange: handleRowCheckboxChange },
});

onMounted(() => {
  // 默认过滤出“待确认”的数据进行处理
  gridApi.formApi.setValues({ status: 0 });
});
</script>

<template>
  <Page auto-content-height>
    <FormModal @success="handleRefresh" />
    <div class="h-full flex flex-col min-h-0 gap-3">

      <div class="border border-indigo-200 rounded-md bg-indigo-50/30 overflow-hidden shadow-sm">
        <div class="px-4 py-3 flex justify-between items-center cursor-pointer hover:bg-indigo-50/60" @click="isDescExpanded = !isDescExpanded">
          <span class="font-bold text-[15px] text-indigo-800 flex items-center">
            <span class="i-ep:medal mr-2 text-lg"></span> 车间人员绩效与奖惩记录台账
          </span>
          <a-button type="link" class="p-0 h-auto text-indigo-700">
            {{ isDescExpanded ? '收起说明' : '展开说明' }}
            <span :class="isDescExpanded ? 'i-ep:arrow-up' : 'i-ep:arrow-down'" class="ml-1"></span>
          </a-button>
        </div>
        <div v-show="isDescExpanded" class="px-4 pb-4 text-sm leading-relaxed text-gray-700 border-t border-indigo-100/50">
          <p class="mb-2 mt-2">
            管理员工除计件/计时基础工资外的额外奖惩款项。数据来源分为 <span class="text-blue-600 font-medium">车间主任手工提报</span> 与 <span class="text-purple-600 font-medium">周边系统自动推送</span>（如质检系统的 NCR 报废判定）。
          </p>
          <div class="bg-white p-3 rounded border border-indigo-100 text-indigo-900 mt-2 font-mono text-[13px] shadow-sm inline-block">
            <span class="i-ep:circle-check text-green-500 mr-1"></span>
            <span class="font-bold">核算流转逻辑：</span> 只有经审核人员核对并变为 <span class="bg-indigo-100 text-indigo-800 px-1 py-0.5 rounded">已审核锁定</span> 的记录，才会作为 <span class="text-green-600 font-bold">增项</span> 或 <span class="text-red-600 font-bold">减项</span>，合并入员工当月的最终发薪台账。
          </div>
        </div>
      </div>

      <div class="flex-1 overflow-hidden bg-background rounded-md shadow-sm border border-border flex flex-col">
        <BaseGrid table-title="个人绩效奖惩台账" class="flex-1">
          <template #toolbar-tools>
            <TableAction
              :actions="[
                { label: '手工新增奖惩', type: 'primary', icon: ACTION_ICON.ADD, auth: ['mes:cost-wage:perf:create'], onClick: handleCreate },
                { label: '批量审核锁定', type: 'primary', icon: 'ep:check', disabled: isEmpty(checkedIds), auth: ['mes:cost-wage:perf:update'], onClick: handleApproveBatch },
                { label: '批量删除草稿', type: 'default', danger: true, icon: ACTION_ICON.DELETE, disabled: isEmpty(checkedIds), auth: ['mes:cost-wage:perf:delete'], onClick: handleDeleteBatch },
                { label: '导出台账', type: 'default', icon: ACTION_ICON.DOWNLOAD },
              ]"
            />
          </template>

          <template #status="{ row }">
            <a-tag :color="STATUS_OPTIONS.find(o => o.value === row.status)?.color" class="m-0 border-0">
              <template #icon v-if="row.status === 1"><span class="i-ep:success-filled mr-1"></span></template>
              {{ STATUS_OPTIONS.find(o => o.value === row.status)?.label }}
            </a-tag>
          </template>

          <template #actions="{ row }">
            <TableAction
              :actions="[
                { label: '审核确认', type: 'link', icon: 'ep:check', ifShow: row.status === 0, onClick: () => { checkedIds.value=[row.id]; handleApproveBatch(); } },
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
