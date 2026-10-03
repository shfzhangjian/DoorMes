<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesFqcApi } from '#/api/mes/quality/fqc';

import { ref } from 'vue';

import { Page, useVbenModal } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import { Button, Input, message, Modal, Radio, Tag } from 'ant-design-vue';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import { auditFqcProgramEntry, getFqcPage } from '#/api/mes/quality/fqc';

import {
  FqcStatusOptions,
  resolveFqcOperationLabel,
  sortFqcRecordsByStatus,
  submissionTypeOptions,
  useGridColumns,
  useGridFormSchema,
} from './data';
import DetailModalVue from './modules/detail-modal.vue';
import QmsFqcCreateModal from './modules/qms-fqc-create-modal.vue';
import QmsFqcScanResolveModal from './modules/qms-fqc-scan-resolve-modal.vue';
import WorkbenchMode from './modules/workbench.vue';

defineOptions({ name: 'MesQualityFqc' });

const viewMode = ref<'LEDGER' | 'OVERVIEW'>('LEDGER');
const activeOverviewId = ref<number>();
const activeOverviewRecord = ref<MesFqcApi.FqcRecord>();
const scanModalOpen = ref(false);
const auditModalOpen = ref(false);
const auditTargetRecord = ref<MesFqcApi.FqcRecord>();
const auditResult = ref<MesFqcApi.AuditResult>('PASS');
const rejectReason = ref('');
const [DetailModal, detailModalApi] = useVbenModal({
  connectedComponent: DetailModalVue,
  destroyOnClose: true,
});
const [CreateModal, createModalApi] = useVbenModal({
  connectedComponent: QmsFqcCreateModal,
  destroyOnClose: true,
});

