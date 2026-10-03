<script lang="ts" setup>
import type { TableColumnsType, TablePaginationConfig } from 'ant-design-vue';
import type { MesHcTestOuterPackagePrintApi } from '#/api/mes/hc/test-outer-package-print';

import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue';

import { Page } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import {
  Alert,
  Button,
  Input,
  Modal,
  Select,
  Table as ATable,
  Tag,
  Tooltip,
  message,
} from 'ant-design-vue';

import {
  buildTestOuterPrintPayload,
  getTestOuterCustomerProductPage,
  getTestOuterTemplateList,
  getTestOuterWaitSegmentPage,
  getTestOuterWaitSegmentPieceList,
} from '#/api/mes/hc/test-outer-package-print';

defineOptions({ name: 'MesHcExecutionTestOuterPackagePrint' });

type CustomerProduct = MesHcTestOuterPackagePrintApi.CustomerProduct;
type LabelKind = MesHcTestOuterPackagePrintApi.LabelKind;
type PrintDesign = MesHcTestOuterPackagePrintApi.PrintDesign;
type PrintPayload = MesHcTestOuterPackagePrintApi.PrintPayload;
type PrintPayloadItem = MesHcTestOuterPackagePrintApi.PrintPayloadItem;
type VariableCheck = MesHcTestOuterPackagePrintApi.VariableCheck;
type WaitPiece = MesHcTestOuterPackagePrintApi.WaitPiece;
type WaitSegment = MesHcTestOuterPackagePrintApi.WaitSegment;

type LocalPrinter = {
  dpi?: number;
  dpiSource?: string;
  isDefault?: boolean;
  name: string;
};

const PRINT_AGENT_URL = 'http://127.0.0.1:18081';
const FILE_PRINT_TARGET = '__file__';

const labelKindOptions: Array<{ label: string; value: LabelKind }> = [
  { label: 'Pad背标', value: 'padBack' },
  { label: '洁净袋', value: 'cleanBag' },
  { label: '盒正标', value: 'boxFront' },
  { label: '客户侧标', value: 'customerSide' },
];

const segmentKeyword = ref('');
const segmentRows = ref<WaitSegment[]>([]);
const segmentLoading = ref(false);
const selectedSegment = ref<WaitSegment>();
const segmentPagination = reactive<TablePaginationConfig>({
  current: 1,
  pageSize: 20,
  showSizeChanger: true,
  total: 0,
});

const pieceRows = ref<WaitPiece[]>([]);
const pieceLoading = ref(false);
const selectedPieceKeys = ref<string[]>([]);
const selectedPieces = ref<WaitPiece[]>([]);

const printModalOpen = ref(false);
const printLoading = ref(false);
const customerLoading = ref(false);
const customerKeyword = ref('');
const customerProducts = ref<CustomerProduct[]>([]);
const selectedCustomerProductKey = ref<string>();
const selectedLabelKind = ref<LabelKind>();
const templateLoading = ref(false);
const templateRows = ref<PrintDesign[]>([]);
const selectedDesignId = ref<number>();
const selectedPrintTarget = ref(FILE_PRINT_TARGET);
const selectedPrinterDpi = ref(300);
const localPrinters = ref<LocalPrinter[]>([]);
const printerLoading = ref(false);
const printAgentStatus = ref<'checking' | 'offline' | 'online'>('offline');
const printAgentError = ref('');
const printAgentSchemaVersion = ref(0);
const previewLoading = ref(false);
const previewImageUrl = ref('');
const previewError = ref('');
const preparedPrintPayload = ref<PrintPayload>();
const preparedFingerprint = ref('');
const selectedPreviewSliceBatchNo = ref<string>();
let customerSearchTimer: number | undefined;
let pieceRequestSequence = 0;
let templateRequestSequence = 0;

const selectedCustomerProduct = computed(() => {
  return customerProducts.value.find((item) => customerProductKey(item) === selectedCustomerProductKey.value);
});

const segmentColumns: TableColumnsType<WaitSegment> = [
  { dataIndex: 'segmentBatchNo', fixed: 'left', key: 'segmentBatchNo', title: '段号', width: 170 },
  { align: 'right', dataIndex: 'pieceCount', key: 'pieceCount', title: '片数', width: 80 },
  { dataIndex: 'modelCode', key: 'modelCode', title: '型号', width: 110 },
  { dataIndex: 'materialCode', key: 'materialCode', title: '料号', width: 130 },
  { dataIndex: 'productionDateStart', key: 'productionDateStart', title: '生产日期', width: 150 },
  { dataIndex: 'sampleSliceBatchNo', key: 'sampleSliceBatchNo', title: '样例片号', width: 170 },
  { dataIndex: 'packagingQualityStatus', key: 'packagingQualityStatus', title: '质量', width: 80 },
];

