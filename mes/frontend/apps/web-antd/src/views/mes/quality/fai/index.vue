<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesFaiApi } from '#/api/mes/quality/fai';

import { nextTick, onMounted, ref, watch } from 'vue';
import { useRoute } from 'vue-router';

import { Page, useVbenModal } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import { Button, Input, message, Modal, Tag } from 'ant-design-vue';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  applyFaiRecheck,
  auditFaiRecheckApply,
  getFaiPage,
  oneClickFailFai,
  oneClickPassFai,
  resolveFaiDisplayBatchNo,
} from '#/api/mes/quality/fai';
import StationFormRuntimeFillModal from '#/views/mes/hc/stationform/modules/runtime-fill-modal.vue';

import EnvironmentDailyRecordModal from '../shared/EnvironmentDailyRecordModal.vue';
import SelfCheckRecordRuntimeViewModal from '../shared/SelfCheckRecordRuntimeViewModal.vue';
import {
  ENVIRONMENT_WORKSHOPS,
  useEnvironmentDailyGuard,
} from '../shared/environmentDailyGuard';
import {
  faiStatusOptions,
  resolveFaiInspectionTypeMeta,
  resolveFaiOperationLabel,
  retentionStatusColor,
  retentionStatusLabel,
  sortFaiRecordsByStatus,
  submissionTypeOptions,
  useGridColumns,
  useGridFormSchema,
} from './data';
import DetailModalVue from './modules/detail-modal.vue';
import QmsFaiCreateModal from './modules/qms-fai-create-modal.vue';
import QmsFaiScanResolveModal from './modules/qms-fai-scan-resolve-modal.vue';
import WorkbenchMode from './modules/workbench.vue';
import {
  printFaiInspectionSheet,
  printFaiInspectionSheetById,
} from './shared/faiPrint';
import { useFaiProcessSelfCheckGuard } from './shared/faiSelfCheck';

import './shared/faiSelfCheckRuntime.css';

defineOptions({ name: 'MesQualityFai' });

const route = useRoute();
const viewMode = ref<'LEDGER' | 'OVERVIEW'>('LEDGER');
const activeOverviewId = ref<number>();
const activeOverviewRecord = ref<MesFaiApi.FaiRecord>();
const scanModalOpen = ref(false);
const recheckApplyOpen = ref(false);
const recheckApplyRecord = ref<MesFaiApi.FaiRecord>();
const recheckApplyReason = ref('');
const recheckApplySubmitting = ref(false);
const recheckAuditOpen = ref(false);
const recheckAuditRecord = ref<MesFaiApi.FaiRecord>();
const recheckAuditOpinion = ref('');
const recheckAuditSubmitting = ref(false);
const {
  checking: selfCheckChecking,
  ensureReady: ensureFaiProcessSelfCheckReady,
  handleRuntimeSuccess: handleSelfCheckRuntimeSuccess,
  runtimeForm: selfCheckRuntimeForm,
  runtimeInitialParams: selfCheckRuntimeInitialParams,
  runtimeOpen: selfCheckRuntimeOpen,
  runtimeRecord: selfCheckRuntimeRecord,
  runtimeRecordOpen: selfCheckRuntimeRecordOpen,
} = useFaiProcessSelfCheckGuard();
const {
  ensureReady: ensureTodayEnvironmentReady,
  goFill: goFillEnvironment,
  handleRecordSaved: handleEnvironmentRecordSaved,
  handleStatusClick: handleEnvironmentStatusClick,
  loading: environmentChecking,
  recordOpen: environmentRecordOpen,
  refresh: refreshTodayEnvironment,
  status: environmentStatus,
  todayRecord: todayEnvironmentRecord,
  warnIfAbnormal: warnEnvironmentIfAbnormal,
} = useEnvironmentDailyGuard(ENVIRONMENT_WORKSHOPS.FINAL_INSPECTION_ROOM);
const [DetailModal, detailModalApi] = useVbenModal({
  connectedComponent: DetailModalVue,
  destroyOnClose: true,
});
const [CreateModal, createModalApi] = useVbenModal({
  connectedComponent: QmsFaiCreateModal,
  destroyOnClose: true,
});

