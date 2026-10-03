<script lang="ts" setup>
import type { Dayjs } from 'dayjs';

import type { MesHcPlanOrderApi } from '#/api/mes/hc/planorder';
import type { MesHcPlanProcessPivotDailyApi } from '#/api/mes/hc/planorder/process-pivot-daily';

import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue';

import { Page, useVbenModal } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';
import { downloadFileFromBlobPart } from '@vben/utils';

import {
  Button,
  Checkbox,
  DatePicker,
  Input,
  message,
  Modal,
  Pagination,
  RangePicker,
  Select,
  Spin,
  Tag,
  Tooltip,
} from 'ant-design-vue';
import dayjs from 'dayjs';

import {
  comparePlanProcessPivotDaily,
  exportPlanProcessPivot,
  getPlanProcessPivotDailySyncStatus,
  getPlanProcessPivotPage,
  syncPlanProcessPivotDaily,
} from '#/api/mes/hc/planorder/process-pivot-daily';

import InspectionDetailModalForm from '../../../quality/shared/inspection-detail-modal.vue';
import { getPlanStatusMeta, PLAN_STATUS_OPTIONS } from '../plan-order/data';

import '../../package-fg/shared/cut-round-board.css';

interface StageColumn {
  code: string;
  label: string;
  batchLabel: string;
  metricColumns?: StageMetricColumn[];
  showBatch?: boolean;
}

interface StageMetricColumn {
  field: 'processLength' | 'startPosition';
  label: string;
}

type BaseColumnKey =
  | 'materialCode'
  | 'modelCode'
  | 'motherRollBatchNo'
  | 'planNo'
  | 'planStatus'
  | 'sizeSpec';
type ColumnWidthKey =
  | 'batch'
  | 'material'
  | 'model'
  | 'motherBatch'
  | 'plan'
  | 'size'
  | 'stageMetric'
  | 'stageQty'
  | 'stageTime'
  | 'status';
type StageSubColumnKey =
  | 'batch'
  | 'defectQty'
  | 'doneQty'
  | 'inspectionNgQty'
  | 'inspectionQty'
  | 'lastReportTime'
  | 'pendingQty'
  | StageMetricColumn['field'];

interface BaseColumn {
  className: string;
  key: BaseColumnKey;
  label: string;
  required?: boolean;
  sticky?: 'motherBatch' | 'plan' | 'status';
  widthKey: ColumnWidthKey;
}

interface StageSubColumn {
  className: string;
  key: StageSubColumnKey;
  label: string;
  metricField?: StageMetricColumn['field'];
  widthKey: ColumnWidthKey;
}

interface ColumnConfig {
  base: Record<BaseColumnKey, boolean>;
  stages: Record<string, Partial<Record<StageSubColumnKey, boolean>>>;
}

interface StoredColumnConfig extends Partial<ColumnConfig> {
  version?: number;
}

interface SelectedQtimeItem {
  actualLabel: string;
  actualText: string;
  message: string;
  sourceEndTimeText: string;
  stageCode: string;
  stageName: string;
  standardText: string;
  statusText: string;
  targetStartTimeText: string;
  timeout: boolean;
}

const STAGE_COLUMNS: StageColumn[] = [
  { code: 'FORMULA', label: '配料', batchLabel: '母批批号', showBatch: false },
  { code: 'WET', label: '湿法', batchLabel: '母批批号', showBatch: false },
  {
    batchLabel: '分段批号',
    code: 'GRINDING',
    label: '磨皮',
    metricColumns: [
      { field: 'startPosition', label: '起位置(m)' },
      { field: 'processLength', label: '长度(m)' },
    ],
  },
  { code: 'ADHESIVE1', label: '粘胶1', batchLabel: '分段批号' },
  { code: 'SLITTING', label: '分切', batchLabel: '片号', showBatch: false },
  { code: 'PRESS_SLOT', label: '压槽', batchLabel: '片号', showBatch: false },
  { code: 'ADHESIVE2', label: '粘胶2', batchLabel: '片号', showBatch: false },
  { code: 'CUT_ROUND', label: '裁切', batchLabel: '片号', showBatch: false },
  {
    code: 'SHIPPING_INSPECTION',
    label: '发货检验',
    batchLabel: '片号',
    showBatch: false,
  },
];

const PENDING_STAGE_CODES = new Set([
  'ADHESIVE2',
  'CUT_ROUND',
  'PRESS_SLOT',
  'SLITTING',
]);
const OUTPUT_BATCH_PREFERRED_STAGE_CODES = new Set([
  'ADHESIVE2',
  'CUT_ROUND',
  'PRESS_SLOT',
]);

type PieceDialogKind =
  | 'DEFECT'
  | 'DONE'
  | 'INSPECTION'
  | 'INSPECTION_NG'
  | 'PENDING';

interface DrilldownTableColumn {
  align?: 'center' | 'left' | 'right';
  key: string;
  label: string;
  minWidth?: number;
  width?: number;
}

interface PieceCardItem {
  [key: string]: unknown;
  actionText?: string;
  actualSizeSpec?: string;
  actualSizeSpecText?: string;
  actualModelCodeText?: string;
  coaFlag?: boolean;
  coaFlagText?: string;
  confirmStatus?: string;
  defectSummary?: string;
  inspectionId?: number;
  inspectionNo?: string;
  inspectionResult?: string;
  inspectionType?: string;
  inspectionFlagText?: string;
  lastReportTime?: string;
  ngType?: string;
  no: string;
  outputActualSizeSpecText?: string;
  pieceNo?: string;
  remark?: string;
  scanConfirmTime?: string;
  sourceType?: string;
  status: string;
  subtitle: string;
  qtyText: string;
}

const PIECE_REPORT_INFO_COLUMNS: DrilldownTableColumn[] = [
  { key: 'actualSizeSpecText', label: '尺寸', minWidth: 96 },
  { key: 'outputActualSizeSpecText', label: '实际尺寸', minWidth: 96 },
  { key: 'actualModelCodeText', label: '实际报工型号', minWidth: 128 },
];

const PIECE_DRILLDOWN_COLUMNS: DrilldownTableColumn[] = [
  { align: 'center', key: 'index', label: '序号', width: 56 },
  { key: 'pieceNo', label: '片号', minWidth: 188 },
  ...PIECE_REPORT_INFO_COLUMNS,
  { key: 'scanConfirmTime', label: '扫码确认时间', minWidth: 150 },
  { align: 'center', key: 'confirmStatus', label: '确认状态', width: 88 },
  { key: 'ngType', label: 'NG类型', minWidth: 136 },
  { align: 'center', key: 'coaFlagText', label: 'COA标记', width: 84 },
  { align: 'center', key: 'inspectionFlagText', label: '送检标记', width: 84 },
  { key: 'inspectionResult', label: '送检结果', minWidth: 108 },
];

const INSPECTION_DRILLDOWN_COLUMNS: DrilldownTableColumn[] = [
  { align: 'center', key: 'index', label: '序号', width: 56 },
  { key: 'pieceNo', label: '片号/批号', minWidth: 188 },
  { key: 'scanConfirmTime', label: '送检时间', minWidth: 150 },
  { align: 'right', key: 'inspectionQtyText', label: '送检数', minWidth: 94 },
  { align: 'right', key: 'inspectionNgQtyText', label: '送检NG', minWidth: 94 },
  { key: 'inspectionNo', label: '检验单号', minWidth: 150 },
  { key: 'inspectionType', label: '检验类型', minWidth: 110 },
  { key: 'inspectionResult', label: '送检结果', minWidth: 108 },
  { key: 'defectSummary', label: '缺陷摘要', minWidth: 150 },
];

const DEFAULT_PLAN_DATE_RANGE = getDefaultPlanDateRange(dayjs());
const DEFAULT_QUERY = {
  keyword: '',
  materialKeyword: '',
  modelCode: '',
  motherMaterialCode: '',
  motherModelCode: '',
  motherRollBatchNo: '',
  motherSegmentBatchNo: '',
  planNo: '',
  planStatuses: [] as string[],
  productionEndDateEnd: undefined as string | undefined,
  productionEndDateStart: undefined as string | undefined,
  productionStartDateEnd: formatDateRangeValue(DEFAULT_PLAN_DATE_RANGE.end),
  productionStartDateStart: formatDateRangeValue(DEFAULT_PLAN_DATE_RANGE.start),
  sizeSpec: '',
};

type PivotQuery = typeof DEFAULT_QUERY;
type PivotQueryFieldKey = Exclude<keyof PivotQuery, 'keyword'>;
type PivotQueryFieldConfig = {
  key: PivotQueryFieldKey;
  label: string;
  options?: { label: string; value: string }[];
  type?: 'date' | 'multiple';
};

const COLUMN_CONFIG_STORAGE_KEY =
  'mes:plan-process-pivot-daily:column-config:v1';
const COLUMN_CONFIG_VERSION = 8;

const COLUMN_WIDTHS = {
  batch: 126,
  material: 132,
  model: 112,
  motherBatch: 128,
  plan: 132,
  size: 78,
  stageMetric: 78,
  stageQty: 76,
  stageTime: 96,
  status: 78,
};

const BASE_COLUMNS: BaseColumn[] = [
  {
    className: 'col-plan',
    key: 'planNo',
    label: '计划号',
    required: true,
    sticky: 'plan',
    widthKey: 'plan',
  },
  {
    className: 'col-mother-batch',
    key: 'motherRollBatchNo',
    label: '母批批号',
    sticky: 'motherBatch',
    widthKey: 'motherBatch',
  },
  {
    className: 'col-status',
    key: 'planStatus',
    label: '状态',
    sticky: 'status',
    widthKey: 'status',
  },
  {
    className: 'col-model',
    key: 'modelCode',
    label: '型号',
    widthKey: 'model',
  },
  {
    className: 'col-material',
    key: 'materialCode',
    label: '料号',
    widthKey: 'material',
  },
  {
    className: 'col-size',
    key: 'sizeSpec',
    label: '尺寸',
    widthKey: 'size',
  },
];

const loading = ref(false);
const rows = ref<MesHcPlanOrderApi.ProcessPivotRow[]>([]);
const total = ref(0);
const pageNo = ref(1);
const pageSize = ref(20);
const selectedPivotRowKey = ref('');
const qtimeNowTimestamp = ref(Date.now());
const qtimeDetailExpanded = ref(false);
const advancedQueryVisible = ref(false);
const columnSettingVisible = ref(false);
const exporting = ref(false);
const comparing = ref(false);
const syncing = ref(false);
const syncStatus = ref<MesHcPlanProcessPivotDailyApi.SyncStatus>();
const selectedPlanDateRange = ref<[Dayjs, Dayjs] | null>([
  DEFAULT_PLAN_DATE_RANGE.start,
  DEFAULT_PLAN_DATE_RANGE.end,
]);
const columnConfig = reactive<ColumnConfig>(loadColumnConfig());
const queryForm = reactive({
  ...DEFAULT_QUERY,
  planStatuses: [...DEFAULT_QUERY.planStatuses],
});
const pieceDialog = reactive({
  columns: [] as DrilldownTableColumn[],
  kind: 'DONE' as PieceDialogKind,
  keyword: '',
  open: false,
  quantityText: '',
  stageName: '',
  subtitle: '',
  title: '',
  items: [] as PieceCardItem[],
});
let qtimeRefreshTimer: ReturnType<typeof window.setInterval> | undefined;
let fetchRequestSeq = 0;
const [InspectionDetailModal, inspectionDetailModalApi] = useVbenModal({
  connectedComponent: InspectionDetailModalForm,
  destroyOnClose: true,
});

const totalColumnCount = computed(
  () =>
    visibleBaseColumns.value.length +
    visibleStageColumns.value.reduce(
      (sum, stage) => sum + stageColumnCount(stage),
      0,
    ),
);
const pivotTableWidth = computed(() => {
  let width = visibleBaseColumns.value.reduce(
    (sum, column) => sum + COLUMN_WIDTHS[column.widthKey],
    0,
  );
  for (const stage of visibleStageColumns.value) {
    width += visibleStageSubColumns(stage).reduce(
      (sum, column) => sum + COLUMN_WIDTHS[column.widthKey],
      0,
    );
  }
  return width;
});
const pivotTableStyle = computed(() => ({
  minWidth: `${pivotTableWidth.value}px`,
  width: `${pivotTableWidth.value}px`,
}));
const motherBatchStickyStyle = computed(() => ({
  left: isBaseColumnVisible('planNo') ? `${COLUMN_WIDTHS.plan}px` : '0',
}));
const stickyStatusOffset = computed(() => {
  let left = 0;
  if (isBaseColumnVisible('planNo')) {
    left += COLUMN_WIDTHS.plan;
  }
  if (isBaseColumnVisible('motherRollBatchNo')) {
    left += COLUMN_WIDTHS.motherBatch;
  }
  return left;
});
const statusStickyStyle = computed(() => ({
  left: `${stickyStatusOffset.value}px`,
}));
const visibleBaseColumns = computed(() =>
  BASE_COLUMNS.filter((column) => isBaseColumnVisible(column.key)),
);
const visibleStageColumns = computed(() =>
  STAGE_COLUMNS.filter((stage) => visibleStageSubColumns(stage).length > 0),
);
const activeFilterCount = computed(() => {
  return Object.entries(queryForm).filter(([, value]) => {
    if (Array.isArray(value)) return value.length > 0;
    return value !== null && value !== undefined && String(value).trim() !== '';
  }).length;
});
const currentPagePlanCount = computed(
  () => new Set(rows.value.map((row) => planRowKey(row)).filter(Boolean)).size,
);
const syncStatusMetaValue = computed(() =>
  syncStatus.value?.latestSyncTime ? '已同步' : '未同步',
);
const syncStatusMetaSub = computed(() =>
  syncStatus.value?.latestStatDate
    ? formatLocalDateValue(syncStatus.value.latestStatDate)
    : '快照',
);
const syncStatusText = computed(() => {
  if (!syncStatus.value?.latestSyncTime) return '日结快照尚未同步';
  return `日结快照至 ${formatLocalDateValue(syncStatus.value.latestStatDate)}，更新于 ${formatDateTime(syncStatus.value.latestSyncTime)}`;
});
const selectedPivotRow = computed(
  () =>
    rows.value.find(
      (row) => resolvePivotRowKey(row) === selectedPivotRowKey.value,
    ) || null,
);
const selectedQtimeItems = computed(() =>
  buildSelectedQtimeItems(selectedPivotRow.value),
);
const selectedQtimeTimeoutCount = computed(
  () => selectedQtimeItems.value.filter((item) => item.timeout).length,
);
const selectedQtimeSummaryText = computed(() => {
  if (!selectedPivotRow.value) return '未选中行';
  const totalCount = selectedQtimeItems.value.length;
  if (totalCount === 0) return '当前行暂无 QTIME';
  return selectedQtimeTimeoutCount.value > 0
    ? `共 ${totalCount} 项，超时 ${selectedQtimeTimeoutCount.value} 项`
    : `共 ${totalCount} 项，无超时`;
});
const queryFieldConfigs: PivotQueryFieldConfig[] = [
  {
    key: 'planStatuses',
    label: '计划状态',
    options: PLAN_STATUS_OPTIONS,
    type: 'multiple',
  },
  { key: 'planNo', label: '计划号' },
  { key: 'motherRollBatchNo', label: '母批批号' },
  { key: 'motherSegmentBatchNo', label: '分段批号' },
  { key: 'modelCode', label: '型号' },
  { key: 'materialKeyword', label: '料号' },
  { key: 'sizeSpec', label: '尺寸规格' },
  { key: 'productionStartDateStart', label: '开始日期起', type: 'date' },
  { key: 'productionStartDateEnd', label: '开始日期止', type: 'date' },
  { key: 'productionEndDateStart', label: '结束日期起', type: 'date' },
  { key: 'productionEndDateEnd', label: '结束日期止', type: 'date' },
];
const planRowSpanMap = computed(() => {
  return buildRowSpanMap(rows.value, planRowKey);
});
const modelSizeRowSpanMap = computed(() => {
  return buildRowSpanMap(rows.value, modelSizeRowKey);
});
const stageRowSpanMap = computed(() => {
  const spanMap = new Map<string, { firstIndex: number; rowSpan: number }>();
  STAGE_COLUMNS.forEach((stage) => {
    const stageMap = buildRowSpanMap(rows.value, (row) =>
      stageRowKey(row, stage.code),
    );
    stageMap.forEach((value, index) => {
      spanMap.set(`${stage.code}|${index}`, value);
    });
  });
  return spanMap;
});
const filteredPieceItems = computed(() => {
  const keyword = pieceDialog.keyword.trim().toLowerCase();
  if (!keyword) {
    return pieceDialog.items;
  }
  return pieceDialog.items.filter((item) =>
    [
      item.no,
      item.pieceNo,
      item.status,
      item.subtitle,
      item.actualSizeSpec,
      item.actualSizeSpecText,
      item.outputActualSizeSpecText,
      item.actualModelCodeText,
      item.confirmStatus,
      item.ngType,
      item.coaFlagText,
      item.inspectionNo,
      item.inspectionType,
      item.inspectionResult,
      item.inspectionFlagText,
      item.defectSummary,
      item.remark,
    ]
      .filter(Boolean)
      .some((value) => String(value).toLowerCase().includes(keyword)),
  );
});

