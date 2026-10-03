<script lang="ts" setup>
import type {
  CellClassParams,
  CellClickedEvent,
  ColDef,
  ColGroupDef,
  GetRowIdParams,
  GridApi,
  GridReadyEvent,
  ICellRendererParams,
  SpanRowsParams,
  ValueFormatterParams,
} from 'ag-grid-enterprise';

import type { MesQmsMotherRollGoodStatisticsApi } from '#/api/mes/quality/statistics/mother-roll-good-statistics';

import { computed, nextTick, onMounted, reactive, ref, shallowRef } from 'vue';

import { Page, useVbenModal } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import {
  AllEnterpriseModule,
  LicenseManager,
  ModuleRegistry,
  themeQuartz,
} from 'ag-grid-enterprise';
import { AgGridVue } from 'ag-grid-vue3';
import {
  Button,
  DatePicker,
  Input,
  message,
  Modal,
  Pagination,
  Spin,
  Tag,
} from 'ant-design-vue';
import dayjs from 'dayjs';

import { getMotherRollGoodStatisticsPage } from '#/api/mes/quality/statistics/mother-roll-good-statistics';

import InspectionDetailModalForm from '../../shared/inspection-detail-modal.vue';

defineOptions({ name: 'MesQmsMotherRollGoodStatisticsAggrid' });

let enterpriseModuleRegistered = false;

function setupAgGridEnterprise() {
  if (enterpriseModuleRegistered) {
    return;
  }
  ModuleRegistry.registerModules([AllEnterpriseModule]);
  const env = import.meta.env as Record<string, string | undefined>;
  const licenseKey = String(env.VITE_AG_GRID_LICENSE_KEY || '').trim();
  if (licenseKey) {
    LicenseManager.setLicenseKey(licenseKey);
  }
  enterpriseModuleRegistered = true;
}

setupAgGridEnterprise();

const RangePicker = DatePicker.RangePicker;

type StageCode =
  | 'ADHESIVE1'
  | 'ADHESIVE2'
  | 'CUT_ROUND'
  | 'FORMULA'
  | 'GRINDING'
  | 'PRESS_SLOT'
  | 'SHIPPING_INSPECTION'
  | 'SLITTING'
  | 'WET';

type StageMetricKey =
  | 'batch'
  | 'defectQty'
  | 'doneQty'
  | 'finalInspectionOutputQty'
  | 'glueBoardInspectionQty'
  | 'goodYieldRate'
  | 'inputQty'
  | 'inspectionNgQty'
  | 'inspectionQty'
  | 'lastReportTime'
  | 'motherOutputQty'
  | 'pendingQty'
  | 'processLength'
  | 'segmentDefectQty'
  | 'segmentInputQty'
  | 'startPosition'
  | 'theoreticalOutputQty';

type PieceDialogKind =
  | 'DEFECT'
  | 'DONE'
  | 'INSPECTION'
  | 'INSPECTION_NG'
  | 'PENDING';

interface StageDefinition {
  batchLabel: string;
  code: StageCode;
  label: string;
  metricColumns?: Array<{
    field: 'processLength' | 'startPosition';
    label: string;
  }>;
  showBatch?: boolean;
}

interface StageMetricColumnDefinition {
  aggFunc?: 'sum';
  cellClass?: ColDef<AgGridEvaluationRow>['cellClass'];
  cellRenderer?: ColDef<AgGridEvaluationRow>['cellRenderer'];
  field: StageMetricKey;
  filter?: ColDef<AgGridEvaluationRow>['filter'];
  headerClass?: ColDef<AgGridEvaluationRow>['headerClass'];
  headerName: string;
  hide?: boolean;
  minWidth: number;
  onCellClicked?: ColDef<AgGridEvaluationRow>['onCellClicked'];
  spanRows?: ColDef<AgGridEvaluationRow>['spanRows'];
  suppressHeaderFilterButton?: boolean;
  suppressHeaderMenuButton?: boolean;
  type?: ColDef<AgGridEvaluationRow>['type'];
  valueFormatter?: ColDef<AgGridEvaluationRow>['valueFormatter'];
}

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

interface AgGridEvaluationRow {
  [key: string]: number | string | undefined;
  materialCode?: string;
  modelCode?: string;
  motherRollBatchNo?: string;
  rowKey: string;
  segmentBatchNo?: string;
  sizeSpec?: string;
  sourceIndex: number;
}

const STAGE_DEFINITIONS: StageDefinition[] = [
  { batchLabel: '母批批号', code: 'FORMULA', label: '配料', showBatch: false },
  { batchLabel: '母批批号', code: 'WET', label: '湿法', showBatch: false },
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
  { batchLabel: '分段批号', code: 'ADHESIVE1', label: '粘胶1' },
  { batchLabel: '片号', code: 'SLITTING', label: '分切', showBatch: false },
  { batchLabel: '片号', code: 'PRESS_SLOT', label: '压槽', showBatch: false },
  { batchLabel: '片号', code: 'ADHESIVE2', label: '粘胶2', showBatch: false },
  { batchLabel: '片号', code: 'CUT_ROUND', label: '裁切', showBatch: false },
  {
    batchLabel: '片号',
    code: 'SHIPPING_INSPECTION',
    label: '发货检验',
    showBatch: false,
  },
];

const PENDING_STAGE_CODES = new Set<StageCode>([
  'ADHESIVE2',
  'CUT_ROUND',
  'PRESS_SLOT',
  'SLITTING',
]);
const PIECE_INPUT_STAGE_CODES = PENDING_STAGE_CODES;
const PIECE_SELF_CHECK_STAGE_CODES = PENDING_STAGE_CODES;
const OUTPUT_BATCH_PREFERRED_STAGE_CODES = new Set<StageCode>([
  'ADHESIVE2',
  'CUT_ROUND',
  'PRESS_SLOT',
]);
const METER_REPORT_STAGE_CODES = new Set<StageCode>(['ADHESIVE1', 'GRINDING']);
const MOTHER_OUTPUT_STAGE_CODES = new Set<StageCode>([
  'ADHESIVE1',
  'ADHESIVE2',
  'CUT_ROUND',
  'GRINDING',
  'PRESS_SLOT',
  'SLITTING',
]);
const THEORETICAL_OUTPUT_STAGE_CODES = new Set<StageCode>([
  'ADHESIVE1',
  'ADHESIVE2',
  'CUT_ROUND',
  'FORMULA',
  'GRINDING',
  'PRESS_SLOT',
  'SLITTING',
  'WET',
]);

const PIECE_DRILLDOWN_COLUMNS: DrilldownTableColumn[] = [
  { align: 'center', key: 'index', label: '序号', width: 56 },
  { key: 'pieceNo', label: '片号', minWidth: 188 },
  { key: 'scanConfirmTime', label: '扫码确认时间', minWidth: 150 },
  { align: 'center', key: 'confirmStatus', label: '确认状态', width: 88 },
  { key: 'ngType', label: 'NG类型', minWidth: 136 },
  { align: 'center', key: 'coaFlagText', label: 'COA标记', width: 84 },
  {
    align: 'center',
    key: 'firstInspectionFlagText',
    label: '首检标记',
    width: 84,
  },
  { align: 'center', key: 'inspectionFlagText', label: '送检标记', width: 84 },
  { key: 'inspectionResult', label: '送检结果', minWidth: 108 },
];

const DEFECT_CATEGORY_COLUMNS: DrilldownTableColumn[] = [
  {
    align: 'right',
    defectColumn: true,
    key: 'defectBlackDot',
    label: '黑点',
    width: 72,
  },
  {
    align: 'right',
    defectColumn: true,
    key: 'defectBlueDot',
    label: '蓝点',
    width: 72,
  },
  {
    align: 'right',
    defectColumn: true,
    key: 'defectYellowDot',
    label: '黄点',
    width: 72,
  },
  {
    align: 'right',
    defectColumn: true,
    key: 'defectRedDot',
    label: '红点',
    width: 72,
  },
  {
    align: 'right',
    defectColumn: true,
    key: 'defectPinHole',
    label: '针孔',
    width: 72,
  },
  {
    align: 'right',
    defectColumn: true,
    key: 'defectStripe',
    label: '条纹',
    width: 72,
  },
  {
    align: 'right',
    defectColumn: true,
    key: 'defectWrinkle',
    label: '褶皱',
    width: 72,
  },
  {
    align: 'right',
    defectColumn: true,
    key: 'defectWave',
    label: '波浪纹',
    width: 82,
  },
  {
    align: 'right',
    defectColumn: true,
    key: 'defectOther',
    label: '其他',
    width: 72,
  },
];
const DEFECT_CATEGORY_LABELS = DEFECT_CATEGORY_COLUMNS.map((column) => ({
  key: column.key,
  label: column.label,
}));

const PIECE_NG_DRILLDOWN_COLUMNS: DrilldownTableColumn[] = [
  { align: 'center', key: 'index', label: '序号', width: 56 },
  { key: 'pieceNo', label: '片号', minWidth: 188 },
  { key: 'scanConfirmTime', label: '扫码确认时间', minWidth: 150 },
  ...DEFECT_CATEGORY_COLUMNS,
];

const FORMULA_DRILLDOWN_COLUMNS: DrilldownTableColumn[] = [
  { align: 'center', key: 'index', label: '序号', width: 56 },
  { align: 'right', key: 'doneQtyText', label: '完工', minWidth: 120 },
];

const WET_DRILLDOWN_COLUMNS: DrilldownTableColumn[] = [
  { align: 'center', key: 'index', label: '序号', width: 56 },
  {
    align: 'right',
    key: 'wetReportQtyText',
    label: '收卷米数(报工数)',
    minWidth: 150,
  },
  {
    align: 'right',
    key: 'napInspectionQtyText',
    label: 'NAP层送检(米)',
    minWidth: 138,
  },
  {
    align: 'right',
    key: 'fixedLossQtyText',
    label: '固定损耗（米）',
    minWidth: 132,
  },
];

