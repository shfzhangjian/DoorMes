<script lang="ts" setup>
import type { Dayjs } from 'dayjs';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesHcFinishedPackagingApi } from '#/api/mes/hc/package-fg/finished-packaging';

import { computed, reactive, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import {
  Button,
  Checkbox,
  DatePicker,
  Input,
  message,
  Modal,
  Select,
  Tag,
  Textarea,
} from 'ant-design-vue';
import dayjs from 'dayjs';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  getFgStockLedgerPage,
  getPackagingPieceLabels,
  manualOutboundFgStocks,
  markPackagingPieceLabelsPrinted,
  updateImportedFgStockData,
} from '#/api/mes/hc/package-fg/finished-packaging';

import '../shared/cut-round-board.css';
import {
  buildPackagingPieceLabelPayload,
  resolvePackagingPieceLabelTemplateCode,
} from '../../execution/report/shared/packagingPieceLabelPrint';

defineOptions({ name: 'MesPackageFgStock' });

type StockLedger = MesHcFinishedPackagingApi.FgStockLedger;
type PieceLabel = MesHcFinishedPackagingApi.PieceLabel;
type StockQuery = {
  batchNo: string;
  inboundDateEnd: Dayjs | null;
  inboundDateStart: Dayjs | null;
  keyword: string;
  locationCode: string;
  locationKeyword: string;
  materialCode: string;
  modelCode: string;
  packageNo: string;
  productionDateEnd: Dayjs | null;
  productionDateStart: Dayjs | null;
  qualityStatus: string;
  sliceBatchNo: string;
};
type QueryFieldKey =
  | 'batchNo'
  | 'inboundDateEnd'
  | 'inboundDateStart'
  | 'locationCode'
  | 'locationKeyword'
  | 'materialCode'
  | 'modelCode'
  | 'packageNo'
  | 'productionDateEnd'
  | 'productionDateStart'
  | 'qualityStatus'
  | 'sliceBatchNo';
type QueryFieldConfig = {
  key: QueryFieldKey;
  label: string;
  options?: Array<{ label: string; value: string }>;
  type?: 'date';
};
type QueryTemplate = {
  name: string;
  values: Partial<Record<keyof StockQuery, string>>;
};
type LocationMeta = {
  areaText: string;
  layerText: string;
  rackText: string;
};
type ImportedStockEditForm = {
  coaInspectionResult: string;
  correctionReason: string;
  inspectionResult: string;
  materialCode: string;
  modelCode: string;
  productionDate: string;
  remark: string;
  segmentBatchNo: string;
  sliceBatchNo: string;
  stockId?: number;
};

const QUERY_TEMPLATE_STORAGE_KEY = 'mes:fg-stock:query-templates';
const PRINT_AGENT_URL = 'http://127.0.0.1:17820';
const PRINT_BATCH_MAX_COUNT = 50;

const rows = ref<StockLedger[]>([]);
const total = ref(0);
const selectedStockMap = ref(new Map<number, StockLedger>());
const stockPrintVisible = ref(false);
const stockPrintLoading = ref(false);
const stockOutboundVisible = ref(false);
const stockOutboundLoading = ref(false);
const stockOutboundReason = ref('');
const importedStockEditVisible = ref(false);
const importedStockEditLoading = ref(false);
const importedStockEditForm = reactive<ImportedStockEditForm>({
  coaInspectionResult: '',
  correctionReason: '',
  inspectionResult: '',
  materialCode: '',
  modelCode: '',
  productionDate: '',
  remark: '',
  segmentBatchNo: '',
  sliceBatchNo: '',
  stockId: undefined,
});
const query = reactive<StockQuery>({
  batchNo: '',
  inboundDateEnd: null,
  inboundDateStart: null,
  keyword: '',
  locationCode: '',
  locationKeyword: '',
  materialCode: '',
  modelCode: '',
  packageNo: '',
  productionDateEnd: null as Dayjs | null,
  productionDateStart: null as Dayjs | null,
  qualityStatus: '',
  sliceBatchNo: '',
});
const advancedQueryVisible = ref(false);
const queryTemplateName = ref('');
const selectedTemplateName = ref<string | undefined>();
const queryTemplates = ref<QueryTemplate[]>(loadQueryTemplates());

const qualityStatusOptions = [
  { label: '冻结', value: 'FROZEN' },
  { label: 'OK 合格', value: 'OK' },
  { label: 'NG 不合格', value: 'NG' },
];
const importedStockEditQualityOptions = [
  { label: 'OK 合格', value: 'OK' },
  { label: 'NG 不合格', value: 'NG' },
];
const queryFieldConfigs: QueryFieldConfig[] = [
  { key: 'locationCode', label: '库位编号' },
  { key: 'locationKeyword', label: '所属库位' },
  { key: 'packageNo', label: '包装编号' },
  { key: 'batchNo', label: '分段批号' },
  { key: 'sliceBatchNo', label: '片号' },
  { key: 'materialCode', label: '料号' },
  { key: 'modelCode', label: '型号' },
  { key: 'qualityStatus', label: '检验结果', options: qualityStatusOptions },
  { key: 'productionDateStart', label: '生产日期起', type: 'date' },
  { key: 'productionDateEnd', label: '生产日期止', type: 'date' },
  { key: 'inboundDateStart', label: '入库日期起', type: 'date' },
  { key: 'inboundDateEnd', label: '入库日期止', type: 'date' },
];
const activeQueryTags = computed(() =>
  queryFieldConfigs
    .map((config) => ({
      key: config.key,
      label: config.label,
      value: getQueryFieldText(config.key),
    }))
    .filter((item) => item.value),
);
const queryTemplateOptions = computed(() =>
  queryTemplates.value.map((item) => ({
    label: item.name,
    value: item.name,
  })),
);

