<script lang="ts" setup>
import type { MesProductAbnormalEventApi } from '#/api/mes/quality/abnormal/product-event';
import type { MesCutRoundFqcApi } from '#/api/mes/quality/cut-round-fqc';
import type { MesFaiApi } from '#/api/mes/quality/fai';
import type { MesFgShippingFqcApi } from '#/api/mes/quality/fg-shipping-fqc';
import type { MesOqcApi } from '#/api/mes/quality/oqc';

import { computed, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import {
  Button,
  Checkbox,
  Empty,
  message,
  Spin,
  Table,
  Tag,
} from 'ant-design-vue';

import { getProductAbnormalEventDetail } from '#/api/mes/quality/abnormal/product-event';
import { getCutRoundFqcLightDetail } from '#/api/mes/quality/cut-round-fqc';
import { getFaiDetail, getGlueBoardFaiDetail } from '#/api/mes/quality/fai';
import { getFgShippingFqcDetail } from '#/api/mes/quality/fg-shipping-fqc';
import { getOqcDetail } from '#/api/mes/quality/oqc';
import TaskWizardModal from '../../../task-center/modules/task-wizard-modal.vue';

import CutRoundFqcEventDetailPanel from './CutRoundFqcEventDetailPanel.vue';
import FaiEventDetailPanel from './FaiEventDetailPanel.vue';
import FgShippingFqcEventDetailPanel from './FgShippingFqcEventDetailPanel.vue';
import OqcEventDetailPanel from './OqcEventDetailPanel.vue';

defineOptions({ name: 'ProductAbnormalEventDetailModal' });

const emit = defineEmits(['fill-ncr', 'success']);

interface BasicField {
  label: string;
  mono?: boolean;
  value?: unknown;
}

interface CutRoundAbnormalFilter {
  inspectionItem: string;
  targetNo: string;
}

const detail =
  ref<MesProductAbnormalEventApi.ProductAbnormalEventDetail | null>(null);
const faiRecord = ref<MesFaiApi.FaiRecord | null>(null);
const cutRoundRecord = ref<MesCutRoundFqcApi.Record | null>(null);
const fgShippingRecord = ref<MesFgShippingFqcApi.Record | null>(null);
const oqcRecord = ref<MesOqcApi.OqcRecord | null>(null);
const loading = ref(false);
const detailMaximized = ref(false);
const cutRoundAbnormalFilter = ref<CutRoundAbnormalFilter | null>(null);
const abnormalSummaryExpanded = ref(false);
const showOnlyRecheck = ref(false);
const taskWizardRef = ref<InstanceType<typeof TaskWizardModal>>();

const [Modal, modalApi] = useVbenModal({
  class: 'qms-product-event-detail-modal',
  closeOnClickModal: false,
  contentClass: '!p-0 overflow-hidden',
  footer: false,
  fullscreen: true,
  fullscreenButton: false,
  header: false,
  showCancelButton: false,
  showConfirmButton: false,
  title: '产品异常事件查看',
  async onOpenChange(isOpen) {
    if (!isOpen) {
      resetDetailState();
      return;
    }
    const data =
      modalApi.getData<{
        inspectionId?: number;
        sourceType?: MesProductAbnormalEventApi.SourceType;
      }>() || {};
    if (!data.sourceType || !data.inspectionId) {
      message.warning('缺少检验单来源信息');
      return;
    }
    loading.value = true;
    try {
      modalApi.setState({ fullscreen: true });
      detail.value = await getProductAbnormalEventDetail(
        data.sourceType,
        data.inspectionId,
      );
      showOnlyRecheck.value = !!detail.value?.recheckGroupId;
      try {
        await loadSourceDetail(
          data.sourceType,
          resolveSourceDetailInspectionId(data.inspectionId),
        );
      } catch {
        message.warning('源检验详情读取失败，已展示统一异常明细');
      }
    } finally {
      loading.value = false;
    }
  },
});

const basicFields = computed<BasicField[]>(() => {
  const current = detail.value;
  if (!current) return [];
  return [
    { label: '检验类型', value: current.inspectionType },
    { label: '生产工单', mono: true, value: current.workOrderNo },
    { label: '检验工序', value: current.operationName },
    { label: '产品型号', value: current.productModel },
    { label: '产品批次', mono: true, value: current.productBatchNo },
    { label: '物料编码', mono: true, value: current.materialCode },
    { label: '规格型号', value: current.specification },
    { label: '送检数量', value: current.inspectionQty },
    { label: '送检时间', value: current.submissionTime },
    { label: '检验时间', value: current.inspectionTime },
    { label: '送检人', value: current.submitterName },
    { label: '检验人', value: current.inspectorName },
  ];
});

const cutRoundAbnormalItems = computed(() => {
  if (detail.value?.sourceType !== 'CUT_ROUND_FQC') return [];
  return (detail.value.abnormalItems || []).filter(
    (item): item is CutRoundAbnormalFilter =>
      !!item.targetNo?.trim() && !!item.inspectionItem?.trim(),
  );
});

const detailSummaryText = computed(() => {
  const sourceType = detail.value?.sourceType;
  if (sourceType === 'FAI' || sourceType === 'GLUE_BOARD_FAI') {
    const count = faiRecord.value
      ? filterRecheckItems(faiRecord.value.items || [], onlyRecheckActive.value)
          .length
      : fallbackDetails.value.length;
    return `${count} 项${onlyRecheckActive.value ? '（复检项）' : ''}`;
  }
  if (sourceType === 'CUT_ROUND_FQC') {
    const details = filterRecheckCutRoundDetails(
      cutRoundRecord.value?.submissionDetails || [],
      cutRoundRecord.value?.items || [],
      onlyRecheckActive.value,
    );
    const itemCount = details.reduce(
      (sum, row) =>
        sum +
        filterRecheckItems(
          row.items?.length
            ? row.items
            : fqcItemsForDetail(row, cutRoundRecord.value?.items || []),
          onlyRecheckActive.value,
        ).length,
      0,
    );
    return `${details.length} 个片号 / ${itemCount} 项${onlyRecheckActive.value ? '（复检项）' : ''}`;
  }
  if (sourceType === 'FG_SHIPPING_FQC') {
    const details = filterRecheckShippingDetails(
      fgShippingRecord.value?.shippingDetails || [],
      fgShippingRecord.value?.items || [],
      onlyRecheckActive.value,
    );
    const itemCount = details.reduce(
      (sum, row) =>
        sum +
        filterRecheckItems(
          row.items?.length
            ? row.items
            : fqcItemsForShippingDetail(row, fgShippingRecord.value?.items || []),
          onlyRecheckActive.value,
        ).length,
      0,
    );
    return `${details.length} 个片号 / ${itemCount} 项${onlyRecheckActive.value ? '（复检项）' : ''}`;
  }
  if (sourceType === 'OQC') {
    const count = oqcRecord.value
      ? filterRecheckItems(oqcRecord.value.items || [], onlyRecheckActive.value)
          .length
      : fallbackDetails.value.length;
    return `${count} 项${onlyRecheckActive.value ? '（复检项）' : ''}`;
  }
  return `${fallbackDetails.value.length} 行`;
});

const hasSourceDetail = computed(() => {
  const sourceType = detail.value?.sourceType;
  if (sourceType === 'FAI' || sourceType === 'GLUE_BOARD_FAI') {
    return !!faiRecord.value;
  }
  if (sourceType === 'CUT_ROUND_FQC') return !!cutRoundRecord.value;
  if (sourceType === 'FG_SHIPPING_FQC') return !!fgShippingRecord.value;
  if (sourceType === 'OQC') return !!oqcRecord.value;
  return false;
});

const hasRecheckFilter = computed(
  () =>
    !!detail.value?.recheckGroupId ||
    !!faiRecord.value?.recheckFlag ||
    !!cutRoundRecord.value?.recheckFlag ||
    !!fgShippingRecord.value?.recheckFlag ||
    !!oqcRecord.value?.recheckFlag,
);

const onlyRecheckActive = computed(
  () => hasRecheckFilter.value && showOnlyRecheck.value,
);

const fallbackDetails = computed(() => {
  const rows = detail.value?.details || [];
  if (!onlyRecheckActive.value) return rows;
  return rows.filter(
    (row) => row.recheckDetailFlag || row.recheckItemFlag,
  );
});

const detailColumns = [
  { align: 'center', dataIndex: 'rowNo', title: '序号', width: 64 },
  { dataIndex: 'sectionName', title: '明细来源', width: 130 },
  { dataIndex: 'inspectionItem', title: '检验项目', width: 220 },
  { dataIndex: 'itemType', title: '类型', width: 90 },
  { dataIndex: 'standardDesc', title: '标准/要求', width: 260 },
  { align: 'right', dataIndex: 'sampleSize', title: '样本数', width: 90 },
  { dataIndex: 'measuredValue', title: '实测/统计值', width: 170 },
  { dataIndex: 'unit', title: '单位', width: 80 },
  { align: 'center', dataIndex: 'result', title: '判定', width: 90 },
  { dataIndex: 'defectName', title: '缺陷', width: 130 },
  { dataIndex: 'abnormalDesc', title: '异常描述', width: 220 },
  { dataIndex: 'inspectorName', title: '检验员', width: 110 },
  { dataIndex: 'inspectionTime', title: '检验时间', width: 170 },
  { dataIndex: 'remark', title: '备注', width: 180 },
];

function resetDetailState() {
  detail.value = null;
  detailMaximized.value = false;
  cutRoundAbnormalFilter.value = null;
  abnormalSummaryExpanded.value = false;
  showOnlyRecheck.value = false;
  clearSourceDetail();
}

function clearSourceDetail() {
  faiRecord.value = null;
  cutRoundRecord.value = null;
  fgShippingRecord.value = null;
  oqcRecord.value = null;
}

async function loadSourceDetail(
  sourceType: MesProductAbnormalEventApi.SourceType,
  inspectionId: number,
) {
  clearSourceDetail();
  if (sourceType === 'FAI') {
    faiRecord.value = await getFaiDetail(inspectionId);
    return;
  }
  if (sourceType === 'GLUE_BOARD_FAI') {
    faiRecord.value = await getGlueBoardFaiDetail(inspectionId);
    return;
  }
  if (sourceType === 'CUT_ROUND_FQC') {
    cutRoundRecord.value = await getCutRoundFqcLightDetail(inspectionId);
    return;
  }
  if (sourceType === 'FG_SHIPPING_FQC') {
    fgShippingRecord.value = await getFgShippingFqcDetail(inspectionId);
    return;
  }
  if (sourceType === 'OQC') {
    oqcRecord.value = await getOqcDetail(inspectionId);
  }
}

function resolveSourceDetailInspectionId(fallbackInspectionId: number) {
  if (detail.value?.recheckGroupId && detail.value.recheckLatestInspectionId) {
    return detail.value.recheckLatestInspectionId;
  }
  return fallbackInspectionId;
}

function closeModal() {
  modalApi.close();
}

function toggleDetailMaximized() {
  detailMaximized.value = !detailMaximized.value;
}

function selectCutRoundAbnormalItem(item: CutRoundAbnormalFilter) {
  if (isCutRoundAbnormalItemActive(item)) {
    cutRoundAbnormalFilter.value = null;
    return;
  }
  cutRoundAbnormalFilter.value = {
    inspectionItem: item.inspectionItem,
    targetNo: item.targetNo,
  };
}

function isCutRoundAbnormalItemActive(item: CutRoundAbnormalFilter) {
  return (
    cutRoundAbnormalFilter.value?.targetNo === item.targetNo &&
    cutRoundAbnormalFilter.value?.inspectionItem === item.inspectionItem
  );
}

function displayValue(value?: unknown) {
  if (value === undefined || value === null || value === '') {
    return '-';
  }
  return String(value);
}

function displayCodeLabel(value?: unknown) {
  const normalized = typeof value === 'string' ? value.trim().toUpperCase() : '';
  if (normalized === 'NG') return '不合格';
  if (normalized === 'OK') return '合格';
  if (normalized === 'PENDING') return '待判定';
  if (normalized === 'ABNORMAL') return '异常';
  if (normalized === 'COMPLETED' || normalized === 'COMPLETE') return '已完成';
  if (normalized === 'REJECTED') return '已驳回';
  if (normalized === 'CANCELED') return '已取消';
  return displayValue(value);
}

function resultColor(value?: string) {
  if (value === 'NG' || value === 'ABNORMAL') return 'error';
  if (value === 'OK' || value === 'COMPLETE') return 'success';
  if (value === 'PENDING') return 'warning';
  return 'default';
}

function statusColor(value?: string) {
  if (value === 'COMPLETED') return 'success';
  if (value === 'REJECTED' || value === 'CANCELED') return 'error';
  if (value === 'PENDING') return 'warning';
  return 'processing';
}

function openRejectRecheck() {
  const current = detail.value;
  if (!current?.sourceType || !current.inspectionId) {
    message.warning('缺少检验单来源信息，无法驳回复检');
    return;
  }
  const inspectionId =
    current.recheckStatus === 'RECHECK_NG' && current.recheckLatestInspectionId
      ? current.recheckLatestInspectionId
      : current.inspectionId;
  const inspectionNo =
    current.recheckStatus === 'RECHECK_NG' && current.recheckLatestInspectionNo
      ? current.recheckLatestInspectionNo
      : current.inspectionNo;
  if (current.recheckStatus === 'RECHECKING') {
    message.warning('该检验单正在复检中，不能重复驳回');
    return;
  }
  if (current.canRejectRecheck === false) {
    message.warning('该检验单当前状态不允许驳回复检');
    return;
  }
  const checkType = productEventCheckType(current.sourceType);
  if (!checkType) {
    message.warning('当前检验场景暂不支持驳回复检');
    return;
  }
  taskWizardRef.value?.open({
    abnormalSummary: current.abnormalSummary,
    checkType,
    inspectionId,
    inspectionNo,
    inspectionScene: current.sourceType,
    mode: 'PRODUCT_EVENT_RECHECK',
  });
}

function hasRecheckItemFlag(item?: {
  qaValues?: Array<{ recheckItemFlag?: boolean }>;
  recheckItemFlag?: boolean;
  samples?: Array<{ recheckItemFlag?: boolean }>;
}) {
  return (
    !!item?.recheckItemFlag ||
    (item?.samples || []).some((sample) => !!sample.recheckItemFlag) ||
    (item?.qaValues || []).some((sample) => !!sample.recheckItemFlag)
  );
}

function filterRecheckItems<T extends {
  qaValues?: Array<{ recheckItemFlag?: boolean }>;
  recheckItemFlag?: boolean;
  samples?: Array<{ recheckItemFlag?: boolean }>;
}>(items: T[], onlyRecheck: boolean) {
  if (!onlyRecheck) return items;
  return items.filter((item) => hasRecheckItemFlag(item));
}

function fqcItemsForDetail(
  detailRow: MesCutRoundFqcApi.SubmissionDetail,
  items: MesCutRoundFqcApi.FqcItem[],
) {
  return items.filter(
    (item) =>
      item.submissionDetailId === detailRow.id ||
      item.cutRoundInspectionDetailId === detailRow.cutRoundInspectionDetailId ||
      item.productionBatchNo === detailRow.productionBatchNo,
  );
}

function fqcItemsForShippingDetail(
  detailRow: MesFgShippingFqcApi.ShippingDetail,
  items: MesFgShippingFqcApi.FqcItem[],
) {
  return items.filter(
    (item) =>
      item.submissionDetailId === detailRow.id ||
      item.cutRoundInspectionDetailId === detailRow.shippingNoticeItemId ||
      item.productionBatchNo === detailRow.actualSliceBatchNo ||
      item.productionBatchNo === detailRow.sliceBatchNo,
  );
}

function filterRecheckCutRoundDetails(
  rows: MesCutRoundFqcApi.SubmissionDetail[],
  items: MesCutRoundFqcApi.FqcItem[],
  onlyRecheck: boolean,
) {
  if (!onlyRecheck) return rows;
  return rows.filter(
    (row) =>
      row.recheckDetailFlag ||
      filterRecheckItems(
        row.items?.length ? row.items : fqcItemsForDetail(row, items),
        true,
      ).length > 0,
  );
}

function filterRecheckShippingDetails(
  rows: MesFgShippingFqcApi.ShippingDetail[],
  items: MesFgShippingFqcApi.FqcItem[],
  onlyRecheck: boolean,
) {
  if (!onlyRecheck) return rows;
  return rows.filter(
    (row) =>
      row.recheckDetailFlag ||
      filterRecheckItems(
        row.items?.length ? row.items : fqcItemsForShippingDetail(row, items),
        true,
      ).length > 0,
  );
}

function productEventCheckType(sourceType?: string) {
  if (sourceType === 'FAI' || sourceType === 'GLUE_BOARD_FAI') return sourceType;
  if (sourceType === 'CUT_ROUND_FQC' || sourceType === 'FG_SHIPPING_FQC') return 'FQC';
  if (sourceType === 'OQC') return 'OQC';
  return undefined;
}

function handleRecheckSuccess() {
  emit('success');
  modalApi.close();
}

function handleFillNcr() {
  const current = detail.value;
  if (!current?.sourceType || !current.inspectionId) {
    message.warning('缺少检验单来源信息，无法填写 NCR');
    return;
  }
  if (!current.ncrGenerated && current.canGenerateNcr === false) {
    message.warning('该记录处于复检中或复检OK，不能生成 NCR');
    return;
  }
  emit('fill-ncr', current);
}
</script>

<template>
  <Modal>
    <TaskWizardModal ref="taskWizardRef" @success="handleRecheckSuccess" />
    <div
      class="product-event-detail-shell"
      :class="{ 'is-detail-maximized': detailMaximized }"
    >
      <header class="detail-toolbar">
        <div class="detail-toolbar-left">
          <Tag v-if="detail" color="blue" class="!m-0">
            {{ detail.inspectionType || '-' }}
          </Tag>
        </div>
        <div class="detail-toolbar-title">
          <strong>产品异常事件查看</strong>
          <div class="detail-toolbar-subtitle">
            <span class="detail-toolbar-subtitle-label">判定结果</span>
            <Tag :color="resultColor(detail?.judgment)" class="!m-0">
              {{ displayCodeLabel(detail?.judgment) }}
            </Tag>
          </div>
        </div>
        <div class="detail-toolbar-actions">
          <Button size="small" @click="openRejectRecheck">
            <IconifyIcon icon="lucide:rotate-ccw" class="mr-1" />
            驳回重检
          </Button>
          <Button
            size="small"
            type="primary"
            @click="handleFillNcr"
          >
            <IconifyIcon
              :icon="detail?.ncrGenerated ? 'lucide:file-search' : 'lucide:file-plus-2'"
              class="mr-1"
            />
            {{ detail?.ncrGenerated ? '查看NCR' : '填写NCR' }}
          </Button>
          <Button size="small" @click="closeModal">
            <IconifyIcon icon="lucide:x" class="mr-1" />
            关闭
          </Button>
        </div>
      </header>

      <Spin :spinning="loading" class="detail-spin">
        <div v-if="detail" class="detail-content">
          <section class="erp-basic-form">
            <div class="erp-form-grid">
              <template v-for="field in basicFields" :key="field.label">
                <div class="erp-form-label">{{ field.label }}</div>
                <div class="erp-form-value" :class="{ mono: field.mono }">
                  <template v-if="field.label === '单据状态'">
                    <Tag :color="statusColor(detail.status)" class="!m-0">
                      {{ displayCodeLabel(field.value) }}
                    </Tag>
                  </template>
                  <template v-else-if="field.label === '判定结果'">
                    <Tag :color="resultColor(detail.judgment)" class="!m-0">
                      {{ displayCodeLabel(field.value) }}
                    </Tag>
                  </template>
                  <template v-else>
                    {{ displayValue(field.value) }}
                  </template>
                </div>
              </template>
            </div>
            <div class="erp-summary-row">
              <div class="erp-summary-label">异常总结</div>
              <div
                class="erp-summary-content"
                :class="{
                  'is-expanded': abnormalSummaryExpanded,
                  'is-collapsed': !abnormalSummaryExpanded,
                }"
              >
                <template v-if="cutRoundAbnormalItems.length > 0">
                  <div class="abnormal-summary-items">
                    <button
                      v-for="item in cutRoundAbnormalItems"
                      :key="`${item.targetNo}-${item.inspectionItem}`"
                      :aria-pressed="isCutRoundAbnormalItemActive(item)"
                      class="abnormal-summary-pill"
                      :class="{
                        active: isCutRoundAbnormalItemActive(item),
                      }"
                      type="button"
                      @click="selectCutRoundAbnormalItem(item)"
                    >
                      <span class="mono">{{ item.targetNo }}</span>
                      <span>片号，{{ item.inspectionItem }} 不合格项目</span>
                    </button>
                  </div>
                  <div class="abnormal-summary-actions">
                    <button
                      v-if="cutRoundAbnormalFilter"
                      class="abnormal-summary-clear"
                      type="button"
                      @click="cutRoundAbnormalFilter = null"
                    >
                      显示全部
                    </button>
                    <button
                      v-if="cutRoundAbnormalItems.length > 1"
                      class="abnormal-summary-toggle"
                      type="button"
                      @click="
                        abnormalSummaryExpanded = !abnormalSummaryExpanded
                      "
                    >
                      {{ abnormalSummaryExpanded ? '收起' : '更多' }}
                    </button>
                  </div>
                </template>
                <span v-else class="abnormal-summary-text">
                  {{ displayValue(detail.abnormalSummary) }}
                </span>
              </div>
            </div>
          </section>

          <section class="inspection-detail-list">
            <div class="detail-list-head">
              <div class="detail-list-title">
                <strong>检验详情列表</strong>
                <span>{{ detailSummaryText }}</span>
              </div>
              <div class="detail-list-tools">
                <Checkbox
                  v-if="hasRecheckFilter"
                  v-model:checked="showOnlyRecheck"
                >
                  只看复检项
                </Checkbox>
                <Button size="small" @click="toggleDetailMaximized">
                  <IconifyIcon
                    :icon="
                      detailMaximized
                        ? 'lucide:minimize-2'
                        : 'lucide:maximize-2'
                    "
                    class="mr-1"
                  />
                  {{ detailMaximized ? '还原' : '最大化' }}
                </Button>
              </div>
            </div>

            <div class="detail-table-host">
              <template v-if="hasSourceDetail">
                <FaiEventDetailPanel
                  v-if="
                    detail.sourceType === 'FAI' ||
                    detail.sourceType === 'GLUE_BOARD_FAI'
                  "
                  :only-recheck="onlyRecheckActive"
                  :record="faiRecord"
                />
                <CutRoundFqcEventDetailPanel
                  v-else-if="detail.sourceType === 'CUT_ROUND_FQC'"
                  :active-abnormal-filter="cutRoundAbnormalFilter"
                  :only-recheck="onlyRecheckActive"
                  :record="cutRoundRecord"
                  @clear-abnormal-filter="cutRoundAbnormalFilter = null"
                />
                <FgShippingFqcEventDetailPanel
                  v-else-if="detail.sourceType === 'FG_SHIPPING_FQC'"
                  :only-recheck="onlyRecheckActive"
                  :record="fgShippingRecord"
                />
                <OqcEventDetailPanel
                  v-else-if="detail.sourceType === 'OQC'"
                  :only-recheck="onlyRecheckActive"
                  :record="oqcRecord"
                />
              </template>
              <div
                v-else-if="fallbackDetails.length"
                class="fallback-table-scroll"
              >
                <Table
                  bordered
                  :columns="detailColumns"
                  :data-source="fallbackDetails"
                  :pagination="false"
                  row-key="rowNo"
                  size="small"
                  class="fallback-detail-table"
                >
                  <template #bodyCell="{ column, record }">
                    <template v-if="column.dataIndex === 'result'">
                      <Tag :color="resultColor(record.result)" class="!m-0">
                        {{ displayCodeLabel(record.result) }}
                      </Tag>
                    </template>
                    <template v-else-if="column.dataIndex === 'standardDesc'">
                      <span class="whitespace-pre-wrap">
                        {{ record.standardDesc || '-' }}
                      </span>
                    </template>
                    <template v-else-if="column.dataIndex === 'abnormalDesc'">
                      <span class="text-red-700">
                        {{ record.abnormalDesc || '-' }}
                      </span>
                    </template>
                    <template v-else>
                      {{ displayValue(record[column.dataIndex]) }}
                    </template>
                  </template>
                </Table>
              </div>
              <Empty v-else class="py-14" description="暂无检验详情" />
            </div>
          </section>
        </div>
        <div v-else class="empty-host">
          <Empty description="暂无产品异常事件详情" />
        </div>
      </Spin>
    </div>
  </Modal>
