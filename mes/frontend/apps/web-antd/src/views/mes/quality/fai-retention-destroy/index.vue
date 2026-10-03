<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesFaiApi } from '#/api/mes/quality/fai';

import { computed, ref } from 'vue';

import { Page, useVbenModal } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import { Button, Input, message, Modal, RadioGroup, Tag } from 'ant-design-vue';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  destroyFaiRetention,
  getFaiRetentionExpiredPage,
} from '#/api/mes/quality/fai';
import { getRangePickerDefaultProps } from '#/utils';

import {
  faiStatusOptions,
  retentionDestroyStatusColor,
  retentionDestroyStatusLabel,
  retentionDestroyStatusOptions,
  retentionStatusColor,
  retentionStatusLabel,
  resolveFaiOperationLabel,
  sortFaiRecordsByStatus,
  submissionTypeOptions,
  useGridColumns,
  useGridFormSchema,
} from '../fai/data';
import DetailModalVue from '../fai/modules/detail-modal.vue';

defineOptions({ name: 'MesQualityFaiRetentionDestroy' });

const RETENTION_STATUS_RETAINED: MesFaiApi.RetentionStatus = 'RETAINED';
const DEFAULT_RETENTION_DESTROY_STATUS: MesFaiApi.RetentionDestroyStatus =
  'WAIT_DESTROY';
const checkedRows = ref<MesFaiApi.FaiRecord[]>([]);
const destroyModalOpen = ref(false);
const destroySaving = ref(false);
const destroyRemark = ref('');
const destroyTargetRows = ref<MesFaiApi.FaiRecord[]>([]);
const retentionDestroyStatusFilter = ref<MesFaiApi.RetentionDestroyStatus>(
  DEFAULT_RETENTION_DESTROY_STATUS,
);
const checkedDestroyableRows = computed(() =>
  checkedRows.value.filter((row) => row.retentionDestroyStatus !== 'DESTROYED'),
);

const searchFormFields = [
  'faiNo',
  'processCategory',
  'submissionType',
  'materialCode',
  'productModel',
  'productBatchNo',
  'retentionExpireTime',
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

function buildDestroySearchFormSchema(collapsed = false) {
  const schemaMap = new Map(
    useGridFormSchema()
      .filter((item) => item.fieldName !== 'retentionStatus')
      .map((item) => [item.fieldName, item]),
  );
  [
    {
      fieldName: 'retentionExpireTime',
      label: '过期时间',
      component: 'RangePicker',
      componentProps: { ...getRangePickerDefaultProps() },
    },
  ].forEach((item) => schemaMap.set(item.fieldName, item));

  const keepFields = new Set(
    searchFormFields.slice(0, collapsed ? getCollapsedKeepCount() : undefined),
  );
  const schema: ReturnType<typeof useGridFormSchema> = [];
  for (const fieldName of searchFormFields) {
    const item = schemaMap.get(fieldName);
    if (!item) continue;
    schema.push({
      ...item,
      hide: collapsed && !keepFields.has(item.fieldName),
    });
  }
  return schema;
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

function buildDestroyColumns() {
  const columns = [...(useGridColumns() || [])].filter(
    (column: any) => column.field !== 'action',
  );
  const firstColumn = columns[0] as any;
  if (firstColumn) {
    firstColumn.type = 'checkbox';
    firstColumn.width = 48;
  }
  const retentionIndex = columns.findIndex(
    (column: any) => column.field === 'retentionStatus',
  );
  columns.splice(Math.max(retentionIndex + 1, 0), 0,
    {
      field: 'retentionExpireTime',
      title: '留样过期时间',
      width: 170,
      align: 'center',
    },
    {
      field: 'retentionDestroyStatus',
      title: '报废状态',
      width: 110,
      align: 'center',
      slots: { default: 'retentionDestroyStatus' },
    },
    {
      field: 'retentionDestroyTime',
      title: '销毁时间',
      width: 170,
      align: 'center',
    },
    {
      field: 'retentionDestroyUserName',
      title: '销毁人',
      width: 110,
      align: 'center',
    },
  );
  columns.push({
    title: '操作',
    field: 'action',
    fixed: 'right',
    width: 220,
    align: 'center',
    slots: { default: 'actions' },
  });
  return columns;
}

function handleRowCheckboxChange({ records }: { records: MesFaiApi.FaiRecord[] }) {
  checkedRows.value = records || [];
}

const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: {
    collapsed: true,
    handleCollapsedChange: handleSearchCollapsedChange,
    schema: buildDestroySearchFormSchema(true),
  },
  gridOptions: {
    columns: buildDestroyColumns(),
    height: 'auto',
    keepSource: true,
    rowConfig: {
      keyField: 'id',
      isHover: true,
    },
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) => {
          const result = await getFaiRetentionExpiredPage({
            pageNo: page.currentPage,
            pageSize: page.pageSize,
            ...formValues,
            retentionDestroyStatus: retentionDestroyStatusFilter.value,
            retentionStatus: RETENTION_STATUS_RETAINED,
          });
          return {
            ...result,
            list: sortFaiRecordsByStatus(result.list || []),
          };
        },
      },
    },
  } as VxeTableGridOptions<MesFaiApi.FaiRecord>,
  gridEvents: {
    checkboxAll: handleRowCheckboxChange,
    checkboxChange: handleRowCheckboxChange,
  },
});