const METER_REPORT_DRILLDOWN_COLUMNS: DrilldownTableColumn[] = [
  { align: 'center', key: 'index', label: '序号', width: 56 },
  { align: 'right', key: 'inputQtyText', label: '投入米数', minWidth: 110 },
  {
    align: 'right',
    key: 'fixedLossQtyText',
    label: '固定损耗（米）',
    minWidth: 132,
  },
  {
    align: 'right',
    key: 'napSampleQtyText',
    label: 'NAP留样米数/送检送数',
    minWidth: 178,
  },
  { align: 'right', key: 'outputQtyText', label: '产出米数', minWidth: 110 },
];

const INSPECTION_DRILLDOWN_COLUMNS: DrilldownTableColumn[] = [
  { align: 'center', key: 'index', label: '序号', width: 56 },
  { key: 'pieceNo', label: '片号/批号', minWidth: 188 },
  { key: 'scanConfirmTime', label: '送检时间', minWidth: 150 },
  { key: 'inspectionType', label: '送检类型', minWidth: 110 },
  {
    align: 'right',
    key: 'inspectionQtyText',
    label: '送检数量',
    minWidth: 104,
  },
  { align: 'right', key: 'inspectionNgQtyText', label: 'NG数量', minWidth: 92 },
  { key: 'inspectionResult', label: '送检结果', minWidth: 108 },
  { key: 'ngType', label: 'NG类型', minWidth: 136 },
];

const gridTheme = themeQuartz.withParams({
  accentColor: '#1677ff',
  borderColor: '#d8e0eb',
  browserColorScheme: 'light',
  cellHorizontalPaddingScale: 0.78,
  columnBorder: true,
  fontFamily:
    'Microsoft YaHei, PingFang SC, Hiragino Sans GB, Segoe UI, sans-serif',
  fontSize: 12,
  headerBackgroundColor: '#f5f7fb',
  headerFontWeight: 600,
  headerTextColor: '#1f2937',
  oddRowBackgroundColor: '#fbfdff',
  rowHoverColor: '#eef6ff',
  spacing: 6,
  wrapperBorderRadius: 6,
});

const query = reactive({
  pageNo: 1,
  pageSize: 100,
  planDateRange: getDefaultPlanDateRange(),
});

const gridApi = shallowRef<GridApi<AgGridEvaluationRow>>();
const rows = ref<MesQmsMotherRollGoodStatisticsApi.ProcessPivotRow[]>([]);
const total = ref(0);
const loading = ref(false);
const quickFilterText = ref('');
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

const defaultColDef: ColDef<AgGridEvaluationRow> = {
  cellDataType: false,
  enablePivot: true,
  enableRowGroup: true,
  enableValue: true,
  filter: true,
  minWidth: 96,
  resizable: true,
  sortable: true,
  valueFormatter: textFormatter,
};

const autoGroupColumnDef: ColDef<AgGridEvaluationRow> = {
  cellRendererParams: {
    suppressCount: false,
  },
  headerName: '母批批号',
  minWidth: 220,
  pinned: 'left',
  valueFormatter: textFormatter,
};

const localeText = {
  aggregationColumnsEmptyMessage: '拖入列进行汇总',
  applyFilter: '应用',
  autosizeAllColumns: '自适应全部列',
  blanks: '空值',
  cancelFilter: '取消',
  columns: '列',
  columnsToolPanel: '列面板',
  contains: '包含',
  copy: '复制',
  csvExport: '导出 CSV',
  equals: '等于',
  excelExport: '导出 Excel',
  filters: '筛选',
  filtersToolPanel: '筛选面板',
  groupColumnsEmptyMessage: '拖入列进行分组',
  loadingOoo: '加载中...',
  noRowsToShow: '暂无母卷批次良品统计数据',
  notEqual: '不等于',
  pivotColumnsEmptyMessage: '拖入列进行透视',
  pinColumn: '固定列',
  resetColumns: '重置列',
  searchOoo: '搜索...',
  selectAll: '全选',
  sum: '合计',
  ungroupBy: '取消分组',
  valueColumnsEmptyMessage: '拖入列进行聚合',
};

const sideBar = {
  defaultToolPanel: 'columns',
  toolPanels: ['columns', 'filters'],
};

const statusBar = {
  statusPanels: [
    { align: 'left', statusPanel: 'agTotalAndFilteredRowCountComponent' },
    { statusPanel: 'agSelectedRowCountComponent' },
    { statusPanel: 'agAggregationComponent' },
  ],
};

const rowSelection = {
  checkboxes: false,
  enableClickSelection: false,
  headerCheckbox: false,
  mode: 'multiRow',
} as const;

const columnDefs = computed<
  Array<ColDef<AgGridEvaluationRow> | ColGroupDef<AgGridEvaluationRow>>
>(() => {
  const baseColumns: ColDef<AgGridEvaluationRow>[] = [
    {
      cellClass: 'aggrid-cell-strong',
      field: 'motherRollBatchNo',
      headerName: '母批批号',
      minWidth: 150,
      pinned: 'left',
      spanRows: baseColumnSpanRows('motherRollBatchNo'),
    },
    {
      field: 'modelCode',
      headerName: '型号',
      minWidth: 116,
      pinned: 'left',
      spanRows: baseColumnSpanRows('modelCode'),
    },
    {
      field: 'materialCode',
      headerName: '料号',
      minWidth: 145,
      spanRows: baseColumnSpanRows('materialCode'),
    },
    {
      field: 'sizeSpec',
      headerName: '尺寸',
      minWidth: 108,
      spanRows: baseColumnSpanRows('sizeSpec'),
    },
    {
      cellClass: 'aggrid-cell-strong',
      field: 'segmentBatchNo',
      headerName: '磨皮分段片号',
      minWidth: 160,
      spanRows: baseColumnSpanRows('segmentBatchNo'),
    },
  ];

  return [
    ...baseColumns,
    ...STAGE_DEFINITIONS.map((stage) => buildStageColumnGroup(stage)),
  ];
});

const gridRows = computed(() =>
  rows.value.map((row, index) => flattenRow(row, index)),
);

function buildStageColumnGroup(
  stage: StageDefinition,
): ColGroupDef<AgGridEvaluationRow> {
  return {
    children: buildStageMetricColumns(stage).map((column) => {
      const metricKey = column.field;
      return {
        ...column,
        cellRenderer: stageCellRenderer(stage.code, metricKey),
        field: stageField(stage.code, metricKey),
        onCellClicked: (
          event: CellClickedEvent<AgGridEvaluationRow, unknown>,
        ) => handleStageCellClick(event.data, stage, metricKey),
        spanRows: stageColumnSpanRows(stage.code, metricKey),
        tooltipValueGetter: ({ data }) =>
          data ? stageCellText(data, stage.code, metricKey) : '',
        valueFormatter: (
          params: ValueFormatterParams<AgGridEvaluationRow, unknown>,
        ) =>
          params.data ? stageCellText(params.data, stage.code, metricKey) : '-',
      };
    }),
    headerClass: 'aggrid-header-center',
    headerName: stage.label,
    marryChildren: true,
  };
}

function buildStageMetricColumns(
  stage: StageDefinition,
): StageMetricColumnDefinition[] {
  const columns: StageMetricColumnDefinition[] = [];

  if (stage.showBatch !== false) {
    columns.push(
      textColumn('batch', stage.batchLabel, 126, stage.code === 'ADHESIVE1'),
    );
  }

  for (const metric of stage.metricColumns || []) {
    columns.push(numberColumn(metric.field, metric.label, 92, true));
  }

  if (stage.code === 'SHIPPING_INSPECTION') {
    columns.push(
      numberColumn('inspectionQty', '送检', 92),
      numberColumn('inspectionNgQty', '检验NG', 92, false, alertCellClass),
      dateColumn('lastReportTime', stageLastReportTimeLabel(stage.code), true),
    );
    return columns;
  }

  if (METER_REPORT_STAGE_CODES.has(stage.code)) {
    columns.push(numberColumn('inputQty', '投入米数', 92));
  }

  if (PIECE_INPUT_STAGE_CODES.has(stage.code)) {
    columns.push(
      numberColumn('doneQty', stageDoneColumnLabel(stage.code), 148),
      numberColumn(
        'segmentDefectQty',
        '分段自检NG',
        148,
        false,
        alertCellClass,
      ),
      numberColumn('inspectionQty', stageInspectionColumnLabel(stage.code), 92),
      numberColumn(
        'inspectionNgQty',
        stageInspectionNgColumnLabel(stage.code),
        92,
        false,
        alertCellClass,
      ),
    );

    if (stage.code === 'ADHESIVE2') {
      columns.push(numberColumn('glueBoardInspectionQty', '胶板送检(m)', 104));
    }

    columns.push(
      numberColumn('pendingQty', '未加工', 92),
      numberColumn(
        'defectQty',
        stageDefectColumnLabel(stage.code),
        92,
        true,
        alertCellClass,
      ),
      numberColumn('segmentInputQty', '分段投入', 104, true),
      numberColumn('inputQty', '总投入', 104),
      numberColumn(
        'motherOutputQty',
        stageMotherOutputColumnLabel(stage.code),
        104,
      ),
      numberColumn('theoreticalOutputQty', '理论产量', 104),
      percentColumn('goodYieldRate', '良品率（%）', 112),
    );

    if (stage.code === 'CUT_ROUND') {
      columns.push(numberColumn('finalInspectionOutputQty', '终检产出', 104));
    }

    columns.push(
      dateColumn('lastReportTime', stageLastReportTimeLabel(stage.code), true),
    );
    return columns;
  }

  columns.push(
    numberColumn(
      'doneQty',
      stageDoneColumnLabel(stage.code),
      stage.code === 'WET' ? 148 : 92,
    ),
  );

  if (MOTHER_OUTPUT_STAGE_CODES.has(stage.code)) {
    columns.push(
      numberColumn(
        'motherOutputQty',
        stageMotherOutputColumnLabel(stage.code),
        104,
      ),
    );
  }

  if (THEORETICAL_OUTPUT_STAGE_CODES.has(stage.code)) {
    columns.push(
      numberColumn('theoreticalOutputQty', '理论产量', 104),
      percentColumn('goodYieldRate', '良品率（%）', 112),
    );
  }

  columns.push(
    numberColumn(
      'inspectionQty',
      stageInspectionColumnLabel(stage.code),
      stage.code === 'WET' || METER_REPORT_STAGE_CODES.has(stage.code)
        ? 148
        : 92,
      true,
    ),
    numberColumn(
      'inspectionNgQty',
      stageInspectionNgColumnLabel(stage.code),
      92,
      true,
      alertCellClass,
    ),
  );

  if (stage.code !== 'FORMULA') {
    columns.push(
      numberColumn(
        'defectQty',
        stageDefectColumnLabel(stage.code),
        stage.code === 'WET' || METER_REPORT_STAGE_CODES.has(stage.code)
          ? 148
          : 92,
        true,
        alertCellClass,
      ),
    );
  }

  columns.push(
    dateColumn('lastReportTime', stageLastReportTimeLabel(stage.code), true),
  );
  return columns;
}

