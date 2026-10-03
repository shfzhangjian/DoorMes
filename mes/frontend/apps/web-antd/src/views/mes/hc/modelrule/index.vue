<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesHcModelRuleApi } from '#/api/mes/hc/modelrule';

import { ref } from 'vue';

import { confirm, Page, useVbenModal } from '@vben/common-ui';
import { downloadFileFromBlobPart, isEmpty } from '@vben/utils';
import { message } from 'ant-design-vue';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import { deleteModelRule, deleteModelRuleList, exportModelRule, getModelRulePage } from '#/api/mes/hc/modelrule';

import { useGridColumns, useGridFormSchema } from './data';
import Form from './modules/form.vue';

const [FormModal, formModalApi] = useVbenModal({
  connectedComponent: Form,
  destroyOnClose: true,
});

function handleRefresh() {
  gridApi.query();
}

function handleCreate() {
  formModalApi.setData(null).open();
}

function handleEdit(row: MesHcModelRuleApi.ModelRule) {
  formModalApi.setData(row).open();
}

async function handleDelete(row: MesHcModelRuleApi.ModelRule) {
  const hideLoading = message.loading({ content: '正在删除...', duration: 0 });
  try {
    await deleteModelRule(row.id);
    message.success('删除成功');
    handleRefresh();
  } finally {
    hideLoading();
  }
}

async function handleDeleteBatch() {
  await confirm('确认删除选中的记录吗？');
  const hideLoading = message.loading({ content: '正在批量删除...', duration: 0 });
  try {
    await deleteModelRuleList(checkedIds.value);
    checkedIds.value = [];
    message.success('删除成功');
    handleRefresh();
  } finally {
    hideLoading();
  }
}

const checkedIds = ref<number[]>([]);
function handleRowCheckboxChange({ records }: { records: MesHcModelRuleApi.ModelRule[] }) {
  checkedIds.value = records.map((item) => item.id);
}

async function handleExport() {
  const data = await exportModelRule(await gridApi.formApi.getValues());
  downloadFileFromBlobPart({ fileName: '型号编码规则.xls', source: data });
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
          return await getModelRulePage({
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
  } as VxeTableGridOptions<MesHcModelRuleApi.ModelRule>,
  gridEvents: {
    checkboxAll: handleRowCheckboxChange,
    checkboxChange: handleRowCheckboxChange,
  },
});
</script>

<template>
  <Page auto-content-height>
    <FormModal @success="handleRefresh" />
    <Grid table-title="型号编码规则列表">
      <template #toolbar-tools>
        <TableAction
          :actions="[
            {
              label: '新增',
              type: 'primary',
              icon: ACTION_ICON.ADD,
              auth: ['mes:md:model-rule:create'],
              onClick: handleCreate,
            },
            {
              label: '导出',
              type: 'primary',
              icon: ACTION_ICON.DOWNLOAD,
              auth: ['mes:md:model-rule:export'],
              onClick: handleExport,
            },
            {
              label: '批量删除',
              type: 'primary',
              danger: true,
              icon: ACTION_ICON.DELETE,
              auth: ['mes:md:model-rule:delete'],
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
              auth: ['mes:md:model-rule:update'],
              onClick: handleEdit.bind(null, row),
            },
            {
              label: '删除',
              type: 'link',
              danger: true,
              icon: ACTION_ICON.DELETE,
              auth: ['mes:md:model-rule:delete'],
              popConfirm: {
                title: '确认删除当前记录吗？',
                confirm: handleDelete.bind(null, row),
              },
            },
          ]"
        />
      </template>
    </Grid>
  </Page>
</template>
