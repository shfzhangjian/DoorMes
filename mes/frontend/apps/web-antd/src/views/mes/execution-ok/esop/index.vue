<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesEsopApi } from '#/api/mes/execution/esop';

import { ref } from 'vue';
import { confirm, Page, useVbenModal } from '@vben/common-ui';
import { isEmpty } from '@vben/utils';
import { message, Tag, Button } from 'ant-design-vue';
import { IconifyIcon } from '@vben/icons';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import { deleteEsopList, getEsopPage } from '#/api/mes/execution/esop';
import { $t } from '#/locales';

import { useGridColumns, useGridFormSchema } from './data';
import FormComponent from './modules/form.vue';

defineOptions({ name: 'MesEsopConfig' });

const [FormModal, formModalApi] = useVbenModal({ connectedComponent: FormComponent, destroyOnClose: true });

function handleRefresh() { gridApi.query(); }

function handleCreate() { formModalApi.setData({ type: 'create' }).open(); }
function handleEdit(row: MesEsopApi.Esop) { formModalApi.setData({ type: 'edit', id: row.id }).open(); }
function handleDetail(row: MesEsopApi.Esop) { formModalApi.setData({ type: 'detail', id: row.id }).open(); }

// 模拟预览文件
function handlePreview(url: string) {
  message.info(`正在打开文件预览: ${url}`);
}

async function handleDeleteBatch() {
  await confirm('确认要删除选中的作业指导书吗？');
  const hideLoading = message.loading({ content: '删除中...', duration: 0 });
  try {
    await deleteEsopList(checkedIds.value);
    checkedIds.value = [];
    message.success('删除成功');
    handleRefresh();
  } finally { hideLoading(); }
}

const checkedIds = ref<number[]>([]);
function handleRowCheckboxChange({ records }: { records: MesEsopApi.Esop[] }) {
  checkedIds.value = records.map((item) => item.id!);
}

const [BaseGrid, gridApi] = useVbenVxeGrid({
  formOptions: { schema: useGridFormSchema() },
  gridOptions: {
    columns: useGridColumns(),
    height: 'auto',
    keepSource: true,
    proxyConfig: {
      ajax: { query: async ({ page }, formValues) => await getEsopPage({ pageNo: page.currentPage, pageSize: page.pageSize, ...formValues }) },
    },
    rowConfig: { keyField: 'id', isHover: true },
    toolbarConfig: { refresh: true, search: true },
  } as VxeTableGridOptions<MesEsopApi.Esop>,
  gridEvents: { checkboxAll: handleRowCheckboxChange, checkboxChange: handleRowCheckboxChange },
});
</script>

<template>
  <Page auto-content-height>
    <FormModal @success="handleRefresh" />
    <BaseGrid table-title="电子作业指导书库">
      <template #toolbar-tools>
        <TableAction
          :actions="[
            { label: '上传指导书', type: 'primary', icon: ACTION_ICON.ADD, onClick: handleCreate },
            { label: '批量删除', type: 'primary', danger: true, icon: ACTION_ICON.DELETE, disabled: isEmpty(checkedIds), onClick: handleDeleteBatch },
          ]"
        />
      </template>

      <template #processCode="{ row }">
        <Tag color="purple" v-if="row.processCode === 'MIXING'">配料</Tag>
        <Tag color="cyan" v-else-if="row.processCode === 'COATING'">涂布</Tag>
        <Tag color="blue" v-else-if="row.processCode === 'SLITTING'">分切</Tag>
        <Tag color="orange" v-else-if="row.processCode === 'QC'">检验</Tag>
      </template>

      <template #fileUrl="{ row }">
        <Button type="link" size="small" @click="handlePreview(row.fileUrl)" title="点击预览">
          <IconifyIcon icon="lucide:file-text" class="text-lg text-indigo-600" />
        </Button>
      </template>

      <template #status="{ row }">
        <Tag :color="row.status === 1 ? 'success' : 'default'">{{ row.status === 1 ? '启用' : '已作废' }}</Tag>
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
