<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';

import { nextTick, ref } from 'vue';

import { Page, useVbenModal } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';
import { downloadFileFromBlobPart } from '@vben/utils';

import { Button, Tabs } from 'ant-design-vue';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  exportEightDExcel,
  getEightDPage,
} from '#/api/mes/quality/abnormal/eightD';
import { DictTag } from '#/components/dict-tag';

import { QMS_8D_DICT, useGridColumns, useGridFormSchema } from './data';
import DetailModalForm from './modules/detail-modal.vue';

defineOptions({ name: 'MesQualityEightD' });

const activeTab = ref('todo');
const advancedSearchVisible = ref(false);

const [DetailModal, detailModalApi] = useVbenModal({
  connectedComponent: DetailModalForm,
  destroyOnClose: true,
});

const [Grid, gridApi] = useVbenVxeGrid({
  class: 'qms-8d-vben-grid',
  gridClass: 'qms-8d-vxe-grid',
  formOptions: {
    schema: useGridFormSchema(false),
    collapsed: false,
    showCollapseButton: false,
  },
  gridOptions: {
    columns: useGridColumns(),
    height: '100%',
    keepSource: true,
    rowConfig: { keyField: 'id', isHover: true },
    toolbarConfig: { search: true },
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) =>
          await getEightDPage({
            pageNo: page.currentPage,
            pageSize: page.pageSize,
            ...formValues,
            tabType: activeTab.value,
          }),
      },
    },
  } as VxeTableGridOptions<any>,
});

function handleTabChange(key: string) {
  activeTab.value = key;
  gridApi.query();
}

function handleView(row?: any) {
  detailModalApi
    .setData(row?.id ? { ...row, tabType: activeTab.value } : { isNew: true })
    .open();
}

function toggleAdvancedSearch() {
  advancedSearchVisible.value = !advancedSearchVisible.value;
  gridApi.formApi.setState((prev) => ({
    ...prev,
    schema: useGridFormSchema(advancedSearchVisible.value),
  }));
  nextTick(() => gridApi.grid?.recalculate?.(true));
}

async function handleExport() {
  const data = await exportEightDExcel({
    ...(await gridApi.formApi.getValues()),
    tabType: activeTab.value,
  });
  downloadFileFromBlobPart({ fileName: '8D改善报告台账.xls', source: data });
}

function isOverdue(row: any) {
  if (!row.targetDate || row.status === 'PASSED') {
    return false;
  }
  return new Date(row.targetDate).getTime() < Date.now();
}
</script>

<template>
  <Page auto-content-height>
    <DetailModal @success="gridApi.query()" />
    <div class="qms-8d-page">
      <div class="qms-8d-content">
        <div class="qms-8d-grid-host">
          <Grid>
            <template #toolbar-actions>
              <Tabs
                v-model:active-key="activeTab"
                class="qms-capsule-tabs"
                @change="handleTabChange"
              >
                <Tabs.TabPane key="todo" tab="待我处理的 8D" />
                <Tabs.TabPane key="initiated" tab="我发起的" />
                <Tabs.TabPane key="processed" tab="我参与的" />
                <Tabs.TabPane key="monitor" tab="全局所有 8D" />
              </Tabs>
            </template>

            <template #toolbar-tools>
              <div class="flex items-center gap-2">
                <Button type="link" class="px-1" @click="toggleAdvancedSearch">
                  <IconifyIcon
                    :icon="
                      advancedSearchVisible
                        ? 'lucide:chevron-up'
                        : 'lucide:chevron-down'
                    "
                    class="mr-1"
                  />
                  {{ advancedSearchVisible ? '收起' : '展开' }}
                </Button>
                <TableAction
                  :actions="[
                    {
                      label: '新建 8D 报告',
                      type: 'primary',
                      icon: ACTION_ICON.ADD,
                      onClick: () => handleView(),
                    },
                    {
                      label: '导出',
                      type: 'link',
                      icon: ACTION_ICON.DOWNLOAD,
                      onClick: handleExport,
                    },
                  ]"
                />
              </div>
            </template>

            <template #reportNo="{ row }">
              <a
                class="font-mono font-bold text-blue-700 hover:underline"
                @click="handleView(row)"
              >
                {{ row.reportNo }}
              </a>
            </template>

            <template #sourceType="{ row }">
              <DictTag :type="QMS_8D_DICT.sourceType" :value="row.sourceType" />
            </template>

            <template #currentStep="{ row }">
              <DictTag :type="QMS_8D_DICT.step" :value="row.currentStep" />
            </template>

            <template #targetDate="{ row }">
              <span :class="isOverdue(row) ? 'font-bold text-red-600' : ''">
                {{ row.targetDate || '-' }}
              </span>
            </template>

            <template #status="{ row }">
              <DictTag :type="QMS_8D_DICT.status" :value="row.status" />
            </template>

            <template #actions="{ row }">
              <TableAction
                :actions="[
                  {
                    label: activeTab === 'todo' ? '办理' : '详情',
                    type: 'link',
                    icon:
                      activeTab === 'todo'
                        ? ACTION_ICON.EDIT
                        : ACTION_ICON.PREVIEW,
                    onClick: () => handleView(row),
                  },
                ]"
              />
            </template>
          </Grid>
        </div>
      </div>
    </div>
  </Page>
</template>

<style scoped>
.qms-8d-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  max-height: 100%;
  min-height: 0;
  overflow: hidden;
  background: #fff;
}

.qms-8d-content {
  position: relative;
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.qms-8d-grid-host {
  position: absolute;
  inset: 0;
  min-height: 0;
  overflow: hidden;
}

.qms-8d-grid-host :deep(.qms-8d-vben-grid) {
  height: 100% !important;
  max-height: 100% !important;
  min-height: 0 !important;
  overflow: hidden !important;
}

.qms-8d-grid-host :deep(.qms-8d-vxe-grid) {
  display: grid !important;
  grid-template-rows: auto auto auto minmax(0, 1fr) auto auto !important;
  height: 100% !important;
  min-height: 0 !important;
  overflow: hidden !important;
}

.qms-8d-grid-host :deep(.vxe-grid--table-wrapper) {
  grid-row: 4;
  min-height: 0 !important;
  overflow: hidden !important;
}

.qms-8d-grid-host :deep(.vxe-grid--pager-wrapper) {
  grid-row: 6;
  background: #fff;
}

.qms-capsule-tabs :deep(.ant-tabs-nav) {
  margin: 0 !important;
}

.qms-capsule-tabs :deep(.ant-tabs-tab) {
  margin: 0 24px 0 0 !important;
  padding: 0 0 8px !important;
}

.qms-capsule-tabs :deep(.ant-tabs-tab-btn) {
  color: #475569;
  font-weight: 500;
}

.qms-capsule-tabs :deep(.ant-tabs-tab-active .ant-tabs-tab-btn) {
  color: #1677ff !important;
}
</style>
