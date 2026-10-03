<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesIqcApi } from '#/api/mes/quality/iqc';

import { computed, ref } from 'vue';

import { Page, useVbenModal } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import { Button, Input, message, Modal, RadioGroup, Tag } from 'ant-design-vue';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  destroyIqcRetention,
  getIqcRetentionExpiredPage,
} from '#/api/mes/quality/iqc';
import { getRangePickerDefaultProps } from '#/utils';

import { useGridColumns, useGridFormSchema } from '../iqc/data';
import DetailModalVue from '../iqc/modules/detail-modal.vue';

defineOptions({ name: 'MesQualityIqcRetentionDestroy' });

const RETENTION_STATUS_RETAINED: MesIqcApi.RetentionStatus = 'RETAINED';
const DEFAULT_DESTROY_STATUS: MesIqcApi.RetentionDestroyStatus = 'WAIT_DESTROY';
const checkedRows = ref<MesIqcApi.IqcRecord[]>([]);
const destroyModalOpen = ref(false);
const destroySaving = ref(false);
const destroyRemark = ref('');
const destroyTargetRows = ref<MesIqcApi.IqcRecord[]>([]);
const retentionDestroyStatusFilter = ref<MesIqcApi.RetentionDestroyStatus>(
  DEFAULT_DESTROY_STATUS,
);
const checkedDestroyableRows = computed(() =>
  checkedRows.value.filter((row) => row.retentionDestroyStatus !== 'DESTROYED'),
);

const destroyStatusOptions = [
  { label: '待销毁', value: 'WAIT_DESTROY' },
  { label: '已销毁', value: 'DESTROYED' },
];

const searchFormFields = [
  'materialCode',
  'batchNo',
  'iqcNo',
  'receiptNo',
  'supplierName',
  'retentionExpireTime',
  'status',
  'judgment',
  'inspectionTime',
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
  const schemaMap = new Map(
    useGridFormSchema().map((item) => [item.fieldName, item]),
  );
  schemaMap.set('retentionExpireTime', {
    fieldName: 'retentionExpireTime',
    label: '过期时间',
    component: 'RangePicker',
    componentProps: { ...getRangePickerDefaultProps() },
  });
  const keepFields = new Set(
    searchFormFields.slice(0, collapsed ? getCollapsedKeepCount() : undefined),
  );
  return searchFormFields
    .map((fieldName) => schemaMap.get(fieldName))
    .filter(Boolean)
    .map((item: any) => ({
      ...item,
      hide: collapsed && !keepFields.has(item.fieldName),
    }));
}

function buildColumns() {
  const columns = [...(useGridColumns() || [])].filter(
    (column: any) => column.field !== 'action',
  );
  const firstColumn = columns[0] as any;
  if (firstColumn) {
    firstColumn.type = 'checkbox';
    firstColumn.width = 48;
  }
  const judgmentIndex = columns.findIndex(
    (column: any) => column.field === 'judgment',
  );
  columns.splice(
    Math.max(judgmentIndex + 1, 0),
    0,
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
      field: 'retentionExpireTime',
      title: '留样过期时间',
      width: 170,
      align: 'center',
    },
    {
      field: 'retentionDestroyStatus',
      title: '销毁状态',
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
    width: 210,
    align: 'center',
    slots: { default: 'actions' },
  });
  return columns;
}

function handleRowCheckboxChange({
  records,
}: {
  records: MesIqcApi.IqcRecord[];
}) {
  checkedRows.value = records || [];
}

const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: {
    collapsed: true,
    handleCollapsedChange,
    schema: buildSearchFormSchema(true),
  },
  gridOptions: {
    columns: buildColumns(),
    height: 'auto',
    keepSource: true,
    rowConfig: { keyField: 'id', isHover: true },
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) =>
          await getIqcRetentionExpiredPage({
            pageNo: page.currentPage,
            pageSize: page.pageSize,
            ...formValues,
            retentionDestroyStatus: retentionDestroyStatusFilter.value,
            retentionStatus: RETENTION_STATUS_RETAINED,
          }),
      },
    },
  } as VxeTableGridOptions<MesIqcApi.IqcRecord>,
  gridEvents: {
    checkboxAll: handleRowCheckboxChange,
    checkboxChange: handleRowCheckboxChange,
  },
});

function handleCollapsedChange(collapsed: boolean) {
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

async function handleDestroyStatusChange() {
  checkedRows.value = [];
  await gridApi.query();
}

function handleOpenDestroy(rows: MesIqcApi.IqcRecord[]) {
  const targets = rows.filter(
    (row) => row.id && row.retentionDestroyStatus !== 'DESTROYED',
  );
  if (targets.length === 0) {
    message.warning('请选择待销毁的过期留样记录');
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
    await destroyIqcRetention({ ids, remark: destroyRemark.value });
    message.success('销毁记录已保存');
    checkedRows.value = [];
    destroyModalOpen.value = false;
    await gridApi.query();
  } finally {
    destroySaving.value = false;
  }
}

function destroyStatusLabel(value?: string) {
  return value === 'DESTROYED' ? '已销毁' : '待销毁';
}

function retentionStatusLabel(value?: string) {
  return value === 'RETAINED' ? '已留样' : '未留样';
}

function retentionStatusColor(value?: string) {
  return value === 'RETAINED' ? 'success' : 'default';
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
      <Grid table-title="进料销毁样品台账">
        <template #table-title>
          <div class="destroy-ledger-title-bar">
            <span class="destroy-ledger-title">进料销毁样品台账</span>
            <div class="destroy-status-filter">
              <span class="destroy-status-filter__label">销毁状态</span>
              <RadioGroup
                v-model:value="retentionDestroyStatusFilter"
                :options="destroyStatusOptions"
                button-style="solid"
                class="retention-destroy-status-filter"
                option-type="button"
                @change="handleDestroyStatusChange"
              />
            </div>
          </div>
        </template>
        <template #toolbar-tools>
          <div
            class="mr-2 flex flex-wrap items-center justify-end gap-2 border-r border-gray-200 pr-3"
          >
            <Button
              v-access:code="['mes:iqc-retention-destroy:destroy']"
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
        <template #retentionDestroyStatus="{ row }">
          <Tag
            :color="
              row.retentionDestroyStatus === 'DESTROYED' ? 'default' : 'warning'
            "
            class="!m-0"
          >
            {{ destroyStatusLabel(row.retentionDestroyStatus) }}
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
                label: '销毁',
                type: 'link',
                danger: true,
                icon: 'lucide:trash-2',
                auth: ['mes:iqc-retention-destroy:destroy'],
                disabled: row.retentionDestroyStatus === 'DESTROYED',
                onClick: () => handleOpenDestroy([row]),
              },
              {
                label: '查看',
                type: 'link',
                icon: ACTION_ICON.VIEW,
                auth: ['mes:iqc-retention-destroy:query'],
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
