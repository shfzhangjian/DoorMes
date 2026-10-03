<script lang="ts" setup>
import type { MesFaiApi } from '#/api/mes/quality/fai';

import { computed, ref, watch } from 'vue';

import { IconifyIcon } from '@vben/icons';

import { Empty, Tag } from 'ant-design-vue';

import { parseEntryRuleParams } from '#/api/mes/quality/entry-rule';

defineOptions({ name: 'FaiEventDetailPanel' });

const props = defineProps<{
  onlyRecheck?: boolean;
  record?: MesFaiApi.FaiRecord | null;
}>();

const expandedItemKeys = ref<Set<string>>(new Set());

interface EntryGroup {
  items: MesFaiApi.FaiItem[];
  key: string;
  subtitle: string;
  title: string;
}

interface SampleColumn {
  key: string;
  label: string;
  source?: string;
  type: 'field' | 'group' | 'position' | 'remark' | 'result' | 'seq' | 'value';
}

interface DataRuleField {
  code: string;
  name: string;
  unit?: string;
}

interface DataRuleConfig {
  inputFields: DataRuleField[];
  resultFields: DataRuleField[];
}

const sortedItems = computed(() =>
  (props.record?.items || [])
    .filter((item) => !props.onlyRecheck || hasRecheckItemFlag(item))
    .toSorted((a, b) => {
      const leftSort = Number(a.sort ?? 0);
      const rightSort = Number(b.sort ?? 0);
      if (leftSort !== rightSort) return leftSort - rightSort;
      return groupKey(a).localeCompare(groupKey(b));
    }),
);

const groupedItems = computed<EntryGroup[]>(() => {
  const groupMap = new Map<string, EntryGroup>();
  sortedItems.value.forEach((item) => {
    const key = groupKey(item);
    if (!groupMap.has(key)) {
      groupMap.set(key, {
        items: [],
        key,
        subtitle: groupSubtitle(item),
        title: groupTitle(item),
      });
    }
    groupMap.get(key)!.items.push(item);
  });
  return [...groupMap.values()];
});

watch(
  () => props.record,
  () => {
    const firstItem = groupedItems.value[0]?.items[0];
    expandedItemKeys.value = new Set(firstItem ? [itemKey(firstItem)] : []);
  },
  { immediate: true },
);

function itemKey(item: MesFaiApi.FaiItem) {
  return String(
    item.id ??
      item.standardItemId ??
      [
        item.stepCode,
        item.sheetSectionCode,
        item.metricCode,
        item.inspectionItem,
      ].join('|'),
  );
}

function isItemExpanded(item: MesFaiApi.FaiItem) {
  return expandedItemKeys.value.has(itemKey(item));
}

function toggleItem(item: MesFaiApi.FaiItem) {
  const key = itemKey(item);
  const nextKeys = new Set(expandedItemKeys.value);
  if (nextKeys.has(key)) {
    nextKeys.delete(key);
  } else {
    nextKeys.add(key);
  }
  expandedItemKeys.value = nextKeys;
}

function groupKey(item: MesFaiApi.FaiItem) {
  if (isBasicInfoGroupItem(item)) return 'fai-inspection-items';
  return [
    item.stepCode || '-',
    item.sheetSectionCode || '-',
    item.stepName || '-',
    item.sheetSectionName || '-',
  ].join('|');
}

function isBasicInfoGroupItem(item: MesFaiApi.FaiItem) {
  const groupText = [
    item.stepName,
    item.sheetSectionName,
    item.stepCode,
    item.sheetSectionCode,
  ]
    .filter(Boolean)
    .join('|');
  return groupText.includes('基本信息') || groupText.includes('基础信息');
}

function groupTitle(item: MesFaiApi.FaiItem) {
  if (isBasicInfoGroupItem(item)) return '检验项目';
  return (
    item.stepName || item.sheetSectionName || item.stepCode || '未分组项目'
  );
}

