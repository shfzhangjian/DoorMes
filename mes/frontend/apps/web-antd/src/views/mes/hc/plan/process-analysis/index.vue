<script lang="ts" setup>
import type { EchartsUIType } from '@vben/plugins/echarts';
import type { MesHcProcessAnalysisApi } from '#/api/mes/hc/process-analysis';
import type { MesQmsYieldAnalysisApi } from '#/api/mes/quality/statistics/yield-analysis';
import type {
  AnalysisConfig,
  AnalysisFilter,
  AnalysisFormula,
  AnalysisMetric,
  DrillStep,
  PivotCellColumn,
  PivotDisplayRow,
} from './types';

import {
  computed,
  nextTick,
  onBeforeUnmount,
  onMounted,
  reactive,
  ref,
  watch,
} from 'vue';

import { Page } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';
import { EchartsUI, useEcharts } from '@vben/plugins/echarts';
import { downloadFileFromBlobPart } from '@vben/utils';

import {
  Button,
  Checkbox,
  Empty,
  Input,
  InputNumber,
  message,
  Modal,
  RangePicker,
  Select,
  Spin,
  Table,
  Tag,
  Tooltip,
} from 'ant-design-vue';
import dayjs from 'dayjs';

import {
  createProcessAnalysisConfig,
  deleteProcessAnalysisConfig,
  getProcessAnalysisConfigList,
  getProcessAnalysisFieldCatalog,
  getProcessAnalysisSourceData,
  updateProcessAnalysisConfig,
} from '#/api/mes/hc/process-analysis';
import {
  getYieldAnalysisDetailPage,
  getYieldAnalysisOverview,
} from '#/api/mes/quality/statistics/yield-analysis';

import { PLAN_STATUS_OPTIONS } from '../plan-order/data';
import { FALLBACK_FIELD_CATALOG } from './field-catalog';
import {
  applyAnalysisFilters,
  buildChartData,
  buildPiecePivotFacts,
  buildPivotResult,
  filterDefectivePieceFacts,
  formatAnalysisValue,
  normalizeProcessPivotRows,
  validateFormulaExpression,
} from './pivot-engine';

defineOptions({ name: 'MesHcPlanProcessAnalysis' });

type FieldDefinition = MesHcProcessAnalysisApi.FieldDefinition;
type SavedConfig = MesHcProcessAnalysisApi.Config;
type AnalysisViewMode = 'BROWSE' | 'DESIGN';

interface ColumnWidthOption {
  defaultWidth: number;
  key: string;
  label: string;
}

interface WidthAwareColumn {
  children?: WidthAwareColumn[];
  width?: number;
}

const DEFAULT_TABLE_HEIGHT = 430;
const MIN_COLUMN_WIDTH = 60;
const MAX_COLUMN_WIDTH = 600;
const MIN_TABLE_HEIGHT = 240;
const MAX_TABLE_HEIGHT = 1000;

const AGGREGATION_OPTIONS = [
  { label: '按业务粒度求和', value: 'SUM_DISTINCT' },
  { label: '普通求和', value: 'SUM' },
  { label: '平均值', value: 'AVG' },
  { label: '最小值', value: 'MIN' },
  { label: '最大值', value: 'MAX' },
  { label: '计数', value: 'COUNT' },
  { label: '去重计数', value: 'COUNT_DISTINCT' },
];

const OPERATOR_LABELS: Record<string, string> = {
  BETWEEN: '区间',
  CONTAINS: '包含',
  EMPTY: '为空',
  EQ: '等于',
  GT: '大于',
  GTE: '大于等于',
  IN: '属于（逗号分隔）',
  LT: '小于',
  LTE: '小于等于',
  NE: '不等于',
  NOT_CONTAINS: '不包含',
  NOT_EMPTY: '非空',
};

const STAGE_STATUS_LABELS: Record<string, string> = {
  FINISHED: '已完成',
  NOT_STARTED: '未开始',
  PENDING: '待加工',
  RUNNING: '进行中',
};

const DEFAULT_VISIBLE_STAGE_NAMES = [
  '分切',
  '压槽',
  '粘胶2',
  '裁切',
  '发货检验',
];

const DEFECT_METRIC_DEFINITIONS = [
  { field: 'blackDotCount', id: 'm_black_dot', label: '黑点' },
  { field: 'blueDotCount', id: 'm_blue_dot', label: '蓝点' },
  { field: 'yellowDotCount', id: 'm_yellow_dot', label: '黄点' },
  { field: 'redDotCount', id: 'm_red_dot', label: '红点' },
  { field: 'pinholeCount', id: 'm_pinhole', label: '针孔' },
  { field: 'stripeCount', id: 'm_stripe', label: '条纹' },
  { field: 'wrinkleCount', id: 'm_wrinkle', label: '褶皱' },
  { field: 'waveCount', id: 'm_wave', label: '波浪纹' },
  { field: 'otherCount', id: 'm_other', label: '其他' },
] as const;

const loading = ref(false);
const pieceListLoading = ref(false);
const metadataLoading = ref(false);
const saving = ref(false);
const rawRows = ref<any[]>([]);
const qualitySummaryRows = ref<MesQmsYieldAnalysisApi.SummaryRow[]>([]);
const pieceQualityRows = ref<MesQmsYieldAnalysisApi.DetailRow[]>([]);
const fieldCatalog = ref<FieldDefinition[]>([...FALLBACK_FIELD_CATALOG]);
const savedConfigs = ref<SavedConfig[]>([]);
const currentConfigId = ref<number>();
const viewMode = ref<AnalysisViewMode>('BROWSE');
const fieldKeyword = ref('');
const drillPath = ref<DrillStep[]>([]);
const pieceListMode = ref(false);
const pieceNgOnly = ref(false);
const resultPanelRef = ref<HTMLElement>();
const autoTableHeight = ref(DEFAULT_TABLE_HEIGHT);
const chartRef = ref<EchartsUIType>();
const { renderEcharts } = useEcharts(chartRef);
let resultPanelResizeObserver: ResizeObserver | undefined;
let pieceQualityRequestKey = '';

const analysisConfig = ref<AnalysisConfig>(createDefaultConfig());

const saveModalVisible = ref(false);
const saveForm = reactive({
  configName: '',
  defaultFlag: false,
  remark: '',
  scopeType: 'PRIVATE' as 'PRIVATE' | 'SHARED',
});

const formulaModalVisible = ref(false);
const editingFormulaIndex = ref(-1);
const formulaForm = reactive<AnalysisFormula>({
  decimals: 2,
  expression: '',
  format: 'NUMBER',
  id: '',
  label: '',
});

