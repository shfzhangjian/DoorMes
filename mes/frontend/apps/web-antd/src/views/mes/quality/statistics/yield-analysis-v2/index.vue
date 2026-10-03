<script lang="ts" setup>
import type { TableColumnsType } from 'ant-design-vue';

import type { EchartsUIType } from '@vben/plugins/echarts';

import type { MesQmsYieldAnalysisV2Api } from '#/api/mes/quality/statistics/yield-analysis-v2';

import { computed, nextTick, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';

import { Page } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';
import { EchartsUI, useEcharts } from '@vben/plugins/echarts';

import {
  Button,
  DatePicker,
  Input,
  message,
  Pagination,
  Select,
  Statistic,
  Table,
  Tag,
} from 'ant-design-vue';
import dayjs from 'dayjs';

import {
  getYieldAnalysisV2DetailPage,
  getYieldAnalysisV2List,
  getYieldAnalysisV2Overview,
  getYieldAnalysisV2SyncStatus,
  getYieldAnalysisV2TargetModelOptions,
  syncYieldAnalysisV2,
} from '#/api/mes/quality/statistics/yield-analysis-v2';

defineOptions({ name: 'MesQmsYieldAnalysisV2' });

type PivotRow = MesQmsYieldAnalysisV2Api.ProcessPivotRow;
type PivotStage = MesQmsYieldAnalysisV2Api.ProcessPivotStage;
type DetailRow = MesQmsYieldAnalysisV2Api.DetailRow;
type StageFieldKey = 'batch' | keyof PivotStage;
type ViewLevel = 'detail' | 'summary';

interface StageField {
  key: StageFieldKey;
  label: string;
  width?: number;
}

interface StageDefinition {
  code: string;
  fields: StageField[];
  label: string;
  unit: string;
}

interface DrillContext {
  fieldKey: StageFieldKey;
  fieldLabel: string;
  processCode: string;
  processName: string;
  row: PivotRow;
}

const processOptions = [
  { label: '全部工序', value: 'ALL' },
  { label: '配料', value: 'FORMULA' },
  { label: '湿法', value: 'WET' },
  { label: '磨皮', value: 'ROUGH_GRINDING' },
  { label: '粘胶1', value: 'ADHESIVE1' },
  { label: '分切', value: 'SLITTING' },
  { label: '压槽', value: 'PRESS_SLOT' },
  { label: '粘胶2', value: 'ADHESIVE2' },
  { label: '裁切', value: 'CUT_ROUND' },
  { label: '终检', value: 'FINAL_INSPECTION' },
];

const formulaStageFields: StageField[] = [
  { key: 'doneQty', label: '完工', width: 76 },
  { key: 'theoreticalOutputQty', label: '理论产量', width: 92 },
  { key: 'goodYieldRate', label: '良品率（%）', width: 90 },
  { key: 'inspectionQty', label: '送检', width: 76 },
  { key: 'inspectionNgQty', label: '检验NG', width: 76 },
  { key: 'lastReportTime', label: '完工时间', width: 142 },
];

const wetStageFields: StageField[] = [
  { key: 'doneQty', label: '收卷米数(报工数)', width: 122 },
  { key: 'theoreticalOutputQty', label: '理论产量', width: 92 },
  { key: 'goodYieldRate', label: '良品率（%）', width: 90 },
  { key: 'inspectionQty', label: 'NAP层送检(米)', width: 108 },
  { key: 'inspectionNgQty', label: '检验NG', width: 76 },
  { key: 'defectQty', label: '固定损耗（米）', width: 108 },
  { key: 'lastReportTime', label: '完工时间', width: 142 },
];

const grindingStageFields: StageField[] = [
  { key: 'startPosition', label: '起位置(m)', width: 82 },
  { key: 'processLength', label: '长度(m)', width: 82 },
  { key: 'inputQty', label: '投入米数', width: 82 },
  { key: 'doneQty', label: '产出米数', width: 86 },
  { key: 'motherOutputQty', label: '母卷产出', width: 90 },
  { key: 'theoreticalOutputQty', label: '理论产量', width: 92 },
  { key: 'goodYieldRate', label: '良品率（%）', width: 90 },
  { key: 'inspectionQty', label: 'NAP留样米数/送检送数', width: 148 },
  { key: 'inspectionNgQty', label: '检验NG', width: 76 },
  { key: 'defectQty', label: '固定损耗（米）', width: 108 },
  { key: 'lastReportTime', label: '完工时间', width: 142 },
];

const adhesive1StageFields: StageField[] = [
  { key: 'batch', label: '分段批号', width: 132 },
  { key: 'inputQty', label: '投入米数', width: 82 },
  { key: 'doneQty', label: '产出米数', width: 86 },
  { key: 'motherOutputQty', label: '母卷产出', width: 90 },
  { key: 'theoreticalOutputQty', label: '理论产量', width: 92 },
  { key: 'goodYieldRate', label: '良品率（%）', width: 90 },
  { key: 'inspectionQty', label: 'NAP留样米数/送检送数', width: 148 },
  { key: 'inspectionNgQty', label: '检验NG', width: 76 },
  { key: 'defectQty', label: '固定损耗（米）', width: 108 },
  { key: 'lastReportTime', label: '完工时间', width: 142 },
];

function pieceStageFields(stageCode: string): StageField[] {
  const fields: StageField[] = [
    { key: 'doneQty', label: '分段自检OK', width: 98 },
    { key: 'segmentDefectQty', label: '分段自检NG', width: 98 },
    { key: 'inspectionQty', label: '送检', width: 76 },
    ...(['ADHESIVE2', 'CUT_ROUND', 'PRESS_SLOT'].includes(stageCode)
      ? [
          {
            key: 'processProductionInspectionQty' as const,
            label: '本工序损耗',
            width: 98,
          },
        ]
      : []),
    { key: 'inspectionNgQty', label: '送检NG', width: 76 },
  ];
  if (['ADHESIVE2', 'CUT_ROUND', 'PRESS_SLOT'].includes(stageCode)) {
    fields.push(
      { key: 'coaInspectionQty', label: 'COA送检', width: 82 },
      { key: 'coaInspectionNgQty', label: 'COA NG', width: 82 },
    );
  }
  if (stageCode === 'ADHESIVE2') {
    fields.push({
      key: 'glueBoardInspectionQty',
      label: '胶板送检(m)',
      width: 98,
    });
  }
  fields.push(
    { key: 'pendingQty', label: '未加工', width: 76 },
    { key: 'defectQty', label: '自检总NG', width: 82 },
    { key: 'segmentInputQty', label: '分段投入', width: 90 },
    { key: 'inputQty', label: '总投入', width: 82 },
    { key: 'motherOutputQty', label: '总产出', width: 82 },
    { key: 'theoreticalOutputQty', label: '理论产量', width: 92 },
    { key: 'goodYieldRate', label: '良品率（%）', width: 90 },
  );
  if (stageCode === 'CUT_ROUND') {
    fields.push(
      {
        key: 'finalInspectionOutputQty',
        label: '终检产出',
        width: 88,
      },
      {
        key: 'goodTargetRate',
        label: '良品达标率（%）',
        width: 116,
      },
    );
  }
  fields.push({ key: 'lastReportTime', label: '扫码确认时间', width: 142 });
  return fields;
}

const stageDefinitions: StageDefinition[] = [
  { code: 'FORMULA', fields: formulaStageFields, label: '配料', unit: 'kg' },
  { code: 'WET', fields: wetStageFields, label: '湿法', unit: 'm' },
  { code: 'GRINDING', fields: grindingStageFields, label: '磨皮', unit: 'm' },
  {
    code: 'ADHESIVE1',
    fields: adhesive1StageFields,
    label: '粘胶1',
    unit: 'm',
  },
  {
    code: 'SLITTING',
    fields: pieceStageFields('SLITTING'),
    label: '分切',
    unit: '片',
  },
  {
    code: 'PRESS_SLOT',
    fields: pieceStageFields('PRESS_SLOT'),
    label: '压槽',
    unit: '片',
  },
  {
    code: 'ADHESIVE2',
    fields: pieceStageFields('ADHESIVE2'),
    label: '粘胶2',
    unit: '片',
  },
  {
    code: 'CUT_ROUND',
    fields: pieceStageFields('CUT_ROUND'),
    label: '裁切',
    unit: '片',
  },
  {
    code: 'SHIPPING_INSPECTION',
    fields: [
      { key: 'inspectionQty', label: '送检', width: 76 },
      { key: 'inspectionNgQty', label: '检验NG', width: 76 },
      { key: 'lastReportTime', label: '送检时间', width: 142 },
    ],
    label: '发货检验',
    unit: '片',
  },
];

function naturalWeekRange(reference = dayjs()) {
  const weekday = reference.day();
  const monday = reference
    .subtract(weekday === 0 ? 6 : weekday - 1, 'day')
    .startOf('day');
  return [
    monday.format('YYYY-MM-DD'),
    monday.add(6, 'day').format('YYYY-MM-DD'),
  ];
}

const router = useRouter();
const query = reactive({
  dateRange: naturalWeekRange() as string[],
  materialKeyword: undefined as string | undefined,
  modelCode: undefined as string | undefined,
  motherRollBatchNo: undefined as string | undefined,
  motherSegmentBatchNo: undefined as string | undefined,
  pieceNo: undefined as string | undefined,
  planNo: undefined as string | undefined,
  processCode: 'ALL',
});

const loading = ref(false);
const rows = ref<PivotRow[]>([]);
const total = ref(0);
const modelCodes = ref<string[]>([]);
const overview = ref<MesQmsYieldAnalysisV2Api.OverviewResult>();
const previousOverview = ref<MesQmsYieldAnalysisV2Api.OverviewResult>();
const syncStatus = ref<MesQmsYieldAnalysisV2Api.SyncStatus>();
const analysisExpanded = ref(true);
const queryExpanded = ref(false);
const syncing = ref(false);
const viewLevel = ref<ViewLevel>('summary');
const activeDrill = ref<DrillContext>();
const detailLoading = ref(false);
const detailRows = ref<DetailRow[]>([]);
const detailPagination = reactive({ current: 1, pageSize: 20, total: 0 });
const trendChartRef = ref<EchartsUIType>();
const defectChartRef = ref<EchartsUIType>();
const { renderEcharts: renderTrendChart } = useEcharts(trendChartRef);
const { renderEcharts: renderDefectChart } = useEcharts(defectChartRef);

const modelOptions = computed(() =>
  modelCodes.value.map((value) => ({ label: value, value })),
);

const selectedProcessLabel = computed(
  () =>
    processOptions.find((item) => item.value === query.processCode)?.label ||
    '全部工序',
);

const processTitleSuffix = computed(() =>
  query.processCode === 'ALL' ? '' : `（${selectedProcessLabel.value}工序）`,
);

function toStageCode(processCode: string) {
  if (processCode === 'ROUGH_GRINDING') return 'GRINDING';
  if (processCode === 'FINAL_INSPECTION') return 'SHIPPING_INSPECTION';
  return processCode;
}

const visibleStageDefinitions = computed(() => {
  if (query.processCode === 'ALL') return stageDefinitions;
  const stageCode = toStageCode(query.processCode);
  return stageDefinitions.filter((stage) => stage.code === stageCode);
});

function formatLocalDate(value?: number[] | string) {
  if (Array.isArray(value) && value.length >= 3) {
    return `${value[0]}-${String(value[1]).padStart(2, '0')}-${String(value[2]).padStart(2, '0')}`;
  }
  return value || '-';
}

const syncStatusText = computed(() => {
  if (!syncStatus.value?.latestSyncTime) return '日结数据：尚未同步';
  const statDate = formatLocalDate(syncStatus.value.latestStatDate);
  const updateTime = dayjs(syncStatus.value.latestSyncTime).format(
    'YYYY-MM-DD HH:mm:ss',
  );
  return `日结数据至 ${statDate}，更新于 ${updateTime}`;
});

function buildMetricTrend(currentValue: unknown, previousValue: unknown) {
  const current = Number(currentValue || 0);
  const previous = Number(previousValue || 0);
  const difference = current - previous;
  if (Math.abs(difference) < 0.000_001) {
    return {
      className: 'metric-trend--stable',
      icon: 'lucide:minus',
      title: `与上一期持平（本期 ${formatNumber(current)} / 上期 ${formatNumber(previous)}）`,
    };
  }
  const rising = difference > 0;
  return {
    className: rising ? 'metric-trend--up' : 'metric-trend--down',
    icon: rising ? 'lucide:trending-up' : 'lucide:trending-down',
    title: `${rising ? '较上一期上升' : '较上一期下降'}（本期 ${formatNumber(current)} / 上期 ${formatNumber(previous)}）`,
  };
}

const metricItems = computed(() => {
  const value = overview.value?.overview;
  const previousValue = previousOverview.value?.overview;
  return [
    {
      color: '#15803d',
      icon: 'lucide:badge-check',
      suffix: '%',
      title: '良品率',
      trend: buildMetricTrend(value?.yieldRate, previousValue?.yieldRate),
      value: value?.yieldRate ?? 0,
    },
    {
      color: '#1d4ed8',
      icon: 'lucide:scan-line',
      title: '实际报工记录',
      trend: buildMetricTrend(
        value?.confirmedTotal,
        previousValue?.confirmedTotal,
      ),
      value: value?.confirmedTotal ?? 0,
    },
    {
      color: '#0369a1',
      icon: 'lucide:package-open',
      title: '投入数',
      trend: buildMetricTrend(value?.inputTotal, previousValue?.inputTotal),
      value: value?.inputTotal ?? 0,
    },
    {
      color: '#15803d',
      icon: 'lucide:package-check',
      title: '产出良品数',
      trend: buildMetricTrend(
        value?.outputGoodTotal,
        previousValue?.outputGoodTotal,
      ),
      value: value?.outputGoodTotal ?? 0,
    },
    {
      color: '#dc2626',
      icon: 'lucide:package-x',
      title: '产出不良品数',
      trend: buildMetricTrend(
        value?.outputNgTotal,
        previousValue?.outputNgTotal,
      ),
      value: value?.outputNgTotal ?? 0,
    },
  ];
});

function buildParams(): MesQmsYieldAnalysisV2Api.QueryParams {
  return {
    actualReportDateEnd: query.dateRange?.[1],
    actualReportDateStart: query.dateRange?.[0],
    materialKeyword: query.materialKeyword,
    modelCode: query.modelCode,
    motherRollBatchNo: query.motherRollBatchNo,
    motherSegmentBatchNo: query.motherSegmentBatchNo,
    pageNo: 1,
    pageSize: 20,
    pieceNo: query.pieceNo,
    planNo: query.planNo,
    processCode: query.processCode,
  };
}

function buildPreviousParams(): MesQmsYieldAnalysisV2Api.QueryParams {
  const params = buildParams();
  const start = dayjs(query.dateRange?.[0]).subtract(7, 'day');
  const end = dayjs(query.dateRange?.[1]).subtract(7, 'day');
  return {
    ...params,
    actualReportDateEnd: end.format('YYYY-MM-DD'),
    actualReportDateStart: start.format('YYYY-MM-DD'),
  };
}

function stageOf(row: PivotRow, stageCode: string): PivotStage {
  return (row.stages?.[stageCode] || {}) as PivotStage;
}

function formatNumber(value: unknown) {
  if (value === undefined || value === null || value === '') return '-';
  const numberValue = Number(value);
  if (!Number.isFinite(numberValue)) return String(value);
  return numberValue.toLocaleString('zh-CN', { maximumFractionDigits: 2 });
}

function formatPercentValue(value: unknown) {
  return value === undefined || value === null
    ? '-'
    : `${formatNumber(value)}%`;
}

function formatBusinessDateTime(value: unknown) {
  const text = String(value || '').trim();
  if (!text) return '-';
  const parsed = dayjs(text);
  if (!parsed.isValid() || parsed.year() < 2000) return '-';
  return parsed.format('YYYY-MM-DD HH:mm:ss');
}

function formatStageValue(
  row: PivotRow,
  stageCode: string,
  key: StageFieldKey,
) {
  const stage = stageOf(row, stageCode);
  if (key === 'batch') {
    return stage.outputBatchNos || stage.sourceBatchNos || '-';
  }
  const value = stage[key];
  if (key === 'lastReportTime') return formatBusinessDateTime(value);
  if (key === 'goodYieldRate' || key === 'goodTargetRate') {
    return formatPercentValue(value);
  }
  return formatNumber(value);
}

function formatBaseValue(row: PivotRow, key: string) {
  if (key === 'motherRollBatchNo') return row.motherRollBatchNo || '-';
  if (key === 'modelCode') return modelText(row);
  if (key === 'materialCode') return row.materialCode || '-';
  if (key === 'sizeSpec') return sizeText(row);
  if (key === 'segmentBatchNo') return row.segmentBatchNo || '-';
  return '-';
}

function getStageColumnCode(column: { key?: unknown }) {
  const [stageCode] = String(column?.key || '').split('.');
  return stageDefinitions.some((stage) => stage.code === stageCode)
    ? stageCode
    : '';
}

function getStageColumnField(column: { key?: unknown }): StageFieldKey {
  return String(column?.key || '').split('.')[1] as StageFieldKey;
}

function hasStageReport(row: PivotRow, stageCode: string) {
  const stage = stageOf(row, stageCode);
  if (stage.stageStatus && stage.stageStatus !== 'NOT_STARTED') return true;
  if (formatBusinessDateTime(stage.lastReportTime) !== '-') return true;
  if (stage.outputBatchNos || stage.sourceBatchNos) return true;
  if (stage.pieceDetails?.length || stage.inspectionDetails?.length)
    return true;
  const numericKeys: (keyof PivotStage)[] = [
    'coaInspectionNgQty',
    'coaInspectionQty',
    'confirmedQty',
    'defectQty',
    'doneQty',
    'finalInspectionOutputQty',
    'glueBoardInspectionQty',
    'inputQty',
    'inspectionNgQty',
    'inspectionQty',
    'motherOutputQty',
    'processProductionInspectionQty',
    'processLength',
    'reportQty',
    'segmentDefectQty',
    'segmentInputQty',
    'theoreticalOutputQty',
  ];
  return (
    numericKeys.some((key) => Number(stage[key] || 0) !== 0) ||
    Number(stage.goodYieldRate || 0) !== 0 ||
    Number(stage.goodTargetRate || 0) !== 0
  );
}

function normalizeStageProcessCode(stageCode: string) {
  if (stageCode === 'GRINDING') return 'ROUGH_GRINDING';
  if (stageCode === 'SHIPPING_INSPECTION') return 'FINAL_INSPECTION';
  return stageCode;
}

function getStageDefinition(stageCode: string) {
  return stageDefinitions.find((stage) => stage.code === stageCode);
}

function resolveDrillMetricKey(fieldKey: StageFieldKey) {
  const metricMap: Partial<Record<StageFieldKey, string>> = {
    defectQty: 'outputNgTotal',
    doneQty: 'outputGoodTotal',
    finalInspectionOutputQty: 'outputGoodTotal',
    goodYieldRate: 'yieldRate',
    inputQty: 'inputTotal',
    inspectionNgQty: 'submissionNgTotal',
    inspectionQty: 'inspectionTotal',
    motherOutputQty: 'outputGoodTotal',
    segmentDefectQty: 'outputNgTotal',
    segmentInputQty: 'inputTotal',
  };
  return metricMap[fieldKey];
}

function modelText(row: PivotRow) {
  return (
    row.actualModelCode ||
    row.modelSeriesCode ||
    row.modelCode ||
    row.modelName ||
    '-'
  );
}

function sizeText(row: PivotRow) {
  return row.actualSizeSpec || row.sizeName || row.sizeSpec || '-';
}

function sizeLines(row: PivotRow) {
  return sizeText(row)
    .split(/[,，]/)
    .map((item) => item.trim())
    .filter(Boolean);
}

const tableColumns = computed<TableColumnsType<PivotRow>>(() => {
  const baseColumns: TableColumnsType<PivotRow> = [
    {
      customRender: ({ record }) => record.motherRollBatchNo || '-',
      fixed: 'left',
      key: 'motherRollBatchNo',
      title: '母批批号',
      width: 132,
    },
    {
      customRender: ({ record }) => modelText(record),
      fixed: 'left',
      key: 'modelCode',
      title: '型号',
      width: 104,
    },
    {
      customRender: ({ record }) => sizeText(record),
      key: 'sizeSpec',
      title: '尺寸',
      width: 86,
    },
    {
      customRender: ({ record }) => record.segmentBatchNo || '-',
      key: 'segmentBatchNo',
      title: '磨皮分段片号',
      width: 132,
    },
  ];
  const stageColumns = visibleStageDefinitions.value.map((stage) => ({
    children: stage.fields.map((field) => ({
      align: 'center' as const,
      customRender: ({ record }: { record: PivotRow }) =>
        formatStageValue(record, stage.code, field.key),
      key: `${stage.code}.${String(field.key)}`,
      title: field.label,
      width: field.width || 82,
    })),
    key: stage.code,
    title: `${stage.label}(${stage.unit})`,
  }));
  return [...baseColumns, ...stageColumns];
});

const tableScrollX = computed(
  () =>
    454 +
    visibleStageDefinitions.value.reduce(
      (sum, stage) =>
        sum +
        stage.fields.reduce(
          (stageSum, field) => stageSum + (field.width || 82),
          0,
        ),
      0,
    ),
);

const summaryTableScroll = computed(() => ({
  x: tableScrollX.value,
  ...(rows.value.length > 0 ? { y: 360 } : {}),
}));

const detailColumns: TableColumnsType<DetailRow> = [
  {
    dataIndex: 'segmentNo',
    fixed: 'left',
    key: 'segmentNo',
    title: '分段',
    width: 126,
  },
  {
    dataIndex: 'pieceNo',
    fixed: 'left',
    key: 'pieceNo',
    title: '片号',
    width: 152,
  },
  {
    dataIndex: 'sizeSpec',
    key: 'sizeSpec',
    title: '尺寸',
    width: 96,
  },
  {
    dataIndex: 'actualSizeSpec',
    key: 'actualSizeSpec',
    title: '实际尺寸',
    width: 96,
  },
  {
    dataIndex: 'actualReportModelCode',
    key: 'actualReportModelCode',
    title: '实际报工型号',
    width: 128,
  },
  { dataIndex: 'processName', key: 'processName', title: '工序', width: 92 },
  {
    children: [
      {
        dataIndex: 'inputCount',
        key: 'inputCount',
        title: '投入数',
        width: 82,
      },
      {
        dataIndex: 'outputGoodCount',
        key: 'outputGoodCount',
        title: '产出良品数',
        width: 100,
      },
      {
        dataIndex: 'outputNgCount',
        key: 'outputNgCount',
        title: '产出不良品数',
        width: 112,
      },
      {
        dataIndex: 'inspectionCount',
        key: 'inspectionCount',
        title: '送检数',
        width: 78,
      },
      { dataIndex: 'yieldRate', key: 'yieldRate', title: '良品率', width: 82 },
    ],
    key: 'reportMetrics',
    title: '实际报工指标',
  },
  {
    children: [
      {
        dataIndex: 'blackDotCount',
        key: 'blackDotCount',
        title: '黑点',
        width: 68,
      },
      {
        dataIndex: 'blueDotCount',
        key: 'blueDotCount',
        title: '蓝点',
        width: 68,
      },
      {
        dataIndex: 'yellowDotCount',
        key: 'yellowDotCount',
        title: '黄点',
        width: 68,
      },
      {
        dataIndex: 'redDotCount',
        key: 'redDotCount',
        title: '红点',
        width: 68,
      },
      {
        dataIndex: 'pinholeCount',
        key: 'pinholeCount',
        title: '针孔',
        width: 68,
      },
      {
        dataIndex: 'stripeCount',
        key: 'stripeCount',
        title: '条纹',
        width: 68,
      },
      {
        dataIndex: 'wrinkleCount',
        key: 'wrinkleCount',
        title: '褶皱',
        width: 68,
      },
      { dataIndex: 'waveCount', key: 'waveCount', title: '波浪纹', width: 76 },
      { dataIndex: 'otherCount', key: 'otherCount', title: '其他', width: 68 },
    ],
    key: 'defects',
    title: '自检不良分类',
  },
  {
    dataIndex: 'confirmTime',
    key: 'confirmTime',
    title: '实际报工时间',
    width: 164,
  },
  {
    dataIndex: 'defectSummary',
    key: 'defectSummary',
    title: '缺陷摘要',
    width: 180,
  },
  { key: 'source', title: '操作', width: 92 },
];

const detailTableScroll = computed(() => ({
  x: 2080,
  ...(detailRows.value.length > 0 ? { y: 360 } : {}),
}));

const activeDrillText = computed(() => {
  const context = activeDrill.value;
  if (!context) return '';
  const row = context.row;
  return [
    row.motherRollBatchNo || '-',
    row.segmentBatchNo || '-',
    context.processName,
    context.fieldLabel,
  ].join(' / ');
});

function rowKey(record: PivotRow) {
  return (
    record.pivotRowKey ||
    `${record.id || record.planNo || '-'}|${record.segmentBatchNo || '-'}|${modelText(record)}`
  );
}

function getDetailRowKey(record: DetailRow) {
  return record.sourceKey || `${record.processCode}|${record.sourceId}`;
}

function formatDetailValue(record: DetailRow, key: string) {
  const value = (record as unknown as Record<string, unknown>)[key];
  if (key === 'confirmTime') return formatBusinessDateTime(value);
  if (key === 'yieldRate') {
    return formatPercentValue(value);
  }
  if (
    key.endsWith('Count') ||
    ['inputCount', 'outputGoodCount', 'outputNgCount'].includes(key)
  ) {
    return formatNumber(value);
  }
  return String(value || '-');
}

function getDetailValueClass(record: DetailRow, key: string) {
  const value = Number(
    (record as unknown as Record<string, unknown>)[key] || 0,
  );
  if (
    value > 0 &&
    (key === 'outputNgCount' ||
      key.endsWith('DotCount') ||
      [
        'otherCount',
        'pinholeCount',
        'stripeCount',
        'waveCount',
        'wrinkleCount',
      ].includes(key))
  ) {
    return 'detail-value--warn';
  }
  if (key === 'yieldRate' && value > 0) return 'detail-value--rate';
  return '';
}

function resetDrill() {
  viewLevel.value = 'summary';
  activeDrill.value = undefined;
  detailRows.value = [];
  detailPagination.current = 1;
  detailPagination.total = 0;
}

async function openStageDetail(
  row: PivotRow,
  stageCode: string,
  fieldKey: StageFieldKey,
) {
  const stage = getStageDefinition(stageCode);
  const field = stage?.fields.find((item) => item.key === fieldKey);
  activeDrill.value = {
    fieldKey,
    fieldLabel: field?.label || '工序明细',
    processCode: normalizeStageProcessCode(stageCode),
    processName: stage?.label || stageCode,
    row,
  };
  detailPagination.current = 1;
  viewLevel.value = 'detail';
  await loadDetail();
}

async function loadDetail() {
  const context = activeDrill.value;
  if (!context) return;
  detailLoading.value = true;
  try {
    const row = context.row;
    const planLevel = ['FORMULA', 'WET'].includes(context.processCode);
    const modelCode = modelText(row);
    const page = await getYieldAnalysisV2DetailPage({
      ...buildParams(),
      metricKey: resolveDrillMetricKey(context.fieldKey),
      modelCode: modelCode === '-' ? query.modelCode : modelCode,
      motherRollBatchNo: row.motherRollBatchNo || query.motherRollBatchNo,
      motherSegmentBatchNo: planLevel
        ? undefined
        : row.segmentBatchNo || query.motherSegmentBatchNo,
      pageNo: detailPagination.current,
      pageSize: detailPagination.pageSize,
      planNo: row.planNo || query.planNo,
      processCode: context.processCode,
    });
    detailRows.value = page.list || [];
    detailPagination.total = Number(page.total || 0);
  } catch (error) {
    console.error(error);
    message.error('加载实际报工钻取明细失败');
  } finally {
    detailLoading.value = false;
  }
}

function handleDetailPageChange(nextPage: number, nextPageSize: number) {
  detailPagination.current = nextPage;
  detailPagination.pageSize = nextPageSize;
  void loadDetail();
}

function backToSummary() {
  resetDrill();
}

function openSource(record: DetailRow) {
  if (!record.sourceRoute) {
    message.warning('该报工记录暂无原始记录入口');
    return;
  }
  void router.push(record.sourceRoute);
}

async function loadData() {
  loading.value = true;
  try {
    const params = buildParams();
    const [listResult, overviewResult, previousOverviewResult] =
      await Promise.all([
        getYieldAnalysisV2List(params),
        getYieldAnalysisV2Overview(params),
        getYieldAnalysisV2Overview(buildPreviousParams()),
      ]);
    rows.value = listResult || [];
    total.value = rows.value.length;
    overview.value = overviewResult;
    previousOverview.value = previousOverviewResult;
    await nextTick();
    if (analysisExpanded.value) await renderCharts();
  } catch (error) {
    console.error(error);
    message.error('加载良品率分析（新版）失败');
  } finally {
    loading.value = false;
  }
}

async function loadModelOptions() {
  try {
    modelCodes.value = (await getYieldAnalysisV2TargetModelOptions()) || [];
  } catch (error) {
    console.warn('加载型号下拉失败', error);
  }
}

async function loadSyncStatus() {
  try {
    syncStatus.value = await getYieldAnalysisV2SyncStatus();
  } catch (error) {
    console.warn('加载日结数据时间失败', error);
  }
}

async function renderCharts() {
  const trendRows = overview.value?.trendRows || [];
  await renderTrendChart({
    color: ['#2563eb', '#16a34a', '#dc2626', '#059669'],
    grid: { bottom: 28, containLabel: true, left: 16, right: 32, top: 58 },
    legend: { left: 'center', top: 4 },
    series: [
      {
        barMaxWidth: 26,
        data: trendRows.map((item) => item.inputTotal),
        name: '投入数',
        type: 'bar',
      },
      {
        barMaxWidth: 26,
        data: trendRows.map((item) => item.outputGoodTotal),
        name: '产出良品数',
        type: 'bar',
      },
      {
        barMaxWidth: 26,
        data: trendRows.map((item) => item.outputNgTotal),
        name: '产出不良品数',
        type: 'bar',
      },
      {
        data: trendRows.map((item) => item.yieldRate),
        name: '良品率',
        smooth: true,
        type: 'line',
        yAxisIndex: 1,
      },
    ],
    tooltip: { trigger: 'axis' },
    xAxis: { data: trendRows.map((item) => item.statDate), type: 'category' },
    yAxis: [
      { name: '数量', type: 'value' },
      {
        axisLabel: { formatter: '{value}%' },
        max: 100,
        name: '良品率',
        nameGap: 10,
        type: 'value',
      },
    ],
  });

  const defectRows = (overview.value?.defectDistribution || []).filter(
    (item) => Number(item.defectCount || 0) > 0,
  );
  await renderDefectChart({
    color: ['#4f6edb', '#b9df1f', '#4a4d69', '#f59e0b', '#ef4444', '#06b6d4'],
    legend: { bottom: 2, type: 'scroll' },
    series: [
      {
        center: ['50%', '46%'],
        data:
          defectRows.length > 0
            ? defectRows.map((item) => ({
                name: item.defectName,
                value: item.defectCount,
              }))
            : [{ name: '暂无缺陷', value: 0 }],
        label: { formatter: '{b}: {c}' },
        radius: ['48%', '72%'],
        type: 'pie',
      },
    ],
    tooltip: { trigger: 'item' },
  });
}

function handleQuery() {
  resetDrill();
  void loadData();
}

function handleShiftWeek(offset: number) {
  const [startDate, endDate] = query.dateRange?.length
    ? query.dateRange
    : naturalWeekRange();
  query.dateRange = [
    dayjs(startDate).add(offset, 'week').format('YYYY-MM-DD'),
    dayjs(endDate).add(offset, 'week').format('YYYY-MM-DD'),
  ];
  handleQuery();
}

function handleProcessChange() {
  handleQuery();
}

function handleReset() {
  Object.assign(query, {
    dateRange: naturalWeekRange(),
    materialKeyword: undefined,
    modelCode: undefined,
    motherRollBatchNo: undefined,
    motherSegmentBatchNo: undefined,
    pieceNo: undefined,
    planNo: undefined,
    processCode: 'ALL',
  });
  queryExpanded.value = false;
  resetDrill();
  void loadData();
}

async function handleSync() {
  syncing.value = true;
  try {
    const result = await syncYieldAnalysisV2();
    message.success(
      `最新状态同步完成：新增 ${result.insertedSourceCount} 条，更新 ${result.updatedSourceCount} 条，移除 ${result.removedSourceCount} 条，未变 ${result.alreadySettledRecordCount} 条，耗时 ${(Number(result.durationMs || 0) / 1000).toFixed(1)} 秒`,
    );
    resetDrill();
    await Promise.all([loadSyncStatus(), loadData()]);
  } catch (error) {
    console.error(error);
    message.error('日结同步失败，请检查服务端日志');
  } finally {
    syncing.value = false;
  }
}

async function toggleAnalysis() {
  analysisExpanded.value = !analysisExpanded.value;
  if (analysisExpanded.value) {
    await nextTick();
    await renderCharts();
  }
}

onMounted(() => {
  void Promise.all([loadModelOptions(), loadSyncStatus(), loadData()]);
});
</script>

<template>
  <Page class="yield-v2-page">
    <section class="query-card">
      <div class="query-primary">
        <div class="query-item query-date">
          <label>实际报工日期</label>
          <div class="week-range-control">
            <Button @click="handleShiftWeek(-1)">
              <template #icon>
                <IconifyIcon icon="lucide:chevron-left" />
              </template>
              上周
            </Button>
            <DatePicker.RangePicker
              v-model:value="query.dateRange"
              :allow-clear="false"
              value-format="YYYY-MM-DD"
            />
            <Button @click="handleShiftWeek(1)">
              下周
              <template #icon>
                <IconifyIcon icon="lucide:chevron-right" />
              </template>
            </Button>
          </div>
        </div>
        <div class="query-item">
          <label>工序</label>
          <Select
            v-model:value="query.processCode"
            :options="processOptions"
            @change="handleProcessChange"
          />
        </div>
        <div class="query-actions">
          <Button type="text" @click="queryExpanded = !queryExpanded">
            <template #icon>
              <IconifyIcon icon="lucide:sliders-horizontal" />
            </template>
            {{ queryExpanded ? '收起条件' : '更多条件' }}
          </Button>
          <Button type="primary" :loading="loading" @click="handleQuery">
            <template #icon><IconifyIcon icon="lucide:search" /></template>
            查询
          </Button>
          <Button :disabled="syncing" @click="handleReset">
            <template #icon><IconifyIcon icon="lucide:rotate-ccw" /></template>
            重置
          </Button>
          <Button :loading="syncing" @click="handleSync">
            <template #icon><IconifyIcon icon="lucide:refresh-cw" /></template>
            同步最新数据
          </Button>
        </div>
      </div>
      <div v-if="queryExpanded" class="query-more">
        <div class="query-item">
          <label>型号</label>
          <Select
            v-model:value="query.modelCode"
            allow-clear
            :options="modelOptions"
            placeholder="全部型号"
            show-search
          />
        </div>
        <div class="query-item">
          <label>母卷批号</label>
          <Input
            v-model:value="query.motherRollBatchNo"
            allow-clear
            placeholder="母批/生产/主批号"
            @press-enter="handleQuery"
          />
        </div>
        <div class="query-item">
          <label>分段批号</label>
          <Input
            v-model:value="query.motherSegmentBatchNo"
            allow-clear
            placeholder="分段批号"
            @press-enter="handleQuery"
          />
        </div>
        <div class="query-item">
          <label>片号</label>
          <Input
            v-model:value="query.pieceNo"
            allow-clear
            placeholder="片号"
            @press-enter="handleQuery"
          />
        </div>
        <div class="query-item">
          <label>计划号</label>
          <Input
            v-model:value="query.planNo"
            allow-clear
            placeholder="计划号"
            @press-enter="handleQuery"
          />
        </div>
        <div class="query-item">
          <label>物料/型号</label>
          <Input
            v-model:value="query.materialKeyword"
            allow-clear
            placeholder="物料编码、名称或型号"
            @press-enter="handleQuery"
          />
        </div>
      </div>
    </section>

    <section class="analysis-card">
      <button class="analysis-toggle" type="button" @click="toggleAnalysis">
        <span class="analysis-title">
          <IconifyIcon icon="lucide:chart-column" />
          指标卡与图表
        </span>
        <span class="analysis-summary">
          良品率 {{ overview?.overview?.yieldRate ?? 0 }}% / 实际报工
          {{ overview?.overview?.confirmedTotal ?? 0 }} 条
        </span>
        <IconifyIcon
          :icon="analysisExpanded ? 'lucide:chevron-up' : 'lucide:chevron-down'"
        />
      </button>
      <div v-if="analysisExpanded" class="analysis-body">
        <div class="metric-grid">
          <article
            v-for="item in metricItems"
            :key="item.title"
            class="metric-card"
          >
            <IconifyIcon
              class="metric-icon"
              :icon="item.icon"
              :style="{ color: item.color }"
            />
            <Statistic
              :suffix="item.suffix"
              :title="item.title"
              :value="item.value"
              :value-style="{
                color: item.color,
                fontSize: '22px',
                fontWeight: 700,
              }"
            />
            <span
              class="metric-trend"
              :class="item.trend.className"
              :title="item.trend.title"
            >
              <IconifyIcon :icon="item.trend.icon" />
              较上期
            </span>
          </article>
        </div>
        <div class="chart-grid">
          <article class="chart-card">
            <header>
              <IconifyIcon icon="lucide:chart-line" />投入产出与良品率趋势{{
                processTitleSuffix
              }}
            </header>
            <EchartsUI ref="trendChartRef" class="chart-host" />
          </article>
          <article class="chart-card">
            <header>
              <IconifyIcon icon="lucide:chart-pie" />主要缺陷分布{{
                processTitleSuffix
              }}
            </header>
            <EchartsUI ref="defectChartRef" class="chart-host" />
          </article>
        </div>
      </div>
    </section>

    <section class="table-card">
      <header class="table-head">
        <div class="table-head-title">
          <template v-if="viewLevel !== 'detail'">
            <IconifyIcon icon="lucide:table-2" />
            <span>母卷批号 / 分段汇总{{ processTitleSuffix }}</span>
          </template>
          <template v-else>
            <Button size="small" type="link" @click="backToSummary">
              <template #icon>
                <IconifyIcon icon="lucide:arrow-left" />
              </template>
              返回汇总
            </Button>
            <span>实际报工钻取明细</span>
            <Tag color="processing" :title="activeDrillText">
              {{ activeDrillText }}
            </Tag>
          </template>
        </div>
        <Tag
          class="settlement-status"
          :color="syncStatus?.latestSyncTime ? 'blue' : 'orange'"
          :title="syncStatusText"
        >
          <IconifyIcon icon="lucide:clock-3" />
          {{ syncStatusText }}
        </Tag>
        <div class="table-head-actions">
          <Tag v-if="viewLevel === 'summary'" color="blue">
            点击工序指标进入报工详情
          </Tag>
          <Tag v-if="viewLevel === 'summary'">共 {{ total }} 行</Tag>
          <Tag v-if="viewLevel === 'detail'">
            共 {{ detailPagination.total }} 条
          </Tag>
        </div>
      </header>
      <div class="table-host">
        <Table
          v-if="viewLevel === 'summary'"
          bordered
          :columns="tableColumns"
          :data-source="rows"
          :loading="loading"
          :locale="{ emptyText: '所选实际报工日期内暂无数据' }"
          :pagination="false"
          :row-key="rowKey"
          :scroll="summaryTableScroll"
          size="small"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="getStageColumnCode(column)">
              <button
                v-if="hasStageReport(record, getStageColumnCode(column))"
                class="stage-drill-link"
                type="button"
                @click="
                  openStageDetail(
                    record,
                    getStageColumnCode(column),
                    getStageColumnField(column),
                  )
                "
              >
                {{
                  formatStageValue(
                    record,
                    getStageColumnCode(column),
                    getStageColumnField(column),
                  )
                }}
              </button>
              <span v-else>
                {{
                  formatStageValue(
                    record,
                    getStageColumnCode(column),
                    getStageColumnField(column),
                  )
                }}
              </span>
            </template>
            <span
              v-else-if="String(column.key || '') === 'sizeSpec'"
              class="size-lines"
            >
              <span
                v-for="(size, index) in sizeLines(record)"
                :key="`${size}-${index}`"
              >
                {{ size }}
              </span>
            </span>
            <template v-else>
              {{ formatBaseValue(record, String(column.key || '')) }}
            </template>
          </template>
        </Table>
        <Table
          v-else
          bordered
          :columns="detailColumns"
          :data-source="detailRows"
          :loading="detailLoading"
          :locale="{ emptyText: '暂无对应实际报工明细' }"
          :pagination="false"
          :row-key="getDetailRowKey"
          :scroll="detailTableScroll"
          size="small"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="String(column.key || '') === 'source'">
              <Button size="small" type="link" @click="openSource(record)">
                原始记录
              </Button>
            </template>
            <span
              v-else
              :class="getDetailValueClass(record, String(column.key || ''))"
            >
              {{ formatDetailValue(record, String(column.key || '')) }}
            </span>
          </template>
        </Table>
      </div>
      <footer
        v-if="viewLevel === 'detail' && detailPagination.total > 0"
        class="table-footer"
      >
        <Pagination
          v-model:current="detailPagination.current"
          v-model:page-size="detailPagination.pageSize"
          :page-size-options="['10', '20', '50', '100']"
          :show-total="(value: number) => `共 ${value} 条`"
          show-size-changer
          :total="detailPagination.total"
          @change="handleDetailPageChange"
        />
      </footer>
    </section>
  </Page>
