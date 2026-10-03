<script lang="ts" setup>
import type { MesFgShippingFqcApi } from '#/api/mes/quality/fg-shipping-fqc';

import { computed, ref, watch } from 'vue';

import { Empty, Table, Tag } from 'ant-design-vue';

defineOptions({ name: 'FgShippingFqcEventDetailPanel' });

const props = defineProps<{
  onlyNg?: boolean;
  onlyRecheck?: boolean;
  record?: MesFgShippingFqcApi.Record | null;
}>();

const selectedDetailId = ref<number>();

const itemColumns = [
  { dataIndex: 'inspectionItem', title: '检验项目', width: 190 },
  { align: 'center', dataIndex: 'itemType', title: '类型', width: 90 },
  { dataIndex: 'standardDesc', title: '标准', width: 260 },
  { align: 'right', dataIndex: 'sampleSize', title: '抽样数', width: 90 },
  { align: 'center', dataIndex: 'qaResult', title: '判定', width: 90 },
  { dataIndex: 'inputStatus', title: '录入状态', width: 110 },
  { dataIndex: 'defectText', title: '缺陷/异常', width: 190 },
];
const allDetailRows = computed(() => props.record?.shippingDetails || []);
const detailRows = computed(() => {
  let rows = allDetailRows.value;
  if (props.onlyRecheck) {
    rows = rows.filter((detail) => hasRecheckDetailFlag(detail));
  }
  if (props.onlyNg) {
    rows = rows.filter((detail) => hasNgDetailFlag(detail));
  }
  return rows;
});

const selectedDetail = computed(() => {
  if (detailRows.value.length === 0) return undefined;
  return (
    detailRows.value.find((item) => item.id === selectedDetailId.value) ||
    detailRows.value[0]
  );
});

const selectedItems = computed(() => {
  const detail = selectedDetail.value;
  if (!detail) {
    const items = props.record?.items || [];
    return filterVisibleItems(items);
  }
  if (detail.items && detail.items.length > 0) {
    return filterVisibleItems(detail.items);
  }
  const items = (props.record?.items || []).filter(
    (item) =>
      item.submissionDetailId === detail.id ||
      item.cutRoundInspectionDetailId === detail.shippingNoticeItemId ||
      item.productionBatchNo === detail.actualSliceBatchNo ||
      item.productionBatchNo === detail.sliceBatchNo,
  );
  return filterVisibleItems(items);
});

watch(
  detailRows,
  (rows) => {
    if (rows.length === 0) {
      selectedDetailId.value = undefined;
      return;
    }
    if (!rows.some((row) => row.id === selectedDetailId.value)) {
      selectedDetailId.value = rows[0]?.id;
    }
  },
  { immediate: true },
);

function selectDetail(detail: MesFgShippingFqcApi.ShippingDetail) {
  selectedDetailId.value = detail.id;
}

function rowKey(row: MesFgShippingFqcApi.FqcItem) {
  return (
    row.id ||
    `${row.submissionDetailId || row.cutRoundInspectionDetailId || '-'}-${row.inspectionItem}`
  );
}

function itemTypeLabel(value?: string) {
  if (value === 'QUALITATIVE') return '定性';
  if (value === 'QUANTITATIVE') return '定量';
  return value || '-';
}

function resultLabel(value?: string) {
  if (value === 'OK') return '合格';
  if (value === 'NG') return '不合格';
  if (value === 'PENDING') return '待判定';
  return value || '-';
}

function resultColor(value?: string) {
  if (value === 'OK') return 'success';
  if (value === 'NG') return 'error';
  if (value === 'PENDING') return 'warning';
  return 'default';
}

function inputStatusLabel(value?: string) {
  if (value === 'COMPLETE') return '已完成';
  if (value === 'FILLING') return '填写中';
  if (value === 'ABNORMAL') return '异常';
  if (value === 'EMPTY') return '未填';
  return value || '-';
}

function rowJudgmentTag(value?: string) {
  if (value === 'OK') return { color: 'success', label: '合格' };
  if (value === 'NG') return { color: 'error', label: '不合格' };
  return { color: 'warning', label: '待判定' };
}