const currentDateText = computed(() => dayjs().format('YYYY-MM-DD'));
const currentTimeText = computed(() => dayjs().format('HH:mm:ss'));
const pageLocationCount = computed(
  () =>
    new Set(rows.value.map((item) => item.locationCode).filter(Boolean)).size,
);
const pagePackageCount = computed(
  () => new Set(rows.value.map((item) => item.packageNo).filter(Boolean)).size,
);
const pagePieceCount = computed(() => rows.value.length);
const selectedStocks = computed(() =>
  Array.from(selectedStockMap.value.values()),
);
const selectedStockCount = computed(() => selectedStocks.value.length);
const selectedStockPreview = computed(() =>
  selectedStocks.value
    .map((stock) => stock.sliceBatchNo || stock.stockNo || `库存#${stock.id}`)
    .slice(0, 20),
);
const importedStockEditExpiryDate = computed(() => {
  const productionDate = dayjs(importedStockEditForm.productionDate);
  if (!productionDate.isValid() || productionDate.year() < 2000)
    return '请输入有效生产日期';
  return productionDate
    .add(10, 'month')
    .subtract(1, 'day')
    .format('YYYY-MM-DD');
});
const isCurrentPageAllSelected = computed(
  () =>
    rows.value.length > 0 &&
    rows.value.every((row) => selectedStockMap.value.has(row.id)),
);
const isCurrentPagePartiallySelected = computed(
  () =>
    !isCurrentPageAllSelected.value &&
    rows.value.some((row) => selectedStockMap.value.has(row.id)),
);

function getValidExpiryDate(value?: string) {
  if (!value) return undefined;
  const expiryDate = dayjs(value);
  return expiryDate.isValid() && expiryDate.year() >= 2000
    ? expiryDate
    : undefined;
}

function isExpiredStock(row: StockLedger) {
  return getValidExpiryDate(row.expiryDate)?.isBefore(dayjs(), 'day') || false;
}

function formatExpiryDate(row: StockLedger) {
  return (
    getValidExpiryDate(row.expiryDate)?.format('YYYY-MM-DD') || '未记录有效时间'
  );
}

function qualityColor(value?: string) {
  if (value === 'OK') return 'green';
  if (value === 'NG') return 'red';
  return 'default';
}

function formatDateTime(value?: string) {
  if (!value) return '-';
  return String(value).replace('T', ' ').slice(0, 16);
}

function parseStockLocationMeta(row?: StockLedger | null): LocationMeta {
  const code = String(row?.locationCode || '');
  // 兼容历史库位编码“1-L1-1”及当前带仓库前缀的编码“S1-1-L1-1”。
  const matched = /^(?:[A-Z][A-Z0-9_-]{0,31}-)?(\d+)-L(\d+)-(\d+)$/i.exec(code);
  if (!matched) {
    return {
      areaText: '-',
      layerText: '-',
      rackText: '-',
    };
  }
  return {
    areaText: `${matched[3]}区`,
    layerText: `L${Number(matched[2])}层`,
    rackText: `${matched[1]}#货架`,
  };
}

function loadQueryTemplates() {
  if (typeof window === 'undefined') return [];
  try {
    const rawTemplates = window.localStorage.getItem(
      QUERY_TEMPLATE_STORAGE_KEY,
    );
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
  window.localStorage.setItem(
    QUERY_TEMPLATE_STORAGE_KEY,
    JSON.stringify(queryTemplates.value),
  );
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
  if (getQueryFieldConfig(key)?.type === 'date')
    return (value as Dayjs).format('YYYY-MM-DD');
  return String(value).trim();
}

function setQueryFieldValue(key: QueryFieldKey, value?: string) {
  const mutableQuery = query as unknown as Record<
    QueryFieldKey,
    Dayjs | null | string
  >;
  if (getQueryFieldConfig(key)?.type === 'date') {
    mutableQuery[key] = value ? dayjs(value) : null;
    return;
  }
  mutableQuery[key] = value || '';
}

function clearAdvancedQueryValues() {
  queryFieldConfigs.forEach((config) => setQueryFieldValue(config.key));
}

function clearAdvancedQuery() {
  clearAdvancedQueryValues();
  selectedTemplateName.value = undefined;
  queryTemplateName.value = '';
}

function snapshotQuery() {
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
  handleSearch();
}

function removeQueryCondition(key: QueryFieldKey) {
  setQueryFieldValue(key);
  selectedTemplateName.value = undefined;
  handleSearch();
}

function saveQueryTemplate() {
  const name = queryTemplateName.value.trim();
  if (!name) {
    message.warning('请填写查询模板名称');
    return;
  }
  const values = snapshotQuery();
  if (Object.keys(values).length === 0) {
    message.warning('请先填写查询条件');
    return;
  }
  const nextTemplates = queryTemplates.value.filter(
    (item) => item.name !== name,
  );
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
    if (queryFieldConfigs.some((item) => item.key === key)) {
      setQueryFieldValue(key as QueryFieldKey, value);
    }
  });
  queryTemplateName.value = template.name;
  handleSearch();
}

function handleLoadQueryTemplate(value?: string | number) {
  loadQueryTemplate(value ? String(value) : undefined);
}

function buildQueryParams(page?: { currentPage?: number; pageSize?: number }) {
  const params: Record<string, any> = {
    pageNo: page?.currentPage || 1,
    pageSize: page?.pageSize || 20,
  };
  if (query.keyword.trim()) params.keyword = query.keyword.trim();
  if (query.locationCode.trim())
    params.locationCode = query.locationCode.trim();
  if (query.batchNo.trim()) params.batchNo = query.batchNo.trim();
  if (query.sliceBatchNo.trim())
    params.sliceBatchNo = query.sliceBatchNo.trim();
  if (query.locationKeyword.trim())
    params.locationKeyword = query.locationKeyword.trim();
  if (query.packageNo.trim()) params.packageNo = query.packageNo.trim();
  if (query.materialCode.trim())
    params.materialCode = query.materialCode.trim();
  if (query.modelCode.trim()) params.modelCode = query.modelCode.trim();
  if (query.qualityStatus) params.qualityStatus = query.qualityStatus;
  if (query.productionDateStart)
    params.productionDateStart = query.productionDateStart.format('YYYY-MM-DD');
  if (query.productionDateEnd)
    params.productionDateEnd = query.productionDateEnd.format('YYYY-MM-DD');
  if (query.inboundDateStart)
    params.inboundDateStart = query.inboundDateStart.format('YYYY-MM-DD');
  if (query.inboundDateEnd)
    params.inboundDateEnd = query.inboundDateEnd.format('YYYY-MM-DD');
  return params;
}

function handleSearch() {
  gridApi.query();
}

function handleReset() {
  query.keyword = '';
  clearAdvancedQuery();
  advancedQueryVisible.value = false;
  gridApi.query();
}

