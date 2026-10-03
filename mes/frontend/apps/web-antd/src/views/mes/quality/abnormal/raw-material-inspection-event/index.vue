<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesNcrApi } from '#/api/mes/quality/abnormal/ncr';
import type { MesProductAbnormalEventApi } from '#/api/mes/quality/abnormal/product-event';

import { computed, nextTick, ref } from 'vue';

import { Page, useVbenModal } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import { Button, Checkbox, message, Modal, Table, Tag } from 'ant-design-vue';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import { getProductAbnormalEventRecheckHistory } from '#/api/mes/quality/abnormal/product-event';
import {
  createRawMaterialNcrFromInspection,
  getRawMaterialNcrSourceInspectionPage,
} from '#/api/mes/quality/abnormal/raw-material-ncr';
import IqcDetailModalForm from '#/views/mes/quality/iqc/modules/detail-modal.vue';

import TaskWizardModal from '../../task-center/modules/task-wizard-modal.vue';
import RawMaterialNcrDetailModalForm from '../ncr/modules/detail-modal.vue';
import {
  RAW_MATERIAL_NCR_STATUS_OPTIONS,
  useGridColumns,
  useGridFormSchema,
} from './data';
import BatchNcrModalForm from './modules/batch-ncr-modal.vue';

type RawMaterialInspectionEvent = MesNcrApi.RawMaterialInspection & {
  eventKey: string;
};
type NcrStatus = (typeof RAW_MATERIAL_NCR_STATUS_OPTIONS)[number]['value'];

defineOptions({ name: 'MesQualityRawMaterialInspectionEvent' });

const ncrStatusScope = ref<NcrStatus[]>([]);
const advancedSearchVisible = ref(false);
const checkedRows = ref<RawMaterialInspectionEvent[]>([]);
const generatingNcrKeys = ref<Set<string>>(new Set());
const ncrStatusFilterOptions = RAW_MATERIAL_NCR_STATUS_OPTIONS;

const pendingCheckedRows = computed(() =>
  checkedRows.value.filter(
    (row) => !row.ncrGenerated && row.canGenerateNcr !== false,
  ),
);
const selectedCanReject = computed(
  () =>
    checkedRows.value.length === 1 &&
    checkedRows.value[0]?.canRejectRecheck !== false &&
    checkedRows.value[0]?.recheckStatus !== 'RECHECKING',
);
const taskWizardRef = ref<InstanceType<typeof TaskWizardModal>>();
const historyModalOpen = ref(false);
const historyLoading = ref(false);
const recheckHistory = ref<MesProductAbnormalEventApi.RecheckHistory | null>(
  null,
);
const legacyRecheckRounds = computed(() =>
  (recheckHistory.value?.details || []).some((item) => item.roundNo === 0),
);
const recheckHistoryColumns = [
  { align: 'center', dataIndex: 'roundNo', title: '轮次', width: 96 },
  { dataIndex: 'inspectionNo', title: 'IQC单号', width: 180 },
  {
    align: 'center',
    dataIndex: 'inspectionJudgment',
    title: '判定',
    width: 90,
  },
  { dataIndex: 'rejectReason', title: '驳回说明', width: 220 },
  { dataIndex: 'rejectUserName', title: '驳回人', width: 110 },
  { dataIndex: 'rejectTime', title: '驳回时间', width: 170 },
  { dataIndex: 'resultTime', title: '完成时间', width: 170 },
  {
    align: 'center',
    dataIndex: 'actions',
    fixed: 'right',
    title: '操作',
    width: 86,
  },
];
const recheckHistoryTableScroll = { x: 1120, y: 'calc(64vh - 92px)' };

const [RawMaterialNcrDetailModal, rawMaterialNcrDetailModalApi] = useVbenModal({
  connectedComponent: RawMaterialNcrDetailModalForm,
  destroyOnClose: true,
});

const [IqcDetailModal, iqcDetailModalApi] = useVbenModal({
  connectedComponent: IqcDetailModalForm,
  destroyOnClose: true,
});