function getDefaultPlanDateRange(value: Dayjs) {
  const mondayOffset = (value.day() + 6) % 7;
  const start = value.subtract(mondayOffset, 'day').startOf('day');
  return {
    end: start.add(6, 'day').startOf('day'),
    start,
  };
}

function formatDateRangeValue(value: Dayjs) {
  return value.format('YYYY-MM-DD');
}

function formatLocalDateValue(value?: number[] | string) {
  if (Array.isArray(value) && value.length >= 3) {
    return `${value[0]}-${String(value[1]).padStart(2, '0')}-${String(value[2]).padStart(2, '0')}`;
  }
  return value || '-';
}

function applyPlanDateRange(value: [Dayjs, Dayjs] | null, refresh = true) {
  if (value) {
    selectedPlanDateRange.value = value;
    queryForm.productionStartDateStart = formatDateRangeValue(value[0]);
    queryForm.productionStartDateEnd = formatDateRangeValue(value[1]);
  } else {
    selectedPlanDateRange.value = null;
    queryForm.productionStartDateStart = undefined;
    queryForm.productionStartDateEnd = undefined;
  }
  if (refresh) handleSearch();
}

function syncPlanDateRangeFromProductionStartRange() {
  const startValue = queryForm.productionStartDateStart;
  const endValue = queryForm.productionStartDateEnd;
  if (!startValue || !endValue) {
    selectedPlanDateRange.value = null;
    return;
  }
  const start = dayjs(startValue).startOf('day');
  const end = dayjs(endValue).startOf('day');
  selectedPlanDateRange.value =
    start.isValid() && end.isValid() ? ([start, end] as [Dayjs, Dayjs]) : null;
}

function createDefaultColumnConfig(): ColumnConfig {
  const base = {} as Record<BaseColumnKey, boolean>;
  BASE_COLUMNS.forEach((column) => {
    base[column.key] = isDefaultBaseColumnVisible(column);
  });
  const stages: ColumnConfig['stages'] = {};
  STAGE_COLUMNS.forEach((stage) => {
    stages[stage.code] = {};
    stageSubColumns(stage).forEach((column) => {
      stages[stage.code]![column.key] = isDefaultStageSubColumnVisible(
        stage,
        column,
      );
    });
  });
  return { base, stages };
}

function normalizeColumnConfig(value?: Partial<ColumnConfig>): ColumnConfig {
  const normalized = createDefaultColumnConfig();
  BASE_COLUMNS.forEach((column) => {
    const hasSourceValue = Object.prototype.hasOwnProperty.call(
      value?.base || {},
      column.key,
    );
    if (column.required) {
      normalized.base[column.key] = true;
    } else if (hasSourceValue) {
      normalized.base[column.key] = value?.base?.[column.key] !== false;
    } else {
      normalized.base[column.key] = isDefaultBaseColumnVisible(column);
    }
  });
  STAGE_COLUMNS.forEach((stage) => {
    const source = value?.stages?.[stage.code] || {};
    stageSubColumns(stage).forEach((column) => {
      const hasSourceValue = Object.prototype.hasOwnProperty.call(
        source,
        column.key,
      );
      normalized.stages[stage.code]![column.key] = hasSourceValue
        ? source[column.key] !== false
        : isDefaultStageSubColumnVisible(stage, column);
    });
  });
  return normalized;
}

function applyColumnConfigMigration(
  config: ColumnConfig,
  source?: StoredColumnConfig,
) {
  if ((source?.version || 0) >= COLUMN_CONFIG_VERSION) {
    return config;
  }
  config.stages.WET = {
    ...config.stages.WET,
    inspectionNgQty: false,
    inspectionQty: false,
  };
  config.stages.SHIPPING_INSPECTION = {
    ...config.stages.SHIPPING_INSPECTION,
    inspectionNgQty: false,
    inspectionQty: false,
  };
  return config;
}

function loadColumnConfig(): ColumnConfig {
  if (typeof window === 'undefined') return createDefaultColumnConfig();
  try {
    const rawConfig = window.localStorage.getItem(COLUMN_CONFIG_STORAGE_KEY);
    const parsed = rawConfig
      ? (JSON.parse(rawConfig) as StoredColumnConfig)
      : undefined;
    return applyColumnConfigMigration(normalizeColumnConfig(parsed), parsed);
  } catch {
    return createDefaultColumnConfig();
  }
}

function persistColumnConfig() {
  if (typeof window === 'undefined') return;
  window.localStorage.setItem(
    COLUMN_CONFIG_STORAGE_KEY,
    JSON.stringify({
      base: columnConfig.base,
      stages: columnConfig.stages,
      version: COLUMN_CONFIG_VERSION,
    }),
  );
}

function applyColumnConfig(nextConfig: ColumnConfig) {
  BASE_COLUMNS.forEach((column) => {
    columnConfig.base[column.key] = nextConfig.base[column.key];
  });
  STAGE_COLUMNS.forEach((stage) => {
    const target = columnConfig.stages[stage.code] as Record<string, boolean>;
    Object.keys(target).forEach((key) => delete target[key]);
    stageSubColumns(stage).forEach((column) => {
      target[column.key] =
        nextConfig.stages[stage.code]?.[column.key] !== false;
    });
  });
}

function isBaseColumnVisible(key: BaseColumnKey) {
  const column = BASE_COLUMNS.find((item) => item.key === key);
  return Boolean(column?.required || columnConfig.base[key] !== false);
}

function isDefaultBaseColumnVisible(column: BaseColumn) {
  return column.key !== 'materialCode';
}

function setBaseColumnVisible(key: BaseColumnKey, visible: boolean) {
  const column = BASE_COLUMNS.find((item) => item.key === key);
  if (column?.required && !visible) return;
  columnConfig.base[key] = visible;
  persistColumnConfig();
}

function isStageSubColumnVisible(stage: StageColumn, key: StageSubColumnKey) {
  return columnConfig.stages[stage.code]?.[key] !== false;
}

function setStageSubColumnVisible(
  stageCode: string,
  key: StageSubColumnKey,
  visible: boolean,
) {
  if (!columnConfig.stages[stageCode]) {
    columnConfig.stages[stageCode] = {};
  }
  columnConfig.stages[stageCode]![key] = visible;
  persistColumnConfig();
}

function resetColumnConfig() {
  applyColumnConfig(createDefaultColumnConfig());
  persistColumnConfig();
  message.success('列设置已恢复默认');
}

function getCheckedFromEvent(
  event: Event | { target?: { checked?: boolean } },
) {
  return Boolean((event as { target?: { checked?: boolean } }).target?.checked);
}

function setQueryFieldValue(
  key: PivotQueryFieldKey,
  value?: string | string[],
) {
  if (key === 'planStatuses') {
    if (Array.isArray(value)) {
      queryForm.planStatuses = [...value];
    } else {
      queryForm.planStatuses = value ? [value] : [];
    }
    return;
  }
  (queryForm as Record<string, any>)[key] = Array.isArray(value)
    ? value[0]
    : value || undefined;
}

function clearAdvancedQueryValues() {
  queryFieldConfigs.forEach((config) => setQueryFieldValue(config.key));
  selectedPlanDateRange.value = null;
}

function clearAdvancedQuery() {
  clearAdvancedQueryValues();
}

function openAdvancedQuery() {
  advancedQueryVisible.value = true;
}

function applyAdvancedQuery() {
  advancedQueryVisible.value = false;
  syncPlanDateRangeFromProductionStartRange();
  handleSearch();
}

function buildQueryParams() {
  const params: MesHcPlanOrderApi.ProcessPivotPageReqVO = {
    pageNo: pageNo.value,
    pageSize: pageSize.value,
  };
  Object.entries(queryForm).forEach(([key, value]) => {
    if (Array.isArray(value)) {
      if (value.length > 0) {
        (params as Record<string, unknown>)[key] = value;
      }
      return;
    }
    if (value !== null && value !== undefined && String(value).trim() !== '') {
      (params as Record<string, unknown>)[key] = String(value).trim();
    }
  });
  return params;
}

function buildExportColumnParams() {
  return {
    exportBaseColumns: visibleBaseColumns.value.map((column) => column.key),
    exportStageColumns: visibleStageColumns.value.flatMap((stage) =>
      visibleStageSubColumns(stage).map(
        (column) => `${stage.code}.${column.key}`,
      ),
    ),
  };
}

function buildExportQueryParams() {
  const params = { ...buildQueryParams() } as Record<string, unknown>;
  delete params.pageNo;
  delete params.pageSize;
  Object.assign(params, buildExportColumnParams());
  return params as MesHcPlanOrderApi.ProcessPivotPageReqVO;
}

async function fetchData() {
  const requestSeq = ++fetchRequestSeq;
  loading.value = true;
  try {
    const result = await getPlanProcessPivotPage(buildQueryParams());
    if (requestSeq !== fetchRequestSeq) return;
    rows.value = result.list || [];
    total.value = Number(result.total || 0);
    syncSelectedPivotRow();
  } catch (error) {
    if (requestSeq === fetchRequestSeq) {
      throw error;
    }
  } finally {
    if (requestSeq === fetchRequestSeq) {
      loading.value = false;
    }
  }
}

async function loadSyncStatus() {
  try {
    syncStatus.value = await getPlanProcessPivotDailySyncStatus();
  } catch (error) {
    console.warn('加载生产进度日结同步状态失败', error);
  }
}

function handleSearch() {
  pageNo.value = 1;
  fetchData();
}

function handlePlanDateRangeChange(
  value: (Dayjs | null)[] | [Dayjs, Dayjs] | null,
) {
  if (!value || !value[0] || !value[1]) {
    applyPlanDateRange(null);
    return;
  }
  applyPlanDateRange([value[0], value[1]]);
}

function handleShiftPlanDateRange(offset: number) {
  const [start, end] = selectedPlanDateRange.value || [
    dayjs().startOf('week').add(1, 'day'),
    dayjs().startOf('week').add(7, 'day'),
  ];
  applyPlanDateRange([start.add(offset, 'week'), end.add(offset, 'week')] as [
    Dayjs,
    Dayjs,
  ]);
}

async function handleExportExcel() {
  const hideLoading = message.loading({
    content: '正在导出生产进度（日结版）...',
    duration: 0,
  });
  exporting.value = true;
  try {
    const data = await exportPlanProcessPivot(buildExportQueryParams());
    downloadFileFromBlobPart({
      fileName: '生产进度（日结版）.xlsx',
      source: data,
    });
    message.success('生产进度（日结版）已导出');
  } finally {
    exporting.value = false;
    hideLoading();
  }
}

async function handleSyncDaily() {
  const hideLoading = message.loading({
    content: '正在同步生产进度日结快照...',
    duration: 0,
  });
  syncing.value = true;
  try {
    const result = await syncPlanProcessPivotDaily();
    message.success(
      `同步完成：新增 ${result.insertedRowCount} 行，更新 ${result.updatedRowCount} 行，移除 ${result.removedRowCount} 行，未变 ${result.alreadySettledRowCount} 行，耗时 ${(Number(result.durationMs || 0) / 1000).toFixed(1)} 秒`,
    );
    await Promise.all([loadSyncStatus(), fetchData()]);
  } catch (error) {
    console.error(error);
    message.error('生产进度日结同步失败，请检查服务端日志');
  } finally {
    syncing.value = false;
    hideLoading();
  }
}

async function handleCompareDaily() {
  comparing.value = true;
  try {
    const result = await comparePlanProcessPivotDaily(buildQueryParams());
    const mismatchCount =
      Number(result.missingSnapshotCount || 0) +
      Number(result.staleSnapshotCount || 0) +
      Number(result.extraSnapshotCount || 0);
    if (mismatchCount === 0) {
      message.success('数据一致');
      return;
    }
    Modal.warning({
      content:
        '当前数据和实时生产进度存在差异，请点击“同步”。同步完成后数据即可保持一致。',
      title: '数据存在差异',
    });
  } catch (error) {
    console.error(error);
    message.error('生产进度日结比对失败，请检查服务端日志');
  } finally {
    comparing.value = false;
  }
}

function handleReset() {
  const defaultRange = getDefaultPlanDateRange(dayjs());
  Object.assign(queryForm, {
    ...DEFAULT_QUERY,
    productionStartDateEnd: formatDateRangeValue(defaultRange.end),
    productionStartDateStart: formatDateRangeValue(defaultRange.start),
    planStatuses: [...DEFAULT_QUERY.planStatuses],
  });
  applyPlanDateRange([defaultRange.start, defaultRange.end], false);
  advancedQueryVisible.value = false;
  pageNo.value = 1;
  fetchData();
}

function handlePageChange(current: number, size: number) {
  pageNo.value = current;
  pageSize.value = size;
  fetchData();
}

function stageOf(row: MesHcPlanOrderApi.ProcessPivotRow, code: string) {
  return (row.stages?.[code] || {}) as MesHcPlanOrderApi.ProcessPivotStage;
}