const fieldMap = computed(
  () => new Map(fieldCatalog.value.map((field) => [field.code, field])),
);
const currentConfig = computed(() =>
  savedConfigs.value.find((item) => item.id === currentConfigId.value),
);
const isBrowseMode = computed(() => viewMode.value === 'BROWSE');
const normalizedFacts = computed(() =>
  normalizeProcessPivotRows(rawRows.value, qualitySummaryRows.value),
);
const filteredFacts = computed(() =>
  applyAnalysisFilters(normalizedFacts.value, analysisConfig.value.filters),
);
const pivotResult = computed(() =>
  buildPivotResult(filteredFacts.value, analysisConfig.value, drillPath.value),
);
const pieceListFacts = computed(() =>
  filteredFacts.value.filter((fact) =>
    drillPath.value.every(
      (step) => normalizeCellText(fact[step.field]) === step.value,
    ),
  ),
);
const piecePivotFacts = computed(() =>
  buildPiecePivotFacts(pieceListFacts.value, pieceQualityRows.value),
);
const visiblePiecePivotFacts = computed(() =>
  pieceNgOnly.value
    ? filterDefectivePieceFacts(piecePivotFacts.value)
    : piecePivotFacts.value,
);
const pieceColumnTemplateFacts = computed(() =>
  filteredFacts.value.filter((fact) =>
    drillPath.value
      .slice(0, -1)
      .every((step) => normalizeCellText(fact[step.field]) === step.value),
  ),
);
const piecePivotResult = computed(() =>
  buildPivotResult(
    visiblePiecePivotFacts.value,
    {
      ...analysisConfig.value,
      rowDimensions: ['pieceListNo'],
    },
    [],
    pieceColumnTemplateFacts.value,
  ),
);
const activePivotResult = computed(() =>
  pieceListMode.value ? piecePivotResult.value : pivotResult.value,
);
const currentDimensionLabel = computed(() =>
  pieceListMode.value
    ? '生产片号'
    : getFieldLabel(pivotResult.value.currentDimension),
);
const canDrillFurther = computed(
  () =>
    !pieceListMode.value &&
    Boolean(pivotResult.value.currentDimension) &&
    (pivotResult.value.currentDimension === 'segmentBatchNo' ||
      drillPath.value.length < analysisConfig.value.rowDimensions.length - 1),
);
const valueDefinitions = computed(() => [
  ...analysisConfig.value.metrics,
  ...analysisConfig.value.formulas,
]);
const chartValueOptions = computed(() =>
  valueDefinitions.value.map((item) => ({
    label: item.label,
    value: item.id,
  })),
);
const dimensionOptions = computed(() =>
  fieldCatalog.value
    .filter((field) => field.role === 'DIMENSION' || field.role === 'TIME')
    .map((field) => ({ label: field.label, value: field.code })),
);
const columnValueOptionsByField = computed(() => {
  const options = new Map<string, Array<{ label: string; value: string }>>();
  analysisConfig.value.columnDimensions.forEach((field) => {
    const values = [...new Set(getColumnDimensionValues(field))].sort(
      (left, right) =>
        left.localeCompare(right, 'zh-CN', {
          numeric: true,
          sensitivity: 'base',
        }),
    );
    options.set(
      field,
      values.map((value) => ({
        label: formatDimensionValue(field, value),
        value,
      })),
    );
  });
  return options;
});
const filterFieldOptions = computed(() =>
  fieldCatalog.value
    .filter((field) => field.role !== 'DETAIL')
    .map((field) => ({
      label: `${field.category} / ${field.label}`,
      value: field.code,
    })),
);
const catalogGroups = computed(() => {
  const keyword = fieldKeyword.value.trim().toLowerCase();
  const groups = new Map<string, FieldDefinition[]>();
  fieldCatalog.value
    .filter((field) => {
      if (!keyword) return true;
      return [field.code, field.label, field.category, field.description]
        .filter(Boolean)
        .some((item) => String(item).toLowerCase().includes(keyword));
    })
    .forEach((field) => {
      const group = groups.get(field.category) || [];
      group.push(field);
      groups.set(field.category, group);
    });
  return [...groups.entries()].map(([category, fields]) => ({
    category,
    fields,
  }));
});
const pivotColumns = computed(() => {
  const result = activePivotResult.value;
  const firstColumn = {
    dataIndex: '__groupLabel',
    fixed: 'left' as const,
    title: pieceListMode.value
      ? '生产片号'
      : result.currentDimension
        ? canDrillFurther.value
          ? `${currentDimensionLabel.value}（点击钻取）`
          : currentDimensionLabel.value
        : '全部来源',
    width: resolveColumnWidth('pivot:dimension', 190),
  };
  if (analysisConfig.value.columnDimensions.length > 0) {
    return [
      firstColumn,
      ...result.columnGroups.map((group) => ({
        children: group.children.map((column) => ({
          align: 'right' as const,
          dataIndex: column.dataIndex,
          formulaId: column.formulaId,
          metricId: column.metricId,
          title: column.title,
          width: resolveColumnWidth(
            getPivotValueWidthKey(group.key, column),
            112,
          ),
        })),
        title: group.title,
      })),
    ];
  }
  return [
    firstColumn,
    ...result.valueColumns.map((column) => ({
      align: 'right' as const,
      dataIndex: column.dataIndex,
      formulaId: column.formulaId,
      metricId: column.metricId,
      title: column.title,
      width: resolveColumnWidth(getPivotValueWidthKey(undefined, column), 120),
    })),
  ];
});
const pivotScrollX = computed(() =>
  Math.max(760, sumColumnWidths(pivotColumns.value)),
);
const tableScrollY = computed(() =>
  analysisConfig.value.layout.tableHeight === undefined
    ? autoTableHeight.value
    : clampNumber(
        analysisConfig.value.layout.tableHeight,
        MIN_TABLE_HEIGHT,
        MAX_TABLE_HEIGHT,
        autoTableHeight.value,
      ),
);
const configurableColumnWidths = computed<ColumnWidthOption[]>(() => {
  const result = activePivotResult.value;
  const columns: ColumnWidthOption[] = [
    {
      defaultWidth: 190,
      key: 'pivot:dimension',
      label: `行 / ${currentDimensionLabel.value || '全部来源'}`,
    },
  ];
  if (analysisConfig.value.columnDimensions.length > 0) {
    result.columnGroups.forEach((group) => {
      group.children.forEach((column) => {
        columns.push({
          defaultWidth: 112,
          key: getPivotValueWidthKey(group.key, column),
          label: `${group.title} / ${column.title}`,
        });
      });
    });
  } else {
    result.valueColumns.forEach((column) => {
      columns.push({
        defaultWidth: 120,
        key: getPivotValueWidthKey(undefined, column),
        label: column.title,
      });
    });
  }
  return columns;
});

onMounted(async () => {
  await loadMetadata();
  await loadSourceData();
  await nextTick();
  observeResultPanelSize();
});

onBeforeUnmount(() => {
  resultPanelResizeObserver?.disconnect();
});

watch(
  () => analysisConfig.value.rowDimensions.join('|'),
  () => resetDrill(),
);

watch(
  [filteredFacts, () => analysisConfig.value.chart],
  async () => {
    await nextTick();
    updateAutoTableHeight();
    await renderAnalysisChart();
  },
  { deep: true },
);

watch(isBrowseMode, async () => {
  await nextTick();
  observeResultPanelSize();
  await renderAnalysisChart();
});

async function loadMetadata() {
  metadataLoading.value = true;
  const [fieldResult, configResult] = await Promise.allSettled([
    getProcessAnalysisFieldCatalog(),
    getProcessAnalysisConfigList(),
  ]);
  if (fieldResult.status === 'fulfilled' && fieldResult.value.length > 0) {
    fieldCatalog.value = fieldResult.value;
  }
  if (configResult.status === 'fulfilled') {
    savedConfigs.value = configResult.value;
    const defaultConfig = savedConfigs.value.find(
      (item) => item.defaultFlag && item.editable,
    );
    if (defaultConfig) {
      applySavedConfig(defaultConfig, false);
    }
  }
  metadataLoading.value = false;
}

async function loadSourceData() {
  const [startDate, endDate] = analysisConfig.value.query.dateRange;
  if (!startDate || !endDate) {
    message.warning('请选择生产开始日期范围');
    return;
  }
  loading.value = true;
  try {
    const [sourceResult, qualityResult] = await Promise.allSettled([
      getProcessAnalysisSourceData({
        keyword: analysisConfig.value.query.keyword || undefined,
        pageNo: 1,
        pageSize: 200,
        productionStartDateEnd: endDate,
        productionStartDateStart: startDate,
      }),
      getYieldAnalysisOverview({
        endDate,
        pageNo: 1,
        pageSize: 1,
        processCode: 'ALL',
        startDate,
      }),
    ]);
    if (sourceResult.status === 'rejected') {
      throw sourceResult.reason;
    }
    rawRows.value = sourceResult.value;
    qualitySummaryRows.value =
      qualityResult.status === 'fulfilled'
        ? qualityResult.value.summaryRows || []
        : [];
    resetDrill();
    message.success(
      `已加载 ${rawRows.value.length} 条生产进度显示行、${qualitySummaryRows.value.length} 条分段工序质量汇总`,
    );
    if (qualityResult.status === 'rejected') {
      console.error(qualityResult.reason);
      message.warning('九类缺陷汇总加载失败，工序进度仍可继续查看');
    }
    await nextTick();
    await renderAnalysisChart();
  } catch (error) {
    console.error(error);
    message.error('加载统计分析源数据失败');
  } finally {
    loading.value = false;
  }
}

async function loadPieceQualityRows() {
  const motherRollNo = drillPath.value.find(
    (step) => step.field === 'motherRollBatchNo',
  )?.value;
  const segmentNo = drillPath.value.find(
    (step) => step.field === 'segmentBatchNo',
  )?.value;
  const [startDate, endDate] = analysisConfig.value.query.dateRange;
  if (!segmentNo || !startDate || !endDate) {
    pieceQualityRows.value = [];
    return;
  }
  const requestKey = [motherRollNo, segmentNo, startDate, endDate].join('|');
  pieceQualityRequestKey = requestKey;
  pieceListLoading.value = true;
  try {
    const pageSize = 200;
    const baseParams = {
      endDate,
      motherRollNo,
      pageSize,
      processCode: 'ALL',
      segmentNo,
      startDate,
    };
    const firstPage = await getYieldAnalysisDetailPage({
      ...baseParams,
      pageNo: 1,
    });
    const total = Number(firstPage.total || 0);
    const pageCount = Math.ceil(total / pageSize);
    const remainingPages =
      pageCount > 1
        ? await Promise.all(
            Array.from({ length: pageCount - 1 }, (_, index) =>
              getYieldAnalysisDetailPage({
                ...baseParams,
                pageNo: index + 2,
              }),
            ),
          )
        : [];
    if (pieceQualityRequestKey !== requestKey) return;
    pieceQualityRows.value = [
      ...(firstPage.list || []),
      ...remainingPages.flatMap((page) => page.list || []),
    ];
  } catch (error) {
    console.error(error);
    if (pieceQualityRequestKey === requestKey) {
      pieceQualityRows.value = [];
      message.warning('片号九类缺陷明细加载失败，其他片级指标仍可查看');
    }
  } finally {
    if (pieceQualityRequestKey === requestKey) {
      pieceListLoading.value = false;
    }
  }
}

function createDefaultConfig(): AnalysisConfig {
  return {
    chart: {
      categoryField: 'planDate',
      enabled: true,
      seriesField: 'stageName',
      type: 'LINE',
      valueField: 'yieldRate',
    },
    columnDimensions: ['stageName'],
    columnValueFilters: {
      stageName: [...DEFAULT_VISIBLE_STAGE_NAMES],
    },
    filters: [],
    formulas: [
      {
        decimals: 2,
        expression: '{m_done}/({m_done}+{m_defect})*100',
        format: 'PERCENT',
        id: 'yieldRate',
        label: '工序良品率',
      },
    ],
    metrics: [
      {
        aggregation: 'SUM_DISTINCT',
        decimals: 2,
        distinctKeyField: 'stageGrainKey',
        field: 'doneQty',
        id: 'm_done',
        label: '完工量',
      },
      {
        aggregation: 'SUM_DISTINCT',
        decimals: 2,
        distinctKeyField: 'stageGrainKey',
        field: 'pendingQty',
        id: 'm_pending',
        label: '未加工量',
      },
      {
        aggregation: 'SUM_DISTINCT',
        decimals: 0,
        distinctKeyField: 'stageGrainKey',
        field: 'pieceNgCount',
        id: 'm_defect',
        label: '不良片数',
      },
      ...DEFECT_METRIC_DEFINITIONS.map((item) => ({
        aggregation: 'SUM_DISTINCT' as const,
        decimals: 0,
        distinctKeyField: 'qualityGrainKey',
        ...item,
      })),
    ],
    layout: {
      columnWidths: {},
    },
    query: {
      dateRange: [
        dayjs().subtract(29, 'day').format('YYYY-MM-DD'),
        dayjs().format('YYYY-MM-DD'),
      ],
      keyword: '',
    },
    rowDimensions: ['motherRollBatchNo', 'segmentBatchNo'],
    version: 3,
  };
}

