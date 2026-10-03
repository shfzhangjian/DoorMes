<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesDeviceLedgerApi } from '#/api/mes/resource/device/ledger';

import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue';

import { confirm, Page, useVbenModal } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';
import { downloadFileFromBlobPart, isEmpty } from '@vben/utils';

import { Card, Input, message, Spin, Tag, Tree } from 'ant-design-vue';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import { getCategoryList } from '#/api/mes/resource/device/category';
import {
  deleteDeviceList,
  exportDevice,
  getDevicePage,
} from '#/api/mes/resource/device/ledger';
import { $t } from '#/locales';

import { DEVICE_STATUS_OPTIONS, optionColor, optionLabel } from '../shared';
import { mapTreeData, useGridColumns, useGridFormSchema } from './data';
import FormComponent from './modules/form.vue';

defineOptions({ name: 'MesDeviceLedger' });

const [FormModal, formModalApi] = useVbenModal({
  connectedComponent: FormComponent,
  destroyOnClose: true,
});
const selectedCategoryId = ref<number>();
const selectedCategoryKeys = ref<Array<number | string>>([0]);
const expandedCategoryKeys = ref<Array<number | string>>([]);
const categoryRows = ref<any[]>([]);
const categoryLoading = ref(false);
const categorySearchValue = ref('');
const splitLayoutRef = ref<HTMLElement>();
const categoryPanelWidth = ref(288);
const splitDragging = ref(false);
const categoryPanelStyle = computed(() => ({
  width: `${categoryPanelWidth.value}px`,
}));
const splitPanelMinWidth = 220;
const splitPanelMaxWidth = 520;
const splitContentMinWidth = 720;
let bodyCursor = '';
let bodyUserSelect = '';

const filteredCategoryRows = computed(() => {
  const keyword = categorySearchValue.value.trim().toLowerCase();
  if (!keyword) {
    return categoryRows.value;
  }
  return categoryRows.value.filter((item) =>
    `${item.categoryName || ''}${item.categoryCode || ''}`
      .toLowerCase()
      .includes(keyword),
  );
});
const treeData = computed(() => mapTreeData(filteredCategoryRows.value));

function collectExpandedKeys(nodes: any[]): Array<number | string> {
  return nodes.flatMap((node) => {
    const currentKey = node.key === undefined ? [] : [node.key];
    return [...currentKey, ...collectExpandedKeys(node.children ?? [])];
  });
}

function handleRefresh() {
  gridApi.query();
}

function handleCreate() {
  formModalApi.setData({ type: 'create' }).open();
}
function handleEdit(row: MesDeviceLedgerApi.Device) {
  formModalApi.setData({ type: 'edit', id: row.id }).open();
}
function handleDetail(row: MesDeviceLedgerApi.Device) {
  formModalApi.setData({ type: 'detail', id: row.id }).open();
}

async function handleDeleteBatch() {
  await confirm('确认要删除选中的设备档案吗？');
  const hideLoading = message.loading({ content: '删除中...', duration: 0 });
  try {
    await deleteDeviceList(checkedIds.value);
    checkedIds.value = [];
    message.success('删除成功');
    handleRefresh();
  } finally {
    hideLoading();
  }
}

async function handleDelete(row: MesDeviceLedgerApi.Device) {
  await confirm(
    `确认要删除设备档案 [${row.deviceName || row.deviceCode}] 吗？`,
  );
  const hideLoading = message.loading({ content: '删除中...', duration: 0 });
  try {
    await deleteDeviceList([row.id!]);
    message.success('删除成功');
    handleRefresh();
  } finally {
    hideLoading();
  }
}

const checkedIds = ref<number[]>([]);
function handleRowCheckboxChange({
  records,
}: {
  records: MesDeviceLedgerApi.Device[];
}) {
  checkedIds.value = records.map((item) => item.id!);
}

async function handleExport() {
  const data = await exportDevice({
    ...(await gridApi.formApi.getValues()),
    categoryId: selectedCategoryId.value,
  });
  downloadFileFromBlobPart({ fileName: '设备台账.xls', source: data });
}

function getCategoryPanelWidth(clientX: number) {
  const rect = splitLayoutRef.value?.getBoundingClientRect();
  if (!rect) {
    return categoryPanelWidth.value;
  }
  const maxWidth = Math.min(
    splitPanelMaxWidth,
    Math.max(splitPanelMinWidth, rect.width - splitContentMinWidth),
  );
  return Math.min(maxWidth, Math.max(splitPanelMinWidth, clientX - rect.left));
}

