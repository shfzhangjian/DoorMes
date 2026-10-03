<script lang="ts" setup>
import type { EchartsUIType } from '@vben/plugins/echarts';
import type { MesQmsYieldAnalysisApi } from '#/api/mes/quality/statistics/yield-analysis';

import { computed, nextTick, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { Page, useVbenModal } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';
import { EchartsUI, useEcharts } from '@vben/plugins/echarts';
import { downloadFileFromBlobPart } from '@vben/utils';

import dayjs from 'dayjs';
import {
  Button,
  DatePicker,
  Form,
  Input,
  message,
  Pagination,
  Segmented,
  Select,
  Statistic,
  Table,
  Tag,
} from 'ant-design-vue';

import {
  exportYieldAnalysisExcel,
  getYieldAnalysisDetailPage,
  getYieldAnalysisOverview,
  getYieldAnalysisTargetModelOptions,
} from '#/api/mes/quality/statistics/yield-analysis';
import InspectionDetailModalForm from '#/views/mes/quality/shared/inspection-detail-modal.vue';

defineOptions({ name: 'MesQmsYieldAnalysis' });

type DetailRow = MesQmsYieldAnalysisApi.DetailRow;
type OverviewResult = MesQmsYieldAnalysisApi.OverviewResult;
type TableDisplayMode = 'grid' | 'property';
type ViewLevel = 'detail' | 'summary';
type ChartFocusType = 'date' | 'defect';

interface SummaryPivotRow extends Record<string, number | string> {
  groupKey: string;
  modelCode: string;
  motherRollNo: string;
  segmentNo: string;
}

interface SummaryPropertyRow extends Record<string, unknown> {
  metricKey: string;
  metricName: string;
  modelCode: string;
  motherRollNo: string;
  processCode: string;
  processName: string;
  rowKey: string;
  segmentNo: string;
  sourceRow: SummaryPivotRow;
}

interface DetailPropertyRow extends Record<string, unknown> {
  pieceNo: string;
  processName: string;
  propertyKey: string;
  propertyName: string;
  rowKey: string;
  segmentNo: string;
  sourceRow: DetailRow;
}

interface ChartFocus {
  label: string;
  params: Partial<MesQmsYieldAnalysisApi.QueryParams>;
  type: ChartFocusType;
  value: string;
}

interface YieldTableColumn extends Record<string, unknown> {
  className?: string;
  dataIndex?: string;
  detailDefect?: boolean;
  detailMetric?: boolean;
  drillable?: boolean;
  fixed?: 'left';
  key: string;
  metricKey?: string;
  processCode?: string;
  title: string;
  width?: number;
}

interface MetricColumn {
  dataIndex: string;
  drillable?: boolean;
  title: string;
  width: number;
}

const FINAL_INSPECTION_PROCESS = 'FINAL_INSPECTION';

const defectColumns = [
  { dataIndex: 'blackDotCount', title: '黑点' },
  { dataIndex: 'blueDotCount', title: '蓝点' },
  { dataIndex: 'yellowDotCount', title: '黄点' },
  { dataIndex: 'redDotCount', title: '红点' },
  { dataIndex: 'pinholeCount', title: '针孔' },
  { dataIndex: 'stripeCount', title: '条纹' },
  { dataIndex: 'wrinkleCount', title: '褶皱' },
  { dataIndex: 'waveCount', title: '波浪纹' },
  { dataIndex: 'otherCount', title: '其他' },
];

const processDefinitions = [
  { label: '配料', value: 'FORMULA' },
  { label: '湿法', value: 'WET' },
  { label: '磨皮', value: 'ROUGH_GRINDING' },
  { label: '粘胶1', value: 'ADHESIVE1' },
  { label: '分切', value: 'SLITTING' },
  { label: '压槽', value: 'PRESS_SLOT' },
  { label: '粘胶2', value: 'ADHESIVE2' },
  { label: '裁切', value: 'CUT_ROUND' },
  { label: '终检', value: FINAL_INSPECTION_PROCESS },
];

const processOptions = [
  { label: '全部工序', value: 'ALL' },
  ...processDefinitions,
];

const tableDisplayModeOptions = [
  { label: '横向表格', value: 'grid' },
  { label: '属性行', value: 'property' },
];

const earlyProcessCodes = new Set([
  'FORMULA',
  'WET',
  'ROUGH_GRINDING',
  'ADHESIVE1',
]);

const earlyProcessSummaryMetricColumns: MetricColumn[] = [
  { dataIndex: 'inputTotal', title: '投入数', width: 74 },
  { dataIndex: 'outputGoodTotal', title: '产出良品数', width: 96 },
  { dataIndex: 'yieldRate', title: '良品率', width: 82 },
  { dataIndex: 'targetQualifiedQty', title: '目标合格', width: 86 },
  { dataIndex: 'targetAchievementRate', title: '目标达成率', width: 96 },
  { dataIndex: 'targetDifference', title: '目标差异', width: 86 },
  { dataIndex: 'outputNgTotal', title: '产出不良品数', width: 118 },
];

const summaryMetricColumns: MetricColumn[] = [
  { dataIndex: 'inputTotal', title: '投入数', width: 74 },
  { dataIndex: 'outputGoodTotal', title: '产出良品数', width: 96 },
  { dataIndex: 'yieldRate', title: '良品率', width: 82 },
  { dataIndex: 'targetQualifiedQty', title: '目标合格', width: 86 },
  { dataIndex: 'targetAchievementRate', title: '目标达成率', width: 96 },
  { dataIndex: 'targetDifference', title: '目标差异', width: 86 },
  { dataIndex: 'outputNgTotal', title: '产出不良品数', width: 118 },
  ...defectColumns.map((column) => ({ ...column, width: 66 })),
  {
    dataIndex: 'inspectionTotal',
    title: '送检次数',
    width: 76,
  },
];

const finalInspectionSummaryMetricColumns: MetricColumn[] = [
  { dataIndex: 'inputTotal', title: '投入数', width: 74 },
  { dataIndex: 'outputGoodTotal', title: '产出良品数', width: 96 },
  { dataIndex: 'yieldRate', title: '良品率', width: 82 },
  { dataIndex: 'targetQualifiedQty', title: '目标合格', width: 86 },
  { dataIndex: 'targetAchievementRate', title: '目标达成率', width: 96 },
  { dataIndex: 'targetDifference', title: '目标差异', width: 86 },
  { dataIndex: 'outputNgTotal', title: '产出不良品数', width: 118 },
  {
    dataIndex: 'inspectionTotal',
    title: '送检片数',
    width: 76,
  },
  {
    dataIndex: 'defectSummary',
    drillable: false,
    title: '缺陷码汇总',
    width: 180,
  },
];

const detailInfoColumns = [
  { dataIndex: 'segmentNo', title: '分段', width: 120 },
  { dataIndex: 'pieceNo', title: '片号', width: 150 },
  { dataIndex: 'processName', title: '工序', width: 92 },
];

const detailMetricColumns = [
  { dataIndex: 'inputCount', title: '投入数', width: 74 },
  { dataIndex: 'outputGoodCount', title: '产出良品数', width: 96 },
  { dataIndex: 'outputNgCount', title: '产出不良品数', width: 118 },
];

const detailDefectColumns = defectColumns.map((column) => ({
  ...column,
  width: 76,
}));

const detailTailColumns = [
  { dataIndex: 'inspectionCount', title: '送检次数', width: 86 },
  { dataIndex: 'yieldRate', title: '良品率', width: 82 },
  { dataIndex: 'confirmTime', title: '扫码确认时间', width: 170 },
  { dataIndex: 'defectSummary', title: '缺陷摘要', width: 180 },
  { dataIndex: 'source', title: '操作', width: 92 },
];

const finalInspectionDetailTailColumns = [
  { dataIndex: 'inspectionCount', title: '送检片数', width: 86 },
  { dataIndex: 'yieldRate', title: '良品率', width: 82 },
  { dataIndex: 'confirmTime', title: '扫码确认时间', width: 170 },
  { dataIndex: 'defectSummary', title: '终检缺陷码', width: 180 },
  { dataIndex: 'source', title: '操作', width: 92 },
];

const warningMetricKeys = new Set([
  'outputNgTotal',
  'outputNgCount',
  'inspectionTotal',
  'inspectionCount',
  ...defectColumns.map((column) => column.dataIndex),
]);

const targetMetricKeys = new Set([
  'targetAchievementRate',
  'targetDifference',
  'targetQualifiedQty',
]);

const router = useRouter();
const loading = ref(false);
const tableLoading = ref(false);
const detailLoading = ref(false);
const trendChartRef = ref<EchartsUIType>();
const defectChartRef = ref<EchartsUIType>();
const { renderEcharts: renderTrendChart } = useEcharts(trendChartRef);
const { renderEcharts: renderDefectChart } = useEcharts(defectChartRef);
const [InspectionDetailModal, inspectionDetailModalApi] = useVbenModal({
  connectedComponent: InspectionDetailModalForm,
  destroyOnClose: true,
});

const query = reactive({
  dateRange: [
    dayjs().subtract(1, 'day').format('YYYY-MM-DD'),
    dayjs().subtract(1, 'day').format('YYYY-MM-DD'),
  ] as string[],
  keyword: '',
  materialKeyword: '',
  modelCode: '',
  motherRollNo: '',
  planNo: '',
  processCode: 'ALL',
  segmentNo: '',
});

const overview = ref<OverviewResult>(createEmptyOverview());
const tableOverview = ref<OverviewResult>(createEmptyOverview());
const modelOptions = ref<{ label: string; value: string }[]>([]);
const chartFocus = ref<ChartFocus>();
const analysisExpanded = ref(false);

const viewLevel = ref<ViewLevel>('summary');
const activeSummary = ref<SummaryPivotRow>();
const activeProcessCode = ref('');
const activeMetricKey = ref('');
const detailRows = ref<DetailRow[]>([]);
const tableDisplayMode = ref<TableDisplayMode>('grid');
const detailPagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
});

