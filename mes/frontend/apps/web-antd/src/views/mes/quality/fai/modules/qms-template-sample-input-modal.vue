<script lang="ts" setup>
import type { TableProps } from 'ant-design-vue';

import type { MesFaiApi } from '#/api/mes/quality/fai';

import { computed, ref, watch } from 'vue';

import {
  Input,
  InputNumber,
  message,
  Modal,
  Select,
  Table,
  Tag,
} from 'ant-design-vue';

import {
  buildFaiGridPosition,
  findFaiSheetSection,
  getFaiJudgmentMetricName,
  getFaiValueTemplateName,
  resolveFaiExpectedSampleCount,
} from '#/api/mes/quality/fai/template';

defineOptions({ name: 'QmsTemplateSampleInputModal' });

const props = defineProps<{
  open: boolean;
  row: MesFaiApi.FaiItem | null;
  template?: MesFaiApi.FaiSheetTemplate;
}>();

const emit = defineEmits<{
  save: [values: any[]];
  'update:open': [value: boolean];
}>();

const rows = ref<any[]>([]);

const templateName = computed(
  () =>
    props.row?.valueTemplateName ||
    getFaiValueTemplateName(props.row?.valueTemplate),
);
const judgmentMetricName = computed(() =>
  getFaiJudgmentMetricName(props.row?.judgmentMetric),
);
const expectedSampleCount = computed(() =>
  resolveFaiExpectedSampleCount(props.row, props.template),
);

watch(
  () => [props.open, props.row?.id],
  () => {
    if (!props.open || !props.row) return;
    rows.value = buildRows(props.row);
  },
  { immediate: true },
);

