<script lang="ts" setup>
import type { TableColumnsType } from 'ant-design-vue';

import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { SrmPerformanceActualReportApi } from '#/api/mes/srm/performance/actual-report';

import { computed, reactive, ref } from 'vue';

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
  Segmented,
  Select,
  Space,
  Spin,
  Table,
  Tag,
  Tooltip,
} from 'ant-design-vue';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  batchCreateActualReport,
  confirmActualReport,
  deleteActualReport,
  getActualReport,
  getActualReportAvailableSuppliers,
  getActualReportPage,
  pullActualReportMonthlyValues,
  rejectActualReport,
  submitActualReport,
  updateActualReport,
} from '#/api/mes/srm/performance/actual-report';

import SrmAttachmentPanel from '../../shared/SrmAttachmentPanel.vue';

import '../../shared/srm-erp-detail.css';

defineOptions({ name: 'SrmPerformanceActualReport' });

type DetailMode = 'detail' | 'edit';

type ReportForm = SrmPerformanceActualReportApi.Report & {
  values: SrmPerformanceActualReportApi.Value[];
};

const SRM_NESTED_MODAL_Z_INDEX = 5600;
const SRM_NESTED_DROPDOWN_Z_INDEX = SRM_NESTED_MODAL_Z_INDEX + 10;
const ATTACHMENT_BIZ_TYPE = 'SRM_PERFORMANCE_ACTUAL_VALUE';

const detailMode = ref<DetailMode>('detail');
const saving = ref(false);
const basicExpanded = ref(true);
const attachmentOpen = ref(false);
const selectedValue = ref<SrmPerformanceActualReportApi.Value>();
const selectedReport = ref<null | ReportForm>(null);
const selectedReportLoading = ref(false);
const detailPanelOpen = ref(false);
const checkedIds = ref<number[]>([]);
const batchOperating = ref(false);
const cardLoading = ref(false);
const cardRows = ref<SrmPerformanceActualReportApi.Report[]>([]);
const cardPageNo = ref(1);
const cardPageSize = ref(20);
const cardTotal = ref(0);
const generateModalOpen = ref(false);
const generateLoading = ref(false);
const availableSupplierLoading = ref(false);
const availableSuppliers = ref<
  SrmPerformanceActualReportApi.AvailableSupplier[]
>([]);
const selectedGenerateSupplierIds = ref<number[]>([]);

const currentPeriod = createCurrentPeriod();
const queryForm = reactive<{
  evalMonth?: number;
  evalQuarter?: number;
  evalYear?: number;
  periodType?: SrmPerformanceActualReportApi.PeriodType;
  status?: string;
  supplierInfo?: string;
}>({
  evalQuarter: currentPeriod.evalQuarter,
  evalYear: currentPeriod.evalYear,
  periodType: 'QUARTER',
});
const generateForm = reactive<{
  evalMonth?: number;
  evalQuarter?: number;
  evalYear?: number;
  periodType: SrmPerformanceActualReportApi.PeriodType;
}>({
  evalMonth: currentPeriod.evalMonth,
  evalQuarter: currentPeriod.evalQuarter,
  evalYear: currentPeriod.evalYear,
  periodType: 'QUARTER',
});
const generateFilter = reactive<{ supplierInfo?: string }>({});
const form = reactive<ReportForm>(createEmptyForm());
const QUERY_BATCH_PAGE_SIZE = 200;

type BatchRowsResult = {
  cancelled: boolean;
  rows: SrmPerformanceActualReportApi.Report[];
};

const isReadonly = computed(() => detailMode.value === 'detail');
const canEdit = computed(
  () =>
    detailMode.value === 'edit' &&
    (!form.id ||
      form.canEdit === true ||
      ['DRAFT', 'REJECTED'].includes(form.status || '')),
);
const subtitleItems = computed(() =>
  [
    form.reportNo ? `单号 ${form.reportNo}` : '',
    form.supplierName || '',
    periodText(form),
    statusText(form.status),
  ].filter(Boolean),
);
const basicSummaryItems = computed(() => [
  { label: '供应商', value: form.supplierName || '-' },
  { label: '期间类型', value: periodTypeText(form.periodType) },
  { label: '年度', value: form.evalYear || '-' },
  { label: '季度', value: form.evalQuarter ? `Q${form.evalQuarter}` : '-' },
  { label: '月份', value: form.evalMonth ? `${form.evalMonth}月` : '-' },
  { label: '状态', value: statusText(form.status) },
]);
const selectedReportTitle = computed(() => {
  const record = selectedReport.value;
  if (!record) {
    return '选中供应商实际值明细';
  }
  return [record.supplierName, periodText(record)].filter(Boolean).join(' · ');
});
const panelToggleText = computed(() =>
  detailPanelOpen.value ? '收起右侧' : '展开右侧',
);
const cardTotalPage = computed(() =>
  Math.max(1, Math.ceil(cardTotal.value / cardPageSize.value)),
);
const generateSupplierRowSelection = computed(() => ({
  onChange: (keys: Array<number | string>) => {
    selectedGenerateSupplierIds.value = keys.map(Number);
  },
  selectedRowKeys: selectedGenerateSupplierIds.value,
}));