function textColumn(
  field: StageMetricKey,
  headerName: string,
  minWidth: number,
  hide = false,
): StageMetricColumnDefinition {
  return {
    cellClass: 'aggrid-cell-center',
    field,
    headerClass: 'aggrid-header-center',
    headerName,
    hide,
    minWidth,
    suppressHeaderFilterButton: true,
    suppressHeaderMenuButton: true,
    valueFormatter: textFormatter,
  };
}

function numberColumn(
  field: StageMetricKey,
  headerName: string,
  minWidth: number,
  hide = false,
  cellClass?: ColDef<AgGridEvaluationRow>['cellClass'],
): StageMetricColumnDefinition {
  return {
    aggFunc: 'sum',
    cellClass: cellClass || 'aggrid-cell-center',
    field,
    filter: false,
    headerClass: 'aggrid-header-center',
    headerName,
    hide,
    minWidth,
    suppressHeaderFilterButton: true,
    suppressHeaderMenuButton: true,
    type: 'numericColumn',
    valueFormatter: decimalFormatter,
  };
}

function percentColumn(
  field: StageMetricKey,
  headerName: string,
  minWidth: number,
): StageMetricColumnDefinition {
  return {
    cellClass: yieldCellClass,
    field,
    filter: false,
    headerClass: 'aggrid-header-center',
    headerName,
    minWidth,
    suppressHeaderFilterButton: true,
    suppressHeaderMenuButton: true,
    type: 'numericColumn',
    valueFormatter: percentFormatter,
  };
}

function dateColumn(
  field: StageMetricKey,
  headerName: string,
  hide = false,
): StageMetricColumnDefinition {
  return {
    cellClass: 'aggrid-cell-center',
    field,
    headerClass: 'aggrid-header-center',
    headerName,
    hide,
    minWidth: 120,
    suppressHeaderFilterButton: true,
    suppressHeaderMenuButton: true,
    valueFormatter: dateFormatter,
  };
}

function stageDoneColumnLabel(stageCode: StageCode) {
  if (stageCode === 'WET') return '收卷米数(报工数)';
  if (METER_REPORT_STAGE_CODES.has(stageCode)) return '产出米数';
  if (PIECE_INPUT_STAGE_CODES.has(stageCode)) return '分段自检OK';
  return '完工';
}

function stageMotherOutputColumnLabel(stageCode: StageCode) {
  if (PIECE_INPUT_STAGE_CODES.has(stageCode)) return '总产出';
  return '母卷产出';
}

function stageInspectionColumnLabel(stageCode: StageCode) {
  if (stageCode === 'WET') return 'NAP层送检(米)';
  if (METER_REPORT_STAGE_CODES.has(stageCode)) return 'NAP留样米数/送检送数';
  return '送检';
}

function stageInspectionNgColumnLabel(stageCode: StageCode) {
  if (PIECE_INPUT_STAGE_CODES.has(stageCode)) return '送检NG';
  return '检验NG';
}

function stageDefectColumnLabel(stageCode: StageCode) {
  if (stageCode === 'WET' || METER_REPORT_STAGE_CODES.has(stageCode)) {
    return '固定损耗（米）';
  }
  if (PIECE_SELF_CHECK_STAGE_CODES.has(stageCode)) {
    return '自检总NG';
  }
  return 'NG';
}

function stageLastReportTimeLabel(stageCode: StageCode) {
  if (PIECE_SELF_CHECK_STAGE_CODES.has(stageCode)) return '扫码确认时间';
  if (stageCode === 'SHIPPING_INSPECTION') return '送检时间';
  return '完工时间';
}

function stageField(stageCode: StageCode, metricKey: StageMetricKey) {
  return `${stageCode}_${metricKey}`;
}

function flattenRow(
  row: MesQmsMotherRollGoodStatisticsApi.ProcessPivotRow,
  index: number,
): AgGridEvaluationRow {
  const result: AgGridEvaluationRow = {
    materialCode: row.materialCode,
    modelCode: modelText(row),
    motherRollBatchNo: motherBatchText(row),
    rowKey:
      row.pivotRowKey ||
      compactJoin([row.planNo, row.segmentBatchNo, row.id?.toString()], '_') ||
      `row_${index}`,
    segmentBatchNo: segmentText(row),
    sizeSpec: sizeText(row),
    sourceIndex: index,
  };

  for (const stage of STAGE_DEFINITIONS) {
    const stageData = stageOf(row, stage.code);
    result[stageField(stage.code, 'batch')] = stageBatchText(stageData);
    result[stageField(stage.code, 'doneQty')] = stageData.doneQty;
    result[stageField(stage.code, 'defectQty')] = stageData.defectQty;
    result[stageField(stage.code, 'finalInspectionOutputQty')] =
      stageData.finalInspectionOutputQty;
    result[stageField(stage.code, 'goodYieldRate')] = stageData.goodYieldRate;
    result[stageField(stage.code, 'glueBoardInspectionQty')] =
      stageData.glueBoardInspectionQty;
    result[stageField(stage.code, 'inputQty')] = stageData.inputQty;
    result[stageField(stage.code, 'inspectionNgQty')] =
      stageData.inspectionNgQty;
    result[stageField(stage.code, 'inspectionQty')] = stageData.inspectionQty;
    result[stageField(stage.code, 'lastReportTime')] = formatDateOnly(
      stageData.lastReportTime,
    );
    result[stageField(stage.code, 'motherOutputQty')] =
      stageData.motherOutputQty;
    result[stageField(stage.code, 'pendingQty')] = stageData.pendingQty;
    result[stageField(stage.code, 'processLength')] = stageData.processLength;
    result[stageField(stage.code, 'segmentDefectQty')] =
      stageData.segmentDefectQty;
    result[stageField(stage.code, 'segmentInputQty')] =
      stageData.segmentInputQty;
    result[stageField(stage.code, 'startPosition')] = stageData.startPosition;
    result[stageField(stage.code, 'theoreticalOutputQty')] =
      stageData.theoreticalOutputQty;
  }

  return result;
}

function baseColumnSpanRows(field: keyof AgGridEvaluationRow) {
  return (params: SpanRowsParams<AgGridEvaluationRow, unknown>) => {
    const left = sourcePivotRow(params.nodeA?.data);
    const right = sourcePivotRow(params.nodeB?.data);
    if (!left || !right) {
      return false;
    }

    return baseColumnMergeKey(left, field) === baseColumnMergeKey(right, field);
  };
}

function baseColumnMergeKey(
  row: MesQmsMotherRollGoodStatisticsApi.ProcessPivotRow,
  field: keyof AgGridEvaluationRow,
) {
  if (field === 'segmentBatchNo') {
    return segmentRowKey(row);
  }
  if (field === 'modelCode' || field === 'sizeSpec') {
    return modelSizeRowKey(row);
  }
  return planRowKey(row);
}

function stageColumnSpanRows(stageCode: StageCode, metricKey: StageMetricKey) {
  return (params: SpanRowsParams<AgGridEvaluationRow, unknown>) => {
    const left = sourcePivotRow(params.nodeA?.data);
    const right = sourcePivotRow(params.nodeB?.data);
    if (!left || !right) {
      return false;
    }
    return (
      stageCellRowKey(left, stageCode, metricKey) ===
      stageCellRowKey(right, stageCode, metricKey)
    );
  };
}

function sourcePivotRow(data?: AgGridEvaluationRow) {
  if (!data) {
    return undefined;
  }
  return rows.value[data.sourceIndex];
}

function normalizePivotRows(
  sourceRows: MesQmsMotherRollGoodStatisticsApi.ProcessPivotRow[],
) {
  return sourceRows
    .map((row, index) => ({
      index,
      row: normalizePivotRow(row),
    }))
    .toSorted(
      (left, right) =>
        comparePivotRows(left.row, right.row) || left.index - right.index,
    )
    .map((item) => item.row);
}

function normalizePivotRow(
  row: MesQmsMotherRollGoodStatisticsApi.ProcessPivotRow,
) {
  const motherBatchNo = motherBatchText(row);
  const segmentBatchNo = segmentText(row);
  return {
    ...row,
    motherRollBatchNo:
      motherBatchNo === '-' ? row.motherRollBatchNo : motherBatchNo,
    segmentBatchNo:
      segmentBatchNo === '-' ? row.segmentBatchNo : segmentBatchNo,
  };
}

