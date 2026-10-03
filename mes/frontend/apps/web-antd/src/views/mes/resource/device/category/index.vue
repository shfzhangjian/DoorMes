<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesDeviceCategoryApi } from '#/api/mes/resource/device/category';

import { confirm, Page, useVbenModal } from '@vben/common-ui';

import { message, Tag } from 'ant-design-vue';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  deleteCategory,
  getCategoryList,
} from '#/api/mes/resource/device/category';

import { useGridColumns, useGridFormSchema } from './data';
import FormComponent from './modules/form.vue';
import {
  DEVICE_CATEGORY_STATUS_OPTIONS,
  optionColor,
  optionLabel,
} from '../shared';

defineOptions({ name: 'MesResourceDeviceCategory' });

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

function handleEdit(row: MesDeviceCategoryApi.Category) {
  formModalApi.setData(row).open();
}

async function handleDelete(row: MesDeviceCategoryApi.Category) {
  await confirm(`确认删除设备分类 [${row.categoryName}] 吗？`);
  const hideLoading = message.loading({ content: '删除中...', duration: 0 });
  try {
    await deleteCategory(row.id!);
    message.success('删除成功');
    handleRefresh();
  } finally {
    hideLoading();
  }
}

const [BaseGrid, gridApi] = useVbenVxeGrid({
  class: 'resource-device-category-vben-grid',
  gridClass: 'resource-device-category-vxe-grid',
  formOptions: { schema: useGridFormSchema() },
  gridOptions: {
    columns: useGridColumns(),
    height: 'auto',
    keepSource: true,
    pagerConfig: { enabled: false },
    proxyConfig: {
      ajax: {
        query: async (_, formValues) => ({ list: await getCategoryList(formValues) }),
      },
    },
    rowConfig: { isHover: true, keyField: 'id' },
    toolbarConfig: { refresh: true, search: true },
    treeConfig: { expandAll: true, line: true, parentField: 'parentId', rowField: 'id', transform: true },
  } as VxeTableGridOptions<MesDeviceCategoryApi.Category>,
});
</script>

<template>
  <Page auto-content-height>
    <FormModal @success="handleRefresh" />
    <BaseGrid table-title="设备分类维护">
      <template #toolbar-tools>
        <TableAction
          :actions="[
            {
              label: '新增一级分类',
              type: 'primary',
              icon: ACTION_ICON.ADD,
              auth: ['mes:resource-device-category:create'],
              onClick: () => handleCreate(0),
            },
          ]"
        />
      </template>

      <template #status="{ row }">
        <Tag :color="optionColor(DEVICE_CATEGORY_STATUS_OPTIONS, row.status)">
          {{ optionLabel(DEVICE_CATEGORY_STATUS_OPTIONS, row.status) }}
        </Tag>
      </template>

      <template #actions="{ row }">
        <TableAction
          :actions="[
            {
              label: '新增子分类',
              type: 'link',
              icon: ACTION_ICON.ADD,
              auth: ['mes:resource-device-category:create'],
              onClick: () => handleCreate(row.id),
            },
            {
              label: '编辑',
              type: 'link',
              icon: ACTION_ICON.EDIT,
              auth: ['mes:resource-device-category:update'],
              onClick: () => handleEdit(row),
            },
            {
              label: '删除',
              type: 'link',
              danger: true,
              icon: ACTION_ICON.DELETE,
              auth: ['mes:resource-device-category:delete'],
              onClick: () => handleDelete(row),
            },
          ]"
        />
      </template>
    </BaseGrid>
  </Page>
</template>

<style scoped>
:deep(.resource-device-category-vxe-grid .vxe-body--column) {
  vertical-align: middle;
}
</style>
