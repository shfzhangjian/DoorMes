<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesAqlApi } from '#/api/mes/quality/base/aql';

import { ref } from 'vue';
import { confirm, Page, useVbenModal } from '@vben/common-ui';
import { isEmpty } from '@vben/utils';
import { message, Tag } from 'ant-design-vue';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import { deleteAqlList, getAqlPage } from '#/api/mes/quality/base/aql';

import { useGridColumns, useGridFormSchema } from './data';
import FormComponent from './modules/form.vue';

defineOptions({ name: 'MesQualityAql' });

const [FormModal, formModalApi] = useVbenModal({ connectedComponent: FormComponent, destroyOnClose: true });

function handleRefresh() { gridApi.query(); }

function handleCreate() { formModalApi.setData({ type: 'create' }).open(); }
function handleEdit(row: MesAqlApi.AqlStandard) { formModalApi.setData({ type: 'edit', id: row.id }).open(); }

async function handleDeleteBatch() {
  await confirm('确认删除选中的抽样方案吗？');
  try {
    await deleteAqlList(checkedIds.value);
    checkedIds.value = [];
    message.success('删除成功');
    handleRefresh();
  } catch (err) {}
}

const checkedIds = ref<number[]>([]);
function handleRowCheckboxChange({ records }: { records: MesAqlApi.AqlStandard[] }) {
  checkedIds.value = records.map((item) => item.id!);
}

const [BaseGrid, gridApi] = useVbenVxeGrid({
  formOptions: { schema: useGridFormSchema() },
  gridOptions: {
    columns: useGridColumns(),
    height: 'auto',
    proxyConfig: { ajax: { query: async ({ page }, formValues) => await getAqlPage({ pageNo: page.currentPage, pageSize: page.pageSize, ...formValues }) } },
    rowConfig: { keyField: 'id', isHover: true },
    toolbarConfig: { refresh: true, search: true },
  } as VxeTableGridOptions<MesAqlApi.AqlStandard>,
  gridEvents: { checkboxAll: handleRowCheckboxChange, checkboxChange: handleRowCheckboxChange },
});
</script>

<template>
  <Page auto-content-height>
    <FormModal @success="handleRefresh" />
    <BaseGrid table-title="抽样方案与判定规则管理">
      <template #toolbar-tools>
        <TableAction :actions="[{ label: '新增抽样方案', type: 'primary', icon: ACTION_ICON.ADD, onClick: handleCreate }, { label: '批量删除', type: 'primary', danger: true, icon: ACTION_ICON.DELETE, disabled: isEmpty(checkedIds), onClick: handleDeleteBatch }]" />
      </template>
      <template #status="{ row }">
        <Tag :color="row.status === 1 ? 'success' : 'error'">{{ row.status === 1 ? '启用' : '停用' }}</Tag>
      </template>
      <template #actions="{ row }">
        <TableAction :actions="[{ label: '编辑与规则配置', type: 'link', icon: ACTION_ICON.EDIT, onClick: () => handleEdit(row) }]" />
      </template>
    </BaseGrid>
  </Page>
</template>