const pieceColumns: TableColumnsType<WaitPiece> = [
  { dataIndex: 'sliceBatchNo', fixed: 'left', key: 'sliceBatchNo', title: '片号', width: 190 },
  { dataIndex: 'sourceType', key: 'sourceType', title: '来源', width: 120 },
  { dataIndex: 'modelCode', key: 'modelCode', title: '型号', width: 110 },
  { dataIndex: 'materialCode', key: 'materialCode', title: '料号', width: 130 },
  { dataIndex: 'productionDate', key: 'productionDate', title: '生产日期', width: 110 },
  { dataIndex: 'expiryDate', key: 'expiryDate', title: '有效期', width: 110 },
  { dataIndex: 'inspectionResult', key: 'inspectionResult', title: '裁切', width: 80 },
  { dataIndex: 'coaInspectionResult', key: 'coaInspectionResult', title: 'COA', width: 80 },
];

const selectedPieceColumns: TableColumnsType<WaitPiece> = [
  { dataIndex: 'sliceBatchNo', key: 'sliceBatchNo', title: '片号', width: 190 },
  { dataIndex: 'segmentBatchNo', key: 'segmentBatchNo', title: '段号', width: 150 },
  { dataIndex: 'modelCode', key: 'modelCode', title: '型号', width: 110 },
  { dataIndex: 'productionDate', key: 'productionDate', title: '生产日期', width: 110 },
];

const pieceRowSelection = computed(() => ({
  onChange: (keys: (number | string)[], rows: WaitPiece[]) => {
    selectedPieceKeys.value = keys.map(String);
    selectedPieces.value = rows;
    invalidatePrintPreview();
  },
  selectedRowKeys: selectedPieceKeys.value,
}));

const printTargetOptions = computed(() => [
  { label: '生成调试文件', value: FILE_PRINT_TARGET },
  ...localPrinters.value.map((printer) => ({
    label: `${printer.isDefault ? `${printer.name}（默认）` : printer.name}${printer.dpi ? ` / ${printer.dpi}dpi` : ''}`,
    value: printer.name,
  })),
]);

const duplicateCustomerProductKeys = computed(() => {
  const counts = new Map<string, number>();
  for (const item of customerProducts.value) {
    const key = customerProductLabelKey(item);
    counts.set(key, (counts.get(key) || 0) + 1);
  }
  return new Set(Array.from(counts.entries()).filter(([, count]) => count > 1).map(([key]) => key));
});

const customerProductOptions = computed(() => customerProducts.value.map((item) => {
  const baseLabel = `${item.customer || '-'} / ${item.productType || '-'} / ${item.sizeMm || '-'}`;
  const label = duplicateCustomerProductKeys.value.has(customerProductLabelKey(item)) && item.sourceRow
    ? `${baseLabel} / 来源行${item.sourceRow}`
    : baseLabel;
  return {
    label,
    value: customerProductKey(item),
  };
}));

const templateOptions = computed(() => templateRows.value.map((item) => ({
  label: `${item.labelName || `模板${item.id}`}${item.templateSource ? ` / ${item.templateSource}` : ''}${
    item.widthMm && item.heightMm ? ` / ${item.widthMm}×${item.heightMm}mm` : ''
  }${item.dpi ? ` / ${item.dpi}dpi` : ''}`,
  value: Number(item.id),
})));

const previewItemOptions = computed(() => (preparedPrintPayload.value?.items || []).map((item) => ({
  label: `${item.sliceBatchNo || '-'}${item.segmentBatchNo ? ` / ${item.segmentBatchNo}` : ''}`,
  value: item.sliceBatchNo,
})));

const selectedPreviewItem = computed(() => (preparedPrintPayload.value?.items || []).find(
  (item) => item.sliceBatchNo === selectedPreviewSliceBatchNo.value,
));

const selectedVariableChecks = computed(() => selectedPreviewItem.value?.variableChecks || []);
const previewErrorCount = computed(() => Number(preparedPrintPayload.value?.errorCount || 0));
const previewWarningCount = computed(() => Number(preparedPrintPayload.value?.warningCount || 0));
const previewReady = computed(() => Boolean(
  preparedPrintPayload.value
  && previewImageUrl.value
  && preparedFingerprint.value === buildCurrentFingerprint(),
));

const variableCheckColumns: TableColumnsType<VariableCheck> = [
  { dataIndex: 'fieldLabel', key: 'fieldLabel', title: '变量', width: 150 },
  { dataIndex: 'value', key: 'value', title: '实际值', width: 210 },
  { dataIndex: 'source', key: 'source', title: '来源', width: 130 },
  { dataIndex: 'status', key: 'status', title: '状态', width: 90 },
  { dataIndex: 'message', key: 'message', title: '说明', width: 180 },
];

onMounted(async () => {
  await loadSegments();
  await checkPrintAgentHealth(true);
});

onBeforeUnmount(() => {
  if (customerSearchTimer) {
    window.clearTimeout(customerSearchTimer);
  }
  revokePreviewImageUrl();
});

