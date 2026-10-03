<script lang="ts" setup>
import type { Dayjs } from 'dayjs';
import type { MesQmsMotherRollGoodStatisticsApi } from '#/api/mes/quality/statistics/mother-roll-good-statistics';

import { computed, onMounted, reactive, ref } from 'vue';

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
  exportMotherRollGoodStatistics,
  getMotherRollGoodStatisticsPage,
} from '#/api/mes/quality/statistics/mother-roll-good-statistics';

import { PLAN_STATUS_OPTIONS } from './data';
import { collectProcessLossPieces } from './process-loss-details';
import InspectionDetailModalForm from '../../shared/inspection-detail-modal.vue';
import '../../../hc/package-fg/shared/cut-round-board.css';

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
  | 'segmentBatchNo'
  | 'sizeSpec';
type ColumnWidthKey =
  | 'batch'
  | 'material'
  | 'model'
  | 'motherBatch'
  | 'plan'
  | 'segmentBatch'
  | 'size'
  | 'stageMetric'
  | 'stageOutput'
  | 'stageQty'
  | 'stageTarget'
  | 'stageTime'
  | 'stageWideQty'
  | 'status';
type StageSubColumnKey =
  | 'batch'
  | 'defectQty'
  | 'doneQty'
  | 'coaInspectionNgQty'
  | 'coaInspectionQty'
  | 'finalInspectionOutputQty'
  | 'finalInspectionYieldRate'
  | 'goodTargetRate'
  | 'goodYieldRate'
  | 'glueBoardInspectionQty'
  | 'inputQty'
  | 'inspectionNgQty'
  | 'inspectionQty'
  | 'lastReportTime'
  | 'motherOutputQty'
  | 'pendingQty'
  | 'processProductionInspectionQty'
  | 'segmentDefectQty'
  | 'segmentInputQty'
  | 'theoreticalOutputQty'
  | StageMetricColumn['field'];

interface BaseColumn {
  className: string;
  key: BaseColumnKey;
  label: string;
  required?: boolean;
  sticky?: 'motherBatch' | 'plan' | 'segmentBatch' | 'status';
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
    showBatch: false,
  },
  { code: 'ADHESIVE1', label: '粘胶1', batchLabel: '分段批号' },
  { code: 'SLITTING', label: '分切', batchLabel: '片号', showBatch: false },
  { code: 'PRESS_SLOT', label: '压槽', batchLabel: '片号', showBatch: false },
  { code: 'ADHESIVE2', label: '粘胶2', batchLabel: '片号', showBatch: false },
  { code: 'CUT_ROUND', label: '裁切', batchLabel: '片号', showBatch: false },
  { code: 'SHIPPING_INSPECTION', label: '发货检验', batchLabel: '片号', showBatch: false },
];

const PENDING_STAGE_CODES = new Set([
  'SLITTING',
  'PRESS_SLOT',
  'ADHESIVE2',
  'CUT_ROUND',
]);
const PIECE_INPUT_STAGE_CODES = new Set([
  'SLITTING',
  'PRESS_SLOT',
  'ADHESIVE2',
  'CUT_ROUND',
]);
const COA_INSPECTION_STAGE_CODES = new Set([
  'PRESS_SLOT',
  'ADHESIVE2',
  'CUT_ROUND',
]);
const OUTPUT_BATCH_PREFERRED_STAGE_CODES = new Set([
  'PRESS_SLOT',
  'ADHESIVE2',
  'CUT_ROUND',
]);
const PIECE_SELF_CHECK_STAGE_CODES = PENDING_STAGE_CODES;
const MOTHER_OUTPUT_STAGE_CODES = new Set([
  'GRINDING',
  'ADHESIVE1',
  'SLITTING',
  'PRESS_SLOT',
  'ADHESIVE2',
  'CUT_ROUND',
]);
const THEORETICAL_OUTPUT_STAGE_CODES = new Set([
  'FORMULA',
  'WET',
  'GRINDING',
  'ADHESIVE1',
  'SLITTING',
  'PRESS_SLOT',
  'ADHESIVE2',
  'CUT_ROUND',
]);

type PieceDialogKind =
  | 'COA_INSPECTION'
  | 'COA_INSPECTION_NG'
  | 'DEFECT'
  | 'DONE'
  | 'INSPECTION'
  | 'INSPECTION_NG'
  | 'PENDING'
  | 'PROCESS_LOSS';

interface DrilldownTableColumn {
  align?: 'center' | 'left' | 'right';
  defectColumn?: boolean;
  key: string;
  label: string;
  minWidth?: number;
  width?: number;
}

interface DrilldownTableRow {
  actionText?: string;
  [key: string]: unknown;
  defectSummary?: string;
  inspectionId?: number;
  inspectionNo?: string;
  inspectionType?: string;
  remark?: string;
  sourceType?: string;
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
  { align: 'center', key: 'firstInspectionFlagText', label: '首检标记', width: 84 },
  { align: 'center', key: 'inspectionFlagText', label: '送检标记', width: 84 },
  { key: 'inspectionResult', label: '送检结果', minWidth: 108 },
];

const DEFECT_CATEGORY_COLUMNS: DrilldownTableColumn[] = [
  { align: 'right', defectColumn: true, key: 'defectBlackDot', label: '黑点', width: 72 },
  { align: 'right', defectColumn: true, key: 'defectBlueDot', label: '蓝点', width: 72 },
  { align: 'right', defectColumn: true, key: 'defectYellowDot', label: '黄点', width: 72 },
  { align: 'right', defectColumn: true, key: 'defectRedDot', label: '红点', width: 72 },
  { align: 'right', defectColumn: true, key: 'defectPinHole', label: '针孔', width: 72 },
  { align: 'right', defectColumn: true, key: 'defectStripe', label: '条纹', width: 72 },
  { align: 'right', defectColumn: true, key: 'defectWrinkle', label: '褶皱', width: 72 },
  { align: 'right', defectColumn: true, key: 'defectWave', label: '波浪纹', width: 82 },
  { align: 'right', defectColumn: true, key: 'defectOther', label: '其他', width: 72 },
];
const DEFECT_CATEGORY_LABELS = DEFECT_CATEGORY_COLUMNS.map((column) => ({
  key: column.key,
  label: column.label,
}));

const PIECE_NG_DRILLDOWN_COLUMNS: DrilldownTableColumn[] = [
  { align: 'center', key: 'index', label: '序号', width: 56 },
  { key: 'pieceNo', label: '片号', minWidth: 188 },
  ...PIECE_REPORT_INFO_COLUMNS,
  { key: 'scanConfirmTime', label: '扫码确认时间', minWidth: 150 },
  ...DEFECT_CATEGORY_COLUMNS,
];

const FORMULA_DRILLDOWN_COLUMNS: DrilldownTableColumn[] = [
  { align: 'center', key: 'index', label: '序号', width: 56 },
  { align: 'right', key: 'doneQtyText', label: '完工', minWidth: 120 },
];

const WET_DRILLDOWN_COLUMNS: DrilldownTableColumn[] = [
  { align: 'center', key: 'index', label: '序号', width: 56 },
  { align: 'right', key: 'wetReportQtyText', label: '收卷米数(报工数)', minWidth: 150 },
  { align: 'right', key: 'napInspectionQtyText', label: 'NAP层送检(米)', minWidth: 138 },
  { align: 'right', key: 'fixedLossQtyText', label: '固定损耗（米）', minWidth: 132 },
];

const METER_REPORT_DRILLDOWN_COLUMNS: DrilldownTableColumn[] = [
  { align: 'center', key: 'index', label: '序号', width: 56 },
  { align: 'right', key: 'inputQtyText', label: '投入米数', minWidth: 110 },
  { align: 'right', key: 'fixedLossQtyText', label: '固定损耗（米）', minWidth: 132 },
  { align: 'right', key: 'napSampleQtyText', label: 'NAP留样米数/送检送数', minWidth: 178 },
  { align: 'right', key: 'outputQtyText', label: '产出米数', minWidth: 110 },
];