function normalizeLoadedConfig(input: Partial<AnalysisConfig>): AnalysisConfig {
  const fallback = createDefaultConfig();
  const inputVersion = Number(input.version || 1);
  const inputColumnWidths =
    input.layout?.columnWidths &&
    typeof input.layout.columnWidths === 'object' &&
    !Array.isArray(input.layout.columnWidths)
      ? input.layout.columnWidths
      : {};
  const columnWidths = Object.fromEntries(
    Object.entries(inputColumnWidths)
      .filter(([, value]) => Number.isFinite(Number(value)))
      .map(([key, value]) => [
        key,
        clampNumber(
          value,
          MIN_COLUMN_WIDTH,
          MAX_COLUMN_WIDTH,
          MIN_COLUMN_WIDTH,
        ),
      ]),
  );
  const columnValueFilters = Object.fromEntries(
    Object.entries(input.columnValueFilters || {})
      .filter(([field, values]) => field !== 'pieceNo' && Array.isArray(values))
      .map(([field, values]) => [
        field,
        [...new Set(values.map((value) => String(value)))],
      ]),
  );
  const tableHeight = input.layout?.tableHeight;
  const columnDimensions = (
    Array.isArray(input.columnDimensions)
      ? input.columnDimensions
      : fallback.columnDimensions
  ).filter((field) => field !== 'pieceNo');
  const rowDimensions = (
    Array.isArray(input.rowDimensions)
      ? input.rowDimensions
      : fallback.rowDimensions
  ).filter((field) => field !== 'pieceNo');
  const chart = { ...fallback.chart, ...(input.chart || {}) };
  if (chart.categoryField === 'pieceNo') {
    chart.categoryField = fallback.chart.categoryField;
  }
  if (chart.seriesField === 'pieceNo') {
    chart.seriesField = fallback.chart.seriesField;
  }
  if (
    inputVersion < 3 &&
    columnDimensions.includes('stageName') &&
    !Object.hasOwn(input.columnValueFilters || {}, 'stageName')
  ) {
    columnValueFilters.stageName = [...DEFAULT_VISIBLE_STAGE_NAMES];
  }
  const inputMetrics = (
    Array.isArray(input.metrics) ? input.metrics : fallback.metrics
  ).map((metric) =>
    metric.id === 'm_defect'
      ? {
          ...metric,
          decimals: 0,
          distinctKeyField: 'stageGrainKey',
          field: 'pieceNgCount',
          label: '不良片数',
        }
      : metric,
  );
  const metrics =
    inputVersion < 2 &&
    inputMetrics.some((metric) => metric.id === 'm_defect') &&
    !inputMetrics.some((metric) => metric.id === 'm_black_dot')
      ? [
          ...inputMetrics,
          ...DEFECT_METRIC_DEFINITIONS.map((item) => ({
            aggregation: 'SUM_DISTINCT' as const,
            decimals: 0,
            distinctKeyField: 'qualityGrainKey',
            ...item,
          })),
        ]
      : inputMetrics;
  const formulas = (
    Array.isArray(input.formulas) ? input.formulas : fallback.formulas
  ).map((formula) =>
    formula.expression === '({m_done}-{m_defect})/{m_done}*100'
      ? {
          ...formula,
          expression: '{m_done}/({m_done}+{m_defect})*100',
        }
      : formula,
  );
  return {
    chart,
    columnDimensions,
    columnValueFilters,
    filters: Array.isArray(input.filters)
      ? input.filters.filter((filter) => filter.field !== 'pieceNo')
      : [],
    formulas,
    layout: {
      columnWidths,
      tableHeight:
        tableHeight === undefined || tableHeight === null
          ? undefined
          : clampNumber(
              tableHeight,
              MIN_TABLE_HEIGHT,
              MAX_TABLE_HEIGHT,
              DEFAULT_TABLE_HEIGHT,
            ),
    },
    metrics,
    query: {
      dateRange: Array.isArray(input.query?.dateRange)
        ? input.query.dateRange
        : fallback.query.dateRange,
      keyword: String(input.query?.keyword || ''),
    },
    rowDimensions:
      rowDimensions.length > 0 ? rowDimensions : fallback.rowDimensions,
    version: 3,
  };
}

function clampNumber(
  value: unknown,
  min: number,
  max: number,
  fallback: number,
) {
  const number = Number(value);
  if (!Number.isFinite(number)) return fallback;
  return Math.round(Math.min(max, Math.max(min, number)));
}

function resolveColumnWidth(key: string, fallback: number) {
  return clampNumber(
    analysisConfig.value.layout.columnWidths[key],
    MIN_COLUMN_WIDTH,
    MAX_COLUMN_WIDTH,
    fallback,
  );
}

function getPivotValueWidthKey(
  groupKey: string | undefined,
  column: PivotCellColumn,
) {
  const valueId = column.metricId || column.formulaId || column.dataIndex;
  return groupKey
    ? `pivot:group:${encodeURIComponent(groupKey)}:${valueId}`
    : `pivot:value:${valueId}`;
}

function sumColumnWidths(columns: WidthAwareColumn[]): number {
  return columns.reduce((sum, column) => {
    if (column.children?.length) {
      return sum + sumColumnWidths(column.children);
    }
    return sum + Number(column.width || 0);
  }, 0);
}

function updateTableHeight(value: unknown) {
  if (value === null || value === undefined || value === '') {
    delete analysisConfig.value.layout.tableHeight;
    return;
  }
  analysisConfig.value.layout.tableHeight = clampNumber(
    value,
    MIN_TABLE_HEIGHT,
    MAX_TABLE_HEIGHT,
    DEFAULT_TABLE_HEIGHT,
  );
}

function resetTableHeight() {
  delete analysisConfig.value.layout.tableHeight;
}

function updateAutoTableHeight() {
  const element = resultPanelRef.value;
  if (!element) return;
  const reservedHeight = analysisConfig.value.chart.enabled ? 530 : 190;
  autoTableHeight.value = Math.max(
    MIN_TABLE_HEIGHT,
    Math.floor(element.clientHeight - reservedHeight),
  );
}

function observeResultPanelSize() {
  resultPanelResizeObserver?.disconnect();
  const element = resultPanelRef.value;
  if (!element || typeof ResizeObserver === 'undefined') return;
  updateAutoTableHeight();
  resultPanelResizeObserver = new ResizeObserver(updateAutoTableHeight);
  resultPanelResizeObserver.observe(element);
}

function normalizeColumnDimensionValue(value: unknown) {
  if (value === null || value === undefined) return '';
  if (typeof value === 'boolean') return value ? '是' : '否';
  return String(value).trim();
}

function getColumnDimensionValues(field: string) {
  if (field === 'pieceNo') {
    return normalizedFacts.value.flatMap((fact) =>
      (fact.stage.pieceDetails || [])
        .map((piece) =>
          normalizeColumnDimensionValue(piece.pieceNo || piece.outputBatchNo),
        )
        .filter(Boolean),
    );
  }
  return normalizedFacts.value.map((fact) =>
    normalizeColumnDimensionValue(fact[field]),
  );
}

function getColumnValueOptions(field: string) {
  return columnValueOptionsByField.value.get(field) || [];
}

function updateColumnValueFilter(field: string, values: unknown) {
  if (!Array.isArray(values) || values.length === 0) {
    delete analysisConfig.value.columnValueFilters[field];
    return;
  }
  analysisConfig.value.columnValueFilters[field] = values.map((value) =>
    String(value),
  );
}

function updateColumnWidth(key: string, value: unknown, defaultWidth: number) {
  analysisConfig.value.layout.columnWidths[key] = clampNumber(
    value,
    MIN_COLUMN_WIDTH,
    MAX_COLUMN_WIDTH,
    defaultWidth,
  );
}

function resetColumnWidths() {
  analysisConfig.value.layout.columnWidths = {};
}

function enterBrowseMode() {
  viewMode.value = 'BROWSE';
}

function exitBrowseMode() {
  viewMode.value = 'DESIGN';
}

function removeColumnDimension(index: number) {
  const field = analysisConfig.value.columnDimensions[index];
  if (field) {
    delete analysisConfig.value.columnValueFilters[field];
  }
  analysisConfig.value.columnDimensions.splice(index, 1);
}

function applySavedConfig(config: SavedConfig, reloadData = true) {
  try {
    analysisConfig.value = normalizeLoadedConfig(JSON.parse(config.configJson));
    currentConfigId.value = config.id;
    resetDrill();
    if (reloadData) {
      void loadSourceData();
    }
  } catch (error) {
    console.error(error);
    message.error('分析方案配置解析失败');
  }
}

function handleConfigChange(id?: number) {
  if (!id) {
    currentConfigId.value = undefined;
    analysisConfig.value = createDefaultConfig();
    void loadSourceData();
    return;
  }
  const config = savedConfigs.value.find((item) => item.id === id);
  if (config) applySavedConfig(config);
}

function resetToDefault() {
  currentConfigId.value = undefined;
  analysisConfig.value = createDefaultConfig();
  resetDrill();
  void loadSourceData();
}

function resetDrill() {
  drillPath.value = [];
  pieceListMode.value = false;
  pieceNgOnly.value = false;
  pieceQualityRequestKey = '';
  pieceQualityRows.value = [];
  pieceListLoading.value = false;
}

function handleDrill(row: PivotDisplayRow) {
  const currentDimension = pivotResult.value.currentDimension;
  if (!currentDimension || !canDrillFurther.value) return;
  const step: DrillStep = {
    field: currentDimension,
    label: getFieldLabel(currentDimension),
    value: row.__groupValue,
  };
  drillPath.value.push(step);
  if (currentDimension === 'segmentBatchNo') {
    pieceListMode.value = true;
    pieceNgOnly.value = false;
    void loadPieceQualityRows();
  }
}