watch(selectedCustomerProductKey, (value, oldValue) => {
  if (value !== oldValue) {
    selectedLabelKind.value = undefined;
    selectedDesignId.value = undefined;
    templateRows.value = [];
    invalidatePrintPreview();
  }
});

watch(selectedLabelKind, async (value, oldValue) => {
  if (value === oldValue) return;
  selectedDesignId.value = undefined;
  invalidatePrintPreview();
  await loadTemplates();
});

watch(selectedDesignId, () => {
  invalidatePrintPreview();
});

watch(selectedPrintTarget, (value) => {
  const printer = localPrinters.value.find((item) => item.name === value);
  if (printer?.dpi) selectedPrinterDpi.value = printer.dpi;
  invalidatePrintPreview();
});

watch(selectedPrinterDpi, () => {
  invalidatePrintPreview();
});

async function loadSegments() {
  segmentLoading.value = true;
  try {
    const page = await getTestOuterWaitSegmentPage({
      keyword: segmentKeyword.value,
      pageNo: segmentPagination.current,
      pageSize: segmentPagination.pageSize,
    });
    segmentRows.value = page.list || [];
    segmentPagination.total = Number(page.total || 0);
    const selectedInCurrentPage = segmentRows.value.some(
      (item) => item.segmentBatchNo === selectedSegment.value?.segmentBatchNo,
    );
    if ((!selectedSegment.value || !selectedInCurrentPage) && segmentRows.value.length > 0) {
      await selectSegment(segmentRows.value[0]);
    } else if (segmentRows.value.length === 0) {
      pieceRequestSequence += 1;
      selectedSegment.value = undefined;
      pieceRows.value = [];
      pieceLoading.value = false;
      selectedPieceKeys.value = [];
      selectedPieces.value = [];
    }
  } catch (error) {
    message.error(getErrorMessage(error, '加载合格待包装段失败'));
  } finally {
    segmentLoading.value = false;
  }
}

async function loadPieces(segmentBatchNo = selectedSegment.value?.segmentBatchNo) {
  const requestSequence = ++pieceRequestSequence;
  if (!segmentBatchNo) {
    pieceRows.value = [];
    pieceLoading.value = false;
    selectedPieceKeys.value = [];
    selectedPieces.value = [];
    return;
  }
  pieceLoading.value = true;
  try {
    const rows = await getTestOuterWaitSegmentPieceList({
      segmentBatchNo,
    });
    if (
      requestSequence !== pieceRequestSequence
      || selectedSegment.value?.segmentBatchNo !== segmentBatchNo
    ) return;
    pieceRows.value = rows;
  } catch (error) {
    if (requestSequence === pieceRequestSequence) {
      message.error(getErrorMessage(error, '加载片号失败'));
    }
  } finally {
    if (requestSequence === pieceRequestSequence) {
      pieceLoading.value = false;
    }
  }
}

async function selectSegment(row: WaitSegment) {
  selectedSegment.value = row;
  selectedPieceKeys.value = [];
  selectedPieces.value = [];
  invalidatePrintPreview();
  await loadPieces(row.segmentBatchNo);
}

async function handleSearch() {
  segmentPagination.current = 1;
  selectedSegment.value = undefined;
  await loadSegments();
}

async function handleReset() {
  segmentKeyword.value = '';
  await handleSearch();
}

async function handleSegmentChange(pagination: TablePaginationConfig) {
  segmentPagination.current = pagination.current || 1;
  segmentPagination.pageSize = pagination.pageSize || 20;
  await loadSegments();
}

async function openPrintModal() {
  if (selectedPieces.value.length === 0) {
    message.warning('请选择片号');
    return;
  }
  invalidatePrintPreview();
  printModalOpen.value = true;
  await loadCustomerProducts();
  if (selectedCustomerProduct.value && selectedLabelKind.value) {
    await loadTemplates();
  }
}

async function loadCustomerProducts() {
  customerLoading.value = true;
  try {
    const page = await getTestOuterCustomerProductPage({
      keyword: customerKeyword.value,
      pageNo: 1,
      pageSize: 80,
    });
    customerProducts.value = page.list || [];
  } catch (error) {
    message.error(getErrorMessage(error, '加载客户产品失败'));
  } finally {
    customerLoading.value = false;
  }
}

function handleCustomerProductSearch(value: string) {
  customerKeyword.value = value;
  if (customerSearchTimer) {
    window.clearTimeout(customerSearchTimer);
  }
  customerSearchTimer = window.setTimeout(() => {
    loadCustomerProducts();
  }, 250);
}

async function handleCustomerProductDropdown(open: boolean) {
  if (open && customerProducts.value.length === 0) {
    await loadCustomerProducts();
  }
}

