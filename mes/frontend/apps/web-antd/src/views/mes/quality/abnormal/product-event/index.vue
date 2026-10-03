<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesProductAbnormalEventApi } from '#/api/mes/quality/abnormal/product-event';

import { computed, nextTick, onMounted, ref, watch } from 'vue';
import { useRoute } from 'vue-router';

import { Page, useVbenModal } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import {
  Button,
  Checkbox,
  message,
  Modal,
  Table,
  Tag,
} from 'ant-design-vue';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import { createNcrFromProductEvent } from '#/api/mes/quality/abnormal/ncr';
import {
  getProductAbnormalEventPage,
  getProductAbnormalEventRecheckHistory,
} from '#/api/mes/quality/abnormal/product-event';

import TaskWizardModal from '../../task-center/modules/task-wizard-modal.vue';
import QualityTaskDetail from '../../task-center/detail.vue';
import NcrDetailModalForm from '../ncr/modules/detail-modal.vue';
import {
  NCR_GENERATION_STATUS_OPTIONS,
  useGridColumns,
  useGridFormSchema,
} from './data';
import BatchNcrModalForm from './modules/batch-ncr-modal.vue';
import DetailModalForm from './modules/detail-modal.vue';

defineOptions({ name: 'MesQualityProductAbnormalEvent' });

type NcrGenerationStatus =
  (typeof NCR_GENERATION_STATUS_OPTIONS)[number]['value'];

const route = useRoute();
const ncrStatusScope = ref<NcrGenerationStatus[]>([]);
const advancedSearchVisible = ref(false);
const checkedRows = ref<MesProductAbnormalEventApi.ProductAbnormalEvent[]>([]);
const generatingNcrKeys = ref<Set<string>>(new Set());
const openedRouteInspectionKey = ref('');
const ncrStatusFilterOptions = NCR_GENERATION_STATUS_OPTIONS;
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
const linkedTaskOpen = ref(false);
const linkedTaskId = ref<number>();
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
  { dataIndex: 'inspectionNo', title: '检验单号', width: 170 },
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

const [DetailModal, detailModalApi] = useVbenModal({
  connectedComponent: DetailModalForm,
  destroyOnClose: true,
});
const [NcrDetailModal, ncrDetailModalApi] = useVbenModal({
  connectedComponent: NcrDetailModalForm,
  destroyOnClose: true,
});
const [BatchNcrModal, batchNcrModalApi] = useVbenModal({
  connectedComponent: BatchNcrModalForm,
  destroyOnClose: true,
});

function getRouteSingleValue(value: unknown) {
  if (Array.isArray(value)) {
    return value[0] ? String(value[0]) : '';
  }
  return value ? String(value) : '';
}

function openRouteInspectionDetail() {
  const sourceType = getRouteSingleValue(route.query.sourceType);
  const inspectionId = Number(getRouteSingleValue(route.query.inspectionId));
  if (!sourceType || !Number.isFinite(inspectionId) || inspectionId <= 0) {
    return;
  }
  const key = `${sourceType}:${inspectionId}`;
  if (openedRouteInspectionKey.value === key) {
    return;
  }
  openedRouteInspectionKey.value = key;
  detailModalApi.setData({ inspectionId, sourceType }).open();
}

function handleRowCheckboxChange({
  records,
}: {
  records: MesProductAbnormalEventApi.ProductAbnormalEvent[];
}) {
  checkedRows.value = records || [];
}

const [Grid, gridApi] = useVbenVxeGrid({
  class: 'qms-product-event-vben-grid',
  gridClass: 'qms-product-event-vxe-grid',
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
        query: async ({ page }, formValues) =>
          await getProductAbnormalEventPage({
            pageNo: page.currentPage,
            pageSize: page.pageSize,
            ...formValues,
            ...getNcrStatusFilterParams(),
          }),
      },
    },
  } as VxeTableGridOptions<MesProductAbnormalEventApi.ProductAbnormalEvent>,
  gridEvents: {
    checkboxAll: handleRowCheckboxChange,
    checkboxChange: handleRowCheckboxChange,
  },
});

onMounted(openRouteInspectionDetail);

watch(
  () => [route.query.sourceType, route.query.inspectionId],
  () => openRouteInspectionDetail(),
);

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

function handleView(row: MesProductAbnormalEventApi.ProductAbnormalEvent) {
  if (!row.sourceType || !row.inspectionId) {
    message.warning('缺少检验单来源信息，无法查看详情');
    return;
  }
  detailModalApi
    .setData({ inspectionId: row.inspectionId, sourceType: row.sourceType })
    .open();
}