const [BatchNcrModal, batchNcrModalApi] = useVbenModal({
  connectedComponent: BatchNcrModalForm,
  destroyOnClose: true,
});

function handleRowCheckboxChange({
  records,
}: {
  records: RawMaterialInspectionEvent[];
}) {
  checkedRows.value = records || [];
}

const [Grid, gridApi] = useVbenVxeGrid({
  class: 'qms-raw-material-event-vben-grid',
  gridClass: 'qms-raw-material-event-vxe-grid',
  formOptions: {
    schema: useGridFormSchema(false),
    collapsed: false,
    showCollapseButton: false,
  },
  gridOptions: {
    columns: useGridColumns(),
    height: '100%',
    keepSource: true,
    rowConfig: { keyField: 'eventKey', isHover: true },
    toolbarConfig: { refresh: true, search: true },
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) => {
          const result = await getRawMaterialNcrSourceInspectionPage({
            pageNo: page.currentPage,
            pageSize: page.pageSize,
            ...formValues,
            inspectionType: 'IQC',
            ...getNcrStatusFilterParams(),
          });
          return {
            ...result,
            list: (result.list || []).map((row) => normalizeRow(row)),
          };
        },
      },
    },
  } as VxeTableGridOptions<RawMaterialInspectionEvent>,
  gridEvents: {
    checkboxAll: handleRowCheckboxChange,
    checkboxChange: handleRowCheckboxChange,
  },
});

function normalizeRow(
  row: MesNcrApi.RawMaterialInspection,
): RawMaterialInspectionEvent {
  return {
    ...row,
    eventKey: `${row.inspectionType || 'IQC'}:${row.inspectionId || row.inspectionNo || ''}`,
  };
}

function handleRefresh() {
  checkedRows.value = [];
  gridApi.query();
}

function getNcrStatusFilterParams() {
  return ncrStatusScope.value.length === 1
    ? { ncrStatus: ncrStatusScope.value[0] }
    : {};
}

function handleNcrStatusFilterChange() {
  handleRefresh();
}

function toggleAdvancedSearch() {
  advancedSearchVisible.value = !advancedSearchVisible.value;
  gridApi.formApi.setState((prev) => ({
    ...prev,
    schema: useGridFormSchema(advancedSearchVisible.value),
  }));
  nextTick(() => gridApi.grid?.recalculate?.(true));
}

function handleBatchGenerateNcr() {
  const targets = pendingCheckedRows.value;
  if (targets.length === 0) {
    message.warning('请先选择待生成 NCR 的原材料检验异常');
    return;
  }
  const validTargets = targets.filter((item) => item.inspectionId);
  if (validTargets.length === 0) {
    message.warning('缺少进料检验单来源信息，无法生成 NCR');
    return;
  }
  if (checkedRows.value.length !== validTargets.length) {
    message.warning('已生成 NCR 或来源不完整的记录已自动跳过');
  }
  batchNcrModalApi.setData({ rows: validTargets }).open();
}

async function handleGenerateNcr(row: RawMaterialInspectionEvent) {
  if (row.ncrGenerated) {
    handleOpenNcr(row);
    return;
  }
  if (row.canGenerateNcr === false) {
    message.warning('该进料检验单处于复检中或复检OK，不能生成 NCR');
    return;
  }
  if (!row.inspectionId) {
    message.warning('缺少进料检验单来源信息，无法生成 NCR');
    return;
  }
  if (isGeneratingNcr(row)) {
    return;
  }
  setGeneratingNcr(row, true);
  try {
    const ncRecordId = await createRawMaterialNcrFromInspection({
      inspectionId: row.inspectionId,
      inspectionType: row.inspectionType || 'IQC',
    });
    if (!ncRecordId) {
      message.warning('NCR 生成失败，未返回单据ID');
      return;
    }
    message.success('已打开 NCR 详情');
    rawMaterialNcrDetailModalApi
      .setData({ id: ncRecordId, sourceType: 'RAW_MATERIAL' })
      .open();
    handleRefresh();
  } finally {
    setGeneratingNcr(row, false);
  }
}

