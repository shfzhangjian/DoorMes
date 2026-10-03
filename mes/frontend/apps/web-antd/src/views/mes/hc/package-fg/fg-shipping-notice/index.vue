<script lang="ts" setup>
import type { Dayjs } from 'dayjs';
import type { UploadProps } from 'ant-design-vue';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesHcFinishedPackagingApi } from '#/api/mes/hc/package-fg/finished-packaging';
import type { PickerOption } from '#/components/picker';

import { computed, h, reactive, ref, watch } from 'vue';

import { Page } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import {
  Alert,
  Button,
  Checkbox,
  DatePicker,
  Input,
  InputNumber,
  message,
  Modal,
  Pagination,
  Select,
  Tag,
  Tabs,
  TabPane,
  UploadDragger,
} from 'ant-design-vue';
import dayjs from 'dayjs';

import { useVbenVxeGrid, VxeColumn, VxeTable } from '#/adapter/vxe-table';
import {
  cancelShippingNotice,
  changeShippingNotice,
  deleteShippingNotice,
  getShippingNotice,
  getShippingNoticeBatchCandidatePage,
  getShippingNoticePage,
  getShippingNoticeStockCandidatePage,
  importShippingNoticeExcel,
  issueShippingNotice,
  saveAndLockShippingNotice,
} from '#/api/mes/hc/package-fg/finished-packaging';
import { uploadFile as uploadInfraFile } from '#/api/infra/file';
import { MES_DATETIME_FORMAT, normalizeMesDateTime } from '#/api/mes/hc/shared/date-time';
import { PickerInline, PickerModal, productModelPickerConfig } from '#/components/picker';

import '../shared/cut-round-board.css';

defineOptions({ name: 'MesPackageFgShippingNotice' });

type ShippingNotice = MesHcFinishedPackagingApi.ShippingNotice;
type ShippingNoticeItem = MesHcFinishedPackagingApi.ShippingNoticeItem;
type ShippingNoticePickItem = MesHcFinishedPackagingApi.ShippingNoticePickItem;
type ShippingNoticeAttachment = MesHcFinishedPackagingApi.ShippingNoticeAttachment;
type ShippingBatchCandidate = MesHcFinishedPackagingApi.ShippingBatchCandidate;
type StockLedger = MesHcFinishedPackagingApi.FgStockLedger;
type ShippingNoticeDraftItem = {
  id?: number;
  customerProductBatchNo?: string;
  internalItemCode?: string;
  internalModelCode?: string;
  materialCode?: string;
  materialName?: string;
  modelCode?: string;
  packageSliceNo?: string;
  remark?: string;
  rowKey: number;
  shipQty: number;
};
type ShippingNoticeQuery = {
  customerName: string;
  keyword: string;
  materialCode: string;
  modelCode: string;
  noticeNo: string;
  noticeStatus?: string;
  orderNo: string;
  productType?: string;
  shippingDateEnd: Dayjs | null;
  shippingDateStart: Dayjs | null;
};
type QueryFieldKey = Exclude<keyof ShippingNoticeQuery, 'keyword'>;
type QueryFieldConfig = {
  key: QueryFieldKey;
  label: string;
  options?: { label: string; value: string }[];
  type?: 'date';
};
type QueryTemplate = {
  name: string;
  values: Partial<Record<keyof ShippingNoticeQuery, string>>;
};

const QUERY_TEMPLATE_STORAGE_KEY = 'mes:fg-shipping-notice:query-templates';

const saveLoading = ref(false);
const detailLoading = ref(false);
const candidateLoading = ref(false);
const importVisible = ref(false);
const importLoading = ref(false);
const advancedQueryVisible = ref(false);
const notices = ref<ShippingNotice[]>([]);
const noticeTotal = ref(0);
const pageNo = ref(1);
const pageSize = ref(20);
const queryTemplateName = ref('');
const selectedTemplateName = ref<string>();
const queryTemplates = ref<QueryTemplate[]>(loadQueryTemplates());
let draftRowKeySeed = Date.now();

const createVisible = ref(false);
const createActiveTab = ref('items');
const createDetailMaximized = ref(false);
const editingNoticeId = ref<number>();
const editingNoticeStatus = ref<string>();
const editingNoticeChangeVersion = ref(0);
const detailVisible = ref(false);
const detailActiveTab = ref('items');
const detailItemsMaximized = ref(false);
const stockSelectorVisible = ref(false);
const batchSelectorVisible = ref(false);
const batchSelectorLoading = ref(false);
const productModelPickerOpen = ref(false);
const currentNotice = ref<ShippingNotice>();
const activeBatchDraftRow = ref<ShippingNoticeDraftItem>();
const activeModelDraftRow = ref<ShippingNoticeDraftItem>();
const candidateRows = ref<StockLedger[]>([]);
const candidateTotal = ref(0);
const candidatePageNo = ref(1);
const candidatePageSize = ref(10);
const batchCandidateRows = ref<ShippingBatchCandidate[]>([]);
const batchCandidateTotal = ref(0);
const batchCandidatePageNo = ref(1);
const batchCandidatePageSize = ref(10);
const batchCandidateTableHeight = computed(() =>
  Math.min(420, Math.max(144, 44 + batchCandidateRows.value.length * 34)),
);
const selectorSelectedRowKeys = ref<number[]>([]);
const selectorSelectedStocks = ref<StockLedger[]>([]);
const shippingItems = ref<ShippingNoticeDraftItem[]>([]);

const query = reactive<ShippingNoticeQuery>({
  customerName: '',
  keyword: '',
  materialCode: '',
  modelCode: '',
  noticeNo: '',
  noticeStatus: undefined as string | undefined,
  orderNo: '',
  productType: undefined as string | undefined,
  shippingDateEnd: null as Dayjs | null,
  shippingDateStart: null as Dayjs | null,
});

const candidateQuery = reactive({
  batchNo: '',
  locationCode: '',
  materialCode: '',
  modelCode: '',
  packageNo: '',
  qualityStatus: undefined as string | undefined,
  sliceBatchNo: '',
});

const batchCandidateQuery = reactive({
  batchNo: '',
  keyword: '',
  materialCode: '',
  modelCode: '',
});

const form = reactive({
  customerCode: '',
  customerName: '',
  erpOrderNo: '',
  externalProductCode: '',
  externalProductInfo: '',
  externalProductModel: '',
  materialCode: '',
  materialName: '',
  modelCode: '',
  noticeNo: '',
  noticeQty: 0,
  orderNo: '',
  packingRequirement: '',
  productSize: '',
  productType: 'MASS',
  recorderName: '',
  requiredBatchNo: '',
  requiredExpiryDate: null as Dayjs | null,
  requiredProductionDate: null as Dayjs | null,
  requiredShipQty: 0,
  requiredSliceRange: '',
  remark: '',
  changeReason: '',
  shippingConfirmName: '',
  shippingTime: null as Dayjs | string | null,
});

const statusOptions = [
  { label: '草稿', value: 'DRAFT' },
  { label: '执行', value: 'EXECUTING' },
  { label: '关闭', value: 'CLOSED' },
  { label: '取消', value: 'CANCELLED' },
];

const productTypeOptions = [
  { label: '量产', value: 'MASS' },
  { label: '样品', value: 'SAMPLE' },
];

const qualityOptions = [
  { label: 'OK 合格', value: 'OK' },
  { label: 'NG 不合格', value: 'NG' },
];

const queryFieldConfigs: QueryFieldConfig[] = [
  { key: 'noticeNo', label: '需求单号' },
  { key: 'customerName', label: '发货客户' },
  { key: 'productType', label: '类型', options: productTypeOptions },
  { key: 'modelCode', label: '产品型号' },
  { key: 'materialCode', label: '料号' },
  { key: 'orderNo', label: '订单编号' },
  { key: 'noticeStatus', label: '状态', options: statusOptions },
  { key: 'shippingDateStart', label: '发货日期起', type: 'date' },
  { key: 'shippingDateEnd', label: '发货日期止', type: 'date' },
];

const selectedQty = computed(() => shippingItems.value.length);
const shipQtyTotal = computed(() => shippingItems.value.reduce((sum, item) => sum + Number(item.shipQty || 0), 0));
const lockedQty = computed(() => notices.value.reduce((sum, item) => sum + Number(item.lockedQty || 0), 0));
const activeNoticeCount = computed(() =>
  notices.value.filter((item) => isExecutingStatus(item.noticeStatus)).length,
);
const detailItems = computed<ShippingNoticeItem[]>(() => currentNotice.value?.items || []);
const detailAttachments = computed<ShippingNoticeAttachment[]>(() => currentNotice.value?.attachments || []);
const currentExcelAttachment = computed<ShippingNoticeAttachment | undefined>(
  () => detailAttachments.value.find((item) => item.attachmentType === 'EXCEL_IMPORT') || detailAttachments.value[0],
);
const detailQtyTotal = computed(() => detailItems.value.reduce((sum, item) => sum + Number(item.actualShipQty || item.lockedQty || 0), 0));
const createProcessingRecordFields = computed(() => buildCreateProcessingRecordFields());
const processingRecordFields = computed(() => buildProcessingRecordFields());
const isSampleType = computed(() => form.productType === 'SAMPLE');
const activeQueryTags = computed(() =>
  queryFieldConfigs
    .map((config) => ({
      key: config.key,
      label: config.label,
      value: getQueryFieldText(config.key),
    }))
    .filter((item) => item.value),
);

watch(
  () => form.requiredShipQty,
  (value) => {
    const requiredShipQty = parseShipQty(value);
    if (requiredShipQty <= 0) return;
    syncHiddenShipQty();
  },
);
watch(
  () => form.requiredBatchNo,
  (_value, oldValue) => {
    syncDetailProductBatchNoFromHeader(String(oldValue || '').trim());
  },
);
const queryTemplateOptions = computed(() =>
  queryTemplates.value.map((item) => ({
    label: item.name,
    value: item.name,
  })),
);

const noticeColumns = [
  { field: 'noticeNo', fixed: 'left', showOverflow: 'tooltip', title: '需求单号', width: 180 },
  { field: 'customerName', formatter: ({ row, cellValue }) => row.productType === 'SAMPLE' ? '' : cellValue, fixed: 'left', showOverflow: 'tooltip', title: '发货客户', width: 160 },
  { field: 'productType', slots: { default: 'productType' }, title: '类型', width: 90 },
  { field: 'externalProductModel', formatter: ({ row, cellValue }) => row.productType === 'SAMPLE' ? '' : cellValue, showOverflow: 'tooltip', title: '外部型号', width: 130 },
  { field: 'externalProductCode', formatter: ({ row, cellValue }) => row.productType === 'SAMPLE' ? '' : cellValue, showOverflow: 'tooltip', title: '外部编码', width: 130 },
  { field: 'orderNo', showOverflow: 'tooltip', title: '订单编号', width: 160 },
  { field: 'shippingTime', slots: { default: 'shippingTime' }, title: '发货时间', width: 160 },
  { field: 'noticeStatus', slots: { default: 'noticeStatus' }, title: '状态', width: 100 },
  { field: 'recorderName', showOverflow: 'tooltip', title: '记录人', width: 100 },
  { field: 'recorderTime', slots: { default: 'recorderTime' }, title: '记录时间', width: 160 },
  { field: 'action', fixed: 'right', slots: { default: 'action' }, title: '操作', width: 220 },
];

function isCandidateSelected(row: StockLedger) {
  return selectorSelectedRowKeys.value.includes(row.id);
}

function toggleCandidateStock(row: StockLedger) {
  const rowMap = new Map(selectorSelectedStocks.value.map((item) => [item.id, item]));
  if (isCandidateSelected(row)) {
    selectorSelectedRowKeys.value = selectorSelectedRowKeys.value.filter((id) => id !== row.id);
    rowMap.delete(row.id);
  } else {
    selectorSelectedRowKeys.value = [...selectorSelectedRowKeys.value, row.id];
    rowMap.set(row.id, row);
  }
  selectorSelectedStocks.value = selectorSelectedRowKeys.value
    .map((id) => rowMap.get(id))
    .filter(Boolean) as StockLedger[];
}

function formatDateTime(value?: string) {
  if (!value) return '-';
  return String(value).replace('T', ' ').slice(0, 16);
}