function buildRowSpanMap(
  sourceRows: MesHcPlanOrderApi.ProcessPivotRow[],
  keyGetter: (row: MesHcPlanOrderApi.ProcessPivotRow) => string,
) {
  const spanMap = new Map<number, { firstIndex: number; rowSpan: number }>();
  let currentKey = '';
  let firstIndex = 0;
  let rowSpan = 0;
  const flush = () => {
    if (!rowSpan) {
      return;
    }
    const spanInfo = { firstIndex, rowSpan };
    for (let index = firstIndex; index < firstIndex + rowSpan; index += 1) {
      spanMap.set(index, spanInfo);
    }
  };
  sourceRows.forEach((row, index) => {
    const key = keyGetter(row);
    if (index === 0 || key !== currentKey) {
      flush();
      currentKey = key;
      firstIndex = index;
      rowSpan = 1;
      return;
    }
    rowSpan += 1;
  });
  flush();
  return spanMap;
}

function planRowKey(row: MesHcPlanOrderApi.ProcessPivotRow) {
  return row.planMergeKey || String(row.id || row.planNo || '');
}

function resolvePivotRowKey(row: MesHcPlanOrderApi.ProcessPivotRow) {
  return (
    row.pivotRowKey ||
    `${row.id || row.planNo}-${segmentText(row)}-${modelText(row)}-${sizeText(row)}`
  );
}

function isSelectedPivotRow(row: MesHcPlanOrderApi.ProcessPivotRow) {
  return resolvePivotRowKey(row) === selectedPivotRowKey.value;
}

function handleSelectPivotRow(row: MesHcPlanOrderApi.ProcessPivotRow) {
  selectedPivotRowKey.value = resolvePivotRowKey(row);
}

function syncSelectedPivotRow() {
  if (rows.value.length === 0) {
    selectedPivotRowKey.value = '';
    return;
  }
  if (
    !rows.value.some(
      (row) => resolvePivotRowKey(row) === selectedPivotRowKey.value,
    )
  ) {
    selectedPivotRowKey.value = resolvePivotRowKey(rows.value[0]!);
  }
}

function modelSizeRowKey(row: MesHcPlanOrderApi.ProcessPivotRow) {
  return (
    row.modelSizeMergeKey ||
    `${planRowKey(row)}|${modelText(row)}|${sizeText(row)}`
  );
}

function stageRowKey(
  row: MesHcPlanOrderApi.ProcessPivotRow,
  stageCode: string,
) {
  return (
    row.stageMergeKeys?.[stageCode] || `${modelSizeRowKey(row)}|${stageCode}`
  );
}

function showStageBatch(stage: StageColumn) {
  return stage.showBatch !== false;
}

function stageMetricColumns(stage: StageColumn) {
  return stage.metricColumns || [];
}

function stageSubColumns(stage: StageColumn): StageSubColumn[] {
  const columns: StageSubColumn[] = [];
  if (showStageBatch(stage)) {
    columns.push({
      className: 'col-batch',
      key: 'batch',
      label: stage.batchLabel,
      widthKey: 'batch',
    });
  }
  stageMetricColumns(stage).forEach((metric) => {
    columns.push({
      className: 'col-stage-metric',
      key: metric.field,
      label: metric.label,
      metricField: metric.field,
      widthKey: 'stageMetric',
    });
  });
  if (stage.code === 'SHIPPING_INSPECTION') {
    columns.push(
      {
        className: 'col-stage-qty',
        key: 'inspectionQty',
        label: '送检',
        widthKey: 'stageQty',
      },
      {
        className: 'col-stage-qty',
        key: 'inspectionNgQty',
        label: '检验NG',
        widthKey: 'stageQty',
      },
      {
        className: 'col-stage-time',
        key: 'lastReportTime',
        label: '最后时间',
        widthKey: 'stageTime',
      },
    );
    return columns;
  }
  columns.push(
    {
      className: 'col-stage-qty',
      key: 'doneQty',
      label: '完工',
      widthKey: 'stageQty',
    },
    {
      className: 'col-stage-qty',
      key: 'inspectionQty',
      label: '送检',
      widthKey: 'stageQty',
    },
    {
      className: 'col-stage-qty',
      key: 'inspectionNgQty',
      label: '检验NG',
      widthKey: 'stageQty',
    },
  );
  if (PENDING_STAGE_CODES.has(stage.code)) {
    columns.push({
      className: 'col-stage-qty',
      key: 'pendingQty',
      label: '未加工',
      widthKey: 'stageQty',
    });
  }
  columns.push(
    {
      className: 'col-stage-qty',
      key: 'defectQty',
      label: 'NG/损耗',
      widthKey: 'stageQty',
    },
    {
      className: 'col-stage-time',
      key: 'lastReportTime',
      label: '最后时间',
      widthKey: 'stageTime',
    },
  );
  return columns;
}

function isDefaultStageSubColumnVisible(
  stage: StageColumn,
  column: StageSubColumn,
) {
  if (column.key === 'batch' && stage.code === 'ADHESIVE1') {
    return false;
  }
  if (column.key === 'defectQty' || column.key === 'lastReportTime') {
    return false;
  }
  if (column.key === 'inspectionQty' || column.key === 'inspectionNgQty') {
    return false;
  }
  if (
    stage.code === 'GRINDING' &&
    (column.key === 'startPosition' || column.key === 'processLength')
  ) {
    return false;
  }
  return true;
}

function visibleStageSubColumns(stage: StageColumn) {
  return stageSubColumns(stage).filter((column) =>
    isStageSubColumnVisible(stage, column.key),
  );
}

function stageColumnCount(stage: StageColumn) {
  return visibleStageSubColumns(stage).length;
}

function isStageQtimeTimeout(stage?: MesHcPlanOrderApi.ProcessPivotStage) {
  return stage?.qtime?.timeout || stage?.qtime?.status === 'TIMEOUT';
}

function stageQtimeTitle(stage?: MesHcPlanOrderApi.ProcessPivotStage) {
  return isStageQtimeTimeout(stage)
    ? stage?.qtime?.message || 'QTIME 超时'
    : undefined;
}

function parseQtimeDateTime(value?: string) {
  if (!value) return null;
  const parsed = dayjs(value);
  return parsed.isValid() ? parsed : null;
}

function qtimeElapsedMinutes(qtime?: MesHcPlanOrderApi.QtimeInfo | null) {
  if (!qtime?.sourceEndTime) return qtime?.elapsedMinutes;
  const sourceTime = parseQtimeDateTime(qtime.sourceEndTime);
  if (!sourceTime) return qtime.elapsedMinutes;
  const targetTime = qtime.targetStartTime
    ? parseQtimeDateTime(qtime.targetStartTime)
    : dayjs(qtimeNowTimestamp.value);
  return targetTime
    ? Math.max(0, targetTime.diff(sourceTime, 'minute'))
    : qtime.elapsedMinutes;
}

function formatQtimeDuration(totalMinutes?: null | number) {
  if (
    totalMinutes === undefined ||
    totalMinutes === null ||
    Number.isNaN(Number(totalMinutes))
  ) {
    return '-';
  }
  const safeMinutes = Math.max(0, Math.floor(Number(totalMinutes)));
  const hours = Math.floor(safeMinutes / 60);
  const minutes = safeMinutes % 60;
  if (hours > 0 && minutes > 0) return `${hours}小时${minutes}分钟`;
  if (hours > 0) return `${hours}小时`;
  return `${safeMinutes}分钟`;
}

function isQtimeTimeout(qtime?: MesHcPlanOrderApi.QtimeInfo | null) {
  const elapsedMinutes = qtimeElapsedMinutes(qtime);
  if (
    elapsedMinutes !== undefined &&
    elapsedMinutes !== null &&
    qtime?.standardMinutes !== undefined &&
    qtime.standardMinutes !== null
  ) {
    return elapsedMinutes > qtime.standardMinutes;
  }
  return !!qtime?.timeout || qtime?.status === 'TIMEOUT';
}

function qtimeStatusText(qtime?: MesHcPlanOrderApi.QtimeInfo | null) {
  if (!qtime) return '无规则';
  if (qtime.status === 'MISSING_SOURCE_TIME') return '上道未完工';
  if (qtime.status === 'NO_RULE') return '未配置';
  if (isQtimeTimeout(qtime)) return '超时';
  if (qtime.status === 'NORMAL') return '正常';
  return qtime.status || '-';
}

function qtimeItemClass(item: SelectedQtimeItem) {
  return ['qtime-detail-card', item.timeout ? 'qtime-detail-card--timeout' : '']
    .filter(Boolean)
    .join(' ');
}

function buildQtimeMessage(
  qtime: MesHcPlanOrderApi.QtimeInfo,
  stageName: string,
) {
  if (qtime.status === 'MISSING_SOURCE_TIME')
    return '上道工序还没有完工时间，暂不能计算 QTIME。';
  if (qtime.status === 'NO_RULE')
    return '当前型号/批号没有匹配到额定 QTIME 配置。';
  const elapsedText = formatQtimeDuration(qtimeElapsedMinutes(qtime));
  const standardText = formatQtimeDuration(qtime.standardMinutes);
  const targetText = qtime.targetStarted
    ? `实际${stageName}开工`
    : `当前${stageName}开工`;
  return isQtimeTimeout(qtime)
    ? `${targetText}间隔 ${elapsedText}，已超出额定 ${standardText}`
    : `${targetText}间隔 ${elapsedText}，额定 ${standardText} 内`;
}

function buildSelectedQtimeItems(
  row?: MesHcPlanOrderApi.ProcessPivotRow | null,
) {
  if (!row?.stages) return [];
  return STAGE_COLUMNS.map((stageMeta) => {
    const stage = stageOf(row, stageMeta.code);
    const qtime = stage.qtime;
    if (!qtime) return null;
    const targetStartTimeText = qtime.targetStartTime
      ? `开工 ${formatDateTime(qtime.targetStartTime)}`
      : '未开工，按当前时间';
    return {
      actualLabel: qtime.targetStarted ? '实际时长' : '当前时长',
      actualText: formatQtimeDuration(qtimeElapsedMinutes(qtime)),
      message: buildQtimeMessage(qtime, stageMeta.label),
      sourceEndTimeText: formatDateTime(qtime.sourceEndTime),
      stageCode: stageMeta.code,
      stageName: stageMeta.label,
      standardText: formatQtimeDuration(qtime.standardMinutes),
      statusText: qtimeStatusText(qtime),
      targetStartTimeText,
      timeout: isQtimeTimeout(qtime),
    } as SelectedQtimeItem;
  }).filter(Boolean) as SelectedQtimeItem[];
}

function stageCellClass(
  column: StageSubColumn,
  stage?: MesHcPlanOrderApi.ProcessPivotStage,
) {
  let baseClass = 'num';
  switch (column.key) {
    case 'batch':
    case 'lastReportTime': {
      baseClass = 'text-cell';

      break;
    }
    case 'defectQty': {
      baseClass = 'num defect-cell';

      break;
    }
    case 'inspectionNgQty': {
      baseClass = 'num inspection-ng-cell';

      break;
    }
    case 'pendingQty': {
      baseClass = 'num pending-cell';

      break;
    }
    // No default
  }
  return [baseClass, isStageQtimeTimeout(stage) ? 'qtime-timeout-cell' : '']
    .filter(Boolean)
    .join(' ');
}

function canOpenPieceList(stage: StageColumn) {
  return !showStageBatch(stage);
}

function isFirstPlanRow(
  _row: MesHcPlanOrderApi.ProcessPivotRow,
  index: number,
) {
  return planRowSpanMap.value.get(index)?.firstIndex === index;
}

function planRowSpan(index: number) {
  return planRowSpanMap.value.get(index)?.rowSpan || 1;
}

function isFirstModelSizeRow(
  _row: MesHcPlanOrderApi.ProcessPivotRow,
  index: number,
) {
  return modelSizeRowSpanMap.value.get(index)?.firstIndex === index;
}

function modelSizeRowSpan(index: number) {
  return modelSizeRowSpanMap.value.get(index)?.rowSpan || 1;
}

function isFirstStageRow(
  _row: MesHcPlanOrderApi.ProcessPivotRow,
  index: number,
  stageCode: string,
) {
  const key = `${stageCode}|${index}`;
  return stageRowSpanMap.value.get(key)?.firstIndex === index;
}

function stageRowSpan(
  _row: MesHcPlanOrderApi.ProcessPivotRow,
  index: number,
  stageCode: string,
) {
  const key = `${stageCode}|${index}`;
  return stageRowSpanMap.value.get(key)?.rowSpan || 1;
}

function formatDateTime(value?: string) {
  if (!value) return '-';
  return String(value).replace('T', ' ').slice(0, 16);
}

function formatNumber(value?: null | number | string, digits = 3) {
  if (value === null || value === undefined || value === '') return '-';
  const number = Number(value);
  if (!Number.isFinite(number)) return '-';
  return number.toLocaleString('zh-CN', {
    maximumFractionDigits: digits,
    minimumFractionDigits: 0,
  });
}

function formatQty(value?: null | number | string, unit?: string, digits = 3) {
  const text = formatNumber(value, digits);
  if (text === '-') return '-';
  return unit ? `${text}${unit}` : text;
}

function compactText(value?: string, maxLength = 18) {
  if (!value) return '-';
  return value.length > maxLength ? `${value.slice(0, maxLength)}...` : value;
}

function motherBatchText(row: MesHcPlanOrderApi.ProcessPivotRow) {
  return (
    row.motherRollBatchNo ||
    row.parentProductionBatchNo ||
    row.productionBatchNo ||
    row.batchNo ||
    '-'
  );
}

function batchText(stage: MesHcPlanOrderApi.ProcessPivotStage) {
  return stage.outputBatchNos || stage.sourceBatchNos || '-';
}

function formatStageBatchText(
  stage: MesHcPlanOrderApi.ProcessPivotStage,
  stageCode: string,
) {
  const text = batchText(stage);
  if (stageCode !== 'ADHESIVE1' || text === '-') return text;
  return (
    text
      .replaceAll(
        /\s*\d+(?:\.\d+)?\s*[-~至到]\s*\d+(?:\.\d+)?\s*(?:m|米)/gi,
        '',
      )
      .replaceAll(/\s{2,}/g, ' ')
      .replaceAll(/\s*([,，；;])\s*/g, '$1 ')
      .trim() || text
  );
}

function compactBatchText(
  stage: MesHcPlanOrderApi.ProcessPivotStage,
  stageCode: string,
) {
  return compactText(formatStageBatchText(stage, stageCode), 24);
}

function stageDoneText(stage: MesHcPlanOrderApi.ProcessPivotStage) {
  return formatQty(stage.doneQty, stage.reportUnit);
}

function stagePendingText(stage: MesHcPlanOrderApi.ProcessPivotStage) {
  return formatQty(stage.pendingQty, stage.pendingUnit || stage.reportUnit);
}

function stageDefectText(stage: MesHcPlanOrderApi.ProcessPivotStage) {
  return formatQty(stage.defectQty, stage.reportUnit);
}

function stageInspectionText(stage: MesHcPlanOrderApi.ProcessPivotStage) {
  return formatQty(stage.inspectionQty, stage.reportUnit);
}

function stageInspectionNgText(stage: MesHcPlanOrderApi.ProcessPivotStage) {
  return formatQty(stage.inspectionNgQty, stage.reportUnit);
}

