<script lang="ts" setup>
import { computed, nextTick, onActivated, onBeforeUnmount, onDeactivated, onMounted, reactive, ref, watch } from 'vue';

import { useAccess } from '@vben/access';
import { Page } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';
import { useUserStore } from '@vben/stores';
import { downloadFileFromBlobPart } from '@vben/utils';
import { useQRCode } from '@vueuse/integrations/useQRCode';

import {
  Badge,
  Button,
  DatePicker,
  Form,
  FormItem,
  Input,
  InputNumber,
  Modal as AModal,
  Pagination,
  Radio,
  RadioGroup,
  Select,
  Table as ATable,
  Tag,
  Tabs,
  TabPane,
  message,
} from 'ant-design-vue';
import dayjs from 'dayjs';
import { useRoute } from 'vue-router';

import {
  correctSlittingConsoleSliceAbnormalCategory,
  completeSlittingConsoleSource,
  confirmSlittingConsoleSlice,
  deleteSlittingConsoleSlice,
  generateSlittingConsoleSlices,
  getSlittingConsoleSliceList,
  getSlittingConsoleSourceList,
  getSlittingConsoleTaskList,
  markSlittingConsoleSlicesPrinted,
  type MesHcSlittingConsoleApi,
} from '#/api/mes/hc/execution/slitting-console';
import { getPlanOrderDetail } from '#/api/mes/hc/planorder';
import {
  getDownstreamPreProcessFeedbackReason,
  getDownstreamPreProcessFeedbackTitle,
  hasDownstreamPreProcessFeedback,
} from '../shared/preProcessSelfCheckAttribution';
import { useExecutionFullscreenClock } from '../shared/useExecutionFullscreenClock';
import {
  buildTransferTicketQrValue,
  buildWorkOrderTicketHtml,
  resolveTransferTicketQrBusinessNo,
} from '../shared/workOrderTicketPrint';
import { buildSlittingTransferTicketPayload as buildSharedSlittingTransferTicketPayload } from '../shared/slittingTransferTicketPrint';
import {
  ProductionInstructionMessageTab,
  type ProductionInstructionContext,
} from '#/views/mes/hc/shared/production-instruction';
import { getProductionInstructionUnreadCount } from '#/api/mes/hc/production-instruction';
import AuthModal from '#/views/mes/work-order-booking/modules/components/AuthModal.vue';
import {
  buildSegmentChainSampleLockCandidates,
  ensureProcessingSampleAbnormalUnlocked as ensureSampleAbnormalUnlocked,
  findActiveProcessingSampleAbnormalLock as findActiveSampleAbnormalLock,
} from '../shared/sampleAbnormalLockGuard';

defineOptions({ name: 'MesExecutionSlittingConsolePrototype' });

const SLITTING_PRINT_AGENT_URL = 'http://127.0.0.1:17820';
const TASK_LIST_DEFAULT_PAGE_SIZE = 10;
const STANDARD_SLICE_LENGTH = 0.8;
const ABNORMAL_CATEGORY_CORRECT_PERMISSION = 'mes:sfc:slitting-report:correct-abnormal-category';
const SLITTING_SEGMENT_MARKS = ['P', 'Q', 'R', 'S'] as const;
type SlittingSegmentMark = (typeof SLITTING_SEGMENT_MARKS)[number];
type SlittingSizeCode = '740' | '775' | 'UNKNOWN';
const SLITTING_SIZE_OPTIONS: Array<{ label: string; value: SlittingSizeCode }> = [
  { label: '775mm / 尾号A', value: '775' },
  { label: '740mm / 尾号B', value: '740' },
  { label: '不确定 / 不加尾号', value: 'UNKNOWN' },
];

interface CurrentPlan {
  availableSourceLength: number;
  batchNo: string;
  equipmentCode: string;
  equipmentId?: number;
  equipmentName: string;
  materialCode: string;
  modelCode: string;
  planSizeSpec: string;
  planId?: number;
  planNo: string;
  planOperationId?: number;
  requirements: string;
  sourceBatchNo: string;
  sourceProductionBatchNo: string;
  status: string;
  workCenterName: string;
}

interface VisualItem {
  itemName: string;
  remark: string;
  result: 'NG' | 'OK';
}

interface SliceDetailExcelColumn {
  getValue: (row: MesHcSlittingConsoleApi.SliceItem) => unknown;
  title: string;
}

const route = useRoute();
const { hasAccessByCodes } = useAccess();
const boardLoading = ref(false);
const currentDateTime = ref(dayjs().format('YYYY-MM-DD HH:mm:ss'));
let timer: ReturnType<typeof setInterval> | null = null;
let lastSliceAdjustRemark = '';
let skipInitialActivation = true;

const currentPlan = reactive<CurrentPlan>({
  availableSourceLength: 0,
  batchNo: '',
  equipmentCode: '',
  equipmentName: '',
  materialCode: '',
  modelCode: '',
  planSizeSpec: '',
  planNo: '',
  requirements: '',
  sourceBatchNo: '',
  sourceProductionBatchNo: '',
  status: '',
  workCenterName: '',
});

const productionInstructionUnreadCount = ref(0);
const productionInstructionContext = computed<ProductionInstructionContext>(() => ({
  operationCode: 'WC-SLIT',
  operationName: '分切',
  processCode: 'WC-SLIT',
  processName: '分切',
}));
async function refreshProductionInstructionUnreadCount() {
  const context = productionInstructionContext.value;
  productionInstructionUnreadCount.value = Number(await getProductionInstructionUnreadCount({
    operationCode: context.operationCode,
    operationName: context.operationName,
    pageNo: 1,
    pageSize: 1,
    processCode: context.processCode,
    processName: context.processName,
  }) || 0);
}
const handleProductionInstructionUnreadChange = (count: number) => {
  productionInstructionUnreadCount.value = Number(count || 0);
};
watch(
  () => [
    productionInstructionContext.value.operationCode,
    productionInstructionContext.value.operationName,
    productionInstructionContext.value.processCode,
    productionInstructionContext.value.processName,
  ],
  () => {
    void refreshProductionInstructionUnreadCount();
  },
  { immediate: true },
);

const scanPlanNo = ref('');
const scanError = ref('');
const planScanInputRef = ref<any>();
const sliceScanLookupInputRef = ref<any>();
let planScanTimer: ReturnType<typeof setTimeout> | null = null;
let pendingScannerPlanNo = '';
let pendingScannerSegmentBatchNo = '';
let globalScannerBuffer = '';
let globalScannerLastAt = 0;
let globalScannerTimer: ReturnType<typeof setTimeout> | null = null;
let globalScannerListenerAttached = false;
const PLAN_SCAN_SEPARATOR_REGEXP = /[，,；;]/;
const GLOBAL_SCANNER_MAX_GAP_MS = 80;
const GLOBAL_SCANNER_IDLE_FLUSH_MS = 160;
const PLAN_SCAN_MIN_LENGTH = 6;
const hasPlanScanDelimiter = (value?: string) => PLAN_SCAN_SEPARATOR_REGEXP.test(value || '');
const parsePlanScanValue = (value?: string) => {
  const text = String(value || '').trim();
  const separatorIndex = text.search(PLAN_SCAN_SEPARATOR_REGEXP);
  const planNo = separatorIndex >= 0 ? text.slice(0, separatorIndex).trim() : text;
  const segmentBatchNo = separatorIndex >= 0 ? normalizeReportBatchNo(text.slice(separatorIndex + 1)) : '';
  return { planNo, segmentBatchNo };
};
const normalizePlanScanNo = (value?: string) => parsePlanScanValue(value).planNo;
const normalizeConfirmScanCode = (value?: string) => {
  const text = String(value || '').trim();
  const separatorIndex = text.search(PLAN_SCAN_SEPARATOR_REGEXP);
  return separatorIndex >= 0 ? text.slice(separatorIndex + 1).trim() || text : text;
};
const syncPlanScanNo = (value?: string) => {
  const scannerInput = hasPlanScanDelimiter(value);
  const { planNo, segmentBatchNo } = parsePlanScanValue(value);
  if (scannerInput) {
    pendingScannerPlanNo = planNo;
    pendingScannerSegmentBatchNo = segmentBatchNo;
  }
  if (String(value || '').trim() !== planNo && scanPlanNo.value !== planNo) {
    scanPlanNo.value = planNo;
  }
  return planNo;
};
const activeBoardTab = ref('VISUAL');
const sources = ref<MesHcSlittingConsoleApi.SourceItem[]>([]);
const slices = ref<MesHcSlittingConsoleApi.SliceItem[]>([]);
const upstreamSampleWarning = computed(() => [...new Set(
  slices.value.map((slice) => slice.upstreamSampleLockReason).filter(Boolean),
)].join('；'));

const workListVisible = ref(false);
const workListLoading = ref(false);
type WorkSourceRow = MesHcSlittingConsoleApi.TaskItem &
  MesHcSlittingConsoleApi.SourceItem & {
    operationTaskStatus?: string;
    sampleAbnormalLocked?: boolean;
    sourceEndPosition: number;
    sourceKey: string;
    sourcePositionText: string;
    sourceStartPosition: number;
  };

const workListRows = ref<WorkSourceRow[]>([]);
const workListPage = ref(1);
const workListPageSize = ref(TASK_LIST_DEFAULT_PAGE_SIZE);
const workListFilters = reactive({
  batchNo: '',
  modelCode: '',
  planNo: '',
  status: 'UNFINISHED',
});

const generateVisible = ref(false);
const generateMode = ref<'AUTO' | 'MANUAL'>('AUTO');
const activeSource = ref<MesHcSlittingConsoleApi.SourceItem | null>(null);
const generateAuthVisible = ref(false);
const generateAuthExecuting = ref(false);
const pendingGeneratePayload = ref<MesHcSlittingConsoleApi.SliceGenerateReq | null>(null);
const sourceCompleteAuthVisible = ref(false);
const pendingSourceComplete = ref<{
  disposeRemainingTail: boolean;
  segmentBatchNo: string;
  sourceAdhesiveReportId: number;
  tailDisposalLength: number;
} | null>(null);
const generateForm = reactive<{
  segmentLength: number;
  sizeCode: SlittingSizeCode;
  sliceCount: number;
  startSerialNo?: number;
}>({
  segmentLength: 1,
  sizeCode: 'UNKNOWN' as SlittingSizeCode,
  sliceCount: 1,
  startSerialNo: undefined,
});
const generateError = ref('');

const confirmVisible = ref(false);
const sliceScanVisible = ref(false);
const sliceDialogMode = ref<'confirm' | 'view'>('view');
const activeSlice = ref<MesHcSlittingConsoleApi.SliceItem | null>(null);
const abnormalCategoryCorrectionVisible = ref(false);
const abnormalCategoryCorrectionSaving = ref(false);
const abnormalCategoryCorrectionForm = reactive({
  category: '',
  reason: '',
});
const visualMaximized = ref(false);
const sourceCompleting = ref(false);
const transferPrintSelectionMode = ref(false);
const selectedTransferSliceIds = ref<string[]>([]);
const sliceDetailExporting = ref(false);
const sliceTableWrapRef = ref<HTMLElement | null>(null);
const sliceTableScrollY = ref(360);
let sliceTableResizeObserver: ResizeObserver | null = null;
const confirmForm = reactive({
  originalSliceNo: '',
  remark: '',
  scannedSliceNo: '',
  selfCheck: 'OK' as 'NG' | 'OK',
  sizeCode: 'UNKNOWN' as SlittingSizeCode,
  visualItems: [] as VisualItem[],
});
const sliceScanForm = reactive({
  message: '',
  scanCode: '',
});
const filterForm = reactive({
  recordDate: '',
  scanConfirmDate: '',
  segmentBatchNo: '',
  status: 'ALL',
});

const visualItemNames = ['黑点', '蓝点', '黄点', '红点', '针孔', '条纹', '褶皱', '波浪纹', '其他'];
const abnormalCategoryCorrectionOptions = visualItemNames.map((item) => ({ label: item, value: item }));
const filterStatusOptions = [
  { label: '全部', value: 'ALL' },
  { label: '待确认', value: 'PENDING' },
  { label: '已确认', value: 'CONFIRMED' },
  { label: '已打印', value: 'PRINTED' },
  { label: '异常', value: 'NG' },
];

const userStore = useUserStore();
const oneClickScanConfirmingSourceId = ref<number | null>(null);

const sliceColumns = [
  { dataIndex: 'sliceSerialNo', fixed: 'left' as const, title: '切片流水号', width: 180 },
  { dataIndex: 'sourceProductionBatchNo', title: '所属分段批号', width: 180 },
  { dataIndex: 'slicePositionText', title: '位置(m)', width: 130 },
  ...visualItemNames.map((itemName) => ({
    dataIndex: `visual_${itemName}`,
    title: itemName,
    width: 92,
  })),
  { dataIndex: 'selfCheck', title: '综合判断', width: 110 },
  { dataIndex: 'remark', title: '备注说明', width: 180 },
  { dataIndex: 'printStatus', title: '打印状态', width: 100 },
  { dataIndex: 'scanStatus', title: '确认状态', width: 110 },
  { dataIndex: 'scanConfirmDate', title: '扫码确认日期', width: 130 },
  { dataIndex: 'action', fixed: 'right' as const, title: '操作', width: 220 },
];

const workListBaseColumns = [
  { dataIndex: 'planNo', title: '计划号' },
  { dataIndex: 'modelCode', title: '产品型号' },
  { dataIndex: 'sourceProductionBatchNo', title: '分段批次号' },
  { dataIndex: 'sourcePositionText', title: '位置' },
  { dataIndex: 'availableSourceLength', title: '可加工(m)' },
  { dataIndex: 'status', title: '状态' },
  { dataIndex: 'action', fixed: 'right' as const, title: '操作' },
];
const workListColumnWidthRules: Record<string, { max: number; min: number; padding: number }> = {
  action: { max: 86, min: 72, padding: 24 },
  availableSourceLength: { max: 126, min: 98, padding: 28 },
  modelCode: { max: 220, min: 108, padding: 32 },
  planNo: { max: 260, min: 110, padding: 32 },
  sourcePositionText: { max: 160, min: 92, padding: 28 },
  sourceProductionBatchNo: { max: 320, min: 142, padding: 32 },
  status: { max: 92, min: 78, padding: 24 },
};
const workListColumnTextGetters: Record<string, (row: Record<string, any>) => unknown> = {
  action: (row) => getWorkListActionText(row.status),
  availableSourceLength: (row) => formatNumber(row.availableSourceLength),
  sourceProductionBatchNo: (row) => getSourceSegmentBatchNo(row),
  status: (row) => getWorkListStatusMeta(row).text,
};

const workListStatusOptions = [
  { label: '未完工', value: 'UNFINISHED' },
  { label: '全部', value: 'ALL' },
  { label: '待开工', value: 'PENDING' },
  { label: '生产中', value: 'IN_PROGRESS' },
  { label: '已完工', value: 'COMPLETED' },
  { label: '已暂停', value: 'PAUSED' },
  { label: '已作废', value: 'CANCELLED' },
];

const hasPlan = computed(() => Boolean(currentPlan.planId && currentPlan.planOperationId));
const sourceLengthTotal = computed(() => sources.value.reduce((sum, item) => sum + Number(item.outputLength || 0), 0));
const currentDateText = computed(() => dayjs(currentDateTime.value).format('YYYY-MM-DD'));
const currentTimeText = computed(() => dayjs(currentDateTime.value).format('HH:mm:ss'));
const { showExecutionClock } = useExecutionFullscreenClock();
const isSliceFiltering = computed(
  () => Boolean(filterForm.recordDate)
    || Boolean(filterForm.scanConfirmDate)
    || Boolean(filterForm.segmentBatchNo.trim())
    || filterForm.status !== 'ALL',
);
const filteredSlices = computed(() => slices.value.filter(matchSliceFilter));
const workListFilteredRows = computed(() => {
  const statusFilter = workListFilters.status;
  return workListRows.value.filter((row) => {
    const rowAny = row as Record<string, any>;
    const status = normalizeWorkListStatusValue(rowAny.status);
    if (statusFilter === 'UNFINISHED' && ['COMPLETED', 'CANCELLED'].includes(status)) return false;
    if (statusFilter && statusFilter !== 'ALL' && statusFilter !== 'UNFINISHED' && status !== statusFilter) return false;
    if (!containsWorkListFilterText([rowAny.planNo, rowAny.id, rowAny.sourceKey], workListFilters.planNo)) return false;
    if (!containsWorkListFilterText([rowAny.modelCode, rowAny.modelName, rowAny.productModel, rowAny.materialCode], workListFilters.modelCode)) return false;
    return containsWorkListFilterText(
      [
        rowAny.batchNo,
        rowAny.sourceBatchNo,
        rowAny.parentProductionBatchNo,
        rowAny.sourceProductionBatchNo,
        rowAny.segmentBatchNo,
        rowAny.segmentMark,
        rowAny.productionBatchNo,
        rowAny.adhesiveProductionBatchNo,
      ],
      workListFilters.batchNo,
    );
  });
});
const workListPagedRows = computed(() => {
  const start = (workListPage.value - 1) * workListPageSize.value;
  return workListFilteredRows.value.slice(start, start + workListPageSize.value);
});

function getWorkListTextVisualWidth(value: unknown) {
  const text = String(value ?? '').trim() || '-';
  return Array.from(text).reduce((sum, char) => sum + (char.charCodeAt(0) > 255 ? 14 : 8), 0);
}

function clampWorkListColumnWidth(width: number, min: number, max: number) {
  return Math.min(max, Math.max(min, Math.ceil(width)));
}

function getWorkListColumnWidth(column: (typeof workListBaseColumns)[number]) {
  const dataIndex = String(column.dataIndex);
  const rule = workListColumnWidthRules[dataIndex] || { max: 220, min: 100, padding: 32 };
  const titleWidth = getWorkListTextVisualWidth(column.title) + rule.padding;
  const contentWidth = workListFilteredRows.value.reduce((max, row) => {
    const rowAny = row as Record<string, any>;
    const getter = workListColumnTextGetters[dataIndex];
    return Math.max(max, getWorkListTextVisualWidth(getter ? getter(rowAny) : rowAny[dataIndex]) + rule.padding);
  }, 0);
  return clampWorkListColumnWidth(Math.max(titleWidth, contentWidth), rule.min, rule.max);
}

const workListColumns = computed(() =>
  workListBaseColumns.map((column) => ({
    ...column,
    width: getWorkListColumnWidth(column),
  })),
);

const workListScrollX = computed(() =>
  workListColumns.value.reduce((total, column) => total + Number(column.width || 0), 0),
);
const totalSliceCount = computed(() => filteredSlices.value.length);
const confirmedSliceCount = computed(() => filteredSlices.value.filter((item) => item.scanStatus === 'CONFIRMED').length);
const printedSliceCount = computed(() => filteredSlices.value.filter((item) => item.printStatus === 'PRINTED').length);
const currentSource = computed(() => activeSource.value || sources.value[0] || null);
const currentSourceCompleted = computed(() => normalizeWorkListStatusValue(currentSource.value?.status) === 'COMPLETED');
const currentWorkStatus = computed(() => resolveMergedWorkStatus(currentSource.value));
const isWorkOrderPaused = computed(() => currentWorkStatus.value === 'PAUSED');
const isWorkOrderCancelled = computed(() => currentWorkStatus.value === 'CANCELLED');
const isWorkOrderCompleted = computed(() => currentWorkStatus.value === 'COMPLETED');
const isCurrentWorkReadonly = computed(() => isWorkOrderPaused.value || isWorkOrderCancelled.value || isWorkOrderCompleted.value);
const isCurrentWorkPrintBlocked = computed(() => isPrintBlockedWorkStatus(currentWorkStatus.value));
const workbenchBlockedReason = computed(() => {
  const batchNo =
    getSourceSegmentBatchNo(currentSource.value) ||
    currentPlan.sourceProductionBatchNo ||
    currentPlan.batchNo ||
    currentPlan.sourceBatchNo ||
    '-';
  if (isWorkOrderPaused.value) {
    return `当前分切工序已暂停，计划 ${currentPlan.planNo || '-'}，批号 ${batchNo} 暂不能继续分切或提交操作，请等待生产计划复工指令。`;
  }
  if (isWorkOrderCancelled.value) {
    return `当前分切工序已作废取消，计划 ${currentPlan.planNo || '-'}，批号 ${batchNo} 不能继续分切或提交操作。`;
  }
  return '';
});
const workbenchBlockedTitle = computed(() =>
  isWorkOrderCancelled.value ? '当前分切工序已作废取消' : '当前分切工序已暂停',
);
const visibleSources = computed(() => {
  const source = currentSource.value;
  if (!source) return [];
  if (!isSliceFiltering.value) return [source];
  const rows = getSourceSlices(source.adhesiveReportId);
  if (rows.length === 0) return matchSourceSegmentFilter(source) ? [source] : [];
  return rows.some(matchSliceFilter) ? [source] : [];
});
const visualDisplaySources = computed(() => {
  if (!transferPrintSelectionMode.value) return visibleSources.value;
  return sources.value.filter((source) => {
    const rows = getSourceSlices(source.adhesiveReportId);
    if (rows.length === 0) return false;
    if (isSliceFiltering.value) return rows.some(matchSliceFilter);
    return rows.length > 0;
  });
});
const selectableTransferSlices = computed(() =>
  visualDisplaySources.value
    .flatMap((source) => getVisualSourceSlices(source.adhesiveReportId))
    .filter((item) => !!item?.id && !!item.sliceSerialNo),
);
const selectedTransferSlices = computed(() =>
  selectedTransferSliceIds.value
    .map((id) => slices.value.find((item) => getTransferSliceKey(item) === id))
    .filter(Boolean) as MesHcSlittingConsoleApi.SliceItem[],
);
const selectableTransferSliceCount = computed(() => selectableTransferSlices.value.length);
const selectedTransferSliceCount = computed(() => selectedTransferSlices.value.length);
const allTransferSlicesSelected = computed(
  () =>
    selectableTransferSliceCount.value > 0 &&
    selectableTransferSlices.value.every((item) => selectedTransferSliceIds.value.includes(getTransferSliceKey(item))),
);
const currentSegmentBatchNo = computed(() => {
  const source = currentSource.value;
  return getSourceSegmentBatchNo(source) || normalizeSegmentBatchNo(currentPlan.sourceProductionBatchNo) || '-';
});
const currentSegmentLength = computed(() => Number(currentSource.value?.outputLength || currentPlan.availableSourceLength || 0));
const currentAvailableSourceLength = computed(() => Number(currentSource.value?.availableSourceLength ?? currentPlan.availableSourceLength ?? 0));
const currentSegmentStart = computed(() => {
  const source = currentSource.value;
  if (!source) return 0;
  if (source.sourceStartPosition !== undefined && source.sourceStartPosition !== null) {
    const startPosition = Number(source.sourceStartPosition);
    if (Number.isFinite(startPosition)) return startPosition;
  }
  const motherBatch = resolveMotherBatchNo(source.sourceBatchNo || source.parentProductionBatchNo || '');
  let total = 0;
  for (const item of sources.value) {
    const sameMother = !motherBatch || isSameMotherBatchNo(item.sourceBatchNo, motherBatch) || isSameMotherBatchNo(item.parentProductionBatchNo, motherBatch);
    if (item.adhesiveReportId === source.adhesiveReportId) break;
    if (sameMother) total += Number(item.outputLength || 0);
  }
  return total;
});
const currentSegmentEnd = computed(() => {
  if (currentSource.value?.sourceEndPosition !== undefined && currentSource.value?.sourceEndPosition !== null) {
    const endPosition = Number(currentSource.value.sourceEndPosition);
    if (Number.isFinite(endPosition)) return endPosition;
  }
  return currentSegmentStart.value + currentSegmentLength.value;
});
const currentSegmentRangeText = computed(() => {
  if (!currentSource.value) return '-';
  return `${formatNumber(currentSegmentStart.value)}-${formatNumber(currentSegmentEnd.value)} m`;
});
const sliceDialogTitle = computed(() => (sliceDialogMode.value === 'confirm' ? '切片扫码确认与目视自检' : '切片详情查看'));
const sliceDialogOkText = computed(() => (sliceDialogMode.value === 'confirm' ? '确认入账' : '扫码后才能修改'));
const activeSliceSegmentBatchNo = computed(() => getSliceSegmentBatchNo(activeSlice.value));
const activeSlicePositionText = computed(() => getSlicePositionText(activeSlice.value));
const generateModalTitle = computed(() => (generateMode.value === 'MANUAL' ? '人工切片' : '自动切片'));
const generateAuthActionName = computed(() => (generateMode.value === 'MANUAL' ? '人工分切执行确认' : '自动分切执行确认'));
const generateAuthWorkstation = computed(() => currentPlan.equipmentName || currentPlan.equipmentCode || currentPlan.workCenterName || '分切工位');
const sourceCompleteAuthActionName = computed(() => {
  const segmentBatchNo = pendingSourceComplete.value?.segmentBatchNo || currentSegmentBatchNo.value;
  return `分切本段完成确认（${segmentBatchNo}）`;
});
const generateCurrentSliceCount = computed(() => getSourceSlices(activeSource.value?.adhesiveReportId).length);
const hasActiveVisualItems = computed(() => confirmForm.visualItems.some((item) => isVisualItemActive(item)));
const canCorrectAbnormalCategory = computed(() =>
  sliceDialogMode.value === 'view'
  && !!activeSlice.value?.id
  && hasOwnSliceVisualIssue(activeSlice.value)
  && hasAccessByCodes([ABNORMAL_CATEGORY_CORRECT_PERMISSION]),
);
const canOneClickScanConfirm = computed(() => String(userStore.userInfo?.username || '').trim().toLowerCase() === 'hcadmin');
const previewFirstSliceNo = computed(() => {
  const source = activeSource.value;
  if (!source) return '-';
  const segmentBatchNo = getSourceSegmentBatchNo(source);
  if (!segmentBatchNo) return '-';
  const startSerialNo = Number(generateForm.startSerialNo);
  if (!Number.isInteger(startSerialNo) || startSerialNo <= 0) return '-';
  return buildSlittingSliceNoWithSize(
    `${segmentBatchNo}${String(startSerialNo).padStart(3, '0')}`,
    generateForm.sizeCode,
  );
});
const confirmTargetSliceNo = computed(() => buildSlittingSliceNoWithSize(
  confirmForm.originalSliceNo || activeSlice.value?.sliceSerialNo || '',
  confirmForm.sizeCode,
));
const generateEstimateSliceLength = computed(() =>
  generateMode.value === 'AUTO'
    ? Number(generateForm.segmentLength || STANDARD_SLICE_LENGTH)
    : STANDARD_SLICE_LENGTH,
);
const currentSegmentSequenceNotice = computed(() =>
  buildSlittingSegmentSequenceNotice(currentSource.value, STANDARD_SLICE_LENGTH, resolveCurrentPlanSizeCode()),
);
const generateSegmentSequenceNotice = computed(() =>
  buildSlittingSegmentSequenceNotice(activeSource.value, generateEstimateSliceLength.value, generateForm.sizeCode),
);
watch(() => confirmForm.sizeCode, () => syncSliceAdjustRemark());