</template>

<style scoped>
.yield-v2-page {
  --panel-border: #d9e2ef;
  --panel-bg: #fff;
}

.analysis-card,
.table-card {
  border: 1px solid var(--panel-border);
  background: var(--panel-bg);
  border-radius: 6px;
  box-shadow: 0 1px 2px rgb(15 23 42 / 4%);
}

.query-card {
  padding: 4px 2px;
}

.query-primary {
  display: grid;
  min-width: 0;
  align-items: center;
  grid-template-columns: minmax(360px, 1.2fr) minmax(230px, 0.8fr) minmax(
      360px,
      auto
    );
  gap: 12px;
}

.query-more {
  display: grid;
  min-width: 0;
  padding-top: 12px;
  grid-template-columns: repeat(3, minmax(260px, 1fr));
  gap: 12px;
}

.query-item {
  display: grid;
  min-width: 0;
  align-items: center;
  grid-template-columns: 82px minmax(0, 1fr);
}

.query-date {
  grid-template-columns: 96px minmax(0, 1fr);
}

.week-range-control {
  display: grid;
  min-width: 0;
  align-items: center;
  grid-template-columns: auto minmax(250px, 1fr) auto;
  gap: 6px;
}

.week-range-control :deep(.ant-btn) {
  min-width: 68px;
  height: 34px;
}