function groupSubtitle(item: MesFaiApi.FaiItem) {
  if (isBasicInfoGroupItem(item)) return '';
  return [
    item.stepCode ? `步骤 ${item.stepCode}` : '',
    item.sheetSectionCode ? `区块 ${item.sheetSectionCode}` : '',
    item.sheetSectionName && item.sheetSectionName !== groupTitle(item)
      ? item.sheetSectionName
      : '',
  ]
    .filter(Boolean)
    .join(' / ');
}

function itemTitle(item: MesFaiApi.FaiItem) {
  return (
    item.sheetMetricName || item.inspectionItem || item.sheetMetricCode || '-'
  );
}

function itemCodeText(item: MesFaiApi.FaiItem) {
  return [
    item.metricGroupCode ? `组 ${item.metricGroupCode}` : '',
    item.metricCode ? `指标 ${item.metricCode}` : '',
    item.sheetMetricCode ? `表格指标 ${item.sheetMetricCode}` : '',
  ]
    .filter(Boolean)
    .join(' / ');
}

function typeLabel(item: MesFaiApi.FaiItem) {
  return item.itemType === 'QUALITATIVE' ? '定性' : '定量';
}

function valueTemplateLabel(item: MesFaiApi.FaiItem) {
  if (item.valueTemplateName) return item.valueTemplateName;
  if (item.valueTemplate === 'DENSITY_CALC') return '密度计算';
  if (item.valueTemplate === 'COMPRESSION_CALC') return '压缩性能';
  if (item.valueTemplate === 'SINGLE_VALUE') return '单值';
  return '常规';
}

function resultLabel(value?: string) {
  if (value === 'OK') return '合格';
  if (value === 'NG') return '不合格';
  if (value === 'PENDING' || value === '-') return '待判定';
  return value || '待判定';
}

function resultColor(value?: string) {
  if (value === 'OK') return 'success';
  if (value === 'NG') return 'error';
  return 'default';
}

function inputStatusLabel(status?: string) {
  if (status === 'COMPLETE') return '已完成';
  if (status === 'FILLING') return '填写中';
  if (status === 'ABNORMAL') return '异常';
  return '未填';
}

function inputStatusColor(status?: string) {
  if (status === 'COMPLETE') return 'success';
  if (status === 'FILLING') return 'processing';
  if (status === 'ABNORMAL') return 'error';
  return 'default';
}

function sampleRows(item: MesFaiApi.FaiItem) {
  const qaSamples = (item.samples || []).filter(
    (sample) => !sample.sampleRole || sample.sampleRole === 'QA',
  );
  const values =
    Array.isArray(item.qaValues) && item.qaValues.length > 0
      ? item.qaValues
      : qaSamples;
  const rows = values.map((sample, index) =>
    normalizeSampleRow(sample, item, index),
  );
  if (!props.onlyRecheck) return rows;
  const recheckRows = rows.filter((sample) => !!sample.recheckItemFlag);
  return recheckRows.length > 0 ? recheckRows : rows;
}

function normalizeSampleRow(
  sample: MesFaiApi.FaiSample | Record<string, unknown>,
  item: MesFaiApi.FaiItem,
  index: number,
) {
  const row =
    sample && typeof sample === 'object' ? { ...sample } : { value: sample };
  if ('rawValuesJson' in row && row.rawValuesJson) {
    try {
      Object.assign(row, JSON.parse(String(row.rawValuesJson)));
    } catch {
      // 原始字段仍可展示，避免异常 JSON 阻断详情查看。
    }
  }
  return {
    ...row,
    metricCode: row.metricCode ?? item.metricCode,
    metricGroupCode: row.metricGroupCode ?? item.metricGroupCode,
    sampleGroupNo: row.sampleGroupNo ?? index + 1,
    samplePosition: row.samplePosition ?? `${index + 1}`,
    sampleResult:
      row.sampleResult ??
      (item.itemType === 'QUALITATIVE'
        ? (row.qualitativeValue ?? row.value)
        : undefined) ??
      (item.qaResult === '-' ? 'PENDING' : item.qaResult),
    sampleSeq: row.sampleSeq ?? index + 1,
  };
}

