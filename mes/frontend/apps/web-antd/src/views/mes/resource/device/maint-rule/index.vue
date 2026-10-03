<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import { ref } from 'vue';
import { confirm, Page, useVbenModal } from '@vben/common-ui';
import { isEmpty } from '@vben/utils';
import { message } from 'ant-design-vue';
import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import { deleteRuleList, getRulePage } from '#/api/mes/resource/device/maint-rule';

import { useGridColumns, useGridFormSchema } from './data';
import FormComponent from './modules/form.vue';

defineOptions({ name: 'MesMaintRule' });

const [FormModal, formModalApi] = useVbenModal({ connectedComponent: FormComponent, destroyOnClose: true });

function handleRefresh() { gridApi.query(); }
function handleCreate() { formModalApi.setData({ type: 'create' }).open(); }
function handleEdit(row: any) { formModalApi.setData({ type: 'edit', id: row.id }).open(); }
function handleDetail(row: any) { formModalApi.setData({ type: 'detail', id: row.id }).open(); }

async function handleDeleteBatch() {
  await confirm('确认删除选中的维保规程吗？已生成的计划不受影响。');
  await deleteRuleList(checkedIds.value);
  checkedIds.value = [];
  message.success('删除成功');
  handleRefresh();
}

const checkedIds = ref<any[]>([]);
function handleRowCheckboxChange({ records }: { records: any[] }) { checkedIds.value = records.map((item) => item.id!); }

const [BaseGrid, gridApi] = useVbenVxeGrid({
  formOptions: { schema: useGridFormSchema() },
  gridOptions: {
    columns: useGridColumns(),
    height: 'auto',
    proxyConfig: { ajax: { query: async ({ page }, formValues) => await getRulePage({ pageNo: page.currentPage, pageSize: page.pageSize, ...formValues }) } },
    rowConfig: { keyField: 'id', isHover: true },
    toolbarConfig: { refresh: true, search: true },
  } as VxeTableGridOptions<any>,
  gridEvents: { checkboxAll: handleRowCheckboxChange, checkboxChange: handleRowCheckboxChange },
});
</script>

<template>
  <Page auto-content-height>
    <FormModal @success="handleRefresh" />
    <BaseGrid table-title="设备维保规程基准库">
      <template #toolbar-tools>
        <TableAction
          :actions="[
            { label: '新增规程', type: 'primary', icon: ACTION_ICON.ADD, onClick: handleCreate },
            { label: '批量删除', type: 'primary', danger: true, icon: ACTION_ICON.DELETE, disabled: isEmpty(checkedIds), onClick: handleDeleteBatch },
          ]"
        />
      </template>
      <template #actions="{ row }">
        <TableAction
          :actions="[
            { label: '详情', type: 'link', icon: ACTION_ICON.VIEW, onClick: handleDetail.bind(null, row) },
            { label: '编辑', type: 'link', icon: ACTION_ICON.EDIT, onClick: handleEdit.bind(null, row) },
          ]"
        />
      </template>
    </BaseGrid>
  </Page>
</template>