function handleSplitPointerMove(event: PointerEvent) {
  categoryPanelWidth.value = getCategoryPanelWidth(event.clientX);
  nextTick(() => gridApi.grid?.recalculate?.(true));
}

function stopSplitResize() {
  if (!splitDragging.value) {
    return;
  }
  splitDragging.value = false;
  window.removeEventListener('pointermove', handleSplitPointerMove);
  window.removeEventListener('pointerup', stopSplitResize);
  window.removeEventListener('pointercancel', stopSplitResize);
  document.body.style.cursor = bodyCursor;
  document.body.style.userSelect = bodyUserSelect;
  nextTick(() => gridApi.grid?.recalculate?.(true));
}

function handleSplitPointerDown(event: PointerEvent) {
  event.preventDefault();
  splitDragging.value = true;
  bodyCursor = document.body.style.cursor;
  bodyUserSelect = document.body.style.userSelect;
  document.body.style.cursor = 'col-resize';
  document.body.style.userSelect = 'none';
  window.addEventListener('pointermove', handleSplitPointerMove);
  window.addEventListener('pointerup', stopSplitResize);
  window.addEventListener('pointercancel', stopSplitResize);
  handleSplitPointerMove(event);
}

const [BaseGrid, gridApi] = useVbenVxeGrid({
  formOptions: { schema: useGridFormSchema() },
  gridOptions: {
    columns: useGridColumns(),
    height: 'auto',
    keepSource: true,
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) =>
          await getDevicePage({
            pageNo: page.currentPage,
            pageSize: page.pageSize,
            ...formValues,
            categoryId: selectedCategoryId.value,
          }),
      },
    },
    rowConfig: { keyField: 'id', isHover: true },
    toolbarConfig: { refresh: true, search: true },
  } as VxeTableGridOptions<MesDeviceLedgerApi.Device>,
  gridEvents: {
    checkboxAll: handleRowCheckboxChange,
    checkboxChange: handleRowCheckboxChange,
  },
});

function handleTreeSelect(keys: Array<number | string>) {
  const key = Number(keys[0] ?? 0);
  selectedCategoryId.value = key > 0 ? key : undefined;
  selectedCategoryKeys.value = [Math.max(key, 0)];
  handleRefresh();
}

async function handleCategorySearch(e: any) {
  categorySearchValue.value = e.target.value;
  await nextTick();
  expandedCategoryKeys.value = collectExpandedKeys(treeData.value);
}

onMounted(async () => {
  await loadCategoryTree();
  expandedCategoryKeys.value = collectExpandedKeys(treeData.value);
});
onBeforeUnmount(stopSplitResize);

async function loadCategoryTree() {
  categoryLoading.value = true;
  try {
    categoryRows.value = await getCategoryList();
  } finally {
    categoryLoading.value = false;
  }
}
</script>