function handleOpenNcr(row: RawMaterialInspectionEvent) {
  if (!row.ncrId) {
    message.warning('未找到关联 NCR');
    return;
  }
  rawMaterialNcrDetailModalApi
    .setData({ id: row.ncrId, sourceType: 'RAW_MATERIAL' })
    .open();
}

function handleBatchNcrSuccess(ncRecordId: number) {
  handleRefresh();
  if (ncRecordId) {
    rawMaterialNcrDetailModalApi
      .setData({ id: ncRecordId, sourceType: 'RAW_MATERIAL' })
      .open();
  }
}

function handleViewIqc(row: RawMaterialInspectionEvent) {
  const detailId = row.recheckLatestInspectionId || row.inspectionId;
  if (!detailId) {
    message.warning('缺少进料检验单ID，无法查看');
    return;
  }
  iqcDetailModalApi
    .setData({ id: detailId, onlyRecheck: Boolean(row.recheckGroupId) })
    .open();
}

function handleRejectRecheck(row?: RawMaterialInspectionEvent) {
  const target = row || checkedRows.value[0];
  if (!target) {
    message.warning('请先选择原材料检验异常');
    return;
  }
  if (!row && checkedRows.value.length !== 1) {
    message.warning('驳回重检一次只能选择一条进料检验单');
    return;
  }
  if (!target.inspectionId) {
    message.warning('缺少进料检验单来源信息，无法驳回复检');
    return;
  }
  if (target.recheckStatus === 'RECHECKING') {
    message.warning('该进料检验单正在复检中，不能重复驳回');
    return;
  }
  if (target.canRejectRecheck === false) {
    message.warning('该进料检验单当前状态不允许驳回复检');
    return;
  }
  const inspectionId =
    target.recheckStatus === 'RECHECK_NG' && target.recheckLatestInspectionId
      ? target.recheckLatestInspectionId
      : target.inspectionId;
  const inspectionNo =
    target.recheckStatus === 'RECHECK_NG' && target.recheckLatestInspectionNo
      ? target.recheckLatestInspectionNo
      : target.inspectionNo;
  taskWizardRef.value?.open({
    abnormalSummary: target.abnormalSummary,
    checkType: 'IQC',
    inspectionId,
    inspectionNo,
    inspectionScene: 'IQC',
    mode: 'PRODUCT_EVENT_RECHECK',
  });
}

async function handleViewRecheckHistory(row: RawMaterialInspectionEvent) {
  const inspectionId = row.recheckRootInspectionId || row.inspectionId;
  if (!inspectionId) {
    message.warning('缺少进料检验单来源信息，无法查看复检历史');
    return;
  }
  historyModalOpen.value = true;
  historyLoading.value = true;
  recheckHistory.value = null;
  try {
    recheckHistory.value = await getProductAbnormalEventRecheckHistory(
      'IQC',
      inspectionId,
    );
  } finally {
    historyLoading.value = false;
  }
}

async function handleViewHistoryDetail(
  record: MesProductAbnormalEventApi.RecheckHistoryDetail,
) {
  if (!record.inspectionId) {
    message.warning('缺少进料检验单来源信息，无法查看详情');
    return;
  }
  historyModalOpen.value = false;
  await nextTick();
  window.setTimeout(() => {
    iqcDetailModalApi
      .setData({
        id: record.inspectionId,
        onlyRecheck: !isOriginalRound(record.roundNo),
      })
      .open();
  }, 120);
}

function judgmentColor(value?: string) {
  if (value === 'GENERATED') return 'success';
  return 'warning';
}

function ncrStatusLabel(row: RawMaterialInspectionEvent) {
  return row.ncrGenerated ? '已生成' : '待生成';
}

function recheckStatusColor(value?: string) {
  if (value === 'RECHECK_OK') return 'success';
  if (value === 'RECHECK_NG') return 'error';
  if (value === 'RECHECKING') return 'processing';
  return 'default';
}

function recheckStatusLabel(value?: string) {
  if (value === 'RECHECK_OK') return '复检OK';
  if (value === 'RECHECK_NG') return '复检NG';
  if (value === 'RECHECKING') return '复检中';
  return '未驳回';
}