function normalizeRows(payload: any): any[] {
  const source = payload?.data ?? payload;
  if (Array.isArray(source)) return source;
  if (Array.isArray(source?.list)) return source.list;
  if (Array.isArray(source?.records)) return source.records;
  if (Array.isArray(source?.rows)) return source.rows;
  return [];
}

function formatNumber(value?: number) {
  const num = Number(value || 0);
  if (!Number.isFinite(num)) return '0';
  return Number(num.toFixed(3)).toString();
}

function getSlittingQtimeText(qtime?: MesHcSlittingConsoleApi.QtimeInfo) {
  if (!qtime) return '-';
  if (qtime.status === 'WAITING_SOURCE_FINISH') return '0分钟（并行开工）';
  if (qtime.status === 'MISSING_SOURCE_TIME') return '上道未完工';
  if (qtime.status === 'NO_RULE') return '未配置';
  const minutes = Math.max(0, Number(qtime.elapsedMinutes || 0));
  return qtime.timeout ? `超时 ${minutes}分钟` : `${minutes}分钟`;
}

function getSlittingQtimeColor(qtime?: MesHcSlittingConsoleApi.QtimeInfo) {
  if (qtime?.timeout) return 'red';
  if (qtime?.status === 'WAITING_SOURCE_FINISH') return 'blue';
  if (qtime?.status === 'NORMAL') return 'green';
  return 'default';
}

function formatTaskStatus(status?: string) {
  const normalized = normalizeWorkListStatusValue(status);
  if (normalized === 'COMPLETED') return '已完工';
  if (normalized === 'IN_PROGRESS') return '生产中';
  if (normalized === 'PENDING') return '待开工';
  if (normalized === 'PAUSED') return '已暂停';
  if (normalized === 'CANCELLED') return '已作废';
  return status || '-';
}

function normalizeWorkListStatusValue(status?: string) {
  const raw = String(status || '').trim();
  const normalized = raw.toUpperCase();
  if (normalized === 'RUNNING') return 'IN_PROGRESS';
  if (normalized === 'FINISHED') return 'COMPLETED';
  if (normalized === 'RELEASED') return 'PENDING';
  if (raw.includes('暂停')) return 'PAUSED';
  if (raw.includes('作废') || raw.includes('取消')) return 'CANCELLED';
  if (normalized === 'CANCELED' || normalized === 'VOID' || normalized === 'ABORTED') return 'CANCELLED';
  if (
    normalized === 'IN_PROGRESS' ||
    normalized === 'COMPLETED' ||
    normalized === 'PENDING' ||
    normalized === 'PAUSED' ||
    normalized === 'CANCELLED'
  ) return normalized;
  return normalized;
}

function getWorkListStatusMeta(rowOrStatus?: string | WorkSourceRow | Record<string, any>) {
  const status = typeof rowOrStatus === 'object' ? rowOrStatus?.status : rowOrStatus;
  const sampleAbnormalLocked = typeof rowOrStatus === 'object' && !!rowOrStatus?.sampleAbnormalLocked;
  const normalized = normalizeWorkListStatusValue(status);
  if (normalized === 'COMPLETED') return { color: 'success', text: '已完工' };
  if (normalized === 'IN_PROGRESS') {
    return sampleAbnormalLocked
      ? { color: 'error', text: '品质异常' }
      : { color: 'processing', text: '生产中' };
  }
  if (normalized === 'PENDING') return { color: 'default', text: '待开工' };
  if (normalized === 'PAUSED') return { color: 'warning', text: '已暂停' };
  if (normalized === 'CANCELLED') return { color: 'red', text: '已作废' };
  return { color: 'default', text: formatTaskStatus(status) };
}

function resolveMergedWorkStatus(source?: MesHcSlittingConsoleApi.SourceItem | Record<string, any> | null) {
  const planStatus = normalizeWorkListStatusValue(currentPlan.status);
  if (planStatus === 'PAUSED' || planStatus === 'CANCELLED' || planStatus === 'COMPLETED') return planStatus;
  return normalizeWorkListStatusValue(source?.status || currentPlan.status);
}

function mergeWorkSourceStatus(taskStatus?: string, sourceStatus?: string) {
  const normalizedTaskStatus = normalizeWorkListStatusValue(taskStatus);
  const normalizedSourceStatus = normalizeWorkListStatusValue(sourceStatus);
  if (normalizedTaskStatus === 'PAUSED' || normalizedTaskStatus === 'CANCELLED' || normalizedTaskStatus === 'COMPLETED') {
    return normalizedTaskStatus;
  }
  return normalizedSourceStatus || normalizedTaskStatus;
}

function isReadonlyWorkStatus(status?: string) {
  const normalized = normalizeWorkListStatusValue(status);
  return normalized === 'PAUSED' || normalized === 'CANCELLED' || normalized === 'COMPLETED';
}

function isWorkSourceReadonly(source?: MesHcSlittingConsoleApi.SourceItem | Record<string, any> | null) {
  return isReadonlyWorkStatus(resolveMergedWorkStatus(source));
}

function isSliceReadonly(slice?: MesHcSlittingConsoleApi.SliceItem | Record<string, any> | null) {
  return isWorkSourceReadonly(getSliceSource(slice));
}

function isPrintBlockedWorkStatus(status?: string) {
  const normalized = normalizeWorkListStatusValue(status);
  return normalized === 'PAUSED' || normalized === 'CANCELLED';
}

function isWorkSourcePrintBlocked(source?: MesHcSlittingConsoleApi.SourceItem | Record<string, any> | null) {
  return isPrintBlockedWorkStatus(resolveMergedWorkStatus(source));
}

function isSlicePrintBlocked(slice?: MesHcSlittingConsoleApi.SliceItem | Record<string, any> | null) {
  return isWorkSourcePrintBlocked(getSliceSource(slice));
}

function canDeleteSlice(slice?: MesHcSlittingConsoleApi.SliceItem | Record<string, any> | null) {
  return !!slice?.id
    && normalizeSliceStatus(slice.scanStatus) !== 'CONFIRMED'
    && !getSliceEditBlockedReason(slice)
    && !isSliceReadonly(slice);
}

function getWorkReadonlyMessage(status: string, actionName = '操作') {
  if (status === 'PAUSED') return `当前分切工序已暂停，请等待生产计划复工后再${actionName}`;
  if (status === 'CANCELLED') return `当前分切工序已作废取消，不能再${actionName}`;
  if (status === 'COMPLETED') return `当前分切段已完工，仅可查看，不能再${actionName}`;
  return '';
}

function warnReadonlyWork(actionName = '操作', source?: MesHcSlittingConsoleApi.SourceItem | Record<string, any> | null) {
  const status = resolveMergedWorkStatus(source || currentSource.value);
  const messageText = getWorkReadonlyMessage(status, actionName);
  if (!messageText) return false;
  message.warning(messageText);
  return true;
}

function warnPrintBlockedWork(source?: MesHcSlittingConsoleApi.SourceItem | Record<string, any> | null) {
  const status = resolveMergedWorkStatus(source || currentSource.value);
  if (status === 'PAUSED') {
    message.warning('当前分切工序已暂停，请等待生产计划复工后再打印流转单');
    return true;
  }
  if (status === 'CANCELLED') {
    message.warning('当前分切工序已作废取消，不能打印流转单');
    return true;
  }
  return false;
}

function getWorkListActionText(status?: string) {
  return isReadonlyWorkStatus(status) ? '查看' : '开工';
}

function getWorkSourceLockCandidates(row: MesHcSlittingConsoleApi.SourceItem | Record<string, any>) {
  return buildSegmentChainSampleLockCandidates({
    motherBatchNo: row.sourceBatchNo || row.parentProductionBatchNo,
    segmentBatchNo: getSourceSegmentBatchNo(row),
  });
}

async function markWorkSourceRowsSampleLockStatus(rows: WorkSourceRow[]) {
  await Promise.all(
    rows.map(async (row) => {
      const normalized = normalizeWorkListStatusValue(row.status);
      if (normalized !== 'IN_PROGRESS') {
        row.sampleAbnormalLocked = false;
        return;
      }
      row.sampleAbnormalLocked = !!(await findActiveSampleAbnormalLock(getWorkSourceLockCandidates(row)));
    }),
  );
}

function containsWorkListFilterText(values: unknown[], keyword: string) {
  const text = String(keyword || '').trim().toLowerCase();
  if (!text) return true;
  return values.some((value) => String(value ?? '').toLowerCase().includes(text));
}

function buildNowText() {
  return dayjs().format('YYYY-MM-DD HH:mm:ss');
}

function resolvePlanPrintOperations(planDetail: Record<string, any>) {
  return (
    planDetail?.operations ||
    planDetail?.operationList ||
    planDetail?.processList ||
    planDetail?.routeOperations ||
    planDetail?.planOperations ||
    []
  );
}

function createPrintQrDataUrl(value: string) {
  const source = ref(value);
  const qrCode = useQRCode(source, {
    errorCorrectionLevel: 'M',
    margin: 1,
    width: 128,
  });
  return new Promise<string>((resolve) => {
    const timer = window.setTimeout(() => {
      stop();
      resolve('');
    }, 1500);
    const stop = watch(
      qrCode,
      (dataUrl) => {
        if (!dataUrl) return;
        window.clearTimeout(timer);
        stop();
        resolve(dataUrl);
      },
      { immediate: true },
    );
  });
}

function parseVisualItems(row: MesHcSlittingConsoleApi.SliceItem | Record<string, any>): VisualItem[] {
  try {
    const parsed = JSON.parse(row.visualResultJson || '[]');
    const source = Array.isArray(parsed)
      ? parsed
      : parsed?.visualItems || parsed?.items || parsed?.details || parsed?.list || [];
    if (!Array.isArray(source)) return [];
    return source
      .map((item: any) => ({
        itemName: item.itemName || item.name || item.defectName || item.checkItem || item.label || '',
        remark: item.remark || item.value || item.actualValue || item.description || item.text || '',
        result: normalizeVisualResult(item.result ?? item.checkResult ?? item.status ?? item.ok),
      }))
      .filter((item) => !!item.itemName);
  } catch {
    return [];
  }
}

function normalizeVisualResult(value: unknown): 'NG' | 'OK' {
  const text = String(value ?? '').trim().toUpperCase();
  if (['NG', 'N', 'FALSE', '0', '异常', '不合格'].includes(text)) return 'NG';
  return 'OK';
}

function getVisualItem(row: MesHcSlittingConsoleApi.SliceItem | Record<string, any>, itemName: string) {
  return parseVisualItems(row).find((item) => item.itemName === itemName);
}

function isVisualItemOn(row: MesHcSlittingConsoleApi.SliceItem | Record<string, any>, itemName: string) {
  const item = getVisualItem(row, itemName);
  return item?.result === 'NG';
}

function hasVisualIssue(row: MesHcSlittingConsoleApi.SliceItem | Record<string, any>) {
  return row.selfCheck === 'NG'
    || parseVisualItems(row).some(isVisualItemActive)
    || hasDownstreamPreProcessFeedback(row);
}

function hasOwnSliceVisualIssue(row?: MesHcSlittingConsoleApi.SliceItem | Record<string, any> | null) {
  if (!row) return false;
  return row.selfCheck === 'NG' || parseVisualItems(row).some(isVisualItemActive);
}

function getActiveVisualCategoryItems(row?: MesHcSlittingConsoleApi.SliceItem | Record<string, any> | null) {
  const source = row || activeSlice.value;
  if (!source) return [];
  return Array.from(new Set(
    parseVisualItems(source)
      .filter((item) => isVisualItemActive(item))
      .map((item) => item.itemName)
      .filter((itemName) => visualItemNames.includes(itemName)),
  ));
}

function getActiveVisualCategoryText(row?: MesHcSlittingConsoleApi.SliceItem | Record<string, any> | null) {
  return getActiveVisualCategoryItems(row).join('、') || '-';
}

function getVisualCheckItemClass(item: VisualItem) {
  return isVisualItemActive(item) ? 'visual-light-item is-active' : 'visual-light-item';
}

function isVisualItemActive(item: VisualItem) {
  return item.result === 'NG';
}

function normalizeSliceStatus(status?: unknown) {
  return String(status || '').trim().toUpperCase();
}

function matchSliceFilter(row: MesHcSlittingConsoleApi.SliceItem | Record<string, any>) {
  if (filterForm.recordDate && resolveSliceRecordDate(row) !== filterForm.recordDate) {
    return false;
  }
  const segmentBatchNo = normalizeSegmentBatchNo(filterForm.segmentBatchNo);
  if (segmentBatchNo) {
    const rowSegmentBatchNo = normalizeSegmentBatchNo(getSliceSegmentBatchNo(row));
    if (rowSegmentBatchNo !== segmentBatchNo) return false;
  }
  const scanStatus = normalizeSliceStatus(row.scanStatus);
  const printStatus = normalizeSliceStatus(row.printStatus);
  if (filterForm.status === 'CONFIRMED') return scanStatus === 'CONFIRMED' && !hasVisualIssue(row);
  if (filterForm.status === 'PENDING') return scanStatus !== 'CONFIRMED';
  if (filterForm.status === 'PRINTED') return printStatus === 'PRINTED';
  if (filterForm.status === 'NG') return hasVisualIssue(row);
  return true;
}

function matchSourceSegmentFilter(source?: MesHcSlittingConsoleApi.SourceItem | null) {
  const segmentBatchNo = normalizeSegmentBatchNo(filterForm.segmentBatchNo);
  if (!segmentBatchNo) return true;
  return getSourceSegmentBatchNo(source) === segmentBatchNo;
}

function resolveSliceRecordDate(row: MesHcSlittingConsoleApi.SliceItem | Record<string, any>) {
  const raw = row.createTime || row.scanTime || row.lastPrintTime || '';
  if (!raw) return '';
  const parsed = dayjs(raw);
  return parsed.isValid() ? parsed.format('YYYY-MM-DD') : String(raw).slice(0, 10);
}

function formatScanConfirmDate(row: MesHcSlittingConsoleApi.SliceItem | Record<string, any>) {
  const raw = row.scanTime || '';
  if (!raw) return '-';
  const parsed = dayjs(raw);
  return parsed.isValid() ? parsed.format('YYYY-MM-DD') : String(raw).slice(0, 10);
}

function resetPlan() {
  Object.assign(currentPlan, {
    availableSourceLength: 0,
    batchNo: '',
    equipmentCode: '',
    equipmentId: undefined,
    equipmentName: '',
    materialCode: '',
    modelCode: '',
    planSizeSpec: '',
    planId: undefined,
    planNo: '',
    planOperationId: undefined,
    requirements: '',
    sourceBatchNo: '',
    sourceProductionBatchNo: '',
    status: '',
    workCenterName: '',
  });
  sources.value = [];
  slices.value = [];
  activeSource.value = null;
}

function applyTask(task: MesHcSlittingConsoleApi.TaskItem) {
  currentPlan.availableSourceLength = Number(task.availableSourceLength || 0);
  currentPlan.batchNo = task.batchNo || task.parentProductionBatchNo || '';
  currentPlan.equipmentCode = task.equipmentCode || '';
  currentPlan.equipmentId = task.equipmentId;
  currentPlan.equipmentName = task.equipmentName || '';
  currentPlan.materialCode = task.materialCode || '';
  currentPlan.modelCode = task.modelCode || '';
  currentPlan.planSizeSpec = task.sizeSpec || task.sizeName || task.spec || '';
  currentPlan.planId = task.planId;
  currentPlan.planNo = task.planNo || '';
  currentPlan.planOperationId = task.planOperationId;
  currentPlan.requirements = task.requirements || '';
  currentPlan.sourceBatchNo = task.sourceBatchNo || currentPlan.batchNo;
  currentPlan.sourceProductionBatchNo = task.sourceProductionBatchNo || '';
  currentPlan.status = (task as any).operationTaskStatus || task.status || '';
  currentPlan.workCenterName = task.workCenterName || '';
  filterForm.segmentBatchNo = '';
}

function getSourceAvailableLength(source?: MesHcSlittingConsoleApi.SourceItem | null) {
  return Number(source?.availableSourceLength ?? source?.outputLength ?? 0);
}

function applySource(source?: MesHcSlittingConsoleApi.SourceItem | null) {
  if (!source) return;
  currentPlan.availableSourceLength = getSourceAvailableLength(source);
  currentPlan.batchNo = source.parentProductionBatchNo || source.sourceBatchNo || currentPlan.batchNo;
  currentPlan.materialCode = source.materialCode || currentPlan.materialCode;
  currentPlan.modelCode = source.modelCode || currentPlan.modelCode;
  currentPlan.sourceBatchNo = source.sourceBatchNo || source.parentProductionBatchNo || currentPlan.sourceBatchNo;
  currentPlan.sourceProductionBatchNo = getSourceSegmentBatchNo(source) || currentPlan.sourceProductionBatchNo;
}

function syncSliceFiltersWithSource(source?: MesHcSlittingConsoleApi.SourceItem | null, force = false) {
  const segmentBatchNo = getSourceSegmentBatchNo(source);
  if (segmentBatchNo && (force || !filterForm.segmentBatchNo.trim())) {
    filterForm.segmentBatchNo = segmentBatchNo;
  }
}

function selectActiveSource(source?: MesHcSlittingConsoleApi.SourceItem | null, forceFilter = true) {
  if (!source) return;
  activeSource.value = source;
  applySource(source);
  syncSliceFiltersWithSource(source, forceFilter);
}

function buildWorkSourceRow(
  task: MesHcSlittingConsoleApi.TaskItem,
  source: MesHcSlittingConsoleApi.SourceItem,
  sourceStartPosition = 0,
): WorkSourceRow {
  const sourceKey = `${task.planOperationId || task.planId || task.planNo}-${source.adhesiveReportId}`;
  const sourceLength = Number(source.outputLength || 0);
  const backendStartPosition = source.sourceStartPosition === undefined || source.sourceStartPosition === null
    ? Number.NaN
    : Number(source.sourceStartPosition);
  const normalizedSourceStartPosition = Number.isFinite(backendStartPosition)
    ? backendStartPosition
    : sourceStartPosition;
  const backendEndPosition = source.sourceEndPosition === undefined || source.sourceEndPosition === null
    ? Number.NaN
    : Number(source.sourceEndPosition);
  const sourceEndPosition = Number.isFinite(backendEndPosition)
    ? backendEndPosition
    : normalizedSourceStartPosition + sourceLength;
  return {
    ...task,
    ...source,
    availableSourceLength: getSourceAvailableLength(source),
    id: task.id,
    materialCode: source.materialCode || task.materialCode,
    modelCode: source.modelCode || task.modelCode,
    operationTaskStatus: task.status,
    planId: task.planId,
    planNo: task.planNo,
    planOperationId: task.planOperationId,
    sourceEndPosition,
    sourceBatchNo: source.sourceBatchNo || source.parentProductionBatchNo || task.sourceBatchNo,
    sourceKey,
    sourcePositionText: `${formatNumber(normalizedSourceStartPosition)}-${formatNumber(sourceEndPosition)} m`,
    sourceStartPosition: normalizedSourceStartPosition,
    sourceProductionBatchNo: getSourceSegmentBatchNo(source) || task.sourceProductionBatchNo,
    status: mergeWorkSourceStatus(task.status, source.status),
  };
}

function getSourceSlices(sourceId?: number) {
  if (!sourceId) return [];
  return slices.value
    .filter((item) => item.sourceAdhesiveReportId === sourceId)
    .sort((a, b) => {
      const cardDiff = compareSlittingCardNo(a.sliceSerialNo, b.sliceSerialNo);
      if (cardDiff !== 0) return cardDiff;
      return Number(a.sliceIndex || 0) - Number(b.sliceIndex || 0);
    });
}

function getVisualSourceSlices(sourceId?: number) {
  const rows = getSourceSlices(sourceId);
  if (transferPrintSelectionMode.value || isSliceFiltering.value) return rows.filter(matchSliceFilter);
  return rows;
}

function getGridColumns(_count: number) {
  return 'repeat(auto-fill, 168px)';
}

function getSliceClass(slice: MesHcSlittingConsoleApi.SliceItem) {
  if (hasVisualIssue(slice)) return 'slice-cell is-ng';
  if (slice.scanStatus === 'CONFIRMED') return 'slice-cell is-confirmed';
  return 'slice-cell is-pending';
}

function getTransferSliceKey(slice?: MesHcSlittingConsoleApi.SliceItem | Record<string, any> | null) {
  return slice?.id == null ? '' : String(slice.id);
}

function isTransferSliceSelected(slice: MesHcSlittingConsoleApi.SliceItem | Record<string, any>) {
  const key = getTransferSliceKey(slice);
  return !!key && selectedTransferSliceIds.value.includes(key);
}

