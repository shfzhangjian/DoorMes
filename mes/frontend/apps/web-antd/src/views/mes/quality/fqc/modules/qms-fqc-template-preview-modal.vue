<script lang="ts" setup>
import type { MesFqcApi } from '#/api/mes/quality/fqc';

import { computed } from 'vue';

import { Empty, Modal, Tag } from 'ant-design-vue';

import { parseEntryRuleParams } from '#/api/mes/quality/entry-rule';

import { formatReportQty } from '../data';

defineOptions({ name: 'QmsFqcTemplatePreviewModal' });

const props = defineProps<{
  items: MesFqcApi.FqcItem[];
  open: boolean;
  record?: MesFqcApi.FqcRecord | null;
}>();

const emit = defineEmits<{
  'update:open': [open: boolean];
}>();

interface DataRuleField {
  code: string;
  name: string;
  unit?: string;
}

interface DataRuleConfig {
  inputFields: DataRuleField[];
  resultFields: DataRuleField[];
}

interface EntryGroup {
  items: MesFqcApi.FqcItem[];
  key: string;
  subtitle: string;
  title: string;
}

interface SampleColumn {
  key: string;
  label: string;
  source?: string;
  type:
    | 'defect'
    | 'field'
    | 'group'
    | 'position'
    | 'remark'
    | 'result'
    | 'seq'
    | 'value';
}

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

const sortedItems = computed(() =>
  [...(props.items || [])].toSorted((a, b) => {
    const leftSort = Number(a.sort ?? 0);
    const rightSort = Number(b.sort ?? 0);
    if (leftSort !== rightSort) return leftSort - rightSort;
    return groupKey(a).localeCompare(groupKey(b));
  }),
);

function closeModal() {
  emit('update:open', false);
}

function groupKey(item: MesFqcApi.FqcItem) {
  return [
    item.stepCode || '-',
    item.sheetSectionCode || '-',
    item.stepName || '-',
    item.sheetSectionName || '-',
  ].join('|');
}

function groupTitle(item: MesFqcApi.FqcItem) {
  return (
    item.stepName || item.sheetSectionName || item.stepCode || '未分组项目'
  );
}

