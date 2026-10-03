<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesHcFinishedGlueBoardMapApi } from '#/api/mes/hc/finishedglueboardmap';

import { ref } from 'vue';

import { confirm, Page, useVbenModal } from '@vben/common-ui';
import { downloadFileFromBlobPart, isEmpty } from '@vben/utils';
import { message } from 'ant-design-vue';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  deleteFinishedGlueBoardMap,
  deleteFinishedGlueBoardMapList,
  exportFinishedGlueBoardMap,
  getFinishedGlueBoardMapPage,
} from '#/api/mes/hc/finishedglueboardmap';

import { useGridColumns, useGridFormSchema } from './data';
import Form from './modules/form.vue';

const [FormModal, formModalApi] = useVbenModal({
  connectedComponent: Form,
  destroyOnClose: true,
});

const checkedIds = ref<number[]>([]);

function handleRefresh() {
  gridApi.query();
}

function handleCreate() {
  formModalApi.setData(null).open();
}

function handleEdit(row: MesHcFinishedGlueBoardMapApi.FinishedGlueBoardMap) {
  formModalApi.setData(row).open();
}

async function handleDelete(row: MesHcFinishedGlueBoardMapApi.FinishedGlueBoardMap) {
  const hideLoading = message.loading({ content: '正在删除...', duration: 0 });
  try {
    await deleteFinishedGlueBoardMap(row.id!);
    message.success('删除成功');
    handleRefresh();
  } finally {
    hideLoading();
  }
}

async function handleDeleteBatch() {
  await confirm('确认删除选中的成品胶板对照吗？');
  const hideLoading = message.loading({ content: '正在批量删除...', duration: 0 });
  try {
    await deleteFinishedGlueBoardMapList(checkedIds.value);
    checkedIds.value = [];
    message.success('删除成功');
    handleRefresh();
  } finally {
    hideLoading();
  }
}

function handleRowCheckboxChange({ records }: { records: MesHcFinishedGlueBoardMapApi.FinishedGlueBoardMap[] }) {
  checkedIds.value = records.map((item) => item.id!).filter(Boolean);
}

async function handleExport() {
  const data = await exportFinishedGlueBoardMap(await gridApi.formApi.getValues());
  downloadFileFromBlobPart({ fileName: '成品胶板对照表.xls', source: data });
}

const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: {
    schema: useGridFormSchema(),
  },
  gridOptions: {
    columns: useGridColumns(),
    height: 'auto',
    keepSource: true,
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) =>
          await getFinishedGlueBoardMapPage({
            pageNo: page.currentPage,
            pageSize: page.pageSize,
            ...formValues,
          }),
      },
    },
    rowConfig: {
      keyField: 'id',
      isHover: true,
    },
    toolbarConfig: {
      refresh: true,
      search: true,
    },
  } as VxeTableGridOptions<MesHcFinishedGlueBoardMapApi.FinishedGlueBoardMap>,
  gridEvents: {
    checkboxAll: handleRowCheckboxChange,
    checkboxChange: handleRowCheckboxChange,
  },
});
</script>

<template>
  <Page auto-content-height>
    <FormModal @success="handleRefresh" />
    <Grid table-title="成品胶板对照表">
      <template #toolbar-tools>
        <TableAction
          :actions="[
            {
              label: '新增',
              type: 'primary',
              icon: ACTION_ICON.ADD,
              auth: ['mes:md:finished-glue-board-map:create'],
              onClick: handleCreate,
            },
            {
              label: '导出',
              type: 'primary',
              icon: ACTION_ICON.DOWNLOAD,
              auth: ['mes:md:finished-glue-board-map:export'],
              onClick: handleExport,
            },
            {
              label: '批量删除',
              type: 'primary',
              danger: true,
              icon: ACTION_ICON.DELETE,
              auth: ['mes:md:finished-glue-board-map:delete'],
              disabled: isEmpty(checkedIds),
              onClick: handleDeleteBatch,
            },
          ]"
        />
      </template>
      <template #actions="{ row }">
        <TableAction
          :actions="[
            {
              label: '编辑',
              type: 'link',
              icon: ACTION_ICON.EDIT,
              auth: ['mes:md:finished-glue-board-map:update'],
              onClick: handleEdit.bind(null, row),
            },
            {
              label: '删除',
              type: 'link',
              danger: true,
              icon: ACTION_ICON.DELETE,
              auth: ['mes:md:finished-glue-board-map:delete'],
              popConfirm: {
                title: '确认删除当前成品胶板对照吗？',
                confirm: handleDelete.bind(null, row),
              },
            },
          ]"
        />
      </template>
    </Grid>
  </Page>
</template>