function stageMetricText(
  stage: MesHcPlanOrderApi.ProcessPivotStage,
  field: StageMetricColumn['field'],
) {
  return formatNumber(stage[field], 3);
}

function stageMetricColumnText(
  stage: MesHcPlanOrderApi.ProcessPivotStage,
  column: StageSubColumn,
) {
  return column.metricField ? stageMetricText(stage, column.metricField) : '-';
}

function stageStatusClass(status?: string) {
  const normalized = String(status || 'NOT_STARTED').toUpperCase();
  return `stage-status--${normalized.toLowerCase().replaceAll('_', '-')}`;
}

function modelText(row: MesHcPlanOrderApi.ProcessPivotRow) {
  return row.actualModelCode || row.modelCode || row.modelName || '-';
}

function sizeText(row: MesHcPlanOrderApi.ProcessPivotRow) {
  return row.actualSizeSpec || row.sizeName || row.sizeSpec || '-';
}

function segmentText(row: MesHcPlanOrderApi.ProcessPivotRow) {
  return row.segmentBatchNo || '-';
}

async function handleCopyPlanNo(planNo?: string) {
  if (!planNo) return;
  await navigator.clipboard?.writeText(planNo);
  message.success('计划号已复制');
}

function splitBatchNos(value?: string) {
  if (!value) {
    return [];
  }
  const result: string[] = [];
  String(value)
    .split(/[,\s，；;]+/)
    .map((item) => item.trim())
    .filter(Boolean)
    .forEach((item) => {
      if (!result.includes(item)) {
        result.push(item);
      }
    });
  return result;
}

function summarizeRemarkText(value?: string) {
  if (!value) {
    return '';
  }
  const text = String(value).trim();
  if (!text) {
    return '';
  }
  const jsonBlocks = extractJsonBlocks(text);
  const defectNames = new Set<string>();
  jsonBlocks.forEach((block) => {
    const parsed = parseJsonBlock(block);
    if (parsed !== null) {
      collectNgDefectNames(parsed, defectNames);
    }
  });
  collectNgDefectNamesFromText(text, defectNames);

  let plainText = text;
  jsonBlocks.forEach((block) => {
    plainText = plainText.replace(block, '');
  });
  const lines = plainText
    .split(/[；;\n]+/)
    .map((item) => cleanRemarkPart(item))
    .filter(Boolean);
  if (defectNames.size > 0) {
    lines.push(`目视缺陷：${[...defectNames].join('、')}`);
  } else if (jsonBlocks.length > 0 && /目视|visual|inspection/i.test(text)) {
    lines.push('目视自检：异常');
  }
  return [...new Set(lines)].join('；');
}

function cleanRemarkPart(value: string) {
  const text = value.replace(/目视自检[:：]?\s*$/u, '').trim();
  if (!text || /[{}[\]"]/.test(text)) {
    return '';
  }
  if (/^\s*(?:reportType|itemName|result|remark)\s*[:：]/i.test(text)) {
    return '';
  }
  return text.length > 80 ? `${text.slice(0, 80)}...` : text;
}

function pieceConfirmStatus(kind: PieceDialogKind) {
  if (kind === 'DONE') {
    return 'OK';
  }
  return kind === 'DEFECT' ? 'NG' : '待加工';
}

function extractJsonBlocks(text: string) {
  const blocks: string[] = [];
  let start = -1;
  let depth = 0;
  let quote = '';
  let escaped = false;
  for (let index = 0; index < text.length; index += 1) {
    const char = text[index]!;
    if (start < 0) {
      if (char === '{' || char === '[') {
        start = index;
        depth = 1;
      }
      continue;
    }
    if (quote) {
      if (escaped) {
        escaped = false;
      } else if (char === '\\') {
        escaped = true;
      } else if (char === quote) {
        quote = '';
      }
      continue;
    }
    switch (char) {
      case '"':
      case "'": {
        quote = char;

        break;
      }
      case '[':
      case '{': {
        depth += 1;

        break;
      }
      case ']':
      case '}': {
        depth -= 1;
        if (depth === 0) {
          blocks.push(text.slice(start, index + 1));
          start = -1;
        }

        break;
      }
      // No default
    }
  }
  return blocks;
}

function parseJsonBlock(block: string) {
  try {
    return JSON.parse(block);
  } catch {
    return null;
  }
}

function collectNgDefectNames(value: unknown, target: Set<string>) {
  if (Array.isArray(value)) {
    value.forEach((item) => collectNgDefectNames(item, target));
    return;
  }
  if (!value || typeof value !== 'object') {
    return;
  }
  const record = value as Record<string, unknown>;
  const resultText = firstText(
    record.result,
    record.sampleResult,
    record.selfCheck,
    record.inspectionResult,
    record.feedbackResult,
    record.judgment,
  );
  const name = firstText(
    record.itemName,
    record.defectName,
    record.defectCode,
    record.inspectionItem,
    record.metricName,
    record.name,
    record.label,
  );
  const remark = firstText(record.remark, record.abnormalDesc);
  const abnormal =
    isNgText(resultText) || !!firstText(record.defectName, record.defectCode);
  if (abnormal && name) {
    target.add(remark ? `${name}(${remark})` : name);
  }
  Object.values(record).forEach((item) => collectNgDefectNames(item, target));
}

function collectNgDefectNamesFromText(text: string, target: Set<string>) {
  const patterns = [
    /"itemName"\s*:\s*"([^"]+)"[\s\S]{0,220}?"result"\s*:\s*"([^"]+)"/gi,
    /"result"\s*:\s*"([^"]+)"[\s\S]{0,220}?"itemName"\s*:\s*"([^"]+)"/gi,
    /"defectName"\s*:\s*"([^"]+)"/gi,
    /"defectCode"\s*:\s*"([^"]+)"/gi,
  ];
  patterns.forEach((pattern, patternIndex) => {
    let match: null | RegExpExecArray;
    while (true) {
      match = pattern.exec(text);
      if (!match) {
        break;
      }
      if (patternIndex === 0 && isNgText(match[2])) {
        target.add(match[1]!);
      } else if (patternIndex === 1 && isNgText(match[1])) {
        target.add(match[2]!);
      } else if (patternIndex >= 2) {
        target.add(match[1]!);
      }
    }
  });
}

function firstText(...values: unknown[]) {
  for (const value of values) {
    if (value === null || value === undefined) {
      continue;
    }
    const text = String(value).trim();
    if (text) {
      return text;
    }
  }
  return '';
}

function drilldownPieceNoText(
  stageCode: string | undefined,
  item: MesHcPlanOrderApi.ProcessPivotPiece,
  kind: PieceDialogKind,
) {
  const normalizedStageCode = String(stageCode || '')
    .trim()
    .toUpperCase();
  if (
    OUTPUT_BATCH_PREFERRED_STAGE_CODES.has(normalizedStageCode) &&
    kind !== 'PENDING'
  ) {
    return item.outputBatchNo || item.pieceNo || '-';
  }
  return item.pieceNo || item.outputBatchNo || '-';
}

function isNgText(value?: string) {
  const text = String(value || '')
    .trim()
    .toUpperCase();
  if (!text) {
    return false;
  }
  return (
    text === 'NG' ||
    text === 'N' ||
    text === 'FALSE' ||
    text === 'ABNORMAL' ||
    text === 'FAIL' ||
    text === 'FAILED' ||
    text.includes('不合格') ||
    text.includes('异常')
  );
}

function buildPieceItems(
  row: MesHcPlanOrderApi.ProcessPivotRow,
  stage: MesHcPlanOrderApi.ProcessPivotStage,
  kind: PieceDialogKind,
) {
  const detailItems = (stage.pieceDetails || [])
    .filter((item) => String(item.status || '').toUpperCase() === kind)
    .map((item) => {
      const pieceNo = drilldownPieceNoText(stage.stageCode, item, kind);
      const ngType = summarizeRemarkText(item.remark);
      const status = drilldownKindLabel(stage.stageCode, kind);
      return {
        actualModelCodeText: firstText(
          item.actualModelCode,
          row.actualModelCode,
          row.modelCode,
          '-',
        ),
        actualSizeSpec: firstText(
          item.actualSizeSpec,
          row.actualSizeSpec,
          row.sizeName,
          row.sizeSpec,
          '-',
        ),
        actualSizeSpecText: firstText(
          item.actualSizeSpec,
          row.actualSizeSpec,
          row.sizeName,
          row.sizeSpec,
          '-',
        ),
        coaFlag: item.coaFlag,
        coaFlagText: item.coaFlag ? '是' : '否',
        confirmStatus: pieceConfirmStatus(kind),
        inspectionFlagText: '否',
        inspectionResult: '-',
        lastReportTime: item.lastReportTime,
        ngType: ngType || (kind === 'DEFECT' ? '自检NG' : '-'),
        no: pieceNo,
        outputActualSizeSpecText: firstText(
          item.outputActualSizeSpec,
          item.actualSizeSpec,
          row.actualSizeSpec,
          row.sizeName,
          row.sizeSpec,
          '-',
        ),
        pieceNo,
        qtyText: pieceKindQtyText(stage, kind),
        remark: item.remark,
        scanConfirmTime: formatDateTime(item.lastReportTime),
        status,
        subtitle: [
          row.planNo || '-',
          segmentText(row),
          item.coaFlag ? 'COA片' : '',
        ]
          .filter(Boolean)
          .join(' / '),
      };
    });
  if (detailItems.length > 0 || (stage.pieceDetails || []).length > 0) {
    return detailItems;
  }
  const batchNos = splitBatchNos(
    kind === 'DONE' ? stage.outputBatchNos : stage.sourceBatchNos,
  );
  const fallbackNo = segmentText(row);
  const normalizedNos = batchNos.length > 0 ? batchNos : [fallbackNo];
  const qtyText = pieceKindQtyText(stage, kind);
  return normalizedNos.map((no) => ({
    actualSizeSpec: firstText(row.actualSizeSpec, row.sizeName, row.sizeSpec),
    actualSizeSpecText: firstText(
      row.actualSizeSpec,
      row.sizeName,
      row.sizeSpec,
      '-',
    ),
    actualModelCodeText: firstText(row.actualModelCode, row.modelCode, '-'),
    coaFlag: false,
    coaFlagText: '否',
    confirmStatus: pieceConfirmStatus(kind),
    inspectionFlagText: '否',
    inspectionResult: '-',
    ngType: kind === 'DEFECT' ? '自检NG' : '-',
    no,
    outputActualSizeSpecText: firstText(
      row.actualSizeSpec,
      row.sizeName,
      row.sizeSpec,
      '-',
    ),
    pieceNo: no,
    qtyText,
    scanConfirmTime: '-',
    status: drilldownKindLabel(stage.stageCode, kind),
    subtitle: `${row.planNo || '-'} / ${segmentText(row)}`,
  }));
}

function inspectionIsNg(item: MesHcPlanOrderApi.ProcessPivotInspection) {
  const judgment = String(item.judgment || '').toUpperCase();
  const status = String(item.status || '').toUpperCase();
  return (
    Number(item.inspectionNgQty || 0) > 0 ||
    judgment === 'NG' ||
    status === 'REJECTED' ||
    status === 'FAILED'
  );
}

function canOpenInspectionList(
  stage: MesHcPlanOrderApi.ProcessPivotStage,
  ngOnly = false,
) {
  const details = stage.inspectionDetails || [];
  return ngOnly
    ? details.some((item) => inspectionIsNg(item))
    : details.length > 0;
}

function buildInspectionItems(
  row: MesHcPlanOrderApi.ProcessPivotRow,
  stage: MesHcPlanOrderApi.ProcessPivotStage,
  ngOnly: boolean,
) {
  return (stage.inspectionDetails || [])
    .filter((item) => !ngOnly || inspectionIsNg(item))
    .map((item) => ({
      actionText: '检验详情',
      defectSummary: item.defectSummary,
      inspectionId: item.inspectionId,
      inspectionNgQtyText: formatQty(item.inspectionNgQty, stage.reportUnit),
      inspectionNo: item.inspectionNo,
      inspectionQtyText: formatQty(item.inspectionQty, stage.reportUnit),
      inspectionResult: inspectionStatusText(item),
      inspectionType: item.inspectionType,
      lastReportTime: item.inspectionTime,
      no: item.productBatchNo || item.inspectionNo || '-',
      pieceNo: item.productBatchNo || item.inspectionNo || '-',
      qtyText: ngOnly
        ? formatQty(item.inspectionNgQty, stage.reportUnit)
        : formatQty(item.inspectionQty, stage.reportUnit),
      remark: item.remark,
      scanConfirmTime: formatDateTime(item.inspectionTime),
      sourceType: item.sourceType,
      status: inspectionStatusText(item),
      subtitle: [
        row.planNo || '-',
        segmentText(row),
        item.inspectionType,
        item.inspectionNo,
      ]
        .filter(Boolean)
        .join(' / '),
    }));
}

function inspectionStatusText(item: MesHcPlanOrderApi.ProcessPivotInspection) {
  if (inspectionIsNg(item)) return '检验NG';
  const judgment = String(item.judgment || '').toUpperCase();
  if (judgment === 'OK') return '检验OK';
  return (
    inspectionCodeLabel(item.status) ||
    inspectionCodeLabel(item.judgment) ||
    '已送检'
  );
}

function inspectionCodeLabel(value?: unknown) {
  const normalized = String(value || '')
    .trim()
    .toUpperCase();
  if (!normalized) return '';
  if (normalized === 'OK') return '检验OK';
  if (normalized === 'NG') return '检验NG';
  if (normalized === 'PENDING') return '待判定';
  if (normalized === 'WAITING_QA') return '待检验';
  if (normalized === 'INSPECTING') return '检验中';
  if (normalized === 'COMPLETED' || normalized === 'COMPLETE') return '已完成';
  if (normalized === 'REJECTED') return '已驳回';
  if (normalized === 'FAILED' || normalized === 'FAIL') return '检验失败';
  if (normalized === 'CANCELED' || normalized === 'CANCELLED') return '已取消';
  if (normalized === 'ABNORMAL') return '异常';
  return String(value || '');
}

function pieceKindLabel(kind: PieceDialogKind) {
  if (kind === 'DEFECT') return 'NG/损耗';
  if (kind === 'INSPECTION') return '送检';
  if (kind === 'INSPECTION_NG') return '检验NG';
  return kind === 'DONE' ? '已完工' : '待加工';
}

function drilldownKindLabel(
  stageCode: string | undefined,
  kind: PieceDialogKind,
) {
  const normalizedStageCode = String(stageCode || '')
    .trim()
    .toUpperCase();
  if (kind === 'DEFECT' && PENDING_STAGE_CODES.has(normalizedStageCode)) {
    return '分段自检NG';
  }
  if (kind === 'DONE' && PENDING_STAGE_CODES.has(normalizedStageCode)) {
    return '分段自检OK';
  }
  if (
    kind === 'INSPECTION_NG' &&
    PENDING_STAGE_CODES.has(normalizedStageCode)
  ) {
    return '送检NG';
  }
  return pieceKindLabel(kind);
}