function getSliceCellClass(slice: MesHcSlittingConsoleApi.SliceItem) {
  return [
    getSliceClass(slice),
    {
      'is-transfer-selectable': transferPrintSelectionMode.value,
      'is-transfer-selected': isTransferSliceSelected(slice),
    },
  ];
}

function getSliceStatusText(slice: MesHcSlittingConsoleApi.SliceItem | Record<string, any>) {
  if (hasDownstreamPreProcessFeedback(slice)) {
    return `${slice.downstreamFeedbackProcessName || '后工序'}反馈`;
  }
  if (hasVisualIssue(slice)) return '不良';
  if (slice.scanStatus === 'CONFIRMED') return '确认';
  return '待确认';
}

function getSliceSelfCheckText(slice: MesHcSlittingConsoleApi.SliceItem | Record<string, any>) {
  if (hasDownstreamPreProcessFeedback(slice)) {
    return (getDownstreamPreProcessFeedbackReason(slice) || '分切自检异常').replace(/（.*$/, '');
  }
  return `${slice.selfCheck || '待检'} ${slice.remark || ''}`.trim();
}

function getSliceEditBlockedReason(slice: MesHcSlittingConsoleApi.SliceItem | Record<string, any>) {
  return String(slice.editBlockedReason || '').trim();
}

function getSliceNgTitle(slice: MesHcSlittingConsoleApi.SliceItem | Record<string, any>) {
  return [
    getSliceEditBlockedReason(slice) || getDownstreamPreProcessFeedbackTitle(slice) || getSliceSelfCheckText(slice),
    getSlittingSizeChangeText(slice) ? `改型：${getSlittingSizeChangeText(slice)}` : '',
  ].filter(Boolean).join('\n');
}

function getSliceSegmentBatchNo(slice?: MesHcSlittingConsoleApi.SliceItem | Record<string, any> | null) {
  if (!slice) return '-';
  const source = sources.value.find((item) => item.adhesiveReportId === slice.sourceAdhesiveReportId);
  return getSourceSegmentBatchNo(source) || normalizeSegmentBatchNo(slice.sourceProductionBatchNo) || '-';
}

function getSliceSource(slice?: MesHcSlittingConsoleApi.SliceItem | Record<string, any> | null) {
  if (!slice) return undefined;
  return sources.value.find((item) => item.adhesiveReportId === slice.sourceAdhesiveReportId);
}

function getSlicePositionText(slice?: MesHcSlittingConsoleApi.SliceItem | Record<string, any> | null) {
  if (!slice) return '-';
  const start = Number(slice.startPosition);
  const end = Number(slice.endPosition);
  if (Number.isFinite(start) && Number.isFinite(end) && end > start) {
    return `${formatNumber(start)}-${formatNumber(end)} m`;
  }
  return '-';
}

function isSlittingSegmentMark(value?: unknown): value is SlittingSegmentMark {
  return SLITTING_SEGMENT_MARKS.includes(String(value || '').trim().toUpperCase() as SlittingSegmentMark);
}

function getSegmentMarkFromBatchNo(batchNo?: string) {
  const segmentBatchNo = normalizeSegmentBatchNo(batchNo);
  const mark = segmentBatchNo.slice(-1);
  return isSlittingSegmentMark(mark) ? mark : '';
}

function getSourceSegmentMark(source?: MesHcSlittingConsoleApi.SourceItem | Record<string, any> | null) {
  const explicitMark = String(source?.segmentMark || '').trim().toUpperCase();
  if (isSlittingSegmentMark(explicitMark)) return explicitMark;
  return getSegmentMarkFromBatchNo(getSourceSegmentBatchNo(source));
}

function getSourceMotherBatchNo(source?: MesHcSlittingConsoleApi.SourceItem | Record<string, any> | null) {
  if (!source) return '';
  return resolveMotherBatchNo(
    getSourceSegmentBatchNo(source) ||
    source.sourceBatchNo ||
    source.parentProductionBatchNo ||
    source.sourceProductionBatchNo ||
    source.productionBatchNo ||
    source.adhesiveProductionBatchNo,
  );
}

function findSourceByMotherAndSegmentMark(motherBatchNo: string, segmentMark: SlittingSegmentMark) {
  return sources.value.find((source) =>
    getSourceMotherBatchNo(source) === motherBatchNo && getSourceSegmentMark(source) === segmentMark,
  );
}

function isSliceOfMother(slice: MesHcSlittingConsoleApi.SliceItem | Record<string, any>, motherBatchNo: string) {
  return isSameMotherBatchNo(getSliceSegmentBatchNo(slice), motherBatchNo)
    || isSameMotherBatchNo(slice.sourceBatchNo, motherBatchNo)
    || isSameMotherBatchNo(slice.sourceProductionBatchNo, motherBatchNo);
}

function getSegmentSlicesByMotherAndMark(motherBatchNo: string, segmentMark: SlittingSegmentMark) {
  return slices.value.filter((slice) =>
    isSliceOfMother(slice, motherBatchNo) && getSegmentMarkFromBatchNo(getSliceSegmentBatchNo(slice)) === segmentMark,
  );
}

function getSegmentExpectedSliceCount(source: MesHcSlittingConsoleApi.SourceItem | undefined, sliceLength: number) {
  if (!source) return 0;
  const sourceLength = Number(source.outputLength || 0);
  const safeSliceLength = Number(sliceLength || 0) > 0 ? Number(sliceLength) : STANDARD_SLICE_LENGTH;
  if (!Number.isFinite(sourceLength) || sourceLength <= 0) return 0;
  return Math.max(1, Math.floor(sourceLength / safeSliceLength));
}

function getMotherWetOutputLength(
  motherBatchNo: string,
  source?: MesHcSlittingConsoleApi.SourceItem | Record<string, any> | null,
) {
  const directValue = Number(source?.motherWetOutputLength || 0);
  if (Number.isFinite(directValue) && directValue > 0) return directValue;
  const matchedValue = sources.value
    .filter((item) => getSourceMotherBatchNo(item) === motherBatchNo)
    .map((item) => Number(item.motherWetOutputLength || 0))
    .find((value) => Number.isFinite(value) && value > 0);
  return matchedValue || 0;
}

function getAverageSegmentStartEstimate(
  motherBatchNo: string,
  currentIndex: number,
  sliceLength: number,
  source?: MesHcSlittingConsoleApi.SourceItem | Record<string, any> | null,
) {
  const wetOutputLength = getMotherWetOutputLength(motherBatchNo, source);
  const safeSliceLength = Number(sliceLength || 0) > 0 ? Number(sliceLength) : STANDARD_SLICE_LENGTH;
  if (!Number.isFinite(wetOutputLength) || wetOutputLength <= 0) return null;
  const totalSliceCount = Math.max(1, Math.floor(wetOutputLength / safeSliceLength));
  const baseCount = Math.floor(totalSliceCount / SLITTING_SEGMENT_MARKS.length);
  const remainder = totalSliceCount % SLITTING_SEGMENT_MARKS.length;
  const beforeCount = SLITTING_SEGMENT_MARKS
    .slice(0, currentIndex)
    .reduce((sum, _mark, index) => sum + baseCount + (index < remainder ? 1 : 0), 0);
  return {
    startSerialNo: beforeCount + 1,
    totalSliceCount,
    wetOutputLength,
  };
}

function buildSlittingSegmentSequenceNotice(
  source: MesHcSlittingConsoleApi.SourceItem | Record<string, any> | null | undefined,
  sliceLength: number,
  sizeCode: SlittingSizeCode,
) {
  if (!source) return null;
  const currentMark = getSourceSegmentMark(source);
  const currentIndex = SLITTING_SEGMENT_MARKS.indexOf(currentMark as SlittingSegmentMark);
  if (!currentMark || currentIndex <= 0) return null;
  const motherBatchNo = getSourceMotherBatchNo(source);
  const currentSegmentBatchNo = getSourceSegmentBatchNo(source);
  if (!motherBatchNo || !currentSegmentBatchNo) return null;
  const previousMarks = SLITTING_SEGMENT_MARKS.slice(0, currentIndex);
  const missingMarks = previousMarks.filter((mark) =>
    getSegmentSlicesByMotherAndMark(motherBatchNo, mark).length === 0,
  );
  if (missingMarks.length === 0) return null;
  const safeSliceLength = Number(sliceLength || 0) > 0 ? Number(sliceLength) : STANDARD_SLICE_LENGTH;
  const expectedBeforeCurrent = previousMarks.reduce((sum, mark) => {
    const existingCount = getSegmentSlicesByMotherAndMark(motherBatchNo, mark).length;
    if (existingCount > 0) return sum + existingCount;
    return sum + getSegmentExpectedSliceCount(findSourceByMotherAndSegmentMark(motherBatchNo, mark), safeSliceLength);
  }, 0);
  const maxUsedSerialNo = slices.value
    .filter((slice) => isSliceOfMother(slice, motherBatchNo))
    .reduce((max, slice) => Math.max(max, extractSliceSerialNo(slice.sliceSerialNo)), 0);
  const averageEstimate = getAverageSegmentStartEstimate(motherBatchNo, currentIndex, safeSliceLength, source);
  const recommendedStartSerialNo = Math.max(
    maxUsedSerialNo + 1,
    expectedBeforeCurrent + 1,
    averageEstimate?.startSerialNo || 1,
  );
  const recommendedSliceNo = buildSlittingSliceNoWithSize(
    `${currentSegmentBatchNo}${String(recommendedStartSerialNo).padStart(3, '0')}`,
    sizeCode,
  );
  const averageTip = averageEstimate
    ? `；均分依据：湿法 ${formatNumber(averageEstimate.wetOutputLength)}m / 每片 ${formatNumber(safeSliceLength)}m ≈ ${averageEstimate.totalSliceCount} 片，${currentMark} 段理论从流水号 ${averageEstimate.startSerialNo} 开始`
    : '';
  return {
    currentMark,
    missingMarks,
    recommendedSliceNo,
    recommendedStartSerialNo,
    text: `前序分段 ${missingMarks.join('/')} 尚未生成分切片号，当前先切 ${currentMark} 段会造成片号顺序风险。`,
    tip: `推荐当前段起始片号 ${recommendedSliceNo}（流水号 ${recommendedStartSerialNo}）${averageTip}，仅提示，不自动填写。`,
  };
}

async function loadBoardData(sourceQuery?: MesHcSlittingConsoleApi.SourceQuery) {
  if (!currentPlan.planId || !currentPlan.planOperationId) return;
  const [sourceResp, sliceResp] = await Promise.all([
    getSlittingConsoleSourceList(currentPlan.planId, currentPlan.planOperationId, sourceQuery),
    getSlittingConsoleSliceList(currentPlan.planOperationId, {
      scanConfirmDate: filterForm.scanConfirmDate || undefined,
    }),
  ]);
  sources.value = sortSlittingSourceCards(normalizeRows(sourceResp) as MesHcSlittingConsoleApi.SourceItem[]);
  slices.value = normalizeRows(sliceResp) as MesHcSlittingConsoleApi.SliceItem[];
  const activeSourceId = activeSource.value?.adhesiveReportId;
  activeSource.value = (activeSourceId ? sources.value.find((item) => item.adhesiveReportId === activeSourceId) : null) || sources.value[0] || null;
  applySource(activeSource.value);
  syncSliceFiltersWithSource(activeSource.value);
  if (activeBoardTab.value === 'SLICES') updateSliceTableHeight();
}

function isTaskPlanMatched(task: MesHcSlittingConsoleApi.TaskItem, planNo: string) {
  return !!planNo && (task.planNo === planNo || task.id === planNo);
}

function normalizeReportBatchNo(batchNo?: unknown) {
  return String(batchNo || '').trim().toUpperCase();
}

function hasReportBatchSuffix(batchNo?: unknown) {
  return /-J\d+$/i.test(normalizeReportBatchNo(batchNo));
}

function isSameSegmentBatchNo(left?: string, right?: string) {
  const leftBatchNo = normalizeSegmentBatchNo(left);
  const rightBatchNo = normalizeSegmentBatchNo(right);
  return !!leftBatchNo && !!rightBatchNo && leftBatchNo === rightBatchNo;
}

function isSameReportBatchNo(left?: unknown, right?: unknown) {
  const leftBatchNo = normalizeReportBatchNo(left);
  const rightBatchNo = normalizeReportBatchNo(right);
  return !!leftBatchNo && !!rightBatchNo && leftBatchNo === rightBatchNo;
}

function getSourceReportBatchNo(source?: MesHcSlittingConsoleApi.SourceItem | Record<string, any> | null) {
  if (!source) return '';
  return normalizeReportBatchNo(source.productionBatchNo || source.adhesiveProductionBatchNo);
}

function isScannedSourceBatchMatched(
  scannedBatchNo: string,
  source?: MesHcSlittingConsoleApi.SourceItem | Record<string, any> | null,
) {
  if (!scannedBatchNo || !source) return false;
  if (hasReportBatchSuffix(scannedBatchNo)) {
    return isSameReportBatchNo(getSourceReportBatchNo(source), scannedBatchNo);
  }
  return isSameSegmentBatchNo(getSourceSegmentBatchNo(source), scannedBatchNo);
}

function getPendingScannerSegmentBatchNo(planNo: string) {
  return pendingScannerPlanNo === planNo ? pendingScannerSegmentBatchNo : '';
}

function clearPendingScannerPlan(planNo: string) {
  if (pendingScannerPlanNo === planNo) {
    pendingScannerPlanNo = '';
    pendingScannerSegmentBatchNo = '';
  }
}

function focusInputRef(inputRef: { value?: any }) {
  void nextTick(() => {
    window.setTimeout(() => {
      inputRef.value?.focus?.();
      inputRef.value?.input?.focus?.();
    }, 0);
  });
}

function focusPlanScanInput() {
  focusInputRef(planScanInputRef);
}

function focusSliceScanLookupInput() {
  focusInputRef(sliceScanLookupInputRef);
}

function resetPlanScanInput() {
  scanPlanNo.value = '';
  focusPlanScanInput();
}

function isEventTargetInInputRef(target: EventTarget | null, inputRef: { value?: any }) {
  const element = inputRef.value?.input || inputRef.value?.$el || inputRef.value;
  return !!(element && target instanceof Node && element.contains?.(target));
}

function isEventFromScannerInput(event: KeyboardEvent) {
  return isEventTargetInInputRef(event.target, planScanInputRef)
    || isEventTargetInInputRef(event.target, sliceScanLookupInputRef);
}

function isEditableEventTarget(target: EventTarget | null) {
  if (!(target instanceof HTMLElement)) return false;
  const tagName = target.tagName.toUpperCase();
  return target.isContentEditable || tagName === 'INPUT' || tagName === 'TEXTAREA' || tagName === 'SELECT';
}

function clearGlobalScannerBuffer() {
  globalScannerBuffer = '';
  globalScannerLastAt = 0;
  if (globalScannerTimer) {
    clearTimeout(globalScannerTimer);
    globalScannerTimer = null;
  }
}

function isCurrentPlanScanMatched(planNo: string, segmentBatchNo?: string) {
  if (currentPlan.planNo !== planNo) return false;
  if (!segmentBatchNo) return true;
  if (isScannedSourceBatchMatched(segmentBatchNo, activeSource.value)) return true;
  if (hasReportBatchSuffix(segmentBatchNo)) return false;
  return isSameSegmentBatchNo(currentPlan.sourceProductionBatchNo, segmentBatchNo);
}

function buildWorkSourceRowsForTask(
  task: MesHcSlittingConsoleApi.TaskItem,
  sourceRows: MesHcSlittingConsoleApi.SourceItem[],
) {
  const positionByMother = new Map<string, number>();
  return sourceRows.map((source) => {
    const motherKey = source.sourceBatchNo || source.parentProductionBatchNo || task.sourceBatchNo || task.planNo || '';
    const startPosition = positionByMother.get(motherKey) || 0;
    const row = buildWorkSourceRow(task, source, startPosition);
    positionByMother.set(motherKey, startPosition + Number(source.outputLength || 0));
    return row;
  });
}

async function findScannedWorkSourceRow(
  tasks: MesHcSlittingConsoleApi.TaskItem[],
  planNo: string,
  segmentBatchNo: string,
) {
  const candidateTasks = tasks.filter((task) => isTaskPlanMatched(task, planNo));
  for (const task of candidateTasks) {
    if (!task.planId || !task.planOperationId) continue;
    const sourceResp = await getSlittingConsoleSourceList(task.planId, task.planOperationId);
    const sourceRows = normalizeRows(sourceResp) as MesHcSlittingConsoleApi.SourceItem[];
    const matchedSourceRow = buildWorkSourceRowsForTask(task, sourceRows)
      .find((source) => isScannedSourceBatchMatched(segmentBatchNo, source));
    if (matchedSourceRow) return matchedSourceRow;
  }
  return null;
}

async function handleScanPlan() {
  if (planScanTimer) {
    clearTimeout(planScanTimer);
    planScanTimer = null;
  }
  const keyword = syncPlanScanNo(scanPlanNo.value);
  const segmentBatchNo = getPendingScannerSegmentBatchNo(keyword);
  clearPendingScannerPlan(keyword);
  if (!keyword) {
    scanError.value = '请输入或扫描计划号';
    resetPlanScanInput();
    return;
  }
  boardLoading.value = true;
  scanError.value = '';
  try {
    const resp = await getSlittingConsoleTaskList({ taskKeyword: keyword, taskStatus: 'ALL' });
    const rows = normalizeRows(resp) as MesHcSlittingConsoleApi.TaskItem[];
    const scannedReportBatch = hasReportBatchSuffix(segmentBatchNo);
    const exact = scannedReportBatch
      ? rows.find((item) => isTaskPlanMatched(item, keyword))
      : segmentBatchNo
        ? await findScannedWorkSourceRow(rows, keyword, segmentBatchNo)
        : rows.find((item) => isTaskPlanMatched(item, keyword)) || rows[0];
    if (!exact) {
      resetPlan();
      scanError.value = segmentBatchNo
        ? `未找到计划号 ${keyword} 且粘胶报工批次 ${segmentBatchNo} 的分切来源`
        : '未找到分切计划或当前计划没有已下达分切工序';
      return;
    }
    if (
      !(await ensureSampleAbnormalUnlocked(
        buildSegmentChainSampleLockCandidates({
          motherBatchNo: (exact as Record<string, any>).sourceBatchNo || (exact as Record<string, any>).parentProductionBatchNo,
          segmentBatchNo: getSourceSegmentBatchNo(exact as Record<string, any>) || segmentBatchNo,
        }),
        '加载分切',
      ))
    ) {
      return;
    }
    applyTask(exact);
    if (scannedReportBatch) {
      await loadBoardData({ productionBatchNo: segmentBatchNo });
      const matchedSource = sources.value.find((source) => isScannedSourceBatchMatched(segmentBatchNo, source));
      if (!matchedSource) {
        resetPlan();
        scanError.value = `未找到计划号 ${keyword} 且粘胶报工批次 ${segmentBatchNo} 的分切来源`;
        return;
      }
      selectActiveSource(matchedSource, true);
      return;
    }
    if (segmentBatchNo) {
      applySource(exact as WorkSourceRow);
      syncSliceFiltersWithSource(exact as WorkSourceRow, true);
    }
    await loadBoardData();
    if (segmentBatchNo) {
      const matchedSource = sources.value.find((source) => source.adhesiveReportId === (exact as WorkSourceRow).adhesiveReportId);
      selectActiveSource(matchedSource || activeSource.value, true);
    }
  } finally {
    boardLoading.value = false;
    resetPlanScanInput();
  }
}

function scheduleScannerPlanScan(value?: string) {
  if (!hasPlanScanDelimiter(value)) return;
  const planNo = syncPlanScanNo(value);
  const segmentBatchNo = getPendingScannerSegmentBatchNo(planNo);
  if (planScanTimer) {
    clearTimeout(planScanTimer);
    planScanTimer = null;
  }
  if (!planNo) return;
  planScanTimer = setTimeout(() => {
    if (planNo !== normalizePlanScanNo(scanPlanNo.value)) {
      clearPendingScannerPlan(planNo);
      return;
    }
    if (isCurrentPlanScanMatched(planNo, segmentBatchNo)) {
      clearPendingScannerPlan(planNo);
      resetPlanScanInput();
      return;
    }
    void handleScanPlan();
  }, 180);
}

function isGlobalScannerCandidate(value: string) {
  const text = value.trim();
  if (!text) return false;
  if (sliceScanVisible.value) return text.length >= 3;
  if (confirmVisible.value) return false;
  return hasPlanScanDelimiter(text) || normalizePlanScanNo(text).length >= PLAN_SCAN_MIN_LENGTH;
}

function routeGlobalScannerInput(value: string) {
  const scanned = value.trim();
  if (sliceScanVisible.value) {
    sliceScanForm.scanCode = scanned;
    focusSliceScanLookupInput();
    nextTick(() => handleSliceScanLookup());
    return;
  }
  scanPlanNo.value = scanned;
  focusPlanScanInput();
  nextTick(() => void handleScanPlan());
}

function flushGlobalScannerBuffer() {
  const scanned = globalScannerBuffer.trim();
  clearGlobalScannerBuffer();
  if (!isGlobalScannerCandidate(scanned)) return false;
  routeGlobalScannerInput(scanned);
  return true;
}

function scheduleGlobalScannerFlush() {
  if (globalScannerTimer) clearTimeout(globalScannerTimer);
  globalScannerTimer = setTimeout(() => {
    void flushGlobalScannerBuffer();
  }, GLOBAL_SCANNER_IDLE_FLUSH_MS);
}

function handleGlobalScannerKeydown(event: KeyboardEvent) {
  if (
    event.ctrlKey ||
    event.altKey ||
    event.metaKey ||
    event.isComposing ||
    isEventFromScannerInput(event) ||
    isEditableEventTarget(event.target)
  ) {
    return;
  }
  if (event.key === 'Enter') {
    if (flushGlobalScannerBuffer()) {
      event.preventDefault();
      event.stopPropagation();
    }
    return;
  }
  if (event.key.length !== 1) return;
  const now = Date.now();
  const gap = globalScannerLastAt ? now - globalScannerLastAt : 0;
  if (!globalScannerLastAt || gap > GLOBAL_SCANNER_MAX_GAP_MS) {
    clearGlobalScannerBuffer();
  }
  globalScannerBuffer += event.key;
  globalScannerLastAt = now;
  scheduleGlobalScannerFlush();
  if (globalScannerBuffer.length > 1 && gap > 0 && gap <= GLOBAL_SCANNER_MAX_GAP_MS) {
    event.preventDefault();
  }
}

function attachGlobalScannerListener() {
  if (globalScannerListenerAttached) return;
  window.addEventListener('keydown', handleGlobalScannerKeydown, true);
  globalScannerListenerAttached = true;
}

function detachGlobalScannerListener() {
  if (!globalScannerListenerAttached) return;
  window.removeEventListener('keydown', handleGlobalScannerKeydown, true);
  globalScannerListenerAttached = false;
  clearGlobalScannerBuffer();
}

async function openWorkListDialog() {
  workListVisible.value = true;
  await loadWorkList();
}

async function loadWorkList() {
  workListLoading.value = true;
  try {
    const resp = await getSlittingConsoleTaskList({ taskStatus: 'ALL' });
    const tasks = normalizeRows(resp) as MesHcSlittingConsoleApi.TaskItem[];
    const rows: WorkSourceRow[] = [];
    await Promise.all(tasks.map(async (task) => {
      if (!task.planId || !task.planOperationId) return;
      const sourceResp = await getSlittingConsoleSourceList(task.planId, task.planOperationId);
      const sourceRows = normalizeRows(sourceResp) as MesHcSlittingConsoleApi.SourceItem[];
      const positionByMother = new Map<string, number>();
      sourceRows.forEach((source) => {
        const motherKey = source.sourceBatchNo || source.parentProductionBatchNo || task.sourceBatchNo || task.planNo || '';
        const startPosition = positionByMother.get(motherKey) || 0;
        rows.push(buildWorkSourceRow(task, source, startPosition));
        positionByMother.set(motherKey, startPosition + Number(source.outputLength || 0));
      });
    }));
    await markWorkSourceRowsSampleLockStatus(rows);
    workListRows.value = rows.sort((a, b) => {
      const planCompare = String(a.planNo || '').localeCompare(String(b.planNo || ''));
      if (planCompare !== 0) return planCompare;
      return compareSlittingCardNo(getSourceSegmentBatchNo(a), getSourceSegmentBatchNo(b));
    });
    workListPage.value = 1;
  } finally {
    workListLoading.value = false;
  }
}