function handleViewDetail(row: MesFaiApi.FaiRecord) {
  detailModalApi.setData({ apiMode: 'RETENTION', id: row.id }).open();
}

async function handleRetentionDestroyStatusChange() {
  checkedRows.value = [];
  await gridApi.query();
}

function handleOpenDestroy(rows: MesFaiApi.FaiRecord[]) {
  const targets = rows.filter((row) => row.id && row.retentionDestroyStatus !== 'DESTROYED');
  if (targets.length === 0) {
    message.warning('请选择待报废的过期留样记录');
    return;
  }
  destroyTargetRows.value = targets;
  destroyRemark.value = '';
  destroyModalOpen.value = true;
}

async function handleConfirmDestroy() {
  const ids = destroyTargetRows.value.map((row) => row.id!).filter(Boolean);
  if (ids.length === 0) return;
  destroySaving.value = true;
  try {
    await destroyFaiRetention({
      ids,
      remark: destroyRemark.value,
    });
    message.success('销毁记录已保存');
    checkedRows.value = [];
    destroyModalOpen.value = false;
    await gridApi.query();
  } finally {
    destroySaving.value = false;
  }
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
    <Modal
      v-model:open="destroyModalOpen"
      title="销毁过期留样"
      :confirm-loading="destroySaving"
      @ok="handleConfirmDestroy"
    >
      <div class="space-y-3">
        <div class="text-sm text-gray-500">
          本次销毁 {{ destroyTargetRows.length }} 条过期留样记录
        </div>
        <Input.TextArea
          v-model:value="destroyRemark"
          :rows="4"
          placeholder="请输入销毁备注"
        />
      </div>
    </Modal>
    <div class="flex h-full flex-col">
      <Grid table-title="销毁样品台账">
        <template #table-title>
          <div class="destroy-ledger-title-bar">
            <span class="destroy-ledger-title">销毁样品台账</span>
            <div class="destroy-status-filter">
              <span class="destroy-status-filter__label">报废状态</span>
              <RadioGroup
                v-model:value="retentionDestroyStatusFilter"
                :options="retentionDestroyStatusOptions"
                button-style="solid"
                class="retention-destroy-status-filter"
                option-type="button"
                @change="handleRetentionDestroyStatusChange"
              />
            </div>
          </div>
        </template>
        <template #toolbar-tools>
          <div
            class="mr-2 flex flex-wrap items-center justify-end gap-2 border-r border-gray-200 pr-3"
          >
            <Button
              v-access:code="['mes:fai-retention-destroy:destroy']"
              type="primary"
              :disabled="checkedDestroyableRows.length === 0"
              @click="handleOpenDestroy(checkedDestroyableRows)"
            >
              <IconifyIcon icon="lucide:trash-2" class="mr-1" /> 批量销毁
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
        <template #retentionDestroyStatus="{ row }">
          <Tag
            :color="retentionDestroyStatusColor(row.retentionDestroyStatus)"
            class="!m-0"
          >
            {{ retentionDestroyStatusLabel(row.retentionDestroyStatus) }}
          </Tag>
        </template>
        <template #actions="{ row }">
          <TableAction
            :actions="[
              {
                label: '销毁',
                type: 'link',
                danger: true,
                icon: 'lucide:trash-2',
                auth: ['mes:fai-retention-destroy:destroy'],
                disabled: row.retentionDestroyStatus === 'DESTROYED',
                onClick: () => handleOpenDestroy([row]),
              },
              {
                label: '查看',
                type: 'link',
                icon: ACTION_ICON.VIEW,
                auth: ['mes:fai-retention-destroy:query'],
                onClick: () => handleViewDetail(row),
              },
            ]"
          />
        </template>
      </Grid>
    </div>
  </Page>
</template>

<style scoped>
.destroy-ledger-title-bar {
  display: flex;
  align-items: center;
  gap: 28px;
  min-height: 34px;
  padding-left: 4px;
}

.destroy-ledger-title {
  font-size: 1rem;
  font-weight: 600;
  white-space: nowrap;
}

.destroy-status-filter {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.destroy-status-filter__label {
  color: hsl(var(--foreground));
  font-size: 14px;
  font-weight: 600;
  white-space: nowrap;
}

:deep(.retention-destroy-status-filter) {
  display: inline-flex;
  gap: 6px;
}

:deep(.retention-destroy-status-filter .ant-radio-button-wrapper) {
  min-width: 78px;
  border-inline-start-width: 1px;
  border-radius: 999px;
  text-align: center;
}

:deep(.retention-destroy-status-filter .ant-radio-button-wrapper::before) {
  display: none;
}
</style>