function dataRule(item: MesFaiApi.FaiItem): DataRuleConfig | undefined {
  try {
    const params = parseEntryRuleParams(item.templateParams);
    const rule = params.dataRule || params;
    const inputFields = Array.isArray(rule.inputFields) ? rule.inputFields : [];
    const resultFields = Array.isArray(rule.resultFields)
      ? rule.resultFields
      : [];
    if (inputFields.length === 0 && resultFields.length === 0) return undefined;
    return {
      inputFields: inputFields.map((field: Record<string, unknown>) => ({
        code: String(field.code || ''),
        name: String(field.name || field.code || ''),
        unit: field.unit ? String(field.unit) : undefined,
      })),
      resultFields: resultFields.map((field: Record<string, unknown>) => ({
        code: String(field.code || ''),
        name: String(field.name || field.code || ''),
        unit: field.unit ? String(field.unit) : undefined,
      })),
    };
  } catch {
    return undefined;
  }
}

function sampleColumns(item: MesFaiApi.FaiItem): SampleColumn[] {
  const baseColumns: SampleColumn[] = [
    { key: 'seq', label: '序号', type: 'seq' },
    { key: 'position', label: '位置', type: 'position' },
    { key: 'group', label: '组别', type: 'group' },
  ];
  if (item.itemType === 'QUALITATIVE') {
    return [
      ...baseColumns,
      { key: 'qualitative', label: '判定值', type: 'value' },
      { key: 'result', label: '结果', type: 'result' },
      { key: 'remark', label: '备注', type: 'remark' },
    ];
  }

  const rule = dataRule(item);
  if (rule) {
    return [
      ...baseColumns,
      ...rule.inputFields.map((field) => ({
        key: `input-${field.code}`,
        label: labelWithUnit(field.name, field.unit),
        source: field.code,
        type: 'field' as const,
      })),
      ...rule.resultFields.map((field) => ({
        key: `result-${field.code}`,
        label: labelWithUnit(field.name, field.unit),
        source: field.code,
        type: 'field' as const,
      })),
      { key: 'result', label: '结果', type: 'result' },
      { key: 'remark', label: '备注', type: 'remark' },
    ];
  }

  if (item.valueTemplate === 'DENSITY_CALC') {
    return [
      ...baseColumns,
      {
        key: 'thicknessMm',
        label: '厚度/mm',
        source: 'thicknessMm',
        type: 'field',
      },
      { key: 'weightG', label: '重量/g', source: 'weightG', type: 'field' },
      {
        key: 'densityValue',
        label: '密度',
        source: 'densityValue',
        type: 'field',
      },
      { key: 'result', label: '结果', type: 'result' },
      { key: 'remark', label: '备注', type: 'remark' },
    ];
  }

  if (item.valueTemplate === 'COMPRESSION_CALC') {
    return [
      ...baseColumns,
      { key: 't1Mm', label: 'T1/mm', source: 't1Mm', type: 'field' },
      { key: 't2Mm', label: 'T2/mm', source: 't2Mm', type: 'field' },
      { key: 't3Mm', label: 'T3/mm', source: 't3Mm', type: 'field' },
      {
        key: 'compressionRate',
        label: '压缩率',
        source: 'compressionRate',
        type: 'field',
      },
      {
        key: 'compressionElasticityRate',
        label: '弹性率',
        source: 'compressionElasticityRate',
        type: 'field',
      },
      { key: 'result', label: '结果', type: 'result' },
      { key: 'remark', label: '备注', type: 'remark' },
    ];
  }

  return [
    ...baseColumns,
    {
      key: 'actualValue',
      label: labelWithUnit('实测值', item.unit),
      type: 'value',
    },
    { key: 'result', label: '结果', type: 'result' },
    { key: 'remark', label: '备注', type: 'remark' },
  ];
}