function alignmentTag(value?: string) {
  if (value === 'ALIGNED') return { color: 'success', label: '已对齐' };
  if (value === 'MISMATCH') return { color: 'error', label: '不一致' };
  return { color: 'warning', label: '待对齐' };
}

function detailSliceModelText(detail: MesFgShippingFqcApi.ShippingDetail) {
  return (
    [
      detail.actualSliceBatchNo ? `实际片号 ${detail.actualSliceBatchNo}` : '',
      detail.sliceBatchNo ? `计划片号 ${detail.sliceBatchNo}` : '',
      detail.customerProductBatchNo
        ? `客户批号 ${detail.customerProductBatchNo}`
        : '',
    ]
      .filter(Boolean)
      .join(' / ') || '-'
  );
}

function limitText(item: MesFgShippingFqcApi.FqcItem) {
  if (item.minValueLimit === undefined && item.maxValueLimit === undefined) {
    return '-';
  }
  return `${displayValue(item.minValueLimit)}~${displayValue(item.maxValueLimit)}${item.unit || ''}`;
}

function sampleRows(item: MesFgShippingFqcApi.FqcItem) {
  if (item.qaValues?.length) {
    const rows = item.qaValues.map((sample, index) => ({
      ...sample,
      sampleSeq: index + 1,
    }));
    return filterSampleRows(rows);
  }
  const rows = (item.samples || []).map((sample, index) => ({
    ...sample,
    sampleSeq: sample.sampleSeq || index + 1,
    value:
      sample.qualitativeValue ||
      sample.sampleResult ||
      parseRawValue(sample.rawValuesJson),
  }));
  return filterSampleRows(rows);
}

function parseRawValue(raw?: string) {
  if (!raw) return undefined;
  try {
    const parsed = JSON.parse(raw);
    if (parsed && typeof parsed === 'object') {
      return Object.entries(parsed)
        .map(([key, value]) => `${key}:${String(value)}`)
        .join('，');
    }
    return String(parsed);
  } catch {
    return raw;
  }
}

function itemDefectText(item: MesFgShippingFqcApi.FqcItem) {
  const names = sampleRows(item)
    .flatMap((sample) => sample.defects || [])
    .map((defect) => defect.defectName || defect.defectCode)
    .filter(Boolean);
  return [...new Set(names)].join('；') || '-';
}

function sampleResultText(sample: {
  qualitativeValue?: string;
  sampleResult?: string;
  value?: string;
}) {
  return sample.value || sample.qualitativeValue || sample.sampleResult || '-';
}

function sampleDefectText(sample: {
  defects?: MesFgShippingFqcApi.FqcSampleDefect[];
}) {
  return (
    (sample.defects || [])
      .map((defect) => defect.defectName || defect.defectCode)
      .filter(Boolean)
      .join('；') || '-'
  );
}

function displayValue(value?: unknown) {
  if (value === undefined || value === null || value === '') return '-';
  return String(value);
}

function hasRecheckItemFlag(item?: MesFgShippingFqcApi.FqcItem) {
  return (
    !!item?.recheckItemFlag ||
    (item?.samples || []).some((sample) => !!sample.recheckItemFlag) ||
    (item?.qaValues || []).some((sample) => !!sample?.recheckItemFlag)
  );
}

function filterRecheckItems(items: MesFgShippingFqcApi.FqcItem[]) {
  return items.filter((item) => hasRecheckItemFlag(item));
}

function filterVisibleItems(items: MesFgShippingFqcApi.FqcItem[]) {
  let rows = items;
  if (props.onlyRecheck) {
    rows = rows.filter((item) => hasRecheckItemFlag(item));
  }
  if (props.onlyNg) {
    rows = rows.filter((item) => hasNgItemFlag(item));
  }
  return rows;
}

function filterSampleRows<T extends { recheckItemFlag?: boolean }>(rows: T[]) {
  if (!props.onlyRecheck) return rows;
  const recheckRows = rows.filter((sample) => !!sample.recheckItemFlag);
  return recheckRows.length > 0 ? recheckRows : rows;
}