const INSPECTION_DRILLDOWN_COLUMNS: DrilldownTableColumn[] = [
  { align: 'center', key: 'index', label: '序号', width: 56 },
  { key: 'pieceNo', label: '片号/批号', minWidth: 188 },
  { key: 'scanConfirmTime', label: '送检时间', minWidth: 150 },
  { key: 'inspectionType', label: '送检类型', minWidth: 110 },
  { align: 'right', key: 'inspectionQtyText', label: '送检数量', minWidth: 104 },
  { align: 'right', key: 'inspectionNgQtyText', label: 'NG数量', minWidth: 92 },
  { key: 'inspectionResult', label: '送检结果', minWidth: 108 },
  { key: 'ngType', label: 'NG类型', minWidth: 136 },
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

const COLUMN_CONFIG_STORAGE_KEY = 'mes:qms-mother-roll-good-statistics:column-config:v1';
const COLUMN_CONFIG_VERSION = 16;

const COLUMN_WIDTHS = {
  batch: 126,
  material: 132,
  model: 112,
  motherBatch: 128,
  plan: 132,
  segmentBatch: 136,
  size: 78,
  stageMetric: 78,
  stageOutput: 92,
  stageQty: 76,
  stageTarget: 92,
  stageTime: 96,
  stageWideQty: 148,
  status: 78,
};

const BASE_COLUMNS: BaseColumn[] = [
  {
    className: 'col-mother-batch',
    key: 'motherRollBatchNo',
    label: '母批批号',
    sticky: 'motherBatch',
    widthKey: 'motherBatch',
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
  {
    className: 'col-segment-batch',
    key: 'segmentBatchNo',
    label: '磨皮分段片号',
    sticky: 'segmentBatch',
    widthKey: 'segmentBatch',
  },
];

const loading = ref(false);
const rows = ref<MesQmsMotherRollGoodStatisticsApi.ProcessPivotRow[]>([]);
const total = ref(0);
const pageNo = ref(1);
const pageSize = ref(20);
const advancedQueryVisible = ref(false);
const columnSettingVisible = ref(false);
const exporting = ref(false);
const formulaNoteExpanded = ref(false);
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
  rows: [] as DrilldownTableRow[],
  stageName: '',
  subtitle: '',
  title: '',
});
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
  left: '0',
}));
const segmentBatchStickyStyle = computed(() => ({
  left: isBaseColumnVisible('motherRollBatchNo')
    ? `${COLUMN_WIDTHS.motherBatch}px`
    : '0',
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
const queryFieldConfigs: PivotQueryFieldConfig[] = [
  { key: 'planStatuses', label: '计划状态', options: PLAN_STATUS_OPTIONS, type: 'multiple' },
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
const segmentRowSpanMap = computed(() => {
  return buildRowSpanMap(rows.value, segmentRowKey);
});
const modelSizeRowSpanMap = computed(() => {
  return buildRowSpanMap(rows.value, modelSizeRowKey);
});
const stageCellRowSpanMap = computed(() => {
  const spanMap = new Map<string, { firstIndex: number; rowSpan: number }>();
  STAGE_COLUMNS.forEach((stage) => {
    stageSubColumns(stage).forEach((column) => {
      const stageMap = buildRowSpanMap(rows.value, (row) =>
        stageCellRowKey(row, stage.code, column.key),
      );
      stageMap.forEach((value, key) => {
        spanMap.set(`${stage.code}|${column.key}|${key}`, value);
      });
    });
  });
  return spanMap;
});
const filteredPieceItems = computed(() => {
  const keyword = pieceDialog.keyword.trim().toLowerCase();
  if (!keyword) {
    return pieceDialog.rows;
  }
  return pieceDialog.rows.filter((item) =>
    Object.values(item)
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

function applyPlanDateRange(value: [Dayjs, Dayjs] | null, refresh = true) {
  if (!value) {
    selectedPlanDateRange.value = null;
    queryForm.productionStartDateStart = undefined;
    queryForm.productionStartDateEnd = undefined;
  } else {
    selectedPlanDateRange.value = value;
    queryForm.productionStartDateStart = formatDateRangeValue(value[0]);
    queryForm.productionStartDateEnd = formatDateRangeValue(value[1]);
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
    start.isValid() && end.isValid()
      ? ([start, end] as [Dayjs, Dayjs])
      : null;
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
    normalized.base[column.key] = column.required
      ? true
      : hasSourceValue
        ? value?.base?.[column.key] !== false
        : isDefaultBaseColumnVisible(column);
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
  PIECE_INPUT_STAGE_CODES.forEach((stageCode) => {
    config.stages[stageCode] = {
      ...config.stages[stageCode],
      defectQty: false,
      inspectionNgQty: true,
      inspectionQty: true,
      segmentInputQty: false,
    };
  });
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
      target[column.key] = nextConfig.stages[stage.code]?.[column.key] !== false;
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

function getCheckedFromEvent(event: Event | { target?: { checked?: boolean } }) {
  return Boolean((event as { target?: { checked?: boolean } }).target?.checked);
}

function setQueryFieldValue(key: PivotQueryFieldKey, value?: string | string[]) {
  if (key === 'planStatuses') {
    queryForm.planStatuses = Array.isArray(value)
      ? [...value]
      : value
        ? [value]
        : [];
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
  const params: MesQmsMotherRollGoodStatisticsApi.ProcessPivotPageReqVO = {
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
      visibleStageSubColumns(stage).map((column) => `${stage.code}.${column.key}`),
    ),
  };
}

function buildExportQueryParams() {
  const params = { ...buildQueryParams() } as Record<string, unknown>;
  delete params.pageNo;
  delete params.pageSize;
  Object.assign(params, buildExportColumnParams());
  return params as MesQmsMotherRollGoodStatisticsApi.ProcessPivotPageReqVO;
}

async function fetchData() {
  loading.value = true;
  try {
    const result = await getMotherRollGoodStatisticsPage(buildQueryParams());
    rows.value = normalizePivotRows(result.list || []);
    total.value = Number(result.total || 0);
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  pageNo.value = 1;
  fetchData();
}

function handlePlanDateRangeChange(value: (Dayjs | null)[] | [Dayjs, Dayjs] | null) {
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
  applyPlanDateRange([
    start.add(offset, 'week'),
    end.add(offset, 'week'),
  ] as [Dayjs, Dayjs]);
}

async function handleExportExcel() {
  const hideLoading = message.loading({
    content: '正在导出母卷批次良品统计...',
    duration: 0,
  });
  exporting.value = true;
  try {
    const data = await exportMotherRollGoodStatistics(buildExportQueryParams());
    downloadFileFromBlobPart({ fileName: '母卷批次良品统计.xlsx', source: data });
    message.success('母卷批次良品统计已导出');
  } finally {
    exporting.value = false;
    hideLoading();
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

function stageOf(row: MesQmsMotherRollGoodStatisticsApi.ProcessPivotRow, code: string) {
  return (row.stages?.[code] || {}) as MesQmsMotherRollGoodStatisticsApi.ProcessPivotStage;
}

function buildRowSpanMap(
  sourceRows: MesQmsMotherRollGoodStatisticsApi.ProcessPivotRow[],
  keyGetter: (row: MesQmsMotherRollGoodStatisticsApi.ProcessPivotRow) => string,
) {
  const spanMap = new Map<string, { firstIndex: number; rowSpan: number }>();
  let previousKey = '';
  sourceRows.forEach((row, index) => {
    const key = keyGetter(row);
    if (key && key === previousKey) {
      const current = spanMap.get(key);
      if (current) {
        current.rowSpan += 1;
      }
    } else {
      spanMap.set(key, { firstIndex: index, rowSpan: 1 });
      previousKey = key;
    }
  });
  return spanMap;
}

function normalizePivotRows(
  sourceRows: MesQmsMotherRollGoodStatisticsApi.ProcessPivotRow[],
) {
  return sourceRows
    .map((row, index) => ({
      index,
      row: normalizePivotRow(row),
    }))
    .sort((left, right) => comparePivotRows(left.row, right.row) || left.index - right.index)
    .map((item) => item.row);
}

function normalizePivotRow(
  row: MesQmsMotherRollGoodStatisticsApi.ProcessPivotRow,
) {
  const motherBatchNo = motherBatchText(row);
  const segmentBatchNo = segmentText(row);
  return {
    ...row,
    motherRollBatchNo: motherBatchNo === '-' ? row.motherRollBatchNo : motherBatchNo,
    segmentBatchNo: segmentBatchNo === '-' ? row.segmentBatchNo : segmentBatchNo,
  };
}

function comparePivotRows(
  left: MesQmsMotherRollGoodStatisticsApi.ProcessPivotRow,
  right: MesQmsMotherRollGoodStatisticsApi.ProcessPivotRow,
) {
  return (
    motherGroupKey(left).localeCompare(motherGroupKey(right), 'zh-Hans-CN') ||
    segmentText(left).localeCompare(segmentText(right), 'zh-Hans-CN') ||
    String(left.pivotRowKey || '').localeCompare(String(right.pivotRowKey || ''), 'zh-Hans-CN')
  );
}

function motherGroupKey(row: MesQmsMotherRollGoodStatisticsApi.ProcessPivotRow) {
  return `${motherBatchText(row)}|${modelText(row)}`;
}

function planRowKey(row: MesQmsMotherRollGoodStatisticsApi.ProcessPivotRow) {
  return `MOTHER|${motherGroupKey(row)}`;
}

function segmentRowKey(row: MesQmsMotherRollGoodStatisticsApi.ProcessPivotRow) {
  return `MOTHER_SEGMENT|${motherGroupKey(row)}|${segmentText(row)}`;
}

function modelSizeRowKey(row: MesQmsMotherRollGoodStatisticsApi.ProcessPivotRow) {
  return `MOTHER_MODEL_SIZE|${motherGroupKey(row)}`;
}

function stageRowKey(
  row: MesQmsMotherRollGoodStatisticsApi.ProcessPivotRow,
  stageCode: string,
) {
  if (stageCode === 'FORMULA' || stageCode === 'WET') {
    return `MOTHER_STAGE|${motherGroupKey(row)}|${stageCode}`;
  }
  return `MOTHER_STAGE|${segmentRowKey(row)}|${stageCode}`;
}

function stageCellRowKey(
  row: MesQmsMotherRollGoodStatisticsApi.ProcessPivotRow,
  stageCode: string,
  columnKey: StageSubColumnKey,
) {
  if (columnKey === 'motherOutputQty') {
    return `MOTHER_OUTPUT|${motherGroupKey(row)}|${stageCode}`;
  }
  if (columnKey === 'inputQty' && PIECE_INPUT_STAGE_CODES.has(stageCode)) {
    return (
      stageOf(row, stageCode).inputQtyMergeKey ||
      `INPUT|${motherGroupKey(row)}|${stageCode}`
    );
  }
  if (columnKey === 'finalInspectionOutputQty') {
    return `FINAL_INSPECTION_OUTPUT|${motherGroupKey(row)}|${stageCode}`;
  }
  if (columnKey === 'finalInspectionYieldRate') {
    return (
      stageOf(row, stageCode).finalInspectionYieldRateMergeKey ||
      `FINAL_INSPECTION_YIELD|${motherGroupKey(row)}|${stageCode}`
    );
  }
  if (columnKey === 'theoreticalOutputQty') {
    return `THEORY|${motherGroupKey(row)}|${stageCode}|${modelText(row)}`;
  }
  if (columnKey === 'goodYieldRate') {
    return `GOOD_YIELD|${motherGroupKey(row)}|${stageCode}|${modelText(row)}`;
  }
  if (columnKey === 'goodTargetRate') {
    return (
      stageOf(row, stageCode).goodTargetRateMergeKey ||
      `GOOD_TARGET|${motherGroupKey(row)}|${stageCode}|${modelText(row)}`
    );
  }
  if (columnKey === 'defectQty' && PIECE_INPUT_STAGE_CODES.has(stageCode)) {
    return `PIECE_SELF_CHECK_NG|${motherGroupKey(row)}|${stageCode}`;
  }
  return stageRowKey(row, stageCode);
}

function showStageBatch(stage: StageColumn) {
  return stage.showBatch !== false;
}

function stageMetricColumns(stage: StageColumn) {
  return stage.metricColumns || [];
}

function isMeterReportStage(stageCode: string) {
  return stageCode === 'GRINDING' || stageCode === 'ADHESIVE1';
}

function stageDoneColumnLabel(stageCode: string) {
  if (stageCode === 'WET') return '收卷米数(报工数)';
  if (isMeterReportStage(stageCode)) return '产出米数';
  if (PIECE_INPUT_STAGE_CODES.has(stageCode)) return '分段自检OK';
  return '完工';
}

function stageMotherOutputColumnLabel(stageCode: string) {
  if (PIECE_INPUT_STAGE_CODES.has(stageCode)) return '总产出';
  return '母卷产出';
}

function stageInspectionColumnLabel(stageCode: string) {
  if (stageCode === 'WET') return 'NAP层送检(米)';
  if (isMeterReportStage(stageCode)) return 'NAP留样米数/送检送数';
  return '送检';
}

function stageInspectionNgColumnLabel(stageCode: string) {
  if (PIECE_INPUT_STAGE_CODES.has(stageCode)) return '送检NG';
  return '检验NG';
}

function stageDefectColumnLabel(stageCode: string) {
  if (stageCode === 'WET' || isMeterReportStage(stageCode)) {
    return '固定损耗（米）';
  }
  if (PIECE_SELF_CHECK_STAGE_CODES.has(stageCode)) {
    return '自检总NG';
  }
  return 'NG';
}

function stageLastReportTimeLabel(stageCode: string) {
  if (PIECE_SELF_CHECK_STAGE_CODES.has(stageCode)) return '扫码确认时间';
  if (stageCode === 'SHIPPING_INSPECTION') return '送检时间';
  return '完工时间';
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
        label: stageLastReportTimeLabel(stage.code),
        widthKey: 'stageTime',
      },
    );
    return columns;
  }
  if (isMeterReportStage(stage.code)) {
    columns.push({
      className: 'col-stage-qty',
      key: 'inputQty',
      label: '投入米数',
      widthKey: 'stageQty',
    });
  }
  if (PIECE_INPUT_STAGE_CODES.has(stage.code)) {
    columns.push(
      {
        className: 'col-stage-qty col-stage-qty-wide',
        key: 'doneQty',
        label: stageDoneColumnLabel(stage.code),
        widthKey: 'stageWideQty',
      },
      {
        className: 'col-stage-qty col-stage-qty-wide',
        key: 'segmentDefectQty',
        label: '分段自检NG',
        widthKey: 'stageWideQty',
      },
      {
        className: 'col-stage-qty',
        key: 'inspectionQty',
        label: stageInspectionColumnLabel(stage.code),
        widthKey: 'stageQty',
      },
      ...(['PRESS_SLOT', 'ADHESIVE2', 'CUT_ROUND'].includes(stage.code)
        ? [
            {
              className: 'col-stage-qty col-stage-qty-wide',
              key: 'processProductionInspectionQty' as const,
              label: '本工序损耗',
              widthKey: 'stageWideQty' as const,
            },
          ]
        : []),
      {
        className: 'col-stage-qty',
        key: 'inspectionNgQty',
        label: stageInspectionNgColumnLabel(stage.code),
        widthKey: 'stageQty',
      },
    );
    if (COA_INSPECTION_STAGE_CODES.has(stage.code)) {
      columns.push(
        {
          className: 'col-stage-qty',
          key: 'coaInspectionQty',
          label: 'COA送检',
          widthKey: 'stageQty',
        },
        {
          className: 'col-stage-qty',
          key: 'coaInspectionNgQty',
          label: 'COA NG',
          widthKey: 'stageQty',
        },
      );
    }
    if (stage.code === 'ADHESIVE2') {
      columns.push({
        className: 'col-stage-qty',
        key: 'glueBoardInspectionQty',
        label: '胶板送检(m)',
        widthKey: 'stageQty',
      });
    }
    columns.push(
      {
        className: 'col-stage-qty',
        key: 'pendingQty',
        label: '未加工',
        widthKey: 'stageQty',
      },
      {
        className: 'col-stage-qty',
        key: 'defectQty',
        label: stageDefectColumnLabel(stage.code),
        widthKey: 'stageQty',
      },
      {
        className: 'col-stage-output',
        key: 'segmentInputQty',
        label: '分段投入',
        widthKey: 'stageOutput',
      },
      {
        className: 'col-stage-output',
        key: 'inputQty',
        label: '总投入',
        widthKey: 'stageOutput',
      },
      {
        className: 'col-stage-output',
        key: 'motherOutputQty',
        label: stageMotherOutputColumnLabel(stage.code),
        widthKey: 'stageOutput',
      },
      {
        className: 'col-stage-target',
        key: 'theoreticalOutputQty',
        label: '理论产量',
        widthKey: 'stageTarget',
      },
      {
        className: 'col-stage-target',
        key: 'goodYieldRate',
        label: '良品率（%）',
        widthKey: 'stageTarget',
      },
    );
    if (stage.code === 'CUT_ROUND') {
      columns.push(
        {
          className: 'col-stage-output',
          key: 'finalInspectionOutputQty',
          label: '终检产出',
          widthKey: 'stageOutput',
        },
        {
          className: 'col-stage-target',
          key: 'finalInspectionYieldRate',
          label: '终检良率',
          widthKey: 'stageTarget',
        },
        {
          className: 'col-stage-target',
          key: 'goodTargetRate',
          label: '母卷良品达标率（%）',
          widthKey: 'stageTarget',
        },
      );
    }
    columns.push({
      className: 'col-stage-time',
      key: 'lastReportTime',
      label: stageLastReportTimeLabel(stage.code),
      widthKey: 'stageTime',
    });
    return columns;
  }
  const doneColumnWide = stage.code === 'WET';
  columns.push(
    {
      className: doneColumnWide
        ? 'col-stage-qty col-stage-qty-wide'
        : 'col-stage-qty',
      key: 'doneQty',
      label: stageDoneColumnLabel(stage.code),
      widthKey: doneColumnWide ? 'stageWideQty' : 'stageQty',
    },
  );
  if (MOTHER_OUTPUT_STAGE_CODES.has(stage.code)) {
    columns.push({
      className: 'col-stage-output',
      key: 'motherOutputQty',
      label: stageMotherOutputColumnLabel(stage.code),
      widthKey: 'stageOutput',
    });
  }
  if (THEORETICAL_OUTPUT_STAGE_CODES.has(stage.code)) {
    columns.push(
      {
        className: 'col-stage-target',
        key: 'theoreticalOutputQty',
        label: '理论产量',
        widthKey: 'stageTarget',
      },
      {
        className: 'col-stage-target',
        key: 'goodYieldRate',
        label: '良品率（%）',
        widthKey: 'stageTarget',
      },
    );
  }
  const inspectionColumnWide = stage.code === 'WET' || isMeterReportStage(stage.code);
  columns.push(
    {
      className: inspectionColumnWide
        ? 'col-stage-qty col-stage-qty-wide'
        : 'col-stage-qty',
      key: 'inspectionQty',
      label: stageInspectionColumnLabel(stage.code),
      widthKey: inspectionColumnWide ? 'stageWideQty' : 'stageQty',
    },
    {
      className: 'col-stage-qty',
      key: 'inspectionNgQty',
      label: stageInspectionNgColumnLabel(stage.code),
      widthKey: 'stageQty',
    },
  );
  if (stage.code !== 'FORMULA') {
    const defectColumnWide = stage.code === 'WET' || isMeterReportStage(stage.code);
    columns.push({
      className: defectColumnWide
        ? 'col-stage-qty col-stage-qty-wide'
        : 'col-stage-qty',
      key: 'defectQty',
      label: stageDefectColumnLabel(stage.code),
      widthKey: defectColumnWide ? 'stageWideQty' : 'stageQty',
    });
  }
  columns.push(
    {
      className: 'col-stage-time',
      key: 'lastReportTime',
      label: stageLastReportTimeLabel(stage.code),
      widthKey: 'stageTime',
    },
  );
  return columns;
}

function isDefaultStageSubColumnVisible(
  stage: StageColumn,
  column: StageSubColumn,
) {
  if (
    column.key === 'batch' &&
    stage.code === 'ADHESIVE1'
  ) {
    return false;
  }
  if (column.key === 'defectQty' || column.key === 'lastReportTime') {
    return false;
  }
  if (column.key === 'segmentInputQty' && PIECE_INPUT_STAGE_CODES.has(stage.code)) {
    return false;
  }
  if (
    (column.key === 'inspectionQty' || column.key === 'inspectionNgQty') &&
    !PIECE_INPUT_STAGE_CODES.has(stage.code)
  ) {
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

function stageToneClass(stage: StageColumn) {
  const stageIndex = visibleStageColumns.value.findIndex(
    (item) => item.code === stage.code,
  );
  return stageIndex % 2 === 0 ? 'stage-tone-a' : 'stage-tone-b';
}

function stageCellClass(column: StageSubColumn) {
  if (column.key === 'batch' || column.key === 'lastReportTime') {
    return 'text-cell';
  }
  if (column.key === 'pendingQty') {
    return 'num pending-cell';
  }
  if (
    column.key === 'defectQty' ||
    column.key === 'segmentDefectQty' ||
    column.key === 'processProductionInspectionQty'
  ) {
    return 'num defect-cell';
  }
  if (column.key === 'inspectionNgQty' || column.key === 'coaInspectionNgQty') {
    return 'num inspection-ng-cell';
  }
  if (
    column.key === 'glueBoardInspectionQty' ||
    column.key === 'coaInspectionQty'
  ) {
    return 'num inspection-cell';
  }
  if (column.key === 'inputQty' || column.key === 'segmentInputQty') {
    return 'num input-cell';
  }
  if (column.key === 'motherOutputQty') {
    return 'num mother-output-cell';
  }
  if (column.key === 'finalInspectionOutputQty') {
    return 'num final-inspection-output-cell';
  }
  if (column.key === 'theoreticalOutputQty') {
    return 'num theoretical-output-cell';
  }
  if (
    column.key === 'goodYieldRate' ||
    column.key === 'finalInspectionYieldRate' ||
    column.key === 'goodTargetRate'
  ) {
    return 'num good-yield-rate-cell';
  }
  return 'num';
}

function canOpenPieceList(stage: StageColumn) {
  return stage.code !== 'SHIPPING_INSPECTION';
}

function isFirstPlanRow(row: MesQmsMotherRollGoodStatisticsApi.ProcessPivotRow, index: number) {
  return planRowSpanMap.value.get(planRowKey(row))?.firstIndex === index;
}

function planRowSpan(row: MesQmsMotherRollGoodStatisticsApi.ProcessPivotRow) {
  return planRowSpanMap.value.get(planRowKey(row))?.rowSpan || 1;
}

function isFirstSegmentRow(
  row: MesQmsMotherRollGoodStatisticsApi.ProcessPivotRow,
  index: number,
) {
  return segmentRowSpanMap.value.get(segmentRowKey(row))?.firstIndex === index;
}

function segmentRowSpan(row: MesQmsMotherRollGoodStatisticsApi.ProcessPivotRow) {
  return segmentRowSpanMap.value.get(segmentRowKey(row))?.rowSpan || 1;
}

function isFirstModelSizeRow(
  row: MesQmsMotherRollGoodStatisticsApi.ProcessPivotRow,
  index: number,
) {
  return (
    modelSizeRowSpanMap.value.get(modelSizeRowKey(row))?.firstIndex === index
  );
}

function modelSizeRowSpan(row: MesQmsMotherRollGoodStatisticsApi.ProcessPivotRow) {
  return modelSizeRowSpanMap.value.get(modelSizeRowKey(row))?.rowSpan || 1;
}

function isFirstStageCellRow(
  row: MesQmsMotherRollGoodStatisticsApi.ProcessPivotRow,
  index: number,
  stageCode: string,
  columnKey: StageSubColumnKey,
) {
  const key = `${stageCode}|${columnKey}|${stageCellRowKey(row, stageCode, columnKey)}`;
  return stageCellRowSpanMap.value.get(key)?.firstIndex === index;
}

function stageCellRowSpan(
  row: MesQmsMotherRollGoodStatisticsApi.ProcessPivotRow,
  stageCode: string,
  columnKey: StageSubColumnKey,
) {
  const key = `${stageCode}|${columnKey}|${stageCellRowKey(row, stageCode, columnKey)}`;
  return stageCellRowSpanMap.value.get(key)?.rowSpan || 1;
}

function isMergedPieceDefectCell(stageCode: string, columnKey?: StageSubColumnKey) {
  return columnKey === 'defectQty' && PIECE_INPUT_STAGE_CODES.has(stageCode);
}

function stageForCell(
  row: MesQmsMotherRollGoodStatisticsApi.ProcessPivotRow,
  stageCode: string,
  columnKey?: StageSubColumnKey,
) {
  if (!isMergedPieceDefectCell(stageCode, columnKey)) {
    return stageOf(row, stageCode);
  }
  return mergePieceStagesForCell(row, stageCode, columnKey);
}

function mergePieceStagesForCell(
  row: MesQmsMotherRollGoodStatisticsApi.ProcessPivotRow,
  stageCode: string,
  columnKey: StageSubColumnKey,
) {
  const mergeKey = stageCellRowKey(row, stageCode, columnKey);
  const relatedStages = rows.value
    .filter((item) => stageCellRowKey(item, stageCode, columnKey) === mergeKey)
    .map((item) => stageOf(item, stageCode));
  const base = { ...stageOf(row, stageCode) };
  const pieceMap = new Map<string, MesQmsMotherRollGoodStatisticsApi.ProcessPivotPiece>();
  const inspectionMap = new Map<string, MesQmsMotherRollGoodStatisticsApi.ProcessPivotInspection>();
  relatedStages.forEach((stage) => {
    (stage.pieceDetails || []).forEach((item) => {
      const key = normalizeDrilldownKey(
        firstText(item.pieceNo, item.outputBatchNo, item.sourceBatchNo),
      );
      if (!key) return;
      const current = pieceMap.get(key);
      if (!current) {
        pieceMap.set(key, { ...item });
        return;
      }
      current.sourceBatchNo = firstText(current.sourceBatchNo, item.sourceBatchNo);
      current.outputBatchNo = firstText(current.outputBatchNo, item.outputBatchNo, item.pieceNo);
      current.reportConfirmed = Boolean(current.reportConfirmed || item.reportConfirmed);
      current.defectFlag = Boolean(current.defectFlag || item.defectFlag);
      current.coaFlag = Boolean(current.coaFlag || item.coaFlag);
      current.lastReportTime = latestDateTimeText(current.lastReportTime, item.lastReportTime);
      current.remark = firstText(current.remark, item.remark);
      current.status = current.defectFlag ? 'DEFECT' : current.reportConfirmed ? 'DONE' : 'PENDING';
    });
    (stage.inspectionDetails || []).forEach((item) => {
      const key = normalizeDrilldownKey(
        firstText(item.inspectionId, item.inspectionNo, item.productBatchNo),
      );
      if (key && !inspectionMap.has(key)) {
        inspectionMap.set(key, { ...item });
      }
    });
  });
  const pieceDetails = [...pieceMap.values()];
  const defectQty = pieceDetails.filter((item) => item.defectFlag).length;
  const doneQty = pieceDetails.filter((item) => item.reportConfirmed && !item.defectFlag).length;
  base.pieceDetails = pieceDetails;
  base.inspectionDetails = [...inspectionMap.values()];
  base.defectQty = defectQty;
  base.doneQty = doneQty;
  base.inputQty = doneQty + defectQty;
  base.confirmedQty = pieceDetails.filter((item) => item.reportConfirmed).length;
  base.reportQty = pieceDetails.length;
  base.reportUnit = base.reportUnit || '片';
  return base;
}

function latestDateTimeText(left?: string, right?: string) {
  if (!left) return right;
  if (!right) return left;
  return right > left ? right : left;
}

function formatDateTime(value?: string) {
  if (!value) return '-';
  return String(value).replace('T', ' ').slice(0, 19);
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

function numberOrZero(value?: null | number | string) {
  const number = Number(value || 0);
  return Number.isFinite(number) ? number : 0;
}

function formatQty(value?: null | number | string, unit?: string, digits = 3) {
  const text = formatNumber(value, digits);
  if (text === '-') return '-';
  return unit ? `${text}${unit}` : text;
}

function formatPercent(value?: null | number | string, digits = 2) {
  const text = formatNumber(value, digits);
  return text === '-' ? '-' : `${text}%`;
}

function compactText(value?: string, maxLength = 18) {
  if (!value) return '-';
  return value.length > maxLength ? `${value.slice(0, maxLength)}...` : value;
}

function motherBatchText(row: MesQmsMotherRollGoodStatisticsApi.ProcessPivotRow) {
  return normalizeMotherBatchText(
    row.motherRollBatchNo ||
    row.parentProductionBatchNo ||
    row.productionBatchNo ||
    row.batchNo,
  );
}

function normalizeMotherBatchText(value?: string) {
  const text = String(value || '').trim();
  if (!text) return '-';
  const values: string[] = [];
  text
    .split(/[,\s，；;]+/)
    .map((item) => normalizeMotherBatchNo(item))
    .filter(Boolean)
    .forEach((item) => {
      if (!values.includes(item)) values.push(item);
    });
  return values.length > 0 ? values.join('，') : '-';
}

function normalizeMotherBatchNo(value?: string) {
  const text = normalizeBatchCode(value);
  if (!text) return '';
  if (isMotherSegmentBatchNo(text)) {
    return text.slice(0, -1);
  }
  return text;
}

function normalizeBatchCode(value?: string) {
  return String(value || '').trim().toUpperCase();
}

function isMotherSegmentBatchNo(value?: string) {
  const text = normalizeBatchCode(value);
  if (text.length <= 1) return false;
  const lastChar = text.slice(-1);
  const previousChar = text.slice(-2, -1);
  return ['P', 'Q', 'R', 'S'].includes(lastChar) && previousChar === 'A';
}

function batchText(stage: MesQmsMotherRollGoodStatisticsApi.ProcessPivotStage) {
  return stage.outputBatchNos || stage.sourceBatchNos || '-';
}

function formatStageBatchText(
  stage: MesQmsMotherRollGoodStatisticsApi.ProcessPivotStage,
  stageCode: string,
) {
  const text = batchText(stage);
  if (stageCode !== 'ADHESIVE1' || text === '-') return text;
  return (
    text
      .replace(
        /\s*\d+(?:\.\d+)?\s*(?:-|~|至|到)\s*\d+(?:\.\d+)?\s*(?:m|米)/gi,
        '',
      )
      .replace(/\s{2,}/g, ' ')
      .replace(/\s*([,，；;])\s*/g, '$1 ')
      .trim() || text
  );
}

function compactBatchText(
  stage: MesQmsMotherRollGoodStatisticsApi.ProcessPivotStage,
  stageCode: string,
) {
  return compactText(formatStageBatchText(stage, stageCode), 24);
}

function stageDoneText(stage: MesQmsMotherRollGoodStatisticsApi.ProcessPivotStage) {
  return formatQty(stage.doneQty, stage.reportUnit);
}

function stageInputText(stage: MesQmsMotherRollGoodStatisticsApi.ProcessPivotStage) {
  return formatQty(stage.inputQty, stage.reportUnit);
}

function stageSegmentInputText(stage: MesQmsMotherRollGoodStatisticsApi.ProcessPivotStage) {
  return formatQty(stage.segmentInputQty, stage.reportUnit);
}

function stageMotherOutputText(stage: MesQmsMotherRollGoodStatisticsApi.ProcessPivotStage) {
  return formatQty(stage.motherOutputQty, stage.motherOutputUnit || stage.reportUnit);
}

function stageFinalInspectionOutputText(stage: MesQmsMotherRollGoodStatisticsApi.ProcessPivotStage) {
  return formatQty(stage.finalInspectionOutputQty, stage.finalInspectionOutputUnit || '片');
}

function stageFinalInspectionYieldRateText(stage: MesQmsMotherRollGoodStatisticsApi.ProcessPivotStage) {
  return formatPercent(stage.finalInspectionYieldRate);
}

function stageTheoreticalOutputText(stage: MesQmsMotherRollGoodStatisticsApi.ProcessPivotStage) {
  if (!stage.theoreticalOutputMatched) {
    return '-';
  }
  return formatQty(stage.theoreticalOutputQty, theoreticalOutputUnit(stage));
}

function stageGoodYieldRateText(stage: MesQmsMotherRollGoodStatisticsApi.ProcessPivotStage) {
  return formatPercent(stage.goodYieldRate);
}

function stageGoodTargetRateText(stage: MesQmsMotherRollGoodStatisticsApi.ProcessPivotStage) {
  return formatPercent(stage.goodTargetRate);
}

function theoreticalOutputUnit(stage: MesQmsMotherRollGoodStatisticsApi.ProcessPivotStage) {
  if (PENDING_STAGE_CODES.has(String(stage.stageCode || ''))) {
    return '片';
  }
  return stage.theoreticalOutputUnit || stage.reportUnit;
}

function stagePendingText(stage: MesQmsMotherRollGoodStatisticsApi.ProcessPivotStage) {
  return formatQty(stage.pendingQty, stage.pendingUnit || stage.reportUnit);
}

function stageDefectText(stage: MesQmsMotherRollGoodStatisticsApi.ProcessPivotStage) {
  return formatQty(stage.defectQty, stage.reportUnit);
}

function stageSegmentDefectText(stage: MesQmsMotherRollGoodStatisticsApi.ProcessPivotStage) {
  return formatQty(stage.segmentDefectQty, stage.reportUnit);
}

function stageInspectionText(stage: MesQmsMotherRollGoodStatisticsApi.ProcessPivotStage) {
  return formatQty(stage.inspectionQty, stage.reportUnit);
}

function stageProcessLossText(
  stage: MesQmsMotherRollGoodStatisticsApi.ProcessPivotStage,
) {
  return formatQty(stage.processProductionInspectionQty, stage.reportUnit);
}

function stageCoaInspectionText(stage: MesQmsMotherRollGoodStatisticsApi.ProcessPivotStage) {
  return formatQty(stage.coaInspectionQty, stage.reportUnit);
}

function stageGlueBoardInspectionText(stage: MesQmsMotherRollGoodStatisticsApi.ProcessPivotStage) {
  return formatQty(stage.glueBoardInspectionQty, 'm');
}

function stageInspectionNgText(stage: MesQmsMotherRollGoodStatisticsApi.ProcessPivotStage) {
  return formatQty(stage.inspectionNgQty, stage.reportUnit);
}

function stageCoaInspectionNgText(stage: MesQmsMotherRollGoodStatisticsApi.ProcessPivotStage) {
  return formatQty(stage.coaInspectionNgQty, stage.reportUnit);
}

function grindingOutputQty(stage: MesQmsMotherRollGoodStatisticsApi.ProcessPivotStage) {
  return Math.max(
    numberOrZero(stage.inputQty)
      - numberOrZero(stage.defectQty)
      - numberOrZero(stage.inspectionQty),
    0,
  );
}

function stageMetricText(
  stage: MesQmsMotherRollGoodStatisticsApi.ProcessPivotStage,
  field: StageMetricColumn['field'],
) {
  return formatNumber(stage[field], 3);
}

function stageMetricColumnText(
  stage: MesQmsMotherRollGoodStatisticsApi.ProcessPivotStage,
  column: StageSubColumn,
) {
  return column.metricField ? stageMetricText(stage, column.metricField) : '-';
}

function stageStatusClass(status?: string) {
  const normalized = String(status || 'NOT_STARTED').toUpperCase();
  return `stage-status--${normalized.toLowerCase().replaceAll('_', '-')}`;
}

function modelText(row: MesQmsMotherRollGoodStatisticsApi.ProcessPivotRow) {
  return (
    row.modelSeriesCode ||
    modelCodePrefix(row.actualModelCode || row.modelCode || row.modelName) ||
    '-'
  );
}

function modelCodePrefix(value?: string) {
  const text = String(value || '').trim().toUpperCase();
  if (!text) return '';
  return text.length <= 3 ? text : text.slice(0, 3);
}

function sizeText(row: MesQmsMotherRollGoodStatisticsApi.ProcessPivotRow) {
  return row.actualSizeSpec || row.sizeName || row.sizeSpec || '-';
}

function segmentText(row: MesQmsMotherRollGoodStatisticsApi.ProcessPivotRow) {
  if (row.segmentBatchNo) return normalizeBatchCode(row.segmentBatchNo);
  if (isMotherSegmentBatchNo(row.motherRollBatchNo)) {
    return normalizeBatchCode(row.motherRollBatchNo);
  }
  return '-';
}

function textOf(value: unknown) {
  if (value === null || value === undefined) {
    return '';
  }
  return String(value).trim();
}

function pieceRemarkText(item: DrilldownTableRow) {
  return summarizeRemarkText(textOf(item.remark));
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
  if (/^\s*(reportType|itemName|result|remark)\s*[:：]/i.test(text)) {
    return '';
  }
  return text.length > 80 ? `${text.slice(0, 80)}...` : text;
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
    if (char === '"' || char === "'") {
      quote = char;
    } else if (char === '{' || char === '[') {
      depth += 1;
    } else if (char === '}' || char === ']') {
      depth -= 1;
      if (depth === 0) {
        blocks.push(text.slice(start, index + 1));
        start = -1;
      }
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
    let match: RegExpExecArray | null;
    while ((match = pattern.exec(text))) {
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

function createDefectCountMap() {
  return DEFECT_CATEGORY_LABELS.reduce(
    (target, item) => {
      target[item.key] = 0;
      return target;
    },
    {} as Record<string, number>,
  );
}

function addDefectCount(
  target: Record<string, number>,
  value?: string,
  amount = 1,
) {
  const key = defectCategoryKey(value);
  const nextAmount = Number.isFinite(amount) && amount > 0 ? amount : 1;
  target[key] = (target[key] || 0) + nextAmount;
}

function defectCategoryKey(value?: string) {
  const text = String(value || '').trim();
  const matched = DEFECT_CATEGORY_LABELS.find((item) =>
    text.includes(item.label),
  );
  return matched?.key || 'defectOther';
}

function firstPositiveNumber(...values: unknown[]) {
  for (const value of values) {
    if (value === null || value === undefined || value === '') continue;
    const number = Number(value);
    if (Number.isFinite(number) && number > 0) {
      return number;
    }
  }
  return undefined;
}

function collectNgDefectCounts(value: unknown, target: Record<string, number>) {
  if (Array.isArray(value)) {
    value.forEach((item) => collectNgDefectCounts(item, target));
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
  if (abnormal && (name || remark)) {
    addDefectCount(
      target,
      name || remark,
      firstPositiveNumber(
        record.qty,
        record.quantity,
        record.count,
        record.defectQty,
        record.ngQty,
        record.value,
      ) || 1,
    );
  }
  Object.values(record).forEach((item) => collectNgDefectCounts(item, target));
}

function collectNgDefectCountsFromText(
  text: string,
  target: Record<string, number>,
) {
  const jsonLike = /"(?:visualItems|itemName|result|visualInspectionResult|inspectionResult)"/i.test(text);
  if (!jsonLike) {
    DEFECT_CATEGORY_LABELS.forEach((item) => {
      const pattern = new RegExp(
        `${escapeRegExp(item.label)}\\s*(?:[:：=xX×*]?\\s*(\\d+(?:\\.\\d+)?))?`,
        'gu',
      );
      let match: RegExpExecArray | null;
      while ((match = pattern.exec(text))) {
        const amountText = match[1];
        if (amountText !== undefined && Number(amountText) <= 0) {
          continue;
        }
        addDefectCount(target, item.label, amountText ? Number(amountText) : 1);
      }
    });
  }

  const patterns = jsonLike
    ? [
        /"itemName"\s*:\s*"([^"]+)"[\s\S]{0,220}?"result"\s*:\s*"([^"]+)"/gi,
        /"defectName"\s*:\s*"([^"]+)"/gi,
        /"defectCode"\s*:\s*"([^"]+)"/gi,
      ]
    : [
        /"itemName"\s*:\s*"([^"]+)"[\s\S]{0,220}?"result"\s*:\s*"([^"]+)"/gi,
        /"result"\s*:\s*"([^"]+)"[\s\S]{0,220}?"itemName"\s*:\s*"([^"]+)"/gi,
        /"defectName"\s*:\s*"([^"]+)"/gi,
        /"defectCode"\s*:\s*"([^"]+)"/gi,
      ];
  const matchedTextDefects = new Set<string>();
  patterns.forEach((pattern, patternIndex) => {
    let match: RegExpExecArray | null;
    while ((match = pattern.exec(text))) {
      if (patternIndex === 0 && isNgText(match[2])) {
        const defectName = match[1]!;
        if (!matchedTextDefects.has(defectName)) {
          matchedTextDefects.add(defectName);
          addDefectCount(target, defectName);
        }
      } else if (!jsonLike && patternIndex === 1 && isNgText(match[1])) {
        const defectName = match[2]!;
        if (!matchedTextDefects.has(defectName)) {
          matchedTextDefects.add(defectName);
          addDefectCount(target, defectName);
        }
      } else if ((jsonLike && patternIndex >= 1) || (!jsonLike && patternIndex >= 2)) {
        const defectName = match[1]!;
        if (!matchedTextDefects.has(defectName)) {
          matchedTextDefects.add(defectName);
          addDefectCount(target, defectName);
        }
      }
    }
  });
}

function collectDefectCountsFromRemark(
  value: unknown,
  target: Record<string, number>,
) {
  const text = textOf(value);
  if (!text) {
    return;
  }
  const jsonBlocks = extractJsonBlocks(text);
  jsonBlocks.forEach((block) => {
    const parsed = parseJsonBlock(block);
    if (parsed !== null) {
      collectNgDefectCounts(parsed, target);
    }
  });
  let plainText = text;
  jsonBlocks.forEach((block) => {
    plainText = plainText.replace(block, '');
  });
  if (plainText.trim()) {
    collectNgDefectCountsFromText(plainText, target);
  } else if (jsonBlocks.length === 0) {
    collectNgDefectCountsFromText(text, target);
  }
}

function defectCountCells(
  values: unknown[],
  fallbackToOther: boolean,
): Record<string, number | string> {
  const counts = createDefectCountMap();
  values.forEach((value) => collectDefectCountsFromRemark(value, counts));
  if (
    fallbackToOther &&
    !Object.values(counts).some((value) => value > 0)
  ) {
    addDefectCount(counts, '其他');
  }
  return DEFECT_CATEGORY_LABELS.reduce(
    (target, item) => {
      const value = counts[item.key] || 0;
      target[item.key] = value > 0 ? value : '-';
      return target;
    },
    {} as Record<string, number | string>,
  );
}

function escapeRegExp(value: string) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
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

function isNgText(value?: string) {
  const text = String(value || '').trim().toUpperCase();
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

function inspectionIsNg(item: MesQmsMotherRollGoodStatisticsApi.ProcessPivotInspection) {
  const judgment = String(item.judgment || '').toUpperCase();
  const status = String(item.status || '').toUpperCase();
  return (
    Number(item.inspectionNgQty || 0) > 0 ||
    judgment === 'NG' ||
    status === 'REJECTED' ||
    status === 'FAILED'
  );
}

function isGlueBoardInspection(
  stage: MesQmsMotherRollGoodStatisticsApi.ProcessPivotStage,
  item: MesQmsMotherRollGoodStatisticsApi.ProcessPivotInspection,
) {
  return (
    String(stage.stageCode || '').toUpperCase() === 'ADHESIVE2' &&
    String(item.sourceType || '').toUpperCase() === 'GLUE_BOARD_FAI'
  );
}

function isCoaInspectionDetail(
  stage: MesQmsMotherRollGoodStatisticsApi.ProcessPivotStage,
  item: MesQmsMotherRollGoodStatisticsApi.ProcessPivotInspection,
) {
  return (
    COA_INSPECTION_STAGE_CODES.has(String(stage.stageCode || '').toUpperCase()) &&
    Boolean(item.coaInspectionFlag)
  );
}

function regularInspectionDetails(
  stage: MesQmsMotherRollGoodStatisticsApi.ProcessPivotStage,
) {
  return (stage.inspectionDetails || []).filter(
    (item) => !isGlueBoardInspection(stage, item),
  );
}

function coaInspectionDetails(
  stage: MesQmsMotherRollGoodStatisticsApi.ProcessPivotStage,
) {
  return regularInspectionDetails(stage).filter((item) =>
    isCoaInspectionDetail(stage, item),
  );
}

function canOpenInspectionList(
  stage: MesQmsMotherRollGoodStatisticsApi.ProcessPivotStage,
  ngOnly = false,
) {
  if (isProcessDrilldownStage(stage.stageCode)) {
    return true;
  }
  const details = regularInspectionDetails(stage);
  return ngOnly ? details.some(inspectionIsNg) : details.length > 0;
}

function canOpenCoaInspectionList(
  stage: MesQmsMotherRollGoodStatisticsApi.ProcessPivotStage,
  ngOnly = false,
) {
  const details = coaInspectionDetails(stage);
  return ngOnly ? details.some(inspectionIsNg) : details.length > 0;
}

function isInspectionDialogKind(kind: PieceDialogKind) {
  return [
    'COA_INSPECTION',
    'COA_INSPECTION_NG',
    'INSPECTION',
    'INSPECTION_NG',
  ].includes(kind);
}

function inspectionStatusText(item: MesQmsMotherRollGoodStatisticsApi.ProcessPivotInspection) {
  if (inspectionIsNg(item)) return '检验NG';
  const judgment = String(item.judgment || '').toUpperCase();
  if (judgment === 'OK') return '检验OK';
  return inspectionCodeLabel(item.status)
    || inspectionCodeLabel(item.judgment)
    || '已送检';
}

function inspectionCodeLabel(value?: unknown) {
  const normalized = String(value || '').trim().toUpperCase();
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
  if (kind === 'PROCESS_LOSS') return '本工序损耗';
  if (kind === 'COA_INSPECTION') return 'COA送检';
  if (kind === 'COA_INSPECTION_NG') return 'COA NG';
  if (kind === 'DEFECT') return 'NG';
  if (kind === 'INSPECTION') return '送检';
  if (kind === 'INSPECTION_NG') return '检验NG';
  return kind === 'DONE' ? '完工' : '待加工';
}

function drilldownKindLabel(
  stageCode: string,
  kind: PieceDialogKind,
  columnKey?: StageSubColumnKey,
) {
  if (kind === 'DEFECT') {
    if (stageCode === 'WET' || isMeterReportStage(stageCode)) {
      return '固定损耗';
    }
    if (PIECE_SELF_CHECK_STAGE_CODES.has(stageCode)) {
      return columnKey === 'defectQty' ? '自检总NG' : '分段自检NG';
    }
    return 'NG';
  }
  if (kind === 'DONE') {
    if (stageCode === 'WET') return '收卷米数';
    if (isMeterReportStage(stageCode)) return '产出米数';
    if (PIECE_SELF_CHECK_STAGE_CODES.has(stageCode)) return '分段自检OK';
  }
  if (kind === 'COA_INSPECTION') return 'COA送检';
  if (kind === 'COA_INSPECTION_NG') return 'COA NG';
  if (kind === 'INSPECTION_NG' && PIECE_SELF_CHECK_STAGE_CODES.has(stageCode)) {
    return '送检NG';
  }
  return pieceKindLabel(kind);
}

function pieceKindQtyText(
  stage: MesQmsMotherRollGoodStatisticsApi.ProcessPivotStage,
  kind: PieceDialogKind,
  columnKey?: StageSubColumnKey,
) {
  if (kind === 'DEFECT') {
    return columnKey === 'segmentDefectQty'
      ? stageSegmentDefectText(stage)
      : stageDefectText(stage);
  }
  if (kind === 'PROCESS_LOSS') return stageProcessLossText(stage);
  if (kind === 'INSPECTION') return stageInspectionText(stage);
  if (kind === 'INSPECTION_NG') return stageInspectionNgText(stage);
  if (kind === 'COA_INSPECTION') return stageCoaInspectionText(stage);
  if (kind === 'COA_INSPECTION_NG') return stageCoaInspectionNgText(stage);
  if (kind === 'DONE' && stage.stageCode === 'GRINDING') {
    return formatQty(grindingOutputQty(stage), 'm');
  }
  return kind === 'DONE' ? stageDoneText(stage) : stagePendingText(stage);
}

function isProcessDrilldownStage(stageCode?: string) {
  return ['FORMULA', 'WET', 'GRINDING', 'ADHESIVE1'].includes(
    String(stageCode || '').toUpperCase(),
  );
}

function drilldownColumns(stageCode: string, kind: PieceDialogKind) {
  if (kind === 'PROCESS_LOSS') {
    return [
      { key: 'index', label: '序号', width: 56 },
      { key: 'pieceNo', label: '片号', minWidth: 188 },
      { key: 'outputBatchNo', label: '产出批号', minWidth: 188 },
      { key: 'lossSource', label: '损耗来源', minWidth: 140 },
      { key: 'inspectionNo', label: '检验单号', minWidth: 180 },
      { key: 'inspectionResult', label: '检验结果', minWidth: 120 },
      { key: 'remark', label: '异常说明', minWidth: 200 },
    ] satisfies DrilldownTableColumn[];
  }
  if (stageCode === 'FORMULA') {
    return FORMULA_DRILLDOWN_COLUMNS;
  }
  if (stageCode === 'WET') {
    return WET_DRILLDOWN_COLUMNS;
  }
  if (stageCode === 'GRINDING' || stageCode === 'ADHESIVE1') {
    return METER_REPORT_DRILLDOWN_COLUMNS;
  }
  if (isInspectionDialogKind(kind)) {
    return INSPECTION_DRILLDOWN_COLUMNS;
  }
  if (PIECE_SELF_CHECK_STAGE_CODES.has(stageCode) && kind === 'DEFECT') {
    return PIECE_NG_DRILLDOWN_COLUMNS;
  }
  return PIECE_DRILLDOWN_COLUMNS;
}

function buildProcessDrilldownRows(
  stage: MesQmsMotherRollGoodStatisticsApi.ProcessPivotStage,
) {
  if (stage.stageCode === 'FORMULA') {
    return [
      {
        doneQtyText: stageDoneText(stage),
      },
    ];
  }
  if (stage.stageCode === 'WET') {
    return [
      {
        fixedLossQtyText: formatQty(stage.defectQty, 'm'),
        napInspectionQtyText: formatQty(stage.inspectionQty, 'm'),
        wetReportQtyText: formatQty(stage.reportQty || stage.doneQty, 'm'),
      },
    ];
  }
  return [
    {
      fixedLossQtyText: formatQty(stage.defectQty, 'm'),
      inputQtyText: formatQty(stage.inputQty, 'm'),
      napSampleQtyText: formatQty(stage.inspectionQty, 'm'),
      outputQtyText: formatQty(
        stage.stageCode === 'GRINDING'
          ? grindingOutputQty(stage)
          : stage.doneQty,
        'm',
      ),
    },
  ];
}

function normalizeDrilldownKey(value?: string) {
  return String(value || '').trim().toUpperCase();
}

function pieceInspectionMatches(
  inspection: MesQmsMotherRollGoodStatisticsApi.ProcessPivotInspection,
  pieceKeys: Set<string>,
) {
  const productBatchNo = normalizeDrilldownKey(inspection.productBatchNo);
  const inspectionNo = normalizeDrilldownKey(inspection.inspectionNo);
  return (
    (!!productBatchNo && pieceKeys.has(productBatchNo)) ||
    (!!inspectionNo && pieceKeys.has(inspectionNo))
  );
}

function matchedPieceInspections(
  stage: MesQmsMotherRollGoodStatisticsApi.ProcessPivotStage,
  item: MesQmsMotherRollGoodStatisticsApi.ProcessPivotPiece,
) {
  const pieceKeys = new Set(
    [item.pieceNo, item.outputBatchNo, item.sourceBatchNo]
      .map((value) => normalizeDrilldownKey(value))
      .filter(Boolean),
  );
  if (pieceKeys.size === 0) {
    return [];
  }
  return regularInspectionDetails(stage).filter((inspection) =>
    pieceInspectionMatches(inspection, pieceKeys),
  );
}

function inspectionResultText(
  inspections: MesQmsMotherRollGoodStatisticsApi.ProcessPivotInspection[],
) {
  const values = inspections.map(inspectionStatusText).filter(Boolean);
  return values.length > 0 ? [...new Set(values)].join('、') : '-';
}

function inspectionDefectText(
  inspections: MesQmsMotherRollGoodStatisticsApi.ProcessPivotInspection[],
) {
  const values = inspections
    .map((item) => firstText(item.defectSummary, summarizeRemarkText(item.remark)))
    .filter(Boolean);
  return values.length > 0 ? [...new Set(values)].join('、') : '';
}

function firstInspectionFlagText(
  inspections: MesQmsMotherRollGoodStatisticsApi.ProcessPivotInspection[],
) {
  return inspections.some((item) => {
    const type = `${item.sourceType || ''} ${item.inspectionType || ''}`.toUpperCase();
    return type.includes('FAI') || type.includes('首');
  })
    ? '是'
    : '否';
}

function confirmStatusText(
  item: MesQmsMotherRollGoodStatisticsApi.ProcessPivotPiece,
  kind: PieceDialogKind,
) {
  if (Boolean.TRUE === item.defectFlag || kind === 'DEFECT') {
    return 'NG';
  }
  if (Boolean.TRUE === item.reportConfirmed || kind === 'DONE') {
    return 'OK';
  }
  return '未确认';
}

function drilldownPieceNoText(
  stageCode: string,
  item: MesQmsMotherRollGoodStatisticsApi.ProcessPivotPiece,
  kind: PieceDialogKind,
) {
  if (
    OUTPUT_BATCH_PREFERRED_STAGE_CODES.has(normalizeDrilldownKey(stageCode)) &&
    kind !== 'PENDING'
  ) {
    return item.outputBatchNo || item.pieceNo || '-';
  }
  return item.pieceNo || item.outputBatchNo || '-';
}

function buildPieceDrilldownRows(
  row: MesQmsMotherRollGoodStatisticsApi.ProcessPivotRow,
  stage: MesQmsMotherRollGoodStatisticsApi.ProcessPivotStage,
  kind: PieceDialogKind,
) {
  const detailItems = (stage.pieceDetails || [])
    .filter((item) => String(item.status || '').toUpperCase() === kind)
    .map((item) => {
      const inspections = matchedPieceInspections(stage, item);
      const inspectionNgType = inspectionDefectText(inspections);
      const ngType = firstText(
        summarizeRemarkText(item.remark),
        inspectionNgType,
      );
      return {
        actualModelCodeText: firstText(
          item.actualModelCode,
          row.actualModelCode,
          row.modelCode,
          '-',
        ),
        actualSizeSpecText: firstText(
          item.actualSizeSpec,
          row.actualSizeSpec,
          row.sizeName,
          row.sizeSpec,
          '-',
        ),
        outputActualSizeSpecText: firstText(
          item.outputActualSizeSpec,
          item.actualSizeSpec,
          row.actualSizeSpec,
          row.sizeName,
          row.sizeSpec,
          '-',
        ),
        coaFlagText: item.coaFlag ? '是' : '否',
        confirmStatus: confirmStatusText(item, kind),
        defectSummary: ngType,
        ...(
          kind === 'DEFECT'
            ? defectCountCells([item.remark || inspectionNgType], true)
            : {}
        ),
        firstInspectionFlagText: firstInspectionFlagText(inspections),
        inspectionFlagText: inspections.length > 0 ? '是' : '否',
        inspectionResult: inspectionResultText(inspections),
        ngType: ngType || (kind === 'DEFECT' ? '自检NG' : '-'),
        outputBatchNo: item.outputBatchNo,
        pieceNo: drilldownPieceNoText(stage.stageCode, item, kind),
        remark: item.remark,
        scanConfirmTime: formatDateTime(item.lastReportTime),
        sourceBatchNo: item.sourceBatchNo,
        tracePieceNo: item.pieceNo,
      };
    });
  return detailItems;
}

function buildInspectionDrilldownRows(
  stage: MesQmsMotherRollGoodStatisticsApi.ProcessPivotStage,
  ngOnly: boolean,
  coaOnly = false,
) {
  const details = coaOnly ? coaInspectionDetails(stage) : regularInspectionDetails(stage);
  return details
    .filter((item) => !ngOnly || inspectionIsNg(item))
    .map((item) => ({
      actionText: '检验详情',
      defectSummary: item.defectSummary,
      inspectionId: item.inspectionId,
      inspectionNgQtyText: formatQty(item.inspectionNgQty, stage.reportUnit),
      inspectionNo: item.inspectionNo,
      inspectionQtyText: formatQty(item.inspectionQty, stage.reportUnit),
      inspectionResult: inspectionStatusText(item),
      inspectionType: item.inspectionType || '-',
      ngType: firstText(item.defectSummary, summarizeRemarkText(item.remark), '-'),
      pieceNo: item.productBatchNo || item.inspectionNo || '-',
      remark: item.remark,
      scanConfirmTime: formatDateTime(item.inspectionTime),
      sourceType: item.sourceType,
    }));
}

function buildDrilldownRows(
  row: MesQmsMotherRollGoodStatisticsApi.ProcessPivotRow,
  stage: MesQmsMotherRollGoodStatisticsApi.ProcessPivotStage,
  kind: PieceDialogKind,
) {
  if (kind === 'PROCESS_LOSS') {
    return collectProcessLossPieces(stage).map(({ piece, selfCheckNg, inspections }) => ({
      pieceNo: piece.pieceNo || piece.outputBatchNo || piece.sourceBatchNo,
      outputBatchNo: piece.outputBatchNo,
      lossSource: [selfCheckNg ? '自检NG' : '', inspections.length ? '送检' : '']
        .filter(Boolean).join('、'),
      inspectionNo: [...new Set(inspections.map((item) => item.inspectionNo).filter(Boolean))].join('、'),
      inspectionResult: inspectionResultText(inspections),
      remark: [summarizeRemarkText(piece.remark), inspectionDefectText(inspections)]
        .filter(Boolean).join('；'),
    }));
  }
  if (isProcessDrilldownStage(stage.stageCode)) {
    return buildProcessDrilldownRows(stage);
  }
  if (kind === 'INSPECTION' || kind === 'INSPECTION_NG') {
    return buildInspectionDrilldownRows(stage, kind === 'INSPECTION_NG');
  }
  if (kind === 'COA_INSPECTION' || kind === 'COA_INSPECTION_NG') {
    return buildInspectionDrilldownRows(stage, kind === 'COA_INSPECTION_NG', true);
  }
  return buildPieceDrilldownRows(row, stage, kind);
}

function drilldownTitle(
  stageMeta: StageColumn,
  kind: PieceDialogKind,
  columnKey?: StageSubColumnKey,
) {
  const kindLabel = drilldownKindLabel(stageMeta.code, kind, columnKey);
  if (isProcessDrilldownStage(stageMeta.code)) {
    return `${stageMeta.label}${kindLabel}明细`;
  }
  if (isInspectionDialogKind(kind)) {
    return `${stageMeta.label}${kindLabel}明细`;
  }
  return `${stageMeta.label}${kindLabel}片列表`;
}

function openPieceDialog(
  row: MesQmsMotherRollGoodStatisticsApi.ProcessPivotRow,
  stageMeta: StageColumn,
  kind: PieceDialogKind,
  columnKey?: StageSubColumnKey,
) {
  if (!canOpenPieceList(stageMeta)) {
    return;
  }
  const stage = stageForCell(row, stageMeta.code, columnKey);
  pieceDialog.open = true;
  pieceDialog.kind = kind;
  pieceDialog.keyword = '';
  pieceDialog.stageName = stageMeta.label;
  pieceDialog.title = drilldownTitle(stageMeta, kind, columnKey);
  pieceDialog.subtitle = drilldownSubtitle(row);
  pieceDialog.quantityText = pieceKindQtyText(stage, kind, columnKey);
  pieceDialog.columns = drilldownColumns(stageMeta.code, kind);
  pieceDialog.rows = buildDrilldownRows(row, stage, kind);
}

function openInspectionDialog(
  row: MesQmsMotherRollGoodStatisticsApi.ProcessPivotRow,
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
  pieceDialog.title = drilldownTitle(stageMeta, kind);
  pieceDialog.subtitle = drilldownSubtitle(row);
  pieceDialog.quantityText = pieceKindQtyText(stage, kind);
  pieceDialog.columns = drilldownColumns(stageMeta.code, kind);
  pieceDialog.rows = buildDrilldownRows(row, stage, kind);
}

function openCoaInspectionDialog(
  row: MesQmsMotherRollGoodStatisticsApi.ProcessPivotRow,
  stageMeta: StageColumn,
  ngOnly = false,
) {
  const stage = stageOf(row, stageMeta.code);
  const kind: PieceDialogKind = ngOnly ? 'COA_INSPECTION_NG' : 'COA_INSPECTION';
  if (!canOpenCoaInspectionList(stage, ngOnly)) {
    return;
  }
  pieceDialog.open = true;
  pieceDialog.kind = kind;
  pieceDialog.keyword = '';
  pieceDialog.stageName = stageMeta.label;
  pieceDialog.title = drilldownTitle(stageMeta, kind);
  pieceDialog.subtitle = drilldownSubtitle(row);
  pieceDialog.quantityText = pieceKindQtyText(stage, kind);
  pieceDialog.columns = drilldownColumns(stageMeta.code, kind);
  pieceDialog.rows = buildDrilldownRows(row, stage, kind);
}

function drilldownSubtitle(row: MesQmsMotherRollGoodStatisticsApi.ProcessPivotRow) {
  return `${motherBatchText(row)} / ${modelText(row)} / ${sizeText(row)}`;
}

function canOpenInspectionDetail(item: DrilldownTableRow) {
  return !!textOf(item.sourceType) && Number.isFinite(Number(item.inspectionId));
}

function openInspectionDetail(item: DrilldownTableRow) {
  if (!canOpenInspectionDetail(item)) {
    message.warning('缺少检验单来源信息，无法查看检验详情');
    return;
  }
  pieceDialog.open = false;
  inspectionDetailModalApi
    .setData({
      inspectionId: Number(item.inspectionId),
      inspectionNo: textOf(item.inspectionNo),
      ngOnly:
        pieceDialog.kind === 'INSPECTION_NG' ||
        pieceDialog.kind === 'COA_INSPECTION_NG',
      sourceType: textOf(item.sourceType),
      title: `${textOf(item.inspectionResult) || '检验'}详情`,
    })
    .open();
}

function drilldownTagColor(kind: PieceDialogKind) {
  if (kind === 'COA_INSPECTION') return 'purple';
  if (kind === 'COA_INSPECTION_NG') return 'red';
  if (kind === 'DEFECT') return 'red';
  if (kind === 'INSPECTION') return 'blue';
  if (kind === 'INSPECTION_NG') return 'red';
  return kind === 'DONE' ? 'green' : 'orange';
}

function drilldownRowKey(item: DrilldownTableRow, index: number) {
  return [
    textOf(item.pieceNo),
    textOf(item.inspectionNo),
    textOf(item.scanConfirmTime),
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
    'is-defect-column': column.defectColumn,
    'is-center': column.align === 'center',
    'is-piece-no': column.key === 'pieceNo',
    'is-right': column.align === 'right',
  };
}

function isDefectCountValue(value: unknown) {
  const number = Number(value);
  return Number.isFinite(number) && number > 0;
}

function drilldownCellText(item: DrilldownTableRow, column: DrilldownTableColumn) {
  const value = item[column.key];
  const text = textOf(value);
  return text || '-';
}

function drilldownStatusColor(value: unknown) {
  const text = textOf(value).toUpperCase();
  if (text === 'OK') return 'green';
  if (text === 'NG') return 'red';
  return 'orange';
}

function drilldownFlagColor(value: unknown) {
  return textOf(value) === '是' ? 'blue' : 'default';
}

function drilldownInspectionResultColor(value: unknown) {
  const text = textOf(value);
  if (text.includes('NG') || text.includes('驳回') || text.includes('失败')) {
    return 'red';
  }
  if (text.includes('OK') || text.includes('完成')) {
    return 'green';
  }
  if (text === '-') {
    return 'default';
  }
  return 'blue';
}

function showTotal(count: number) {
  return `共 ${count} 条`;
}

onMounted(fetchData);
</script>

<template>
  <Page
    auto-content-height
    class="process-pivot-page"
    content-class="process-pivot-content"
  >
    <div class="package-fg-console process-pivot-report">
      <section class="prototype-banner process-pivot-banner">
        <span class="console-main-icon"><IconifyIcon icon="lucide:table-2" /></span>
        <div class="console-title-block">
          <div class="console-title-row">
            <h2 class="console-title-text">母卷批次良品统计</h2>
          </div>
          <div class="console-meta-row">
            <span class="console-meta-item">
              <span class="console-meta-label">当前页</span>
              <span class="console-meta-value">{{ rows.length }}</span>
              <span class="console-meta-sub">行</span>
            </span>
            <span class="console-meta-item">
              <span class="console-meta-label">总数</span>
              <span class="console-meta-value">{{ total }}</span>
              <span class="console-meta-sub">行</span>
            </span>
            <span class="console-meta-item">
              <span class="console-meta-label">条件</span>
              <span class="console-meta-value">{{ activeFilterCount }}</span>
              <span class="console-meta-sub">项</span>
            </span>
          </div>
        </div>
        <div class="console-action-group process-pivot-action-group">
          <button class="action-tile" type="button" @click="handleShiftPlanDateRange(-1)">
            <IconifyIcon icon="lucide:chevron-left" />
            <span>上周</span>
          </button>
          <button class="action-tile" type="button" @click="handleShiftPlanDateRange(1)">
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
            :disabled="exporting"
            @click="handleExportExcel"
          >
            <IconifyIcon icon="lucide:file-spreadsheet" />
            <span>{{ exporting ? '导出中' : '导出' }}</span>
          </button>
          <button class="action-tile" type="button" @click="columnSettingVisible = true">
            <IconifyIcon icon="lucide:settings-2" />
            <span>列设置</span>
          </button>
        </div>
      </section>

      <section class="package-fg-filter-bar process-pivot-query-panel">
        <div class="process-pivot-simple-query">
          <label class="process-pivot-simple-query-label">计划日期</label>
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
            <template #icon><IconifyIcon icon="lucide:sliders-horizontal" /></template>
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
                    v-if="isBaseColumnVisible('motherRollBatchNo')"
                    class="col-mother-batch sticky-mother-batch"
                    :style="motherBatchStickyStyle"
                    rowspan="2"
                  >
                    母批批号
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
                    v-if="isBaseColumnVisible('segmentBatchNo')"
                    class="col-segment-batch sticky-segment-batch"
                    :style="segmentBatchStickyStyle"
                    rowspan="2"
                  >
                    磨皮分段片号
                  </th>
                  <th
                    v-for="stage in visibleStageColumns"
                    :key="stage.code"
                    :class="['stage-group', stageToneClass(stage)]"
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
                      :class="[column.className, stageToneClass(stage)]"
                    >
                      {{ column.label }}
                    </th>
                  </template>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="(row, rowIndex) in rows"
                  :key="row.pivotRowKey || `${motherBatchText(row)}-${modelText(row)}-${sizeText(row)}`"
                >
                  <td
                    v-if="isBaseColumnVisible('motherRollBatchNo') && isFirstPlanRow(row, rowIndex)"
                    class="cell-mother-batch sticky-mother-batch"
                    :style="motherBatchStickyStyle"
                    :rowspan="planRowSpan(row)"
                  >
                    <Tooltip :title="motherBatchText(row)">
                      <span class="batch-text">{{ compactText(motherBatchText(row), 18) }}</span>
                    </Tooltip>
                  </td>
                  <td
                    v-if="isBaseColumnVisible('modelCode') && isFirstModelSizeRow(row, rowIndex)"
                    :rowspan="modelSizeRowSpan(row)"
                  >
                    {{ modelText(row) }}
                  </td>
                  <td
                    v-if="isBaseColumnVisible('materialCode') && isFirstPlanRow(row, rowIndex)"
                    :rowspan="planRowSpan(row)"
                  >
                    <Tooltip :title="row.materialName || row.materialCode">
                      <span>{{ row.materialCode || '-' }}</span>
                    </Tooltip>
                  </td>
                  <td
                    v-if="isBaseColumnVisible('sizeSpec') && isFirstModelSizeRow(row, rowIndex)"
                    :rowspan="modelSizeRowSpan(row)"
                  >
                    {{ sizeText(row) }}
                  </td>
                  <td
                    v-if="isBaseColumnVisible('segmentBatchNo') && isFirstSegmentRow(row, rowIndex)"
                    class="cell-segment-batch sticky-segment-batch"
                    :style="segmentBatchStickyStyle"
                    :rowspan="segmentRowSpan(row)"
                  >
                    <Tooltip :title="segmentText(row)">
                      <span class="batch-text">{{ compactText(segmentText(row), 18) }}</span>
                    </Tooltip>
                  </td>
                  <template
                    v-for="stageMeta in visibleStageColumns"
                    :key="`${row.id}-${stageMeta.code}`"
                  >
                    <template
                      v-for="column in visibleStageSubColumns(stageMeta)"
                      :key="`${stageMeta.code}-${column.key}-${row.pivotRowKey || rowIndex}`"
                    >
                      <td
                        v-if="
                          isFirstStageCellRow(
                            row,
                            rowIndex,
                            stageMeta.code,
                            column.key,
                          )
                        "
                        :class="[stageCellClass(column), stageToneClass(stageMeta)]"
                        :rowspan="
                          stageCellRowSpan(row, stageMeta.code, column.key)
                        "
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
                        <template v-else-if="column.key === 'segmentInputQty'">
                          {{
                            stageSegmentInputText(
                              stageOf(row, stageMeta.code),
                            )
                          }}
                        </template>
                        <template v-else-if="column.key === 'inputQty'">
                          {{ stageInputText(stageOf(row, stageMeta.code)) }}
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
                        <template v-else-if="column.key === 'motherOutputQty'">
                          {{
                            stageMotherOutputText(
                              stageOf(row, stageMeta.code),
                            )
                          }}
                        </template>
                        <template
                          v-else-if="
                            column.key === 'finalInspectionOutputQty'
                          "
                        >
                          {{
                            stageFinalInspectionOutputText(
                              stageOf(row, stageMeta.code),
                            )
                          }}
                        </template>
                        <template
                          v-else-if="column.key === 'finalInspectionYieldRate'"
                        >
                          {{
                            stageFinalInspectionYieldRateText(
                              stageOf(row, stageMeta.code),
                            )
                          }}
                        </template>
                        <template
                          v-else-if="column.key === 'theoreticalOutputQty'"
                        >
                          <Tooltip
                            v-if="
                              !stageOf(row, stageMeta.code)
                                .theoreticalOutputMatched
                            "
                            title="需维护该产品的配料/湿法理论产量，并识别有效分段数后按公式计算"
                          >
                            <span>-</span>
                          </Tooltip>
                          <span v-else>
                            {{
                              stageTheoreticalOutputText(
                                stageOf(row, stageMeta.code),
                              )
                            }}
                          </span>
                        </template>
                        <template v-else-if="column.key === 'goodYieldRate'">
                          {{
                            stageGoodYieldRateText(
                              stageOf(row, stageMeta.code),
                            )
                          }}
                        </template>
                        <template v-else-if="column.key === 'goodTargetRate'">
                          {{
                            stageGoodTargetRateText(
                              stageOf(row, stageMeta.code),
                            )
                          }}
                        </template>
                        <template
                          v-else-if="column.key === 'glueBoardInspectionQty'"
                        >
                          {{
                            stageGlueBoardInspectionText(
                              stageOf(row, stageMeta.code),
                            )
                          }}
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
                              stageInspectionText(
                                stageOf(row, stageMeta.code),
                              )
                            }}
                          </button>
                          <span v-else>
                            {{
                              stageInspectionText(
                                stageOf(row, stageMeta.code),
                              )
                            }}
                          </span>
                        </template>
                        <template
                          v-else-if="
                            column.key === 'processProductionInspectionQty'
                          "
                        >
                          <button
                            v-if="stageMeta.code === 'PRESS_SLOT'"
                            class="qty-link qty-link--inspection-ng"
                            type="button"
                            @click="openPieceDialog(row, stageMeta, 'PROCESS_LOSS')"
                          >
                            {{ stageProcessLossText(stageOf(row, stageMeta.code)) }}
                          </button>
                          <span v-else>
                            {{ stageProcessLossText(stageOf(row, stageMeta.code)) }}
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
                        <template v-else-if="column.key === 'coaInspectionQty'">
                          <button
                            v-if="
                              canOpenCoaInspectionList(
                                stageOf(row, stageMeta.code),
                              )
                            "
                            class="qty-link qty-link--inspection"
                            type="button"
                            @click="openCoaInspectionDialog(row, stageMeta)"
                          >
                            {{
                              stageCoaInspectionText(
                                stageOf(row, stageMeta.code),
                              )
                            }}
                          </button>
                          <span v-else>
                            {{
                              stageCoaInspectionText(
                                stageOf(row, stageMeta.code),
                              )
                            }}
                          </span>
                        </template>
                        <template v-else-if="column.key === 'coaInspectionNgQty'">
                          <button
                            v-if="
                              canOpenCoaInspectionList(
                                stageOf(row, stageMeta.code),
                                true,
                              )
                            "
                            class="qty-link qty-link--inspection-ng"
                            type="button"
                            @click="openCoaInspectionDialog(row, stageMeta, true)"
                          >
                            {{
                              stageCoaInspectionNgText(
                                stageOf(row, stageMeta.code),
                              )
                            }}
                          </button>
                          <span v-else>
                            {{
                              stageCoaInspectionNgText(
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
                            {{
                              stagePendingText(stageOf(row, stageMeta.code))
                            }}
                          </button>
                          <span v-else>
                            {{
                              stagePendingText(stageOf(row, stageMeta.code))
                            }}
                          </span>
                        </template>
                        <template
                          v-else-if="column.key === 'segmentDefectQty'"
                        >
                          <button
                            v-if="canOpenPieceList(stageMeta)"
                            class="qty-link qty-link--defect"
                            type="button"
                            @click="openPieceDialog(row, stageMeta, 'DEFECT', column.key)"
                          >
                            {{
                              stageSegmentDefectText(
                                stageOf(row, stageMeta.code),
                              )
                            }}
                          </button>
                          <span v-else>
                            {{
                              stageSegmentDefectText(
                                stageOf(row, stageMeta.code),
                              )
                            }}
                          </span>
                        </template>
                        <template v-else-if="column.key === 'defectQty'">
                          <button
                            v-if="canOpenPieceList(stageMeta)"
                            class="qty-link qty-link--defect"
                            type="button"
                            @click="openPieceDialog(row, stageMeta, 'DEFECT', column.key)"
                          >
                            {{
                              stageDefectText(
                                stageForCell(row, stageMeta.code, column.key),
                              )
                            }}
                          </button>
                          <span v-else>
                            {{
                              stageDefectText(
                                stageForCell(row, stageMeta.code, column.key),
                              )
                            }}
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
                    暂无母卷批次良品统计数据
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
          class="report-formula-note"
          :class="{ 'is-expanded': formulaNoteExpanded }"
        >
          <button
            class="formula-note-toggle"
            type="button"
            :aria-expanded="formulaNoteExpanded"
            @click="formulaNoteExpanded = !formulaNoteExpanded"
          >
            <IconifyIcon
              :icon="
                formulaNoteExpanded
                  ? 'lucide:chevron-up'
                  : 'lucide:chevron-down'
              "
            />
            <span>{{ formulaNoteExpanded ? '收起列公式' : '展开列公式' }}</span>
          </button>
          <div class="formula-note-summary">
            列公式默认收缩；配料、湿法读取基础配置，后续理论产出按各工序投入和分段数自动推导。
          </div>
          <div v-if="formulaNoteExpanded" class="formula-note-body">
            <div class="formula-note-row">
              <strong>固定理论产出</strong>
              <span>公式：读取工序合格目标配置中该产品的配料理论产量和湿法理论产量。</span>
              <span>说明：磨皮及后续工序不维护静态理论产量，按各工序投入和分段数公式计算。</span>
            </div>
            <div class="formula-note-row">
              <strong>磨皮理论产出</strong>
              <span>公式：磨皮投入 - (1 + 2) × 分段数。</span>
              <span>示例：磨皮投入105m、四段，则理论产出=105 - 3 × 4 = 93m。</span>
            </div>
            <div class="formula-note-row">
              <strong>粘胶1理论产出</strong>
              <span>公式：粘胶1投入 - (1 + 1) × 分段数。</span>
              <span>示例：粘胶1投入93m、四段，则理论产出=93 - 2 × 4 = 85m。</span>
            </div>
            <div class="formula-note-row">
              <strong>分切理论产出</strong>
              <span>公式：INT(分切投入米数 / 0.85)，有小数时向下取整。</span>
              <span>示例：分切投入90.2m，则理论产出=INT(90.2 / 0.85)=106片。</span>
            </div>
            <div class="formula-note-row">
              <strong>压槽理论产出</strong>
              <span>公式：等于压槽投入。</span>
              <span>示例：压槽投入106片，则理论产出=106片。</span>
            </div>
            <div class="formula-note-row">
              <strong>粘胶2理论产出</strong>
              <span>公式：粘胶2投入 - 1 × 分段数。</span>
              <span>示例：粘胶2投入100片、四段，则理论产出=100 - 4 = 96片。</span>
            </div>
            <div class="formula-note-row">
              <strong>裁切/检验理论产出</strong>
              <span>公式：裁切理论产出=裁切投入；检验理论产出=检验投入。</span>
              <span>说明：母卷最终理论产出单独按 INT((湿法理论产出 - 3n - 2n) / 0.85) - n 计算。</span>
            </div>
            <div class="formula-note-row">
              <strong>良品率（%）</strong>
              <span>公式：本工序产出 / 本工序理论产出 × 100%，超过 100% 时显示为 100%；裁切使用裁切产出作为分子。</span>
              <span>示例：裁切产出73片、裁切理论产出106片，则良品率=68.87%。</span>
            </div>
            <div class="formula-note-row">
              <strong>母卷良品达标率（%）</strong>
              <span>公式：检验产出 / 母卷最终理论产出 × 100%，超过 100% 时显示为 100%。</span>
              <span>示例：W26 两段最终理论产出 INT((102 - 3 × 2 - 2 × 2) / 0.85) - 2 = 106片。</span>
            </div>
            <div class="formula-note-row">
              <strong>终检良率</strong>
              <span>公式：终检产出 / 裁切总产出 × 100%。</span>
              <span>示例：终检产出64片、裁切总产出76片，则终检良率=84.21%。</span>
            </div>
            <div class="formula-note-row">
              <strong>分段自检/送检明细</strong>
              <span>公式：分段自检OK、分段自检NG、未加工、送检NG等仍来自各工序实际报工与检验记录。</span>
              <span>说明：这些明细用于追溯实际产出，不再作为理论产出的分母公式。</span>
            </div>
            <div class="formula-note-row">
              <strong>胶板送检(m)</strong>
              <span>公式：仅展示粘胶2胶板送检米数，不参与理论产出、良品率和母卷良品达标率计算。</span>
              <span>示例：胶板送检0.5m，页面显示0.5m。</span>
            </div>
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
                    setBaseColumnVisible(
                      column.key,
                      getCheckedFromEvent(event),
                    )
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
            <Input v-model:value="queryForm.planNo" allow-clear placeholder="计划号" @press-enter="applyAdvancedQuery" />
          </div>
          <div class="process-pivot-query-item">
            <label>母批批号</label>
            <Input v-model:value="queryForm.motherRollBatchNo" allow-clear placeholder="母批/生产/主批号" @press-enter="applyAdvancedQuery" />
          </div>
          <div class="process-pivot-query-item">
            <label>分段批号</label>
            <Input v-model:value="queryForm.motherSegmentBatchNo" allow-clear placeholder="分段批号" @press-enter="applyAdvancedQuery" />
          </div>
          <div class="process-pivot-query-item">
            <label>型号</label>
            <Input v-model:value="queryForm.modelCode" allow-clear placeholder="型号" @press-enter="applyAdvancedQuery" />
          </div>
          <div class="process-pivot-query-item">
            <label>料号</label>
            <Input v-model:value="queryForm.materialKeyword" allow-clear placeholder="料号或名称" @press-enter="applyAdvancedQuery" />
          </div>
          <div class="process-pivot-query-item">
            <label>尺寸规格</label>
            <Input v-model:value="queryForm.sizeSpec" allow-clear placeholder="尺寸规格" @press-enter="applyAdvancedQuery" />
          </div>
          <div class="process-pivot-query-item">
            <label>开始日期起</label>
            <DatePicker v-model:value="queryForm.productionStartDateStart" placeholder="计划开始起" value-format="YYYY-MM-DD" />
          </div>
          <div class="process-pivot-query-item">
            <label>开始日期止</label>
            <DatePicker v-model:value="queryForm.productionStartDateEnd" placeholder="计划开始止" value-format="YYYY-MM-DD" />
          </div>
          <div class="process-pivot-query-item">
            <label>结束日期起</label>
            <DatePicker v-model:value="queryForm.productionEndDateStart" placeholder="计划结束起" value-format="YYYY-MM-DD" />
          </div>
          <div class="process-pivot-query-item">
            <label>结束日期止</label>
            <DatePicker v-model:value="queryForm.productionEndDateEnd" placeholder="计划结束止" value-format="YYYY-MM-DD" />
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
            isInspectionDialogKind(pieceDialog.kind)
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
      <p v-if="pieceDialog.kind === 'PROCESS_LOSS'">
        仅统计分切已确认且OK的片；压槽自检NG片与送检片按片号去重，送检OK也计入，同一片只计一次。
        当前明细 {{ pieceDialog.rows.length }} 片。
        <span v-if="pieceDialog.quantityText !== formatQty(pieceDialog.rows.length, '片')">
          请核对汇总数量与明细数量，若不一致请刷新后检查数据。
        </span>
      </p>
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
                    column.key === 'firstInspectionFlagText' ||
                    column.key === 'inspectionFlagText'
                  "
                >
                  <Tag :color="drilldownFlagColor(item[column.key])">
                    {{ drilldownCellText(item, column) }}
                  </Tag>
                </template>
                <template v-else-if="column.key === 'inspectionResult'">
                  <Button
                    v-if="canOpenInspectionDetail(item)"
                    size="small"
                    type="link"
                    @click="openInspectionDetail(item)"
                  >
                    {{ drilldownCellText(item, column) }}
                  </Button>
                  <Tag v-else :color="drilldownInspectionResultColor(item.inspectionResult)">
                    {{ drilldownCellText(item, column) }}
                  </Tag>
                </template>
                <template v-else-if="column.defectColumn">
                  <span
                    :class="{
                      'defect-count-text': isDefectCountValue(item[column.key]),
                    }"
                  >
                    {{ drilldownCellText(item, column) }}
                  </span>
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
  isolation: isolate;
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

.report-formula-note {
  display: grid;
  flex-shrink: 0;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 6px 10px;
  align-items: center;
  border-right: 1px solid var(--report-border);
  border-left: 1px solid var(--report-border);
  background: #f8fafc;
  padding: 5px 8px 6px;
  color: #475569;
  font-size: 12px;
  line-height: 18px;
}

.formula-note-toggle {
  display: inline-flex;
  height: 24px;
  min-width: 0;
  align-items: center;
  gap: 4px;
  border: 1px solid #b7c7d9;
  border-radius: 4px;
  background: #e8eff8;
  padding: 0 8px;
  color: #1f3f66;
  font-weight: 800;
  cursor: pointer;
}

.formula-note-toggle:hover {
  border-color: #3576c8;
  background: #edf5ff;
}

.formula-note-toggle svg {
  width: 14px;
  height: 14px;
}

.formula-note-summary {
  min-width: 0;
  overflow: hidden;
  color: #64748b;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.formula-note-body {
  display: grid;
  grid-column: 1 / -1;
  grid-template-columns: repeat(auto-fit, minmax(min(100%, 360px), 1fr));
  gap: 6px;
}

.formula-note-row {
  display: grid;
  grid-template-columns: 92px minmax(0, 1.1fr) minmax(0, 1fr);
  min-width: 0;
  overflow: hidden;
  border: 1px solid #d4deea;
  background: #fff;
}

.formula-note-row strong,
.formula-note-row span {
  min-width: 0;
  padding: 5px 7px;
  overflow: visible;
  line-height: 18px;
  overflow-wrap: break-word;
  white-space: normal;
}

.formula-note-row strong {
  display: flex;
  align-items: center;
  border-right: 1px solid #d4deea;
  background: #edf3fa;
  color: #203a5f;
  font-weight: 800;
}

.formula-note-row span + span {
  border-left: 1px solid #d4deea;
  color: #667085;
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

.pivot-table tbody tr:hover td {
  background: #f8fbff;
}

.stage-group {
  border-top: 2px solid #8aa4c4;
}

.pivot-table thead th.stage-tone-a {
  background: #e7f1ff;
}

.pivot-table thead th.stage-tone-b {
  background: #e9f7ef;
}

.pivot-table thead tr:nth-child(2) th.stage-tone-a {
  background: #f1f7ff;
}

.pivot-table thead tr:nth-child(2) th.stage-tone-b {
  background: #f1faf4;
}

.pivot-table tbody td.stage-tone-a {
  background: #fbfdff;
}

.pivot-table tbody td.stage-tone-b {
  background: #f8fdf9;
}

.pivot-table tbody tr:hover td.stage-tone-a {
  background: #eef6ff;
}

.pivot-table tbody tr:hover td.stage-tone-b {
  background: #edf9f1;
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

.col-segment-batch {
  width: 136px;
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

.col-stage-qty-wide {
  width: 148px;
}

.col-stage-output,
.col-stage-target {
  width: 92px;
}

.sticky-mother-batch {
  position: sticky;
  z-index: 4;
  background: #fff;
  background-clip: padding-box;
  box-shadow:
    inset 1px 0 0 var(--report-border),
    inset -1px 0 0 var(--report-border),
    inset 0 -1px 0 var(--report-border);
}

th.sticky-mother-batch {
  z-index: 5;
  background: var(--report-header);
  box-shadow:
    inset 1px 0 0 var(--report-border),
    inset -1px 0 0 var(--report-border),
    inset 0 1px 0 var(--report-border),
    inset 0 -1px 0 var(--report-border);
}

.sticky-mother-batch {
  left: 0;
}

.sticky-segment-batch {
  position: sticky;
  z-index: 4;
  background: #fff;
  background-clip: padding-box;
  box-shadow:
    inset 1px 0 0 var(--report-border),
    inset -1px 0 0 var(--report-border),
    inset 0 -1px 0 var(--report-border);
}

th.sticky-segment-batch {
  z-index: 5;
  background: var(--report-header);
  box-shadow:
    inset 1px 0 0 var(--report-border),
    inset -1px 0 0 var(--report-border),
    inset 0 1px 0 var(--report-border),
    inset 0 -1px 0 var(--report-border);
}

.cell-mother-batch {
  font-weight: 500;
}

.cell-segment-batch {
  font-weight: 500;
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

.inspection-cell {
  color: #2563eb;
  font-weight: 600;
}

.input-cell {
  color: #854d0e;
  font-weight: 600;
}

.mother-output-cell {
  color: #155e75;
  font-weight: 600;
}

.final-inspection-output-cell {
  color: #0f766e;
  font-weight: 600;
}

.theoretical-output-cell {
  color: #166534;
  font-weight: 600;
}

.good-yield-rate-cell {
  color: #1d4ed8;
  font-weight: 600;
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

.drilldown-table td.is-defect-column,
.drilldown-table tr:nth-child(even) td.is-defect-column {
  background: #fff1f1;
}

.defect-count-text {
  color: #f5222d;
  font-weight: 700;
}

.drilldown-table :deep(.ant-tag) {
  margin-inline-end: 0;
}

.drilldown-table :deep(.ant-btn-link) {
  height: 20px;
  padding: 0;
  font-size: 12px;
}

.piece-empty {
  color: #667085;
  font-size: 12px;
  padding: 28px 0;
  text-align: center;
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