function comparePivotRows(
  left: MesQmsMotherRollGoodStatisticsApi.ProcessPivotRow,
  right: MesQmsMotherRollGoodStatisticsApi.ProcessPivotRow,
) {
  return (
    motherGroupKey(left).localeCompare(motherGroupKey(right), 'zh-Hans-CN') ||
    segmentText(left).localeCompare(segmentText(right), 'zh-Hans-CN') ||
    String(left.pivotRowKey || '').localeCompare(
      String(right.pivotRowKey || ''),
      'zh-Hans-CN',
    )
  );
}

function motherGroupKey(
  row: MesQmsMotherRollGoodStatisticsApi.ProcessPivotRow,
) {
  return `${motherBatchText(row)}|${modelText(row)}`;
}

function planRowKey(row: MesQmsMotherRollGoodStatisticsApi.ProcessPivotRow) {
  return `MOTHER|${motherGroupKey(row)}`;
}

function segmentRowKey(row: MesQmsMotherRollGoodStatisticsApi.ProcessPivotRow) {
  return `MOTHER_SEGMENT|${motherGroupKey(row)}|${segmentText(row)}`;
}

function modelSizeRowKey(
  row: MesQmsMotherRollGoodStatisticsApi.ProcessPivotRow,
) {
  return `MOTHER_MODEL_SIZE|${motherGroupKey(row)}`;
}

function stageRowKey(
  row: MesQmsMotherRollGoodStatisticsApi.ProcessPivotRow,
  stageCode: StageCode,
) {
  if (stageCode === 'FORMULA' || stageCode === 'WET') {
    return `MOTHER_STAGE|${motherGroupKey(row)}|${stageCode}`;
  }
  return `MOTHER_STAGE|${segmentRowKey(row)}|${stageCode}`;
}

function stageCellRowKey(
  row: MesQmsMotherRollGoodStatisticsApi.ProcessPivotRow,
  stageCode: StageCode,
  columnKey: StageMetricKey,
) {
  const stage = stageOf(row, stageCode);
  if (columnKey === 'motherOutputQty') {
    return (
      stage.motherOutputMergeKey ||
      `MOTHER_OUTPUT|${motherGroupKey(row)}|${stageCode}`
    );
  }
  if (columnKey === 'inputQty' && PIECE_INPUT_STAGE_CODES.has(stageCode)) {
    return (
      stage.inputQtyMergeKey || `INPUT|${motherGroupKey(row)}|${stageCode}`
    );
  }
  if (columnKey === 'finalInspectionOutputQty') {
    return (
      stage.finalInspectionOutputMergeKey ||
      `FINAL_INSPECTION_OUTPUT|${motherGroupKey(row)}|${stageCode}`
    );
  }
  if (columnKey === 'theoreticalOutputQty') {
    return (
      stage.theoreticalOutputMergeKey ||
      `THEORY|${motherGroupKey(row)}|${stageCode}|${modelText(row)}`
    );
  }
  if (columnKey === 'goodYieldRate') {
    return (
      stage.goodYieldRateMergeKey ||
      `GOOD_YIELD|${motherGroupKey(row)}|${stageCode}|${modelText(row)}`
    );
  }
  if (columnKey === 'defectQty' && PIECE_INPUT_STAGE_CODES.has(stageCode)) {
    return `PIECE_SELF_CHECK_NG|${motherGroupKey(row)}|${stageCode}`;
  }
  return stageRowKey(row, stageCode);
}

function stageOf(
  row: MesQmsMotherRollGoodStatisticsApi.ProcessPivotRow,
  code: StageCode,
) {
  const stage = row.stages?.[code] || {};
  return {
    ...stage,
    stageCode: stage.stageCode || code,
  } as MesQmsMotherRollGoodStatisticsApi.ProcessPivotStage;
}

function stageBatchText(
  stage: MesQmsMotherRollGoodStatisticsApi.ProcessPivotStage,
) {
  return normalizeText(stage.outputBatchNos || stage.sourceBatchNos);
}

function normalizeText(value?: null | number | string) {
  const text = String(value ?? '').trim();
  return text || undefined;
}

function compactJoin(items: Array<string | undefined>, separator: string) {
  return items.filter(Boolean).join(separator);
}

function toNumber(value: unknown) {
  if (value === null || value === undefined || value === '') {
    return undefined;
  }
  const numberValue = Number(value);
  return Number.isFinite(numberValue) ? numberValue : undefined;
}

function formatNumber(value: unknown, digits = 3) {
  const numberValue = toNumber(value);
  if (numberValue === undefined) {
    return '-';
  }
  return numberValue.toLocaleString('zh-CN', {
    maximumFractionDigits: digits,
    minimumFractionDigits: 0,
  });
}

function decimalFormatter(
  params: ValueFormatterParams<AgGridEvaluationRow, unknown>,
) {
  return formatNumber(params.value, 3);
}

function percentFormatter(
  params: ValueFormatterParams<AgGridEvaluationRow, unknown>,
) {
  return formatPercentValue(params.value);
}

function dateFormatter(
  params: ValueFormatterParams<AgGridEvaluationRow, unknown>,
) {
  return formatDateOnly(params.value) || '-';
}

function textFormatter(
  params: ValueFormatterParams<AgGridEvaluationRow, unknown>,
) {
  return formatDisplayText(params.value);
}

function formatDisplayText(value: unknown) {
  if (value === null || value === undefined || value === '') {
    return '-';
  }
  if (Array.isArray(value)) {
    return value.map((item) => formatDisplayText(item)).join('、');
  }
  if (typeof value === 'object') {
    try {
      return JSON.stringify(value);
    } catch {
      return String(value);
    }
  }
  return String(value);
}

function formatDateOnly(value: unknown) {
  if (value === null || value === undefined || value === '') {
    return undefined;
  }

  if (Array.isArray(value)) {
    const [year, month, day] = value;
    const yearNumber = Number(year);
    const monthNumber = Number(month);
    const dayNumber = Number(day);
    if (
      Number.isFinite(yearNumber) &&
      Number.isFinite(monthNumber) &&
      Number.isFinite(dayNumber)
    ) {
      return `${String(yearNumber).padStart(4, '0')}-${String(monthNumber).padStart(2, '0')}-${String(dayNumber).padStart(2, '0')}`;
    }
    return formatDisplayText(value);
  }

  if (value instanceof Date) {
    return dayjs(value).format('YYYY-MM-DD');
  }

  const text = String(value).trim();
  const match = text.match(/^(\d{4})[-/年,.\s]+(\d{1,2})[-/月,.\s]+(\d{1,2})/);
  if (match) {
    return `${match[1]}-${match[2].padStart(2, '0')}-${match[3].padStart(2, '0')}`;
  }

  const parsed = dayjs(text);
  return parsed.isValid() ? parsed.format('YYYY-MM-DD') : text;
}

