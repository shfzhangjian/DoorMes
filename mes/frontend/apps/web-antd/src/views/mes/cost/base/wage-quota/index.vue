<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesCostWageApi } from '#/api/mes/cost/base/wage-quota';

import { ref } from 'vue';
import { confirm, Page, useVbenModal } from '@vben/common-ui';
import { isEmpty } from '@vben/utils';
import { message, Tag as ATag, Alert as AAlert } from 'ant-design-vue';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import { getWageQuotaPage, deleteWageQuota, enableWageQuota } from '#/api/mes/cost/base/wage-quota';
import { useGridColumns, useGridFormSchema, STATUS_OPTIONS } from './data';
import FormComponent from './modules/form.vue';

defineOptions({ name: 'MesCostWageQuota' });

const [FormModal, formModalApi] = useVbenModal({ connectedComponent: FormComponent, destroyOnClose: true });
const checkedIds = ref<number[]>([]);

function handleRefresh() { gridApi.query(); }

function handleCreate() { formModalApi.setData({}).open(); }

function handleEdit(row: MesCostWageApi.WageQuota) {
  formModalApi.setData({ id: row.id }).open();
}

function handleNewVersion(row: MesCostWageApi.WageQuota) {
  formModalApi.setData({ id: row.id, action: 'newVersion' }).open();
}

async function handleDeleteBatch() {
  await confirm('确认要删除选中的草稿定额吗？已生效或历史版本无法删除。');
  const hideLoading = message.loading({ content: '删除中...', duration: 0 });
  try {
    await deleteWageQuota(checkedIds.value);
    checkedIds.value = [];
    message.success('删除成功');
    handleRefresh();
  } finally { hideLoading(); }
}

async function handleEnable(row: MesCostWageApi.WageQuota) {
  await confirm(`确认要发布并生效 ${row.productName} [${row.processName}] 的 ${row.version} 版本吗？这将自动截断当前生效版本的有效期！`);
  const hideLoading = message.loading({ content: '版本发布中...', duration: 0 });
  try {
    const res = await enableWageQuota(row.id);
    message.success(res.msg);
    handleRefresh();
  } finally { hideLoading(); }
}

function handleRowCheckboxChange({ records }: { records: MesCostWageApi.WageQuota[] }) {
  checkedIds.value = records.map((item) => item.id);
}

const [BaseGrid, gridApi] = useVbenVxeGrid({
  formOptions: { schema: useGridFormSchema() },
  gridOptions: {
    columns: useGridColumns(),
    height: 'auto', // 配合外层 flex 布局实现自适应高度
    keepSource: true,
    pagerConfig: { enabled: true },
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) => await getWageQuotaPage({
          pageNo: page.currentPage, pageSize: page.pageSize, ...formValues
        })
      },
    },
    checkboxConfig: { checkMethod: ({ row }) => row.status === 0 },
    rowConfig: { keyField: 'id', isHover: true },
    toolbarConfig: { refresh: true, search: true },
  } as VxeTableGridOptions<MesCostWageApi.WageQuota>,
  gridEvents: { checkboxAll: handleRowCheckboxChange, checkboxChange: handleRowCheckboxChange },
});
</script>

<template>
  <Page auto-content-height>
    <FormModal @success="handleRefresh" />
    <div class="h-full flex flex-col min-h-0">
      <a-alert type="info" show-icon class="mb-3">
        <template #message>
          <span class="font-bold">薪酬定额管理模型</span>
        </template>
        <template #description>
          维护各类产品在不同工序加工时的“计件单价”或“标准计费率”。
          <span class="text-blue-600">系统实行强版本控制：新版本发布生效后，系统将自动把旧版本变更为“历史版本”并截断其失效日期，确保月末计算人工成本时费率唯一不交叉。</span>
        </template>
      </a-alert>

      <div class="flex-1 overflow-hidden bg-background rounded-md shadow-sm border border-border flex flex-col">
        <BaseGrid table-title="生产薪酬定额台账" class="flex-1">
          <template #toolbar-tools>
            <TableAction
              :actions="[
                { label: '新增定额', type: 'primary', icon: ACTION_ICON.ADD, auth: ['mes:cost-wage:create'], onClick: handleCreate },
                { label: '批量删除草稿', type: 'primary', danger: true, icon: ACTION_ICON.DELETE, disabled: isEmpty(checkedIds), onClick: handleDeleteBatch },
              ]"
            />
          </template>

          <template #status="{ row }">
            <a-tag :color="STATUS_OPTIONS.find(o => o.value === row.status)?.color" class="m-0 border-0">
              {{ STATUS_OPTIONS.find(o => o.value === row.status)?.label }}
            </a-tag>
          </template>

          <template #actions="{ row }">
            <TableAction
              :actions="[
                { label: '发布生效', type: 'link', icon: 'ep:check', ifShow: row.status === 0, onClick: handleEnable.bind(null, row) },
                { label: '编辑草稿', type: 'link', icon: ACTION_ICON.EDIT, ifShow: row.status === 0, onClick: handleEdit.bind(null, row) },
                { label: '删除', type: 'link', danger: true, icon: ACTION_ICON.DELETE, ifShow: row.status === 0, popConfirm: { title: '确认删除?', confirm: () => { checkedIds.value=[row.id]; handleDeleteBatch(); } } },
                { label: '变更版本', type: 'link', icon: 'ep:document-copy', ifShow: row.status === 1, onClick: handleNewVersion.bind(null, row) },
              ]"
            />
          </template>
        </BaseGrid>
      </div>
    </div>
  </Page>
</template>
