<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';

import { ref } from 'vue';
import { useRouter } from 'vue-router';

import { Page, useVbenModal } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';
import { Button, Form, Input, message, Tag } from 'ant-design-vue';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import { deleteLocation, exportLocation, getLocationPage } from '#/api/mes/hc/location';

import { buildLocationTree, filterLocationTree, useTreeColumns } from './data';
import type { LocationTreeNode } from './data';
import FormModal from './modules/form.vue';

const [LocationFormModal, formModalApi] = useVbenModal({
  connectedComponent: FormModal,
  destroyOnClose: true,
});

const router = useRouter();
const isExpanded = ref(true);
const queryForm = ref({ keyword: '' });

function filterTree(tree: LocationTreeNode[]) {
  return filterLocationTree(tree, queryForm.value.keyword);
}

function handleRefresh() {
  gridApi.query();
}

function handleSearch() {
  isExpanded.value = true;
  handleRefresh();
}

function handleReset() {
  queryForm.value = { keyword: '' };
  isExpanded.value = true;
  handleRefresh();
}

function handleExpand() {
  isExpanded.value = !isExpanded.value;
  gridApi.grid.setAllTreeExpand(isExpanded.value);
}

function handleCreate(warehouseNode?: LocationTreeNode) {
  formModalApi
    .setData(
      warehouseNode
        ? { warehouseCode: warehouseNode.warehouseCode, warehouseName: warehouseNode.warehouseName }
        : null,
    )
    .open();
}

function handleEdit(row: LocationTreeNode) {
  formModalApi.setData({ id: row.id }).open();
}

async function handleDelete(row: LocationTreeNode) {
  const hideLoading = message.loading({ content: '正在删除...', duration: 0 });
  try {
    await deleteLocation(row.id);
    message.success('已删除库位：' + row.locationName);
    handleRefresh();
  } finally {
    hideLoading();
  }
}

async function handleExport() {
  const data = await exportLocation({});
  const { downloadFileFromBlobPart } = await import('@vben/utils');
  downloadFileFromBlobPart({ fileName: '库位.xls', source: data });
}

async function handleOpenFgLocationManagement() {
  await router.push({ name: 'MesPackageFgLocation' });
}

// ── actions 全部在 script 里构建，模板只调用函数，彻底规避嵌套引号 ──────────

function getWarehouseActions(row: LocationTreeNode) {
  return [
    {
      label: '新增库位',
      type: 'link' as const,
      icon: ACTION_ICON.ADD,
      auth: ['mes:inv:location:create'],
      onClick: () => handleCreate(row),
    },
  ];
}

function getLocationActions(row: LocationTreeNode) {
  return [
    {
      label: '编辑',
      type: 'link' as const,
      icon: ACTION_ICON.EDIT,
      auth: ['mes:inv:location:update'],
      onClick: () => handleEdit(row),
    },
    {
      label: '删除',
      type: 'link' as const,
      danger: true,
      icon: ACTION_ICON.DELETE,
      auth: ['mes:inv:location:delete'],
      popConfirm: {
        title: '确认删除该库位吗？',
        confirm: () => handleDelete(row),
      },
    },
  ];
}

function getToolbarActions() {
  return [
    {
      label: isExpanded.value ? '收起' : '展开',
      type: 'primary' as const,
      onClick: handleExpand,
    },
    {
      label: '导出',
      type: 'default' as const,
      icon: ACTION_ICON.DOWNLOAD,
      auth: ['mes:inv:location:export'],
      onClick: handleExport,
    },
  ];
}

const [Grid, gridApi] = useVbenVxeGrid({
  gridOptions: {
    columns: useTreeColumns(),
    height: 'auto',
    keepSource: true,
    pagerConfig: { enabled: false },
    proxyConfig: {
      ajax: {
        query: async () => {
          const result = await getLocationPage({ pageNo: 1, pageSize: 200 });
          const tree = buildLocationTree(result.list ?? []);
          return filterTree(tree);
        },
      },
    },
    rowConfig: { keyField: 'id', isHover: true },
    toolbarConfig: { refresh: true },
    treeConfig: {
      transform: false,
      childrenField: 'children',
      expandAll: true,
      reserve: true,
      line: true,
    },
  } as VxeTableGridOptions<LocationTreeNode>,
});
</script>

