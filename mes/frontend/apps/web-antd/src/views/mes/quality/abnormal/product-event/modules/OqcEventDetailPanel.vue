<script lang="ts" setup>
import type { MesOqcApi } from '#/api/mes/quality/oqc';

import { computed } from 'vue';

import { Empty, Table, Tag } from 'ant-design-vue';

defineOptions({ name: 'OqcEventDetailPanel' });

const props = defineProps<{
  onlyRecheck?: boolean;
  record?: MesOqcApi.OqcRecord | null;
}>();

const columns = [
  { align: 'center', dataIndex: 'category', title: '类别', width: 130 },
  { align: 'center', dataIndex: 'sort', title: '序号', width: 70 },
  { dataIndex: 'inspectionItem', title: '检验项目', width: 190 },
  { dataIndex: 'standardDesc', title: '检验内容', width: 320 },
  { align: 'center', dataIndex: 'yes', title: '是/符合', width: 80 },
  { align: 'center', dataIndex: 'no', title: '否/不符合', width: 90 },
  { dataIndex: 'remark', title: '备注/异常说明', width: 220 },
];
const items = computed(() => {
  const rows = props.record?.items || [];
  if (!props.onlyRecheck) return rows;
  return rows.filter((item) => hasRecheckItemFlag(item));
});

function rowKey(item: MesOqcApi.OqcItem) {
  return (
    item.id ||
    `${item.category || '-'}-${item.sort || '-'}-${item.inspectionItem}`
  );
}

function getChecklistResult(item: MesOqcApi.OqcItem) {
  const sample = item.sampleValues?.[0];
  const sampleResult =
    sample && typeof sample === 'object'
      ? (sample.value ?? sample.qualitativeValue ?? sample.sampleResult)
      : sample;
  return item.itemResult === '-' ? sampleResult : item.itemResult;
}

function getChecklistRemark(item: MesOqcApi.OqcItem) {
  const sample = item.sampleValues?.[0];
  if (sample && typeof sample === 'object') return sample.remark;
  return undefined;
}

function categoryLabel(category?: string) {
  if (category === 'COA') return '随货 COA';
  if (category === 'LABEL') return '包装盒及产品标签';
  if (category === 'PACKING') return '打包';
  if (category === 'OTHER') return '其他检查项';
  if (category === 'PRODUCT') return '产品检查项';
  return category || '检查项';
}

function categoryColor(category?: string) {
  if (category === 'COA') return 'blue';
  if (category === 'LABEL') return 'cyan';
  if (category === 'PACKING') return 'orange';
  if (category === 'OTHER') return 'purple';
  if (category === 'PRODUCT') return 'geekblue';
  return 'default';
}

function checklistNo(item: MesOqcApi.OqcItem) {
  return item.sort ? Math.round(item.sort / 10) : '-';
}

function resultColor(value?: string) {
  if (value === 'OK') return 'success';
  if (value === 'NG') return 'error';
  if (value === 'NA') return 'default';
  if (value === 'PENDING') return 'warning';
  return 'default';
}

function resultLabel(value?: string) {
  if (value === 'OK') return '合格';
  if (value === 'NG') return '不合格';
  if (value === 'NA') return '不适用';
  if (value === 'PENDING' || value === '-') return '待判定';
  return value || '-';
}

function limitText(item: MesOqcApi.OqcItem) {
  const rows = [
    rangeText('单值', item.minValueLimit, item.maxValueLimit, item.unit),
    rangeText('平均值', item.avgMinLimit, item.avgMaxLimit, item.unit),
    rangeText('标准差', item.stdMinLimit, item.stdMaxLimit, item.unit),
  ].filter(Boolean);
  return rows.join('；') || '-';
}

function rangeText(label: string, min?: number, max?: number, unit?: string) {
  if (min === undefined && max === undefined) return '';
  return `${label} ${displayValue(min)}~${displayValue(max)}${unit || ''}`;
}

