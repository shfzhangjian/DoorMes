<script lang="ts" setup>
import { Page, useVbenModal } from '@vben/common-ui';
import { Tag } from 'ant-design-vue';
import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import { getEdcPage } from '#/api/mes/execution/edc-record';
import { useGridColumns, useGridFormSchema } from './data';
import FormComponent from './modules/form.vue';

defineOptions({ name: 'MesEdcRecord' });

const [FormModal, formModalApi] = useVbenModal({
  connectedComponent: FormComponent,
  destroyOnClose: true
});

const [BaseGrid, gridApi] = useVbenVxeGrid({
  gridOptions: {
    columns: useGridColumns(),
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) =>
          await getEdcPage({ pageNo: page.currentPage, pageSize: page.pageSize, ...formValues }),
      },
    },
    rowConfig: { keyField: 'id', isHover: true },
  },
  formOptions: { schema: useGridFormSchema() },
});

const handleDetail = (row: any) => formModalApi.setData({ id: row.id }).open();
</script>

<template>
  <Page auto-content-height>
    <BaseGrid table-title="现场数据采集追溯">
      <template #actualValue="{ row }">
        <span :class="row.result === 'FAIL' ? 'text-red-500 font-bold' : 'text-green-600'">
          {{ row.actualValue }} {{ row.unit }}
        </span>
      </template>

      <template #result="{ row }">
        <Tag :color="row.result === 'PASS' ? 'success' : 'error'">
          {{ row.result === 'PASS' ? '合格' : '异常' }}
        </Tag>
      </template>

      <template #actions="{ row }">
        <TableAction
          :actions="[
            { label: '详情', type: 'link', icon: ACTION_ICON.VIEW, onClick: handleDetail.bind(null, row) },
          ]"
        />
      </template>
    </BaseGrid>
    <FormModal />
  </Page>
</template>
