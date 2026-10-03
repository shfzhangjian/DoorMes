<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesHandoverHistoryApi } from '#/api/mes/report/handover-history';

import { Page, useVbenModal } from '@vben/common-ui';
import { Tag } from 'ant-design-vue';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import { getHistoryPage } from '#/api/mes/report/handover-history';
import { $t } from '#/locales';

import { useGridColumns, useGridFormSchema } from './data';
import FormComponent from './modules/form.vue';

defineOptions({ name: 'MesHandoverHistory' });

const [FormModal, formModalApi] = useVbenModal({ connectedComponent: FormComponent, destroyOnClose: true });

function handleDetail(row: MesHandoverHistoryApi.Record) {
  formModalApi.setData({ id: row.id }).open();
}

const [BaseGrid] = useVbenVxeGrid({
  formOptions: { schema: useGridFormSchema() },
  gridOptions: {
    columns: useGridColumns(),
    height: 'auto',
    keepSource: true,
    proxyConfig: {
      ajax: { query: async ({ page }, formValues) => await getHistoryPage({ pageNo: page.currentPage, pageSize: page.pageSize, ...formValues }) },
    },
    rowConfig: { keyField: 'id', isHover: true },
    toolbarConfig: { refresh: true, search: true },
  } as VxeTableGridOptions<MesHandoverHistoryApi.Record>,
});
</script>

<template>
  <Page auto-content-height>
    <FormModal />
    <BaseGrid table-title="交接班历史追溯">
      <template #status="{ row }">
        <Tag :color="row.status === 'COMPLETED' ? 'success' : 'error'">
          {{ row.status === 'COMPLETED' ? '正常完成' : '异常报备' }}
        </Tag>
      </template>

      <template #actions="{ row }">
        <TableAction
          :actions="[
            { label: '查看明细', type: 'link', icon: ACTION_ICON.VIEW, onClick: handleDetail.bind(null, row) },
          ]"
        />
      </template>
    </BaseGrid>
  </Page>
</template>