const selectedYieldTargetText = computed(() => {
  const target = overview.value.overview;
  if (!query.modelCode || !query.processCode || query.processCode === 'ALL') {
    return '';
  }
  const targetValue = Number(target?.targetQualifiedQty || 0);
  if (targetValue <= 0) {
    return '';
  }
  const unit = target?.targetUnit || '';
  const processName =
    target?.targetProcessName || getProcessLabel(query.processCode);
  const modelCode = target?.targetModelCode || query.modelCode;
  return `目标：${formatDecimal(targetValue)}${unit} / ${modelCode} / ${processName}`;
});

const metricItems = computed(() => [
  {
    color: '#166534',
    icon: 'lucide:badge-check',
    subtitle: selectedYieldTargetText.value,
    title: '良品率',
    value: overview.value.overview?.yieldRate ?? 0,
    suffix: '%',
  },
  {
    color: '#0f172a',
    icon: 'lucide:scan-line',
    title: '投入数',
    value: overview.value.overview?.inputTotal ?? 0,
  },
  {
    color: '#166534',
    icon: 'lucide:package-check',
    title: '产出良品数',
    value: overview.value.overview?.outputGoodTotal ?? 0,
  },
  {
    color: '#b91c1c',
    icon: 'lucide:octagon-alert',
    title: '产出不良品数',
    value: overview.value.overview?.outputNgTotal ?? 0,
  },
  {
    color: '#7c2d12',
    icon: 'lucide:pie-chart',
    title: '主要缺陷',
    value: overview.value.overview?.primaryDefectCount ?? 0,
    subtitle: overview.value.overview?.primaryDefectName || '-',
  },
]);

const visibleProcesses = computed(() => {
  if (query.processCode && query.processCode !== 'ALL') {
    return processDefinitions.filter(
      (process) => process.value === query.processCode,
    );
  }
  return processDefinitions;
});

const summaryRows = computed(() => {
  return (tableOverview.value.segmentRows || []).map((segmentRow) => {
    const row: SummaryPivotRow = {
      groupKey:
        segmentRow.groupKey ||
        `${segmentRow.modelCode || '-'}|${segmentRow.motherRollNo || '-'}|${segmentRow.segmentNo || '-'}`,
      modelCode: segmentRow.modelCode || '-',
      motherRollNo: segmentRow.motherRollNo || '-',
      segmentNo: segmentRow.segmentNo || '-',
    };
    for (const metric of segmentRow.processMetrics || []) {
      for (const column of getSummaryMetricColumns(metric.processCode)) {
        const rawValue = (metric as unknown as Record<string, unknown>)[
          column.dataIndex
        ];
        row[buildProcessField(metric.processCode, column.dataIndex)] =
          column.dataIndex === 'defectSummary'
            ? String(rawValue || '-')
            : Number(rawValue) || 0;
      }
      row[buildProcessField(metric.processCode, 'targetMatched')] =
        metric.targetMatched ? 1 : 0;
      row[buildProcessField(metric.processCode, 'targetReached')] =
        metric.targetReached === false ? 0 : 1;
      row[buildProcessField(metric.processCode, 'targetType')] =
        metric.targetType || '';
      row[buildProcessField(metric.processCode, 'targetUnit')] =
        metric.targetUnit || '';
    }
    return row;
  });
});

const summaryTableScrollX = computed(() => {
  const metricWidth = visibleProcesses.value.reduce(
    (processTotal, process) =>
      processTotal +
      getSummaryMetricColumns(process.value).reduce(
        (columnTotal, column) => columnTotal + Number(column.width || 0),
        0,
      ),
    0,
  );
  return 86 + 130 + 130 + metricWidth;
});

const detailTableScrollX = computed(() => {
  return sumLeafColumnWidths(detailTableColumns.value);
});

const tableScrollY = 'calc(100vh - 430px)';

const summaryTableScroll = computed(() => ({
  x: summaryTableScrollX.value,
  y: tableScrollY,
}));

const detailTableScroll = computed(() => ({
  x: detailTableScrollX.value,
  y: tableScrollY,
}));

const propertyTableScroll = computed(() => ({
  x: 760,
  y: tableScrollY,
}));

const summaryTableColumns = computed<YieldTableColumn[]>(() => [
  {
    align: 'center',
    className: 'yield-table-model-cell',
    dataIndex: 'modelCode',
    fixed: 'left',
    key: 'modelCode',
    title: '产品型号',
    width: 86,
  },
  {
    dataIndex: 'motherRollNo',
    fixed: 'left',
    key: 'motherRollNo',
    title: '母卷批号',
    width: 130,
  },
  {
    dataIndex: 'segmentNo',
    fixed: 'left',
    key: 'segmentNo',
    title: '分段',
    width: 130,
  },
  ...visibleProcesses.value.map((process) => ({
    children: getSummaryMetricColumns(process.value).map((column) => ({
      dataIndex: buildProcessField(process.value, column.dataIndex),
      drillable: column.drillable !== false,
      key: `${process.value}-${column.dataIndex}`,
      metricKey: column.dataIndex,
      processCode: process.value,
      title: column.title,
      width: column.width,
    })),
    className: 'yield-table-group-title',
    key: process.value,
    title: process.label,
  })),
]);

const summaryPropertyColumns = computed<YieldTableColumn[]>(() => [
  {
    dataIndex: 'modelCode',
    key: 'modelCode',
    title: '产品型号',
    width: 86,
  },
  {
    dataIndex: 'motherRollNo',
    key: 'motherRollNo',
    title: '母卷批号',
    width: 130,
  },
  {
    dataIndex: 'segmentNo',
    key: 'segmentNo',
    title: '分段',
    width: 130,
  },
  {
    dataIndex: 'processName',
    key: 'processName',
    title: '工序',
    width: 92,
  },
  {
    className: 'yield-property-name-cell',
    dataIndex: 'metricName',
    key: 'metricName',
    title: '属性',
    width: 160,
  },
  {
    className: 'yield-property-value-cell',
    dataIndex: 'metricValue',
    key: 'metricValue',
    title: '值',
    width: 150,
  },
]);