function handleBatchGenerateNcr() {
  const targets = pendingCheckedRows.value;
  if (targets.length === 0) {
    message.warning('请先选择产品异常事件');
    return;
  }
  const validTargets = targets.filter(
    (item) =>
      item.sourceType && item.inspectionId && item.canGenerateNcr !== false,
  );
  if (validTargets.length === 0) {
    message.warning('缺少检验单来源信息，无法生成 NCR');
    return;
  }
  if (checkedRows.value.length !== validTargets.length) {
    message.warning('已生成 NCR 或来源不完整的记录已自动跳过');
  }
  batchNcrModalApi.setData({ rows: validTargets }).open();
}

async function handleGenerateNcr(
  row: MesProductAbnormalEventApi.ProductAbnormalEvent,
) {
  if (row.ncrGenerated) {
    handleOpenNcr(row);
    return;
  }
  if (row.canGenerateNcr === false) {
    message.warning('该记录处于复检中或复检OK，不能生成 NCR');
    return;
  }
  if (!row.sourceType || !row.inspectionId) {
    message.warning('缺少检验单来源信息，无法生成 NCR');
    return;
  }
  if (isGeneratingNcr(row)) {
    return;
  }
  setGeneratingNcr(row, true);
  try {
    const ncRecordId = await createNcrFromProductEvent({
      inspectionId: row.inspectionId,
      sourceType: row.sourceType,
    });
    if (!ncRecordId) {
      message.warning('NCR 生成失败，未返回单据ID');
      return;
    }
    message.success('已打开 NCR 详情');
    ncrDetailModalApi.setData({ id: ncRecordId }).open();
    handleRefresh();
  } finally {
    setGeneratingNcr(row, false);
  }
}

function handleOpenNcr(row: MesProductAbnormalEventApi.ProductAbnormalEvent) {
  if (!row.ncrId) {
    message.warning('未找到关联 NCR');
    return;
  }
  ncrDetailModalApi.setData({ id: row.ncrId }).open();
}

async function handleDetailFillNcr(
  row: MesProductAbnormalEventApi.ProductAbnormalEvent,
) {
  detailModalApi.close();
  await nextTick();
  window.setTimeout(() => {
    if (row.ncrGenerated) {
      handleOpenNcr(row);
      return;
    }
    handleGenerateNcr(row);
  }, 120);
}

function handleBatchNcrSuccess(records: { ncRecordId?: number }[]) {
  handleRefresh();
  if (records.length === 1 && records[0]?.ncRecordId) {
    ncrDetailModalApi.setData({ id: records[0].ncRecordId }).open();
  }
}