</template>

<style>
.qms-product-event-detail-modal [class*='modal__header'],
.qms-product-event-detail-modal .ant-modal-header {
  display: none !important;
}

.qms-product-event-detail-modal [class*='modal__body'],
.qms-product-event-detail-modal .ant-modal-body {
  height: 100dvh !important;
  padding: 0 !important;
  overflow: hidden !important;
  background: #eef2f6;
}

.qms-product-event-detail-modal [class*='modal__content'],
.qms-product-event-detail-modal .ant-modal-content {
  height: 100dvh !important;
  padding: 0 !important;
  overflow: hidden !important;
}

.qms-product-event-detail-modal .ant-spin-nested-loading,
.qms-product-event-detail-modal .ant-spin-container {
  height: 100%;
  min-height: 0;
}

.qms-product-event-detail-modal .ant-spin-container {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
</style>

<style scoped>
.product-event-detail-shell {
  display: flex;
  height: 100%;
  min-height: 0;
  flex-direction: column;
  overflow: hidden;
  background: #eef2f6;
  color: #1f2937;
}

.detail-toolbar {
  display: grid;
  flex: 0 0 56px;
  grid-template-columns: minmax(180px, 1fr) auto minmax(280px, 1fr);
  align-items: center;
  gap: 12px;
  height: 56px;
  border-bottom: 1px solid #cbd5e1;
  background: #fff;
  padding: 0 16px;
}

.detail-toolbar-left {
  min-width: 0;
}

.detail-toolbar-title {
  min-width: 260px;
  text-align: center;
  line-height: 1.2;
}

.detail-toolbar-title strong {
  display: block;
  color: #0f172a;
  font-size: 17px;
  font-weight: 800;
}

.detail-toolbar-subtitle {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  margin-top: 4px;
}

.detail-toolbar-subtitle-label {
  color: #64748b;
  font-size: 12px;
}

.detail-toolbar-actions {
  display: flex;
  min-width: 0;
  justify-content: flex-end;
  gap: 8px;
}

.detail-spin {
  display: block;
  flex: 1 1 0%;
  min-height: 0;
  overflow: hidden;
}

.detail-spin :deep(.ant-spin-nested-loading),
.detail-spin :deep(.ant-spin-container) {
  height: 100%;
  min-height: 0;
}

.detail-content {
  display: flex;
  flex: 1 1 0%;
  min-height: 0;
  flex-direction: column;
  overflow: hidden;
}

.erp-basic-form {
  flex: 0 1 auto;
  max-height: 38dvh;
  margin: 12px 12px 0;
  overflow: auto;
  border: 1px solid #cbd5e1;
  background: #fff;
}

.erp-form-grid {
  display: grid;
  grid-template-columns: repeat(4, 110px minmax(120px, 1fr));
  border-top: 1px solid #e2e8f0;
  border-left: 1px solid #e2e8f0;
}

.erp-form-label,
.erp-form-value {
  min-height: 38px;
  border-right: 1px solid #e2e8f0;
  border-bottom: 1px solid #e2e8f0;
  padding: 9px 10px;
  font-size: 13px;
  line-height: 20px;
}

.erp-form-label {
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f1f5f9;
  color: #475569;
  font-weight: 700;
  white-space: nowrap;
}

.erp-form-value {
  min-width: 0;
  overflow-wrap: anywhere;
  background: #fff;
  color: #111827;
}

.erp-form-value.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
}

