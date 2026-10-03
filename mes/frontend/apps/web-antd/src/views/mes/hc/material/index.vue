<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesHcMaterialApi } from '#/api/mes/hc/material';
import type { MesHcMaterialCategoryApi } from '#/api/mes/hc/materialcategory';

import { computed, onMounted, ref } from 'vue';

import { confirm, Page, useVbenModal } from '@vben/common-ui';
import { downloadFileFromBlobPart, isEmpty } from '@vben/utils';
import { Button, Input, Tree, message } from 'ant-design-vue';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  deleteMaterial,
  deleteMaterialList,
  exportMaterial,
  getMaterialPage,
} from '#/api/mes/hc/material';
import { getMaterialCategoryTree } from '#/api/mes/hc/materialcategory';

import { useGridColumns, useGridFormSchema } from './data';
import Form from './modules/form.vue';

const [FormModal, formModalApi] = useVbenModal({
  connectedComponent: Form,
  destroyOnClose: true,
});

const checkedIds = ref<number[]>([]);
const categoryKeyword = ref('');
const categoryTree = ref<MesHcMaterialCategoryApi.TreeNode[]>([]);
const selectedCategoryId = ref<number>();
const selectedKeys = ref<number[]>([]);
const expandedKeys = ref<number[]>([]);

const ALL_CATEGORY_KEY = 0;

const filteredCategoryTree = computed(() =>
  filterCategoryTree(categoryTree.value, categoryKeyword.value.trim()),
);

function filterCategoryTree(
  list: MesHcMaterialCategoryApi.TreeNode[],
  keyword: string,
): MesHcMaterialCategoryApi.TreeNode[] {
  if (!keyword) {
    return list;
  }
  return list
    .map((item) => {
      const children = filterCategoryTree(item.children || [], keyword);
      const matched =
        String(item.categoryName || '').includes(keyword) ||
        String(item.categoryCode || '').includes(keyword);
      if (matched || children.length > 0) {
        return {
          ...item,
          children,
        };
      }
      return null;
    })
    .filter(Boolean) as MesHcMaterialCategoryApi.TreeNode[];
}

async function loadCategoryTree() {
  const data = await getMaterialCategoryTree();
  categoryTree.value = [
    {
      id: ALL_CATEGORY_KEY,
      categoryName: '全部',
      categoryCode: 'ALL',
      children: data,
    } as MesHcMaterialCategoryApi.TreeNode,
  ];
  expandedKeys.value = [ALL_CATEGORY_KEY, ...data.map((item) => item.id)];
}

function handleRefresh() {
  gridApi.query();
}

function handleCreate() {
  formModalApi.setData(null).open();
}

function handleEdit(row: MesHcMaterialApi.Material) {
  formModalApi.setData(row).open();
}

async function handleDelete(row: MesHcMaterialApi.Material) {
  const hideLoading = message.loading({ content: '正在删除...', duration: 0 });
  try {
    await deleteMaterial(row.id);
    message.success('删除成功');
    handleRefresh();
  } finally {
    hideLoading();
  }
}

async function handleDeleteBatch() {
  await confirm('确认删除选中的记录吗？');
  const hideLoading = message.loading({ content: '正在批量删除...', duration: 0 });
  try {
    await deleteMaterialList(checkedIds.value);
    checkedIds.value = [];
    message.success('删除成功');
    handleRefresh();
  } finally {
    hideLoading();
  }
}

function handleRowCheckboxChange({
  records,
}: {
  records: MesHcMaterialApi.Material[];
}) {
  checkedIds.value = records.map((item) => item.id);
}

async function handleExport() {
  const formValues = await gridApi.formApi.getValues();
  const data = await exportMaterial({
    ...formValues,
    defaultRecipeName: formValues.defaultRecipeCode || undefined,
    materialCategoryId: selectedCategoryId.value,
  });
  downloadFileFromBlobPart({ fileName: '物料主数据.xls', source: data });
}

function handleCategorySelect(keys: (number | string)[]) {
  const categoryId = Number(keys[0] || 0);
  selectedKeys.value = [categoryId || ALL_CATEGORY_KEY];
  selectedCategoryId.value =
    categoryId && categoryId !== ALL_CATEGORY_KEY ? categoryId : undefined;
  handleRefresh();
}

function handleCategoryReset() {
  categoryKeyword.value = '';
  selectedCategoryId.value = undefined;
  selectedKeys.value = [ALL_CATEGORY_KEY];
  handleRefresh();
}

function handleTreeExpand(keys: (number | string)[]) {
  expandedKeys.value = keys.map((item) => Number(item));
}