function pieceKindQtyText(
  stage: MesHcPlanOrderApi.ProcessPivotStage,
  kind: PieceDialogKind,
) {
  if (kind === 'DEFECT') return stageDefectText(stage);
  if (kind === 'INSPECTION') return stageInspectionText(stage);
  if (kind === 'INSPECTION_NG') return stageInspectionNgText(stage);
  return kind === 'DONE' ? stageDoneText(stage) : stagePendingText(stage);
}

function drilldownColumns(kind: PieceDialogKind) {
  return kind === 'INSPECTION' || kind === 'INSPECTION_NG'
    ? INSPECTION_DRILLDOWN_COLUMNS
    : PIECE_DRILLDOWN_COLUMNS;
}

function openPieceDialog(
  row: MesHcPlanOrderApi.ProcessPivotRow,
  stageMeta: StageColumn,
  kind: PieceDialogKind,
) {
  if (!canOpenPieceList(stageMeta)) {
    return;
  }
  const stage = stageOf(row, stageMeta.code);
  pieceDialog.open = true;
  pieceDialog.kind = kind;
  pieceDialog.keyword = '';
  pieceDialog.stageName = stageMeta.label;
  pieceDialog.title = `${stageMeta.label}${drilldownKindLabel(stageMeta.code, kind)}片列表`;
  pieceDialog.subtitle = `${row.planNo || '-'} / ${segmentText(row)}`;
  pieceDialog.quantityText = pieceKindQtyText(stage, kind);
  pieceDialog.columns = drilldownColumns(kind);
  pieceDialog.items = buildPieceItems(row, stage, kind);
}

function openInspectionDialog(
  row: MesHcPlanOrderApi.ProcessPivotRow,
  stageMeta: StageColumn,
  ngOnly = false,
) {
  const stage = stageOf(row, stageMeta.code);
  const kind: PieceDialogKind = ngOnly ? 'INSPECTION_NG' : 'INSPECTION';
  if (!canOpenInspectionList(stage, ngOnly)) {
    return;
  }
  pieceDialog.open = true;
  pieceDialog.kind = kind;
  pieceDialog.keyword = '';
  pieceDialog.stageName = stageMeta.label;
  pieceDialog.title = `${stageMeta.label}${drilldownKindLabel(stageMeta.code, kind)}明细`;
  pieceDialog.subtitle = `${row.planNo || '-'} / ${segmentText(row)}`;
  pieceDialog.quantityText = pieceKindQtyText(stage, kind);
  pieceDialog.columns = drilldownColumns(kind);
  pieceDialog.items = buildInspectionItems(row, stage, ngOnly);
}

function openInspectionDetail(item: PieceCardItem) {
  if (!item.sourceType || !item.inspectionId) {
    message.warning('缺少检验单来源信息，无法查看检验详情');
    return;
  }
  pieceDialog.open = false;
  inspectionDetailModalApi
    .setData({
      inspectionId: Number(item.inspectionId),
      inspectionNo: String(item.inspectionNo || ''),
      ngOnly: pieceDialog.kind === 'INSPECTION_NG',
      sourceType: String(item.sourceType || ''),
      title: `${item.status || '检验'}详情`,
    })
    .open();
}

function drilldownTagColor(kind: PieceDialogKind) {
  if (kind === 'DEFECT') return 'red';
  if (kind === 'INSPECTION') return 'blue';
  if (kind === 'INSPECTION_NG') return 'red';
  return kind === 'DONE' ? 'green' : 'orange';
}

function drilldownRowKey(item: PieceCardItem, index: number) {
  return [
    String(item.pieceNo || item.no || ''),
    String(item.inspectionNo || ''),
    String(item.scanConfirmTime || item.lastReportTime || ''),
    index,
  ]
    .filter(Boolean)
    .join('|');
}

function drilldownColumnStyle(column: DrilldownTableColumn) {
  const width = column.width || column.minWidth;
  return {
    minWidth: column.minWidth ? `${column.minWidth}px` : undefined,
    width: width ? `${width}px` : undefined,
  };
}

function drilldownCellClass(column: DrilldownTableColumn) {
  return {
    'is-center': column.align === 'center',
    'is-piece-no': column.key === 'pieceNo',
    'is-right': column.align === 'right',
  };
}

function drilldownCellText(item: PieceCardItem, column: DrilldownTableColumn) {
  const value = item[column.key];
  if (value === null || value === undefined) {
    return '-';
  }
  const text = String(value).trim();
  return text || '-';
}

function drilldownStatusColor(value: unknown) {
  const text = String(value || '')
    .trim()
    .toUpperCase();
  if (text === 'OK') return 'green';
  if (text === 'NG') return 'red';
  return 'orange';
}

function drilldownFlagColor(value: unknown) {
  return String(value || '') === '是' ? 'blue' : 'default';
}

function drilldownInspectionResultColor(value: unknown) {
  const text = String(value || '').trim();
  if (text.includes('NG') || text.includes('驳回') || text.includes('失败')) {
    return 'red';
  }
  if (text.includes('OK') || text.includes('完成')) {
    return 'green';
  }
  if (!text || text === '-') {
    return 'default';
  }
  return 'blue';
}

function showTotal(count: number) {
  return `共 ${count} 显示行`;
}

onMounted(() => {
  void Promise.all([loadSyncStatus(), fetchData()]);
  qtimeRefreshTimer = window.setInterval(() => {
    qtimeNowTimestamp.value = Date.now();
  }, 60_000);
});

onBeforeUnmount(() => {
  if (qtimeRefreshTimer) {
    window.clearInterval(qtimeRefreshTimer);
    qtimeRefreshTimer = undefined;
  }
});
</script>