async function loadTemplates() {
  const product = selectedCustomerProduct.value;
  const labelKind = selectedLabelKind.value;
  const requestSequence = ++templateRequestSequence;
  if (!product || !labelKind) {
    templateRows.value = [];
    selectedDesignId.value = undefined;
    return;
  }
  templateLoading.value = true;
  try {
    const rows = await getTestOuterTemplateList({
      customerInfoId: product.customerInfoId,
      labelKind,
      productItemId: product.productItemId,
    });
    if (requestSequence !== templateRequestSequence) return;
    templateRows.value = (rows || []).filter((item) => Number(item.id || 0) > 0);
    if (!templateRows.value.some((item) => item.id === selectedDesignId.value)) {
      selectedDesignId.value = undefined;
    }
    if (templateRows.value.length === 0) {
      message.warning(`当前客户产品没有可用的${labelKindText(labelKind)}模板`);
    }
  } catch (error) {
    if (requestSequence !== templateRequestSequence) return;
    templateRows.value = [];
    selectedDesignId.value = undefined;
    message.error(getErrorMessage(error, '加载打印模板失败'));
  } finally {
    if (requestSequence === templateRequestSequence) {
      templateLoading.value = false;
    }
  }
}

async function checkPrintAgentHealth(silent = false) {
  printAgentStatus.value = 'checking';
  const controller = new AbortController();
  const timer = window.setTimeout(() => controller.abort(), 2500);
  try {
    const response = await fetch(`${PRINT_AGENT_URL}/health`, { signal: controller.signal });
    const result = await response.json();
    if (!response.ok || result?.success === false) {
      throw new Error(result?.message || '本地打印服务未响应');
    }
    const schemaVersion = Number(result?.schemaVersion || 0);
    if (schemaVersion < 7) {
      throw new Error(`本地打印代理版本过低（Schema ${schemaVersion || '未知'}），请升级到Schema 7`);
    }
    printAgentStatus.value = 'online';
    printAgentSchemaVersion.value = schemaVersion;
    printAgentError.value = '';
    await loadLocalPrinters(true);
    if (!silent) {
      message.success('本地打印服务已连接');
    }
    return true;
  } catch (error) {
    printAgentStatus.value = 'offline';
    printAgentSchemaVersion.value = 0;
    printAgentError.value = error instanceof Error ? error.message : '本地打印服务未启动';
    localPrinters.value = [];
    if (!silent) {
      message.warning('未检测到本地打印服务');
    }
    return false;
  } finally {
    window.clearTimeout(timer);
  }
}

async function loadLocalPrinters(silent = false) {
  printerLoading.value = true;
  try {
    const response = await fetch(`${PRINT_AGENT_URL}/printers`);
    const result = await response.json();
    if (!response.ok || result?.success === false) {
      throw new Error(result?.message || '读取本地打印机失败');
    }
    printAgentStatus.value = 'online';
    localPrinters.value = (Array.isArray(result.printers) ? result.printers : [])
      .map((item: LocalPrinter) => ({
        dpi: Number(item.dpi || 0) || undefined,
        dpiSource: item.dpiSource,
        isDefault: Boolean(item.isDefault),
        name: String(item.name || '').trim(),
      }))
      .filter((item: LocalPrinter) => item.name);
    const defaultPrinter = localPrinters.value.find((item) => item.isDefault)?.name || localPrinters.value[0]?.name;
    if (selectedPrintTarget.value === FILE_PRINT_TARGET && defaultPrinter) {
      selectedPrintTarget.value = defaultPrinter;
    }
    if (!silent) {
      message.success(`已读取本地打印机：${localPrinters.value.length} 台`);
    }
  } catch (error) {
    printAgentStatus.value = 'offline';
    printAgentError.value = error instanceof Error ? error.message : '读取本地打印机失败';
    if (!silent) {
      message.warning(printAgentError.value);
    }
  } finally {
    printerLoading.value = false;
  }
}

function revokePreviewImageUrl() {
  if (previewImageUrl.value) {
    URL.revokeObjectURL(previewImageUrl.value);
    previewImageUrl.value = '';
  }
}

function invalidatePrintPreview() {
  revokePreviewImageUrl();
  preparedPrintPayload.value = undefined;
  preparedFingerprint.value = '';
  selectedPreviewSliceBatchNo.value = undefined;
  previewError.value = '';
}

function buildCurrentFingerprint() {
  return JSON.stringify({
    customerProductKey: selectedCustomerProductKey.value || '',
    designId: selectedDesignId.value || 0,
    labelKind: selectedLabelKind.value || '',
    pieces: selectedPieces.value.map((item) => item.sliceBatchNo),
    printerDpi: selectedPrinterDpi.value,
    printTarget: selectedPrintTarget.value,
  });
}

function payloadDataSignature(payload?: PrintPayload) {
  return JSON.stringify({
    designId: payload?.design?.id,
    items: (payload?.items || []).map((item) => ({
      data: item.data,
      segmentBatchNo: item.segmentBatchNo,
      sliceBatchNo: item.sliceBatchNo,
      variableChecks: item.variableChecks,
    })),
    rendererTemplate: payload?.rendererTemplate,
  });
}

