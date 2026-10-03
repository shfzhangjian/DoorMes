<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesHcIntermediateStockLedgerApi } from '#/api/mes/hc/execution/intermediate-stock-ledger';

import { computed, h, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';
import { buildSortingField } from '@vben/request';
import { downloadFileFromBlobPart } from '@vben/utils';

import { Button, DatePicker, Input, InputNumber, message, Modal as AModal, Segmented, Select, Switch, Tabs, TabPane, Tag } from 'ant-design-vue';
import dayjs from 'dayjs';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  exportIntermediateStockHistoryTemplate,
  getIntermediateStockLedgerAggregatePage,
  getIntermediateStockLedgerPage,
  importIntermediateStockHistory,
  releaseIntermediateStockLedgerLock,
} from '#/api/mes/hc/execution/intermediate-stock-ledger';

import '../../package-fg/shared/cut-round-board.css';
import {
  SORTABLE_FIELD_OPTIONS,
  STAGE_COLOR,
  STAGE_OPTIONS,
  QUALITY_STATUS_OPTIONS,
  STOCK_STATUS_COLOR,
  STOCK_STATUS_LABEL,
  useGridColumns,
} from './data';

defineOptions({ name: 'MesHcExecutionIntermediateStockLedger' });

type QueryState = Omit<MesHcIntermediateStockLedgerApi.PageReqVO, 'pageNo' | 'pageSize'>;
type SortOrder = 'asc' | 'desc';
type SortState = { field: string; label: string; order: SortOrder };
type ViewMode = 'DETAIL' | 'AGGREGATE';
type DrillMetric =
  | 'availableQty'
  | 'consumedQty'
  | 'lockRemainingQty'
  | 'onHandQty'
  | 'planLockedQty'
  | 'releasedQty'
  | 'shareableQty';

type LedgerPageResult = {
  list?: MesHcIntermediateStockLedgerApi.Ledger[];
  total?: number;
};

const activeTab = ref('ALL');
const advancedQueryVisible = ref(false);
const aggregateDrillLoading = ref(false);
const aggregateDrillRows = ref<MesHcIntermediateStockLedgerApi.Ledger[]>([]);
const aggregateDrillTitle = ref('');
const aggregateDrillVisible = ref(false);
const currentDateTime = ref(dayjs().format('YYYY-MM-DD HH:mm:ss'));
const historyImportInputRef = ref<HTMLInputElement>();
const historyImporting = ref(false);
const historyTemplateExporting = ref(false);
const newSortField = ref<string>();
const pageRows = ref<MesHcIntermediateStockLedgerApi.Ledger[]>([]);
const pageTotal = ref(0);
const sortConfigVisible = ref(false);
const sourceDetailVisible = ref(false);
const sourceDetailRow = ref<MesHcIntermediateStockLedgerApi.Ledger | null>(null);
const releaseModalVisible = ref(false);
const releaseSubmitting = ref(false);
const releaseTargetRow = ref<MesHcIntermediateStockLedgerApi.Ledger | null>(null);
const txnDetailVisible = ref(false);
const txnDebugVisible = ref(false);
const txnDetailRow = ref<MesHcIntermediateStockLedgerApi.Ledger | null>(null);
const activeSortFields = ref<SortState[]>([]);
const viewMode = ref<ViewMode>('DETAIL');
let currentTimer: ReturnType<typeof setInterval> | null = null;
let countRequestSeq = 0;
let ledgerRequestSeq = 0;
let queryFirstPageChain: Promise<void> = Promise.resolve();
let queryFirstPageSeq = 0;

const MODAL_Z_INDEX = {
  advancedQuery: 1200,
  sortConfig: 1210,
  sourceDetail: 1220,
  aggregateDrill: 1230,
  txnDetail: 1280,
  release: 1320,
};

const VIEW_MODE_OPTIONS = [
  { label: '明细', value: 'DETAIL' },
  { label: '母卷聚合', value: 'AGGREGATE' },
];

const DRILL_METRIC_LABEL: Record<DrillMetric, string> = {
  availableQty: '可用量',
  consumedQty: '已消耗',
  lockRemainingQty: '锁定剩余',
  onHandQty: '在库量',
  planLockedQty: '计划锁定',
  releasedQty: '已释放',
  shareableQty: '可利库量',
};

const DRILL_METRIC_STATUS: Partial<Record<DrillMetric, string>> = {
  availableQty: 'AVAILABLE',
  consumedQty: 'CONSUMED',
  planLockedQty: 'LOCKED',
  shareableQty: 'AVAILABLE',
};

const queryParams = reactive<QueryState>({
  batchNo: '',
  keyword: '',
  materialCode: '',
  modelNo: '',
  onlyAvailable: false,
  qualityStatus: '',
  sourceBatchNo: '',
  sourceParentBatchNo: '',
  sourcePlanNo: '',
  sourceType: '',
  txnDateEnd: '',
  txnDateStart: '',
});

const advancedQuery = reactive<QueryState>({ ...queryParams });
const releaseForm = reactive({
  releaseQty: 0,
  releaseReason: '',
});

const statusTabCounts = reactive({
  ALL: 0,
  AVAILABLE: 0,
  CONSUMED: 0,
  FROZEN: 0,
  LOCKED: 0,
});

const availableRows = computed(() =>
  pageRows.value.filter((item) => Number(item.availableQty || 0) > 0).length,
);

const activeQueryTags = computed(() => {
  const tags: Array<{ key: keyof QueryState; label: string; value: string }> = [];
  const addTag = (key: keyof QueryState, label: string, value?: string | boolean) => {
    if (value === undefined || value === null || value === '' || value === false) return;
    tags.push({ key, label, value: value === true ? '是' : String(value) });
  };
  addTag('sourcePlanNo', '计划号', queryParams.sourcePlanNo);
  addTag('batchNo', '中间品批号', queryParams.batchNo);
  addTag('sourceBatchNo', '来源批号', queryParams.sourceBatchNo);
  addTag('sourceParentBatchNo', '母卷批次号', queryParams.sourceParentBatchNo);
  addTag('materialCode', '物料编码', queryParams.materialCode);
  addTag('modelNo', '型号', queryParams.modelNo);
  addTag('sourceType', '工序', STAGE_OPTIONS.find((item) => item.value === queryParams.sourceType)?.label);
  addTag('qualityStatus', '质量状态', queryParams.qualityStatus);
  addTag('txnDateStart', '过账日期起', queryParams.txnDateStart);
  addTag('txnDateEnd', '过账日期止', queryParams.txnDateEnd);
  addTag('onlyAvailable', '只看可用', queryParams.onlyAvailable);
  return tags;
});

const sortFieldLabelMap = computed(() =>
  SORTABLE_FIELD_OPTIONS.reduce<Record<string, string>>((map, item) => {
    map[item.value] = item.label;
    return map;
  }, {}),
);

const availableSortOptions = computed(() => {
  const selectedFields = new Set(activeSortFields.value.map((item) => item.field));
  return SORTABLE_FIELD_OPTIONS.filter((item) => !selectedFields.has(item.value));
});

function formatTabCount(count: number) {
  return count > 100 ? '99+' : String(count);
}

function formatSortOrder(order: SortOrder) {
  return order === 'asc' ? '升序' : '降序';
}

function getSortIndex(field?: string) {
  const index = activeSortFields.value.findIndex((item) => item.field === field);
  return index >= 0 ? index + 1 : undefined;
}

function normalizeSorts(sorts?: Array<{ field?: string; order?: string }>): SortState[] {
  if (!sorts || sorts.length === 0) {
    return [];
  }
  const fields = new Set<string>();
  return sorts.reduce<SortState[]>((list, item) => {
    const field = item.field;
    const order = item.order;
    if (!field || fields.has(field) || (order !== 'asc' && order !== 'desc')) {
      return list;
    }
    const label = sortFieldLabelMap.value[field];
    if (!label) {
      return list;
    }
    fields.add(field);
    list.push({ field, label, order });
    return list;
  }, []);
}