const summaryPropertyRows = computed<SummaryPropertyRow[]>(() => {
  const rows: SummaryPropertyRow[] = [];
  for (const summaryRow of summaryRows.value) {
    for (const process of visibleProcesses.value) {
      if (!hasSummaryProcessMetric(summaryRow, process.value)) {
        continue;
      }
      for (const column of getSummaryMetricColumns(process.value)) {
        rows.push({
          metricKey: column.dataIndex,
          metricName: column.title,
          modelCode: summaryRow.modelCode,
          motherRollNo: summaryRow.motherRollNo,
          processCode: process.value,
          processName: process.label,
          rowKey: `${summaryRow.groupKey}-${process.value}-${column.dataIndex}`,
          segmentNo: summaryRow.segmentNo,
          sourceRow: summaryRow,
        });
      }
    }
  }
  return rows;
});

const detailTableColumns = computed<YieldTableColumn[]>(() => {
  const columns: YieldTableColumn[] = [
    {
      dataIndex: 'segmentNo',
      fixed: 'left',
      key: 'segmentNo',
      title: '分段',
      width: 120,
    },
    {
      dataIndex: 'pieceNo',
      fixed: 'left',
      key: 'pieceNo',
      title: '片号',
      width: 150,
    },
    {
      dataIndex: 'processName',
      fixed: 'left',
      key: 'processName',
      title: '工序',
      width: 92,
    },
    {
      children: detailMetricColumns.map((column) => ({
        dataIndex: column.dataIndex,
        detailMetric: true,
        key: column.dataIndex,
        title: column.title,
        width: column.width,
      })),
      className: 'yield-table-group-title',
      key: 'pieceMetrics',
      title: '片号指标',
    },
  ];

  if (shouldShowDetailDefectColumns(activeProcessCode.value)) {
    columns.push({
      children: detailDefectColumns.map((column) => ({
        dataIndex: column.dataIndex,
        detailDefect: true,
        key: column.dataIndex,
        title: column.title,
        width: column.width,
      })),
      className: 'yield-table-group-title',
      key: 'selfCheckDefects',
      title: '自检不良品分类',
    });
  }

  columns.push(
    ...getDetailTailColumns().map((column) => ({
      dataIndex: column.dataIndex,
      key: column.dataIndex,
      title: column.title,
      width: column.width,
    })),
  );
  return columns;
});

const detailPropertyColumns = computed<YieldTableColumn[]>(() => [
  {
    dataIndex: 'segmentNo',
    key: 'segmentNo',
    title: '分段',
    width: 130,
  },
  {
    dataIndex: 'pieceNo',
    key: 'pieceNo',
    title: '片号',
    width: 160,
  },
  {
    dataIndex: 'processName',
    key: 'processName',
    title: '工序',
    width: 92,
  },
  {
    className: 'yield-property-name-cell',
    dataIndex: 'propertyName',
    key: 'propertyName',
    title: '属性',
    width: 160,
  },
  {
    className: 'yield-property-value-cell',
    dataIndex: 'propertyValue',
    key: 'propertyValue',
    title: '值',
    width: 180,
  },
]);

const detailPropertyRows = computed<DetailPropertyRow[]>(() => {
  const propertyColumns = [
    ...detailMetricColumns,
    ...(shouldShowDetailDefectColumns(activeProcessCode.value)
      ? detailDefectColumns
      : []),
    ...getDetailTailColumns(),
  ];
  return detailRows.value.flatMap((row) =>
    propertyColumns.map((column) => ({
      pieceNo: row.pieceNo || '-',
      processName: row.processName || '-',
      propertyKey: column.dataIndex,
      propertyName: column.title,
      rowKey: `${getDetailRowKey(row)}-${column.dataIndex}`,
      segmentNo: row.segmentNo || '-',
      sourceRow: row,
    })),
  );
});

const activeProcessName = computed(() =>
  activeProcessCode.value
    ? getProcessLabel(activeProcessCode.value)
    : '全部工序',
);

const activeMetricName = computed(() =>
  activeMetricKey.value ? getMetricTitle(activeMetricKey.value) : '',
);

const activeMetricConditionText = computed(() => {
  if (!activeMetricName.value) {
    return '';
  }
  return activeMetricKey.value === 'yieldRate'
    ? `${activeMetricName.value}：有投入记录`
    : `${activeMetricName.value} >= 1`;
});

const tableBusy = computed(() => loading.value || tableLoading.value);

onMounted(async () => {
  await Promise.all([loadTargetModelOptions(), loadOverview()]);
});

function createEmptyOverview(): OverviewResult {
  return {
    defectDistribution: [],
    formulaExample: '',
    formulaText: '',
    overview: {
      confirmedTotal: 0,
      goodTotal: 0,
      inputTotal: 0,
      inspectionTotal: 0,
      ngTotal: 0,
      outputGoodTotal: 0,
      outputNgTotal: 0,
      primaryDefectCount: 0,
      primaryDefectName: '-',
      selfCheckNgTotal: 0,
      submissionNgTotal: 0,
      targetModelCode: '',
      targetProcessName: '',
      targetQualifiedQty: 0,
      targetType: '',
      targetUnit: '',
      yieldRate: 0,
    },
    segmentRows: [],
    summaryRows: [],
    trendRows: [],
  };
}

async function loadTargetModelOptions() {
  try {
    const data = await getYieldAnalysisTargetModelOptions();
    modelOptions.value = (data || [])
      .filter(Boolean)
      .map((value) => ({ label: value, value }));
  } catch {
    modelOptions.value = [];
  }
}

function filterModelOption(input: string, option: any) {
  return String(option?.label || '')
    .toLowerCase()
    .includes(input.toLowerCase());
}

async function loadOverview() {
  loading.value = true;
  try {
    const data = await getYieldAnalysisOverview(buildParams());
    overview.value = data;
    tableOverview.value = data;
    chartFocus.value = undefined;
    viewLevel.value = 'summary';
    activeSummary.value = undefined;
    activeProcessCode.value = '';
    activeMetricKey.value = '';
    detailRows.value = [];
    detailPagination.current = 1;
    detailPagination.total = 0;
    await renderChartsIfExpanded();
  } finally {
    loading.value = false;
  }
}

async function loadFocusedTable() {
  if (!chartFocus.value) {
    tableOverview.value = overview.value;
    return;
  }
  tableLoading.value = true;
  try {
    tableOverview.value = await getYieldAnalysisOverview(
      buildParams(chartFocus.value.params),
    );
  } finally {
    tableLoading.value = false;
  }
}

function buildParams(extra: Record<string, any> = {}) {
  const [startDate, endDate] = query.dateRange || [];
  return cleanParams({
    endDate,
    materialKeyword: query.materialKeyword,
    modelCode: query.modelCode,
    motherRollNo: query.motherRollNo,
    processCode: query.processCode,
    segmentNo: query.segmentNo,
    startDate,
    ...extra,
  });
}

function cleanParams(params: Record<string, any>) {
  return Object.fromEntries(
    Object.entries(params).filter(
      ([, value]) => value !== '' && value !== undefined && value !== null,
    ),
  );
}

async function toggleAnalysisExpanded() {
  analysisExpanded.value = !analysisExpanded.value;
  await renderChartsIfExpanded();
}

async function renderChartsIfExpanded() {
  if (!analysisExpanded.value) {
    return;
  }
  await nextTick();
  await Promise.all([drawTrendChart(), drawDefectChart()]);
}

async function drawTrendChart() {
  const rows = overview.value.trendRows || [];
  const chart = await renderTrendChart({
    grid: { bottom: 24, containLabel: true, left: 8, right: 16, top: 42 },
    legend: { right: 8, top: 4 },
    tooltip: { trigger: 'axis' },
    xAxis: {
      axisLabel: { color: '#64748b' },
      data: rows.map((row) => row.statDate),
      type: 'category',
    },
    yAxis: [
      { axisLabel: { color: '#64748b' }, name: '数量', type: 'value' },
      {
        axisLabel: { color: '#64748b', formatter: '{value}%' },
        max: 100,
        min: 0,
        name: '良品率',
        type: 'value',
      },
    ],
    series: [
      {
        barMaxWidth: 22,
        data: rows.map((row) => row.inputTotal ?? row.confirmedTotal),
        itemStyle: { color: '#2563eb' },
        name: '投入数',
        type: 'bar',
      },
      {
        barMaxWidth: 22,
        data: rows.map(
          (row) =>
            row.outputGoodTotal ??
            Math.max(
              Number(row.confirmedTotal || 0) - Number(row.ngTotal || 0),
              0,
            ),
        ),
        itemStyle: { color: '#16a34a' },
        name: '产出良品数',
        type: 'bar',
      },
      {
        barMaxWidth: 22,
        data: rows.map((row) => row.outputNgTotal ?? row.ngTotal),
        itemStyle: { color: '#dc2626' },
        name: '产出不良品数',
        type: 'bar',
      },
      {
        data: rows.map((row) => row.yieldRate),
        itemStyle: { color: '#16a34a' },
        name: '良品率',
        smooth: true,
        type: 'line',
        yAxisIndex: 1,
      },
    ],
  });
  chart?.off('click');
  chart?.on('click', handleTrendChartClick);
}