function resetWorkListFilters() {
  workListFilters.planNo = '';
  workListFilters.modelCode = '';
  workListFilters.batchNo = '';
  workListFilters.status = 'UNFINISHED';
  workListPage.value = 1;
}

async function handleWorkListSearch() {
  workListPage.value = 1;
  await loadWorkList();
}

async function handleSelectWorkTask(task: WorkSourceRow) {
  if (
    !(await ensureSampleAbnormalUnlocked(
      getWorkSourceLockCandidates(task),
      '开工分切',
    ))
  ) {
    return;
  }
  applyTask(task);
  applySource(task);
  syncSliceFiltersWithSource(task, true);
  scanPlanNo.value = task.planNo || '';
  workListVisible.value = false;
  await loadBoardData();
  const matchedSource = sources.value.find((source) => source.adhesiveReportId === task.adhesiveReportId);
  selectActiveSource(matchedSource || activeSource.value, true);
}

function normalizeSegmentBatchNo(batchNo?: string) {
  let text = String(batchNo || '').trim().toUpperCase();
  if (!text) return '';
  text = stripSlittingSizeSuffix(text);
  text = text.replace(/-S\d+$/i, '');
  text = text.replace(/-J\d+$/i, '');
  if (/[PQRS]\d{3}$/i.test(text)) {
    text = text.slice(0, -3);
  }
  return text;
}

function getSourceSegmentBatchNo(source?: MesHcSlittingConsoleApi.SourceItem | Record<string, any> | null) {
  if (!source) return '';
  const segmentBatchNo = normalizeSegmentBatchNo(String(source.segmentBatchNo || source.sourceProductionBatchNo || ''));
  if (segmentBatchNo) return segmentBatchNo;
  const motherBatchNo = normalizeSegmentBatchNo(String(source.sourceBatchNo || source.parentProductionBatchNo || ''));
  const segmentMark = String(source.segmentMark || '').trim().toUpperCase();
  if (motherBatchNo && /^[PQRS]$/.test(segmentMark)) {
    return /[PQRS]$/.test(motherBatchNo) ? motherBatchNo : `${motherBatchNo}${segmentMark}`;
  }
  return normalizeSegmentBatchNo(String(source.productionBatchNo || source.adhesiveProductionBatchNo || ''));
}

function resolveMotherBatchNo(batchNo?: string) {
  const segmentBatchNo = normalizeSegmentBatchNo(batchNo);
  return /[PQRS]$/i.test(segmentBatchNo) ? segmentBatchNo.slice(0, -1) : segmentBatchNo;
}

function isSameMotherBatchNo(left?: string, right?: string) {
  const leftMother = resolveMotherBatchNo(left);
  const rightMother = resolveMotherBatchNo(right);
  return !!leftMother && !!rightMother && leftMother === rightMother;
}

function normalizeSlittingSizeCode(value?: unknown): SlittingSizeCode | '' {
  const text = String(value || '').trim().toUpperCase();
  if (!text) return '';
  if (text === 'A' || text.includes('775')) return '775';
  if (text === 'B' || text.includes('740')) return '740';
  if (['UNKNOWN', 'UNCERTAIN', 'NONE', 'NA', 'N/A', '不确定', '空'].includes(text)) return 'UNKNOWN';
  return '';
}

function resolveCurrentPlanSizeCode(): SlittingSizeCode {
  return normalizeSlittingSizeCode(currentPlan.planSizeSpec) || 'UNKNOWN';
}

function resolveSlittingSizeName(sizeCode: SlittingSizeCode) {
  if (sizeCode === '775') return '775mm';
  if (sizeCode === '740') return '740mm';
  return '不确定';
}

function stripSlittingSizeSuffix(sliceSerialNo?: string) {
  const text = String(sliceSerialNo || '').trim().toUpperCase();
  return /[PQRS]\d{3}[AB]$/i.test(text) ? text.slice(0, -1) : text;
}

function getSlittingCardSerialNo(cardNo?: string) {
  const text = stripSlittingSizeSuffix(
    String(cardNo || '')
      .trim()
      .replace(/-(?:J|S)\d+$/i, ''),
  );
  if (!text) return Number.MAX_SAFE_INTEGER;
  const normalized = /[AB]$/i.test(text) ? text.slice(0, -1) : text;
  const sortText = normalized.slice(-3);
  return /^\d+$/.test(sortText) ? Number(sortText) : Number.MAX_SAFE_INTEGER;
}

function compareSlittingCardNo(left?: string, right?: string) {
  const serialDiff = getSlittingCardSerialNo(left) - getSlittingCardSerialNo(right);
  if (serialDiff !== 0) return serialDiff;
  return String(left || '').localeCompare(String(right || ''), 'zh-Hans-CN', { numeric: true });
}

function sortSlittingSourceCards(rows: MesHcSlittingConsoleApi.SourceItem[]) {
  return [...rows].sort((a, b) => compareSlittingCardNo(getSourceSegmentBatchNo(a), getSourceSegmentBatchNo(b)));
}

function buildSlittingSliceNoWithSize(sliceSerialNo: string, sizeCode: SlittingSizeCode) {
  const baseNo = stripSlittingSizeSuffix(sliceSerialNo);
  if (!baseNo || sizeCode === 'UNKNOWN') return baseNo || '-';
  return `${baseNo}${sizeCode === '775' ? 'A' : 'B'}`;
}

function resolveSliceSizeCode(slice?: MesHcSlittingConsoleApi.SliceItem | Record<string, any> | null): SlittingSizeCode {
  const storedSize = normalizeSlittingSizeCode(slice?.sizeCode || slice?.sizeName);
  if (storedSize) return storedSize;
  const sliceNo = String(slice?.sliceSerialNo || '').trim().toUpperCase();
  if (/[PQRS]\d{3}A$/i.test(sliceNo)) return '775';
  if (/[PQRS]\d{3}B$/i.test(sliceNo)) return '740';
  return resolveCurrentPlanSizeCode();
}

function resolveExplicitSliceSizeCode(slice?: MesHcSlittingConsoleApi.SliceItem | Record<string, any> | null): SlittingSizeCode {
  const storedSize = normalizeSlittingSizeCode(slice?.sizeCode || slice?.sizeName);
  if (storedSize) return storedSize;
  const sliceNo = String(slice?.sliceSerialNo || '').trim().toUpperCase();
  if (/[PQRS]\d{3}A$/i.test(sliceNo)) return '775';
  if (/[PQRS]\d{3}B$/i.test(sliceNo)) return '740';
  return 'UNKNOWN';
}

function getSlittingSizeChangeMeta(slice?: MesHcSlittingConsoleApi.SliceItem | Record<string, any> | null) {
  const plannedSizeCode = resolveCurrentPlanSizeCode();
  const actualSizeCode = resolveExplicitSliceSizeCode(slice);
  const changed = plannedSizeCode !== 'UNKNOWN' && actualSizeCode !== 'UNKNOWN' && plannedSizeCode !== actualSizeCode;
  return {
    actualSizeCode,
    actualSizeName: resolveSlittingSizeName(actualSizeCode),
    changed,
    plannedSizeCode,
    plannedSizeName: resolveSlittingSizeName(plannedSizeCode),
  };
}

function getSlittingSizeChangeText(slice?: MesHcSlittingConsoleApi.SliceItem | Record<string, any> | null) {
  const meta = getSlittingSizeChangeMeta(slice);
  if (!meta.changed) return '';
  return `${meta.plannedSizeName}->${meta.actualSizeName}`;
}

function buildSlittingSliceAdjustRemark(remark: string, originalSliceNo: string, targetSliceNo: string) {
  if (stripSlittingSizeSuffix(originalSliceNo) !== stripSlittingSizeSuffix(targetSliceNo)
    || originalSliceNo.toUpperCase() === targetSliceNo.toUpperCase()) {
    return remark;
  }
  const adjustRemark = `${originalSliceNo || '-'} 调整 为 ${targetSliceNo || '-'}`;
  if (!remark.trim()) return adjustRemark;
  if (remark.includes(adjustRemark)) return remark;
  return `${remark}；${adjustRemark}`;
}

function removeLastSliceAdjustRemark(remark: string) {
  if (!lastSliceAdjustRemark) return remark;
  return remark
    .replace(`；${lastSliceAdjustRemark}`, '')
    .replace(`${lastSliceAdjustRemark}；`, '')
    .replace(lastSliceAdjustRemark, '')
    .trim();
}

function syncSliceAdjustRemark() {
  if (sliceDialogMode.value !== 'confirm') return;
  const targetSliceNo = confirmTargetSliceNo.value === '-' ? '' : confirmTargetSliceNo.value;
  const cleanRemark = removeLastSliceAdjustRemark(confirmForm.remark);
  lastSliceAdjustRemark = '';
  const nextRemark = buildSlittingSliceAdjustRemark(cleanRemark, confirmForm.originalSliceNo, targetSliceNo);
  if (nextRemark !== cleanRemark) {
    lastSliceAdjustRemark = nextRemark.replace(`${cleanRemark}；`, '');
  }
  confirmForm.remark = nextRemark;
}

function extractSliceSerialNo(sliceSerialNo?: string) {
  const text = stripSlittingSizeSuffix(sliceSerialNo);
  if (!text) return 0;
  const oldMatch = text.match(/-S(\d+)$/i);
  if (oldMatch?.[1]) return Number(oldMatch[1]);
  const newMatch = text.match(/[PQRS](\d{3})$/i);
  return newMatch?.[1] ? Number(newMatch[1]) : 0;
}

function getDefaultStartSerialNo(source: MesHcSlittingConsoleApi.SourceItem) {
  const motherBatch = resolveMotherBatchNo(source.sourceBatchNo || source.parentProductionBatchNo || '');
  const maxUsedSerialNo = slices.value
    .filter((item) => !motherBatch || isSameMotherBatchNo(item.sourceBatchNo, motherBatch) || isSameMotherBatchNo(item.sourceProductionBatchNo, motherBatch))
    .reduce((max, item) => Math.max(max, extractSliceSerialNo(item.sliceSerialNo)), 0);
  return maxUsedSerialNo + 1;
}

function hasSliceSerialNoOverlap(source: MesHcSlittingConsoleApi.SourceItem, startSerialNo: number, sliceCount: number) {
  const motherBatch = resolveMotherBatchNo(source.sourceBatchNo || source.parentProductionBatchNo || '');
  const usedSerialNos = new Set(
    slices.value
      .filter((item) => !motherBatch || isSameMotherBatchNo(item.sourceBatchNo, motherBatch) || isSameMotherBatchNo(item.sourceProductionBatchNo, motherBatch))
      .map((item) => extractSliceSerialNo(item.sliceSerialNo))
      .filter(Boolean),
  );
  for (let index = 0; index < sliceCount; index += 1) {
    if (usedSerialNos.has(startSerialNo + index)) return true;
  }
  return false;
}

function buildSourceCompleteWarnings(source: MesHcSlittingConsoleApi.SourceItem) {
  const rows = getSourceSlices(source.adhesiveReportId);
  const warnings: string[] = [];
  if (rows.length === 0) {
    warnings.push('当前段还没有生成分切生产片号');
  }
  const unconfirmedCount = rows.filter((item) => item.scanStatus !== 'CONFIRMED').length;
  if (rows.length > 0 && unconfirmedCount > 0) {
    warnings.push(`当前段有 ${unconfirmedCount} 片还没有扫码确认`);
  }
  return warnings;
}

async function handleCompleteCurrentSource() {
  const source = currentSource.value;
  if (!currentPlan.planId || !currentPlan.planOperationId || !source?.adhesiveReportId) {
    message.warning('请先扫描计划并选择当前分段来源');
    return;
  }
  if (warnReadonlyWork('标记切片完成', source)) return;
  if (currentSourceCompleted.value) {
    message.warning('当前段已标记切片完成');
    return;
  }
  const segmentBatchNo = getSourceSegmentBatchNo(source) || '当前段';
  const warnings = buildSourceCompleteWarnings(source);
  const rows = getSourceSlices(source.adhesiveReportId);
  const unconfirmedCount = rows.filter((item) => item.scanStatus !== 'CONFIRMED').length;
  const tailLength = Number(source.availableSourceLength || 0);
  const shouldDisposeTail = tailLength > 0 && tailLength < STANDARD_SLICE_LENGTH && unconfirmedCount === 0;
  const tailDisposeText = shouldDisposeTail
    ? `剩余 ${formatNumber(tailLength)}m 小于标准单片长度，将按分切尾料消耗并清零。`
    : '';
  const content = warnings.length > 0
    ? `请再确认一下：${warnings.join('；')}。${tailDisposeText}进入认证后需输入用户名确认；认证完成后，这一段会从未完成的待加工列表里隐藏，并进入只读查看状态。确定要完成这一段吗？`
    : `${tailDisposeText}这一段看起来已经切完了。进入认证后需输入用户名确认；认证完成后，这一段会从未完成的待加工列表里隐藏，并进入只读查看状态。确定要完成这一段吗？`;
  AModal.confirm({
    cancelText: '再检查一下',
    content,
    okText: '进入认证',
    title: `确认${segmentBatchNo}切片完成`,
    onOk() {
      pendingSourceComplete.value = {
        disposeRemainingTail: shouldDisposeTail,
        segmentBatchNo,
        sourceAdhesiveReportId: source.adhesiveReportId,
        tailDisposalLength: Number(tailLength.toFixed(3)),
      };
      sourceCompleteAuthVisible.value = true;
    },
  });
}

async function handleSourceCompleteAuthSuccess(authInfo?: any) {
  if (sourceCompleting.value) return;
  const pending = pendingSourceComplete.value;
  const confirmerUserId = Number(authInfo?.userId || 0);
  if (!pending || !currentPlan.planId || !currentPlan.planOperationId) {
    message.warning('未找到待确认的分切分段，请重新点击切片完成');
    sourceCompleteAuthVisible.value = false;
    return;
  }
  if (!confirmerUserId) {
    message.warning('未识别认证用户，请重新输入用户名确认');
    return;
  }
  sourceCompleting.value = true;
  try {
    await completeSlittingConsoleSource({
      confirmerUserId,
      planId: currentPlan.planId,
      planOperationId: currentPlan.planOperationId,
      sourceAdhesiveReportId: pending.sourceAdhesiveReportId,
      ...(pending.disposeRemainingTail
        ? {
            disposeRemainingTail: true,
            tailDisposalLength: pending.tailDisposalLength,
            tailDisposalReason: '分切完成尾料消耗',
          }
        : {}),
    });
    const confirmerName = authInfo?.empName || authInfo?.nickname || authInfo?.username || '操作员';
    message.success(`${pending.segmentBatchNo} 已标记切片完成${pending.disposeRemainingTail ? '，尾料已消耗' : ''}，认证人：${confirmerName}`);
    await loadBoardData();
  } finally {
    sourceCompleting.value = false;
    pendingSourceComplete.value = null;
    sourceCompleteAuthVisible.value = false;
  }
}

function handleSourceCompleteAuthCancel() {
  pendingSourceComplete.value = null;
  sourceCompleteAuthVisible.value = false;
}

function openGenerateDialog(source: MesHcSlittingConsoleApi.SourceItem, mode: 'AUTO' | 'MANUAL' = 'AUTO') {
  if (!hasPlan.value) {
    message.warning('请先扫描计划');
    return;
  }
  if (warnReadonlyWork(mode === 'MANUAL' ? '人工切片' : '自动切片', source)) return;
  generateError.value = '';
  selectActiveSource(source);
  generateMode.value = mode;
  generateForm.segmentLength = mode === 'MANUAL' ? 1 : STANDARD_SLICE_LENGTH;
  generateForm.sizeCode = resolveCurrentPlanSizeCode();
  const totalCount = Math.max(1, Math.floor(Number(source.outputLength || 0) / Math.max(generateForm.segmentLength, 0.001)));
  const remainCount = Math.max(1, totalCount - getSourceSlices(source.adhesiveReportId).length);
  generateForm.sliceCount = mode === 'MANUAL' ? 1 : remainCount;
  const sequenceNotice = buildSlittingSegmentSequenceNotice(source, generateEstimateSliceLength.value, generateForm.sizeCode);
  generateForm.startSerialNo = sequenceNotice ? undefined : getDefaultStartSerialNo(source);
  generateVisible.value = true;
}

function openTopGenerateDialog(mode: 'AUTO' | 'MANUAL' = 'AUTO') {
  if (!hasPlan.value) {
    message.warning('请先扫描计划');
    return;
  }
  if (warnReadonlyWork(mode === 'MANUAL' ? '人工切片' : '自动切片')) return;
  const unfinishedSources = sources.value.filter((source) => normalizeWorkListStatusValue(source.status) !== 'COMPLETED');
  const current = currentSource.value && normalizeWorkListStatusValue(currentSource.value.status) !== 'COMPLETED'
    ? currentSource.value
    : undefined;
  const target = mode === 'MANUAL'
    ? current || unfinishedSources[0] || sources.value[0]
    : unfinishedSources.find((source) => getSourceSlices(source.adhesiveReportId).length === 0)
      || current
      || unfinishedSources[0]
      || sources.value[0];
  if (!target) {
    message.warning('当前计划没有可初始化的分段来源');
    return;
  }
  openGenerateDialog(target, mode);
}

function refreshStartSerialNo() {
  if (!activeSource.value) return;
  generateError.value = '';
  const sequenceNotice = buildSlittingSegmentSequenceNotice(activeSource.value, generateEstimateSliceLength.value, generateForm.sizeCode);
  generateForm.startSerialNo = sequenceNotice ? undefined : getDefaultStartSerialNo(activeSource.value);
  if (generateMode.value === 'AUTO') {
    const totalCount = Math.max(1, Math.floor(Number(activeSource.value.outputLength || 0) / Math.max(Number(generateForm.segmentLength || 0), 0.001)));
    generateForm.sliceCount = Math.max(1, totalCount - getSourceSlices(activeSource.value.adhesiveReportId).length);
  }
}

function buildSlittingGeneratePayload(): MesHcSlittingConsoleApi.SliceGenerateReq | null {
  if (!currentPlan.planId || !currentPlan.planOperationId || !activeSource.value?.adhesiveReportId) return null;
  if (warnReadonlyWork(generateMode.value === 'MANUAL' ? '人工切片' : '自动切片', activeSource.value)) return null;
  generateError.value = '';
  const isManual = generateMode.value === 'MANUAL';
  const sourceLength = Number(activeSource.value.outputLength || 0);
  const sliceLength = isManual ? undefined : Number(generateForm.segmentLength || 0);
  if (!isManual) {
    if (sourceLength <= 0) {
      generateError.value = '来源可分切长度必须大于0';
      return null;
    }
    if (!sliceLength || sliceLength <= 0) {
      generateError.value = '每片长度必须大于0';
      return null;
    }
  }
  const totalCount = !isManual && sliceLength ? Math.max(1, Math.floor(sourceLength / sliceLength)) : 0;
  const sliceCount = isManual
    ? Number(generateForm.sliceCount || 0)
    : Math.max(0, totalCount - getSourceSlices(activeSource.value.adhesiveReportId).length);
  if (sliceCount < 1) {
    generateError.value = '本次切片数量必须大于0';
    return null;
  }
  const startSerialNo = Number(generateForm.startSerialNo || 0);
  if (!Number.isInteger(startSerialNo) || startSerialNo <= 0) {
    generateError.value = '起始流水号必须是大于0的整数';
    return null;
  }
  if (hasSliceSerialNoOverlap(activeSource.value, startSerialNo, sliceCount)) {
    generateError.value = '当前起始流水号会产生重复片号，请调整起始流水号';
    return null;
  }
  return {
    cutMode: generateMode.value,
    planId: currentPlan.planId,
    planOperationId: currentPlan.planOperationId,
    sliceCount,
    ...(sliceLength ? { sliceLength } : {}),
    sizeCode: generateForm.sizeCode,
    sizeName: resolveSlittingSizeName(generateForm.sizeCode),
    sourceAdhesiveReportId: activeSource.value.adhesiveReportId,
    startSerialNo,
  };
}

async function submitGenerateSlices(payload: MesHcSlittingConsoleApi.SliceGenerateReq) {
  const isManual = payload.cutMode === 'MANUAL';
  try {
    await generateSlittingConsoleSlices(payload);
  } catch (error: any) {
    const fallbackError = isManual
      ? '人工切片初始化失败，请检查起始流水号和切片数量'
      : '切片初始化失败，请检查起始流水号和每片长度';
    const errorMessage = error?.msg || error?.message || error?.response?.data?.msg || fallbackError;
    generateError.value = String(errorMessage).includes('切片流水号')
      ? '当前起始流水号会产生重复片号，请调整起始流水号'
      : errorMessage;
    return;
  }
  message.success(isManual ? '人工切片已追加' : '自动切片已初始化');
  generateVisible.value = false;
  await loadBoardData();
}

async function handleGenerateSlices() {
  const payload = buildSlittingGeneratePayload();
  if (!payload) return;
  pendingGeneratePayload.value = payload;
  generateAuthVisible.value = true;
}

async function handleGenerateAuthSuccess(authInfo?: any) {
  if (generateAuthExecuting.value) return;
  const payload = pendingGeneratePayload.value;
  if (!payload) {
    generateAuthVisible.value = false;
    return;
  }
  generateAuthExecuting.value = true;
  try {
    message.success(`认证通过：${authInfo?.empName || authInfo?.username || '操作员'}，正在执行${payload.cutMode === 'MANUAL' ? '人工分切' : '自动分切'}`);
    await submitGenerateSlices(payload);
    pendingGeneratePayload.value = null;
  } finally {
    generateAuthExecuting.value = false;
  }
}

function handleGenerateAuthCancel() {
  pendingGeneratePayload.value = null;
  generateAuthExecuting.value = false;
}

async function exportSlittingWorkOrders(rows: MesHcSlittingConsoleApi.SliceItem[]) {
  const validRows = rows.filter((item) => !!item?.id && !!item.sliceSerialNo);
  if (validRows.length === 0) {
    AModal.info({ title: '没有可导出切片', content: '请先初始化切片，再导出工单。' });
    return;
  }
  const printWindow = window.open('', '_blank', 'width=980,height=720');
  if (!printWindow) return;
  const now = buildNowText();
  const planDetail = currentPlan.planId ? await getPlanOrderDetail(currentPlan.planId as any) : {};
  const ticketRows = await Promise.all(
    validRows.map(async (slice) => ({
      confirmer: slice.scannerName || '',
      currentSection: '分切',
      endTime: slice.scanTime || now,
      materialCode: (planDetail as any)?.materialCode || currentPlan.materialCode,
      metricValue: 1,
      modelCode: (planDetail as any)?.modelCode || currentPlan.modelCode,
      parentBatchNo: getSliceSegmentBatchNo(slice) || currentPlan.sourceProductionBatchNo,
      planNo: slice.planNo || currentPlan.planNo,
      productionBatchNo: slice.sliceSerialNo,
      qrDataUrl: await createPrintQrDataUrl(
        buildTransferTicketQrValue(slice.planNo || currentPlan.planNo, slice.sliceSerialNo || currentPlan.planNo),
      ),
      recorder: slice.scannerName || '',
      remark: [
        `所属分段批号:${getSliceSegmentBatchNo(slice)}`,
        `位置:${getSlicePositionText(slice)}`,
        `综合判断:${selfCheckText(slice.selfCheck)}`,
        slice.remark ? `备注:${slice.remark}` : '',
      ]
        .filter(Boolean)
        .join(' '),
      segmentMark: '',
      startTime: slice.scanTime || now,
    })),
  );
  printWindow.document.write(
    buildWorkOrderTicketHtml(ticketRows, {
      materialCode: (planDetail as any)?.materialCode || currentPlan.materialCode,
      modelCode: (planDetail as any)?.modelCode || currentPlan.modelCode,
      operations: resolvePlanPrintOperations(planDetail as Record<string, any>),
      planNo: currentPlan.planNo,
      printTime: now,
    }),
  );
  printWindow.document.close();
  printWindow.focus();
  window.setTimeout(() => {
    printWindow.print();
    printWindow.close();
  }, 300);
}