function handleBreadcrumb(index: number) {
  if (index < 0) {
    resetDrill();
    return;
  }
  drillPath.value = drillPath.value.slice(0, index + 1);
  pieceListMode.value = drillPath.value.at(-1)?.field === 'segmentBatchNo';
  if (!pieceListMode.value) {
    pieceNgOnly.value = false;
  }
}

function handleBackLevel() {
  if (pieceListMode.value) {
    pieceListMode.value = false;
    pieceNgOnly.value = false;
    drillPath.value = drillPath.value.slice(0, -1);
    return;
  }
  drillPath.value = drillPath.value.slice(0, -1);
}

function addDimension(field: FieldDefinition, target: 'column' | 'row') {
  const rows = analysisConfig.value.rowDimensions;
  const columns = analysisConfig.value.columnDimensions;
  removeFromArray(rows, field.code);
  removeFromArray(columns, field.code);
  (target === 'row' ? rows : columns).push(field.code);
  if (target === 'row') {
    delete analysisConfig.value.columnValueFilters[field.code];
  }
  resetDrill();
}

function addMetric(field: FieldDefinition) {
  if (analysisConfig.value.metrics.some((item) => item.field === field.code)) {
    message.info(`${field.label}已在值区域`);
    return;
  }
  const baseId = `m_${field.code}`;
  let id = baseId;
  let suffix = 2;
  while (analysisConfig.value.metrics.some((item) => item.id === id)) {
    id = `${baseId}_${suffix}`;
    suffix += 1;
  }
  analysisConfig.value.metrics.push({
    aggregation: field.defaultAggregation || 'SUM',
    decimals: 2,
    distinctKeyField: field.distinctKeyField,
    field: field.code,
    id,
    label: field.label,
  });
  if (!analysisConfig.value.chart.valueField) {
    analysisConfig.value.chart.valueField = id;
  }
}

function addFilter(field?: FieldDefinition) {
  const target =
    field ||
    fieldCatalog.value.find((item) => item.role !== 'DETAIL') ||
    FALLBACK_FIELD_CATALOG[0];
  if (!target) return;
  analysisConfig.value.filters.push({
    field: target.code,
    id: `filter_${Date.now()}_${analysisConfig.value.filters.length}`,
    operator: target.filterOperators[0] || 'EQ',
    value: '',
  });
}

function handleFilterFieldChange(filter: AnalysisFilter) {
  const field = fieldMap.value.get(filter.field);
  filter.operator = field?.filterOperators[0] || 'EQ';
  filter.value = '';
}

function removeMetric(index: number) {
  const metric = analysisConfig.value.metrics[index];
  if (!metric) return;
  const reference = `{${metric.id}}`;
  if (
    analysisConfig.value.formulas.some((formula) =>
      formula.expression.includes(reference),
    )
  ) {
    message.warning(`已有公式引用 ${reference}，删除后该引用按 0 计算`);
  }
  analysisConfig.value.metrics.splice(index, 1);
}

function moveItem<T>(items: T[], index: number, direction: -1 | 1) {
  const targetIndex = index + direction;
  if (targetIndex < 0 || targetIndex >= items.length) return;
  const [item] = items.splice(index, 1);
  if (item !== undefined) items.splice(targetIndex, 0, item);
}

function removeFromArray(items: string[], value: string) {
  const index = items.indexOf(value);
  if (index >= 0) items.splice(index, 1);
}

function openFormulaModal(index = -1) {
  editingFormulaIndex.value = index;
  const source = index >= 0 ? analysisConfig.value.formulas[index] : undefined;
  Object.assign(formulaForm, {
    decimals: source?.decimals ?? 2,
    expression: source?.expression || '',
    format: source?.format || 'NUMBER',
    id: source?.id || `formula_${analysisConfig.value.formulas.length + 1}`,
    label: source?.label || '',
  });
  formulaModalVisible.value = true;
}

function insertMetricToken(metric: AnalysisMetric) {
  formulaForm.expression += `{${metric.id}}`;
}

function formatMetricToken(metric: AnalysisMetric) {
  return `{${metric.id}}`;
}

function saveFormula() {
  const id = formulaForm.id.trim();
  const label = formulaForm.label.trim();
  if (!/^[A-Za-z][\w-]*$/.test(id)) {
    message.warning(
      '公式编码必须以字母开头，只能包含字母、数字、下划线或短横线',
    );
    return;
  }
  if (!label) {
    message.warning('请输入虚拟列名称');
    return;
  }
  const duplicate = analysisConfig.value.formulas.some(
    (item, index) => item.id === id && index !== editingFormulaIndex.value,
  );
  if (
    duplicate ||
    analysisConfig.value.metrics.some((item) => item.id === id)
  ) {
    message.warning('公式编码不能与其他指标或公式重复');
    return;
  }
  const error = validateFormulaExpression(
    formulaForm.expression,
    analysisConfig.value.metrics.map((item) => item.id),
  );
  if (error) {
    message.warning(error);
    return;
  }
  const value: AnalysisFormula = {
    decimals: Number(formulaForm.decimals || 0),
    expression: formulaForm.expression.trim(),
    format: formulaForm.format,
    id,
    label,
  };
  if (editingFormulaIndex.value >= 0) {
    analysisConfig.value.formulas.splice(editingFormulaIndex.value, 1, value);
  } else {
    analysisConfig.value.formulas.push(value);
  }
  formulaModalVisible.value = false;
}

function openSaveAsModal() {
  const current = currentConfig.value;
  Object.assign(saveForm, {
    configName: current ? `${current.configName} - 副本` : '我的生产进度分析',
    defaultFlag: false,
    remark: current?.remark || '',
    scopeType: current?.scopeType || 'PRIVATE',
  });
  saveModalVisible.value = true;
}

async function saveCurrentConfig() {
  const current = currentConfig.value;
  if (!current?.editable) {
    openSaveAsModal();
    return;
  }
  saving.value = true;
  try {
    await updateProcessAnalysisConfig({
      configJson: JSON.stringify(analysisConfig.value),
      configName: current.configName,
      defaultFlag: current.defaultFlag,
      id: current.id,
      remark: current.remark,
      scopeType: current.scopeType,
    });
    await refreshConfigList(current.id);
    message.success('分析方案已保存');
  } catch (error) {
    console.error(error);
    message.error('保存分析方案失败');
  } finally {
    saving.value = false;
  }
}

async function saveAsConfig() {
  if (!saveForm.configName.trim()) {
    message.warning('请输入方案名称');
    return;
  }
  saving.value = true;
  try {
    const id = await createProcessAnalysisConfig({
      configJson: JSON.stringify(analysisConfig.value),
      configName: saveForm.configName.trim(),
      defaultFlag: saveForm.defaultFlag,
      remark: saveForm.remark.trim(),
      scopeType: saveForm.scopeType,
    });
    saveModalVisible.value = false;
    await refreshConfigList(id);
    message.success('分析方案已另存');
  } catch (error) {
    console.error(error);
    message.error('另存分析方案失败');
  } finally {
    saving.value = false;
  }
}

function deleteCurrentConfig() {
  const current = currentConfig.value;
  if (!current?.editable) return;
  Modal.confirm({
    content: `删除后不可恢复，但不会影响生产数据。确认删除“${current.configName}”吗？`,
    okButtonProps: { danger: true },
    okText: '删除',
    onOk: async () => {
      await deleteProcessAnalysisConfig(current.id);
      currentConfigId.value = undefined;
      await refreshConfigList();
      message.success('分析方案已删除');
    },
    title: '删除分析方案',
  });
}

async function refreshConfigList(selectedId?: number) {
  savedConfigs.value = await getProcessAnalysisConfigList();
  if (selectedId) currentConfigId.value = selectedId;
}

function exportCurrentView() {
  const result = activePivotResult.value;
  const lines: string[][] = [];
  const headers = [currentDimensionLabel.value || '全部来源'];
  const dataIndexes = ['__groupLabel'];
  if (analysisConfig.value.columnDimensions.length > 0) {
    result.columnGroups.forEach((group) => {
      group.children.forEach((column) => {
        headers.push(`${group.title} / ${column.title}`);
        dataIndexes.push(column.dataIndex);
      });
    });
  } else {
    result.valueColumns.forEach((column) => {
      headers.push(column.title);
      dataIndexes.push(column.dataIndex);
    });
  }
  lines.push(headers);
  result.rows.forEach((row) => {
    lines.push(
      dataIndexes.map((key, index) =>
        index === 0
          ? formatDimensionValue(
              pieceListMode.value ? 'pieceListNo' : result.currentDimension,
              row.__groupValue,
            )
          : formatCellValue(row[key], key),
      ),
    );
  });
  if (lines.length <= 1) {
    message.warning('当前没有可导出的数据');
    return;
  }
  const csv = `\uFEFF${lines
    .map((line) => line.map(escapeCsvCell).join(','))
    .join('\r\n')}`;
  downloadFileFromBlobPart({
    fileName: `统计分析${pieceListMode.value ? '_片号列表' : ''}_${dayjs().format('YYYYMMDD_HHmmss')}.csv`,
    source: new Blob([csv], { type: 'text/csv;charset=utf-8' }),
  });
}