function formatPercentValue(value: unknown) {
  const text = formatNumber(value, 2);
  return text === '-' ? '-' : `${text}%`;
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

function stageCellText(
  data: AgGridEvaluationRow,
  stageCode: StageCode,
  metricKey: StageMetricKey,
) {
  const row = sourcePivotRow(data);
  if (!row) {
    return formatDisplayText(data[stageField(stageCode, metricKey)]);
  }
  const stage = stageOf(row, stageCode);
  if (metricKey === 'batch') return formatStageBatchText(stage, stageCode);
  if (metricKey === 'doneQty') return stageDoneText(stage);
  if (metricKey === 'inputQty') return stageInputText(stage);
  if (metricKey === 'segmentInputQty') return stageSegmentInputText(stage);
  if (metricKey === 'motherOutputQty') return stageMotherOutputText(stage);
  if (metricKey === 'finalInspectionOutputQty') {
    return stageFinalInspectionOutputText(stage);
  }
  if (metricKey === 'theoreticalOutputQty') {
    return stageTheoreticalOutputText(stage);
  }
  if (metricKey === 'goodYieldRate') return stageGoodYieldRateText(stage);
  if (metricKey === 'glueBoardInspectionQty') {
    return stageGlueBoardInspectionText(stage);
  }
  if (metricKey === 'inspectionQty') return stageInspectionText(stage);
  if (metricKey === 'inspectionNgQty') return stageInspectionNgText(stage);
  if (metricKey === 'pendingQty') return stagePendingText(stage);
  if (metricKey === 'segmentDefectQty') return stageSegmentDefectText(stage);
  if (metricKey === 'defectQty') {
    return stageDefectText(stageForCell(row, stageCode, metricKey));
  }
  if (metricKey === 'lastReportTime') {
    return formatDateTime(stage.lastReportTime);
  }
  if (metricKey === 'processLength' || metricKey === 'startPosition') {
    return formatNumber(stage[metricKey], 3);
  }
  return formatDisplayText(data[stageField(stageCode, metricKey)]);
}

function stageCellRenderer(stageCode: StageCode, metricKey: StageMetricKey) {
  return (params: ICellRendererParams<AgGridEvaluationRow, unknown>) => {
    const wrapper = document.createElement('span');
    wrapper.className = 'aggrid-stage-cell-content';

    const row = params.data ? sourcePivotRow(params.data) : undefined;
    if (metricKey === 'doneQty' && row) {
      const dot = document.createElement('span');
      dot.className = `stage-status-dot ${stageStatusClass(
        stageOf(row, stageCode).stageStatus,
      )}`;
      wrapper.append(dot);
    }

    const content = document.createElement(
      params.data && canOpenStageCell(params.data, stageCode, metricKey)
        ? 'button'
        : 'span',
    );
    content.textContent = params.data
      ? stageCellText(params.data, stageCode, metricKey)
      : formatDisplayText(params.value);
    if (content instanceof HTMLButtonElement) {
      content.type = 'button';
      content.className = stageCellLinkClass(metricKey);
    }
    wrapper.append(content);
    return wrapper;
  };
}

function stageCellLinkClass(metricKey: StageMetricKey) {
  if (metricKey === 'inspectionQty')
    return 'aggrid-qty-link aggrid-qty-link--inspection';
  if (metricKey === 'inspectionNgQty')
    return 'aggrid-qty-link aggrid-qty-link--inspection-ng';
  if (metricKey === 'pendingQty')
    return 'aggrid-qty-link aggrid-qty-link--pending';
  if (metricKey === 'defectQty' || metricKey === 'segmentDefectQty') {
    return 'aggrid-qty-link aggrid-qty-link--defect';
  }
  return 'aggrid-qty-link';
}

function canOpenStageCell(
  data: AgGridEvaluationRow,
  stageCode: StageCode,
  metricKey: StageMetricKey,
) {
  const row = sourcePivotRow(data);
  if (!row) {
    return false;
  }
  const stage = stageOf(row, stageCode);
  if (metricKey === 'doneQty') return canOpenPieceList(stageCode);
  if (metricKey === 'inspectionQty') return canOpenInspectionList(stage);
  if (metricKey === 'inspectionNgQty')
    return canOpenInspectionList(stage, true);
  if (metricKey === 'pendingQty') return canOpenPieceList(stageCode);
  if (metricKey === 'segmentDefectQty' || metricKey === 'defectQty') {
    return canOpenPieceList(stageCode);
  }
  return false;
}

function handleStageCellClick(
  data: AgGridEvaluationRow | undefined,
  stageMeta: StageDefinition,
  metricKey: StageMetricKey,
) {
  if (!data || !canOpenStageCell(data, stageMeta.code, metricKey)) {
    return;
  }
  const row = sourcePivotRow(data);
  if (!row) {
    return;
  }
  switch (metricKey) {
    case 'defectQty':
    case 'segmentDefectQty': {
      openPieceDialog(row, stageMeta, 'DEFECT', metricKey);

      break;
    }
    case 'doneQty': {
      openPieceDialog(row, stageMeta, 'DONE');

      break;
    }
    case 'inspectionNgQty': {
      openInspectionDialog(row, stageMeta, true);

      break;
    }
    case 'inspectionQty': {
      openInspectionDialog(row, stageMeta);

      break;
    }
    case 'pendingQty': {
      openPieceDialog(row, stageMeta, 'PENDING');

      break;
    }
    // No default
  }
}

function canOpenPieceList(stageCode: StageCode) {
  return stageCode !== 'SHIPPING_INSPECTION';
}

function stageForCell(
  row: MesQmsMotherRollGoodStatisticsApi.ProcessPivotRow,
  stageCode: StageCode,
  columnKey?: StageMetricKey,
) {
  if (!(columnKey === 'defectQty' && PIECE_INPUT_STAGE_CODES.has(stageCode))) {
    return stageOf(row, stageCode);
  }
  return mergePieceStagesForCell(row, stageCode, columnKey);
}

function mergePieceStagesForCell(
  row: MesQmsMotherRollGoodStatisticsApi.ProcessPivotRow,
  stageCode: StageCode,
  columnKey: StageMetricKey,
) {
  const mergeKey = stageCellRowKey(row, stageCode, columnKey);
  const relatedStages = rows.value
    .filter((item) => stageCellRowKey(item, stageCode, columnKey) === mergeKey)
    .map((item) => stageOf(item, stageCode));
  const base = { ...stageOf(row, stageCode) };
  const pieceMap = new Map<
    string,
    MesQmsMotherRollGoodStatisticsApi.ProcessPivotPiece
  >();
  const inspectionMap = new Map<
    string,
    MesQmsMotherRollGoodStatisticsApi.ProcessPivotInspection
  >();
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
      current.sourceBatchNo = firstText(
        current.sourceBatchNo,
        item.sourceBatchNo,
      );
      current.outputBatchNo = firstText(
        current.outputBatchNo,
        item.outputBatchNo,
      );
      current.remark = [current.remark, item.remark].filter(Boolean).join('；');
      current.defectFlag = current.defectFlag || item.defectFlag;
      current.reportConfirmed = current.reportConfirmed || item.reportConfirmed;
      if (String(item.status || '').toUpperCase() === 'DEFECT') {
        current.status = item.status;
      }
    });
    regularInspectionDetails(stage).forEach((item) => {
      const key = normalizeDrilldownKey(
        firstText(item.inspectionNo, item.productBatchNo),
      );
      if (key && !inspectionMap.has(key)) {
        inspectionMap.set(key, { ...item });
      }
    });
  });
  base.pieceDetails = [...pieceMap.values()];
  base.inspectionDetails = [...inspectionMap.values()];
  base.defectQty = base.pieceDetails.filter(
    (item) => String(item.status || '').toUpperCase() === 'DEFECT',
  ).length;
  return base;
}

function batchText(stage: MesQmsMotherRollGoodStatisticsApi.ProcessPivotStage) {
  return stage.outputBatchNos || stage.sourceBatchNos || '-';
}

