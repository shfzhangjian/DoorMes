<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';

import { Page, useVbenModal } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import { Button, message, Tag } from 'ant-design-vue';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  deleteIqcRecord,
  getIqcPage,
  getIqcPrintDetail,
} from '#/api/mes/quality/iqc';

import {
  retentionStatusColor,
  retentionStatusLabel,
  useGridColumns,
  useGridFormSchema,
} from '../iqc/data';
import QmsIqcCreateModal from '../iqc/modules/qms-iqc-create-modal.vue';
import QmsIqcInspectionPrintPreviewModal from '../iqc/modules/qms-iqc-inspection-print-preview-modal.vue';

defineOptions({ name: 'MesQualityIqcInspection' });

const [CreateModal, createModalApi] = useVbenModal({
  connectedComponent: QmsIqcCreateModal,
  destroyOnClose: true,
});

const [PrintPreviewModal, printPreviewModalApi] = useVbenModal({
  connectedComponent: QmsIqcInspectionPrintPreviewModal,
  destroyOnClose: true,
});

const searchFormFields = [
  'iqcNo',
  'receiptNo',
  'supplierName',
  'status',
  'judgment',
  'inspectionTime',
];

function getCollapsedKeepCount() {
  if (window.innerWidth < 768) return 1;
  if (window.innerWidth < 1024) return 2;
  return 3;
}

function handleSearchCollapsedChange(collapsed: boolean) {
  const keepFields = new Set(
    searchFormFields.slice(0, collapsed ? getCollapsedKeepCount() : undefined),
  );
  gridApi.formApi.updateSchema(
    searchFormFields.map((fieldName) => ({
      fieldName,
      hide: collapsed && !keepFields.has(fieldName),
    })),
  );
}

const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: {
    handleCollapsedChange: handleSearchCollapsedChange,
    schema: useGridFormSchema({ includeMaterialBatch: false }),
  },
  gridOptions: {
    columns: useGridColumns()?.map((column: any) =>
      column.field === 'action' ? { ...column, width: 140 } : column,
    ),
    height: 'auto',
    keepSource: true,
    rowConfig: { keyField: 'id', isHover: true },
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) =>
          await getIqcPage({
            ...formValues,
            pageNo: page.currentPage,
            pageSize: page.pageSize,
          }),
      },
    },
  } as VxeTableGridOptions<any>,
});

function handleCreateSuccess() {
  gridApi.query();
}

async function handlePrintInspection(row: any) {
  if (!row.id) return;
  const detail = await getIqcPrintDetail(row.id);
  printPreviewModalApi.setData(detail).open();
}

async function handleDelete(row: any) {
  if (!row.id) return;
  const hideLoading = message.loading({ content: '正在删除...', duration: 0 });
  try {
    await deleteIqcRecord(row.id);
    message.success('删除成功');
    await gridApi.query();
  } finally {
    hideLoading();
  }
}

function rowActions(row: any) {
  return [
    {
      label: '打印送检单',
      type: 'link',
      icon: 'lucide:printer',
      auth: ['mes:iqc:print'],
      onClick: () => handlePrintInspection(row),
    },
    {
      label: '删除',
      type: 'link',
      danger: true,
      icon: ACTION_ICON.DELETE,
      auth: ['mes:iqc:delete'],
      popConfirm: {
        title: `确认删除送检单“${row.iqcNo || '-'}”及其检验明细吗？`,
        confirm: () => handleDelete(row),
      },
    },
  ];
}
</script>

<template>
  <Page auto-content-height class="relative">
    <div class="flex h-full flex-col">
      <CreateModal @success="handleCreateSuccess" />
      <PrintPreviewModal />
      <Grid table-title="进料送检记录">
        <template #toolbar-tools>
          <div class="inspection-toolbar-actions">
            <Button
              v-access:code="['mes:iqc:create']"
              type="primary"
              @click="createModalApi.open()"
            >
              <IconifyIcon icon="lucide:plus" class="mr-1" />
              新增送检
            </Button>
            <Button @click="gridApi.query()">
              <IconifyIcon icon="lucide:refresh-cw" class="mr-1" />
              刷新
            </Button>
          </div>
        </template>
        <template #iqcNo="{ row }">
          <span class="font-mono font-bold text-indigo-700">{{
            row.iqcNo
          }}</span>
        </template>
        <template #judgment="{ row }">
          <Tag
            v-if="row.judgment === 'OK'"
            color="success"
            class="!m-0 border-none font-bold"
          >
            合格
          </Tag>
          <Tag
            v-else-if="row.judgment === 'NG'"
            color="error"
            class="!m-0 border-none font-bold"
          >
            不合格
          </Tag>
          <Tag v-else color="default" class="!m-0 border-none font-bold">
            待判定
          </Tag>
        </template>
        <template #status="{ row }">
          <Tag
            v-if="row.status === 'COMPLETED'"
            color="success"
            class="!m-0 border-none font-bold"
          >
            已完成
          </Tag>
          <Tag
            v-else-if="row.status === 'REJECTED'"
            color="error"
            class="!m-0 border-none font-bold"
          >
            已拒收
          </Tag>
          <Tag
            v-else-if="row.status === 'SUSPENDED'"
            color="warning"
            class="!m-0 border-none font-bold"
          >
            已挂起
          </Tag>
          <Tag
            v-else-if="row.status === 'WAITING_CONFIRM'"
            color="processing"
            class="!m-0 border-none font-bold"
          >
            待审核
          </Tag>
          <Tag v-else color="processing" class="!m-0 border-none font-bold">
            {{ row.status === 'INSPECTING' ? '检验中' : '待检验' }}
          </Tag>
        </template>
        <template #attachments="{ row }">
          <Tag
            v-if="row.inspectionApplyAttachmentUrls?.length"
            color="blue"
            class="!m-0"
          >
            {{ row.inspectionApplyAttachmentUrls.length }} 个
          </Tag>
          <span v-else class="text-slate-400">无</span>
        </template>
        <template #retentionStatus="{ row }">
          <Tag
            :color="retentionStatusColor(row.retentionStatus)"
            class="!m-0 border-none font-bold"
          >
            {{ retentionStatusLabel(row.retentionStatus) }}
          </Tag>
        </template>
        <template #actions="{ row }">
          <TableAction :actions="rowActions(row)" />
        </template>
      </Grid>
    </div>
  </Page>
</template>

<style scoped>
.inspection-toolbar-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}
</style>
