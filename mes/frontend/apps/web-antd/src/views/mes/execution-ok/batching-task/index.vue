<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesBatchingTaskApi } from '#/api/mes/execution/batching-task';

import { ref } from 'vue';
import { confirm, Page, useVbenModal } from '@vben/common-ui';
import { downloadFileFromBlobPart, isEmpty } from '@vben/utils';
import { message } from 'ant-design-vue';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import { deleteTask, deleteTaskList, exportTask, getTaskPage } from '#/api/mes/execution/batching-task';
import { $t } from '#/locales';

import { useGridColumns, useGridFormSchema } from './data';
import FormComponent from './modules/form.vue';

defineOptions({ name: 'MesBatchingTaskMaster' });

const [FormModal, formModalApi] = useVbenModal({ connectedComponent: FormComponent, destroyOnClose: true });

function handleRefresh() { gridApi.query(); }

function handleCreate() { formModalApi.setData({ type: 'create' }).open(); }
function handleEdit(row: MesBatchingTaskApi.Task) { formModalApi.setData({ type: 'edit', id: row.id }).open(); }
function handleDetail(row: MesBatchingTaskApi.Task) { formModalApi.setData({ type: 'detail', id: row.id }).open(); }

async function handleDelete(row: MesBatchingTaskApi.Task) {
  const hideLoading = message.loading({ content: '删除中...', duration: 0 });
  try {
    await deleteTask(row.id!);
    message.success('删除成功');
    handleRefresh();
  } finally { hideLoading(); }
}

async function handleDeleteBatch() {
  await confirm('确认要删除选中的任务吗？');
  const hideLoading = message.loading({ content: '批量删除中...', duration: 0 });
  try {
    await deleteTaskList(checkedIds.value);
    checkedIds.value = [];
    message.success('批量删除成功');
    handleRefresh();
  } finally { hideLoading(); }
}

const checkedIds = ref<number[]>([]);
function handleRowCheckboxChange({ records }: { records: MesBatchingTaskApi.Task[] }) {
  checkedIds.value = records.map((item) => item.id!);
}

async function handleExport() {
  const data = await exportTask(await gridApi.formApi.getValues());
  downloadFileFromBlobPart({ fileName: '配料任务单.xls', source: data });
}

const [BaseGrid, gridApi] = useVbenVxeGrid({
  formOptions: { schema: useGridFormSchema() },
  gridOptions: {
    columns: useGridColumns(),
    height: 'auto',
    keepSource: true,
    proxyConfig: {
      ajax: { query: async ({ page }, formValues) => await getTaskPage({ pageNo: page.currentPage, pageSize: page.pageSize, ...formValues }) },
    },
    rowConfig: { keyField: 'id', isHover: true },
    toolbarConfig: { refresh: true, search: true },
  } as VxeTableGridOptions<MesBatchingTaskApi.Task>,
  gridEvents: { checkboxAll: handleRowCheckboxChange, checkboxChange: handleRowCheckboxChange },
});
</script>

<template>
  <Page auto-content-height>
    <FormModal @success="handleRefresh" />
    <BaseGrid table-title="配料任务列表">
      <template #toolbar-tools>
        <TableAction
          :actions="[
            { label: '下发任务', type: 'primary', icon: ACTION_ICON.ADD, onClick: handleCreate },
            { label: '导出', type: 'primary', icon: ACTION_ICON.DOWNLOAD, onClick: handleExport },
            { label: '批量删除', type: 'primary', danger: true, icon: ACTION_ICON.DELETE, disabled: isEmpty(checkedIds), onClick: handleDeleteBatch },
          ]"
        />
      </template>
      <template #actions="{ row }">
        <TableAction
          :actions="[
            { label: $t('common.detail'), type: 'link', icon: ACTION_ICON.VIEW, onClick: handleDetail.bind(null, row) },
            { label: $t('common.edit'), type: 'link', icon: ACTION_ICON.EDIT, ifShow: () => row.status !== 30, onClick: handleEdit.bind(null, row) },
            { label: $t('common.delete'), type: 'link', danger: true, icon: ACTION_ICON.DELETE, popConfirm: { title: `确认删除任务 ${row.taskNo} 吗？`, confirm: handleDelete.bind(null, row) } },
          ]"
        />
      </template>
    </BaseGrid>
  </Page>
</template>