.week-range-control :deep(.ant-picker) {
  width: 100%;
}

.query-item label {
  display: flex;
  min-height: 34px;
  align-items: center;
  justify-content: flex-end;
  padding-right: 10px;
  color: #334155;
  font-size: 13px;
  font-weight: 500;
  white-space: nowrap;
}

.query-item > :not(label) {
  min-width: 0;
}

.query-item :deep(.ant-input),
.query-item :deep(.ant-input-affix-wrapper),
.query-item :deep(.ant-picker),
.query-item :deep(.ant-select),
.query-item :deep(.ant-select-selector) {
  width: 100%;
  min-height: 34px;
}

.query-actions {
  display: flex;
  min-width: 0;
  align-items: center;
  justify-self: end;
  justify-content: flex-end;
  gap: 8px;
}

.query-actions :deep(.ant-btn) {
  min-width: 88px;
  height: 34px;
}

.settlement-status {
  display: inline-flex;
  max-width: 340px;
  min-height: 28px;
  align-items: center;
  gap: 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.analysis-card,
.table-card {
  margin-top: 12px;
}

.analysis-toggle {
  display: grid;
  width: 100%;
  min-height: 44px;
  align-items: center;
  padding: 0 14px;
  border: 0;
  background: #fff;
  color: #334155;
  cursor: pointer;
  grid-template-columns: auto 1fr auto;
  text-align: left;
}

.analysis-title,
.table-head-title,
.chart-card header {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  font-weight: 600;
}

.analysis-summary {
  padding: 0 18px;
  color: #64748b;
  text-align: right;
}

.analysis-body {
  max-height: min(450px, 50vh);
  padding: 12px;
  border-top: 1px solid #edf2f7;
  overflow: auto;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(150px, 1fr));
  gap: 10px;
}

