<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesHcLotRuleApi } from '#/api/mes/hc/lotrule';

import { ref } from 'vue';

import { confirm, Page, useVbenModal } from '@vben/common-ui';
import { downloadFileFromBlobPart, isEmpty } from '@vben/utils';
import { message } from 'ant-design-vue';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  copyLotRuleAsNewVersion,
  deleteLotRule,
  deleteLotRuleList,
  disableLotRule,
  exportLotRule,
  getLotRulePage,
  publishLotRule,
} from '#/api/mes/hc/lotrule';

import { useGridColumns, useGridFormSchema } from './data';
import CounterModal from './modules/counter-modal.vue';
import Form from './modules/form.vue';

const [FormModal, formModalApi] = useVbenModal({
  connectedComponent: Form,
  destroyOnClose: true,
});

const [CounterListModal, counterListModalApi] = useVbenModal({
  connectedComponent: CounterModal,
  destroyOnClose: true,
});

function handleRefresh() {
  gridApi.query();
}

function handleCreate() {
  formModalApi.setData(null).open();
}

function handleEdit(row: MesHcLotRuleApi.LotRule) {
  formModalApi.setData(row).open();
}

function handleCounterList(row: MesHcLotRuleApi.LotRule) {
  counterListModalApi.setData(row).open();
}

async function handleDelete(row: MesHcLotRuleApi.LotRule) {
  const hideLoading = message.loading({ content: '正在删除...', duration: 0 });
  try {
    await deleteLotRule(row.id!);
    message.success('删除成功');
    handleRefresh();
  } finally {
    hideLoading();
  }
}

async function handlePublish(row: MesHcLotRuleApi.LotRule) {
  await publishLotRule(row.id!);
  message.success('规则已发布并启用');
  handleRefresh();
}

async function handleDisable(row: MesHcLotRuleApi.LotRule) {
  await disableLotRule(row.id!);
  message.success('规则已停用');
  handleRefresh();
}

async function handleCopyNewVersion(row: MesHcLotRuleApi.LotRule) {
  await copyLotRuleAsNewVersion(row.id!);
  message.success('已复制为草稿新版本');
  handleRefresh();
}

async function handleDeleteBatch() {
  await confirm('确认删除选中的规则吗？');
  const hideLoading = message.loading({ content: '正在批量删除...', duration: 0 });
  try {
    await deleteLotRuleList(checkedIds.value);
    checkedIds.value = [];
    message.success('删除成功');
    handleRefresh();
  } finally {
    hideLoading();
  }
}

const checkedIds = ref<number[]>([]);

function handleRowCheckboxChange({ records }: { records: MesHcLotRuleApi.LotRule[] }) {
  checkedIds.value = records.map((item) => item.id || 0).filter(Boolean);
}

async function handleExport() {
  const data = await exportLotRule(await gridApi.formApi.getValues());
  downloadFileFromBlobPart({ fileName: '批号规则.xls', source: data });
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
          await getLotRulePage({
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
  } as VxeTableGridOptions<MesHcLotRuleApi.LotRule>,
  gridEvents: {
    checkboxAll: handleRowCheckboxChange,
    checkboxChange: handleRowCheckboxChange,
  },
});
</script>

<template>
  <Page auto-content-height>
    <FormModal @success="handleRefresh" />
    <CounterListModal @success="handleRefresh" />
    <Grid table-title="批号规则列表">
      <template #toolbar-tools>
        <TableAction
          :actions="[
            {
              label: '新增',
              type: 'primary',
              icon: ACTION_ICON.ADD,
              auth: ['mes:md:lot-rule:create'],
              onClick: handleCreate,
            },
            {
              label: '导出',
              type: 'primary',
              icon: ACTION_ICON.DOWNLOAD,
              auth: ['mes:md:lot-rule:export'],
              onClick: handleExport,
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
              label: '编辑',
              type: 'link',
              icon: ACTION_ICON.EDIT,
              auth: ['mes:md:lot-rule:update'],
              onClick: handleEdit.bind(null, row),
            },
            {
              label: '复制新版本',
              type: 'link',
              icon: ACTION_ICON.COPY,
              onClick: handleCopyNewVersion.bind(null, row),
            },
            ...(row.status === 1
              ? [{
                  label: '停用',
                  type: 'link' as const,
                  danger: true,
                  onClick: handleDisable.bind(null, row),
                }]
              : [{
                  label: '发布',
                  type: 'link' as const,
                  onClick: handlePublish.bind(null, row),
                }]),
            {
              label: '流水设置',
              type: 'link',
              icon: ACTION_ICON.EDIT,
              auth: ['mes:md:lot-rule:query'],
              onClick: handleCounterList.bind(null, row),
            },
            {
              label: '删除',
              type: 'link',
              danger: true,
              icon: ACTION_ICON.DELETE,
              auth: ['mes:md:lot-rule:delete'],
              popConfirm: {
                title: '确认删除当前规则吗？',
                confirm: handleDelete.bind(null, row),
              },
            },
          ]"
        />
      </template>
    </Grid>
  </Page>
</template>