async function drawDefectChart() {
  const rows = (overview.value.defectDistribution || []).filter(
    (row) => row.defectCount > 0,
  );
  const chart = await renderDefectChart({
    legend: { bottom: 0, type: 'scroll' },
    series: [
      {
        avoidLabelOverlap: true,
        data: rows.length
          ? rows.map((row) => ({
              name: row.defectName,
              value: row.defectCount,
            }))
          : [{ name: '暂无缺陷', value: 0 }],
        label: { formatter: '{b}: {c}' },
        name: '缺陷分布',
        radius: ['42%', '68%'],
        type: 'pie',
      },
    ],
    tooltip: { trigger: 'item' },
  });
  chart?.off('click');
  chart?.on('click', handleDefectChartClick);
}

function handleTrendChartClick(params: any) {
  if (params?.componentType !== 'series') return;
  const row = (overview.value.trendRows || [])[Number(params?.dataIndex)];
  const statDate = String(params?.name || row?.statDate || '');
  if (!statDate) return;
  void toggleChartFocus({
    label: `统计日期：${statDate}`,
    params: { statDate },
    type: 'date',
    value: statDate,
  });
}

function handleDefectChartClick(params: any) {
  if (params?.componentType !== 'series') return;
  const defectName = String(params?.name || '');
  if (!defectName || defectName === '暂无缺陷') return;
  void toggleChartFocus({
    label: `缺陷类型：${defectName}`,
    params: { defectName },
    type: 'defect',
    value: defectName,
  });
}

async function toggleChartFocus(nextFocus: ChartFocus) {
  if (
    chartFocus.value?.type === nextFocus.type &&
    chartFocus.value?.value === nextFocus.value
  ) {
    clearChartFocus();
    return;
  }
  chartFocus.value = nextFocus;
  resetTableView();
  await loadFocusedTable();
}

function clearChartFocus() {
  if (!chartFocus.value) return;
  chartFocus.value = undefined;
  tableOverview.value = overview.value;
  resetTableView();
}

function resetTableView() {
  viewLevel.value = 'summary';
  activeSummary.value = undefined;
  activeProcessCode.value = '';
  activeMetricKey.value = '';
  detailRows.value = [];
  detailPagination.current = 1;
  detailPagination.total = 0;
}

function handleQuery() {
  loadOverview();
}

function handleReset() {
  query.dateRange = [
    dayjs().subtract(1, 'day').format('YYYY-MM-DD'),
    dayjs().subtract(1, 'day').format('YYYY-MM-DD'),
  ];
  query.keyword = '';
  query.materialKeyword = '';
  query.modelCode = '';
  query.motherRollNo = '';
  query.planNo = '';
  query.processCode = 'ALL';
  query.segmentNo = '';
  loadOverview();
}

function shiftDateRange(days: number) {
  const [startDate, endDate] = query.dateRange || [];
  const fallback = dayjs().subtract(1, 'day');
  query.dateRange = [
    dayjs(startDate || fallback).add(days, 'day').format('YYYY-MM-DD'),
    dayjs(endDate || startDate || fallback).add(days, 'day').format('YYYY-MM-DD'),
  ];
  loadOverview();
}

async function handleExport() {
  const data = await exportYieldAnalysisExcel(
    buildParams(chartFocus.value?.params || {}),
  );
  downloadFileFromBlobPart({ fileName: '良品率分析.xls', source: data });
}

function openDetail(record: SummaryPivotRow, processCode = '', metricKey = '') {
  activeSummary.value = record;
  activeProcessCode.value = processCode;
  activeMetricKey.value = metricKey;
  detailPagination.current = 1;
  viewLevel.value = 'detail';
  loadDetail();
}

async function loadDetail() {
  if (!activeSummary.value) return;
  detailLoading.value = true;
  try {
    const page = await getYieldAnalysisDetailPage(
      buildParams({
        ...(chartFocus.value?.params || {}),
        modelCode: activeSummary.value.modelCode,
        motherRollNo: activeSummary.value.motherRollNo,
        pageNo: detailPagination.current,
        pageSize: detailPagination.pageSize,
        metricKey: activeMetricKey.value,
        processCode: activeProcessCode.value || query.processCode,
        segmentNo: activeSummary.value.segmentNo,
      }),
    );
    detailRows.value = page.list || [];
    detailPagination.total = Number(page.total || 0);
  } finally {
    detailLoading.value = false;
  }
}

function handleDetailPageChange(page: number, pageSize?: number) {
  detailPagination.current = page || 1;
  detailPagination.pageSize = pageSize || detailPagination.pageSize;
  loadDetail();
}

function backToSummary() {
  viewLevel.value = 'summary';
  activeSummary.value = undefined;
  activeProcessCode.value = '';
  activeMetricKey.value = '';
}

function buildProcessField(processCode: string, metricKey: string) {
  return `${processCode}__${metricKey}`;
}

function getSummaryMetricColumns(processCode = '') {
  if (processCode === FINAL_INSPECTION_PROCESS) {
    return finalInspectionSummaryMetricColumns;
  }
  return earlyProcessCodes.has(processCode)
    ? earlyProcessSummaryMetricColumns
    : summaryMetricColumns;
}

function hasSummaryProcessMetric(record: SummaryPivotRow, processCode: string) {
  return getSummaryMetricColumns(processCode).some((column) =>
    Object.prototype.hasOwnProperty.call(
      record,
      buildProcessField(processCode, column.dataIndex),
    ),
  );
}

function getDetailTailColumns() {
  return activeProcessCode.value === FINAL_INSPECTION_PROCESS
    ? finalInspectionDetailTailColumns
    : detailTailColumns;
}

function shouldShowDetailDefectColumns(processCode: string) {
  return (
    processCode !== FINAL_INSPECTION_PROCESS && !earlyProcessCodes.has(processCode)
  );
}

function getSummaryPropertyDataIndex(record: SummaryPropertyRow) {
  return buildProcessField(record.processCode, record.metricKey);
}

function getSummaryPropertyRowClassName(record: SummaryPropertyRow) {
  return isTargetNotReached(record.sourceRow, record.processCode)
    ? 'yield-row--not-reached'
    : '';
}

function isDetailPropertyMetricKey(propertyKey: string) {
  return detailMetricColumns.some((column) => column.dataIndex === propertyKey);
}

function isDetailPropertyDefectKey(propertyKey: string) {
  return detailDefectColumns.some((column) => column.dataIndex === propertyKey);
}

function getDetailPropertyValueClass(record: DetailPropertyRow) {
  const value = getCellValue(record.sourceRow, record.propertyKey);
  if (isDetailPropertyMetricKey(record.propertyKey)) {
    return getMetricCountClass(record.propertyKey, value);
  }
  if (
    isDetailPropertyDefectKey(record.propertyKey) ||
    record.propertyKey === 'inspectionCount'
  ) {
    return getCountClass(value);
  }
  if (record.propertyKey === 'yieldRate') {
    return getYieldClass(value);
  }
  if (record.propertyKey === 'defectSummary') {
    return 'yield-detail-summary';
  }
  return '';
}

function getDetailPropertyValueText(record: DetailPropertyRow) {
  const value = getCellValue(record.sourceRow, record.propertyKey);
  if (
    isDetailPropertyMetricKey(record.propertyKey) ||
    isDetailPropertyDefectKey(record.propertyKey) ||
    record.propertyKey === 'inspectionCount'
  ) {
    return formatCount(value);
  }
  if (record.propertyKey === 'yieldRate') {
    return Number(value || 0) > 0 ? `${value}%` : '-';
  }
  return formatPlainCell(record.sourceRow, record.propertyKey);
}

