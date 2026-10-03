<script lang="ts" setup>
import type { MesCutRoundFqcApi } from '#/api/mes/quality/cut-round-fqc';

import { computed, ref, watch } from 'vue';

import { Empty, Image, Table, Tag } from 'ant-design-vue';

import { getCutRoundFqcSubmissionDetailItems } from '#/api/mes/quality/cut-round-fqc';

defineOptions({ name: 'CutRoundFqcEventDetailPanel' });

const props = defineProps<{
  activeAbnormalFilter?: null | {
    inspectionItem: string;
    targetNo: string;
  };
  onlyNg?: boolean;
  onlyRecheck?: boolean;
  record?: MesCutRoundFqcApi.Record | null;
}>();

const emit = defineEmits<{
  clearAbnormalFilter: [];
}>();

const selectedDetailId = ref<number>();
const loadingDetailId = ref<number>();
const lazyItemsMap = ref<Record<number, MesCutRoundFqcApi.FqcItem[]>>({});

const itemColumns = [
  { dataIndex: 'inspectionItem', title: '检验项目', width: 190 },
  { align: 'center', dataIndex: 'itemType', title: '类型', width: 90 },
  { dataIndex: 'standardDesc', title: '标准', width: 260 },
  { align: 'right', dataIndex: 'sampleSize', title: '抽样数', width: 90 },
  { align: 'center', dataIndex: 'qaResult', title: '判定', width: 90 },
  { dataIndex: 'inputStatus', title: '录入状态', width: 110 },
  { dataIndex: 'defectText', title: '缺陷/异常', width: 190 },
];
const allDetailRows = computed(() => props.record?.submissionDetails || []);
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
const selectedPhotoUrls = computed(() => selectedDetail.value?.photoUrls || []);

const selectedItems = computed(() => {
  const detail = selectedDetail.value;
  if (!detail) {
    const items = props.record?.items || [];
    return filterVisibleItems(items);
  }
  const lazyItems = lazyItemsMap.value[detail.id];
  if (lazyItems) {
    return filterVisibleItems(lazyItems);
  }
  if (detail.items && detail.items.length > 0) {
    return filterVisibleItems(detail.items);
  }
  const items = (props.record?.items || []).filter(
    (item) =>
      item.submissionDetailId === detail.id ||
      item.cutRoundInspectionDetailId === detail.cutRoundInspectionDetailId ||
      item.productionBatchNo === detail.productionBatchNo,
  );
  return filterVisibleItems(items);
});

const visibleItems = computed(() => {
  const filter = props.activeAbnormalFilter;
  const items = selectedItems.value;
  if (!filter) return items;
  if (
    normalizeText(selectedDetail.value?.productionBatchNo) !==
    normalizeText(filter.targetNo)
  ) {
    return [];
  }
  return items.filter(
    (item) =>
      normalizeText(item.inspectionItem) ===
      normalizeText(filter.inspectionItem),
  );
});

const selectedDetailLoading = computed(
  () =>
    !!selectedDetail.value && loadingDetailId.value === selectedDetail.value.id,
);

watch(
  [detailRows, () => props.activeAbnormalFilter] as const,
  ([rows, filter]) => {
    if (rows.length === 0) {
      selectedDetailId.value = undefined;
      return;
    }
    const filteredDetail = filter
      ? rows.find(
          (row) =>
            normalizeText(row.productionBatchNo) ===
            normalizeText(filter.targetNo),
        )
      : undefined;
    if (filteredDetail) {
      selectedDetailId.value = filteredDetail.id;
      return;
    }
    if (!rows.some((row) => row.id === selectedDetailId.value)) {
      selectedDetailId.value = rows[0]?.id;
    }
  },
  { deep: true, immediate: true },
);

watch(
  () => props.record?.id,
  () => {
    lazyItemsMap.value = {};
    loadingDetailId.value = undefined;
  },
);

watch(
  selectedDetail,
  (detail) => {
    void loadSelectedDetailItems(detail);
  },
  { immediate: true },
);

function selectDetail(detail: MesCutRoundFqcApi.SubmissionDetail) {
  if (
    props.activeAbnormalFilter &&
    normalizeText(detail.productionBatchNo) !==
      normalizeText(props.activeAbnormalFilter.targetNo)
  ) {
    emit('clearAbnormalFilter');
  }
  selectedDetailId.value = detail.id;
}