function canEditImportedStock(row: StockLedger) {
  const sourceType = String(row.sourceType || '')
    .trim()
    .toUpperCase();
  const inboundUserName = String(row.inboundUserName || '').trim();
  // 兼容旧接口未返回来源字段的历史库存导入行；提交时仍由后端锁定并校验实际可更正条件。
  return sourceType === 'MANUAL_HISTORY' || inboundUserName === '历史库存导入';
}

function openImportedStockEditModal(row: StockLedger) {
  if (!canEditImportedStock(row)) {
    message.warning('仅可更正未被下游占用的历史导入单片包装库存');
    return;
  }
  importedStockEditForm.stockId = row.id;
  importedStockEditForm.sliceBatchNo = String(row.sliceBatchNo || '')
    .trim()
    .toUpperCase();
  importedStockEditForm.segmentBatchNo = String(
    row.batchNo || row.materialCode || '',
  )
    .trim()
    .toUpperCase();
  importedStockEditForm.materialCode = String(row.materialCode || '').trim();
  importedStockEditForm.modelCode = String(row.modelCode || '').trim();
  importedStockEditForm.productionDate = row.productionDate
    ? dayjs(row.productionDate).format('YYYY-MM-DD')
    : '';
  importedStockEditForm.inspectionResult = String(
    row.inspectionResult || row.qualityStatus || '',
  )
    .trim()
    .toUpperCase();
  importedStockEditForm.coaInspectionResult = String(
    row.coaInspectionResult || 'OK',
  )
    .trim()
    .toUpperCase();
  importedStockEditForm.remark = String(row.remark || '');
  importedStockEditForm.correctionReason = '';
  importedStockEditVisible.value = true;
}

async function submitImportedStockEdit() {
  const stockId = importedStockEditForm.stockId;
  const sliceBatchNo = importedStockEditForm.sliceBatchNo.trim().toUpperCase();
  const segmentBatchNo = importedStockEditForm.segmentBatchNo
    .trim()
    .toUpperCase();
  const materialCode = importedStockEditForm.materialCode.trim();
  const modelCode = importedStockEditForm.modelCode.trim();
  const productionDate = importedStockEditForm.productionDate.trim();
  const inspectionResult = importedStockEditForm.inspectionResult
    .trim()
    .toUpperCase();
  const coaInspectionResult = importedStockEditForm.coaInspectionResult
    .trim()
    .toUpperCase();
  const correctionReason = importedStockEditForm.correctionReason.trim();
  if (
    !stockId ||
    !sliceBatchNo ||
    !segmentBatchNo ||
    !materialCode ||
    !modelCode ||
    !productionDate ||
    !inspectionResult ||
    !coaInspectionResult
  ) {
    message.warning('请完整填写片号、分段批号、料号、型号、生产日期、FQC 和 COA 结果');
    return;
  }
  if (sliceBatchNo.length > 100) {
    message.warning('片号不能超过100个字符');
    return;
  }
  if (segmentBatchNo.length > 100) {
    message.warning('分段批号不能超过100个字符');
    return;
  }
  if (
    !dayjs(productionDate, 'YYYY-MM-DD', true).isValid() ||
    dayjs(productionDate).year() < 2000
  ) {
    message.warning('请输入有效生产日期');
    return;
  }
  importedStockEditLoading.value = true;
  try {
    const result = await updateImportedFgStockData({
      coaInspectionResult,
      correctionReason: correctionReason || undefined,
      inspectionResult,
      materialCode,
      modelCode,
      productionDate,
      remark: importedStockEditForm.remark.trim() || undefined,
      segmentBatchNo,
      sliceBatchNo,
      stockId,
    });
    importedStockEditVisible.value = false;
    message.success(
      result.labelReprintRequired
        ? '历史导入数据已更正，请按新数据补打片号标签'
        : '历史导入数据已更正',
    );
    await gridApi.query();
  } finally {
    importedStockEditLoading.value = false;
  }
}

function isStockSelected(row: StockLedger) {
  return selectedStockMap.value.has(row.id);
}

function setStockSelected(row: StockLedger, selected: boolean) {
  const nextSelectedStockMap = new Map(selectedStockMap.value);
  if (selected) {
    nextSelectedStockMap.set(row.id, row);
  } else {
    nextSelectedStockMap.delete(row.id);
  }
  selectedStockMap.value = nextSelectedStockMap;
}

function toggleCurrentPageSelection(selected: boolean) {
  const nextSelectedStockMap = new Map(selectedStockMap.value);
  rows.value.forEach((row) => {
    if (selected) {
      nextSelectedStockMap.set(row.id, row);
    } else {
      nextSelectedStockMap.delete(row.id);
    }
  });
  selectedStockMap.value = nextSelectedStockMap;
}

function handleStockCellClick({
  column,
  row,
}: {
  column?: { field?: string };
  row: StockLedger;
}) {
  if (!row || column?.field === 'selected' || column?.field === 'actions') {
    return;
  }
  setStockSelected(row, !isStockSelected(row));
}

function clearSelectedStocks() {
  selectedStockMap.value = new Map();
}

function normalizeSliceBatchNo(value?: string) {
  return String(value || '')
    .trim()
    .toUpperCase();
}

function piecePrintTicketId(label: PieceLabel) {
  return `${label.sourceType}-${label.sourceId}`;
}

function groupPieceLabelsByTemplate(labels: PieceLabel[]) {
  const groups = new Map<string, PieceLabel[]>();
  labels.forEach((label) => {
    const templateCode = resolvePackagingPieceLabelTemplateCode(
      label.qualityStatus,
    );
    groups.set(templateCode, [...(groups.get(templateCode) || []), label]);
  });
  return groups;
}

function buildNowText() {
  return dayjs().format('YYYY-MM-DD HH:mm:ss');
}

function buildPieceLabelContentSnapshot(
  payload: any,
  ticketId: string,
  printTime: string,
  templateCode: string,
) {
  const item = Array.isArray(payload?.items)
    ? payload.items.find((candidate: any) => candidate?.ticketId === ticketId)
    : payload;
  return JSON.stringify({
    fields: item?.fields || [],
    printTime,
    qrBottomText: item?.qrBottomText || printTime,
    qrTopText: item?.qrTopText || '',
    qrValue: item?.qrValue || '',
    documentTitle: item?.title || payload?.title || '',
    templateCode,
  });
}

