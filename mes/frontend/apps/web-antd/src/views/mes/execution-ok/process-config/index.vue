<script lang="ts" setup>
import { ref } from 'vue';
import { Page, useVbenModal, confirm } from '@vben/common-ui';
import { isEmpty } from '@vben/utils';
import { message, Tag } from 'ant-design-vue';
import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import { getConfigPage, deleteConfigs } from '#/api/mes/execution/process-config';
import { useGridColumns, useGridFormSchema } from './data';
import FormComponent from './modules/form.vue';

defineOptions({ name: 'MesProcessConfig' });

const [FormModal, formModalApi] = useVbenModal({ connectedComponent: FormComponent, destroyOnClose: true });
const checkedIds = ref<number[]>([]);

const [BaseGrid, gridApi] = useVbenVxeGrid({
  formOptions: { schema: useGridFormSchema() },
  gridOptions: {
    height: 'auto', // 1. 新增：开启高度自适应填充
    pagerConfig: {}, // 2. 建议新增：显式声明分页配置选项
    columns: useGridColumns(),
    keepSource: true,
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) =>
          await getConfigPage({ pageNo: page.currentPage, pageSize: page.pageSize, ...formValues }),
      },
    },
    rowConfig: { keyField: 'id', isHover: true },
    toolbarConfig: { refresh: true, search: true },
  },
  gridEvents: {
    checkboxAll: ({ records }) => { checkedIds.value = records.map(i => i.id!) },
    checkboxChange: ({ records }) => { checkedIds.value = records.map(i => i.id!) },
  }
});

function handleRefresh() { gridApi.query(); }
const handleCreate = () => formModalApi.setData({ type: 'create' }).open();
const handleEdit = (row: any) => formModalApi.setData({ type: 'edit', id: row.id }).open();
const handleDetail = (row: any) => formModalApi.setData({ type: 'detail', id: row.id }).open();

async function handleDeleteBatch() {
  await confirm('确认要删除选中的配置吗？');
  await deleteConfigs(checkedIds.value);
  message.success('删除成功');
  handleRefresh();
}
</script>

<template>
  <Page auto-content-height>
    <FormModal @success="handleRefresh" />
    <BaseGrid table-title="工艺参数配置列表">
      <template #toolbar-tools>
        <TableAction :actions="[
          { label: '新增配置', type: 'primary', icon: ACTION_ICON.ADD, onClick: handleCreate },
          { label: '批量删除', type: 'primary', danger: true, icon: ACTION_ICON.DELETE, disabled: isEmpty(checkedIds), onClick: handleDeleteBatch }
        ]" />
      </template>

      <template #range="{ row }">
        <span class="text-blue-600 font-bold">{{ row.minValue }}</span>
        <span class="mx-1 text-slate-400">~</span>
        <span class="text-red-600 font-bold">{{ row.maxValue }}</span>
        <small class="ml-1 text-slate-400">{{ row.paramUnit }}</small>
      </template>

      <template #isRequired="{ row }">
        <Tag :color="row.isRequired === 1 ? 'red' : 'blue'">{{ row.isRequired === 1 ? '必填' : '选填' }}</Tag>
      </template>

      <template #status="{ row }">
        <Tag :color="row.status === 1 ? 'success' : 'error'">{{ row.status === 1 ? '启用' : '停用' }}</Tag>
      </template>

      <template #actions="{ row }">
        <TableAction :actions="[
          { label: '详情', type: 'link', icon: ACTION_ICON.VIEW, onClick: handleDetail.bind(null, row) },
          { label: '编辑', type: 'link', icon: ACTION_ICON.EDIT, onClick: handleEdit.bind(null, row) },
          { label: '删除', type: 'link', danger: true, icon: ACTION_ICON.DELETE, popConfirm: { title: '确认删除?', confirm: () => deleteConfigs([row.id!]).then(handleRefresh) } }
        ]" />
      </template>
    </BaseGrid>
  </Page>
</template>