<template>
  <Page
    auto-content-height
    class="process-pivot-page"
    content-class="process-pivot-content"
  >
    <div class="package-fg-console process-pivot-report">
      <section class="prototype-banner process-pivot-banner">
        <span class="console-main-icon">
          <IconifyIcon icon="lucide:table-2" />
        </span>
        <div class="console-title-block">
          <div class="console-title-row">
            <h2 class="console-title-text">生产进度（日结版）</h2>
          </div>
          <div class="console-meta-row">
            <span class="console-meta-item">
              <span class="console-meta-label">本页显示</span>
              <span class="console-meta-value">{{ rows.length }}</span>
              <span class="console-meta-sub">行</span>
            </span>
            <span class="console-meta-item">
              <span class="console-meta-label">本页计划</span>
              <span class="console-meta-value">{{ currentPagePlanCount }}</span>
              <span class="console-meta-sub">单</span>
            </span>
            <span class="console-meta-item">
              <span class="console-meta-label">显示行</span>
              <span class="console-meta-value">{{ total }}</span>
              <span class="console-meta-sub">行</span>
            </span>
            <span class="console-meta-item">
              <span class="console-meta-label">条件</span>
              <span class="console-meta-value">{{ activeFilterCount }}</span>
              <span class="console-meta-sub">项</span>
            </span>
            <span class="console-meta-item" :title="syncStatusText">
              <span class="console-meta-label">日结</span>
              <span class="console-meta-value">{{ syncStatusMetaValue }}</span>
              <span class="console-meta-sub">{{ syncStatusMetaSub }}</span>
            </span>
          </div>
        </div>
        <div class="console-action-group process-pivot-action-group">
          <button
            class="action-tile"
            type="button"
            @click="handleShiftPlanDateRange(-1)"
          >
            <IconifyIcon icon="lucide:chevron-left" />
            <span>上周</span>
          </button>
          <button
            class="action-tile"
            type="button"
            @click="handleShiftPlanDateRange(1)"
          >
            <IconifyIcon icon="lucide:chevron-right" />
            <span>下周</span>
          </button>
          <button class="action-tile" type="button" @click="handleSearch">
            <IconifyIcon icon="lucide:search" />
            <span>查询</span>
          </button>
          <button class="action-tile" type="button" @click="handleReset">
            <IconifyIcon icon="lucide:rotate-ccw" />
            <span>重置</span>
          </button>
          <button class="action-tile" type="button" @click="fetchData">
            <IconifyIcon icon="lucide:refresh-cw" />
            <span>刷新</span>
          </button>
          <button
            class="action-tile"
            type="button"
            :disabled="syncing"
            :title="syncStatusText"
            @click="handleSyncDaily"
          >
            <IconifyIcon icon="lucide:database-zap" />
            <span>{{ syncing ? '同步中' : '同步' }}</span>
          </button>
          <button
            class="action-tile"
            type="button"
            :disabled="comparing"
            @click="handleCompareDaily"
          >
            <IconifyIcon icon="lucide:git-compare-arrows" />
            <span>{{ comparing ? '比对中' : '比对' }}</span>
          </button>
          <button
            class="action-tile"
            type="button"
            :disabled="exporting"
            @click="handleExportExcel"
          >
            <IconifyIcon icon="lucide:file-spreadsheet" />
            <span>{{ exporting ? '导出中' : '导出' }}</span>
          </button>
          <button
            class="action-tile"
            type="button"
            @click="columnSettingVisible = true"
          >
            <IconifyIcon icon="lucide:settings-2" />
            <span>列设置</span>
          </button>
        </div>
      </section>

      <section class="package-fg-filter-bar process-pivot-query-panel">
        <div class="process-pivot-simple-query">
          <label class="process-pivot-simple-query-label">生产开始</label>
          <RangePicker
            v-model:value="selectedPlanDateRange"
            allow-clear
            format="YYYY-MM-DD"
            @change="handlePlanDateRangeChange"
          />
          <label class="process-pivot-simple-query-label">关键词</label>
          <Input
            v-model:value="queryForm.keyword"
            allow-clear
            placeholder="计划号 / 料号 / 型号 / 母批批号 / 分段批号"
            @press-enter="handleSearch"
          />
          <Button type="primary" @click="handleSearch">
            <template #icon><IconifyIcon icon="lucide:search" /></template>
            查询
          </Button>
          <Button @click="openAdvancedQuery">
            <template #icon>
              <IconifyIcon icon="lucide:sliders-horizontal" />
            </template>
            多条件查询
          </Button>
        </div>
      </section>

      <div class="report-body">
        <div class="report-table-host">
          <div class="pivot-table-wrap">
            <table class="pivot-table" :style="pivotTableStyle">
              <colgroup>
                <col
                  v-for="column in visibleBaseColumns"
                  :key="column.key"
                  :class="column.className"
                />
                <template
                  v-for="stage in visibleStageColumns"
                  :key="`${stage.code}-cols`"
                >
                  <col
                    v-for="column in visibleStageSubColumns(stage)"
                    :key="`${stage.code}-${column.key}`"
                    :class="column.className"
                  />
                </template>
              </colgroup>
              <thead>
                <tr>
                  <th
                    v-if="isBaseColumnVisible('planNo')"
                    class="col-plan sticky-plan"
                    rowspan="2"
                  >
                    计划号
                  </th>
                  <th
                    v-if="isBaseColumnVisible('motherRollBatchNo')"
                    class="col-mother-batch sticky-mother-batch"
                    :style="motherBatchStickyStyle"
                    rowspan="2"
                  >
                    母批批号
                  </th>
                  <th
                    v-if="isBaseColumnVisible('planStatus')"
                    class="col-status sticky-status"
                    :style="statusStickyStyle"
                    rowspan="2"
                  >
                    状态
                  </th>
                  <th
                    v-if="isBaseColumnVisible('modelCode')"
                    class="col-model"
                    rowspan="2"
                  >
                    型号
                  </th>
                  <th
                    v-if="isBaseColumnVisible('materialCode')"
                    class="col-material"
                    rowspan="2"
                  >
                    料号
                  </th>
                  <th
                    v-if="isBaseColumnVisible('sizeSpec')"
                    class="col-size"
                    rowspan="2"
                  >
                    尺寸
                  </th>
                  <th
                    v-for="stage in visibleStageColumns"
                    :key="stage.code"
                    class="stage-group"
                    :colspan="stageColumnCount(stage)"
                  >
                    {{ stage.label }}
                  </th>
                </tr>
                <tr>
                  <template
                    v-for="stage in visibleStageColumns"
                    :key="`${stage.code}-sub`"
                  >
                    <th
                      v-for="column in visibleStageSubColumns(stage)"
                      :key="`${stage.code}-${column.key}-head`"
                      :class="column.className"
                    >
                      {{ column.label }}
                    </th>
                  </template>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="(row, rowIndex) in rows"
                  :key="resolvePivotRowKey(row)"
                  :class="{ 'pivot-row--selected': isSelectedPivotRow(row) }"
                  @click="handleSelectPivotRow(row)"
                >
                  <td
                    v-if="
                      isBaseColumnVisible('planNo') &&
                      isFirstPlanRow(row, rowIndex)
                    "
                    class="cell-plan sticky-plan"
                    :class="{ 'cell-plan--with-tag': row.planNoTagText }"
                    :rowspan="planRowSpan(rowIndex)"
                  >
                    <div class="plan-cell-content">
                      <button
                        class="plan-link"
                        type="button"
                        @click="handleCopyPlanNo(row.planNo)"
                      >
                        {{ row.planNo || '-' }}
                      </button>
                      <Tooltip
                        v-if="row.planNoTagText"
                        title="该计划挂接半成品库存继续后加工"
                      >
                        <Tag class="plan-no-tag" color="processing">
                          {{ row.planNoTagText }}
                        </Tag>
                      </Tooltip>
                    </div>
                  </td>
                  <td
                    v-if="
                      isBaseColumnVisible('motherRollBatchNo') &&
                      isFirstPlanRow(row, rowIndex)
                    "
                    class="cell-mother-batch sticky-mother-batch"
                    :style="motherBatchStickyStyle"
                    :rowspan="planRowSpan(rowIndex)"
                  >
                    <Tooltip :title="motherBatchText(row)">
                      <span class="batch-text">{{
                        compactText(motherBatchText(row), 18)
                      }}</span>
                    </Tooltip>
                  </td>
                  <td
                    v-if="
                      isBaseColumnVisible('planStatus') &&
                      isFirstPlanRow(row, rowIndex)
                    "
                    class="cell-status sticky-status"
                    :style="statusStickyStyle"
                    :rowspan="planRowSpan(rowIndex)"
                  >
                    <Tag :color="getPlanStatusMeta(row.planStatus).color">
                      {{ getPlanStatusMeta(row.planStatus).label }}
                    </Tag>
                  </td>
                  <td
                    v-if="
                      isBaseColumnVisible('modelCode') &&
                      isFirstModelSizeRow(row, rowIndex)
                    "
                    :rowspan="modelSizeRowSpan(rowIndex)"
                  >
                    {{ modelText(row) }}
                  </td>
                  <td
                    v-if="
                      isBaseColumnVisible('materialCode') &&
                      isFirstPlanRow(row, rowIndex)
                    "
                    :rowspan="planRowSpan(rowIndex)"
                  >
                    <Tooltip :title="row.materialName || row.materialCode">
                      <span>{{ row.materialCode || '-' }}</span>
                    </Tooltip>
                  </td>
                  <td
                    v-if="
                      isBaseColumnVisible('sizeSpec') &&
                      isFirstModelSizeRow(row, rowIndex)
                    "
                    :rowspan="modelSizeRowSpan(rowIndex)"
                  >
                    {{ sizeText(row) }}
                  </td>
                  <template
                    v-for="stageMeta in visibleStageColumns"
                    :key="`${resolvePivotRowKey(row)}-${stageMeta.code}`"
                  >
                    <template
                      v-if="isFirstStageRow(row, rowIndex, stageMeta.code)"
                    >
                      <td
                        v-for="column in visibleStageSubColumns(stageMeta)"
                        :key="`${resolvePivotRowKey(row)}-${stageMeta.code}-${column.key}`"
                        :class="
                          stageCellClass(column, stageOf(row, stageMeta.code))
                        "
                        :rowspan="stageRowSpan(row, rowIndex, stageMeta.code)"
                        :title="stageQtimeTitle(stageOf(row, stageMeta.code))"
                      >
                        <template v-if="column.key === 'batch'">
                          <Tooltip
                            :title="
                              formatStageBatchText(
                                stageOf(row, stageMeta.code),
                                stageMeta.code,
                              )
                            "
                          >
                            <span class="batch-text">{{
                              compactBatchText(
                                stageOf(row, stageMeta.code),
                                stageMeta.code,
                              )
                            }}</span>
                          </Tooltip>
                        </template>
                        <template v-else-if="column.metricField">
                          {{
                            stageMetricColumnText(
                              stageOf(row, stageMeta.code),
                              column,
                            )
                          }}
                        </template>
                        <template v-else-if="column.key === 'doneQty'">
                          <span
                            class="stage-status-dot"
                            :class="
                              stageStatusClass(
                                stageOf(row, stageMeta.code).stageStatus,
                              )
                            "
                          ></span>
                          <button
                            v-if="canOpenPieceList(stageMeta)"
                            class="qty-link"
                            type="button"
                            @click="openPieceDialog(row, stageMeta, 'DONE')"
                          >
                            {{ stageDoneText(stageOf(row, stageMeta.code)) }}
                          </button>
                          <span v-else>
                            {{ stageDoneText(stageOf(row, stageMeta.code)) }}
                          </span>
                        </template>
                        <template v-else-if="column.key === 'inspectionQty'">
                          <button
                            v-if="
                              canOpenInspectionList(
                                stageOf(row, stageMeta.code),
                              )
                            "
                            class="qty-link qty-link--inspection"
                            type="button"
                            @click="openInspectionDialog(row, stageMeta)"
                          >
                            {{
                              stageInspectionText(stageOf(row, stageMeta.code))
                            }}
                          </button>
                          <span v-else>
                            {{
                              stageInspectionText(stageOf(row, stageMeta.code))
                            }}
                          </span>
                        </template>
                        <template v-else-if="column.key === 'inspectionNgQty'">
                          <button
                            v-if="
                              canOpenInspectionList(
                                stageOf(row, stageMeta.code),
                                true,
                              )
                            "
                            class="qty-link qty-link--inspection-ng"
                            type="button"
                            @click="openInspectionDialog(row, stageMeta, true)"
                          >
                            {{
                              stageInspectionNgText(
                                stageOf(row, stageMeta.code),
                              )
                            }}
                          </button>
                          <span v-else>
                            {{
                              stageInspectionNgText(
                                stageOf(row, stageMeta.code),
                              )
                            }}
                          </span>
                        </template>
                        <template v-else-if="column.key === 'pendingQty'">
                          <button
                            v-if="canOpenPieceList(stageMeta)"
                            class="qty-link qty-link--pending"
                            type="button"
                            @click="openPieceDialog(row, stageMeta, 'PENDING')"
                          >
                            {{ stagePendingText(stageOf(row, stageMeta.code)) }}
                          </button>
                          <span v-else>
                            {{ stagePendingText(stageOf(row, stageMeta.code)) }}
                          </span>
                        </template>
                        <template v-else-if="column.key === 'defectQty'">
                          <button
                            v-if="canOpenPieceList(stageMeta)"
                            class="qty-link qty-link--defect"
                            type="button"
                            @click="openPieceDialog(row, stageMeta, 'DEFECT')"
                          >
                            {{ stageDefectText(stageOf(row, stageMeta.code)) }}
                          </button>
                          <span v-else>
                            {{ stageDefectText(stageOf(row, stageMeta.code)) }}
                          </span>
                        </template>
                        <template v-else-if="column.key === 'lastReportTime'">
                          {{
                            formatDateTime(
                              stageOf(row, stageMeta.code).lastReportTime,
                            )
                          }}
                        </template>
                      </td>
                    </template>
                  </template>
                </tr>
                <tr v-if="!loading && rows.length === 0">
                  <td class="empty-cell" :colspan="totalColumnCount">
                    暂无生产进度数据
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <div v-if="loading" class="table-loading-cover">
            <Spin />
          </div>
        </div>

        <div
          class="qtime-detail-panel"
          :class="[{ 'qtime-detail-panel--expanded': qtimeDetailExpanded }]"
        >
          <button
            class="qtime-detail-toggle"
            type="button"
            @click="qtimeDetailExpanded = !qtimeDetailExpanded"
          >
            <span class="qtime-detail-toggle__title">
              <IconifyIcon icon="lucide:timer-reset" />
              <span>选中行 QTIME</span>
            </span>
            <span v-if="selectedPivotRow" class="qtime-detail-toggle__meta">
              <strong>{{ selectedPivotRow.planNo || '-' }}</strong>
              <span>{{ segmentText(selectedPivotRow) }}</span>
              <span>{{ modelText(selectedPivotRow) }}</span>
            </span>
            <span v-else class="qtime-detail-toggle__meta">
              <span>请选择一行</span>
            </span>
            <Tag
              :color="selectedQtimeTimeoutCount > 0 ? 'red' : 'blue'"
              class="qtime-detail-status-tag"
            >
              {{ selectedQtimeSummaryText }}
            </Tag>
            <span class="qtime-detail-toggle__action">
              {{ qtimeDetailExpanded ? '收起' : '展开' }}
              <IconifyIcon
                :icon="
                  qtimeDetailExpanded
                    ? 'lucide:chevron-down'
                    : 'lucide:chevron-up'
                "
              />
            </span>
          </button>
          <div v-if="qtimeDetailExpanded" class="qtime-detail-content">
            <div v-if="selectedPivotRow" class="qtime-detail-context">
              <strong>{{ selectedPivotRow.planNo || '-' }}</strong>
              <span>{{ segmentText(selectedPivotRow) }}</span>
              <span>{{ modelText(selectedPivotRow) }}</span>
              <span>{{ sizeText(selectedPivotRow) }}</span>
              <Tag
                :color="selectedQtimeTimeoutCount > 0 ? 'red' : 'green'"
                class="qtime-detail-status-tag"
              >
                {{
                  selectedQtimeTimeoutCount > 0
                    ? `超时 ${selectedQtimeTimeoutCount} 项`
                    : '无超时'
                }}
              </Tag>
            </div>
            <div v-else class="qtime-detail-context">
              <span>未选中计划行</span>
            </div>
            <div v-if="selectedQtimeItems.length > 0" class="qtime-detail-list">
              <div
                v-for="item in selectedQtimeItems"
                :key="item.stageCode"
                :class="qtimeItemClass(item)"
              >
                <div class="qtime-detail-card__head">
                  <span>{{ item.stageName }}</span>
                  <Tag
                    :color="item.timeout ? 'red' : 'green'"
                    class="qtime-detail-card__tag"
                  >
                    {{ item.statusText }}
                  </Tag>
                </div>
                <div class="qtime-detail-card__duration">
                  <span>{{ item.actualLabel }}</span>
                  <strong>{{ item.actualText }}</strong>
                  <em>额定 {{ item.standardText }}</em>
                </div>
                <div class="qtime-detail-card__times">
                  <span>上道 {{ item.sourceEndTimeText }}</span>
                  <span>{{ item.targetStartTimeText }}</span>
                </div>
                <div class="qtime-detail-card__message">{{ item.message }}</div>
              </div>
            </div>
            <div v-else class="qtime-detail-empty">当前行暂无 QTIME 提醒</div>
          </div>
        </div>

        <div class="report-pagination">
          <Pagination
            v-model:current="pageNo"
            v-model:page-size="pageSize"
            show-less-items
            show-size-changer
            :page-size-options="['10', '20', '50', '100']"
            :show-total="showTotal"
            :total="total"
            size="small"
            @change="handlePageChange"
            @show-size-change="handlePageChange"
          />
        </div>
      </div>
    </div>

    <Modal
      v-model:open="columnSettingVisible"
      :footer="null"
      title="列设置"
      width="780px"
      wrap-class-name="process-pivot-column-setting-modal"
    >
      <div class="process-pivot-column-setting">
        <div class="column-setting-scroll">
          <section class="column-setting-section">
            <div class="column-setting-title">基础列</div>
            <div class="column-setting-grid">
              <Checkbox
                v-for="column in BASE_COLUMNS"
                :key="column.key"
                :checked="isBaseColumnVisible(column.key)"
                :disabled="column.required"
                @change="
                  (event) =>
                    setBaseColumnVisible(column.key, getCheckedFromEvent(event))
                "
              >
                {{ column.label }}
              </Checkbox>
            </div>
          </section>
          <section class="column-setting-section">
            <div class="column-setting-title">工序列</div>
            <div class="column-setting-stage-list">
              <div
                v-for="stage in STAGE_COLUMNS"
                :key="stage.code"
                class="column-setting-stage"
              >
                <div class="column-setting-stage-name">{{ stage.label }}</div>
                <div class="column-setting-grid">
                  <Checkbox
                    v-for="column in stageSubColumns(stage)"
                    :key="`${stage.code}-${column.key}`"
                    :checked="isStageSubColumnVisible(stage, column.key)"
                    @change="
                      (event) =>
                        setStageSubColumnVisible(
                          stage.code,
                          column.key,
                          getCheckedFromEvent(event),
                        )
                    "
                  >
                    {{ column.label }}
                  </Checkbox>
                </div>
              </div>
            </div>
          </section>
        </div>
        <div class="column-setting-footer">
          <Button @click="resetColumnConfig">恢复默认</Button>
          <Button type="primary" @click="columnSettingVisible = false">
            完成
          </Button>
        </div>
      </div>
    </Modal>

    <Modal
      v-model:open="advancedQueryVisible"
      :footer="null"
      title="多条件查询"
      width="860px"
      wrap-class-name="process-pivot-advanced-query-modal"
    >
      <div class="process-pivot-advanced-query-body">
        <div class="process-pivot-advanced-query-grid">
          <div class="process-pivot-query-item">
            <label>计划状态</label>
            <Select
              v-model:value="queryForm.planStatuses"
              allow-clear
              mode="multiple"
              :max-tag-count="1"
              :options="PLAN_STATUS_OPTIONS"
              placeholder="计划状态"
            />
          </div>
          <div class="process-pivot-query-item">
            <label>计划号</label>
            <Input
              v-model:value="queryForm.planNo"
              allow-clear
              placeholder="计划号"
              @press-enter="applyAdvancedQuery"
            />
          </div>
          <div class="process-pivot-query-item">
            <label>母批批号</label>
            <Input
              v-model:value="queryForm.motherRollBatchNo"
              allow-clear
              placeholder="母批/生产/主批号"
              @press-enter="applyAdvancedQuery"
            />
          </div>
          <div class="process-pivot-query-item">
            <label>分段批号</label>
            <Input
              v-model:value="queryForm.motherSegmentBatchNo"
              allow-clear
              placeholder="分段批号"
              @press-enter="applyAdvancedQuery"
            />
          </div>
          <div class="process-pivot-query-item">
            <label>型号</label>
            <Input
              v-model:value="queryForm.modelCode"
              allow-clear
              placeholder="型号"
              @press-enter="applyAdvancedQuery"
            />
          </div>
          <div class="process-pivot-query-item">
            <label>料号</label>
            <Input
              v-model:value="queryForm.materialKeyword"
              allow-clear
              placeholder="料号或名称"
              @press-enter="applyAdvancedQuery"
            />
          </div>
          <div class="process-pivot-query-item">
            <label>尺寸规格</label>
            <Input
              v-model:value="queryForm.sizeSpec"
              allow-clear
              placeholder="尺寸规格"
              @press-enter="applyAdvancedQuery"
            />
          </div>
          <div class="process-pivot-query-item">
            <label>生产开始起</label>
            <DatePicker
              v-model:value="queryForm.productionStartDateStart"
              placeholder="生产开始起"
              value-format="YYYY-MM-DD"
            />
          </div>
          <div class="process-pivot-query-item">
            <label>生产开始止</label>
            <DatePicker
              v-model:value="queryForm.productionStartDateEnd"
              placeholder="生产开始止"
              value-format="YYYY-MM-DD"
            />
          </div>
          <div class="process-pivot-query-item">
            <label>生产结束起</label>
            <DatePicker
              v-model:value="queryForm.productionEndDateStart"
              placeholder="生产结束起"
              value-format="YYYY-MM-DD"
            />
          </div>
          <div class="process-pivot-query-item">
            <label>生产结束止</label>
            <DatePicker
              v-model:value="queryForm.productionEndDateEnd"
              placeholder="生产结束止"
              value-format="YYYY-MM-DD"
            />
          </div>
        </div>
        <div class="process-pivot-advanced-footer">
          <Button @click="advancedQueryVisible = false">关闭</Button>
          <Button @click="clearAdvancedQuery">清空条件</Button>
          <Button type="primary" @click="applyAdvancedQuery">应用查询</Button>
        </div>
      </div>
    </Modal>

    <Modal
      v-model:open="pieceDialog.open"
      centered
      :footer="null"
      width="960px"
      class="piece-modal"
    >
      <template #title>
        <div class="piece-modal-title">
          <span>{{ pieceDialog.title }}</span>
          <small>{{ pieceDialog.subtitle }}</small>
        </div>
      </template>
      <div class="piece-modal-toolbar">
        <Input
          v-model:value="pieceDialog.keyword"
          allow-clear
          :placeholder="
            pieceDialog.kind === 'INSPECTION' ||
            pieceDialog.kind === 'INSPECTION_NG'
              ? '过滤检验单/批号'
              : '过滤片号'
          "
          size="small"
        >
          <template #prefix>
            <IconifyIcon icon="lucide:search" />
          </template>
        </Input>
        <Tag :color="drilldownTagColor(pieceDialog.kind)">
          {{ pieceDialog.quantityText }}
        </Tag>
      </div>
      <div class="drilldown-table-wrap">
        <table class="drilldown-table">
          <thead>
            <tr>
              <th
                v-for="column in pieceDialog.columns"
                :key="column.key"
                :class="drilldownCellClass(column)"
                :style="drilldownColumnStyle(column)"
              >
                {{ column.label }}
              </th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="(item, index) in filteredPieceItems"
              :key="drilldownRowKey(item, index)"
            >
              <td
                v-for="column in pieceDialog.columns"
                :key="column.key"
                :class="drilldownCellClass(column)"
                :style="drilldownColumnStyle(column)"
              >
                <template v-if="column.key === 'index'">
                  {{ index + 1 }}
                </template>
                <template v-else-if="column.key === 'confirmStatus'">
                  <Tag :color="drilldownStatusColor(item.confirmStatus)">
                    {{ drilldownCellText(item, column) }}
                  </Tag>
                </template>
                <template
                  v-else-if="
                    column.key === 'coaFlagText' ||
                    column.key === 'inspectionFlagText'
                  "
                >
                  <Tag :color="drilldownFlagColor(item[column.key])">
                    {{ drilldownCellText(item, column) }}
                  </Tag>
                </template>
                <template v-else-if="column.key === 'inspectionResult'">
                  <Button
                    v-if="
                      item.actionText && item.sourceType && item.inspectionId
                    "
                    size="small"
                    type="link"
                    @click="openInspectionDetail(item)"
                  >
                    {{ drilldownCellText(item, column) }}
                  </Button>
                  <Tag
                    v-else
                    :color="
                      drilldownInspectionResultColor(item.inspectionResult)
                    "
                  >
                    {{ drilldownCellText(item, column) }}
                  </Tag>
                </template>
                <template v-else>
                  {{ drilldownCellText(item, column) }}
                </template>
              </td>
            </tr>
            <tr v-if="filteredPieceItems.length === 0">
              <td class="piece-empty" :colspan="pieceDialog.columns.length">
                暂无匹配明细
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </Modal>
    <InspectionDetailModal />
  </Page>
