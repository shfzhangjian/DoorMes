<script lang="ts" setup>
import type { TableColumnsType } from 'ant-design-vue';

import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { SrmQuarterlyPerformanceApi } from '#/api/mes/srm/assessment/quarterly-performance';
import type { SrmEvaluationTemplateApi } from '#/api/mes/srm/standard/template';
import type { SystemUserApi } from '#/api/system/user';

import { computed, onMounted, reactive, ref } from 'vue';

import { Page, useVbenModal } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import {
  Modal as AntModal,
  Button,
  Checkbox,
  Dropdown,
  Form,
  Input,
  InputNumber,
  Menu,
  message,
  Radio,
  Select,
  Space,
  Spin,
  Table,
  Tabs,
  Tag,
  Tooltip,
} from 'ant-design-vue';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  batchCreateQuarterlyPerformance,
  calculateQuarterlyTotal,
  createQuarterlyPerformance,
  deleteQuarterlyPerformance,
  getQuarterlyPerformance,
  getQuarterlyPerformanceAvailableSuppliers,
  getQuarterlyPerformancePage,
  getQuarterlyPerformanceTrend,
  pullQuarterlyActuals,
  sendQuarterlyScoring,
  startQuarterlySign,
  submitQuarterlyScore,
  submitQuarterlySign,
  updateQuarterlyPerformance,
} from '#/api/mes/srm/assessment/quarterly-performance';
import { getPublishedTemplateList } from '#/api/mes/srm/standard/template';
import { UserSelectModal } from '#/views/system/user/components';

import TrendPanel from '../../performance/trend-analysis/components/TrendPanel.vue';
import SrmReferenceSelectModal from '../../shared/SrmReferenceSelectModal.vue';

import '../../shared/srm-erp-detail.css';

defineOptions({ name: 'SrmAssessmentQuarterlyPerformance' });

type DetailMode = 'create' | 'detail' | 'edit';
type DetailBottomTab = 'trace' | 'trend';

type EvaluationForm = SrmQuarterlyPerformanceApi.Evaluation & {
  items: SrmQuarterlyPerformanceApi.Item[];
  logs: SrmQuarterlyPerformanceApi.Log[];
  signs: SrmQuarterlyPerformanceApi.Sign[];
  traces: SrmQuarterlyPerformanceApi.Trace[];
};

const SRM_NESTED_MODAL_Z_INDEX = 5600;
const SRM_NESTED_DROPDOWN_Z_INDEX = SRM_NESTED_MODAL_Z_INDEX + 10;

const detailMode = ref<DetailMode>('detail');
const saving = ref(false);
const basicExpanded = ref(true);
const publishedTemplates = ref<SrmEvaluationTemplateApi.Template[]>([]);
const checkedIds = ref<number[]>([]);
const batchOperating = ref(false);
const generateModalOpen = ref(false);
const generateLoading = ref(false);
const availableSupplierLoading = ref(false);
const availableSuppliers = ref<SrmQuarterlyPerformanceApi.AvailableSupplier[]>(
  [],
);
const selectedGenerateSupplierIds = ref<number[]>([]);
const selectedEvaluation = ref<EvaluationForm | null>(null);
const selectedEvaluationLoading = ref(false);
const detailPanelOpen = ref(false);
const cardLoading = ref(false);
const cardRows = ref<SrmQuarterlyPerformanceApi.Evaluation[]>([]);
const cardPageNo = ref(1);
const cardPageSize = ref(20);
const cardTotal = ref(0);
const logModalOpen = ref(false);
const trendModalOpen = ref(false);
const trendLoading = ref(false);
const trendData = ref<SrmQuarterlyPerformanceApi.Trend>(createEmptyTrendData());
const detailBottomActiveKey = ref<DetailBottomTab>('trace');
const selectedDetailItemId = ref<number>();
const detailTrendLoading = ref(false);
const detailTrendData = ref<SrmQuarterlyPerformanceApi.Trend>(
  createEmptyTrendData(),
);
const detailTrendRequestKey = ref('');
const signOpinionOpen = ref(false);
const signResult = ref<'FAIL' | 'PASS'>('PASS');
const signOpinion = ref('');
const generateForm = reactive<{
  evalQuarter?: number;
  evalYear?: number;
  templateVersionId?: number;
}>(createEmptyForm());
const generateFilter = reactive<{
  level?: string;
  supplierInfo?: string;
}>({});
const queryForm = reactive<{
  evalQuarter?: number;
  evalYear?: number;
  status?: string;
  supplierInfo?: string;
  todoOnly?: boolean;
}>({
  ...createCurrentPeriod(),
  todoOnly: false,
});
const form = reactive<EvaluationForm>(createEmptyForm());
const QUERY_BATCH_PAGE_SIZE = 200;

type BatchRowsResult = {
  cancelled: boolean;
  rows: SrmQuarterlyPerformanceApi.Evaluation[];
};

const isReadonly = computed(() => detailMode.value === 'detail');
const canEditBasic = computed(
  () =>
    !isReadonly.value &&
    (!form.id ||
      form.canMaintain === true ||
      ['DRAFT', 'PENDING_DATA'].includes(form.status || '')),
);
const templateOptions = computed(() =>
  publishedTemplates.value
    .map((template) => {
      const version = template.currentVersion;
      const versionId = normalizeId(template.currentVersionId || version?.id);
      if (!versionId) {
        return undefined;
      }
      return {
        label: [
          template.templateCode,
          template.templateName,
          template.currentVersionNo || version?.versionNo,
        ]
          .filter(Boolean)
          .join(' / '),
        template,
        value: versionId,
      };
    })
    .filter(
      (
        item,
      ): item is {
        label: string;
        template: SrmEvaluationTemplateApi.Template;
        value: number;
      } => !!item,
    ),
);
const selectedTemplateVersionId = computed({
  get: () => normalizeId(form.templateVersionId),
  set: (value?: number) => {
    form.templateVersionId = value;
    applySelectedTemplateSnapshot(value);
  },
});
const selectedTemplateLabel = computed(() => templateText(form));
const myScoreItems = computed(() =>
  (form.items || []).filter(
    (item) =>
      item.currentUserItem &&
      ['MANUAL_SCORE', 'MIXED'].includes(item.indicatorTypeSnapshot || '') &&
      item.scoreStatus !== 'COMPLETED',
  ),
);
const currentTotalScoreText = computed(() => {
  if (
    form.totalScoreDisplay !== undefined &&
    form.totalScoreDisplay !== null &&
    form.totalScoreDisplay !== ''
  ) {
    return displayValue(form.totalScoreDisplay);
  }
  if (form.totalScore !== undefined && form.totalScore !== null) {
    return displayValue(form.totalScore);
  }
  return '';
});
const subtitleItems = computed(() =>
  [
    form.supplierName || '',
    form.evalYear && form.evalQuarter
      ? `${form.evalYear} Q${form.evalQuarter}`
      : '',
    currentTotalScoreText.value ? `总分 ${currentTotalScoreText.value}` : '',
    form.evalGrade ? `等级 ${form.evalGrade}` : '',
    form.id ? `红线 ${form.redlineTriggered ? '触发' : '正常'}` : '',
    statusText(form.status),
  ].filter(Boolean),
);
const basicSummaryItems = computed(() => [
  { label: '评价供应商', value: form.supplierName || '-' },
  { label: '年度', value: form.evalYear || '-' },
  { label: '季度', value: form.evalQuarter ? `Q${form.evalQuarter}` : '-' },
  {
    label: '总分',
    value: displayValue(form.totalScoreDisplay ?? form.totalScore),
  },
  { label: '等级', value: form.evalGrade || '-' },
  { label: '红线', value: form.redlineTriggered ? '触发' : '正常' },
]);
const selectedDetailItem = computed(() =>
  (form.items || []).find(
    (item) => normalizeId(item.id) === selectedDetailItemId.value,
  ),
);
const selectedDetailItemTitle = computed(() => {
  const item = selectedDetailItem.value;
  if (!item) {
    return '全部指标';
  }
  return [item.groupNameSnapshot, item.indicatorNameSnapshot]
    .filter(Boolean)
    .join(' / ');
});
const filteredTraces = computed(() => {
  const itemId = selectedDetailItemId.value;
  if (!itemId) {
    return form.traces || [];
  }
  return (form.traces || []).filter(
    (trace) => normalizeId(trace.evaluationItemId) === itemId,
  );
});
const detailTrendPanels = computed(() => detailTrendData.value.panels || []);
const selectedEvaluationTitle = computed(() => {
  const record = selectedEvaluation.value;
  if (!record) {
    return '选中供应商指标明细';
  }
  return [
    record.supplierName,
    record.evalYear && record.evalQuarter
      ? `${record.evalYear} Q${record.evalQuarter}`
      : '',
  ]
    .filter(Boolean)
    .join(' · ');
});
const panelToggleText = computed(() =>
  detailPanelOpen.value ? '收起右侧' : '展开右侧',
);
const cardTotalPage = computed(() =>
  Math.max(1, Math.ceil(cardTotal.value / cardPageSize.value)),
);
const generateTemplateLabel = computed(() => {
  const option = templateOptions.value.find(
    (item) => item.value === generateForm.templateVersionId,
  );
  return option?.label || '-';
});
const generateSupplierRowSelection = computed(() => ({
  onChange: (keys: Array<number | string>) => {
    selectedGenerateSupplierIds.value = keys.map(Number);
  },
  selectedRowKeys: selectedGenerateSupplierIds.value,
}));
const levelOptions = [
  { label: 'A级', value: 'A' },
  { label: 'B级', value: 'B' },
  { label: 'C级', value: 'C' },
  { label: 'D级', value: 'D' },
];

const gridColumns: VxeTableGridOptions['columns'] = [
  {
    align: 'center',
    fixed: 'left',
    title: '',
    type: 'checkbox',
    width: 42,
  },
  { field: 'supplierCode', minWidth: 140, title: '供应商编号' },
  {
    field: 'supplierName',
    minWidth: 220,
    slots: { default: 'supplierName' },
    title: '供应商名称',
  },
  { align: 'center', field: 'evalYear', width: 90, title: '年度' },
  {
    align: 'center',
    field: 'evalQuarter',
    width: 90,
    slots: { default: 'quarter' },
    title: '季度',
  },
  {
    align: 'center',
    field: 'status',
    minWidth: 130,
    slots: { default: 'status' },
    title: '状态',
  },
  {
    align: 'center',
    field: 'totalScore',
    width: 90,
    slots: { default: 'totalScore' },
    title: '总分',
  },
  {
    align: 'center',
    field: 'evalGrade',
    width: 90,
    slots: { default: 'evalGrade' },
    title: '等级',
  },
  {
    align: 'center',
    field: 'redlineTriggered',
    width: 100,
    slots: { default: 'redline' },
    title: '红线',
  },
  { align: 'center', field: 'updateTime', minWidth: 170, title: '更新时间' },
  {
    align: 'center',
    field: 'rowActions',
    fixed: 'right',
    slots: { default: 'rowActions' },
    title: '操作',
    width: 72,
  },
];

const itemColumns: TableColumnsType = [
  {
    align: 'center',
    dataIndex: 'seq',
    fixed: 'left',
    title: '序号',
    width: 70,
  },
  { dataIndex: 'groupNameSnapshot', fixed: 'left', title: '维度', width: 120 },
  {
    dataIndex: 'indicatorNameSnapshot',
    fixed: 'left',
    title: '评估指标',
    width: 220,
  },
  { dataIndex: 'indicatorTypeSnapshot', title: '类型', width: 130 },
  { dataIndex: 'scoringRuleSnapshot', title: '评分规则', width: 320 },
  { align: 'center', dataIndex: 'maxScoreSnapshot', title: '满分', width: 80 },
  {
    align: 'center',
    dataIndex: 'targetValueSnapshot',
    title: '目标',
    width: 90,
  },
  {
    align: 'center',
    dataIndex: 'redlineScoreSnapshot',
    title: '红线分',
    width: 90,
  },
  { dataIndex: 'reporterUserName', title: '实际上报人', width: 140 },
  { dataIndex: 'calcActualValue', title: '实际计算值', width: 120 },
  { dataIndex: 'calcScore', title: '自动分', width: 100 },
  { dataIndex: 'manualScore', title: '人工分', width: 120 },
  { dataIndex: 'finalScore', title: '最终分', width: 100 },
  { dataIndex: 'scorerUserNameDisplay', title: '评分人', width: 150 },
  { align: 'center', dataIndex: 'dataStatus', title: '数据状态', width: 110 },
  { align: 'center', dataIndex: 'scoreStatus', title: '评分状态', width: 110 },
  { dataIndex: 'scoringDescription', title: '评分说明', width: 260 },
];