function buildQueryParams(): MesHcIntermediateStockLedgerApi.PageReqVO {
  return {
    ...queryParams,
    stockStatus: activeTab.value === 'ALL' ? undefined : activeTab.value,
  };
}

function buildCountQueryParams(
  stockStatus?: string,
  querySnapshot: QueryState = { ...queryParams },
): MesHcIntermediateStockLedgerApi.PageReqVO {
  return {
    ...querySnapshot,
    pageNo: 1,
    pageSize: 1,
    stockStatus,
  };
}

function getRequestMessage(error: unknown) {
  const data = (error as any)?.response?.data || (error as any)?.data || error || {};
  return String(
    data?.msg
      || data?.message
      || data?.data?.msg
      || data?.data?.message
      || (error as any)?.msg
      || (error as any)?.message
      || '',
  );
}

function isInvalidPageResult(value: unknown) {
  const result = value as LedgerPageResult;
  return !result || !Array.isArray(result.list) || !Number.isFinite(Number(result.total ?? 0));
}

function normalizePageResult(value: unknown, moduleName: string): Required<LedgerPageResult> {
  if (!isInvalidPageResult(value)) {
    const result = value as LedgerPageResult;
    return {
      list: result.list || [],
      total: Number(result.total || 0),
    };
  }
  const messageText = getRequestMessage(value);
  const code = (value as any)?.code ?? (value as any)?.data?.code;
  if (code === 401 || messageText.includes('未登录')) {
    throw new Error(`${moduleName}加载失败：登录状态已刷新，请重新点击查询。`);
  }
  throw new Error(messageText || `${moduleName}加载失败：后台返回的数据结构不正确。`);
}

async function fetchLedgerPage(params: MesHcIntermediateStockLedgerApi.PageReqVO, mode: ViewMode) {
  const result = mode === 'AGGREGATE'
    ? await getIntermediateStockLedgerAggregatePage(params)
    : await getIntermediateStockLedgerPage(params);
  return normalizePageResult(result, '工序中间品库台账');
}

function keepCurrentLedgerResult() {
  return {
    list: pageRows.value,
    total: pageTotal.value,
  };
}

function closeFloatingModals(exclude?: 'aggregateDrill' | 'sourceDetail' | 'txnDetail') {
  if (exclude !== 'sourceDetail') {
    sourceDetailVisible.value = false;
  }
  if (exclude !== 'aggregateDrill') {
    aggregateDrillVisible.value = false;
  }
  if (exclude !== 'txnDetail') {
    txnDetailVisible.value = false;
  }
}

const [Grid, gridApi] = useVbenVxeGrid({
  class: 'process-output-balance-vben-grid',
  gridClass: 'process-output-balance-vxe-grid',
  gridOptions: {
    border: true,
    columns: useGridColumns(),
    height: 'auto',
    keepSource: true,
    pagerConfig: { enabled: true, pageSize: 20, pageSizes: [20, 50, 100] },
    proxyConfig: {
      ajax: {
        query: async ({ page, sorts }) => {
          const requestSeq = ++ledgerRequestSeq;
          const modeSnapshot = viewMode.value;
          const sortFields = normalizeSorts(sorts);
          const querySnapshot = buildQueryParams();
          const sortingFields = buildSortingField(sortFields);
          const pageSize = Number(page.pageSize || 20);
          const pageNo = Math.max(1, Number(page.currentPage || 1));
          activeSortFields.value = sortFields;
          const loadPage = (currentPage: number) =>
            fetchLedgerPage({
              ...querySnapshot,
              ...sortingFields,
              pageNo: currentPage,
              pageSize,
            }, modeSnapshot);
          let result: Required<LedgerPageResult>;
          try {
            result = await loadPage(pageNo);
          } catch (error) {
            if (requestSeq === ledgerRequestSeq) {
              message.warning(getRequestMessage(error) || '台账查询失败，已保留当前表格数据。');
            }
            return keepCurrentLedgerResult();
          }
          if (requestSeq !== ledgerRequestSeq) {
            return keepCurrentLedgerResult();
          }
          if ((result.total || 0) > 0 && (result.list || []).length === 0 && pageNo > 1) {
            await (gridApi.grid as any)?.setCurrentPage?.(1);
            try {
              result = await loadPage(1);
            } catch (error) {
              if (requestSeq === ledgerRequestSeq) {
                message.warning(getRequestMessage(error) || '台账第一页补查失败，已保留当前表格数据。');
              }
              return keepCurrentLedgerResult();
            }
            if (requestSeq !== ledgerRequestSeq) {
              return keepCurrentLedgerResult();
            }
          }
          pageRows.value = result.list || [];
          pageTotal.value = result.total || 0;
          return result;
        },
      },
      autoLoad: false,
      sort: true,
    },
    rowConfig: {
      height: 46,
      isHover: true,
      keyField: 'id',
    },
    sortConfig: {
      chronological: true,
      multiple: true,
      remote: true,
    },
    toolbarConfig: { enabled: false },
  } as VxeTableGridOptions<MesHcIntermediateStockLedgerApi.Ledger>,
});

async function refreshTabCounts(
  modeSnapshot: ViewMode = viewMode.value,
  querySnapshot: QueryState = { ...queryParams },
) {
  const requestSeq = ++countRequestSeq;
  const nextCounts = { ...statusTabCounts };
  const countQueries: Array<[keyof typeof statusTabCounts, string | undefined]> = [
    ['ALL', undefined],
    ['AVAILABLE', 'AVAILABLE'],
    ['LOCKED', 'LOCKED'],
    ['FROZEN', 'FROZEN'],
    ['CONSUMED', 'CONSUMED'],
  ];
  try {
    for (const [key, stockStatus] of countQueries) {
      const result = await fetchLedgerPage(buildCountQueryParams(stockStatus, querySnapshot), modeSnapshot);
      if (requestSeq !== countRequestSeq) return;
      nextCounts[key] = result.total || 0;
    }
    if (requestSeq !== countRequestSeq) return;
    Object.assign(statusTabCounts, nextCounts);
  } catch (error) {
    if (requestSeq === countRequestSeq) {
      message.warning(getRequestMessage(error) || '台账状态数量刷新失败，已保留当前数量。');
    }
  }
}

function queryFirstPage() {
  const seq = ++queryFirstPageSeq;
  queryFirstPageChain = queryFirstPageChain
    .catch(() => undefined)
    .then(async () => {
      if (seq !== queryFirstPageSeq) {
        return;
      }
      const modeSnapshot = viewMode.value;
      const querySnapshot = { ...queryParams };
      await nextTick();
      await (gridApi.grid as any)?.setCurrentPage?.(1);
      await gridApi.query();
      void refreshTabCounts(modeSnapshot, querySnapshot);
    });
  return queryFirstPageChain;
}

function applyQuery() {
  void queryFirstPage();
}

function handleHistoryImportClick() {
  historyImportInputRef.value?.click();
}

function showHistoryImportFailures(resp: MesHcIntermediateStockLedgerApi.HistoryImportResp) {
  const failures = resp.failures || [];
  const text = failures.slice(0, 30).join('\n');
  const moreText = failures.length > 30 ? `\n... 还有 ${failures.length - 30} 条未显示` : '';
  AModal.warning({
    title: '历史中间品台账导入校验未通过',
    width: 760,
    content: h('div', { class: 'space-y-2' }, [
      h('div', `读取 ${resp.totalRows || 0} 行，跳过 ${resp.skippedRows || 0} 行，失败 ${resp.failureCount || 0} 条。`),
      h(
        'pre',
        {
          style: 'white-space: pre-wrap; margin: 0; max-height: 360px; overflow: auto; font-size: 12px;',
        },
        text + moreText,
      ),
    ]),
  });
}

async function handleExportHistoryTemplate() {
  historyTemplateExporting.value = true;
  try {
    const data = await exportIntermediateStockHistoryTemplate({
      ...buildQueryParams(),
      pageNo: 1,
      pageSize: 1,
    });
    downloadFileFromBlobPart({ fileName: '工序中间品历史导入模板.xlsx', source: data });
  } finally {
    historyTemplateExporting.value = false;
  }
}