function formatFileSize(value?: number) {
  const size = Number(value || 0);
  if (size <= 0) return '-';
  if (size < 1024) return `${size} B`;
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`;
  return `${(size / 1024 / 1024).toFixed(1)} MB`;
}

function formatSheetPosition(attachment?: ShippingNoticeAttachment) {
  const index = Number(attachment?.sourceSheetIndex || 0);
  const total = Number(attachment?.sourceSheetTotal || 0);
  if (index > 0 && total > 0) return `${index}/${total}`;
  if (index > 0) return `第 ${index} 张`;
  return '-';
}

function resolveUploadedFileUrl(uploaded: unknown) {
  if (typeof uploaded === 'string') return uploaded;
  if (!uploaded || typeof uploaded !== 'object') return '';
  const data = uploaded as Record<string, unknown>;
  const direct = data.url || data.fileUrl || data.path;
  if (typeof direct === 'string') return direct;
  const nested = data.data;
  if (typeof nested === 'string') return nested;
  if (nested && typeof nested === 'object') {
    const nestedData = nested as Record<string, unknown>;
    const nestedUrl = nestedData.url || nestedData.fileUrl || nestedData.path;
    return typeof nestedUrl === 'string' ? nestedUrl : '';
  }
  return '';
}

function openAttachment(url?: string) {
  if (!url) {
    message.warning('附件地址为空');
    return;
  }
  window.open(url, '_blank', 'noopener,noreferrer');
}

function isExecutingStatus(status?: string) {
  return [
    'SUBMITTED',
    'PICKED',
    'SHIP_CONFIRMED',
    'INSPECTED',
    'OQC_INSPECTING',
    'OQC_PASSED',
    'OQC_REJECTED',
    'PACKAGED',
    'LOCKED',
    'OUTBOUND',
  ].includes(status || '');
}

function isDraftStatus(status?: string) {
  return status === 'DRAFT';
}

function isClosedStatus(status?: string) {
  return status === 'CLOSED' || status === 'SHIPPED';
}

function statusText(status?: string) {
  const map: Record<string, string> = {
    CANCELLED: '取消',
    CLOSED: '已发货完成',
    DRAFT: '草稿',
    INSPECTED: '发货检验完成',
    LOCKED: '已下架配货',
    OQC_INSPECTING: 'OQC待检验',
    OQC_PASSED: 'OQC合格待外包装',
    OQC_REJECTED: 'OQC不合格',
    OUTBOUND: '出库中',
    PACKAGED: '发货包装完成',
    PICKED: '已下架配货',
    SHIP_CONFIRMED: '待发货成品检验',
    SHIPPED: '已发货完成',
    SUBMITTED: '待配货',
  };
  return map[status || ''] || status || '-';
}

function statusColor(status?: string) {
  if (isExecutingStatus(status)) return 'processing';
  if (isClosedStatus(status)) return 'green';
  const map: Record<string, string> = {
    CANCELLED: 'default',
    DRAFT: 'default',
    INSPECTED: 'purple',
    LOCKED: 'gold',
    OQC_INSPECTING: 'processing',
    OQC_PASSED: 'green',
    OQC_REJECTED: 'red',
    OUTBOUND: 'processing',
    PACKAGED: 'cyan',
    PICKED: 'blue',
    SHIP_CONFIRMED: 'processing',
    SHIPPED: 'green',
    SUBMITTED: 'gold',
  };
  return map[status || ''] || 'default';
}

function productTypeText(value?: string) {
  if (value === 'RND') return '研发';
  return productTypeOptions.find((item) => item.value === value)?.label || value || '-';
}

function qualityColor(value?: string) {
  if (value === 'OK') return 'green';
  if (value === 'NG') return 'red';
  return 'default';
}

function locationText(row?: StockLedger | ShippingNoticeItem) {
  return [row?.locationCode, row?.locationName].filter(Boolean).join(' / ') || '-';
}

function latestItemByTime(field: keyof ShippingNoticeItem) {
  return [...detailItems.value]
    .filter((item) => item[field])
    .sort((left, right) =>
      dayjs(String(right[field])).valueOf() - dayjs(String(left[field])).valueOf(),
    )[0];
}

function latestPickItemByTime(field: keyof ShippingNoticePickItem) {
  return [...(currentNotice.value?.pickItems || [])]
    .filter((item) => item[field])
    .sort((left, right) =>
      dayjs(String(right[field])).valueOf() - dayjs(String(left[field])).valueOf(),
    )[0];
}

function hasAnyItemValue(field: keyof ShippingNoticeItem) {
  return detailItems.value.some((item) => item[field]);
}

function hasAnyPickItemValue(field: keyof ShippingNoticePickItem) {
  return (currentNotice.value?.pickItems || []).some((item) => item[field]);
}

function pickNoticeText(...values: Array<unknown>) {
  const value = values.find((item) => String(item ?? '').trim());
  return value === undefined || value === null || String(value).trim() === ''
    ? '-'
    : String(value);
}

function aggregateInspectionStatus() {
  const sourceItems = (currentNotice.value?.pickItems || []).length ? currentNotice.value?.pickItems || [] : detailItems.value;
  const results = sourceItems
    .map((item) => String(item.shippingInspectionResult || '').trim())
    .filter(Boolean);
  if (!results.length) return '待检验';
  if (results.some((item) => item === 'NG')) return '不合格';
  return results.length === sourceItems.length ? '合格' : '检验中';
}

function aggregatePackageStatus() {
  if (['PACKAGED', 'OUTBOUND', 'SHIPPED', 'CLOSED'].includes(currentNotice.value?.noticeStatus || '')) {
    return '已确认';
  }
  return hasAnyPickItemValue('shippingPackageTime') || hasAnyItemValue('shippingPackageTime') ? '确认中' : '待确认';
}

function aggregateShippingCompleteStatus() {
  if (isClosedStatus(currentNotice.value?.noticeStatus)) return '已完成';
  return hasAnyPickItemValue('shippedTime') || hasAnyItemValue('shippedTime') ? '部分完成' : '未完成';
}

function buildProcessingRecordFields() {
  const notice = currentNotice.value as (ShippingNotice & Record<string, any>) | undefined;
  const inspectionItem = latestPickItemByTime('shippingInspectionTime') || latestItemByTime('shippingInspectionTime');
  const packageItem = latestPickItemByTime('shippingPackageTime') || latestItemByTime('shippingPackageTime');
  const shippedItem = latestPickItemByTime('shippedTime') || latestItemByTime('shippedTime');
  const pickItem = latestPickItemByTime('lockTime');
  return [
    {
      label: '创建人',
      value: pickNoticeText(notice?.creatorName, notice?.recorderName),
    },
    {
      label: '创建时间',
      value: formatDateTime(pickNoticeText(notice?.createTime, notice?.recorderTime)),
    },
    {
      label: '检验人',
      value: pickNoticeText(inspectionItem?.shippingInspectorName),
    },
    {
      label: '检验时间',
      value: formatDateTime(inspectionItem?.shippingInspectionTime),
    },
    {
      label: '检验状态',
      value: aggregateInspectionStatus(),
    },
    {
      label: '配货包装人',
      value: pickNoticeText(packageItem?.shippingPackageName, pickItem?.lockName),
    },
    {
      label: '配货包装确认时间',
      value: formatDateTime(pickNoticeText(packageItem?.shippingPackageTime, pickItem?.lockTime)),
    },
    {
      label: '配货包装确认状态',
      value: aggregatePackageStatus(),
    },
    {
      label: '最终检验人',
      value: pickNoticeText(inspectionItem?.shippingInspectorName),
    },
    {
      label: '最终检验状态',
      value: aggregateInspectionStatus(),
    },
    {
      label: '最终确认检验人',
      value: pickNoticeText(inspectionItem?.shippingInspectorName),
    },
    {
      label: '最终检验时间',
      value: formatDateTime(inspectionItem?.shippingInspectionTime),
    },
    {
      label: '发货完成状态',
      value: aggregateShippingCompleteStatus(),
    },
    {
      label: '发货完成时间',
      value: formatDateTime(shippedItem?.shippedTime),
    },
    {
      label: '发货完成人',
      value: pickNoticeText(shippedItem?.shippedName),
    },
    {
      label: '取消人',
      value: pickNoticeText(notice?.cancelName),
    },
    {
      label: '取消时间',
      value: formatDateTime(notice?.cancelTime),
    },
    {
      label: '取消原因',
      value: pickNoticeText(notice?.cancelReason),
    },
  ];
}

function buildCreateProcessingRecordFields() {
  return [
    { label: '创建人', value: pickNoticeText(form.recorderName) },
    { label: '创建时间', value: '-' },
    { label: '检验人', value: '-' },
    { label: '检验时间', value: '-' },
    { label: '检验状态', value: '待检验' },
    { label: '配货包装人', value: '-' },
    { label: '配货包装确认时间', value: '-' },
    { label: '配货包装确认状态', value: '待确认' },
    { label: '最终检验人', value: '-' },
    { label: '最终检验状态', value: '待检验' },
    { label: '最终确认检验人', value: '-' },
    { label: '最终检验时间', value: '-' },
    { label: '发货完成状态', value: '未完成' },
    { label: '发货完成时间', value: '-' },
    { label: '发货完成人', value: '-' },
    { label: '取消人', value: '-' },
    { label: '取消时间', value: '-' },
    { label: '取消原因', value: '-' },
  ];
}

function loadQueryTemplates() {
  if (typeof window === 'undefined') return [];
  try {
    const rawTemplates = window.localStorage.getItem(QUERY_TEMPLATE_STORAGE_KEY);
    const parsedTemplates = rawTemplates ? JSON.parse(rawTemplates) : [];
    if (!Array.isArray(parsedTemplates)) return [];
    return parsedTemplates
      .filter((item) => item?.name && item?.values)
      .map((item) => ({
        name: String(item.name),
        values: item.values,
      })) as QueryTemplate[];
  } catch {
    return [];
  }
}

function persistQueryTemplates() {
  if (typeof window === 'undefined') return;
  window.localStorage.setItem(QUERY_TEMPLATE_STORAGE_KEY, JSON.stringify(queryTemplates.value));
}

function getQueryFieldConfig(key: QueryFieldKey) {
  return queryFieldConfigs.find((item) => item.key === key);
}

function getQueryFieldText(key: QueryFieldKey) {
  const value = query[key];
  if (!value) return '';
  const config = getQueryFieldConfig(key);
  if (config?.type === 'date') return (value as Dayjs).format('YYYY-MM-DD');
  const text = String(value).trim();
  if (!text) return '';
  return config?.options?.find((item) => item.value === text)?.label || text;
}

function getQueryFieldRawValue(key: QueryFieldKey) {
  const value = query[key];
  if (!value) return '';
  if (getQueryFieldConfig(key)?.type === 'date') return (value as Dayjs).format('YYYY-MM-DD');
  return String(value).trim();
}

function setQueryFieldValue(key: QueryFieldKey, value?: string) {
  switch (key) {
    case 'customerName': {
      query.customerName = value || '';
      break;
    }
    case 'materialCode': {
      query.materialCode = value || '';
      break;
    }
    case 'modelCode': {
      query.modelCode = value || '';
      break;
    }
    case 'noticeNo': {
      query.noticeNo = value || '';
      break;
    }
    case 'noticeStatus': {
      query.noticeStatus = value || undefined;
      break;
    }
    case 'orderNo': {
      query.orderNo = value || '';
      break;
    }
    case 'productType': {
      query.productType = value || undefined;
      break;
    }
    case 'shippingDateEnd': {
      query.shippingDateEnd = value ? dayjs(value) : null;
      break;
    }
    case 'shippingDateStart': {
      query.shippingDateStart = value ? dayjs(value) : null;
      break;
    }
  }
}

function clearAdvancedQueryValues() {
  queryFieldConfigs.forEach((config) => setQueryFieldValue(config.key));
}

function clearAdvancedQuery() {
  clearAdvancedQueryValues();
  selectedTemplateName.value = undefined;
  queryTemplateName.value = '';
}

function snapshotAdvancedQuery() {
  const values: QueryTemplate['values'] = {};
  if (query.keyword.trim()) values.keyword = query.keyword.trim();
  queryFieldConfigs.forEach((config) => {
    const value = getQueryFieldRawValue(config.key);
    if (value) values[config.key] = value;
  });
  return values;
}

function openAdvancedQuery() {
  advancedQueryVisible.value = true;
}

function applyAdvancedQuery() {
  advancedQueryVisible.value = false;
  searchNotices();
}

function removeQueryCondition(key: QueryFieldKey) {
  setQueryFieldValue(key);
  selectedTemplateName.value = undefined;
  searchNotices();
}

function saveQueryTemplate() {
  const name = queryTemplateName.value.trim();
  if (!name) {
    message.warning('请填写查询模板名称');
    return;
  }
  const values = snapshotAdvancedQuery();
  if (Object.keys(values).length === 0) {
    message.warning('请先填写查询条件');
    return;
  }
  const nextTemplates = queryTemplates.value.filter((item) => item.name !== name);
  nextTemplates.unshift({ name, values });
  queryTemplates.value = nextTemplates.slice(0, 20);
  selectedTemplateName.value = name;
  persistQueryTemplates();
  message.success('查询模板已保存');
}

function loadQueryTemplate(name?: string) {
  if (!name) return;
  const template = queryTemplates.value.find((item) => item.name === name);
  if (!template) return;
  query.keyword = '';
  clearAdvancedQueryValues();
  Object.entries(template.values).forEach(([key, value]) => {
    if (key === 'keyword') {
      query.keyword = value || '';
      return;
    }
    setQueryFieldValue(key as QueryFieldKey, value);
  });
  queryTemplateName.value = template.name;
  searchNotices();
}

function handleLoadQueryTemplate(value?: string | number) {
  loadQueryTemplate(value ? String(value) : undefined);
}

function buildQuery(page?: { currentPage?: number; pageSize?: number }) {
  const params: Record<string, any> = {
    pageNo: page?.currentPage || pageNo.value,
    pageSize: page?.pageSize || pageSize.value,
  };
  if (query.keyword.trim()) params.keyword = query.keyword.trim();
  if (query.noticeNo.trim()) params.noticeNo = query.noticeNo.trim();
  if (query.customerName.trim()) params.customerName = query.customerName.trim();
  if (query.productType) params.productType = query.productType;
  if (query.materialCode.trim()) params.materialCode = query.materialCode.trim();
  if (query.modelCode.trim()) params.modelCode = query.modelCode.trim();
  if (query.orderNo.trim()) params.orderNo = query.orderNo.trim();
  if (query.noticeStatus) params.noticeStatus = query.noticeStatus;
  if (query.shippingDateStart) params.shippingDateStart = query.shippingDateStart.format('YYYY-MM-DD');
  if (query.shippingDateEnd) params.shippingDateEnd = query.shippingDateEnd.format('YYYY-MM-DD');
  return params;
}

function buildCandidateQuery() {
  const params: Record<string, any> = {
    pageNo: candidatePageNo.value,
    pageSize: candidatePageSize.value,
  };
  if (candidateQuery.locationCode.trim()) params.locationCode = candidateQuery.locationCode.trim();
  if (candidateQuery.packageNo.trim()) params.packageNo = candidateQuery.packageNo.trim();
  if (candidateQuery.sliceBatchNo.trim()) params.sliceBatchNo = candidateQuery.sliceBatchNo.trim();
  if (candidateQuery.batchNo.trim()) params.batchNo = candidateQuery.batchNo.trim();
  if (candidateQuery.materialCode.trim()) params.materialCode = candidateQuery.materialCode.trim();
  if (candidateQuery.modelCode.trim()) params.modelCode = candidateQuery.modelCode.trim();
  if (candidateQuery.qualityStatus) params.qualityStatus = candidateQuery.qualityStatus;
  return params;
}

function buildBatchCandidateQuery() {
  const params: Record<string, any> = {
    pageNo: batchCandidatePageNo.value,
    pageSize: batchCandidatePageSize.value,
  };
  if (batchCandidateQuery.keyword.trim()) params.keyword = batchCandidateQuery.keyword.trim();
  if (batchCandidateQuery.batchNo.trim()) params.batchNo = batchCandidateQuery.batchNo.trim();
  if (batchCandidateQuery.materialCode.trim()) params.materialCode = batchCandidateQuery.materialCode.trim();
  if (batchCandidateQuery.modelCode.trim()) params.modelCode = batchCandidateQuery.modelCode.trim();
  return params;
}

async function queryNoticePage(page?: { currentPage?: number; pageSize?: number }) {
  pageNo.value = page?.currentPage || pageNo.value;
  pageSize.value = page?.pageSize || pageSize.value;
  const result = await getShippingNoticePage(buildQuery(page));
  notices.value = result.list || [];
  noticeTotal.value = Number(result.total || 0);
  return result;
}

async function fetchNotices() {
  await noticeGridApi.query();
}

async function fetchCandidates(reset = false) {
  if (reset) candidatePageNo.value = 1;
  candidateLoading.value = true;
  try {
    const result = await getShippingNoticeStockCandidatePage(buildCandidateQuery());
    candidateRows.value = result.list || [];
    candidateTotal.value = Number(result.total || 0);
  } finally {
    candidateLoading.value = false;
  }
}

async function fetchBatchCandidates(reset = false) {
  if (reset) batchCandidatePageNo.value = 1;
  batchSelectorLoading.value = true;
  try {
    const result = await getShippingNoticeBatchCandidatePage(buildBatchCandidateQuery());
    batchCandidateRows.value = result.list || [];
    batchCandidateTotal.value = Number(result.total || 0);
  } finally {
    batchSelectorLoading.value = false;
  }
}

function searchNotices() {
  pageNo.value = 1;
  fetchNotices();
}

function openImportExcel() {
  importVisible.value = true;
}

const beforeExcelImportUpload: UploadProps['beforeUpload'] = async (file) => {
  const rawFile = file as File;
  if (!/\.(xlsx|xls)$/i.test(rawFile.name || '')) {
    message.warning('请上传 .xlsx 或 .xls 文件');
    return false;
  }
  importLoading.value = true;
  try {
    const uploaded = await uploadInfraFile({
      directory: 'mes/fg-shipping-notice',
      file: rawFile,
    });
    const fileUrl = resolveUploadedFileUrl(uploaded);
    if (!fileUrl) {
      throw new Error('原始Excel上传成功，但未返回附件地址');
    }
    const result = await importShippingNoticeExcel({
      file: rawFile,
      fileUrl,
    });
    message.success(`已导入 ${result.importCount || 0} 张发货需求单`);
    importVisible.value = false;
    await fetchNotices();
    if (result.notices?.[0]) {
      await openDetail(result.notices[0], { activeTab: 'attachments' });
    }
  } catch (error) {
    message.error(error instanceof Error && error.message ? error.message : '导入失败，请检查Excel结构后重试');
  } finally {
    importLoading.value = false;
  }
  return false;
};

function handleCandidatePageChange() {
  fetchCandidates(false);
}

function handleBatchCandidatePageChange() {
  fetchBatchCandidates(false);
}

function resetForm() {
  editingNoticeId.value = undefined;
  editingNoticeStatus.value = undefined;
  editingNoticeChangeVersion.value = 0;
  Object.assign(form, {
    customerCode: '',
    customerName: '',
    erpOrderNo: '',
    externalProductCode: '',
    externalProductInfo: '',
    externalProductModel: '',
    materialCode: '',
    materialName: '',
    modelCode: '',
    noticeNo: '',
    noticeQty: 0,
    orderNo: '',
    packingRequirement: '',
    productSize: '',
    productType: 'MASS',
    recorderName: '',
    requiredBatchNo: '',
    requiredExpiryDate: null,
    requiredProductionDate: null,
    requiredShipQty: 0,
    requiredSliceRange: '',
    remark: '',
    changeReason: '',
    shippingConfirmName: '',
    shippingTime: null,
  });
  shippingItems.value = [];
  selectorSelectedRowKeys.value = [];
  selectorSelectedStocks.value = [];
  candidatePageNo.value = 1;
  batchCandidatePageNo.value = 1;
  Object.assign(candidateQuery, {
    batchNo: '',
    locationCode: '',
    materialCode: '',
    modelCode: '',
    packageNo: '',
    qualityStatus: undefined,
    sliceBatchNo: '',
  });
  Object.assign(batchCandidateQuery, {
    batchNo: '',
    keyword: '',
    materialCode: '',
    modelCode: '',
  });
}

async function openCreate() {
  resetForm();
  addShippingItem();
  createActiveTab.value = 'items';
  createDetailMaximized.value = false;
  createVisible.value = true;
}

function toShippingDraftItemFromNoticeItem(item: ShippingNoticeItem, index: number): ShippingNoticeDraftItem {
  return {
    id: item.id,
    customerProductBatchNo: item.customerProductBatchNo || item.batchNo || '',
    internalItemCode: item.internalItemCode || item.customerSliceBatchNo || '',
    internalModelCode: item.internalModelCode || item.customerModelCode || item.modelCode || '',
    materialCode: item.materialCode || '',
    materialName: item.materialName || '',
    modelCode: item.modelCode || item.internalModelCode || '',
    packageSliceNo: item.packageSliceNo || item.sliceBatchNo || '',
    remark: item.remark || '',
    rowKey: item.id || Date.now() + index,
    shipQty: Math.max(1, Number(item.lockedQty || item.actualShipQty || 1)),
  };
}

function fillFormFromNotice(notice: ShippingNotice) {
  Object.assign(form, {
    customerCode: notice.customerCode || '',
    customerName: notice.customerName || '',
    erpOrderNo: notice.erpOrderNo || '',
    externalProductCode: notice.externalProductCode || '',
    externalProductInfo: notice.externalProductInfo || '',
    externalProductModel: notice.externalProductModel || '',
    materialCode: notice.materialCode || '',
    materialName: notice.materialName || '',
    modelCode: notice.modelCode || '',
    noticeNo: notice.noticeNo || '',
    noticeQty: Number(notice.noticeQty || 0),
    orderNo: notice.orderNo || '',
    packingRequirement: notice.packingRequirement || '',
    productSize: notice.productSize || '',
    productType: notice.productType || 'MASS',
    recorderName: notice.recorderName || '',
    requiredBatchNo: notice.requiredBatchNo || '',
    requiredExpiryDate: notice.requiredExpiryDate ? dayjs(notice.requiredExpiryDate) : null,
    requiredProductionDate: notice.requiredProductionDate ? dayjs(notice.requiredProductionDate) : null,
    requiredShipQty: Number(notice.requiredShipQty || notice.noticeQty || 0),
    requiredSliceRange: notice.requiredSliceRange || '',
    remark: notice.remark || '',
    changeReason: '',
    shippingConfirmName: notice.shippingConfirmName || '',
    shippingTime: normalizeMesDateTime(notice.shippingTime) || null,
  });
  shippingItems.value = (notice.items || []).map(toShippingDraftItemFromNoticeItem);
  syncDetailProductBatchNoFromHeader();
  if (!shippingItems.value.length && notice.productType !== 'SAMPLE') {
    addShippingItem();
  }
}

async function openEdit(row: ShippingNotice) {
  const notice = await getShippingNotice(row.id);
  if (notice.noticeStatus === 'CLOSED') {
    message.warning('发货需求单已完成出库关闭，不能修改');
    return;
  }
  resetForm();
  editingNoticeId.value = notice.id;
  editingNoticeStatus.value = notice.noticeStatus;
  editingNoticeChangeVersion.value = Number(notice.changeVersion || 0);
  fillFormFromNotice(notice);
  createActiveTab.value = 'items';
  createDetailMaximized.value = false;
  detailVisible.value = false;
  createVisible.value = true;
}

function toShippingDraftItem(row: StockLedger, current?: ShippingNoticeDraftItem): ShippingNoticeDraftItem {
  const currentShipQty = Number(current?.shipQty || 0);
  return {
    customerProductBatchNo: row.batchNo,
    internalItemCode: row.sliceBatchNo,
    internalModelCode: row.modelCode,
    materialCode: row.materialCode,
    materialName: row.materialName,
    modelCode: row.modelCode,
    packageSliceNo: row.sliceBatchNo,
    remark: current?.remark || '',
    rowKey: row.id,
    shipQty: Math.max(1, Math.floor(currentShipQty || 1)),
  };
}

function syncNoticeQty() {
  form.noticeQty = shipQtyTotal.value;
  if (!Number(form.requiredShipQty || 0)) form.requiredShipQty = shipQtyTotal.value;
}

function syncHiddenShipQty() {
  if (shippingItems.value.length === 0) {
    form.noticeQty = 0;
    return;
  }
  const requiredShipQty = parseShipQty(form.requiredShipQty);
  if (requiredShipQty < shippingItems.value.length) {
    shippingItems.value.forEach((item) => {
      item.shipQty = 1;
    });
    syncNoticeQty();
    return;
  }
  const targetQty = requiredShipQty > 0 ? requiredShipQty : shippingItems.value.length;
  const baseQty = Math.floor(targetQty / shippingItems.value.length);
  let remainQty = targetQty % shippingItems.value.length;
  shippingItems.value.forEach((item) => {
    item.shipQty = baseQty + (remainQty > 0 ? 1 : 0);
    remainQty = Math.max(0, remainQty - 1);
  });
  syncNoticeQty();
}

function createDraftRowKey() {
  draftRowKeySeed += 1;
  return draftRowKeySeed;
}

function getHeaderProductBatchNo() {
  return isSampleType.value ? '' : form.requiredBatchNo.trim();
}

function syncDetailProductBatchNoFromHeader(previousProductBatchNo = '') {
  if (isSampleType.value) return;
  const headerProductBatchNo = getHeaderProductBatchNo();
  shippingItems.value.forEach((item) => {
    const currentProductBatchNo = String(item.customerProductBatchNo || '').trim();
    if (!currentProductBatchNo || currentProductBatchNo === previousProductBatchNo) {
      item.customerProductBatchNo = headerProductBatchNo;
    }
  });
}

function addShippingItem() {
  const requiredShipQty = parseShipQty(form.requiredShipQty);
  shippingItems.value.push({
    customerProductBatchNo: getHeaderProductBatchNo() || undefined,
    rowKey: createDraftRowKey(),
    shipQty: shippingItems.value.length === 0 && requiredShipQty > 0 ? requiredShipQty : 1,
  });
  syncHiddenShipQty();
}

function copyShippingItem(row: ShippingNoticeDraftItem) {
  const defaultCopyCount = Math.max(1, parseShipQty(form.requiredShipQty) - shippingItems.value.length);
  let copyCount = defaultCopyCount;
  Modal.confirm({
    cancelText: '取消',
    content: h('div', { class: 'shipping-copy-row-modal' }, [
      h('p', '要把此行复制多少行？'),
      h(InputNumber, {
        defaultValue: defaultCopyCount,
        min: 1,
        precision: 0,
        style: { width: '100%' },
        onChange: (value) => {
          copyCount = Math.max(1, parseShipQty(value));
        },
      }),
    ]),
    okText: '确认复制',
    title: '复制发货明细',
    onOk: () => {
      const count = Math.max(1, parseShipQty(copyCount));
      const rowIndex = shippingItems.value.findIndex((item) => item.rowKey === row.rowKey);
      const copiedRows = Array.from({ length: count }, () => ({
        ...row,
        rowKey: createDraftRowKey(),
      }));
      if (rowIndex >= 0) {
        shippingItems.value.splice(rowIndex + 1, 0, ...copiedRows);
      } else {
        shippingItems.value.push(...copiedRows);
      }
      syncHiddenShipQty();
    },
  });
}

function hasDraftItemText(item: ShippingNoticeDraftItem) {
  return [
    isSampleType.value ? undefined : item.customerProductBatchNo,
    item.internalItemCode,
    item.internalModelCode,
    item.materialCode,
    item.materialName,
    item.modelCode,
    isSampleType.value ? undefined : item.packageSliceNo,
    item.remark,
  ].some((value) => String(value || '').trim());
}

function handleProductTypeChange(value: string) {
  form.productType = value;
  if (value === 'SAMPLE') {
    if (shippingItems.value.every((item) => !hasDraftItemText(item))) {
      shippingItems.value = [];
      form.noticeQty = Number(form.requiredShipQty || 0);
    }
    return;
  }
  if (shippingItems.value.length === 0) {
    addShippingItem();
  }
  syncDetailProductBatchNoFromHeader();
}

function handleDraftModelInput(row: ShippingNoticeDraftItem, value: string) {
  row.internalModelCode = value;
  row.modelCode = value;
}

function handleDraftModelSelected(row: ShippingNoticeDraftItem, option: PickerOption) {
  row.internalModelCode = option.code;
  row.modelCode = option.code;
}

function openDraftModelPicker(row: ShippingNoticeDraftItem) {
  activeModelDraftRow.value = row;
  productModelPickerOpen.value = true;
}

function handleDraftModelModalPick(option: PickerOption) {
  if (activeModelDraftRow.value) {
    handleDraftModelSelected(activeModelDraftRow.value, option);
  }
  productModelPickerOpen.value = false;
  activeModelDraftRow.value = undefined;
}

function handleDraftInternalItemInput(row: ShippingNoticeDraftItem, value: string) {
  row.internalItemCode = value;
}

async function openBatchCandidateSelector(row: ShippingNoticeDraftItem) {
  activeBatchDraftRow.value = row;
  Object.assign(batchCandidateQuery, {
    batchNo: row.internalItemCode || row.customerProductBatchNo || '',
    keyword: '',
    materialCode: row.materialCode || form.materialCode || '',
    modelCode: row.internalModelCode || form.modelCode || form.externalProductModel || '',
  });
  batchSelectorVisible.value = true;
  await fetchBatchCandidates(true);
}

function selectBatchCandidate(row: ShippingBatchCandidate) {
  const target = activeBatchDraftRow.value;
  if (!target) return;
  const batchNo = row.batchNo || '';
  const currentShipQty = parseShipQty(target.shipQty);
  target.internalItemCode = batchNo;
  target.customerProductBatchNo = target.customerProductBatchNo || getHeaderProductBatchNo();
  target.internalModelCode = row.modelCode || target.internalModelCode;
  target.modelCode = row.modelCode || target.modelCode;
  target.materialCode = row.materialCode || target.materialCode;
  target.materialName = row.materialName || target.materialName;
  target.shipQty = currentShipQty > 0 ? currentShipQty : Math.max(1, Number(row.availableQty || form.requiredShipQty || 1));
  syncHiddenShipQty();
  if (!form.materialCode && row.materialCode) form.materialCode = row.materialCode;
  if (!form.materialName && row.materialName) form.materialName = row.materialName;
  if (!form.modelCode && row.modelCode) form.modelCode = row.modelCode;
  batchSelectorVisible.value = false;
  activeBatchDraftRow.value = undefined;
}

function confirmAddShippingItems() {
  if (selectorSelectedRowKeys.value.length === 0) {
    message.warning('请选择发货库存明细');
    return;
  }
  const currentMap = new Map(shippingItems.value.map((item) => [item.rowKey, item]));
  const selectorMap = new Map(selectorSelectedStocks.value.map((item) => [item.id, item]));
  shippingItems.value = selectorSelectedRowKeys.value
    .map((id) => {
      const row = selectorMap.get(id);
      return row ? toShippingDraftItem(row, currentMap.get(id)) : currentMap.get(id);
    })
    .filter(Boolean) as ShippingNoticeDraftItem[];
  syncHiddenShipQty();
  stockSelectorVisible.value = false;
}

function removeShippingItem(row: ShippingNoticeDraftItem) {
  shippingItems.value = shippingItems.value.filter((item) => item.rowKey !== row.rowKey);
  syncHiddenShipQty();
}

function parseShipQty(value: number | string | null | undefined) {
  const parsedQty = Number(value);
  return Math.max(0, Math.floor(Number.isFinite(parsedQty) ? parsedQty : 0));
}

function getSubmittableShippingItems() {
  return isSampleType.value ? shippingItems.value.filter((item) => !!item.id || hasDraftItemText(item)) : shippingItems.value;
}

function validateShipQtyTotal(items: ShippingNoticeDraftItem[], requiredShipQty: number) {
  const detailShipQtyTotal = items.reduce((sum, item) => sum + parseShipQty(item.shipQty), 0);
  if (detailShipQtyTotal !== requiredShipQty) {
    message.warning(`发货明细数量合计 ${detailShipQtyTotal} 与发货数量 ${requiredShipQty} 不一致，请调整发货数量或明细行数`);
    return false;
  }
  return true;
}

function validateBeforeSave() {
  if (!isSampleType.value && !form.customerName.trim()) {
    message.warning(isSampleType.value ? '请填写客户名称' : '请填写发货客户');
    return false;
  }
  if (!form.productType) {
    message.warning('请选择类型');
    return false;
  }
  if (isSampleType.value) {
    if (Number(form.requiredShipQty || 0) <= 0) {
      message.warning('请填写样品数量');
      return false;
    }
    const requiredShipQty = parseShipQty(form.requiredShipQty);
    syncHiddenShipQty();
    const submitItems = getSubmittableShippingItems();
    if (submitItems.length > 0 && !validateShipQtyTotal(submitItems, requiredShipQty)) return false;
    form.noticeQty = requiredShipQty;
    return true;
  }
  const requiredShipQty = parseShipQty(form.requiredShipQty);
  if (requiredShipQty <= 0) {
    message.warning('请填写外部要求发货数量');
    return false;
  }
  if (!getHeaderProductBatchNo()) {
    message.warning('请填写生产批号');
    return false;
  }
  syncDetailProductBatchNoFromHeader();
  syncHiddenShipQty();
  const submitItems = getSubmittableShippingItems();
  if (submitItems.length === 0) {
    message.warning('请增加客户批号明细');
    return false;
  }
  for (const item of submitItems) {
    if (!String(item.internalModelCode || '').trim()) {
      message.warning('请填写客户批号明细的产品型号');
      return false;
    }
    if (!String(item.internalItemCode || '').trim()) {
      message.warning('请填写客户批号明细的分段批号');
      return false;
    }
    if (!String(item.customerProductBatchNo || '').trim()) {
      message.warning('请填写客户批号明细的产品批号');
      return false;
    }
    const shipQty = Number(item.shipQty || 0);
    if (shipQty <= 0) {
      message.warning(`请填写 ${item.internalItemCode || '明细'} 的发货数量`);
      return false;
    }
  }
  if (!validateShipQtyTotal(submitItems, requiredShipQty)) return false;
  syncNoticeQty();
  return true;
}

async function submitSave() {
  if (!validateBeforeSave()) return;
  const changingExecution = !!editingNoticeId.value && !isDraftStatus(editingNoticeStatus.value);
  if (changingExecution && !form.changeReason.trim()) {
    message.warning('请填写变更原因');
    return;
  }
  syncDetailProductBatchNoFromHeader();
  syncHiddenShipQty();
  const submitItems = getSubmittableShippingItems();
  const noticeProductBatchNo = getHeaderProductBatchNo();
  saveLoading.value = true;
  try {
    const payload = {
      customerCode: form.customerCode.trim() || undefined,
      customerName: form.customerName.trim(),
      erpOrderNo: form.erpOrderNo.trim() || undefined,
      externalProductCode: form.externalProductCode.trim() || undefined,
      externalProductInfo: form.externalProductInfo.trim() || undefined,
      externalProductModel: form.externalProductModel.trim() || undefined,
      id: editingNoticeId.value,
      materialCode: form.materialCode.trim() || undefined,
      materialName: form.materialName.trim() || undefined,
      modelCode: form.modelCode.trim() || undefined,
      noticeNo: form.noticeNo.trim() || undefined,
      noticeQty: Number(form.requiredShipQty || form.noticeQty || 0),
      orderNo: form.orderNo.trim() || undefined,
      packingRequirement: form.packingRequirement.trim() || undefined,
      productSize: form.productSize.trim() || undefined,
      productType: form.productType,
      recorderName: form.recorderName.trim() || undefined,
      requiredBatchNo: form.requiredBatchNo.trim() || undefined,
      requiredExpiryDate: form.requiredExpiryDate ? form.requiredExpiryDate.format('YYYY-MM-DD') : undefined,
      requiredProductionDate: form.requiredProductionDate ? form.requiredProductionDate.format('YYYY-MM-DD') : undefined,
      requiredShipQty: Number(form.requiredShipQty || 0),
      requiredSliceRange: form.requiredSliceRange.trim() || undefined,
      remark: form.remark.trim() || undefined,
      shippingConfirmName: form.shippingConfirmName.trim() || undefined,
      shippingTime: normalizeMesDateTime(form.shippingTime),
      items: submitItems.map((item) => {
        const productBatchNo = item.customerProductBatchNo?.trim() || noticeProductBatchNo || undefined;
        return {
          id: item.id,
          batchNo: productBatchNo,
          customerProductBatchNo: productBatchNo,
          internalItemCode: item.internalItemCode?.trim() || undefined,
          internalModelCode: item.internalModelCode?.trim() || undefined,
          materialCode: item.materialCode?.trim() || undefined,
          materialName: item.materialName?.trim() || undefined,
          modelCode: item.modelCode?.trim() || item.internalModelCode?.trim() || undefined,
          packageSliceNo: item.packageSliceNo?.trim() || undefined,
          remark: item.remark?.trim() || undefined,
          shipQty: Number(item.shipQty || 0),
        };
      }),
    };
    if (changingExecution) {
      await changeShippingNotice({
        ...payload,
        changeReason: form.changeReason.trim(),
        expectedChangeVersion: editingNoticeChangeVersion.value,
      });
    } else {
      await saveAndLockShippingNotice(payload);
    }
    message.success(changingExecution ? '发货需求单已变更并同步后续流程' : '发货需求单草稿已保存');
    createVisible.value = false;
    await fetchNotices();
  } finally {
    saveLoading.value = false;
  }
}

async function openDetail(row: ShippingNotice, options?: { activeTab?: string }) {
  detailItemsMaximized.value = false;
  detailActiveTab.value = options?.activeTab || 'items';
  currentNotice.value = row;
  detailVisible.value = true;
  detailLoading.value = true;
  try {
    currentNotice.value = await getShippingNotice(row.id);
  } finally {
    detailLoading.value = false;
  }
}

async function switchExcelSheetNotice(targetNoticeId?: number) {
  if (!targetNoticeId) return;
  await openDetail({ id: targetNoticeId } as ShippingNotice, { activeTab: 'attachments' });
}

function canDeleteNotice(row?: ShippingNotice) {
  return isDraftStatus(row?.noticeStatus);
}

function canIssueNotice(row?: ShippingNotice) {
  return isDraftStatus(row?.noticeStatus);
}

function canEditNotice(row?: ShippingNotice) {
  return row?.noticeStatus !== 'CLOSED';
}

function canCancelNotice(row?: ShippingNotice) {
  return isExecutingStatus(row?.noticeStatus);
}

function deleteNotice(row: ShippingNotice) {
  Modal.confirm({
    title: '删除发货需求单',
    content: `确认物理删除 ${row.noticeNo || ''} 吗？删除后不可恢复。`,
    okButtonProps: { danger: true },
    okText: '删除',
    onOk: async () => {
      await deleteShippingNotice(row.id);
      message.success('发货需求单已删除');
      if (currentNotice.value?.id === row.id) {
        detailVisible.value = false;
        currentNotice.value = undefined;
      }
      await fetchNotices();
    },
  });
}

function issueNotice(row: ShippingNotice) {
  Modal.confirm({
    title: '下达执行',
    content: `确认下达执行 ${row.noticeNo || ''} 吗？下达后将不能再修改。`,
    okText: '下达执行',
    onOk: async () => {
      const result = await issueShippingNotice({ id: row.id });
      message.success('发货需求单已下达执行');
      if (currentNotice.value?.id === row.id) currentNotice.value = result;
      await fetchNotices();
    },
  });
}

function cancelNotice(row: ShippingNotice) {
  let reason = '';
  let physicalReturned = false;
  Modal.confirm({
    title: '取消发货需求单',
    content: h(
      'div',
      { style: 'display: flex; flex-direction: column; gap: 12px;' },
      [
        h(
          'div',
          { style: 'color: #d46b08; line-height: 1.6;' },
          '取消后，本单全部已配货产品将拆除原包装并退回待包装：FQC 合格退回合格区，FQC 不合格退回不合格区，未检产品沿用原质量状态。',
        ),
        h(Input.TextArea, {
          placeholder: '请填写取消原因',
          rows: 4,
          value: reason,
          'onUpdate:value': (value: string) => {
            reason = value;
          },
        }),
        h(
          Checkbox,
          {
            'onUpdate:checked': (value: boolean) => {
              physicalReturned = value;
            },
          },
          { default: () => '我已确认整单实物已退回包装工位并拆除原包装' },
        ),
      ],
    ),
    okButtonProps: { danger: true },
    okText: '确认取消',
    onOk: async () => {
      if (!reason.trim()) {
        message.warning('请填写取消原因');
        return Promise.reject(new Error('cancel reason required'));
      }
      if (!physicalReturned) {
        message.warning('请确认实物已退回包装工位并拆除原包装');
        return Promise.reject(
          new Error('physical return confirmation required'),
        );
      }
      const result = await cancelShippingNotice({
        id: row.id,
        physicalReturned,
        reason: reason.trim(),
      });
      message.success('发货需求单已取消，产品已按质量状态退回待包装');
      if (currentNotice.value?.id === row.id) currentNotice.value = result;
      await fetchNotices();
    },
  });
}

function resetQuery() {
  Object.assign(query, {
    customerName: '',
    keyword: '',
    materialCode: '',
    modelCode: '',
    noticeNo: '',
    noticeStatus: undefined,
    orderNo: '',
    productType: undefined,
    shippingDateEnd: null,
    shippingDateStart: null,
  });
  queryTemplateName.value = '';
  selectedTemplateName.value = undefined;
  advancedQueryVisible.value = false;
  pageNo.value = 1;
  fetchNotices();
}

const [NoticeGrid, noticeGridApi] = useVbenVxeGrid({
  gridOptions: {
    autoResize: true,
    border: true,
    columns: noticeColumns,
    height: 'auto',
    pagerConfig: {
      enabled: true,
      pageSize: 20,
      pageSizes: [20, 50, 100],
    },
    proxyConfig: {
      ajax: {
        query: async ({ page }) => queryNoticePage(page),
      },
    },
    rowConfig: {
      isHover: true,
      keyField: 'id',
    },
    toolbarConfig: {
      custom: true,
      refresh: true,
      zoom: true,
    },
  } as VxeTableGridOptions<ShippingNotice>,
});
</script>

<template>
  <Page auto-content-height>
    <div class="package-fg-console shipping-notice-board">
      <section class="prototype-banner">
        <span class="console-main-icon"><IconifyIcon icon="lucide:clipboard-list" /></span>
        <div class="console-title-block">
          <div class="console-title-row">
            <h2 class="console-title-text">发货需求单管理</h2>
            <Tag class="console-title-tag" color="blue">需求提交</Tag>
          </div>
          <div class="console-meta-row">
            <span class="console-meta-item">
              <span class="console-meta-label">需求单</span>
              <span class="console-meta-value">{{ noticeTotal }}</span>
            </span>
            <span class="console-meta-item">
              <span class="console-meta-label">处理中</span>
              <span class="console-meta-value">{{ activeNoticeCount }}</span>
            </span>
            <span class="console-meta-item">
              <span class="console-meta-label">配货片数</span>
              <span class="console-meta-value">{{ lockedQty }}</span>
            </span>
            <span class="console-meta-item">
              <span class="console-meta-label">本页记录</span>
              <span class="console-meta-value">{{ notices.length }}</span>
            </span>
          </div>
        </div>
        <div class="console-action-group">
          <button class="action-tile" type="button" @click="searchNotices">
            <IconifyIcon icon="lucide:search" />
            <span>查询</span>
          </button>
          <button class="action-tile" type="button" @click="resetQuery">
            <IconifyIcon icon="lucide:rotate-ccw" />
            <span>重置</span>
          </button>
          <button class="action-tile" type="button" @click="openCreate">
            <IconifyIcon icon="lucide:file-plus-2" />
            <span>新建</span>
          </button>
          <button class="action-tile" type="button" @click="openImportExcel">
            <IconifyIcon icon="lucide:file-spreadsheet" />
            <span>导入EXCEL</span>
          </button>
        </div>
      </section>

      <section class="package-fg-filter-bar shipping-query-panel">
        <div class="shipping-simple-query">
          <label class="shipping-simple-query-label">关键词</label>
          <Input
            v-model:value="query.keyword"
            allow-clear
            placeholder="需求单号 / 发货客户 / 订单编号 / ERP订单号 / 料号 / 型号 / 产品名称"
            @press-enter="searchNotices"
          />
          <Button type="primary" @click="searchNotices">
            <template #icon><IconifyIcon icon="lucide:search" /></template>
            查询
          </Button>
          <Button @click="openAdvancedQuery">
            <template #icon><IconifyIcon icon="lucide:sliders-horizontal" /></template>
            多条件查询
          </Button>
          <Select
            v-model:value="selectedTemplateName"
            :options="queryTemplateOptions"
            allow-clear
            class="shipping-query-template-select"
            placeholder="查询条件模板"
            @change="handleLoadQueryTemplate"
          />
        </div>
        <div v-if="activeQueryTags.length > 0" class="shipping-query-tags">
          <span v-for="tag in activeQueryTags" :key="tag.key" class="shipping-query-tag">
            <b>{{ tag.label }}</b>
            <span>{{ tag.value }}</span>
            <button type="button" @click="removeQueryCondition(tag.key)">x</button>
          </span>
        </div>
      </section>

      <Modal
        v-model:open="importVisible"
        :confirm-loading="importLoading"
        :footer="null"
        centered
        title="导入发货需求单Excel"
        width="720px"
      >
        <div class="shipping-import-panel">
          <Alert
            message="每个Sheet会生成一张发货需求单草稿；原始Excel会作为附件挂到生成的需求单附件Tab中。"
            description="系统以Excel文件名作为导入批次键，同名文件不能重复导入。"
            show-icon
            type="info"
          />
          <UploadDragger
            accept=".xlsx,.xls"
            :before-upload="beforeExcelImportUpload"
            :disabled="importLoading"
            :show-upload-list="false"
          >
            <p class="ant-upload-drag-icon">
              <IconifyIcon class="text-4xl text-green-600" icon="lucide:file-spreadsheet" />
            </p>
            <p class="ant-upload-text">{{ importLoading ? '正在上传并导入...' : '点击或拖拽Excel到此处' }}</p>
            <p class="ant-upload-hint">支持 .xlsx/.xls；表格需包含“产品信息”和“明细追溯及检验记录”结构。</p>
          </UploadDragger>
        </div>
      </Modal>

      <Modal
        v-model:open="advancedQueryVisible"
        :footer="null"
        title="多条件查询"
        width="860px"
        wrap-class-name="shipping-advanced-query-modal"
      >
        <div class="shipping-advanced-query-body">
          <div class="shipping-advanced-query-grid">
            <div class="shipping-query-item">
              <label>需求单号</label>
              <Input v-model:value="query.noticeNo" allow-clear placeholder="需求单号" @press-enter="applyAdvancedQuery" />
            </div>
            <div class="shipping-query-item">
              <label>发货客户</label>
              <Input v-model:value="query.customerName" allow-clear placeholder="发货客户" @press-enter="applyAdvancedQuery" />
            </div>
            <div class="shipping-query-item">
              <label>类型</label>
              <Select v-model:value="query.productType" :options="productTypeOptions" allow-clear placeholder="类型" />
            </div>
            <div class="shipping-query-item">
              <label>产品型号</label>
              <Input v-model:value="query.modelCode" allow-clear placeholder="产品型号" @press-enter="applyAdvancedQuery" />
            </div>
            <div class="shipping-query-item">
              <label>料号</label>
              <Input v-model:value="query.materialCode" allow-clear placeholder="料号" @press-enter="applyAdvancedQuery" />
            </div>
            <div class="shipping-query-item">
              <label>订单编号</label>
              <Input v-model:value="query.orderNo" allow-clear placeholder="订单编号" @press-enter="applyAdvancedQuery" />
            </div>
            <div class="shipping-query-item">
              <label>状态</label>
              <Select v-model:value="query.noticeStatus" :options="statusOptions" allow-clear placeholder="状态" />
            </div>
            <div class="shipping-query-item">
              <label>发货日期起</label>
              <DatePicker v-model:value="query.shippingDateStart" placeholder="发货日期起" />
            </div>
            <div class="shipping-query-item">
              <label>发货日期止</label>
              <DatePicker v-model:value="query.shippingDateEnd" placeholder="发货日期止" />
            </div>
          </div>
          <div class="shipping-template-row">
            <label>模板名称</label>
            <Input v-model:value="queryTemplateName" allow-clear placeholder="填写名称后可保存当前查询条件" />
            <Button @click="saveQueryTemplate">
              <template #icon><IconifyIcon icon="lucide:save" /></template>
              保存模板
            </Button>
          </div>
          <div class="shipping-advanced-footer">
            <Button @click="advancedQueryVisible = false">关闭</Button>
            <Button @click="clearAdvancedQuery">清空条件</Button>
            <Button type="primary" @click="applyAdvancedQuery">应用查询</Button>
          </div>
        </div>
      </Modal>

      <section class="package-fg-grid-panel shipping-notice-grid-panel">
        <NoticeGrid table-title="发货需求单台账">
          <template #productType="{ row }">
            <Tag>{{ productTypeText(row.productType) }}</Tag>
          </template>
          <template #shippingTime="{ row }">{{ formatDateTime(row.shippingTime) }}</template>
          <template #noticeStatus="{ row }">
            <Tag :color="statusColor(row.noticeStatus)">{{ statusText(row.noticeStatus) }}</Tag>
          </template>
          <template #recorderTime="{ row }">{{ formatDateTime(row.recorderTime) }}</template>
          <template #action="{ row }">
            <div class="row-actions">
              <Button size="small" type="link" @click="openDetail(row)">详情</Button>
              <Button v-if="canEditNotice(row)" size="small" type="link" @click="openEdit(row)">编辑</Button>
              <Button v-if="canIssueNotice(row)" size="small" type="link" @click="issueNotice(row)">下达</Button>
              <Button v-if="canCancelNotice(row)" danger size="small" type="link" @click="cancelNotice(row)">取消</Button>
              <Button
                v-if="canDeleteNotice(row)"
                danger
                size="small"
                type="link"
                @click="deleteNotice(row)"
              >
                删除
              </Button>
            </div>
          </template>
        </NoticeGrid>
      </section>

      <Modal
        v-model:open="createVisible"
        :footer="null"
        :title="null"
        width="calc(100vw - 24px)"
        wrap-class-name="hc-pass-work-modal rough-report-work-modal inspection-task-work-modal shipping-notice-work-modal"
        @cancel="createVisible = false"
      >
        <div class="report-modal-body inspection-task-modal-body" :class="{ 'shipping-detail-maximized-body': createDetailMaximized }">
          <div class="shipping-notice-form-top" :class="{ 'shipping-notice-form-top--compact': createDetailMaximized }">
            <div class="shipping-notice-form-title-row">
              <span></span>
              <div class="inspection-form-title shipping-notice-title">{{ editingNoticeId ? '编辑发货需求单' : '新建发货需求单' }}</div>
              <div class="shipping-notice-form-actions">
                <Button size="small" @click="createVisible = false">关闭</Button>
                <Button :loading="saveLoading" size="small" type="primary" @click="submitSave">
                  {{ editingNoticeId && !isDraftStatus(editingNoticeStatus) ? '确认变更' : '保存草稿' }}
                </Button>
              </div>
            </div>
            <div class="shipping-form-sections">
              <fieldset class="shipping-form-fieldset">
                <legend>基本信息</legend>
                <div class="inspection-form-head shipping-section-form-head">
                  <label>需求单号</label>
                  <div class="inspection-form-control"><Input v-model:value="form.noticeNo" allow-clear placeholder="自动生成，可手填" /></div>
                  <label>类型</label>
                  <div class="inspection-form-control">
                    <Select
                      v-model:value="form.productType"
                      :options="productTypeOptions"
                      placeholder="类型"
                      @change="(value) => handleProductTypeChange(String(value || ''))"
                    />
                  </div>
                  <template v-if="isSampleType">
                    <label>样品数量</label>
                    <div class="inspection-form-control"><InputNumber v-model:value="form.requiredShipQty" :min="1" :precision="0" class="full-input" placeholder="样品数量" /></div>
                  </template>
                  <label>ERP订单号</label>
                  <div class="inspection-form-control"><Input v-model:value="form.erpOrderNo" allow-clear placeholder="ERP订单号" /></div>
                  <label>发货时间</label>
                  <div class="inspection-form-control">
                    <DatePicker
                      v-model:value="form.shippingTime"
                      :format="MES_DATETIME_FORMAT"
                      placeholder="发货时间"
                      show-time
                      :value-format="MES_DATETIME_FORMAT"
                    />
                  </div>
                  <label>发货备注</label>
                  <div class="inspection-form-control shipping-form-control--full-row"><Input v-model:value="form.remark" allow-clear placeholder="发货备注" /></div>
                  <template v-if="editingNoticeId && !isDraftStatus(editingNoticeStatus)">
                    <label>变更原因</label>
                    <div class="inspection-form-control shipping-form-control--full-row"><Input v-model:value="form.changeReason" allow-clear placeholder="必填；关键变更将回退并按新内容重走后续流程" /></div>
                  </template>
                </div>
              </fieldset>
              <fieldset v-if="!isSampleType" class="shipping-form-fieldset">
                <legend>客户要求</legend>
                <div class="inspection-form-head shipping-section-form-head">
                  <label>客户名称</label>
                  <div class="inspection-form-control"><Input v-model:value="form.customerName" allow-clear placeholder="客户名称" /></div>
                  <label>产品型号</label>
                  <div class="inspection-form-control"><Input v-model:value="form.externalProductModel" allow-clear placeholder="客户要求产品型号" /></div>
                  <label>产品编码</label>
                  <div class="inspection-form-control"><Input v-model:value="form.externalProductCode" allow-clear placeholder="客户要求产品编码" /></div>
                  <label>产品信息</label>
                  <div class="inspection-form-control"><Input v-model:value="form.externalProductInfo" allow-clear placeholder="产品信息" /></div>
                  <label>发货数量</label>
                  <div class="inspection-form-control"><InputNumber v-model:value="form.requiredShipQty" :min="1" :precision="0" class="full-input" placeholder="客户要求发货数量" /></div>
                  <label>片号范围</label>
                  <div class="inspection-form-control"><Input v-model:value="form.requiredSliceRange" allow-clear placeholder="客户要求片号范围" /></div>
                  <label>生产批号</label>
                  <div class="inspection-form-control"><Input v-model:value="form.requiredBatchNo" allow-clear placeholder="生产批号" /></div>
                  <label>生产日期</label>
                  <div class="inspection-form-control"><DatePicker v-model:value="form.requiredProductionDate" placeholder="生产日期" /></div>
                  <label>有效日期</label>
                  <div class="inspection-form-control"><DatePicker v-model:value="form.requiredExpiryDate" placeholder="有效日期" /></div>
                  <label>包装要求</label>
                  <div class="inspection-form-control shipping-form-control--span-3"><Input v-model:value="form.packingRequirement" allow-clear placeholder="包装要求" /></div>
                </div>
              </fieldset>

            </div>
          </div>
          <Tabs v-model:active-key="createActiveTab" class="shipping-detail-tabs">
            <template #rightExtra>
              <Button v-if="createActiveTab === 'items'" size="small" @click="createDetailMaximized = !createDetailMaximized">
                <template #icon>
                  <IconifyIcon :icon="createDetailMaximized ? 'lucide:minimize-2' : 'lucide:maximize-2'" />
                </template>
                {{ createDetailMaximized ? '还原' : '最大化' }}
              </Button>
            </template>
            <TabPane key="items" :tab="isSampleType ? '发货明细（选填）' : '发货明细'">
              <div class="inspection-form-subtitle shipping-inspection-subtitle">
                <span>{{ isSampleType ? '发货明细（选填）' : '发货明细' }}</span>
                <em>{{ isSampleType ? `可不填，当前 ${selectedQty} 条` : `明细 ${selectedQty} 条` }}</em>
                <div class="shipping-subtitle-actions">
                  <Button size="small" type="primary" @click="addShippingItem">
                    <template #icon><IconifyIcon icon="lucide:plus" /></template>
                    增加明细
                  </Button>
                </div>
              </div>
              <div
                :class="{ 'shipping-vxe-table--maximized': createDetailMaximized }"
                class="inspection-task-detail-table shipping-vxe-table"
              >
                <VxeTable :data="shippingItems" auto-resize border height="100%" row-id="rowKey" show-overflow size="small" stripe>
                  <VxeColumn field="internalModelCode" fixed="left" title="产品型号" width="230">
                    <template #default="{ row }">
                      <PickerInline
                        :model-value="row.internalModelCode"
                        :config="productModelPickerConfig"
                        placeholder="输入或选择型号"
                        @update:model-value="(value) => handleDraftModelInput(row, value)"
                        @pick="(option) => handleDraftModelSelected(row, option)"
                        @search="openDraftModelPicker(row)"
                      />
                    </template>
                  </VxeColumn>
                  <VxeColumn field="internalItemCode" fixed="left" title="分段批号" width="220">
                    <template #default="{ row }">
                      <Input
                        :value="row.internalItemCode"
                        allow-clear
                        placeholder="手填或选择母批段"
                        size="small"
                        @update:value="(value) => handleDraftInternalItemInput(row, String(value || ''))"
                        @press-enter="openBatchCandidateSelector(row)"
                      >
                        <template #suffix>
                          <IconifyIcon
                            class="shipping-cell-search-icon"
                            icon="lucide:search"
                            @click.stop="openBatchCandidateSelector(row)"
                          />
                        </template>
                      </Input>
                    </template>
                  </VxeColumn>
                  <VxeColumn :visible="!isSampleType" field="customerProductBatchNo" title="产品批号" width="160">
                    <template #default="{ row }"><Input v-model:value="row.customerProductBatchNo" allow-clear placeholder="默认生产批号，可修改" size="small" /></template>
                  </VxeColumn>
                  <VxeColumn :visible="!isSampleType" field="packageSliceNo" title="包装片号" width="180">
                    <template #default="{ row }"><Input v-model:value="row.packageSliceNo" allow-clear size="small" /></template>
                  </VxeColumn>
                  <VxeColumn field="remark" title="备注" min-width="180">
                    <template #default="{ row }"><Input v-model:value="row.remark" allow-clear size="small" /></template>
                  </VxeColumn>
                  <VxeColumn field="action" fixed="right" title="操作" width="120" align="center">
                    <template #default="{ row }">
                      <div class="shipping-row-actions">
                        <Button size="small" type="link" @click="copyShippingItem(row)">复制</Button>
                        <Button danger size="small" type="link" @click="removeShippingItem(row)">移除</Button>
                      </div>
                    </template>
                  </VxeColumn>
                </VxeTable>
              </div>
            </TabPane>
            <TabPane key="records" tab="处理记录">
              <div class="shipping-processing-record-grid">
                <div
                  v-for="field in createProcessingRecordFields"
                  :key="field.label"
                  class="shipping-processing-record-item"
                >
                  <label>{{ field.label }}</label>
                  <strong>{{ field.value }}</strong>
                </div>
              </div>
            </TabPane>
          </Tabs>
        </div>
      </Modal>

      <PickerModal
        :config="productModelPickerConfig"
        :open="productModelPickerOpen"
        @close="productModelPickerOpen = false"
        @pick="handleDraftModelModalPick"
      />

      <Modal
        v-model:open="batchSelectorVisible"
        :footer="null"
        :title="null"
        width="980px"
        wrap-class-name="shipping-batch-candidate-modal"
        @cancel="batchSelectorVisible = false"
      >
        <div class="shipping-batch-candidate">
          <div class="shipping-batch-candidate__head">
            <strong>选择分段批号</strong>
            <span>已入库可配成品及检验合格待包装片，按分段批号（母批段）聚合</span>
          </div>
          <div class="shipping-batch-candidate__query">
            <Input v-model:value="batchCandidateQuery.keyword" allow-clear placeholder="型号/母批段/料号" @press-enter="fetchBatchCandidates(true)" />
            <Input v-model:value="batchCandidateQuery.modelCode" allow-clear placeholder="产品型号" @press-enter="fetchBatchCandidates(true)" />
            <Input v-model:value="batchCandidateQuery.batchNo" allow-clear placeholder="分段批号（母批段）" @press-enter="fetchBatchCandidates(true)" />
            <Input v-model:value="batchCandidateQuery.materialCode" allow-clear placeholder="产品料号" @press-enter="fetchBatchCandidates(true)" />
            <Button type="primary" @click="fetchBatchCandidates(true)">查询</Button>
          </div>
          <div class="shipping-batch-candidate__table">
            <VxeTable
              :data="batchCandidateRows"
              :loading="batchSelectorLoading"
              auto-resize
              border
              :height="batchCandidateTableHeight"
              row-id="batchNo"
              show-overflow
              size="small"
              stripe
            >
              <VxeColumn field="modelCode" fixed="left" title="产品型号" width="112" />
              <VxeColumn field="batchNo" fixed="left" title="分段批号（母批段）" width="150" />
              <VxeColumn field="materialCode" title="产品料号" width="124" />
              <VxeColumn field="stockAvailableQty" title="在库可配" width="76" align="right" />
              <VxeColumn field="packagingReadyQty" title="待包装可配" width="92" align="right" />
              <VxeColumn field="availableQty" title="可配合计" width="76" align="right" />
              <VxeColumn field="lockedQty" title="锁定数量" width="76" align="right" />
              <VxeColumn field="totalQty" title="总数量" width="70" align="right" />
              <VxeColumn field="action" fixed="right" title="操作" width="66" align="center">
                <template #default="{ row }">
                  <Button size="small" type="link" @click="selectBatchCandidate(row)">选择</Button>
                </template>
              </VxeColumn>
            </VxeTable>
          </div>
          <Pagination
            v-model:current="batchCandidatePageNo"
            v-model:page-size="batchCandidatePageSize"
            :page-size-options="['10', '20', '50']"
            :show-total="(total: number) => `共 ${total} 条`"
            :total="batchCandidateTotal"
            class="shipping-selector-pagination"
            show-size-changer
            size="small"
            @change="handleBatchCandidatePageChange"
            @show-size-change="handleBatchCandidatePageChange"
          />
        </div>
      </Modal>

      <Modal
        v-model:open="stockSelectorVisible"
        :footer="null"
        :title="null"
        width="calc(100vw - 24px)"
        wrap-class-name="hc-pass-work-modal rough-report-work-modal inspection-task-work-modal shipping-notice-work-modal"
        @cancel="stockSelectorVisible = false"
      >
        <div class="report-modal-body inspection-task-modal-body">
          <fieldset class="erp-fieldset report-modal-toolbar inspection-form-sheet">
            <legend>发货库存明细查询</legend>
            <div class="shipping-selector-title-row">
              <div></div>
              <div class="inspection-form-title">发货库存明细查询</div>
              <div class="shipping-notice-form-actions">
                <Button size="small" @click="stockSelectorVisible = false">关闭</Button>
                <Button size="small" type="primary" @click="confirmAddShippingItems">加入发货明细</Button>
              </div>
            </div>
            <div class="inspection-form-head shipping-selector-form-head">
              <label>包装编号</label>
              <div class="inspection-form-control"><Input v-model:value="candidateQuery.packageNo" allow-clear placeholder="包装编号" @press-enter="fetchCandidates(true)" /></div>
              <label>片号</label>
              <div class="inspection-form-control"><Input v-model:value="candidateQuery.sliceBatchNo" allow-clear placeholder="片号" @press-enter="fetchCandidates(true)" /></div>
              <label>分段批次</label>
              <div class="inspection-form-control"><Input v-model:value="candidateQuery.batchNo" allow-clear placeholder="分段批次" @press-enter="fetchCandidates(true)" /></div>
              <label>库位</label>
              <div class="inspection-form-control"><Input v-model:value="candidateQuery.locationCode" allow-clear placeholder="库位" @press-enter="fetchCandidates(true)" /></div>
              <label>料号</label>
              <div class="inspection-form-control"><Input v-model:value="candidateQuery.materialCode" allow-clear placeholder="料号" @press-enter="fetchCandidates(true)" /></div>
              <label>型号</label>
              <div class="inspection-form-control"><Input v-model:value="candidateQuery.modelCode" allow-clear placeholder="型号" @press-enter="fetchCandidates(true)" /></div>
              <label>检验结果</label>
              <div class="inspection-form-control"><Select v-model:value="candidateQuery.qualityStatus" :options="qualityOptions" allow-clear placeholder="检验结果" /></div>
            </div>
            <div class="shipping-selector-actions">
              <Button type="primary" @click="fetchCandidates(true)">查询库存</Button>
            </div>
          </fieldset>
          <div class="inspection-form-subtitle">
            <span>可发货库存</span>
            <em>已勾选 {{ selectorSelectedRowKeys.length }} 条 / 共 {{ candidateTotal }} 条</em>
          </div>
          <div class="inspection-task-detail-table shipping-vxe-table">
            <VxeTable :data="candidateRows" :loading="candidateLoading" auto-resize border height="100%" row-id="id" show-overflow size="small" stripe>
              <VxeColumn field="select" fixed="left" title="选择" width="72" align="center">
                <template #default="{ row }">
                  <Button :type="isCandidateSelected(row) ? 'primary' : 'default'" size="small" @click="toggleCandidateStock(row)">
                    {{ isCandidateSelected(row) ? '已选' : '选择' }}
                  </Button>
                </template>
              </VxeColumn>
              <VxeColumn field="stockNo" fixed="left" title="库存号" width="170" />
              <VxeColumn field="packageNo" fixed="left" title="包装编号" width="170" />
              <VxeColumn field="sliceBatchNo" fixed="left" title="片号" width="170" />
              <VxeColumn field="batchNo" title="分段批次" width="150" />
              <VxeColumn field="materialCode" title="料号" width="140" />
              <VxeColumn field="modelCode" title="型号" width="110" />
              <VxeColumn field="qualityStatus" title="结果" width="80">
                <template #default="{ row }"><Tag :color="qualityColor(row.qualityStatus)">{{ row.qualityStatus || '-' }}</Tag></template>
              </VxeColumn>
              <VxeColumn field="qty" title="库存量" width="80" />
              <VxeColumn field="lockedQty" title="锁定量" width="80" />
              <VxeColumn field="location" title="库位" width="160">
                <template #default="{ row }">{{ locationText(row) }}</template>
              </VxeColumn>
              <VxeColumn field="inboundTime" title="入库时间" min-width="150">
                <template #default="{ row }">{{ formatDateTime(row.inboundTime) }}</template>
              </VxeColumn>
            </VxeTable>
          </div>
          <Pagination
            v-model:current="candidatePageNo"
            v-model:page-size="candidatePageSize"
            :page-size-options="['10', '20', '50']"
            :show-total="(total: number) => `共 ${total} 条`"
            :total="candidateTotal"
            class="shipping-selector-pagination"
            show-size-changer
            size="small"
            @change="handleCandidatePageChange"
            @show-size-change="handleCandidatePageChange"
          />
        </div>
      </Modal>

    </div>

    <Modal
      v-model:open="detailVisible"
      :footer="null"
      :title="null"
      width="calc(100vw - 24px)"
      wrap-class-name="hc-pass-work-modal rough-report-work-modal inspection-task-work-modal shipping-notice-work-modal"
      @cancel="detailVisible = false"
    >
      <div v-if="currentNotice" class="report-modal-body inspection-task-modal-body" :class="{ 'shipping-detail-maximized-body': detailItemsMaximized }">
        <div class="shipping-notice-form-top" :class="{ 'shipping-notice-form-top--compact': detailItemsMaximized }">
          <div class="shipping-notice-form-title-row">
            <span></span>
            <div class="inspection-form-title shipping-notice-title">发货需求单</div>
            <div class="shipping-notice-form-actions">
              <Button size="small" @click="detailVisible = false">
                关闭
              </Button>
              <Button v-if="canEditNotice(currentNotice)" size="small" @click="openEdit(currentNotice)">
                编辑
              </Button>
              <Button v-if="canIssueNotice(currentNotice)" size="small" type="primary" @click="issueNotice(currentNotice)">
                下达执行
              </Button>
              <Button v-if="canCancelNotice(currentNotice)" danger size="small" @click="cancelNotice(currentNotice)">
                取消
              </Button>
              <Button v-if="canDeleteNotice(currentNotice)" danger size="small" @click="deleteNotice(currentNotice)">
                删除
              </Button>
            </div>
          </div>
          <div class="shipping-form-sections">
            <fieldset class="shipping-form-fieldset">
              <legend>基本信息</legend>
              <div class="inspection-form-head shipping-section-form-head shipping-detail-readonly-head">
                <label>需求单号</label>
                <div class="inspection-form-control"><strong>{{ currentNotice.noticeNo || '-' }}</strong></div>
                <label>类型</label>
                <div class="inspection-form-control"><strong>{{ productTypeText(currentNotice.productType) }}</strong></div>
                <template v-if="currentNotice.productType === 'SAMPLE'">
                  <label>样品数量</label>
                  <div class="inspection-form-control"><strong>{{ currentNotice.requiredShipQty || currentNotice.noticeQty || 0 }}</strong></div>
                </template>
                <label>ERP订单号</label>
                <div class="inspection-form-control"><strong>{{ currentNotice.erpOrderNo || '-' }}</strong></div>
                <label>发货时间</label>
                <div class="inspection-form-control"><strong>{{ formatDateTime(currentNotice.shippingTime) }}</strong></div>
                <label>发货备注</label>
                <div class="inspection-form-control shipping-form-control--full-row"><strong>{{ currentNotice.remark || '-' }}</strong></div>
              </div>
            </fieldset>
            <fieldset v-if="currentNotice.productType !== 'SAMPLE'" class="shipping-form-fieldset">
              <legend>客户要求</legend>
              <div class="inspection-form-head shipping-section-form-head shipping-detail-readonly-head">
                <label>客户名称</label>
                <div class="inspection-form-control"><strong>{{ currentNotice.customerName || '-' }}</strong></div>
                <label>产品型号</label>
                <div class="inspection-form-control"><strong>{{ currentNotice.externalProductModel || '-' }}</strong></div>
                <label>产品编码</label>
                <div class="inspection-form-control"><strong>{{ currentNotice.externalProductCode || '-' }}</strong></div>
                <label>产品信息</label>
                <div class="inspection-form-control"><strong>{{ currentNotice.externalProductInfo || '-' }}</strong></div>
                <label>发货数量</label>
                <div class="inspection-form-control"><strong>{{ currentNotice.requiredShipQty || currentNotice.noticeQty || 0 }}</strong></div>
                <label>片号范围</label>
                <div class="inspection-form-control"><strong>{{ currentNotice.requiredSliceRange || '-' }}</strong></div>
                <label>生产批号</label>
                <div class="inspection-form-control"><strong>{{ currentNotice.requiredBatchNo || '-' }}</strong></div>
                <label>生产日期</label>
                <div class="inspection-form-control"><strong>{{ currentNotice.requiredProductionDate || '-' }}</strong></div>
                <label>有效日期</label>
                <div class="inspection-form-control"><strong>{{ currentNotice.requiredExpiryDate || '-' }}</strong></div>
                <label>包装要求</label>
                <div class="inspection-form-control shipping-form-control--span-3"><strong>{{ currentNotice.packingRequirement || '-' }}</strong></div>
              </div>
            </fieldset>

          </div>
        </div>
        <Tabs v-model:active-key="detailActiveTab" class="shipping-detail-tabs">
          <template #rightExtra>
            <Button v-if="detailActiveTab === 'items'" size="small" @click="detailItemsMaximized = !detailItemsMaximized">
              <template #icon>
                <IconifyIcon :icon="detailItemsMaximized ? 'lucide:minimize-2' : 'lucide:maximize-2'" />
              </template>
              {{ detailItemsMaximized ? '还原' : '最大化' }}
            </Button>
          </template>
          <TabPane key="items" :tab="currentNotice.productType === 'SAMPLE' ? '发货明细（选填）' : '发货明细'">
            <div class="inspection-form-subtitle shipping-inspection-subtitle">
              <span>{{ currentNotice.productType === 'SAMPLE' ? '发货明细（选填）' : '发货明细' }}</span>
              <em>{{ currentNotice.productType === 'SAMPLE' ? `可不填，当前 ${detailItems.length} 条` : `明细 ${detailItems.length} 条` }}</em>
            </div>
            <div
              :class="{ 'shipping-vxe-table--maximized': detailItemsMaximized }"
              class="inspection-task-detail-table shipping-vxe-table"
            >
              <VxeTable :data="detailItems" :loading="detailLoading" auto-resize border height="100%" row-id="id" show-overflow size="small" stripe>
                <VxeColumn field="internalModelCode" fixed="left" title="产品型号" width="140" />
                <VxeColumn field="internalItemCode" fixed="left" title="分段批号" width="170" />
                <VxeColumn :visible="currentNotice?.productType !== 'SAMPLE'" field="customerProductBatchNo" title="产品批号" width="150" />
                <VxeColumn :visible="currentNotice?.productType !== 'SAMPLE'" field="packageSliceNo" title="包装片号" width="170" />
                <VxeColumn field="actualSliceBatchNo" title="实际片号" width="180" />
                <VxeColumn field="shippingInspectionResult" title="检验结论" width="100">
                  <template #default="{ row }"><Tag :color="qualityColor(row.shippingInspectionResult)">{{ row.shippingInspectionResult || '-' }}</Tag></template>
                </VxeColumn>
                <VxeColumn field="shippingInspectorName" title="检验人" width="110" />
                <VxeColumn field="shippingInspectionTime" title="检验时间" width="160">
                  <template #default="{ row }">{{ formatDateTime(row.shippingInspectionTime) }}</template>
                </VxeColumn>
                <VxeColumn field="shippingPackageName" title="包装人" width="110" />
                <VxeColumn field="shippingPackageTime" title="包装时间" width="160">
                  <template #default="{ row }">{{ formatDateTime(row.shippingPackageTime) }}</template>
                </VxeColumn>
                <VxeColumn field="location" title="库位" width="170">
                  <template #default="{ row }">{{ locationText(row) }}</template>
                </VxeColumn>
                <VxeColumn field="lockStatus" title="状态" width="110">
                  <template #default="{ row }"><Tag :color="statusColor(row.lockStatus)">{{ statusText(row.lockStatus) }}</Tag></template>
                </VxeColumn>
              </VxeTable>
            </div>
          </TabPane>
          <TabPane key="records" tab="处理记录">
            <div class="shipping-processing-record-grid">
              <div
                v-for="field in processingRecordFields"
                :key="field.label"
                class="shipping-processing-record-item"
              >
                <label>{{ field.label }}</label>
                <strong>{{ field.value }}</strong>
              </div>
            </div>
          </TabPane>
          <TabPane key="attachments" :tab="`附件(${detailAttachments.length})`">
            <div v-if="detailAttachments.length" class="shipping-attachment-panel">
              <div v-if="currentExcelAttachment" class="shipping-excel-sheet-nav">
                <div class="shipping-excel-sheet-nav__main">
                  <span>原始Excel</span>
                  <strong>{{ currentExcelAttachment.sourceFileName || currentExcelAttachment.attachmentName || '原始Excel' }}</strong>
                  <em>
                    当前Sheet：{{ currentExcelAttachment.sourceSheetName || '-' }}
                    · {{ formatSheetPosition(currentExcelAttachment) }}
                  </em>
                </div>
                <div class="shipping-excel-sheet-nav__actions">
                  <Button
                    :disabled="!currentExcelAttachment.previousNoticeId"
                    size="small"
                    @click="switchExcelSheetNotice(currentExcelAttachment.previousNoticeId)"
                  >
                    <template #icon><IconifyIcon icon="lucide:chevron-left" /></template>
                    上一张Sheet
                  </Button>
                  <Button
                    :disabled="!currentExcelAttachment.nextNoticeId"
                    size="small"
                    @click="switchExcelSheetNotice(currentExcelAttachment.nextNoticeId)"
                  >
                    <template #icon><IconifyIcon icon="lucide:chevron-right" /></template>
                    下一张Sheet
                  </Button>
                  <Button size="small" type="primary" @click="openAttachment(currentExcelAttachment.attachmentUrl)">
                    <template #icon><IconifyIcon icon="lucide:download" /></template>
                    打开/下载
                  </Button>
                </div>
              </div>
              <div class="shipping-attachment-list">
                <article
                  v-for="attachment in detailAttachments"
                  :key="attachment.id || attachment.attachmentUrl"
                  class="shipping-attachment-item"
                >
                  <div class="shipping-attachment-icon">
                    <IconifyIcon icon="lucide:file-spreadsheet" />
                  </div>
                  <div class="shipping-attachment-main">
                    <strong>{{ attachment.attachmentName || '原始Excel' }}</strong>
                    <span>来源Sheet：{{ attachment.sourceSheetName || '-' }} · {{ formatSheetPosition(attachment) }}</span>
                    <em>{{ formatFileSize(attachment.fileSize) }} · {{ formatDateTime(attachment.createTime) }}</em>
                  </div>
                  <Button size="small" type="link" @click="openAttachment(attachment.attachmentUrl)">
                    打开原文件
                  </Button>
                </article>
              </div>
            </div>
            <div v-else class="shipping-attachment-empty">暂无附件</div>
          </TabPane>
        </Tabs>
      </div>
    </Modal>
  </Page>
</template>

<style scoped>
.shipping-notice-board {
  gap: 6px;
  grid-template-rows: max-content max-content minmax(0, 1fr);
}

.shipping-notice-board .prototype-banner {
  min-height: 66px;
  max-height: 74px;
}

.shipping-query-panel {
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

.shipping-simple-query {
  display: grid;
  grid-template-columns: 86px minmax(320px, 1fr) 94px 118px 210px;
  gap: 8px;
  align-items: stretch;
  min-width: 0;
}

.shipping-simple-query-label {
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

.shipping-simple-query :deep(.ant-input-affix-wrapper),
.shipping-simple-query :deep(.ant-select-selector) {
  min-height: 34px;
  border-radius: 0;
}

.shipping-simple-query :deep(.ant-btn) {
  height: 34px;
  border-radius: 0;
  font-weight: 800;
}

.shipping-query-template-select {
  min-width: 0;
}

.shipping-query-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  min-height: 26px;
  padding: 0 0 1px 86px;
}

.shipping-query-tag {
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

.shipping-query-tag b {
  flex: 0 0 auto;
  padding: 0 7px;
  color: #334155;
  background: #e2e8f0;
  border-right: 1px solid #c6d3df;
}

.shipping-query-tag span {
  min-width: 0;
  padding: 0 7px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.shipping-query-tag button {
  flex: 0 0 24px;
  height: 24px;
  color: #64748b;
  cursor: pointer;
  background: transparent;
  border: 0;
  border-left: 1px solid #c6d3df;
}

.shipping-query-tag button:hover {
  color: #dc2626;
  background: #fee2e2;
}

.shipping-query-item {
  display: grid;
  grid-template-columns: 88px minmax(0, 1fr);
  min-width: 0;
  overflow: hidden;
  border: 1px solid #c6d3df;
}

.shipping-query-item label {
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

.shipping-query-item > :not(label) {
  min-width: 0;
  background: #f8fafc;
}

.shipping-query-item :deep(.ant-input),
.shipping-query-item :deep(.ant-input-affix-wrapper),
.shipping-query-item :deep(.ant-picker),
.shipping-query-item :deep(.ant-select),
.shipping-query-item :deep(.ant-select-selector) {
  width: 100%;
  min-height: 32px;
  border: 0;
  border-radius: 0;
  box-shadow: none;
}

.shipping-advanced-query-body {
  display: grid;
  gap: 10px;
}

.shipping-advanced-query-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.shipping-template-row {
  display: grid;
  grid-template-columns: 88px minmax(0, 1fr) 110px;
  gap: 8px;
  align-items: stretch;
  padding-top: 2px;
}

.shipping-template-row label {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  min-height: 32px;
  padding: 0 10px;
  color: #334155;
  font-size: 12px;
  font-weight: 800;
  background: #dbe3ed;
  border: 1px solid #c6d3df;
}

.shipping-template-row :deep(.ant-input-affix-wrapper),
.shipping-template-row :deep(.ant-btn) {
  min-height: 32px;
  border-radius: 0;
}

.shipping-advanced-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding-top: 8px;
  border-top: 1px solid #d7e0ea;
}

.shipping-notice-grid-panel,
.candidate-band {
  display: flex;
  min-height: 0;
  flex-direction: column;
  padding: 8px;
  overflow: hidden;
  background: #f8fafc;
  border: 1px solid #8794a4;
}

.shipping-notice-grid-panel :deep(.vben-vxe-grid),
.shipping-notice-grid-panel :deep(.vxe-grid) {
  height: 100%;
  min-height: 0;
}

.shipping-notice-grid-panel :deep(.vxe-grid) {
  display: flex;
  flex-direction: column;
}

.shipping-notice-grid-panel :deep(.vxe-grid--table-wrapper) {
  flex: 1 1 auto;
  min-height: 0;
}

.shipping-notice-grid-panel :deep(.vxe-grid--pager-wrapper) {
  flex: 0 0 auto;
}

.shipping-notice-grid-panel :deep(.vxe-pager) {
  margin: 0;
  border-top: 1px solid #d8e0ea;
}

.row-actions {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.report-modal-body {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 24px);
  min-height: 0;
  overflow: hidden;
  color: #1f2937;
  background: #f5f7fa;
}

:global(.shipping-notice-work-modal .ant-modal) {
  top: 12px;
  max-width: calc(100vw - 24px);
  padding-bottom: 0;
}

:global(.shipping-notice-work-modal .ant-modal-content) {
  overflow: hidden;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  box-shadow: 0 10px 26px rgb(15 23 42 / 18%);
}

.erp-fieldset {
  padding: 8px 10px 10px;
  margin: 0;
  border: 1px solid #9fb6cd;
}

.erp-fieldset legend {
  padding: 0 8px;
  color: #075985;
  font-size: 14px;
  font-weight: 900;
}

.report-modal-toolbar {
  display: block;
  flex: 0 0 auto;
  min-height: 58px;
  background: linear-gradient(180deg, #f7fbff 0%, #e8f1fb 100%);
}

.inspection-task-modal-body {
  gap: 8px;
}

.inspection-task-modal-body .inspection-task-detail-table {
  flex: 0 1 320px;
  min-height: 0;
}

.report-modal-body .inspection-task-detail-table.shipping-vxe-table {
  display: flex;
  flex: 1 1 auto;
  min-height: 0;
  flex-direction: column;
}

.report-modal-body .shipping-detail-tabs .inspection-task-detail-table.shipping-vxe-table {
  flex: 1 1 auto;
}

.shipping-vxe-table :deep(.vxe-table),
.shipping-vxe-table :deep(.vxe-table--render-wrapper),
.shipping-vxe-table :deep(.vxe-table--main-wrapper),
.shipping-vxe-table :deep(.vxe-table--body-wrapper) {
  min-height: 0;
}

.inspection-task-detail-table :deep(.ant-spin-nested-loading),
.inspection-task-detail-table :deep(.ant-spin-container),
.inspection-task-detail-table :deep(.ant-table),
.inspection-task-detail-table :deep(.ant-table-container) {
  height: 100%;
  min-height: 0;
}

.inspection-task-detail-table :deep(.ant-spin-container),
.inspection-task-detail-table :deep(.ant-table-container) {
  display: flex;
  flex-direction: column;
}

.inspection-task-detail-table :deep(.ant-table) {
  flex: 1 1 auto;
}

.inspection-task-detail-table :deep(.ant-table-header) {
  flex: 0 0 auto;
}

.inspection-task-detail-table :deep(.ant-table-body) {
  flex: 1 1 auto;
  min-height: 0;
  max-height: none !important;
  overflow: auto !important;
}

.inspection-task-detail-table :deep(.ant-table-cell) {
  border-right: 1px solid #e5e7eb;
}

.inspection-task-view {
  display: grid;
  gap: 10px;
}

.inspection-form-sheet {
  display: grid;
  gap: 8px;
  background: linear-gradient(180deg, #f7fbff 0%, #e8f1fb 100%);
}

.shipping-notice-form-top {
  display: grid;
  flex: 0 0 auto;
  gap: 6px;
  min-height: 0;
  padding: 8px;
  background: #fff;
  border: 1px solid #e5e7eb;
}

.shipping-notice-form-title-row {
  display: grid;
  grid-template-columns: 260px minmax(0, 1fr) 260px;
  align-items: center;
  min-height: 28px;
}

.shipping-selector-title-row {
  display: grid;
  grid-template-columns: 260px minmax(0, 1fr) 260px;
  align-items: center;
  min-height: 28px;
}

.shipping-notice-form-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
}

.shipping-notice-form-top--compact {
  gap: 0;
  padding-bottom: 6px;
}

.shipping-notice-form-top--compact .shipping-form-sections {
  display: none;
}

.shipping-form-sections {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 8px;
  min-height: 0;
}

.shipping-form-section {
  display: grid;
  gap: 6px;
  min-width: 0;
}

.shipping-form-section-title {
  display: flex;
  align-items: center;
  min-height: 26px;
  padding: 0 10px;
  color: #1677ff;
  font-size: 13px;
  font-weight: 700;
  background: #f8fafc;
  border: 1px solid #e5e7eb;
  border-bottom: 0;
}

.shipping-form-fieldset {
  display: grid;
  gap: 6px;
  min-width: 0;
  padding: 10px 10px 8px;
  margin: 0;
  background: #fff;
  border: 1px solid #d8e0ec;
}

.shipping-form-fieldset > legend {
  padding: 0 8px;
  color: #1677ff;
  font-size: 13px;
  font-weight: 700;
  line-height: 20px;
  background: #fff;
}

.shipping-section-form-head {
  grid-template-columns:
    92px minmax(0, 1fr)
    92px minmax(0, 1fr)
    92px minmax(0, 1fr)
    92px minmax(0, 1fr);
}

.shipping-form-control--full-row {
  grid-column: span 7;
}

.shipping-form-control--span-3 {
  grid-column: span 5;
}

.shipping-create-workbench {
  display: flex;
  width: 100%;
  height: 100%;
  min-height: 0;
  flex-direction: column;
  overflow: hidden;
  color: #0f172a;
  background: #dbe4ee;
}

.shipping-create-sheet {
  display: grid;
  flex: 1 1 auto;
  grid-template-rows: max-content max-content max-content minmax(0, 1fr);
  gap: 8px;
  min-height: 0;
  padding: 14px;
  overflow: hidden;
  background: linear-gradient(180deg, #f7fbff 0%, #e8f1fb 100%);
}

.shipping-create-master {
  flex: 0 0 auto;
}

.inspection-form-head {
  display: grid;
  grid-template-columns: 92px minmax(0, 1fr) 92px minmax(0, 1fr) 92px minmax(0, 1fr) 92px minmax(0, 1fr);
  align-items: stretch;
  overflow: hidden;
  border: 1px solid #e5e7eb;
  border-right: 0;
  border-bottom: 0;
}

.inspection-form-head label {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  min-height: 32px;
  padding: 0 10px;
  color: #334155;
  font-size: 12px;
  font-weight: 800;
  background: #f3f4f6;
  border-right: 1px solid #e5e7eb;
  border-bottom: 1px solid #e5e7eb;
}

.inspection-form-control {
  display: flex;
  align-items: center;
  min-width: 0;
  min-height: 32px;
  padding: 0;
  background: #fff;
  border-right: 1px solid #e5e7eb;
  border-bottom: 1px solid #e5e7eb;
}

.inspection-form-control :deep(.ant-input),
.inspection-form-control :deep(.ant-input-affix-wrapper),
.inspection-form-control :deep(.ant-picker),
.inspection-form-control :deep(.ant-select),
.inspection-form-control :deep(.ant-select-selector),
.inspection-form-control :deep(.ant-input-number),
.inspection-form-control :deep(.ant-input-number-input),
.inspection-form-control :deep(.ant-input-number-affix-wrapper),
.inspection-form-control :deep(textarea.ant-input) {
  width: 100%;
  border: 0;
  border-radius: 0;
  box-shadow: none;
}

.shipping-detail-readonly-head .inspection-form-control {
  padding: 0 10px;
}

.shipping-detail-readonly-head .inspection-form-control strong {
  min-width: 0;
  overflow: hidden;
  color: #1f2937;
  font-family: Consolas, 'Microsoft YaHei', monospace;
  font-size: 13px;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.shipping-detail-readonly-head .inspection-form-control :deep(.ant-tag) {
  margin: 0;
}

.inspection-form-control--remark {
  grid-column: span 5;
}

.candidate-band {
  gap: 8px;
}

.shipping-create-detail {
  display: flex;
  min-height: 0;
  flex-direction: column;
  overflow: hidden;
}

.candidate-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 8px 10px;
  background:
    linear-gradient(90deg, rgba(14, 165, 233, 0.08) 0 1px, transparent 1px) 0 0 / 18px 100%,
    linear-gradient(180deg, #dfe7f0 0%, #cbd5e1 100%);
  border: 1px solid #8794a4;
}

.candidate-head > div {
  display: inline-flex;
  align-items: baseline;
  gap: 8px;
  color: #075985;
  font-weight: 900;
}

.candidate-head strong {
  font-family: Consolas, 'Microsoft YaHei', monospace;
  font-size: 20px;
  line-height: 24px;
}

.candidate-head span {
  color: #334155;
  font-size: 12px;
  font-weight: 800;
}

.candidate-query {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
  flex: 0 0 auto;
}

.candidate-query :deep(.ant-input),
.candidate-query :deep(.ant-input-affix-wrapper),
.candidate-query :deep(.ant-select-selector) {
  min-height: 32px;
  border-radius: 4px;
}

.full-input {
  width: 100%;
}

.shipping-create-detail > .ant-table-wrapper {
  flex: 1 1 auto;
  min-height: 0;
}

.shipping-create-detail-table {
  flex: 1 1 auto;
  min-height: 0;
}

.shipping-create-detail :deep(.ant-spin-nested-loading),
.shipping-create-detail :deep(.ant-spin-container),
.shipping-create-detail :deep(.ant-table),
.shipping-create-detail :deep(.ant-table-container) {
  height: 100%;
  min-height: 0;
}

.shipping-create-detail :deep(.ant-spin-container),
.shipping-create-detail :deep(.ant-table-container) {
  display: flex;
  flex-direction: column;
}

.shipping-create-detail :deep(.ant-table) {
  flex: 1 1 auto;
}

.shipping-create-detail :deep(.ant-table-body) {
  flex: 1 1 auto;
  min-height: 0;
  max-height: none !important;
  overflow-y: auto !important;
}

.shipping-create-footer {
  display: flex;
  flex: 0 0 52px;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 8px 14px;
  background: #edf2f7;
  border-top: 1px solid #8794a4;
}

.shipping-create-footer span {
  color: #334155;
  font-size: 12px;
  font-weight: 900;
}

.shipping-create-footer > div {
  display: inline-flex;
  gap: 8px;
}

.shipping-selector-sheet {
  display: grid;
  flex: 1 1 auto;
  grid-template-rows: max-content minmax(0, 1fr);
  gap: 8px;
  min-height: 0;
  padding: 14px;
  overflow: hidden;
  background: linear-gradient(180deg, #f7fbff 0%, #e8f1fb 100%);
}

.shipping-selector-fieldset,
.shipping-selector-table-fieldset {
  min-width: 0;
}

.shipping-selector-table-fieldset {
  display: flex;
  min-height: 0;
  flex-direction: column;
  overflow: hidden;
}

.shipping-selector-table {
  flex: 1 1 auto;
  min-height: 0;
}

.shipping-selector-table :deep(.ant-spin-nested-loading),
.shipping-selector-table :deep(.ant-spin-container),
.shipping-selector-table :deep(.ant-table),
.shipping-selector-table :deep(.ant-table-container) {
  height: 100%;
  min-height: 0;
}

.shipping-selector-table :deep(.ant-spin-container),
.shipping-selector-table :deep(.ant-table-container) {
  display: flex;
  flex-direction: column;
}

.shipping-selector-table :deep(.ant-table) {
  flex: 1 1 auto;
}

.shipping-selector-table :deep(.ant-table-body) {
  flex: 1 1 auto;
  min-height: 0;
  max-height: none !important;
  overflow-y: auto !important;
}

.shipping-selector-table-fieldset :deep(.ant-pagination) {
  flex: 0 0 auto;
  margin: 8px 0 0;
}

.shipping-detail-workbench {
  display: flex;
  width: 100%;
  height: 100%;
  min-height: 0;
  flex-direction: column;
  overflow: hidden;
  color: #0f172a;
  background: #dbe4ee;
}

.detail-toolbar {
  display: flex;
  flex: 0 0 54px;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 8px 14px;
  background:
    linear-gradient(90deg, rgba(14, 165, 233, 0.08) 0 1px, transparent 1px) 0 0 / 18px 100%,
    linear-gradient(180deg, #dfe7f0 0%, #cbd5e1 100%);
  border-bottom: 1px solid #8794a4;
}

.detail-toolbar > div {
  display: inline-flex;
  align-items: baseline;
  gap: 12px;
  min-width: 0;
}

.detail-toolbar strong {
  color: #172033;
  font-size: 22px;
  font-weight: 950;
}

.detail-toolbar span {
  min-width: 0;
  overflow: hidden;
  color: #075985;
  font-family: Consolas, 'Microsoft YaHei', monospace;
  font-size: 14px;
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.shipping-detail-sheet {
  display: grid;
  flex: 1 1 auto;
  grid-template-rows: max-content max-content max-content minmax(0, 1fr);
  gap: 8px;
  min-height: 0;
  padding: 14px;
  overflow: hidden;
  background: linear-gradient(180deg, #f7fbff 0%, #e8f1fb 100%);
}

.inspection-form-title {
  color: #1677ff;
  font-size: 15px;
  font-weight: 700;
  letter-spacing: 0;
  line-height: 20px;
  text-align: center;
}

.shipping-notice-title {
  color: #0f5fcf;
  font-size: 22px;
  font-weight: 950;
  line-height: 30px;
}

.inspection-form-subtitle {
  display: flex;
  flex: 0 0 auto;
  justify-content: space-between;
  color: #334155;
  font-size: 12px;
}

.inspection-form-subtitle span {
  font-weight: 900;
}

.inspection-form-subtitle em {
  color: #64748b;
  font-style: normal;
}

.shipping-inspection-subtitle {
  align-items: center;
  gap: 10px;
}

.shipping-inspection-subtitle :deep(.ant-btn) {
  margin-left: auto;
}

.shipping-subtitle-actions {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin-left: auto;
}

.shipping-subtitle-actions :deep(.ant-btn) {
  margin-left: 0;
}

.shipping-row-actions {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
}

.shipping-vxe-table--maximized {
  flex: 1 1 auto;
  min-height: 0;
}

.shipping-detail-maximized-body .shipping-detail-tabs {
  flex: 1 1 auto;
}

.shipping-detail-maximized-body .inspection-task-detail-table.shipping-vxe-table--maximized {
  flex: 1 1 auto;
  min-height: 0;
}

.shipping-vxe-table :deep(.hc-picker-inline) {
  width: 100%;
}

.shipping-vxe-table :deep(.hc-picker-inline .ant-input-affix-wrapper) {
  height: 24px;
  border-radius: 2px;
}

.shipping-cell-search-icon {
  color: #1677ff;
  cursor: pointer;
  font-size: 15px;
}

.shipping-detail-tabs {
  display: flex;
  flex: 1 1 auto;
  min-height: 0;
  flex-direction: column;
  overflow: hidden;
  padding: 0 8px 8px;
  background: #fff;
  border: 1px solid #e5e7eb;
}

.shipping-detail-tabs :deep(.ant-tabs-nav) {
  flex: 0 0 auto;
  margin: 0 0 6px;
  background: #fff;
}

.shipping-detail-tabs :deep(.ant-tabs-content-holder),
.shipping-detail-tabs :deep(.ant-tabs-content),
.shipping-detail-tabs :deep(.ant-tabs-tabpane) {
  display: flex;
  min-height: 0;
  flex: 1 1 auto;
  background: #fff;
}

.shipping-detail-tabs :deep(.ant-tabs-content) {
  height: 100%;
}

.shipping-detail-tabs :deep(.ant-tabs-tabpane-active) {
  display: flex;
  flex-direction: column;
  gap: 8px;
  overflow: hidden;
}

.shipping-processing-record-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 8px 12px;
  padding: 10px;
  overflow: auto;
  background: #fff;
  border: 1px solid #e5e7eb;
}

.shipping-processing-record-item {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 4px;
}

.shipping-processing-record-item label {
  color: #6b7280;
  font-size: 12px;
  font-weight: 700;
}

.shipping-processing-record-item strong {
  min-height: 32px;
  padding: 6px 8px;
  overflow: hidden;
  color: #1f2937;
  font-family: Consolas, 'Microsoft YaHei', monospace;
  font-size: 13px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
  background: #f9fafb;
  border: 1px solid #e5e7eb;
}

.shipping-import-panel {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.shipping-attachment-panel {
  display: flex;
  min-height: 0;
  flex: 1 1 auto;
  flex-direction: column;
  gap: 8px;
}

.shipping-excel-sheet-nav {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  background: #f0fdf4;
  border: 1px solid #bbf7d0;
}

.shipping-excel-sheet-nav__main {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 3px;
}

.shipping-excel-sheet-nav__main span,
.shipping-excel-sheet-nav__main em {
  color: #64748b;
  font-size: 12px;
  font-style: normal;
}

.shipping-excel-sheet-nav__main strong {
  min-width: 0;
  overflow: hidden;
  color: #064e3b;
  font-size: 13px;
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.shipping-excel-sheet-nav__actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 6px;
}

.shipping-attachment-list {
  display: flex;
  min-height: 0;
  flex: 1 1 auto;
  flex-direction: column;
  gap: 8px;
  padding: 8px;
  overflow: auto;
  background: #fff;
  border: 1px solid #e5e7eb;
}

.shipping-attachment-item {
  display: grid;
  grid-template-columns: 38px minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
  padding: 10px;
  background: #f8fafc;
  border: 1px solid #dbe3ef;
}

.shipping-attachment-icon {
  display: grid;
  width: 34px;
  height: 34px;
  place-items: center;
  color: #047857;
  background: #ecfdf5;
  border: 1px solid #bbf7d0;
}

.shipping-attachment-main {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 3px;
}

.shipping-attachment-main strong,
.shipping-attachment-main span,
.shipping-attachment-main em {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.shipping-attachment-main strong {
  color: #0f172a;
  font-size: 13px;
  font-weight: 900;
}

.shipping-attachment-main span,
.shipping-attachment-main em {
  color: #64748b;
  font-size: 12px;
  font-style: normal;
}

.shipping-attachment-empty {
  display: grid;
  min-height: 180px;
  place-items: center;
  color: #94a3b8;
  font-weight: 800;
  background: #f8fafc;
  border: 1px dashed #cbd5e1;
}

.shipping-batch-candidate {
  box-sizing: border-box;
  display: grid;
  grid-template-rows: max-content max-content max-content max-content;
  gap: 10px;
  width: 100%;
  min-height: 0;
  min-width: 0;
  max-width: 100%;
  padding: 4px 2px 0;
  overflow: hidden;
}

.shipping-batch-candidate__head {
  display: flex;
  align-items: baseline;
  gap: 10px;
  min-width: 0;
  max-width: 100%;
  overflow: hidden;
}

.shipping-batch-candidate__head strong {
  color: #1677ff;
  font-size: 16px;
  font-weight: 800;
}

.shipping-batch-candidate__head span {
  color: #64748b;
  font-size: 12px;
}

.shipping-batch-candidate__query {
  display: grid;
  grid-template-columns: minmax(180px, 1.2fr) repeat(3, minmax(140px, 1fr)) 88px;
  gap: 8px;
  min-width: 0;
  max-width: 100%;
}

.shipping-batch-candidate__table {
  width: 100%;
  min-height: 0;
  min-width: 0;
  max-width: 100%;
  overflow-x: auto;
  overflow-y: hidden;
}

.shipping-selector-form-head .inspection-form-control :deep(.ant-btn) {
  width: 100%;
  border-radius: 0;
}

.shipping-selector-actions {
  display: flex;
  justify-content: flex-end;
  padding-top: 8px;
}

.shipping-selector-actions :deep(.ant-btn) {
  width: 152px;
  min-width: 152px;
}

.shipping-selector-pagination {
  flex: 0 0 auto;
  margin: -2px 0 0;
  padding: 0 2px;
}

.modal-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  padding-top: 10px;
}

.report-action-footer :deep(.ant-btn) {
  width: 152px;
  min-width: 152px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.shipping-detail-fieldset {
  min-width: 0;
  padding: 10px;
  margin: 0;
  background: rgba(248, 250, 252, 0.82);
  border: 1px solid #8794a4;
}

.shipping-detail-fieldset legend {
  padding: 0 8px;
  color: #075985;
  font-size: 13px;
  font-weight: 900;
}

.inspection-task-view-head {
  display: grid;
  grid-template-columns: 92px minmax(0, 1fr) 92px minmax(0, 1fr) 92px minmax(0, 1fr) 92px minmax(0, 1fr);
  overflow: hidden;
  border: 1px solid #9fb6cd;
  border-right: 0;
  border-bottom: 0;
}

.inspection-task-view-head label,
.inspection-task-view-head strong {
  display: flex;
  align-items: center;
  min-height: 34px;
  padding: 0 10px;
  font-size: 12px;
  border-right: 1px solid #cbd5e1;
  border-bottom: 1px solid #cbd5e1;
}

.inspection-task-view-head label {
  justify-content: flex-end;
  color: #334155;
  font-weight: 800;
  background: #e2e8f0;
}

.inspection-task-view-head strong {
  min-width: 0;
  overflow: hidden;
  color: #075985;
  font-family: Consolas, 'Microsoft YaHei', monospace;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
  background: #f8fafc;
}

.inspection-task-view-head__remark {
  grid-column: span 7;
  white-space: normal !important;
}

.shipping-detail-items-fieldset {
  display: flex;
  min-height: 0;
  flex-direction: column;
  overflow: hidden;
}

.shipping-detail-table {
  flex: 1 1 auto;
  min-height: 0;
}

.shipping-detail-table :deep(.ant-spin-nested-loading),
.shipping-detail-table :deep(.ant-spin-container),
.shipping-detail-table :deep(.ant-table),
.shipping-detail-table :deep(.ant-table-container) {
  height: 100%;
  min-height: 0;
}

.shipping-detail-table :deep(.ant-spin-container),
.shipping-detail-table :deep(.ant-table-container) {
  display: flex;
  flex-direction: column;
}

.shipping-detail-table :deep(.ant-table) {
  flex: 1 1 auto;
}

.shipping-detail-table :deep(.ant-table-header) {
  flex: 0 0 auto;
}

.shipping-detail-table :deep(.ant-table-body) {
  flex: 1 1 auto;
  min-height: 0;
  max-height: none !important;
  overflow-y: auto !important;
}

.shipping-detail-footer {
  display: flex;
  flex: 0 0 38px;
  align-items: center;
  justify-content: flex-end;
  padding: 0 10px;
  color: #334155;
  font-size: 12px;
  font-weight: 900;
  background: #f8fafc;
  border: 1px solid #cbd5e1;
  border-top: 0;
}

@media (max-width: 1200px) {
  .shipping-simple-query {
    grid-template-columns: 86px minmax(240px, 1fr) 88px 110px;
  }

  .shipping-query-template-select {
    grid-column: 2 / -1;
  }

  .shipping-notice-board .prototype-banner {
    max-height: none;
  }

  .candidate-query {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .shipping-form-sections {
    grid-template-columns: 1fr;
  }

  .inspection-form-head {
    grid-template-columns: 92px minmax(0, 1fr) 92px minmax(0, 1fr);
  }

  .inspection-form-control--remark {
    grid-column: span 3;
  }

  .inspection-task-view-head {
    grid-template-columns: 92px minmax(0, 1fr) 92px minmax(0, 1fr);
  }

  .shipping-processing-record-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .inspection-task-view-head__remark {
    grid-column: span 3;
  }
}

@media (max-width: 760px) {
  .shipping-simple-query,
  .shipping-advanced-query-grid,
  .shipping-template-row {
    grid-template-columns: 1fr;
  }

  .shipping-query-tags {
    padding-left: 0;
  }

  .shipping-query-item {
    grid-template-columns: 82px minmax(0, 1fr);
  }

  .shipping-section-form-head {
    grid-template-columns: 88px minmax(0, 1fr);
  }

  .shipping-form-control--wide {
    grid-column: span 1;
  }

  .shipping-processing-record-grid {
    grid-template-columns: 1fr;
  }

  .shipping-template-row label,
  .shipping-simple-query-label {
    justify-content: flex-start;
  }
}
</style>

<style>
.hc-pass-work-modal .ant-modal {
  top: 0;
  width: 100vw !important;
  max-width: none;
  height: 100vh;
  margin: 0 !important;
  padding-bottom: 0;
}

.hc-pass-work-modal .ant-modal-header,
.hc-pass-work-modal .ant-modal-close {
  display: none !important;
}

.hc-pass-work-modal .ant-modal-body {
  display: flex;
  width: 100vw !important;
  height: 100vh !important;
  flex-direction: column;
  padding: 0 !important;
  overflow: hidden !important;
  background: #f5f7fa;
}

.hc-pass-work-modal .ant-modal-content {
  width: 100vw !important;
  height: 100vh !important;
  padding: 0 !important;
  border-radius: 0 !important;
  box-shadow: none;
}

.rough-report-work-modal .report-modal-body {
  box-sizing: border-box;
  flex: 1 1 auto;
  width: 100vw;
  height: auto;
  min-height: 0;
  padding: 8px;
  overflow: hidden;
  background:
    linear-gradient(90deg, rgba(30, 41, 59, 0.04) 1px, transparent 1px) 0 0 / 28px 28px,
    linear-gradient(0deg, rgba(30, 41, 59, 0.04) 1px, transparent 1px) 0 0 / 28px 28px,
    #f5f7fa;
}

.rough-report-work-modal .report-modal-toolbar {
  flex: 0 0 auto;
}

.rough-report-work-modal .modal-footer {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  padding: 8px 10px;
  background: #f5f7fa;
  border-top: 1px solid #cbd5e1;
}
</style>