const previewItemColumns: TableColumnsType = [
  { align: 'center', dataIndex: 'seq', title: '序号', width: 70 },
  { dataIndex: 'groupNameSnapshot', title: '维度', width: 130 },
  { dataIndex: 'indicatorNameSnapshot', title: '评估指标', width: 220 },
  {
    align: 'center',
    dataIndex: 'targetValueSnapshot',
    title: '目标',
    width: 110,
  },
  {
    align: 'center',
    dataIndex: 'calcActualValue',
    title: '实际计算值',
    width: 120,
  },
  { align: 'center', dataIndex: 'finalScore', title: '最终分', width: 100 },
  { dataIndex: 'scorerUserNameDisplay', title: '评分人', width: 150 },
];

const generateSupplierColumns: TableColumnsType = [
  { dataIndex: 'supplierCode', title: '供应商编号', width: 180 },
  { dataIndex: 'supplierName', title: '供应商名称', width: 320 },
  { align: 'center', dataIndex: 'level', title: '评定等级', width: 120 },
];

const traceColumns: TableColumnsType = [
  { dataIndex: 'displayName', title: '节点/指标', width: 220 },
  { dataIndex: 'sourceMetricName', title: '来源指标名称', width: 220 },
  { align: 'center', dataIndex: 'periodMonth', title: '月份', width: 80 },
  { dataIndex: 'rawValue', title: '原始值', width: 100 },
  { dataIndex: 'normalizedValue', title: '换算值/得分', width: 120 },
  { dataIndex: 'unit', title: '单位', width: 80 },
  { dataIndex: 'reporterUserName', title: '上报人', width: 120 },
  { align: 'center', dataIndex: 'attachmentCount', title: '附件', width: 80 },
  { dataIndex: 'resultMessage', title: '说明', width: 220 },
];

const signColumns: TableColumnsType = [
  { dataIndex: 'deptName', title: '会签部门', width: 150 },
  { dataIndex: 'userName', title: '会签人', width: 140 },
  { align: 'center', dataIndex: 'signStatus', title: '状态', width: 110 },
  { align: 'center', dataIndex: 'signResult', title: '结果', width: 110 },
  { dataIndex: 'signOpinion', title: '意见', width: 300 },
  { dataIndex: 'signTime', title: '会签时间', width: 170 },
];

const logColumns: TableColumnsType = [
  { dataIndex: 'action', title: '动作', width: 150 },
  { dataIndex: 'operatorName', title: '操作人', width: 130 },
  { dataIndex: 'actionDescription', title: '说明', width: 300 },
  { dataIndex: 'fromStatus', title: '原状态', width: 130 },
  { dataIndex: 'toStatus', title: '新状态', width: 130 },
  { dataIndex: 'createTime', title: '时间', width: 170 },
];

const itemTableScrollX = computed(() =>
  itemColumns.reduce((sum, column) => sum + Number(column.width || 120), 0),
);
const traceTableScrollX = computed(() =>
  traceColumns.reduce((sum, column) => sum + Number(column.width || 120), 0),
);

const [Grid, gridApi] = useVbenVxeGrid({
  gridOptions: {
    checkboxConfig: { highlight: true },
    columns: gridColumns,
    height: 'auto',
    keepSource: true,
    pagerConfig: { enabled: true, pageSize: 20 },
    rowConfig: { height: 46, isCurrent: true, isHover: true, keyField: 'id' },
    showOverflow: true,
    proxyConfig: {
      ajax: {
        query: async ({ page }) => {
          const pageNo = page?.currentPage || 1;
          const pageSize = page?.pageSize || 20;
          const result = await getQuarterlyPerformancePage({
            ...buildQueryParams(),
            pageNo,
            pageSize,
          });
          const list = result.list || [];
          const total = Number(result.total || 0);
          cardRows.value = list;
          cardTotal.value = total;
          cardPageNo.value = pageNo;
          cardPageSize.value = pageSize;
          return {
            list,
            total,
          };
        },
      },
    },
  } as VxeTableGridOptions<SrmQuarterlyPerformanceApi.Evaluation>,
  gridEvents: {
    cellClick: handleGridCellClick,
    checkboxAll: handleGridCheckboxChange,
    checkboxChange: handleGridCheckboxChange,
  },
});

const [DetailModal, detailModalApi] = useVbenModal({
  class: 'qms-product-event-detail-modal srm-erp-crud-modal',
  closeOnClickModal: false,
  closable: false,
  contentClass: '!p-0 overflow-hidden',
  footer: false,
  fullscreen: true,
  fullscreenButton: false,
  header: false,
  showCancelButton: false,
  showConfirmButton: false,
});

const [SupplierModal, supplierModalApi] = useVbenModal({
  connectedComponent: SrmReferenceSelectModal,
  destroyOnClose: true,
});

const [UserModal, userModalApi] = useVbenModal({
  connectedComponent: UserSelectModal,
  destroyOnClose: true,
});

function createCurrentPeriod() {
  const now = new Date();
  const month = now.getMonth() + 1;
  return {
    evalQuarter: Math.ceil(month / 3),
    evalYear: now.getFullYear(),
  };
}

function createEmptyForm(): EvaluationForm {
  const currentPeriod = createCurrentPeriod();
  return {
    evalQuarter: currentPeriod.evalQuarter,
    evalYear: currentPeriod.evalYear,
    items: [],
    logs: [],
    signs: [],
    status: 'DRAFT',
    traces: [],
  };
}

function createEmptyTrendData(): SrmQuarterlyPerformanceApi.Trend {
  return {
    panels: [],
    selectedIndicatorCodes: [],
    tree: [],
  };
}

function resetForm(record: Partial<EvaluationForm> = {}) {
  const previousItemId = selectedDetailItemId.value;
  Object.keys(form).forEach((key) => delete (form as Record<string, any>)[key]);
  Object.assign(form, toEvaluationForm(record));
  syncSelectedDetailItem(previousItemId);
  resetDetailTrendData();
}

function toEvaluationForm(
  record: Partial<EvaluationForm> = {},
): EvaluationForm {
  return {
    ...createEmptyForm(),
    ...record,
    items: (record.items || []).map((item) => ({ ...item })),
    logs: record.logs || [],
    signs: record.signs || [],
    traces: record.traces || [],
  };
}

function syncSelectedDetailItem(preferredItemId?: number) {
  const items = form.items || [];
  const preferredItem = items.find(
    (item) => normalizeId(item.id) === preferredItemId,
  );
  const firstItem = preferredItem || items[0];
  selectedDetailItemId.value = normalizeId(firstItem?.id);
}

function resetDetailTrendData() {
  detailTrendData.value = createEmptyTrendData();
  detailTrendRequestKey.value = '';
}

function buildQueryParams() {
  const params: Record<string, unknown> = {};
  const supplierInfo = queryForm.supplierInfo?.trim();
  const evalYear = normalizeNumber(queryForm.evalYear);
  const evalQuarter = normalizeNumber(queryForm.evalQuarter);
  if (supplierInfo) {
    params.supplierInfo = supplierInfo;
  }
  if (evalYear) {
    params.evalYear = evalYear;
  }
  if (evalQuarter) {
    params.evalQuarter = evalQuarter;
  }
  if (queryForm.status) {
    params.status = queryForm.status;
  }
  if (queryForm.todoOnly) {
    params.todoOnly = true;
  }
  return params;
}

function buildListQueryParams(pageNo: number, pageSize: number) {
  return {
    ...buildQueryParams(),
    pageNo,
    pageSize,
  };
}

async function queryCardList(pageNo = cardPageNo.value) {
  cardLoading.value = true;
  try {
    const result = await getQuarterlyPerformancePage(
      buildListQueryParams(pageNo, cardPageSize.value),
    );
    cardRows.value = result.list || [];
    cardTotal.value = Number(result.total || 0);
    cardPageNo.value = pageNo;
  } finally {
    cardLoading.value = false;
  }
}

async function refreshList() {
  await (detailPanelOpen.value
    ? queryCardList(cardPageNo.value)
    : gridApi.query());
}

async function handleQuery() {
  checkedIds.value = [];
  clearSelectedEvaluation();
  cardPageNo.value = 1;
  await refreshList();
}

async function resetQueryForm() {
  const currentPeriod = createCurrentPeriod();
  Object.assign(queryForm, {
    evalQuarter: currentPeriod.evalQuarter,
    evalYear: currentPeriod.evalYear,
    status: undefined,
    supplierInfo: undefined,
    todoOnly: false,
  });
  checkedIds.value = [];
  clearSelectedEvaluation();
  cardPageNo.value = 1;
  await refreshList();
}

async function toggleDetailPanel() {
  detailPanelOpen.value = !detailPanelOpen.value;
  checkedIds.value = [];
  await (gridApi.grid as any)?.clearCheckboxRow?.();
  if (!detailPanelOpen.value) {
    clearSelectedEvaluation();
  }
  cardPageNo.value = 1;
  await refreshList();
}

function isCardChecked(row: SrmQuarterlyPerformanceApi.Evaluation) {
  const id = normalizeId(row.id);
  return !!id && checkedIds.value.includes(id);
}

function selectedRowsForAction() {
  if (!detailPanelOpen.value) {
    return ((gridApi.grid as any)?.getCheckboxRecords?.() ||
      []) as SrmQuarterlyPerformanceApi.Evaluation[];
  }
  const idSet = new Set(checkedIds.value);
  return cardRows.value.filter((row) => {
    const id = normalizeId(row.id);
    return !!id && idSet.has(id);
  });
}

function confirmQueryScopeBatch(label: string, total: number) {
  return new Promise<boolean>((resolve) => {
    AntModal.confirm({
      cancelText: '取消',
      content: `当前未勾选季度评价单，将按当前查询条件对全部 ${total} 张供应商季度评价单执行“批量${label}”。请确认筛选条件无误后继续。`,
      okText: '继续执行',
      onCancel: () => resolve(false),
      onOk: () => resolve(true),
      title: '按查询条件批量操作',
    });
  });
}

async function queryRowsForBatch(label: string): Promise<BatchRowsResult> {
  const params = buildQueryParams();
  const firstPage = await getQuarterlyPerformancePage({
    ...params,
    pageNo: 1,
    pageSize: QUERY_BATCH_PAGE_SIZE,
  });
  const total = Number(firstPage.total || 0);
  if (total === 0) {
    message.warning('当前查询条件下没有可操作的供应商季度评价单');
    return { cancelled: true, rows: [] };
  }
  const confirmed = await confirmQueryScopeBatch(label, total);
  if (!confirmed) {
    return { cancelled: true, rows: [] };
  }
  const rows: SrmQuarterlyPerformanceApi.Evaluation[] = [
    ...(firstPage.list || []),
  ];
  const totalPages = Math.ceil(total / QUERY_BATCH_PAGE_SIZE);
  for (let pageNo = 2; pageNo <= totalPages; pageNo += 1) {
    const page = await getQuarterlyPerformancePage({
      ...params,
      pageNo,
      pageSize: QUERY_BATCH_PAGE_SIZE,
    });
    rows.push(...(page.list || []));
  }
  return { cancelled: false, rows };
}

async function resolveRowsForBatch(label: string): Promise<BatchRowsResult> {
  const rows = selectedRowsForAction();
  if (rows.length > 0) {
    return { cancelled: false, rows };
  }
  return queryRowsForBatch(label);
}

