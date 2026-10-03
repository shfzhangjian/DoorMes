<script lang="ts" setup>
import type { MesCutRoundFqcApi } from '#/api/mes/quality/cut-round-fqc';
import type { MesFaiApi } from '#/api/mes/quality/fai';
import type { MesFgShippingFqcApi } from '#/api/mes/quality/fg-shipping-fqc';
import type { MesOqcApi } from '#/api/mes/quality/oqc';

import { computed, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import { Button, Checkbox, Empty, message, Spin, Tag, Tooltip } from 'ant-design-vue';

import { getCutRoundFqcLightDetail } from '#/api/mes/quality/cut-round-fqc';
import { getFaiDetail, getGlueBoardFaiDetail } from '#/api/mes/quality/fai';
import { getFgShippingFqcDetail } from '#/api/mes/quality/fg-shipping-fqc';
import { getOqcDetail } from '#/api/mes/quality/oqc';

import CutRoundFqcEventDetailPanel from '../abnormal/product-event/modules/CutRoundFqcEventDetailPanel.vue';
import FaiEventDetailPanel from '../abnormal/product-event/modules/FaiEventDetailPanel.vue';
import FgShippingFqcEventDetailPanel from '../abnormal/product-event/modules/FgShippingFqcEventDetailPanel.vue';
import OqcEventDetailPanel from '../abnormal/product-event/modules/OqcEventDetailPanel.vue';

defineOptions({ name: 'QmsInspectionDetailModal' });

type InspectionSourceType =
  | 'CUT_ROUND_FQC'
  | 'FAI'
  | 'FG_SHIPPING_FQC'
  | 'GLUE_BOARD_FAI'
  | 'OQC';

type InspectionRecord =
  | MesCutRoundFqcApi.Record
  | MesFaiApi.FaiRecord
  | MesFgShippingFqcApi.Record
  | MesOqcApi.OqcRecord;

interface BasicField {
  label: string;
  mono?: boolean;
  value?: unknown;
}

interface ModalData {
  inspectionId?: number;
  inspectionNo?: string;
  ngOnly?: boolean;
  sourceType?: string;
  title?: string;
}

const SUPPORTED_SOURCE_TYPES = new Set<string>([
  'CUT_ROUND_FQC',
  'FAI',
  'FG_SHIPPING_FQC',
  'GLUE_BOARD_FAI',
  'OQC',
]);

const loading = ref(false);
const emptyText = ref('请选择检验单');
const inspectionNo = ref('');
const sourceType = ref<InspectionSourceType>();
const customTitle = ref('');
const detailExpanded = ref(false);
const detailScale = ref(100);
const faiRecord = ref<MesFaiApi.FaiRecord | null>(null);
const fullscreenState = ref(true);
const cutRoundRecord = ref<MesCutRoundFqcApi.Record | null>(null);
const fgShippingRecord = ref<MesFgShippingFqcApi.Record | null>(null);
const ngOnly = ref(false);
const oqcRecord = ref<MesOqcApi.OqcRecord | null>(null);
const zoomLevels = [90, 100, 110, 125];

const [Modal, modalApi] = useVbenModal({
  class: 'qms-inspection-detail-modal',
  closeOnClickModal: false,
  contentClass: '!p-0 overflow-hidden',
  footer: false,
  fullscreen: true,
  fullscreenButton: false,
  header: false,
  showCancelButton: false,
  showConfirmButton: false,
  title: '检验详情',
  width: 1180,
  async onOpenChange(isOpen) {
    if (!isOpen) {
      resetDetailState();
      return;
    }
    const data = modalApi.getData<ModalData>() || {};
    fullscreenState.value = true;
    modalApi.setState({ fullscreen: true });
    detailExpanded.value = false;
    detailScale.value = 100;
    ngOnly.value = Boolean(data.ngOnly);
    const rawSourceType = String(data.sourceType || '').trim();
    const nextSourceType = normalizeSourceType(data.sourceType);
    if (!rawSourceType || !data.inspectionId) {
      emptyText.value = '缺少检验单来源信息';
      message.warning(emptyText.value);
      return;
    }
    if (!nextSourceType) {
      emptyText.value = `暂不支持该检验来源：${rawSourceType}`;
      message.warning(emptyText.value);
      return;
    }
    inspectionNo.value = data.inspectionNo || '';
    sourceType.value = nextSourceType;
    customTitle.value = data.title || '';
    await loadInspectionDetail(nextSourceType, data.inspectionId);
  },
});

const currentRecord = computed<InspectionRecord | null>(() => {
  if (sourceType.value === 'FAI' || sourceType.value === 'GLUE_BOARD_FAI') {
    return faiRecord.value;
  }
  if (sourceType.value === 'CUT_ROUND_FQC') return cutRoundRecord.value;
  if (sourceType.value === 'FG_SHIPPING_FQC') return fgShippingRecord.value;
  if (sourceType.value === 'OQC') return oqcRecord.value;
  return null;
});

const hasDetail = computed(() => !!currentRecord.value);

const panelRecord = computed<InspectionRecord | null>(() => {
  const record = currentRecord.value;
  if (!record || !ngOnly.value) return record;
  if (sourceType.value === 'FAI' || sourceType.value === 'GLUE_BOARD_FAI') {
    return filterNgRecordItems(record, 'items') as InspectionRecord;
  }
  if (sourceType.value === 'CUT_ROUND_FQC') {
    return filterNgInspectionRecord(record, 'submissionDetails') as InspectionRecord;
  }
  if (sourceType.value === 'FG_SHIPPING_FQC') {
    return filterNgInspectionRecord(record, 'shippingDetails') as InspectionRecord;
  }
  if (sourceType.value === 'OQC') {
    return filterNgRecordItems(record, 'items') as InspectionRecord;
  }
  return record;
});

const faiPanelRecord = computed(() => panelRecord.value as MesFaiApi.FaiRecord | null);
const cutRoundPanelRecord = computed(
  () => panelRecord.value as MesCutRoundFqcApi.Record | null,
);
const fgShippingPanelRecord = computed(
  () => panelRecord.value as MesFgShippingFqcApi.Record | null,
);
const oqcPanelRecord = computed(() => panelRecord.value as MesOqcApi.OqcRecord | null);

const detailScaleStyle = computed<Record<string, string>>(() => ({
  '--inspection-detail-scale': String(detailScale.value / 100),
}));

const zoomText = computed(() => `${detailScale.value}%`);

const resolvedInspectionNo = computed(
  () =>
    inspectionNo.value ||
    String(recordValue('faiNo', 'fqcNo', 'oqcNo') || '-'),
);

const judgmentValue = computed(() =>
  normalizeCode(recordValue('judgment', 'releaseResult')),
);

const statusValue = computed(() => normalizeCode(recordValue('status')));

const basicFields = computed<BasicField[]>(() => [
  { label: '检验单号', mono: true, value: resolvedInspectionNo.value },
  { label: '来源单号', mono: true, value: recordValue('sourceReportNo', 'reportNo', 'shippingNo', 'noticeNo') },
  { label: '计划/工单', mono: true, value: recordValue('workOrderNo', 'planNo') },
  { label: '产品型号', value: recordValue('productModel', 'modelCode') },
  { label: '产品批次', mono: true, value: recordValue('productBatchNo', 'batchNo') },
  { label: '物料编码', mono: true, value: recordValue('materialCode') },
  { label: '物料名称', value: recordValue('materialName') },
  { label: '规格型号', value: recordValue('specification', 'productSize', 'sizeRule') },
  { label: '送检数量', value: qtyValueText() },
  { label: '送检时间', value: recordValue('submissionTime') },
  { label: '检验时间', value: recordValue('inspectionTime', 'qaTime') },
  { label: '送检人', value: recordValue('submitterName') },
  { label: '检验人', value: recordValue('qaInspectorName', 'inspectorName') },
]);

const detailSummaryText = computed(() => {
  const record = panelRecord.value as null | Record<string, any>;
  if (sourceType.value === 'FAI' || sourceType.value === 'GLUE_BOARD_FAI') {
    return `${record?.items?.length || 0} 项`;
  }
  if (sourceType.value === 'CUT_ROUND_FQC') {
    const details = record?.submissionDetails || [];
    const itemCount =
      record?.items?.length ||
      details.reduce((sum, detail) => sum + (detail.items?.length || 0), 0);
    return `${details.length} 个片号 / ${itemCount} 项`;
  }
  if (sourceType.value === 'FG_SHIPPING_FQC') {
    const details = record?.shippingDetails || [];
    const itemCount =
      record?.items?.length ||
      details.reduce((sum, detail) => sum + (detail.items?.length || 0), 0);
    return `${details.length} 个片号 / ${itemCount} 项`;
  }
  if (sourceType.value === 'OQC') {
    return `${record?.items?.length || 0} 项`;
  }
  return '-';
});

async function loadInspectionDetail(
  nextSourceType: InspectionSourceType,
  nextInspectionId: number,
) {
  loading.value = true;
  emptyText.value = '正在加载检验详情';
  clearSourceDetail();
  try {
    if (nextSourceType === 'FAI') {
      faiRecord.value = await getFaiDetail(nextInspectionId);
    } else if (nextSourceType === 'GLUE_BOARD_FAI') {
      faiRecord.value = await getGlueBoardFaiDetail(nextInspectionId);
    } else if (nextSourceType === 'CUT_ROUND_FQC') {
      cutRoundRecord.value = await getCutRoundFqcLightDetail(nextInspectionId);
    } else if (nextSourceType === 'FG_SHIPPING_FQC') {
      fgShippingRecord.value = await getFgShippingFqcDetail(nextInspectionId);
    } else if (nextSourceType === 'OQC') {
      oqcRecord.value = await getOqcDetail(nextInspectionId);
    }
    if (!currentRecord.value) {
      emptyText.value = '暂无检验详情';
    }
  } catch {
    emptyText.value = '检验详情读取失败';
    message.error(emptyText.value);
  } finally {
    loading.value = false;
  }
}

function resetDetailState() {
  inspectionNo.value = '';
  sourceType.value = undefined;
  customTitle.value = '';
  detailExpanded.value = false;
  detailScale.value = 100;
  emptyText.value = '请选择检验单';
  fullscreenState.value = true;
  ngOnly.value = false;
  clearSourceDetail();
}

function clearSourceDetail() {
  faiRecord.value = null;
  cutRoundRecord.value = null;
  fgShippingRecord.value = null;
  oqcRecord.value = null;
}

function closeModal() {
  modalApi.close();
}

function toggleFullscreen() {
  fullscreenState.value = !fullscreenState.value;
  modalApi.setState({ fullscreen: fullscreenState.value });
}

function toggleDetailExpanded() {
  detailExpanded.value = !detailExpanded.value;
}

function zoomDetail(delta: number) {
  const currentIndex = zoomLevels.indexOf(detailScale.value);
  const fallbackIndex = zoomLevels.findIndex((item) => item >= detailScale.value);
  const nextIndex = Math.min(
    zoomLevels.length - 1,
    Math.max(0, (currentIndex >= 0 ? currentIndex : fallbackIndex) + delta),
  );
  detailScale.value = zoomLevels[nextIndex] || 100;
}

function resetZoom() {
  detailScale.value = 100;
}

function normalizeSourceType(value?: unknown): InspectionSourceType | undefined {
  const normalized = String(value || '').trim().toUpperCase();
  if (!SUPPORTED_SOURCE_TYPES.has(normalized)) return undefined;
  return normalized as InspectionSourceType;
}

function sourceTypeLabel(value?: InspectionSourceType) {
  if (value === 'FAI') return '首件检验';
  if (value === 'GLUE_BOARD_FAI') return '胶板首检';
  if (value === 'CUT_ROUND_FQC') return '裁切成品检验';
  if (value === 'FG_SHIPPING_FQC') return '成品发货检验';
  if (value === 'OQC') return '出货检验';
  return '检验';
}

function recordValue(...keys: string[]) {
  const record = currentRecord.value as null | Record<string, unknown>;
  if (!record) return undefined;
  for (const key of keys) {
    const value = record[key];
    if (value !== undefined && value !== null && value !== '') return value;
  }
  return undefined;
}

function qtyValueText() {
  const rows = [
    pairText('送检', recordValue('inspectionQty')),
    pairText('抽样', recordValue('sampleQty')),
    pairText('生产', recordValue('produceQty')),
    pairText('发货', recordValue('shippingQty')),
  ].filter(Boolean);
  return rows.join(' / ') || undefined;
}

function pairText(label: string, value?: unknown) {
  if (value === undefined || value === null || value === '') return '';
  return `${label} ${value}`;
}

function normalizeCode(value?: unknown) {
  return String(value || '').trim().toUpperCase();
}

function displayValue(value?: unknown) {
  if (value === undefined || value === null || value === '') return '-';
  return String(value);
}

function displayCodeLabel(value?: unknown) {
  const normalized = normalizeCode(value);
  if (normalized === 'NG') return '不合格';
  if (normalized === 'OK') return '合格';
  if (normalized === 'PENDING') return '待判定';
  if (normalized === 'ABNORMAL') return '异常';
  if (normalized === 'CANCELED') return '已取消';
  if (normalized === 'COMPLETED' || normalized === 'COMPLETE') return '已完成';
  if (normalized === 'INSPECTING') return '检验中';
  if (normalized === 'REJECTED') return '已驳回';
  if (normalized === 'WAITING_QA') return '待检验';
  return displayValue(value);
}

function resultColor(value?: unknown) {
  const normalized = normalizeCode(value);
  if (normalized === 'NG' || normalized === 'ABNORMAL') return 'error';
  if (normalized === 'OK' || normalized === 'COMPLETED') return 'success';
  if (normalized === 'PENDING' || normalized === 'WAITING_QA') return 'warning';
  return 'default';
}

function filterNgRecordItems(record: InspectionRecord, itemsKey: string) {
  const source = record as Record<string, any>;
  const nextRecord: Record<string, any> = {
    ...source,
    [itemsKey]: filterNgRows(source[itemsKey] || []),
  };
  if (Array.isArray(source.abnormals)) {
    nextRecord.abnormals = filterNgRows(source.abnormals);
  }
  return nextRecord;
}

function filterNgInspectionRecord(record: InspectionRecord, detailsKey: string) {
  const source = record as Record<string, any>;
  const filteredItems = filterNgRows(source.items || []);
  const filteredDetails = (source[detailsKey] || [])
    .map((detail: Record<string, any>) => {
      const detailItems = filterNgRows(detail.items || []);
      const relatedItems = filteredItems.filter((item) => itemBelongsToDetail(item, detail));
      return {
        ...detail,
        items: detailItems.length > 0 ? detailItems : relatedItems,
      };
    })
    .filter(
      (detail: Record<string, any>) =>
        isNgInspectionRowSelf(detail) || (detail.items || []).length > 0,
    );
  return {
    ...source,
    [detailsKey]: filteredDetails,
    items: filteredItems,
  };
}

function filterNgRows<T>(rows: T[]) {
  return rows
    .map((row) => filterNgRow(row))
    .filter((row): row is T => !!row);
}

function filterNgRow<T>(row: T): T | null {
  if (!isObjectRow(row)) return isNgLike(row) ? row : null;
  const selfNg = isNgInspectionRowSelf(row);
  const nextRow = { ...row } as Record<string, any>;
  let childNg = false;
  for (const key of ['abnormals', 'defects', 'items', 'qaValues', 'sampleValues', 'samples']) {
    if (!Array.isArray(row[key])) continue;
    const filteredChildren = filterNgRows(row[key]);
    if (filteredChildren.length > 0) {
      nextRow[key] = filteredChildren;
      childNg = true;
    } else if (!selfNg) {
      nextRow[key] = [];
    }
  }
  return selfNg || childNg ? (nextRow as T) : null;
}

function itemBelongsToDetail(item: Record<string, any>, detail: Record<string, any>) {
  const itemDetailIds = [
    item.submissionDetailId,
    item.cutRoundInspectionDetailId,
    item.shippingNoticeItemId,
  ].filter((value) => value !== undefined && value !== null);
  const detailIds = [
    detail.id,
    detail.cutRoundInspectionDetailId,
    detail.shippingNoticeItemId,
  ].filter((value) => value !== undefined && value !== null);
  if (itemDetailIds.some((value) => detailIds.includes(value))) return true;
  const itemBatch = normalizeText(item.productionBatchNo);
  return [
    detail.productionBatchNo,
    detail.actualSliceBatchNo,
    detail.sliceBatchNo,
  ].some((value) => normalizeText(value) === itemBatch);
}

function isNgInspectionRow(row?: unknown) {
  if (!row) return false;
  if (!isObjectRow(row)) return isNgLike(row);
  if (isNgInspectionRowSelf(row)) return true;
  return ['abnormals', 'defects', 'items', 'qaValues', 'sampleValues', 'samples'].some((key) =>
    Array.isArray(row[key]) && row[key].some((item: unknown) => isNgInspectionRow(item)),
  );
}

function isNgInspectionRowSelf(row?: unknown) {
  if (!row) return false;
  if (!isObjectRow(row)) return isNgLike(row);
  const resultKeys = [
    'abnormalResult',
    'alignmentStatus',
    'qaResult',
    'itemResult',
    'rowJudgment',
    'sampleResult',
    'qualitativeValue',
    'inspectionResult',
    'judgment',
    'releaseResult',
    'result',
  ];
  if (resultKeys.some((key) => isNgLike(row[key]))) return true;
  const defectKeys = [
    'abnormalDesc',
    'abnormalReason',
    'abnormalSummary',
    'defectCode',
    'defectLevel',
    'defectName',
    'defectText',
    'mismatchReason',
    'ncDescription',
    'ngReason',
    'reasonName',
  ];
  if (defectKeys.some((key) => hasMeaningfulText(row[key]))) return true;
  return false;
}

function isObjectRow(value?: unknown): value is Record<string, any> {
  return !!value && typeof value === 'object' && !Array.isArray(value);
}

function isNgLike(value?: unknown) {
  const text = normalizeText(value);
  return (
    text === 'NG' ||
    text === 'ABNORMAL' ||
    text === 'FAIL' ||
    text === 'MISMATCH' ||
    text.includes('不合格') ||
    text.includes('不一致') ||
    text.includes('异常')
  );
}

function hasMeaningfulText(value?: unknown) {
  const text = normalizeText(value);
  return !!text && text !== '-' && text !== '无' && text !== 'NA' && text !== 'N/A';
}

function normalizeText(value?: unknown) {
  return String(value ?? '').trim().toUpperCase();
}
</script>

<template>
  <Modal>
    <div class="inspection-detail-shell">
      <Spin :spinning="loading">
        <header class="inspection-detail-header">
          <div class="inspection-title-group">
            <span class="inspection-eyebrow">
              {{ sourceTypeLabel(sourceType) }}
            </span>
            <h2>{{ customTitle || '检验详情' }}</h2>
            <div class="inspection-subtitle">
              <span class="mono">{{ resolvedInspectionNo }}</span>
              <Tag v-if="judgmentValue" :color="resultColor(judgmentValue)">
                {{ displayCodeLabel(judgmentValue) }}
              </Tag>
              <Tag v-if="statusValue" :color="resultColor(statusValue)">
                {{ displayCodeLabel(statusValue) }}
              </Tag>
            </div>
          </div>
          <div class="inspection-toolbar">
            <Checkbox v-model:checked="ngOnly">不合格项</Checkbox>
            <Tooltip :title="detailExpanded ? '恢复基础信息' : '展开检验明细'">
              <Button
                class="inspection-tool-btn"
                size="small"
                type="text"
                @click="toggleDetailExpanded"
              >
                <IconifyIcon
                  :icon="detailExpanded ? 'lucide:panel-top-open' : 'lucide:panel-bottom-open'"
                />
              </Button>
            </Tooltip>
            <div class="inspection-zoom-control">
              <Tooltip title="缩小">
                <Button
                  class="inspection-tool-btn"
                  :disabled="detailScale <= zoomLevels[0]"
                  size="small"
                  type="text"
                  @click="zoomDetail(-1)"
                >
                  <IconifyIcon icon="lucide:zoom-out" />
                </Button>
              </Tooltip>
              <button class="inspection-zoom-value" type="button" @click="resetZoom">
                {{ zoomText }}
              </button>
              <Tooltip title="放大">
                <Button
                  class="inspection-tool-btn"
                  :disabled="detailScale >= zoomLevels[zoomLevels.length - 1]"
                  size="small"
                  type="text"
                  @click="zoomDetail(1)"
                >
                  <IconifyIcon icon="lucide:zoom-in" />
                </Button>
              </Tooltip>
            </div>
            <Tooltip :title="fullscreenState ? '恢复窗口' : '最大化'">
              <Button
                class="inspection-tool-btn"
                size="small"
                type="text"
                @click="toggleFullscreen"
              >
                <IconifyIcon
                  :icon="fullscreenState ? 'lucide:minimize-2' : 'lucide:maximize-2'"
                />
              </Button>
            </Tooltip>
            <Button class="inspection-close-btn" type="text" @click="closeModal">
              <IconifyIcon icon="lucide:x" />
            </Button>
          </div>
        </header>

        <main
          v-if="hasDetail"
          class="inspection-detail-body"
          :class="{ 'is-detail-expanded': detailExpanded }"
        >
          <section class="inspection-basic-panel">
            <div
              v-for="field in basicFields"
              :key="field.label"
              class="inspection-basic-item"
            >
              <span>{{ field.label }}</span>
              <strong :class="{ mono: field.mono }">
                {{ displayValue(field.value) }}
              </strong>
            </div>
          </section>

          <section class="inspection-list-panel">
            <div class="inspection-list-head">
              <strong>检验明细</strong>
              <span>{{ detailSummaryText }}</span>
            </div>
            <div class="inspection-list-host" :style="detailScaleStyle">
              <div class="inspection-list-zoom-surface">
                <FaiEventDetailPanel
                  v-if="sourceType === 'FAI' || sourceType === 'GLUE_BOARD_FAI'"
                  :record="faiPanelRecord"
                />
                <CutRoundFqcEventDetailPanel
                  v-else-if="sourceType === 'CUT_ROUND_FQC'"
                  :record="cutRoundPanelRecord"
                />
                <FgShippingFqcEventDetailPanel
                  v-else-if="sourceType === 'FG_SHIPPING_FQC'"
                  :record="fgShippingPanelRecord"
                />
                <OqcEventDetailPanel
                  v-else-if="sourceType === 'OQC'"
                  :record="oqcPanelRecord"
                />
              </div>
            </div>
          </section>
        </main>

        <div v-else class="inspection-empty-host">
          <Empty :description="emptyText" />
        </div>
      </Spin>
    </div>
  </Modal>
</template>

<style>
.qms-inspection-detail-modal [class*='modal__header'],
.qms-inspection-detail-modal .ant-modal-header {
  display: none !important;
}

.qms-inspection-detail-modal .ant-modal {
  max-width: calc(100vw - 16px) !important;
  padding-bottom: 0 !important;
}

.qms-inspection-detail-modal [class*='modal__body'],
.qms-inspection-detail-modal .ant-modal-body {
  height: calc(100vh - 16px) !important;
  padding: 0 !important;
  overflow: hidden !important;
  background: #eef2f6;
}

.qms-inspection-detail-modal [class*='modal__content'],
.qms-inspection-detail-modal .ant-modal-content {
  padding: 0 !important;
  overflow: hidden !important;
}

.qms-inspection-detail-modal .ant-spin-nested-loading,
.qms-inspection-detail-modal .ant-spin-container {
  height: 100%;
  min-height: 0;
}

.qms-inspection-detail-modal .ant-spin-container {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
</style>

<style scoped>
.inspection-detail-shell {
  display: flex;
  height: 100%;
  min-height: 0;
  flex-direction: column;
  overflow: hidden;
  background: #eef2f6;
  color: #1f2937;
}

.inspection-detail-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 24px 14px;
  border-bottom: 1px solid #d9e2ee;
  background: #ffffff;
}

.inspection-title-group {
  display: grid;
  gap: 6px;
  min-width: 0;
}

.inspection-eyebrow {
  color: #1d6fae;
  font-size: 13px;
  font-weight: 700;
}

.inspection-title-group h2 {
  margin: 0;
  color: #0f172a;
  font-size: 22px;
  font-weight: 800;
  letter-spacing: 0;
  line-height: 1.2;
}

.inspection-subtitle {
  display: flex;
  min-width: 0;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  color: #64748b;
}

.inspection-toolbar {
  display: flex;
  min-width: max-content;
  flex: 0 0 auto;
  align-items: center;
  gap: 8px;
}

.inspection-tool-btn,
.inspection-close-btn {
  display: inline-flex;
  width: 36px;
  height: 36px;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  color: #64748b;
}

.inspection-zoom-control {
  display: inline-flex;
  overflow: hidden;
  align-items: center;
  border: 1px solid #d9e2ee;
  border-radius: 8px;
  background: #f8fafc;
}

.inspection-zoom-control .inspection-tool-btn {
  width: 32px;
  height: 32px;
}

.inspection-zoom-value {
  width: 54px;
  height: 32px;
  border: 0;
  border-right: 1px solid #d9e2ee;
  border-left: 1px solid #d9e2ee;
  background: transparent;
  color: #334155;
  cursor: pointer;
  font-size: 12px;
  font-weight: 700;
}

.inspection-detail-body {
  display: grid;
  min-height: 0;
  flex: 1;
  grid-template-rows: max-content minmax(0, 1fr);
  gap: 12px;
  padding: 12px;
  overflow: hidden;
}

.inspection-detail-body.is-detail-expanded {
  grid-template-rows: minmax(0, 1fr);
}

.inspection-detail-body.is-detail-expanded .inspection-basic-panel {
  display: none;
}

.inspection-basic-panel {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  overflow: hidden;
  border: 1px solid #d9e2ee;
  border-radius: 8px;
  background: #ffffff;
}

.inspection-basic-item {
  display: grid;
  min-width: 0;
  grid-template-columns: 82px minmax(0, 1fr);
  gap: 8px;
  align-items: center;
  padding: 10px 12px;
  border-right: 1px solid #e8edf5;
  border-bottom: 1px solid #e8edf5;
}

.inspection-basic-item span {
  color: #64748b;
  font-size: 12px;
  white-space: nowrap;
}

.inspection-basic-item strong {
  min-width: 0;
  overflow: hidden;
  color: #1f2937;
  font-size: 13px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.inspection-list-panel {
  display: grid;
  min-height: 0;
  grid-template-rows: max-content minmax(0, 1fr);
  overflow: hidden;
  border: 1px solid #d9e2ee;
  border-radius: 8px;
  background: #ffffff;
}

.inspection-list-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 14px;
  border-bottom: 1px solid #e8edf5;
}

.inspection-list-head strong {
  color: #0f172a;
  font-size: 15px;
}

.inspection-list-head span {
  color: #64748b;
  font-size: 13px;
}

.inspection-list-host {
  min-height: 0;
  overflow: auto;
  padding: 10px;
  scrollbar-color: #94a3b8 #e2e8f0;
  scrollbar-width: thin;
}

.inspection-list-host::-webkit-scrollbar {
  width: 10px;
  height: 10px;
}

.inspection-list-host::-webkit-scrollbar-track {
  border-radius: 999px;
  background: #e2e8f0;
}

.inspection-list-host::-webkit-scrollbar-thumb {
  border: 2px solid #e2e8f0;
  border-radius: 999px;
  background: #94a3b8;
}

.inspection-list-zoom-surface {
  min-height: calc(100% / var(--inspection-detail-scale));
  transform: scale(var(--inspection-detail-scale));
  transform-origin: top left;
  width: calc(100% / var(--inspection-detail-scale));
}

.inspection-empty-host {
  display: grid;
  min-height: 320px;
  flex: 1;
  place-items: center;
}

.mono {
  font-family:
    ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono',
    monospace;
}

@media (max-width: 1024px) {
  .inspection-basic-panel {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .inspection-detail-header {
    align-items: flex-start;
    flex-direction: column;
    padding: 14px 16px 12px;
  }

  .inspection-toolbar {
    width: 100%;
    flex-wrap: wrap;
  }

  .inspection-basic-panel {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