async function getSelectedPieceLabels() {
  const selectedSliceBatchNos = selectedStocks.value
    .map((stock) => normalizeSliceBatchNo(stock.sliceBatchNo))
    .filter(Boolean);
  if (selectedSliceBatchNos.length !== selectedStockCount.value) {
    throw new Error('所选库存中存在未记录片号的数据，无法打印标签');
  }
  const labelsBySliceBatchNo = new Map<string, PieceLabel>();
  const failures: string[] = [];
  for (
    let start = 0;
    start < selectedSliceBatchNos.length;
    start += PRINT_BATCH_MAX_COUNT
  ) {
    const result = await getPackagingPieceLabels(
      selectedSliceBatchNos.slice(start, start + PRINT_BATCH_MAX_COUNT),
    );
    (result.failures || []).forEach((failure) => failures.push(failure));
    (result.labels || []).forEach((label) => {
      labelsBySliceBatchNo.set(
        normalizeSliceBatchNo(label.sliceBatchNo),
        label,
      );
    });
  }
  const missingSliceBatchNos = selectedSliceBatchNos.filter(
    (sliceBatchNo) => !labelsBySliceBatchNo.has(sliceBatchNo),
  );
  if (failures.length > 0 || missingSliceBatchNos.length > 0) {
    const detail = [
      ...failures,
      ...missingSliceBatchNos.map(
        (sliceBatchNo) => `未获取到片号 ${sliceBatchNo} 的标签数据`,
      ),
    ]
      .slice(0, 5)
      .join('；');
    throw new Error(detail || '未获取到可打印片号标签数据');
  }
  return selectedSliceBatchNos.map(
    (sliceBatchNo) => labelsBySliceBatchNo.get(sliceBatchNo)!,
  );
}

async function sendPiecePrintToAgent(
  labels: PieceLabel[],
  templateCode: string,
) {
  const printTime = buildNowText();
  const payload = await buildPackagingPieceLabelPayload(
    labels.map((label) => ({
      expiryDate: label.expiryDate,
      modelCode: label.modelCode,
      segmentBatchNo: label.segmentBatchNo,
      sliceBatchNo: label.sliceBatchNo,
      ticketId: piecePrintTicketId(label),
    })),
    printTime,
    { templateCode },
  );
  const endpoint =
    labels.length > 1 ? '/print/transfer-tickets' : '/print/transfer-ticket';
  const response = await fetch(`${PRINT_AGENT_URL}${endpoint}`, {
    body: JSON.stringify(payload),
    headers: { 'Content-Type': 'application/json' },
    method: 'POST',
  });
  const result = await response.json().catch(() => ({}));
  if (!response.ok) {
    throw new Error(
      result?.message || `本机打印服务返回异常：${response.status}`,
    );
  }
  return { payload, printTime, result };
}

function openStockPrintModal() {
  if (selectedStockCount.value === 0) {
    message.warning('请先勾选需要打印的成品库存片');
    return;
  }
  stockPrintVisible.value = true;
}

async function printSelectedStockLabels() {
  if (selectedStockCount.value === 0) {
    message.warning('请先勾选需要打印的成品库存片');
    return;
  }
  stockPrintLoading.value = true;
  let acceptedCount = 0;
  const acceptedSliceBatchNos = new Set<string>();
  const failures: string[] = [];
  const writeBackFailures: string[] = [];
  try {
    const labels = await getSelectedPieceLabels();
    for (const [templateCode, templateLabels] of groupPieceLabelsByTemplate(
      labels,
    )) {
      for (
        let start = 0;
        start < templateLabels.length;
        start += PRINT_BATCH_MAX_COUNT
      ) {
        const batch = templateLabels.slice(
          start,
          start + PRINT_BATCH_MAX_COUNT,
        );
        const { payload, printTime, result } = await sendPiecePrintToAgent(
          batch,
          templateCode,
        );
        const itemResults = Array.isArray(result?.itemResults)
          ? result.itemResults
          : [];
        const successfulLabels = batch.filter((label) => {
          const itemResult = itemResults.find(
            (item: any) => item.ticketId === piecePrintTicketId(label),
          );
          return itemResult
            ? itemResult.success === true && itemResult.accepted !== false
            : result?.success === true;
        });
        acceptedCount += successfulLabels.length;
        successfulLabels.forEach((label) =>
          acceptedSliceBatchNos.add(normalizeSliceBatchNo(label.sliceBatchNo)),
        );
        if (successfulLabels.length > 0) {
          try {
            await markPackagingPieceLabelsPrinted({
              items: successfulLabels.map((label) => ({
                labelContentJson: buildPieceLabelContentSnapshot(
                  payload,
                  piecePrintTicketId(label),
                  printTime,
                  templateCode,
                ),
                printerName: result?.printerName || 'packaging-piece-label',
                sourceId: label.sourceId,
                sourceType: label.sourceType,
              })),
            });
          } catch (error: any) {
            writeBackFailures.push(
              `${successfulLabels.map((label) => label.sliceBatchNo).join('、')}：${error?.message || error}`,
            );
          }
        }
        itemResults
          .filter(
            (item: any) => item?.success !== true || item?.accepted === false,
          )
          .forEach((item: any) =>
            failures.push(
              item?.message || `标签 ${item?.ticketId || '-'} 未进入打印队列`,
            ),
          );
      }
    }
    if (acceptedCount === 0) {
      throw new Error(failures[0] || '本机打印服务未确认任何标签进入打印队列');
    }
    const nextSelectedStockMap = new Map(selectedStockMap.value);
    selectedStocks.value.forEach((stock) => {
      if (
        acceptedSliceBatchNos.has(normalizeSliceBatchNo(stock.sliceBatchNo))
      ) {
        nextSelectedStockMap.delete(stock.id);
      }
    });
    selectedStockMap.value = nextSelectedStockMap;
    stockPrintVisible.value = false;
    if (writeBackFailures.length > 0 || failures.length > 0) {
      Modal.warning({
        content: `已发送 ${acceptedCount} 张标签。${[...writeBackFailures, ...failures].slice(0, 3).join('；')}`,
        title: '片号标签打印部分完成',
      });
    } else {
      message.success(`已发送 ${acceptedCount} 张片号标签`);
    }
  } catch (error: any) {
    Modal.warning({
      content: `未能完成片号标签打印：${error?.message || error}。请确认 HC-MES-PrintAgent 已启动且分切标签打印机配置正确。`,
      title: '片号标签打印失败',
    });
  } finally {
    stockPrintLoading.value = false;
  }
}