function hasRecheckDetailFlag(detail: MesFgShippingFqcApi.ShippingDetail) {
  if (detail.recheckDetailFlag) return true;
  if ((detail.items || []).some((item) => hasRecheckItemFlag(item))) {
    return true;
  }
  return (props.record?.items || []).some(
    (item) =>
      hasRecheckItemFlag(item) &&
      (item.submissionDetailId === detail.id ||
        item.cutRoundInspectionDetailId === detail.shippingNoticeItemId ||
        item.productionBatchNo === detail.actualSliceBatchNo ||
        item.productionBatchNo === detail.sliceBatchNo),
  );
}

function hasNgDetailFlag(detail: MesFgShippingFqcApi.ShippingDetail) {
  if (
    isNgResult(detail.rowJudgment) ||
    !!detail.defectCode ||
    !!detail.defectName ||
    Number(detail.abnormalItemCount || 0) > 0
  ) {
    return true;
  }
  if ((detail.items || []).some((item) => hasNgItemFlag(item))) {
    return true;
  }
  return (props.record?.items || []).some(
    (item) =>
      hasNgItemFlag(item) &&
      (item.submissionDetailId === detail.id ||
        item.cutRoundInspectionDetailId === detail.shippingNoticeItemId ||
        item.productionBatchNo === detail.actualSliceBatchNo ||
        item.productionBatchNo === detail.sliceBatchNo),
  );
}

function hasNgItemFlag(item?: MesFgShippingFqcApi.FqcItem) {
  if (!item) return false;
  if (isNgResult(item.qaResult || item.itemResult || item.inputStatus)) {
    return true;
  }
  return sampleRows(item).some(
    (sample) =>
      isNgResult(sample.sampleResult || sample.qualitativeValue) ||
      (sample.defects || []).length > 0,
  );
}

function isNgResult(value?: string) {
  const normalized = (value || '').trim().toUpperCase();
  return (
    normalized === 'NG' ||
    normalized === 'N' ||
    normalized === 'ABNORMAL' ||
    normalized === 'FAIL' ||
    normalized === 'FAILED' ||
    normalized.includes('不合格') ||
    normalized.includes('异常')
  );
}
</script>