function toggleCardSelection(
  row: SrmQuarterlyPerformanceApi.Evaluation,
  event: { target?: { checked?: boolean } },
) {
  const id = normalizeId(row.id);
  if (!id) {
    return;
  }
  const checked = !!event.target?.checked;
  if (checked && !checkedIds.value.includes(id)) {
    checkedIds.value = [...checkedIds.value, id];
  }
  if (!checked) {
    checkedIds.value = checkedIds.value.filter((item) => item !== id);
  }
}

async function handleCardPageChange(direction: 'next' | 'prev') {
  const nextPage =
    direction === 'next' ? cardPageNo.value + 1 : cardPageNo.value - 1;
  if (nextPage < 1 || nextPage > cardTotalPage.value) {
    return;
  }
  checkedIds.value = [];
  await queryCardList(nextPage);
}

function shiftPeriod(
  target: { evalQuarter?: number; evalYear?: number },
  step: -1 | 1,
) {
  const currentPeriod = createCurrentPeriod();
  let evalYear = normalizeNumber(target.evalYear) || currentPeriod.evalYear;
  let evalQuarter =
    normalizeNumber(target.evalQuarter) || currentPeriod.evalQuarter;
  evalQuarter += step;
  if (evalQuarter < 1) {
    evalQuarter = 4;
    evalYear -= 1;
  }
  if (evalQuarter > 4) {
    evalQuarter = 1;
    evalYear += 1;
  }
  target.evalYear = evalYear;
  target.evalQuarter = evalQuarter;
}

async function shiftQueryPeriod(step: -1 | 1) {
  shiftPeriod(queryForm, step);
  await handleQuery();
}

async function shiftGeneratePeriod(step: -1 | 1) {
  shiftPeriod(generateForm, step);
  await loadAvailableSuppliers();
}

async function openPreviewPanel(row: SrmQuarterlyPerformanceApi.Evaluation) {
  detailPanelOpen.value = true;
  checkedIds.value = [];
  await (gridApi.grid as any)?.clearCheckboxRow?.();
  await queryCardList(cardPageNo.value);
  await previewEvaluation(row);
}

async function openCreate() {
  await loadPublishedTemplates();
  const empty = createEmptyForm();
  generateForm.evalYear = empty.evalYear;
  generateForm.evalQuarter = empty.evalQuarter;
  generateForm.templateVersionId = templateOptions.value[0]?.value;
  Object.assign(generateFilter, {
    level: undefined,
    supplierInfo: undefined,
  });
  selectedGenerateSupplierIds.value = [];
  availableSuppliers.value = [];
  if (!generateForm.templateVersionId) {
    message.warning('请先发布季度评分模板');
    return;
  }
  generateModalOpen.value = true;
  await loadAvailableSuppliers();
}

async function loadAvailableSuppliers() {
  const evalYear = normalizeNumber(generateForm.evalYear);
  const evalQuarter = normalizeNumber(generateForm.evalQuarter);
  if (!evalYear || !evalQuarter) {
    availableSuppliers.value = [];
    selectedGenerateSupplierIds.value = [];
    return;
  }
  availableSupplierLoading.value = true;
  try {
    const result = await getQuarterlyPerformanceAvailableSuppliers({
      evalQuarter,
      evalYear,
      level: generateFilter.level,
      supplierInfo: generateFilter.supplierInfo?.trim(),
    });
    availableSuppliers.value = result || [];
    const availableIds = new Set(
      availableSuppliers.value
        .map((item) => normalizeId(item.supplierId))
        .filter((item): item is number => !!item),
    );
    selectedGenerateSupplierIds.value =
      selectedGenerateSupplierIds.value.filter((id) => availableIds.has(id));
  } finally {
    availableSupplierLoading.value = false;
  }
}

async function handleBatchGenerate() {
  const evalYear = normalizeNumber(generateForm.evalYear);
  const evalQuarter = normalizeNumber(generateForm.evalQuarter);
  const templateVersionId = normalizeId(generateForm.templateVersionId);
  if (!evalYear || !evalQuarter || !templateVersionId) {
    message.warning('请先选择年度、季度，并确认存在默认评分模板');
    return;
  }
  if (selectedGenerateSupplierIds.value.length === 0) {
    message.warning('请选择需要生成评价单的供应商');
    return;
  }
  generateLoading.value = true;
  try {
    const ids = await batchCreateQuarterlyPerformance({
      evalQuarter,
      evalYear,
      supplierIds: selectedGenerateSupplierIds.value,
      templateVersionId,
    });
    message.success(`已生成 ${ids.length} 张季度绩效评价单`);
    generateModalOpen.value = false;
    selectedGenerateSupplierIds.value = [];
    await refreshList();
    if (ids[0]) {
      detailPanelOpen.value = true;
      cardPageNo.value = 1;
      await queryCardList(1);
      await previewEvaluation({ id: ids[0] });
    }
  } finally {
    generateLoading.value = false;
  }
}

function handleGridCheckboxChange({
  records,
}: {
  records: SrmQuarterlyPerformanceApi.Evaluation[];
}) {
  checkedIds.value = records
    .map((item) => normalizeId(item.id))
    .filter((item): item is number => !!item);
}

async function handleGridCellClick({
  column,
  row,
}: {
  column?: Record<string, any>;
  row: SrmQuarterlyPerformanceApi.Evaluation;
}) {
  const columnType = String(column?.type || '');
  const columnField = String(column?.field || column?.property || '');
  const columnTitle = String(column?.title || '');
  if (
    columnType === 'checkbox' ||
    columnField === 'operation' ||
    columnField === 'rowActions' ||
    columnTitle === '操作'
  ) {
    return;
  }
  await openPreviewPanel(row);
}

function clearSelectedEvaluation() {
  selectedEvaluation.value = null;
}

async function previewEvaluation(
  row: Partial<SrmQuarterlyPerformanceApi.Evaluation>,
) {
  const id = normalizeId(row.id);
  if (!id) {
    return;
  }
  selectedEvaluationLoading.value = true;
  try {
    const result = await getQuarterlyPerformance(id);
    selectedEvaluation.value = toEvaluationForm({
      ...result,
      items: result.items || [],
    });
  } finally {
    selectedEvaluationLoading.value = false;
  }
}

async function openDetail(row: SrmQuarterlyPerformanceApi.Evaluation) {
  if (!row.id) {
    return;
  }
  await loadPublishedTemplates();
  const result = await getQuarterlyPerformance(row.id);
  selectedDetailItemId.value = undefined;
  detailBottomActiveKey.value = 'trace';
  resetDetailTrendData();
  resetForm({ ...result, items: result.items || [] });
  detailMode.value = 'detail';
  basicExpanded.value = false;
  detailModalApi.open();
}

async function openEdit(row: SrmQuarterlyPerformanceApi.Evaluation) {
  await openDetail(row);
  if (!form.canMaintain) {
    message.warning('当前用户不能编辑此季度评价');
    return;
  }
  detailMode.value = 'edit';
  basicExpanded.value = true;
}

function switchToEdit() {
  if (!form.canMaintain) {
    message.warning('当前用户不能编辑此季度评价');
    return;
  }
  detailMode.value = 'edit';
  basicExpanded.value = true;
}

async function closeDetail() {
  logModalOpen.value = false;
  trendModalOpen.value = false;
  signOpinionOpen.value = false;
  selectedDetailItemId.value = undefined;
  detailBottomActiveKey.value = 'trace';
  resetDetailTrendData();
  await detailModalApi.close();
}

async function openTrend(item?: SrmQuarterlyPerformanceApi.Item) {
  const evaluationId = normalizeId(form.id);
  if (!evaluationId) {
    message.warning('请先保存季度评价单');
    return;
  }
  trendModalOpen.value = true;
  trendLoading.value = true;
  try {
    trendData.value = await getQuarterlyPerformanceTrend({
      evalYear: normalizeNumber(form.evalYear),
      evaluationId,
      indicatorCodes: item?.indicatorCodeSnapshot,
    });
  } finally {
    trendLoading.value = false;
  }
}

async function handleDetailBottomTabChange(activeKey: string) {
  detailBottomActiveKey.value = activeKey as DetailBottomTab;
  if (activeKey === 'trend') {
    await loadSelectedDetailTrend();
  }
}

async function selectDetailItem(record: SrmQuarterlyPerformanceApi.Item) {
  const itemId = normalizeId(record.id);
  if (!itemId) {
    return;
  }
  if (selectedDetailItemId.value === itemId) {
    if (detailBottomActiveKey.value === 'trend') {
      await loadSelectedDetailTrend();
    }
    return;
  }
  selectedDetailItemId.value = itemId;
  resetDetailTrendData();
  if (detailBottomActiveKey.value === 'trend') {
    await loadSelectedDetailTrend();
  }
}

async function loadSelectedDetailTrend(force = false) {
  const evaluationId = normalizeId(form.id);
  const item = selectedDetailItem.value;
  const itemId = normalizeId(item?.id);
  const indicatorCode = item?.indicatorCodeSnapshot;
  if (!evaluationId || !itemId || !indicatorCode || !canViewTrend(item)) {
    resetDetailTrendData();
    return;
  }
  const requestKey = [
    evaluationId,
    itemId,
    normalizeNumber(form.evalYear),
    indicatorCode,
  ].join(':');
  if (!force && detailTrendRequestKey.value === requestKey) {
    return;
  }
  detailTrendRequestKey.value = requestKey;
  detailTrendLoading.value = true;
  try {
    const result = await getQuarterlyPerformanceTrend({
      evalYear: normalizeNumber(form.evalYear),
      evaluationId,
      indicatorCodes: indicatorCode,
    });
    if (selectedDetailItemId.value === itemId) {
      detailTrendData.value = result || createEmptyTrendData();
    }
  } finally {
    detailTrendLoading.value = false;
  }
}

function detailItemCustomRow(record: SrmQuarterlyPerformanceApi.Item) {
  return {
    onClick: () => {
      void selectDetailItem(record);
    },
  };
}

function detailItemRowClassName(record: SrmQuarterlyPerformanceApi.Item) {
  return normalizeId(record.id) === selectedDetailItemId.value
    ? 'srm-quarter-item-row--selected'
    : '';
}

async function loadPublishedTemplates() {
  if (publishedTemplates.value.length > 0) {
    return;
  }
  publishedTemplates.value = await getPublishedTemplateList('QUARTER');
}

async function saveEvaluation(closeAfter = false) {
  if (!normalizeId(form.supplierId) || !form.supplierName?.trim()) {
    message.warning('请选择评价供应商');
    return;
  }
  if (!form.evalYear || !form.evalQuarter) {
    message.warning('请填写年度和季度');
    return;
  }
  if (!normalizeId(form.templateVersionId)) {
    message.warning('请选择已发布的季度评分模板');
    return;
  }
  saving.value = true;
  try {
    const payload: SrmQuarterlyPerformanceApi.Evaluation = {
      evalQuarter: normalizeNumber(form.evalQuarter),
      evalYear: normalizeNumber(form.evalYear),
      evaluationNo: form.evaluationNo,
      id: form.id,
      remark: form.remark,
      supplierCode: form.supplierCode,
      supplierId: normalizeId(form.supplierId),
      supplierName: form.supplierName?.trim(),
      supplierSourceType: form.supplierSourceType || 'REGISTERED',
      templateVersionId: normalizeId(form.templateVersionId),
      version: form.version,
    };
    if (form.id) {
      await updateQuarterlyPerformance(payload);
    } else {
      form.id = await createQuarterlyPerformance(payload);
    }
    await refreshCurrent(form.id!);
    detailMode.value = 'detail';
    message.success('季度评价单已保存');
    if (closeAfter) {
      await closeDetail();
    }
  } finally {
    saving.value = false;
  }
}

async function handlePullActuals(row?: SrmQuarterlyPerformanceApi.Evaluation) {
  const id = normalizeId(row?.id || form.id);
  if (!id) {
    message.warning('请先保存季度评价单');
    return;
  }
  await pullQuarterlyActuals(id);
  message.success('已拉取实际值并刷新计算结果');
  await refreshCurrent(id);
}

