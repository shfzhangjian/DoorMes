<script lang="ts" setup>
import type { TableColumnsType, TablePaginationConfig } from 'ant-design-vue';

import type { SrmReferenceType } from './crud';

import { computed, reactive, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';

import {
  Button,
  Form,
  Input,
  message,
  Pagination,
  Radio,
  Select,
  Table,
  Tag,
} from 'ant-design-vue';

import { getDocumentPage } from '#/api/mes/srm/document';
import { getProjectPage } from '#/api/mes/srm/preliminary-project';
import { getSampleEvaluationSampleRequestPage } from '#/api/mes/srm/sample-evaluation';
import { getMetricPage } from '#/api/mes/srm/standard/metric';
import { getTemplatePage } from '#/api/mes/srm/standard/template';
import { getSupplierCandidateSelectPage } from '#/api/mes/srm/supplier-candidate';

import { getSrmStatusColor, getSrmValueLabel } from './crud';

type ReferenceRow = Record<string, any> & { key: string };
type SupplierSource = 'candidate' | 'roster';
type PageLike = {
  items?: Record<string, any>[];
  list?: Record<string, any>[];
  total?: number;
};
type LoadPage = (params: Record<string, any>) => Promise<PageLike>;
type ReferenceSelectType = Exclude<SrmReferenceType, 'material' | 'productBom'>;

interface SearchField {
  component?: 'Input' | 'RadioGroup' | 'Select';
  field: string;
  label: string;
  options?: Array<{ label: string; value: any }>;
  placeholder?: string;
  widthClass?: string;
}

interface SelectorConfig {
  columns: TableColumnsType<ReferenceRow>;
  defaultSearchField: string;
  loadPage: LoadPage;
  rowKey: string;
  scrollX: number;
  searchFields: SearchField[];
  title: string;
}

defineOptions({ name: 'SrmReferenceSelectModal' });

const emit = defineEmits<{
  select: [row: Record<string, any>];
  // 兼容既有模板监听写法：@select-multiple
  'select-multiple': [rows: Record<string, any>[]];
}>();

const metricTypeOptions = [
  { label: '定量', value: 1 },
  { label: '定性', value: 2 },
  { label: '红线', value: 3 },
];
const periodTypeOptions = [
  { label: '选择初评', value: 'PRELIMINARY' },
  { label: '季度考核', value: 'QUARTER' },
  { label: '年度考核', value: 'YEAR' },
  { label: '专项稽核', value: 'AUDIT' },
];
const supplierStatusFilterOptions = [
  { label: '全部', value: '' },
  { label: '考察中', value: 'PENDING' },
  { label: '合格', value: 'QUALIFIED' },
  { label: '冻结', value: 'FROZEN' },
  { label: '淘汰', value: 'ELIMINATED' },
  { label: '退出', value: 'EXITED' },
  { label: '不合格', value: 'UNQUALIFIED' },
];
const projectStatusOptions = [
  { label: '启用', value: 'ENABLED' },
  { label: '停用', value: 'DISABLED' },
];
const selectorConfigMap: Record<ReferenceSelectType, SelectorConfig> = {
  supplier: {
    columns: [
      { dataIndex: 'supplierCode', title: '供应商代码', width: 180 },
      { dataIndex: 'supplierName', title: '供应商名称', width: 320 },
    ],
    defaultSearchField: 'supplierName',
    loadPage: (params) =>
      getSupplierCandidateSelectPage({
        ...params,
        distinctSupplier: params.distinctSupplier ?? true,
      }) as Promise<PageLike>,
    rowKey: 'candidateKey',
    scrollX: 560,
    searchFields: [
      {
        component: 'RadioGroup',
        field: 'status',
        label: '资源状态',
        options: supplierStatusFilterOptions,
        widthClass: 'srm-reference-select__radio-group',
      },
      {
        field: 'supplierCode',
        label: '供应商代码',
        placeholder: '请输入供应商代码',
      },
      {
        field: 'supplierName',
        label: '供应商名称',
        placeholder: '请输入供应商名称',
      },
    ],
    title: '选择供应商',
  },
  metric: {
    columns: [
      { dataIndex: 'code', title: '指标编码', width: 150 },
      { dataIndex: 'name', title: '指标题干/名称', width: 260 },
      { dataIndex: 'category', title: '所属维度', width: 120 },
      { dataIndex: 'type', title: '指标类型', width: 110 },
      { dataIndex: 'scoringMethod', title: '评分规则与计算逻辑', width: 430 },
      { dataIndex: 'dataSource', title: '客观数据来源/证据要求', width: 330 },
    ],
    defaultSearchField: 'name',
    loadPage: (params) => getMetricPage(params) as Promise<PageLike>,
    rowKey: 'id',
    scrollX: 1500,
    searchFields: [
      { field: 'code', label: '指标编码', placeholder: '请输入指标编码' },
      { field: 'name', label: '指标名称', placeholder: '请输入指标名称' },
      {
        component: 'Select',
        field: 'category',
        label: '所属维度',
        options: [
          { label: '质量', value: '质量' },
          { label: '交付', value: '交付' },
          { label: '成本', value: '成本' },
          { label: '服务', value: '服务' },
          { label: '技术', value: '技术' },
          { label: '体系', value: '体系' },
          { label: 'EHS', value: 'EHS' },
          { label: 'IT', value: 'IT' },
        ],
        placeholder: '请选择维度',
        widthClass: 'srm-reference-select__status',
      },
    ],
    title: '选择考核指标',
  },
  performancePlan: {
    columns: [
      { dataIndex: 'docNo', title: '计划单号', width: 170 },
      { dataIndex: 'title', title: '计划标题', width: 280 },
      { dataIndex: 'periodType', title: '适用场景', width: 130 },
      { align: 'right', dataIndex: 'evalYear', title: '年份', width: 90 },
      { align: 'right', dataIndex: 'evalQuarter', title: '季度', width: 90 },
      { dataIndex: 'dueDate', title: '打分截止日期', width: 150 },
      { dataIndex: 'remark', title: '备注', width: 420 },
    ],
    defaultSearchField: 'title',
    loadPage: (params) =>
      getDocumentPage({
        ...params,
        bizType: 'SRM_PERFORMANCE_PLAN',
      }) as Promise<PageLike>,
    rowKey: 'id',
    scrollX: 1450,
    searchFields: [
      { field: 'docNo', label: '计划单号', placeholder: '请输入计划单号' },
      { field: 'title', label: '计划标题', placeholder: '请输入计划标题' },
      {
        component: 'Select',
        field: 'periodType',
        label: '适用场景',
        options: periodTypeOptions,
        placeholder: '请选择场景',
        widthClass: 'srm-reference-select__status',
      },
    ],
    title: '选择供方评审计划',
  },
  sampleRequest: {
    columns: [
      { dataIndex: 'requestNo', title: '申请编号', width: 190 },
      { dataIndex: 'materialName', title: '品名', width: 240 },
      { dataIndex: 'materialModel', title: '规格/型号', width: 180 },
      { dataIndex: 'supplierName', title: '供应商名称', width: 220 },
      {
        align: 'right',
        dataIndex: 'requireQty',
        title: '需求数量',
        width: 120,
      },
      { dataIndex: 'applyDept', title: '申请部门', width: 140 },
      { dataIndex: 'applyDate', title: '申请日期', width: 140 },
      {
        align: 'right',
        dataIndex: 'sampleEvaluationCount',
        title: '送检次数',
        width: 110,
      },
    ],
    defaultSearchField: 'keyword',
    loadPage: (params) =>
      getSampleEvaluationSampleRequestPage(params) as Promise<PageLike>,
    rowKey: 'id',
    scrollX: 1340,
    searchFields: [
      {
        field: 'keyword',
        label: '样品需求',
        placeholder: '请输入申请编号、品名或供应商名称',
      },
      {
        field: 'requestNo',
        label: '申请编号',
        placeholder: '请输入申请编号',
      },
      { field: 'materialName', label: '品名', placeholder: '请输入品名' },
    ],
    title: '选择样品需求单',
  },
  project: {
    columns: [
      { dataIndex: 'projectCode', title: '项目编码', width: 180 },
      { dataIndex: 'projectName', title: '项目名称', width: 300 },
      { dataIndex: 'status', title: '状态', width: 110 },
      { dataIndex: 'remark', title: '备注', width: 460 },
    ],
    defaultSearchField: 'projectName',
    loadPage: (params) => getProjectPage(params) as Promise<PageLike>,
    rowKey: 'id',
    scrollX: 1050,
    searchFields: [
      {
        field: 'projectCode',
        label: '项目编码',
        placeholder: '请输入项目编码',
      },
      {
        field: 'projectName',
        label: '项目名称',
        placeholder: '请输入项目名称',
      },
      {
        component: 'Select',
        field: 'status',
        label: '状态',
        options: projectStatusOptions,
        placeholder: '请选择状态',
        widthClass: 'srm-reference-select__status',
      },
    ],
    title: '选择初评项目',
  },
  template: {
    columns: [
      { dataIndex: 'name', title: '模板名称', width: 280 },
      { dataIndex: 'currentVersionNo', title: '当前版本', width: 170 },
      { dataIndex: 'materialType', title: '适用物料类别', width: 180 },
      { dataIndex: 'periodType', title: '适用场景', width: 130 },
      { align: 'right', dataIndex: 'totalScore', title: '满分', width: 90 },
      { dataIndex: 'remark', title: '备注', width: 520 },
    ],
    defaultSearchField: 'name',
    loadPage: (params) => getTemplatePage(params) as Promise<PageLike>,
    rowKey: 'id',
    scrollX: 1320,
    searchFields: [
      { field: 'name', label: '模板名称', placeholder: '请输入模板名称' },
      {
        field: 'materialType',
        label: '物料类别',
        placeholder: '请输入物料类别',
      },
      {
        component: 'Select',
        field: 'periodType',
        label: '适用场景',
        options: periodTypeOptions,
        placeholder: '请选择场景',
        widthClass: 'srm-reference-select__status',
      },
    ],
    title: '选择考核模板',
  },
};

const supplierRosterConfig: SelectorConfig = {
  columns: [
    { dataIndex: 'supplierCode', title: '供应商代码', width: 160 },
    { dataIndex: 'supplierName', title: '供应商名称', width: 260 },
    { dataIndex: 'materialCode', title: '物料编码', width: 170 },
    { dataIndex: 'providedProduct', title: '提供/协作产品', width: 220 },
    { dataIndex: 'model', title: '型号', width: 180 },
    { dataIndex: 'scopeName', title: '管理范围', width: 180 },
  ],
  defaultSearchField: 'supplierName',
  loadPage: (params) =>
    getSupplierCandidateSelectPage({
      ...params,
      distinctSupplier: params.distinctSupplier ?? true,
    }) as Promise<PageLike>,
  rowKey: 'candidateKey',
  scrollX: 1170,
  searchFields: [
    {
      component: 'RadioGroup',
      field: 'status',
      label: '资源状态',
      options: supplierStatusFilterOptions,
      widthClass: 'srm-reference-select__radio-group',
    },
    {
      field: 'supplierCode',
      label: '供应商代码',
      placeholder: '请输入供应商代码',
    },
    {
      field: 'supplierName',
      label: '供应商名称',
      placeholder: '请输入供应商名称',
    },
  ],
  title: '选择供应商',
};

const loading = ref(false);
const effectiveModalZIndex = ref<number>();
const multiple = ref(false);
const referenceType = ref<ReferenceSelectType>('supplier');
const supplierSource = ref<SupplierSource>('candidate');
const rows = ref<ReferenceRow[]>([]);
const searchState = reactive<Record<string, any>>({});
const fixedSearchParams = ref<Record<string, any>>({});
const selectedRowKeys = ref<string[]>([]);
const selectedRowMap = reactive<Record<string, ReferenceRow>>({});

const pagination = reactive<TablePaginationConfig>({
  current: 1,
  pageSize: 10,
  showSizeChanger: true,
  total: 0,
});

const currentConfig = computed(() => {
  if (referenceType.value === 'supplier' && supplierSource.value === 'roster') {
    return supplierRosterConfig;
  }
  return selectorConfigMap[referenceType.value] || selectorConfigMap.supplier;
});
const fixedSearchFieldSet = computed(
  () => new Set(Object.keys(fixedSearchParams.value)),
);
const statusSearchFields = computed(() =>
  currentConfig.value.searchFields.filter(
    (field) =>
      field.component === 'RadioGroup' &&
      !fixedSearchFieldSet.value.has(field.field),
  ),
);
const normalSearchFields = computed(() =>
  currentConfig.value.searchFields.filter(
    (field) =>
      field.component !== 'RadioGroup' &&
      !fixedSearchFieldSet.value.has(field.field),
  ),
);
const selectedRows = computed(() =>
  selectedRowKeys.value
    .map(
      (key) => selectedRowMap[key] || rows.value.find((row) => row.key === key),
    )
    .filter((row): row is ReferenceRow => !!row),
);
const selectedRow = computed(() => selectedRows.value[0]);
const referenceDropdownStyle = computed(() =>
  effectiveModalZIndex.value
    ? { zIndex: effectiveModalZIndex.value + 10 }
    : undefined,
);
const rowSelection = computed(() => ({
  getCheckboxProps: (record: ReferenceRow) => ({
    disabled: isReferenceRowDisabled(record),
  }),
  onChange: (keys: Array<number | string>, selectedRecords: ReferenceRow[]) => {
    const validRecords = selectedRecords.filter(
      (record) => !isReferenceRowDisabled(record),
    );
    syncSelectedRows(
      validRecords.map((record) => record.key),
      validRecords,
    );
  },
  selectedRowKeys: selectedRowKeys.value,
  type: multiple.value ? ('checkbox' as const) : ('radio' as const),
}));

const [Modal, modalApi] = useVbenModal({
  class: 'srm-reference-select-modal w-[1360px] max-w-[96vw]',
  closeOnClickModal: false,
  contentClass: 'srm-reference-select-modal__body',
  footerClass: 'srm-reference-select-modal__footer',
  title: '选择引用数据',
  onConfirm: confirmSelection,
  async onOpenChange(isOpen) {
    if (!isOpen) {
      supplierSource.value = 'candidate';
      fixedSearchParams.value = {};
      resetState();
      return;
    }
    const data =
      modalApi.getData<{
        fixedSearchParams?: Record<string, any>;
        initialSearchParams?: Record<string, any>;
        keyword?: string;
        modalZIndex?: number;
        multiple?: boolean;
        referenceType?: SrmReferenceType;
        supplierSource?: SupplierSource;
        title?: string;
      }>() || {};
    referenceType.value = resolveReferenceType(data.referenceType);
    supplierSource.value =
      data.supplierSource === 'roster' ? 'roster' : 'candidate';
    multiple.value = Boolean(data.multiple);
    fixedSearchParams.value = data.fixedSearchParams
      ? { ...data.fixedSearchParams }
      : {};
    effectiveModalZIndex.value = data.modalZIndex ?? 5200;
    modalApi.setState({ title: data.title || currentConfig.value.title });
    resetState(data.keyword, data.initialSearchParams);
    await loadRows();
  },
});

function resetState(
  keyword?: string,
  initialSearchParams: Record<string, any> = {},
) {
  Object.keys(searchState).forEach((key) => {
    delete searchState[key];
  });
  currentConfig.value.searchFields.forEach((field) => {
    searchState[field.field] = field.component === 'Select' ? undefined : '';
  });
  const normalizedKeyword = normalizeKeyword(keyword);
  if (normalizedKeyword) {
    searchState[currentConfig.value.defaultSearchField] = normalizedKeyword;
  }
  Object.assign(searchState, initialSearchParams);
  Object.assign(searchState, fixedSearchParams.value);
  rows.value = [];
  selectedRowKeys.value = [];
  clearSelectedRowMap();
  pagination.current = 1;
  pagination.pageSize = 10;
  pagination.total = 0;
}

function resolveReferenceType(type?: SrmReferenceType): ReferenceSelectType {
  if (
    type &&
    type !== 'material' &&
    type !== 'productBom' &&
    type in selectorConfigMap
  ) {
    return type as ReferenceSelectType;
  }
  return 'supplier';
}

async function loadRows() {
  loading.value = true;
  try {
    const result = await currentConfig.value.loadPage({
      ...cleanParams(searchState),
      ...fixedSearchParams.value,
      pageNo: Number(pagination.current || 1),
      pageSize: Number(pagination.pageSize || 10),
    });
    const list = result.list || result.items || [];
    rows.value = list.map((item, index) => ({
      ...item,
      key: buildRowKey(item, index),
    }));
    rows.value.forEach((row) => {
      if (selectedRowKeys.value.includes(row.key)) {
        selectedRowMap[row.key] = row;
      }
    });
    pagination.total = result.total ?? rows.value.length;
    if (
      !multiple.value &&
      !rows.value.some((row) => row.key === selectedRowKeys.value[0])
    ) {
      selectedRowKeys.value = [];
      clearSelectedRowMap();
    }
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  pagination.current = 1;
  void loadRows();
}

function handlePaginationChange(page: number, pageSize: number) {
  pagination.current = page;
  pagination.pageSize = pageSize;
  void loadRows();
}

function getPaginationTotal(total: number) {
  return `共 ${total} 条`;
}

async function confirmSelection() {
  if (multiple.value) {
    if (selectedRows.value.length === 0) {
      message.warning('请至少选择一条数据');
      return;
    }
    const disabledRow = selectedRows.value.find((row) =>
      isReferenceRowDisabled(row),
    );
    if (disabledRow) {
      warnDisabledRow(disabledRow);
      return;
    }
    emit(
      // eslint-disable-next-line vue/custom-event-name-casing
      'select-multiple',
      selectedRows.value.map((row) => stripInternalKey(row)),
    );
    await modalApi.close();
    return;
  }
  if (!selectedRow.value) {
    message.warning('请选择一条数据');
    return;
  }
  if (isReferenceRowDisabled(selectedRow.value)) {
    warnDisabledRow(selectedRow.value);
    return;
  }
  emit('select', stripInternalKey(selectedRow.value));
  await modalApi.close();
}

function resolvePopupContainer(triggerNode: HTMLElement) {
  return (
    (triggerNode.closest('[role="dialog"]') as HTMLElement | null) ||
    triggerNode.parentElement ||
    triggerNode.ownerDocument?.body ||
    document.body
  );
}

function handlePick(row: ReferenceRow) {
  if (isReferenceRowDisabled(row)) {
    warnDisabledRow(row);
    return;
  }
  if (multiple.value) {
    toggleSelectedRow(row);
    return;
  }
  emit('select', stripInternalKey(row));
  void modalApi.close();
}

function handleRowClick(row: ReferenceRow) {
  if (isReferenceRowDisabled(row)) {
    warnDisabledRow(row);
    return;
  }
  if (multiple.value) {
    return;
  }
  setSingleSelectedRow(row);
}

function getCustomRow(record: ReferenceRow) {
  return {
    onClick: () => handleRowClick(record),
    onDblclick: () => handlePick(record),
  };
}

function syncSelectedRows(
  keys: string[],
  selectedRecords: ReferenceRow[] = [],
) {
  selectedRowKeys.value = multiple.value ? keys : keys.slice(0, 1);
  selectedRecords.forEach((row) => {
    selectedRowMap[row.key] = row;
  });
  Object.keys(selectedRowMap).forEach((key) => {
    if (!selectedRowKeys.value.includes(key)) {
      delete selectedRowMap[key];
    }
  });
}

function setSingleSelectedRow(row?: ReferenceRow) {
  selectedRowKeys.value = row ? [row.key] : [];
  clearSelectedRowMap();
  if (row) {
    selectedRowMap[row.key] = row;
  }
}

function toggleSelectedRow(row: ReferenceRow) {
  if (isReferenceRowDisabled(row)) {
    warnDisabledRow(row);
    return;
  }
  if (selectedRowKeys.value.includes(row.key)) {
    syncSelectedRows(
      selectedRowKeys.value.filter((key) => key !== row.key),
      [],
    );
    return;
  }
  syncSelectedRows([...selectedRowKeys.value, row.key], [row]);
}

function clearSelectedRowMap() {
  Object.keys(selectedRowMap).forEach((key) => {
    delete selectedRowMap[key];
  });
}

function isReferenceRowDisabled(row: ReferenceRow) {
  return !!getReferenceRowDisabledReason(row);
}

function getReferenceRowDisabledReason(row: ReferenceRow) {
  if (referenceType.value !== 'template') {
    return '';
  }
  if (!row.currentVersionId || !row.currentVersionNo) {
    return '该评估模板暂无已发布版本，不能选择';
  }
  const latestVersion = row.currentVersion || {};
  const latestVersionId = normalizeId(latestVersion.id);
  const publishedVersionId = normalizeId(row.currentVersionId);
  if (
    latestVersionId &&
    publishedVersionId &&
    latestVersionId !== publishedVersionId &&
    latestVersion.status !== 'PUBLISHED'
  ) {
    return '该评估模板正在升级审核中，发布前不能选择';
  }
  if (latestVersion.status && latestVersion.status !== 'PUBLISHED') {
    return '该评估模板当前版本未发布，不能选择';
  }
  return '';
}

function warnDisabledRow(row: ReferenceRow) {
  const reason = getReferenceRowDisabledReason(row);
  if (reason) {
    message.warning(reason);
  }
}

function getRowClassName(record: ReferenceRow) {
  return isReferenceRowDisabled(record)
    ? 'srm-reference-select__row--disabled'
    : '';
}

function cleanParams(params: Record<string, any>) {
  return Object.fromEntries(
    Object.entries(params).filter(([, value]) => {
      if (Array.isArray(value)) {
        return value.length > 0;
      }
      return (
        value !== undefined && value !== null && String(value).trim() !== ''
      );
    }),
  );
}

function normalizeKeyword(keyword?: string) {
  const text = String(keyword || '').trim();
  return text && text !== '-' ? text : '';
}

function buildRowKey(row: Record<string, any>, index: number) {
  const value =
    row[currentConfig.value.rowKey] ||
    row.id ||
    row.materialCode ||
    row.code ||
    row.supplierCode ||
    row.name ||
    index;
  return `${referenceType.value}:${value}`;
}

function stripInternalKey(row: ReferenceRow) {
  const { key: _key, ...rest } = row;
  return rest;
}

function displayValue(value?: unknown) {
  if (value === undefined || value === null || value === '') {
    return '-';
  }
  return String(value);
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

function isMaskedCell(record: ReferenceRow, dataIndex?: unknown) {
  const field = String(dataIndex || '');
  return (
    Array.isArray(record.maskedFields) && record.maskedFields.includes(field)
  );
}

function getCellValue(record: ReferenceRow, dataIndex?: unknown) {
  if (typeof dataIndex !== 'string') {
    return undefined;
  }
  return record[dataIndex];
}

function isPrimaryCode(dataIndex?: unknown) {
  return [
    'code',
    'docNo',
    'materialCode',
    'projectCode',
    'requestNo',
    'supplierCode',
  ].includes(String(dataIndex || ''));
}

function isPrimaryName(dataIndex?: unknown) {
  return [
    'materialName',
    'name',
    'projectName',
    'supplierName',
    'title',
  ].includes(String(dataIndex || ''));
}

function isLongText(dataIndex?: unknown) {
  return ['dataSource', 'remark', 'scoringMethod'].includes(
    String(dataIndex || ''),
  );
}

function isStatusColumn(dataIndex?: unknown) {
  return ['materialStatus', 'status'].includes(String(dataIndex || ''));
}

function isTemplateVersionColumn(dataIndex?: unknown) {
  return referenceType.value === 'template' && dataIndex === 'currentVersionNo';
}

function getSecondaryText(record: ReferenceRow, dataIndex?: unknown) {
  const field = String(dataIndex || '');
  if (referenceType.value === 'supplier' && supplierSource.value === 'roster') {
    return '';
  }
  if (referenceType.value === 'supplier') {
    return '';
  }
  if (field === 'supplierName') {
    return record.supplierCode || record.model || '';
  }
  if (referenceType.value === 'project' && field === 'projectName') {
    return record.projectCode || '';
  }
  if (referenceType.value === 'material' && field === 'materialName') {
    return record.specModel || record.materialCode || '';
  }
  if (referenceType.value === 'metric' && field === 'name') {
    return record.code || record.dataSource || '';
  }
  if (referenceType.value === 'template' && field === 'name') {
    return [getPeriodTypeLabel(record.periodType), record.materialType]
      .filter(Boolean)
      .join(' / ');
  }
  if (referenceType.value === 'performancePlan' && field === 'title') {
    return [record.docNo, getPeriodTypeLabel(record.periodType)]
      .filter(Boolean)
      .join(' / ');
  }
  if (referenceType.value === 'sampleRequest' && field === 'materialName') {
    return [record.requestNo, record.materialModel].filter(Boolean).join(' / ');
  }
  return '';
}

function getTemplateCurrentVersionText(record: ReferenceRow) {
  return displayValue(
    record.currentVersionNo || record.currentVersion?.versionNo,
  );
}

function getTemplateUpgradeVersionText(record: ReferenceRow) {
  const latestVersion = record.currentVersion || {};
  const latestVersionId = normalizeId(latestVersion.id);
  const publishedVersionId = normalizeId(record.currentVersionId);
  if (
    !latestVersionId ||
    !publishedVersionId ||
    latestVersionId === publishedVersionId ||
    latestVersion.status === 'PUBLISHED'
  ) {
    return '';
  }
  return `${displayValue(latestVersion.versionNo)} ${getTemplateVersionStatusLabel(
    latestVersion.status,
  )}`;
}

function getTemplateVersionStatusLabel(value?: unknown) {
  return (
    (
      {
        APPROVED: '待发布',
        DRAFT: '升级草稿',
        PENDING_AUDIT: '审核中',
        REJECTED: '审核驳回',
      } as Record<string, string>
    )[String(value || '')] || '未发布'
  );
}

function getTypeLabel(value?: unknown) {
  return (
    metricTypeOptions.find((item) => String(item.value) === String(value))
      ?.label || displayValue(value)
  );
}

function getPeriodTypeLabel(value?: unknown) {
  return (
    periodTypeOptions.find((item) => String(item.value) === String(value))
      ?.label || displayValue(value)
  );
}

function getStatusText(value?: unknown) {
  return getSrmValueLabel(value);
}

function getStatusColor(record: ReferenceRow, dataIndex?: unknown) {
  const value = getCellValue(record, dataIndex);
  return getSrmStatusColor(value);
}

function getStatusLabel(record: ReferenceRow, dataIndex?: unknown) {
  const value = getCellValue(record, dataIndex);
  return getStatusText(value);
}
</script>

<template>
  <Modal :z-index="effectiveModalZIndex || undefined">
    <div class="qms-product-source-select srm-reference-select">
      <div
        v-if="statusSearchFields.length > 0"
        class="srm-reference-select__status-row"
      >
        <div
          v-for="field in statusSearchFields"
          :key="field.field"
          class="srm-reference-select__status-filter"
        >
          <span class="srm-reference-select__status-label">
            {{ field.label }}：
          </span>
          <Radio.Group
            v-model:value="searchState[field.field]"
            button-style="solid"
            :class="field.widthClass || 'srm-reference-select__radio-group'"
            option-type="button"
            :options="field.options || []"
            @change="handleSearch"
          />
        </div>
      </div>
      <Form class="qms-product-source-select__search" layout="inline">
        <Form.Item
          v-for="field in normalSearchFields"
          :key="field.field"
          :label="field.label"
        >
          <Select
            v-if="field.component === 'Select'"
            v-model:value="searchState[field.field]"
            allow-clear
            :class="field.widthClass || 'qms-product-source-select__select'"
            :dropdown-style="referenceDropdownStyle"
            :get-popup-container="resolvePopupContainer"
            :options="field.options || []"
            :placeholder="field.placeholder || `请选择${field.label}`"
            popup-class-name="srm-reference-select__dropdown"
          />
          <Input
            v-else
            v-model:value="searchState[field.field]"
            allow-clear
            autocomplete="off"
            :placeholder="field.placeholder || `请输入${field.label}`"
            @press-enter="handleSearch"
          />
        </Form.Item>
        <Form.Item>
          <Button type="primary" @click="handleSearch">查询</Button>
        </Form.Item>
      </Form>

      <div class="qms-product-source-select__table-wrap">
        <Table
          bordered
          class="qms-product-source-select__table"
          :columns="currentConfig.columns"
          :custom-row="getCustomRow"
          :data-source="rows"
          :loading="loading"
          :pagination="false"
          row-key="key"
          :row-class-name="getRowClassName"
          :row-selection="rowSelection"
          :scroll="{ x: currentConfig.scrollX, y: '100%' }"
          size="small"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="isPrimaryCode(column.dataIndex)">
              <span class="font-mono font-bold text-blue-700">
                {{ displayValue(getCellValue(record, column.dataIndex)) }}
              </span>
            </template>
            <template v-else-if="isPrimaryName(column.dataIndex)">
              <div class="leading-tight">
                <div class="font-bold text-slate-800">
                  {{ displayValue(getCellValue(record, column.dataIndex)) }}
                </div>
                <div
                  v-if="getSecondaryText(record, column.dataIndex)"
                  class="mt-1 text-xs text-slate-400"
                >
                  {{ getSecondaryText(record, column.dataIndex) }}
                </div>
              </div>
            </template>
            <template v-else-if="column.dataIndex === 'type'">
              <Tag color="blue" class="!m-0">
                {{ getTypeLabel(record.type) }}
              </Tag>
            </template>
            <template v-else-if="column.dataIndex === 'periodType'">
              <Tag color="processing" class="!m-0">
                {{ getPeriodTypeLabel(record.periodType) }}
              </Tag>
            </template>
            <template v-else-if="isTemplateVersionColumn(column.dataIndex)">
              <div class="srm-reference-select__version-cell">
                <span>{{ getTemplateCurrentVersionText(record) }}</span>
                <Tag
                  v-if="getReferenceRowDisabledReason(record)"
                  color="warning"
                  class="!m-0"
                >
                  {{ getTemplateUpgradeVersionText(record) || '未发布' }}
                </Tag>
              </div>
            </template>
            <template v-else-if="isMaskedCell(record, column.dataIndex)">
              *
            </template>
            <template v-else-if="isStatusColumn(column.dataIndex)">
              <Tag
                :color="getStatusColor(record, column.dataIndex)"
                class="!m-0"
              >
                {{ getStatusLabel(record, column.dataIndex) }}
              </Tag>
            </template>
            <template v-else-if="isLongText(column.dataIndex)">
              <span class="qms-product-source-select__summary">
                {{ displayValue(getCellValue(record, column.dataIndex)) }}
              </span>
            </template>
            <template v-else>
              {{ displayValue(getCellValue(record, column.dataIndex)) }}
            </template>
          </template>
        </Table>
      </div>
    </div>
    <template #footer>
      <div class="srm-reference-select__footer">
        <Pagination
          v-model:current="pagination.current"
          v-model:page-size="pagination.pageSize"
          :show-size-changer="true"
          :show-total="getPaginationTotal"
          :total="pagination.total"
          size="small"
          @change="handlePaginationChange"
        />
        <div class="srm-reference-select__footer-actions">
          <Button @click="modalApi.close()">取消</Button>
          <Button type="primary" @click="confirmSelection">确认</Button>
        </div>
      </div>
    </template>
  </Modal>
</template>

<style scoped>
.qms-product-source-select {
  display: flex;
  overflow: hidden;
  width: 100%;
  height: 100%;
  min-height: 0;
  flex: 1 1 auto;
  flex-direction: column;
  gap: 12px;
}

.qms-product-source-select__search {
  flex: 0 0 auto;
  row-gap: 8px;
}

.qms-product-source-select__select {
  width: 170px;
}

.srm-reference-select__status-row {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 12px;
  padding-bottom: 2px;
}

.srm-reference-select__status-filter {
  display: flex;
  min-width: 0;
  align-items: center;
  white-space: nowrap;
}

.srm-reference-select__status-label {
  flex: 0 0 auto;
  margin-right: 8px;
  color: #334155;
  font-weight: 600;
}

.srm-reference-select__status {
  width: 140px;
}

.srm-reference-select__radio-group {
  display: inline-flex;
  flex-wrap: nowrap;
  white-space: nowrap;
}

.qms-product-source-select__table-wrap {
  min-height: 0;
  flex: 1 1 auto;
  overflow: hidden;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
}

.qms-product-source-select__table {
  height: 100%;
  min-height: 0;
  flex: 1 1 auto;
}

.qms-product-source-select__summary {
  display: -webkit-box;
  overflow: hidden;
  -webkit-box-orient: vertical;
  line-height: 18px;
  overflow-wrap: anywhere;
  -webkit-line-clamp: 3;
}

.srm-reference-select__version-cell {
  display: flex;
  align-items: center;
  gap: 6px;
  white-space: nowrap;
}

.qms-product-source-select__table :deep(.srm-reference-select__row--disabled) {
  color: rgb(148 163 184);
  cursor: not-allowed;
  background: rgb(248 250 252);
}

.qms-product-source-select__table
  :deep(.srm-reference-select__row--disabled td) {
  cursor: not-allowed;
}

.qms-product-source-select__table :deep(.ant-spin-nested-loading),
.qms-product-source-select__table :deep(.ant-spin-container) {
  height: 100%;
  min-height: 0;
}

.qms-product-source-select__table :deep(.ant-spin-container) {
  display: flex;
  flex-direction: column;
}

.qms-product-source-select__table :deep(.ant-table) {
  display: flex;
  height: 100%;
  min-height: 0;
  flex: 1 1 auto;
  flex-direction: column;
}

.qms-product-source-select__table :deep(.ant-table-content),
.qms-product-source-select__table :deep(.ant-table-container) {
  display: flex;
  height: 100%;
  min-height: 0;
  flex-direction: column;
}

.qms-product-source-select__table :deep(.ant-table-body) {
  height: 100%;
  max-height: none !important;
  flex: 1 1 auto;
  overflow: auto !important;
}

.srm-reference-select__footer {
  display: flex;
  width: 100%;
  min-width: 0;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.srm-reference-select__footer-actions {
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 8px;
}

.srm-reference-select__footer :deep(.ant-pagination) {
  min-width: 0;
  flex: 1 1 auto;
  margin: 0 !important;
}

:global(.srm-reference-select-modal.size-full) {
  width: 100vw !important;
  max-width: none !important;
  height: 100vh !important;
}

:global(.srm-reference-select-modal:not(.size-full)) {
  height: min(760px, 90vh);
}

:global(.srm-reference-select-modal__body) {
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden !important;
  padding: 12px 18px !important;
}

:global(.srm-reference-select-modal__footer) {
  flex: 0 0 auto;
  padding: 10px 18px !important;
}

:global(
  .srm-reference-select-modal.size-full .srm-reference-select-modal__body
) {
  padding-right: 16px !important;
  padding-left: 16px !important;
}

:global(
  .srm-reference-select-modal.size-full .srm-reference-select-modal__footer
) {
  padding-right: 16px !important;
  padding-left: 16px !important;
}
</style>