<template>
  <div class="type-detail-panel fg-shipping-event-panel">
    <Empty v-if="!record" class="py-14" description="源检验详情加载失败" />
    <Empty
      v-else-if="detailRows.length === 0 && selectedItems.length === 0"
      class="py-14"
      :description="onlyRecheck ? '暂无发货成品复检项' : '暂无发货成品检验项目'"
    />
    <div v-else class="type-detail-layout">
      <aside v-if="detailRows.length > 0" class="type-detail-side">
        <div class="side-title">实际片号检验</div>
        <div class="side-content">
          <button
            v-for="detail in detailRows"
            :key="detail.id"
            class="side-row"
            :class="{ active: detail.id === selectedDetail?.id }"
            type="button"
            @click="selectDetail(detail)"
          >
            <div class="side-row-main">
              <span class="mono">{{
                detail.actualSliceBatchNo || detail.sliceBatchNo || '-'
              }}</span>
              <Tag
                :color="rowJudgmentTag(detail.rowJudgment).color"
                class="!m-0"
              >
                {{ rowJudgmentTag(detail.rowJudgment).label }}
              </Tag>
            </div>
            <div class="side-row-sub">
              客户批号 {{ detail.customerProductBatchNo || '-' }}
            </div>
            <div class="side-row-sub">
              对齐 {{ alignmentTag(detail.alignmentStatus).label }} / 项目
              {{ detail.completedItemCount || 0 }}/{{
                detail.requiredItemCount || 0
              }}
            </div>
          </button>
        </div>
      </aside>

      <section class="type-detail-main">
        <div v-if="selectedDetail" class="type-detail-selected-header">
          <div class="selected-title-block">
            <div class="selected-title">
              {{ detailSliceModelText(selectedDetail) }}
            </div>
            <div class="selected-subtitle">
              客户
              {{ selectedDetail.customerName || record.customerName || '-' }} /
              ERP
              {{ selectedDetail.erpOrderNo || record.erpOrderNo || '-' }}
            </div>
            <div class="selected-subtitle">
              检验
              {{
                selectedDetail.inspectorName || record.qaInspectorName || '-'
              }}
              /
              {{
                selectedDetail.inspectionTime ||
                record.qaTime ||
                record.submissionTime ||
                '-'
              }}
            </div>
          </div>
          <div class="selected-tags">
            <Tag :color="rowJudgmentTag(selectedDetail.rowJudgment).color">
              {{ rowJudgmentTag(selectedDetail.rowJudgment).label }}
            </Tag>
            <Tag :color="alignmentTag(selectedDetail.alignmentStatus).color">
              {{ alignmentTag(selectedDetail.alignmentStatus).label }}
            </Tag>
            <Tag color="blue">
              项目 {{ selectedDetail.completedItemCount || 0 }}/{{
                selectedDetail.requiredItemCount || 0
              }}
            </Tag>
          </div>
        </div>

        <div class="detail-table-scroll">
          <Table
            bordered
            :columns="itemColumns"
            :data-source="selectedItems"
            :pagination="false"
            :row-key="rowKey"
            size="small"
            class="detail-item-table"
          >
            <template #bodyCell="{ column, record: item }">
              <template v-if="column.dataIndex === 'itemType'">
                {{ itemTypeLabel(item.itemType) }}
              </template>
              <template v-else-if="column.dataIndex === 'qaResult'">
                <Tag
                  :color="resultColor(item.qaResult || item.itemResult)"
                  class="!m-0"
                >
                  {{ resultLabel(item.qaResult || item.itemResult) }}
                </Tag>
              </template>
              <template v-else-if="column.dataIndex === 'inputStatus'">
                {{ inputStatusLabel(item.inputStatus) }}
              </template>
              <template v-else-if="column.dataIndex === 'defectText'">
                <span class="danger-text">{{ itemDefectText(item) }}</span>
              </template>
              <template v-else-if="column.dataIndex === 'standardDesc'">
                <span class="pre-wrap">{{ item.standardDesc || '-' }}</span>
              </template>
            </template>

            <template #expandedRowRender="{ record: item }">
              <div class="expanded-detail">
                <div class="expanded-grid">
                  <div>
                    <strong>检验标准</strong>
                    <span>{{ item.standardDesc || '-' }}</span>
                  </div>
                  <div>
                    <strong>控制限</strong>
                    <span>{{ limitText(item) }}</span>
                  </div>
                  <div>
                    <strong>方法/频次</strong>
                    <span>
                      {{ item.inspectionMethod || '-' }}
                      <template v-if="item.testFrequencyJudgement">
                        / {{ item.testFrequencyJudgement }}
                      </template>
                    </span>
                  </div>
                  <div>
                    <strong>填写规则</strong>
                    <span>{{ item.ruleDescription || '-' }}</span>
                  </div>
                </div>

                <div class="sample-strip">
                  <div
                    v-if="sampleRows(item).length === 0"
                    class="sample-empty"
                  >
                    暂无样本
                  </div>
                  <template v-else>
                    <div
                      v-for="sample in sampleRows(item)"
                      :key="`${rowKey(item)}-${sample.sampleSeq}`"
                      class="sample-pill"
                    >
                      <span class="mono">#{{ sample.sampleSeq }}</span>
                      <Tag
                        :color="
                          resultColor(
                            sample.sampleResult || sample.qualitativeValue,
                          )
                        "
                        class="!m-0"
                      >
                        {{
                          resultLabel(
                            sample.sampleResult || sample.qualitativeValue,
                          )
                        }}
                      </Tag>
                      <span>{{ sampleResultText(sample) }}</span>
                      <span
                        v-if="sampleDefectText(sample) !== '-'"
                        class="danger-text"
                      >
                        {{ sampleDefectText(sample) }}
                      </span>
                      <span v-if="sample.remark">{{ sample.remark }}</span>
                    </div>
                  </template>
                </div>
              </div>
            </template>
          </Table>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.type-detail-panel {
  box-sizing: border-box;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.type-detail-layout {
  display: flex;
  gap: 10px;
  height: 100%;
  min-height: 0;
  padding: 8px;
}

.type-detail-side,
.type-detail-main {
  min-height: 0;
  border: 1px solid #cbd5e1;
  background: #fff;
}

.type-detail-side {
  flex: 0 0 300px;
  overflow: auto;
  scrollbar-color: #64748b #e2e8f0;
}

.type-detail-side::-webkit-scrollbar,
.detail-table-scroll::-webkit-scrollbar {
  width: 12px;
  height: 12px;
}

.type-detail-side::-webkit-scrollbar-track,
.detail-table-scroll::-webkit-scrollbar-track {
  background: #e2e8f0;
}

.type-detail-side::-webkit-scrollbar-thumb,
.detail-table-scroll::-webkit-scrollbar-thumb {
  border: 2px solid #e2e8f0;
  background: #64748b;
  border-radius: 6px;
}

.side-content {
  min-width: 0;
}

.side-title {
  position: sticky;
  top: 0;
  z-index: 1;
  border-bottom: 1px solid #e2e8f0;
  background: #f8fafc;
  padding: 9px 10px;
  color: #0f172a;
  font-weight: 800;
}

.side-row {
  display: block;
  width: 100%;
  border: 0;
  border-bottom: 1px solid #e2e8f0;
  background: #fff;
  padding: 10px;
  text-align: left;
  cursor: pointer;
}

.side-row:hover,
.side-row.active {
  background: #eff6ff;
}

.side-row-main {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  color: #0f172a;
  font-weight: 700;
}

.side-row-sub {
  margin-top: 4px;
  color: #64748b;
  font-size: 12px;
}

.type-detail-main {
  display: flex;
  flex: 1 1 auto;
  min-width: 0;
  flex-direction: column;
  overflow: hidden;
}

.type-detail-selected-header {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border-bottom: 1px solid #e2e8f0;
  background: #fff;
  padding: 9px 12px;
}

.selected-title-block {
  min-width: 0;
}

.selected-title {
  color: #0f172a;
  font-size: 14px;
  font-weight: 800;
}

.selected-subtitle {
  margin-top: 3px;
  color: #64748b;
  font-size: 12px;
}

.selected-tags {
  display: flex;
  flex-shrink: 0;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: flex-end;
}

.detail-table-scroll {
  flex: 1 1 auto;
  min-height: 0;
  overflow: auto;
  scrollbar-color: #64748b #e2e8f0;
}

.detail-item-table {
  min-width: 1080px;
}

.detail-item-table :deep(.ant-table-thead > tr > th) {
  position: sticky;
  z-index: 2;
  top: 0;
}

.expanded-detail {
  background: #f8fafc;
  padding: 10px 12px;
}

.expanded-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
}

.expanded-grid div {
  min-width: 0;
  background: #fff;
  padding: 8px;
}

.expanded-grid strong {
  display: block;
  color: #64748b;
  font-size: 12px;
}

.expanded-grid span {
  display: block;
  margin-top: 4px;
  color: #334155;
  font-size: 12px;
  line-height: 18px;
  overflow-wrap: anywhere;
}

.sample-strip {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 8px;
}

.sample-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border: 1px solid #e2e8f0;
  background: #fff;
  padding: 5px 8px;
  color: #334155;
  font-size: 12px;
}

.sample-empty {
  color: #94a3b8;
  font-size: 12px;
}

.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
}

.danger-text {
  color: #b91c1c;
}

.pre-wrap {
  white-space: pre-wrap;
}

@media (max-width: 1180px) {
  .type-detail-layout {
    flex-direction: column;
  }

  .type-detail-side {
    flex: 0 1 220px;
    width: 100%;
    max-height: 220px;
  }

  .expanded-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