function normalizeText(value?: string) {
  return (value || '').trim().toLocaleLowerCase();
}

async function loadSelectedDetailItems(
  detail?: MesCutRoundFqcApi.SubmissionDetail,
) {
  if (!props.record?.id || !detail?.id) return;
  if (lazyItemsMap.value[detail.id] || detail.items?.length) return;

  loadingDetailId.value = detail.id;
  try {
    const items = await getCutRoundFqcSubmissionDetailItems(
      props.record.id,
      detail.id,
    );
    lazyItemsMap.value = {
      ...lazyItemsMap.value,
      [detail.id]: items,
    };
  } finally {
    if (loadingDetailId.value === detail.id) {
      loadingDetailId.value = undefined;
    }
  }
}

function rowKey(row: MesCutRoundFqcApi.FqcItem) {
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

function limitText(item: MesCutRoundFqcApi.FqcItem) {
  if (item.minValueLimit === undefined && item.maxValueLimit === undefined) {
    return '-';
  }
  return `${displayValue(item.minValueLimit)}~${displayValue(item.maxValueLimit)}${item.unit || ''}`;
}

function sampleRows(item: MesCutRoundFqcApi.FqcItem) {
  if (item.qaValues?.length) {
    return item.qaValues.map((sample, index) => ({
      ...sample,
      sampleSeq: index + 1,
    }));
  }
  return (item.samples || []).map((sample, index) => ({
    ...sample,
    sampleSeq: sample.sampleSeq || index + 1,
    value:
      sample.qualitativeValue ||
      sample.sampleResult ||
      parseRawValue(sample.rawValuesJson),
  }));
}

function visibleSampleRows(item: MesCutRoundFqcApi.FqcItem) {
  const rows = sampleRows(item);
  if (!props.onlyRecheck) return rows;
  const recheckRows = rows.filter((sample) => !!sample.recheckItemFlag);
  return recheckRows.length > 0 ? recheckRows : rows;
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

function itemDefectText(item: MesCutRoundFqcApi.FqcItem) {
  const names = visibleSampleRows(item)
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
  defects?: MesCutRoundFqcApi.FqcSampleDefect[];
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

function hasRecheckItemFlag(item?: MesCutRoundFqcApi.FqcItem) {
  return (
    !!item?.recheckItemFlag ||
    (item?.samples || []).some((sample) => !!sample.recheckItemFlag) ||
    (item?.qaValues || []).some((sample) => !!sample?.recheckItemFlag)
  );
}

function filterRecheckItems(items: MesCutRoundFqcApi.FqcItem[]) {
  return items.filter((item) => hasRecheckItemFlag(item));
}

function filterVisibleItems(items: MesCutRoundFqcApi.FqcItem[]) {
  let rows = items;
  if (props.onlyRecheck) {
    rows = rows.filter((item) => hasRecheckItemFlag(item));
  }
  if (props.onlyNg) {
    rows = rows.filter((item) => hasNgItemFlag(item));
  }
  return rows;
}

function hasRecheckDetailFlag(detail: MesCutRoundFqcApi.SubmissionDetail) {
  if (detail.recheckDetailFlag) return true;
  if ((detail.items || []).some((item) => hasRecheckItemFlag(item))) {
    return true;
  }
  return (props.record?.items || []).some(
    (item) =>
      hasRecheckItemFlag(item) &&
      (item.submissionDetailId === detail.id ||
        item.cutRoundInspectionDetailId === detail.cutRoundInspectionDetailId ||
        item.productionBatchNo === detail.productionBatchNo),
  );
}

function hasNgDetailFlag(detail: MesCutRoundFqcApi.SubmissionDetail) {
  if (
    isNgResult(detail.rowJudgment) ||
    !!detail.defectCode ||
    !!detail.defectName ||
    !!detail.ngReason ||
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
        item.cutRoundInspectionDetailId === detail.cutRoundInspectionDetailId ||
        item.productionBatchNo === detail.productionBatchNo),
  );
}

function hasNgItemFlag(item?: MesCutRoundFqcApi.FqcItem) {
  if (!item) return false;
  if (isNgResult(item.qaResult || item.itemResult || item.inputStatus)) {
    return true;
  }
  return visibleSampleRows(item).some(
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
  <div class="type-detail-panel cut-round-event-panel">
    <Empty v-if="!record" class="py-14" description="源检验详情加载失败" />
    <Empty
      v-else-if="detailRows.length === 0 && selectedItems.length === 0"
      class="py-14"
      :description="onlyRecheck ? '暂无裁切成品复检项' : '暂无裁切成品检验项目'"
    />
    <div v-else class="type-detail-layout">
      <aside v-if="detailRows.length > 0" class="type-detail-side">
        <div class="side-title">送检片号</div>
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
              <span class="mono">{{ detail.productionBatchNo || '-' }}</span>
              <Tag
                :color="rowJudgmentTag(detail.rowJudgment).color"
                class="!m-0"
              >
                {{ rowJudgmentTag(detail.rowJudgment).label }}
              </Tag>
            </div>
            <div class="side-row-sub">
              母卷批号 {{ detail.parentProductionBatchNo || '-' }}
            </div>
          </button>
        </div>
      </aside>

      <section class="type-detail-main">
        <div v-if="selectedDetail" class="type-detail-selected-header">
          <div class="selected-title-block">
            <div class="selected-title">
              {{ selectedDetail.productionBatchNo || '-' }}
            </div>
            <div class="selected-subtitle">
              母卷批号 {{ selectedDetail.parentProductionBatchNo || '-' }} /
              物料 {{ selectedDetail.materialCode || '-' }} / 规格
              {{ selectedDetail.sizeRule || '-' }}
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
            <Tag v-if="activeAbnormalFilter" color="error">
              {{ activeAbnormalFilter.inspectionItem }}
            </Tag>
          </div>
        </div>
        <div v-if="selectedPhotoUrls.length > 0" class="selected-photo-strip">
          <div
            v-for="(url, index) in selectedPhotoUrls"
            :key="`${url}-${index}`"
            class="selected-photo-card"
          >
            <Image
              :src="url"
              :height="64"
              :width="88"
              class="selected-photo-img"
            />
            <span>照片 {{ index + 1 }}</span>
          </div>
        </div>

        <div class="detail-table-scroll">
          <Table
            bordered
            :columns="itemColumns"
            :data-source="visibleItems"
            :loading="selectedDetailLoading"
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
                    v-if="visibleSampleRows(item).length === 0"
                    class="sample-empty"
                  >
                    暂无样本
                  </div>
                  <template v-else>
                    <div
                      v-for="sample in visibleSampleRows(item)"
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

.type-detail-panel.qms-ncr-ng-cut-round-panel {
  height: 100%;
  max-height: 100%;
  min-height: 0;
}

.type-detail-layout {
  display: flex;
  gap: 8px;
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
  flex: 0 0 232px;
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
  padding: 10px 8px;
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
  gap: 6px;
  color: #0f172a;
  font-weight: 700;
}

.side-row-sub {
  margin-top: 4px;
  color: #64748b;
  font-size: 12px;
  line-height: 18px;
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

.selected-photo-strip {
  display: flex;
  flex-shrink: 0;
  gap: 8px;
  overflow-x: auto;
  border-bottom: 1px solid #e2e8f0;
  background: #f8fafc;
  padding: 8px 12px;
}

.selected-photo-card {
  display: flex;
  min-width: 112px;
  flex-direction: column;
  gap: 5px;
  border: 1px solid #e2e8f0;
  background: #fff;
  padding: 6px;
  color: #64748b;
  font-size: 12px;
}

.selected-photo-img {
  object-fit: cover;
}

.detail-table-scroll {
  flex: 1 1 auto;
  min-height: 0;
  overflow: auto;
  scrollbar-color: #64748b #e2e8f0;
}

.qms-ncr-ng-cut-round-panel .type-detail-layout,
.qms-ncr-ng-cut-round-panel .type-detail-main {
  height: 100%;
  min-height: 0;
}

.qms-ncr-ng-cut-round-panel .type-detail-side,
.qms-ncr-ng-cut-round-panel .detail-table-scroll {
  min-height: 0;
  overflow-x: auto;
  overflow-y: scroll;
}

.qms-ncr-ng-cut-round-panel .type-detail-side {
  height: 100%;
}

.qms-ncr-ng-cut-round-panel .detail-table-scroll {
  flex: 1 1 0%;
  height: auto;
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