function groupSubtitle(item: MesFqcApi.FqcItem) {
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

function itemTitle(item: MesFqcApi.FqcItem) {
  return (
    item.sheetMetricName || item.inspectionItem || item.sheetMetricCode || '-'
  );
}

function itemCodeText(item: MesFqcApi.FqcItem) {
  return [
    item.metricGroupCode ? `组 ${item.metricGroupCode}` : '',
    item.metricCode ? `指标 ${item.metricCode}` : '',
    item.sheetMetricCode ? `表格指标 ${item.sheetMetricCode}` : '',
  ]
    .filter(Boolean)
    .join(' / ');
}

function typeLabel(item: MesFqcApi.FqcItem) {
  return item.itemType === 'QUALITATIVE' ? '定性' : '定量';
}

function valueTemplateLabel(item: MesFqcApi.FqcItem) {
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

function sampleRows(item: MesFqcApi.FqcItem) {
  const qaSamples = (item.samples || []).filter(
    (sample) => !sample.sampleRole || sample.sampleRole === 'QA',
  );
  const values =
    Array.isArray(item.qaValues) && item.qaValues.length > 0
      ? item.qaValues
      : qaSamples;
  return values.map((sample, index) => normalizeSampleRow(sample, item, index));
}

function normalizeSampleRow(
  sample: MesFqcApi.FqcSample | Record<string, any>,
  item: MesFqcApi.FqcItem,
  index: number,
) {
  const row =
    sample && typeof sample === 'object' ? { ...sample } : { value: sample };
  if ('rawValuesJson' in row && row.rawValuesJson) {
    try {
      Object.assign(row, JSON.parse(String(row.rawValuesJson)));
    } catch {
      // 保留原始字段展示，避免异常 JSON 阻断查看。
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

function dataRule(item: MesFqcApi.FqcItem): DataRuleConfig | undefined {
  try {
    const params = parseEntryRuleParams(item.templateParams);
    const rule = params.dataRule || params;
    const inputFields = Array.isArray(rule.inputFields) ? rule.inputFields : [];
    const resultFields = Array.isArray(rule.resultFields)
      ? rule.resultFields
      : [];
    if (inputFields.length === 0 && resultFields.length === 0) return undefined;
    return {
      inputFields: inputFields.map((field: any) => ({
        code: String(field.code || ''),
        name: String(field.name || field.code || ''),
        unit: field.unit,
      })),
      resultFields: resultFields.map((field: any) => ({
        code: String(field.code || ''),
        name: String(field.name || field.code || ''),
        unit: field.unit,
      })),
    };
  } catch {
    return undefined;
  }
}

function sampleColumns(item: MesFqcApi.FqcItem): SampleColumn[] {
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
      { key: 'defect', label: '缺陷', type: 'defect' },
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
      { key: 'defect', label: '缺陷', type: 'defect' },
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
      { key: 'defect', label: '缺陷', type: 'defect' },
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
      { key: 'defect', label: '缺陷', type: 'defect' },
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
    { key: 'defect', label: '缺陷', type: 'defect' },
    { key: 'remark', label: '备注', type: 'remark' },
  ];
}

function sampleColumnValue(
  row: Record<string, any>,
  item: MesFqcApi.FqcItem,
  column: SampleColumn,
) {
  if (column.type === 'seq') return row.sampleSeq;
  if (column.type === 'position') return row.samplePosition || '-';
  if (column.type === 'group') return row.sampleGroupNo || '-';
  if (column.type === 'defect') return defectText(row);
  if (column.type === 'remark') return row.remark || '-';
  if (column.type === 'result') return resultLabel(row.sampleResult);
  if (column.type === 'field')
    return formatDisplayValue(row[column.source || '']);
  return formatDisplayValue(resolveActualValue(row, item));
}

function resolveActualValue(row: Record<string, any>, item: MesFqcApi.FqcItem) {
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

function defectText(row: Record<string, any>) {
  if (Array.isArray(row.defects) && row.defects.length > 0) {
    return row.defects
      .map((defect: MesFqcApi.FqcSampleDefect) =>
        [defect.defectCode, defect.defectName, defect.defectLevel]
          .filter(Boolean)
          .join('/'),
      )
      .join('；');
  }
  return [row.defectCode, row.defectName].filter(Boolean).join('/') || '-';
}

function labelWithUnit(label: string, unit?: string) {
  return unit ? `${label}/${unit}` : label;
}

function limitText(item: MesFqcApi.FqcItem) {
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

function statText(item: MesFqcApi.FqcItem) {
  const rows: string[] = [];
  appendStat(rows, '最小', item.calculatedMin);
  appendStat(rows, '最大', item.calculatedMax);
  appendStat(rows, '平均', item.calculatedAvg);
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

function progressText(item: MesFqcApi.FqcItem) {
  const required =
    item.requiredSampleCount ?? item.cellRequiredCount ?? item.sampleSize ?? 0;
  const completed = item.completedSampleCount ?? item.cellCompletedCount ?? 0;
  return required > 0 ? `${completed}/${required}` : '-';
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

function formatDate(value?: string) {
  if (!value) return '-';
  return value.slice(0, 19).replace('T', ' ');
}
</script>

<template>
  <Modal
    :open="open"
    title="成品检验真实录入表"
    width="1180px"
    :footer="null"
    @cancel="closeModal"
  >
    <div class="max-h-[76vh] overflow-auto">
      <div class="mb-3 grid grid-cols-4 gap-2 text-xs">
        <div class="rounded border bg-slate-50 p-2">
          <div class="font-bold text-slate-400">FQC单号</div>
          <div class="mt-1 font-mono text-slate-800">
            {{ record?.fqcNo || '-' }}
          </div>
        </div>
        <div class="rounded border bg-slate-50 p-2">
          <div class="font-bold text-slate-400">报告号</div>
          <div class="mt-1 font-mono text-slate-800">
            {{ record?.reportNo || '-' }}
          </div>
        </div>
        <div class="rounded border bg-slate-50 p-2">
          <div class="font-bold text-slate-400">产品型号</div>
          <div class="mt-1 text-slate-800">
            {{ record?.productModel || '-' }}
          </div>
        </div>
        <div class="rounded border bg-slate-50 p-2">
          <div class="font-bold text-slate-400">批次</div>
          <div class="mt-1 font-mono text-slate-800">
            {{ record?.batchNo || record?.productBatchNo || '-' }}
          </div>
        </div>
        <div class="rounded border bg-slate-50 p-2">
          <div class="font-bold text-slate-400">报检数量</div>
          <div class="mt-1 text-slate-800">
            {{ formatReportQty(record) }}
          </div>
        </div>
        <div class="rounded border bg-slate-50 p-2">
          <div class="font-bold text-slate-400">物料编码</div>
          <div class="mt-1 font-mono text-slate-800">
            {{ record?.materialCode || '-' }}
          </div>
        </div>
        <div class="rounded border bg-slate-50 p-2">
          <div class="font-bold text-slate-400">检验员</div>
          <div class="mt-1 text-slate-800">
            {{ record?.inspectorName || record?.operatorName || '-' }}
          </div>
        </div>
        <div class="rounded border bg-slate-50 p-2">
          <div class="font-bold text-slate-400">检验时间</div>
          <div class="mt-1 text-slate-800">
            {{ formatDate(record?.inspectionTime || record?.qaTime) }}
          </div>
        </div>
        <div class="rounded border bg-slate-50 p-2">
          <div class="font-bold text-slate-400">最近保存</div>
          <div class="mt-1 text-slate-800">
            {{ formatDate(record?.lastSaveTime || record?.qaTime) }}
          </div>
        </div>
      </div>

      <Empty
        v-if="groupedItems.length === 0"
        description="当前 FQC 单暂无录入项"
      />

      <div v-else class="space-y-4">
        <section
          v-for="group in groupedItems"
          :key="group.key"
          class="rounded border bg-white"
        >
          <header class="border-b bg-slate-50 px-3 py-2">
            <div class="text-sm font-bold text-slate-800">
              {{ group.title }}
            </div>
            <div v-if="group.subtitle" class="mt-1 text-xs text-slate-500">
              {{ group.subtitle }}
            </div>
          </header>

          <div class="divide-y">
            <article
              v-for="item in group.items"
              :key="item.id || item.standardItemId || item.inspectionItem"
              class="p-3"
            >
              <div class="flex items-start justify-between gap-3">
                <div class="min-w-0">
                  <div class="flex flex-wrap items-center gap-2">
                    <span class="text-sm font-bold text-slate-800">
                      {{ itemTitle(item) }}
                    </span>
                    <Tag color="blue" class="!m-0">{{ typeLabel(item) }}</Tag>
                    <Tag color="purple" class="!m-0">
                      {{ valueTemplateLabel(item) }}
                    </Tag>
                    <Tag
                      :color="inputStatusColor(item.inputStatus)"
                      class="!m-0"
                    >
                      {{ inputStatusLabel(item.inputStatus) }}
                    </Tag>
                    <Tag :color="resultColor(item.qaResult)" class="!m-0">
                      {{ resultLabel(item.qaResult) }}
                    </Tag>
                  </div>
                  <div
                    v-if="itemCodeText(item)"
                    class="mt-1 text-xs text-slate-500"
                  >
                    {{ itemCodeText(item) }}
                  </div>
                </div>
                <div class="shrink-0 text-right text-xs text-slate-500">
                  录入进度
                  <span class="font-mono font-bold text-slate-800">
                    {{ progressText(item) }}
                  </span>
                </div>
              </div>

              <div class="mt-2 grid grid-cols-3 gap-2 text-xs">
                <div class="rounded bg-slate-50 p-2">
                  <div class="font-bold text-slate-400">标准说明</div>
                  <div class="mt-1 whitespace-pre-wrap text-slate-700">
                    {{ item.standardDesc || '-' }}
                  </div>
                </div>
                <div class="rounded bg-slate-50 p-2">
                  <div class="font-bold text-slate-400">控制限</div>
                  <div class="mt-1 text-slate-700">
                    {{ limitText(item) || '-' }}
                  </div>
                </div>
                <div class="rounded bg-slate-50 p-2">
                  <div class="font-bold text-slate-400">统计结果</div>
                  <div class="mt-1 text-slate-700">
                    {{ statText(item) || '-' }}
                  </div>
                </div>
              </div>

              <div
                v-if="
                  item.ruleDescription || item.inspectionMethod || item.testTool
                "
                class="mt-2 rounded bg-amber-50 px-3 py-2 text-xs text-amber-800"
              >
                <span v-if="item.ruleDescription">
                  填写说明：{{ item.ruleDescription }}
                </span>
                <span v-if="item.inspectionMethod" class="ml-3">
                  方法：{{ item.inspectionMethod }}
                </span>
                <span v-if="item.testTool" class="ml-3">
                  工具：{{ item.testTool }}
                </span>
              </div>

              <div class="mt-3 overflow-auto rounded border">
                <table class="w-full min-w-[920px] border-collapse text-xs">
                  <thead class="bg-slate-50 text-slate-500">
                    <tr>
                      <th
                        v-for="column in sampleColumns(item)"
                        :key="column.key"
                        class="border-b px-2 py-2 text-left"
                      >
                        {{ column.label }}
                      </th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-if="sampleRows(item).length === 0">
                      <td
                        class="px-2 py-6 text-center text-slate-400"
                        :colspan="sampleColumns(item).length"
                      >
                        暂无样本数据
                      </td>
                    </tr>
                    <tr
                      v-for="row in sampleRows(item)"
                      :key="`${item.id || item.inspectionItem}-${row.sampleSeq}`"
                      class="border-b last:border-b-0"
                    >
                      <td
                        v-for="column in sampleColumns(item)"
                        :key="column.key"
                        class="whitespace-nowrap px-2 py-2 align-top"
                        :class="
                          column.type === 'result'
                            ? row.sampleResult === 'NG'
                              ? 'font-bold text-red-600'
                              : row.sampleResult === 'OK'
                                ? 'font-bold text-green-700'
                                : 'text-slate-500'
                            : ''
                        "
                      >
                        {{ sampleColumnValue(row, item, column) }}
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </article>
          </div>
        </section>
      </div>
    </div>
  </Modal>
</template>