async function renderAnalysisChart() {
  if (!analysisConfig.value.chart.enabled || !chartRef.value) return;
  const data = buildChartData(filteredFacts.value, analysisConfig.value);
  const valueDefinition = valueDefinitions.value.find(
    (item) => item.id === analysisConfig.value.chart.valueField,
  );
  const valueName = valueDefinition?.label || '数值';
  const percent =
    valueDefinition &&
    'format' in valueDefinition &&
    valueDefinition.format === 'PERCENT';
  if (analysisConfig.value.chart.type === 'PIE') {
    const values = data.categories.map((name, index) => ({
      name,
      value: data.series.reduce(
        (sum, series) => sum + Number(series.data[index] || 0),
        0,
      ),
    }));
    await renderEcharts({
      legend: { bottom: 0, type: 'scroll' },
      series: [
        {
          data: values,
          label: {
            formatter: percent ? '{b}: {c}%' : '{b}: {c}',
          },
          radius: ['35%', '68%'],
          type: 'pie',
        },
      ],
      title: { left: 'center', text: `${valueName}分布` },
      tooltip: { trigger: 'item' },
    });
    return;
  }
  await renderEcharts({
    grid: { bottom: 62, containLabel: true, left: 26, right: 28, top: 54 },
    legend: { bottom: 4, type: 'scroll' },
    series: data.series.map((series) => ({
      ...series,
      areaStyle:
        analysisConfig.value.chart.type === 'LINE'
          ? { opacity: 0.06 }
          : undefined,
      smooth: analysisConfig.value.chart.type === 'LINE',
      type: analysisConfig.value.chart.type.toLowerCase(),
    })),
    title: { left: 12, text: `${valueName}分析` },
    tooltip: {
      trigger: 'axis',
      valueFormatter: (value: unknown) =>
        `${Number(value || 0).toFixed(valueDefinition?.decimals ?? 2)}${percent ? '%' : ''}`,
    },
    xAxis: {
      axisLabel: {
        hideOverlap: true,
        rotate: data.categories.length > 10 ? 30 : 0,
      },
      data: data.categories,
      type: 'category',
    },
    yAxis: {
      axisLabel: { formatter: percent ? '{value}%' : '{value}' },
      type: 'value',
    },
  });
}

function getFieldLabel(code?: string) {
  if (!code) return '全部来源';
  return fieldMap.value.get(code)?.label || code;
}

function getMetricFieldLabel(metric: AnalysisMetric) {
  return fieldMap.value.get(metric.field)?.label || metric.field;
}

function getFilterOperatorOptions(filter: AnalysisFilter) {
  return (fieldMap.value.get(filter.field)?.filterOperators || ['EQ']).map(
    (value) => ({
      label: OPERATOR_LABELS[value] || value,
      value,
    }),
  );
}

function filterNeedsValue(filter: AnalysisFilter) {
  return !['EMPTY', 'NOT_EMPTY'].includes(filter.operator);
}

function isDimension(field: FieldDefinition) {
  return field.role === 'DIMENSION' || field.role === 'TIME';
}

function isMetric(field: FieldDefinition) {
  return field.role === 'METRIC';
}

function isDetail(field: FieldDefinition) {
  return field.role === 'DETAIL';
}

function formatDimensionValue(field: string | undefined, value: unknown) {
  const raw = String(value ?? '');
  if (!raw) return '（空）';
  if (field === 'planStatus') {
    return PLAN_STATUS_OPTIONS.find((item) => item.value === raw)?.label || raw;
  }
  if (field === 'stageStatus') return STAGE_STATUS_LABELS[raw] || raw;
  return raw;
}

function normalizeCellText(value: unknown) {
  return String(value ?? '').trim();
}

function formatCellValue(value: unknown, dataIndex: string) {
  const metric = analysisConfig.value.metrics.find((item) =>
    dataIndex.endsWith(`__${item.id}`),
  );
  const formula = analysisConfig.value.formulas.find((item) =>
    dataIndex.endsWith(`__${item.id}`),
  );
  return formatAnalysisValue(value, formula || metric);
}