function sumLeafColumnWidths(columns: YieldTableColumn[]): number {
  return columns.reduce((total, column) => {
    const children = column.children as YieldTableColumn[] | undefined;
    if (children?.length) {
      return total + sumLeafColumnWidths(children);
    }
    return total + Number(column.width || 0);
  }, 0);
}

function getMetricKey(dataIndex: unknown) {
  const value = String(dataIndex || '');
  return value.includes('__') ? value.split('__').pop() || '' : value;
}

function getProcessCodeFromField(dataIndex: unknown) {
  const value = String(dataIndex || '');
  return value.includes('__') ? value.split('__')[0] : '';
}

function isYieldColumn(dataIndex: unknown) {
  return getMetricKey(dataIndex) === 'yieldRate';
}

function getInputForMetric(record: SummaryPivotRow, dataIndex: unknown) {
  const processCode = getProcessCodeFromField(dataIndex);
  return Number(record[buildProcessField(processCode, 'inputTotal')] || 0);
}

function getCellValue(record: DetailRow | SummaryPivotRow, dataIndex: unknown) {
  return (record as Record<string, unknown>)[String(dataIndex)];
}

function getColumnDataIndex(column: Record<string, unknown>) {
  const dataIndex = column.dataIndex;
  if (Array.isArray(dataIndex)) return dataIndex.join('.');
  return typeof dataIndex === 'string' || typeof dataIndex === 'number'
    ? String(dataIndex)
    : '';
}

function getColumnMetricKey(column: Record<string, unknown>) {
  return typeof column.metricKey === 'string' ? column.metricKey : '';
}

function getColumnProcessCode(column: Record<string, unknown>) {
  return typeof column.processCode === 'string' ? column.processCode : '';
}

function isSummaryMetricColumn(column: Record<string, unknown>) {
  return Boolean(getColumnProcessCode(column) && getColumnMetricKey(column));
}

function isDetailMetricColumn(column: Record<string, unknown>) {
  return column.detailMetric === true;
}

function isDetailDefectColumn(column: Record<string, unknown>) {
  return column.detailDefect === true;
}

function formatPlainCell(
  record: DetailRow | SummaryPivotRow,
  dataIndex: unknown,
) {
  const value = getCellValue(record, dataIndex);
  return value === undefined || value === null || value === ''
    ? '-'
    : String(value);
}

function getProcessLabel(processCode: string) {
  return (
    processDefinitions.find((process) => process.value === processCode)
      ?.label ||
    processCode ||
    '-'
  );
}

function getMetricTitle(metricKey: string) {
  return (
    getSummaryMetricColumns(activeProcessCode.value).find(
      (column) => column.dataIndex === metricKey,
    )?.title ||
    summaryMetricColumns.find((column) => column.dataIndex === metricKey)
      ?.title ||
    finalInspectionSummaryMetricColumns.find(
      (column) => column.dataIndex === metricKey,
    )?.title ||
    detailMetricColumns.find((column) => column.dataIndex === metricKey)
      ?.title ||
    defectColumns.find((column) => column.dataIndex === metricKey)?.title ||
    getDetailTailColumns().find((column) => column.dataIndex === metricKey)
      ?.title ||
    detailTailColumns.find((column) => column.dataIndex === metricKey)?.title ||
    metricKey
  );
}

function canDrillMetric(
  record: SummaryPivotRow,
  processCode: string,
  metricKey: string,
) {
  if (!metricKey || metricKey === 'defectSummary') {
    return false;
  }
  if (targetMetricKeys.has(metricKey)) {
    return false;
  }
  if (metricKey === 'yieldRate') {
    return (
      Number(record[buildProcessField(processCode, 'inputTotal')] || 0) > 0
    );
  }
  return Number(record[buildProcessField(processCode, metricKey)] || 0) > 0;
}

function getSummaryMetricText(record: SummaryPivotRow, dataIndex: string) {
  const metricKey = getMetricKey(dataIndex);
  if (metricKey === 'defectSummary') {
    return formatPlainCell(record, dataIndex);
  }
  if (targetMetricKeys.has(metricKey)) {
    return getTargetMetricText(record, dataIndex, metricKey);
  }
  if (isYieldColumn(dataIndex)) {
    return getInputForMetric(record, dataIndex) > 0
      ? `${getCellValue(record, dataIndex)}%`
      : '-';
  }
  return formatCount(getCellValue(record, dataIndex));
}

function getSummaryMetricClass(record: SummaryPivotRow, dataIndex: string) {
  const metricKey = getMetricKey(dataIndex);
  if (metricKey === 'defectSummary') {
    return 'yield-detail-summary';
  }
  if (targetMetricKeys.has(metricKey)) {
    return isTargetNotReached(record, getProcessCodeFromField(dataIndex))
      ? 'yield-target--bad'
      : 'yield-count';
  }
  if (isYieldColumn(dataIndex)) {
    return getYieldClass(getCellValue(record, dataIndex));
  }
  return getMetricCountClass(dataIndex, getCellValue(record, dataIndex));
}

function getTargetMetricText(
  record: SummaryPivotRow,
  dataIndex: string,
  metricKey: string,
) {
  const processCode = getProcessCodeFromField(dataIndex);
  if (!isTargetMatched(record, processCode)) {
    return '-';
  }
  const value = Number(getCellValue(record, dataIndex) || 0);
  const unit = getTargetUnit(record, processCode);
  if (metricKey === 'targetAchievementRate') {
    return `${value}%`;
  }
  if (metricKey === 'targetDifference') {
    return `${value > 0 ? '+' : ''}${value}${unit}`;
  }
  return `${formatDecimal(value)}${unit}`;
}

function formatDecimal(value: number) {
  return Number.isInteger(value) ? String(value) : String(value);
}

function isTargetMatched(record: SummaryPivotRow, processCode: string) {
  return Number(record[buildProcessField(processCode, 'targetMatched')] || 0) > 0;
}

function isTargetNotReached(record: SummaryPivotRow, processCode: string) {
  return (
    isTargetMatched(record, processCode) &&
    Number(record[buildProcessField(processCode, 'targetReached')] || 0) <= 0
  );
}

function getTargetUnit(record: SummaryPivotRow, processCode: string) {
  const targetType = String(record[buildProcessField(processCode, 'targetType')] || '');
  if (targetType === 'YIELD_RATE') {
    return '%';
  }
  return String(record[buildProcessField(processCode, 'targetUnit')] || '');
}

function getSummaryRowClassName(record: SummaryPivotRow) {
  return visibleProcesses.value.some((process) =>
    isTargetNotReached(record, process.value),
  )
    ? 'yield-row--not-reached'
    : '';
}

function getMetricCountClass(dataIndex: unknown, value: unknown) {
  return warningMetricKeys.has(getMetricKey(dataIndex))
    ? getCountClass(value)
    : 'yield-count';
}

function formatCount(value: unknown) {
  const count = Number(value || 0);
  return count > 0 ? count : '-';
}

function getCountClass(value: unknown) {
  return Number(value || 0) > 0
    ? 'yield-count yield-count--warn'
    : 'yield-count';
}

function getYieldClass(value: unknown) {
  const rate = Number(value || 0);
  if (rate >= 98) return 'yield-rate yield-rate--good';
  if (rate >= 95) return 'yield-rate yield-rate--watch';
  return 'yield-rate yield-rate--bad';
}

function openSubmissionNgInspection(row: DetailRow) {
  if (Number(row.inspectionCount || 0) <= 0) {
    return;
  }
  if (!row.inspectionId || !row.inspectionSourceType) {
    message.warning('未找到关联的送检单');
    return;
  }
  inspectionDetailModalApi
    .setData({
      inspectionId: row.inspectionId,
      inspectionNo: row.inspectionNo,
      ngOnly: false,
      sourceType: row.inspectionSourceType,
      title: `${row.processName || '过程'}送检详情`,
    })
    .open();
}

function formatDetailTotal(total: number) {
  return `共 ${total} 条`;
}

function getDetailRowKey(row: DetailRow) {
  return row.sourceKey || `${row.sourceTable}:${row.sourceId}`;
}

