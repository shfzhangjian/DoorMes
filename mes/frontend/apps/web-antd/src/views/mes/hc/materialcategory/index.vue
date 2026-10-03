<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesHcMaterialCategoryApi } from '#/api/mes/hc/materialcategory';

import { ref } from 'vue';

import { Page, useVbenModal } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';
import { downloadFileFromBlobPart } from '@vben/utils';
import { Button, Form, Input, message } from 'ant-design-vue';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  deleteMaterialCategory,
  exportMaterialCategory,
  getMaterialCategoryTree,
  moveDownMaterialCategory,
  moveUpMaterialCategory,
} from '#/api/mes/hc/materialcategory';

import { useGridColumns } from './data';
import FormModal from './modules/form.vue';

const [MaterialCategoryFormModal, formModalApi] = useVbenModal({
  connectedComponent: FormModal,
  destroyOnClose: true,
});

const isExpanded = ref(true);
const queryForm = ref({
  categoryCode: '',
  categoryName: '',
});

function filterTree(
  list: MesHcMaterialCategoryApi.TreeNode[],
): MesHcMaterialCategoryApi.TreeNode[] {
  const codeKeyword = queryForm.value.categoryCode.trim();
  const nameKeyword = queryForm.value.categoryName.trim();

  return list
    .map((item) => {
      const children = filterTree(item.children || []);
      const matchedCode = !codeKeyword || String(item.categoryCode || '').includes(codeKeyword);
      const matchedName = !nameKeyword || String(item.categoryName || '').includes(nameKeyword);
      if ((matchedCode && matchedName) || children.length > 0) {
        return {
          ...item,
          children,
        };
      }
      return null;
    })
    .filter(Boolean) as MesHcMaterialCategoryApi.TreeNode[];
}

function handleRefresh() {
  gridApi.query();
}

function handleSearch() {
  isExpanded.value = true;
  handleRefresh();
}

function handleReset() {
  queryForm.value = {
    categoryCode: '',
    categoryName: '',
  };
  isExpanded.value = true;
  handleRefresh();
}

function handleCreateRoot() {
  formModalApi.setData({ parentIdOverride: 0 }).open();
}

function handleCreateChild(row: MesHcMaterialCategoryApi.TreeNode) {
  formModalApi.setData({ parentIdOverride: row.id }).open();
}

function handleEdit(row: MesHcMaterialCategoryApi.TreeNode) {
  formModalApi.setData(row).open();
}

async function handleDelete(row: MesHcMaterialCategoryApi.TreeNode) {
  const hideLoading = message.loading({
    content: `正在删除分类“${row.categoryName}”`,
    duration: 0,
  });
  try {
    await deleteMaterialCategory(row.id);
    message.success(`已删除分类“${row.categoryName}”`);
    handleRefresh();
  } finally {
    hideLoading();
  }
}

async function handleMoveUp(row: MesHcMaterialCategoryApi.TreeNode) {
  await moveUpMaterialCategory(row.id);
  message.success(`已上移“${row.categoryName}”`);
  handleRefresh();
}

async function handleMoveDown(row: MesHcMaterialCategoryApi.TreeNode) {
  await moveDownMaterialCategory(row.id);
  message.success(`已下移“${row.categoryName}”`);
  handleRefresh();
}

function handleExpand() {
  isExpanded.value = !isExpanded.value;
  gridApi.grid.setAllTreeExpand(isExpanded.value);
}

async function handleExport() {
  const data = await exportMaterialCategory({
    categoryCode: queryForm.value.categoryCode || undefined,
    categoryName: queryForm.value.categoryName || undefined,
  });
  downloadFileFromBlobPart({
    fileName: '物料分类.xls',
    source: data,
  });
}

const [Grid, gridApi] = useVbenVxeGrid({
  gridOptions: {
    columns: useGridColumns(),
    height: 'auto',
    keepSource: true,
    pagerConfig: {
      enabled: false,
    },
    proxyConfig: {
      ajax: {
        query: async () => {
          const data = await getMaterialCategoryTree();
          return filterTree(data);
        },
      },
    },
    rowConfig: {
      keyField: 'id',
      isHover: true,
    },
    toolbarConfig: {
      refresh: true,
    },
    treeConfig: {
      transform: false,
      childrenField: 'children',
      expandAll: true,
      reserve: true,
      line: true,
    },
  } as VxeTableGridOptions<MesHcMaterialCategoryApi.TreeNode>,
});
</script>

