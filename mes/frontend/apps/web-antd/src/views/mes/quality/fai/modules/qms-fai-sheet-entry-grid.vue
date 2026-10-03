<script lang="ts" setup>
import type { TableProps } from 'ant-design-vue';

import type { MesFaiApi } from '#/api/mes/quality/fai';

import { computed, reactive, watch } from 'vue';

import { IconifyIcon } from '@vben/icons';

import {
  Alert,
  Button,
  Empty,
  Input,
  InputNumber,
  message,
  Select,
  Table,
  Tabs,
  Tag,
} from 'ant-design-vue';

import {
  buildFaiGridPosition,
  findFaiSheetSection,
  getFaiValueTemplateName,
  resolveFaiExpectedSampleCount,
} from '#/api/mes/quality/fai/template';

defineOptions({ name: 'QmsFaiSheetEntryGrid' });

const props = defineProps<{
  disabled?: boolean;
  items: MesFaiApi.FaiItem[];
  template?: MesFaiApi.FaiSheetTemplate;
}>();

const emit = defineEmits<{
  saveItemValues: [row: MesFaiApi.FaiItem, values: any[]];
}>();

const editBuffers = reactive<Record<string, any[]>>({});

const quantitativeItems = computed(() =>
  props.items.filter((item) => item.itemType === 'QUANTITATIVE'),
);
const qualitativeItems = computed(() =>
  props.items.filter((item) => item.itemType === 'QUALITATIVE'),
);
const sortedSections = computed(() =>
  (props.template?.sections || []).toSorted(
    (a, b) => (a.sort || 0) - (b.sort || 0),
  ),
);
const hasQuantitativeUnmapped = computed(() =>
  quantitativeItems.value.some(
    (item) => !item.sheetSectionCode || !item.sheetMetricCode,
  ),
);

watch(
  () => [props.items, props.template],
  () => {
    props.items.forEach((item) => {
      editBuffers[itemKey(item)] = buildRows(item);
    });
  },
  { immediate: true, deep: true },
);

function itemKey(item: MesFaiApi.FaiItem) {
  return String(item.id || `${item.inspectionItem}-${item.sort || ''}`);
}

function findSection(item: MesFaiApi.FaiItem) {
  return findFaiSheetSection(item, props.template);
}

function expectedSampleCount(item: MesFaiApi.FaiItem) {
  return resolveFaiExpectedSampleCount(item, props.template);
}

function buildRows(item: MesFaiApi.FaiItem) {
  if (item.itemType === 'QUALITATIVE') {
    const current = item.qaValues?.[0];
    const base =
      current && typeof current === 'object'
        ? { ...current }
        : { value: current };
    return [
      {
        sampleSeq: 1,
        samplePosition: base.samplePosition,
        remark: base.remark,
        ...base,
      },
    ];
  }
  const section = findSection(item);
  const size = expectedSampleCount(item);
  return Array.from({ length: size }).map((_, index) => {
    const current = item.qaValues?.[index];
    const base =
      current && typeof current === 'object'
        ? { ...current }
        : { value: current };
    const position = buildFaiGridPosition(index, section);
    return {
      sampleSeq: index + 1,
      ...position,
      samplePosition: base.samplePosition,
      ...base,
    };
  });
}

function getSectionItems(sectionCode: string) {
  return quantitativeItems.value.filter(
    (item) => item.sheetSectionCode === sectionCode,
  );
}

function getUnmappedItems() {
  return quantitativeItems.value.filter(
    (item) => !item.sheetSectionCode || !item.sheetMetricCode,
  );
}

function metricTitle(item: MesFaiApi.FaiItem) {
  return item.sheetMetricName || item.inspectionItem;
}

function templateName(item: MesFaiApi.FaiItem) {
  if (item.valueTemplateName) return item.valueTemplateName;
  return getFaiValueTemplateName(item.valueTemplate).replace('模板', '');
}

function getTemplateParams(item: MesFaiApi.FaiItem) {
  if (!item.templateParams) return {};
  try {
    return JSON.parse(item.templateParams);
  } catch {
    return {};
  }
}

function round(value: number) {
  return Number.isFinite(value) ? Number(value.toFixed(6)) : undefined;
}