function handleRejectRecheck(
  row?: MesProductAbnormalEventApi.ProductAbnormalEvent,
) {
  const target = row || checkedRows.value[0];
  if (!target) {
    message.warning('请先选择一条产品异常事件');
    return;
  }
  if (!row && checkedRows.value.length !== 1) {
    message.warning('驳回重检一次只能选择一条检验单');
    return;
  }
  if (!target.sourceType || !target.inspectionId) {
    message.warning('缺少检验单来源信息，无法驳回复检');
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
  if (target.recheckStatus === 'RECHECKING') {
    message.warning('该检验单正在复检中，不能重复驳回');
    return;
  }
  if (target.canRejectRecheck === false) {
    message.warning('该检验单当前状态不允许驳回复检');
    return;
  }
  const checkType = productEventCheckType(target.sourceType);
  if (!checkType) {
    message.warning('当前检验场景暂不支持驳回复检');
    return;
  }
  taskWizardRef.value?.open({
    abnormalSummary: target.abnormalSummary,
    checkType,
    inspectionId,
    inspectionNo,
    inspectionScene: target.sourceType,
    mode: 'PRODUCT_EVENT_RECHECK',
  });
}

async function handleViewRecheckHistory(
  row: MesProductAbnormalEventApi.ProductAbnormalEvent,
) {
  if (!row.sourceType || !row.inspectionId) {
    message.warning('缺少检验单来源信息，无法查看复检历史');
    return;
  }
  historyModalOpen.value = true;
  historyLoading.value = true;
  recheckHistory.value = null;
  try {
    recheckHistory.value = await getProductAbnormalEventRecheckHistory(
      row.sourceType,
      row.inspectionId,
    );
  } finally {
    historyLoading.value = false;
  }
}

async function handleViewHistoryDetail(
  record: MesProductAbnormalEventApi.RecheckHistoryDetail,
) {
  const sourceType = recheckHistory.value?.sourceType;
  if (!sourceType || !record.inspectionId) {
    message.warning('缺少检验单来源信息，无法查看详情');
    return;
  }
  historyModalOpen.value = false;
  await nextTick();
  window.setTimeout(() => {
    detailModalApi
      .setData({ inspectionId: record.inspectionId, sourceType })
      .open();
  }, 120);
}

function judgmentColor(value?: string) {
  if (value === 'NG') return 'error';
  if (value === 'OK') return 'success';
  if (value === 'PENDING') return 'warning';
  return 'default';
}

function judgmentLabel(value?: string) {
  const normalized = value?.trim().toUpperCase();
  if (normalized === 'NG') return '不合格';
  if (normalized === 'OK') return '合格';
  if (normalized === 'PENDING') return '待判定';
  return value || '-';
}

function ncrStatusColor(value?: string) {
  if (value === 'GENERATED') return 'success';
  return 'warning';
}

function ncrStatusLabel(value?: string) {
  if (value === 'GENERATED') return '已生成';
  return '待生成';
}

function productEventCheckType(sourceType?: string) {
  if (sourceType === 'FAI' || sourceType === 'GLUE_BOARD_FAI') return sourceType;
  if (sourceType === 'CUT_ROUND_FQC' || sourceType === 'FG_SHIPPING_FQC') {
    return 'FQC';
  }
  if (sourceType === 'IQC') return 'IQC';
  if (sourceType === 'OQC') return 'OQC';
  return undefined;
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

function rowGenerateKey(row: MesProductAbnormalEventApi.ProductAbnormalEvent) {
  return (
    row.eventKey || `${row.sourceType || 'UNKNOWN'}:${row.inspectionId || ''}`
  );
}

function isGeneratingNcr(row: MesProductAbnormalEventApi.ProductAbnormalEvent) {
  return generatingNcrKeys.value.has(rowGenerateKey(row));
}

function setGeneratingNcr(
  row: MesProductAbnormalEventApi.ProductAbnormalEvent,
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

function getNcrActionMeta(row: MesProductAbnormalEventApi.ProductAbnormalEvent) {
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

function handleNcrAction(row: MesProductAbnormalEventApi.ProductAbnormalEvent) {
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
    <Modal
      v-model:open="linkedTaskOpen"
      destroy-on-close
      :footer="null"
      title="关联质量任务"
      width="96vw"
      :body-style="{ height: 'calc(100vh - 104px)', overflow: 'hidden', padding: '0' }"
    >
      <QualityTaskDetail
        v-if="linkedTaskId"
        :id="linkedTaskId"
        @close="linkedTaskOpen = false"
      />
    </Modal>
    <DetailModal
      @fill-ncr="handleDetailFillNcr"
      @success="handleRefresh"
    />
    <NcrDetailModal @success="handleRefresh" />
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
                :color="judgmentColor(record.inspectionJudgment)"
                class="!m-0"
              >
                {{ judgmentLabel(record.inspectionJudgment) }}
              </Tag>
            </template>
            <template v-else-if="column.dataIndex === 'rejectReason'">
              <span class="qms-recheck-history__reason-text">
                {{ isOriginalRound(record.roundNo) ? '-' : record.rejectReason || '-' }}
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
    <div class="qms-product-event-page">
      <div class="qms-product-event-grid-host">
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
                    label: '批量生成NCR',
                    type: 'primary',
                    icon: ACTION_ICON.ADD,
                    disabled: pendingCheckedRows.length === 0,
                    onClick: () => handleBatchGenerateNcr(),
                  },
                ]"
              />
              <Button size="small" @click="handleRefresh">刷新</Button>
            </div>
          </template>

          <template #inspectionType="{ row }">
            <Tag color="blue" class="!m-0">{{ row.inspectionType || '-' }}</Tag>
          </template>

          <template #inspectionNo="{ row }">
            <a
              class="font-mono font-bold text-blue-700 hover:underline"
              @click="handleView(row)"
            >
              {{ row.inspectionNo || '-' }}
            </a>
          </template>

          <template #inspectionQty="{ row }">
            <span class="font-mono">{{ row.inspectionQty ?? '-' }}</span>
          </template>

          <template #unqualifiedQty="{ row }">
            <span class="font-mono">{{ row.unqualifiedQty ?? '-' }}</span>
          </template>

          <template #rejectNextInspectionNo="{ row }">
            <a
              v-if="row.recheckGroupId"
              class="font-mono text-blue-700 hover:underline"
              @click="handleViewRecheckHistory(row)"
            >
              {{ row.rejectNextInspectionNo || row.recheckLatestInspectionNo || '-' }}
            </a>
            <span v-else class="text-slate-400">-</span>
          </template>

          <template #abnormalSummary="{ row }">
            <span
              class="qms-product-event-summary text-slate-700"
              :title="row.abnormalSummary || ''"
            >
              {{ row.abnormalSummary || '-' }}
            </span>
          </template>

          <template #judgment="{ row }">
            <Tag :color="judgmentColor(row.judgment)" class="!m-0">
              {{ judgmentLabel(row.judgment) }}
            </Tag>
          </template>

          <template #ncrStatus="{ row }">
            <Tag :color="ncrStatusColor(row.ncrStatus)" class="!m-0">
              {{ ncrStatusLabel(row.ncrStatus) }}
            </Tag>
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

          <template #recheckResult="{ row }">
            <Tag
              v-if="row.recheckGroupId"
              :color="judgmentColor(row.recheckResult)"
              class="!m-0"
            >
              {{ judgmentLabel(row.recheckResult) }}
            </Tag>
            <span v-else class="text-slate-400">-</span>
          </template>

          <template #recheckCount="{ row }">
            <a
              v-if="row.recheckGroupId"
              class="font-mono text-blue-700 hover:underline"
              @click="handleViewRecheckHistory(row)"
            >
              {{ row.recheckCount ?? 0 }}
            </a>
            <span v-else class="text-slate-400">-</span>
          </template>

          <template #actions="{ row }">
            <div class="qms-product-event-actions">
              <Button
                aria-label="查看"
                class="qms-product-event-action-btn"
                size="small"
                title="查看"
                type="text"
                @click="handleView(row)"
              >
                <IconifyIcon icon="lucide:eye" />
              </Button>
              <Button
                aria-label="驳回重检"
                class="qms-product-event-action-btn"
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
                aria-label="复检历史"
                class="qms-product-event-action-btn"
                :disabled="!row.recheckGroupId"
                size="small"
                title="复检历史"
                type="text"
                @click="handleViewRecheckHistory(row)"
              >
                <IconifyIcon icon="lucide:history" />
              </Button>
              <Button
                :aria-label="getNcrActionMeta(row).ariaLabel"
                class="qms-product-event-action-btn"
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
.qms-product-event-page {
  position: relative;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  background: #fff;
}

.qms-product-event-grid-host {
  position: absolute;
  inset: 0;
  min-height: 0;
  overflow: hidden;
}

.qms-product-event-grid-host :deep(.qms-product-event-vben-grid) {
  height: 100% !important;
  max-height: 100% !important;
  min-height: 0 !important;
  overflow: hidden !important;
}

.qms-product-event-grid-host :deep(.qms-product-event-vxe-grid) {
  display: grid !important;
  grid-template-rows: auto auto auto minmax(0, 1fr) auto auto !important;
  height: 100% !important;
  min-height: 0 !important;
  overflow: hidden !important;
}

.qms-product-event-grid-host :deep(.vxe-grid--table-wrapper) {
  grid-row: 4;
  min-height: 0 !important;
  overflow: hidden !important;
}

.qms-product-event-grid-host :deep(.vxe-grid--pager-wrapper) {
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

.qms-recheck-reject {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.qms-recheck-reject__meta,
.qms-recheck-history__head {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 20px;
  color: #475569;
}

.qms-recheck-history__loading {
  height: 100%;
  padding: 24px 0;
  text-align: center;
  color: #64748b;
}

.qms-recheck-history {
  display: flex;
  flex-direction: column;
  gap: 14px;
  height: 100%;
  min-height: 0;
}

.qms-recheck-history__table {
  display: flex;
  flex: 1 1 0;
  min-height: 0;
}

.qms-recheck-history__table :deep(.ant-spin-nested-loading),
.qms-recheck-history__table :deep(.ant-spin-container),
.qms-recheck-history__table :deep(.ant-table),
.qms-recheck-history__table :deep(.ant-table-container) {
  height: 100%;
  min-height: 0;
  width: 100%;
}

.qms-recheck-history__table :deep(.ant-spin-nested-loading),
.qms-recheck-history__table :deep(.ant-spin-container),
.qms-recheck-history__table :deep(.ant-table),
.qms-recheck-history__table :deep(.ant-table-container) {
  display: flex;
  flex: 1 1 0;
  flex-direction: column;
}

.qms-recheck-history__table :deep(.ant-table-body) {
  flex: 1 1 0;
  height: auto !important;
  max-height: none !important;
}

.qms-recheck-history__reason-text {
  display: inline-block;
  max-width: 210px;
  overflow: hidden;
  text-overflow: ellipsis;
  vertical-align: middle;
  white-space: nowrap;
}

.qms-product-event-summary {
  display: inline-block;
  line-height: 1.45;
  white-space: normal;
  word-break: break-word;
}

.qms-product-event-actions {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 2px;
  width: 100%;
}

.qms-product-event-action-btn {
  width: 28px;
  height: 28px;
  padding: 0;
  color: #2563eb;
}

.qms-product-event-action-btn[disabled] {
  color: #cbd5e1;
}
</style>
