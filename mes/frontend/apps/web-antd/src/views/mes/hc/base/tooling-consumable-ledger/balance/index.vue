<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesHcToolingConsumableLedgerApi } from '#/api/mes/hc/tooling-consumable-ledger';

import { computed } from 'vue';
import { useRoute } from 'vue-router';

import { Page, useVbenModal } from '@vben/common-ui';

import { message } from 'ant-design-vue';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import { getToolingConsumableBalancePage } from '#/api/mes/hc/tooling-consumable-ledger';

import {
  processText,
  resolveLedgerProcessCode,
  useBalanceGridColumns,
  useBalanceGridFormSchema,
} from '../data';
import ConsumeForm from '../modules/consume-form.vue';

const route = useRoute();
const fixedProcessCode = computed(() => resolveLedgerProcessCode(route));
const fixedProcessName = computed(() => processText(fixedProcessCode.value));
const tableTitle = computed(() =>
  fixedProcessName.value
    ? `${fixedProcessName.value}边库耗材余额`
    : '边库耗材余额查询',
);

const [ConsumeFormModal, consumeFormModalApi] = useVbenModal({
  connectedComponent: ConsumeForm,
  destroyOnClose: true,
});

function handleRefresh() {
  gridApi.query();
}

function handleRegisterConsume(row: MesHcToolingConsumableLedgerApi.Balance) {
  if (!row.ledgerId) {
    message.warning('当前余额记录缺少领用台账ID');
    return;
  }
  if ((row.balanceQty ?? 0) <= 0) {
    message.warning('当前批次已无边库余额');
    return;
  }
  consumeFormModalApi
    .setData({
      batchNo: row.batchNo,
      consumableType: row.consumableType,
      fixedProcessCode: fixedProcessCode.value,
      ledgerId: row.ledgerId,
      model: row.model,
      processCode: row.processCode,
    })
    .open();
}

const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: {
    schema: useBalanceGridFormSchema(fixedProcessCode.value),
  },
  gridOptions: {
    columns: useBalanceGridColumns(),
    height: 'auto',
    keepSource: true,
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) =>
          await getToolingConsumableBalancePage({
            pageNo: page.currentPage,
            pageSize: page.pageSize,
            ...formValues,
            processCode: fixedProcessCode.value || formValues.processCode,
          }),
      },
    },
    rowConfig: {
      keyField: 'ledgerId',
      isHover: true,
    },
    toolbarConfig: {
      refresh: true,
      search: true,
    },
  } as VxeTableGridOptions<MesHcToolingConsumableLedgerApi.Balance>,
});
</script>

<template>
  <Page auto-content-height>
    <ConsumeFormModal @success="handleRefresh" />
    <Grid :table-title="tableTitle">
      <template #actions="{ row }">
        <TableAction
          :actions="[
            {
              label: '登记消耗',
              type: 'link',
              icon: ACTION_ICON.ADD,
              disabled: (row.balanceQty ?? 0) <= 0,
              onClick: handleRegisterConsume.bind(null, row),
            },
          ]"
        />
      </template>
    </Grid>
  </Page>
</template>