function recalculate(item: MesFaiApi.FaiItem, row: any) {
  if (item.valueTemplate === 'DENSITY_CALC') {
    const thickness = Number(row.thicknessMm);
    const weight = Number(row.weightG);
    const diameter = Number(
      row.diameterMm || getTemplateParams(item).diameterMm || 39,
    );
    if (thickness > 0 && weight >= 0 && diameter > 0) {
      row.densityValue = round(
        weight / (((thickness / 10) * 3.14 * diameter * diameter) / 4),
      );
      row.resultValue = row.densityValue;
    } else {
      row.densityValue = undefined;
      row.resultValue = undefined;
    }
    return;
  }
  if (item.valueTemplate === 'COMPRESSION_CALC') {
    const t1 = Number(row.t1Mm);
    const t2 = Number(row.t2Mm);
    const t3 = Number(row.t3Mm);
    if (
      t1 !== 0 &&
      Number.isFinite(t1) &&
      Number.isFinite(t2) &&
      Number.isFinite(t3) &&
      t1 !== t2
    ) {
      row.compressionRate = round(((t1 - t2) / t1) * 100);
      row.compressionElasticityRate = round(((t3 - t2) / (t1 - t2)) * 100);
      row.resultValue =
        item.judgmentMetric === 'COMPRESSION_ELASTICITY_RATE'
          ? row.compressionElasticityRate
          : row.compressionRate;
    } else {
      row.compressionRate = undefined;
      row.compressionElasticityRate = undefined;
      row.resultValue = undefined;
    }
    return;
  }
  row.resultValue = row.value;
}

function metricValue(item: MesFaiApi.FaiItem, row: any) {
  if (item.itemType === 'QUALITATIVE') return row.value;
  recalculate(item, row);
  return (
    row.resultValue ??
    row.densityValue ??
    row.compressionRate ??
    row.compressionElasticityRate ??
    row.value
  );
}

function isNg(item: MesFaiApi.FaiItem, row: any) {
  if (item.itemType === 'QUALITATIVE') return row.value === 'NG';
  const value = Number(metricValue(item, row));
  if (!Number.isFinite(value)) return false;
  return (
    (item.minValueLimit !== undefined && value < item.minValueLimit) ||
    (item.maxValueLimit !== undefined && value > item.maxValueLimit)
  );
}

function isComplete(item: MesFaiApi.FaiItem, row: any) {
  if (item.itemType === 'QUALITATIVE')
    return !!row.value && (row.value !== 'NG' || !!row.remark);
  if (item.valueTemplate === 'DENSITY_CALC')
    return (
      row.thicknessMm !== undefined &&
      row.thicknessMm !== '' &&
      row.weightG !== undefined &&
      row.weightG !== ''
    );
  if (item.valueTemplate === 'COMPRESSION_CALC')
    return (
      row.t1Mm !== undefined &&
      row.t1Mm !== '' &&
      row.t2Mm !== undefined &&
      row.t2Mm !== '' &&
      row.t3Mm !== undefined &&
      row.t3Mm !== ''
    );
  return row.value !== undefined && row.value !== '';
}

function handleSave(item: MesFaiApi.FaiItem) {
  const rows = editBuffers[itemKey(item)] || [];
  const expectedCount = expectedSampleCount(item);
  if (rows.length !== expectedCount) {
    message.warning(
      `当前模板要求录入 ${expectedCount} 条，实际为 ${rows.length} 条`,
    );
    return;
  }
  if (rows.some((row) => !isComplete(item, row))) {
    message.warning('请按当前表格模板录满所有实际值后再保存');
    return;
  }
  rows.forEach((row) => recalculate(item, row));
  item.sampleSize = expectedCount;
  emit(
    'saveItemValues',
    item,
    rows.map((row) => ({
      ...row,
      sampleResult: isNg(item, row) ? 'NG' : 'OK',
    })),
  );
}