function openStockOutboundModal() {
  if (selectedStockCount.value === 0) {
    message.warning('请先勾选需要出库的成品库存片');
    return;
  }
  stockOutboundReason.value = '';
  stockOutboundVisible.value = true;
}

async function outboundSelectedStocks() {
  const reason = stockOutboundReason.value.trim();
  if (!reason) {
    message.warning('请填写出库原因');
    return;
  }
  if (selectedStockCount.value === 0) {
    message.warning('请先勾选需要出库的成品库存片');
    return;
  }
  stockOutboundLoading.value = true;
  try {
    const result = await manualOutboundFgStocks({
      reason,
      stockIds: selectedStocks.value.map((stock) => stock.id),
    });
    message.success(
      `已手工出库 ${result.stockCount} 片、${result.packageCount} 个内包装`,
    );
    stockOutboundVisible.value = false;
    clearSelectedStocks();
    await gridApi.query();
  } finally {
    stockOutboundLoading.value = false;
  }
}

const columns = [
  {
    field: 'selected',
    fixed: 'left',
    slots: { default: 'selection', header: 'selectionHeader' },
    title: '选择',
    width: 84,
  },
  {
    field: 'rack',
    formatter: ({ row }: { row: StockLedger }) =>
      parseStockLocationMeta(row).rackText,
    title: '货架',
    width: 100,
  },
  {
    field: 'layer',
    formatter: ({ row }: { row: StockLedger }) =>
      parseStockLocationMeta(row).layerText,
    title: '层',
    width: 90,
  },
  {
    field: 'area',
    formatter: ({ row }: { row: StockLedger }) =>
      parseStockLocationMeta(row).areaText,
    title: '区',
    width: 80,
  },
  {
    field: 'locationCode',
    showOverflow: 'tooltip',
    title: '货位编号',
    width: 150,
  },
  {
    field: 'packageNo',
    showOverflow: 'tooltip',
    title: '包装编号',
    width: 180,
  },
  { field: 'modelCode', showOverflow: 'tooltip', title: '型号', width: 140 },
  { field: 'sliceBatchNo', showOverflow: 'tooltip', title: '片号', width: 190 },
  { field: 'productionDate', title: '生产日期', width: 120 },
  {
    field: 'expiryDate',
    slots: { default: 'expiryDate' },
    title: '到期日期',
    width: 120,
  },
  {
    field: 'inboundTime',
    formatter: ({ row }: { row: StockLedger }) =>
      formatDateTime(row.inboundTime),
    title: '入库时间',
    width: 160,
  },
  {
    field: 'inboundUserName',
    showOverflow: 'tooltip',
    title: '入库人',
    width: 120,
  },
  { field: 'coaFrozen', title: '库存状态', width: 110,
    formatter: ({ row }: { row: StockLedger }) => row.coaFrozen ? '冻结' : row.qualityStatus === 'NG' ? '不合格' : '可用' },
  { field: 'coaFreezeReason', title: '冻结原因', width: 170, showOverflow: 'tooltip' },
  {
    field: 'actions',
    fixed: 'right',
    slots: { default: 'actions' },
    title: '操作',
    width: 92,
  },
];

const [Grid, gridApi] = useVbenVxeGrid({
  gridOptions: {
    autoResize: true,
    border: true,
    columns,
    height: 'auto',
    pagerConfig: {
      enabled: true,
      pageSize: 20,
      pageSizes: [20, 50, 100],
    },
    proxyConfig: {
      ajax: {
        query: async ({ page }) => {
          const result = await getFgStockLedgerPage(buildQueryParams(page));
          rows.value = result.list || [];
          const nextSelectedStockMap = new Map(selectedStockMap.value);
          rows.value.forEach((row) => {
            if (nextSelectedStockMap.has(row.id)) {
              nextSelectedStockMap.set(row.id, row);
            }
          });
          selectedStockMap.value = nextSelectedStockMap;
          total.value = Number(result.total || 0);
          return result;
        },
      },
    },
    rowConfig: {
      isHover: true,
      keyField: 'id',
    },
    rowClassName: ({ row }: { row: StockLedger }) =>
      isExpiredStock(row) ? 'fg-stock-expired-row' : row.coaFrozen ? 'fg-stock-frozen-row' : '',
    toolbarConfig: {
      custom: true,
      refresh: true,
      zoom: true,
    },
  } as VxeTableGridOptions<StockLedger>,
});
</script>

