<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesHcInvTxnApi } from '#/api/mes/hc/inv/txn';

import { Page } from '@vben/common-ui';
import { downloadFileFromBlobPart } from '@vben/utils';
import { Tag } from 'ant-design-vue';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import { exportInvTxnLog, getInvTxnLogPage } from '#/api/mes/hc/inv/txn';

import { TXN_TYPE_COLOR, TXN_TYPE_LABEL, useGridColumns, useGridFormSchema } from './data';

const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: {
    schema: useGridFormSchema(),
    collapsed: true,
    showCollapseButton: true,
  },
  gridOptions: {
    columns: useGridColumns(),
    height: 'auto',
    keepSource: true,
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) => {
          return await getInvTxnLogPage({
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
  } as VxeTableGridOptions<MesHcInvTxnApi.TxnLog>,
});

async function handleExport() {
  const formValues = await gridApi.formApi.getValues();
  const data = await exportInvTxnLog(formValues);
  downloadFileFromBlobPart({ fileName: '库存流水.xls', source: data });
}
</script>

<template>
  <Page auto-content-height>
    <Grid table-title="库存流水列表">
      <template #toolbar-tools>
        <TableAction
          :actions="[
            {
              label: '导出',
              type: 'primary',
              icon: ACTION_ICON.DOWNLOAD,
              auth: ['mes:inv:txn:export'],
              onClick: handleExport,
            },
          ]"
        />
      </template>

      <template #txnType="{ row }">
        <Tag :color="TXN_TYPE_COLOR[row.txnType]">
          {{ TXN_TYPE_LABEL[row.txnType] ?? row.txnType }}
        </Tag>
      </template>

      <template #txnQty="{ row }">
        <span :style="{ color: row.txnQty < 0 ? '#ff4d4f' : 'inherit' }">
          {{ row.txnQty != null ? Number(row.txnQty).toFixed(2) : '-' }}
        </span>
      </template>

      <template #txnNo="{ row }">
        <span>{{ row.txnNo }}</span>
      </template>
    </Grid>
  </Page>
</template>
