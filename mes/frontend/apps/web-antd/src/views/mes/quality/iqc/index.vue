<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';

import { ref } from 'vue';

import { Page, useVbenModal } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';
import { downloadFileFromBlobPart } from '@vben/utils';

import { Button, message, Modal, Tag } from 'ant-design-vue';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  buildIqcCoaWordFileName,
  confirmIqcRetention,
  exportIqcCoaWord,
  getIqcPage,
  resolveIqcRetentionRule,
} from '#/api/mes/quality/iqc';

import {
  retentionStatusColor,
  retentionStatusLabel,
  useGridColumns,
  useGridFormSchema,
} from './data';
// 💡 替换为 Modal 引用
import DetailModalVue from './modules/detail-modal.vue';
import QmsIqcScanResolveModal from './modules/qms-iqc-scan-resolve-modal.vue';
import WorkbenchMode from './modules/workbench.vue';

defineOptions({ name: 'MesQualityIqc' });

const viewMode = ref<'LEDGER' | 'WORKBENCH'>('LEDGER');
const activeWorkbenchId = ref<number>();
const scanModalOpen = ref(false);

// 💡 注册详情弹窗
const [DetailModal, detailModalApi] = useVbenModal({
  connectedComponent: DetailModalVue,
  destroyOnClose: true,
});

const searchFormFields = [
  'materialCode',
  'batchNo',
  'supplierName',
  'iqcNo',
  'receiptNo',
  'status',
  'judgment',
  'retentionStatus',
  'standardNo',
  'standardMatchMode',
  'inspectionTime',
];

const collapsedSearchFormFields = ['materialCode', 'batchNo', 'supplierName'];

function buildSearchFormSchema(collapsed = false) {
  const keepFields = new Set(
    collapsed ? collapsedSearchFormFields : searchFormFields,
  );
  return useGridFormSchema().map((item) => ({
    ...item,
    hide: collapsed && !keepFields.has(item.fieldName),
  }));
}

function handleSearchCollapsedChange(collapsed: boolean) {
  const keepFields = new Set(
    collapsed ? collapsedSearchFormFields : searchFormFields,
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
    collapsed: true,
    handleCollapsedChange: handleSearchCollapsedChange,
    schema: buildSearchFormSchema(true),
  },
  gridOptions: {
    columns: useGridColumns(),
    height: 'auto',
    keepSource: true,
    rowConfig: { keyField: 'id', isHover: true },
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) =>
          await getIqcPage({
            pageNo: page.currentPage,
            pageSize: page.pageSize,
            ...formValues,
          }),
      },
    },
  } as VxeTableGridOptions<any>,
});

// 查看详情 (仅预览)
function handleViewDetail(row: any) {
  detailModalApi.setData({ id: row.id }).open();
}

function handleAudit(row: any) {
  detailModalApi
    .setData({
      id: row.id,
      mode: 'audit',
      onSuccess: () => gridApi.query(),
    })
    .open();
}

function canAudit(row: any) {
  return row.status === 'WAITING_CONFIRM';
}

function isAuditReturnedRecord(row: any) {
  return (
    row?.status === 'INSPECTING' &&
    (!!row.lastReturnReason || Number(row.returnCount || 0) > 0)
  );
}

function statusColor(row: any) {
  if (isAuditReturnedRecord(row)) return 'warning';
  if (['COMPLETED', 'FINISHED'].includes(row?.status)) return 'success';
  if (row?.status === 'REJECTED') return 'error';
  if (row?.status === 'SUSPENDED') return 'warning';
  if (['INSPECTING', 'PENDING', 'WAITING_CONFIRM'].includes(row?.status)) {
    return 'processing';
  }
  return 'default';
}

function statusLabel(row: any) {
  if (isAuditReturnedRecord(row)) return '已驳回待重填';
  const map: Record<string, string> = {
    CANCELED: '已取消',
    COMPLETED: '已完成',
    FINISHED: '已完成',
    INSPECTING: '检验中',
    PENDING: '待检验',
    REJECTED: '已拒收',
    SUSPENDED: '已挂起',
    WAITING_CONFIRM: '待审核',
  };
  return row?.status ? map[row.status] || row.status : '-';
}

function standardMatchModeColor(value?: string) {
  return value === 'UNIVERSAL' ? 'purple' : 'blue';
}

function standardMatchModeLabel(value?: string) {
  if (value === 'UNIVERSAL') return '通用';
  return '物料专用';
}

function isReadonlyRecord(row: any) {
  return [
    'CANCELED',
    'COMPLETED',
    'FINISHED',
    'REJECTED',
    'WAITING_CONFIRM',
  ].includes(row.status);
}

function handleScanResolved(resp: any) {
  if (!resp.record?.id) {
    if (resp.message) message.info(resp.message);
    return;
  }
  if (resp.openTarget === 'REPORT' || isReadonlyRecord(resp.record)) {
    handleViewDetail(resp.record);
    return;
  }
  handleOpenWorkbench(resp.record);
}

function handleOpenWorkbench(row: any) {
  if (isReadonlyRecord(row)) {
    handleViewDetail(row);
    return;
  }
  activeWorkbenchId.value = row.id;
  viewMode.value = 'WORKBENCH';
}

