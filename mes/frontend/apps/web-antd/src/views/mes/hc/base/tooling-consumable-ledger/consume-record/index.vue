<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesHcToolingConsumableLedgerApi } from '#/api/mes/hc/tooling-consumable-ledger';

import { Page } from '@vben/common-ui';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { getToolingConsumableConsumePage } from '#/api/mes/hc/tooling-consumable-ledger';

import {
  useConsumeRecordGridColumns,
  useConsumeRecordGridFormSchema,
} from '../data';

defineOptions({ name: 'MesHcBaseToolingConsumableConsumeRecord' });

const [Grid] = useVbenVxeGrid({
  formOptions: {
    schema: useConsumeRecordGridFormSchema(),
  },
  gridOptions: {
    columns: useConsumeRecordGridColumns(),
    height: 'auto',
    keepSource: true,
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) =>
          await getToolingConsumableConsumePage({
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
  } as VxeTableGridOptions<MesHcToolingConsumableLedgerApi.Consume>,
});
</script>

<template>
  <Page auto-content-height>
    <Grid table-title="边库耗材消耗记录" />
  </Page>
</template>