async function handleHistoryImportFileChange(event: Event) {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  input.value = '';
  if (!file) return;

  const hideLoading = message.loading({ content: '正在导入历史中间品台账...', duration: 0 });
  historyImporting.value = true;
  try {
    const resp = await importIntermediateStockHistory(file);
    if ((resp.failureCount || 0) > 0) {
      showHistoryImportFailures(resp);
      return;
    }
    message.success(`导入成功：新增 ${resp.createdCount || 0} 条，更新 ${resp.updatedCount || 0} 条，跳过 ${resp.skippedRows || 0} 行`);
    await queryFirstPage();
  } finally {
    historyImporting.value = false;
    hideLoading();
  }
}

function handleViewModeChange(value: string | number) {
  viewMode.value = value as ViewMode;
  void queryFirstPage();
}

async function applySortFields(sortFields: SortState[]) {
  const previousFields = new Set(activeSortFields.value.map((item) => item.field));
  const nextFields = new Set(sortFields.map((item) => item.field));
  const removedFields = [...previousFields].filter((field) => !nextFields.has(field));

  if (sortFields.length === 0) {
    await (gridApi.grid as any)?.clearSort?.();
  } else {
    for (const field of removedFields) {
      await (gridApi.grid as any)?.clearSort?.(field);
    }
    await (gridApi.grid as any)?.sort?.(sortFields.map(({ field, order }) => ({ field, order })));
  }
  activeSortFields.value = sortFields;
  await queryFirstPage();
}

function addSortField(field?: string) {
  if (!field || activeSortFields.value.some((item) => item.field === field)) {
    return;
  }
  const label = sortFieldLabelMap.value[field];
  if (!label) {
    return;
  }
  newSortField.value = undefined;
  void applySortFields([...activeSortFields.value, { field, label, order: 'asc' }]);
}

function moveSortField(index: number, offset: number) {
  const nextIndex = index + offset;
  if (nextIndex < 0 || nextIndex >= activeSortFields.value.length) {
    return;
  }
  const next = [...activeSortFields.value];
  const [item] = next.splice(index, 1);
  next.splice(nextIndex, 0, item);
  void applySortFields(next);
}

function toggleSortOrder(field: string) {
  const next = activeSortFields.value.map((item) => {
    const order: SortOrder = item.order === 'asc' ? 'desc' : 'asc';
    return item.field === field ? { ...item, order } : item;
  });
  void applySortFields(next);
}

function removeSortField(field: string) {
  void applySortFields(activeSortFields.value.filter((item) => item.field !== field));
}

function clearSortFields() {
  void applySortFields([]);
}

function resetQuery() {
  Object.assign(queryParams, {
    batchNo: '',
    keyword: '',
    materialCode: '',
    modelNo: '',
    onlyAvailable: false,
    qualityStatus: '',
    sourceBatchNo: '',
    sourceParentBatchNo: '',
    sourcePlanNo: '',
    sourceType: '',
    txnDateEnd: '',
    txnDateStart: '',
  });
  activeTab.value = 'ALL';
  clearSortFields();
}

function openAdvancedQuery() {
  closeFloatingModals();
  sortConfigVisible.value = false;
  Object.assign(advancedQuery, queryParams);
  advancedQueryVisible.value = true;
}

function openSortConfig() {
  closeFloatingModals();
  advancedQueryVisible.value = false;
  sortConfigVisible.value = true;
}

function applyAdvancedQuery() {
  Object.assign(queryParams, advancedQuery);
  advancedQueryVisible.value = false;
  applyQuery();
}

function removeQueryCondition(key: keyof QueryState) {
  (queryParams[key] as any) = key === 'onlyAvailable' ? false : '';
  applyQuery();
}

function openSourceDetail(row: MesHcIntermediateStockLedgerApi.Ledger) {
  closeFloatingModals('sourceDetail');
  advancedQueryVisible.value = false;
  sortConfigVisible.value = false;
  sourceDetailRow.value = row;
  sourceDetailVisible.value = true;
}

function handleCellDblClick(row: MesHcIntermediateStockLedgerApi.Ledger) {
  if (isAggregateRow(row)) {
    void openAggregateDrill(row, 'onHandQty');
    return;
  }
  openSourceDetail(row);
}

function getTxnDetails(row?: MesHcIntermediateStockLedgerApi.Ledger | null) {
  return row?.txnDetails || [];
}

function getTxnRemarkParts(detail?: MesHcIntermediateStockLedgerApi.TxnDetail) {
  return String(detail?.remark || '')
    .split(/[;；]/)
    .map((item) => item.trim())
    .filter(Boolean);
}

function isTxnDebugRemarkPart(value: string) {
  return /(?:记录|流水|单据)[:：]/.test(value);
}

function getTxnBusinessRemark(detail?: MesHcIntermediateStockLedgerApi.TxnDetail) {
  return getTxnRemarkParts(detail)
    .filter((item) => !isTxnDebugRemarkPart(item))
    .join('；');
}

function getTxnDebugRemark(detail?: MesHcIntermediateStockLedgerApi.TxnDetail) {
  return getTxnRemarkParts(detail)
    .filter(isTxnDebugRemarkPart)
    .join('；');
}

function hasTxnDetails(row?: MesHcIntermediateStockLedgerApi.Ledger | null) {
  return getTxnDetails(row).length > 0;
}

function getTxnSummaryPreview(row: MesHcIntermediateStockLedgerApi.Ledger) {
  return getTxnDetails(row)[0]?.summary || row.txnSummary || row.businessRemark || '-';
}

function openTxnDetails(row: MesHcIntermediateStockLedgerApi.Ledger) {
  advancedQueryVisible.value = false;
  sortConfigVisible.value = false;
  sourceDetailVisible.value = false;
  txnDebugVisible.value = false;
  txnDetailRow.value = row;
  txnDetailVisible.value = true;
}

function isAggregateRow(row?: MesHcIntermediateStockLedgerApi.Ledger | null) {
  return String(row?.rowType || '').toUpperCase() === 'GROUP';
}

function getRowMotherBatchNo(row?: MesHcIntermediateStockLedgerApi.Ledger | null) {
  return row?.motherBatchNo || row?.sourceParentBatchNo || row?.sourceBatchNo || row?.batchNo || '';
}

function formatLedgerQty(value?: number | string, uom?: string) {
  if (value === null || value === undefined || value === '') {
    return '-';
  }
  const unit = uom ? ` ${uom}` : '';
  return `${Number(value).toFixed(3)}${unit}`;
}

function canDrillQuantity(row: MesHcIntermediateStockLedgerApi.Ledger, field?: string) {
  const metric = field as DrillMetric;
  return isAggregateRow(row) && Boolean(DRILL_METRIC_LABEL[metric]);
}

function openAggregateDrillByField(row: MesHcIntermediateStockLedgerApi.Ledger, field?: string) {
  if (!canDrillQuantity(row, field)) {
    return;
  }
  void openAggregateDrill(row, field as DrillMetric);
}

async function openAggregateDrill(row: MesHcIntermediateStockLedgerApi.Ledger, metric: DrillMetric) {
  const motherBatchNo = getRowMotherBatchNo(row);
  closeFloatingModals('aggregateDrill');
  advancedQueryVisible.value = false;
  sortConfigVisible.value = false;
  aggregateDrillTitle.value = `${motherBatchNo || '-'} / ${row.opName || row.sourceType || '-'} / ${DRILL_METRIC_LABEL[metric]}`;
  aggregateDrillVisible.value = true;
  aggregateDrillLoading.value = true;
  aggregateDrillRows.value = [];
  try {
    const result = normalizePageResult(await getIntermediateStockLedgerPage({
      ...queryParams,
      onlyAvailable: false,
      pageNo: 1,
      pageSize: 200,
      sourceParentBatchNo: motherBatchNo,
      sourceType: row.sourceType,
      stockStatus: DRILL_METRIC_STATUS[metric],
    }), '聚合片号钻取');
    const detailRows = (result.list || []).filter((item) => {
      const itemMotherBatchNo = getRowMotherBatchNo(item);
      const metricValue = Number((item as any)[metric] || 0);
      const metricMatched = metric === 'onHandQty' ? true : metricValue > 0;
      return !isAggregateRow(item)
        && String(item.sourceType || '').toUpperCase() === String(row.sourceType || '').toUpperCase()
        && String(itemMotherBatchNo || '').toUpperCase() === String(motherBatchNo || '').toUpperCase()
        && metricMatched;
    });
    aggregateDrillRows.value = detailRows;
  } catch (error) {
    message.warning(getRequestMessage(error) || '聚合片号钻取失败，请重新点击数量。');
  } finally {
    aggregateDrillLoading.value = false;
  }
}