<template>
  <Page auto-content-height class="location-tree-page">
    <LocationFormModal @success="handleRefresh" />

    <div class="location-tree-page__container">
      <!-- 查询栏 -->
      <div class="location-tree-page__query">
        <Form layout="inline" class="flex flex-wrap gap-y-3">
          <Form.Item label="库位编码/名称">
            <Input
              v-model:value="queryForm.keyword"
              allow-clear
              placeholder="输入编码或名称关键字"
              style="width: 220px"
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
        <div class="location-tree-page__fg-entry">
          <div>
            <strong>包装成品库位</strong>
            <span>按“货架-层-区”维护，请在成品库位管理中新增或修改。</span>
          </div>
          <Button type="primary" @click="handleOpenFgLocationManagement">
            成品库位管理
          </Button>
        </div>
      </div>

      <!-- 树形表格 -->
      <div class="location-tree-page__grid">
        <Grid table-title="仓库 · 库位维护">
          <template #toolbar-tools>
            <TableAction :actions="getToolbarActions()" />
          </template>

          <!-- 节点名称列 -->
          <template #nodeName="{ row }">
            <div class="location-tree-node">
              <IconifyIcon
                v-if="row.nodeType === 'warehouse'"
                icon="carbon:building"
                class="location-tree-node__icon size-4 text-[var(--ant-color-primary)]"
              />
              <IconifyIcon
                v-else
                icon="carbon:location-filled"
                class="location-tree-node__icon size-4 text-[var(--ant-color-success)]"
              />
              <span class="location-tree-node__text">{{ row.nodeName }}</span>
            </div>
          </template>

          <!-- 状态列 -->
          <template #status="{ row }">
            <span v-if="row.nodeType === 'warehouse'" />
            <Tag v-else :color="row.status === '启用' ? 'green' : 'default'">
              {{ row.status || '-' }}
            </Tag>
          </template>

          <!-- 操作列 -->
          <template #actions="{ row }">
            <TableAction
              v-if="row.nodeType === 'warehouse'"
              :actions="getWarehouseActions(row)"
            />
            <TableAction
              v-else
              :actions="getLocationActions(row)"
            />
          </template>
        </Grid>
      </div>
    </div>
  </Page>
</template>

<style scoped>
.location-tree-page {
  height: 100%;
}

.location-tree-page :deep(.page-content) {
  display: flex;
  height: 100%;
  min-height: 0;
  flex-direction: column;
  overflow: hidden;
}

.location-tree-page__container {
  display: flex;
  height: 100%;
  min-height: 0;
  flex: 1;
  flex-direction: column;
  gap: 12px;
  overflow: hidden;
}

.location-tree-page__query {
  flex-shrink: 0;
  border: 1px solid var(--ant-color-border-secondary);
  border-radius: 8px;
  background: #fff;
  padding: 16px;
}

.location-tree-page__fg-entry {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-top: 12px;
  padding: 10px 12px;
  border-radius: 6px;
  background: #f0f7ff;
  color: #315b85;
}

.location-tree-page__fg-entry strong,
.location-tree-page__fg-entry span {
  display: block;
}

.location-tree-page__fg-entry span {
  margin-top: 2px;
  color: #6481a0;
  font-size: 13px;
}

.location-tree-page__grid {
  min-height: 0;
  flex: 1;
  overflow: hidden;
}

.location-tree-page__grid :deep(.vben-vxe-grid) {
  height: 100%;
}

.location-tree-page__grid :deep(.vxe-grid--body-wrapper) {
  overflow: auto !important;
}

.location-tree-node {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.location-tree-node__icon {
  flex-shrink: 0;
}

.location-tree-node__text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
