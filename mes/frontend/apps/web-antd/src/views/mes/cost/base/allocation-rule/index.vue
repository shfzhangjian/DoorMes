<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesCostRuleApi } from '#/api/mes/cost/base/allocation-rule';

import { Page, useVbenModal } from '@vben/common-ui';
import { downloadFileFromBlobPart } from '@vben/utils';
import { message } from 'ant-design-vue';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
// 注意这里引入的方法名改成了 getCostRulePage
import { deleteCostRule, exportCostRule, getCostRulePage } from '#/api/mes/cost/base/allocation-rule';
import { $t } from '#/locales';

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
  formModalApi.setData({}).open();
}

function handleEdit(row: MesCostRuleApi.Rule) {
  formModalApi.setData(row).open();
}

async function handleDelete(row: MesCostRuleApi.Rule) {
  const hideLoading = message.loading({
    content: $t('ui.actionMessage.deleting', [row.ruleName]),
    duration: 0,
  });
  try {
    await deleteCostRule(row.id);
    message.success($t('ui.actionMessage.deleteSuccess', [row.ruleName]));
    handleRefresh();
  } finally {
    hideLoading();
  }
}

async function handleExport() {
  const data = await exportCostRule(await gridApi.formApi.getValues());
  downloadFileFromBlobPart({ fileName: '分摊规则配置.xls', source: data });
}

const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: {
    schema: useGridFormSchema(),
  },
  gridOptions: {
    columns: useGridColumns(),
    height: 'auto',
    keepSource: true, // 【修复】：参考源码添加
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) => {
          // 【核心修复】：严格使用 pageNo 和 pageSize
          return await getCostRulePage({
            pageNo: page.currentPage,
            pageSize: page.pageSize,
            ...formValues,
          });
        },
      },
    },
    rowConfig: { keyField: 'id', isHover: true },
    toolbarConfig: { refresh: true, search: true },
  } as VxeTableGridOptions<MesCostRuleApi.Rule>,
});
</script>

<template>
  <Page auto-content-height>
    <FormModal @success="handleRefresh" />
    <Grid table-title="分摊规则配置列表">
      <template #toolbar-tools>
        <TableAction
          :actions="[
            {
              label: '新增分摊规则',
              type: 'primary',
              icon: ACTION_ICON.ADD,
              onClick: handleCreate,
            },
            {
              label: $t('ui.actionTitle.export'),
              type: 'primary',
              icon: ACTION_ICON.DOWNLOAD,
              onClick: handleExport,
            },
          ]"
        />
      </template>
      <template #actions="{ row }">
        <TableAction
          :actions="[
            {
              label: $t('common.edit'),
              type: 'link',
              icon: ACTION_ICON.EDIT,
              onClick: handleEdit.bind(null, row),
            },
            {
              label: $t('common.delete'),
              type: 'link',
              danger: true,
              icon: ACTION_ICON.DELETE,
              popConfirm: {
                title: `确认删除分摊规则 [${row.ruleName}] 吗？`,
                confirm: handleDelete.bind(null, row),
              },
            },
          ]"
        />
      </template>
    </Grid>
  </Page>
</template>