function formatTxnQty(value?: number, uom?: string) {
  if (value === null || value === undefined) {
    return '-';
  }
  const unit = uom ? ` ${uom}` : '';
  return `${Number(value).toFixed(3)}${unit}`;
}

function formatTxnTime(value?: string) {
  if (!value) {
    return '-';
  }
  const parsedValue = dayjs(value);
  return parsedValue.isValid() ? parsedValue.format('YYYY-MM-DD HH:mm:ss') : value;
}

function getTxnDetailColor(eventType?: string) {
  const type = String(eventType || '').toUpperCase();
  if (type === 'CONSUME') return 'red';
  if (type === 'RELEASE') return 'green';
  if (type === 'LOCK') return 'blue';
  return 'default';
}

function canReleaseSourceLock(row: MesHcIntermediateStockLedgerApi.Ledger) {
  return Boolean(
    !isAggregateRow(row) &&
      row.activeLockId &&
      String(row.activeLockStatus || 'ACTIVE').toUpperCase() === 'ACTIVE' &&
      Number(row.activeLockRemainingQty || 0) > 0,
  );
}

function getReleaseMaxQty() {
  return Math.max(Number(releaseTargetRow.value?.activeLockRemainingQty || 0), 0);
}

function normalizeReleaseQty() {
  const maxQty = getReleaseMaxQty();
  releaseForm.releaseQty = Math.min(Math.max(Number(releaseForm.releaseQty || 0), 0), maxQty);
}

function openReleaseSourceLock(row: MesHcIntermediateStockLedgerApi.Ledger) {
  if (!canReleaseSourceLock(row)) {
    message.warning('当前中间品没有可释放的锁定余量');
    return;
  }
  releaseTargetRow.value = row;
  releaseForm.releaseQty = Number(row.activeLockRemainingQty || 0);
  releaseForm.releaseReason = `工序中间品库台账释放余料：${row.batchNo || row.id}`;
  releaseModalVisible.value = true;
}

function closeReleaseSourceLock() {
  releaseModalVisible.value = false;
  releaseTargetRow.value = null;
  releaseForm.releaseQty = 0;
  releaseForm.releaseReason = '';
}

async function confirmReleaseSourceLock() {
  const row = releaseTargetRow.value;
  if (!row?.activeLockId) {
    message.warning('当前中间品没有可释放的锁定记录');
    return;
  }
  normalizeReleaseQty();
  if (releaseForm.releaseQty <= 0) {
    message.warning('释放数量必须大于0');
    return;
  }
  const releaseReason = releaseForm.releaseReason.trim();
  if (!releaseReason) {
    message.warning('请填写释放原因');
    return;
  }
  releaseSubmitting.value = true;
  try {
    await releaseIntermediateStockLedgerLock({
      lockId: row.activeLockId,
      releaseQty: releaseForm.releaseQty,
      releaseReason,
    });
    message.success('锁定余量已释放');
    closeReleaseSourceLock();
    await queryFirstPage();
  } finally {
    releaseSubmitting.value = false;
  }
}

onMounted(() => {
  currentTimer = setInterval(() => {
    currentDateTime.value = dayjs().format('YYYY-MM-DD HH:mm:ss');
  }, 1000);
  void queryFirstPage();
});

onBeforeUnmount(() => {
  if (currentTimer) clearInterval(currentTimer);
});
</script>