async function handleSend(row?: SrmQuarterlyPerformanceApi.Evaluation) {
  const id = normalizeId(row?.id || form.id);
  if (!id) {
    return;
  }
  await sendQuarterlyScoring(id);
  message.success('已发送人工评分');
  await refreshCurrent(id);
}

async function handleSubmitScore() {
  if (!form.id) {
    return;
  }
  const items = myScoreItems.value.map((item) => ({
    itemId: item.id,
    manualScore: normalizeNumber(item.manualScore),
    scoringDescription: item.scoringDescription,
  }));
  if (items.length === 0) {
    message.warning('没有需要你评分的指标');
    return;
  }
  if (items.some((item) => item.manualScore === undefined)) {
    message.warning('请填写所有本人待评分指标的人工分');
    return;
  }
  await submitQuarterlyScore(
    form.id,
    items as Array<{
      itemId: number;
      manualScore: number;
      scoringDescription?: string;
    }>,
  );
  message.success('本人评分已提交');
  await refreshCurrent(form.id);
}

async function handleCalculate(row?: SrmQuarterlyPerformanceApi.Evaluation) {
  const id = normalizeId(row?.id || form.id);
  if (!id) {
    return;
  }
  await calculateQuarterlyTotal(id);
  message.success('季度总分与等级已计算');
  await refreshCurrent(id);
}

async function handleBatchOperation(action: 'calculate' | 'pull' | 'send') {
  const actionMap = {
    calculate: {
      label: '计算总分',
      runner: calculateQuarterlyTotal,
      success: '批量计算总分完成',
    },
    pull: {
      label: '拉取实际值',
      runner: pullQuarterlyActuals,
      success: '批量拉取实际值完成',
    },
    send: {
      label: '发送评分',
      runner: sendQuarterlyScoring,
      success: '批量发送评分完成',
    },
  };
  const currentAction = actionMap[action];
  batchOperating.value = true;
  let hideLoading: (() => void) | undefined;
  const failedNames: string[] = [];
  try {
    const { cancelled, rows } = await resolveRowsForBatch(currentAction.label);
    if (cancelled || rows.length === 0) {
      return;
    }
    hideLoading = message.loading({
      content: `正在${currentAction.label}...`,
      duration: 0,
    });
    for (const row of rows) {
      const id = normalizeId(row.id);
      if (!id) {
        continue;
      }
      try {
        await currentAction.runner(id);
      } catch {
        failedNames.push(row.supplierName || row.evaluationNo || String(id));
      }
    }
    if (failedNames.length > 0) {
      const failedPreview = failedNames.slice(0, 8).join('、');
      const failedText =
        failedNames.length > 8
          ? `${failedPreview}等 ${failedNames.length} 条`
          : failedPreview;
      message.warning(
        `${currentAction.label}完成，${rows.length - failedNames.length} 条成功，${failedNames.length} 条失败：${failedText}`,
      );
    } else {
      message.success(`${currentAction.success}，共 ${rows.length} 条`);
    }
    checkedIds.value = [];
    await (gridApi.grid as any)?.clearCheckboxRow?.();
    await refreshList();
    if (selectedEvaluation.value?.id) {
      await previewEvaluation({ id: selectedEvaluation.value.id });
    }
  } finally {
    batchOperating.value = false;
    hideLoading?.();
  }
}

async function handleBatchMenuClick({ key }: { key: number | string }) {
  const action = String(key);
  if (action === 'generate') {
    await openCreate();
    return;
  }
  if (['calculate', 'pull', 'send'].includes(action)) {
    await handleBatchOperation(action as 'calculate' | 'pull' | 'send');
  }
}

async function handleRowMenuClick(
  { key }: { key: number | string },
  row: SrmQuarterlyPerformanceApi.Evaluation,
) {
  const action = String(key);
  if (action === 'detail') {
    await openDetail(row);
    return;
  }
  if (action === 'edit') {
    await openEdit(row);
    return;
  }
  if (action === 'pull') {
    await handlePullActuals(row);
    return;
  }
  if (action === 'send') {
    await handleSend(row);
    return;
  }
  if (action === 'calculate') {
    await handleCalculate(row);
    return;
  }
  if (action === 'delete') {
    handleDelete(row);
    return;
  }
  if (action === 'collapse') {
    clearSelectedEvaluation();
    detailPanelOpen.value = false;
    checkedIds.value = [];
    await gridApi.query();
  }
}

function openStartSign() {
  if (!form.id) {
    return;
  }
  userModalApi
    .setData({
      maximized: true,
      modalZIndex: SRM_NESTED_MODAL_Z_INDEX,
      multiple: true,
      userIds: [],
    })
    .open();
}

async function handleUserConfirm(users: SystemUserApi.User[]) {
  if (!form.id) {
    return;
  }
  const signers = users
    .filter((user) => normalizeId(user.id))
    .map((user) => ({
      deptName: (user as Record<string, any>).deptName,
      userId: Number(user.id),
      userName: user.nickname || user.username,
    }));
  if (signers.length === 0) {
    message.warning('请选择会签人员');
    return;
  }
  await startQuarterlySign(form.id, signers);
  message.success('已发起季度评价会签');
  await refreshCurrent(form.id);
}

function openSignOpinion(result: 'FAIL' | 'PASS') {
  signResult.value = result;
  signOpinion.value = '';
  signOpinionOpen.value = true;
}

async function confirmSignOpinion() {
  if (!form.id) {
    return;
  }
  await submitQuarterlySign(form.id, signResult.value, signOpinion.value);
  message.success('会签意见已提交');
  signOpinionOpen.value = false;
  await refreshCurrent(form.id);
}

async function refreshCurrent(id: number) {
  const result = await getQuarterlyPerformance(id);
  if (form.id === id) {
    resetForm({ ...result, items: result.items || [] });
    if (detailBottomActiveKey.value === 'trend') {
      await loadSelectedDetailTrend(true);
    }
  }
  if (selectedEvaluation.value?.id === id) {
    selectedEvaluation.value = toEvaluationForm({
      ...result,
      items: result.items || [],
    });
  }
  await refreshList();
}

function openSupplierPicker() {
  supplierModalApi
    .setData({
      keyword: form.supplierName || form.supplierCode,
      modalZIndex: SRM_NESTED_MODAL_Z_INDEX,
      referenceType: 'supplier',
      supplierSource: 'roster',
      title: '选择评价供应商',
    })
    .open();
}

async function handleSupplierSelected(row: Record<string, any>) {
  const data = {
    supplierCode: row.supplierCode,
    supplierId: normalizeId(row.supplierId || row.id),
    supplierName: row.supplierName,
  };
  if (!canEditBasic.value) {
    return;
  }
  Object.assign(form, {
    ...data,
    supplierSourceType: row.sourceType || 'REGISTERED',
  });
}

onMounted(() => {
  gridApi.query();
});

function handleDelete(row: SrmQuarterlyPerformanceApi.Evaluation) {
  AntModal.confirm({
    content: `确认删除季度评价单 ${row.evaluationNo || row.supplierName} 吗？`,
    okButtonProps: { danger: true },
    okText: '删除',
    async onOk() {
      await deleteQuarterlyPerformance(row.id!);
      message.success('季度评价单已删除');
      if (selectedEvaluation.value?.id === row.id) {
        clearSelectedEvaluation();
      }
      await refreshList();
    },
    title: '删除季度评价单',
  });
}

function applySelectedTemplateSnapshot(versionId?: number) {
  const selectedOption = templateOptions.value.find(
    (item) => item.value === versionId,
  );
  const template = selectedOption?.template;
  form.templateId = normalizeId(template?.id);
  form.templateVersionId = normalizeId(selectedOption?.value);
  form.templateCodeSnapshot = template?.templateCode;
  form.templateNameSnapshot = template?.templateName;
  form.templateVersionSnapshot =
    template?.currentVersionNo || template?.currentVersion?.versionNo;
}

function quarterOptions() {
  return [
    { label: 'Q1', value: 1 },
    { label: 'Q2', value: 2 },
    { label: 'Q3', value: 3 },
    { label: 'Q4', value: 4 },
  ];
}

function statusOptions() {
  return [
    { label: '草稿', value: 'DRAFT' },
    { label: '待补实际值', value: 'PENDING_DATA' },
    { label: '评分中', value: 'SCORING' },
    { label: '待计算总分', value: 'PENDING_CALCULATION' },
    { label: '待发起会签', value: 'PENDING_SIGN' },
    { label: '会签中', value: 'SIGNING' },
    { label: '已归档', value: 'ARCHIVED' },
    { label: '已退回', value: 'REJECTED' },
  ];
}

function statusText(status?: unknown) {
  const value = status === undefined || status === null ? '' : String(status);
  return statusOptions().find((item) => item.value === value)?.label || '草稿';
}

function statusDisplayText(status?: unknown) {
  if (status === undefined || status === null || status === '') {
    return '-';
  }
  return statusText(status);
}

function statusColor(status?: unknown) {
  const value = status === undefined || status === null ? '' : String(status);
  if (value === 'ARCHIVED') {
    return 'success';
  }
  if (['SCORING', 'SIGNING'].includes(value)) {
    return 'processing';
  }
  if (['PENDING_DATA', 'REJECTED'].includes(value)) {
    return 'error';
  }
  if (['PENDING_CALCULATION', 'PENDING_SIGN'].includes(value)) {
    return 'warning';
  }
  return 'default';
}

function indicatorTypeText(type?: unknown) {
  const value = type === undefined || type === null ? '' : String(type);
  if (value === 'CALCULATED_SCORE') {
    return '计算评分';
  }
  if (value === 'MIXED') {
    return '计算+人工';
  }
  return '人工评分';
}

function canViewTrend(item: SrmQuarterlyPerformanceApi.Item) {
  return (
    !!item.calcRuleId ||
    ['CALCULATED_SCORE', 'MIXED'].includes(item.indicatorTypeSnapshot || '')
  );
}

function scoreStatusText(status?: unknown) {
  const value = status === undefined || status === null ? '' : String(status);
  if (value === 'AUTO_COMPLETED') {
    return '自动完成';
  }
  if (value === 'COMPLETED') {
    return '已评分';
  }
  if (value === 'ADJUSTED') {
    return '已修正';
  }
  return '待评分';
}

function dataStatusText(status?: unknown) {
  return status === 'READY' ? '已就绪' : '待实际值';
}

function signStatusText(status?: unknown) {
  return status === 'COMPLETED' ? '已会签' : '待会签';
}

function signResultText(result?: unknown) {
  const value = result === undefined || result === null ? '' : String(result);
  if (value === 'PASS') {
    return '同意';
  }
  if (value === 'FAIL') {
    return '不同意';
  }
  return '-';
}

function logActionText(action?: unknown) {
  const value = action === undefined || action === null ? '' : String(action);
  const textMap: Record<string, string> = {
    CALCULATE_TOTAL: '计算总分',
    CREATE: '生成评价',
    DELETE: '删除',
    PULL_ACTUALS: '拉取实际值',
    SCORE: '提交评分',
    SEND_SCORING: '发送评分',
    SIGN: '提交会签',
    START_SIGN: '发起会签',
    UPDATE: '更新草稿',
  };
  return textMap[value] || displayValue(value);
}

function gradeColor(grade?: unknown) {
  const value = grade === undefined || grade === null ? '' : String(grade);
  if (value === 'A') {
    return 'success';
  }
  if (value === 'B') {
    return 'processing';
  }
  if (value === 'C') {
    return 'warning';
  }
  if (value === 'D') {
    return 'error';
  }
  return 'default';
}

function templateText(config: Partial<SrmQuarterlyPerformanceApi.Evaluation>) {
  return [
    config.templateCodeSnapshot,
    config.templateNameSnapshot,
    config.templateVersionSnapshot,
  ]
    .filter(Boolean)
    .join(' / ');
}

function displayValue(value?: unknown) {
  if (value === undefined || value === null || value === '') {
    return '-';
  }
  return String(value);
}