function escapeCsvCell(value: string) {
  const text = String(value ?? '');
  return /[",\r\n]/.test(text) ? `"${text.replaceAll('"', '""')}"` : text;
}
</script>

<template>
  <Page
    auto-content-height
    class="process-analysis-page"
    content-class="process-analysis-content"
  >
    <div class="analysis-shell">
      <section v-if="!isBrowseMode" class="analysis-banner">
        <div class="banner-copy">
          <div class="banner-icon">
            <IconifyIcon icon="lucide:chart-no-axes-combined" />
          </div>
          <div>
            <div class="banner-title-row">
              <h2>统计分析</h2>
              <Tag color="blue">生产进度自由透视</Tag>
              <Tag>{{ fieldCatalog.length }} 个字段</Tag>
            </div>
            <p>
              行、列、值自由组合，支持安全公式、动态图表、字段过滤与当前区域分级钻取
            </p>
          </div>
        </div>
        <div class="banner-actions">
          <Select
            allow-clear
            class="config-select"
            :loading="metadataLoading"
            placeholder="选择分析方案"
            :value="currentConfigId"
            @change="handleConfigChange"
          >
            <Select.Option
              v-for="item in savedConfigs"
              :key="item.id"
              :value="item.id"
            >
              {{ item.configName }}
              {{ item.scopeType === 'SHARED' ? '（共享）' : '' }}
            </Select.Option>
          </Select>
          <Button :loading="loading" type="primary" @click="loadSourceData">
            <template #icon>
              <IconifyIcon icon="lucide:refresh-cw" />
            </template>
            刷新分析
          </Button>
          <Button :loading="saving" @click="saveCurrentConfig">
            <template #icon><IconifyIcon icon="lucide:save" /></template>
            保存配置
          </Button>
          <Button @click="openSaveAsModal">
            <template #icon><IconifyIcon icon="lucide:copy-plus" /></template>
            另存为
          </Button>
          <Button
            danger
            :disabled="!currentConfig?.editable"
            @click="deleteCurrentConfig"
          >
            <template #icon><IconifyIcon icon="lucide:trash-2" /></template>
            删除配置
          </Button>
          <Button @click="exportCurrentView">
            <template #icon><IconifyIcon icon="lucide:file-down" /></template>
            导出当前视图
          </Button>
          <Button type="primary" ghost @click="enterBrowseMode">
            <template #icon><IconifyIcon icon="lucide:eye" /></template>
            浏览模式
          </Button>
        </div>
      </section>

      <section
        :class="{ 'browse-query-bar': isBrowseMode }"
        class="analysis-query-bar"
      >
        <div class="query-item query-date">
          <label>生产开始日期</label>
          <RangePicker
            v-model:value="analysisConfig.query.dateRange"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
          />
        </div>
        <div class="query-item query-keyword">
          <label>关键词</label>
          <Input
            v-model:value="analysisConfig.query.keyword"
            allow-clear
            placeholder="计划号、料号、型号、批号、路线"
            @press-enter="loadSourceData"
          />
        </div>
        <Button :loading="loading" type="primary" @click="loadSourceData">
          查询
        </Button>
        <Button v-if="!isBrowseMode" @click="resetToDefault">
          恢复默认方案
        </Button>
        <template v-else>
          <Checkbox v-model:checked="analysisConfig.chart.enabled">
            显示图形
          </Checkbox>
          <Button @click="exportCurrentView">
            <template #icon><IconifyIcon icon="lucide:file-down" /></template>
            导出
          </Button>
          <Button @click="exitBrowseMode">
            <template #icon><IconifyIcon icon="lucide:settings-2" /></template>
            返回设计
          </Button>
        </template>
        <div class="query-stats">
          <span
            >显示行 <strong>{{ rawRows.length }}</strong></span
          >
          <span
            >工序事实 <strong>{{ normalizedFacts.length }}</strong></span
          >
          <span
            >过滤后 <strong>{{ filteredFacts.length }}</strong></span
          >
        </div>
      </section>

      <Spin :spinning="loading">
        <div
          :class="{ 'browse-mode': isBrowseMode }"
          class="analysis-workspace"
        >
          <aside v-if="!isBrowseMode" class="field-panel">
            <div class="panel-heading">
              <div>
                <strong>字段目录</strong>
                <span>生产进度所有元素字段化</span>
              </div>
            </div>
            <Input
              v-model:value="fieldKeyword"
              allow-clear
              class="field-search"
              placeholder="搜索字段名或编码"
            >
              <template #prefix>
                <IconifyIcon icon="lucide:search" />
              </template>
            </Input>
            <div class="field-list">
              <section
                v-for="group in catalogGroups"
                :key="group.category"
                class="field-group"
              >
                <div class="field-group-title">
                  <span>{{ group.category }}</span>
                  <em>{{ group.fields.length }}</em>
                </div>
                <div
                  v-for="field in group.fields"
                  :key="field.code"
                  class="field-card"
                >
                  <div class="field-card-main">
                    <Tooltip :title="field.description || field.sourcePath">
                      <span class="field-label">{{ field.label }}</span>
                    </Tooltip>
                    <Tag
                      :color="
                        field.role === 'METRIC'
                          ? 'green'
                          : field.role === 'DETAIL'
                            ? 'orange'
                            : 'blue'
                      "
                    >
                      {{
                        field.role === 'METRIC'
                          ? '数值'
                          : field.role === 'DETAIL'
                            ? '明细'
                            : '维度'
                      }}
                    </Tag>
                  </div>
                  <code>{{ field.code }}</code>
                  <div v-if="!isDetail(field)" class="field-actions">
                    <Button
                      v-if="isDimension(field)"
                      size="small"
                      type="text"
                      @click="addDimension(field, 'row')"
                    >
                      + 行
                    </Button>
                    <Button
                      v-if="isDimension(field)"
                      size="small"
                      type="text"
                      @click="addDimension(field, 'column')"
                    >
                      + 列
                    </Button>
                    <Button
                      v-if="isMetric(field)"
                      size="small"
                      type="text"
                      @click="addMetric(field)"
                    >
                      + 值
                    </Button>
                    <Button size="small" type="text" @click="addFilter(field)">
                      + 筛
                    </Button>
                  </div>
                  <div v-else class="detail-hint">
                    仅用于来源或末级列表，不加入透视行列
                  </div>
                </div>
              </section>
            </div>
          </aside>

          <main ref="resultPanelRef" class="result-panel">
            <section
              :class="{
                'fill-parent':
                  !analysisConfig.chart.enabled &&
                  analysisConfig.layout.tableHeight === undefined,
              }"
              :style="{
                '--analysis-table-body-height': `${tableScrollY}px`,
              }"
              class="result-card pivot-card"
            >
              <header class="result-card-header">
                <div>
                  <strong>{{
                    pieceListMode ? '分段片号列表' : '动态透视结果'
                  }}</strong>
                  <span>
                    {{
                      `${activePivotResult.rows.length} ${
                        pieceListMode ? '个片号' : '个当前层级分组'
                      }`
                    }}
                  </span>
                </div>
                <div
                  v-if="pieceListMode || drillPath.length"
                  class="result-card-actions"
                >
                  <Checkbox v-if="pieceListMode" v-model:checked="pieceNgOnly">
                    只看不良品
                  </Checkbox>
                  <Button
                    v-if="drillPath.length"
                    size="small"
                    @click="handleBackLevel"
                  >
                    <template #icon>
                      <IconifyIcon icon="lucide:arrow-left" />
                    </template>
                    返回上一级
                  </Button>
                </div>
              </header>

              <div class="drill-breadcrumb">
                <button type="button" @click="handleBreadcrumb(-1)">
                  全部数据
                </button>
                <template
                  v-for="(step, index) in drillPath"
                  :key="`${step.field}-${index}`"
                >
                  <IconifyIcon icon="lucide:chevron-right" />
                  <button type="button" @click="handleBreadcrumb(index)">
                    {{ step.label }}：{{
                      formatDimensionValue(step.field, step.value)
                    }}
                  </button>
                </template>
                <span v-if="canDrillFurther" class="drill-tip">
                  当前层级：{{ currentDimensionLabel }}，点击首列向下钻取
                </span>
                <span v-else-if="pieceListMode" class="drill-tip">
                  当前层级：生产片号，片号列表为末级
                </span>
                <span v-else class="drill-tip">
                  当前层级：{{ currentDimensionLabel }}
                </span>
              </div>

              <Table
                :columns="pivotColumns"
                :data-source="activePivotResult.rows"
                :locale="{
                  emptyText: pieceListMode
                    ? pieceNgOnly
                      ? '当前分段没有不良片号'
                      : '当前分段没有片号数据'
                    : '当前组合没有数据，请调整筛选或字段配置',
                }"
                :loading="pieceListMode && pieceListLoading"
                :pagination="{
                  pageSize: 20,
                  showSizeChanger: true,
                  showTotal: (total: number) =>
                    `共 ${total} ${pieceListMode ? '个片号' : '组'}`,
                }"
                row-key="__key"
                size="small"
                :scroll="{ x: pivotScrollX, y: tableScrollY }"
              >
                <template #bodyCell="{ column, record, text }">
                  <button
                    v-if="
                      column.dataIndex === '__groupLabel' && canDrillFurther
                    "
                    class="drill-link"
                    type="button"
                    @click="handleDrill(record)"
                  >
                    <span>
                      {{
                        formatDimensionValue(
                          activePivotResult.currentDimension,
                          record.__groupValue,
                        )
                      }}
                    </span>
                    <IconifyIcon icon="lucide:corner-right-down" />
                  </button>
                  <span
                    v-else-if="column.dataIndex === '__groupLabel'"
                    class="dimension-cell"
                  >
                    {{
                      formatDimensionValue(
                        activePivotResult.currentDimension,
                        record.__groupValue,
                      )
                    }}
                  </span>
                  <span v-else class="metric-cell">
                    {{ formatCellValue(text, String(column.dataIndex)) }}
                  </span>
                </template>
              </Table>
            </section>

            <section
              v-if="analysisConfig.chart.enabled"
              class="result-card chart-card"
            >
              <header class="result-card-header">
                <div>
                  <strong>动态图表</strong>
                  <span>与当前源数据过滤条件同步</span>
                </div>
                <Tag color="purple">
                  {{
                    analysisConfig.chart.type === 'LINE'
                      ? '折线'
                      : analysisConfig.chart.type === 'BAR'
                        ? '柱状'
                        : '饼图'
                  }}
                </Tag>
              </header>
              <EchartsUI ref="chartRef" class="analysis-chart" />
            </section>
          </main>

          <aside v-if="!isBrowseMode" class="config-panel">
            <div class="panel-heading">
              <div>
                <strong>透视表字段</strong>
                <span>顺序即展示与钻取顺序</span>
              </div>
            </div>

            <section class="config-section">
              <div class="config-section-title">
                <span><IconifyIcon icon="lucide:rows-3" /> 行（钻取层级）</span>
                <em>{{ analysisConfig.rowDimensions.length }}</em>
              </div>
              <Empty
                v-if="analysisConfig.rowDimensions.length === 0"
                :image="Empty.PRESENTED_IMAGE_SIMPLE"
                description="从左侧加入行字段"
              />
              <div
                v-for="(field, index) in analysisConfig.rowDimensions"
                :key="field"
                class="config-pill"
              >
                <span>{{ index + 1 }}. {{ getFieldLabel(field) }}</span>
                <div>
                  <Button
                    :disabled="index === 0"
                    size="small"
                    type="text"
                    @click="moveItem(analysisConfig.rowDimensions, index, -1)"
                  >
                    ↑
                  </Button>
                  <Button
                    :disabled="
                      index === analysisConfig.rowDimensions.length - 1
                    "
                    size="small"
                    type="text"
                    @click="moveItem(analysisConfig.rowDimensions, index, 1)"
                  >
                    ↓
                  </Button>
                  <Button
                    danger
                    size="small"
                    type="text"
                    @click="analysisConfig.rowDimensions.splice(index, 1)"
                  >
                    ×
                  </Button>
                </div>
              </div>
            </section>

            <section class="config-section">
              <div class="config-section-title">
                <span><IconifyIcon icon="lucide:columns-3" /> 列</span>
                <em>{{ analysisConfig.columnDimensions.length }}</em>
              </div>
              <Empty
                v-if="analysisConfig.columnDimensions.length === 0"
                :image="Empty.PRESENTED_IMAGE_SIMPLE"
                description="无列字段时只显示值"
              />
              <div
                v-for="(field, index) in analysisConfig.columnDimensions"
                :key="field"
                class="column-config-card"
              >
                <div class="config-pill column-config-title">
                  <span>{{ getFieldLabel(field) }}</span>
                  <div>
                    <Button
                      :disabled="index === 0"
                      size="small"
                      type="text"
                      @click="
                        moveItem(analysisConfig.columnDimensions, index, -1)
                      "
                    >
                      ↑
                    </Button>
                    <Button
                      :disabled="
                        index === analysisConfig.columnDimensions.length - 1
                      "
                      size="small"
                      type="text"
                      @click="
                        moveItem(analysisConfig.columnDimensions, index, 1)
                      "
                    >
                      ↓
                    </Button>
                    <Button
                      danger
                      size="small"
                      type="text"
                      @click="removeColumnDimension(index)"
                    >
                      ×
                    </Button>
                  </div>
                </div>
                <Select
                  allow-clear
                  class="column-value-filter"
                  :max-tag-count="1"
                  mode="multiple"
                  :options="getColumnValueOptions(field)"
                  placeholder="全部列值"
                  size="small"
                  :value="analysisConfig.columnValueFilters[field] || []"
                  @change="(values) => updateColumnValueFilter(field, values)"
                />
                <span class="column-filter-hint">
                  仅控制该维度显示哪些列值
                </span>
              </div>
            </section>

            <section class="config-section">
              <div class="config-section-title">
                <span><IconifyIcon icon="lucide:sigma" /> 值</span>
                <em>{{ analysisConfig.metrics.length }}</em>
              </div>
              <Empty
                v-if="analysisConfig.metrics.length === 0"
                :image="Empty.PRESENTED_IMAGE_SIMPLE"
                description="从左侧加入数值字段"
              />
              <div
                v-for="(metric, index) in analysisConfig.metrics"
                :key="metric.id"
                class="metric-config-card"
              >
                <div class="metric-config-title">
                  <span>{{ metric.label }}</span>
                  <Button
                    danger
                    size="small"
                    type="text"
                    @click="removeMetric(index)"
                  >
                    ×
                  </Button>
                </div>
                <code>{{ metric.id }} · {{ getMetricFieldLabel(metric) }}</code>
                <div class="metric-config-row">
                  <Select
                    v-model:value="metric.aggregation"
                    :options="AGGREGATION_OPTIONS"
                    size="small"
                  />
                  <InputNumber
                    v-model:value="metric.decimals"
                    :max="6"
                    :min="0"
                    size="small"
                  />
                </div>
                <div class="order-actions">
                  <Button
                    :disabled="index === 0"
                    size="small"
                    type="text"
                    @click="moveItem(analysisConfig.metrics, index, -1)"
                  >
                    上移
                  </Button>
                  <Button
                    :disabled="index === analysisConfig.metrics.length - 1"
                    size="small"
                    type="text"
                    @click="moveItem(analysisConfig.metrics, index, 1)"
                  >
                    下移
                  </Button>
                </div>
              </div>
            </section>

            <section class="config-section">
              <div class="config-section-title">
                <span
                  ><IconifyIcon icon="lucide:function-square" /> 虚拟列</span
                >
                <Button size="small" type="link" @click="openFormulaModal()">
                  新增公式
                </Button>
              </div>
              <div
                v-for="(formula, index) in analysisConfig.formulas"
                :key="formula.id"
                class="formula-card"
              >
                <div>
                  <strong>{{ formula.label }}</strong>
                  <Tag :color="formula.format === 'PERCENT' ? 'green' : 'blue'">
                    {{ formula.format === 'PERCENT' ? '百分比' : '数值' }}
                  </Tag>
                </div>
                <code>{{ formula.expression }}</code>
                <div>
                  <Button
                    size="small"
                    type="link"
                    @click="openFormulaModal(index)"
                  >
                    编辑
                  </Button>
                  <Button
                    danger
                    size="small"
                    type="link"
                    @click="analysisConfig.formulas.splice(index, 1)"
                  >
                    删除
                  </Button>
                </div>
              </div>
            </section>

            <section class="config-section">
              <div class="config-section-title">
                <span><IconifyIcon icon="lucide:list-filter" /> 过滤器</span>
                <Button size="small" type="link" @click="addFilter()">
                  新增过滤
                </Button>
              </div>
              <div
                v-for="(filter, index) in analysisConfig.filters"
                :key="filter.id"
                class="filter-card"
              >
                <div class="filter-card-title">
                  <span>过滤条件 {{ index + 1 }}</span>
                  <Button
                    danger
                    size="small"
                    type="text"
                    @click="analysisConfig.filters.splice(index, 1)"
                  >
                    ×
                  </Button>
                </div>
                <Select
                  v-model:value="filter.field"
                  show-search
                  :options="filterFieldOptions"
                  size="small"
                  @change="handleFilterFieldChange(filter)"
                />
                <Select
                  v-model:value="filter.operator"
                  :options="getFilterOperatorOptions(filter)"
                  size="small"
                />
                <Input
                  v-if="filterNeedsValue(filter)"
                  v-model:value="filter.value"
                  :placeholder="
                    filter.operator === 'BETWEEN'
                      ? '起始值,结束值'
                      : filter.operator === 'IN'
                        ? '值1,值2'
                        : '输入过滤值'
                  "
                  size="small"
                />
              </div>
            </section>

            <section class="config-section">
              <div class="config-section-title">
                <span
                  ><IconifyIcon icon="lucide:table-properties" /> 表格布局</span
                >
                <Button size="small" type="link" @click="resetColumnWidths">
                  重置列宽
                </Button>
              </div>
              <div class="table-layout-row">
                <label>表格高度</label>
                <InputNumber
                  :max="MAX_TABLE_HEIGHT"
                  :min="MIN_TABLE_HEIGHT"
                  placeholder="自动撑满"
                  :step="20"
                  :value="analysisConfig.layout.tableHeight"
                  size="small"
                  @change="updateTableHeight"
                />
                <span>px</span>
              </div>
              <div
                v-if="analysisConfig.layout.tableHeight === undefined"
                class="auto-height-hint"
              >
                自动撑满父结果区，当前约 {{ tableScrollY }}px
              </div>
              <Button v-else size="small" type="link" @click="resetTableHeight">
                恢复自动撑满
              </Button>
              <div class="column-width-heading">当前透视列宽</div>
              <div class="column-width-list">
                <div
                  v-for="column in configurableColumnWidths"
                  :key="column.key"
                  class="column-width-row"
                >
                  <Tooltip :title="column.label">
                    <label>{{ column.label }}</label>
                  </Tooltip>
                  <InputNumber
                    :max="MAX_COLUMN_WIDTH"
                    :min="MIN_COLUMN_WIDTH"
                    :step="10"
                    :value="resolveColumnWidth(column.key, column.defaultWidth)"
                    size="small"
                    @change="
                      (value) =>
                        updateColumnWidth(
                          column.key,
                          value,
                          column.defaultWidth,
                        )
                    "
                  />
                </div>
              </div>
            </section>

            <section class="config-section">
              <div class="config-section-title">
                <span><IconifyIcon icon="lucide:chart-line" /> 图表</span>
                <Checkbox v-model:checked="analysisConfig.chart.enabled">
                  显示
                </Checkbox>
              </div>
              <div class="chart-config-grid">
                <label>类型</label>
                <Select
                  v-model:value="analysisConfig.chart.type"
                  :options="[
                    { label: '折线图', value: 'LINE' },
                    { label: '柱状图', value: 'BAR' },
                    { label: '饼图', value: 'PIE' },
                  ]"
                  size="small"
                />
                <label>分类轴</label>
                <Select
                  v-model:value="analysisConfig.chart.categoryField"
                  show-search
                  :options="dimensionOptions"
                  size="small"
                />
                <label>数值</label>
                <Select
                  v-model:value="analysisConfig.chart.valueField"
                  :options="chartValueOptions"
                  size="small"
                />
                <label>系列</label>
                <Select
                  v-model:value="analysisConfig.chart.seriesField"
                  allow-clear
                  show-search
                  :options="dimensionOptions"
                  placeholder="可不选"
                  size="small"
                />
              </div>
            </section>
          </aside>
        </div>
      </Spin>
    </div>

    <Modal
      v-model:open="formulaModalVisible"
      :title="editingFormulaIndex >= 0 ? '编辑虚拟列' : '新增虚拟列'"
      width="680px"
      @ok="saveFormula"
    >
      <div class="formula-modal">
        <label>公式编码</label>
        <Input
          v-model:value="formulaForm.id"
          :disabled="editingFormulaIndex >= 0"
          placeholder="例如 yieldRate"
        />
        <label>显示名称</label>
        <Input
          v-model:value="formulaForm.label"
          placeholder="例如 工序良品率"
        />
        <label>表达式</label>
        <Input.TextArea
          v-model:value="formulaForm.expression"
          :auto-size="{ minRows: 3, maxRows: 6 }"
          placeholder="例如 {m_done}/({m_done}+{m_defect})*100"
        />
        <div class="formula-token-list">
          <span>点击插入指标：</span>
          <Button
            v-for="metric in analysisConfig.metrics"
            :key="metric.id"
            size="small"
            @click="insertMetricToken(metric)"
          >
            {{ metric.label }} · {{ formatMetricToken(metric) }}
          </Button>
        </div>
        <label>格式</label>
        <Select
          v-model:value="formulaForm.format"
          :options="[
            { label: '普通数值', value: 'NUMBER' },
            { label: '百分比', value: 'PERCENT' },
          ]"
        />
        <label>小数位</label>
        <InputNumber v-model:value="formulaForm.decimals" :max="6" :min="0" />
        <div class="formula-help">
          <IconifyIcon icon="lucide:shield-check" />
          仅支持数字、指标引用、括号和 + - * /；除数为零时结果按 0
          处理，不执行任何脚本。
        </div>
      </div>
    </Modal>

    <Modal
      v-model:open="saveModalVisible"
      :confirm-loading="saving"
      title="另存分析方案"
      @ok="saveAsConfig"
    >
      <div class="save-modal">
        <label>方案名称</label>
        <Input
          v-model:value="saveForm.configName"
          :maxlength="100"
          placeholder="请输入方案名称"
        />
        <label>可见范围</label>
        <Select
          v-model:value="saveForm.scopeType"
          :options="[
            { label: '个人方案', value: 'PRIVATE' },
            { label: '租户共享', value: 'SHARED' },
          ]"
        />
        <label>方案说明</label>
        <Input.TextArea
          v-model:value="saveForm.remark"
          :maxlength="500"
          :rows="3"
        />
        <Checkbox v-model:checked="saveForm.defaultFlag">
          设为我的默认方案
        </Checkbox>
      </div>
    </Modal>
  </Page>