async function buildSlittingTransferTicketPayload(rows: MesHcSlittingConsoleApi.SliceItem[]) {
  const now = buildNowText();
  return buildSharedSlittingTransferTicketPayload(rows.map((slice) => {
    const source = getSliceSource(slice);
    return {
      materialCode: source?.materialCode || currentPlan.materialCode,
      modelCode: source?.modelCode || currentPlan.modelCode,
      planNo: slice.planNo || currentPlan.planNo,
      recorderName: slice.scannerName,
      segmentBatchNo: getSliceSegmentBatchNo(slice),
      sliceSerialNo: slice.sliceSerialNo,
      ticketId: slice.id,
      workTime: slice.scanTime || now,
    };
  }), now);
}

async function sendSlittingTransferTicketsToPrintAgent(rows: MesHcSlittingConsoleApi.SliceItem[]) {
  const payload = await buildSlittingTransferTicketPayload(rows);
  const endpoint = rows.length > 1 ? '/print/transfer-tickets' : '/print/transfer-ticket';
  const response = await fetch(`${SLITTING_PRINT_AGENT_URL}${endpoint}`, {
    body: JSON.stringify(payload),
    headers: {
      'Content-Type': 'application/json',
    },
    method: 'POST',
  });
  const result = await response.json().catch(() => ({}));
  if (!response.ok || !result?.success) {
    throw new Error(result?.message || `本机打印服务返回异常：${response.status}`);
  }
  return result;
}

async function printSlittingTransferTickets(rows: MesHcSlittingConsoleApi.SliceItem[]) {
  const validRows = rows.filter((item) => !!item?.id && !!item.sliceSerialNo);
  if (validRows.length === 0) {
    AModal.info({ title: '没有可打印切片', content: '请先初始化切片，再打印流转单。' });
    return false;
  }
  try {
    const result = await sendSlittingTransferTicketsToPrintAgent(validRows);
    await markSlittingConsoleSlicesPrinted(validRows.map((item) => item.id));
    await loadBoardData();
    message.success(`流转单已发送到 ${result.printerName || '配置打印机'}，数量：${result.printedCount || validRows.length}。`);
    return true;
  } catch (error: any) {
    AModal.warning({
      content: `未能连接或确认本机打印服务：${error?.message || error}。请先启动工序流转单 Python 打印服务。`,
      title: '打印流转单失败',
    });
    return false;
  }
}

async function handleExportAllWorkOrders() {
  if (slices.value.length === 0) {
    message.warning('请先初始化切片');
    return;
  }
  await exportSlittingWorkOrders(slices.value);
}

function buildSliceDetailExcelColumns(): SliceDetailExcelColumn[] {
  return [
    { title: '切片号', getValue: (row) => row.sliceSerialNo || '' },
    { title: '分段批号', getValue: (row) => getSliceSegmentBatchNo(row) },
    { title: '位置', getValue: (row) => getSlicePositionText(row) },
    ...visualItemNames.map((itemName) => ({
      title: itemName,
      getValue: (row: MesHcSlittingConsoleApi.SliceItem) => (isVisualItemOn(row, itemName) ? '是' : ''),
    })),
    { title: '综合判断', getValue: (row) => (hasVisualIssue(row) ? '异常' : selfCheckText(row.selfCheck)) },
    { title: '备注说明', getValue: (row) => row.remark || '' },
    { title: '扫码确认日期', getValue: (row) => formatScanConfirmDate(row) },
  ];
}

function escapeExcelCell(value: unknown) {
  return String(value ?? '')
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;');
}

function buildSliceDetailExcelHtml(rows: MesHcSlittingConsoleApi.SliceItem[]) {
  const columns = buildSliceDetailExcelColumns();
  const headerCells = columns.map((column) => `<th>${escapeExcelCell(column.title)}</th>`).join('');
  const bodyRows = rows
    .map((row) => `<tr>${columns.map((column) => `<td>${escapeExcelCell(column.getValue(row))}</td>`).join('')}</tr>`)
    .join('');
  return `\uFEFF<!DOCTYPE html>
<html xmlns:o="urn:schemas-microsoft-com:office:office" xmlns:x="urn:schemas-microsoft-com:office:excel">
<head>
  <meta charset="UTF-8" />
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
  <!--[if gte mso 9]><xml><x:ExcelWorkbook><x:ExcelWorksheets><x:ExcelWorksheet><x:Name>分切明细</x:Name><x:WorksheetOptions><x:DisplayGridlines /></x:WorksheetOptions></x:ExcelWorksheet></x:ExcelWorksheets></x:ExcelWorkbook></xml><![endif]-->
  <style>
    table { border-collapse: collapse; font-family: "Microsoft YaHei", Arial, sans-serif; font-size: 12px; }
    th, td { border: 1px solid #999; padding: 6px 8px; mso-number-format: "\\@"; white-space: nowrap; }
    th { background: #dbeafe; color: #0f172a; font-weight: 700; text-align: center; }
  </style>
</head>
<body>
  <table>
    <thead><tr>${headerCells}</tr></thead>
    <tbody>${bodyRows}</tbody>
  </table>
</body>
</html>`;
}