function sampleRows(item: MesOqcApi.OqcItem) {
  const fromSamples = (item.samples || []).map((sample) => ({
    measuredValue:
      sample.measuredValue ??
      sample.resultValue ??
      sample.qualitativeValue ??
      parseRawValue(sample.rawValuesJson),
    recheckItemFlag: sample.recheckItemFlag,
    remark: sample.remark,
    sampleResult: sample.sampleResult || sample.qualitativeValue,
    sampleSeq: sample.sampleSeq,
  }));
  const fromValues = (item.sampleValues || []).map((sample, index) => {
    if (sample && typeof sample === 'object') {
      return {
        measuredValue:
          sample.value ??
          sample.measuredValue ??
          sample.resultValue ??
          sample.qualitativeValue ??
          sample.sampleResult,
        recheckItemFlag: sample.recheckItemFlag,
        remark: sample.remark,
        sampleResult:
          sample.sampleResult ?? sample.qualitativeValue ?? sample.value,
        sampleSeq: sample.sampleSeq || index + 1,
      };
    }
    return {
      measuredValue: sample,
      sampleResult: sample,
      sampleSeq: index + 1,
    };
  });
  const rows = fromSamples.length > 0 ? fromSamples : fromValues;
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

function displayValue(value?: unknown) {
  if (value === undefined || value === null || value === '') return '-';
  return String(value);
}

function hasRecheckItemFlag(item: MesOqcApi.OqcItem) {
  return (
    !!item.recheckItemFlag ||
    (item.samples || []).some((sample) => !!sample.recheckItemFlag) ||
    (item.sampleValues || []).some((sample) => !!sample?.recheckItemFlag)
  );
}
</script>

<template>
  <div class="oqc-event-panel">
    <Empty v-if="!record" class="py-14" description="源检验详情加载失败" />
    <Empty
      v-else-if="items.length === 0"
      class="py-14"
      :description="onlyRecheck ? '暂无 OQC 复检项' : '暂无 OQC 检验项目'"
    />
    <div v-else class="oqc-table-scroll">
      <Table
        bordered
        :columns="columns"
        :data-source="items"
        :pagination="false"
        :row-key="rowKey"
        size="small"
        class="oqc-detail-table"
      >
        <template #bodyCell="{ column, record: item }">
          <template v-if="column.dataIndex === 'category'">
            <Tag :color="categoryColor(item.category)" class="!m-0">
              {{ categoryLabel(item.category) }}
            </Tag>
          </template>
          <template v-else-if="column.dataIndex === 'sort'">
            <span class="mono">{{ checklistNo(item) }}</span>
          </template>
          <template v-else-if="column.dataIndex === 'yes'">
            <span class="ok-mark">{{
              getChecklistResult(item) === 'OK' ? '√' : ''
            }}</span>
          </template>
          <template v-else-if="column.dataIndex === 'no'">
            <span class="ng-mark">{{
              getChecklistResult(item) === 'NG' ? '√' : ''
            }}</span>
          </template>
          <template v-else-if="column.dataIndex === 'remark'">
            <span class="danger-text">{{
              getChecklistRemark(item) || '-'
            }}</span>
          </template>
          <template v-else-if="column.dataIndex === 'standardDesc'">
            <span class="pre-wrap">{{ item.standardDesc || '-' }}</span>
          </template>
        </template>

        <template #expandedRowRender="{ record: item }">
          <div class="expanded-detail">
            <div class="expanded-grid">
              <div>
                <strong>项目结果</strong>
                <Tag
                  :color="resultColor(getChecklistResult(item))"
                  class="!mt-1"
                >
                  {{ resultLabel(getChecklistResult(item)) }}
                </Tag>
              </div>
              <div>
                <strong>控制限</strong>
                <span>{{ limitText(item) }}</span>
              </div>
              <div>
                <strong>检验方法/工具</strong>
                <span>{{ item.inspectionMethod || item.testTool || '-' }}</span>
              </div>
              <div>
                <strong>填写规则</strong>
                <span>{{ item.ruleDescription || '-' }}</span>
              </div>
            </div>

            <div class="sample-strip">
              <div v-if="sampleRows(item).length === 0" class="sample-empty">
                暂无样本
              </div>
              <template v-else>
                <div
                  v-for="sample in sampleRows(item)"
                  :key="`${rowKey(item)}-${sample.sampleSeq}`"
                  class="sample-pill"
                >
                  <span class="mono">#{{ sample.sampleSeq }}</span>
                  <Tag :color="resultColor(sample.sampleResult)" class="!m-0">
                    {{ resultLabel(sample.sampleResult) }}
                  </Tag>
                  <span>{{ displayValue(sample.measuredValue) }}</span>
                  <span v-if="sample.remark" class="danger-text">{{
                    sample.remark
                  }}</span>
                </div>
              </template>
            </div>
          </div>
        </template>
      </Table>
    </div>
  </div>
</template>

<style scoped>
.oqc-event-panel {
  display: flex;
  box-sizing: border-box;
  height: 100%;
  min-height: 0;
  flex-direction: column;
  overflow: hidden;
  padding: 8px;
}

.oqc-table-scroll {
  flex: 1 1 auto;
  min-height: 0;
  overflow: auto;
  scrollbar-color: #64748b #e2e8f0;
}

.oqc-table-scroll::-webkit-scrollbar {
  width: 12px;
  height: 12px;
}

.oqc-table-scroll::-webkit-scrollbar-track {
  background: #e2e8f0;
}

.oqc-table-scroll::-webkit-scrollbar-thumb {
  border: 2px solid #e2e8f0;
  background: #64748b;
  border-radius: 6px;
}

.oqc-detail-table {
  min-width: 1160px;
}

.oqc-detail-table :deep(.ant-table-thead > tr > th) {
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
  font-weight: 700;
}

.ok-mark {
  color: #15803d;
  font-weight: 800;
}

.ng-mark,
.danger-text {
  color: #b91c1c;
}

.pre-wrap {
  white-space: pre-wrap;
}

@media (max-width: 1180px) {
  .expanded-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
