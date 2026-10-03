<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { QmsMeasureToolApi } from '#/api/mes/quality/measure-tool';

import { confirm, Page, useVbenModal } from '@vben/common-ui';

import { message } from 'ant-design-vue';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  deleteMeasureToolCategory,
  getMeasureToolCategoryList,
} from '#/api/mes/quality/measure-tool';

import { useGridColumns, useGridFormSchema } from './data';
import FormComponent from './modules/form.vue';

defineOptions({ name: 'MesQmsMeasureToolCategory' });

const [FormModal, formModalApi] = useVbenModal({
  connectedComponent: FormComponent,
  destroyOnClose: true,
});

function handleRefresh() {
  gridApi.query();
}

function handleCreate(parentId = 0) {
  formModalApi.setData({ parentId }).open();
}

function handleEdit(row: QmsMeasureToolApi.Category) {
  formModalApi.setData(row).open();
}

async function handleDelete(row: QmsMeasureToolApi.Category) {
  await confirm(`确认删除量检设备位置/区域 [${row.categoryName}] 吗？`);
  const hideLoading = message.loading({ content: '删除中...', duration: 0 });
  try {
    await deleteMeasureToolCategory(row.id!);
    message.success('删除成功');
    handleRefresh();
  } finally {
    hideLoading();
  }
}

const [BaseGrid, gridApi] = useVbenVxeGrid({
  class: 'measure-tool-category-vben-grid',
  gridClass: 'measure-tool-category-vxe-grid',
  formOptions: { schema: useGridFormSchema() },
  gridOptions: {
    columns: useGridColumns(),
    height: 'auto',
    keepSource: true,
    pagerConfig: { enabled: false },
    proxyConfig: {
      ajax: {
        query: async (_, formValues) => {
          return {
            list: await getMeasureToolCategoryList({
              ...formValues,
              status: 1,
            }),
          };
        },
      },
    },
    rowConfig: { isHover: true, keyField: 'id' },
    toolbarConfig: { refresh: true, search: true },
    treeConfig: {
      expandAll: true,
      line: true,
      parentField: 'parentId',
      rowField: 'id',
      transform: true,
    },
  } as VxeTableGridOptions<QmsMeasureToolApi.Category>,
});
</script>

<template>
  <Page auto-content-height>
    <FormModal @success="handleRefresh" />
    <BaseGrid table-title="量检设备位置">
      <template #toolbar-tools>
        <TableAction
          :actions="[
            {
              label: '新增位置',
              type: 'primary',
              icon: ACTION_ICON.ADD,
              auth: ['mes:qms-measure-tool-category:create'],
              onClick: () => handleCreate(0),
            },
          ]"
        />
      </template>

      <template #actions="{ row }">
        <TableAction
          :actions="[
            {
              label: '新增区域',
              type: 'link',
              icon: ACTION_ICON.ADD,
              auth: ['mes:qms-measure-tool-category:create'],
              ifShow: !row.parentId || row.parentId <= 0,
              onClick: () => handleCreate(row.id),
            },
            {
              label: '编辑',
              type: 'link',
              icon: ACTION_ICON.EDIT,
              auth: ['mes:qms-measure-tool-category:update'],
              onClick: () => handleEdit(row),
            },
            {
              label: '删除',
              type: 'link',
              danger: true,
              icon: ACTION_ICON.DELETE,
              auth: ['mes:qms-measure-tool-category:delete'],
              onClick: () => handleDelete(row),
            },
          ]"
        />
      </template>
    </BaseGrid>
  </Page>
</template>

<style scoped>
:deep(.measure-tool-category-vxe-grid .vxe-body--column) {
  vertical-align: middle;
}

:deep(
  .measure-tool-category-vxe-grid .vxe-body--column.col--fixed-right .cell
) {
  overflow: visible;
  white-space: nowrap;
}
</style>