function judgmentRecheckColor(value?: string) {
  if (value === 'OK') return 'success';
  if (value === 'NG') return 'error';
  return 'processing';
}

function judgmentLabel(value?: string) {
  if (value === 'OK') return '合格';
  if (value === 'NG') return '不合格';
  if (value === 'PENDING') return '待判定';
  return value || '-';
}

function normalizedRoundNo(roundNo?: number) {
  const value = roundNo ?? (legacyRecheckRounds.value ? 0 : 1);
  return legacyRecheckRounds.value ? value + 1 : Math.max(1, value);
}

function isOriginalRound(roundNo?: number) {
  return normalizedRoundNo(roundNo) === 1;
}

function recheckRoundLabel(roundNo?: number) {
  const displayRound = normalizedRoundNo(roundNo);
  if (displayRound === 1) return '第1轮 原始检验单';
  return `第${displayRound}轮 第${displayRound - 1}次复检`;
}

function displayValue(value?: unknown) {
  if (value === undefined || value === null || value === '') {
    return '-';
  }
  return String(value);
}

function rowGenerateKey(row: RawMaterialInspectionEvent) {
  return (
    row.eventKey || `${row.inspectionType || 'IQC'}:${row.inspectionId || ''}`
  );
}

function isGeneratingNcr(row: RawMaterialInspectionEvent) {
  return generatingNcrKeys.value.has(rowGenerateKey(row));
}

function setGeneratingNcr(
  row: RawMaterialInspectionEvent,
  generating: boolean,
) {
  const key = rowGenerateKey(row);
  const next = new Set(generatingNcrKeys.value);
  if (generating) {
    next.add(key);
  } else {
    next.delete(key);
  }
  generatingNcrKeys.value = next;
}

function getNcrActionMeta(row: RawMaterialInspectionEvent) {
  if (row.ncrGenerated) {
    return {
      ariaLabel: '查看NCR',
      disabled: !row.ncrId || isGeneratingNcr(row),
      icon: 'lucide:file-search',
      title: '查看NCR',
    };
  }
  return {
    ariaLabel: '生成NCR',
    disabled: isGeneratingNcr(row) || row.canGenerateNcr === false,
    icon: 'lucide:file-plus-2',
    title: row.canGenerateNcr === false ? '当前状态不可生成NCR' : '生成NCR',
  };
}

function handleNcrAction(row: RawMaterialInspectionEvent) {
  if (row.ncrGenerated) {
    handleOpenNcr(row);
    return;
  }
  handleGenerateNcr(row);
}
</script>

