<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';

import { ref } from 'vue';

import { Page, useVbenModal } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';
import { downloadFileFromBlobPart } from '@vben/utils';

import { Checkbox, Tag } from 'ant-design-vue';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  exportOrder,
  getOrderPage,
} from '#/api/mes/resource/device/fault-repair';

import { useGridColumns, useGridFormSchema } from './data';
import DetailModalForm from './modules/form.vue';

defineOptions({ name: 'MesResourceDeviceException' });

const DEVICE_EXCEPTION_MODAL_DEFAULTS = {
  exceptionLevel: 'MAJOR',
  status: 'REPORTED',
};
const mineScope = ref<string[]>(['pendingMine']);
const mineScopeOptions = [
  { label: '待我处理的', value: 'pendingMine' },
  { label: '我提报的', value: 'reportedMine' },
  { label: '全局台账', value: 'monitor' },
];
const [DetailModal, detailModalApi] = useVbenModal({
  connectedComponent: DetailModalForm,
  destroyOnClose: true,
});

const [Grid, gridApi] = useVbenVxeGrid({
  class: 'qms-exception-vben-grid',
  gridClass: 'qms-exception-vxe-grid',
  formOptions: {
    schema: useGridFormSchema(),
    collapsed: false,
    showCollapseButton: false,
  },
  gridOptions: {
    columns: useGridColumns(),
    height: '100%',
    keepSource: true,
    pagerConfig: { enabled: true },
    rowConfig: { keyField: 'id', isHover: true },
    toolbarConfig: { search: true },
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) => {
          const res: any = await getOrderPage({
            pageNo: page.currentPage,
            pageSize: page.pageSize,
            tabType: getTabType(),
            ...formValues,
          });
          return { list: res.list, total: res.total };
        },
      },
    },
  } as VxeTableGridOptions<any>,
});

function getTabType() {
  if (mineScope.value.includes('monitor')) return 'monitor';
  if (mineScope.value.includes('reportedMine')) return 'initiated';
  return 'todo';
}
function handleMineScopeChange() {
  gridApi.query();
}
function handleView(row?: any) {
  detailModalApi
    .setData(
      row?.id
        ? { ...row, tabType: getTabType() }
        : { ...DEVICE_EXCEPTION_MODAL_DEFAULTS, isNew: true, type: 'create' },
    )
    .open();
}
async function handleExport() {
  const data = await exportOrder({
    ...(await gridApi.formApi.getValues()),
    tabType: getTabType(),
  });
  downloadFileFromBlobPart({ fileName: '设备异常事件.xls', source: data });
}

function getNodeText(status: string) {
  const map: any = {
    REPORTED: '响应分派',
    DISPATCHED: '维修执行',
    PENDING_CONFIRM: '完成确认',
    PENDING_ARCHIVE: '关闭归档',
    CLOSED: '已关闭归档',
  };
  return map[status] || status;
}
function buildRowActions(row: any) {
  const canHandle = row.canHandle === true || row.currentUserTaskTodo === true;
  return [
    {
      label: row.listActionName || (canHandle ? '办理' : '详情'),
      type: 'link' as const,
      icon: canHandle ? ACTION_ICON.EDIT : ACTION_ICON.VIEW,
      onClick: handleView.bind(null, row),
    },
  ];
}
function getNodeColor(status: string) {
  if (status === 'CLOSED') return 'bg-green-50 text-green-700 border-green-200';
  if (status === 'REPORTED') return 'bg-red-50 text-red-700 border-red-200';
  if (status === 'PENDING_ARCHIVE')
    return 'bg-purple-50 text-purple-700 border-purple-200';
  return 'bg-blue-50 text-blue-700 border-blue-200';
}
</script>

