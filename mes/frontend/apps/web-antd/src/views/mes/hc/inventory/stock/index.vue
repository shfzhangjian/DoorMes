<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesHcInvStockApi } from '#/api/mes/hc/inv/stock';

import { nextTick, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';
import { downloadFileFromBlobPart } from '@vben/utils';
import { Tag } from 'ant-design-vue';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import { exportInvStock, getInvStockPage } from '#/api/mes/hc/inv/stock';

import {
  BIZ_STATUS_COLOR,
  QUALITY_STATUS_COLOR,
  resolveStockTypeLabel,
  useGridColumns,
  useGridFormSchema,
} from './data';

const advancedSearchVisible = ref(false);

function toggleAdvancedSearch() {
  advancedSearchVisible.value = !advancedSearchVisible.value;
  gridApi.formApi.setState((prev) => ({
    ...prev,
    schema: useGridFormSchema(advancedSearchVisible.value),
  }));
  nextTick(() => gridApi.grid?.recalculate?.(true));
}

const [Grid, gridApi] = useVbenVxeGrid({
  class: 'inventory-stock-vben-grid',
  formOptions: {
    schema: useGridFormSchema(false),
    collapsed: false,
    wrapperClass: 'grid-cols-1 md:grid-cols-3 lg:grid-cols-3',
    showCollapseButton: false,
  },
  gridClass: 'inventory-stock-vxe-grid',
  gridOptions: {
    columns: useGridColumns(),
    height: '100%',
    keepSource: true,
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) => {
          return await getInvStockPage({
            pageNo: page.currentPage,
            pageSize: page.pageSize,
            ...formValues,
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
  } as VxeTableGridOptions<MesHcInvStockApi.Stock>,
});

async function handleExport() {
  const formValues = await gridApi.formApi.getValues();
  const data = await exportInvStock(formValues);
  downloadFileFromBlobPart({ fileName: '实时库存余额.xls', source: data });
}
</script>

<template>
  <Page auto-content-height>
    <div class="inventory-stock-page">
      <div class="inventory-stock-page__content">
        <div class="inventory-stock-page__grid-host">
          <Grid table-title="实时库存余额列表">
            <template #toolbar-tools>
              <TableAction
                :actions="[
                  {
                    label: '导出',
                    type: 'primary',
                    icon: ACTION_ICON.DOWNLOAD,
                    auth: ['mes:inv:stock:export'],
                    onClick: handleExport,
                  },
                ]"
              />
            </template>

            <template #expand-after>
              <a class="inventory-stock-page__expand-link" @click="toggleAdvancedSearch">
                {{ advancedSearchVisible ? '收起' : '展开' }}
                <IconifyIcon
                  :icon="advancedSearchVisible ? 'lucide:chevron-up' : 'lucide:chevron-down'"
                  class="inventory-stock-page__expand-icon"
                />
              </a>
            </template>

            <template #qualityStatus="{ row }">
              <Tag :color="QUALITY_STATUS_COLOR[row.qualityStatus] || 'default'">
                {{ row.qualityStatus || '-' }}
              </Tag>
            </template>

            <template #bizStatus="{ row }">
              <Tag :color="BIZ_STATUS_COLOR[row.bizStatus] || 'default'">
                {{ row.bizStatus || '-' }}
              </Tag>
            </template>

            <template #stockType="{ row }">
              <Tag :color="row.stockType === 'WIP' ? 'processing' : 'success'">
                {{ resolveStockTypeLabel(row.stockType) }}
              </Tag>
            </template>
          </Grid>
        </div>
      </div>
    </div>
  </Page>
</template>

<style scoped>
.inventory-stock-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  max-height: 100%;
  min-height: 0;
  overflow: hidden;
  background: rgb(255 255 255);
  border-radius: 12px;
}

.inventory-stock-page__content {
  flex: 1;
  min-height: 0;
  position: relative;
  overflow: hidden;
}

.inventory-stock-page__grid-host {
  position: absolute;
  inset: 0;
  min-height: 0;
  overflow: hidden;
}

.inventory-stock-page__grid-host :deep(.inventory-stock-vben-grid) {
  height: 100% !important;
  max-height: 100% !important;
  min-height: 0 !important;
  overflow: hidden !important;
}

.inventory-stock-page__grid-host :deep(.inventory-stock-vxe-grid) {
  display: grid !important;
  grid-template-rows: auto auto auto minmax(0, 1fr) auto auto !important;
  height: 100% !important;
  max-height: 100% !important;
  min-height: 0 !important;
  overflow: hidden !important;
}

.inventory-stock-page__grid-host :deep(.vxe-grid--form-wrapper) {
  grid-row: 1;
  min-height: 0;
  overflow: visible;
}

.inventory-stock-page__grid-host :deep(.vxe-grid--toolbar-wrapper) {
  grid-row: 2;
  min-height: 0;
}

.inventory-stock-page__grid-host :deep(.vxe-grid--top-wrapper) {
  grid-row: 3;
  min-height: 0;
}

.inventory-stock-page__grid-host :deep(.vxe-grid--table-wrapper) {
  grid-row: 4;
  min-height: 0 !important;
  overflow: hidden !important;
}

.inventory-stock-page__grid-host :deep(.vxe-grid--bottom-wrapper) {
  grid-row: 5;
  min-height: 0;
}

.inventory-stock-page__grid-host :deep(.vxe-grid--pager-wrapper) {
  grid-row: 6;
  min-height: 0;
  background: rgb(255 255 255);
}

.inventory-stock-page__grid-host :deep(.vxe-grid--table-wrapper .vxe-table) {
  height: 100% !important;
  max-height: 100% !important;
}

.inventory-stock-page__grid-host :deep(.vxe-pager) {
  min-height: 36px;
}

.inventory-stock-page__expand-link {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  margin-left: 8px;
  color: rgb(22 119 255);
  font-size: 13px;
  cursor: pointer;
}

.inventory-stock-page__expand-link:hover {
  text-decoration: underline;
}

.inventory-stock-page__expand-icon {
  font-size: 14px;
}
</style>