</template>

<style scoped>
.process-pivot-page {
  --report-border: #d8dee8;
  --report-header: #f3f5f8;
  --report-soft: #f8fafc;
  --report-text: #1f2937;
  --report-muted: #667085;

  min-height: 0;
  overflow: hidden;
}

:global(.process-pivot-content) {
  box-sizing: border-box;
  display: flex;
  height: 100%;
  width: 100%;
  min-height: 0;
  min-width: 0;
  padding: 8px !important;
  overflow: hidden !important;
}

.process-pivot-report {
  display: grid;
  box-sizing: border-box;
  grid-template-rows: max-content max-content minmax(0, 1fr);
  gap: 8px;
  width: 100%;
  height: 100%;
  max-height: 100%;
  flex: 1 1 0;
  min-height: 0;
  min-width: 0;
  color: var(--report-text);
  overflow: hidden;
}

.process-pivot-banner {
  min-height: 78px;
  max-height: 90px;
}

.process-pivot-action-group {
  flex-wrap: nowrap;
}

.process-pivot-action-group .action-tile:disabled {
  cursor: not-allowed;
  opacity: 0.58;
}

.process-pivot-query-panel {
  display: flex;
  box-sizing: border-box;
  flex-direction: column;
  gap: 8px;
  min-height: 0;
  min-width: 0;
  padding: 8px 10px;
  overflow: hidden;
  background: linear-gradient(180deg, #eef3f8 0%, #e4ebf3 100%);
  border: 1px solid #8794a4;
}

.process-pivot-simple-query {
  display: grid;
  grid-template-columns: 86px 240px 86px minmax(260px, 1fr) 94px 118px;
  gap: 8px;
  align-items: stretch;
  min-width: 0;
}

.process-pivot-simple-query-label {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  min-height: 34px;
  padding: 0 10px;
  color: #334155;
  font-size: 12px;
  font-weight: 900;
  white-space: nowrap;
  background: #dbe3ed;
  border: 1px solid #c6d3df;
}

.process-pivot-simple-query :deep(.ant-input-affix-wrapper),
.process-pivot-simple-query :deep(.ant-picker),
.process-pivot-simple-query :deep(.ant-select-selector) {
  min-height: 34px;
  border-radius: 0;
}

.process-pivot-simple-query :deep(.ant-btn) {
  height: 34px;
  border-radius: 0;
  font-weight: 800;
}

.process-pivot-column-setting {
  display: grid;
  grid-template-rows: minmax(0, 1fr) auto;
  gap: 0;
  max-height: min(72vh, 680px);
  margin: -2px -2px 0;
  overflow: hidden;
}

:global(.process-pivot-column-setting-modal .ant-modal-body) {
  padding: 12px 14px 14px;
}

.column-setting-scroll {
  display: flex;
  min-height: 0;
  flex-direction: column;
  gap: 10px;
  overflow: auto;
  padding: 2px 4px 10px;
}

.column-setting-section {
  border: 1px solid #d3dce8;
  border-radius: 6px;
  padding: 10px 10px 12px;
  background: #f8fafc;
}

.column-setting-title {
  display: flex;
  align-items: center;
  min-height: 24px;
  margin-bottom: 10px;
  color: var(--report-text);
  font-size: 13px;
  font-weight: 700;
}

.column-setting-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(112px, 1fr));
  gap: 8px;
}

.column-setting-stage-list {
  display: flex;
  flex-direction: column;
  gap: 7px;
}

.column-setting-stage {
  display: grid;
  grid-template-columns: 86px minmax(0, 1fr);
  gap: 10px;
  align-items: flex-start;
  border: 1px solid #dbe4ef;
  border-radius: 6px;
  background: #fff;
  padding: 8px;
}

.column-setting-stage-name {
  display: flex;
  align-items: center;
  min-height: 30px;
  border-radius: 4px;
  background: #e8eff8;
  padding: 0 8px;
  color: #344054;
  font-weight: 650;
}

.process-pivot-column-setting :deep(.ant-checkbox-wrapper) {
  display: flex;
  min-width: 0;
  min-height: 30px;
  align-items: center;
  margin-inline-start: 0;
  border: 1px solid #d7dfeb;
  border-radius: 6px;
  background: #fff;
  padding: 5px 8px;
  color: #344054;
  line-height: 18px;
  transition:
    border-color 0.15s ease,
    background 0.15s ease,
    color 0.15s ease;
}

.process-pivot-column-setting :deep(.ant-checkbox-wrapper:hover) {
  border-color: #7aa3d5;
  background: #f4f8ff;
}

.process-pivot-column-setting :deep(.ant-checkbox-wrapper-checked) {
  border-color: #3576c8;
  background: #edf5ff;
  color: #1d4f91;
}

.process-pivot-column-setting :deep(.ant-checkbox-wrapper-disabled) {
  border-style: dashed;
  background: #eef2f7;
  color: #667085;
}

.process-pivot-column-setting :deep(.ant-checkbox + span) {
  min-width: 0;
  padding-inline-start: 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.column-setting-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  border-top: 1px solid var(--report-border);
  background: #fff;
  padding: 12px 4px 0;
}

.process-pivot-advanced-query-body {
  display: grid;
  gap: 10px;
}

.process-pivot-advanced-query-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.process-pivot-query-item {
  display: grid;
  grid-template-columns: 90px minmax(0, 1fr);
  min-width: 0;
  overflow: hidden;
  border: 1px solid #c6d3df;
}

.process-pivot-query-item label {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  min-height: 32px;
  padding: 0 10px;
  color: #334155;
  font-size: 12px;
  font-weight: 800;
  white-space: nowrap;
  background: #dbe3ed;
  border-right: 1px solid #c6d3df;
}

.process-pivot-query-item > :not(label) {
  min-width: 0;
  background: #f8fafc;
}

.process-pivot-query-item :deep(.ant-input),
.process-pivot-query-item :deep(.ant-input-affix-wrapper),
.process-pivot-query-item :deep(.ant-picker),
.process-pivot-query-item :deep(.ant-select),
.process-pivot-query-item :deep(.ant-select-selector) {
  width: 100%;
  min-height: 32px;
  border: 0;
  border-radius: 0;
  box-shadow: none;
}

.process-pivot-advanced-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding-top: 8px;
  border-top: 1px solid #d7e0ea;
}

.report-toolbar,
.report-filter-shell,
.report-pagination {
  border: 1px solid var(--report-border);
  border-radius: 4px;
  background: #fff;
}

.report-toolbar {
  display: flex;
  min-height: 36px;
  align-items: center;
  justify-content: space-between;
  padding: 5px 8px;
}

.report-title {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 12px;
}

.report-title-main {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  white-space: nowrap;
}

.report-title-main span {
  font-size: 15px;
  font-weight: 650;
}

.report-stats {
  display: flex;
  min-width: 0;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  color: var(--report-muted);
}

.report-stats span {
  display: inline-flex;
  height: 20px;
  align-items: center;
  border: 1px solid #e4e7ec;
  border-radius: 3px;
  background: #f8fafc;
  padding: 0 6px;
  white-space: nowrap;
}

.cell-sub {
  color: var(--report-muted);
  font-size: 11px;
}

.report-filter-shell {
  padding: 6px;
}

.quick-filter-row {
  display: grid;
  grid-template-columns:
    140px
    152px
    152px
    164px
    minmax(12px, 1fr)
    auto
    auto
    auto;
  gap: 6px;
  align-items: center;
}

.advanced-filter-row {
  display: grid;
  grid-template-columns: repeat(9, minmax(106px, 1fr));
  gap: 6px;
  margin-top: 6px;
  border-top: 1px dashed #e4e7ec;
  padding-top: 6px;
}

.date-input {
  width: 100%;
}

.filter-buttons {
  display: flex;
  gap: 6px;
}

.filter-spacer {
  min-width: 0;
}

.report-body {
  display: flex;
  flex: 1 1 0;
  min-height: 0;
  min-width: 0;
  flex-direction: column;
  overflow: hidden;
}

.report-table-host {
  position: relative;
  flex: 1 1 0;
  min-height: 0;
  min-width: 0;
  overflow: hidden;
}

.pivot-table-wrap {
  position: absolute;
  inset: 0;
  overflow: scroll;
  border: 1px solid var(--report-border);
  border-radius: 6px 6px 0 0;
  background: #fff;
  scrollbar-color: #8ca3b8 #d9e3ec;
  scrollbar-gutter: stable;
}

.pivot-table-wrap::-webkit-scrollbar {
  width: 12px;
  height: 12px;
}

.pivot-table-wrap::-webkit-scrollbar-track {
  background: #d9e3ec;
}

.pivot-table-wrap::-webkit-scrollbar-thumb {
  border: 2px solid #d9e3ec;
  border-radius: 999px;
  background: #7f98ad;
}

.pivot-table-wrap::-webkit-scrollbar-corner {
  background: #d9e3ec;
}

.table-loading-cover {
  position: absolute;
  z-index: 8;
  display: grid;
  background: rgb(255 255 255 / 58%);
  inset: 0;
  place-items: center;
  pointer-events: none;
}

.pivot-table {
  width: 4306px;
  min-width: 4306px;
  border-collapse: collapse;
  font-size: 12px;
  table-layout: fixed;
}

.pivot-table th,
.pivot-table td {
  height: 28px;
  border-right: 1px solid var(--report-border);
  border-bottom: 1px solid var(--report-border);
  padding: 2px 5px;
  overflow: hidden;
  text-overflow: ellipsis;
  text-align: center;
  vertical-align: middle;
  white-space: nowrap;
}

.pivot-table th {
  position: sticky;
  top: 0;
  z-index: 3;
  background: var(--report-header);
  color: #344054;
  font-weight: 650;
  text-align: center;
}

.pivot-table thead tr:nth-child(2) th {
  top: 28px;
  background: var(--report-soft);
  font-weight: 600;
}

.pivot-table tbody td[rowspan]:not([rowspan='1']) {
  padding-top: 6px;
  vertical-align: top;
}

.pivot-table tbody tr:hover td {
  background: #f8fbff;
}

.pivot-table tbody tr.pivot-row--selected td {
  background: #eaf4ff;
  box-shadow:
    inset 0 1px 0 rgba(37, 99, 235, 0.16),
    inset 0 -1px 0 rgba(37, 99, 235, 0.16);
}

.pivot-table tbody tr.pivot-row--selected td:first-child {
  box-shadow:
    inset 3px 0 0 #2563eb,
    inset 0 1px 0 rgba(37, 99, 235, 0.16),
    inset 0 -1px 0 rgba(37, 99, 235, 0.16);
}

.pivot-table tbody tr.pivot-row--selected:hover td {
  background: #e0f0ff;
}

.stage-group {
  border-top: 2px solid #8aa4c4;
}

.col-plan {
  width: 132px;
}

.col-status {
  width: 78px;
}

.col-mother-batch {
  width: 128px;
}

.col-model {
  width: 112px;
}

.col-material {
  width: 132px;
}

.col-size {
  width: 78px;
}

.col-time,
.col-stage-time {
  width: 96px;
}

.col-batch {
  width: 126px;
}

.col-stage-metric {
  width: 78px;
}

.col-stage-qty {
  width: 76px;
}

.sticky-plan,
.sticky-mother-batch,
.sticky-status {
  position: sticky;
  z-index: 4;
  background: #fff;
}

th.sticky-plan,
th.sticky-mother-batch,
th.sticky-status {
  z-index: 5;
  background: var(--report-header);
}

.sticky-plan {
  left: 0;
}

.sticky-mother-batch {
  left: 132px;
}

.sticky-status {
  left: 260px;
}

.cell-plan {
  font-weight: 600;
}

.pivot-table td.cell-plan--with-tag {
  height: 38px;
  padding-top: 3px;
  padding-bottom: 3px;
}

.plan-cell-content {
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  max-width: 100%;
  gap: 1px;
  line-height: 16px;
}

.cell-plan--with-tag .plan-cell-content {
  min-height: 32px;
}

.cell-mother-batch {
  font-weight: 500;
}

.cell-status {
  text-align: center;
}

.plan-link {
  display: block;
  flex: 0 1 auto;
  max-width: 100%;
  min-width: 0;
  overflow: hidden;
  border: 0;
  background: transparent;
  color: #1d4ed8;
  cursor: pointer;
  font: inherit;
  padding: 0;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.plan-no-tag {
  display: inline-flex;
  flex: 0 0 16px;
  max-width: 100%;
  height: 16px;
  align-items: center;
  margin-inline-end: 0;
  padding-inline: 4px;
  border-radius: 2px;
  font-size: 11px;
  font-weight: 500;
  line-height: 14px;
}

.num {
  text-align: right;
}

.text-cell {
  text-align: center;
}

.qty-link {
  border: 0;
  background: transparent;
  color: #1d4ed8;
  cursor: pointer;
  font: inherit;
  padding: 0;
}

.qty-link:hover {
  text-decoration: underline;
}

.qty-link--pending {
  color: #9a3412;
}

.qty-link--defect {
  color: #b42318;
}

.qty-link--inspection {
  color: #0369a1;
}

.qty-link--inspection-ng {
  color: #be123c;
}

.batch-text {
  display: inline-block;
  max-width: 116px;
  overflow: hidden;
  text-overflow: ellipsis;
  vertical-align: bottom;
}

.pending-cell {
  color: #9a3412;
}

.defect-cell {
  color: #b42318;
}

.inspection-ng-cell {
  color: #be123c;
}

.qtime-timeout-cell {
  background: #fff1f0;
  color: #a8071a;
}

.qtime-timeout-cell .qty-link {
  color: #a8071a;
  font-weight: 700;
}

.qtime-timeout-cell .stage-status-dot {
  box-shadow: 0 0 0 2px rgba(248, 113, 113, 0.28);
}

.qtime-detail-panel {
  display: flex;
  flex: 0 0 32px;
  min-height: 32px;
  min-width: 0;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid var(--report-border);
  border-top: 0;
  background: #fff;
}

.qtime-detail-panel--expanded {
  flex-basis: 144px;
  min-height: 144px;
}

.qtime-detail-toggle {
  display: grid;
  flex: 0 0 32px;
  width: 100%;
  height: 32px;
  min-width: 0;
  align-items: center;
  gap: 8px;
  grid-template-columns: max-content minmax(0, 1fr) max-content max-content;
  border: 0;
  border-bottom: 1px solid var(--report-border);
  background: #f8fafc;
  color: #334155;
  cursor: pointer;
  padding: 0 10px;
  text-align: left;
}

.qtime-detail-toggle:hover {
  background: #eef6ff;
}

.qtime-detail-toggle__title,
.qtime-detail-toggle__meta,
.qtime-detail-toggle__action {
  display: inline-flex;
  min-width: 0;
  align-items: center;
  gap: 6px;
  overflow: hidden;
  white-space: nowrap;
}

.qtime-detail-toggle__title {
  color: #0f4f7a;
  font-weight: 800;
}

.qtime-detail-toggle__meta {
  color: #475569;
}

.qtime-detail-toggle__meta strong,
.qtime-detail-toggle__meta span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
}