<template>
  <Page auto-content-height>
    <div class="package-fg-console fg-stock-board">
      <section class="prototype-banner">
        <span class="console-main-icon"
          ><IconifyIcon icon="lucide:boxes"
        /></span>
        <div class="console-title-block">
          <div class="console-title-row">
            <h2 class="console-title-text">成品库存查询</h2>
            <Tag class="console-title-tag" color="blue">现有成品库存</Tag>
          </div>
          <div class="console-meta-row">
            <span class="console-meta-item">
              <span class="console-meta-label">现存总数</span>
              <span class="console-meta-value">{{ total }}</span>
              <span class="console-meta-sub">片</span>
            </span>
            <span class="console-meta-item">
              <span class="console-meta-label">本页库位</span>
              <span class="console-meta-value">{{ pageLocationCount }}</span>
            </span>
            <span class="console-meta-item">
              <span class="console-meta-label">本页包装</span>
              <span class="console-meta-value">{{ pagePackageCount }}</span>
            </span>
            <span class="console-meta-item">
              <span class="console-meta-label">本页片数</span>
              <span class="console-meta-value">{{ pagePieceCount }}</span>
            </span>
          </div>
        </div>
        <div class="work-time-card">
          <div>{{ currentDateText }}</div>
          <strong>{{ currentTimeText }}</strong>
        </div>
        <div class="console-action-group">
          <button class="action-tile" type="button" @click="handleSearch">
            <IconifyIcon icon="lucide:search" />
            <span>查询</span>
          </button>
          <button
            v-access:code="['mes:inv:fg-stock:print']"
            class="action-tile"
            :disabled="selectedStockCount === 0"
            type="button"
            @click="openStockPrintModal"
          >
            <IconifyIcon icon="lucide:printer" />
            <span
              >打印{{
                selectedStockCount > 0 ? `（${selectedStockCount}）` : ''
              }}</span
            >
          </button>
          <button
            v-access:code="['mes:inv:fg-stock:manual-outbound']"
            class="action-tile"
            :disabled="selectedStockCount === 0"
            type="button"
            @click="openStockOutboundModal"
          >
            <IconifyIcon icon="lucide:log-out" />
            <span
              >出库{{
                selectedStockCount > 0 ? `（${selectedStockCount}）` : ''
              }}</span
            >
          </button>
          <button class="action-tile" type="button" @click="handleReset">
            <IconifyIcon icon="lucide:rotate-ccw" />
            <span>重置</span>
          </button>
        </div>
      </section>

      <section class="package-fg-filter-bar stock-query-panel">
        <div class="stock-simple-query">
          <label class="stock-simple-query-label">关键词</label>
          <Input
            v-model:value="query.keyword"
            allow-clear
            placeholder="库存号 / 包装编号 / 片号 / 分段批号 / 料号 / 型号 / 库位 / 入库单"
            @press-enter="handleSearch"
          />
          <Button type="primary" @click="handleSearch">
            <template #icon><IconifyIcon icon="lucide:search" /></template>
            查询
          </Button>
          <Button @click="openAdvancedQuery">
            <template #icon
              ><IconifyIcon icon="lucide:sliders-horizontal"
            /></template>
            多条件查询
          </Button>
          <Select
            v-model:value="selectedTemplateName"
            :options="queryTemplateOptions"
            allow-clear
            class="stock-query-template-select"
            placeholder="查询条件模板"
            @change="handleLoadQueryTemplate"
          />
        </div>
        <div v-if="activeQueryTags.length > 0" class="stock-query-tags">
          <span
            v-for="tag in activeQueryTags"
            :key="tag.key"
            class="stock-query-tag"
          >
            <b>{{ tag.label }}</b>
            <span>{{ tag.value }}</span>
            <button type="button" @click="removeQueryCondition(tag.key)">
              x
            </button>
          </span>
        </div>
      </section>

      <Modal
        v-model:open="advancedQueryVisible"
        :footer="null"
        title="多条件查询"
        width="920px"
        wrap-class-name="stock-advanced-query-modal"
      >
        <div class="stock-advanced-query-body">
          <div class="stock-advanced-query-grid">
            <div class="stock-query-item">
              <label>库位编号</label>
              <Input
                v-model:value="query.locationCode"
                allow-clear
                placeholder="精确库位编号"
              />
            </div>
            <div class="stock-query-item">
              <label>所属库位</label>
              <Input
                v-model:value="query.locationKeyword"
                allow-clear
                placeholder="库位编号 / 名称"
              />
            </div>
            <div class="stock-query-item">
              <label>包装编号</label>
              <Input
                v-model:value="query.packageNo"
                allow-clear
                placeholder="内包装 / 外箱号"
              />
            </div>
            <div class="stock-query-item">
              <label>分段批号</label>
              <Input
                v-model:value="query.batchNo"
                allow-clear
                placeholder="分段批号"
              />
            </div>
            <div class="stock-query-item">
              <label>片号</label>
              <Input
                v-model:value="query.sliceBatchNo"
                allow-clear
                placeholder="片号"
              />
            </div>
            <div class="stock-query-item">
              <label>料号</label>
              <Input
                v-model:value="query.materialCode"
                allow-clear
                placeholder="料号"
              />
            </div>
            <div class="stock-query-item">
              <label>型号</label>
              <Input
                v-model:value="query.modelCode"
                allow-clear
                placeholder="型号"
              />
            </div>
            <div class="stock-query-item">
              <label>检验结果</label>
              <Select
                v-model:value="query.qualityStatus"
                :options="qualityStatusOptions"
                allow-clear
                placeholder="请选择"
              />
            </div>
            <div class="stock-query-item">
              <label>生产日期起</label>
              <DatePicker
                v-model:value="query.productionDateStart"
                placeholder="请选择"
              />
            </div>
            <div class="stock-query-item">
              <label>生产日期止</label>
              <DatePicker
                v-model:value="query.productionDateEnd"
                placeholder="请选择"
              />
            </div>
            <div class="stock-query-item">
              <label>入库日期起</label>
              <DatePicker
                v-model:value="query.inboundDateStart"
                placeholder="请选择"
              />
            </div>
            <div class="stock-query-item">
              <label>入库日期止</label>
              <DatePicker
                v-model:value="query.inboundDateEnd"
                placeholder="请选择"
              />
            </div>
          </div>
          <div class="stock-template-row">
            <label>模板名称</label>
            <Input
              v-model:value="queryTemplateName"
              allow-clear
              placeholder="填写名称后可保存当前查询条件"
            />
            <Button @click="saveQueryTemplate">
              <template #icon><IconifyIcon icon="lucide:save" /></template>
              保存模板
            </Button>
          </div>
          <div class="stock-advanced-footer">
            <Button @click="advancedQueryVisible = false">关闭</Button>
            <Button @click="clearAdvancedQuery">清空条件</Button>
            <Button type="primary" @click="applyAdvancedQuery">应用查询</Button>
          </div>
        </div>
      </Modal>

      <section class="package-fg-grid-panel stock-ledger-panel">
        <Grid
          :table-title="`现有成品库存${selectedStockCount > 0 ? `（已选 ${selectedStockCount} 片）` : ''}`"
          @cell-click="handleStockCellClick"
        >
          <template #selectionHeader>
            <span class="stock-select-all" title="全选当前页">
              <Checkbox
                :checked="isCurrentPageAllSelected"
                :indeterminate="isCurrentPagePartiallySelected"
                @click.stop
                @change="toggleCurrentPageSelection($event.target.checked)"
              />
              <span>全选</span>
            </span>
          </template>
          <template #selection="{ row }">
            <Checkbox
              :checked="isStockSelected(row)"
              @click.stop
              @change="setStockSelected(row, $event.target.checked)"
            />
          </template>
          <template #expiryDate="{ row }">
            <span
              :class="{ 'fg-stock-expiry-date--expired': isExpiredStock(row) }"
            >
              {{ formatExpiryDate(row) }}
            </span>
          </template>
          <template #qualityStatus="{ row }">
            <Tag v-if="row.coaFrozen" color="gold" :title="row.coaFreezeReason">冻结</Tag>
            <Tag :color="qualityColor(row.qualityStatus)">{{
              row.qualityStatus === 'FROZEN' ? (row.coaFreezeReason || 'COA未放行') : row.qualityStatus || '-'
            }}</Tag>
          </template>
          <template #actions="{ row }">
            <Button
              v-if="canEditImportedStock(row)"
              v-access:code="[
                'mes:inv:fg-stock:manual-outbound',
                'mes:inv:fg-stock:imported-data-update',
              ]"
              size="small"
              type="link"
              @click.stop="openImportedStockEditModal(row)"
            >
              编辑
            </Button>
          </template>
        </Grid>
      </section>

      <Modal
        v-model:open="importedStockEditVisible"
        :confirm-loading="importedStockEditLoading"
        ok-text="确认更正"
        title="更正历史导入成品数据"
        width="760px"
        @ok="submitImportedStockEdit"
      >
        <div class="imported-stock-edit-tip">
          仅更正历史导入数据快照；新片号不可与现有追溯片号重复。数量、库位、包装号、库存状态和入库时间不可在此修改。有效期将按生产日期自动计算。
        </div>
        <div class="imported-stock-edit-grid">
          <div class="imported-stock-edit-item">
            <label><span class="required-mark">*</span>片号</label>
            <Input
              v-model:value="importedStockEditForm.sliceBatchNo"
              :maxlength="100"
              placeholder="请输入新片号"
              @blur="
                importedStockEditForm.sliceBatchNo =
                  importedStockEditForm.sliceBatchNo.trim().toUpperCase()
              "
            />
          </div>
          <div class="imported-stock-edit-item">
            <label><span class="required-mark">*</span>分段批号</label>
            <Input
              v-model:value="importedStockEditForm.segmentBatchNo"
              :maxlength="100"
              placeholder="请输入分段批号"
              @blur="
                importedStockEditForm.segmentBatchNo =
                  importedStockEditForm.segmentBatchNo.trim().toUpperCase()
              "
            />
          </div>
          <div class="imported-stock-edit-item">
            <label>料号</label>
            <Input
              v-model:value="importedStockEditForm.materialCode"
              :maxlength="64"
              placeholder="请输入产品料号"
            />
          </div>
          <div class="imported-stock-edit-item">
            <label>型号</label>
            <Input
              v-model:value="importedStockEditForm.modelCode"
              :maxlength="64"
              placeholder="请输入产品型号"
            />
          </div>
          <div class="imported-stock-edit-item">
            <label>生产日期</label>
            <DatePicker
              v-model:value="importedStockEditForm.productionDate"
              value-format="YYYY-MM-DD"
              placeholder="请选择生产日期"
            />
          </div>
          <div class="imported-stock-edit-item">
            <label>有效期</label>
            <Input :value="importedStockEditExpiryDate" disabled />
          </div>
          <div class="imported-stock-edit-item">
            <label>裁切 FQC</label>
            <Select
              v-model:value="importedStockEditForm.inspectionResult"
              :options="importedStockEditQualityOptions"
              placeholder="请选择"
            />
          </div>
          <div class="imported-stock-edit-item">
            <label>COA 送检</label>
            <Select
              v-model:value="importedStockEditForm.coaInspectionResult"
              :options="importedStockEditQualityOptions"
              placeholder="请选择"
            />
          </div>
          <div class="imported-stock-edit-item imported-stock-edit-item--wide">
            <label>备注</label>
            <Textarea
              v-model:value="importedStockEditForm.remark"
              :auto-size="{ minRows: 2, maxRows: 4 }"
              :maxlength="500"
              placeholder="可填写补充说明"
              show-count
            />
          </div>
          <div class="imported-stock-edit-item imported-stock-edit-item--wide">
            <label>修改原因（可选）</label>
            <Textarea
              v-model:value="importedStockEditForm.correctionReason"
              :auto-size="{ minRows: 2, maxRows: 4 }"
              :maxlength="500"
              placeholder="可选：说明本次历史数据更正原因"
              show-count
            />
          </div>
        </div>
      </Modal>

      <Modal
        v-model:open="stockPrintVisible"
        :confirm-loading="stockPrintLoading"
        :ok-button-props="{ disabled: selectedStockCount === 0 }"
        ok-text="确认打印"
        title="批量打印片号标签"
        width="680px"
        @ok="printSelectedStockLabels"
      >
        <div class="stock-action-modal-body">
          <p>
            将按现有包装片号标签模板打印所选片号；已打印的片号可直接重复打印。
          </p>
          <div class="stock-selection-summary">
            已选 <strong>{{ selectedStockCount }}</strong> 片，超过
            {{ PRINT_BATCH_MAX_COUNT }} 张时将自动分批发送。
          </div>
          <div class="stock-selection-list">
            {{ selectedStockPreview.join('、') }}
          </div>
          <div
            v-if="selectedStockCount > selectedStockPreview.length"
            class="stock-selection-more"
          >
            另有 {{ selectedStockCount - selectedStockPreview.length }} 片未展开
          </div>
        </div>
      </Modal>

      <Modal
        v-model:open="stockOutboundVisible"
        :confirm-loading="stockOutboundLoading"
        :ok-button-props="{
          disabled: !stockOutboundReason.trim() || selectedStockCount === 0,
        }"
        ok-text="确认出库"
        title="批量手工出库"
        width="680px"
        @ok="outboundSelectedStocks"
      >
        <div class="stock-action-modal-body">
          <p>
            系统会按内包装校验：同一包装内的所有可用片号必须全部被勾选，才允许批量出库。历史多片包装未选全时将拒绝出库。
          </p>
          <div class="stock-selection-summary">
            已选 <strong>{{ selectedStockCount }}</strong> 片
          </div>
          <div class="stock-selection-list">
            {{ selectedStockPreview.join('、') }}
          </div>
          <div
            v-if="selectedStockCount > selectedStockPreview.length"
            class="stock-selection-more"
          >
            另有 {{ selectedStockCount - selectedStockPreview.length }} 片未展开
          </div>
          <div class="stock-outbound-reason">
            <label>出库原因</label>
            <Textarea
              v-model:value="stockOutboundReason"
              :auto-size="{ minRows: 2, maxRows: 4 }"
              :maxlength="200"
              placeholder="请输入本次手工出库原因"
              show-count
            />
          </div>
        </div>
      </Modal>
    </div>
  </Page>