function tableCellValue(
  record: Record<string, any>,
  dataIndex: TableColumnsType[number]['dataIndex'],
) {
  if (Array.isArray(dataIndex)) {
    let current: any = record;
    for (const key of dataIndex) {
      current = current?.[key as keyof typeof current];
    }
    return current;
  }
  if (typeof dataIndex === 'number' || typeof dataIndex === 'string') {
    return record[dataIndex];
  }
  return undefined;
}

function normalizeId(value: unknown) {
  if (value === undefined || value === null || value === '') {
    return undefined;
  }
  const numericValue = Number(value);
  return Number.isFinite(numericValue) && numericValue > 0
    ? numericValue
    : undefined;
}

function normalizeNumber(value: unknown) {
  if (value === undefined || value === null || value === '') {
    return undefined;
  }
  const numericValue = Number(value);
  return Number.isFinite(numericValue) ? numericValue : undefined;
}

function resolveNestedPopupContainer(triggerNode: HTMLElement) {
  return triggerNode.parentElement || triggerNode;
}
</script>

<template>
  <Page auto-content-height>
    <SupplierModal title="选择供应商" @select="handleSupplierSelected" />
    <UserModal title="选择会签人员" @confirm="handleUserConfirm" />

    <div class="srm-crud-page">
      <div class="srm-crud-page__header">
        <div class="srm-crud-page__title">季度绩效评定</div>
      </div>
      <div class="srm-quarter-query-panel">
        <div class="srm-quarter-query-row">
          <div class="srm-quarter-query-main-row">
            <Form class="srm-quarter-query-form" layout="inline">
              <Form.Item label="供应商名称/编码">
                <Input
                  v-model:value="queryForm.supplierInfo"
                  allow-clear
                  class="srm-query-input"
                  placeholder="名称/编码模糊搜索"
                  @press-enter="handleQuery"
                />
              </Form.Item>
              <Form.Item label="年度/季度">
                <div class="srm-period-control">
                  <Button title="上一季度" @click="shiftQueryPeriod(-1)">
                    <IconifyIcon icon="lucide:chevron-left" />
                  </Button>
                  <InputNumber
                    v-model:value="queryForm.evalYear"
                    class="srm-query-year"
                    :min="2000"
                    placeholder="年度"
                    @press-enter="handleQuery"
                  />
                  <Select
                    v-model:value="queryForm.evalQuarter"
                    class="srm-query-quarter"
                    :dropdown-style="{ zIndex: SRM_NESTED_DROPDOWN_Z_INDEX }"
                    :get-popup-container="resolveNestedPopupContainer"
                    :options="quarterOptions()"
                    placeholder="季度"
                  />
                  <Button title="下一季度" @click="shiftQueryPeriod(1)">
                    <IconifyIcon icon="lucide:chevron-right" />
                  </Button>
                </div>
              </Form.Item>
              <Form.Item label="状态">
                <Select
                  v-model:value="queryForm.status"
                  allow-clear
                  class="srm-query-status"
                  :dropdown-style="{ zIndex: SRM_NESTED_DROPDOWN_Z_INDEX }"
                  :get-popup-container="resolveNestedPopupContainer"
                  :options="statusOptions()"
                  placeholder="状态"
                />
              </Form.Item>
              <Form.Item class="srm-quarter-todo-item" label="待办范围">
                <Checkbox
                  v-model:checked="queryForm.todoOnly"
                  @change="handleQuery"
                >
                  只看我的待办
                </Checkbox>
              </Form.Item>
            </Form>
            <Space class="srm-quarter-query-actions" :size="8">
              <Button @click="resetQueryForm">重置</Button>
              <Button type="primary" @click="handleQuery">搜索</Button>
              <Dropdown :trigger="['click']">
                <Button :loading="batchOperating">
                  <IconifyIcon icon="lucide:menu" />
                  操作
                  <IconifyIcon icon="lucide:chevron-down" />
                </Button>
                <template #overlay>
                  <Menu @click="handleBatchMenuClick">
                    <Menu.Item key="pull" :disabled="batchOperating">
                      批量拉取实际值
                    </Menu.Item>
                    <Menu.Item key="send" :disabled="batchOperating">
                      批量发送评分
                    </Menu.Item>
                    <Menu.Item key="calculate" :disabled="batchOperating">
                      批量计算总分
                    </Menu.Item>
                    <Menu.Divider />
                    <Menu.Item key="generate">生成评价</Menu.Item>
                  </Menu>
                </template>
              </Dropdown>
              <Tooltip :title="panelToggleText">
                <Button
                  :aria-label="panelToggleText"
                  class="srm-quarter-panel-toggle"
                  shape="circle"
                  @click="toggleDetailPanel"
                >
                  <IconifyIcon
                    :icon="
                      detailPanelOpen
                        ? 'lucide:panel-right-close'
                        : 'lucide:panel-right-open'
                    "
                  />
                </Button>
              </Tooltip>
            </Space>
          </div>
        </div>
      </div>
      <div class="srm-crud-page__body">
        <div
          class="srm-quarter-split-layout"
          :class="{ 'is-panel-open': detailPanelOpen }"
        >
          <section class="srm-quarter-list-card">
            <div class="srm-quarter-list-head">
              <div>
                <div class="srm-quarter-list-title">供应商考核表</div>
                <div class="srm-quarter-list-subtitle">
                  {{ detailPanelOpen ? '卡片视图' : '表格视图' }} · 已选
                  {{ checkedIds.length }} 项
                </div>
              </div>
            </div>
            <div v-show="!detailPanelOpen" class="srm-quarter-table-mode">
              <Grid>
                <template #supplierName="{ row }">
                  <a class="srm-crud-link" @click.stop="openPreviewPanel(row)">
                    {{ row.supplierName || '-' }}
                  </a>
                </template>
                <template #quarter="{ row }">
                  Q{{ row.evalQuarter || '-' }}
                </template>
                <template #status="{ row }">
                  <Tag :color="statusColor(row.status)" class="!m-0">
                    {{ statusText(row.status) }}
                  </Tag>
                </template>
                <template #totalScore="{ row }">
                  {{ displayValue(row.totalScoreDisplay ?? row.totalScore) }}
                </template>
                <template #evalGrade="{ row }">
                  <Tag :color="gradeColor(row.evalGrade)" class="!m-0">
                    {{ row.evalGrade || '-' }}
                  </Tag>
                </template>
                <template #redline="{ row }">
                  <Tag
                    :color="row.redlineTriggered ? 'error' : 'success'"
                    class="!m-0"
                  >
                    {{ row.redlineTriggered ? '触发' : '正常' }}
                  </Tag>
                </template>
                <template #rowActions="{ row }">
                  <Dropdown :trigger="['click']">
                    <Button shape="circle" size="small" @click.stop>
                      <IconifyIcon icon="lucide:more-horizontal" />
                    </Button>
                    <template #overlay>
                      <Menu @click="handleRowMenuClick($event, row)">
                        <Menu.Item key="detail">详情</Menu.Item>
                        <Menu.Item
                          key="edit"
                          :disabled="!row.canMaintain && row.status !== 'DRAFT'"
                        >
                          编辑
                        </Menu.Item>
                        <Menu.Item key="pull" :disabled="!row.canPullActuals">
                          拉取实际值
                        </Menu.Item>
                        <Menu.Item
                          key="send"
                          :disabled="
                            !(
                              row.canMaintain &&
                              ['DRAFT', 'SCORING'].includes(row.status || '')
                            )
                          "
                        >
                          发送评分
                        </Menu.Item>
                        <Menu.Item
                          key="calculate"
                          :disabled="!row.canCalculate"
                        >
                          计算总分
                        </Menu.Item>
                        <Menu.Divider />
                        <Menu.Item
                          key="delete"
                          danger
                          :disabled="
                            !(row.canMaintain && row.status === 'DRAFT')
                          "
                        >
                          删除
                        </Menu.Item>
                      </Menu>
                    </template>
                  </Dropdown>
                </template>
              </Grid>
            </div>
            <div v-show="detailPanelOpen" class="srm-quarter-card-mode">
              <Spin :spinning="cardLoading">
                <div v-if="cardRows.length > 0" class="srm-quarter-card-list">
                  <div
                    v-for="row in cardRows"
                    :key="row.id"
                    class="srm-quarter-eval-card"
                    :class="{
                      'is-current': selectedEvaluation?.id === row.id,
                      'is-selected': isCardChecked(row),
                    }"
                    @click="previewEvaluation(row)"
                  >
                    <Checkbox
                      :checked="isCardChecked(row)"
                      class="srm-quarter-eval-card__check"
                      @change.stop="toggleCardSelection(row, $event)"
                      @click.stop
                    />
                    <div class="srm-quarter-eval-card__body">
                      <div class="srm-quarter-eval-card__top">
                        <strong>{{ row.supplierName || '-' }}</strong>
                        <span>Q{{ row.evalQuarter || '-' }}</span>
                      </div>
                      <div class="srm-quarter-eval-card__meta">
                        <span>编号 {{ row.supplierCode || '-' }}</span>
                        <span>年 {{ row.evalYear || '-' }}</span>
                        <span>
                          总分
                          {{
                            displayValue(
                              row.totalScoreDisplay ?? row.totalScore,
                            )
                          }}
                        </span>
                      </div>
                    </div>
                    <Dropdown :trigger="['click']">
                      <Button
                        class="srm-quarter-eval-card__more"
                        shape="circle"
                        size="small"
                        @click.stop
                      >
                        <IconifyIcon icon="lucide:more-horizontal" />
                      </Button>
                      <template #overlay>
                        <Menu @click="handleRowMenuClick($event, row)">
                          <Menu.Item key="detail">详情</Menu.Item>
                          <Menu.Item
                            key="edit"
                            :disabled="
                              !row.canMaintain && row.status !== 'DRAFT'
                            "
                          >
                            编辑
                          </Menu.Item>
                          <Menu.Item key="pull" :disabled="!row.canPullActuals">
                            拉取实际值
                          </Menu.Item>
                          <Menu.Item
                            key="send"
                            :disabled="
                              !(
                                row.canMaintain &&
                                ['DRAFT', 'SCORING'].includes(row.status || '')
                              )
                            "
                          >
                            发送评分
                          </Menu.Item>
                          <Menu.Item
                            key="calculate"
                            :disabled="!row.canCalculate"
                          >
                            计算总分
                          </Menu.Item>
                          <Menu.Divider />
                          <Menu.Item
                            key="delete"
                            danger
                            :disabled="
                              !(row.canMaintain && row.status === 'DRAFT')
                            "
                          >
                            删除
                          </Menu.Item>
                        </Menu>
                      </template>
                    </Dropdown>
                  </div>
                </div>
                <div v-else class="srm-quarter-card-empty">
                  暂无供应商考核表
                </div>
              </Spin>
              <div class="srm-quarter-card-pager">
                <span>共 {{ cardTotal }} 条</span>
                <Space :size="6">
                  <Button
                    :disabled="cardPageNo <= 1 || cardLoading"
                    size="small"
                    @click="handleCardPageChange('prev')"
                  >
                    上一页
                  </Button>
                  <span>{{ cardPageNo }} / {{ cardTotalPage }}</span>
                  <Button
                    :disabled="cardPageNo >= cardTotalPage || cardLoading"
                    size="small"
                    @click="handleCardPageChange('next')"
                  >
                    下一页
                  </Button>
                </Space>
              </div>
            </div>
          </section>
          <section v-if="detailPanelOpen" class="srm-quarter-items-card">
            <Spin :spinning="selectedEvaluationLoading">
              <div class="srm-preview-panel">
                <div class="srm-preview-panel__head">
                  <div>
                    <div class="srm-preview-panel__title">
                      {{ selectedEvaluationTitle }}
                    </div>
                    <div
                      v-if="selectedEvaluation"
                      class="srm-preview-panel__subtitle"
                    >
                      {{ statusText(selectedEvaluation.status) }} · 总分
                      {{
                        displayValue(
                          selectedEvaluation.totalScoreDisplay ??
                            selectedEvaluation.totalScore,
                        )
                      }}
                    </div>
                  </div>
                  <Dropdown v-if="selectedEvaluation" :trigger="['click']">
                    <Button size="small">
                      <IconifyIcon icon="lucide:menu" />
                      操作
                      <IconifyIcon icon="lucide:chevron-down" />
                    </Button>
                    <template #overlay>
                      <Menu
                        @click="handleRowMenuClick($event, selectedEvaluation)"
                      >
                        <Menu.Item key="detail">详情</Menu.Item>
                        <Menu.Item
                          key="edit"
                          :disabled="
                            !selectedEvaluation.canMaintain &&
                            selectedEvaluation.status !== 'DRAFT'
                          "
                        >
                          编辑
                        </Menu.Item>
                        <Menu.Item
                          key="pull"
                          :disabled="!selectedEvaluation.canPullActuals"
                        >
                          拉取实际值
                        </Menu.Item>
                        <Menu.Item
                          key="send"
                          :disabled="
                            !(
                              selectedEvaluation.canMaintain &&
                              ['DRAFT', 'SCORING'].includes(
                                selectedEvaluation.status || '',
                              )
                            )
                          "
                        >
                          发送评分
                        </Menu.Item>
                        <Menu.Item
                          key="calculate"
                          :disabled="!selectedEvaluation.canCalculate"
                        >
                          计算总分
                        </Menu.Item>
                        <Menu.Divider />
                        <Menu.Item
                          key="delete"
                          danger
                          :disabled="
                            !(
                              selectedEvaluation.canMaintain &&
                              selectedEvaluation.status === 'DRAFT'
                            )
                          "
                        >
                          删除
                        </Menu.Item>
                        <Menu.Item key="collapse">收起右侧</Menu.Item>
                      </Menu>
                    </template>
                  </Dropdown>
                </div>
                <template v-if="selectedEvaluation">
                  <div class="srm-preview-panel__stats">
                    <span>等级 {{ selectedEvaluation.evalGrade || '-' }}</span>
                    <span>
                      红线
                      {{
                        selectedEvaluation.redlineTriggered ? '触发' : '正常'
                      }}
                    </span>
                    <span>{{ selectedEvaluation.items.length }} 项指标</span>
                  </div>
                  <Table
                    :columns="previewItemColumns"
                    :data-source="selectedEvaluation.items"
                    :pagination="false"
                    row-key="id"
                    :scroll="{ x: 900, y: 620 }"
                    size="small"
                  >
                    <template #bodyCell="{ column, index, record }">
                      <template v-if="column.dataIndex === 'seq'">
                        {{ index + 1 }}
                      </template>
                      <template v-else>
                        {{
                          displayValue(tableCellValue(record, column.dataIndex))
                        }}
                      </template>
                    </template>
                  </Table>
                </template>
                <div v-else class="srm-preview-panel__empty">
                  点击左侧供应商评价单查看指标明细
                </div>
              </div>
            </Spin>
          </section>
        </div>
      </div>
    </div>

    <AntModal
      v-model:open="generateModalOpen"
      :confirm-loading="generateLoading"
      destroy-on-close
      ok-text="批量生成"
      title="生成季度绩效评价"
      width="980px"
      :z-index="SRM_NESTED_MODAL_Z_INDEX"
      @ok="handleBatchGenerate"
    >
      <Form layout="vertical">
        <div class="srm-generate-form-grid">
          <Form.Item label="年度/季度">
            <div class="srm-period-control">
              <Button title="上一季度" @click="shiftGeneratePeriod(-1)">
                <IconifyIcon icon="lucide:chevron-left" />
              </Button>
              <InputNumber
                v-model:value="generateForm.evalYear"
                class="srm-query-year"
                :min="2000"
                @change="loadAvailableSuppliers"
              />
              <Select
                v-model:value="generateForm.evalQuarter"
                class="srm-query-quarter"
                :dropdown-style="{ zIndex: SRM_NESTED_DROPDOWN_Z_INDEX }"
                :get-popup-container="resolveNestedPopupContainer"
                :options="quarterOptions()"
                @change="loadAvailableSuppliers"
              />
              <Button title="下一季度" @click="shiftGeneratePeriod(1)">
                <IconifyIcon icon="lucide:chevron-right" />
              </Button>
            </div>
          </Form.Item>
          <Form.Item label="供应商">
            <Input
              v-model:value="generateFilter.supplierInfo"
              allow-clear
              placeholder="编码/名称"
              @press-enter="loadAvailableSuppliers"
            />
          </Form.Item>
          <Form.Item label="评定等级">
            <Select
              v-model:value="generateFilter.level"
              allow-clear
              :dropdown-style="{ zIndex: SRM_NESTED_DROPDOWN_Z_INDEX }"
              :get-popup-container="resolveNestedPopupContainer"
              :options="levelOptions"
              placeholder="全部"
              @change="loadAvailableSuppliers"
            />
          </Form.Item>
          <Form.Item label=" ">
            <Button
              :loading="availableSupplierLoading"
              @click="loadAvailableSuppliers"
            >
              <IconifyIcon icon="lucide:refresh-cw" />
              刷新名单
            </Button>
          </Form.Item>
          <div class="srm-generate-template-note">
            默认评分模板：{{ generateTemplateLabel }}
          </div>
        </div>
        <div class="srm-generate-hint">
          下表仅显示目标年度、季度未生成季度绩效考核记录的供应商。
        </div>
        <Table
          :columns="generateSupplierColumns"
          :data-source="availableSuppliers"
          :loading="availableSupplierLoading"
          :pagination="{ pageSize: 10, showSizeChanger: false }"
          row-key="supplierId"
          :row-selection="generateSupplierRowSelection"
          :scroll="{ x: 700, y: 420 }"
          size="small"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'level'">
              <Tag :color="gradeColor(record.level)" class="!m-0">
                {{ record.level || '-' }}
              </Tag>
            </template>
            <template v-else>
              {{ displayValue(tableCellValue(record, column.dataIndex)) }}
            </template>
          </template>
        </Table>
      </Form>
    </AntModal>

    <DetailModal>
      <Spin :spinning="saving" class="detail-spin">
        <div class="qms-ncr-detail">
          <div class="qms-ncr-toolbar">
            <div class="qms-ncr-toolbar__placeholder">
              <Button class="qms-ncr-toolbar-action" @click="closeDetail">
                <IconifyIcon icon="lucide:arrow-left" />
                返回
              </Button>
            </div>
            <div class="qms-ncr-title-panel">
              <div class="qms-ncr-title-panel__name">季度绩效评定</div>
              <div class="qms-ncr-title-panel__subtitle">
                <span
                  v-for="item in subtitleItems"
                  :key="item"
                  class="qms-ncr-title-panel__subtitle-item"
                >
                  {{ item }}
                </span>
              </div>
            </div>
            <div class="qms-ncr-toolbar__actions">
              <Button
                v-if="isReadonly && form.canMaintain"
                class="qms-ncr-toolbar-action"
                type="primary"
                @click="switchToEdit"
              >
                <IconifyIcon icon="lucide:edit-3" />
                编辑
              </Button>
              <Button
                v-if="canEditBasic"
                class="qms-ncr-toolbar-action"
                type="primary"
                @click="saveEvaluation(false)"
              >
                <IconifyIcon icon="lucide:save" />
                保存
              </Button>
              <Button
                v-if="form.id && form.canPullActuals"
                class="qms-ncr-toolbar-action"
                @click="handlePullActuals()"
              >
                <IconifyIcon icon="lucide:database-zap" />
                拉取实际值
              </Button>
              <Button
                v-if="form.id"
                class="qms-ncr-toolbar-action"
                @click="openTrend()"
              >
                <IconifyIcon icon="lucide:chart-no-axes-combined" />
                数据趋势
              </Button>
              <Button
                v-if="
                  form.id &&
                  form.canMaintain &&
                  ['DRAFT', 'SCORING'].includes(form.status || '')
                "
                class="qms-ncr-toolbar-action"
                @click="handleSend()"
              >
                <IconifyIcon icon="lucide:send" />
                发送评分
              </Button>
              <Button
                v-if="form.canScore"
                class="qms-ncr-toolbar-action"
                type="primary"
                @click="handleSubmitScore"
              >
                <IconifyIcon icon="lucide:clipboard-check" />
                提交本人评分
              </Button>
              <Button
                v-if="form.canCalculate"
                class="qms-ncr-toolbar-action"
                type="primary"
                @click="handleCalculate()"
              >
                <IconifyIcon icon="lucide:calculator" />
                计算总分
              </Button>
              <Button
                v-if="form.canStartSign"
                class="qms-ncr-toolbar-action"
                type="primary"
                @click="openStartSign"
              >
                <IconifyIcon icon="lucide:users-round" />
                发起会签
              </Button>
              <Button
                v-if="form.canSign"
                class="qms-ncr-toolbar-action"
                type="primary"
                @click="openSignOpinion('PASS')"
              >
                <IconifyIcon icon="lucide:badge-check" />
                同意会签
              </Button>
              <Button
                v-if="form.canSign"
                class="qms-ncr-toolbar-action"
                danger
                @click="openSignOpinion('FAIL')"
              >
                <IconifyIcon icon="lucide:circle-x" />
                不同意
              </Button>
              <Button
                v-if="form.id"
                class="qms-ncr-toolbar-action"
                @click="logModalOpen = true"
              >
                <IconifyIcon icon="lucide:history" />
                操作日志（{{ form.logs.length }}）
              </Button>
              <Button class="qms-ncr-toolbar-action" @click="closeDetail">
                关闭
              </Button>
            </div>
          </div>

          <div class="detail-content">
            <div class="qms-exception-workbench">
              <div class="qms-exception-form">
                <section class="erp-basic-form">
                  <div class="detail-list-head">
                    <div class="detail-list-title">
                      <strong>评价基本信息</strong>
                    </div>
                    <Button
                      size="small"
                      type="link"
                      @click="basicExpanded = !basicExpanded"
                    >
                      <span class="srm-nowrap-action">
                        <IconifyIcon
                          :icon="
                            basicExpanded
                              ? 'lucide:chevron-up'
                              : 'lucide:chevron-down'
                          "
                        />
                        {{ basicExpanded ? '收起' : '展开' }}
                      </span>
                    </Button>
                  </div>
                  <div v-if="!basicExpanded" class="srm-basic-summary">
                    <div
                      v-for="item in basicSummaryItems"
                      :key="item.label"
                      class="srm-basic-summary__item"
                    >
                      <span>{{ item.label }}</span>
                      <strong>{{ item.value }}</strong>
                    </div>
                  </div>
                  <div v-show="basicExpanded" class="erp-form-grid">
                    <div class="erp-form-item">
                      <label class="erp-form-label">评价单号</label>
                      <div class="erp-form-value">
                        <Input
                          v-if="canEditBasic"
                          v-model:value="form.evaluationNo"
                          placeholder="不填则自动生成"
                        />
                        <span v-else class="qms-exception-readonly-value">
                          {{ form.evaluationNo || '-' }}
                        </span>
                      </div>
                    </div>
                    <div class="erp-form-item">
                      <label class="erp-form-label">评价供应商</label>
                      <div class="erp-form-value">
                        <Input
                          v-if="canEditBasic"
                          :value="form.supplierName"
                          placeholder="点击选择供应商"
                          readonly
                          @click="openSupplierPicker"
                        >
                          <template #suffix>
                            <Button
                              class="qms-exception-inline-icon-btn"
                              size="small"
                              title="选择供应商"
                              type="text"
                              @click.stop="openSupplierPicker"
                            >
                              <IconifyIcon icon="lucide:search" />
                            </Button>
                          </template>
                        </Input>
                        <span v-else class="qms-exception-readonly-value">
                          {{ form.supplierName || '-' }}
                        </span>
                      </div>
                    </div>
                    <div class="erp-form-item">
                      <label class="erp-form-label">年度</label>
                      <div class="erp-form-value">
                        <InputNumber
                          v-if="canEditBasic"
                          v-model:value="form.evalYear"
                          class="srm-number-input"
                          :min="2000"
                        />
                        <span v-else>{{ form.evalYear || '-' }}</span>
                      </div>
                    </div>
                    <div class="erp-form-item">
                      <label class="erp-form-label">季度</label>
                      <div class="erp-form-value">
                        <Select
                          v-if="canEditBasic"
                          v-model:value="form.evalQuarter"
                          :dropdown-style="{
                            zIndex: SRM_NESTED_DROPDOWN_Z_INDEX,
                          }"
                          :get-popup-container="resolveNestedPopupContainer"
                          :options="quarterOptions()"
                        />
                        <span v-else>Q{{ form.evalQuarter || '-' }}</span>
                      </div>
                    </div>
                    <div class="erp-form-item">
                      <label class="erp-form-label">评分模板</label>
                      <div class="erp-form-value">
                        <Select
                          v-if="canEditBasic"
                          v-model:value="selectedTemplateVersionId"
                          :dropdown-style="{
                            zIndex: SRM_NESTED_DROPDOWN_Z_INDEX,
                          }"
                          :get-popup-container="resolveNestedPopupContainer"
                          :options="templateOptions"
                          placeholder="请选择已发布的季度评分模板"
                          show-search
                        />
                        <span v-else class="qms-exception-readonly-value">
                          {{ selectedTemplateLabel || '-' }}
                        </span>
                      </div>
                    </div>
                    <div class="erp-form-item">
                      <label class="erp-form-label">状态</label>
                      <div class="erp-form-value">
                        <Tag :color="statusColor(form.status)" class="!m-0">
                          {{ statusText(form.status) }}
                        </Tag>
                      </div>
                    </div>
                    <div class="erp-form-item">
                      <label class="erp-form-label">总分/等级</label>
                      <div class="erp-form-value">
                        {{
                          displayValue(
                            form.totalScoreDisplay ?? form.totalScore,
                          )
                        }}
                        <Tag
                          v-if="form.evalGrade"
                          :color="gradeColor(form.evalGrade)"
                          class="ml-2"
                        >
                          {{ form.evalGrade }}
                        </Tag>
                      </div>
                    </div>
                    <div class="erp-form-item">
                      <label class="erp-form-label">红线状态</label>
                      <div class="erp-form-value">
                        <Tag
                          :color="form.redlineTriggered ? 'error' : 'success'"
                          class="!m-0"
                        >
                          {{ form.redlineTriggered ? '触发' : '正常' }}
                        </Tag>
                      </div>
                    </div>
                    <div class="erp-form-item erp-form-item--full">
                      <label class="erp-form-label erp-form-label--tall">
                        备注
                      </label>
                      <div class="erp-form-value">
                        <Input.TextArea
                          v-if="canEditBasic"
                          v-model:value="form.remark"
                          :rows="3"
                        />
                        <span
                          v-else
                          class="qms-exception-readonly-value qms-exception-readonly-value--multiline"
                        >
                          {{ form.remark || '-' }}
                        </span>
                      </div>
                    </div>
                    <div
                      v-if="form.redlineDescription"
                      class="erp-form-item erp-form-item--full"
                    >
                      <label class="erp-form-label erp-form-label--tall">
                        红线说明
                      </label>
                      <div class="erp-form-value">
                        {{ form.redlineDescription }}
                      </div>
                    </div>
                  </div>
                </section>

                <section
                  v-if="form.id"
                  class="erp-basic-form srm-detail-section"
                >
                  <div class="detail-list-head">
                    <div class="detail-list-title">
                      <strong>评价指标（{{ form.items.length }} 项）</strong>
                    </div>
                  </div>
                  <Table
                    class="srm-quarter-item-table"
                    :columns="itemColumns"
                    :custom-row="detailItemCustomRow"
                    :data-source="form.items"
                    :pagination="false"
                    :row-class-name="detailItemRowClassName"
                    row-key="id"
                    :scroll="{ x: itemTableScrollX, y: 380 }"
                    size="small"
                  >
                    <template #bodyCell="{ column, index, record }">
                      <template v-if="column.dataIndex === 'seq'">
                        {{ index + 1 }}
                      </template>
                      <template
                        v-else-if="column.dataIndex === 'indicatorTypeSnapshot'"
                      >
                        <Tag class="!m-0">
                          {{ indicatorTypeText(record.indicatorTypeSnapshot) }}
                        </Tag>
                      </template>
                      <template v-else-if="column.dataIndex === 'manualScore'">
                        <InputNumber
                          v-if="
                            form.canScore &&
                            record.currentUserItem &&
                            ['MANUAL_SCORE', 'MIXED'].includes(
                              record.indicatorTypeSnapshot,
                            ) &&
                            record.scoreStatus !== 'COMPLETED'
                          "
                          v-model:value="record.manualScore"
                          :max="record.maxScoreSnapshot"
                          :min="0"
                          class="srm-score-input"
                          :precision="2"
                        />
                        <span v-else>{{
                          displayValue(record.manualScore)
                        }}</span>
                      </template>
                      <template v-else-if="column.dataIndex === 'dataStatus'">
                        <Tag
                          :color="
                            record.dataStatus === 'READY'
                              ? 'success'
                              : 'warning'
                          "
                          class="!m-0"
                        >
                          {{ dataStatusText(record.dataStatus) }}
                        </Tag>
                      </template>
                      <template v-else-if="column.dataIndex === 'scoreStatus'">
                        <Tag
                          :color="
                            [
                              'AUTO_COMPLETED',
                              'COMPLETED',
                              'ADJUSTED',
                            ].includes(record.scoreStatus)
                              ? 'success'
                              : 'default'
                          "
                          class="!m-0"
                        >
                          {{ scoreStatusText(record.scoreStatus) }}
                        </Tag>
                      </template>
                      <template
                        v-else-if="column.dataIndex === 'scoringDescription'"
                      >
                        <Input.TextArea
                          v-if="
                            form.canScore &&
                            record.currentUserItem &&
                            record.scoreStatus !== 'COMPLETED'
                          "
                          v-model:value="record.scoringDescription"
                          :auto-size="{ minRows: 1, maxRows: 3 }"
                          class="srm-score-description"
                        />
                        <span v-else>
                          {{ displayValue(record.scoringDescription) }}
                        </span>
                      </template>
                      <template v-else>
                        {{
                          displayValue(tableCellValue(record, column.dataIndex))
                        }}
                      </template>
                    </template>
                  </Table>
                </section>

                <section
                  v-if="form.id"
                  class="erp-basic-form srm-detail-section srm-detail-bottom-tabs"
                >
                  <Tabs
                    v-model:active-key="detailBottomActiveKey"
                    class="srm-detail-tabs"
                    size="small"
                    @change="handleDetailBottomTabChange"
                  >
                    <Tabs.TabPane key="trace">
                      <template #tab>
                        计算追溯（{{ filteredTraces.length }} 条）
                      </template>
                      <div class="srm-detail-tab-context">
                        <span>当前指标</span>
                        <strong>{{ selectedDetailItemTitle }}</strong>
                      </div>
                      <Table
                        :columns="traceColumns"
                        :data-source="filteredTraces"
                        :pagination="false"
                        row-key="id"
                        :scroll="{ x: traceTableScrollX, y: 280 }"
                        size="small"
                      >
                        <template #bodyCell="{ column, record }">
                          <template v-if="column.dataIndex === 'periodMonth'">
                            {{
                              record.periodMonth
                                ? `${record.periodMonth}月`
                                : '-'
                            }}
                          </template>
                          <template v-else>
                            {{
                              displayValue(
                                tableCellValue(record, column.dataIndex),
                              )
                            }}
                          </template>
                        </template>
                      </Table>
                    </Tabs.TabPane>
                    <Tabs.TabPane key="trend" tab="实绩指标趋势图">
                      <Spin :spinning="detailTrendLoading">
                        <div
                          v-if="!selectedDetailItem"
                          class="srm-detail-tab-empty"
                        >
                          请先选择上方评价指标
                        </div>
                        <div
                          v-else-if="!canViewTrend(selectedDetailItem)"
                          class="srm-detail-tab-empty"
                        >
                          当前指标为人工评分，没有实绩指标趋势图
                        </div>
                        <div v-else class="srm-detail-trend-panel">
                          <div class="srm-detail-tab-context">
                            <span>当前指标</span>
                            <strong>{{ selectedDetailItemTitle }}</strong>
                          </div>
                          <TrendPanel
                            empty-description="当前指标暂无实绩指标趋势图"
                            :panels="detailTrendPanels"
                          />
                        </div>
                      </Spin>
                    </Tabs.TabPane>
                  </Tabs>
                </section>

                <section
                  v-if="form.id && form.signs.length > 0"
                  class="erp-basic-form srm-detail-section"
                >
                  <div class="detail-list-head">
                    <div class="detail-list-title">
                      <strong>会签记录（{{ form.signs.length }} 人）</strong>
                    </div>
                  </div>
                  <Table
                    :columns="signColumns"
                    :data-source="form.signs"
                    :pagination="false"
                    row-key="id"
                    :scroll="{ x: 1010, y: 220 }"
                    size="small"
                  >
                    <template #bodyCell="{ column, record }">
                      <template v-if="column.dataIndex === 'signStatus'">
                        <Tag
                          :color="
                            record.signStatus === 'COMPLETED'
                              ? 'success'
                              : 'default'
                          "
                          class="!m-0"
                        >
                          {{ signStatusText(record.signStatus) }}
                        </Tag>
                      </template>
                      <template v-else-if="column.dataIndex === 'signResult'">
                        <Tag
                          :color="
                            record.signResult === 'FAIL' ? 'error' : 'success'
                          "
                          class="!m-0"
                        >
                          {{ signResultText(record.signResult) }}
                        </Tag>
                      </template>
                      <template v-else>
                        {{
                          displayValue(tableCellValue(record, column.dataIndex))
                        }}
                      </template>
                    </template>
                  </Table>
                </section>
              </div>
            </div>
          </div>
        </div>
      </Spin>
    </DetailModal>

    <AntModal
      v-model:open="signOpinionOpen"
      title="提交会签意见"
      :z-index="SRM_NESTED_MODAL_Z_INDEX"
      @ok="confirmSignOpinion"
    >
      <Form layout="vertical">
        <Form.Item label="会签结果">
          <Radio.Group v-model:value="signResult">
            <Radio value="PASS">同意</Radio>
            <Radio value="FAIL">不同意</Radio>
          </Radio.Group>
        </Form.Item>
        <Form.Item label="会签意见">
          <Input.TextArea v-model:value="signOpinion" :rows="4" />
        </Form.Item>
      </Form>
    </AntModal>

    <AntModal
      v-model:open="logModalOpen"
      :footer="null"
      title="操作日志"
      width="980px"
      :z-index="SRM_NESTED_MODAL_Z_INDEX"
    >
      <Table
        :columns="logColumns"
        :data-source="form.logs"
        :pagination="false"
        row-key="id"
        :scroll="{ x: 1010, y: 420 }"
        size="small"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'action'">
            {{ logActionText(record.action) }}
          </template>
          <template
            v-else-if="
              ['fromStatus', 'toStatus'].includes(String(column.dataIndex))
            "
          >
            {{ statusDisplayText(tableCellValue(record, column.dataIndex)) }}
          </template>
          <template v-else>
            {{ displayValue(tableCellValue(record, column.dataIndex)) }}
          </template>
        </template>
      </Table>
    </AntModal>

    <AntModal
      v-model:open="trendModalOpen"
      :footer="null"
      title="数据源趋势"
      width="1180px"
      :z-index="SRM_NESTED_MODAL_Z_INDEX"
    >
      <Spin :spinning="trendLoading">
        <div class="srm-quarter-trend-modal">
          <div class="srm-quarter-trend-modal__head">
            <strong>
              {{ trendData.supplierName || form.supplierName || '-' }}
            </strong>
            <span>{{ trendData.evalYear || form.evalYear }} 年</span>
          </div>
          <TrendPanel :panels="trendData.panels || []" />
        </div>
      </Spin>
    </AntModal>
  </Page>
