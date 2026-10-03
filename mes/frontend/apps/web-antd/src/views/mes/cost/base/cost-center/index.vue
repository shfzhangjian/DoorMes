<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesCostCenterApi } from '#/api/mes/cost/base/cost-center';

import { ref } from 'vue';
import { Page, useVbenModal } from '@vben/common-ui';
import { downloadFileFromBlobPart } from '@vben/utils';
import { message } from 'ant-design-vue';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import { deleteCostCenter, exportCostCenter, getCostCenterList } from '#/api/mes/cost/base/cost-center';
import { $t } from '#/locales';

import { useGridColumns, useGridFormSchema } from './data';
import Form from './modules/form.vue';

const [FormModal, formModalApi] = useVbenModal({
  connectedComponent: Form,
  destroyOnClose: true,
});

const isExpanded = ref(true);
function handleExpand() {
  isExpanded.value = !isExpanded.value;
  gridApi.grid.setAllTreeExpand(isExpanded.value);
}

function handleRefresh() {
  gridApi.query();
}

function handleCreate() {
  formModalApi.setData({ parentId: 0 }).open();
}

function handleAppend(row: MesCostCenterApi.CostCenter) {
  formModalApi.setData({ parentId: row.id }).open();
}

function handleEdit(row: MesCostCenterApi.CostCenter) {
  formModalApi.setData(row).open();
}

async function handleDelete(row: MesCostCenterApi.CostCenter) {
  const hideLoading = message.loading({
    content: $t('ui.actionMessage.deleting', [row.name]),
    duration: 0,
  });
  try {
    await deleteCostCenter(row.id);
    message.success($t('ui.actionMessage.deleteSuccess', [row.name]));
    handleRefresh();
  } finally {
    hideLoading();
  }
}

async function handleExport() {
  const data = await exportCostCenter(await gridApi.formApi.getValues());
  downloadFileFromBlobPart({ fileName: '成本中心定义.xls', source: data });
}

const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: {
    schema: useGridFormSchema(),
  },
  gridOptions: {
    columns: useGridColumns(),
    height: 'auto',
    pagerConfig: { enabled: false }, // 树形结构通常不分页
    proxyConfig: {
      ajax: {
        query: async (_, formValues) => {
          return await getCostCenterList(formValues);
        },
      },
    },
    rowConfig: { keyField: 'id', isHover: true },
    toolbarConfig: { refresh: true, search: true },
    treeConfig: {
      parentField: 'parentId',
      rowField: 'id',
      transform: true,
      expandAll: true,
      reserve: true,
    },
  } as VxeTableGridOptions<MesCostCenterApi.CostCenter>,
});
</script>

<template>
  <Page auto-content-height>
    <FormModal @success="handleRefresh" />
    <Grid table-title="成本中心列表">
      <template #toolbar-tools>
        <TableAction
          :actions="[
            {
              label: '新增成本中心',
              type: 'primary',
              icon: ACTION_ICON.ADD,
              auth: ['mes:cost-center:create'],
              onClick: handleCreate,
            },
            {
              label: isExpanded ? '收缩' : '展开',
              type: 'primary',
              onClick: handleExpand,
            },
            {
              label: $t('ui.actionTitle.export'),
              type: 'primary',
              icon: ACTION_ICON.DOWNLOAD,
              auth: ['mes:cost-center:export'],
              onClick: handleExport,
            },
          ]"
        />
      </template>
      <template #actions="{ row }">
        <TableAction
          :actions="[
            {
              label: '新增下级',
              type: 'link',
              icon: ACTION_ICON.ADD,
              auth: ['mes:cost-center:create'],
              onClick: handleAppend.bind(null, row),
            },
            {
              label: $t('common.edit'),
              type: 'link',
              icon: ACTION_ICON.EDIT,
              auth: ['mes:cost-center:update'],
              onClick: handleEdit.bind(null, row),
            },
            {
              label: $t('common.delete'),
              type: 'link',
              danger: true,
              icon: ACTION_ICON.DELETE,
              auth: ['mes:cost-center:delete'],
              popConfirm: {
                title: `确认删除成本中心 [${row.name}] 吗？`,
                confirm: handleDelete.bind(null, row),
              },
            },
          ]"
        />
      </template>
    </Grid>
  </Page>
</template>