function buildRows(row: MesFaiApi.FaiItem) {
  if (row.itemType === 'QUALITATIVE') {
    const current = row.qaValues?.[0];
    const base =
      current && typeof current === 'object'
        ? { ...current }
        : { value: current };
    return [{ sampleSeq: 1, samplePosition: base.samplePosition, ...base }];
  }
  const section = findFaiSheetSection(row, props.template);
  const size = expectedSampleCount.value;
  return Array.from({ length: size }).map((_, index) => {
    const current = row.qaValues?.[index];
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

function getTemplateParams() {
  if (!props.row?.templateParams) return {};
  try {
    return JSON.parse(props.row.templateParams);
  } catch {
    return {};
  }
}

function getDiameterMm(row: any) {
  return Number(row.diameterMm || getTemplateParams().diameterMm || 39);
}

function round(value: number) {
  return Number.isFinite(value) ? Number(value.toFixed(6)) : undefined;
}

function recalculate(row: any) {
  const template = props.row?.valueTemplate || 'SINGLE_VALUE';
  if (template === 'DENSITY_CALC') {
    const thickness = Number(row.thicknessMm);
    const weight = Number(row.weightG);
    const diameter = getDiameterMm(row);
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
  if (template === 'COMPRESSION_CALC') {
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
        props.row?.judgmentMetric === 'COMPRESSION_ELASTICITY_RATE'
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

function isRowComplete(row: any) {
  if (props.row?.itemType === 'QUALITATIVE') return !!row.value;
  const template = props.row?.valueTemplate || 'SINGLE_VALUE';
  if (template === 'DENSITY_CALC')
    return (
      row.thicknessMm !== undefined &&
      row.thicknessMm !== '' &&
      row.weightG !== undefined &&
      row.weightG !== ''
    );
  if (template === 'COMPRESSION_CALC')
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

function getMetricValue(row: any) {
  if (props.row?.itemType === 'QUALITATIVE') return row.value;
  recalculate(row);
  return row.resultValue;
}

function hasMetricValue(row: any) {
  const value = getMetricValue(row);
  return value !== undefined && value !== null && value !== '';
}

function isNg(row: any) {
  if (props.row?.itemType === 'QUALITATIVE') return row.value === 'NG';
  const value = Number(getMetricValue(row));
  if (!Number.isFinite(value)) return false;
  const min = props.row?.minValueLimit;
  const max = props.row?.maxValueLimit;
  return (
    (min !== undefined && value < min) || (max !== undefined && value > max)
  );
}

function handleCancel() {
  emit('update:open', false);
}

function handleOk() {
  for (const row of rows.value) {
    recalculate(row);
  }
  if (rows.value.some((row) => !isRowComplete(row))) {
    message.warning(`必须按表格录满 ${expectedSampleCount.value} 组实测数据`);
    return;
  }
  emit(
    'save',
    rows.value.map((row) => ({
      ...row,
      sampleResult: isNg(row) ? 'NG' : 'OK',
    })),
  );
  emit('update:open', false);
}

const columns = computed<TableProps['columns']>(() => {
  const base: TableProps['columns'] = [
    { title: '序号', dataIndex: 'sampleSeq', width: 56, align: 'center' },
    ...(findFaiSheetSection(props.row, props.template)?.sectionType ===
    'GRID_SAMPLE'
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
  if (props.row?.itemType === 'QUALITATIVE') {
    return [
      ...base,
      { title: '判定值', dataIndex: 'value', width: 150 },
      { title: '结果', dataIndex: 'sampleResult', width: 80, align: 'center' },
    ];
  }
  if (props.row?.valueTemplate === 'DENSITY_CALC') {
    return [
      ...base,
      { title: '厚度 mm', dataIndex: 'thicknessMm', width: 120 },
      { title: '重量 g', dataIndex: 'weightG', width: 120 },
      { title: '密度', dataIndex: 'densityValue', width: 110 },
      { title: '结果', dataIndex: 'sampleResult', width: 80, align: 'center' },
    ];
  }
  if (props.row?.valueTemplate === 'COMPRESSION_CALC') {
    return [
      ...base,
      { title: 'T1 mm', dataIndex: 't1Mm', width: 100 },
      { title: 'T2 mm', dataIndex: 't2Mm', width: 100 },
      { title: 'T3 mm', dataIndex: 't3Mm', width: 100 },
      { title: '压缩率', dataIndex: 'compressionRate', width: 110 },
      {
        title: '压缩弹性率',
        dataIndex: 'compressionElasticityRate',
        width: 120,
      },
      { title: '结果', dataIndex: 'sampleResult', width: 80, align: 'center' },
    ];
  }
  return [
    ...base,
    { title: '实测值', dataIndex: 'value', width: 140 },
    { title: '结果', dataIndex: 'sampleResult', width: 80, align: 'center' },
  ];
});
</script>

<template>
  <Modal
    :open="open"
    :title="`录入实测值：${row?.inspectionItem || ''}`"
    width="820px"
    centered
    @cancel="handleCancel"
    @ok="handleOk"
  >
    <div
      v-if="row"
      class="mb-3 rounded border border-indigo-100 bg-indigo-50 p-2 text-xs text-indigo-800"
    >
      <div class="flex flex-wrap gap-x-4 gap-y-1">
        <span
          >模板：<b>{{ templateName }}</b></span
        >
        <span
          >判定指标：<b>{{ judgmentMetricName }}</b></span
        >
        <span>标准：{{ row.standardDesc || '-' }}</span>
      </div>
      <div class="mt-1 flex flex-wrap gap-x-4 gap-y-1 text-indigo-700">
        <span
          >单点：{{ row.minValueLimit ?? '-' }} ~
          {{ row.maxValueLimit ?? '-' }}</span
        >
        <span
          >平均值：{{ row.avgMinLimit ?? '-' }} ~
          {{ row.avgMaxLimit ?? '-' }}</span
        >
        <span
          >标准差：{{ row.stdMinLimit ?? '-' }} ~
          {{ row.stdMaxLimit ?? '-' }}</span
        >
      </div>
    </div>

    <Table
      :columns="columns"
      :data-source="rows"
      :pagination="false"
      size="small"
      bordered
      row-key="sampleSeq"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'samplePosition'">
          <Input
            v-model:value="record.samplePosition"
            size="small"
            placeholder="如 L5/R3"
          />
        </template>
        <template
          v-else-if="
            column.dataIndex === 'value' && row?.itemType === 'QUALITATIVE'
          "
        >
          <Select
            v-model:value="record.value"
            size="small"
            class="w-full"
            :options="[
              { label: '合格 (OK)', value: 'OK' },
              { label: '不合格 (NG)', value: 'NG' },
            ]"
          />
        </template>
        <template v-else-if="column.dataIndex === 'value'">
          <InputNumber
            v-model:value="record.value"
            size="small"
            class="w-full"
            @change="() => recalculate(record)"
          />
        </template>
        <template v-else-if="column.dataIndex === 'thicknessMm'">
          <InputNumber
            v-model:value="record.thicknessMm"
            size="small"
            class="w-full"
            @change="() => recalculate(record)"
          />
        </template>
        <template v-else-if="column.dataIndex === 'weightG'">
          <InputNumber
            v-model:value="record.weightG"
            size="small"
            class="w-full"
            @change="() => recalculate(record)"
          />
        </template>
        <template v-else-if="column.dataIndex === 't1Mm'">
          <InputNumber
            v-model:value="record.t1Mm"
            size="small"
            class="w-full"
            @change="() => recalculate(record)"
          />
        </template>
        <template v-else-if="column.dataIndex === 't2Mm'">
          <InputNumber
            v-model:value="record.t2Mm"
            size="small"
            class="w-full"
            @change="() => recalculate(record)"
          />
        </template>
        <template v-else-if="column.dataIndex === 't3Mm'">
          <InputNumber
            v-model:value="record.t3Mm"
            size="small"
            class="w-full"
            @change="() => recalculate(record)"
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
              isNg(record)
                ? 'error'
                : hasMetricValue(record)
                  ? 'success'
                  : 'default'
            "
            class="!m-0"
          >
            {{ isNg(record) ? 'NG' : hasMetricValue(record) ? 'OK' : '-' }}
          </Tag>
        </template>
      </template>
    </Table>
  </Modal>
</template>