.qtime-detail-toggle__action {
  color: #0f6fce;
  font-weight: 800;
}

.qtime-detail-content {
  display: grid;
  flex: 1 1 0;
  min-height: 0;
  min-width: 0;
  grid-template-columns: 250px minmax(0, 1fr);
  overflow: hidden;
}

.qtime-detail-context {
  display: grid;
  min-width: 0;
  align-content: center;
  gap: 4px;
  border-right: 1px solid var(--report-border);
  padding: 9px 10px;
  color: #334155;
}

.qtime-detail-context strong,
.qtime-detail-context span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.qtime-detail-title {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  color: #0f4f7a;
  font-weight: 800;
}

.qtime-detail-status-tag {
  width: fit-content;
  margin-inline-end: 0;
}

.qtime-detail-list {
  display: flex;
  min-width: 0;
  gap: 8px;
  overflow-x: auto;
  overflow-y: hidden;
  padding: 8px;
  scrollbar-color: #8ca3b8 #eef2f7;
}

.qtime-detail-card {
  display: grid;
  flex: 0 0 286px;
  min-width: 286px;
  align-content: start;
  gap: 4px;
  border: 1px solid #cbd5e1;
  border-left: 4px solid #16a34a;
  background: #f8fafc;
  padding: 7px 9px;
  color: #263445;
}

.qtime-detail-card--timeout {
  border-color: #fca5a5;
  border-left-color: #dc2626;
  background: #fff1f0;
}

.qtime-detail-card__head,
.qtime-detail-card__duration,
.qtime-detail-card__times {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 6px;
  overflow: hidden;
  white-space: nowrap;
}

.qtime-detail-card__head {
  justify-content: space-between;
  font-weight: 800;
}

.qtime-detail-card__tag {
  flex: 0 0 auto;
  margin-inline-end: 0;
}

.qtime-detail-card__duration span,
.qtime-detail-card__duration em,
.qtime-detail-card__times {
  color: #64748b;
  font-size: 11px;
  font-style: normal;
}

.qtime-detail-card__duration strong {
  color: #0f172a;
  font-size: 14px;
}

.qtime-detail-card--timeout .qtime-detail-card__duration strong {
  color: #b91c1c;
}

.qtime-detail-card__times span,
.qtime-detail-card__message {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
}

.qtime-detail-card__message {
  color: #475569;
  font-size: 11px;
  line-height: 1.35;
  white-space: nowrap;
}

.qtime-detail-card--timeout .qtime-detail-card__message {
  color: #991b1b;
  font-weight: 700;
}

.qtime-detail-empty {
  display: flex;
  min-width: 0;
  align-items: center;
  color: var(--report-muted);
  padding: 0 14px;
}

.empty-cell {
  height: 96px;
  color: var(--report-muted);
  text-align: center;
}

.report-pagination {
  display: flex;
  min-height: 32px;
  flex-shrink: 0;
  align-items: center;
  justify-content: flex-end;
  border-top: 0;
  border-radius: 0 0 6px 6px;
  padding: 3px 8px;
  box-shadow: 0 -1px 0 rgba(15, 23, 42, 0.04);
}

.stage-status-dot {
  display: inline-block;
  width: 7px;
  height: 7px;
  margin-right: 4px;
  border-radius: 50%;
  background: #cbd5e1;
  vertical-align: 1px;
}

.stage-status--pending {
  background: #f59e0b;
}

.stage-status--running {
  background: #2563eb;
}

.stage-status--finished {
  background: #16a34a;
}

.piece-modal-title {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.piece-modal-title span {
  font-size: 15px;
  font-weight: 650;
}

.piece-modal-title small {
  color: var(--report-muted);
  font-size: 12px;
}

.piece-modal-toolbar {
  display: grid;
  grid-template-columns: minmax(160px, 1fr) auto;
  gap: 8px;
  align-items: center;
  margin-bottom: 10px;
}

.drilldown-table-wrap {
  max-height: 460px;
  overflow: auto;
  border: 1px solid var(--report-border);
}

.drilldown-table {
  width: max-content;
  min-width: 760px;
  border-collapse: collapse;
  table-layout: auto;
  font-size: 12px;
}

.drilldown-table th {
  position: sticky;
  top: 0;
  z-index: 1;
  border: 1px solid #d7e1ec;
  background: #eef3f8;
  color: #344054;
  font-weight: 700;
  padding: 7px 8px;
  text-align: left;
  white-space: nowrap;
}

.drilldown-table td {
  border: 1px solid #dfe7f0;
  color: var(--report-text);
  line-height: 1.45;
  padding: 7px 8px;
  text-align: left;
  vertical-align: middle;
  word-break: break-word;
}

.drilldown-table th.is-piece-no,
.drilldown-table td.is-piece-no {
  white-space: nowrap;
  word-break: keep-all;
  overflow-wrap: normal;
}

.drilldown-table tr:nth-child(even) td {
  background: #fbfdff;
}

.drilldown-table .is-center {
  text-align: center;
}

.drilldown-table .is-right {
  text-align: right;
}

.drilldown-table :deep(.ant-tag) {
  margin-inline-end: 0;
}

.drilldown-table :deep(.ant-btn-link) {
  height: 20px;
  padding: 0;
  font-size: 12px;
}

.piece-card-grid {
  display: grid;
  max-height: 420px;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  grid-auto-rows: minmax(76px, auto);
  gap: 5px;
  overflow: auto;
  border: 1px solid #9bb8cc;
  background:
    radial-gradient(circle at 18% 14%, rgb(255 255 255 / 18%), transparent 18%),
    repeating-linear-gradient(
      0deg,
      rgb(255 255 255 / 8%) 0 1px,
      transparent 1px 10px
    ),
    repeating-linear-gradient(
      90deg,
      rgb(15 76 117 / 10%) 0 1px,
      transparent 1px 10px
    ),
    linear-gradient(135deg, #a6c8dc 0%, #79a8c4 100%);
  padding: 10px;
  scrollbar-color: #6f91aa #c8d9e7;
}

.piece-card {
  position: relative;
  display: grid;
  min-height: 76px;
  align-content: center;
  gap: 3px;
  overflow: hidden;
  border: 1px solid rgb(43 90 124 / 74%);
  border-radius: 2px;
  background:
    linear-gradient(
      135deg,
      rgb(255 255 255 / 46%) 0%,
      rgb(255 255 255 / 10%) 36%,
      transparent 37%
    ),
    repeating-linear-gradient(
      45deg,
      rgb(255 255 255 / 16%) 0 1px,
      transparent 1px 7px
    ),
    repeating-linear-gradient(
      -45deg,
      rgb(19 72 105 / 10%) 0 1px,
      transparent 1px 7px
    ),
    #d9ebf4;
  box-shadow:
    inset 0 0 0 1px rgb(255 255 255 / 55%),
    inset 0 -12px 20px rgb(47 93 118 / 12%),
    0 1px 0 rgb(255 255 255 / 45%);
  color: #19324b;
  padding: 8px;
  text-align: center;
}

.piece-card::before {
  position: absolute;
  inset: 7px;
  border: 1px solid rgb(255 255 255 / 36%);
  border-radius: 1px;
  content: '';
  pointer-events: none;
}

.piece-card::after {
  position: absolute;
  top: 7px;
  right: 7px;
  width: 14px;
  height: 14px;
  border-top: 2px solid rgb(15 76 117 / 36%);
  border-right: 2px solid rgb(15 76 117 / 36%);
  content: '';
  pointer-events: none;
}

.piece-card strong,
.piece-card span,
.piece-card small,
.piece-card em,
.piece-card-head {
  position: relative;
  z-index: 1;
  overflow: hidden;
  color: inherit;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.piece-card .piece-card-note {
  display: block;
  line-height: 1.35;
  max-height: 46px;
  overflow: auto;
  text-overflow: clip;
  white-space: normal;
  word-break: break-word;
}

.piece-card .piece-card-note--defect {
  color: #9f1239;
  font-weight: 700;
}

.piece-card-action {
  position: relative;
  z-index: 1;
  justify-self: center;
  width: fit-content;
  height: 20px;
  padding: 0 4px;
  font-size: 11px;
  line-height: 18px;
}

.piece-card-head {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  min-width: 0;
}

.piece-card-head strong {
  min-width: 0;
}

.piece-card-head :deep(.ant-tag) {
  margin-inline-end: 0;
  font-size: 10px;
  line-height: 16px;
  padding: 0 4px;
}

.piece-card strong {
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.2px;
}

.piece-card span,
.piece-card small,
.piece-card em {
  font-size: 11px;
}

.piece-card em {
  font-style: normal;
}

.piece-card.is-done {
  border-color: rgb(22 101 52 / 72%);
  background:
    linear-gradient(
      135deg,
      rgb(255 255 255 / 46%) 0%,
      rgb(255 255 255 / 10%) 36%,
      transparent 37%
    ),
    repeating-linear-gradient(
      45deg,
      rgb(255 255 255 / 18%) 0 1px,
      transparent 1px 7px
    ),
    repeating-linear-gradient(
      -45deg,
      rgb(21 128 61 / 10%) 0 1px,
      transparent 1px 7px
    ),
    #a9efbf;
  box-shadow:
    inset 0 0 0 2px rgb(22 163 74 / 28%),
    0 0 12px rgb(34 197 94 / 24%);
  color: #14532d;
}

.piece-card.is-defect {
  border-color: rgb(185 28 28 / 80%);
  background:
    linear-gradient(
      135deg,
      rgb(255 255 255 / 40%) 0%,
      rgb(255 255 255 / 10%) 34%,
      transparent 35%
    ),
    repeating-linear-gradient(
      45deg,
      rgb(255 255 255 / 14%) 0 1px,
      transparent 1px 7px
    ),
    repeating-linear-gradient(
      -45deg,
      rgb(127 29 29 / 12%) 0 1px,
      transparent 1px 7px
    ),
    #f7b4b4;
  box-shadow:
    inset 0 0 0 2px rgb(220 38 38 / 28%),
    inset 0 -12px 20px rgb(127 29 29 / 14%);
  color: #5f1616;
}

.piece-card.is-pending {
  border-color: rgb(194 110 28 / 72%);
  background:
    linear-gradient(
      135deg,
      rgb(255 255 255 / 42%) 0%,
      rgb(255 255 255 / 12%) 34%,
      transparent 35%
    ),
    repeating-linear-gradient(
      45deg,
      rgb(255 255 255 / 16%) 0 1px,
      transparent 1px 7px
    ),
    repeating-linear-gradient(
      -45deg,
      rgb(146 64 14 / 10%) 0 1px,
      transparent 1px 7px
    ),
    #ffd6a3;
  box-shadow:
    inset 0 0 0 2px rgb(249 115 22 / 24%),
    inset 0 -12px 20px rgb(154 75 16 / 12%);
  color: #7c2d12;
}

.piece-card.is-inspection {
  border-color: rgb(3 105 161 / 72%);
  background:
    linear-gradient(
      135deg,
      rgb(255 255 255 / 44%) 0%,
      rgb(255 255 255 / 12%) 35%,
      transparent 36%
    ),
    repeating-linear-gradient(
      45deg,
      rgb(255 255 255 / 16%) 0 1px,
      transparent 1px 7px
    ),
    repeating-linear-gradient(
      -45deg,
      rgb(7 89 133 / 10%) 0 1px,
      transparent 1px 7px
    ),
    #bde6f8;
  box-shadow:
    inset 0 0 0 2px rgb(14 165 233 / 24%),
    inset 0 -12px 20px rgb(7 89 133 / 12%);
  color: #075985;
}

.piece-card.is-inspection_ng {
  border-color: rgb(190 18 60 / 80%);
  background:
    linear-gradient(
      135deg,
      rgb(255 255 255 / 40%) 0%,
      rgb(255 255 255 / 10%) 34%,
      transparent 35%
    ),
    repeating-linear-gradient(
      45deg,
      rgb(255 255 255 / 14%) 0 1px,
      transparent 1px 7px
    ),
    repeating-linear-gradient(
      -45deg,
      rgb(136 19 55 / 12%) 0 1px,
      transparent 1px 7px
    ),
    #fac0cf;
  box-shadow:
    inset 0 0 0 2px rgb(225 29 72 / 26%),
    inset 0 -12px 20px rgb(136 19 55 / 14%);
  color: #881337;
}

.piece-empty {
  grid-column: 1 / -1;
  color: #24455f;
  font-size: 12px;
  padding: 28px 0;
  text-align: center;
}

@media (max-width: 900px) {
  .piece-card-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

:deep(.ant-input-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-select-multiple.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small) {
  min-height: 24px;
  font-size: 12px;
}

:deep(.ant-btn-sm) {
  height: 24px;
  padding-inline: 8px;
  font-size: 12px;
}

@media (max-width: 1180px) {
  .process-pivot-report {
    grid-template-rows: max-content max-content minmax(0, 1fr);
  }

  .process-pivot-banner {
    max-height: none;
    flex-wrap: wrap;
  }

  .process-pivot-action-group {
    width: 100%;
    padding-left: 0 !important;
    border-left: 0 !important;
  }

  .process-pivot-simple-query {
    grid-template-columns: 86px 220px 86px minmax(220px, 1fr) 88px 110px;
  }

  .quick-filter-row {
    grid-template-columns: repeat(4, minmax(120px, 1fr)) auto auto auto;
  }

  .filter-spacer {
    display: none;
  }

  .advanced-filter-row {
    grid-template-columns: repeat(5, minmax(106px, 1fr));
  }
}

@media (max-width: 1100px) {
  .report-title {
    align-items: flex-start;
    flex-direction: column;
    gap: 4px;
  }

  .quick-filter-row,
  .advanced-filter-row {
    grid-template-columns: repeat(3, minmax(106px, 1fr));
  }
}

@media (max-width: 760px) {
  .process-pivot-simple-query,
  .process-pivot-advanced-query-grid,
  .column-setting-stage {
    grid-template-columns: 1fr;
  }

  .process-pivot-query-item {
    grid-template-columns: 86px minmax(0, 1fr);
  }

  .process-pivot-simple-query-label {
    justify-content: flex-start;
  }
}
</style>