.erp-summary-row {
  display: grid;
  grid-template-columns: 110px minmax(0, 1fr);
  border-left: 1px solid #e2e8f0;
}

.erp-summary-label,
.erp-summary-content {
  min-height: 44px;
  border-right: 1px solid #e2e8f0;
  border-bottom: 1px solid #e2e8f0;
}

.erp-summary-label {
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f1f5f9;
  color: #475569;
  font-size: 13px;
  font-weight: 700;
}

.erp-summary-content {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  min-width: 0;
  align-items: start;
  gap: 6px;
  background: #fff;
  padding: 7px 10px;
}

.abnormal-summary-items {
  display: flex;
  min-width: 0;
  flex-wrap: wrap;
  gap: 6px;
}

.erp-summary-content.is-collapsed .abnormal-summary-items {
  flex-wrap: nowrap;
  overflow: hidden;
}

.abnormal-summary-actions {
  display: flex;
  align-items: center;
  gap: 6px;
}

.abnormal-summary-pill,
.abnormal-summary-clear,
.abnormal-summary-toggle {
  min-height: 28px;
  border-radius: 999px;
  padding: 4px 10px;
  font-size: 12px;
  line-height: 18px;
  cursor: pointer;
}

.abnormal-summary-pill {
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 4px;
  border: 1px solid #fca5a5;
  background: #fff1f2;
  color: #b91c1c;
}

