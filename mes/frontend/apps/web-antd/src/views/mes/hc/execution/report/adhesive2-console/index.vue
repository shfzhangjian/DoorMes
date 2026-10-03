<script lang="ts" setup>
import { isSampleLockDeferredToCutRound } from '../shared/sampleAbnormalLockPolicy';
import { glueBoardMatchReason } from '#/utils/glue-board-match';
import { computed, h, nextTick, onActivated, onBeforeUnmount, onDeactivated, onMounted, reactive, ref, watch } from 'vue';

import { useAccess } from '@vben/access';
import { Page, useVbenModal } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';
import { useUserStore } from '@vben/stores';
import { downloadFileFromBlobPart } from '@vben/utils';
import { useQRCode } from '@vueuse/integrations/useQRCode';

import { Badge, Button, Checkbox, DatePicker, Form, FormItem, Input, InputNumber, Modal as AModal, Pagination, Radio, RadioButton, RadioGroup, Select, Table as ATable, Tabs, TabPane, Tag, message } from 'ant-design-vue';
import dayjs from 'dayjs';
import { useRoute } from 'vue-router';

import {
  completeAdhesive2ConsoleSegment,
  completeAdhesiveConsoleWorkOrder,
  confirmAdhesiveConsolePassWork,
  confirmAdhesiveConsoleReport,
  assignAdhesive2TailForAllSources,
  assignAdhesive2TailForSelectedSources,
  applyAdhesive2Fai,
  applyAdhesive2PostConfirmCoa,
  withdrawAdhesive2Coa,
  correctAdhesive2ReportAbnormalCategory,
  getAdhesive2CoaFaiList,
  getAdhesive2ProcessCheckFaiList,
  getAdhesive2RuntimeProductSnapshot,
  deleteAdhesive2ProcessParam,
  exportAdhesive2MiddleLedger,
  exportAdhesive2ProcessParams,
  getAdhesiveConsoleCheckTemplate,
  getAdhesiveConsoleCurrentGlueBoardUsage,
  getAdhesiveConsoleGlueBoardStockByBatch,
  getAdhesiveConsoleGlueBoardStockPage,
  getAdhesiveConsoleIntermediate,
  getAdhesiveConsoleLatestGlueBoardFai,
  getAdhesiveConsolePassWorkList,
  getAdhesiveConsoleReportList,
  getAdhesiveConsoleSourceList,
  getAdhesiveConsoleTaskList,
  getAdhesive2IntermediateList,
  getAdhesive2ProcessParamList,
  importAdhesive2MiddleLedger,
  importAdhesive2ProcessParams,
  markAdhesiveConsoleReportPrinted,
  reportAdhesiveConsoleGlueBoardLoss,
  saveAdhesiveConsoleGlueBoardUsage,
  saveAdhesiveConsoleIntermediate,
  saveAdhesiveConsolePassWork,
  saveAdhesiveConsoleReport,
  saveAndConfirmAdhesiveConsoleReport,
  saveAdhesive2ProcessParam,
  confirmAdhesive2ProcessParam,
  scanAdhesiveConsoleSource,
  setAdhesive2ReportMiddleType,
  getAdhesive2SourcePressSlotFaiList,
  getAdhesive2SourcePressSlotProcessCheckFaiList,
  startAdhesiveConsoleWorkOrder,
  submitAdhesiveConsoleAqcTask,
  type MesHcAdhesiveConsoleApi,
} from '#/api/mes/hc/execution/adhesive2-console';
import { getEquipmentPage, type MesHcEquipmentApi } from '#/api/mes/hc/equipment';
import { getOperationInstructionList, getProductionInstructionUnreadCount, startChangeoverInstruction, type MesHcProductionInstructionApi } from '#/api/mes/hc/production-instruction';
import { getMatchedFinishedGlueBoardMapItems, type MesHcFinishedGlueBoardMapApi } from '#/api/mes/hc/finishedglueboardmap';
import { getPlanOrderDetail } from '#/api/mes/hc/planorder';
import { uploadFile } from '#/api/infra/file';
import { createGlueBoardFaiRecordFromAdhesive2 } from '#/api/mes/quality/fai';
import { getActiveSampleAbnormalLock, type MesQmsSampleAbnormalRecheckApi } from '#/api/mes/quality/sample-abnormal-recheck';
import AuthModal from '#/views/mes/work-order-booking/modules/components/AuthModal.vue';
import FaiDetailModal from '#/views/mes/quality/fai/modules/detail-modal.vue';
import QmsSampleAbnormalLockGuard from '#/views/mes/quality/sample-abnormal-recheck/components/QmsSampleAbnormalLockGuard.vue';
import { ProductionInstructionIssueModal, ProductionInstructionMessageTab } from '#/views/mes/hc/shared/production-instruction';
import { useExecutionFullscreenClock } from '../shared/useExecutionFullscreenClock';
import { getReportRequestErrorMessage, resolveSavedReportId } from '../shared/reportSubmitGuard';
import { applyPreProcessSelfCheckAttribution, getDownstreamPreProcessFeedbackTitle, hasDownstreamPreProcessFeedback, isPreProcessSelfCheckAbnormal } from '../shared/preProcessSelfCheckAttribution';
import { buildInspectionTransferTicketPayload, buildTransferTicketQrValue, resolveTransferTicketQrBusinessNo, sendTransferTicketToPrintAgent } from '../shared/workOrderTicketPrint';
import { applyPrintFieldTemplate } from '../shared/printFieldTemplate';
import Adhesive2GlueBoardEdgeConsumableTab from './Adhesive2GlueBoardEdgeConsumableTab.vue';

defineOptions({ name: 'MesExecutionAdhesive2Console' });

const ADHESIVE2_PRE_PROCESS_ATTRIBUTION = {
  processCode: 'PRESS_SLOT',
  processName: '压槽',
} as const;

const ADHESIVE_DEFAULT_GLUE_BOARD_MATERIAL_CODE = '01.02.00002';
const ADHESIVE2_PRINT_AGENT_URL = 'http://127.0.0.1:17820';
const ADHESIVE2_GLUE_BOARD_USE_LENGTH_PER_SCAN = 1.2;
const TASK_LIST_DEFAULT_PAGE_SIZE = 10;
const GLUE_BOARD_MODEL_OPTIONS = [
  { label: 'W220', value: 'W220' },
  { label: 'W250', value: 'W250' },
  { label: 'SDK', value: 'SDK' },
];
const SIZE_RULE_OPTIONS = [
  { color: '#0ea5e9', label: '775mm', value: '775mm' },
  { color: '#16a34a', label: '740mm', value: '740mm' },
];

type Adhesive2ReportType = 'CHANGEOVER' | 'END' | 'FRONT' | 'MIDDLE' | 'PROCESS_CHECK' | 'PRODUCT';
type Adhesive2SegmentStatus = 'COMPLETED' | 'CURRENT_NG' | 'PENDING' | 'SOURCE_NG';

type Adhesive2TailAssignRow = {
  sourceProductionBatchNo: string;
};

type DailyRecordMode = 'confirm' | 'edit' | 'view';
type DailyRecordSubmitAction = {
  key: string;
  mode: Exclude<DailyRecordMode, 'view'>;
};

interface SubmitAdhesiveReportOptions {
  closeAfterSave?: boolean;
  confirmAfterSave?: boolean;
  expectedRuntimeModelCode?: string;
  oneClickBatchConfirm?: boolean;
  silentSuccess?: boolean;
  successMessage?: string;
}

interface Adhesive2OneClickConfirmTarget {
  group: SourceGroup;
  record?: MesHcAdhesiveConsoleApi.ReportItem;
  segment: SourceSegment;
}

interface Adhesive2TransferPrintTarget {
  group: SourceGroup;
  record?: MesHcAdhesiveConsoleApi.ReportItem;
  segment: SourceSegment;
}

interface Adhesive2InspectionTransferPrintOptions {
  applyTime?: string;
  boardBatchNo?: string;
  boardMaterialCode?: string;
  extraFields?: Array<{ label: string; value?: unknown }>;
  fields?: Array<{ label: string; value?: unknown }>;
  inspectionType: string;
  materialCode?: string;
  productionBatchNo?: string;
  sampleType: string;
  statusText?: string;
  templateCode?: string;
  title?: string;
}

interface CurrentPlan {
  actualSizeRule: string;
  availableSourceLength: number;
  batchNo: string;
  endTime: string;
  equipmentCode: string;
  equipmentId?: number;
  equipmentName: string;
  glueBoardModel: string;
  materialCode: string;
  modelCode: string;
  planGlueBoardModel: string;
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
  applicablePadType: string;
  applicablePadTypeName: string;
  code: string;
  id?: number;
  name: string;
  workCenterId?: number;
  workCenterName: string;
  workStatus: string;
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
  confirmTime?: string;
  defectCode?: string;
  extraJson?: string;
  grindingSecondDetailId?: number;
  label: string;
  outputLength: number;
  qtime?: MesHcAdhesiveConsoleApi.QtimeInfo;
  sourceMotherBatchNo?: string;
  sourceNgProcessName?: string;
  sourceNgReason?: string;
  sourceNgText?: string;
  sourceProcessStage?: string;
  sourceMenuCode?: string;
  sourceModelCode?: string;
  sourcePlanId?: number;
  sourcePlanOperationId?: number;
  reportRanges: SourceReportRange[];
  segmentMark: string;
  selfCheck?: string;
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
  qtime?: MesHcAdhesiveConsoleApi.QtimeInfo;
  segments: SourceSegment[];
  totalAvailableLength: number;
  totalOutputLength: number;
  totalUsedLength: number;
}

type SampleLockCandidate = MesQmsSampleAbnormalRecheckApi.ActiveReqVO & {
  objectLabel: string;
  processName: string;
};

interface AdhesiveCheckItem {
  abnormalRemark: string;
  actualValue: string;
  checkResult: 'NG' | 'OK' | string;
  itemCategory: string;
  itemName: string;
  sortNo: number;
  standardValue: string;
}

interface Adhesive2ImportAttachment {
  name: string;
  path?: string;
  size?: number;
  type?: string;
  uid: string;
  uploadTime: string;
  url?: string;
}

interface VisualItem {
  itemName: string;
  remark: string;
  result: 'NG' | 'OK';
}

const route = useRoute();
const userStore = useUserStore();
const { hasAccessByCodes } = useAccess();
const PLAN_SCAN_MIN_LENGTH = 11;
const ABNORMAL_CATEGORY_CORRECT_PERMISSION = 'mes:sfc:adhesive2-report:correct-abnormal-category';
const INSPECTION_STATUS_REFRESH_INTERVAL_MS = 5 * 60_000;
const currentDateTime = ref(dayjs().format('YYYY-MM-DD HH:mm:ss'));
const currentDateText = computed(() => dayjs(currentDateTime.value).format('YYYY-MM-DD'));
const currentTimeText = computed(() => dayjs(currentDateTime.value).format('HH:mm:ss'));
const { showExecutionClock } = useExecutionFullscreenClock();
const boardLoading = ref(false);
const visualMaximized = ref(false);
const transferPrintSelectionMode = ref(false);
const selectedTransferReportIds = ref<string[]>([]);
let timer: ReturnType<typeof setInterval> | null = null;
let skipInitialActivation = true;
let inspectionStatusRefreshTimer: ReturnType<typeof setInterval> | null = null;
let planScanTimer: ReturnType<typeof setTimeout> | null = null;
let syncingReportPosition = false;
let inspectionStatusRefreshInFlight = false;

const currentUserName = computed(() => userStore.userInfo?.nickname || userStore.userInfo?.username || '');

function resolveAuthenticatedUserName(authPayload: any, fallback = '') {
  return authPayload?.empName || authPayload?.nickname || authPayload?.username || authPayload?.empNo || fallback || currentUserName.value || '';
}

const currentPlan = reactive<CurrentPlan>({
  actualSizeRule: '',
  availableSourceLength: 0,
  batchNo: '',
  endTime: '',
  equipmentCode: '',
  equipmentName: '',
  glueBoardModel: '',
  materialCode: '',
  modelCode: '',
  planGlueBoardModel: '',
  planNo: '',
  requirements: '',
  sourceBatchNo: '',
  sourceProductionBatchNo: '',
  startTime: '',
  status: '',
  workCenterId: undefined,
  workCenterName: '',
});

const instructionUnreadCount = ref(0);
const latestPlanInstruction = ref<MesHcProductionInstructionApi.Instruction | null>(null);
const currentChangeoverInstruction = ref<MesHcProductionInstructionApi.Instruction | null>(null);
const changeoverInstructionIssueVisible = ref(false);
const changeoverInstructionIssueAuthVisible = ref(false);
const changeoverInstructionExecuteAuthVisible = ref(false);
let changeoverInstructionIssueAuthResolver: ((result: false | Partial<MesHcProductionInstructionApi.Instruction>) => void) | undefined;
const changeoverInstructionVisible = ref(false);
const changeoverInstructionStarting = ref(false);
const changeoverInstructionForm = reactive({
  remark: '',
});
const adhesive2InstructionContext = computed(() => ({
  operationCode: 'WC-ADH2',
  operationName: '粘胶2',
  planId: currentPlan.planId,
  planNo: currentPlan.planNo,
  planOperationId: currentPlan.planOperationId,
  processCode: 'WC-ADH2',
  processName: '粘胶2',
}));
const adhesive2ChangeoverIssueContext = computed(() => {
  const segmentBatchNo = currentPlan.batchNo || currentPlan.sourceBatchNo || '';
  return {
    batchNo: segmentBatchNo || currentPlan.sourceProductionBatchNo || '',
    instructionType: 'CHANGEOVER',
    operationCode: 'WC-ADH2',
    operationName: '粘胶2',
    planId: currentPlan.planId,
    planNo: currentPlan.planNo,
    planOperationId: currentPlan.planOperationId,
    processCode: 'WC-ADH2',
    processName: '粘胶2',
    productMaterialCode: currentPlan.materialCode,
    productModelCode: currentPlan.modelCode,
    productionBatchNo: currentPlan.sourceProductionBatchNo || currentPlan.sourceBatchNo || segmentBatchNo,
    scopeType: 'SEGMENT',
    segmentBatchNo,
  };
});

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
const isEventFromScannerInput = (event: KeyboardEvent) => isEventTargetInInputRef(event.target, planScanInputRef) || isEventTargetInInputRef(event.target, recordConfirmScanInputRef);
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
const reportRecordListVisible = ref(false);
const dailyRecordMode = ref<DailyRecordMode>('edit');
const dailyRecordKey = ref('');
const dailyRecordAuthVisible = ref(false);
const dailyRecordAuthAction = ref('工作准备记录认证');
const pendingDailyRecordAction = ref<DailyRecordSubmitAction>();
const intermediateAuthVisible = ref(false);
const intermediateAuthAction = ref('中间品记录确认');
const pendingIntermediateAuthMode = ref<'confirm' | 'save'>('confirm');
const changeoverSaveAuthVisible = ref(false);
const changeoverConfirmAuthVisible = ref(false);
const changeoverSaveAuthAction = ref('保存粘胶2工艺参数点检记录');
const changeoverConfirmAuthAction = ref('确认粘胶2工艺参数点检记录');
const activeBoardTab = ref('SOURCE');
const workbenchRefreshing = ref(false);
const activeReportTab = ref('scan');
const reportVisible = ref(false);
const reportSubmitting = ref(false);
// 仅用于单片扫码会话：先完成原有外观/NG填写，再在同一次提交中保存并确认。
const singleScanConfirmAfterInspection = ref(false);
const middleTypeSetting = ref(false);
const middleAutoMarking = ref(false);
const showExtendedBoardTabs = ref(false);
const reportDialogMode = ref<'confirm' | 'view'>('view');
const reportStep = ref<'PROCESS' | 'REPORT' | 'SCAN'>('SCAN');
const reportRecords = ref<MesHcAdhesiveConsoleApi.ReportItem[]>([]);
// COA 使用独立的完整报工列表，不受报工表格日期筛选影响。
const coaReportRecords = ref<MesHcAdhesiveConsoleApi.ReportItem[]>([]);
const reportScanConfirmDate = ref('');
const selectedReportRecordKeys = ref<(number | string)[]>([]);
const sourceGroups = ref<SourceGroup[]>([]);
const segmentCompleteAuthVisible = ref(false);
const pendingSegmentCompleteGroup = ref<SourceGroup | null>(null);
const segmentCompleteAuthActionName = computed(() => {
  const batchNo = pendingSegmentCompleteGroup.value?.baseBatchNo;
  return batchNo ? `确认本段是否确认完工？（${batchNo}）` : '确认本段是否确认完工？';
});
const visualFilterForm = reactive({
  batchNo: '',
  reportType: 'ALL',
  status: 'ALL',
});
const checkTemplate = ref<AdhesiveCheckItem[]>([]);
const visualInspectionItems = ref<VisualItem[]>([]);
const recordConfirmVisible = ref(false);
const recordConfirmProcessing = ref(false);
const activeRecord = ref<MesHcAdhesiveConsoleApi.ReportItem | null>(null);
const abnormalCategoryCorrectionVisible = ref(false);
const abnormalCategoryCorrectionSaving = ref(false);
const abnormalCategoryCorrectionForm = reactive({
  category: '',
  reason: '',
});
const isConfirmedReportOverwrite = computed(() => reportDialogMode.value === 'confirm' && String(activeRecord.value?.reportStatus || '').toUpperCase() === 'CONFIRMED');
const taskListVisible = ref(false);
const pendingGlueBoardInspectionAfterConsume = ref(false);
const taskListLoading = ref(false);
const taskRows = ref<MesHcAdhesiveConsoleApi.TaskItem[]>([]);
const adhesive2WorkbenchBlockedReason = ref('');
const taskListPage = ref(1);
const taskListPageSize = ref(TASK_LIST_DEFAULT_PAGE_SIZE);
const taskListFilters = reactive({
  batchNo: '',
  modelCode: '',
  planNo: '',
  status: 'UNFINISHED',
});
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
const equipmentSelectVisible = ref(false);
const equipmentSelectLoading = ref(false);
const equipmentSelectRows = ref<MesHcEquipmentApi.Equipment[]>([]);
const equipmentSelectKeyword = ref('');
const boardEquipmentManualSelected = ref(false);
const openDailyRecordAfterEquipmentSelected = ref(false);
const intermediateLoading = ref(false);
const intermediateDetails = ref<MesHcAdhesiveConsoleApi.IntermediateDetail[]>([]);
const adhesive2IntermediateRecords = ref<MesHcAdhesiveConsoleApi.IntermediateRecord[]>([]);
const selectedIntermediateRecord = ref<MesHcAdhesiveConsoleApi.IntermediateRecord | null>(null);
const intermediateDetailVisible = ref(false);
const middleLedgerFileInput = ref<HTMLInputElement | null>(null);
const middleLedgerImporting = ref(false);
const pendingStartTask = ref<MesHcAdhesiveConsoleApi.TaskItem | null>(null);
const sizeRuleVisible = ref(false);
const sizeRuleForm = reactive({
  actualSizeRule: '775mm',
  glueBoardModel: 'W220',
  planSizeSpec: '',
});
const changeoverInspections = ref<MesHcAdhesiveConsoleApi.ChangeoverInspection[]>([]);
let changeoverInstructionLoadSeq = 0;
let runtimeProductSnapshotLoadSeq = 0;
let changeoverInspectionsLoadSeq = 0;
const latestChangeoverInspection = ref<MesHcAdhesiveConsoleApi.ChangeoverInspection | null>(null);
const processParamFileInput = ref<HTMLInputElement | null>(null);
const changeoverVisible = ref(false);
const changeoverImporting = ref(false);
const changeoverConfirming = ref(false);
const changeoverSubmitting = ref(false);
const modelOverride = reactive({
  changeoverLogId: undefined as number | undefined,
  changeoverInstructionId: undefined as number | undefined,
  changeoverInstructionNo: '',
  changeoverInstructionTargetQty: undefined as number | undefined,
  changeoverTime: '',
  changeoverUserName: '',
  enabled: false,
  glueBoardModel: '',
  materialCode: '',
  materialName: '',
  modelCode: '',
  originalGlueBoardModel: '',
  originalMaterialCode: '',
  originalModelCode: '',
  specification: '',
});
const firstInspection = ref({
  allowReportSubmit: false,
  displayText: '未提交',
  faiApplyTime: '',
  faiId: undefined as number | undefined,
  faiJudgment: 'PENDING',
  faiNo: '',
  faiRejectReason: '',
  faiReturnTime: '',
  faiStandardNo: '',
  faiStandardVersion: '',
  faiStatus: 'PENDING',
  glueBoardBatchNo: '',
  glueBoardMaterialCode: '',
  glueBoardUsageId: undefined as number | undefined,
  sampleLength: undefined as number | undefined,
  sampleStartPosition: undefined as number | undefined,
  sourceReportNo: '',
});
const firstInspectionApplying = ref(false);
const changeoverForm = reactive<MesHcAdhesiveConsoleApi.ChangeoverInspection>({
  checkItems: [],
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

const intermediateForm = reactive<MesHcAdhesiveConsoleApi.IntermediateRecord>({
  adhesiveReportId: undefined,
  batchNo: '',
  confirmerName: '',
  confirmTime: '',
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
  productionDate: buildNowText(),
  recordDate: buildNowText(),
  recorderName: '',
  recordStatus: 'DRAFT',
  remark: '',
  showGlueBoardModel: undefined,
  sourceExcel: '',
  sourceSheet: '',
  stationFormCode: '',
  stationFormDisplayName: '',
  stationFormId: undefined,
  stationFormName: '',
  stationFormProcessCode: '',
  stationFormSchemaJson: '',
  templateCode: '',
  templateName: '',
  thicknessColumnCount: undefined,
  thicknessHeaderText: '',
  thicknessIntervalCm: undefined,
  thicknessStandard: '',
  widthEnd: undefined,
  widthMiddle: undefined,
  widthStart: undefined,
});
const intermediateRecordLocked = computed(() => isCurrentTaskReadonly.value || intermediateForm.recordStatus === 'CONFIRMED');

const glueBoard = reactive({
  alarm: '',
  batchNo: '',
  id: undefined as number | undefined,
  stockId: undefined as number | undefined,
  materialCode: '',
  model: '',
  qualityLockReason: '',
  qualityLockStartPosition: undefined as number | undefined,
  qualityStatus: '',
  receiveLength: 0,
  receiveStartPosition: 0,
  returnedLength: 0,
  returnedStartPosition: 0,
  stockLength: 0,
  todayUsedLength: 0,
  availableStartPosition: 0,
  aqcSampleLength: 0,
  inspectionSubmitTime: '',
  latestAqcTask: undefined as MesHcAdhesiveConsoleApi.AqcTask | undefined,
  latestInspectionId: undefined as number | undefined,
  latestInspectionNo: '',
  latestInspectionResult: '',
  lossLength: 0,
});
const glueBoardEdgeConsumableTabRef = ref<InstanceType<typeof Adhesive2GlueBoardEdgeConsumableTab>>();

const glueConsumeVisible = ref(false);
const glueConsumeForm = reactive({
  batchNo: '',
  materialScanCode: '',
  receiveLength: undefined as number | undefined,
  receiveStartPosition: 0,
  stockAvailableLength: 0,
  stockId: undefined as number | undefined,
});
const glueStockSelectVisible = ref(false);
const glueStockSelectLoading = ref(false);
const glueStockRows = ref<MesHcAdhesiveConsoleApi.GlueBoardStock[]>([]);
const glueStockFilter = reactive({
  batchNo: '',
  materialCode: '',
  model: '',
});
const glueStockPagination = reactive({
  pageNo: 1,
  pageSize: 10,
  total: 0,
});
const glueBoardMapCandidates = ref<MesHcFinishedGlueBoardMapApi.FinishedGlueBoardMapItem[]>([]);
const glueBoardMapLoading = ref(false);

const aqcVisible = ref(false);
const aqcSubmitting = ref(false);
const aqcForm = reactive({
  id: undefined as number | undefined,
  sampleLength: undefined as number | undefined,
  sampleStartPosition: 0,
});
const glueBoardAqcSubmittedStatuses = new Set(['WAITING', 'SUBMITTED', 'PASSED', 'OK', 'NORMAL']);
const glueBoardAqcAbnormalStatuses = new Set(['FAILED', 'NG', 'ABNORMAL', 'REJECTED', 'CANCELED', 'CANCELLED']);

const glueBoardFaiVisible = ref(false);
const glueBoardFaiForm = reactive({
  sampleLength: undefined as number | undefined,
  sampleStartPosition: 0,
});

const coaInspectionVisible = ref(false);
const coaInspectionApplying = ref(false);
const selectedCoaSliceNo = ref('');
const coaFaiRows = ref<MesHcAdhesiveConsoleApi.FaiSummary[]>([]);
const withdrawingCoaId = ref<number | null>(null);
function requestCoaWithdrawal(record: MesHcAdhesiveConsoleApi.FaiSummary) {
  const { planId, planOperationId } = currentPlan;
  const faiId = record.faiId;
  if (!planId || !planOperationId || !faiId || !record.canWithdraw || isCurrentTaskReadonly.value) return;
  const reason = ref('');
  AModal.confirm({
    title: '撤回COA送检',
    content: () => h('div', [
      h('p', `撤回 ${record.faiNo}（片号：${record.productBatchNo}）。仅品质未处理的待检单可撤回，已确认报工保留。`),
      h(Input.TextArea, { value: reason.value, maxlength: 500, placeholder: '请填写撤回原因', 'onUpdate:value': (value: string) => { reason.value = value; } }),
    ]),
    okText: '确认撤回', cancelText: '取消', okButtonProps: { danger: true },
    async onOk() {
      if (!reason.value.trim()) { message.warning('请填写撤回原因'); throw new Error('请填写撤回原因'); }
      withdrawingCoaId.value = faiId;
      try {
        await withdrawAdhesive2Coa({ planId, planOperationId, faiId, withdrawReason: reason.value.trim() });
        await Promise.all([loadCoaFaiRows(), loadReports(), loadSourceGroups(), loadTaskList()]);
        message.success('COA送检已撤回，已保留粘胶2报工记录');
      } catch (error: any) {
        message.warning(getReportRequestErrorMessage(error, '撤回失败，请刷新后确认'));
        throw error;
      } finally { withdrawingCoaId.value = null; }
    },
  });
}
const processCheckApplying = ref(false);
const processCheckFaiRows = ref<MesHcAdhesiveConsoleApi.FaiSummary[]>([]);
const sourcePressSlotFirstInspectionRows = ref<MesHcAdhesiveConsoleApi.FaiSummary[]>([]);
const sourcePressSlotProcessCheckRows = ref<MesHcAdhesiveConsoleApi.FaiSummary[]>([]);
const coaInspectionScanForm = reactive({
  normalizedSliceNo: '',
  scanError: '',
  scanMessage: '',
  scannedSliceNo: '',
});

const glueLossVisible = ref(false);
const glueLossForm = reactive({
  endPosition: undefined as number | undefined,
  lossLength: undefined as number | undefined,
  lossReason: '',
  startPosition: 0,
});

const recordConfirmForm = reactive({
  error: '',
  message: '',
  reportType: 'PRODUCT' as Adhesive2ReportType,
  scannedBatchNo: '',
});

const tailAssignVisible = ref(false);
const tailAssignSubmitting = ref(false);
const tailAssignDefaultSizeRule = ref('775mm');
const tailAssignRows = ref<Adhesive2TailAssignRow[]>([]);
const oneClickScanConfirming = ref(false);

const reportForm = reactive({
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
  napSampleLength: 0,
  outputLength: 0,
  parentBatchNo: '',
  processLength: 0,
  productionBatchNo: '',
  preProcessSelfCheckAbnormal: false,
  remark: '',
  reportDate: dayjs().format('YYYY-MM-DD'),
  reportType: 'PRODUCT' as Adhesive2ReportType,
  selfCheck: 'OK',
  sourceCode: '',
  startPosition: 0,
  endPosition: 0,
  sourceGrindingSecondDetailId: undefined as number | undefined,
  sourceProductionBatchNo: '',
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
  { dataIndex: 'sourceBatchNo', title: '分段批号' },
  { dataIndex: 'modelCode', title: '产品型号' },
  { dataIndex: 'planSizeSpec', title: '计划胶板尺寸' },
  { dataIndex: 'equipmentCode', title: '设备编码' },
  { dataIndex: 'availableSourceLength', title: '可加工数' },
  { dataIndex: 'status', title: '状态' },
  { dataIndex: 'action', fixed: 'right' as const, title: '操作' },
];
const taskListColumnWidthRules: Record<string, { max: number; min: number; padding: number }> = {
  action: { max: 86, min: 72, padding: 24 },
  availableSourceLength: { max: 112, min: 92, padding: 28 },
  equipmentCode: { max: 190, min: 106, padding: 32 },
  modelCode: { max: 220, min: 108, padding: 32 },
  planNo: { max: 260, min: 110, padding: 32 },
  planSizeSpec: { max: 220, min: 120, padding: 32 },
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
  { label: '已暂停', value: 'PAUSED' },
  { label: '已完工', value: 'FINISHED' },
  { label: '已取消', value: 'CANCELLED' },
];

const glueBoardStockSelectColumns = [
  { dataIndex: 'glueBoardBatchNo', title: '胶板批号', width: 180 },
  { dataIndex: 'glueBoardModel', title: '胶板型号', width: 120 },
  { dataIndex: 'glueBoardMaterialCode', title: '胶板料号', width: 130 },
  { dataIndex: 'receiveTime', title: '领用时间', width: 170 },
  { dataIndex: 'availableLength', title: '边库余量(m)', width: 120 },
  { dataIndex: 'qualityStatus', title: '今日质量状态', width: 120 },
  { dataIndex: 'action', fixed: 'right' as const, title: '操作', width: 190 },
];

const adhesive2FilterStatusOptions = [
  { label: '全部', value: 'ALL' },
  { label: '待确认', value: 'PENDING' },
  { label: '本工序NG', value: 'CURRENT_NG' },
  { label: '其他工序NG', value: 'SOURCE_NG' },
  { label: '已确认', value: 'COMPLETED' },
];

const adhesive2IntermediatePositions = [
  { label: '首件', value: 'FIRST' },
  { label: '前段', value: 'FRONT' },
  { label: '中段', value: 'MIDDLE' },
  { label: '后段', value: 'END' },
];

const adhesive2ReportTypeOptions: Array<{
  label: string;
  value: Adhesive2ReportType;
}> = [
  { label: '工艺参数点检', value: 'CHANGEOVER' },
  { label: '成品加工', value: 'PRODUCT' },
  { label: '中间品-前段', value: 'FRONT' },
  { label: '中间品-中段', value: 'MIDDLE' },
  { label: '中间品-后段', value: 'END' },
  { label: '过程加检', value: 'PROCESS_CHECK' },
];

const adhesive2ReportTypeFilterOptions = [{ label: '全部作业', value: 'ALL' }, ...adhesive2ReportTypeOptions];

const adhesive2VisualItemNames = ['黑点', '蓝点', '黄点', '红点', '针孔', '条纹', '褶皱', '波浪纹', '其他'];
const abnormalCategoryCorrectionOptions = adhesive2VisualItemNames.map((item) => ({ label: item, value: item }));
const adhesive2MeasuredPositions = adhesive2IntermediatePositions.filter((item) => item.value !== 'FIRST');

const adhesive2MiddleCheckItems = ref<AdhesiveCheckItem[]>([
  {
    abnormalRemark: '',
    actualValue: '',
    checkResult: 'OK',
    itemCategory: '中间品',
    itemName: '宽幅/mm',
    sortNo: 501,
    standardValue: '/',
  },
  ...Array.from({ length: 10 }, (_, index) => ({
    abnormalRemark: '',
    actualValue: '',
    checkResult: 'OK',
    itemCategory: '中间品',
    itemName: `${(index + 1) * 100}cm厚度/mm`,
    sortNo: 502 + index,
    standardValue: '1.500±0.050',
  })),
]);

const adhesive2ProcessCheckItems = ref<AdhesiveCheckItem[]>([
  {
    abnormalRemark: '',
    actualValue: buildNowText(),
    checkResult: 'OK',
    itemCategory: '过程加检',
    itemName: '检测时间',
    sortNo: 601,
    standardValue: '扫码触发',
  },
  {
    abnormalRemark: '',
    actualValue: '待检',
    checkResult: 'OK',
    itemCategory: '过程加检',
    itemName: '反馈状态',
    sortNo: 602,
    standardValue: 'QMS反馈',
  },
]);

const adhesive2MiddleLedgerColumns = [
  { dataIndex: 'batchNo', title: '批号', width: 190 },
  { dataIndex: 'recordDate', title: '生产日期', width: 180 },
  { dataIndex: 'modelCode', title: '型号', width: 140 },
  { dataIndex: 'materialCode', title: '料号', width: 150 },
  { dataIndex: 'glueBoardModel', title: '胶板型号', width: 150 },
  { dataIndex: 'glueBoardWidthMm', title: '宽幅/MM', width: 120 },
  { dataIndex: 'glueBoardAdhesionGf', title: '使用面粘性/gf', width: 160 },
  { dataIndex: 'inputQty', title: '投入数量', width: 110 },
  { dataIndex: 'outputQty', title: '产出数量', width: 110 },
  { dataIndex: 'recordStatus', title: '状态', width: 110 },
  { dataIndex: 'recorderName', title: '填写人', width: 120 },
  { dataIndex: 'confirmerName', title: '确认人', width: 120 },
  { dataIndex: 'action', fixed: 'right' as const, title: '操作', width: 130 },
];

const adhesive2ProcessCheckFaiColumns = [
  { dataIndex: 'faiNo', title: 'FAI单号', width: 180 },
  { dataIndex: 'productBatchNo', title: '粘胶2片号', width: 190 },
  { dataIndex: 'inspectionScopeBatchNo', title: '分段批号', width: 180 },
  { dataIndex: 'faiStatus', title: '状态', width: 120 },
  { dataIndex: 'faiJudgment', title: '检测结论', width: 120 },
  { dataIndex: 'faiStandardNo', title: '标准号', width: 160 },
  { dataIndex: 'faiApplyTime', title: '申请时间', width: 170 },
  { dataIndex: 'sourceReportNo', title: '来源单号', width: 260 },
  { dataIndex: 'remark', title: '备注', width: 280 },
  { dataIndex: 'action', fixed: 'right' as const, title: '操作', width: 210 },
];

const ADHESIVE2_INTERMEDIATE_DEFAULT_THICKNESS_STANDARD = '1.500±0.050';
const ADHESIVE2_INTERMEDIATE_DEFAULT_THICKNESS_COLUMN_COUNT = 10;
const ADHESIVE2_INTERMEDIATE_DEFAULT_THICKNESS_INTERVAL_CM = 100;

const intermediateTemplateSchema = computed(() => parseIntermediateStationFormSchema());
const intermediateThicknessStandardText = computed(() => normalizeIntermediateThicknessStandard(readIntermediateTemplateText('thicknessStandard', 'intermediateThicknessStandard')));
const intermediateThicknessColumnCount = computed(() => readIntermediateTemplateNumber('thicknessColumnCount', 'thicknessColumns') || ADHESIVE2_INTERMEDIATE_DEFAULT_THICKNESS_COLUMN_COUNT);
const intermediateThicknessIntervalCm = computed(() => readIntermediateTemplateNumber('thicknessIntervalCm', 'thicknessMeasureIntervalCm') || ADHESIVE2_INTERMEDIATE_DEFAULT_THICKNESS_INTERVAL_CM);

const intermediateDetailColumns = computed(() => {
  const standard = intermediateThicknessStandardText.value;
  const interval = intermediateThicknessIntervalCm.value;
  const count = Math.min(Math.max(intermediateThicknessColumnCount.value, 1), 10);
  const thicknessColumns = Array.from({ length: count }, (_, index) => ({
    dataIndex: `thickness${index + 1}`,
    title: `${(index + 1) * interval}cm厚度/mm(${standard})`,
    width: index === count - 1 ? 196 : 190,
  }));
  return [
    { align: 'center' as const, dataIndex: 'seq', title: '序号', width: 48 },
    { dataIndex: 'samplePositionName', title: '采样段', width: 82 },
    { dataIndex: 'sliceBatchNo', title: '片号', width: 190 },
    ...thicknessColumns,
    { dataIndex: 'remark', title: '备注', width: 160 },
  ];
});

const coaInspectionColumns = [
  { dataIndex: 'batchNo', title: '粘胶2成品片号', width: 210 },
  { dataIndex: 'motherBatchNo', title: '分段批号', width: 190 },
  { dataIndex: 'reportStatus', title: '确认状态', width: 110 },
  { dataIndex: 'confirmTime', title: '扫码确认时间', width: 170 },
  { dataIndex: 'action', fixed: 'right' as const, title: '选择', width: 90 },
];

const selectedDailyRecord = computed(() => dailyRecordRows.value.find((item) => item.key === dailyRecordKey.value));
const selectedReportRecords = computed(() => reportRecords.value.filter((record) => selectedReportRecordKeys.value.includes(record.id as number | string)));
const printableReportRecords = computed(() => selectedReportRecords.value.filter((record) => isPrintableTransferReport(record)));
// 流转单打印不以报工确认状态、NG 状态或既往打印状态为门槛；粘胶2仅需片号已选择尾号。
const allPrintableReportRecords = computed(() => reportRecords.value.filter((record) => isPrintableTransferReport(record)));
const glueBoardStatusMeta = computed(() => {
  if (glueBoard.alarm && glueBoard.alarm.includes('不匹配')) return { color: 'red', text: '型号不匹配' };
  if (glueBoard.alarm) return { color: 'red', text: '状态提醒' };
  if (!glueBoard.id && glueBoard.stockLength <= 0) return { color: 'default', text: '未加载' };
  return { color: 'green', text: '正常' };
});
const glueBoardAvailableRange = computed(() => {
  const start = Number(glueBoard.availableStartPosition || 0);
  const length = Number(glueBoard.stockLength || 0);
  return `${formatNumber(start)}-${formatNumber(start + length)} m`;
});
const glueBoardAvailableText = computed(() => `${formatNumber(glueBoard.stockLength)} m`);
const glueBoardMapCandidateModels = computed(() => getDistinctCandidateValues(glueBoardMapCandidates.value.map((item) => item.glueBoardModel)));
const glueBoardMapCandidateModelQuery = computed(() => glueBoardMapCandidateModels.value.join(','));
const glueBoardMapCandidateText = computed(() => glueBoardMapCandidates.value.map(formatGlueBoardMapCandidate).filter(Boolean).join('、'));
const glueBoardMapFilterNotice = computed(() => {
  const productModelCode = currentProductModelCode.value;
  if (glueBoardMapLoading.value) return '正在匹配当前产品型号的胶板对照...';
  if (!productModelCode) return '未选择待加工计划，已放开查询；请注意选择对应待加工计划。';
  if (glueBoardMapCandidates.value.length > 0) {
    return `按产品型号 ${productModelCode} 的成品胶板对照过滤`;
  }
  return `未维护产品型号 ${productModelCode} 的粘胶2胶板对照，已放开查询；请注意选择对应待加工计划。`;
});
const glueBoardMapMismatchTip = computed(() =>
  buildGlueBoardMapMismatchTip({
    glueBoardBatchNo: glueBoard.batchNo,
    glueBoardMaterialCode: glueBoard.materialCode,
    glueBoardModel: getCurrentGlueBoardModelForGuard(),
  }),
);
const glueBoardTitleTip = computed(() => {
  const messages = Array.from(new Set([glueBoardMapMismatchTip.value, glueBoard.alarm].filter(Boolean)));
  if (messages.length > 0) return messages.join('；');
  return !glueBoard.id ? '请先登记胶板领用，系统会加载当前可用长度。' : '报工前先登记胶板领用，并可提交胶板送检。';
});
const glueBoardTitleTipWarning = computed(() => !!glueBoardMapMismatchTip.value || !!glueBoard.alarm);
const glueBoardAqcStatusMeta = computed(() => {
  if (!glueBoard.stockId && !firstInspection.value.faiId) {
    return {
      color: 'default',
      stampClass: 'empty',
      stampText: '未领用',
      subText: '未登记',
      text: '未领用',
    };
  }
  if (!hasGlueBoardInspectionRecord()) {
    return {
      color: 'default',
      stampClass: 'empty',
      stampText: '未送检',
      subText: firstInspection.value.faiId ? '有历史单' : '未登记',
      text: '未送检',
    };
  }
  const judgment = String(glueBoard.latestInspectionResult || 'PENDING').toUpperCase();
  const status = resolveGlueBoardInspectionStatus(judgment, glueBoard.latestInspectionId);
  if (status === 'PENDING') {
    return {
      color: 'processing',
      stampClass: 'waiting',
      stampText: '已送检',
      subText: '待检测',
      text: '待检测',
    };
  }
  if (status === 'INSPECTING') {
    return {
      color: 'processing',
      stampClass: 'waiting',
      stampText: '已送检',
      subText: '检测中',
      text: '检测中',
    };
  }
  if (status === 'WAITING_QA') {
    return {
      color: 'processing',
      stampClass: 'waiting',
      stampText: '已送检',
      subText: '待复核',
      text: '待复核',
    };
  }
  if (status === 'COMPLETED' && judgment === 'OK') {
    return {
      color: 'green',
      stampClass: 'ok',
      stampText: '已放行',
      subText: 'OK',
      text: 'OK',
    };
  }
  if (status === 'COMPLETED' && judgment === 'NG') {
    return {
      color: 'red',
      stampClass: 'ng',
      stampText: '检验NG',
      subText: '待重检',
      text: '待重检',
    };
  }
  if (status === 'REJECTED') {
    return {
      color: 'red',
      stampClass: 'ng',
      stampText: '已退回',
      subText: '待重送',
      text: '待重送',
    };
  }
  if (status === 'CANCELED') {
    return {
      color: 'red',
      stampClass: 'ng',
      stampText: '已取消',
      subText: '待重送',
      text: '待重送',
    };
  }
  return {
    color: 'default',
    stampClass: 'empty',
    stampText: '未送检',
    subText: judgment,
    text: '未送检',
  };
});
const glueBoardLatestInspectionStatusMeta = computed(() => {
  const status = String(firstInspection.value.faiStatus || '').toUpperCase();
  const judgment = String(firstInspection.value.faiJudgment || '').toUpperCase();
  if (!firstInspection.value.faiId) {
    return {
      color: 'default',
      stampClass: 'empty',
      stampText: '未送检',
      subText: '未登记',
      text: '未送检',
    };
  }
  if (status === 'PENDING') {
    return {
      color: 'processing',
      stampClass: 'waiting',
      stampText: '已送检',
      subText: '待检测',
      text: '待检测',
    };
  }
  if (status === 'INSPECTING') {
    return {
      color: 'processing',
      stampClass: 'waiting',
      stampText: '已送检',
      subText: '检测中',
      text: '检测中',
    };
  }
  if (status === 'WAITING_QA') {
    return {
      color: 'processing',
      stampClass: 'waiting',
      stampText: '已送检',
      subText: '待复核',
      text: '待复核',
    };
  }
  if (status === 'COMPLETED' && judgment === 'OK') {
    return {
      color: 'green',
      stampClass: 'ok',
      stampText: '已放行',
      subText: 'OK',
      text: 'OK',
    };
  }
  if (status === 'COMPLETED' && judgment === 'NG') {
    return {
      color: 'red',
      stampClass: 'ng',
      stampText: '检验NG',
      subText: '待重检',
      text: '待重检',
    };
  }
  if (status === 'REJECTED') {
    return {
      color: 'red',
      stampClass: 'ng',
      stampText: '已退回',
      subText: '待重送',
      text: '待重送',
    };
  }
  if (status === 'CANCELED') {
    return {
      color: 'red',
      stampClass: 'ng',
      stampText: '已取消',
      subText: '待重送',
      text: '待重送',
    };
  }
  return {
    color: 'default',
    stampClass: 'empty',
    stampText: firstInspection.value.displayText || '未送检',
    subText: judgment,
    text: firstInspection.value.displayText || '-',
  };
});
const glueBoardAqcStampTime = computed(() => {
  if (!hasGlueBoardInspectionRecord()) return '未送检';
  return glueBoard.inspectionSubmitTime || firstInspection.value.faiApplyTime || '已送检';
});
const glueBoardInspectionTabNotice = computed(() => {
  const latestText = firstInspection.value.faiId ? `当前批号最后一次送检：${firstInspection.value.faiNo || firstInspection.value.sourceReportNo || firstInspection.value.faiId}` : '当前批号暂无送检单';
  if (!glueBoard.stockId && !glueBoard.batchNo) {
    return '当前未登记胶板领用，请先登记胶板批号后再提交胶板送检。';
  }
  if (!hasGlueBoardInspectionRecord()) {
    return `${latestText}；当前尚未提交胶板送检，可点击“胶板送检”完成送检登记。`;
  }
  return `${latestText}；顶部状态显示当前胶板送检结果。`;
});
const reportRecordRowSelection = computed(() => ({
  fixed: true,
  onChange: (keys: (number | string)[]) => {
    selectedReportRecordKeys.value = keys;
  },
  selectedRowKeys: selectedReportRecordKeys.value,
}));
const reportOutputLength = computed(() => Math.max(Number(reportForm.processLength || 0) - Number(reportForm.lossLength || 0) - Number(reportForm.napSampleLength || 0), 0));
const currentMotherBatchNo = computed(() =>
  resolveMotherBatchNo({
    parentProductionBatchNo: currentPlan.sourceBatchNo || currentPlan.batchNo,
    sourceProductionBatchNo: currentPlan.sourceProductionBatchNo,
  }),
);
const visibleSourceGroups = computed<SourceGroup[]>(() =>
  sourceGroups.value
    .filter((group) => !currentMotherBatchNo.value || group.baseBatchNo === currentMotherBatchNo.value)
    .map((group) => {
      const segments = group.segments.filter(matchAdhesive2Segment);
      return {
        ...group,
        segments,
        totalAvailableLength: segments.reduce((sum, item) => sum + getAdhesive2SegmentAvailableLength(item), 0),
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
  const rows = new Map<string, Adhesive2TransferPrintTarget>();
  visualDisplaySourceGroups.value.forEach((group) => {
    group.segments.forEach((segment) => {
      if (!isPrintableTransferSegment(segment)) return;
      const target: Adhesive2TransferPrintTarget = {
        group,
        record: findReportBySegment(segment),
        segment,
      };
      rows.set(getTransferPrintTargetKey(target.segment), target);
    });
  });
  return Array.from(rows.values());
});
const selectedTransferTargets = computed(() => selectedTransferReportIds.value.map((id) => selectableTransferTargets.value.find((target) => getTransferPrintTargetKey(target.segment) === id)).filter((target): target is Adhesive2TransferPrintTarget => !!target));
const selectableTransferReportCount = computed(() => selectableTransferTargets.value.length);
const selectedTransferReportCount = computed(() => selectedTransferTargets.value.length);
const allTransferReportsSelected = computed(() => selectableTransferReportCount.value > 0 && selectableTransferTargets.value.every((target) => selectedTransferReportIds.value.includes(getTransferPrintTargetKey(target.segment))));
const visibleAdhesive2SliceCount = computed(() => visibleSourceGroups.value.reduce((sum, group) => sum + group.segments.length, 0));
const visibleAdhesive2CompletedCount = computed(() => visibleSourceGroups.value.reduce((sum, group) => sum + group.segments.filter((segment) => getAdhesive2SegmentStatus(segment) === 'COMPLETED').length, 0));
const visibleAdhesive2PendingConfirmCount = computed(() => visibleSourceGroups.value.reduce((sum, group) => sum + group.segments.filter((segment) => getAdhesive2SegmentStatus(segment) === 'PENDING').length, 0));
const visibleAdhesive2CurrentNgCount = computed(() => visibleSourceGroups.value.reduce((sum, group) => sum + group.segments.filter((segment) => getAdhesive2SegmentStatus(segment) === 'CURRENT_NG').length, 0));
const visibleAdhesive2SourceNgCount = computed(() => visibleSourceGroups.value.reduce((sum, group) => sum + group.segments.filter((segment) => getAdhesive2SegmentStatus(segment) === 'SOURCE_NG').length, 0));
const coaInspectionReportRows = computed(() =>
  coaReportRecords.value
    .filter((record) => !!record.id && !!String(record.productionBatchNo || '').trim())
    .map((record) => ({
      batchNo: String(record.productionBatchNo || '').trim(),
      confirmTime: record.confirmerTime || '',
      key: `ADHESIVE2-${record.id}`,
      blockedReason: getCoaReportBlockedReason(record),
      motherBatchNo: resolveMotherBatchNo(record as Record<string, any>),
      reportStatus: String(record.reportStatus || 'DRAFT').toUpperCase(),
      sourceProductionBatchNo: String(record.sourceProductionBatchNo || '').trim(),
      sourceReportId: Number(record.id),
    }))
    .filter((row) => !currentMotherBatchNo.value || isSameText(row.motherBatchNo, currentMotherBatchNo.value)),
);
const coaInspectionRows = computed(() => coaInspectionReportRows.value.filter((row) => !row.blockedReason));
const coaInspectionBlockedRows = computed(() => coaInspectionReportRows.value.filter((row) => !!row.blockedReason));
const coaInspectionEmptyMessage = computed(() =>
  coaInspectionReportRows.value.length
    ? '当前范围内的粘胶2成品报工均因质量异常或锁定不可送检，请先处理以下异常。'
    : '当前范围内暂无已保存且已生成成品片号的粘胶2报工记录，请先保存报工并选择尾号。',
);
const selectedCoaInspectionRow = computed(() =>
  coaInspectionRows.value.find(
    (row) =>
      String(row.batchNo || '')
        .trim()
        .toUpperCase() ===
      String(selectedCoaSliceNo.value || '')
        .trim()
        .toUpperCase(),
  ),
);
const selectedCoaSliceDisplay = computed(() => selectedCoaInspectionRow.value?.batchNo || selectedCoaSliceNo.value || coaInspectionScanForm.normalizedSliceNo || normalizeCoaInspectionScanNo(coaInspectionScanForm.scannedSliceNo) || '');
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
    feedbackResult: extra.feedbackResult || extra.inspectionResult || extra.visualInspectionResult || reportForm.aqcStatus || '-',
    feedbackTime: extra.feedbackTime || extra.visualInspectionTime || '',
    submitTime: extra.submitTime || extra.visualInspectionTime || reportForm.startTime || '',
  };
});

const shouldShowProcessParameterTab = computed(() => reportForm.reportType === 'CHANGEOVER');
const shouldShowVisualInspectionTab = computed(() => reportForm.reportType !== 'CHANGEOVER');
const isVisualInspectionEditable = computed(() => reportDialogMode.value !== 'view' && shouldShowVisualInspectionTab.value);
const hasActiveVisualItems = computed(() => visualInspectionItems.value.some((item) => isVisualItemActive(item)));
const canCorrectAbnormalCategory = computed(
  () => reportDialogMode.value === 'view' && !!activeRecord.value?.id && hasVisualIssueFromRecord(activeRecord.value as Record<string, any>) && hasAccessByCodes([ABNORMAL_CATEGORY_CORRECT_PERMISSION]),
);

function buildDefaultAdhesive2IntermediateDetails(): MesHcAdhesiveConsoleApi.IntermediateDetail[] {
  return adhesive2MeasuredPositions.map((item, index) => ({
    samplePosition: item.value,
    samplePositionName: item.label,
    remark: '',
    sortNo: index + 1,
  }));
}

const intermediateTableRows = computed(() =>
  intermediateDetails.value.map((row, index) => {
    (row as MesHcAdhesiveConsoleApi.IntermediateDetail & { seq: number }).seq = index + 1;
    return row as MesHcAdhesiveConsoleApi.IntermediateDetail & { seq: number };
  }),
);

const reportCheckCategories = computed(() => {
  const categories: string[] = [];
  if (shouldShowProcessParameterTab.value) {
    const baseItems = ensureAdhesive2ChangeoverItems(checkTemplate.value);
    categories.push(...Array.from(new Set(baseItems.map((item) => item.itemCategory || '其他').filter((category) => category !== '粘胶2段半成品'))));
  }
  if (isAdhesive2MiddleReportType(reportForm.reportType)) {
    categories.push('中间品');
  }
  if (reportForm.reportType === 'PROCESS_CHECK') {
    categories.push('过程加检');
  }
  const order = ['环境', '粘胶机', '胶板', '工艺参数', '过程加检', '中间品', '其他'];
  return Array.from(new Set(categories)).sort((a, b) => {
    const aIndex = order.includes(a) ? order.indexOf(a) : order.length;
    const bIndex = order.includes(b) ? order.indexOf(b) : order.length;
    return aIndex - bIndex;
  });
});
const reportSubmitButtonText = computed(() => {
  if (activeReportTab.value === 'visual-inspection') return '保存并扫码确认';
  if (shouldShowVisualInspectionTab.value) return '下一步：外观检验';
  return '保存并扫码确认';
});
const startReportWorkButtonText = computed(() => {
  if (isAdhesive2MiddleReportType(reportForm.reportType)) return '开始填写中间品';
  if (shouldShowVisualInspectionTab.value) return '开始外观检验';
  return '开工并进入工艺参数';
});
const reportDialogTitle = computed(() => {
  if (reportDialogMode.value === 'view') return '粘胶2片详情查看';
  return '粘胶2扫码确认与外观检验';
});
const recordConfirmTypeBasis = computed(() => {
  return '扫码确认默认按成品加工登记，确认后直接进入外观检验；中间品前/中/后段请在已确认详情中手动设置。';
});

const recordColumns = [
  {
    dataIndex: 'parentProductionBatchNo',
    key: 'motherBatchNo',
    title: '分段批号',
    width: 180,
  },
  {
    dataIndex: 'productionBatchNo',
    key: 'productionBatchNo',
    title: '粘胶2片号',
    width: 160,
  },
  {
    dataIndex: 'confirmerTime',
    key: 'scanConfirmDate',
    title: '扫码确认日期',
    width: 130,
  },
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
];

const changeoverInspectionColumns = [
  { dataIndex: 'recordTime', title: '记录时间', width: 170 },
  { dataIndex: 'motherSegmentBatchNo', title: '分段批号', width: 180 },
  { dataIndex: 'recordStatus', title: '状态', width: 110 },
  { dataIndex: 'recorderName', title: '检验人', width: 120 },
  { dataIndex: 'confirmUserName', title: '确认人', width: 120 },
  { dataIndex: 'action', fixed: 'right' as const, title: '操作', width: 190 },
];

const reportProcessColumns = [
  { dataIndex: 'itemCategory', title: '类别', width: 120 },
  { dataIndex: 'itemName', title: '工艺参数项目', width: 170 },
  { dataIndex: 'standardValue', title: '标准', width: 180 },
  { dataIndex: 'actualValue', title: '实测值', width: 200 },
  { dataIndex: 'abnormalRemark', title: '异常备注', width: 180 },
];
const changeoverCheckTableScroll = computed(() => {
  return {
    x: 980,
    y: 'calc(100vh - 278px)',
  };
});

function normalizeRows(payload: any): any[] {
  const source = payload?.data ?? payload;
  if (Array.isArray(source)) return source;
  if (Array.isArray(source?.list)) return source.list;
  if (Array.isArray(source?.rows)) return source.rows;
  if (Array.isArray(source?.records)) return source.records;
  return [];
}

function normalizePageTotal(payload: any) {
  const source = payload?.data ?? payload;
  const total = Number(source?.total ?? source?.totalCount ?? source?.count ?? 0);
  return Number.isFinite(total) ? total : 0;
}

function formatNumber(value?: number) {
  const num = Number(value || 0);
  if (!Number.isFinite(num)) return '0';
  return Number(num.toFixed(3)).toString();
}

function getAdhesive2QtimeText(qtime?: MesHcAdhesiveConsoleApi.QtimeInfo) {
  if (!qtime) return '-';
  if (qtime.status === 'WAITING_SOURCE_FINISH') return '0分钟（并行开工）';
  if (qtime.status === 'MISSING_SOURCE_TIME') return '上道未完工';
  if (qtime.status === 'NO_RULE') return '未配置';
  const minutes = Math.max(0, Number(qtime.elapsedMinutes || 0));
  return qtime.timeout ? `超时 ${minutes}分钟` : `${minutes}分钟`;
}

function getAdhesive2QtimeColor(qtime?: MesHcAdhesiveConsoleApi.QtimeInfo) {
  if (qtime?.timeout) return 'red';
  if (qtime?.status === 'WAITING_SOURCE_FINISH') return 'blue';
  if (qtime?.status === 'NORMAL') return 'green';
  return 'default';
}

function mergeAdhesive2BatchQtime(current?: MesHcAdhesiveConsoleApi.QtimeInfo, candidate?: MesHcAdhesiveConsoleApi.QtimeInfo) {
  if (!current) return candidate;
  if (!candidate) return current;
  if (!current.targetStarted) return candidate.targetStarted ? candidate : current;
  if (!candidate.targetStarted) return current;
  const currentStart = String(current.targetStartTime || '9999-12-31 23:59:59');
  const candidateStart = String(candidate.targetStartTime || '9999-12-31 23:59:59');
  return candidateStart < currentStart ? candidate : current;
}

function getDistinctCandidateValues(values: Array<string | undefined>) {
  return Array.from(new Set(values.map((value) => String(value || '').trim()).filter(Boolean)));
}

function formatGlueBoardMapCandidate(item: MesHcFinishedGlueBoardMapApi.FinishedGlueBoardMapItem) {
  const materialCode = String(item.glueBoardMaterialCode || '').trim();
  const model = String(item.glueBoardModel || '').trim();
  return [materialCode, model].filter(Boolean).join(' / ');
}

function getGlueBoardMapCandidateDisplay() {
  return glueBoardMapCandidateText.value || '未维护胶板型号';
}

function getCurrentGlueBoardModelForGuard() {
  if (!glueBoard.batchNo && !glueBoard.stockId && !glueBoard.id) return '';
  return String(glueBoard.model || getSelectedGlueBoardModel() || '').trim();
}

function buildGlueBoardMapMismatchTip(stock: { glueBoardBatchNo?: string; glueBoardMaterialCode?: string; glueBoardModel?: string }) {
  const productModelCode = currentProductModelCode.value;
  if (!productModelCode || glueBoardMapCandidates.value.length === 0) return '';
  const currentModel = String(stock.glueBoardModel || '').trim();
  const currentMaterialCode = String(stock.glueBoardMaterialCode || '').trim();
  const matched = glueBoardMapCandidates.value.some((candidate) => {
    const expectedModel = String(candidate.glueBoardModel || '').trim();
    const expectedMaterialCode = String(candidate.glueBoardMaterialCode || '').trim();
    const modelMatched = expectedModel ? currentModel.toUpperCase() === expectedModel.toUpperCase() : true;
    const materialMatched = expectedMaterialCode ? currentMaterialCode.toUpperCase() === expectedMaterialCode.toUpperCase() : true;
    return modelMatched && materialMatched;
  });
  if (!currentModel || matched) return '';
  const currentDisplay = [stock.glueBoardMaterialCode, currentModel, stock.glueBoardBatchNo]
    .map((item) => String(item || '').trim())
    .filter(Boolean)
    .join(' / ');
  return `当前产品型号 ${productModelCode} 建议领用胶板 ${getGlueBoardMapCandidateDisplay()}，当前胶板 ${currentDisplay || currentModel} 不匹配，请重新选择批次。`;
}

function roundMeter(value: number) {
  if (!Number.isFinite(value)) return 0;
  return Number(value.toFixed(3));
}

function buildReportRange(record: MesHcAdhesiveConsoleApi.ReportItem): SourceReportRange | null {
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

function getReportRanges(sourceProductionBatchNo: string) {
  return reportRecords.value
    .filter((item) => String(item.sourceProductionBatchNo || '') === sourceProductionBatchNo)
    .filter(isAdhesive2ReportDeductible)
    .map(buildReportRange)
    .filter((item): item is SourceReportRange => !!item)
    .sort((a, b) => a.start - b.start || a.end - b.end);
}

function getReportRecordsBySourceBatchNo(sourceProductionBatchNo?: string) {
  const batchNo = String(sourceProductionBatchNo || '')
    .trim()
    .toUpperCase();
  if (!batchNo) return [];
  return reportRecords.value.filter(
    (item) =>
      String(item.sourceProductionBatchNo || '')
        .trim()
        .toUpperCase() === batchNo,
  );
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
  const scanned = String(batchNo || '')
    .trim()
    .toUpperCase();
  if (!scanned) return undefined;
  const scannedBase = scanned.replace(/([PQRS]\d{3})[AB]$/i, '$1');
  for (const group of sourceGroups.value) {
    const segment = group.segments.find((item) => {
      const sourceBatchNo = String(item.batchNo || '')
        .trim()
        .toUpperCase();
      const displayBatchNo = getAdhesive2SegmentDisplayBatchNo(item);
      const sourceBase = sourceBatchNo.replace(/([PQRS]\d{3})[AB]$/i, '$1');
      return sourceBatchNo === scanned || displayBatchNo === scanned || sourceBase === scannedBase;
    });
    if (segment) return { group, segment };
  }
  const report = reportRecords.value.find(
    (item) =>
      String(item.productionBatchNo || '')
        .trim()
        .toUpperCase() === scanned,
  );
  if (report?.sourceProductionBatchNo) {
    for (const group of sourceGroups.value) {
      const segment = group.segments.find(
        (item) =>
          String(item.batchNo || '')
            .trim()
            .toUpperCase() ===
          String(report.sourceProductionBatchNo || '')
            .trim()
            .toUpperCase(),
      );
      if (segment) return { group, segment };
    }
  }
  return undefined;
}

function findSourceGroup(batchNo?: string) {
  const normalized = normalizeAdhesive2MiddleBatchNo(batchNo);
  if (!normalized) return undefined;
  return sourceGroups.value.find((group) => normalizeAdhesive2MiddleBatchNo(group.baseBatchNo) === normalized);
}

function getActiveSourceGroup() {
  const groupBatchNo = currentMotherBatchNo.value || currentPlan.sourceProductionBatchNo || currentPlan.sourceBatchNo || currentPlan.batchNo;
  if (groupBatchNo) {
    const matched = findSourceGroup(groupBatchNo);
    if (matched) return matched;
  }
  return sourceGroups.value.length === 1 ? sourceGroups.value[0] : undefined;
}

function getRangeStyle(range: SourceReportRange, segment: SourceSegment) {
  const outputLength = Number(segment.outputLength || 0);
  if (outputLength <= 0) return { left: '0%', width: '0%' };
  const left = Math.min(Math.max((range.start / outputLength) * 100, 0), 100);
  const width = Math.min(Math.max(((range.end - range.start) / outputLength) * 100, 0), 100 - left);
  return { left: `${left}%`, width: `${width}%` };
}

function getAdhesive2GridColumns(_count: number) {
  return 'repeat(auto-fill, 168px)';
}

function getAdhesive2SegmentStatus(segment: SourceSegment): Adhesive2SegmentStatus {
  if (getAdhesive2SegmentCurrentNgText(segment)) return 'CURRENT_NG';
  if (getAdhesive2SegmentSourceNgText(segment)) return 'SOURCE_NG';
  if (isSegmentCompleted(segment)) return 'COMPLETED';
  if (hasAdhesive2SegmentCoaConclusion(segment)) return 'COMPLETED';
  if (isAdhesive2SegmentPendingConfirm(segment) || getAdhesive2SegmentAvailableLength(segment) > 0) return 'PENDING';
  return 'COMPLETED';
}

function getAdhesive2SegmentNgStatusClass(segment: SourceSegment) {
  const status = getAdhesive2SegmentStatus(segment);
  if (status === 'CURRENT_NG') return 'is-current-ng';
  if (status === 'SOURCE_NG') return 'is-source-ng';
  return '';
}

function getAdhesive2SegmentNgReason(segment: SourceSegment) {
  if (getAdhesive2SegmentCurrentNgText(segment)) {
    const existedRecord = findReportBySegment(segment);
    const downstreamFeedbackTitle = getDownstreamPreProcessFeedbackTitle(existedRecord);
    if (downstreamFeedbackTitle) return downstreamFeedbackTitle;
    return getNgReasonText(existedRecord) || '本工序外观检测 NG';
  }
  if (getAdhesive2SegmentSourceNgText(segment)) {
    const existedRecord = findReportBySegment(segment);
    if (existedRecord && isPreProcessSelfCheckAbnormal(parseRecordExtra(existedRecord))) {
      return getNgReasonText(existedRecord) || getAdhesive2SegmentSourceNgText(segment);
    }
    return segment.sourceNgReason || getAdhesive2SegmentSourceNgText(segment);
  }
  return '';
}

function normalizeAdhesive2CardResultText(value?: string) {
  const text = String(value || '').trim();
  const normalized = text.toUpperCase();
  if (!text || text === '-' || text.includes('未提交') || text.includes('草稿') || text.includes('取消')) return '';
  if (normalized === 'OK' || normalized === 'QUALIFIED' || text.includes('完成') || text.includes('合格') || text.includes('正常')) return '正常';
  if (normalized === 'NG' || normalized === 'ABNORMAL' || normalized === 'REJECTED' || text.includes('驳回') || text.includes('异常')) return 'NG';
  if (['PENDING', 'WAIT_QA', 'WAITING_QA', 'INSPECTING'].includes(normalized) || text.includes('待') || text.includes('检测') || text.includes('复核')) return '待检';
  return text;
}

function getAdhesive2CardResultTone(resultText?: string) {
  const text = normalizeAdhesive2CardResultText(resultText);
  if (text === 'NG') return 'is-ng';
  if (text === '正常') return 'is-ok';
  if (text === '待检') return 'is-pending';
  return '';
}

function getAdhesive2FaiResultText(record?: MesHcAdhesiveConsoleApi.FaiSummary | Record<string, any> | null) {
  if (!record?.faiId) return '';
  const status = String(record.faiStatus || '').toUpperCase();
  if (status === 'CANCELED') return '';
  const judgment = String(record.faiJudgment || '').toUpperCase();
  if (judgment === 'OK') return '正常';
  if (judgment === 'NG') return 'NG';
  if (status === 'COMPLETED') return '正常';
  if (['INSPECTING', 'PENDING', 'WAITING_QA', 'WAIT_QA'].includes(status)) return '待检';
  return normalizeAdhesive2CardResultText(record.displayText || getCoaFaiStatusMeta(record).text) || '待检';
}

function getAdhesive2FaiSortMeta(record?: MesHcAdhesiveConsoleApi.FaiSummary | Record<string, any> | null) {
  const timeText = String(record?.faiReturnTime || record?.faiApplyTime || '').trim();
  return {
    id: Number(record?.faiId || record?.sourceReportId || 0),
    time: timeText ? dayjs(timeText).valueOf() : 0,
  };
}

function findAdhesive2CoaFaiAnyBySegment(segment?: SourceSegment | null) {
  if (!segment) return null;
  const batchNo = normalizeAdhesive2CoaCompareNo(segment.batchNo);
  // 历史 COA 以压槽报工为来源；确认后成品 COA 以粘胶2报工为来源。
  // 两种来源都保留匹配，才能让片号卡在送检后实时显示“COA 待检”。
  const pressSlotReportId = Number(segment.grindingSecondDetailId || 0);
  const adhesive2ReportId = Number(findReportBySegment(segment)?.id || 0);
  return (
    coaFaiRows.value
      .filter((record) => isAdhesive2CoaFaiRecord(record))
      // 取消单保留在送检历史中，但不再参与当前片号标签和COA样片标记。
      .filter((record) => String(record.faiStatus || '').toUpperCase() !== 'CANCELED')
      .filter((record) => {
        const recordBatchNo = normalizeAdhesive2CoaCompareNo(record.productBatchNo);
        const recordSourceReportId = Number(record.sourceReportId || 0);
        return (
          (!!batchNo && recordBatchNo === batchNo) ||
          (!!pressSlotReportId && recordSourceReportId === pressSlotReportId) ||
          (!!adhesive2ReportId && recordSourceReportId === adhesive2ReportId)
        );
      })
      .sort((a, b) => getAdhesive2FaiSortMeta(b).time - getAdhesive2FaiSortMeta(a).time || Number(b.faiId || 0) - Number(a.faiId || 0))[0] || null
  );
}

function findAdhesive2ProcessCheckFaiBySegment(segment?: SourceSegment | null) {
  if (!segment) return null;
  const batchNo = normalizeAdhesive2CoaCompareNo(segment.batchNo);
  const sourceReportId = Number(findReportBySegment(segment)?.id || segment.grindingSecondDetailId || 0);
  return (
    processCheckFaiRows.value
      .filter((record) => {
        const recordBatchNo = normalizeAdhesive2CoaCompareNo(record.productBatchNo);
        const recordSourceReportId = Number(record.sourceReportId || 0);
        return (!!batchNo && recordBatchNo === batchNo) || (!!sourceReportId && recordSourceReportId === sourceReportId);
      })
      .sort((a, b) => getAdhesive2FaiSortMeta(b).time - getAdhesive2FaiSortMeta(a).time || Number(b.faiId || 0) - Number(a.faiId || 0))[0] || null
  );
}

function findSourcePressSlotFaiBySegment(rows: MesHcAdhesiveConsoleApi.FaiSummary[], segment?: SourceSegment | null) {
  if (!segment) return null;
  const batchNo = normalizeAdhesive2CoaCompareNo(segment.batchNo);
  const sourceReportId = Number(segment.grindingSecondDetailId || 0);
  return (
    rows
      .filter((record) => {
        const recordBatchNo = normalizeAdhesive2CoaCompareNo(record.productBatchNo);
        const recordSourceReportId = Number(record.sourceReportId || 0);
        return (!!batchNo && recordBatchNo === batchNo) || (!!sourceReportId && recordSourceReportId === sourceReportId);
      })
      .sort((a, b) => getAdhesive2FaiSortMeta(b).time - getAdhesive2FaiSortMeta(a).time || Number(b.faiId || 0) - Number(a.faiId || 0))[0] || null
  );
}

function findSourcePressSlotFirstInspectionBySegment(segment?: SourceSegment | null) {
  return findSourcePressSlotFaiBySegment(sourcePressSlotFirstInspectionRows.value, segment);
}

function findSourcePressSlotProcessCheckBySegment(segment?: SourceSegment | null) {
  return findSourcePressSlotFaiBySegment(sourcePressSlotProcessCheckRows.value, segment);
}

function getAdhesive2SegmentInspectionEntries(segment: SourceSegment) {
  const entries: Array<{ label: string; resultText: string; tone: string }> = [];
  const seen = new Set<string>();
  const pushEntry = (label: string, resultText = '') => {
    const normalizedResult = normalizeAdhesive2CardResultText(resultText);
    if (!label || !normalizedResult) return;
    const key = `${label}|${normalizedResult}`;
    if (seen.has(key)) return;
    seen.add(key);
    entries.push({
      label,
      resultText: normalizedResult,
      tone: getAdhesive2CardResultTone(normalizedResult),
    });
  };
  const sourcePressSlotFirstInspection = findSourcePressSlotFirstInspectionBySegment(segment);
  if (sourcePressSlotFirstInspection) {
    pushEntry('压槽首检', getAdhesive2FaiResultText(sourcePressSlotFirstInspection));
  }
  const sourcePressSlotProcessCheck = findSourcePressSlotProcessCheckBySegment(segment);
  if (sourcePressSlotProcessCheck) {
    pushEntry('压槽加检', getAdhesive2FaiResultText(sourcePressSlotProcessCheck));
  }
  const coaRecord = findAdhesive2CoaFaiAnyBySegment(segment);
  if (coaRecord || getAdhesive2SegmentCoaFlag(segment)) {
    pushEntry('COA', getAdhesive2FaiResultText(coaRecord) || '待检');
  }
  const firstInspectionBatchNo = normalizeAdhesive2CoaCompareNo((firstInspection.value as Record<string, any>)?.productBatchNo);
  if (firstInspection.value?.faiId && firstInspectionBatchNo && firstInspectionBatchNo === normalizeAdhesive2CoaCompareNo(segment.batchNo)) {
    pushEntry('首检', getAdhesive2FaiResultText(firstInspection.value));
  }
  const processCheckRecord = findAdhesive2ProcessCheckFaiBySegment(segment);
  if (processCheckRecord) {
    pushEntry('加检', getAdhesive2FaiResultText(processCheckRecord));
  }
  return entries;
}

function getAdhesive2SegmentInspectionBackgroundClass(segment: SourceSegment) {
  const primary = getAdhesive2SegmentInspectionEntries(segment)[0];
  if (!primary) return '';
  if (primary.resultText === 'NG') return 'is-inspection-bg-ng';
  if (primary.resultText === '待检') return 'is-inspection-bg-pending';
  if (primary.resultText === '正常') return 'is-inspection-bg-ok';
  return '';
}

function shouldShowAdhesive2ScanConfirmLine(segment: SourceSegment) {
  const status = getAdhesive2SegmentStatus(segment);
  return status === 'PENDING' || status === 'CURRENT_NG';
}

function getAdhesive2ScanConfirmText(segment: SourceSegment) {
  const existedRecord = findReportBySegment(segment);
  if (isAdhesiveRecordConfirmed(existedRecord?.reportStatus)) return '已确认';
  return isSegmentCompleted(segment) || getAdhesive2SegmentStatus(segment) === 'COMPLETED' ? '已确认' : '待确认';
}

function getAdhesive2ScanConfirmLineClass(segment: SourceSegment) {
  return getAdhesive2ScanConfirmText(segment) === '已确认' ? 'is-ok' : 'is-scan-pending';
}

function shouldShowAdhesive2SelfCheckLine(segment: SourceSegment) {
  if (getAdhesive2SegmentCurrentNgText(segment) || getAdhesive2SegmentSourceNgText(segment)) return true;
  if (getAdhesive2SegmentInspectionEntries(segment).length > 0) return false;
  return !!findReportBySegment(segment) || !!getAdhesive2SegmentSourceNgText(segment);
}

function formatAdhesive2SelfCheckLabel(processName?: string) {
  const normalized = normalizeProcessName(processName).replace(/工序$/, '');
  return normalized ? `${normalized}工序自检` : '自检';
}

function getAdhesive2SelfCheckLabel(segment: SourceSegment) {
  if (getAdhesive2SegmentCurrentNgText(segment)) return formatAdhesive2SelfCheckLabel('粘胶2');
  if (getAdhesive2SegmentSourceNgText(segment)) {
    const existedRecord = findReportBySegment(segment);
    if (existedRecord && isPreProcessSelfCheckAbnormal(parseRecordExtra(existedRecord))) {
      return formatAdhesive2SelfCheckLabel(resolveNgProcessName(existedRecord, '压槽'));
    }
    return formatAdhesive2SelfCheckLabel(segment.sourceNgProcessName || segment.sourceNgText?.replace(/工序NG$/, ''));
  }
  return '自检';
}

function getAdhesive2SelfCheckText(segment: SourceSegment) {
  if (getAdhesive2SegmentCurrentNgText(segment) || getAdhesive2SegmentSourceNgText(segment)) return 'NG';
  return findReportBySegment(segment) ? '正常' : '';
}

function getAdhesive2SelfCheckLineClass(segment: SourceSegment) {
  if (getAdhesive2SegmentCurrentNgText(segment) || getAdhesive2SegmentSourceNgText(segment)) return 'is-ng';
  return 'is-ok';
}

function getAdhesive2SegmentPositionText(segment: SourceSegment) {
  const reportType = getAdhesive2SegmentReportType(segment);
  if (reportType === 'FRONT') return '前段';
  if (reportType === 'MIDDLE') return '中段';
  if (reportType === 'END') return '后段';
  return '';
}

function matchAdhesive2Segment(segment: SourceSegment) {
  const keyword = visualFilterForm.batchNo.trim().toLowerCase();
  if (keyword) {
    const matched = [segment.batchNo, segment.segmentMark, segment.label, getAdhesive2SegmentReportNgText(segment), getAdhesive2SegmentNgReason(segment)].filter(Boolean).some((value) => String(value).toLowerCase().includes(keyword));
    if (!matched) return false;
  }
  const status = getAdhesive2SegmentStatus(segment);
  if (visualFilterForm.status !== 'ALL' && status !== visualFilterForm.status) return false;
  if (visualFilterForm.reportType !== 'ALL' && getAdhesive2SegmentReportType(segment) !== visualFilterForm.reportType) return false;
  return true;
}

function setAdhesive2FilterStatus(status: string) {
  visualFilterForm.status = status;
}

function setAdhesive2FilterReportType(reportType: string) {
  visualFilterForm.reportType = reportType;
}

function getAdhesive2SliceClass(segment: SourceSegment) {
  const status = getAdhesive2SegmentStatus(segment);
  const typeClass = getAdhesive2ReportTypeClass(getAdhesive2SegmentReportType(segment));
  const flags = [
    getAdhesive2SegmentCoaFlag(segment) ? 'is-coa' : '',
    getAdhesive2SegmentInspectionNg(segment) || getAdhesive2SegmentCoaNg(segment) ? 'is-inspection-ng' : '',
    getAdhesive2SegmentInspectionBackgroundClass(segment),
    getAdhesive2SegmentRuntimeMismatch(segment) ? 'is-runtime-mismatch' : '',
    transferPrintSelectionMode.value ? 'is-transfer-selectable' : '',
    transferPrintSelectionMode.value && !isPrintableTransferSegment(segment) ? 'is-transfer-disabled' : '',
    transferPrintSelectionMode.value && isTransferSegmentSelected(segment) ? 'is-transfer-selected' : '',
  ].filter(Boolean);
  if (status === 'COMPLETED') return ['slice-cell', 'is-confirmed', typeClass, ...flags];
  if (status === 'CURRENT_NG') return ['slice-cell', 'is-current-ng', typeClass, ...flags];
  if (status === 'SOURCE_NG') return ['slice-cell', 'is-source-ng', typeClass, ...flags];
  return ['slice-cell', 'is-scan-pending', typeClass, ...flags];
}

function normalizeRuntimeCompareText(value?: unknown) {
  return String(value ?? '')
    .trim()
    .toUpperCase();
}

function isRuntimeTextChanged(actual?: unknown, planned?: unknown) {
  const actualText = normalizeRuntimeCompareText(actual);
  const plannedText = normalizeRuntimeCompareText(planned);
  return !!actualText && !!plannedText && actualText !== plannedText;
}

function getAdhesive2SegmentRuntimeMeta(segment: SourceSegment) {
  const record = findReportBySegment(segment);
  if (!record) {
    return {
      actualGlueBoardModel: '',
      actualModelCode: '',
      changed: false,
      glueBoardModelChanged: false,
      modelChanged: false,
      plannedGlueBoardModel: currentPlan.planGlueBoardModel || currentPlan.glueBoardModel || '',
      plannedModelCode: currentPlan.modelCode || '',
    };
  }
  const extra = parseRecordExtra(record) as Record<string, any>;
  // 加工前自检异常仅由粘胶2发现，责任和产品快照均属于压槽；不能在工作台误显示为换型。
  if (isPreProcessSelfCheckAbnormal(extra)) {
    return {
      actualGlueBoardModel: '',
      actualModelCode: '',
      changed: false,
      glueBoardModelChanged: false,
      modelChanged: false,
      plannedGlueBoardModel: '',
      plannedModelCode: '',
    };
  }
  const actualModelCode = String(record.modelCode || extra.runtimeModelCode || '').trim();
  const plannedModelCode = String(extra.plannedModelCode || modelOverride.originalModelCode || currentPlan.modelCode || '').trim();
  const actualGlueBoardModel = String(record.glueBoardModel || extra.runtimeGlueBoardModel || extra.glueBoardModel || '').trim();
  const plannedGlueBoardModel = String(extra.plannedGlueBoardModel || modelOverride.originalGlueBoardModel || currentPlan.planGlueBoardModel || currentPlan.glueBoardModel || '').trim();
  const modelChanged = isRuntimeTextChanged(actualModelCode, plannedModelCode);
  const explicitlyNotChanged = extra.modelChanged === false || String(extra.modelChanged).toLowerCase() === 'false';
  const glueBoardModelChanged = !explicitlyNotChanged && isRuntimeTextChanged(actualGlueBoardModel, plannedGlueBoardModel);
  return {
    actualGlueBoardModel,
    actualModelCode,
    changed: modelChanged || glueBoardModelChanged,
    glueBoardModelChanged,
    modelChanged,
    plannedGlueBoardModel,
    plannedModelCode,
  };
}

function getAdhesive2SegmentRuntimeMismatch(segment: SourceSegment) {
  return getAdhesive2SegmentRuntimeMeta(segment).changed;
}

function getAdhesive2SegmentRuntimeText(segment: SourceSegment) {
  const meta = getAdhesive2SegmentRuntimeMeta(segment);
  if (!meta.changed) return '';
  const parts: string[] = [];
  if (meta.modelChanged && meta.actualModelCode) {
    parts.push(`型号 ${meta.plannedModelCode || '-'}->${meta.actualModelCode || '-'}`);
  }
  if (meta.glueBoardModelChanged && meta.actualGlueBoardModel) {
    parts.push(`胶板 ${meta.plannedGlueBoardModel || '-'}->${meta.actualGlueBoardModel || '-'}`);
  }

  return parts.join(' / ');
}

function getAdhesive2SegmentRuntimeTitle(segment: SourceSegment) {
  const meta = getAdhesive2SegmentRuntimeMeta(segment);
  if (!meta.changed) return segment.batchNo;
  return [
    `片号：${segment.batchNo}`,
    meta.modelChanged ? `型号：${meta.plannedModelCode || '-'} -> ${meta.actualModelCode || '-'}` : '',
    meta.glueBoardModelChanged ? `胶板：${meta.plannedGlueBoardModel || '-'} -> ${meta.actualGlueBoardModel || '-'}` : '',
  ]
    .filter(Boolean)
    .join('\n');
}

function getReportFormProductModelCode() {
  const record = activeRecord.value;
  const extra = parseRecordExtra(record || {}) as Record<string, any>;
  return String(record?.modelCode || extra.runtimeModelCode || currentProductModelCode.value || currentPlan.modelCode || '').trim();
}

function buildNowText() {
  return dayjs().format('YYYY-MM-DD HH:mm:ss');
}

function normalizeIntermediateRecordDateTime(value?: string) {
  const parsed = dayjs(value);
  return parsed.isValid() ? parsed.format('YYYY-MM-DD HH:mm:ss') : buildNowText();
}

function displayDateTimeText(value?: string) {
  if (!value) return '-';
  const parsed = dayjs(value);
  return parsed.isValid() ? parsed.format('YYYY-MM-DD HH:mm:ss') : String(value);
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
  if (['FINISHED', 'PAUSED', 'CANCELLED'].includes(normalized)) return '查看';
  return '开工';
}

function getEquipmentWorkStatusMeta(status?: string) {
  if (status === 'PRODUCING') return { color: 'processing', text: '生产中' };
  if (status === 'MAINTENANCE') return { color: 'warning', text: '检修' };
  if (status === 'FAULT') return { color: 'error', text: '故障' };
  return { color: 'default', text: '待机' };
}

function containsTaskFilterText(values: unknown[], keyword: string) {
  const text = String(keyword || '')
    .trim()
    .toLowerCase();
  if (!text) return true;
  return values.some((value) =>
    String(value ?? '')
      .toLowerCase()
      .includes(text),
  );
}

const taskListFilteredRows = computed(() => {
  const statusFilter = taskListFilters.status;
  return taskRows.value.filter((row) => {
    if (!isTaskMatchedBoardEquipment(row)) return false;
    const status = normalizeWorkOrderStatus(row?.status);
    if (statusFilter === 'UNFINISHED' && status === 'FINISHED') return false;
    if (statusFilter && statusFilter !== 'ALL' && statusFilter !== 'UNFINISHED' && status !== statusFilter) return false;
    if (!containsTaskFilterText([row?.planNo, row?.id], taskListFilters.planNo)) return false;
    if (!containsTaskFilterText([row?.motherModelCode, row?.modelCode, row?.materialCode, row?.motherMaterialCode], taskListFilters.modelCode)) {
      return false;
    }
    return containsTaskFilterText([row?.batchNo, row?.parentProductionBatchNo, row?.productionBatchNo, row?.sourceBatchNo, row?.sourceProductionBatchNo, resolveMotherBatchNo(row as Record<string, any>)], taskListFilters.batchNo);
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
  const rule = taskListColumnWidthRules[dataIndex] || {
    max: 220,
    min: 100,
    padding: 32,
  };
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

const taskListScrollX = computed(() => taskListColumns.value.reduce((total, column) => total + Number(column.width || 0), 0));

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
const isCurrentSegmentFinished = computed(() => isSourceGroupCompleted(activeSourceGroup.value));
const isCurrentTaskReadonly = computed(() => isWorkOrderFinished.value || isCurrentSegmentFinished.value || isWorkOrderPaused.value || isWorkOrderCancelled.value);
const hasLoadedGlueBoardPlan = computed(() => Boolean(currentPlan.planId && currentPlan.planOperationId && (currentPlan.equipmentId || String(currentPlan.equipmentCode || '').trim())));
const isGlueBoardConsumeDisabled = computed(() => isCurrentTaskReadonly.value);
const activeReportReadonlyReason = computed(() => {
  if (reportDialogMode.value !== 'view' || !activeRecord.value) return '';
  if (activeRecord.value.editBlockedReason) return activeRecord.value.editBlockedReason;
  if (String(activeRecord.value.reportStatus || '').toUpperCase() === 'SUBMITTED') {
    return '该粘胶2片已过站提交，只能查看，不能覆盖修改。';
  }
  if (isWorkOrderFinished.value || isCurrentSegmentFinished.value) return '当前粘胶2工序已完工，只能查看已有报工。';
  if (isWorkOrderPaused.value) return '当前粘胶2工序已暂停，只能查看已有报工。';
  if (isWorkOrderCancelled.value) return '当前粘胶2工序已取消，只能查看已有报工。';
  return '';
});
const selectedBoardEquipmentId = computed(() => boardEquipment.id ?? currentPlan.equipmentId);
const selectedBoardEquipmentCode = computed(() => boardEquipment.code || currentPlan.equipmentCode || '');
const selectedBoardEquipmentName = computed(() => boardEquipment.name || currentPlan.equipmentName || '');
const selectedBoardApplicablePadType = computed(() => normalizeAdhesive2PadTypeCode(boardEquipment.applicablePadType));
const selectedBoardWorkCenterName = computed(() => boardEquipment.workCenterName || currentPlan.workCenterName || '');
const adhesive2TaskListTitle = computed(() => `粘胶2待加工列表 - ${selectedBoardEquipmentCode.value || '未绑定'} / ${selectedBoardEquipmentName.value || '-'}`);
const currentProductModelCode = computed(() => (modelOverride.enabled && modelOverride.modelCode ? modelOverride.modelCode : currentPlan.modelCode));
const currentProductMaterialCode = computed(() => (modelOverride.enabled && modelOverride.materialCode ? modelOverride.materialCode : currentPlan.materialCode));
const currentProductSpecification = computed(() => (modelOverride.enabled && modelOverride.specification ? modelOverride.specification : currentPlan.planSizeSpec || currentPlan.actualSizeRule));
const isProductModelChanged = computed(() => modelOverride.enabled && !!modelOverride.modelCode && (modelOverride.modelCode !== modelOverride.originalModelCode || modelOverride.glueBoardModel !== modelOverride.originalGlueBoardModel));
const currentPlanModelText = computed(() => currentPlan.modelCode || '-');
const currentPlanGlueBoardText = computed(() => currentPlan.planGlueBoardModel || currentPlan.glueBoardModel || '-');
const currentRuntimeGlueBoardModelText = computed(() => (modelOverride.enabled && modelOverride.glueBoardModel ? modelOverride.glueBoardModel : currentPlan.glueBoardModel));
const currentChangeoverExecuteStatus = computed(() => String(currentChangeoverInstruction.value?.executeStatus || 'PENDING').toUpperCase());
const hasExecutingChangeoverInstruction = computed(() => currentChangeoverInstruction.value?.instructionType === 'CHANGEOVER' && currentChangeoverExecuteStatus.value === 'EXECUTING');
const hasPendingChangeoverInstruction = computed(
  () => currentChangeoverInstruction.value?.instructionType === 'CHANGEOVER' && ['PENDING', ''].includes(currentChangeoverExecuteStatus.value) && String(currentChangeoverInstruction.value?.status || '').toUpperCase() !== 'REVOKED',
);
const currentChangeoverProgressText = computed(() => {
  const instruction = currentChangeoverInstruction.value;
  if (!instruction?.id) return '';
  const completed = Number(instruction.completedQty || 0);
  const target = Number(instruction.targetQty || 0);
  return target > 0 ? `${completed}/${target} 片` : `${completed} 片`;
});
const currentChangeoverSummaryText = computed(() => {
  const instruction = currentChangeoverInstruction.value;
  if (!instruction?.id) return '';
  const modelText = `${instruction.beforeModelCode || currentPlan.modelCode || '-'} -> ${instruction.targetModelCode || '-'}`;
  const statusText = hasExecutingChangeoverInstruction.value ? '执行中' : '待执行';
  return `${instruction.instructionNo || '换型指令'}：型号 ${modelText}，${statusText} ${currentChangeoverProgressText.value}`;
});
const executionRequirementText = computed(() => latestPlanInstruction.value?.instructionContent?.trim() || currentPlan.requirements || '暂无执行要求');
const dailyPreparationReady = computed(() => hasReadyDailyPreparationRecord(dailyRecordRows.value));

function hasReadyDailyPreparationRecord(rows: DailyRecordRow[]) {
  return rows.some((row) => row.status !== 'PENDING');
}

function isCleaningMaintenanceDailyRecord(row: DailyRecordRow) {
  const detailText = row.details.map((item) => `${item.category || ''}${item.item || ''}${item.standard || ''}`).join('');
  const text = `${row.formCode || ''}${row.name || ''}${detailText}`.toUpperCase();
  return text.includes('CLEAN') || text.includes('MAINT') || text.includes('清洁') || text.includes('保养');
}

function hasReadyCleaningMaintenanceRecord(rows: DailyRecordRow[]) {
  return rows.some((row) => isCleaningMaintenanceDailyRecord(row) && row.status !== 'PENDING');
}

function showReadonlyTaskWarning(actionName = '继续操作') {
  if (isWorkOrderPaused.value) {
    message.warning(`当前粘胶2工序已暂停，请等待复工后再${actionName}`);
    return;
  }
  if (isWorkOrderCancelled.value) {
    message.warning(`当前粘胶2工序已作废取消，不能${actionName}`);
    return;
  }
  const subject = isWorkOrderFinished.value ? '当前粘胶2工单' : '当前粘胶2分段';
  message.warning(`${subject}已完工，仅可查看，不能${actionName}`);
}

function buildWorkOrderBlockedReason(status = currentPlan.status) {
  const normalized = normalizeWorkOrderStatus(status);
  if (normalized === 'PAUSED') {
    return `当前粘胶2工序已暂停，计划 ${currentPlan.planNo || '-'} 暂不能继续报工或提交操作，请等待生产计划复工指令。`;
  }
  if (normalized === 'CANCELLED') {
    return `当前粘胶2工序已作废取消，计划 ${currentPlan.planNo || '-'} 不能继续报工或提交操作。`;
  }
  return '';
}

function syncWorkOrderBlockedReason(status = currentPlan.status) {
  adhesive2WorkbenchBlockedReason.value = buildWorkOrderBlockedReason(status);
}

async function refreshInstructionUnreadCount() {
  const context = adhesive2InstructionContext.value;
  instructionUnreadCount.value = Number(
    (await getProductionInstructionUnreadCount({
      operationCode: context.operationCode,
      operationName: context.operationName,
      pageNo: 1,
      pageSize: 1,
      planId: context.planId,
      planNo: context.planNo || undefined,
      planOperationId: context.planOperationId,
      processCode: context.processCode,
      processName: context.processName,
    })) || 0,
  );
}

function handleInstructionUnreadChange(count: number) {
  instructionUnreadCount.value = Number(count || 0);
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

function normalizeSampleLockBatchNo(value?: string | null) {
  return String(value || '').trim();
}

function pushSampleLockCandidate(candidates: SampleLockCandidate[], seen: Set<string>, candidate: SampleLockCandidate) {
  if (!candidate.objectNo || !candidate.objectType || !candidate.sourceProcessCode) return;
  const key = `${candidate.sourceProcessCode}|${candidate.objectType}|${candidate.objectNo}|${candidate.qualificationObjectNo || ''}`;
  if (seen.has(key)) return;
  seen.add(key);
  candidates.push(candidate);
}

function getAdhesive2SourceSegmentBatchNo(segmentOrBatchNo?: SourceSegment | string) {
  return normalizeSampleLockBatchNo(typeof segmentOrBatchNo === 'string' ? segmentOrBatchNo : segmentOrBatchNo?.batchNo);
}

function getAdhesive2SampleLockMotherBatchNo(segmentBatchNo?: string, explicitMotherBatchNo?: string) {
  return normalizeSampleLockBatchNo(explicitMotherBatchNo || stripSegmentMark(segmentBatchNo) || currentPlan.batchNo || currentPlan.sourceBatchNo);
}

function getAdhesive2WetMotherLockObjectNos(batchNo?: string) {
  const normalized = normalizeSampleLockBatchNo(stripSegmentMark(batchNo) || batchNo);
  const objectNos = [normalized];
  if (/[PQRS]$/i.test(normalized)) {
    objectNos.push(normalized.slice(0, -1));
  }
  return objectNos.filter(Boolean);
}

function getAdhesive2SampleLockCandidates(segmentOrBatchNo?: SourceSegment | string, explicitMotherBatchNo?: string) {
  const segmentBatchNo = getAdhesive2SourceSegmentBatchNo(segmentOrBatchNo);
  const motherBatchNo = getAdhesive2SampleLockMotherBatchNo(segmentBatchNo, explicitMotherBatchNo);
  const candidates: SampleLockCandidate[] = [];
  const seen = new Set<string>();
  getAdhesive2WetMotherLockObjectNos(motherBatchNo).forEach((objectNo) => {
    pushSampleLockCandidate(candidates, seen, {
      objectLabel: '母卷',
      objectNo,
      objectType: 'MOTHER_ROLL',
      qualificationObjectNo: motherBatchNo,
      processName: '湿法',
      sourceProcessCode: 'WET',
    });
  });
  pushSampleLockCandidate(candidates, seen, {
    objectLabel: '分段',
    objectNo: motherBatchNo,
    objectType: 'SEGMENT',
    processName: '磨皮',
    sourceProcessCode: 'ROUGH_GRINDING',
  });
  pushSampleLockCandidate(candidates, seen, {
    objectLabel: '分段',
    objectNo: motherBatchNo,
    objectType: 'SEGMENT',
    processName: '粘胶1',
    sourceProcessCode: 'ADHESIVE1',
  });
  return candidates;
}

async function ensureAdhesive2SampleAbnormalUnlocked(segmentOrBatchNo?: SourceSegment | string, actionName = '报工', explicitMotherBatchNo?: string) {
  const candidates = getAdhesive2SampleLockCandidates(segmentOrBatchNo, explicitMotherBatchNo);
  for (const candidate of candidates) {
    if (isSampleLockDeferredToCutRound(candidate.sourceProcessCode)) continue;
    const lock = await getActiveSampleAbnormalLock(candidate);
    if (!lock) continue;
    const objectLabel = String(lock.objectType || '').toUpperCase() === 'MOTHER_ROLL' ? '母卷' : candidate.objectLabel;
    const processName = lock.sourceProcessName || candidate.processName;
    AModal.warning({
      content: lock.lockReason || `当前${objectLabel} ${lock.objectNo || candidate.objectNo} 因 ${lock.abnormalFeedbackTime || '-'}，${processName} 留样送检NG异常，锁定不允许继续${actionName}，等待复检确认后继续。`,
      title: '留样异常锁定',
    });
    return false;
  }
  return true;
}

function resolveMotherBatchNo(row: Record<string, any>) {
  const directCandidates = [row.sourceBatchNo, row.parentProductionBatchNo, row.motherBatchNo].map((value) => stripSegmentMark(value)).filter(Boolean);
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

function resolveAdhesive2SourceMotherBatchNo(source: MesHcAdhesiveConsoleApi.SourceItem | Record<string, any> | undefined, fallbackSliceBatchNo?: string) {
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
  return [row.sourceBatchNo, row.sourceMotherBatchNo, row.parentProductionBatchNo, row.motherBatchNo, row.sourceProductionBatchNo, row.productionBatchNo, row.batchNo].some((value) => isSameText(stripSegmentMark(value), target));
}

function findTaskByPlanAndMother(rows: Array<MesHcAdhesiveConsoleApi.TaskItem | Record<string, any>>, planNo: string, motherBatchNo?: string) {
  const planRows = rows.filter((row) => !planNo || isSameText(row.planNo, planNo));
  if (planNo && !planRows.length) return undefined;
  if (motherBatchNo) {
    const matched = planRows.find((row) => matchesTaskMotherBatch(row, motherBatchNo));
    if (matched) return matched;
    return undefined;
  }
  return planRows[0] || rows[0];
}

function markTaskListSourceGroupCompleted(group?: SourceGroup) {
  const motherBatchNo = group?.baseBatchNo;
  if (!motherBatchNo) return;
  taskRows.value = taskRows.value.map((row) => {
    if (!isSameText(row.planNo, currentPlan.planNo) || !matchesTaskMotherBatch(row, motherBatchNo)) {
      return row;
    }
    return {
      ...row,
      availableSourceLength: 0,
      status: 'COMPLETED',
    };
  });
}

function normalizeAdhesive2ScanBatchNo(value?: unknown) {
  return resolveTransferTicketQrBusinessNo(normalizeConfirmScanCode(String(value ?? '')))
    .trim()
    .toUpperCase();
}

function isReportMatchedByScanBatch(record: MesHcAdhesiveConsoleApi.ReportItem | Record<string, any>, batchNo?: string) {
  const target = normalizeAdhesive2ScanBatchNo(batchNo);
  if (!target) return false;
  return [record.sourceProductionBatchNo, record.productionBatchNo].some((value) => normalizeAdhesive2ScanBatchNo(value) === target);
}

async function findExistingAdhesive2ReportBySlice(rows: Array<MesHcAdhesiveConsoleApi.TaskItem | Record<string, any>>, planNo: string, sliceBatchNo?: string) {
  const target = normalizeAdhesive2ScanBatchNo(sliceBatchNo);
  if (!target) return null;
  const taskRows = rows.filter((row) => !planNo || isSameText(row.planNo, planNo));
  for (const task of taskRows) {
    const planOperationId = Number(task.planOperationId || 0);
    if (!planOperationId) continue;
    const reports = normalizeRows(await getAdhesiveConsoleReportList(planOperationId)) as MesHcAdhesiveConsoleApi.ReportItem[];
    const report = reports.find((item) => isReportMatchedByScanBatch(item, target));
    if (report) return { report, task };
  }
  return null;
}

function getSegmentMark(batchNo?: string) {
  const source = String(batchNo || '').trim();
  if (!source) return '';
  const mark = source.slice(-1);
  return ['P', 'Q', 'R', 'S'].includes(mark) ? mark : '';
}

function isGlueBoardMaterialItem(itemCategory: string, itemName: string) {
  const name = itemName.trim();
  return itemCategory.trim() === '胶板' && (name.includes('胶板型号') || name.includes('胶板料号'));
}

function isGlueBoardBatchItem(itemCategory: string, itemName: string) {
  return itemCategory.trim() === '胶板' && (itemName.trim().includes('胶板批号') || itemName.trim().includes('胶板批次'));
}

function getDefaultCheckActualValue(item: Pick<AdhesiveCheckItem, 'itemCategory' | 'itemName'>) {
  const itemCategory = item.itemCategory || '';
  const itemName = item.itemName || '';
  if (isGlueBoardMaterialItem(itemCategory, itemName)) {
    return getSelectedGlueBoardModel();
  }
  if (isGlueBoardBatchItem(itemCategory, itemName)) {
    return glueBoard.batchNo || '';
  }
  return '';
}

function applyGlueBoardDefaultsToCheckItems() {
  checkTemplate.value = checkTemplate.value.map((item) => {
    if (isGlueBoardMaterialItem(item.itemCategory || '', item.itemName || '')) {
      return { ...item, actualValue: getSelectedGlueBoardModel() };
    }
    if (isGlueBoardBatchItem(item.itemCategory || '', item.itemName || '')) {
      return { ...item, actualValue: glueBoard.batchNo || '' };
    }
    return item;
  });
}

function resolveAdhesive2TemplateModelCode() {
  const modelText = String(currentProductModelCode.value || currentPlan.modelCode || '')
    .trim()
    .toUpperCase();
  return modelText.match(/(?:W\d{2}P|C\d{2}P)\d{4}/)?.[0] || modelText;
}

function getAdhesive2ProcessCheckItems() {
  return checkTemplate.value
    .filter((item) => !['环境'].includes(item.itemCategory || ''))
    .map((item, index) => ({
      ...item,
      itemCategory: '工艺参数',
      sortNo: Number(item.sortNo || 100 + index),
      standardValue: item.standardValue || '/',
    }));
}

function ensureAdhesive2ChangeoverItems(items: AdhesiveCheckItem[]) {
  const rows = items.map((item) => ({
    ...item,
    itemCategory: item.itemCategory || '其他',
  }));
  if (!rows.some((item) => item.itemName.includes('湿度'))) {
    rows.splice(1, 0, {
      abnormalRemark: '',
      actualValue: '',
      checkResult: 'OK',
      itemCategory: '环境',
      itemName: '湿度',
      sortNo: 2,
      standardValue: '55±10%RH',
    });
  }
  return rows.map((item, index) => ({
    ...item,
    sortNo: item.sortNo || index + 1,
  }));
}

function normalizeCheckItem(item: any, index = 0): AdhesiveCheckItem {
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
  const normalizedStatus = status === 'CONFIRMED' || status === 'COMPLETED' ? 'COMPLETED' : status === 'RECORDED' || status === 'FILLED' || status === 'WAITING' ? 'FILLED' : 'PENDING';
  const details = Array.isArray(record?.details) && record.details.length > 0 ? record.details : Array.isArray(record?.presetDetails) ? record.presetDetails : [];
  return {
    canConfirm: record?.canConfirm ?? normalizedStatus === 'FILLED',
    canFill: record?.canFill ?? normalizedStatus !== 'COMPLETED',
    canView: record?.canView ?? true,
    confirmer: record?.confirmer || '-',
    confirmerTime: record?.confirmerTime || '-',
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
    formCode: record?.formCode || `ADHESIVE_DAILY_${index + 1}`,
    key: record?.formCode || record?.id || `adhesive-daily-${index + 1}`,
    name: record?.name || '粘胶工作准备记录',
    recordId: record?.recordId,
    recorder: record?.recorder || '-',
    recorderTime: record?.recorderTime || '-',
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
        {
          category: '开机点检',
          item: '开机确认',
          node: '加工前',
          remark: '',
          result: 'OK',
          seq: 1,
          standard: '按动态表单配置执行',
          value: '',
        },
      ],
      formCode: 'ADHESIVE2_STARTUP_CHECK_11_2_102',
      key: 'ADHESIVE2_STARTUP_CHECK_11_2_102',
      name: 'CMP软垫粘胶2开机点检表',
      recorder: '-',
      recorderTime: '-',
      result: '未填写',
      status: 'PENDING',
      timing: '加工前',
    },
    {
      canConfirm: false,
      canFill: true,
      canView: true,
      confirmer: '-',
      confirmerTime: '-',
      details: [
        {
          category: '设备清洁',
          item: '操作台',
          node: '加工前',
          remark: '',
          result: 'OK',
          seq: 1,
          standard: '表面整洁无脏污',
          value: '',
        },
        {
          category: '设备清洁',
          item: '放卷辊',
          node: '加工前',
          remark: '',
          result: 'OK',
          seq: 2,
          standard: '表面无脏污和异物',
          value: '',
        },
        {
          category: '设备清洁',
          item: '旋转丝杆',
          node: '加工前',
          remark: '',
          result: 'OK',
          seq: 3,
          standard: '表面无油污、脏污、异物',
          value: '',
        },
        {
          category: '设备清洁',
          item: '左右电机',
          node: '加工前',
          remark: '',
          result: 'OK',
          seq: 4,
          standard: '表面无油污、脏污、异物',
          value: '',
        },
        {
          category: '设备清洁',
          item: '镜面辊',
          node: '加工前',
          remark: '',
          result: 'OK',
          seq: 5,
          standard: '目视表面洁净且用无尘布擦拭无脏污',
          value: '',
        },
        {
          category: '设备清洁',
          item: '橡胶辊',
          node: '加工前',
          remark: '',
          result: 'OK',
          seq: 6,
          standard: '目视表面洁净且用无尘布擦拭无脏污',
          value: '',
        },
        {
          category: '设备清洁',
          item: '整机框架',
          node: '加工前',
          remark: '',
          result: 'OK',
          seq: 7,
          standard: '表面无油污、脏污',
          value: '',
        },
      ],
      formCode: 'ADHESIVE2_CLEANING_CHECK_11_2_103',
      key: 'ADHESIVE2_CLEANING_CHECK_11_2_103',
      name: 'CMP粘胶2设备清洁点检表',
      recorder: '-',
      recorderTime: '-',
      result: '未填写',
      status: 'PENDING',
      timing: '加工前',
    },
  ];
}

function sortAdhesive2DailyRecords(rows: DailyRecordRow[]) {
  const order = (row: DailyRecordRow) => {
    const text = `${row.formCode || ''}${row.name || ''}`.toUpperCase();
    if (text.includes('START') || text.includes('开机')) return 1;
    if (text.includes('CLEAN') || text.includes('清洁')) return 2;
    return 9;
  };
  return [...rows].sort((a, b) => order(a) - order(b) || String(a.name || '').localeCompare(String(b.name || ''), 'zh-Hans-CN'));
}

function getChangeoverPlanContextKey() {
  return [currentPlan.planId, currentPlan.planOperationId, normalizeAdhesive2MiddleBatchNo(currentPlan.batchNo || currentPlan.sourceBatchNo || '')].join('|');
}

function getCurrentChangeoverInstructionId() {
  return modelOverride.enabled ? modelOverride.changeoverInstructionId : undefined;
}

function matchesCurrentChangeoverInstruction(record: { changeoverInstructionId?: number }) {
  return Number(record.changeoverInstructionId || 0) === Number(getCurrentChangeoverInstructionId() || 0);
}

function resetModelOverride() {
  Object.assign(modelOverride, {
    changeoverLogId: undefined,
    changeoverInstructionId: undefined,
    changeoverInstructionNo: '',
    changeoverInstructionTargetQty: undefined,
    changeoverTime: '',
    changeoverUserName: '',
    enabled: false,
    glueBoardModel: '',
    materialCode: '',
    materialName: '',
    modelCode: '',
    originalGlueBoardModel: '',
    originalMaterialCode: '',
    originalModelCode: '',
    specification: '',
  });
}

async function loadLatestPlanInstruction() {
  if (!currentPlan.planOperationId) {
    latestPlanInstruction.value = null;
    return;
  }
  try {
    const rows = await getOperationInstructionList({
      includeConfirmed: true,
      operationCode: adhesive2InstructionContext.value.operationCode,
      operationName: adhesive2InstructionContext.value.operationName,
      planId: currentPlan.planId,
      planNo: currentPlan.planNo || undefined,
      planOperationId: currentPlan.planOperationId,
      processCode: adhesive2InstructionContext.value.processCode,
      processName: adhesive2InstructionContext.value.processName,
    });
    latestPlanInstruction.value = Array.isArray(rows) ? rows.find((row) => row.instructionType !== 'CHANGEOVER') || null : null;
  } catch (error) {
    console.warn('加载粘胶2计划最新指令失败', error);
    latestPlanInstruction.value = null;
  }
}

function applyRuntimeChangeoverInstruction(instruction?: MesHcProductionInstructionApi.Instruction | null) {
  if (!instruction?.id || instruction.instructionType !== 'CHANGEOVER') return false;
  if (!['EXECUTING', 'COMPLETED'].includes(String(instruction.executeStatus || '').toUpperCase())) return false;
  const plannedGlueBoardModel = currentPlan.planGlueBoardModel || currentPlan.glueBoardModel || getDefaultGlueBoardModel(currentPlan.modelCode || currentPlan.materialCode);
  const sameInstruction = getCurrentChangeoverInstructionId() === instruction.id;
  Object.assign(modelOverride, {
    changeoverLogId: undefined,
    changeoverInstructionId: instruction.id,
    changeoverInstructionNo: instruction.instructionNo || '',
    changeoverInstructionTargetQty: instruction.targetQty,
    changeoverTime: instruction.executeStartTime || '',
    changeoverUserName: instruction.executeUserName || '',
    enabled: true,
    glueBoardModel: sameInstruction ? modelOverride.glueBoardModel : '',
    materialCode: instruction.targetMaterialCode || '',
    materialName: '',
    modelCode: instruction.targetModelCode || currentPlan.modelCode || '',
    originalGlueBoardModel: plannedGlueBoardModel || '',
    originalMaterialCode: instruction.beforeMaterialCode || currentPlan.materialCode || '',
    originalModelCode: instruction.beforeModelCode || currentPlan.modelCode || '',
    specification: '',
  });
  return true;
}

async function loadRuntimeProductSnapshot() {
  if (!currentPlan.planId || !currentPlan.planOperationId) return;
  const loadSeq = ++runtimeProductSnapshotLoadSeq;
  const contextKey = getChangeoverPlanContextKey();
  const instructionId = getCurrentChangeoverInstructionId();
  try {
    const snapshot = await getAdhesive2RuntimeProductSnapshot({
      planId: currentPlan.planId,
      planOperationId: currentPlan.planOperationId,
      segmentBatchNo: currentPlan.batchNo || currentPlan.sourceBatchNo || undefined,
    });
    if (loadSeq !== runtimeProductSnapshotLoadSeq || contextKey !== getChangeoverPlanContextKey() || instructionId !== getCurrentChangeoverInstructionId()) return;
    if (!modelOverride.enabled || !snapshot) return;
    const executingInstruction = hasExecutingChangeoverInstruction.value ? currentChangeoverInstruction.value : null;
    const unexpectedModel = executingInstruction?.id === instructionId && Boolean(executingInstruction?.targetModelCode)
      && String(snapshot.productModel || '').trim() !== String(executingInstruction?.targetModelCode || '').trim();
    const unexpectedMaterial = executingInstruction?.id === instructionId && Boolean(executingInstruction?.targetMaterialCode)
      && String(snapshot.materialCode || '').trim() !== String(executingInstruction?.targetMaterialCode || '').trim();
    if (Number(snapshot.changeoverInstructionId || 0) !== Number(instructionId || 0) || snapshot.sourceType === 'PLAN' || unexpectedModel || unexpectedMaterial) {
      message.warning('运行产品快照与当前换型指令不一致，已保留当前指令目标型号，请刷新工作台后重试');
      return;
    }
    Object.assign(modelOverride, {
      changeoverInstructionId: snapshot.changeoverInstructionId || modelOverride.changeoverInstructionId,
      changeoverInstructionNo: snapshot.changeoverInstructionNo || modelOverride.changeoverInstructionNo,
      materialCode: snapshot.materialCode || modelOverride.materialCode,
      materialName: snapshot.materialName || '',
      modelCode: snapshot.productModel || modelOverride.modelCode,
      specification: snapshot.specification || '',
    });
  } catch (error) {
    console.warn('加载粘胶2实际产品快照失败', error);
  }
}

function sortRuntimeChangeoverInstructions(rows: MesHcProductionInstructionApi.Instruction[]) {
  const startTime = (row: MesHcProductionInstructionApi.Instruction) => {
    const value = row.executeStartTime ? dayjs(row.executeStartTime).valueOf() : 0;
    return Number.isFinite(value) ? value : 0;
  };
  return [...rows].sort((a, b) => startTime(b) - startTime(a) || Number(b.id || 0) - Number(a.id || 0));
}

async function loadChangeoverInstruction() {
  const loadSeq = ++changeoverInstructionLoadSeq;
  const contextKey = getChangeoverPlanContextKey();
  const previousRuntimeKey = `${getCurrentChangeoverInstructionId() || ''}|${currentProductModelCode.value}`;
  if (!currentPlan.planOperationId) {
    currentChangeoverInstruction.value = null;
    resetModelOverride();
    return null;
  }
  try {
    const rows = await getOperationInstructionList({
      includeConfirmed: true,
      instructionType: 'CHANGEOVER',
      operationCode: adhesive2InstructionContext.value.operationCode,
      operationName: adhesive2InstructionContext.value.operationName,
      planId: currentPlan.planId,
      planNo: currentPlan.planNo || undefined,
      planOperationId: currentPlan.planOperationId,
      processCode: adhesive2InstructionContext.value.processCode,
      processName: adhesive2InstructionContext.value.processName,
      segmentBatchNo: currentPlan.batchNo || currentPlan.sourceBatchNo || undefined,
    });
    if (loadSeq !== changeoverInstructionLoadSeq || contextKey !== getChangeoverPlanContextKey()) return null;
    const availableRows = sortRuntimeChangeoverInstructions(Array.isArray(rows) ? rows.filter((row) => String(row.status || '').toUpperCase() !== 'REVOKED') : []);
    const executing = availableRows.find((row) => String(row.executeStatus || '').toUpperCase() === 'EXECUTING');
    const pending = availableRows.find((row) => ['PENDING', ''].includes(String(row.executeStatus || '').toUpperCase()));
    const completed = availableRows.find((row) => String(row.executeStatus || '').toUpperCase() === 'COMPLETED');
    const current = executing || pending || null;
    const effectiveRuntimeInstruction = executing || completed;
    currentChangeoverInstruction.value = current;
    if (effectiveRuntimeInstruction) {
      // 换型完成后，当前生产仍应使用本次换型后的实际产品，不能回退为计划型号。
      applyRuntimeChangeoverInstruction(effectiveRuntimeInstruction);
      await loadRuntimeProductSnapshot();
    } else {
      // 切换到没有有效换型记录的分段时，不能沿用上一分段的实际产品。
      resetModelOverride();
    }
    if (loadSeq !== changeoverInstructionLoadSeq || contextKey !== getChangeoverPlanContextKey()) return null;
    if (previousRuntimeKey !== `${getCurrentChangeoverInstructionId() || ''}|${currentProductModelCode.value}`) {
      await refreshChangeoverInspectionContext();
    }
    return current;
  } catch (error) {
    console.warn('加载粘胶2换型指令失败', error);
    return undefined;
  }
}

function resetPlan() {
  const selectedEquipment = {
    equipmentCode: boardEquipment.code,
    equipmentId: boardEquipment.id,
    equipmentName: boardEquipment.name,
    workCenterId: boardEquipment.workCenterId,
    workCenterName: boardEquipment.workCenterName,
  };
  Object.assign(currentPlan, {
    actualSizeRule: '',
    availableSourceLength: 0,
    batchNo: '',
    endTime: '',
    equipmentCode: selectedEquipment.equipmentCode,
    equipmentId: selectedEquipment.equipmentId,
    equipmentName: selectedEquipment.equipmentName,
    glueBoardModel: '',
    materialCode: '',
    modelCode: '',
    planGlueBoardModel: '',
    planId: undefined,
    planNo: '',
    planOperationId: undefined,
    planSizeSpec: '',
    requirements: '',
    sourceBatchNo: '',
    sourceProductionBatchNo: '',
    startTime: '',
    status: '',
    workCenterId: selectedEquipment.workCenterId,
    workCenterName: selectedEquipment.workCenterName,
  });
  resetModelOverride();
  latestPlanInstruction.value = null;
  currentChangeoverInstruction.value = null;
  reportRecords.value = [];
  coaReportRecords.value = [];
  selectedReportRecordKeys.value = [];
  sourceGroups.value = [];
  adhesive2IntermediateRecords.value = [];
  selectedIntermediateRecord.value = null;
  intermediateDetailVisible.value = false;
  adhesive2WorkbenchBlockedReason.value = '';
  changeoverInspections.value = [];
  latestChangeoverInspection.value = null;
  glueBoardMapCandidates.value = [];
  selectedCoaSliceNo.value = '';
  coaInspectionVisible.value = false;
  coaFaiRows.value = [];
  processCheckFaiRows.value = [];
  applyFaiSummary(null);
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
    returnedLength: 0,
    returnedStartPosition: 0,
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
  return normalizeSizeRule(task.actualSizeRule || task.actualSizeSpec || task.actualSizeName || task.sizeRule || resolvePlanSizeSpec(task)) || '775mm';
}

function normalizeAdhesive2ProductionBatchNo(batchNo?: string, _sizeRule?: string) {
  const source = String(batchNo || '').trim();
  if (!source) return '';
  const base = stripAdhesive2RuntimeBatchSuffix(source)
    .replace(/-J\d+$/i, '')
    .toUpperCase();
  return base;
}

function stripAdhesive2RuntimeBatchSuffix(batchNo?: string) {
  return String(batchNo || '')
    .trim()
    .replace(/-M[A-Z0-9]+(?:-G[A-Z0-9]+)?$/i, '');
}

function applyBoardEquipment(equipment: Partial<BoardEquipment>) {
  boardEquipment.id = equipment.id;
  boardEquipment.applicablePadType = equipment.applicablePadType || '';
  boardEquipment.applicablePadTypeName = equipment.applicablePadTypeName || '';
  boardEquipment.code = equipment.code || '';
  boardEquipment.name = equipment.name || '';
  boardEquipment.workCenterId = equipment.workCenterId;
  boardEquipment.workCenterName = equipment.workCenterName || '';
  boardEquipment.workStatus = equipment.workStatus || '';
}

function applyBoardEquipmentFromTask(task: any, manual = false) {
  const equipmentId = task?.equipmentId;
  const equipmentCode = String(task?.equipmentCode || '').trim();
  if (!equipmentId && !equipmentCode) return;
  if (manual) {
    boardEquipmentManualSelected.value = true;
  }
  const taskPadType = resolveAdhesive2TaskPadType(task);
  applyBoardEquipment({
    applicablePadType: taskPadType,
    applicablePadTypeName: taskPadType === 'BLACK_PAD' ? '黑垫' : taskPadType === 'WHITE_PAD' ? '白垫' : '',
    code: equipmentCode,
    id: equipmentId,
    name: task?.equipmentName || '',
    workCenterId: task?.workCenterId,
    workCenterName: task?.workCenterName || '',
    workStatus: task?.workStatus || '',
  });
}

function getEffectiveBoardEquipment(task?: any): BoardEquipment {
  return {
    applicablePadType: boardEquipment.applicablePadType || resolveAdhesive2TaskPadType(task),
    applicablePadTypeName: boardEquipment.applicablePadTypeName || '',
    code: boardEquipment.code || task?.equipmentCode || currentPlan.equipmentCode || '',
    id: boardEquipment.id ?? task?.equipmentId ?? currentPlan.equipmentId,
    name: boardEquipment.name || task?.equipmentName || currentPlan.equipmentName || '',
    workCenterId: boardEquipment.workCenterId ?? task?.workCenterId ?? currentPlan.workCenterId,
    workCenterName: boardEquipment.workCenterName || task?.workCenterName || currentPlan.workCenterName || '',
    workStatus: boardEquipment.workStatus || task?.workStatus || '',
  };
}

function syncCurrentPlanEquipmentFromBoard(context: Partial<MesHcAdhesiveConsoleApi.TaskItem> | Record<string, any> = {}) {
  const equipment = getEffectiveBoardEquipment(context);
  currentPlan.equipmentId = equipment.id;
  currentPlan.equipmentCode = equipment.code;
  currentPlan.equipmentName = equipment.name;
  currentPlan.workCenterId = equipment.workCenterId;
  currentPlan.workCenterName = equipment.workCenterName || currentPlan.workCenterName;
}

function ensureBoardEquipmentFromTasks(rows = taskRows.value) {
  if (boardEquipmentManualSelected.value || selectedBoardEquipmentId.value || selectedBoardEquipmentCode.value) return;
  const workCenterCandidate = rows.find((row: any) => row.workCenterId || row.workCenterName);
  if (workCenterCandidate) {
    boardEquipment.workCenterId = workCenterCandidate.workCenterId;
    boardEquipment.workCenterName = workCenterCandidate.workCenterName || '';
  }
  const matched = rows.find((row: any) => row.equipmentId || row.equipmentCode);
  if (matched) {
    applyBoardEquipmentFromTask(matched);
  }
}

function isSameTaskEquipment(task: any, equipment: BoardEquipment) {
  const taskEquipmentId = task.equipmentId;
  const taskEquipmentCode = String(task.equipmentCode || '').trim();
  if (!equipment.id && !equipment.code) return true;
  if (equipment.id && taskEquipmentId) return Number(taskEquipmentId) === Number(equipment.id);
  if (equipment.code && taskEquipmentCode) return taskEquipmentCode === equipment.code;
  return false;
}

function normalizeAdhesive2PadTypeCode(value?: string) {
  return String(value || '').trim().toUpperCase();
}

function resolveAdhesive2TaskPadType(task: MesHcAdhesiveConsoleApi.TaskItem | Record<string, any>) {
  const categoryCode = normalizeAdhesive2PadTypeCode(task?.categoryCode);
  if (['BLACK_PAD', 'WHITE_PAD'].includes(categoryCode)) return categoryCode;
  const categoryName = String(task?.categoryName || '').trim().toUpperCase();
  if (categoryName.includes('黑') || categoryName.includes('BLACK')) return 'BLACK_PAD';
  if (categoryName.includes('白') || categoryName.includes('软') || categoryName.includes('WHITE') || categoryName.includes('SOFT')) return 'WHITE_PAD';
  const modelCode = String(task?.modelCode || '').trim().toUpperCase();
  if (modelCode.startsWith('HCR') || modelCode.startsWith('HCSP') || modelCode.includes('BLACK') || modelCode.includes('黑')) return 'BLACK_PAD';
  if (modelCode.startsWith('W') || modelCode.includes('WHITE') || modelCode.includes('SOFT') || modelCode.includes('白') || modelCode.includes('软')) return 'WHITE_PAD';
  return '';
}

function isAdhesive2TaskPadTypeCompatibleWithBoard(task: MesHcAdhesiveConsoleApi.TaskItem | Record<string, any>) {
  const equipmentPadType = selectedBoardApplicablePadType.value;
  if (!equipmentPadType) return false;
  if (equipmentPadType === 'COMMON') return true;
  return equipmentPadType === resolveAdhesive2TaskPadType(task);
}

function isTaskMatchedBoardEquipment(task: MesHcAdhesiveConsoleApi.TaskItem | Record<string, any>) {
  const selectedId = selectedBoardEquipmentId.value;
  const selectedCode = String(selectedBoardEquipmentCode.value || '').trim();
  if (!selectedId && !selectedCode) return true;
  if (!isAdhesive2TaskPadTypeCompatibleWithBoard(task)) return false;
  const taskEquipmentId = task?.equipmentId;
  const taskEquipmentCode = String(task?.equipmentCode || '').trim();
  if (selectedId && taskEquipmentId) return Number(selectedId) === Number(taskEquipmentId);
  if (selectedCode && taskEquipmentCode) return selectedCode === taskEquipmentCode;
  return !taskEquipmentId && !taskEquipmentCode;
}

async function ensureAdhesive2TaskEquipmentForBoard(
  task: MesHcAdhesiveConsoleApi.TaskItem | Record<string, any>,
) {
  const hasSelectedEquipment = Boolean(selectedBoardEquipmentId.value || selectedBoardEquipmentCode.value);
  if (hasSelectedEquipment) {
    if (!isAdhesive2TaskPadTypeCompatibleWithBoard(task)) {
      const taskPadTypeCode = resolveAdhesive2TaskPadType(task);
      const taskPadType = taskPadTypeCode === 'BLACK_PAD' ? '黑垫' : taskPadTypeCode === 'WHITE_PAD' ? '白垫' : '未配置垫型';
      message.warning(`计划 ${task.planNo || '-'} 为${taskPadType}，与当前粘胶2设备适用垫型不一致`);
      return false;
    }
    if (!isTaskMatchedBoardEquipment(task) && (task.equipmentId || task.equipmentCode)) {
      message.warning(`计划 ${task.planNo || '-'} 已绑定其他粘胶2设备，不能在当前设备看板直接加载`);
      return false;
    }
    const equipment = getEffectiveBoardEquipment(task);
    task.equipmentId = equipment.id;
    task.equipmentCode = equipment.code;
    task.equipmentName = equipment.name;
    return true;
  }
  if (task.equipmentId || task.equipmentCode) {
    applyBoardEquipmentFromTask(task);
    return true;
  }
  message.warning('当前粘胶2工序未绑定设备，请先选择粘胶2机台');
  await openEquipmentSelect();
  return false;
}

function getDailyRecordContext() {
  if (currentPlan.planId && currentPlan.planOperationId) {
    return {
      equipmentCode: selectedBoardEquipmentCode.value || currentPlan.equipmentCode,
      equipmentId: selectedBoardEquipmentId.value,
      equipmentName: selectedBoardEquipmentName.value || currentPlan.equipmentName,
      planId: currentPlan.planId,
      planOperationId: currentPlan.planOperationId,
      workCenterId: currentPlan.workCenterId,
      workCenterName: currentPlan.workCenterName,
    };
  }
  const equipment = getEffectiveBoardEquipment();
  const matched = taskRows.value.find(
    (task: any) => task?.planId && task?.planOperationId && normalizeWorkOrderStatus(task.status) !== 'FINISHED' && isSameTaskEquipment(task, equipment),
  );
  if (matched) return matched;
  if (!equipment.id && !equipment.code) {
    return taskRows.value.find((task: any) => task?.planId && task?.planOperationId && normalizeWorkOrderStatus(task.status) !== 'FINISHED');
  }
  return undefined;
}

function applyTask(task: any) {
  const previousContextKey = getChangeoverPlanContextKey();
  const planOperationChanged = currentPlan.planOperationId !== task.planOperationId;
  const previousEquipmentId = currentPlan.equipmentId;
  const previousEquipmentCode = currentPlan.equipmentCode;
  const sourceProductionBatchNo = task.sourceProductionBatchNo || '';
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
  currentPlan.equipmentCode = task.equipmentCode || '';
  currentPlan.equipmentId = task.equipmentId;
  currentPlan.equipmentName = task.equipmentName || '';
  const planGlueBoardModel = task.glueBoardModel || getDefaultGlueBoardModel(task.modelCode || task.materialCode);
  currentPlan.glueBoardModel = planGlueBoardModel;
  currentPlan.materialCode = task.materialCode || '';
  currentPlan.modelCode = task.modelCode || '';
  currentPlan.planGlueBoardModel = planGlueBoardModel;
  currentPlan.planId = task.planId;
  currentPlan.planNo = task.planNo || '';
  currentPlan.planOperationId = task.planOperationId;
  currentPlan.planSizeSpec = planSizeSpec;
  currentPlan.requirements = task.requirements || task.executeRequirement || '';
  currentPlan.sourceBatchNo = motherBatchNo;
  currentPlan.sourceProductionBatchNo = sourceProductionBatchNo;
  currentPlan.startTime = task.startTime || '';
  currentPlan.status = task.status || '';
  syncWorkOrderBlockedReason(task.status);
  currentPlan.workCenterId = task.workCenterId;
  currentPlan.workCenterName = task.workCenterName || '';
  if (previousContextKey !== getChangeoverPlanContextKey()) {
    ++changeoverInstructionLoadSeq;
    ++runtimeProductSnapshotLoadSeq;
    ++changeoverInspectionsLoadSeq;
    currentChangeoverInstruction.value = null;
    changeoverInspections.value = [];
    latestChangeoverInspection.value = null;
    changeoverVisible.value = false;
    resetModelOverride();
  }
  if (!boardEquipmentManualSelected.value) {
    applyBoardEquipmentFromTask(task);
  }
  syncCurrentPlanEquipmentFromBoard(task);
  if (planOperationChanged && !isSameEquipmentIdentity(previousEquipmentId, previousEquipmentCode, currentPlan.equipmentId, currentPlan.equipmentCode)) {
    clearCurrentGlueBoardUsage(currentPlan.glueBoardModel);
  }
  sourceGroups.value = [];
}

function getSegmentLabel(segmentMark?: string) {
  return segmentMark ? `${segmentMark}段` : '不分段';
}

function buildSourceGroupsFromSources(sources: MesHcAdhesiveConsoleApi.SourceItem[]) {
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
    const reportRanges = getReportRanges(batchNo);
    const usedLength = getReportUsedLength(batchNo);
    const sourceNgText = getSourceProcessNgText(source as unknown as Record<string, any>, '压槽');
    const sourceCoaSubmitted = isSourceCoaSubmitted(source as unknown as Record<string, any>);
    const sourceReports = getReportRecordsBySourceBatchNo(batchNo);
    const segmentCompleted = sourceReports.length > 0 && sourceReports.every((report) => isAdhesiveRecordSubmitted(report.reportStatus));
    const availableLength = sourceNgText || segmentCompleted || sourceCoaSubmitted ? 0 : getAvailableLengthByRanges(outputLength, reportRanges);
    const segment: SourceSegment = {
      actualSizeRule: normalizeSizeRule(source.actualSizeRule),
      actualSizeSuffix: String(source.actualSizeSuffix || '')
        .trim()
        .toUpperCase(),
      availableLength,
      batchNo,
      coaFlag: source.coaFlag,
      confirmTime: source.confirmTime || '',
      defectCode: source.defectCode || '',
      extraJson: source.extraJson || '',
      grindingSecondDetailId: source.grindingSecondDetailId,
      label: getSegmentLabel(source.segmentMark),
      outputLength,
      qtime: source.qtime,
      reportRanges,
      segmentMark: source.segmentMark || '',
      selfCheck: source.selfCheck || '',
      sourceMotherBatchNo: baseBatchNo,
      sourceNgProcessName: sourceNgText ? resolveNgProcessName(source as unknown as Record<string, any>, '压槽') : '',
      sourceNgReason: sourceNgText ? getNgReasonText(source as unknown as Record<string, any>) : '',
      sourceNgText,
      sourceProcessStage: source.processStage || '',
      sourceMenuCode: source.sourceMenuCode || '',
      sourceModelCode: source.modelCode || '',
      sourcePlanId: source.grindingPlanId,
      sourcePlanOperationId: source.grindingPlanOperationId,
      usedLength,
    };
    const existed = groups.get(baseBatchNo);
    if (existed) {
      existed.segments.push(segment);
      existed.qtime = mergeAdhesive2BatchQtime(existed.qtime, segment.qtime);
      existed.totalOutputLength += outputLength;
      existed.totalUsedLength += usedLength;
      existed.totalAvailableLength += getAdhesive2SegmentAvailableLength(segment);
      return;
    }
    groups.set(baseBatchNo, {
      baseBatchNo,
      materialCode: currentProductMaterialCode.value,
      modelCode: currentProductModelCode.value,
      planNo: currentPlan.planNo,
      qtime: segment.qtime,
      segments: [segment],
      totalAvailableLength: getAdhesive2SegmentAvailableLength(segment),
      totalOutputLength: outputLength,
      totalUsedLength: usedLength,
    });
  });
  sourceGroups.value = Array.from(groups.values()).map((group) => ({
    ...group,
    segments: group.segments.sort((a, b) => compareAdhesive2CardNo(a.batchNo, b.batchNo)),
  }));
}

function getReportUsedLength(sourceProductionBatchNo: string) {
  return reportRecords.value
    .filter((item) => String(item.sourceProductionBatchNo || '') === sourceProductionBatchNo)
    .filter(isAdhesive2ReportDeductible)
    .reduce((sum, item) => sum + Number(item.inputLength || item.outputLength || 0), 0);
}

async function loadCheckTemplate() {
  const contextKey = getChangeoverPlanContextKey();
  const modelCode = resolveAdhesive2TemplateModelCode();
  if (!modelCode) {
    checkTemplate.value = [];
    return;
  }
  const rows = normalizeRows(await getAdhesiveConsoleCheckTemplate(modelCode));
  if (contextKey !== getChangeoverPlanContextKey() || modelCode !== resolveAdhesive2TemplateModelCode()) return;
  checkTemplate.value = rows.map(normalizeCheckItem);
}

async function loadDailyRecords() {
  const fallbackRows = createDailyRecordFallbackRows();
  const context = getDailyRecordContext() as Record<string, any> | undefined;
  if (!context?.planId || !context?.planOperationId) {
    dailyRecordRows.value = dailyRecordRows.value.length > 0 ? dailyRecordRows.value : fallbackRows;
    return;
  }
  const rows = normalizeRows(
    await getAdhesiveConsolePassWorkList(context.planId, context.planOperationId, {
      equipmentId: context.equipmentId,
      recordDate: dayjs().format('YYYY-MM-DD'),
    }),
  );
  const backendRows = rows.map(mapPassWorkRecord).filter((item) => item.name.includes('开机') || item.name.includes('清洁') || item.formCode.includes('START') || item.formCode.includes('CLEAN'));
  dailyRecordRows.value = sortAdhesive2DailyRecords(backendRows.length > 0 ? backendRows : fallbackRows);
}

async function loadDailyRecordsForStart(task: MesHcAdhesiveConsoleApi.TaskItem | Record<string, any>, equipment: BoardEquipment) {
  const fallbackRows = createDailyRecordFallbackRows();
  if (!task?.planId || !task?.planOperationId) return fallbackRows;
  const rows = normalizeRows(
    await getAdhesiveConsolePassWorkList(Number(task.planId), Number(task.planOperationId), {
      equipmentId: equipment.id,
      recordDate: dayjs().format('YYYY-MM-DD'),
    }),
  );
  const backendRows = rows
    .map(mapPassWorkRecord)
    .filter((item) => item.name.includes('开机') || item.name.includes('清洁') || item.name.includes('保养') || item.formCode.includes('START') || item.formCode.includes('CLEAN') || item.formCode.includes('MAINT'));
  return sortAdhesive2DailyRecords(backendRows.length > 0 ? backendRows : fallbackRows);
}

async function loadReports() {
  if (!currentPlan.planOperationId) return;
  const rows = normalizeRows(
    await getAdhesiveConsoleReportList(currentPlan.planOperationId, {
      scanConfirmDate: reportScanConfirmDate.value || undefined,
    }),
  );
  reportRecords.value = rows;
  syncIntermediateCountersFromReports();
  selectedReportRecordKeys.value = selectedReportRecordKeys.value.filter((key) => rows.some((record: any) => record.id === key));
}

function formatReportScanConfirmDate(record?: Partial<MesHcAdhesiveConsoleApi.ReportItem> | Record<string, any>) {
  const value = String((record as Record<string, any> | undefined)?.confirmerTime || '');
  if (!value) return '-';
  const parsed = dayjs(value);
  if (parsed.isValid()) return parsed.format('YYYY-MM-DD');
  return value.length >= 10 ? value.slice(0, 10) : value;
}

async function fetchGlueBoardMapCandidatesForProductModel(productModelCode?: string) {
  const modelCode = String(productModelCode || '').trim();
  if (!modelCode) return [];
  return normalizeRows(
    await getMatchedFinishedGlueBoardMapItems({
      glueProcess: 'ADHESIVE2',
      productModelCode: modelCode,
    }),
  ) as MesHcFinishedGlueBoardMapApi.FinishedGlueBoardMapItem[];
}

async function loadGlueBoardMapCandidates(options: { warnMismatch?: boolean } = {}) {
  const productModelCode = String(currentProductModelCode.value || '').trim();
  if (!productModelCode) {
    glueBoardMapCandidates.value = [];
    return;
  }
  glueBoardMapLoading.value = true;
  try {
    glueBoardMapCandidates.value = await fetchGlueBoardMapCandidatesForProductModel(productModelCode);
    if (options.warnMismatch) {
      const tip = glueBoardMapMismatchTip.value;
      if (tip) {
        glueBoard.alarm = tip;
      }
    }
  } finally {
    glueBoardMapLoading.value = false;
  }
}

async function loadChangeoverInspections() {
  const loadSeq = ++changeoverInspectionsLoadSeq;
  const contextKey = getChangeoverPlanContextKey();
  if (!currentPlan.planOperationId) {
    changeoverInspections.value = [];
    latestChangeoverInspection.value = null;
    return;
  }
  const motherBatchNo = currentMotherBatchNo.value || currentPlan.sourceBatchNo || currentPlan.batchNo || '';
  const records = normalizeRows(
    await getAdhesive2ProcessParamList(currentPlan.planOperationId, {
      formType: 'PRODUCTION_CHECK',
      motherBatchNo,
    }),
  );
  if (loadSeq !== changeoverInspectionsLoadSeq || contextKey !== getChangeoverPlanContextKey()) return;
  changeoverInspections.value = records.map(mapProcessParamRecordToChangeover);
  latestChangeoverInspection.value = changeoverInspections.value[0] || null;
}

function mapProcessParamRecordToChangeover(record: MesHcAdhesiveConsoleApi.ProcessParamRecord): MesHcAdhesiveConsoleApi.ChangeoverInspection {
  const checkItems = normalizeRows(record.items).map((item, index) => ({
    abnormalRemark: item.abnormalRemark || '',
    actualValue: item.actualValue || '',
    checkResult: item.checkResult || 'OK',
    itemCategory: item.itemCategory || '其他',
    itemName: item.itemName || '',
    sortNo: item.sortNo || item.seq || index + 1,
    standardValue: item.standardValue || '',
  }));
  const inspectionStatus = checkItems.some((item) => String(item.checkResult || '').toUpperCase() === 'NG') ? 'ABNORMAL' : 'QUALIFIED';
  const result: MesHcAdhesiveConsoleApi.ChangeoverInspection = {
    changeoverInstructionId: record.changeoverInstructionId,
    checkItems,
    currentPlanNo: record.planNo || currentPlan.planNo,
    detailItemsJson: JSON.stringify(checkItems),
    extraJson: JSON.stringify({ importAttachment: record.importAttachment }),
    id: record.id,
    inspectionStatus,
    motherSegmentBatchNo: record.motherBatchNo || record.parentProductionBatchNo || '',
    planId: record.planId || currentPlan.planId,
    planNo: record.planNo || currentPlan.planNo,
    planOperationId: record.planOperationId || currentPlan.planOperationId,
    productionMaterialCode: record.materialCode || currentProductMaterialCode.value || '',
    productionModelCode: record.modelCode || currentProductModelCode.value || '',
    recordTime: record.fillTime || record.reportDate || '',
    recorderName: record.fillUserName || record.recorderName || '',
    remark: record.remark || '',
    submitTime: record.fillTime || record.reportDate || '',
  };
  (result as Record<string, any>).importAttachment = record.importAttachment;
  (result as Record<string, any>).recordStatus = record.recordStatus || record.reportStatus;
  (result as Record<string, any>).confirmTime = record.confirmTime || '';
  (result as Record<string, any>).confirmUserName = record.confirmUserName || '';
  return result;
}

function applyFaiSummary(summary?: MesHcAdhesiveConsoleApi.FaiSummary | null) {
  firstInspection.value = {
    allowReportSubmit: Boolean(summary?.allowReportSubmit),
    displayText: summary?.displayText || resolveFirstInspectionDisplayText(summary?.faiStatus, summary?.faiJudgment),
    faiApplyTime: summary?.faiApplyTime || '',
    faiId: summary?.faiId,
    faiJudgment: summary?.faiJudgment || 'PENDING',
    faiNo: summary?.faiNo || '',
    faiRejectReason: summary?.faiRejectReason || '',
    faiReturnTime: summary?.faiReturnTime || '',
    faiStandardNo: summary?.faiStandardNo || '',
    faiStandardVersion: summary?.faiStandardVersion || '',
    faiStatus: summary?.faiStatus || 'PENDING',
    glueBoardBatchNo: summary?.glueBoardBatchNo || '',
    glueBoardMaterialCode: summary?.glueBoardMaterialCode || '',
    glueBoardUsageId: summary?.glueBoardUsageId,
    sampleLength: summary?.sampleLength,
    sampleStartPosition: summary?.sampleStartPosition,
    sourceReportNo: summary?.sourceReportNo || '',
  };
}

async function loadAdhesive2FaiSummary() {
  const hasGlueBoard = hasSelectedGlueBoardBatch();
  const loaded = await loadLatestGlueBoardFaiForCurrentSegment({
    applyEmpty: !hasGlueBoard,
    includeGlueBoard: hasGlueBoard,
  });
  if (!loaded && hasGlueBoard) {
    applyGlueBoardInspectionSummary();
  }
}

async function refreshGlueBoardInspectionSummary() {
  if (!currentPlan.planOperationId) {
    message.warning('请先加载粘胶2计划');
    return;
  }
  await loadGlueBoardUsage();
  await loadAdhesive2FaiSummary();
}

async function refreshAdhesive2InspectionStatus(options: { silent?: boolean } = {}) {
  if (!currentPlan.planOperationId) {
    if (!options.silent) {
      message.warning('请先加载粘胶2计划');
    }
    return false;
  }
  if (inspectionStatusRefreshInFlight) return false;
  inspectionStatusRefreshInFlight = true;
  try {
    await Promise.all([loadGlueBoardUsage({ applyCheckDefaults: false }), loadChangeoverInspections(), loadCoaFaiRows(), loadProcessCheckFaiRows(), loadSourcePressSlotFaiRows()]);
    if (!options.silent) {
      // message.success('检验状态已刷新');
    }
    return true;
  } catch (error) {
    if (!options.silent) {
      const data = (error as any)?.response?.data || (error as any)?.data || {};
      const errorMessage = String(data.msg || data.message || (error as any)?.message || '');
      message.warning(errorMessage || '检验状态刷新失败，请稍后重试');
    }
    return false;
  } finally {
    inspectionStatusRefreshInFlight = false;
  }
}

async function loadCoaFaiRows() {
  if (!currentPlan.planId || !currentPlan.planOperationId) {
    coaFaiRows.value = [];
    return;
  }
  coaFaiRows.value = normalizeRows(
    await getAdhesive2CoaFaiList({
      motherBatchNo: currentMotherBatchNo.value || currentPlan.sourceBatchNo || currentPlan.batchNo || undefined,
      planId: currentPlan.planId,
      planOperationId: currentPlan.planOperationId,
    }),
  );
  refreshCurrentPlanAvailableSourceLength();
}

async function loadProcessCheckFaiRows() {
  if (!currentPlan.planId || !currentPlan.planOperationId) {
    processCheckFaiRows.value = [];
    return;
  }
  processCheckFaiRows.value = normalizeRows(
    await getAdhesive2ProcessCheckFaiList({
      motherBatchNo: currentMotherBatchNo.value || currentPlan.sourceBatchNo || currentPlan.batchNo || undefined,
      planId: currentPlan.planId,
      planOperationId: currentPlan.planOperationId,
    }),
  );
}

function dedupeFaiRows(rows: MesHcAdhesiveConsoleApi.FaiSummary[]) {
  const map = new Map<string, MesHcAdhesiveConsoleApi.FaiSummary>();
  rows.forEach((row, index) => {
    const key = String(row.faiId || row.faiNo || `${row.inspectionScene || ''}-${row.productBatchNo || ''}-${row.sourceReportId || ''}-${index}`);
    if (!map.has(key)) map.set(key, row);
  });
  return Array.from(map.values());
}

function isPressSlotSourceSegment(segment?: SourceSegment | null) {
  const sourceMenuCode = String(segment?.sourceMenuCode || '')
    .trim()
    .toUpperCase();
  const processStage = String(segment?.sourceProcessStage || '')
    .trim()
    .toUpperCase();
  return sourceMenuCode.includes('PRESS_SLOT') || processStage === 'PRESS_SLOT';
}

async function loadSourcePressSlotFaiRows(expectedLoadSeq?: number) {
  const currentPlanId = Number(currentPlan.planId || 0);
  const currentPlanOperationId = Number(currentPlan.planOperationId || 0);
  if (!currentPlanId || !currentPlanOperationId) {
    sourcePressSlotFirstInspectionRows.value = [];
    sourcePressSlotProcessCheckRows.value = [];
    return;
  }
  const queryMap = new Map<string, MesHcAdhesiveConsoleApi.SourcePressSlotFaiQuery>();
  sourceGroups.value.forEach((group) => {
    group.segments.forEach((segment) => {
      if (!isPressSlotSourceSegment(segment)) return;
      const sourcePlanId = Number(segment.sourcePlanId || 0);
      const sourcePlanOperationId = Number(segment.sourcePlanOperationId || 0);
      if (!sourcePlanId || !sourcePlanOperationId) return;
      const motherBatchNo = segment.sourceMotherBatchNo || group.baseBatchNo || undefined;
      const key = [currentPlanId, currentPlanOperationId, sourcePlanId, sourcePlanOperationId, normalizeAdhesive2MiddleBatchNo(motherBatchNo || '')].join('|');
      queryMap.set(key, {
        motherBatchNo,
        planId: currentPlanId,
        planOperationId: currentPlanOperationId,
        sourcePlanId,
        sourcePlanOperationId,
      });
    });
  });
  if (!queryMap.size) {
    sourcePressSlotFirstInspectionRows.value = [];
    sourcePressSlotProcessCheckRows.value = [];
    return;
  }
  const results = await Promise.all(
    Array.from(queryMap.values()).map(async (params) => {
      try {
        const [firstRows, processRows] = await Promise.all([getAdhesive2SourcePressSlotFaiList(params), getAdhesive2SourcePressSlotProcessCheckFaiList(params)]);
        return {
          firstRows: normalizeRows(firstRows) as MesHcAdhesiveConsoleApi.FaiSummary[],
          processRows: normalizeRows(processRows) as MesHcAdhesiveConsoleApi.FaiSummary[],
        };
      } catch {
        return { firstRows: [], processRows: [] };
      }
    }),
  );
  if (expectedLoadSeq !== undefined && expectedLoadSeq !== sourceGroupsLoadSeq) return;
  sourcePressSlotFirstInspectionRows.value = dedupeFaiRows(results.flatMap((item) => item.firstRows));
  sourcePressSlotProcessCheckRows.value = dedupeFaiRows(results.flatMap((item) => item.processRows));
}

const latestCoaFai = computed(() => {
  return (
    coaFaiRows.value
      .filter((record) => !!record.faiId)
      .slice()
      .sort((a, b) => {
        const timeDiff = dayjs(b.faiApplyTime || 0).valueOf() - dayjs(a.faiApplyTime || 0).valueOf();
        if (Number.isFinite(timeDiff) && timeDiff !== 0) return timeDiff;
        return Number(b.faiId || 0) - Number(a.faiId || 0);
      })[0] || null
  );
});
const latestProcessCheckFai = computed(() => {
  return (
    processCheckFaiRows.value
      .filter((record) => !!record.faiId)
      .slice()
      .sort((a, b) => {
        const timeDiff = dayjs(b.faiApplyTime || 0).valueOf() - dayjs(a.faiApplyTime || 0).valueOf();
        if (Number.isFinite(timeDiff) && timeDiff !== 0) return timeDiff;
        return Number(b.faiId || 0) - Number(a.faiId || 0);
      })[0] || null
  );
});

function getCurrentChangeoverMotherBatchNo() {
  return (
    currentMotherBatchNo.value ||
    resolveMotherBatchNo({
      parentProductionBatchNo: currentPlan.sourceBatchNo || currentPlan.batchNo,
      sourceProductionBatchNo: currentPlan.sourceProductionBatchNo,
    })
  );
}

function getLatestChangeoverMotherBatchNo(record?: MesHcAdhesiveConsoleApi.ChangeoverInspection | null) {
  return resolveMotherBatchNo({
    parentProductionBatchNo: record?.motherSegmentBatchNo,
    sourceProductionBatchNo: record?.pressSlotSliceNo,
  });
}

function hasCurrentMotherChangeoverInspection() {
  const currentMother = getCurrentChangeoverMotherBatchNo();
  if (!currentMother) return false;
  return changeoverInspections.value.some((record) => Boolean(record?.id)
    && getLatestChangeoverMotherBatchNo(record) === currentMother
    && matchesCurrentChangeoverInstruction(record)
    && ['SUBMITTED', 'RECORDED', 'CONFIRMED'].includes(String((record as Record<string, any>).recordStatus || '').toUpperCase()));
}

function resolveFirstInspectionDisplayText(status?: string, judgment?: string) {
  if (status === 'COMPLETED' && judgment === 'OK') return '已完成';
  if (status === 'COMPLETED' && judgment === 'NG') return '检验NG';
  if (status === 'REJECTED') return '已驳回';
  if (status === 'CANCELED') return '已取消';
  if (status === 'INSPECTING') return '检测中';
  if (status === 'WAITING_QA') return '待复核';
  if (status === 'PENDING') return '待检测';
  return '未提交';
}

function getFirstInspectionStatusMeta() {
  const status = firstInspection.value.faiStatus;
  const judgment = firstInspection.value.faiJudgment;
  if (!firstInspection.value.faiId) return { color: 'default', text: '未提交' };
  if (status === 'COMPLETED' && judgment === 'OK') return { color: 'green', text: '检验OK' };
  if (status === 'COMPLETED' && judgment === 'NG') return { color: 'red', text: '检验NG' };
  if (['CANCELED', 'REJECTED'].includes(status))
    return {
      color: 'red',
      text: resolveFirstInspectionDisplayText(status, judgment),
    };
  if (['INSPECTING', 'PENDING', 'WAITING_QA'].includes(status))
    return {
      color: 'processing',
      text: resolveFirstInspectionDisplayText(status, judgment),
    };
  return {
    color: 'default',
    text: resolveFirstInspectionDisplayText(status, judgment),
  };
}

function getCoaFaiStatusMeta(record: MesHcAdhesiveConsoleApi.FaiSummary | Record<string, any>) {
  const status = String(record?.faiStatus || '').toUpperCase();
  const judgment = String(record?.faiJudgment || '').toUpperCase();
  if (!record?.faiId) return { color: 'default', text: '未提交' };
  if (status === 'COMPLETED' && judgment === 'OK') return { color: 'green', text: '检验OK' };
  if (status === 'COMPLETED' && judgment === 'NG') return { color: 'red', text: '检验NG' };
  if (['CANCELED', 'REJECTED'].includes(status))
    return {
      color: 'red',
      text: resolveFirstInspectionDisplayText(status, judgment),
    };
  if (['INSPECTING', 'PENDING', 'WAITING_QA'].includes(status))
    return {
      color: 'processing',
      text: resolveFirstInspectionDisplayText(status, judgment),
    };
  return {
    color: 'default',
    text: resolveFirstInspectionDisplayText(status, judgment),
  };
}

function normalizeAdhesive2CoaCompareNo(value?: unknown) {
  return String(value ?? '')
    .trim()
    .toUpperCase();
}

function isAdhesive2CoaFaiRecord(record?: MesHcAdhesiveConsoleApi.FaiSummary | Record<string, any> | null) {
  const scene = String(record?.inspectionScene || '')
    .trim()
    .toUpperCase();
  const sourceReportNo = String(record?.sourceReportNo || '')
    .trim()
    .toUpperCase();
  return scene === 'COA' || scene === 'POST_CONFIRM_COA' || sourceReportNo.includes('-COA-');
}

function hasAdhesive2CoaFaiConclusion(record?: MesHcAdhesiveConsoleApi.FaiSummary | Record<string, any> | null) {
  if (!record?.faiId) return false;
  const status = String(record.faiStatus || '')
    .trim()
    .toUpperCase();
  const judgment = String(record.faiJudgment || '')
    .trim()
    .toUpperCase();
  if (['OK', 'NG'].includes(judgment)) return true;
  return status === 'COMPLETED' && !!judgment && !['-', 'NONE', 'PENDING'].includes(judgment);
}

function findAdhesive2CoaFaiBySegment(segment?: SourceSegment | null) {
  if (!segment) return null;
  const batchNo = normalizeAdhesive2CoaCompareNo(segment.batchNo);
  const pressSlotReportId = Number(segment.grindingSecondDetailId || 0);
  const adhesive2ReportId = Number(findReportBySegment(segment)?.id || 0);
  return (
    coaFaiRows.value.find((record) => {
      if (!isAdhesive2CoaFaiRecord(record) || !hasAdhesive2CoaFaiConclusion(record)) return false;
      const recordBatchNo = normalizeAdhesive2CoaCompareNo(record.productBatchNo);
      const recordSourceReportId = Number(record.sourceReportId || 0);
      return (
        (!!batchNo && recordBatchNo === batchNo) ||
        (!!pressSlotReportId && recordSourceReportId === pressSlotReportId) ||
        (!!adhesive2ReportId && recordSourceReportId === adhesive2ReportId)
      );
    }) || null
  );
}

function hasAdhesive2SegmentCoaConclusion(segment?: SourceSegment | null) {
  return !!findAdhesive2CoaFaiBySegment(segment);
}

function getAdhesive2SegmentCoaNg(segment: SourceSegment) {
  const record = findAdhesive2CoaFaiBySegment(segment);
  return (
    String(record?.faiJudgment || '')
      .trim()
      .toUpperCase() === 'NG'
  );
}

function getAdhesive2SegmentCoaConclusionText(segment: SourceSegment) {
  const record = findAdhesive2CoaFaiBySegment(segment);
  if (!record) return '';
  const judgment = String(record.faiJudgment || '')
    .trim()
    .toUpperCase();
  if (judgment === 'OK') return 'COA OK';
  if (judgment === 'NG') return 'COA NG';
  return `COA ${getCoaFaiStatusMeta(record).text}`;
}

function getAdhesive2SegmentAvailableLength(segment?: SourceSegment | null) {
  if (!segment || hasAdhesive2SegmentCoaConclusion(segment)) return 0;
  return Number(segment.availableLength || 0);
}

function getAdhesive2GroupAvailableLength(group?: SourceGroup | null) {
  return group?.segments.reduce((sum, segment) => sum + getAdhesive2SegmentAvailableLength(segment), 0) || 0;
}

function refreshCurrentPlanAvailableSourceLength() {
  const group = getActiveSourceGroup();
  if (group) {
    currentPlan.availableSourceLength = getAdhesive2GroupAvailableLength(group);
  }
}

function resolveGlueBoardInspectionStatus(result?: string, inspectionId?: number) {
  const judgment = String(result || '').toUpperCase();
  if (!inspectionId) return 'PENDING';
  if (['OK', 'NG'].includes(judgment)) return 'COMPLETED';
  return 'PENDING';
}

function getGlueBoardStockQualityMeta(record: MesHcAdhesiveConsoleApi.GlueBoardStock | Record<string, any>) {
  const rawStatus = String(record?.qualityStatus || '')
    .trim()
    .toUpperCase();
  const rawResult = String(record?.latestInspectionResult || '')
    .trim()
    .toUpperCase();
  const statusText = rawResult || rawStatus;
  if (['NG', 'ABNORMAL', 'FAILED', 'REJECTED', 'UNQUALIFIED'].includes(statusText) || ['NG', 'ABNORMAL', 'FAILED', 'REJECTED', 'UNQUALIFIED'].includes(rawStatus)) {
    return { color: 'red', text: '检验NG' };
  }
  if (['OK', 'PASSED', 'QUALIFIED', 'NORMAL'].includes(statusText) || ['OK', 'PASSED', 'QUALIFIED', 'NORMAL'].includes(rawStatus)) {
    return { color: 'green', text: '检验OK' };
  }
  if (['WAITING', 'SUBMITTED', 'PENDING', 'INSPECTING', 'WAITING_QA'].includes(statusText) || ['WAITING', 'SUBMITTED', 'PENDING', 'INSPECTING', 'WAITING_QA'].includes(rawStatus)) {
    return { color: 'processing', text: '已送检待检' };
  }
  return { color: 'default', text: '未送检' };
}

function applyGlueBoardInspectionSummary() {
  const judgment = glueBoard.latestInspectionResult || 'PENDING';
  if (!glueBoard.latestInspectionId) {
    applyFaiSummary({
      allowReportSubmit: true,
      displayText: '当前批号未送检',
      faiApplyTime: '',
      faiJudgment: 'PENDING',
      faiNo: '',
      faiStatus: 'PENDING',
      glueBoardBatchNo: glueBoard.batchNo || '',
      glueBoardMaterialCode: getActualGlueBoardMaterialCode(),
      glueBoardUsageId: glueBoard.id,
      sourceReportNo: '',
    });
    return;
  }
  applyFaiSummary({
    allowReportSubmit: true,
    displayText: glueBoard.latestInspectionId ? `${glueBoard.latestInspectionNo || '-'} / ${judgment}` : '未送检',
    faiApplyTime: glueBoard.inspectionSubmitTime || '',
    faiId: glueBoard.latestInspectionId,
    faiJudgment: judgment,
    faiNo: glueBoard.latestInspectionNo || '',
    faiReturnTime: '',
    faiStatus: resolveGlueBoardInspectionStatus(judgment, glueBoard.latestInspectionId),
    glueBoardBatchNo: glueBoard.batchNo || '',
    glueBoardMaterialCode: getActualGlueBoardMaterialCode(),
    glueBoardUsageId: glueBoard.id,
    sampleLength: glueBoard.aqcSampleLength || undefined,
    sampleStartPosition: glueBoard.receiveStartPosition || undefined,
    sourceReportNo: glueBoard.latestInspectionNo || '',
  });
}

function getCurrentGlueBoardFaiSegmentBatchNo() {
  return String(currentPlan.sourceProductionBatchNo || currentMotherBatchNo.value || reportForm.sourceProductionBatchNo || '').trim();
}

function applyGlueBoardFaiSummary(source: Partial<MesHcAdhesiveConsoleApi.FaiSummary> | null | undefined) {
  if (!source?.faiId) {
    applyFaiSummary({
      allowReportSubmit: true,
      displayText: '当前分段未送检',
      faiApplyTime: '',
      faiJudgment: 'PENDING',
      faiNo: '',
      faiStatus: 'PENDING',
      glueBoardBatchNo: glueBoard.batchNo || source?.glueBoardBatchNo || '',
      glueBoardMaterialCode: getActualGlueBoardMaterialCode(),
      glueBoardUsageId: glueBoard.id,
      sampleLength: undefined,
      sampleStartPosition: undefined,
      sourceReportNo: source?.sourceReportNo || getCurrentGlueBoardFaiSegmentBatchNo(),
    });
    return;
  }
  const judgment = source.faiJudgment || 'PENDING';
  applyFaiSummary({
    allowReportSubmit: true,
    displayText: source.displayText || `${source.faiNo || '-'} / ${judgment}`,
    faiApplyTime: source.faiApplyTime || '',
    faiId: source.faiId,
    faiJudgment: judgment,
    faiNo: source.faiNo || '',
    faiRejectReason: source.faiRejectReason || '',
    faiReturnTime: source.faiReturnTime || '',
    faiStandardNo: source.faiStandardNo || '',
    faiStandardVersion: source.faiStandardVersion || '',
    faiStatus: source.faiStatus || resolveGlueBoardInspectionStatus(judgment, source.faiId),
    glueBoardBatchNo: source.glueBoardBatchNo || glueBoard.batchNo || '',
    glueBoardMaterialCode: source.glueBoardMaterialCode || getActualGlueBoardMaterialCode(),
    glueBoardUsageId: source.glueBoardUsageId || glueBoard.id,
    productBatchNo: source.productBatchNo,
    sampleLength: source.sampleLength,
    sampleStartPosition: source.sampleStartPosition,
    sourceReportNo: source.sourceReportNo || getCurrentGlueBoardFaiSegmentBatchNo(),
  });
}

async function loadLatestGlueBoardFaiForCurrentSegment(options: { applyEmpty?: boolean; includeGlueBoard?: boolean } = {}) {
  const segmentBatchNo = getCurrentGlueBoardFaiSegmentBatchNo();
  if (!currentPlan.planOperationId && !segmentBatchNo) {
    if (options.applyEmpty !== false) {
      applyGlueBoardFaiSummary(null);
    }
    return false;
  }
  const includeGlueBoard = options.includeGlueBoard !== false;
  const summary = await getAdhesiveConsoleLatestGlueBoardFai({
    glueBoardBatchNo: includeGlueBoard ? glueBoard.batchNo || undefined : undefined,
    glueBoardModel: includeGlueBoard ? getActualGlueBoardModel() || undefined : undefined,
    glueBoardStockId: includeGlueBoard ? glueBoard.stockId : undefined,
    planOperationId: currentPlan.planOperationId || undefined,
    sourceProductionBatchNo: segmentBatchNo || undefined,
  });
  const source = (summary as any)?.data ?? summary;
  if (source?.faiId || options.applyEmpty !== false) {
    applyGlueBoardFaiSummary(source);
  }
  return Boolean(source?.faiId);
}

function isFirstInspectionReapplyAllowed() {
  const judgment = String(firstInspection.value.faiJudgment || '')
    .trim()
    .toUpperCase();
  const status = String(firstInspection.value.faiStatus || '')
    .trim()
    .toUpperCase();
  const rejectedStatuses = [
    'CANCELED',
    'CANCELLED',
    'REJECTED',
    'RETURNED',
    'RETURNED_WAIT_RESUBMIT',
    'WAIT_RESUBMIT',
  ];
  return judgment === 'NG' || rejectedStatuses.includes(status);
}

function getGlueBoardFaiMaxSampleLength() {
  const availableLength = Number(glueBoard.stockLength || 0);
  const stockLength = Number(glueBoard.stockLength || 0);
  if (availableLength <= 0) return 0;
  return stockLength > 0 ? Math.min(availableLength, stockLength) : availableLength;
}

function getChangeoverInspectionBlockMessage() {
  if (!firstInspection.value.faiId) {
    return `当前粘胶2工单暂无 FAI 工艺参数点检单。来源单号 ${firstInspection.value.sourceReportNo || '-'}，请先提交工艺参数点检并等待检验 OK 放行。`;
  }
  if (isFirstInspectionReapplyAllowed()) {
    return `当前粘胶2 FAI 工艺参数点检状态为 ${getFirstInspectionStatusMeta().text}，请重新提交工艺参数点检并完成 OK 放行后再报工。`;
  }
  return `当前粘胶2 FAI 工艺参数点检状态为 ${getFirstInspectionStatusMeta().text}，尚未 OK 放行，不能保存报工、扫码确认或工单完工。`;
}

function showChangeoverInspectionRequiredWarning(_initialSliceNo = '') {
  const canApplyInspection = !firstInspection.value.faiId || isFirstInspectionReapplyAllowed();
  AModal.warning({
    title: '需要先完成 FAI 工艺参数点检放行',
    content: getChangeoverInspectionBlockMessage(),
    okText: canApplyInspection ? '胶板送检' : '查看状态',
    onOk: () => {
      if (canApplyInspection) {
        openGlueBoardAqcDialog();
        return;
      }
      goGlueBoardInspectionTab();
    },
  });
}

async function loadGlueBoardUsage(options: { applyCheckDefaults?: boolean } = {}) {
  if (!hasLoadedGlueBoardPlan.value) {
    if (glueBoard.id && glueBoard.stockId && glueBoard.batchNo) {
      applyGlueBoardInspectionSummary();
      if (options.applyCheckDefaults !== false) {
        applyGlueBoardDefaultsToCheckItems();
      }
      return;
    }
    clearCurrentGlueBoardUsage(currentPlan.glueBoardModel);
    applyGlueBoardInspectionSummary();
    if (options.applyCheckDefaults !== false) {
      applyGlueBoardDefaultsToCheckItems();
    }
    return;
  }
  const expectedGlueBoardModel = glueBoardMapCandidateModels.value.length === 1 ? String(glueBoardMapCandidateModels.value[0] || '').trim() : '';
  let usage = await getAdhesiveConsoleCurrentGlueBoardUsage(currentPlan.planOperationId, expectedGlueBoardModel);
  let source = (usage as any)?.data ?? usage;
  if (!source?.id && expectedGlueBoardModel) {
    usage = await getAdhesiveConsoleCurrentGlueBoardUsage(currentPlan.planOperationId);
    source = (usage as any)?.data ?? usage;
  }
  const missingModelAlarm = expectedGlueBoardModel ? `未找到胶板型号 ${expectedGlueBoardModel} 的有效领用记录，请领用正确型号的胶板批次。` : '';
  if (!source?.id) {
    clearCurrentGlueBoardUsage(currentPlan.glueBoardModel, missingModelAlarm || '当前计划绑定机台未找到有效胶板领用记录，请先领用胶板。');
    applyGlueBoardInspectionSummary();
    if (options.applyCheckDefaults !== false) {
      applyGlueBoardDefaultsToCheckItems();
    }
    return;
  }
  Object.assign(glueBoard, {
    alarm: source?.id ? (source?.qualityStatus === 'ABNORMAL' ? source?.qualityLockReason || '胶板粘胶检验异常，请更换胶板或处理质量状态。' : '') : missingModelAlarm,
    aqcSampleLength: Number(source?.aqcSampleLength || 0),
    availableStartPosition: Number(source?.availableStartPosition || 0),
    batchNo: source?.glueBoardBatchNo || '',
    id: source?.id,
    inspectionSubmitTime: source?.inspectionSubmitTime || '',
    latestAqcTask: source?.latestAqcTask,
    latestInspectionId: source?.latestInspectionId,
    latestInspectionNo: source?.latestInspectionNo || '',
    latestInspectionResult: source?.latestInspectionResult || '',
    lossLength: Number(source?.lossLength || 0),
    materialCode: source?.glueBoardMaterialCode || '',
    model: source?.glueBoardModel || '',
    qualityLockReason: source?.qualityLockReason || '',
    qualityLockStartPosition: source?.qualityLockStartPosition,
    qualityStatus: source?.qualityStatus || '',
    receiveLength: Number(source?.receiveLength || 0),
    receiveStartPosition: Number(source?.receiveStartPosition || 0),
    returnedLength: Number(source?.returnedLength || 0),
    returnedStartPosition: Number(source?.returnedStartPosition || 0),
    stockLength: Number(source?.availableLength || 0),
    stockId: source?.glueBoardStockId,
    todayUsedLength: Number(source?.consumedLength || 0),
  });
  const mismatchTip = source?.id
    ? buildGlueBoardMapMismatchTip({
        glueBoardBatchNo: source?.glueBoardBatchNo,
        glueBoardMaterialCode: source?.glueBoardMaterialCode,
        glueBoardModel: source?.glueBoardModel,
      })
    : '';
  if (mismatchTip) {
    glueBoard.alarm = mismatchTip;
  }
  applyGlueBoardInspectionSummary();
  if (options.applyCheckDefaults !== false) {
    applyGlueBoardDefaultsToCheckItems();
  }
  await loadLatestGlueBoardFaiForCurrentSegment({ applyEmpty: false });
}

function resetIntermediateForm() {
  Object.assign(intermediateForm, {
    adhesiveReportId: undefined,
    batchNo: '',
    confirmerName: '',
    confirmTime: '',
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
    glueBoardAdhesionGf: '',
    glueBoardModel: '',
    glueBoardWidthMm: '',
    id: undefined,
    importAttachment: undefined,
    inputQty: 0,
    materialCode: currentProductMaterialCode.value || '',
    middleSliceNo: '',
    modelCode: currentProductModelCode.value || '',
    outputQty: 0,
    planId: currentPlan.planId || 0,
    planNo: currentPlan.planNo || '',
    planOperationId: currentPlan.planOperationId || 0,
    processLength: 0,
    productionDate: buildNowText(),
    recordDate: buildNowText(),
    recorderName: currentUserName.value || '',
    recordStatus: 'DRAFT',
    remark: '',
    showGlueBoardModel: undefined,
    sourceExcel: '',
    sourceSheet: '',
    stationFormCode: '',
    stationFormDisplayName: '',
    stationFormId: undefined,
    stationFormName: '',
    stationFormProcessCode: '',
    stationFormSchemaJson: '',
    templateCode: '',
    templateName: '',
    thicknessColumnCount: undefined,
    thicknessHeaderText: '',
    thicknessIntervalCm: undefined,
    thicknessStandard: '',
    widthEnd: undefined,
    widthMiddle: undefined,
    widthStart: undefined,
  });
  intermediateDetails.value = buildDefaultAdhesive2IntermediateDetails();
}

function parseIntermediateStationFormSchema() {
  const source = String((intermediateForm as Record<string, any>).stationFormSchemaJson || '').trim();
  if (!source) return {};
  try {
    const parsed = JSON.parse(source);
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? (parsed as Record<string, any>) : {};
  } catch {
    return {};
  }
}

function readIntermediateTemplateText(...keys: string[]) {
  const form = intermediateForm as Record<string, any>;
  const schema = intermediateTemplateSchema.value;
  for (const key of keys) {
    const value = form[key] ?? schema[key];
    const text = String(value ?? '').trim();
    if (text) return text;
  }
  return '';
}

function readIntermediateTemplateNumber(...keys: string[]) {
  const text = readIntermediateTemplateText(...keys);
  const value = Number(text);
  return Number.isFinite(value) && value > 0 ? value : undefined;
}

function normalizeIntermediateThicknessStandard(value?: string) {
  const text = String(value || '').trim();
  if (!text) return ADHESIVE2_INTERMEDIATE_DEFAULT_THICKNESS_STANDARD;
  const matched = text.match(/[\d.]+\s*±\s*[\d.]+/);
  return (matched?.[0] || text).replace(/\s+/g, '').replace(/mm$/i, '').replace(/㎜$/i, '');
}

function resolveCurrentGlueBoardModelForIntermediate() {
  return String(getActualGlueBoardModel() || getSelectedGlueBoardModel() || glueBoard.model || currentPlan.glueBoardModel || reportForm.glueBoardModel || '').trim();
}

function normalizeAdhesive2MiddleBatchNo(batchNo?: string) {
  return String(batchNo || '')
    .trim()
    .replace(/-J\d+$/i, '')
    .replace(/-S\d+$/i, '')
    .replace(/^(.+[PQRS])\d{3}[A-Z]?$/i, '$1');
}

function resolveCurrentIntermediateBatchNo() {
  return normalizeAdhesive2MiddleBatchNo(currentMotherBatchNo.value || currentPlan.sourceBatchNo || currentPlan.sourceProductionBatchNo || currentPlan.batchNo);
}

function resolveIntermediateReportMotherBatchNo(record: Record<string, any>) {
  return normalizeAdhesive2MiddleBatchNo(resolveMotherBatchNo(record));
}

function resolveIntermediateBatchNo() {
  const currentBatchNo = resolveCurrentIntermediateBatchNo();
  if (currentBatchNo) return currentBatchNo;
  const fromReport = reportRecords.value.map((item) => item.parentProductionBatchNo || item.sourceProductionBatchNo || item.sourceBatchNo || item.productionBatchNo).find(Boolean);
  const fromSource = sourceGroups.value
    .flatMap((group) => group.segments)
    .map((item) => item.batchNo)
    .find(Boolean);
  return normalizeAdhesive2MiddleBatchNo(fromReport || fromSource || currentPlan.sourceProductionBatchNo || currentPlan.batchNo);
}

function syncIntermediateCountersFromReports() {
  const today = dayjs(intermediateForm.recordDate || undefined).format('YYYY-MM-DD');
  const currentBatchNo = normalizeAdhesive2MiddleBatchNo(intermediateForm.batchNo || resolveCurrentIntermediateBatchNo());
  const sameDay = reportRecords.value.filter((item) => {
    const dateMatched = !item.reportDate || String(item.reportDate).startsWith(today);
    const batchMatched = !currentBatchNo || isSameText(resolveIntermediateReportMotherBatchNo(item as Record<string, any>), currentBatchNo);
    return dateMatched && batchMatched;
  });
  intermediateForm.inputQty = sameDay.reduce((sum, item) => sum + Number(item.inputLength || item.outputLength || 1), 0);
  intermediateForm.outputQty = sameDay.filter((item) => isAdhesiveRecordConfirmed(item.reportStatus)).reduce((sum, item) => sum + Number(item.outputLength || item.inputLength || 1), 0);
  intermediateForm.batchNo = normalizeAdhesive2MiddleBatchNo(intermediateForm.batchNo || resolveIntermediateBatchNo());
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

function mapAdhesive2IntermediateRecord(data?: MesHcAdhesiveConsoleApi.IntermediateRecord) {
  resetIntermediateForm();
  if (data?.id) {
    selectedIntermediateRecord.value = data;
  }
  Object.assign(intermediateForm, {
    ...data,
    batchNo: normalizeAdhesive2MiddleBatchNo(data?.batchNo || resolveIntermediateBatchNo()),
    glueBoardModel: data?.glueBoardModel || (data?.id ? '' : resolveCurrentGlueBoardModelForIntermediate()),
    materialCode: data?.materialCode || currentProductMaterialCode.value || '',
    modelCode: data?.modelCode || currentProductModelCode.value || '',
    planId: currentPlan.planId || data?.planId || 0,
    planNo: currentPlan.planNo || data?.planNo || '',
    planOperationId: currentPlan.planOperationId || data?.planOperationId || 0,
    productionDate: normalizeIntermediateRecordDateTime(data?.recordDate || data?.productionDate),
    recordDate: normalizeIntermediateRecordDateTime(data?.recordDate || data?.productionDate),
    recorderName: data?.recorderName || currentUserName.value || '',
    recordStatus: data?.recordStatus || 'DRAFT',
  });
  const backendDetails = Array.isArray(data?.details) ? data.details : [];
  const detailMap = new Map<string, MesHcAdhesiveConsoleApi.IntermediateDetail>();
  backendDetails.forEach((item) => {
    detailMap.set(item.samplePosition || `ROW-${item.sortNo || detailMap.size + 1}`, item);
  });
  intermediateDetails.value = buildDefaultAdhesive2IntermediateDetails().map((row) => ({
    ...row,
    ...(detailMap.get(row.samplePosition || '') || {}),
  }));
  backendDetails.forEach((row) => {
    if (row.samplePosition && adhesive2IntermediatePositions.some((item) => item.value === row.samplePosition)) return;
    intermediateDetails.value.push(row);
  });
  syncIntermediateSliceNoFieldsFromDetails();
  syncIntermediateCountersFromReports();
}

async function loadAdhesive2IntermediateRecords() {
  if (!currentPlan.planId || !currentPlan.planOperationId) {
    adhesive2IntermediateRecords.value = [];
    selectedIntermediateRecord.value = null;
    resetIntermediateForm();
    return;
  }
  intermediateLoading.value = true;
  try {
    const currentBatchNo = resolveCurrentIntermediateBatchNo();
    adhesive2IntermediateRecords.value = normalizeRows(await getAdhesive2IntermediateList(currentPlan.planOperationId, currentBatchNo)).filter(
      (item) => !currentBatchNo || isSameText(normalizeAdhesive2MiddleBatchNo(item.batchNo), currentBatchNo),
    );
    if (selectedIntermediateRecord.value?.id) {
      selectedIntermediateRecord.value = adhesive2IntermediateRecords.value.find((item) => item.id === selectedIntermediateRecord.value?.id) || null;
    }
  } finally {
    intermediateLoading.value = false;
  }
}

async function loadIntermediateRecord(record?: MesHcAdhesiveConsoleApi.IntermediateRecord) {
  if (!currentPlan.planId || !currentPlan.planOperationId) {
    resetIntermediateForm();
    return;
  }
  const targetRecord = record || selectedIntermediateRecord.value;
  if (!targetRecord && !intermediateDetailVisible.value) {
    resetIntermediateForm();
    return;
  }
  intermediateLoading.value = true;
  try {
    const data = await getAdhesiveConsoleIntermediate({
      batchNo: targetRecord?.batchNo || intermediateForm.batchNo || resolveCurrentIntermediateBatchNo() || undefined,
      id: targetRecord?.id || intermediateForm.id,
      planId: currentPlan.planId,
      planOperationId: currentPlan.planOperationId,
      recordDate: normalizeIntermediateRecordDateTime(targetRecord?.recordDate || intermediateForm.recordDate),
    });
    mapAdhesive2IntermediateRecord(data);
  } finally {
    intermediateLoading.value = false;
  }
}

async function openIntermediateRecordDetail(record: MesHcAdhesiveConsoleApi.IntermediateRecord) {
  selectedIntermediateRecord.value = record;
  intermediateDetailVisible.value = true;
  await loadIntermediateRecord(record);
}

async function openNewIntermediateRecordDetail() {
  if (!(await ensureAdhesive2PlanGlueBoardReadyForAction('新建中间品记录'))) return;
  const batchNo = resolveCurrentIntermediateBatchNo();
  if (!batchNo) {
    message.warning('当前计划缺少母批号，不能新建粘胶2中间品记录表');
    return;
  }
  selectedIntermediateRecord.value = null;
  resetIntermediateForm();
  Object.assign(intermediateForm, {
    batchNo,
    planId: currentPlan.planId,
    planNo: currentPlan.planNo,
    planOperationId: currentPlan.planOperationId,
    productionDate: buildNowText(),
    recordDate: buildNowText(),
    recorderName: currentUserName.value || '',
    recordStatus: 'DRAFT',
    glueBoardModel: resolveCurrentGlueBoardModelForIntermediate(),
  });
  intermediateDetailVisible.value = true;
  await loadIntermediateRecord({
    batchNo,
    planId: currentPlan.planId,
    planNo: currentPlan.planNo,
    planOperationId: currentPlan.planOperationId,
    recordDate: intermediateForm.recordDate,
  } as MesHcAdhesiveConsoleApi.IntermediateRecord);
}

function closeIntermediateRecordDetail() {
  intermediateDetailVisible.value = false;
  selectedIntermediateRecord.value = null;
  resetIntermediateForm();
}

function requestCloseReport() {
  if (reportDialogMode.value === 'view') {
    reportVisible.value = false;
    singleScanConfirmAfterInspection.value = false;
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
      singleScanConfirmAfterInspection.value = false;
    },
  });
}

function clearReportIntermediateDraft() {
  // 粘胶2中间品已调整为按日业务表，不再使用报工弹窗内的临时草稿。
}

async function saveIntermediateRecord(recordStatus: 'CONFIRMED' | 'RECORDED' = 'RECORDED', options: { closeAfterSuccess?: boolean } = {}) {
  if (isCurrentTaskReadonly.value) {
    showReadonlyTaskWarning('保存中间品记录');
    return intermediateForm.id;
  }
  if (intermediateRecordLocked.value) {
    message.warning('粘胶2中间品记录单已确认，不能修改');
    return intermediateForm.id;
  }
  if (!currentPlan.planId || !currentPlan.planOperationId) {
    message.warning('请先扫描或选择粘胶2计划');
    return undefined;
  }
  if (recordStatus === 'CONFIRMED') {
    intermediateForm.confirmerName = intermediateForm.confirmerName || currentUserName.value || '';
    intermediateForm.confirmTime = dayjs().format('YYYY-MM-DD HH:mm:ss');
  }
  syncIntermediateDetailsFromSliceNoFields();
  syncIntermediateCountersFromReports();
  intermediateLoading.value = true;
  try {
    const recordId = await saveAdhesiveConsoleIntermediate({
      ...intermediateForm,
      batchNo: normalizeAdhesive2MiddleBatchNo(intermediateForm.batchNo || resolveCurrentIntermediateBatchNo() || resolveIntermediateBatchNo()),
      details: intermediateDetails.value,
      planId: currentPlan.planId,
      planNo: currentPlan.planNo,
      planOperationId: currentPlan.planOperationId,
      recordDate: normalizeIntermediateRecordDateTime(intermediateForm.recordDate),
      recordStatus,
    });
    const savedId = Number((recordId as any)?.data ?? recordId ?? intermediateForm.id);
    intermediateForm.id = savedId;
    intermediateForm.recordStatus = recordStatus;
    selectedIntermediateRecord.value = {
      ...(selectedIntermediateRecord.value || {}),
      ...intermediateForm,
      id: intermediateForm.id,
    } as MesHcAdhesiveConsoleApi.IntermediateRecord;
    message.success(recordStatus === 'CONFIRMED' ? '粘胶2中间品记录单确认成功' : '粘胶2中间品记录单保存成功');
    await loadAdhesive2IntermediateRecords();
    if (options.closeAfterSuccess === false) {
      await loadIntermediateRecord(selectedIntermediateRecord.value || undefined);
    } else {
      activeBoardTab.value = 'MIDDLE_LEDGER';
      closeIntermediateRecordDetail();
    }
    return savedId;
  } finally {
    intermediateLoading.value = false;
  }
}

function openIntermediateSaveAuth() {
  if (isCurrentTaskReadonly.value) {
    showReadonlyTaskWarning('保存中间品记录');
    return;
  }
  if (intermediateRecordLocked.value) {
    message.warning('粘胶2中间品记录单已确认，不能修改');
    return;
  }
  pendingIntermediateAuthMode.value = 'save';
  intermediateAuthAction.value = `保存${intermediateForm.templateName || intermediateForm.stationFormDisplayName || '粘胶2中间品记录表'}`;
  intermediateAuthVisible.value = true;
}

function confirmIntermediateRecord() {
  if (isCurrentTaskReadonly.value) {
    showReadonlyTaskWarning('确认中间品记录');
    return;
  }
  if (intermediateRecordLocked.value) {
    message.warning('粘胶2中间品记录单已确认，不能重复确认或修改');
    return;
  }
  pendingIntermediateAuthMode.value = 'confirm';
  intermediateAuthAction.value = `确认${intermediateForm.templateName || intermediateForm.stationFormDisplayName || '粘胶2中间品记录表'}`;
  intermediateAuthVisible.value = true;
}

async function handleIntermediateAuthSuccess(userInfo: any) {
  const mode = pendingIntermediateAuthMode.value;
  intermediateAuthVisible.value = false;
  const operatorName = resolveAuthenticatedUserName(userInfo, mode === 'confirm' ? intermediateForm.confirmerName : intermediateForm.recorderName);
  if (!operatorName) {
    message.warning('未识别到认证人员，请重新认证');
    return;
  }
  if (mode === 'save') {
    intermediateForm.recorderName = operatorName;
    await saveIntermediateRecord('RECORDED');
    return;
  }
  intermediateForm.confirmerName = operatorName;
  intermediateForm.confirmTime = dayjs().format('YYYY-MM-DD HH:mm:ss');
  await saveIntermediateRecord('CONFIRMED');
}

function handleIntermediateAuthCancel() {
  intermediateAuthVisible.value = false;
}

function triggerMiddleLedgerImport() {
  if (isCurrentTaskReadonly.value) {
    showReadonlyTaskWarning('导入中间品记录');
    return;
  }
  if (intermediateRecordLocked.value) {
    message.warning('粘胶2中间品记录单已确认，不能导入修改');
    return;
  }
  if (!intermediateDetailVisible.value || !String(intermediateForm.batchNo || '').trim()) {
    message.warning('请先打开需要导入的粘胶2中间品记录表');
    return;
  }
  middleLedgerFileInput.value?.click();
}

function buildAdhesive2ImportAttachment(file: File, uploaded: any): Adhesive2ImportAttachment {
  const url = typeof uploaded === 'string' ? uploaded : uploaded?.url;
  return {
    name: uploaded?.name || file.name,
    path: uploaded?.path,
    size: uploaded?.size || file.size,
    type: uploaded?.type || file.type,
    uid: `${Date.now()}-${file.name}`,
    uploadTime: dayjs().format('YYYY-MM-DD HH:mm:ss'),
    url: url || uploaded?.path,
  };
}

function normalizeAdhesive2ImportAttachments(value?: Adhesive2ImportAttachment | Adhesive2ImportAttachment[] | unknown) {
  const list = Array.isArray(value) ? value : value ? [value] : [];
  return list
    .map((item, index) => {
      if (!item || typeof item !== 'object') return null;
      const raw = item as Adhesive2ImportAttachment & Record<string, any>;
      const path = String(raw.path || raw.filePath || '');
      const url = String(raw.url || raw.fileUrl || path || '');
      const name = String(raw.name || raw.fileName || url.split('/').pop() || `附件${index + 1}`);
      if (!url) return null;
      return {
        name,
        path: path || undefined,
        size: Number(raw.size) || undefined,
        type: raw.type,
        uploadTime: raw.uploadTime,
        uid: raw.uid,
        url,
      };
    })
    .filter(Boolean)
    .slice(-1) as Adhesive2ImportAttachment[];
}

function formatAdhesive2AttachmentSize(size?: number) {
  if (!size || size <= 0) return '';
  if (size < 1024) return `${size} B`;
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`;
  return `${(size / 1024 / 1024).toFixed(1)} MB`;
}

function openAdhesive2ImportAttachment(attachment?: Adhesive2ImportAttachment) {
  const url = attachment?.url || attachment?.path;
  if (!url) {
    message.warning('附件地址为空，无法下载');
    return;
  }
  window.open(url, '_blank');
}

async function uploadAdhesive2ImportAttachment(file: File, directory: string) {
  const uploaded = await uploadFile({ directory, file });
  return buildAdhesive2ImportAttachment(file, uploaded);
}

function isChangeoverRecordConfirmed(record?: MesHcAdhesiveConsoleApi.ChangeoverInspection | Record<string, any> | null) {
  const row = (record || {}) as Record<string, any>;
  const status = String(row.recordStatus || row.reportStatus || row.inspectionStatus || '').toUpperCase();
  return status === 'CONFIRMED';
}

function getChangeoverRecordStatusMeta(record?: MesHcAdhesiveConsoleApi.ChangeoverInspection | Record<string, any> | null) {
  if (isChangeoverRecordConfirmed(record)) return { color: 'processing', text: '已确认' };
  const status = String(((record || {}) as Record<string, any>).recordStatus || '').toUpperCase();
  if (status === 'RECORDED' || status === 'SUBMITTED' || status === 'QUALIFIED' || status === 'ABNORMAL') {
    return { color: 'success', text: '已保存' };
  }
  return { color: 'default', text: '草稿' };
}

const changeoverRecordLocked = computed(() => isCurrentTaskReadonly.value || isChangeoverRecordConfirmed(changeoverForm as Record<string, any>) || !matchesCurrentChangeoverInstruction(changeoverForm));
const changeoverConfirmUserName = computed(() => String((changeoverForm as Record<string, any>).confirmUserName || ''));
const changeoverConfirmTime = computed(() => String((changeoverForm as Record<string, any>).confirmTime || ''));

const changeoverImportAttachment = computed(() => {
  const row = changeoverForm as Record<string, any>;
  const extra = parseRecordExtra(row) as Record<string, any>;
  return normalizeAdhesive2ImportAttachments(row.importAttachment || extra.importAttachment || extra.attachments || extra.processFormExcelAttachments)[0];
});

const intermediateImportAttachment = computed(() => {
  const extra = parseRecordExtra(intermediateForm as Record<string, any>) as Record<string, any>;
  return normalizeAdhesive2ImportAttachments(intermediateForm.importAttachment || extra.attachments || extra.importAttachment || extra.processFormExcelAttachments)[0];
});

async function handleMiddleLedgerFileChange(event: Event) {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  input.value = '';
  if (!file) return;
  if (!currentPlan.planId || !currentPlan.planOperationId) {
    message.warning('请先扫描或选择粘胶2计划');
    return;
  }
  if (!intermediateDetailVisible.value || !String(intermediateForm.batchNo || '').trim()) {
    message.warning('请先打开需要导入的粘胶2中间品记录表');
    return;
  }
  if (intermediateRecordLocked.value) {
    message.warning('粘胶2中间品记录单已确认，不能导入修改');
    return;
  }
  middleLedgerImporting.value = true;
  try {
    const attachment = await uploadAdhesive2ImportAttachment(file, 'mes/adhesive2/intermediate');
    const count = await importAdhesive2MiddleLedger(currentPlan.planId, currentPlan.planOperationId, file, attachment, {
      batchNo: normalizeAdhesive2MiddleBatchNo(intermediateForm.batchNo || resolveCurrentIntermediateBatchNo() || resolveIntermediateBatchNo()),
      recordDate: normalizeIntermediateRecordDateTime(intermediateForm.recordDate),
      recordId: selectedIntermediateRecord.value?.id || intermediateForm.id,
    });
    await Promise.all([loadReports(), loadAdhesive2IntermediateRecords(), loadSourceGroups()]);
    await loadIntermediateRecord(selectedIntermediateRecord.value || undefined);
    message.success(`已导入 ${Number(((count as any)?.data ?? count) || 0)} 条粘胶2中间品记录`);
  } finally {
    middleLedgerImporting.value = false;
  }
}

async function exportMiddleLedgerExcel() {
  if (!currentPlan.planOperationId) {
    message.warning('请先扫描或选择粘胶2计划');
    return;
  }
  if (!intermediateDetailVisible.value || !String(intermediateForm.batchNo || '').trim()) {
    message.warning('请先打开需要导出的粘胶2中间品记录表');
    return;
  }
  let recordId = selectedIntermediateRecord.value?.id || intermediateForm.id;
  if (!recordId) {
    recordId = await saveIntermediateRecord('RECORDED', {
      closeAfterSuccess: false,
    });
  }
  if (!recordId) {
    message.warning('请先保存粘胶2中间品记录表后再导出');
    return;
  }
  const data = await exportAdhesive2MiddleLedger(currentPlan.planOperationId, {
    batchNo: normalizeAdhesive2MiddleBatchNo(intermediateForm.batchNo || resolveCurrentIntermediateBatchNo() || resolveIntermediateBatchNo()),
    recordId,
  });
  downloadFileFromBlobPart({
    fileName: 'CMP粘胶2中间品记录表.xlsx',
    source: data,
  });
}

function findReportBySegment(segment: SourceSegment) {
  return reportRecords.value.find((item) => String(item.sourceProductionBatchNo || '') === segment.batchNo);
}

function getSegmentReportRecords(segment: SourceSegment) {
  return getReportRecordsBySourceBatchNo(segment.batchNo);
}

function getAdhesive2SegmentEditBlockedReason(segment: SourceSegment) {
  return (
    getSegmentReportRecords(segment)
      .sort((a, b) => Number(b.id || 0) - Number(a.id || 0))
      .map((record) => String(record.editBlockedReason || '').trim())
      .find(Boolean) || ''
  );
}

function getSourceGroupReportRecords(group?: SourceGroup | null) {
  if (!group) return [];
  const groupBatchNo = normalizeAdhesive2MiddleBatchNo(group.baseBatchNo);
  const segmentBatchNos = new Set(
    group.segments
      .map((segment) =>
        String(segment.batchNo || '')
          .trim()
          .toUpperCase(),
      )
      .filter(Boolean),
  );
  return reportRecords.value.filter((record) => {
    const sourceBatchNo = String(record.sourceProductionBatchNo || '')
      .trim()
      .toUpperCase();
    if (sourceBatchNo && segmentBatchNos.has(sourceBatchNo)) return true;
    return !!groupBatchNo && normalizeAdhesive2MiddleBatchNo(resolveMotherBatchNo(record as Record<string, any>)) === groupBatchNo;
  });
}

function isAdhesiveRecordSubmitted(status?: string) {
  return String(status || '').toUpperCase() === 'SUBMITTED';
}

function isSegmentCompleted(segment?: SourceSegment | null) {
  if (!segment) return false;
  const records = getSegmentReportRecords(segment);
  return records.length > 0 && records.every((record) => isAdhesiveRecordSubmitted(record.reportStatus));
}

function isSourceGroupCompleted(group?: SourceGroup | null) {
  const records = getSourceGroupReportRecords(group);
  return records.length > 0 && records.every((record) => isAdhesiveRecordSubmitted(record.reportStatus));
}

function hasAdhesive2TailSelection(record?: MesHcAdhesiveConsoleApi.ReportItem | null) {
  return !!String(record?.actualSizeRule || '').trim() && !!String(record?.actualSizeSuffix || '').trim();
}

function getAdhesive2TailBatchNo(sourceBatchNo?: string, actualSizeRule?: string) {
  const source = String(sourceBatchNo || '')
    .trim()
    .toUpperCase();
  const normalizedSizeRule = normalizeSizeRule(actualSizeRule);
  if (!source || !normalizedSizeRule) return source;
  const suffix = normalizedSizeRule === '740mm' ? 'B' : 'A';
  return source.replace(/([PQRS]\d{3})[AB]$/i, '$1') + suffix;
}

function getAdhesive2SegmentDisplayBatchNo(segment: SourceSegment) {
  return getAdhesive2TailBatchNo(segment.batchNo, segment.actualSizeRule);
}

type SelectedTailRow = {
  sourceProductionBatchNo: string;
  currentSizeRule: string;
  actualSizeRule: string;
  currentBatchNo: string;
};
const selectedTailVisible = ref(false);
const selectedTailSubmitting = ref(false);
const selectedTailScanInputRef = ref<any>();
const selectedTailScanCode = ref('');
const selectedTailSizeRule = ref('');
const selectedTailRow = ref<SelectedTailRow>();
const selectedTailFeedback = ref('');
const selectedTailContext = ref<{ planId: number; planOperationId: number }>();

function openSelectedTailAssign() {
  if (isCurrentTaskReadonly.value) { showReadonlyTaskWarning('扫码改尾号'); return; }
  if (!currentPlan.planId || !currentPlan.planOperationId) return;
  selectedTailContext.value = { planId: currentPlan.planId, planOperationId: currentPlan.planOperationId };
  selectedTailSizeRule.value = '';
  selectedTailScanCode.value = '';
  selectedTailRow.value = undefined;
  selectedTailFeedback.value = '';
  selectedTailVisible.value = true;
  focusInputRef(selectedTailScanInputRef);
}

async function scanSelectedTail() {
  if (selectedTailSubmitting.value) return;
  selectedTailRow.value = undefined;
  selectedTailFeedback.value = '';
  if (!selectedTailSizeRule.value) { message.warning('请选择目标尺寸/尾号后确认'); return; }
  const scanned = normalizeAdhesive2ScanBatchNo(selectedTailScanCode.value);
  if (!scanned) { message.warning('请扫描或输入片号'); return; }
  const matches = getAdhesive2TailAssignableSegments().filter((segment) => {
    const records = getSegmentReportRecords(segment);
    return [segment.batchNo, getAdhesive2SegmentDisplayBatchNo(segment), ...records.map((record) => record.productionBatchNo)]
      .some((batchNo) => normalizeAdhesive2ScanBatchNo(batchNo) === scanned);
  });
  if (matches.length !== 1) { message.warning('未找到唯一的当前任务片号，请核对流转单'); return; }
  const segment = matches[0]!;
  const records = getSegmentReportRecords(segment);
  if (records.some((record) => String(record.reportStatus || 'DRAFT').toUpperCase() !== 'DRAFT'
    || getRecordPrintMeta(record).status === '已打印' || getRecordPrintMeta(record).count > 0)) {
    message.warning('该片已确认或已打印，不能直接修改尾号'); return;
  }
  const currentSizeRule = normalizeSizeRule(records[0]?.actualSizeRule || segment.actualSizeRule);
  selectedTailRow.value = {
    sourceProductionBatchNo: String(segment.batchNo || ''), currentSizeRule,
    actualSizeRule: selectedTailSizeRule.value,
    currentBatchNo: records[0]?.productionBatchNo || getAdhesive2SegmentDisplayBatchNo(segment),
  };
  await submitSelectedTailAssign();
}

async function submitSelectedTailAssign() {
  if (selectedTailSubmitting.value) return;
  const context = selectedTailContext.value;
  if (!context || context.planId !== currentPlan.planId || context.planOperationId !== currentPlan.planOperationId || isCurrentTaskReadonly.value) {
    message.warning('当前任务已变化，请重新打开扫码改尾号'); return;
  }
  const row = selectedTailRow.value;
  if (!row) { message.warning('请先扫描片号'); return; }
  if (!row.actualSizeRule) { message.warning('请选择该片的实际尺寸'); return; }
  if (row.actualSizeRule === row.currentSizeRule) {
    selectedTailFeedback.value = `${row.currentBatchNo} 已是所选尾号，无需修改，请扫描下一片`;
    selectedTailRow.value = undefined;
    selectedTailScanCode.value = '';
    focusInputRef(selectedTailScanInputRef);
    return;
  }
  selectedTailSubmitting.value = true;
  try {
    await assignAdhesive2TailForSelectedSources({ ...context, items: [{
      sourceProductionBatchNo: row.sourceProductionBatchNo, actualSizeRule: row.actualSizeRule,
    }] });
    selectedTailFeedback.value = `已修改 ${row.currentBatchNo} → ${getAdhesive2TailBatchNo(row.sourceProductionBatchNo, row.actualSizeRule)}，请扫描下一片`;
    selectedTailRow.value = undefined;
    selectedTailScanCode.value = '';
    await Promise.all([loadReports(), loadSourceGroups()]);
  } catch (error: any) {
    message.error(error?.message || '扫码改尾号失败');
  } finally {
    selectedTailSubmitting.value = false;
    focusInputRef(selectedTailScanInputRef);
  }
}

function getAdhesive2TailAssignableSegments() {
  return sourceGroups.value.flatMap((group) => group.segments);
}

function openAdhesive2TailAssign() {
  if (isCurrentTaskReadonly.value) {
    showReadonlyTaskWarning('一键选择尾号');
    return;
  }
  const segments = getAdhesive2TailAssignableSegments();
  if (!segments.length) {
    message.info('当前工作台没有可统一修改尾号的片号');
    return;
  }
  tailAssignRows.value = segments.map((segment) => ({
    sourceProductionBatchNo: String(segment.batchNo || ''),
  }));
  const selectedSizeRules = [...new Set(segments.map((segment) => normalizeSizeRule(segment.actualSizeRule)).filter(Boolean))];
  tailAssignDefaultSizeRule.value = selectedSizeRules.length === 1 ? selectedSizeRules[0] : normalizeSizeRule(currentPlan.actualSizeRule) || '775mm';
  tailAssignVisible.value = true;
}

async function submitAdhesive2TailAssign() {
  if (tailAssignSubmitting.value || !tailAssignRows.value.length) return;
  const actualSizeRule = normalizeSizeRule(tailAssignDefaultSizeRule.value);
  if (!actualSizeRule || !currentPlan.planId || !currentPlan.planOperationId) {
    message.warning('当前任务缺少计划信息或实际尺寸');
    return;
  }
  tailAssignSubmitting.value = true;
  try {
    const affectedCount = await assignAdhesive2TailForAllSources({
      actualSizeRule,
      planId: currentPlan.planId,
      planOperationId: currentPlan.planOperationId,
    });
    tailAssignVisible.value = false;
    await Promise.all([loadReports(), loadSourceGroups()]);
    message.success(`已统一修改 ${affectedCount || tailAssignRows.value.length} 个片号尾号，可继续打印或扫码确认`);
  } catch (error: any) {
    AModal.warning({
      title: '尾号选择失败',
      content: error?.message || '请稍后重试',
    });
  } finally {
    tailAssignSubmitting.value = false;
  }
}

function getAdhesive2OneClickConfirmTargets(): Adhesive2OneClickConfirmTarget[] {
  const targets: Adhesive2OneClickConfirmTarget[] = [];
  sourceGroups.value.forEach((group) => {
    group.segments.forEach((segment) => {
      if (getAdhesive2SegmentStatus(segment) !== 'PENDING') return;
      const records = getSegmentReportRecords(segment);
      const pendingRecords = records.filter((record) => Boolean(record.id) && !isAdhesiveRecordConfirmed(record.reportStatus));
      if (pendingRecords.length) {
        pendingRecords.forEach((record) => targets.push({ group, record, segment }));
        return;
      }
      if (!records.some((record) => isAdhesiveRecordConfirmed(record.reportStatus))) {
        targets.push({ group, segment });
      }
    });
  });
  return targets;
}

function hasAdhesive2TailSelectionForOneClickTarget(target: Adhesive2OneClickConfirmTarget) {
  if (target.record) return hasAdhesive2TailSelection(target.record);
  return !!normalizeSizeRule(target.segment.actualSizeRule);
}

async function createAdhesive2OneClickConfirmReport(
  target: Adhesive2OneClickConfirmTarget,
  expectedRuntimeModelCode: string,
) {
  const matched = findSourceSegmentWithGroup(target.segment.batchNo);
  const group = matched?.group || target.group;
  const segment = matched?.segment || target.segment;
  resetReportForm();
  applyGlueBoardDefaultsToCheckItems();
  activeRecord.value = null;
  reportDialogMode.value = 'confirm';
  reportStep.value = 'PROCESS';
  const availableRange = findFirstAvailableRange(segment);
  reportForm.sourceCode = segment.batchNo;
  reportForm.sourceProductionBatchNo = segment.batchNo;
  reportForm.parentBatchNo = group.baseBatchNo;
  reportForm.reportType = getDefaultAdhesive2ReportType(segment);
  reportForm.startPosition = availableRange.start;
  reportForm.endPosition = availableRange.end;
  reportForm.processLength = availableRange.length;
  reportForm.outputLength = reportOutputLength.value;
  reportForm.glueBoardBatchNo = glueBoard.batchNo || '';
  reportForm.glueBoardMaterialCode = getActualGlueBoardMaterialCode();
  reportForm.glueBoardModel = getActualGlueBoardModel();
  reportForm.glueBoardStartPosition = Number(glueBoard.availableStartPosition || 0);
  reportForm.glueBoardUseLength = getGlueBoardUseLengthPerScan(glueBoard.stockLength);
  reportForm.productionBatchNo = getAdhesive2SegmentDisplayBatchNo(segment);
  reportForm.startTime = buildNowText();
  reportForm.endTime = buildNowText();
  const sourceScanOk = await scanReportSource();
  if (!sourceScanOk) {
    throw new Error(`${getAdhesive2SegmentDisplayBatchNo(segment)} 来源扫码失败，未生成粘胶2报工记录`);
  }
  const reportId = await submitAdhesiveReport({
    closeAfterSave: false,
    confirmAfterSave: true,
    expectedRuntimeModelCode,
    oneClickBatchConfirm: true,
    silentSuccess: true,
  });
  if (!reportId) {
    throw new Error(`${reportForm.productionBatchNo || getAdhesive2SegmentDisplayBatchNo(segment)} 保存粘胶2报工记录失败`);
  }
  return {
    confirmed: true,
    id: reportId,
    glueBoardMaterialCode: reportForm.glueBoardMaterialCode,
    productionBatchNo: reportForm.productionBatchNo,
  };
}

async function ensureStrictGlueBoardMatch() {
  try {
    await loadRuntimeProductSnapshot();
    await loadGlueBoardMapCandidates();
    await loadGlueBoardUsage({ applyCheckDefaults: false });
    const reason = glueBoardMatchReason(String(currentProductModelCode.value || ''),
      glueBoardMapCandidates.value, getActualGlueBoardMaterialCode(), getActualGlueBoardModel());
    if (reason) { message.warning(reason); return false; }
    if (!hasCurrentGlueBoardUsageForAction()) {
      showGlueBoardUsageRequiredWarning('扫码确认');
      return false;
    }
    return true;
  } catch (error: any) {
    message.warning(error?.message || '胶板匹配校验失败，请刷新后重试');
    return false;
  }
}

async function confirmAllAdhesive2ReportsOneClick() {
  if (oneClickScanConfirming.value) return;
  if (isCurrentTaskReadonly.value) {
    showReadonlyTaskWarning('一键扫码确认');
    return;
  }
  const pendingTargets = getAdhesive2OneClickConfirmTargets();
  if (!pendingTargets.length) {
    message.info('当前工作台没有待扫码确认的粘胶2片号');
    return;
  }
  await loadChangeoverInstruction();
  await loadRuntimeProductSnapshot();
  if (hasPendingChangeoverInstruction.value) {
    const instruction = currentChangeoverInstruction.value;
    AModal.warning({
      title: '换型期间禁止一键确认',
      content: `当前存在换型指令 ${instruction?.instructionNo || '-'}（${instruction?.beforeModelCode || currentPlan.modelCode || '-'} → ${instruction?.targetModelCode || '-'}）。为防止未实际完成的片号提前按新型号入账，请完成换型后再一键确认，或改为逐片扫码确认。`,
    });
    return;
  }
  let changeoverConfirmHint = '';
  if (hasExecutingChangeoverInstruction.value) {
    const instruction = currentChangeoverInstruction.value;
    changeoverConfirmHint = `当前换型指令 ${instruction?.instructionNo || '-'} 正在执行，本次将按当前实际型号确认全部待确认片号。`;
  }
  const expectedRuntimeModelCode = String(currentProductModelCode.value || '').trim();
  const expectedRuntimeMaterialCode = String(currentProductMaterialCode.value || '').trim();
  if (!expectedRuntimeModelCode) {
    message.warning('未获取到本次确认的实际生产型号，请刷新当前粘胶2计划后重试');
    return;
  }
  const targetsWithoutTail = pendingTargets.filter((target) => !hasAdhesive2TailSelectionForOneClickTarget(target));
  if (targetsWithoutTail.length) {
    const batchNos = targetsWithoutTail.map((target) => getAdhesive2SegmentDisplayBatchNo(target.segment) || target.segment.batchNo || '-').join('、');
    AModal.warning({
      title: '请先选择尾号',
      content: `以下 ${targetsWithoutTail.length} 个待确认片号尚未选择实际尺寸和尾号：${batchNos}`,
    });
    return;
  }
  for (const target of pendingTargets) {
    if (!(await ensureAdhesive2SampleAbnormalUnlocked(target.segment, '一键扫码确认', target.group.baseBatchNo))) {
      return;
    }
  }
  if (!(await ensureStrictGlueBoardMatch())) return;
  if (!ensureGlueBoardReadyForAction('一键扫码确认')) return;
  if (!(await ensureDailyPreparationReadyForReport())) return;
  if (!(await ensureCurrentMotherChangeoverSubmittedForReport())) return;
  if (!isChangeoverInspectionReady()) {
    showChangeoverInspectionRequiredWarning();
    return;
  }
  const glueBoardModel = String(getActualGlueBoardModel() || getSelectedGlueBoardModel() || '').trim();
  const glueBoardBatchNo = String(glueBoard.batchNo || '').trim();
  if (!glueBoard.id || !glueBoardBatchNo || !glueBoardModel) {
    message.warning('请先完成胶板领用后再一键扫码确认');
    return;
  }
  const mismatchedTargets = pendingTargets.filter(({ record }) => record?.id && (
    String(record.modelCode || '').trim().toUpperCase() !== expectedRuntimeModelCode.toUpperCase()
    || Number(record.glueBoardUsageId) !== Number(glueBoard.id)
    || glueBoardMatchReason(expectedRuntimeModelCode, glueBoardMapCandidates.value,
      record.glueBoardMaterialCode, record.glueBoardModel)));
  if (mismatchedTargets.length) {
    AModal.warning({ title: '用料不匹配，未启动一键确认', content: mismatchedTargets
      .map(({ record }) => record?.productionBatchNo || '-').join('、') + '：请先核实未确认报工的型号及胶板领用。' });
    return;
  }
  const autoCreateCount = pendingTargets.filter((target) => !target.record?.id).length;
  AModal.confirm({
    cancelText: '取消',
    content: `${changeoverConfirmHint}本次确认入账型号：${expectedRuntimeModelCode}${expectedRuntimeMaterialCode ? `；产品料号：${expectedRuntimeMaterialCode}` : ''}。将对全部 ${pendingTargets.length} 个待确认片号逐片调用现有报工扫码确认，其中 ${autoCreateCount} 片会先按当前单片报工规则生成记录。确认后等同逐片扫码入账。确定继续吗？`,
    okText: '一键扫码确认',
    title: '粘胶2一键扫码确认',
    async onOk() {
      if (!(await ensureStrictGlueBoardMatch())) return;
      if (getActualGlueBoardModel() !== glueBoardModel || glueBoard.batchNo !== glueBoardBatchNo
        || String(currentProductModelCode.value || '') !== expectedRuntimeModelCode) {
        message.warning('实际型号或上机胶板已变化，请重新发起一键确认');
        return;
      }
      oneClickScanConfirming.value = true;
      let confirmedCount = 0;
      let createdReportCount = 0;
      try {
        for (const target of pendingTargets) {
          let reportId = Number(target.record?.id || 0);
          let productionBatchNo = target.record?.productionBatchNo || getAdhesive2SegmentDisplayBatchNo(target.segment);
          let glueBoardMaterialCode = target.record?.glueBoardMaterialCode;
          let createdAndConfirmed = false;
          if (!reportId) {
            const created = await createAdhesive2OneClickConfirmReport(
              target,
              expectedRuntimeModelCode,
            );
            reportId = created.id;
            productionBatchNo = created.productionBatchNo;
            glueBoardMaterialCode = created.glueBoardMaterialCode;
            createdAndConfirmed = created.confirmed;
            createdReportCount += 1;
          }
          if (!createdAndConfirmed) {
            await confirmAdhesiveConsoleReport({
              confirmerName: currentUserName.value || 'admin',
              confirmerTime: buildNowText(),
              expectedRuntimeModelCode,
              glueBoardBatchNo,
              glueBoardMaterialCode: getActualGlueBoardMaterialCode() || glueBoardMaterialCode,
              glueBoardModel,
              glueBoardUsageId: glueBoard.id,
              id: reportId,
              oneClickBatchConfirm: true,
              scannedBatchNo: productionBatchNo || '',
            });
          }
          confirmedCount += 1;
          await refreshChangeoverAfterReportConfirmation();
        }
        await Promise.all([loadReports(), loadSourceGroups(), loadGlueBoardUsage()]);
        message.success(`粘胶2已完成 ${confirmedCount} 片一键扫码确认${createdReportCount ? `，其中 ${createdReportCount} 片已自动生成报工记录` : ''}`);
      } catch (error: any) {
        await Promise.all([loadReports(), loadSourceGroups(), loadGlueBoardUsage()]);
        AModal.warning({
          title: '一键扫码确认未全部完成',
          content: `已完成 ${confirmedCount} 片，${error?.message || '其余片号请检查报工条件后重试'}`,
        });
      } finally {
        oneClickScanConfirming.value = false;
      }
    },
  });
}

function isPrintableTransferReport(record?: MesHcAdhesiveConsoleApi.ReportItem | null): record is MesHcAdhesiveConsoleApi.ReportItem {
  return Boolean(record?.id && record.productionBatchNo);
}

function isPrintableTransferSegment(segment?: SourceSegment | null) {
  return Boolean(String(segment?.batchNo || '').trim());
}

function getTransferReportKey(record?: MesHcAdhesiveConsoleApi.ReportItem | null) {
  return record?.id == null ? '' : String(record.id);
}

function getTransferPrintTargetKey(segment?: SourceSegment | null) {
  return segment?.batchNo ? `SOURCE:${String(segment.batchNo).trim().toUpperCase()}` : '';
}

function isTransferSegmentSelected(segment: SourceSegment) {
  const key = getTransferPrintTargetKey(segment);
  return !!key && selectedTransferReportIds.value.includes(key);
}

function applyReportRecordToForm(record?: MesHcAdhesiveConsoleApi.ReportItem) {
  if (!record) return;
  const extra = parseRecordExtra(record) as Record<string, any>;
  Object.assign(reportForm, {
    aqcStatus: record.aqcStatus || '',
    aqcTaskId: record.aqcTaskId,
    defectCode: record.defectCode || '',
    endPosition: Number(record.endPosition || 0),
    endTime: record.endTime || '',
    glueBoardBatchNo: record.glueBoardBatchNo || glueBoard.batchNo,
    glueBoardMaterialCode: record.glueBoardMaterialCode || extra.glueBoardMaterialCode || getActualGlueBoardMaterialCode(),
    glueBoardModel: record.glueBoardModel || extra.glueBoardModel || getSelectedGlueBoardModel(),
    glueBoardStartPosition: Number(record.glueBoardStartPosition || glueBoard.availableStartPosition || 0),
    glueBoardUsageId: record.glueBoardUsageId,
    glueBoardUseLength: Number(record.glueBoardUseLength || record.inputLength || 0),
    lossLength: Number(record.lossLength || 0),
    napSampleLength: Number(record.napSampleLength || 0),
    outputLength: Number(record.outputLength || 0),
    parentBatchNo: record.parentProductionBatchNo || record.sourceBatchNo || reportForm.parentBatchNo,
    processLength: Number(record.inputLength || record.outputLength || 0),
    productionBatchNo: getAdhesive2TailBatchNo(record.sourceProductionBatchNo || record.productionBatchNo || reportForm.sourceProductionBatchNo, record.actualSizeRule),
    preProcessSelfCheckAbnormal: isPreProcessSelfCheckAbnormal(extra),
    remark: record.remark || '',
    reportDate: record.reportDate || reportForm.reportDate,
    reportType: isAdhesive2ReportType(extra.reportType) ? extra.reportType : reportForm.reportType,
    selfCheck: record.selfCheck || 'OK',
    sourceCode: record.sourceProductionBatchNo || reportForm.sourceCode,
    sourceGrindingSecondDetailId: record.sourceGrindingSecondDetailId || reportForm.sourceGrindingSecondDetailId,
    sourceProductionBatchNo: record.sourceProductionBatchNo || reportForm.sourceProductionBatchNo,
    startPosition: Number(record.startPosition || 0),
    startTime: record.startTime || '',
  });
  applyReportCheckItems(record.checkItems);
  resetVisualInspectionItems(parseVisualItemsFromRecord(record));
}

async function assignIntermediateSlice(group: SourceGroup, segment: SourceSegment, samplePosition: string) {
  if (isCurrentTaskReadonly.value || isSourceGroupCompleted(group) || isSegmentCompleted(segment)) {
    showReadonlyTaskWarning('新建中间品记录');
    return;
  }
  if (!currentPlan.planId || !currentPlan.planOperationId) {
    message.warning('请先扫描或选择粘胶2计划');
    return;
  }
  const targetBatchNo = normalizeAdhesive2MiddleBatchNo(group.baseBatchNo || segment.batchNo);
  selectedIntermediateRecord.value = null;
  intermediateDetailVisible.value = true;
  if (!intermediateDetails.value.length) {
    await loadIntermediateRecord({
      batchNo: targetBatchNo,
      recordDate: normalizeIntermediateRecordDateTime(intermediateForm.recordDate),
    } as MesHcAdhesiveConsoleApi.IntermediateRecord);
  }
  const position = adhesive2IntermediatePositions.find((item) => item.value === samplePosition);
  if (!position) return;
  const report = findReportBySegment(segment);
  const sliceField = getIntermediateSliceField(samplePosition);
  if (sliceField) {
    (intermediateForm as Record<string, any>)[sliceField] = segment.batchNo;
  }
  if (samplePosition !== 'FIRST') {
    const target = intermediateDetails.value.find((row) => row.samplePosition === samplePosition);
    const nextRow: MesHcAdhesiveConsoleApi.IntermediateDetail = {
      ...(target || {}),
      pressSlotReportId: report?.id,
      samplePosition,
      samplePositionName: position.label,
      sliceBatchNo: segment.batchNo,
      sourceSlittingSliceId: segment.grindingSecondDetailId,
      sortNo: adhesive2MeasuredPositions.findIndex((item) => item.value === samplePosition) + 1,
    };
    if (target) {
      Object.assign(target, nextRow);
    } else {
      intermediateDetails.value.push(nextRow);
    }
  }
  Object.assign(intermediateForm, {
    batchNo: targetBatchNo,
    materialCode: group.materialCode || currentProductMaterialCode.value || intermediateForm.materialCode || '',
    modelCode: group.modelCode || currentProductModelCode.value || intermediateForm.modelCode || '',
    planId: currentPlan.planId || 0,
    planNo: currentPlan.planNo || '',
    planOperationId: currentPlan.planOperationId || 0,
    recordDate: normalizeIntermediateRecordDateTime(intermediateForm.recordDate),
    recorderName: intermediateForm.recorderName || currentUserName.value || '',
  });
  syncIntermediateCountersFromReports();
  message.success('已带入粘胶2中间品片号，请在中间品记录表中保存');
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
  clearPlanScanTimer();
  if (suppressNextPlanScanSchedule) {
    suppressNextPlanScanSchedule = false;
    return;
  }
  const sliceBatchNo = normalizePlanScanSliceBatchNo(value) || pendingScannerSliceBatchNo;
  const planNo = syncPlanScanNo(value);
  const scannerInput = hasPlanScanDelimiter(value) || pendingScannerPlanNo === planNo;
  if (!planNo || (!scannerInput && !isPlanNoReadyForAutoScan(planNo)) || (planNo === lastAutoScannedPlanNo.value && !sliceBatchNo)) return;
  planScanTimer = setTimeout(() => {
    if (planNo !== normalizePlanScanNo(scanPlanNo.value)) {
      if (pendingScannerPlanNo === planNo) pendingScannerPlanNo = '';
      if (sliceBatchNo && pendingScannerSliceBatchNo === sliceBatchNo) pendingScannerSliceBatchNo = '';
      return;
    }
    if (pendingScannerPlanNo === planNo) pendingScannerPlanNo = '';
    void consumePlanScan(true);
  }, 260);
}

async function loadTaskList() {
  taskListLoading.value = true;
  try {
    const equipment = getEffectiveBoardEquipment();
    taskRows.value = normalizeRows(
      await getAdhesiveConsoleTaskList({
        equipmentCode: equipment.id ? undefined : equipment.code || undefined,
        equipmentId: equipment.id,
        taskStatus: taskListFilters.status as MesHcAdhesiveConsoleApi.TaskQuery['taskStatus'],
      }),
    );
    ensureBoardEquipmentFromTasks(taskRows.value);
    taskListPage.value = 1;
  } finally {
    taskListLoading.value = false;
  }
}

async function loadEquipmentSelectRows() {
  equipmentSelectLoading.value = true;
  try {
    const keyword = equipmentSelectKeyword.value.trim();
    const workCenterId = currentPlan.workCenterId || boardEquipment.workCenterId;
    const workCenterName = currentPlan.workCenterName || boardEquipment.workCenterName;
    if (!workCenterId && !workCenterName && taskRows.value.length === 0) {
      await loadTaskList();
    }
    const effectiveWorkCenterId = currentPlan.workCenterId || boardEquipment.workCenterId;
    const effectiveWorkCenterName = currentPlan.workCenterName || boardEquipment.workCenterName;
    if (!effectiveWorkCenterId && !effectiveWorkCenterName) {
      equipmentSelectRows.value = [];
      message.warning('当前粘胶2任务未带出工作中心，不能展示全厂设备；请先维护粘胶2工序工作中心或机台。');
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
    rows = rows.filter((row) => ['BLACK_PAD', 'COMMON', 'WHITE_PAD'].includes(normalizeAdhesive2PadTypeCode(row.applicablePadType)));
    if (!rows.length) {
      message.warning('当前粘胶2工作中心下未找到可用设备，请检查设备台账的工作中心和启用状态。');
    }
    equipmentSelectRows.value = rows;
  } finally {
    equipmentSelectLoading.value = false;
  }
}

async function openEquipmentSelect() {
  equipmentSelectVisible.value = true;
  if (!currentPlan.workCenterId && !currentPlan.workCenterName && !boardEquipment.workCenterId && !boardEquipment.workCenterName && taskRows.value.length === 0) {
    await loadTaskList();
  }
  await loadEquipmentSelectRows();
}

function closeEquipmentSelect() {
  equipmentSelectVisible.value = false;
  openDailyRecordAfterEquipmentSelected.value = false;
}

async function openDailyRecordList() {
  if (!selectedBoardEquipmentId.value && !selectedBoardEquipmentCode.value) {
    openDailyRecordAfterEquipmentSelected.value = true;
    message.warning('请先选择粘胶2设备，再填写清洁点检');
    await openEquipmentSelect();
    return;
  }
  await loadDailyRecords();
  dailyRecordListVisible.value = true;
}

async function selectBoardEquipment(equipment: MesHcEquipmentApi.Equipment) {
  applyBoardEquipment({
    applicablePadType: equipment.applicablePadType || '',
    applicablePadTypeName: equipment.applicablePadTypeName || '',
    code: equipment.equipmentCode || '',
    id: equipment.id,
    name: equipment.equipmentName || '',
    workCenterId: equipment.workCenterId,
    workCenterName: equipment.workCenterName || '',
    workStatus: equipment.workStatus || '',
  });
  boardEquipmentManualSelected.value = true;
  resetPlan();
  await loadTaskList();
  await loadDailyRecords();
  equipmentSelectVisible.value = false;
  // message.success(`已切换粘胶2设备：${boardEquipment.code || boardEquipment.name || '-'}`);
  if (openDailyRecordAfterEquipmentSelected.value) {
    openDailyRecordAfterEquipmentSelected.value = false;
    dailyRecordListVisible.value = true;
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

async function openStartTaskSizeRule(task: MesHcAdhesiveConsoleApi.TaskItem) {
  await startTaskFromList(task);
}

async function confirmStartTaskSizeRule() {
  if (!pendingStartTask.value) return;
  if (!sizeRuleForm.glueBoardModel) {
    message.warning('请选择胶板型号');
    return;
  }
  sizeRuleVisible.value = false;
  await startTaskFromList(pendingStartTask.value, sizeRuleForm.actualSizeRule, sizeRuleForm.glueBoardModel);
  pendingStartTask.value = null;
}

async function startAdhesive2WorkOrderIfNeeded(task: MesHcAdhesiveConsoleApi.TaskItem | Record<string, any>, taskStatus = normalizeWorkOrderStatus(task.status)) {
  if (taskStatus === 'FINISHED') return true;
  if (taskStatus === 'PAUSED' || taskStatus === 'CANCELLED') {
    const reason = buildWorkOrderBlockedReason(task.status);
    adhesive2WorkbenchBlockedReason.value = reason;
    return false;
  }
  if (!(await ensureAdhesive2TaskEquipmentForBoard(task))) return false;
  if (taskStatus === 'RUNNING') return true;
  if (!(await ensureAdhesive2SampleAbnormalUnlocked(undefined, '开工', resolveMotherBatchNo(task as Record<string, any>)))) {
    return false;
  }
  const equipment = getEffectiveBoardEquipment(task);
  if (!equipment.id && !equipment.code) {
    message.warning('当前粘胶2工序未绑定设备，请先选择粘胶2机台');
    await openEquipmentSelect();
    return false;
  }
  try {
    await startAdhesiveConsoleWorkOrder({
      equipmentCode: equipment.code,
      equipmentId: equipment.id,
      equipmentName: equipment.name,
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
  syncWorkOrderBlockedReason();
  syncCurrentPlanEquipmentFromBoard(equipment);
  return true;
}

async function startTaskFromList(task: MesHcAdhesiveConsoleApi.TaskItem, actualSizeRule?: string, glueBoardModel?: string) {
  if (!task?.planOperationId) return;
  const taskStatus = normalizeWorkOrderStatus(task.status);
  if (taskStatus !== 'FINISHED' && !(await ensureAdhesive2TaskEquipmentForBoard(task))) return;
  if (taskStatus === 'FINISHED' && !selectedBoardEquipmentId.value && !selectedBoardEquipmentCode.value && (task.equipmentId || task.equipmentCode)) {
    applyBoardEquipmentFromTask(task);
  }
  const taskSourceBatchNo = resolveMotherBatchNo(task as Record<string, any>);
  if (taskStatus !== 'FINISHED' && !(await ensureAdhesive2SampleAbnormalUnlocked(undefined, '加载计划', taskSourceBatchNo))) {
    return;
  }
  setManualTaskPlanScanNo(task.planNo);
  currentPlan.actualSizeRule = actualSizeRule || resolveActualSizeRule(task);
  applyTask(task);
  await loadLatestPlanInstruction();
  await loadChangeoverInstruction();
  const mapCandidates = await fetchGlueBoardMapCandidatesForProductModel(currentProductModelCode.value || task.modelCode);
  currentPlan.actualSizeRule = actualSizeRule || currentPlan.actualSizeRule || resolveActualSizeRule(task);
  glueBoardMapCandidates.value = mapCandidates;
  applySelectedGlueBoardModel(glueBoardModel || modelOverride.glueBoardModel || task.glueBoardModel || currentPlan.glueBoardModel);
  applyPreferredGlueBoardModelFromMap();
  await loadCheckTemplate();
  await loadGlueBoardUsage();
  lastAutoScannedPlanNo.value = task.planNo || scanPlanNo.value.trim();
  syncWorkOrderBlockedReason(task.status);
  if (taskStatus === 'PAUSED' || taskStatus === 'CANCELLED') {
    taskListVisible.value = false;
    return;
  }
  if (taskStatus !== 'FINISHED') {
    const equipment = getEffectiveBoardEquipment(task);
    if (!equipment.id && !equipment.code) {
      message.warning('当前粘胶2工序未绑定设备，请先选择粘胶2机台');
      await openEquipmentSelect();
      return;
    }
    if (!(await ensureCleaningMaintenanceReadyForStart(task, equipment))) return;
  }
  if (!(await startAdhesive2WorkOrderIfNeeded(task, taskStatus))) return;
  await Promise.all([loadDailyRecords(), loadReports()]);
  await loadSourceGroups(taskSourceBatchNo);
  await loadChangeoverInspections();
  await loadAdhesive2FaiSummary();
  await loadAdhesive2IntermediateRecords();
  taskListVisible.value = false;
  // message.success('已开工加载粘胶2计划与可加工来源');
}

function handleWorkOrderCompleteTodo() {
  if (!currentPlan.planId || !currentPlan.planOperationId) {
    message.warning('请先扫码或从待加工列表选择粘胶2工单');
    return;
  }
  if (isWorkOrderFinished.value) {
    message.warning('当前粘胶2工单已完工');
    return;
  }
  if (adhesive2WorkbenchBlockedReason.value) {
    AModal.warning({
      okText: '知道了',
      title: '粘胶2工作台已阻断',
      content: adhesive2WorkbenchBlockedReason.value,
    });
    return;
  }
  if (!isWorkOrderRunning.value) {
    message.warning('当前粘胶2工单未开工，不能执行工单完工');
    return;
  }
  AModal.confirm({
    okText: '确认完工',
    title: '确认粘胶2工单完工',
    content: '执行后会标识当前粘胶2工单全部完成，后续不能再新增粘胶2报工记录。请确认所有报工记录已打印并扫码确认。',
    async onOk() {
      await completeAdhesiveConsoleWorkOrder({
        confirmerName: currentUserName.value || undefined,
        confirmerTime: buildNowText(),
        endTime: buildNowText(),
        planId: currentPlan.planId!,
        planOperationId: currentPlan.planOperationId!,
        recorderName: currentUserName.value || undefined,
        recorderTime: buildNowText(),
        remark: '粘胶2看板工单完工',
        reportDate: dayjs().format('YYYY-MM-DD'),
      });
      currentPlan.status = 'FINISHED';
      currentPlan.endTime = buildNowText();
      syncWorkOrderBlockedReason();
      await Promise.all([loadReports(), loadSourceGroups(), loadTaskList(), loadAdhesive2FaiSummary()]);
      message.success('粘胶2工单已完工');
    },
  });
}

async function refreshActiveSourceGroup(group: SourceGroup) {
  await loadReports();
  await loadSourceGroups();
  return findSourceGroup(group.baseBatchNo) || group;
}

async function handleSegmentComplete() {
  if (!currentPlan.planId || !currentPlan.planOperationId) {
    message.warning('请先扫码或从待加工列表选择粘胶2工单');
    return;
  }
  let group = getActiveSourceGroup();
  if (!group) {
    message.warning('请先选择或扫码当前分段批号');
    return;
  }
  group = await refreshActiveSourceGroup(group);
  const records = getSourceGroupReportRecords(group);
  if (!records.length) {
    message.warning(`当前分段 ${group.baseBatchNo} 暂无粘胶2报工记录，不能本段完成`);
    return;
  }
  const unconfirmedRecords = records.filter((record) => !isAdhesiveRecordConfirmed(record.reportStatus));
  if (unconfirmedRecords.length) {
    selectedReportRecordKeys.value = unconfirmedRecords.map((record) => record.id).filter((id): id is number => !!id);
    message.warning(`当前分段 ${group.baseBatchNo} 存在 ${unconfirmedRecords.length} 条未扫码确认的报工记录，请先扫码确认`);
    reportRecordListVisible.value = true;
    return;
  }
  pendingSegmentCompleteGroup.value = group;
  segmentCompleteAuthVisible.value = true;
  message.info(`当前分段 ${group.baseBatchNo} 共 ${records.length} 条粘胶2报工；请认证确认本段完工。`);
}

async function handleSegmentCompleteAuthSuccess(userInfo: any) {
  const pendingGroup = pendingSegmentCompleteGroup.value;
  if (!pendingGroup) {
    message.warning('未找到待确认的分段，请重新点击本段完成');
    return;
  }
  try {
    const group = await refreshActiveSourceGroup(pendingGroup);
    const records = getSourceGroupReportRecords(group);
    if (!records.length) {
      message.warning(`当前分段 ${group.baseBatchNo} 暂无粘胶2报工记录，不能本段完成`);
      return;
    }
    const unconfirmedRecords = records.filter((record) => !isAdhesiveRecordConfirmed(record.reportStatus));
    if (unconfirmedRecords.length) {
      selectedReportRecordKeys.value = unconfirmedRecords.map((record) => record.id).filter((id): id is number => !!id);
      message.warning(`当前分段 ${group.baseBatchNo} 存在 ${unconfirmedRecords.length} 条未扫码确认的报工记录，请先扫码确认`);
      reportRecordListVisible.value = true;
      return;
    }
    const now = buildNowText();
    const confirmerName = userInfo?.empName || userInfo?.nickname || userInfo?.username || currentUserName.value || undefined;
    await completeAdhesive2ConsoleSegment({
      confirmerName,
      confirmerTime: now,
      endTime: now,
      planId: currentPlan.planId!,
      planOperationId: currentPlan.planOperationId!,
      recorderName: confirmerName,
      recorderTime: now,
      remark: `粘胶2看板本段完成：${group.baseBatchNo}`,
      reportDate: dayjs().format('YYYY-MM-DD'),
      sourceProductionBatchNo: group.baseBatchNo,
    });
    await Promise.all([loadReports(), loadSourceGroups(), loadTaskList(), loadAdhesive2FaiSummary()]);
    markTaskListSourceGroupCompleted(group);
    message.success(`当前分段 ${group.baseBatchNo} 已完成，认证人：${confirmerName || '-'}`);
  } catch (error) {
    message.error(getReportRequestErrorMessage(error, '本段完成失败，请稍后重试'));
  } finally {
    pendingSegmentCompleteGroup.value = null;
    segmentCompleteAuthVisible.value = false;
  }
}

function handleSegmentCompleteAuthCancel() {
  pendingSegmentCompleteGroup.value = null;
  segmentCompleteAuthVisible.value = false;
}

function parseRecordExtra(record: MesHcAdhesiveConsoleApi.ReportItem | Record<string, any>) {
  try {
    const extra = record?.extraJson ? JSON.parse(record.extraJson) : {};
    return extra && typeof extra === 'object' ? extra : {};
  } catch {
    return {};
  }
}

function isPressSlotProcessCheckSource(source?: MesHcAdhesiveConsoleApi.SourceItem | Record<string, any> | null) {
  const row = (source || {}) as Record<string, any>;
  const extra = parseRecordExtra(row);
  const reportType = String(extra.reportType || extra.inspectionScene || extra.inspectionSampleCategory || '')
    .trim()
    .toUpperCase();
  const sourceMenuCode = String(row.sourceMenuCode || '')
    .trim()
    .toUpperCase();
  return sourceMenuCode.includes('PRESS_SLOT') && reportType === 'PROCESS_CHECK';
}

function isSourceCoaSubmitted(source?: MesHcAdhesiveConsoleApi.SourceItem | Record<string, any> | null) {
  const row = (source || {}) as Record<string, any>;
  const extra = parseRecordExtra(row);
  const downstreamStatus = String(row.downstreamStatus || extra.downstreamStatus || '')
    .trim()
    .toUpperCase();
  return ['COA_INSPECTION', 'COA_SUBMITTED', 'COA_SENT'].includes(downstreamStatus) || Boolean(extra.coaFaiId || extra.coaFaiNo || extra.coaInspectionSubmitted);
}

function buildDefaultVisualItems(): VisualItem[] {
  return adhesive2VisualItemNames.map((itemName) => ({
    itemName,
    remark: '',
    result: 'OK',
  }));
}

function normalizeVisualResult(value: unknown): 'NG' | 'OK' {
  const text = String(value ?? '')
    .trim()
    .toUpperCase();
  if (['NG', 'N', 'FALSE', '0', 'ABNORMAL', 'FAIL', 'FAILED', 'UNQUALIFIED', '异常', '不合格'].includes(text)) return 'NG';
  return 'OK';
}

function isNgCodeLike(value: unknown) {
  const text = String(value ?? '')
    .trim()
    .toUpperCase();
  return (
    normalizeVisualResult(text) === 'NG' ||
    text.startsWith('NG') ||
    text.includes('_NG') ||
    text.includes('NG_') ||
    text.includes(' NG') ||
    text.includes('NG ') ||
    text.includes('NG片') ||
    text.includes('NG异常') ||
    text.includes('不合格') ||
    text.includes('异常')
  );
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

function resolveNgProcessName(record?: MesHcAdhesiveConsoleApi.ReportItem | Record<string, any> | null, fallback = '前工序') {
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

function getNgReasonText(record?: MesHcAdhesiveConsoleApi.ReportItem | Record<string, any> | null) {
  const extra = parseRecordExtra(record || {}) as Record<string, any>;
  const source = (record || {}) as Record<string, any>;
  const visualItem = parseVisualItemsFromRecord(source).find((item) => isVisualItemActive(item));
  return String(
    source.sourceNgReason ||
      extra.sourceNgReason ||
      source.qualityLockReason ||
      extra.qualityLockReason ||
      source.defectCode ||
      extra.defectCode ||
      source.productQualityStatus ||
      extra.productQualityStatus ||
      source.inspectionResult ||
      extra.inspectionResult ||
      visualItem?.remark ||
      visualItem?.itemName ||
      '',
  ).trim();
}

function hasProcessNgFromRecord(record?: MesHcAdhesiveConsoleApi.ReportItem | Record<string, any> | null) {
  if (!record) return false;
  if (hasVisualIssueFromRecord(record as Record<string, any>)) return true;
  const extra = parseRecordExtra(record) as Record<string, any>;
  return [
    (record as Record<string, any>).productQualityStatus,
    (record as Record<string, any>).qualityLockReason,
    (record as Record<string, any>).inspectionResult,
    extra.productQualityStatus,
    extra.qualityLockReason,
    extra.inspectionResult,
  ].some(isNgCodeLike);
}

function getSourceProcessNgText(record?: MesHcAdhesiveConsoleApi.ReportItem | Record<string, any> | null, fallback = '前工序') {
  if (!hasProcessNgFromRecord(record)) return '';
  return formatProcessNgText(resolveNgProcessName(record, fallback));
}

function normalizeVisualItems(source: unknown): VisualItem[] {
  let rows: any[] = [];
  if (typeof source === 'string') {
    try {
      const parsed = JSON.parse(source || '[]');
      rows = Array.isArray(parsed) ? parsed : parsed?.visualItems || parsed?.items || parsed?.details || parsed?.list || [];
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
    }))
    .filter((item) => !!item.itemName);
  return adhesive2VisualItemNames.map((itemName) => {
    const matched = normalized.find((item) => item.itemName === itemName);
    return {
      itemName,
      remark: matched?.remark || '',
      result: matched && (matched.result === 'NG' || String(matched.remark || '').trim()) ? 'NG' : 'OK',
    };
  });
}

function parseVisualItemsFromRecord(record?: MesHcAdhesiveConsoleApi.ReportItem | Record<string, any>) {
  const extra = parseRecordExtra(record || {}) as Record<string, any>;
  const source = extra.visualItems || extra.visualInspectionItems || extra.visualResult || extra.visualResults || (record as Record<string, any> | undefined)?.visualResultJson || [];
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

function hasVisualIssueFromRecord(record?: Record<string, any> | null) {
  if (!record) return false;
  if (normalizeVisualResult(record.selfCheck) === 'NG') return true;
  if (String(record.defectCode || '').trim()) return true;
  const extra = parseRecordExtra(record) as Record<string, any>;
  if (normalizeVisualResult(extra.visualInspectionResult || extra.selfCheck) === 'NG') return true;
  if (String(extra.defectCode || '').trim()) return true;
  return parseVisualItemsFromRecord(record).some((item) => isVisualItemActive(item));
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

function getDefaultGlueBoardModel(modelCode?: string) {
  const code = String(modelCode || '').toUpperCase();
  if (code.includes('W26P0200')) return 'W250';
  if (code.includes('W26P0300')) return 'SDK';
  return 'W220';
}

function getSelectedGlueBoardModel() {
  if (!currentPlan.planNo && !currentPlan.planOperationId && !currentPlan.glueBoardModel) return '';
  if ((glueBoard.batchNo || glueBoard.stockId || glueBoard.id) && glueBoard.model) {
    return glueBoard.model;
  }
  return currentPlan.glueBoardModel || reportForm.glueBoardModel || getDefaultGlueBoardModel(currentProductModelCode.value);
}

function getActualGlueBoardModel() {
  return String(glueBoard.model || '').trim();
}

function getActualGlueBoardMaterialCode() {
  return String(glueBoard.materialCode || '').trim();
}

function hasSelectedGlueBoardBatch() {
  return Boolean(glueBoard.batchNo || glueBoard.stockId || glueBoard.id);
}

function hasGlueBoardModelAndStockForStart() {
  return Boolean(getActualGlueBoardModel() && Number(glueBoard.stockLength || 0) > 0);
}

function hasGlueBoardInspectionRecord() {
  return Boolean(glueBoard.latestInspectionId);
}

function buildGlueBoardBasicOperationGuardReason(actionName = '操作') {
  if (!hasGlueBoardModelAndStockForStart()) {
    return `当前界面胶板型号或边库余量为空，不能${actionName}。请先在粘胶2看板选择/领用胶板批次，确认胶板型号与边库余量有值。`;
  }
  if (!String(glueBoard.batchNo || '').trim()) {
    return `当前界面胶板批号为空，不能${actionName}。请重新选择正确胶板边库批次。`;
  }
  return '';
}

function ensureGlueBoardReadyForAction(actionName = '操作') {
  const glueBoardGuardReason = buildGlueBoardBasicOperationGuardReason(actionName);
  if (!glueBoardGuardReason) return true;
  AModal.warning({
    content: glueBoardGuardReason,
    okText: '知道了',
    onOk: focusPlanScanInput,
    title: '胶板未满足操作条件',
  });
  return false;
}

function showAdhesive2PlanRequiredWarning(actionName = '操作') {
  AModal.warning({
    content: `${actionName}需要先从待加工列表开工或扫码加载当前粘胶2待加工计划。`,
    okText: '打开待加工',
    onOk: () => {
      void openTaskList();
    },
    title: '请先加载粘胶2待加工计划',
  });
}

function hasCurrentGlueBoardUsageForAction() {
  const alarm = String(glueBoard.alarm || '');
  const isMissingCurrentUsage = alarm.includes('未找到当前机台有效胶板领用记录') || alarm.includes('未找到胶板型号');
  return Boolean(!isMissingCurrentUsage && glueBoard.id && Number(glueBoard.stockId || 0) > 0 && String(glueBoard.batchNo || '').trim());
}

function showGlueBoardUsageRequiredWarning(actionName = '操作') {
  AModal.warning({
    content: `${actionName}前需要先在当前粘胶2计划下保存胶板领用记录，并确认胶板型号、批号和边库余量。`,
    okText: '去胶板领用',
    onOk: () => {
      void openGlueConsumeDialog();
    },
    title: '请先完成胶板领用',
  });
}

async function ensureAdhesive2PlanGlueBoardReadyForAction(actionName = '操作') {
  if (!currentPlan.planId || !currentPlan.planOperationId || !currentPlan.planNo) {
    showAdhesive2PlanRequiredWarning(actionName);
    return false;
  }
  await loadGlueBoardUsage({ applyCheckDefaults: false });
  if (!ensureGlueBoardReadyForAction(actionName)) return false;
  if (!hasCurrentGlueBoardUsageForAction()) {
    showGlueBoardUsageRequiredWarning(actionName);
    return false;
  }
  return true;
}

function applySelectedGlueBoardModel(model?: string) {
  const nextModel = model || getDefaultGlueBoardModel(currentProductModelCode.value);
  currentPlan.glueBoardModel = nextModel;
  reportForm.glueBoardModel = nextModel;
  reportForm.glueBoardMaterialCode = nextModel;
  applyGlueBoardDefaultsToCheckItems();
}

function isSameEquipmentIdentity(leftId?: number, leftCode?: string, rightId?: number, rightCode?: string) {
  const effectiveLeftId = Number(leftId || 0);
  const effectiveRightId = Number(rightId || 0);
  if (effectiveLeftId > 0 && effectiveRightId > 0) {
    return effectiveLeftId === effectiveRightId;
  }
  const effectiveLeftCode = String(leftCode || '')
    .trim()
    .toUpperCase();
  const effectiveRightCode = String(rightCode || '')
    .trim()
    .toUpperCase();
  return Boolean(effectiveLeftCode && effectiveRightCode && effectiveLeftCode === effectiveRightCode);
}

function clearCurrentGlueBoardUsage(preferredGlueBoardModel = '', alarm = '') {
  Object.assign(glueBoard, {
    alarm,
    aqcSampleLength: 0,
    availableStartPosition: 0,
    batchNo: '',
    id: undefined,
    inspectionSubmitTime: '',
    latestAqcTask: undefined,
    latestInspectionId: undefined,
    latestInspectionNo: '',
    latestInspectionResult: '',
    lossLength: 0,
    materialCode: '',
    model: '',
    qualityLockReason: '',
    qualityLockStartPosition: undefined,
    qualityStatus: '',
    receiveLength: 0,
    receiveStartPosition: 0,
    returnedLength: 0,
    returnedStartPosition: 0,
    stockId: undefined,
    stockLength: 0,
    todayUsedLength: 0,
  });
  reportForm.glueBoardBatchNo = '';
  reportForm.glueBoardMaterialCode = preferredGlueBoardModel;
  glueConsumeForm.batchNo = '';
  glueConsumeForm.materialScanCode = '';
  glueConsumeForm.stockAvailableLength = 0;
  glueConsumeForm.stockId = undefined;
}

function clearSelectedGlueBoardForModelChange(nextGlueBoardModel: string) {
  if (!hasSelectedGlueBoardBatch() || isSameText(glueBoard.model, nextGlueBoardModel)) return;
  clearCurrentGlueBoardUsage(nextGlueBoardModel);
}

function applyPreferredGlueBoardModelFromMap() {
  const models = glueBoardMapCandidateModels.value;
  if (hasExecutingChangeoverInstruction.value) {
    const targetGlueBoardModel = models[0] || '';
    clearSelectedGlueBoardForModelChange(targetGlueBoardModel);
    modelOverride.glueBoardModel = targetGlueBoardModel;
    if (targetGlueBoardModel) {
      applySelectedGlueBoardModel(targetGlueBoardModel);
    } else {
      currentPlan.glueBoardModel = '';
      reportForm.glueBoardModel = '';
      reportForm.glueBoardMaterialCode = '';
      glueBoard.alarm = `目标型号 ${currentProductModelCode.value || '-'} 未配置可用胶板型号，请先维护映射后再领用。`;
    }
    return;
  }
  if (models.length === 0) return;
  const selected = String(getSelectedGlueBoardModel() || '')
    .trim()
    .toUpperCase();
  if (selected && models.some((model) => model.toUpperCase() === selected)) return;
  applySelectedGlueBoardModel(models[0]);
}

function openChangeoverInstructionIssueDialog() {
  if (isCurrentTaskReadonly.value) {
    showReadonlyTaskWarning('下达换型指令');
    return;
  }
  if (!currentPlan.planId || !currentPlan.planOperationId) {
    message.warning('请先扫描或选择粘胶2计划');
    return;
  }
  if (hasPendingChangeoverInstruction.value || hasExecutingChangeoverInstruction.value) {
    message.warning('当前工序已有待执行或执行中的换型指令，请先处理后再下达');
    return;
  }
  changeoverInstructionIssueVisible.value = true;
}

function authenticateChangeoverInstructionIssue() {
  changeoverInstructionIssueAuthResolver?.(false);
  changeoverInstructionIssueAuthVisible.value = true;
  return new Promise<false | Partial<MesHcProductionInstructionApi.Instruction>>((resolve) => {
    changeoverInstructionIssueAuthResolver = resolve;
  });
}

function handleChangeoverInstructionIssueAuthCancel() {
  changeoverInstructionIssueAuthVisible.value = false;
  changeoverInstructionIssueAuthResolver?.(false);
  changeoverInstructionIssueAuthResolver = undefined;
}

function handleChangeoverInstructionIssueAuthSuccess(authPayload: any) {
  const issuerId = Number(authPayload?.userId || 0);
  changeoverInstructionIssueAuthVisible.value = false;
  changeoverInstructionIssueAuthResolver?.({
    issuerId: issuerId > 0 ? issuerId : undefined,
    issuerName: resolveAuthenticatedUserName(authPayload, currentUserName.value),
  });
  changeoverInstructionIssueAuthResolver = undefined;
}

async function handleChangeoverInstructionIssued() {
  await Promise.all([loadChangeoverInstruction(), refreshInstructionUnreadCount()]);
}

async function refreshChangeoverInspectionContext() {
  const contextKey = getChangeoverPlanContextKey();
  const instructionId = getCurrentChangeoverInstructionId();
  await Promise.all([loadCheckTemplate(), loadChangeoverInspections()]);
  if (contextKey !== getChangeoverPlanContextKey() || instructionId !== getCurrentChangeoverInstructionId()) return;
  if (changeoverVisible.value && !matchesCurrentChangeoverInstruction(changeoverForm)) {
    changeoverVisible.value = false;
  }
  if (!changeoverVisible.value) resetChangeoverForm(findPendingChangeoverInspection());
}

async function prepareGlueBoardForExecutingChangeover() {
  const contextKey = getChangeoverPlanContextKey();
  const instructionId = getCurrentChangeoverInstructionId();
  await loadGlueBoardMapCandidates();
  if (contextKey !== getChangeoverPlanContextKey() || instructionId !== getCurrentChangeoverInstructionId()) return;
  applyPreferredGlueBoardModelFromMap();
  await refreshChangeoverInspectionContext();
  if (contextKey !== getChangeoverPlanContextKey() || instructionId !== getCurrentChangeoverInstructionId()) return;
  await loadGlueBoardUsage({ applyCheckDefaults: false });
}

function openChangeoverInstructionDialog() {
  if (isCurrentTaskReadonly.value) {
    showReadonlyTaskWarning('执行换型指令');
    return;
  }
  const instruction = currentChangeoverInstruction.value;
  if (!instruction?.id || instruction.instructionType !== 'CHANGEOVER') {
    message.warning('当前计划没有待执行的换型指令');
    return;
  }
  if (String(instruction.executeStatus || '').toUpperCase() === 'COMPLETED') {
    message.info('当前换型指令已完成');
    return;
  }
  changeoverInstructionForm.remark = '';
  changeoverInstructionVisible.value = true;
}

function requestStartChangeoverInstructionAuth() {
  const instruction = currentChangeoverInstruction.value;
  if (!instruction?.id) {
    message.warning('当前计划没有待执行的换型指令');
    return;
  }
  changeoverInstructionExecuteAuthVisible.value = true;
}

function handleChangeoverInstructionExecuteAuthCancel() {
  changeoverInstructionExecuteAuthVisible.value = false;
}

async function handleChangeoverInstructionExecuteAuthSuccess(authPayload: any) {
  const instruction = currentChangeoverInstruction.value;
  changeoverInstructionExecuteAuthVisible.value = false;
  if (!instruction?.id) {
    message.warning('当前计划没有待执行的换型指令');
    return;
  }
  const executeUserId = Number(authPayload?.userId || 0);
  const executeUserName = resolveAuthenticatedUserName(authPayload, currentUserName.value);
  if (!executeUserName) {
    message.warning('未识别到认证执行人，请重新认证');
    return;
  }
  changeoverInstructionStarting.value = true;
  const contextKey = getChangeoverPlanContextKey();
  ++changeoverInstructionLoadSeq;
  ++runtimeProductSnapshotLoadSeq;
  try {
    const updated = await startChangeoverInstruction({
      executeUserId: executeUserId > 0 ? executeUserId : undefined,
      executeUserName,
      id: instruction.id,
      remark: changeoverInstructionForm.remark || undefined,
    });
    if (contextKey !== getChangeoverPlanContextKey()) return;
    ++changeoverInstructionLoadSeq;
    ++runtimeProductSnapshotLoadSeq;
    currentChangeoverInstruction.value = updated;
    applyRuntimeChangeoverInstruction(updated);
    await loadRuntimeProductSnapshot();
    if (contextKey !== getChangeoverPlanContextKey()) return;
    await prepareGlueBoardForExecutingChangeover();
    if (contextKey !== getChangeoverPlanContextKey()) return;
    changeoverInstructionVisible.value = false;
    message.success('换型指令已开始执行，请按目标型号重新领用并送检胶板');
  } finally {
    changeoverInstructionStarting.value = false;
  }
}

async function refreshChangeoverAfterReportConfirmation() {
  const contextKey = getChangeoverPlanContextKey();
  const previousInstruction = hasExecutingChangeoverInstruction.value ? currentChangeoverInstruction.value : null;
  // 换型记片由后端与报工确认同事务完成，前端只重新查询执行状态。
  const instruction = await loadChangeoverInstruction();
  if (instruction === undefined || contextKey !== getChangeoverPlanContextKey()) return;
  if (previousInstruction?.id && previousInstruction.id !== currentChangeoverInstruction.value?.id && previousInstruction.id === getCurrentChangeoverInstructionId()) {
    message.success(`换型指令 ${previousInstruction.instructionNo || ''} 已完成，当前型号保持为 ${currentProductModelCode.value || '-'}`);
  }
}

function getRecordPrintMeta(record: MesHcAdhesiveConsoleApi.ReportItem | Record<string, any>) {
  const extra = parseRecordExtra(record) as Record<string, any>;
  const status = String(extra.printStatus || '').toUpperCase() === 'PRINTED' || extra.printStatus === '已打印' ? '已打印' : '未打印';
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

function isAdhesiveRecordConfirmed(status?: string) {
  const normalized = String(status || '').toUpperCase();
  return normalized === 'CONFIRMED' || normalized === 'SUBMITTED';
}

function isAdhesive2ReportDeductible(record?: MesHcAdhesiveConsoleApi.ReportItem | Record<string, any> | null) {
  const row = (record || {}) as Record<string, any>;
  return isAdhesiveRecordConfirmed(String(row.reportStatus || row.recordStatus || '')) || hasAdhesive2ReportCurrentNg(row);
}

function hasAdhesive2ReportCurrentNg(record?: MesHcAdhesiveConsoleApi.ReportItem | Record<string, any> | null) {
  const row = (record || {}) as Record<string, any>;
  const extra = parseRecordExtra(row);
  const selfCheck = String(row.selfCheck || extra.selfCheck || '')
    .trim()
    .toUpperCase();
  const productQualityStatus = String(row.productQualityStatus || extra.productQualityStatus || '')
    .trim()
    .toUpperCase();
  const feedbackResult = String(extra.feedbackResult || extra.inspectionResult || '')
    .trim()
    .toUpperCase();
  return (
    hasVisualIssueFromRecord(row) ||
    ['NG', 'ABNORMAL', 'FAILED', 'FAIL', 'N', '不合格', '异常'].includes(selfCheck) ||
    ['QUALITY_ABNORMAL', 'ABNORMAL', 'NG'].includes(productQualityStatus) ||
    ['NG', 'ABNORMAL', 'FAILED', 'FAIL'].includes(feedbackResult) ||
    Boolean(String(row.defectCode || extra.defectCode || '').trim()) ||
    Boolean(String(row.qualityLockReason || extra.qualityLockReason || '').trim())
  );
}

function isAdhesive2SegmentPendingConfirm(segment: SourceSegment) {
  const existedRecord = findReportBySegment(segment);
  return !!existedRecord?.id && !isAdhesiveRecordConfirmed(existedRecord.reportStatus);
}

function getAdhesive2SegmentSourceNgText(segment: SourceSegment) {
  const existedRecord = findReportBySegment(segment);
  if (existedRecord && hasVisualIssueFromRecord(existedRecord as Record<string, any>) && isPreProcessSelfCheckAbnormal(parseRecordExtra(existedRecord))) {
    return formatProcessNgText(resolveNgProcessName(existedRecord, '压槽'));
  }
  if (!segment.sourceNgText) return '';
  return formatProcessNgText(segment.sourceNgProcessName || segment.sourceNgText.replace(/工序NG$/, ''));
}

function getAdhesive2SegmentCurrentNgText(segment: SourceSegment) {
  const existedRecord = findReportBySegment(segment);
  if (!existedRecord) return '';
  if (hasDownstreamPreProcessFeedback(existedRecord)) return '粘胶2工序NG';
  const visualIssue = hasVisualIssueFromRecord(existedRecord as Record<string, any>);
  const attributedToPreProcess = visualIssue && isPreProcessSelfCheckAbnormal(parseRecordExtra(existedRecord));
  return (visualIssue && !attributedToPreProcess) || getAdhesive2SegmentInspectionNg(segment) ? '粘胶2工序NG' : '';
}

function getAdhesive2SegmentReportType(segment: SourceSegment): Adhesive2ReportType {
  const existedRecord = findReportBySegment(segment);
  if (!existedRecord) return 'PRODUCT';
  const extra = parseRecordExtra(existedRecord) as Record<string, any>;
  return isAdhesive2ReportType(extra.reportType) ? extra.reportType : 'PRODUCT';
}

function getAdhesive2SegmentCoaFlag(segment: SourceSegment) {
  const existedRecord = findReportBySegment(segment);
  const extra = parseRecordExtra(existedRecord || {}) as Record<string, any>;
  const sourceExtra = parseRecordExtra(segment as Record<string, any>);
  const raw = extra.coaFlag ?? sourceExtra.coaFlag ?? (segment as Record<string, any>).coaFlag;
  return raw === true || raw === 'true' || raw === 'Y' || raw === 1 || !!findAdhesive2CoaFaiAnyBySegment(segment);
}

function getAdhesive2SegmentInspectionNg(segment: SourceSegment) {
  const existedRecord = findReportBySegment(segment);
  const extra = parseRecordExtra(existedRecord || {}) as Record<string, any>;
  return ['NG', 'ABNORMAL'].includes(String(extra.feedbackResult || extra.inspectionResult || '').toUpperCase());
}

function getAdhesive2SegmentReportNgText(segment: SourceSegment) {
  return getAdhesive2SegmentCurrentNgText(segment) || getAdhesive2SegmentSourceNgText(segment);
}

function getAdhesive2ReportTypeClass(type?: string) {
  const classMap: Record<Adhesive2ReportType, string> = {
    CHANGEOVER: 'is-type-changeover',
    END: 'is-type-end',
    FRONT: 'is-type-front',
    MIDDLE: 'is-type-middle',
    PROCESS_CHECK: 'is-type-process',
    PRODUCT: 'is-type-product',
  };
  return isAdhesive2ReportType(type) ? classMap[type] : classMap.PRODUCT;
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

function formatAdhesive2TicketLength(value?: number) {
  return `${formatNumber(Number(value || 0))} m`;
}

async function buildAdhesive2TransferTicketItem(record: MesHcAdhesiveConsoleApi.ReportItem, now: string, planDetail: Record<string, any>) {
  const planNo = record.planNo || currentPlan.planNo;
  const extra = parseRecordExtra(record) as Record<string, any>;
  // 流转单必须以该片报工时固化的实际产品为准，避免计划原型号覆盖换型后的型号。
  // 当前运行型号只用于兼容历史报工缺失快照的场景，不能优先于报工事实，否则补打旧片会串型。
  const materialCode = record.materialCode || extra.runtimeMaterialCode || currentProductMaterialCode.value || planDetail?.materialCode || '-';
  const modelCode = record.modelCode || extra.runtimeModelCode || currentProductModelCode.value || planDetail?.modelCode || '-';
  const adhesive2BatchNo = record.productionBatchNo || '-';
  const segmentBatchNo = resolveMotherBatchNo(record as Record<string, any>) || '-';
  const sliceSerialNo = adhesive2BatchNo;
  const endTime = record.endTime || record.confirmerTime || record.recorderTime || now;
  const recorderName = record.recorderName || record.confirmerName || currentUserName.value || '-';
  const glueBoardModel =
    (record as Record<string, any>).glueBoardModel || extra.glueBoardModel || (record as Record<string, any>).glueBoardMaterialModel || getActualGlueBoardModel() || getSelectedGlueBoardModel() || currentPlan.glueBoardModel || '-';
  const glueBoardMaterialCode = record.glueBoardMaterialCode || extra.glueBoardMaterialCode || getActualGlueBoardMaterialCode() || '-';
  const glueBoardBatchNo = record.glueBoardBatchNo || '-';
  const startTime = record.startTime || record.recorderTime || now;
  const fallbackFields = [
    { label: '料号', value: materialCode },
    { label: '型号', value: modelCode },
    { label: '分段批号', value: segmentBatchNo },
    { label: '片号', value: sliceSerialNo },
  ];
  const fields = await applyPrintFieldTemplate('ADHESIVE2_TRANSFER', fallbackFields, {
    adhesive2BatchNo,
    endTime,
    glueBoardBatchNo,
    glueBoardMaterialCode,
    glueBoardModel,
    materialCode,
    modelCode,
    planNo,
    processName: '粘胶2',
    recorderName,
    segmentBatchNo,
    sliceSerialNo,
    startTime,
  });
  return {
    endTime,
    fields,
    materialCode,
    modelCode,
    planNo,
    processName: '粘胶2',
    productionBatchNo: adhesive2BatchNo,
    qrBottomText: adhesive2BatchNo,
    qrTopText: planNo,
    qrValue: buildTransferTicketQrValue(planNo, adhesive2BatchNo),
    recorderName,
    startTime,
    ticketId: record.id,
  };
}

async function buildAdhesive2TransferTicketPayload(records: MesHcAdhesiveConsoleApi.ReportItem[], planDetail: Record<string, any>) {
  const now = buildNowText();
  const items = await Promise.all(records.map((record) => buildAdhesive2TransferTicketItem(record, now, planDetail)));
  const commonPayload = {
    continueOnError: false,
    copies: 1,
    labelGapDots: 24,
    labelGapMm: 3,
    labelHeightMm: 50,
    labelWidthMm: 80,
    offsetXmm: 0,
    offsetYmm: 0,
    printerKey: 'adhesive2',
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

async function sendAdhesive2TransferTicketsToPrintAgent(records: MesHcAdhesiveConsoleApi.ReportItem[], planDetail: Record<string, any>) {
  const payload = await buildAdhesive2TransferTicketPayload(records, planDetail);
  const endpoint = records.length > 1 ? '/print/transfer-tickets' : '/print/transfer-ticket';
  const response = await fetch(`${ADHESIVE2_PRINT_AGENT_URL}${endpoint}`, {
    body: JSON.stringify(payload),
    headers: {
      'Content-Type': 'application/json',
    },
    method: 'POST',
  });
  const result = await response.json().catch(() => ({}));
  if (!response.ok || !result?.success || result?.accepted === false) {
    throw new Error(result?.message || `本机打印服务返回异常：${response.status}`);
  }
  return result;
}

async function markAdhesive2RecordsPrinted(records: MesHcAdhesiveConsoleApi.ReportItem[], printTime: string) {
  await Promise.all(
    records.map(async (record) => {
      const meta = getRecordPrintMeta(record);
      await markAdhesiveConsoleReportPrinted({
        id: record.id as number,
        printCount: meta.count + 1,
        printStatus: '已打印',
        printTime,
      });
    }),
  );
  selectedReportRecordKeys.value = selectedReportRecordKeys.value.filter((key) => !records.some((record) => record.id === key));
}

async function printAdhesiveRecords(records: MesHcAdhesiveConsoleApi.ReportItem[]) {
  const validRows = records.filter((record) => isPrintableTransferReport(record));
  if (!validRows.length) {
    AModal.info({
      title: '没有可打印记录',
      content: '请先对粘胶2报工选择实际尺寸和片号尾号。',
    });
    return false;
  }
  const now = buildNowText();
  try {
    const planDetail = currentPlan.planId ? await getPlanOrderDetail(currentPlan.planId as any) : {};
    const result = await sendAdhesive2TransferTicketsToPrintAgent(validRows, planDetail as Record<string, any>);
    try {
      await markAdhesive2RecordsPrinted(validRows, now);
      await loadReports();
      message.success(`粘胶2流转单已发送到 ${result.printerName || '配置打印机'}，数量：${result.printedCount || validRows.length}。`);
    } catch (markError: any) {
      await loadReports();
      AModal.warning({
        content: `本机打印服务已确认接收，但已打印状态回写失败：${markError?.message || markError}。请刷新后确认记录状态。`,
        title: '打印已发送，状态回写失败',
      });
    }
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

function buildAdhesive2TransferPrintRecord(target: Adhesive2TransferPrintTarget): MesHcAdhesiveConsoleApi.ReportItem {
  if (target.record) return target.record;
  const { group, segment } = target;
  const productionBatchNo = getAdhesive2SegmentDisplayBatchNo(segment) || segment.batchNo;
  const tailMatch = productionBatchNo.match(/([AB])$/i);
  return {
    actualSizeRule: normalizeSizeRule(segment.actualSizeRule) || undefined,
    actualSizeSuffix: String(segment.actualSizeSuffix || tailMatch?.[1] || '')
      .trim()
      .toUpperCase(),
    endTime: segment.confirmTime || '',
    materialCode: group.materialCode || currentProductMaterialCode.value || currentPlan.materialCode,
    modelCode: group.modelCode || currentProductModelCode.value || currentPlan.modelCode,
    parentProductionBatchNo: group.baseBatchNo,
    planId: Number(currentPlan.planId || segment.sourcePlanId || 0),
    planNo: group.planNo || currentPlan.planNo,
    planOperationId: Number(currentPlan.planOperationId || segment.sourcePlanOperationId || 0),
    productionBatchNo,
    recorderName: currentUserName.value || '',
    reportStatus: 'DRAFT',
    sourceProductionBatchNo: segment.batchNo,
    startTime: segment.confirmTime || '',
  };
}

async function printAdhesiveTransferTargets(targets: Adhesive2TransferPrintTarget[]) {
  const validTargets = targets.filter((target) => isPrintableTransferSegment(target.segment));
  if (!validTargets.length) {
    message.warning('当前没有可打印的粘胶2片号');
    return false;
  }
  const records = validTargets.map(buildAdhesive2TransferPrintRecord);
  const now = buildNowText();
  try {
    const planDetail = currentPlan.planId ? await getPlanOrderDetail(currentPlan.planId as any) : {};
    const result = await sendAdhesive2TransferTicketsToPrintAgent(records, planDetail as Record<string, any>);
    const persistedRecords = validTargets.map((target) => target.record).filter(isPrintableTransferReport);
    if (persistedRecords.length) {
      try {
        await markAdhesive2RecordsPrinted(persistedRecords, now);
        await loadReports();
      } catch (markError: any) {
        await loadReports();
        AModal.warning({
          content: `本机打印服务已确认接收，但已有报工记录的打印状态回写失败：${markError?.message || markError}。请刷新后确认记录状态。`,
          title: '打印已发送，状态回写失败',
        });
      }
    }
    message.success(`粘胶2流转单已发送到 ${result.printerName || '配置打印机'}，数量：${result.printedCount || validTargets.length}。`);
    return true;
  } catch (error: any) {
    AModal.warning({
      content: `未能连接或确认本机打印服务：${error?.message || error}。请先启动工序流转单 Python 打印服务。`,
      title: '打印流转单失败',
    });
    return false;
  }
}

const printSelectedAdhesiveRecords = () => void printAdhesiveRecords(printableReportRecords.value);
const printAllAdhesiveRecords = () => void printAdhesiveRecords(allPrintableReportRecords.value);

function openTransferPrintSelector() {
  transferPrintSelectionMode.value = true;
  activeBoardTab.value = 'SOURCE';
  visualMaximized.value = true;
  if (!selectableTransferReportCount.value) {
    // message.warning(reportRecords.value.length > 0 ? '当前过滤条件下暂无可打印粘胶2流转单' : '请先完成粘胶2报工后再选择打印流转单');
  }
}

function closeTransferPrintSelector() {
  transferPrintSelectionMode.value = false;
  selectedTransferReportIds.value = [];
}

function toggleTransferSegmentSelection(segment: SourceSegment) {
  const key = getTransferPrintTargetKey(segment);
  if (!isPrintableTransferSegment(segment) || !key) {
    message.warning('当前粘胶2片号不可打印');
    return;
  }
  selectedTransferReportIds.value = selectedTransferReportIds.value.includes(key) ? selectedTransferReportIds.value.filter((id) => id !== key) : [...selectedTransferReportIds.value, key];
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
  selectedTransferReportIds.value = selectableTransferTargets.value.map((target) => getTransferPrintTargetKey(target.segment));
}

function toggleAllTransferReports() {
  if (allTransferReportsSelected.value) {
    const visibleKeys = new Set(selectableTransferTargets.value.map((target) => getTransferPrintTargetKey(target.segment)));
    selectedTransferReportIds.value = selectedTransferReportIds.value.filter((id) => !visibleKeys.has(id));
    return;
  }
  selectAllTransferReports();
}

function selectGroupTransferReports(group: SourceGroup) {
  const groupKeys = group.segments
    .filter((segment) => isPrintableTransferSegment(segment))
    .map((segment) => getTransferPrintTargetKey(segment));
  selectedTransferReportIds.value = Array.from(new Set([...selectedTransferReportIds.value, ...groupKeys]));
}

async function handlePrintSelectedTransferReports() {
  if (!selectedTransferReportCount.value) {
    message.warning('请先选择要打印的粘胶2片');
    return;
  }
  const printed = await printAdhesiveTransferTargets(selectedTransferTargets.value);
  if (printed) closeTransferPrintSelector();
}

async function printChangeoverInspection(record: MesHcAdhesiveConsoleApi.ChangeoverInspection | Record<string, any>) {
  if (!record?.id) {
    message.warning('请先保存工艺参数点检记录后再打印记录单');
    return;
  }
  const printWindow = window.open('', '_blank', 'width=980,height=720');
  if (!printWindow) return;
  const qrDataUrl = await createPrintQrDataUrl(record.motherSegmentBatchNo || record.currentPlanNo || currentPlan.planNo);
  const checkRows = ((record.checkItems || changeoverForm.checkItems || []) as AdhesiveCheckItem[])
    .map(
      (item, index) => `
    <tr>
      <td>${index + 1}</td>
      <td>${item.itemCategory || ''}</td>
      <td>${item.itemName || ''}</td>
      <td>${item.standardValue || ''}</td>
      <td>${item.actualValue || ''}</td>
      <td>${item.abnormalRemark || ''}</td>
    </tr>
  `,
    )
    .join('');
  printWindow.document.write(`
    <html>
      <head>
        <title>粘胶2工艺参数点检记录单</title>
        <style>
          body { font-family: "Microsoft YaHei", Arial, sans-serif; margin: 24px; color: #111827; }
          .title { text-align: center; font-size: 24px; font-weight: 700; margin-bottom: 18px; }
          .head { display: grid; grid-template-columns: repeat(3, 1fr) 120px; border: 1px solid #111827; border-bottom: 0; }
          .cell { min-height: 34px; padding: 8px 10px; border-right: 1px solid #111827; border-bottom: 1px solid #111827; font-size: 14px; }
          .cell strong { margin-right: 6px; }
          .qr { grid-row: span 3; display: flex; align-items: center; justify-content: center; border-right: 0; }
          .qr img { width: 96px; height: 96px; }
          table { width: 100%; border-collapse: collapse; margin-top: 14px; font-size: 13px; }
          th, td { border: 1px solid #111827; padding: 7px 6px; text-align: center; }
          th { background: #eef2f7; }
          .remark { margin-top: 14px; font-size: 13px; }
        </style>
      </head>
      <body>
        <div class="title">粘胶2工艺参数点检记录单</div>
        <div class="head">
          <div class="cell"><strong>计划号</strong>${record.currentPlanNo || record.planNo || currentPlan.planNo || ''}</div>
          <div class="cell"><strong>生产型号</strong>${record.productionModelCode || currentProductModelCode.value || ''}</div>
          <div class="cell"><strong>当前料号</strong>${record.productionMaterialCode || currentProductMaterialCode.value || ''}</div>
          <div class="cell qr">${qrDataUrl ? `<img src="${qrDataUrl}" />` : ''}</div>
          <div class="cell"><strong>分段批号</strong>${record.motherSegmentBatchNo || ''}</div>
          <div class="cell"><strong>检验人</strong>${record.recorderName || ''}</div>
          <div class="cell"><strong>记录时间</strong>${displayDateTimeText(record.recordTime || record.submitTime)}</div>
          <div class="cell"><strong>反馈时间</strong>${displayDateTimeText(record.feedbackTime)}</div>
        </div>
        <table>
          <thead>
            <tr><th>序号</th><th>类别</th><th>项目</th><th>标准</th><th>实测值</th><th>异常备注</th></tr>
          </thead>
          <tbody>${checkRows || '<tr><td colspan="6">暂无明细</td></tr>'}</tbody>
        </table>
        <div class="remark">备注：${record.remark || ''}</div>
      </body>
    </html>
  `);
  printWindow.document.close();
  printWindow.focus();
  printWindow.print();
}

function resetChangeoverForm(record?: MesHcAdhesiveConsoleApi.ChangeoverInspection) {
  const now = buildNowText();
  const row = (record || {}) as Record<string, any>;
  Object.assign(changeoverForm, {
    changeoverInstructionId: record ? record.changeoverInstructionId : getCurrentChangeoverInstructionId(),
    checkItems: record?.checkItems?.length
      ? record.checkItems.map((item, index) => normalizeCheckItem(item, index))
      : ensureAdhesive2ChangeoverItems(checkTemplate.value).map((item) => ({
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
    motherSegmentBatchNo: record?.motherSegmentBatchNo || currentMotherBatchNo.value,
    planId: currentPlan.planId || record?.planId || 0,
    planNo: currentPlan.planNo || record?.planNo || '',
    planOperationId: currentPlan.planOperationId || record?.planOperationId || 0,
    previousModelCode: record?.previousModelCode || '',
    productionMaterialCode: record?.productionMaterialCode || currentProductMaterialCode.value || '',
    productionModelCode: record?.productionModelCode || currentProductModelCode.value || '',
    recordTime: record?.recordTime || now,
    recorderName: record?.recorderName || currentUserName.value || 'admin',
    remark: record?.remark || '',
    sourceSlittingSliceId: undefined,
    submitTime: record?.submitTime || record?.recordTime || now,
  });
  (changeoverForm as Record<string, any>).confirmTime = row.confirmTime || '';
  (changeoverForm as Record<string, any>).confirmUserName = row.confirmUserName || '';
  (changeoverForm as Record<string, any>).importAttachment = row.importAttachment;
  (changeoverForm as Record<string, any>).recordStatus = row.recordStatus || 'DRAFT';
}

function findPendingChangeoverInspection() {
  const currentMother = normalizeAdhesive2MiddleBatchNo(currentMotherBatchNo.value || changeoverForm.motherSegmentBatchNo || '');
  return changeoverInspections.value.find((record) =>
    !isChangeoverRecordConfirmed(record)
    && matchesCurrentChangeoverInstruction(record)
    && Boolean(currentMother)
    && normalizeAdhesive2MiddleBatchNo(record.motherSegmentBatchNo || '') === currentMother,
  );
}

async function openChangeoverInspectionScan(_initialSliceNo: string | Event = '') {
  if (isCurrentTaskReadonly.value) {
    showReadonlyTaskWarning('上报工艺参数');
    return;
  }
  if (!(await ensureAdhesive2PlanGlueBoardReadyForAction('工艺参数点检'))) return;
  const contextKey = getChangeoverPlanContextKey();
  const instruction = await loadChangeoverInstruction();
  if (instruction === undefined) {
    message.warning('换型状态刷新失败，请重试后填写工艺参数');
    return;
  }
  if (contextKey !== getChangeoverPlanContextKey()) return;
  activeBoardTab.value = 'CHANGEOVER';
  await refreshChangeoverInspectionContext();
  if (contextKey !== getChangeoverPlanContextKey()) return;
  resetChangeoverForm(findPendingChangeoverInspection());
  changeoverVisible.value = true;
}

function resolveLocalChangeoverInspectionStatus(items: AdhesiveCheckItem[] = []) {
  return 'QUALIFIED';
}

function buildChangeoverProcessParamPayload(checkItems: AdhesiveCheckItem[]): MesHcAdhesiveConsoleApi.ProcessParamRecord {
  const motherSegmentBatchNo = currentMotherBatchNo.value || changeoverForm.motherSegmentBatchNo || '';
  return {
    changeoverInstructionId: getCurrentChangeoverInstructionId(),
    formName: '粘胶2点检表',
    formType: 'PRODUCTION_CHECK',
    formTypeName: '工艺参数点检',
    id: changeoverForm.id,
    importAttachment: (changeoverForm as Record<string, any>).importAttachment,
    items: checkItems.map((item, index) => ({
      abnormalRemark: item.abnormalRemark || '',
      actualValue: item.actualValue || '',
      checkResult: 'OK',
      itemCategory: item.itemCategory || '其他',
      itemName: item.itemName || '',
      modelCode: currentProductModelCode.value || changeoverForm.productionModelCode || '',
      motherBatchNo: motherSegmentBatchNo,
      productionBatchNo: motherSegmentBatchNo,
      recorderName: changeoverForm.recorderName || currentUserName.value || '',
      reportDate: dayjs(changeoverForm.recordTime || changeoverForm.submitTime || undefined).format('YYYY-MM-DD'),
      seq: item.sortNo || index + 1,
      sortNo: item.sortNo || index + 1,
      standardValue: item.standardValue || '',
    })),
    materialCode: currentProductMaterialCode.value || changeoverForm.productionMaterialCode || '',
    modelCode: currentProductModelCode.value || changeoverForm.productionModelCode || '',
    motherBatchNo: motherSegmentBatchNo,
    parentProductionBatchNo: motherSegmentBatchNo,
    planId: currentPlan.planId || changeoverForm.planId,
    planNo: currentPlan.planNo || changeoverForm.planNo,
    planOperationId: currentPlan.planOperationId || changeoverForm.planOperationId,
    productionBatchNo: motherSegmentBatchNo,
    confirmTime: changeoverConfirmTime.value || undefined,
    confirmUserName: changeoverConfirmUserName.value || undefined,
    recordStatus: (changeoverForm as Record<string, any>).recordStatus || undefined,
    recorderName: changeoverForm.recorderName || currentUserName.value || '',
    remark: changeoverForm.remark || '',
    reportDate: dayjs(changeoverForm.recordTime || changeoverForm.submitTime || undefined).format('YYYY-MM-DD'),
  };
}

function triggerChangeoverImport() {
  if (isCurrentTaskReadonly.value) {
    showReadonlyTaskWarning('导入工艺参数点检');
    return;
  }
  if (changeoverRecordLocked.value) {
    message.warning('已确认记录不能导入');
    return;
  }
  processParamFileInput.value?.click();
}

async function handleProcessParamFileChange(event: Event) {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  input.value = '';
  if (!file) return;
  if (isCurrentTaskReadonly.value) {
    showReadonlyTaskWarning('导入工艺参数点检');
    return;
  }
  if (changeoverRecordLocked.value) {
    message.warning('已确认记录不能导入');
    return;
  }
  if (!currentPlan.planId || !currentPlan.planOperationId) {
    message.warning('请先扫描或选择粘胶2计划');
    return;
  }
  const motherBatchNo = currentMotherBatchNo.value || changeoverForm.motherSegmentBatchNo;
  if (!motherBatchNo) {
    message.warning('未找到当前分段批号，请先加载粘胶2计划与可加工来源');
    return;
  }
  changeoverImporting.value = true;
  try {
    const attachment = await uploadAdhesive2ImportAttachment(file, 'mes/adhesive2/process-check');
    const count = await importAdhesive2ProcessParams(currentPlan.planId, currentPlan.planOperationId, file, attachment, {
      changeoverInstructionId: getCurrentChangeoverInstructionId(),
      formType: 'PRODUCTION_CHECK',
      motherBatchNo,
      productionBatchNo: motherBatchNo,
      recordDate: dayjs(changeoverForm.recordTime || changeoverForm.submitTime || undefined).format('YYYY-MM-DD'),
      recordId: changeoverForm.id,
    });
    (changeoverForm as Record<string, any>).importAttachment = attachment;
    await loadChangeoverInspections();
    const latest = changeoverInspections.value.find((item) => item.id === changeoverForm.id && matchesCurrentChangeoverInstruction(item)) || findPendingChangeoverInspection();
    if (latest) resetChangeoverForm(latest);
    message.success(`已导入 ${Number(((count as any)?.data ?? count) || 0)} 条粘胶2点检明细`);
  } finally {
    changeoverImporting.value = false;
  }
}

async function exportChangeoverInspectionExcel() {
  if (!currentPlan.planOperationId) {
    message.warning('请先扫描或选择粘胶2计划');
    return;
  }
  let recordId = changeoverForm.id;
  if (changeoverVisible.value && !recordId) {
    const checkItems = (changeoverForm.checkItems || []).map((item, index) => ({
      ...item,
      sortNo: item.sortNo || index + 1,
    }));
    recordId = await saveAdhesive2ProcessParam(buildChangeoverProcessParamPayload(checkItems));
    changeoverForm.id = recordId;
  }
  const data = await exportAdhesive2ProcessParams(currentPlan.planOperationId, {
    formType: 'PRODUCTION_CHECK',
    motherBatchNo: currentMotherBatchNo.value || changeoverForm.motherSegmentBatchNo || undefined,
    recordId,
  });
  downloadFileFromBlobPart({ fileName: '粘胶2点检表.xlsx', source: data });
}

function getChangeoverCheckItemsForSubmit() {
  return (changeoverForm.checkItems || []).map((item, index) => ({
    ...item,
    sortNo: item.sortNo || index + 1,
  }));
}

function openChangeoverSaveAuth() {
  if (changeoverSubmitting.value) return;
  if (isCurrentTaskReadonly.value) {
    showReadonlyTaskWarning('保存工艺参数点检');
    return;
  }
  if (changeoverRecordLocked.value) {
    message.warning('已确认记录不能修改');
    return;
  }
  const motherSegmentBatchNo = currentMotherBatchNo.value || changeoverForm.motherSegmentBatchNo;
  if (!motherSegmentBatchNo) {
    message.warning('未找到当前分段批号，请先加载粘胶2计划与可加工来源');
    return;
  }
  changeoverSaveAuthAction.value = '保存粘胶2工艺参数点检记录';
  changeoverSaveAuthVisible.value = true;
}

async function handleChangeoverSaveAuthSuccess(userInfo: any) {
  changeoverSaveAuthVisible.value = false;
  const recorderName = resolveAuthenticatedUserName(userInfo, changeoverForm.recorderName);
  if (!recorderName) {
    message.warning('未识别到工艺参数点检记录人，请重新认证');
    return;
  }
  const now = buildNowText();
  changeoverForm.recorderName = recorderName;
  changeoverForm.recordTime = changeoverForm.recordTime || now;
  changeoverForm.submitTime = now;
  await submitChangeoverInspection(recorderName);
}

function handleChangeoverSaveAuthCancel() {
  changeoverSaveAuthVisible.value = false;
}

function openChangeoverConfirmAuth() {
  if (changeoverConfirming.value) return;
  if (isCurrentTaskReadonly.value) {
    showReadonlyTaskWarning('确认工艺参数点检');
    return;
  }
  if (changeoverRecordLocked.value) {
    message.info('当前粘胶2工艺参数点检记录已确认');
    return;
  }
  if (!getChangeoverCheckItemsForSubmit().length) {
    message.warning('粘胶2点检明细不能为空');
    return;
  }
  changeoverConfirmAuthAction.value = '确认粘胶2工艺参数点检记录';
  changeoverConfirmAuthVisible.value = true;
}

async function handleChangeoverConfirmAuthSuccess(userInfo: any) {
  changeoverConfirmAuthVisible.value = false;
  const confirmerName = resolveAuthenticatedUserName(userInfo, changeoverConfirmUserName.value);
  if (!confirmerName) {
    message.warning('未识别到工艺参数点检确认人，请重新认证');
    return;
  }
  await confirmChangeoverInspection(confirmerName);
}

function handleChangeoverConfirmAuthCancel() {
  changeoverConfirmAuthVisible.value = false;
}

async function confirmChangeoverInspection(authUserName?: string) {
  if (changeoverConfirming.value) return;
  if (!authUserName) {
    message.warning('请先完成工艺参数点检确认认证');
    return;
  }
  if (isCurrentTaskReadonly.value) {
    showReadonlyTaskWarning('确认工艺参数点检');
    return;
  }
  if (changeoverRecordLocked.value) {
    message.info('当前粘胶2工艺参数点检记录已确认');
    return;
  }
  const checkItems = getChangeoverCheckItemsForSubmit();
  if (!checkItems.length) {
    message.warning('粘胶2点检明细不能为空');
    return;
  }
  AModal.confirm({
    title: '确认粘胶2工艺参数点检记录',
    content: '确认后不能修改，是否继续确认？',
    okText: '确认',
    cancelText: '取消',
    zIndex: 3000,
    async onOk() {
      if (changeoverConfirming.value) return;
      changeoverConfirming.value = true;
      try {
        (changeoverForm as Record<string, any>).confirmUserName = authUserName;
        (changeoverForm as Record<string, any>).confirmTime = buildNowText();
        const id = await confirmAdhesive2ProcessParam(buildChangeoverProcessParamPayload(checkItems));
        changeoverForm.id = id;
        await loadChangeoverInspections();
        const latest = changeoverInspections.value.find((item) => item.id === id);
        if (latest) resetChangeoverForm(latest);
        changeoverVisible.value = false;
        message.success('粘胶2工艺参数点检记录已确认');
      } finally {
        changeoverConfirming.value = false;
      }
    },
  });
}

async function submitChangeoverInspection(authUserName?: string) {
  if (changeoverSubmitting.value) return;
  if (!authUserName) {
    message.warning('请先完成工艺参数点检保存认证');
    return;
  }
  if (isCurrentTaskReadonly.value) {
    showReadonlyTaskWarning('保存工艺参数点检');
    return;
  }
  if (changeoverRecordLocked.value) {
    message.warning('已确认记录不能修改');
    return;
  }
  const motherSegmentBatchNo = currentMotherBatchNo.value || changeoverForm.motherSegmentBatchNo;
  if (!motherSegmentBatchNo) {
    message.warning('未找到当前分段批号，请先加载粘胶2计划与可加工来源');
    return;
  }
  changeoverSubmitting.value = true;
  try {
    const now = buildNowText();
    const submitTime = changeoverForm.submitTime || now;
    const recordTime = changeoverForm.recordTime || submitTime;
    changeoverForm.recorderName = authUserName;
    changeoverForm.recordTime = recordTime;
    changeoverForm.submitTime = submitTime;
    const checkItems = getChangeoverCheckItemsForSubmit();
    const inspectionStatus = resolveLocalChangeoverInspectionStatus(checkItems);
    const payload: MesHcAdhesiveConsoleApi.ChangeoverInspection = {
      ...changeoverForm,
      checkItems,
      currentPlanNo: currentPlan.planNo,
      detailItemsJson: JSON.stringify(checkItems),
      inspectionStatus,
      motherSegmentBatchNo,
      planId: currentPlan.planId || changeoverForm.planId,
      planNo: currentPlan.planNo,
      planOperationId: currentPlan.planOperationId || changeoverForm.planOperationId,
      pressSlotSliceNo: undefined,
      productionMaterialCode: currentProductMaterialCode.value || changeoverForm.productionMaterialCode,
      productionModelCode: currentProductModelCode.value || changeoverForm.productionModelCode,
      recordTime,
      sourceSlittingSliceId: undefined,
      submitTime,
    };
    const savedId = await saveAdhesive2ProcessParam(buildChangeoverProcessParamPayload(checkItems));
    const savedRecord = { ...payload, id: savedId, recordStatus: 'RECORDED' };
    changeoverInspections.value = [savedRecord, ...changeoverInspections.value.filter((record) => record.id !== savedId)];
    latestChangeoverInspection.value = savedRecord;
    changeoverVisible.value = false;
    await loadChangeoverInspections();
    message.success('粘胶2工艺参数点检记录已保存');
  } finally {
    changeoverSubmitting.value = false;
  }
}

function goFirstInspectionTab() {
  activeBoardTab.value = 'CHANGEOVER';
}

function goGlueBoardInspectionTab() {
  activeBoardTab.value = 'GLUE_BOARD_INSPECTION';
}

function getCoaReportBlockedReason(record: MesHcAdhesiveConsoleApi.ReportItem) {
  const extra = parseRecordExtra(record);
  const lockReason = String(record.qualityLockReason || extra.qualityLockReason || '').trim();
  if (lockReason) return lockReason;
  const qualityStatus = String(record.productQualityStatus || extra.productQualityStatus || '').trim().toUpperCase();
  if (qualityStatus === 'LOCKED') return '成品处于质量锁定状态';
  if (!hasAdhesive2ReportCurrentNg(record)) return '';
  const defectCode = String(record.defectCode || extra.defectCode || '').trim();
  if (defectCode) return `不良代码：${defectCode}`;
  return String(extra.visualInspectionRemark || '').trim() || '成品自检、外观或质量状态异常';
}

function getCoaInspectionScanError(batchNo: string) {
  const row = coaInspectionReportRows.value.find((item) => isSameText(item.batchNo, batchNo));
  if (row?.blockedReason) {
    return `成品片号 ${batchNo} 已保存报工，但当前存在质量异常或锁定，不能 COA 送检。原因：${row.blockedReason}。`;
  }
  if (row) return '';
  if (coaReportRecords.value.some((record) => record.id && isSameText(record.productionBatchNo, batchNo))) {
    return `成品片号 ${batchNo} 已保存报工，但不属于当前分段，请切换到对应分段后送检。`;
  }
  return `当前任务未找到粘胶2成品片号 ${batchNo} 的已保存报工记录，请核对任务、片号，并确认已保存报工及选择尾号。`;
}

async function loadCoaReports() {
  coaReportRecords.value = [];
  const planOperationId = currentPlan.planOperationId;
  if (!planOperationId) return;
  const rows = normalizeRows(await getAdhesiveConsoleReportList(planOperationId));
  if (currentPlan.planOperationId === planOperationId) coaReportRecords.value = rows;
}

function resetCoaInspectionScanForm() {
  Object.assign(coaInspectionScanForm, {
    normalizedSliceNo: '',
    scanError: '',
    scanMessage: '',
    scannedSliceNo: '',
  });
}

function selectCoaInspectionSlice(batchNo?: string) {
  selectedCoaSliceNo.value = String(batchNo || '').trim();
  resetCoaInspectionScanForm();
}

function normalizeCoaInspectionScanNo(value?: string) {
  return resolveTransferTicketQrBusinessNo(normalizeConfirmScanCode(value)).trim();
}

function handleCoaInspectionScan() {
  const scannedSliceNo = normalizeCoaInspectionScanNo(coaInspectionScanForm.scannedSliceNo);
  coaInspectionScanForm.normalizedSliceNo = scannedSliceNo;
  coaInspectionScanForm.scanError = '';
  coaInspectionScanForm.scanMessage = '';
  if (!scannedSliceNo) {
    coaInspectionScanForm.scanError = '请先扫描或输入COA送检片号。';
    return false;
  }
  coaInspectionScanForm.scannedSliceNo = scannedSliceNo;
  const scanError = getCoaInspectionScanError(scannedSliceNo);
  if (scanError) {
    selectedCoaSliceNo.value = '';
    coaInspectionScanForm.scanError = scanError;
    return false;
  }
  const matchedRow = coaInspectionRows.value.find(
    (row) =>
      String(row.batchNo || '')
        .trim()
      .toUpperCase() === scannedSliceNo.toUpperCase(),
  );
  if (!matchedRow) {
    selectedCoaSliceNo.value = '';
    coaInspectionScanForm.scanError = getCoaInspectionScanError(scannedSliceNo);
    return false;
  }
  selectedCoaSliceNo.value = matchedRow.batchNo;
  coaInspectionScanForm.scanMessage = isAdhesiveRecordConfirmed(matchedRow.reportStatus)
    ? `已带入已确认成品片号 ${matchedRow.batchNo}，将直接提交COA送检。`
    : `已带入未确认成品片号 ${matchedRow.batchNo}，提交时将先完成粘胶2扫码确认，再创建COA送检。`;
  return true;
}

async function openCoaInspectionDialog() {
  if (isCurrentTaskReadonly.value) {
    showReadonlyTaskWarning('COA送检');
    return;
  }
  if (!currentPlan.planId || !currentPlan.planOperationId) {
    AModal.warning({
      title: '请先加载粘胶2计划',
      content: 'COA送检需要先扫码或从待加工列表带出当前粘胶2计划。',
    });
    return;
  }
  await Promise.all([loadReports(), loadCoaReports(), loadSourceGroups()]);
  const rows = coaInspectionRows.value;
  if (!rows.some((row) => row.batchNo === selectedCoaSliceNo.value)) {
    selectedCoaSliceNo.value = '';
  }
  resetCoaInspectionScanForm();
  coaInspectionVisible.value = true;
  if (!rows.length) {
    message.warning(coaInspectionEmptyMessage.value);
  }
}

async function submitCoaInspection() {
  if (isCurrentTaskReadonly.value) {
    showReadonlyTaskWarning('COA送检');
    return;
  }
  if (!currentPlan.planId || !currentPlan.planOperationId) {
    message.warning('请先加载粘胶2计划');
    return;
  }
  const scanInput = normalizeCoaInspectionScanNo(coaInspectionScanForm.scannedSliceNo);
  if (!scanInput) {
    coaInspectionScanForm.normalizedSliceNo = '';
  } else if (scanInput !== coaInspectionScanForm.normalizedSliceNo) {
    const scanOk = handleCoaInspectionScan();
    if (!scanOk) return;
  }
  const row = selectedCoaInspectionRow.value;
  const selectedBatchNo = String(selectedCoaSliceNo.value || '').trim();
  const submitBatchNo = String(coaInspectionScanForm.normalizedSliceNo || selectedBatchNo || row?.batchNo || '').trim();
  if (!submitBatchNo) {
    message.warning('请选择或扫码需要送检的粘胶2成品片号');
    return;
  }
  if (coaInspectionApplying.value) return;
  const scanError = getCoaInspectionScanError(submitBatchNo);
  if (scanError) {
    selectedCoaSliceNo.value = '';
    coaInspectionScanForm.scanError = scanError;
    return;
  }
  const matchedRow =
    row &&
    String(row.batchNo || '')
      .trim()
      .toUpperCase() === submitBatchNo.toUpperCase()
      ? row
      : coaInspectionRows.value.find(
          (item) =>
            String(item.batchNo || '')
          .trim()
          .toUpperCase() === submitBatchNo.toUpperCase(),
        );
  if (!matchedRow?.sourceReportId) {
    coaInspectionScanForm.scanError = getCoaInspectionScanError(submitBatchNo);
    return;
  }
  const requiresConfirm = !isAdhesiveRecordConfirmed(matchedRow.reportStatus);
  if (requiresConfirm) {
    const sourceMatch = findSourceSegmentWithGroup(matchedRow.sourceProductionBatchNo || submitBatchNo);
    if (!(await ensureAdhesive2SampleAbnormalUnlocked(
      sourceMatch?.segment || matchedRow.sourceProductionBatchNo || submitBatchNo,
      '粘胶2确认并COA送检',
      sourceMatch?.group?.baseBatchNo || matchedRow.motherBatchNo,
    ))) {
      return;
    }
    if (!(await ensureDailyPreparationReadyForReport())) return;
    if (!(await ensureCurrentMotherChangeoverSubmittedForReport())) return;
    if (!isChangeoverInspectionReady()) {
      showChangeoverInspectionRequiredWarning(submitBatchNo);
      return;
    }
    if (!ensureGlueBoardReadyForAction('粘胶2确认并COA送检')) return;
  }
  const actualGlueBoardBatchNo = String(glueBoard.batchNo || '').trim();
  const actualGlueBoardMaterialCode = String(getActualGlueBoardMaterialCode() || '').trim();
  const actualGlueBoardModel = String(getActualGlueBoardModel() || getSelectedGlueBoardModel() || '').trim();
  if (requiresConfirm && (!glueBoard.id || !actualGlueBoardBatchNo)) {
    coaInspectionScanForm.scanError = '请先登记本次胶板领用信息，再执行粘胶2确认并COA送检。';
    return;
  }
  if (requiresConfirm && !actualGlueBoardModel) {
    coaInspectionScanForm.scanError = '请选择胶板型号，再执行粘胶2确认并COA送检。';
    return;
  }
  coaInspectionApplying.value = true;
  try {
    const summary = await applyAdhesive2PostConfirmCoa({
      confirmerName: currentUserName.value || 'admin',
      confirmerTime: buildNowText(),
      glueBoardBatchNo: requiresConfirm ? actualGlueBoardBatchNo : undefined,
      glueBoardMaterialCode: requiresConfirm ? actualGlueBoardMaterialCode || undefined : undefined,
      glueBoardModel: requiresConfirm ? actualGlueBoardModel : undefined,
      glueBoardUsageId: requiresConfirm ? glueBoard.id || undefined : undefined,
      id: matchedRow.sourceReportId,
      scannedBatchNo: submitBatchNo,
    });
    if (requiresConfirm) {
      await refreshChangeoverAfterReportConfirmation();
    }
    await Promise.all([loadCoaFaiRows(), loadSourceGroups(), loadReports()]);
    coaInspectionVisible.value = false;
    const submittedRecord = {
      ...summary,
      faiApplyTime: summary.faiApplyTime || buildNowText(),
      inspectionScopeBatchNo: summary.inspectionScopeBatchNo || matchedRow.motherBatchNo,
      productBatchNo: summary.productBatchNo || submitBatchNo,
      productModel: summary.productModel || currentProductModelCode.value,
    };
    const submittedInspectionNo = resolveAdhesive2InspectionNo(submittedRecord);
    const successPrefix = requiresConfirm ? '粘胶2已扫码确认并提交COA送检' : 'COA送检已提交';
    message.success(submittedInspectionNo ? `${successPrefix}，单号：${submittedInspectionNo}` : successPrefix);
    if (summary.faiId) {
      faiDetailModalApi.setData({ id: summary.faiId }).open();
    }
    promptPrintCoaInspectionTransferTicket(submittedRecord);
  } catch (error: any) {
    AModal.warning({
      content: getReportRequestErrorMessage(error, 'COA送检失败，请检查粘胶2成品确认状态、片号和FAI检验标准。'),
      title: 'COA送检未提交',
    });
  } finally {
    coaInspectionApplying.value = false;
  }
}

async function submitFirstInspection() {
  if (!glueBoard.id || !glueBoard.stockId || !glueBoard.batchNo) {
    message.warning('请先保存本次胶板领用记录，再提交胶板送检');
    return;
  }
  const glueBoardModel = getActualGlueBoardModel();
  if (!glueBoardModel) {
    message.warning('当前胶板缺少胶板型号，请先扫码或选择胶板边库批次');
    return;
  }
  const sampleLength = Number(glueBoardFaiForm.sampleLength || 0);
  if (sampleLength <= 0) {
    message.warning('请填写大于0的送检长度');
    return;
  }
  const maxSampleLength = getGlueBoardFaiMaxSampleLength();
  if (maxSampleLength <= 0 || sampleLength > maxSampleLength) {
    message.warning(`送检长度不能超过当前可用长度 ${formatNumber(maxSampleLength)} m`);
    return;
  }
  if (firstInspectionApplying.value) return;
  firstInspectionApplying.value = true;
  try {
    const actualGlueBoardMaterialCode = String(getActualGlueBoardMaterialCode() || '').trim();
    const record = await createGlueBoardFaiRecordFromAdhesive2({
      glueBoardMaterialCode: actualGlueBoardMaterialCode || undefined,
      glueBoardMaterialName: undefined,
      glueBoardModel,
      glueBoardStockId: glueBoard.stockId,
      glueBoardUsageId: glueBoard.id,
      gluePlateBatchNo: glueBoard.batchNo,
      operationCode: 'GLUE_2',
      operationName: '粘胶2',
      planOrderId: currentPlan.planOperationId || undefined,
      processCategory: 'GLUE_2',
      remark: `粘胶2看板胶板送检；计划 ${currentPlan.planNo || '-'}；分段 ${getCurrentGlueBoardFaiSegmentBatchNo() || '-'}；工序任务 ${currentPlan.planOperationId || '-'}`,
      sampleLength,
      sampleStartPosition: Number(glueBoardFaiForm.sampleStartPosition || 0),
      sourceProductionBatchNo: getCurrentGlueBoardFaiSegmentBatchNo() || undefined,
      sourceReportNo: getCurrentGlueBoardFaiSegmentBatchNo() || undefined,
      submissionTime: buildNowText(),
      submitterName: currentUserName.value || undefined,
      workOrderNo: currentPlan.planNo || glueBoard.batchNo,
    });
    glueBoard.qualityStatus = 'WAITING';
    glueBoard.latestInspectionId = record.id;
    glueBoard.latestInspectionNo = record.faiNo || '';
    glueBoard.latestInspectionResult = record.judgment && record.judgment !== '-' ? record.judgment : 'PENDING';
    glueBoard.inspectionSubmitTime = record.submissionTime || buildNowText();
    applyGlueBoardInspectionSummary();
    await loadGlueBoardUsage();
    await glueBoardEdgeConsumableTabRef.value?.reload?.();
    await reloadWorkbenchForLoadedPlan();
    glueBoardFaiVisible.value = false;
    goGlueBoardInspectionTab();
    const submittedRecord = {
      ...record,
      faiApplyTime: record.faiApplyTime || record.submissionTime || buildNowText(),
      glueBoardBatchNo: record.glueBoardBatchNo || glueBoard.batchNo,
      glueBoardMaterialCode: record.glueBoardMaterialCode || actualGlueBoardMaterialCode,
      glueBoardModel: record.glueBoardModel || glueBoardModel,
      sampleLength: record.sampleLength ?? sampleLength,
    };
    message.success(`胶板 ${glueBoard.batchNo} 检验已提交，已按 ${formatNumber(sampleLength)} m 核减当前可用长度，单号：${record.faiNo || '-'}`);
    promptPrintGlueBoardInspectionTransferTicket(submittedRecord);
  } catch (error: any) {
    AModal.warning({
      content: error?.message || '当前胶板不满足送检申请条件，请检查胶板型号、边库批次和胶板送检标准。',
      title: '胶板送检未提交',
    });
  } finally {
    firstInspectionApplying.value = false;
  }
}

function viewFirstInspectionDetail() {
  if (!firstInspection.value.faiId) return;
  faiDetailModalApi.setData({ id: firstInspection.value.faiId }).open();
}

function viewCoaFaiDetail(record: MesHcAdhesiveConsoleApi.FaiSummary | Record<string, any>) {
  if (!record?.faiId) return;
  faiDetailModalApi.setData({ id: record.faiId }).open();
}

function viewProcessCheckFaiDetail(record: MesHcAdhesiveConsoleApi.FaiSummary | Record<string, any>) {
  if (!record?.faiId) return;
  faiDetailModalApi.setData({ id: record.faiId }).open();
}

function viewChangeoverInspection(record: MesHcAdhesiveConsoleApi.ChangeoverInspection | Record<string, any>) {
  resetChangeoverForm(record as MesHcAdhesiveConsoleApi.ChangeoverInspection);
  changeoverVisible.value = true;
}

function deleteChangeoverInspection(record: MesHcAdhesiveConsoleApi.ChangeoverInspection | Record<string, any>) {
  const recordId = Number(record?.id || 0);
  const planOperationId = Number(record?.planOperationId || currentPlan.planOperationId || 0);
  if (!recordId || !planOperationId) {
    message.warning('未找到要删除的粘胶2工艺参数点检记录');
    return;
  }
  AModal.confirm({
    title: '删除粘胶2工艺参数点检记录',
    content: `确认删除分段批号 ${record?.motherSegmentBatchNo || '-'} 的工艺参数点检记录？删除后列表将不再显示该记录。`,
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    zIndex: 3000,
    async onOk() {
      await deleteAdhesive2ProcessParam(recordId, planOperationId);
      if (Number(changeoverForm.id || 0) === recordId) {
        changeoverVisible.value = false;
        resetChangeoverForm();
      }
      await loadChangeoverInspections();
      message.success('粘胶2工艺参数点检记录已删除');
    },
  });
}

const [FaiDetailPreviewModal, faiDetailModalApi] = useVbenModal({
  connectedComponent: FaiDetailModal,
});

function resolveAdhesive2InspectionNo(record?: MesHcAdhesiveConsoleApi.FaiSummary | Record<string, any> | null) {
  return String(record?.faiNo || record?.sourceReportNo || record?.faiId || '').trim();
}

function formatAdhesive2InspectionStandard(record: MesHcAdhesiveConsoleApi.FaiSummary | Record<string, any>) {
  return record.faiStandardNo ? `${record.faiStandardNo}${record.faiStandardVersion ? ` / ${record.faiStandardVersion}` : ''}` : '-';
}

async function buildAdhesive2InspectionTransferTicketPayload(record: MesHcAdhesiveConsoleApi.FaiSummary | Record<string, any>, options: Adhesive2InspectionTransferPrintOptions) {
  const inspectionNo = resolveAdhesive2InspectionNo(record);
  const applyTime = options.applyTime || record.faiApplyTime || record.submissionTime || buildNowText();
  const productionBatchNo = options.productionBatchNo || record.productBatchNo || record.inspectionScopeBatchNo || currentPlan.batchNo;
  const glueBoardMaterialCode = options.boardMaterialCode || record.glueBoardMaterialCode || getActualGlueBoardMaterialCode();
  const productModel = record.productModel || currentProductModelCode.value;
  const fallbackFields = options.fields || [
    { label: '当前工序', value: '粘胶2' },
    { label: '产品型号', value: productModel || '-' },
    { label: '送检时间', value: applyTime },
  ];
  const printContext = {
    applyTime,
    coaSliceNo: record.productBatchNo || productionBatchNo,
    glueBoardBatchNo: options.boardBatchNo || record.glueBoardBatchNo || record.gluePlateBatchNo,
    glueBoardMaterialCode,
    glueBoardModel: record.glueBoardModel || getActualGlueBoardModel() || getSelectedGlueBoardModel() || currentPlan.glueBoardModel,
    modelCode: productModel,
    inspectionType: options.inspectionType,
    processName: '粘胶2',
    productionBatchNo,
    sampleQty: record.sampleQty || '1',
    sampleLength: record.sampleLength ? `${formatNumber(Number(record.sampleLength))} m` : '-',
    sampleType: options.sampleType,
  };
  const fields = options.templateCode ? await applyPrintFieldTemplate(options.templateCode, fallbackFields, printContext) : fallbackFields;
  return buildInspectionTransferTicketPayload({
    applicantName: currentUserName.value,
    applyTime,
    boardBatchNo: options.boardBatchNo,
    extraFields: [
      {
        label: '检验状态',
        value: options.statusText || getCoaFaiStatusMeta(record).text,
      },
      { label: '检测结论', value: record.faiJudgment || '-' },
      { label: '检验标准', value: formatAdhesive2InspectionStandard(record) },
      { label: '来源单号', value: record.sourceReportNo || '-' },
      {
        label: '反馈备注',
        value: record.faiRejectReason || record.remark || '-',
      },
      ...(options.extraFields || []),
    ],
    fields,
    inspectionNo,
    inspectionTransferNo: inspectionNo,
    inspectionType: options.inspectionType,
    materialCode: options.materialCode || currentProductMaterialCode.value,
    modelCode: currentProductModelCode.value,
    planNo: currentPlan.planNo,
    processName: '粘胶2',
    productionBatchNo,
    sampleType: options.sampleType,
    title: options.title || '检验流转单',
  });
}

async function printAdhesive2InspectionTransferTicket(
  record?: MesHcAdhesiveConsoleApi.FaiSummary | Record<string, any> | null,
  options: Adhesive2InspectionTransferPrintOptions = {
    inspectionType: '检验',
    sampleType: '检验样',
  },
) {
  const ticketTitle = options.title || '检验流转单';
  const inspectionNo = resolveAdhesive2InspectionNo(record);
  if (!record || !inspectionNo) {
    AModal.warning({
      content: `当前检验记录还没有检验单号，无法生成${ticketTitle}二维码。请先完成送检后再打印。`,
      title: `无法打印${ticketTitle}`,
    });
    return;
  }
  try {
    const result = await sendTransferTicketToPrintAgent(await buildAdhesive2InspectionTransferTicketPayload(record, options), ADHESIVE2_PRINT_AGENT_URL);
    AModal.success({
      content: `${ticketTitle}已发送到 ${result.printerName || '配置打印机'}，数量：${result.printedCount || 1}。`,
      okText: '知道了',
      title: `打印${ticketTitle}`,
    });
  } catch (error: any) {
    AModal.warning({
      content: `未能连接或确认本机打印服务：${error?.message || error}。请先启动工序流转单 Python 打印服务。`,
      title: `打印${ticketTitle}失败`,
    });
  }
}

function printGlueBoardInspectionTransferTicket(record?: MesHcAdhesiveConsoleApi.FaiSummary | Record<string, any> | null) {
  const inspection = record && resolveAdhesive2InspectionNo(record) ? record : firstInspection.value;
  const sampleLengthValue = Number(inspection.sampleLength || 0);
  const sampleLength = sampleLengthValue > 0 ? `${formatNumber(sampleLengthValue)} m` : '-';
  const applyTime = displayDateTimeText(inspection.faiApplyTime || inspection.submissionTime || glueBoard.inspectionSubmitTime || buildNowText());
  const glueBoardBatchNo = inspection.glueBoardBatchNo || inspection.gluePlateBatchNo || glueBoard.batchNo;
  const glueBoardMaterialCode = inspection.glueBoardMaterialCode || getActualGlueBoardMaterialCode();
  const glueBoardModel = inspection.glueBoardModel || getActualGlueBoardModel() || getSelectedGlueBoardModel() || currentPlan.glueBoardModel;
  return printAdhesive2InspectionTransferTicket(inspection, {
    applyTime,
    boardBatchNo: glueBoardBatchNo,
    boardMaterialCode: glueBoardMaterialCode,
    fields: [
      { label: '型号', value: currentProductModelCode.value || '-' },
      {
        label: '分段批次',
        value: currentPlan.batchNo || currentPlan.sourceProductionBatchNo || currentPlan.sourceBatchNo || '-',
      },
      { label: '胶板型号', value: glueBoardModel || '-' },
      { label: '胶板批次', value: glueBoardBatchNo || '-' },
      { label: '胶板料号', value: glueBoardMaterialCode || '-' },
      { label: '送检长度', value: sampleLength },
      { label: '送检时间', value: applyTime },
    ],
    inspectionType: '胶板送检',
    materialCode: glueBoardMaterialCode,
    productionBatchNo: currentPlan.batchNo || currentPlan.sourceProductionBatchNo || currentPlan.sourceBatchNo,
    sampleType: '胶板送检',
    templateCode: 'ADHESIVE2_GLUE_BOARD_INSPECTION',
    title: '胶板送检单',
  });
}

function printCoaInspectionTransferTicket(record?: MesHcAdhesiveConsoleApi.FaiSummary | Record<string, any> | null) {
  const isClickEvent = typeof Event !== 'undefined' && record instanceof Event;
  const inspection = isClickEvent ? latestCoaFai.value : record || latestCoaFai.value;
  const coaSliceNo = inspection?.productBatchNo || selectedCoaSliceDisplay.value || selectedCoaSliceNo.value;
  const applyTime = displayDateTimeText(inspection?.faiApplyTime || buildNowText());
  const glueBoardBatchNo = inspection?.glueBoardBatchNo || inspection?.gluePlateBatchNo || glueBoard.batchNo;
  const glueBoardMaterialCode = inspection?.glueBoardMaterialCode || getActualGlueBoardMaterialCode();
  return printAdhesive2InspectionTransferTicket(inspection, {
    applyTime,
    boardBatchNo: glueBoardBatchNo,
    boardMaterialCode: glueBoardMaterialCode,
    fields: [
      { label: '当前工序', value: '粘胶2' },
      {
        label: '产品型号',
        value: inspection?.productModel || currentProductModelCode.value || '-',
      },
      { label: '胶板料号', value: glueBoardMaterialCode || '-' },
      { label: '胶板批号', value: glueBoardBatchNo || '-' },
      { label: 'COA送检片号', value: coaSliceNo || '-' },
      { label: '送检时间', value: applyTime },
      { label: '送检数量', value: '1' },
    ],
    inspectionType: 'COA送检',
    productionBatchNo: coaSliceNo || currentPlan.batchNo,
    sampleType: 'COA送检片',
    templateCode: 'ADHESIVE2_COA_INSPECTION',
    title: 'COA送检单',
  });
}

function promptPrintGlueBoardInspectionTransferTicket(record: MesHcAdhesiveConsoleApi.FaiSummary | Record<string, any>) {
  AModal.confirm({
    cancelText: '暂不打印',
    content: `胶板送检已提交，单号：${resolveAdhesive2InspectionNo(record) || '-'}。是否立即打印胶板送检单？`,
    okText: '打印',
    onOk: () => printGlueBoardInspectionTransferTicket(record),
    title: '打印胶板送检单',
  });
}

function promptPrintCoaInspectionTransferTicket(record: MesHcAdhesiveConsoleApi.FaiSummary | Record<string, any>) {
  const inspectionNo = resolveAdhesive2InspectionNo(record);
  AModal.confirm({
    cancelText: '暂不打印',
    content: inspectionNo ? `COA送检已提交，单号：${inspectionNo}。是否立即打印COA送检流转单？` : 'COA送检已提交。是否立即打印COA送检流转单？',
    okText: '打印',
    onOk: () => printCoaInspectionTransferTicket(record),
    title: '打印COA送检流转单',
  });
}

function printProcessCheckInspectionTransferTicket(record: MesHcAdhesiveConsoleApi.FaiSummary | Record<string, any>) {
  const glueBoardBatchNo = record.glueBoardBatchNo || record.gluePlateBatchNo || glueBoard.batchNo;
  const glueBoardMaterialCode = record.glueBoardMaterialCode || getActualGlueBoardMaterialCode();
  return printAdhesive2InspectionTransferTicket(record, {
    boardBatchNo: glueBoardBatchNo,
    boardMaterialCode: glueBoardMaterialCode,
    extraFields: [
      {
        label: '分段批号',
        value: record.inspectionScopeBatchNo || currentMotherBatchNo.value,
      },
    ],
    fields: [
      { label: '当前工序', value: '粘胶2' },
      {
        label: '产品型号',
        value: record.productModel || currentProductModelCode.value || '-',
      },
      { label: '胶板料号', value: glueBoardMaterialCode || '-' },
      { label: '胶板批号', value: glueBoardBatchNo || '-' },
    ],
    inspectionType: '过程加检',
    sampleType: '粘胶2过程加检片',
    templateCode: 'ADHESIVE2_PROCESS_CHECK_INSPECTION',
  });
}

function printLatestProcessCheckInspectionTransferTicket() {
  return printProcessCheckInspectionTransferTicket(latestProcessCheckFai.value || {});
}

function isChangeoverInspectionReady() {
  return true;
}

function openRecordConfirm(record?: MesHcAdhesiveConsoleApi.ReportItem | Record<string, any>, initialScannedBatchNo = '') {
  activeRecord.value = (record as MesHcAdhesiveConsoleApi.ReportItem) || null;
  recordConfirmProcessing.value = false;
  recordConfirmForm.error = '';
  recordConfirmForm.message = '';
  recordConfirmForm.reportType = 'PRODUCT';
  recordConfirmForm.scannedBatchNo = initialScannedBatchNo;
  recordConfirmVisible.value = true;
  focusRecordConfirmScanInput();
}

function startSingleScanConfirm(record?: MesHcAdhesiveConsoleApi.ReportItem | Record<string, any>, scannedBatchNo = '') {
  openRecordConfirm(record, scannedBatchNo);
  nextTick(() => void confirmRecordScan());
}

function promptOpenExistingReportDetail(record: MesHcAdhesiveConsoleApi.ReportItem | Record<string, any>, scannedBatchNo?: string) {
  const sourceBatchNo = String(record.sourceProductionBatchNo || scannedBatchNo || record.productionBatchNo || '').trim();
  const displayBatchNo = sourceBatchNo || String(scannedBatchNo || '').trim() || '-';
  const reportStatus = String(record.reportStatus || '').toUpperCase();
  const blockedReason = String(record.editBlockedReason || '').trim();
  const readonly = reportStatus === 'SUBMITTED' || !!blockedReason || isCurrentTaskReadonly.value;
  const openExistingReport = async (viewOnly: boolean, overwriteExisting: boolean) => {
    const expectedBatchNo =
      resolveMotherBatchNo(record as Record<string, any>) ||
      resolveMotherBatchNo({
        productionBatchNo: scannedBatchNo,
        sourceProductionBatchNo: scannedBatchNo || sourceBatchNo,
      });
    if (expectedBatchNo && !findSourceGroup(expectedBatchNo)) {
      await loadSourceGroups(expectedBatchNo);
    }
    const matched = findSourceSegmentWithGroup(sourceBatchNo) || findSourceSegmentWithGroup(scannedBatchNo) || findSourceSegmentWithGroup(record.productionBatchNo);
    if (!matched) {
      AModal.warning({
        content: `已找到片号 ${displayBatchNo} 的报工记录，但当前工作台未加载到对应来源片，请重新扫码计划与片号后查看。`,
        okText: '知道了',
        onOk: focusPlanScanInput,
        title: '未加载到片号来源',
      });
      return;
    }
    await openReportDialog(matched.group, matched.segment, getAdhesive2SegmentReportType(matched.segment), sourceBatchNo || scannedBatchNo || matched.segment.batchNo, viewOnly, overwriteExisting);
  };
  if (readonly) {
    void openExistingReport(true, false);
    return;
  }
  AModal.confirm({
    cancelText: '关闭',
    content: `片号 ${displayBatchNo} 已存在粘胶2报工记录，可打开原记录修改检查结果并覆盖确认；不会重复扣减胶板或生成库存流水。`,
    okText: '打开覆盖修改',
    onCancel: focusPlanScanInput,
    onOk: () => openExistingReport(false, true),
    title: '片号已存在报工',
  });
}

function showDailyPreparationRequiredWarning() {
  AModal.warning({
    okText: '查看点检清洁',
    title: '请先填写今日点检/清洁记录',
    content: '粘胶2扫码确认前至少需要保存今日设备清洁点检记录，可暂不确认。',
    onOk: () => {
      dailyRecordListVisible.value = true;
    },
  });
}

function showCleaningMaintenanceRequiredForStartWarning() {
  AModal.warning({
    okText: '查看清洁保养',
    title: '请先填写今日清洁保养记录',
    content: '粘胶2开工前至少需要保存今日设备清洁保养记录，可暂不确认。',
    onOk: () => {
      dailyRecordListVisible.value = true;
    },
  });
}

async function ensureDailyPreparationReadyForReport() {
  await loadDailyRecords();
  if (dailyPreparationReady.value) return true;
  showDailyPreparationRequiredWarning();
  return false;
}

async function ensureCleaningMaintenanceReadyForStart(task: MesHcAdhesiveConsoleApi.TaskItem | Record<string, any>, equipment: BoardEquipment) {
  const rows = await loadDailyRecordsForStart(task, equipment);
  dailyRecordRows.value = rows;
  if (hasReadyCleaningMaintenanceRecord(rows)) return true;
  showCleaningMaintenanceRequiredForStartWarning();
  return false;
}

function showChangeoverSubmitRequiredWarning() {
  const motherBatchNo = getCurrentChangeoverMotherBatchNo();
  AModal.warning({
    okText: '上报工艺参数',
    title: '请先提交粘胶2工艺参数点检',
    content: `当前分段批号 ${motherBatchNo || '-'}${getCurrentChangeoverInstructionId() ? ` 的本次换型 ${modelOverride.changeoverInstructionNo || ''}` : ''} 还没有提交粘胶2工艺参数点检记录，请先上报并保存工艺参数后再扫码确认。`,
    onOk: () => {
      openChangeoverInspectionScan();
    },
  });
}

async function ensureCurrentMotherChangeoverSubmittedForReport() {
  if (!currentPlan.planOperationId) {
    AModal.warning({
      title: '请先加载粘胶2计划',
      content: '扫码确认前需要先加载当前粘胶2计划。',
    });
    return false;
  }
  if (changeoverSubmitting.value) {
    message.info('粘胶2工艺参数点检记录正在保存，请保存完成后再扫码确认');
    return false;
  }
  const contextKey = getChangeoverPlanContextKey();
  const instruction = await loadChangeoverInstruction();
  if (instruction === undefined) {
    message.warning('换型状态刷新失败，请刷新后重新报工');
    return false;
  }
  if (contextKey !== getChangeoverPlanContextKey()) return false;
  await loadChangeoverInspections();
  if (contextKey !== getChangeoverPlanContextKey()) return false;
  if (hasCurrentMotherChangeoverInspection()) return true;
  showChangeoverSubmitRequiredWarning();
  return false;
}

async function openSelectedRecordConfirm() {
  if (!sourceGroups.value.some((group) => group.segments.length > 0)) {
    AModal.info({
      title: '暂无来源片',
      content: '请先扫码计划号，系统会按已确认压槽片号加载工作台。',
    });
    return;
  }
  if (isCurrentTaskReadonly.value) {
    showReadonlyTaskWarning('扫码确认');
    return;
  }
  if (!ensureGlueBoardReadyForAction('扫码确认')) return;
  if (!(await ensureDailyPreparationReadyForReport())) return;
  if (!(await ensureCurrentMotherChangeoverSubmittedForReport())) return;
  if (!isChangeoverInspectionReady()) {
    showChangeoverInspectionRequiredWarning();
    return;
  }
  openRecordConfirm();
}

async function confirmRecordScan() {
  if (recordConfirmProcessing.value) return;
  recordConfirmProcessing.value = true;
  try {
    const scannedBatchNo = resolveTransferTicketQrBusinessNo(normalizeConfirmScanCode(recordConfirmForm.scannedBatchNo));
    if (!scannedBatchNo) {
      recordConfirmForm.error = '请扫码或输入粘胶2流转单片号。';
      recordConfirmForm.message = '';
      return;
    }
    const selectedReportType: Adhesive2ReportType = 'PRODUCT';
    recordConfirmForm.reportType = selectedReportType;
    const matched = findSourceSegmentWithGroup(scannedBatchNo);
    if (!matched) {
      recordConfirmForm.error = '未在当前工作台中找到该片号，请检查计划或重新扫码。';
      recordConfirmForm.message = '';
      return;
    }
    const existedRecord = findReportBySegment(matched.segment);
    // 连续扫码时不能沿用上一片（或列表预读的下一片）的记录，否则会把本次扫码
    // 错误提交给旧记录，导致该片只保存或因片号不一致无法确认。
    if (activeRecord.value?.id && !isReportMatchedByScanBatch(activeRecord.value, scannedBatchNo)) {
      activeRecord.value = null;
    }
    const overwriteConfirmed = String(existedRecord?.reportStatus || '').toUpperCase() === 'CONFIRMED';
    if (!overwriteConfirmed && !(await ensureAdhesive2SampleAbnormalUnlocked(matched.segment, activeRecord.value?.id ? '扫码确认' : '新建报工', matched.group.baseBatchNo))) {
      recordConfirmForm.scannedBatchNo = '';
      return;
    }
    if (!activeRecord.value?.id && existedRecord) {
      if (hasAdhesive2TailSelection(existedRecord) && !isAdhesiveRecordConfirmed(existedRecord.reportStatus)) {
        activeRecord.value = existedRecord;
      } else {
        recordConfirmVisible.value = false;
        promptOpenExistingReportDetail(existedRecord, scannedBatchNo);
        return;
      }
    }
    if (activeRecord.value?.id && !isAdhesiveRecordConfirmed(activeRecord.value.reportStatus) && !hasAdhesive2TailSelection(activeRecord.value)) {
      recordConfirmForm.error = '请先在右上角点击“一键选择尾号”或“扫码改尾号”，选择实际尺寸后再扫码确认。';
      recordConfirmForm.message = '';
      return;
    }
    if (!activeRecord.value?.id && (isSourceGroupCompleted(matched.group) || isSegmentCompleted(matched.segment))) {
      recordConfirmForm.error = '当前粘胶2分段已完工，仅可查看，不能新建或扫码确认。';
      recordConfirmForm.message = '';
      return;
    }
    if (!overwriteConfirmed && !(await ensureStrictGlueBoardMatch())) return;
    if (!ensureGlueBoardReadyForAction(activeRecord.value?.id ? '扫码确认' : '新建报工')) return;
    if (!(await ensureCurrentMotherChangeoverSubmittedForReport())) {
      if (changeoverSubmitting.value) {
        recordConfirmForm.message = '粘胶2工艺参数点检记录正在保存，请保存完成后再扫码确认。';
        return;
      }
      recordConfirmVisible.value = false;
      return;
    }
    if (!isChangeoverInspectionReady()) {
      recordConfirmVisible.value = false;
      showChangeoverInspectionRequiredWarning(scannedBatchNo);
      return;
    }
    if (!activeRecord.value?.id && !normalizeSizeRule(matched.segment.actualSizeRule)) {
      recordConfirmForm.error = '请先在右上角点击“一键选择尾号”或“扫码改尾号”，选择实际尺寸后再扫码确认。';
      recordConfirmForm.message = '';
      return;
    }
    // 单片扫码先打开既有外观/NG填写页。此处不建单、不确认，最终由该页一次保存并确认。
    recordConfirmVisible.value = false;
    await openReportDialog(
      matched.group,
      matched.segment,
      selectedReportType,
      scannedBatchNo,
      false,
      Boolean(activeRecord.value?.id),
      true,
    );
  } finally {
    recordConfirmProcessing.value = false;
  }
}

async function loadSourceGroups(expectedBatchNo?: string) {
  if (!currentPlan.planId || !currentPlan.planOperationId) {
    sourceGroups.value = [];
    coaFaiRows.value = [];
    processCheckFaiRows.value = [];
    sourcePressSlotFirstInspectionRows.value = [];
    sourcePressSlotProcessCheckRows.value = [];
    return;
  }
  const loadSeq = ++sourceGroupsLoadSeq;
  const planId = currentPlan.planId;
  const planOperationId = currentPlan.planOperationId;
  const requestedBatchNo = expectedBatchNo || currentMotherBatchNo.value || currentPlan.sourceBatchNo || currentPlan.batchNo || '';
  const selectedSourceBatchNo = normalizeAdhesive2MiddleBatchNo(
    resolveMotherBatchNo({
      batchNo: requestedBatchNo,
      parentProductionBatchNo: requestedBatchNo,
      productionBatchNo: requestedBatchNo,
      sourceBatchNo: requestedBatchNo,
      sourceProductionBatchNo: requestedBatchNo,
    }) || requestedBatchNo,
  );
  const rows = normalizeRows(await getAdhesiveConsoleSourceList(planId, planOperationId, selectedSourceBatchNo));
  if (loadSeq !== sourceGroupsLoadSeq || planId !== currentPlan.planId || planOperationId !== currentPlan.planOperationId) return;
  buildSourceGroupsFromSources(rows);
  await loadSourcePressSlotFaiRows(loadSeq);
  if (loadSeq !== sourceGroupsLoadSeq || planId !== currentPlan.planId || planOperationId !== currentPlan.planOperationId) return;
  const selectedGroup = selectedSourceBatchNo ? sourceGroups.value.find((group) => normalizeAdhesive2MiddleBatchNo(group.baseBatchNo) === selectedSourceBatchNo) : sourceGroups.value[0];
  const nextSourceBatchNo = selectedGroup?.baseBatchNo || selectedSourceBatchNo;
  currentPlan.sourceBatchNo = nextSourceBatchNo;
  currentPlan.sourceProductionBatchNo = nextSourceBatchNo;
  currentPlan.batchNo = currentPlan.sourceBatchNo;
  currentPlan.availableSourceLength = selectedGroup ? getAdhesive2GroupAvailableLength(selectedGroup) : !selectedSourceBatchNo ? sourceGroups.value.reduce((sum, group) => sum + getAdhesive2GroupAvailableLength(group), 0) : 0;
}

async function reloadWorkbenchForLoadedPlan() {
  if (!currentPlan.planId || !currentPlan.planOperationId) return;
  syncWorkOrderBlockedReason();
  await loadLatestPlanInstruction();
  await loadChangeoverInstruction();
  await Promise.all([loadDailyRecords(), loadReports()]);
  await loadCoaFaiRows();
  await loadSourceGroups();
  await loadChangeoverInspections();
  await loadAdhesive2FaiSummary();
  await loadProcessCheckFaiRows();
  await loadAdhesive2IntermediateRecords();
}

async function refreshAdhesive2Workbench() {
  if (!currentPlan.planId || !currentPlan.planOperationId) {
    message.warning('请先扫描或选择粘胶2计划');
    return;
  }
  if (workbenchRefreshing.value) return;
  workbenchRefreshing.value = true;
  try {
    await reloadWorkbenchForLoadedPlan();
    // message.success('粘胶2工作台已刷新');
  } catch (error: any) {
    message.error(error?.message || '粘胶2工作台刷新失败');
  } finally {
    workbenchRefreshing.value = false;
  }
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
    const equipment = getEffectiveBoardEquipment();
    const rows = normalizeRows(
      await getAdhesiveConsoleTaskList({
        equipmentCode: equipment.id ? undefined : equipment.code || undefined,
        equipmentId: equipment.id,
        taskKeyword: planNo,
        taskStatus: 'ALL',
      }),
    );
    const existingReportMatch = scannerSliceBatchNo ? await findExistingAdhesive2ReportBySlice(rows, planNo, scannerSliceBatchNo) : null;
    let scannerSourceMotherBatchNo = '';
    let scannerSourcePlanNo = '';
    if (scannerSliceBatchNo && !existingReportMatch) {
      try {
        const source = await scanAdhesiveConsoleSource(scannerSliceBatchNo);
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
        scannerSourceMotherBatchNo = resolveAdhesive2SourceMotherBatchNo(source, scannerSliceBatchNo);
      } catch (error: any) {
        clearPlanScannerState();
        showSinglePlanScanWarning({
          content: error?.message || `未找到片号 ${scannerSliceBatchNo} 对应的已确认压槽来源，扫码计划框已清空，请确认后重新扫码。`,
          okText: '知道了',
          onOk: focusPlanScanInput,
          title: '扫码片号未找到',
        });
        return;
      }
    }
    let task = (existingReportMatch?.task || findTaskByPlanAndMother(rows, planNo, scannerSourceMotherBatchNo)) as MesHcAdhesiveConsoleApi.TaskItem | undefined;
    if (!task?.planOperationId && scannerSliceBatchNo && scannerSourceMotherBatchNo) {
      task = findTaskByPlanAndMother(rows, planNo) as MesHcAdhesiveConsoleApi.TaskItem | undefined;
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
            ? `未找到计划号 ${planNo} 且分段批号 ${scannerSourceMotherBatchNo} 同时匹配的粘胶2工单，扫码计划框已清空，请确认后重新扫码。`
            : `未找到计划号 ${planNo} 对应的粘胶2工单，请确认计划号或工序任务状态。`,
          okText: '知道了',
          onOk: focusPlanScanInput,
          title: '扫码计划未找到',
        });
      }
      return;
    }
    const taskStatus = normalizeWorkOrderStatus(task.status);
    if (scannerSliceBatchNo && !existingReportMatch && taskStatus === 'FINISHED') {
      clearPlanScannerState();
      showSinglePlanScanWarning({
        content: `计划号 ${planNo} 对应粘胶2工单已完工，片号 ${scannerSliceBatchNo} 不能再新建粘胶2报工。`,
        okText: '知道了',
        onOk: focusPlanScanInput,
        title: '粘胶2工单已完工',
      });
      return;
    }
    if (taskStatus !== 'FINISHED' && !(await ensureAdhesive2SampleAbnormalUnlocked(scannerSliceBatchNo || undefined, '扫码加载', scannerSourceMotherBatchNo || resolveMotherBatchNo(task as Record<string, any>)))) {
      return;
    }
    if (taskStatus !== 'FINISHED' && !(await ensureAdhesive2TaskEquipmentForBoard(task))) {
      if (scannerInput) {
        clearPlanScannerState();
        focusPlanScanInput();
      }
      return;
    }
    if (taskStatus === 'FINISHED' && !selectedBoardEquipmentId.value && !selectedBoardEquipmentCode.value && (task.equipmentId || task.equipmentCode)) {
      applyBoardEquipmentFromTask(task);
    }
    applyTask(task);
    await loadLatestPlanInstruction();
    await loadChangeoverInstruction();
    const mapCandidates = await fetchGlueBoardMapCandidatesForProductModel(currentProductModelCode.value || task.modelCode);
    applySelectedGlueBoardModel(modelOverride.glueBoardModel || task.glueBoardModel || currentPlan.glueBoardModel || getDefaultGlueBoardModel(currentProductModelCode.value || task.materialCode));
    glueBoardMapCandidates.value = mapCandidates;
    applyPreferredGlueBoardModelFromMap();
    await loadCheckTemplate();
    await loadGlueBoardUsage();
    syncWorkOrderBlockedReason(task.status);
    if (!(await startAdhesive2WorkOrderIfNeeded(task, taskStatus))) {
      if (scannerInput) {
        clearPlanScannerState();
        focusPlanScanInput();
      }
      return;
    }
    await Promise.all([loadDailyRecords(), loadReports()]);
    await loadSourceGroups(existingReportMatch?.report ? resolveMotherBatchNo(existingReportMatch.report as Record<string, any>) : scannerSourceMotherBatchNo || resolveMotherBatchNo(task as Record<string, any>));
    await loadChangeoverInspections();
    await loadAdhesive2FaiSummary();
    await loadAdhesive2IntermediateRecords();
    if (existingReportMatch) {
      clearPlanScannerState();
      if (isAdhesiveRecordConfirmed(existingReportMatch.report.reportStatus)) {
        promptOpenExistingReportDetail(existingReportMatch.report, scannerSliceBatchNo);
      } else {
        startSingleScanConfirm(existingReportMatch.report, scannerSliceBatchNo);
      }
      return;
    }
    if (scannerSliceBatchNo) {
      clearPlanScannerState();
      const matchedSource = findSourceSegmentWithGroup(scannerSliceBatchNo);
      if (!matchedSource) {
        showSinglePlanScanWarning({
          content: `片号 ${scannerSliceBatchNo} 已确认且属于计划 ${planNo}，但当前粘胶2工作台未加载到该片号来源，请刷新后重新扫码。`,
          okText: '知道了',
          onOk: focusPlanScanInput,
          title: '未加载到片号来源',
        });
        return;
      }
      currentPlan.sourceBatchNo = matchedSource.group.baseBatchNo;
      currentPlan.sourceProductionBatchNo = matchedSource.group.baseBatchNo;
      currentPlan.batchNo = matchedSource.group.baseBatchNo;
      currentPlan.availableSourceLength = getAdhesive2GroupAvailableLength(matchedSource.group);
      if (!ensureGlueBoardReadyForAction('扫码确认')) return;
      startSingleScanConfirm(undefined, scannerSliceBatchNo);
    } else {
      lastAutoScannedPlanNo.value = task.planNo || planNo;
      scanPlanNo.value = task.planNo || planNo;
      focusPlanScanInput();
    }
    // if (!silent) message.success('已带出粘胶2计划、工作准备和可加工来源');
  } finally {
    planScanInFlight = false;
    boardLoading.value = false;
  }
}

const isGlobalScannerCandidate = (value: string) => {
  const text = value.trim();
  if (!text) return false;
  if (recordConfirmVisible.value || selectedTailVisible.value) return text.length >= 3;
  return hasPlanScanDelimiter(text) || normalizePlanScanNo(text).length >= PLAN_SCAN_MIN_LENGTH;
};

const routeGlobalScannerInput = (value: string) => {
  if (selectedTailVisible.value) {
    if (selectedTailSubmitting.value) return;
    selectedTailScanCode.value = value.trim();
    focusInputRef(selectedTailScanInputRef);
    void scanSelectedTail();
    return;
  }
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
  if (event.ctrlKey || event.altKey || event.metaKey || event.isComposing || isEventFromScannerInput(event) || isEditableEventTarget(event.target)) {
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
  const inputs = Array.from(activePane.querySelectorAll<HTMLInputElement>('input.adhesive-check-cell'));
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
  const inputs = Array.from(document.querySelectorAll<HTMLInputElement>('.adhesive-intermediate-cell input, input.adhesive-intermediate-cell'));
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
  return authPayload?.empName || authPayload?.nickname || authPayload?.username || currentUserName.value || '';
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
  const context = getDailyRecordContext() as Record<string, any> | undefined;
  if (!operator) {
    message.warning('请先完成记录人/确认人身份确认');
    return;
  }
  if (!context?.equipmentId && !selectedBoardEquipmentCode.value) {
    message.warning('请先选择粘胶2设备，再保存今日点检/清洁记录');
    return;
  }
  if (!context?.planId || !context?.planOperationId) {
    message.warning('请先扫描或选择一个粘胶2计划，用于关联今日设备点检记录');
    return;
  }
  const existingRecorder = row.recorder === '-' ? operator : row.recorder;
  const recorder = action.mode === 'edit' ? operator : existingRecorder;
  const existingRecorderTime = row.recorderTime === '-' ? now : row.recorderTime;
  const recorderTime = action.mode === 'confirm' ? existingRecorderTime : now;
  const existingConfirmer = row.confirmer === '-' ? undefined : row.confirmer;
  const confirmer = action.mode === 'confirm' ? operator : existingConfirmer;
  const existingConfirmerTime = row.confirmerTime === '-' ? undefined : row.confirmerTime;
  const confirmerTime = action.mode === 'confirm' ? now : existingConfirmerTime;
  const payload: MesHcAdhesiveConsoleApi.PassWorkSaveReq = {
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
    equipmentCode: context.equipmentCode || selectedBoardEquipmentCode.value || undefined,
    equipmentId: context.equipmentId || selectedBoardEquipmentId.value,
    equipmentName: context.equipmentName || selectedBoardEquipmentName.value || undefined,
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
    await confirmAdhesiveConsolePassWork(payload);
  } else {
    await saveAdhesiveConsolePassWork(payload);
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
  const context = getDailyRecordContext() as Record<string, any> | undefined;
  if (!context?.equipmentId && !selectedBoardEquipmentCode.value) {
    openDailyRecordAfterEquipmentSelected.value = true;
    message.warning('请先选择粘胶2设备，再保存今日点检/清洁记录');
    await openEquipmentSelect();
    return;
  }
  if (!context?.planId || !context?.planOperationId) {
    message.warning('请先扫描或选择一个粘胶2计划，用于关联今日设备点检记录');
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
    return false;
  }
  await loadGlueBoardMapCandidates();
  const glueBoardModel = glueBoardMapCandidateModels.value.length === 1 ? glueBoardMapCandidateModels.value[0] || '' : '';
  const stock = await getAdhesiveConsoleGlueBoardStockByBatch(scanCode, glueBoardModel);
  const source = (stock as any)?.data ?? stock;
  return await applyGlueBoardStock(source, scanCode);
}

function getGlueBoardUseLengthPerScan(availableLength?: number) {
  const stockLength = Number(availableLength || 0);
  if (stockLength > 0) return Math.min(ADHESIVE2_GLUE_BOARD_USE_LENGTH_PER_SCAN, stockLength);
  return ADHESIVE2_GLUE_BOARD_USE_LENGTH_PER_SCAN;
}

async function applyGlueBoardStock(source: MesHcAdhesiveConsoleApi.GlueBoardStock | Record<string, any> | null | undefined, fallbackBatchNo = '') {
  const currentGlueBoardModel = String(getSelectedGlueBoardModel() || '').trim();
  if (!source?.id) {
    const lookupModels = glueBoardMapCandidateText.value;
    glueBoard.alarm = lookupModels ? `未找到推荐胶板 ${lookupModels}、批号 ${fallbackBatchNo} 的边库批次，请先在胶板边料管理中领料登记。` : '未找到该胶板边库批次，请先在胶板边料管理中领料登记。';
    message.warning(glueBoard.alarm);
    return false;
  }
  const stockGlueBoardModel = String(source.glueBoardModel || '').trim();
  const availableLength = Number(source.availableLength || 0);
  if (availableLength <= 0) {
    glueBoard.alarm = '该胶板边库批次已无可用长度，请更换批次。';
    message.warning(glueBoard.alarm);
    return false;
  }
  const receiveLength = availableLength;
  glueConsumeForm.stockId = source.id;
  glueConsumeForm.materialScanCode = source.glueBoardBatchNo || fallbackBatchNo;
  glueConsumeForm.batchNo = source.glueBoardBatchNo || fallbackBatchNo;
  glueConsumeForm.receiveStartPosition = Number(source.availableStartPosition || 0);
  glueConsumeForm.receiveLength = receiveLength;
  glueConsumeForm.stockAvailableLength = availableLength;
  glueBoard.stockId = source.id;
  glueBoard.materialCode = source.glueBoardMaterialCode || '';
  glueBoard.model = stockGlueBoardModel || currentGlueBoardModel || '';
  glueBoard.batchNo = source.glueBoardBatchNo || '';
  glueBoard.receiveStartPosition = Number(source.receiveStartPosition || 0);
  glueBoard.receiveLength = receiveLength;
  glueBoard.availableStartPosition = Number(source.availableStartPosition || 0);
  glueBoard.stockLength = availableLength;
  glueBoard.inspectionSubmitTime = source.inspectionSubmitTime || '';
  glueBoard.latestInspectionId = source.latestInspectionId;
  glueBoard.latestInspectionNo = source.latestInspectionNo || '';
  glueBoard.latestInspectionResult = source.latestInspectionResult || '';
  glueBoard.qualityStatus = source.qualityStatus || '';
  applyGlueBoardInspectionSummary();
  await loadLatestGlueBoardFaiForCurrentSegment({ applyEmpty: false });
  const mismatchTip = buildGlueBoardMapMismatchTip(source);
  if (mismatchTip) {
    glueBoard.alarm = mismatchTip;
    message.warning(mismatchTip);
  } else {
    glueBoard.alarm = '';
  }
  return true;
}

async function loadGlueStockRows(options: { resetPage?: boolean } = {}) {
  if (options.resetPage) {
    glueStockPagination.pageNo = 1;
  }
  const glueBoardModel = String(glueStockFilter.model || '').trim();
  const glueBoardBatchNo = String(glueStockFilter.batchNo || '').trim();
  const glueBoardMaterialCode = String(glueStockFilter.materialCode || '').trim();
  const equipment = getEffectiveBoardEquipment();
  glueStockSelectLoading.value = true;
  try {
    const page = await getAdhesiveConsoleGlueBoardStockPage({
      candidateGlueBoardModels: glueBoardMapCandidateModelQuery.value || undefined,
      equipmentCode: equipment.id ? undefined : equipment.code || undefined,
      equipmentId: equipment.id,
      glueBoardBatchNo: glueBoardBatchNo || undefined,
      glueBoardMaterialCode: glueBoardMaterialCode || undefined,
      glueBoardModel: glueBoardModel || undefined,
      pageNo: glueStockPagination.pageNo,
      pageSize: glueStockPagination.pageSize,
    });
    glueStockRows.value = normalizeRows(page).filter((row) => Number(row.availableLength || 0) > 0);
    glueStockPagination.total = normalizePageTotal(page);
  } finally {
    glueStockSelectLoading.value = false;
  }
}

async function openGlueStockSelect(options?: unknown) {
  if (isCurrentTaskReadonly.value) {
    showReadonlyTaskWarning('选择胶板批次');
    return;
  }
  if (!currentPlan.planId && !currentPlan.planOperationId && !selectedBoardEquipmentId.value && !selectedBoardEquipmentCode.value) {
    message.warning('未选择工单时，请先选择粘胶2机台，再选择胶板批次');
    await openEquipmentSelect();
    return;
  }
  const clearBatchFilter = Boolean(options && typeof options === 'object' && 'clearBatchFilter' in options ? (options as { clearBatchFilter?: boolean }).clearBatchFilter : false);
  await loadGlueBoardMapCandidates();
  glueStockFilter.batchNo = clearBatchFilter ? '' : String(glueConsumeForm.materialScanCode || glueConsumeForm.batchNo || '').trim();
  glueStockFilter.materialCode = '';
  glueStockFilter.model = glueBoardMapCandidateModels.value.length === 1 ? glueBoardMapCandidateModels.value[0] || '' : '';
  glueStockSelectVisible.value = true;
  await loadGlueStockRows({ resetPage: true });
}

async function selectGlueBoardStock(record: MesHcAdhesiveConsoleApi.GlueBoardStock) {
  if (glueBoard.id && Number(glueBoard.stockId || 0) === Number(record.id || 0)) {
    glueStockSelectVisible.value = false;
    await loadGlueBoardUsage();
    message.success(`继续使用胶板批次 ${record.glueBoardBatchNo || '-'}`);
    return;
  }
  const previousGlueBoard = { ...glueBoard };
  const applied = await applyGlueBoardStock(record, record.glueBoardBatchNo || '');
  if (!applied) {
    return;
  }
  glueStockSelectVisible.value = false;
  glueConsumeVisible.value = false;
  try {
    await confirmGlueConsume();
  } catch (error) {
    Object.assign(glueBoard, previousGlueBoard);
    throw error;
  }
}

async function handleGlueStockPaginationChange(pageNo: number, pageSize: number) {
  glueStockPagination.pageNo = Number(pageNo || 1);
  glueStockPagination.pageSize = Number(pageSize || glueStockPagination.pageSize || 10);
  await loadGlueStockRows();
}

async function openGlueConsumeDialog() {
  if (isCurrentTaskReadonly.value) {
    showReadonlyTaskWarning('胶板领用');
    return;
  }
  glueConsumeForm.stockId = undefined;
  glueConsumeForm.materialScanCode = '';
  glueConsumeForm.batchNo = '';
  glueConsumeForm.receiveStartPosition = 0;
  glueConsumeForm.receiveLength = undefined;
  glueConsumeForm.stockAvailableLength = 0;
  glueConsumeVisible.value = false;
  await openGlueStockSelect({ clearBatchFilter: true });
}

async function confirmGlueConsume() {
  if (isCurrentTaskReadonly.value) {
    showReadonlyTaskWarning('胶板领用');
    return;
  }
  const receiveLength = Number(glueConsumeForm.receiveLength || 0);
  if (receiveLength <= 0) {
    message.warning('请填写本次领用长度');
    return;
  }
  const materialCode = String(getActualGlueBoardMaterialCode() || ADHESIVE_DEFAULT_GLUE_BOARD_MATERIAL_CODE).trim();
  const batchNo = String(glueConsumeForm.batchNo || glueBoard.batchNo || '').trim();
  if (!materialCode || !batchNo) {
    message.warning('请填写胶板型号和胶板批号');
    return;
  }
  const equipment = getEffectiveBoardEquipment();
  if (!currentPlan.planId && !currentPlan.planOperationId && !equipment.id && !equipment.code) {
    message.warning('未选择工单时，请先选择粘胶2机台，再确认胶板领用');
    await openEquipmentSelect();
    return;
  }
  const usageResult = await saveAdhesiveConsoleGlueBoardUsage({
    equipmentCode: equipment.code || undefined,
    equipmentId: equipment.id,
    equipmentName: equipment.name || undefined,
    glueBoardBatchNo: batchNo,
    glueBoardMaterialCode: materialCode,
    glueBoardStockId: glueConsumeForm.stockId || glueBoard.stockId,
    operationCode: 'ADHESIVE2',
    operationName: '粘胶2',
    planId: currentPlan.planId || undefined,
    planOperationId: currentPlan.planOperationId || undefined,
    receiveLength,
    receiveStartPosition: Number(glueConsumeForm.receiveStartPosition || 0),
    recordDate: dayjs().format('YYYY-MM-DD'),
    recorderName: currentUserName.value || undefined,
    recorderTime: buildNowText(),
  });
  const usageId = Number((usageResult as any)?.data ?? usageResult ?? 0);
  if (usageId > 0) {
    glueBoard.id = usageId;
  }
  await loadGlueBoardUsage();
  await glueBoardEdgeConsumableTabRef.value?.reload?.();
  glueConsumeVisible.value = false;
  await reloadWorkbenchForLoadedPlan();
  message.success('胶板领用记录已保存');
  message.warning('胶板领用前100m，建议完成胶板送检。');
  if (pendingGlueBoardInspectionAfterConsume.value) {
    pendingGlueBoardInspectionAfterConsume.value = false;
    openGlueBoardAqcDialog();
  }
}

function openGlueBoardAqcDialog() {
  if (isCurrentTaskReadonly.value) {
    showReadonlyTaskWarning('提交胶板送检');
    return;
  }
  if (!glueBoard.id || !glueBoard.stockId || !glueBoard.batchNo) {
    pendingGlueBoardInspectionAfterConsume.value = true;
    message.info('请先选择胶板边库批次完成领用，系统随后将直接打开胶板送检。');
    void openGlueConsumeDialog();
    return;
  }
  const submittedToday = glueBoard.inspectionSubmitTime && dayjs(glueBoard.inspectionSubmitTime).isSame(dayjs(), 'day');
  if (submittedToday && firstInspection.value.faiId && !isFirstInspectionReapplyAllowed()) {
    message.info('当前胶板已提交检验；只有检验结果 NG 时才允许重新送检。');
    goGlueBoardInspectionTab();
    return;
  }
  glueBoardFaiForm.sampleStartPosition = Number(glueBoard.availableStartPosition || glueBoard.receiveStartPosition || 0);
  glueBoardFaiForm.sampleLength = undefined;
  glueBoardFaiVisible.value = true;
}

function getGlueBoardAqcRepeatBlockMessage() {
  const latestTask = glueBoard.latestAqcTask;
  if (!latestTask?.id) return '';
  const status = String(latestTask.taskStatus || '').toUpperCase();
  if (glueBoardAqcAbnormalStatuses.has(status)) {
    return '当前胶板已有异常检验记录，请走复检/处理流程';
  }
  if (!status || glueBoardAqcSubmittedStatuses.has(status)) {
    return '当前胶板已提交检验，请勿重复送检';
  }
  return '当前胶板已存在检验记录，请勿重复送检';
}

async function submitGlueBoardAqc() {
  if (isCurrentTaskReadonly.value) {
    showReadonlyTaskWarning('提交胶板送检');
    return;
  }
  if (!glueBoard.id) {
    message.warning('请先登记本次胶板领用信息');
    return;
  }
  const blockMessage = getGlueBoardAqcRepeatBlockMessage();
  if (blockMessage) {
    message.warning(blockMessage);
    return;
  }
  const sampleLength = Number(aqcForm.sampleLength || 0);
  if (sampleLength <= 0) {
    message.warning('请填写胶板送检长度');
    return;
  }
  if (glueBoard.stockLength > 0 && sampleLength > Number(glueBoard.stockLength || 0)) {
    message.warning(`送检长度不能超过当前边库余量 ${formatNumber(glueBoard.stockLength)} m`);
    return;
  }
  if (aqcSubmitting.value) return;
  aqcSubmitting.value = true;
  try {
    const task = await submitAdhesiveConsoleAqcTask({
      id: aqcForm.id,
      glueBoardBatchNo: glueBoard.batchNo,
      glueBoardMaterialCode: getActualGlueBoardMaterialCode(),
      glueBoardUsageId: glueBoard.id,
      planId: currentPlan.planId || undefined,
      planOperationId: currentPlan.planOperationId || undefined,
      recordDate: dayjs().format('YYYY-MM-DD'),
      sampleLength,
      sampleStartPosition: Number(aqcForm.sampleStartPosition || 0),
      submitterName: currentUserName.value || undefined,
      submitTime: buildNowText(),
      taskType: 'DAILY_GLUE_BOARD',
    });
    aqcForm.id = task?.id;
    await loadGlueBoardUsage();
    await glueBoardEdgeConsumableTabRef.value?.reload?.();
    await reloadWorkbenchForLoadedPlan();
    aqcVisible.value = false;
    message.success(`胶板送检已提交，已按 ${formatNumber(sampleLength)} m 扣减边库余量`);
  } finally {
    aqcSubmitting.value = false;
  }
}

function openGlueBoardLossDialog() {
  if (isCurrentTaskReadonly.value) {
    showReadonlyTaskWarning('胶板损耗报备');
    return;
  }
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

async function submitGlueBoardLoss() {
  if (isCurrentTaskReadonly.value) {
    showReadonlyTaskWarning('胶板损耗报备');
    return;
  }
  if (!glueBoard.id) {
    message.warning('请先登记本次胶板领用信息');
    return;
  }
  const startPosition = Number(glueLossForm.startPosition || 0);
  const lossLength = Number(glueLossForm.lossLength || 0);
  if (lossLength <= 0) {
    message.warning('请填写大于0的损耗长度');
    return;
  }
  const endPosition = Number((startPosition + lossLength).toFixed(3));
  AModal.confirm({
    title: '确认保存胶板损耗报备',
    content: `本次损耗长度 ${formatNumber(lossLength)} m，保存后会扣减当前胶板可用长度。`,
    okText: '确认保存',
    cancelText: '取消',
    onOk: async () => {
      await reportAdhesiveConsoleGlueBoardLoss({
        endPosition,
        glueBoardUsageId: glueBoard.id!,
        lossLength,
        lossReason: glueLossForm.lossReason || undefined,
        startPosition,
      });
      await loadGlueBoardUsage();
      await glueBoardEdgeConsumableTabRef.value?.reload?.();
      glueLossVisible.value = false;
      message.success('胶板损耗已报备');
    },
  });
}

function resetReportForm() {
  Object.assign(reportForm, {
    aqcStatus: '',
    aqcTaskId: undefined,
    defectCode: '',
    endTime: '',
    glueBoardBatchNo: glueBoard.batchNo,
    glueBoardMaterialCode: getActualGlueBoardMaterialCode(),
    glueBoardModel: getActualGlueBoardModel(),
    glueBoardStartPosition: Number(glueBoard.availableStartPosition || 0),
    glueBoardUsageId: glueBoard.id,
    glueBoardUseLength: 0,
    lossLength: 0,
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
    sourceProductionBatchNo: '',
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
  adhesive2ProcessCheckItems.value = adhesive2ProcessCheckItems.value.map((item) => ({
    ...item,
    abnormalRemark: '',
    actualValue: item.itemName === '检测时间' ? buildNowText() : item.itemName === '反馈状态' ? '待检' : '',
    checkResult: 'OK',
  }));
  adhesive2MiddleCheckItems.value = adhesive2MiddleCheckItems.value.map((item) => ({
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
    reportForm.sourceScanError = '请先扫描或输入压槽已确认片号。';
    return false;
  }
  try {
    const matchedSource = findSourceSegmentWithGroup(sourceCode)?.segment;
    const source = await scanAdhesiveConsoleSource(matchedSource?.batchNo || sourceCode);
    const sourceProductionBatchNo = source.productionBatchNo || sourceCode;
    const parentBatchNo = source.motherBatchNo || source.parentProductionBatchNo || stripSegmentMark(sourceCode);
    if (!isConfirmedReportOverwrite.value && !(await ensureAdhesive2SampleAbnormalUnlocked(sourceProductionBatchNo, '来源扫码', parentBatchNo))) {
      reportForm.sourceScanError = '来源批次存在前置留样NG异常锁定，等待复检OK后才能继续粘胶2。';
      reportForm.sourceScanMessage = '';
      return false;
    }
    reportForm.sourceGrindingSecondDetailId = source.grindingSecondDetailId;
    reportForm.sourceProductionBatchNo = sourceProductionBatchNo;
    reportForm.parentBatchNo = parentBatchNo;
    const sourceSegment = findSourceSegment(sourceProductionBatchNo);
    const selectedActualSizeRule = normalizeSizeRule(sourceSegment?.actualSizeRule || source.actualSizeRule);
    reportForm.productionBatchNo = modelOverride.enabled
      ? getAdhesive2TailBatchNo(reportForm.sourceProductionBatchNo, selectedActualSizeRule)
      : reportForm.productionBatchNo || getAdhesive2TailBatchNo(reportForm.sourceProductionBatchNo, selectedActualSizeRule);
    const sourceLength = Number(source.outputLength || source.processLength || reportForm.processLength || 0);
    const availableRange = sourceSegment ? findFirstAvailableRange(sourceSegment) : { end: sourceLength, length: sourceLength, start: 0 };
    reportForm.startPosition = availableRange.start;
    reportForm.endPosition = availableRange.end;
    reportForm.processLength = availableRange.length;
    reportForm.outputLength = reportOutputLength.value;
    reportForm.sourceScanMessage = '来源已通过粘胶来源扫码接口带出。';
    return true;
  } catch (error) {
    reportForm.sourceScanError = '来源扫码接口未找到该批次，当前只能作为视觉原型预览，不能提交后台。';
    return false;
  }
}

async function openReportDialog(
  group: SourceGroup,
  segment: SourceSegment,
  reportType: Adhesive2ReportType = getDefaultAdhesive2ReportType(segment),
  scannedBatchNo = segment.batchNo,
  viewOnly = false,
  overwriteExisting = false,
  confirmAfterInspection = false,
) {
  if (reportType === 'CHANGEOVER') {
    openChangeoverInspectionScan(scannedBatchNo || segment.batchNo);
    return;
  }
  const existedRecord = findReportBySegment(segment);
  const editingExisting = overwriteExisting && !!existedRecord;
  if (!viewOnly && !editingExisting && segment.sourceNgText && !existedRecord) {
    AModal.warning({
      okText: '知道了',
      title: '前工序 NG 片不可继续粘胶2',
      content: `片号 ${segment.batchNo} 已标记为${segment.sourceNgText}${segment.sourceNgReason ? `：${segment.sourceNgReason}` : ''}，请先处理前工序异常。`,
    });
    return;
  }
  if (!viewOnly && !editingExisting && (isSourceGroupCompleted(group) || isSegmentCompleted(segment))) {
    showReadonlyTaskWarning('报工');
    return;
  }
  if (!viewOnly && !editingExisting && getAdhesive2SegmentAvailableLength(segment) <= 0) {
    message.warning('当前片号已完成粘胶2报工，没有可加工数量');
    return;
  }
  if (!viewOnly) {
    if (!editingExisting && !(await ensureAdhesive2SampleAbnormalUnlocked(segment, '新增报工', group.baseBatchNo))) {
      return;
    }
    if (isWorkOrderFinished.value) {
      message.warning('当前工单此工序已完工，不能再报工！');
      return;
    }
    if (adhesive2WorkbenchBlockedReason.value) {
      AModal.warning({
        okText: '知道了',
        title: '粘胶2工作台已阻断',
        content: adhesive2WorkbenchBlockedReason.value,
      });
      return;
    }
    if (!isWorkOrderRunning.value) {
      AModal.warning({
        okText: '去待加工开工',
        title: '当前粘胶2工单未开工',
        content: '请先在右上角“待加工”中选择当前计划并执行开工，再进行粘胶2报工登记。',
        onOk: () => {
          void openTaskList();
        },
      });
      return;
    }
    const glueBoardGuardReason = editingExisting ? '' : buildGlueBoardBasicOperationGuardReason('报工');
    if (glueBoardGuardReason) {
      AModal.warning({
        okText: '知道了',
        title: '胶板未满足报工条件',
        content: glueBoardGuardReason,
      });
      return;
    }
    if (!editingExisting && !isChangeoverInspectionReady()) {
      showChangeoverInspectionRequiredWarning(scannedBatchNo || segment.batchNo);
      return;
    }
    if (!editingExisting && !(await ensureDailyPreparationReadyForReport())) return;
    if (!editingExisting && !(await ensureCurrentMotherChangeoverSubmittedForReport())) return;
    if (!editingExisting && !glueBoard.id) {
      AModal.warning({
        okText: '进入胶板领用',
        title: '未登记胶板领用',
        content: '粘胶2报工前必须先登记本次胶板领用。确认后选择胶板边库批次并自动领用。',
        onOk: () => {
          openGlueConsumeDialog();
        },
      });
      return;
    }
  }
  resetReportForm();
  applyGlueBoardDefaultsToCheckItems();
  activeRecord.value = viewOnly || editingExisting ? existedRecord || null : null;
  const availableRange = findFirstAvailableRange(segment);
  reportForm.sourceCode = segment.batchNo;
  reportForm.sourceProductionBatchNo = segment.batchNo;
  reportForm.parentBatchNo = group.baseBatchNo;
  reportForm.reportType = reportType;
  reportForm.startPosition = availableRange.start;
  reportForm.endPosition = availableRange.end;
  reportForm.processLength = availableRange.length;
  reportForm.outputLength = reportOutputLength.value;
  reportForm.glueBoardBatchNo = glueBoard.batchNo || '';
  reportForm.glueBoardMaterialCode = getActualGlueBoardMaterialCode();
  reportForm.glueBoardModel = getActualGlueBoardModel();
  reportForm.glueBoardStartPosition = Number(glueBoard.availableStartPosition || 0);
  reportForm.glueBoardUseLength = getGlueBoardUseLengthPerScan(glueBoard.stockLength);
  reportForm.productionBatchNo = getAdhesive2SegmentDisplayBatchNo(segment);
  singleScanConfirmAfterInspection.value = !viewOnly && confirmAfterInspection;
  reportDialogMode.value = viewOnly ? 'view' : 'confirm';
  reportStep.value = 'PROCESS';
  activeReportTab.value = singleScanConfirmAfterInspection.value && shouldShowVisualInspectionTab.value
    ? 'visual-inspection'
    : getDefaultReportTabKey();
  reportVisible.value = true;
  const sourceScanOk = await scanReportSource();
  if (!viewOnly && !sourceScanOk) {
    reportVisible.value = false;
    singleScanConfirmAfterInspection.value = false;
    message.warning('来源扫码接口未找到该片号，不能进入粘胶2外观检验');
    return;
  }
  applyReportRecordToForm(existedRecord);
  activeReportTab.value = singleScanConfirmAfterInspection.value && shouldShowVisualInspectionTab.value
    ? 'visual-inspection'
    : getDefaultReportTabKey();
}

function openReportViewDialog(group: SourceGroup, segment: SourceSegment) {
  void openReportDialog(group, segment, getAdhesive2SegmentReportType(segment), segment.batchNo, true);
}

async function startReportWork() {
  const ok = await scanReportSource();
  if (!ok) return;
  if (!(await ensureAdhesive2SampleAbnormalUnlocked(reportForm.sourceProductionBatchNo, '开工报工', reportForm.parentBatchNo))) {
    return;
  }
  reportForm.startTime = reportForm.startTime || buildNowText();
  reportStep.value = 'PROCESS';
  activeReportTab.value = getDefaultReportTabKey();
  if (!activeReportTab.value) {
    reportStep.value = 'REPORT';
    activeReportTab.value = 'report';
  }
  if (activeReportTab.value === 'visual-inspection') {
    reportForm.selfCheck = hasActiveVisualItems.value ? 'NG' : reportForm.selfCheck || 'OK';
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
    const input = document.querySelector<HTMLInputElement>('.ant-tabs-tabpane-active input.adhesive-check-cell');
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
    reportForm.selfCheck = hasActiveVisualItems.value ? 'NG' : reportForm.selfCheck || 'OK';
    message.info('请补充外观检验、综合判断和备注后再保存扫码确认。');
    return;
  }
  reportForm.endTime = reportForm.endTime || buildNowText();
  const confirmAfterSave = singleScanConfirmAfterInspection.value;
  const reportId = await submitAdhesiveReport({ confirmAfterSave });
  if (reportId && confirmAfterSave) {
    await refreshChangeoverAfterReportConfirmation();
    selectedReportRecordKeys.value = selectedReportRecordKeys.value.filter((key) => key !== reportId);
    focusPlanScanInput();
  }
}

function getCheckItemsByCategory(category: string) {
  let rows: AdhesiveCheckItem[] = [];
  if (category === '中间品') {
    rows = adhesive2MiddleCheckItems.value;
  } else if (category === '过程加检') {
    rows = adhesive2ProcessCheckItems.value;
  } else if (shouldShowProcessParameterTab.value) {
    rows = ensureAdhesive2ChangeoverItems(checkTemplate.value).filter((item) => item.itemCategory === category);
  }
  return rows.filter((item) => item.itemCategory === category).sort((a, b) => Number(a.sortNo || 0) - Number(b.sortNo || 0));
}

function isAdhesive2ReportType(value?: string): value is Adhesive2ReportType {
  return adhesive2ReportTypeOptions.some((item) => item.value === value);
}

function isAdhesive2MiddleReportType(value?: string): value is 'END' | 'FRONT' | 'MIDDLE' {
  return ['END', 'FRONT', 'MIDDLE'].includes(String(value || ''));
}

function isAdhesive2InspectionReportType(value?: string) {
  return ['CHANGEOVER', 'PROCESS_CHECK'].includes(String(value || ''));
}

function getAdhesive2ReportTypeText(value?: string) {
  return adhesive2ReportTypeOptions.find((item) => item.value === value)?.label || '成品加工';
}

async function setActiveReportMiddleType(reportType: 'END' | 'FRONT' | 'MIDDLE') {
  const currentRecord = activeRecord.value;
  if (!currentRecord?.id) {
    message.warning('请先打开已保存的粘胶2片详情');
    return;
  }
  const reportId = Number(currentRecord.id);
  if (middleTypeSetting.value) return;
  middleTypeSetting.value = true;
  try {
    await setAdhesive2ReportMiddleType(reportId, reportType);
    const extra = {
      ...(parseRecordExtra(currentRecord) as Record<string, any>),
      reportType,
      reportTypeName: getAdhesive2ReportTypeText(reportType),
    };
    activeRecord.value = { ...currentRecord, extraJson: JSON.stringify(extra) };
    reportForm.reportType = reportType;
    await Promise.all([loadReports(), loadSourceGroups(), loadAdhesive2IntermediateRecords()]);
    message.success(`已设置为${getAdhesive2ReportTypeText(reportType)}，新建粘胶2中间品记录表时会带入该片号`);
  } catch (error: any) {
    message.error(error?.message || '设置中间品段位失败');
  } finally {
    middleTypeSetting.value = false;
  }
}

function getActiveVisualCategoryItems(record?: MesHcAdhesiveConsoleApi.ReportItem | Record<string, any> | null) {
  const source = (record || activeRecord.value || {}) as Record<string, any>;
  const extra = parseRecordExtra(source) as Record<string, any>;
  const categories = parseVisualItemsFromRecord(source)
    .filter((item) => isVisualItemActive(item))
    .map((item) => item.itemName)
    .filter((itemName) => adhesive2VisualItemNames.includes(itemName));
  const defectCode = String(source.defectCode || extra.defectCode || '').trim();
  if (adhesive2VisualItemNames.includes(defectCode) && !categories.includes(defectCode)) {
    categories.push(defectCode);
  }
  return Array.from(new Set(categories));
}

function getActiveVisualCategoryText(record?: MesHcAdhesiveConsoleApi.ReportItem | Record<string, any> | null) {
  return getActiveVisualCategoryItems(record).join('、') || '-';
}

function openAbnormalCategoryCorrection() {
  if (!canCorrectAbnormalCategory.value) {
    message.warning('当前账号没有修正异常类别权限，或当前片没有外观异常类别');
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
    message.warning('请先打开已保存的粘胶2片详情');
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
    const updatedRecord = await correctAdhesive2ReportAbnormalCategory({
      category: abnormalCategoryCorrectionForm.category,
      id: Number(currentRecord.id),
      reason,
    });
    activeRecord.value = updatedRecord;
    applyReportRecordToForm(updatedRecord);
    await Promise.all([loadReports(), loadSourceGroups()]);
    abnormalCategoryCorrectionVisible.value = false;
    message.success('异常类别已修正，统计归类将按新类别计算');
  } catch (error: any) {
    message.error(error?.message || '修正异常类别失败');
  } finally {
    abnormalCategoryCorrectionSaving.value = false;
  }
}

type Adhesive2MiddleAutoMarkType = 'END' | 'FRONT' | 'MIDDLE';
interface Adhesive2MiddleAutoMarkTarget {
  batchNo: string;
  index: number;
  record: MesHcAdhesiveConsoleApi.ReportItem;
  reportType: Adhesive2MiddleAutoMarkType;
}

interface Adhesive2MiddleAutoMarkPreviewRow {
  batchNo: string;
  index: number;
  key: string;
  markText: string;
}

function isAdhesive2RecordCoa(record?: MesHcAdhesiveConsoleApi.ReportItem | Record<string, any> | null) {
  if (!record) return false;
  const extra = parseRecordExtra(record) as Record<string, any>;
  const raw = (record as Record<string, any>).coaFlag ?? extra.coaFlag;
  return raw === true || raw === 'true' || raw === 'Y' || raw === 1;
}

function findAdhesive2SourceSegmentByReport(record: MesHcAdhesiveConsoleApi.ReportItem) {
  const batchNos = [record.sourceProductionBatchNo, record.productionBatchNo].map((item) => String(item || '').trim()).filter(Boolean);
  if (!batchNos.length) return undefined;
  const batchNoSet = new Set(batchNos);
  return sourceGroups.value.flatMap((group) => group.segments).find((segment) => batchNoSet.has(segment.batchNo));
}

function isAdhesive2MiddleAutoMarkCoaRecord(record: MesHcAdhesiveConsoleApi.ReportItem) {
  if (isAdhesive2RecordCoa(record)) return true;
  const segment = findAdhesive2SourceSegmentByReport(record);
  return segment ? getAdhesive2SegmentCoaFlag(segment) : false;
}

function isAdhesive2MiddleAutoMarkNormalFirstInspectionRecord(record: MesHcAdhesiveConsoleApi.ReportItem) {
  const inspection = firstInspection.value as Record<string, any>;
  if (!inspection?.faiId || getAdhesive2FaiResultText(inspection) !== '正常') return false;
  const batchNo = normalizeAdhesive2CoaCompareNo(record.productionBatchNo);
  const sourceBatchNo = normalizeAdhesive2CoaCompareNo(record.sourceProductionBatchNo);
  const inspectionBatchNo = normalizeAdhesive2CoaCompareNo(inspection.productBatchNo);
  const reportId = Number(record.id || 0);
  const inspectionSourceReportId = Number(inspection.sourceReportId || 0);
  return (!!inspectionBatchNo && (inspectionBatchNo === batchNo || inspectionBatchNo === sourceBatchNo)) || (!!reportId && !!inspectionSourceReportId && inspectionSourceReportId === reportId);
}

function isAdhesive2MiddleAutoMarkProcessCheckRecord(record: MesHcAdhesiveConsoleApi.ReportItem) {
  const batchNo = normalizeAdhesive2CoaCompareNo(record.productionBatchNo);
  const sourceBatchNo = normalizeAdhesive2CoaCompareNo(record.sourceProductionBatchNo);
  const reportId = Number(record.id || 0);
  return processCheckFaiRows.value.some((inspection) => {
    const inspectionBatchNo = normalizeAdhesive2CoaCompareNo(inspection.productBatchNo);
    const inspectionSourceReportId = Number(inspection.sourceReportId || 0);
    const sameBatchNo = !!inspectionBatchNo && (inspectionBatchNo === batchNo || inspectionBatchNo === sourceBatchNo);
    const sameSourceReport = !!reportId && !!inspectionSourceReportId && inspectionSourceReportId === reportId;
    return sameBatchNo || sameSourceReport;
  });
}

function getAdhesive2BatchSerialNo(batchNo?: string) {
  const text = String(batchNo || '').trim();
  if (!text) return Number.MAX_SAFE_INTEGER;
  const normalized = /[AB]$/i.test(text) ? text.slice(0, -1) : text;
  const sortText = normalized.slice(-3);
  return /^\d+$/.test(sortText) ? Number(sortText) : Number.MAX_SAFE_INTEGER;
}

function compareAdhesive2CardNo(left?: string, right?: string) {
  const serialDiff = getAdhesive2BatchSerialNo(left) - getAdhesive2BatchSerialNo(right);
  if (serialDiff !== 0) return serialDiff;
  return String(left || '').localeCompare(String(right || ''), 'zh-Hans-CN', {
    numeric: true,
  });
}

function sortAdhesive2MiddleCandidateRecords(records: MesHcAdhesiveConsoleApi.ReportItem[]) {
  return [...records].sort((a, b) => {
    return compareAdhesive2CardNo(a.productionBatchNo, b.productionBatchNo);
  });
}

function getAdhesive2MiddleAutoMarkCandidates() {
  const currentMother = normalizeAdhesive2MiddleBatchNo(resolveCurrentIntermediateBatchNo() || currentMotherBatchNo.value || '');
  return sortAdhesive2MiddleCandidateRecords(
    reportRecords.value
      .filter((record) => !!record.id && !!record.productionBatchNo)
      .filter((record) => isAdhesiveRecordConfirmed(record.reportStatus))
      .filter((record) => !hasVisualIssueFromRecord(record as Record<string, any>))
      .filter((record) => !isAdhesive2MiddleAutoMarkCoaRecord(record))
      .filter((record) => !isAdhesive2MiddleAutoMarkNormalFirstInspectionRecord(record))
      .filter((record) => !isAdhesive2MiddleAutoMarkProcessCheckRecord(record))
      .filter((record) => {
        if (!currentMother) return true;
        return resolveIntermediateReportMotherBatchNo(record as Record<string, any>) === currentMother;
      }),
  );
}

function buildAdhesive2MiddleAutoMarkTargets(records: MesHcAdhesiveConsoleApi.ReportItem[]) {
  const middleIndex = Math.floor((records.length - 1) / 2);
  const rawTargets: Adhesive2MiddleAutoMarkTarget[] = [
    {
      batchNo: records[0]?.productionBatchNo || '',
      index: 1,
      record: records[0]!,
      reportType: 'FRONT',
    },
    {
      batchNo: records[middleIndex]?.productionBatchNo || '',
      index: middleIndex + 1,
      record: records[middleIndex]!,
      reportType: 'MIDDLE',
    },
    {
      batchNo: records[records.length - 1]?.productionBatchNo || '',
      index: records.length,
      record: records[records.length - 1]!,
      reportType: 'END',
    },
  ];
  const seenRecordIds = new Set<number | string>();
  return rawTargets.filter((target) => {
    const recordId = target.record?.id;
    if (!recordId || seenRecordIds.has(recordId)) return false;
    seenRecordIds.add(recordId);
    return !!target.batchNo;
  });
}

function getAdhesive2MiddleAutoMarkTypeText(reportType?: Adhesive2MiddleAutoMarkType) {
  if (reportType === 'FRONT') return '前段';
  if (reportType === 'MIDDLE') return '中段';
  if (reportType === 'END') return '后段';
  return '';
}

function buildAdhesive2MiddleAutoMarkPreviewRows(records: MesHcAdhesiveConsoleApi.ReportItem[], targets: Adhesive2MiddleAutoMarkTarget[]): Adhesive2MiddleAutoMarkPreviewRow[] {
  const targetTypeMap = new Map<string, Adhesive2MiddleAutoMarkType>();
  targets.forEach((target) => {
    if (target.record?.id != null) targetTypeMap.set(String(target.record.id), target.reportType);
  });
  return records.map((record, index) => {
    const reportType = record.id == null ? undefined : targetTypeMap.get(String(record.id));
    return {
      batchNo: record.productionBatchNo || '',
      index: index + 1,
      key: String(record.id || record.productionBatchNo || index),
      markText: getAdhesive2MiddleAutoMarkTypeText(reportType),
    };
  });
}

function renderAdhesive2MiddleAutoMarkConfirmContent(motherBatchNo: string, rows: Adhesive2MiddleAutoMarkPreviewRow[]) {
  return h('div', { class: 'press-slot-auto-middle-confirm' }, [
    h('p', `当前母卷批号 ${motherBatchNo || '-'} 共自检正常且非首检/加检/COA ${rows.length} 片，确认后按前段、中段、后段更新中间品标记。`),
    h(ATable, {
      bordered: true,
      columns: [
        {
          align: 'center',
          dataIndex: 'index',
          key: 'index',
          title: '顺序号',
          width: 80,
        },
        { dataIndex: 'batchNo', key: 'batchNo', title: '卡号', width: 240 },
        {
          align: 'center',
          dataIndex: 'markText',
          key: 'markText',
          title: '前中尾标记',
          width: 120,
        },
      ],
      dataSource: rows,
      pagination: false,
      rowKey: 'key',
      scroll: { x: 520, y: 320 },
      size: 'small',
    }),
  ]);
}

async function openAutoMarkAdhesive2MiddleSegments() {
  if (isCurrentTaskReadonly.value) {
    showReadonlyTaskWarning('标记中间品');
    return;
  }
  if (!currentPlan.planId || !currentPlan.planOperationId) {
    message.warning('请先加载粘胶2计划');
    return;
  }
  const motherBatchNo = normalizeAdhesive2MiddleBatchNo(resolveCurrentIntermediateBatchNo() || currentMotherBatchNo.value || currentPlan.sourceBatchNo || '');
  const candidates = getAdhesive2MiddleAutoMarkCandidates();
  if (candidates.length < 3) {
    message.warning(`当前母卷批号 ${motherBatchNo || '-'} 已确认、自检正常且非首检/加检/COA片数为 ${candidates.length}，至少需要 3 片才能标记前中后段`);
    return;
  }
  const targets = buildAdhesive2MiddleAutoMarkTargets(candidates);
  if (targets.length < 3) {
    message.warning('计算出的前段、中段、后段片号不完整，请确认片号流水号后重试');
    return;
  }
  const previewRows = buildAdhesive2MiddleAutoMarkPreviewRows(candidates, targets);
  AModal.confirm({
    cancelText: '取消',
    content: renderAdhesive2MiddleAutoMarkConfirmContent(motherBatchNo, previewRows),
    okText: '确认标记',
    title: '标记中间品确认',
    width: 680,
    onOk: async () => {
      if (middleAutoMarking.value) return;
      middleAutoMarking.value = true;
      try {
        for (const target of targets) {
          await setAdhesive2ReportMiddleType(Number(target.record.id), target.reportType);
        }
        await Promise.all([loadReports(), loadSourceGroups(), loadAdhesive2IntermediateRecords()]);
        message.success('中间品前中后段已标记完成');
      } catch (error: any) {
        message.error(error?.message || '标记中间品失败');
        throw error;
      } finally {
        middleAutoMarking.value = false;
      }
    },
  });
}

function getInspectionResultText(record?: Partial<MesHcAdhesiveConsoleApi.ChangeoverInspection>) {
  if (record?.feedbackResult) return record.feedbackResult;
  if (record?.inspectionStatus === 'QUALIFIED') return 'OK';
  if (record?.inspectionStatus === 'ABNORMAL') return 'NG';
  return '-';
}

function getDefaultAdhesive2ReportType(_segment?: SourceSegment): Adhesive2ReportType {
  return 'PRODUCT';
}

function getReportCheckItemsForSave() {
  const baseItems = shouldShowProcessParameterTab.value ? ensureAdhesive2ChangeoverItems(checkTemplate.value) : [];
  if (isAdhesive2MiddleReportType(reportForm.reportType)) {
    return [...baseItems, ...adhesive2MiddleCheckItems.value];
  }
  if (reportForm.reportType === 'PROCESS_CHECK') {
    return [...baseItems, ...adhesive2ProcessCheckItems.value];
  }
  return baseItems;
}

function applyReportCheckItems(checkItems?: MesHcAdhesiveConsoleApi.CheckItem[]) {
  const savedItems = Array.isArray(checkItems) ? checkItems : [];
  checkTemplate.value = checkTemplate.value.map((item) => {
    const matched = savedItems.find((saved) => saved.itemCategory === item.itemCategory && saved.itemName === item.itemName) || savedItems.find((saved) => saved.itemName === item.itemName);
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
  adhesive2MiddleCheckItems.value = adhesive2MiddleCheckItems.value.map((item) => {
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
  adhesive2ProcessCheckItems.value = adhesive2ProcessCheckItems.value.map((item) => {
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
  const editingReportId = Number(activeRecord.value?.id || 0);
  const overlapRange = (segment?.reportRanges || []).find((range) => (!editingReportId || Number(range.reportId || 0) !== editingReportId) && start < range.end && end > range.start);
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
  const editingReportId = Number(activeRecord.value?.id || 0);
  return segment.reportRanges.filter((range) => (!editingReportId || Number(range.reportId || 0) !== editingReportId) && current.start < range.end && current.end > range.start);
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
const reportRangeText = computed(() => `${formatNumber(currentReportRange.value.start)}-${formatNumber(currentReportRange.value.end)} m`);

function confirmSwitchToProcessCheck() {
  if (reportForm.reportType === 'PROCESS_CHECK') return Promise.resolve(true);
  return new Promise<boolean>((resolve) => {
    AModal.confirm({
      content: `当前作业类型是“${getAdhesive2ReportTypeText(reportForm.reportType)}”，是否改为“过程加检”并推送 FAI 检验记录？`,
      okText: '确认改为过程加检',
      onCancel: () => resolve(false),
      onOk: () => {
        reportForm.reportType = 'PROCESS_CHECK';
        adhesive2ProcessCheckItems.value = adhesive2ProcessCheckItems.value.map((item) => ({
          ...item,
          actualValue: item.itemName === '检测时间' ? buildNowText() : item.actualValue,
        }));
        resolve(true);
      },
      title: '确认作业类型',
    });
  });
}

function buildProcessCheckFaiRemark(reportId?: number) {
  return [
    '粘胶2操作看板过程加检',
    `分段批号 ${currentMotherBatchNo.value || reportForm.parentBatchNo || '-'}`,
    `来源片号 ${reportForm.sourceProductionBatchNo || '-'}`,
    `粘胶2片号 ${reportForm.productionBatchNo || '-'}`,
    `报工ID ${reportId || '-'}`,
  ].join('；');
}

async function saveAndPushProcessCheck() {
  if (reportDialogMode.value === 'view') return;
  if (isCurrentTaskReadonly.value) {
    showReadonlyTaskWarning('过程加检');
    return;
  }
  const confirmed = await confirmSwitchToProcessCheck();
  if (!confirmed) return;
  if (shouldShowVisualInspectionTab.value && activeReportTab.value !== 'visual-inspection') {
    activeReportTab.value = 'visual-inspection';
    reportForm.selfCheck = hasActiveVisualItems.value ? 'NG' : reportForm.selfCheck || 'OK';
    message.info('请确认外观检验后，再次点击保存并推送过程加检。');
    return;
  }
  if (processCheckApplying.value) return;
  processCheckApplying.value = true;
  try {
    reportForm.endTime = reportForm.endTime || buildNowText();
    const existingConfirmedReportId = activeRecord.value?.id && isAdhesiveRecordConfirmed(activeRecord.value.reportStatus) ? Number(activeRecord.value.id) : undefined;
    const reportId =
      existingConfirmedReportId ||
      (await submitAdhesiveReport({
        closeAfterSave: false,
        successMessage: '过程加检报工已保存，正在推送 FAI 检验记录',
      }));
    if (!reportId) return;
    const summary = await applyAdhesive2Fai({
      glueBoardBatchNo: glueBoard.batchNo || reportForm.glueBoardBatchNo || undefined,
      glueBoardMaterialCode: getActualGlueBoardMaterialCode() || reportForm.glueBoardMaterialCode || undefined,
      glueBoardUsageId: glueBoard.id || reportForm.glueBoardUsageId || undefined,
      inspectionScene: 'PROCESS_CHECK',
      inspectionScopeBatchNo: currentMotherBatchNo.value || reportForm.parentBatchNo || undefined,
      planId: currentPlan.planId!,
      planOperationId: currentPlan.planOperationId!,
      productBatchNo: reportForm.productionBatchNo || undefined,
      remark: buildProcessCheckFaiRemark(reportId),
      sourceReportId: reportId,
      standardMatchMode: 'PRODUCT_MODEL_PROCESS',
      submitterName: currentUserName.value || undefined,
      triggerReason: 'NEW_ORDER',
    });
    await Promise.all([loadProcessCheckFaiRows(), loadReports(), loadSourceGroups()]);
    reportVisible.value = false;
    clearReportIntermediateDraft();
    activeBoardTab.value = 'SOURCE';
    message.success(`过程加检已推送，FAI单号：${summary.faiNo || '-'}`);
  } catch (error: any) {
    AModal.warning({
      content: error?.message || '过程加检推送失败，请检查当前计划、粘胶2片号和 FAI 检验标准。',
      title: '过程加检未推送',
    });
  } finally {
    processCheckApplying.value = false;
  }
}

async function submitAdhesiveReport(options: SubmitAdhesiveReportOptions = {}) {
  if (reportDialogMode.value === 'view') return;
  if (reportSubmitting.value) return;
  reportSubmitting.value = true;
  try {
    if (!currentPlan.planId || !currentPlan.planOperationId) {
      message.warning('请先扫码带出粘胶2计划');
      return;
    }
    if (isWorkOrderFinished.value) {
      message.warning('当前工单此工序已完工，不能再报工！');
      return;
    }
    const reportSource = findSourceSegmentWithGroup(reportForm.sourceProductionBatchNo || reportForm.sourceCode);
    if (!isConfirmedReportOverwrite.value && !(await ensureAdhesive2SampleAbnormalUnlocked(reportSource?.segment || reportForm.sourceProductionBatchNo, '提交报工', reportSource?.group?.baseBatchNo || reportForm.parentBatchNo))) {
      return;
    }
    if (!isConfirmedReportOverwrite.value && (isSourceGroupCompleted(reportSource?.group) || isSegmentCompleted(reportSource?.segment))) {
      showReadonlyTaskWarning('保存报工');
      return;
    }
    if (adhesive2WorkbenchBlockedReason.value) {
      AModal.warning({
        okText: '知道了',
        title: '粘胶2工作台已阻断',
        content: adhesive2WorkbenchBlockedReason.value,
      });
      return;
    }
    if (!isWorkOrderRunning.value) {
      AModal.warning({
        okText: '去待加工开工',
        title: '当前粘胶2工单未开工',
        content: '请先在右上角“待加工”中选择当前计划并执行开工，再进行粘胶2报工登记。',
        onOk: () => {
          void openTaskList();
        },
      });
      return;
    }
    if (!isConfirmedReportOverwrite.value && !(await ensureStrictGlueBoardMatch())) return;
    const glueBoardGuardReason = isConfirmedReportOverwrite.value ? '' : buildGlueBoardBasicOperationGuardReason('提交报工');
    if (glueBoardGuardReason) {
      AModal.warning({
        okText: '知道了',
        title: '胶板未满足报工条件',
        content: glueBoardGuardReason,
      });
      return;
    }
    if (!reportForm.sourceGrindingSecondDetailId) {
      message.warning('请先通过来源扫码接口确认压槽片号');
      return;
    }
    if (!reportForm.processLength || reportForm.processLength <= 0) {
      message.warning('请填写加工片数');
      return;
    }
    const positionMessage = getReportPositionValidationMessage();
    if (positionMessage) {
      message.warning(positionMessage);
      return;
    }
    if (!glueBoard.id) {
      AModal.warning({
        okText: '进入胶板领用',
        title: '未登记胶板领用',
        content: '粘胶2报工前必须先登记本次胶板领用。确认后选择胶板边库批次并自动领用。',
        onOk: () => {
          openGlueConsumeDialog();
        },
      });
      return;
    }
    const outputLength = reportOutputLength.value;
    const visualItemsForSave = normalizeVisualItems(visualInspectionItems.value);
    const reportSelfCheck = hasActiveVisualItems.value ? 'NG' : reportForm.selfCheck || 'OK';
    const preProcessSelfCheckAbnormal = reportSelfCheck === 'NG' && reportForm.preProcessSelfCheckAbnormal;
    reportForm.selfCheck = reportSelfCheck;
    if (preProcessSelfCheckAbnormal && reportForm.reportType === 'CHANGEOVER') {
      message.warning('加工前自检异常应归属压槽，不能按换型报工提交');
      return;
    }
    const visualInspectionTime = buildNowText();
    const actualGlueBoardModel = getActualGlueBoardModel();
    const actualGlueBoardMaterialCode = getActualGlueBoardMaterialCode();
    const actualGlueBoardBatchNo = glueBoard.batchNo || reportForm.glueBoardBatchNo || '';
    const currentModelCode = currentProductModelCode.value;
    const currentMaterialCode = currentProductMaterialCode.value;
    const sourceSegment = findSourceSegment(reportForm.sourceProductionBatchNo);
    const actualSizeRule = normalizeSizeRule(
      sourceSegment?.actualSizeRule || activeRecord.value?.actualSizeRule || currentPlan.actualSizeRule,
    );
    if (options.confirmAfterSave && !actualSizeRule) {
      message.warning('未选择实际尺寸和片号尾号，不能保存并扫码确认');
      return;
    }
    const finalProductionBatchNo = getAdhesive2TailBatchNo(
      reportForm.sourceProductionBatchNo || reportForm.productionBatchNo,
      actualSizeRule,
    );
    reportForm.productionBatchNo = finalProductionBatchNo;
    const sourceModelCode = String(sourceSegment?.sourceModelCode || currentPlan.modelCode || '').trim();
    const extra = applyPreProcessSelfCheckAttribution(
      {
        ...(parseRecordExtra(activeRecord.value || {}) as Record<string, any>),
        generatedProductionBatchNo: finalProductionBatchNo,
        glueBoardBatchNo: actualGlueBoardBatchNo,
        glueBoardMaterialCode: actualGlueBoardMaterialCode,
        glueBoardModel: actualGlueBoardModel,
        changeoverInstructionId: getCurrentChangeoverInstructionId(),
        changeoverInstructionNo: modelOverride.changeoverInstructionNo || undefined,
        changeoverInstructionTargetQty: modelOverride.changeoverInstructionTargetQty,
        modelChanged: isProductModelChanged.value,
        originalProductionBatchNo: stripAdhesive2RuntimeBatchSuffix(normalizeAdhesive2ProductionBatchNo(reportForm.sourceProductionBatchNo || reportForm.productionBatchNo, currentPlan.actualSizeRule)),
        planSizeSpec: currentPlan.planSizeSpec,
        plannedMaterialCode: modelOverride.originalMaterialCode || currentPlan.materialCode,
        plannedModelCode: modelOverride.originalModelCode || currentPlan.modelCode,
        plannedGlueBoardModel: modelOverride.originalGlueBoardModel || currentPlan.planGlueBoardModel || currentPlan.glueBoardModel,
        reportType: reportForm.reportType,
        reportTypeName: getAdhesive2ReportTypeText(reportForm.reportType),
        runtimeGlueBoardModel: actualGlueBoardModel,
        runtimeMaterialCode: currentMaterialCode,
        runtimeModelCode: currentModelCode,
        runtimeProductSpecification: currentProductSpecification.value,
        visualInspectionRemark: reportForm.remark || '',
        visualInspectionResult: reportSelfCheck,
        visualInspectionTime,
        visualItems: shouldShowVisualInspectionTab.value ? visualItemsForSave : [],
      },
      reportSelfCheck === 'NG' && reportForm.preProcessSelfCheckAbnormal,
      ADHESIVE2_PRE_PROCESS_ATTRIBUTION,
    );
    if (preProcessSelfCheckAbnormal) {
      delete extra.changeoverInstructionId;
      delete extra.changeoverInstructionNo;
      delete extra.changeoverInstructionTargetQty;
      delete extra.runtimeGlueBoardModel;
      delete extra.runtimeMaterialCode;
      delete extra.runtimeProductSpecification;
      extra.modelChanged = false;
      extra.reportType = 'PRODUCT';
      extra.reportTypeName = getAdhesive2ReportTypeText('PRODUCT');
      if (sourceModelCode) {
        extra.plannedModelCode = sourceModelCode;
        extra.runtimeModelCode = sourceModelCode;
      } else {
        delete extra.runtimeModelCode;
      }
    }
    const savePayload = {
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
      aqcStatus: reportForm.aqcStatus || undefined,
      aqcTaskId: reportForm.aqcTaskId,
      glueBoardBatchNo: actualGlueBoardBatchNo || undefined,
      glueBoardMaterialCode: actualGlueBoardMaterialCode || undefined,
      glueBoardUsageId: glueBoard.id,
      glueBoardModel: actualGlueBoardModel,
      glueBoardUseLength: Number(reportForm.glueBoardUseLength || getGlueBoardUseLengthPerScan(glueBoard.stockLength)),
      inputLength: Number(reportForm.processLength || 0),
      startPosition: Number(reportForm.startPosition || 0),
      endPosition: Number(reportForm.endPosition || 0),
      lossLength: Number(reportForm.lossLength || 0),
      materialCode: preProcessSelfCheckAbnormal ? undefined : currentMaterialCode || undefined,
      modelCode: preProcessSelfCheckAbnormal ? sourceModelCode || undefined : currentModelCode || undefined,
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
      sourceType: '扫码母料',
      startTime: reportForm.startTime || buildNowText(),
    } satisfies MesHcAdhesiveConsoleApi.SaveReportReq;
    const saveResult = options.confirmAfterSave
      ? await saveAndConfirmAdhesiveConsoleReport({
          ...savePayload,
          actualSizeRule,
          confirmerName: currentUserName.value || undefined,
          confirmerTime: buildNowText(),
          expectedRuntimeModelCode: options.expectedRuntimeModelCode || undefined,
          glueBoardModel: actualGlueBoardModel,
          oneClickBatchConfirm: options.oneClickBatchConfirm || undefined,
          scannedBatchNo: finalProductionBatchNo,
        })
      : await saveAdhesiveConsoleReport(savePayload);
    const reportId = resolveSavedReportId(saveResult, '粘胶2');
    await loadReports();
    const savedRecord = reportRecords.value.find((record) => record.id === reportId);
    if (savedRecord) {
      activeRecord.value = savedRecord;
    }
    await loadGlueBoardUsage();
    await glueBoardEdgeConsumableTabRef.value?.reload?.();
    await loadSourceGroups();
    await loadAdhesive2IntermediateRecords();
    if (options.closeAfterSave !== false) {
      reportVisible.value = false;
      clearReportIntermediateDraft();
      singleScanConfirmAfterInspection.value = false;
    }
    if (!options.silentSuccess) {
      message.success(options.successMessage || (options.confirmAfterSave
        ? '粘胶2报工已保存并扫码确认'
        : '粘胶2报工已保存，已进入待确认；请在右上角选择尾号后再打印或扫码确认'));
    }
    return reportId;
  } catch (error: any) {
    AModal.warning({
      content: getReportRequestErrorMessage(error, '粘胶2报工保存或确认失败，请检查登录状态后重试。'),
      title: '粘胶2扫码确认失败',
    });
    return undefined;
  } finally {
    reportSubmitting.value = false;
  }
}

function toggleExtendedBoardTabs() {
  showExtendedBoardTabs.value = !showExtendedBoardTabs.value;
  if (!showExtendedBoardTabs.value && ['CHECK', 'RECORDS'].includes(activeBoardTab.value)) {
    activeBoardTab.value = 'SOURCE';
  }
  message.info(showExtendedBoardTabs.value ? '已显示点检/清洁和今日粘胶2报工记录' : '已隐藏扩展记录页签');
}

watch(showExtendedBoardTabs, (visible) => {
  if (!visible && ['CHECK', 'RECORDS'].includes(activeBoardTab.value)) {
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
      reportForm.glueBoardUseLength = getGlueBoardUseLengthPerScan(glueBoard.stockLength);
      syncingReportPosition = false;
    }
  },
);

watch(scanPlanNo, (value) => schedulePlanScan(value));

watch(
  () => [
    adhesive2InstructionContext.value.operationCode,
    adhesive2InstructionContext.value.operationName,
    adhesive2InstructionContext.value.planId,
    adhesive2InstructionContext.value.planNo,
    adhesive2InstructionContext.value.planOperationId,
    adhesive2InstructionContext.value.processCode,
    adhesive2InstructionContext.value.processName,
    currentPlan.batchNo,
    currentPlan.sourceBatchNo,
  ],
  () => {
    void refreshInstructionUnreadCount();
    void loadLatestPlanInstruction();
    void loadChangeoverInstruction();
  },
  { immediate: true },
);

watch(activeBoardTab, async (tab) => {
  if (tab !== 'SOURCE') {
    closeTransferPrintSelector();
  }
  if (tab === 'MIDDLE_LEDGER' && currentPlan.planId && currentPlan.planOperationId) {
    await loadAdhesive2IntermediateRecords();
  }
  if (tab === 'COA_INSPECTION' && currentPlan.planId && currentPlan.planOperationId) {
    await loadCoaFaiRows();
  }
  if (tab === 'PROCESS_CHECK' && currentPlan.planId && currentPlan.planOperationId) {
    await loadProcessCheckFaiRows();
  }
  await nextTick();
  window.dispatchEvent(new Event('resize'));
});

watch([reportRecords, sourceGroups], () => {
  const selectableKeys = new Set(selectableTransferTargets.value.map((target) => getTransferPrintTargetKey(target.segment)));
  selectedTransferReportIds.value = selectedTransferReportIds.value.filter((id) => selectableKeys.has(id));
});

watch(reportScanConfirmDate, () => {
  if (currentPlan.planOperationId) {
    void loadReports();
  }
});

onActivated(() => {
  attachGlobalScannerListener();
  if (skipInitialActivation) {
    skipInitialActivation = false;
    return;
  }
  if (currentPlan.planOperationId) {
    void refreshAdhesive2Workbench();
  }
});

onMounted(async () => {
  attachGlobalScannerListener();
  timer = setInterval(() => {
    currentDateTime.value = dayjs().format('YYYY-MM-DD HH:mm:ss');
  }, 1000);
  inspectionStatusRefreshTimer = setInterval(() => {
    void refreshAdhesive2InspectionStatus({ silent: true });
  }, INSPECTION_STATUS_REFRESH_INTERVAL_MS);
  await loadCheckTemplate();
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
  changeoverInstructionIssueAuthResolver?.(false);
  changeoverInstructionIssueAuthResolver = undefined;
  if (timer) clearInterval(timer);
  if (inspectionStatusRefreshTimer) clearInterval(inspectionStatusRefreshTimer);
  clearPlanScanTimer();
  detachGlobalScannerListener();
});
</script>

<template>
  <Page auto-content-height :loading="boardLoading">
    <input ref="middleLedgerFileInput" accept=".xls,.xlsx" class="hidden" type="file" @change="handleMiddleLedgerFileChange" />
    <input ref="processParamFileInput" accept=".xls,.xlsx" class="hidden" type="file" @change="handleProcessParamFileChange" />
    <div class="adhesive-console" :class="{ 'is-visual-maximized': visualMaximized }">
      <div class="prototype-banner relative flex shrink-0 overflow-hidden rounded-xl border border-slate-200 bg-white p-3 shadow-sm">
        <div class="flex min-w-0 flex-1 items-center gap-4 pl-1">
          <div
            class="console-main-icon flex h-[60px] w-[60px] shrink-0 items-center justify-center rounded-xl bg-gradient-to-br from-cyan-500 to-blue-600 text-white shadow-md"
            title="双击显示/隐藏点检清洁和今日报工记录"
            @dblclick="toggleExtendedBoardTabs"
          >
            <IconifyIcon icon="lucide:layers" class="text-[32px]" />
          </div>
          <div class="console-title-block">
            <div class="console-title-row">
              <span class="console-title-text">粘胶2操作看板</span>
              <Tag color="processing" class="console-title-tag">单片背胶</Tag>
            </div>
            <div class="console-meta-row">
              <div class="console-meta-item console-meta-item--machine">
                <span class="console-meta-label">粘胶2机台</span>
                <strong class="console-meta-value">{{ selectedBoardEquipmentCode || '未绑定' }}</strong>
                <em class="console-meta-sub">{{ selectedBoardEquipmentName || '-' }}</em>
                <Button class="console-meta-action" size="small" type="link" @click.stop="openEquipmentSelect">切换</Button>
              </div>
              <div class="console-meta-item">
                <span class="console-meta-label">产线</span>
                <strong class="console-meta-value">{{ selectedBoardWorkCenterName || '-' }}</strong>
              </div>
            </div>
          </div>
        </div>
        <div class="inspection-stamp-slot adhesive-aqc-stamp-slot adhesive-stamp-group">
          <button class="inspection-stamp-side" :class="`inspection-stamp-side--${glueBoardAqcStatusMeta.stampClass}`" type="button" @click="openGlueBoardAqcDialog">
            <span class="inspection-stamp-content">
              <span>胶板送检</span>
              <strong>{{ glueBoardAqcStatusMeta.stampText }}</strong>
              <em>{{ glueBoardAqcStampTime }}</em>
            </span>
          </button>
        </div>
        <div v-if="showExecutionClock" class="work-time-card">
          <div>{{ currentDateText }}</div>
          <strong>{{ currentTimeText }}</strong>
        </div>
        <div class="console-action-group flex shrink-0 items-center gap-2 border-l border-slate-100 pl-5">
          <div
            class="flex h-[64px] w-[64px] cursor-pointer flex-col items-center justify-center rounded-xl border border-cyan-200 bg-cyan-50 text-cyan-700 transition-all hover:bg-cyan-100 hover:shadow-md active:scale-95"
            @click="openDailyRecordList"
          >
            <IconifyIcon icon="lucide:clipboard-check" class="mb-1 text-[22px] opacity-80" />
            <span class="text-[11px] font-bold">点检清洁</span>
          </div>
          <div
            class="flex h-[64px] w-[64px] cursor-pointer flex-col items-center justify-center rounded-xl border border-slate-200 bg-slate-50 text-slate-700 transition-all hover:bg-slate-100 hover:shadow-md active:scale-95"
            @click="openTaskList"
          >
            <IconifyIcon icon="lucide:clipboard-list" class="mb-1 text-[22px] opacity-80" />
            <span class="text-[11px] font-bold">待加工</span>
          </div>
          <div
            v-if="!isWorkOrderPaused && !isWorkOrderCancelled"
            class="flex h-[64px] w-[64px] cursor-pointer flex-col items-center justify-center rounded-xl border border-amber-200 bg-amber-50 text-amber-700 transition-all hover:bg-amber-100 hover:shadow-md active:scale-95"
            @click="openGlueBoardAqcDialog"
          >
            <IconifyIcon icon="lucide:shield-check" class="mb-1 text-[22px] opacity-80" />
            <span class="text-[11px] font-bold">胶板送检</span>
          </div>
          <div
            v-if="!isWorkOrderPaused && !isWorkOrderCancelled"
            class="flex h-[64px] w-[64px] cursor-pointer flex-col items-center justify-center rounded-xl border border-emerald-200 bg-emerald-50 text-emerald-700 transition-all hover:bg-emerald-100 hover:shadow-md active:scale-95"
            @click="openCoaInspectionDialog"
          >
            <IconifyIcon icon="lucide:file-check-2" class="mb-1 text-[22px] opacity-80" />
            <span class="text-[11px] font-bold">COA送检</span>
          </div>
          <div
            v-if="!isWorkOrderPaused && !isWorkOrderCancelled"
            class="flex h-[64px] w-[64px] cursor-pointer flex-col items-center justify-center rounded-xl border border-orange-200 bg-orange-50 text-orange-700 transition-all hover:bg-orange-100 hover:shadow-md active:scale-95"
            @click="openChangeoverInspectionScan"
          >
            <IconifyIcon icon="lucide:stamp" class="mb-1 text-[22px] opacity-80" />
            <span class="text-center text-[11px] font-bold leading-tight">上报<br />工艺参数</span>
          </div>
          <div
            v-if="!isWorkOrderPaused && !isWorkOrderCancelled"
            class="flex h-[64px] w-[64px] cursor-pointer flex-col items-center justify-center rounded-xl border border-indigo-200 bg-indigo-50 text-indigo-700 transition-all hover:bg-indigo-100 hover:shadow-md active:scale-95"
            @click="openTransferPrintSelector"
          >
            <IconifyIcon icon="lucide:printer" class="mb-1 text-[22px] opacity-80" />
            <span class="text-[11px] font-bold">打印流转单</span>
          </div>
          <div
            v-if="!isWorkOrderPaused && !isWorkOrderCancelled"
            class="flex h-[64px] w-[64px] cursor-pointer flex-col items-center justify-center rounded-xl border border-violet-200 bg-violet-50 text-violet-700 transition-all hover:bg-violet-100 hover:shadow-md active:scale-95"
            :class="{ 'is-disabled': isCurrentTaskReadonly }"
            @click="openAdhesive2TailAssign"
          >
            <IconifyIcon icon="lucide:tags" class="mb-1 text-[22px] opacity-80" />
            <span class="text-center text-[11px] font-bold leading-tight">一键选择<br />尾号</span>
          </div>
          <div
            v-if="!isWorkOrderPaused && !isWorkOrderCancelled"
            class="flex h-[64px] w-[64px] cursor-pointer flex-col items-center justify-center rounded-xl border border-violet-200 bg-violet-50 text-violet-700 transition-all hover:bg-violet-100 hover:shadow-md active:scale-95"
            :class="{ 'is-disabled': isCurrentTaskReadonly }"
            role="button"
            :tabindex="isCurrentTaskReadonly ? -1 : 0"
            :aria-disabled="isCurrentTaskReadonly"
            aria-label="扫码改尾号"
            @click="openSelectedTailAssign"
            @keydown.enter.prevent="openSelectedTailAssign"
            @keydown.space.prevent="openSelectedTailAssign"
          >
            <IconifyIcon icon="lucide:scan-line" class="mb-1 text-[22px] opacity-80" />
            <span class="text-center text-[11px] font-bold leading-tight">扫码修改<br />尾号</span>
          </div>
          <div
            v-if="!isWorkOrderPaused && !isWorkOrderCancelled"
            class="flex h-[64px] w-[64px] cursor-pointer flex-col items-center justify-center rounded-xl border border-sky-200 bg-sky-50 text-sky-700 transition-all hover:bg-sky-100 hover:shadow-md active:scale-95"
            @click="openSelectedRecordConfirm"
          >
            <IconifyIcon icon="lucide:scan-line" class="mb-1 text-[22px] opacity-80" />
            <span class="text-[11px] font-bold">扫码确认</span>
          </div>
          <div
            v-if="!isWorkOrderPaused && !isWorkOrderCancelled"
            class="flex h-[64px] w-[64px] cursor-pointer flex-col items-center justify-center rounded-xl border border-blue-200 bg-blue-50 text-blue-700 transition-all hover:bg-blue-100 hover:shadow-md active:scale-95"
            :class="{
              'is-disabled': isCurrentTaskReadonly || oneClickScanConfirming || hasPendingChangeoverInstruction,
            }"
            @click="confirmAllAdhesive2ReportsOneClick"
          >
            <IconifyIcon icon="lucide:scan-barcode" class="mb-1 text-[22px] opacity-80" />
            <span class="text-center text-[11px] font-bold leading-tight">一键扫码<br />确认</span>
          </div>
          <div
            v-if="!isWorkOrderPaused && !isWorkOrderCancelled"
            class="flex h-[64px] w-[64px] cursor-pointer flex-col items-center justify-center rounded-xl border border-rose-300 bg-rose-50 text-rose-700 transition-all hover:bg-rose-100 hover:shadow-md active:scale-95"
            @click="handleSegmentComplete"
          >
            <IconifyIcon icon="lucide:badge-check" class="mb-1 text-[22px] opacity-80" />
            <span class="text-[11px] font-bold">本段完成</span>
          </div>
          <div
            v-if="!isWorkOrderPaused && !isWorkOrderCancelled"
            class="flex h-[64px] w-[64px] cursor-pointer flex-col items-center justify-center rounded-xl border border-teal-200 bg-teal-50 text-teal-700 transition-all hover:bg-teal-100 hover:shadow-md active:scale-95"
            @click="reportRecordListVisible = true"
          >
            <IconifyIcon icon="lucide:scan-line" class="mb-1 text-[22px] opacity-80" />
            <span class="text-[11px] font-bold">报工记录</span>
          </div>
        </div>
      </div>

      <section v-show="!visualMaximized" class="erp-card glue-board-card">
        <div class="erp-card-title">
          <IconifyIcon icon="lucide:package-search" />
          胶板型号、料号与边库余量
          <Tag :color="glueBoardStatusMeta.color">{{ glueBoardStatusMeta.text }}</Tag>
          <Button size="small" type="primary" class="consumable-replace-btn" :disabled="isGlueBoardConsumeDisabled" @click="openGlueConsumeDialog">胶板领用</Button>
          <Button size="small" danger class="consumable-replace-btn" :disabled="isCurrentTaskReadonly" @click="openGlueBoardAqcDialog">胶板送检</Button>
          <Button size="small" class="consumable-replace-btn" :disabled="isCurrentTaskReadonly" @click="openGlueBoardLossDialog">损耗报备</Button>
          <span class="glue-board-title-tip" :class="{ warning: glueBoardTitleTipWarning }" :title="glueBoardTitleTip">
            {{ glueBoardTitleTip }}
          </span>
        </div>
        <div class="glue-board-layout glue-board-layout--plain">
          <div class="glue-board-form-grid">
            <div class="glue-board-field">
              <label>胶板型号</label>
              <strong>{{ getActualGlueBoardModel() || '-' }}</strong>
            </div>
            <div class="glue-board-field">
              <label>胶板料号</label>
              <strong>{{ getActualGlueBoardMaterialCode() || '-' }}</strong>
            </div>
            <div class="glue-board-field">
              <label>胶板批号</label>
              <strong>{{ glueBoard.batchNo || '-' }}</strong>
            </div>
            <div class="glue-board-field glue-board-field--highlight">
              <label>边库余量</label>
              <strong>{{ glueBoardAvailableText }}</strong>
            </div>
            <div class="glue-board-field">
              <label>损耗长度</label>
              <strong>{{ formatNumber(glueBoard.lossLength) }} m</strong>
            </div>
          </div>
        </div>
      </section>

      <section v-show="!visualMaximized" class="erp-card plan-scan-card">
        <div class="erp-card-title">
          <IconifyIcon icon="lucide:scan-line" />
          当前计划与扫码
        </div>
        <div class="erp-form-grid">
          <label>扫码计划</label>
          <div class="erp-input-line erp-scan-value">
            <Input ref="planScanInputRef" :value="scanPlanNo" allow-clear class="scan-input" placeholder="请扫码或输入计划号，达到长度后自动查询" @press-enter="consumePlanScan(false)" @update:value="handlePlanScanInput" />
          </div>
          <label>胶板尺寸</label>
          <strong>
            <Tag :color="currentPlan.actualSizeRule === '740mm' ? 'green' : 'blue'" class="!m-0">
              {{ currentPlan.actualSizeRule || currentPlan.planSizeSpec || '-' }}
            </Tag>
            <span class="ml-1 text-xs text-slate-500">计划 {{ currentPlan.planSizeSpec || '-' }}</span>
          </strong>
          <label>分段批号</label><strong>{{ currentPlan.batchNo || currentPlan.sourceBatchNo || stripSegmentMark(currentPlan.sourceProductionBatchNo) || '-' }}</strong>
          <label>当前型号</label>
          <strong class="product-model-display">
            <Tag v-if="isProductModelChanged" color="warning" class="!m-0">换型</Tag>
            <Tag v-if="hasExecutingChangeoverInstruction" color="processing" class="!m-0">指令执行中</Tag>
            <span>{{ currentProductModelCode || '-' }}</span>
            <Button v-if="hasPendingChangeoverInstruction" size="small" type="primary" :disabled="isCurrentTaskReadonly" @click="openChangeoverInstructionDialog">
              <template #icon><IconifyIcon icon="lucide:play" /></template>
              执行换型
            </Button>
            <Button size="small" type="link" :disabled="isCurrentTaskReadonly || hasPendingChangeoverInstruction || hasExecutingChangeoverInstruction" @click="openChangeoverInstructionIssueDialog">
              <template #icon><IconifyIcon icon="lucide:send" /></template>
              下达换型
            </Button>
          </strong>
          <label>计划型号</label><strong>{{ currentPlanModelText }}</strong> <label>当前胶板</label><strong :title="`计划胶板：${currentPlanGlueBoardText}`">{{ currentRuntimeGlueBoardModelText || '-' }}</strong>
          <label>产品料号</label>
          <strong class="product-model-display">
            <span>{{ currentProductMaterialCode || '-' }}</span>
          </strong>
          <label>产品规格</label><strong>{{ currentProductSpecification || '-' }}</strong> <label>可加工数</label><strong>{{ formatNumber(currentPlan.availableSourceLength) }} 片</strong>
          <label>状态</label>
          <strong>
            <Tag :color="currentPlan.planNo ? getWorkOrderStatusMeta(currentPlan.status).color : 'default'">
              {{ currentPlan.planNo ? getWorkOrderStatusMeta(currentPlan.status).text : '待扫码' }}
            </Tag>
          </strong>
          <label>执行要求</label>
          <strong class="erp-full-value erp-requirement-value" :title="executionRequirementText">
            <span>{{ executionRequirementText }}</span>
          </strong>
          <template v-if="currentChangeoverSummaryText">
            <label class="model-change-summary-label">换型指令</label>
            <strong class="model-change-summary" :title="currentChangeoverSummaryText">
              {{ currentChangeoverSummaryText }}
            </strong>
          </template>
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
            :class="{
              'visual-panel--blocked': adhesive2WorkbenchBlockedReason,
            }"
          >
            <div class="panel-title">
              <IconifyIcon icon="lucide:layout-grid" />
              工作台
              <div class="panel-filter-bar">
                <Input v-model:value="visualFilterForm.batchNo" allow-clear size="small" placeholder="扫码批次号/片号" />
                <Select v-model:value="visualFilterForm.status" class="panel-filter-select" :options="adhesive2FilterStatusOptions" size="small" @change="setAdhesive2FilterStatus" />
                <Select v-model:value="visualFilterForm.reportType" class="panel-filter-select panel-filter-select--type" :options="adhesive2ReportTypeFilterOptions" size="small" @change="setAdhesive2FilterReportType" />
                <Button class="panel-filter-refresh" size="small" :loading="workbenchRefreshing" @click="refreshAdhesive2Workbench"> 刷新 </Button>
                <Button class="panel-filter-refresh" size="small" :disabled="isCurrentTaskReadonly || !reportRecords.length" :loading="middleAutoMarking" @click="openAutoMarkAdhesive2MiddleSegments"> 标记中间品 </Button>
              </div>
              <div class="panel-metrics">
                <span>片数 {{ visibleAdhesive2SliceCount }}</span>
                <span>待确认 {{ visibleAdhesive2PendingConfirmCount }}</span>
                <span>本工序NG {{ visibleAdhesive2CurrentNgCount }}</span>
                <span>其他工序NG {{ visibleAdhesive2SourceNgCount }}</span>
                <span>已确认 {{ visibleAdhesive2CompletedCount }}</span>
              </div>
              <div class="press-slot-card-legend" aria-label="粘胶2卡片图示说明">
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
                <Button size="small" type="primary" :disabled="selectedTransferReportCount === 0" @click="handlePrintSelectedTransferReports"> 打印选中 </Button>
                <Button size="small" @click="closeTransferPrintSelector">退出</Button>
              </div>
            </div>
            <div v-if="!sourceGroups.length" class="empty-hint">
              <IconifyIcon icon="lucide:scan-line" />
              {{ currentPlan.planNo ? '暂无已确认的压槽片号，请先完成压槽扫码确认。' : '请先扫描计划号，系统会按后台已确认压槽片号生成工作台。' }}
            </div>
            <div v-else-if="!visualDisplaySourceGroups.length" class="empty-hint">
              <IconifyIcon icon="lucide:filter-x" />
              当前过滤条件下没有匹配的粘胶2片。
            </div>
            <div v-else class="cloth-list press-slot-cloth-list" :class="{ 'is-print-select-mode': transferPrintSelectionMode }">
              <div v-for="group in visualDisplaySourceGroups" :key="group.baseBatchNo" class="cloth-source press-slot-cloth-source">
                <div class="cloth-source-header">
                  <div>
                    <strong>{{ group.baseBatchNo }}</strong>
                    <span>来源 {{ group.planNo || currentPlan.planNo || '-' }}</span>
                    <Tag v-if="group.qtime" :color="getAdhesive2QtimeColor(group.qtime)" :title="group.qtime.message"> 本批 QTIME {{ getAdhesive2QtimeText(group.qtime) }} </Tag>
                  </div>
                  <div v-if="transferPrintSelectionMode" class="source-actions">
                    <Button size="small" @click="selectGroupTransferReports(group)">全选本组</Button>
                  </div>
                </div>
                <div class="cloth-source-body">
                  <div
                    class="slice-grid press-slot-slice-grid"
                    :style="{
                      gridTemplateColumns: getAdhesive2GridColumns(group.segments.length),
                    }"
                  >
                    <QmsSampleAbnormalLockGuard
                  defer-to-cut-round
                      v-for="segment in group.segments"
                      :key="segment.batchNo"
                      :class="getAdhesive2SliceClass(segment)"
                      object-label="分段"
                      :object-no="segment.batchNo"
                      :candidates="getAdhesive2SampleLockCandidates(segment, group.baseBatchNo)"
                      process-name="粘胶2"
                      variant="cell"
                      :title="getAdhesive2SegmentRuntimeTitle(segment)"
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
                          <strong class="slice-cell__batch">{{ getAdhesive2SegmentDisplayBatchNo(segment) }}</strong>
                          <span v-if="getAdhesive2SegmentEditBlockedReason(segment)" class="slice-edit-lock" :title="getAdhesive2SegmentEditBlockedReason(segment)" aria-label="后道工序已报工或送检，当前粘胶2记录不可修改">
                            <IconifyIcon icon="lucide:lock-keyhole" />
                          </span>
                        </span>
                      </div>
                      <div class="slice-cell__info">
                        <span v-if="shouldShowAdhesive2ScanConfirmLine(segment)" class="slice-cell__line" :class="getAdhesive2ScanConfirmLineClass(segment)">
                          <b>扫码</b><em>{{ getAdhesive2ScanConfirmText(segment) }}</em>
                        </span>
                        <span v-for="entry in getAdhesive2SegmentInspectionEntries(segment)" :key="`${segment.batchNo}-${entry.label}-${entry.resultText}`" class="slice-cell__line" :class="entry.tone">
                          <b>{{ entry.label }}</b
                          ><em>{{ entry.resultText }}</em>
                        </span>
                        <span v-if="shouldShowAdhesive2SelfCheckLine(segment)" class="slice-cell__line" :class="getAdhesive2SelfCheckLineClass(segment)" :title="getAdhesive2SegmentNgReason(segment) || getAdhesive2SelfCheckText(segment)">
                          <b>{{ getAdhesive2SelfCheckLabel(segment) }}</b
                          ><em>{{ getAdhesive2SelfCheckText(segment) }}</em>
                        </span>
                        <span v-if="getAdhesive2SegmentPositionText(segment)" class="slice-cell__line is-middle">
                          <b>中间品</b><em>{{ getAdhesive2SegmentPositionText(segment) }}</em>
                        </span>
                        <span v-if="getAdhesive2SegmentRuntimeMismatch(segment)" class="slice-cell__line is-pending">
                          <b>换型</b><em>{{ getAdhesive2SegmentRuntimeText(segment) }}</em>
                        </span>
                      </div>
                    </QmsSampleAbnormalLockGuard>
                  </div>
                </div>
              </div>
            </div>
            <div v-if="adhesive2WorkbenchBlockedReason" class="adhesive-workbench-blocked-mask">
              <div class="adhesive-workbench-blocked-mask__content">
                <IconifyIcon icon="lucide:octagon-alert" />
                <strong>当前粘胶2工序已暂停</strong>
                <span>{{ adhesive2WorkbenchBlockedReason }}</span>
              </div>
            </div>
          </section>
        </TabPane>
        <TabPane key="CHANGEOVER" tab="工艺参数点检">
          <div class="tab-table-content">
            <div class="console-table-shell">
              <div class="rough-grid-toolbar console-record-toolbar">
                <div class="rough-grid-toolbar__title">粘胶2 工艺参数点检记录</div>
                <div class="pass-work-actions rough-grid-toolbar__actions">
                  <span class="console-table-count">记录 {{ changeoverInspections.length }} 条</span>
                  <Button size="small" type="primary" :disabled="isCurrentTaskReadonly" @click="openChangeoverInspectionScan">上报工艺参数</Button>
                </div>
              </div>
              <div class="console-table-body">
                <ATable class="rough-check-table console-record-table" :columns="changeoverInspectionColumns" :data-source="changeoverInspections" :pagination="false" :scroll="{ x: 800, y: 220 }" row-key="id" size="small">
                  <template #bodyCell="{ column, record }">
                    <template v-if="column.dataIndex === 'recordStatus'">
                      <Tag :color="getChangeoverRecordStatusMeta(record).color">
                        {{ getChangeoverRecordStatusMeta(record).text }}
                      </Tag>
                    </template>
                    <template v-if="column.dataIndex === 'confirmUserName'">
                      {{ record.confirmUserName || '-' }}
                    </template>
                    <template v-if="column.dataIndex === 'action'">
                      <Button size="small" type="link" @click="viewChangeoverInspection(record)">查看</Button>
                      <Button size="small" type="link" :disabled="!record.id" @click="printChangeoverInspection(record)">打印</Button>
                      <Button danger size="small" type="link" :disabled="!record.id" @click="deleteChangeoverInspection(record)">删除</Button>
                    </template>
                  </template>
                </ATable>
              </div>
            </div>
          </div>
        </TabPane>
        <TabPane key="COA_INSPECTION" tab="COA送检记录">
          <div class="tab-table-content">
            <div class="console-table-shell">
              <div class="rough-grid-toolbar console-record-toolbar">
                <div class="rough-grid-toolbar__title">COA送检单据详情</div>
                <div class="pass-work-actions rough-grid-toolbar__actions">
                  <span class="console-table-count">
                    分段批号 {{ currentMotherBatchNo || '-' }} / 检验单号
                    {{ latestCoaFai?.faiNo || '-' }}
                  </span>
                  <Button size="small" :disabled="!latestCoaFai?.faiId" @click="viewCoaFaiDetail(latestCoaFai || {})"> 查看FAI明细 </Button>
                  <Button size="small" @click="loadCoaFaiRows">刷新</Button>
                  <Button size="small" :disabled="!latestCoaFai?.faiId" @click="printCoaInspectionTransferTicket()"> 打印检验流转单 </Button>
                </div>
              </div>
              <ATable :data-source="coaFaiRows.filter((row) => !!row.faiId)" row-key="faiId" size="small" :pagination="{ pageSize: 10 }"
                :columns="[
                  { title: '送检单号', dataIndex: 'faiNo', key: 'faiNo' },
                  { title: '片号', dataIndex: 'productBatchNo', key: 'productBatchNo' },
                  { title: '提交时间', dataIndex: 'faiApplyTime', key: 'faiApplyTime' },
                  { title: '状态', key: 'status' },
                  { title: '撤回原因', dataIndex: 'withdrawReason', key: 'withdrawReason' },
                  { title: '操作', key: 'action', width: 220 },
                ]">
                <template #bodyCell="{ column, record }">
                  <Tag v-if="column.key === 'status'" :color="getCoaFaiStatusMeta(record).color">{{ getCoaFaiStatusMeta(record).text }}</Tag>
                  <template v-else-if="column.key === 'action'">
                    <Button type="link" size="small" @click="viewCoaFaiDetail(record)">查看</Button>
                    <Button type="link" size="small" @click="printCoaInspectionTransferTicket(record)">打印</Button>
                    <Button type="link" danger size="small" :disabled="isCurrentTaskReadonly || !record.canWithdraw || withdrawingCoaId !== null"
                      :loading="withdrawingCoaId === record.faiId" :title="record.withdrawBlockedReason || '撤回未处理的COA送检单'"
                      @click="requestCoaWithdrawal(record)">撤回</Button>
                  </template>
                </template>
              </ATable>
              <div class="console-table-body first-inspection-summary">
                <div class="first-inspection-summary__grid">
                  <span>FAI单号</span>
                  <strong>
                    <button v-if="latestCoaFai?.faiId" class="first-inspection-summary__value-link" type="button" @click="viewCoaFaiDetail(latestCoaFai)">
                      {{ latestCoaFai.faiNo || '-' }}
                    </button>
                    <template v-else>-</template>
                  </strong>
                  <span>状态</span>
                  <strong>
                    <Tag :color="getCoaFaiStatusMeta(latestCoaFai || {}).color">
                      {{ getCoaFaiStatusMeta(latestCoaFai || {}).text }}
                    </Tag>
                  </strong>
                  <span>COA送检片号</span>
                  <strong>{{ latestCoaFai?.productBatchNo || '-' }}</strong>
                  <span>分段批号</span>
                  <strong>{{ latestCoaFai?.inspectionScopeBatchNo || currentMotherBatchNo || '-' }}</strong>
                  <span>标准号</span>
                  <strong>{{ latestCoaFai?.faiStandardNo || '-' }}</strong>
                  <span>申请时间</span>
                  <strong>{{ displayDateTimeText(latestCoaFai?.faiApplyTime) }}</strong>
                  <span>检测结论</span>
                  <strong>{{ latestCoaFai?.faiJudgment || '-' }}</strong>
                  <span>来源单号</span>
                  <strong>{{ latestCoaFai?.sourceReportNo || '-' }}</strong>
                  <span>退回/NG原因</span>
                  <strong>{{ latestCoaFai?.faiRejectReason || '-' }}</strong>
                  <span>备注</span>
                  <strong>{{ latestCoaFai?.remark || '-' }}</strong>
                </div>
                <div class="first-inspection-summary__hint">
                  {{ latestCoaFai?.faiId ? '仅显示当前分段批号下最新一张 COA 送检单据。' : '当前分段批号暂无已提交的 COA 送检单据。' }}
                </div>
              </div>
            </div>
          </div>
        </TabPane>
        <TabPane key="GLUE_BOARD_INSPECTION" tab="胶板送检记录">
          <div class="tab-table-content">
            <div class="console-table-shell">
              <div class="rough-grid-toolbar console-record-toolbar">
                <div class="rough-grid-toolbar__title">胶板送检 / FAI 状态</div>
                <div class="pass-work-actions rough-grid-toolbar__actions">
                  <span class="console-table-count">检验单号 {{ firstInspection.sourceReportNo || '-' }}</span>
                  <Button size="small" :disabled="!firstInspection.faiId" @click="viewFirstInspectionDetail"> 查看FAI明细 </Button>
                  <Button size="small" @click="refreshGlueBoardInspectionSummary()">刷新</Button>
                  <Button size="small" :disabled="!firstInspection.faiId" @click="printGlueBoardInspectionTransferTicket()"> 打印检验流转单 </Button>
                </div>
              </div>
              <div class="console-table-body first-inspection-summary">
                <div class="first-inspection-summary__grid">
                  <span>FAI单号</span>
                  <strong>
                    <button v-if="firstInspection.faiId" class="first-inspection-summary__value-link" type="button" @click="viewFirstInspectionDetail">
                      {{ firstInspection.faiNo || '-' }}
                    </button>
                    <template v-else>-</template>
                  </strong>
                  <span>状态</span>
                  <strong>
                    <Tag :color="glueBoardLatestInspectionStatusMeta.color">
                      {{ glueBoardLatestInspectionStatusMeta.stampText }} /
                      {{ glueBoardLatestInspectionStatusMeta.subText }}
                    </Tag>
                  </strong>
                  <span>胶板批号</span>
                  <strong>{{ firstInspection.glueBoardBatchNo || glueBoard.batchNo || '-' }}</strong>
                  <span>胶板料号</span>
                  <strong>{{ firstInspection.glueBoardMaterialCode || getActualGlueBoardMaterialCode() || '-' }}</strong>
                  <span>送检长度</span>
                  <strong>{{ firstInspection.sampleLength ? `${formatNumber(firstInspection.sampleLength)} m` : '-' }}</strong>
                  <span>取样起点</span>
                  <strong>{{ firstInspection.sampleStartPosition !== undefined && firstInspection.sampleStartPosition !== null ? `${formatNumber(firstInspection.sampleStartPosition)} m` : '-' }}</strong>
                  <span>申请时间</span>
                  <strong>{{ displayDateTimeText(firstInspection.faiApplyTime) }}</strong>
                  <span>检测结论</span>
                  <strong>{{ firstInspection.faiJudgment || '-' }}</strong>
                  <span>退回/NG原因</span>
                  <strong>{{ firstInspection.faiRejectReason || '-' }}</strong>
                </div>
                <div class="first-inspection-summary__hint">
                  {{ glueBoardInspectionTabNotice }}
                </div>
                <div class="first-inspection-summary__hint">
                  {{ '胶板领用后会提示建议完成胶板送检，不阻断报工；若检验判定 NG，关联报工会标记异常。' }}
                </div>
              </div>
            </div>
          </div>
        </TabPane>
        <TabPane v-if="false" key="EDGE_CONSUMABLE" tab="胶板耗材余额">
          <div class="tab-table-content">
            <Adhesive2GlueBoardEdgeConsumableTab
              ref="glueBoardEdgeConsumableTabRef"
              :plan-no="currentPlan.planNo"
              :production-batch-no="currentMotherBatchNo || currentPlan.sourceBatchNo || currentPlan.batchNo"
              :readonly="isCurrentTaskReadonly"
              :table-height="320"
            />
          </div>
        </TabPane>
        <TabPane key="INSTRUCTION_MESSAGES">
          <template #tab>
            <Badge :count="instructionUnreadCount" :offset="[8, -4]" :overflow-count="99" size="small">
              <span>指令消息</span>
            </Badge>
          </template>
          <ProductionInstructionMessageTab :context="adhesive2InstructionContext" title="粘胶2指令消息" @unread-change="handleInstructionUnreadChange" />
        </TabPane>
        <TabPane key="MIDDLE_LEDGER" tab="中间品">
          <div class="tab-table-content">
            <div class="console-table-shell">
              <div class="rough-grid-toolbar console-record-toolbar">
                <div class="rough-grid-toolbar__title">粘胶2中间品记录表</div>
                <div class="pass-work-actions rough-grid-toolbar__actions">
                  <span class="console-table-count">记录 {{ adhesive2IntermediateRecords.length }} 张</span>
                  <Button size="small" type="primary" :loading="intermediateLoading" @click="openNewIntermediateRecordDetail">新建</Button>
                  <Button size="small" :loading="intermediateLoading" @click="loadAdhesive2IntermediateRecords">刷新</Button>
                </div>
              </div>
              <div class="console-table-body">
                <ATable
                  class="rough-check-table console-record-table"
                  :columns="adhesive2MiddleLedgerColumns"
                  :data-source="adhesive2IntermediateRecords"
                  :loading="intermediateLoading"
                  :pagination="false"
                  :scroll="{ x: 1710, y: 220 }"
                  row-key="id"
                  size="small"
                >
                  <template #bodyCell="{ column, record }">
                    <template v-if="column.dataIndex === 'batchNo'">
                      <Button size="small" type="link" @click="openIntermediateRecordDetail(record)">
                        {{ record.batchNo || '-' }}
                      </Button>
                    </template>
                    <template v-if="column.dataIndex === 'recordStatus'">
                      <Tag :color="record.recordStatus === 'CONFIRMED' ? 'processing' : record.recordStatus === 'RECORDED' ? 'success' : 'default'">
                        {{ record.recordStatus === 'CONFIRMED' ? '已确认' : record.recordStatus === 'RECORDED' ? '已保存' : '草稿' }}
                      </Tag>
                    </template>
                    <template v-if="column.dataIndex === 'action'">
                      <Button size="small" type="link" @click="openIntermediateRecordDetail(record)">查看/填写</Button>
                    </template>
                  </template>
                </ATable>
              </div>
            </div>
          </div>
        </TabPane>
        <TabPane v-if="false" key="PROCESS_CHECK" tab="过程加检">
          <div class="tab-table-content">
            <div class="console-table-shell">
              <div class="rough-grid-toolbar console-record-toolbar">
                <div class="rough-grid-toolbar__title">过程加检 FAI 单据</div>
                <div class="pass-work-actions rough-grid-toolbar__actions">
                  <span class="console-table-count"> 分段批号 {{ currentMotherBatchNo || '-' }} / 记录 {{ processCheckFaiRows.length }} 条 </span>
                  <Button size="small" @click="loadProcessCheckFaiRows">刷新</Button>
                  <Button size="small" :disabled="!latestProcessCheckFai?.faiId" @click="printLatestProcessCheckInspectionTransferTicket"> 打印检验流转单 </Button>
                </div>
              </div>
              <div class="console-table-body">
                <ATable class="rough-check-table console-record-table" :columns="adhesive2ProcessCheckFaiColumns" :data-source="processCheckFaiRows" :pagination="false" :scroll="{ x: 1870, y: 220 }" row-key="faiId" size="small">
                  <template #bodyCell="{ column, record }">
                    <template v-if="column.dataIndex === 'faiNo'">
                      <button v-if="record.faiId" class="first-inspection-summary__value-link" type="button" @click="viewProcessCheckFaiDetail(record)">
                        {{ record.faiNo || '-' }}
                      </button>
                      <template v-else>-</template>
                    </template>
                    <template v-if="column.dataIndex === 'faiStatus'">
                      <Tag :color="getCoaFaiStatusMeta(record).color">
                        {{ getCoaFaiStatusMeta(record).text }}
                      </Tag>
                    </template>
                    <template v-if="column.dataIndex === 'faiApplyTime'">
                      {{ displayDateTimeText(record.faiApplyTime) }}
                    </template>
                    <template v-if="column.dataIndex === 'action'">
                      <Button size="small" type="link" :disabled="!record.faiId" @click="viewProcessCheckFaiDetail(record)"> 查看FAI明细 </Button>
                      <Button size="small" type="link" :disabled="!record.faiId" @click="printProcessCheckInspectionTransferTicket(record)"> 打印检验流转单 </Button>
                    </template>
                  </template>
                </ATable>
              </div>
            </div>
          </div>
        </TabPane>
        <TabPane v-if="showExtendedBoardTabs" key="CHECK" tab="点检/清洁记录">
          <div class="tab-table-content">
            <ATable class="rough-check-table" :columns="dailyRecordColumns" :data-source="dailyRecordRows" :pagination="false" :scroll="{ x: 1130, y: 176 }" row-key="key" size="small">
              <template #bodyCell="{ column, record }">
                <template v-if="column.dataIndex === 'status'">
                  <Tag :color="getDailyRecordStatusMeta(record.status).color">
                    {{ getDailyRecordStatusMeta(record.status).text }}
                  </Tag>
                </template>
                <template v-if="column.dataIndex === 'recorderInfo'">
                  <div>{{ record.recorder || '-' }}</div>
                  <div class="rough-table-sub">
                    {{ record.recorderTime || '-' }}
                  </div>
                </template>
                <template v-if="column.dataIndex === 'confirmerInfo'">
                  <div>{{ record.confirmer || '-' }}</div>
                  <div class="rough-table-sub">
                    {{ record.confirmerTime || '-' }}
                  </div>
                </template>
                <template v-if="column.dataIndex === 'action'">
                  <div class="pass-work-actions">
                    <Button size="small" type="link" :disabled="!canFillDailyRecord(record)" @click="openDailyRecord(record, 'edit')"> 填写 </Button>
                    <Button size="small" type="link" :disabled="!canConfirmDailyRecord(record)" @click="openDailyRecord(record, 'confirm')"> 确认 </Button>
                    <Button size="small" type="link" :disabled="!canViewDailyRecord(record)" @click="openDailyRecord(record, 'view')">查看</Button>
                  </div>
                </template>
              </template>
            </ATable>
          </div>
        </TabPane>
        <TabPane v-if="showExtendedBoardTabs" key="RECORDS" tab="今日粘胶2报工记录">
          <div class="tab-table-content">
            <div class="console-table-shell">
              <div class="rough-grid-toolbar console-record-toolbar">
                <div class="rough-grid-toolbar__title">今日粘胶2报工记录</div>
                <div class="pass-work-actions rough-grid-toolbar__actions">
                  <div class="console-record-date-filter">
                    <span>扫码确认日期</span>
                    <DatePicker v-model:value="reportScanConfirmDate" allow-clear placeholder="扫码确认日期" size="small" value-format="YYYY-MM-DD" />
                  </div>
                  <span class="console-table-count">记录 {{ reportRecords.length }} 条 / 已选 {{ selectedReportRecords.length }} 条</span>
                  <Button size="small" :disabled="isCurrentTaskReadonly || !reportRecords.length" @click="openSelectedRecordConfirm">扫码确认</Button>
                  <Button size="small" :disabled="!printableReportRecords.length" @click="printSelectedAdhesiveRecords">选择打印</Button>
                  <Button size="small" :disabled="!allPrintableReportRecords.length" @click="printAllAdhesiveRecords">打印全部报工</Button>
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
      </Tabs>

      <AModal
        v-model:open="intermediateDetailVisible"
        :footer="null"
        :title="null"
        :width="'100vw'"
        class="rough-prototype-modal press-slot-middle-modal"
        destroy-on-close
        wrap-class-name="hc-pass-work-modal press-slot-middle-modal-wrap"
        @cancel="closeIntermediateRecordDetail"
      >
        <div class="tab-table-content press-slot-middle-tab">
          <div class="console-table-shell press-slot-middle-shell">
            <div class="rough-grid-toolbar console-record-toolbar">
              <div class="rough-grid-toolbar__title">
                {{ intermediateForm.templateName || intermediateForm.stationFormDisplayName || '粘胶2中间品记录表' }}
              </div>
              <div class="pass-work-actions rough-grid-toolbar__actions">
                <span class="console-table-count"> 投入 {{ formatNumber(intermediateForm.inputQty) }} PCS / 产出 {{ formatNumber(intermediateForm.outputQty) }} PCS </span>
                <Tag :color="intermediateForm.recordStatus === 'CONFIRMED' ? 'processing' : intermediateForm.recordStatus === 'RECORDED' ? 'success' : 'default'">
                  {{ intermediateForm.recordStatus === 'CONFIRMED' ? '已确认' : intermediateForm.recordStatus === 'RECORDED' ? '已保存' : '草稿' }}
                </Tag>
                <Button size="small" :loading="intermediateLoading" @click="loadIntermediateRecord(selectedIntermediateRecord || undefined)">刷新</Button>
                <Button size="small" :disabled="intermediateRecordLocked" :loading="middleLedgerImporting" @click="triggerMiddleLedgerImport">导入Excel</Button>
                <Button size="small" @click="exportMiddleLedgerExcel">导出Excel</Button>
                <Button size="small" type="primary" :disabled="intermediateRecordLocked" :loading="intermediateLoading" @click="openIntermediateSaveAuth">保存</Button>
                <Button size="small" type="primary" :disabled="intermediateRecordLocked" :loading="intermediateLoading" @click="confirmIntermediateRecord">确认</Button>
                <Button size="small" @click="closeIntermediateRecordDetail">关闭</Button>
              </div>
            </div>
            <div class="press-slot-middle-body">
              <div class="press-slot-middle-sheet">
                <div class="press-slot-section-title">
                  <span>主表信息</span>
                </div>
                <div class="press-slot-middle-form">
                  <label>生产日期</label>
                  <div class="middle-product-header-input">
                    <DatePicker v-model:value="intermediateForm.recordDate" :disabled="intermediateRecordLocked" format="YYYY-MM-DD HH:mm:ss" show-time value-format="YYYY-MM-DD HH:mm:ss" size="small" />
                  </div>
                  <label>型号</label><strong>{{ intermediateForm.modelCode || '-' }}</strong> <label>料号</label><strong>{{ intermediateForm.materialCode || '-' }}</strong> <label>批号</label
                  ><strong>{{ intermediateForm.batchNo || '-' }}</strong>
                  <label>填写人</label>
                  <Input v-model:value="intermediateForm.recorderName" :disabled="intermediateRecordLocked" size="small" />
                  <label>填写时间</label><strong>{{ displayDateTimeText(intermediateForm.fillTime || intermediateForm.updateTime || intermediateForm.createTime) }}</strong>
                  <label>确认人</label>
                  <Input v-model:value="intermediateForm.confirmerName" :disabled="intermediateRecordLocked" size="small" />
                  <label>确认时间</label><strong>{{ displayDateTimeText(intermediateForm.confirmTime) }}</strong>
                  <label>胶板型号</label>
                  <Input v-model:value="intermediateForm.glueBoardModel" :disabled="intermediateRecordLocked" size="small" />
                  <label>宽幅/MM</label>
                  <Input v-model:value="intermediateForm.glueBoardWidthMm" :disabled="intermediateRecordLocked" size="small" />
                  <label>使用面粘性/gf</label>
                  <Input v-model:value="intermediateForm.glueBoardAdhesionGf" :disabled="intermediateRecordLocked" size="small" />
                </div>
              </div>
              <div class="press-slot-middle-attachment">
                <div class="press-slot-middle-attachment__title">原始导入附件</div>
                <div v-if="intermediateImportAttachment" class="press-slot-middle-attachment__content">
                  <button class="press-slot-middle-attachment__link" type="button" @click="openAdhesive2ImportAttachment(intermediateImportAttachment)">
                    <IconifyIcon icon="lucide:paperclip" />
                    <span>{{ intermediateImportAttachment.name || '原始导入文件' }}</span>
                  </button>
                  <span>导入时间：{{ intermediateImportAttachment.uploadTime || '-' }}</span>
                  <span v-if="formatAdhesive2AttachmentSize(intermediateImportAttachment.size)"> 大小：{{ formatAdhesive2AttachmentSize(intermediateImportAttachment.size) }} </span>
                </div>
                <span v-else class="press-slot-middle-attachment__empty">暂无原始导入附件</span>
              </div>
              <div class="press-slot-middle-sheet press-slot-middle-sheet--table">
                <div class="press-slot-section-title">
                  <span>前段 / 中段 / 后段厚度测量</span>
                  <em
                    >每片每 {{ intermediateThicknessIntervalCm }}cm 测量一次厚度，共
                    {{ intermediateThicknessColumnCount }}
                    个测量点；厚度标准：{{ intermediateThicknessStandardText }}mm</em
                  >
                </div>
                <div class="middle-product-detail-table press-slot-middle-detail-table">
                  <ATable
                    class="rough-check-table"
                    :columns="intermediateDetailColumns"
                    :data-source="intermediateTableRows"
                    :loading="intermediateLoading"
                    :pagination="false"
                    :scroll="{ x: 2500, y: 'calc(100vh - 390px)' }"
                    row-key="samplePosition"
                    size="small"
                  >
                    <template #bodyCell="{ column, record }">
                      <template v-if="column.dataIndex === 'sliceBatchNo'">
                        <Input
                          v-model:value="record.sliceBatchNo"
                          class="adhesive-intermediate-cell"
                          :disabled="intermediateRecordLocked"
                          size="small"
                          @change="syncIntermediateSliceNoFieldsFromDetails"
                          @keydown="handleIntermediateCellKeydown"
                        />
                      </template>
                      <template v-if="isIntermediateThicknessColumn(column.dataIndex)">
                        <InputNumber
                          :value="getIntermediateNumericValue(record, column.dataIndex)"
                          class="adhesive-intermediate-cell full-input"
                          :min="0"
                          :precision="3"
                          :disabled="intermediateRecordLocked"
                          size="small"
                          @change="(value) => setIntermediateNumericValue(record, column.dataIndex, value as number | null)"
                          @keydown="handleIntermediateCellKeydown"
                        />
                      </template>
                      <template v-if="column.dataIndex === 'remark'">
                        <Input v-model:value="record.remark" class="adhesive-intermediate-cell" :disabled="intermediateRecordLocked" size="small" @keydown="handleIntermediateCellKeydown" />
                      </template>
                    </template>
                  </ATable>
                </div>
              </div>
            </div>
          </div>
        </div>
      </AModal>

      <AModal v-model:open="taskListVisible" :footer="null" :width="1280" class="rough-prototype-modal" :title="adhesive2TaskListTitle">
        <div class="console-table-shell console-table-shell--modal task-list-table-shell">
          <div class="rough-grid-toolbar console-record-toolbar">
            <div class="rough-grid-toolbar__title">
              {{ adhesive2TaskListTitle }}
            </div>
            <div class="pass-work-actions rough-grid-toolbar__actions">
              <span class="console-table-count">筛选 {{ taskListFilteredRows.length }} 条 / 共 {{ taskRows.length }} 条</span>
              <Button size="small" :loading="taskListLoading" @click="loadTaskList">刷新</Button>
            </div>
          </div>
          <div class="task-list-filter-bar">
            <div class="task-list-filter-item">
              <label>计划号</label>
              <Input v-model:value="taskListFilters.planNo" allow-clear placeholder="请输入计划号" size="small" @press-enter="handleTaskListSearch" />
            </div>
            <div class="task-list-filter-item">
              <label>产品型号</label>
              <Input v-model:value="taskListFilters.modelCode" allow-clear placeholder="请输入产品型号" size="small" @press-enter="handleTaskListSearch" />
            </div>
            <div class="task-list-filter-item">
              <label>批次</label>
              <Input v-model:value="taskListFilters.batchNo" allow-clear placeholder="请输入批次" size="small" @press-enter="handleTaskListSearch" />
            </div>
            <div class="task-list-filter-item task-list-filter-item--status">
              <label>状态</label>
              <Select v-model:value="taskListFilters.status" :options="taskListStatusOptions" size="small" />
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
              :custom-row="
                (record) => ({
                  onDblclick: () => openStartTaskSizeRule(record),
                })
              "
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

      <AModal v-model:open="equipmentSelectVisible" :footer="null" :width="820" class="rough-prototype-modal" title="选择粘胶2设备" @cancel="closeEquipmentSelect">
        <div class="console-table-shell console-table-shell--modal">
          <div class="rough-grid-toolbar console-record-toolbar">
            <div class="rough-grid-toolbar__title">
              当前机台：{{ selectedBoardEquipmentCode || '未绑定' }} /
              {{ selectedBoardEquipmentName || '-' }}
            </div>
            <div class="pass-work-actions rough-grid-toolbar__actions">
              <Input v-model:value="equipmentSelectKeyword" allow-clear class="process-param-filter process-param-filter--slice" placeholder="设备编码/名称" size="small" @press-enter="loadEquipmentSelectRows" />
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

      <AModal v-model:open="sizeRuleVisible" :footer="null" :width="560" class="rough-prototype-modal" title="选择本次胶板尺寸与型号">
        <div class="size-rule-panel">
          <div class="size-rule-summary">
            <label>计划号</label><strong>{{ pendingStartTask?.planNo || '-' }}</strong> <label>计划胶板尺寸</label><strong>{{ sizeRuleForm.planSizeSpec || '-' }}</strong>
          </div>
          <div class="size-rule-section">
            <label>实际胶板尺寸</label>
            <RadioGroup v-model:value="sizeRuleForm.actualSizeRule" button-style="solid" class="size-rule-radio-group">
              <RadioButton v-for="option in SIZE_RULE_OPTIONS" :key="option.value" :value="option.value">
                <span class="size-rule-dot" :style="{ '--rule-color': option.color }"></span>
                {{ option.label }}
              </RadioButton>
            </RadioGroup>
          </div>
          <div class="size-rule-section">
            <label>胶板型号</label>
            <RadioGroup v-model:value="sizeRuleForm.glueBoardModel" button-style="solid" class="size-rule-radio-group">
              <RadioButton v-for="option in GLUE_BOARD_MODEL_OPTIONS" :key="option.value" :value="option.value">
                {{ option.label }}
              </RadioButton>
            </RadioGroup>
          </div>
          <div class="modal-footer">
            <Button @click="sizeRuleVisible = false">取消</Button>
            <Button type="primary" @click="confirmStartTaskSizeRule">确认开工</Button>
          </div>
        </div>
      </AModal>

      <AModal v-model:open="selectedTailVisible" :footer="null" :width="900" class="rough-prototype-modal" destroy-on-close title="粘胶2扫码改尾号" :closable="!selectedTailSubmitting" :keyboard="false" :mask-closable="false">
        <div class="record-confirm-panel">
          <div class="record-confirm-summary">
            <span>当前计划</span><strong>{{ currentPlan.planNo || '-' }}</strong>
            <span>分段批号</span><strong>{{ currentMotherBatchNo || '-' }}</strong>
            <span>当前胶板尺寸</span><strong>{{ currentPlan.actualSizeRule || currentPlan.planSizeSpec || '-' }}</strong>
            <span>胶板型号</span><strong>{{ getSelectedGlueBoardModel() || '-' }}</strong>
          </div>
          <div class="record-confirm-type-row">
            <span>目标尾号</span>
            <RadioGroup v-model:value="selectedTailSizeRule" :disabled="selectedTailSubmitting" @change="focusInputRef(selectedTailScanInputRef)">
              <Radio value="775mm">775mm / 尾号A</Radio>
              <Radio value="740mm">740mm / 尾号B</Radio>
            </RadioGroup>
          </div>
          <div class="record-confirm-basis">片号输入与尺寸选择不分先后，填写完成后点击确认或回车修改本片；所选尾号保留，可连续扫描下一片。</div>
          <div class="record-confirm-scan">
            <IconifyIcon icon="lucide:scan-line" />
            <Input
              ref="selectedTailScanInputRef"
              v-model:value="selectedTailScanCode"
              allow-clear
              :disabled="selectedTailSubmitting"
              placeholder="请扫描流转单上的粘胶2片号，也可人工输入后回车"
              @update:value="selectedTailRow = undefined"
              @press-enter="scanSelectedTail"
            />
          </div>
          <div v-if="selectedTailSubmitting" class="record-confirm-basis">正在修改本片尾号，请稍候…</div>
          <div v-if="selectedTailFeedback" class="record-scan-confirm__message">{{ selectedTailFeedback }}</div>
          <div class="record-confirm-basis">未报工片如已打印流转单，改号后请作废旧单并重新打印。</div>
          <div class="modal-footer">
            <Button :disabled="selectedTailSubmitting" @click="selectedTailVisible = false">取消</Button>
            <Button type="primary" :loading="selectedTailSubmitting" @click="scanSelectedTail">确认</Button>
          </div>
        </div>
      </AModal>

      <AModal v-model:open="tailAssignVisible" :footer="null" :width="760" class="rough-prototype-modal" title="粘胶2一键选择尾号">
        <div class="size-rule-panel">
          <div class="size-rule-summary">
            <label>当前片数</label><strong>{{ tailAssignRows.length }} 片</strong> <label>操作说明</label><strong>整体改写全部片号，包括已按片设置的不同尾号；请核对以下预览。</strong>
          </div>
          <div class="size-rule-section">
            <label>统一尾号</label>
            <RadioGroup v-model:value="tailAssignDefaultSizeRule" button-style="solid" class="size-rule-radio-group">
              <RadioButton v-for="option in SIZE_RULE_OPTIONS" :key="option.value" :value="option.value">
                <span class="size-rule-dot" :style="{ '--rule-color': option.color }"></span>
                {{ option.label }} / 尾号{{ option.value === '775mm' ? 'A' : 'B' }}
              </RadioButton>
            </RadioGroup>
          </div>
          <div class="tail-assign-list">
            <div v-for="row in tailAssignRows" :key="row.sourceProductionBatchNo" class="tail-assign-row">
              <strong>{{ getAdhesive2TailBatchNo(row.sourceProductionBatchNo, tailAssignDefaultSizeRule) }}</strong>
            </div>
          </div>
          <div class="size-rule-hint">尾号只是片号属性，不产生新的确认状态；已确认、NG 和已打印的当前片号均会按本次选择统一更新。</div>
          <div class="modal-action-row">
            <Button @click="tailAssignVisible = false">取消</Button>
            <Button type="primary" :loading="tailAssignSubmitting" @click="submitAdhesive2TailAssign">保存尾号选择</Button>
          </div>
        </div>
      </AModal>

      <ProductionInstructionIssueModal
        v-model:open="changeoverInstructionIssueVisible"
        :before-submit="authenticateChangeoverInstructionIssue"
        :context="adhesive2ChangeoverIssueContext"
        default-instruction-type="CHANGEOVER"
        :instruction-type-options="[{ label: '换型指令', value: 'CHANGEOVER' }]"
        :show-instruction-type="false"
        :show-operation="false"
        :show-segment="false"
        title="下达粘胶2换型指令"
        :z-index="5000"
        @success="handleChangeoverInstructionIssued"
      />

      <AModal v-model:open="changeoverInstructionVisible" :footer="null" :width="620" class="rough-prototype-modal" title="执行换型指令">
        <div class="size-rule-panel model-change-panel">
          <div class="changeover-instruction-summary">
            <label>指令号</label><strong>{{ currentChangeoverInstruction?.instructionNo || '-' }}</strong> <label>目标型号</label><strong>{{ currentChangeoverInstruction?.targetModelCode || '-' }}</strong> <label>目标片数</label
            ><strong>{{ currentChangeoverInstruction?.targetQty || '-' }} 片</strong> <label>已完成</label><strong>{{ currentChangeoverInstruction?.completedQty || 0 }} 片</strong>
          </div>
          <div class="model-change-form">
            <div class="model-change-form-row">
              <label>计划型号</label>
              <Input :value="currentChangeoverInstruction?.beforeModelCode || currentPlan.modelCode || '-'" disabled />
            </div>
            <div class="model-change-form-row model-change-form-row--wide">
              <label>执行备注</label>
              <Textarea v-model:value="changeoverInstructionForm.remark" :auto-size="{ minRows: 2, maxRows: 4 }" placeholder="可填写本次换型执行说明" />
            </div>
          </div>
          <div class="glue-board-message">开始执行后当前型号切换为目标型号，并清除不匹配的胶板领用。请按目标型号重新领用并送检胶板；扫码确认达到目标片数后结束本次指令，当前型号继续保持为目标型号。</div>
          <div class="modal-footer">
            <Button @click="changeoverInstructionVisible = false">取消</Button>
            <Button type="primary" :disabled="isCurrentTaskReadonly" :loading="changeoverInstructionStarting" @click="requestStartChangeoverInstructionAuth"> 开始执行 </Button>
          </div>
        </div>
      </AModal>

      <AModal v-model:open="reportRecordListVisible" :footer="null" :width="1180" title="今日粘胶2报工记录">
        <div class="console-table-shell console-table-shell--modal">
          <div class="rough-grid-toolbar console-record-toolbar">
            <div class="rough-grid-toolbar__title">今日粘胶2报工记录</div>
            <div class="pass-work-actions rough-grid-toolbar__actions">
              <div class="console-record-date-filter">
                <span>扫码确认日期</span>
                <DatePicker v-model:value="reportScanConfirmDate" allow-clear placeholder="扫码确认日期" size="small" value-format="YYYY-MM-DD" />
              </div>
              <span class="console-table-count">记录 {{ reportRecords.length }} 条 / 已选 {{ selectedReportRecords.length }} 条</span>
              <Button size="small" :disabled="isCurrentTaskReadonly || !reportRecords.length" @click="openSelectedRecordConfirm">扫码确认</Button>
              <Button size="small" :disabled="!printableReportRecords.length" @click="printSelectedAdhesiveRecords">选择打印</Button>
              <Button size="small" :disabled="!allPrintableReportRecords.length" @click="printAllAdhesiveRecords">打印全部报工</Button>
            </div>
          </div>
          <div class="console-table-body">
            <ATable class="rough-check-table console-record-table" :columns="recordColumns" :data-source="reportRecords" :pagination="false" :row-selection="reportRecordRowSelection" :scroll="{ x: 920, y: 420 }" row-key="id" size="small">
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

      <AModal v-model:open="dailyRecordListVisible" :footer="null" :width="760" class="rough-prototype-modal" title="今日粘胶2点检/清洁记录">
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
          <div v-if="dailyRecordRows.length === 0" class="console-empty compact">暂无点检清洁记录，请检查粘胶2动态表单配置。</div>
        </div>
        <div class="modal-footer daily-check-close-footer">
          <Button type="primary" @click="dailyRecordListVisible = false">已检关闭</Button>
        </div>
      </AModal>

      <AModal v-model:open="dailyRecordVisible" :footer="null" :title="null" width="100vw" wrap-class-name="hc-pass-work-modal rough-prep-work-modal" @cancel="dailyRecordVisible = false">
        <div class="pp-plan-modal">
          <div class="pp-plan-toolbar">
            <div class="pp-plan-toolbar__title pass-work-dialog-title">
              <span class="pp-plan-toolbar__main"> {{ dailyRecordMode === 'edit' ? '填写' : dailyRecordMode === 'confirm' ? '确认' : '查看' }}{{ selectedDailyRecord?.name || '工作准备记录' }} </span>
              <div class="toolbar-meta">
                <span>计划：{{ currentPlan.planNo || '-' }}</span>
                <span>机台：{{ selectedBoardEquipmentCode || '-' }} / {{ selectedBoardEquipmentName || '-' }}</span>
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
                <div class="head-item">
                  <span class="head-item__label">表单名称：</span><span class="head-item__value">{{ selectedDailyRecord?.name || '-' }}</span>
                </div>
                <div class="head-item">
                  <span class="head-item__label">执行时机：</span><span class="head-item__value">{{ selectedDailyRecord?.timing || '-' }}</span>
                </div>
                <div class="head-item">
                  <span class="head-item__label">记录人：</span><span class="head-item__value">{{ selectedDailyRecord?.recorder || '-' }}</span>
                </div>
                <div class="head-item">
                  <span class="head-item__label">记录时间：</span><span class="head-item__value">{{ selectedDailyRecord?.recorderTime || '-' }}</span>
                </div>
                <div class="head-item">
                  <span class="head-item__label">确认人：</span><span class="head-item__value">{{ selectedDailyRecord?.confirmer || '-' }}</span>
                </div>
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
                        <Input v-if="dailyRecordMode !== 'view'" v-model:value="row.remark" :data-daily-cell="`${index}-remark`" size="small" @keydown="handleDailyCellKeydown($event, index, 'remark')" />
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

      <AModal v-model:open="glueConsumeVisible" :footer="null" :width="720" class="rough-prototype-modal" title="胶板领用登记">
        <div class="glue-consume-form">
          <div class="rough-material-scan-row">
            <div class="pp-form-item rough-material-scan-row__field">
              <label>胶板批号（选择/扫码）</label>
              <Input v-model:value="glueConsumeForm.materialScanCode" placeholder="扫码、输入或点击选择胶板边库批号" @press-enter="applyGlueBoardScan" />
            </div>
            <Button size="small" :disabled="isGlueBoardConsumeDisabled" @click="openGlueStockSelect">选择批次</Button>
            <Button size="small" type="primary" @click="applyGlueBoardScan">带出边库批次</Button>
          </div>
          <div class="pp-form-grid rough-material-scan-grid">
            <div class="pp-form-item">
              <label>胶板型号</label>
              <div class="pp-readonly-box">
                {{ getActualGlueBoardModel() || '-' }}
              </div>
            </div>
            <div class="pp-form-item">
              <label>胶板批号</label>
              <div class="pp-readonly-box">
                {{ glueConsumeForm.batchNo || '-' }}
              </div>
            </div>
            <div class="pp-form-item">
              <label>边库余量</label>
              <div class="pp-readonly-box">{{ glueBoardAvailableText }}</div>
            </div>
            <div class="pp-form-item"><label>本次领用(m)</label><InputNumber v-model:value="glueConsumeForm.receiveLength" class="w-full" :min="0" :precision="3" disabled /></div>
          </div>
          <div v-if="glueBoard.alarm" class="record-scan-confirm__error">
            {{ glueBoard.alarm }}
          </div>
          <div class="modal-footer">
            <Button @click="glueConsumeVisible = false">取消</Button>
            <Button type="primary" :disabled="isGlueBoardConsumeDisabled" @click="confirmGlueConsume">确认领用</Button>
          </div>
        </div>
      </AModal>

      <AModal v-model:open="glueStockSelectVisible" :footer="null" :width="860" class="rough-prototype-modal" title="选择胶板边库批次">
        <div class="console-table-shell console-table-shell--modal">
          <div class="rough-grid-toolbar console-record-toolbar">
            <div class="rough-grid-toolbar__title">
              胶板型号
              {{ glueBoardMapCandidateModels.length ? glueBoardMapCandidateModels.join('、') : '未限定' }}
            </div>
            <div class="pass-work-actions rough-grid-toolbar__actions">
              <Input v-model:value="glueStockFilter.materialCode" allow-clear class="glue-stock-material-filter" placeholder="按胶板料号过滤" size="small" @press-enter="loadGlueStockRows({ resetPage: true })" />
              <Input v-model:value="glueStockFilter.model" allow-clear class="glue-stock-model-filter" placeholder="按胶板型号过滤" size="small" @press-enter="loadGlueStockRows({ resetPage: true })" />
              <Input v-model:value="glueStockFilter.batchNo" allow-clear class="glue-stock-batch-filter" placeholder="按胶板批号过滤" size="small" @press-enter="loadGlueStockRows({ resetPage: true })" />
              <span class="console-table-count">批次 {{ glueStockPagination.total }} 条</span>
              <Button size="small" :loading="glueStockSelectLoading" @click="loadGlueStockRows({ resetPage: true })">刷新</Button>
            </div>
          </div>
          <div class="glue-board-map-filter-tip" :class="{ warning: glueBoardMapCandidates.length === 0 }">
            <span>{{ glueBoardMapFilterNotice }}</span>
            <template v-if="glueBoardMapCandidates.length > 0">
              <Tag v-for="candidate in glueBoardMapCandidates" :key="`${candidate.glueBoardMaterialCode || ''}-${candidate.glueBoardModel || ''}`" color="processing">
                {{ formatGlueBoardMapCandidate(candidate) || '-' }}
              </Tag>
            </template>
          </div>
          <div class="console-table-body">
            <ATable
              class="rough-check-table console-record-table"
              :columns="glueBoardStockSelectColumns"
              :data-source="glueStockRows"
              :loading="glueStockSelectLoading"
              :pagination="false"
              :scroll="{ x: 980, y: 360 }"
              row-key="id"
              size="small"
              :custom-row="(record) => ({ onDblclick: () => selectGlueBoardStock(record) })"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.dataIndex === 'availableLength'">
                  {{ formatNumber(record.availableLength) }}
                </template>
                <template v-if="column.dataIndex === 'receiveTime'">
                  {{ record.receiveTime ? dayjs(record.receiveTime).format('YYYY-MM-DD HH:mm:ss') : '-' }}
                </template>
                <template v-if="column.dataIndex === 'qualityStatus'">
                  <Tag :color="getGlueBoardStockQualityMeta(record).color">
                    {{ getGlueBoardStockQualityMeta(record).text }}
                  </Tag>
                </template>
                <template v-if="column.dataIndex === 'action'">
                  <Button size="small" type="link" @click="selectGlueBoardStock(record)">选择</Button>
                </template>
              </template>
            </ATable>
          </div>
          <div class="task-list-pagination glue-stock-pagination">
            <Pagination
              :current="glueStockPagination.pageNo"
              :page-size="glueStockPagination.pageSize"
              :page-size-options="['10', '20', '50']"
              :show-total="(total) => `共 ${total} 条`"
              :total="glueStockPagination.total"
              show-less-items
              show-size-changer
              size="small"
              @change="handleGlueStockPaginationChange"
              @show-size-change="handleGlueStockPaginationChange"
            />
          </div>
        </div>
      </AModal>

      <AModal v-model:open="coaInspectionVisible" :footer="null" :width="760" class="rough-prototype-modal" title="COA送检">
        <div class="record-confirm-panel">
          <div class="record-confirm-summary">
            <span>计划号</span><strong>{{ currentPlan.planNo || '-' }}</strong> <span>产品型号</span><strong>{{ currentProductModelCode || '-' }}</strong> <span>胶板型号</span><strong>{{ getActualGlueBoardModel() || '-' }}</strong>
            <span>胶板料号</span><strong>{{ getActualGlueBoardMaterialCode() || '-' }}</strong> <span>COA送检片号</span><strong>{{ selectedCoaSliceDisplay || '-' }}</strong>
          </div>
          <div class="record-confirm-scan">
            <IconifyIcon icon="lucide:scan-line" />
            <Input v-model:value="coaInspectionScanForm.scannedSliceNo" allow-clear placeholder="请扫描或输入当前要送检的粘胶2成品片号" @press-enter="handleCoaInspectionScan" />
            <Button size="small" type="primary" @click="handleCoaInspectionScan">带入片号</Button>
          </div>
          <div v-if="coaInspectionScanForm.scanError" class="record-scan-confirm__error">
            {{ coaInspectionScanForm.scanError }}
          </div>
          <div v-if="coaInspectionScanForm.scanMessage" class="record-scan-confirm__message">
            {{ coaInspectionScanForm.scanMessage }}
          </div>
          <div class="console-table-body">
            <ATable
              class="rough-check-table console-record-table"
              :columns="coaInspectionColumns"
              :data-source="coaInspectionRows"
              :pagination="false"
              :scroll="{ x: 680, y: 260 }"
              row-key="key"
              size="small"
              :custom-row="
                (record) => ({
                  onDblclick: () => selectCoaInspectionSlice(record.batchNo),
                })
              "
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.dataIndex === 'reportStatus'">
                  <Tag :color="getRecordStatusMeta(record.reportStatus).color">
                    {{ getRecordStatusMeta(record.reportStatus).text }}
                  </Tag>
                </template>
                <template v-if="column.dataIndex === 'confirmTime'">
                  {{ displayDateTimeText(record.confirmTime) }}
                </template>
                <template v-if="column.dataIndex === 'action'">
                  <Radio :checked="selectedCoaSliceNo === record.batchNo" @change="selectCoaInspectionSlice(record.batchNo)" />
                </template>
              </template>
            </ATable>
          </div>
          <div v-if="!coaInspectionRows.length" class="record-scan-confirm__error">{{ coaInspectionEmptyMessage }}</div>
          <div v-if="coaInspectionBlockedRows.length" class="record-scan-confirm__error">
            <div>以下成品已保存报工，但因质量异常或锁定不能 COA 送检：</div>
            <div style="max-height: 160px; overflow-y: auto">
              <div v-for="record in coaInspectionBlockedRows" :key="record.key">{{ record.batchNo }}：{{ record.blockedReason }}</div>
            </div>
          </div>
          <div class="modal-footer">
            <Button @click="coaInspectionVisible = false">取消</Button>
            <Button type="primary" :disabled="isCurrentTaskReadonly || (!selectedCoaSliceNo && !coaInspectionScanForm.scannedSliceNo)" :loading="coaInspectionApplying" @click="submitCoaInspection"> 确认并生成送检记录 </Button>
          </div>
        </div>
      </AModal>

      <AModal v-model:open="glueBoardFaiVisible" :footer="null" :width="720" class="rough-prototype-modal" title="胶板送检">
        <div class="glue-consume-form">
          <div class="pp-form-grid rough-material-scan-grid">
            <div class="pp-form-item">
              <label>胶板型号</label>
              <div class="pp-readonly-box">
                {{ getActualGlueBoardModel() || '-' }}
              </div>
            </div>
            <div class="pp-form-item">
              <label>胶板批号</label>
              <div class="pp-readonly-box">{{ glueBoard.batchNo || '-' }}</div>
            </div>
            <div class="pp-form-item">
              <label>当前可用</label>
              <div class="pp-readonly-box">{{ glueBoardAvailableText }}</div>
            </div>
            <div class="pp-form-item"><label>取样起点(m)</label><InputNumber v-model:value="glueBoardFaiForm.sampleStartPosition" class="w-full" :min="0" :precision="3" /></div>
            <div class="pp-form-item"><label>送检长度(m)</label><InputNumber v-model:value="glueBoardFaiForm.sampleLength" class="w-full" :min="0" :max="getGlueBoardFaiMaxSampleLength() || undefined" :precision="3" /></div>
            <div class="pp-form-item">
              <label>FAI状态</label>
              <div class="pp-readonly-box">
                {{ glueBoardAqcStatusMeta.stampText }} /
                {{ glueBoardAqcStatusMeta.subText }}
              </div>
            </div>
          </div>
          <div class="glue-board-message">本次送检会创建真实 QMS 胶板送检记录，并同步回写胶板边库最近检验信息；今日已送检且非 NG 时不能重复提交。</div>
          <div class="modal-footer">
            <Button @click="glueBoardFaiVisible = false">关闭</Button>
            <Button type="primary" :loading="firstInspectionApplying" @click="submitFirstInspection">提交送检</Button>
          </div>
        </div>
      </AModal>

      <AModal v-model:open="aqcVisible" :footer="null" :width="720" class="rough-prototype-modal" title="胶板粘胶检验">
        <div class="glue-consume-form">
          <div class="pp-form-grid rough-material-scan-grid">
            <div class="pp-form-item">
              <label>胶板型号</label>
              <div class="pp-readonly-box">
                {{ getActualGlueBoardModel() || '-' }}
              </div>
            </div>
            <div class="pp-form-item">
              <label>胶板批号</label>
              <div class="pp-readonly-box">{{ glueBoard.batchNo || '-' }}</div>
            </div>
            <div class="pp-form-item">
              <label>边库余量</label>
              <div class="pp-readonly-box">{{ glueBoardAvailableText }}</div>
            </div>
            <div class="pp-form-item"><label>送检长度(m)</label><InputNumber v-model:value="aqcForm.sampleLength" class="w-full" :min="0" :precision="3" /></div>
          </div>
          <div class="modal-footer">
            <Button @click="aqcVisible = false">关闭</Button>
            <Button type="primary" :loading="aqcSubmitting" @click="submitGlueBoardAqc">提交送检</Button>
          </div>
        </div>
      </AModal>

      <AModal v-model:open="glueLossVisible" :footer="null" :width="720" class="rough-prototype-modal" title="胶板损耗报备">
        <div class="glue-consume-form">
          <div class="pp-form-grid rough-material-scan-grid">
            <div class="pp-form-item"><label>损耗长度(m)</label><InputNumber v-model:value="glueLossForm.lossLength" class="w-full" :min="0" :precision="3" /></div>
            <div class="pp-form-item pp-form-item--span-2"><label>损耗原因</label><Input v-model:value="glueLossForm.lossReason" placeholder="请输入损耗原因" /></div>
          </div>
          <div class="modal-footer">
            <Button @click="glueLossVisible = false">取消</Button>
            <Button type="primary" @click="submitGlueBoardLoss">确认保存</Button>
          </div>
        </div>
      </AModal>

      <AModal v-model:open="changeoverVisible" :footer="null" :title="null" width="100vw" wrap-class-name="adhesive2-changeover-sheet-modal" @cancel="changeoverVisible = false">
        <div class="adhesive2-changeover-sheet">
          <div class="adhesive2-changeover-sheet__header">
            <strong>粘胶2工艺参数点检</strong>
            <div class="adhesive2-changeover-sheet__actions">
              <Button size="small" :loading="changeoverSubmitting" @click="exportChangeoverInspectionExcel">导出Excel</Button>
              <Button size="small" :disabled="changeoverRecordLocked" :loading="changeoverImporting" @click="triggerChangeoverImport">导入Excel</Button>
              <Button size="small" :disabled="changeoverSubmitting || !changeoverForm.id" @click="printChangeoverInspection(changeoverForm)">打印记录单</Button>
              <Button size="small" type="primary" :disabled="changeoverRecordLocked" :loading="changeoverSubmitting" @click="openChangeoverSaveAuth">保存</Button>
              <Button size="small" type="primary" :disabled="changeoverRecordLocked" :loading="changeoverConfirming" @click="openChangeoverConfirmAuth">确认</Button>
              <Button size="small" @click="changeoverVisible = false">关闭</Button>
            </div>
          </div>
          <div class="adhesive2-changeover-sheet__info">
            <label>计划号</label><strong>{{ changeoverForm.currentPlanNo || currentPlan.planNo || '-' }}</strong> <label>当前型号</label><strong>{{ changeoverForm.productionModelCode || currentProductModelCode || '-' }}</strong>
            <label>当前料号</label><strong>{{ changeoverForm.productionMaterialCode || currentProductMaterialCode || '-' }}</strong> <label>记录时间</label
            ><strong>{{ displayDateTimeText(changeoverForm.recordTime || changeoverForm.submitTime) }}</strong> <label>分段批号</label><strong>{{ changeoverForm.motherSegmentBatchNo || currentMotherBatchNo || '-' }}</strong>
            <label>检验人</label><strong>{{ changeoverForm.recorderName || '-' }}</strong> <label>确认人</label><strong>{{ changeoverConfirmUserName || '-' }}</strong> <label>确认时间</label
            ><strong>{{ displayDateTimeText(changeoverConfirmTime) }}</strong> <label>备注</label
            ><Input v-model:value="changeoverForm.remark" class="adhesive2-changeover-sheet__remark" :disabled="changeoverRecordLocked" placeholder="请输入备注" />
          </div>
          <div v-if="changeoverImportAttachment" class="adhesive2-changeover-sheet__attachment">
            <span class="adhesive2-changeover-sheet__attachment-title">最后导入Excel</span>
            <button class="press-slot-middle-attachment__link" type="button" @click="openAdhesive2ImportAttachment(changeoverImportAttachment)">
              <IconifyIcon icon="lucide:paperclip" />
              <span>{{ changeoverImportAttachment.name || '原始导入文件' }}</span>
            </button>
            <span>导入时间：{{ changeoverImportAttachment.uploadTime || '-' }}</span>
            <span v-if="formatAdhesive2AttachmentSize(changeoverImportAttachment.size)"> 大小：{{ formatAdhesive2AttachmentSize(changeoverImportAttachment.size) }} </span>
          </div>
          <div class="adhesive2-changeover-sheet__table">
            <ATable
              class="rough-check-table adhesive2-changeover-table"
              :columns="reportProcessColumns"
              :data-source="changeoverForm.checkItems || []"
              :pagination="false"
              :scroll="changeoverCheckTableScroll"
              row-key="itemName"
              size="small"
            >
              <template #bodyCell="{ column, record, index }">
                <template v-if="column.dataIndex === 'actualValue'">
                  <Input
                    v-model:value="record.actualValue"
                    class="adhesive-check-cell"
                    :data-report-check-field="'actualValue'"
                    :disabled="changeoverRecordLocked"
                    placeholder="填写实际值"
                    @keydown="handleReportCheckKeydown($event, index, 'actualValue')"
                  />
                </template>
                <template v-if="column.dataIndex === 'abnormalRemark'">
                  <Input
                    v-model:value="record.abnormalRemark"
                    class="adhesive-check-cell"
                    :data-report-check-field="'abnormalRemark'"
                    :disabled="changeoverRecordLocked"
                    placeholder="填写异常备注"
                    @keydown="handleReportCheckKeydown($event, index, 'abnormalRemark')"
                  />
                </template>
              </template>
            </ATable>
          </div>
        </div>
      </AModal>

      <AuthModal
        v-model:visible="changeoverInstructionIssueAuthVisible"
        actionName="下达粘胶2换型指令"
        authMode="username"
        :workstation="selectedBoardEquipmentCode || selectedBoardEquipmentName || '粘胶2工位'"
        @cancel="handleChangeoverInstructionIssueAuthCancel"
        @success="handleChangeoverInstructionIssueAuthSuccess"
      />
      <AuthModal
        v-model:visible="changeoverInstructionExecuteAuthVisible"
        actionName="执行粘胶2换型指令"
        authMode="username"
        :workstation="selectedBoardEquipmentCode || selectedBoardEquipmentName || '粘胶2工位'"
        @cancel="handleChangeoverInstructionExecuteAuthCancel"
        @success="handleChangeoverInstructionExecuteAuthSuccess"
      />
      <AuthModal
        v-model:visible="dailyRecordAuthVisible"
        :actionName="dailyRecordAuthAction"
        authMode="username"
        :equipment-id="selectedBoardEquipmentId"
        :workstation="selectedBoardEquipmentCode || selectedBoardEquipmentName || '粘胶2工位'"
        @cancel="handleDailyRecordAuthCancel"
        @success="handleDailyRecordAuthSuccess"
      />
      <AuthModal
        v-model:visible="intermediateAuthVisible"
        :actionName="intermediateAuthAction"
        authMode="username"
        :workstation="selectedBoardEquipmentCode || selectedBoardEquipmentName || '粘胶2工位'"
        @cancel="handleIntermediateAuthCancel"
        @success="handleIntermediateAuthSuccess"
      />
      <AuthModal
        v-model:visible="changeoverSaveAuthVisible"
        :actionName="changeoverSaveAuthAction"
        authMode="username"
        :workstation="selectedBoardEquipmentCode || selectedBoardEquipmentName || '粘胶2工位'"
        @cancel="handleChangeoverSaveAuthCancel"
        @success="handleChangeoverSaveAuthSuccess"
      />
      <AuthModal
        v-model:visible="changeoverConfirmAuthVisible"
        :actionName="changeoverConfirmAuthAction"
        authMode="username"
        :workstation="selectedBoardEquipmentCode || selectedBoardEquipmentName || '粘胶2工位'"
        @cancel="handleChangeoverConfirmAuthCancel"
        @success="handleChangeoverConfirmAuthSuccess"
      />

      <AuthModal
        v-model:visible="segmentCompleteAuthVisible"
        :actionName="segmentCompleteAuthActionName"
        authMode="username"
        title="确认本段是否确认完工？"
        @cancel="handleSegmentCompleteAuthCancel"
        @success="handleSegmentCompleteAuthSuccess"
      />

      <AModal v-model:open="recordConfirmVisible" :footer="null" :keyboard="false" :mask-closable="false" :width="900" class="rough-prototype-modal" destroy-on-close title="粘胶2报工扫码确认">
        <div class="record-confirm-panel">
          <div class="record-confirm-summary">
            <span>当前计划</span><strong>{{ currentPlan.planNo || '-' }}</strong> <span>分段批号</span><strong>{{ currentMotherBatchNo || '-' }}</strong> <span>当前胶板尺寸</span
            ><strong>{{ currentPlan.actualSizeRule || currentPlan.planSizeSpec || '-' }}</strong> <span>胶板型号</span><strong>{{ getSelectedGlueBoardModel() || '-' }}</strong>
          </div>
          <div class="record-confirm-basis">
            {{ recordConfirmTypeBasis }}
          </div>
          <div class="record-confirm-scan">
            <IconifyIcon icon="lucide:scan-line" />
            <Input
              ref="recordConfirmScanInputRef"
              v-model:value="recordConfirmForm.scannedBatchNo"
              allow-clear
              autofocus
              data-adhesive-confirm-scan
              :disabled="recordConfirmProcessing"
              placeholder="请扫描流转单上的粘胶2片号，也可人工输入"
              @press-enter="confirmRecordScan"
            />
          </div>
          <div v-if="recordConfirmForm.error" class="record-scan-confirm__error">
            {{ recordConfirmForm.error }}
          </div>
          <div v-if="recordConfirmForm.message" class="record-scan-confirm__message">
            {{ recordConfirmForm.message }}
          </div>
          <div class="modal-footer">
            <Button :disabled="recordConfirmProcessing" @click="recordConfirmVisible = false">取消</Button>
            <Button type="primary" :disabled="isCurrentTaskReadonly" :loading="recordConfirmProcessing" @click="confirmRecordScan">确认扫码</Button>
          </div>
        </div>
      </AModal>

      <AModal :open="reportVisible" :footer="null" :title="null" width="100vw" class="rough-prototype-modal" destroy-on-close wrap-class-name="hc-pass-work-modal rough-report-work-modal" @cancel="requestCloseReport">
        <div class="report-modal-body">
          <div v-if="activeReportReadonlyReason" class="report-readonly-reason">
            {{ activeReportReadonlyReason }}
          </div>
          <fieldset class="erp-fieldset report-modal-toolbar">
            <legend>{{ reportDialogTitle }}</legend>
            <div class="press-slot-report-head press-slot-report-head--adhesive2">
              <div v-if="reportDialogMode === 'view'" class="press-slot-report-head__item press-slot-report-head__item--type">
                <label>作业类型</label>
                <div class="press-slot-report-head__value">
                  <strong class="press-slot-report-type-text">
                    {{ getAdhesive2ReportTypeText(reportForm.reportType) }}
                  </strong>
                </div>
              </div>
              <div class="press-slot-report-head__item">
                <label>分段批号</label>
                <strong class="press-slot-report-head__value press-slot-report-head__value--batch">{{
                  reportForm.parentBatchNo ||
                  resolveMotherBatchNo({
                    sourceProductionBatchNo: reportForm.sourceProductionBatchNo,
                  }) ||
                  '-'
                }}</strong>
              </div>
              <div class="press-slot-report-head__item">
                <label>粘胶2片号</label>
                <strong class="press-slot-report-head__value press-slot-report-head__value--batch">{{ reportForm.productionBatchNo || '-' }}</strong>
              </div>
              <div class="press-slot-report-head__item">
                <label>产品型号</label>
                <strong class="press-slot-report-head__value">{{ getReportFormProductModelCode() || '-' }}</strong>
              </div>
              <div class="press-slot-report-head__item">
                <label>胶板型号</label>
                <strong class="press-slot-report-head__value">{{ reportForm.glueBoardModel || '-' }}</strong>
              </div>
              <div class="press-slot-report-head__item">
                <label>COA送检</label>
                <strong
                  class="press-slot-report-head__value"
                  :class="{
                    warning: getAdhesive2SegmentCoaFlag({
                      batchNo: reportForm.sourceProductionBatchNo,
                    } as SourceSegment),
                  }"
                >
                  {{
                    getAdhesive2SegmentCoaFlag({
                      batchNo: reportForm.sourceProductionBatchNo,
                    } as SourceSegment)
                      ? '已选为COA送检片'
                      : '未送检'
                  }}
                </strong>
              </div>
              <div v-if="isAdhesive2InspectionReportType(reportForm.reportType)" class="press-slot-report-head__item press-slot-report-head__item--full">
                <label>检验信息</label>
                <strong class="press-slot-report-head__value"> 记录时间：{{ displayDateTimeText(reportInspectionInfo.submitTime) }} / 反馈时间：{{ displayDateTimeText(reportInspectionInfo.feedbackTime) }} </strong>
              </div>
            </div>
          </fieldset>
          <Tabs v-model:active-key="activeReportTab" class="rough-report-tabs">
            <TabPane v-if="reportDialogMode === 'confirm' && reportStep === 'SCAN'" key="scan" tab="来源扫码">
              <div class="rough-material-scan-row">
                <div class="pp-form-item rough-material-scan-row__field">
                  <label>来源片号（扫码/输入）</label>
                  <Input v-model:value="reportForm.sourceCode" placeholder="扫描压槽已确认片号" @press-enter="scanReportSource" />
                </div>
                <Button size="small" type="primary" @click="scanReportSource">扫码带出</Button>
              </div>
              <div class="rough-material-scan-hint">先扫码确认来源；中间品先填写中间品记录再进入外观检验，成品加工和过程加检直接进入外观检验。</div>
              <div v-if="reportForm.sourceScanError" class="record-scan-confirm__error">
                {{ reportForm.sourceScanError }}
              </div>
              <div v-if="reportForm.sourceScanMessage" class="record-scan-confirm__message">
                {{ reportForm.sourceScanMessage }}
              </div>
              <div class="pp-form-grid rough-material-scan-grid">
                <div class="pp-form-item"><label>来源片号</label><Input v-model:value="reportForm.sourceProductionBatchNo" disabled /></div>
                <div class="pp-form-item"><label>来源母批</label><Input v-model:value="reportForm.parentBatchNo" disabled /></div>
                <div class="pp-form-item"><label>粘胶2片号</label><Input v-model:value="reportForm.productionBatchNo" disabled /></div>
                <div class="pp-form-item"><label>压槽报工ID</label><Input :value="reportForm.sourceGrindingSecondDetailId || '-'" disabled /></div>
              </div>
            </TabPane>
            <template v-if="reportDialogMode === 'view' || reportStep === 'PROCESS'">
              <TabPane v-for="category in reportCheckCategories" :key="`check-${category}`" :tab="category">
                <div class="report-tab-stack">
                  <ATable class="rough-check-table" :columns="reportProcessColumns" :data-source="getCheckItemsByCategory(category)" :pagination="false" row-key="itemName" size="small">
                    <template #bodyCell="{ column, record }">
                      <template v-if="column.dataIndex === 'actualValue'">
                        <Input
                          v-model:value="record.actualValue"
                          class="adhesive-check-cell"
                          :data-report-check-field="'actualValue'"
                          :disabled="reportDialogMode === 'view'"
                          placeholder="填写实际值"
                          @keydown="
                            handleReportCheckKeydown(
                              $event,
                              getCheckItemsByCategory(category).findIndex((item) => item.itemName === record.itemName),
                              'actualValue',
                            )
                          "
                        />
                      </template>
                      <template v-if="column.dataIndex === 'abnormalRemark'">
                        <Input
                          v-model:value="record.abnormalRemark"
                          class="adhesive-check-cell"
                          :data-report-check-field="'abnormalRemark'"
                          :disabled="reportDialogMode === 'view'"
                          placeholder="填写异常备注"
                          @keydown="
                            handleReportCheckKeydown(
                              $event,
                              getCheckItemsByCategory(category).findIndex((item) => item.itemName === record.itemName),
                              'abnormalRemark',
                            )
                          "
                        />
                      </template>
                    </template>
                  </ATable>
                </div>
              </TabPane>
            </template>
            <TabPane v-if="shouldShowVisualInspectionTab && (reportDialogMode === 'view' || reportStep === 'PROCESS')" key="visual-inspection" tab="外观检验">
              <div class="report-tab-stack press-slot-visual-tab">
                <div class="visual-check-grid">
                  <div v-for="item in visualInspectionItems" :key="item.itemName" :class="getVisualCheckItemClass(item)">
                    <button class="visual-light-button" type="button" :disabled="!isVisualInspectionEditable" @click="toggleVisualItem(item)">
                      <span class="visual-light-dot" :class="{ 'is-active': isVisualItemActive(item) }"></span>
                      <span>{{ item.itemName }}</span>
                    </button>
                    <Input v-if="isVisualItemActive(item)" v-model:value="item.remark" allow-clear class="visual-item-remark" :disabled="!isVisualInspectionEditable" placeholder="缺陷说明" />
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
                  <div v-if="normalizeVisualResult(reportForm.selfCheck) === 'NG' || hasActiveVisualItems" class="press-slot-visual-field">
                    <label>异常归属</label>
                    <div class="press-slot-visual-field__control">
                      <Checkbox v-model:checked="reportForm.preProcessSelfCheckAbnormal" :disabled="!isVisualInspectionEditable">
                        归属压槽（加工前自检异常，非换型）
                      </Checkbox>
                      <span v-if="reportForm.preProcessSelfCheckAbnormal" class="visual-check-lock">
                        保留本次外观 NG、不良代码和备注；仅变更异常归属，不变更型号、换型状态或工艺参数。
                      </span>
                    </div>
                  </div>
                  <div class="press-slot-visual-field press-slot-visual-field--remark">
                    <label>备注</label>
                    <Input.TextArea v-model:value="reportForm.remark" allow-clear :disabled="!isVisualInspectionEditable" :rows="4" placeholder="填写外观检验备注" />
                  </div>
                </div>
              </div>
            </TabPane>
            <TabPane v-if="reportDialogMode === 'confirm' && reportStep === 'REPORT'" key="report" tab="报工收卷">
              <div class="report-tab-stack report-tab-stack--report">
                <div class="report-tab-scroll-body">
                  <fieldset class="erp-fieldset">
                    <legend>报工信息</legend>
                    <Form layout="vertical" class="report-form-grid">
                      <FormItem label="来源片号"><Input v-model:value="reportForm.sourceProductionBatchNo" disabled /></FormItem>
                      <FormItem label="粘胶2片号"><Input v-model:value="reportForm.productionBatchNo" disabled /></FormItem>
                      <FormItem label="起始位置(m)" class="report-required-field"><InputNumber v-model:value="reportForm.startPosition" :disabled="isConfirmedReportOverwrite" :min="0" :precision="3" class="full-input" /></FormItem>
                      <FormItem label="结束位置(m)" class="report-required-field"><InputNumber v-model:value="reportForm.endPosition" :disabled="isConfirmedReportOverwrite" :min="0" :precision="3" class="full-input" /></FormItem>
                      <FormItem label="加工片数" class="report-required-field"><InputNumber :value="reportForm.processLength" disabled :precision="0" class="full-input report-calculated-input" /></FormItem>
                      <FormItem label="损耗片数"><InputNumber v-model:value="reportForm.lossLength" :disabled="isConfirmedReportOverwrite" :min="0" :precision="0" class="full-input" /></FormItem>
                      <FormItem label="留样片数"><InputNumber v-model:value="reportForm.napSampleLength" :disabled="isConfirmedReportOverwrite" :min="0" :precision="0" class="full-input" /></FormItem>
                      <FormItem label="合格片数"><InputNumber :value="reportOutputLength" disabled class="full-input" /></FormItem>
                      <FormItem label="胶板型号"><Input :value="getSelectedGlueBoardModel()" disabled /></FormItem>
                      <FormItem label="胶板批号"><Input v-model:value="reportForm.glueBoardBatchNo" :disabled="isConfirmedReportOverwrite" /></FormItem>
                      <FormItem label="生产日期"><DatePicker v-model:value="reportForm.reportDate" :disabled="isConfirmedReportOverwrite" value-format="YYYY-MM-DD" class="full-input" /></FormItem>
                      <FormItem label="开始时间"><DatePicker v-model:value="reportForm.startTime" :disabled="isConfirmedReportOverwrite" show-time value-format="YYYY-MM-DD HH:mm:ss" class="full-input" /></FormItem>
                      <FormItem label="结束时间"><DatePicker v-model:value="reportForm.endTime" :disabled="isConfirmedReportOverwrite" show-time value-format="YYYY-MM-DD HH:mm:ss" class="full-input" /></FormItem>
                      <FormItem label="自检">
                        <RadioGroup v-model:value="reportForm.selfCheck"><Radio value="OK">OK</Radio><Radio value="NG">NG</Radio></RadioGroup>
                      </FormItem>
                      <FormItem label="不良代码"><Select v-model:value="reportForm.defectCode" :options="defectOptions" class="full-input" /></FormItem>
                      <FormItem label="备注" class="report-remark"><Input v-model:value="reportForm.remark" /></FormItem>
                    </Form>
                    <div class="report-output-hint">合格片数 = 加工片数 - 损耗片数 - 留样片数</div>
                  </fieldset>
                  <div class="second-range-visual">
                    <div class="second-range-visual__head">
                      <strong>粘胶2片号占用</strong>
                      <span>当前区间：{{ reportRangeText }}</span>
                      <span>来源总数：{{ formatNumber(reportRangeTotal) }} 片</span>
                      <span>可加工：{{ formatNumber(getAdhesive2SegmentAvailableLength(findSourceSegment(reportForm.sourceProductionBatchNo))) }} 片</span>
                      <em v-if="hasReportRangeOverlap"> 与已报区间重叠：{{ formatNumber(reportOverlapRanges[0]?.start) }}-{{ formatNumber(reportOverlapRanges[0]?.end) }} m </em>
                    </div>
                    <div class="second-range-bar">
                      <span class="second-range-bar__empty">未报区间</span>
                      <span
                        v-for="segment in reportRangeSegments"
                        :key="segment.key"
                        class="second-range-segment"
                        :class="`second-range-segment--${segment.type}`"
                        :style="{
                          left: `${segment.left}%`,
                          width: `${segment.width}%`,
                        }"
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
          <Button v-if="reportDialogMode === 'view' && activeRecord?.id && isAdhesiveRecordConfirmed(activeRecord.reportStatus)" :loading="middleTypeSetting" @click="setActiveReportMiddleType('FRONT')"> 设为中间品-前段 </Button>
          <Button v-if="reportDialogMode === 'view' && activeRecord?.id && isAdhesiveRecordConfirmed(activeRecord.reportStatus)" :loading="middleTypeSetting" @click="setActiveReportMiddleType('MIDDLE')"> 设为中间品-中段 </Button>
          <Button v-if="reportDialogMode === 'view' && activeRecord?.id && isAdhesiveRecordConfirmed(activeRecord.reportStatus)" :loading="middleTypeSetting" @click="setActiveReportMiddleType('END')"> 设为中间品-后段 </Button>
          <Button v-if="canCorrectAbnormalCategory" :loading="abnormalCategoryCorrectionSaving" @click="openAbnormalCategoryCorrection"> 修正异常类别 </Button>
          <Button :disabled="reportSubmitting || processCheckApplying" @click="requestCloseReport">{{ reportDialogMode === 'view' ? '关闭' : '取消' }}</Button>
          <Button v-if="reportDialogMode === 'confirm' && reportStep === 'SCAN'" type="primary" :disabled="isCurrentTaskReadonly || reportSubmitting || processCheckApplying" @click="startReportWork">{{ startReportWorkButtonText }}</Button>
          <Button v-if="reportDialogMode === 'confirm' && reportStep === 'PROCESS'" type="primary" :disabled="isCurrentTaskReadonly || processCheckApplying" :loading="reportSubmitting" @click="submitProcessItems">{{
            reportSubmitButtonText
          }}</Button>
          <Button v-if="reportDialogMode === 'confirm' && reportStep === 'REPORT'" type="primary" :disabled="isCurrentTaskReadonly" :loading="reportSubmitting" @click="submitAdhesiveReport()">提交报工收卷</Button>
        </div>
      </AModal>
      <AModal
        v-model:open="abnormalCategoryCorrectionVisible"
        centered
        destroy-on-close
        :confirm-loading="abnormalCategoryCorrectionSaving"
        :mask-closable="false"
        :z-index="5200"
        title="修正外观异常类别"
        width="560px"
        @cancel="closeAbnormalCategoryCorrection"
        @ok="submitAbnormalCategoryCorrection"
      >
        <Form layout="vertical">
          <FormItem label="粘胶2片号">
            <Input :value="activeRecord?.productionBatchNo || '-'" disabled />
          </FormItem>
          <FormItem label="当前异常类别">
            <Input :value="getActiveVisualCategoryText()" disabled />
          </FormItem>
          <FormItem label="修正后异常类别" required>
            <RadioGroup v-model:value="abnormalCategoryCorrectionForm.category" class="abnormal-category-correction-options">
              <RadioButton v-for="item in abnormalCategoryCorrectionOptions" :key="item.value" :value="item.value">
                {{ item.label }}
              </RadioButton>
            </RadioGroup>
          </FormItem>
          <FormItem label="修正原因" required>
            <Input.TextArea v-model:value="abnormalCategoryCorrectionForm.reason" allow-clear placeholder="请输入修正原因" :rows="3" />
          </FormItem>
        </Form>
      </AModal>
      <FaiDetailPreviewModal />
    </div>
  </Page>
</template>

<style scoped>
:deep(.vben-page),
:deep(.vben-page-content),
:deep(.vben-page-content-wrapper) {
  height: 100%;
  min-height: 0;
}

.adhesive-console {
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

.adhesive-console.is-visual-maximized {
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

.product-model-display {
  display: flex;
  align-items: center;
  min-width: 0;
  gap: 6px;
}

.product-model-display > span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-model-display :deep(.ant-btn-link) {
  flex: 0 0 auto;
  height: 22px;
  padding: 0 2px;
  font-weight: 800;
}

.model-change-origin {
  color: #b45309;
  font-size: 12px;
  font-style: normal;
  font-weight: 800;
  white-space: nowrap;
}

.model-change-summary-label {
  grid-column: 1 / 2;
}

.model-change-summary {
  display: block !important;
  grid-column: 2 / -1;
  min-width: 0;
  color: #92400e !important;
  font-weight: 900 !important;
  line-height: 18px !important;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap !important;
}

.model-change-form {
  display: grid;
  gap: 10px;
}

.model-change-form-row {
  display: grid;
  grid-template-columns: 118px minmax(0, 1fr);
  align-items: center;
  gap: 10px;
}

.model-change-form-row--wide {
  align-items: start;
}

.model-change-form-row > label {
  color: #334155;
  font-size: 13px;
  font-weight: 900;
  text-align: right;
}

.model-change-control,
.model-change-form-row :deep(.ant-input),
.model-change-form-row :deep(.ant-select) {
  width: 100%;
}

.changeover-instruction-summary {
  display: grid;
  grid-template-columns: repeat(4, max-content minmax(0, 1fr));
  gap: 8px 10px;
  padding: 10px 12px;
  margin-bottom: 12px;
  color: #0f172a;
  background: #f8fafc;
  border: 1px solid #dbeafe;
  border-radius: 6px;
}

.changeover-instruction-summary label {
  color: #64748b;
  font-size: 12px;
  font-weight: 900;
  white-space: nowrap;
}

.changeover-instruction-summary strong {
  min-width: 0;
  overflow: hidden;
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.console-action-group {
  gap: clamp(5px, 0.42vw, 8px) !important;
  padding-left: clamp(10px, 1vw, 20px) !important;
}

.console-action-group > div {
  width: clamp(54px, 3.4vw, 64px) !important;
  height: clamp(54px, 3.4vw, 64px) !important;
  border-radius: 6px !important;
}

.console-action-group > div :deep(.iconify),
.console-action-group > div :deep(svg) {
  font-size: clamp(18px, 1.2vw, 22px) !important;
}

.console-action-group > div span {
  font-size: clamp(10px, 0.68vw, 11px) !important;
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

.adhesive-stamp-group {
  flex-basis: clamp(278px, 18vw, 330px);
  gap: clamp(4px, 0.42vw, 8px);
}

.adhesive-stamp-group .inspection-stamp-side {
  width: clamp(126px, 8vw, 154px);
}

.adhesive-stamp-group .inspection-stamp-content em {
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

.adhesive-console :deep(.ant-btn-primary) {
  background: linear-gradient(180deg, #0ea5e9 0%, #0369a1 100%);
  border-color: #075985;
  border-radius: 2px;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.28);
}

.adhesive-console :deep(.ant-btn) {
  border-radius: 2px;
}

.adhesive-console :deep(.ant-btn[disabled]),
.adhesive-console :deep(.ant-btn.ant-btn-disabled) {
  color: #64748b !important;
  text-shadow: none !important;
  background: #e2e8f0 !important;
  border-color: #cbd5e1 !important;
  box-shadow: none !important;
  opacity: 1;
}

.adhesive-console :deep(.ant-btn[disabled] *),
.adhesive-console :deep(.ant-btn.ant-btn-disabled *) {
  color: #64748b !important;
}

.erp-card {
  min-height: 0;
  padding: 8px 10px;
  overflow: hidden;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.72), rgba(230, 236, 244, 0.76)), var(--industrial-card);
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

.erp-requirement-value {
  grid-column: span 3;
  min-width: 0;
}

.erp-requirement-value span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.erp-value-span-2 {
  grid-column: span 3 !important;
  width: 100%;
  min-width: 0;
}

.abnormal-category-correction-options {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
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
  color: #dc2626;
  font-weight: 900;
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
  grid-template-columns: repeat(5, 7fr 18fr);
  grid-template-rows: minmax(32px, auto);
  gap: 6px 8px;
  align-items: stretch;
  min-height: auto;
}

.glue-board-field {
  display: contents;
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

.adhesive-workbench-blocked-mask {
  position: absolute;
  inset: 0;
  z-index: 20;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding: 44px 24px 24px;
  background: rgba(241, 245, 249, 0.28);
}

.adhesive-workbench-blocked-mask__content {
  display: flex;
  flex-direction: column;
  align-items: center;
  max-width: min(560px, 84%);
  padding: 12px 24px;
  color: #991b1b;
  text-align: center;
  background: rgba(255, 241, 242, 0.82);
  border: 1px solid #fecdd3;
  border-radius: 6px;
  box-shadow: 0 8px 24px rgba(148, 27, 27, 0.12);
}

.adhesive-workbench-blocked-mask__content :deep(.iconify),
.adhesive-workbench-blocked-mask__content :deep(svg) {
  margin-bottom: 4px;
  font-size: 22px;
}

.adhesive-workbench-blocked-mask__content strong {
  font-size: 16px;
  font-weight: 950;
}

.adhesive-workbench-blocked-mask__content span {
  margin-top: 6px;
  font-size: 13px;
  font-weight: 800;
  line-height: 1.7;
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
    radial-gradient(circle at 18% 14%, rgb(255 255 255 / 18%), transparent 18%), repeating-linear-gradient(0deg, rgb(255 255 255 / 8%) 0 1px, transparent 1px 10px),
    repeating-linear-gradient(90deg, rgb(15 76 117 / 10%) 0 1px, transparent 1px 10px), #8eb7d3;
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
    radial-gradient(circle at 18% 14%, rgb(255 255 255 / 18%), transparent 18%), repeating-linear-gradient(0deg, rgb(255 255 255 / 8%) 0 1px, transparent 1px 10px),
    repeating-linear-gradient(90deg, rgb(15 76 117 / 10%) 0 1px, transparent 1px 10px), linear-gradient(135deg, #a6c8dc 0%, #79a8c4 100%);
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
    linear-gradient(135deg, rgb(255 255 255 / 46%) 0%, rgb(255 255 255 / 10%) 36%, transparent 37%), repeating-linear-gradient(45deg, rgb(255 255 255 / 16%) 0 1px, transparent 1px 7px),
    repeating-linear-gradient(-45deg, rgb(19 72 105 / 10%) 0 1px, transparent 1px 7px), #d9ebf4;
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
  gap: 2px;
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
    linear-gradient(135deg, rgb(255 255 255 / 42%) 0%, rgb(255 255 255 / 12%) 34%, transparent 35%), repeating-linear-gradient(45deg, rgb(255 255 255 / 16%) 0 1px, transparent 1px 7px),
    repeating-linear-gradient(-45deg, rgb(146 64 14 / 10%) 0 1px, transparent 1px 7px), #ffd6a3;
  border-color: rgb(194 110 28 / 72%);
}

.slice-cell.is-scan-pending {
  color: #0f2f5f;
  background:
    linear-gradient(135deg, rgb(255 255 255 / 46%) 0%, rgb(255 255 255 / 10%) 34%, transparent 35%), repeating-linear-gradient(45deg, rgb(255 255 255 / 16%) 0 1px, transparent 1px 7px),
    repeating-linear-gradient(-45deg, rgb(37 99 235 / 10%) 0 1px, transparent 1px 7px), #c7dcff;
  border-color: rgb(37 99 235 / 76%);
  box-shadow:
    inset 0 0 0 2px rgb(37 99 235 / 28%),
    inset 0 -12px 20px rgb(30 64 175 / 12%);
}

.slice-cell.is-confirmed {
  color: #14532d;
  cursor: not-allowed;
  background:
    linear-gradient(135deg, rgb(255 255 255 / 46%) 0%, rgb(255 255 255 / 10%) 36%, transparent 37%), repeating-linear-gradient(45deg, rgb(255 255 255 / 18%) 0 1px, transparent 1px 7px),
    repeating-linear-gradient(-45deg, rgb(21 128 61 / 10%) 0 1px, transparent 1px 7px), #a9efbf;
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

.slice-cell.is-partial {
  color: #92400e;
  background:
    linear-gradient(135deg, rgb(255 255 255 / 44%) 0%, rgb(255 255 255 / 12%) 34%, transparent 35%), repeating-linear-gradient(45deg, rgb(255 255 255 / 16%) 0 1px, transparent 1px 7px),
    repeating-linear-gradient(-45deg, rgb(180 83 9 / 11%) 0 1px, transparent 1px 7px), #fef3c7;
  border-color: rgb(180 83 9 / 72%);
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
  font-weight: 950;
  line-height: 13px;
  overflow-wrap: anywhere;
  text-align: center;
  background: #b91c1c;
  border: 1px solid rgb(255 255 255 / 74%);
  border-radius: 1px;
  box-shadow: none;
}

.slice-cell__ng.is-current-ng {
  background: #dc2626;
}

.slice-cell__ng.is-source-ng {
  background: #7f1d1d;
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
  background: linear-gradient(135deg, rgb(255 255 255 / 38%) 0%, rgb(255 255 255 / 10%) 34%, transparent 35%), repeating-linear-gradient(45deg, rgb(255 255 255 / 16%) 0 1px, transparent 1px 7px), #fecaca;
  box-shadow:
    inset 0 0 0 3px rgb(220 38 38 / 42%),
    0 0 14px rgb(220 38 38 / 22%);
}

.slice-cell.is-current-ng {
  color: #7f1d1d;
  background:
    linear-gradient(135deg, rgb(255 255 255 / 40%) 0%, rgb(255 255 255 / 10%) 34%, transparent 35%), repeating-linear-gradient(45deg, rgb(255 255 255 / 15%) 0 1px, transparent 1px 7px),
    repeating-linear-gradient(-45deg, rgb(153 27 27 / 10%) 0 1px, transparent 1px 7px), #f7b4b4;
  border-color: rgb(185 28 28 / 76%);
  box-shadow:
    inset 0 0 0 3px rgb(220 38 38 / 36%),
    0 0 14px rgb(239 68 68 / 24%);
}

.slice-cell.is-source-ng {
  color: #7f1d1d;
  background:
    linear-gradient(135deg, rgb(255 255 255 / 42%) 0%, rgb(255 255 255 / 10%) 34%, transparent 35%), repeating-linear-gradient(45deg, rgb(255 255 255 / 14%) 0 1px, transparent 1px 7px),
    repeating-linear-gradient(-45deg, rgb(127 29 29 / 12%) 0 1px, transparent 1px 7px), #fecdd3;
  border-color: rgb(127 29 29 / 76%);
  box-shadow:
    inset 0 0 0 3px rgb(127 29 29 / 26%),
    0 0 14px rgb(190 18 60 / 20%);
}

.slice-cell.is-inspection-bg-ok {
  color: #14532d;
  background:
    linear-gradient(135deg, rgb(255 255 255 / 46%) 0%, rgb(255 255 255 / 10%) 36%, transparent 37%), repeating-linear-gradient(45deg, rgb(255 255 255 / 18%) 0 1px, transparent 1px 7px),
    repeating-linear-gradient(-45deg, rgb(21 128 61 / 10%) 0 1px, transparent 1px 7px), #a9efbf;
  border-color: rgb(22 101 52 / 76%);
  box-shadow:
    inset 0 0 0 3px rgb(22 163 74 / 30%),
    0 0 14px rgb(34 197 94 / 22%);
}

.slice-cell.is-inspection-bg-pending {
  color: #7c2d12;
  background:
    linear-gradient(135deg, rgb(255 255 255 / 44%) 0%, rgb(255 255 255 / 12%) 34%, transparent 35%), repeating-linear-gradient(45deg, rgb(255 255 255 / 18%) 0 1px, transparent 1px 7px),
    repeating-linear-gradient(-45deg, rgb(146 64 14 / 10%) 0 1px, transparent 1px 7px), #fde68a;
  border-color: rgb(217 119 6 / 78%);
  box-shadow:
    inset 0 0 0 3px rgb(217 119 6 / 34%),
    0 0 14px rgb(217 119 6 / 20%);
}

.slice-cell.is-inspection-bg-ng {
  color: #7f1d1d;
  background:
    linear-gradient(135deg, rgb(255 255 255 / 38%) 0%, rgb(255 255 255 / 10%) 34%, transparent 35%), repeating-linear-gradient(45deg, rgb(255 255 255 / 16%) 0 1px, transparent 1px 7px),
    repeating-linear-gradient(-45deg, rgb(153 27 27 / 12%) 0 1px, transparent 1px 7px), #fecaca;
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

.adhesive-position-strip {
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

.adhesive-position-strip i {
  position: absolute;
  top: 0;
  bottom: 0;
  display: block;
  background: repeating-linear-gradient(90deg, rgba(249, 115, 22, 0.98) 0 8px, rgba(234, 88, 12, 0.98) 8px 12px);
  border-right: 1px solid rgba(124, 45, 18, 0.72);
}

.adhesive-position-strip i.confirmed {
  background: repeating-linear-gradient(90deg, rgba(22, 163, 74, 0.96) 0 8px, rgba(21, 128, 61, 0.96) 8px 12px);
}

.adhesive-position-scale {
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
  grid-template-columns:
    minmax(150px, 1.1fr) minmax(150px, 1fr) minmax(150px, 1fr)
    minmax(126px, 0.72fr) auto;
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

.glue-stock-batch-filter {
  width: 180px;
}

.glue-stock-material-filter {
  width: 170px;
}

.glue-stock-model-filter {
  width: 150px;
}

.glue-board-map-filter-tip {
  display: flex;
  flex: 0 0 auto;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
  padding: 8px 10px;
  color: #0f766e;
  font-size: 12px;
  font-weight: 800;
  background: #f0fdfa;
  border-bottom: 1px solid rgba(13, 148, 136, 0.18);
}

.glue-board-map-filter-tip.warning {
  color: #b45309;
  background: #fffbeb;
  border-bottom-color: rgba(245, 158, 11, 0.24);
}

.glue-stock-pagination {
  justify-content: space-between;
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

.report-readonly-reason {
  flex: 0 0 auto;
  padding: 9px 12px;
  color: #991b1b;
  font-size: 13px;
  font-weight: 800;
  background: #fef2f2;
  border: 1px solid #fca5a5;
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

.press-slot-report-head--adhesive2 {
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

.adhesive-middle-modal {
  height: 100%;
}

.adhesive-middle-body {
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-height: 0;
}

.adhesive-middle-head {
  grid-template-columns: repeat(4, minmax(0, 1fr));
  padding: 0;
}

.adhesive-middle-panel {
  min-height: 0;
}

.adhesive-middle-detail-wrap {
  height: 100%;
  min-height: 0;
  overflow: auto;
}

.adhesive-middle-grid th:first-child,
.adhesive-middle-grid td:first-child {
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
  height: 100%;
  min-height: 0;
}

.press-slot-middle-shell {
  height: 100%;
  min-height: 0;
  background: linear-gradient(180deg, #f3f7fb 0%, #e6edf5 100%);
  border: 1px solid #8794a4;
}

.press-slot-middle-body {
  display: grid;
  flex: 1 1 0;
  grid-template-rows: max-content max-content minmax(280px, 1fr);
  gap: 8px;
  height: auto;
  min-height: 0;
  padding: 8px;
  overflow: hidden;
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
  height: 100%;
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
  grid-template-columns:
    112px minmax(0, 1fr) 112px minmax(0, 1fr) 112px minmax(0, 1fr)
    112px minmax(0, 1fr);
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
  min-height: 280px;
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
  background: repeating-linear-gradient(90deg, rgba(71, 85, 105, 0.15) 0 1px, transparent 1px 24px), linear-gradient(180deg, #e0f2fe 0%, #bfdbfe 100%);
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
  background: repeating-linear-gradient(45deg, rgba(255, 255, 255, 0.28) 0 6px, transparent 6px 12px), linear-gradient(180deg, #ef4444 0%, #b91c1c 100%);
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

.size-rule-section {
  display: grid;
  grid-template-columns: 120px minmax(0, 1fr);
  gap: 8px;
  align-items: center;
}

.size-rule-section > label {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  min-height: 38px;
  padding: 6px 8px;
  color: #334155;
  font-weight: 700;
  background: #e2e8f0;
  border: 1px solid #cbd5e1;
}

.size-rule-radio-group {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  min-height: 38px;
  padding: 4px;
  background: #f8fafc;
  border: 1px solid #cbd5e1;
}

.size-rule-radio-group :deep(.ant-radio-button-wrapper) {
  height: 30px;
  line-height: 28px;
  font-weight: 800;
  border-inline-start-width: 1px;
}

.tail-assign-list {
  display: flex;
  max-height: 360px;
  overflow: auto;
  flex-direction: column;
  border: 1px solid #cbd5e1;
}

.tail-assign-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 12px;
  align-items: center;
  min-height: 52px;
  padding: 8px 10px;
  background: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
}

.tail-assign-row:last-child {
  border-bottom: 0;
}

.tail-assign-row strong,
.tail-assign-row span {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tail-assign-row strong {
  color: #0f172a;
  font-family: Consolas, 'Courier New', monospace;
  font-weight: 800;
}

.tail-assign-row span {
  margin-top: 3px;
  color: #475569;
  font-size: 12px;
}

.size-rule-dot {
  display: inline-block;
  width: 10px;
  height: 10px;
  margin-right: 6px;
  vertical-align: -1px;
  background: var(--rule-color);
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

.rough-report-work-modal .report-modal-toolbar .rough-grid-toolbar__actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-bottom: 8px;
}

.adhesive2-changeover-sheet-modal .ant-modal {
  top: 0;
  width: 100vw !important;
  max-width: none;
  margin: 0 !important;
  padding-bottom: 0;
}

.adhesive2-changeover-sheet-modal .ant-modal-content {
  height: 100vh !important;
  padding: 0 !important;
  overflow: hidden;
  border-radius: 0 !important;
  box-shadow: none;
}

.adhesive2-changeover-sheet-modal .ant-modal-body {
  height: 100vh !important;
  padding: 0 !important;
  overflow: hidden !important;
  background: #f5f7fa;
}

.adhesive2-changeover-sheet-modal .ant-modal-header,
.adhesive2-changeover-sheet-modal .ant-modal-close {
  display: none !important;
}

.adhesive2-changeover-sheet {
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  gap: 8px;
  height: 100vh;
  min-height: 0 !important;
  padding: 8px;
  overflow: hidden;
  background:
    linear-gradient(90deg, rgba(30, 41, 59, 0.04) 1px, transparent 1px) 0 0 / 28px 28px,
    linear-gradient(0deg, rgba(30, 41, 59, 0.04) 1px, transparent 1px) 0 0 / 28px 28px,
    #f5f7fa;
}

.adhesive2-changeover-sheet__header {
  display: flex;
  flex: 0 0 40px;
  align-items: center;
  gap: 12px;
  min-width: 0;
  padding: 0 10px;
  background: linear-gradient(180deg, #f7fbff 0%, #e8f1fb 100%);
  border: 1px solid #9fb6cd;
}

.adhesive2-changeover-sheet__header > strong {
  flex: 1 1 auto;
  min-width: 0;
  overflow: hidden;
  color: #0f172a;
  font-size: 16px;
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.adhesive2-changeover-sheet__actions {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
}

.adhesive2-changeover-sheet__info {
  display: grid;
  flex: 0 0 auto;
  grid-template-columns:
    86px minmax(160px, 1fr) 86px minmax(160px, 1fr)
    86px minmax(160px, 1fr) 86px minmax(180px, 1.2fr);
  gap: 6px 8px;
  align-items: center;
  padding: 8px;
  background: rgb(248 250 252 / 82%);
  border: 1px solid #9fb6cd;
}

.adhesive2-changeover-sheet__info label {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  height: 30px;
  padding: 0 9px;
  color: #334155;
  font-weight: 800;
  background: #e2e8f0;
  border: 1px solid #cbd5e1;
}

.adhesive2-changeover-sheet__info strong {
  display: flex;
  align-items: center;
  min-width: 0;
  height: 30px;
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

.adhesive2-changeover-sheet__remark {
  grid-column: span 3;
  height: 30px;
}

.adhesive2-changeover-sheet__attachment {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 14px;
  min-height: 42px;
  padding: 0 12px;
  color: #475569;
  font-size: 13px;
  background: rgb(248 250 252 / 82%);
  border: 1px solid #9fb6cd;
}

.adhesive2-changeover-sheet__attachment-title {
  color: #075985;
  font-weight: 900;
}

.adhesive2-changeover-sheet__table {
  flex: 1 1 auto;
  min-height: 0;
  overflow: hidden;
}

.adhesive2-changeover-table,
.adhesive2-changeover-table :deep(.ant-spin-nested-loading),
.adhesive2-changeover-table :deep(.ant-spin-container),
.adhesive2-changeover-table :deep(.ant-table),
.adhesive2-changeover-table :deep(.ant-table-container) {
  height: 100%;
}

.adhesive2-changeover-table :deep(.ant-table-body) {
  overflow-y: auto !important;
}

.press-slot-middle-attachment__link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 0;
  color: #1677ff;
  font-weight: 700;
  cursor: pointer;
  background: transparent;
  border: 0;
}

.first-inspection-summary {
  padding: 12px;
}

.first-inspection-summary__grid {
  display: grid;
  grid-template-columns: 120px minmax(0, 1fr) 120px minmax(0, 1fr);
  gap: 8px;
}

.first-inspection-summary__grid span,
.first-inspection-summary__grid strong {
  min-height: 34px;
  padding: 7px 10px;
  border: 1px solid #a2adba;
}

.first-inspection-summary__grid span {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  color: #334155;
  font-size: 13px;
  font-weight: 700;
  background: #e2e8f0;
}

.first-inspection-summary__grid strong {
  display: flex;
  align-items: center;
  min-width: 0;
  overflow: hidden;
  color: #0f172a;
  font-size: 13px;
  font-weight: 700;
  background: #f8fafc;
}

.first-inspection-summary__value-link {
  max-width: 100%;
  padding: 0;
  overflow: hidden;
  color: #1677ff;
  font: inherit;
  font-weight: 800;
  line-height: 1.35;
  text-align: left;
  text-overflow: ellipsis;
  white-space: nowrap;
  cursor: pointer;
  background: transparent;
  border: 0;
}

.first-inspection-summary__value-link:hover {
  color: #0958d9;
}

.first-inspection-summary__hint {
  margin-top: 10px;
  padding: 8px 10px;
  color: #92400e;
  font-size: 13px;
  font-weight: 700;
  background: #fffbeb;
  border: 1px solid #f59e0b;
}

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
  grid-template-columns:
    92px minmax(0, 1fr) 92px minmax(0, 1fr) 92px minmax(0, 1fr)
    92px minmax(0, 1fr);
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
  grid-column: span 3;
}

.changeover-check-panel {
  flex: 0 0 auto;
  min-height: 0;
  overflow: hidden;
}

.changeover-check-table {
  height: auto;
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
