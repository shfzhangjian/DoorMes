<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesHandoverApi } from '#/api/mes/execution/handover';

import { ref } from 'vue';
import { confirm, Page, useVbenModal } from '@vben/common-ui';
import { isEmpty } from '@vben/utils';
import { message, Tag } from 'ant-design-vue';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import { deleteHandoverList, getHandoverPage } from '#/api/mes/execution/handover';
import { $t } from '#/locales';

import { useGridColumns, useGridFormSchema } from './data';
import FormComponent from './modules/form.vue';

defineOptions({ name: 'MesHandoverWorkbench' });

const [FormModal, formModalApi] = useVbenModal({ connectedComponent: FormComponent, destroyOnClose: true });

function handleRefresh() { gridApi.query(); }

function handleCreate() { formModalApi.setData({ type: 'create' }).open(); }
function handleEdit(row: MesHandoverApi.Handover) { formModalApi.setData({ type: 'edit', id: row.id }).open(); }
function handleDetail(row: MesHandoverApi.Handover) { formModalApi.setData({ type: 'detail', id: row.id }).open(); }

async function handleDeleteBatch() {
  await confirm('确认要删除选中的交接班记录吗？');
  const hideLoading = message.loading({ content: '删除中...', duration: 0 });
  try {
    await deleteHandoverList(checkedIds.value);
    checkedIds.value = [];
    message.success('删除成功');
    handleRefresh();
  } finally { hideLoading(); }
}

const checkedIds = ref<number[]>([]);
function handleRowCheckboxChange({ records }: { records: MesHandoverApi.Handover[] }) {
  checkedIds.value = records.map((item) => item.id!);
}

const [BaseGrid, gridApi] = useVbenVxeGrid({
  formOptions: { schema: useGridFormSchema() },
  gridOptions: {
    columns: useGridColumns(),
    height: 'auto',
    keepSource: true,
    proxyConfig: {
      ajax: { query: async ({ page }, formValues) => await getHandoverPage({ pageNo: page.currentPage, pageSize: page.pageSize, ...formValues }) },
    },
    rowConfig: { keyField: 'id', isHover: true },
    toolbarConfig: { refresh: true, search: true },
  } as VxeTableGridOptions<MesHandoverApi.Handover>,
  gridEvents: { checkboxAll: handleRowCheckboxChange, checkboxChange: handleRowCheckboxChange },
});
</script>

<template>
  <Page auto-content-height>
    <FormModal @success="handleRefresh" />
    <BaseGrid table-title="交接班工作台记录">
      <template #toolbar-tools>
        <TableAction
          :actions="[
            { label: '发起交接', type: 'primary', icon: ACTION_ICON.ADD, onClick: handleCreate },
            { label: '批量删除', type: 'primary', danger: true, icon: ACTION_ICON.DELETE, disabled: isEmpty(checkedIds), onClick: handleDeleteBatch },
          ]"
        />
      </template>

      <template #status="{ row }">
        <Tag color="orange" v-if="row.status === 'PENDING'">待接班</Tag>
        <Tag color="success" v-else-if="row.status === 'COMPLETED'">已完成</Tag>
        <Tag color="error" v-else-if="row.status === 'ABNORMAL'">异常报备</Tag>
      </template>

      <template #actions="{ row }">
        <TableAction
          :actions="[
            { label: $t('common.detail'), type: 'link', icon: ACTION_ICON.VIEW, onClick: handleDetail.bind(null, row) },
            { label: $t('common.edit'), type: 'link', icon: ACTION_ICON.EDIT, onClick: handleEdit.bind(null, row) },
            { label: $t('common.delete'), type: 'link', danger: true, icon: ACTION_ICON.DELETE, popConfirm: { title: '确认删除?', confirm: () => handleDeleteBatch() } },
          ]"
        />
      </template>
    </BaseGrid>
  </Page>
</template>
