<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesHcToolingConsumableLedgerApi } from '#/api/mes/hc/tooling-consumable-ledger';

import { ref } from 'vue';

import { confirm, Page, useVbenModal } from '@vben/common-ui';
import { isEmpty } from '@vben/utils';

import { message } from 'ant-design-vue';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  deleteToolingProcessConsumable,
  deleteToolingProcessConsumableList,
  getToolingProcessConsumablePage,
} from '#/api/mes/hc/tooling-consumable-ledger';

import { useConfigGridColumns, useConfigGridFormSchema } from './data';
import ConfigForm from './modules/config-form.vue';

const checkedIds = ref<number[]>([]);

const [ConfigFormModal, configFormModalApi] = useVbenModal({
  connectedComponent: ConfigForm,
  destroyOnClose: true,
});

function handleRefresh() {
  gridApi.query();
}

function handleCreate() {
  configFormModalApi.setData(null).open();
}

function handleEdit(row: MesHcToolingConsumableLedgerApi.ProcessConsumable) {
  configFormModalApi.setData(row).open();
}

async function handleDelete(
  row: MesHcToolingConsumableLedgerApi.ProcessConsumable,
) {
  const hideLoading = message.loading({ content: '正在删除...', duration: 0 });
  try {
    await deleteToolingProcessConsumable(row.id!);
    checkedIds.value = checkedIds.value.filter((id) => id !== row.id);
    message.success('删除成功');
    handleRefresh();
  } finally {
    hideLoading();
  }
}

async function handleDeleteBatch() {
  await confirm('确认删除选中的工序耗材配置吗？');
  const hideLoading = message.loading({
    content: '正在批量删除...',
    duration: 0,
  });
  try {
    await deleteToolingProcessConsumableList(checkedIds.value);
    checkedIds.value = [];
    message.success('删除成功');
    handleRefresh();
  } finally {
    hideLoading();
  }
}

function handleRowCheckboxChange({
  records,
}: {
  records: MesHcToolingConsumableLedgerApi.ProcessConsumable[];
}) {
  checkedIds.value = records.map((item) => item.id!).filter(Boolean);
}

const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: {
    schema: useConfigGridFormSchema(),
  },
  gridOptions: {
    columns: useConfigGridColumns(),
    height: 'auto',
    keepSource: true,
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) =>
          await getToolingProcessConsumablePage({
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
  } as VxeTableGridOptions<MesHcToolingConsumableLedgerApi.ProcessConsumable>,
  gridEvents: {
    checkboxAll: handleRowCheckboxChange,
    checkboxChange: handleRowCheckboxChange,
  },
});
</script>

<template>
  <Page auto-content-height>
    <ConfigFormModal @success="handleRefresh" />
    <Grid table-title="工序耗材维护">
      <template #toolbar-tools>
        <TableAction
          :actions="[
            {
              label: '新增',
              type: 'primary',
              icon: ACTION_ICON.ADD,
              onClick: handleCreate,
            },
            {
              label: '批量删除',
              type: 'primary',
              danger: true,
              icon: ACTION_ICON.DELETE,
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
              onClick: handleEdit.bind(null, row),
            },
            {
              label: '删除',
              type: 'link',
              danger: true,
              icon: ACTION_ICON.DELETE,
              popConfirm: {
                title: '确认删除当前工序耗材配置吗？',
                confirm: handleDelete.bind(null, row),
              },
            },
          ]"
        />
      </template>
    </Grid>
  </Page>
</template>
