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
  SOURCE_TYPE_COLOR,
  SOURCE_TYPE_LABEL,
  useGridColumns,
  useGridFormSchema,
} from './data';

const advancedSearchVisible = ref(false);

const [Grid, gridApi] = useVbenVxeGrid({
  class: 'intermediate-stock-vben-grid',
  formOptions: {
    collapsed: false,
    schema: useGridFormSchema(false),
    showCollapseButton: false,
    wrapperClass: 'grid-cols-1 md:grid-cols-3 lg:grid-cols-3',
  },
  gridClass: 'intermediate-stock-vxe-grid',
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
            includeUnavailable: true,
            stockType: 'WIP',
          });
        },
      },
    },
    rowConfig: {
      isHover: true,
      keyField: 'id',
    },
    toolbarConfig: {
      refresh: true,
      search: true,
    },
  } as VxeTableGridOptions<MesHcInvStockApi.Stock>,
});

function toggleAdvancedSearch() {
  advancedSearchVisible.value = !advancedSearchVisible.value;
  gridApi.formApi.setState((prev) => ({
    ...prev,
    schema: useGridFormSchema(advancedSearchVisible.value),
  }));
  nextTick(() => gridApi.grid?.recalculate?.(true));
}

async function handleExport() {
  const formValues = await gridApi.formApi.getValues();
  const data = await exportInvStock({
    ...formValues,
    includeUnavailable: true,
    stockType: 'WIP',
  });
  downloadFileFromBlobPart({ fileName: '中间品边库查询.xls', source: data });
}
</script>

<template>
  <Page auto-content-height>
    <div class="intermediate-stock-page">
      <div class="intermediate-stock-page__content">
        <div class="intermediate-stock-page__grid-host">
          <Grid table-title="中间品边库台账">
            <template #toolbar-tools>
              <TableAction
                :actions="[
                  {
                    auth: ['mes:inv:stock:export'],
                    icon: ACTION_ICON.DOWNLOAD,
                    label: '导出',
                    onClick: handleExport,
                    type: 'primary',
                  },
                ]"
              />
            </template>

            <template #expand-after>
              <a class="intermediate-stock-page__expand-link" @click="toggleAdvancedSearch">
                {{ advancedSearchVisible ? '收起' : '展开' }}
                <IconifyIcon
                  :icon="advancedSearchVisible ? 'lucide:chevron-up' : 'lucide:chevron-down'"
                  class="intermediate-stock-page__expand-icon"
                />
              </a>
            </template>

            <template #sourceType="{ row }">
              <Tag :color="SOURCE_TYPE_COLOR[row.sourceType] || 'default'">
                {{ SOURCE_TYPE_LABEL[row.sourceType] || row.sourceType || '-' }}
              </Tag>
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
          </Grid>
        </div>
      </div>
    </div>
  </Page>
</template>

<style scoped>
.intermediate-stock-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  max-height: 100%;
  min-height: 0;
  overflow: hidden;
  background: rgb(255 255 255);
  border-radius: 8px;
}

.intermediate-stock-page__content {
  position: relative;
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.intermediate-stock-page__grid-host {
  position: absolute;
  inset: 0;
  min-height: 0;
  overflow: hidden;
}

.intermediate-stock-page__grid-host :deep(.intermediate-stock-vben-grid) {
  height: 100% !important;
  max-height: 100% !important;
  min-height: 0 !important;
  overflow: hidden !important;
}

.intermediate-stock-page__grid-host :deep(.intermediate-stock-vxe-grid) {
  display: grid !important;
  grid-template-rows: auto auto auto minmax(0, 1fr) auto auto !important;
  height: 100% !important;
  max-height: 100% !important;
  min-height: 0 !important;
  overflow: hidden !important;
}

.intermediate-stock-page__grid-host :deep(.vxe-grid--form-wrapper) {
  grid-row: 1;
  min-height: 0;
  overflow: visible;
}

.intermediate-stock-page__grid-host :deep(.vxe-grid--toolbar-wrapper) {
  grid-row: 2;
  min-height: 0;
}

.intermediate-stock-page__grid-host :deep(.vxe-grid--top-wrapper) {
  grid-row: 3;
  min-height: 0;
}

.intermediate-stock-page__grid-host :deep(.vxe-grid--table-wrapper) {
  grid-row: 4;
  min-height: 0 !important;
  overflow: hidden !important;
}

.intermediate-stock-page__grid-host :deep(.vxe-grid--bottom-wrapper) {
  grid-row: 5;
  min-height: 0;
}

.intermediate-stock-page__grid-host :deep(.vxe-grid--pager-wrapper) {
  grid-row: 6;
  min-height: 0;
  background: rgb(255 255 255);
}

.intermediate-stock-page__grid-host :deep(.vxe-grid--table-wrapper .vxe-table) {
  height: 100% !important;
  max-height: 100% !important;
}

.intermediate-stock-page__grid-host :deep(.vxe-pager) {
  min-height: 36px;
}

.intermediate-stock-page__expand-link {
  display: inline-flex;
  gap: 2px;
  align-items: center;
  margin-left: 8px;
  color: rgb(22 119 255);
  font-size: 13px;
  cursor: pointer;
}

.intermediate-stock-page__expand-link:hover {
  text-decoration: underline;
}

.intermediate-stock-page__expand-icon {
  font-size: 14px;
}
</style>