<template>
  <Page auto-content-height>
    <TaskWizardModal ref="taskWizardRef" @success="handleRefresh" />
    <RawMaterialNcrDetailModal @success="handleRefresh" />
    <IqcDetailModal />
    <BatchNcrModal @success="handleBatchNcrSuccess" />
    <Modal
      v-model:open="historyModalOpen"
      title="驳回复检历史"
      width="980px"
      :footer="null"
      :body-style="{ height: '64vh', overflow: 'hidden' }"
    >
      <div v-if="historyLoading" class="qms-recheck-history__loading">
        加载中...
      </div>
      <div v-else-if="recheckHistory" class="qms-recheck-history">
        <div class="qms-recheck-history__head">
          <span>根单：{{ recheckHistory.rootInspectionNo || '-' }}</span>
          <span>最新：{{ recheckHistory.latestInspectionNo || '-' }}</span>
          <span>复检次数：{{ recheckHistory.totalRecheckCount ?? 0 }}</span>
        </div>
        <Table
          bordered
          :columns="recheckHistoryColumns"
          :data-source="recheckHistory.details || []"
          :pagination="false"
          row-key="inspectionId"
          size="small"
          :scroll="recheckHistoryTableScroll"
          class="qms-recheck-history__table"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'roundNo'">
              {{ recheckRoundLabel(record.roundNo) }}
            </template>
            <template v-else-if="column.dataIndex === 'inspectionNo'">
              <span class="font-mono">{{ record.inspectionNo || '-' }}</span>
            </template>
            <template v-else-if="column.dataIndex === 'inspectionJudgment'">
              <Tag
                :color="judgmentRecheckColor(record.inspectionJudgment)"
                class="!m-0"
              >
                {{ judgmentLabel(record.inspectionJudgment) }}
              </Tag>
            </template>
            <template v-else-if="column.dataIndex === 'rejectReason'">
              <span class="qms-recheck-history__reason-text">
                {{
                  isOriginalRound(record.roundNo)
                    ? '-'
                    : record.rejectReason || '-'
                }}
              </span>
            </template>
            <template v-else-if="column.dataIndex === 'actions'">
              <Button
                size="small"
                type="link"
                @click="handleViewHistoryDetail(record)"
              >
                详情
              </Button>
            </template>
            <template v-else>
              {{ record[column.dataIndex] || '-' }}
            </template>
          </template>
        </Table>
      </div>
      <div v-else class="qms-recheck-history__loading">暂无复检历史</div>
    </Modal>
    <div class="qms-raw-material-event-page">
      <div class="qms-raw-material-event-grid-host">
        <Grid>
          <template #toolbar-actions>
            <div class="qms-ncr-status-filter">
              <Checkbox.Group
                v-model:value="ncrStatusScope"
                :options="ncrStatusFilterOptions"
                @change="handleNcrStatusFilterChange"
              />
            </div>
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
                    label: '驳回重检',
                    type: 'primary',
                    icon: 'lucide:rotate-ccw',
                    disabled: !selectedCanReject,
                    onClick: () => handleRejectRecheck(),
                  },
                  {
                    label: '合并生成NCR',
                    type: 'primary',
                    icon: ACTION_ICON.ADD,
                    disabled: pendingCheckedRows.length === 0,
                    onClick: () => handleBatchGenerateNcr(),
                  },
                ]"
              />
              <Button size="small" @click="handleRefresh">
                <IconifyIcon icon="lucide:refresh-cw" class="mr-1" />
                刷新
              </Button>
            </div>
          </template>

          <template #inspectionNo="{ row }">
            <a
              class="font-mono font-bold text-blue-700 hover:underline"
              @click="handleViewIqc(row)"
            >
              {{ row.inspectionNo || '-' }}
            </a>
          </template>

          <template #inspectionTypeName="{ row }">
            <Tag color="blue" class="!m-0">
              {{ row.inspectionTypeName || '进料检验(IQC)' }}
            </Tag>
          </template>

          <template #materialName="{ row }">
            <div class="leading-tight">
              <div class="font-bold text-slate-800">
                {{ row.materialName || '-' }}
              </div>
              <div class="mt-1 text-xs text-slate-400">
                {{ row.materialCode || '-' }} / {{ row.specification || '-' }}
              </div>
            </div>
          </template>

          <template #quantity="{ row }">
            <span class="font-mono">
              {{ displayValue(row.quantity) }} {{ row.unitCode || '' }}
            </span>
          </template>

          <template #abnormalSummary="{ row }">
            <span
              class="qms-raw-material-event-summary text-slate-700"
              :title="row.abnormalSummary || ''"
            >
              {{ row.abnormalSummary || '-' }}
            </span>
          </template>

          <template #ncrNo="{ row }">
            <a
              v-if="row.ncrId"
              class="font-mono font-bold text-blue-700 hover:underline"
              @click="handleOpenNcr(row)"
            >
              {{ row.ncrNo || row.ncrId }}
            </a>
            <span v-else class="text-slate-400">-</span>
          </template>

          <template #ncrStatus="{ row }">
            <Tag :color="judgmentColor(row.ncrStatus)" class="!m-0">
              {{ ncrStatusLabel(row) }}
            </Tag>
          </template>

          <template #rejectNextInspectionNo="{ row }">
            <span
              v-if="row.rejectNextInspectionNo || row.recheckLatestInspectionNo"
              class="font-mono text-slate-800"
            >
              {{ row.rejectNextInspectionNo || row.recheckLatestInspectionNo }}
            </span>
            <span v-else class="text-slate-400">-</span>
          </template>

          <template #recheckStatus="{ row }">
            <Tag :color="recheckStatusColor(row.recheckStatus)" class="!m-0">
              {{
                row.recheckStatusName || recheckStatusLabel(row.recheckStatus)
              }}
            </Tag>
          </template>

          <template #actions="{ row }">
            <div class="qms-raw-material-event-actions">
              <Button
                aria-label="查看进料检验单"
                class="qms-raw-material-event-action-btn"
                size="small"
                title="查看进料检验单"
                type="text"
                @click="handleViewIqc(row)"
              >
                <IconifyIcon icon="lucide:clipboard-search" />
              </Button>
              <Button
                aria-label="驳回重检"
                class="qms-raw-material-event-action-btn"
                :disabled="
                  row.canRejectRecheck === false ||
                  row.recheckStatus === 'RECHECKING'
                "
                size="small"
                title="驳回重检"
                type="text"
                @click="handleRejectRecheck(row)"
              >
                <IconifyIcon icon="lucide:rotate-ccw" />
              </Button>
              <Button
                v-if="row.recheckGroupId"
                aria-label="复检历史"
                class="qms-raw-material-event-action-btn"
                size="small"
                title="复检历史"
                type="text"
                @click="handleViewRecheckHistory(row)"
              >
                <IconifyIcon icon="lucide:history" />
              </Button>
              <Button
                :aria-label="getNcrActionMeta(row).ariaLabel"
                class="qms-raw-material-event-action-btn"
                :disabled="getNcrActionMeta(row).disabled"
                :loading="isGeneratingNcr(row)"
                size="small"
                :title="getNcrActionMeta(row).title"
                type="text"
                @click="handleNcrAction(row)"
              >
                <IconifyIcon :icon="getNcrActionMeta(row).icon" />
              </Button>
            </div>
          </template>
        </Grid>
      </div>
    </div>
  </Page>