function openSource(row: DetailRow) {
  router.push({
    path: '/mes/quality/statistics/yield-analysis/source-preview',
    query: {
      processCode: row.processCode,
      sourceId: row.sourceId,
      sourceTable: row.sourceTable,
    },
  });
}
</script>

<template>
  <Page auto-content-height class="yield-page">
    <div class="yield-toolbar">
      <Form class="yield-query-form" layout="inline">
        <div class="yield-query-grid">
          <Form.Item class="yield-query-item yield-query-item--date" label="统计日期">
            <div class="yield-date-field">
              <DatePicker.RangePicker
                v-model:value="query.dateRange"
                allow-clear
                value-format="YYYY-MM-DD"
              />
            </div>
          </Form.Item>
          <Form.Item class="yield-query-item" label="工序">
            <Select
              v-model:value="query.processCode"
              :options="processOptions"
              :list-height="360"
              class="yield-filter"
            />
          </Form.Item>
          <Form.Item class="yield-query-item" label="型号">
            <Select
              v-model:value="query.modelCode"
              allow-clear
              show-search
              :filter-option="filterModelOption"
              :options="modelOptions"
              class="yield-filter"
              placeholder="请选择型号"
            />
          </Form.Item>
          <Form.Item class="yield-query-item" label="母卷批号">
            <Input
              v-model:value="query.motherRollNo"
              allow-clear
              class="yield-filter"
              placeholder="请输入母卷批号"
            />
          </Form.Item>
          <Form.Item class="yield-query-item" label="分段">
            <Input
              v-model:value="query.segmentNo"
              allow-clear
              class="yield-filter"
              placeholder="请输入分段"
            />
          </Form.Item>
          <Form.Item class="yield-query-actions">
            <Button
              aria-label="前一天"
              shape="circle"
              title="前一天"
              @click="shiftDateRange(-1)"
            >
              <template #icon>
                <IconifyIcon icon="lucide:chevron-left" />
              </template>
            </Button>
            <Button
              aria-label="后一天"
              shape="circle"
              title="后一天"
              @click="shiftDateRange(1)"
            >
              <template #icon>
                <IconifyIcon icon="lucide:chevron-right" />
              </template>
            </Button>
            <Button type="primary" :loading="loading" @click="handleQuery">
              <template #icon><IconifyIcon icon="lucide:search" /></template>
              查询
            </Button>
            <Button @click="handleReset">
              <template #icon><IconifyIcon icon="lucide:rotate-ccw" /></template>
              重置
            </Button>
            <Button @click="handleExport">
              <template #icon><IconifyIcon icon="lucide:download" /></template>
              导出
            </Button>
          </Form.Item>
        </div>
      </Form>
    </div>

    <div class="yield-content">
      <section class="yield-insight-panel">
        <button
          class="yield-insight-toggle"
          type="button"
          @click="toggleAnalysisExpanded"
        >
          <span class="yield-insight-toggle__title">
            <IconifyIcon icon="lucide:chart-column" />
            <span>指标卡与图表</span>
          </span>
          <span class="yield-insight-toggle__metrics">
            良品率 {{ overview.overview?.yieldRate ?? 0 }}% / 投入
            {{ overview.overview?.inputTotal ?? 0 }} / 产出良品
            {{ overview.overview?.outputGoodTotal ?? 0 }}
          </span>
          <IconifyIcon
            :icon="
              analysisExpanded ? 'lucide:chevron-up' : 'lucide:chevron-down'
            "
          />
        </button>

        <div v-if="analysisExpanded" class="yield-insight-body">
          <div class="yield-metrics">
            <div
              v-for="item in metricItems"
              :key="item.title"
              class="metric-panel"
            >
              <div class="metric-panel__icon" :style="{ color: item.color }">
                <IconifyIcon :icon="item.icon" />
              </div>
              <div>
                <Statistic
                  :suffix="item.suffix"
                  :title="item.title"
                  :value="item.value"
                  :value-style="{
                    color: item.color,
                    fontWeight: 700,
                    fontSize: '22px',
                  }"
                />
                <div v-if="item.subtitle" class="metric-panel__sub">
                  {{ item.subtitle }}
                </div>
              </div>
            </div>
          </div>

          <div class="yield-charts">
            <section class="yield-panel yield-panel--trend">
              <div class="yield-panel__head">
                <IconifyIcon icon="lucide:chart-line" />
                <span>投入产出与良品率趋势</span>
              </div>
              <EchartsUI ref="trendChartRef" class="yield-chart" />
            </section>
            <section class="yield-panel">
              <div class="yield-panel__head">
                <IconifyIcon icon="lucide:chart-pie" />
                <span>主要缺陷分布</span>
              </div>
              <EchartsUI ref="defectChartRef" class="yield-chart" />
            </section>
          </div>
        </div>
      </section>

      <section class="yield-panel yield-table-panel">
        <div class="yield-panel__head">
          <IconifyIcon icon="lucide:table-2" />
          <template v-if="viewLevel === 'summary'">
            <span>母卷批号 / 分段汇总</span>
            <Tag v-if="chartFocus" class="yield-focus-tag" color="processing">
              当前聚焦：{{ chartFocus.label }}
            </Tag>
            <Button
              v-if="chartFocus"
              size="small"
              type="link"
              @click="clearChartFocus"
            >
              取消聚焦
            </Button>
          </template>
          <template v-else>
            <Button size="small" type="link" @click="backToSummary">
              <template #icon
                ><IconifyIcon icon="lucide:arrow-left"
              /></template>
              返回上级
            </Button>
            <span>片号详情</span>
            <Tag v-if="activeSummary" color="blue">
              {{ activeSummary.modelCode }} / {{ activeSummary.motherRollNo }}
              / {{ activeSummary.segmentNo }} / {{ activeProcessName }}
            </Tag>
            <Tag v-if="activeMetricConditionText" color="green">
              {{ activeMetricConditionText }}
            </Tag>
            <Tag v-if="chartFocus" class="yield-focus-tag" color="processing">
              当前聚焦：{{ chartFocus.label }}
            </Tag>
            <Button
              v-if="chartFocus"
              size="small"
              type="link"
              @click="clearChartFocus"
            >
              取消聚焦
            </Button>
          </template>
          <div class="yield-table-head-actions">
            <em v-if="viewLevel === 'summary'">点击工序指标进入片号详情</em>
            <Segmented
              v-model:value="tableDisplayMode"
              :options="tableDisplayModeOptions"
              size="small"
            />
          </div>
        </div>
        <div v-if="viewLevel === 'summary'" class="yield-pivot-host">
          <Table
            v-if="tableDisplayMode === 'grid'"
            bordered
            class="yield-erp-table yield-erp-table--summary"
            :columns="summaryTableColumns"
            :data-source="summaryRows"
            :loading="tableBusy"
            :locale="{ emptyText: '暂无良品率统计数据' }"
            :pagination="false"
            :row-class-name="getSummaryRowClassName"
            row-key="groupKey"
            :scroll="summaryTableScroll"
            size="small"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="isSummaryMetricColumn(column)">
                <button
                  v-if="
                    canDrillMetric(
                      record,
                      getColumnProcessCode(column),
                      getColumnMetricKey(column),
                    )
                  "
                  class="yield-pivot-link"
                  type="button"
                  @click="
                    openDetail(
                      record,
                      getColumnProcessCode(column),
                      getColumnMetricKey(column),
                    )
                  "
                >
                  <span
                    :class="
                      getSummaryMetricClass(
                        record,
                        getColumnDataIndex(column),
                      )
                    "
                  >
                    {{
                      getSummaryMetricText(record, getColumnDataIndex(column))
                    }}
                  </span>
                </button>
                <span
                  v-else
                  :class="
                    getSummaryMetricClass(record, getColumnDataIndex(column))
                  "
                >
                  {{ getSummaryMetricText(record, getColumnDataIndex(column)) }}
                </span>
              </template>
              <template v-else>
                {{ formatPlainCell(record, getColumnDataIndex(column)) }}
              </template>
            </template>
          </Table>
          <Table
            v-else
            bordered
            class="yield-erp-table yield-property-table"
            :columns="summaryPropertyColumns"
            :data-source="summaryPropertyRows"
            :loading="tableBusy"
            :locale="{ emptyText: '暂无良品率统计数据' }"
            :pagination="false"
            :row-class-name="getSummaryPropertyRowClassName"
            row-key="rowKey"
            :scroll="propertyTableScroll"
            size="small"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="getColumnDataIndex(column) === 'metricValue'">
                <button
                  v-if="
                    canDrillMetric(
                      record.sourceRow,
                      record.processCode,
                      record.metricKey,
                    )
                  "
                  class="yield-pivot-link yield-property-link"
                  type="button"
                  @click="
                    openDetail(
                      record.sourceRow,
                      record.processCode,
                      record.metricKey,
                    )
                  "
                >
                  <span
                    :class="
                      getSummaryMetricClass(
                        record.sourceRow,
                        getSummaryPropertyDataIndex(record),
                      )
                    "
                  >
                    {{
                      getSummaryMetricText(
                        record.sourceRow,
                        getSummaryPropertyDataIndex(record),
                      )
                    }}
                  </span>
                </button>
                <span
                  v-else
                  :class="
                    getSummaryMetricClass(
                      record.sourceRow,
                      getSummaryPropertyDataIndex(record),
                    )
                  "
                >
                  {{
                    getSummaryMetricText(
                      record.sourceRow,
                      getSummaryPropertyDataIndex(record),
                    )
                  }}
                </span>
              </template>
              <template v-else>
                {{ formatPlainCell(record, getColumnDataIndex(column)) }}
              </template>
            </template>
          </Table>
        </div>

        <div v-else class="yield-pivot-host yield-detail-host">
          <Table
            v-if="tableDisplayMode === 'grid'"
            bordered
            class="yield-erp-table yield-erp-table--detail"
            :columns="detailTableColumns"
            :data-source="detailRows"
            :loading="detailLoading"
            :locale="{ emptyText: '暂无片号详情' }"
            :pagination="false"
            :row-key="getDetailRowKey"
            :scroll="detailTableScroll"
            size="small"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="isDetailMetricColumn(column)">
                <span
                  :class="
                    getMetricCountClass(
                      getColumnDataIndex(column),
                      getCellValue(record, getColumnDataIndex(column)),
                    )
                  "
                >
                  {{
                    formatCount(getCellValue(record, getColumnDataIndex(column)))
                  }}
                </span>
              </template>
              <template v-else-if="isDetailDefectColumn(column)">
                <span
                  :class="
                    getCountClass(
                      getCellValue(record, getColumnDataIndex(column)),
                    )
                  "
                >
                  {{
                    formatCount(getCellValue(record, getColumnDataIndex(column)))
                  }}
                </span>
              </template>
              <template
                v-else-if="getColumnDataIndex(column) === 'inspectionCount'"
              >
                <button
                  v-if="Number(record.inspectionCount || 0) > 0"
                  class="yield-pivot-link yield-inspection-link"
                  type="button"
                  @click="openSubmissionNgInspection(record)"
                >
                  <span :class="getCountClass(record.inspectionCount)">
                    {{ formatCount(record.inspectionCount) }}
                  </span>
                </button>
                <span v-else :class="getCountClass(record.inspectionCount)">
                  {{ formatCount(record.inspectionCount) }}
                </span>
              </template>
              <template v-else-if="getColumnDataIndex(column) === 'yieldRate'">
                <span :class="getYieldClass(record.yieldRate)">
                  {{
                    Number(record.yieldRate || 0) > 0
                      ? `${record.yieldRate}%`
                      : '-'
                  }}
                </span>
              </template>
              <template v-else-if="getColumnDataIndex(column) === 'source'">
                <Button size="small" type="link" @click="openSource(record)">
                  原始记录
                </Button>
              </template>
              <template
                v-else-if="getColumnDataIndex(column) === 'defectSummary'"
              >
                <span class="yield-detail-summary">
                  {{ formatPlainCell(record, getColumnDataIndex(column)) }}
                </span>
              </template>
              <template v-else>
                {{ formatPlainCell(record, getColumnDataIndex(column)) }}
              </template>
            </template>
          </Table>
          <Table
            v-else
            bordered
            class="yield-erp-table yield-property-table"
            :columns="detailPropertyColumns"
            :data-source="detailPropertyRows"
            :loading="detailLoading"
            :locale="{ emptyText: '暂无片号详情' }"
            :pagination="false"
            row-key="rowKey"
            :scroll="propertyTableScroll"
            size="small"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="getColumnDataIndex(column) === 'propertyValue'">
                <template v-if="record.propertyKey === 'inspectionCount'">
                  <button
                    v-if="Number(record.sourceRow.inspectionCount || 0) > 0"
                    class="yield-pivot-link yield-inspection-link yield-property-link"
                    type="button"
                    @click="openSubmissionNgInspection(record.sourceRow)"
                  >
                    <span :class="getCountClass(record.sourceRow.inspectionCount)">
                      {{ formatCount(record.sourceRow.inspectionCount) }}
                    </span>
                  </button>
                  <span v-else :class="getCountClass(record.sourceRow.inspectionCount)">
                    {{ formatCount(record.sourceRow.inspectionCount) }}
                  </span>
                </template>
                <Button
                  v-else-if="record.propertyKey === 'source'"
                  size="small"
                  type="link"
                  @click="openSource(record.sourceRow)"
                >
                  原始记录
                </Button>
                <span v-else :class="getDetailPropertyValueClass(record)">
                  {{ getDetailPropertyValueText(record) }}
                </span>
              </template>
              <template v-else>
                {{ formatPlainCell(record, getColumnDataIndex(column)) }}
              </template>
            </template>
          </Table>
          <div class="yield-detail-pagination">
            <Pagination
              :current="detailPagination.current"
              :page-size="detailPagination.pageSize"
              :page-size-options="['10', '20', '50', '100']"
              :show-total="formatDetailTotal"
              :total="detailPagination.total"
              show-size-changer
              size="small"
              @change="handleDetailPageChange"
            />
          </div>
        </div>
      </section>

      <section class="yield-formula">
        <div>
          <b>良品计算公式</b>
          <span>{{ overview.formulaText }}</span>
        </div>
        <div>
          <b>示例</b>
          <span>{{ overview.formulaExample }}</span>
        </div>
      </section>
    </div>
    <InspectionDetailModal />
  </Page>
