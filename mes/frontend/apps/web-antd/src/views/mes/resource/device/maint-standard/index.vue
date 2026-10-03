<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesMaintStandardApi } from '#/api/mes/resource/device/maint-standard';

import { ref } from 'vue';
import { confirm, Page, useVbenModal } from '@vben/common-ui';
import { downloadFileFromBlobPart, isEmpty } from '@vben/utils';
import { message } from 'ant-design-vue';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import { deleteStandardList, exportStandard, getStandardPage } from '#/api/mes/resource/device/maint-standard';
import { $t } from '#/locales';

import { useGridColumns, useGridFormSchema } from './data';
import FormComponent from './modules/form.vue';

defineOptions({ name: 'MesMaintStandard' });

const [FormModal, formModalApi] = useVbenModal({ connectedComponent: FormComponent, destroyOnClose: true });

function handleRefresh() { gridApi.query(); }

function handleCreate() { formModalApi.setData({ type: 'create' }).open(); }
function handleEdit(row: MesMaintStandardApi.Standard) { formModalApi.setData({ type: 'edit', id: row.id }).open(); }
function handleDetail(row: MesMaintStandardApi.Standard) { formModalApi.setData({ type: 'detail', id: row.id }).open(); }

async function handleDeleteBatch() {
  await confirm('确认要删除选中的检查标准吗？');
  const hideLoading = message.loading({ content: '删除中...', duration: 0 });
  try {
    await deleteStandardList(checkedIds.value);
    checkedIds.value = [];
    message.success('删除成功');
    handleRefresh();
  } finally { hideLoading(); }
}

async function handleDelete(row: MesMaintStandardApi.Standard) {
  await confirm(`确认删除保养标准 [${row.name || row.code}] 吗？`);
  const hideLoading = message.loading({ content: '删除中...', duration: 0 });
  try {
    await deleteStandardList([row.id!]);
    message.success('删除成功');
    handleRefresh();
  } finally { hideLoading(); }
}

const checkedIds = ref<number[]>([]);
function handleRowCheckboxChange({ records }: { records: MesMaintStandardApi.Standard[] }) {
  checkedIds.value = records.map((item) => item.id!);
}

async function handleExport() {
  const data = await exportStandard(await gridApi.formApi.getValues());
  downloadFileFromBlobPart({ fileName: '设备保养标准.xls', source: data });
}

const [BaseGrid, gridApi] = useVbenVxeGrid({
  formOptions: { schema: useGridFormSchema() },
  gridOptions: {
    columns: useGridColumns(),
    height: 'auto',
    keepSource: true,
    proxyConfig: {
      ajax: { query: async ({ page }, formValues) => await getStandardPage({ pageNo: page.currentPage, pageSize: page.pageSize, ...formValues }) },
    },
    rowConfig: { keyField: 'id', isHover: true },
    toolbarConfig: { refresh: true, search: true },
  } as VxeTableGridOptions<MesMaintStandardApi.Standard>,
  gridEvents: { checkboxAll: handleRowCheckboxChange, checkboxChange: handleRowCheckboxChange },
});
</script>

<template>
  <Page auto-content-height>
    <FormModal @success="handleRefresh" />
    <BaseGrid table-title="设备保养清扫标准">
      <template #toolbar-tools>
        <TableAction
          :actions="[
            { label: '新增标准', type: 'primary', icon: ACTION_ICON.ADD, auth: ['mes:resource-device-maint-standard:create'], onClick: handleCreate },
            { label: '导出', type: 'primary', icon: ACTION_ICON.DOWNLOAD, auth: ['mes:resource-device-maint-standard:export'], onClick: handleExport },
            { label: '批量删除', type: 'primary', danger: true, icon: ACTION_ICON.DELETE, auth: ['mes:resource-device-maint-standard:delete'], disabled: isEmpty(checkedIds), onClick: handleDeleteBatch },
          ]"
        />
      </template>
      <template #actions="{ row }">
        <TableAction
          :actions="[
            { label: $t('common.detail'), type: 'link', icon: ACTION_ICON.VIEW, onClick: handleDetail.bind(null, row) },
            { label: $t('common.edit'), type: 'link', icon: ACTION_ICON.EDIT, auth: ['mes:resource-device-maint-standard:update'], onClick: handleEdit.bind(null, row) },
            { label: $t('common.delete'), type: 'link', danger: true, icon: ACTION_ICON.DELETE, auth: ['mes:resource-device-maint-standard:delete'], onClick: handleDelete.bind(null, row) },
          ]"
        />
      </template>
    </BaseGrid>
  </Page>
</template>