const searchFormFields = [
  'faiNo',
  'processCategory',
  'submissionType',
  'materialCode',
  'productModel',
  'productBatchNo',
  'status',
  'judgment',
  'recheckFlag',
  'itemRecheckStatusFilter',
  'retentionStatus',
  'submissionTime',
  'inspectionTime',
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
          const result = await getFaiPage({
            pageNo: page.currentPage,
            pageSize: page.pageSize,
            ...formValues,
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

function getRouteQueryString(value: unknown) {
  if (Array.isArray(value)) {
    return String(value[0] || '');
  }
  return typeof value === 'string' ? value : '';
}

async function applyProcessCategoryFromRoute() {
  const processCategory = getRouteQueryString(route.query.processCategory);
  const faiNo = getRouteQueryString(route.query.faiNo);
  await gridApi.formApi.setValues({
    faiNo: faiNo || undefined,
    processCategory: processCategory || undefined,
  });
  await gridApi.query();
}

onMounted(() => {
  void refreshTodayEnvironment();
  if (!route.query.processCategory && !route.query.faiNo) {
    return;
  }
  void nextTick(applyProcessCategoryFromRoute);
});

watch(
  () => [route.query.processCategory, route.query.faiNo],
  () => {
    void applyProcessCategoryFromRoute();
  },
);

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

function openOverview(row: MesFaiApi.FaiRecord) {
  activeOverviewId.value = row.id;
  activeOverviewRecord.value = row;
  viewMode.value = 'OVERVIEW';
}

async function handleOpenOverview(row: MesFaiApi.FaiRecord) {
  if (!(await ensureTodayEnvironmentReady())) return;
  warnEnvironmentIfAbnormal();
  await ensureFaiProcessSelfCheckReady(() => openOverview(row));
}

async function handleOpenScan() {
  if (!(await ensureTodayEnvironmentReady())) return;
  warnEnvironmentIfAbnormal();
  await ensureFaiProcessSelfCheckReady(() => {
    scanModalOpen.value = true;
  });
}

async function handleScanResolved(resp: MesFaiApi.FaiScanResp) {
  if (!resp.record?.id) {
    if (resp.message) message.info(resp.message);
    return;
  }
  if (resp.openTarget === 'REPORT') {
    handleViewDetail(resp.record);
    return;
  }
  if (!(await ensureTodayEnvironmentReady())) return;
  warnEnvironmentIfAbnormal();
  await ensureFaiProcessSelfCheckReady(() => openOverview(resp.record!));
}

function handleCreateSuccess(record: MesFaiApi.FaiRecord) {
  gridApi.query();
  void handleOpenOverview(record);
}

function canAudit(row: MesFaiApi.FaiRecord) {
  return row.status === 'WAITING_QA';
}

function canOneClickPass(row: MesFaiApi.FaiRecord) {
  return (
    !['CANCELED', 'COMPLETED', 'REJECTED'].includes(row.status || '')
  );
}

function canOneClickFail(row: MesFaiApi.FaiRecord) {
  return canOneClickPass(row);
}

function hasPendingRecheckApply(row: MesFaiApi.FaiRecord) {
  return row.latestRecheckApplyStatus === 'PENDING_AUDIT';
}

function canApplyRecheck(row: MesFaiApi.FaiRecord) {
  return (
    Boolean(row.id) &&
    !row.recheckFlag &&
    !hasPendingRecheckApply(row) &&
    row.latestRecheckApplyStatus !== 'APPROVED' &&
    (row.judgment === 'NG' || row.status === 'REJECTED')
  );
}

function canAuditRecheckApply(row: MesFaiApi.FaiRecord) {
  return Boolean(row.latestRecheckApplyId) && hasPendingRecheckApply(row);
}

function handleOpenAudit(row: MesFaiApi.FaiRecord) {
  if (!canAudit(row)) {
    message.warning('首检单提交检测结果后才允许审核');
    return;
  }
  detailModalApi.setData({ id: row.id, mode: 'AUDIT' }).open();
}

function handleAuditSuccess() {
  gridApi.query();
}

function handleOneClickPass(row: MesFaiApi.FaiRecord) {
  if (!row.id) return;
  if (!canOneClickPass(row)) {
    message.warning('当前首检单已结束，不允许一键合格');
    return;
  }
  Modal.confirm({
    title: '一键合格',
    content: `确认将首检单 ${row.faiNo || '-'} 的所有检测项置为合格并完成审核？`,
    okText: '确认合格',
    cancelText: '取消',
    onOk: async () => {
      await oneClickPassFai(row.id!);
      message.success('首检单已一键合格');
      gridApi.query();
    },
  });
}

function handleOneClickFail(row: MesFaiApi.FaiRecord) {
  if (!row.id) return;
  if (!canOneClickFail(row)) {
    message.warning('当前首检单已结束，不允许一键不合格');
    return;
  }
  Modal.confirm({
    title: '一键不合格',
    content: `确认将首检单 ${row.faiNo || '-'} 的所有检测项置为不合格并锁定？`,
    okText: '确认不合格',
    cancelText: '取消',
    okButtonProps: { danger: true },
    onOk: async () => {
      await oneClickFailFai(row.id!);
      message.success('首检单已一键不合格');
      gridApi.query();
    },
  });
}

function handleOpenRecheckApply(row: MesFaiApi.FaiRecord) {
  if (!row.id) return;
  if (!canApplyRecheck(row)) {
    message.warning('只有原检或加检不合格后才允许申请复检，且不能重复提交待审核申请');
    return;
  }
  recheckApplyRecord.value = row;
  recheckApplyReason.value = row.lastReturnReason || '';
  recheckApplyOpen.value = true;
}

async function handleSubmitRecheckApply() {
  const row = recheckApplyRecord.value;
  if (!row?.id) return;
  recheckApplySubmitting.value = true;
  try {
    await applyFaiRecheck(row.id, recheckApplyReason.value);
    message.success('复检申请已提交，待审核通过后自动生成复检单');
    recheckApplyOpen.value = false;
    await gridApi.query();
  } finally {
    recheckApplySubmitting.value = false;
  }
}

function handleOpenRecheckAudit(row: MesFaiApi.FaiRecord) {
  if (!canAuditRecheckApply(row)) {
    message.warning('当前首检单没有待审核复检申请');
    return;
  }
  recheckAuditRecord.value = row;
  recheckAuditOpinion.value = '';
  recheckAuditOpen.value = true;
}

async function handleSubmitRecheckAudit(approved: boolean) {
  const row = recheckAuditRecord.value;
  if (!row?.latestRecheckApplyId) return;
  recheckAuditSubmitting.value = true;
  try {
    const apply = await auditFaiRecheckApply({
      approved,
      auditOpinion: recheckAuditOpinion.value,
      id: row.latestRecheckApplyId,
    });
    message.success(
      approved
        ? `复检申请已通过，已生成复检单：${apply.generatedFaiNo || '-'}`
        : '复检申请已驳回',
    );
    recheckAuditOpen.value = false;
    await gridApi.query();
  } finally {
    recheckAuditSubmitting.value = false;
  }
}

function optionLabel(
  options: Array<{ label: string; value: string }>,
  value?: string,
) {
  return options.find((item) => item.value === value)?.label || '';
}

function isInspected(row: MesFaiApi.FaiRecord) {
  return row.status === 'COMPLETED' || row.status === 'REJECTED';
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
  return isInspected(row) ? 'success' : 'blue';
}

function qualifiedLabel(row: MesFaiApi.FaiRecord) {
  if (row.judgment === 'OK') return '合格';
  if (row.judgment === 'NG') return '不合格';
  return '待判定';
}

function recheckApplyStatusLabel(status?: MesFaiApi.RecheckApplyStatus) {
  if (status === 'PENDING_AUDIT') return '待审核';
  if (status === 'APPROVED') return '已通过';
  if (status === 'REJECTED') return '已驳回';
  if (status === 'CANCELED') return '已取消';
  return '-';
}

function recheckApplyStatusColor(status?: MesFaiApi.RecheckApplyStatus) {
  if (status === 'PENDING_AUDIT') return 'processing';
  if (status === 'APPROVED') return 'success';
  if (status === 'REJECTED') return 'error';
  if (status === 'CANCELED') return 'default';
  return 'default';
}

async function handlePrint(row: MesFaiApi.FaiRecord) {
  if (row.id) {
    await printFaiInspectionSheetById(row.id);
    return;
  }
  await printFaiInspectionSheet(row);
}
</script>

<template>
  <Page auto-content-height class="relative">
    <DetailModal @success="handleAuditSuccess" />
    <CreateModal @success="handleCreateSuccess" />
    <StationFormRuntimeFillModal
      v-model:open="selfCheckRuntimeOpen"
      confirm-auth-action-name="确认过程首检自检记录"
      enable-confirm
      :form="selfCheckRuntimeForm"
      :initial-params="selfCheckRuntimeInitialParams"
      readonly-pass-work-header
      save-auth-action-name="保存过程首检自检记录"
      skip-business-param-step
      @success="handleSelfCheckRuntimeSuccess"
    />
    <SelfCheckRecordRuntimeViewModal
      v-model:open="selfCheckRuntimeRecordOpen"
      confirm-auth-action-name="确认过程首检自检记录"
      empty-message="加载过程首检自检记录详情失败"
      fallback-process-code="FAI_PROCESS_SELF_CHECK"
      fallback-process-name="过程首检自检记录"
      :record="selfCheckRuntimeRecord"
      @success="handleSelfCheckRuntimeSuccess"
    />
    <QmsFaiScanResolveModal
      v-model:open="scanModalOpen"
      scene="LEDGER_TOOLBAR"
      @resolved="handleScanResolved"
    />
    <EnvironmentDailyRecordModal
      v-model:open="environmentRecordOpen"
      :record="todayEnvironmentRecord"
      :workshop-code="ENVIRONMENT_WORKSHOPS.FINAL_INSPECTION_ROOM.code"
      :workshop-name="ENVIRONMENT_WORKSHOPS.FINAL_INSPECTION_ROOM.name"
      @go-confirm="goFillEnvironment"
      @saved="handleEnvironmentRecordSaved"
    />
    <div v-show="viewMode === 'LEDGER'" class="flex h-full flex-col">
      <Grid table-title="首件检验记录">
        <template #toolbar-tools>
          <div
            class="mr-2 flex flex-wrap items-center justify-end gap-2 border-r border-gray-200 pr-3"
          >
            <Button
              v-access:code="['mes:fai:create']"
              type="primary"
              @click="createModalApi.open()"
            >
              <IconifyIcon icon="lucide:plus" class="mr-1" /> 新增
            </Button>
            <Tag
              :color="environmentStatus.color"
              class="!m-0 cursor-pointer"
              @click="handleEnvironmentStatusClick"
            >
              <IconifyIcon icon="lucide:thermometer" class="mr-1" />
              {{ environmentStatus.text }}
            </Tag>
            <Button
              v-access:code="['mes:fai:workbench']"
              :loading="selfCheckChecking || environmentChecking"
              type="primary"
              @click="handleOpenScan"
            >
              <IconifyIcon icon="lucide:scan-line" class="mr-1" /> 扫码填写
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
        <template #productBatchNo="{ row }">
          <span class="font-mono">{{ resolveFaiDisplayBatchNo(row) }}</span>
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
          <Tag :color="resolveFaiInspectionTypeMeta(row).color" class="!m-0">
            {{ resolveFaiInspectionTypeMeta(row).label }}
          </Tag>
        </template>
        <template #recheckApplyStatus="{ row }">
          <Tag
            v-if="row.latestRecheckApplyStatus"
            :color="recheckApplyStatusColor(row.latestRecheckApplyStatus)"
            class="!m-0"
          >
            {{ recheckApplyStatusLabel(row.latestRecheckApplyStatus) }}
          </Tag>
          <span v-else class="text-gray-400">-</span>
        </template>
        <template #retentionStatus="{ row }">
          <Tag :color="retentionStatusColor(row.retentionStatus)" class="!m-0">
            {{ retentionStatusLabel(row.retentionStatus) }}
          </Tag>
        </template>
        <template #actions="{ row }">
          <div class="flex items-center justify-center gap-1">
            <TableAction
              :actions="[
                {
                  label: '查看',
                  type: 'link',
                  icon: ACTION_ICON.VIEW,
                  auth: ['mes:fai:query'],
                  onClick: () => handleViewDetail(row),
                },
                {
                  label: '数据录入',
                  type: 'link',
                  icon: 'lucide:keyboard',
                  auth: ['mes:fai:workbench'],
                  onClick: () => handleOpenOverview(row),
                },
                {
                  label: '审核',
                  type: 'link',
                  icon: 'lucide:badge-check',
                  auth: ['mes:fai:confirm'],
                  disabled: row.status !== 'WAITING_QA',
                  onClick: () => handleOpenAudit(row),
                },
                {
                  label: '申请复检',
                  type: 'link',
                  icon: 'lucide:repeat-2',
                  auth: ['mes:fai:workbench'],
                  disabled: !canApplyRecheck(row),
                  onClick: () => handleOpenRecheckApply(row),
                },
                {
                  label: '复检申请审核',
                  type: 'link',
                  icon: 'lucide:clipboard-check',
                  auth: ['mes:fai:confirm'],
                  ifShow: canAuditRecheckApply(row),
                  onClick: () => handleOpenRecheckAudit(row),
                },
                {
                  label: '打印',
                  type: 'link',
                  icon: 'lucide:printer',
                  ifShow: false,
                  onClick: () => handlePrint(row),
                },
              ]"
            />
            <Button
              v-access:code="['mes:fai:confirm']"
              aria-label="一键合格"
              :disabled="!canOneClickPass(row)"
              title="一键合格"
              class="!inline-flex !items-center !justify-center !px-2 text-emerald-600"
              size="small"
              type="text"
              @click="handleOneClickPass(row)"
            >
              <IconifyIcon icon="lucide:check-check" />
            </Button>
            <Button
              v-access:code="['mes:fai:confirm']"
              aria-label="一键不合格"
              :disabled="!canOneClickFail(row)"
              title="一键不合格"
              class="!inline-flex !items-center !justify-center !px-2"
              danger
              size="small"
              type="text"
              @click="handleOneClickFail(row)"
            >
              <IconifyIcon icon="lucide:x-circle" />
            </Button>
          </div>
        </template>
      </Grid>
    </div>

    <Modal
      v-model:open="recheckApplyOpen"
      :confirm-loading="recheckApplySubmitting"
      title="申请复检"
      ok-text="提交申请"
      cancel-text="取消"
      @ok="handleSubmitRecheckApply"
    >
      <div class="space-y-3">
        <div class="text-sm text-gray-600">
          基于首检单
          <span class="font-mono text-blue-600">
            {{ recheckApplyRecord?.faiNo || '-' }}
          </span>
          申请复检；审核通过后系统会生成新的复检 FAI 单号，并使用原首检同一送样。
        </div>
        <Input.TextArea
          v-model:value="recheckApplyReason"
          :auto-size="{ minRows: 3, maxRows: 5 }"
          allow-clear
          placeholder="请输入申请原因"
        />
      </div>
    </Modal>

    <Modal
      v-model:open="recheckAuditOpen"
      :confirm-loading="recheckAuditSubmitting"
      title="复检申请审核"
      :footer="null"
    >
      <div class="space-y-3">
        <div class="rounded border border-gray-100 bg-gray-50 p-3 text-sm">
          <div>来源单号：{{ recheckAuditRecord?.faiNo || '-' }}</div>
          <div>
            申请单号：{{ recheckAuditRecord?.latestRecheckApplyNo || '-' }}
          </div>
          <div>
            申请原因：{{ recheckAuditRecord?.latestRecheckApplyReason || '-' }}
          </div>
        </div>
        <Input.TextArea
          v-model:value="recheckAuditOpinion"
          :auto-size="{ minRows: 3, maxRows: 5 }"
          allow-clear
          placeholder="请输入审核意见"
        />
        <div class="flex justify-end gap-2">
          <Button @click="recheckAuditOpen = false">取消</Button>
          <Button
            danger
            :loading="recheckAuditSubmitting"
            @click="handleSubmitRecheckAudit(false)"
          >
            驳回
          </Button>
          <Button
            type="primary"
            :loading="recheckAuditSubmitting"
            @click="handleSubmitRecheckAudit(true)"
          >
            通过并生成复检单
          </Button>
        </div>
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