async function requestPrintPayload() {
  const product = selectedCustomerProduct.value;
  const labelKind = selectedLabelKind.value;
  const designId = selectedDesignId.value;
  if (!product) throw new Error('请选择客户产品');
  if (!labelKind) throw new Error('请选择打印类型');
  if (!designId) throw new Error('请选择具体打印模板');
  if (selectedPieces.value.length === 0) throw new Error('请选择片号');
  return buildTestOuterPrintPayload({
    customerInfoId: product.customerInfoId,
    designId,
    labelKind,
    productItemId: product.productItemId,
    sliceBatchNos: selectedPieces.value.map((item) => item.sliceBatchNo).join(','),
  });
}

async function preparePrintPreview() {
  previewLoading.value = true;
  previewError.value = '';
  revokePreviewImageUrl();
  try {
    const agentReady = await checkPrintAgentHealth(true);
    if (!agentReady) {
      throw new Error(printAgentError.value || '本地打印服务未启动或版本不正确');
    }
    const payload = await requestPrintPayload();
    if (!payload.rendererTemplate || !(payload.items || []).length) {
      throw new Error('打印负载为空');
    }
    preparedPrintPayload.value = payload;
    preparedFingerprint.value = buildCurrentFingerprint();
    selectedPreviewSliceBatchNo.value = payload.items?.[0]?.sliceBatchNo;
    await renderSelectedPreview();
    if (previewErrorCount.value > 0) {
      message.warning(`变量预检发现 ${previewErrorCount.value} 个错误，请修正后重新预检`);
    } else {
      message.success(`变量预检完成${previewWarningCount.value ? `，有${previewWarningCount.value}个可选空值` : ''}`);
    }
  } catch (error) {
    previewError.value = getErrorMessage(error, '生成变量预检和Python预览失败');
    message.error(previewError.value);
  } finally {
    previewLoading.value = false;
  }
}

async function renderSelectedPreview() {
  const payload = preparedPrintPayload.value;
  const item = selectedPreviewItem.value || payload?.items?.[0];
  if (!payload?.rendererTemplate || !item) {
    throw new Error('没有可预览的打印数据');
  }
  revokePreviewImageUrl();
  const outputMode = selectedPrintTarget.value === FILE_PRINT_TARGET ? 'file' : 'windows_raw';
  const response = await fetch(`${PRINT_AGENT_URL}/render`, {
    body: JSON.stringify({
      data: item.data,
      printerDpi: outputMode === 'windows_raw' ? selectedPrinterDpi.value : undefined,
      rendererTemplate: payload.rendererTemplate,
      requestId: `PREVIEW-${item.sliceBatchNo || Date.now()}`,
      templateName: item.templateName,
    }),
    headers: { 'Content-Type': 'application/json' },
    method: 'POST',
  });
  if (!response.ok) {
    const result = await response.json().catch(() => undefined);
    throw new Error(result?.message || 'Python最终预览生成失败');
  }
  const imageBlob = await response.blob();
  previewImageUrl.value = URL.createObjectURL(imageBlob);
}

async function handlePreviewPieceChange() {
  previewLoading.value = true;
  previewError.value = '';
  try {
    await renderSelectedPreview();
  } catch (error) {
    previewError.value = getErrorMessage(error, '切换片号预览失败');
    message.error(previewError.value);
  } finally {
    previewLoading.value = false;
  }
}

async function executePrint() {
  const product = selectedCustomerProduct.value;
  if (!product) {
    message.warning('请选择客户产品');
    return;
  }
  if (!selectedLabelKind.value) {
    message.warning('请选择打印类型');
    return;
  }
  if (!selectedDesignId.value) {
    message.warning('请选择具体打印模板');
    return;
  }
  if (!previewReady.value) {
    message.warning('请先生成变量预检和Python最终预览');
    return;
  }
  if (previewErrorCount.value > 0) {
    message.error('变量预检存在错误，禁止打印');
    return;
  }
  const labelKind = selectedLabelKind.value;
  if (selectedPrintTarget.value !== FILE_PRINT_TARGET && !selectedPrintTarget.value) {
    message.warning('请选择打印机');
    return;
  }
  printLoading.value = true;
  try {
    const payload = await requestPrintPayload();
    const items = payload.items || [];
    if (!payload.rendererTemplate || items.length === 0) {
      throw new Error('打印负载为空');
    }
    if (Number(payload.errorCount || 0) > 0) {
      preparedPrintPayload.value = payload;
      throw new Error('打印前复核发现必填变量缺失，请重新生成预检和预览');
    }
    if (payloadDataSignature(payload) !== payloadDataSignature(preparedPrintPayload.value)) {
      preparedPrintPayload.value = payload;
      preparedFingerprint.value = '';
      revokePreviewImageUrl();
      throw new Error('片号或模板变量已发生变化，请重新生成预检和预览');
    }
    const outputMode = selectedPrintTarget.value === FILE_PRINT_TARGET ? 'file' : 'windows_raw';
    for (const item of items) {
      const response = await fetch(`${PRINT_AGENT_URL}/print`, {
        body: JSON.stringify({
          copies: 1,
          data: item.data,
          meta: {
            customer: product.customer,
            imageFile: payload.design?.imageFile,
            imageId: payload.design?.imageId,
            productType: product.productType,
            sizeMm: product.sizeMm,
          },
          outputMode,
          printerDpi: outputMode === 'windows_raw' ? selectedPrinterDpi.value : undefined,
          printerName: outputMode === 'windows_raw' ? selectedPrintTarget.value : undefined,
          rendererTemplate: payload.rendererTemplate,
          requestId: item.requestId,
          templateName: item.templateName,
        }),
        headers: { 'Content-Type': 'application/json' },
        method: 'POST',
      });
      const result = await response.json();
      if (!response.ok || result?.success === false) {
        throw new Error(`${item.sliceBatchNo || ''} ${result?.message || '打印失败'}`.trim());
      }
    }
    message.success(`已发送 ${items.length} 张${labelKindText(labelKind)}标签`);
    printModalOpen.value = false;
    invalidatePrintPreview();
  } catch (error) {
    message.error(getErrorMessage(error, '打印失败，请确认本地打印服务已启动'));
  } finally {
    printLoading.value = false;
  }
}