<template>
  <Page auto-content-height class="material-category-page">
    <MaterialCategoryFormModal @success="handleRefresh" />

    <div class="material-category-page__container">
      <div class="material-category-page__query">
        <Form layout="inline" class="flex flex-wrap gap-y-3">
          <Form.Item label="分类编码">
            <Input
              v-model:value="queryForm.categoryCode"
              allow-clear
              placeholder="请输入分类编码"
              @press-enter="handleSearch"
            />
          </Form.Item>
          <Form.Item label="分类名称">
            <Input
              v-model:value="queryForm.categoryName"
              allow-clear
              placeholder="请输入分类名称"
              @press-enter="handleSearch"
            />
          </Form.Item>
          <Form.Item>
            <div class="flex items-center gap-2">
              <Button type="primary" @click="handleSearch">查询</Button>
              <Button @click="handleReset">重置</Button>
            </div>
          </Form.Item>
        </Form>
      </div>

      <div class="material-category-page__grid">
        <Grid table-title="物料分类树维护">
          <template #toolbar-tools>
            <TableAction
              :actions="[
                {
                  label: '新增顶级分类',
                  type: 'primary',
                  icon: ACTION_ICON.ADD,
                  auth: ['mes:md:material-category:create'],
                  onClick: handleCreateRoot,
                },
                {
                  label: isExpanded ? '收缩' : '展开',
                  type: 'primary',
                  onClick: handleExpand,
                },
                {
                  label: '导出',
                  type: 'default',
                  auth: ['mes:md:material-category:export'],
                  onClick: handleExport,
                },
              ]"
            />
          </template>
          <template #categoryName="{ row }">
            <div class="material-category-node">
              <IconifyIcon
                icon="carbon:category"
                class="material-category-node__icon size-4 text-[var(--ant-color-primary)]"
              />
              <span class="material-category-node__text">{{ row.categoryName }}</span>
            </div>
          </template>
          <template #actions="{ row }">
            <TableAction
              :actions="[
                {
                  label: '新增下级',
                  type: 'link',
                  icon: ACTION_ICON.ADD,
                  auth: ['mes:md:material-category:create'],
                  onClick: handleCreateChild.bind(null, row),
                },
                {
                  label: '编辑',
                  type: 'link',
                  icon: ACTION_ICON.EDIT,
                  auth: ['mes:md:material-category:update'],
                  onClick: handleEdit.bind(null, row),
                },
                {
                  label: '上移',
                  type: 'link',
                  auth: ['mes:md:material-category:update'],
                  onClick: handleMoveUp.bind(null, row),
                },
                {
                  label: '下移',
                  type: 'link',
                  auth: ['mes:md:material-category:update'],
                  onClick: handleMoveDown.bind(null, row),
                },
                {
                  label: '删除',
                  type: 'link',
                  danger: true,
                  icon: ACTION_ICON.DELETE,
                  auth: ['mes:md:material-category:delete'],
                  disabled: row.children && row.children.length > 0,
                  popConfirm: {
                    title: `确认删除分类“${row.categoryName}”吗？`,
                    confirm: handleDelete.bind(null, row),
                  },
                },
              ]"
            />
          </template>
        </Grid>
      </div>
    </div>
  </Page>
</template>

<style scoped>
.material-category-page {
  height: 100%;
}

.material-category-page :deep(.page-content) {
  display: flex;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  flex-direction: column;
}

.material-category-page__container {
  display: flex;
  height: 100%;
  min-height: 0;
  flex: 1;
  flex-direction: column;
  gap: 12px;
  overflow: hidden;
}

.material-category-page__query {
  flex-shrink: 0;
  border: 1px solid var(--ant-color-border-secondary);
  border-radius: 8px;
  background: #fff;
  padding: 16px;
}

.material-category-page__grid {
  min-height: 0;
  flex: 1;
  overflow: hidden;
}

.material-category-page__grid :deep(.vben-vxe-grid) {
  height: 100%;
}

.material-category-page__grid :deep(.vben-vxe-grid .vxe-grid) {
  height: 100%;
}

.material-category-page__grid :deep(.vxe-grid--body-wrapper) {
  overflow: auto !important;
}

.material-category-node {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.material-category-node__icon {
  flex-shrink: 0;
}

.material-category-node__text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>