</template>

<style scoped>
.fg-stock-board {
  grid-template-rows: max-content max-content minmax(0, 1fr);
  gap: 8px;
}

.stock-query-panel {
  display: flex;
  box-sizing: border-box;
  flex-direction: column;
  gap: 8px;
  min-height: 0;
  min-width: 0;
  padding: 8px 10px;
  overflow: hidden;
  background: #eef3f8;
  border: 1px solid #8794a4;
}

.stock-simple-query {
  display: grid;
  grid-template-columns:
    max-content minmax(300px, 1fr)
    max-content max-content minmax(190px, 0.65fr);
  gap: 8px;
  align-items: center;
  min-width: 0;
}

.stock-simple-query-label {
  color: #243142;
  font-size: 13px;
  font-weight: 600;
  white-space: nowrap;
}

.stock-query-template-select {
  width: 100%;
  min-width: 0;
}

.stock-query-panel :deep(.ant-input),
.stock-query-panel :deep(.ant-input-affix-wrapper),
.stock-query-panel :deep(.ant-picker),
.stock-query-panel :deep(.ant-select-selector),
.stock-query-item :deep(.ant-input),
.stock-query-item :deep(.ant-input-affix-wrapper),
.stock-query-item :deep(.ant-picker),
.stock-query-item :deep(.ant-select-selector) {
  width: 100%;
  min-height: 34px;
  border-radius: 6px;
}