function customerProductKey(item: CustomerProduct) {
  return `${item.customerInfoId}-${item.productItemId}`;
}

function customerProductLabelKey(item: CustomerProduct) {
  return `${item.customer || ''}|${item.productType || ''}|${item.sizeMm || ''}`;
}

function labelKindText(value: string) {
  return labelKindOptions.find((item) => item.value === value)?.label || value;
}

function getErrorMessage(error: unknown, fallback: string) {
  if (error instanceof Error && error.message) {
    return error.message;
  }
  if (typeof error === 'object' && error && 'message' in error) {
    return String((error as { message?: string }).message || fallback);
  }
  return fallback;
}

function segmentRowClassName(row: WaitSegment) {
  return row.segmentBatchNo === selectedSegment.value?.segmentBatchNo ? 'test-outer-print__row--active' : '';
}

function buildSegmentRowEvents(row: WaitSegment) {
  return {
    onClick: () => selectSegment(row),
  };
}
</script>

<template>
  <Page auto-content-height>
    <div class="test-outer-print">
      <div class="test-outer-print__toolbar">
        <div class="test-outer-print__title">
          <IconifyIcon icon="lucide:package-check" />
          <span>测试外包装打印</span>
          <Tag color="green">合格待包装</Tag>
          <Tag :color="printAgentStatus === 'online' ? 'blue' : 'default'">
            {{ printAgentStatus === 'online' ? `18081已连接 / Schema ${printAgentSchemaVersion}` : '18081未连接' }}
          </Tag>
        </div>
        <div class="test-outer-print__filters">
          <Input
            v-model:value="segmentKeyword"
            allow-clear
            class="test-outer-print__keyword"
            placeholder="段号 / 片号 / 型号 / 料号"
            @press-enter="handleSearch"
          />
          <Button type="primary" @click="handleSearch">
            <template #icon><IconifyIcon icon="lucide:search" /></template>
            查询
          </Button>
          <Button @click="handleReset">
            <template #icon><IconifyIcon icon="lucide:rotate-ccw" /></template>
            重置
          </Button>
          <Tooltip :title="printAgentError || '检测本地打印服务'">
            <Button :loading="printAgentStatus === 'checking'" @click="checkPrintAgentHealth(false)">
              <template #icon><IconifyIcon icon="lucide:wifi" /></template>
            </Button>
          </Tooltip>
          <Button type="primary" :disabled="selectedPieces.length === 0" @click="openPrintModal">
            <template #icon><IconifyIcon icon="lucide:printer" /></template>
            打印
          </Button>
        </div>
      </div>

      <div class="test-outer-print__content">
        <section class="test-outer-print__panel test-outer-print__panel--segments">
          <div class="test-outer-print__panel-title">
            <span>段号筛选</span>
            <Tag>{{ segmentPagination.total || 0 }}</Tag>
          </div>
          <ATable
            :columns="segmentColumns"
            :custom-row="buildSegmentRowEvents"
            :data-source="segmentRows"
            :loading="segmentLoading"
            :pagination="segmentPagination"
            :row-class-name="segmentRowClassName"
            :scroll="{ x: 900, y: 540 }"
            row-key="segmentBatchNo"
            size="small"
            @change="handleSegmentChange"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'packagingQualityStatus'">
                <Tag color="green">{{ record.packagingQualityStatus || 'OK' }}</Tag>
              </template>
              <template v-else-if="column.key === 'productionDateStart'">
                <span>{{ record.productionDateStart || '-' }} ~ {{ record.productionDateEnd || '-' }}</span>
              </template>
            </template>
          </ATable>
        </section>

        <section class="test-outer-print__panel">
          <div class="test-outer-print__panel-title">
            <span>{{ selectedSegment?.segmentBatchNo || '未选择段' }}</span>
            <Tag color="blue">已选 {{ selectedPieces.length }}</Tag>
          </div>
          <ATable
            :columns="pieceColumns"
            :data-source="pieceRows"
            :loading="pieceLoading"
            :pagination="false"
            :row-selection="pieceRowSelection"
            :scroll="{ x: 950, y: 540 }"
            row-key="sliceBatchNo"
            size="small"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'sourceType'">
                <Tag :color="record.sourceType === 'MANUAL_HISTORY' ? 'purple' : 'blue'">
                  {{ record.sourceType === 'MANUAL_HISTORY' ? '历史片' : '裁切' }}
                </Tag>
              </template>
              <template v-else-if="column.key === 'inspectionResult' || column.key === 'coaInspectionResult'">
                <Tag color="green">
                  {{ column.key === 'inspectionResult' ? (record.inspectionResult || 'OK') : (record.coaInspectionResult || 'OK') }}
                </Tag>
              </template>
            </template>
          </ATable>
        </section>
      </div>
    </div>

    <Modal
      v-model:open="printModalOpen"
      :confirm-loading="printLoading"
      :ok-button-props="{ disabled: !previewReady || previewErrorCount > 0 }"
      :width="1180"
      ok-text="打印"
      title="打印外包装标签"
      @ok="executePrint"
    >
      <div class="test-outer-print__modal-form">
        <div class="test-outer-print__form-field">
          <span>客户产品</span>
          <div class="test-outer-print__customer-select">
            <Select
              v-model:value="selectedCustomerProductKey"
              :filter-option="false"
              :loading="customerLoading"
              :options="customerProductOptions"
              allow-clear
              placeholder="客户 / 产品 / 尺寸"
              show-search
              @dropdown-visible-change="handleCustomerProductDropdown"
              @search="handleCustomerProductSearch"
            />
            <Button :loading="customerLoading" @click="loadCustomerProducts">
              <template #icon><IconifyIcon icon="lucide:refresh-cw" /></template>
            </Button>
          </div>
        </div>
        <div class="test-outer-print__form-field">
          <span>打印类型</span>
          <Select
            v-model:value="selectedLabelKind"
            :disabled="!selectedCustomerProduct"
            :options="labelKindOptions"
            placeholder="先选客户产品"
          />
        </div>
        <div class="test-outer-print__form-field test-outer-print__form-field--template">
          <span>打印模板</span>
          <Select
            v-model:value="selectedDesignId"
            :disabled="!selectedLabelKind"
            :loading="templateLoading"
            :options="templateOptions"
            placeholder="请选择具体模板"
            show-search
          />
        </div>
        <div class="test-outer-print__form-field">
          <span>打印机</span>
          <div class="test-outer-print__customer-select">
            <Select v-model:value="selectedPrintTarget" :options="printTargetOptions" />
            <Button :loading="printerLoading" @click="loadLocalPrinters(false)">
              <template #icon><IconifyIcon icon="lucide:refresh-cw" /></template>
            </Button>
          </div>
        </div>
        <div class="test-outer-print__form-field">
          <span>打印DPI</span>
          <Select
            v-model:value="selectedPrinterDpi"
            :disabled="selectedPrintTarget === FILE_PRINT_TARGET"
            :options="[
              { label: '203 dpi', value: 203 },
              { label: '300 dpi', value: 300 },
              { label: '600 dpi', value: 600 },
            ]"
          />
        </div>
        <div class="test-outer-print__form-field test-outer-print__form-field--preview-action">
          <span>打印前校验</span>
          <Button
            type="primary"
            ghost
            :disabled="!selectedDesignId"
            :loading="previewLoading"
            @click="preparePrintPreview"
          >
            <template #icon><IconifyIcon icon="lucide:scan-eye" /></template>
            变量预检 + Python预览
          </Button>
        </div>
      </div>

      <Alert
        v-if="previewError"
        class="mt-4"
        :message="previewError"
        show-icon
        type="error"
      />

      <div v-if="preparedPrintPayload" class="test-outer-print__preview-summary">
        <Alert
          :message="previewErrorCount > 0
            ? `预检失败：${previewErrorCount}个错误、${previewWarningCount}个提醒，禁止打印`
            : `预检通过：${previewWarningCount}个可选空值提醒，可执行打印`"
          :type="previewErrorCount > 0 ? 'error' : (previewWarningCount > 0 ? 'warning' : 'success')"
          show-icon
        />
        <Select
          v-model:value="selectedPreviewSliceBatchNo"
          :options="previewItemOptions"
          class="test-outer-print__preview-piece"
          placeholder="选择片号查看变量和最终效果"
          @change="handlePreviewPieceChange"
        />
      </div>

      <div v-if="preparedPrintPayload" class="test-outer-print__preview-grid">
        <div class="test-outer-print__variable-panel">
          <div class="test-outer-print__preview-title">变量预检</div>
          <ATable
            :columns="variableCheckColumns"
            :data-source="selectedVariableChecks"
            :pagination="false"
            :scroll="{ x: 760, y: 300 }"
            row-key="fieldKey"
            size="small"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'fieldLabel'">
                <span>{{ record.fieldLabel || record.fieldKey }}</span>
                <Tag v-if="record.required" color="red" class="ml-1">必填</Tag>
              </template>
              <template v-else-if="column.key === 'value'">
                <span :class="{ 'test-outer-print__empty-value': record.value === '' || record.value == null }">
                  {{ record.value === '' || record.value == null ? '（空）' : String(record.value) }}
                </span>
              </template>
              <template v-else-if="column.key === 'status'">
                <Tag :color="record.status === 'ERROR' ? 'red' : (record.status === 'WARNING' ? 'orange' : 'green')">
                  {{ record.status === 'ERROR' ? '错误' : (record.status === 'WARNING' ? '提醒' : '正常') }}
                </Tag>
              </template>
            </template>
          </ATable>
        </div>
        <div class="test-outer-print__python-preview">
          <div class="test-outer-print__preview-title">Python最终打印效果</div>
          <div class="test-outer-print__preview-image-wrap">
            <img v-if="previewImageUrl" :src="previewImageUrl" alt="Python最终打印预览" />
            <span v-else>{{ previewLoading ? '正在生成Python最终预览…' : '尚未生成预览' }}</span>
          </div>
        </div>
      </div>

      <ATable
        :columns="selectedPieceColumns"
        :data-source="selectedPieces"
        :pagination="false"
        :scroll="{ x: 650, y: 220 }"
        class="mt-4"
        row-key="sliceBatchNo"
        size="small"
      />
    </Modal>
  </Page>