</template>

<style scoped>
.process-analysis-page {
  --analysis-blue: #155eef;
  --analysis-border: #dbe4f0;
  --analysis-muted: #64748b;
  --analysis-surface: #f8fafc;
  color: #172033;
}

:global(.process-analysis-content) {
  padding: 10px !important;
  overflow: hidden;
}

.analysis-shell {
  display: flex;
  min-height: 100%;
  flex-direction: column;
  gap: 10px;
}

.analysis-banner,
.analysis-query-bar,
.field-panel,
.config-panel,
.result-card {
  border: 1px solid var(--analysis-border);
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 1px 3px rgb(15 23 42 / 5%);
}

.analysis-banner {
  display: flex;
  min-height: 72px;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  gap: 14px;
}

.banner-copy {
  display: flex;
  min-width: 340px;
  align-items: center;
  gap: 12px;
}

.banner-icon {
  display: grid;
  width: 42px;
  height: 42px;
  flex: 0 0 auto;
  place-items: center;
  border-radius: 8px;
  background: linear-gradient(145deg, #155eef, #3b82f6);
  color: #fff;
  font-size: 22px;
}

.banner-title-row {
  display: flex;
  align-items: center;
  gap: 7px;
}

.banner-title-row h2 {
  margin: 0;
  color: #0f172a;
  font-size: 19px;
  font-weight: 700;
}

.banner-copy p {
  margin: 4px 0 0;
  color: var(--analysis-muted);
  font-size: 12px;
}

.banner-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
  gap: 6px;
}

.config-select {
  width: 190px;
}

.analysis-query-bar {
  display: flex;
  min-height: 54px;
  align-items: end;
  padding: 8px 12px;
  gap: 10px;
}