const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: {
    schema: useGridFormSchema(),
    collapsed: true,
    showCollapseButton: true,
  },
  gridOptions: {
    columns: useGridColumns(),
    height: 'auto',
    keepSource: true,
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) => {
          return await getMaterialPage({
            pageNo: page.currentPage,
            pageSize: page.pageSize,
            ...formValues,
            defaultRecipeName: formValues.defaultRecipeCode || undefined,
            materialCategoryId: selectedCategoryId.value,
          });
        },
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
  } as VxeTableGridOptions<MesHcMaterialApi.Material>,
  gridEvents: {
    checkboxAll: handleRowCheckboxChange,
    checkboxChange: handleRowCheckboxChange,
  },
});

onMounted(async () => {
  await loadCategoryTree();
  selectedKeys.value = [ALL_CATEGORY_KEY];
});
</script>

<template>
  <Page auto-content-height class="material-page">
    <FormModal @success="handleRefresh" />
    <div class="material-page__container">
      <div class="material-page__tree-panel">
        <div class="material-page__tree-header">
          <div class="text-base font-medium">物料分类</div>
          <Button type="link" @click="handleCategoryReset">清空</Button>
        </div>
        <Input
          v-model:value="categoryKeyword"
          allow-clear
          class="mb-3"
          placeholder="请输入分类名称/编码"
        />
        <div class="material-page__tree-body">
          <Tree
            :tree-data="filteredCategoryTree"
            :selected-keys="selectedKeys"
            :expanded-keys="expandedKeys"
            :auto-expand-parent="false"
            :field-names="{ title: 'categoryName', key: 'id', children: 'children' }"
            show-line
            block-node
            @expand="handleTreeExpand"
            @select="handleCategorySelect"
          />
        </div>
      </div>

      <div class="material-page__grid-panel">
        <Grid table-title="物料主数据列表">
          <template #toolbar-tools>
            <TableAction
              :actions="[
                {
                  label: '新增',
                  type: 'primary',
                  icon: ACTION_ICON.ADD,
                  auth: ['mes:md:material:create'],
                  onClick: handleCreate,
                },
                {
                  label: '导出',
                  type: 'primary',
                  auth: ['mes:md:material:export'],
                  onClick: handleExport,
                },
                {
                  label: '批量删除',
                  type: 'primary',
                  danger: true,
                  icon: ACTION_ICON.DELETE,
                  auth: ['mes:md:material:delete'],
                  disabled: isEmpty(checkedIds),
                  onClick: handleDeleteBatch,
                },
              ]"
            />
          </template>
          <template #actions="{ row }">
            <TableAction
              :actions="[
                {
                  label: '编辑',
                  type: 'link',
                  icon: ACTION_ICON.EDIT,
                  auth: ['mes:md:material:update'],
                  onClick: handleEdit.bind(null, row),
                },
                {
                  label: '删除',
                  type: 'link',
                  danger: true,
                  icon: ACTION_ICON.DELETE,
                  auth: ['mes:md:material:delete'],
                  popConfirm: {
                    title: '确认删除当前记录吗？',
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
.material-page {
  height: 100%;
}

.material-page :deep(.page) {
  height: 100%;
}

.material-page :deep(.page-content) {
  display: flex;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  padding: 0;
}

.material-page__container {
  display: flex;
  flex: 1;
  min-height: 0;
  height: 100%;
  gap: 12px;
  overflow: hidden;
}

.material-page__tree-panel {
  display: flex;
  width: 280px;
  min-width: 280px;
  flex-direction: column;
  border: 1px solid var(--ant-color-border);
  border-radius: 8px;
  background: #fff;
  padding: 12px;
  overflow: hidden;
}

.material-page__tree-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.material-page__tree-body {
  flex: 1;
  min-height: 0;
  overflow: auto;
}

.material-page__tree-body :deep(.ant-tree) {
  background: transparent;
}

.material-page__tree-body :deep(.ant-tree-switcher-line-icon) {
  color: #bfbfbf;
}

.material-page__tree-body :deep(.ant-tree-indent-unit) {
  width: 20px;
}

.material-page__tree-body :deep(.ant-tree-treenode) {
  width: 100%;
}

.material-page__tree-body :deep(.ant-tree-node-content-wrapper) {
  min-width: 0;
  border-radius: 6px;
}

.material-page__tree-body :deep(.ant-tree-title) {
  display: inline-block;
  min-width: 0;
}

.material-page__grid-panel {
  flex: 1;
  min-width: 0;
  min-height: 0;
  height: 100%;
  overflow: hidden;
}

.material-page__grid-panel :deep(.vben-vxe-grid) {
  height: 100%;
}

.material-page__grid-panel :deep(.vben-vxe-grid .vxe-grid) {
  height: 100%;
}

.material-page__grid-panel :deep(.vxe-grid--body-wrapper) {
  overflow: auto !important;
}
</style>
