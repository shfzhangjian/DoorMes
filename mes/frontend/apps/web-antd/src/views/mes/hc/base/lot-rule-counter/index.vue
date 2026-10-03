<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';

import { ref } from 'vue';

import { confirm, Page, useVbenModal } from '@vben/common-ui';
import { isEmpty } from '@vben/utils';
import { message } from 'ant-design-vue';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import { deleteLotRuleCounter, getLotRuleCounterPage } from '#/api/mes/hc/lotrule';

import { useGridColumns, useGridFormSchema } from './data';
import CounterFormModal from './modules/counter-form-modal.vue';
import CounterSequenceModal from './modules/counter-sequence-modal.vue';

const [CounterFormModalComp, counterFormModalApi] = useVbenModal({
  connectedComponent: CounterFormModal,
  destroyOnClose: true,
});

const [CounterSequenceModalComp, counterSequenceModalApi] = useVbenModal({
  connectedComponent: CounterSequenceModal,
  destroyOnClose: true,
});

function handleRefresh() {
  gridApi.query();
}

function handleCreate() {
  counterFormModalApi.setData({ mode: 'create' }).open();
}

function handleView(row: Record<string, any>) {
  counterFormModalApi.setData({ mode: 'view', record: row }).open();
}

function handleEdit(row: Record<string, any>) {
  counterFormModalApi.setData({ mode: 'edit', record: row }).open();
}

function handleInitialize() {
  counterSequenceModalApi.setData({ mode: 'initialize' }).open();
}

function handleAdjust(row: Record<string, any>) {
  counterSequenceModalApi.setData({ mode: 'adjust', record: row }).open();
}

async function handleDelete(row: Record<string, any>) {
  const hideLoading = message.loading({ content: '正在删除...', duration: 0 });
  try {
    await deleteLotRuleCounter(row.id);
    message.success('删除成功');
    handleRefresh();
  } finally {
    hideLoading();
  }
}

const checkedIds = ref<number[]>([]);
function handleRowCheckboxChange({ records }: { records: Array<Record<string, any>> }) {
  checkedIds.value = records.map((item) => item.id);
}

async function handleDeleteBatch() {
  await confirm('确认删除选中的实例台账吗？');
  const hideLoading = message.loading({ content: '正在批量删除...', duration: 0 });
  try {
    for (const id of checkedIds.value) {
      await deleteLotRuleCounter(id);
    }
    checkedIds.value = [];
    message.success('删除成功');
    handleRefresh();
  } finally {
    hideLoading();
  }
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
        query: async ({ page }, formValues) => {
          return await getLotRuleCounterPage({
            pageNo: page.currentPage,
            pageSize: page.pageSize,
            ...formValues,
          });
        },
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
  } as VxeTableGridOptions<Record<string, any>>,
  gridEvents: {
    checkboxAll: handleRowCheckboxChange,
    checkboxChange: handleRowCheckboxChange,
  },
});
</script>

<template>
  <Page auto-content-height>
    <CounterFormModalComp @success="handleRefresh" />
    <CounterSequenceModalComp @success="handleRefresh" />
    <Grid table-title="批次实例台账列表">
      <template #toolbar-tools>
        <TableAction
          :actions="[
            {
              label: '初始化流水',
              type: 'primary',
              auth: ['mes:md:lot-rule:update'],
              onClick: handleInitialize,
            },
            {
              label: '新增',
              type: 'primary',
              icon: ACTION_ICON.ADD,
              auth: ['mes:md:lot-rule:update'],
              onClick: handleCreate,
            },
            {
              label: '批量删除',
              type: 'primary',
              danger: true,
              icon: ACTION_ICON.DELETE,
              auth: ['mes:md:lot-rule:delete'],
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
              label: '详情',
              type: 'link',
              icon: ACTION_ICON.VIEW,
              auth: ['mes:md:lot-rule:query'],
              onClick: handleView.bind(null, row),
            },
            {
              label: '编辑',
              type: 'link',
              icon: ACTION_ICON.EDIT,
              auth: ['mes:md:lot-rule:update'],
              onClick: handleEdit.bind(null, row),
            },
            {
              label: '人工设置',
              type: 'link',
              auth: ['mes:md:lot-rule:update'],
              onClick: handleAdjust.bind(null, row),
            },
            {
              label: '删除',
              type: 'link',
              danger: true,
              icon: ACTION_ICON.DELETE,
              auth: ['mes:md:lot-rule:delete'],
              popConfirm: {
                title: '确认删除当前实例台账吗？',
                confirm: handleDelete.bind(null, row),
              },
            },
          ]"
        />
      </template>
    </Grid>
  </Page>
</template>