</template>

<style scoped>
.qms-raw-material-event-page {
  position: relative;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  background: #fff;
}

.qms-raw-material-event-grid-host {
  position: absolute;
  inset: 0;
  min-height: 0;
  overflow: hidden;
}

.qms-raw-material-event-grid-host :deep(.qms-raw-material-event-vben-grid) {
  height: 100% !important;
  max-height: 100% !important;
  min-height: 0 !important;
  overflow: hidden !important;
}

.qms-raw-material-event-grid-host :deep(.qms-raw-material-event-vxe-grid) {
  display: grid !important;
  grid-template-rows: auto auto auto minmax(0, 1fr) auto auto !important;
  height: 100% !important;
  min-height: 0 !important;
  overflow: hidden !important;
}

.qms-raw-material-event-grid-host :deep(.vxe-grid--table-wrapper) {
  grid-row: 4;
  min-height: 0 !important;
  overflow: hidden !important;
}

.qms-raw-material-event-grid-host :deep(.vxe-grid--pager-wrapper) {
  grid-row: 6;
  background: #fff;
}

.qms-ncr-status-filter {
  display: flex;
  min-height: 32px;
  align-items: center;
}

.qms-ncr-status-filter :deep(.ant-checkbox-group) {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px 18px;
}

.qms-ncr-status-filter :deep(.ant-checkbox-wrapper) {
  margin-inline-start: 0;
  color: #475569;
  font-weight: 500;
}

.qms-raw-material-event-summary {
  display: inline-block;
  line-height: 1.45;
  white-space: normal;
  word-break: break-word;
}

.qms-raw-material-event-actions {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 2px;
  width: 100%;
}

.qms-raw-material-event-action-btn {
  width: 28px;
  height: 28px;
  padding: 0;
  color: #2563eb;
}

.qms-raw-material-event-action-btn[disabled] {
  color: #cbd5e1;
}

.qms-recheck-history {
  display: flex;
  flex-direction: column;
  gap: 10px;
  height: 100%;
  min-height: 0;
}

.qms-recheck-history__head {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  color: #475569;
  font-size: 13px;
}

.qms-recheck-history__table {
  min-height: 0;
}

.qms-recheck-history__loading {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #64748b;
}

.qms-recheck-history__reason-text {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