</template>

<style scoped>
.test-outer-print {
  display: flex;
  flex-direction: column;
  gap: 12px;
  height: 100%;
  min-height: 0;
}

.test-outer-print__toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 16px;
  background: hsl(var(--background));
  border: 1px solid hsl(var(--border));
  border-radius: 8px;
}

.test-outer-print__title,
.test-outer-print__filters {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.test-outer-print__title {
  font-size: 16px;
  font-weight: 600;
  white-space: nowrap;
}

.test-outer-print__keyword {
  width: 260px;
}

.test-outer-print__content {
  display: grid;
  grid-template-columns: minmax(520px, 0.95fr) minmax(560px, 1.05fr);
  gap: 12px;
  min-height: 0;
  flex: 1;
}

.test-outer-print__panel {
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
  padding: 12px;
  background: hsl(var(--background));
  border: 1px solid hsl(var(--border));
  border-radius: 8px;
}

.test-outer-print__panel-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 10px;
  font-size: 14px;
  font-weight: 600;
}

.test-outer-print__modal-form {
  display: grid;
  grid-template-columns: minmax(280px, 2fr) minmax(150px, 1fr) minmax(300px, 2fr);
  gap: 12px;
}

.test-outer-print__form-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 13px;
  font-weight: 500;
}

.test-outer-print__customer-select {
  display: flex;
  gap: 6px;
}