function formatStageBatchText(
  stage: MesQmsMotherRollGoodStatisticsApi.ProcessPivotStage,
  stageCode: StageCode,
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

function stageDoneText(
  stage: MesQmsMotherRollGoodStatisticsApi.ProcessPivotStage,
) {
  return formatQty(stage.doneQty, stage.reportUnit);
}

function stageInputText(
  stage: MesQmsMotherRollGoodStatisticsApi.ProcessPivotStage,
) {
  return formatQty(stage.inputQty, stage.reportUnit);
}

function stageSegmentInputText(
  stage: MesQmsMotherRollGoodStatisticsApi.ProcessPivotStage,
) {
  return formatQty(stage.segmentInputQty, stage.reportUnit);
}

function stageMotherOutputText(
  stage: MesQmsMotherRollGoodStatisticsApi.ProcessPivotStage,
) {
  return formatQty(
    stage.motherOutputQty,
    stage.motherOutputUnit || stage.reportUnit,
  );
}

function stageFinalInspectionOutputText(
  stage: MesQmsMotherRollGoodStatisticsApi.ProcessPivotStage,
) {
  return formatQty(
    stage.finalInspectionOutputQty,
    stage.finalInspectionOutputUnit || '片',
  );
}

function stageTheoreticalOutputText(
  stage: MesQmsMotherRollGoodStatisticsApi.ProcessPivotStage,
) {
  if (!stage.theoreticalOutputMatched) {
    return '-';
  }
  return formatQty(stage.theoreticalOutputQty, theoreticalOutputUnit(stage));
}

function stageGoodYieldRateText(
  stage: MesQmsMotherRollGoodStatisticsApi.ProcessPivotStage,
) {
  return formatPercent(stage.goodYieldRate);
}

function theoreticalOutputUnit(
  stage: MesQmsMotherRollGoodStatisticsApi.ProcessPivotStage,
) {
  if (PENDING_STAGE_CODES.has(String(stage.stageCode || '') as StageCode)) {
    return '片';
  }
  return stage.theoreticalOutputUnit || stage.reportUnit;
}

function stagePendingText(
  stage: MesQmsMotherRollGoodStatisticsApi.ProcessPivotStage,
) {
  return formatQty(stage.pendingQty, stage.pendingUnit || stage.reportUnit);
}

function stageDefectText(
  stage: MesQmsMotherRollGoodStatisticsApi.ProcessPivotStage,
) {
  return formatQty(stage.defectQty, stage.reportUnit);
}

function stageSegmentDefectText(
  stage: MesQmsMotherRollGoodStatisticsApi.ProcessPivotStage,
) {
  return formatQty(stage.segmentDefectQty, stage.reportUnit);
}

function stageInspectionText(
  stage: MesQmsMotherRollGoodStatisticsApi.ProcessPivotStage,
) {
  return formatQty(stage.inspectionQty, stage.reportUnit);
}

function stageGlueBoardInspectionText(
  stage: MesQmsMotherRollGoodStatisticsApi.ProcessPivotStage,
) {
  return formatQty(stage.glueBoardInspectionQty, 'm');
}

function stageInspectionNgText(
  stage: MesQmsMotherRollGoodStatisticsApi.ProcessPivotStage,
) {
  return formatQty(stage.inspectionNgQty, stage.reportUnit);
}

function grindingOutputQty(
  stage: MesQmsMotherRollGoodStatisticsApi.ProcessPivotStage,
) {
  return Math.max(
    numberOrZero(stage.inputQty) -
      numberOrZero(stage.defectQty) -
      numberOrZero(stage.inspectionQty),
    0,
  );
}

function stageStatusClass(status?: string) {
  const normalized = String(status || 'NOT_STARTED').toUpperCase();
  return `stage-status--${normalized.toLowerCase().replaceAll('_', '-')}`;
}

function motherBatchText(
  row: MesQmsMotherRollGoodStatisticsApi.ProcessPivotRow,
) {
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
  return String(value || '')
    .trim()
    .toUpperCase();
}

function isMotherSegmentBatchNo(value?: string) {
  const text = normalizeBatchCode(value);
  if (text.length <= 1) return false;
  const lastChar = text.slice(-1);
  const previousChar = text.slice(-2, -1);
  return ['P', 'Q', 'R', 'S'].includes(lastChar) && previousChar === 'A';
}

function modelText(row: MesQmsMotherRollGoodStatisticsApi.ProcessPivotRow) {
  return (
    row.modelSeriesCode ||
    modelCodePrefix(row.actualModelCode || row.modelCode || row.modelName) ||
    '-'
  );
}

function modelCodePrefix(value?: string) {
  const text = String(value || '')
    .trim()
    .toUpperCase();
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

function formatDateTime(value: unknown) {
  return formatDateOnly(value) || '-';
}

function textOf(value: unknown) {
  if (value === null || value === undefined) {
    return '';
  }
  return String(value).trim();
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
    for (const match of text.matchAll(pattern)) {
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
  const target: Record<string, number> = {};
  DEFECT_CATEGORY_LABELS.forEach((item) => {
    target[item.key] = 0;
  });
  return target;
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
  const jsonLike =
    /"(?:visualItems|itemName|result|visualInspectionResult|inspectionResult)"/i.test(
      text,
    );
  if (!jsonLike) {
    DEFECT_CATEGORY_LABELS.forEach((item) => {
      const pattern = new RegExp(
        String.raw`${escapeRegExp(item.label)}\s*(?:[:：=xX×*]?\s*(\d+(?:\.\d+)?))?`,
        'gu',
      );
      for (const match of text.matchAll(pattern)) {
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
    for (const match of text.matchAll(pattern)) {
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
      } else if (
        (jsonLike && patternIndex >= 1) ||
        (!jsonLike && patternIndex >= 2)
      ) {
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
  if (fallbackToOther && !Object.values(counts).some((value) => value > 0)) {
    addDefectCount(counts, '其他');
  }
  const target: Record<string, number | string> = {};
  DEFECT_CATEGORY_LABELS.forEach((item) => {
    const value = counts[item.key] || 0;
    target[item.key] = value > 0 ? value : '-';
  });
  return target;
}

function escapeRegExp(value: string) {
  return value.replaceAll(/[.*+?^${}()|[\]\\]/g, String.raw`\$&`);
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

function inspectionIsNg(
  item: MesQmsMotherRollGoodStatisticsApi.ProcessPivotInspection,
) {
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

function regularInspectionDetails(
  stage: MesQmsMotherRollGoodStatisticsApi.ProcessPivotStage,
) {
  return (stage.inspectionDetails || []).filter(
    (item) => !isGlueBoardInspection(stage, item),
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
  return ngOnly
    ? details.some((item) => inspectionIsNg(item))
    : details.length > 0;
}

function inspectionStatusText(
  item: MesQmsMotherRollGoodStatisticsApi.ProcessPivotInspection,
) {
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
  if (kind === 'DEFECT') return 'NG';
  if (kind === 'INSPECTION') return '送检';
  if (kind === 'INSPECTION_NG') return '检验NG';
  return kind === 'DONE' ? '完工' : '待加工';
}

function drilldownKindLabel(
  stageCode: StageCode,
  kind: PieceDialogKind,
  columnKey?: StageMetricKey,
) {
  if (kind === 'DEFECT') {
    if (stageCode === 'WET' || METER_REPORT_STAGE_CODES.has(stageCode)) {
      return '固定损耗';
    }
    if (PIECE_SELF_CHECK_STAGE_CODES.has(stageCode)) {
      return columnKey === 'defectQty' ? '自检总NG' : '分段自检NG';
    }
    return 'NG';
  }
  if (kind === 'DONE') {
    if (stageCode === 'WET') return '收卷米数';
    if (METER_REPORT_STAGE_CODES.has(stageCode)) return '产出米数';
    if (PIECE_SELF_CHECK_STAGE_CODES.has(stageCode)) return '分段自检OK';
  }
  if (kind === 'INSPECTION_NG' && PIECE_SELF_CHECK_STAGE_CODES.has(stageCode)) {
    return '送检NG';
  }
  return pieceKindLabel(kind);
}

function pieceKindQtyText(
  stage: MesQmsMotherRollGoodStatisticsApi.ProcessPivotStage,
  kind: PieceDialogKind,
  columnKey?: StageMetricKey,
) {
  if (kind === 'DEFECT') {
    return columnKey === 'segmentDefectQty'
      ? stageSegmentDefectText(stage)
      : stageDefectText(stage);
  }
  if (kind === 'INSPECTION') return stageInspectionText(stage);
  if (kind === 'INSPECTION_NG') return stageInspectionNgText(stage);
  if (kind === 'DONE' && stage.stageCode === 'GRINDING') {
    return formatQty(grindingOutputQty(stage), 'm');
  }
  return kind === 'DONE' ? stageDoneText(stage) : stagePendingText(stage);
}

function isProcessDrilldownStage(stageCode?: string) {
  return ['ADHESIVE1', 'FORMULA', 'GRINDING', 'WET'].includes(
    String(stageCode || '').toUpperCase(),
  );
}

function drilldownColumns(stageCode: StageCode, kind: PieceDialogKind) {
  if (stageCode === 'FORMULA') return FORMULA_DRILLDOWN_COLUMNS;
  if (stageCode === 'WET') return WET_DRILLDOWN_COLUMNS;
  if (stageCode === 'GRINDING' || stageCode === 'ADHESIVE1') {
    return METER_REPORT_DRILLDOWN_COLUMNS;
  }
  if (kind === 'INSPECTION' || kind === 'INSPECTION_NG') {
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
    return [{ doneQtyText: stageDoneText(stage) }];
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
  return String(value || '')
    .trim()
    .toUpperCase();
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
  const values = inspections
    .map((item) => inspectionStatusText(item))
    .filter(Boolean);
  return values.length > 0 ? [...new Set(values)].join('、') : '-';
}

function inspectionDefectText(
  inspections: MesQmsMotherRollGoodStatisticsApi.ProcessPivotInspection[],
) {
  const values = inspections
    .map((item) =>
      firstText(item.defectSummary, summarizeRemarkText(item.remark)),
    )
    .filter(Boolean);
  return values.length > 0 ? [...new Set(values)].join('、') : '';
}

function firstInspectionFlagText(
  inspections: MesQmsMotherRollGoodStatisticsApi.ProcessPivotInspection[],
) {
  return inspections.some((item) => {
    const type =
      `${item.sourceType || ''} ${item.inspectionType || ''}`.toUpperCase();
    return type.includes('FAI') || type.includes('首');
  })
    ? '是'
    : '否';
}

function confirmStatusText(
  item: MesQmsMotherRollGoodStatisticsApi.ProcessPivotPiece,
  kind: PieceDialogKind,
) {
  if (item.defectFlag || kind === 'DEFECT') {
    return 'NG';
  }
  if (item.reportConfirmed || kind === 'DONE') {
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
    OUTPUT_BATCH_PREFERRED_STAGE_CODES.has(
      normalizeDrilldownKey(stageCode) as StageCode,
    ) &&
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
  return (stage.pieceDetails || [])
    .filter((item) => String(item.status || '').toUpperCase() === kind)
    .map((item) => {
      const inspections = matchedPieceInspections(stage, item);
      const inspectionNgType = inspectionDefectText(inspections);
      const ngType = firstText(
        summarizeRemarkText(item.remark),
        inspectionNgType,
      );
      return {
        coaFlagText: item.coaFlag ? '是' : '否',
        confirmStatus: confirmStatusText(item, kind),
        defectSummary: ngType,
        ...(kind === 'DEFECT'
          ? defectCountCells([item.remark || inspectionNgType], true)
          : {}),
        firstInspectionFlagText: firstInspectionFlagText(inspections),
        inspectionFlagText: inspections.length > 0 ? '是' : '否',
        inspectionResult: inspectionResultText(inspections),
        ngType: ngType || (kind === 'DEFECT' ? '自检NG' : '-'),
        outputBatchNo: item.outputBatchNo,
        pieceNo: drilldownPieceNoText(stage.stageCode || '', item, kind),
        remark: item.remark,
        scanConfirmTime: formatDateTime(item.lastReportTime),
        sourceBatchNo: item.sourceBatchNo,
        tracePieceNo: item.pieceNo,
        motherRollBatchNo: motherBatchText(row),
      };
    });
}

function buildInspectionDrilldownRows(
  stage: MesQmsMotherRollGoodStatisticsApi.ProcessPivotStage,
  ngOnly: boolean,
) {
  return regularInspectionDetails(stage)
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
      ngType: firstText(
        item.defectSummary,
        summarizeRemarkText(item.remark),
        '-',
      ),
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
  if (isProcessDrilldownStage(stage.stageCode)) {
    return buildProcessDrilldownRows(stage);
  }
  if (kind === 'INSPECTION' || kind === 'INSPECTION_NG') {
    return buildInspectionDrilldownRows(stage, kind === 'INSPECTION_NG');
  }
  return buildPieceDrilldownRows(row, stage, kind);
}

function drilldownTitle(
  stageMeta: StageDefinition,
  kind: PieceDialogKind,
  columnKey?: StageMetricKey,
) {
  const kindLabel = drilldownKindLabel(stageMeta.code, kind, columnKey);
  if (isProcessDrilldownStage(stageMeta.code)) {
    return `${stageMeta.label}${kindLabel}明细`;
  }
  if (kind === 'INSPECTION' || kind === 'INSPECTION_NG') {
    return `${stageMeta.label}${kindLabel}明细`;
  }
  return `${stageMeta.label}${kindLabel}片列表`;
}

function openPieceDialog(
  row: MesQmsMotherRollGoodStatisticsApi.ProcessPivotRow,
  stageMeta: StageDefinition,
  kind: PieceDialogKind,
  columnKey?: StageMetricKey,
) {
  if (!canOpenPieceList(stageMeta.code)) {
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
  stageMeta: StageDefinition,
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

function drilldownSubtitle(
  row: MesQmsMotherRollGoodStatisticsApi.ProcessPivotRow,
) {
  return `${motherBatchText(row)} / ${modelText(row)} / ${sizeText(row)}`;
}

function canOpenInspectionDetail(item: DrilldownTableRow) {
  return (
    !!textOf(item.sourceType) && Number.isFinite(Number(item.inspectionId))
  );
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
      ngOnly: pieceDialog.kind === 'INSPECTION_NG',
      sourceType: textOf(item.sourceType),
      title: `${textOf(item.inspectionResult) || '检验'}详情`,
    })
    .open();
}

function drilldownTagColor(kind: PieceDialogKind) {
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
    'is-center': column.align === 'center',
    'is-defect-column': column.defectColumn,
    'is-piece-no': column.key === 'pieceNo',
    'is-right': column.align === 'right',
  };
}

function isDefectCountValue(value: unknown) {
  const number = Number(value);
  return Number.isFinite(number) && number > 0;
}

function drilldownCellText(
  item: DrilldownTableRow,
  column: DrilldownTableColumn,
) {
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

function alertCellClass(params: CellClassParams<AgGridEvaluationRow>) {
  return Number(params.value || 0) > 0
    ? 'aggrid-cell-center aggrid-cell-alert'
    : 'aggrid-cell-center';
}

function yieldCellClass(params: CellClassParams<AgGridEvaluationRow>) {
  const value = toNumber(params.value);
  if (value === undefined) {
    return 'aggrid-cell-center';
  }
  if (value >= 98) {
    return 'aggrid-cell-center aggrid-cell-good';
  }
  if (value >= 95) {
    return 'aggrid-cell-center aggrid-cell-warn';
  }
  return 'aggrid-cell-center aggrid-cell-alert';
}

function buildQueryParams(): MesQmsMotherRollGoodStatisticsApi.ProcessPivotPageReqVO {
  const [productionStartDateStart, productionStartDateEnd] =
    query.planDateRange;
  return {
    pageNo: query.pageNo,
    pageSize: query.pageSize,
    productionStartDateEnd,
    productionStartDateStart,
  };
}

async function loadData() {
  loading.value = true;
  try {
    const result = await getMotherRollGoodStatisticsPage(buildQueryParams());
    rows.value = normalizePivotRows(result?.list || []);
    total.value = Number(result?.total || 0);
    await nextTick();
    scheduleGridLayoutRefresh();
  } catch (error) {
    console.error(error);
    message.error('加载母卷批次良品统计数据失败');
  } finally {
    loading.value = false;
  }
}

function handlePageChange(pageNo: number, pageSize: number) {
  query.pageNo = pageNo;
  query.pageSize = pageSize;
  loadData();
}

function handlePlanDateRangeChange(value?: string[]) {
  query.planDateRange =
    value && value[0] && value[1] ? [value[0], value[1]] : [];
  query.pageNo = 1;
  loadData();
}

function handleShiftPlanDateRange(offset: number) {
  const [startValue, endValue] =
    query.planDateRange.length === 2
      ? query.planDateRange
      : getDefaultPlanDateRange();
  const fallbackRange = getDefaultPlanDateRange();
  const start = dayjs(startValue).isValid()
    ? dayjs(startValue)
    : dayjs(fallbackRange[0]);
  const end = dayjs(endValue).isValid()
    ? dayjs(endValue)
    : dayjs(fallbackRange[1]);
  query.planDateRange = [
    start.add(offset, 'week').format('YYYY-MM-DD'),
    end.add(offset, 'week').format('YYYY-MM-DD'),
  ];
  query.pageNo = 1;
  loadData();
}

function getDefaultPlanDateRange() {
  const value = dayjs();
  const mondayOffset = (value.day() + 6) % 7;
  const start = value.subtract(mondayOffset, 'day').startOf('day');
  return [start.format('YYYY-MM-DD'), start.add(6, 'day').format('YYYY-MM-DD')];
}

function onGridReady(event: GridReadyEvent<AgGridEvaluationRow>) {
  gridApi.value = event.api;
  scheduleGridLayoutRefresh();
}

function getRowId(params: GetRowIdParams<AgGridEvaluationRow>) {
  return params.data.rowKey;
}

function expandAll() {
  gridApi.value?.expandAll();
}

function collapseAll() {
  gridApi.value?.collapseAll();
}

function autoSizeColumns() {
  const columns = gridApi.value?.getColumns() || [];
  gridApi.value?.autoSizeColumns(
    columns.map((column) => column.getId()),
    false,
  );
}

function scheduleGridLayoutRefresh() {
  const refresh = () => {
    if (!gridApi.value) {
      return;
    }
    gridApi.value.resetRowHeights();
    gridApi.value.sizeColumnsToFit();
    gridApi.value.redrawRows();
  };

  requestAnimationFrame(refresh);
  window.setTimeout(refresh, 100);
  window.setTimeout(refresh, 300);
}

function exportExcel() {
  if (!gridApi.value || gridRows.value.length === 0) {
    message.warning('当前没有可导出的表格数据');
    return;
  }
  gridApi.value.exportDataAsExcel({
    fileName: `母卷批次良品统计_Aggrid_${dayjs().format('YYYYMMDD_HHmmss')}.xlsx`,
    sheetName: 'AgGrid评估',
  });
}

onMounted(loadData);
</script>

<template>
  <Page
    auto-content-height
    class="aggrid-eval-page"
    content-class="aggrid-eval-content"
  >
    <div class="aggrid-eval-shell">
      <section class="aggrid-eval-header">
        <div class="aggrid-eval-title">
          <div>
            <h2>母卷批次良品统计（Aggrid）</h2>
            <div class="aggrid-eval-meta">
              <Tag color="blue">AG Grid Enterprise</Tag>
              <Tag color="green">独立评估页</Tag>
            </div>
          </div>
          <div class="aggrid-eval-actions">
            <Button @click="collapseAll">
              <template #icon>
                <IconifyIcon icon="lucide:fold-vertical" />
              </template>
              折叠
            </Button>
            <Button @click="expandAll">
              <template #icon>
                <IconifyIcon icon="lucide:unfold-vertical" />
              </template>
              展开
            </Button>
            <Button @click="autoSizeColumns">
              <template #icon>
                <IconifyIcon icon="lucide:columns-3" />
              </template>
              自适应列
            </Button>
            <Button type="primary" @click="exportExcel">
              <template #icon>
                <IconifyIcon icon="lucide:file-spreadsheet" />
              </template>
              导出 Excel
            </Button>
          </div>
        </div>
      </section>

      <section class="aggrid-eval-grid-panel">
        <div class="aggrid-eval-grid-toolbar">
          <div class="aggrid-eval-toolbar-left">
            <span class="aggrid-eval-toolbar-label">计划日期</span>
            <Button title="上一周" @click="handleShiftPlanDateRange(-1)">
              <template #icon>
                <IconifyIcon icon="lucide:chevron-left" />
              </template>
              上周
            </Button>
            <RangePicker
              v-model:value="query.planDateRange"
              allow-clear
              class="aggrid-eval-date-range"
              value-format="YYYY-MM-DD"
              @change="
                (_, dateStrings) =>
                  handlePlanDateRangeChange(dateStrings as string[])
              "
            />
            <Button title="下一周" @click="handleShiftPlanDateRange(1)">
              <template #icon>
                <IconifyIcon icon="lucide:chevron-right" />
              </template>
              下周
            </Button>
            <Input
              v-model:value="quickFilterText"
              allow-clear
              class="aggrid-eval-quick-filter"
              placeholder="当前页快速过滤"
            >
              <template #prefix>
                <IconifyIcon icon="lucide:list-filter" />
              </template>
            </Input>
          </div>
          <Pagination
            :current="query.pageNo"
            :page-size="query.pageSize"
            :page-size-options="['50', '100', '200', '500']"
            :show-total="(value: number) => `共 ${value} 条`"
            :total="total"
            show-quick-jumper
            show-size-changer
            size="small"
            @change="handlePageChange"
            @show-size-change="handlePageChange"
          />
        </div>

        <Spin class="aggrid-eval-spin" :spinning="loading">
          <div class="aggrid-eval-grid-host">
            <AgGridVue
              class="aggrid-eval-grid aggrid-eval-grid-v35"
              :auto-group-column-def="autoGroupColumnDef"
              :cell-selection="true"
              :column-defs="columnDefs"
              :default-col-def="defaultColDef"
              :enable-cell-span="true"
              :enable-charts="false"
              :get-row-id="getRowId"
              :group-default-expanded="1"
              :group-header-height="38"
              :header-height="38"
              :locale-text="localeText"
              :quick-filter-text="quickFilterText"
              :row-height="36"
              :row-data="gridRows"
              :row-selection="rowSelection"
              :side-bar="sideBar"
              :status-bar="statusBar"
              :suppress-drag-leave-hides-columns="true"
              :theme="gridTheme"
              @grid-ready="onGridReady"
            />
          </div>
        </Spin>
      </section>
    </div>

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
                  <Tag
                    v-else
                    :color="
                      drilldownInspectionResultColor(item.inspectionResult)
                    "
                  >
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
.aggrid-eval-page {
  min-height: 0;
  overflow: auto;
}

:global(.aggrid-eval-content) {
  box-sizing: border-box;
  display: flex;
  height: 100%;
  width: 100%;
  min-height: 0;
  min-width: 0;
  padding: 0 !important;
  overflow: auto !important;
}

.aggrid-eval-shell {
  display: grid;
  box-sizing: border-box;
  flex: 1 1 0;
  grid-template-rows: max-content minmax(560px, 1fr);
  width: 100%;
  height: auto;
  min-height: calc(100vh - 72px);
  min-width: 0;
  overflow: visible;
  background: #eef2f7;
}

.aggrid-eval-header {
  display: grid;
  gap: 12px;
  padding: 14px 16px 10px;
  background: #ffffff;
  border-bottom: 1px solid #dbe3ef;
}

.aggrid-eval-title {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  justify-content: space-between;
}

.aggrid-eval-title h2 {
  margin: 0;
  color: #172033;
  font-size: 20px;
  font-weight: 650;
  line-height: 1.28;
}

.aggrid-eval-meta,
.aggrid-eval-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.aggrid-eval-meta {
  margin-top: 6px;
}

.aggrid-eval-actions :deep(.ant-btn) {
  display: inline-flex;
  align-items: center;
}

.aggrid-eval-grid-panel {
  display: grid;
  height: auto;
  min-height: 560px;
  grid-template-rows: auto auto;
  overflow: visible;
  padding: 10px 16px 14px;
}

.aggrid-eval-grid-toolbar {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
  padding: 8px 0;
}

.aggrid-eval-toolbar-left {
  display: flex;
  flex: 1 1 auto;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  min-width: 0;
}

.aggrid-eval-toolbar-label {
  color: #475467;
  font-size: 12px;
  font-weight: 650;
  white-space: nowrap;
}

.aggrid-eval-date-range {
  width: 250px;
}

.aggrid-eval-quick-filter {
  width: min(360px, 42vw);
}

.aggrid-eval-grid-host {
  flex: 1 1 auto;
  height: calc(100vh - 210px);
  min-height: 560px;
  overflow: hidden;
  background: #ffffff;
  border: 1px solid #d8e0eb;
  border-radius: 8px;
}

.aggrid-eval-grid {
  width: 100%;
  height: 100%;
}

.aggrid-eval-grid-v35 {
  --ag-v35-grid-line-color: #d8e0eb;
  --ag-checkbox-checked-color: #1677ff;
  --ag-checkbox-unchecked-color: #98a2b3;
  --ag-icon-font-color: #334155;
  --ag-icon-font-display: block;
  --ag-icon-font-family: agGridAlpine, Microsoft YaHei, sans-serif;
  --ag-icon-image-display: none;
  --ag-icon-size: 14px;
}

.aggrid-eval-grid-v35 :deep(.ag-root-wrapper) {
  overflow: hidden;
  background: #ffffff;
  border: 0;
  border-radius: 6px;
}

.aggrid-eval-grid-v35 :deep(.ag-header) {
  background: #f5f7fb;
  border-bottom: 1px solid var(--ag-v35-grid-line-color);
}

.aggrid-eval-grid-v35 :deep(.ag-header-cell),
.aggrid-eval-grid-v35 :deep(.ag-header-group-cell) {
  background: #f5f7fb;
  border-right: 1px solid var(--ag-v35-grid-line-color) !important;
  border-bottom: 1px solid var(--ag-v35-grid-line-color) !important;
}

.aggrid-eval-grid-v35 :deep(.ag-cell) {
  border-right: 1px solid var(--ag-v35-grid-line-color) !important;
  border-bottom: 1px solid var(--ag-v35-grid-line-color) !important;
}

.aggrid-eval-grid-v35 :deep(.ag-icon),
.aggrid-eval-grid-v35 :deep(.ag-icon::before),
.aggrid-eval-grid-v35 :deep(.ag-checkbox-input-wrapper::after),
.aggrid-eval-grid-v35 :deep(.ag-radio-button-input-wrapper::after) {
  color: #334155;
  font-family:
    agGridAlpine,
    Microsoft YaHei,
    sans-serif !important;
  font-size: 14px;
  font-style: normal;
  font-weight: 400;
  line-height: 1;
}

.aggrid-eval-grid-v35 :deep(.ag-checkbox-input-wrapper.ag-checked::after) {
  color: #1677ff;
}

.aggrid-eval-grid-v35 :deep(.ag-icon::before) {
  content: '' !important;
}

.aggrid-eval-grid-v35 :deep(.ag-icon::after) {
  display: none !important;
}

.aggrid-eval-grid-v35 :deep(.ag-icon-menu::before),
.aggrid-eval-grid-v35 :deep(.ag-icon-menu-alt::before) {
  content: '⋮' !important;
}

.aggrid-eval-grid-v35 :deep(.ag-icon-filter::before) {
  content: '≡' !important;
}

.aggrid-eval-grid-v35 :deep(.ag-icon-columns::before) {
  content: '▦' !important;
}

.aggrid-eval-grid-v35 :deep(.ag-icon-tree-open::before),
.aggrid-eval-grid-v35 :deep(.ag-icon-expanded::before),
.aggrid-eval-grid-v35 :deep(.ag-icon-small-down::before),
.aggrid-eval-grid-v35 :deep(.ag-icon-down::before) {
  content: '⌄' !important;
}

.aggrid-eval-grid-v35 :deep(.ag-icon-tree-closed::before),
.aggrid-eval-grid-v35 :deep(.ag-icon-contracted::before),
.aggrid-eval-grid-v35 :deep(.ag-icon-small-right::before),
.aggrid-eval-grid-v35 :deep(.ag-icon-right::before) {
  content: '›' !important;
}

.aggrid-eval-grid-v35 :deep(.ag-icon-grip::before),
.aggrid-eval-grid-v35 :deep(.ag-icon-row-drag::before) {
  content: '⋮⋮' !important;
  letter-spacing: -2px;
}

.aggrid-eval-grid-v35 :deep(.ag-icon-checkbox-checked::before),
.aggrid-eval-grid-v35 :deep(.ag-icon-tick::before) {
  content: '✓' !important;
}

.aggrid-eval-grid-v35 :deep(.ag-icon-checkbox-indeterminate::before),
.aggrid-eval-grid-v35 :deep(.ag-icon-minus::before) {
  content: '−' !important;
}

.aggrid-eval-grid-v35 :deep(.ag-icon-checkbox-unchecked::before) {
  content: '' !important;
}

.aggrid-eval-grid-v35 :deep(.ag-icon-cross::before),
.aggrid-eval-grid-v35 :deep(.ag-icon-cancel::before) {
  content: '×' !important;
}

.aggrid-eval-grid-v35 :deep(.ag-spanned-cell-wrapper > .ag-spanned-cell) {
  display: flex;
  align-items: center;
  justify-content: center;
  background: #ffffff;
  box-shadow: inset 0 -1px 0 var(--ag-v35-grid-line-color);
}

.aggrid-eval-grid-v35
  :deep(.ag-spanned-cell-wrapper > .ag-spanned-cell.aggrid-cell-strong) {
  color: #1d4ed8;
  font-weight: 600;
}

.aggrid-eval-spin {
  display: block;
  height: auto;
  min-height: 560px;
}

.aggrid-eval-spin :deep(.ant-spin-container) {
  display: flex;
  height: auto;
  min-height: 560px;
}

:deep(.aggrid-cell-strong) {
  color: #1d4ed8;
  font-weight: 600;
}

:deep(.aggrid-header-center .ag-header-cell-label),
:deep(.aggrid-header-center .ag-header-group-cell-label) {
  width: 100%;
  height: 100%;
  align-items: center;
  justify-content: center;
  text-align: center;
}

:deep(.aggrid-header-center .ag-header-cell-text),
:deep(.aggrid-header-center .ag-header-group-text) {
  flex: 0 1 auto;
  text-align: center;
}

:deep(.ag-cell.aggrid-cell-center) {
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center !important;
}

:deep(.aggrid-stage-cell-content) {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  max-width: 100%;
  min-width: 0;
  line-height: 1.35;
  text-align: center;
}

:deep(.aggrid-qty-link) {
  max-width: 100%;
  padding: 0;
  overflow: hidden;
  color: #1677ff;
  font: inherit;
  line-height: inherit;
  text-overflow: ellipsis;
  white-space: nowrap;
  cursor: pointer;
  background: transparent;
  border: 0;
}

:deep(.aggrid-qty-link:hover) {
  color: #0958d9;
  text-decoration: underline;
}

:deep(.aggrid-qty-link--inspection) {
  color: #175cd3;
}

:deep(.aggrid-qty-link--inspection-ng),
:deep(.aggrid-qty-link--defect) {
  color: #b42318;
}

:deep(.aggrid-qty-link--pending) {
  color: #b54708;
}

:deep(.stage-status-dot) {
  display: inline-block;
  flex: 0 0 auto;
  width: 7px;
  height: 7px;
  margin-right: 4px;
  vertical-align: 1px;
  background: #cbd5e1;
  border-radius: 50%;
}

:deep(.stage-status--pending) {
  background: #f59e0b;
}

:deep(.stage-status--running) {
  background: #2563eb;
}

:deep(.stage-status--finished) {
  background: #16a34a;
}

:deep(.aggrid-cell-alert) {
  color: #b42318;
  font-weight: 650;
}

:deep(.aggrid-cell-good) {
  color: #057a55;
  font-weight: 650;
}

:deep(.aggrid-cell-warn) {
  color: #b54708;
  font-weight: 650;
}

:deep(.aggrid-row-alert .ag-cell:first-child) {
  box-shadow: inset 3px 0 0 #f04438;
}

:deep(.aggrid-status-cell) {
  font-weight: 600;
}

:deep(.aggrid-status-released) {
  color: #067647;
}

:deep(.aggrid-status-paused) {
  color: #b54708;
}

:deep(.aggrid-status-closed) {
  color: #175cd3;
}

:deep(.aggrid-status-canceled),
:deep(.aggrid-status-cancelled) {
  color: #b42318;
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
  color: #667085;
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
  border: 1px solid #d8e0eb;
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
  padding: 7px 8px;
  color: #344054;
  font-weight: 700;
  text-align: left;
  white-space: nowrap;
  background: #eef3f8;
  border: 1px solid #d7e1ec;
}

.drilldown-table td {
  padding: 7px 8px;
  color: #172033;
  line-height: 1.45;
  text-align: left;
  vertical-align: middle;
  word-break: break-word;
  border: 1px solid #dfe7f0;
}

.drilldown-table th.is-piece-no,
.drilldown-table td.is-piece-no {
  overflow-wrap: normal;
  word-break: keep-all;
  white-space: nowrap;
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
  padding: 28px 0;
  color: #667085;
  font-size: 12px;
  text-align: center;
}

@media (max-width: 900px) {
  .aggrid-eval-grid-panel {
    grid-template-rows: auto minmax(420px, 1fr);
  }

  .aggrid-eval-title,
  .aggrid-eval-grid-toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .aggrid-eval-toolbar-left {
    align-items: stretch;
  }

  .aggrid-eval-date-range {
    width: 100%;
  }

  .aggrid-eval-quick-filter {
    width: 100%;
  }
}
</style>