function sampleColumnValue(
  row: Record<string, unknown>,
  item: MesFaiApi.FaiItem,
  column: SampleColumn,
) {
  if (column.type === 'seq') return row.sampleSeq;
  if (column.type === 'position') return row.samplePosition || '-';
  if (column.type === 'group') return row.sampleGroupNo || '-';
  if (column.type === 'remark') return row.remark || row.defectName || '-';
  if (column.type === 'result')
    return resultLabel(String(row.sampleResult || ''));
  if (column.type === 'field') {
    return formatDisplayValue(row[column.source || '']);
  }
  return formatDisplayValue(resolveActualValue(row, item));
}

function resolveActualValue(
  row: Record<string, unknown>,
  item: MesFaiApi.FaiItem,
) {
  if (item.itemType === 'QUALITATIVE') {
    return row.value ?? row.qualitativeValue ?? row.sampleResult;
  }
  return (
    row.value ??
    row.resultValue ??
    row.measuredValue ??
    row.densityValue ??
    row.compressionRate ??
    row.compressionElasticityRate
  );
}

function labelWithUnit(label: string, unit?: string) {
  return unit ? `${label}/${unit}` : label;
}

function limitText(item: MesFaiApi.FaiItem) {
  const baseLimit = rangeText(
    item.minValueLimit,
    item.maxValueLimit,
    item.unit,
  );
  const avgLimit = rangeText(item.avgMinLimit, item.avgMaxLimit, item.unit);
  const stdLimit = rangeText(item.stdMinLimit, item.stdMaxLimit, item.unit);
  return [
    baseLimit ? `单值 ${baseLimit}` : '',
    avgLimit ? `平均值 ${avgLimit}` : '',
    stdLimit ? `标准差 ${stdLimit}` : '',
  ]
    .filter(Boolean)
    .join('；');
}

function statText(item: MesFaiApi.FaiItem) {
  const rows: string[] = [];
  appendStat(rows, '最小', item.calculatedMin ?? item.qaMin);
  appendStat(rows, '最大', item.calculatedMax ?? item.qaMax);
  appendStat(rows, '平均', item.calculatedAvg ?? item.qaAvg);
  appendStat(rows, '标准差', item.calculatedStd);
  return rows.join(' / ');
}

function appendStat(rows: string[], label: string, value?: number) {
  if (value === undefined) return;
  rows.push(`${label} ${formatDisplayValue(value)}`);
}

function rangeText(min?: number, max?: number, unit?: string) {
  if (min === undefined && max === undefined) return '';
  return `${formatDisplayValue(min)}~${formatDisplayValue(max)}${unit || ''}`;
}

function sampleCount(item: MesFaiApi.FaiItem) {
  return (
    item.requiredSampleCount ??
    item.cellRequiredCount ??
    item.sampleSize ??
    sampleRows(item).length
  );
}

function formatDisplayValue(value: unknown) {
  if (value === undefined || value === null || value === '') return '-';
  if (typeof value === 'number') {
    if (!Number.isFinite(value)) return '-';
    const fixedNumber = Number(value.toFixed(6));
    return Object.is(fixedNumber, -0) ? '0' : String(fixedNumber);
  }
  return String(value);
}

function hasRecheckItemFlag(item: MesFaiApi.FaiItem) {
  return (
    !!item.recheckItemFlag ||
    (item.samples || []).some((sample) => !!sample.recheckItemFlag) ||
    (item.qaValues || []).some((sample) => !!sample?.recheckItemFlag)
  );
}
</script>