function columns(item: MesFaiApi.FaiItem): TableProps['columns'] {
  const section = findSection(item);
  const base: TableProps['columns'] = [
    { title: '序号', dataIndex: 'sampleSeq', width: 56, align: 'center' },
    ...(section?.sectionType === 'GRID_SAMPLE'
      ? [
          {
            title: '样本行',
            dataIndex: 'sampleGroupNo',
            width: 70,
            align: 'center' as const,
          },
          {
            title: '列',
            dataIndex: 'sampleColumnNo',
            width: 56,
            align: 'center' as const,
          },
        ]
      : []),
    { title: '点位', dataIndex: 'samplePosition', width: 110 },
  ];
  if (item.itemType === 'QUALITATIVE') {
    return [
      ...base,
      { title: '是否通过', dataIndex: 'value', width: 130 },
      { title: '备注', dataIndex: 'remark', minWidth: 180 },
      { title: '结果', dataIndex: 'sampleResult', width: 76, align: 'center' },
    ];
  }
  if (item.valueTemplate === 'DENSITY_CALC') {
    return [
      ...base,
      { title: '厚度 mm', dataIndex: 'thicknessMm', width: 110 },
      { title: '重量 g', dataIndex: 'weightG', width: 110 },
      { title: '密度', dataIndex: 'densityValue', width: 110 },
      { title: '结果', dataIndex: 'sampleResult', width: 76, align: 'center' },
    ];
  }
  if (item.valueTemplate === 'COMPRESSION_CALC') {
    return [
      ...base,
      { title: 'T1 mm', dataIndex: 't1Mm', width: 96 },
      { title: 'T2 mm', dataIndex: 't2Mm', width: 96 },
      { title: 'T3 mm', dataIndex: 't3Mm', width: 96 },
      { title: '压缩率', dataIndex: 'compressionRate', width: 108 },
      {
        title: '压缩弹性率',
        dataIndex: 'compressionElasticityRate',
        width: 120,
      },
      { title: '结果', dataIndex: 'sampleResult', width: 76, align: 'center' },
    ];
  }
  return [
    ...base,
    { title: '实测值', dataIndex: 'value', width: 130 },
    { title: '结果', dataIndex: 'sampleResult', width: 76, align: 'center' },
  ];
}
</script>