.abnormal-summary-pill:hover,
.abnormal-summary-pill.active {
  border-color: #dc2626;
  background: #dc2626;
  color: #fff;
}

.abnormal-summary-clear,
.abnormal-summary-toggle {
  border: 1px solid #cbd5e1;
  background: #fff;
  color: #475569;
}

.abnormal-summary-clear:hover,
.abnormal-summary-toggle:hover {
  border-color: #64748b;
  color: #0f172a;
}

.abnormal-summary-text {
  grid-column: 1 / -1;
  color: #b91c1c;
  overflow-wrap: anywhere;
}

.inspection-detail-list {
  display: flex;
  flex: 1 1 0%;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
  margin: 12px;
  border: 1px solid #cbd5e1;
  background: #fff;
}

.detail-list-head {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 44px;
  border-bottom: 1px solid #e2e8f0;
  background: #f8fafc;
  padding: 8px 12px;
}

.detail-list-title {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.detail-list-title strong {
  color: #0f172a;
  font-size: 14px;
}

.detail-list-title span {
  color: #64748b;
  font-size: 12px;
}

.detail-list-tools {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  gap: 12px;
}

.detail-table-host {
  display: flex;
  flex: 1 1 0%;
  min-height: 0;
  flex-direction: column;
  overflow: hidden;
  padding: 8px;
}

.is-detail-maximized .erp-basic-form {
  display: none;
}

.is-detail-maximized .inspection-detail-list {
  margin-top: 12px;
}

.empty-host {
  display: flex;
  height: 100%;
  align-items: center;
  justify-content: center;
}

.fallback-table-scroll {
  flex: 1 1 auto;
  min-height: 0;
  overflow: auto;
  scrollbar-color: #64748b #e2e8f0;
}

.fallback-table-scroll::-webkit-scrollbar {
  width: 12px;
  height: 12px;
}

.fallback-table-scroll::-webkit-scrollbar-track {
  background: #e2e8f0;
}

.fallback-table-scroll::-webkit-scrollbar-thumb {
  border: 2px solid #e2e8f0;
  background: #64748b;
  border-radius: 6px;
}

.fallback-detail-table {
  min-width: 2060px;
}

.fallback-detail-table :deep(.ant-table-thead > tr > th) {
  position: sticky;
  z-index: 2;
  top: 0;
}

.detail-reject-recheck {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.detail-reject-recheck__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 20px;
  color: #475569;
}

@media (max-width: 1180px) {
  .detail-toolbar {
    flex-basis: auto;
    grid-template-columns: 1fr;
    height: auto;
    padding: 10px 12px;
  }

  .detail-toolbar-left,
  .detail-toolbar-title,
  .detail-toolbar-actions {
    justify-content: center;
    text-align: center;
  }

  .detail-toolbar-actions {
    flex-wrap: wrap;
  }

  .erp-form-grid {
    grid-template-columns: 96px minmax(0, 1fr) 96px minmax(0, 1fr);
  }

  .erp-summary-row {
    grid-template-columns: 96px minmax(0, 1fr);
  }
}
</style>
