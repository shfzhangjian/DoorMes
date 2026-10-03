<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesFaiApi } from '#/api/mes/quality/fai';

import { Page, useVbenModal } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';
import { downloadFileFromBlobPart } from '@vben/utils';

import { Button, Tag } from 'ant-design-vue';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  exportFaiRetentionExcel,
  getFaiRetentionPage,
} from '#/api/mes/quality/fai';

import {
  faiStatusOptions,
  resolveFaiOperationLabel,
  retentionStatusColor,
  retentionStatusLabel,
  sortFaiRecordsByStatus,
  submissionTypeOptions,
  useGridColumns,
  useGridFormSchema,
} from '../fai/data';
import DetailModalVue from '../fai/modules/detail-modal.vue';

defineOptions({ name: 'MesQualityFaiRetention' });

const RETENTION_STATUS_RETAINED: MesFaiApi.RetentionStatus = 'RETAINED';

const searchFormFields = [
  'faiNo',
  'processCategory',
  'submissionType',
  'materialCode',
  'productModel',
  'productBatchNo',
  'status',
  'judgment',
  'submissionTime',
  'inspectionTime',
  'submitterName',
];

const [DetailModal, detailModalApi] = useVbenModal({
  connectedComponent: DetailModalVue,
  destroyOnClose: true,
});

function getCollapsedKeepCount() {
  if (window.innerWidth < 768) return 1;
  if (window.innerWidth < 1024) return 2;
  return 3;
}

function buildSearchFormSchema(collapsed = false) {
  const keepFields = new Set(
    searchFormFields.slice(0, collapsed ? getCollapsedKeepCount() : undefined),
  );
  return useGridFormSchema()
    .filter((item) => item.fieldName !== 'retentionStatus')
    .map((item) => ({
      ...item,
      hide: collapsed && !keepFields.has(item.fieldName),
    }));
}

function buildRetentionColumns() {
  const columns = [...(useGridColumns() || [])];
  const retentionIndex = columns.findIndex(
    (column: any) => column.field === 'retentionStatus',
  );
  columns.splice(Math.max(retentionIndex + 1, 0), 0, {
    field: 'retentionExpireTime',
    title: '过期时间',
    width: 170,
    align: 'center',
  });
  const actionColumn = columns.find((column: any) => column.field === 'action');
  if (actionColumn) {
    actionColumn.width = 100;
  }
  return columns;
}

const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: {
    collapsed: true,
    handleCollapsedChange: handleSearchCollapsedChange,
    schema: buildSearchFormSchema(true),
  },
  gridOptions: {
    columns: buildRetentionColumns(),
    height: 'auto',
    keepSource: true,
    rowConfig: { isHover: true },
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) => {
          const result = await getFaiRetentionPage({
            pageNo: page.currentPage,
            pageSize: page.pageSize,
            ...formValues,
            retentionStatus: RETENTION_STATUS_RETAINED,
          });
          return {
            ...result,
            list: sortFaiRecordsByStatus(result.list || []),
          };
        },
      },
    },
  } as VxeTableGridOptions<any>,
});

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

function handleViewDetail(row: MesFaiApi.FaiRecord) {
  detailModalApi.setData({ apiMode: 'RETENTION', id: row.id }).open();
}

async function handleExport() {
  const formValues = await gridApi.formApi.getValues();
  const data = await exportFaiRetentionExcel({
    ...formValues,
    retentionStatus: RETENTION_STATUS_RETAINED,
  });
  downloadFileFromBlobPart({ fileName: '首件留样记录.xls', source: data });
}

function optionLabel(
  options: Array<{ label: string; value: string }>,
  value?: string,
) {
  return options.find((item) => item.value === value)?.label || '';
}

function statusLabel(row: MesFaiApi.FaiRecord) {
  if (row.status === 'REJECTED' && row.judgment === 'NG') return '不合格';
  return (
    faiStatusOptions.find((item) => item.value === row.status)?.label ||
    row.status ||
    '-'
  );
}

function statusColor(row: MesFaiApi.FaiRecord) {
  if (row.status === 'COMPLETED') return 'success';
  if (row.status === 'REJECTED' || row.status === 'CANCELED') return 'error';
  if (row.status === 'WAITING_QA') return 'purple';
  if (row.status === 'PENDING') return 'warning';
  return 'blue';
}

function qualifiedLabel(row: MesFaiApi.FaiRecord) {
  if (row.judgment === 'OK') return '合格';
  if (row.judgment === 'NG') return '不合格';
  return '待判定';
}
</script>

<template>
  <Page auto-content-height class="relative">
    <DetailModal />
    <div class="flex h-full flex-col">
      <Grid table-title="首件留样记录">
        <template #toolbar-tools>
          <div
            class="mr-2 flex flex-wrap items-center justify-end gap-2 border-r border-gray-200 pr-3"
          >
            <Button
              v-access:code="['mes:fai-retention:export']"
              @click="handleExport"
            >
              <IconifyIcon icon="lucide:download" class="mr-1" /> 导出
            </Button>
            <Button @click="gridApi.query()">
              <IconifyIcon icon="lucide:refresh-cw" class="mr-1" /> 刷新
            </Button>
          </div>
        </template>
        <template #faiNo="{ row }">
          <span class="font-mono font-bold text-indigo-700">{{
            row.faiNo
          }}</span>
        </template>
        <template #processCategory="{ row }">
          <Tag color="blue" class="!m-0">
            {{ resolveFaiOperationLabel(row) }}
          </Tag>
        </template>
        <template #submissionType="{ row }">
          <Tag color="purple" class="!m-0">
            {{ optionLabel(submissionTypeOptions, row.submissionType) || '-' }}
          </Tag>
        </template>
        <template #inspectionStatus="{ row }">
          <Tag :color="statusColor(row)" class="!m-0">
            {{ statusLabel(row) }}
          </Tag>
        </template>
        <template #qualifiedStatus="{ row }">
          <Tag
            :color="
              row.judgment === 'OK'
                ? 'success'
                : row.judgment === 'NG'
                  ? 'error'
                  : 'default'
            "
            class="!m-0"
          >
            {{ qualifiedLabel(row) }}
          </Tag>
        </template>
        <template #recheckFlag="{ row }">
          <Tag :color="row.recheckFlag ? 'orange' : 'default'" class="!m-0">
            {{ row.recheckFlag ? '是' : '否' }}
          </Tag>
        </template>
        <template #retentionStatus="{ row }">
          <Tag :color="retentionStatusColor(row.retentionStatus)" class="!m-0">
            {{ retentionStatusLabel(row.retentionStatus) }}
          </Tag>
        </template>
        <template #actions="{ row }">
          <TableAction
            :actions="[
              {
                label: '查看',
                type: 'link',
                icon: ACTION_ICON.VIEW,
                auth: ['mes:fai-retention:query'],
                onClick: () => handleViewDetail(row),
              },
            ]"
          />
        </template>
      </Grid>
    </div>
  </Page>
</template>
