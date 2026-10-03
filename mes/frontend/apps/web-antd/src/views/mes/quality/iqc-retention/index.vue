<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesIqcApi } from '#/api/mes/quality/iqc';

import { Page, useVbenModal } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';
import { downloadFileFromBlobPart } from '@vben/utils';

import { Button, Tag } from 'ant-design-vue';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  exportIqcRetentionExcel,
  getIqcRetentionPage,
} from '#/api/mes/quality/iqc';

import { useGridColumns, useGridFormSchema } from '../iqc/data';
import DetailModalVue from '../iqc/modules/detail-modal.vue';

defineOptions({ name: 'MesQualityIqcRetention' });

const RETENTION_STATUS_RETAINED: MesIqcApi.RetentionStatus = 'RETAINED';

const [DetailModal, detailModalApi] = useVbenModal({
  connectedComponent: DetailModalVue,
  destroyOnClose: true,
});

const searchFormFields = [
  'materialCode',
  'batchNo',
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

function buildSearchFormSchema(collapsed = false) {
  const keepFields = new Set(
    searchFormFields.slice(0, collapsed ? getCollapsedKeepCount() : undefined),
  );
  return useGridFormSchema().map((item) => ({
    ...item,
    hide: collapsed && !keepFields.has(item.fieldName),
  }));
}

function buildRetentionColumns() {
  const columns = [...(useGridColumns() || [])].filter(
    (column: any) => column.field !== 'retentionStatus',
  );
  const actionColumn = columns.find((column: any) => column.field === 'action');
  if (actionColumn) actionColumn.width = 100;
  const judgmentIndex = columns.findIndex(
    (column: any) => column.field === 'judgment',
  );
  columns.splice(
    Math.max(judgmentIndex + 1, 0),
    0,
    {
      field: 'recheckFlag',
      title: '是否复检',
      width: 90,
      align: 'center',
      slots: { default: 'recheckFlag' },
    },
    {
      field: 'retentionStatus',
      title: '留样状态',
      width: 110,
      align: 'center',
      slots: { default: 'retentionStatus' },
    },
    {
      field: 'retentionMaterialCategoryName',
      title: '留样物料种类',
      width: 130,
      align: 'center',
      formatter: ({ cellValue }) => cellValue || '-',
    },
    {
      field: 'retentionRuleDesc',
      title: '留样数量',
      width: 140,
      align: 'center',
      formatter: ({ row }) =>
        row.retentionRuleDesc ||
        (row.retentionQty
          ? `${row.retentionQty}${row.retentionUnit || ''}/每来料批次`
          : '-'),
    },
    {
      field: 'retentionConfirmTime',
      title: '留样确认时间',
      width: 170,
      align: 'center',
    },
    {
      field: 'retentionExpireTime',
      title: '过期时间',
      width: 170,
      align: 'center',
    },
  );
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
        query: async ({ page }, formValues) =>
          await getIqcRetentionPage({
            pageNo: page.currentPage,
            pageSize: page.pageSize,
            ...formValues,
            retentionStatus: RETENTION_STATUS_RETAINED,
          }),
      },
    },
  } as VxeTableGridOptions<MesIqcApi.IqcRecord>,
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

function handleViewDetail(row: MesIqcApi.IqcRecord) {
  detailModalApi.setData({ id: row.id }).open();
}

async function handleExport() {
  const formValues = await gridApi.formApi.getValues();
  const data = await exportIqcRetentionExcel({
    ...formValues,
    retentionStatus: RETENTION_STATUS_RETAINED,
  });
  downloadFileFromBlobPart({ fileName: 'IQC进料留样记录.xls', source: data });
}

function retentionStatusLabel(value?: string) {
  if (value === 'RETAINED') return '已留样';
  return '未留样';
}

function retentionStatusColor(value?: string) {
  if (value === 'RETAINED') return 'success';
  if (value === 'NOT_RETAINED') return 'default';
  return 'warning';
}
</script>

<template>
  <Page auto-content-height class="relative">
    <DetailModal />
    <div class="flex h-full flex-col">
      <Grid table-title="进料留样记录">
        <template #toolbar-tools>
          <div
            class="mr-2 flex flex-wrap items-center justify-end gap-2 border-r border-gray-200 pr-3"
          >
            <Button
              v-access:code="['mes:iqc-retention:export']"
              @click="handleExport"
            >
              <IconifyIcon icon="lucide:download" class="mr-1" /> 导出
            </Button>
            <Button @click="gridApi.query()">
              <IconifyIcon icon="lucide:refresh-cw" class="mr-1" /> 刷新
            </Button>
          </div>
        </template>
        <template #iqcNo="{ row }">
          <span class="font-mono font-bold text-indigo-700">{{
            row.iqcNo
          }}</span>
        </template>
        <template #attachments="{ row }">
          <Tag
            :color="
              row.inspectionApplyAttachmentUrls?.length ? 'blue' : 'default'
            "
            class="!m-0"
          >
            {{ row.inspectionApplyAttachmentUrls?.length || 0 }}
          </Tag>
        </template>
        <template #standardNo="{ row }">
          <span>{{ row.standardNo || '未选择' }}</span>
        </template>
        <template #status="{ row }">
          <Tag
            :color="
              row.status === 'REJECTED'
                ? 'error'
                : row.status === 'COMPLETED'
                  ? 'success'
                  : 'processing'
            "
            class="!m-0"
          >
            {{ row.status || '-' }}
          </Tag>
        </template>
        <template #judgment="{ row }">
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
            {{
              row.judgment === 'OK'
                ? '合格'
                : row.judgment === 'NG'
                  ? '拒收'
                  : '待判定'
            }}
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
                auth: ['mes:iqc-retention:query'],
                onClick: () => handleViewDetail(row),
              },
            ]"
          />
        </template>
      </Grid>
    </div>
  </Page>
</template>