</template>

<style scoped>
.srm-crud-page {
  display: flex;
  height: 100%;
  min-height: 0;
  flex-direction: column;
  background: #fff;
}

.srm-crud-page__header {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  border-bottom: 1px solid #e2e8f0;
  background: #f8fafc;
  padding: 12px 16px;
}

.srm-crud-page__title {
  color: #10233d;
  font-size: 16px;
  font-weight: 800;
  line-height: 24px;
}

.srm-quarter-query-panel {
  flex-shrink: 0;
  border-top: 1px solid #edf2f7;
  border-bottom: 1px solid #e2e8f0;
  background: #fff;
  padding: 12px 24px;
}

.srm-quarter-query-row {
  display: flex;
  flex-direction: column;
  width: 100%;
  align-items: stretch;
  gap: 8px;
  min-width: 0;
}

.srm-quarter-query-main-row {
  display: flex;
  width: 100%;
  min-width: 0;
  align-items: center;
}

.srm-quarter-query-form {
  display: flex;
  flex: 0 1 auto;
  flex-wrap: nowrap;
  min-width: 0;
  align-items: center;
  gap: 12px;
}

.srm-quarter-query-form :deep(.ant-form-item) {
  margin-right: 0;
  margin-bottom: 0;
}

.srm-quarter-query-form :deep(.ant-form-item-label > label) {
  height: 32px;
}