<template>
  <Page auto-content-height>
    <FormModal @success="handleRefresh" />
    <div ref="splitLayoutRef" class="device-ledger-layout">
      <Card class="device-ledger-tree-card" :style="categoryPanelStyle">
        <div class="device-ledger-category-tree">
          <Input
            v-model:value="categorySearchValue"
            allow-clear
            class="device-ledger-category-tree__search"
            placeholder="搜索设备分类"
            @change="handleCategorySearch"
          >
            <template #prefix>
              <IconifyIcon icon="lucide:search" class="size-4" />
            </template>
          </Input>
          <Spin
            :spinning="categoryLoading"
            wrapper-class-name="device-ledger-category-tree__spin"
          >
            <div class="device-ledger-category-tree__scroll">
              <Tree
                v-if="treeData.length > 0"
                v-model:expanded-keys="expandedCategoryKeys"
                block-node
                class="device-ledger-category-tree__tree"
                :field-names="{
                  title: 'title',
                  key: 'key',
                  children: 'children',
                }"
                :selected-keys="selectedCategoryKeys"
                :show-line="{ showLeafIcon: false }"
                :tree-data="treeData"
                @select="handleTreeSelect"
              />
              <div
                v-else-if="!categoryLoading"
                class="py-4 text-center text-gray-500"
              >
                暂无数据
              </div>
            </div>
          </Spin>
        </div>
      </Card>
      <button
        type="button"
        class="device-ledger-layout__splitter"
        :class="{ 'is-dragging': splitDragging }"
        aria-label="调整设备分类树宽度"
        @pointerdown="handleSplitPointerDown"
      ></button>
      <section class="device-ledger-grid">
        <BaseGrid table-title="设备台账总览">
          <template #toolbar-tools>
            <TableAction
              :actions="[
                {
                  label: '新增设备',
                  type: 'primary',
                  icon: ACTION_ICON.ADD,
                  auth: ['mes:resource-device-ledger:create'],
                  onClick: handleCreate,
                },
                {
                  label: '导出台账',
                  type: 'primary',
                  icon: ACTION_ICON.DOWNLOAD,
                  auth: ['mes:resource-device-ledger:export'],
                  onClick: handleExport,
                },
                {
                  label: '批量删除',
                  type: 'primary',
                  danger: true,
                  icon: ACTION_ICON.DELETE,
                  auth: ['mes:resource-device-ledger:delete'],
                  disabled: isEmpty(checkedIds),
                  onClick: handleDeleteBatch,
                },
              ]"
            />
          </template>

          <template #status="{ row }">
            <Tag :color="optionColor(DEVICE_STATUS_OPTIONS, row.status)">
              {{ optionLabel(DEVICE_STATUS_OPTIONS, row.status) }}
            </Tag>
          </template>

          <template #actions="{ row }">
            <div class="device-ledger-row-actions">
              <TableAction
                :actions="[
                  {
                    label: '',
                    tooltip: $t('common.detail'),
                    type: 'link',
                    icon: ACTION_ICON.VIEW,
                    onClick: handleDetail.bind(null, row),
                  },
                  {
                    label: '',
                    tooltip: $t('common.edit'),
                    type: 'link',
                    icon: ACTION_ICON.EDIT,
                    auth: ['mes:resource-device-ledger:update'],
                    onClick: handleEdit.bind(null, row),
                  },
                  {
                    label: '',
                    tooltip: $t('common.delete'),
                    type: 'link',
                    danger: true,
                    icon: ACTION_ICON.DELETE,
                    auth: ['mes:resource-device-ledger:delete'],
                    onClick: handleDelete.bind(null, row),
                  },
                ]"
              />
            </div>
          </template>
        </BaseGrid>
      </section>
    </div>
  </Page>
</template>

<style scoped>
.device-ledger-layout {
  display: flex;
  width: 100%;
  height: 100%;
  max-height: 100%;
  min-height: 0;
  overflow: hidden;
}

.device-ledger-tree-card {
  flex: 0 0 auto;
  height: 100%;
  min-width: 220px;
  max-width: 520px;
  overflow: hidden;
}

.device-ledger-tree-card :deep(.ant-card-body) {
  height: 100%;
  min-height: 0;
  padding: 16px;
  overflow: hidden;
}

.device-ledger-category-tree {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  row-gap: 8px;
  height: 100%;
  min-height: 0;
}

.device-ledger-category-tree__search {
  width: 100%;
}

.device-ledger-category-tree :deep(.device-ledger-category-tree__spin),
.device-ledger-category-tree :deep(.ant-spin-container) {
  height: 100%;
  min-height: 0;
}

.device-ledger-category-tree__scroll {
  height: 100%;
  min-height: 0;
  padding-right: 4px;
  overflow: auto;
}

.device-ledger-category-tree__tree {
  min-width: max-content;
}

.device-ledger-category-tree__tree :deep(.ant-tree-indent-unit::before) {
  border-color: hsl(var(--border));
}

.device-ledger-layout__splitter {
  position: relative;
  flex: 0 0 16px;
  width: 16px;
  height: 100%;
  padding: 0;
  cursor: col-resize;
  background: transparent;
  border: 0;
  touch-action: none;
}

.device-ledger-layout__splitter::before {
  position: absolute;
  top: 12px;
  bottom: 12px;
  left: 50%;
  width: 2px;
  content: '';
  background: hsl(var(--border));
  border-radius: 999px;
  transform: translateX(-50%);
  transition: background-color 0.2s ease;
}

.device-ledger-layout__splitter:hover::before,
.device-ledger-layout__splitter.is-dragging::before {
  background: hsl(var(--primary));
}

.device-ledger-grid {
  flex: 1 1 auto;
  height: 100%;
  min-width: 0;
  min-height: 0;
  overflow: auto;
}

.device-ledger-row-actions {
  display: flex;
  justify-content: center;
  width: 100%;
}

@media (max-width: 900px) {
  .device-ledger-layout {
    display: grid;
    grid-template-columns: 1fr;
    overflow: auto;
  }

  .device-ledger-tree-card {
    width: 100% !important;
    max-height: 220px;
  }

  .device-ledger-layout__splitter {
    display: none;
  }
}
</style>