.test-outer-print__customer-select :deep(.ant-select),
.test-outer-print__customer-select :deep(.ant-input) {
  flex: 1;
}

.test-outer-print__preview-summary {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 300px;
  gap: 12px;
  align-items: center;
  margin-top: 16px;
}

.test-outer-print__preview-piece {
  width: 100%;
}

.test-outer-print__preview-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.45fr) minmax(320px, 0.75fr);
  gap: 12px;
  margin-top: 12px;
}

.test-outer-print__variable-panel,
.test-outer-print__python-preview {
  min-width: 0;
  padding: 10px;
  border: 1px solid hsl(var(--border));
  border-radius: 8px;
}

.test-outer-print__preview-title {
  margin-bottom: 8px;
  font-size: 13px;
  font-weight: 600;
}

.test-outer-print__preview-image-wrap {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 280px;
  max-height: 360px;
  padding: 10px;
  overflow: auto;
  color: hsl(var(--muted-foreground));
  background: #f5f7fa;
  border-radius: 6px;
}

.test-outer-print__preview-image-wrap img {
  display: block;
  max-width: 100%;
  max-height: 330px;
  object-fit: contain;
  background: #fff;
  box-shadow: 0 2px 10px rgb(0 0 0 / 12%);
}

.test-outer-print__empty-value {
  color: #d46b08;
  font-style: italic;
}

:deep(.test-outer-print__row--active td) {
  background: #e6f4ff !important;
}

:deep(.test-outer-print__panel--segments .ant-table-tbody > tr) {
  cursor: pointer;
}

@media (max-width: 1180px) {
  .test-outer-print__toolbar,
  .test-outer-print__filters {
    flex-wrap: wrap;
  }

  .test-outer-print__content {
    grid-template-columns: 1fr;
  }

  .test-outer-print__modal-form {
    grid-template-columns: 1fr;
  }

  .test-outer-print__preview-summary,
  .test-outer-print__preview-grid {
    grid-template-columns: 1fr;
  }
}
</style>