<template>
  <div class="event-fai-panel">
    <Empty v-if="!record" class="py-14" description="源检验详情加载失败" />
    <Empty
      v-else-if="groupedItems.length === 0"
      class="py-14"
      :description="onlyRecheck ? '当前检验单暂无复检项' : '当前检验单暂无录入项'"
    />
    <div v-else class="event-fai-groups">
      <section
        v-for="group in groupedItems"
        :key="group.key"
        class="event-fai-group"
      >
        <header class="event-fai-group-head">
          <div class="event-fai-group-title">{{ group.title }}</div>
          <div v-if="group.subtitle" class="event-fai-group-subtitle">
            {{ group.subtitle }}
          </div>
        </header>

        <div class="event-fai-items">
          <article
            v-for="item in group.items"
            :key="itemKey(item)"
            class="event-fai-item"
          >
            <button
              :aria-expanded="isItemExpanded(item)"
              class="event-item-head"
              type="button"
              @click="toggleItem(item)"
            >
              <div class="event-item-title-block">
                <div class="event-item-title-row">
                  <span class="event-item-title">{{ itemTitle(item) }}</span>
                  <Tag color="blue" class="!m-0">{{ typeLabel(item) }}</Tag>
                  <Tag color="purple" class="!m-0">
                    {{ valueTemplateLabel(item) }}
                  </Tag>
                  <Tag :color="inputStatusColor(item.inputStatus)" class="!m-0">
                    {{ inputStatusLabel(item.inputStatus) }}
                  </Tag>
                  <Tag :color="resultColor(item.qaResult)" class="!m-0">
                    {{ resultLabel(item.qaResult) }}
                  </Tag>
                </div>
                <div v-if="itemCodeText(item)" class="event-item-code">
                  {{ itemCodeText(item) }}
                </div>
              </div>
              <div class="event-item-summary">
                <div class="event-item-progress">
                  样本数量：<strong>{{ sampleCount(item) }}</strong>
                </div>
                <IconifyIcon
                  :icon="
                    isItemExpanded(item)
                      ? 'lucide:chevron-up'
                      : 'lucide:chevron-down'
                  "
                  class="event-item-toggle"
                />
              </div>
            </button>

            <div v-show="isItemExpanded(item)" class="event-item-body">
              <div class="event-item-meta-grid">
                <div class="event-meta-cell">
                  <div class="event-meta-label">标准说明</div>
                  <div class="event-meta-value pre-wrap">
                    {{ item.standardDesc || '-' }}
                  </div>
                </div>
                <div class="event-meta-cell">
                  <div class="event-meta-label">控制限</div>
                  <div class="event-meta-value">
                    {{ limitText(item) || '-' }}
                  </div>
                </div>
                <div class="event-meta-cell">
                  <div class="event-meta-label">统计结果</div>
                  <div class="event-meta-value">
                    {{ statText(item) || '-' }}
                  </div>
                </div>
              </div>

              <div
                v-if="
                  item.ruleDescription || item.inspectionMethod || item.testTool
                "
                class="event-rule-note"
              >
                <span v-if="item.ruleDescription">
                  填写说明：{{ item.ruleDescription }}
                </span>
                <span v-if="item.inspectionMethod">
                  方法：{{ item.inspectionMethod }}
                </span>
                <span v-if="item.testTool">工具：{{ item.testTool }}</span>
              </div>

              <div class="event-sample-table-wrap">
                <table class="event-sample-table">
                  <thead>
                    <tr>
                      <th
                        v-for="column in sampleColumns(item)"
                        :key="column.key"
                      >
                        {{ column.label }}
                      </th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-if="sampleRows(item).length === 0">
                      <td
                        :colspan="sampleColumns(item).length"
                        class="empty-sample"
                      >
                        暂无样本数据
                      </td>
                    </tr>
                    <template v-else>
                      <tr
                        v-for="row in sampleRows(item)"
                        :key="`${item.id || item.inspectionItem}-${row.sampleSeq}-${row.samplePosition}`"
                      >
                        <td
                          v-for="column in sampleColumns(item)"
                          :key="column.key"
                          :class="{ 'result-cell': column.type === 'result' }"
                        >
                          <Tag
                            v-if="column.type === 'result'"
                            :color="resultColor(String(row.sampleResult || ''))"
                            class="!m-0"
                          >
                            {{ sampleColumnValue(row, item, column) }}
                          </Tag>
                          <span v-else>
                            {{ sampleColumnValue(row, item, column) }}
                          </span>
                        </td>
                      </tr>
                    </template>
                  </tbody>
                </table>
              </div>
            </div>
          </article>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.event-fai-panel {
  box-sizing: border-box;
  height: 100%;
  min-height: 0;
  overflow: auto;
  padding: 10px;
  scrollbar-color: #64748b #e2e8f0;
}