<template>
  <Page auto-content-height>
    <input
      ref="historyImportInputRef"
      type="file"
      accept=".xlsx,.xls"
      class="hidden"
      @change="handleHistoryImportFileChange"
    />
    <div class="package-fg-console process-output-balance-console">
      <section class="prototype-banner process-output-balance-banner">
        <span class="console-main-icon">
          <IconifyIcon icon="lucide:warehouse" />
        </span>
        <div class="console-title-block">
          <div class="console-title-row">
            <h2 class="console-title-text">工序中间品库台账</h2>
            <Tag color="processing" class="console-title-tag">车间执行</Tag>
          </div>
          <div class="console-meta-row">
            <span class="console-meta-item">
              <span class="console-meta-label">总数</span>
              <span class="console-meta-value">{{ statusTabCounts.ALL }}</span>
            </span>
            <span class="console-meta-item">
              <span class="console-meta-label">本页可用</span>
              <span class="console-meta-value">{{ availableRows }}</span>
            </span>
            <span class="console-meta-item">
              <span class="console-meta-label">当前状态</span>
              <span class="console-meta-value">
                {{ activeTab === 'ALL' ? '全部' : STOCK_STATUS_LABEL[activeTab] || activeTab }}
              </span>
            </span>
            <span class="console-meta-item">
              <span class="console-meta-label">当前视图</span>
              <span class="console-meta-value">{{ viewMode === 'AGGREGATE' ? '母卷聚合' : '明细' }}</span>
            </span>
          </div>
        </div>
        <div class="work-time-card">
          <div>{{ currentDateTime.slice(0, 10) }}</div>
          <strong>{{ currentDateTime.slice(11) }}</strong>
        </div>
        <div class="console-action-group process-output-balance-action-group">
          <button class="action-tile" type="button" @click="applyQuery">
            <IconifyIcon icon="lucide:search" />
            <span>查询</span>
          </button>
          <button class="action-tile" type="button" @click="resetQuery">
            <IconifyIcon icon="lucide:rotate-ccw" />
            <span>重置</span>
          </button>
          <button
            class="action-tile"
            type="button"
            :disabled="historyTemplateExporting"
            @click="handleExportHistoryTemplate"
          >
            <IconifyIcon icon="lucide:file-down" />
            <span>导出模板</span>
          </button>
          <button
            class="action-tile"
            type="button"
            :disabled="historyImporting"
            @click="handleHistoryImportClick"
          >
            <IconifyIcon icon="lucide:file-up" />
            <span>历史导入</span>
          </button>
        </div>
      </section>

      <section class="package-fg-filter-bar process-output-balance-query-panel">
        <div class="process-output-balance-simple-query">
          <label class="process-output-balance-query-label">查询</label>
          <Segmented
            v-model:value="viewMode"
            :options="VIEW_MODE_OPTIONS"
            @change="handleViewModeChange"
          />
          <Select
            v-model:value="queryParams.sourceType"
            allow-clear
            :options="STAGE_OPTIONS"
            placeholder="工序"
            @change="applyQuery"
          />
          <Input
            v-model:value="queryParams.keyword"
            allow-clear
            placeholder="计划号 / 中间品批号 / 来源批号 / 物料 / 型号 / 工序"
            @press-enter="applyQuery"
          />
          <Button type="primary" @click="applyQuery">
            <template #icon><IconifyIcon icon="lucide:search" /></template>
            查询
          </Button>
          <Button @click="openAdvancedQuery">
            <template #icon><IconifyIcon icon="lucide:sliders-horizontal" /></template>
            多条件查询
          </Button>
          <Button @click="openSortConfig">
            <template #icon><IconifyIcon icon="lucide:list-ordered" /></template>
            排序
          </Button>
          <div class="process-output-balance-switch">
            <span>只看可用</span>
            <Switch v-model:checked="queryParams.onlyAvailable" checked-children="是" un-checked-children="否" @change="applyQuery" />
          </div>
        </div>
        <div v-if="activeQueryTags.length > 0" class="process-output-balance-query-tags">
          <span v-for="tag in activeQueryTags" :key="tag.key" class="process-output-balance-query-tag">
            <b>{{ tag.label }}</b>
            <span>{{ tag.value }}</span>
            <button type="button" @click="removeQueryCondition(tag.key)">x</button>
          </span>
        </div>
      </section>

      <div class="process-output-balance-body">
        <Tabs v-model:activeKey="activeTab" class="process-output-balance-tabs" @change="applyQuery">
          <TabPane key="ALL">
            <template #tab>
              <span class="status-tab-label">
                全部
                <span class="status-tab-badge">{{ formatTabCount(statusTabCounts.ALL) }}</span>
              </span>
            </template>
          </TabPane>
          <TabPane key="AVAILABLE">
            <template #tab>
              <span class="status-tab-label">
                可用
                <span class="status-tab-badge">{{ formatTabCount(statusTabCounts.AVAILABLE) }}</span>
              </span>
            </template>
          </TabPane>
          <TabPane key="LOCKED">
            <template #tab>
              <span class="status-tab-label">
                已锁定
                <span class="status-tab-badge">{{ formatTabCount(statusTabCounts.LOCKED) }}</span>
              </span>
            </template>
          </TabPane>
          <TabPane key="FROZEN">
            <template #tab>
              <span class="status-tab-label">
                冻结
                <span class="status-tab-badge">{{ formatTabCount(statusTabCounts.FROZEN) }}</span>
              </span>
            </template>
          </TabPane>
          <TabPane key="CONSUMED">
            <template #tab>
              <span class="status-tab-label">
                已耗尽
                <span class="status-tab-badge">{{ formatTabCount(statusTabCounts.CONSUMED) }}</span>
              </span>
            </template>
          </TabPane>
        </Tabs>

        <div class="process-output-balance-grid-host">
          <Grid @cell-dblclick="({ row }) => handleCellDblClick(row)">
            <template #sortableHeader="{ column }">
              <span class="process-output-balance-sort-header">
                <span>{{ column.title }}</span>
                <i v-if="getSortIndex(column.field)">{{ getSortIndex(column.field) }}</i>
              </span>
            </template>

            <template #quantityCell="{ row, column }">
              <button
                v-if="canDrillQuantity(row, column.field)"
                class="process-output-balance-qty-drill"
                type="button"
                @click.stop="openAggregateDrillByField(row, column.field)"
              >
                {{ formatLedgerQty(row[column.field], row.uom) }}
              </button>
              <span v-else>{{ formatLedgerQty(row[column.field], row.uom) }}</span>
            </template>

            <template #sourceType="{ row }">
              <Tag :color="STAGE_COLOR[row.sourceType] || 'default'">
                {{ row.opName || row.sourceType || '-' }}
              </Tag>
            </template>

            <template #stockStatus="{ row }">
              <Tag :color="STOCK_STATUS_COLOR[row.stockStatus] || 'default'">
                {{ STOCK_STATUS_LABEL[row.stockStatus] || row.stockStatus || '-' }}
              </Tag>
            </template>

            <template #txnSummary="{ row }">
              <div class="process-output-balance-txn-summary">
                <span>{{ getTxnSummaryPreview(row) }}</span>
                <Button
                  v-if="hasTxnDetails(row)"
                  size="small"
                  type="link"
                  @click.stop="openTxnDetails(row)"
                >
                  明细
                </Button>
              </div>
            </template>

            <template #actions="{ row }">
              <Button
                size="small"
                type="link"
                :disabled="!canReleaseSourceLock(row)"
                @click.stop="openReleaseSourceLock(row)"
              >
                释放
              </Button>
            </template>
          </Grid>
        </div>
      </div>

      <AModal
        v-model:open="advancedQueryVisible"
        title="多条件查询"
        width="760px"
        :z-index="MODAL_Z_INDEX.advancedQuery"
        @ok="applyAdvancedQuery"
      >
        <div class="process-output-balance-advanced-body">
          <div class="process-output-balance-advanced-grid">
            <label class="process-output-balance-query-item">
              <span>计划号</span>
              <Input v-model:value="advancedQuery.sourcePlanNo" allow-clear placeholder="请输入来源计划号" />
            </label>
            <label class="process-output-balance-query-item">
              <span>中间品批号</span>
              <Input v-model:value="advancedQuery.batchNo" allow-clear placeholder="请输入中间品批号" />
            </label>
            <label class="process-output-balance-query-item">
              <span>来源批号</span>
              <Input v-model:value="advancedQuery.sourceBatchNo" allow-clear placeholder="请输入来源批号" />
            </label>
            <label class="process-output-balance-query-item">
              <span>母卷批次号</span>
              <Input v-model:value="advancedQuery.sourceParentBatchNo" allow-clear placeholder="请输入母卷批次号" />
            </label>
            <label class="process-output-balance-query-item">
              <span>工序</span>
              <Select
                v-model:value="advancedQuery.sourceType"
                allow-clear
                :options="STAGE_OPTIONS"
                placeholder="请选择工序"
              />
            </label>
            <label class="process-output-balance-query-item">
              <span>质量状态</span>
              <Select
                v-model:value="advancedQuery.qualityStatus"
                allow-clear
                :options="QUALITY_STATUS_OPTIONS"
                placeholder="请选择质量状态"
              />
            </label>
            <label class="process-output-balance-query-item">
              <span>物料编码</span>
              <Input v-model:value="advancedQuery.materialCode" allow-clear placeholder="请输入物料编码" />
            </label>
            <label class="process-output-balance-query-item">
              <span>型号</span>
              <Input v-model:value="advancedQuery.modelNo" allow-clear placeholder="请输入型号" />
            </label>
            <label class="process-output-balance-query-item">
              <span>过账日期起</span>
              <DatePicker
                v-model:value="advancedQuery.txnDateStart"
                class="w-full"
                value-format="YYYY-MM-DD"
                format="YYYY-MM-DD"
                placeholder="开始日期"
              />
            </label>
            <label class="process-output-balance-query-item">
              <span>过账日期止</span>
              <DatePicker
                v-model:value="advancedQuery.txnDateEnd"
                class="w-full"
                value-format="YYYY-MM-DD"
                format="YYYY-MM-DD"
                placeholder="结束日期"
              />
            </label>
            <label class="process-output-balance-query-item">
              <span>只看可用</span>
              <Switch v-model:checked="advancedQuery.onlyAvailable" checked-children="是" un-checked-children="否" />
            </label>
          </div>
        </div>
      </AModal>

      <AModal
        v-model:open="sortConfigVisible"
        title="排序设置"
        width="720px"
        :footer="null"
        :z-index="MODAL_Z_INDEX.sortConfig"
      >
        <div class="process-output-balance-sort-modal">
          <div class="process-output-balance-sort-add">
            <span>新增字段</span>
            <Select
              v-model:value="newSortField"
              allow-clear
              :options="availableSortOptions"
              placeholder="请选择排序字段"
              @change="addSortField"
            />
          </div>
          <div class="process-output-balance-sort-list">
            <div
              v-for="(sort, index) in activeSortFields"
              :key="sort.field"
              class="process-output-balance-sort-list-item"
            >
              <b>{{ index + 1 }}</b>
              <span>{{ sort.label }}</span>
              <Button size="small" @click="toggleSortOrder(sort.field)">
                {{ formatSortOrder(sort.order) }}
              </Button>
              <Button size="small" :disabled="index === 0" @click="moveSortField(index, -1)">
                <template #icon><IconifyIcon icon="lucide:chevron-up" /></template>
              </Button>
              <Button size="small" :disabled="index === activeSortFields.length - 1" @click="moveSortField(index, 1)">
                <template #icon><IconifyIcon icon="lucide:chevron-down" /></template>
              </Button>
              <Button size="small" danger @click="removeSortField(sort.field)">
                <template #icon><IconifyIcon icon="lucide:x" /></template>
              </Button>
            </div>
            <div v-if="activeSortFields.length === 0" class="process-output-balance-sort-empty">暂无排序字段</div>
          </div>
        </div>
      </AModal>

      <AModal
        v-model:open="sourceDetailVisible"
        title="库存来源与锁定信息"
        width="680px"
        :footer="null"
        destroy-on-close
        :z-index="MODAL_Z_INDEX.sourceDetail"
      >
        <div class="process-output-balance-source-detail">
          <div>
            <span>库存ID</span>
            <b>{{ sourceDetailRow?.id || '-' }}</b>
          </div>
          <div>
            <span>来源表</span>
            <b>{{ sourceDetailRow?.sourceTable || '-' }}</b>
          </div>
          <div>
            <span>来源ID</span>
            <b>{{ sourceDetailRow?.sourceId || '-' }}</b>
          </div>
          <div>
            <span>来源报工ID</span>
            <b>{{ sourceDetailRow?.sourceReportId || '-' }}</b>
          </div>
          <div>
            <span>锁定明细ID</span>
            <b>{{ sourceDetailRow?.activeLockId || '-' }}</b>
          </div>
          <div>
            <span>锁定状态</span>
            <b>{{ sourceDetailRow?.activeLockStatus || '-' }}</b>
          </div>
          <div>
            <span>锁定计划</span>
            <b>{{ sourceDetailRow?.activeLockTargetPlanNo || '-' }}</b>
          </div>
          <div>
            <span>锁定工序</span>
            <b>{{ sourceDetailRow?.activeLockTargetOpName || '-' }}</b>
          </div>
          <div>
            <span>产品型号</span>
            <b>{{ sourceDetailRow?.modelNo || '-' }}</b>
          </div>
          <div>
            <span>锁定剩余</span>
            <b>{{ sourceDetailRow?.lockRemainingQty ?? '-' }}</b>
          </div>
          <div>
            <span>活动锁剩余</span>
            <b>{{ sourceDetailRow?.activeLockRemainingQty ?? '-' }}</b>
          </div>
          <div>
            <span>最近流水</span>
            <b>{{ sourceDetailRow?.lastTxnNo || '-' }}</b>
          </div>
          <div>
            <span>来源批号</span>
            <b>{{ sourceDetailRow?.sourceBatchNo || '-' }}</b>
          </div>
          <div>
            <span>母卷批次号</span>
            <b>{{ sourceDetailRow?.sourceParentBatchNo || '-' }}</b>
          </div>
        </div>
      </AModal>

      <AModal
        v-model:open="aggregateDrillVisible"
        :title="aggregateDrillTitle"
        width="980px"
        :footer="null"
        destroy-on-close
        :z-index="MODAL_Z_INDEX.aggregateDrill"
      >
        <div class="process-output-balance-drill">
          <div class="process-output-balance-drill-head">
            <span>片号</span>
            <span>计划号</span>
            <span>产品型号</span>
            <span>状态</span>
            <span>在库</span>
            <span>可用</span>
            <span>锁定</span>
            <span>消耗</span>
            <span>最近过账</span>
            <span>流水</span>
          </div>
          <div v-if="aggregateDrillLoading" class="process-output-balance-drill-empty">
            正在加载明细
          </div>
          <div v-else-if="aggregateDrillRows.length === 0" class="process-output-balance-drill-empty">
            暂无匹配片号
          </div>
          <div v-else class="process-output-balance-drill-list">
            <div
              v-for="row in aggregateDrillRows"
              :key="row.id"
              class="process-output-balance-drill-row"
            >
              <b>{{ row.batchNo || '-' }}</b>
              <span>{{ row.sourcePlanNo || '-' }}</span>
              <span>{{ row.modelNo || '-' }}</span>
              <span>
                <Tag :color="STOCK_STATUS_COLOR[row.stockStatus] || 'default'">
                  {{ STOCK_STATUS_LABEL[row.stockStatus] || row.stockStatus || '-' }}
                </Tag>
              </span>
              <span>{{ formatLedgerQty(row.onHandQty, row.uom) }}</span>
              <span>{{ formatLedgerQty(row.availableQty, row.uom) }}</span>
              <span>{{ formatLedgerQty(row.planLockedQty, row.uom) }}</span>
              <span>{{ formatLedgerQty(row.consumedQty, row.uom) }}</span>
              <span>{{ formatTxnTime(row.lastTxnTime) }}</span>
              <span>
                <Button size="small" type="link" @click="openTxnDetails(row)">
                  明细
                </Button>
              </span>
            </div>
          </div>
        </div>
      </AModal>

      <AModal
        v-model:open="txnDetailVisible"
        width="860px"
        :footer="null"
        destroy-on-close
        :z-index="MODAL_Z_INDEX.txnDetail"
      >
        <template #title>
          <div class="process-output-balance-txn-title">
            <span>消耗/锁定流水明细</span>
            <Button
              class="process-output-balance-debug-toggle"
              :class="{ 'is-active': txnDebugVisible }"
              size="small"
              type="text"
              :title="txnDebugVisible ? '隐藏调试信息' : '显示调试信息'"
              @click.stop="txnDebugVisible = !txnDebugVisible"
            >
              <template #icon><IconifyIcon icon="lucide:bug" /></template>
            </Button>
          </div>
        </template>
        <div class="process-output-balance-txn-detail">
          <div class="process-output-balance-txn-detail-head">
            <span>
              <b>中间品批号</b>
              <em>{{ txnDetailRow?.batchNo || '-' }}</em>
            </span>
            <span>
              <b>来源计划</b>
              <em>{{ txnDetailRow?.sourcePlanNo || '-' }}</em>
            </span>
            <span>
              <b>来源工序</b>
              <em>{{ txnDetailRow?.opName || txnDetailRow?.sourceType || '-' }}</em>
            </span>
          </div>
          <div v-if="hasTxnDetails(txnDetailRow)" class="process-output-balance-txn-detail-list">
            <div
              v-for="(detail, index) in getTxnDetails(txnDetailRow)"
              :key="`${detail.txnNo || detail.lockId || index}-${index}`"
              class="process-output-balance-txn-detail-item"
            >
              <Tag :color="getTxnDetailColor(detail.eventType)">
                {{ detail.eventTypeName || detail.eventType || '-' }}
              </Tag>
              <div>
                <strong>{{ detail.summary || '-' }}</strong>
                <small>
                  {{ formatTxnTime(detail.txnTime) }}
                  <template v-if="txnDebugVisible">
                    <span>流水：{{ detail.txnNo || '-' }}</span>
                    <span>单据：{{ detail.refDocType || '-' }} / {{ detail.refDocId || '-' }}</span>
                  </template>
                </small>
                <p v-if="getTxnBusinessRemark(detail)">{{ getTxnBusinessRemark(detail) }}</p>
                <p
                  v-if="txnDebugVisible && getTxnDebugRemark(detail)"
                  class="process-output-balance-txn-debug-remark"
                >
                  {{ getTxnDebugRemark(detail) }}
                </p>
              </div>
              <b>{{ formatTxnQty(detail.qty, detail.uom || txnDetailRow?.uom) }}</b>
            </div>
          </div>
          <div v-else class="process-output-balance-txn-detail-empty">
            暂无消耗/锁定流水明细
          </div>
        </div>
      </AModal>

      <AModal
        v-model:open="releaseModalVisible"
        title="释放锁定余量"
        width="560px"
        :confirm-loading="releaseSubmitting"
        ok-text="确认释放"
        :z-index="MODAL_Z_INDEX.release"
        @cancel="closeReleaseSourceLock"
        @ok="confirmReleaseSourceLock"
      >
        <div class="process-output-balance-release-body">
          <div class="process-output-balance-release-grid">
            <span>
              <b>中间品批号</b>
              <em>{{ releaseTargetRow?.batchNo || '-' }}</em>
            </span>
            <span>
              <b>锁定计划</b>
              <em>{{ releaseTargetRow?.activeLockTargetPlanNo || '-' }}</em>
            </span>
            <span>
              <b>锁定工序</b>
              <em>{{ releaseTargetRow?.activeLockTargetOpName || '-' }}</em>
            </span>
            <span>
              <b>最大可释放</b>
              <em>{{ formatTxnQty(getReleaseMaxQty(), releaseTargetRow?.uom) }}</em>
            </span>
          </div>
          <label class="process-output-balance-release-field">
            <span>本次释放量</span>
            <InputNumber
              v-model:value="releaseForm.releaseQty"
              :max="getReleaseMaxQty()"
              :min="0"
              :precision="3"
              class="w-full"
              @change="normalizeReleaseQty"
            />
          </label>
          <label class="process-output-balance-release-field">
            <span>释放原因</span>
            <Input.TextArea
              v-model:value="releaseForm.releaseReason"
              :maxlength="300"
              :rows="3"
              placeholder="请填写释放原因"
            />
          </label>
        </div>
      </AModal>
    </div>
  </Page>
