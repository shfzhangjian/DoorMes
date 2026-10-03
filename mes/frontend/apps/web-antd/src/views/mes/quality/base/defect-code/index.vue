<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesDefectCodeApi } from '#/api/mes/quality/base/defect-code';

import { confirm, Page, useVbenModal } from '@vben/common-ui';

import { message, Tag } from 'ant-design-vue';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  deleteDefectCode,
  getDefectCodeList,
} from '#/api/mes/quality/base/defect-code';

import { useGridColumns, useGridFormSchema } from './data';
import FormComponent from './modules/form.vue';

defineOptions({ name: 'MesDefectCode' });

const [FormModal, formModalApi] = useVbenModal({
  connectedComponent: FormComponent,
  destroyOnClose: true,
});

function handleRefresh() {
  gridApi.query();
}

function handleCreate(
  parentId: number = 0,
  nodeType: 'CATEGORY' | 'ITEM' = 'ITEM',
) {
  formModalApi.setData({ type: 'create', parentId, nodeType }).open();
}

function handleEdit(row: MesDefectCodeApi.DefectCode) {
  formModalApi.setData({ type: 'edit', id: row.id }).open();
}

function handleDetail(row: MesDefectCodeApi.DefectCode) {
  formModalApi.setData({ type: 'detail', id: row.id }).open();
}

async function handleDelete(row: MesDefectCodeApi.DefectCode) {
  await confirm(`确认要删除 [${row.name}] 吗？如果包含子项将无法删除。`);
  const hideLoading = message.loading({ content: '删除中...', duration: 0 });
  try {
    await deleteDefectCode(row.id!);
    message.success('删除成功');
    handleRefresh();
  } catch (error: any) {
    message.error(error.message || '删除失败');
  } finally {
    hideLoading();
  }
}

// 💡 核心配置：开启 VxeGrid 的树形结构支持
const [BaseGrid, gridApi] = useVbenVxeGrid({
  formOptions: { schema: useGridFormSchema() },
  gridOptions: {
    columns: useGridColumns(),
    height: 'auto',
    keepSource: true,
    rowConfig: { keyField: 'id', isHover: true },
    treeConfig: {
      transform: true,
      rowField: 'id',
      parentField: 'parentId',
      expandAll: true,
    }, // 将平铺数据转为树形
    pagerConfig: { enabled: false }, // 树形结构通常不分页
    proxyConfig: {
      ajax: {
        query: async (_, formValues) => {
          const res = await getDefectCodeList(formValues);
          return { list: res };
        },
      },
    },
    toolbarConfig: { refresh: true, search: true },
  } as VxeTableGridOptions<MesDefectCodeApi.DefectCode>,
});
</script>

<template>
  <Page auto-content-height>
    <FormModal @success="handleRefresh" />
    <BaseGrid table-title="缺陷代码库维护">
      <template #toolbar-tools>
        <TableAction
          :actions="[
            {
              label: '新增一级分类',
              type: 'primary',
              icon: ACTION_ICON.ADD,
              onClick: () => handleCreate(0, 'CATEGORY'),
            },
          ]"
        />
      </template>

      <template #type="{ row }">
        <Tag v-if="row.type === 'CATEGORY'" color="blue">分类</Tag>
        <Tag v-else color="cyan">缺陷项</Tag>
      </template>

      <template #level="{ row }">
        <span v-if="row.type === 'CATEGORY'">-</span>
        <Tag
          v-else-if="row.level === 'CRITICAL'"
          color="error"
          class="font-bold"
        >
          严重
        </Tag>
        <Tag v-else-if="row.level === 'MAJOR'" color="warning">一般</Tag>
        <Tag v-else-if="row.level === 'MINOR'" color="default">轻微</Tag>
      </template>

      <template #status="{ row }">
        <Tag :color="row.status === 1 ? 'success' : 'error'">
          {{ row.status === 1 ? '启用' : '停用' }}
        </Tag>
      </template>

      <template #actions="{ row }">
        <TableAction
          :actions="[
            {
              label: '新增子项',
              type: 'link',
              icon: ACTION_ICON.ADD,
              onClick: () => handleCreate(row.id, 'ITEM'),
              ifShow: row.type === 'CATEGORY',
            },
            {
              label: '详情',
              type: 'link',
              icon: ACTION_ICON.VIEW,
              onClick: () => handleDetail(row),
            },
            {
              label: '编辑',
              type: 'link',
              icon: ACTION_ICON.EDIT,
              onClick: () => handleEdit(row),
            },
            {
              label: '删除',
              type: 'link',
              danger: true,
              icon: ACTION_ICON.DELETE,
              onClick: () => handleDelete(row),
            },
          ]"
        />
      </template>
    </BaseGrid>
  </Page>
</template>