<template>
  <div class="flex h-full min-h-0 flex-col gap-3">
    <Alert
      v-if="hasQuantitativeUnmapped"
      type="warning"
      show-icon
      message="存在定量标准项未映射表格模板指标，请先在检验标准定义中维护区块、指标和判定字段。"
    />

    <div
      v-if="!template && quantitativeItems.length > 0"
      class="flex flex-1 items-center justify-center rounded border border-dashed border-slate-300 bg-slate-50"
    >
      <Empty description="请选择 FAI 原始记录表模板" />
    </div>

    <Tabs v-else-if="template" class="min-h-0 flex-1" type="card">
      <Tabs.TabPane
        v-for="section in sortedSections"
        :key="section.sectionCode"
        :tab="section.sectionName"
      >
        <div
          class="flex max-h-[calc(100vh-360px)] flex-col gap-3 overflow-y-auto pr-1"
        >
          <div
            v-for="item in getSectionItems(section.sectionCode)"
            :key="itemKey(item)"
            class="rounded border border-slate-200 bg-white"
          >
            <div
              class="flex flex-wrap items-center justify-between gap-2 border-b bg-slate-50 px-3 py-2"
            >
              <div class="min-w-0">
                <div class="flex flex-wrap items-center gap-2">
                  <span class="font-bold text-slate-800">{{
                    metricTitle(item)
                  }}</span>
                  <Tag color="blue" class="!m-0">{{ templateName(item) }}</Tag>
                  <Tag
                    v-if="item.qaResult && item.qaResult !== '-'"
                    :color="item.qaResult === 'OK' ? 'success' : 'error'"
                    class="!m-0"
                  >
                    {{ item.qaResult }}
                  </Tag>
                </div>
                <div class="mt-1 text-xs text-slate-500">
                  单点 {{ item.minValueLimit ?? '-' }} ~
                  {{ item.maxValueLimit ?? '-' }} / 平均
                  {{ item.avgMinLimit ?? '-' }} ~
                  {{ item.avgMaxLimit ?? '-' }} / 标准差
                  {{ item.stdMinLimit ?? '-' }} ~ {{ item.stdMaxLimit ?? '-' }}
                </div>
              </div>
              <Button
                size="small"
                type="primary"
                ghost
                :disabled="disabled"
                @click="handleSave(item)"
              >
                <IconifyIcon icon="lucide:save" class="mr-1" /> 保存表格数据
              </Button>
            </div>
            <Table
              :columns="columns(item)"
              :data-source="editBuffers[itemKey(item)] || []"
              :pagination="false"
              bordered
              row-key="sampleSeq"
              size="small"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.dataIndex === 'samplePosition'">
                  <Input
                    v-model:value="record.samplePosition"
                    :disabled="disabled"
                    size="small"
                    placeholder="如 L5/R3"
                  />
                </template>
                <template v-else-if="column.dataIndex === 'value'">
                  <InputNumber
                    v-model:value="record.value"
                    :disabled="disabled"
                    class="w-full"
                    size="small"
                    @change="() => recalculate(item, record)"
                  />
                </template>
                <template
                  v-else-if="
                    ['thicknessMm', 'weightG', 't1Mm', 't2Mm', 't3Mm'].includes(
                      String(column.dataIndex),
                    )
                  "
                >
                  <InputNumber
                    v-model:value="record[String(column.dataIndex)]"
                    :disabled="disabled"
                    class="w-full"
                    size="small"
                    @change="() => recalculate(item, record)"
                  />
                </template>
                <template
                  v-else-if="
                    [
                      'densityValue',
                      'compressionRate',
                      'compressionElasticityRate',
                    ].includes(String(column.dataIndex))
                  "
                >
                  <span class="font-mono">{{
                    record[String(column.dataIndex)] ?? '-'
                  }}</span>
                </template>
                <template v-else-if="column.dataIndex === 'sampleResult'">
                  <Tag
                    :color="
                      isNg(item, record)
                        ? 'error'
                        : isComplete(item, record)
                          ? 'success'
                          : 'default'
                    "
                    class="!m-0"
                  >
                    {{
                      isNg(item, record)
                        ? 'NG'
                        : isComplete(item, record)
                          ? 'OK'
                          : '-'
                    }}
                  </Tag>
                </template>
              </template>
            </Table>
          </div>
          <Empty
            v-if="getSectionItems(section.sectionCode).length === 0"
            description="当前区块暂无已映射的定量检验项"
          />
        </div>
      </Tabs.TabPane>
      <Tabs.TabPane
        v-if="getUnmappedItems().length > 0"
        key="UNMAPPED"
        tab="未映射"
      >
        <div class="flex flex-col gap-2">
          <Alert
            type="warning"
            show-icon
            message="以下定量项尚未维护 sheetSectionCode / sheetMetricCode，本阶段只能在明细概览中查看，不能按表格模板归档。"
          />
          <div
            v-for="item in getUnmappedItems()"
            :key="itemKey(item)"
            class="rounded border bg-white px-3 py-2 text-sm"
          >
            <span class="font-bold">{{ item.inspectionItem }}</span>
            <span class="ml-2 text-slate-500">{{ item.standardDesc }}</span>
          </div>
        </div>
      </Tabs.TabPane>
    </Tabs>

    <div
      v-if="qualitativeItems.length > 0"
      class="rounded border border-slate-200 bg-white"
    >
      <div class="flex items-center gap-2 border-b bg-slate-50 px-3 py-2">
        <IconifyIcon icon="lucide:check-square" class="text-green-600" />
        <span class="font-bold text-slate-700">定性判定</span>
        <span class="text-xs text-slate-500">定性项不使用模板，只填写 OK/NG；NG 必填备注。</span>
      </div>
      <div class="p-2">
        <div
          v-for="item in qualitativeItems"
          :key="itemKey(item)"
          class="mb-2 rounded border border-slate-100"
        >
          <div
            class="flex items-center justify-between border-b bg-white px-3 py-2"
          >
            <div>
              <span class="font-bold">{{ item.inspectionItem }}</span>
              <span class="ml-2 text-xs text-slate-500">{{
                item.standardDesc
              }}</span>
            </div>
            <Button
              size="small"
              type="primary"
              ghost
              :disabled="disabled"
              @click="handleSave(item)"
            >
              保存判定
            </Button>
          </div>
          <Table
            :columns="columns(item)"
            :data-source="editBuffers[itemKey(item)] || []"
            :pagination="false"
            bordered
            row-key="sampleSeq"
            size="small"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'samplePosition'">
                <Input
                  v-model:value="record.samplePosition"
                  :disabled="disabled"
                  size="small"
                  placeholder="点位"
                />
              </template>
              <template v-else-if="column.dataIndex === 'value'">
                <Select
                  v-model:value="record.value"
                  :disabled="disabled"
                  class="w-full"
                  size="small"
                  :options="[
                    { label: '合格 (OK)', value: 'OK' },
                    { label: '不合格 (NG)', value: 'NG' },
                  ]"
                />
              </template>
              <template v-else-if="column.dataIndex === 'remark'">
                <Input
                  v-model:value="record.remark"
                  :disabled="disabled"
                  size="small"
                  placeholder="NG 时填写原因"
                />
              </template>
              <template v-else-if="column.dataIndex === 'sampleResult'">
                <Tag
                  :color="
                    isNg(item, record)
                      ? 'error'
                      : isComplete(item, record)
                        ? 'success'
                        : 'default'
                  "
                  class="!m-0"
                >
                  {{
                    isNg(item, record)
                      ? 'NG'
                      : isComplete(item, record)
                        ? 'OK'
                        : '-'
                  }}
                </Tag>
              </template>
            </template>
          </Table>
        </div>
      </div>
    </div>
  </div>
</template>