.metric-card {
  position: relative;
  display: flex;
  min-height: 92px;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border: 1px solid #dbe5f1;
  border-radius: 5px;
  background: #fff;
}

.metric-icon {
  width: 28px;
  height: 28px;
}

.metric-trend {
  position: absolute;
  top: 9px;
  right: 10px;
  display: inline-flex;
  align-items: center;
  gap: 3px;
  font-size: 11px;
  line-height: 1;
}

.metric-trend--up {
  color: #16a34a;
}

.metric-trend--down {
  color: #dc2626;
}

.metric-trend--stable {
  color: #64748b;
}

.chart-grid {
  display: grid;
  margin-top: 10px;
  grid-template-columns: minmax(0, 1.7fr) minmax(320px, 1fr);
  gap: 10px;
}

.chart-card {
  min-width: 0;
  padding: 10px 12px 0;
  border: 1px solid #dbe5f1;
  border-radius: 5px;
}

.chart-card header {
  height: 26px;
  color: #334155;
}

.chart-host {
  height: 250px;
}

.table-card {
  overflow: hidden;
}

.table-head {
  display: grid;
  min-height: 44px;
  align-items: center;
  gap: 10px;
  grid-template-columns: auto minmax(240px, 1fr) auto;
  padding: 0 14px;
  border-bottom: 1px solid #e5eaf1;
}