function buildSliceDetailExcelFileName() {
  const planNo = currentPlan.planNo || '未选计划';
  const segmentBatchNo = filterForm.segmentBatchNo.trim() || currentSegmentBatchNo.value || '全部分段';
  return `分切明细_${planNo}_${segmentBatchNo}_${dayjs().format('YYYYMMDDHHmmss')}.xls`.replace(/[\\/:*?"<>|]/gu, '_');
}

function handleExportSliceDetailExcel() {
  const rows = filteredSlices.value;
  if (rows.length === 0) {
    message.warning('当前筛选条件下没有可导出的分切明细');
    return;
  }
  sliceDetailExporting.value = true;
  try {
    const html = buildSliceDetailExcelHtml(rows);
    const blob = new Blob([html], { type: 'application/vnd.ms-excel;charset=utf-8' });
    downloadFileFromBlobPart({
      fileName: buildSliceDetailExcelFileName(),
      source: blob,
    });
    message.success(`已导出 ${rows.length} 条分切明细`);
  } finally {
    sliceDetailExporting.value = false;
  }
}

async function handlePrintSlice(slice: MesHcSlittingConsoleApi.SliceItem | Record<string, any>) {
  if (warnPrintBlockedWork(getSliceSource(slice))) return;
  await printSlittingTransferTickets([slice as MesHcSlittingConsoleApi.SliceItem]);
}

function openTransferPrintSelector() {
  if (warnPrintBlockedWork()) return;
  transferPrintSelectionMode.value = true;
  activeBoardTab.value = 'VISUAL';
  visualMaximized.value = true;
  if (slices.value.length === 0) {
    // message.warning('请先初始化切片后再选择打印流转单');
  }
}

function closeTransferPrintSelector() {
  transferPrintSelectionMode.value = false;
  selectedTransferSliceIds.value = [];
}

function toggleTransferSliceSelection(slice: MesHcSlittingConsoleApi.SliceItem | Record<string, any>) {
  if (warnPrintBlockedWork(getSliceSource(slice))) return;
  const key = getTransferSliceKey(slice);
  if (!key || !slice.sliceSerialNo) {
    message.warning('当前切片缺少流水号，不能打印流转单');
    return;
  }
  selectedTransferSliceIds.value = selectedTransferSliceIds.value.includes(key)
    ? selectedTransferSliceIds.value.filter((id) => id !== key)
    : [...selectedTransferSliceIds.value, key];
}

function handleVisualSliceClick(slice: MesHcSlittingConsoleApi.SliceItem) {
  if (transferPrintSelectionMode.value) {
    toggleTransferSliceSelection(slice);
    return;
  }
  openSliceViewDialog(slice);
}

function clearTransferSliceSelection() {
  selectedTransferSliceIds.value = [];
}

function selectAllTransferSlices() {
  selectedTransferSliceIds.value = selectableTransferSlices.value.map((item) => getTransferSliceKey(item));
}

function toggleAllTransferSlices() {
  if (allTransferSlicesSelected.value) {
    const visibleKeys = new Set(selectableTransferSlices.value.map((item) => getTransferSliceKey(item)));
    selectedTransferSliceIds.value = selectedTransferSliceIds.value.filter((id) => !visibleKeys.has(id));
    return;
  }
  selectAllTransferSlices();
}

function selectSourceTransferSlices(source: MesHcSlittingConsoleApi.SourceItem) {
  const sourceKeys = getVisualSourceSlices(source.adhesiveReportId)
    .filter((item) => !!item?.id && !!item.sliceSerialNo)
    .map((item) => getTransferSliceKey(item));
  selectedTransferSliceIds.value = Array.from(new Set([...selectedTransferSliceIds.value, ...sourceKeys]));
}

function getOneClickConfirmPendingSlices(source: MesHcSlittingConsoleApi.SourceItem) {
  return getSourceSlices(source.adhesiveReportId).filter((item) => !!item?.id && !!item.sliceSerialNo && item.scanStatus !== 'CONFIRMED');
}

function getOneClickScanConfirmTitle(source: MesHcSlittingConsoleApi.SourceItem) {
  if (!canOneClickScanConfirm.value) return '仅 hcadmin 可点击';
  const pendingCount = getOneClickConfirmPendingSlices(source).length;
  return pendingCount > 0 ? `一键确认当前段 ${pendingCount} 片` : '当前段没有待确认切片';
}

function buildOneClickScanVisualItems(slice: MesHcSlittingConsoleApi.SliceItem) {
  const parsed = parseVisualItems(slice);
  return visualItemNames.map((itemName) => {
    const matched = parsed.find((item) => item.itemName === itemName);
    return {
      itemName,
      remark: matched?.remark || '',
      result: normalizeVisualResult(matched?.result),
    };
  });
}

function buildOneClickScanConfirmPayload(slice: MesHcSlittingConsoleApi.SliceItem): MesHcSlittingConsoleApi.SliceConfirmReq {
  const sizeCode = resolveSliceSizeCode(slice);
  return {
    id: slice.id,
    remark: slice.remark,
    scannedSliceNo: String(slice.sliceSerialNo || '').trim(),
    selfCheck: hasVisualIssue(slice) ? 'NG' : 'OK',
    sizeCode,
    sizeName: resolveSlittingSizeName(sizeCode),
    visualItems: buildOneClickScanVisualItems(slice),
  };
}

async function handleOneClickScanConfirmSource(source: MesHcSlittingConsoleApi.SourceItem) {
  if (!canOneClickScanConfirm.value) {
    message.warning('仅 hcadmin 可执行一键扫码确认');
    return;
  }
  if (warnReadonlyWork('一键扫码确认', source)) return;
  const pendingSlices = getOneClickConfirmPendingSlices(source);
  if (pendingSlices.length === 0) {
    message.info('当前段没有待确认切片');
    return;
  }
  const segmentBatchNo = getSourceSegmentBatchNo(source) || '当前段';
  AModal.confirm({
    cancelText: '取消',
    content: `将按切片流水号自动扫码确认 ${pendingSlices.length} 片，确认后等同逐片扫码入账。确定继续吗？`,
    okText: '一键确认',
    title: `一键扫码确认${segmentBatchNo}`,
    async onOk() {
      oneClickScanConfirmingSourceId.value = source.adhesiveReportId;
      try {
        for (const slice of pendingSlices) {
          await confirmSlittingConsoleSlice(buildOneClickScanConfirmPayload(slice));
        }
        message.success(`${segmentBatchNo} 已完成 ${pendingSlices.length} 片一键扫码确认`);
        await loadBoardData();
      } finally {
        oneClickScanConfirmingSourceId.value = null;
      }
    },
  });
}

async function handlePrintSelectedTransferTickets() {
  if (selectedTransferSliceCount.value === 0) {
    message.warning('请先选择要打印的切片');
    return;
  }
  if (selectedTransferSlices.value.some((item) => isSlicePrintBlocked(item))) {
    message.warning('当前存在已暂停或已作废的分切片，不能打印流转单');
    return;
  }
  const printed = await printSlittingTransferTickets(selectedTransferSlices.value);
  if (printed) closeTransferPrintSelector();
}

function handleDeleteSlice(slice: MesHcSlittingConsoleApi.SliceItem | Record<string, any>) {
  if (!slice?.id) {
    message.warning('未找到要删除的切片记录');
    return;
  }
  if (normalizeSliceStatus(slice.scanStatus) === 'CONFIRMED') {
    message.warning('切片已确认入账，不能删除');
    return;
  }
  if (warnReadonlyWork('删除切片', getSliceSource(slice))) return;
  const sliceNo = String(slice.sliceSerialNo || '-');
  const printed = slice.printStatus === 'PRINTED' || Number(slice.printCount || 0) > 0;
  AModal.confirm({
    cancelText: '取消',
    content: printed
      ? `切片 ${sliceNo} 已打印流转单。删除的是本次切片记录，片号后续可重新生成；请立即销毁或隔离现有标签，避免旧标签被误用于新切片。`
      : `确认删除未扫码确认的切片 ${sliceNo} 吗？删除的是本次切片记录，片号后续可重新使用。`,
    okButtonProps: { danger: true },
    okText: '确认删除',
    title: '删除未确认切片',
    async onOk() {
      await deleteSlittingConsoleSlice(Number(slice.id));
      message.success(`切片 ${sliceNo} 已删除`);
      await loadBoardData();
    },
  });
}

function openPendingSliceConfirm() {
  if (warnReadonlyWork('扫码确认')) return;
  if (slices.value.length === 0) {
    message.warning('请先初始化切片');
    return;
  }
  sliceScanForm.scanCode = '';
  sliceScanForm.message = '';
  sliceScanVisible.value = true;
  nextTick(() => {
    document.querySelector<HTMLInputElement>('.slitting-scan-lookup input')?.focus();
  });
}

function fillSliceDialogForm(slice: MesHcSlittingConsoleApi.SliceItem | Record<string, any>, scannedSliceNo = '') {
  activeSlice.value = slice as MesHcSlittingConsoleApi.SliceItem;
  lastSliceAdjustRemark = '';
  confirmForm.originalSliceNo = String(slice.sliceSerialNo || '').trim();
  confirmForm.scannedSliceNo = scannedSliceNo;
  confirmForm.selfCheck = (slice.selfCheck === 'NG' ? 'NG' : 'OK') as 'NG' | 'OK';
  confirmForm.sizeCode = resolveSliceSizeCode(slice);
  confirmForm.remark = slice.remark || '';
  confirmForm.visualItems = visualItemNames.map((itemName) => ({
    itemName,
    remark: '',
    result: 'OK',
  }));
  try {
    const parsed = parseVisualItems(slice);
    if (parsed.length > 0) {
      confirmForm.visualItems = visualItemNames.map((itemName) => {
        const matched = parsed.find((item: any) => item.itemName === itemName);
        return {
          itemName,
          remark: matched?.remark || '',
          result: normalizeVisualResult(matched?.result),
        };
      });
    }
  } catch {
    // 历史数据异常时不阻断扫码确认。
  }
  if (confirmForm.visualItems.some((item) => isVisualItemActive(item))) {
    confirmForm.selfCheck = 'NG';
  }
  syncSliceAdjustRemark();
}

function toggleVisualItem(item: VisualItem) {
  if (sliceDialogMode.value === 'view') return;
  item.result = isVisualItemActive(item) ? 'OK' : 'NG';
  item.remark = '';
  confirmForm.selfCheck = hasActiveVisualItems.value ? 'NG' : 'OK';
}

function openSliceViewDialog(slice: MesHcSlittingConsoleApi.SliceItem | Record<string, any>) {
  sliceDialogMode.value = 'view';
  fillSliceDialogForm(slice);
  confirmVisible.value = true;
  if (!slice.editBlockedReason && !isSliceReadonly(slice)) {
    message.info('默认只允许查看；请点击右上角扫码确认，扫码命中后才能修改并确认。');
  }
  nextTick(() => {
    document.querySelector<HTMLInputElement>('.slitting-confirm-scan')?.focus();
  });
}

function openAbnormalCategoryCorrection() {
  if (!canCorrectAbnormalCategory.value) {
    message.warning('当前账号没有修正异常类别权限，或当前切片没有本工序外观异常类别');
    return;
  }
  abnormalCategoryCorrectionForm.category = getActiveVisualCategoryItems()[0] || '';
  abnormalCategoryCorrectionForm.reason = '';
  abnormalCategoryCorrectionVisible.value = true;
}

function closeAbnormalCategoryCorrection() {
  if (abnormalCategoryCorrectionSaving.value) return;
  abnormalCategoryCorrectionVisible.value = false;
}

async function submitAbnormalCategoryCorrection() {
  const currentSlice = activeSlice.value;
  if (!currentSlice?.id) {
    message.warning('请先打开已保存的分切切片详情');
    return;
  }
  if (!abnormalCategoryCorrectionForm.category) {
    message.warning('请选择修正后的异常类别');
    return;
  }
  const reason = abnormalCategoryCorrectionForm.reason.trim();
  if (!reason) {
    message.warning('请输入修正原因');
    return;
  }
  abnormalCategoryCorrectionSaving.value = true;
  try {
    const updatedSlice = await correctSlittingConsoleSliceAbnormalCategory({
      category: abnormalCategoryCorrectionForm.category,
      id: Number(currentSlice.id),
      reason,
    });
    await loadBoardData();
    const refreshedSlice = slices.value.find((item) => Number(item.id) === Number(updatedSlice.id)) || updatedSlice;
    sliceDialogMode.value = 'view';
    fillSliceDialogForm(refreshedSlice);
    abnormalCategoryCorrectionVisible.value = false;
    message.success('异常类别已修正，统计归类将按新类别计算');
  } catch (error: any) {
    message.error(error?.message || '修正异常类别失败');
  } finally {
    abnormalCategoryCorrectionSaving.value = false;
  }
}

function openConfirmDialog(slice: MesHcSlittingConsoleApi.SliceItem | Record<string, any>, scannedSliceNo = '') {
  if (warnReadonlyWork('扫码确认', getSliceSource(slice))) {
    openSliceViewDialog(slice);
    return;
  }
  if (slice.editBlockedReason) {
    openSliceViewDialog(slice);
    return;
  }
  sliceDialogMode.value = 'confirm';
  fillSliceDialogForm(slice, scannedSliceNo);
  confirmVisible.value = true;
}

function handleSliceScanLookup() {
  if (warnReadonlyWork('扫码确认')) return;
  const scanned = resolveTransferTicketQrBusinessNo(normalizeConfirmScanCode(sliceScanForm.scanCode));
  if (!scanned) {
    sliceScanForm.message = '请扫描或输入切片流水号';
    return;
  }
  const scannedBaseNo = stripSlittingSizeSuffix(scanned);
  const matched = filteredSlices.value.find((item) => {
    const sliceNo = String(item.sliceSerialNo || '').trim();
    return sliceNo.toLowerCase() === scanned.toLowerCase()
      || stripSlittingSizeSuffix(sliceNo) === scannedBaseNo;
  });
  if (!matched) {
    sliceScanForm.message = isSliceFiltering.value
      ? `当前过滤结果中未找到切片流水号：${scanned}`
      : `未找到切片流水号：${scanned}`;
    return;
  }
  sliceScanVisible.value = false;
  sliceScanForm.message = '';
  openConfirmDialog(matched, scanned);
}

async function handleSubmitSliceDialog() {
  if (!activeSlice.value?.id) return;
  if (sliceDialogMode.value === 'view') return;
  if (warnReadonlyWork('扫码确认', getSliceSource(activeSlice.value))) return;
  if (sliceDialogMode.value === 'confirm' && !confirmForm.scannedSliceNo.trim()) {
    message.warning('请扫码或输入切片流水号');
    return;
  }
  const targetSliceNo = confirmTargetSliceNo.value === '-' ? '' : confirmTargetSliceNo.value;
  const remark = buildSlittingSliceAdjustRemark(confirmForm.remark, confirmForm.originalSliceNo, targetSliceNo);
  const payload = {
    id: activeSlice.value.id,
    remark,
    scannedSliceNo: confirmForm.scannedSliceNo.trim() || undefined,
    selfCheck: hasActiveVisualItems.value ? 'NG' : confirmForm.selfCheck,
    sizeCode: confirmForm.sizeCode,
    sizeName: resolveSlittingSizeName(confirmForm.sizeCode),
    visualItems: confirmForm.visualItems,
  };
  await confirmSlittingConsoleSlice(payload);
  message.success('扫码确认完成，请继续扫描下一张');
  confirmVisible.value = false;
  await loadBoardData();
  sliceScanForm.scanCode = '';
  sliceScanForm.message = '扫码确认完成，请继续扫描下一张';
  sliceScanVisible.value = true;
  await nextTick();
  document.querySelector<HTMLInputElement>('.slitting-scan-lookup input')?.focus();
}

function updateSliceTableHeight() {
  nextTick(() => {
    if (sliceTableResizeObserver && sliceTableWrapRef.value) {
      sliceTableResizeObserver.observe(sliceTableWrapRef.value);
    }
    const wrap = sliceTableWrapRef.value;
    const height = wrap?.clientHeight || 0;
    const headerHeight = wrap?.querySelector<HTMLElement>('.ant-table-thead')?.getBoundingClientRect().height || 40;
    const paginationHeight = wrap?.querySelector<HTMLElement>('.ant-pagination')?.getBoundingClientRect().height || 42;
    sliceTableScrollY.value = Math.max(160, Math.floor(height - headerHeight - paginationHeight - 18));
  });
}

function printStatusText(status?: string) {
  return status === 'PRINTED' ? '已打印' : '未打印';
}

function scanStatusText(status?: string) {
  return status === 'CONFIRMED' ? '已确认' : '待确认';
}

function selfCheckText(status?: string) {
  if (status === 'NG') return '异常';
  if (status === 'OK') return '合格';
  return '待检';
}

function selfCheckColor(status?: string) {
  if (status === 'NG') return 'red';
  if (status === 'OK') return 'green';
  return 'orange';
}

function setFilterStatus(value: string) {
  filterForm.status = value;
}

watch(activeBoardTab, async (tab) => {
  if (tab !== 'VISUAL') {
    closeTransferPrintSelector();
  }
  await nextTick();
  if (tab === 'SLICES') updateSliceTableHeight();
  window.dispatchEvent(new Event('resize'));
});

watch(slices, () => {
  const existingIds = new Set(slices.value.map((item) => getTransferSliceKey(item)).filter(Boolean));
  selectedTransferSliceIds.value = selectedTransferSliceIds.value.filter((id) => existingIds.has(id));
});

watch(
  () => [workListFilters.planNo, workListFilters.modelCode, workListFilters.batchNo, workListFilters.status],
  () => {
    workListPage.value = 1;
  },
);

watch(
  () => [workListFilteredRows.value.length, workListPageSize.value],
  () => {
    const maxPage = Math.max(1, Math.ceil(workListFilteredRows.value.length / workListPageSize.value));
    if (workListPage.value > maxPage) workListPage.value = maxPage;
  },
);

watch(scanPlanNo, (value) => scheduleScannerPlanScan(value));

watch(
  () => filterForm.scanConfirmDate,
  () => {
    if (currentPlan.planOperationId) {
      void loadBoardData();
    }
  },
);

onActivated(() => {
  attachGlobalScannerListener();
  if (skipInitialActivation) {
    skipInitialActivation = false;
    return;
  }
  if (currentPlan.planOperationId) {
    void loadBoardData();
  }
});

onMounted(async () => {
  attachGlobalScannerListener();
  timer = setInterval(() => {
    currentDateTime.value = dayjs().format('YYYY-MM-DD HH:mm:ss');
  }, 1000);
  window.addEventListener('resize', updateSliceTableHeight);
  if (typeof ResizeObserver !== 'undefined') {
    sliceTableResizeObserver = new ResizeObserver(updateSliceTableHeight);
    if (sliceTableWrapRef.value) sliceTableResizeObserver.observe(sliceTableWrapRef.value);
  }
  const queryPlanNo = String(route.query.planNo || '').trim();
  if (queryPlanNo) {
    scanPlanNo.value = queryPlanNo;
    await handleScanPlan();
  }
  updateSliceTableHeight();
  focusPlanScanInput();
});

onDeactivated(() => {
  detachGlobalScannerListener();
});

onBeforeUnmount(() => {
  if (timer) clearInterval(timer);
  if (planScanTimer) clearTimeout(planScanTimer);
  detachGlobalScannerListener();
  window.removeEventListener('resize', updateSliceTableHeight);
  sliceTableResizeObserver?.disconnect();
});
</script>

<template>
  <Page auto-content-height class="slitting-console-page" :loading="boardLoading">
    <div class="slitting-console-shell" :class="{ 'is-visual-maximized': visualMaximized, 'has-sample-lock': !!upstreamSampleWarning }">
      <div v-if="upstreamSampleWarning" role="alert" class="sample-lock-notice rounded border border-amber-300 bg-amber-50 p-3 text-amber-800">
        {{ upstreamSampleWarning }}
      </div>
      <div class="prototype-banner shrink-0 bg-white rounded-xl shadow-sm border border-slate-200 flex p-3 relative overflow-hidden">
        <div class="flex flex-1 items-center gap-4 min-w-0 pl-1">
          <div class="w-[60px] h-[60px] bg-gradient-to-br from-sky-500 to-blue-600 rounded-xl shadow-md flex items-center justify-center shrink-0 text-white">
            <IconifyIcon icon="lucide:scissors" class="text-[32px]" />
          </div>
          <div class="flex flex-col justify-center gap-1.5 min-w-0">
            <div class="flex items-center w-max">
              <span class="text-xl font-black text-slate-800 tracking-wide truncate">分切操作看板</span>
              <Tag color="processing" class="ml-3 !border-none font-bold shadow-sm h-5 flex items-center">分切</Tag>
            </div>
            <div class="flex items-center gap-2 flex-wrap">
              <div class="flex items-center text-xs bg-slate-50 px-2 py-1 rounded border border-slate-200">
                <span class="font-bold text-indigo-600 mr-1.5">产线</span>
                <span class="font-bold text-slate-700">{{ currentPlan.workCenterName || '-' }}</span>
              </div>
            </div>
          </div>
        </div>

        <div v-if="showExecutionClock" class="work-time-card">
          <div>{{ currentDateText }}</div>
          <strong>{{ currentTimeText }}</strong>
        </div>

        <div class="flex items-center gap-2 pl-5 border-l border-slate-100 shrink-0">
          <div
            class="w-[64px] h-[64px] bg-amber-50 border border-amber-200 text-amber-700 rounded-xl flex flex-col items-center justify-center cursor-pointer hover:bg-amber-100 hover:shadow-md transition-all active:scale-95"
            @click="openWorkListDialog"
          >
            <IconifyIcon icon="lucide:list-start" class="text-[22px] mb-1 opacity-80" />
            <span class="text-[11px] font-bold">待加工</span>
          </div>
          <div
            v-if="!isCurrentWorkReadonly"
            class="w-[64px] h-[64px] bg-indigo-50 border border-indigo-200 text-indigo-700 rounded-xl flex flex-col items-center justify-center cursor-pointer hover:bg-indigo-100 hover:shadow-md transition-all active:scale-95"
            :class="{ 'is-disabled': slices.length === 0 }"
            @click="slices.length > 0 && handleExportAllWorkOrders()"
          >
            <IconifyIcon icon="lucide:file-down" class="text-[22px] mb-1 opacity-80" />
            <span class="text-[11px] font-bold">导出工单</span>
          </div>
          <div
            v-if="!isCurrentWorkPrintBlocked"
            class="w-[64px] h-[64px] bg-violet-50 border border-violet-200 text-violet-700 rounded-xl flex flex-col items-center justify-center cursor-pointer hover:bg-violet-100 hover:shadow-md transition-all active:scale-95"
            @click="openTransferPrintSelector"
          >
            <IconifyIcon icon="lucide:printer" class="text-[22px] mb-1 opacity-80" />
            <span class="text-[11px] font-bold">打印流转单</span>
          </div>
          <div
            v-if="!isCurrentWorkReadonly"
            class="w-[64px] h-[64px] bg-cyan-50 border border-cyan-200 text-cyan-700 rounded-xl flex flex-col items-center justify-center cursor-pointer hover:bg-cyan-100 hover:shadow-md transition-all active:scale-95"
            :class="{ 'is-disabled': slices.length === 0 }"
            @click="slices.length > 0 && openPendingSliceConfirm()"
          >
            <IconifyIcon icon="lucide:scan-line" class="text-[22px] mb-1 opacity-80" />
            <span class="text-[11px] font-bold">扫码确认</span>
          </div>
          <div
            v-if="!isCurrentWorkReadonly"
            class="w-[64px] h-[64px] bg-orange-50 border border-orange-200 text-orange-700 rounded-xl flex flex-col items-center justify-center cursor-pointer hover:bg-orange-100 hover:shadow-md transition-all active:scale-95"
            :class="{ 'is-disabled': !hasPlan || sources.length === 0 }"
            @click="hasPlan && sources.length > 0 && openTopGenerateDialog('MANUAL')"
          >
            <IconifyIcon icon="lucide:hand" class="text-[22px] mb-1 opacity-80" />
            <span class="text-[11px] font-bold">人工切片</span>
          </div>
          <div
            v-if="!isCurrentWorkReadonly"
            class="w-[64px] h-[64px] bg-teal-50 border border-teal-200 text-teal-700 rounded-xl flex flex-col items-center justify-center cursor-pointer hover:bg-teal-100 hover:shadow-md transition-all active:scale-95"
            :class="{ 'is-disabled': !hasPlan || sources.length === 0 }"
            @click="hasPlan && sources.length > 0 && openTopGenerateDialog('AUTO')"
          >
            <IconifyIcon icon="lucide:grid-2x2-plus" class="text-[22px] mb-1 opacity-80" />
            <span class="text-[11px] font-bold">自动切片</span>
          </div>
          <div
            v-if="!isCurrentWorkReadonly"
            class="w-[64px] h-[64px] bg-green-50 border border-green-200 text-green-700 rounded-xl flex flex-col items-center justify-center cursor-pointer hover:bg-green-100 hover:shadow-md transition-all active:scale-95"
            :class="{ 'is-disabled': !hasPlan || !currentSource || sourceCompleting || sourceCompleteAuthVisible }"
            @click="hasPlan && currentSource && !sourceCompleting && !sourceCompleteAuthVisible && handleCompleteCurrentSource()"
          >
            <IconifyIcon :icon="sourceCompleting ? 'lucide:loader-circle' : 'lucide:circle-check-big'" class="text-[22px] mb-1 opacity-80" />
            <span class="text-[11px] font-bold">切片完成</span>
          </div>
          <div
            class="w-[64px] h-[64px] bg-emerald-50 border border-emerald-200 text-emerald-700 rounded-xl flex flex-col items-center justify-center cursor-pointer hover:bg-emerald-100 hover:shadow-md transition-all active:scale-95"
            @click="activeBoardTab = 'SLICES'"
          >
            <IconifyIcon icon="lucide:list-checks" class="text-[22px] mb-1 opacity-80" />
            <span class="text-[11px] font-bold">今日明细</span>
          </div>
        </div>
      </div>

      <section v-if="!visualMaximized" class="erp-card plan-scan-card">
        <div class="erp-card-title">
          <IconifyIcon icon="lucide:scan-line" />
          当前计划与扫码
          <span v-if="scanError" class="scan-error">{{ scanError }}</span>
        </div>
        <div class="erp-form-grid">
          <label>扫码计划</label>
          <div class="erp-input-line">
            <Input
              ref="planScanInputRef"
              v-model:value="scanPlanNo"
              allow-clear
              class="scan-input"
              placeholder="请扫描或输入计划号"
              @press-enter="handleScanPlan"
            />
          </div>
          <label>计划号</label><strong class="erp-strong">{{ currentPlan.planNo || '请扫码计划号' }}</strong>
          <label>分段批号</label><strong>{{ currentSegmentBatchNo }}</strong>
          <label>产品型号</label><strong>{{ currentPlan.modelCode || '-' }}</strong>
          <label>产品料号</label><strong>{{ currentPlan.materialCode || '-' }}</strong>
          <label>位置(m)</label><strong>{{ currentSegmentRangeText }}</strong>
          <label>可加工(m)</label><strong>{{ formatNumber(currentAvailableSourceLength) }} m</strong>
          <label>执行要求</label><strong>{{ currentPlan.requirements || '暂无执行要求' }}</strong>
        </div>
        <div v-if="currentSegmentSequenceNotice" class="segment-sequence-warning">
          <IconifyIcon icon="lucide:triangle-alert" />
          <div>
            <strong>{{ currentSegmentSequenceNotice.text }}</strong>
            <span>{{ currentSegmentSequenceNotice.tip }}</span>
          </div>
        </div>
      </section>

      <Tabs v-model:active-key="activeBoardTab" class="slitting-tabs">
        <template #rightExtra>
          <Button class="tab-maximize-button" size="small" @click="visualMaximized = !visualMaximized">
            <IconifyIcon :icon="visualMaximized ? 'lucide:minimize-2' : 'lucide:maximize-2'" />
            {{ visualMaximized ? '还原' : '最大化' }}
          </Button>
        </template>
        <TabPane key="VISUAL" tab="分切作业">
          <section
            class="visual-panel slitting-tab-section"
            :class="{ 'visual-panel--blocked': workbenchBlockedReason }"
          >
            <div class="panel-title">
              <IconifyIcon icon="lucide:layout-grid" />
              分切作业
              <div class="panel-filter-bar">
                <DatePicker v-model:value="filterForm.recordDate" allow-clear size="small" value-format="YYYY-MM-DD" placeholder="日期" />
                <DatePicker v-model:value="filterForm.scanConfirmDate" allow-clear size="small" value-format="YYYY-MM-DD" placeholder="扫码确认日期" />
                <Input v-model:value="filterForm.segmentBatchNo" allow-clear size="small" placeholder="分段批号" />
                <div class="status-pill-group">
                  <button
                    v-for="option in filterStatusOptions"
                    :key="option.value"
                    :class="['status-pill', { 'is-active': filterForm.status === option.value }]"
                    type="button"
                    @click="setFilterStatus(option.value)"
                  >
                    {{ option.label }}
                  </button>
                </div>
              </div>
              <div class="panel-metrics">
                <span>来源 {{ sources.length }} 卷</span>
                <span>来源米数 {{ formatNumber(sourceLengthTotal) }} m</span>
                <span>切片 {{ totalSliceCount }}</span>
                <span>已打印 {{ printedSliceCount }}</span>
                <span>已确认 {{ confirmedSliceCount }}</span>
              </div>
            </div>
            <div v-if="transferPrintSelectionMode" class="transfer-print-bar">
              <div class="transfer-print-status">
                <IconifyIcon icon="lucide:mouse-pointer-click" />
                <span>已选 {{ selectedTransferSliceCount }} / 可选 {{ selectableTransferSliceCount }}</span>
              </div>
              <div class="transfer-print-actions">
                <Button size="small" @click="toggleAllTransferSlices">
                  {{ allTransferSlicesSelected ? '取消全选' : '全选' }}
                </Button>
                <Button size="small" @click="clearTransferSliceSelection">清空</Button>
                <Button size="small" type="primary" :disabled="selectedTransferSliceCount === 0" @click="handlePrintSelectedTransferTickets">
                  打印选中
                </Button>
                <Button size="small" @click="closeTransferPrintSelector">退出</Button>
              </div>
            </div>

            <div v-if="!hasPlan" class="empty-hint">
              <IconifyIcon icon="lucide:scan-line" />
              请先扫描计划号，系统将从后台加载该计划粘胶已过站分段批次和切片记录。
            </div>
            <div v-else-if="sources.length === 0" class="empty-hint">
              <IconifyIcon icon="lucide:database-zap" />
              当前计划未加载到粘胶已过站分段批次，不能分切。
            </div>
            <div v-else-if="visualDisplaySources.length === 0" class="empty-hint">
              <IconifyIcon icon="lucide:filter-x" />
              当前过滤条件下没有匹配的切片记录。
            </div>
            <div v-else class="cloth-list" :class="{ 'is-print-select-mode': transferPrintSelectionMode }">
              <div v-for="source in visualDisplaySources" :key="source.adhesiveReportId" class="cloth-source">
                <div class="cloth-source-header">
                  <div>
                    <strong>{{ getSourceSegmentBatchNo(source) || '-' }}</strong>
                    <span>母批 {{ source.sourceBatchNo || '-' }}</span>
                    <span>长度 {{ formatNumber(source.outputLength) }} m</span>
                    <Tag :color="getSlittingQtimeColor(source.qtime)" :title="source.qtime?.message">
                      本批 QTIME {{ getSlittingQtimeText(source.qtime) }}
                    </Tag>
                  </div>
                  <div class="source-actions">
                    <Button size="small" @click="selectActiveSource(source)">选中</Button>
                    <Button
                      v-if="!isWorkSourceReadonly(source)"
                      size="small"
                      :disabled="!canOneClickScanConfirm || getOneClickConfirmPendingSlices(source).length === 0"
                      :loading="oneClickScanConfirmingSourceId === source.adhesiveReportId"
                      :title="getOneClickScanConfirmTitle(source)"
                      @click="handleOneClickScanConfirmSource(source)"
                    >
                      <IconifyIcon icon="lucide:scan-line" class="mr-1" />
                      一键扫码确认
                    </Button>
                    <Button v-if="transferPrintSelectionMode" size="small" @click="selectSourceTransferSlices(source)">全选本段</Button>
                  </div>
                </div>
                <div class="cloth-source-body">
                  <div
                    v-if="getVisualSourceSlices(source.adhesiveReportId).length === 0"
                    class="cloth-whole"
                    :class="{ 'is-print-select-mode': transferPrintSelectionMode }"
                    @click="!transferPrintSelectionMode && openGenerateDialog(source, 'AUTO')"
                  >
                    <span>整块待自动切片</span>
                    <small>点击后输入每片长度，系统计算切片数量并生成流水号</small>
                  </div>
                  <div v-else class="slice-grid" :style="{ gridTemplateColumns: getGridColumns(getVisualSourceSlices(source.adhesiveReportId).length) }">
                    <div
                      v-for="slice in getVisualSourceSlices(source.adhesiveReportId)"
                      :key="slice.id"
                      :class="{ 'has-delete-action': !transferPrintSelectionMode && canDeleteSlice(slice) }"
                      class="slice-card"
                    >
                      <button
                        :class="getSliceCellClass(slice)"
                        :title="getSliceNgTitle(slice)"
                        type="button"
                        @click="handleVisualSliceClick(slice)"
                      >
                        <div class="slice-cell__head">
                          <span class="slice-cell__batch-wrap">
                            <span v-if="transferPrintSelectionMode" class="slice-select-mark">
                              <IconifyIcon :icon="isTransferSliceSelected(slice) ? 'lucide:check' : 'lucide:square'" />
                            </span>
                            <strong class="slice-cell__batch">{{ slice.sliceSerialNo }}</strong>
                            <span
                              v-if="getSliceEditBlockedReason(slice)"
                              class="slice-edit-lock"
                              :title="getSliceEditBlockedReason(slice)"
                              aria-label="后道工序已报工或送检，当前分切记录不可修改"
                            >
                              <IconifyIcon icon="lucide:lock-keyhole" />
                            </span>
                          </span>
                        </div>
                        <span class="slice-cell__status">{{ getSliceStatusText(slice) }}</span>
                        <span v-if="getSlittingSizeChangeText(slice)" class="slice-cell__changeover">
                          <b>改型</b><em>{{ getSlittingSizeChangeText(slice) }}</em>
                        </span>
                        <small>{{ getSliceSelfCheckText(slice) }}</small>
                      </button>
                      <button
                        v-if="!transferPrintSelectionMode && canDeleteSlice(slice)"
                        class="slice-delete-action"
                        title="删除未确认切片"
                        type="button"
                        @click.stop="handleDeleteSlice(slice)"
                      >
                        <IconifyIcon icon="lucide:trash-2" />
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
            <div v-if="workbenchBlockedReason" class="workbench-blocked-mask">
              <div class="workbench-blocked-mask__content">
                <IconifyIcon icon="lucide:octagon-alert" />
                <strong>{{ workbenchBlockedTitle }}</strong>
                <span>{{ workbenchBlockedReason }}</span>
              </div>
            </div>
          </section>
        </TabPane>

        <TabPane key="SLICES" tab="今日分切明细">
          <section class="table-panel slitting-tab-section">
            <div class="panel-title">
              <IconifyIcon icon="lucide:table-2" />
              分切切片明细
              <div class="panel-filter-bar">
                <DatePicker v-model:value="filterForm.recordDate" allow-clear size="small" value-format="YYYY-MM-DD" placeholder="日期" />
                <DatePicker v-model:value="filterForm.scanConfirmDate" allow-clear size="small" value-format="YYYY-MM-DD" placeholder="扫码确认日期" />
                <Input v-model:value="filterForm.segmentBatchNo" allow-clear size="small" placeholder="分段批号" />
                <div class="status-pill-group">
                  <button
                    v-for="option in filterStatusOptions"
                    :key="option.value"
                    :class="['status-pill', { 'is-active': filterForm.status === option.value }]"
                    type="button"
                    @click="setFilterStatus(option.value)"
                  >
                    {{ option.label }}
                  </button>
                </div>
              </div>
              <div class="panel-metrics">
                <Button
                  size="small"
                  :disabled="filteredSlices.length === 0"
                  :loading="sliceDetailExporting"
                  @click="handleExportSliceDetailExcel"
                >
                  <IconifyIcon icon="lucide:file-spreadsheet" class="mr-1" />
                  导出Excel
                </Button>
                <span>切片 {{ totalSliceCount }}</span>
                <span>已打印 {{ printedSliceCount }}</span>
                <span>已确认 {{ confirmedSliceCount }}</span>
              </div>
            </div>
            <div
              ref="sliceTableWrapRef"
              class="slice-table-wrap"
              :style="{ '--slice-table-body-height': `${sliceTableScrollY}px` }"
            >
              <ATable
                bordered
                :columns="sliceColumns"
                :data-source="filteredSlices"
                :pagination="{ pageSize: 20, showSizeChanger: true, showTotal: (total) => `共 ${total} 条` }"
                row-key="id"
                size="small"
                :scroll="{ x: 2370, y: sliceTableScrollY }"
              >
                <template #bodyCell="{ column, record }">
                  <template v-if="String(column.dataIndex).startsWith('visual_')">
                    <span :class="['visual-light-dot', { 'is-active': isVisualItemOn(record, String(column.dataIndex).replace('visual_', '')) }]"></span>
                  </template>
                  <template v-else-if="column.dataIndex === 'sourceProductionBatchNo'">
                    {{ getSliceSegmentBatchNo(record) }}
                  </template>
                  <template v-else-if="column.dataIndex === 'slicePositionText'">
                    {{ getSlicePositionText(record) }}
                  </template>
                  <template v-else-if="column.dataIndex === 'printStatus'">
                    <Tag :color="record.printStatus === 'PRINTED' ? 'blue' : 'default'">{{ printStatusText(record.printStatus) }}</Tag>
                  </template>
                  <template v-else-if="column.dataIndex === 'scanStatus'">
                    <Tag :color="record.scanStatus === 'CONFIRMED' ? 'green' : 'orange'">{{ scanStatusText(record.scanStatus) }}</Tag>
                  </template>
                  <template v-else-if="column.dataIndex === 'scanConfirmDate'">
                    {{ formatScanConfirmDate(record) }}
                  </template>
                  <template v-else-if="column.dataIndex === 'selfCheck'">
                    <Tag :color="hasVisualIssue(record) ? 'red' : selfCheckColor(record.selfCheck)">
                      {{ hasVisualIssue(record) ? '异常' : selfCheckText(record.selfCheck) }}
                    </Tag>
                  </template>
                  <template v-else-if="column.dataIndex === 'action'">
                    <Button size="small" type="link" @click="openSliceViewDialog(record)">查看</Button>
                    <Button v-if="!isSlicePrintBlocked(record)" size="small" type="link" @click="handlePrintSlice(record)">打印当前行</Button>
                    <Button v-if="canDeleteSlice(record)" danger size="small" type="link" @click="handleDeleteSlice(record)">删除</Button>
                  </template>
                </template>
              </ATable>
            </div>
          </section>
        </TabPane>
        <TabPane key="INSTRUCTION_MESSAGES">
          <template #tab>
            <Badge :count="productionInstructionUnreadCount" :offset="[8, -4]" :overflow-count="99" size="small">
              <span>指令消息</span>
            </Badge>
          </template>
          <ProductionInstructionMessageTab
            :context="productionInstructionContext"
            title="分切指令消息"
            @unread-change="handleProductionInstructionUnreadChange"
          />
        </TabPane>
      </Tabs>
    </div>

    <AModal
      v-model:open="workListVisible"
      class="rough-prototype-modal"
      title="待加工来源选择"
      :width="1280"
      :footer="null"
    >
      <div class="console-table-shell console-table-shell--modal task-list-table-shell">
        <div class="rough-grid-toolbar console-record-toolbar">
          <div class="rough-grid-toolbar__title">待加工来源选择</div>
          <div class="pass-work-actions rough-grid-toolbar__actions">
            <span class="console-table-count">筛选 {{ workListFilteredRows.length }} 条 / 共 {{ workListRows.length }} 条</span>
            <Button size="small" :loading="workListLoading" @click="loadWorkList">刷新</Button>
          </div>
        </div>
        <div class="task-list-filter-bar">
          <div class="task-list-filter-item">
            <label>计划号</label>
            <Input
              v-model:value="workListFilters.planNo"
              allow-clear
              placeholder="请输入计划号"
              size="small"
              @press-enter="handleWorkListSearch"
            />
          </div>
          <div class="task-list-filter-item">
            <label>产品型号</label>
            <Input
              v-model:value="workListFilters.modelCode"
              allow-clear
              placeholder="请输入产品型号"
              size="small"
              @press-enter="handleWorkListSearch"
            />
          </div>
          <div class="task-list-filter-item">
            <label>批次</label>
            <Input
              v-model:value="workListFilters.batchNo"
              allow-clear
              placeholder="请输入母卷/批次"
              size="small"
              @press-enter="handleWorkListSearch"
            />
          </div>
          <div class="task-list-filter-item task-list-filter-item--status">
            <label>状态</label>
            <Select
              v-model:value="workListFilters.status"
              :options="workListStatusOptions"
              size="small"
            />
          </div>
          <div class="task-list-filter-actions">
            <Button size="small" type="primary" :loading="workListLoading" @click="handleWorkListSearch">查询</Button>
            <Button size="small" @click="resetWorkListFilters">重置</Button>
          </div>
        </div>
        <div class="console-table-body">
          <ATable
            class="rough-check-table console-record-table work-list-auto-width-table"
            :columns="workListColumns"
            :custom-row="(record) => ({ onDblclick: () => handleSelectWorkTask(record) })"
            :data-source="workListPagedRows"
            :loading="workListLoading"
            :pagination="false"
            row-key="sourceKey"
            size="small"
            :scroll="{ x: workListScrollX, y: 366 }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'sourceProductionBatchNo'">
                {{ getSourceSegmentBatchNo(record) || '-' }}
              </template>
              <template v-else-if="column.dataIndex === 'sourcePositionText'">
                {{ record.sourcePositionText || '-' }}
              </template>
              <template v-else-if="column.dataIndex === 'availableSourceLength'">
                {{ formatNumber(record.availableSourceLength) }}
              </template>
              <template v-else-if="column.dataIndex === 'status'">
                <Tag :color="getWorkListStatusMeta(record).color" class="!m-0">
                  {{ getWorkListStatusMeta(record).text }}
                </Tag>
              </template>
              <template v-else-if="column.dataIndex === 'action'">
                <Button size="small" type="link" @click="handleSelectWorkTask(record)">{{ getWorkListActionText(record.status) }}</Button>
              </template>
            </template>
          </ATable>
        </div>
        <div class="task-list-pagination">
          <Pagination
            v-model:current="workListPage"
            v-model:pageSize="workListPageSize"
            :page-size-options="['10', '20', '50']"
            :show-total="(total) => `共 ${total} 条`"
            :total="workListFilteredRows.length"
            show-less-items
            show-size-changer
            size="small"
          />
        </div>
      </div>
    </AModal>

    <AModal
      v-model:open="generateVisible"
      centered
      destroy-on-close
      :title="generateModalTitle"
      width="680px"
      @ok="handleGenerateSlices"
    >
      <Form layout="vertical">
        <div class="modal-summary">
          <strong>{{ getSourceSegmentBatchNo(activeSource) || '-' }}</strong>
          <span v-if="generateMode === 'AUTO'">来源米数 {{ formatNumber(activeSource?.outputLength) }} m</span>
          <span v-if="generateMode === 'AUTO'">当前已切 {{ formatNumber(getSourceSlices(activeSource?.adhesiveReportId).reduce((sum, item) => sum + Number(item.sliceLength || 0), 0)) }} m</span>
          <span v-else>当前已切 {{ generateCurrentSliceCount }} 片</span>
          <span>预计首片 {{ previewFirstSliceNo }}</span>
        </div>
        <div v-if="generateSegmentSequenceNotice" class="segment-sequence-warning segment-sequence-warning--modal">
          <IconifyIcon icon="lucide:triangle-alert" />
          <div>
            <strong>{{ generateSegmentSequenceNotice.text }}</strong>
            <span>{{ generateSegmentSequenceNotice.tip }}</span>
          </div>
        </div>
        <div class="generate-form-grid">
          <FormItem v-if="generateMode === 'AUTO'" label="每片长度(m)">
            <InputNumber v-model:value="generateForm.segmentLength" :min="0.001" :precision="3" class="full-input" @change="refreshStartSerialNo" />
          </FormItem>
          <FormItem label="起始流水号">
            <InputNumber v-model:value="generateForm.startSerialNo" :min="1" :precision="0" class="full-input" @change="generateError = ''" />
          </FormItem>
          <FormItem class="generate-form-grid__wide" label="片号尾号">
            <RadioGroup v-model:value="generateForm.sizeCode">
              <Radio v-for="option in SLITTING_SIZE_OPTIONS" :key="option.value" :value="option.value">
                {{ option.label }}
              </Radio>
            </RadioGroup>
            <div class="size-rule-hint">计划尺寸 {{ currentPlan.planSizeSpec || '未定义' }}</div>
          </FormItem>
          <FormItem v-if="generateMode === 'AUTO'" label="自动计算切片数">
            <InputNumber
              :value="generateForm.sliceCount"
              disabled
              class="full-input"
            />
          </FormItem>
          <FormItem v-else label="本次切片数量">
            <InputNumber v-model:value="generateForm.sliceCount" :min="1" :precision="0" class="full-input" @change="generateError = ''" />
          </FormItem>
        </div>
        <div v-if="generateError" class="generate-error">{{ generateError }}</div>
      </Form>
    </AModal>

    <AuthModal
      v-model:visible="generateAuthVisible"
      auth-mode="username"
      :action-name="generateAuthActionName"
      :workstation="generateAuthWorkstation"
      title="分切操作认证确认"
      @cancel="handleGenerateAuthCancel"
      @success="handleGenerateAuthSuccess"
    />

    <AuthModal
      v-model:visible="sourceCompleteAuthVisible"
      auth-mode="username"
      :action-name="sourceCompleteAuthActionName"
      :workstation="generateAuthWorkstation"
      title="分切本段完成认证"
      @cancel="handleSourceCompleteAuthCancel"
      @success="handleSourceCompleteAuthSuccess"
    />

    <AModal
      v-model:open="confirmVisible"
      centered
      destroy-on-close
      :footer="sliceDialogMode === 'view' ? null : undefined"
      :keyboard="false"
      :mask-closable="false"
      :ok-text="sliceDialogOkText"
      :title="sliceDialogTitle"
      width="980px"
      @ok="handleSubmitSliceDialog"
    >
      <div v-if="sliceDialogMode === 'view' && activeSlice?.editBlockedReason" class="slice-readonly-hint slice-readonly-hint--blocked">
        {{ activeSlice.editBlockedReason }}
      </div>
      <div v-else-if="sliceDialogMode === 'view'" class="slice-readonly-hint">
        默认只允许查看切片内容；请点击右上角“扫码确认”，扫码命中当前切片后才能修改自检内容并确认入账。
      </div>
      <div class="confirm-head">
        <div>
          <span>当前切片流水号</span>
          <strong>{{ activeSlice?.sliceSerialNo || '-' }}</strong>
        </div>
        <div>
          <span>所属分段批号</span>
          <strong>{{ activeSliceSegmentBatchNo }}</strong>
        </div>
        <div>
          <span>位置</span>
          <strong>{{ activeSlicePositionText }}</strong>
        </div>
      </div>
      <Form layout="vertical">
        <FormItem v-if="sliceDialogMode === 'confirm'" label="扫码切片流水号">
          <Input
            v-model:value="confirmForm.scannedSliceNo"
            class="slitting-confirm-scan"
            readonly
          />
        </FormItem>
        <FormItem label="片号尾号">
          <RadioGroup v-model:value="confirmForm.sizeCode" :disabled="sliceDialogMode === 'view'">
            <Radio v-for="option in SLITTING_SIZE_OPTIONS" :key="option.value" :value="option.value">
              {{ option.label }}
            </Radio>
          </RadioGroup>
          <div class="size-rule-hint">确认后片号 <strong>{{ confirmTargetSliceNo }}</strong></div>
        </FormItem>
        <div class="visual-check-grid">
          <div v-for="item in confirmForm.visualItems" :key="item.itemName" :class="getVisualCheckItemClass(item)">
            <button class="visual-light-button" type="button" :disabled="sliceDialogMode === 'view'" @click="toggleVisualItem(item)">
              <span class="visual-light-dot" :class="{ 'is-active': isVisualItemActive(item) }"></span>
              <span>{{ item.itemName }}</span>
            </button>
          </div>
        </div>
        <FormItem label="综合判定">
          <RadioGroup v-model:value="confirmForm.selfCheck" :disabled="sliceDialogMode === 'view' || hasActiveVisualItems">
            <Radio value="OK">合格</Radio>
            <Radio value="NG">异常</Radio>
          </RadioGroup>
          <div v-if="hasActiveVisualItems" class="visual-check-lock">已点亮缺陷项，综合判定自动为异常，不允许手工改为合格。</div>
        </FormItem>
        <FormItem label="备注">
          <Input v-model:value="confirmForm.remark" allow-clear :disabled="sliceDialogMode === 'view'" />
        </FormItem>
      </Form>
      <div v-if="canCorrectAbnormalCategory" class="abnormal-category-correction-action">
        <Button :loading="abnormalCategoryCorrectionSaving" @click="openAbnormalCategoryCorrection">
          修正异常类别
        </Button>
      </div>
    </AModal>

    <AModal
      v-model:open="abnormalCategoryCorrectionVisible"
      centered
      destroy-on-close
      :confirm-loading="abnormalCategoryCorrectionSaving"
      :mask-closable="false"
      :z-index="5200"
      title="修正分切外观异常类别"
      width="560px"
      @cancel="closeAbnormalCategoryCorrection"
      @ok="submitAbnormalCategoryCorrection"
    >
      <Form layout="vertical">
        <FormItem label="切片流水号">
          <Input :value="activeSlice?.sliceSerialNo || '-'" disabled />
        </FormItem>
        <FormItem label="当前异常类别">
          <Input :value="getActiveVisualCategoryText()" disabled />
        </FormItem>
        <FormItem label="修正后异常类别" required>
          <RadioGroup v-model:value="abnormalCategoryCorrectionForm.category" class="abnormal-category-correction-options">
            <Radio
              v-for="item in abnormalCategoryCorrectionOptions"
              :key="item.value"
              :value="item.value"
            >
              {{ item.label }}
            </Radio>
          </RadioGroup>
        </FormItem>
        <FormItem label="修正原因" required>
          <Input.TextArea
            v-model:value="abnormalCategoryCorrectionForm.reason"
            allow-clear
            placeholder="请输入修正原因"
            :rows="3"
          />
        </FormItem>
      </Form>
    </AModal>

    <AModal
      v-model:open="sliceScanVisible"
      centered
      destroy-on-close
      :keyboard="false"
      :mask-closable="false"
      ok-text="查询并确认"
      title="扫码确认分切切片"
      width="560px"
      @ok="handleSliceScanLookup"
    >
      <Form layout="vertical">
        <FormItem label="扫码/输入切片流水号">
          <Input
            ref="sliceScanLookupInputRef"
            v-model:value="sliceScanForm.scanCode"
            class="slitting-scan-lookup"
            placeholder="请扫描打印流转单上的切片流水号"
            @press-enter="handleSliceScanLookup"
          />
        </FormItem>
        <div v-if="sliceScanForm.message" class="scan-lookup-error">{{ sliceScanForm.message }}</div>
        <div class="scan-lookup-hint">系统只在当前分切作业列表中匹配切片流水号；未匹配时不会加载任何切片数据。</div>
      </Form>
    </AModal>

  </Page>
</template>

<style scoped>
:deep(.vben-page),
:deep(.vben-page-content),
:deep(.vben-page-content-wrapper) {
  height: 100%;
  min-height: 0;
}

.slitting-console-page :deep(.page-content) {
  overflow: hidden;
}

.slitting-console-shell {
  --industrial-border: #8794a4;
  --industrial-card: #e8edf3;
  display: grid;
  box-sizing: border-box;
  height: 100%;
  min-width: 0;
  min-height: 0;
  grid-template-rows:
    max-content
    max-content
    minmax(0, 1fr);
  gap: 8px;
  padding: 8px;
  color: #1f2a37;
  overflow: hidden;
  background:
    linear-gradient(90deg, rgba(30, 41, 59, 0.04) 1px, transparent 1px) 0 0 / 28px 28px,
    linear-gradient(0deg, rgba(30, 41, 59, 0.04) 1px, transparent 1px) 0 0 / 28px 28px,
    linear-gradient(180deg, #dfe6ee 0%, #f4f7fa 42%, #e8edf3 100%);
}

.slitting-console-shell.is-visual-maximized {
  grid-template-rows:
    max-content
    minmax(0, 1fr);
}

.slitting-tabs {
  border: 1px solid var(--industrial-border);
  background: #f8fbff;
}

.console-table-shell {
  display: flex;
  flex-direction: column;
  width: 100%;
  max-width: 100%;
  height: 100%;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
}

.console-table-shell--modal {
  height: 520px;
}

.task-list-table-shell {
  height: 610px;
}

.rough-grid-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  background:
    linear-gradient(90deg, rgba(14, 165, 233, 0.08) 0 1px, transparent 1px) 0 0 / 18px 100%,
    linear-gradient(180deg, #dfe7f0 0%, #cbd5e1 100%);
  border: 1px solid #8794a4;
}

.rough-grid-toolbar__title {
  color: #075985;
  font-size: 15px;
  font-weight: 900;
}

.rough-grid-toolbar__actions {
  display: inline-flex;
  flex-wrap: nowrap;
  justify-content: flex-end;
  min-width: 0;
}

.console-record-toolbar {
  flex: 0 0 38px;
  min-height: 38px;
  padding: 5px 8px;
  margin-bottom: 0;
  border-bottom: 0;
}

.pass-work-actions {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 2px;
}

.console-table-count {
  color: #475569;
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
}

.task-list-filter-bar {
  display: grid;
  grid-template-columns: minmax(150px, 1.1fr) minmax(150px, 1fr) minmax(150px, 1fr) minmax(126px, 0.72fr) auto;
  gap: 8px;
  align-items: center;
  padding: 8px;
  background: linear-gradient(180deg, #f8fafc 0%, #e4ebf3 100%);
  border: 1px solid #8794a4;
  border-top: 0;
}

.task-list-filter-item {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 6px;
  align-items: center;
  min-width: 0;
}

.task-list-filter-item label {
  color: #334155;
  font-size: 12px;
  font-weight: 900;
  white-space: nowrap;
}

.task-list-filter-item :deep(.ant-input),
.task-list-filter-item :deep(.ant-select-selector) {
  border-radius: 2px !important;
}

.task-list-filter-item--status {
  grid-template-columns: auto minmax(96px, 1fr);
}

.task-list-filter-actions {
  display: inline-flex;
  gap: 6px;
  justify-content: flex-end;
  min-width: 0;
}

.console-table-body {
  flex: 1 1 0;
  width: 100%;
  max-width: 100%;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
}

.console-table-body :deep(.ant-table-wrapper),
.console-table-body :deep(.ant-spin-nested-loading),
.console-table-body :deep(.ant-spin-container) {
  width: 100%;
  max-width: 100%;
  height: 100%;
  min-width: 0;
  min-height: 0;
}

.task-list-pagination {
  display: flex;
  flex: 0 0 38px;
  align-items: center;
  justify-content: flex-end;
  padding: 6px 8px 0;
  background: linear-gradient(180deg, rgba(248, 250, 252, 0.72), rgba(226, 232, 240, 0.54));
  border-top: 1px solid rgba(135, 148, 164, 0.56);
}

.rough-check-table {
  width: 100%;
  max-width: 100%;
  height: 100%;
  min-width: 0;
  border: 1px solid #8794a4;
  border-radius: 0 !important;
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.78),
    0 1px 0 rgba(15, 23, 42, 0.08);
}

.rough-check-table :deep(.ant-table) {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  background: #edf2f7;
  border-radius: 0 !important;
}

.console-record-table :deep(.ant-table-container) {
  display: flex;
  flex: 1 1 0;
  flex-direction: column;
  min-height: 0;
  border-radius: 0 !important;
  border-start-start-radius: 0;
  border-start-end-radius: 0;
}

.rough-check-table :deep(.ant-table-content),
.rough-check-table :deep(.ant-table-header),
.rough-check-table :deep(.ant-table-body),
.rough-check-table :deep(.ant-table-cell),
.rough-check-table :deep(.ant-table-thead > tr > th),
.rough-check-table :deep(.ant-table-tbody > tr > td) {
  border-radius: 0 !important;
}

.console-record-table :deep(.ant-table-body) {
  flex: 1 1 0;
  min-height: 0;
  max-height: none !important;
  overflow: auto !important;
}

.work-list-auto-width-table :deep(.ant-table-cell) {
  white-space: nowrap;
}

.work-list-auto-width-table :deep(.ant-table-thead > tr > th),
.work-list-auto-width-table :deep(.ant-table-tbody > tr > td) {
  padding-inline: 8px;
}

.console-record-table :deep(.ant-table-thead > tr > th) {
  color: #263445;
  font-weight: 800;
  background:
    linear-gradient(90deg, rgba(14, 165, 233, 0.08) 0 1px, transparent 1px) 0 0 / 18px 100%,
    linear-gradient(180deg, #d7dee7 0%, #cbd5e1 100%);
  border-bottom: 1px solid #8794a4;
  border-inline-end: 1px solid #a2adba;
}

.console-record-table :deep(.ant-table-tbody > tr > td) {
  color: #172033;
  background: linear-gradient(180deg, #f8fafc 0%, #edf2f7 100%);
  border-bottom: 1px solid #c6d0dc;
  border-inline-end: 1px solid #d7dee8;
}

.console-record-table :deep(.ant-table-tbody > tr:hover > td) {
  background: linear-gradient(180deg, #e0f2fe 0%, #dbeafe 100%);
}

.rough-check-table :deep(.ant-input),
.rough-check-table :deep(.ant-radio-wrapper) {
  font-size: 12px;
}

.prototype-banner {
  min-height: clamp(76px, 8.5vh, 96px);
  margin-bottom: 0;
  color: #0f172a;
  background:
    linear-gradient(90deg, rgba(14, 165, 233, 0.08) 0 1px, transparent 1px) 0 0 / 22px 100%,
    linear-gradient(180deg, #d7dee7 0%, #cbd5e1 100%) !important;
  border-color: #8794a4 !important;
  border-radius: 4px !important;
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.78),
    inset 0 -1px 0 rgba(15, 23, 42, 0.12),
    0 2px 0 rgba(15, 23, 42, 0.14) !important;
}

.prototype-banner :deep(.text-slate-800),
.prototype-banner :deep(.text-slate-700),
.prototype-banner :deep(.text-slate-500) {
  color: #172033 !important;
}

.prototype-banner :deep(.bg-slate-50),
.prototype-banner :deep(.bg-amber-50),
.prototype-banner :deep(.bg-emerald-50),
.prototype-banner :deep(.bg-indigo-50),
.prototype-banner :deep(.bg-cyan-50),
.prototype-banner :deep(.bg-sky-50),
.prototype-banner :deep(.bg-teal-50) {
  background: linear-gradient(180deg, #eef3f8 0%, #dde5ee 100%) !important;
  border-color: #8794a4 !important;
  border-radius: 2px !important;
}

.prototype-banner :deep(.rounded-xl),
.prototype-banner :deep(.rounded),
.prototype-banner :deep(.rounded-md) {
  border-radius: 2px !important;
}

.prototype-banner :deep(.text-indigo-600),
.prototype-banner :deep(.text-indigo-700),
.prototype-banner :deep(.text-cyan-700),
.prototype-banner :deep(.text-sky-700),
.prototype-banner :deep(.text-teal-700) {
  color: #0369a1 !important;
}

.prototype-banner .is-disabled {
  cursor: not-allowed;
  opacity: 0.48;
}

.banner-left,
.banner-actions,
.banner-meta,
.panel-title,
.source-actions,
.cloth-source-header > div {
  display: flex;
  align-items: center;
}

.banner-left,
.banner-meta,
.panel-title {
  gap: 10px;
}

.banner-icon {
  display: grid;
  width: 42px;
  height: 42px;
  place-items: center;
  border: 1px solid #9fb2c8;
  background: #e9f0f7;
  color: #2563eb;
  font-size: 24px;
}

.banner-title {
  color: #102033;
  font-size: 19px;
  font-weight: 800;
}

.banner-meta {
  margin-top: 4px;
  color: #425466;
  font-size: 12px;
}

.banner-actions {
  gap: 12px;
}

.time-card {
  min-width: 180px;
  border-left: 1px solid #a8b6c8;
  padding-left: 16px;
}

.time-card span {
  display: block;
  color: #6b7280;
  font-size: 12px;
}

.time-card strong {
  color: #1d4ed8;
  font-family: Consolas, monospace;
}

.work-time-card {
  display: flex;
  flex: 0 0 118px;
  flex-direction: column;
  justify-content: center;
  height: 62px;
  padding: 0 12px;
  margin-left: 8px;
  font-family: Consolas, 'Microsoft YaHei', monospace;
  color: #075985;
  text-align: center;
  background: transparent;
  border: 0;
  border-radius: 2px;
}

.work-time-card div {
  font-size: 14px;
  font-weight: 800;
  line-height: 20px;
}

.work-time-card strong {
  font-size: 20px;
  font-weight: 900;
  line-height: 24px;
}

.slitting-console-shell :deep(.ant-btn-primary) {
  background: linear-gradient(180deg, #0ea5e9 0%, #0369a1 100%);
  border-color: #075985;
  border-radius: 2px;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.28);
}

.slitting-console-shell :deep(.ant-btn) {
  border-radius: 2px;
}

.slitting-console-shell :deep(.ant-btn[disabled]),
.slitting-console-shell :deep(.ant-btn.ant-btn-disabled) {
  color: #64748b !important;
  text-shadow: none !important;
  background: #e2e8f0 !important;
  border-color: #cbd5e1 !important;
  box-shadow: none !important;
  opacity: 1;
}

.slitting-console-shell :deep(.ant-btn[disabled] *),
.slitting-console-shell :deep(.ant-btn.ant-btn-disabled *) {
  color: #64748b !important;
}

.erp-card {
  min-height: 0;
  padding: 8px 10px;
  overflow: hidden;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.72), rgba(230, 236, 244, 0.76)),
    var(--industrial-card);
  border: 1px solid var(--industrial-border);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.82),
    0 2px 0 rgba(15, 23, 42, 0.1);
}

.erp-card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 30px;
  margin-bottom: 8px;
  color: #075985;
  font-weight: 700;
  letter-spacing: 0.04em;
  text-transform: uppercase;
}

.erp-card-title::after {
  flex: 1;
  height: 1px;
  content: '';
  background: linear-gradient(90deg, rgba(14, 165, 233, 0.55), transparent);
}

.plan-scan-card {
  display: flex;
  flex-direction: column;
  height: auto;
  min-height: 0;
}

.plan-scan-card .erp-form-grid {
  flex: 0 0 auto;
  min-height: 0;
  align-items: stretch;
}

.segment-sequence-warning {
  display: flex;
  gap: 8px;
  align-items: flex-start;
  padding: 7px 10px;
  margin-top: 8px;
  color: #b91c1c;
  background: #fff1f2;
  border: 1px solid #fca5a5;
  box-shadow: inset 3px 0 0 #ef4444;
}

.segment-sequence-warning :deep(.iconify),
.segment-sequence-warning :deep(svg) {
  flex: 0 0 auto;
  margin-top: 2px;
  color: #dc2626;
  font-size: 16px;
}

.segment-sequence-warning strong,
.segment-sequence-warning span {
  display: block;
  font-size: 12px;
  line-height: 18px;
}

.segment-sequence-warning strong {
  font-weight: 900;
}

.segment-sequence-warning span {
  color: #991b1b;
  font-weight: 800;
}

.segment-sequence-warning--modal {
  margin: -4px 0 12px;
}

.erp-form-grid {
  display: grid;
  grid-template-columns: 7fr 18fr 7fr 18fr 7fr 18fr 7fr 18fr;
  gap: 6px 8px;
  align-items: stretch;
}

.erp-form-grid label {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  min-height: 32px;
  padding: 5px 8px;
  color: #334155;
  font-size: 13px;
  font-weight: 600;
  text-align: right;
  background: linear-gradient(180deg, #d7dee7 0%, #cbd5e1 100%);
  border: 1px solid #8794a4;
}

.erp-form-grid strong {
  display: flex;
  align-items: center;
  min-height: 32px;
  padding: 5px 8px;
  overflow: hidden;
  color: #0f172a;
  font-size: 13px;
  line-height: 20px;
  text-overflow: ellipsis;
  white-space: nowrap;
  background: linear-gradient(180deg, #f8fafc 0%, #edf2f7 100%);
  border: 1px solid #a2adba;
  box-shadow: inset 0 1px 2px rgba(15, 23, 42, 0.07);
}

.erp-strong {
  color: #0f172a;
  font-weight: 800;
}

.erp-full-value {
  grid-column: span 5;
}

.erp-wide-value {
  grid-column: span 5;
}

.erp-requirement-value {
  grid-column: span 3;
}

.erp-input-line {
  display: flex;
  gap: 8px;
  align-items: stretch;
  min-height: 32px;
  min-width: 0;
}

.erp-input-line :deep(.ant-input-affix-wrapper) {
  height: 32px;
  border-radius: 2px;
}

.scan-input {
  flex: 1;
  max-width: none;
}

.mother-roll-card {
  display: flex;
  flex-direction: column;
  padding: 5px 10px;
}

.mother-roll-card .erp-card-title {
  height: 24px;
  margin-bottom: 4px;
}

.mother-roll-summary {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-left: 6px;
  color: #334155;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0;
  text-transform: none;
}

.mother-roll-track {
  position: relative;
  height: 10px;
  overflow: hidden;
  border: 1px solid #7f9bb8;
  background:
    repeating-linear-gradient(90deg, rgba(15, 23, 42, 0.08) 0 1px, transparent 1px 20px),
    linear-gradient(180deg, #d6e7f4 0%, #b7d2e7 100%);
}

.mother-roll-base,
.mother-roll-segment,
.mother-roll-sliced {
  position: absolute;
  top: 0;
  bottom: 0;
}

.mother-roll-base {
  right: 0;
  left: 0;
}

.mother-roll-segment {
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 24px;
  color: transparent;
  font-size: 0;
  background: rgba(125, 211, 252, 0.6);
  border-right: 2px solid #0369a1;
  border-left: 2px solid #0369a1;
}

.mother-roll-segment span {
  display: none;
}

.mother-roll-sliced {
  background: rgba(14, 165, 233, 0.45);
  border-right: 3px solid #075985;
  pointer-events: none;
}

.panel-title {
  height: 34px;
  border-bottom: 1px solid #c6d1df;
  padding: 0 10px;
  color: #0f6fce;
  font-weight: 800;
}

.panel-filter-bar {
  display: flex;
  align-items: center;
  gap: 6px;
  flex: 0 0 640px;
  width: 640px;
  min-width: 640px;
  max-width: 640px;
  margin-left: 12px;
}

.panel-filter-bar :deep(.ant-picker) {
  flex: 0 0 126px;
  min-width: 0;
}

.panel-filter-bar :deep(.ant-input-affix-wrapper) {
  flex: 0 0 150px;
  min-width: 0;
}

.status-pill-group {
  display: flex;
  flex: 0 0 322px;
  align-items: center;
  gap: 4px;
  width: 322px;
  min-width: 322px;
  overflow: hidden;
}

.status-pill {
  flex: 0 0 60px;
  height: 24px;
  padding: 0;
  color: #475569;
  font-size: 12px;
  font-weight: 800;
  white-space: nowrap;
  cursor: pointer;
  background: linear-gradient(180deg, #f8fafc 0%, #e2e8f0 100%);
  border: 1px solid #aab8c9;
  border-radius: 999px;
}

.status-pill.is-active {
  color: #fff;
  background: linear-gradient(180deg, #0ea5e9 0%, #0369a1 100%);
  border-color: #075985;
}

.panel-metrics {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-left: auto;
  color: #31445b;
  font-size: 12px;
}

.transfer-print-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 38px;
  padding: 5px 10px;
  background: linear-gradient(180deg, #eef6ff 0%, #dceaf8 100%);
  border-bottom: 1px solid #aebed0;
}

.transfer-print-status,
.transfer-print-actions {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.transfer-print-status {
  color: #0f4f7a;
  font-size: 12px;
  font-weight: 900;
}

.scan-error {
  margin-left: 12px;
  color: #dc2626;
  font-weight: 600;
}

.scan-lookup-error {
  padding: 8px 10px;
  margin-top: -4px;
  color: #b91c1c;
  font-weight: 700;
  background: #fef2f2;
  border: 1px solid #fecaca;
}

.scan-lookup-hint {
  padding: 8px 10px;
  margin-top: 8px;
  color: #475569;
  background: #f8fafc;
  border: 1px solid #cbd5e1;
}

.plan-grid {
  display: grid;
  padding: 8px;
  grid-template-columns: 110px 1.2fr 120px 110px 1fr 110px 1fr;
  gap: 6px;
}

.plan-grid label {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  border: 1px solid #b8c5d6;
  background: #dfe7f1;
  padding: 0 10px;
  color: #334155;
  font-weight: 700;
}

.plan-grid label:last-of-type {
  justify-content: flex-end;
}

.plan-scan-input,
.readonly-cell {
  height: 30px;
}

.readonly-cell {
  display: flex;
  align-items: center;
  border: 1px solid #b8c5d6;
  background: #fff;
  padding: 0 10px;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.readonly-cell.strong {
  color: #0f172a;
  font-weight: 800;
}

.readonly-cell.requirements {
  grid-column: span 6;
}

.visual-panel {
  position: relative;
  display: flex;
  height: 100%;
  min-height: 0;
  flex-direction: column;
}

.visual-panel--blocked .panel-title,
.visual-panel--blocked .transfer-print-bar,
.visual-panel--blocked .empty-hint,
.visual-panel--blocked .cloth-list {
  pointer-events: none;
}

.visual-panel--blocked .cloth-list,
.visual-panel--blocked .empty-hint {
  opacity: 0.42;
  filter: grayscale(0.2);
}

.workbench-blocked-mask {
  position: absolute;
  inset: 0;
  z-index: 20;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding: 44px 24px 24px;
  pointer-events: auto;
  background: rgba(241, 245, 249, 0.28);
}

.workbench-blocked-mask__content {
  display: flex;
  flex-direction: column;
  align-items: center;
  max-width: min(560px, 80%);
  padding: 14px 28px 16px;
  color: #b91c1c;
  text-align: center;
  background: rgba(254, 242, 242, 0.84);
  border: 1px solid rgba(248, 113, 113, 0.55);
}

.workbench-blocked-mask__content :deep(.iconify),
.workbench-blocked-mask__content :deep(svg) {
  margin-bottom: 6px;
  font-size: 22px;
}

.workbench-blocked-mask__content strong {
  margin-bottom: 8px;
  font-size: 15px;
}

.workbench-blocked-mask__content span {
  font-size: 13px;
  line-height: 1.6;
}

.slitting-tabs {
  display: flex;
  box-sizing: border-box;
  height: 100%;
  max-height: 100%;
  min-width: 0;
  min-height: 0;
  flex-direction: column;
  overflow: hidden;
  background: linear-gradient(180deg, #eef3f8 0%, #dfe7f0 100%);
}

.slitting-tabs :deep(.ant-tabs-nav) {
  flex: 0 0 38px;
  height: 38px;
  margin: 0;
  border-bottom: 1px solid #c6d1df;
  background: #eef4fa;
  padding: 0 12px;
}

.slitting-tabs :deep(.ant-tabs-tab) {
  color: #334155;
  font-weight: 800;
}

.slitting-tabs :deep(.ant-tabs-tab-active .ant-tabs-tab-btn) {
  color: #0f6fce;
}

.slitting-tabs :deep(.ant-tabs-ink-bar) {
  height: 3px;
  background: #0f6fce;
}

.tab-maximize-button {
  display: inline-flex;
  align-items: center;
  border-color: #8fb3d6;
  background: #e9f2fa;
  color: #0f5f99;
  gap: 4px;
  font-weight: 700;
}

.slitting-tabs :deep(.ant-tabs-content-holder),
.slitting-tabs :deep(.ant-tabs-content),
.slitting-tabs :deep(.ant-tabs-tabpane) {
  min-width: 0;
  min-height: 0;
  width: 100%;
  height: 100%;
  background: #f3f7fb;
  overflow: hidden;
}

.slitting-tabs :deep(.ant-tabs-content-holder) {
  flex: 1;
}

.slitting-tab-section {
  box-sizing: border-box;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  border: 0;
  background: linear-gradient(180deg, #f3f7fb 0%, #e6edf5 100%);
}

.empty-hint {
  display: grid;
  flex: 1;
  place-items: center;
  color: #64748b;
  font-size: 16px;
  text-align: center;
}

.empty-hint svg {
  margin-bottom: 8px;
  color: #94a3b8;
  font-size: 42px;
}

.cloth-list {
  display: grid;
  min-height: 0;
  flex: 1;
  gap: 10px;
  overflow: hidden;
  padding: 10px;
  grid-auto-rows: minmax(160px, 1fr);
  grid-template-columns: 1fr;
  align-items: stretch;
  justify-content: stretch;
  background:
    linear-gradient(90deg, rgba(15, 23, 42, 0.03) 1px, transparent 1px) 0 0 / 22px 22px,
    linear-gradient(180deg, #f3f7fb 0%, #e6edf5 100%);
}

.cloth-list.is-print-select-mode {
  grid-auto-rows: minmax(180px, auto);
  align-content: start;
  overflow: auto;
}

.cloth-list.is-print-select-mode .cloth-source {
  height: auto;
  min-height: 180px;
}

.cloth-source {
  display: flex;
  width: 100%;
  height: 100%;
  min-height: 0;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid #aebed0;
  background: #dce8f3;
}

.cloth-source-header {
  display: flex;
  flex: 0 0 38px;
  align-items: center;
  justify-content: space-between;
  min-height: 38px;
  border-bottom: 1px solid #b8c6d8;
  padding: 0 10px;
  background: #dce8f3;
}

.cloth-source-header > div {
  gap: 14px;
}

.cloth-source-header strong {
  color: #0d4f8b;
  font-size: 15px;
}

.cloth-source-header span {
  color: #34495e;
  font-size: 12px;
}

.source-actions {
  gap: 6px;
}

.cloth-source-body {
  flex: 1;
  min-height: 0;
  overflow: auto;
  background:
    radial-gradient(circle at 18% 14%, rgb(255 255 255 / 18%), transparent 18%),
    repeating-linear-gradient(0deg, rgb(255 255 255 / 8%) 0 1px, transparent 1px 10px),
    repeating-linear-gradient(90deg, rgb(15 76 117 / 10%) 0 1px, transparent 1px 10px),
    #8eb7d3;
  scrollbar-color: #6f91aa #c8d9e7;
  scrollbar-gutter: stable;
}

.cloth-whole,
.slice-grid {
  min-height: 100%;
  flex: 1;
}

.cloth-whole {
  display: grid;
  place-content: center;
  border: 8px solid #7ea9c9;
  background:
    repeating-linear-gradient(0deg, rgb(41 93 130 / 7%) 0 1px, transparent 1px 12px),
    repeating-linear-gradient(90deg, rgb(41 93 130 / 7%) 0 1px, transparent 1px 12px),
    #a8c8df;
  color: #17324c;
  cursor: pointer;
  text-align: center;
}

.cloth-whole span {
  font-size: 22px;
  font-weight: 900;
}

.cloth-whole small {
  margin-top: 6px;
  color: #24455f;
}

.cloth-whole.is-print-select-mode {
  cursor: default;
  opacity: 0.72;
}

.slice-grid {
  display: grid;
  align-content: start;
  grid-auto-rows: 110px;
  gap: 6px;
  justify-content: start;
  padding: 10px;
  background:
    radial-gradient(circle at 18% 14%, rgb(255 255 255 / 18%), transparent 18%),
    repeating-linear-gradient(0deg, rgb(255 255 255 / 8%) 0 1px, transparent 1px 10px),
    repeating-linear-gradient(90deg, rgb(15 76 117 / 10%) 0 1px, transparent 1px 10px),
    linear-gradient(135deg, #a6c8dc 0%, #79a8c4 100%);
}

.table-panel :deep(.ant-table-wrapper) {
  flex: 1;
  min-height: 0;
  background: #f3f7fb;
  border-radius: 0;
}

.table-panel :deep(.ant-spin-nested-loading),
.table-panel :deep(.ant-spin-container),
.table-panel :deep(.ant-table),
.table-panel :deep(.ant-table-container) {
  height: 100%;
  min-height: 0;
  background: #f3f7fb;
  border-radius: 0;
}

.table-panel :deep(.ant-table-content),
.table-panel :deep(.ant-table-body) {
  background: #f8fbff;
  border-radius: 0;
  scrollbar-color: #8ea4bb #d7e1eb;
}

.table-panel :deep(.ant-table-cell-fix-left),
.table-panel :deep(.ant-table-cell-fix-right) {
  background: #f8fbff;
}

.table-panel :deep(.ant-table-cell-fix-left-first::after),
.table-panel :deep(.ant-table-cell-fix-left-last::after),
.table-panel :deep(.ant-table-cell-fix-right-first::after),
.table-panel :deep(.ant-table-cell-fix-right-last::after) {
  box-shadow: none !important;
}

.table-panel :deep(.ant-table-sticky-scroll) {
  background: #d7e1eb;
}

.slice-table-wrap {
  display: flex;
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.slice-table-wrap :deep(.ant-table-wrapper) {
  width: 100%;
}

.slice-card {
  position: relative;
  width: 100%;
  height: 100%;
  min-width: 0;
}

.slice-card > .slice-cell {
  width: 100%;
}

.slice-card.has-delete-action .slice-cell__head {
  box-sizing: border-box;
  padding-right: 24px;
}

.slice-delete-action {
  position: absolute;
  z-index: 5;
  top: 7px;
  right: 7px;
  display: inline-flex;
  width: 22px;
  height: 22px;
  align-items: center;
  justify-content: center;
  padding: 0;
  color: #b91c1c;
  cursor: pointer;
  background: rgb(254 242 242 / 94%);
  border: 1px solid #ef4444;
  border-radius: 50%;
  box-shadow: 0 1px 4px rgb(127 29 29 / 24%);
}

.slice-delete-action:hover {
  color: #fff;
  background: #dc2626;
}

.slice-cell {
  position: relative;
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  box-sizing: border-box;
  padding: 9px 8px;
  overflow: hidden;
  border: 1px solid rgb(43 90 124 / 74%);
  border-radius: 2px;
  background:
    linear-gradient(135deg, rgb(255 255 255 / 46%) 0%, rgb(255 255 255 / 10%) 36%, transparent 37%),
    repeating-linear-gradient(45deg, rgb(255 255 255 / 16%) 0 1px, transparent 1px 7px),
    repeating-linear-gradient(-45deg, rgb(19 72 105 / 10%) 0 1px, transparent 1px 7px),
    #d9ebf4;
  box-shadow:
    inset 0 0 0 1px rgb(255 255 255 / 55%),
    inset 0 -12px 20px rgb(47 93 118 / 12%),
    0 1px 0 rgb(255 255 255 / 45%);
  color: #19324b;
  cursor: pointer;
  gap: 4px;
  text-align: left;
}

.slice-cell::before {
  position: absolute;
  inset: 7px;
  border: 1px solid rgb(255 255 255 / 36%);
  border-radius: 1px;
  content: '';
  pointer-events: none;
}

.slice-cell::after {
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

.slice-cell strong {
  position: relative;
  z-index: 1;
  color: inherit;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0;
}

.slice-cell span,
.slice-cell small {
  position: relative;
  z-index: 1;
  color: inherit;
  font-size: 11px;
}

.slice-cell__head {
  position: relative;
  z-index: 1;
  display: grid;
  width: 100%;
  min-width: 0;
  align-items: start;
}

.slice-cell__batch-wrap {
  display: flex;
  min-width: 0;
  align-items: flex-start;
  gap: 4px;
}

.slice-cell__batch {
  min-width: 0;
  overflow-wrap: anywhere;
  word-break: break-all;
  line-height: 1.2;
}

.slice-cell .slice-edit-lock {
  display: inline-flex;
  width: 20px;
  height: 20px;
  flex: 0 0 20px;
  align-items: center;
  justify-content: center;
  margin-left: auto;
  color: #c2410c;
  font-size: 13px;
  background: #fff7ed;
  border: 1px solid #fb923c;
  border-radius: 50%;
  box-shadow: 0 1px 3px rgb(124 45 18 / 20%);
}

.slice-cell__status {
  display: block;
  width: fit-content;
  max-width: 100%;
  margin-top: auto;
  justify-self: center;
  align-self: center;
  padding: 0 5px;
  overflow-wrap: anywhere;
  font-size: 10px;
  font-weight: 900;
  line-height: 15px;
  text-align: center;
  background: rgb(255 255 255 / 38%);
  border: 0;
  border-left: 3px solid currentColor;
  border-radius: 1px;
}

.slice-cell__changeover {
  display: inline-flex;
  max-width: 100%;
  align-self: center;
  align-items: center;
  gap: 3px;
  padding: 0 5px;
  overflow: hidden;
  color: #9a3412;
  font-size: 10px;
  font-weight: 900;
  line-height: 16px;
  white-space: nowrap;
  background: rgb(255 247 237 / 88%);
  border: 1px solid rgb(251 146 60 / 72%);
  border-radius: 2px;
}

.slice-cell__changeover b,
.slice-cell__changeover em {
  min-width: 0;
  overflow: hidden;
  font-style: normal;
  text-overflow: ellipsis;
}

.slice-cell__changeover b {
  flex: 0 0 auto;
}

.slice-cell small {
  max-width: 100%;
  align-self: center;
  margin-bottom: auto;
  overflow: hidden;
  text-align: center;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.slice-cell.is-pending {
  border-color: rgb(194 110 28 / 72%);
  background:
    linear-gradient(135deg, rgb(255 255 255 / 42%) 0%, rgb(255 255 255 / 12%) 34%, transparent 35%),
    repeating-linear-gradient(45deg, rgb(255 255 255 / 16%) 0 1px, transparent 1px 7px),
    repeating-linear-gradient(-45deg, rgb(146 64 14 / 10%) 0 1px, transparent 1px 7px),
    #ffd6a3;
  color: #7c2d12;
  box-shadow:
    inset 0 0 0 2px rgb(249 115 22 / 24%),
    inset 0 -12px 20px rgb(154 75 16 / 12%);
}

.slice-cell.is-confirmed {
  border-color: rgb(22 101 52 / 72%);
  background:
    linear-gradient(135deg, rgb(255 255 255 / 46%) 0%, rgb(255 255 255 / 10%) 36%, transparent 37%),
    repeating-linear-gradient(45deg, rgb(255 255 255 / 18%) 0 1px, transparent 1px 7px),
    repeating-linear-gradient(-45deg, rgb(21 128 61 / 10%) 0 1px, transparent 1px 7px),
    #a9efbf;
  color: #14532d;
  box-shadow:
    inset 0 0 0 2px rgb(22 163 74 / 28%),
    0 0 12px rgb(34 197 94 / 24%);
}

.slice-cell.is-ng {
  border-color: rgb(185 28 28 / 72%);
  background:
    linear-gradient(135deg, rgb(255 255 255 / 40%) 0%, rgb(255 255 255 / 10%) 34%, transparent 35%),
    repeating-linear-gradient(45deg, rgb(255 255 255 / 15%) 0 1px, transparent 1px 7px),
    repeating-linear-gradient(-45deg, rgb(153 27 27 / 10%) 0 1px, transparent 1px 7px),
    #f7b4b4;
  color: #7f1d1d;
  box-shadow:
    inset 0 0 0 2px rgb(220 38 38 / 28%),
    0 0 12px rgb(239 68 68 / 22%);
}

.slice-cell.is-transfer-selectable {
  padding-top: 8px;
}

.slice-cell.is-transfer-selected {
  border-color: #1d4ed8;
  box-shadow:
    inset 0 0 0 3px rgb(37 99 235 / 42%),
    0 0 0 2px rgb(255 255 255 / 88%),
    0 0 16px rgb(37 99 235 / 28%);
}

.slice-select-mark {
  position: static;
  z-index: 2;
  display: inline-flex;
  flex: 0 0 18px;
  width: 18px;
  height: 18px;
  align-items: center;
  justify-content: center;
  color: #1d4ed8;
  font-size: 15px;
  background: rgb(255 255 255 / 86%);
  border: 1px solid rgb(37 99 235 / 45%);
  border-radius: 2px;
}

.table-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.table-panel :deep(.ant-table-wrapper) {
  display: flex;
  flex: 1;
  min-height: 0;
  flex-direction: column;
}

.table-panel :deep(.ant-spin-nested-loading),
.table-panel :deep(.ant-spin-container) {
  display: flex;
  flex: 1;
  min-height: 0;
  flex-direction: column;
}

.table-panel :deep(.ant-table) {
  flex: 1;
  min-height: 0;
}

.table-panel :deep(.ant-table-container) {
  height: 100%;
  min-height: 0;
}

.table-panel :deep(.ant-pagination) {
  flex: 0 0 42px;
  margin: 8px 0 0 !important;
}

.slice-table-wrap :deep(.ant-table-wrapper) {
  display: flex;
  width: 100%;
  height: 100%;
  min-height: 0;
  flex-direction: column;
}

.slice-table-wrap :deep(.ant-spin-nested-loading),
.slice-table-wrap :deep(.ant-spin-container) {
  display: flex;
  flex: 1 1 auto;
  min-height: 0;
  flex-direction: column;
}

.slice-table-wrap :deep(.ant-table) {
  flex: 1 1 auto;
  min-height: 0;
}

.slice-table-wrap :deep(.ant-table-container) {
  min-height: 0;
}

.slice-table-wrap :deep(.ant-table-body) {
  height: var(--slice-table-body-height) !important;
  min-height: var(--slice-table-body-height);
  max-height: var(--slice-table-body-height) !important;
  overflow: auto !important;
}

.slice-table-wrap :deep(.ant-pagination) {
  flex: 0 0 34px;
  margin: 6px 0 0 !important;
}

.modal-summary {
  display: flex;
  gap: 16px;
  justify-content: space-between;
  border: 1px solid #cbd5e1;
  background: #f1f5f9;
  padding: 10px 12px;
  margin-bottom: 12px;
}

.generate-form-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.generate-form-grid__wide {
  grid-column: 1 / -1;
}

.size-rule-hint {
  margin-top: 6px;
  color: #64748b;
  font-size: 12px;
}

.generate-error {
  padding: 8px 10px;
  margin-top: 10px;
  color: #b91c1c;
  font-weight: 800;
  background: #fef2f2;
  border: 1px solid #fecaca;
}

.full-input {
  width: 100%;
}

.confirm-head {
  display: grid;
  margin-bottom: 12px;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
}

.confirm-head > div {
  min-width: 0;
  border: 1px solid #cbd5e1;
  background: #f8fafc;
  padding: 8px 10px;
}

.confirm-head span {
  display: block;
  color: #64748b;
  font-size: 12px;
}

.confirm-head strong {
  display: block;
  color: #0f172a;
  line-height: 1.35;
  overflow-wrap: anywhere;
}

.visual-check-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
  margin-bottom: 12px;
}

.visual-light-item {
  min-height: 42px;
}

.visual-light-button {
  display: flex;
  width: 100%;
  height: 42px;
  align-items: center;
  gap: 10px;
  padding: 0 12px;
  color: #334155;
  font-weight: 800;
  cursor: pointer;
  background: linear-gradient(180deg, #f8fafc 0%, #e2e8f0 100%);
  border: 1px solid #b8c5d6;
}

.visual-light-button:disabled {
  cursor: default;
  opacity: 0.72;
}

.visual-light-item.is-active .visual-light-button {
  color: #7f1d1d;
  background: linear-gradient(180deg, #fee2e2 0%, #fecaca 100%);
  border-color: #ef4444;
  box-shadow: 0 0 14px rgba(239, 68, 68, 0.2);
}

.visual-light-dot {
  display: inline-block;
  width: 14px;
  height: 14px;
  border-radius: 50%;
  background: #cbd5e1;
  border: 1px solid #94a3b8;
  box-shadow: inset 0 1px 2px rgba(15, 23, 42, 0.22);
}

.visual-light-dot.is-active {
  background: #ef4444;
  border-color: #b91c1c;
  box-shadow:
    0 0 0 3px rgba(239, 68, 68, 0.12),
    0 0 12px rgba(239, 68, 68, 0.82);
}

.visual-check-lock {
  margin-top: 6px;
  color: #b91c1c;
  font-size: 12px;
  font-weight: 700;
}

.slice-readonly-hint {
  padding: 8px 10px;
  margin-bottom: 10px;
  color: #92400e;
  font-weight: 700;
  background: #fffbeb;
  border: 1px solid #fbbf24;
}

.slice-readonly-hint--blocked {
  color: #991b1b;
  background: #fef2f2;
  border-color: #fca5a5;
}

.abnormal-category-correction-action {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}

.abnormal-category-correction-options {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.slitting-console-shell.has-sample-lock {
  grid-template-rows: repeat(3, max-content) minmax(0, 1fr);
}
.slitting-console-shell.has-sample-lock.is-visual-maximized {
  grid-template-rows: max-content max-content minmax(0, 1fr);
}
.sample-lock-notice {
  max-height: 84px;
  overflow: auto;
}
</style>