.stock-query-panel :deep(.ant-input-affix-wrapper > input.ant-input),
.stock-query-item :deep(.ant-input-affix-wrapper > input.ant-input) {
  height: 24px;
}

.stock-query-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  max-height: 64px;
  overflow: auto;
}

.stock-query-tag {
  display: inline-flex;
  gap: 6px;
  align-items: center;
  max-width: 100%;
  padding: 4px 8px;
  color: #243142;
  background: #ffffff;
  border: 1px solid #c8d3df;
  border-radius: 6px;
}

.stock-query-tag span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.stock-query-tag button {
  width: 18px;
  height: 18px;
  padding: 0;
  color: #64748b;
  cursor: pointer;
  background: transparent;
  border: 0;
}

.stock-advanced-query-body {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.stock-advanced-query-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.stock-query-item,
.stock-template-row {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.stock-query-item label,
.stock-template-row label {
  color: #475569;
  font-size: 12px;
  font-weight: 600;
}

.stock-template-row {
  display: grid;
  grid-template-columns: 72px minmax(0, 1fr) max-content;
  align-items: center;
}

.stock-advanced-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.stock-ledger-panel {
  display: flex;
  min-height: 0;
  flex-direction: column;
  padding: 8px;
  overflow: hidden;
  background: #f8fafc;
  border: 1px solid #8794a4;
}

.stock-ledger-panel :deep(.vben-vxe-grid),
.stock-ledger-panel :deep(.vxe-grid) {
  height: 100%;
  min-height: 0;
}

.stock-ledger-panel :deep(.vxe-grid) {
  display: flex;
  flex-direction: column;
}

.stock-ledger-panel :deep(.vxe-grid--table-wrapper) {
  flex: 1 1 auto;
  min-height: 0;
}

.stock-ledger-panel :deep(.vxe-grid--pager-wrapper) {
  flex: 0 0 auto;
}

.stock-ledger-panel :deep(.vxe-pager) {
  margin: 0;
  border-top: 1px solid #d8e0ea;
}

.stock-ledger-panel :deep(.fg-stock-expired-row > td) {
  background-color: #fff1f0 !important;
  color: #cf1322;
}

.stock-ledger-panel :deep(.fg-stock-expired-row:hover > td) {
  background-color: #ffccc7 !important;
}

.fg-stock-expiry-date--expired {
  color: #cf1322;
  font-weight: 700;
}

.stock-select-all {
  display: inline-flex;
  gap: 3px;
  align-items: center;
  color: #334155;
  white-space: nowrap;
}

.stock-action-modal-body {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.stock-action-modal-body p {
  margin: 0;
  color: #475569;
  line-height: 1.65;
}

.stock-selection-summary {
  padding: 8px 10px;
  color: #1e3a5f;
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  border-radius: 6px;
}

.stock-selection-list {
  max-height: 112px;
  padding: 8px 10px;
  overflow: auto;
  color: #334155;
  line-height: 1.7;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
}

.stock-selection-more {
  margin-top: -6px;
  color: #64748b;
  font-size: 12px;
}

.stock-outbound-reason {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.stock-outbound-reason label {
  color: #334155;
  font-weight: 600;
}

.imported-stock-edit-tip {
  margin-bottom: 14px;
  padding: 8px 10px;
  color: #7c2d12;
  line-height: 1.65;
  background: #fff7ed;
  border: 1px solid #fed7aa;
  border-radius: 6px;
}

.imported-stock-edit-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.imported-stock-edit-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.imported-stock-edit-item label {
  color: #334155;
  font-size: 13px;
  font-weight: 600;
}

.imported-stock-edit-item--wide {
  grid-column: 1 / -1;
}

.required-mark {
  margin-right: 3px;
  color: #dc2626;
}

@media (max-width: 720px) {
  .imported-stock-edit-grid {
    grid-template-columns: 1fr;
  }

  .imported-stock-edit-item--wide {
    grid-column: auto;
  }
}

@media (max-width: 1280px) {
  .stock-simple-query {
    grid-template-columns: max-content minmax(0, 1fr) max-content max-content;
  }

  .stock-query-template-select {
    grid-column: 2 / -1;
  }

  .stock-advanced-query-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .stock-simple-query,
  .stock-advanced-query-grid,
  .stock-template-row {
    grid-template-columns: 1fr;
  }

  .stock-query-template-select {
    grid-column: auto;
  }
}
</style>

<style scoped>
.stock-ledger-panel :deep(.fg-stock-frozen-row > td) { background: #fff7cc !important; }
.stock-ledger-panel :deep(.fg-stock-frozen-row:hover > td) { background: #ffef99 !important; }
</style>
