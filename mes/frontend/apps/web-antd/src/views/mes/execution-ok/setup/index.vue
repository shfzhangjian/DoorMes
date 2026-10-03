<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesSetupRuleApi } from '#/api/mes/execution/setup';

import { ref } from 'vue';
import { confirm, Page, useVbenModal } from '@vben/common-ui';
import { isEmpty } from '@vben/utils';
import { message, Tag } from 'ant-design-vue';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import { deleteRuleList, getRulePage } from '#/api/mes/execution/setup';
import { $t } from '#/locales';

import { useGridColumns, useGridFormSchema } from './data';
import FormComponent from './modules/form.vue';

defineOptions({ name: 'MesSetupVerify' });

const [FormModal, formModalApi] = useVbenModal({ connectedComponent: FormComponent, destroyOnClose: true });

function handleRefresh() { gridApi.query(); }

function handleCreate() { formModalApi.setData({ type: 'create' }).open(); }
function handleEdit(row: MesSetupRuleApi.Rule) { formModalApi.setData({ type: 'edit', id: row.id }).open(); }
function handleDetail(row: MesSetupRuleApi.Rule) { formModalApi.setData({ type: 'detail', id: row.id }).open(); }

async function handleDeleteBatch() {
  await confirm('确认要删除选中的防错规则吗？');
  const hideLoading = message.loading({ content: '删除中...', duration: 0 });
  try {
    await deleteRuleList(checkedIds.value);
    checkedIds.value = [];
    message.success('删除成功');
    handleRefresh();
  } finally { hideLoading(); }
}

const checkedIds = ref<number[]>([]);
function handleRowCheckboxChange({ records }: { records: MesSetupRuleApi.Rule[] }) {
  checkedIds.value = records.map((item) => item.id!);
}

const [BaseGrid, gridApi] = useVbenVxeGrid({
  formOptions: { schema: useGridFormSchema() },
  gridOptions: {
    columns: useGridColumns(),
    height: 'auto',
    keepSource: true,
    proxyConfig: {
      ajax: { query: async ({ page }, formValues) => await getRulePage({ pageNo: page.currentPage, pageSize: page.pageSize, ...formValues }) },
    },
    rowConfig: { keyField: 'id', isHover: true },
    toolbarConfig: { refresh: true, search: true },
  } as VxeTableGridOptions<MesSetupRuleApi.Rule>,
  gridEvents: { checkboxAll: handleRowCheckboxChange, checkboxChange: handleRowCheckboxChange },
});
</script>

<template>
  <Page auto-content-height>
    <FormModal @success="handleRefresh" />
    <BaseGrid table-title="投料防错规则列表">
      <template #toolbar-tools>
        <TableAction
          :actions="[
            { label: '新增规则', type: 'primary', icon: ACTION_ICON.ADD, onClick: handleCreate },
            { label: '批量删除', type: 'primary', danger: true, icon: ACTION_ICON.DELETE, disabled: isEmpty(checkedIds), onClick: handleDeleteBatch },
          ]"
        />
      </template>

      <template #processCode="{ row }">
        <Tag color="purple" v-if="row.processCode === 'MIXING'">配料</Tag>
        <Tag color="cyan" v-else-if="row.processCode === 'COATING'">涂布</Tag>
        <Tag color="blue" v-else-if="row.processCode === 'SLITTING'">分切</Tag>
      </template>

      <template #status="{ row }">
        <Tag :color="row.status === 1 ? 'success' : 'error'">{{ row.status === 1 ? '启用' : '停用' }}</Tag>
      </template>

      <template #actions="{ row }">
        <TableAction
          :actions="[
            { label: $t('common.detail'), type: 'link', icon: ACTION_ICON.VIEW, onClick: handleDetail.bind(null, row) },
            { label: $t('common.edit'), type: 'link', icon: ACTION_ICON.EDIT, onClick: handleEdit.bind(null, row) },
            { label: $t('common.delete'), type: 'link', danger: true, icon: ACTION_ICON.DELETE, popConfirm: { title: '确认删除?', confirm: () => handleDeleteBatch() } },
          ]"
        />
      </template>
    </BaseGrid>
  </Page>
</template>