.browse-query-bar {
  border-color: #bfd3f8;
  background: linear-gradient(90deg, #f8fbff, #fff);
}

.browse-query-bar > :deep(.ant-checkbox-wrapper) {
  align-self: center;
  white-space: nowrap;
}

.query-item {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.query-item label {
  color: #475569;
  font-size: 12px;
}

.query-date {
  width: 250px;
}

.query-keyword {
  width: min(320px, 24vw);
}

.query-stats {
  display: flex;
  margin-left: auto;
  align-items: center;
  align-self: center;
  color: var(--analysis-muted);
  font-size: 12px;
  gap: 12px;
}

.query-stats strong {
  color: var(--analysis-blue);
}

.analysis-workspace {
  display: grid;
  height: calc(100vh - 208px);
  min-height: 600px;
  grid-template-columns: 248px minmax(0, 1fr) 330px;
  gap: 10px;
}

.analysis-workspace.browse-mode {
  height: calc(100vh - 128px);
  grid-template-columns: minmax(0, 1fr);
}

.field-panel,
.config-panel,
.result-panel {
  min-width: 0;
  min-height: 0;
}

.field-panel,
.config-panel {
  display: flex;
  overflow: hidden;
  flex-direction: column;
}

.panel-heading {
  display: flex;
  min-height: 52px;
  align-items: center;
  justify-content: space-between;
  padding: 9px 11px;
  border-bottom: 1px solid #e8eef6;
}

.panel-heading div {
  display: flex;
  flex-direction: column;
}

.panel-heading strong {
  font-size: 14px;
}

.panel-heading span {
  margin-top: 2px;
  color: var(--analysis-muted);
  font-size: 11px;
}

.field-search {
  width: auto;
  margin: 9px 10px 6px;
}

.field-list,
.config-panel {
  overflow-y: auto;
}

.field-list {
  padding: 0 8px 12px;
}

.field-group {
  margin-top: 9px;
}

.field-group-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 4px 3px;
  color: #334155;
  font-size: 12px;
  font-weight: 700;
}

.field-group-title em,
.config-section-title em {
  display: inline-grid;
  min-width: 20px;
  height: 20px;
  place-items: center;
  border-radius: 10px;
  background: #eaf1ff;
  color: var(--analysis-blue);
  font-size: 11px;
  font-style: normal;
}

.field-card {
  margin-top: 5px;
  padding: 7px 8px 5px;
  border: 1px solid #e3eaf3;
  border-radius: 6px;
  background: #fbfdff;
}

.field-card:hover {
  border-color: #9fbbf4;
  box-shadow: 0 2px 5px rgb(21 94 239 / 8%);
}

.field-card-main {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
}

.field-label {
  overflow: hidden;
  font-size: 12px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.field-card code,
.metric-config-card code {
  display: block;
  overflow: hidden;
  margin-top: 3px;
  color: #8492a6;
  font-size: 10px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.field-actions {
  display: flex;
  margin-top: 3px;
  justify-content: flex-end;
}

.field-actions :deep(.ant-btn) {
  height: 22px;
  padding-inline: 5px;
  font-size: 11px;
}

.detail-hint {
  margin-top: 5px;
  color: #b45309;
  font-size: 10px;
}

.result-panel {
  display: flex;
  overflow-y: auto;
  flex-direction: column;
  gap: 10px;
}

.result-card {
  overflow: hidden;
}

.pivot-card {
  flex: 0 0 auto;
}

.pivot-card.fill-parent {
  min-height: 100%;
  flex: 1 1 auto;
}

.pivot-card.fill-parent :deep(.ant-table-body) {
  min-height: var(--analysis-table-body-height);
}

.result-card-header {
  display: flex;
  min-height: 48px;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  border-bottom: 1px solid #e8eef6;
  background: linear-gradient(180deg, #fff, #fbfdff);
}

.result-card-header > div {
  display: flex;
  flex-direction: column;
}

.result-card-header > .result-card-actions {
  flex-direction: row;
  align-items: center;
  gap: 12px;
}

.result-card-actions :deep(.ant-checkbox-wrapper) {
  color: #334155;
  font-size: 13px;
}

.result-card-actions :deep(.ant-checkbox + span) {
  margin-top: 0;
  color: inherit;
  font-size: inherit;
}

.result-card-header strong {
  font-size: 14px;
}

.result-card-header span {
  margin-top: 2px;
  color: var(--analysis-muted);
  font-size: 11px;
}

.drill-breadcrumb {
  display: flex;
  min-height: 37px;
  align-items: center;
  overflow-x: auto;
  padding: 5px 10px;
  border-bottom: 1px solid #eef2f7;
  background: #f8fafc;
  color: #475569;
  font-size: 12px;
  gap: 3px;
  white-space: nowrap;
}

.drill-breadcrumb button {
  padding: 2px 5px;
  border: 0;
  background: transparent;
  color: var(--analysis-blue);
  cursor: pointer;
}

.drill-tip {
  margin-left: auto;
  color: #8492a6 !important;
}

.drill-link {
  display: inline-flex;
  max-width: 100%;
  align-items: center;
  border: 0;
  background: transparent;
  color: var(--analysis-blue);
  cursor: pointer;
  gap: 5px;
}

.drill-link span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dimension-cell {
  color: #334155;
  font-weight: 500;
}

.metric-cell {
  font-variant-numeric: tabular-nums;
}

.chart-card {
  min-height: 330px;
}

.analysis-chart {
  height: 290px;
}

.config-panel {
  padding-bottom: 12px;
}

.config-section {
  margin: 8px 9px 0;
  padding: 8px;
  border: 1px solid #e2e8f0;
  border-radius: 7px;
  background: #fbfdff;
}

.config-section-title {
  display: flex;
  min-height: 26px;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 5px;
  color: #334155;
  font-size: 12px;
  font-weight: 700;
}

.config-section-title > span {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.config-section :deep(.ant-empty) {
  margin-block: 6px;
}

.config-section :deep(.ant-empty-description) {
  font-size: 11px;
}

.config-pill {
  display: flex;
  min-height: 32px;
  align-items: center;
  justify-content: space-between;
  margin-top: 5px;
  padding-left: 8px;
  border: 1px solid #cfdcf0;
  border-radius: 5px;
  background: #fff;
  color: #334155;
  font-size: 12px;
}

.config-pill :deep(.ant-btn) {
  height: 26px;
  padding-inline: 5px;
}

.column-config-card {
  margin-top: 6px;
  padding: 0 6px 6px;
  border: 1px solid #cfdcf0;
  border-radius: 6px;
  background: #fff;
}

.column-config-title {
  margin: 0 -6px 5px;
  border: 0;
  border-bottom: 1px solid #e6edf6;
  border-radius: 0;
  background: #f8fbff;
}

.column-value-filter {
  width: 100%;
}

.column-filter-hint,
.auto-height-hint {
  display: block;
  margin-top: 4px;
  color: #94a3b8;
  font-size: 10px;
}

.metric-config-card,
.formula-card,
.filter-card {
  margin-top: 6px;
  padding: 7px;
  border: 1px solid #d9e3f0;
  border-radius: 6px;
  background: #fff;
}

.metric-config-title,
.filter-card-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
  font-weight: 600;
}

.metric-config-row {
  display: grid;
  margin-top: 6px;
  grid-template-columns: 1fr 62px;
  gap: 5px;
}

.order-actions {
  display: flex;
  justify-content: flex-end;
}

.order-actions :deep(.ant-btn) {
  height: 22px;
  padding-inline: 5px;
  font-size: 10px;
}

.formula-card > div:first-child {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 5px;
}

.formula-card strong {
  font-size: 12px;
}

.formula-card code {
  display: block;
  overflow-x: auto;
  margin: 6px 0 2px;
  padding: 4px 5px;
  border-radius: 3px;
  background: #f1f5f9;
  color: #475569;
  font-size: 10px;
  white-space: nowrap;
}

.filter-card {
  display: grid;
  gap: 5px;
}

.chart-config-grid {
  display: grid;
  align-items: center;
  grid-template-columns: 48px minmax(0, 1fr);
  gap: 6px;
}

.chart-config-grid label {
  color: #64748b;
  font-size: 11px;
}

.table-layout-row {
  display: grid;
  align-items: center;
  grid-template-columns: minmax(0, 1fr) 92px 20px;
  gap: 6px;
}

.table-layout-row label,
.column-width-heading {
  color: #64748b;
  font-size: 11px;
}

.column-width-heading {
  margin-top: 9px;
  padding-top: 7px;
  border-top: 1px dashed #d9e3f0;
}

.column-width-list {
  display: grid;
  max-height: 230px;
  margin-top: 4px;
  overflow-y: auto;
  gap: 4px;
}

.column-width-row {
  display: grid;
  align-items: center;
  grid-template-columns: minmax(0, 1fr) 82px;
  gap: 6px;
}

.column-width-row label {
  overflow: hidden;
  color: #475569;
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.formula-modal,
.save-modal {
  display: grid;
  align-items: start;
  grid-template-columns: 86px minmax(0, 1fr);
  gap: 12px 10px;
}

.formula-modal > label,
.save-modal > label {
  padding-top: 6px;
  color: #475569;
  font-size: 12px;
}

.formula-token-list,
.formula-help {
  grid-column: 2;
}

.formula-token-list {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 5px;
}

.formula-token-list > span {
  width: 100%;
  color: #64748b;
  font-size: 11px;
}

.formula-help {
  display: flex;
  align-items: flex-start;
  padding: 8px;
  border: 1px solid #bfdbfe;
  border-radius: 5px;
  background: #eff6ff;
  color: #1e40af;
  font-size: 11px;
  gap: 6px;
}

.save-modal :deep(.ant-checkbox-wrapper) {
  grid-column: 2;
}

:deep(.ant-table-wrapper .ant-table) {
  font-size: 12px;
}

:deep(.ant-table-wrapper .ant-table-thead > tr > th) {
  border-color: #d7e2ef;
  background: #edf4ff;
  color: #334155;
  font-weight: 600;
  text-align: center;
}

:deep(.ant-table-wrapper .ant-table-tbody > tr > td) {
  border-color: #e5ebf3;
}

@media (max-width: 1450px) {
  .analysis-banner {
    align-items: flex-start;
  }

  .banner-actions {
    max-width: 760px;
  }

  .analysis-workspace {
    grid-template-columns: 220px minmax(0, 1fr) 300px;
  }
}

@media (max-width: 1180px) {
  :global(.process-analysis-content) {
    overflow: auto;
  }

  .analysis-banner,
  .analysis-query-bar {
    flex-wrap: wrap;
  }

  .query-stats {
    width: 100%;
    margin-left: 0;
  }

  .analysis-workspace {
    height: auto;
    grid-template-columns: 1fr;
  }

  .analysis-workspace.browse-mode {
    height: auto;
  }

  .field-panel,
  .config-panel {
    max-height: 520px;
  }

  .field-list {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 8px;
  }
}
</style>