</template>

<style scoped>
.yield-page {
  height: 100%;
  background: #f6f8fb;
}

.yield-toolbar {
  border-bottom: 1px solid #dbe3ef;
  background: #ffffff;
  padding: 12px 16px 10px;
}

.yield-query-form {
  width: 100%;
}

.yield-query-form :deep(.ant-form-item) {
  margin: 0;
}

.yield-query-form :deep(.ant-form-item-label) {
  min-width: 58px;
  padding-right: 8px;
  text-align: right;
}

.yield-query-form :deep(.ant-form-item-label > label) {
  height: 32px;
  color: #475569;
  font-size: 13px;
}

.yield-query-grid {
  display: grid;
  width: 100%;
  align-items: center;
  gap: 10px 12px;
  grid-template-columns: repeat(3, minmax(260px, 1fr));
}

.yield-query-item {
  min-width: 0;
}

.yield-query-item :deep(.ant-form-item-control) {
  min-width: 0;
}

.yield-query-item :deep(.ant-form-item-control-input),
.yield-query-item :deep(.ant-form-item-control-input-content) {
  width: 100%;
}

.yield-query-item :deep(.ant-input),
.yield-query-item :deep(.ant-picker),
.yield-query-item :deep(.ant-select) {
  width: 100%;
}

.yield-query-item--date {
  grid-column: span 1;
}

.yield-date-field {
  display: flex;
  width: 100%;
  min-width: 0;
  align-items: center;
  gap: 8px;
}

.yield-date-field :deep(.ant-picker) {
  min-width: 0;
  flex: 1;
}

.yield-date-field .ant-btn {
  flex: 0 0 auto;
}

.yield-query-actions {
  grid-column: auto;
  justify-self: stretch;
}

.yield-query-actions :deep(.ant-form-item-control-input-content) {
  display: flex;
  width: 100%;
  justify-content: flex-end;
  gap: 8px;
}