const gridColumns: VxeTableGridOptions['columns'] = [
  {
    align: 'center',
    fixed: 'left',
    title: '',
    type: 'checkbox',
    width: 42,
  },
  {
    field: 'reportNo',
    minWidth: 190,
    slots: { default: 'reportNo' },
    title: '上报单号',
  },
  { field: 'supplierCode', minWidth: 140, title: '供应商编号' },
  {
    field: 'supplierName',
    minWidth: 220,
    slots: { default: 'supplierName' },
    title: '供应商名称',
  },
  {
    align: 'center',
    field: 'periodType',
    minWidth: 110,
    slots: { default: 'periodType' },
    title: '期间类型',
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
    field: 'evalMonth',
    width: 90,
    slots: { default: 'month' },
    title: '月份',
  },
  {
    align: 'center',
    field: 'status',
    minWidth: 110,
    slots: { default: 'status' },
    title: '状态',
  },
  { field: 'reporterUserName', minWidth: 130, title: '上报人' },
  { field: 'confirmUserName', minWidth: 130, title: '确认人' },
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

const previewValueColumns: TableColumnsType = [
  { align: 'center', dataIndex: 'seq', title: '序号', width: 70 },
  { dataIndex: 'metricName', title: '来源指标名称', width: 320 },
  { align: 'center', dataIndex: 'numericValue', title: '实际值', width: 110 },
  { align: 'center', dataIndex: 'unit', title: '单位', width: 80 },
  { dataIndex: 'remark', title: '实际说明', width: 260 },
];

const valueColumns: TableColumnsType = [
  {
    align: 'center',
    dataIndex: 'seq',
    fixed: 'left',
    title: '序号',
    width: 70,
  },
  {
    dataIndex: 'metricName',
    fixed: 'left',
    title: '来源指标名称',
    width: 360,
  },
  { align: 'center', dataIndex: 'numericValue', title: '实际值', width: 150 },
  { align: 'center', dataIndex: 'unit', title: '单位', width: 90 },
  { dataIndex: 'remark', title: '实际说明', width: 280 },
  {
    align: 'center',
    dataIndex: 'attachment',
    fixed: 'right',
    title: '附件',
    width: 90,
  },
];

const generateSupplierColumns: TableColumnsType = [
  { dataIndex: 'supplierCode', title: '供应商编号', width: 180 },
  { dataIndex: 'supplierName', title: '供应商名称', width: 300 },
  { dataIndex: 'templateNameSnapshot', title: '季度考核模板', width: 260 },
  {
    align: 'center',
    dataIndex: 'templateVersionNoSnapshot',
    title: '模板版本',
    width: 110,
  },
];

const valueTableScrollX = computed(() =>
  valueColumns.reduce((sum, column) => sum + Number(column.width || 120), 0),
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
          const result = await getActualReportPage({
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
  } as VxeTableGridOptions<SrmPerformanceActualReportApi.Report>,
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

function createCurrentPeriod() {
  const now = new Date();
  const month = now.getMonth() + 1;
  return {
    evalMonth: month,
    evalQuarter: Math.ceil(month / 3),
    evalYear: now.getFullYear(),
  };
}

function createEmptyForm(): ReportForm {
  const period = createCurrentPeriod();
  return {
    evalMonth: period.evalMonth,
    evalQuarter: period.evalQuarter,
    evalYear: period.evalYear,
    periodType: 'MONTH',
    status: 'DRAFT',
    values: [],
  };
}

function resetForm(record: Partial<ReportForm> = {}) {
  Object.keys(form).forEach((key) => delete (form as Record<string, any>)[key]);
  Object.assign(form, toReportForm(record));
}

function toReportForm(record: Partial<ReportForm> = {}): ReportForm {
  return {
    ...createEmptyForm(),
    ...record,
    values: (record.values || []).map((item) => ({ ...item })),
  };
}

function buildQueryParams() {
  const params: Record<string, unknown> = {};
  const supplierInfo = queryForm.supplierInfo?.trim();
  const evalYear = normalizeNumber(queryForm.evalYear);
  if (supplierInfo) {
    params.supplierInfo = supplierInfo;
  }
  if (queryForm.periodType) {
    params.periodType = queryForm.periodType;
  }
  if (evalYear) {
    params.evalYear = evalYear;
  }
  if (queryForm.periodType === 'MONTH') {
    const evalMonth = normalizeNumber(queryForm.evalMonth);
    if (evalMonth) {
      params.evalMonth = evalMonth;
      params.evalQuarter = Math.ceil(evalMonth / 3);
    }
  } else {
    const evalQuarter = normalizeNumber(queryForm.evalQuarter);
    if (evalQuarter) {
      params.evalQuarter = evalQuarter;
    }
  }
  if (queryForm.status) {
    params.status = queryForm.status;
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
    const result = await getActualReportPage(
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
  clearSelectedReport();
  cardPageNo.value = 1;
  await refreshList();
}

async function resetQueryForm() {
  const period = createCurrentPeriod();
  Object.assign(queryForm, {
    evalMonth: undefined,
    evalQuarter: period.evalQuarter,
    evalYear: period.evalYear,
    periodType: 'QUARTER',
    status: undefined,
    supplierInfo: undefined,
  });
  checkedIds.value = [];
  clearSelectedReport();
  cardPageNo.value = 1;
  await refreshList();
}

async function handleQueryPeriodTypeChange() {
  const period = createCurrentPeriod();
  if (queryForm.periodType === 'MONTH') {
    queryForm.evalMonth = queryForm.evalMonth || period.evalMonth;
    queryForm.evalQuarter = Math.ceil(
      (queryForm.evalMonth || period.evalMonth) / 3,
    );
  } else {
    queryForm.evalMonth = undefined;
    queryForm.evalQuarter = queryForm.evalQuarter || period.evalQuarter;
  }
  await handleQuery();
}

async function toggleDetailPanel() {
  detailPanelOpen.value = !detailPanelOpen.value;
  checkedIds.value = [];
  await (gridApi.grid as any)?.clearCheckboxRow?.();
  if (!detailPanelOpen.value) {
    clearSelectedReport();
  }
  cardPageNo.value = 1;
  await refreshList();
}

function isCardChecked(row: SrmPerformanceActualReportApi.Report) {
  const id = normalizeId(row.id);
  return !!id && checkedIds.value.includes(id);
}

function selectedRowsForAction() {
  if (!detailPanelOpen.value) {
    return ((gridApi.grid as any)?.getCheckboxRecords?.() ||
      []) as SrmPerformanceActualReportApi.Report[];
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
      content: `当前未勾选实际上报单，将按当前查询条件对全部 ${total} 张绩效实际上报单执行“批量${label}”。不满足状态或权限的记录会自动跳过，请确认筛选条件无误后继续。`,
      okText: '继续执行',
      onCancel: () => resolve(false),
      onOk: () => resolve(true),
      title: '按查询条件批量操作',
    });
  });
}

async function queryRowsForBatch(label: string): Promise<BatchRowsResult> {
  const params = buildQueryParams();
  const firstPage = await getActualReportPage({
    ...params,
    pageNo: 1,
    pageSize: QUERY_BATCH_PAGE_SIZE,
  });
  const total = Number(firstPage.total || 0);
  if (total === 0) {
    message.warning('当前查询条件下没有可操作的绩效实际上报单');
    return { cancelled: true, rows: [] };
  }
  const confirmed = await confirmQueryScopeBatch(label, total);
  if (!confirmed) {
    return { cancelled: true, rows: [] };
  }
  const rows: SrmPerformanceActualReportApi.Report[] = [
    ...(firstPage.list || []),
  ];
  const totalPages = Math.ceil(total / QUERY_BATCH_PAGE_SIZE);
  for (let pageNo = 2; pageNo <= totalPages; pageNo += 1) {
    const page = await getActualReportPage({
      ...params,
      pageNo,
      pageSize: QUERY_BATCH_PAGE_SIZE,
    });
    rows.push(...(page.list || []));
  }
  return { cancelled: false, rows };
}

function toggleCardSelection(
  row: SrmPerformanceActualReportApi.Report,
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
  target: {
    evalMonth?: number;
    evalQuarter?: number;
    evalYear?: number;
    periodType?: SrmPerformanceActualReportApi.PeriodType;
  },
  step: -1 | 1,
) {
  const period = createCurrentPeriod();
  let evalYear = normalizeNumber(target.evalYear) || period.evalYear;
  if (target.periodType === 'MONTH') {
    let evalMonth = normalizeNumber(target.evalMonth) || period.evalMonth;
    evalMonth += step;
    if (evalMonth < 1) {
      evalMonth = 12;
      evalYear -= 1;
    }
    if (evalMonth > 12) {
      evalMonth = 1;
      evalYear += 1;
    }
    target.evalYear = evalYear;
    target.evalMonth = evalMonth;
    target.evalQuarter = Math.ceil(evalMonth / 3);
    return;
  }
  let evalQuarter = normalizeNumber(target.evalQuarter) || period.evalQuarter;
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

async function openPreviewPanel(row: SrmPerformanceActualReportApi.Report) {
  detailPanelOpen.value = true;
  checkedIds.value = [];
  await (gridApi.grid as any)?.clearCheckboxRow?.();
  await queryCardList(cardPageNo.value);
  await previewReport(row);
}

async function previewReport(row: SrmPerformanceActualReportApi.Report) {
  const id = normalizeId(row.id);
  if (!id) {
    return;
  }
  selectedReportLoading.value = true;
  try {
    const result = await getActualReport(id);
    selectedReport.value = toReportForm({
      ...result,
      values: result.values || [],
    });
  } finally {
    selectedReportLoading.value = false;
  }
}

function clearSelectedReport() {
  selectedReport.value = null;
}

async function openCreate() {
  const period = createCurrentPeriod();
  Object.assign(generateForm, {
    evalMonth: period.evalMonth,
    evalQuarter: period.evalQuarter,
    evalYear: period.evalYear,
    periodType: 'QUARTER',
  });
  Object.assign(generateFilter, { supplierInfo: undefined });
  selectedGenerateSupplierIds.value = [];
  availableSuppliers.value = [];
  generateModalOpen.value = true;
  await loadAvailableSuppliers();
}

function buildGenerateParams() {
  const evalYear = normalizeNumber(generateForm.evalYear);
  if (!evalYear) {
    return undefined;
  }
  if (generateForm.periodType === 'MONTH') {
    const evalMonth = normalizeNumber(generateForm.evalMonth);
    if (!evalMonth) {
      return undefined;
    }
    return {
      evalMonth,
      evalQuarter: Math.ceil(evalMonth / 3),
      evalYear,
      periodType: generateForm.periodType,
    };
  }
  const evalQuarter = normalizeNumber(generateForm.evalQuarter);
  if (!evalQuarter) {
    return undefined;
  }
  return {
    evalMonth: undefined,
    evalQuarter,
    evalYear,
    periodType: generateForm.periodType,
  };
}

async function handleGeneratePeriodTypeChange() {
  const period = createCurrentPeriod();
  if (generateForm.periodType === 'MONTH') {
    generateForm.evalMonth = generateForm.evalMonth || period.evalMonth;
    generateForm.evalQuarter = Math.ceil(
      (generateForm.evalMonth || period.evalMonth) / 3,
    );
  } else {
    generateForm.evalMonth = undefined;
    generateForm.evalQuarter = generateForm.evalQuarter || period.evalQuarter;
  }
  await loadAvailableSuppliers();
}

async function loadAvailableSuppliers() {
  const params = buildGenerateParams();
  if (!params) {
    availableSuppliers.value = [];
    selectedGenerateSupplierIds.value = [];
    return;
  }
  availableSupplierLoading.value = true;
  try {
    const result = await getActualReportAvailableSuppliers({
      ...params,
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
  const params = buildGenerateParams();
  if (!params) {
    message.warning('请先选择期间类型和期间');
    return;
  }
  if (selectedGenerateSupplierIds.value.length === 0) {
    message.warning('请选择需要生成实际上报单的供应商');
    return;
  }
  generateLoading.value = true;
  try {
    const ids = await batchCreateActualReport({
      ...params,
      supplierIds: selectedGenerateSupplierIds.value,
    });
    message.success(`已生成 ${ids.length} 张绩效实际上报单`);
    generateModalOpen.value = false;
    selectedGenerateSupplierIds.value = [];
    await refreshList();
    if (ids[0]) {
      detailPanelOpen.value = true;
      cardPageNo.value = 1;
      await queryCardList(1);
      await previewReport({ id: ids[0] });
    }
  } finally {
    generateLoading.value = false;
  }
}

function handleGridCheckboxChange({
  records,
}: {
  records: SrmPerformanceActualReportApi.Report[];
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
  row: SrmPerformanceActualReportApi.Report;
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

async function openDetail(row: SrmPerformanceActualReportApi.Report) {
  const id = normalizeId(row.id);
  if (!id) {
    return;
  }
  const result = await getActualReport(id);
  resetForm({ ...result, values: result.values || [] });
  detailMode.value = 'detail';
  basicExpanded.value = true;
  detailModalApi.open();
}

async function openEdit(row: SrmPerformanceActualReportApi.Report) {
  await openDetail(row);
  if (form.canEdit === false && form.status !== 'REJECTED') {
    message.warning('当前状态不允许编辑');
    return;
  }
  detailMode.value = 'edit';
}

function switchToEdit() {
  if (form.canEdit === false && form.status !== 'REJECTED') {
    message.warning('当前状态不允许编辑');
    return;
  }
  detailMode.value = 'edit';
}

async function closeDetail() {
  selectedValue.value = undefined;
  attachmentOpen.value = false;
  await detailModalApi.close();
}

async function saveReport(closeAfter = false) {
  if (!form.id) {
    message.warning('请先生成实际上报单');
    return;
  }
  saving.value = true;
  try {
    const payload: SrmPerformanceActualReportApi.Report = {
      ...form,
      values: form.values.map((value) => ({
        ...value,
        evidenceRequired: Boolean(value.evidenceRequired),
        numericValue: normalizeNumber(value.numericValue),
        valueType: value.valueType || 'NUMBER',
      })),
    };
    await updateActualReport(payload);
    const result = await getActualReport(form.id);
    resetForm({ ...result, values: result.values || [] });
    selectedReport.value = toReportForm({
      ...result,
      values: result.values || [],
    });
    detailMode.value = 'edit';
    message.success('绩效实际值已保存');
    await refreshList();
    if (closeAfter) {
      await closeDetail();
    }
  } finally {
    saving.value = false;
  }
}

async function handleSubmit(row?: SrmPerformanceActualReportApi.Report) {
  const id = normalizeId(row?.id || form.id);
  if (!id) {
    message.warning('请先选择实际上报单');
    return;
  }
  await submitActualReport(id);
  message.success('已提交确认');
  await refreshCurrent(id);
}

async function handleConfirm(row?: SrmPerformanceActualReportApi.Report) {
  const id = normalizeId(row?.id || form.id);
  if (!id) {
    return;
  }
  await confirmActualReport(id);
  message.success('实际值已确认');
  await refreshCurrent(id);
}

async function handleReject(row?: SrmPerformanceActualReportApi.Report) {
  const id = normalizeId(row?.id || form.id);
  if (!id) {
    return;
  }
  await rejectActualReport(id, '退回修改');
  message.success('已退回上报人修改');
  await refreshCurrent(id);
}

async function handlePullMonthly(row?: SrmPerformanceActualReportApi.Report) {
  const id = normalizeId(row?.id || form.id);
  const periodType = row?.periodType || form.periodType;
  if (!id) {
    return;
  }
  if (periodType !== 'QUARTER') {
    message.warning('只有季度汇总单可以批量获取月度值');
    return;
  }
  await pullActualReportMonthlyValues(id);
  message.success('已获取已确认月度实际值');
  await refreshCurrent(id);
}

async function refreshCurrent(id: number) {
  if (form.id === id) {
    const result = await getActualReport(id);
    resetForm({ ...result, values: result.values || [] });
  }
  if (selectedReport.value?.id === id) {
    const result = await getActualReport(id);
    selectedReport.value = toReportForm({
      ...result,
      values: result.values || [],
    });
  }
  await refreshList();
}

async function batchOperate(
  action: 'confirm' | 'delete' | 'pullMonthly' | 'reject' | 'submit',
) {
  const actionMap = {
    confirm: { label: '确认' },
    delete: { label: '删除' },
    pullMonthly: { label: '获取月度值' },
    reject: { label: '退回' },
    submit: { label: '提交' },
  };
  const currentAction = actionMap[action];
  const selectedRows = selectedRowsForAction();
  if (action === 'delete' && selectedRows.length === 0) {
    message.warning('请先选择需要删除的上报单');
    return;
  }
  batchOperating.value = true;
  try {
    const { cancelled, rows } =
      selectedRows.length > 0
        ? { cancelled: false, rows: selectedRows }
        : await queryRowsForBatch(currentAction.label);
    if (cancelled || rows.length === 0) {
      return;
    }
    let successCount = 0;
    let skippedCount = 0;
    const failedNames: string[] = [];
    for (const row of rows) {
      const id = normalizeId(row.id);
      if (!id) {
        skippedCount += 1;
        continue;
      }
      try {
        if (action === 'submit' && row.canSubmit) {
          await submitActualReport(id);
          successCount += 1;
          continue;
        }
        if (action === 'confirm' && row.canConfirm) {
          await confirmActualReport(id);
          successCount += 1;
          continue;
        }
        if (action === 'reject' && row.canConfirm) {
          await rejectActualReport(id, '退回修改');
          successCount += 1;
          continue;
        }
        if (
          action === 'pullMonthly' &&
          row.periodType === 'QUARTER' &&
          row.canEdit
        ) {
          await pullActualReportMonthlyValues(id);
          successCount += 1;
          continue;
        }
        if (action === 'delete' && row.canEdit) {
          await deleteActualReport(id);
          successCount += 1;
          continue;
        }
        skippedCount += 1;
      } catch {
        failedNames.push(row.supplierName || row.reportNo || String(id));
      }
    }
    if (failedNames.length > 0) {
      const failedPreview = failedNames.slice(0, 8).join('、');
      const failedText =
        failedNames.length > 8
          ? `${failedPreview}等 ${failedNames.length} 条`
          : failedPreview;
      message.warning(
        `${currentAction.label}完成，${successCount} 条成功，${skippedCount} 条跳过，${failedNames.length} 条失败：${failedText}`,
      );
    } else if (successCount > 0) {
      message.success(
        `已${currentAction.label} ${successCount} 张上报单${skippedCount > 0 ? `，跳过 ${skippedCount} 张` : ''}`,
      );
    } else {
      message.warning(`没有符合状态的上报单可${currentAction.label}`);
    }
    checkedIds.value = [];
    await (gridApi.grid as any)?.clearCheckboxRow?.();
    await refreshList();
  } finally {
    batchOperating.value = false;
  }
}

async function handleBatchMenuClick(event: { key: string }) {
  if (event.key === 'generate') {
    await openCreate();
    return;
  }
  if (event.key === 'submit') {
    await batchOperate('submit');
    return;
  }
  if (event.key === 'confirm') {
    await batchOperate('confirm');
    return;
  }
  if (event.key === 'reject') {
    await batchOperate('reject');
    return;
  }
  if (event.key === 'pullMonthly') {
    await batchOperate('pullMonthly');
  }
}

function handleRowMenuClick(
  event: { key: string },
  row: SrmPerformanceActualReportApi.Report,
) {
  if (event.key === 'detail') {
    openDetail(row);
    return;
  }
  if (event.key === 'edit') {
    openEdit(row);
    return;
  }
  if (event.key === 'preview') {
    openPreviewPanel(row);
    return;
  }
  if (event.key === 'submit') {
    handleSubmit(row);
    return;
  }
  if (event.key === 'confirm') {
    handleConfirm(row);
    return;
  }
  if (event.key === 'reject') {
    handleReject(row);
    return;
  }
  if (event.key === 'pullMonthly') {
    handlePullMonthly(row);
    return;
  }
  if (event.key === 'delete') {
    handleDelete(row);
    return;
  }
  if (event.key === 'collapse') {
    toggleDetailPanel();
  }
}

function handleDelete(row: SrmPerformanceActualReportApi.Report) {
  AntModal.confirm({
    content: `确认删除实际上报单 ${row.reportNo || row.supplierName} 吗？`,
    okButtonProps: { danger: true },
    okText: '删除',
    async onOk() {
      await deleteActualReport(row.id!);
      message.success('实际上报单已删除');
      if (selectedReport.value?.id === row.id) {
        clearSelectedReport();
      }
      await refreshList();
    },
    title: '删除绩效实际上报',
  });
}

function openAttachment(record: SrmPerformanceActualReportApi.Value) {
  if (!record.id) {
    message.warning('请先保存明细后再上传附件');
    return;
  }
  selectedValue.value = record;
  attachmentOpen.value = true;
}

function quarterOptions() {
  return [
    { label: 'Q1', value: 1 },
    { label: 'Q2', value: 2 },
    { label: 'Q3', value: 3 },
    { label: 'Q4', value: 4 },
  ];
}

function monthOptions() {
  return Array.from({ length: 12 }, (_, index) => ({
    label: `${index + 1}月`,
    value: index + 1,
  }));
}

function periodTypeOptions() {
  return [
    { label: '月度实际', value: 'MONTH' },
    { label: '季度汇总', value: 'QUARTER' },
  ];
}

function statusOptions() {
  return [
    { label: '草稿', value: 'DRAFT' },
    { label: '已提交', value: 'SUBMITTED' },
    { label: '已确认', value: 'CONFIRMED' },
    { label: '已退回', value: 'REJECTED' },
  ];
}

function statusText(status?: string) {
  return statusOptions().find((item) => item.value === status)?.label || '草稿';
}

function periodTypeText(periodType?: string) {
  return periodType === 'QUARTER' ? '季度汇总' : '月度实际';
}

function periodText(record: Partial<SrmPerformanceActualReportApi.Report>) {
  if (!record.evalYear) {
    return '';
  }
  if (record.periodType === 'MONTH') {
    return `${record.evalYear}年${record.evalMonth || '-'}月`;
  }
  return `${record.evalYear} Q${record.evalQuarter || '-'}`;
}

function statusColor(status?: string) {
  if (status === 'CONFIRMED') {
    return 'success';
  }
  if (status === 'SUBMITTED') {
    return 'processing';
  }
  if (status === 'REJECTED') {
    return 'error';
  }
  return 'default';
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
    <div class="srm-crud-page">
      <div class="srm-crud-page__header">
        <div class="srm-crud-page__title">绩效实际上报</div>
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
              <Form.Item label="期间类型">
                <Segmented
                  v-model:value="queryForm.periodType"
                  class="srm-period-type-switch"
                  :options="periodTypeOptions()"
                  @change="handleQueryPeriodTypeChange"
                />
              </Form.Item>
              <Form.Item label="年度/期间">
                <div
                  class="srm-period-control"
                  :class="{ 'is-month': queryForm.periodType === 'MONTH' }"
                >
                  <Button title="上一期" @click="shiftQueryPeriod(-1)">
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
                    v-if="queryForm.periodType === 'MONTH'"
                    v-model:value="queryForm.evalMonth"
                    class="srm-query-month"
                    :dropdown-style="{ zIndex: SRM_NESTED_DROPDOWN_Z_INDEX }"
                    :get-popup-container="resolveNestedPopupContainer"
                    :options="monthOptions()"
                    placeholder="月份"
                    @change="handleQuery"
                  />
                  <Select
                    v-else
                    v-model:value="queryForm.evalQuarter"
                    class="srm-query-quarter"
                    :dropdown-style="{ zIndex: SRM_NESTED_DROPDOWN_Z_INDEX }"
                    :get-popup-container="resolveNestedPopupContainer"
                    :options="quarterOptions()"
                    placeholder="季度"
                    @change="handleQuery"
                  />
                  <Button title="下一期" @click="shiftQueryPeriod(1)">
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
                    <Menu.Item key="submit" :disabled="batchOperating">
                      批量提交
                    </Menu.Item>
                    <Menu.Item key="confirm" :disabled="batchOperating">
                      批量确认
                    </Menu.Item>
                    <Menu.Item key="reject" :disabled="batchOperating">
                      批量退回
                    </Menu.Item>
                    <Menu.Item key="pullMonthly" :disabled="batchOperating">
                      批量获取月度值
                    </Menu.Item>
                    <Menu.Divider />
                    <Menu.Item key="generate">批量新建</Menu.Item>
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
                <div class="srm-quarter-list-title">绩效实际上报单</div>
                <div class="srm-quarter-list-subtitle">
                  {{ detailPanelOpen ? '卡片视图' : '表格视图' }} · 已选
                  {{ checkedIds.length }} 项
                </div>
              </div>
            </div>
            <div v-show="!detailPanelOpen" class="srm-quarter-table-mode">
              <Grid>
                <template #reportNo="{ row }">
                  <a class="srm-crud-link" @click.stop="openDetail(row)">
                    {{ row.reportNo || '-' }}
                  </a>
                </template>
                <template #supplierName="{ row }">
                  <a class="srm-crud-link" @click.stop="openPreviewPanel(row)">
                    {{ row.supplierName || '-' }}
                  </a>
                </template>
                <template #periodType="{ row }">
                  {{ periodTypeText(row.periodType) }}
                </template>
                <template #quarter="{ row }">
                  Q{{ row.evalQuarter || '-' }}
                </template>
                <template #month="{ row }">
                  {{ row.evalMonth ? `${row.evalMonth}月` : '-' }}
                </template>
                <template #status="{ row }">
                  <Tag :color="statusColor(row.status)" class="!m-0">
                    {{ statusText(row.status) }}
                  </Tag>
                </template>
                <template #rowActions="{ row }">
                  <Dropdown :trigger="['click']">
                    <Button shape="circle" size="small" @click.stop>
                      <IconifyIcon icon="lucide:more-horizontal" />
                    </Button>
                    <template #overlay>
                      <Menu @click="handleRowMenuClick($event, row)">
                        <Menu.Item key="preview">展开明细</Menu.Item>
                        <Menu.Item key="detail">详情</Menu.Item>
                        <Menu.Item
                          key="edit"
                          :disabled="!row.canEdit && row.status !== 'REJECTED'"
                        >
                          编辑实际值
                        </Menu.Item>
                        <Menu.Item
                          key="pullMonthly"
                          :disabled="
                            row.periodType !== 'QUARTER' || !row.canEdit
                          "
                        >
                          获取月度值
                        </Menu.Item>
                        <Menu.Item key="submit" :disabled="!row.canSubmit">
                          提交
                        </Menu.Item>
                        <Menu.Item key="confirm" :disabled="!row.canConfirm">
                          确认
                        </Menu.Item>
                        <Menu.Item key="reject" :disabled="!row.canConfirm">
                          退回
                        </Menu.Item>
                        <Menu.Divider />
                        <Menu.Item key="delete" danger :disabled="!row.canEdit">
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
                      'is-current': selectedReport?.id === row.id,
                      'is-selected': isCardChecked(row),
                    }"
                    @click="previewReport(row)"
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
                        <span>{{ periodTypeText(row.periodType) }}</span>
                      </div>
                      <div class="srm-quarter-eval-card__meta">
                        <span>编号 {{ row.supplierCode || '-' }}</span>
                        <span>{{ periodText(row) }}</span>
                        <span>{{ statusText(row.status) }}</span>
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
                              !row.canEdit && row.status !== 'REJECTED'
                            "
                          >
                            编辑实际值
                          </Menu.Item>
                          <Menu.Item
                            key="pullMonthly"
                            :disabled="
                              row.periodType !== 'QUARTER' || !row.canEdit
                            "
                          >
                            获取月度值
                          </Menu.Item>
                          <Menu.Item key="submit" :disabled="!row.canSubmit">
                            提交
                          </Menu.Item>
                          <Menu.Item key="confirm" :disabled="!row.canConfirm">
                            确认
                          </Menu.Item>
                          <Menu.Item key="reject" :disabled="!row.canConfirm">
                            退回
                          </Menu.Item>
                          <Menu.Divider />
                          <Menu.Item
                            key="delete"
                            danger
                            :disabled="!row.canEdit"
                          >
                            删除
                          </Menu.Item>
                        </Menu>
                      </template>
                    </Dropdown>
                  </div>
                </div>
                <div v-else class="srm-quarter-card-empty">
                  暂无绩效实际上报单
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
            <Spin :spinning="selectedReportLoading">
              <div class="srm-preview-panel">
                <div class="srm-preview-panel__head">
                  <div>
                    <div class="srm-preview-panel__title">
                      {{ selectedReportTitle }}
                    </div>
                    <div
                      v-if="selectedReport"
                      class="srm-preview-panel__subtitle"
                    >
                      {{ statusText(selectedReport.status) }} ·
                      {{ selectedReport.values.length }} 项实际值
                    </div>
                  </div>
                  <Dropdown v-if="selectedReport" :trigger="['click']">
                    <Button size="small">
                      <IconifyIcon icon="lucide:menu" />
                      操作
                      <IconifyIcon icon="lucide:chevron-down" />
                    </Button>
                    <template #overlay>
                      <Menu @click="handleRowMenuClick($event, selectedReport)">
                        <Menu.Item key="detail">详情</Menu.Item>
                        <Menu.Item
                          key="edit"
                          :disabled="
                            !selectedReport.canEdit &&
                            selectedReport.status !== 'REJECTED'
                          "
                        >
                          编辑实际值
                        </Menu.Item>
                        <Menu.Item
                          key="pullMonthly"
                          :disabled="
                            selectedReport.periodType !== 'QUARTER' ||
                            !selectedReport.canEdit
                          "
                        >
                          获取月度值
                        </Menu.Item>
                        <Menu.Item
                          key="submit"
                          :disabled="!selectedReport.canSubmit"
                        >
                          提交
                        </Menu.Item>
                        <Menu.Item
                          key="confirm"
                          :disabled="!selectedReport.canConfirm"
                        >
                          确认
                        </Menu.Item>
                        <Menu.Item key="collapse">收起右侧</Menu.Item>
                      </Menu>
                    </template>
                  </Dropdown>
                </div>
                <template v-if="selectedReport">
                  <div class="srm-preview-panel__stats">
                    <span>{{ periodTypeText(selectedReport.periodType) }}</span>
                    <span>{{ periodText(selectedReport) }}</span>
                    <span>
                      上报人 {{ selectedReport.reporterUserName || '-' }}
                    </span>
                  </div>
                  <Table
                    :columns="previewValueColumns"
                    :data-source="selectedReport.values"
                    :pagination="false"
                    row-key="id"
                    :scroll="{ x: 840, y: 620 }"
                    size="small"
                  >
                    <template #bodyCell="{ column, index, record }">
                      <template v-if="column.dataIndex === 'seq'">
                        {{ index + 1 }}
                      </template>
                      <template v-else-if="column.dataIndex === 'metricName'">
                        <div class="srm-source-metric">
                          <strong>{{ record.metricName || '-' }}</strong>
                          <span v-if="record.sourceIndicatorName">
                            季度指标：{{ record.sourceIndicatorName }}
                          </span>
                          <small v-if="record.calcDescription">
                            {{ record.calcDescription }}
                          </small>
                        </div>
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
                  点击左侧实际上报单查看明细
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
      title="批量新建绩效实际上报"
      width="980px"
      :z-index="SRM_NESTED_MODAL_Z_INDEX"
      @ok="handleBatchGenerate"
    >
      <Form layout="vertical">
        <div class="srm-generate-form-grid">
          <Form.Item label="期间类型">
            <Select
              v-model:value="generateForm.periodType"
              :dropdown-style="{ zIndex: SRM_NESTED_DROPDOWN_Z_INDEX }"
              :get-popup-container="resolveNestedPopupContainer"
              :options="periodTypeOptions()"
              @change="handleGeneratePeriodTypeChange"
            />
          </Form.Item>
          <Form.Item label="年度/期间">
            <div
              class="srm-period-control"
              :class="{ 'is-month': generateForm.periodType === 'MONTH' }"
            >
              <Button title="上一期" @click="shiftGeneratePeriod(-1)">
                <IconifyIcon icon="lucide:chevron-left" />
              </Button>
              <InputNumber
                v-model:value="generateForm.evalYear"
                class="srm-query-year"
                :min="2000"
                @change="loadAvailableSuppliers"
              />
              <Select
                v-if="generateForm.periodType === 'MONTH'"
                v-model:value="generateForm.evalMonth"
                class="srm-query-month"
                :dropdown-style="{ zIndex: SRM_NESTED_DROPDOWN_Z_INDEX }"
                :get-popup-container="resolveNestedPopupContainer"
                :options="monthOptions()"
                @change="loadAvailableSuppliers"
              />
              <Select
                v-else
                v-model:value="generateForm.evalQuarter"
                class="srm-query-quarter"
                :dropdown-style="{ zIndex: SRM_NESTED_DROPDOWN_Z_INDEX }"
                :get-popup-container="resolveNestedPopupContainer"
                :options="quarterOptions()"
                @change="loadAvailableSuppliers"
              />
              <Button title="下一期" @click="shiftGeneratePeriod(1)">
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
          <Form.Item label=" ">
            <Button
              :loading="availableSupplierLoading"
              @click="loadAvailableSuppliers"
            >
              <IconifyIcon icon="lucide:refresh-cw" />
              刷新名单
            </Button>
          </Form.Item>
        </div>
        <div class="srm-generate-hint">
          下表仅显示已启用供应商季度评分配置，且目标期间未生成绩效实际上报单的供应商；生成后自动按季度考核模板计算树生成实际值明细。
        </div>
        <Table
          :columns="generateSupplierColumns"
          :data-source="availableSuppliers"
          :loading="availableSupplierLoading"
          :pagination="{ pageSize: 10, showSizeChanger: false }"
          row-key="supplierId"
          :row-selection="generateSupplierRowSelection"
          :scroll="{ x: 850, y: 420 }"
          size="small"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'templateNameSnapshot'">
              {{
                [record.templateCodeSnapshot, record.templateNameSnapshot]
                  .filter(Boolean)
                  .join(' / ') || '-'
              }}
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
              <div class="qms-ncr-title-panel__name">绩效实际上报</div>
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
                v-if="isReadonly && form.canEdit !== false"
                class="qms-ncr-toolbar-action"
                type="primary"
                @click="switchToEdit"
              >
                <IconifyIcon icon="lucide:edit-3" />
                编辑实际值
              </Button>
              <Button
                v-if="canEdit"
                class="qms-ncr-toolbar-action"
                type="primary"
                @click="saveReport(false)"
              >
                <IconifyIcon icon="lucide:save" />
                保存
              </Button>
              <Button
                v-if="canEdit && form.periodType === 'QUARTER'"
                class="qms-ncr-toolbar-action"
                @click="handlePullMonthly()"
              >
                <IconifyIcon icon="lucide:database-zap" />
                获取月度值
              </Button>
              <Button
                v-if="form.id && form.canSubmit"
                class="qms-ncr-toolbar-action"
                @click="handleSubmit()"
              >
                <IconifyIcon icon="lucide:send" />
                提交
              </Button>
              <Button
                v-if="form.id && form.canConfirm"
                class="qms-ncr-toolbar-action"
                type="primary"
                @click="handleConfirm()"
              >
                <IconifyIcon icon="lucide:badge-check" />
                确认
              </Button>
              <Button
                v-if="form.id && form.canConfirm"
                class="qms-ncr-toolbar-action"
                danger
                @click="handleReject()"
              >
                <IconifyIcon icon="lucide:undo-2" />
                退回
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
                      <strong>上报基本信息</strong>
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
                      <label class="erp-form-label">上报单号</label>
                      <div class="erp-form-value">
                        <span class="qms-exception-readonly-value">
                          {{ form.reportNo || '-' }}
                        </span>
                      </div>
                    </div>
                    <div class="erp-form-item">
                      <label class="erp-form-label">供应商</label>
                      <div class="erp-form-value">
                        <span class="qms-exception-readonly-value">
                          {{ form.supplierName || '-' }}
                        </span>
                      </div>
                    </div>
                    <div class="erp-form-item">
                      <label class="erp-form-label">期间类型</label>
                      <div class="erp-form-value">
                        <span class="qms-exception-readonly-value">
                          {{ periodTypeText(form.periodType) }}
                        </span>
                      </div>
                    </div>
                    <div class="erp-form-item">
                      <label class="erp-form-label">年度</label>
                      <div class="erp-form-value">
                        <span>{{ form.evalYear || '-' }}</span>
                      </div>
                    </div>
                    <div class="erp-form-item">
                      <label class="erp-form-label">季度</label>
                      <div class="erp-form-value">
                        <span>Q{{ form.evalQuarter || '-' }}</span>
                      </div>
                    </div>
                    <div class="erp-form-item">
                      <label class="erp-form-label">月份</label>
                      <div class="erp-form-value">
                        <span>
                          {{ form.evalMonth ? `${form.evalMonth}月` : '-' }}
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
                      <label class="erp-form-label">上报人</label>
                      <div class="erp-form-value">
                        <span>{{ form.reporterUserName || '-' }}</span>
                      </div>
                    </div>
                    <div class="erp-form-item erp-form-item--full">
                      <label class="erp-form-label erp-form-label--tall">
                        备注
                      </label>
                      <div class="erp-form-value">
                        <span
                          class="qms-exception-readonly-value qms-exception-readonly-value--multiline"
                        >
                          {{ form.remark || '-' }}
                        </span>
                      </div>
                    </div>
                  </div>
                </section>

                <section class="erp-basic-form srm-detail-section">
                  <div class="detail-list-head">
                    <div class="detail-list-title">
                      <strong>实际值明细（{{ form.values.length }} 项）</strong>
                    </div>
                  </div>
                  <Table
                    :columns="valueColumns"
                    :data-source="form.values"
                    :pagination="false"
                    row-key="id"
                    :scroll="{ x: valueTableScrollX, y: 430 }"
                    size="small"
                  >
                    <template #bodyCell="{ column, index, record }">
                      <template v-if="column.dataIndex === 'seq'">
                        {{ index + 1 }}
                      </template>
                      <template v-else-if="column.dataIndex === 'metricName'">
                        <div class="srm-source-metric">
                          <strong>{{ record.metricName || '-' }}</strong>
                          <span v-if="record.sourceIndicatorName">
                            季度指标：{{ record.sourceIndicatorName }}
                          </span>
                          <small v-if="record.calcDescription">
                            {{ record.calcDescription }}
                          </small>
                        </div>
                      </template>
                      <template v-else-if="column.dataIndex === 'numericValue'">
                        <InputNumber
                          v-if="canEdit"
                          v-model:value="record.numericValue"
                          class="srm-value-input"
                          :precision="4"
                        />
                        <span v-else>
                          {{ displayValue(record.numericValue) }}
                        </span>
                      </template>
                      <template v-else-if="column.dataIndex === 'remark'">
                        <Input
                          v-if="canEdit"
                          v-model:value="record.remark"
                          placeholder="填写实际说明"
                        />
                        <span v-else>{{ displayValue(record.remark) }}</span>
                      </template>
                      <template v-else-if="column.dataIndex === 'attachment'">
                        <Button
                          size="small"
                          type="link"
                          @click="openAttachment(record)"
                        >
                          附件
                        </Button>
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
      v-model:open="attachmentOpen"
      :footer="null"
      :title="`实际值证据｜${selectedValue?.metricName || ''}`"
      width="860px"
      :z-index="SRM_NESTED_MODAL_Z_INDEX"
    >
      <SrmAttachmentPanel
        v-if="selectedValue?.id"
        :biz-id="selectedValue.id"
        :biz-type="ATTACHMENT_BIZ_TYPE"
        :mode="canEdit ? 'edit' : 'detail'"
      />
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
  width: 100%;
  min-width: 0;
  flex-direction: column;
  align-items: stretch;
  gap: 8px;
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

.srm-quarter-panel-toggle {
  flex-shrink: 0;
}

.srm-query-input {
  width: 220px;
}

.srm-period-type-switch {
  display: inline-flex;
  height: 32px;
  align-items: center;
  border: 1px solid #dbe3ef;
  border-radius: 999px;
  background: #f8fafc;
  padding: 2px;
}

.srm-period-type-switch :deep(.ant-segmented-group) {
  gap: 2px;
}

.srm-period-type-switch :deep(.ant-segmented-item) {
  min-width: 76px;
  border-radius: 999px;
  color: #475569;
  font-weight: 600;
  line-height: 26px;
}

.srm-period-type-switch :deep(.ant-segmented-thumb),
.srm-period-type-switch :deep(.ant-segmented-item-selected) {
  border-radius: 999px;
  background: #1677ff;
  color: #fff;
}

.srm-period-control {
  display: grid;
  align-items: center;
  grid-template-columns: 32px 96px 86px 32px;
}

.srm-period-control.is-month {
  grid-template-columns: 32px 96px 96px 32px;
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
.srm-period-control :deep(.ant-select),
.srm-period-control :deep(.ant-btn:last-child) {
  margin-left: -1px;
}

.srm-query-year {
  width: 96px;
}

.srm-query-quarter {
  width: 86px;
}

.srm-query-month {
  width: 96px;
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
  overflow: hidden;
  border: 1px solid #dbe3ef;
  border-radius: 6px;
  background: #fff;
  box-shadow: 0 8px 22px rgb(15 23 42 / 5%);
}

.srm-quarter-list-card :deep(.vben-vxe-grid),
.srm-quarter-items-card :deep(.ant-spin-nested-loading),
.srm-quarter-items-card :deep(.ant-spin-container) {
  height: 100%;
  min-height: 0;
}

.srm-quarter-list-head {
  display: flex;
  min-height: 58px;
  flex-shrink: 0;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
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
  grid-template-columns: minmax(0, 1fr) 90px 70px;
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
  min-height: 32px;
  flex-shrink: 0;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
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

.srm-source-metric {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 3px;
  line-height: 18px;
}

.srm-source-metric strong {
  color: #10233d;
  font-size: 13px;
}

.srm-source-metric span {
  color: #475569;
  font-size: 12px;
}

.srm-source-metric small {
  color: #64748b;
  font-size: 12px;
  white-space: normal;
}

.srm-generate-form-grid {
  display: grid;
  align-items: start;
  grid-template-columns: 130px 292px minmax(180px, 1fr) 116px;
  gap: 12px;
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

.srm-value-input {
  width: 132px;
}

:deep(.vben-vxe-grid) {
  display: flex;
  height: 100%;
  flex-direction: column;
}

@media (max-width: 1280px) {
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