.table-head-title,
.table-head-actions {
  display: inline-flex;
  min-width: 0;
  align-items: center;
  gap: 8px;
}

.table-head-title > span,
.table-head-title :deep(.ant-tag) {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.table-head-actions {
  justify-content: flex-end;
}

.table-head .settlement-status {
  justify-self: start;
}

.table-host {
  min-width: 0;
  overflow: hidden;
}

.table-host :deep(.ant-table-thead > tr > th) {
  padding: 7px 6px;
  background: #edf4fc;
  color: #334155;
  font-size: 12px;
  text-align: center;
  white-space: nowrap;
}

.table-host :deep(.ant-table-tbody > tr > td) {
  padding: 6px;
  font-size: 12px;
  text-align: center;
  white-space: nowrap;
}

.table-host :deep(.ant-table-tbody > tr:hover > td) {
  background: #f3f8ff !important;
}

.size-lines {
  display: inline-flex;
  flex-direction: column;
  line-height: 1.5;
  white-space: normal;
}

.stage-drill-link {
  width: 100%;
  min-height: 30px;
  padding: 0 4px;
  border: 0;
  background: transparent;
  color: inherit;
  cursor: pointer;
  font: inherit;
}

.stage-drill-link:hover {
  background: #eaf3ff;
  color: #1677ff;
}

.detail-value--warn {
  color: #dc2626;
  font-weight: 600;
}

.detail-value--rate {
  color: #15803d;
  font-weight: 600;
}

.table-footer {
  display: flex;
  min-height: 50px;
  align-items: center;
  justify-content: flex-end;
  padding: 8px 14px;
  border-top: 1px solid #e5eaf1;
}

@media (max-width: 1400px) {
  .query-primary {
    grid-template-columns: minmax(500px, 1.2fr) minmax(210px, 0.8fr);
  }

  .query-actions {
    grid-column: 1 / -1;
    justify-self: end;
  }

  .query-more {
    grid-template-columns: repeat(2, minmax(260px, 1fr));
  }

  .metric-grid {
    grid-template-columns: repeat(3, minmax(160px, 1fr));
  }
}

@media (max-width: 900px) {
  .query-primary,
  .query-more,
  .metric-grid,
  .chart-grid {
    grid-template-columns: 1fr;
  }

  .query-actions {
    grid-column: 1 / -1;
    justify-self: end;
  }

  .analysis-summary {
    display: none;
  }
}
</style>