.yield-filter {
  width: 100%;
}

.yield-content {
  display: flex;
  height: 100%;
  min-height: 0;
  flex: 1;
  flex-direction: column;
  gap: 12px;
  overflow: hidden;
  padding: 12px;
}

.yield-insight-panel {
  flex: 0 0 auto;
  border: 1px solid #dbe3ef;
  background: #ffffff;
}

.yield-insight-toggle {
  display: flex;
  width: 100%;
  min-height: 42px;
  align-items: center;
  gap: 12px;
  border: 0;
  background: #ffffff;
  color: #1e293b;
  cursor: pointer;
  font: inherit;
  padding: 0 12px;
  text-align: left;
}

.yield-insight-toggle:hover {
  background: #f8fbff;
}

.yield-insight-toggle__title {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
}

.yield-insight-toggle__metrics {
  margin-left: auto;
  color: #64748b;
  font-size: 12px;
}

.yield-insight-body {
  display: grid;
  gap: 12px;
  border-top: 1px solid #edf1f7;
  padding: 12px;
}

.yield-metrics {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 12px;
}

.metric-panel {
  display: flex;
  min-height: 86px;
  align-items: center;
  gap: 12px;
  border: 1px solid #dbe3ef;
  background: #ffffff;
  padding: 14px;
}

.metric-panel__icon {
  display: grid;
  width: 38px;
  height: 38px;
  place-items: center;
  border: 1px solid #dbe3ef;
  background: #f8fafc;
  font-size: 20px;
}

.metric-panel__sub {
  margin-top: 2px;
  color: #64748b;
  font-size: 12px;
}

.yield-charts {
  display: grid;
  min-height: 280px;
  grid-template-columns: minmax(0, 1.45fr) minmax(320px, 0.8fr);
  gap: 12px;
}

.yield-panel {
  min-width: 0;
  border: 1px solid #dbe3ef;
  background: #ffffff;
}

.yield-panel__head {
  display: flex;
  min-height: 42px;
  flex: 0 0 auto;
  align-items: center;
  gap: 8px;
  border-bottom: 1px solid #edf1f7;
  padding: 0 12px;
  color: #1e293b;
  font-weight: 600;
}

.yield-panel__head em {
  margin-left: auto;
  color: #64748b;
  font-size: 12px;
  font-style: normal;
  font-weight: 400;
}

.yield-table-head-actions {
  display: inline-flex;
  margin-left: auto;
  align-items: center;
  gap: 10px;
}

.yield-table-head-actions em {
  margin-left: 0;
}

.yield-focus-tag {
  max-width: 360px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.yield-chart {
  height: 236px;
  width: 100%;
}

.yield-table-panel {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  min-height: 320px;
}

.yield-pivot-host {
  position: relative;
  display: flex;
  min-height: 0;
  flex: 1 1 auto;
  flex-direction: column;
  overflow: hidden;
}

.yield-detail-host {
  min-height: 300px;
}

.yield-erp-table {
  display: flex;
  min-height: 0;
  flex: 1 1 auto;
  flex-direction: column;
  overflow: hidden;
}

.yield-erp-table :deep(.ant-spin-container),
.yield-erp-table :deep(.ant-spin-nested-loading) {
  display: flex;
  height: 100%;
  min-height: 0;
  flex: 1 1 auto;
  flex-direction: column;
}

.yield-erp-table :deep(.ant-table) {
  display: flex;
  height: 100%;
  min-height: 0;
  flex: 1 1 auto;
  flex-direction: column;
  background: #ffffff;
  color: #334155;
  font-size: 12px;
}

.yield-erp-table :deep(.ant-table-container) {
  display: flex;
  height: 100%;
  min-height: 0;
  flex: 1 1 auto;
  flex-direction: column;
  border-color: #dbe3ef;
}

.yield-erp-table :deep(.ant-table-header) {
  flex: 0 0 auto;
  overflow: hidden !important;
}

.yield-erp-table :deep(.ant-table-cell) {
  height: 36px;
  border-color: #e5edf6 !important;
  padding: 0 8px !important;
  text-align: center;
  white-space: nowrap;
}

.yield-erp-table :deep(.ant-table-thead > tr > th) {
  background: #f8fbff;
  color: #475569;
  font-weight: 600;
  text-align: center !important;
}

.yield-erp-table :deep(.ant-table-thead > tr > th.yield-table-group-title) {
  background: #eef6ff !important;
  color: #1d4ed8 !important;
}

.yield-erp-table :deep(.ant-table-cell-fix-left) {
  background: #ffffff;
  text-align: left;
}

.yield-erp-table :deep(.yield-table-model-cell) {
  text-align: center !important;
}

.yield-erp-table :deep(.yield-property-name-cell),
.yield-erp-table :deep(.yield-property-value-cell) {
  text-align: left !important;
}

.yield-erp-table :deep(.ant-table-thead .ant-table-cell-fix-left) {
  background: #f8fbff;
  text-align: center !important;
}

.yield-erp-table :deep(.ant-table-cell-fix-left-last::after) {
  box-shadow: inset 10px 0 8px -8px rgb(15 23 42 / 18%);
}

.yield-erp-table :deep(.ant-table-body) {
  height: auto !important;
  min-height: 0;
  flex: 1 1 auto;
  max-height: none !important;
  overflow: auto !important;
  scrollbar-width: thin;
}

.yield-erp-table :deep(.yield-row--not-reached > td) {
  background: #fff1f2 !important;
}

.yield-erp-table :deep(.yield-row--not-reached > td.ant-table-cell-fix-left) {
  background: #fff1f2 !important;
}

.yield-pivot-link {
  display: grid;
  width: 100%;
  height: 35px;
  place-items: center;
  border: 0;
  background: transparent;
  cursor: pointer;
  font: inherit;
}

.yield-pivot-link:hover {
  background: #eff6ff;
}

.yield-property-link {
  height: 32px;
  place-items: center start;
}

.yield-detail-summary {
  display: block;
  overflow: hidden;
  text-align: left !important;
  text-overflow: ellipsis;
}

.yield-detail-pagination {
  display: flex;
  min-height: 40px;
  flex: 0 0 auto;
  align-items: center;
  justify-content: flex-end;
  border-top: 1px solid #edf1f7;
  padding: 6px 10px;
}

.yield-count {
  color: #94a3b8;
}

.yield-count--warn {
  color: #dc2626;
  font-weight: 700;
}

.yield-target--bad {
  color: #dc2626;
  font-weight: 700;
}

.yield-rate {
  font-weight: 700;
}

.yield-rate--good {
  color: #15803d;
}

.yield-rate--watch {
  color: #b45309;
}

.yield-rate--bad {
  color: #b91c1c;
}

.yield-formula {
  display: grid;
  flex: 0 0 auto;
  gap: 6px;
  border: 1px solid #dbe3ef;
  background: #ffffff;
  padding: 12px;
  color: #334155;
  font-size: 13px;
}

.yield-formula div {
  display: flex;
  gap: 10px;
}

.yield-formula b {
  min-width: 84px;
  color: #0f172a;
}

.detail-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

@media (max-width: 1440px) {
  .yield-query-grid {
    grid-template-columns: repeat(3, minmax(220px, 1fr));
  }
}

@media (max-width: 1280px) {
  .yield-query-grid {
    grid-template-columns: repeat(2, minmax(260px, 1fr));
  }

  .yield-query-item--date {
    grid-column: auto;
  }

  .yield-query-actions {
    grid-column: 1 / -1;
  }
}

@media (max-width: 1100px) {
  .yield-query-grid {
    grid-template-columns: 1fr;
  }

  .yield-query-item--date {
    grid-column: auto;
  }

  .yield-date-field {
    flex-wrap: wrap;
  }

  .yield-date-field :deep(.ant-picker) {
    min-width: 100%;
  }

  .yield-query-actions :deep(.ant-form-item-control-input-content) {
    justify-content: flex-start;
  }

  .yield-metrics,
  .yield-charts {
    grid-template-columns: 1fr;
  }

  .yield-insight-toggle {
    align-items: flex-start;
    flex-direction: column;
    padding: 10px 12px;
  }

  .yield-insight-toggle__metrics {
    margin-left: 0;
  }
}
</style>