<template>
  <Page auto-content-height>
    <DetailModal @success="gridApi.query()" />

    <div class="qms-exception-page">
      <div class="qms-exception-content">
        <div class="qms-exception-grid-host">
          <Grid>
            <template #toolbar-actions>
              <div class="qms-mine-scope">
                <Checkbox.Group
                  v-model:value="mineScope"
                  :options="mineScopeOptions"
                  @change="handleMineScopeChange"
                />
              </div>
            </template>

            <template #toolbar-tools>
              <TableAction
                :actions="[
                  {
                    label: '设备异常上报',
                    type: 'primary',
                    icon: ACTION_ICON.ADD,
                    auth: ['mes:resource-device-exception:create'],
                    onClick: () => handleView(),
                  },
                  {
                    label: '导出',
                    type: 'primary',
                    icon: ACTION_ICON.DOWNLOAD,
                    auth: ['mes:resource-device-exception:export'],
                    onClick: handleExport,
                  },
                ]"
              />
            </template>

            <template #orderNo="{ row }">
              <a
                class="font-mono font-bold text-indigo-700 hover:underline"
                @click="handleView(row)"
              >
                {{ row.orderNo }}
              </a>
            </template>
            <template #deviceName="{ row }">
              <span class="font-bold text-slate-800">{{ row.deviceName }}</span>
            </template>

            <template #node="{ row }">
              <div
                class="inline-block rounded border px-2 py-1 text-xs font-bold shadow-sm"
                :class="getNodeColor(row.status)"
              >
                <IconifyIcon
                  v-if="row.status !== 'CLOSED'"
                  icon="lucide:loader-2"
                  class="mr-1 inline animate-spin"
                />
                {{ row.currentNodeName || getNodeText(row.status) }}
              </div>
            </template>

            <template #status="{ row }">
              <Tag
                v-if="row.status === 'REPORTED'"
                color="error"
                class="!m-0 border-none font-bold"
              >
                待响应
              </Tag>
              <Tag
                v-else-if="row.status === 'DISPATCHED'"
                color="processing"
                class="!m-0 border-none font-bold"
              >
                维修中
              </Tag>
              <Tag
                v-else-if="row.status === 'PENDING_CONFIRM'"
                color="warning"
                class="!m-0 border-none font-bold"
              >
                待确认
              </Tag>
              <Tag
                v-else-if="row.status === 'PENDING_ARCHIVE'"
                color="purple"
                class="!m-0 border-none font-bold"
              >
                待归档
              </Tag>
              <Tag
                v-else-if="row.status === 'CLOSED'"
                color="success"
                class="!m-0 border-none font-bold"
              >
                已闭环
              </Tag>
            </template>

            <template #actions="{ row }">
              <TableAction :actions="buildRowActions(row)" />
            </template>
          </Grid>
        </div>
      </div>
    </div>
  </Page>
</template>

<style scoped>
.qms-exception-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  max-height: 100%;
  min-height: 0;
  overflow: hidden;
  background: #fff;
}

.qms-exception-content {
  position: relative;
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.qms-exception-grid-host {
  position: absolute;
  inset: 0;
  min-height: 0;
  overflow: hidden;
}

.qms-exception-grid-host :deep(.qms-exception-vben-grid) {
  height: 100% !important;
  max-height: 100% !important;
  min-height: 0 !important;
  overflow: hidden !important;
}

.qms-exception-grid-host :deep(.qms-exception-vxe-grid) {
  display: grid !important;
  grid-template-rows: auto auto auto minmax(0, 1fr) auto auto !important;
  height: 100% !important;
  min-height: 0 !important;
  overflow: hidden !important;
}

.qms-exception-grid-host :deep(.vxe-grid--table-wrapper) {
  grid-row: 4;
  min-height: 0 !important;
  overflow: hidden !important;
}

.qms-exception-grid-host :deep(.vxe-grid--pager-wrapper) {
  grid-row: 6;
  background: #fff;
}

.qms-mine-scope {
  display: flex;
  min-height: 32px;
  align-items: center;
}

.qms-mine-scope :deep(.ant-checkbox-group) {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px 18px;
}

.qms-mine-scope :deep(.ant-checkbox-wrapper) {
  margin-inline-start: 0;
  color: #475569;
  font-weight: 500;
}
</style>