</template>

<style scoped>
.process-output-balance-console {
  grid-template-rows: max-content max-content minmax(0, 1fr);
}

.process-output-balance-banner {
  min-height: 78px;
  max-height: 90px;
}

.process-output-balance-action-group {
  flex-wrap: nowrap;
}

.process-output-balance-action-group .action-tile:disabled {
  cursor: not-allowed;
  opacity: 0.48;
}

.process-output-balance-query-panel {
  display: flex;
  box-sizing: border-box;
  flex-direction: column;
  gap: 8px;
  min-width: 0;
  min-height: 0;
  padding: 8px 10px;
  overflow: hidden;
  background: linear-gradient(180deg, #eef3f8 0%, #e4ebf3 100%);
  border: 1px solid #8794a4;
}

.process-output-balance-simple-query {
  display: grid;
  grid-template-columns: 86px 132px 170px minmax(260px, 1fr) 86px 106px 86px 150px;
  gap: 8px;
  align-items: stretch;
  min-width: 0;
}

.process-output-balance-query-label {
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

.process-output-balance-simple-query :deep(.ant-input-affix-wrapper),
.process-output-balance-simple-query :deep(.ant-select-selector),
.process-output-balance-simple-query :deep(.ant-segmented) {
  min-height: 34px;
  border-radius: 0;
}

.process-output-balance-simple-query :deep(.ant-segmented) {
  padding: 2px;
  background: #f8fafc;
  border: 1px solid #c6d3df;
}

.process-output-balance-simple-query :deep(.ant-segmented-item) {
  min-height: 28px;
  font-size: 12px;
  font-weight: 900;
  line-height: 28px;
  border-radius: 0;
}

.process-output-balance-simple-query :deep(.ant-btn) {
  height: 34px;
  font-weight: 800;
  border-radius: 0;
}

.process-output-balance-switch {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-width: 0;
  min-height: 34px;
  padding: 0 10px;
  color: #334155;
  font-size: 12px;
  font-weight: 900;
  background: #f8fafc;
  border: 1px solid #c6d3df;
}

.process-output-balance-query-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  min-height: 26px;
  padding: 0 0 1px 86px;
}

.process-output-balance-query-tag {
  display: inline-flex;
  align-items: center;
  max-width: 360px;
  min-height: 24px;
  overflow: hidden;
  color: #075985;
  font-size: 12px;
  font-weight: 700;
  background: #f8fafc;
  border: 1px solid #9fb6cd;
}

.process-output-balance-query-tag b {
  flex: 0 0 auto;
  padding: 0 7px;
  color: #334155;
  background: #e2e8f0;
  border-right: 1px solid #c6d3df;
}

.process-output-balance-query-tag span {
  min-width: 0;
  padding: 0 7px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.process-output-balance-query-tag button {
  flex: 0 0 24px;
  height: 24px;
  color: #64748b;
  cursor: pointer;
  background: transparent;
  border: 0;
  border-left: 1px solid #c6d3df;
}

.process-output-balance-query-tag button:hover {
  color: #dc2626;
  background: #fee2e2;
}

.process-output-balance-body {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
}

.process-output-balance-tabs {
  flex: 0 0 auto;
  min-height: 0;
  overflow: hidden;
}

.process-output-balance-tabs :deep(.ant-tabs-nav) {
  padding: 0 8px;
  margin: 0;
  background: #eef3f8;
  border: 1px solid #8794a4;
  border-bottom: 0;
}

.process-output-balance-tabs :deep(.ant-tabs-content-holder) {
  display: none;
}

.status-tab-label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-weight: 900;
}

.status-tab-badge {
  min-width: 22px;
  padding: 0 6px;
  color: #075985;
  font-size: 11px;
  line-height: 18px;
  text-align: center;
  background: #dbeafe;
  border: 1px solid #9fb6cd;
}

.process-output-balance-grid-host {
  position: relative;
  flex: 1;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
  background: #fff;
  border: 1px solid #8794a4;
}

.process-output-balance-grid-host :deep(.vxe-grid) {
  height: 100% !important;
  min-height: 0 !important;
}

.process-output-balance-grid-host :deep(.vxe-grid--table-wrapper) {
  min-height: 0 !important;
  overflow: hidden !important;
}

.process-output-balance-grid-host :deep(.vxe-grid--table-wrapper .vxe-table) {
  height: 100% !important;
  max-height: 100% !important;
}

.process-output-balance-sort-header {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 3px;
  min-width: 0;
  max-width: 100%;
  vertical-align: middle;
}

.process-output-balance-sort-header > span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.process-output-balance-sort-header i {
  display: inline-flex;
  flex: 0 0 14px;
  align-items: center;
  justify-content: center;
  width: 14px;
  height: 14px;
  color: #075985;
  font-size: 10px;
  font-style: normal;
  font-weight: 900;
  line-height: 14px;
  background: #dbeafe;
  border: 1px solid #60a5fa;
  border-radius: 999px;
}

.process-output-balance-qty-drill {
  display: inline-flex;
  align-items: center;
  justify-content: flex-end;
  max-width: 100%;
  min-height: 22px;
  padding: 0 2px;
  overflow: hidden;
  color: #075985;
  font-size: 12px;
  font-weight: 900;
  text-align: right;
  text-decoration: underline;
  text-overflow: ellipsis;
  white-space: nowrap;
  cursor: pointer;
  background: transparent;
  border: 0;
}

.process-output-balance-qty-drill:hover {
  color: #1d4ed8;
}

.process-output-balance-advanced-body,
.process-output-balance-sort-modal {
  display: grid;
  gap: 10px;
}

.process-output-balance-advanced-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.process-output-balance-query-item,
.process-output-balance-sort-add {
  display: grid;
  grid-template-columns: 96px minmax(0, 1fr);
  min-width: 0;
  overflow: hidden;
  border: 1px solid #c6d3df;
}

.process-output-balance-query-item > span,
.process-output-balance-sort-add > span {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  min-height: 34px;
  padding: 0 10px;
  color: #334155;
  font-size: 12px;
  font-weight: 900;
  background: #e2e8f0;
  border-right: 1px solid #c6d3df;
}

.process-output-balance-query-item :deep(.ant-input-affix-wrapper),
.process-output-balance-query-item :deep(.ant-select-selector),
.process-output-balance-query-item :deep(.ant-picker),
.process-output-balance-sort-add :deep(.ant-select-selector) {
  min-height: 34px;
  border: 0;
  border-radius: 0;
}

.process-output-balance-sort-list {
  display: grid;
  gap: 6px;
}

.process-output-balance-sort-list-item {
  display: grid;
  grid-template-columns: 36px minmax(0, 1fr) 78px 34px 34px 34px;
  gap: 6px;
  align-items: center;
  min-width: 0;
  padding: 6px;
  background: #f8fafc;
  border: 1px solid #c6d3df;
}

.process-output-balance-sort-list-item b {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 28px;
  color: #075985;
  background: #dbeafe;
  border: 1px solid #9fb6cd;
}

.process-output-balance-sort-list-item span {
  min-width: 0;
  overflow: hidden;
  color: #0f172a;
  font-size: 12px;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.process-output-balance-sort-list-item :deep(.ant-btn) {
  border-radius: 0;
}

.process-output-balance-sort-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 48px;
  color: #64748b;
  font-size: 12px;
  font-weight: 800;
  background: #f8fafc;
  border: 1px dashed #c6d3df;
}

.process-output-balance-txn-summary {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
  overflow: hidden;
}

.process-output-balance-txn-summary span {
  min-width: 0;
  overflow: hidden;
  color: #0f172a;
  font-size: 12px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.process-output-balance-txn-summary :deep(.ant-btn) {
  flex: 0 0 auto;
  height: 24px;
  padding: 0 4px;
  font-size: 12px;
  font-weight: 900;
}

.process-output-balance-source-detail {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  overflow: hidden;
  border: 1px solid #c6d3df;
}

.process-output-balance-source-detail > div {
  display: grid;
  grid-template-columns: 120px minmax(0, 1fr);
  min-width: 0;
  border-bottom: 1px solid #c6d3df;
}

.process-output-balance-source-detail > div:last-child {
  border-bottom: 0;
}

.process-output-balance-source-detail span {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  min-height: 36px;
  padding: 0 10px;
  color: #334155;
  font-size: 12px;
  font-weight: 900;
  background: #e2e8f0;
  border-right: 1px solid #c6d3df;
}

.process-output-balance-source-detail b {
  display: flex;
  align-items: center;
  min-width: 0;
  min-height: 36px;
  padding: 0 10px;
  overflow: hidden;
  color: #0f172a;
  font-size: 12px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.process-output-balance-drill {
  display: grid;
  gap: 6px;
  min-width: 0;
}

.process-output-balance-drill-head,
.process-output-balance-drill-row {
  display: grid;
  grid-template-columns: minmax(150px, 1.2fr) minmax(120px, 1fr) minmax(110px, 0.8fr) 92px repeat(4, 92px) 150px 58px;
  min-width: 0;
  border: 1px solid #c6d3df;
}

.process-output-balance-drill-head {
  color: #334155;
  font-size: 12px;
  font-weight: 900;
  background: #e2e8f0;
}

.process-output-balance-drill-head span,
.process-output-balance-drill-row span,
.process-output-balance-drill-row b {
  display: flex;
  align-items: center;
  min-width: 0;
  min-height: 34px;
  padding: 0 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  border-right: 1px solid #c6d3df;
}

.process-output-balance-drill-head span:last-child,
.process-output-balance-drill-row span:last-child {
  border-right: 0;
}

.process-output-balance-drill-list {
  display: grid;
  gap: 6px;
  max-height: 440px;
  overflow: auto;
}

.process-output-balance-drill-row {
  color: #0f172a;
  font-size: 12px;
  font-weight: 700;
  background: #fff;
}

.process-output-balance-drill-row b {
  color: #075985;
  font-weight: 900;
}

.process-output-balance-drill-row :deep(.ant-btn) {
  height: 24px;
  padding: 0;
  font-size: 12px;
  font-weight: 900;
}

.process-output-balance-drill-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 80px;
  color: #64748b;
  font-size: 12px;
  font-weight: 800;
  background: #f8fafc;
  border: 1px dashed #c6d3df;
}

.process-output-balance-txn-detail {
  display: grid;
  gap: 10px;
  min-width: 0;
}

.process-output-balance-txn-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding-right: 42px;
  font-weight: 900;
}

.process-output-balance-debug-toggle {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  color: #64748b;
  border-radius: 0;
}

.process-output-balance-debug-toggle.is-active {
  color: #075985;
  background: #dbeafe;
  border: 1px solid #60a5fa;
}

.process-output-balance-txn-detail-head {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  overflow: hidden;
  border: 1px solid #c6d3df;
}

.process-output-balance-txn-detail-head span {
  display: grid;
  grid-template-columns: 86px minmax(0, 1fr);
  min-width: 0;
  color: #0f172a;
  font-size: 12px;
  font-weight: 800;
  border-right: 1px solid #c6d3df;
}

.process-output-balance-txn-detail-head span:last-child {
  border-right: 0;
}

.process-output-balance-txn-detail-head b {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  min-height: 34px;
  padding: 0 8px;
  color: #334155;
  background: #e2e8f0;
  border-right: 1px solid #c6d3df;
}

.process-output-balance-txn-detail-head span {
  align-items: center;
}

.process-output-balance-txn-detail-head em {
  min-width: 0;
  padding: 0 8px;
  overflow: hidden;
  font-style: normal;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.process-output-balance-txn-detail-list {
  display: grid;
  gap: 6px;
  max-height: 420px;
  overflow: auto;
}

.process-output-balance-txn-detail-item {
  display: grid;
  grid-template-columns: 64px minmax(0, 1fr) 110px;
  gap: 8px;
  align-items: start;
  min-width: 0;
  padding: 8px;
  background: #f8fafc;
  border: 1px solid #c6d3df;
}

.process-output-balance-txn-detail-item > div {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.process-output-balance-txn-detail-item strong {
  min-width: 0;
  overflow: hidden;
  color: #0f172a;
  font-size: 13px;
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.process-output-balance-txn-detail-item small {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  color: #64748b;
  font-size: 12px;
  font-weight: 700;
}

.process-output-balance-txn-detail-item p {
  margin: 0;
  color: #475569;
  font-size: 12px;
  line-height: 1.5;
}

.process-output-balance-txn-debug-remark {
  color: #64748b !important;
}

.process-output-balance-txn-detail-item > b {
  justify-self: end;
  color: #075985;
  font-size: 12px;
  font-weight: 900;
  white-space: nowrap;
}

.process-output-balance-txn-detail-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 80px;
  color: #64748b;
  font-size: 12px;
  font-weight: 800;
  background: #f8fafc;
  border: 1px dashed #c6d3df;
}

.process-output-balance-release-body {
  display: grid;
  gap: 12px;
  min-width: 0;
}

.process-output-balance-release-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  overflow: hidden;
  border: 1px solid #c6d3df;
}

.process-output-balance-release-grid span {
  display: grid;
  grid-template-columns: 86px minmax(0, 1fr);
  min-width: 0;
  color: #0f172a;
  font-size: 12px;
  font-weight: 800;
  border-right: 1px solid #c6d3df;
  border-bottom: 1px solid #c6d3df;
}

.process-output-balance-release-grid span:nth-child(2n) {
  border-right: 0;
}

.process-output-balance-release-grid span:nth-last-child(-n + 2) {
  border-bottom: 0;
}

.process-output-balance-release-grid b {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  min-height: 34px;
  padding: 0 8px;
  color: #334155;
  background: #e2e8f0;
  border-right: 1px solid #c6d3df;
}

.process-output-balance-release-grid em {
  min-width: 0;
  padding: 0 8px;
  overflow: hidden;
  font-style: normal;
  line-height: 34px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.process-output-balance-release-field {
  display: grid;
  gap: 6px;
  color: #334155;
  font-size: 12px;
  font-weight: 800;
}

.process-output-balance-release-field > span {
  color: #0f172a;
}

@media (max-width: 900px) {
  .process-output-balance-simple-query {
    grid-template-columns: 78px minmax(0, 1fr);
  }

  .process-output-balance-query-tags {
    padding-left: 0;
  }

  .process-output-balance-advanced-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .process-output-balance-sort-list-item {
    grid-template-columns: 32px minmax(0, 1fr) 72px 32px 32px 32px;
  }

  .process-output-balance-txn-detail-head,
  .process-output-balance-release-grid,
  .process-output-balance-drill-head,
  .process-output-balance-drill-row,
  .process-output-balance-txn-detail-item {
    grid-template-columns: minmax(0, 1fr);
  }

  .process-output-balance-release-grid span {
    grid-template-columns: 78px minmax(0, 1fr);
    border-right: 0;
  }

  .process-output-balance-release-grid span:nth-last-child(-n + 2) {
    border-bottom: 1px solid #c6d3df;
  }

  .process-output-balance-release-grid span:last-child {
    border-bottom: 0;
  }
}
</style>