.event-fai-panel::-webkit-scrollbar,
.event-sample-table-wrap::-webkit-scrollbar {
  width: 12px;
  height: 12px;
}

.event-fai-panel::-webkit-scrollbar-track,
.event-sample-table-wrap::-webkit-scrollbar-track {
  background: #e2e8f0;
}

.event-fai-panel::-webkit-scrollbar-thumb,
.event-sample-table-wrap::-webkit-scrollbar-thumb {
  border: 2px solid #e2e8f0;
  background: #64748b;
  border-radius: 6px;
}

.event-fai-groups {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.event-fai-group {
  overflow: hidden;
  border: 1px solid #cbd5e1;
  background: #fff;
}

.event-fai-group-head {
  border-bottom: 1px solid #e2e8f0;
  background: #f8fafc;
  padding: 9px 12px;
}

.event-fai-group-title {
  color: #0f172a;
  font-size: 14px;
  font-weight: 800;
}

.event-fai-group-subtitle {
  margin-top: 3px;
  color: #64748b;
  font-size: 12px;
}

.event-fai-items {
  display: flex;
  flex-direction: column;
}

.event-fai-item {
  border-bottom: 1px solid #e2e8f0;
  padding: 12px;
}

.event-fai-item:last-child {
  border-bottom: 0;
}

.event-item-head {
  display: flex;
  width: 100%;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  border: 0;
  background: transparent;
  padding: 0;
  text-align: left;
  cursor: pointer;
}

.event-item-head:hover .event-item-title {
  color: #2563eb;
}

.event-item-title-block {
  min-width: 0;
}

.event-item-title-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.event-item-title {
  color: #0f172a;
  font-size: 14px;
  font-weight: 800;
}

.event-item-code,
.event-item-progress {
  color: #64748b;
  font-size: 12px;
}

.event-item-code {
  margin-top: 4px;
}

.event-item-progress {
  flex-shrink: 0;
  text-align: right;
}

.event-item-summary {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  gap: 8px;
}

.event-item-toggle {
  width: 16px;
  height: 16px;
  color: #64748b;
}

.event-item-progress strong {
  margin-left: 4px;
  color: #0f172a;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
}

.event-item-meta-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  margin-top: 10px;
}

.event-meta-cell {
  min-width: 0;
  background: #f8fafc;
  padding: 8px;
}

.event-meta-label {
  color: #64748b;
  font-size: 12px;
  font-weight: 800;
}

.event-meta-value {
  margin-top: 4px;
  color: #334155;
  font-size: 12px;
  line-height: 18px;
  overflow-wrap: anywhere;
}

.pre-wrap {
  white-space: pre-wrap;
}

.event-rule-note {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 10px;
  border: 1px solid #fde68a;
  background: #fffbeb;
  padding: 8px 10px;
  color: #92400e;
  font-size: 12px;
}

.event-sample-table-wrap {
  margin-top: 10px;
  overflow: auto;
  border: 1px solid #e2e8f0;
  scrollbar-color: #64748b #e2e8f0;
}

.event-sample-table {
  width: 100%;
  min-width: 860px;
  border-collapse: collapse;
  font-size: 12px;
}

.event-sample-table th {
  border-bottom: 1px solid #e2e8f0;
  background: #f8fafc;
  padding: 8px;
  color: #64748b;
  font-weight: 800;
  text-align: left;
}

.event-sample-table td {
  border-bottom: 1px solid #e2e8f0;
  padding: 8px;
  color: #334155;
}

.event-sample-table tr:last-child td {
  border-bottom: 0;
}

.result-cell {
  font-weight: 800;
}

.empty-sample {
  padding: 20px 8px !important;
  color: #94a3b8 !important;
  text-align: center;
}

@media (max-width: 960px) {
  .event-item-head,
  .event-item-meta-grid {
    grid-template-columns: 1fr;
  }

  .event-item-head {
    display: grid;
  }

  .event-item-progress {
    text-align: left;
  }
}
</style>
