<script lang="ts" setup>
import type { MesHcToolingConsumableLedgerApi } from '#/api/mes/hc/tooling-consumable-ledger';
import CutRoundBladeStockFields from '#/views/mes/hc/base/tooling-consumable-ledger/components/CutRoundBladeStockFields.vue';
import type { MesHcProcessFormApi } from '#/api/mes/hc/processform';

import { computed, nextTick, onActivated, onBeforeUnmount, onDeactivated, onMounted, reactive, ref, watch } from 'vue';

import { useAccess } from '@vben/access';
import { Page, useVbenModal } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';
import { useUserStore } from '@vben/stores';
import { downloadFileFromBlobPart } from '@vben/utils';
import { useQRCode } from '@vueuse/integrations/useQRCode';

import {
  Badge,
  Button,
  Checkbox,
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
  Tabs,
  TabPane,
  Tag,
  message,
} from 'ant-design-vue';
import dayjs from 'dayjs';
import { useRoute } from 'vue-router';

import { uploadFile } from '#/api/infra/file';
import EdgeConsumableRegisterTab from '#/views/mes/hc/base/tooling-consumable-ledger/components/EdgeConsumableRegisterTab.vue';
import {
  completeCutRoundConsoleSegment,
  completeCutRoundConsoleWorkOrder,
  confirmCutRoundConsolePassWork,
  confirmCutRoundConsoleReport,
  correctCutRoundConsoleReportAbnormalCategory,
  createCutRoundInspectionTask,
  deleteCutRoundChangeover,
  findCutRoundReportByBatchNo,
  getCutRoundChangeoverLatest,
  getCutRoundChangeoverList,
  getCutRoundConsoleCheckTemplate,
  getCutRoundConsoleCurrentGlueBoardUsage,
  getCutRoundConsoleGlueBoardStockByBatch,
  getCutRoundConsoleIntermediate,
  getCutRoundConsolePassWorkList,
  getCutRoundConsoleReportList,
  getCutRoundConsoleSourceList,
  getCutRoundConsoleTaskList,
  getCutRoundInspectionTasks,
  markCutRoundConsoleReportPrinted,
  reportCutRoundConsoleGlueBoardLoss,
  replaceCutRoundConsumable,
  saveAndConfirmCutRoundConsoleReport,
  saveCutRoundConsoleGlueBoardUsage,
  saveCutRoundConsoleIntermediate,
  saveCutRoundConsolePassWork,
  saveCutRoundConsoleReport,
  saveCutRoundChangeover,
  scanCutRoundConsoleSource,
  startCutRoundConsoleWorkOrder,
  submitCutRoundConsoleAqcTask,
  type MesHcCutRoundConsoleApi,
} from '#/api/mes/hc/execution/cut-round-console';
import { getEquipmentPage, type MesHcEquipmentApi } from '#/api/mes/hc/equipment';
import { getPlanOrderDetail } from '#/api/mes/hc/planorder';
import {
  exportProcessFormRecordLayout,
  importProcessFormRecordLayout,
} from '#/api/mes/hc/processform';
import FqcDetailModalVue from '#/views/mes/quality/fqc/modules/detail-modal.vue';
import AuthModal from '#/views/mes/work-order-booking/modules/components/AuthModal.vue';
import {
  ProductionInstructionMessageTab,
  type ProductionInstructionContext,
} from '#/views/mes/hc/shared/production-instruction';
import { getProductionInstructionUnreadCount } from '#/api/mes/hc/production-instruction';
import { useExecutionFullscreenClock } from '../shared/useExecutionFullscreenClock';
import { getReportRequestErrorMessage, resolveSavedReportId } from '../shared/reportSubmitGuard';
import {
  applyPreProcessSelfCheckAttribution,
  isPreProcessSelfCheckAbnormal,
} from '../shared/preProcessSelfCheckAttribution';
import {
  buildTransferTicketQrValue,
  buildWorkOrderTicketHtml,
  resolveTransferTicketQrBusinessNo,
} from '../shared/workOrderTicketPrint';
import { applyPrintFieldTemplate } from '../shared/printFieldTemplate';
import {
  buildSegmentChainSampleLockCandidates,
  ensureProcessingSampleAbnormalUnlocked as ensureSampleAbnormalUnlocked,
} from '../shared/sampleAbnormalLockGuard';

defineOptions({ name: 'MesExecutionCutRoundConsole' });

const CUT_ROUND_PRE_PROCESS_ATTRIBUTION = {
  processCode: 'ADHESIVE2',
  processName: '粘胶2',
} as const;

const ADHESIVE_DEFAULT_GLUE_BOARD_MATERIAL_CODE = '01.02.00002';
const CUT_ROUND_PRINT_AGENT_URL = 'http://127.0.0.1:17820';
const TASK_LIST_DEFAULT_PAGE_SIZE = 10;
const GLUE_BOARD_MODEL_OPTIONS = [
  { label: 'W220', value: 'W220' },
  { label: 'W250', value: 'W250' },
  { label: 'SDK', value: 'SDK' },
];
type CutRoundReportType = 'CHANGEOVER' | 'END' | 'FRONT' | 'MIDDLE' | 'PROCESS_CHECK' | 'PRODUCT';

type DailyRecordMode = 'confirm' | 'edit' | 'view';
type DailyRecordSubmitAction = {
  key: string;
  mode: Exclude<DailyRecordMode, 'view'>;
};

interface CurrentPlan {
  actualSizeRule: string;
  availableSourceLength: number;
  batchNo: string;
  endTime: string;
  equipmentCode: string;
  equipmentId?: number;
  equipmentName: string;
  materialCode: string;
  modelCode: string;
  planId?: number;
  planNo: string;
  planOperationId?: number;
  planSizeSpec: string;
  requirements: string;
  sourceBatchNo: string;
  sourceProductionBatchNo: string;
  startTime: string;
  status: string;
  workCenterId?: number;
  workCenterName: string;
}

interface BoardEquipment {
  applicablePadType?: string;
  applicablePadTypeName?: string;
  code: string;
  id?: number;
  name: string;
  workCenterId?: number;
  workCenterName: string;
  workStatus?: string;
}

interface DailyRecordRow {
  canConfirm?: boolean;
  canFill?: boolean;
  canView?: boolean;
  confirmer: string;
  confirmerTime: string;
  details: WorkPrepareDetail[];
  formCode: string;
  key: string;
  name: string;
  recordId?: number;
  recorder: string;
  recorderTime: string;
  result: string;
  status: 'COMPLETED' | 'FILLED' | 'PENDING';
  timing: string;
}

interface WorkPrepareDetail {
  category?: string;
  item: string;
  node?: string;
  remark: string;
  result: 'NG' | 'OK';
  seq: number;
  standard: string;
  value: string;
}

interface SourceSegment {
  actualSizeRule?: string;
  actualSizeSuffix?: string;
  availableLength: number;
  batchNo: string;
  coaFlag?: boolean;
  confirmStatus?: string;
  defectCode?: string;
  extraJson?: string;
  grindingSecondDetailId?: number;
  label: string;
  modelCode?: string;
  outputLength: number;
  qtime?: MesHcCutRoundConsoleApi.QtimeInfo;
  reportRanges: SourceReportRange[];
  segmentMark: string;
  selfCheck?: string;
  sourceNgProcessName?: string;
  sourceNgReason?: string;
  sourceNgText?: string;
  usedLength: number;
}

interface SourceReportRange {
  end: number;
  label: string;
  reportId?: number;
  start: number;
  status?: string;
}

interface SourceGroup {
  baseBatchNo: string;
  materialCode?: string;
  modelCode?: string;
  planNo?: string;
  qtime?: MesHcCutRoundConsoleApi.QtimeInfo;
  segments: SourceSegment[];
  totalAvailableLength: number;
  totalOutputLength: number;
  totalUsedLength: number;
}

interface CutRoundOneClickConfirmTarget {
  group: SourceGroup;
  record?: MesHcCutRoundConsoleApi.ReportItem;
  segment: SourceSegment;
}

interface CutRoundTransferPrintTarget {
  group: SourceGroup;
  record?: MesHcCutRoundConsoleApi.ReportItem;
  segment: SourceSegment;
}

interface SubmitCutRoundReportOptions {
  confirmAfterSave?: boolean;
  closeAfterSave?: boolean;
  silentSuccess?: boolean;
}

interface CutRoundCheckItem {
  abnormalRemark: string;
  actualValue: string;
  checkResult: 'NG' | 'OK' | string;
  itemCategory: string;
  itemName: string;
  sortNo: number;
  standardValue: string;
}

interface VisualItem {
  inheritedDefectCode?: string;
  inheritedFromAdhesive2?: boolean;
  itemName: string;
  remark: string;
  result: 'NG' | 'OK';
}

interface SourceNgVisualItem {
  defectCode?: string;
  itemName: string;
  remark: string;
}

const route = useRoute();
const userStore = useUserStore();
const { hasAccessByCodes } = useAccess();
const PLAN_SCAN_MIN_LENGTH = 11;
const ABNORMAL_CATEGORY_CORRECT_PERMISSION = 'mes:sfc:cut-round-report:correct-abnormal-category';
const INSPECTION_TASK_REFRESH_INTERVAL_MS = 5 * 60_000;
const currentDateTime = ref(dayjs().format('YYYY-MM-DD HH:mm:ss'));
const currentDateText = computed(() => dayjs(currentDateTime.value).format('YYYY-MM-DD'));
const currentTimeText = computed(() => dayjs(currentDateTime.value).format('HH:mm:ss'));
const { showExecutionClock } = useExecutionFullscreenClock();
const boardLoading = ref(false);
const visualMaximized = ref(false);
const transferPrintSelectionMode = ref(false);
const selectedTransferReportIds = ref<string[]>([]);
let timer: ReturnType<typeof setInterval> | null = null;
let inspectionTaskRefreshTimer: ReturnType<typeof setInterval> | null = null;
let planScanTimer: ReturnType<typeof setTimeout> | null = null;
let syncingReportPosition = false;
let inspectionTaskRefreshInFlight = false;

const currentUserName = computed(() => userStore.userInfo?.nickname || userStore.userInfo?.username || '');

const currentPlan = reactive<CurrentPlan>({
  actualSizeRule: '',
  availableSourceLength: 0,
  batchNo: '',
  endTime: '',
  equipmentCode: '',
  equipmentName: '',
  materialCode: '',
  modelCode: '',
  planNo: '',
  requirements: '',
  sourceBatchNo: '',
  sourceProductionBatchNo: '',
  startTime: '',
  status: '',
  workCenterId: undefined,
  workCenterName: '',
});

const productionInstructionUnreadCount = ref(0);
const productionInstructionContext = computed<ProductionInstructionContext>(() => ({
  operationCode: 'WC-CUT',
  operationName: '裁切',
  processCode: 'WC-CUT',
  processName: '裁切',
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

const boardEquipment = reactive<BoardEquipment>({
  applicablePadType: '',
  applicablePadTypeName: '',
  code: '',
  id: undefined,
  name: '',
  workCenterId: undefined,
  workCenterName: '',
  workStatus: '',
});
const boardEquipmentManualSelected = ref(false);

const scanPlanNo = ref('');
const planScanInputRef = ref<any>();
const recordConfirmScanInputRef = ref<any>();
const lastAutoScannedPlanNo = ref('');
let pendingScannerPlanNo = '';
let pendingScannerSliceBatchNo = '';
let globalScannerBuffer = '';
let globalScannerLastAt = 0;
let globalScannerTimer: ReturnType<typeof setTimeout> | null = null;
let globalScannerListenerAttached = false;
let planScanInFlight = false;
let planScanWarningVisible = false;
let suppressNextPlanScanSchedule = false;
let sourceGroupsLoadSeq = 0;
const PLAN_SCAN_SEPARATOR_REGEXP = /[，,；;]/;
const GLOBAL_SCANNER_MAX_GAP_MS = 80;
const GLOBAL_SCANNER_IDLE_FLUSH_MS = 160;
const hasPlanScanDelimiter = (value?: string) => PLAN_SCAN_SEPARATOR_REGEXP.test(value || '');
const parsePlanScanCode = (value?: string) => {
  const parts = String(value || '')
    .trim()
    .split(PLAN_SCAN_SEPARATOR_REGEXP)
    .map((item) => item.trim())
    .filter(Boolean);
  return {
    planNo: parts[0] || '',
    sliceBatchNo: parts[1] || '',
  };
};
const normalizePlanScanNo = (value?: string) => parsePlanScanCode(value).planNo;
const normalizePlanScanSliceBatchNo = (value?: string) => parsePlanScanCode(value).sliceBatchNo;
const normalizeConfirmScanCode = (value?: string) => {
  const text = String(value || '').trim();
  const separatorIndex = text.search(PLAN_SCAN_SEPARATOR_REGEXP);
  return separatorIndex >= 0 ? text.slice(separatorIndex + 1).trim() || text : text;
};
const syncPlanScanNo = (value?: string) => {
  const scannerInput = hasPlanScanDelimiter(value);
  const { planNo, sliceBatchNo } = parsePlanScanCode(value);
  if (scannerInput) {
    pendingScannerPlanNo = planNo;
    pendingScannerSliceBatchNo = sliceBatchNo;
  }
  if (String(value || '').trim() !== planNo && scanPlanNo.value !== planNo) {
    scanPlanNo.value = planNo;
  }
  return planNo;
};
const clearPlanScanTimer = () => {
  if (planScanTimer) {
    clearTimeout(planScanTimer);
    planScanTimer = null;
  }
};
const clearPlanScannerState = () => {
  clearPlanScanTimer();
  suppressNextPlanScanSchedule = false;
  scanPlanNo.value = '';
  pendingScannerPlanNo = '';
  pendingScannerSliceBatchNo = '';
  lastAutoScannedPlanNo.value = '';
};
const setManualTaskPlanScanNo = (planNo?: string) => {
  const normalized = String(planNo || '').trim();
  clearPlanScanTimer();
  pendingScannerPlanNo = '';
  pendingScannerSliceBatchNo = '';
  lastAutoScannedPlanNo.value = normalized;
  if (scanPlanNo.value === normalized) return;
  suppressNextPlanScanSchedule = true;
  scanPlanNo.value = normalized;
  nextTick(() => {
    suppressNextPlanScanSchedule = false;
  });
};
const focusInputRef = (inputRef: { value?: any }) => {
  nextTick(() => {
    window.setTimeout(() => {
      inputRef.value?.focus?.();
      inputRef.value?.input?.focus?.();
    }, 0);
  });
};
const focusPlanScanInput = () => focusInputRef(planScanInputRef);
const focusRecordConfirmScanInput = () => focusInputRef(recordConfirmScanInputRef);
const handlePlanScanInput = (value?: string) => {
  const rawValue = String(value || '');
  if (hasPlanScanDelimiter(rawValue)) {
    const { planNo, sliceBatchNo } = parsePlanScanCode(rawValue);
    pendingScannerPlanNo = planNo;
    pendingScannerSliceBatchNo = sliceBatchNo;
    scanPlanNo.value = planNo;
    return;
  }
  scanPlanNo.value = rawValue;
};
const showSinglePlanScanWarning = (options: Record<string, any>) => {
  if (planScanWarningVisible) return;
  planScanWarningVisible = true;
  const afterClose = options.afterClose;
  AModal.warning({
    ...options,
    afterClose: () => {
      planScanWarningVisible = false;
      afterClose?.();
    },
  });
};
const isEventTargetInInputRef = (target: EventTarget | null, inputRef: { value?: any }) => {
  const element = inputRef.value?.input || inputRef.value?.$el || inputRef.value;
  return !!(element && target instanceof Node && element.contains?.(target));
};
const isEventFromScannerInput = (event: KeyboardEvent) =>
  isEventTargetInInputRef(event.target, planScanInputRef) ||
  isEventTargetInInputRef(event.target, recordConfirmScanInputRef);
const isEditableEventTarget = (target: EventTarget | null) => {
  if (!(target instanceof HTMLElement)) return false;
  const tagName = target.tagName.toUpperCase();
  return target.isContentEditable || tagName === 'INPUT' || tagName === 'TEXTAREA' || tagName === 'SELECT';
};
const clearGlobalScannerBuffer = () => {
  globalScannerBuffer = '';
  globalScannerLastAt = 0;
  if (globalScannerTimer) {
    clearTimeout(globalScannerTimer);
    globalScannerTimer = null;
  }
};
const dailyRecordRows = ref<DailyRecordRow[]>([]);
const dailyRecordVisible = ref(false);
const dailyRecordListVisible = ref(false);
const equipmentOptions = ref<any[]>([]);
const equipmentSelectVisible = ref(false);
const equipmentSelectLoading = ref(false);
const equipmentSelectRows = ref<MesHcEquipmentApi.Equipment[]>([]);
const equipmentSelectKeyword = ref('');
const openDailyRecordAfterEquipmentSelected = ref(false);
const reportRecordListVisible = ref(false);
const dailyRecordMode = ref<DailyRecordMode>('edit');
const dailyRecordKey = ref('');
const dailyRecordAuthVisible = ref(false);
const dailyRecordAuthAction = ref('工作准备记录认证');
const pendingDailyRecordAction = ref<DailyRecordSubmitAction>();
const activeBoardTab = ref('SOURCE');
const activeReportTab = ref('scan');
const reportVisible = ref(false);
const showExtendedBoardTabs = ref(false);
const reportDialogMode = ref<'confirm' | 'view'>('view');
const reportStep = ref<'PROCESS' | 'REPORT' | 'SCAN'>('SCAN');
const reportRecords = ref<MesHcCutRoundConsoleApi.ReportItem[]>([]);
const reportScanConfirmDate = ref('');
const selectedReportRecordKeys = ref<(number | string)[]>([]);
const oneClickScanConfirming = ref(false);
const sourceGroups = ref<SourceGroup[]>([]);
const workbenchRefreshing = ref(false);
const visualFilterForm = reactive({
  batchNo: '',
  reportType: 'ALL',
  status: 'ALL',
});
const checkTemplate = ref<CutRoundCheckItem[]>([]);
const recordConfirmVisible = ref(false);
const activeRecord = ref<MesHcCutRoundConsoleApi.ReportItem | null>(null);
const abnormalCategoryCorrectionVisible = ref(false);
const abnormalCategoryCorrectionSaving = ref(false);
const abnormalCategoryCorrectionForm = reactive({
  category: '',
  reason: '',
});
const taskListVisible = ref(false);
const taskListLoading = ref(false);
const taskRows = ref<MesHcCutRoundConsoleApi.TaskItem[]>([]);
const taskListPage = ref(1);
const taskListPageSize = ref(TASK_LIST_DEFAULT_PAGE_SIZE);
const taskListFilters = reactive({
  batchNo: '',
  modelCode: '',
  planNo: '',
  status: 'UNFINISHED',
});
const intermediateLoading = ref(false);
const intermediateDetails = ref<MesHcCutRoundConsoleApi.IntermediateDetail[]>([]);
const changeoverInspections = ref<MesHcCutRoundConsoleApi.ChangeoverInspection[]>([]);
const latestChangeoverInspection = ref<MesHcCutRoundConsoleApi.ChangeoverInspection | null>(null);
const inspectionTasks = ref<MesHcCutRoundConsoleApi.CutRoundInspectionTask[]>([]);
const inspectionTaskDetailVisible = ref(false);
const selectedInspectionTask = ref<MesHcCutRoundConsoleApi.CutRoundInspectionTask | null>(null);
const [FqcDetailModal, fqcDetailModalApi] = useVbenModal({
  connectedComponent: FqcDetailModalVue,
  destroyOnClose: true,
});
const inspectionTaskVisible = ref(false);
const inspectionTaskSubmitting = ref(false);
const inspectionTaskRows = ref<MesHcCutRoundConsoleApi.ReportItem[]>([]);
const inspectionTaskForm = reactive({
  expectedFinishDate: '',
  priorityLevel: 'NORMAL',
  receiveLocation: '检验室',
  receiverName: '',
  remark: '',
  reportDate: dayjs().format('YYYY-MM-DD'),
  reportProcess: '裁切',
  reportTime: buildNowText(),
  reporterName: '',
});
const changeoverVisible = ref(false);
const changeoverAuthVisible = ref(false);
const changeoverAuthAction = ref('工艺参数点检认证');
const changeoverExcelLoading = ref(false);
const changeoverSubmitting = ref(false);
const changeoverConfirming = ref(false);
const changeoverExcelImportInputRef = ref<HTMLInputElement>();
const pendingChangeoverAuthAction = ref<'confirm' | 'save'>();
const changeoverForm = reactive<MesHcCutRoundConsoleApi.ChangeoverInspection>({
  checkItems: [],
  confirmerName: '',
  confirmerTime: '',
  currentPlanNo: '',
  inspectionStatus: 'DRAFT',
  motherSegmentBatchNo: '',
  planId: 0,
  planOperationId: 0,
  productionMaterialCode: '',
  productionModelCode: '',
  recordTime: '',
  recorderName: '',
  remark: '',
  submitTime: '',
});

const intermediateForm = reactive<MesHcCutRoundConsoleApi.IntermediateRecord>({
  adhesiveReportId: undefined,
  batchNo: '',
  confirmerName: '',
  endSliceNo: '',
  firstSampleSliceNo: '',
  firstSlotDepthAvg: undefined,
  firstSlotDepthMax: undefined,
  firstSlotDepthMin: undefined,
  firstSlotDepthXAvg: undefined,
  firstSlotDepthXMax: undefined,
  firstSlotDepthXMin: undefined,
  firstSlotDepthYAvg: undefined,
  firstSlotDepthYMax: undefined,
  firstSlotDepthYMin: undefined,
  frontSliceNo: '',
  inputQty: 0,
  materialCode: '',
  middleSliceNo: '',
  modelCode: '',
  outputQty: 0,
  planId: 0,
  planNo: '',
  planOperationId: 0,
  processLength: 0,
  productionDate: dayjs().format('YYYY-MM-DD'),
  recordDate: dayjs().format('YYYY-MM-DD'),
  recorderName: '',
  recordStatus: 'DRAFT',
  remark: '',
  widthEnd: undefined,
  widthMiddle: undefined,
  widthStart: undefined,
});

const glueBoard = reactive({
  alarm: '',
  batchNo: '',
  id: undefined as number | undefined,
  stockId: undefined as number | undefined,
  materialCode: '',
  qualityLockReason: '',
  qualityLockStartPosition: undefined as number | undefined,
  qualityStatus: '',
  receiveLength: 0,
  receiveStartPosition: 0,
  stockLength: 0,
  todayUsedLength: 0,
  availableStartPosition: 0,
  aqcSampleLength: 0,
  latestAqcTask: undefined as MesHcCutRoundConsoleApi.AqcTask | undefined,
  lossLength: 0,
});

const glueConsumeVisible = ref(false);
const glueConsumeForm = reactive({
  batchNo: '',
  materialScanCode: '',
  receiveLength: undefined as number | undefined,
  receiveStartPosition: 0,
  stockAvailableLength: 0,
  stockId: undefined as number | undefined,
});

const aqcVisible = ref(false);
const aqcForm = reactive({
  id: undefined as number | undefined,
  sampleLength: undefined as number | undefined,
  sampleStartPosition: 0,
});

const glueLossVisible = ref(false);
const glueLossForm = reactive({
  endPosition: undefined as number | undefined,
  lossLength: undefined as number | undefined,
  lossReason: '',
  startPosition: 0,
});

const cutRoundConsumables = ref<any[]>([]);
type CutRoundConsumableType = 'CUTTING_BLADE' | 'CUTTING_FELT';
const bladeLedger = ref<MesHcToolingConsumableLedgerApi.Ledger>();
const bladeReplaceQuantity = ref(1);
const bladeRequestKey = ref('');
const consumableSubmitting = ref(false);
const consumableReplaceVisible = ref(false);
const consumableReplaceType = ref<CutRoundConsumableType>('CUTTING_BLADE');
const consumableAuthVisible = ref(false);
const consumableAuthAction = ref('');
const pendingConsumableAction = ref<CutRoundConsumableType | ''>('');
const segmentCompleteAuthVisible = ref(false);
const segmentCompleteAuthActionName = '裁切本段完成';
const pendingSegmentCompleteGroup = ref<SourceGroup | null>(null);
const consumableReplaceForm = reactive({
  initialUseCount: 0,
  lastReplaceTime: '',
  limitCount: 0,
  replaceReason: '',
  replaceTime: '',
  useCount: 0,
});
const activeCutRoundConsumableName = computed(() => consumableReplaceType.value === 'CUTTING_BLADE' ? '刀片' : '毛毡');
const visualInspectionItems = ref<VisualItem[]>([]);

const recordConfirmForm = reactive({
  actualSizeRule: '',
  error: '',
  glueBoardModel: 'W220',
  message: '',
  reportType: 'PRODUCT' as CutRoundReportType,
  scannedBatchNo: '',
  sourceActualSizeRule: '',
});

const reportForm = reactive({
  actualSizeRule: '',
  defectCode: '',
  endTime: '',
  glueBoardBatchNo: '',
  glueBoardMaterialCode: '',
  glueBoardModel: 'W220',
  glueBoardStartPosition: 0,
  glueBoardUsageId: undefined as number | undefined,
  glueBoardUseLength: 0,
  aqcStatus: '',
  aqcTaskId: undefined as number | undefined,
  lossLength: 0,
  modelCode: '',
  napSampleLength: 0,
  outputLength: 0,
  parentBatchNo: '',
  processLength: 0,
  productionBatchNo: '',
  preProcessSelfCheckAbnormal: false,
  remark: '',
  reportDate: dayjs().format('YYYY-MM-DD'),
  reportType: 'PRODUCT' as CutRoundReportType,
  selfCheck: 'OK',
  sourceCode: '',
  startPosition: 0,
  endPosition: 0,
  sourceGrindingSecondDetailId: undefined as number | undefined,
  sourceNgDefectCode: '',
  sourceProductionBatchNo: '',
  sourceActualSizeRule: '',
  sourceNgReviewResult: '',
  sourceNgDefectSummary: '',
  sourceNgVisualItems: [] as SourceNgVisualItem[],
  sourceDownstreamStatus: '',
  sourceScanError: '',
  sourceScanMessage: '',
  startTime: '',
});

const defectOptions = [
  { label: '无', value: '' },
  { label: '厚度异常', value: 'THICKNESS_NG' },
  { label: '宽幅异常', value: 'WIDTH_NG' },
  { label: '粘胶异常', value: 'ADHESIVE_NG' },
];

const dailyRecordColumns = [
  { dataIndex: 'name', title: '表单名称', width: 260 },
  { dataIndex: 'timing', title: '执行时机', width: 130 },
  { dataIndex: 'status', title: '单据状态', width: 120 },
  { dataIndex: 'recorderInfo', title: '记录人/时间', width: 180 },
  { dataIndex: 'confirmerInfo', title: '确认人/确认时间', width: 180 },
  { dataIndex: 'action', fixed: 'right' as const, title: '操作', width: 180 },
];
const taskListBaseColumns = [
  { dataIndex: 'planNo', title: '计划号' },
  { dataIndex: 'sourceBatchNo', title: '分段批次号' },
  { dataIndex: 'modelCode', title: '产品型号' },
  { dataIndex: 'planSizeSpec', title: '计划尺寸' },
  { dataIndex: 'availableSourceLength', title: '可加工数' },
  { dataIndex: 'status', title: '状态' },
  { dataIndex: 'action', fixed: 'right' as const, title: '操作' },
];
const taskListColumnWidthRules: Record<string, { max: number; min: number; padding: number }> = {
  action: { max: 86, min: 72, padding: 24 },
  availableSourceLength: { max: 112, min: 92, padding: 28 },
  modelCode: { max: 220, min: 108, padding: 32 },
  planNo: { max: 260, min: 110, padding: 32 },
  planSizeSpec: { max: 200, min: 106, padding: 32 },
  sourceBatchNo: { max: 320, min: 142, padding: 32 },
  status: { max: 92, min: 78, padding: 24 },
};
const taskListColumnTextGetters: Record<string, (row: Record<string, any>) => unknown> = {
  action: (row) => getTaskActionText(row.status),
  availableSourceLength: (row) => formatNumber(row.availableSourceLength),
  planSizeSpec: (row) => resolvePlanSizeSpec(row),
  sourceBatchNo: (row) => resolveMotherBatchNo(row),
  status: (row) => getWorkOrderStatusMeta(row.status).text,
};

const equipmentSelectColumns = [
  { dataIndex: 'equipmentCode', title: '设备编码', width: 150 },
  { dataIndex: 'equipmentName', title: '设备名称', width: 180 },
  { dataIndex: 'applicablePadTypeName', title: '适用垫型', width: 100 },
  { dataIndex: 'workCenterName', title: '工作中心', width: 160 },
  { dataIndex: 'workStatus', title: '状态', width: 100 },
  { dataIndex: 'action', fixed: 'right' as const, title: '操作', width: 90 },
];

const taskListStatusOptions = [
  { label: '未完工', value: 'UNFINISHED' },
  { label: '全部', value: 'ALL' },
  { label: '待开工', value: 'PENDING' },
  { label: '生产中', value: 'RUNNING' },
  { label: '已完工', value: 'FINISHED' },
  { label: '已暂停', value: 'PAUSED' },
  { label: '已作废', value: 'CANCELLED' },
];

const cutRoundFilterStatusOptions = [
  { label: '全部', value: 'ALL' },
  { label: '待确认', value: 'PENDING' },
  { label: '本工序NG', value: 'CURRENT_NG' },
  { label: '粘胶2来源NG', value: 'SOURCE_NG' },
  { label: '已确认', value: 'COMPLETED' },
  { label: '检测中', value: 'INSPECTING' },
  { label: '已检验', value: 'INSPECTED' },
];

const cutRoundIntermediatePositions = [
  { label: '首件', value: 'FIRST' },
  { label: '前段', value: 'FRONT' },
  { label: '中段', value: 'MIDDLE' },
  { label: '后段', value: 'END' },
];

const cutRoundReportTypeOptions: Array<{ label: string; value: CutRoundReportType }> = [
  { label: '工艺参数点检', value: 'CHANGEOVER' },
  { label: '成品加工', value: 'PRODUCT' },
  { label: '中间品-前段', value: 'FRONT' },
  { label: '中间品-中段', value: 'MIDDLE' },
  { label: '中间品-后段', value: 'END' },
  { label: '过程加检', value: 'PROCESS_CHECK' },
];

const cutRoundReportTypeFilterOptions = [
  { label: '全部作业', value: 'ALL' },
  ...cutRoundReportTypeOptions,
];
const cutRoundVisualItemNames = ['黑点', '蓝点', '黄点', '红点', '针孔', '条纹', '褶皱', '波浪纹', '其他'];
const abnormalCategoryCorrectionOptions = cutRoundVisualItemNames.map((item) => ({ label: item, value: item }));

const cutRoundMeasuredPositions = cutRoundIntermediatePositions.filter((item) => item.value !== 'FIRST');

const cutRoundIntermediateCheckItems = ref<CutRoundCheckItem[]>([
  { abnormalRemark: '', actualValue: '', checkResult: 'OK', itemCategory: '中间品', itemName: '宽幅/mm', sortNo: 501, standardValue: '/' },
  ...Array.from({ length: 10 }, (_, index) => ({
    abnormalRemark: '',
    actualValue: '',
    checkResult: 'OK',
    itemCategory: '中间品',
    itemName: `${(index + 1) * 100}cm厚度/mm`,
    sortNo: 502 + index,
    standardValue: '1.520±0.050',
  })),
]);

const cutRoundProcessCheckItems = ref<CutRoundCheckItem[]>([
  { abnormalRemark: '', actualValue: buildNowText(), checkResult: 'OK', itemCategory: '过程加检', itemName: '检测时间', sortNo: 601, standardValue: '扫码触发' },
  { abnormalRemark: '', actualValue: '待检', checkResult: 'OK', itemCategory: '过程加检', itemName: '反馈状态', sortNo: 602, standardValue: 'QMS反馈' },
]);

const cutRoundIntermediateLedgerColumns = [
  { dataIndex: 'reportDate', title: '生产日期', width: 120 },
  { dataIndex: 'sliceBatchNo', title: '片号', width: 180 },
  { dataIndex: 'reportTypeName', title: '类型', width: 120 },
  { dataIndex: 'widthMm', title: '宽幅/mm', width: 110 },
  ...Array.from({ length: 10 }, (_, index) => ({
    dataIndex: `thickness${index + 1}`,
    title: `${(index + 1) * 100}cm厚度/mm(1.520±0.050)`,
    width: 190,
  })),
];

const intermediateDetailColumns = [
  { align: 'center' as const, dataIndex: 'seq', title: '序号', width: 48 },
  { dataIndex: 'samplePositionName', title: '采样段', width: 82 },
  { dataIndex: 'sliceBatchNo', title: '片号', width: 190 },
  { dataIndex: 'widthMm', title: '宽幅/mm', width: 108 },
  { dataIndex: 'thickness1', title: '100cm厚度/mm(1.520±0.050)', width: 190 },
  { dataIndex: 'thickness2', title: '200cm厚度/mm(1.520±0.050)', width: 190 },
  { dataIndex: 'thickness3', title: '300cm厚度/mm(1.520±0.050)', width: 190 },
  { dataIndex: 'thickness4', title: '400cm厚度/mm(1.520±0.050)', width: 190 },
  { dataIndex: 'thickness5', title: '500cm厚度/mm(1.520±0.050)', width: 190 },
  { dataIndex: 'thickness6', title: '600cm厚度/mm(1.520±0.050)', width: 190 },
  { dataIndex: 'thickness7', title: '700cm厚度/mm(1.520±0.050)', width: 190 },
  { dataIndex: 'thickness8', title: '800cm厚度/mm(1.520±0.050)', width: 190 },
  { dataIndex: 'thickness9', title: '900cm厚度/mm(1.520±0.050)', width: 190 },
  { dataIndex: 'thickness10', title: '1000cm厚度/mm(1.520±0.050)', width: 196 },
  { dataIndex: 'remark', title: '备注', width: 160 },
];

const selectedDailyRecord = computed(() => dailyRecordRows.value.find((item) => item.key === dailyRecordKey.value));
const selectedReportRecords = computed(() =>
  reportRecords.value.filter((record) => selectedReportRecordKeys.value.includes(record.id as number | string)),
);
function isReportInCurrentSegment(record?: MesHcCutRoundConsoleApi.ReportItem | Record<string, any> | null) {
  const currentSegment = currentSegmentBatchNo.value;
  if (!currentSegment || !record) return true;
  const candidates = [
    resolveMotherBatchNo(record as Record<string, any>),
    stripSegmentMark((record as Record<string, any>).sourceProductionBatchNo),
    stripSegmentMark((record as Record<string, any>).productionBatchNo),
    stripSegmentMark((record as Record<string, any>).parentProductionBatchNo),
    stripSegmentMark((record as Record<string, any>).sourceBatchNo),
  ];
  return candidates.some((item) => isSameText(item, currentSegment));
}
const currentSegmentReportRecords = computed(() => reportRecords.value.filter((record) => isReportInCurrentSegment(record)));
const currentSegmentSampleLockReason = computed(() => sampleLockReason(currentSegmentReportRecords.value));
const inspectionSampleLockReason = computed(() => sampleLockReason(inspectionTaskRows.value));
function sampleLockReason(rows: MesHcCutRoundConsoleApi.ReportItem[]) {
  return [...new Set(rows.map((row) => row.upstreamSampleLockReason).filter(Boolean))].join('；');
}
function warnSampleLocked(rows: MesHcCutRoundConsoleApi.ReportItem[]) {
  const reason = sampleLockReason(rows);
  if (reason) message.warning(reason);
  return !!reason;
}

const printableReportRecords = computed(() =>
  selectedReportRecords.value.filter((record) => !!record.productionBatchNo && isReportInCurrentSegment(record)),
);
// 流转单打印不以报工确认状态、NG 状态或既往打印状态为门槛。
const allPrintableReportRecords = computed(() => currentSegmentReportRecords.value.filter((record) => !!record.productionBatchNo));
const inspectionEligibleReports = computed(() =>
  currentSegmentReportRecords.value.filter((record) =>
    isCutRoundRecordConfirmed(record.reportStatus)
    && !isReportSubmittedForInspection(record),
  ),
);
const inspectionTaskPreviewRows = computed(() =>
  inspectionTaskRows.value.map((record, index) => ({
    ...record,
    inspectionResult: '待检',
    inspectorName: '-',
    parentProductionBatchNo: record.parentProductionBatchNo || resolveMotherBatchNo(record),
    remark: record.inspectionRemark || '',
    seqNo: index + 1,
    sizeRule: getReportActualSizeRule(record) || currentPlan.actualSizeRule || currentPlan.planSizeSpec || '-',
  })),
);
const glueBoardAfterStock = computed(() => Math.max(Number(glueBoard.stockLength || 0) - reportOutputLength.value, 0));
const glueBoardAfterTodayUsed = computed(() => Number(glueBoard.todayUsedLength || 0) + reportOutputLength.value);
const glueBoardAfterNeedSupply = computed(() => glueBoard.stockLength > 0 && glueBoardAfterStock.value < 10);
const glueBoardStatusMeta = computed(() => {
  if (glueBoard.alarm) return { color: 'red', text: '余量提醒' };
  if (!glueBoard.materialCode && glueBoard.stockLength <= 0) return { color: 'default', text: '未加载' };
  return { color: 'green', text: '正常' };
});
const glueBoardAvailableRange = computed(() => {
  const start = Number(glueBoard.availableStartPosition || 0);
  const length = Number(glueBoard.stockLength || 0);
  return `${formatNumber(start)}-${formatNumber(start + length)} m`;
});
const glueBoardAvailableText = computed(() => `${formatNumber(glueBoard.stockLength)} m`);
const glueBoardAqcStatusMeta = computed(() => {
  const status = String(glueBoard.latestAqcTask?.taskStatus || glueBoard.qualityStatus || '').toUpperCase();
  if (status === 'PASSED' || status === 'OK' || status === 'NORMAL') {
    return { color: 'green', stampClass: 'ok', stampText: '首检正常', text: '正常' };
  }
  if (status === 'FAILED' || status === 'NG' || status === 'ABNORMAL') {
    return { color: 'red', stampClass: 'ng', stampText: '首检异常', text: '异常' };
  }
  if (status === 'WAITING' || status === 'SUBMITTED') {
    return { color: 'green', stampClass: 'ok', stampText: '已记录', text: '已记录' };
  }
  return { color: 'default', stampClass: 'empty', stampText: '未记录', text: '未记录' };
});
const glueBoardAqcStampTime = computed(() => {
  const task = glueBoard.latestAqcTask;
  return task?.feedbackTime || task?.submitTime || '未登记';
});
const bladeConsumable = computed(() => findCutRoundConsumable('CUTTING_BLADE'));
const feltConsumable = computed(() => findCutRoundConsumable('CUTTING_FELT'));
const bladeConsumableMeta = computed(() => getCutRoundConsumableMeta(bladeConsumable.value));
const feltConsumableMeta = computed(() => getCutRoundConsumableMeta(feltConsumable.value));
const hasCutRoundConsumableAttention = computed(() =>
  [bladeConsumable.value, feltConsumable.value].some((item) =>
    isCutRoundConsumableMissing(item) || isCutRoundConsumableNeedReplace(item) || isCutRoundConsumableWarning(item),
  ),
);
const cutRoundConsumableTitleTip = computed(() => {
  const base = `${buildCutRoundConsumableRuleText('CUTTING_BLADE', bladeConsumable.value)}，${buildCutRoundConsumableRuleText('CUTTING_FELT', feltConsumable.value)}。`;
  const reminder = getCutRoundConsumableReminder();
  return reminder ? `${base}${reminder}` : base;
});
const activeCutRoundConsumable = computed(() =>
  consumableReplaceType.value === 'CUTTING_BLADE' ? bladeConsumable.value : feltConsumable.value,
);
const activeCutRoundConsumableIssue = computed(() => {
  const current = activeCutRoundConsumable.value;
  if (!current || !current.stateId) return `${activeCutRoundConsumableName.value}未挂接，请先在裁切备件管理维护`;
  if (isCutRoundConsumableMissing(current)) return current.message || `${activeCutRoundConsumableName.value}寿命上限未配置，请先在裁切备件管理维护`;
  if (isCutRoundConsumableNeedReplace(current)) {
    return current.message || `${activeCutRoundConsumableName.value}需更换`;
  }
  return '';
});
function isCutRoundConsumableMissing(item?: any) {
  const status = String(item?.status || '').toUpperCase();
  return !item || !item.stateId || status === 'EMPTY' || status === 'CONFIG_MISSING';
}
function isCutRoundConsumableNeedReplace(item?: any) {
  return !!item && String(item.status || '').toUpperCase() === 'NEED_REPLACE';
}
function isCutRoundConsumableWarning(item?: any) {
  if (!item || isCutRoundConsumableMissing(item) || isCutRoundConsumableNeedReplace(item)) return false;
  return Number(item.warningFlag || 0) === 1 || String(item.status || '').toUpperCase() === 'WARNING';
}
function getCutRoundConsumableIssue() {
  if (!currentPlan.planOperationId) return '请先扫码或选择裁切工单';
  if (!currentPlan.equipmentId) return '当前裁切工序未绑定设备，不能裁切报工';
  const items = [
    { item: bladeConsumable.value, name: '刀片' },
    { item: feltConsumable.value, name: '毛毡' },
  ];
  for (const { item, name } of items) {
    if (isCutRoundConsumableMissing(item)) return item?.message || `${name}未挂接，请先在裁切备件管理维护`;
    if (isCutRoundConsumableNeedReplace(item)) {
      return item.message || `${name}寿命已达上限，请先更换或复位`;
    }
  }
  return '';
}
function getCutRoundConsumableReminder() {
  const items = [
    { item: bladeConsumable.value, name: '刀片' },
    { item: feltConsumable.value, name: '毛毡' },
  ];
  return items
    .filter(({ item }) => isCutRoundConsumableWarning(item))
    .map(({ item, name }) => item?.message || `${name}寿命已达到90%提醒，请及时更换或复位`)
    .join('；');
}
function showCutRoundConsumableRequiredWarning(actionName = '扫码确认') {
  const issue = getCutRoundConsumableIssue();
  if (!issue) return;
  showSinglePlanScanWarning({
    content: `${actionName}前需要完成刀片/毛毡寿命配置：${issue}`,
    okText: '知道了',
    onOk: focusPlanScanInput,
    title: '刀片/毛毡寿命需维护',
  });
}
function ensureCutRoundConsumablesReady(actionName = '扫码确认') {
  const issue = getCutRoundConsumableIssue();
  if (!issue) {
    const reminder = getCutRoundConsumableReminder();
    if (reminder) {
      message.warning(`${actionName}提醒：${reminder}`);
    }
    return true;
  }
  showCutRoundConsumableRequiredWarning(actionName);
  return false;
}
const reportRecordRowSelection = computed(() => ({
  fixed: true,
  onChange: (keys: (number | string)[]) => {
    selectedReportRecordKeys.value = keys;
  },
  selectedRowKeys: selectedReportRecordKeys.value,
}));
const reportOutputLength = computed(() =>
  Math.max(Number(reportForm.processLength || 0) - Number(reportForm.lossLength || 0) - Number(reportForm.napSampleLength || 0), 0),
);
const recordConfirmFinalBatchNo = computed(() =>
  normalizeCutRoundProductionBatchNo(
    recordConfirmForm.scannedBatchNo || activeRecord.value?.productionBatchNo || activeRecord.value?.sourceProductionBatchNo,
    recordConfirmForm.actualSizeRule,
  ),
);
const recordConfirmModelCode = computed(() => {
  const sourceSegment = getCutRoundSourceSegmentByBatchNo(activeRecord.value?.sourceProductionBatchNo);
  return activeRecord.value?.modelCode || sourceSegment?.modelCode || currentCutRoundProductModelCode.value || '-';
});
const reportFinalProductionBatchNo = computed(() =>
  normalizeCutRoundProductionBatchNo(
    reportForm.productionBatchNo || reportForm.sourceProductionBatchNo || reportForm.sourceCode,
    reportForm.actualSizeRule,
  ),
);
const currentMotherBatchNo = computed(() =>
  resolveMotherBatchNo({
    parentProductionBatchNo: currentPlan.sourceBatchNo || currentPlan.batchNo,
    sourceProductionBatchNo: currentPlan.sourceProductionBatchNo,
  }),
);
const currentSegmentBatchNo = computed(() => getCurrentSegmentBatchNo());
const visibleSourceGroups = computed<SourceGroup[]>(() =>
  sourceGroups.value
    .filter((group) => !currentSegmentBatchNo.value || group.baseBatchNo === currentSegmentBatchNo.value)
    .map((group) => {
      const segments = group.segments.filter(
        (segment) => (transferPrintSelectionMode.value || !isCutRoundHiddenUpstreamNgSegment(segment)) && matchCutRoundSegment(segment),
      );
      return {
        ...group,
        segments,
        totalAvailableLength: segments.reduce((sum, item) => sum + Number(item.availableLength || 0), 0),
        totalOutputLength: segments.reduce((sum, item) => sum + Number(item.outputLength || 0), 0),
        totalUsedLength: segments.reduce((sum, item) => sum + Number(item.usedLength || 0), 0),
      };
    })
    .filter((group) => group.segments.length > 0),
);
const visualDisplaySourceGroups = computed<SourceGroup[]>(() => {
  return visibleSourceGroups.value;
});
const selectableTransferTargets = computed(() => {
  const rows = new Map<string, CutRoundTransferPrintTarget>();
  visualDisplaySourceGroups.value.forEach((group) => {
    group.segments.forEach((segment) => {
      if (!isPrintableCutRoundTransferSegment(segment)) return;
      const target: CutRoundTransferPrintTarget = {
        group,
        record: findReportBySegment(segment),
        segment,
      };
      rows.set(getCutRoundTransferPrintTargetKey(target.segment), target);
    });
  });
  return Array.from(rows.values());
});
const selectedTransferTargets = computed(() =>
  selectedTransferReportIds.value
    .map((id) => selectableTransferTargets.value.find((target) => getCutRoundTransferPrintTargetKey(target.segment) === id))
    .filter((target): target is CutRoundTransferPrintTarget => !!target),
);
const selectableTransferReportCount = computed(() => selectableTransferTargets.value.length);
const selectedTransferReportCount = computed(() => selectedTransferTargets.value.length);
const allTransferReportsSelected = computed(
  () =>
    selectableTransferReportCount.value > 0 &&
    selectableTransferTargets.value.every((target) => selectedTransferReportIds.value.includes(getCutRoundTransferPrintTargetKey(target.segment))),
);
const visibleCutRoundSliceCount = computed(() => visibleSourceGroups.value.reduce((sum, group) => sum + group.segments.length, 0));
const visibleCutRoundCompletedCount = computed(() =>
  visibleSourceGroups.value.reduce(
    (sum, group) =>
      sum + group.segments.filter((segment) => ['COMPLETED', 'INSPECTED', 'INSPECTING'].includes(getCutRoundSegmentStatus(segment))).length,
    0,
  ),
);
const visibleCutRoundPendingConfirmCount = computed(() =>
  visibleSourceGroups.value.reduce((sum, group) => sum + group.segments.filter((segment) => getCutRoundSegmentStatus(segment) === 'PENDING').length, 0),
);
const visibleCutRoundCurrentNgCount = computed(() =>
  visibleSourceGroups.value.reduce(
    (sum, group) => sum + group.segments.filter((segment) => getCutRoundSegmentStatus(segment) === 'NG' && !isSourceSegmentNg(segment)).length,
    0,
  ),
);
const visibleCutRoundSourceNgCount = computed(() =>
  visibleSourceGroups.value.reduce(
    (sum, group) =>
      sum +
      group.segments.filter(
        (segment) =>
          getCutRoundSegmentStatus(segment) === 'NG'
          && isSourceSegmentNg(segment)
          && !isCutRoundHiddenUpstreamNgSegment(segment),
      ).length,
    0,
  ),
);
const changeoverStampMeta = computed(() => {
  const latest = latestChangeoverInspection.value;
  if (!currentPlan.planNo) return { stampClass: 'empty', stampText: '待加载', time: '扫码计划后校验工艺参数点检' };
  if (!latest?.id) return { stampClass: 'ng', stampText: '未记录', time: '请先填写工艺参数点检' };
  const currentMother = getCurrentChangeoverMotherBatchNo();
  const latestMother = getLatestChangeoverMotherBatchNo(latest);
  if (!currentMother || latestMother !== currentMother) {
    return { stampClass: 'ng', stampText: '未记录', time: `${latest.currentPlanNo || '-'} / ${latestMother || '-'}` };
  }
  return {
    stampClass: 'ok',
    stampText: isChangeoverInspectionConfirmed(latest) ? '已确认' : '已保存',
    time: displayDateTimeText(latest.recordTime || latest.submitTime),
  };
});

const reportInspectionInfo = computed(() => {
  if (reportForm.reportType === 'CHANGEOVER') {
    const latest = latestChangeoverInspection.value;
    return {
      feedbackResult: latest?.feedbackResult || '-',
      feedbackTime: latest?.feedbackTime || '',
      submitTime: latest?.submitTime || latest?.recordTime || '',
    };
  }
  const extra = parseRecordExtra(activeRecord.value || {}) as Record<string, any>;
  return {
    feedbackResult: extra.feedbackResult || reportForm.aqcStatus || '-',
    feedbackTime: extra.feedbackTime || '',
    submitTime: extra.submitTime || reportForm.startTime || '',
  };
});

function buildDefaultCutRoundIntermediateDetails(): MesHcCutRoundConsoleApi.IntermediateDetail[] {
  return cutRoundMeasuredPositions.map((item, index) => ({
    samplePosition: item.value,
    samplePositionName: item.label,
    remark: '',
    sortNo: index + 1,
  }));
}

const intermediateTableRows = computed(() =>
  intermediateDetails.value.map((row, index) => {
    (row as MesHcCutRoundConsoleApi.IntermediateDetail & { seq: number }).seq = index + 1;
    return row as MesHcCutRoundConsoleApi.IntermediateDetail & { seq: number };
  }),
);

const cutRoundIntermediateLedgerRows = computed(() => {
  const currentMother = currentSegmentBatchNo.value;
  return reportRecords.value
    .filter((record) => {
      const extra = parseRecordExtra(record) as Record<string, any>;
      const reportType = extra.reportType || '';
      if (!isCutRoundIntermediateReportType(reportType)) return false;
      if (!currentMother) return true;
      const recordMother = resolveMotherBatchNo(record);
      return recordMother === currentMother;
    })
    .map((record) => {
      const extra = parseRecordExtra(record) as Record<string, any>;
      const reportType = extra.reportType || '';
      const readValue = (itemName: string) =>
        (record.checkItems || []).find((item) => item.itemCategory === '中间品' && item.itemName === itemName)?.actualValue || '';
      const row: Record<string, any> = {
        id: record.id || record.productionBatchNo,
        reportDate: record.reportDate,
        reportTypeName: getCutRoundReportTypeText(reportType),
        sliceBatchNo: record.productionBatchNo || record.sourceProductionBatchNo || '',
        widthMm: readValue('宽幅/mm'),
      };
      for (let i = 1; i <= 10; i += 1) {
        row[`thickness${i}`] = readValue(`${i * 100}cm厚度/mm`);
      }
      return row;
    });
});

const reportCheckCategories = computed(() => {
  const categories: string[] = [];
  if (shouldShowProcessParameterTab.value) {
    const baseItems = ensureCutRoundChangeoverItems(checkTemplate.value);
    categories.push(
      ...Array.from(new Set(
        baseItems
          .map((item) => item.itemCategory || '其他')
          .filter((category) => category !== '粘胶2段半成品'),
      )),
    );
  }
  if (isCutRoundIntermediateReportType(reportForm.reportType)) {
    categories.push('中间品');
  }
  const order = ['环境', '粘胶机', '胶板', '工艺参数', '过程加检', '中间品', '其他'];
  return Array.from(new Set(categories)).sort((a, b) => {
    const aIndex = order.includes(a) ? order.indexOf(a) : order.length;
    const bIndex = order.includes(b) ? order.indexOf(b) : order.length;
    return aIndex - bIndex;
  });
});
const shouldShowProcessParameterTab = computed(() => reportForm.reportType === 'CHANGEOVER');
const shouldShowVisualInspectionTab = computed(() => reportForm.reportType !== 'CHANGEOVER');
const isVisualInspectionEditable = computed(() => reportDialogMode.value !== 'view' && shouldShowVisualInspectionTab.value);
const canCorrectAbnormalCategory = computed(() =>
  reportDialogMode.value === 'view'
  && !!activeRecord.value?.id
  && shouldShowVisualInspectionTab.value
  && hasCutRoundReportVisualIssue(activeRecord.value)
  && !isPreProcessSelfCheckAbnormal(parseRecordExtra(activeRecord.value))
  && hasAccessByCodes([ABNORMAL_CATEGORY_CORRECT_PERMISSION]),
);
const hasActiveVisualItems = computed(() => visualInspectionItems.value.some((item) => isVisualItemActive(item)));
const hasActiveInheritedVisualItems = computed(() =>
  visualInspectionItems.value.some((item) => item.inheritedFromAdhesive2 && isVisualItemActive(item)),
);
const reportSubmitButtonText = computed(() => {
  if (activeReportTab.value === 'visual-inspection') return '保存并扫码确认';
  if (shouldShowVisualInspectionTab.value) return '下一步：外观检验';
  return '保存并扫码确认';
});
const startReportWorkButtonText = computed(() => {
  if (shouldShowVisualInspectionTab.value) return '开始外观检验';
  return '进入工艺参数';
});
const reportDialogTitle = computed(() => {
  if (reportDialogMode.value === 'view') return '裁切片详情查看';
  return shouldShowProcessParameterTab.value ? '裁切扫码确认与工艺参数登记' : '裁切扫码确认与外观检验';
});
const recordConfirmTypeBasis = computed(() => {
  const currentMother = currentSegmentBatchNo.value || '-';
  const totalCount = visibleSourceGroups.value.reduce((sum, group) => sum + group.segments.length, 0);
  const confirmedCount = reportRecords.value.filter((record) =>
    isCutRoundRecordConfirmed(record.reportStatus)
    && (!currentSegmentBatchNo.value || resolveMotherBatchNo(record) === currentSegmentBatchNo.value),
  ).length;
  const currentIndex = totalCount > 0 ? Math.min(confirmedCount + 1, totalCount) : confirmedCount + 1;
  return `系统默认：${getCutRoundReportTypeText(getDefaultCutRoundReportType())}；依据：当前分段批次号 ${currentMother}，当前是第 ${currentIndex} 片 / 共 ${totalCount || '-'} 片`;
});

const recordColumns = [
  { dataIndex: 'parentProductionBatchNo', key: 'motherBatchNo', title: '分段批次号', width: 180 },
  { dataIndex: 'productionBatchNo', key: 'productionBatchNo', title: '裁切片号', width: 160 },
  { dataIndex: 'confirmerTime', key: 'scanConfirmDate', title: '扫码确认日期', width: 130 },
  {
    dataIndex: 'printCount',
    fixed: 'right' as const,
    key: 'printCount',
    title: '打印次数',
    width: 92,
  },
  {
    dataIndex: 'printStatus',
    fixed: 'right' as const,
    key: 'printStatus',
    title: '打印标记',
    width: 112,
  },
  {
    dataIndex: 'reportStatus',
    fixed: 'right' as const,
    key: 'reportStatus',
    title: '确认状态',
    width: 112,
  },
  {
    dataIndex: 'inspectionStatus',
    fixed: 'right' as const,
    key: 'inspectionStatus',
    title: '检验状态',
    width: 112,
  },
];

const changeoverInspectionColumns = [
  { dataIndex: 'recordTime', title: '记录时间', width: 170 },
  { dataIndex: 'motherSegmentBatchNo', title: '母卷批号', width: 180 },
  { dataIndex: 'inspectionStatus', title: '状态', width: 110 },
  { dataIndex: 'recorderName', title: '记录人', width: 120 },
  { dataIndex: 'confirmerName', title: '确认人', width: 120 },
  { dataIndex: 'action', fixed: 'right' as const, title: '操作', width: 180 },
];

const inspectionTaskColumns = [
  { dataIndex: 'fqcNo', title: 'FQC单号', width: 210 },
  { dataIndex: 'taskStatus', title: '任务状态', width: 110 },
  { dataIndex: 'reportProcess', title: '报检工序', width: 120 },
  { dataIndex: 'reporterName', title: '报检人', width: 110 },
  { dataIndex: 'reportTime', title: '报检时间', width: 170 },
  { align: 'center' as const, dataIndex: 'detailCount', title: '明细数', width: 90 },
  { dataIndex: 'remark', title: '备注说明', width: 180 },
  { dataIndex: 'action', fixed: 'right' as const, title: '操作', width: 110 },
];

const inspectionTaskDetailColumns = [
  { align: 'center' as const, dataIndex: 'seqNo', title: '序号', width: 70 },
  { dataIndex: 'parentProductionBatchNo', title: '分段批次', width: 160 },
  { dataIndex: 'materialCode', title: '产品料号', width: 140 },
  { dataIndex: 'modelCode', title: '产品型号', width: 150 },
  { dataIndex: 'sizeRule', title: '尺寸', width: 100 },
  { dataIndex: 'productionBatchNo', title: '片号', width: 180 },
  { dataIndex: 'fqcNo', title: 'FQC单号', width: 180 },
  { dataIndex: 'fqcStatus', title: 'FQC状态', width: 110 },
  { dataIndex: 'fqcJudgment', title: 'FQC判定', width: 110 },
  { dataIndex: 'inspectionResult', title: '检验结果', width: 110 },
  { dataIndex: 'inspectorName', title: '检验员', width: 110 },
  { dataIndex: 'remark', title: '备注说明', width: 180 },
  { dataIndex: 'action', fixed: 'right' as const, title: '操作', width: 120 },
];

const inspectionTaskCreateColumns = [
  { align: 'center' as const, dataIndex: 'seqNo', title: '序号', width: 70 },
  { dataIndex: 'parentProductionBatchNo', title: '分段批次', width: 160 },
  { dataIndex: 'materialCode', title: '产品料号', width: 140 },
  { dataIndex: 'modelCode', title: '产品型号', width: 150 },
  { dataIndex: 'sizeRule', title: '尺寸', width: 100 },
  { dataIndex: 'productionBatchNo', title: '片号', width: 180 },
  { dataIndex: 'inspectionResult', title: '检验结果', width: 110 },
  { dataIndex: 'inspectorName', title: '检验员', width: 110 },
  { dataIndex: 'remark', title: '备注说明', width: 180 },
  { dataIndex: 'action', fixed: 'right' as const, title: '操作', width: 90 },
];

const reportProcessColumns = [
  { dataIndex: 'itemCategory', title: '类别', width: 120 },
  { dataIndex: 'itemName', title: '工艺参数项目', width: 170 },
  { dataIndex: 'standardValue', title: '标准', width: 180 },
  { dataIndex: 'actualValue', title: '实测值', width: 200 },
  { dataIndex: 'abnormalRemark', title: '异常备注', width: 180 },
];

function normalizeRows(payload: any): any[] {
  const source = payload?.data ?? payload;
  if (Array.isArray(source)) return source;
  if (Array.isArray(source?.list)) return source.list;
  if (Array.isArray(source?.rows)) return source.rows;
  if (Array.isArray(source?.records)) return source.records;
  return [];
}

function formatNumber(value?: number) {
  const num = Number(value || 0);
  if (!Number.isFinite(num)) return '0';
  return Number(num.toFixed(3)).toString();
}

function getCutRoundQtimeText(qtime?: MesHcCutRoundConsoleApi.QtimeInfo) {
  if (!qtime) return '-';
  if (qtime.status === 'WAITING_SOURCE_FINISH') return '0分钟（并行开工）';
  if (qtime.status === 'MISSING_SOURCE_TIME') return '上道未完工';
  if (qtime.status === 'NO_RULE') return '未配置';
  const minutes = Math.max(0, Number(qtime.elapsedMinutes || 0));
  return qtime.timeout ? `超时 ${minutes}分钟` : `${minutes}分钟`;
}

function getCutRoundQtimeColor(qtime?: MesHcCutRoundConsoleApi.QtimeInfo) {
  if (qtime?.timeout) return 'red';
  if (qtime?.status === 'WAITING_SOURCE_FINISH') return 'blue';
  if (qtime?.status === 'NORMAL') return 'green';
  return 'default';
}

function mergeCutRoundBatchQtime(
  current?: MesHcCutRoundConsoleApi.QtimeInfo,
  candidate?: MesHcCutRoundConsoleApi.QtimeInfo,
) {
  if (!current) return candidate;
  if (!candidate) return current;
  if (!current.targetStarted) return candidate.targetStarted ? candidate : current;
  if (!candidate.targetStarted) return current;
  const currentStart = String(current.targetStartTime || '9999-12-31 23:59:59');
  const candidateStart = String(candidate.targetStartTime || '9999-12-31 23:59:59');
  return candidateStart < currentStart ? candidate : current;
}

function formatDateTimeText(value?: string) {
  if (!value) return '-';
  const parsed = dayjs(value);
  return parsed.isValid() ? parsed.format('YYYY-MM-DD HH:mm:ss') : value;
}

function formatDateText(value?: string) {
  if (!value) return '-';
  const parsed = dayjs(value);
  return parsed.isValid() ? parsed.format('YYYY-MM-DD') : value;
}

function findCutRoundConsumable(type: string) {
  return cutRoundConsumables.value.find((item) => String(item?.consumableType || '').toUpperCase() === type);
}

function getCutRoundConsumableMeta(item?: any) {
  if (!item) return { color: 'default', text: '未挂接' };
  if (isCutRoundConsumableNeedReplace(item)) {
    return { color: 'red', text: item.message || '需更换' };
  }
  if (isCutRoundConsumableMissing(item)) {
    return { color: 'default', text: item.message || '未挂接' };
  }
  if (isCutRoundConsumableWarning(item)) {
    return { color: 'orange', text: item.message || '90%提醒' };
  }
  return { color: 'green', text: item.message || '正常' };
}

function resolveCutRoundConsumableLimitCount(type: CutRoundConsumableType, item?: any) {
  if (type !== 'CUTTING_BLADE' && type !== 'CUTTING_FELT') {
    return undefined;
  }
  const configured = Number(item?.limitCount);
  if (Number.isFinite(configured) && configured > 0) {
    return configured;
  }
  return undefined;
}

function resolveCutRoundConsumableLimitDays(type: CutRoundConsumableType, item?: any) {
  if (type !== 'CUTTING_FELT') {
    return undefined;
  }
  const configured = Number(item?.limitDays);
  if (Number.isFinite(configured) && configured > 0) {
    return configured;
  }
  return undefined;
}

function formatCutRoundConsumableLimitDays(days: number) {
  if (days === 180) return '半年';
  if (days === 90) return '3个月';
  return `${days}天`;
}

function buildCutRoundConsumableRuleText(type: CutRoundConsumableType, item?: any) {
  const limitCount = resolveCutRoundConsumableLimitCount(type, item);
  if (type === 'CUTTING_BLADE') {
    return limitCount
      ? `刀片使用寿命≤${limitCount}次（90%预警）`
      : '刀片使用寿命未配置';
  }
  const limitDays = resolveCutRoundConsumableLimitDays(type, item);
  const dayText = limitDays ? formatCutRoundConsumableLimitDays(limitDays) : '天数未配置';
  const countText = limitCount ? `${limitCount}片` : '片数未配置';
  return `毛毡使用寿命≤${dayText}或${countText}（90%预警）`;
}

function formatCutRoundConsumableCountText(type: CutRoundConsumableType, item: any, unit: string) {
  const limitCount = resolveCutRoundConsumableLimitCount(type, item);
  return `${Number(item?.useCount || 0)} / ${limitCount ?? '未配置'} ${unit}`;
}

function formatCutRoundConsumableDaysText(item: any) {
  const limitDays = resolveCutRoundConsumableLimitDays('CUTTING_FELT', item);
  return `${Number(item?.useDays || 0)} / ${limitDays ?? '未配置'} 天`;
}

function openCutRoundConsumableReplaceDialog(type: CutRoundConsumableType) {
  if (consumableSubmitting.value) return;
  bladeLedger.value = undefined;
  bladeReplaceQuantity.value = 1;
  bladeRequestKey.value = `blade-${Date.now()}-${Math.random().toString(36).slice(2)}-${Math.random().toString(36).slice(2)}`;
  consumableReplaceType.value = type;
  const current = type === 'CUTTING_BLADE' ? bladeConsumable.value : feltConsumable.value;
  Object.assign(consumableReplaceForm, {
    initialUseCount: 0,
    lastReplaceTime: current?.lastReplaceTime || '',
    limitCount: resolveCutRoundConsumableLimitCount(type, current) ?? 0,
    replaceReason: '',
    replaceTime: buildNowText(),
    useCount: Number(current?.useCount || 0),
  });
  consumableReplaceVisible.value = true;
}

async function confirmCutRoundConsumableReplace() {
  if (consumableSubmitting.value) return;
  if (consumableReplaceType.value === 'CUTTING_BLADE' && (!bladeLedger.value?.id
      || !Number.isInteger(bladeReplaceQuantity.value) || bladeReplaceQuantity.value <= 0
      || Number(bladeLedger.value.balanceQty || 0) < bladeReplaceQuantity.value)) {
    message.warning('刀片可用数量不足或未选择领用记录，请先完成刀片领用');
    return;
  }
  if (!currentPlan.planOperationId) {
    message.warning('请先扫码或选择裁切工单');
    return;
  }
  if (!currentPlan.equipmentId) {
    message.warning('当前裁切工单未绑定机台，不能更换耗材');
    return;
  }
  if (!activeCutRoundConsumable.value?.stateId) {
    message.warning(`请先在裁切备件管理维护${activeCutRoundConsumableName.value}基础信息`);
    return;
  }
  if (!normalizeDateTimeText(consumableReplaceForm.replaceTime)) {
    message.warning('请选择上次更换时间');
    return;
  }
  if (Number(consumableReplaceForm.initialUseCount || 0) < 0) {
    message.warning('初始化片数不能小于0');
    return;
  }
  if (!String(consumableReplaceForm.replaceReason || '').trim()) {
    message.warning('请填写更换原因');
    return;
  }
  pendingConsumableAction.value = consumableReplaceType.value;
  consumableAuthAction.value = `确认${activeCutRoundConsumableName.value}更换/复位并记录人员`;
  consumableAuthVisible.value = true;
}

async function handleConsumableAuthSuccess(userInfo: any) {
  if (!pendingConsumableAction.value || consumableSubmitting.value) return;
  consumableSubmitting.value = true;
  try {
    await replaceCutRoundConsumable({
      ledgerId: pendingConsumableAction.value === 'CUTTING_BLADE' ? bladeLedger.value?.id : undefined,
      requestKey: pendingConsumableAction.value === 'CUTTING_BLADE' ? bladeRequestKey.value : undefined,
      replaceQuantity: pendingConsumableAction.value === 'CUTTING_BLADE' ? bladeReplaceQuantity.value : undefined,
      consumableType: pendingConsumableAction.value,
      equipmentCode: currentPlan.equipmentCode,
      equipmentId: currentPlan.equipmentId,
      equipmentName: currentPlan.equipmentName,
      operatorId: userInfo?.userId,
      operatorName: userInfo?.empName || currentUserName.value || undefined,
      planId: currentPlan.planId,
      planNo: currentPlan.planNo,
      planOperationId: currentPlan.planOperationId,
      initialUseCount: pendingConsumableAction.value === 'CUTTING_BLADE' ? 0 : Number(consumableReplaceForm.initialUseCount || 0),
      replaceReason: consumableReplaceForm.replaceReason || undefined,
      replaceTime: normalizeDateTimeText(consumableReplaceForm.replaceTime),
    });
    consumableReplaceVisible.value = false;
    consumableAuthVisible.value = false;
    message.success(consumableReplaceType.value === 'CUTTING_BLADE' ? '刀片更换成功，已扣减边库耗材' : '毛毡更换/复位记录已保存');
    pendingConsumableAction.value = '';
    consumableAuthAction.value = '';
    await loadGlueBoardUsage();
  } finally {
    consumableSubmitting.value = false;
  }
}

function roundMeter(value: number) {
  if (!Number.isFinite(value)) return 0;
  return Number(value.toFixed(3));
}

function buildReportRange(record: MesHcCutRoundConsoleApi.ReportItem): SourceReportRange | null {
  const fallbackLength = Number(record.inputLength || record.outputLength || 0);
  let start = Number(record.startPosition ?? 0);
  let end = Number(record.endPosition ?? start + fallbackLength);
  if (!Number.isFinite(start)) start = 0;
  if (!Number.isFinite(end)) end = start + fallbackLength;
  if (end <= start) return null;
  return {
    end: roundMeter(end),
    label: record.productionBatchNo || record.sourceProductionBatchNo || '已报工',
    reportId: record.id,
    start: roundMeter(start),
    status: record.reportStatus,
  };
}

function isCutRoundReportForSource(
  record: MesHcCutRoundConsoleApi.ReportItem,
  sourceProductionBatchNo: string,
  sourceGrindingSecondDetailId?: number,
) {
  const sourceReportId = Number(sourceGrindingSecondDetailId || 0);
  if (sourceReportId > 0) {
    return Number(record.sourceGrindingSecondDetailId || 0) === sourceReportId;
  }
  return String(record.sourceProductionBatchNo || '') === sourceProductionBatchNo;
}

function getReportRanges(sourceProductionBatchNo: string, sourceGrindingSecondDetailId?: number) {
  return reportRecords.value
    .filter((item) => isCutRoundReportForSource(item, sourceProductionBatchNo, sourceGrindingSecondDetailId))
    .map(buildReportRange)
    .filter((item): item is SourceReportRange => !!item)
    .sort((a, b) => a.start - b.start || a.end - b.end);
}

function getAvailableLengthByRanges(outputLength: number, ranges: SourceReportRange[]) {
  if (outputLength <= 0) return 0;
  let cursor = 0;
  let available = 0;
  ranges.forEach((range) => {
    if (range.start > cursor) {
      available += range.start - cursor;
    }
    cursor = Math.max(cursor, range.end);
  });
  if (outputLength > cursor) {
    available += outputLength - cursor;
  }
  return roundMeter(Math.max(available, 0));
}

function findFirstAvailableRange(segment: SourceSegment) {
  const outputLength = Number(segment.outputLength || 0);
  let cursor = 0;
  for (const range of segment.reportRanges) {
    if (range.start > cursor) {
      return {
        end: roundMeter(range.start),
        length: roundMeter(range.start - cursor),
        start: roundMeter(cursor),
      };
    }
    cursor = Math.max(cursor, range.end);
  }
  return {
    end: roundMeter(outputLength),
    length: roundMeter(Math.max(outputLength - cursor, 0)),
    start: roundMeter(cursor),
  };
}

function findSourceSegment(batchNo?: string) {
  const sourceBatchNo = String(batchNo || '');
  for (const group of sourceGroups.value) {
    const segment = group.segments.find((item) => item.batchNo === sourceBatchNo);
    if (segment) return segment;
  }
  return undefined;
}

function findSourceSegmentWithGroup(batchNo?: string) {
  const scanned = String(batchNo || '').trim().toUpperCase();
  if (!scanned) return undefined;
  const scannedBase = stripCutRoundSizeSuffix(scanned);
  for (const group of sourceGroups.value) {
    const segment = group.segments.find((item) => {
      const batchNo = String(item.batchNo || '').trim().toUpperCase();
      return batchNo === scanned
        || (!getInheritedCutRoundSizeRule(item) && stripCutRoundSizeSuffix(batchNo) === scannedBase);
    });
    if (segment) return { group, segment };
  }
  return undefined;
}

function findSourceGroup(batchNo?: string) {
  const normalized = resolveMotherBatchNo({
    batchNo,
    parentProductionBatchNo: batchNo,
    productionBatchNo: batchNo,
    sourceBatchNo: batchNo,
    sourceProductionBatchNo: batchNo,
  });
  if (!normalized) return undefined;
  return sourceGroups.value.find((group) => group.baseBatchNo === normalized);
}

function getActiveSourceGroup() {
  const batchNo = currentSegmentBatchNo.value || currentPlan.sourceBatchNo || currentPlan.batchNo;
  return findSourceGroup(batchNo) || sourceGroups.value[0];
}

function getSourceGroupReportRecords(group?: SourceGroup | null) {
  if (!group?.baseBatchNo) return [];
  return reportRecords.value.filter((record) => resolveMotherBatchNo(record as Record<string, any>) === group.baseBatchNo);
}

function isSourceGroupCompleted(group?: SourceGroup | null) {
  const records = getSourceGroupReportRecords(group);
  return records.length > 0 && records.every((record) => isCutRoundRecordSubmitted(record.reportStatus));
}

async function refreshActiveSourceGroup(group: SourceGroup) {
  await loadReports();
  await loadSourceGroups(group.baseBatchNo);
  return findSourceGroup(group.baseBatchNo) || group;
}

function markTaskListSourceGroupCompleted(group?: SourceGroup | null) {
  if (!group?.baseBatchNo) return;
  taskRows.value = taskRows.value.map((row) => {
    if (resolveMotherBatchNo(row as Record<string, any>) !== group.baseBatchNo) return row;
    return {
      ...row,
      availableSourceLength: 0,
      endTime: buildNowText(),
      status: 'COMPLETED',
    };
  });
  if (currentSegmentBatchNo.value === group.baseBatchNo) {
    currentPlan.availableSourceLength = 0;
  }
}

function getRangeStyle(range: SourceReportRange, segment: SourceSegment) {
  const outputLength = Number(segment.outputLength || 0);
  if (outputLength <= 0) return { left: '0%', width: '0%' };
  const left = Math.min(Math.max((range.start / outputLength) * 100, 0), 100);
  const width = Math.min(Math.max(((range.end - range.start) / outputLength) * 100, 0), 100 - left);
  return { left: `${left}%`, width: `${width}%` };
}

function getCutRoundGridColumns(_count: number) {
  return 'repeat(auto-fill, 168px)';
}

function getCutRoundSegmentStatus(segment: SourceSegment) {
  if (getCutRoundSegmentReportNgText(segment)) return 'NG';
  const report = findReportBySegment(segment);
  if (isReportInspectionCompleted(report)) return 'INSPECTED';
  if (isReportInspecting(report)) return 'INSPECTING';
  if (report || segment.usedLength > 0) return 'COMPLETED';
  if (segment.availableLength <= 0) return 'COMPLETED';
  return 'PENDING';
}

function isSourceSegmentNg(segment: SourceSegment) {
  return Boolean(segment.sourceNgText) || isCutRoundPreProcessAttributedNg(segment);
}

function isCutRoundHiddenUpstreamNgSegment(segment: SourceSegment) {
  const processName = normalizeProcessName(segment.sourceNgProcessName || segment.sourceNgText?.replace(/工序NG$/, ''));
  return processName === '分切' || processName === '压槽';
}

function isCutRoundPreProcessAttributedNg(segment: SourceSegment) {
  const record = findReportBySegment(segment);
  return Boolean(
    record &&
      hasProcessNgFromRecord(record) &&
      isPreProcessSelfCheckAbnormal(parseRecordExtra(record)),
  );
}

function matchCutRoundSegment(segment: SourceSegment) {
  const keyword = visualFilterForm.batchNo.trim().toLowerCase();
  if (keyword) {
    const record = findReportBySegment(segment);
    const matched = [
      segment.batchNo,
      getCutRoundSegmentDisplayBatchNo(segment),
      segment.segmentMark,
      segment.label,
      segment.sourceNgText,
      segment.sourceNgReason,
      getCutRoundSegmentReportNgText(segment),
      getNgReasonText(record),
    ]
      .filter(Boolean)
      .some((value) => String(value).toLowerCase().includes(keyword));
    if (!matched) return false;
  }
  if (visualFilterForm.status === 'SOURCE_NG') return isSourceSegmentNg(segment);
  const status = getCutRoundSegmentStatus(segment);
  if (visualFilterForm.status === 'CURRENT_NG') return status === 'NG' && !isSourceSegmentNg(segment);
  if (visualFilterForm.status === 'COMPLETED') return ['COMPLETED', 'INSPECTED', 'INSPECTING'].includes(status);
  if (visualFilterForm.status !== 'ALL' && status !== visualFilterForm.status) return false;
  if (visualFilterForm.reportType !== 'ALL' && getCutRoundSegmentReportType(segment) !== visualFilterForm.reportType) return false;
  return true;
}

function setCutRoundFilterStatus(status: string) {
  visualFilterForm.status = status;
}

function setCutRoundFilterReportType(reportType: string) {
  visualFilterForm.reportType = reportType;
}

function getCutRoundSliceClass(segment: SourceSegment) {
  const status = getCutRoundSegmentStatus(segment);
  const typeClass = getCutRoundReportTypeClass(getCutRoundSegmentReportType(segment));
  const flags = [
    isSourceSegmentNg(segment) ? 'is-source-ng' : '',
    getCutRoundSegmentCoaFlag(segment) ? 'is-coa' : '',
    getCutRoundSegmentReportNgText(segment) ? 'is-visual-ng' : '',
    getCutRoundSegmentInspectionNg(segment) ? 'is-inspection-ng' : '',
    getCutRoundSegmentInspectionBackgroundClass(segment),
    getCutRoundSegmentRuntimeMismatch(segment) ? 'is-runtime-mismatch' : '',
    getCutRoundSegmentBatchChanged(segment) ? 'is-runtime-mismatch' : '',
    transferPrintSelectionMode.value ? 'is-transfer-selectable' : '',
    transferPrintSelectionMode.value && !isPrintableCutRoundTransferSegment(segment) ? 'is-transfer-disabled' : '',
    transferPrintSelectionMode.value && isTransferSegmentSelected(segment) ? 'is-transfer-selected' : '',
  ].filter(Boolean);
  if (status === 'NG') return ['slice-cell', isSourceSegmentNg(segment) ? 'is-source-ng' : 'is-current-ng', typeClass, ...flags];
  if (status === 'INSPECTED') return ['slice-cell', 'is-inspected', typeClass, ...flags];
  if (status === 'INSPECTING') return ['slice-cell', 'is-inspecting', typeClass, ...flags];
  if (status === 'COMPLETED') return ['slice-cell', 'is-confirmed', typeClass, ...flags];
  return ['slice-cell', 'is-scan-pending', typeClass, ...flags];
}

function normalizeRuntimeCompareText(value?: unknown) {
  return String(value ?? '').trim().toUpperCase();
}

function isRuntimeTextChanged(actual?: unknown, planned?: unknown) {
  const actualText = normalizeRuntimeCompareText(actual);
  const plannedText = normalizeRuntimeCompareText(planned);
  return !!actualText && !!plannedText && actualText !== plannedText;
}

function getCutRoundSourceSegmentByBatchNo(batchNo?: string) {
  const target = String(batchNo || '').trim();
  if (!target) return undefined;
  for (const group of sourceGroups.value) {
    const matched = group.segments.find((segment) => segment.batchNo === target);
    if (matched) return matched;
  }
  return undefined;
}

function getCutRoundSegmentRuntimeMeta(segment: SourceSegment) {
  const record = findReportBySegment(segment);
  const extra = parseRecordExtra(record || (segment as Record<string, any>)) as Record<string, any>;
  const actualModelCode = String(
    record?.modelCode || extra.runtimeModelCode || extra.modelCode || extra.productionModelCode || segment.modelCode || '',
  ).trim();
  const plannedModelCode = String(extra.plannedModelCode || currentPlan.modelCode || '').trim();
  const modelChanged = isRuntimeTextChanged(actualModelCode, plannedModelCode);
  return {
    actualModelCode,
    changed: modelChanged,
    modelChanged,
    plannedModelCode,
  };
}

function getCutRoundSegmentRuntimeMismatch(segment: SourceSegment) {
  return getCutRoundSegmentRuntimeMeta(segment).changed;
}

function getCutRoundSegmentRuntimeText(segment: SourceSegment) {
  const meta = getCutRoundSegmentRuntimeMeta(segment);
  return meta.changed && meta.actualModelCode ? `型号 ${meta.actualModelCode}` : '';
}

function getCutRoundSegmentBatchMeta(segment: SourceSegment) {
  const record = findReportBySegment(segment);
  const sourceBatchNo = String(record?.sourceProductionBatchNo || segment.batchNo || '').trim().toUpperCase();
  const productionBatchNo = String(record?.productionBatchNo || sourceBatchNo).trim().toUpperCase();
  const sourceSuffix = resolveCutRoundExistingSizeSuffix(sourceBatchNo);
  const productionSuffix = resolveCutRoundExistingSizeSuffix(productionBatchNo);
  const plannedSizeRule = resolveCutRoundSizeRuleFromSuffix(sourceSuffix);
  const actualSizeRule = resolveCutRoundSizeRuleFromSuffix(productionSuffix);
  return {
    changed: !!record?.id && sourceSuffix === 'A' && productionSuffix === 'B',
    actualSizeRule,
    plannedSizeRule,
    productionBatchNo,
    productionSuffix,
    sourceBatchNo,
    sourceSuffix,
  };
}

function getCutRoundSegmentDisplayBatchNo(segment: SourceSegment) {
  return getCutRoundSegmentBatchMeta(segment).productionBatchNo || segment.batchNo;
}

function getCutRoundSegmentBatchChanged(segment: SourceSegment) {
  return getCutRoundSegmentBatchMeta(segment).changed;
}

function getCutRoundSegmentBatchChangeText(segment: SourceSegment) {
  const meta = getCutRoundSegmentBatchMeta(segment);
  if (!meta.changed) return '';
  return `${meta.plannedSizeRule || '计划尺寸'}->${meta.actualSizeRule || '实际尺寸'}`;
}

function getCutRoundSegmentRuntimeTitle(segment: SourceSegment) {
  const meta = getCutRoundSegmentRuntimeMeta(segment);
  const batchMeta = getCutRoundSegmentBatchMeta(segment);
  if (!meta.changed && !batchMeta.changed) return batchMeta.productionBatchNo || segment.batchNo;
  const lines = [`裁切片号：${batchMeta.productionBatchNo || segment.batchNo}`];
  if (batchMeta.changed) {
    lines.push(`改型：${getCutRoundSegmentBatchChangeText(segment)}`);
  }
  if (meta.changed) {
    lines.push(`实际型号：${meta.actualModelCode || '-'}；计划型号：${meta.plannedModelCode || '-'}`);
  }
  return lines.join('\n');
}

function getReportFormProductModelCode() {
  const record = activeRecord.value;
  const extra = parseRecordExtra(record || {}) as Record<string, any>;
  const sourceSegment = getCutRoundSourceSegmentByBatchNo(reportForm.sourceProductionBatchNo || reportForm.sourceCode);
  return String(record?.modelCode || extra.runtimeModelCode || reportForm.modelCode || sourceSegment?.modelCode || currentCutRoundProductModelCode.value || '').trim();
}
function buildNowText() {
  return dayjs().format('YYYY-MM-DD HH:mm:ss');
}

function normalizeDateTimeText(value?: null | number | string) {
  const text = String(value ?? '').trim();
  if (!text || text === '-' || text === '0') return '';
  const parsed = dayjs(text);
  if (!parsed.isValid() || parsed.year() < 2000) return '';
  return parsed.format('YYYY-MM-DD HH:mm:ss');
}

function displayDateTimeText(value?: null | number | string) {
  return normalizeDateTimeText(value) || '-';
}

function normalizeRecordTimeText(value?: null | number | string, fallback = buildNowText()) {
  return normalizeDateTimeText(value) || fallback;
}

function normalizeWorkOrderStatus(status?: string) {
  const raw = String(status || '').trim();
  const upper = raw.toUpperCase();
  if (['FINISHED', 'COMPLETED', 'DONE'].includes(upper) || raw.includes('完工') || raw.includes('完成')) return 'FINISHED';
  if (['RUNNING', 'IN_PROGRESS', 'PROCESSING'].includes(upper) || raw.includes('执行') || raw.includes('生产')) return 'RUNNING';
  if (upper === 'PAUSED' || raw.includes('暂停')) return 'PAUSED';
  if (['CANCELLED', 'CANCELED'].includes(upper) || raw.includes('取消') || raw.includes('作废')) return 'CANCELLED';
  return 'PENDING';
}

function getWorkOrderStatusMeta(status?: string) {
  const normalized = normalizeWorkOrderStatus(status);
  if (normalized === 'FINISHED') return { color: 'green', text: '已完工' };
  if (normalized === 'RUNNING') return { color: 'blue', text: '生产中' };
  if (normalized === 'PAUSED') return { color: 'warning', text: '已暂停' };
  if (normalized === 'CANCELLED') return { color: 'red', text: '已取消' };
  return { color: 'orange', text: '待开工' };
}

function getTaskActionText(status?: string) {
  const normalized = normalizeWorkOrderStatus(status);
  if (normalized === 'RUNNING') return '继续';
  if (['CANCELLED', 'FINISHED', 'PAUSED'].includes(normalized)) return '查看';
  return '开工';
}

function containsTaskFilterText(values: unknown[], keyword: string) {
  const text = String(keyword || '').trim().toLowerCase();
  if (!text) return true;
  return values.some((value) => String(value ?? '').toLowerCase().includes(text));
}

const taskListFilteredRows = computed(() => {
  const statusFilter = taskListFilters.status;
  return taskRows.value.filter((row) => {
    const status = normalizeWorkOrderStatus(row?.status);
    if (!isTaskMatchedBoardEquipment(row)) return false;
    if (statusFilter === 'UNFINISHED' && status === 'FINISHED') return false;
    if (statusFilter && statusFilter !== 'ALL' && statusFilter !== 'UNFINISHED' && status !== statusFilter) return false;
    if (!containsTaskFilterText([row?.planNo, row?.id], taskListFilters.planNo)) return false;
    if (
      !containsTaskFilterText(
        [row?.motherModelCode, row?.modelCode, row?.materialCode, row?.motherMaterialCode],
        taskListFilters.modelCode,
      )
    ) {
      return false;
    }
    return containsTaskFilterText(
      [
        row?.batchNo,
        row?.parentProductionBatchNo,
        row?.productionBatchNo,
        row?.sourceBatchNo,
        row?.sourceProductionBatchNo,
        resolveMotherBatchNo(row as Record<string, any>),
      ],
      taskListFilters.batchNo,
    );
  });
});

const taskListPagedRows = computed(() => {
  const start = (taskListPage.value - 1) * taskListPageSize.value;
  return taskListFilteredRows.value.slice(start, start + taskListPageSize.value);
});

function getTaskListTextVisualWidth(value: unknown) {
  const text = String(value ?? '').trim() || '-';
  return Array.from(text).reduce((sum, char) => sum + (char.charCodeAt(0) > 255 ? 14 : 8), 0);
}

function clampTaskListColumnWidth(width: number, min: number, max: number) {
  return Math.min(max, Math.max(min, Math.ceil(width)));
}

function getTaskListColumnWidth(column: (typeof taskListBaseColumns)[number]) {
  const dataIndex = String(column.dataIndex);
  const rule = taskListColumnWidthRules[dataIndex] || { max: 220, min: 100, padding: 32 };
  const titleWidth = getTaskListTextVisualWidth(column.title) + rule.padding;
  const contentWidth = taskListFilteredRows.value.reduce((max, row) => {
    const rowAny = row as Record<string, any>;
    const getter = taskListColumnTextGetters[dataIndex];
    return Math.max(max, getTaskListTextVisualWidth(getter ? getter(rowAny) : rowAny[dataIndex]) + rule.padding);
  }, 0);
  return clampTaskListColumnWidth(Math.max(titleWidth, contentWidth), rule.min, rule.max);
}

const taskListColumns = computed(() =>
  taskListBaseColumns.map((column) => ({
    ...column,
    width: getTaskListColumnWidth(column),
  })),
);

const taskListScrollX = computed(() =>
  taskListColumns.value.reduce((total, column) => total + Number(column.width || 0), 0),
);

watch(
  () => [taskListFilters.planNo, taskListFilters.modelCode, taskListFilters.batchNo, taskListFilters.status],
  () => {
    taskListPage.value = 1;
  },
);

watch(
  () => [taskListFilteredRows.value.length, taskListPageSize.value],
  () => {
    const maxPage = Math.max(1, Math.ceil(taskListFilteredRows.value.length / taskListPageSize.value));
    if (taskListPage.value > maxPage) {
      taskListPage.value = maxPage;
    }
  },
);

const currentOperationStatus = computed(() => normalizeWorkOrderStatus(currentPlan.status));
const isWorkOrderRunning = computed(() => currentOperationStatus.value === 'RUNNING');
const isWorkOrderFinished = computed(() => currentOperationStatus.value === 'FINISHED');
const isWorkOrderPaused = computed(() => currentOperationStatus.value === 'PAUSED');
const isWorkOrderCancelled = computed(() => currentOperationStatus.value === 'CANCELLED');
const activeSourceGroup = computed(() => getActiveSourceGroup());
const currentCutRoundProductModelCode = computed(() => {
  const group = activeSourceGroup.value;
  return String(
    group?.segments.map((segment) => segment.modelCode).find(Boolean)
      || group?.modelCode
      || currentPlan.modelCode
      || '',
  ).trim();
});
const isCurrentSegmentFinished = computed(() => isSourceGroupCompleted(activeSourceGroup.value));
const isCurrentTaskReadonly = computed(
  () => isWorkOrderFinished.value || isCurrentSegmentFinished.value || isWorkOrderPaused.value || isWorkOrderCancelled.value,
);
function buildWorkOrderBlockedReason(status = currentPlan.status) {
  const normalized = normalizeWorkOrderStatus(status);
  const batchNo = currentPlan.sourceProductionBatchNo || currentPlan.batchNo || currentPlan.sourceBatchNo || '-';
  if (normalized === 'PAUSED') {
    return `当前裁切工序已暂停，计划 ${currentPlan.planNo || '-'}，批号 ${batchNo} 暂不能继续报工或提交操作，请等待生产计划复工指令。`;
  }
  if (normalized === 'CANCELLED') {
    return `当前裁切工序已作废取消，计划 ${currentPlan.planNo || '-'}，批号 ${batchNo} 不能继续报工或提交操作。`;
  }
  return '';
}

const workbenchBlockedReason = computed(() => buildWorkOrderBlockedReason());
const workbenchBlockedTitle = computed(() =>
  isWorkOrderCancelled.value ? '当前裁切工序已作废取消' : '当前裁切工序已暂停',
);
function isDailyRecordSaved(row: DailyRecordRow) {
  return row.status !== 'PENDING' && Boolean(normalizeDateTimeText(row.recorderTime));
}
function getDailyRecordIdentityText(row: DailyRecordRow) {
  return `${row.formCode || ''} ${row.name || ''} ${row.timing || ''}`;
}
function isDailyCleanRecord(row: DailyRecordRow) {
  const text = getDailyRecordIdentityText(row).toUpperCase();
  return text.includes('CLEAN') || text.includes('清洁');
}
function isDailyProcessCheckRecord(row: DailyRecordRow) {
  const text = getDailyRecordIdentityText(row).toUpperCase();
  return text.includes('START') || text.includes('CHECK') || text.includes('点检') || text.includes('开机') || text.includes('工艺');
}
function isDailyPreparationRowsReady(rows: DailyRecordRow[]) {
  const cleanRows = rows.filter(isDailyCleanRecord);
  const processRows = rows.filter(isDailyProcessCheckRecord);
  if (cleanRows.length || processRows.length) {
    return (!cleanRows.length || cleanRows.some(isDailyRecordSaved))
      && (!processRows.length || processRows.some(isDailyRecordSaved));
  }
  return rows.some(isDailyRecordSaved);
}
const dailyPreparationReady = computed(() => isDailyPreparationRowsReady(dailyRecordRows.value));

function normalizeEquipmentId(value?: number | string | null) {
  const id = Number(value);
  return Number.isFinite(id) && id > 0 ? id : undefined;
}

const CUT_ROUND_CONTEXT_KEYWORDS = ['CUT_ROUND', 'CUTROUND', 'CUT-ROUND', 'OP-CUT', 'OP-CUT-ROUND', '裁切', '裁圆', '切圆'];
const PACKAGING_CONTEXT_KEYWORDS = ['PACKAGING', 'PACKING', 'PACKAGE', '包装', '内包装', '外包装', '包装机'];

function buildEquipmentContextText(context: Record<string, any> = {}) {
  return [
    context.code,
    context.equipmentCode,
    context.equipmentName,
    context.name,
    context.operationCode,
    context.operationName,
    context.process,
    context.processCode,
    context.processName,
    context.workCenterCode,
    context.workCenterName,
  ]
    .filter((value) => value !== undefined && value !== null)
    .join(' ')
    .toUpperCase();
}

function containsAnyKeyword(text: string, keywords: string[]) {
  return keywords.some((keyword) => text.includes(keyword.toUpperCase()));
}

function isPackagingEquipmentContext(context: Record<string, any> = {}) {
  return containsAnyKeyword(buildEquipmentContextText(context), PACKAGING_CONTEXT_KEYWORDS);
}

function isCutRoundContext(context: Record<string, any> = {}) {
  return containsAnyKeyword(buildEquipmentContextText(context), CUT_ROUND_CONTEXT_KEYWORDS);
}

function isAllowedCutRoundEquipmentContext(context: Record<string, any> = {}) {
  return !isPackagingEquipmentContext(context);
}

function getCutRoundEquipmentCandidate<T extends Record<string, any>>(candidates: T[]) {
  return candidates.find((item) => isAllowedCutRoundEquipmentContext(item) && (item.equipmentId || item.id || item.equipmentCode || item.code || item.equipmentName || item.name));
}

function getCutRoundWorkCenterCandidate<T extends Record<string, any>>(candidates: T[]) {
  return candidates.find((item) => isAllowedCutRoundEquipmentContext(item) && (item.workCenterId || item.workCenterName || item.workCenterCode));
}

function filterCutRoundEquipmentRows(rows: MesHcEquipmentApi.Equipment[]) {
  return rows.filter((row) => isAllowedCutRoundEquipmentContext(row));
}

function hasBoardEquipmentValue() {
  return Boolean(boardEquipment.id || boardEquipment.code || boardEquipment.name);
}

const selectedBoardEquipmentId = computed(() => boardEquipment.id ?? currentPlan.equipmentId);
const selectedBoardEquipmentCode = computed(() => boardEquipment.code || currentPlan.equipmentCode || '');
const selectedBoardEquipmentName = computed(() => boardEquipment.name || currentPlan.equipmentName || '');
const selectedBoardApplicablePadType = computed(() => normalizeCutRoundPadTypeCode(boardEquipment.applicablePadType));
const selectedBoardWorkCenterName = computed(() => boardEquipment.workCenterName || currentPlan.workCenterName || '-');
const cutRoundTaskListTitle = computed(
  () => `裁切待加工列表 - ${selectedBoardEquipmentCode.value || '未绑定'} / ${selectedBoardEquipmentName.value || '-'}`,
);

function getTaskEquipment(task: Partial<MesHcCutRoundConsoleApi.TaskItem> | Record<string, any> = {}) {
  return {
    equipmentCode: String(task.equipmentCode || ''),
    equipmentId: normalizeEquipmentId(task.equipmentId),
    equipmentName: String(task.equipmentName || ''),
    workCenterId: normalizeEquipmentId(task.workCenterId),
    workCenterName: String(task.workCenterName || ''),
    workStatus: String(task.workStatus || ''),
  };
}

function getEffectiveBoardEquipment(context: Partial<MesHcCutRoundConsoleApi.TaskItem> | CurrentPlan | Record<string, any> = {}) {
  const taskEquipment = getTaskEquipment(context);
  const boardContext = {
    applicablePadType: boardEquipment.applicablePadType,
    applicablePadTypeName: boardEquipment.applicablePadTypeName,
    equipmentCode: boardEquipment.code,
    equipmentId: boardEquipment.id,
    equipmentName: boardEquipment.name,
    workCenterId: boardEquipment.workCenterId,
    workCenterName: boardEquipment.workCenterName,
    workStatus: boardEquipment.workStatus,
  };
  const currentContext = {
    equipmentCode: currentPlan.equipmentCode,
    equipmentId: currentPlan.equipmentId,
    equipmentName: currentPlan.equipmentName,
    workCenterId: currentPlan.workCenterId,
    workCenterName: currentPlan.workCenterName,
  };
  const equipmentSource = getCutRoundEquipmentCandidate([boardContext, taskEquipment, currentContext]) as Record<string, any> | undefined;
  const workCenterSource = getCutRoundWorkCenterCandidate([boardContext, taskEquipment, currentContext]) as Record<string, any> | undefined;
  return {
    applicablePadType: equipmentSource?.applicablePadType || '',
    applicablePadTypeName: equipmentSource?.applicablePadTypeName || '',
    equipmentCode: equipmentSource?.equipmentCode || '',
    equipmentId: normalizeEquipmentId(equipmentSource?.equipmentId),
    equipmentName: equipmentSource?.equipmentName || '',
    workCenterId: normalizeEquipmentId(workCenterSource?.workCenterId),
    workCenterName: workCenterSource?.workCenterName || '',
    workStatus: equipmentSource?.workStatus || '',
  };
}

function applyBoardEquipment(equipment: Partial<BoardEquipment>, options: { manual?: boolean } = {}) {
  if (!isAllowedCutRoundEquipmentContext(equipment as Record<string, any>)) {
    if (options.manual) {
      message.warning('包装设备不能切换到裁切操作看板，请选择裁切工作中心设备。');
    }
    return;
  }
  boardEquipment.id = normalizeEquipmentId(equipment.id);
  boardEquipment.applicablePadType = String(equipment.applicablePadType || '');
  boardEquipment.applicablePadTypeName = String(equipment.applicablePadTypeName || '');
  boardEquipment.code = String(equipment.code || '');
  boardEquipment.name = String(equipment.name || '');
  boardEquipment.workCenterId = normalizeEquipmentId(equipment.workCenterId);
  boardEquipment.workCenterName = String(equipment.workCenterName || '');
  boardEquipment.workStatus = String(equipment.workStatus || '');
  if (options.manual) {
    boardEquipmentManualSelected.value = true;
  }
}

function applyBoardEquipmentFromTask(task: Partial<MesHcCutRoundConsoleApi.TaskItem> | Record<string, any>, options: { force?: boolean } = {}) {
  if (!options.force && boardEquipmentManualSelected.value && hasBoardEquipmentValue()) return;
  if (!isAllowedCutRoundEquipmentContext(task)) return;
  const equipment = getTaskEquipment(task);
  if (!equipment.equipmentId && !equipment.equipmentCode && !equipment.equipmentName && !equipment.workCenterId && !equipment.workCenterName) return;
  applyBoardEquipment({
    applicablePadType: resolveTaskPadType(task),
    applicablePadTypeName: resolveTaskPadType(task) === 'BLACK_PAD' ? '黑垫' : '白垫',
    code: equipment.equipmentCode,
    id: equipment.equipmentId,
    name: equipment.equipmentName,
    workCenterId: equipment.workCenterId,
    workCenterName: equipment.workCenterName,
    workStatus: equipment.workStatus,
  });
}

function applyBoardEquipmentFromLedger(equipment: MesHcEquipmentApi.Equipment) {
  if (!isAllowedCutRoundEquipmentContext(equipment)) {
    message.warning('包装设备不能切换到裁切操作看板，请选择裁切工作中心设备。');
    return;
  }
  applyBoardEquipment(
    {
      code: equipment.equipmentCode || '',
      id: equipment.id,
      name: equipment.equipmentName || '',
      applicablePadType: equipment.applicablePadType || '',
      applicablePadTypeName: equipment.applicablePadTypeName || '',
      workCenterId: equipment.workCenterId,
      workCenterName: equipment.workCenterName || '',
      workStatus: equipment.workStatus || '',
    },
    { manual: true },
  );
}

function applyBoardEquipmentFromOption(equipment: any, options: { manual?: boolean } = {}) {
  applyBoardEquipment(
    {
      code: equipment?.code || '',
      id: equipment?.value,
      name: equipment?.name || '',
      applicablePadType: equipment?.applicablePadType || '',
      applicablePadTypeName: equipment?.applicablePadTypeName || '',
      workCenterId: equipment?.workCenterId,
      workCenterName: equipment?.workCenterName || '',
      workStatus: equipment?.workStatus || '',
    },
    options,
  );
}

function syncCurrentPlanEquipmentFromBoard(context: Partial<MesHcCutRoundConsoleApi.TaskItem> | Record<string, any> = {}) {
  const equipment = getEffectiveBoardEquipment(context);
  currentPlan.equipmentCode = equipment.equipmentCode;
  currentPlan.equipmentId = equipment.equipmentId;
  currentPlan.equipmentName = equipment.equipmentName;
  currentPlan.workCenterId = equipment.workCenterId ?? currentPlan.workCenterId;
  currentPlan.workCenterName = equipment.workCenterName || currentPlan.workCenterName;
}

function isTaskMatchedBoardEquipment(task: MesHcCutRoundConsoleApi.TaskItem | Record<string, any>) {
  const selectedId = selectedBoardEquipmentId.value;
  const selectedCode = String(selectedBoardEquipmentCode.value || '').trim();
  if (!selectedId && !selectedCode) return true;
  if (!isTaskPadTypeCompatibleWithBoard(task)) return false;
  const taskEquipment = getTaskEquipment(task);
  if (selectedId && taskEquipment.equipmentId) return selectedId === taskEquipment.equipmentId;
  if (selectedCode && taskEquipment.equipmentCode) return selectedCode === taskEquipment.equipmentCode;
  return !taskEquipment.equipmentId && !taskEquipment.equipmentCode;
}

function normalizeCutRoundPadTypeCode(value?: string) {
  return String(value || '').trim().toUpperCase();
}

function resolveTaskPadType(task: MesHcCutRoundConsoleApi.TaskItem | Record<string, any>) {
  const categoryCode = normalizeCutRoundPadTypeCode(task.categoryCode);
  if (['BLACK_PAD', 'WHITE_PAD'].includes(categoryCode)) return categoryCode;
  const modelCode = String(task.modelCode || '').trim().toUpperCase();
  if (modelCode.startsWith('HCR') || modelCode.includes('BLACK') || modelCode.includes('黑')) return 'BLACK_PAD';
  if (modelCode.startsWith('W') || modelCode.includes('WHITE') || modelCode.includes('SOFT') || modelCode.includes('白') || modelCode.includes('软')) return 'WHITE_PAD';
  return '';
}

function isTaskPadTypeCompatibleWithBoard(task: MesHcCutRoundConsoleApi.TaskItem | Record<string, any>) {
  const equipmentPadType = selectedBoardApplicablePadType.value;
  if (!equipmentPadType) return false;
  if (equipmentPadType === 'COMMON') return true;
  return equipmentPadType === resolveTaskPadType(task);
}

function isEquipmentPadTypeCompatibleWithTask(
  equipment: Record<string, any>,
  task: MesHcCutRoundConsoleApi.TaskItem | Record<string, any>,
) {
  const equipmentPadType = normalizeCutRoundPadTypeCode(equipment.applicablePadType);
  return equipmentPadType === 'COMMON' || equipmentPadType === resolveTaskPadType(task);
}

function ensureBoardEquipmentFromTasks(rows: MesHcCutRoundConsoleApi.TaskItem[]) {
  if (hasBoardEquipmentValue()) return;
  const cutRoundRows = rows.filter((row) => isAllowedCutRoundEquipmentContext(row));
  const candidates = cutRoundRows.filter((row) => row.equipmentId || row.equipmentCode || row.equipmentName);
  const candidate =
    candidates.find((row) => normalizeWorkOrderStatus(row.status) === 'RUNNING') ||
    candidates.find((row) => normalizeWorkOrderStatus(row.status) === 'PENDING') ||
    candidates[0];
  if (candidate) {
    applyBoardEquipmentFromTask(candidate, { force: true });
    return;
  }
  const workCenterCandidate =
    cutRoundRows.find((row) => normalizeWorkOrderStatus(row.status) === 'RUNNING' && (row.workCenterId || row.workCenterName)) ||
    cutRoundRows.find((row) => normalizeWorkOrderStatus(row.status) === 'PENDING' && (row.workCenterId || row.workCenterName)) ||
    cutRoundRows.find((row) => row.workCenterId || row.workCenterName);
  if (workCenterCandidate) {
    boardEquipment.workCenterId = normalizeEquipmentId(workCenterCandidate.workCenterId);
    boardEquipment.workCenterName = String(workCenterCandidate.workCenterName || '');
  }
}

async function ensureDefaultBoardEquipment() {
  if (!hasBoardEquipmentValue()) {
    await loadEquipmentOptions(boardEquipment.workCenterId || currentPlan.workCenterId);
    const target = equipmentOptions.value[0];
    if (target?.value) {
      applyBoardEquipmentFromOption(target);
    }
  }
  syncCurrentPlanEquipmentFromBoard();
}

function getDailyRecordContext() {
  if (currentPlan.planId && currentPlan.planOperationId) return currentPlan;
  const availableTasks = taskRows.value.filter((row) => isAllowedCutRoundEquipmentContext(row));
  const matchedTask =
    availableTasks.find((row) => row.planId && row.planOperationId && isTaskMatchedBoardEquipment(row)) ||
    availableTasks.find((row) => row.planId && row.planOperationId);
  if (matchedTask && !hasBoardEquipmentValue()) {
    applyBoardEquipmentFromTask(matchedTask, { force: true });
    syncCurrentPlanEquipmentFromBoard(matchedTask);
  }
  return matchedTask;
}

const currentEquipmentLabel = computed(() => {
  if (selectedBoardEquipmentCode.value && selectedBoardEquipmentName.value) return `${selectedBoardEquipmentCode.value} / ${selectedBoardEquipmentName.value}`;
  return selectedBoardEquipmentCode.value || selectedBoardEquipmentName.value || '未绑定';
});

function getEquipmentWorkStatusMeta(status?: string) {
  const normalized = String(status || '').toUpperCase();
  if (['BUSY', 'IN_PROGRESS', 'OCCUPIED', 'PRODUCING', 'RUNNING', 'WORKING'].includes(normalized)) {
    return { color: 'processing', text: '生产中' };
  }
  if (['IDLE', 'FREE', 'AVAILABLE'].includes(normalized)) {
    return { color: 'green', text: '空闲' };
  }
  if (['MAINTAIN', 'MAINTENANCE', 'REPAIR'].includes(normalized)) {
    return { color: 'orange', text: '维护中' };
  }
  if (['DISABLED', 'STOPPED'].includes(normalized)) {
    return { color: 'red', text: '停用' };
  }
  return { color: 'default', text: '待确认' };
}

async function loadEquipmentOptions(workCenterId = currentPlan.workCenterId || boardEquipment.workCenterId) {
  const workCenter = getCutRoundWorkCenterCandidate([
    { workCenterId, workCenterName: currentPlan.workCenterName },
    { workCenterId: boardEquipment.workCenterId, workCenterName: boardEquipment.workCenterName },
    { workCenterId: currentPlan.workCenterId, workCenterName: currentPlan.workCenterName },
  ]);
  let list: MesHcEquipmentApi.Equipment[] = [];
  if (workCenter?.workCenterId || workCenter?.workCenterName) {
    const primaryPage = await getEquipmentPage({
      pageNo: 1,
      pageSize: 200,
      status: 0,
      ...(workCenter.workCenterId ? { workCenterId: workCenter.workCenterId } : { workCenterName: workCenter.workCenterName }),
    } as any);
    list = filterCutRoundEquipmentRows(primaryPage?.list || []);
  }
  equipmentOptions.value = list.map((item: any) => ({
    applicablePadType: item.applicablePadType,
    applicablePadTypeName: item.applicablePadTypeName,
    code: item.equipmentCode,
    label: item.equipmentCode && item.equipmentName ? `${item.equipmentCode} / ${item.equipmentName}` : item.equipmentCode || item.equipmentName,
    name: item.equipmentName,
    value: item.id,
    workCenterId: item.workCenterId,
    workCenterName: item.workCenterName,
    workStatus: item.workStatus,
  }));
}

function applyEquipmentToTask(task: MesHcCutRoundConsoleApi.TaskItem, equipment: any) {
  task.equipmentId = equipment?.value;
  task.equipmentCode = equipment?.code || '';
  task.equipmentName = equipment?.name || '';
}

async function ensureTaskEquipment(task: MesHcCutRoundConsoleApi.TaskItem): Promise<boolean | null> {
  const originalEquipmentId = task.equipmentId;
  if (hasBoardEquipmentValue()) {
    if (!isTaskPadTypeCompatibleWithBoard(task)) {
      const taskPadTypeCode = resolveTaskPadType(task);
      const taskPadType = taskPadTypeCode === 'BLACK_PAD' ? '黑垫' : taskPadTypeCode === 'WHITE_PAD' ? '白垫' : '未配置垫型';
      message.warning(`计划 ${task.planNo || '-'} 为${taskPadType}，与当前裁切设备适用垫型不一致`);
      return null;
    }
    if (!isTaskMatchedBoardEquipment(task) && (task.equipmentId || task.equipmentCode)) {
      message.warning(`计划 ${task.planNo || '-'} 已绑定其他裁切设备，不能在当前设备看板直接加载`);
      return null;
    }
    const equipment = getEffectiveBoardEquipment(task);
    task.equipmentId = equipment.equipmentId;
    task.equipmentCode = equipment.equipmentCode;
    task.equipmentName = equipment.equipmentName;
    return Boolean(task.equipmentId && task.equipmentId !== originalEquipmentId);
  }
  if (task.equipmentId || task.equipmentCode || task.equipmentName) {
    applyBoardEquipmentFromTask(task, { force: true });
    return false;
  }
  await loadEquipmentOptions(task.workCenterId);
  const target = equipmentOptions.value.find((item) => isEquipmentPadTypeCompatibleWithTask(item, task));
  if (!target?.value) {
    message.warning(`计划 ${task.planNo || '-'} 未找到垫型匹配的可用裁切设备`);
    return null;
  }
  applyBoardEquipmentFromOption(target);
  applyEquipmentToTask(task, target);
  return true;
}

async function loadEquipmentSelectRows() {
  equipmentSelectLoading.value = true;
  try {
    const keyword = equipmentSelectKeyword.value.trim();
    const initialWorkCenter = getCutRoundWorkCenterCandidate([
      {
        workCenterId: currentPlan.workCenterId,
        workCenterName: currentPlan.workCenterName,
      },
      {
        workCenterId: boardEquipment.workCenterId,
        workCenterName: boardEquipment.workCenterName,
      },
    ]);
    if (!initialWorkCenter && taskRows.value.length === 0) {
      await loadTaskList();
    }
    const effectiveWorkCenter = getCutRoundWorkCenterCandidate([
      {
        workCenterId: currentPlan.workCenterId,
        workCenterName: currentPlan.workCenterName,
      },
      {
        workCenterId: boardEquipment.workCenterId,
        workCenterName: boardEquipment.workCenterName,
      },
      ...taskRows.value.filter((row) => isCutRoundContext(row) || isAllowedCutRoundEquipmentContext(row)),
    ]);
    const effectiveWorkCenterId = normalizeEquipmentId(effectiveWorkCenter?.workCenterId);
    const effectiveWorkCenterName = String(effectiveWorkCenter?.workCenterName || '');
    if (!effectiveWorkCenterId && !effectiveWorkCenterName) {
      equipmentSelectRows.value = [];
      message.warning('当前裁切任务未带出工作中心，不能展示设备；请先维护裁切工序工作中心或机台。');
      return;
    }
    const baseParams: Record<string, any> = {
      pageNo: 1,
      pageSize: 200,
      status: 0,
      ...(effectiveWorkCenterId ? { workCenterId: effectiveWorkCenterId } : { workCenterName: effectiveWorkCenterName }),
    };
    let rows: MesHcEquipmentApi.Equipment[] = [];
    if (keyword) {
      rows = normalizeRows(await getEquipmentPage({ ...baseParams, equipmentCode: keyword }));
      if (!rows.length) {
        rows = normalizeRows(await getEquipmentPage({ ...baseParams, equipmentName: keyword }));
      }
    } else {
      rows = normalizeRows(await getEquipmentPage(baseParams));
    }
    rows = filterCutRoundEquipmentRows(rows);
    if (!rows.length) {
      message.warning('当前裁切工作中心下未找到可用设备，请检查设备台账的工作中心和启用状态。');
    }
    equipmentSelectRows.value = rows;
  } finally {
    equipmentSelectLoading.value = false;
  }
}

async function openEquipmentSelect() {
  equipmentSelectVisible.value = true;
  if (
    !currentPlan.workCenterId &&
    !currentPlan.workCenterName &&
    !boardEquipment.workCenterId &&
    !boardEquipment.workCenterName &&
    taskRows.value.length === 0
  ) {
    await loadTaskList();
  }
  await loadEquipmentSelectRows();
}

function closeEquipmentSelect() {
  equipmentSelectVisible.value = false;
  openDailyRecordAfterEquipmentSelected.value = false;
}

async function selectBoardEquipment(equipment: MesHcEquipmentApi.Equipment) {
  if (!isAllowedCutRoundEquipmentContext(equipment)) {
    message.warning('包装设备不能切换到裁切操作看板，请选择裁切工作中心设备。');
    return;
  }
  applyBoardEquipmentFromLedger(equipment);
  resetPlan();
  await loadTaskList();
  await loadDailyRecords();
  equipmentSelectVisible.value = false;
  // message.success(`已切换裁切设备：${boardEquipment.code || boardEquipment.name || '-'}`);
  if (openDailyRecordAfterEquipmentSelected.value) {
    openDailyRecordAfterEquipmentSelected.value = false;
    dailyRecordListVisible.value = true;
  }
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

function stripSegmentMark(batchNo?: string) {
  const source = String(batchNo || '').trim();
  if (!source) return '';
  const withoutSuffix = source.replace(/-(?:J|S)\d+$/i, '');
  const sliceMatched = withoutSuffix.match(/^(.+[PQRS])\d{3}[A-Z]?$/i);
  if (sliceMatched?.[1]) return sliceMatched[1];
  return /[PQRS]$/.test(withoutSuffix) ? withoutSuffix : withoutSuffix;
}

function isSegmentBatchNo(batchNo?: string) {
  return /[PQRS]$/i.test(String(batchNo || '').trim());
}

function resolveMotherBatchNo(row: Record<string, any>) {
  const directCandidates = [row.sourceBatchNo, row.parentProductionBatchNo, row.motherBatchNo]
    .map((value) => stripSegmentMark(value))
    .filter(Boolean);
  const directSegment = directCandidates.find(isSegmentBatchNo);
  if (directSegment) return directSegment;
  const derivedSegment = stripSegmentMark(row.sourceProductionBatchNo || row.productionBatchNo || row.batchNo);
  if (derivedSegment) return derivedSegment;
  return directCandidates[0] || '';
}

function isSameText(left?: unknown, right?: unknown) {
  const leftText = String(left ?? '').trim();
  const rightText = String(right ?? '').trim();
  return !!leftText && !!rightText && leftText.toUpperCase() === rightText.toUpperCase();
}

function resolveCutRoundSourceMotherBatchNo(
  source: MesHcCutRoundConsoleApi.SourceItem | Record<string, any> | undefined,
  fallbackSliceBatchNo?: string,
) {
  if (!source) return stripSegmentMark(fallbackSliceBatchNo);
  const record = source as Record<string, any>;
  return resolveMotherBatchNo({
    motherBatchNo: record.motherBatchNo,
    parentProductionBatchNo: record.parentProductionBatchNo,
    productionBatchNo: record.productionBatchNo || record.confirmedBatchNo || fallbackSliceBatchNo,
    sourceBatchNo: record.sourceBatchNo,
    sourceProductionBatchNo: record.sourceProductionBatchNo || record.productionBatchNo || fallbackSliceBatchNo,
  });
}

function matchesTaskMotherBatch(row: Record<string, any>, motherBatchNo?: string) {
  const target = String(motherBatchNo || '').trim();
  if (!target) return true;
  const taskMotherBatchNo = resolveMotherBatchNo(row);
  if (isSameText(taskMotherBatchNo, target)) return true;
  return [
    row.sourceBatchNo,
    row.sourceMotherBatchNo,
    row.parentProductionBatchNo,
    row.motherBatchNo,
    row.sourceProductionBatchNo,
    row.productionBatchNo,
    row.batchNo,
  ].some((value) => isSameText(stripSegmentMark(value), target));
}

function findTaskByPlanAndMother(
  rows: Array<MesHcCutRoundConsoleApi.TaskItem | Record<string, any>>,
  planNo: string,
  motherBatchNo?: string,
) {
  const planRows = rows.filter((row) => !planNo || isSameText(row.planNo, planNo));
  if (planNo && !planRows.length) return undefined;
  if (motherBatchNo) {
    const matched = planRows.find((row) => matchesTaskMotherBatch(row, motherBatchNo));
    if (matched) return matched;
    return undefined;
  }
  return planRows[0] || rows[0];
}

function getSegmentMark(batchNo?: string) {
  const source = String(batchNo || '').trim();
  if (!source) return '';
  const mark = source.slice(-1);
  return ['P', 'Q', 'R', 'S'].includes(mark) ? mark : '';
}

function isGlueBoardMaterialItem(itemCategory: string, itemName: string) {
  return itemCategory.trim() === '胶板' && itemName.trim().includes('胶板料号');
}

function isGlueBoardBatchItem(itemCategory: string, itemName: string) {
  return itemCategory.trim() === '胶板' && (itemName.trim().includes('胶板批号') || itemName.trim().includes('胶板批次'));
}

function getDefaultCheckActualValue(item: Pick<CutRoundCheckItem, 'itemCategory' | 'itemName'>) {
  const itemCategory = item.itemCategory || '';
  const itemName = item.itemName || '';
  if (isGlueBoardMaterialItem(itemCategory, itemName)) {
    return glueBoard.materialCode || ADHESIVE_DEFAULT_GLUE_BOARD_MATERIAL_CODE;
  }
  if (isGlueBoardBatchItem(itemCategory, itemName)) {
    return glueBoard.batchNo || '';
  }
  return '';
}

function applyGlueBoardDefaultsToCheckItems() {
  checkTemplate.value = checkTemplate.value.map((item) => {
    if (isGlueBoardMaterialItem(item.itemCategory || '', item.itemName || '')) {
      return { ...item, actualValue: glueBoard.materialCode || ADHESIVE_DEFAULT_GLUE_BOARD_MATERIAL_CODE };
    }
    if (isGlueBoardBatchItem(item.itemCategory || '', item.itemName || '')) {
      return { ...item, actualValue: glueBoard.batchNo || '' };
    }
    return item;
  });
}

function resolveCutRoundTemplateModelCode() {
  const candidates = [
    currentCutRoundProductModelCode.value,
    reportForm.glueBoardModel,
  ]
    .map((item) => String(item || '').trim().toUpperCase())
    .filter(Boolean);
  return candidates[0] || 'W26P0100';
}

function getCutRoundProcessCheckItems() {
  return checkTemplate.value
    .filter((item) => !['环境'].includes(item.itemCategory || ''))
    .map((item, index) => ({
      ...item,
      itemCategory: '工艺参数',
      sortNo: Number(item.sortNo || 100 + index),
      standardValue: item.standardValue || '/',
    }));
}

function ensureCutRoundChangeoverItems(items: CutRoundCheckItem[]) {
  const rows = items.map((item) => ({ ...item, itemCategory: item.itemCategory || '其他' }));
  if (!rows.some((item) => item.itemName.includes('湿度'))) {
    rows.splice(1, 0, {
      abnormalRemark: '',
      actualValue: '',
      checkResult: 'OK',
      itemCategory: '环境',
      itemName: '湿度',
      sortNo: 2,
      standardValue: '55.0±10.0%RH',
    });
  }
  return rows.map((item, index) => ({ ...item, sortNo: item.sortNo || index + 1 }));
}

function normalizeCheckItem(item: any, index = 0): CutRoundCheckItem {
  const itemCategory = item?.itemCategory || item?.category || '其他';
  const itemName = item?.itemName || item?.item || '';
  const actualValue = item?.actualValue ?? item?.value ?? '';
  return {
    abnormalRemark: item?.abnormalRemark || item?.remark || '',
    actualValue: String(actualValue).trim() || getDefaultCheckActualValue({ itemCategory, itemName }),
    checkResult: item?.checkResult || item?.result || 'OK',
    itemCategory,
    itemName,
    sortNo: Number(item?.sortNo || item?.itemSeq || index + 1),
    standardValue: item?.standardValue || item?.standard || '',
  };
}

function mapPassWorkRecord(record: any, index: number): DailyRecordRow {
  const status = String(record?.status || '').toUpperCase();
  const normalizedStatus =
    status === 'CONFIRMED' || status === 'COMPLETED'
      ? 'COMPLETED'
      : status === 'RECORDED' || status === 'FILLED' || status === 'WAITING'
        ? 'FILLED'
        : 'PENDING';
  const details = Array.isArray(record?.details) && record.details.length > 0
    ? record.details
    : Array.isArray(record?.presetDetails)
      ? record.presetDetails
      : [];
  return {
    canConfirm: record?.canConfirm ?? normalizedStatus === 'FILLED',
    canFill: record?.canFill ?? normalizedStatus !== 'COMPLETED',
    canView: record?.canView ?? true,
    confirmer: record?.confirmer || '-',
    confirmerTime: normalizeDateTimeText(record?.confirmerTime) || '-',
    details: details.map((item: any, detailIndex: number) => ({
      category: item.category || item.itemCategory || '',
      item: item.item || item.itemName || '',
      node: item.node || '',
      remark: item.remark || item.abnormalRemark || '',
      result: item.status || item.checkResult || 'OK',
      seq: Number(item.itemSeq || item.sortNo || detailIndex + 1),
      standard: item.standard || item.standardValue || '',
      value: item.actualValue ?? item.value ?? '',
    })),
    formCode: record?.formCode || `CUT_ROUND_DAILY_${index + 1}`,
    key: record?.formCode || record?.id || `cut-round-daily-${index + 1}`,
    name: record?.name || '裁切工作准备记录',
    recordId: record?.recordId,
    recorder: record?.recorder || '-',
    recorderTime: normalizeDateTimeText(record?.recorderTime) || '-',
    result: record?.result || record?.inspectionResult || '-',
    status: normalizedStatus,
    timing: record?.timing || '-',
  };
}

function createDailyRecordFallbackRows(): DailyRecordRow[] {
  return [
    {
      canConfirm: false,
      canFill: true,
      canView: true,
      confirmer: '-',
      confirmerTime: '-',
      details: [
        { category: '检查项目', item: '拖链槽', node: '开工前', remark: '', result: 'OK', seq: 1, standard: '运行正常,无异响', value: '' },
        { category: '检查项目', item: '机头安装螺丝', node: '开工前', remark: '', result: 'OK', seq: 2, standard: '螺丝无松动、无脱落现象', value: '' },
        { category: '检查项目', item: 'X、Y方向进行情况', node: '开工前', remark: '', result: 'OK', seq: 3, standard: '在机器切割之前空跑无异响', value: '' },
        { category: '检查项目', item: '机头和刀头', node: '开工前', remark: '', result: 'OK', seq: 4, standard: '未工作前启动并运转正确', value: '' },
        { category: '检查项目', item: '毛刷头', node: '开工前', remark: '', result: 'OK', seq: 5, standard: '毛刷头无变形破损、刷毛整齐均匀', value: '' },
        { category: '检查项目', item: '总气压值', node: '开工前', remark: '', result: 'OK', seq: 6, standard: '0.5-0.7MPa', value: '' },
      ],
      formCode: 'CUT_ROUND_STARTUP_CHECK',
      key: 'CUT_ROUND_STARTUP_CHECK',
      name: 'CMP软垫裁切开机点检表',
      recorder: '-',
      recorderTime: '-',
      result: '未填写',
      status: 'PENDING',
      timing: '开工前',
    },
    {
      canConfirm: false,
      canFill: true,
      canView: true,
      confirmer: '-',
      confirmerTime: '-',
      details: [
        { category: '检验项目', item: '刀头和刀片', node: '开工前', remark: '', result: 'OK', seq: 1, standard: '目视表面洁净，无破损且用无尘布清洁无脏污', value: '' },
        { category: '检验项目', item: '龙门架', node: '开工前', remark: '', result: 'OK', seq: 2, standard: '表面无脏污，无杂物', value: '' },
        { category: '检验项目', item: '毛毡', node: '开工前', remark: '', result: 'OK', seq: 3, standard: '目视表面洁净，无破损且用滚筒清洁无脏污', value: '' },
        { category: '检验项目', item: '控制台', node: '开工前', remark: '', result: 'OK', seq: 4, standard: '表面无脏污，无异物', value: '' },
        { category: '检验项目', item: '拖链槽', node: '开工前', remark: '', result: 'OK', seq: 5, standard: '表面无杂物，无异响', value: '' },
        { category: '检验项目', item: 'X、Y导轨', node: '开工前', remark: '', result: 'OK', seq: 6, standard: '表面无脏污，无异物', value: '' },
        { category: '检验项目', item: '气压阀积水', node: '开工前', remark: '', result: 'OK', seq: 7, standard: '气压阀内无积水', value: '' },
      ],
      formCode: 'CUT_ROUND_CLEANING_CHECK',
      key: 'CUT_ROUND_CLEANING_CHECK',
      name: 'CMP软垫裁切设备清洁点检表',
      recorder: '-',
      recorderTime: '-',
      result: '未填写',
      status: 'PENDING',
      timing: '开工前',
    },
  ];
}

function resetPlan() {
  const equipment = getEffectiveBoardEquipment();
  Object.assign(currentPlan, {
    actualSizeRule: '',
    availableSourceLength: 0,
    batchNo: '',
    endTime: '',
    equipmentCode: equipment.equipmentCode,
    equipmentId: equipment.equipmentId,
    equipmentName: equipment.equipmentName,
    materialCode: '',
    modelCode: '',
    planId: undefined,
    planNo: '',
    planOperationId: undefined,
    planSizeSpec: '',
    requirements: '',
    sourceBatchNo: '',
    sourceProductionBatchNo: '',
    startTime: '',
    status: '',
    workCenterId: equipment.workCenterId,
    workCenterName: equipment.workCenterName,
  });
  reportRecords.value = [];
  selectedReportRecordKeys.value = [];
  sourceGroups.value = [];
  changeoverInspections.value = [];
  latestChangeoverInspection.value = null;
  Object.assign(glueBoard, {
    alarm: '',
    aqcSampleLength: 0,
    availableStartPosition: 0,
    batchNo: '',
    id: undefined,
    latestAqcTask: undefined,
    lossLength: 0,
    materialCode: '',
    qualityLockReason: '',
    qualityLockStartPosition: undefined,
    qualityStatus: '',
    receiveLength: 0,
    receiveStartPosition: 0,
    stockLength: 0,
    todayUsedLength: 0,
  });
}

function normalizeSizeRule(value?: string) {
  const text = String(value || '').trim();
  if (text.includes('740') || text.toUpperCase() === 'B') return '740mm';
  if (text.includes('775') || text.toUpperCase() === 'A') return '775mm';
  return '';
}

function resolvePlanSizeSpec(task: Record<string, any>) {
  return task.spec || task.sizeSpec || task.sizeName || task.planSizeSpec || task.sizeRuleName || '';
}

function resolveActualSizeRule(task: Record<string, any>) {
  return normalizeSizeRule(task.actualSizeRule || task.actualSizeSpec || task.actualSizeName);
}

function getCutRoundSizeSuffix(sizeRule?: string) {
  const normalized = normalizeSizeRule(sizeRule);
  if (!normalized) return '';
  return normalized === '740mm' ? 'B' : 'A';
}

function stripCutRoundSizeSuffix(batchNo?: string) {
  const base = String(batchNo || '')
    .trim()
    .replace(/-J\d+$/i, '')
    .replace(/-S\d+$/i, '')
    .toUpperCase();
  return base.replace(/([PQRS]\d{3})[AB]$/i, '$1');
}

function getCutRoundCardSerialNo(batchNo?: string) {
  const text = stripCutRoundSizeSuffix(batchNo);
  if (!text) return Number.MAX_SAFE_INTEGER;
  const normalized = /[AB]$/i.test(text) ? text.slice(0, -1) : text;
  const sortText = normalized.slice(-3);
  return /^\d+$/.test(sortText) ? Number(sortText) : Number.MAX_SAFE_INTEGER;
}

function compareCutRoundCardNo(left?: string, right?: string) {
  const serialDiff = getCutRoundCardSerialNo(left) - getCutRoundCardSerialNo(right);
  if (serialDiff !== 0) return serialDiff;
  return String(left || '').localeCompare(String(right || ''), 'zh-Hans-CN', { numeric: true });
}

function resolveCutRoundExistingSizeSuffix(batchNo?: string) {
  const normalized = String(batchNo || '')
    .trim()
    .replace(/-J\d+$/i, '')
    .replace(/-S\d+$/i, '')
    .toUpperCase();
  return normalized.match(/[PQRS]\d{3}([AB])$/i)?.[1]?.toUpperCase() || '';
}

function resolveCutRoundSizeRuleFromSuffix(suffix?: string) {
  const normalized = String(suffix || '').trim().toUpperCase();
  if (normalized === 'A') return '775mm';
  if (normalized === 'B') return '740mm';
  return '';
}

function resolveCutRoundSizeRuleFromBatchNo(batchNo?: string) {
  const suffix = resolveCutRoundExistingSizeSuffix(batchNo);
  return resolveCutRoundSizeRuleFromSuffix(suffix);
}

function normalizeCutRoundProductionBatchNo(batchNo?: string, sizeRule?: string) {
  const existingSizeSuffix = resolveCutRoundExistingSizeSuffix(batchNo);
  const base = stripCutRoundSizeSuffix(batchNo);
  if (!base) return '';
  const sizeSuffix = getCutRoundSizeSuffix(sizeRule);
  if (/^[A-Z]\d{2}[A-Z]\d{3}[A-Z][PQRS]\d{3}$/.test(base)) {
    return `${base}${sizeSuffix || existingSizeSuffix}`;
  }
  return base;
}

function getReportActualSizeRule(record?: MesHcCutRoundConsoleApi.ReportItem | Record<string, any>) {
  const extra = parseRecordExtra(record || {}) as Record<string, any>;
  return normalizeSizeRule((record as Record<string, any> | undefined)?.actualSizeRule || extra.actualSizeRule || extra.actualSizeSpec || '');
}

function syncReportProductionBatchNoByActualSize() {
  reportForm.actualSizeRule = normalizeSizeRule(reportForm.actualSizeRule);
  reportForm.productionBatchNo = normalizeCutRoundProductionBatchNo(
    reportForm.productionBatchNo || reportForm.sourceProductionBatchNo || reportForm.sourceCode,
    reportForm.actualSizeRule,
  );
}

function applyTask(task: any) {
  if (!hasBoardEquipmentValue()) {
    applyBoardEquipmentFromTask(task, { force: true });
  }
  const equipment = getEffectiveBoardEquipment(task);
  const sourceProductionBatchNo = task.sourceProductionBatchNo || task.productionBatchNo || task.batchNo || '';
  const motherBatchNo = resolveMotherBatchNo({
    ...task,
    sourceBatchNo: task.sourceBatchNo,
    sourceProductionBatchNo,
  });
  const planSizeSpec = resolvePlanSizeSpec(task);
  currentPlan.availableSourceLength = Number(task.availableSourceLength || 0);
  currentPlan.actualSizeRule = resolveActualSizeRule(task);
  currentPlan.batchNo = motherBatchNo;
  currentPlan.endTime = task.endTime || '';
  currentPlan.equipmentCode = equipment.equipmentCode;
  currentPlan.equipmentId = equipment.equipmentId;
  currentPlan.equipmentName = equipment.equipmentName;
  currentPlan.materialCode = task.materialCode || '';
  currentPlan.modelCode = task.modelCode || '';
  currentPlan.planId = task.planId;
  currentPlan.planNo = task.planNo || '';
  currentPlan.planOperationId = task.planOperationId;
  currentPlan.planSizeSpec = planSizeSpec;
  currentPlan.requirements = task.requirements || task.executeRequirement || '';
  currentPlan.sourceBatchNo = motherBatchNo;
  currentPlan.sourceProductionBatchNo = sourceProductionBatchNo;
  currentPlan.startTime = task.startTime || '';
  currentPlan.status = task.status || '';
  currentPlan.workCenterId = equipment.workCenterId ?? task.workCenterId;
  currentPlan.workCenterName = equipment.workCenterName || task.workCenterName || '';
  sourceGroups.value = [];
}

function getSegmentLabel(segmentMark?: string) {
  return segmentMark ? `${segmentMark}段` : '不分段';
}

function buildSourceGroupsFromSources(sources: MesHcCutRoundConsoleApi.SourceItem[]) {
  const groups = new Map<string, SourceGroup>();
  sources.forEach((source) => {
    if (isPressSlotProcessCheckSource(source)) return;
    const batchNo = source.productionBatchNo || source.confirmedBatchNo || '';
    if (!batchNo) return;
    const baseBatchNo = resolveMotherBatchNo({
      parentProductionBatchNo: source.parentProductionBatchNo,
      productionBatchNo: batchNo,
      sourceBatchNo: source.motherBatchNo || source.sourceBatchNo,
      sourceProductionBatchNo: source.sourceProductionBatchNo,
    });
    if (!baseBatchNo) return;
    const outputLength = Number(source.outputLength ?? source.processLength ?? 0);
    const reportRanges = getReportRanges(batchNo, source.grindingSecondDetailId);
    const usedLength = getReportUsedLength(batchNo, source.grindingSecondDetailId);
    const sourceNgText = getSourceProcessNgText(source as unknown as Record<string, any>, '粘胶2');
    const availableLength = getAvailableLengthByRanges(outputLength, reportRanges);
    const segment: SourceSegment = {
      actualSizeRule: normalizeSizeRule(source.actualSizeRule),
      actualSizeSuffix: source.actualSizeSuffix || '',
      availableLength,
      batchNo,
      coaFlag: source.coaFlag,
      confirmStatus: source.confirmStatus || '',
      defectCode: source.defectCode || '',
      extraJson: source.extraJson || '',
      grindingSecondDetailId: source.grindingSecondDetailId,
      label: getSegmentLabel(source.segmentMark),
      modelCode: String((source as Record<string, any>).modelCode || (source as Record<string, any>).productionModelCode || (source as Record<string, any>).productModelCode || '').trim(),
      outputLength,
      qtime: source.qtime,
      reportRanges,
      segmentMark: source.segmentMark || '',
      selfCheck: source.selfCheck || '',
      sourceNgProcessName: sourceNgText ? resolveNgProcessName(source as unknown as Record<string, any>, '粘胶2') : '',
      sourceNgReason: sourceNgText ? getNgReasonText(source as unknown as Record<string, any>) : '',
      sourceNgText,
      usedLength,
    };
    const existed = groups.get(baseBatchNo);
    if (existed) {
      existed.segments.push(segment);
      existed.qtime = mergeCutRoundBatchQtime(existed.qtime, segment.qtime);
      existed.totalOutputLength += outputLength;
      existed.totalUsedLength += usedLength;
      existed.totalAvailableLength += segment.availableLength;
      return;
    }
    groups.set(baseBatchNo, {
      baseBatchNo,
      materialCode: currentPlan.materialCode,
      modelCode: segment.modelCode || currentPlan.modelCode,
      planNo: currentPlan.planNo,
      qtime: segment.qtime,
      segments: [segment],
      totalAvailableLength: segment.availableLength,
      totalOutputLength: outputLength,
      totalUsedLength: usedLength,
    });
  });
  sourceGroups.value = Array.from(groups.values()).map((group) => ({
    ...group,
    segments: group.segments.sort((a, b) => compareCutRoundCardNo(a.batchNo, b.batchNo)),
  }));
}

function getReportUsedLength(sourceProductionBatchNo: string, sourceGrindingSecondDetailId?: number) {
  return reportRecords.value
    .filter((item) => isCutRoundReportForSource(item, sourceProductionBatchNo, sourceGrindingSecondDetailId))
    .reduce((sum, item) => sum + Number(item.inputLength || item.outputLength || 0), 0);
}

async function loadCheckTemplate() {
  const rows = normalizeRows(await getCutRoundConsoleCheckTemplate(resolveCutRoundTemplateModelCode()));
  checkTemplate.value = rows.map(normalizeCheckItem);
}

async function loadDailyRecords() {
  const fallbackRows = createDailyRecordFallbackRows();
  const equipmentId = selectedBoardEquipmentId.value;
  if (!equipmentId) {
    dailyRecordRows.value = fallbackRows.map((row) => ({
      ...row,
      canConfirm: false,
      canFill: false,
      canView: false,
    }));
    return;
  }
  const context = getDailyRecordContext();
  if (!context?.planId || !context?.planOperationId) {
    dailyRecordRows.value = dailyRecordRows.value.length > 0 ? dailyRecordRows.value : fallbackRows;
    return;
  }
  const recordDate = dayjs().format('YYYY-MM-DD');
  const rows = normalizeRows(await getCutRoundConsolePassWorkList(
    context.planId,
    context.planOperationId,
    {
      equipmentId,
      recordDate,
    },
  ));
  const backendRows = rows
    .map(mapPassWorkRecord)
    .filter((item) => isDailyCleanRecord(item) || isDailyProcessCheckRecord(item))
    .sort((a, b) => Number(!isDailyCleanRecord(a)) - Number(!isDailyCleanRecord(b)));
  dailyRecordRows.value = backendRows.length > 0 ? backendRows : fallbackRows;
}

async function loadReports() {
  if (!currentPlan.planOperationId) return;
  const rows = normalizeRows(await getCutRoundConsoleReportList(currentPlan.planOperationId, {
    scanConfirmDate: reportScanConfirmDate.value || undefined,
  }));
  reportRecords.value = rows;
  syncIntermediateCountersFromReports();
  selectedReportRecordKeys.value = selectedReportRecordKeys.value.filter((key) =>
    rows.some((record: any) => record.id === key),
  );
}

function formatReportScanConfirmDate(record?: Partial<MesHcCutRoundConsoleApi.ReportItem> | Record<string, any>) {
  const value = String((record as Record<string, any> | undefined)?.confirmerTime || '');
  if (!value) return '-';
  const parsed = dayjs(value);
  if (parsed.isValid()) return parsed.format('YYYY-MM-DD');
  return value.length >= 10 ? value.slice(0, 10) : value;
}

async function loadChangeoverInspections() {
  if (!currentPlan.planOperationId) {
    changeoverInspections.value = [];
    latestChangeoverInspection.value = null;
    return;
  }
  const motherBatchNo = currentSegmentBatchNo.value || currentPlan.sourceBatchNo || currentPlan.batchNo || '';
  const rows = normalizeRows(await getCutRoundChangeoverList(currentPlan.planOperationId, motherBatchNo))
    .map((row) => normalizeChangeoverInspection(row))
    .filter(Boolean) as MesHcCutRoundConsoleApi.ChangeoverInspection[];
  changeoverInspections.value = rows;
  const latest = normalizeChangeoverInspection(await getCutRoundChangeoverLatest(currentPlan.planOperationId));
  latestChangeoverInspection.value =
    (latest && (!motherBatchNo || getLatestChangeoverMotherBatchNo(latest) === motherBatchNo) ? latest : null)
    || rows[0]
    || latest
    || null;
}

async function loadInspectionTasks() {
  if (!currentPlan.planOperationId) {
    inspectionTasks.value = [];
    selectedInspectionTask.value = null;
    return;
  }
  inspectionTasks.value = normalizeRows(await getCutRoundInspectionTasks(currentPlan.planOperationId));
  if (selectedInspectionTask.value?.id) {
    selectedInspectionTask.value = inspectionTasks.value.find((task) => task.id === selectedInspectionTask.value?.id) || selectedInspectionTask.value;
  }
}

async function refreshInspectionTaskStatus(options: { silent?: boolean } = {}) {
  if (!currentPlan.planOperationId) {
    if (!options.silent) {
      message.warning('请先扫码或选择裁切工单');
    }
    return false;
  }
  if (inspectionTaskRefreshInFlight) return false;
  inspectionTaskRefreshInFlight = true;
  try {
    await loadInspectionTasks();
    if (!options.silent) {
      // message.success('检测任务状态已刷新');
    }
    return true;
  } catch (error) {
    if (!options.silent) {
      const data = (error as any)?.response?.data || (error as any)?.data || {};
      const errorMessage = String(data.msg || data.message || (error as any)?.message || '');
      message.warning(errorMessage || '检测任务状态刷新失败，请稍后重试');
    }
    return false;
  } finally {
    inspectionTaskRefreshInFlight = false;
  }
}

function openInspectionTaskDetail(record: MesHcCutRoundConsoleApi.CutRoundInspectionTask) {
  selectedInspectionTask.value = record;
  inspectionTaskDetailVisible.value = true;
}

function openFqcDetail(record?: MesHcCutRoundConsoleApi.CutRoundInspectionTaskDetail | null) {
  if (!record?.fqcOrderId) {
    message.warning('当前裁切片还没有关联成品检验记录');
    return;
  }
  fqcDetailModalApi.setData({ id: record.fqcOrderId }).open();
}

function getInspectionTaskFqcNo(task?: MesHcCutRoundConsoleApi.CutRoundInspectionTask | null) {
  return task?.fqcNo || task?.details?.find((detail) => detail.fqcNo)?.fqcNo || '';
}

function getInspectionTaskFqcOrderId(task?: MesHcCutRoundConsoleApi.CutRoundInspectionTask | null) {
  return task?.fqcOrderId || task?.details?.find((detail) => detail.fqcOrderId)?.fqcOrderId;
}

function openInspectionTaskFqcDetail(task?: MesHcCutRoundConsoleApi.CutRoundInspectionTask | null) {
  const fqcOrderId = getInspectionTaskFqcOrderId(task);
  if (!fqcOrderId) {
    message.warning('当前任务暂未生成FQC单');
    return;
  }
  const detail = task?.details?.find((item) => item.fqcOrderId);
  if (detail) {
    openFqcDetail(detail);
  } else {
    fqcDetailModalApi.setData({ id: fqcOrderId }).open();
  }
}

function openReportFqcDetail(record?: MesHcCutRoundConsoleApi.ReportItem | null) {
  if (!record?.fqcOrderId) {
    message.warning('当前报工记录暂未生成FQC单');
    return;
  }
  fqcDetailModalApi.setData({ id: record.fqcOrderId }).open();
}

function getCurrentSegmentBatchNo() {
  return currentMotherBatchNo.value
    || currentPlan.sourceBatchNo
    || currentPlan.batchNo
    || sourceGroups.value[0]?.baseBatchNo
    || resolveMotherBatchNo({
      parentProductionBatchNo: currentPlan.sourceBatchNo || currentPlan.batchNo,
      sourceProductionBatchNo: currentPlan.sourceProductionBatchNo,
    });
}

function getCurrentChangeoverMotherBatchNo() {
  return getCurrentSegmentBatchNo();
}

function getLatestChangeoverMotherBatchNo(record?: MesHcCutRoundConsoleApi.ChangeoverInspection | null) {
  return resolveMotherBatchNo({
    parentProductionBatchNo: record?.motherSegmentBatchNo,
    sourceProductionBatchNo: record?.pressSlotSliceNo,
  });
}

function getChangeoverInspectionBlockMessage() {
  const currentMother = getCurrentChangeoverMotherBatchNo() || '-';
  const latest = latestChangeoverInspection.value;
  const latestMother = getLatestChangeoverMotherBatchNo(latest) || '-';
  if (!latest?.id) {
    return `今天暂无裁切工艺参数点检记录。当前母卷批号 ${currentMother}，请先完成工艺参数点检后再扫码确认。`;
  }
  return `今天最新工艺参数点检母卷批号为 ${latestMother}，当前母卷批号为 ${currentMother}，两者不一致。请先完成当前母卷批号的工艺参数点检后再扫码确认。`;
}

function showChangeoverInspectionRequiredWarning(initialSliceNo = '') {
  showSinglePlanScanWarning({
    title: '需要先填写工艺参数点检',
    content: getChangeoverInspectionBlockMessage(),
    onOk: () => openChangeoverInspectionScan(initialSliceNo),
  });
}

async function loadGlueBoardUsage() {
  if (!currentPlan.planOperationId) {
    cutRoundConsumables.value = [];
    return;
  }
  cutRoundConsumables.value = normalizeRows(await getCutRoundConsoleCurrentGlueBoardUsage(
    currentPlan.planOperationId,
    selectedBoardEquipmentId.value,
  ));
}

function resetIntermediateForm() {
  Object.assign(intermediateForm, {
    adhesiveReportId: undefined,
    batchNo: '',
    confirmerName: '',
    endSliceNo: '',
    firstSampleSliceNo: '',
    firstSlotDepthAvg: undefined,
    firstSlotDepthMax: undefined,
    firstSlotDepthMin: undefined,
    firstSlotDepthXAvg: undefined,
    firstSlotDepthXMax: undefined,
    firstSlotDepthXMin: undefined,
    firstSlotDepthYAvg: undefined,
    firstSlotDepthYMax: undefined,
    firstSlotDepthYMin: undefined,
    frontSliceNo: '',
    id: undefined,
    inputQty: 0,
    materialCode: currentPlan.materialCode || '',
    middleSliceNo: '',
    modelCode: currentCutRoundProductModelCode.value || '',
    outputQty: 0,
    planId: currentPlan.planId || 0,
    planNo: currentPlan.planNo || '',
    planOperationId: currentPlan.planOperationId || 0,
    processLength: 0,
    productionDate: dayjs().format('YYYY-MM-DD'),
    recordDate: dayjs().format('YYYY-MM-DD'),
    recorderName: currentUserName.value || '',
    recordStatus: 'DRAFT',
    remark: '',
    widthEnd: undefined,
    widthMiddle: undefined,
    widthStart: undefined,
  });
  intermediateDetails.value = buildDefaultCutRoundIntermediateDetails();
}

function normalizeCutRoundIntermediateBatchNo(batchNo?: string) {
  return String(batchNo || '')
    .trim()
    .replace(/-J\d+$/i, '')
    .replace(/-S\d+$/i, '')
    .replace(/^(.+[PQRS])\d{3}[A-Z]?$/i, '$1');
}

function resolveIntermediateBatchNo() {
  const fromReport = reportRecords.value
    .map((item) => item.parentProductionBatchNo || item.sourceProductionBatchNo || item.sourceBatchNo || item.productionBatchNo)
    .find(Boolean);
  const fromSource = sourceGroups.value
    .flatMap((group) => group.segments)
    .map((item) => item.batchNo)
    .find(Boolean);
  return normalizeCutRoundIntermediateBatchNo(fromReport || fromSource || currentPlan.sourceProductionBatchNo || currentPlan.batchNo);
}

function syncIntermediateCountersFromReports() {
  const today = intermediateForm.recordDate || dayjs().format('YYYY-MM-DD');
  const sameDay = reportRecords.value.filter((item) => !item.reportDate || String(item.reportDate).startsWith(today));
  intermediateForm.inputQty = sameDay.reduce((sum, item) => sum + Number(item.inputLength || item.outputLength || 1), 0);
  intermediateForm.outputQty = sameDay
    .filter((item) => isCutRoundRecordConfirmed(item.reportStatus))
    .reduce((sum, item) => sum + Number(item.outputLength || item.inputLength || 1), 0);
  intermediateForm.batchNo = intermediateForm.batchNo || resolveIntermediateBatchNo();
}

function getIntermediateSliceField(position?: string) {
  if (position === 'FIRST') return 'firstSampleSliceNo';
  if (position === 'FRONT') return 'frontSliceNo';
  if (position === 'MIDDLE') return 'middleSliceNo';
  if (position === 'END') return 'endSliceNo';
  return '';
}

function syncIntermediateSliceNoFieldsFromDetails() {
  for (const row of intermediateDetails.value) {
    const field = getIntermediateSliceField(row.samplePosition);
    if (field && row.sliceBatchNo) {
      (intermediateForm as Record<string, any>)[field] = row.sliceBatchNo;
    }
  }
}

function syncIntermediateDetailsFromSliceNoFields() {
  for (const row of intermediateDetails.value) {
    const field = getIntermediateSliceField(row.samplePosition);
    if (field) {
      row.sliceBatchNo = (intermediateForm as Record<string, any>)[field] || row.sliceBatchNo;
    }
  }
}

function getIntermediateDataIndexKey(dataIndex: unknown) {
  if (Array.isArray(dataIndex)) return String(dataIndex[0] || '');
  return String(dataIndex || '');
}

function isIntermediateThicknessColumn(dataIndex: unknown) {
  return getIntermediateDataIndexKey(dataIndex).startsWith('thickness');
}

function getIntermediateNumericValue(record: Record<string, any>, dataIndex: unknown) {
  const key = getIntermediateDataIndexKey(dataIndex);
  return key ? record[key] : undefined;
}

function setIntermediateNumericValue(record: Record<string, any>, dataIndex: unknown, value: number | null) {
  const key = getIntermediateDataIndexKey(dataIndex);
  if (key) {
    record[key] = value ?? undefined;
  }
}

function mapCutRoundIntermediateRecord(data?: MesHcCutRoundConsoleApi.IntermediateRecord) {
  resetIntermediateForm();
  Object.assign(intermediateForm, {
    ...data,
    batchNo: data?.batchNo || resolveIntermediateBatchNo(),
    materialCode: data?.materialCode || currentPlan.materialCode || '',
    modelCode: data?.modelCode || currentCutRoundProductModelCode.value || '',
    planId: currentPlan.planId || data?.planId || 0,
    planNo: currentPlan.planNo || data?.planNo || '',
    planOperationId: currentPlan.planOperationId || data?.planOperationId || 0,
    productionDate: data?.recordDate || data?.productionDate || dayjs().format('YYYY-MM-DD'),
    recordDate: data?.recordDate || data?.productionDate || dayjs().format('YYYY-MM-DD'),
    recorderName: data?.recorderName || currentUserName.value || '',
    recordStatus: data?.recordStatus || 'DRAFT',
  });
  const backendDetails = Array.isArray(data?.details) ? data.details : [];
  const detailMap = new Map<string, MesHcCutRoundConsoleApi.IntermediateDetail>();
  backendDetails.forEach((item) => {
    detailMap.set(item.samplePosition || `ROW-${item.sortNo || detailMap.size + 1}`, item);
  });
  intermediateDetails.value = buildDefaultCutRoundIntermediateDetails().map((row) => ({
    ...row,
    ...(detailMap.get(row.samplePosition || '') || {}),
  }));
  backendDetails.forEach((row) => {
    if (row.samplePosition && cutRoundIntermediatePositions.some((item) => item.value === row.samplePosition)) return;
    intermediateDetails.value.push(row);
  });
  syncIntermediateSliceNoFieldsFromDetails();
  syncIntermediateCountersFromReports();
}

async function loadIntermediateRecord() {
  if (!currentPlan.planId || !currentPlan.planOperationId) {
    resetIntermediateForm();
    return;
  }
  intermediateLoading.value = true;
  try {
    const data = await getCutRoundConsoleIntermediate({
      planId: currentPlan.planId,
      planOperationId: currentPlan.planOperationId,
      recordDate: dayjs().format('YYYY-MM-DD'),
    });
    mapCutRoundIntermediateRecord(data);
  } finally {
    intermediateLoading.value = false;
  }
}

function requestCloseReport() {
  if (reportDialogMode.value === 'view') {
    reportVisible.value = false;
    return;
  }
  AModal.confirm({
    cancelText: '继续填写',
    content: '当前报工登记尚未提交，关闭后本次扫码和工艺参数暂存数据会丢失，确认关闭？',
    okText: '确认关闭',
    okType: 'danger',
    title: '确认关闭报工登记',
    onOk: () => {
      reportVisible.value = false;
    },
  });
}

function clearReportIntermediateDraft() {
  // 裁切中间品已调整为按日业务表，不再使用报工弹窗内的临时草稿。
}

async function saveIntermediateRecord() {
  if (!currentPlan.planId || !currentPlan.planOperationId) {
    message.warning('请先扫描或选择裁切计划');
    return;
  }
  syncIntermediateDetailsFromSliceNoFields();
  syncIntermediateCountersFromReports();
  intermediateLoading.value = true;
  try {
    await saveCutRoundConsoleIntermediate({
      ...intermediateForm,
      batchNo: intermediateForm.batchNo || resolveIntermediateBatchNo(),
      details: intermediateDetails.value,
      planId: currentPlan.planId,
      planNo: currentPlan.planNo,
      planOperationId: currentPlan.planOperationId,
      recordDate: intermediateForm.recordDate || dayjs().format('YYYY-MM-DD'),
      recordStatus: 'RECORDED',
    });
    intermediateForm.recordStatus = 'RECORDED';
    message.success('裁切中间品记录单已保存');
    await loadIntermediateRecord();
  } finally {
    intermediateLoading.value = false;
  }
}

function findReportBySegment(segment: SourceSegment) {
  return reportRecords.value.find((item) => isCutRoundReportForSource(item, segment.batchNo, segment.grindingSecondDetailId));
}

function isPrintableTransferReport(record?: MesHcCutRoundConsoleApi.ReportItem | null): record is MesHcCutRoundConsoleApi.ReportItem {
  return Boolean(record?.id && record.productionBatchNo);
}

function getInheritedCutRoundSizeRule(segment?: SourceSegment | null) {
  return normalizeSizeRule(segment?.actualSizeRule || '');
}

function getCutRoundSegmentReportRecords(segment: SourceSegment) {
  return reportRecords.value.filter((record) =>
    isCutRoundReportForSource(record, segment.batchNo, segment.grindingSecondDetailId),
  );
}

function getCutRoundOneClickConfirmTargets(): CutRoundOneClickConfirmTarget[] {
  const targets: CutRoundOneClickConfirmTarget[] = [];
  sourceGroups.value.forEach((group) => {
    group.segments.forEach((segment) => {
      const pendingRecords = getCutRoundSegmentReportRecords(segment).filter(
        (record) => Boolean(record.id) && !isCutRoundRecordConfirmed(record.reportStatus),
      );
      if (pendingRecords.length) {
        pendingRecords.forEach((record) => targets.push({ group, record, segment }));
        return;
      }
      if (getCutRoundSegmentStatus(segment) === 'PENDING') {
        targets.push({ group, segment });
      }
    });
  });
  return targets;
}

function getCutRoundOneClickActualSizeRule(target: CutRoundOneClickConfirmTarget) {
  return normalizeSizeRule(getReportActualSizeRule(target.record) || getInheritedCutRoundSizeRule(target.segment));
}

async function createCutRoundOneClickConfirmReport(target: CutRoundOneClickConfirmTarget) {
  const matched = findSourceSegmentWithGroup(target.segment.batchNo);
  const group = matched?.group || target.group;
  const segment = matched?.segment || target.segment;
  const actualSizeRule = getCutRoundOneClickActualSizeRule({ group, segment });
  if (!actualSizeRule) {
    throw new Error(`${segment.batchNo} 未继承粘胶2实际尺寸和尾号，不能生成裁切报工记录`);
  }
  resetReportForm();
  activeRecord.value = null;
  reportDialogMode.value = 'confirm';
  reportStep.value = 'SCAN';
  const availableRange = findFirstAvailableRange(segment);
  reportForm.sourceCode = segment.batchNo;
  reportForm.sourceProductionBatchNo = segment.batchNo;
  reportForm.modelCode = segment.modelCode || currentCutRoundProductModelCode.value || '';
  reportForm.parentBatchNo = group.baseBatchNo;
  reportForm.reportType = getDefaultCutRoundReportType(segment);
  reportForm.actualSizeRule = actualSizeRule;
  reportForm.sourceActualSizeRule = getInheritedCutRoundSizeRule(segment);
  reportForm.startPosition = availableRange.start;
  reportForm.endPosition = availableRange.end;
  reportForm.processLength = availableRange.length;
  reportForm.outputLength = reportOutputLength.value;
  reportForm.glueBoardUseLength = availableRange.length;
  reportForm.productionBatchNo = reportForm.sourceActualSizeRule
    ? segment.batchNo
    : normalizeCutRoundProductionBatchNo(segment.batchNo, actualSizeRule);
  reportForm.startTime = buildNowText();
  reportForm.endTime = buildNowText();
  const sourceScanOk = await scanReportSource();
  if (!sourceScanOk) {
    throw new Error(`${segment.batchNo} ${reportForm.sourceScanError || '来源扫码失败'}，未生成裁切报工记录`);
  }
  const reportId = await submitCutRoundReport({
    closeAfterSave: false,
    confirmAfterSave: true,
    silentSuccess: true,
  });
  if (!reportId) {
    throw new Error(`${reportForm.productionBatchNo || segment.batchNo} 保存并扫码确认裁切报工记录失败`);
  }
  return {
    actualSizeRule: normalizeSizeRule(reportForm.actualSizeRule),
    confirmed: true,
    id: reportId,
    productionBatchNo: reportForm.productionBatchNo,
  };
}

async function confirmAllCutRoundReportsOneClick() {
  if (oneClickScanConfirming.value) return;
  if (isCurrentTaskReadonly.value) {
    showReadonlyTaskWarning('一键扫码确认');
    return;
  }
  const pendingTargets = getCutRoundOneClickConfirmTargets();
  if (!pendingTargets.length) {
    message.info('当前工作台没有待扫码确认的裁切片号');
    return;
  }
  if (!ensureDailyPreparationReady() || !isChangeoverInspectionReady() || !ensureCutRoundConsumablesReady('一键扫码确认')) {
    return;
  }
  const targetsWithoutSize = pendingTargets.filter((target) => !getCutRoundOneClickActualSizeRule(target));
  if (targetsWithoutSize.length) {
    const batchNos = targetsWithoutSize
      .map((target) => target.record?.productionBatchNo || target.segment.batchNo || '-')
      .join('、');
    AModal.warning({
      title: '缺少实际尺寸',
      content: `以下 ${targetsWithoutSize.length} 个待确认裁切片号缺少从粘胶2继承的尾号尺寸，不能一键扫码确认：${batchNos}`,
    });
    return;
  }
  const autoCreateCount = pendingTargets.filter((target) => !target.record?.id).length;
  AModal.confirm({
    cancelText: '取消',
    content: `将对 ${pendingTargets.length} 个待确认片号逐片调用现有报工扫码确认，其中 ${autoCreateCount} 片会先按当前单片报工规则生成记录。确认后等同逐片扫码入账。确定继续吗？`,
    okText: '一键扫码确认',
    title: '裁切一键扫码确认',
    async onOk() {
      oneClickScanConfirming.value = true;
      let confirmedCount = 0;
      let createdReportCount = 0;
      const failedItems: string[] = [];
      try {
        for (const target of pendingTargets) {
          let reportId = Number(target.record?.id || 0);
          let productionBatchNo = target.record?.productionBatchNo || target.segment.batchNo;
          let actualSizeRule = getCutRoundOneClickActualSizeRule(target);
          let createdAndConfirmed = false;
          try {
            if (!reportId) {
              const created = await createCutRoundOneClickConfirmReport(target);
              reportId = created.id;
              productionBatchNo = created.productionBatchNo;
              actualSizeRule = created.actualSizeRule;
              createdAndConfirmed = created.confirmed;
              createdReportCount += 1;
            }
            if (!createdAndConfirmed) {
              await confirmCutRoundConsoleReport({
                actualSizeRule,
                confirmerName: currentUserName.value || 'admin',
                confirmerTime: buildNowText(),
                glueBoardModel: undefined,
                id: reportId,
                scannedBatchNo: productionBatchNo || '',
              });
            }
            confirmedCount += 1;
          } catch (error: unknown) {
            const batchNo = productionBatchNo || target.segment.batchNo || '-';
            const reason = getReportRequestErrorMessage(error, '扫码确认失败，请检查报工条件后重试。');
            failedItems.push(`${batchNo}：${reason}`);
          }
        }
        await Promise.all([loadReports(), loadSourceGroups(), loadGlueBoardUsage()]);
        if (failedItems.length) {
          const failurePreview = failedItems.slice(0, 5).join('；');
          const remainingFailureText = failedItems.length > 5 ? `；另有 ${failedItems.length - 5} 片失败` : '';
          AModal.warning({
            title: '一键扫码确认未全部完成',
            content: `已完成 ${confirmedCount}/${pendingTargets.length} 片${createdReportCount ? `，其中 ${createdReportCount} 片已自动生成报工记录` : ''}。失败原因：${failurePreview}${remainingFailureText}`,
          });
          return;
        }
        message.success(`裁切已完成 ${confirmedCount} 片一键扫码确认${createdReportCount ? `，其中 ${createdReportCount} 片已自动生成报工记录` : ''}`);
      } catch (error: unknown) {
        await Promise.all([loadReports(), loadSourceGroups(), loadGlueBoardUsage()]);
        AModal.warning({
          title: '一键扫码确认未全部完成',
          content: `已完成 ${confirmedCount} 片，${getReportRequestErrorMessage(error, '刷新裁切工作台失败，请刷新页面后重试。')}`,
        });
      } finally {
        oneClickScanConfirming.value = false;
      }
    },
  });
}

function isPrintableCutRoundTransferSegment(segment?: SourceSegment | null) {
  return Boolean(String(segment?.batchNo || '').trim());
}

function getTransferReportKey(record?: MesHcCutRoundConsoleApi.ReportItem | null) {
  return record?.id == null ? '' : String(record.id);
}

function getCutRoundTransferPrintTargetKey(segment?: SourceSegment | null) {
  return segment?.batchNo ? `SOURCE:${String(segment.batchNo).trim().toUpperCase()}` : '';
}

function isTransferSegmentSelected(segment: SourceSegment) {
  const key = getCutRoundTransferPrintTargetKey(segment);
  return !!key && selectedTransferReportIds.value.includes(key);
}

function applyReportRecordToForm(record?: MesHcCutRoundConsoleApi.ReportItem) {
  if (!record) return;
  const extra = parseRecordExtra(record) as Record<string, any>;
  const actualSizeRule = getReportActualSizeRule(record) || reportForm.actualSizeRule;
  const productionBatchSizeRule = actualSizeRule
    || resolveCutRoundSizeRuleFromBatchNo(record.productionBatchNo)
    || resolveCutRoundSizeRuleFromBatchNo(record.sourceProductionBatchNo);
  Object.assign(reportForm, {
    actualSizeRule,
    aqcStatus: record.aqcStatus || '',
    aqcTaskId: record.aqcTaskId,
    defectCode: record.defectCode || '',
    endPosition: Number(record.endPosition || 0),
    endTime: record.endTime || '',
    glueBoardBatchNo: record.glueBoardBatchNo || glueBoard.batchNo,
    glueBoardMaterialCode: record.glueBoardMaterialCode || glueBoard.materialCode,
    glueBoardStartPosition: Number(record.glueBoardStartPosition || glueBoard.availableStartPosition || 0),
    glueBoardUsageId: record.glueBoardUsageId,
    glueBoardUseLength: Number(record.glueBoardUseLength || record.inputLength || 0),
    lossLength: Number(record.lossLength || 0),
    modelCode: record.modelCode || extra.runtimeModelCode || reportForm.modelCode || currentCutRoundProductModelCode.value || '',
    napSampleLength: Number(record.napSampleLength || 0),
    outputLength: Number(record.outputLength || 0),
    parentBatchNo: record.parentProductionBatchNo || record.sourceBatchNo || reportForm.parentBatchNo,
    processLength: Number(record.inputLength || record.outputLength || 0),
    productionBatchNo: normalizeCutRoundProductionBatchNo(
      record.productionBatchNo || reportForm.productionBatchNo || record.sourceProductionBatchNo,
      productionBatchSizeRule,
    ),
    preProcessSelfCheckAbnormal: isPreProcessSelfCheckAbnormal(extra),
    remark: record.remark || '',
    reportDate: record.reportDate || reportForm.reportDate,
    reportType: isCutRoundReportType(extra.reportType) ? extra.reportType : reportForm.reportType,
    selfCheck: record.selfCheck || 'OK',
    sourceCode: record.sourceProductionBatchNo || reportForm.sourceCode,
    sourceGrindingSecondDetailId: record.sourceGrindingSecondDetailId || reportForm.sourceGrindingSecondDetailId,
    sourceProductionBatchNo: record.sourceProductionBatchNo || reportForm.sourceProductionBatchNo,
    sourceNgDefectCode: String(extra.sourceNgDefectCode || reportForm.sourceNgDefectCode || ''),
    sourceNgReviewResult: String(extra.sourceNgReviewResult || ''),
    sourceNgDefectSummary: String(extra.sourceNgDefectSummary || ''),
    sourceNgVisualItems: normalizeSourceNgVisualItems(extra.sourceNgVisualItems),
    startPosition: Number(record.startPosition || 0),
    startTime: record.startTime || '',
  });
  applyReportCheckItems(record.checkItems);
  resetVisualInspectionItems(parseVisualItemsFromRecord(record));
}

function openAbnormalCategoryCorrection() {
  if (!canCorrectAbnormalCategory.value) {
    message.warning('当前账号没有修正异常类别权限，或当前片没有裁切外观异常类别');
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
  const currentRecord = activeRecord.value;
  if (!currentRecord?.id) {
    message.warning('未找到可修正的裁切报工记录');
    return;
  }
  if (!abnormalCategoryCorrectionForm.category) {
    message.warning('请选择修正后的异常类别');
    return;
  }
  const reason = abnormalCategoryCorrectionForm.reason.trim();
  if (!reason) {
    message.warning('请填写修正原因');
    return;
  }
  abnormalCategoryCorrectionSaving.value = true;
  try {
    const updatedRecord = await correctCutRoundConsoleReportAbnormalCategory({
      category: abnormalCategoryCorrectionForm.category,
      id: Number(currentRecord.id),
      reason,
    });
    activeRecord.value = updatedRecord;
    applyReportRecordToForm(updatedRecord);
    await Promise.all([loadReports(), loadSourceGroups()]);
    abnormalCategoryCorrectionVisible.value = false;
    message.success('异常类别已修正');
  } catch (error: any) {
    message.error(error?.message || '修正异常类别失败');
  } finally {
    abnormalCategoryCorrectionSaving.value = false;
  }
}

async function assignIntermediateSlice(group: SourceGroup, segment: SourceSegment, samplePosition: string) {
  if (!currentPlan.planId || !currentPlan.planOperationId) {
    message.warning('请先扫描或选择裁切计划');
    return;
  }
  if (!intermediateDetails.value.length) {
    await loadIntermediateRecord();
  }
  const position = cutRoundIntermediatePositions.find((item) => item.value === samplePosition);
  if (!position) return;
  const report = findReportBySegment(segment);
  const sliceField = getIntermediateSliceField(samplePosition);
  if (sliceField) {
    (intermediateForm as Record<string, any>)[sliceField] = segment.batchNo;
  }
  if (samplePosition !== 'FIRST') {
    const target = intermediateDetails.value.find((row) => row.samplePosition === samplePosition);
    const nextRow: MesHcCutRoundConsoleApi.IntermediateDetail = {
      ...(target || {}),
      pressSlotReportId: report?.id,
      samplePosition,
      samplePositionName: position.label,
      sliceBatchNo: segment.batchNo,
      sourceSlittingSliceId: segment.grindingSecondDetailId,
      sortNo: cutRoundMeasuredPositions.findIndex((item) => item.value === samplePosition) + 1,
    };
    if (target) {
      Object.assign(target, nextRow);
    } else {
      intermediateDetails.value.push(nextRow);
    }
  }
  Object.assign(intermediateForm, {
    batchNo: normalizeCutRoundIntermediateBatchNo(group.baseBatchNo || segment.batchNo),
    materialCode: group.materialCode || currentPlan.materialCode || intermediateForm.materialCode || '',
    modelCode: group.modelCode || currentCutRoundProductModelCode.value || intermediateForm.modelCode || '',
    planId: currentPlan.planId || 0,
    planNo: currentPlan.planNo || '',
    planOperationId: currentPlan.planOperationId || 0,
    recordDate: intermediateForm.recordDate || dayjs().format('YYYY-MM-DD'),
    recorderName: intermediateForm.recorderName || currentUserName.value || '',
  });
  syncIntermediateCountersFromReports();
  await saveIntermediateRecord();
  activeBoardTab.value = 'MIDDLE_LEDGER';
}


function isPlanNoReadyForAutoScan(value?: string) {
  const planNo = String(value || '').trim();
  if (planNo.length < PLAN_SCAN_MIN_LENGTH) return false;
  if (planNo.startsWith('PPD')) return planNo.length >= 17;
  if (planNo.startsWith('P-')) return planNo.length >= 11;
  return planNo.length >= PLAN_SCAN_MIN_LENGTH;
}

function schedulePlanScan(value?: string) {
  if (planScanTimer) {
    clearTimeout(planScanTimer);
    planScanTimer = null;
  }
  if (suppressNextPlanScanSchedule) {
    suppressNextPlanScanSchedule = false;
    return;
  }
  const planNo = syncPlanScanNo(value);
  const scannerInput = hasPlanScanDelimiter(value) || pendingScannerPlanNo === planNo || !!pendingScannerSliceBatchNo;
  if (!planNo || (!scannerInput && !isPlanNoReadyForAutoScan(planNo)) || (!scannerInput && planNo === lastAutoScannedPlanNo.value)) return;
  planScanTimer = setTimeout(() => {
    if (planNo !== normalizePlanScanNo(scanPlanNo.value)) {
      if (pendingScannerPlanNo === planNo) pendingScannerPlanNo = '';
      return;
    }
    if (pendingScannerPlanNo === planNo) pendingScannerPlanNo = '';
    void consumePlanScan(true);
  }, 260);
}

async function loadTaskList() {
  taskListLoading.value = true;
  try {
    taskRows.value = normalizeRows(await getCutRoundConsoleTaskList({
      equipmentCode: selectedBoardEquipmentCode.value || undefined,
      equipmentId: selectedBoardEquipmentId.value,
      taskStatus: taskListFilters.status as MesHcCutRoundConsoleApi.TaskQuery['taskStatus'],
    }));
    ensureBoardEquipmentFromTasks(taskRows.value);
    taskListPage.value = 1;
  } finally {
    taskListLoading.value = false;
  }
}

function resetTaskListFilters() {
  taskListFilters.planNo = '';
  taskListFilters.modelCode = '';
  taskListFilters.batchNo = '';
  taskListFilters.status = 'UNFINISHED';
  taskListPage.value = 1;
}

async function handleTaskListSearch() {
  taskListPage.value = 1;
  await loadTaskList();
}

async function openTaskList() {
  taskListVisible.value = true;
  await loadTaskList();
}

async function openStartTaskSizeRule(task: MesHcCutRoundConsoleApi.TaskItem) {
  if (!task?.planOperationId) return;
  await startTaskFromList(task, resolveActualSizeRule(task));
}

async function loadDailyPreparationRowsForTask(task: MesHcCutRoundConsoleApi.TaskItem) {
  if (!task?.planId || !task?.planOperationId) return [];
  const taskEquipment = hasBoardEquipmentValue() ? getEffectiveBoardEquipment(task) : getTaskEquipment(task);
  const rows = normalizeRows(await getCutRoundConsolePassWorkList(
    task.planId,
    task.planOperationId,
    {
      equipmentId: taskEquipment.equipmentId,
      recordDate: dayjs().format('YYYY-MM-DD'),
    },
  ));
  return rows
    .map(mapPassWorkRecord)
    .filter((item) => isDailyCleanRecord(item) || isDailyProcessCheckRecord(item))
    .sort((a, b) => Number(!isDailyCleanRecord(a)) - Number(!isDailyCleanRecord(b)));
}

function showTaskDailyPreparationRequiredWarning(task: MesHcCutRoundConsoleApi.TaskItem) {
  const today = dayjs().format('YYYY-MM-DD');
  showSinglePlanScanWarning({
    okText: '去填写',
    title: '请先填写今日点检/清洁记录',
    content: `今天 ${today} 暂无当前裁切设备的已保存点检/清洁记录。请先完成点检/清洁记录后，再加载计划 ${task.planNo || '-'} 的裁切任务。`,
    onOk: () => {
      if (!hasBoardEquipmentValue()) {
        applyBoardEquipmentFromTask(task, { force: true });
      }
      void openDailyRecordList();
    },
  });
}

async function ensureTaskDailyPreparationReady(task: MesHcCutRoundConsoleApi.TaskItem) {
  const rows = await loadDailyPreparationRowsForTask(task);
  if (isDailyPreparationRowsReady(rows)) return true;
  dailyRecordRows.value = rows.length > 0 ? rows : createDailyRecordFallbackRows();
  showTaskDailyPreparationRequiredWarning(task);
  return false;
}

async function startTaskFromList(task: MesHcCutRoundConsoleApi.TaskItem, actualSizeRule?: string) {
  if (!task?.planOperationId) return;
  const taskStatus = normalizeWorkOrderStatus(task.status);
  const readonlyBlockedTask = taskStatus === 'PAUSED' || taskStatus === 'CANCELLED';
  if (
    taskStatus !== 'FINISHED' &&
    !readonlyBlockedTask &&
    !(await ensureTaskDailyPreparationReady(task))
  ) {
    return;
  }
  const taskSourceBatchNo = resolveMotherBatchNo(task as Record<string, any>);
  if (
    normalizeWorkOrderStatus(task.status) !== 'FINISHED' &&
    !(await ensureSampleAbnormalUnlocked(
      buildSegmentChainSampleLockCandidates({
        motherBatchNo: taskSourceBatchNo,
        segmentBatchNo: taskSourceBatchNo,
      }),
      '加载裁切',
    ))
  ) {
    return;
  }
  setManualTaskPlanScanNo(task.planNo);
  const taskActualSizeRule = normalizeSizeRule(actualSizeRule || resolveActualSizeRule(task));
  const equipmentReady = await ensureTaskEquipment(task);
  if (equipmentReady === null) return;
  currentPlan.actualSizeRule = taskActualSizeRule;
  applyTask(task);
  currentPlan.actualSizeRule = taskActualSizeRule;
  await loadCheckTemplate();
  lastAutoScannedPlanNo.value = task.planNo || scanPlanNo.value.trim();
  if (taskStatus !== 'FINISHED' && !readonlyBlockedTask && taskStatus !== 'RUNNING') {
    if (!task.equipmentId) {
      message.warning('当前裁切工序未绑定设备，请先在计划工艺路线配置裁切机台');
      return;
    }
    try {
      await startCutRoundConsoleWorkOrder({
        equipmentCode: task.equipmentCode,
        equipmentId: task.equipmentId,
        equipmentName: task.equipmentName,
        planId: task.planId,
        planOperationId: task.planOperationId,
        recorderName: currentUserName.value || undefined,
        recorderTime: buildNowText(),
        reportDate: dayjs().format('YYYY-MM-DD'),
        startTime: buildNowText(),
      });
    } catch (error: any) {
      if (!String(error?.message || '').includes('已开工')) throw error;
    }
    currentPlan.status = 'RUNNING';
    currentPlan.startTime = currentPlan.startTime || buildNowText();
  }
  await Promise.all([loadDailyRecords(), loadReports(), loadGlueBoardUsage()]);
  await loadSourceGroups(taskSourceBatchNo);
  await loadChangeoverInspections();
  await loadInspectionTasks();
  await loadIntermediateRecord();
  taskListVisible.value = false;
  if (readonlyBlockedTask) {
    return;
  }
  if (taskStatus !== 'FINISHED' && !isChangeoverInspectionReady()) {
    showChangeoverInspectionRequiredWarning();
  }
  // message.success('已开工加载裁切计划与可加工来源');
}

async function startCutRoundWorkOrderIfNeeded(
  task: MesHcCutRoundConsoleApi.TaskItem,
  taskStatus = normalizeWorkOrderStatus(task.status),
) {
  if (taskStatus === 'FINISHED') {
    showSinglePlanScanWarning({
      content: `计划号 ${task.planNo || '-'} 对应裁切分段已完成，仅可查看，不能再新建裁切报工。`,
      okText: '知道了',
      onOk: focusPlanScanInput,
      title: '裁切分段已完成',
    });
    return false;
  }
  if (taskStatus === 'PAUSED' || taskStatus === 'CANCELLED') {
    currentPlan.status = taskStatus;
    return false;
  }
  const taskSourceBatchNo = resolveMotherBatchNo(task as Record<string, any>);
  if (
    !(await ensureSampleAbnormalUnlocked(
      buildSegmentChainSampleLockCandidates({
        motherBatchNo: taskSourceBatchNo,
        segmentBatchNo: taskSourceBatchNo,
      }),
      '开工裁切',
    ))
  ) {
    return false;
  }
  const equipmentReady = await ensureTaskEquipment(task);
  if (equipmentReady === null) return false;
  if (taskStatus === 'RUNNING') {
    currentPlan.status = 'RUNNING';
    syncCurrentPlanEquipmentFromBoard(task);
    return true;
  }
  if (!task.equipmentId) {
    showSinglePlanScanWarning({
      content: '当前裁切工序未绑定设备，请先在计划工艺路线配置裁切机台。',
      okText: '知道了',
      onOk: focusPlanScanInput,
      title: '裁切工单未绑定设备',
    });
    return false;
  }
  try {
    await startCutRoundConsoleWorkOrder({
      equipmentCode: task.equipmentCode,
      equipmentId: task.equipmentId,
      equipmentName: task.equipmentName,
      planId: task.planId,
      planOperationId: task.planOperationId,
      recorderName: currentUserName.value || undefined,
      recorderTime: buildNowText(),
      reportDate: dayjs().format('YYYY-MM-DD'),
      startTime: buildNowText(),
    });
  } catch (error: any) {
    if (!String(error?.message || '').includes('已开工')) throw error;
  }
  currentPlan.status = 'RUNNING';
  currentPlan.startTime = currentPlan.startTime || buildNowText();
  return true;
}

async function handleWorkOrderCompleteTodo() {
  await loadReports();
  if (warnSampleLocked(reportRecords.value)) return;
  if (!currentPlan.planId || !currentPlan.planOperationId) {
    message.warning('请先扫码或从待加工列表选择裁切工单');
    return;
  }
  if (isWorkOrderFinished.value) {
    message.warning('当前裁切工单已完工');
    return;
  }
  if (!isWorkOrderRunning.value) {
    message.warning('当前裁切工单未开工，不能执行工单完工');
    return;
  }
  AModal.confirm({
    okText: '确认完工',
    title: '确认裁切工单完工',
    content: '执行后会标识当前裁切工单全部完成，后续不能再新增裁切报工记录。请确认所有报工记录已打印并扫码确认。',
    async onOk() {
      await completeCutRoundConsoleWorkOrder({
        confirmerName: currentUserName.value || undefined,
        confirmerTime: buildNowText(),
        endTime: buildNowText(),
        planId: currentPlan.planId!,
        planOperationId: currentPlan.planOperationId!,
        recorderName: currentUserName.value || undefined,
        recorderTime: buildNowText(),
        remark: '裁切看板工单完工',
        reportDate: dayjs().format('YYYY-MM-DD'),
      });
      currentPlan.status = 'FINISHED';
      currentPlan.endTime = buildNowText();
      await Promise.all([loadReports(), loadSourceGroups(), loadTaskList(), loadInspectionTasks()]);
      message.success('裁切工单已完工');
    },
  });
}

function showReadonlyTaskWarning(actionName = '操作', group?: SourceGroup | null) {
  const segmentFinished = group ? isSourceGroupCompleted(group) : isCurrentSegmentFinished.value;
  const subject = segmentFinished ? '当前裁切分段' : '当前裁切工单';
  message.warning(`${subject}已完工，仅可查看，不能${actionName}`);
}

async function handleSegmentComplete() {
  if (!currentPlan.planId || !currentPlan.planOperationId) {
    message.warning('请先扫码或从待加工列表选择裁切工单');
    return;
  }
  const group = await refreshActiveSourceGroup(activeSourceGroup.value || {
    baseBatchNo: currentSegmentBatchNo.value || currentPlan.sourceBatchNo || currentPlan.batchNo,
    segments: [],
    totalAvailableLength: 0,
    totalOutputLength: 0,
    totalUsedLength: 0,
  });
  if (!group?.baseBatchNo) {
    message.warning('请先选择当前裁切分段');
    return;
  }
  if (isSourceGroupCompleted(group)) {
    message.warning(`当前裁切分段 ${group.baseBatchNo} 已完成`);
    return;
  }
  const records = getSourceGroupReportRecords(group);
  if (warnSampleLocked(records)) return;
  if (!records.length) {
    message.warning(`当前分段 ${group.baseBatchNo} 暂无裁切报工记录，不能本段完工`);
    return;
  }
  const unconfirmedRecords = records.filter((record) => !isCutRoundRecordConfirmed(record.reportStatus));
  if (unconfirmedRecords.length) {
    selectedReportRecordKeys.value = unconfirmedRecords
      .map((record) => record.id)
      .filter((id): id is number => id !== undefined && id !== null);
    reportRecordListVisible.value = true;
    message.warning(`当前分段 ${group.baseBatchNo} 还有 ${unconfirmedRecords.length} 条报工未扫码确认，不能本段完工`);
    return;
  }
  pendingSegmentCompleteGroup.value = group;
  segmentCompleteAuthVisible.value = true;
}

async function handleSegmentCompleteAuthSuccess(userInfo?: Record<string, any>) {
  const pendingGroup = pendingSegmentCompleteGroup.value;
  if (!pendingGroup?.baseBatchNo || !currentPlan.planId || !currentPlan.planOperationId) {
    segmentCompleteAuthVisible.value = false;
    pendingSegmentCompleteGroup.value = null;
    message.warning('当前裁切分段信息已失效，请重新选择后再本段完工');
    return;
  }
  const group = await refreshActiveSourceGroup(pendingGroup);
  const records = getSourceGroupReportRecords(group);
  if (warnSampleLocked(records)) return;
  if (!records.length) {
    message.warning(`当前分段 ${group.baseBatchNo} 暂无裁切报工记录，不能本段完工`);
    return;
  }
  const unconfirmedRecords = records.filter((record) => !isCutRoundRecordConfirmed(record.reportStatus));
  if (unconfirmedRecords.length) {
    selectedReportRecordKeys.value = unconfirmedRecords
      .map((record) => record.id)
      .filter((id): id is number => id !== undefined && id !== null);
    reportRecordListVisible.value = true;
    message.warning(`当前分段 ${group.baseBatchNo} 还有 ${unconfirmedRecords.length} 条报工未扫码确认，不能本段完工`);
    return;
  }
  const confirmerName = String(userInfo?.empName || userInfo?.nickname || userInfo?.realName || userInfo?.username || currentUserName.value || '系统');
  const now = buildNowText();
  await completeCutRoundConsoleSegment({
    confirmerName,
    confirmerTime: now,
    endTime: now,
    planId: currentPlan.planId,
    planOperationId: currentPlan.planOperationId,
    recorderName: confirmerName,
    recorderTime: now,
    remark: `裁切看板本段完工：${group.baseBatchNo}`,
    reportDate: dayjs().format('YYYY-MM-DD'),
    sourceProductionBatchNo: group.baseBatchNo,
  });
  await Promise.all([loadReports(), loadSourceGroups(), loadTaskList(), loadInspectionTasks()]);
  markTaskListSourceGroupCompleted(group);
  segmentCompleteAuthVisible.value = false;
  pendingSegmentCompleteGroup.value = null;
  message.success(`裁切分段 ${group.baseBatchNo} 已完工`);
}

function handleSegmentCompleteAuthCancel() {
  pendingSegmentCompleteGroup.value = null;
  segmentCompleteAuthVisible.value = false;
}

function parseRecordExtra(record: MesHcCutRoundConsoleApi.ReportItem | Record<string, any>) {
  try {
    const extra = record?.extraJson ? JSON.parse(record.extraJson) : {};
    return extra && typeof extra === 'object' ? extra : {};
  } catch {
    return {};
  }
}

function isPressSlotProcessCheckSource(source?: MesHcCutRoundConsoleApi.SourceItem | Record<string, any> | null) {
  const row = (source || {}) as Record<string, any>;
  const extra = parseRecordExtra(row);
  const reportType = String(extra.reportType || extra.inspectionScene || extra.inspectionSampleCategory || '').trim().toUpperCase();
  const sourceMenuCode = String(row.sourceMenuCode || '').trim().toUpperCase();
  return sourceMenuCode.includes('PRESS_SLOT') && reportType === 'PROCESS_CHECK';
}

function getDefaultGlueBoardModel(modelCode?: string) {
  const code = String(modelCode || '').toUpperCase();
  if (code.includes('W26P0200')) return 'W250';
  if (code.includes('W26P0300')) return 'SDK';
  return 'W220';
}

function getRecordPrintMeta(record: MesHcCutRoundConsoleApi.ReportItem | Record<string, any>) {
  const extra = parseRecordExtra(record) as Record<string, any>;
  const status =
    String(extra.printStatus || '').toUpperCase() === 'PRINTED' || extra.printStatus === '已打印'
      ? '已打印'
      : '未打印';
  return {
    count: Number(extra.printCount || 0),
    status,
    time: extra.printTime || '',
  };
}

function getRecordStatusMeta(status?: string) {
  const normalized = String(status || 'DRAFT').toUpperCase();
  if (normalized === 'SUBMITTED') return { color: 'green', text: '已过站' };
  if (normalized === 'CONFIRMED') return { color: 'blue', text: '已扫码确认' };
  return { color: 'orange', text: '草稿' };
}

function getInspectionStatusMeta(status?: string, result?: string) {
  const normalizedResult = String(result || '').toUpperCase();
  if (['OK', 'PASS', 'PASSED'].includes(normalizedResult)) return { color: 'green', text: '检测OK' };
  if (['NG', 'FAIL', 'FAILED', 'ABNORMAL'].includes(normalizedResult)) return { color: 'red', text: '检测NG' };
  const normalized = String(status || '').toUpperCase();
  if (normalized === 'COMPLETED') return { color: 'green', text: '已检验' };
  if (normalized === 'REJECTED') return { color: 'red', text: '不合格' };
  if (normalized === 'PARTIAL_NG') return { color: 'orange', text: '部分NG' };
  if (normalized === 'WAITING_QA') return { color: 'gold', text: '待审核' };
  if (normalized === 'PENDING') return { color: 'default', text: '待检' };
  if (normalized === 'SUSPENDED') return { color: 'warning', text: '已挂起' };
  if (normalized === 'INSPECTING') return { color: 'processing', text: '检测中' };
  return { color: 'default', text: '未报检' };
}

function isReportInspecting(record?: MesHcCutRoundConsoleApi.ReportItem | null) {
  return String(record?.inspectionStatus || '').toUpperCase() === 'INSPECTING';
}

function isReportInspectionCompleted(record?: MesHcCutRoundConsoleApi.ReportItem | null) {
  const status = String(record?.inspectionStatus || '').toUpperCase();
  return status === 'COMPLETED' || ['OK', 'NG', 'PASS', 'PASSED', 'FAIL', 'FAILED'].includes(String(record?.inspectionResult || '').toUpperCase());
}

function isReportSubmittedForInspection(record?: MesHcCutRoundConsoleApi.ReportItem | null) {
  return Boolean(record?.inspectionTaskId) || isReportInspecting(record) || isReportInspectionCompleted(record);
}

function isCutRoundRecordConfirmed(status?: string) {
  const normalized = String(status || '').toUpperCase();
  return normalized === 'CONFIRMED' || normalized === 'SUBMITTED';
}

function isCutRoundRecordSubmitted(status?: string) {
  return String(status || '').toUpperCase() === 'SUBMITTED';
}

function getCutRoundSegmentReportType(segment: SourceSegment): CutRoundReportType {
  const existedRecord = findReportBySegment(segment);
  if (!existedRecord) return 'PRODUCT';
  const extra = parseRecordExtra(existedRecord) as Record<string, any>;
  return isCutRoundReportType(extra.reportType) ? extra.reportType : 'PRODUCT';
}

function getCutRoundSegmentCoaFlag(segment: SourceSegment) {
  const existedRecord = findReportBySegment(segment);
  const extra = parseRecordExtra(existedRecord || {}) as Record<string, any>;
  const sourceExtra = parseRecordExtra(segment as Record<string, any>);
  const raw = extra.coaFlag ?? sourceExtra.coaFlag ?? (segment as Record<string, any>).coaFlag;
  return raw === true || raw === 'true' || raw === 'Y' || raw === 1;
}

function getCutRoundSegmentInspectionNg(segment: SourceSegment) {
  const existedRecord = findReportBySegment(segment);
  const extra = parseRecordExtra(existedRecord || {}) as Record<string, any>;
  return ['NG', 'ABNORMAL'].includes(String(existedRecord?.inspectionResult || extra.feedbackResult || extra.inspectionResult || '').toUpperCase());
}

function normalizeCutRoundCardResultText(value?: string) {
  const text = String(value || '').trim();
  const normalized = text.toUpperCase();
  if (!text || text === '-' || text.includes('未提交') || text.includes('草稿') || text.includes('取消')) return '';
  if (normalized === 'OK' || normalized === 'QUALIFIED' || text.includes('完成') || text.includes('合格') || text.includes('正常')) return '正常';
  if (normalized === 'NG' || normalized === 'ABNORMAL' || normalized === 'REJECTED' || text.includes('驳回') || text.includes('异常')) return 'NG';
  if (['PENDING', 'WAIT_QA', 'WAITING_QA', 'INSPECTING'].includes(normalized) || text.includes('待') || text.includes('检测') || text.includes('复核')) return '待检';
  return text;
}

function getCutRoundCardResultTone(resultText?: string) {
  const text = normalizeCutRoundCardResultText(resultText);
  if (text === 'NG') return 'is-ng';
  if (text === '正常') return 'is-ok';
  if (text === '待检') return 'is-pending';
  return '';
}

function getCutRoundInspectionResultText(record?: MesHcCutRoundConsoleApi.ReportItem | Record<string, any> | null) {
  if (!record) return '';
  const rawRecord = record as Record<string, any>;
  const extra = parseRecordExtra(rawRecord) as Record<string, any>;
  const result = String(rawRecord.inspectionResult || extra.feedbackResult || extra.inspectionResult || '').toUpperCase();
  if (['OK', 'PASS', 'PASSED'].includes(result)) return '正常';
  if (['NG', 'FAIL', 'FAILED', 'ABNORMAL'].includes(result)) return 'NG';
  const status = String(rawRecord.inspectionStatus || extra.inspectionStatus || '').toUpperCase();
  if (status === 'COMPLETED') return '正常';
  if (['INSPECTING', 'PENDING', 'WAITING_QA', 'WAIT_QA'].includes(status)) return '待检';
  if (rawRecord.inspectionTaskId || extra.inspectionTaskId) return '待检';
  return normalizeCutRoundCardResultText(extra.displayText || getInspectionStatusMeta(status, result).text);
}

function getCutRoundSegmentInspectionLabel(
  reportType: CutRoundReportType,
  record: MesHcCutRoundConsoleApi.ReportItem | Record<string, any>,
) {
  const rawRecord = record as Record<string, any>;
  const extra = parseRecordExtra(rawRecord) as Record<string, any>;
  if (reportType === 'CHANGEOVER') return '首检';
  if (
    rawRecord.inspectionTaskId ||
    rawRecord.fqcOrderId ||
    rawRecord.fqcNo ||
    extra.inspectionTaskId ||
    extra.fqcOrderId ||
    extra.fqcNo
  ) {
    return '成品检验';
  }
  if (reportType === 'PROCESS_CHECK') return '加检';
  return '成品检验';
}

function getCutRoundSegmentInspectionEntries(segment: SourceSegment) {
  const entries: Array<{ label: string; resultText: string; tone: string }> = [];
  const pushEntry = (label: string, resultText = '') => {
    const normalizedResult = normalizeCutRoundCardResultText(resultText);
    if (!label || !normalizedResult) return;
    entries.push({ label, resultText: normalizedResult, tone: getCutRoundCardResultTone(normalizedResult) });
  };
  const record = findReportBySegment(segment);
  if (getCutRoundSegmentCoaFlag(segment)) {
    pushEntry('COA', getCutRoundInspectionResultText(record) || '待检');
    return entries.slice(0, 1);
  }
  if (!record || !isReportSubmittedForInspection(record)) return entries;
  const reportType = getCutRoundSegmentReportType(segment);
  pushEntry(getCutRoundSegmentInspectionLabel(reportType, record), getCutRoundInspectionResultText(record) || '待检');
  return entries.slice(0, 1);
}

function getCutRoundSegmentInspectionBackgroundClass(segment: SourceSegment) {
  const primary = getCutRoundSegmentInspectionEntries(segment)[0];
  if (!primary) return '';
  if (primary.resultText === 'NG') return 'is-inspection-bg-ng';
  if (primary.resultText === '待检') return 'is-inspection-bg-pending';
  if (primary.resultText === '正常') return 'is-inspection-bg-ok';
  return '';
}

function shouldShowCutRoundScanConfirmLine(segment: SourceSegment) {
  const status = getCutRoundSegmentStatus(segment);
  return status === 'PENDING' || (!!findReportBySegment(segment) && !!getCutRoundSegmentReportNgText(segment));
}

function getCutRoundScanConfirmText(segment: SourceSegment) {
  const status = getCutRoundSegmentStatus(segment);
  return findReportBySegment(segment) || ['COMPLETED', 'INSPECTED', 'INSPECTING'].includes(status) ? '已确认' : '待确认';
}

function getCutRoundScanConfirmLineClass(segment: SourceSegment) {
  return getCutRoundScanConfirmText(segment) === '已确认' ? 'is-ok' : 'is-scan-pending';
}

function shouldShowCutRoundSelfCheckLine(segment: SourceSegment) {
  if (getCutRoundSegmentReportNgText(segment)) return true;
  if (getCutRoundSegmentInspectionEntries(segment).length > 0) return false;
  return !!findReportBySegment(segment) || isSourceSegmentNg(segment);
}

function formatCutRoundSelfCheckLabel(processName?: string) {
  const normalized = normalizeProcessName(processName).replace(/工序$/, '');
  return normalized ? `${normalized}工序自检` : '自检';
}

function getCutRoundSelfCheckLabel(segment: SourceSegment) {
  const existedRecord = findReportBySegment(segment);
  if (existedRecord && isCutRoundPreProcessAttributedNg(segment)) {
    return formatCutRoundSelfCheckLabel(resolveNgProcessName(existedRecord, '粘胶2'));
  }
  if (isSourceSegmentNg(segment)) {
    return formatCutRoundSelfCheckLabel(segment.sourceNgProcessName || segment.sourceNgText?.replace(/工序NG$/, ''));
  }
  if (getCutRoundSegmentReportNgText(segment)) {
    return formatCutRoundSelfCheckLabel(resolveNgProcessName(findReportBySegment(segment), '裁切'));
  }
  return '自检';
}

function getCutRoundSelfCheckText(segment: SourceSegment) {
  const reportNgText = getCutRoundSegmentReportNgText(segment);
  if (reportNgText) return 'NG';
  return findReportBySegment(segment) ? '正常' : '';
}

function getCutRoundSelfCheckLineClass(segment: SourceSegment) {
  return getCutRoundSegmentReportNgText(segment) ? 'is-ng' : 'is-ok';
}

function getCutRoundSegmentNgReason(segment: SourceSegment) {
  const existedRecord = findReportBySegment(segment);
  if (existedRecord && isCutRoundPreProcessAttributedNg(segment)) {
    return getNgReasonText(existedRecord) || getCutRoundSegmentReportNgText(segment);
  }
  return segment.sourceNgReason || getCutRoundSelfCheckText(segment);
}

function getCutRoundSegmentPositionText(segment: SourceSegment) {
  const reportType = getCutRoundSegmentReportType(segment);
  if (reportType === 'FRONT') return '前段';
  if (reportType === 'MIDDLE') return '中段';
  if (reportType === 'END') return '后段';
  const report = findReportBySegment(segment);
  const extra = parseRecordExtra(report || (segment as Record<string, any>)) as Record<string, any>;
  const extraText = String(extra.samplePositionName || extra.segmentPositionName || extra.reportTypeName || '').trim();
  if (extraText.includes('前')) return '前段';
  if (extraText.includes('中')) return '中段';
  if (extraText.includes('后')) return '后段';
  return '';
}

function hasCutRoundReportVisualIssue(record?: MesHcCutRoundConsoleApi.ReportItem | Record<string, any> | null) {
  if (!record) return false;
  if (normalizeVisualResult((record as Record<string, any>).selfCheck) === 'NG') return true;
  const extra = parseRecordExtra(record) as Record<string, any>;
  if (normalizeVisualResult(extra.visualInspectionResult || extra.selfCheck) === 'NG') return true;
  return parseVisualItemsFromRecord(record).some((item) => isVisualItemActive(item));
}

function getActiveVisualCategoryItems(record?: MesHcCutRoundConsoleApi.ReportItem | Record<string, any> | null) {
  const source = (record || activeRecord.value || {}) as Record<string, any>;
  const extra = parseRecordExtra(source) as Record<string, any>;
  const categories = parseVisualItemsFromRecord(source)
    .filter((item) => isVisualItemActive(item))
    .map((item) => item.itemName)
    .filter((itemName) => cutRoundVisualItemNames.includes(itemName));
  const defectCode = String(source.defectCode || extra.defectCode || '').trim();
  if (cutRoundVisualItemNames.includes(defectCode) && !categories.includes(defectCode)) {
    categories.push(defectCode);
  }
  return Array.from(new Set(categories));
}

function getActiveVisualCategoryText(record?: MesHcCutRoundConsoleApi.ReportItem | Record<string, any> | null) {
  const items = getActiveVisualCategoryItems(record);
  return items.length > 0 ? items.join('、') : '-';
}

function getCutRoundSegmentReportNgText(segment: SourceSegment) {
  if (segment.sourceNgText) return segment.sourceNgText;
  const existedRecord = findReportBySegment(segment);
  return hasProcessNgFromRecord(existedRecord) ? formatProcessNgText(resolveNgProcessName(existedRecord, '裁切')) : '';
}

function getCutRoundReportTypeClass(type?: string) {
  const classMap: Record<CutRoundReportType, string> = {
    CHANGEOVER: 'is-type-changeover',
    END: 'is-type-end',
    FRONT: 'is-type-front',
    MIDDLE: 'is-type-middle',
    PROCESS_CHECK: 'is-type-process',
    PRODUCT: 'is-type-product',
  };
  return isCutRoundReportType(type) ? classMap[type] : classMap.PRODUCT;
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

async function exportCutRoundWorkOrders(records: MesHcCutRoundConsoleApi.ReportItem[]) {
  const validRows = records.filter((record) => !!record?.id && !!record.productionBatchNo);
  if (!validRows.length) {
    AModal.info({ title: '没有可导出记录', content: '请选择已有生产批次号的裁切报工记录。' });
    return;
  }
  const printWindow = window.open('', '_blank', 'width=980,height=720');
  if (!printWindow) return;
  const now = buildNowText();
  const planDetail = currentPlan.planId ? await getPlanOrderDetail(currentPlan.planId as any) : {};
  const ticketRows = await Promise.all(
    validRows.map(async (record) => {
      const extra = parseRecordExtra(record) as Record<string, any>;
      const sourceSegment = getCutRoundSourceSegmentByBatchNo(record.sourceProductionBatchNo);
      return {
        confirmer: currentUserName.value || 'admin',
        currentSection: '裁切',
        endTime: record.endTime,
        materialCode: record.materialCode || extra.runtimeMaterialCode || currentPlan.materialCode,
        metricValue: Number(record.outputLength || record.inputLength || 0),
        modelCode: record.modelCode || extra.runtimeModelCode || sourceSegment?.modelCode || currentCutRoundProductModelCode.value,
        parentBatchNo: record.parentProductionBatchNo || record.sourceProductionBatchNo,
        planNo: record.planNo || currentPlan.planNo,
        productionBatchNo: record.productionBatchNo,
        qrDataUrl: await createPrintQrDataUrl(
          buildTransferTicketQrValue(record.planNo || currentPlan.planNo, record.productionBatchNo),
        ),
        recorder: currentUserName.value || 'admin',
        remark: [
          `来源分段:${record.sourceProductionBatchNo || '-'}`,
          `分段批次号:${record.parentProductionBatchNo || '-'}`,
          `损耗:${formatNumber(record.lossLength) || '-'}`,
          `产出:${formatNumber(record.outputLength) || '-'}`,
          `NAP:${formatNumber(record.napSampleLength) || '-'}`,
          `刀片:${record.glueBoardMaterialCode || '-'} / ${record.glueBoardBatchNo || '-'}`,
        ].join(' '),
        segmentMark: '',
        startTime: record.startTime,
      };
    }),
  );
  printWindow.document.write(
    buildWorkOrderTicketHtml(ticketRows, {
      materialCode: currentPlan.materialCode,
      modelCode: currentCutRoundProductModelCode.value,
      operations: resolvePlanPrintOperations(planDetail as Record<string, any>),
      planNo: currentPlan.planNo,
      printTime: now,
    }),
  );
  printWindow.document.close();
  printWindow.focus();
  message.success(`已打开${validRows.length}条裁切工单预览`);
}

function resolveCutRoundProcessName(record?: MesHcCutRoundConsoleApi.ReportItem | Record<string, any>) {
  return String(
    (record as Record<string, any> | undefined)?.operationName ||
      (record as Record<string, any> | undefined)?.reportProcess ||
      (record as Record<string, any> | undefined)?.processName ||
      '裁切',
  );
}

async function buildCutRoundTransferTicketItem(record: MesHcCutRoundConsoleApi.ReportItem, now: string) {
  const planNo = record.planNo || currentPlan.planNo;
  const processName = resolveCutRoundProcessName(record);
  const extra = parseRecordExtra(record) as Record<string, any>;
  const sourceSegment = getCutRoundSourceSegmentByBatchNo(record.sourceProductionBatchNo);
  const materialCode = record.materialCode || extra.runtimeMaterialCode || currentPlan.materialCode || '-';
  const modelCode = record.modelCode || extra.runtimeModelCode || sourceSegment?.modelCode || currentCutRoundProductModelCode.value || '-';
  const sliceBatchNo = record.productionBatchNo || record.sourceProductionBatchNo || '-';
  const segmentBatchNo = resolveMotherBatchNo(record as Record<string, any>) || '-';
  const sliceSerialNo = sliceBatchNo;
  const startTime = record.startTime || record.recorderTime || now;
  const endTime = record.endTime || record.confirmerTime || record.recorderTime || now;
  const recorderName = record.recorderName || record.confirmerName || currentUserName.value || '-';
  const fallbackFields = [
    { label: '料号', value: materialCode },
    { label: '型号', value: modelCode },
    { label: '分段批号', value: segmentBatchNo },
    { label: '片号', value: sliceSerialNo },
  ];
  const fields = await applyPrintFieldTemplate('CUT_ROUND_TRANSFER', fallbackFields, {
    endTime,
    materialCode,
    modelCode,
    planNo,
    processName,
    recorderName,
    segmentBatchNo,
    sliceBatchNo,
    sliceSerialNo,
    startTime,
  });
  return {
    endTime,
    fields,
    materialCode,
    modelCode,
    planNo,
    processName,
    productionBatchNo: sliceBatchNo,
    qrBottomText: sliceBatchNo,
    qrTopText: sliceBatchNo,
    qrValue: buildTransferTicketQrValue(planNo, sliceBatchNo),
    recorderName,
    startTime,
    ticketId: record.id,
  };
}

async function buildCutRoundTransferTicketPayload(records: MesHcCutRoundConsoleApi.ReportItem[]) {
  const now = buildNowText();
  const items = await Promise.all(records.map((record) => buildCutRoundTransferTicketItem(record, now)));
  const commonPayload = {
    continueOnError: false,
    copies: 1,
    labelGapDots: 24,
    labelGapMm: 3,
    labelHeightMm: 50,
    labelWidthMm: 80,
    offsetXmm: 0,
    offsetYmm: 0,
    printerKey: 'slitting',
    printMode: 'raw',
    rawProtocol: 'pplb',
    title: '工艺流转单',
    waitForSpooler: true,
  };
  if (items.length === 1) {
    return { ...commonPayload, ...items[0] };
  }
  return { ...commonPayload, items };
}

async function sendCutRoundTransferTicketsToPrintAgent(records: MesHcCutRoundConsoleApi.ReportItem[]) {
  const payload = await buildCutRoundTransferTicketPayload(records);
  const endpoint = records.length > 1 ? '/print/transfer-tickets' : '/print/transfer-ticket';
  const response = await fetch(`${CUT_ROUND_PRINT_AGENT_URL}${endpoint}`, {
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

async function markCutRoundRecordsPrinted(records: MesHcCutRoundConsoleApi.ReportItem[], printTime: string) {
  await Promise.all(
    records.map(async (record) => {
      const meta = getRecordPrintMeta(record);
      await markCutRoundConsoleReportPrinted({
        id: record.id as number,
        printCount: meta.count + 1,
        printStatus: '已打印',
        printTime,
      });
    }),
  );
  selectedReportRecordKeys.value = selectedReportRecordKeys.value.filter((key) =>
    !records.some((record) => record.id === key),
  );
}

async function printCutRoundTransferTickets(records: MesHcCutRoundConsoleApi.ReportItem[]) {
  const validRows = records.filter((record) => !!record?.id && !!record.productionBatchNo);
  if (!validRows.length) {
    AModal.info({ title: '没有可打印记录', content: '请选择已有生产批次号的裁切报工记录。' });
    return false;
  }
  try {
    const result = await sendCutRoundTransferTicketsToPrintAgent(validRows);
    await markCutRoundRecordsPrinted(validRows, buildNowText());
    message.success(`裁切流转单已发送到 ${result.printerName || '配置打印机'}，数量：${result.printedCount || validRows.length}。`);
    await loadReports();
    return true;
  } catch (error: any) {
    AModal.warning({
      content: `未能连接或确认本机打印服务：${error?.message || error}。请先启动工序流转单 Python 打印服务。`,
      title: '打印流转单失败',
    });
    await loadReports();
    return false;
  }
}

function buildCutRoundTransferPrintRecord(target: CutRoundTransferPrintTarget): MesHcCutRoundConsoleApi.ReportItem {
  if (target.record) return target.record;
  const { group, segment } = target;
  return {
    actualSizeRule: getInheritedCutRoundSizeRule(segment) || undefined,
    actualSizeSuffix: String(segment.actualSizeSuffix || '')
      .trim()
      .toUpperCase(),
    materialCode: group.materialCode || currentPlan.materialCode,
    modelCode: segment.modelCode || group.modelCode || currentCutRoundProductModelCode.value || currentPlan.modelCode,
    parentProductionBatchNo: group.baseBatchNo,
    planId: Number(currentPlan.planId || 0),
    planNo: group.planNo || currentPlan.planNo,
    planOperationId: Number(currentPlan.planOperationId || 0),
    productionBatchNo: segment.batchNo,
    recorderName: currentUserName.value || '',
    reportStatus: 'DRAFT',
    sourceProductionBatchNo: segment.batchNo,
  };
}

async function printCutRoundTransferTargets(targets: CutRoundTransferPrintTarget[]) {
  const validTargets = targets.filter((target) => isPrintableCutRoundTransferSegment(target.segment));
  if (!validTargets.length) {
    message.warning('当前没有可打印的裁切片号');
    return false;
  }
  const records = validTargets.map(buildCutRoundTransferPrintRecord);
  try {
    const result = await sendCutRoundTransferTicketsToPrintAgent(records);
    const persistedRecords = validTargets.map((target) => target.record).filter(isPrintableTransferReport);
    if (persistedRecords.length) {
      await markCutRoundRecordsPrinted(persistedRecords, buildNowText());
      await loadReports();
    }
    message.success(`裁切流转单已发送到 ${result.printerName || '配置打印机'}，数量：${result.printedCount || validTargets.length}。`);
    return true;
  } catch (error: any) {
    AModal.warning({
      content: `未能连接或确认本机打印服务：${error?.message || error}。请先启动工序流转单 Python 打印服务。`,
      title: '打印流转单失败',
    });
    return false;
  }
}

const exportAllCutRoundWorkOrders = () => void exportCutRoundWorkOrders(reportRecords.value);
const printSelectedCutRoundRecords = () => void printCutRoundTransferTickets(printableReportRecords.value);
const printAllCutRoundRecords = () => void printCutRoundTransferTickets(allPrintableReportRecords.value);

function openTransferPrintSelector() {
  transferPrintSelectionMode.value = true;
  activeBoardTab.value = 'SOURCE';
  visualMaximized.value = true;
  if (!selectableTransferReportCount.value) {
    // message.warning(reportRecords.value.length > 0 ? '当前过滤条件下暂无可打印裁切流转单' : '请先完成裁切报工后再选择打印流转单');
  }
}

function closeTransferPrintSelector() {
  transferPrintSelectionMode.value = false;
  selectedTransferReportIds.value = [];
}

function toggleTransferSegmentSelection(segment: SourceSegment) {
  const key = getCutRoundTransferPrintTargetKey(segment);
  if (!isPrintableCutRoundTransferSegment(segment) || !key) {
    message.warning('当前裁切片号不可打印');
    return;
  }
  selectedTransferReportIds.value = selectedTransferReportIds.value.includes(key)
    ? selectedTransferReportIds.value.filter((id) => id !== key)
    : [...selectedTransferReportIds.value, key];
}

function handleVisualSegmentClick(group: SourceGroup, segment: SourceSegment) {
  if (transferPrintSelectionMode.value) {
    toggleTransferSegmentSelection(segment);
    return;
  }
  openReportViewDialog(group, segment);
}

function clearTransferReportSelection() {
  selectedTransferReportIds.value = [];
}

function selectAllTransferReports() {
  selectedTransferReportIds.value = selectableTransferTargets.value.map((target) => getCutRoundTransferPrintTargetKey(target.segment));
}

function toggleAllTransferReports() {
  if (allTransferReportsSelected.value) {
    const visibleKeys = new Set(selectableTransferTargets.value.map((target) => getCutRoundTransferPrintTargetKey(target.segment)));
    selectedTransferReportIds.value = selectedTransferReportIds.value.filter((id) => !visibleKeys.has(id));
    return;
  }
  selectAllTransferReports();
}

function selectGroupTransferReports(group: SourceGroup) {
  const groupKeys = group.segments
    .filter((segment) => isPrintableCutRoundTransferSegment(segment))
    .map((segment) => getCutRoundTransferPrintTargetKey(segment));
  selectedTransferReportIds.value = Array.from(new Set([...selectedTransferReportIds.value, ...groupKeys]));
}

async function handlePrintSelectedTransferReports() {
  if (!selectedTransferReportCount.value) {
    message.warning('请先选择要打印的裁切片');
    return;
  }
  const printed = await printCutRoundTransferTargets(selectedTransferTargets.value);
  if (printed) closeTransferPrintSelector();
}

function resetInspectionTaskForm() {
  Object.assign(inspectionTaskForm, {
    expectedFinishDate: '',
    priorityLevel: 'NORMAL',
    receiveLocation: '检验室',
    receiverName: '',
    remark: '',
    reportDate: dayjs().format('YYYY-MM-DD'),
    reportProcess: '裁切',
    reportTime: buildNowText(),
    reporterName: currentUserName.value || '',
  });
}

async function openInspectionTaskDialog() {
  await loadReports();
  if (!currentPlan.planId || !currentPlan.planOperationId) {
    message.warning('请先扫码或选择裁切工单');
    return;
  }
  const rows = inspectionEligibleReports.value.filter((record) => isReportInCurrentSegment(record));
  if (!rows.length) {
    message.warning('当前分段下没有已裁切且未提交检验的裁切片');
    return;
  }
  if (warnSampleLocked(rows)) return;
  resetInspectionTaskForm();
  inspectionTaskRows.value = [...rows];
  inspectionTaskVisible.value = true;
}

function removeInspectionTaskRow(record: MesHcCutRoundConsoleApi.ReportItem) {
  inspectionTaskRows.value = inspectionTaskRows.value.filter((item) => item.id !== record.id);
}

async function submitInspectionTask() {
  if (warnSampleLocked(inspectionTaskRows.value)) return;
  if (!currentPlan.planId || !currentPlan.planOperationId) {
    message.warning('请先扫码或选择裁切工单');
    return;
  }
  const rows = inspectionTaskRows.value.filter((record) => isReportInCurrentSegment(record));
  if (rows.length !== inspectionTaskRows.value.length) {
    inspectionTaskRows.value = rows;
    message.warning('已移除非当前分段裁切片，请确认后重新提交');
    return;
  }
  const reportIds = rows.map((record) => record.id).filter(Boolean) as number[];
  if (!reportIds.length) {
    message.warning('请至少保留一张当前分段的裁切片明细');
    return;
  }
  inspectionTaskSubmitting.value = true;
  try {
    await createCutRoundInspectionTask({
      expectedFinishDate: inspectionTaskForm.expectedFinishDate || undefined,
      planId: currentPlan.planId,
      planOperationId: currentPlan.planOperationId,
      priorityLevel: inspectionTaskForm.priorityLevel,
      receiveLocation: inspectionTaskForm.receiveLocation || '检验室',
      receiverName: inspectionTaskForm.receiverName || undefined,
      remark: inspectionTaskForm.remark || undefined,
      reportDate: inspectionTaskForm.reportDate || dayjs().format('YYYY-MM-DD'),
      reportIds,
      reportProcess: inspectionTaskForm.reportProcess || '裁切',
      reportTime: inspectionTaskForm.reportTime || buildNowText(),
      reporterName: inspectionTaskForm.reporterName || currentUserName.value || undefined,
    });
    inspectionTaskVisible.value = false;
    activeBoardTab.value = 'INSPECTION_TASK';
    await Promise.all([loadReports(), loadInspectionTasks()]);
    await loadSourceGroups();
    message.success('裁切产品报检单已提交，裁切片已标记为检测中');
  } finally {
    inspectionTaskSubmitting.value = false;
  }
}

function sanitizeExcelFileName(value?: string) {
  return String(value || '裁切工艺参数点检记录').replaceAll(/[\\/:*?"<>|]/gu, '_');
}

function cutRoundLayoutCell(
  colIndex: number,
  text?: null | number | string,
  options: Partial<MesHcProcessFormApi.LayoutCell> = {},
): MesHcProcessFormApi.LayoutCell {
  return {
    colIndex,
    colSpan: options.colSpan || 1,
    editable: options.editable || false,
    rowSpan: options.rowSpan || 1,
    text: String(text ?? ''),
    ...(options.bindField ? { bindField: options.bindField } : {}),
    ...(options.bindKey ? { bindKey: options.bindKey } : {}),
  };
}

function buildChangeoverExcelLayout(): MesHcProcessFormApi.LayoutExcelReq {
  const title = '裁切工艺参数点检记录';
  const motherBatchNo = changeoverForm.motherSegmentBatchNo || currentSegmentBatchNo.value || '';
  const fileName = sanitizeExcelFileName(
    `${currentPlan.planNo || changeoverForm.currentPlanNo || '裁切'}_${motherBatchNo || '母卷'}_${title}`,
  );
  return {
    columns: [
      { title: '类别', width: 120 },
      { title: '工艺参数项目', width: 220 },
      { title: '标准', width: 220 },
      { title: '实测值', width: 220 },
      { title: '异常备注', width: 240 },
    ],
    detailTitle: '明细项目',
    fileName: `${fileName}.xlsx`,
    headerItems: [
      { editable: false, label: '计划号', value: changeoverForm.currentPlanNo || currentPlan.planNo || '' },
      { editable: false, label: '当前型号', value: changeoverForm.productionModelCode || currentCutRoundProductModelCode.value || '' },
      { editable: false, label: '当前料号', value: changeoverForm.productionMaterialCode || currentPlan.materialCode || '' },
      { editable: false, label: '记录时间', value: displayDateTimeText(changeoverForm.recordTime) },
      { editable: false, label: '母卷批号', value: motherBatchNo },
      { editable: false, label: '记录人', value: changeoverForm.recorderName || currentUserName.value || '' },
      { editable: false, label: '确认人', value: changeoverForm.confirmerName || '' },
      { editable: true, bindField: 'remark', bindKey: 'remark', label: '备注', value: changeoverForm.remark || '' },
    ],
    rows: (changeoverForm.checkItems || []).map((row, index) => ({
      cells: [
        cutRoundLayoutCell(0, row.itemCategory || ''),
        cutRoundLayoutCell(1, row.itemName || ''),
        cutRoundLayoutCell(2, row.standardValue || ''),
        cutRoundLayoutCell(3, row.actualValue || '', {
          bindField: 'actualValue',
          bindKey: String(index),
          editable: true,
        }),
        cutRoundLayoutCell(4, row.abnormalRemark || '', {
          bindField: 'abnormalRemark',
          bindKey: String(index),
          editable: true,
        }),
      ],
    })),
    sheetName: '工艺参数点检',
    title,
    visualMode: 'rough-pass-work',
  };
}

function applyImportedChangeoverExcel(resp: MesHcProcessFormApi.LayoutImportResp) {
  let appliedCount = 0;
  (resp.headerValues || []).forEach((item) => {
    if (item.bindField !== 'remark') return;
    changeoverForm.remark = String(item.value ?? '').trim();
    appliedCount += 1;
  });
  (resp.cellValues || []).forEach((item) => {
    if (!item.bindField) return;
    const rowIndex = Number(item.bindKey);
    if (!Number.isInteger(rowIndex)) return;
    const row = changeoverForm.checkItems?.[rowIndex];
    if (!row) return;
    const value = String(item.value ?? '').trim();
    if (!value && value !== '0') return;
    if (item.bindField === 'checkResult') return;
    if (item.bindField === 'actualValue' || item.bindField === 'abnormalRemark') {
      row[item.bindField] = value;
      appliedCount += 1;
    }
  });
  return appliedCount;
}

function resolveUploadedFileUrl(uploaded: any) {
  return String(uploaded?.url || uploaded?.path || uploaded?.data?.url || uploaded?.data?.path || '');
}

async function attachImportedChangeoverExcel(file: File) {
  const uploaded = (await uploadFile({
    directory: 'mes/cut-round/changeover-import',
    file,
  })) as any;
  const extra = parseRecordExtra(changeoverForm as Record<string, any>) as Record<string, any>;
  const attachment = {
    name: uploaded?.name || uploaded?.data?.name || file.name,
    path: uploaded?.path || uploaded?.data?.path,
    size: uploaded?.size || uploaded?.data?.size || file.size,
    type: uploaded?.type || uploaded?.data?.type || file.type,
    uploadTime: buildNowText(),
    url: resolveUploadedFileUrl(uploaded),
  };
  const previousAttachments = getChangeoverImportAttachments(changeoverForm as Record<string, any>);
  changeoverForm.extraJson = JSON.stringify({
    ...extra,
    importAttachment: attachment,
    importAttachments: [attachment, ...previousAttachments].slice(0, 10),
  });
}

async function handleChangeoverExportExcel() {
  if (!changeoverForm.checkItems?.length) {
    message.warning('当前工艺参数点检明细为空，不能导出');
    return;
  }
  changeoverExcelLoading.value = true;
  try {
    const layout = buildChangeoverExcelLayout();
    const data = await exportProcessFormRecordLayout(layout);
    downloadFileFromBlobPart({
      fileName: layout.fileName || '裁切工艺参数点检记录.xlsx',
      source: data,
    });
  } finally {
    changeoverExcelLoading.value = false;
  }
}

function triggerChangeoverImportExcel() {
  if (isChangeoverFormConfirmed.value) {
    message.warning('已确认记录不能导入');
    return;
  }
  changeoverExcelImportInputRef.value?.click();
}

async function handleChangeoverExcelImportChange(event: Event) {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  input.value = '';
  if (!file) return;
  if (isChangeoverFormConfirmed.value) {
    message.warning('已确认记录不能导入');
    return;
  }
  if (!/\.xls[xm]?$/iu.test(file.name)) {
    message.warning('请选择 Excel 文件');
    return;
  }
  if (!changeoverForm.checkItems?.length) {
    message.warning('当前工艺参数点检明细为空，不能导入');
    return;
  }
  changeoverExcelLoading.value = true;
  try {
    const resp = await importProcessFormRecordLayout(file, buildChangeoverExcelLayout());
    const appliedCount = applyImportedChangeoverExcel(resp);
    await attachImportedChangeoverExcel(file);
    AModal.success({
      content: `导入成功，已回填 ${appliedCount || resp.totalCellCount || 0} 个有值单元格。请核实后点击保存。`,
      title: '导入完成',
    });
  } finally {
    changeoverExcelLoading.value = false;
  }
}

function resetChangeoverForm(record?: MesHcCutRoundConsoleApi.ChangeoverInspection) {
  const now = buildNowText();
  const segmentBatchNo = getCurrentSegmentBatchNo();
  const savedCheckItems = parseChangeoverCheckItems(record);
  const extra = parseRecordExtra((record || {}) as Record<string, any>) as Record<string, any>;
  const confirmed = isChangeoverInspectionConfirmed(record);
  const safeRecordTime = normalizeRecordTimeText(record?.recordTime || record?.submitTime, now);
  const safeSubmitTime = normalizeRecordTimeText(record?.submitTime || record?.recordTime, safeRecordTime);
  Object.assign(changeoverForm, {
    checkItems: savedCheckItems.length
      ? savedCheckItems.map((item, index) => normalizeCheckItem(item, index))
      : ensureCutRoundChangeoverItems(checkTemplate.value).map((item) => ({
          ...item,
          abnormalRemark: '',
          actualValue: getDefaultCheckActualValue(item),
          checkResult: 'OK',
        })),
    currentPlanNo: record?.currentPlanNo || currentPlan.planNo,
    detailItemsJson: record?.detailItemsJson || '',
    extraJson: record?.extraJson || '',
    feedbackRemark: record?.feedbackRemark || '',
    feedbackResult: record?.feedbackResult || '',
    feedbackTime: record?.feedbackTime || '',
    headerDataJson: record?.headerDataJson || '',
    id: record?.id,
    inspectionStatus: record?.inspectionStatus || 'DRAFT',
    motherSegmentBatchNo: record?.motherSegmentBatchNo || segmentBatchNo,
    planId: currentPlan.planId || record?.planId || 0,
    planNo: currentPlan.planNo || record?.planNo || '',
    planOperationId: currentPlan.planOperationId || record?.planOperationId || 0,
    previousModelCode: record?.previousModelCode || '',
    productionMaterialCode: record?.productionMaterialCode || currentPlan.materialCode || '',
    productionModelCode: record?.productionModelCode || currentCutRoundProductModelCode.value || '',
    recordTime: record?.id ? safeRecordTime : now,
    recorderName: record?.recorderName || extra.recorderName || currentUserName.value || 'admin',
    remark: record?.remark || '',
    sourceSlittingSliceId: undefined,
    submitTime: record?.id ? safeSubmitTime : now,
    confirmerName: confirmed ? ((record as Record<string, any> | undefined)?.confirmerName || extra.confirmerName || '') : '',
    confirmerTime: confirmed ? normalizeRecordTimeText((record as Record<string, any> | undefined)?.confirmerTime || extra.confirmerTime, '') : '',
  });
}

function findPendingChangeoverInspection() {
  const currentMother = getCurrentChangeoverMotherBatchNo();
  const rows = changeoverInspections.value.filter((record) => !isChangeoverInspectionConfirmed(record));
  return rows.find((record) => !currentMother || getLatestChangeoverMotherBatchNo(record) === currentMother) || rows[0];
}

async function openChangeoverInspectionScan(_initialSliceNo: string | Event = '') {
  if (!currentPlan.planNo) {
    AModal.warning({ title: '请先加载裁切计划', content: '工艺参数点检需要先从待加工列表开工并带出当前裁切计划。' });
    return;
  }
  activeBoardTab.value = 'CHANGEOVER';
  await loadChangeoverInspections();
  const pendingRecord = findPendingChangeoverInspection();
  resetChangeoverForm(pendingRecord);
  changeoverVisible.value = true;
}

async function submitChangeoverInspection() {
  if (isChangeoverFormConfirmed.value) {
    message.warning('已确认记录不能修改');
    return;
  }
  if (changeoverSubmitting.value) return;
  const motherSegmentBatchNo = getCurrentSegmentBatchNo() || changeoverForm.motherSegmentBatchNo;
  if (!motherSegmentBatchNo) {
    message.warning('未找到当前母卷批号，请先从待加工列表开工并加载裁切计划');
    return;
  }
  requestChangeoverInspectionAuth('save');
}

function requestChangeoverInspectionAuth(action: 'confirm' | 'save') {
  pendingChangeoverAuthAction.value = action;
  changeoverAuthAction.value = action === 'confirm' ? '确认工艺参数点检' : '保存工艺参数点检';
  changeoverAuthVisible.value = true;
}

async function handleChangeoverAuthSuccess(authPayload: any) {
  const action = pendingChangeoverAuthAction.value;
  pendingChangeoverAuthAction.value = undefined;
  changeoverAuthVisible.value = false;
  if (!action) {
    message.warning('未找到待认证的工艺参数点检操作，请重新点击保存或确认');
    return;
  }
  if (action === 'save') {
    if (changeoverSubmitting.value) return;
    changeoverSubmitting.value = true;
    try {
      await persistChangeoverInspection(false, authPayload);
      changeoverVisible.value = false;
      await loadChangeoverInspections();
      message.success('工艺参数点检记录已保存');
    } finally {
      changeoverSubmitting.value = false;
    }
    return;
  }
  if (changeoverConfirming.value) return;
  changeoverConfirming.value = true;
  try {
    await persistChangeoverInspection(true, authPayload);
    changeoverVisible.value = false;
    await loadChangeoverInspections();
    message.success('工艺参数点检确认成功');
  } finally {
    changeoverConfirming.value = false;
  }
}

function handleChangeoverAuthCancel() {
  pendingChangeoverAuthAction.value = undefined;
  changeoverAuthVisible.value = false;
}

async function persistChangeoverInspection(confirmRecord: boolean, authPayload?: any) {
  const now = buildNowText();
  const motherSegmentBatchNo = getCurrentSegmentBatchNo() || changeoverForm.motherSegmentBatchNo;
  const checkItems = (changeoverForm.checkItems || []).map((item, index) => ({ ...item, sortNo: item.sortNo || index + 1 }));
  const authOperatorName = authPayload ? resolveDailyRecordAuthOperator(authPayload) : '';
  const recorderName = confirmRecord
    ? changeoverForm.recorderName || authOperatorName || currentUserName.value || 'admin'
    : authOperatorName || changeoverForm.recorderName || currentUserName.value || 'admin';
  const recordTime = changeoverForm.recordTime || now;
  const submitTime = now;
  const extra = {
    ...(parseRecordExtra(changeoverForm as Record<string, any>) as Record<string, any>),
    recorderName,
  };
  let confirmerName = changeoverForm.confirmerName || '';
  let confirmerTime = changeoverForm.confirmerTime || '';
  if (confirmRecord) {
    confirmerName = authOperatorName || currentUserName.value || recorderName;
    confirmerTime = now;
    Object.assign(extra, {
      confirmerName,
      confirmerTime,
      docStatus: 'CONFIRMED',
      recordStatus: 'CONFIRMED',
    });
  }
  const savedId = await saveCutRoundChangeover({
    ...changeoverForm,
    checkItems,
    currentPlanNo: currentPlan.planNo,
    detailItemsJson: JSON.stringify(checkItems),
    extraJson: JSON.stringify(extra),
    inspectionStatus: confirmRecord ? 'CONFIRMED' : 'RECORDED',
    motherSegmentBatchNo,
    planId: currentPlan.planId || changeoverForm.planId,
    planNo: currentPlan.planNo,
    planOperationId: currentPlan.planOperationId || changeoverForm.planOperationId,
    pressSlotSliceNo: undefined,
    productionMaterialCode: currentPlan.materialCode || changeoverForm.productionMaterialCode,
    productionModelCode: currentCutRoundProductModelCode.value || changeoverForm.productionModelCode,
    recordTime,
    recorderName,
    sourceSlittingSliceId: undefined,
    submitTime,
    confirmerName,
    confirmerTime,
  });
  changeoverForm.id = Number(savedId || changeoverForm.id || 0);
  changeoverForm.inspectionStatus = confirmRecord ? 'CONFIRMED' : 'RECORDED';
  changeoverForm.recordTime = recordTime;
  changeoverForm.submitTime = submitTime;
  changeoverForm.confirmerName = confirmerName;
  changeoverForm.confirmerTime = confirmerTime;
  changeoverForm.extraJson = JSON.stringify(extra);
  return savedId;
}

async function confirmChangeoverInspection() {
  if (isChangeoverFormConfirmed.value) {
    message.info('当前工艺参数点检已确认');
    return;
  }
  if (!(getCurrentSegmentBatchNo() || changeoverForm.motherSegmentBatchNo)) {
    message.warning('未找到当前母卷批号，请先从待加工列表开工并加载裁切计划');
    return;
  }
  if (!changeoverForm.checkItems?.length) {
    message.warning('工艺参数点检明细不能为空');
    return;
  }
  requestChangeoverInspectionAuth('confirm');
}

function viewChangeoverInspection(record: MesHcCutRoundConsoleApi.ChangeoverInspection | Record<string, any>) {
  resetChangeoverForm(record as MesHcCutRoundConsoleApi.ChangeoverInspection);
  changeoverVisible.value = true;
}

function deleteChangeoverInspection(record: MesHcCutRoundConsoleApi.ChangeoverInspection | Record<string, any>) {
  const recordId = Number(record?.id || 0);
  const planOperationId = Number(record?.planOperationId || currentPlan.planOperationId || 0);
  if (!recordId || !planOperationId) {
    message.warning('未找到要删除的工艺参数点检记录');
    return;
  }
  AModal.confirm({
    title: '删除工艺参数点检记录',
    content: `确认删除母卷批号 ${record?.motherSegmentBatchNo || '-'} 的工艺参数点检记录？删除后列表将不再显示该记录。`,
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    zIndex: 3000,
    async onOk() {
      await deleteCutRoundChangeover(recordId, planOperationId);
      if (Number(changeoverForm.id || 0) === recordId) {
        changeoverVisible.value = false;
        resetChangeoverForm();
      }
      await loadChangeoverInspections();
      message.success('工艺参数点检记录已删除');
    },
  });
}

function isChangeoverInspectionReady() {
  if (!currentPlan.planOperationId || !currentPlan.planNo) return true;
  const latest = latestChangeoverInspection.value;
  if (!latest?.id) return false;
  const currentMother = getCurrentChangeoverMotherBatchNo();
  const latestMother = getLatestChangeoverMotherBatchNo(latest);
  return Boolean(currentMother && latestMother && latestMother === currentMother);
}

function openRecordConfirm(
  record?: MesHcCutRoundConsoleApi.ReportItem | Record<string, any>,
  initialScannedBatchNo = '',
) {
  activeRecord.value = (record as MesHcCutRoundConsoleApi.ReportItem) || null;
  const extra = parseRecordExtra(record || {});
  const sourceSegment = findSourceSegment((record as MesHcCutRoundConsoleApi.ReportItem | undefined)?.sourceProductionBatchNo || initialScannedBatchNo);
  recordConfirmForm.sourceActualSizeRule = getInheritedCutRoundSizeRule(sourceSegment);
  recordConfirmForm.actualSizeRule = recordConfirmForm.sourceActualSizeRule || getReportActualSizeRule(record);
  recordConfirmForm.error = '';
  recordConfirmForm.glueBoardModel =
    String(extra.glueBoardModel || reportForm.glueBoardModel || getDefaultGlueBoardModel(record?.modelCode || currentCutRoundProductModelCode.value));
  recordConfirmForm.message = '';
  recordConfirmForm.reportType = getDefaultCutRoundReportType();
  recordConfirmForm.scannedBatchNo = initialScannedBatchNo;
  recordConfirmVisible.value = true;
  focusRecordConfirmScanInput();
}

function openSelectedRecordConfirm() {
  if (!sourceGroups.value.some((group) => group.segments.length > 0)) {
    AModal.info({ title: '暂无来源片', content: '请先扫码计划号，系统会按已确认粘胶2片号加载裁切片。' });
    return;
  }
  if (isCurrentTaskReadonly.value) {
    showReadonlyTaskWarning('扫码确认');
    return;
  }
  if (!ensureDailyPreparationReady()) {
    return;
  }
  if (!isChangeoverInspectionReady()) {
    showChangeoverInspectionRequiredWarning();
    return;
  }
  if (!ensureCutRoundConsumablesReady('扫码确认')) {
    return;
  }
  openRecordConfirm();
}

function showDailyPreparationRequiredWarning() {
  showSinglePlanScanWarning({
    okText: '查看点检清洁',
    title: '请先填写今日点检/清洁记录',
    content: '裁切扫码确认前需要先保存今日设备清洁点检记录。',
    onOk: () => {
      void openDailyRecordList();
    },
  });
}

function ensureDailyPreparationReady() {
  if (dailyPreparationReady.value) return true;
  showDailyPreparationRequiredWarning();
  return false;
}

async function confirmRecordScan() {
  const scannedBatchNo = resolveTransferTicketQrBusinessNo(normalizeConfirmScanCode(recordConfirmForm.scannedBatchNo));
  if (!scannedBatchNo) {
    recordConfirmForm.error = '请扫码或输入粘胶2已确认片号。';
    recordConfirmForm.message = '';
    return;
  }
  if (!ensureDailyPreparationReady()) {
    recordConfirmVisible.value = false;
    return;
  }
  recordConfirmForm.reportType = 'PRODUCT';
  if (!isChangeoverInspectionReady()) {
    recordConfirmVisible.value = false;
    showChangeoverInspectionRequiredWarning(scannedBatchNo);
    return;
  }
  if (!ensureCutRoundConsumablesReady('扫码确认')) {
    recordConfirmVisible.value = false;
    return;
  }
  const matched = findSourceSegmentWithGroup(scannedBatchNo);
  if (!matched) {
    recordConfirmForm.error = '未在当前裁切片中找到该片号，请检查计划或重新扫码。';
    recordConfirmForm.message = '';
    return;
  }
  recordConfirmForm.sourceActualSizeRule = getInheritedCutRoundSizeRule(matched.segment);
  if (recordConfirmForm.sourceActualSizeRule) {
    recordConfirmForm.actualSizeRule = recordConfirmForm.sourceActualSizeRule;
  }
  const existedRecord = findReportBySegment(matched.segment);
  if (isSourceGroupCompleted(matched.group)) {
    if (existedRecord) {
      recordConfirmVisible.value = false;
      message.info('当前裁切分段已完工，已打开登记明细查看。');
      await openReportDialog(matched.group, matched.segment, getCutRoundSegmentReportType(matched.segment), scannedBatchNo, true);
      return;
    }
    recordConfirmForm.error = '当前裁切分段已完工，仅可查看，不能新建或扫码确认。';
    recordConfirmForm.message = '';
    return;
  }
  if (existedRecord && isCutRoundRecordConfirmed(existedRecord.reportStatus)) {
    recordConfirmVisible.value = false;
    message.info('该裁切片已扫码确认，已打开登记明细查看。');
    await openReportDialog(matched.group, matched.segment, getCutRoundSegmentReportType(matched.segment), scannedBatchNo, true);
    return;
  }
  if (!existedRecord?.id) {
    const actualSizeRule = normalizeSizeRule(recordConfirmForm.sourceActualSizeRule);
    if (!actualSizeRule) {
      recordConfirmForm.error = '粘胶2已确认来源未携带实际尺寸，不能裁切扫码确认。';
      recordConfirmForm.message = '';
      return;
    }
    activeRecord.value = null;
    recordConfirmVisible.value = false;
    await openReportDialog(matched.group, matched.segment, recordConfirmForm.reportType, scannedBatchNo);
    return;
  }
  // 每次扫码均以当前片号重新定位草稿，不能沿用上一次已确认后的待处理记录。
  activeRecord.value = existedRecord;
  recordConfirmForm.actualSizeRule = normalizeSizeRule(
    recordConfirmForm.sourceActualSizeRule || getReportActualSizeRule(existedRecord),
  );
  const actualSizeRule = normalizeSizeRule(recordConfirmForm.sourceActualSizeRule || getReportActualSizeRule(activeRecord.value));
  if (!actualSizeRule) {
    recordConfirmForm.error = '粘胶2已确认来源未携带实际尺寸，不能裁切扫码确认。';
    recordConfirmForm.message = '';
    return;
  }
  try {
    const finalBatchNo = recordConfirmForm.sourceActualSizeRule
      ? scannedBatchNo
      : normalizeCutRoundProductionBatchNo(scannedBatchNo, actualSizeRule);
    await confirmCutRoundConsoleReport({
      actualSizeRule,
      confirmerName: currentUserName.value || 'admin',
      confirmerTime: buildNowText(),
      glueBoardModel: undefined,
      id: activeRecord.value.id,
      scannedBatchNo: finalBatchNo || scannedBatchNo,
    });
    const confirmedRecordId = activeRecord.value.id;
    selectedReportRecordKeys.value = selectedReportRecordKeys.value.filter((key) => key !== confirmedRecordId);
    await loadReports();
    await loadSourceGroups();
    await loadGlueBoardUsage();
    activeRecord.value = null;
    recordConfirmForm.sourceActualSizeRule = '';
    recordConfirmForm.actualSizeRule = '';
    recordConfirmForm.scannedBatchNo = '';
    recordConfirmForm.error = '';
    recordConfirmForm.message = '已确认，可继续扫描任意待确认裁切片号。';
    message.success('裁切报工记录已扫码确认，可继续扫描下一张流转单');
    focusRecordConfirmScanInput();
  } catch (error: any) {
    recordConfirmForm.error = error?.message || '扫码批次号与裁切片号不一致。';
    recordConfirmForm.message = '';
  }
}

async function refreshWorkbench() {
  if (!currentPlan.planId || !currentPlan.planOperationId) {
    message.warning('请先扫描或选择裁切计划，再刷新工作台');
    return;
  }
  workbenchRefreshing.value = true;
  try {
    await loadReports();
    await loadSourceGroups();
    // message.success('工作台已刷新');
  } finally {
    workbenchRefreshing.value = false;
  }
}

async function loadSourceGroups(expectedBatchNo?: string) {
  if (!currentPlan.planId || !currentPlan.planOperationId) {
    sourceGroups.value = [];
    return;
  }
  const loadSeq = ++sourceGroupsLoadSeq;
  const planId = currentPlan.planId;
  const planOperationId = currentPlan.planOperationId;
  const previousTemplateModelCode = resolveCutRoundTemplateModelCode();
  const requestedBatchNo = expectedBatchNo || currentSegmentBatchNo.value || currentPlan.sourceBatchNo || currentPlan.batchNo || '';
  const selectedSourceBatchNo = resolveMotherBatchNo({
    batchNo: requestedBatchNo,
    parentProductionBatchNo: requestedBatchNo,
    productionBatchNo: requestedBatchNo,
    sourceBatchNo: requestedBatchNo,
    sourceProductionBatchNo: requestedBatchNo,
  }) || stripSegmentMark(requestedBatchNo);
  const rows = normalizeRows(await getCutRoundConsoleSourceList(
    planId,
    planOperationId,
    selectedSourceBatchNo,
  ));
  if (loadSeq !== sourceGroupsLoadSeq || planId !== currentPlan.planId || planOperationId !== currentPlan.planOperationId) return;
  buildSourceGroupsFromSources(rows);
  const selectedGroup = selectedSourceBatchNo
    ? sourceGroups.value.find((group) => isSameText(group.baseBatchNo, selectedSourceBatchNo))
    : sourceGroups.value[0];
  const nextSegmentBatchNo = selectedGroup?.baseBatchNo || selectedSourceBatchNo;
  currentPlan.sourceBatchNo = nextSegmentBatchNo;
  currentPlan.sourceProductionBatchNo = selectedGroup?.segments?.[0]?.batchNo
    || (isSameText(resolveMotherBatchNo({ sourceProductionBatchNo: currentPlan.sourceProductionBatchNo }), nextSegmentBatchNo)
      ? currentPlan.sourceProductionBatchNo
      : nextSegmentBatchNo);
  currentPlan.batchNo = nextSegmentBatchNo;
  currentPlan.availableSourceLength = selectedGroup?.totalAvailableLength
    ?? (!selectedSourceBatchNo && sourceGroups.value.length
      ? sourceGroups.value.reduce((sum, group) => sum + group.totalAvailableLength, 0)
      : 0);
  if (resolveCutRoundTemplateModelCode() !== previousTemplateModelCode) {
    await loadCheckTemplate();
  }
}

async function openExistingCutRoundReportDetail(
  record: MesHcCutRoundConsoleApi.ReportItem | Record<string, any>,
  scannedBatchNo?: string,
) {
  const sourceBatchNo = String(record.sourceProductionBatchNo || '').trim();
  const expectedBatchNo = resolveMotherBatchNo(record as Record<string, any>)
    || resolveMotherBatchNo({
      productionBatchNo: scannedBatchNo,
      sourceProductionBatchNo: scannedBatchNo || sourceBatchNo,
    });
  if (expectedBatchNo && !findSourceGroup(expectedBatchNo)) {
    await loadSourceGroups(expectedBatchNo);
  }
  const matched = findSourceSegmentWithGroup(sourceBatchNo)
    || findSourceSegmentWithGroup(scannedBatchNo)
    || findSourceSegmentWithGroup(record.productionBatchNo);
  if (!matched) {
    showSinglePlanScanWarning({
      content: `已找到片号 ${scannedBatchNo || record.productionBatchNo || sourceBatchNo || '-'} 的裁切报工记录，但当前工作台未加载到对应来源片，请刷新后重新扫码。`,
      okText: '知道了',
      onOk: focusPlanScanInput,
      title: '未加载到裁切片来源',
    });
    return;
  }
  currentPlan.sourceBatchNo = matched.group.baseBatchNo;
  currentPlan.sourceProductionBatchNo = matched.segment.batchNo || matched.group.baseBatchNo;
  currentPlan.batchNo = matched.group.baseBatchNo;
  currentPlan.availableSourceLength = matched.group.totalAvailableLength;
  await openReportDialog(
    matched.group,
    matched.segment,
    getCutRoundSegmentReportType(matched.segment),
    sourceBatchNo || String(scannedBatchNo || record.productionBatchNo || matched.segment.batchNo),
    true,
  );
}

async function consumePlanScan(silent = false) {
  if (planScanInFlight) return;
  clearPlanScanTimer();
  const rawScanValue = scanPlanNo.value;
  const scannerSliceBatchNo = pendingScannerSliceBatchNo || normalizePlanScanSliceBatchNo(rawScanValue);
  const scannerInput = hasPlanScanDelimiter(rawScanValue) || !!pendingScannerPlanNo || !!scannerSliceBatchNo;
  const planNo = syncPlanScanNo(scanPlanNo.value);
  if (!planNo) {
    if (!silent) {
      message.warning('请先扫码或输入计划号');
      focusPlanScanInput();
    }
    return;
  }
  planScanInFlight = true;
  boardLoading.value = true;
  try {
    const rows = normalizeRows(await getCutRoundConsoleTaskList({
      equipmentCode: selectedBoardEquipmentCode.value || undefined,
      equipmentId: selectedBoardEquipmentId.value,
      taskKeyword: planNo,
      taskStatus: 'ALL',
    }));
    const existingReport = scannerSliceBatchNo
      ? await findCutRoundReportByBatchNo(scannerSliceBatchNo, planNo)
      : null;
    let scannerSourceMotherBatchNo = '';
    let scannerSourcePlanNo = '';
    if (scannerSliceBatchNo && !existingReport) {
      try {
        const sourceScanBatchNo = stripCutRoundSizeSuffix(scannerSliceBatchNo);
        const source = await scanCutRoundConsoleSource(sourceScanBatchNo);
        scannerSourcePlanNo = String(source.grindingPlanNo || (source as Record<string, any>).planNo || '').trim();
        if (scannerSourcePlanNo && !isSameText(scannerSourcePlanNo, planNo)) {
          clearPlanScannerState();
          showSinglePlanScanWarning({
            content: `片号 ${scannerSliceBatchNo} 属于计划 ${scannerSourcePlanNo}，与扫码计划 ${planNo} 不一致，扫码计划框已清空，请确认后重新扫码。`,
            okText: '知道了',
            onOk: focusPlanScanInput,
            title: '扫码计划与片号不一致',
          });
          return;
        }
        scannerSourceMotherBatchNo = resolveCutRoundSourceMotherBatchNo(source, sourceScanBatchNo);
      } catch (error: any) {
        clearPlanScannerState();
        showSinglePlanScanWarning({
          content: error?.message || `未找到片号 ${scannerSliceBatchNo} 对应的已确认粘胶2来源，扫码计划框已清空，请确认后重新扫码。`,
          okText: '知道了',
          onOk: focusPlanScanInput,
          title: '扫码片号未找到',
        });
        return;
      }
    }
    let task = (existingReport
      ? findTaskByPlanAndMother(rows, existingReport.planNo || planNo, resolveMotherBatchNo(existingReport as Record<string, any>))
      : findTaskByPlanAndMother(rows, planNo, scannerSourceMotherBatchNo)) as MesHcCutRoundConsoleApi.TaskItem | undefined;
    if (!task?.planOperationId && existingReport) {
      task = findTaskByPlanAndMother(rows, existingReport.planNo || planNo) as MesHcCutRoundConsoleApi.TaskItem | undefined;
    }
    if (!task?.planOperationId && scannerSliceBatchNo && scannerSourceMotherBatchNo) {
      task = findTaskByPlanAndMother(rows, planNo) as MesHcCutRoundConsoleApi.TaskItem | undefined;
    }
    if (!task?.planOperationId) {
      if (!scannerInput) {
        resetPlan();
        await loadDailyRecords();
      } else {
        clearPlanScannerState();
      }
      if (!silent || scannerInput) {
        showSinglePlanScanWarning({
          content: scannerSourceMotherBatchNo
            ? `未找到计划号 ${planNo} 且分段批次 ${scannerSourceMotherBatchNo} 同时匹配的裁切工单，扫码计划框已清空，请确认后重新扫码。`
            : `未找到计划号 ${planNo} 对应的裁切工单，请确认计划号或工序任务状态。`,
          okText: '知道了',
          onOk: focusPlanScanInput,
          title: '扫码计划未找到',
        });
      }
      return;
    }
    const taskStatus = normalizeWorkOrderStatus(task.status);
    if (scannerSliceBatchNo && !existingReport && taskStatus === 'FINISHED') {
      clearPlanScannerState();
      showSinglePlanScanWarning({
        content: `计划号 ${planNo} 对应裁切工单已完工，片号 ${scannerSliceBatchNo} 不能再新建裁切报工。`,
        okText: '知道了',
        onOk: focusPlanScanInput,
        title: '裁切工单已完工',
      });
      return;
    }
    const taskSourceBatchNo = scannerSourceMotherBatchNo || resolveMotherBatchNo(task as Record<string, any>);
    if (
      taskStatus !== 'FINISHED' &&
      !(await ensureSampleAbnormalUnlocked(
        buildSegmentChainSampleLockCandidates({
          motherBatchNo: taskSourceBatchNo,
          segmentBatchNo: taskSourceBatchNo,
        }),
        '扫码加载裁切',
      ))
    ) {
      return;
    }
    const equipmentReady = await ensureTaskEquipment(task);
    if (equipmentReady === null) {
      clearPlanScannerState();
      focusPlanScanInput();
      return;
    }
    applyTask(task);
    await loadCheckTemplate();
    lastAutoScannedPlanNo.value = task.planNo || planNo;
    scanPlanNo.value = task.planNo || planNo;
    if (
      scannerSliceBatchNo
      && !existingReport
      && !['PAUSED', 'CANCELLED'].includes(taskStatus)
      && !(await startCutRoundWorkOrderIfNeeded(task, taskStatus))
    ) {
      clearPlanScannerState();
      focusPlanScanInput();
      return;
    }
    await Promise.all([loadDailyRecords(), loadReports(), loadGlueBoardUsage()]);
    await loadSourceGroups(
      existingReport
        ? resolveMotherBatchNo(existingReport as Record<string, any>)
        : scannerSourceMotherBatchNo || resolveMotherBatchNo(task as Record<string, any>),
    );
    await loadChangeoverInspections();
    await loadInspectionTasks();
    await loadIntermediateRecord();
    if (taskStatus === 'PAUSED' || taskStatus === 'CANCELLED') {
      clearPlanScannerState();
      return;
    }
    if (existingReport) {
      clearPlanScannerState();
      await openExistingCutRoundReportDetail(existingReport, scannerSliceBatchNo);
      return;
    }
    if (scannerSliceBatchNo) {
      clearPlanScannerState();
      const matchedSource = findSourceSegmentWithGroup(scannerSliceBatchNo)
        || findSourceSegmentWithGroup(stripCutRoundSizeSuffix(scannerSliceBatchNo));
      if (!matchedSource) {
        showSinglePlanScanWarning({
          content: `片号 ${scannerSliceBatchNo} 已确认且属于计划 ${planNo}，但当前裁切工作台未加载到该片号来源，请刷新后重新扫码。`,
          okText: '知道了',
          onOk: focusPlanScanInput,
          title: '未加载到片号来源',
        });
        return;
      }
      currentPlan.sourceBatchNo = matchedSource.group.baseBatchNo;
      currentPlan.sourceProductionBatchNo = matchedSource.segment.batchNo || matchedSource.group.baseBatchNo;
      currentPlan.batchNo = matchedSource.group.baseBatchNo;
      currentPlan.availableSourceLength = matchedSource.group.totalAvailableLength;
      if (!ensureDailyPreparationReady()) return;
      if (!isChangeoverInspectionReady()) {
        showChangeoverInspectionRequiredWarning(scannerSliceBatchNo);
        return;
      }
      if (!ensureCutRoundConsumablesReady('扫码确认')) return;
      openRecordConfirm(undefined, scannerSliceBatchNo);
    } else {
      focusPlanScanInput();
    }
    // if (!silent) message.success('已带出裁切计划、工作准备和可加工来源');
  } finally {
    planScanInFlight = false;
    boardLoading.value = false;
  }
}

const isGlobalScannerCandidate = (value: string) => {
  const text = value.trim();
  if (!text) return false;
  if (recordConfirmVisible.value) return text.length >= 3;
  return hasPlanScanDelimiter(text) || normalizePlanScanNo(text).length >= PLAN_SCAN_MIN_LENGTH;
};

const routeGlobalScannerInput = (value: string) => {
  if (recordConfirmVisible.value) {
    recordConfirmForm.scannedBatchNo = value.trim();
    focusRecordConfirmScanInput();
    nextTick(() => void confirmRecordScan());
    return;
  }
  handlePlanScanInput(value);
  focusPlanScanInput();
  nextTick(() => void consumePlanScan(true));
};

const flushGlobalScannerBuffer = () => {
  const scanned = globalScannerBuffer.trim();
  clearGlobalScannerBuffer();
  if (!isGlobalScannerCandidate(scanned)) return false;
  routeGlobalScannerInput(scanned);
  return true;
};

const scheduleGlobalScannerFlush = () => {
  if (globalScannerTimer) clearTimeout(globalScannerTimer);
  globalScannerTimer = setTimeout(() => {
    void flushGlobalScannerBuffer();
  }, GLOBAL_SCANNER_IDLE_FLUSH_MS);
};

const handleGlobalScannerKeydown = (event: KeyboardEvent) => {
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
};

const attachGlobalScannerListener = () => {
  if (globalScannerListenerAttached) return;
  window.addEventListener('keydown', handleGlobalScannerKeydown, true);
  globalScannerListenerAttached = true;
};

const detachGlobalScannerListener = () => {
  if (!globalScannerListenerAttached) return;
  window.removeEventListener('keydown', handleGlobalScannerKeydown, true);
  globalScannerListenerAttached = false;
  clearGlobalScannerBuffer();
};

function getDailyRecordStatusMeta(status: DailyRecordRow['status']) {
  if (status === 'COMPLETED') return { color: 'green', text: '已确认' };
  if (status === 'FILLED') return { color: 'blue', text: '待确认' };
  return { color: 'orange', text: '未填写' };
}

function canFillDailyRecord(record: DailyRecordRow) {
  return record.canFill === true;
}

function canConfirmDailyRecord(record: DailyRecordRow) {
  return record.canConfirm === true;
}

function canViewDailyRecord(record: DailyRecordRow) {
  return record.canView !== false;
}

async function openDailyRecordList() {
  if (!selectedBoardEquipmentId.value && !selectedBoardEquipmentCode.value) {
    openDailyRecordAfterEquipmentSelected.value = true;
    message.warning('请先选择裁切设备，再填写清洁点检');
    await openEquipmentSelect();
    return;
  }
  await loadDailyRecords();
  dailyRecordListVisible.value = true;
}

function openDailyRecord(row: DailyRecordRow, mode: DailyRecordMode) {
  if (mode === 'edit' && !canFillDailyRecord(row)) {
    message.warning('后台状态不允许填写当前记录');
    return;
  }
  if (mode === 'confirm' && !canConfirmDailyRecord(row)) {
    message.warning('后台状态不允许确认当前记录，请先填写保存');
    return;
  }
  if (mode === 'view' && !canViewDailyRecord(row)) {
    message.warning('后台状态不允许查看当前记录');
    return;
  }
  dailyRecordKey.value = row.key;
  dailyRecordMode.value = mode;
  dailyRecordVisible.value = true;
}

function getDetailCategoryRowSpan(rows: WorkPrepareDetail[], index: number) {
  const current = rows[index];
  if (!current?.category) return 1;
  if (index > 0 && rows[index - 1]?.category === current.category) return 0;
  let span = 1;
  for (let i = index + 1; i < rows.length; i += 1) {
    if (rows[i]?.category !== current.category) break;
    span += 1;
  }
  return span;
}

function focusDailyCell(rowIndex: number, field: 'remark' | 'value') {
  nextTick(() => {
    const input = document.querySelector<HTMLInputElement>(`[data-daily-cell="${rowIndex}-${field}"] input`);
    input?.focus();
    input?.select?.();
  });
}

function handleDailyCellKeydown(event: KeyboardEvent, rowIndex: number, field: 'remark' | 'value') {
  const rows = selectedDailyRecord.value?.details || [];
  if (!rows.length) return;
  let nextRow = rowIndex;
  let nextField = field;
  if (event.key === 'Enter' || event.key === 'ArrowRight') {
    if (field === 'value') {
      nextField = 'remark';
    } else {
      nextField = 'value';
      nextRow = Math.min(rows.length - 1, rowIndex + 1);
    }
  } else if (event.key === 'ArrowLeft') {
    if (field === 'remark') {
      nextField = 'value';
    } else {
      nextRow = Math.max(0, rowIndex - 1);
      nextField = 'remark';
    }
  } else if (event.key === 'ArrowDown') {
    nextRow = Math.min(rows.length - 1, rowIndex + 1);
  } else if (event.key === 'ArrowUp') {
    nextRow = Math.max(0, rowIndex - 1);
  } else {
    return;
  }
  event.preventDefault();
  focusDailyCell(nextRow, nextField);
}

function handleReportCheckKeydown(event: KeyboardEvent, rowIndex?: number, field: 'abnormalRemark' | 'actualValue' = 'actualValue') {
  if (typeof rowIndex === 'number') {
    const keyMap: Record<string, 'down' | 'left' | 'next' | 'right' | 'up'> = {
      ArrowDown: 'down',
      ArrowLeft: 'left',
      ArrowRight: 'right',
      ArrowUp: 'up',
      Enter: 'next',
    };
    const direction = keyMap[event.key];
    if (!direction) return;
    event.preventDefault();
    const activePane = (event.target as HTMLElement).closest('.ant-tabs-tabpane-active') || document;
    const actualInputs = Array.from(activePane.querySelectorAll<HTMLInputElement>('[data-report-check-field="actualValue"] input'));
    const remarkInputs = Array.from(activePane.querySelectorAll<HTMLInputElement>('[data-report-check-field="abnormalRemark"] input'));
    const max = Math.max(actualInputs.length, remarkInputs.length) - 1;
    let nextRow = rowIndex;
    let nextField = field;
    if (direction === 'next' || direction === 'right') {
      if (field === 'actualValue') {
        nextField = 'abnormalRemark';
      } else {
        nextRow = Math.min(max, rowIndex + 1);
        nextField = 'actualValue';
      }
    } else if (direction === 'left') {
      if (field === 'abnormalRemark') {
        nextField = 'actualValue';
      } else {
        nextRow = Math.max(0, rowIndex - 1);
        nextField = 'abnormalRemark';
      }
    } else {
      nextRow = direction === 'up' ? Math.max(0, rowIndex - 1) : Math.min(max, rowIndex + 1);
    }
    const target = nextField === 'actualValue' ? actualInputs[nextRow] : remarkInputs[nextRow];
    target?.focus();
    target?.select?.();
    return;
  }
  const key = event.key;
  if (!['ArrowDown', 'ArrowLeft', 'ArrowRight', 'ArrowUp', 'Enter'].includes(key)) return;
  const activePane = (event.target as HTMLElement).closest('.ant-tabs-tabpane-active') || document;
  const inputs = Array.from(activePane.querySelectorAll<HTMLInputElement>('input.cut-round-check-cell'));
  const currentIndex = inputs.indexOf(event.target as HTMLInputElement);
  if (currentIndex < 0) return;
  const columnCount = 2;
  let nextIndex = currentIndex;
  if (key === 'Enter' || key === 'ArrowRight') {
    nextIndex = Math.min(inputs.length - 1, currentIndex + 1);
  } else if (key === 'ArrowLeft') {
    nextIndex = Math.max(0, currentIndex - 1);
  } else if (key === 'ArrowDown') {
    nextIndex = Math.min(inputs.length - 1, currentIndex + columnCount);
  } else if (key === 'ArrowUp') {
    nextIndex = Math.max(0, currentIndex - columnCount);
  }
  event.preventDefault();
  inputs[nextIndex]?.focus();
  inputs[nextIndex]?.select?.();
}

function handleIntermediateCellKeydown(event: KeyboardEvent) {
  const key = event.key;
  if (!['ArrowDown', 'ArrowLeft', 'ArrowRight', 'ArrowUp', 'Enter'].includes(key)) return;
  const inputs = Array.from(document.querySelectorAll<HTMLInputElement>('.cut-round-intermediate-cell input, input.cut-round-intermediate-cell'));
  const currentIndex = inputs.indexOf(event.target as HTMLInputElement);
  if (currentIndex < 0) return;
  const columnCount = 4;
  let nextIndex = currentIndex;
  if (key === 'Enter' || key === 'ArrowRight') {
    nextIndex = Math.min(inputs.length - 1, currentIndex + 1);
  } else if (key === 'ArrowLeft') {
    nextIndex = Math.max(0, currentIndex - 1);
  } else if (key === 'ArrowDown') {
    nextIndex = Math.min(inputs.length - 1, currentIndex + columnCount);
  } else if (key === 'ArrowUp') {
    nextIndex = Math.max(0, currentIndex - columnCount);
  }
  event.preventDefault();
  inputs[nextIndex]?.focus();
  inputs[nextIndex]?.select?.();
}

function resolveDailyRecordAuthOperator(authPayload: any) {
  return authPayload?.empName || authPayload?.nickname || authPayload?.realName || authPayload?.username || currentUserName.value || '';
}

async function submitDailyRecord(action: DailyRecordSubmitAction, authPayload: any) {
  const row = dailyRecordRows.value.find((item) => String(item.key) === action.key);
  if (!row) {
    message.warning('未找到待提交的点检/清洁记录，请重新打开记录列表');
    return;
  }
  if (action.mode === 'edit' && !canFillDailyRecord(row)) {
    message.warning('后台状态不允许填写当前记录');
    return;
  }
  if (action.mode === 'confirm' && !canConfirmDailyRecord(row)) {
    message.warning('后台状态不允许确认当前记录，请先填写保存');
    return;
  }
  const now = buildNowText();
  const operator = resolveDailyRecordAuthOperator(authPayload);
  const safeRecorderTime = normalizeDateTimeText(row.recorderTime);
  const safeConfirmerTime = normalizeDateTimeText(row.confirmerTime);
  const context = getDailyRecordContext();
  const equipment = getEffectiveBoardEquipment(context || {});
  if (!operator) {
    message.warning('请先完成记录人/确认人身份确认');
    return;
  }
  if (!equipment.equipmentId && !equipment.equipmentCode) {
    message.warning('请先选择裁切设备，再保存今日点检/清洁记录');
    return;
  }
  if (!context?.planId || !context?.planOperationId) {
    message.warning('当前裁切机台暂无可关联的裁切任务，无法保存后台点检/清洁记录');
    return;
  }
  const existingRecorder = row.recorder === '-' ? operator : row.recorder;
  const recorder = action.mode === 'edit' ? operator : existingRecorder;
  const recorderTime = action.mode === 'confirm' ? safeRecorderTime || now : now;
  const existingConfirmer = row.confirmer === '-' ? undefined : row.confirmer;
  const confirmer = action.mode === 'confirm' ? operator : existingConfirmer;
  const confirmerTime = action.mode === 'confirm' ? now : safeConfirmerTime || undefined;
  const payload: MesHcCutRoundConsoleApi.PassWorkSaveReq = {
    confirmer,
    confirmerTime,
    details: row.details.map((item) => ({
      actualValue: item.value,
      category: item.category,
      item: item.item,
      itemSeq: item.seq,
      node: item.node,
      remark: item.remark,
      standard: item.standard,
      status: 'OK',
    })),
    equipmentCode: equipment.equipmentCode || undefined,
    equipmentId: equipment.equipmentId,
    equipmentName: equipment.equipmentName || undefined,
    formCode: row.formCode,
    inspectionResult: 'OK',
    planId: context.planId,
    planOperationId: context.planOperationId,
    recordId: row.recordId,
    recordDate: dayjs().format('YYYY-MM-DD'),
    recorder,
    recorderTime,
    result: 'OK',
  };
  if (action.mode === 'confirm') {
    await confirmCutRoundConsolePassWork(payload);
  } else {
    await saveCutRoundConsolePassWork(payload);
  }
  await loadDailyRecords();
  dailyRecordVisible.value = false;
  message.success(action.mode === 'confirm' ? '工作准备记录已确认' : '工作准备记录已保存');
}

async function saveDailyRecord(confirm = false) {
  const row = selectedDailyRecord.value;
  if (!row) return;
  const mode: DailyRecordSubmitAction['mode'] = confirm ? 'confirm' : 'edit';
  if (mode === 'edit' && !canFillDailyRecord(row)) {
    message.warning('后台状态不允许填写当前记录');
    return;
  }
  if (mode === 'confirm' && !canConfirmDailyRecord(row)) {
    message.warning('后台状态不允许确认当前记录，请先填写保存');
    return;
  }
  const context = getDailyRecordContext();
  const equipment = getEffectiveBoardEquipment(context || {});
  if (!equipment.equipmentId && !equipment.equipmentCode) {
    openDailyRecordAfterEquipmentSelected.value = true;
    message.warning('请先选择裁切设备，再保存今日点检/清洁记录');
    await openEquipmentSelect();
    return;
  }
  if (!context?.planId || !context?.planOperationId) {
    message.warning('当前裁切机台暂无可关联的裁切任务，无法保存后台点检/清洁记录');
    return;
  }
  pendingDailyRecordAction.value = {
    key: String(row.key),
    mode,
  };
  dailyRecordAuthAction.value = `${mode === 'confirm' ? '确认' : '保存'}${row.name || '工作准备记录'}`;
  dailyRecordAuthVisible.value = true;
}

async function handleDailyRecordAuthSuccess(payload: any) {
  const pending = pendingDailyRecordAction.value;
  pendingDailyRecordAction.value = undefined;
  if (!pending) {
    message.warning('未找到待提交的点检/清洁记录，请重新点击保存或确认');
    return;
  }
  await submitDailyRecord(pending, payload);
}

function handleDailyRecordAuthCancel() {
  pendingDailyRecordAction.value = undefined;
  dailyRecordAuthVisible.value = false;
}

async function applyGlueBoardScan() {
  const scanCode = glueConsumeForm.materialScanCode.trim();
  if (!scanCode) {
    message.warning('请扫码或输入胶板边库批号');
    return;
  }
  const stock = await getCutRoundConsoleGlueBoardStockByBatch(scanCode);
  const source = (stock as any)?.data ?? stock;
  if (!source?.id) {
    glueBoard.alarm = '未找到该胶板边库批次，请先在胶板边料管理中领料登记。';
    message.warning(glueBoard.alarm);
    return;
  }
  const availableLength = Number(source.availableLength || 0);
  if (availableLength <= 0) {
    glueBoard.alarm = '该胶板边库批次已无可用长度，请更换批次。';
    message.warning(glueBoard.alarm);
    return;
  }
  glueConsumeForm.stockId = source.id;
  glueConsumeForm.materialScanCode = source.glueBoardBatchNo || scanCode;
  glueConsumeForm.batchNo = source.glueBoardBatchNo || scanCode;
  glueConsumeForm.receiveStartPosition = Number(source.availableStartPosition || 0);
  glueConsumeForm.receiveLength = availableLength;
  glueConsumeForm.stockAvailableLength = availableLength;
  glueBoard.stockId = source.id;
  glueBoard.materialCode = source.glueBoardMaterialCode || '';
  glueBoard.batchNo = source.glueBoardBatchNo || '';
  glueBoard.receiveStartPosition = Number(source.receiveStartPosition || 0);
  glueBoard.receiveLength = Number(source.receiveLength || 0);
  glueBoard.availableStartPosition = Number(source.availableStartPosition || 0);
  glueBoard.stockLength = availableLength;
  glueBoard.alarm = '';
  // message.success('已带出胶板边库批次，可填写本次领用长度');
}

function openGlueConsumeDialog() {
  glueConsumeForm.stockId = glueBoard.stockId;
  glueConsumeForm.materialScanCode = glueBoard.batchNo;
  glueConsumeForm.batchNo = glueBoard.batchNo;
  glueConsumeForm.receiveStartPosition = Number(glueBoard.availableStartPosition || glueBoard.receiveStartPosition || 0);
  glueConsumeForm.receiveLength = undefined;
  glueConsumeForm.stockAvailableLength = Number(glueBoard.stockLength || 0);
  glueConsumeVisible.value = true;
}

async function confirmGlueConsume() {
  if (!currentPlan.planId || !currentPlan.planOperationId) {
    message.warning('请先扫码带出裁切计划');
    return;
  }
  const receiveLength = Number(glueConsumeForm.receiveLength || 0);
  if (receiveLength <= 0) {
    message.warning('请填写本次领用长度');
    return;
  }
  const materialCode = String(glueBoard.materialCode || ADHESIVE_DEFAULT_GLUE_BOARD_MATERIAL_CODE).trim();
  const batchNo = String(glueConsumeForm.batchNo || glueBoard.batchNo || '').trim();
  if (!materialCode || !batchNo) {
    message.warning('请填写胶板料号和胶板批号');
    return;
  }
  if (glueConsumeForm.stockAvailableLength > 0 && receiveLength > Number(glueConsumeForm.stockAvailableLength || 0)) {
    message.warning(`本次领用长度不能超过边库可用长度 ${formatNumber(glueConsumeForm.stockAvailableLength)} m`);
    return;
  }
  await saveCutRoundConsoleGlueBoardUsage({
    equipmentCode: currentPlan.equipmentCode,
    equipmentId: currentPlan.equipmentId,
    equipmentName: currentPlan.equipmentName,
    glueBoardBatchNo: batchNo,
    glueBoardMaterialCode: materialCode,
    glueBoardStockId: glueConsumeForm.stockId || glueBoard.stockId,
    planId: currentPlan.planId,
    planOperationId: currentPlan.planOperationId,
    receiveLength,
    receiveStartPosition: Number(glueConsumeForm.receiveStartPosition || 0),
    recordDate: dayjs().format('YYYY-MM-DD'),
    recorderName: currentUserName.value || undefined,
    recorderTime: buildNowText(),
  });
  await loadGlueBoardUsage();
  glueConsumeVisible.value = false;
  message.success('胶板领用记录已保存');
}

function openGlueBoardAqcDialog() {
  if (!currentPlan.planId || !currentPlan.planOperationId) {
    message.warning('请先扫码带出裁切计划');
    return;
  }
  if (!glueBoard.id) {
    message.warning('请先登记本次胶板领用信息');
    return;
  }
  aqcForm.id = glueBoard.latestAqcTask?.id;
  aqcForm.sampleStartPosition = Number(glueBoard.availableStartPosition || glueBoard.receiveStartPosition || 0);
  aqcForm.sampleLength = undefined;
  aqcVisible.value = true;
}

async function submitGlueBoardAqc() {
  if (!currentPlan.planId || !currentPlan.planOperationId || !glueBoard.id) {
    message.warning('请先登记本次胶板领用信息');
    return;
  }
  const sampleLength = Number(aqcForm.sampleLength || 0);
  if (sampleLength <= 0) {
    message.warning('请填写首检送检长度');
    return;
  }
  const task = await submitCutRoundConsoleAqcTask({
    glueBoardBatchNo: glueBoard.batchNo,
    glueBoardMaterialCode: glueBoard.materialCode,
    glueBoardUsageId: glueBoard.id,
    planId: currentPlan.planId,
    planOperationId: currentPlan.planOperationId,
    recordDate: dayjs().format('YYYY-MM-DD'),
    sampleLength,
    sampleStartPosition: Number(aqcForm.sampleStartPosition || 0),
    submitterName: currentUserName.value || undefined,
    submitTime: buildNowText(),
    taskType: 'DAILY_GLUE_BOARD',
  });
  aqcForm.id = task?.id;
  await loadGlueBoardUsage();
  message.success('粘胶检验任务已提交，等待检验反馈');
}

function openGlueBoardLossDialog() {
  if (!glueBoard.id) {
    message.warning('请先登记本次胶板领用信息');
    return;
  }
  glueLossForm.startPosition = Number(glueBoard.availableStartPosition || glueBoard.receiveStartPosition || 0);
  glueLossForm.lossLength = undefined;
  glueLossForm.endPosition = undefined;
  glueLossForm.lossReason = '';
  glueLossVisible.value = true;
}

function syncGlueLossEndFromLength() {
  const startPosition = Number(glueLossForm.startPosition || 0);
  const lossLength = Number(glueLossForm.lossLength || 0);
  if (lossLength > 0) {
    glueLossForm.endPosition = Number((startPosition + lossLength).toFixed(3));
  }
}

function syncGlueLossLengthFromEnd() {
  const startPosition = Number(glueLossForm.startPosition || 0);
  const endPosition = Number(glueLossForm.endPosition || 0);
  if (endPosition > startPosition) {
    glueLossForm.lossLength = Number((endPosition - startPosition).toFixed(3));
  }
}

async function submitGlueBoardLoss() {
  if (!glueBoard.id) {
    message.warning('请先登记本次胶板领用信息');
    return;
  }
  const startPosition = Number(glueLossForm.startPosition || 0);
  const endPosition = Number(glueLossForm.endPosition || 0);
  const lossLength = Number(glueLossForm.lossLength || 0);
  if (endPosition <= startPosition) {
    message.warning('损耗止位置必须大于起位置');
    return;
  }
  if (lossLength <= 0) {
    message.warning('请填写大于0的损耗长度');
    return;
  }
  AModal.confirm({
    title: '确认保存胶板损耗报备',
    content: `本次损耗区间 ${formatNumber(startPosition)}-${formatNumber(endPosition)} m，保存后会扣减当前胶板可用长度。`,
    okText: '确认保存',
    cancelText: '取消',
    onOk: async () => {
      await reportCutRoundConsoleGlueBoardLoss({
        endPosition,
        glueBoardUsageId: glueBoard.id!,
        lossLength,
        lossReason: glueLossForm.lossReason || undefined,
        startPosition,
      });
      await loadGlueBoardUsage();
      glueLossVisible.value = false;
      message.success('胶板损耗已报备');
    },
  });
}

function resetReportForm() {
  Object.assign(reportForm, {
    actualSizeRule: '',
    aqcStatus: '',
    aqcTaskId: undefined,
    defectCode: '',
    endTime: '',
    glueBoardBatchNo: glueBoard.batchNo,
    glueBoardMaterialCode: glueBoard.materialCode,
    glueBoardModel: getDefaultGlueBoardModel(currentCutRoundProductModelCode.value),
    glueBoardStartPosition: Number(glueBoard.availableStartPosition || 0),
    glueBoardUsageId: glueBoard.id,
    glueBoardUseLength: 0,
    lossLength: 0,
    modelCode: '',
    napSampleLength: 0,
    outputLength: 0,
    parentBatchNo: '',
    processLength: 0,
    productionBatchNo: '',
    preProcessSelfCheckAbnormal: false,
    remark: '',
    reportDate: dayjs().format('YYYY-MM-DD'),
    reportType: 'PRODUCT',
    selfCheck: 'OK',
    sourceCode: '',
    startPosition: 0,
    endPosition: 0,
    sourceGrindingSecondDetailId: undefined,
    sourceNgDefectCode: '',
    sourceProductionBatchNo: '',
    sourceActualSizeRule: '',
    sourceNgReviewResult: '',
    sourceNgDefectSummary: '',
    sourceNgVisualItems: [],
    sourceDownstreamStatus: '',
    sourceScanError: '',
    sourceScanMessage: '',
    startTime: '',
  });
  checkTemplate.value = checkTemplate.value.map((item) => ({
    ...item,
    abnormalRemark: '',
    actualValue: getDefaultCheckActualValue(item),
    checkResult: 'OK',
  }));
  cutRoundProcessCheckItems.value = cutRoundProcessCheckItems.value.map((item) => ({
    ...item,
    abnormalRemark: '',
    actualValue: item.itemName === '检测时间' ? buildNowText() : item.itemName === '反馈状态' ? '待检' : '',
    checkResult: 'OK',
  }));
  cutRoundIntermediateCheckItems.value = cutRoundIntermediateCheckItems.value.map((item) => ({
    ...item,
    abnormalRemark: '',
    actualValue: '',
    checkResult: 'OK',
  }));
  resetVisualInspectionItems();
  clearReportIntermediateDraft();
}

async function scanReportSource() {
  const sourceCode = reportForm.sourceCode.trim();
  reportForm.sourceScanError = '';
  reportForm.sourceScanMessage = '';
  if (!sourceCode) {
    reportForm.sourceScanError = '请先扫描或输入粘胶2已确认片或NG片号。';
    return false;
  }
  try {
    const source = await scanCutRoundConsoleSource(sourceCode);
    reportForm.sourceGrindingSecondDetailId = source.grindingSecondDetailId;
    reportForm.sourceProductionBatchNo = source.productionBatchNo || sourceCode;
    reportForm.sourceActualSizeRule = normalizeSizeRule(source.actualSizeRule);
    if (reportForm.sourceActualSizeRule) {
      reportForm.actualSizeRule = reportForm.sourceActualSizeRule;
    }
    reportForm.sourceDownstreamStatus = source.downstreamStatus || '未裁切报工';
    reportForm.modelCode = source.modelCode || reportForm.modelCode || currentCutRoundProductModelCode.value || '';
    reportForm.parentBatchNo = source.motherBatchNo || source.parentProductionBatchNo || stripSegmentMark(sourceCode);
    reportForm.productionBatchNo = reportForm.sourceActualSizeRule
      ? reportForm.sourceProductionBatchNo
      : (reportForm.productionBatchNo
        || normalizeCutRoundProductionBatchNo(reportForm.sourceProductionBatchNo, reportForm.actualSizeRule));
    const sourceLength = Number(source.outputLength || source.processLength || reportForm.processLength || 0);
    const segment = findSourceSegment(reportForm.sourceProductionBatchNo);
    const availableRange = segment
      ? findFirstAvailableRange(segment)
      : { end: sourceLength, length: sourceLength, start: 0 };
    reportForm.startPosition = availableRange.start;
    reportForm.endPosition = availableRange.end;
    reportForm.processLength = availableRange.length;
    reportForm.outputLength = reportOutputLength.value;
    applyAdhesive2SourceNgDefectDefaults(source);
    reportForm.sourceScanMessage = reportForm.sourceNgDefectSummary
      ? '已自动带入粘胶2不合格项，请在“外观检验”中复核；取消全部继承项后可按裁切合格流转。'
      : `来源已通过粘胶2片号扫码接口带出；裁切状态：${reportForm.sourceDownstreamStatus}。`;
    if (
      !activeRecord.value?.id
      && ['裁切已扫码报工', '裁切已报工待扫码确认'].includes(reportForm.sourceDownstreamStatus)
    ) {
      reportForm.sourceScanError = `该粘胶2片${reportForm.sourceDownstreamStatus}，不能重复新建裁切报工。`;
      return false;
    }
    return true;
  } catch (error: unknown) {
    reportForm.sourceScanError = getReportRequestErrorMessage(
      error,
      '来源扫码接口调用失败，请检查网络或登录状态后重试。',
    );
    return false;
  }
}

async function openReportDialog(
  group: SourceGroup,
  segment: SourceSegment,
  reportType: CutRoundReportType = getDefaultCutRoundReportType(segment),
  scannedBatchNo = segment.batchNo,
  viewOnly = false,
) {
  if (reportType === 'CHANGEOVER') {
    openChangeoverInspectionScan(scannedBatchNo || segment.batchNo);
    return;
  }
  const existedRecord = findReportBySegment(segment);
  if (!viewOnly && (isCurrentTaskReadonly.value || isSourceGroupCompleted(group))) {
    showReadonlyTaskWarning('报工', group);
    return;
  }
  if (!viewOnly && segment.sourceNgText && !existedRecord) {
    message.warning(`片号 ${segment.batchNo} 已标记为${segment.sourceNgText}${segment.sourceNgReason ? `：${segment.sourceNgReason}` : ''}，将带风险标记进入裁切和FQC检验。`);
  }
  if (!viewOnly && Number(segment.availableLength || 0) <= 0 && !existedRecord) {
    message.warning('当前片号已完成裁切报工，没有可加工数量');
    return;
  }
  if (!viewOnly) {
    if (isWorkOrderFinished.value) {
      message.warning('当前工单此工序已完工，不能再报工！');
      return;
    }
    if (!isWorkOrderRunning.value) {
      AModal.warning({
        okText: '去待加工开工',
        title: '当前裁切工单未开工',
        content: '请先在右上角“待加工”中选择当前计划并执行开工，再进行裁切报工登记。',
        onOk: () => {
          void openTaskList();
        },
      });
      return;
    }
    if (!isChangeoverInspectionReady()) {
      showChangeoverInspectionRequiredWarning(scannedBatchNo || segment.batchNo);
      return;
    }
    if (!ensureDailyPreparationReady()) {
      return;
    }
    if (!ensureCutRoundConsumablesReady('裁切报工')) {
      return;
    }
  }
  resetReportForm();
  activeRecord.value = existedRecord || null;
  const inheritedActualSizeRule = getInheritedCutRoundSizeRule(segment);
  const initialActualSizeRule = viewOnly ? '' : inheritedActualSizeRule;
  const availableRange = findFirstAvailableRange(segment);
  reportForm.sourceCode = segment.batchNo;
  reportForm.sourceProductionBatchNo = segment.batchNo;
  reportForm.modelCode = segment.modelCode || currentCutRoundProductModelCode.value || '';
  reportForm.parentBatchNo = group.baseBatchNo;
  reportForm.reportType = reportType;
  reportForm.actualSizeRule = initialActualSizeRule;
  reportForm.sourceActualSizeRule = inheritedActualSizeRule;
  reportForm.startPosition = availableRange.start;
  reportForm.endPosition = availableRange.end;
  reportForm.processLength = availableRange.length;
  reportForm.outputLength = reportOutputLength.value;
  reportForm.glueBoardUseLength = availableRange.length;
  reportForm.productionBatchNo = inheritedActualSizeRule
    ? segment.batchNo
    : normalizeCutRoundProductionBatchNo(segment.batchNo, initialActualSizeRule);
  reportDialogMode.value = viewOnly ? 'view' : 'confirm';
  reportStep.value = 'SCAN';
  activeReportTab.value = viewOnly ? getDefaultReportTabKey() : 'scan';
  reportVisible.value = true;
  await scanReportSource();
  applyReportRecordToForm(existedRecord);
  if (viewOnly) ensureActiveReportTab();
}

function openReportViewDialog(group: SourceGroup, segment: SourceSegment) {
  void openReportDialog(group, segment, getCutRoundSegmentReportType(segment), segment.batchNo, true);
}

async function startReportWork() {
  const ok = await scanReportSource();
  if (!ok) return;
  if (!normalizeSizeRule(reportForm.actualSizeRule)) {
    message.warning('粘胶2已确认来源未携带实际尺寸，不能进入裁切报工');
    return;
  }
  syncReportProductionBatchNoByActualSize();
  reportForm.startTime = reportForm.startTime || buildNowText();
  reportStep.value = 'PROCESS';
  activeReportTab.value = getDefaultReportTabKey();
  if (!activeReportTab.value) {
    reportStep.value = 'REPORT';
    activeReportTab.value = 'report';
  }
  if (activeReportTab.value === 'visual-inspection') {
    reportForm.selfCheck = hasActiveVisualItems.value ? 'NG' : (reportForm.selfCheck || 'OK');
  }
  if (activeReportTab.value.startsWith('check-')) {
    focusActiveReportCheckCell();
  }
}

function getActiveCheckCategory() {
  const activeKey = String(activeReportTab.value || '');
  if (!activeKey.startsWith('check-')) return '';
  return activeKey.slice('check-'.length);
}

function focusActiveReportCheckCell() {
  nextTick(() => {
    const input = document.querySelector<HTMLInputElement>('.ant-tabs-tabpane-active input.cut-round-check-cell');
    input?.focus();
    input?.select?.();
  });
}

async function submitProcessItems() {
  if (reportDialogMode.value === 'view') return;
  const categories = reportCheckCategories.value;
  const activeCategory = getActiveCheckCategory();
  const currentIndex = activeCategory ? categories.indexOf(activeCategory) : -1;
  if (currentIndex >= 0 && currentIndex < categories.length - 1) {
    activeReportTab.value = `check-${categories[currentIndex + 1]}`;
    focusActiveReportCheckCell();
    return;
  }
  if (shouldShowVisualInspectionTab.value && activeReportTab.value !== 'visual-inspection') {
    activeReportTab.value = 'visual-inspection';
    reportForm.selfCheck = hasActiveVisualItems.value ? 'NG' : (reportForm.selfCheck || 'OK');
    message.info('请补充外观检验、综合判断和备注后再保存扫码确认。');
    return;
  }
  reportForm.endTime = reportForm.endTime || buildNowText();
  await submitCutRoundReport({ confirmAfterSave: true });
}

function getCheckItemsByCategory(category: string) {
  let rows: CutRoundCheckItem[] = [];
  if (category === '中间品') {
    rows = cutRoundIntermediateCheckItems.value;
  } else if (shouldShowProcessParameterTab.value) {
    rows = ensureCutRoundChangeoverItems(checkTemplate.value).filter((item) => item.itemCategory === category);
  }
  return rows
    .filter((item) => item.itemCategory === category)
    .sort((a, b) => Number(a.sortNo || 0) - Number(b.sortNo || 0));
}

function isCutRoundReportType(value?: string): value is CutRoundReportType {
  return cutRoundReportTypeOptions.some((item) => item.value === value);
}

function isCutRoundIntermediateReportType(value?: string): value is 'END' | 'FRONT' | 'MIDDLE' {
  return ['END', 'FRONT', 'MIDDLE'].includes(String(value || ''));
}

function isCutRoundInspectionReportType(value?: string) {
  return ['CHANGEOVER', 'PROCESS_CHECK'].includes(String(value || ''));
}

function getCutRoundReportTypeText(value?: string) {
  return cutRoundReportTypeOptions.find((item) => item.value === value)?.label || '成品加工';
}

function getChangeoverConfirmerName(record?: MesHcCutRoundConsoleApi.ChangeoverInspection | Record<string, any>) {
  const extra = parseRecordExtra(record || {}) as Record<string, any>;
  if (!isChangeoverInspectionConfirmed(record)) return '-';
  return String((record as Record<string, any> | undefined)?.confirmerName || extra.confirmerName || '-');
}

function normalizeChangeoverStatus(status?: string) {
  return String(status || 'RECORDED').trim().toUpperCase();
}

function isChangeoverInspectionConfirmed(record?: MesHcCutRoundConsoleApi.ChangeoverInspection | Record<string, any> | null) {
  const extra = parseRecordExtra(record || {}) as Record<string, any>;
  return normalizeChangeoverStatus((record as Record<string, any> | undefined)?.inspectionStatus) === 'CONFIRMED'
    || normalizeChangeoverStatus(extra.recordStatus) === 'CONFIRMED'
    || normalizeChangeoverStatus(extra.docStatus) === 'CONFIRMED';
}

function getChangeoverStatusMeta(record?: MesHcCutRoundConsoleApi.ChangeoverInspection | Record<string, any>) {
  if (isChangeoverInspectionConfirmed(record)) return { color: 'processing', text: '已确认' };
  const status = normalizeChangeoverStatus((record as Record<string, any> | undefined)?.inspectionStatus);
  if (status === 'DRAFT') return { color: 'default', text: '草稿' };
  return { color: 'success', text: '已保存' };
}

const isChangeoverFormConfirmed = computed(() => isChangeoverInspectionConfirmed(changeoverForm as Record<string, any>));
const changeoverFormStatusMeta = computed(() => getChangeoverStatusMeta(changeoverForm as Record<string, any>));

function getChangeoverImportAttachments(record?: Record<string, any>) {
  const extra = parseRecordExtra(record || (changeoverForm as Record<string, any>)) as Record<string, any>;
  const list = Array.isArray(extra.importAttachments)
    ? extra.importAttachments
    : (extra.importAttachment ? [extra.importAttachment] : []);
  return list.filter(Boolean).map((item: any) => ({
    name: String(item?.name || '导入附件'),
    path: String(item?.path || ''),
    size: Number(item?.size || 0),
    type: String(item?.type || ''),
    uploadTime: String(item?.uploadTime || ''),
    url: String(item?.url || ''),
  }));
}

const changeoverImportAttachments = computed(() => getChangeoverImportAttachments(changeoverForm as Record<string, any>));

function formatAttachmentSize(size?: number) {
  const value = Number(size || 0);
  if (value >= 1024 * 1024) return `${(value / 1024 / 1024).toFixed(1)} MB`;
  if (value >= 1024) return `${(value / 1024).toFixed(1)} KB`;
  return value > 0 ? `${value} B` : '-';
}

function getValidDateTimeText(value?: string) {
  if (!value) return '';
  const parsed = dayjs(value);
  if (!parsed.isValid() || parsed.year() <= 1971) return '';
  return parsed.format('YYYY-MM-DD HH:mm:ss');
}

function normalizeChangeoverInspection(record?: MesHcCutRoundConsoleApi.ChangeoverInspection | null) {
  if (!record) return null;
  const extra = parseRecordExtra(record as Record<string, any>) as Record<string, any>;
  const confirmed = isChangeoverInspectionConfirmed(record);
  const fallbackTime =
    getValidDateTimeText(record.recordTime)
    || getValidDateTimeText(record.submitTime)
    || getValidDateTimeText(record.updateTime)
    || getValidDateTimeText(record.createTime)
    || buildNowText();
  const checkItems = parseChangeoverCheckItems(record);
  return {
    ...record,
    checkItems: checkItems.length ? checkItems.map((item, index) => normalizeCheckItem(item, index)) : record.checkItems,
    confirmerName: confirmed ? (record.confirmerName || extra.confirmerName || '') : '',
    confirmerTime: confirmed ? getValidDateTimeText(record.confirmerTime || extra.confirmerTime) : '',
    recordTime: fallbackTime,
    submitTime: getValidDateTimeText(record.submitTime) || fallbackTime,
  };
}

function parseChangeoverCheckItems(record?: MesHcCutRoundConsoleApi.ChangeoverInspection) {
  if (Array.isArray(record?.checkItems) && record.checkItems.length > 0) {
    return record.checkItems;
  }
  if (!record?.detailItemsJson) return [];
  try {
    const parsed = JSON.parse(record.detailItemsJson);
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}

function getDefaultCutRoundReportType(_segment?: SourceSegment): CutRoundReportType {
  // 裁切工序只按“成品加工”扫码确认，不再沿用粘胶2/压槽的前中后段作业类型。
  return 'PRODUCT';
}

function getReportCheckItemsForSave() {
  if (reportForm.reportType === 'PRODUCT') {
    return [];
  }
  const baseItems = reportForm.reportType === 'CHANGEOVER'
    ? ensureCutRoundChangeoverItems(checkTemplate.value)
    : getCutRoundProcessCheckItems();
  if (isCutRoundIntermediateReportType(reportForm.reportType)) {
    return [...baseItems, ...cutRoundIntermediateCheckItems.value];
  }
  return baseItems;
}

function applyReportCheckItems(checkItems?: MesHcCutRoundConsoleApi.CheckItem[]) {
  const savedItems = Array.isArray(checkItems) ? checkItems : [];
  checkTemplate.value = checkTemplate.value.map((item) => {
    const matched = savedItems.find((saved) => saved.itemCategory === item.itemCategory && saved.itemName === item.itemName)
      || savedItems.find((saved) => saved.itemName === item.itemName);
    return matched
      ? {
          ...item,
          abnormalRemark: matched.abnormalRemark || '',
          actualValue: matched.actualValue || getDefaultCheckActualValue(item),
          checkResult: matched.checkResult || 'OK',
          standardValue: matched.standardValue || item.standardValue,
        }
      : item;
  });
  cutRoundIntermediateCheckItems.value = cutRoundIntermediateCheckItems.value.map((item) => {
    const matched = savedItems.find((saved) => saved.itemCategory === item.itemCategory && saved.itemName === item.itemName);
    return matched
      ? {
          ...item,
          abnormalRemark: matched.abnormalRemark || '',
          actualValue: matched.actualValue || '',
          checkResult: matched.checkResult || 'OK',
          standardValue: matched.standardValue || item.standardValue,
        }
      : item;
  });
  cutRoundProcessCheckItems.value = cutRoundProcessCheckItems.value.map((item) => {
    const matched = savedItems.find((saved) => saved.itemCategory === item.itemCategory && saved.itemName === item.itemName);
    return matched
      ? {
          ...item,
          abnormalRemark: matched.abnormalRemark || '',
          actualValue: matched.actualValue || item.actualValue || '',
          checkResult: matched.checkResult || 'OK',
          standardValue: matched.standardValue || item.standardValue,
        }
      : {
          ...item,
          actualValue: item.itemName === '检测时间' ? buildNowText() : item.actualValue,
        };
  });
}

function buildDefaultVisualItems(): VisualItem[] {
  return cutRoundVisualItemNames.map((itemName) => ({
    itemName,
    remark: '',
    result: 'OK',
  }));
}

function normalizeVisualResult(value: unknown): 'NG' | 'OK' {
  const text = String(value ?? '').trim().toUpperCase();
  if (['1', 'ABNORMAL', 'FAIL', 'FAILED', 'FALSE', 'N', 'NG', '不合格', '异常'].includes(text)) return 'NG';
  return 'OK';
}

function isNgCodeLike(value: unknown) {
  const text = String(value ?? '').trim().toUpperCase();
  return normalizeVisualResult(text) === 'NG'
    || text.startsWith('NG')
    || text.includes('_NG')
    || text.includes('NG_')
    || text.includes(' NG')
    || text.includes('NG ')
    || text.includes('NG片')
    || text.includes('NG异常');
}

function normalizeProcessName(value: unknown) {
  const text = String(value ?? '').trim();
  if (!text) return '';
  const upper = text.toUpperCase();
  if (upper.includes('PRESS') || upper.includes('SLOT') || text.includes('压槽')) return '压槽';
  if (upper.includes('SLITTING') || upper.includes('SLIT') || text.includes('分切')) return '分切';
  if (upper.includes('ADHESIVE2') || upper.includes('ADHESIVE_2') || upper.includes('ADHESIVE-2') || text.includes('粘胶2')) return '粘胶2';
  if (upper.includes('ADHESIVE1') || upper.includes('ADHESIVE_1') || upper.includes('ADHESIVE-1') || text.includes('粘胶1')) return '粘胶1';
  if (upper.includes('CUT') || text.includes('裁切')) return '裁切';
  if (upper.includes('GRIND') || text.includes('研磨') || text.includes('二磨')) return '二次研磨';
  return text.endsWith('工序') ? text.slice(0, -2) : text;
}

function resolveNgProcessName(record?: MesHcCutRoundConsoleApi.ReportItem | Record<string, any> | null, fallback = '前工序') {
  const extra = parseRecordExtra(record || {}) as Record<string, any>;
  const source = (record || {}) as Record<string, any>;
  const candidates = [
    extra.sourceNgProcessName,
    extra.ngProcessName,
    extra.ngOperationName,
    extra.sourceOperationName,
    extra.operationName,
    extra.reportProcess,
    extra.processName,
    extra.processStage,
    extra.sourceMenuCode,
    source.sourceNgProcessName,
    source.ngProcessName,
    source.ngOperationName,
    source.sourceOperationName,
    source.operationName,
    source.reportProcess,
    source.processName,
    source.processStage,
    source.sourceMenuCode,
    fallback,
  ];
  return candidates.map(normalizeProcessName).find(Boolean) || fallback;
}

function formatProcessNgText(processName?: string) {
  return `${normalizeProcessName(processName) || '前工序'}工序NG`;
}

function getNgReasonText(record?: MesHcCutRoundConsoleApi.ReportItem | Record<string, any> | null) {
  const extra = parseRecordExtra(record || {}) as Record<string, any>;
  const source = (record || {}) as Record<string, any>;
  const visualItem = parseVisualItemsFromRecord(source).find((item) => isVisualItemActive(item));
  return String(
    source.sourceNgReason
      || extra.sourceNgReason
      || source.qualityLockReason
      || extra.qualityLockReason
      || source.defectCode
      || extra.defectCode
      || source.productQualityStatus
      || extra.productQualityStatus
      || source.inspectionResult
      || extra.inspectionResult
      || visualItem?.remark
      || visualItem?.itemName
      || '',
  ).trim();
}

function hasProcessNgFromRecord(record?: MesHcCutRoundConsoleApi.ReportItem | Record<string, any> | null) {
  if (!record) return false;
  if (hasCutRoundReportVisualIssue(record)) return true;
  const extra = parseRecordExtra(record) as Record<string, any>;
  return [
    (record as Record<string, any>).defectCode,
    (record as Record<string, any>).productQualityStatus,
    (record as Record<string, any>).qualityLockReason,
    (record as Record<string, any>).inspectionResult,
    extra.defectCode,
    extra.productQualityStatus,
    extra.qualityLockReason,
    extra.inspectionResult,
  ].some(isNgCodeLike);
}

function getSourceProcessNgText(record?: MesHcCutRoundConsoleApi.ReportItem | Record<string, any> | null, fallback = '前工序') {
  if (!hasProcessNgFromRecord(record)) return '';
  return formatProcessNgText(resolveNgProcessName(record, fallback));
}

function applyAdhesive2SourceNgDefectDefaults(source: MesHcCutRoundConsoleApi.SourceItem) {
  const sourceRecord = source as Record<string, any>;
  if (!hasProcessNgFromRecord(sourceRecord)) {
    reportForm.sourceNgDefectCode = '';
    reportForm.sourceNgReviewResult = '';
    reportForm.sourceNgDefectSummary = '';
    reportForm.sourceNgVisualItems = [];
    return;
  }
  const sourceNgVisualItems = parseVisualItemsFromRecord(sourceRecord)
    .filter((item) => isVisualItemActive(item))
    .map((item) => ({
      defectCode: item.inheritedDefectCode || String(source.defectCode || '').trim() || undefined,
      itemName: item.itemName,
      remark: String(item.remark || '').trim(),
    }));
  const summaryParts = [
    String(source.defectCode || '').trim(),
    getNgReasonText(sourceRecord),
    ...sourceNgVisualItems.map((item) => `${item.itemName}${item.remark ? `（${item.remark}）` : ''}`),
  ].filter(Boolean);
  reportForm.sourceNgDefectSummary = [...new Set(summaryParts)].join('；') || '粘胶2自检NG';
  reportForm.sourceNgDefectCode = String(source.defectCode || '').trim() || reportForm.sourceNgDefectSummary;
  reportForm.sourceNgVisualItems = normalizeSourceNgVisualItems(
    sourceNgVisualItems.length > 0
      ? sourceNgVisualItems
      : [{
          defectCode: reportForm.sourceNgDefectCode,
          itemName: '其他',
          remark: reportForm.sourceNgDefectSummary,
        }],
  );
  reportForm.sourceNgReviewResult = '';
  mergeAdhesive2SourceNgVisualItems(reportForm.sourceNgVisualItems);
}

function normalizeSourceNgVisualItems(source: unknown): SourceNgVisualItem[] {
  const rows = Array.isArray(source) ? source : [];
  const itemMap = new Map<string, SourceNgVisualItem>();
  rows.forEach((item) => {
    const itemName = String(item?.itemName || item?.name || item?.defectName || item?.label || '').trim();
    const defectCode = String(item?.defectCode || item?.code || '').trim();
    const remark = String(item?.remark || item?.description || item?.text || '').trim();
    if (!itemName && !defectCode && !remark) return;
    const normalizedItem: SourceNgVisualItem = {
      defectCode: defectCode || undefined,
      itemName: itemName || '其他',
      remark,
    };
    const key = `${normalizedItem.itemName}|${normalizedItem.defectCode || ''}|${normalizedItem.remark}`;
    if (!itemMap.has(key)) itemMap.set(key, normalizedItem);
  });
  return [...itemMap.values()];
}

function mergeAdhesive2SourceNgVisualItems(sourceItems: SourceNgVisualItem[]) {
  if (sourceItems.length === 0) return;
  const items = visualInspectionItems.value.length > 0
    ? visualInspectionItems.value
    : buildDefaultVisualItems();
  sourceItems.forEach((sourceItem) => {
    const targetItemName = cutRoundVisualItemNames.includes(sourceItem.itemName)
      ? sourceItem.itemName
      : '其他';
    const target = items.find((item) => item.itemName === targetItemName);
    if (!target) return;
    target.inheritedFromAdhesive2 = true;
    target.inheritedDefectCode = sourceItem.defectCode;
    target.result = 'NG';
    target.remark = target.remark || [sourceItem.remark, sourceItem.defectCode]
      .filter(Boolean)
      .join('；');
  });
  visualInspectionItems.value = items;
  reportForm.selfCheck = 'NG';
}

function normalizeVisualItems(source: unknown): VisualItem[] {
  let rows: any[] = [];
  if (typeof source === 'string') {
    try {
      const parsed = JSON.parse(source);
      rows = Array.isArray(parsed) ? parsed : parsed?.visualItems || parsed?.items || [];
    } catch {
      rows = [];
    }
  } else if (Array.isArray(source)) {
    rows = source;
  } else if (source && typeof source === 'object') {
    const objectSource = source as Record<string, any>;
    rows = objectSource.visualItems || objectSource.items || objectSource.details || objectSource.list || [];
  }
  const normalized = rows
    .map((item) => ({
      itemName: item?.itemName || item?.name || item?.defectName || item?.checkItem || item?.label || '',
      remark: item?.remark || item?.value || item?.actualValue || item?.description || item?.text || '',
      result: normalizeVisualResult(item?.result ?? item?.checkResult ?? item?.status ?? item?.ok),
      inheritedDefectCode: item?.inheritedDefectCode || item?.defectCode || undefined,
      inheritedFromAdhesive2: Boolean(item?.inheritedFromAdhesive2),
    }))
    .filter((item) => !!item.itemName);
  return cutRoundVisualItemNames.map((itemName) => {
    const matched = normalized.find((item) => item.itemName === itemName);
    return {
      itemName,
      inheritedDefectCode: matched?.inheritedDefectCode,
      inheritedFromAdhesive2: matched?.inheritedFromAdhesive2,
      remark: matched?.remark || '',
      result: matched && (matched.result === 'NG' || String(matched.remark || '').trim()) ? 'NG' : 'OK',
    };
  });
}

function parseVisualItemsFromRecord(record?: MesHcCutRoundConsoleApi.ReportItem | Record<string, any>) {
  const extra = parseRecordExtra(record || {}) as Record<string, any>;
  const source = extra.visualItems
    || extra.visualInspectionItems
    || extra.visualResult
    || extra.visualResults
    || (record as Record<string, any> | undefined)?.visualResultJson
    || [];
  return normalizeVisualItems(source);
}

function resetVisualInspectionItems(items?: VisualItem[]) {
  visualInspectionItems.value = items && items.length > 0 ? normalizeVisualItems(items) : buildDefaultVisualItems();
  if (visualInspectionItems.value.some((item) => isVisualItemActive(item))) {
    reportForm.selfCheck = 'NG';
  } else if (!reportForm.selfCheck) {
    reportForm.selfCheck = 'OK';
  }
}

function isVisualItemActive(item: VisualItem) {
  return item.result === 'NG' || Boolean(String(item.remark || '').trim());
}

function getVisualCheckItemClass(item: VisualItem) {
  return isVisualItemActive(item) ? 'visual-light-item is-active' : 'visual-light-item';
}

function toggleVisualItem(item: VisualItem) {
  if (!isVisualInspectionEditable.value) return;
  item.result = isVisualItemActive(item) ? 'OK' : 'NG';
  item.remark = '';
  reportForm.selfCheck = hasActiveVisualItems.value ? 'NG' : 'OK';
}

function getDefaultReportTabKey() {
  const firstCategory = reportCheckCategories.value[0];
  if (firstCategory) return `check-${firstCategory}`;
  return shouldShowVisualInspectionTab.value ? 'visual-inspection' : '';
}

function getReportTabKeys() {
  const keys = reportCheckCategories.value.map((category) => `check-${category}`);
  if (shouldShowVisualInspectionTab.value) keys.push('visual-inspection');
  if (reportDialogMode.value === 'view') keys.push('quality-result');
  return keys;
}

function ensureActiveReportTab() {
  const keys = getReportTabKeys();
  if (!keys.length) {
    activeReportTab.value = '';
    return;
  }
  if (!keys.includes(activeReportTab.value)) {
    activeReportTab.value = getDefaultReportTabKey();
  }
}

function getReportPositionValidationMessage() {
  const segment = findSourceSegment(reportForm.sourceProductionBatchNo);
  const start = Number(reportForm.startPosition ?? 0);
  const end = Number(reportForm.endPosition ?? 0);
  const processLength = Number(reportForm.processLength || 0);
  if (!Number.isFinite(start) || !Number.isFinite(end)) return '请填写有效的起止位置';
  if (start < 0) return '起始位置不能小于0';
  if (end <= start) return '结束位置必须大于起始位置';
  if (Math.abs(end - start - processLength) > 0.001) return '加工片数必须等于结束位置-起始位置';
  if (segment?.outputLength && end > Number(segment.outputLength)) {
    return `结束位置不能超出来源长度 ${formatNumber(segment.outputLength)} m`;
  }
  const overlapRange = (segment?.reportRanges || []).find((range) => start < range.end && end > range.start);
  if (overlapRange) {
    return `当前位置与已有报工 ${overlapRange.label}（${formatNumber(overlapRange.start)}-${formatNumber(overlapRange.end)}m）重叠`;
  }
  return '';
}

const reportPositionWarning = computed(() => getReportPositionValidationMessage());
const currentReportRange = computed(() => {
  const start = Number(reportForm.startPosition || 0);
  const end = Number(reportForm.endPosition || 0);
  return {
    end: Number.isFinite(end) ? roundMeter(end) : 0,
    start: Number.isFinite(start) ? roundMeter(start) : 0,
  };
});
const reportOverlapRanges = computed(() => {
  const current = currentReportRange.value;
  const segment = findSourceSegment(reportForm.sourceProductionBatchNo);
  if (!segment || current.end <= current.start) return [];
  return segment.reportRanges.filter((range) => current.start < range.end && current.end > range.start);
});
const hasReportRangeOverlap = computed(() => reportOverlapRanges.value.length > 0);
const reportRangeTotal = computed(() => {
  const segment = findSourceSegment(reportForm.sourceProductionBatchNo);
  const reportedMax = (segment?.reportRanges || []).reduce((max, item) => Math.max(max, item.end), 0);
  return Math.max(Number(segment?.outputLength || 0), reportedMax, currentReportRange.value.end, 1);
});
const reportRangeSegments = computed(() => {
  const total = reportRangeTotal.value || 1;
  const current = currentReportRange.value;
  const segment = findSourceSegment(reportForm.sourceProductionBatchNo);
  const toStyle = (start: number, end: number) => {
    const left = Math.min(Math.max((start / total) * 100, 0), 100);
    const width = Math.min(Math.max(((end - start) / total) * 100, 0), 100 - left);
    return { left, width };
  };
  const items = (segment?.reportRanges || []).map((range) => {
    const style = toStyle(range.start, range.end);
    const overlap = current.end > current.start && current.start < range.end && current.end > range.start;
    return {
      key: `reported-${range.reportId || range.label}-${range.start}-${range.end}`,
      label: range.label || '已报',
      left: style.left,
      type: overlap ? 'overlap' : 'reported',
      width: style.width,
    };
  });
  if (current.end > current.start) {
    const style = toStyle(current.start, current.end);
    items.push({
      key: 'CURRENT',
      label: '当前',
      left: style.left,
      type: hasReportRangeOverlap.value ? 'overlap' : 'current',
      width: style.width,
    });
  }
  return items;
});
const reportRangeText = computed(() =>
  `${formatNumber(currentReportRange.value.start)}-${formatNumber(currentReportRange.value.end)} m`,
);

async function submitReportAqcTask() {
  if (!currentPlan.planId || !currentPlan.planOperationId || !glueBoard.id) {
    message.warning('请先扫码计划并登记胶板领用');
    return undefined;
  }
  const sampleLength = Number(reportForm.processLength || 0);
  if (sampleLength <= 0) {
    message.warning('请先确认本次报工加工片数');
    return undefined;
  }
  const aqcTask = await submitCutRoundConsoleAqcTask({
    adhesiveReportId: undefined,
    glueBoardBatchNo: glueBoard.batchNo,
    glueBoardMaterialCode: glueBoard.materialCode,
    glueBoardUsageId: glueBoard.id,
    planId: currentPlan.planId,
    planOperationId: currentPlan.planOperationId,
    recordDate: reportForm.reportDate || dayjs().format('YYYY-MM-DD'),
    sampleLength,
    sampleStartPosition: Number(reportForm.startPosition || 0),
    submitterName: currentUserName.value || undefined,
    submitTime: buildNowText(),
    taskType: 'REPORT_FIRST_INSPECTION',
  });
  reportForm.aqcTaskId = aqcTask?.id;
  reportForm.aqcStatus = aqcTask?.taskStatus || 'WAITING';
  message.success('本次裁切首检记录已提交');
  return aqcTask;
}

async function submitCutRoundReport(options: SubmitCutRoundReportOptions = {}) {
  if (reportDialogMode.value === 'view') return;
  if (!currentPlan.planId || !currentPlan.planOperationId) {
    message.warning('请先扫码带出裁切计划');
    return;
  }
  const reportGroup = findSourceGroup(reportForm.parentBatchNo || currentPlan.sourceBatchNo || currentPlan.batchNo) || activeSourceGroup.value;
  if (isCurrentTaskReadonly.value || isSourceGroupCompleted(reportGroup)) {
    showReadonlyTaskWarning('保存报工', reportGroup);
    return;
  }
  if (isWorkOrderFinished.value) {
    message.warning('当前工单此工序已完工，不能再报工！');
    return;
  }
  if (!isWorkOrderRunning.value) {
    AModal.warning({
      okText: '去待加工开工',
      title: '当前裁切工单未开工',
      content: '请先在右上角“待加工”中选择当前计划并执行开工，再进行裁切报工登记。',
      onOk: () => {
        void openTaskList();
      },
    });
    return;
  }
  if (!reportForm.sourceGrindingSecondDetailId) {
    message.warning('请先通过来源扫码接口确认粘胶2片号');
    return;
  }
  if (!reportForm.processLength || reportForm.processLength <= 0) {
    message.warning('请填写加工片数');
    return;
  }
  const positionMessage = reportForm.reportType === 'PRODUCT' ? '' : getReportPositionValidationMessage();
  if (positionMessage) {
    message.warning(positionMessage);
    return;
  }
  const actualSizeRule = normalizeSizeRule(reportForm.actualSizeRule);
  if (!actualSizeRule) {
    message.warning('粘胶2已确认来源未携带实际尺寸，不能保存裁切报工');
    return;
  }
  reportForm.actualSizeRule = actualSizeRule;
  const finalProductionBatchNo =
    normalizeCutRoundProductionBatchNo(
      reportForm.productionBatchNo || reportForm.sourceProductionBatchNo,
      actualSizeRule,
    )
    || reportForm.productionBatchNo
    || reportForm.sourceProductionBatchNo;
  reportForm.productionBatchNo = finalProductionBatchNo;
  const outputLength = reportOutputLength.value;
  const visualItemsForSave = normalizeVisualItems(visualInspectionItems.value);
  const reportSelfCheck = hasActiveVisualItems.value ? 'NG' : (reportForm.selfCheck || 'OK');
  const sourceNgReviewResult = reportForm.sourceNgVisualItems.length > 0
    ? (hasActiveInheritedVisualItems.value ? 'NG' : 'OK')
    : '';
  reportForm.selfCheck = reportSelfCheck;
  reportForm.sourceNgReviewResult = sourceNgReviewResult;
  const visualInspectionTime = buildNowText();
  const actualModelCodeForSave = getReportFormProductModelCode() || currentCutRoundProductModelCode.value;
  const plannedModelCodeForSave = currentPlan.modelCode || '';
  const extra = applyPreProcessSelfCheckAttribution(
    {
      ...(parseRecordExtra(activeRecord.value || {}) as Record<string, any>),
      actualSizeRule,
      actualSizeSuffix: getCutRoundSizeSuffix(actualSizeRule),
      bladeLastReplaceTime: bladeConsumable.value?.lastReplaceTime || '',
      bladeUseCount: Number(bladeConsumable.value?.useCount || 0),
      feltLastReplaceTime: feltConsumable.value?.lastReplaceTime || '',
      feltUseCount: Number(feltConsumable.value?.useCount || 0),
      planSizeSpec: currentPlan.planSizeSpec,
      plannedModelCode: plannedModelCodeForSave,
      runtimeModelCode: actualModelCodeForSave,
      reportType: reportForm.reportType,
      reportTypeName: getCutRoundReportTypeText(reportForm.reportType),
      sourceNgDefectCode: reportForm.sourceNgDefectCode,
      sourceNgDefectSummary: reportForm.sourceNgDefectSummary,
      sourceNgReviewResult,
      sourceNgReviewTime: sourceNgReviewResult ? visualInspectionTime : '',
      sourceNgVisualItems: reportForm.sourceNgVisualItems,
      visualInspectionRemark: reportForm.remark || '',
      visualInspectionResult: reportSelfCheck,
      visualInspectionTime,
      visualItems: shouldShowVisualInspectionTab.value ? visualItemsForSave : [],
    },
    reportSelfCheck === 'NG' && reportForm.preProcessSelfCheckAbnormal,
    CUT_ROUND_PRE_PROCESS_ATTRIBUTION,
  );
  const savePayload: MesHcCutRoundConsoleApi.SaveReportReq = {
      id: activeRecord.value?.id,
      checkItems: getReportCheckItemsForSave().map((item, index) => ({
        abnormalRemark: item.abnormalRemark,
        actualValue: item.actualValue,
        checkResult: item.checkResult || 'OK',
        itemCategory: item.itemCategory,
        itemName: item.itemName,
        sortNo: Number(item.sortNo || index + 1),
        standardValue: item.standardValue,
      })),
      defectCode: reportForm.defectCode || undefined,
      endTime: reportForm.endTime || buildNowText(),
      extraJson: JSON.stringify(extra),
      aqcStatus: undefined,
      aqcTaskId: undefined,
      glueBoardBatchNo: undefined,
      glueBoardMaterialCode: undefined,
      glueBoardStartPosition: undefined,
      glueBoardUsageId: undefined,
      glueBoardUseLength: Number(reportForm.glueBoardUseLength || reportForm.processLength || 0),
      inputLength: Number(reportForm.processLength || 0),
      startPosition: Number(reportForm.startPosition || 0),
      endPosition: Number(reportForm.endPosition || 0),
      lossLength: Number(reportForm.lossLength || 0),
      materialCode: currentPlan.materialCode || undefined,
      modelCode: actualModelCodeForSave || undefined,
      napSampleLength: Number(reportForm.napSampleLength || 0),
      outputLength,
      parentProductionBatchNo: reportForm.parentBatchNo || undefined,
      planId: currentPlan.planId,
      planOperationId: currentPlan.planOperationId,
      productionBatchNo: finalProductionBatchNo || undefined,
      recorderName: currentUserName.value || undefined,
      recorderTime: buildNowText(),
      remark: reportForm.remark || undefined,
      reportDate: reportForm.reportDate || dayjs().format('YYYY-MM-DD'),
      selfCheck: reportSelfCheck,
      sourceBatchNo: reportForm.parentBatchNo || undefined,
      sourceGrindingSecondDetailId: reportForm.sourceGrindingSecondDetailId,
      sourceProductionBatchNo: reportForm.sourceProductionBatchNo || undefined,
      sourceType: '粘胶2片号',
      startTime: reportForm.startTime || buildNowText(),
  };
  try {
    const saveResult = options.confirmAfterSave
      ? await saveAndConfirmCutRoundConsoleReport({
          ...savePayload,
          actualSizeRule,
          confirmerName: currentUserName.value || undefined,
          confirmerTime: buildNowText(),
          scannedBatchNo: finalProductionBatchNo || reportForm.sourceProductionBatchNo || '',
        })
      : await saveCutRoundConsoleReport(savePayload);
    const reportId = resolveSavedReportId(saveResult, '裁切');
    await loadReports();
    await loadGlueBoardUsage();
    await loadSourceGroups();
    await loadIntermediateRecord();
    if (options.closeAfterSave !== false) {
      reportVisible.value = false;
      clearReportIntermediateDraft();
    }
    if (!options.silentSuccess) {
      message.success(options.confirmAfterSave
        ? '裁切报工已保存并扫码确认，可继续扫描下一张流转单'
        : '裁切报工已保存，可直接打印流转单；扫码确认可稍后执行');
    }
    if (options.confirmAfterSave && options.closeAfterSave !== false) {
      activeRecord.value = null;
      openRecordConfirm();
    }
    return reportId;
  } catch (error: any) {
    AModal.warning({
      content: getReportRequestErrorMessage(error, '裁切报工保存或确认失败，请检查登录状态后重试。'),
      title: options.confirmAfterSave ? '裁切保存并确认失败' : '裁切报工保存失败',
    });
    return undefined;
  }
}

function toggleExtendedBoardTabs() {
  showExtendedBoardTabs.value = !showExtendedBoardTabs.value;
  if (!showExtendedBoardTabs.value && activeBoardTab.value === 'CHECK') {
    activeBoardTab.value = 'SOURCE';
  }
  message.info(showExtendedBoardTabs.value ? '已显示点检/清洁记录' : '已隐藏点检/清洁记录');
}

watch(showExtendedBoardTabs, (visible) => {
  if (!visible && activeBoardTab.value === 'CHECK') {
    activeBoardTab.value = 'SOURCE';
  }
});

watch(
  () => [reportForm.lossLength, reportForm.napSampleLength],
  () => {
    reportForm.outputLength = reportOutputLength.value;
  },
);

watch(
  () => [reportForm.startPosition, reportForm.endPosition],
  () => {
    if (syncingReportPosition) return;
    const start = Number(reportForm.startPosition || 0);
    const end = Number(reportForm.endPosition || 0);
    if (Number.isFinite(start) && Number.isFinite(end) && end > start) {
      syncingReportPosition = true;
      reportForm.processLength = roundMeter(end - start);
      reportForm.outputLength = reportOutputLength.value;
      reportForm.glueBoardUseLength = reportForm.processLength;
      syncingReportPosition = false;
    }
  },
);

watch(
  () => [reportForm.reportType, reportCheckCategories.value.join('|'), shouldShowVisualInspectionTab.value],
  () => {
    if (reportStep.value === 'PROCESS') ensureActiveReportTab();
  },
);

watch(hasActiveVisualItems, (active) => {
  if (active) {
    reportForm.selfCheck = 'NG';
  }
});

watch(
  () => reportForm.selfCheck,
  (value) => {
    if (normalizeVisualResult(value) !== 'NG') {
      reportForm.preProcessSelfCheckAbnormal = false;
    }
  },
);

watch(scanPlanNo, (value) => schedulePlanScan(value));

watch(activeBoardTab, async (tab) => {
  if (tab !== 'SOURCE') {
    closeTransferPrintSelector();
  }
  if (tab === 'INTERMEDIATE' && currentPlan.planId && currentPlan.planOperationId) {
    await loadIntermediateRecord();
  }
  if (tab === 'INSPECTION_TASK' && currentPlan.planOperationId) {
    await loadInspectionTasks();
  }
  await nextTick();
  window.dispatchEvent(new Event('resize'));
});

watch([reportRecords, sourceGroups, currentSegmentBatchNo], () => {
  const selectableKeys = new Set(selectableTransferTargets.value.map((target) => getCutRoundTransferPrintTargetKey(target.segment)));
  selectedTransferReportIds.value = selectedTransferReportIds.value.filter((id) => selectableKeys.has(id));
  const allowedReportIds = new Set(
    currentSegmentReportRecords.value
      .map((record) => (record.id == null ? '' : String(record.id)))
      .filter(Boolean),
  );
  selectedReportRecordKeys.value = selectedReportRecordKeys.value.filter((id) => allowedReportIds.has(String(id)));
  inspectionTaskRows.value = inspectionTaskRows.value.filter((record) => isReportInCurrentSegment(record));
});

watch(reportScanConfirmDate, () => {
  if (currentPlan.planOperationId) {
    void loadReports();
  }
});

onActivated(() => {
  attachGlobalScannerListener();
});

onMounted(async () => {
  attachGlobalScannerListener();
  timer = setInterval(() => {
    currentDateTime.value = dayjs().format('YYYY-MM-DD HH:mm:ss');
  }, 1000);
  inspectionTaskRefreshTimer = setInterval(() => {
    void refreshInspectionTaskStatus({ silent: true });
  }, INSPECTION_TASK_REFRESH_INTERVAL_MS);
  await loadCheckTemplate();
  await loadTaskList();
  await ensureDefaultBoardEquipment();
  await loadDailyRecords();
  const planNo = String(route.query.planNo || '');
  if (planNo) {
    scanPlanNo.value = planNo;
    await consumePlanScan();
  }
  focusPlanScanInput();
});

onDeactivated(() => {
  detachGlobalScannerListener();
});

onBeforeUnmount(() => {
  if (timer) clearInterval(timer);
  if (inspectionTaskRefreshTimer) clearInterval(inspectionTaskRefreshTimer);
  if (planScanTimer) clearTimeout(planScanTimer);
  detachGlobalScannerListener();
});
</script>

<template>
  <Page auto-content-height :loading="boardLoading">
    <div class="cut-round-console" :class="{ 'is-visual-maximized': visualMaximized, 'has-sample-lock': !!currentSegmentSampleLockReason }">
      <div v-if="currentSegmentSampleLockReason" role="alert" class="sample-lock-notice rounded border border-amber-300 bg-amber-50 p-3 text-amber-800">
        {{ currentSegmentSampleLockReason }}
      </div>
      <div class="prototype-banner shrink-0 bg-white rounded-xl shadow-sm border border-slate-200 flex p-3 relative overflow-hidden">
        <div class="flex flex-1 items-center gap-4 min-w-0 pl-1">
          <div
            class="console-main-icon w-[60px] h-[60px] bg-gradient-to-br from-cyan-500 to-blue-600 rounded-xl shadow-md flex items-center justify-center shrink-0 text-white"
            title="双击显示/隐藏点检清洁和今日报工记录"
            @dblclick="toggleExtendedBoardTabs"
          >
            <IconifyIcon icon="lucide:layers" class="text-[32px]" />
          </div>
          <div class="console-title-block">
            <div class="console-title-row">
              <span class="console-title-text">裁切操作看板</span>
              <Tag color="processing" class="console-title-tag">裁切</Tag>
            </div>
            <div class="console-meta-row">
              <div class="console-meta-item console-meta-item--machine">
                <span class="console-meta-label">裁切机台</span>
                <strong class="console-meta-value">{{ selectedBoardEquipmentCode || '未绑定' }}</strong>
                <em class="console-meta-sub">{{ selectedBoardEquipmentName || '-' }}</em>
                <Button class="console-meta-action" size="small" type="link" @click.stop="openEquipmentSelect">切换</Button>
              </div>
              <div class="console-meta-item">
                <span class="console-meta-label">产线</span>
                <strong class="console-meta-value">{{ selectedBoardWorkCenterName }}</strong>
              </div>
            </div>
          </div>
        </div>
        <div class="inspection-stamp-slot cut-round-aqc-stamp-slot cut-round-stamp-group">
          <button
            v-if="false"
            class="inspection-stamp-side"
            :class="`inspection-stamp-side--${glueBoardAqcStatusMeta.stampClass}`"
            type="button"
            @click="openGlueBoardAqcDialog"
          >
            <span class="inspection-stamp-content">
              <span>粘胶检验</span>
              <strong>{{ glueBoardAqcStatusMeta.stampText }}</strong>
              <em>{{ glueBoardAqcStampTime }}</em>
            </span>
          </button>
          <button
            class="inspection-stamp-side"
            :class="`inspection-stamp-side--${changeoverStampMeta.stampClass}`"
            type="button"
            @click="() => openChangeoverInspectionScan()"
          >
            <span class="inspection-stamp-content">
              <span>工艺参数点检</span>
              <strong>{{ changeoverStampMeta.stampText }}</strong>
            </span>
          </button>
        </div>
        <div v-if="showExecutionClock" class="work-time-card">
          <div>{{ currentDateText }}</div>
          <strong>{{ currentTimeText }}</strong>
        </div>
        <div class="console-action-group flex items-center gap-2 pl-5 border-l border-slate-100 shrink-0">
          <div
            class="w-[64px] h-[64px] bg-cyan-50 border border-cyan-200 text-cyan-700 rounded-xl flex flex-col items-center justify-center cursor-pointer hover:bg-cyan-100 hover:shadow-md transition-all active:scale-95"
            @click="openDailyRecordList"
          >
            <IconifyIcon icon="lucide:clipboard-check" class="text-[22px] mb-1 opacity-80" />
            <span class="text-[11px] font-bold">点检清洁</span>
          </div>
          <div
            class="w-[64px] h-[64px] bg-slate-50 border border-slate-200 text-slate-700 rounded-xl flex flex-col items-center justify-center cursor-pointer hover:bg-slate-100 hover:shadow-md transition-all active:scale-95"
            @click="openTaskList"
          >
            <IconifyIcon icon="lucide:clipboard-list" class="text-[22px] mb-1 opacity-80" />
            <span class="text-[11px] font-bold">待加工</span>
          </div>
          <div
            v-if="false"
            class="w-[64px] h-[64px] bg-amber-50 border border-amber-200 text-amber-700 rounded-xl flex flex-col items-center justify-center cursor-pointer hover:bg-amber-100 hover:shadow-md transition-all active:scale-95"
            @click="openGlueBoardAqcDialog"
          >
            <IconifyIcon icon="lucide:shield-check" class="text-[22px] mb-1 opacity-80" />
            <span class="text-[11px] font-bold">粘胶检验</span>
          </div>
          <div
            v-if="!isWorkOrderPaused && !isWorkOrderCancelled"
            class="w-[64px] h-[64px] bg-orange-50 border border-orange-200 text-orange-700 rounded-xl flex flex-col items-center justify-center cursor-pointer hover:bg-orange-100 hover:shadow-md transition-all active:scale-95"
            @click="() => openChangeoverInspectionScan()"
          >
            <IconifyIcon icon="lucide:stamp" class="text-[22px] mb-1 opacity-80" />
            <span class="text-[11px] font-bold leading-tight text-center">工艺<br />参数点检</span>
          </div>
          <div
            v-if="!isWorkOrderPaused && !isWorkOrderCancelled"
            class="w-[64px] h-[64px] bg-rose-50 border border-rose-200 text-rose-700 rounded-xl flex flex-col items-center justify-center cursor-pointer hover:bg-rose-100 hover:shadow-md transition-all active:scale-95"
            :class="{ 'is-disabled': !currentPlan.planOperationId || isCurrentSegmentFinished || !!currentSegmentSampleLockReason }"
            @click="handleSegmentComplete"
          >
            <IconifyIcon icon="lucide:badge-check" class="text-[22px] mb-1 opacity-80" />
            <span class="text-[11px] font-bold">本段完成</span>
          </div>
          <div
            v-if="!isWorkOrderPaused && !isWorkOrderCancelled"
            class="w-[64px] h-[64px] bg-violet-50 border border-violet-200 text-violet-700 rounded-xl flex flex-col items-center justify-center cursor-pointer hover:bg-violet-100 hover:shadow-md transition-all active:scale-95"
            @click="openTransferPrintSelector"
          >
            <IconifyIcon icon="lucide:printer" class="text-[22px] mb-1 opacity-80" />
            <span class="text-[11px] font-bold">打印流转单</span>
          </div>
          <div
            v-if="!isWorkOrderPaused && !isWorkOrderCancelled"
            class="w-[64px] h-[64px] bg-sky-50 border border-sky-200 text-sky-700 rounded-xl flex flex-col items-center justify-center cursor-pointer hover:bg-sky-100 hover:shadow-md transition-all active:scale-95"
            :class="{ 'is-disabled': isCurrentTaskReadonly }"
            @click="openSelectedRecordConfirm"
          >
            <IconifyIcon icon="lucide:scan-line" class="text-[22px] mb-1 opacity-80" />
            <span class="text-[11px] font-bold">扫码确认</span>
          </div>
          <div
            v-if="!isWorkOrderPaused && !isWorkOrderCancelled"
            class="w-[64px] h-[64px] bg-blue-50 border border-blue-200 text-blue-700 rounded-xl flex flex-col items-center justify-center cursor-pointer hover:bg-blue-100 hover:shadow-md transition-all active:scale-95"
            :class="{ 'is-disabled': isCurrentTaskReadonly || oneClickScanConfirming }"
            @click="confirmAllCutRoundReportsOneClick"
          >
            <IconifyIcon icon="lucide:scan-barcode" class="text-[22px] mb-1 opacity-80" />
            <span class="text-[11px] font-bold leading-tight text-center">一键扫码<br />确认</span>
          </div>
          <div
            v-if="!isWorkOrderPaused && !isWorkOrderCancelled"
            class="w-[64px] h-[64px] bg-emerald-50 border border-emerald-200 text-emerald-700 rounded-xl flex flex-col items-center justify-center cursor-pointer hover:bg-emerald-100 hover:shadow-md transition-all active:scale-95"
            :title="currentSegmentSampleLockReason"
            :aria-disabled="!!currentSegmentSampleLockReason"
            :class="{ 'is-disabled': !!currentSegmentSampleLockReason }"
            @click="openInspectionTaskDialog"
          >
            <IconifyIcon icon="lucide:send" class="text-[22px] mb-1 opacity-80" />
            <span class="text-[11px] font-bold">提交检验</span>
          </div>
          <div
            v-if="!isWorkOrderPaused && !isWorkOrderCancelled"
            class="w-[64px] h-[64px] bg-teal-50 border border-teal-200 text-teal-700 rounded-xl flex flex-col items-center justify-center cursor-pointer hover:bg-teal-100 hover:shadow-md transition-all active:scale-95"
            @click="reportRecordListVisible = true"
          >
            <IconifyIcon icon="lucide:list-checks" class="text-[22px] mb-1 opacity-80" />
            <span class="text-[11px] font-bold">报工记录</span>
          </div>
        </div>
      </div>

      <section v-show="!visualMaximized" class="erp-card plan-scan-card">
        <div class="erp-card-title">
          <IconifyIcon icon="lucide:scan-line" />
          当前计划与扫码
        </div>
        <div class="erp-form-grid">
          <label>扫码计划</label>
          <div class="erp-input-line erp-scan-value">
            <Input
              ref="planScanInputRef"
              :value="scanPlanNo"
              allow-clear
              class="scan-input"
              placeholder="请扫码或输入计划号，达到长度后自动查询"
              @press-enter="consumePlanScan(false)"
              @update:value="handlePlanScanInput"
            />
          </div>
          <label>计划尺寸</label>
          <strong>
            <Tag :color="currentPlan.actualSizeRule === '740mm' ? 'green' : 'blue'" class="!m-0">
              {{ currentPlan.actualSizeRule || currentPlan.planSizeSpec || '-' }}
            </Tag>
            <span class="ml-1 text-xs text-slate-500">计划 {{ currentPlan.planSizeSpec || '-' }}</span>
          </strong>
          <label>分段批次</label><strong>{{ currentSegmentBatchNo || '-' }}</strong>
          <label>产品型号</label><strong>{{ currentCutRoundProductModelCode || '-' }}</strong>
          <label>产品料号</label><strong>{{ currentPlan.materialCode || '-' }}</strong>
          <label>可加工数</label><strong>{{ formatNumber(currentPlan.availableSourceLength) }} 片</strong>
          <label>状态</label>
          <strong>
            <Tag :color="currentPlan.planNo ? getWorkOrderStatusMeta(currentPlan.status).color : 'default'">
              {{ currentPlan.planNo ? getWorkOrderStatusMeta(currentPlan.status).text : '待扫码' }}
            </Tag>
          </strong>
          <label>执行要求</label><strong class="erp-requirement-value">{{ currentPlan.requirements || '暂无执行要求' }}</strong>
        </div>
      </section>

      <section v-show="!visualMaximized" class="erp-card glue-board-card">
        <div class="erp-card-title">
          <IconifyIcon icon="lucide:disc-3" />
          刀片 / 毛毡使用寿命
          <Tag :color="bladeConsumableMeta.color">刀片：{{ bladeConsumableMeta.text }}</Tag>
          <Tag :color="feltConsumableMeta.color">毛毡：{{ feltConsumableMeta.text }}</Tag>
          <Button size="small" type="primary" danger class="consumable-replace-btn" :disabled="isCurrentTaskReadonly" @click="openCutRoundConsumableReplaceDialog('CUTTING_BLADE')">更换刀片</Button>
          <Button size="small" type="primary" class="consumable-replace-btn" :disabled="isCurrentTaskReadonly" @click="openCutRoundConsumableReplaceDialog('CUTTING_FELT')">更换/复位毛毡</Button>
          <span class="glue-board-title-tip" :class="{ warning: hasCutRoundConsumableAttention }">
            {{ cutRoundConsumableTitleTip }}
          </span>
        </div>
        <div class="glue-board-layout glue-board-layout--plain">
          <div class="glue-board-form-grid glue-board-form-grid--cut-consumable">
            <div class="glue-board-field">
              <label>刀片寿命</label>
              <strong>{{ formatCutRoundConsumableCountText('CUTTING_BLADE', bladeConsumable, '次') }}</strong>
            </div>
            <div class="glue-board-field">
              <label>刀片上次更换</label>
              <strong>{{ formatDateText(bladeConsumable?.lastReplaceTime) }}</strong>
            </div>
            <div class="glue-board-field">
              <label>毛毡寿命(片)</label>
              <strong>{{ formatCutRoundConsumableCountText('CUTTING_FELT', feltConsumable, '片') }}</strong>
            </div>
            <div class="glue-board-field">
              <label>毛毡寿命(天)</label>
              <strong>{{ formatCutRoundConsumableDaysText(feltConsumable) }}</strong>
            </div>
            <div class="glue-board-field">
              <label>毛毡上次更换</label>
              <strong>{{ formatDateText(feltConsumable?.lastReplaceTime) }}</strong>
            </div>
          </div>
        </div>
      </section>

      <Tabs v-model:active-key="activeBoardTab" class="console-tabs">
        <template #rightExtra>
          <Button class="tab-maximize-button" size="small" @click="visualMaximized = !visualMaximized">
            <IconifyIcon :icon="visualMaximized ? 'lucide:minimize-2' : 'lucide:maximize-2'" />
            {{ visualMaximized ? '还原' : '最大化' }}
          </Button>
        </template>
        <TabPane key="SOURCE" tab="工作台">
          <section
            class="visual-panel press-slot-visual-panel"
            :class="{ 'visual-panel--blocked': workbenchBlockedReason }"
          >
            <div class="panel-title">
              <IconifyIcon icon="lucide:layout-grid" />
              工作台
              <div class="panel-filter-bar">
                <Input v-model:value="visualFilterForm.batchNo" allow-clear size="small" placeholder="扫码批次号/片号" />
                <Select
                  v-model:value="visualFilterForm.status"
                  class="panel-filter-select"
                  :options="cutRoundFilterStatusOptions"
                  size="small"
                  @change="setCutRoundFilterStatus"
                />
                <Select
                  v-if="false"
                  v-model:value="visualFilterForm.reportType"
                  class="panel-filter-select panel-filter-select--type"
                  :options="cutRoundReportTypeFilterOptions"
                  size="small"
                  @change="setCutRoundFilterReportType"
                />
                <Button class="panel-refresh-button" size="small" :loading="workbenchRefreshing" @click="refreshWorkbench">
                  <IconifyIcon icon="lucide:refresh-cw" />
                  刷新
                </Button>
              </div>
              <div class="panel-metrics">
                <span>来源 {{ visibleSourceGroups.length }} 组</span>
                <span>卡片 {{ visibleCutRoundSliceCount }}</span>
                <span>待确认 {{ visibleCutRoundPendingConfirmCount }}</span>
                <span>本工序NG {{ visibleCutRoundCurrentNgCount }}</span>
                <span>粘胶2来源NG {{ visibleCutRoundSourceNgCount }}</span>
                <span>已确认 {{ visibleCutRoundCompletedCount }}</span>
              </div>
              <div class="press-slot-card-legend" aria-label="裁切卡片图示说明">
                <span><i class="is-ok"></i>正常/确认</span>
                <span><i class="is-scan-pending"></i>待确认</span>
                <span><i class="is-pending"></i>待检</span>
                <span><i class="is-ng"></i>工序NG</span>
                <span><i class="is-middle"></i>中间品</span>
              </div>
            </div>
            <div v-if="transferPrintSelectionMode" class="transfer-print-bar">
              <div class="transfer-print-status">
                <IconifyIcon icon="lucide:mouse-pointer-click" />
                <span>已选 {{ selectedTransferReportCount }} / 可选 {{ selectableTransferReportCount }}</span>
              </div>
              <div class="transfer-print-actions">
                <Button size="small" @click="toggleAllTransferReports">
                  {{ allTransferReportsSelected ? '取消全选' : '全选' }}
                </Button>
                <Button size="small" @click="clearTransferReportSelection">清空</Button>
                <Button size="small" type="primary" :disabled="selectedTransferReportCount === 0" @click="handlePrintSelectedTransferReports">
                  打印选中
                </Button>
                <Button size="small" @click="closeTransferPrintSelector">退出</Button>
              </div>
            </div>
            <div v-if="!sourceGroups.length" class="empty-hint">
              <IconifyIcon icon="lucide:scan-line" />
              {{ currentPlan.planNo ? '暂无可加载的工作台卡片，请先确认前工序片号。' : '请先扫描计划号，系统会按后台已确认粘胶2片号生成工作台卡片。' }}
            </div>
            <div v-else-if="!visualDisplaySourceGroups.length" class="empty-hint">
              <IconifyIcon icon="lucide:filter-x" />
              当前过滤条件下没有匹配的工作台卡片。
            </div>
            <div v-else class="cloth-list press-slot-cloth-list" :class="{ 'is-print-select-mode': transferPrintSelectionMode }">
              <div v-for="group in visualDisplaySourceGroups" :key="group.baseBatchNo" class="cloth-source press-slot-cloth-source">
                <div class="cloth-source-header">
                  <div>
                    <strong>{{ group.baseBatchNo }}</strong>
                    <span>来源 {{ group.planNo || currentPlan.planNo || '-' }}</span>
                    <Tag v-if="group.qtime" :color="getCutRoundQtimeColor(group.qtime)" :title="group.qtime.message">
                      本批 QTIME {{ getCutRoundQtimeText(group.qtime) }}
                    </Tag>
                  </div>
                  <div v-if="transferPrintSelectionMode" class="source-actions">
                    <Button size="small" @click="selectGroupTransferReports(group)">全选本组</Button>
                  </div>
                </div>
                <div class="cloth-source-body">
                  <div class="slice-grid press-slot-slice-grid" :style="{ gridTemplateColumns: getCutRoundGridColumns(group.segments.length) }">
                    <div
                      v-for="segment in group.segments"
                      :key="segment.batchNo"
                      :class="getCutRoundSliceClass(segment)"
                      :title="getCutRoundSegmentRuntimeTitle(segment)"
                      role="button"
                      tabindex="0"
                      @click="handleVisualSegmentClick(group, segment)"
                      @keydown.enter="handleVisualSegmentClick(group, segment)"
                    >
                      <div class="slice-cell__head">
                        <span class="slice-cell__batch-wrap">
                          <span v-if="transferPrintSelectionMode" class="slice-select-mark">
                            <IconifyIcon :icon="isTransferSegmentSelected(segment) ? 'lucide:check' : 'lucide:square'" />
                          </span>
                          <strong class="slice-cell__batch">{{ getCutRoundSegmentDisplayBatchNo(segment) }}</strong>
                          </span>
                      </div>
                      <div class="slice-cell__info">
                        <span
                          v-if="shouldShowCutRoundScanConfirmLine(segment)"
                          class="slice-cell__line"
                          :class="getCutRoundScanConfirmLineClass(segment)"
                        >
                          <b>扫码</b><em>{{ getCutRoundScanConfirmText(segment) }}</em>
                        </span>
                        <span
                          v-for="entry in getCutRoundSegmentInspectionEntries(segment)"
                          :key="`${segment.batchNo}-${entry.label}-${entry.resultText}`"
                          class="slice-cell__line"
                          :class="entry.tone"
                        >
                          <b>{{ entry.label }}</b><em>{{ entry.resultText }}</em>
                        </span>
                        <span
                          v-if="shouldShowCutRoundSelfCheckLine(segment)"
                          class="slice-cell__line"
                          :class="getCutRoundSelfCheckLineClass(segment)"
                          :title="getCutRoundSegmentNgReason(segment)"
                        >
                          <b>{{ getCutRoundSelfCheckLabel(segment) }}</b><em>{{ getCutRoundSelfCheckText(segment) }}</em>
                        </span>
                        <span v-if="getCutRoundSegmentPositionText(segment)" class="slice-cell__line is-middle">
                          <b>中间品</b><em>{{ getCutRoundSegmentPositionText(segment) }}</em>
                        </span>
                        <span v-if="getCutRoundSegmentBatchChanged(segment)" class="slice-cell__line is-pending">
                          <b>改型</b><em>{{ getCutRoundSegmentBatchChangeText(segment) }}</em>
                        </span>
                        <span v-if="getCutRoundSegmentRuntimeMismatch(segment)" class="slice-cell__line is-pending">
                          <b>换型</b><em>{{ getCutRoundSegmentRuntimeText(segment) }}</em>
                        </span>
                      </div>
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
        <TabPane key="CHANGEOVER" tab="工艺参数点检">
          <div class="tab-table-content">
            <div class="console-table-shell">
              <div class="rough-grid-toolbar console-record-toolbar">
                <div class="rough-grid-toolbar__title">工艺参数点检记录</div>
                <div class="pass-work-actions rough-grid-toolbar__actions">
                  <span class="console-table-count">记录 {{ changeoverInspections.length }} 条</span>
                  <Button size="small" type="primary" @click="() => openChangeoverInspectionScan()">工艺参数点检</Button>
                </div>
              </div>
              <div class="console-table-body">
                <ATable
                  class="rough-check-table console-record-table"
                  :columns="changeoverInspectionColumns"
                  :data-source="changeoverInspections"
                  :pagination="false"
                  :scroll="{ x: 1120, y: 260 }"
                  row-key="id"
                  size="small"
                >
                  <template #bodyCell="{ column, record }">
                    <template v-if="column.dataIndex === 'recordTime'">
                      {{ displayDateTimeText(record.recordTime || record.submitTime) }}
                    </template>
                    <template v-if="column.dataIndex === 'inspectionStatus'">
                      <Tag :color="getChangeoverStatusMeta(record).color">{{ getChangeoverStatusMeta(record).text }}</Tag>
                    </template>
                    <template v-if="column.dataIndex === 'confirmerName'">
                      {{ getChangeoverConfirmerName(record) }}
                    </template>
                    <template v-if="column.dataIndex === 'action'">
                      <Button size="small" type="link" @click="viewChangeoverInspection(record)">查看</Button>
                      <Button danger size="small" type="link" :disabled="!record.id" @click="deleteChangeoverInspection(record)">删除</Button>
                    </template>
                  </template>
                </ATable>
              </div>
            </div>
          </div>
        </TabPane>
        <TabPane key="INSPECTION_TASK" tab="检测任务">
          <div class="tab-table-content">
            <div class="console-table-shell">
              <div class="rough-grid-toolbar console-record-toolbar">
                <div class="rough-grid-toolbar__title">裁切产品报检单</div>
                <div class="pass-work-actions rough-grid-toolbar__actions">
                  <span class="console-table-count">任务 {{ inspectionTasks.length }} 单 / 待提交 {{ inspectionEligibleReports.length }} 片</span>
                  <Button size="small" @click="() => Promise.all([loadReports(), loadInspectionTasks()])">刷新</Button>
                  <Button size="small" type="primary" :disabled="!!currentSegmentSampleLockReason" @click="openInspectionTaskDialog">提交检验</Button>
                </div>
              </div>
              <div class="console-table-body">
                <ATable
                  class="rough-check-table console-record-table inspection-task-table"
                  :columns="inspectionTaskColumns"
                  :data-source="inspectionTasks"
                  :pagination="false"
                  :scroll="{ x: 1680, y: 260 }"
                  row-key="id"
                  size="small"
                >
                  <template #bodyCell="{ column, record }">
                    <template v-if="column.dataIndex === 'fqcNo'">
                      <Button
                        v-if="getInspectionTaskFqcOrderId(record)"
                        size="small"
                        type="link"
                        @click="openInspectionTaskFqcDetail(record)"
                      >
                        {{ getInspectionTaskFqcNo(record) || '查看FQC' }}
                      </Button>
                      <strong v-else>-</strong>
                    </template>
                    <template v-if="column.dataIndex === 'taskStatus'">
                      <Tag :color="getInspectionStatusMeta(record.taskStatus).color" class="!m-0">
                        {{ getInspectionStatusMeta(record.taskStatus).text }}
                      </Tag>
                    </template>
                    <template v-if="column.dataIndex === 'reportProcess'">
                      {{ record.reportProcess || record.operationName || '裁切' }}
                    </template>
                    <template v-if="column.dataIndex === 'reportTime'">
                      {{ displayDateTimeText(record.reportTime) }}
                    </template>
                    <template v-if="column.dataIndex === 'remark'">
                      {{ record.remark || '-' }}
                    </template>
                    <template v-if="column.dataIndex === 'action'">
                      <Button size="small" type="link" @click="openInspectionTaskDetail(record)">查看详情</Button>
                    </template>
                  </template>
                </ATable>
              </div>
            </div>
          </div>
        </TabPane>
        <TabPane v-if="false" key="MIDDLE_LEDGER" tab="中间品台账">
          <div class="tab-table-content">
            <div class="console-table-shell">
              <div class="rough-grid-toolbar console-record-toolbar">
                <div class="rough-grid-toolbar__title">中间品台账</div>
                <div class="pass-work-actions rough-grid-toolbar__actions">
                  <span class="console-table-count">
                    分段批次号 {{ currentSegmentBatchNo || '-' }} / 记录 {{ cutRoundIntermediateLedgerRows.length }} 条
                  </span>
                </div>
              </div>
              <div class="console-table-body">
                <ATable
                  class="rough-check-table console-record-table"
                  :columns="cutRoundIntermediateLedgerColumns"
                  :data-source="cutRoundIntermediateLedgerRows"
                  :pagination="false"
                  :scroll="{ x: 2250, y: 220 }"
                  row-key="id"
                  size="small"
                />
              </div>
            </div>
          </div>
        </TabPane>
        <TabPane v-if="showExtendedBoardTabs" key="CHECK" tab="点检/清洁记录">
          <div class="tab-table-content">
            <ATable
              class="rough-check-table"
          :columns="dailyRecordColumns"
          :data-source="dailyRecordRows"
          :pagination="false"
          :scroll="{ x: 1130, y: 176 }"
          row-key="key"
          size="small"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.dataIndex === 'status'">
                  <Tag :color="getDailyRecordStatusMeta(record.status).color">
                    {{ getDailyRecordStatusMeta(record.status).text }}
                  </Tag>
                </template>
                <template v-if="column.dataIndex === 'recorderInfo'">
                  <div>{{ record.recorder || '-' }}</div>
                  <div class="rough-table-sub">{{ record.recorderTime || '-' }}</div>
                </template>
                <template v-if="column.dataIndex === 'confirmerInfo'">
                  <div>{{ record.confirmer || '-' }}</div>
                  <div class="rough-table-sub">{{ record.confirmerTime || '-' }}</div>
                </template>
                <template v-if="column.dataIndex === 'action'">
                  <div class="pass-work-actions">
                    <Button size="small" type="link" :disabled="!canFillDailyRecord(record)" @click="openDailyRecord(record, 'edit')">
                      填写
                    </Button>
                    <Button size="small" type="link" :disabled="!canConfirmDailyRecord(record)" @click="openDailyRecord(record, 'confirm')">
                      确认
                    </Button>
                    <Button size="small" type="link" :disabled="!canViewDailyRecord(record)" @click="openDailyRecord(record, 'view')">查看</Button>
                  </div>
                </template>
              </template>
            </ATable>
          </div>
    </TabPane>
        <TabPane key="RECORDS" tab="今日裁切报工记录">
      <div class="tab-table-content">
        <div class="console-table-shell">
          <div class="rough-grid-toolbar console-record-toolbar">
            <div class="rough-grid-toolbar__title">今日裁切报工记录</div>
            <div class="pass-work-actions rough-grid-toolbar__actions">
              <div class="console-record-date-filter">
                <span>扫码确认日期</span>
                <DatePicker
                  v-model:value="reportScanConfirmDate"
                  allow-clear
                  placeholder="扫码确认日期"
                  size="small"
                  value-format="YYYY-MM-DD"
                />
              </div>
              <span class="console-table-count">记录 {{ reportRecords.length }} 条 / 已选 {{ selectedReportRecords.length }} 条</span>
              <Button size="small" :disabled="isCurrentTaskReadonly || !reportRecords.length" @click="openSelectedRecordConfirm">扫码确认</Button>
              <Button size="small" :disabled="!printableReportRecords.length" @click="printSelectedCutRoundRecords">选择打印</Button>
              <Button size="small" :disabled="!allPrintableReportRecords.length" @click="printAllCutRoundRecords">打印全部报工</Button>
            </div>
          </div>
          <div class="console-table-body">
            <ATable
              class="rough-check-table console-record-table"
              :columns="recordColumns"
              :data-source="reportRecords"
              :pagination="false"
              :row-selection="reportRecordRowSelection"
              :scroll="{ x: 920, y: 176 }"
              row-key="id"
              size="small"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'motherBatchNo'">
                  {{ resolveMotherBatchNo(record) || '-' }}
                </template>
                <template v-if="column.key === 'scanConfirmDate'">
                  {{ formatReportScanConfirmDate(record) }}
                </template>
                <template v-if="column.dataIndex === 'reportStatus'">
                  <Tag :color="getRecordStatusMeta(record.reportStatus).color" class="!m-0">
                    {{ getRecordStatusMeta(record.reportStatus).text }}
                  </Tag>
                </template>
                <template v-if="column.dataIndex === 'inspectionStatus'">
                  <Tag :color="getInspectionStatusMeta(record.inspectionStatus, record.inspectionResult).color" class="!m-0">
                    {{ getInspectionStatusMeta(record.inspectionStatus, record.inspectionResult).text }}
                  </Tag>
                </template>
                <template v-if="column.dataIndex === 'printStatus'">
                  <Tag :color="getRecordPrintMeta(record).status === '已打印' ? 'success' : 'default'" class="!m-0">
                    {{ getRecordPrintMeta(record).status }}
                  </Tag>
                </template>
                <template v-if="column.dataIndex === 'printCount'">
                  {{ getRecordPrintMeta(record).count }}
                </template>
              </template>
            </ATable>
          </div>
        </div>
      </div>
    </TabPane>
    <TabPane v-if="false" key="INTERMEDIATE" tab="中间品记录单">
      <div class="tab-table-content press-slot-middle-tab">
        <div class="console-table-shell press-slot-middle-shell">
          <div class="rough-grid-toolbar console-record-toolbar">
            <div class="rough-grid-toolbar__title">CMP软垫(W26P0100 W26P0200 W26P0300)裁切中间品记录表</div>
            <div class="pass-work-actions rough-grid-toolbar__actions">
              <span class="console-table-count">
                投入 {{ formatNumber(intermediateForm.inputQty) }} PCS / 产出 {{ formatNumber(intermediateForm.outputQty) }} PCS
              </span>
              <Tag :color="intermediateForm.recordStatus === 'RECORDED' ? 'success' : 'default'">
                {{ intermediateForm.recordStatus === 'RECORDED' ? '已保存' : '草稿' }}
              </Tag>
              <Button size="small" :loading="intermediateLoading" @click="loadIntermediateRecord">刷新</Button>
              <Button size="small" type="primary" :loading="intermediateLoading" @click="saveIntermediateRecord">保存</Button>
            </div>
          </div>
          <div class="press-slot-middle-body">
            <div class="press-slot-middle-sheet">
              <div class="press-slot-section-title">
                <span>主表与采样片号</span>
                <em>点击裁切片卡中的首件 / 前段 / 中段 / 后段，可自动带入对应片号</em>
              </div>
              <div class="press-slot-middle-form">
                <label>生产日期</label>
                <div class="middle-product-header-input">
                  <DatePicker v-model:value="intermediateForm.recordDate" value-format="YYYY-MM-DD" size="small" />
                </div>
                <label>型号</label><strong>{{ intermediateForm.modelCode || '-' }}</strong>
                <label>料号</label><strong>{{ intermediateForm.materialCode || '-' }}</strong>
                <label>批号</label><strong>{{ intermediateForm.batchNo || '-' }}</strong>
                <label>投入数量/PCS</label><strong>{{ formatNumber(intermediateForm.inputQty) }}</strong>
                <label>产出数量/PCS</label><strong>{{ formatNumber(intermediateForm.outputQty) }}</strong>
                <label>担当</label>
                <Input v-model:value="intermediateForm.recorderName" size="small" />
                <label>确认</label>
                <Input v-model:value="intermediateForm.confirmerName" size="small" />
                <label>首检片号</label>
                <Input v-model:value="intermediateForm.firstSampleSliceNo" size="small" />
                <label>前段片号</label>
                <Input v-model:value="intermediateForm.frontSliceNo" size="small" @change="syncIntermediateDetailsFromSliceNoFields" />
                <label>中段片号</label>
                <Input v-model:value="intermediateForm.middleSliceNo" size="small" @change="syncIntermediateDetailsFromSliceNoFields" />
                <label>后段片号</label>
                <Input v-model:value="intermediateForm.endSliceNo" size="small" @change="syncIntermediateDetailsFromSliceNoFields" />
              </div>
            </div>
            <div class="press-slot-middle-sheet">
              <div class="press-slot-section-title">
                <span>首件厚度/mm（标准：1.520±0.050）</span>
                <em>首检片号对应的 X / Y 轴厚度最大值、最小值、平均值</em>
              </div>
              <div class="press-slot-depth-matrix">
                <div class="press-slot-depth-cell press-slot-depth-cell--head">轴向</div>
                <div class="press-slot-depth-cell press-slot-depth-cell--head">最小值</div>
                <div class="press-slot-depth-cell press-slot-depth-cell--head">最大值</div>
                <div class="press-slot-depth-cell press-slot-depth-cell--head">平均值</div>
                <div class="press-slot-depth-cell press-slot-depth-cell--axis">X轴</div>
                <InputNumber v-model:value="intermediateForm.firstSlotDepthXMin" class="full-input" :precision="3" size="small" />
                <InputNumber v-model:value="intermediateForm.firstSlotDepthXMax" class="full-input" :precision="3" size="small" />
                <InputNumber v-model:value="intermediateForm.firstSlotDepthXAvg" class="full-input" :precision="3" size="small" />
                <div class="press-slot-depth-cell press-slot-depth-cell--axis">Y轴</div>
                <InputNumber v-model:value="intermediateForm.firstSlotDepthYMin" class="full-input" :precision="3" size="small" />
                <InputNumber v-model:value="intermediateForm.firstSlotDepthYMax" class="full-input" :precision="3" size="small" />
                <InputNumber v-model:value="intermediateForm.firstSlotDepthYAvg" class="full-input" :precision="3" size="small" />
              </div>
            </div>
            <div class="press-slot-middle-sheet press-slot-middle-sheet--table">
              <div class="press-slot-section-title">
                <span>前段 / 中段 / 后段宽幅与厚度测量</span>
                <em>每片每 100cm 测量一次厚度，共 10 个测量点；厚度标准：1.520±0.050mm</em>
              </div>
              <div class="middle-product-detail-table press-slot-middle-detail-table">
                <ATable
                  class="rough-check-table"
                  :columns="intermediateDetailColumns"
                  :data-source="intermediateTableRows"
                  :loading="intermediateLoading"
                  :pagination="false"
                  :scroll="{ x: 2500, y: 260 }"
                  row-key="samplePosition"
                  size="small"
                >
                  <template #bodyCell="{ column, record }">
                    <template v-if="column.dataIndex === 'sliceBatchNo'">
                      <Input
                        v-model:value="record.sliceBatchNo"
                        class="cut-round-intermediate-cell"
                        size="small"
                        @change="syncIntermediateSliceNoFieldsFromDetails"
                        @keydown="handleIntermediateCellKeydown"
                      />
                    </template>
                    <template v-if="column.dataIndex === 'widthMm'">
                      <InputNumber v-model:value="record.widthMm" class="cut-round-intermediate-cell full-input" :min="0" :precision="3" size="small" @keydown="handleIntermediateCellKeydown" />
                    </template>
                    <template v-if="isIntermediateThicknessColumn(column.dataIndex)">
                      <InputNumber
                        :value="getIntermediateNumericValue(record, column.dataIndex)"
                        class="cut-round-intermediate-cell full-input"
                        :min="0"
                        :precision="3"
                        size="small"
                        @change="(value) => setIntermediateNumericValue(record, column.dataIndex, value as number | null)"
                        @keydown="handleIntermediateCellKeydown"
                      />
                    </template>
                    <template v-if="column.dataIndex === 'remark'">
                      <Input v-model:value="record.remark" class="cut-round-intermediate-cell" size="small" @keydown="handleIntermediateCellKeydown" />
                    </template>
                  </template>
                </ATable>
              </div>
            </div>
          </div>
        </div>
      </div>
    </TabPane>
    <TabPane v-if="false" key="EDGE_CONSUMABLE" tab="边库耗材登记">
      <div class="tab-table-content">
        <EdgeConsumableRegisterTab
          :plan-no="currentPlan.planNo"
          process-code="CUT_ROUND"
          :production-batch-no="currentSegmentBatchNo || currentMotherBatchNo || currentPlan.sourceProductionBatchNo || currentPlan.batchNo"
          :readonly="isCurrentTaskReadonly"
          :table-height="320"
        />
      </div>
    </TabPane>
    <TabPane key="INSTRUCTION_MESSAGES">
      <template #tab>
        <Badge :count="productionInstructionUnreadCount" :offset="[8, -4]" :overflow-count="99" size="small">
          <span>指令消息</span>
        </Badge>
      </template>
      <ProductionInstructionMessageTab
        :context="productionInstructionContext"
        title="裁切指令消息"
        @unread-change="handleProductionInstructionUnreadChange"
      />
    </TabPane>
  </Tabs>

      <AModal v-model:open="taskListVisible" :footer="null" :width="1280" class="rough-prototype-modal" :title="cutRoundTaskListTitle">
        <div class="console-table-shell console-table-shell--modal task-list-table-shell">
          <div class="rough-grid-toolbar console-record-toolbar">
            <div class="rough-grid-toolbar__title">{{ cutRoundTaskListTitle }}</div>
            <div class="pass-work-actions rough-grid-toolbar__actions">
              <span class="console-table-count">筛选 {{ taskListFilteredRows.length }} 条 / 共 {{ taskRows.length }} 条</span>
              <Button size="small" :loading="taskListLoading" @click="loadTaskList">刷新</Button>
            </div>
          </div>
          <div class="task-list-filter-bar">
            <div class="task-list-filter-item">
              <label>计划号</label>
              <Input
                v-model:value="taskListFilters.planNo"
                allow-clear
                placeholder="请输入计划号"
                size="small"
                @press-enter="handleTaskListSearch"
              />
            </div>
            <div class="task-list-filter-item">
              <label>产品型号</label>
              <Input
                v-model:value="taskListFilters.modelCode"
                allow-clear
                placeholder="请输入产品型号/料号"
                size="small"
                @press-enter="handleTaskListSearch"
              />
            </div>
            <div class="task-list-filter-item">
              <label>批次</label>
              <Input
                v-model:value="taskListFilters.batchNo"
                allow-clear
                placeholder="请输入分段批次"
                size="small"
                @press-enter="handleTaskListSearch"
              />
            </div>
            <div class="task-list-filter-item task-list-filter-item--status">
              <label>状态</label>
              <Select
                v-model:value="taskListFilters.status"
                :options="taskListStatusOptions"
                size="small"
              />
            </div>
            <div class="task-list-filter-actions">
              <Button size="small" type="primary" :loading="taskListLoading" @click="handleTaskListSearch">查询</Button>
              <Button size="small" @click="resetTaskListFilters">重置</Button>
            </div>
          </div>
          <div class="console-table-body">
            <ATable
              class="rough-check-table console-record-table task-list-auto-width-table"
              :columns="taskListColumns"
              :data-source="taskListPagedRows"
              :loading="taskListLoading"
              :pagination="false"
              :scroll="{ x: taskListScrollX, y: 366 }"
              :row-key="(record) => `${record.planOperationId}-${record.sourceBatchNo || record.planNo}`"
              size="small"
              :custom-row="(record) => ({ onDblclick: () => openStartTaskSizeRule(record) })"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.dataIndex === 'sourceBatchNo'">
                  {{ resolveMotherBatchNo(record) || '-' }}
                </template>
                <template v-if="column.dataIndex === 'planSizeSpec'">
                  {{ resolvePlanSizeSpec(record) || '-' }}
                </template>
                <template v-if="column.dataIndex === 'availableSourceLength'">
                  {{ formatNumber(record.availableSourceLength) }}
                </template>
                <template v-if="column.dataIndex === 'status'">
                  <Tag :color="getWorkOrderStatusMeta(record.status).color">{{ getWorkOrderStatusMeta(record.status).text }}</Tag>
                </template>
                <template v-if="column.dataIndex === 'action'">
                  <Button size="small" type="link" @click="openStartTaskSizeRule(record)">
                    {{ getTaskActionText(record.status) }}
                  </Button>
                </template>
              </template>
            </ATable>
          </div>
          <div class="task-list-pagination">
            <Pagination
              v-model:current="taskListPage"
              v-model:pageSize="taskListPageSize"
              :page-size-options="['10', '20', '50']"
              :show-total="(total) => `共 ${total} 条`"
              :total="taskListFilteredRows.length"
              show-less-items
              show-size-changer
              size="small"
            />
          </div>
        </div>
      </AModal>

      <AModal v-model:open="reportRecordListVisible" :footer="null" :width="1180" title="今日裁切报工记录">
        <div class="console-table-shell console-table-shell--modal">
          <div class="rough-grid-toolbar console-record-toolbar">
            <div class="rough-grid-toolbar__title">今日裁切报工记录</div>
            <div class="pass-work-actions rough-grid-toolbar__actions">
              <div class="console-record-date-filter">
                <span>扫码确认日期</span>
                <DatePicker
                  v-model:value="reportScanConfirmDate"
                  allow-clear
                  placeholder="扫码确认日期"
                  size="small"
                  value-format="YYYY-MM-DD"
                />
              </div>
              <span class="console-table-count">记录 {{ reportRecords.length }} 条 / 已选 {{ selectedReportRecords.length }} 条</span>
              <Button size="small" :disabled="isCurrentTaskReadonly || !reportRecords.length" @click="openSelectedRecordConfirm">扫码确认</Button>
              <Button size="small" :disabled="!printableReportRecords.length" @click="printSelectedCutRoundRecords">选择打印</Button>
              <Button size="small" :disabled="!allPrintableReportRecords.length" @click="printAllCutRoundRecords">打印全部报工</Button>
            </div>
          </div>
          <div class="console-table-body">
            <ATable
              class="rough-check-table console-record-table"
              :columns="recordColumns"
              :data-source="reportRecords"
              :pagination="false"
              :row-selection="reportRecordRowSelection"
              :scroll="{ x: 920, y: 420 }"
              row-key="id"
              size="small"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'motherBatchNo'">
                  {{ resolveMotherBatchNo(record) || '-' }}
                </template>
                <template v-if="column.key === 'scanConfirmDate'">
                  {{ formatReportScanConfirmDate(record) }}
                </template>
                <template v-if="column.dataIndex === 'reportStatus'">
                  <Tag :color="getRecordStatusMeta(record.reportStatus).color" class="!m-0">
                    {{ getRecordStatusMeta(record.reportStatus).text }}
                  </Tag>
                </template>
                <template v-if="column.dataIndex === 'inspectionStatus'">
                  <Tag :color="getInspectionStatusMeta(record.inspectionStatus, record.inspectionResult).color" class="!m-0">
                    {{ getInspectionStatusMeta(record.inspectionStatus, record.inspectionResult).text }}
                  </Tag>
                </template>
                <template v-if="column.dataIndex === 'printStatus'">
                  <Tag :color="getRecordPrintMeta(record).status === '已打印' ? 'success' : 'default'" class="!m-0">
                    {{ getRecordPrintMeta(record).status }}
                  </Tag>
                </template>
                <template v-if="column.dataIndex === 'printCount'">
                  {{ getRecordPrintMeta(record).count }}
                </template>
              </template>
            </ATable>
          </div>
        </div>
      </AModal>

      <AModal
        v-model:open="inspectionTaskDetailVisible"
        :footer="null"
        :title="null"
        width="100vw"
        wrap-class-name="hc-pass-work-modal rough-report-work-modal inspection-task-work-modal"
        @cancel="inspectionTaskDetailVisible = false"
      >
        <div class="report-modal-body inspection-task-modal-body inspection-task-view">
          <fieldset class="erp-fieldset report-modal-toolbar inspection-form-sheet">
            <legend>裁切产品报检单详情</legend>
            <div class="inspection-form-title">产品报检单</div>
            <div class="inspection-task-view-head">
              <label>FQC单号</label>
              <strong>
                <Button
                  v-if="getInspectionTaskFqcOrderId(selectedInspectionTask)"
                  size="small"
                  type="link"
                  @click="openInspectionTaskFqcDetail(selectedInspectionTask)"
                >
                  {{ getInspectionTaskFqcNo(selectedInspectionTask) || '查看FQC' }}
                </Button>
                <span v-else>-</span>
              </strong>
              <label>任务状态</label>
              <strong>
                <Tag :color="getInspectionStatusMeta(selectedInspectionTask?.taskStatus).color" class="!m-0">
                  {{ getInspectionStatusMeta(selectedInspectionTask?.taskStatus).text }}
                </Tag>
              </strong>
              <label>报检工序</label><strong>{{ selectedInspectionTask?.reportProcess || selectedInspectionTask?.operationName || '裁切' }}</strong>
              <label>报检日期</label><strong>{{ selectedInspectionTask?.reportDate || '-' }}</strong>
              <label>报检时间</label><strong>{{ displayDateTimeText(selectedInspectionTask?.reportTime) }}</strong>
              <label>报检人</label><strong>{{ selectedInspectionTask?.reporterName || '-' }}</strong>
              <label>备注说明</label><strong class="inspection-task-view-head__remark">{{ selectedInspectionTask?.remark || '-' }}</strong>
            </div>
          </fieldset>
          <div class="inspection-form-subtitle">
            <span>检验明细</span>
            <em>明细 {{ selectedInspectionTask?.details?.length || 0 }} 条</em>
          </div>
          <ATable
            class="rough-check-table inspection-task-detail-table"
            :columns="inspectionTaskDetailColumns"
            :data-source="selectedInspectionTask?.details || []"
            :pagination="false"
            :scroll="{ x: 1680, y: 520 }"
            row-key="id"
            size="small"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'fqcNo'">
                <Button v-if="record.fqcOrderId" size="small" type="link" @click="openFqcDetail(record)">
                  {{ record.fqcNo || '查看FQC' }}
                </Button>
                <span v-else>-</span>
              </template>
              <template v-if="column.dataIndex === 'fqcStatus'">
                <Tag :color="getInspectionStatusMeta(record.fqcStatus, record.fqcJudgment === 'NG' ? 'NG' : undefined).color">
                  {{ getInspectionStatusMeta(record.fqcStatus, record.fqcJudgment === 'NG' ? 'NG' : undefined).text }}
                </Tag>
              </template>
              <template v-if="column.dataIndex === 'fqcJudgment'">
                <Tag :color="getInspectionStatusMeta(undefined, record.fqcJudgment).color">
                  {{ record.fqcJudgment === 'PENDING' ? '待判定' : record.fqcJudgment || '待判定' }}
                </Tag>
              </template>
              <template v-if="column.dataIndex === 'inspectionResult'">
                <Tag :color="getInspectionStatusMeta(undefined, record.inspectionResult).color">
                  {{ record.inspectionResult || '待检' }}
                </Tag>
              </template>
              <template v-if="column.dataIndex === 'remark'">
                {{ record.remark || '-' }}
              </template>
              <template v-if="column.dataIndex === 'action'">
                <Button size="small" type="link" :disabled="!record.fqcOrderId" @click="openFqcDetail(record)">
                  查看FQC
                </Button>
              </template>
            </template>
          </ATable>
        </div>
        <div class="modal-footer report-action-footer">
          <Button @click="inspectionTaskDetailVisible = false">关闭</Button>
        </div>
      </AModal>

      <AModal
        v-model:open="inspectionTaskVisible"
        :footer="null"
        :title="null"
        width="100vw"
        wrap-class-name="hc-pass-work-modal rough-report-work-modal inspection-task-work-modal"
        @cancel="inspectionTaskVisible = false"
      >
        <div class="report-modal-body inspection-task-modal-body">
          <fieldset class="erp-fieldset report-modal-toolbar inspection-form-sheet">
            <legend>产品报检单</legend>
            <div class="inspection-form-title">产品报检单</div>
            <div class="inspection-form-head">
              <label>报检工序</label>
              <div class="inspection-form-control"><Input v-model:value="inspectionTaskForm.reportProcess" size="small" /></div>
              <label>报检日期</label>
              <div class="inspection-form-control"><DatePicker v-model:value="inspectionTaskForm.reportDate" value-format="YYYY-MM-DD" size="small" /></div>
              <label>报检时间</label>
              <div class="inspection-form-control"><Input v-model:value="inspectionTaskForm.reportTime" size="small" /></div>
              <label>报检人</label>
              <div class="inspection-form-control"><Input v-model:value="inspectionTaskForm.reporterName" size="small" /></div>
              <label>备注说明</label>
              <div class="inspection-form-control inspection-form-control--wide">
                <Input v-model:value="inspectionTaskForm.remark" allow-clear size="small" />
              </div>
            </div>
          </fieldset>
          <div class="inspection-form-subtitle">
            <span>检验明细</span>
            <em>提交前可移除本次不报检的裁切片</em>
          </div>
          <ATable
            class="rough-check-table inspection-task-detail-table"
            :columns="inspectionTaskCreateColumns"
            :data-source="inspectionTaskPreviewRows"
            :pagination="false"
            :scroll="{ x: 1300, y: 520 }"
            row-key="id"
            size="small"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'inspectionResult'">
                <Tag color="default">待检</Tag>
              </template>
              <template v-if="column.dataIndex === 'remark'">
                {{ record.remark || '-' }}
              </template>
              <template v-if="column.dataIndex === 'action'">
                <Button size="small" type="link" danger @click="removeInspectionTaskRow(record)">移除</Button>
              </template>
            </template>
          </ATable>
        </div>
        <div class="modal-footer report-action-footer">
          <Button @click="inspectionTaskVisible = false">关闭</Button>
          <Button type="primary" :loading="inspectionTaskSubmitting" :disabled="!!inspectionSampleLockReason" @click="submitInspectionTask">提交检验</Button>
        </div>
      </AModal>

      <AModal v-model:open="dailyRecordListVisible" :footer="null" :width="760" class="rough-prototype-modal" :title="`今日裁切点检/清洁记录 - ${currentEquipmentLabel}`">
        <div class="daily-check-card-grid">
          <div v-for="record in dailyRecordRows" :key="record.key" class="daily-check-card">
            <span class="daily-check-card__icon">
              <IconifyIcon :icon="record.name.includes('清洁') ? 'lucide:sparkles' : 'lucide:clipboard-check'" />
            </span>
            <span class="daily-check-card__content">
              <strong>{{ record.name }}</strong>
              <em>{{ record.timing }} / {{ getDailyRecordStatusMeta(record.status).text }}</em>
              <span>记录：{{ record.recorder }} {{ record.recorderTime }}</span>
            </span>
            <Tag :color="getDailyRecordStatusMeta(record.status).color" class="daily-check-card__tag">
              {{ getDailyRecordStatusMeta(record.status).text }}
            </Tag>
            <span class="daily-check-card__actions">
              <Button size="small" type="primary" :disabled="!canFillDailyRecord(record)" @click="openDailyRecord(record, 'edit')">填写</Button>
              <Button size="small" danger :disabled="!canConfirmDailyRecord(record)" @click="openDailyRecord(record, 'confirm')">确认</Button>
              <Button size="small" :disabled="!canViewDailyRecord(record)" @click="openDailyRecord(record, 'view')">查看</Button>
            </span>
          </div>
          <div v-if="dailyRecordRows.length === 0" class="console-empty compact">暂无点检清洁记录，请检查裁切动态表单配置。</div>
        </div>
        <div class="modal-footer daily-check-close-footer">
          <Button type="primary" @click="dailyRecordListVisible = false">已检关闭</Button>
        </div>
      </AModal>

      <AModal
        v-model:open="dailyRecordVisible"
        :footer="null"
      :title="null"
      width="100vw"
      wrap-class-name="hc-pass-work-modal rough-prep-work-modal"
      @cancel="dailyRecordVisible = false"
    >
        <div class="pp-plan-modal">
          <div class="pp-plan-toolbar">
            <div class="pp-plan-toolbar__title pass-work-dialog-title">
              <span class="pp-plan-toolbar__main">
                {{ dailyRecordMode === 'edit' ? '填写' : dailyRecordMode === 'confirm' ? '确认' : '查看' }}{{ selectedDailyRecord?.name || '工作准备记录' }}
              </span>
              <div class="toolbar-meta">
                <span>日期：{{ dayjs().format('YYYY-MM-DD') }}</span>
                <span>机台：{{ currentPlan.equipmentCode || '-' }} / {{ currentPlan.equipmentName || '-' }}</span>
                <span>状态：{{ selectedDailyRecord ? getDailyRecordStatusMeta(selectedDailyRecord.status).text : '-' }}</span>
              </div>
            </div>
            <div class="pp-plan-toolbar__actions">
              <Button v-if="dailyRecordMode === 'edit'" size="small" type="primary" @click="saveDailyRecord(false)">保存</Button>
              <Button v-if="dailyRecordMode === 'confirm'" size="small" type="primary" danger @click="saveDailyRecord(true)">确认</Button>
              <Button size="small" @click="dailyRecordVisible = false">关闭</Button>
            </div>
          </div>
          <div class="pp-plan-body">
            <fieldset class="pp-fieldset">
              <legend>表单信息</legend>
              <div class="pp-form-grid pass-work-form-grid">
                <div class="head-item"><span class="head-item__label">表单名称：</span><span class="head-item__value">{{ selectedDailyRecord?.name || '-' }}</span></div>
                <div class="head-item"><span class="head-item__label">执行时机：</span><span class="head-item__value">{{ selectedDailyRecord?.timing || '-' }}</span></div>
                <div class="head-item"><span class="head-item__label">记录人：</span><span class="head-item__value">{{ selectedDailyRecord?.recorder || '-' }}</span></div>
                <div class="head-item"><span class="head-item__label">记录时间：</span><span class="head-item__value">{{ selectedDailyRecord?.recorderTime || '-' }}</span></div>
                <div class="head-item"><span class="head-item__label">确认人：</span><span class="head-item__value">{{ selectedDailyRecord?.confirmer || '-' }}</span></div>
              </div>
            </fieldset>
            <div class="pp-panel pass-work-detail-panel">
              <div class="pp-panel__header"><span>明细项目</span></div>
              <div class="pp-table-wrap pass-work-detail-wrap">
                <div class="daily-check-result-tip">开机、清洁保养时遇到问题请记录在备注列说明情况。</div>
                <table class="pp-grid rough-detail-grid">
                  <thead>
                    <tr>
                      <th width="120">工序</th>
                      <th width="220">点检项目</th>
                      <th width="360">标准</th>
                      <th width="120">OK/NG</th>
                      <th width="260">备注</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="(row, index) in selectedDailyRecord?.details || []" :key="`${selectedDailyRecord?.key}-${row.seq}`">
                      <td v-if="getDetailCategoryRowSpan(selectedDailyRecord?.details || [], index) > 0" :rowspan="getDetailCategoryRowSpan(selectedDailyRecord?.details || [], index)">
                        {{ row.category || row.seq }}
                      </td>
                      <td>{{ row.item || '-' }}</td>
                      <td>{{ row.standard || '-' }}</td>
                      <td>
                        <RadioGroup v-if="dailyRecordMode !== 'view'" v-model:value="row.result" class="pp-radio-group" size="small">
                          <Radio value="OK">OK</Radio>
                          <Radio value="NG">NG</Radio>
                        </RadioGroup>
                        <span v-else>{{ row.result || '-' }}</span>
                      </td>
                      <td>
                        <Input
                          v-if="dailyRecordMode !== 'view'"
                          v-model:value="row.remark"
                          :data-daily-cell="`${index}-remark`"
                          size="small"
                          @keydown="handleDailyCellKeydown($event, index, 'remark')"
                        />
                        <span v-else>{{ row.remark || '-' }}</span>
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        </div>
      </AModal>

      <AModal v-if="false" v-model:open="glueConsumeVisible" :footer="null" :width="720" class="rough-prototype-modal" title="胶板领用登记">
        <div class="glue-consume-form">
          <div class="rough-material-scan-row">
            <div class="pp-form-item rough-material-scan-row__field">
          <label>胶板批号（选择/扫码）</label>
          <Input v-model:value="glueConsumeForm.materialScanCode" placeholder="扫码或输入胶板边库批号" @press-enter="applyGlueBoardScan" />
        </div>
        <Button size="small" type="primary" @click="applyGlueBoardScan">带出边库批次</Button>
      </div>
      <div class="pp-form-grid rough-material-scan-grid">
        <div class="pp-form-item"><label>当前料号</label><div class="pp-readonly-box">{{ glueBoard.materialCode }}</div></div>
        <div class="pp-form-item"><label>胶板批号</label><div class="pp-readonly-box">{{ glueConsumeForm.batchNo || '-' }}</div></div>
            <div class="pp-form-item"><label>边库余量</label><div class="pp-readonly-box">{{ glueBoardAvailableText }}</div></div>
            <div class="pp-form-item"><label>领用长度(m)</label><InputNumber v-model:value="glueConsumeForm.receiveLength" class="w-full" :min="0" :precision="3" /></div>
          </div>
          <div v-if="glueBoard.alarm" class="record-scan-confirm__error">{{ glueBoard.alarm }}</div>
          <div class="modal-footer">
            <Button @click="glueConsumeVisible = false">取消</Button>
            <Button type="primary" @click="confirmGlueConsume">确认领用</Button>
          </div>
        </div>
      </AModal>

      <AModal v-if="false" v-model:open="aqcVisible" :footer="null" :width="720" class="rough-prototype-modal" title="胶板粘胶检验">
        <div class="glue-consume-form">
          <div class="pp-form-grid rough-material-scan-grid">
            <div class="pp-form-item"><label>胶板料号</label><div class="pp-readonly-box">{{ glueBoard.materialCode || '-' }}</div></div>
        <div class="pp-form-item"><label>胶板批号</label><div class="pp-readonly-box">{{ glueBoard.batchNo || '-' }}</div></div>
        <div class="pp-form-item"><label>送检起位置(m)</label><InputNumber v-model:value="aqcForm.sampleStartPosition" class="w-full" :min="0" :precision="3" /></div>
        <div class="pp-form-item"><label>送检长度(m)</label><InputNumber v-model:value="aqcForm.sampleLength" class="w-full" :min="0" :precision="3" /></div>
      </div>
      <div class="modal-footer">
        <Button @click="aqcVisible = false">关闭</Button>
        <Button type="primary" @click="submitGlueBoardAqc">提交送检</Button>
      </div>
    </div>
  </AModal>

      <AModal v-if="false" v-model:open="glueLossVisible" :footer="null" :width="720" class="rough-prototype-modal" title="胶板损耗报备">
        <div class="glue-consume-form">
          <div class="pp-form-grid rough-material-scan-grid">
            <div class="pp-form-item"><label>胶板料号</label><div class="pp-readonly-box">{{ glueBoard.materialCode || '-' }}</div></div>
            <div class="pp-form-item"><label>胶板批号</label><div class="pp-readonly-box">{{ glueBoard.batchNo || '-' }}</div></div>
            <div class="pp-form-item"><label>当前可用区间</label><div class="pp-readonly-box">{{ glueBoardAvailableRange }}</div></div>
            <div class="pp-form-item"><label>已报损耗</label><div class="pp-readonly-box">{{ formatNumber(glueBoard.lossLength) }} m</div></div>
            <div class="pp-form-item"><label>损耗起位置(m)</label><InputNumber v-model:value="glueLossForm.startPosition" class="w-full" :min="0" :precision="3" @change="syncGlueLossEndFromLength" /></div>
            <div class="pp-form-item"><label>损耗止位置(m)</label><InputNumber v-model:value="glueLossForm.endPosition" class="w-full" :min="0" :precision="3" @change="syncGlueLossLengthFromEnd" /></div>
            <div class="pp-form-item"><label>损耗长度(m)</label><InputNumber v-model:value="glueLossForm.lossLength" class="w-full" :min="0" :precision="3" @change="syncGlueLossEndFromLength" /></div>
            <div class="pp-form-item pp-form-item--span-2"><label>损耗原因</label><Input v-model:value="glueLossForm.lossReason" placeholder="请输入损耗原因" /></div>
          </div>
          <div class="modal-footer">
            <Button @click="glueLossVisible = false">取消</Button>
            <Button type="primary" @click="submitGlueBoardLoss">确认保存</Button>
          </div>
        </div>
      </AModal>

      <AModal
        v-model:open="consumableReplaceVisible"
        :footer="null"
        :width="680"
        class="rough-prototype-modal"
        :title="consumableReplaceType === 'CUTTING_BLADE' ? '刀片更换登记' : '毛毡更换/复位登记'"
        :closable="!consumableSubmitting"
        :mask-closable="false"
      >
        <div class="glue-consume-form">
          <CutRoundBladeStockFields v-if="consumableReplaceType === 'CUTTING_BLADE'"
            v-model:ledger="bladeLedger" v-model:quantity="bladeReplaceQuantity" :disabled="consumableSubmitting || consumableAuthVisible" />
          <div class="consumable-info-grid">
            <div class="consumable-reason-form">
              <label>本次更换时间</label>
              <DatePicker
                v-model:value="consumableReplaceForm.replaceTime"
                :disabled="consumableSubmitting || consumableAuthVisible"
                class="consumable-number-input"
                format="YYYY-MM-DD HH:mm:ss"
                show-time
                value-format="YYYY-MM-DD HH:mm:ss"
              />
            </div>
            <div v-if="consumableReplaceType !== 'CUTTING_BLADE'" class="consumable-reason-form">
              <label>初始化片数</label>
              <InputNumber v-model:value="consumableReplaceForm.initialUseCount" :min="0" :precision="0" class="consumable-number-input" />
            </div>
          </div>
          <div class="consumable-reason-form">
            <label>更换原因</label>
            <Input.TextArea :disabled="consumableSubmitting || consumableAuthVisible" v-model:value="consumableReplaceForm.replaceReason" :rows="5" placeholder="请输入更换原因" />
          </div>
          <div v-if="activeCutRoundConsumableIssue" class="record-scan-confirm__error">{{ activeCutRoundConsumableIssue }}</div>
          <div class="modal-footer">
            <Button :disabled="consumableSubmitting" @click="consumableReplaceVisible = false">取消</Button>
            <Button type="primary" :loading="consumableSubmitting" :disabled="consumableAuthVisible" @click="confirmCutRoundConsumableReplace">{{ consumableReplaceType === 'CUTTING_BLADE' ? '确认更换并扣减' : '确认更换/复位' }}</Button>
          </div>
        </div>
      </AModal>

      <AModal
        v-model:open="equipmentSelectVisible"
        :footer="null"
        :width="820"
        class="rough-prototype-modal"
        title="选择裁切设备"
        @cancel="closeEquipmentSelect"
      >
        <div class="console-table-shell console-table-shell--modal">
          <div class="rough-grid-toolbar console-record-toolbar">
            <div class="rough-grid-toolbar__title">当前机台：{{ selectedBoardEquipmentCode || '未绑定' }} / {{ selectedBoardEquipmentName || '-' }}</div>
            <div class="pass-work-actions rough-grid-toolbar__actions">
              <Input
                v-model:value="equipmentSelectKeyword"
                allow-clear
                class="process-param-filter process-param-filter--slice"
                placeholder="设备编码/名称"
                size="small"
                @press-enter="loadEquipmentSelectRows"
              />
              <Button size="small" :loading="equipmentSelectLoading" @click="loadEquipmentSelectRows">刷新</Button>
            </div>
          </div>
          <div class="console-table-body">
            <ATable
              class="rough-check-table console-record-table"
              :columns="equipmentSelectColumns"
              :custom-row="(record) => ({ onDblclick: () => selectBoardEquipment(record) })"
              :data-source="equipmentSelectRows"
              :loading="equipmentSelectLoading"
              :pagination="false"
              :scroll="{ x: 700, y: 360 }"
              row-key="id"
              size="small"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.dataIndex === 'workStatus'">
                  <Tag :color="getEquipmentWorkStatusMeta(record.workStatus).color">
                    {{ getEquipmentWorkStatusMeta(record.workStatus).text }}
                  </Tag>
                </template>
                <template v-if="column.dataIndex === 'action'">
                  <Button size="small" type="link" @click="selectBoardEquipment(record)">选择</Button>
                </template>
              </template>
            </ATable>
          </div>
        </div>
      </AModal>

      <AuthModal
        v-model:visible="dailyRecordAuthVisible"
        auth-mode="username"
        :action-name="dailyRecordAuthAction"
        :equipment-id="selectedBoardEquipmentId"
        :workstation="selectedBoardEquipmentCode || selectedBoardEquipmentName || '裁切工位'"
        @cancel="handleDailyRecordAuthCancel"
        @success="handleDailyRecordAuthSuccess"
      />

      <AuthModal
        v-model:visible="consumableAuthVisible"
        auth-mode="username"
        :action-name="consumableAuthAction"
        :workstation="currentPlan.equipmentName || currentPlan.equipmentCode || '裁切工位'"
        @success="handleConsumableAuthSuccess"
      />

      <AuthModal
        v-model:visible="segmentCompleteAuthVisible"
        auth-mode="username"
        :action-name="segmentCompleteAuthActionName"
        :workstation="currentPlan.equipmentName || currentPlan.equipmentCode || '裁切工位'"
        title="确认本段是否完工？"
        @cancel="handleSegmentCompleteAuthCancel"
        @success="handleSegmentCompleteAuthSuccess"
      />

      <AuthModal
        v-model:visible="changeoverAuthVisible"
        auth-mode="username"
        :action-name="changeoverAuthAction"
        :equipment-id="selectedBoardEquipmentId"
        :workstation="currentPlan.equipmentName || currentPlan.equipmentCode || '裁切工位'"
        title="工艺参数点检认证"
        @cancel="handleChangeoverAuthCancel"
        @success="handleChangeoverAuthSuccess"
      />

      <AModal
        v-model:open="changeoverVisible"
        :closable="true"
        :footer="null"
        :z-index="1800"
        width="100vw"
        wrap-class-name="hc-pass-work-modal rough-report-work-modal"
        @cancel="changeoverVisible = false"
      >
        <div class="report-modal-body changeover-modal-body">
          <div class="changeover-modal-titlebar">
            <div class="changeover-modal-title">
              <span>{{ isChangeoverFormConfirmed ? '查看工艺参数点检' : '填写工艺参数点检' }}</span>
              <Tag :color="changeoverFormStatusMeta.color">
                {{ changeoverFormStatusMeta.text }}
              </Tag>
            </div>
            <div class="changeover-modal-actions">
              <Button :loading="changeoverExcelLoading" @click="handleChangeoverExportExcel">导出Excel</Button>
              <Button :disabled="isChangeoverFormConfirmed" :loading="changeoverExcelLoading" @click="triggerChangeoverImportExcel">导入Excel</Button>
              <Button :disabled="isChangeoverFormConfirmed" :loading="changeoverSubmitting" type="primary" @click="submitChangeoverInspection">保存</Button>
              <Button :disabled="isChangeoverFormConfirmed" :loading="changeoverConfirming" danger @click="confirmChangeoverInspection">确认</Button>
              <Button @click="changeoverVisible = false">关闭</Button>
            </div>
          </div>
          <input
            ref="changeoverExcelImportInputRef"
            accept=".xls,.xlsx,.xlsm"
            style="display: none"
            type="file"
            @change="handleChangeoverExcelImportChange"
          />
          <fieldset class="erp-fieldset report-modal-toolbar">
            <legend>工艺参数点检</legend>
            <div class="changeover-form-grid">
              <label>计划号</label><strong>{{ changeoverForm.currentPlanNo || currentPlan.planNo || '-' }}</strong>
              <label>当前型号</label><strong>{{ changeoverForm.productionModelCode || currentCutRoundProductModelCode || '-' }}</strong>
              <label>当前料号</label><strong>{{ changeoverForm.productionMaterialCode || currentPlan.materialCode || '-' }}</strong>
              <label>记录时间</label><strong>{{ displayDateTimeText(changeoverForm.recordTime) }}</strong>
              <label>母卷批号</label><strong>{{ changeoverForm.motherSegmentBatchNo || currentSegmentBatchNo || '-' }}</strong>
              <label>记录人</label><strong>{{ changeoverForm.recorderName || '-' }}</strong>
              <label>确认人</label><strong>{{ changeoverForm.confirmerName || '-' }}</strong>
              <label>备注</label><Input v-model:value="changeoverForm.remark" :disabled="isChangeoverFormConfirmed" class="changeover-remark-input" placeholder="请输入备注" />
            </div>
            <div v-if="changeoverImportAttachments.length" class="changeover-attachment-list">
              <span class="changeover-attachment-list__label">最近导入附件</span>
              <div class="changeover-attachment-list__items">
                <a
                  v-for="item in changeoverImportAttachments"
                  :key="`${item.name}-${item.uploadTime}`"
                  :href="item.url || item.path || undefined"
                  target="_blank"
                >
                  {{ item.name }} <small>{{ item.uploadTime || '-' }} / {{ formatAttachmentSize(item.size) }}</small>
                </a>
              </div>
            </div>
          </fieldset>
          <div class="changeover-check-panel">
              <ATable
                class="rough-check-table"
                :columns="reportProcessColumns"
                :data-source="changeoverForm.checkItems || []"
                :pagination="false"
                :scroll="{ x: 1120, y: 420 }"
                row-key="itemName"
                size="small"
              >
                <template #bodyCell="{ column, record, index }">
                  <template v-if="column.dataIndex === 'actualValue'">
                    <Input
                      v-model:value="record.actualValue"
                      class="cut-round-check-cell"
                      :data-report-check-field="'actualValue'"
                      :disabled="isChangeoverFormConfirmed"
                      placeholder="填写实际值"
                      @keydown="handleReportCheckKeydown($event, index, 'actualValue')"
                    />
                  </template>
                  <template v-if="column.dataIndex === 'abnormalRemark'">
                    <Input
                      v-model:value="record.abnormalRemark"
                      class="cut-round-check-cell"
                      :data-report-check-field="'abnormalRemark'"
                      :disabled="isChangeoverFormConfirmed"
                      placeholder="填写异常备注"
                      @keydown="handleReportCheckKeydown($event, index, 'abnormalRemark')"
                    />
                  </template>
                </template>
              </ATable>
          </div>
        </div>
      </AModal>

      <AModal
        v-model:open="recordConfirmVisible"
        :footer="null"
        :keyboard="false"
        :mask-closable="false"
        :width="900"
        class="rough-prototype-modal"
        destroy-on-close
        title="裁切报工扫码确认"
      >
        <div class="record-confirm-panel">
          <div class="record-confirm-summary">
            <span>当前计划</span><strong>{{ currentPlan.planNo || '-' }}</strong>
            <span>分段批次号</span><strong>{{ currentSegmentBatchNo || '-' }}</strong>
            <span>产品型号</span><strong>{{ recordConfirmModelCode }}</strong>
          </div>
          <div class="record-confirm-type-row">
            <em v-if="recordConfirmForm.sourceActualSizeRule">已自动继承粘胶2已确认尺寸字段。</em>
          </div>
          <div class="record-confirm-basis">
            最终裁切片号：{{ recordConfirmFinalBatchNo || '请先扫码流转单片号' }}
          </div>
          <div v-if="false" class="record-confirm-type-row">
            <span>作业类型</span>
            <RadioGroup v-model:value="recordConfirmForm.reportType">
              <Radio v-for="option in cutRoundReportTypeOptions" :key="option.value" :value="option.value">
                {{ option.label }}
              </Radio>
            </RadioGroup>
          </div>
          <div v-if="false" class="record-confirm-basis">
            {{ recordConfirmTypeBasis }}
          </div>
          <div v-if="false" class="record-confirm-type-row">
            <span>胶板型号</span>
            <RadioGroup v-model:value="recordConfirmForm.glueBoardModel" class="record-confirm-model-radio">
              <Radio v-for="option in GLUE_BOARD_MODEL_OPTIONS" :key="option.value" :value="option.value">
                {{ option.label }}
              </Radio>
            </RadioGroup>
          </div>
          <div class="record-confirm-scan">
            <IconifyIcon icon="lucide:scan-line" />
            <Input
              ref="recordConfirmScanInputRef"
              v-model:value="recordConfirmForm.scannedBatchNo"
              allow-clear
              data-cut-round-confirm-scan
              placeholder="请扫描流转单上的裁切片号，也可人工输入"
              @press-enter="confirmRecordScan"
            />
      </div>
      <div v-if="recordConfirmForm.error" class="record-scan-confirm__error">{{ recordConfirmForm.error }}</div>
      <div v-if="recordConfirmForm.message" class="record-scan-confirm__message">{{ recordConfirmForm.message }}</div>
      <div class="modal-footer">
            <Button @click="recordConfirmVisible = false">取消</Button>
            <Button type="primary" @click="confirmRecordScan">确认扫码</Button>
          </div>
        </div>
      </AModal>

      <AModal
        :open="reportVisible"
        :footer="null"
        :title="null"
        width="100vw"
        class="rough-prototype-modal"
        destroy-on-close
        wrap-class-name="hc-pass-work-modal rough-report-work-modal"
        @cancel="requestCloseReport"
      >
        <div class="report-modal-body">
          <fieldset class="erp-fieldset report-modal-toolbar">
            <legend>{{ reportDialogTitle }}</legend>
            <div class="press-slot-report-head press-slot-report-head--cut-round">
              <div v-if="false" class="press-slot-report-head__item press-slot-report-head__item--type">
                <label>作业类型</label>
                <div class="press-slot-report-head__value">
                  <strong class="press-slot-report-type-text">
                    {{ getCutRoundReportTypeText(reportForm.reportType) }}
                  </strong>
                </div>
              </div>
              <div class="press-slot-report-head__item">
                <label>分段批次号</label>
                <strong class="press-slot-report-head__value press-slot-report-head__value--batch">{{ reportForm.parentBatchNo || resolveMotherBatchNo({ sourceProductionBatchNo: reportForm.sourceProductionBatchNo }) || '-' }}</strong>
              </div>
              <div class="press-slot-report-head__item">
                <label>裁切片号</label>
                <strong class="press-slot-report-head__value press-slot-report-head__value--batch">{{ reportForm.productionBatchNo || '-' }}</strong>
              </div>
              <div class="press-slot-report-head__item">
                <label>产品型号</label>
                <strong class="press-slot-report-head__value">{{ getReportFormProductModelCode() || '-' }}</strong>
              </div>
              <div class="press-slot-report-head__item">
                <label>计划尺寸</label>
                  <strong class="press-slot-report-head__value">{{ currentPlan.planSizeSpec || '-' }}</strong>
              </div>
              <div v-if="false" class="press-slot-report-head__item">
                <label>COA标记</label>
                <strong class="press-slot-report-head__value" :class="{ warning: getCutRoundSegmentCoaFlag({ batchNo: reportForm.sourceProductionBatchNo } as SourceSegment) }">
                  {{ getCutRoundSegmentCoaFlag({ batchNo: reportForm.sourceProductionBatchNo } as SourceSegment) ? '成品COA专用' : '否' }}
                </strong>
              </div>
              <div v-if="isCutRoundInspectionReportType(reportForm.reportType)" class="press-slot-report-head__item press-slot-report-head__item--full">
                <label>工艺参数点检</label>
                <strong class="press-slot-report-head__value">
                  记录时间：{{ displayDateTimeText(reportInspectionInfo.submitTime) }}
                </strong>
              </div>
            </div>
          </fieldset>
          <Tabs v-model:active-key="activeReportTab" class="rough-report-tabs">
            <TabPane v-if="reportDialogMode === 'confirm' && reportStep === 'SCAN'" key="scan" tab="来源扫码">
              <div class="rough-material-scan-row">
                <div class="pp-form-item rough-material-scan-row__field">
                  <label>来源片号（扫码/输入）</label>
                <Input v-model:value="reportForm.sourceCode" placeholder="扫描粘胶2已确认片或NG片号" @press-enter="scanReportSource" />
                </div>
                <Button size="small" type="primary" @click="scanReportSource">扫码带出</Button>
              </div>
              <div class="rough-material-scan-hint">
                裁切确认流程：先扫码确认来源片号，再提交裁切 OK/NG 结果。
              </div>
              <div v-if="reportForm.sourceScanError" class="record-scan-confirm__error">{{ reportForm.sourceScanError }}</div>
              <div v-if="reportForm.sourceScanMessage" class="record-scan-confirm__message">{{ reportForm.sourceScanMessage }}</div>
              <div v-if="reportForm.sourceNgDefectSummary" class="record-confirm-basis">
                <div>粘胶2不良项目：{{ reportForm.sourceNgDefectSummary }}</div>
                <div class="mt-2 flex flex-wrap items-center gap-2">
                  <span>粘胶2缺陷码（自动带出）</span>
                  <Input :value="reportForm.sourceNgDefectCode || '-'" class="w-52" disabled size="small" />
                </div>
                <div class="mt-1 text-xs text-amber-700">缺陷项将在“外观检验”页自动点亮；保留后按裁切 NG 送检，取消全部继承项后按裁切合格流转。粘胶2原始缺陷始终保留追溯。</div>
              </div>
              <div v-if="reportForm.sourceDownstreamStatus" class="record-confirm-basis">
                裁切扫码报工状态：{{ reportForm.sourceDownstreamStatus }}
              </div>
              <div class="record-confirm-type-row cut-round-actual-size-row">
                <em v-if="reportForm.sourceActualSizeRule">已自动继承粘胶2已确认尺寸字段。</em>
              </div>
              <div class="record-confirm-basis">
                最终裁切片号：{{ reportFinalProductionBatchNo || '-' }}
              </div>
              <div class="pp-form-grid rough-material-scan-grid">
                <div class="pp-form-item"><label>来源片号</label><Input v-model:value="reportForm.sourceProductionBatchNo" disabled /></div>
                <div class="pp-form-item"><label>分段批次号</label><Input v-model:value="reportForm.parentBatchNo" disabled /></div>
                <div class="pp-form-item"><label>裁切片号</label><Input v-model:value="reportForm.productionBatchNo" disabled /></div>
                <div class="pp-form-item"><label>来源报工ID</label><Input :value="reportForm.sourceGrindingSecondDetailId || '-'" disabled /></div>
              </div>
            </TabPane>
            <TabPane v-for="category in reportCheckCategories" :key="`check-${category}`" :tab="category">
              <div class="report-tab-stack">
                <ATable
                  class="rough-check-table"
                  :columns="reportProcessColumns"
                  :data-source="getCheckItemsByCategory(category)"
                  :pagination="false"
                  row-key="itemName"
                  size="small"
                >
                  <template #bodyCell="{ column, record }">
                    <template v-if="column.dataIndex === 'actualValue'">
                      <Input
                        v-model:value="record.actualValue"
                        class="cut-round-check-cell"
                        :data-report-check-field="'actualValue'"
                        :disabled="reportDialogMode === 'view'"
                        placeholder="填写实际值"
                        @keydown="handleReportCheckKeydown($event, getCheckItemsByCategory(category).findIndex((item) => item.itemName === record.itemName), 'actualValue')"
                      />
                    </template>
                    <template v-if="column.dataIndex === 'abnormalRemark'">
                      <Input
                        v-model:value="record.abnormalRemark"
                        class="cut-round-check-cell"
                        :data-report-check-field="'abnormalRemark'"
                        :disabled="reportDialogMode === 'view'"
                        placeholder="填写异常备注"
                        @keydown="handleReportCheckKeydown($event, getCheckItemsByCategory(category).findIndex((item) => item.itemName === record.itemName), 'abnormalRemark')"
                      />
                    </template>
                  </template>
                </ATable>
              </div>
            </TabPane>
            <TabPane v-if="shouldShowVisualInspectionTab && (reportDialogMode === 'view' || reportStep === 'PROCESS')" key="visual-inspection" tab="外观检验">
              <div class="report-tab-stack press-slot-visual-tab">
                <div class="visual-check-grid">
                  <div v-for="item in visualInspectionItems" :key="item.itemName" :class="getVisualCheckItemClass(item)">
                    <button class="visual-light-button" type="button" :disabled="!isVisualInspectionEditable" @click="toggleVisualItem(item)">
                      <span class="visual-light-dot" :class="{ 'is-active': isVisualItemActive(item) }"></span>
                      <span>{{ item.itemName }}</span>
                      <span v-if="item.inheritedFromAdhesive2" class="ml-1 text-xs text-amber-600">粘胶2带入</span>
                    </button>
                    <Input
                      v-if="isVisualItemActive(item)"
                      v-model:value="item.remark"
                      allow-clear
                      class="visual-item-remark"
                      :disabled="!isVisualInspectionEditable"
                      placeholder="缺陷说明"
                    />
                  </div>
                </div>
                <div class="press-slot-visual-judge">
                  <div class="press-slot-visual-field">
                    <label>综合判断</label>
                    <div class="press-slot-visual-field__control">
                      <RadioGroup v-model:value="reportForm.selfCheck" :disabled="!isVisualInspectionEditable || hasActiveVisualItems">
                        <Radio value="OK">合格</Radio>
                        <Radio value="NG">异常</Radio>
                      </RadioGroup>
                      <span v-if="hasActiveVisualItems" class="visual-check-lock">已点亮缺陷项，综合判断自动为异常。</span>
                    </div>
                  </div>
                  <div
                    v-if="normalizeVisualResult(reportForm.selfCheck) === 'NG' || hasActiveVisualItems"
                    class="press-slot-visual-field"
                  >
                    <label>异常归属</label>
                    <div class="press-slot-visual-field__control">
                      <Checkbox
                        v-model:checked="reportForm.preProcessSelfCheckAbnormal"
                        :disabled="!isVisualInspectionEditable"
                      >
                        加工前自检异常
                      </Checkbox>
                    </div>
                  </div>
                  <div class="press-slot-visual-field press-slot-visual-field--remark">
                    <label>备注</label>
                    <Input.TextArea
                      v-model:value="reportForm.remark"
                      allow-clear
                      :disabled="!isVisualInspectionEditable"
                      :rows="4"
                      placeholder="填写外观检验备注"
                    />
                  </div>
                </div>
              </div>
            </TabPane>
            <TabPane v-if="reportDialogMode === 'view'" key="quality-result" tab="质检结果">
              <div class="report-tab-stack quality-result-tab">
                <div class="quality-result-grid">
                  <label>FQC单号</label>
                  <strong>
                    <Button
                      v-if="activeRecord?.fqcOrderId"
                      size="small"
                      type="link"
                      @click="openReportFqcDetail(activeRecord)"
                    >
                      {{ activeRecord?.fqcNo || '查看FQC' }}
                    </Button>
                    <span v-else>{{ activeRecord?.fqcNo || '-' }}</span>
                  </strong>
                  <label>检验状态</label>
                  <strong>
                    <Tag :color="getInspectionStatusMeta(activeRecord?.inspectionStatus, activeRecord?.inspectionResult).color" class="!m-0">
                      {{ getInspectionStatusMeta(activeRecord?.inspectionStatus, activeRecord?.inspectionResult).text }}
                    </Tag>
                  </strong>
                  <label>检验结果</label>
                  <strong>{{ activeRecord?.inspectionResult || '-' }}</strong>
                  <label>检验员</label>
                  <strong>{{ activeRecord?.inspectorName || '-' }}</strong>
                  <label>检验时间</label>
                  <strong>{{ displayDateTimeText(activeRecord?.inspectionTime) }}</strong>
                  <label>备注说明</label>
                  <strong class="quality-result-grid__remark">{{ activeRecord?.inspectionRemark || '-' }}</strong>
                </div>
              </div>
            </TabPane>
            <TabPane v-if="reportDialogMode === 'confirm' && reportStep === 'REPORT'" key="report" tab="裁切确认">
              <div class="report-tab-stack report-tab-stack--report">
                <div class="report-tab-scroll-body">
                  <fieldset class="erp-fieldset">
                    <legend>报工信息</legend>
                    <Form layout="vertical" class="report-form-grid">
                      <FormItem label="来源片号"><Input v-model:value="reportForm.sourceProductionBatchNo" disabled /></FormItem>
                      <FormItem label="裁切片号"><Input v-model:value="reportForm.productionBatchNo" disabled /></FormItem>
                      <FormItem v-if="false" label="起始位置(m)" class="report-required-field"><InputNumber v-model:value="reportForm.startPosition" :min="0" :precision="3" class="full-input" /></FormItem>
                      <FormItem v-if="false" label="结束位置(m)" class="report-required-field"><InputNumber v-model:value="reportForm.endPosition" :min="0" :precision="3" class="full-input" /></FormItem>
                      <FormItem label="裁切片数" class="report-required-field"><InputNumber :value="reportForm.processLength || 1" disabled :precision="0" class="full-input report-calculated-input" /></FormItem>
                      <FormItem v-if="false" label="损耗片数"><InputNumber v-model:value="reportForm.lossLength" :min="0" :precision="0" class="full-input" /></FormItem>
                      <FormItem v-if="false" label="留样片数"><InputNumber v-model:value="reportForm.napSampleLength" :min="0" :precision="0" class="full-input" /></FormItem>
                      <FormItem v-if="false" label="合格片数"><InputNumber :value="reportOutputLength" disabled class="full-input" /></FormItem>
                      <FormItem v-if="false" label="胶板料号"><Input v-model:value="reportForm.glueBoardMaterialCode" /></FormItem>
                      <FormItem v-if="false" label="胶板批号"><Input v-model:value="reportForm.glueBoardBatchNo" /></FormItem>
                      <FormItem v-if="false" label="胶板型号">
                        <RadioGroup
                          v-model:value="reportForm.glueBoardModel"
                          :disabled="reportDialogMode === 'view'"
                          class="report-toolbar-radio"
                        >
                          <Radio v-for="option in GLUE_BOARD_MODEL_OPTIONS" :key="option.value" :value="option.value">
                            {{ option.label }}
                          </Radio>
                        </RadioGroup>
                      </FormItem>
                      <FormItem label="自检">
                        <RadioGroup v-model:value="reportForm.selfCheck"><Radio value="OK">OK</Radio><Radio value="NG">NG</Radio></RadioGroup>
                      </FormItem>
                      <FormItem label="不良代码"><Select v-model:value="reportForm.defectCode" :options="defectOptions" class="full-input" /></FormItem>
                      <FormItem label="备注" class="report-remark"><Input v-model:value="reportForm.remark" /></FormItem>
                    </Form>
                  </fieldset>
                  <div v-if="false" class="second-range-visual">
                    <div class="second-range-visual__head">
                      <strong>裁切片号占用</strong>
                      <span>当前区间：{{ reportRangeText }}</span>
                      <span>来源总数：{{ formatNumber(reportRangeTotal) }} 片</span>
                      <span>可加工：{{ formatNumber(findSourceSegment(reportForm.sourceProductionBatchNo)?.availableLength) }} 片</span>
                      <em v-if="hasReportRangeOverlap">
                        与已报区间重叠：{{ formatNumber(reportOverlapRanges[0]?.start) }}-{{ formatNumber(reportOverlapRanges[0]?.end) }} m
                      </em>
                    </div>
                    <div class="second-range-bar">
                      <span class="second-range-bar__empty">未报区间</span>
                      <span
                        v-for="segment in reportRangeSegments"
                        :key="segment.key"
                        class="second-range-segment"
                        :class="`second-range-segment--${segment.type}`"
                        :style="{ left: `${segment.left}%`, width: `${segment.width}%` }"
                      >
                        {{ segment.label }}
                      </span>
                    </div>
                    <div class="second-range-legend">
                      <span><i class="is-empty"></i>未报</span>
                      <span><i class="is-reported"></i>已报</span>
                      <span><i class="is-current"></i>当前</span>
                      <span><i class="is-overlap"></i>重叠</span>
                    </div>
                    <div v-if="reportPositionWarning" class="report-position-hint warning">
                      {{ reportPositionWarning }}
                    </div>
                  </div>
                </div>
              </div>
            </TabPane>
          </Tabs>
        </div>
        <div class="modal-footer report-action-footer">
          <Button
            v-if="canCorrectAbnormalCategory"
            :loading="abnormalCategoryCorrectionSaving"
            @click="openAbnormalCategoryCorrection"
          >
            修正异常类别
          </Button>
          <Button @click="requestCloseReport">{{ reportDialogMode === 'view' ? '关闭' : '取消' }}</Button>
          <Button v-if="reportDialogMode === 'confirm' && reportStep === 'SCAN'" type="primary" @click="startReportWork">{{ startReportWorkButtonText }}</Button>
          <Button v-if="reportDialogMode === 'confirm' && reportStep === 'PROCESS'" type="primary" @click="submitProcessItems">{{ reportSubmitButtonText }}</Button>
          <Button
            v-if="reportDialogMode === 'confirm' && reportStep === 'REPORT'"
            type="primary"
            @click="() => submitCutRoundReport({ confirmAfterSave: true })"
          >
            保存并扫码确认
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
        title="修正裁切外观异常类别"
        width="720px"
        @cancel="closeAbnormalCategoryCorrection"
        @ok="submitAbnormalCategoryCorrection"
      >
        <Form layout="vertical">
          <FormItem label="裁切片号">
            <Input
              :value="activeRecord?.productionBatchNo || activeRecord?.sourceProductionBatchNo || '-'"
              disabled
            />
          </FormItem>
          <FormItem label="当前异常类别">
            <Input :value="getActiveVisualCategoryText(activeRecord)" disabled />
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
              :maxlength="200"
              placeholder="请填写修正原因"
              :rows="3"
              show-count
            />
          </FormItem>
        </Form>
      </AModal>
    </div>
    <FqcDetailModal />
  </Page>
</template>

<style scoped>
:deep(.vben-page),
:deep(.vben-page-content),
:deep(.vben-page-content-wrapper) {
  height: 100%;
  min-height: 0;
}

.cut-round-console {
  --industrial-border: #8794a4;
  --industrial-card: #e8edf3;
  display: grid;
  box-sizing: border-box;
  grid-template-rows:
    max-content
    max-content
    max-content
    minmax(0, 1fr);
  gap: 8px;
  width: 100%;
  min-width: 0;
  height: 100%;
  min-height: 0;
  padding: 8px;
  overflow: hidden;
  background:
    linear-gradient(90deg, rgba(30, 41, 59, 0.04) 1px, transparent 1px) 0 0 / 28px 28px,
    linear-gradient(0deg, rgba(30, 41, 59, 0.04) 1px, transparent 1px) 0 0 / 28px 28px,
    linear-gradient(180deg, #dfe6ee 0%, #f4f7fa 42%, #e8edf3 100%);
}

.cut-round-console.is-visual-maximized {
  grid-template-rows:
    max-content
    minmax(0, 1fr);
}

.prototype-banner {
  min-height: 0;
  padding: clamp(8px, 1vh, 12px) clamp(10px, 0.9vw, 12px) !important;
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

.console-main-icon {
  width: clamp(52px, 3.2vw, 60px) !important;
  height: clamp(52px, 3.2vw, 60px) !important;
}

.console-title-block {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  justify-content: center;
  gap: 0;
  min-width: 0;
  max-height: 60px;
  overflow: visible;
}

.console-title-row {
  display: flex;
  align-items: center;
  min-width: 0;
  height: 40px;
}

.console-title-text {
  overflow: hidden;
  color: #172033;
  font-size: 23px;
  font-weight: 950;
  line-height: 30px;
  letter-spacing: 0.03em;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.console-title-tag {
  display: inline-flex;
  align-items: center;
  height: 20px;
  margin-left: 10px;
  font-weight: 900;
  border: 0 !important;
  border-radius: 2px !important;
  box-shadow: none;
}

.console-meta-row {
  display: flex;
  align-items: center;
  min-width: 0;
  height: 22px;
  overflow: hidden;
}

.console-meta-item {
  display: inline-flex;
  align-items: center;
  min-width: 0;
  max-width: 360px;
  height: 22px;
  padding: 0 12px;
  color: #0f172a;
  line-height: 22px;
  text-align: left;
  background: transparent;
  border: 0;
  border-left: 1px solid rgba(95, 107, 122, 0.46);
  border-radius: 0;
}

.console-meta-item:first-child {
  padding-left: 0;
  border-left: 0;
}

.console-meta-item--machine {
  max-width: 420px;
}

.console-meta-action {
  flex: 0 0 auto;
  height: 22px;
  padding: 0 2px;
  font-size: 12px;
  font-weight: 800;
}

.console-meta-label {
  flex: 0 0 auto;
  margin-right: 8px;
  color: #075985;
  font-size: 12px;
  font-weight: 900;
  letter-spacing: 0.05em;
}

.console-meta-value {
  flex: 0 1 auto;
  overflow: hidden;
  color: #111827;
  font-family: Consolas, 'Microsoft YaHei', monospace;
  font-size: 14px;
  font-weight: 950;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.console-meta-sub {
  flex: 0 1 auto;
  overflow: hidden;
  margin-left: 7px;
  color: #475569;
  font-size: 12px;
  font-style: normal;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.console-action-group {
  flex-shrink: 1 !important;
  gap: clamp(5px, 0.42vw, 8px) !important;
  min-width: 224px;
  max-width: min(720px, 52vw);
  overflow-x: auto;
  overflow-y: hidden;
  padding-left: clamp(10px, 1vw, 20px) !important;
  padding-bottom: 2px;
  scrollbar-width: thin;
}

.console-action-group::-webkit-scrollbar {
  height: 4px;
}

.console-action-group::-webkit-scrollbar-thumb {
  background: rgba(100, 116, 139, 0.45);
  border-radius: 999px;
}

.console-action-group > div {
  flex: 0 0 64px;
  width: 64px !important;
  height: 64px !important;
  border-radius: 6px !important;
}

.console-action-group > div :deep(.iconify),
.console-action-group > div :deep(svg) {
  font-size: clamp(18px, 1.2vw, 22px) !important;
}

.console-action-group > div span {
  white-space: nowrap;
}

.inspection-stamp-slot {
  display: flex;
  align-self: stretch;
  flex: 0 0 198px;
  align-items: center;
  justify-content: center;
  height: auto;
  margin: -12px 0 -12px 12px;
  overflow: visible;
}

.inspection-stamp-side {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 168px;
  height: 100%;
  padding: 0;
  color: #31516f;
  cursor: pointer;
  background: transparent;
  border: 0;
  border-radius: 0;
  box-shadow: none;
}

.cut-round-stamp-group {
  flex-basis: clamp(278px, 18vw, 330px);
  gap: clamp(4px, 0.42vw, 8px);
}

.cut-round-stamp-group .inspection-stamp-side {
  width: clamp(126px, 8vw, 154px);
}

.cut-round-stamp-group .inspection-stamp-content em {
  max-width: 112px;
}

.inspection-stamp-side::before {
  position: absolute;
  inset: 0;
  pointer-events: none;
  content: '';
  background: linear-gradient(90deg, rgba(255, 255, 255, 0.18), rgba(15, 23, 42, 0.04));
  border-right: 2px solid currentColor;
  border-left: 2px solid currentColor;
  transform: skewX(-34deg);
  transform-origin: center;
  opacity: 0.9;
}

.inspection-stamp-content {
  position: relative;
  z-index: 1;
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
}

.inspection-stamp-content span {
  font-size: 10px;
  font-weight: 950;
  line-height: 1;
  letter-spacing: 0.18em;
}

.inspection-stamp-content strong {
  margin-top: 3px;
  font-size: 15px;
  font-weight: 950;
  line-height: 1;
  letter-spacing: 0.1em;
}

.inspection-stamp-content em {
  max-width: 132px;
  margin-top: 4px;
  overflow: hidden;
  font-family: Consolas, 'Microsoft YaHei', monospace;
  font-size: 10px;
  font-style: normal;
  font-weight: 900;
  line-height: 1;
  text-overflow: ellipsis;
  white-space: nowrap;
  opacity: 0.86;
}

.inspection-stamp-side--waiting {
  color: #5f6b7a;
}

.inspection-stamp-side--ok {
  color: #167a52;
}

.inspection-stamp-side--ng {
  color: #b43c3c;
}

.inspection-stamp-side--empty {
  color: #9a6a2f;
  background: transparent;
}

.work-time-card {
  display: flex;
  flex: 0 0 clamp(96px, 6.2vw, 118px);
  flex-direction: column;
  justify-content: center;
  height: clamp(54px, 3.4vw, 62px);
  padding: 0 clamp(8px, 0.7vw, 12px);
  margin-left: 8px;
  font-family: Consolas, 'Microsoft YaHei', monospace;
  color: #075985;
  text-align: center;
  background: transparent;
  border: 0;
  border-radius: 2px;
}

.work-time-card div {
  font-size: clamp(12px, 0.75vw, 14px);
  font-weight: 800;
  line-height: 20px;
}

.work-time-card strong {
  font-size: clamp(18px, 1.05vw, 20px);
  font-weight: 900;
  line-height: 24px;
}

.cut-round-console :deep(.ant-btn-primary) {
  background: linear-gradient(180deg, #0ea5e9 0%, #0369a1 100%);
  border-color: #075985;
  border-radius: 2px;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.28);
}

.cut-round-console :deep(.ant-btn) {
  border-radius: 2px;
}

.cut-round-console :deep(.ant-btn[disabled]),
.cut-round-console :deep(.ant-btn.ant-btn-disabled) {
  color: #64748b !important;
  text-shadow: none !important;
  background: #e2e8f0 !important;
  border-color: #cbd5e1 !important;
  box-shadow: none !important;
  opacity: 1;
}

.cut-round-console :deep(.ant-btn[disabled] *),
.cut-round-console :deep(.ant-btn.ant-btn-disabled *) {
  color: #64748b !important;
}

.cut-round-console .is-disabled {
  cursor: not-allowed;
  opacity: 0.48;
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
}

.plan-scan-card .erp-form-grid {
  flex: 0 0 auto;
  align-items: stretch;
  grid-template-columns: 7fr 18fr 7fr 18fr 7fr 18fr 7fr 18fr;
  grid-template-rows: repeat(2, minmax(32px, auto));
}

.erp-form-grid {
  display: grid;
  gap: 6px 8px;
  align-items: stretch;
}

.erp-form-grid label,
.consumable-erp-grid label {
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

.erp-form-grid strong,
.consumable-erp-grid strong {
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

.erp-full-value {
  grid-column: span 5;
}

.erp-full-value--wide {
  grid-column: span 7;
}

.erp-value-span-2 {
  grid-column: span 3 !important;
  width: 100%;
  min-width: 0;
}

.erp-input-line {
  display: flex;
  gap: 8px;
  align-items: stretch;
  min-width: 0;
  min-height: 32px;
}

.scan-input {
  flex: 1;
  width: 100% !important;
  max-width: none;
}

.glue-board-card {
  display: flex;
  flex-direction: column;
  padding: 8px 10px;
}

.glue-board-card .erp-card-title {
  height: 30px;
  margin-bottom: 8px;
  font-size: 15px;
}

.glue-board-card .erp-card-title::after {
  display: none;
}

.glue-board-card .erp-card-title :deep(.ant-tag) {
  padding: 2px 10px;
  font-size: 13px;
  font-weight: 900;
}

.consumable-replace-btn {
  height: 26px;
  padding: 0 12px;
  font-weight: 900;
  box-shadow: 0 1px 4px rgba(15, 23, 42, 0.18);
}

.glue-board-title-tip {
  display: flex;
  flex: 1;
  justify-content: flex-end;
  min-width: 0;
  overflow: hidden;
  color: #475569;
  font-size: 13px;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.glue-board-title-tip.warning {
  color: #b91c1c;
}

.glue-board-layout {
  display: grid;
  grid-template-columns: 42fr 58fr;
  gap: 8px;
  flex: 0 0 auto;
  min-height: auto;
  height: auto;
}

.glue-board-layout--plain {
  grid-template-columns: 1fr;
}

.glue-board-form-grid {
  display: grid;
  grid-template-columns: 7fr 18fr 7fr 18fr 7fr 18fr 7fr 18fr;
  grid-template-rows: minmax(32px, auto);
  gap: 6px 8px;
  align-items: stretch;
  min-height: auto;
}

.glue-board-form-grid--cut-consumable {
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 4px;
}

.glue-board-form-grid--cut-consumable .glue-board-field {
  display: grid;
  grid-template-columns: minmax(72px, 0.78fr) minmax(0, 1.22fr);
  min-width: 0;
}

.glue-board-field {
  display: contents;
}

.glue-board-form-grid--cut-consumable .glue-board-field label {
  justify-content: flex-end;
  min-height: 28px;
  padding: 4px 5px;
  border-right: 0;
  font-size: 12px;
  text-align: right;
}

.glue-board-form-grid--cut-consumable .glue-board-field strong {
  min-height: 28px;
  padding: 4px 5px;
  font-size: 12px;
}

.glue-board-field--highlight strong {
  background: linear-gradient(180deg, #ecfdf5, #dff6ea);
  border-color: #86b99a;
}

.glue-board-field label {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  min-width: 0;
  min-height: 32px;
  padding: 5px 8px;
  overflow: hidden;
  color: #334155;
  font-size: 13px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
  text-align: right;
  background: linear-gradient(180deg, #d7dee7 0%, #cbd5e1 100%);
  border: 1px solid #8794a4;
}

.glue-board-field strong {
  display: flex;
  align-items: center;
  min-width: 0;
  min-height: 32px;
  padding: 5px 8px;
  overflow: hidden;
  color: #0f172a;
  font-size: 13px;
  font-weight: 700;
  line-height: 20px;
  text-overflow: ellipsis;
  white-space: nowrap;
  background: linear-gradient(180deg, #f8fafc 0%, #edf2f7 100%);
  border: 1px solid #a2adba;
  box-shadow: inset 0 1px 2px rgba(15, 23, 42, 0.07);
}

.glue-board-aqc-status {
  gap: 6px;
}

.glue-board-aqc-status span {
  overflow: hidden;
  color: #64748b;
  font-family: Consolas, 'Microsoft YaHei', monospace;
  font-size: 12px;
  font-weight: 800;
  text-overflow: ellipsis;
}

.glue-board-message {
  min-height: 28px;
  padding: 5px 8px;
  overflow: hidden;
  color: #475569;
  font-size: 13px;
  font-weight: 800;
  line-height: 18px;
  text-overflow: ellipsis;
  white-space: nowrap;
  background: #f8fafc;
  border: 1px solid #a2adba;
}

.glue-board-message.warning {
  color: #b91c1c;
  background: #fef2f2;
  border-color: #fecaca;
}

.console-tabs {
  display: flex;
  flex-direction: column;
  align-self: stretch;
  box-sizing: border-box;
  width: 100%;
  max-width: 100%;
  height: 100%;
  max-height: 100%;
  min-height: 0;
  min-width: 0;
  margin-top: 0;
  padding: 0 10px 10px;
  overflow: hidden;
  background: linear-gradient(180deg, #eef3f8 0%, #dfe7f0 100%);
  border: 1px solid var(--industrial-border);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.76);
}

.console-tabs :deep(.ant-tabs-nav) {
  flex: 0 0 38px;
  height: 38px;
  margin-bottom: 8px;
}

.console-tabs :deep(.ant-tabs-tab) {
  color: #334155;
  font-weight: 800;
}

.console-tabs :deep(.ant-tabs-content-holder),
.console-tabs :deep(.ant-tabs-content),
.console-tabs :deep(.ant-tabs-tabpane) {
  flex: 1;
  width: 100%;
  max-width: 100%;
  height: 100%;
  min-height: 0;
  min-width: 0;
  overflow: hidden;
}

.tab-maximize-button {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  height: 28px;
  margin-right: 8px;
  border-radius: 2px;
}

.visual-panel {
  position: relative;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
  width: 100%;
  height: calc(100% - 8px);
  min-height: 0;
  overflow: hidden;
  background: linear-gradient(180deg, #f3f7fb 0%, #e6edf5 100%);
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

.panel-title {
  display: flex;
  flex: 0 0 40px;
  align-items: center;
  gap: 8px;
  min-height: 40px;
  padding: 0 10px;
  color: #075985;
  font-size: 15px;
  font-weight: 900;
  background: linear-gradient(180deg, #eef5fb 0%, #dce8f3 100%);
  border: 1px solid #aebed0;
  border-bottom: 0;
}

.panel-title > svg {
  font-size: 18px;
}

.panel-filter-bar {
  display: flex;
  flex: 0 0 520px;
  align-items: center;
  gap: 6px;
  width: 520px;
  min-width: 520px;
  max-width: 520px;
  margin-left: 10px;
}

.panel-filter-bar :deep(.ant-input-affix-wrapper) {
  flex: 0 1 145px;
  min-width: 0;
  height: 26px;
  border-radius: 999px;
}

.panel-filter-select {
  flex: 0 0 92px;
  min-width: 0;
}

.panel-filter-select--type {
  flex-basis: 150px;
}

.panel-filter-select :deep(.ant-select-selector) {
  height: 26px !important;
  border-radius: 999px !important;
}

.panel-filter-select :deep(.ant-select-selection-item) {
  font-size: 12px;
  font-weight: 900;
}

.panel-refresh-button {
  display: inline-flex;
  height: 26px;
  align-items: center;
  gap: 4px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 900;
}

.status-pill-group {
  display: flex;
  flex: 0 0 358px;
  align-items: center;
  gap: 4px;
  width: 358px;
  min-width: 358px;
  overflow: hidden;
}

.status-pill {
  flex: 0 0 66px;
  height: 24px;
  padding: 0;
  color: #475569;
  font-size: 12px;
  font-weight: 900;
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
  display: inline-flex;
  flex: 1;
  justify-content: flex-end;
  gap: 14px;
  min-width: 0;
  color: #1e293b;
  font-size: 13px;
  font-weight: 900;
  white-space: nowrap;
}

.press-slot-card-legend {
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 8px;
  margin-left: 8px;
  padding-left: 8px;
  color: #334155;
  font-size: 11px;
  font-weight: 800;
  line-height: 16px;
  white-space: nowrap;
  border-left: 1px solid #b8c6d8;
}

.press-slot-card-legend span {
  display: inline-flex;
  align-items: center;
  gap: 3px;
}

.press-slot-card-legend i {
  display: inline-block;
  width: 11px;
  height: 7px;
  border: 1px solid rgb(51 65 85 / 30%);
}

.press-slot-card-legend i.is-ok {
  background: #bbf7d0;
  border-color: #15803d;
}

.press-slot-card-legend i.is-pending {
  background: #fde68a;
  border-color: #d97706;
}

.press-slot-card-legend i.is-scan-pending {
  background: #dbeafe;
  border-color: #2563eb;
}

.press-slot-card-legend i.is-ng {
  background: #b91c1c;
  border-color: #7f1d1d;
}

.press-slot-card-legend i.is-middle {
  background: #eef2ff;
  border-color: #4f46e5;
  border-left-width: 3px;
  box-shadow: inset 3px 0 0 #4f46e5;
}

.transfer-print-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 38px;
  padding: 5px 10px;
  background: linear-gradient(180deg, #eef6ff 0%, #dceaf8 100%);
  border: 1px solid #aebed0;
  border-bottom: 0;
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

.empty-hint {
  display: grid;
  flex: 1;
  place-items: center;
  color: #64748b;
  font-size: 16px;
  text-align: center;
  background: #f3f7fb;
  border: 1px solid #aebed0;
}

.empty-hint svg {
  margin-bottom: 8px;
  color: #94a3b8;
  font-size: 42px;
}

.cloth-list {
  display: grid;
  flex: 1;
  min-height: 0;
  gap: 10px;
  padding: 10px;
  overflow: hidden;
  grid-auto-rows: minmax(160px, 1fr);
  grid-template-columns: 1fr;
  align-items: stretch;
  justify-content: stretch;
  background:
    linear-gradient(90deg, rgba(15, 23, 42, 0.03) 1px, transparent 1px) 0 0 / 22px 22px,
    linear-gradient(180deg, #f3f7fb 0%, #e6edf5 100%);
  border: 1px solid #aebed0;
}

.cloth-list.is-print-select-mode {
  align-content: start;
  overflow: auto;
  grid-auto-rows: minmax(180px, auto);
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
  background: #dce8f3;
  border: 1px solid #aebed0;
}

.cloth-source-header {
  display: flex;
  flex: 0 0 38px;
  align-items: center;
  justify-content: space-between;
  min-height: 38px;
  padding: 0 10px;
  background: #dce8f3;
  border-bottom: 1px solid #b8c6d8;
}

.cloth-source-header > div {
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
}

.cloth-source-header strong {
  color: #0d4f8b;
  font-size: 15px;
}

.cloth-source-header span {
  color: #34495e;
  font-size: 12px;
  font-weight: 800;
}

.source-actions {
  display: inline-flex;
  align-items: center;
  justify-content: flex-end;
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

.slice-grid {
  display: grid;
  min-height: 100%;
  align-content: start;
  flex: 1;
  gap: 6px;
  padding: 10px;
  background:
    radial-gradient(circle at 18% 14%, rgb(255 255 255 / 18%), transparent 18%),
    repeating-linear-gradient(0deg, rgb(255 255 255 / 8%) 0 1px, transparent 1px 10px),
    repeating-linear-gradient(90deg, rgb(15 76 117 / 10%) 0 1px, transparent 1px 10px),
    linear-gradient(135deg, #a6c8dc 0%, #79a8c4 100%);
  grid-auto-rows: 110px;
  justify-content: start;
}

.slice-cell {
  position: relative;
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  box-sizing: border-box;
  gap: 4px;
  overflow: hidden;
  padding: 8px;
  color: #19324b;
  text-align: left;
  cursor: pointer;
  background:
    linear-gradient(135deg, rgb(255 255 255 / 46%) 0%, rgb(255 255 255 / 10%) 36%, transparent 37%),
    repeating-linear-gradient(45deg, rgb(255 255 255 / 16%) 0 1px, transparent 1px 7px),
    repeating-linear-gradient(-45deg, rgb(19 72 105 / 10%) 0 1px, transparent 1px 7px),
    #d9ebf4;
  border: 1px solid rgb(43 90 124 / 74%);
  border-radius: 2px;
  box-shadow:
    inset 0 0 0 1px rgb(255 255 255 / 55%),
    inset 0 -12px 20px rgb(47 93 118 / 12%),
    0 1px 0 rgb(255 255 255 / 45%);
}

.slice-cell::before {
  position: absolute;
  inset: 7px;
  content: '';
  border: 1px solid rgb(255 255 255 / 36%);
  border-radius: 1px;
  pointer-events: none;
}

.slice-cell::after {
  position: absolute;
  top: 7px;
  right: 7px;
  width: 18px;
  height: 18px;
  content: '';
  border-top: 2px solid rgb(15 76 117 / 36%);
  border-right: 2px solid rgb(15 76 117 / 36%);
  pointer-events: none;
}

.slice-cell strong,
.slice-cell b,
.slice-cell em,
.slice-cell span,
.slice-cell small {
  position: relative;
  z-index: 1;
  color: inherit;
}

.slice-cell strong {
  font-size: 13px;
  font-weight: 800;
  letter-spacing: 0;
}

.slice-cell span,
.slice-cell small {
  font-size: 11px;
}

.slice-cell b,
.slice-cell em {
  font-style: normal;
}

.slice-cell__head {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 4px;
  width: 100%;
  min-width: 0;
}

.slice-cell__quick-actions {
  display: inline-flex;
  flex: none;
}

.slice-cell__quick-actions :deep(.ant-btn) {
  height: 22px;
  padding-inline: 3px;
  font-size: 12px;
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

.slice-cell__info {
  position: relative;
  z-index: 1;
  display: flex;
  min-width: 0;
  flex: 1;
  flex-direction: column;
  justify-content: flex-start;
  gap: 2px;
}

.slice-cell__line {
  display: grid;
  min-width: 0;
  align-items: center;
  grid-template-columns: minmax(0, 1fr) auto;
  column-gap: 4px;
  padding: 1px 4px;
  color: #17324a;
  line-height: 13px;
  background: rgb(241 245 249 / 68%);
  border-left: 2px solid rgb(51 65 85 / 45%);
}

.slice-cell__line b {
  min-width: 0;
  overflow: hidden;
  color: #334155;
  font-size: 10px;
  font-weight: 900;
  text-align: left;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.slice-cell__line em {
  min-width: 0;
  overflow: hidden;
  color: #0f172a;
  font-size: 10px;
  font-weight: 800;
  text-align: right;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.slice-cell__line.is-ok {
  background: rgb(220 252 231 / 76%);
  border-left-color: #15803d;
}

.slice-cell__line.is-pending {
  background: rgb(254 243 199 / 78%);
  border-left-color: #d97706;
}

.slice-cell__line.is-scan-pending {
  background: rgb(219 234 254 / 80%);
  border-left-color: #2563eb;
}

.slice-cell__line.is-middle {
  background: rgb(238 242 255 / 84%);
  border: 1px solid rgb(79 70 229 / 70%);
  border-left: 4px solid #4f46e5;
  box-shadow: inset 0 0 0 1px rgb(199 210 254 / 78%);
}

.slice-cell__line.is-ng,
.slice-cell__line.is-other-ng {
  background: #b91c1c;
  border-left-color: #7f1d1d;
}

.slice-cell__line.is-ng b,
.slice-cell__line.is-ng em,
.slice-cell__line.is-other-ng b,
.slice-cell__line.is-other-ng em {
  color: #fff;
}

.slice-cell__badges {
  display: flex;
  max-width: 100%;
  align-items: flex-start;
  justify-content: center;
  flex-wrap: wrap;
  gap: 2px 3px;
  margin-top: auto;
}

.slice-cell__badges:empty {
  display: none;
}

.slice-cell__meta {
  position: relative;
  z-index: 1;
  display: flex;
  min-width: 0;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  flex-wrap: nowrap;
  gap: 2px;
  text-align: center;
}

.slice-cell__type,
.slice-cell__status {
  display: block;
  width: fit-content;
  max-width: 100%;
  padding: 0 4px;
  overflow-wrap: anywhere;
  font-size: 10px;
  font-weight: 900;
  line-height: 14px;
  text-align: center;
  background: rgb(255 255 255 / 36%);
  border: 0;
  border-left: 3px solid currentColor;
  border-radius: 1px;
}

.slice-cell__badges:empty + .slice-cell__meta {
  margin-top: auto;
}

.slice-cell__meta {
  margin-bottom: auto;
}

.press-slot-sample-actions {
  position: relative;
  z-index: 2;
  display: inline-flex;
  justify-content: center;
  gap: 4px;
  margin-top: 2px;
}

.press-slot-sample-actions button {
  height: 20px;
  padding: 0 6px;
  color: #075985;
  font-size: 11px;
  font-weight: 900;
  line-height: 18px;
  cursor: pointer;
  background: linear-gradient(180deg, #eff6ff 0%, #dbeafe 100%);
  border: 1px solid #60a5fa;
  border-radius: 2px;
}

.press-slot-sample-actions button:hover {
  color: #fff;
  background: linear-gradient(180deg, #0ea5e9 0%, #0369a1 100%);
  border-color: #075985;
}

.slice-cell.is-pending {
  color: #7c2d12;
  background:
    linear-gradient(135deg, rgb(255 255 255 / 42%) 0%, rgb(255 255 255 / 12%) 34%, transparent 35%),
    repeating-linear-gradient(45deg, rgb(255 255 255 / 16%) 0 1px, transparent 1px 7px),
    repeating-linear-gradient(-45deg, rgb(146 64 14 / 10%) 0 1px, transparent 1px 7px),
    #ffd6a3;
  border-color: rgb(194 110 28 / 72%);
}

.slice-cell.is-scan-pending {
  color: #0f2f5f;
  background:
    linear-gradient(135deg, rgb(255 255 255 / 46%) 0%, rgb(255 255 255 / 10%) 34%, transparent 35%),
    repeating-linear-gradient(45deg, rgb(255 255 255 / 16%) 0 1px, transparent 1px 7px),
    repeating-linear-gradient(-45deg, rgb(37 99 235 / 10%) 0 1px, transparent 1px 7px),
    #c7dcff;
  border-color: rgb(37 99 235 / 76%);
  box-shadow:
    inset 0 0 0 2px rgb(37 99 235 / 28%),
    inset 0 -12px 20px rgb(30 64 175 / 12%);
}

.slice-cell.is-confirmed {
  color: #14532d;
  cursor: not-allowed;
  background:
    linear-gradient(135deg, rgb(255 255 255 / 46%) 0%, rgb(255 255 255 / 10%) 36%, transparent 37%),
    repeating-linear-gradient(45deg, rgb(255 255 255 / 18%) 0 1px, transparent 1px 7px),
    repeating-linear-gradient(-45deg, rgb(21 128 61 / 10%) 0 1px, transparent 1px 7px),
    #a9efbf;
  border-color: rgb(22 101 52 / 72%);
  box-shadow:
    inset 0 0 0 2px rgb(22 163 74 / 28%),
    0 0 12px rgb(34 197 94 / 24%);
}

.slice-cell.is-transfer-selectable {
  cursor: pointer;
  padding-top: 8px;
}

.slice-cell.is-transfer-disabled {
  cursor: not-allowed;
  opacity: 0.58;
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
  z-index: 4;
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

.slice-cell.is-inspecting {
  color: #075985;
  background:
    linear-gradient(135deg, rgb(255 255 255 / 45%) 0%, rgb(255 255 255 / 12%) 34%, transparent 35%),
    repeating-linear-gradient(45deg, rgb(255 255 255 / 18%) 0 1px, transparent 1px 7px),
    repeating-linear-gradient(-45deg, rgb(14 116 144 / 13%) 0 1px, transparent 1px 7px),
    #bae6fd;
  border-color: rgb(2 132 199 / 78%);
  box-shadow:
    inset 0 0 0 2px rgb(14 165 233 / 28%),
    0 0 12px rgb(14 165 233 / 22%);
}

.slice-cell.is-inspected {
  color: #064e3b;
  background:
    linear-gradient(135deg, rgb(255 255 255 / 45%) 0%, rgb(255 255 255 / 12%) 34%, transparent 35%),
    repeating-linear-gradient(45deg, rgb(255 255 255 / 18%) 0 1px, transparent 1px 7px),
    repeating-linear-gradient(-45deg, rgb(5 150 105 / 12%) 0 1px, transparent 1px 7px),
    #99f6e4;
  border-color: rgb(13 148 136 / 78%);
  box-shadow:
    inset 0 0 0 2px rgb(20 184 166 / 28%),
    0 0 12px rgb(20 184 166 / 20%);
}

.slice-cell.is-coa {
  border-color: #dc2626;
  box-shadow:
    inset 0 0 0 2px rgb(220 38 38 / 58%),
    inset 0 0 0 5px rgb(254 226 226 / 72%),
    0 0 0 2px rgb(220 38 38 / 25%),
    0 8px 16px rgb(127 29 29 / 24%);
}

.slice-cell.is-runtime-mismatch {
  padding-bottom: 8px;
  border-color: #f97316;
  box-shadow:
    inset 0 0 0 2px rgb(249 115 22 / 44%),
    inset 0 0 0 5px rgb(255 237 213 / 62%),
    0 0 0 2px rgb(249 115 22 / 22%),
    0 8px 16px rgb(154 52 18 / 20%);
}

.slice-cell__runtime-badge {
  position: static;
  z-index: 4;
  display: inline-flex;
  max-width: 100%;
  min-height: 0;
  align-items: center;
  justify-content: center;
  padding: 1px 4px;
  color: #fff;
  font-size: 10px;
  font-style: normal;
  font-weight: 950;
  line-height: 13px;
  overflow-wrap: anywhere;
  text-align: center;
  background: linear-gradient(135deg, #f97316 0%, #c2410c 100%);
  border: 1px solid rgb(255 255 255 / 82%);
  border-radius: 1px;
  box-shadow: none;
}

.slice-cell__runtime-line {
  display: block;
  max-width: calc(100% - 10px);
  justify-self: center;
  padding: 1px 6px;
  overflow: hidden;
  color: #9a3412 !important;
  font-size: 10px !important;
  font-weight: 900;
  line-height: 14px;
  text-overflow: ellipsis;
  white-space: nowrap;
  background: rgb(255 247 237 / 88%);
  border: 1px solid rgb(251 146 60 / 58%);
  border-radius: 1px;
}
.slice-cell__coa {
  position: static;
  z-index: 3;
  display: inline-flex;
  min-width: 34px;
  max-width: 100%;
  min-height: 0;
  align-items: center;
  justify-content: center;
  padding: 1px 4px;
  color: #fff;
  font-family: Arial, 'Microsoft YaHei', sans-serif;
  font-size: 11px;
  font-style: normal;
  font-weight: 950;
  letter-spacing: 0.03em;
  line-height: 13px;
  text-shadow: none;
  background: linear-gradient(135deg, #dc2626 0%, #991b1b 100%);
  border: 1px solid rgb(255 255 255 / 86%);
  border-radius: 1px;
  box-shadow: none;
}

.slice-cell__ng {
  position: static;
  z-index: 4;
  display: inline-flex;
  max-width: 100%;
  min-height: 0;
  align-items: center;
  justify-content: center;
  padding: 1px 4px;
  color: #fff;
  font-size: 10px;
  font-style: normal;
  font-weight: 800;
  line-height: 13px;
  overflow-wrap: anywhere;
  text-align: center;
  background: #dc2626;
  border-radius: 1px;
  box-shadow: none;
}

.slice-cell.is-type-product {
  border-color: #2563eb;
}

.slice-cell.is-type-front {
  border-color: #0f766e;
}

.slice-cell.is-type-middle {
  border-color: #7c3aed;
}

.slice-cell.is-type-end {
  border-color: #db2777;
}

.slice-cell.is-type-process {
  border-color: #ea580c;
}

.slice-cell.is-type-changeover {
  border-color: #be123c;
}

.slice-cell.is-inspection-ng {
  color: #7f1d1d;
  background:
    linear-gradient(135deg, rgb(255 255 255 / 38%) 0%, rgb(255 255 255 / 10%) 34%, transparent 35%),
    repeating-linear-gradient(45deg, rgb(255 255 255 / 16%) 0 1px, transparent 1px 7px),
    #fecaca;
  box-shadow:
    inset 0 0 0 3px rgb(220 38 38 / 42%),
    0 0 14px rgb(220 38 38 / 22%);
}

.slice-cell.is-visual-ng {
  color: #7f1d1d;
  background:
    linear-gradient(135deg, rgb(255 255 255 / 40%) 0%, rgb(255 255 255 / 10%) 34%, transparent 35%),
    repeating-linear-gradient(45deg, rgb(255 255 255 / 15%) 0 1px, transparent 1px 7px),
    repeating-linear-gradient(-45deg, rgb(153 27 27 / 10%) 0 1px, transparent 1px 7px),
    #f7b4b4;
  border-color: rgb(185 28 28 / 76%);
  box-shadow:
    inset 0 0 0 3px rgb(220 38 38 / 36%),
    0 0 14px rgb(239 68 68 / 24%);
}

.slice-cell.is-source-ng {
  cursor: not-allowed;
  background:
    linear-gradient(135deg, rgb(255 255 255 / 42%) 0%, rgb(255 255 255 / 10%) 34%, transparent 35%),
    repeating-linear-gradient(45deg, rgb(255 255 255 / 15%) 0 1px, transparent 1px 7px),
    repeating-linear-gradient(-45deg, rgb(127 29 29 / 14%) 0 1px, transparent 1px 7px),
    #fecaca;
  border-color: rgb(153 27 27 / 82%);
  box-shadow:
    inset 0 0 0 3px rgb(185 28 28 / 36%),
    0 0 14px rgb(185 28 28 / 24%);
}

.slice-cell__ng.is-source-ng {
  background: #991b1b;
}

.slice-cell.is-inspection-bg-ok {
  color: #14532d;
  background:
    linear-gradient(135deg, rgb(255 255 255 / 46%) 0%, rgb(255 255 255 / 10%) 36%, transparent 37%),
    repeating-linear-gradient(45deg, rgb(255 255 255 / 18%) 0 1px, transparent 1px 7px),
    repeating-linear-gradient(-45deg, rgb(21 128 61 / 10%) 0 1px, transparent 1px 7px),
    #a9efbf;
  border-color: rgb(22 101 52 / 76%);
  box-shadow:
    inset 0 0 0 3px rgb(22 163 74 / 30%),
    0 0 14px rgb(34 197 94 / 22%);
}

.slice-cell.is-inspection-bg-pending {
  color: #7c2d12;
  background:
    linear-gradient(135deg, rgb(255 255 255 / 44%) 0%, rgb(255 255 255 / 12%) 34%, transparent 35%),
    repeating-linear-gradient(45deg, rgb(255 255 255 / 18%) 0 1px, transparent 1px 7px),
    repeating-linear-gradient(-45deg, rgb(146 64 14 / 10%) 0 1px, transparent 1px 7px),
    #fde68a;
  border-color: rgb(217 119 6 / 78%);
  box-shadow:
    inset 0 0 0 3px rgb(217 119 6 / 34%),
    0 0 14px rgb(217 119 6 / 20%);
}

.slice-cell.is-inspection-bg-ng {
  color: #7f1d1d;
  background:
    linear-gradient(135deg, rgb(255 255 255 / 38%) 0%, rgb(255 255 255 / 10%) 34%, transparent 35%),
    repeating-linear-gradient(45deg, rgb(255 255 255 / 16%) 0 1px, transparent 1px 7px),
    repeating-linear-gradient(-45deg, rgb(153 27 27 / 12%) 0 1px, transparent 1px 7px),
    #fecaca;
  border-color: rgb(185 28 28 / 80%);
  box-shadow:
    inset 0 0 0 3px rgb(220 38 38 / 42%),
    0 0 14px rgb(220 38 38 / 24%);
}

.slice-cell.is-type-product,
.slice-cell.is-type-front,
.slice-cell.is-type-middle,
.slice-cell.is-type-end,
.slice-cell.is-type-process,
.slice-cell.is-type-changeover {
  border-width: 3px;
}

.source-board-list {
  display: grid;
  grid-auto-rows: minmax(126px, auto);
  gap: 10px;
  height: calc(100% - 8px);
  min-height: 0;
  overflow-y: auto;
}

.source-group-board {
  display: grid;
  grid-template-rows: 34px minmax(0, 1fr);
  gap: 8px;
  min-height: 0;
  padding: 10px;
  background:
    linear-gradient(90deg, rgba(15, 23, 42, 0.035) 1px, transparent 1px) 0 0 / 22px 22px,
    linear-gradient(180deg, #f5f8fb 0%, #e1e9f1 100%);
  border: 1px solid #7d8b9b;
}

.source-group-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 0 8px;
  background: linear-gradient(180deg, #f8fafc 0%, #e5edf5 100%);
  border: 1px solid #8794a4;
}

.source-group-head strong {
  color: #075985;
  font-size: 18px;
  font-family: Consolas, 'Courier New', monospace;
}

.source-group-head span {
  color: #334155;
  font-size: 13px;
  font-weight: 800;
}

.segmented-cloth-strip {
  display: grid;
  grid-template-columns: repeat(4, 1fr) 0.86fr;
  min-height: 0;
  overflow: hidden;
  border: 1px solid #475569;
  box-shadow:
    inset 0 0 0 1px rgba(255, 255, 255, 0.85),
    inset 0 10px 20px rgba(255, 255, 255, 0.42),
    0 6px 0 rgba(15, 23, 42, 0.1);
}

.wafer-pad-strip {
  background:
    radial-gradient(circle at 18px 18px, rgba(15, 23, 42, 0.08) 0 1px, transparent 1.4px) 0 0 / 18px 18px,
    repeating-linear-gradient(90deg, rgba(15, 23, 42, 0.07) 0 1px, transparent 1px 32px),
    repeating-linear-gradient(0deg, rgba(71, 85, 105, 0.16) 0 1px, transparent 1px 13px),
    linear-gradient(180deg, #f8fafc 0%, #cbd5e1 48%, #94a3b8 100%);
}

.cloth-segment {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-width: 0;
  color: #fff;
  cursor: pointer;
  background: rgba(3, 105, 161, 0.42);
  border: 0;
  border-right: 2px solid rgba(30, 41, 59, 0.72);
}

.cloth-segment:last-child {
  border-right: 0;
}

.cloth-segment.empty {
  background: rgba(100, 116, 139, 0.36);
}

.cloth-segment:disabled {
  cursor: not-allowed;
  opacity: 0.58;
}

.cloth-segment.stock {
  background: rgba(15, 23, 42, 0.5);
}

.cloth-segment strong,
.cloth-segment em,
.cloth-segment span,
.cloth-segment small {
  width: fit-content;
  max-width: 92%;
  padding: 3px 10px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  background: rgba(15, 23, 42, 0.72);
}

.cloth-segment strong {
  font-size: 18px;
}

.cloth-segment em,
.cloth-segment span,
.cloth-segment small {
  margin-top: 4px;
  font-size: 12px;
  font-style: normal;
}

.cut-round-position-strip {
  position: absolute;
  right: 8px;
  bottom: 26px;
  left: 8px;
  height: 10px;
  overflow: hidden;
  pointer-events: none;
  background: rgba(226, 232, 240, 0.62);
  border: 1px solid rgba(15, 23, 42, 0.38);
}

.cut-round-position-strip i {
  position: absolute;
  top: 0;
  bottom: 0;
  display: block;
  background: repeating-linear-gradient(
    90deg,
    rgba(249, 115, 22, 0.98) 0 8px,
    rgba(234, 88, 12, 0.98) 8px 12px
  );
  border-right: 1px solid rgba(124, 45, 18, 0.72);
}

.cut-round-position-strip i.confirmed {
  background: repeating-linear-gradient(
    90deg,
    rgba(22, 163, 74, 0.96) 0 8px,
    rgba(21, 128, 61, 0.96) 8px 12px
  );
}

.cut-round-position-scale {
  position: absolute;
  right: 8px;
  bottom: 8px;
  left: 8px;
  display: flex;
  justify-content: space-between;
  pointer-events: none;
  color: #e0f2fe;
  font-size: 11px;
  font-weight: 900;
  text-shadow: 0 1px 1px rgba(15, 23, 42, 0.75);
}

.report-position-hint {
  grid-column: 1 / -1;
  padding: 6px 10px;
  color: #075985;
  font-size: 13px;
  font-weight: 800;
  background: #e0f2fe;
  border: 1px solid #7dd3fc;
}

.report-position-hint.warning {
  color: #b91c1c;
  background: #fef2f2;
  border-color: #fca5a5;
}

.report-output-hint {
  margin: -2px 10px 8px;
  padding: 6px 10px;
  color: #075985;
  font-size: 12px;
  font-weight: 800;
  background: #e0f2fe;
  border: 1px solid #7dd3fc;
}

.tab-table-content {
  display: flex;
  flex-direction: column;
  width: 100%;
  max-width: 100%;
  height: calc(100% - 8px);
  min-height: 0;
  min-width: 0;
  overflow: hidden;
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

.inspection-task-modal-body {
  gap: 10px;
}

.inspection-task-modal-body .inspection-task-detail-table {
  flex: 1 1 auto;
  min-height: 0;
}

.inspection-task-view {
  display: grid;
  gap: 10px;
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
  font-family: Consolas, 'Courier New', monospace;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
  background: #f8fafc;
}

.inspection-task-view-head__remark {
  grid-column: span 3;
  white-space: normal !important;
}

.inspection-task-view-footer {
  display: flex;
  justify-content: flex-end;
  padding-top: 2px;
}

.inspection-form-sheet {
  display: grid;
  gap: 8px;
  background: linear-gradient(180deg, #f7fbff 0%, #e8f1fb 100%);
}

.inspection-form-title {
  color: #0f172a;
  font-size: 24px;
  font-weight: 900;
  line-height: 1.1;
  text-align: center;
}

.inspection-form-head {
  display: grid;
  grid-template-columns: 92px minmax(0, 1fr) 92px minmax(0, 1fr) 92px minmax(0, 1fr) 92px minmax(0, 1fr);
  align-items: stretch;
  overflow: hidden;
  border: 1px solid #9fb6cd;
  border-right: 0;
  border-bottom: 0;
  border-radius: 2px;
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
  background: #e2e8f0;
  border-right: 1px solid #cbd5e1;
  border-bottom: 1px solid #cbd5e1;
}

.inspection-form-control {
  display: flex;
  align-items: center;
  min-width: 0;
  min-height: 32px;
  padding: 0;
  background: #f8fafc;
  border-right: 1px solid #cbd5e1;
  border-bottom: 1px solid #cbd5e1;
}

.inspection-form-control--wide {
  grid-column: span 7;
}

.inspection-form-control :deep(.ant-input),
.inspection-form-control :deep(.ant-picker),
.inspection-form-control :deep(.ant-radio-group) {
  width: 100%;
  border: 0;
  border-radius: 0;
  box-shadow: none;
}

.inspection-form-control :deep(.ant-radio-group) {
  padding: 0 10px;
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

.console-record-date-filter {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #334155;
  font-size: 12px;
  font-weight: 800;
  white-space: nowrap;
}

.console-record-date-filter :deep(.ant-picker) {
  width: 138px;
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

.task-list-pagination {
  display: flex;
  flex: 0 0 38px;
  align-items: center;
  justify-content: flex-end;
  padding: 6px 8px 0;
  background: linear-gradient(180deg, rgba(248, 250, 252, 0.72), rgba(226, 232, 240, 0.54));
  border-top: 1px solid rgba(135, 148, 164, 0.56);
}

.console-table-count {
  color: #475569;
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
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

.console-record-table :deep(.ant-table) {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
}

.console-record-table :deep(.ant-table-container) {
  display: flex;
  flex: 1 1 0;
  flex-direction: column;
  min-height: 0;
}

.console-record-table :deep(.ant-table-body) {
  flex: 1 1 0;
  min-height: 0;
  max-height: none !important;
  overflow: auto !important;
}

.task-list-auto-width-table :deep(.ant-table-cell) {
  white-space: nowrap;
}

.task-list-auto-width-table :deep(.ant-table-thead > tr > th),
.task-list-auto-width-table :deep(.ant-table-tbody > tr > td) {
  padding-inline: 8px;
}

.record-info-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
  line-height: 1.25;
}

.record-info-cell strong {
  color: #172033;
  font-weight: 800;
}

.record-info-cell span {
  color: #64748b;
  font-size: 12px;
}

.table-action-stack {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  white-space: nowrap;
}

.middle-product-record-table {
  flex: 1 1 0;
  min-height: 0;
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

.tab-table-content :deep(.ant-table-wrapper),
.tab-table-content :deep(.ant-spin-nested-loading),
.tab-table-content :deep(.ant-spin-container),
.tab-table-content :deep(.ant-table),
.tab-table-content :deep(.ant-table-container) {
  width: 100%;
  max-width: 100%;
  min-width: 0;
}

.tab-table-content :deep(.ant-table-content),
.tab-table-content :deep(.ant-table-body) {
  overflow-x: auto !important;
}

.rough-check-table :deep(.ant-table) {
  background: #edf2f7;
  border-radius: 0 !important;
}

.rough-check-table :deep(.ant-table-container) {
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

.rough-check-table :deep(.ant-table-thead > tr > th) {
  color: #263445;
  font-weight: 800;
  background:
    linear-gradient(90deg, rgba(14, 165, 233, 0.08) 0 1px, transparent 1px) 0 0 / 18px 100%,
    linear-gradient(180deg, #d7dee7 0%, #cbd5e1 100%);
  border-bottom: 1px solid #8794a4;
  border-inline-end: 1px solid #a2adba;
}

.rough-check-table :deep(.ant-table-tbody > tr > td) {
  color: #172033;
  background: linear-gradient(180deg, #f8fafc 0%, #edf2f7 100%);
  border-bottom: 1px solid #c6d0dc;
  border-inline-end: 1px solid #d7dee8;
}

.rough-check-table :deep(.ant-table-tbody > tr:hover > td) {
  background: linear-gradient(180deg, #e0f2fe 0%, #dbeafe 100%);
}

.rough-check-table :deep(.ant-input),
.rough-check-table :deep(.ant-radio-wrapper) {
  font-size: 12px;
}

.rough-table-sub {
  margin-top: 2px;
  color: #64748b;
  font-size: 12px;
  line-height: 16px;
}

.pass-work-actions {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 2px;
}

.console-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 120px;
  color: #64748b;
  font-size: 13px;
  font-weight: 700;
  background: #f8fafc;
  border: 1px dashed #94a3b8;
}

.console-empty.compact {
  min-height: 80px;
}

.daily-check-result-tip {
  padding: 6px 10px;
  margin-bottom: 8px;
  color: #7a4a00;
  font-size: 13px;
  background: #fff7e6;
  border: 1px solid #ffd591;
}

.daily-check-card-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.daily-check-card {
  display: grid;
  grid-template-columns: 58px minmax(0, 1fr) auto;
  gap: 10px;
  align-items: center;
  padding: 12px;
  background: #f8fafc;
  border: 1px solid #cbd5e1;
}

.daily-check-card__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 46px;
  height: 46px;
  color: #0369a1;
  background: #e0f2fe;
  border: 1px solid #7dd3fc;
}

.daily-check-card__content {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.daily-check-card__content strong,
.daily-check-card__content em,
.daily-check-card__content span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.daily-check-card__content strong {
  color: #0f172a;
  font-weight: 800;
}

.daily-check-card__content em,
.daily-check-card__content span {
  color: #64748b;
  font-size: 12px;
  font-style: normal;
}

.daily-check-card__tag {
  grid-column: 3;
  grid-row: 1;
}

.daily-check-card__actions {
  display: flex;
  grid-column: 2 / 4;
  gap: 8px;
  justify-content: flex-end;
}

.pp-plan-modal {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  background: #f5f7fa;
}

.pp-plan-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 12px;
  background: #fff;
  border-bottom: 1px solid #d9d9d9;
}

.pp-plan-toolbar__main {
  color: #111827;
  font-size: 16px;
  font-weight: 700;
}

.toolbar-meta {
  display: flex;
  gap: 12px;
  color: #64748b;
  font-size: 12px;
}

.pp-plan-toolbar__actions,
.modal-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
}

.modal-footer {
  padding-top: 10px;
}

.report-action-footer :deep(.ant-btn) {
  width: 152px;
  min-width: 152px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.abnormal-category-correction-options {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.pp-plan-body {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 8px;
  min-height: 0;
  padding: 8px;
  overflow: hidden;
}

.pp-fieldset {
  margin: 0;
  padding: 8px 12px 10px;
  background: #fff;
  border: 1px solid #e5e7eb;
}

.pp-fieldset legend {
  padding: 0 6px;
  color: #1677ff;
  font-size: 12px;
  font-weight: 700;
}

.pp-form-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 8px 12px;
}

.pp-form-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.pp-form-item--span-2 {
  grid-column: span 2;
}

.pp-form-item label {
  color: #4b5563;
  font-weight: 700;
}

.pp-readonly-box {
  display: flex;
  align-items: center;
  min-height: 32px;
  padding: 4px 11px;
  color: #374151;
  background: #fafafa;
  border: 1px solid #d9d9d9;
  border-radius: 0;
}

.pp-panel {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
  background: #fff;
  border: 1px solid #e5e7eb;
}

.pp-panel__header {
  display: flex;
  align-items: center;
  min-height: 34px;
  padding: 0 10px;
  color: #1677ff;
  font-weight: 700;
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
}

.pp-table-wrap {
  flex: 1;
  min-height: 0;
  overflow: auto;
}

.pp-grid {
  width: 100%;
  min-width: 980px;
  border-collapse: collapse;
}

.pp-grid th,
.pp-grid td {
  padding: 6px 8px;
  border: 1px solid #e5e7eb;
}

.pp-grid th {
  color: #374151;
  font-weight: 700;
  background: #f3f4f6;
}

.head-item {
  display: flex;
  align-items: center;
  min-height: 30px;
  padding: 4px 8px;
  background: #f8fafc;
  border: 1px solid #e5e7eb;
}

.head-item__label {
  color: #64748b;
  font-weight: 700;
}

.head-item__value {
  color: #0f172a;
}

.rough-material-scan-row {
  display: flex;
  gap: 8px;
  align-items: end;
}

.rough-material-scan-row__field {
  flex: 1;
}

.rough-material-scan-hint {
  margin: 8px 0;
  padding: 8px 10px;
  color: #075985;
  font-size: 12px;
  font-weight: 700;
  background: #eff6ff;
  border-left: 4px solid #0284c7;
}

.rough-material-scan-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.record-scan-confirm__error,
.record-scan-confirm__message {
  margin-top: 8px;
  padding: 8px 10px;
  font-size: 12px;
  font-weight: 700;
}

.record-scan-confirm__error {
  color: #b91c1c;
  background: #fef2f2;
  border: 1px solid #fecaca;
}

.record-scan-confirm__message {
  color: #166534;
  background: #f0fdf4;
  border: 1px solid #bbf7d0;
}

.report-modal-body {
  display: flex;
  flex-direction: column;
  height: clamp(560px, calc(100vh - 170px), 760px);
  min-height: 0;
  overflow: hidden;
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

.report-toolbar-form {
  display: grid;
  grid-template-columns:
    max-content minmax(260px, 1.3fr)
    max-content minmax(260px, 1.3fr)
    max-content minmax(420px, 2fr);
  gap: 0;
  align-items: stretch;
  width: 100%;
  border: 1px solid #a2adba;
}

.report-toolbar-form label,
.report-toolbar-value {
  display: inline-flex;
  align-items: center;
  min-height: 34px;
  padding: 4px 10px;
  border-right: 1px solid #c6d3df;
}

.report-toolbar-form label {
  justify-content: flex-end;
  min-width: 88px;
  color: #334155;
  font-size: 13px;
  font-weight: 800;
  background: linear-gradient(180deg, #d7dee7 0%, #cbd5e1 100%);
}

.report-toolbar-value {
  gap: 6px;
  min-width: 0;
  overflow: hidden;
  color: #0f172a;
  font-family: Consolas, 'Microsoft YaHei', monospace;
  font-size: 15px;
  font-weight: 900;
  line-height: 20px;
  white-space: nowrap;
  background: linear-gradient(180deg, #fff 0%, #eef4fa 100%);
}

.report-toolbar-value.warning {
  color: #b91c1c;
  background: #fff1f2;
}

.report-toolbar-value--batch {
  color: #075985;
}

.press-slot-report-head {
  display: grid;
  grid-template-columns: minmax(460px, 1.5fr) minmax(260px, 0.8fr) minmax(260px, 0.8fr);
  gap: 8px;
}

.press-slot-report-head--cut-round {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.press-slot-report-head__item {
  display: grid;
  grid-template-columns: 112px minmax(0, 1fr);
  min-width: 0;
  overflow: hidden;
  border: 1px solid #a2adba;
}

.press-slot-report-head__item--full {
  grid-column: 1 / -1;
}

.press-slot-report-head__item--type {
  grid-column: 1 / -1;
  grid-template-columns: 112px minmax(0, 1fr);
}

.press-slot-report-head__item label,
.press-slot-report-head__value {
  display: flex;
  align-items: center;
  min-height: 34px;
  padding: 4px 10px;
}

.press-slot-report-head__item label {
  justify-content: flex-end;
  color: #334155;
  font-size: 13px;
  font-weight: 900;
  background: linear-gradient(180deg, #d7dee7 0%, #cbd5e1 100%);
  border-right: 1px solid #c6d3df;
}

.press-slot-report-head__value {
  gap: 8px;
  min-width: 0;
  overflow: hidden;
  color: #0f172a;
  font-family: Consolas, 'Microsoft YaHei', monospace;
  font-size: 15px;
  font-weight: 900;
  line-height: 20px;
  white-space: nowrap;
  background: linear-gradient(180deg, #fff 0%, #eef4fa 100%);
}

.press-slot-report-head__value.warning {
  color: #b91c1c;
  background: #fff1f2;
}

.press-slot-report-head__value--batch {
  color: #075985;
}

.press-slot-report-type-text {
  color: #075985;
  font-family: 'Microsoft YaHei', sans-serif;
  font-size: 14px;
  font-weight: 900;
}

.rough-report-tabs {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
}

.rough-report-tabs :deep(.ant-tabs-content-holder),
.rough-report-tabs :deep(.ant-tabs-content),
.rough-report-tabs :deep(.ant-tabs-tabpane) {
  flex: 1;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.report-tab-stack {
  display: flex;
  flex-direction: column;
  gap: 8px;
  height: 100%;
  min-height: 0;
  overflow: auto;
  padding-right: 4px;
}

.report-tab-stack--report {
  overflow: hidden;
  padding-bottom: 2px;
}

.press-slot-visual-tab {
  gap: 12px;
  padding: 8px 4px 4px 0;
}

.quality-result-tab {
  padding: 8px 4px 4px 0;
}

.quality-result-grid {
  display: grid;
  grid-template-columns: 110px minmax(0, 1fr) 110px minmax(0, 1fr);
  overflow: hidden;
  background: #fff;
  border: 1px solid #cbd5e1;
  border-radius: 4px;
}

.quality-result-grid label,
.quality-result-grid strong {
  min-height: 38px;
  padding: 9px 10px;
  border-right: 1px solid #e2e8f0;
  border-bottom: 1px solid #e2e8f0;
}

.quality-result-grid label {
  color: #475569;
  font-size: 12px;
  font-weight: 900;
  background: #f8fafc;
}

.quality-result-grid strong {
  color: #0f172a;
  font-size: 13px;
  font-weight: 700;
}

.quality-result-grid__remark {
  grid-column: span 3;
}

.visual-check-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 10px;
  align-items: start;
}

.visual-light-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-height: 42px;
}

.visual-light-button {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  height: 42px;
  padding: 0 12px;
  color: #263445;
  font-weight: 900;
  text-align: left;
  cursor: pointer;
  background: linear-gradient(180deg, #f8fafc 0%, #e5edf6 100%);
  border: 1px solid #b8c5d6;
  border-radius: 2px;
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
  flex: 0 0 auto;
  width: 14px;
  height: 14px;
  background: #cbd5e1;
  border: 1px solid #94a3b8;
  border-radius: 50%;
  box-shadow: inset 0 1px 2px rgba(15, 23, 42, 0.22);
}

.visual-light-dot.is-active {
  background: #ef4444;
  border-color: #b91c1c;
  box-shadow:
    0 0 0 3px rgba(239, 68, 68, 0.12),
    0 0 12px rgba(239, 68, 68, 0.82);
}

.visual-item-remark {
  height: 34px;
  font-size: 13px;
  font-weight: 800;
  border-radius: 2px;
}

.press-slot-visual-judge {
  display: grid;
  grid-template-columns: minmax(320px, 0.8fr) minmax(420px, 1.2fr);
  gap: 10px;
  align-items: stretch;
}

.press-slot-visual-field {
  display: grid;
  grid-template-columns: 112px minmax(0, 1fr);
  min-width: 0;
  border: 1px solid #a2adba;
}

.press-slot-visual-field label {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  min-height: 42px;
  padding: 6px 10px;
  color: #334155;
  font-size: 13px;
  font-weight: 900;
  background: linear-gradient(180deg, #d7dee7 0%, #cbd5e1 100%);
  border-right: 1px solid #c6d3df;
}

.press-slot-visual-field__control {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
  min-height: 42px;
  padding: 6px 10px;
  background: linear-gradient(180deg, #fff 0%, #eef4fa 100%);
}

.press-slot-visual-field__control :deep(.ant-radio-wrapper) {
  font-weight: 900;
}

.press-slot-visual-field--remark {
  grid-template-columns: 96px minmax(0, 1fr);
}

.press-slot-visual-field--remark :deep(.ant-input) {
  min-height: 76px;
  font-weight: 800;
  border: 0;
  border-radius: 0;
  resize: none;
}

.visual-check-lock {
  color: #b91c1c;
  font-size: 12px;
  font-weight: 900;
  white-space: nowrap;
}

.report-tab-scroll-body {
  display: flex;
  flex: 1 1 0;
  flex-direction: column;
  gap: 8px;
  min-height: 0;
  overflow: auto;
}

.cut-round-intermediate-modal {
  height: 100%;
}

.cut-round-intermediate-body {
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-height: 0;
}

.cut-round-intermediate-head {
  grid-template-columns: repeat(4, minmax(0, 1fr));
  padding: 0;
}

.cut-round-intermediate-panel {
  min-height: 0;
}

.cut-round-intermediate-detail-wrap {
  height: 100%;
  min-height: 0;
  overflow: auto;
}

.cut-round-intermediate-grid th:first-child,
.cut-round-intermediate-grid td:first-child {
  text-align: center;
}

.middle-product-header {
  display: grid;
  grid-template-columns: 8fr 17fr 8fr 17fr 8fr 17fr 8fr 17fr;
  gap: 6px 8px;
  align-items: stretch;
}

.middle-product-header label,
.middle-product-header strong {
  display: flex;
  align-items: center;
  min-height: 30px;
  padding: 4px 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  border: 1px solid #8794a4;
}

.middle-product-header label {
  justify-content: flex-end;
  color: #334155;
  font-weight: 700;
  background: linear-gradient(180deg, #d7dee7 0%, #cbd5e1 100%);
}

.middle-product-header strong {
  color: #0f172a;
  font-weight: 800;
  background: linear-gradient(180deg, #f8fafc 0%, #edf2f7 100%);
}

.middle-product-header-input {
  padding: 2px 4px !important;
}

.middle-product-header-input :deep(.ant-input),
.middle-product-header-input :deep(.ant-input-number),
.middle-product-header-input :deep(.ant-picker) {
  width: 100%;
  height: 24px;
  font-weight: 800;
  border-radius: 0;
}

.middle-product-detail-body {
  gap: 10px;
}

.press-slot-middle-tab {
  height: calc(100% - 8px);
}

.press-slot-middle-shell {
  background: linear-gradient(180deg, #f3f7fb 0%, #e6edf5 100%);
  border: 1px solid #8794a4;
}

.press-slot-middle-body {
  display: grid;
  grid-template-rows: max-content max-content minmax(0, 1fr);
  gap: 8px;
  height: 100%;
  min-height: 0;
  padding: 8px;
}

.press-slot-middle-sheet {
  min-width: 0;
  overflow: hidden;
  background: #f8fafc;
  border: 1px solid #9aa8b8;
}

.press-slot-middle-sheet--table {
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.press-slot-section-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 30px;
  padding: 0 10px;
  color: #0f172a;
  font-weight: 900;
  background: linear-gradient(180deg, #dfe7f0 0%, #cbd7e3 100%);
  border-bottom: 1px solid #9aa8b8;
}

.press-slot-section-title span {
  font-size: 14px;
}

.press-slot-section-title em {
  color: #475569;
  font-size: 12px;
  font-style: normal;
  font-weight: 700;
  white-space: nowrap;
}

.press-slot-middle-form {
  display: grid;
  grid-template-columns: 112px minmax(0, 1fr) 112px minmax(0, 1fr) 112px minmax(0, 1fr) 112px minmax(0, 1fr);
  gap: 6px;
  align-items: center;
  padding: 8px;
}

.press-slot-middle-form label,
.press-slot-depth-cell--head,
.press-slot-depth-cell--axis {
  height: 26px;
  padding: 0 8px;
  color: #334155;
  font-weight: 900;
  line-height: 26px;
  text-align: right;
  background: linear-gradient(180deg, #d7dee7 0%, #cbd5e1 100%);
  border: 1px solid #a2adba;
}

.press-slot-depth-cell--head,
.press-slot-depth-cell--axis {
  text-align: center;
}

.press-slot-middle-form strong,
.press-slot-depth-cell {
  height: 26px;
  min-width: 0;
  padding: 0 8px;
  overflow: hidden;
  color: #111827;
  font-family: Consolas, 'Microsoft YaHei', monospace;
  font-weight: 900;
  line-height: 26px;
  text-overflow: ellipsis;
  white-space: nowrap;
  background: #f8fafc;
  border: 1px solid #c6d0dc;
}

.press-slot-depth-cell.press-slot-depth-cell--head,
.press-slot-depth-cell.press-slot-depth-cell--axis {
  background: linear-gradient(180deg, #d7dee7 0%, #cbd5e1 100%);
  border-color: #a2adba;
}

.press-slot-middle-form :deep(.ant-input),
.press-slot-middle-form :deep(.ant-picker),
.press-slot-depth-matrix :deep(.ant-input-number) {
  width: 100%;
  height: 26px;
  border-radius: 0;
}

.press-slot-depth-matrix {
  display: grid;
  grid-template-columns: 112px repeat(3, minmax(0, 1fr));
  gap: 6px;
  align-items: center;
  padding: 8px;
}

.press-slot-middle-detail-table {
  flex: 1 1 0;
  min-height: 0;
  padding: 8px;
  overflow: hidden;
}

.press-slot-middle-detail-table :deep(.ant-table-wrapper),
.press-slot-middle-detail-table :deep(.ant-spin-nested-loading),
.press-slot-middle-detail-table :deep(.ant-spin-container),
.press-slot-middle-detail-table :deep(.ant-table) {
  height: 100%;
}

.press-slot-middle-detail-table :deep(.ant-table-body) {
  min-height: 180px;
}

.middle-product-detail-table {
  display: flex;
  flex: 1 1 0;
  flex-direction: column;
  min-height: 0;
}

.middle-product-detail-table .rough-check-table,
.middle-product-detail-table :deep(.ant-table-wrapper),
.middle-product-detail-table :deep(.ant-spin-nested-loading),
.middle-product-detail-table :deep(.ant-spin-container) {
  flex: 1 1 0;
  min-height: 0;
}

.middle-product-detail-table :deep(.ant-table-wrapper),
.middle-product-detail-table :deep(.ant-spin-nested-loading),
.middle-product-detail-table :deep(.ant-spin-container) {
  display: flex;
  flex-direction: column;
}

.middle-product-detail-table :deep(.ant-table) {
  display: flex;
  flex: 1 1 0;
  flex-direction: column;
  min-height: 0;
}

.middle-product-detail-table :deep(.ant-table-container) {
  display: flex;
  flex: 1 1 0;
  flex-direction: column;
  min-height: 0;
}

.middle-product-detail-table :deep(.ant-table-body) {
  flex: 1 1 0;
  min-height: 0;
}

.middle-product-pagination {
  display: flex;
  flex: 0 0 auto;
  justify-content: flex-end;
  padding: 4px 2px 0;
  background: #f5f7fa;
  border-top: 1px solid #c6d0dc;
}

.report-form-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 8px 12px;
  padding: 10px;
}

.report-remark {
  grid-column: span 2;
}

.report-required-field :deep(.ant-form-item-label > label) {
  color: #b91c1c;
  font-weight: 900;
}

.report-required-field :deep(.ant-form-item-label > label::before) {
  display: inline-block;
  margin-right: 4px;
  color: #dc2626;
  content: '*';
}

.full-input {
  width: 100%;
}

.report-calculated-input :deep(.ant-input-number-input) {
  color: #075985;
  font-weight: 900;
}

.second-range-visual {
  flex: 0 0 auto;
  padding: 7px 8px;
  background:
    linear-gradient(90deg, rgba(14, 165, 233, 0.06) 0 1px, transparent 1px) 0 0 / 18px 100%,
    linear-gradient(180deg, #f8fafc 0%, #edf2f7 100%);
  border: 1px solid #94a3b8;
}

.second-range-visual__head {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 22px;
  overflow: hidden;
  white-space: nowrap;
}

.second-range-visual__head strong {
  color: #075985;
  font-weight: 900;
}

.second-range-visual__head span {
  color: #334155;
  font-family: Consolas, 'Microsoft YaHei', monospace;
  font-size: 12px;
  font-weight: 800;
}

.second-range-visual__head em {
  overflow: hidden;
  color: #dc2626;
  font-size: 12px;
  font-style: normal;
  font-weight: 900;
  text-overflow: ellipsis;
}

.second-range-bar {
  position: relative;
  height: 30px;
  margin-top: 5px;
  overflow: hidden;
  background:
    repeating-linear-gradient(90deg, rgba(71, 85, 105, 0.15) 0 1px, transparent 1px 24px),
    linear-gradient(180deg, #e0f2fe 0%, #bfdbfe 100%);
  border: 1px solid #64748b;
}

.second-range-bar__empty {
  position: absolute;
  inset: 0 auto 0 8px;
  display: inline-flex;
  align-items: center;
  color: rgba(15, 23, 42, 0.48);
  font-size: 12px;
  font-weight: 900;
  pointer-events: none;
}

.second-range-segment {
  position: absolute;
  top: 0;
  bottom: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 22px;
  overflow: hidden;
  color: #fff;
  font-size: 12px;
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap;
  border-right: 1px solid rgba(15, 23, 42, 0.38);
  border-left: 1px solid rgba(15, 23, 42, 0.18);
}

.second-range-segment--reported {
  background: linear-gradient(180deg, rgba(22, 163, 74, 0.92), rgba(21, 128, 61, 0.94));
}

.second-range-segment--current {
  background: linear-gradient(180deg, rgba(2, 132, 199, 0.92), rgba(3, 105, 161, 0.96));
  box-shadow: inset 0 0 0 2px rgba(255, 255, 255, 0.52);
}

.second-range-segment--overlap {
  background:
    repeating-linear-gradient(45deg, rgba(255, 255, 255, 0.28) 0 6px, transparent 6px 12px),
    linear-gradient(180deg, #ef4444 0%, #b91c1c 100%);
  box-shadow: inset 0 0 0 2px #7f1d1d;
}

.second-range-legend {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-top: 4px;
  color: #475569;
  font-size: 12px;
  font-weight: 800;
}

.second-range-legend span {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.second-range-legend i {
  display: inline-block;
  width: 12px;
  height: 8px;
  border: 1px solid rgba(15, 23, 42, 0.22);
}

.second-range-legend .is-empty {
  background: #bfdbfe;
}

.second-range-legend .is-reported {
  background: #16a34a;
}

.second-range-legend .is-current {
  background: #0284c7;
}

.second-range-legend .is-overlap {
  background: #dc2626;
}

.glue-consume-form {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.consumable-reason-form {
  display: grid;
  grid-template-columns: 98px minmax(0, 1fr);
  gap: 8px;
  align-items: start;
  width: 100%;
}

.consumable-reason-form label {
  min-height: 32px;
  color: #4b5563;
  font-weight: 700;
  line-height: 32px;
  text-align: right;
}

.consumable-reason-form textarea {
  width: 100%;
}

.consumable-info-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px 12px;
}

.consumable-number-input {
  width: 100%;
}

.record-action-buttons {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 2px;
  min-width: 0;
}

.record-confirm-panel {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.record-confirm-summary {
  display: grid;
  grid-template-columns: 120px minmax(0, 1fr) 120px minmax(0, 1fr);
  gap: 8px;
}

.record-confirm-summary span,
.record-confirm-summary strong {
  display: flex;
  align-items: center;
  min-height: 34px;
  padding: 6px 8px;
  border: 1px solid #cbd5e1;
}

.record-confirm-summary span {
  justify-content: flex-end;
  color: #334155;
  font-weight: 700;
  background: #e2e8f0;
}

.record-confirm-summary strong {
  overflow: hidden;
  color: #075985;
  font-family: Consolas, 'Courier New', monospace;
  text-overflow: ellipsis;
  white-space: nowrap;
  background: #f8fafc;
}

.record-confirm-type-row {
  display: grid;
  grid-template-columns: 120px minmax(0, 1fr);
  gap: 8px;
  align-items: center;
  min-height: 38px;
}

.record-confirm-type-row > span {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  height: 36px;
  padding: 0 8px;
  color: #334155;
  font-weight: 800;
  background: #e2e8f0;
  border: 1px solid #cbd5e1;
}

.record-confirm-type-row :deep(.ant-radio-group) {
  display: flex;
  flex-wrap: nowrap;
  align-items: center;
  height: 36px;
  padding: 0 10px;
  overflow-x: auto;
  background: #f8fafc;
  border: 1px solid #cbd5e1;
}

.record-confirm-type-row :deep(.ant-radio-wrapper) {
  margin-inline-end: 12px;
  font-weight: 800;
  white-space: nowrap;
}

.record-confirm-model-radio {
  display: flex;
  align-items: center;
  min-height: 36px;
  padding: 0 10px;
  overflow-x: auto;
  background: #f8fafc;
  border: 1px solid #cbd5e1;
}

.record-confirm-model-radio :deep(.ant-radio-wrapper),
.report-toolbar-radio :deep(.ant-radio-wrapper) {
  margin-inline-end: 12px;
  font-weight: 800;
  white-space: nowrap;
}

.record-confirm-summary--type {
  grid-template-columns: 120px minmax(0, 1fr);
}

.record-confirm-summary--type :deep(.ant-radio-wrapper) {
  margin-right: 12px;
}

.record-confirm-basis {
  padding: 7px 10px;
  color: #075985;
  font-size: 13px;
  font-weight: 900;
  background: #eff6ff;
  border: 1px solid #bfdbfe;
}

.size-rule-panel {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.size-rule-summary {
  display: grid;
  grid-template-columns: 120px minmax(0, 1fr);
  gap: 8px;
}

.size-rule-summary label,
.size-rule-summary strong {
  display: flex;
  align-items: center;
  min-height: 34px;
  padding: 6px 8px;
  border: 1px solid #cbd5e1;
}

.size-rule-summary label {
  justify-content: flex-end;
  color: #334155;
  font-weight: 700;
  background: #e2e8f0;
}

.size-rule-summary strong {
  overflow: hidden;
  color: #075985;
  font-family: Consolas, 'Courier New', monospace;
  text-overflow: ellipsis;
  white-space: nowrap;
  background: #f8fafc;
}

.size-rule-options {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.size-rule-option {
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr);
  align-items: center;
  min-height: 58px;
  padding: 8px 12px;
  color: #0f172a;
  background: #f8fafc;
  border: 1px solid #cbd5e1;
}

.size-rule-option i {
  width: 28px;
  height: 28px;
  background: var(--rule-color);
  border: 2px solid rgba(255, 255, 255, 0.9);
  box-shadow: 0 0 0 1px rgba(15, 23, 42, 0.18);
}

.size-rule-option strong {
  font-size: 18px;
}

.size-rule-option.is-active {
  color: #075985;
  background: #e0f2fe;
  border-color: #0284c7;
  box-shadow: inset 0 0 0 1px #0284c7;
}

.record-confirm-scan {
  display: grid;
  grid-template-columns: 54px minmax(0, 1fr);
  gap: 10px;
  align-items: center;
  padding: 10px;
  background: #f8fafc;
  border: 1px solid #cbd5e1;
}

.record-confirm-scan svg {
  width: 38px;
  height: 38px;
  color: #0369a1;
}

.cut-round-console.has-sample-lock {
  grid-template-rows: repeat(4, max-content) minmax(0, 1fr);
}
.cut-round-console.has-sample-lock.is-visual-maximized {
  grid-template-rows: max-content max-content minmax(0, 1fr);
}
.sample-lock-notice {
  max-height: 84px;
  overflow: auto;
}
</style>

<style>
.hc-pass-work-modal .ant-modal {
  top: 0;
  width: 100vw !important;
  max-width: none;
  margin: 0 !important;
  padding-bottom: 0;
}

.hc-pass-work-modal .ant-modal-header,
.hc-pass-work-modal .ant-modal-close {
  display: none !important;
}

.hc-pass-work-modal .ant-modal-body {
  height: 100vh !important;
  padding: 0 !important;
  overflow: hidden !important;
  background: #f5f7fa;
}

.hc-pass-work-modal .ant-modal-content {
  height: 100vh !important;
  padding: 0 !important;
  border-radius: 0 !important;
  box-shadow: none;
}

.rough-report-work-modal .report-modal-body {
  box-sizing: border-box;
  height: 100vh;
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

.rough-report-work-modal :deep(.ant-modal-title) {
  width: 100%;
}

.changeover-modal-body {
  gap: 10px;
}

.changeover-modal-titlebar {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  min-width: 0;
}

.changeover-modal-title {
  display: flex;
  gap: 8px;
  align-items: center;
  min-width: 0;
  color: #0f172a;
  font-size: 18px;
  font-weight: 800;
}

.changeover-modal-actions {
  display: flex;
  flex: 0 0 auto;
  gap: 8px;
  align-items: center;
}

.changeover-scan-row {
  display: grid;
  grid-template-columns: 92px minmax(280px, 1fr) 108px;
  gap: 8px;
  align-items: center;
  margin-bottom: 10px;
}

.changeover-scan-row label,
.changeover-form-grid label {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  height: 32px;
  padding: 0 10px;
  color: #334155;
  font-weight: 700;
  background: #e2e8f0;
  border: 1px solid #cbd5e1;
}

.changeover-form-grid {
  display: grid;
  grid-template-columns: 92px minmax(0, 1fr) 92px minmax(0, 1fr) 92px minmax(0, 1fr) 92px minmax(0, 1fr);
  gap: 6px 8px;
  align-items: center;
}

.changeover-form-grid strong {
  display: flex;
  align-items: center;
  min-width: 0;
  height: 32px;
  padding: 0 10px;
  overflow: hidden;
  color: #075985;
  font-family: Consolas, 'Courier New', monospace;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
  background: #f8fafc;
  border: 1px solid #cbd5e1;
}

.changeover-form-grid :deep(.ant-input) {
  height: 32px;
  border-radius: 0;
}

.changeover-remark-input {
  grid-column: auto;
}

.changeover-attachment-list {
  display: flex;
  gap: 10px;
  align-items: flex-start;
  margin-top: 8px;
  padding: 6px 8px;
  color: #334155;
  background: #f8fafc;
  border: 1px solid #cbd5e1;
}

.changeover-attachment-list__label {
  flex: 0 0 auto;
  color: #0369a1;
  font-weight: 800;
}

.changeover-attachment-list__items {
  display: flex;
  flex: 1 1 auto;
  flex-wrap: wrap;
  gap: 8px 14px;
  min-width: 0;
}

.changeover-attachment-list__items a {
  color: #2563eb;
  font-weight: 600;
}

.changeover-attachment-list__items small {
  margin-left: 4px;
  color: #64748b;
  font-weight: 400;
}

.changeover-check-panel {
  flex: 1 1 auto;
  min-height: 0;
}

.rough-report-work-modal .modal-footer {
  position: sticky;
  bottom: 0;
  flex: 0 0 auto;
  padding: 8px 10px;
  background: #f5f7fa;
  border-top: 1px solid #cbd5e1;
}

</style>