const searchFormFields = [
  'fqcNo',
  'processCategory',
  'submissionType',
  'materialCode',
  'productModel',
  'productBatchNo',
  'status',
  'judgment',
  'recheckFlag',
  'submissionTime',
  'submitterName',
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
    rowConfig: { isHover: true },
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) => {
          const result = await getFqcPage({
            pageNo: page.currentPage,
            pageSize: page.pageSize,
            ...formValues,
          });
          return {
            ...result,
            list: sortFqcRecordsByStatus(result.list || []),
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

function handleViewDetail(row: any) {
  detailModalApi.setData({ id: row.id }).open();
}

function handleOpenOverview(row: MesFqcApi.FqcRecord) {
  activeOverviewId.value = row.id;
  activeOverviewRecord.value = row;
  viewMode.value = 'OVERVIEW';
}

function handleScanResolved(resp: MesFqcApi.FqcScanResp) {
  if (!resp.record?.id) {
    if (resp.message) message.info(resp.message);
    return;
  }
  if (resp.openTarget === 'REPORT') {
    handleViewDetail(resp.record);
    return;
  }
  activeOverviewId.value = resp.record.id;
  activeOverviewRecord.value = resp.record;
  viewMode.value = 'OVERVIEW';
}

function handleCreateSuccess(record: MesFqcApi.FqcRecord) {
  gridApi.query();
  handleOpenOverview(record);
}

function canAudit(row: MesFqcApi.FqcRecord) {
  return row.status === 'WAITING_QA';
}

function handleOpenAudit(row: MesFqcApi.FqcRecord) {
  if (!canAudit(row)) {
    message.warning('FQC单提交检测结果后才允许审核');
    return;
  }
  auditTargetRecord.value = row;
  auditResult.value = 'PASS';
  rejectReason.value = '';
  auditModalOpen.value = true;
}

function resetAuditModal() {
  auditTargetRecord.value = undefined;
  auditResult.value = 'PASS';
  rejectReason.value = '';
}

async function handleAuditSubmit() {
  if (!auditTargetRecord.value?.id) return;
  if (auditResult.value === 'REJECT' && !rejectReason.value.trim()) {
    message.warning('请填写驳回原因');
    return;
  }
  await auditFqcProgramEntry({
    id: auditTargetRecord.value.id,
    auditResult: auditResult.value,
    rejectReason: ['FqcL', 'REJECT'].includes(auditResult.value)
      ? rejectReason.value.trim() || undefined
      : undefined,
  });
  message.success(resolveAuditSuccessMessage(auditResult.value));
  auditModalOpen.value = false;
  resetAuditModal();
  gridApi.query();
}

function resolveAuditSuccessMessage(result: MesFqcApi.AuditResult) {
  if (result === 'PASS') return 'FQC单已审核通过';
  if (result === 'FqcL') return 'FQC单已判定不合格';
  return 'FQC单已退回修改';
}

function optionLabel(
  options: Array<{ label: string; value: string }>,
  value?: string,
) {
  return options.find((item) => item.value === value)?.label || '';
}

function isInspected(row: MesFqcApi.FqcRecord) {
  return row.status === 'COMPLETED' || row.status === 'REJECTED';
}

function statusLabel(row: MesFqcApi.FqcRecord) {
  if (row.status === 'REJECTED' && row.judgment === 'NG') return '不合格';
  return (
    FqcStatusOptions.find((item) => item.value === row.status)?.label ||
    row.status ||
    '-'
  );
}

function statusColor(row: MesFqcApi.FqcRecord) {
  if (row.status === 'COMPLETED') return 'success';
  if (row.status === 'REJECTED' || row.status === 'CANCELED') return 'error';
  if (row.status === 'WAITING_QA') return 'purple';
  if (row.status === 'PENDING') return 'warning';
  return isInspected(row) ? 'success' : 'blue';
}

function qualifiedLabel(row: MesFqcApi.FqcRecord) {
  if (row.judgment === 'OK') return '合格';
  if (row.judgment === 'NG') return '不合格';
  return '待判定';
}
</script>

<template>
  <Page auto-content-height class="relative">
    <DetailModal />
    <CreateModal @success="handleCreateSuccess" />
    <QmsFqcScanResolveModal
      v-model:open="scanModalOpen"
      scene="LEDGER_TOOLBAR"
      @resolved="handleScanResolved"
    />
    <div v-show="viewMode === 'LEDGER'" class="flex h-full flex-col">
      <Grid table-title="成品检验记录">
        <template #toolbar-tools>
          <div
            class="mr-2 flex flex-wrap items-center justify-end gap-2 border-r border-gray-200 pr-3"
          >
            <Button
              v-access:code="['mes:fqc:create']"
              type="primary"
              @click="createModalApi.open()"
            >
              <IconifyIcon icon="lucide:plus" class="mr-1" /> 新增
            </Button>
            <Button
              v-access:code="['mes:fqc:workbench']"
              type="primary"
              @click="scanModalOpen = true"
            >
              <IconifyIcon icon="lucide:scan-line" class="mr-1" /> 扫码填写
            </Button>
            <Button @click="gridApi.query()">
              <IconifyIcon icon="lucide:refresh-cw" class="mr-1" /> 刷新
            </Button>
          </div>
        </template>
        <template #fqcNo="{ row }">
          <span class="font-mono font-bold text-indigo-700">{{
            row.fqcNo
          }}</span>
        </template>
        <template #processCategory="{ row }">
          <Tag color="blue" class="!m-0">
            {{ resolveFqcOperationLabel(row) }}
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
            {{ row.recheckFlag ? '复检' : '原检' }}
          </Tag>
        </template>
        <template #actions="{ row }">
          <TableAction
            :actions="[
              {
                label: '查看',
                type: 'link',
                icon: ACTION_ICON.VIEW,
                auth: ['mes:fqc:query'],
                onClick: () => handleViewDetail(row),
              },
              {
                label: '数据录入',
                type: 'link',
                icon: 'lucide:keyboard',
                auth: ['mes:fqc:workbench'],
                onClick: () => handleOpenOverview(row),
              },
              {
                label: '审核',
                type: 'link',
                icon: 'lucide:badge-check',
                auth: ['mes:fqc:confirm'],
                disabled: row.status !== 'WAITING_QA',
                onClick: () => handleOpenAudit(row),
              },
            ]"
          />
        </template>
      </Grid>
    </div>

    <Modal
      v-model:open="auditModalOpen"
      title="FQC单整单审核"
      @cancel="resetAuditModal"
      @ok="handleAuditSubmit"
    >
      <div class="space-y-4">
        <div class="text-sm text-slate-600">
          {{ auditTargetRecord?.fqcNo || '-' }}
        </div>
        <Radio.Group v-model:value="auditResult">
          <Radio value="PASS">通过</Radio>
          <Radio value="REJECT">驳回</Radio>
          <Radio value="FqcL">不合格</Radio>
        </Radio.Group>
        <Input.TextArea
          v-if="['FqcL', 'REJECT'].includes(auditResult)"
          v-model:value="rejectReason"
          :maxlength="300"
          :rows="4"
          :placeholder="
            auditResult === 'FqcL'
              ? '请输入不合格说明（选填）'
              : '请输入驳回原因'
          "
          show-count
        />
      </div>
    </Modal>

    <WorkbenchMode
      v-if="viewMode === 'OVERVIEW'"
      :initial-record-id="activeOverviewId"
      :initial-record="activeOverviewRecord"
      @back-to-ledger="
        viewMode = 'LEDGER';
        activeOverviewRecord = undefined;
        gridApi.query();
      "
    />
  </Page>
</template>