.srm-quarter-query-actions {
  flex: 0 0 auto;
  justify-content: flex-end;
  margin-left: auto;
  white-space: nowrap;
}

.srm-quarter-query-actions :deep(.ant-space-item) {
  flex-shrink: 0;
}

.srm-quarter-query-actions :deep(.ant-tag) {
  max-width: 180px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.srm-quarter-panel-toggle {
  flex-shrink: 0;
}

.srm-quarter-todo-item {
  white-space: nowrap;
}

.srm-quarter-todo-item :deep(.ant-checkbox-wrapper) {
  align-items: center;
  height: 32px;
}

.srm-query-input {
  width: 220px;
}

.srm-period-control {
  display: grid;
  align-items: center;
  grid-template-columns: 32px 96px 86px 32px;
}

.srm-period-control :deep(.ant-btn),
.srm-period-control :deep(.ant-input-number),
.srm-period-control :deep(.ant-select-selector) {
  border-radius: 0;
}

.srm-period-control :deep(.ant-btn:first-child) {
  border-radius: 6px 0 0 6px;
}

.srm-period-control :deep(.ant-btn:last-child) {
  border-radius: 0 6px 6px 0;
}

.srm-period-control :deep(.ant-input-number),
.srm-period-control :deep(.ant-select) {
  margin-left: -1px;
}

.srm-period-control :deep(.ant-btn:last-child) {
  margin-left: -1px;
}

.srm-query-year {
  width: 96px;
}

.srm-query-quarter {
  width: 86px;
}

.srm-query-select {
  width: 120px;
}

.srm-query-status {
  width: 140px;
}

.srm-crud-page__body {
  flex: 1 1 0%;
  min-height: 0;
  background: #f1f5f9;
  overflow: hidden;
  padding: 12px 0 16px;
}

.srm-crud-link {
  color: #1d4ed8;
  cursor: pointer;
  font-weight: 700;
}

.srm-crud-link:hover {
  text-decoration: underline;
}

.srm-crud-cell {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  color: #334155;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.srm-detail-section {
  margin-top: 12px;
}

.srm-quarter-split-layout {
  display: grid;
  height: 100%;
  min-height: 0;
  gap: 12px;
  background: transparent;
  grid-template-columns: minmax(0, 1fr);
}

.srm-quarter-split-layout.is-panel-open {
  grid-template-columns: minmax(360px, 3fr) minmax(0, 7fr);
}

.srm-quarter-list-card,
.srm-quarter-items-card {
  display: flex;
  min-width: 0;
  min-height: 0;
  flex-direction: column;
  border: 1px solid #dbe3ef;
  border-radius: 6px;
  background: #fff;
  box-shadow: 0 8px 22px rgb(15 23 42 / 5%);
  overflow: hidden;
}

.srm-quarter-list-card :deep(.vben-vxe-grid),
.srm-quarter-items-card :deep(.ant-spin-nested-loading),
.srm-quarter-items-card :deep(.ant-spin-container) {
  height: 100%;
  min-height: 0;
}

.srm-quarter-list-head {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 58px;
  border-bottom: 1px solid #e2e8f0;
  padding: 10px 12px;
}

.srm-quarter-list-title {
  color: #10233d;
  font-size: 14px;
  font-weight: 800;
  line-height: 20px;
}

.srm-quarter-list-subtitle {
  margin-top: 3px;
  color: #64748b;
  font-size: 12px;
  line-height: 18px;
}

.srm-quarter-table-mode,
.srm-quarter-card-mode {
  flex: 1 1 0%;
  min-height: 0;
}

.srm-quarter-table-mode {
  display: flex;
  flex-direction: column;
}

.srm-quarter-card-mode {
  display: flex;
  flex-direction: column;
  background: #f8fafc;
}

.srm-quarter-card-mode :deep(.ant-spin-nested-loading),
.srm-quarter-card-mode :deep(.ant-spin-container) {
  flex: 1 1 0%;
  min-height: 0;
}

.srm-quarter-card-list {
  display: flex;
  height: 100%;
  min-height: 0;
  flex-direction: column;
  gap: 8px;
  overflow: auto;
  padding: 10px;
}

.srm-quarter-eval-card {
  display: grid;
  min-height: 74px;
  cursor: pointer;
  align-items: center;
  gap: 10px;
  border: 1px solid #dbe3ef;
  border-radius: 6px;
  background: #fff;
  grid-template-columns: 24px minmax(0, 1fr) 32px;
  padding: 10px;
  transition:
    border-color 0.16s ease,
    box-shadow 0.16s ease;
}

.srm-quarter-eval-card:hover,
.srm-quarter-eval-card.is-current {
  border-color: #1677ff;
  box-shadow: 0 8px 18px rgb(22 119 255 / 12%);
}

.srm-quarter-eval-card.is-selected {
  background: #eff6ff;
}

.srm-quarter-eval-card__check {
  align-self: start;
  padding-top: 2px;
}

.srm-quarter-eval-card__body {
  min-width: 0;
}

.srm-quarter-eval-card__top {
  display: flex;
  min-width: 0;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.srm-quarter-eval-card__top strong {
  min-width: 0;
  overflow: hidden;
  color: #10233d;
  font-size: 14px;
  font-weight: 800;
  line-height: 20px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.srm-quarter-eval-card__top span {
  flex-shrink: 0;
  border-radius: 4px;
  background: #f1f5f9;
  color: #475569;
  font-size: 12px;
  line-height: 20px;
  padding: 0 6px;
}

.srm-quarter-eval-card__meta {
  display: grid;
  margin-top: 8px;
  gap: 6px;
  grid-template-columns: minmax(0, 1fr) 64px 86px;
}

.srm-quarter-eval-card__meta span {
  min-width: 0;
  overflow: hidden;
  color: #64748b;
  font-size: 12px;
  line-height: 18px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.srm-quarter-eval-card__more {
  align-self: start;
}

.srm-quarter-card-empty {
  display: flex;
  height: 100%;
  min-height: 260px;
  align-items: center;
  justify-content: center;
  color: #64748b;
  font-size: 13px;
}

.srm-quarter-card-pager {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  border-top: 1px solid #e2e8f0;
  background: #fff;
  color: #475569;
  font-size: 12px;
  padding: 8px 10px;
}

.srm-preview-panel {
  display: flex;
  height: 100%;
  min-height: 0;
  flex-direction: column;
  gap: 10px;
  padding: 14px 16px 16px;
}

.srm-preview-panel__head {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 32px;
}

.srm-preview-panel__title {
  color: #10233d;
  font-size: 14px;
  font-weight: 800;
  line-height: 20px;
}

.srm-preview-panel__subtitle {
  margin-top: 4px;
  color: #64748b;
  font-size: 12px;
  line-height: 18px;
}

.srm-preview-panel__stats {
  display: flex;
  flex-shrink: 0;
  flex-wrap: wrap;
  gap: 8px;
  color: #475569;
  font-size: 12px;
  line-height: 18px;
}

.srm-preview-panel__stats span {
  border: 1px solid #dbe3ef;
  border-radius: 4px;
  background: #f8fafc;
  padding: 2px 8px;
}

.srm-preview-panel__empty {
  display: flex;
  flex: 1;
  min-height: 260px;
  align-items: center;
  justify-content: center;
  background: #f8fafc;
  color: #64748b;
  font-size: 13px;
}

.srm-basic-summary {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 0;
  border-top: 1px solid #e2e8f0;
}

.srm-basic-summary__item {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 8px;
  border-right: 1px solid #e2e8f0;
  padding: 9px 12px;
}

.srm-basic-summary__item:last-child {
  border-right: 0;
}

.srm-basic-summary__item span {
  flex-shrink: 0;
  color: #64748b;
  font-size: 12px;
}

.srm-basic-summary__item strong {
  min-width: 0;
  overflow: hidden;
  color: #10233d;
  font-size: 13px;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.srm-generate-form-grid {
  display: grid;
  align-items: start;
  grid-template-columns: 292px minmax(180px, 1fr) 130px 116px;
  gap: 12px;
}

.srm-generate-template-note {
  display: flex;
  grid-column: 1 / -1;
  min-width: 0;
  align-items: center;
  color: #475569;
  font-size: 13px;
  line-height: 20px;
}

.srm-generate-hint {
  margin-bottom: 10px;
  border: 1px solid #dbeafe;
  border-radius: 6px;
  background: #eff6ff;
  color: #1e40af;
  font-size: 13px;
  line-height: 20px;
  padding: 8px 10px;
}

.srm-nowrap-action {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  white-space: nowrap;
}

.srm-number-input {
  width: 120px;
}

.srm-score-input {
  width: 96px;
}

.srm-score-description {
  min-width: 220px;
}

.srm-detail-bottom-tabs {
  overflow: hidden;
  padding: 0;
}

.srm-detail-tabs :deep(.ant-tabs-nav) {
  margin: 0;
  border-bottom: 1px solid #e2e8f0;
  padding: 0 14px;
}

.srm-detail-tabs :deep(.ant-tabs-content-holder) {
  padding: 12px;
}

.srm-detail-tab-context {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
  color: #64748b;
  font-size: 13px;
  line-height: 20px;
}

.srm-detail-tab-context strong {
  color: #10233d;
  font-weight: 800;
}

.srm-detail-tab-empty {
  display: flex;
  min-height: 220px;
  align-items: center;
  justify-content: center;
  background: #f8fafc;
  color: #64748b;
  font-size: 13px;
}

.srm-detail-trend-panel {
  max-height: 560px;
  overflow: auto;
}

.srm-quarter-item-table :deep(.ant-table-tbody > tr) {
  cursor: pointer;
}

.srm-quarter-item-table
  :deep(.ant-table-tbody > tr.srm-quarter-item-row--selected > td) {
  background: #eff6ff !important;
}

.srm-quarter-item-table
  :deep(.ant-table-tbody > tr.srm-quarter-item-row--selected:hover > td) {
  background: #dbeafe !important;
}

.srm-quarter-trend-modal {
  max-height: 72vh;
  overflow: auto;
}

.srm-quarter-trend-modal__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
  color: #10233d;
  font-size: 13px;
  line-height: 20px;
}

.srm-quarter-trend-modal__head span {
  color: #64748b;
}

:deep(.vben-vxe-grid) {
  display: flex;
  height: 100%;
  flex-direction: column;
}

@media (max-width: 1280px) {
  .srm-quarter-query-row {
    flex-wrap: wrap;
  }

  .srm-quarter-query-form {
    flex-wrap: wrap;
  }

  .srm-quarter-query-actions {
    width: 100%;
  }

  .srm-generate-form-grid {
    grid-template-columns: 1fr 1fr;
  }

  .srm-quarter-split-layout {
    grid-template-columns: 1fr;
  }

  .srm-quarter-items-card {
    min-height: 360px;
  }

  .srm-basic-summary {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}
</style>