async function handleExportCoa(row: any) {
  if (!row.id) return;
  const data = await exportIqcCoaWord(row.id);
  downloadFileFromBlobPart({
    fileName: buildIqcCoaWordFileName(row),
    source: data,
  });
}

function handleConfirmRetention(row: any) {
  const rule = resolveIqcRetentionRule(row);
  Modal.confirm({
    title: '补确认留样',
    content: `确认将检验单 ${row.iqcNo || '-'} 标记为已留样？物料种类：${rule.materialCategoryName}，留样数量：${rule.ruleDesc}。`,
    okText: '确认留样',
    okType: 'primary',
    onOk: async () => {
      await confirmIqcRetention({ id: row.id });
      message.success('已补确认留样');
      await gridApi.query();
    },
  });
}

function canConfirmRetention(row: any) {
  return (
    row?.id &&
    row.retentionStatus !== 'RETAINED' &&
    ['COMPLETED', 'FINISHED', 'REJECTED', 'WAITING_CONFIRM'].includes(
      row.status,
    )
  );
}

function rowActions(row: any) {
  const actions: any[] = [
    {
      label: '查看',
      type: 'link',
      icon: ACTION_ICON.VIEW,
      onClick: () => handleViewDetail(row),
    },
    {
      label: '审核',
      type: 'link',
      icon: 'lucide:badge-check',
      auth: ['mes:iqc:audit'],
      disabled: !canAudit(row),
      tooltip: canAudit(row) ? undefined : '仅“待审核”状态可执行审核确认',
      onClick: () => handleAudit(row),
    },
    {
      label: '导出 COA',
      type: 'link',
      icon: 'lucide:file-down',
      auth: ['mes:iqc:print'],
      onClick: () => handleExportCoa(row),
    },
  ];
  if (row.id && !isReadonlyRecord(row)) {
    return [
      ...actions,
      {
        label: '数据录入',
        type: 'link',
        icon: 'lucide:clipboard-pen-line',
        onClick: () => handleOpenWorkbench(row),
      },
    ];
  }
  return actions;
}
</script>

<template>
  <Page auto-content-height class="relative">
    <DetailModal />
    <QmsIqcScanResolveModal
      v-model:open="scanModalOpen"
      scene="LEDGER_TOOLBAR"
      @resolved="handleScanResolved"
    />

    <div v-show="viewMode === 'LEDGER'" class="flex h-full flex-col">
      <Grid table-title="IQC 进料检验记录台账">
        <template #toolbar-tools>
          <div class="ledger-toolbar-actions">
            <Button type="primary" @click="scanModalOpen = true">
              <IconifyIcon icon="lucide:scan-line" class="mr-1" />
              扫码填写
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
        <template #attachments="{ row }">
          <Button
            v-if="row.inspectionApplyAttachmentUrls?.length"
            type="link"
            size="small"
            class="!px-0"
            @click="handleViewDetail(row)"
          >
            {{ row.inspectionApplyAttachmentUrls.length }} 个
          </Button>
          <span v-else class="text-slate-400">无</span>
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
            拒收
          </Tag>
          <Tag v-else color="default" class="!m-0 border-none font-bold">
            {{ row.judgment === 'SKIP' ? '不判定' : '待判定' }}
          </Tag>
        </template>
        <template #status="{ row }">
          <Tag :color="statusColor(row)" class="!m-0 border-none font-bold">
            {{ statusLabel(row) }}
          </Tag>
        </template>

        <template #retentionStatus="{ row }">
          <Tag
            :color="retentionStatusColor(row.retentionStatus)"
            class="!m-0 border-none font-bold"
          >
            {{ retentionStatusLabel(row.retentionStatus) }}
          </Tag>
        </template>

        <template #standardNo="{ row }">
          <div class="flex items-center gap-1">
            <span class="font-mono">{{ row.standardNo || '-' }}</span>
            <Tag
              v-if="row.standardNo"
              :color="standardMatchModeColor(row.standardMatchMode)"
              class="!m-0 border-none"
            >
              {{ standardMatchModeLabel(row.standardMatchMode) }}
            </Tag>
          </div>
        </template>

        <template #actions="{ row }">
          <div class="iqc-row-actions">
            <TableAction :actions="rowActions(row)" />
            <Button
              v-if="canConfirmRetention(row)"
              type="link"
              class="!px-1"
              @click="handleConfirmRetention(row)"
            >
              <IconifyIcon icon="lucide:archive" class="mr-1" />
              补确认留样
            </Button>
          </div>
        </template>
      </Grid>
    </div>

    <WorkbenchMode
      v-if="viewMode === 'WORKBENCH'"
      :initial-record-id="activeWorkbenchId"
      @back-to-ledger="
        viewMode = 'LEDGER';
        activeWorkbenchId = undefined;
        gridApi.query();
      "
    />
  </Page>
</template>

<style scoped>
.ledger-toolbar-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.iqc-row-actions {
  display: inline-flex;
  flex-wrap: nowrap;
  align-items: center;
  white-space: nowrap;
}
</style>
