<script lang="ts" setup>
import { isSampleLockDeferredToCutRound } from '../shared/sampleAbnormalLockPolicy';
import { glueBoardMatchReason } from '#/utils/glue-board-match';
import { computed, h, nextTick, onActivated, onBeforeUnmount, onDeactivated, onMounted, reactive, ref, watch } from 'vue';

import { Page, useVbenModal } from '@vben/common-ui';
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
  Tabs,
  TabPane,
  Tag,
  message,
} from 'ant-design-vue';
import dayjs from 'dayjs';
import { useRoute } from 'vue-router';

import { uploadFile } from '#/api/infra/file';
import {
  applyAdhesiveFai,
  completeAdhesiveConsoleSegment,
  confirmAdhesiveConsoleIntermediate,
  confirmAdhesiveConsolePassWork,
  confirmAdhesiveConsoleReport,
  getAdhesiveConsoleCheckTemplate,
  getAdhesiveConsoleCurrentGlueBoardUsage,
  getAdhesiveConsoleGlueBoardStockByBatch,
  getAdhesiveConsoleGlueBoardStockPage,
  getAdhesiveConsoleIntermediate,
  getAdhesiveConsoleIntermediateById,
  getAdhesiveConsoleLatestGlueBoardFai,
  getAdhesiveConsolePassWorkList,
  getAdhesiveConsoleReportAqcTask,
  getAdhesiveConsoleReportList,
  getAdhesiveConsoleSourceList,
  getAdhesiveConsoleTaskList,
  getAdhesiveFaiSummary,
  markAdhesiveConsoleReportPrinted,
  reportAdhesiveConsoleGlueBoardLoss,
  reviseAdhesiveConsoleStatisticsData,
  saveAdhesiveConsoleGlueBoardUsage,
  saveAdhesiveConsoleIntermediate,
  saveAdhesiveConsolePassWork,
  saveAdhesiveConsoleReport,
  scanAdhesiveConsoleSource,
  stampAdhesiveConsoleSegmentTiming,
  startAdhesiveConsoleWorkOrder,
  switchAdhesiveConsoleWorkOrderEquipment,
  updateAdhesiveConsoleReportTime,
  type MesHcAdhesiveConsoleApi,
} from '#/api/mes/hc/execution/adhesive-console';
import type { MesHcProcessFormApi } from '#/api/mes/hc/processform';
import type { MesHcStationFormApi } from '#/api/mes/hc/stationform';
import {
  getEquipmentPage,
  type MesHcEquipmentApi,
} from '#/api/mes/hc/equipment';
import {
  getMatchedFinishedGlueBoardMapItems,
  type MesHcFinishedGlueBoardMapApi,
} from '#/api/mes/hc/finishedglueboardmap';
import { getPlanOrderDetail } from '#/api/mes/hc/planorder';
import {
  confirmProcessFormRecordBySigner,
  exportProcessFormRecordLayout,
  getProcessFormRecordDetail,
  getProcessFormRecordPage,
  importProcessFormRecordLayout,
  updateProcessFormRecord,
} from '#/api/mes/hc/processform';
import { createGlueBoardFaiRecordFromAdhesive1 } from '#/api/mes/quality/fai';
import { getStationFormPage } from '#/api/mes/hc/stationform';
import {
  createRoughLayoutCell,
  toRoughExcelColumns,
} from '#/views/mes/hc/shared/roughGrindingFormLayout';
import { ADHESIVE_INTERMEDIATE_NOTES, buildAdhesiveIntermediateFooterNotes, getAdhesiveIntermediateRowCount, resolveAdhesiveIntermediateColumns } from '#/views/mes/hc/shared/adhesiveIntermediateLayout';
import StationFormRuntimeRenderer from '#/views/mes/hc/stationform/modules/StationFormRuntimeRenderer.vue';
import StationFormRuntimeFillModal from '#/views/mes/hc/stationform/modules/runtime-fill-modal.vue';
import FaiDetailModal from '#/views/mes/quality/fai/modules/detail-modal.vue';
import AuthModal from '#/views/mes/work-order-booking/modules/components/AuthModal.vue';
import {
  getActiveSampleAbnormalLock,
  type MesQmsSampleAbnormalRecheckApi,
} from '#/api/mes/quality/sample-abnormal-recheck';
import QmsSampleAbnormalLockGuard from '#/views/mes/quality/sample-abnormal-recheck/components/QmsSampleAbnormalLockGuard.vue';
import {
  ProductionInstructionMessageTab,
  type ProductionInstructionContext,
} from '#/views/mes/hc/shared/production-instruction';
import { getProductionInstructionUnreadCount } from '#/api/mes/hc/production-instruction';
import { useExecutionFullscreenClock } from '../shared/useExecutionFullscreenClock';
import {
  buildInspectionTransferTicketPayload,
  buildWorkOrderTicketHtml,
  resolveTransferTicketQrBusinessNo,
  sendTransferTicketToPrintAgent,
} from '../shared/workOrderTicketPrint';
import { applyPrintFieldTemplate } from '../shared/printFieldTemplate';
import AdhesiveGlueBoardEdgeConsumableTab from './AdhesiveGlueBoardEdgeConsumableTab.vue';

defineOptions({ name: 'MesExecutionAdhesiveConsolePrototype' });

const ADHESIVE_DEFAULT_GLUE_BOARD_MATERIAL_CODE = '01.02.00002';
const ADHESIVE_DEV_PROCESS_CHECK_FORM_CODE_PREFIX = 'ADHESIVE_PROCESS_CHECK';
const ADHESIVE_PRINT_AGENT_URL = 'http://127.0.0.1:17820';

type DailyRecordMode = 'confirm' | 'edit' | 'view';
type SegmentTimingAction = 'END' | 'START';
type DailyRecordSubmitAction = {
  key: string;
  mode: Exclude<DailyRecordMode, 'view'>;
};

interface CurrentPlan {
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
  qtime?: MesHcAdhesiveConsoleApi.QtimeInfo;
  requirements: string;
  sourceBatchNo: string;
  sourceProductionBatchNo: string;
  startTime: string;
  status: string;
  workCenterId?: number;
  workCenterName: string;
}

interface BoardEquipment {
  code: string;
  id?: number;
  name: string;
  workCenterId?: number;
  workCenterName?: string;
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
  availableLength: number;
  batchNo: string;
  allowReportSubmit?: boolean;
  downstreamStatus?: string;
  faiApplyTime?: string;
  faiDisplayText?: string;
  faiId?: number;
  faiJudgment?: string;
  faiNo?: string;
  faiRejectReason?: string;
  faiReturnTime?: string;
  faiSampleLength?: number;
  faiStandardNo?: string;
  faiStandardVersion?: string;
  faiStatus?: string;
  glueBoardBatchNo?: string;
  glueBoardMaterialCode?: string;
  glueBoardUsageId?: number;
  grindingSecondDetailId?: number;
  label: string;
  outputLength: number;
  qtime?: MesHcAdhesiveConsoleApi.QtimeInfo;
  reportRanges: SourceReportRange[];
  segmentMark: string;
  segmentEndOperatorId?: number;
  segmentEndOperatorName?: string;
  segmentEndTime?: string;
  segmentStartOperatorId?: number;
  segmentStartOperatorName?: string;
  segmentStartTime?: string;
  segmentTimingId?: number;
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
  aqcDisplayText?: string;
  aqcFeedbackRemark?: string;
  aqcFeedbackResult?: string;
  aqcFeedbackTime?: string;
  aqcStatus?: string;
  aqcSubmitTime?: string;
  aqcTaskId?: number;
  baseBatchNo: string;
  materialCode?: string;
  modelCode?: string;
  planNo?: string;
  segments: SourceSegment[];
  totalAvailableLength: number;
  totalOutputLength: number;
  totalUsedLength: number;
}

type SampleLockCandidate = MesQmsSampleAbnormalRecheckApi.ActiveReqVO & {
  objectLabel: string;
  processName: string;
};

interface SampleLockEnsureOptions {
  actionName?: string;
  motherBatchNo?: string;
  segmentBatchNo?: string;
}

interface AdhesiveInspectionRecord {
  displayText?: string;
  faiId?: number;
  faiJudgment?: string;
  faiNo?: string;
  faiRejectReason?: string;
  faiReturnTime?: string;
  faiStandardNo?: string;
  faiStandardVersion?: string;
  faiStatus?: string;
  glueBoardBatchNo?: string;
  glueBoardMaterialCode?: string;
  glueBoardUsageId?: number;
  groupBatchNo?: string;
  id: string;
  kind: 'MOTHER' | 'SEGMENT';
  productionBatchNo?: string;
  pushedAt?: string;
  sampleLength?: number;
  sampleTypeName: string;
  segmentBatchNo?: string;
  segmentLabel: string;
}

interface AdhesiveCheckItem {
  abnormalRemark: string;
  actualValue: string;
  checkResult: 'NG' | 'OK' | string;
  itemCategory: string;
  itemName: string;
  sortNo: number;
  standardValue: string;
}

interface AdhesiveAbnormalPositionRow {
  abnormalLength?: number;
  clientKey: string;
  positionText: string;
  remark?: string;
  sortOrder?: number;
}

const route = useRoute();
const userStore = useUserStore();
const PLAN_SCAN_MIN_LENGTH = 11;
const INTERMEDIATE_PAGE_SIZE = 50;
const TASK_LIST_DEFAULT_PAGE_SIZE = 10;
const GLUE_BOARD_INSPECTION_REFRESH_INTERVAL_MS = 5 * 60_000;
const currentDateTime = ref(dayjs().format('YYYY-MM-DD HH:mm:ss'));
const currentDateText = computed(() => dayjs(currentDateTime.value).format('YYYY-MM-DD'));
const currentTimeText = computed(() => dayjs(currentDateTime.value).format('HH:mm:ss'));
const { showExecutionClock } = useExecutionFullscreenClock();
const boardLoading = ref(false);
let timer: ReturnType<typeof setInterval> | null = null;
let planScanTimer: ReturnType<typeof setTimeout> | null = null;
let glueBoardInspectionRefreshTimer: ReturnType<typeof setInterval> | null = null;
let syncingReportPosition = false;
let glueBoardInspectionRefreshing = false;

const currentUserName = computed(() => userStore.userInfo?.nickname || userStore.userInfo?.username || '');

const currentPlan = reactive<CurrentPlan>({
  availableSourceLength: 0,
  batchNo: '',
  endTime: '',
  equipmentCode: '',
  equipmentName: '',
  materialCode: '',
  modelCode: '',
  planNo: '',
  qtime: undefined,
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
  operationCode: 'WC-ADH1',
  operationName: '粘胶1',
  processCode: 'WC-ADH1',
  processName: '粘胶1',
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
  code: '',
  id: undefined,
  name: '',
  workCenterId: undefined,
  workCenterName: '',
  workStatus: '',
});

const scanPlanNo = ref('');
const planScanInputRef = ref<any>();
const recordConfirmScanInputRef = ref<any>();
const lastAutoScannedPlanNo = ref('');
let pendingScannerPlanNo = '';
let pendingScannerSegmentBatchNo = '';
let globalScannerBuffer = '';
let globalScannerLastAt = 0;
let globalScannerTimer: ReturnType<typeof setTimeout> | null = null;
let globalScannerListenerAttached = false;
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
    segmentBatchNo: parts[1] || '',
  };
};
const normalizePlanScanNo = (value?: string) => parsePlanScanCode(value).planNo;
const normalizePlanScanSegmentBatchNo = (value?: string) => parsePlanScanCode(value).segmentBatchNo;
const normalizeConfirmScanCode = (value?: string) => {
  const text = String(value || '').trim();
  const separatorIndex = text.search(PLAN_SCAN_SEPARATOR_REGEXP);
  return separatorIndex >= 0 ? text.slice(separatorIndex + 1).trim() || text : text;
};
const syncPlanScanNo = (value?: string) => {
  const scannerInput = hasPlanScanDelimiter(value);
  const { planNo, segmentBatchNo } = parsePlanScanCode(value);
  if (scannerInput) {
    pendingScannerPlanNo = planNo;
    pendingScannerSegmentBatchNo = segmentBatchNo;
  }
  if (String(value || '').trim() !== planNo && scanPlanNo.value !== planNo) {
    scanPlanNo.value = planNo;
  }
  return planNo;
};
const clearPlanScannerState = () => {
  scanPlanNo.value = '';
  pendingScannerPlanNo = '';
  pendingScannerSegmentBatchNo = '';
  lastAutoScannedPlanNo.value = '';
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
    const { planNo, segmentBatchNo } = parsePlanScanCode(rawValue);
    pendingScannerPlanNo = planNo;
    pendingScannerSegmentBatchNo = segmentBatchNo;
    scanPlanNo.value = planNo;
    schedulePlanScan(rawValue);
    return;
  }
  scanPlanNo.value = rawValue;
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
const reportRecordListVisible = ref(false);
const segmentReportListVisible = ref(false);
const segmentReportListBatchNo = ref('');
const segmentReportListRows = ref<MesHcAdhesiveConsoleApi.ReportItem[]>([]);
const dailyRecordMode = ref<DailyRecordMode>('edit');
const dailyRecordKey = ref('');
const dailyRecordAuthVisible = ref(false);
const dailyRecordAuthAction = ref('工作准备记录认证');
const pendingDailyRecordAction = ref<DailyRecordSubmitAction>();
const intermediateAuthVisible = ref(false);
const intermediateAuthAction = ref('中间品记录确认');
const activeBoardTab = ref('SOURCE');
const visualMaximized = ref(false);
const baseBoardTabs = ['SOURCE', 'GLUE_BOARD_INSPECTION', 'INSPECTION_RECORDS', 'EDGE_CONSUMABLE', 'INSTRUCTION_MESSAGES'];
const activeReportTab = ref('scan');
const reportVisible = ref(false);
const reportReadonly = ref(false);
const reportDetailViewOnly = ref(false);
const reportTimeEditing = ref(false);
const reportTimeSaving = ref(false);
const reportTimeSnapshot = ref({ endTime: '', reportDate: '', startTime: '' });
const activeReportDetailRecordId = ref<number | undefined>();
const showExtendedBoardTabs = ref(false);
const reportStep = ref<'PROCESS' | 'REPORT' | 'SCAN'>('SCAN');
const [FaiDetailPreviewModal, faiDetailModalApi] = useVbenModal({
  connectedComponent: FaiDetailModal,
});
const reportRecords = ref<MesHcAdhesiveConsoleApi.ReportItem[]>([]);
const statisticsRevisionVisible = ref(false);
const statisticsRevisionSubmitting = ref(false);
const statisticsRevisionRecord = ref<MesHcAdhesiveConsoleApi.ReportItem | null>(null);
const statisticsRevisionForm = reactive({
  inputLength: null as number | null,
  lossLength: null as number | null,
  outputLength: null as number | null,
  reason: '',
});
const selectedReportRecordKeys = ref<(number | string)[]>([]);
const segmentPrintSelectorVisible = ref(false);
const segmentPrintSelectorLoading = ref(false);
const segmentPrintSelectorTitle = ref('');
const segmentPrintSelectorRows = ref<MesHcAdhesiveConsoleApi.ReportItem[]>([]);
const segmentPrintSelectorSelectedKeys = ref<(number | string)[]>([]);
const sourceGroups = ref<SourceGroup[]>([]);
const segmentTimingAuthVisible = ref(false);
const segmentTimingAuthActionName = ref('粘胶1分段时间确认');
const segmentTimingStampingKey = ref('');
const pendingSegmentTimingAction = ref<{
  action: SegmentTimingAction;
  segment: SourceSegment;
}>();
const segmentCompleteAuthVisible = ref(false);
const pendingSegmentCompleteSegment = ref<SourceSegment | null>(null);
const segmentCompleteAuthActionName = computed(() => {
  const batchNo = pendingSegmentCompleteSegment.value?.batchNo;
  return batchNo ? `确认本段是否确认完工？（${batchNo}）` : '确认本段是否确认完工？';
});
const checkTemplate = ref<AdhesiveCheckItem[]>([]);
const adhesiveDevProcessParamForm = ref<MesHcStationFormApi.StationForm | null>(null);
const adhesiveDevProcessParamForms = ref<MesHcStationFormApi.StationForm[]>([]);
const adhesiveDevProcessParamRecords = ref<MesHcProcessFormApi.Record[]>([]);
const adhesiveDevProcessParamLoading = ref(false);
const adhesiveDevProcessParamFillOpen = ref(false);
const pendingReportAfterAdhesiveDevProcessParam = ref(false);
const adhesiveDevProcessParamInitialParams = ref<Record<string, string>>({});
const adhesiveDevProcessParamViewVisible = ref(false);
const adhesiveDevProcessParamViewLoading = ref(false);
const adhesiveDevProcessParamViewRecord = ref<MesHcProcessFormApi.Record | null>(null);
const adhesiveDevProcessParamViewMode = ref<'edit' | 'view'>('view');
const adhesiveDevProcessParamRuntimeRef = ref<any>();
const adhesiveDevProcessParamImportInputRef = ref<HTMLInputElement | null>(null);
const adhesiveDevProcessParamViewHeaderData = ref<Record<string, any>>({});
const adhesiveDevProcessParamSaving = ref(false);
const adhesiveDevProcessParamConfirming = ref(false);
const adhesiveDevProcessParamAuthVisible = ref(false);
const adhesiveDevProcessParamAuthRecord = ref<MesHcProcessFormApi.Record | null>(null);
const adhesiveDevProcessParamExcelLoading = ref(false);
const reportAbnormalRows = ref<AdhesiveAbnormalPositionRow[]>([]);
const recordConfirmVisible = ref(false);
const activeRecord = ref<MesHcAdhesiveConsoleApi.ReportItem | null>(null);
const taskListVisible = ref(false);
const taskListLoading = ref(false);
const taskRows = ref<MesHcAdhesiveConsoleApi.TaskItem[]>([]);
const taskListPage = ref(1);
const taskListPageSize = ref(TASK_LIST_DEFAULT_PAGE_SIZE);
const equipmentSelectVisible = ref(false);
const equipmentSelectLoading = ref(false);
const equipmentSelectRows = ref<MesHcEquipmentApi.Equipment[]>([]);
const equipmentSelectKeyword = ref('');
const boardEquipmentManualSelected = ref(false);
const openDailyRecordAfterEquipmentSelected = ref(false);
const activeTaskSegmentBatchNo = ref('');
const activeTaskSourceDetailIds = ref<string[]>([]);
const activeTaskInventoryLockIds = ref<string[]>([]);
const taskListFilters = reactive({
  batchNo: '',
  modelCode: '',
  planNo: '',
  status: 'UNFINISHED',
});
const intermediateVisible = ref(false);
const intermediateLoading = ref(false);
const intermediateMode = ref<'edit' | 'view'>('edit');
const activeIntermediateReport = ref<MesHcAdhesiveConsoleApi.ReportItem | null>(null);
const intermediateDetails = ref<MesHcAdhesiveConsoleApi.IntermediateDetail[]>([]);
const intermediatePage = ref(1);
const intermediateImportInputRef = ref<HTMLInputElement | null>(null);

const intermediateForm = reactive<MesHcAdhesiveConsoleApi.IntermediateRecord>({
  adhesiveReportId: undefined,
  batchNo: '',
  confirmerName: '',
  materialCode: '',
  modelCode: '',
  planId: 0,
  planNo: '',
  planOperationId: 0,
  processLength: 0,
  productWidthMm: undefined,
  productionDate: buildNowText(),
  recordTime: buildNowText(),
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
  materialName: '',
  model: '',
  qualityLockReason: '',
  qualityLockStartPosition: undefined as number | undefined,
  qualityStatus: '',
  inspectionSubmitTime: '',
  latestInspectionId: undefined as number | undefined,
  latestInspectionNo: '',
  latestInspectionResult: '',
  receiveLength: 0,
  receiveStartPosition: 0,
  returnedLength: 0,
  returnedStartPosition: 0,
  stockLength: 0,
  todayUsedLength: 0,
  availableStartPosition: 0,
  aqcSampleLength: 0,
  latestAqcTask: undefined as MesHcAdhesiveConsoleApi.AqcTask | undefined,
  lossLength: 0,
});

const glueConsumeVisible = ref(false);
const glueBoardEdgeConsumableTabRef = ref<InstanceType<typeof AdhesiveGlueBoardEdgeConsumableTab>>();
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
const motherFaiSummary = ref<MesHcAdhesiveConsoleApi.FaiSummary | null>(null);
const aqcForm = reactive({
  id: undefined as number | undefined,
  glueBoardBatchNo: '',
  sampleLength: undefined as number | undefined,
  sampleStartPosition: 0,
});
const firstInspection = ref({
  allowReportSubmit: true,
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

const glueLossVisible = ref(false);
const glueLossForm = reactive({
  endPosition: undefined as number | undefined,
  lossLength: undefined as number | undefined,
  lossReason: '',
  startPosition: 0,
});

const recordConfirmForm = reactive({
  error: '',
  matchedBatchNo: '',
  message: '',
  scannedBatchNo: '',
});

const reportForm = reactive({
  defectCode: '',
  endTime: '',
  glueBoardBatchNo: '',
  glueBoardMaterialCode: '',
  glueBoardStartPosition: 0,
  glueBoardUsageId: undefined as number | undefined,
  glueBoardUseLength: 0,
  lossLength: 0,
  napSampleLength: 0,
  outputLength: 0,
  parentBatchNo: '',
  processLength: 0,
  productionBatchNo: '',
  remark: '',
  reportDate: dayjs().format('YYYY-MM-DD'),
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
  { dataIndex: 'result', title: '执行结果', width: 120 },
  { dataIndex: 'recorderInfo', title: '记录人/时间', width: 180 },
  { dataIndex: 'confirmerInfo', title: '确认人/确认时间', width: 180 },
  { dataIndex: 'action', fixed: 'right' as const, title: '操作', width: 180 },
];
const taskListBaseColumns = [
  { dataIndex: 'planNo', title: '计划号' },
  { dataIndex: 'sourceProductionBatchNo', title: '分段批号' },
  { dataIndex: 'availableSourceLength', title: '可加工(m)' },
  { dataIndex: 'modelCode', title: '产品型号' },
  { dataIndex: 'materialCode', title: '产品料号' },
  { dataIndex: 'equipmentCode', title: '粘胶机台' },
  { dataIndex: 'qtime', title: 'QTIME' },
  { dataIndex: 'status', title: '状态' },
  { dataIndex: 'action', fixed: 'right' as const, title: '操作' },
];
const taskListColumnWidthRules: Record<string, { max: number; min: number; padding: number }> = {
  action: { max: 86, min: 72, padding: 24 },
  availableSourceLength: { max: 126, min: 98, padding: 28 },
  equipmentCode: { max: 210, min: 106, padding: 32 },
  materialCode: { max: 260, min: 118, padding: 32 },
  modelCode: { max: 220, min: 108, padding: 32 },
  planNo: { max: 260, min: 110, padding: 32 },
  qtime: { max: 220, min: 128, padding: 28 },
  sourceProductionBatchNo: { max: 320, min: 142, padding: 32 },
  status: { max: 92, min: 78, padding: 24 },
};
const taskListColumnTextGetters: Record<string, (row: MesHcAdhesiveConsoleApi.TaskItem | Record<string, any>) => unknown> = {
  action: (row) => (['FINISHED', 'PAUSED', 'CANCELLED'].includes(normalizeWorkOrderStatus(row.status)) ? '查看' : '开工'),
  availableSourceLength: (row) => formatNumber(row.availableSourceLength),
  equipmentCode: (row) => row.equipmentCode || row.equipmentName || '未绑定',
  materialCode: (row) => row.materialCode || row.motherMaterialCode,
  modelCode: (row) => row.modelCode || row.motherModelCode,
  planNo: (row) => row.planNo || row.id,
  qtime: (row) => getAdhesiveQtimeCompactText(row.qtime),
  sourceProductionBatchNo: (row) => resolveTaskSegmentBatchNo(row),
  status: (row) => getWorkOrderStatusMeta(row.status).text,
};

function parseAdhesiveQtimeDateTime(value?: string) {
  if (!value) return null;
  const parsed = dayjs(value);
  return parsed.isValid() ? parsed : null;
}

function formatAdhesiveQtimeDuration(totalMinutes?: number | null) {
  if (totalMinutes === undefined || totalMinutes === null) return '-';
  const safeMinutes = Math.max(0, Math.floor(Number(totalMinutes) || 0));
  const hours = Math.floor(safeMinutes / 60);
  const minutes = safeMinutes % 60;
  if (hours > 0 && minutes > 0) return `${hours}小时${minutes}分钟`;
  if (hours > 0) return `${hours}小时`;
  return `${minutes}分钟`;
}

function adhesiveQtimeElapsedMinutes(qtime?: MesHcAdhesiveConsoleApi.QtimeInfo | null) {
  if (!qtime?.sourceEndTime) return qtime?.elapsedMinutes;
  const sourceTime = parseAdhesiveQtimeDateTime(qtime.sourceEndTime);
  if (!sourceTime) return qtime.elapsedMinutes;
  const targetTime = qtime.targetStartTime
    ? parseAdhesiveQtimeDateTime(qtime.targetStartTime)
    : parseAdhesiveQtimeDateTime(currentDateTime.value);
  return targetTime ? Math.max(0, targetTime.diff(sourceTime, 'minute')) : qtime.elapsedMinutes;
}

function isAdhesiveQtimeTimeout(qtime?: MesHcAdhesiveConsoleApi.QtimeInfo | null) {
  const elapsedMinutes = adhesiveQtimeElapsedMinutes(qtime);
  if (
    elapsedMinutes !== undefined &&
    elapsedMinutes !== null &&
    qtime?.standardMinutes !== undefined &&
    qtime.standardMinutes !== null
  ) {
    return elapsedMinutes > qtime.standardMinutes;
  }
  return !!qtime?.timeout || qtime?.status === 'TIMEOUT';
}

function getAdhesiveQtimeTagColor(qtime?: MesHcAdhesiveConsoleApi.QtimeInfo | null) {
  if (!qtime || qtime.status === 'MISSING_SOURCE_TIME') return 'default';
  if (isAdhesiveQtimeTimeout(qtime)) return 'red';
  if (qtime.status === 'NORMAL') return 'green';
  return 'orange';
}

function getAdhesiveQtimeText(qtime?: MesHcAdhesiveConsoleApi.QtimeInfo | null) {
  if (!qtime) return '-';
  if (qtime.status === 'MISSING_SOURCE_TIME') return '磨皮未完工';
  if (qtime.status === 'NO_RULE') return '未配置额定 QTIME';
  const elapsedMinutes = adhesiveQtimeElapsedMinutes(qtime);
  if (elapsedMinutes !== undefined && elapsedMinutes !== null && qtime.standardMinutes !== undefined && qtime.standardMinutes !== null) {
    const stateText = qtime.targetStarted ? '实际粘胶1开工' : '当前粘胶1开工';
    const elapsedText = formatAdhesiveQtimeDuration(elapsedMinutes);
    const standardText = formatAdhesiveQtimeDuration(qtime.standardMinutes);
    return isAdhesiveQtimeTimeout(qtime)
      ? `${stateText}间隔 ${elapsedText}，已超出额定 ${standardText}`
      : `${stateText}间隔 ${elapsedText}，额定 ${standardText} 内`;
  }
  return qtime.message || '-';
}

function getAdhesiveQtimeCompactText(qtime?: MesHcAdhesiveConsoleApi.QtimeInfo | null) {
  if (!qtime) return '-';
  if (qtime.status === 'MISSING_SOURCE_TIME') return '磨皮未完工';
  if (qtime.status === 'NO_RULE') return '未配置';
  const elapsedText = formatAdhesiveQtimeDuration(adhesiveQtimeElapsedMinutes(qtime));
  return isAdhesiveQtimeTimeout(qtime) ? `超时 ${elapsedText}` : elapsedText;
}

function getAdhesiveQtimeSeverity(qtime?: MesHcAdhesiveConsoleApi.QtimeInfo | null) {
  if (!qtime) return 0;
  if (isAdhesiveQtimeTimeout(qtime)) return 4;
  if (qtime.status === 'NO_RULE') return 3;
  if (qtime.status === 'MISSING_SOURCE_TIME') return 2;
  if (qtime.status === 'NORMAL') return 1;
  return 2;
}

const adhesiveTopQtimeMeta = computed(() => {
  const segmentQtimes = sourceGroups.value
    .flatMap((group) => group.segments.map((segment) => segment.qtime))
    .filter(Boolean) as MesHcAdhesiveConsoleApi.QtimeInfo[];
  const qtimes = segmentQtimes.length > 0
    ? segmentQtimes
    : currentPlan.qtime
      ? [currentPlan.qtime]
      : [];
  const qtime = qtimes
    .slice()
    .sort((left, right) => {
      const severityDiff = getAdhesiveQtimeSeverity(right) - getAdhesiveQtimeSeverity(left);
      if (severityDiff !== 0) return severityDiff;
      return Number(adhesiveQtimeElapsedMinutes(right) || 0) - Number(adhesiveQtimeElapsedMinutes(left) || 0);
    })[0];
  if (!currentPlan.planNo) {
    return {
      stampClass: 'empty',
      stampText: '待扫码',
      subText: '未选择计划',
      title: '请先扫码或选择粘胶1待加工计划',
    };
  }
  if (!qtime) {
    return {
      stampClass: 'empty',
      stampText: '未获取',
      subText: '无QTIME',
      title: '当前计划暂无粘胶1 QTIME 数据',
    };
  }
  if (isAdhesiveQtimeTimeout(qtime)) {
    return {
      stampClass: 'ng',
      stampText: '已超时',
      subText: getAdhesiveQtimeCompactText(qtime),
      title: getAdhesiveQtimeText(qtime),
    };
  }
  if (qtime.status === 'NORMAL') {
    return {
      stampClass: 'ok',
      stampText: '正常',
      subText: getAdhesiveQtimeCompactText(qtime),
      title: getAdhesiveQtimeText(qtime),
    };
  }
  if (qtime.status === 'NO_RULE') {
    return {
      stampClass: 'waiting',
      stampText: '未配置',
      subText: '额定QTIME',
      title: getAdhesiveQtimeText(qtime),
    };
  }
  return {
    stampClass: 'waiting',
    stampText: qtime.status === 'MISSING_SOURCE_TIME' ? '磨皮未完' : '待判定',
    subText: getAdhesiveQtimeCompactText(qtime),
    title: getAdhesiveQtimeText(qtime),
  };
});

const equipmentSelectColumns = [
  { dataIndex: 'equipmentCode', title: '设备编码', width: 150 },
  { dataIndex: 'equipmentName', title: '设备名称', width: 180 },
  { dataIndex: 'workCenterName', title: '工作中心', width: 160 },
  { dataIndex: 'workStatus', title: '运行状态', width: 120 },
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

const glueBoardStockSelectColumns = [
  { dataIndex: 'glueBoardBatchNo', title: '胶板批号', width: 180 },
  { dataIndex: 'glueBoardModel', title: '胶板型号', width: 120 },
  { dataIndex: 'glueBoardMaterialCode', title: '胶板料号', width: 130 },
  { dataIndex: 'receiveTime', title: '领用时间', width: 170 },
  { dataIndex: 'availableLength', title: '边库余量(m)', width: 120 },
  { dataIndex: 'qualityStatus', title: '今日质量状态', width: 120 },
  { dataIndex: 'action', fixed: 'right' as const, title: '操作', width: 190 },
];

const adhesiveIntermediateExcelColumns = computed(() => resolveAdhesiveIntermediateColumns(intermediateForm.thicknessLabels));
const intermediateDetailColumns = computed(() => [
  { align: 'center' as const, dataIndex: 'seq', title: '序号', width: 56 },
  ...adhesiveIntermediateExcelColumns.value.map((column) => ({ dataIndex: column.key, title: column.title, width: column.width })),
]);
const ADHESIVE_INTERMEDIATE_DISPLAY_NAME = '粘胶1中间品记录表';
const intermediateDisplayName = computed(() => intermediateForm.stationFormName || ADHESIVE_INTERMEDIATE_DISPLAY_NAME);
const intermediateNotes = computed(() => ADHESIVE_INTERMEDIATE_NOTES.map((note) => ({ ...note, text: intermediateForm.formNotes?.[note.key] || '' })).filter((note) => note.text));

const selectedDailyRecord = computed(() => dailyRecordRows.value.find((item) => item.key === dailyRecordKey.value));
const selectedReportRecords = computed(() =>
  reportRecords.value.filter((record) => selectedReportRecordKeys.value.includes(record.id as number | string)),
);
const printableReportRecords = computed(() =>
  selectedReportRecords.value.filter((record) => !!record.productionBatchNo),
);
const unprintedReportRecords = computed(() =>
  reportRecords.value.filter((record) => !!record.productionBatchNo && getRecordPrintMeta(record).status !== '已打印'),
);
const glueBoardConsumeLength = computed(() =>
  Math.max(Number(reportForm.processLength || 0) - Number(reportForm.lossLength || 0), 0),
);
const glueBoardAfterStock = computed(() => Math.max(Number(glueBoard.stockLength || 0) - glueBoardConsumeLength.value, 0));
const glueBoardAfterTodayUsed = computed(() => Number(glueBoard.todayUsedLength || 0) + glueBoardConsumeLength.value);
const glueBoardStatusMeta = computed(() => {
  if (!glueBoard.materialCode && glueBoard.stockLength <= 0) return { color: 'default', text: '未加载' };
  if (glueBoard.alarm) return { color: 'red', text: '状态提醒' };
  if (!hasGlueBoardInspectionRecord()) return { color: 'default', text: '未送检' };
  const qualityStatus = String(glueBoard.qualityStatus || '').toUpperCase();
  if (qualityStatus === 'WAITING') return { color: 'processing', text: '已送检' };
  if (qualityStatus === 'ABNORMAL') return { color: 'red', text: '质量异常' };
  if (qualityStatus === 'LOCKED') return { color: 'red', text: '已锁定' };
  return { color: 'green', text: '正常' };
});
const glueBoardAvailableText = computed(() => `${formatNumber(glueBoard.stockLength)} m`);
const glueBoardMapCandidateModels = computed(() =>
  getDistinctCandidateValues(glueBoardMapCandidates.value.map((item) => item.glueBoardModel)),
);
const glueBoardMapCandidateModelQuery = computed(() => glueBoardMapCandidateModels.value.join(','));
const glueBoardMapCandidateText = computed(() =>
  glueBoardMapCandidates.value.map(formatGlueBoardMapCandidate).filter(Boolean).join('、'),
);
const glueBoardMapFilterNotice = computed(() => {
  if (glueBoardMapLoading.value) return '正在匹配当前产品型号的胶板对照...';
  if (!currentPlan.modelCode) return '未选择待加工计划，已放开查询；请注意选择对应待加工计划。';
  if (glueBoardMapCandidates.value.length > 0) {
    return `按产品型号 ${currentPlan.modelCode} 的成品胶板对照过滤`;
  }
  return `未维护产品型号 ${currentPlan.modelCode} 的粘胶1胶板对照，已放开查询；请注意选择对应待加工计划。`;
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
  return !glueBoard.id
    ? '请先登记胶板领用，系统会加载当前可用长度。'
    : '报工前先登记胶板领用，可按需提交胶板检验。';
});
const glueBoardTitleTipWarning = computed(() => !!glueBoardMapMismatchTip.value || !!glueBoard.alarm);
const glueBoardAqcStatusMeta = computed(() => {
  if (!glueBoard.stockId && !firstInspection.value.faiId) {
    return { color: 'default', stampClass: 'empty', stampText: '未领用', subText: '未登记', text: '未领用' };
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
    return { color: 'processing', stampClass: 'waiting', stampText: '已送检', subText: '待检测', text: '待检测' };
  }
  if (status === 'INSPECTING') {
    return { color: 'processing', stampClass: 'waiting', stampText: '已送检', subText: '检测中', text: '检测中' };
  }
  if (status === 'WAITING_QA') {
    return { color: 'processing', stampClass: 'waiting', stampText: '已送检', subText: '待复核', text: '待复核' };
  }
  if (status === 'COMPLETED' && judgment === 'OK') {
    return { color: 'green', stampClass: 'ok', stampText: '已放行', subText: '合格', text: '合格' };
  }
  if (status === 'COMPLETED' && judgment === 'NG') {
    return { color: 'red', stampClass: 'ng', stampText: '检验NG', subText: '不合格', text: '不合格' };
  }
  if (status === 'REJECTED') {
    return { color: 'red', stampClass: 'ng', stampText: '已退回', subText: '待重送', text: '待重送' };
  }
  return {
    color: 'default',
    stampClass: 'empty',
    stampText: '未送检',
    subText: formatInspectionJudgmentText(judgment),
    text: '未送检',
  };
});
const glueBoardLatestInspectionStatusMeta = computed(() => {
  const status = String(firstInspection.value.faiStatus || '').toUpperCase();
  const judgment = String(firstInspection.value.faiJudgment || '').toUpperCase();
  if (!firstInspection.value.faiId) {
    return { color: 'default', stampClass: 'empty', stampText: '未送检', subText: '未登记', text: '未送检' };
  }
  if (status === 'PENDING') {
    return { color: 'processing', stampClass: 'waiting', stampText: '已送检', subText: '待检测', text: '待检测' };
  }
  if (status === 'INSPECTING') {
    return { color: 'processing', stampClass: 'waiting', stampText: '已送检', subText: '检测中', text: '检测中' };
  }
  if (status === 'WAITING_QA') {
    return { color: 'processing', stampClass: 'waiting', stampText: '已送检', subText: '待复核', text: '待复核' };
  }
  if (status === 'COMPLETED' && judgment === 'OK') {
    return { color: 'green', stampClass: 'ok', stampText: '已放行', subText: '合格', text: '合格' };
  }
  if (status === 'COMPLETED' && judgment === 'NG') {
    return { color: 'red', stampClass: 'ng', stampText: '检验NG', subText: '不合格', text: '不合格' };
  }
  if (status === 'REJECTED') {
    return { color: 'red', stampClass: 'ng', stampText: '已退回', subText: '待重送', text: '待重送' };
  }
  return {
    color: 'default',
    stampClass: 'empty',
    stampText: firstInspection.value.displayText || '未提交',
    subText: formatInspectionJudgmentText(judgment),
    text: firstInspection.value.displayText || '-',
  };
});
const glueBoardAqcStampTime = computed(() => {
  if (!hasGlueBoardInspectionRecord()) return '未送检';
  return glueBoard.inspectionSubmitTime || firstInspection.value.faiApplyTime || '已送检';
});
const glueBoardInspectionTabNotice = computed(() => {
  const latestText = firstInspection.value.faiId
    ? `当前批号最后一次送检：${firstInspection.value.faiNo || firstInspection.value.sourceReportNo || firstInspection.value.faiId}`
    : '当前批号暂无送检单';
  if (!glueBoard.stockId && !glueBoard.batchNo) {
    return '当前未登记胶板领用，请先登记胶板批号后再提交胶板检验。';
  }
  if (!hasGlueBoardInspectionRecord()) {
    return `${latestText}；当前尚未提交胶板检验，可点击“胶板检验”完成送检登记。`;
  }
  return `${latestText}；顶部状态显示当前胶板检验结果。`;
});
const reportRecordRowSelection = computed(() => ({
  fixed: true,
  onChange: (keys: (number | string)[]) => {
    selectedReportRecordKeys.value = keys;
  },
  selectedRowKeys: selectedReportRecordKeys.value,
}));
const segmentPrintSelectorSelectedRows = computed(() => {
  const selectedKeys = new Set(segmentPrintSelectorSelectedKeys.value.map((key) => String(key)));
  return segmentPrintSelectorRows.value.filter((record) => selectedKeys.has(String(record.id)));
});
const segmentPrintSelectorRowSelection = computed(() => ({
  fixed: true,
  onChange: (keys: (number | string)[]) => {
    segmentPrintSelectorSelectedKeys.value = keys;
  },
  selectedRowKeys: segmentPrintSelectorSelectedKeys.value,
}));
const reportOutputLength = computed(() =>
  Math.max(Number(reportForm.processLength || 0) - Number(reportForm.lossLength || 0) - Number(reportForm.napSampleLength || 0), 0),
);
const reportNapSampleSummary = computed(() => {
  const segment = findSourceSegment(reportForm.sourceProductionBatchNo);
  const reportedLength = segment ? getSegmentReportNapSampleLength(segment) : 0;
  const inspectionLength = Number(segment?.faiSampleLength || 0);
  return {
    inspectionLength,
    pendingLength: Math.max(inspectionLength - reportedLength, 0),
    reportedLength,
  };
});

function normalizeReportAbnormalLength(value: unknown) {
  if (value === undefined || value === null || value === '') return undefined;
  const numericValue = Number(value);
  return Number.isFinite(numericValue) ? numericValue : undefined;
}

function createReportAbnormalPositionRow(row?: Partial<AdhesiveAbnormalPositionRow>): AdhesiveAbnormalPositionRow {
  return {
    abnormalLength: normalizeReportAbnormalLength(row?.abnormalLength),
    clientKey: row?.clientKey || `adhesive-abnormal-${Date.now()}-${Math.random()}`,
    positionText: String(row?.positionText || ''),
    remark: String(row?.remark || ''),
    sortOrder: row?.sortOrder,
  };
}

function addReportAbnormalRow() {
  reportAbnormalRows.value.push(createReportAbnormalPositionRow());
}

function removeReportAbnormalRow(index: number) {
  reportAbnormalRows.value.splice(index, 1);
}

function openReportAbnormalTab() {
  if (!reportAbnormalRows.value.length) {
    addReportAbnormalRow();
  }
  activeReportTab.value = 'abnormal-position';
}

function buildReportAbnormalPositions() {
  const rows = reportAbnormalRows.value
    .map((row, index) => {
      const abnormalLength = normalizeReportAbnormalLength(row.abnormalLength);
      return {
        abnormalLength,
        positionText: String(row.positionText || '').trim(),
        remark: String(row.remark || '').trim(),
        sortOrder: index + 1,
      };
    })
    .filter((row) => row.positionText || row.abnormalLength !== undefined || Boolean(row.remark));
  for (const row of rows) {
    if (!row.positionText) {
      AModal.warning({
        content: '异常位置不能为空。',
        title: '请完善异常位置',
      });
      activeReportTab.value = 'abnormal-position';
      return null;
    }
    if (row.abnormalLength !== undefined && (!Number.isFinite(row.abnormalLength) || row.abnormalLength < 0)) {
      AModal.warning({
        content: '异常位置米数不能为负数。',
        title: '请完善异常位置',
      });
      activeReportTab.value = 'abnormal-position';
      return null;
    }
  }
  return rows as MesHcAdhesiveConsoleApi.AbnormalPositionSaveReq[];
}

function formatReportAbnormalPositionSummary(rows?: AdhesiveAbnormalPositionRow[]) {
  const validRows = (rows || []).filter(
    (row) =>
      String(row.positionText || '').trim() ||
      normalizeReportAbnormalLength(row.abnormalLength) !== undefined ||
      Boolean(row.remark),
  );
  if (!validRows.length) return '未登记';
  const totalLength = validRows.reduce(
    (sum, row) => sum + Number(normalizeReportAbnormalLength(row.abnormalLength) || 0),
    0,
  );
  return totalLength > 0
    ? `${validRows.length} 条 / ${formatNumber(totalLength)} m`
    : `${validRows.length} 条`;
}

function getIntermediateRowCount(length = Number(reportForm.processLength || 0)) {
  return getAdhesiveIntermediateRowCount(length);
}

function createIntermediateDetailRow(sortNo: number, lengthMark = sortNo): MesHcAdhesiveConsoleApi.IntermediateDetail {
  return {
    lengthMark,
    remark: '',
    sortNo,
  };
}

function buildDefaultIntermediateDetails(processLength: number) {
  return Array.from({ length: getIntermediateRowCount(processLength) }, (_, index) =>
    createIntermediateDetailRow(index + 1),
  );
}

const intermediatePagedDetails = computed(() => {
  const start = (intermediatePage.value - 1) * INTERMEDIATE_PAGE_SIZE;
  return intermediateDetails.value.slice(start, start + INTERMEDIATE_PAGE_SIZE).map((row, index) => {
    (row as MesHcAdhesiveConsoleApi.IntermediateDetail & { seq: number }).seq = start + index + 1;
    return row as MesHcAdhesiveConsoleApi.IntermediateDetail & { seq: number };
  });
});
const reportCheckCategories = computed(() => (checkTemplate.value.length ? ['工艺参数'] : []));
const adhesiveInspectionRecords = computed<AdhesiveInspectionRecord[]>(() => {
  const rows: AdhesiveInspectionRecord[] = [];
  const motherSummary = motherFaiSummary.value;
  if (motherSummary?.faiId) {
    rows.push({
      displayText: motherSummary.displayText,
      faiId: motherSummary.faiId,
      faiJudgment: motherSummary.faiJudgment,
      faiNo: motherSummary.faiNo,
      faiRejectReason: motherSummary.faiRejectReason,
      faiReturnTime: motherSummary.faiReturnTime,
      faiStandardNo: motherSummary.faiStandardNo,
      faiStandardVersion: motherSummary.faiStandardVersion,
      faiStatus: motherSummary.faiStatus,
      glueBoardBatchNo: motherSummary.glueBoardBatchNo,
      glueBoardMaterialCode: motherSummary.glueBoardMaterialCode,
      glueBoardUsageId: motherSummary.glueBoardUsageId,
      groupBatchNo: currentPlan.batchNo || currentPlan.sourceBatchNo,
      id: `MOTHER-${motherSummary.faiId}`,
      kind: 'MOTHER',
      productionBatchNo: currentPlan.batchNo || currentPlan.sourceBatchNo || currentPlan.sourceProductionBatchNo,
      pushedAt: motherSummary.faiApplyTime,
      sampleLength: motherSummary.sampleLength,
      sampleTypeName: '整体母卷首检',
      segmentLabel: '整体母卷',
    });
  }
  sourceGroups.value.forEach((group) => {
    group.segments.forEach((segment) => {
      if (!segment.faiId) return;
      rows.push({
        displayText: segment.faiDisplayText,
        faiId: segment.faiId,
        faiJudgment: segment.faiJudgment,
        faiNo: segment.faiNo,
        faiRejectReason: segment.faiRejectReason,
        faiReturnTime: segment.faiReturnTime,
        faiStandardNo: segment.faiStandardNo,
        faiStandardVersion: segment.faiStandardVersion,
        faiStatus: segment.faiStatus,
        glueBoardBatchNo: segment.glueBoardBatchNo,
        glueBoardMaterialCode: segment.glueBoardMaterialCode,
        glueBoardUsageId: segment.glueBoardUsageId,
        groupBatchNo: group.baseBatchNo,
        id: `SEGMENT-${segment.grindingSecondDetailId || segment.batchNo}-${segment.faiId}`,
        kind: 'SEGMENT',
        productionBatchNo: segment.batchNo,
        pushedAt: segment.faiApplyTime,
        sampleLength: segment.faiSampleLength,
        sampleTypeName: '分段留样',
        segmentBatchNo: segment.batchNo,
        segmentLabel: segment.label || getSegmentLabel(segment.segmentMark),
      });
    });
  });
  return rows.sort((a, b) => dayjs(b.pushedAt || 0).valueOf() - dayjs(a.pushedAt || 0).valueOf());
});
const adhesiveInspectionSummary = computed(() => ({
  abnormal: adhesiveInspectionRecords.value.filter((record) => isAdhesiveInspectionRecordAbnormal(record)).length,
  ok: adhesiveInspectionRecords.value.filter((record) => isAdhesiveInspectionRecordOk(record)).length,
  pending: adhesiveInspectionRecords.value.filter(
    (record) => !isAdhesiveInspectionRecordOk(record) && !isAdhesiveInspectionRecordAbnormal(record),
  ).length,
  total: adhesiveInspectionRecords.value.length,
}));

const renderLossLengthTitle = () =>
  h('span', [
    '固定损耗-',
    h('span', { class: 'report-loss-sample-emphasis' }, '包含小样条'),
    '（米）',
  ]);

const recordColumns = [
  { dataIndex: 'sourceProductionBatchNo', key: 'sourceProductionBatchNo', title: '粘胶批次', width: 180 },
  { dataIndex: 'startPosition', key: 'startPosition', title: '起始位置(m)', width: 110 },
  { dataIndex: 'endPosition', key: 'endPosition', title: '结束位置(m)', width: 110 },
  { dataIndex: 'inputLength', key: 'inputLength', title: '加工米数(m)', width: 120 },
  { dataIndex: 'lossLength', key: 'lossLength', title: renderLossLengthTitle, width: 170 },
  { dataIndex: 'outputLength', key: 'outputLength', title: '产出米数(m)', width: 120 },
  { dataIndex: 'napSampleLength', key: 'napSampleLength', title: '送检米数', width: 150 },
  { dataIndex: 'glueBoardMaterialCode', key: 'glueBoardMaterialCode', title: '胶板料号', width: 140 },
  { dataIndex: 'glueBoardBatchNo', key: 'glueBoardBatchNo', title: '胶板批号', width: 140 },
  { dataIndex: 'startTime', key: 'startTime', title: '开始时间', width: 170 },
  { dataIndex: 'endTime', key: 'endTime', title: '结束时间', width: 170 },
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
    dataIndex: 'action',
    fixed: 'right' as const,
    key: 'action',
    title: '操作',
    width: 168,
  },
];
const segmentPrintSelectColumns = [
  { dataIndex: 'sourceProductionBatchNo', key: 'sourceProductionBatchNo', title: '粘胶批次', width: 170 },
  { dataIndex: 'productionBatchNo', key: 'productionBatchNo', title: '报工批次', width: 190 },
  { dataIndex: 'positionRange', key: 'positionRange', title: '起止位置(m)', width: 160 },
  { dataIndex: 'inputLength', key: 'inputLength', title: '已加工(m)', width: 110 },
  { dataIndex: 'outputLength', key: 'outputLength', title: '产出(m)', width: 110 },
  { dataIndex: 'printCount', key: 'printCount', title: '打印次数', width: 90 },
  { dataIndex: 'printStatus', key: 'printStatus', title: '打印标记', width: 100 },
  { dataIndex: 'reportStatus', key: 'reportStatus', title: '确认状态', width: 100 },
];

const intermediateRecordColumns = [
  { dataIndex: 'sourceProductionBatchNo', title: '粘胶批次', width: 180 },
  { dataIndex: 'outputLength', title: '产出米数(m)', width: 120 },
  { dataIndex: 'reportDate', title: '报工日期', width: 120 },
  { dataIndex: 'recorderName', title: '记录人', width: 120 },
  { dataIndex: 'reportStatus', title: '报工状态', width: 110 },
  { dataIndex: 'action', fixed: 'right' as const, title: '操作', width: 170 },
];
const inspectionRecordColumns = [
  { dataIndex: 'segmentLabel', title: '检验对象', width: 110 },
  { dataIndex: 'faiNo', title: '送检单号', width: 170 },
  { dataIndex: 'productionBatchNo', title: '受检批次', width: 180 },
  { dataIndex: 'glueBoardMaterialCode', title: '胶板料号', width: 140 },
  { dataIndex: 'glueBoardBatchNo', title: '胶板批号', width: 140 },
  { dataIndex: 'sampleTypeName', title: '样品类型', width: 130 },
  { dataIndex: 'pushedAt', title: '送检时间', width: 170 },
  { dataIndex: 'status', title: '送检记录单状态', width: 150 },
  { dataIndex: 'result', title: '检验结果', width: 120 },
  { dataIndex: 'faiStandardNo', title: '检验标准', width: 170 },
  { dataIndex: 'feedbackTime', title: '反馈时间', width: 170 },
  { dataIndex: 'feedbackRemark', title: '反馈备注', width: 220 },
  { dataIndex: 'action', fixed: 'right' as const, title: '操作', width: 190 },
];
const reportProcessColumns = [
  { dataIndex: 'itemCategory', title: '类别', width: 120 },
  { dataIndex: 'itemName', title: '工艺参数项目', width: 170 },
  { dataIndex: 'standardValue', title: '标准', width: 180 },
  { dataIndex: 'actualValue', title: '实测值', width: 200 },
  { dataIndex: 'abnormalRemark', title: '异常备注', width: 180 },
];
const adhesiveDevProcessParamColumns = [
  { dataIndex: 'planNo', title: '计划号', width: 150 },
  { dataIndex: 'batchNo', title: '批号', width: 160 },
  { dataIndex: 'recordStatus', title: '状态', width: 100 },
  { dataIndex: 'fillInfo', title: '填写人/时间', width: 220 },
  { dataIndex: 'confirmResult', title: '确认结果', width: 100 },
  { dataIndex: 'confirmInfo', title: '确认人/时间', width: 220 },
  { dataIndex: 'action', fixed: 'right' as const, title: '操作', width: 190 },
];

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

function displayDateTimeText(value?: string) {
  if (!value) return '-';
  const parsed = dayjs(value);
  return parsed.isValid() ? parsed.format('YYYY-MM-DD HH:mm:ss') : value;
}

function getDistinctCandidateValues(values: Array<string | undefined>) {
  return Array.from(
    new Set(values.map((value) => String(value || '').trim()).filter(Boolean)),
  );
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

function buildGlueBoardMapMismatchTip(stock: {
  glueBoardBatchNo?: string;
  glueBoardMaterialCode?: string;
  glueBoardModel?: string;
}) {
  if (!currentPlan.modelCode || glueBoardMapCandidates.value.length === 0) return '';
  const currentModel = String(stock.glueBoardModel || '').trim();
  const currentMaterialCode = String(stock.glueBoardMaterialCode || '').trim();
  const matched = glueBoardMapCandidates.value.some((candidate) => {
    const expectedModel = String(candidate.glueBoardModel || '').trim();
    const expectedMaterialCode = String(candidate.glueBoardMaterialCode || '').trim();
    const modelMatched = expectedModel
      ? currentModel.toUpperCase() === expectedModel.toUpperCase()
      : true;
    const materialMatched = expectedMaterialCode
      ? currentMaterialCode.toUpperCase() === expectedMaterialCode.toUpperCase()
      : true;
    return modelMatched && materialMatched;
  });
  if (!currentModel || matched) return '';
  const currentDisplay = [stock.glueBoardMaterialCode, currentModel, stock.glueBoardBatchNo]
    .map((item) => String(item || '').trim())
    .filter(Boolean)
    .join(' / ');
  return `当前产品型号 ${currentPlan.modelCode} 建议领用胶板 ${getGlueBoardMapCandidateDisplay()}，当前胶板 ${currentDisplay || currentModel} 不匹配，请重新选择批次。`;
}

function getSelectedGlueBoardModel() {
  if ((glueBoard.batchNo || glueBoard.stockId || glueBoard.id) && glueBoard.model) {
    return glueBoard.model;
  }
  return glueBoardMapCandidateModels.value.length === 1 ? glueBoardMapCandidateModels.value[0] || '' : '';
}

function getActualGlueBoardModel() {
  return String(glueBoard.model || '').trim();
}

function getActualGlueBoardMaterialCode() {
  return String(glueBoard.materialCode || '').trim();
}

function roundMeter(value: number) {
  if (!Number.isFinite(value)) return 0;
  return Number(value.toFixed(3));
}

function normalizeMeterNumber(value?: unknown) {
  if (value === undefined || value === null || value === '') return undefined;
  const numericValue = Number(value);
  return Number.isFinite(numericValue) ? roundMeter(numericValue) : undefined;
}

function resolveAdhesiveReportPositionRange(record: MesHcAdhesiveConsoleApi.ReportItem | Record<string, any>) {
  const fallbackLength =
    normalizeMeterNumber(record.inputLength) ??
    normalizeMeterNumber(record.outputLength) ??
    normalizeMeterNumber(record.glueBoardUseLength);
  let start = normalizeMeterNumber(record.startPosition ?? record.start_position);
  let end = normalizeMeterNumber(record.endPosition ?? record.end_position);
  if (start !== undefined && end !== undefined && end > start) {
    return { end, start };
  }
  if (start !== undefined && fallbackLength !== undefined && fallbackLength > 0) {
    end = roundMeter(start + fallbackLength);
    return { end, start };
  }
  if (end !== undefined && fallbackLength !== undefined && fallbackLength > 0) {
    start = roundMeter(Math.max(end - fallbackLength, 0));
    return { end, start };
  }
  if (fallbackLength !== undefined && fallbackLength > 0) {
    return { end: fallbackLength, start: 0 };
  }
  return { end, start };
}

function buildReportRange(record: MesHcAdhesiveConsoleApi.ReportItem): SourceReportRange | null {
  const { end, start } = resolveAdhesiveReportPositionRange(record);
  if (start === undefined || end === undefined || end <= start) return null;
  return {
    end,
    label: record.productionBatchNo || record.sourceProductionBatchNo || '已报工',
    reportId: record.id,
    start,
    status: record.reportStatus,
  };
}

function isSameSegmentReportBatch(record: MesHcAdhesiveConsoleApi.ReportItem | Record<string, any>, segmentBatchNo?: string) {
  const targetBatchNo = normalizeAdhesiveScanBatch(segmentBatchNo);
  if (!targetBatchNo) return false;
  return (
    normalizeAdhesiveScanBatch(record.sourceProductionBatchNo) === targetBatchNo ||
    normalizeAdhesiveScanBatch(record.productionBatchNo) === targetBatchNo
  );
}

function getReportRanges(sourceProductionBatchNo: string) {
  return reportRecords.value
    .filter((item) => isSameSegmentReportBatch(item, sourceProductionBatchNo))
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

function findSourceGroupBySegmentBatchNo(batchNo?: string) {
  const sourceBatchNo = String(batchNo || '').trim();
  if (!sourceBatchNo) return undefined;
  return sourceGroups.value.find((group) =>
    group.segments.some((segment) => segment.batchNo === sourceBatchNo),
  );
}

function getRequestErrorMessage(error: unknown, fallback: string) {
  const err = error as { message?: string; response?: { data?: { msg?: string } } };
  return err?.response?.data?.msg || err?.message || fallback;
}

function normalizeSampleLockBatchNo(value?: string | null) {
  return String(value || '').trim();
}

function getSampleLockSegmentBatchNo(segmentOrBatchNo?: SourceSegment | string, options?: SampleLockEnsureOptions) {
  return normalizeSampleLockBatchNo(
    options?.segmentBatchNo ||
      (typeof segmentOrBatchNo === 'string' ? segmentOrBatchNo : segmentOrBatchNo?.batchNo),
  );
}

function getSampleLockMotherBatchNo(segmentBatchNo?: string, options?: SampleLockEnsureOptions) {
  const explicitMotherBatchNo = normalizeSampleLockBatchNo(options?.motherBatchNo);
  if (explicitMotherBatchNo) return explicitMotherBatchNo;
  const sourceGroup = findSourceGroupBySegmentBatchNo(segmentBatchNo);
  return normalizeSampleLockBatchNo(
    sourceGroup?.baseBatchNo ||
      currentPlan.batchNo ||
      currentPlan.sourceBatchNo ||
      (segmentBatchNo ? stripSegmentMark(segmentBatchNo) : ''),
  );
}

function getSampleLockWetMotherObjectNos(motherBatchNo?: string, segmentBatchNo?: string) {
  return [
    normalizeSampleLockBatchNo(motherBatchNo),
    normalizeSampleLockBatchNo(segmentBatchNo),
    normalizeSampleLockBatchNo(stripSegmentMark(motherBatchNo)),
    normalizeSampleLockBatchNo(stripSegmentMark(segmentBatchNo)),
  ].filter(Boolean);
}

function pushSampleLockCandidate(
  candidates: SampleLockCandidate[],
  seen: Set<string>,
  candidate: SampleLockCandidate,
) {
  if (!candidate.objectNo || !candidate.objectType || !candidate.sourceProcessCode) return;
  const key = `${candidate.sourceProcessCode}|${candidate.objectType}|${candidate.objectNo}`;
  if (seen.has(key)) return;
  seen.add(key);
  candidates.push(candidate);
}

function getAdhesiveSampleLockCandidates(
  segmentOrBatchNo?: SourceSegment | string,
  options: SampleLockEnsureOptions = {},
) {
  const segmentBatchNo = getSampleLockSegmentBatchNo(segmentOrBatchNo, options);
  const motherBatchNo = getSampleLockMotherBatchNo(segmentBatchNo, options);
  const candidates: SampleLockCandidate[] = [];
  const seen = new Set<string>();
  getSampleLockWetMotherObjectNos(motherBatchNo, segmentBatchNo).forEach((objectNo) => {
    pushSampleLockCandidate(candidates, seen, {
      qualificationObjectNo: segmentBatchNo || motherBatchNo,
      objectLabel: '母卷',
      objectNo,
      objectType: 'MOTHER_ROLL',
      processName: '湿法',
      sourceProcessCode: 'WET',
    });
  });
  pushSampleLockCandidate(candidates, seen, {
    objectLabel: '分段',
    objectNo: segmentBatchNo,
    objectType: 'SEGMENT',
    processName: '磨皮',
    sourceProcessCode: 'ROUGH_GRINDING',
  });
  pushSampleLockCandidate(candidates, seen, {
    objectLabel: '分段',
    objectNo: segmentBatchNo,
    objectType: 'SEGMENT',
    processName: '粘胶1',
    sourceProcessCode: 'ADHESIVE1',
  });
  return candidates;
}

async function ensureAdhesiveSampleAbnormalUnlocked(
  segmentOrBatchNo?: SourceSegment | string,
  options: SampleLockEnsureOptions = {},
) {
  const candidates = getAdhesiveSampleLockCandidates(segmentOrBatchNo, options);
  if (!candidates.length) return true;
  for (const candidate of candidates) {
    if (isSampleLockDeferredToCutRound(candidate.sourceProcessCode)) continue;
    const lock = await getActiveSampleAbnormalLock(candidate);
    if (!lock) continue;
    const objectLabel =
      String(lock.objectType || '').toUpperCase() === 'MOTHER_ROLL' ? '母卷' : candidate.objectLabel;
    const processName = lock.sourceProcessName || candidate.processName;
    AModal.warning({
      content:
        lock.lockReason ||
        `当前${objectLabel} ${lock.objectNo || candidate.objectNo} 因 ${lock.abnormalFeedbackTime || '-'}，${processName} 留样送检NG异常，锁定不允许继续${options.actionName || '报工'}，等待复检确认后继续。`,
      title: '留样异常锁定',
    });
    return false;
  }
  return true;
}

function applySegmentFaiSummary(segment: SourceSegment, summary?: MesHcAdhesiveConsoleApi.FaiSummary | null) {
  segment.allowReportSubmit = !!summary?.allowReportSubmit;
  segment.faiApplyTime = summary?.faiApplyTime || '';
  segment.faiDisplayText = summary?.displayText || '';
  segment.faiId = summary?.faiId;
  segment.faiJudgment = summary?.faiJudgment || '';
  segment.faiNo = summary?.faiNo || '';
  segment.faiRejectReason = summary?.faiRejectReason || '';
  segment.faiReturnTime = summary?.faiReturnTime || '';
  segment.faiSampleLength = Number(summary?.sampleLength || 0) || undefined;
  segment.faiStandardNo = summary?.faiStandardNo || '';
  segment.faiStandardVersion = summary?.faiStandardVersion || '';
  segment.faiStatus = summary?.faiStatus || '';
  segment.glueBoardBatchNo = summary?.glueBoardBatchNo || '';
  segment.glueBoardMaterialCode = summary?.glueBoardMaterialCode || '';
  segment.glueBoardUsageId = summary?.glueBoardUsageId;
}

function getSegmentFaiStatusMeta(segment: SourceSegment) {
  const status = String(segment.faiStatus || '').toUpperCase();
  const judgment = String(segment.faiJudgment || '').toUpperCase();
  if (!segment.faiId) return { color: 'default', text: '未提交' };
  if (status === 'COMPLETED' && judgment === 'OK') return { color: 'green', text: '检验合格' };
  if (status === 'COMPLETED' && judgment === 'NG') return { color: 'red', text: '检验不合格' };
  if (status === 'REJECTED') return { color: 'red', text: '检验退回' };
  if (status === 'CANCELED') return { color: 'default', text: '已取消' };
  if (status === 'PENDING') return { color: 'processing', text: '待检' };
  if (status === 'INSPECTING') return { color: 'processing', text: '检验中' };
  if (status === 'WAITING_QA') return { color: 'warning', text: '待复核' };
  return { color: 'blue', text: segment.faiDisplayText || status || '已送检' };
}

function getSegmentSampleButtonStatus(segment: SourceSegment) {
  const status = String(segment.faiStatus || '').toUpperCase();
  const judgment = String(segment.faiJudgment || '').toUpperCase();
  if (!segment.faiId) return 'empty';
  if (status === 'REJECTED' || status === 'CANCELED' || (status === 'COMPLETED' && judgment === 'NG')) return 'ng';
  if (status === 'COMPLETED' && judgment === 'OK') return 'ok';
  return 'waiting';
}

function getSegmentSampleButtonText(segment: SourceSegment) {
  const status = getSegmentSampleButtonStatus(segment);
  if (status === 'ok') return '检验合格';
  if (status === 'ng') return '异常/重提';
  if (status === 'waiting') return '已送检';
  return '留样送检';
}

function getSegmentInspectionResultText(segment: SourceSegment) {
  const status = getSegmentSampleButtonStatus(segment);
  if (status === 'ok') return '检验合格';
  if (status === 'ng') return '检验不合格';
  if (status === 'waiting') return getSegmentFaiStatusMeta(segment).text;
  return '';
}

function isAdhesiveInspectionRecordOk(record: AdhesiveInspectionRecord) {
  return String(record.faiStatus || '').toUpperCase() === 'COMPLETED' && String(record.faiJudgment || '').toUpperCase() === 'OK';
}

function isAdhesiveInspectionRecordAbnormal(record: AdhesiveInspectionRecord) {
  const status = String(record.faiStatus || '').toUpperCase();
  const judgment = String(record.faiJudgment || '').toUpperCase();
  return status === 'REJECTED' || status === 'CANCELED' || (status === 'COMPLETED' && judgment === 'NG');
}

function getAdhesiveInspectionRecordStatusMeta(record: AdhesiveInspectionRecord) {
  const status = String(record.faiStatus || '').toUpperCase();
  if (isAdhesiveInspectionRecordAbnormal(record)) return { color: 'error', text: status === 'CANCELED' ? '已取消' : '异常退回' };
  if (isAdhesiveInspectionRecordOk(record)) return { color: 'success', text: '已反馈' };
  if (status === 'PENDING') return { color: 'processing', text: '已送检' };
  if (status === 'INSPECTING') return { color: 'processing', text: '检验中' };
  if (status === 'WAITING_QA') return { color: 'warning', text: '待复核' };
  return { color: 'default', text: record.displayText || status || '待反馈' };
}

function getAdhesiveInspectionResultMeta(record: AdhesiveInspectionRecord) {
  const judgment = String(record.faiJudgment || '').toUpperCase();
  if (judgment === 'OK') return { color: 'success', text: '合格' };
  if (judgment === 'NG') return { color: 'error', text: '不合格' };
  return { color: 'default', text: '-' };
}

function formatAdhesiveInspectionStandard(record: AdhesiveInspectionRecord) {
  if (!record.faiStandardNo) return '-';
  return `${record.faiStandardNo}${record.faiStandardVersion ? ` / ${record.faiStandardVersion}` : ''}`;
}

function formatSegmentInspectionStandard(segment: SourceSegment) {
  if (!segment.faiStandardNo) return '-';
  return `${segment.faiStandardNo}${segment.faiStandardVersion ? ` / ${segment.faiStandardVersion}` : ''}`;
}

function viewAdhesiveInspectionRecordDetail(record: AdhesiveInspectionRecord) {
  if (!record?.faiId) return;
  faiDetailModalApi.setData({ id: record.faiId }).open();
}

function getGlueBoardStockQualityMeta(record: MesHcAdhesiveConsoleApi.GlueBoardStock | Record<string, any>) {
  const rawStatus = String(record?.qualityStatus || '').trim().toUpperCase();
  const rawResult = String(record?.latestInspectionResult || '').trim().toUpperCase();
  const statusText = rawResult || rawStatus;
  if (['NG', 'ABNORMAL', 'FAILED', 'REJECTED', 'UNQUALIFIED'].includes(statusText)
    || ['NG', 'ABNORMAL', 'FAILED', 'REJECTED', 'UNQUALIFIED'].includes(rawStatus)) {
    return { color: 'red', text: '检验NG' };
  }
  if (['OK', 'PASSED', 'QUALIFIED', 'NORMAL'].includes(statusText)
    || ['OK', 'PASSED', 'QUALIFIED', 'NORMAL'].includes(rawStatus)) {
    return { color: 'green', text: '检验OK' };
  }
  if (['WAITING', 'SUBMITTED', 'PENDING', 'INSPECTING', 'WAITING_QA'].includes(statusText)
    || ['WAITING', 'SUBMITTED', 'PENDING', 'INSPECTING', 'WAITING_QA'].includes(rawStatus)) {
    return { color: 'processing', text: '已送检待检' };
  }
  return { color: 'default', text: '未送检' };
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

function formatInspectionJudgmentText(judgment?: string, fallback = '-') {
  const value = String(judgment || '').trim().toUpperCase();
  if (value === 'OK') return '合格';
  if (value === 'NG') return '不合格';
  if (value === 'PENDING') return '待检测';
  if (value === 'WAITING' || value === 'WAITING_QA') return '待复核';
  if (value === 'INSPECTING') return '检测中';
  if (value === 'REJECTED') return '已退回';
  if (value === 'CANCELED' || value === 'CANCELLED') return '已取消';
  return value || fallback;
}

function resolveGlueBoardInspectionStatus(result?: string, inspectionId?: number) {
  const judgment = String(result || '').toUpperCase();
  if (!inspectionId) return 'PENDING';
  if (['OK', 'NG'].includes(judgment)) return 'COMPLETED';
  return 'PENDING';
}

function applyGlueBoardInspectionSummary() {
  const judgment = glueBoard.latestInspectionResult || 'PENDING';
  const faiStatus = resolveGlueBoardInspectionStatus(judgment, glueBoard.latestInspectionId);
  if (!glueBoard.latestInspectionId) {
    firstInspection.value = {
      allowReportSubmit: true,
      displayText: '当前批号未送检',
      faiApplyTime: '',
      faiId: undefined,
      faiJudgment: 'PENDING',
      faiNo: '',
      faiRejectReason: '',
      faiReturnTime: '',
      faiStandardNo: '',
      faiStandardVersion: '',
      faiStatus: 'PENDING',
      glueBoardBatchNo: glueBoard.batchNo || '',
      glueBoardMaterialCode: getActualGlueBoardMaterialCode(),
      glueBoardUsageId: glueBoard.id,
      sampleLength: undefined,
      sampleStartPosition: undefined,
      sourceReportNo: '',
    };
    return;
  }
  const sameLocalInspection = firstInspection.value.faiId
    && Number(firstInspection.value.faiId) === Number(glueBoard.latestInspectionId);
  const latestAqcSampleLength = Number(glueBoard.latestAqcTask?.sampleLength || 0);
  const latestAqcSampleStartPosition = Number(glueBoard.latestAqcTask?.sampleStartPosition || 0);
  const sampleLength =
    Number(glueBoard.aqcSampleLength || 0) ||
    latestAqcSampleLength ||
    (sameLocalInspection ? Number(firstInspection.value.sampleLength || 0) : 0);
  const sampleStartPosition =
    latestAqcSampleStartPosition ||
    Number(glueBoard.receiveStartPosition || 0) ||
    (sameLocalInspection ? Number(firstInspection.value.sampleStartPosition || 0) : 0);
  firstInspection.value = {
    allowReportSubmit: true,
    displayText: glueBoard.latestInspectionId
      ? `${glueBoard.latestInspectionNo || '-'} / ${formatInspectionJudgmentText(judgment, '待检测')}`
      : resolveFirstInspectionDisplayText(faiStatus, judgment),
    faiApplyTime: glueBoard.inspectionSubmitTime || '',
    faiId: glueBoard.latestInspectionId,
    faiJudgment: judgment,
    faiNo: glueBoard.latestInspectionNo || '',
    faiRejectReason: glueBoard.qualityLockReason || '',
    faiReturnTime: '',
    faiStandardNo: '',
    faiStandardVersion: '',
    faiStatus,
    glueBoardBatchNo: glueBoard.batchNo || '',
    glueBoardMaterialCode: getActualGlueBoardMaterialCode(),
    glueBoardUsageId: glueBoard.id,
    sampleLength: sampleLength || undefined,
    sampleStartPosition: sampleStartPosition || undefined,
    sourceReportNo: glueBoard.latestInspectionNo || '',
  };
}

function getCurrentGlueBoardFaiSegmentBatchNo() {
  return String(
    currentPlan.sourceProductionBatchNo ||
    activeTaskSegmentBatchNo.value ||
    reportForm.sourceProductionBatchNo ||
    '',
  ).trim();
}

function applyGlueBoardFaiSummary(source: Partial<MesHcAdhesiveConsoleApi.FaiSummary> | null | undefined) {
  if (!source?.faiId) {
    firstInspection.value = {
      allowReportSubmit: true,
      displayText: '当前分段未送检',
      faiApplyTime: '',
      faiId: undefined,
      faiJudgment: 'PENDING',
      faiNo: '',
      faiRejectReason: '',
      faiReturnTime: '',
      faiStandardNo: '',
      faiStandardVersion: '',
      faiStatus: 'PENDING',
      glueBoardBatchNo: glueBoard.batchNo || source?.glueBoardBatchNo || '',
      glueBoardMaterialCode: getActualGlueBoardMaterialCode(),
      glueBoardUsageId: glueBoard.id,
      sampleLength: undefined,
      sampleStartPosition: undefined,
      sourceReportNo: source?.sourceReportNo || getCurrentGlueBoardFaiSegmentBatchNo(),
    };
    return;
  }
  const judgment = source.faiJudgment || 'PENDING';
  firstInspection.value = {
    allowReportSubmit: true,
    displayText: source.displayText || `${source.faiNo || '-'} / ${formatInspectionJudgmentText(judgment, '待检测')}`,
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
    sampleLength: source.sampleLength,
    sampleStartPosition: source.sampleStartPosition,
    sourceReportNo: source.sourceReportNo || getCurrentGlueBoardFaiSegmentBatchNo(),
  };
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

function isCurrentPlanAdhesiveReport(record?: MesHcAdhesiveConsoleApi.ReportItem | null) {
  if (!record) return false;
  const currentPlanOperationId = Number(currentPlan.planOperationId || 0);
  const recordPlanOperationId = Number(record.planOperationId || 0);
  if (currentPlanOperationId && recordPlanOperationId && currentPlanOperationId !== recordPlanOperationId) return false;
  const currentPlanNo = String(currentPlan.planNo || '').trim();
  const recordPlanNo = String(record.planNo || '').trim();
  if (currentPlanNo && recordPlanNo && currentPlanNo !== recordPlanNo) return false;
  return true;
}

function getAdhesiveReportSortValue(record: MesHcAdhesiveConsoleApi.ReportItem | Record<string, any>) {
  const id = Number(record.id || 0);
  if (id > 0) return id;
  const timeValue = dayjs(record.endTime || record.recorderTime || record.startTime || record.reportDate).valueOf();
  return Number.isFinite(timeValue) ? timeValue : 0;
}

function findLatestGlueBoardReportForCurrentInspection() {
  const segmentBatchNo = getCurrentGlueBoardFaiSegmentBatchNo();
  const currentReports = reportRecords.value.filter(isCurrentPlanAdhesiveReport);
  const matchedReports = segmentBatchNo
    ? currentReports.filter((record) => isSameSegmentReportBatch(record, segmentBatchNo))
    : [];
  const activeReportMatched =
    activeRecord.value &&
    isCurrentPlanAdhesiveReport(activeRecord.value) &&
    (!segmentBatchNo || isSameSegmentReportBatch(activeRecord.value, segmentBatchNo));
  const candidates = matchedReports.length > 0
    ? matchedReports
    : activeReportMatched
      ? [activeRecord.value]
      : !segmentBatchNo && currentReports.length === 1
        ? currentReports
        : [];
  return candidates
    .filter((record) => firstText(record.glueBoardMaterialCode, record.glueBoardBatchNo))
    .sort((left, right) => getAdhesiveReportSortValue(right) - getAdhesiveReportSortValue(left))[0];
}

function getCurrentGlueBoardPrintInfo() {
  return {
    batchNo: firstText(
      glueBoard.batchNo,
      aqcForm.glueBoardBatchNo,
      glueConsumeForm.batchNo,
      glueConsumeForm.materialScanCode,
      glueStockFilter.batchNo,
    ),
    materialCode: firstText(getActualGlueBoardMaterialCode(), glueBoard.materialCode, glueStockFilter.materialCode),
    model: firstText(getActualGlueBoardModel(), getSelectedGlueBoardModel(), glueBoard.model, glueStockFilter.model),
  };
}

function resolveGlueBoardInspectionPrintInfo() {
  const report = findLatestGlueBoardReportForCurrentInspection();
  const currentGlueBoardInfo = getCurrentGlueBoardPrintInfo();
  const inspectionInfo = {
    batchNo: firstText(firstInspection.value.glueBoardBatchNo),
    materialCode: firstText(firstInspection.value.glueBoardMaterialCode),
  };
  if (!report) {
    return {
      batchNo: firstText(currentGlueBoardInfo.batchNo, inspectionInfo.batchNo),
      materialCode: firstText(currentGlueBoardInfo.materialCode, inspectionInfo.materialCode),
      model: currentGlueBoardInfo.model,
    };
  }
  const reportBatchNo = firstText(report.glueBoardBatchNo);
  const currentBatchMatchesReport =
    !reportBatchNo ||
    !currentGlueBoardInfo.batchNo ||
    normalizeAdhesiveScanBatch(reportBatchNo) === normalizeAdhesiveScanBatch(currentGlueBoardInfo.batchNo);
  return {
    batchNo: firstText(reportBatchNo, currentGlueBoardInfo.batchNo, inspectionInfo.batchNo),
    materialCode: firstText(
      report.glueBoardMaterialCode,
      currentBatchMatchesReport ? currentGlueBoardInfo.materialCode : '',
      inspectionInfo.materialCode,
    ),
    model: currentBatchMatchesReport ? currentGlueBoardInfo.model : '',
  };
}

function resolveSampleInspectionGlueBoardPrintInfo(source?: {
  glueBoardBatchNo?: string;
  glueBoardMaterialCode?: string;
  glueBoardModel?: string;
}) {
  const currentGlueBoardInfo = getCurrentGlueBoardPrintInfo();
  return {
    batchNo: firstText(source?.glueBoardBatchNo, currentGlueBoardInfo.batchNo),
    materialCode: firstText(source?.glueBoardMaterialCode, currentGlueBoardInfo.materialCode),
    model: firstText(source?.glueBoardModel, currentGlueBoardInfo.model),
  };
}

function isFirstInspectionReapplyAllowed() {
  return firstInspection.value.faiStatus === 'COMPLETED' && firstInspection.value.faiJudgment === 'NG';
}

function hasGlueBoardInspectionRecord() {
  return Boolean(glueBoard.latestInspectionId);
}

function getGlueBoardFaiMaxSampleLength() {
  const availableLength = Number(glueBoard.stockLength || 0);
  if (availableLength <= 0) return 0;
  return availableLength;
}

async function refreshGlueBoardInspectionState(options: { activateTab?: boolean; silent?: boolean } = {}) {
  if (glueBoardInspectionRefreshing) return false;
  if (!currentPlan.planOperationId && !glueBoard.id && !glueBoard.stockId) return false;
  glueBoardInspectionRefreshing = true;
  try {
    await loadGlueBoardUsage();
    if (options.activateTab) {
      activeBoardTab.value = 'GLUE_BOARD_INSPECTION';
    }
    if (!options.silent) {
      // message.success('胶板检验状态已刷新');
    }
    return true;
  } catch (error) {
    if (!options.silent) {
      message.warning(getRequestErrorMessage(error, '胶板检验状态刷新失败，请稍后重试'));
    }
    return false;
  } finally {
    glueBoardInspectionRefreshing = false;
  }
}

async function refreshGlueBoardInspectionSummary() {
  await refreshGlueBoardInspectionState({ activateTab: true });
}

function goGlueBoardInspectionTab() {
  activeBoardTab.value = 'GLUE_BOARD_INSPECTION';
}

function viewFirstInspectionDetail() {
  if (!firstInspection.value.faiId) return;
  faiDetailModalApi.setData({ id: firstInspection.value.faiId }).open();
}

async function buildGlueBoardInspectionTransferTicketPayload() {
  const inspection = firstInspection.value;
  const inspectionNo = inspection.faiNo || inspection.sourceReportNo || inspection.faiId;
  const sampleLength = inspection.sampleLength ? `${formatNumber(inspection.sampleLength)} m` : '-';
  const segmentBatchNo =
    currentPlan.sourceProductionBatchNo ||
    activeTaskSegmentBatchNo.value ||
    currentPlan.sourceBatchNo ||
    currentPlan.batchNo ||
    '-';
  const glueBoardPrintInfo = resolveGlueBoardInspectionPrintInfo();
  const glueBoardMaterialCode = glueBoardPrintInfo.materialCode || '-';
  const glueBoardBatchNo = glueBoardPrintInfo.batchNo || '-';
  const glueBoardModel = glueBoardPrintInfo.model || '-';
  const fallbackFields = [
    { label: '型号', value: currentPlan.modelCode },
    { label: '分段批次', value: segmentBatchNo },
    { label: '胶板型号', value: glueBoardModel },
    { label: '胶板批次', value: glueBoardBatchNo },
    { label: '胶板料号', value: glueBoardMaterialCode },
    { label: '送检长度', value: sampleLength },
    { label: '送检时间', value: inspection.faiApplyTime },
  ];
  const fields = await applyPrintFieldTemplate('ADHESIVE1_GLUE_BOARD_INSPECTION', fallbackFields, {
    applyTime: inspection.faiApplyTime,
    glueBoardBatchNo,
    glueBoardMaterialCode,
    glueBoardModel,
    modelCode: currentPlan.modelCode,
    processName: '粘胶1',
    sampleLength,
    segmentBatchNo,
  });
  return buildInspectionTransferTicketPayload({
    applicantName: currentUserName.value,
    applyTime: inspection.faiApplyTime,
    boardBatchNo: glueBoardBatchNo,
    fields,
    inspectionNo,
    inspectionTransferNo: inspectionNo,
    inspectionType: '胶板送检',
    materialCode: glueBoardMaterialCode,
    modelCode: currentPlan.modelCode,
    planNo: undefined,
    processName: '粘胶1',
    productionBatchNo: segmentBatchNo,
    sampleType: undefined,
    title: '胶板送检单',
  });
}

async function printGlueBoardInspectionTransferTicket() {
  const inspectionNo = String(
    firstInspection.value.faiNo || firstInspection.value.sourceReportNo || firstInspection.value.faiId || '',
  ).trim();
  if (!inspectionNo) {
    AModal.warning({
      content: '当前胶板送检还没有检验单号，无法生成送检单二维码。请先完成胶板送检后再打印。',
      title: '无法打印胶板送检单',
    });
    return;
  }
  try {
    try {
      await loadReports();
    } catch {
      // 报工刷新失败时继续使用页面已有缓存与当前胶板信息兜底，避免影响现场打印。
    }
    const result = await sendTransferTicketToPrintAgent(
      await buildGlueBoardInspectionTransferTicketPayload(),
      ADHESIVE_PRINT_AGENT_URL,
    );
    AModal.success({
      content: `胶板送检单已发送到 ${result.printerName || '配置打印机'}，数量：${result.printedCount || 1}。`,
      okText: '知道了',
      title: '打印胶板送检单',
    });
  } catch (error: any) {
    AModal.warning({
      content: `未能连接或确认本机打印服务：${error?.message || error}。请先启动工序流转单 Python 打印服务。`,
      title: '打印胶板送检单失败',
    });
  }
}

function getMotherAqcStatusMeta(_group?: SourceGroup | null) {
  const status = String(motherFaiSummary.value?.faiStatus || '').toUpperCase();
  const judgment = String(motherFaiSummary.value?.faiJudgment || '').toUpperCase();
  if (!motherFaiSummary.value?.faiId) return { color: 'default', text: '' };
  if (status === 'COMPLETED' && judgment === 'OK') return { color: 'green', text: '整体母卷已放行' };
  if (status === 'COMPLETED' && judgment === 'NG') return { color: 'red', text: '整体母卷NG' };
  if (status === 'REJECTED') return { color: 'red', text: '整体母卷已退回' };
  if (status === 'CANCELED') return { color: 'default', text: '整体母卷已取消' };
  return { color: 'processing', text: motherFaiSummary.value?.displayText || '整体母卷待检' };
}

function applyGroupAqcTask(group: SourceGroup, task?: MesHcAdhesiveConsoleApi.AqcTask | null) {
  group.aqcTaskId = task?.id;
  group.aqcStatus = task?.taskStatus || '';
  group.aqcFeedbackResult = task?.feedbackResult || '';
  group.aqcSubmitTime = task?.submitTime || '';
  group.aqcFeedbackTime = task?.feedbackTime || '';
  group.aqcFeedbackRemark = task?.feedbackRemark || '';
  group.aqcDisplayText = getMotherAqcStatusMeta(group).text;
}

function applyMotherFaiSummary(summary?: MesHcAdhesiveConsoleApi.FaiSummary | null) {
  motherFaiSummary.value = summary || null;
}

async function loadMotherFaiSummary() {
  if (!currentPlan.planId || !currentPlan.planOperationId) {
    applyMotherFaiSummary(null);
    return;
  }
  const summary = await getAdhesiveFaiSummary({
    planId: currentPlan.planId,
    planOperationId: currentPlan.planOperationId,
  });
  applyMotherFaiSummary(summary);
}

async function loadSegmentFaiSummary(segment: SourceSegment) {
  if (!currentPlan.planId || !currentPlan.planOperationId || !segment.grindingSecondDetailId) {
    applySegmentFaiSummary(segment, null);
    return;
  }
  const summary = await getAdhesiveFaiSummary({
    planId: currentPlan.planId,
    planOperationId: currentPlan.planOperationId,
    sourceGrindingSecondDetailId: segment.grindingSecondDetailId,
  });
  applySegmentFaiSummary(segment, summary);
}

async function loadAdhesiveFaiSummariesForSegments() {
  const segments = sourceGroups.value.flatMap((group) => group.segments).filter((segment) => segment.grindingSecondDetailId);
  await Promise.allSettled(segments.map((segment) => loadSegmentFaiSummary(segment)));
}

async function loadMotherAqcStatusesForGroups() {
  if (!currentPlan.planOperationId || !sourceGroups.value.length) return;
  await Promise.all(
    sourceGroups.value.map(async (group) => {
      const task = await getAdhesiveConsoleReportAqcTask({
        batchNo: group.baseBatchNo,
        planOperationId: currentPlan.planOperationId!,
      });
      applyGroupAqcTask(group, task);
    }),
  );
}

function viewSegmentSampleInspectionDetail(segment: SourceSegment) {
  if (!segment.faiId) {
    message.warning('当前分段尚未生成首检送检单');
    return;
  }
  const statusMeta = getSegmentFaiStatusMeta(segment);
  const glueBoardInfo = resolveSampleInspectionGlueBoardPrintInfo(segment);
  let detailDialog: ReturnType<typeof AModal.confirm> | undefined;
  const openFaiDetailFromInspectionDialog = () => {
    if (!segment.faiId) return;
    detailDialog?.destroy();
    nextTick(() => {
      faiDetailModalApi.setData({ id: segment.faiId }).open();
    });
  };
  detailDialog = AModal.confirm({
    cancelText: '关闭',
    content: h('div', { class: 'first-inspection-detail' }, [
      h('p', `计划号：${currentPlan.planNo || '-'}`),
      h('p', `送检分段：${segment.label || '-'}`),
      h('p', `来源批次：${segment.batchNo || '-'}`),
      h('p', `产品型号：${currentPlan.modelCode || '-'}`),
      h('p', `产品料号：${currentPlan.materialCode || '-'}`),
      h('p', `胶板料号：${glueBoardInfo.materialCode || '-'}`),
      h('p', `胶板批号：${glueBoardInfo.batchNo || '-'}`),
      h('p', `检验流转单号：${segment.faiNo || segment.faiId || '-'}`),
      h('p', `送检米：${getSegmentEffectiveInspectionSampleLength(segment) > 0 ? `${formatNumber(getSegmentEffectiveInspectionSampleLength(segment))} m` : '-'}`),
      h('p', `送检时间：${segment.faiApplyTime || '-'}`),
      h('p', `当前状态：${statusMeta.text}`),
      h('p', `反馈备注：${segment.faiRejectReason || '-'}`),
      h('div', { class: 'first-inspection-detail__actions' }, [
        h(Button, {
          disabled: !segment.faiId,
          size: 'small',
          onClick: openFaiDetailFromInspectionDialog,
        }, () => '查看详情'),
        h(Button, {
          size: 'small',
          onClick: () => printSegmentInspectionTransferTicket(segment),
        }, () => '打印检验流转单'),
      ]),
    ]),
    okButtonProps: { style: { display: 'none' } },
    title: '留样送检明细',
  });
}

async function buildSegmentInspectionTransferTicketPayload(segment: SourceSegment) {
  const inspectionNo = segment.faiNo || segment.faiId;
  const sampleLength = getSegmentEffectiveInspectionSampleLength(segment);
  const segmentBatchNo = segment.batchNo || currentPlan.sourceProductionBatchNo || currentPlan.batchNo || '-';
  const sampleLengthText = sampleLength > 0 ? `${formatNumber(sampleLength)} m` : '-';
  const glueBoardInfo = resolveSampleInspectionGlueBoardPrintInfo(segment);
  const glueBoardMaterialCode = glueBoardInfo.materialCode || '-';
  const glueBoardBatchNo = glueBoardInfo.batchNo || '-';
  const fallbackFields = [
    { label: '型号', value: currentPlan.modelCode },
    { label: '分段批次', value: segmentBatchNo },
    { label: '当前工序', value: '粘胶1' },
    { label: '送检时间', value: segment.faiApplyTime },
    { label: '胶板料号', value: glueBoardMaterialCode },
    { label: '胶板批次', value: glueBoardBatchNo },
    { label: '送检长度', value: sampleLengthText },
  ];
  const fields = await applyPrintFieldTemplate('ADHESIVE1_SAMPLE_INSPECTION', fallbackFields, {
    applyTime: segment.faiApplyTime,
    glueBoardBatchNo,
    glueBoardMaterialCode,
    modelCode: currentPlan.modelCode,
    processName: '粘胶1',
    sampleLength: sampleLengthText,
    segmentBatchNo,
  });
  return buildInspectionTransferTicketPayload({
    applicantName: currentUserName.value,
    applyTime: segment.faiApplyTime,
    boardBatchNo: glueBoardBatchNo,
    fields,
    inspectionNo,
    inspectionTransferNo: inspectionNo,
    materialCode: glueBoardMaterialCode,
    modelCode: currentPlan.modelCode,
    planNo: undefined,
    processName: '粘胶1',
    productionBatchNo: segmentBatchNo,
    sampleType: undefined,
    title: '检验流转单',
  });
}

async function printSegmentInspectionTransferTicket(segment: SourceSegment) {
  const inspectionNo = String(segment.faiNo || segment.faiId || '').trim();
  if (!inspectionNo) {
    AModal.warning({
      content: '当前留样送检记录还没有检验单号，无法生成检验流转单二维码。请先完成留样送检后再打印。',
      title: '无法打印检验流转单',
    });
    return;
  }
  try {
    const result = await sendTransferTicketToPrintAgent(
      await buildSegmentInspectionTransferTicketPayload(segment),
      ADHESIVE_PRINT_AGENT_URL,
    );
    AModal.success({
      content: `检验流转单已发送到 ${result.printerName || '配置打印机'}，数量：${result.printedCount || 1}。`,
      okText: '知道了',
      title: '打印检验流转单',
    });
  } catch (error: any) {
    AModal.warning({
      content: `未能连接或确认本机打印服务：${error?.message || error}。请先启动工序流转单 Python 打印服务。`,
      title: '打印检验流转单失败',
    });
  }
}

function findSegmentByInspectionRecord(record: AdhesiveInspectionRecord) {
  if (!record || record.kind !== 'SEGMENT') return undefined;
  for (const group of sourceGroups.value) {
    const segment = group.segments.find((item) =>
      (!!record.faiId && Number(item.faiId) === Number(record.faiId))
      || (!!record.segmentBatchNo && item.batchNo === record.segmentBatchNo)
      || (!!record.productionBatchNo && item.batchNo === record.productionBatchNo),
    );
    if (segment) return segment;
  }
  return undefined;
}

async function buildInspectionRecordTransferTicketPayload(record: AdhesiveInspectionRecord) {
  const inspectionNo = record.faiNo || record.faiId;
  const sampleLength = Number(record.sampleLength || 0);
  const segmentBatchNo = record.productionBatchNo || currentPlan.sourceProductionBatchNo || currentPlan.batchNo || '-';
  const sampleLengthText = sampleLength > 0 ? `${formatNumber(sampleLength)} m` : '-';
  const glueBoardInfo = resolveSampleInspectionGlueBoardPrintInfo(record);
  const glueBoardMaterialCode = glueBoardInfo.materialCode || '-';
  const glueBoardBatchNo = glueBoardInfo.batchNo || '-';
  const fallbackFields = [
    { label: '型号', value: currentPlan.modelCode },
    { label: '分段批次', value: segmentBatchNo },
    { label: '当前工序', value: '粘胶1' },
    { label: '送检时间', value: record.pushedAt },
    { label: '胶板料号', value: glueBoardMaterialCode },
    { label: '胶板批次', value: glueBoardBatchNo },
    { label: '送检长度', value: sampleLengthText },
  ];
  const fields = await applyPrintFieldTemplate('ADHESIVE1_SAMPLE_INSPECTION', fallbackFields, {
    applyTime: record.pushedAt,
    glueBoardBatchNo,
    glueBoardMaterialCode,
    modelCode: currentPlan.modelCode,
    processName: '粘胶1',
    sampleLength: sampleLengthText,
    segmentBatchNo,
  });
  return buildInspectionTransferTicketPayload({
    applicantName: currentUserName.value,
    applyTime: record.pushedAt,
    boardBatchNo: glueBoardBatchNo,
    fields,
    inspectionNo,
    inspectionTransferNo: inspectionNo,
    materialCode: glueBoardMaterialCode,
    modelCode: currentPlan.modelCode,
    planNo: undefined,
    processName: '粘胶1',
    productionBatchNo: segmentBatchNo,
    sampleType: undefined,
    title: '检验流转单',
  });
}

async function printAdhesiveInspectionRecordTransferTicket(record: AdhesiveInspectionRecord) {
  const segment = findSegmentByInspectionRecord(record);
  if (segment) {
    await printSegmentInspectionTransferTicket(segment);
    return;
  }
  const inspectionNo = String(record.faiNo || record.faiId || '').trim();
  if (!inspectionNo) {
    AModal.warning({
      content: '当前检验记录还没有检验单号，无法生成检验流转单二维码。请先完成送检后再打印。',
      title: '无法打印检验流转单',
    });
    return;
  }
  try {
    const result = await sendTransferTicketToPrintAgent(
      await buildInspectionRecordTransferTicketPayload(record),
      ADHESIVE_PRINT_AGENT_URL,
    );
    AModal.success({
      content: `检验流转单已发送到 ${result.printerName || '配置打印机'}，数量：${result.printedCount || 1}。`,
      okText: '知道了',
      title: '打印检验流转单',
    });
  } catch (error: any) {
    AModal.warning({
      content: `未能连接或确认本机打印服务：${error?.message || error}。请先启动工序流转单 Python 打印服务。`,
      title: '打印检验流转单失败',
    });
  }
}

function getRangeStyle(range: SourceReportRange, segment: SourceSegment) {
  const outputLength = Number(segment.outputLength || 0);
  if (outputLength <= 0) return { left: '0%', width: '0%' };
  const left = Math.min(Math.max((range.start / outputLength) * 100, 0), 100);
  const width = Math.min(Math.max(((range.end - range.start) / outputLength) * 100, 0), 100 - left);
  return { left: `${left}%`, width: `${width}%` };
}

function buildNowText() {
  return dayjs().format('YYYY-MM-DD HH:mm:ss');
}

function toDateTimeText(value: any) {
  if (!value) return '';
  if (dayjs.isDayjs(value)) return value.format('YYYY-MM-DD HH:mm:ss');
  if (typeof value === 'string') return value.trim();
  if (typeof value === 'number') return dayjs(value).format('YYYY-MM-DD HH:mm:ss');
  const innerDate = value?.$d || value?.date || value?.value;
  if (innerDate) {
    const parsedInner = dayjs(innerDate);
    if (parsedInner.isValid()) return parsedInner.format('YYYY-MM-DD HH:mm:ss');
  }
  const parsed = dayjs(value);
  return parsed.isValid() ? parsed.format('YYYY-MM-DD HH:mm:ss') : String(value);
}

function normalizeTimeOnlyText(value: string) {
  const [hour = '00', minute = '00', second = '00'] = value.split(':');
  return `${hour.padStart(2, '0')}:${minute.padStart(2, '0')}:${second.padStart(2, '0')}`;
}

function isTimeOnlyText(value: string) {
  return /^\d{1,2}:\d{2}(:\d{2})?$/.test(value.trim());
}

function isPlaceholderDateTime(value: dayjs.Dayjs) {
  return [1900, 1970].includes(value.year());
}

function shouldResetRuntimeDateTime(value?: string) {
  if (!value || value === '-') return true;
  if (isTimeOnlyText(value)) return true;
  const date = dayjs(value);
  return !date.isValid() || isPlaceholderDateTime(date) || date.format('HH:mm:ss') === '08:00:00';
}

function normalizeBizDateTime(fallbackDate: string, value: string) {
  const text = String(value || '').trim();
  if (!text) return '';
  if (isTimeOnlyText(text)) return `${fallbackDate} ${normalizeTimeOnlyText(text)}`;
  const parsed = dayjs(text);
  if (!parsed.isValid()) return text;
  if (isPlaceholderDateTime(parsed)) return `${fallbackDate} ${parsed.format('HH:mm:ss')}`;
  return parsed.format('YYYY-MM-DD HH:mm:ss');
}

function normalizeReportDateTimeText(value: any, fallbackDate = dayjs().format('YYYY-MM-DD')) {
  const text = toDateTimeText(value);
  if (!text || text === '-') return '';
  const normalized = normalizeBizDateTime(fallbackDate, text);
  if (!normalized || isTimeOnlyText(normalized)) return '';
  const parsed = dayjs(normalized);
  if (!parsed.isValid() || isPlaceholderDateTime(parsed)) return '';
  return parsed.format('YYYY-MM-DD HH:mm:ss');
}

function reportDateFromDateTime(value: any, fallbackDate = dayjs().format('YYYY-MM-DD')) {
  const normalized = normalizeReportDateTimeText(value, fallbackDate);
  return normalized ? dayjs(normalized).format('YYYY-MM-DD') : fallbackDate;
}

function firstText(...values: unknown[]) {
  for (const value of values) {
    const text = String(value ?? '').trim();
    if (text) return text;
  }
  return '';
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

function getEquipmentWorkStatusMeta(status?: string) {
  const normalized = String(status || '').toUpperCase();
  if (['PRODUCING', 'RUNNING', 'IN_PROGRESS'].includes(normalized)) return { color: 'blue', text: '生产中' };
  if (['MAINTENANCE', 'REPAIR'].includes(normalized)) return { color: 'orange', text: '维护中' };
  if (['DISABLED', 'STOPPED'].includes(normalized)) return { color: 'red', text: '停用' };
  if (['IDLE', 'FREE'].includes(normalized)) return { color: 'green', text: '空闲' };
  return { color: 'default', text: status || '未记录' };
}

function normalizeEquipmentId(value?: number | string) {
  const numberValue = Number(value);
  return Number.isFinite(numberValue) && numberValue > 0 ? numberValue : undefined;
}

function hasBoardEquipmentValue() {
  return Boolean(boardEquipment.id || boardEquipment.code || boardEquipment.name);
}

const selectedBoardEquipmentId = computed(() => boardEquipment.id ?? currentPlan.equipmentId);
const selectedBoardEquipmentCode = computed(() => boardEquipment.code || currentPlan.equipmentCode || '');
const selectedBoardEquipmentName = computed(() => boardEquipment.name || currentPlan.equipmentName || '');
const selectedBoardWorkCenterName = computed(() => boardEquipment.workCenterName || currentPlan.workCenterName || '-');
const adhesiveTaskListTitle = computed(() =>
  `粘胶工作列表 - ${selectedBoardEquipmentCode.value || '未绑定'} / ${selectedBoardEquipmentName.value || '-'}`,
);

function getTaskEquipment(task: Partial<MesHcAdhesiveConsoleApi.TaskItem> | Record<string, any> = {}) {
  return {
    equipmentCode: String(task.equipmentCode || ''),
    equipmentId: normalizeEquipmentId(task.equipmentId),
    equipmentName: String(task.equipmentName || ''),
    workCenterId: normalizeEquipmentId(task.workCenterId),
    workCenterName: String(task.workCenterName || ''),
  };
}

function getEffectiveBoardEquipment(context: Partial<MesHcAdhesiveConsoleApi.TaskItem> | CurrentPlan | Record<string, any> = {}) {
  const taskEquipment = getTaskEquipment(context);
  return {
    equipmentCode: boardEquipment.code || taskEquipment.equipmentCode || currentPlan.equipmentCode || '',
    equipmentId: boardEquipment.id ?? taskEquipment.equipmentId ?? currentPlan.equipmentId,
    equipmentName: boardEquipment.name || taskEquipment.equipmentName || currentPlan.equipmentName || '',
    workCenterId: boardEquipment.workCenterId ?? taskEquipment.workCenterId ?? currentPlan.workCenterId,
    workCenterName: boardEquipment.workCenterName || taskEquipment.workCenterName || currentPlan.workCenterName || '',
  };
}

function applyBoardEquipment(equipment: Partial<BoardEquipment>, options: { manual?: boolean } = {}) {
  boardEquipment.id = normalizeEquipmentId(equipment.id);
  boardEquipment.code = String(equipment.code || '');
  boardEquipment.name = String(equipment.name || '');
  boardEquipment.workCenterId = normalizeEquipmentId(equipment.workCenterId);
  boardEquipment.workCenterName = String(equipment.workCenterName || '');
  boardEquipment.workStatus = String(equipment.workStatus || '');
  if (options.manual) {
    boardEquipmentManualSelected.value = true;
  }
}

function applyBoardEquipmentFromTask(task: Partial<MesHcAdhesiveConsoleApi.TaskItem> | Record<string, any>, options: { force?: boolean } = {}) {
  if (!options.force && boardEquipmentManualSelected.value && hasBoardEquipmentValue()) return;
  const equipment = getTaskEquipment(task);
  if (!equipment.equipmentId && !equipment.equipmentCode && !equipment.equipmentName) return;
  applyBoardEquipment({
    code: equipment.equipmentCode,
    id: equipment.equipmentId,
    name: equipment.equipmentName,
    workCenterId: equipment.workCenterId,
    workCenterName: equipment.workCenterName,
  });
}

function applyBoardEquipmentFromLedger(equipment: MesHcEquipmentApi.Equipment) {
  applyBoardEquipment({
    code: equipment.equipmentCode || '',
    id: equipment.id,
    name: equipment.equipmentName || '',
    workCenterId: equipment.workCenterId,
    workCenterName: equipment.workCenterName || '',
    workStatus: equipment.workStatus || '',
  }, { manual: true });
}

function isTaskMatchedBoardEquipment(task: MesHcAdhesiveConsoleApi.TaskItem | Record<string, any>) {
  const selectedId = selectedBoardEquipmentId.value;
  const selectedCode = String(selectedBoardEquipmentCode.value || '').trim();
  if (!selectedId && !selectedCode) return true;
  const taskEquipment = getTaskEquipment(task);
  if (selectedId && taskEquipment.equipmentId) return selectedId === taskEquipment.equipmentId;
  if (selectedCode && taskEquipment.equipmentCode) return selectedCode === taskEquipment.equipmentCode;
  return false;
}

function ensureBoardEquipmentFromTasks(rows: MesHcAdhesiveConsoleApi.TaskItem[]) {
  if (hasBoardEquipmentValue()) return;
  const candidates = rows.filter((row) => row.equipmentId || row.equipmentCode || row.equipmentName);
  const candidate =
    candidates.find((row) => normalizeWorkOrderStatus(row.status) === 'RUNNING') ||
    candidates.find((row) => normalizeWorkOrderStatus(row.status) === 'PENDING') ||
    candidates[0];
  if (candidate) {
    applyBoardEquipmentFromTask(candidate, { force: true });
    return;
  }
  const workCenterCandidate =
    rows.find((row) => normalizeWorkOrderStatus(row.status) === 'RUNNING' && (row.workCenterId || row.workCenterName)) ||
    rows.find((row) => normalizeWorkOrderStatus(row.status) === 'PENDING' && (row.workCenterId || row.workCenterName)) ||
    rows.find((row) => row.workCenterId || row.workCenterName);
  if (workCenterCandidate) {
    boardEquipment.workCenterId = normalizeEquipmentId(workCenterCandidate.workCenterId);
    boardEquipment.workCenterName = String(workCenterCandidate.workCenterName || '');
  }
}

function getDailyRecordContext() {
  if (currentPlan.planId && currentPlan.planOperationId) {
    return {
      equipmentCode: currentPlan.equipmentCode,
      equipmentId: currentPlan.equipmentId,
      equipmentName: currentPlan.equipmentName,
      planId: currentPlan.planId,
      planOperationId: currentPlan.planOperationId,
      workCenterId: currentPlan.workCenterId,
      workCenterName: currentPlan.workCenterName,
    };
  }
  const matchedTask =
    taskRows.value.find((row) => row.planId && row.planOperationId && isTaskMatchedBoardEquipment(row)) ||
    taskRows.value.find((row) => row.planId && row.planOperationId);
  if (matchedTask && !hasBoardEquipmentValue()) {
    applyBoardEquipmentFromTask(matchedTask, { force: true });
  }
  return matchedTask;
}

function syncCurrentPlanEquipmentFromBoard(context: Partial<MesHcAdhesiveConsoleApi.TaskItem> | Record<string, any> = {}) {
  const equipment = getEffectiveBoardEquipment(context);
  currentPlan.equipmentCode = equipment.equipmentCode;
  currentPlan.equipmentId = equipment.equipmentId;
  currentPlan.equipmentName = equipment.equipmentName;
  currentPlan.workCenterId = equipment.workCenterId ?? currentPlan.workCenterId;
  currentPlan.workCenterName = equipment.workCenterName || currentPlan.workCenterName;
}

function containsTaskFilterText(values: unknown[], keyword: string) {
  const text = String(keyword || '').trim().toLowerCase();
  if (!text) return true;
  return values.some((value) => String(value ?? '').toLowerCase().includes(text));
}

const taskListFilteredRows = computed(() => {
  const statusFilter = taskListFilters.status;
  return taskRows.value.filter((row) => {
    const motherBatchNo = resolveMotherBatchNo(row);
    const segmentBatchNo = resolveTaskSegmentBatchNo(row);
    if (!segmentBatchNo && !motherBatchNo) return false;
    if (!isTaskMatchedBoardEquipment(row)) return false;
    const status = normalizeWorkOrderStatus(row?.status);
    if (statusFilter === 'UNFINISHED' && status === 'FINISHED') return false;
    if (statusFilter && statusFilter !== 'ALL' && statusFilter !== 'UNFINISHED' && status !== statusFilter) return false;
    if (!containsTaskFilterText([row?.planNo, row?.id], taskListFilters.planNo)) return false;
    if (!containsTaskFilterText([row?.motherModelCode, row?.modelCode, row?.motherMaterialCode, row?.materialCode], taskListFilters.modelCode)) {
      return false;
    }
    return containsTaskFilterText(
      [
        row?.batchNo,
        row?.parentProductionBatchNo,
        row?.productionBatchNo,
        row?.sourceMotherBatchNo,
        row?.sourceBatchNo,
        row?.sourceProductionBatchNo,
        segmentBatchNo,
        motherBatchNo,
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

function clampColumnWidth(width: number, min: number, max: number) {
  return Math.min(max, Math.max(min, Math.ceil(width)));
}

function getTaskListColumnWidth(column: (typeof taskListBaseColumns)[number]) {
  const dataIndex = String(column.dataIndex);
  const rule = taskListColumnWidthRules[dataIndex] || { max: 220, min: 100, padding: 32 };
  const titleWidth = getTaskListTextVisualWidth(column.title) + rule.padding;
  const contentWidth = taskListFilteredRows.value.reduce((max, row) => {
    const getter = taskListColumnTextGetters[dataIndex];
    return Math.max(max, getTaskListTextVisualWidth(getter ? getter(row) : (row as Record<string, any>)[dataIndex]) + rule.padding);
  }, 0);
  return clampColumnWidth(Math.max(titleWidth, contentWidth), rule.min, rule.max);
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
  () => taskListFilters.status,
  () => {
    if (taskListVisible.value) {
      void loadTaskList();
    }
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
const activeSourceSegment = computed(() => getActiveSourceSegment());
const isCurrentSegmentFinished = computed(() => isSegmentCompleted(activeSourceSegment.value));
const isCurrentTaskReadonly = computed(
  () => isWorkOrderFinished.value || isCurrentSegmentFinished.value || isWorkOrderPaused.value || isWorkOrderCancelled.value,
);
const reportFormReadonly = computed(() => reportReadonly.value || isCurrentTaskReadonly.value);
const reportTimeReadonly = computed(
  () => (reportReadonly.value && !reportTimeEditing.value) || (!reportReadonly.value && isCurrentTaskReadonly.value),
);
const canEditReadonlyReportTime = computed(
  () => reportReadonly.value && !reportDetailViewOnly.value && !!activeReportDetailRecordId.value,
);
const intermediateConfirmed = computed(() => String(intermediateForm.recordStatus || '').toUpperCase() === 'CONFIRMED');
const intermediateReadonly = computed(
  () => intermediateMode.value === 'view' || isCurrentTaskReadonly.value || intermediateConfirmed.value,
);
const intermediateCanConfirm = computed(
  () => intermediateMode.value === 'edit' && !intermediateConfirmed.value && !isCurrentTaskReadonly.value,
);

function showReadonlyTaskWarning(actionName = '继续操作') {
  if (isWorkOrderPaused.value) {
    message.warning(`当前粘胶1工序已暂停，请等待复工后再${actionName}`);
    return;
  }
  if (isWorkOrderCancelled.value) {
    message.warning(`当前粘胶1工序已作废取消，不能${actionName}`);
    return;
  }
  const subject = isWorkOrderFinished.value ? '当前粘胶工单' : '当前粘胶分段';
  message.warning(`${subject}已完工，仅可查看，不能${actionName}`);
}

function buildWorkOrderBlockedReason(status = currentPlan.status) {
  const normalized = normalizeWorkOrderStatus(status);
  const batchNo = currentPlan.sourceProductionBatchNo || currentPlan.batchNo || currentPlan.sourceBatchNo || '-';
  if (normalized === 'PAUSED') {
    return `当前粘胶1工序已暂停，计划 ${currentPlan.planNo || '-'}，批号 ${batchNo} 暂不能继续报工或提交操作，请等待生产计划复工指令。`;
  }
  if (normalized === 'CANCELLED') {
    return `当前粘胶1工序已作废取消，计划 ${currentPlan.planNo || '-'}，批号 ${batchNo} 不能继续报工或提交操作。`;
  }
  return '';
}

const workbenchBlockedReason = computed(() => buildWorkOrderBlockedReason());
const workbenchBlockedTitle = computed(() =>
  isWorkOrderCancelled.value ? '当前粘胶1工序已作废取消' : '当前粘胶1工序已暂停',
);

function isDailyPreparationRecordReady(row: DailyRecordRow | Record<string, any>) {
  return String(row?.status || '').toUpperCase() !== 'PENDING';
}

const incompleteDailyPreparationNames = computed(() =>
  dailyRecordRows.value
    .filter((row) => !isDailyPreparationRecordReady(row))
    .map((row) => row.name)
    .filter(Boolean),
);
const dailyPreparationReady = computed(() =>
  dailyRecordRows.value.length > 0 && incompleteDailyPreparationNames.value.length === 0,
);

function showDailyPreparationRequiredWarning() {
  const names = incompleteDailyPreparationNames.value.join('、') || '今日点检/清洁记录';
  AModal.warning({
    okText: '去填写',
    title: '请先完成今日点检/清洁记录',
    content: `粘胶报工前必须先完成：${names}。`,
    onOk: () => {
      void openDailyRecordList();
    },
  });
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
  return /[PQRS]$/.test(source) ? source.slice(0, -1) : source;
}

function splitSourceTokens(value?: unknown) {
  return String(value ?? '')
    .split(PLAN_SCAN_SEPARATOR_REGEXP)
    .map((item) => item.trim())
    .filter(Boolean);
}

function firstSourceToken(value?: unknown) {
  return splitSourceTokens(value)[0] || '';
}

function isSameText(left?: unknown, right?: unknown) {
  const leftText = String(left ?? '').trim();
  const rightText = String(right ?? '').trim();
  return !!leftText && !!rightText && leftText.toUpperCase() === rightText.toUpperCase();
}

function resolveMotherBatchNo(row: Record<string, any>) {
  return (
    row.sourceMotherBatchNo ||
    row.sourceBatchNo ||
    row.parentProductionBatchNo ||
    row.motherBatchNo ||
    stripSegmentMark(row.sourceProductionBatchNo || row.productionBatchNo || row.batchNo) ||
    ''
  );
}

function resolveTaskSegmentBatchNo(row: Record<string, any>) {
  return (
    firstSourceToken(row.sourceProductionBatchNo) ||
    firstSourceToken(row.sourceBatchNo) ||
    firstSourceToken(row.productionBatchNo) ||
    firstSourceToken(row.batchNo) ||
    ''
  );
}

function matchesTaskSegmentBatch(row: Record<string, any>, segmentBatchNo?: string) {
  const target = String(segmentBatchNo || '').trim();
  if (!target) return true;
  return [
    row.sourceProductionBatchNo,
    row.sourceBatchNo,
    row.productionBatchNo,
    row.batchNo,
  ].some((value) => splitSourceTokens(value).some((token) => isSameText(token, target)));
}

function resolveTaskSelectionSegmentBatchNo(task: Record<string, any>) {
  return resolveTaskSegmentBatchNo(task);
}

function getTaskListRowKey(task: Record<string, any>) {
  const segmentBatchNo = resolveTaskSelectionSegmentBatchNo(task);
  return [
    task.planOperationId || task.id || task.planNo || 'task',
    task.sourceMode || 'SELF',
    segmentBatchNo || task.sourceDetailIds || task.inventoryLockIds || task.batchNo || 'segment',
    task.sourceDetailIds || '',
    task.inventoryLockIds || '',
  ].join('-');
}

function findTaskByPlanAndSegment(
  rows: Array<MesHcAdhesiveConsoleApi.TaskItem | Record<string, any>>,
  planNo: string,
  segmentBatchNo?: string,
) {
  const planRows = rows.filter((row) => !planNo || isSameText(row.planNo, planNo));
  if (planNo && !planRows.length) return undefined;
  if (segmentBatchNo) {
    const matched = planRows.find((row) => matchesTaskSegmentBatch(row, segmentBatchNo));
    return matched;
  }
  return planRows[0] || rows[0];
}

function isTaskMatchedSegment(row: Record<string, any>, segment: SourceSegment) {
  if (segment.batchNo && matchesTaskSegmentBatch(row, segment.batchNo)) return true;
  const detailId = segment.grindingSecondDetailId === undefined || segment.grindingSecondDetailId === null
    ? ''
    : String(segment.grindingSecondDetailId);
  return !!detailId && splitSourceTokens(row.sourceDetailIds).includes(detailId);
}

function markTaskListSegmentCompleted(segment: SourceSegment) {
  taskRows.value = taskRows.value.map((row) => {
    if (!isSameText(row.planNo, currentPlan.planNo) || !isTaskMatchedSegment(row, segment)) {
      return row;
    }
    return {
      ...row,
      availableSourceLength: 0,
      status: 'COMPLETED',
    };
  });
}

function rememberActiveTaskSource(task?: MesHcAdhesiveConsoleApi.TaskItem | Record<string, any>, segmentBatchNo?: string) {
  activeTaskSegmentBatchNo.value = segmentBatchNo || resolveTaskSegmentBatchNo(task || {}) || '';
  activeTaskSourceDetailIds.value = splitSourceTokens(task?.sourceDetailIds);
  activeTaskInventoryLockIds.value = splitSourceTokens(task?.inventoryLockIds);
}

function sourceMatchesActiveTask(source: MesHcAdhesiveConsoleApi.SourceItem) {
  const segmentBatchNo = activeTaskSegmentBatchNo.value;
  const sourceBatchNo = source.productionBatchNo || source.confirmedBatchNo || '';
  if (segmentBatchNo) {
    return matchesTaskSegmentBatch({ sourceProductionBatchNo: sourceBatchNo }, segmentBatchNo);
  }
  const detailId = source.grindingSecondDetailId === undefined || source.grindingSecondDetailId === null
    ? ''
    : String(source.grindingSecondDetailId);
  if (detailId && activeTaskSourceDetailIds.value.includes(detailId)) {
    return true;
  }
  const lockId = source.inventoryLockId === undefined || source.inventoryLockId === null ? '' : String(source.inventoryLockId);
  if (lockId && activeTaskInventoryLockIds.value.includes(lockId)) {
    return true;
  }
  return !segmentBatchNo && !activeTaskSourceDetailIds.value.length && !activeTaskInventoryLockIds.value.length;
}

function isGlueBoardMaterialItem(itemCategory: string, itemName: string) {
  return itemCategory.trim() === '胶板' && itemName.trim().includes('胶板料号');
}

function isGlueBoardBatchItem(itemCategory: string, itemName: string) {
  return itemCategory.trim() === '胶板' && (itemName.trim().includes('胶板批号') || itemName.trim().includes('胶板批次'));
}

function getDefaultCheckActualValue(item: Pick<AdhesiveCheckItem, 'itemCategory' | 'itemName'>) {
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
        { category: '设备点检', item: '模温机', node: '开工前', remark: '', result: 'OK', seq: 1, standard: '制热正常，无报警信息', value: '' },
        { category: '设备点检', item: '传动电机', node: '开工前', remark: '', result: 'OK', seq: 2, standard: '运行正常，无异响', value: '' },
        { category: '设备点检', item: '收膜启停', node: '开工前', remark: '', result: 'OK', seq: 3, standard: '运行正常，无异响', value: '' },
        { category: '设备点检', item: '收膜张力调节', node: '开工前', remark: '', result: 'OK', seq: 4, standard: '', value: '' },
        { category: '设备点检', item: '压合启停', node: '开工前', remark: '', result: 'OK', seq: 5, standard: '运行正常，无异响', value: '' },
        { category: '设备点检', item: '压合闭合', node: '开工前', remark: '', result: 'OK', seq: 6, standard: '运行正常，无异响', value: '' },
        { category: '设备点检', item: '压合打开', node: '开工前', remark: '', result: 'OK', seq: 7, standard: '运行正常，无异响', value: '' },
        { category: '设备点检', item: '升降丝杆', node: '开工前', remark: '', result: 'OK', seq: 8, standard: '运行正常，无异响', value: '' },
        { category: '设备点检', item: '镜面辊', node: '开工前', remark: '', result: 'OK', seq: 9, standard: '运转正常，无异响', value: '' },
        { category: '设备点检', item: '橡胶轴', node: '开工前', remark: '', result: 'OK', seq: 10, standard: '运转正常，无异响', value: '' },
      ],
      formCode: 'ADHESIVE_STARTUP_CHECK',
      key: 'ADHESIVE_STARTUP_CHECK',
      name: 'CMP软垫粘胶1开机点检表',
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
        { category: '设备清洁', item: '操作台', node: '开工前', remark: '', result: 'OK', seq: 1, standard: '表面无脏污，未放置与生产无关物品', value: '' },
        { category: '设备清洁', item: '模温机操作面板', node: '开工前', remark: '', result: 'OK', seq: 2, standard: '表面清洁无脏污和异物', value: '' },
        { category: '设备清洁', item: '磁粉刹车', node: '开工前', remark: '', result: 'OK', seq: 3, standard: '表面清洁无脏污和异物', value: '' },
        { category: '设备清洁', item: '放卷辊', node: '开工前', remark: '', result: 'OK', seq: 4, standard: '表面清洁无脏污和异物', value: '' },
        { category: '设备清洁', item: '前三轮', node: '开工前', remark: '', result: 'OK', seq: 5, standard: '表面清洁无脏污和异物', value: '' },
        { category: '设备清洁', item: '伺服电机', node: '开工前', remark: '', result: 'OK', seq: 6, standard: '表面清洁无脏污和异物', value: '' },
        { category: '设备清洁', item: '伺服丝杆', node: '开工前', remark: '', result: 'OK', seq: 7, standard: '表面清洁无脏污和异物', value: '' },
        { category: '设备清洁', item: '传动电机', node: '开工前', remark: '', result: 'OK', seq: 8, standard: '表面清洁无脏污和异物', value: '' },
        { category: '设备清洁', item: '齿轮电机', node: '开工前', remark: '', result: 'OK', seq: 9, standard: '表面清洁无脏污和异物', value: '' },
        { category: '设备清洁', item: '镜面辊', node: '开工前', remark: '', result: 'OK', seq: 10, standard: '表面清洁无脏污和异物', value: '' },
        { category: '设备清洁', item: '橡胶辊', node: '开工前', remark: '', result: 'OK', seq: 11, standard: '表面清洁无脏污和异物', value: '' },
        { category: '设备清洁', item: '离型膜收卷', node: '开工前', remark: '', result: 'OK', seq: 12, standard: '表面清洁无脏污和异物', value: '' },
        { category: '设备清洁', item: '收卷辊轮', node: '开工前', remark: '', result: 'OK', seq: 13, standard: '表面清洁无脏污和异物', value: '' },
        { category: '设备清洁', item: '输油管路', node: '开工前', remark: '', result: 'OK', seq: 14, standard: '无漏油/渗油', value: '' },
      ],
      formCode: 'ADHESIVE_CLEANING_CHECK',
      key: 'ADHESIVE_CLEANING_CHECK',
      name: 'CMP软垫粘胶1设备清洁点检表',
      recorder: '-',
      recorderTime: '-',
      result: '未填写',
      status: 'PENDING',
      timing: '开工前',
    },
  ];
}

function resetPlan() {
  Object.assign(currentPlan, {
    availableSourceLength: 0,
    batchNo: '',
    endTime: '',
    equipmentCode: '',
    equipmentId: undefined,
    equipmentName: '',
    materialCode: '',
    modelCode: '',
    planId: undefined,
    planNo: '',
    planOperationId: undefined,
    qtime: undefined,
    requirements: '',
    sourceBatchNo: '',
    sourceProductionBatchNo: '',
    startTime: '',
    status: '',
    workCenterId: undefined,
    workCenterName: '',
  });
  reportRecords.value = [];
  selectedReportRecordKeys.value = [];
  sourceGroups.value = [];
  activeTaskSegmentBatchNo.value = '';
  activeTaskSourceDetailIds.value = [];
  activeTaskInventoryLockIds.value = [];
  glueBoardMapCandidates.value = [];
  Object.assign(glueBoard, {
    alarm: '',
    aqcSampleLength: 0,
    availableStartPosition: 0,
    batchNo: '',
    id: undefined,
    latestAqcTask: undefined,
    latestInspectionId: undefined,
    latestInspectionNo: '',
    latestInspectionResult: '',
    lossLength: 0,
    materialCode: '',
    materialName: '',
    model: '',
    inspectionSubmitTime: '',
    qualityLockReason: '',
    qualityLockStartPosition: undefined,
    qualityStatus: '',
    receiveLength: 0,
    receiveStartPosition: 0,
    returnedLength: 0,
    returnedStartPosition: 0,
    stockLength: 0,
    stockId: undefined,
    todayUsedLength: 0,
  });
}

function applyTask(task: any, segmentBatchNo?: string) {
  const selectedSegmentBatchNo = segmentBatchNo || resolveTaskSegmentBatchNo(task);
  rememberActiveTaskSource(task, selectedSegmentBatchNo);
  const sourceProductionBatchNo = selectedSegmentBatchNo || resolveTaskSegmentBatchNo(task);
  const motherBatchNo = resolveMotherBatchNo(task);
  currentPlan.availableSourceLength = Number(task.availableSourceLength || 0);
  currentPlan.batchNo = motherBatchNo;
  currentPlan.endTime = task.endTime || '';
  currentPlan.equipmentCode = task.equipmentCode || '';
  currentPlan.equipmentId = task.equipmentId;
  currentPlan.equipmentName = task.equipmentName || '';
  currentPlan.materialCode = task.materialCode || '';
  currentPlan.modelCode = task.modelCode || '';
  currentPlan.planId = task.planId;
  currentPlan.planNo = task.planNo || '';
  currentPlan.planOperationId = task.planOperationId;
  currentPlan.qtime = task.qtime;
  currentPlan.requirements = task.requirements || task.executeRequirement || '';
  currentPlan.sourceBatchNo = motherBatchNo;
  currentPlan.sourceProductionBatchNo = sourceProductionBatchNo;
  currentPlan.startTime = task.startTime || '';
  currentPlan.status = task.status || '';
  currentPlan.workCenterId = task.workCenterId;
  currentPlan.workCenterName = task.workCenterName || '';
  applyBoardEquipmentFromTask(task);
  sourceGroups.value = [];
}

function getSegmentLabel(segmentMark?: string) {
  return segmentMark ? `${segmentMark}段` : '不分段';
}

function buildSourceGroupsFromSources(sources: MesHcAdhesiveConsoleApi.SourceItem[]) {
  const groups = new Map<string, SourceGroup>();
  sources.forEach((source) => {
    const batchNo = source.productionBatchNo || source.confirmedBatchNo || '';
    if (!batchNo) return;
    const baseBatchNo = source.motherBatchNo || source.parentProductionBatchNo || stripSegmentMark(batchNo);
    if (!baseBatchNo) return;
    const outputLength = Number(source.outputLength ?? source.processLength ?? 0);
    const reportRanges = getReportRanges(batchNo);
    const segmentReports = reportRecords.value.filter((record) => isSameSegmentReportBatch(record, batchNo));
    const usedLength = getReportUsedLength(batchNo);
    const completed = segmentReports.length > 0 && segmentReports.every((record) => isAdhesiveRecordSubmitted(record.reportStatus));
    const segment: SourceSegment = {
      availableLength: completed ? 0 : getAvailableLengthByRanges(outputLength, reportRanges),
      batchNo,
      downstreamStatus: source.downstreamStatus,
      grindingSecondDetailId: source.grindingSecondDetailId,
      label: getSegmentLabel(source.segmentMark),
      outputLength,
      qtime: source.qtime,
      reportRanges,
      segmentEndOperatorId: source.segmentEndOperatorId,
      segmentEndOperatorName: source.segmentEndOperatorName,
      segmentEndTime: source.segmentEndTime,
      segmentMark: source.segmentMark || '',
      segmentStartOperatorId: source.segmentStartOperatorId,
      segmentStartOperatorName: source.segmentStartOperatorName,
      segmentStartTime: source.segmentStartTime,
      segmentTimingId: source.segmentTimingId,
      usedLength,
    };
    const existed = groups.get(baseBatchNo);
    if (existed) {
      existed.segments.push(segment);
      existed.totalOutputLength += outputLength;
      existed.totalUsedLength += usedLength;
      existed.totalAvailableLength += segment.availableLength;
      return;
    }
    groups.set(baseBatchNo, {
      baseBatchNo,
      materialCode: currentPlan.materialCode,
      modelCode: currentPlan.modelCode,
      planNo: currentPlan.planNo,
      segments: [segment],
      totalAvailableLength: segment.availableLength,
      totalOutputLength: outputLength,
      totalUsedLength: usedLength,
    });
  });
  sourceGroups.value = Array.from(groups.values()).map((group) => ({
    ...group,
    segments: group.segments.sort((a, b) => (a.segmentMark || 'Z').localeCompare(b.segmentMark || 'Z')),
  }));
}

function getReportUsedLength(sourceProductionBatchNo: string) {
  return reportRecords.value
    .filter((item) => String(item.sourceProductionBatchNo || '') === sourceProductionBatchNo)
    .reduce((sum, item) => sum + Number(item.inputLength || item.outputLength || 0), 0);
}

async function loadCheckTemplate() {
  const rows = normalizeRows(await getAdhesiveConsoleCheckTemplate());
  checkTemplate.value = rows.map(normalizeCheckItem);
}

async function loadDailyRecords() {
  const fallbackRows = createDailyRecordFallbackRows();
  const context = getDailyRecordContext();
  if (!context?.planId || !context?.planOperationId) {
    dailyRecordRows.value = dailyRecordRows.value.length > 0 ? dailyRecordRows.value : fallbackRows;
    return;
  }
  const equipment = getEffectiveBoardEquipment(context);
  const rows = normalizeRows(await getAdhesiveConsolePassWorkList(
    context.planId,
    context.planOperationId,
    {
      equipmentId: equipment.equipmentId,
      recordDate: dayjs().format('YYYY-MM-DD'),
    },
  ));
  const backendRows = rows
    .map(mapPassWorkRecord)
    .filter((item) => item.name.includes('开机') || item.name.includes('清洁') || item.formCode.includes('START') || item.formCode.includes('CLEAN'));
  dailyRecordRows.value = fallbackRows.map((fallback) => backendRows.find((item) => item.formCode === fallback.formCode) || fallback);
}

async function loadReports() {
  if (!currentPlan.planOperationId) return;
  const rows = normalizeRows(await getAdhesiveConsoleReportList(currentPlan.planOperationId));
  reportRecords.value = rows;
  selectedReportRecordKeys.value = selectedReportRecordKeys.value.filter((key) =>
    rows.some((record: any) => record.id === key),
  );
}

async function loadGlueBoardMapCandidates(options: { warnMismatch?: boolean } = {}) {
  const productModelCode = String(currentPlan.modelCode || '').trim();
  if (!productModelCode) {
    glueBoardMapCandidates.value = [];
    return;
  }
  glueBoardMapLoading.value = true;
  try {
    glueBoardMapCandidates.value = normalizeRows(
      await getMatchedFinishedGlueBoardMapItems({
        glueProcess: 'ADHESIVE1',
        productModelCode,
      }),
    );
    if (options.warnMismatch) {
      const tip = glueBoardMapMismatchTip.value;
      if (tip) message.warning(tip);
    }
  } finally {
    glueBoardMapLoading.value = false;
  }
}

async function loadGlueBoardUsage() {
  const hasCurrentGlueBoard = Boolean(glueBoard.id || glueBoard.stockId || glueBoard.batchNo);
  if (!currentPlan.planOperationId && !hasCurrentGlueBoard) {
    applyGlueBoardInspectionSummary();
    applyGlueBoardDefaultsToCheckItems();
    await loadLatestGlueBoardFaiForCurrentSegment({ applyEmpty: false });
    return;
  }
  const expectedGlueBoardModel = glueBoardMapCandidateModels.value.length === 1
    ? String(glueBoardMapCandidateModels.value[0] || '').trim()
    : '';
  let usage = await getAdhesiveConsoleCurrentGlueBoardUsage(currentPlan.planOperationId, expectedGlueBoardModel);
  let source = (usage as any)?.data ?? usage;
  if (!source?.id && expectedGlueBoardModel && hasCurrentGlueBoard) {
    usage = await getAdhesiveConsoleCurrentGlueBoardUsage(currentPlan.planOperationId);
    source = (usage as any)?.data ?? usage;
  }
  const missingModelAlarm = expectedGlueBoardModel
    ? `未找到胶板型号 ${expectedGlueBoardModel} 的有效领用记录，请领用正确型号的胶板批次。`
    : '';
  const currentMismatchAlarm = glueBoardMapMismatchTip.value;
  if (!source?.id && hasCurrentGlueBoard) {
    const currentGlueBoardModel = String(glueBoard.model || '').trim();
    const currentModelMatchesExpected = !expectedGlueBoardModel
      || (currentGlueBoardModel
        && currentGlueBoardModel.toUpperCase() === expectedGlueBoardModel.toUpperCase());
    glueBoard.alarm = currentMismatchAlarm || (currentModelMatchesExpected ? '' : missingModelAlarm);
    applyGlueBoardInspectionSummary();
    applyGlueBoardDefaultsToCheckItems();
    await loadLatestGlueBoardFaiForCurrentSegment({ applyEmpty: false });
    return;
  }
  Object.assign(glueBoard, {
    alarm: source?.id
      ? source?.qualityStatus === 'ABNORMAL'
        ? source?.qualityLockReason || '胶板检验结果异常，关联报工将自动标记异常。'
        : ''
      : missingModelAlarm,
    aqcSampleLength: Number(source?.aqcSampleLength || 0),
    availableStartPosition: Number(source?.availableStartPosition || 0),
    batchNo: source?.glueBoardBatchNo || '',
    id: source?.id,
    latestAqcTask: source?.latestAqcTask,
    latestInspectionId: source?.latestInspectionId,
    latestInspectionNo: source?.latestInspectionNo || '',
    latestInspectionResult: source?.latestInspectionResult || '',
    lossLength: Number(source?.lossLength || 0),
    materialCode: source?.glueBoardMaterialCode || '',
    materialName: source?.glueBoardMaterialName || '',
    model: source?.glueBoardModel || '',
    inspectionSubmitTime: source?.inspectionSubmitTime || '',
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
  applyGlueBoardDefaultsToCheckItems();
  await loadLatestGlueBoardFaiForCurrentSegment({ applyEmpty: false });
}

async function reloadWorkbenchForLoadedPlan() {
  if (!currentPlan.planId || !currentPlan.planOperationId) return;
  await Promise.all([loadDailyRecords(), loadReports()]);
  await loadSourceGroups();
  await loadGlueBoardMapCandidates({ warnMismatch: true });
  await loadLatestGlueBoardFaiForCurrentSegment({ applyEmpty: true });
}

function resetIntermediateForm() {
  intermediateMode.value = 'edit';
  Object.assign(intermediateForm, {
    stationFormId: undefined, stationFormCode: undefined, stationFormName: undefined, thicknessLabels: undefined, formNotes: undefined,
    adhesiveReportId: undefined,
    batchNo: '',
    confirmerName: '',
    extraJson: '',
    id: undefined,
    materialCode: currentPlan.materialCode || '',
    modelCode: currentPlan.modelCode || '',
    planId: currentPlan.planId || 0,
    planNo: currentPlan.planNo || '',
    planOperationId: currentPlan.planOperationId || 0,
    processLength: 0,
    productWidthMm: undefined,
    productionDate: buildNowText(),
    recordTime: buildNowText(),
    recorderName: currentUserName.value || '',
    recordStatus: 'DRAFT',
    remark: '',
    widthEnd: undefined,
    widthMiddle: undefined,
    widthStart: undefined,
  });
  intermediateDetails.value = [];
  intermediatePage.value = 1;
}

function closeIntermediateRecord() {
  intermediateVisible.value = false;
  intermediateMode.value = 'edit';
}

function requestCloseReport() {
  if (reportTimeEditing.value) {
    reportForm.reportDate = reportTimeSnapshot.value.reportDate || reportForm.reportDate;
    reportForm.startTime = reportTimeSnapshot.value.startTime;
    reportForm.endTime = reportTimeSnapshot.value.endTime;
    reportTimeEditing.value = false;
    return;
  }
  if (reportReadonly.value) {
    reportVisible.value = false;
    reportReadonly.value = false;
    reportDetailViewOnly.value = false;
    reportTimeEditing.value = false;
    activeReportDetailRecordId.value = undefined;
    return;
  }
  AModal.confirm({
    cancelText: '继续填写',
    content: '当前报工登记尚未提交，关闭后本次扫码、工艺参数和报工收卷数据会丢失，确认关闭？',
    okText: '确认关闭',
    okType: 'danger',
    title: '确认关闭报工登记',
    onOk: () => {
      reportVisible.value = false;
      reportTimeEditing.value = false;
    },
  });
}

function clearIntermediateRecordState() {
  activeIntermediateReport.value = null;
  intermediateDetails.value = [];
  intermediatePage.value = 1;
}

async function openIntermediateRecord(record: MesHcAdhesiveConsoleApi.ReportItem | Record<string, any>, mode: 'edit' | 'view' = 'edit') {
  if (!currentPlan.planId || !currentPlan.planOperationId || !record?.id) {
    message.warning('请先选择有效的粘胶报工记录');
    return;
  }
  resetIntermediateForm();
  intermediateMode.value = mode;
  activeIntermediateReport.value = record as MesHcAdhesiveConsoleApi.ReportItem;
  intermediatePage.value = 1;
  intermediateVisible.value = true;
  intermediateLoading.value = true;
  try {
    const data = await getAdhesiveConsoleIntermediate({
      adhesiveReportId: record.id,
      batchNo: record.sourceProductionBatchNo || record.productionBatchNo || '',
      planId: currentPlan.planId,
      planOperationId: currentPlan.planOperationId,
    });
    Object.assign(intermediateForm, {
      ...data,
      adhesiveReportId: record.id,
      batchNo: data?.batchNo || record.sourceProductionBatchNo || record.productionBatchNo || '',
      materialCode: data?.materialCode || record.materialCode || currentPlan.materialCode || '',
      modelCode: data?.modelCode || record.modelCode || currentPlan.modelCode || '',
      planId: currentPlan.planId,
      planNo: currentPlan.planNo,
      planOperationId: currentPlan.planOperationId,
      processLength: data?.processLength ?? record.outputLength ?? record.inputLength ?? 0,
      productionDate: normalizeReportDateTimeText(
        data?.productionDate || record.reportDate || buildNowText(),
      ) || buildNowText(),
      recordTime: data?.recordTime || buildNowText(),
      recorderName: data?.recorderName || currentUserName.value || '',
    });
    intermediateDetails.value = (data?.details || []).map((item, index) => ({
      ...item,
      lengthMark: item.lengthMark ?? index + 1,
      sortNo: item.sortNo ?? index + 1,
    }));
  } finally {
    intermediateLoading.value = false;
  }
}

async function persistCurrentIntermediateRecord(recordStatus = 'RECORDED') {
  if (intermediateReadonly.value) {
    showReadonlyTaskWarning('保存中间品记录');
    return undefined;
  }
  if (!currentPlan.planId || !currentPlan.planOperationId || (!activeIntermediateReport.value?.id && !intermediateForm.batchNo)) {
    message.warning('中间品记录单缺少批次或报工关联，不能保存');
    return undefined;
  }
  const recordId = await saveAdhesiveConsoleIntermediate({
    ...intermediateForm,
    adhesiveReportId: activeIntermediateReport.value?.id,
    batchNo: intermediateForm.batchNo || activeIntermediateReport.value?.sourceProductionBatchNo || activeIntermediateReport.value?.productionBatchNo || '',
    details: intermediateDetails.value,
    planId: currentPlan.planId,
    planNo: currentPlan.planNo,
    planOperationId: currentPlan.planOperationId,
    processLength: intermediateForm.processLength || activeIntermediateReport.value?.outputLength || activeIntermediateReport.value?.inputLength || 0,
    recordStatus,
  });
  intermediateForm.id = recordId;
  intermediateForm.recordStatus = recordStatus;
  return recordId;
}

async function saveIntermediateRecord() {
  intermediateLoading.value = true;
  try {
    const recordId = await persistCurrentIntermediateRecord('RECORDED');
    if (!recordId) return;
    await loadReports();
    intermediateVisible.value = false;
    message.success('粘胶中间品记录单已保存');
  } finally {
    intermediateLoading.value = false;
  }
}

async function confirmIntermediateRecord(userInfo?: any) {
  if (intermediateReadonly.value) {
    showReadonlyTaskWarning('确认中间品记录');
    return;
  }
  if (!intermediateDetails.value.length) {
    message.warning('中间品记录单明细不能为空');
    return;
  }
  const now = buildNowText();
  const confirmerName = userInfo?.empName || userInfo?.nickname || userInfo?.username || userInfo?.empNo || intermediateForm.confirmerName || currentUserName.value || '';
  intermediateLoading.value = true;
  try {
    const savedRecordId = intermediateForm.id || (await persistCurrentIntermediateRecord('RECORDED'));
    if (!savedRecordId) return;
    const extra = parseJsonObject(intermediateForm.extraJson);
    extra.confirmer = confirmerName;
    extra.confirmerName = confirmerName;
    extra.confirmerTime = now;
    extra.docStatus = 'CONFIRMED';
    extra.recordStatus = 'CONFIRMED';
    const recordId = await confirmAdhesiveConsoleIntermediate({
      ...intermediateForm,
      id: savedRecordId,
      confirmerName,
      confirmerTime: now,
      details: intermediateDetails.value,
      extraJson: JSON.stringify(extra),
      recordStatus: 'CONFIRMED',
    });
    const refreshed = await getAdhesiveConsoleIntermediateById(recordId || intermediateForm.id!);
    Object.assign(intermediateForm, {
      ...refreshed,
      confirmerName: refreshed?.confirmerName || confirmerName,
      confirmerTime: refreshed?.confirmerTime || now,
      recordStatus: refreshed?.recordStatus || 'CONFIRMED',
    });
    intermediateDetails.value = (refreshed?.details || intermediateDetails.value).map((item, index) => ({
      ...item,
      lengthMark: item.lengthMark ?? index + 1,
      sortNo: item.sortNo ?? index + 1,
    }));
    intermediateMode.value = 'view';
    await loadReports();
    message.success('中间品记录单已确认');
  } finally {
    intermediateLoading.value = false;
  }
}

function openIntermediateConfirmAuth() {
  if (intermediateReadonly.value) {
    showReadonlyTaskWarning('确认中间品记录');
    return;
  }
  if (!intermediateDetails.value.length) {
    message.warning('中间品记录单明细不能为空');
    return;
  }
  intermediateAuthAction.value = '确认中间品记录单';
  intermediateAuthVisible.value = true;
}

async function handleIntermediateAuthSuccess(userInfo: any) {
  intermediateAuthVisible.value = false;
  await confirmIntermediateRecord(userInfo);
}

async function openSegmentIntermediateRecord(group: SourceGroup, segment: SourceSegment) {
  if (!currentPlan.planId || !currentPlan.planOperationId) {
    message.warning('请先扫码带出粘胶1计划');
    return;
  }
  const viewOnly = isSegmentCompleted(segment);
  if (!viewOnly && isCurrentTaskReadonly.value) {
    showReadonlyTaskWarning('新建中间品记录');
    return;
  }
  const processLength = getSegmentIntermediateProcessLength(segment);
  if (!viewOnly && processLength <= 0) {
    message.warning('当前分段没有可初始化的中间品长度');
    return;
  }
  resetIntermediateForm();
  intermediateMode.value = viewOnly ? 'view' : 'edit';
  activeIntermediateReport.value = null;
  intermediatePage.value = 1;
  intermediateVisible.value = true;
  intermediateLoading.value = true;
  try {
    const data = await getAdhesiveConsoleIntermediate({
      batchNo: segment.batchNo,
      planId: currentPlan.planId,
      planOperationId: currentPlan.planOperationId,
    });
    if (viewOnly && !data?.id) {
      intermediateVisible.value = false;
      message.info(`分段 ${segment.batchNo} 暂无已保存的中间品记录`);
      return;
    }
    Object.assign(intermediateForm, {
      ...data,
      batchNo: data?.batchNo || segment.batchNo,
      materialCode: data?.materialCode || group.materialCode || currentPlan.materialCode || '',
      modelCode: data?.modelCode || group.modelCode || currentPlan.modelCode || '',
      planId: currentPlan.planId,
      planNo: currentPlan.planNo,
      planOperationId: currentPlan.planOperationId,
      processLength: data?.id ? data?.processLength ?? processLength : processLength,
      productionDate: normalizeReportDateTimeText(data?.productionDate || buildNowText()) || buildNowText(),
      recordTime: data?.recordTime || buildNowText(),
      recorderName: data?.recorderName || currentUserName.value || '',
    });
    const rows = (data?.id ? data?.details || [] : []).map((item, index) => ({
      ...item,
      lengthMark: item.lengthMark ?? index + 1,
      sortNo: item.sortNo ?? index + 1,
    }));
    intermediateDetails.value = viewOnly || rows.length ? rows : buildDefaultIntermediateDetails(processLength);
  } catch (error) {
    intermediateVisible.value = false;
    message.error(getRequestErrorMessage(error, '中间品记录加载失败，请稍后重试'));
  } finally {
    intermediateLoading.value = false;
  }
}

async function submitSegmentFai(group: SourceGroup, segment: SourceSegment) {
  if (isCurrentTaskReadonly.value) {
    showReadonlyTaskWarning('留样送检');
    return;
  }
  if (!currentPlan.planId || !currentPlan.planOperationId) {
    message.warning('请先扫码带出粘胶1计划');
    return;
  }
  if (!segment.batchNo) {
    message.warning('当前分段批号为空，不能提交 FAI 首检申请');
    return;
  }
  if (!(await ensureAdhesiveSampleAbnormalUnlocked(segment, { actionName: '留样送检', motherBatchNo: group.baseBatchNo }))) {
    return;
  }
  if (!segment.grindingSecondDetailId) {
    message.warning('当前分段缺少来源二磨明细，不能提交 FAI 首检申请');
    return;
  }
  const status = String(segment.faiStatus || '').toUpperCase();
  const judgment = String(segment.faiJudgment || '').toUpperCase();
  const isReapply = status === 'REJECTED' || status === 'CANCELED' || (status === 'COMPLETED' && judgment === 'NG');
  try {
    const reportSampleLength = getSegmentReportNapSampleLength(segment);
    const sampleLength = reportSampleLength > 0 ? reportSampleLength : await confirmSegmentInspectionSampleLength(segment);
    if (!sampleLength) return;
    const summary = await applyAdhesiveFai({
      glueBoardBatchNo: glueBoard.batchNo || undefined,
      glueBoardMaterialCode: getActualGlueBoardMaterialCode() || glueBoard.materialCode || undefined,
      glueBoardUsageId: glueBoard.id,
      planId: currentPlan.planId,
      planOperationId: currentPlan.planOperationId,
      remark: isReapply ? '粘胶1分段留样退回/NG 后重新提交' : '粘胶1分段留样送检',
      sampleLength,
      sourceGrindingSecondDetailId: segment.grindingSecondDetailId,
      standardMatchMode: 'PRODUCT_MODEL_PROCESS',
      submitterName: currentUserName.value || undefined,
      triggerReason: isReapply ? 'REWORK_RECHECK' : 'NEW_ORDER',
    });
    applySegmentFaiSummary(segment, summary);
    message.success(`${group.baseBatchNo} ${segment.label} FAI 首检申请已提交`);
  } catch (error) {
    AModal.warning({
      title: 'FAI 首检申请未提交',
      content: getRequestErrorMessage(error, '当前分段不满足 FAI 首检申请条件，请检查工单、来源分段和已有 FAI 状态。'),
    });
  }
}

async function handleSegmentSampleInspection(group: SourceGroup, segment: SourceSegment) {
  if (isCurrentTaskReadonly.value) {
    if (segment.faiId) {
      viewSegmentSampleInspectionDetail(segment);
      return;
    }
    showReadonlyTaskWarning('留样送检');
    return;
  }
  if (segment.faiId && getSegmentSampleButtonStatus(segment) !== 'ng') {
    viewSegmentSampleInspectionDetail(segment);
    return;
  }
  await submitSegmentFai(group, segment);
}

type IntermediateImportAttachment = {
  name?: string;
  path?: string;
  size?: number;
  type?: string;
  uploadTime?: string;
  url?: string;
};

function getIntermediateExportRows() {
  return intermediateDetails.value.length
    ? intermediateDetails.value
    : buildDefaultIntermediateDetails(Number(intermediateForm.processLength || 0));
}

function buildIntermediateExcelHeaderItems(): MesHcProcessFormApi.LayoutHeaderItem[] {
  return [
    { editable: false, label: '计划号', value: intermediateForm.planNo || currentPlan.planNo || '' },
    { editable: false, label: '当前工序', value: '粘胶1' },
    { bindField: 'headerData', bindKey: 'modelCode', editable: true, label: '型号', value: intermediateForm.modelCode || '' },
    { bindField: 'headerData', bindKey: 'materialCode', editable: true, label: '料号', value: intermediateForm.materialCode || '' },
    { bindField: 'headerData', bindKey: 'batchNo', editable: true, label: '批号', value: intermediateForm.batchNo || '' },
    { bindField: 'headerData', bindKey: 'processLength', editable: true, label: '加工米数/m', value: intermediateForm.processLength == null ? '' : String(intermediateForm.processLength) },
    { bindField: 'headerData', bindKey: 'productWidthMm', editable: true, label: '产品宽幅mm', value: intermediateForm.productWidthMm == null ? '' : String(intermediateForm.productWidthMm) },
    { bindField: 'headerData', bindKey: 'productionDate', editable: true, label: '生产日期', value: intermediateForm.productionDate || '' },
    { bindField: 'headerData', bindKey: 'widthStart', editable: true, label: '开头宽幅', value: intermediateForm.widthStart == null ? '' : String(intermediateForm.widthStart) },
    { bindField: 'headerData', bindKey: 'widthMiddle', editable: true, label: '中间宽幅', value: intermediateForm.widthMiddle == null ? '' : String(intermediateForm.widthMiddle) },
    { bindField: 'headerData', bindKey: 'widthEnd', editable: true, label: '结尾宽幅', value: intermediateForm.widthEnd == null ? '' : String(intermediateForm.widthEnd) },
    { bindField: 'headerData', bindKey: 'recorderName', editable: true, label: '记录人', value: intermediateForm.recorderName || '' },
    { bindField: 'headerData', bindKey: 'recordTime', editable: true, label: '记录时间', value: intermediateForm.recordTime || '' },
    { bindField: 'headerData', bindKey: 'confirmerName', editable: true, label: '确认人', value: intermediateForm.confirmerName || '' },
  ];
}

function buildIntermediateExcelLayout(): MesHcProcessFormApi.LayoutExcelReq {
  return {
    columns: toRoughExcelColumns(adhesiveIntermediateExcelColumns.value),
    detailTitle: '中间品记录明细',
    fileName: `${intermediateForm.planNo || currentPlan.planNo || '粘胶1'}_${intermediateDisplayName.value}.xlsx`.replace(/[\\/:*?"<>|]/gu, '_'),
    headerItems: buildIntermediateExcelHeaderItems(),
      importStopPrefixes: ['填写要求：', '修订信息：', '版权声明：'],
    footerNotes: buildAdhesiveIntermediateFooterNotes(intermediateForm.formNotes),
    rows: getIntermediateExportRows().map((row, index) => ({
      cells: adhesiveIntermediateExcelColumns.value.map((column, colIndex) =>
        createRoughLayoutCell(colIndex, (row as any)[column.key], {
          bindField: column.bindField,
          bindKey: String(index),
          editable: true,
        }),
      ),
    })),
    sheetName: '粘胶1中间品记录',
    title: intermediateDisplayName.value,
    visualMode: 'adhesive1-middle',
  };
}

function normalizeIntermediateImportValue(value: any) {
  const text = String(value ?? '').trim();
  if (!text && text !== '0') return undefined;
  const numberValue = Number(text);
  return Number.isFinite(numberValue) ? numberValue : (text as any);
}

function applyIntermediateImportedHeader(key: string, rawValue: unknown) {
  const value = String(rawValue ?? '').trim();
  if (!value && value !== '0') return false;
  if (['processLength', 'productWidthMm', 'widthStart', 'widthMiddle', 'widthEnd'].includes(key)) {
    (intermediateForm as Record<string, any>)[key] = normalizeIntermediateImportValue(value);
    return true;
  }
  if (['batchNo', 'confirmerName', 'materialCode', 'modelCode', 'productionDate', 'recordTime', 'recorderName'].includes(key)) {
    (intermediateForm as Record<string, any>)[key] = value;
    return true;
  }
  return false;
}

function applyImportedIntermediateExcel(resp: MesHcProcessFormApi.LayoutImportResp) {
  let appliedCount = 0;
  (resp.headerValues || []).forEach((item) => {
    if (item.bindField !== 'headerData' || !item.bindKey) return;
    if (applyIntermediateImportedHeader(item.bindKey, item.value)) {
      appliedCount += 1;
    }
  });
  const rowMap = new Map<number, Record<string, string>>();
  (resp.cellValues || []).forEach((item) => {
    const index = Number(item.bodyRowIndex ?? item.bindKey);
    if (!Number.isInteger(index) || index < 0 || !item.bindField) return;
    if (!['leftThickness', 'lengthMark', 'remark', 'rightThickness'].includes(item.bindField)) return;
    const value = String(item.value ?? '').trim();
    if (!value && value !== '0') return;
    const row = rowMap.get(index) || {};
    row[item.bindField] = value;
    rowMap.set(index, row);
    appliedCount += 1;
  });
  if (rowMap.size > 0) {
    const indexes = [...rowMap.keys()].sort((a, b) => a - b);
    intermediateDetails.value = indexes.map((sourceIndex, targetIndex) => {
      const imported = rowMap.get(sourceIndex) || {};
      const base = intermediateDetails.value[sourceIndex] || intermediateDetails.value[targetIndex] || {};
      return {
        ...base,
        leftThickness: normalizeIntermediateImportValue(imported.leftThickness ?? base.leftThickness),
        lengthMark: normalizeIntermediateImportValue(imported.lengthMark ?? base.lengthMark ?? targetIndex + 1),
        remark: imported.remark ?? base.remark ?? '',
        rightThickness: normalizeIntermediateImportValue(imported.rightThickness ?? base.rightThickness),
        sortNo: targetIndex + 1,
      };
    });
    intermediatePage.value = 1;
  }
  return appliedCount;
}

async function persistIntermediateRecord(extraJson?: string) {
  if (!currentPlan.planId || !currentPlan.planOperationId || !intermediateForm.batchNo) {
    message.warning('中间品记录单缺少计划或批次，不能保存');
    return undefined;
  }
  const recordId = await saveAdhesiveConsoleIntermediate({
    ...intermediateForm,
    adhesiveReportId: activeIntermediateReport.value?.id,
    batchNo: intermediateForm.batchNo || activeIntermediateReport.value?.sourceProductionBatchNo || activeIntermediateReport.value?.productionBatchNo || '',
    details: intermediateDetails.value,
    extraJson: extraJson ?? intermediateForm.extraJson,
    planId: currentPlan.planId,
    planNo: currentPlan.planNo,
    planOperationId: currentPlan.planOperationId,
    processLength: intermediateForm.processLength || activeIntermediateReport.value?.outputLength || activeIntermediateReport.value?.inputLength || 0,
    recordStatus: 'RECORDED',
  });
  intermediateForm.id = recordId;
  intermediateForm.recordStatus = 'RECORDED';
  if (extraJson !== undefined) {
    intermediateForm.extraJson = extraJson;
  }
  return recordId;
}

async function handleIntermediateExport() {
  const layout = buildIntermediateExcelLayout();
  const data = await exportProcessFormRecordLayout(layout);
  downloadFileFromBlobPart({ fileName: layout.fileName || `${intermediateDisplayName.value}.xlsx`, source: data });
}

function handleIntermediateImportClick() {
  if (intermediateReadonly.value) {
    showReadonlyTaskWarning('导入中间品记录');
    return;
  }
  if (!currentPlan.planId || !currentPlan.planOperationId || !intermediateForm.batchNo) {
    message.warning('中间品记录单缺少计划或批次，不能导入');
    return;
  }
  intermediateImportInputRef.value?.click();
}

function addIntermediateDetailRow() {
  if (intermediateReadonly.value) {
    showReadonlyTaskWarning('增加中间品记录明细');
    return;
  }
  const maxSortNo = Math.max(
    0,
    ...intermediateDetails.value
      .map((row, index) => Number(row.sortNo || index + 1))
      .filter((value) => Number.isFinite(value)),
  );
  const maxLengthMark = Math.max(
    0,
    ...intermediateDetails.value
      .map((row) => Number(row.lengthMark || 0))
      .filter((value) => Number.isFinite(value)),
  );
  const nextSortNo = maxSortNo + 1;
  const nextLengthMark = maxLengthMark > 0 ? Number((maxLengthMark + 1).toFixed(3)) : nextSortNo;
  intermediateDetails.value.push(createIntermediateDetailRow(nextSortNo, nextLengthMark));
  intermediatePage.value = Math.max(1, Math.ceil(intermediateDetails.value.length / INTERMEDIATE_PAGE_SIZE));
  nextTick(() => {
    const inputs = Array.from(
      document.querySelectorAll<HTMLInputElement>('.adhesive-intermediate-cell input, input.adhesive-intermediate-cell'),
    );
    const lastRowLengthInput = inputs[Math.max(0, inputs.length - 4)];
    lastRowLengthInput?.focus();
    lastRowLengthInput?.select?.();
  });
}

function parseJsonObject(value?: string) {
  if (!value) return {} as Record<string, any>;
  try {
    const parsed = JSON.parse(value);
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? (parsed as Record<string, any>) : {};
  } catch {
    return {};
  }
}

function normalizeIntermediateAttachments(value?: IntermediateImportAttachment | IntermediateImportAttachment[] | unknown) {
  const list = Array.isArray(value) ? value : value ? [value] : [];
  return list
    .map((item, index) => {
      if (!item || typeof item !== 'object') return null;
      const raw = item as IntermediateImportAttachment & Record<string, any>;
      const path = String(raw.path || raw.filePath || '');
      const url = String(raw.url || raw.fileUrl || path || '');
      const name = String(raw.name || raw.fileName || url.split('/').pop() || `附件${index + 1}`);
      if (!url) return null;
      return { name, path: path || undefined, size: Number(raw.size) || undefined, type: raw.type, uploadTime: raw.uploadTime, url };
    })
    .filter(Boolean)
    .slice(-1) as IntermediateImportAttachment[];
}

const intermediateImportAttachment = computed(() => {
  const extra = parseJsonObject(intermediateForm.extraJson);
  return normalizeIntermediateAttachments(extra.attachments || extra.importAttachment)[0];
});

function buildIntermediateImportedAttachment(file: File, uploaded: any): IntermediateImportAttachment {
  const url = typeof uploaded === 'string' ? uploaded : uploaded?.url;
  return {
    name: uploaded?.name || file.name,
    path: uploaded?.path,
    size: uploaded?.size || file.size,
    type: uploaded?.type || file.type,
    uploadTime: dayjs().format('YYYY-MM-DD HH:mm:ss'),
    url: url || uploaded?.path,
  };
}

function buildIntermediateExtraWithAttachment(attachment: IntermediateImportAttachment, sourceExtra?: string) {
  const extra = parseJsonObject(sourceExtra);
  extra.attachments = [attachment];
  extra.importAttachment = attachment;
  extra.importTime = attachment.uploadTime;
  return JSON.stringify(extra);
}

function formatIntermediateAttachmentSize(size?: number) {
  if (!size || size <= 0) return '';
  if (size < 1024) return `${size} B`;
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`;
  return `${(size / 1024 / 1024).toFixed(1)} MB`;
}

function openIntermediateAttachment(attachment?: IntermediateImportAttachment) {
  const url = String(attachment?.url || attachment?.path || '').trim();
  if (url) window.open(url, '_blank');
}

async function handleIntermediateImportFile(event: Event) {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  input.value = '';
  if (!file) return;
  if (!/\.xls[xm]?$/iu.test(file.name)) {
    message.warning('请选择 Excel 文件');
    return;
  }
  intermediateLoading.value = true;
  try {
    const layout = buildIntermediateExcelLayout();
    const resp = await importProcessFormRecordLayout(file, layout);
    const appliedCount = applyImportedIntermediateExcel(resp);
    const uploaded = await uploadFile({
      directory: 'mes/adhesive1-intermediate',
      file,
    });
    const attachment = buildIntermediateImportedAttachment(file, uploaded);
    const extraJson = buildIntermediateExtraWithAttachment(attachment, intermediateForm.extraJson);
    const recordId = await persistIntermediateRecord(extraJson);
    if (!recordId) return;
    const data = await getAdhesiveConsoleIntermediate({
      batchNo: intermediateForm.batchNo,
      planId: currentPlan.planId || 0,
      planOperationId: currentPlan.planOperationId || 0,
    });
    Object.assign(intermediateForm, {
      ...data,
      extraJson,
      id: recordId,
      recordStatus: 'RECORDED',
    });
    intermediateDetails.value = (data?.details?.length ? data.details : intermediateDetails.value).map((item, index) => ({
      ...item,
      lengthMark: item.lengthMark ?? index + 1,
      sortNo: item.sortNo ?? index + 1,
    }));
    message.success(`已导入 ${appliedCount} 个有值单元格，并挂接原始附件`);
  } finally {
    intermediateLoading.value = false;
  }
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
  const segmentBatchNo = normalizePlanScanSegmentBatchNo(value);
  const planNo = syncPlanScanNo(value);
  const scannerInput = hasPlanScanDelimiter(value) || pendingScannerPlanNo === planNo;
  if (
    !planNo ||
    (!scannerInput && !isPlanNoReadyForAutoScan(planNo)) ||
    (planNo === lastAutoScannedPlanNo.value && (!segmentBatchNo || isSameText(segmentBatchNo, activeTaskSegmentBatchNo.value)))
  ) return;
  planScanTimer = setTimeout(() => {
    if (planNo !== normalizePlanScanNo(scanPlanNo.value)) {
      if (pendingScannerPlanNo === planNo) pendingScannerPlanNo = '';
      if (segmentBatchNo && pendingScannerSegmentBatchNo === segmentBatchNo) pendingScannerSegmentBatchNo = '';
      return;
    }
    void consumePlanScan(true);
  }, 260);
}

async function loadTaskList() {
  taskListLoading.value = true;
  try {
    const equipment = getEffectiveBoardEquipment();
    taskRows.value = normalizeRows(await getAdhesiveConsoleTaskList({
      equipmentCode: equipment.equipmentId ? undefined : equipment.equipmentCode || undefined,
      equipmentId: equipment.equipmentId,
      taskStatus: taskListFilters.status as MesHcAdhesiveConsoleApi.TaskQuery['taskStatus'],
    }));
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
    if (!workCenterId && !workCenterName) {
      equipmentSelectRows.value = [];
      message.warning('当前粘胶1任务未带出工作中心，不能展示全厂设备；请先维护粘胶1工序工作中心或机台。');
      return;
    }
    const baseParams = {
      pageNo: 1,
      pageSize: 200,
      status: 0,
      ...(workCenterId ? { workCenterId } : { workCenterName }),
    };
    let rows: MesHcEquipmentApi.Equipment[] = [];
    if (keyword) {
      rows = normalizeRows(await getEquipmentPage({ ...baseParams, equipmentCode: keyword }));
      if (rows.length === 0) {
        rows = normalizeRows(await getEquipmentPage({ ...baseParams, equipmentName: keyword }));
      }
    } else {
      rows = normalizeRows(await getEquipmentPage(baseParams));
    }
    if (rows.length === 0) {
      message.warning('当前工作中心下未找到可用设备，请检查设备台账的工作中心和启用状态。');
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

async function openDailyRecordList() {
  if (!selectedBoardEquipmentId.value && !selectedBoardEquipmentCode.value) {
    openDailyRecordAfterEquipmentSelected.value = true;
    message.warning('请先绑定粘胶机台，再进入今日点检/清洁记录。');
    await openEquipmentSelect();
    return;
  }
  await loadDailyRecords();
  dailyRecordListVisible.value = true;
}

async function selectBoardEquipment(equipment: MesHcEquipmentApi.Equipment) {
  const target = {
    equipmentCode: equipment.equipmentCode || '',
    equipmentId: equipment.id,
    equipmentName: equipment.equipmentName || '',
    workCenterId: equipment.workCenterId,
    workCenterName: equipment.workCenterName || '',
  };
  const shouldSwitchRunningTask =
    currentPlan.planId &&
    currentPlan.planOperationId &&
    isWorkOrderRunning.value &&
    (target.equipmentId !== currentPlan.equipmentId || target.equipmentCode !== currentPlan.equipmentCode);
  equipmentSelectLoading.value = true;
  try {
    if (shouldSwitchRunningTask) {
      await switchAdhesiveConsoleWorkOrderEquipment({
        equipmentCode: target.equipmentCode || undefined,
        equipmentId: target.equipmentId,
        equipmentName: target.equipmentName || undefined,
        planId: currentPlan.planId!,
        planOperationId: currentPlan.planOperationId!,
      });
    }
    applyBoardEquipmentFromLedger(equipment);
    if (currentPlan.planId && currentPlan.planOperationId) {
      syncCurrentPlanEquipmentFromBoard(target);
    }
    await loadTaskList();
    await loadDailyRecords();
    const shouldOpenDailyRecord = openDailyRecordAfterEquipmentSelected.value;
    equipmentSelectVisible.value = false;
    openDailyRecordAfterEquipmentSelected.value = false;
    if (shouldOpenDailyRecord) {
      dailyRecordListVisible.value = true;
    }
    taskListPage.value = 1;
    // message.success(shouldSwitchRunningTask ? '粘胶机台已切换，点检清洁状态已重新加载' : '已切换看板机台并按设备过滤计划');
  } finally {
    equipmentSelectLoading.value = false;
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

async function startTaskFromList(task: MesHcAdhesiveConsoleApi.TaskItem | Record<string, any>) {
  if (!task?.planOperationId) return;
  const selectedSegmentBatchNo = resolveTaskSelectionSegmentBatchNo(task);
  const taskStatus = normalizeWorkOrderStatus(task.status);
  const readonlyBlockedTask = taskStatus === 'PAUSED' || taskStatus === 'CANCELLED';
  if (taskStatus !== 'RUNNING' && taskStatus !== 'FINISHED' && !readonlyBlockedTask) {
    if (
      !(await ensureAdhesiveSampleAbnormalUnlocked(selectedSegmentBatchNo, {
        actionName: '开工',
        motherBatchNo: resolveMotherBatchNo(task),
      }))
    ) {
      return;
    }
  }
  clearPlanScannerState();
  applyTask(task, selectedSegmentBatchNo);
  const equipment = getEffectiveBoardEquipment(task);
  syncCurrentPlanEquipmentFromBoard(task);
  await loadGlueBoardMapCandidates({ warnMismatch: taskStatus !== 'FINISHED' });
  await loadGlueBoardUsage();
  lastAutoScannedPlanNo.value = task.planNo || scanPlanNo.value.trim();
  if (taskStatus !== 'RUNNING') {
    if (taskStatus !== 'FINISHED' && !readonlyBlockedTask) {
      if (!equipment.equipmentId && !equipment.equipmentCode) {
        message.warning('请先在看板顶部选择粘胶机台，再开工加载计划');
        return;
      }
      try {
        await startAdhesiveConsoleWorkOrder({
          equipmentCode: equipment.equipmentCode || undefined,
          equipmentId: equipment.equipmentId,
          equipmentName: equipment.equipmentName || undefined,
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
  }
  await Promise.all([loadDailyRecords(), loadReports()]);
  await loadSourceGroups();
  await loadLatestGlueBoardFaiForCurrentSegment({ applyEmpty: true, includeGlueBoard: false });
  taskListVisible.value = false;
  if (taskStatus === 'FINISHED') {
    // message.success('已加载已完工粘胶工单，仅可查看');
    return;
  }
  if (readonlyBlockedTask) {
    return;
  }
  if (!dailyPreparationReady.value) {
    dailyRecordListVisible.value = true;
    // message.warning('已加载粘胶计划；请先完成今日点检/清洁记录后再报工');
  } else {
    // message.success('已开工加载粘胶计划与可加工来源');
  }
}

function getActiveSourceSegment() {
  const segmentBatchNo = activeTaskSegmentBatchNo.value || currentPlan.sourceProductionBatchNo;
  if (segmentBatchNo) {
    const matched = findSourceSegment(segmentBatchNo);
    if (matched) return matched;
  }
  const segments = sourceGroups.value.flatMap((group) => group.segments);
  return segments.length === 1 ? segments[0] : undefined;
}

async function refreshActiveSourceSegment(segment: SourceSegment) {
  await loadReports();
  await loadSourceGroups();
  return findSourceSegment(segment.batchNo) || segment;
}

async function handleSegmentComplete() {
  if (!currentPlan.planId || !currentPlan.planOperationId) {
    message.warning('请先扫码或从待加工列表选择粘胶工单');
    return;
  }
  if (isWorkOrderFinished.value) {
    message.warning('当前粘胶工单已完工，不能继续标记分段完成');
    return;
  }
  if (!isWorkOrderRunning.value) {
    message.warning('当前粘胶工单未开工，不能执行本段完成');
    return;
  }
  let segment = getActiveSourceSegment();
  if (!segment) {
    message.warning('请先选择或扫码当前分段批号');
    return;
  }
  segment = await refreshActiveSourceSegment(segment);
  if (isSegmentCompleted(segment)) {
    message.warning(`当前分段 ${segment.batchNo} 已完成`);
    return;
  }
  if (!(await ensureAdhesiveSampleAbnormalUnlocked(segment, { actionName: '本段完成' }))) {
    return;
  }
  const records = getSegmentReportRecords(segment);
  if (!records.length) {
    message.warning(`当前分段 ${segment.batchNo} 暂无粘胶报工记录，不能本段完成`);
    return;
  }
  const unconfirmedRecords = records.filter((record) => !isAdhesiveRecordConfirmed(record.reportStatus));
  if (unconfirmedRecords.length) {
    selectedReportRecordKeys.value = unconfirmedRecords.map((record) => record.id).filter((id): id is number => !!id);
    message.warning(`当前分段 ${segment.batchNo} 存在 ${unconfirmedRecords.length} 条未扫码确认的报工记录，请先扫码确认`);
    reportRecordListVisible.value = true;
    return;
  }
  const summary = getSegmentReportSummary(segment);
  pendingSegmentCompleteSegment.value = segment;
  segmentCompleteAuthVisible.value = true;
  message.info(`当前分段 ${segment.batchNo} 共 ${records.length} 条粘胶报工，已加工 ${formatNumber(summary.processLength)} m；请认证确认本段完工。`);
}

async function handleSegmentCompleteAuthSuccess(userInfo: any) {
  const pendingSegment = pendingSegmentCompleteSegment.value;
  if (!pendingSegment) {
    message.warning('未找到待确认的分段，请重新点击本段完成');
    return;
  }
  try {
    const segment = await refreshActiveSourceSegment(pendingSegment);
    if (isSegmentCompleted(segment)) {
      message.warning(`当前分段 ${segment.batchNo} 已完成`);
      return;
    }
    if (!(await ensureAdhesiveSampleAbnormalUnlocked(segment, { actionName: '本段完成' }))) {
      return;
    }
    const records = getSegmentReportRecords(segment);
    if (!records.length) {
      message.warning(`当前分段 ${segment.batchNo} 暂无粘胶报工记录，不能本段完成`);
      return;
    }
    const unconfirmedRecords = records.filter((record) => !isAdhesiveRecordConfirmed(record.reportStatus));
    if (unconfirmedRecords.length) {
      selectedReportRecordKeys.value = unconfirmedRecords.map((record) => record.id).filter((id): id is number => !!id);
      message.warning(`当前分段 ${segment.batchNo} 存在 ${unconfirmedRecords.length} 条未扫码确认的报工记录，请先扫码确认`);
      reportRecordListVisible.value = true;
      return;
    }
    const now = buildNowText();
    const confirmerName = userInfo?.empName || userInfo?.nickname || userInfo?.username || currentUserName.value || undefined;
    await completeAdhesiveConsoleSegment({
      confirmerName,
      confirmerTime: now,
      endTime: now,
      planId: currentPlan.planId!,
      planOperationId: currentPlan.planOperationId!,
      recorderName: confirmerName,
      recorderTime: now,
      remark: `粘胶1看板本段完成：${segment.batchNo}`,
      reportDate: dayjs().format('YYYY-MM-DD'),
      sourceGrindingSecondDetailId: segment.grindingSecondDetailId,
      sourceProductionBatchNo: segment.batchNo,
    });
    await loadReports();
    await Promise.all([loadSourceGroups(), loadTaskList()]);
    markTaskListSegmentCompleted(segment);
    message.success(`当前分段 ${segment.batchNo} 已完成，认证人：${confirmerName || '-'}`);
  } catch (error) {
    message.error(getRequestErrorMessage(error, '本段完成失败，请稍后重试'));
  } finally {
    pendingSegmentCompleteSegment.value = null;
    segmentCompleteAuthVisible.value = false;
  }
}

function handleSegmentCompleteAuthCancel() {
  pendingSegmentCompleteSegment.value = null;
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

function getRecordPrintMeta(record: MesHcAdhesiveConsoleApi.ReportItem | Record<string, any>) {
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
  if (normalized === 'SUBMITTED') return { color: 'green', text: '已完工' };
  if (normalized === 'CONFIRMED') return { color: 'blue', text: '已扫码确认' };
  return { color: 'orange', text: '草稿' };
}

function getIntermediateStatusMeta(status?: string) {
  const normalized = String(status || 'DRAFT').toUpperCase();
  if (normalized === 'RECORDED') return { color: 'green', text: '已保存' };
  if (normalized === 'CONFIRMED') return { color: 'blue', text: '已确认' };
  if (normalized === 'SUBMITTED') return { color: 'green', text: '已提交' };
  return { color: 'orange', text: '草稿' };
}

function isAdhesiveRecordConfirmed(status?: string) {
  const normalized = String(status || '').toUpperCase();
  return normalized === 'CONFIRMED' || normalized === 'SUBMITTED';
}

function isAdhesiveRecordSubmitted(status?: string) {
  return String(status || '').toUpperCase() === 'SUBMITTED';
}

function canReviseStatisticsRecord(record?: MesHcAdhesiveConsoleApi.ReportItem | Record<string, any>) {
  return Boolean(record?.id && isAdhesiveRecordConfirmed(String(record.reportStatus || '')));
}

function openStatisticsRevisionModal(record?: MesHcAdhesiveConsoleApi.ReportItem | Record<string, any>) {
  if (!record?.id) {
    message.warning('请选择有效的粘胶1报工记录');
    return;
  }
  if (!canReviseStatisticsRecord(record)) {
    message.warning('只有已扫码确认或已完工的粘胶1报工记录允许修订统计数据');
    return;
  }
  statisticsRevisionRecord.value = record as MesHcAdhesiveConsoleApi.ReportItem;
  statisticsRevisionForm.inputLength = normalizeMeterNumber(record.inputLength) ?? 0;
  statisticsRevisionForm.lossLength = normalizeMeterNumber(record.lossLength) ?? 0;
  statisticsRevisionForm.outputLength = normalizeMeterNumber(record.outputLength) ?? 0;
  statisticsRevisionForm.reason = '';
  statisticsRevisionVisible.value = true;
}

function closeStatisticsRevisionModal() {
  if (statisticsRevisionSubmitting.value) return;
  statisticsRevisionVisible.value = false;
  statisticsRevisionRecord.value = null;
  statisticsRevisionForm.inputLength = null;
  statisticsRevisionForm.lossLength = null;
  statisticsRevisionForm.outputLength = null;
  statisticsRevisionForm.reason = '';
}

function hasStatisticsValueChanged(current: unknown, next: number) {
  return roundMeter(Number(current || 0)) !== roundMeter(next);
}

async function handleStatisticsRevisionConfirm() {
  const record = statisticsRevisionRecord.value;
  if (!record?.id) {
    message.warning('请选择有效的粘胶1报工记录');
    return;
  }
  const reason = statisticsRevisionForm.reason.trim();
  if (!reason) {
    message.warning('请填写修订原因');
    return;
  }
  const inputLength = normalizeMeterNumber(statisticsRevisionForm.inputLength);
  const lossLength = normalizeMeterNumber(statisticsRevisionForm.lossLength);
  const outputLength = normalizeMeterNumber(statisticsRevisionForm.outputLength);
  if (inputLength === undefined || lossLength === undefined || outputLength === undefined) {
    message.warning('统计数据必须是有效数字');
    return;
  }
  if (inputLength < 0 || lossLength < 0 || outputLength < 0) {
    message.warning('统计数据不能小于0');
    return;
  }
  if (
    !hasStatisticsValueChanged(record.inputLength, inputLength) &&
    !hasStatisticsValueChanged(record.lossLength, lossLength) &&
    !hasStatisticsValueChanged(record.outputLength, outputLength)
  ) {
    message.info('统计数据未变化，无需修订');
    return;
  }
  statisticsRevisionSubmitting.value = true;
  try {
    await reviseAdhesiveConsoleStatisticsData({
      id: Number(record.id),
      inputLength,
      lossLength,
      outputLength,
      reason,
    });
    const patch = { inputLength, lossLength, outputLength };
    Object.assign(record, patch);
    reportRecords.value = reportRecords.value.map((item) =>
      Number(item.id) === Number(record.id) ? { ...item, ...patch } : item,
    );
    if (Number(activeReportDetailRecordId.value || 0) === Number(record.id)) {
      reportForm.processLength = inputLength;
      reportForm.lossLength = lossLength;
      reportForm.outputLength = outputLength;
    }
    await loadReports();
    statisticsRevisionVisible.value = false;
    statisticsRevisionRecord.value = null;
    message.success('粘胶1统计数据已修订');
  } finally {
    statisticsRevisionSubmitting.value = false;
  }
}

function isSegmentCompleted(segment?: SourceSegment) {
  if (!segment) return false;
  const records = getSegmentReportRecords(segment);
  return records.length > 0 && records.every((record) => isAdhesiveRecordSubmitted(record.reportStatus));
}

function formatSegmentTimingClock(value?: string) {
  if (!value) return '未记录';
  const parsed = dayjs(value);
  return parsed.isValid() ? parsed.format('HH:mm:ss') : value;
}

function getSegmentTimingTitle(segment: SourceSegment, action: SegmentTimingAction) {
  const isStart = action === 'START';
  const time = isStart ? segment.segmentStartTime : segment.segmentEndTime;
  const operatorName = isStart ? segment.segmentStartOperatorName : segment.segmentEndOperatorName;
  if (!time) return `${segment.batchNo}${isStart ? '开工' : '完工'}时间尚未记录`;
  return `${isStart ? '开工' : '完工'}：${time}${operatorName ? `（${operatorName}）` : ''}`;
}

function isSegmentTimingStamping(segment: SourceSegment, action: SegmentTimingAction) {
  return segmentTimingStampingKey.value === `${segment.grindingSecondDetailId}-${action}`;
}

function syncReportFormTimingFromSegment(segment?: SourceSegment) {
  reportForm.startTime = segment?.segmentStartTime || '';
  reportForm.endTime = segment?.segmentEndTime || '';
}

function requestSegmentTimingStamp(segment: SourceSegment, action: SegmentTimingAction) {
  if (!currentPlan.planId || !currentPlan.planOperationId || !segment.grindingSecondDetailId) {
    message.warning('请先扫码带出真实粘胶1计划和 P/Q/R/S 分段');
    return;
  }
  if (!['P', 'Q', 'R', 'S'].includes(String(segment.segmentMark || '').toUpperCase())) {
    message.warning('仅支持记录 P/Q/R/S 四段的开工和完工时间');
    return;
  }
  if (!isWorkOrderRunning.value) {
    message.warning('请先完成粘胶1工序开工，再记录分段时间');
    return;
  }
  if (action === 'START' && segment.segmentStartTime) {
    message.warning(`${segment.batchNo} 已记录开工时间`);
    return;
  }
  if (action === 'END' && (!segment.segmentStartTime || segment.segmentEndTime)) {
    message.warning(segment.segmentEndTime ? `${segment.batchNo} 已记录完工时间` : `请先记录 ${segment.batchNo} 的开工时间`);
    return;
  }
  pendingSegmentTimingAction.value = { action, segment };
  segmentTimingAuthActionName.value = `${segment.batchNo}${action === 'START' ? '开工' : '完工'}时间确认`;
  segmentTimingAuthVisible.value = true;
}

async function stampSegmentTiming(segment: SourceSegment, action: SegmentTimingAction, authPayload: any) {
  if (!currentPlan.planId || !currentPlan.planOperationId || !segment.grindingSecondDetailId) return;
  const operatorId = authPayload?.userId ?? authPayload?.id;
  const operatorName = authPayload?.empName || authPayload?.nickname || authPayload?.username || authPayload?.empNo;
  if (!operatorId || !operatorName) {
    message.warning('请先完成操作人员身份确认后再记录时间');
    return;
  }
  const key = `${segment.grindingSecondDetailId}-${action}`;
  segmentTimingStampingKey.value = key;
  try {
    await stampAdhesiveConsoleSegmentTiming({
      action,
      operatorId,
      operatorName,
      planId: currentPlan.planId,
      planOperationId: currentPlan.planOperationId,
      sourceGrindingSecondDetailId: segment.grindingSecondDetailId,
    });
    await loadReports();
    await loadSourceGroups();
    if (reportForm.sourceGrindingSecondDetailId === segment.grindingSecondDetailId) {
      syncReportFormTimingFromSegment(findSourceSegment(reportForm.sourceProductionBatchNo));
    }
    message.success(`${segment.batchNo}${action === 'START' ? '开工' : '完工'}时间已记录`);
  } finally {
    if (segmentTimingStampingKey.value === key) {
      segmentTimingStampingKey.value = '';
    }
  }
}

async function handleSegmentTimingAuthSuccess(payload: any) {
  const pending = pendingSegmentTimingAction.value;
  pendingSegmentTimingAction.value = undefined;
  if (!pending) return;
  await stampSegmentTiming(pending.segment, pending.action, payload);
}

function handleSegmentTimingAuthCancel() {
  pendingSegmentTimingAction.value = undefined;
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

function formatAdhesiveTicketLength(value?: number) {
  return `${formatNumber(Number(value || 0))} m`;
}

function getAdhesiveTicketSegmentBatchNo(record: MesHcAdhesiveConsoleApi.ReportItem | Record<string, any>) {
  return (
    String(record.sourceProductionBatchNo || '').trim() ||
    normalizeAdhesiveScanBatch(record.productionBatchNo) ||
    currentPlan.sourceProductionBatchNo ||
    '-'
  );
}

function getAdhesiveTicketReportBatchNo(record: MesHcAdhesiveConsoleApi.ReportItem | Record<string, any>) {
  return String(record.productionBatchNo || '').trim() || getAdhesiveTicketSegmentBatchNo(record);
}

function buildAdhesiveTicketQrValue(planNo?: unknown, reportBatchNo?: unknown) {
  const planText = String(planNo ?? '').trim();
  const batchText = String(reportBatchNo ?? '').trim();
  if (!planText && !batchText) return '';
  if (!planText) return batchText;
  if (!batchText) return planText;
  return `${planText},${batchText}`;
}

function formatAdhesiveReportPosition(value?: unknown) {
  const numericValue = normalizeMeterNumber(value);
  if (numericValue === undefined) return '-';
  return `${formatNumber(numericValue)} m`;
}

function formatAdhesiveReportPositionRange(record: MesHcAdhesiveConsoleApi.ReportItem | Record<string, any>) {
  const { end, start } = resolveAdhesiveReportPositionRange(record);
  return `${formatAdhesiveReportPosition(start)} - ${formatAdhesiveReportPosition(end)}`;
}

function resolveAdhesiveProcessName(record?: MesHcAdhesiveConsoleApi.ReportItem | Record<string, any>) {
  return String(
    (record as Record<string, any> | undefined)?.operationName ||
      (record as Record<string, any> | undefined)?.reportProcess ||
      (record as Record<string, any> | undefined)?.processName ||
      '粘胶1',
  );
}

async function buildAdhesiveTransferTicketItem(record: MesHcAdhesiveConsoleApi.ReportItem, now: string) {
  const planNo = record.planNo || currentPlan.planNo;
  const processName = resolveAdhesiveProcessName(record);
  const materialCode = record.materialCode || currentPlan.materialCode || '-';
  const modelCode = record.modelCode || currentPlan.modelCode || '-';
  const positionRange = resolveAdhesiveReportPositionRange(record);
  const segmentBatchNo = getAdhesiveTicketSegmentBatchNo(record);
  const reportBatchNo = getAdhesiveTicketReportBatchNo(record);
  const startTime = record.startTime || record.recorderTime || now;
  const endTime = record.endTime || record.confirmerTime || record.recorderTime || now;
  const recorderName = record.recorderName || record.confirmerName || currentUserName.value || '-';
  const glueBoardBatchNo = record.glueBoardBatchNo || '-';
  const positionRangeText = formatAdhesiveReportPositionRange(record);
  const outputLength = formatAdhesiveTicketLength(record.outputLength || record.inputLength);
  const fallbackFields = [
    { label: '当前工序', value: processName },
    { label: '料号', value: materialCode },
    { label: '型号', value: modelCode },
    { label: '产品批次', value: segmentBatchNo },
    { label: '胶板批次', value: glueBoardBatchNo },
    { label: '起止位置', value: positionRangeText },
    { label: '生产米数', value: outputLength },
  ];
  const fields = await applyPrintFieldTemplate('ADHESIVE1_TRANSFER', fallbackFields, {
    endTime,
    glueBoardBatchNo,
    materialCode,
    modelCode,
    outputLength,
    planNo,
    positionRange: positionRangeText,
    processName,
    recorderName,
    segmentBatchNo,
    startTime,
  });
  return {
    endTime,
    fields,
    materialCode,
    modelCode,
    planNo,
    processName,
    productionBatchNo: segmentBatchNo,
    qrTopText: planNo,
    qrValue: buildAdhesiveTicketQrValue(planNo, reportBatchNo),
    recorderName,
    startTime,
    ticketId: record.id,
  };
}

async function buildAdhesiveTransferTicketPayload(records: MesHcAdhesiveConsoleApi.ReportItem[]) {
  const now = buildNowText();
  const items = await Promise.all(records.map((record) => buildAdhesiveTransferTicketItem(record, now)));
  const commonPayload = {
    continueOnError: false,
    copies: 1,
    labelGapDots: 24,
    labelGapMm: 3,
    labelHeightMm: 50,
    labelWidthMm: 80,
    offsetXmm: 0,
    offsetYmm: 0,
    printerKey: 'adhesive1',
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

async function sendAdhesiveTransferTicketsToPrintAgent(records: MesHcAdhesiveConsoleApi.ReportItem[]) {
  const payload = await buildAdhesiveTransferTicketPayload(records);
  const endpoint = records.length > 1 ? '/print/transfer-tickets' : '/print/transfer-ticket';
  const response = await fetch(`${ADHESIVE_PRINT_AGENT_URL}${endpoint}`, {
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

async function markAdhesiveRecordsPrinted(records: MesHcAdhesiveConsoleApi.ReportItem[], printTime: string) {
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
  selectedReportRecordKeys.value = selectedReportRecordKeys.value.filter((key) =>
    !records.some((record) => record.id === key),
  );
}

async function openAdhesiveRecordTickets(
  records: MesHcAdhesiveConsoleApi.ReportItem[],
  options: { autoPrint?: boolean; markPrinted?: boolean } = {},
) {
  const autoPrint = options.autoPrint !== false;
  const markPrinted = options.markPrinted !== false;
  const validRows = records.filter((record) => !!record?.id && !!record.productionBatchNo);
  if (!validRows.length) {
    AModal.info({ title: '没有可打印记录', content: '请选择已有生产批次号的粘胶报工记录。' });
    return;
  }
  const printWindow = window.open('', '_blank', 'width=980,height=720');
  if (!printWindow) return;
  const now = buildNowText();
  const planDetail = currentPlan.planId ? await getPlanOrderDetail(currentPlan.planId as any) : {};
  const ticketRows = await Promise.all(
    validRows.map(async (record) => {
      const planNo = record.planNo || currentPlan.planNo;
      const segmentBatchNo = getAdhesiveTicketSegmentBatchNo(record);
      const reportBatchNo = getAdhesiveTicketReportBatchNo(record);
      return {
        confirmer: currentUserName.value || 'admin',
        currentSection: '粘胶1',
        endTime: record.endTime,
        materialCode: (planDetail as any)?.materialCode || currentPlan.materialCode,
        metricValue: Number(record.outputLength || record.inputLength || 0),
        modelCode: (planDetail as any)?.modelCode || currentPlan.modelCode,
        parentBatchNo: record.parentProductionBatchNo || segmentBatchNo,
        planNo,
        productionBatchNo: segmentBatchNo,
        qrDataUrl: await createPrintQrDataUrl(buildAdhesiveTicketQrValue(planNo, reportBatchNo)),
        recorder: currentUserName.value || 'admin',
        remark: [
          `报工批次:${reportBatchNo}`,
          `起止:${formatAdhesiveReportPositionRange(record)}`,
          `来源母批:${record.parentProductionBatchNo || '-'}`,
          `固定损耗-包含小样条:${formatNumber(record.lossLength) || '-'}`,
          `产出:${formatNumber(record.outputLength) || '-'}`,
          `送检米数:${formatNumber(record.napSampleLength) || '-'}`,
          `胶板:${record.glueBoardMaterialCode || '-'} / ${record.glueBoardBatchNo || '-'}`,
        ].join(' '),
        segmentMark: '',
        startTime: record.startTime,
      };
    }),
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
  if (autoPrint) {
    window.setTimeout(() => {
      printWindow.print();
      printWindow.close();
    }, 300);
  } else {
    message.success(`已打开${validRows.length}条粘胶工单预览`);
  }

  if (!markPrinted) return;
  await markAdhesiveRecordsPrinted(validRows, now);
  await loadReports();
}

async function printAdhesiveRecords(records: MesHcAdhesiveConsoleApi.ReportItem[]) {
  const validRows = records.filter((record) => !!record?.id && !!record.productionBatchNo);
  if (!validRows.length) {
    AModal.info({ title: '没有可打印记录', content: '请选择已有生产批次号的粘胶1报工记录。' });
    return false;
  }
  try {
    const result = await sendAdhesiveTransferTicketsToPrintAgent(validRows);
    const now = buildNowText();
    try {
      await markAdhesiveRecordsPrinted(validRows, now);
      await loadReports();
      message.success(`粘胶1流转单已发送到 ${result.printerName || '配置打印机'}，数量：${result.printedCount || validRows.length}。`);
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

async function exportAdhesiveRecords(records: MesHcAdhesiveConsoleApi.ReportItem[]) {
  await openAdhesiveRecordTickets(records, { autoPrint: false, markPrinted: false });
}

function getSegmentReportRecords(segment: SourceSegment) {
  return reportRecords.value
    .filter((record) => isSameSegmentReportBatch(record, segment.batchNo))
    .sort((left, right) => Number(right.id || 0) - Number(left.id || 0));
}

function getSegmentReportNapSampleLength(segment: SourceSegment) {
  return getSegmentReportRecords(segment).reduce((sum, record) => sum + Number(record.napSampleLength || 0), 0);
}

function getSegmentEffectiveInspectionSampleLength(segment: SourceSegment) {
  const reportSampleLength = getSegmentReportNapSampleLength(segment);
  if (reportSampleLength > 0) return reportSampleLength;
  return Number(segment.faiSampleLength || 0);
}

function getSegmentPendingInspectionSampleLength(segment: SourceSegment) {
  if (getSegmentReportRecords(segment).length > 0) return 0;
  return Number(segment.faiSampleLength || 0);
}

function confirmSegmentInspectionSampleLength(segment: SourceSegment) {
  let sampleLengthValue = Number(segment.faiSampleLength || 0) || undefined;
  return new Promise<number | undefined>((resolve) => {
    AModal.confirm({
      cancelText: '取消',
      content: h('div', { class: 'second-sample-length-confirm' }, [
        h('p', `${segment.label || segment.batchNo || '当前分段'}送检前请确认本次送检米数；无报工时会先记录，后续报工自动带入送检米数。`),
        h(InputNumber, {
          class: 'w-full',
          defaultValue: sampleLengthValue,
          min: 0.001,
          placeholder: '请输入送检米',
          precision: 3,
          step: 0.1,
          'onUpdate:value': (value: number | null) => {
            sampleLengthValue = Number(value || 0) || undefined;
          },
        }),
      ]),
      okText: '确认送检',
      onCancel: () => resolve(undefined),
      onOk: () => {
        const sampleLength = Number(sampleLengthValue || 0);
        if (!Number.isFinite(sampleLength) || sampleLength <= 0) {
          AModal.warning({
            content: '请填写大于 0 的送检米。',
            title: '送检米不能为空',
          });
          return Promise.reject(new Error('送检米不能为空'));
        }
        resolve(sampleLength);
        return undefined;
      },
      title: '确认送检米',
    });
  });
}

function getAdhesiveReportSequence(batchNo?: unknown) {
  const text = String(batchNo ?? '').trim();
  const match = text.match(/-J(\d+)$/i);
  return match ? Number(match[1]) : 0;
}

function buildNextAdhesiveReportBatchNo(segmentBatchNo?: string) {
  const baseBatchNo = normalizeAdhesiveScanBatch(segmentBatchNo);
  if (!baseBatchNo) return '';
  const relatedRecords = reportRecords.value.filter((record) =>
    normalizeAdhesiveScanBatch(record.sourceProductionBatchNo || record.productionBatchNo) === baseBatchNo,
  );
  const maxSequence = relatedRecords.reduce(
    (max, record) => Math.max(max, getAdhesiveReportSequence(record.productionBatchNo)),
    0,
  );
  return `${baseBatchNo}-J${Math.max(maxSequence, relatedRecords.length) + 1}`;
}

function getSegmentReportCount(segment: SourceSegment) {
  return getSegmentReportRecords(segment).length;
}

function getSegmentLatestReport(segment: SourceSegment) {
  return getSegmentReportRecords(segment)[0];
}

function getSegmentReportSummary(segment: SourceSegment) {
  return getSegmentReportRecords(segment).reduce(
    (summary, record) => {
      summary.processLength += Number(record.inputLength || record.outputLength || 0);
      summary.lossLength += Number(record.lossLength || 0);
      summary.outputLength += Number(record.outputLength || 0);
      summary.napSampleLength += Number(record.napSampleLength || 0);
      return summary;
    },
    {
      lossLength: 0,
      napSampleLength: 0,
      outputLength: 0,
      processLength: 0,
    },
  );
}

function getSegmentIntermediateProcessLength(segment: SourceSegment) {
  const summary = getSegmentReportSummary(segment);
  const hasReportLength =
    summary.processLength > 0 ||
    summary.lossLength > 0 ||
    summary.napSampleLength > 0 ||
    summary.outputLength > 0;
  if (hasReportLength) {
    return roundMeter(summary.outputLength);
  }
  return roundMeter(Number(segment.outputLength || segment.availableLength || 0));
}

function getSegmentScanStatusMeta(segment: SourceSegment) {
  if (isSegmentCompleted(segment)) {
    return { className: 'confirmed', text: '本段完成' };
  }
  const latestRecord = getSegmentLatestReport(segment);
  if (!latestRecord) {
    return { className: 'empty', text: '暂无报工' };
  }
  return isAdhesiveRecordConfirmed(latestRecord.reportStatus)
    ? { className: 'confirmed', text: '扫码已确认' }
    : { className: 'pending', text: '扫码未确认' };
}

function getSegmentPrintableReportRecords(segment: SourceSegment) {
  return getSegmentReportRecords(segment).filter((record) => !!record?.id && !!record.productionBatchNo);
}

function requireSegmentPrintableRecords(segment: SourceSegment) {
  const records = getSegmentPrintableReportRecords(segment);
  if (!records.length) {
    message.warning(`请先完成${segment.label}粘胶报工`);
    return [];
  }
  return records;
}

function printSegmentWorkRecord(segment: SourceSegment) {
  const records = requireSegmentPrintableRecords(segment);
  if (!records.length) return;
  if (records.length === 1) {
    void printAdhesiveRecords(records);
    return;
  }
  openSegmentPrintSelector(segment, records);
}

function exportSegmentWorkRecord(segment: SourceSegment) {
  const record = getSegmentLatestReport(segment);
  if (!record?.id || !record.productionBatchNo) {
    message.warning(`请先完成${segment.label}粘胶报工`);
    return;
  }
  void exportAdhesiveRecords([record]);
}

function openSegmentPrintSelector(segment: SourceSegment, records: MesHcAdhesiveConsoleApi.ReportItem[]) {
  segmentPrintSelectorTitle.value = `${segment.batchNo || segment.label} 流转单选择`;
  segmentPrintSelectorRows.value = records;
  segmentPrintSelectorSelectedKeys.value = records
    .map((record) => record.id)
    .filter((id): id is number => id !== undefined && id !== null);
  segmentPrintSelectorVisible.value = true;
}

function closeSegmentPrintSelector() {
  if (segmentPrintSelectorLoading.value) return;
  segmentPrintSelectorVisible.value = false;
}

async function confirmSegmentPrintSelection() {
  const records = segmentPrintSelectorSelectedRows.value;
  if (!records.length) {
    message.warning('请至少选择一条粘胶报工记录。');
    return;
  }
  segmentPrintSelectorLoading.value = true;
  try {
    const printed = await printAdhesiveRecords(records);
    if (printed) {
      segmentPrintSelectorVisible.value = false;
    }
  } finally {
    segmentPrintSelectorLoading.value = false;
  }
}

const printSelectedAdhesiveRecords = () => void printAdhesiveRecords(printableReportRecords.value);
const printAllAdhesiveRecords = () => void printAdhesiveRecords(unprintedReportRecords.value);

function normalizeAdhesiveScanCodeRaw(batchNo?: unknown) {
  return resolveTransferTicketQrBusinessNo(normalizeConfirmScanCode(String(batchNo ?? ''))).trim();
}

function normalizeAdhesiveScanBatch(batchNo?: string) {
  return normalizeAdhesiveScanCodeRaw(batchNo).replace(/-J\d+$/i, '');
}

function hasAdhesiveReportSuffix(batchNo?: unknown) {
  return /-J\d+$/i.test(normalizeAdhesiveScanCodeRaw(batchNo));
}

function isSameReportMotherBatch(left: MesHcAdhesiveConsoleApi.ReportItem | Record<string, any>, right: MesHcAdhesiveConsoleApi.ReportItem | Record<string, any>) {
  const leftMother = resolveMotherBatchNo(left);
  const rightMother = resolveMotherBatchNo(right);
  return !!leftMother && leftMother === rightMother;
}

function isReportScanBatchMatch(scannedBatchNo: string, record: MesHcAdhesiveConsoleApi.ReportItem | Record<string, any>) {
  const scannedRaw = normalizeAdhesiveScanCodeRaw(scannedBatchNo);
  const productionBatchNo = normalizeAdhesiveScanCodeRaw(record.productionBatchNo);
  if (hasAdhesiveReportSuffix(scannedRaw)) {
    return isSameText(scannedRaw, productionBatchNo);
  }
  const scanned = normalizeAdhesiveScanBatch(scannedRaw);
  const sourceProductionBatchNo = normalizeAdhesiveScanBatch(record.sourceProductionBatchNo);
  return !!scanned && (scanned === normalizeAdhesiveScanBatch(productionBatchNo) || scanned === sourceProductionBatchNo);
}

function findRecordByScannedBatch(scannedBatchNo: string) {
  const anchor = activeRecord.value;
  if (!anchor) return undefined;
  return reportRecords.value
    .filter((record) => isSameReportMotherBatch(anchor, record))
    .filter((record) => isReportScanBatchMatch(scannedBatchNo, record))
    .sort((left, right) => {
      const leftUnconfirmed = !isAdhesiveRecordConfirmed(left.reportStatus);
      const rightUnconfirmed = !isAdhesiveRecordConfirmed(right.reportStatus);
      if (leftUnconfirmed !== rightUnconfirmed) return leftUnconfirmed ? -1 : 1;
      return Number(right.id || 0) - Number(left.id || 0);
    })[0];
}

function openRecordConfirm(record: MesHcAdhesiveConsoleApi.ReportItem | Record<string, any>) {
  if (!record?.id) return;
  if (isCurrentTaskReadonly.value) {
    showReadonlyTaskWarning('扫码确认');
    return;
  }
  activeRecord.value = record as MesHcAdhesiveConsoleApi.ReportItem;
  recordConfirmForm.error = '';
  recordConfirmForm.matchedBatchNo = '';
  recordConfirmForm.message = '';
  recordConfirmForm.scannedBatchNo = '';
  recordConfirmVisible.value = true;
  focusRecordConfirmScanInput();
}

async function openSelectedRecordConfirm() {
  if (isCurrentTaskReadonly.value) {
    showReadonlyTaskWarning('扫码确认');
    return;
  }
  if (!reportRecords.value.length) {
    AModal.info({ title: '暂无记录', content: '请先追加粘胶1报工记录后再扫码确认。' });
    return;
  }
  const selectedRow =
    selectedReportRecords.value.length === 1 &&
    !isAdhesiveRecordConfirmed(selectedReportRecords.value[0]?.reportStatus)
      ? selectedReportRecords.value[0]
      : undefined;
  const row = selectedRow || reportRecords.value.find((item) => !isAdhesiveRecordConfirmed(item.reportStatus));
  if (!row) {
    AModal.info({ title: '已全部确认', content: '粘胶1报工记录均已扫码确认。' });
    return;
  }
  openRecordConfirm(row);
}

async function ensureStrictGlueBoardMatch(record?: MesHcAdhesiveConsoleApi.ReportItem) {
  try {
    const productModel = String(record?.modelCode || currentPlan.modelCode || '').trim();
    if (!record) await loadGlueBoardUsage();
    const candidates = normalizeRows(await getMatchedFinishedGlueBoardMapItems({
      glueProcess: 'ADHESIVE1', productModelCode: productModel,
    })) as MesHcFinishedGlueBoardMapApi.FinishedGlueBoardMapItem[];
    // 历史报工只预检自身料号；原卷真实型号由后端读取，不拿当前新卷替代。
    const reason = glueBoardMatchReason(productModel, candidates,
      record ? record.glueBoardMaterialCode : getActualGlueBoardMaterialCode(),
      record ? undefined : getActualGlueBoardModel(), !record);
    if (reason) { message.warning(reason); return false; }
    return true;
  } catch (error: any) {
    message.warning(error?.message || '胶板匹配校验失败，请刷新后重试');
    return false;
  }
}

async function confirmRecordScan() {
  if (!activeRecord.value?.id) return;
  if (isCurrentTaskReadonly.value) {
    showReadonlyTaskWarning('扫码确认');
    return;
  }
  const scannedBatchNo = resolveTransferTicketQrBusinessNo(normalizeConfirmScanCode(recordConfirmForm.scannedBatchNo));
  if (!scannedBatchNo) {
    recordConfirmForm.error = '请扫码或输入母卷+段的批次号。';
    recordConfirmForm.matchedBatchNo = '';
    recordConfirmForm.message = '';
    focusRecordConfirmScanInput();
    return;
  }
  const matchedRecord = findRecordByScannedBatch(scannedBatchNo);
  if (!matchedRecord?.id) {
    recordConfirmForm.error = '未在当前母卷批次下找到扫码对应的粘胶报工记录。';
    recordConfirmForm.matchedBatchNo = '';
    recordConfirmForm.message = '';
    recordConfirmForm.scannedBatchNo = '';
    focusRecordConfirmScanInput();
    return;
  }
  if (isAdhesiveRecordConfirmed(matchedRecord.reportStatus)) {
    recordConfirmForm.error = `批次 ${scannedBatchNo} 已扫码确认，无需重复确认。`;
    recordConfirmForm.message = '';
    recordConfirmForm.matchedBatchNo =
      matchedRecord.sourceProductionBatchNo ||
      normalizeAdhesiveScanBatch(matchedRecord.productionBatchNo) ||
      scannedBatchNo;
    activeRecord.value = matchedRecord;
    recordConfirmForm.scannedBatchNo = '';
    focusRecordConfirmScanInput();
    return;
  }
  if (
    !(await ensureAdhesiveSampleAbnormalUnlocked(
      matchedRecord.sourceProductionBatchNo ||
        normalizeAdhesiveScanBatch(matchedRecord.productionBatchNo) ||
        scannedBatchNo,
      {
        actionName: '扫码确认',
        motherBatchNo: matchedRecord.parentProductionBatchNo || matchedRecord.sourceBatchNo || currentPlan.batchNo,
      },
    ))
  ) {
    recordConfirmForm.scannedBatchNo = '';
    focusRecordConfirmScanInput();
    return;
  }
  if (!(await ensureStrictGlueBoardMatch(matchedRecord))) return;
  try {
    const confirmedId = await confirmAdhesiveConsoleReport({
      confirmerName: currentUserName.value || 'admin',
      confirmerTime: buildNowText(),
      id: matchedRecord.id,
      scannedBatchNo,
    });
    selectedReportRecordKeys.value = selectedReportRecordKeys.value.filter((key) => key !== confirmedId);
    await loadReports();
    await loadSourceGroups();
    const confirmedRecord = reportRecords.value.find((item) => Number(item.id) === Number(confirmedId)) || matchedRecord;
    const nextRecord = reportRecords.value.find(
      (item) => isSameReportMotherBatch(confirmedRecord, item) && !isAdhesiveRecordConfirmed(item.reportStatus),
    );
    activeRecord.value = confirmedRecord;
    recordConfirmForm.scannedBatchNo = '';
    recordConfirmForm.error = '';
    recordConfirmForm.matchedBatchNo =
      confirmedRecord.sourceProductionBatchNo ||
      normalizeAdhesiveScanBatch(confirmedRecord.productionBatchNo) ||
      scannedBatchNo;
    recordConfirmForm.message = nextRecord
      ? `已确认 ${recordConfirmForm.matchedBatchNo}，请继续扫描下一张流转单。`
      : '当前粘胶报工记录已全部确认，可关闭窗口。';
    message.success('粘胶报工记录已扫码确认，可继续扫描下一张流转单');
    focusRecordConfirmScanInput();
  } catch (error: any) {
    recordConfirmForm.error = error?.message || '未在当前母卷批次下找到扫码对应的粘胶报工记录。';
    recordConfirmForm.matchedBatchNo = '';
    recordConfirmForm.message = '';
    recordConfirmForm.scannedBatchNo = '';
    focusRecordConfirmScanInput();
  }
}

async function loadSourceGroups() {
  if (!currentPlan.planId || !currentPlan.planOperationId) {
    sourceGroups.value = [];
    return;
  }
  const allRows = normalizeRows(await getAdhesiveConsoleSourceList(currentPlan.planId, currentPlan.planOperationId));
  const rows = allRows.filter(sourceMatchesActiveTask);
  if (activeTaskSegmentBatchNo.value && allRows.length > 0 && rows.length === 0) {
    const segmentBatchNo = activeTaskSegmentBatchNo.value;
    activeTaskSegmentBatchNo.value = '';
    activeTaskSourceDetailIds.value = [];
    activeTaskInventoryLockIds.value = [];
    sourceGroups.value = [];
    currentPlan.sourceProductionBatchNo = '';
    currentPlan.availableSourceLength = 0;
    message.warning(`选中分段 ${segmentBatchNo} 不属于当前粘胶1工单，请重新选择待加工行。`);
    return;
  }
  buildSourceGroupsFromSources(rows);
  await loadMotherFaiSummary();
  await loadMotherAqcStatusesForGroups();
  await loadAdhesiveFaiSummariesForSegments();
  const firstSource = rows[0];
  const selectedSegmentBatchNo = activeTaskSegmentBatchNo.value;
  currentPlan.sourceBatchNo = firstSource?.motherBatchNo || firstSource?.parentProductionBatchNo || '';
  currentPlan.sourceProductionBatchNo =
    selectedSegmentBatchNo || firstSource?.productionBatchNo || firstSource?.confirmedBatchNo || '';
  currentPlan.batchNo = currentPlan.sourceBatchNo || stripSegmentMark(currentPlan.sourceProductionBatchNo) || '';
  currentPlan.availableSourceLength = sourceGroups.value.reduce((sum, group) => sum + group.totalAvailableLength, 0);
}

async function consumePlanScan(silent = false) {
  const scannerRequiresSegment = !!pendingScannerPlanNo || hasPlanScanDelimiter(scanPlanNo.value);
  const scannerSegmentBatchNo = pendingScannerSegmentBatchNo || normalizePlanScanSegmentBatchNo(scanPlanNo.value);
  const planNo = syncPlanScanNo(scanPlanNo.value);
  if (!planNo) {
    if (!silent) {
      message.warning('请先扫码或输入计划号');
      focusPlanScanInput();
    }
    return;
  }
  if (scannerRequiresSegment && !scannerSegmentBatchNo) {
    resetPlan();
    clearPlanScannerState();
    AModal.warning({
      content: `粘胶1扫码计划需同时包含计划号和分段批号，请确认流转单二维码内容后重新扫码。`,
      okText: '知道了',
      onOk: focusPlanScanInput,
      title: '扫码计划缺少分段批号',
    });
    return;
  }
  boardLoading.value = true;
  try {
    const rows = normalizeRows(await getAdhesiveConsoleTaskList({ taskKeyword: planNo, taskStatus: 'ALL' }));
    const task = findTaskByPlanAndSegment(rows, planNo, scannerSegmentBatchNo);
    if (!task?.planOperationId) {
      resetPlan();
      clearPlanScannerState();
      await loadDailyRecords();
      AModal.warning({
        content: scannerSegmentBatchNo
          ? `未找到计划号 ${planNo} 且分段批号 ${scannerSegmentBatchNo} 同时匹配的粘胶1工单，扫码计划框已清空，请确认后重新扫码。`
          : `未找到计划号 ${planNo} 对应的粘胶1工单，扫码计划框已清空，请确认后重新扫码。`,
        okText: '知道了',
        onOk: focusPlanScanInput,
        title: '扫码计划未找到',
      });
      return;
    }
    const taskStatus = normalizeWorkOrderStatus(task.status);
    if (taskStatus !== 'FINISHED') {
      const unlocked = await ensureAdhesiveSampleAbnormalUnlocked(
        scannerSegmentBatchNo || resolveTaskSegmentBatchNo(task),
        {
          actionName: '扫码加载',
          motherBatchNo: resolveMotherBatchNo(task),
        },
      );
      if (!unlocked) return;
    }
    applyTask(task, scannerSegmentBatchNo);
    await loadGlueBoardMapCandidates({ warnMismatch: true });
    await loadGlueBoardUsage();
    await Promise.all([loadDailyRecords(), loadReports()]);
    await loadSourceGroups();
    await loadLatestGlueBoardFaiForCurrentSegment({ applyEmpty: true, includeGlueBoard: false });
    clearPlanScannerState();
    focusPlanScanInput();
    // if (!silent) message.success('已带出粘胶计划、工作准备和可加工来源');
  } finally {
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

function getDailyRecordStatusMeta(status: DailyRecordRow['status'] | string) {
  if (status === 'COMPLETED') return { color: 'green', text: '已确认' };
  if (status === 'FILLED') return { color: 'blue', text: '待确认' };
  return { color: 'orange', text: '未填写' };
}

function canFillDailyRecord(record: DailyRecordRow | Record<string, any>) {
  return !isCurrentTaskReadonly.value && record.canFill === true;
}

function canConfirmDailyRecord(record: DailyRecordRow | Record<string, any>) {
  return !isCurrentTaskReadonly.value && record.canConfirm === true;
}

function canViewDailyRecord(record: DailyRecordRow | Record<string, any>) {
  return record.canView !== false;
}

function openDailyRecord(row: DailyRecordRow | Record<string, any>, mode: DailyRecordMode) {
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
  dailyRecordKey.value = String(row.key || '');
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
  const context = getDailyRecordContext();
  const equipment = getEffectiveBoardEquipment(context || {});
  if (!operator) {
    message.warning('请先完成记录人/确认人身份确认');
    return;
  }
  if (!context?.planId || !context?.planOperationId) {
    message.warning('请先选择粘胶机台并加载该设备对应计划后，再保存今日点检/清洁记录');
    return;
  }
  if (!equipment.equipmentId && !equipment.equipmentCode) {
    message.warning('请先在看板顶部选择粘胶机台，再保存今日点检/清洁记录');
    return;
  }
  const result = row.details.some((item) => item.result === 'NG') ? 'NG' : 'OK';
  const existingRecorder = row.recorder === '-' ? operator : row.recorder;
  const recorder = action.mode === 'edit' ? operator : existingRecorder;
  const recorderTime = action.mode === 'edit' || shouldResetRuntimeDateTime(row.recorderTime)
    ? now
    : row.recorderTime;
  const existingConfirmer = row.confirmer === '-' ? undefined : row.confirmer;
  const confirmer = action.mode === 'confirm' ? operator : existingConfirmer;
  const existingConfirmerTime = shouldResetRuntimeDateTime(row.confirmerTime) ? undefined : row.confirmerTime;
  const confirmerTime = action.mode === 'confirm'
    ? now
    : existingConfirmerTime;
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
      status: item.result,
    })),
    equipmentCode: equipment.equipmentCode || undefined,
    equipmentId: equipment.equipmentId,
    equipmentName: equipment.equipmentName || undefined,
    formCode: row.formCode,
    inspectionResult: result,
    planId: context.planId,
    planOperationId: context.planOperationId,
    recordId: row.recordId,
    recordDate: dayjs().format('YYYY-MM-DD'),
    recorder,
    recorderTime,
    result,
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

function saveDailyRecord(confirm = false) {
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
  if (!context?.planId || !context?.planOperationId) {
    message.warning('请先选择粘胶机台并加载该设备对应计划后，再保存今日点检/清洁记录');
    return;
  }
  if (!equipment.equipmentId && !equipment.equipmentCode) {
    message.warning('请先在看板顶部选择粘胶机台，再保存今日点检/清洁记录');
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
  await loadGlueBoardMapCandidates();
  const glueBoardModel = glueBoardMapCandidateModels.value.length === 1 ? glueBoardMapCandidateModels.value[0] || '' : '';
  const stock = await getAdhesiveConsoleGlueBoardStockByBatch(scanCode, glueBoardModel);
  const source = (stock as any)?.data ?? stock;
  await applyGlueBoardStock(source, scanCode);
}

async function applyGlueBoardStock(
  source: MesHcAdhesiveConsoleApi.GlueBoardStock | Record<string, any> | null | undefined,
  fallbackBatchNo = '',
) {
  const currentGlueBoardModel = String(getSelectedGlueBoardModel() || '').trim();
  if (!source?.id) {
    const lookupModels = glueBoardMapCandidateText.value;
    glueBoard.alarm = lookupModels
      ? `未找到推荐胶板 ${lookupModels}、批号 ${fallbackBatchNo} 的边库批次，请先在胶板边料管理中领料登记。`
      : '未找到该胶板边库批次，请先在胶板边料管理中领料登记。';
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
  glueBoard.materialName = source.glueBoardMaterialName || '';
  glueBoard.model = stockGlueBoardModel || currentGlueBoardModel || '';
  glueBoard.batchNo = source.glueBoardBatchNo || '';
  glueBoard.inspectionSubmitTime = source.inspectionSubmitTime || '';
  glueBoard.latestInspectionId = source.latestInspectionId;
  glueBoard.latestInspectionNo = source.latestInspectionNo || '';
  glueBoard.latestInspectionResult = source.latestInspectionResult || '';
  glueBoard.qualityStatus = source.qualityStatus || '';
  glueBoard.receiveStartPosition = Number(source.receiveStartPosition || 0);
  glueBoard.receiveLength = Number(source.receiveLength || 0);
  glueBoard.availableStartPosition = Number(source.availableStartPosition || 0);
  glueBoard.stockLength = receiveLength;
  applyGlueBoardInspectionSummary();
  applyGlueBoardDefaultsToCheckItems();
  await loadLatestGlueBoardFaiForCurrentSegment({ applyEmpty: false });
  const mismatchTip = buildGlueBoardMapMismatchTip(source);
  if (mismatchTip) {
    glueBoard.alarm = mismatchTip;
    message.warning(mismatchTip);
  } else {
    glueBoard.alarm = '';
    // message.success(`已带出胶板边库批次，本次将领用边库余量 ${formatNumber(receiveLength)} m`);
  }
  return true;
}

async function loadGlueStockRows(options: { resetPage?: boolean } = {}) {
  if (options.resetPage) {
    glueStockPagination.pageNo = 1;
  }
  const glueBoardBatchNo = String(glueStockFilter.batchNo || '').trim();
  const glueBoardMaterialCode = String(glueStockFilter.materialCode || '').trim();
  const glueBoardModel = String(glueStockFilter.model || '').trim();
  glueStockSelectLoading.value = true;
  try {
    const page = await getAdhesiveConsoleGlueBoardStockPage({
      candidateGlueBoardModels: glueBoardMapCandidateModelQuery.value || undefined,
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
  const clearBatchFilter = Boolean(
    options && typeof options === 'object' && 'clearBatchFilter' in options
      ? (options as { clearBatchFilter?: boolean }).clearBatchFilter
      : false,
  );
  await loadGlueBoardMapCandidates();
  glueStockFilter.batchNo = clearBatchFilter
    ? ''
    : String(glueConsumeForm.materialScanCode || glueConsumeForm.batchNo || '').trim();
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
  const usageResult = await saveAdhesiveConsoleGlueBoardUsage({
    equipmentCode: equipment.equipmentCode,
    equipmentId: equipment.equipmentId,
    equipmentName: equipment.equipmentName,
    glueBoardBatchNo: batchNo,
    glueBoardMaterialCode: materialCode,
    glueBoardStockId: glueConsumeForm.stockId || glueBoard.stockId,
    operationCode: 'ADHESIVE',
    operationName: '粘胶1',
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
  glueConsumeVisible.value = false;
  await reloadWorkbenchForLoadedPlan();
  message.success('胶板领用记录已保存');
  message.warning('胶板领用前100m，建议完成胶板送检。');
}

function openGlueBoardAqcDialog(_group?: Event | SourceGroup) {
  if (isCurrentTaskReadonly.value) {
    showReadonlyTaskWarning('提交胶板检验');
    return;
  }
  if (!glueBoard.stockId || !glueBoard.batchNo) {
    message.warning('请先登记胶板领用，再提交胶板检验');
    return;
  }
  if (!glueBoard.model) {
    message.warning('当前胶板缺少胶板型号，请先在胶板边库维护型号');
    return;
  }
  if (!glueBoard.materialCode) {
    message.warning('当前胶板缺少料号，请先在胶板边库维护料号');
    return;
  }
  const submittedToday = glueBoard.inspectionSubmitTime
    && dayjs(glueBoard.inspectionSubmitTime).isSame(dayjs(), 'day');
  if (submittedToday && firstInspection.value.faiId && !isFirstInspectionReapplyAllowed()) {
    message.info('当前胶板已提交检验；只有检验结果 NG 时才允许重新送检。');
    goGlueBoardInspectionTab();
    return;
  }
  aqcForm.id = glueBoard.latestInspectionId;
  aqcForm.glueBoardBatchNo = glueBoard.batchNo;
  aqcForm.sampleStartPosition = Number(glueBoard.availableStartPosition || glueBoard.receiveStartPosition || 0);
  aqcForm.sampleLength = undefined;
  aqcVisible.value = true;
}

async function submitGlueBoardAqc() {
  if (isCurrentTaskReadonly.value) {
    showReadonlyTaskWarning('提交胶板检验');
    return;
  }
  if (!glueBoard.stockId || !glueBoard.batchNo || !glueBoard.model) {
    message.warning('请先登记包含型号的胶板领用信息');
    return;
  }
  const sampleLength = Number(aqcForm.sampleLength || 0);
  if (!Number.isFinite(sampleLength) || sampleLength <= 0) {
    message.warning('请填写胶板检验送检米数');
    return;
  }
  const maxSampleLength = getGlueBoardFaiMaxSampleLength();
  if (maxSampleLength <= 0 || sampleLength > maxSampleLength) {
    message.warning(`送检米数不能超过当前边库余量 ${formatNumber(maxSampleLength)} m`);
    return;
  }
  if (aqcSubmitting.value) return;
  aqcSubmitting.value = true;
  try {
    const record = await createGlueBoardFaiRecordFromAdhesive1({
      glueBoardMaterialCode: glueBoard.materialCode || ADHESIVE_DEFAULT_GLUE_BOARD_MATERIAL_CODE,
      glueBoardMaterialName: glueBoard.materialName || glueBoard.materialCode || '胶板',
      glueBoardModel: glueBoard.model,
      glueBoardStockId: glueBoard.stockId,
      glueBoardUsageId: glueBoard.id,
      gluePlateBatchNo: glueBoard.batchNo,
      operationCode: 'GLUE_1',
      operationName: '粘胶1',
      planOrderId: currentPlan.planOperationId || undefined,
      processCategory: 'GLUE_1',
      remark: `粘胶1看板胶板检验；计划 ${currentPlan.planNo || '-'}；分段 ${getCurrentGlueBoardFaiSegmentBatchNo() || '-'}；工序任务 ${currentPlan.planOperationId || '-'}`,
      sampleLength,
      sampleStartPosition: Number(aqcForm.sampleStartPosition || 0),
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
    firstInspection.value.sampleLength = Number((record as any)?.sampleLength || sampleLength);
    firstInspection.value.sampleStartPosition = Number(aqcForm.sampleStartPosition || 0);
    aqcVisible.value = false;
    try {
      await loadGlueBoardUsage();
      await reloadWorkbenchForLoadedPlan();
    } catch {
      message.warning('胶板检验已提交，但边库状态刷新失败，请手动刷新页面确认。');
    }
    goGlueBoardInspectionTab();
    message.success(`胶板 ${glueBoard.batchNo} 检验已提交，单号：${record.faiNo || '-'}`);
  } catch (error) {
    AModal.warning({
      title: '胶板检验未提交',
      content: getRequestErrorMessage(error, '当前胶板不满足检验申请条件，请检查胶板型号、边库批次和胶板检验标准。'),
    });
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
  if (!String(glueLossForm.lossReason || '').trim()) {
    message.warning('请填写损耗原因');
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
      glueLossVisible.value = false;
      message.success('胶板损耗已报备');
    },
  });
}

function resetReportForm() {
  activeReportDetailRecordId.value = undefined;
  Object.assign(reportForm, {
    defectCode: '',
    endTime: '',
    glueBoardBatchNo: glueBoard.batchNo,
    glueBoardMaterialCode: glueBoard.materialCode,
    glueBoardStartPosition: Number(glueBoard.availableStartPosition || 0),
    glueBoardUsageId: glueBoard.id,
    glueBoardUseLength: 0,
    lossLength: 0,
    napSampleLength: 0,
    outputLength: 0,
    parentBatchNo: '',
    processLength: 0,
    productionBatchNo: '',
    remark: '',
    reportDate: dayjs().format('YYYY-MM-DD'),
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
  reportAbnormalRows.value = [];
  clearIntermediateRecordState();
}

function handleSegmentReportClick(group: SourceGroup, segment: SourceSegment) {
  if (!isSegmentCompleted(segment)) {
    return openReportDialog(group, segment);
  }
  const records = getSegmentReportRecords(segment);
  if (!records.length) {
    message.info(`分段 ${segment.batchNo} 暂无已保存的报工记录`);
    return;
  }
  if (records.length === 1) {
    openSubmittedReportDetail(records[0], true);
    return;
  }
  segmentReportListBatchNo.value = segment.batchNo;
  segmentReportListRows.value = records;
  segmentReportListVisible.value = true;
}

function openSubmittedReportDetail(
  record?: MesHcAdhesiveConsoleApi.ReportItem | Record<string, any>,
  viewOnly = false,
) {
  if (!record?.id) {
    message.warning('请选择有效的粘胶报工记录');
    return;
  }
  const positionRange = resolveAdhesiveReportPositionRange(record);
  reportReadonly.value = true;
  reportDetailViewOnly.value = viewOnly;
  reportTimeEditing.value = false;
  resetReportForm();
  activeReportDetailRecordId.value = Number(record.id);
  Object.assign(reportForm, {
    defectCode: record.defectCode || '',
    endTime: record.endTime || '',
    glueBoardBatchNo: record.glueBoardBatchNo || '',
    glueBoardMaterialCode: record.glueBoardMaterialCode || '',
    glueBoardStartPosition: Number(record.glueBoardStartPosition || 0),
    glueBoardUsageId: record.glueBoardUsageId,
    glueBoardUseLength: Number(record.glueBoardUseLength || record.inputLength || 0),
    lossLength: Number(record.lossLength || 0),
    napSampleLength: Number(record.napSampleLength || 0),
    outputLength: Number(record.outputLength || 0),
    parentBatchNo: record.parentProductionBatchNo || record.sourceBatchNo || '',
    processLength: Number(record.inputLength || 0),
    productionBatchNo: record.productionBatchNo || '',
    remark: record.remark || '',
    reportDate: record.reportDate || dayjs().format('YYYY-MM-DD'),
    selfCheck: record.selfCheck || 'OK',
    sourceCode: record.sourceProductionBatchNo || record.productionBatchNo || '',
    sourceGrindingSecondDetailId: record.sourceGrindingSecondDetailId,
    sourceProductionBatchNo: record.sourceProductionBatchNo || record.productionBatchNo || '',
    sourceScanError: '',
    sourceScanMessage: '当前为已保存报工记录详情，仅允许查看。',
    startPosition: positionRange.start,
    endPosition: positionRange.end,
    startTime: record.startTime || '',
  });
  reportTimeSnapshot.value = {
    endTime: reportForm.endTime,
    reportDate: reportForm.reportDate,
    startTime: reportForm.startTime,
  };
  if (Array.isArray(record.checkItems) && record.checkItems.length) {
    checkTemplate.value = record.checkItems.map((item, index) => normalizeCheckItem(item, index));
  }
  reportAbnormalRows.value = (record.abnormalPositions || []).map((row, index) =>
    createReportAbnormalPositionRow({
      abnormalLength: row.abnormalLength,
      positionText: row.positionText,
      remark: row.remark,
      sortOrder: row.sortOrder ?? index + 1,
    }),
  );
  reportStep.value = 'REPORT';
  activeReportTab.value = 'report';
  reportRecordListVisible.value = false;
  segmentReportListVisible.value = false;
  reportVisible.value = true;
}

async function scanReportSource() {
  const sourceCode = resolveTransferTicketQrBusinessNo(reportForm.sourceCode);
  reportForm.sourceScanError = '';
  reportForm.sourceScanMessage = '';
  if (!sourceCode) {
    reportForm.sourceScanError = '请先扫描或输入第二次磨皮已确认分段批号。';
    return false;
  }
  try {
    const source = await scanAdhesiveConsoleSource(sourceCode);
    if (!sourceMatchesActiveTask(source)) {
      reportForm.sourceScanError = `当前已选择分段 ${activeTaskSegmentBatchNo.value || '-'}，扫码来源不属于该分段。`;
      reportForm.sourceScanMessage = '';
      return;
    }
    const sourceProductionBatchNo = source.productionBatchNo || sourceCode;
    const parentBatchNo = source.motherBatchNo || source.parentProductionBatchNo || stripSegmentMark(sourceCode);
    if (
      !(await ensureAdhesiveSampleAbnormalUnlocked(sourceProductionBatchNo, {
        actionName: '来源扫码',
        motherBatchNo: parentBatchNo,
      }))
    ) {
      reportForm.sourceScanError = '来源批次存在留样NG异常锁定，等待复检OK后才能继续报工。';
      reportForm.sourceScanMessage = '';
      return false;
    }
    reportForm.sourceGrindingSecondDetailId = source.grindingSecondDetailId;
    reportForm.sourceProductionBatchNo = sourceProductionBatchNo;
    reportForm.parentBatchNo = parentBatchNo;
    reportForm.productionBatchNo = reportForm.productionBatchNo || buildNextAdhesiveReportBatchNo(reportForm.sourceProductionBatchNo);
    const sourceLength = Number(source.outputLength || source.processLength || reportForm.processLength || 0);
    const segment = findSourceSegment(reportForm.sourceProductionBatchNo);
    const availableRange = segment
      ? findFirstAvailableRange(segment)
      : { end: sourceLength, length: sourceLength, start: 0 };
    if (segment && Number(reportForm.napSampleLength || 0) <= 0) {
      reportForm.napSampleLength = getSegmentPendingInspectionSampleLength(segment);
    }
    reportForm.startPosition = availableRange.start;
    reportForm.endPosition = availableRange.end;
    reportForm.processLength = availableRange.length;
    reportForm.outputLength = reportOutputLength.value;
    syncReportFormTimingFromSegment(segment);
    reportForm.sourceScanMessage = '来源已通过粘胶来源扫码接口带出。';
    return true;
  } catch (error) {
    reportForm.sourceScanError = '来源扫码接口未找到该批次，当前只能作为视觉原型预览，不能提交后台。';
    return false;
  }
}

async function openReportDialog(group: SourceGroup, segment: SourceSegment) {
  if (isWorkOrderFinished.value) {
    message.warning('当前工单此工序已完工，不能再报工！');
    return;
  }
  if (isSegmentCompleted(segment)) {
    message.warning(`当前分段 ${segment.batchNo} 已完成，不能再新增粘胶报工`);
    return;
  }
  if (!segment.segmentStartTime || !segment.segmentEndTime) {
    message.warning(`请先在 ${segment.batchNo} 分段卡片点击开工、完工，再进行粘胶报工登记`);
    return;
  }
  if (!(await ensureAdhesiveSampleAbnormalUnlocked(segment, { actionName: '新增报工', motherBatchNo: group.baseBatchNo }))) {
    return;
  }
  if (!isWorkOrderRunning.value) {
    AModal.warning({
      okText: '去待加工开工',
      title: '当前粘胶工单未开工',
      content: '请先在右上角“待加工”中选择当前计划并执行开工，再进行粘胶报工登记。',
      onOk: () => {
        void openTaskList();
      },
    });
    return;
  }
  if (!dailyPreparationReady.value) {
    showDailyPreparationRequiredWarning();
    return;
  }
  if (!segment.faiId) {
    AModal.warning({
      okText: '去留样送检',
      title: '当前分段尚未送检',
      content: '分段报工前必须先完成留样送检；提交送检后即可继续报工，不要求检验结果放行。',
      onOk: () => {
        void handleSegmentSampleInspection(group, segment);
      },
    });
    return;
  }
  reportReadonly.value = false;
  reportDetailViewOnly.value = false;
  reportTimeEditing.value = false;
  activeReportDetailRecordId.value = undefined;
  resetReportForm();
  applyGlueBoardDefaultsToCheckItems();
  const availableRange = findFirstAvailableRange(segment);
  reportForm.sourceCode = segment.batchNo;
  reportForm.sourceProductionBatchNo = segment.batchNo;
  reportForm.parentBatchNo = group.baseBatchNo;
  reportForm.startPosition = availableRange.start;
  reportForm.endPosition = availableRange.end;
  reportForm.processLength = availableRange.length;
  reportForm.napSampleLength = getSegmentPendingInspectionSampleLength(segment);
  reportForm.outputLength = reportOutputLength.value;
  reportForm.glueBoardStartPosition = Number(glueBoard.availableStartPosition || 0);
  reportForm.glueBoardUseLength = glueBoardConsumeLength.value;
  reportForm.productionBatchNo = buildNextAdhesiveReportBatchNo(segment.batchNo);
  syncReportFormTimingFromSegment(segment);
  reportStep.value = 'SCAN';
  activeReportTab.value = 'scan';
  reportVisible.value = true;
  await scanReportSource();
}

async function startReportWork() {
  if (isCurrentTaskReadonly.value) {
    showReadonlyTaskWarning('开工报工');
    return;
  }
  const ok = await scanReportSource();
  if (!ok) return;
  if (
    !(await ensureAdhesiveSampleAbnormalUnlocked(reportForm.sourceProductionBatchNo, {
      actionName: '开工报工',
      motherBatchNo: reportForm.parentBatchNo,
    }))
  ) {
    return;
  }
  await loadAdhesiveDevProcessParamRecords();
  reportStep.value = 'PROCESS';
  activeReportTab.value = 'dev-process-param';
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
  if (isCurrentTaskReadonly.value) {
    showReadonlyTaskWarning('提交工艺参数');
    return;
  }
  if (!hasFilledAdhesiveDevProcessParamRecord()) {
    message.info('未找到粘胶1工艺参数点检表记录，已自动打开新建上报');
    activeReportTab.value = 'dev-process-param';
    pendingReportAfterAdhesiveDevProcessParam.value = true;
    await openAdhesiveDevProcessParamFill();
    return;
  }
  reportStep.value = 'REPORT';
  activeReportTab.value = 'report';
}

function getCheckItemsByCategory(category: string) {
  if (category === '工艺参数') {
    return [...checkTemplate.value].sort((a, b) => Number(a.sortNo || 0) - Number(b.sortNo || 0));
  }
  return checkTemplate.value
    .filter((item) => item.itemCategory === category)
    .sort((a, b) => Number(a.sortNo || 0) - Number(b.sortNo || 0));
}

function getCurrentLoginUserName() {
  return firstText(currentUserName.value, userStore.userInfo?.nickname, userStore.userInfo?.username);
}

function applyAdhesiveDevCurrentLoginUser(
  header: Record<string, any>,
  options: { confirmer?: boolean; recorder?: boolean } = {},
) {
  const loginUserName = getCurrentLoginUserName();
  if (!loginUserName) return header;
  if (options.recorder !== false) {
    header.recorder = loginUserName;
    header.recorderName = loginUserName;
    header.recordUserName = loginUserName;
    header.checkerName = loginUserName;
    header.fillUserName = loginUserName;
  }
  if (options.confirmer) {
    header.confirmer = loginUserName;
    header.confirmerName = loginUserName;
    header.confirmUserName = loginUserName;
  }
  return header;
}

function getAdhesiveDevRecordTemplateCode(record: MesHcProcessFormApi.Record) {
  const context = parseJsonObject(record.contextJson) as Record<string, any>;
  return firstText(
    record.templateCode,
    record.template?.templateCode,
    context.formCode,
    context.runtimeSchema?.formCode,
  ).toUpperCase();
}

function normalizeAdhesiveDevModelKey(value?: string) {
  return String(value || '').trim().toUpperCase().replace(/[^A-Z0-9]/g, '');
}

function parseAdhesiveDevSchema(form?: MesHcStationFormApi.StationForm | null) {
  return parseJsonObject(form?.schemaJson) as Record<string, any>;
}

function isAdhesiveDevProcessCheckFormCode(formCode?: string) {
  const normalized = String(formCode || '').trim().toUpperCase();
  return normalized.startsWith(ADHESIVE_DEV_PROCESS_CHECK_FORM_CODE_PREFIX) && normalized.endsWith('_DEV');
}

function getAdhesiveDevFormMatchScore(form: MesHcStationFormApi.StationForm) {
  if (!isAdhesiveDevProcessCheckFormCode(form.formCode)) return -1;
  const normalize = (value: unknown) => String(value ?? '').trim().toUpperCase();
  const expectedModel = normalize(currentPlan.modelCode);
  const schema = parseAdhesiveDevSchema(form);
  // 编辑器顶层适用范围优先；旧模板兼容 modelMatch，不使用来源型号或编码猜测。
  const topScope = normalize(schema.modelScope);
  const match = topScope ? schema : (schema.modelMatch || schema);
  const exact = normalize(match.modelCode);
  const prefix = normalize(match.modelPrefix);
  const scope = topScope || normalize(match.scope)
    || (prefix ? 'PREFIX' : exact ? 'MODEL' : '');
  if (scope === 'MODEL') return exact && expectedModel === exact ? 100 : -1;
  if (scope === 'PREFIX') return prefix && expectedModel.startsWith(prefix) ? 80 : -1;
  return scope === 'COMMON' ? 20 : -1;
}

function resolveAdhesiveDevProcessParamForm(rows: MesHcStationFormApi.StationForm[]) {
  const candidates = rows
    .filter((item) => item.status === 1 && isAdhesiveDevProcessCheckFormCode(item.formCode))
    .map((item) => ({ form: item, score: getAdhesiveDevFormMatchScore(item) }))
    .filter((item) => item.score >= 0)
    .sort((left, right) => right.score - left.score || Number(left.form.sortNo || 0) - Number(right.form.sortNo || 0));
  return candidates[0]?.form;
}

function matchesAdhesiveDevProcessModel(record: MesHcProcessFormApi.Record) {
  const expectedModel = normalizeAdhesiveDevModelKey(currentPlan.modelCode);
  if (!expectedModel) return true;
  const header = getAdhesiveDevRecordHeader(record);
  const recordModel = normalizeAdhesiveDevModelKey(firstText(record.modelCode, header.modelCode, header.modelName));
  if (!recordModel) return true;
  return recordModel === expectedModel || recordModel.startsWith(expectedModel) || expectedModel.startsWith(recordModel);
}

function getAdhesiveDevExpectedPlanNo() {
  return firstText(currentPlan.planNo);
}

function getAdhesiveDevExpectedBatchNo() {
  return firstText(
    reportForm.sourceProductionBatchNo,
    reportForm.productionBatchNo,
    currentPlan.sourceProductionBatchNo,
    currentPlan.batchNo,
  );
}

function getAdhesiveDevRecordHeader(record?: MesHcProcessFormApi.Record | null) {
  return parseJsonObject(record?.headerDataJson) as Record<string, any>;
}

function normalizeAdhesiveDevRecordStatus(status?: string) {
  return String(status || 'DRAFT').trim().toUpperCase();
}

function getAdhesiveDevRecordStatusMeta(status?: string) {
  const normalized = normalizeAdhesiveDevRecordStatus(status);
  if (normalized === 'CONFIRMED') return { color: 'green', text: '已确认' };
  if (normalized === 'SUBMITTED') return { color: 'processing', text: '已提交' };
  if (normalized === 'DRAFT') return { color: 'blue', text: '草稿' };
  if (normalized === 'CANCELLED') return { color: 'default', text: '已作废' };
  return { color: 'default', text: status || '草稿' };
}

function isAdhesiveDevProcessParamDraft(record?: MesHcProcessFormApi.Record | null) {
  return normalizeAdhesiveDevRecordStatus(record?.recordStatus) === 'DRAFT';
}

function isAdhesiveDevProcessParamConfirmed(record?: MesHcProcessFormApi.Record | null) {
  return normalizeAdhesiveDevRecordStatus(record?.recordStatus) === 'CONFIRMED';
}

function getAdhesiveDevConfirmResult(record?: MesHcProcessFormApi.Record | null) {
  const header = getAdhesiveDevRecordHeader(record);
  return firstText(header.inspectionResult, header.confirmResult, record?.resultStatus, '-');
}

function getAdhesiveDevConfirmUser(record?: MesHcProcessFormApi.Record | null) {
  const header = getAdhesiveDevRecordHeader(record);
  return firstText(record?.confirmUserName, header.confirmer, header.confirmUserName, '-');
}

function getAdhesiveDevConfirmTime(record?: MesHcProcessFormApi.Record | null) {
  const header = getAdhesiveDevRecordHeader(record);
  return firstText(record?.confirmTime, header.confirmerTime, header.confirmTime, '-');
}

function matchesCurrentAdhesiveDevProcessRecord(record: MesHcProcessFormApi.Record) {
  const templateCode = getAdhesiveDevRecordTemplateCode(record);
  if (!isAdhesiveDevProcessCheckFormCode(templateCode)) {
    return false;
  }
  if (!matchesAdhesiveDevProcessModel(record)) {
    return false;
  }
  const expectedPlanOperationId = Number(currentPlan.planOperationId || 0);
  if (expectedPlanOperationId && record.planOperationId && Number(record.planOperationId) !== expectedPlanOperationId) {
    return false;
  }
  const expectedPlanId = Number(currentPlan.planId || 0);
  if (expectedPlanId && record.planId && Number(record.planId) !== expectedPlanId) {
    return false;
  }
  const expectedPlanNo = getAdhesiveDevExpectedPlanNo();
  if (expectedPlanNo && record.planNo && String(record.planNo).trim() !== expectedPlanNo) {
    return false;
  }
  const expectedBatchNo = getAdhesiveDevExpectedBatchNo();
  if (expectedBatchNo && record.batchNo && String(record.batchNo).trim() !== expectedBatchNo) {
    return false;
  }
  return true;
}

let adhesiveProcessTemplateRequest = 0;
async function ensureAdhesiveDevProcessParamForm() {
  const request = ++adhesiveProcessTemplateRequest;
  const modelKey = String(currentPlan.modelCode || '').trim().toUpperCase();
  adhesiveDevProcessParamForm.value = null;
  adhesiveDevProcessParamForms.value = [];
  const page = await getStationFormPage({
    formCode: ADHESIVE_DEV_PROCESS_CHECK_FORM_CODE_PREFIX,
    processCode: 'ADHESIVE',
    status: 1,
    pageNo: 1,
    pageSize: 100,
  });
  if (request !== adhesiveProcessTemplateRequest
      || modelKey !== String(currentPlan.modelCode || '').trim().toUpperCase()) return undefined;
  const rows = Array.isArray(page?.list) ? page.list : [];
  adhesiveDevProcessParamForms.value = rows.filter((item) => isAdhesiveDevProcessCheckFormCode(item.formCode));
  const form = resolveAdhesiveDevProcessParamForm(adhesiveDevProcessParamForms.value);
  adhesiveDevProcessParamForm.value = form || null;
  return form;
}

async function loadAdhesiveDevProcessParamRecords() {
  adhesiveDevProcessParamLoading.value = true;
  try {
    await ensureAdhesiveDevProcessParamForm();
    const page = await getProcessFormRecordPage({
      batchNo: getAdhesiveDevExpectedBatchNo() || undefined,
      pageNo: 1,
      pageSize: 200,
      planNo: getAdhesiveDevExpectedPlanNo() || undefined,
      processCode: 'ADHESIVE',
    });
    adhesiveDevProcessParamRecords.value = (Array.isArray(page?.list) ? page.list : [])
      .filter((row) => matchesCurrentAdhesiveDevProcessRecord(row))
      .sort((left, right) => String(right.createTime || right.fillTime || '').localeCompare(String(left.createTime || left.fillTime || '')));
  } catch (error: any) {
    message.error(error?.message || '加载粘胶1工艺参数点检表记录失败');
  } finally {
    adhesiveDevProcessParamLoading.value = false;
  }
}

function buildAdhesiveDevProcessParamInitialParams() {
  return applyAdhesiveDevCurrentLoginUser({
    batchNo: getAdhesiveDevExpectedBatchNo(),
    equipmentCode: firstText(currentPlan.equipmentCode, boardEquipment.code),
    equipmentId: firstText(currentPlan.equipmentId, boardEquipment.id),
    equipmentName: firstText(currentPlan.equipmentName, boardEquipment.name),
    materialCode: firstText(currentPlan.materialCode),
    modelCode: firstText(currentPlan.modelCode),
    planId: firstText(currentPlan.planId),
    planNo: getAdhesiveDevExpectedPlanNo(),
    planOperationId: firstText(currentPlan.planOperationId),
    recordDate: dayjs().format('YYYY-MM-DD'),
    sourceRowId: firstText(reportForm.sourceGrindingSecondDetailId, reportForm.sourceProductionBatchNo),
  }, { confirmer: false, recorder: true }) as Record<string, string>;
}

async function openAdhesiveDevProcessParamFill() {
  const form = await ensureAdhesiveDevProcessParamForm();
  if (!form?.id) {
    message.warning('未找到适用的粘胶1生产点检模板，请检查指定型号、型号前缀或通用模板是否启用');
    return;
  }
  adhesiveDevProcessParamInitialParams.value = buildAdhesiveDevProcessParamInitialParams();
  adhesiveDevProcessParamFillOpen.value = true;
}

async function handleAdhesiveDevProcessParamSaved() {
  const shouldEnterReport = pendingReportAfterAdhesiveDevProcessParam.value;
  pendingReportAfterAdhesiveDevProcessParam.value = false;
  await loadAdhesiveDevProcessParamRecords();
  if (shouldEnterReport && hasFilledAdhesiveDevProcessParamRecord()) {
    reportStep.value = 'REPORT';
    activeReportTab.value = 'report';
  }
}

function buildAdhesiveDevRecordRuntimeDetailRows(record?: MesHcProcessFormApi.Record | null) {
  return (record?.items || []).map((item, index) => ({
    actualValue: firstText(item.actualValue),
    actualValue2: firstText(item.actualValue2),
    abnormalRemark: firstText(item.abnormalRemark),
    category: firstText(item.itemCategory),
    fieldLabel: firstText(item.fieldLabel),
    id: item.id || index + 1,
    item: firstText(item.fieldLabel, item.fieldKey),
    itemCategory: firstText(item.itemCategory),
    itemName: firstText(item.fieldLabel, item.fieldKey),
    node: firstText(item.stepNode),
    remark: firstText(item.abnormalRemark),
    resultFlag: firstText(item.resultFlag, 'OK'),
    seq: item.itemSeq || index + 1,
    sortNo: item.itemSeq || index + 1,
    standard: firstText(item.standardText, '-'),
    standardText: firstText(item.standardText, '-'),
    status: firstText(item.resultFlag, 'OK'),
    stepNode: firstText(item.stepNode),
    templateItemId: item.templateItemId,
    valueMode: firstText(item.valueMode, 'TEXT'),
  }));
}

function buildAdhesiveDevProcessParamHeaderData(record?: MesHcProcessFormApi.Record | null) {
  const header = {
    ...(parseJsonObject(record?.headerDataJson) as Record<string, any>),
  };
  if (!Array.isArray(header.previewDetails)) {
    header.previewDetails = buildAdhesiveDevRecordRuntimeDetailRows(record);
  }
  return header;
}

async function openAdhesiveDevProcessParamDetail(record: MesHcProcessFormApi.Record, mode: 'edit' | 'view' = 'view') {
  if (!record.id) return;
  adhesiveDevProcessParamViewLoading.value = true;
  adhesiveDevProcessParamViewVisible.value = true;
  adhesiveDevProcessParamViewMode.value = mode;
  try {
    const detail = await getProcessFormRecordDetail(record.id);
    adhesiveDevProcessParamViewRecord.value = detail;
    adhesiveDevProcessParamViewHeaderData.value = applyAdhesiveDevCurrentLoginUser(
      buildAdhesiveDevProcessParamHeaderData(detail),
      {
        confirmer: false,
        recorder: mode === 'edit' && isAdhesiveDevProcessParamDraft(detail),
      },
    );
  } catch (error: any) {
    adhesiveDevProcessParamViewVisible.value = false;
    message.error(error?.message || '加载粘胶1工艺参数点检表详情失败');
  } finally {
    adhesiveDevProcessParamViewLoading.value = false;
  }
}

function closeAdhesiveDevProcessParamDetail() {
  adhesiveDevProcessParamViewVisible.value = false;
  adhesiveDevProcessParamViewRecord.value = null;
  adhesiveDevProcessParamViewHeaderData.value = {};
  adhesiveDevProcessParamViewMode.value = 'view';
}

const adhesiveDevProcessParamViewContext = computed<Record<string, any>>(
  () => parseJsonObject(adhesiveDevProcessParamViewRecord.value?.contextJson) as Record<string, any>,
);
const adhesiveDevProcessParamViewSchema = computed<Record<string, any>>(
  () => adhesiveDevProcessParamViewContext.value.runtimeSchema || {},
);
const adhesiveDevProcessParamViewItems = computed<MesHcStationFormApi.StationFormItem[]>(() =>
  buildAdhesiveDevRecordRuntimeDetailRows(adhesiveDevProcessParamViewRecord.value).map((row) => ({
    defaultResult: row.resultFlag,
    id: row.templateItemId,
    itemCategory: row.itemCategory,
    itemName: row.itemName,
    itemSeq: row.seq,
    standardText: row.standardText,
    stepNode: row.stepNode,
    valueMode: row.valueMode,
  })),
);
const adhesiveDevProcessParamViewMetaItems = computed(() => [
  `计划号：${adhesiveDevProcessParamViewRecord.value?.planNo || '-'}`,
  `批号：${adhesiveDevProcessParamViewRecord.value?.batchNo || '-'}`,
  '工序：粘胶1',
]);

function buildAdhesiveDevProcessParamUpdatePayload(
  record: MesHcProcessFormApi.Record,
  headerData: Record<string, any>,
  items?: MesHcProcessFormApi.RecordItem[],
) {
  const { template: _template, ...payload } = record as MesHcProcessFormApi.Record & { template?: unknown };
  return {
    ...payload,
    headerDataJson: JSON.stringify(headerData),
    items: items || record.items || [],
  } as MesHcProcessFormApi.Record;
}

async function refreshAdhesiveDevProcessParamRecord(id: number) {
  const detail = await getProcessFormRecordDetail(id);
  adhesiveDevProcessParamViewRecord.value = detail;
  adhesiveDevProcessParamViewHeaderData.value = buildAdhesiveDevProcessParamHeaderData(detail);
  await loadAdhesiveDevProcessParamRecords();
  return detail;
}

function getAdhesiveDevProcessParamRuntimeTitle() {
  return firstText(
    adhesiveDevProcessParamViewRecord.value?.templateName,
    adhesiveDevProcessParamForm.value?.formName,
    '粘胶1工艺参数点检表',
  );
}

async function exportAdhesiveDevProcessParamExcel() {
  if (!adhesiveDevProcessParamViewRecord.value?.id) return;
  adhesiveDevProcessParamExcelLoading.value = true;
  try {
    const title = getAdhesiveDevProcessParamRuntimeTitle();
    const layout = adhesiveDevProcessParamRuntimeRef.value?.buildExcelLayout?.(
      title,
      'export',
    ) as MesHcProcessFormApi.LayoutExcelReq;
    if (!layout) {
      message.warning('当前粘胶1工艺参数点检表还没有可导出的运行布局');
      return;
    }
    const data = await exportProcessFormRecordLayout(layout);
    downloadFileFromBlobPart({ fileName: layout.fileName || `${title}.xlsx`, source: data });
  } catch (error: any) {
    console.error('[粘胶1工艺参数DEV] 导出失败', error);
    message.error(getRequestErrorMessage(error, '导出粘胶1工艺参数点检表 Excel 失败'));
  } finally {
    adhesiveDevProcessParamExcelLoading.value = false;
  }
}

function canImportAdhesiveDevProcessParamExcel() {
  const record = adhesiveDevProcessParamViewRecord.value;
  return adhesiveDevProcessParamViewMode.value === 'edit' && !!record?.id && isAdhesiveDevProcessParamDraft(record);
}

function triggerAdhesiveDevProcessParamImportExcel() {
  if (!canImportAdhesiveDevProcessParamExcel()) {
    message.warning('只有草稿修改状态允许导入粘胶1工艺参数点检表 Excel');
    return;
  }
  adhesiveDevProcessParamImportInputRef.value?.click();
}

async function appendAdhesiveDevProcessParamImportAttachment(file: File) {
  const uploaded = (await uploadFile({
    directory: 'mes/adhesive1-process-param',
    file,
  })) as any;
  const runtimeHeader = adhesiveDevProcessParamRuntimeRef.value?.getHeaderData?.() || adhesiveDevProcessParamViewHeaderData.value;
  const attachments = Array.isArray(runtimeHeader.attachments) ? runtimeHeader.attachments : [];
  const nextAttachments = [
    ...attachments,
    {
      name: uploaded?.name || file.name,
      path: uploaded?.path,
      size: uploaded?.size || file.size,
      type: uploaded?.type || file.type,
      uploadTime: dayjs().format('YYYY-MM-DD HH:mm:ss'),
      url: typeof uploaded === 'string' ? uploaded : uploaded?.url,
    },
  ];
  runtimeHeader.attachments = nextAttachments;
  runtimeHeader.processFormExcelAttachments = nextAttachments;
  adhesiveDevProcessParamViewHeaderData.value = runtimeHeader;
  adhesiveDevProcessParamRuntimeRef.value?.setHeaderData?.(runtimeHeader);
}

async function handleAdhesiveDevProcessParamImportExcel(event: Event) {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  input.value = '';
  if (!file) return;
  if (!/\.xlsx?$/iu.test(file.name)) {
    message.warning('请选择 Excel 文件');
    return;
  }
  if (!canImportAdhesiveDevProcessParamExcel()) {
    message.warning('只有草稿修改状态允许导入粘胶1工艺参数点检表 Excel');
    return;
  }
  adhesiveDevProcessParamExcelLoading.value = true;
  try {
    const layout = adhesiveDevProcessParamRuntimeRef.value?.buildExcelLayout?.(
      getAdhesiveDevProcessParamRuntimeTitle(),
      'import',
    ) as MesHcProcessFormApi.LayoutExcelReq;
    if (!layout) {
      message.warning('当前粘胶1工艺参数点检表还没有可导入的运行布局');
      return;
    }
    const resp = await importProcessFormRecordLayout(file, layout);
    const summary = adhesiveDevProcessParamRuntimeRef.value?.applyImportedLayout?.(resp);
    await appendAdhesiveDevProcessParamImportAttachment(file);
    const rowText = summary?.rowCount === undefined ? '' : `，有效明细 ${summary.rowCount} 行`;
    const skippedText = summary?.skippedRowCount ? `，跳过标题/空行 ${summary.skippedRowCount} 行` : '';
    message.success(`导入成功，已回填 ${summary?.appliedCellCount ?? resp.totalCellCount ?? 0} 个单元格${rowText}${skippedText}`);
  } catch (error: any) {
    console.error('[粘胶1工艺参数DEV] 导入失败', error);
    message.error(getRequestErrorMessage(error, '导入粘胶1工艺参数点检表 Excel 失败'));
  } finally {
    adhesiveDevProcessParamExcelLoading.value = false;
  }
}

async function saveAdhesiveDevProcessParamView(silent = false) {
  const record = adhesiveDevProcessParamViewRecord.value;
  if (!record?.id) return undefined;
  if (!isAdhesiveDevProcessParamDraft(record)) {
    message.warning('只有草稿状态的粘胶1工艺参数点检表允许修改');
    return undefined;
  }
  adhesiveDevProcessParamSaving.value = true;
  try {
    const runtimeHeader = adhesiveDevProcessParamRuntimeRef.value?.getHeaderData?.() || adhesiveDevProcessParamViewHeaderData.value;
    applyAdhesiveDevCurrentLoginUser(runtimeHeader, { confirmer: false, recorder: true });
    const recordTime = buildNowText();
    runtimeHeader.recorderTime = recordTime;
    runtimeHeader.recordTime = recordTime;
    runtimeHeader.checkerTime = recordTime;
    const runtimeItems = adhesiveDevProcessParamRuntimeRef.value?.buildRecordItems?.() || record.items || [];
    await updateProcessFormRecord(buildAdhesiveDevProcessParamUpdatePayload(record, runtimeHeader, runtimeItems));
    if (!silent) {
      AModal.success({
        title: '保存成功',
        content: '粘胶1工艺参数点检表已保存。',
        okText: '确认',
        zIndex: 4300,
        onOk() {
          if (adhesiveDevProcessParamViewRecord.value?.id === record.id) {
            closeAdhesiveDevProcessParamDetail();
          }
        },
      });
    }
    if (adhesiveDevProcessParamViewRecord.value?.id === record.id) {
      await refreshAdhesiveDevProcessParamRecord(record.id);
    } else {
      await loadAdhesiveDevProcessParamRecords();
    }
    return record.id;
  } finally {
    adhesiveDevProcessParamSaving.value = false;
  }
}

async function executeConfirmAdhesiveDevProcessParamRecord(record: MesHcProcessFormApi.Record, userInfo: any) {
  if (!record.id) return;
  if (isAdhesiveDevProcessParamConfirmed(record)) {
    message.info('该粘胶1工艺参数点检表已确认');
    return;
  }
  const confirmedRecordId = record.id!;
  const confirmUserId = Number(userInfo?.userId);
  if (!Number.isSafeInteger(confirmUserId) || confirmUserId <= 0) {
    message.warning('未识别到认证确认人，请重新进行身份认证');
    return;
  }
  adhesiveDevProcessParamConfirming.value = true;
  try {
    const detail = await getProcessFormRecordDetail(confirmedRecordId);
    const isCurrentEditingRecord = adhesiveDevProcessParamViewRecord.value?.id === confirmedRecordId;
    const header = isCurrentEditingRecord
      ? (adhesiveDevProcessParamRuntimeRef.value?.getHeaderData?.() || adhesiveDevProcessParamViewHeaderData.value)
      : buildAdhesiveDevProcessParamHeaderData(detail);
    const items = isCurrentEditingRecord
      ? (adhesiveDevProcessParamRuntimeRef.value?.buildRecordItems?.() || detail.items || [])
      : (detail.items || []);
    applyAdhesiveDevCurrentLoginUser(header, { confirmer: false, recorder: false });
    header.inspectionResult = firstText(header.inspectionResult, header.confirmResult, detail.resultStatus, 'OK');
    await updateProcessFormRecord(buildAdhesiveDevProcessParamUpdatePayload(detail, header, items));
    await confirmProcessFormRecordBySigner({ id: confirmedRecordId, confirmUserId });
    await loadAdhesiveDevProcessParamRecords();
    if (adhesiveDevProcessParamViewRecord.value?.id === confirmedRecordId) {
      await refreshAdhesiveDevProcessParamRecord(confirmedRecordId);
      adhesiveDevProcessParamViewMode.value = 'view';
      closeAdhesiveDevProcessParamDetail();
    }
    message.success('粘胶1工艺参数点检表已保存并确认。');
  } finally {
    adhesiveDevProcessParamConfirming.value = false;
  }
}

function confirmAdhesiveDevProcessParamRecord(record: MesHcProcessFormApi.Record) {
  if (!record.id) return;
  if (isAdhesiveDevProcessParamConfirmed(record)) {
    message.info('该粘胶1工艺参数点检表已确认');
    return;
  }
  adhesiveDevProcessParamAuthRecord.value = record;
  adhesiveDevProcessParamAuthVisible.value = true;
}

async function handleAdhesiveDevProcessParamAuthSuccess(userInfo: any) {
  const record = adhesiveDevProcessParamAuthRecord.value;
  adhesiveDevProcessParamAuthVisible.value = false;
  adhesiveDevProcessParamAuthRecord.value = null;
  if (!record) return;
  await executeConfirmAdhesiveDevProcessParamRecord(record, userInfo);
}

function hasFilledAdhesiveDevProcessParamRecord() {
  return adhesiveDevProcessParamRecords.value.some(
    (record) => normalizeAdhesiveDevRecordStatus(record.recordStatus) !== 'CANCELLED',
  );
}

function getComparableReportRanges(segment?: SourceSegment) {
  const currentRecordId = activeReportDetailRecordId.value;
  return (segment?.reportRanges || []).filter(
    (range) => !currentRecordId || String(range.reportId || '') !== String(currentRecordId),
  );
}

function getReportPositionValidationMessage() {
  if (reportReadonly.value) return '';
  const segment = findSourceSegment(reportForm.sourceProductionBatchNo);
  const start = Number(reportForm.startPosition ?? 0);
  const end = Number(reportForm.endPosition ?? 0);
  const processLength = Number(reportForm.processLength || 0);
  if (!Number.isFinite(start) || !Number.isFinite(end)) return '请填写有效的起止位置';
  if (start < 0) return '起始位置不能小于0';
  if (end <= start) return '结束位置必须大于起始位置';
  if (Math.abs(end - start - processLength) > 0.001) return '加工米数必须等于结束位置-起始位置';
  if (segment?.outputLength && end > Number(segment.outputLength)) {
    return `结束位置不能超出来源长度 ${formatNumber(segment.outputLength)} m`;
  }
  const overlapRange = getComparableReportRanges(segment).find((range) => start < range.end && end > range.start);
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
  return getComparableReportRanges(segment).filter((range) => current.start < range.end && current.end > range.start);
});
const hasReportRangeOverlap = computed(() => reportOverlapRanges.value.length > 0);
const reportRangeTotal = computed(() => {
  const segment = findSourceSegment(reportForm.sourceProductionBatchNo);
  const reportedMax = getComparableReportRanges(segment).reduce((max, item) => Math.max(max, item.end), 0);
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
  const items = getComparableReportRanges(segment).map((range) => {
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

function startEditReadonlyReportTime() {
  if (!canEditReadonlyReportTime.value) {
    message.warning('未找到可修改时间的粘胶报工记录');
    return;
  }
  reportTimeSnapshot.value = {
    endTime: reportForm.endTime || '',
    reportDate: reportForm.reportDate || '',
    startTime: reportForm.startTime || '',
  };
  reportTimeEditing.value = true;
  activeReportTab.value = 'report';
}

async function saveReadonlyReportTime() {
  const reportId = activeReportDetailRecordId.value;
  if (!reportId) {
    message.warning('未找到可保存的粘胶报工记录');
    return;
  }
  const startTime = normalizeReportDateTimeText(reportForm.startTime, reportForm.reportDate || dayjs().format('YYYY-MM-DD'));
  const endTime = normalizeReportDateTimeText(reportForm.endTime, reportForm.reportDate || reportDateFromDateTime(startTime));
  if (!startTime) {
    message.warning('请填写有效的开始时间');
    return;
  }
  if (!endTime) {
    message.warning('请填写有效的结束时间');
    return;
  }
  reportTimeSaving.value = true;
  try {
    await updateAdhesiveConsoleReportTime({
      endTime,
      id: reportId,
      reportDate: reportForm.reportDate || reportDateFromDateTime(startTime),
      startTime,
    });
    await Promise.all([loadReports(), loadSourceGroups()]);
    reportVisible.value = false;
    reportReadonly.value = false;
    reportDetailViewOnly.value = false;
    reportTimeEditing.value = false;
    activeReportDetailRecordId.value = undefined;
    message.success('粘胶1报工开始/结束时间已保存');
  } finally {
    reportTimeSaving.value = false;
  }
}

async function submitAdhesiveReport() {
  if (!(await ensureStrictGlueBoardMatch())) return;
  if (!currentPlan.planId || !currentPlan.planOperationId) {
    message.warning('请先扫码带出粘胶计划');
    return;
  }
  if (isWorkOrderFinished.value) {
    message.warning('当前工单此工序已完工，不能再报工！');
    return;
  }
  if (!isWorkOrderRunning.value) {
    AModal.warning({
      okText: '去待加工开工',
      title: '当前粘胶工单未开工',
      content: '请先在右上角“待加工”中选择当前计划并执行开工，再进行粘胶报工登记。',
      onOk: () => {
        void openTaskList();
      },
    });
    return;
  }
  if (!reportForm.sourceGrindingSecondDetailId) {
    message.warning('请先通过来源扫码接口确认第二次磨皮分段批号');
    return;
  }
  if (
    !(await ensureAdhesiveSampleAbnormalUnlocked(reportForm.sourceProductionBatchNo, {
      actionName: '提交报工',
      motherBatchNo: reportForm.parentBatchNo,
    }))
  ) {
    return;
  }
  if (!reportForm.processLength || reportForm.processLength <= 0) {
    message.warning('请填写加工米数');
    return;
  }
  const positionMessage = getReportPositionValidationMessage();
  if (positionMessage) {
    message.warning(positionMessage);
    return;
  }
  const glueBoardUseLength = glueBoardConsumeLength.value;
  const usesGlueBoard = glueBoardUseLength > 0;
  if (usesGlueBoard && !glueBoard.id) {
    AModal.warning({
      okText: '进入胶板领用',
      title: '未登记胶板领用',
      content: '粘胶报工前必须先登记本次胶板领用。确认后进入胶板领用登记。',
      onOk: () => {
        openGlueConsumeDialog();
      },
    });
    return;
  }
  const abnormalPositions = buildReportAbnormalPositions();
  if (abnormalPositions === null) return;
  const outputLength = reportOutputLength.value;
  const sourceSegment = findSourceSegment(reportForm.sourceProductionBatchNo);
  if (!sourceSegment?.segmentStartTime || !sourceSegment.segmentEndTime) {
    message.warning('请先在当前分段卡片点击开工、完工，报工时间将自动同步');
    return;
  }
  syncReportFormTimingFromSegment(sourceSegment);
  const reportDate = reportForm.reportDate || reportDateFromDateTime(reportForm.startTime);
  const startTime = normalizeReportDateTimeText(reportForm.startTime, reportDate);
  const endTime = normalizeReportDateTimeText(reportForm.endTime, reportDate);
  if (!startTime || !endTime) {
    message.warning('分段开工、完工时间无效，请刷新后重试');
    return;
  }
  await saveAdhesiveConsoleReport({
    abnormalPositions,
    checkItems: checkTemplate.value.map((item, index) => ({
      abnormalRemark: item.abnormalRemark,
      actualValue: item.actualValue,
      checkResult: item.checkResult || 'OK',
      itemCategory: item.itemCategory,
      itemName: item.itemName,
      sortNo: Number(item.sortNo || index + 1),
      standardValue: item.standardValue,
    })),
    defectCode: reportForm.defectCode || undefined,
    endTime,
    glueBoardBatchNo: usesGlueBoard ? reportForm.glueBoardBatchNo || undefined : undefined,
    glueBoardMaterialCode: usesGlueBoard ? reportForm.glueBoardMaterialCode || undefined : undefined,
    glueBoardStartPosition: usesGlueBoard
      ? Number(reportForm.glueBoardStartPosition || glueBoard.availableStartPosition || 0)
      : undefined,
    glueBoardUsageId: usesGlueBoard ? glueBoard.id : undefined,
    glueBoardUseLength,
    inputLength: Number(reportForm.processLength || 0),
    startPosition: Number(reportForm.startPosition || 0),
    endPosition: Number(reportForm.endPosition || 0),
    lossLength: Number(reportForm.lossLength || 0),
    materialCode: currentPlan.materialCode || undefined,
    modelCode: currentPlan.modelCode || undefined,
    napSampleLength: Number(reportForm.napSampleLength || 0),
    outputLength,
    parentProductionBatchNo: reportForm.parentBatchNo || undefined,
    planId: currentPlan.planId,
    planOperationId: currentPlan.planOperationId,
    productionBatchNo: reportForm.productionBatchNo || undefined,
    recorderName: currentUserName.value || undefined,
    recorderTime: buildNowText(),
    remark: reportForm.remark || undefined,
    reportDate,
    selfCheck: reportForm.selfCheck,
    sourceBatchNo: reportForm.parentBatchNo || undefined,
    sourceGrindingSecondDetailId: reportForm.sourceGrindingSecondDetailId,
    sourceProductionBatchNo: reportForm.sourceProductionBatchNo || undefined,
    sourceType: '扫码母料',
    startTime,
  });
  await loadReports();
  await loadGlueBoardUsage();
  await glueBoardEdgeConsumableTabRef.value?.reload?.();
  await loadSourceGroups();
  reportVisible.value = false;
  reportTimeEditing.value = false;
  activeReportDetailRecordId.value = undefined;
  clearIntermediateRecordState();
  message.success('粘胶1报工记录已保存');
}

watch(
  () => [reportForm.lossLength, reportForm.napSampleLength],
  () => {
    reportForm.outputLength = reportOutputLength.value;
    reportForm.glueBoardUseLength = glueBoardConsumeLength.value;
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
      reportForm.glueBoardUseLength = glueBoardConsumeLength.value;
      syncingReportPosition = false;
    }
  },
);

watch(scanPlanNo, (value) => schedulePlanScan(value));

function toggleExtendedBoardTabs() {
  showExtendedBoardTabs.value = !showExtendedBoardTabs.value;
  if (!showExtendedBoardTabs.value && !baseBoardTabs.includes(activeBoardTab.value)) {
    activeBoardTab.value = 'SOURCE';
  }
  message.info(showExtendedBoardTabs.value ? '已显示点检清洁、报工记录和中间品记录单' : '已隐藏扩展记录页签');
}

watch(showExtendedBoardTabs, (visible) => {
  if (!visible && !baseBoardTabs.includes(activeBoardTab.value)) {
    activeBoardTab.value = 'SOURCE';
  }
});

watch(activeBoardTab, async () => {
  await nextTick();
  window.dispatchEvent(new Event('resize'));
});

onActivated(() => {
  attachGlobalScannerListener();
});

onMounted(async () => {
  attachGlobalScannerListener();
  timer = setInterval(() => {
    currentDateTime.value = dayjs().format('YYYY-MM-DD HH:mm:ss');
  }, 1000);
  glueBoardInspectionRefreshTimer = setInterval(() => {
    void refreshGlueBoardInspectionState({ silent: true });
  }, GLUE_BOARD_INSPECTION_REFRESH_INTERVAL_MS);
  await loadCheckTemplate();
  await loadTaskList();
  await loadGlueBoardUsage();
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
  if (planScanTimer) clearTimeout(planScanTimer);
  if (glueBoardInspectionRefreshTimer) clearInterval(glueBoardInspectionRefreshTimer);
  detachGlobalScannerListener();
});
</script>

<template>
  <Page auto-content-height :loading="boardLoading">
    <div class="adhesive-console" :class="{ 'is-source-maximized': visualMaximized }">
      <div class="prototype-banner shrink-0 bg-white rounded-xl shadow-sm border border-slate-200 flex p-3 relative overflow-hidden">
        <div class="flex flex-1 items-center gap-4 min-w-0 pl-1">
          <div
            class="console-main-icon w-[60px] h-[60px] bg-gradient-to-br from-cyan-500 to-blue-600 rounded-xl shadow-md flex items-center justify-center shrink-0 text-white"
            title="双击显示/隐藏点检清洁、报工记录和中间品记录单"
            @dblclick="toggleExtendedBoardTabs"
          >
            <IconifyIcon icon="lucide:layers" class="text-[32px]" />
          </div>
          <div class="console-title-block">
            <div class="console-title-row">
              <span class="console-title-text">粘胶1操作看板</span>
              <Tag color="processing" class="console-title-tag">粘胶1</Tag>
            </div>
            <div class="console-meta-row">
              <div class="console-meta-item console-meta-item--machine">
                <span class="console-meta-label">粘胶机台</span>
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
        <div class="inspection-stamp-slot adhesive-status-stamp-slot">
          <button
            class="inspection-stamp-side adhesive-qtime-stamp"
            :class="`inspection-stamp-side--${adhesiveTopQtimeMeta.stampClass}`"
            :title="adhesiveTopQtimeMeta.title"
            type="button"
          >
            <span class="inspection-stamp-content">
              <span>粘胶1 QTIME</span>
              <strong>{{ adhesiveTopQtimeMeta.stampText }}</strong>
              <em>{{ adhesiveTopQtimeMeta.subText }}</em>
            </span>
          </button>
          <button
            class="inspection-stamp-side adhesive-aqc-stamp"
            :class="`inspection-stamp-side--${glueBoardAqcStatusMeta.stampClass}`"
            type="button"
            @click="openGlueBoardAqcDialog"
          >
            <span class="inspection-stamp-content">
              <span>胶板检验</span>
              <strong>{{ glueBoardAqcStatusMeta.stampText }}</strong>
              <em>{{ glueBoardAqcStampTime }}</em>
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
            v-if="!isCurrentTaskReadonly"
            class="w-[64px] h-[64px] bg-amber-50 border border-amber-200 text-amber-700 rounded-xl flex flex-col items-center justify-center cursor-pointer hover:bg-amber-100 hover:shadow-md transition-all active:scale-95"
            @click="openGlueBoardAqcDialog"
          >
            <IconifyIcon icon="lucide:shield-check" class="text-[22px] mb-1 opacity-80" />
            <span class="text-[11px] font-bold">胶板检验</span>
          </div>
          <div
            v-if="!isCurrentTaskReadonly"
            class="w-[64px] h-[64px] bg-sky-50 border border-sky-200 text-sky-700 rounded-xl flex flex-col items-center justify-center cursor-pointer hover:bg-sky-100 hover:shadow-md transition-all active:scale-95"
            @click="openSelectedRecordConfirm"
          >
            <IconifyIcon icon="lucide:scan-line" class="text-[22px] mb-1 opacity-80" />
            <span class="text-[11px] font-bold">扫码确认</span>
          </div>
          <div
            v-if="!isCurrentTaskReadonly"
            class="w-[64px] h-[64px] bg-rose-50 border border-rose-300 text-rose-700 rounded-xl flex flex-col items-center justify-center cursor-pointer hover:bg-rose-100 hover:shadow-md transition-all active:scale-95"
            @click="handleSegmentComplete"
          >
            <IconifyIcon icon="lucide:badge-check" class="text-[22px] mb-1 opacity-80" />
            <span class="text-[11px] font-bold">本段完成</span>
          </div>
          <div
            v-if="!isCurrentTaskReadonly"
            class="w-[64px] h-[64px] bg-teal-50 border border-teal-200 text-teal-700 rounded-xl flex flex-col items-center justify-center cursor-pointer hover:bg-teal-100 hover:shadow-md transition-all active:scale-95"
            @click="reportRecordListVisible = true"
          >
            <IconifyIcon icon="lucide:list-checks" class="text-[22px] mb-1 opacity-80" />
            <span class="text-[11px] font-bold">报工记录</span>
          </div>
        </div>
      </div>

      <section v-if="!visualMaximized" class="erp-card glue-board-card">
        <div class="erp-card-title">
          <IconifyIcon icon="lucide:package-search" />
          胶板型号、料号与边库余量
          <Tag :color="glueBoardStatusMeta.color">{{ glueBoardStatusMeta.text }}</Tag>
          <Button size="small" type="primary" class="consumable-replace-btn" :disabled="isCurrentTaskReadonly" @click="openGlueConsumeDialog">胶板领用</Button>
          <Button size="small" danger class="consumable-replace-btn" :disabled="isCurrentTaskReadonly" @click="() => openGlueBoardAqcDialog()">胶板检验</Button>
          <Button size="small" class="consumable-replace-btn" :disabled="isCurrentTaskReadonly" @click="openGlueBoardLossDialog">损耗报备</Button>
          <span class="glue-board-title-tip" :class="{ warning: glueBoardTitleTipWarning }">
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

      <section v-if="!visualMaximized" class="erp-card plan-scan-card">
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
              placeholder="请扫码或输入计划号"
              @press-enter="consumePlanScan(false)"
              @update:value="handlePlanScanInput"
            />
          </div>
          <label>计划号</label><strong>{{ currentPlan.planNo || '请扫码计划号' }}</strong>
          <label>分段批号</label><strong>{{ currentPlan.sourceProductionBatchNo || currentPlan.batchNo || currentPlan.sourceBatchNo || '-' }}</strong>
          <label>可加工(m)</label><strong>{{ formatNumber(currentPlan.availableSourceLength) }} m</strong>
          <label>产品型号</label><strong>{{ currentPlan.modelCode || '-' }}</strong>
          <label>产品料号</label><strong>{{ currentPlan.materialCode || '-' }}</strong>

          <label>状态</label>
          <strong>
            <Tag :color="currentPlan.planNo ? getWorkOrderStatusMeta(currentPlan.status).color : 'default'">
              {{ currentPlan.planNo ? getWorkOrderStatusMeta(currentPlan.status).text : '待扫码' }}
            </Tag>
          </strong>
          <label>执行要求</label><strong class="erp-requirement-value">{{ currentPlan.requirements || '暂无执行要求' }}</strong>
        </div>
      </section>

      <Tabs v-model:active-key="activeBoardTab" class="console-tabs">
        <template #rightExtra>
          <Button class="tab-maximize-button" size="small" @click="visualMaximized = !visualMaximized">
            <IconifyIcon :icon="visualMaximized ? 'lucide:minimize-2' : 'lucide:maximize-2'" />
            {{ visualMaximized ? '还原' : '最大化' }}
          </Button>
        </template>
        <TabPane key="SOURCE" tab="可视化加工">
          <section class="workbench-blocked-panel" :class="{ 'workbench-blocked-panel--blocked': workbenchBlockedReason }">
          <div class="source-board-list">
            <div v-if="!sourceGroups.length" class="console-empty">
              {{ currentPlan.planNo ? '暂无已确认的第二次磨皮分段批号，请先完成二磨扫码确认。' : '请先扫描计划号，系统会按后台已确认二次磨皮分段批号生成可加工可视化。' }}
            </div>
            <div v-for="group in sourceGroups" :key="group.baseBatchNo" class="source-group-board">
              <div class="second-sample-toolbar adhesive-sample-toolbar">
                <div class="second-sample-segment-strip">
                  <div
                    v-for="segment in group.segments"
                    :key="`actions-${segment.batchNo}`"
                    class="second-sample-segment-cell"
                    :class="{ 'no-segment': !segment.segmentMark }"
                  >
                    <button
                      class="second-sample-action-button second-sample-action-button--start"
                      :class="{ recorded: !!segment.segmentStartTime }"
                      :disabled="!isWorkOrderRunning || !!segment.segmentStartTime || isSegmentTimingStamping(segment, 'START')"
                      :title="getSegmentTimingTitle(segment, 'START')"
                      type="button"
                      @click.stop="requestSegmentTimingStamp(segment, 'START')"
                    >
                      <IconifyIcon icon="lucide:circle-play" class="second-sample-action-button__icon" />
                      <span class="segment-main-action__copy">
                        <strong>开工</strong>
                        <small>{{ formatSegmentTimingClock(segment.segmentStartTime) }}</small>
                      </span>
                    </button>
                    <button
                      class="second-sample-action-button second-sample-action-button--end"
                      :class="{ recorded: !!segment.segmentEndTime }"
                      :disabled="!isWorkOrderRunning || !segment.segmentStartTime || !!segment.segmentEndTime || isSegmentTimingStamping(segment, 'END')"
                      :title="getSegmentTimingTitle(segment, 'END')"
                      type="button"
                      @click.stop="requestSegmentTimingStamp(segment, 'END')"
                    >
                      <IconifyIcon icon="lucide:circle-stop" class="second-sample-action-button__icon" />
                      <span class="segment-main-action__copy">
                        <strong>完工</strong>
                        <small>{{ formatSegmentTimingClock(segment.segmentEndTime) }}</small>
                      </span>
                    </button>
                    <button
                      class="second-sample-action-button second-sample-action-button--sample"
                      :class="{
                        disabled: (isCurrentTaskReadonly || isSegmentCompleted(segment)) && !segment.faiId,
                        sent: !!segment.faiId,
                        'sample-status-ng': getSegmentSampleButtonStatus(segment) === 'ng',
                        'sample-status-ok': getSegmentSampleButtonStatus(segment) === 'ok',
                        'sample-status-waiting': getSegmentSampleButtonStatus(segment) === 'waiting',
                      }"
                      :disabled="(isCurrentTaskReadonly || isSegmentCompleted(segment)) && !segment.faiId"
                      type="button"
                      @click.stop="handleSegmentSampleInspection(group, segment)"
                    >
                      <IconifyIcon icon="lucide:send" class="second-sample-action-button__icon" />
                      <span class="second-sample-segment-button__text">
                        {{ getSegmentSampleButtonText(segment) }}
                      </span>
                    </button>
                    <button
                      class="second-sample-action-button second-sample-action-button--middle"
                      :class="{ disabled: isCurrentTaskReadonly && !isSegmentCompleted(segment) }"
                      :disabled="isCurrentTaskReadonly && !isSegmentCompleted(segment)"
                      :title="isSegmentCompleted(segment) ? '查看本段中间品记录' : '填写本段中间品记录'"
                      type="button"
                      @click.stop="openSegmentIntermediateRecord(group, segment)"
                    >
                      <IconifyIcon icon="lucide:table-2" class="second-sample-action-button__icon" />
                      <span>中间品</span>
                    </button>
                    <button
                      class="second-sample-action-button second-sample-action-button--report"
                      :disabled="!isSegmentCompleted(segment) && (isCurrentTaskReadonly || segment.availableLength <= 0)"
                      :title="isSegmentCompleted(segment) ? '查看本段报工记录' : '新增本段报工'"
                      type="button"
                      @click.stop="handleSegmentReportClick(group, segment)"
                    >
                      <IconifyIcon icon="lucide:play-circle" class="second-sample-action-button__icon" />
                      <span>报工</span>
                    </button>
                    <button
                      class="second-sample-action-button second-sample-action-button--print"
                      :class="{ disabled: !getSegmentReportCount(segment) }"
                      :disabled="!getSegmentReportCount(segment)"
                      type="button"
                      @click.stop="printSegmentWorkRecord(segment)"
                    >
                      <IconifyIcon icon="lucide:printer" class="second-sample-action-button__icon" />
                      <span>打印</span>
                    </button>
                  </div>
                </div>
              </div>
              <div class="segmented-cloth-strip wafer-pad-strip">
                <QmsSampleAbnormalLockGuard
                  defer-to-cut-round
                  v-for="segment in group.segments"
                  :key="segment.batchNo"
                  class="cloth-segment adhesive-cloth-segment-lock-guard"
                  :aria-disabled="isCurrentTaskReadonly || segment.availableLength <= 0 || isSegmentCompleted(segment)"
                  :class="{
                    empty: isCurrentTaskReadonly || segment.availableLength <= 0 || isSegmentCompleted(segment),
                    'inspection-ng': getSegmentSampleButtonStatus(segment) === 'ng',
                    'inspection-ok': getSegmentSampleButtonStatus(segment) === 'ok',
                    stock: !segment.segmentMark,
                  }"
                  object-label="分段"
                  :object-no="segment.batchNo"
                  action-source-process-code="ADHESIVE1"
                  :candidates="getAdhesiveSampleLockCandidates(segment, { motherBatchNo: group.baseBatchNo })"
                  process-name="粘胶1"
                  role="button"
                  :tabindex="!isCurrentTaskReadonly && segment.availableLength > 0 && !isSegmentCompleted(segment) ? 0 : -1"
                  @click="!isCurrentTaskReadonly && segment.availableLength > 0 && !isSegmentCompleted(segment) && openReportDialog(group, segment)"
                  @keydown.enter.prevent="!isCurrentTaskReadonly && segment.availableLength > 0 && !isSegmentCompleted(segment) && openReportDialog(group, segment)"
                  @keydown.space.prevent="!isCurrentTaskReadonly && segment.availableLength > 0 && !isSegmentCompleted(segment) && openReportDialog(group, segment)"
                >
                  <div class="cloth-segment-topbar">
                    <span class="cloth-segment-count">{{ getSegmentReportCount(segment) }} 条</span>
                    <button
                      class="cloth-segment-export"
                      :class="{ disabled: !getSegmentReportCount(segment) }"
                      :disabled="!getSegmentReportCount(segment)"
                      title="导出当前段工单"
                      type="button"
                      @click.stop="exportSegmentWorkRecord(segment)"
                    >
                      <IconifyIcon icon="lucide:file-down" />
                    </button>
                  </div>
                  <div class="cloth-segment-summary">
                    <div class="cloth-segment-identity">
                      <strong class="cloth-segment-batch-main">{{ segment.batchNo }}</strong>
                      <em class="cloth-segment-label">{{ segment.label }}</em>
                      <em
                        v-if="getSegmentInspectionResultText(segment)"
                        class="cloth-segment-inspection"
                        :class="`cloth-segment-inspection--${getSegmentSampleButtonStatus(segment)}`"
                      >
                        {{ getSegmentInspectionResultText(segment) }}
                      </em>
                      <em
                        class="cloth-segment-scan-status"
                        :class="getSegmentScanStatusMeta(segment).className"
                      >
                        {{ getSegmentScanStatusMeta(segment).text }}
                      </em>
                    </div>
                    <div class="cloth-segment-metrics">
                      <span class="cloth-segment-metric">
                        <b>已加工</b>
                        <i>{{ formatNumber(getSegmentReportSummary(segment).processLength) }} m</i>
                      </span>
                      <span class="cloth-segment-metric">
                        <b>固定损耗-<em class="report-loss-sample-emphasis">包含小样条</em></b>
                        <i>{{ formatNumber(getSegmentReportSummary(segment).lossLength) }} m</i>
                      </span>
                      <span class="cloth-segment-metric">
                        <b>送检米数</b>
                        <i>{{ formatNumber(getSegmentReportSummary(segment).napSampleLength) }} m</i>
                      </span>
                      <span class="cloth-segment-metric">
                        <b>产出</b>
                        <i>{{ formatNumber(getSegmentReportSummary(segment).outputLength) }} m</i>
                      </span>
                      <span class="cloth-segment-metric">
                        <b>剩余米</b>
                        <i>{{ formatNumber(segment.availableLength) }} m</i>
                      </span>
                    </div>
                  </div>
                  <div class="adhesive-position-strip" aria-hidden="true">
                    <i
                      v-for="range in segment.reportRanges"
                      :key="`${range.reportId || range.label}-${range.start}-${range.end}`"
                      :class="{ confirmed: range.status === 'CONFIRMED' || range.status === 'SUBMITTED' }"
                      :style="getRangeStyle(range, segment)"
                    ></i>
                  </div>
                  <div class="adhesive-position-scale">
                    <b>0m</b><b>{{ formatNumber(segment.outputLength) }}m</b>
                  </div>
                </QmsSampleAbnormalLockGuard>
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
        <TabPane key="GLUE_BOARD_INSPECTION" tab="胶板检验">
          <div class="tab-table-content">
            <div class="console-table-shell">
              <div class="rough-grid-toolbar console-record-toolbar">
                <div class="rough-grid-toolbar__title">胶板首样送检 / FAI 状态</div>
                <div class="pass-work-actions rough-grid-toolbar__actions">
                  <span class="console-table-count">检验单号 {{ firstInspection.sourceReportNo || '-' }}</span>
                  <Button size="small" :disabled="!firstInspection.faiId" @click="viewFirstInspectionDetail">
                    查看FAI明细
                  </Button>
                  <Button size="small" :disabled="!firstInspection.faiId" @click="printGlueBoardInspectionTransferTicket">
                    打印胶板送检单
                  </Button>
                  <Button size="small" @click="refreshGlueBoardInspectionSummary">刷新</Button>
                </div>
              </div>
              <div class="console-table-body first-inspection-summary">
                <div class="first-inspection-summary__grid">
                  <span>FAI单号</span>
                  <strong>
                    <button
                      v-if="firstInspection.faiId"
                      class="first-inspection-summary__value-link"
                      type="button"
                      @click="viewFirstInspectionDetail"
                    >
                      {{ firstInspection.faiNo || '-' }}
                    </button>
                    <template v-else>-</template>
                  </strong>
                  <span>状态</span>
                  <strong>
                    <Tag :color="glueBoardLatestInspectionStatusMeta.color">
                      {{ glueBoardLatestInspectionStatusMeta.stampText }} / {{ glueBoardLatestInspectionStatusMeta.subText }}
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
                  <strong>{{ formatInspectionJudgmentText(firstInspection.faiJudgment) }}</strong>
                  <span>退回/NG原因</span>
                  <strong>{{ firstInspection.faiRejectReason || '-' }}</strong>
                </div>
                <div class="first-inspection-summary__hint">
                  {{ glueBoardInspectionTabNotice }}
                </div>
                <div class="first-inspection-summary__hint">
                  胶板检验用于质量追溯，不限制粘胶1报工；若判定 NG，送检当天及以后关联报工会标记异常。
                </div>
              </div>
            </div>
          </div>
        </TabPane>
        <TabPane key="INSPECTION_RECORDS" tab="留样检验记录">
          <div class="tab-table-content">
            <div class="console-table-shell">
              <div class="rough-grid-toolbar console-record-toolbar">
                <div class="rough-grid-toolbar__title">粘胶1检验记录</div>
                <div class="pass-work-actions rough-grid-toolbar__actions">
                  <span class="console-table-count">共 {{ adhesiveInspectionSummary.total }} 张</span>
                </div>
              </div>
              <div class="second-inspection-summary">
                <div class="second-inspection-summary__item">
                  <span>待反馈</span>
                  <strong>{{ adhesiveInspectionSummary.pending }}</strong>
                </div>
                <div class="second-inspection-summary__item ok">
                  <span>合格</span>
                  <strong>{{ adhesiveInspectionSummary.ok }}</strong>
                </div>
                <div class="second-inspection-summary__item abnormal">
                  <span>异常</span>
                  <strong>{{ adhesiveInspectionSummary.abnormal }}</strong>
                </div>
              </div>
              <div class="console-table-body">
                <ATable
                  class="rough-check-table console-record-table"
                  :columns="inspectionRecordColumns"
                  :data-source="adhesiveInspectionRecords"
                  :pagination="false"
                  :scroll="{ x: 1940, y: 156 }"
                  row-key="id"
                  size="small"
                >
                  <template #bodyCell="{ column, record }">
                    <template v-if="column.dataIndex === 'segmentLabel'">
                      {{ record.segmentLabel || '-' }}
                    </template>
                    <template v-if="column.dataIndex === 'faiNo'">
                      {{ record.faiNo || record.faiId || '-' }}
                    </template>
                    <template v-if="column.dataIndex === 'productionBatchNo'">
                      {{ record.productionBatchNo || '-' }}
                    </template>
                    <template v-if="column.dataIndex === 'glueBoardMaterialCode'">
                      {{ record.glueBoardMaterialCode || '-' }}
                    </template>
                    <template v-if="column.dataIndex === 'glueBoardBatchNo'">
                      {{ record.glueBoardBatchNo || '-' }}
                    </template>
                    <template v-if="column.dataIndex === 'sampleTypeName'">
                      {{ record.sampleTypeName || '-' }}
                    </template>
                    <template v-if="column.dataIndex === 'status'">
                      <Tag :color="getAdhesiveInspectionRecordStatusMeta(record).color" class="!m-0">
                        {{ getAdhesiveInspectionRecordStatusMeta(record).text }}
                      </Tag>
                    </template>
                    <template v-if="column.dataIndex === 'result'">
                      <Tag :color="getAdhesiveInspectionResultMeta(record).color" class="!m-0">
                        {{ getAdhesiveInspectionResultMeta(record).text }}
                      </Tag>
                    </template>
                    <template v-if="column.dataIndex === 'faiStandardNo'">
                      {{ formatAdhesiveInspectionStandard(record) }}
                    </template>
                    <template v-if="column.dataIndex === 'feedbackTime'">
                      {{ record.faiReturnTime || '-' }}
                    </template>
                    <template v-if="column.dataIndex === 'feedbackRemark'">
                      {{ record.faiRejectReason || '-' }}
                    </template>
                    <template v-if="column.dataIndex === 'action'">
                      <div class="table-action-stack">
                        <Button size="small" type="link" :disabled="!record.faiId" @click="viewAdhesiveInspectionRecordDetail(record)">
                          查看详情
                        </Button>
                        <Button
                          size="small"
                          type="link"
                          :disabled="!record.faiId"
                          @click="printAdhesiveInspectionRecordTransferTicket(record)"
                        >
                          流转单
                        </Button>
                      </div>
                    </template>
                  </template>
                </ATable>
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
          :scroll="{ x: 1250, y: 176 }"
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
    <TabPane v-if="showExtendedBoardTabs" key="RECORDS" tab="今日粘胶报工记录">
      <div class="tab-table-content">
        <div class="console-table-shell">
          <div class="rough-grid-toolbar console-record-toolbar">
            <div class="rough-grid-toolbar__title">今日粘胶报工记录</div>
            <div class="pass-work-actions rough-grid-toolbar__actions">
              <span class="console-table-count">记录 {{ reportRecords.length }} 条 / 已选 {{ selectedReportRecords.length }} 条</span>
              <Button size="small" :disabled="isCurrentTaskReadonly || !reportRecords.length" @click="openSelectedRecordConfirm">扫码确认</Button>
              <Button size="small" :disabled="!printableReportRecords.length" @click="printSelectedAdhesiveRecords">选择打印</Button>
              <Button size="small" :disabled="!unprintedReportRecords.length" @click="printAllAdhesiveRecords">打印全部新报工</Button>
            </div>
          </div>
          <div class="console-table-body">
            <ATable
              class="rough-check-table console-record-table"
              :columns="recordColumns"
              :data-source="reportRecords"
              :pagination="false"
              :row-selection="reportRecordRowSelection"
          :scroll="{ x: 1950, y: 176 }"
              row-key="id"
              size="small"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.dataIndex === 'startPosition'">
                  {{ formatAdhesiveReportPosition(resolveAdhesiveReportPositionRange(record).start) }}
                </template>
                <template v-if="column.dataIndex === 'endPosition'">
                  {{ formatAdhesiveReportPosition(resolveAdhesiveReportPositionRange(record).end) }}
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
                <template v-if="column.dataIndex === 'action'">
                  <div class="record-action-group">
                    <Button size="small" type="link" :disabled="!record.id" @click.stop="openSubmittedReportDetail(record)">查看</Button>
                    <Button
                      v-access:code="['mes:sfc:adhesive-console-prototype:statistics-data:revise']"
                      size="small"
                      type="link"
                      :disabled="!canReviseStatisticsRecord(record)"
                      @click.stop="openStatisticsRevisionModal(record)"
                    >
                      修订统计
                    </Button>
                  </div>
                </template>
              </template>
            </ATable>
          </div>
        </div>
      </div>
    </TabPane>
    <TabPane v-if="showExtendedBoardTabs" key="INTERMEDIATE" tab="中间品记录单">
      <div class="tab-table-content">
          <div class="console-table-shell">
            <div class="rough-grid-toolbar console-record-toolbar">
            <div class="rough-grid-toolbar__title">中间品记录单</div>
            <div class="pass-work-actions rough-grid-toolbar__actions">
              <span class="console-table-count">共 {{ reportRecords.length }} 张</span>
            </div>
          </div>
          <div class="console-table-body">
            <ATable
              class="rough-check-table console-record-table"
              :columns="intermediateRecordColumns"
              :data-source="reportRecords"
              :pagination="false"
              :scroll="{ x: 1180, y: 176 }"
              row-key="id"
              size="small"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.dataIndex === 'outputLength'">
                  {{ formatNumber(record.outputLength || record.inputLength) }}
                </template>
                <template v-if="column.dataIndex === 'reportStatus'">
                  <Tag :color="getRecordStatusMeta(record.reportStatus).color" class="!m-0">
                    {{ getRecordStatusMeta(record.reportStatus).text }}
                  </Tag>
                </template>
                <template v-if="column.dataIndex === 'action'">
                  <div class="table-action-stack">
                    <Button size="small" type="link" :disabled="!record.id" @click="openIntermediateRecord(record, 'edit')">查看明细</Button>
                    <Button size="small" type="link" :disabled="isCurrentTaskReadonly || !record.id" @click="openIntermediateRecord(record, 'edit')">编辑修改</Button>
                  </div>
                </template>
              </template>
            </ATable>
          </div>
        </div>
      </div>
    </TabPane>
    <TabPane v-if="false" key="EDGE_CONSUMABLE" tab="胶板耗材余额">
      <div class="tab-table-content">
        <AdhesiveGlueBoardEdgeConsumableTab
          ref="glueBoardEdgeConsumableTabRef"
          :plan-no="currentPlan.planNo"
          :production-batch-no="currentPlan.sourceProductionBatchNo || currentPlan.batchNo || currentPlan.sourceBatchNo"
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
        title="粘胶1指令消息"
        @unread-change="handleProductionInstructionUnreadChange"
      />
    </TabPane>
  </Tabs>

      <AModal v-model:open="taskListVisible" :footer="null" :width="1280" class="rough-prototype-modal" :title="adhesiveTaskListTitle">
        <div class="console-table-shell console-table-shell--modal task-list-table-shell">
          <div class="rough-grid-toolbar console-record-toolbar">
            <div class="rough-grid-toolbar__title">{{ adhesiveTaskListTitle }}</div>
            <div class="pass-work-actions rough-grid-toolbar__actions">
              <span class="console-table-count">机台 {{ selectedBoardEquipmentCode || '未绑定' }}</span>
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
                placeholder="请输入产品型号"
                size="small"
                @press-enter="handleTaskListSearch"
              />
            </div>
            <div class="task-list-filter-item">
              <label>批次</label>
              <Input
                v-model:value="taskListFilters.batchNo"
                allow-clear
                placeholder="请输入分段批号/母卷批次"
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
              class="rough-check-table console-record-table adhesive-task-list-table"
              :columns="taskListColumns"
              :data-source="taskListPagedRows"
              :loading="taskListLoading"
              :pagination="false"
              :scroll="{ x: taskListScrollX, y: 366 }"
              :row-key="getTaskListRowKey"
              size="small"
              :custom-row="(record) => ({ onDblclick: () => startTaskFromList(record) })"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'sourceProductionBatchNo'">
              {{ resolveTaskSegmentBatchNo(record) || '-' }}
            </template>
            <template v-if="column.dataIndex === 'equipmentCode'">
              {{ record.equipmentCode || record.equipmentName || '未绑定' }}
            </template>
            <template v-if="column.dataIndex === 'availableSourceLength'">
              {{ formatNumber(record.availableSourceLength) }}
            </template>
            <template v-if="column.dataIndex === 'status'">
              <Tag :color="getWorkOrderStatusMeta(record.status).color">{{ getWorkOrderStatusMeta(record.status).text }}</Tag>
            </template>
            <template v-if="column.dataIndex === 'qtime'">
              <Tag :color="getAdhesiveQtimeTagColor(record.qtime)" :title="getAdhesiveQtimeText(record.qtime)">
                {{ getAdhesiveQtimeCompactText(record.qtime) }}
              </Tag>
            </template>
                <template v-if="column.dataIndex === 'action'">
                  <Button size="small" type="link" @click="startTaskFromList(record)">
                    {{ ['FINISHED', 'PAUSED', 'CANCELLED'].includes(normalizeWorkOrderStatus(record.status)) ? '查看' : '开工' }}
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

      <AModal
        v-model:open="equipmentSelectVisible"
        :footer="null"
        :width="860"
        class="rough-prototype-modal"
        title="切换粘胶机台"
        @cancel="closeEquipmentSelect"
      >
        <div class="console-table-shell console-table-shell--modal">
          <div class="rough-grid-toolbar console-record-toolbar">
            <div class="rough-grid-toolbar__title">当前机台：{{ selectedBoardEquipmentCode || '未绑定' }} / {{ selectedBoardEquipmentName || '-' }}</div>
            <div class="pass-work-actions rough-grid-toolbar__actions">
              <Input
                v-model:value="equipmentSelectKeyword"
                allow-clear
                class="equipment-select-filter"
                placeholder="按设备编码/名称搜索"
                size="small"
                @press-enter="loadEquipmentSelectRows"
              />
              <span class="console-table-count">设备 {{ equipmentSelectRows.length }} 台</span>
              <Button size="small" :loading="equipmentSelectLoading" @click="loadEquipmentSelectRows">刷新</Button>
            </div>
          </div>
          <div class="equipment-select-tip">
            选中机台后，今日点检/清洁记录按该设备和当天日期加载，待加工列表同步按设备过滤。
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

      <AModal
        v-model:open="segmentReportListVisible"
        :footer="null"
        :width="1180"
        :title="`分段 ${segmentReportListBatchNo} 报工记录（只读）`"
      >
        <div class="console-table-shell console-table-shell--modal">
          <div class="console-table-body">
            <ATable
              class="rough-check-table console-record-table"
              :columns="recordColumns"
              :data-source="segmentReportListRows"
              :pagination="false"
              :scroll="{ x: 1950, y: 420 }"
              row-key="id"
              size="small"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.dataIndex === 'startPosition'">
                  {{ formatAdhesiveReportPosition(resolveAdhesiveReportPositionRange(record).start) }}
                </template>
                <template v-if="column.dataIndex === 'endPosition'">
                  {{ formatAdhesiveReportPosition(resolveAdhesiveReportPositionRange(record).end) }}
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
                <template v-if="column.dataIndex === 'action'">
                  <div class="record-action-group">
                    <Button size="small" type="link" :disabled="!record.id" @click.stop="openSubmittedReportDetail(record, true)">查看</Button>
                  </div>
                </template>
              </template>
            </ATable>
          </div>
        </div>
      </AModal>

      <AModal v-model:open="reportRecordListVisible" :footer="null" :width="1180" title="今日粘胶报工记录">
        <div class="console-table-shell console-table-shell--modal">
          <div class="rough-grid-toolbar console-record-toolbar">
            <div class="rough-grid-toolbar__title">今日粘胶报工记录</div>
            <div class="pass-work-actions rough-grid-toolbar__actions">
              <span class="console-table-count">记录 {{ reportRecords.length }} 条 / 已选 {{ selectedReportRecords.length }} 条</span>
              <Button size="small" :disabled="isCurrentTaskReadonly || !reportRecords.length" @click="openSelectedRecordConfirm">扫码确认</Button>
              <Button size="small" :disabled="!printableReportRecords.length" @click="printSelectedAdhesiveRecords">选择打印</Button>
              <Button size="small" :disabled="!unprintedReportRecords.length" @click="printAllAdhesiveRecords">打印全部新报工</Button>
            </div>
          </div>
          <div class="console-table-body">
            <ATable
              class="rough-check-table console-record-table"
              :columns="recordColumns"
              :data-source="reportRecords"
              :pagination="false"
              :row-selection="reportRecordRowSelection"
              :scroll="{ x: 1950, y: 420 }"
              row-key="id"
              size="small"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.dataIndex === 'startPosition'">
                  {{ formatAdhesiveReportPosition(resolveAdhesiveReportPositionRange(record).start) }}
                </template>
                <template v-if="column.dataIndex === 'endPosition'">
                  {{ formatAdhesiveReportPosition(resolveAdhesiveReportPositionRange(record).end) }}
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
                <template v-if="column.dataIndex === 'action'">
                  <div class="record-action-group">
                    <Button size="small" type="link" :disabled="!record.id" @click.stop="openSubmittedReportDetail(record)">查看</Button>
                    <Button
                      v-access:code="['mes:sfc:adhesive-console-prototype:statistics-data:revise']"
                      size="small"
                      type="link"
                      :disabled="!canReviseStatisticsRecord(record)"
                      @click.stop="openStatisticsRevisionModal(record)"
                    >
                      修订统计
                    </Button>
                  </div>
                </template>
              </template>
            </ATable>
          </div>
        </div>
      </AModal>

      <AModal
        v-model:open="segmentPrintSelectorVisible"
        :closable="!segmentPrintSelectorLoading"
        :mask-closable="!segmentPrintSelectorLoading"
        :title="segmentPrintSelectorTitle || '选择要打印的粘胶流转单'"
        :width="980"
        class="rough-prototype-modal"
        @cancel="closeSegmentPrintSelector"
      >
        <div class="console-table-shell console-table-shell--modal segment-print-selector">
          <div class="segment-print-selector-tip">
            当前分段存在 {{ segmentPrintSelectorRows.length }} 次粘胶报工，请选择需要打印的流转单；票面产品批次不带 -J，二维码按“计划号,报工批次”区分。
          </div>
          <div class="console-table-body">
            <ATable
              class="rough-check-table console-record-table"
              :columns="segmentPrintSelectColumns"
              :data-source="segmentPrintSelectorRows"
              :pagination="false"
              :row-selection="segmentPrintSelectorRowSelection"
              :scroll="{ x: 1020, y: 360 }"
              row-key="id"
              size="small"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.dataIndex === 'sourceProductionBatchNo'">
                  {{ getAdhesiveTicketSegmentBatchNo(record) }}
                </template>
                <template v-if="column.dataIndex === 'productionBatchNo'">
                  {{ getAdhesiveTicketReportBatchNo(record) }}
                </template>
                <template v-if="column.dataIndex === 'positionRange'">
                  {{ formatAdhesiveReportPositionRange(record) }}
                </template>
                <template v-if="column.dataIndex === 'inputLength'">
                  {{ formatNumber(record.inputLength || record.outputLength) }}
                </template>
                <template v-if="column.dataIndex === 'outputLength'">
                  {{ formatNumber(record.outputLength) }}
                </template>
                <template v-if="column.dataIndex === 'printCount'">
                  {{ getRecordPrintMeta(record).count }}
                </template>
                <template v-if="column.dataIndex === 'printStatus'">
                  <Tag :color="getRecordPrintMeta(record).status === '已打印' ? 'success' : 'default'" class="!m-0">
                    {{ getRecordPrintMeta(record).status }}
                  </Tag>
                </template>
                <template v-if="column.dataIndex === 'reportStatus'">
                  <Tag :color="getRecordStatusMeta(record.reportStatus).color" class="!m-0">
                    {{ getRecordStatusMeta(record.reportStatus).text }}
                  </Tag>
                </template>
              </template>
            </ATable>
          </div>
        </div>
        <template #footer>
          <Button :disabled="segmentPrintSelectorLoading" @click="closeSegmentPrintSelector">取消</Button>
          <Button
            type="primary"
            :disabled="!segmentPrintSelectorSelectedRows.length"
            :loading="segmentPrintSelectorLoading"
            @click="confirmSegmentPrintSelection"
          >
            打印选中 {{ segmentPrintSelectorSelectedRows.length }} 张
          </Button>
        </template>
      </AModal>

      <AModal v-model:open="dailyRecordListVisible" :footer="null" :width="760" class="rough-prototype-modal" title="今日粘胶点检/清洁记录">
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
          <div v-if="dailyRecordRows.length === 0" class="console-empty compact">暂无点检清洁记录，请检查粘胶动态表单配置。</div>
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
      @cancel="closeIntermediateRecord()"
    >
        <div class="pp-plan-modal">
          <div class="pp-plan-toolbar">
            <div class="pp-plan-toolbar__title pass-work-dialog-title">
              <span class="pp-plan-toolbar__main">
                {{ dailyRecordMode === 'edit' ? '填写' : dailyRecordMode === 'confirm' ? '确认' : '查看' }}{{ selectedDailyRecord?.name || '工作准备记录' }}
              </span>
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

      <AModal v-model:open="glueConsumeVisible" :footer="null" :width="720" class="rough-prototype-modal" title="胶板领用登记">
        <div class="glue-consume-form">
          <div class="rough-material-scan-row">
            <div class="pp-form-item rough-material-scan-row__field">
              <label>胶板批号（选择/扫码）</label>
              <Input v-model:value="glueConsumeForm.materialScanCode" placeholder="扫码、输入或点击选择胶板边库批号" @press-enter="applyGlueBoardScan" />
            </div>
            <Button size="small" @click="openGlueStockSelect">选择批次</Button>
            <Button size="small" type="primary" @click="applyGlueBoardScan">带出边库批次</Button>
          </div>
          <div class="pp-form-grid rough-material-scan-grid">
            <div class="pp-form-item"><label>胶板型号</label><div class="pp-readonly-box">{{ getActualGlueBoardModel() || '-' }}</div></div>
            <div class="pp-form-item"><label>胶板批号</label><div class="pp-readonly-box">{{ glueConsumeForm.batchNo || '-' }}</div></div>
            <div class="pp-form-item"><label>边库余量</label><div class="pp-readonly-box">{{ glueBoardAvailableText }}</div></div>
            <div class="pp-form-item"><label>本次领用(m)</label><InputNumber v-model:value="glueConsumeForm.receiveLength" class="w-full" :min="0" :precision="3" disabled /></div>
          </div>
          <div v-if="glueBoard.alarm" class="record-scan-confirm__error">{{ glueBoard.alarm }}</div>
          <div class="modal-footer">
            <Button @click="glueConsumeVisible = false">取消</Button>
            <Button type="primary" @click="confirmGlueConsume">确认领用</Button>
          </div>
        </div>
      </AModal>

      <AModal v-model:open="glueStockSelectVisible" :footer="null" :width="860" class="rough-prototype-modal" title="选择胶板边库批次">
        <div class="console-table-shell console-table-shell--modal">
          <div class="rough-grid-toolbar console-record-toolbar">
            <div class="rough-grid-toolbar__title">
              胶板型号 {{ glueBoardMapCandidateModels.length ? glueBoardMapCandidateModels.join('、') : '未限定' }}
            </div>
            <div class="pass-work-actions rough-grid-toolbar__actions">
              <Input
                v-model:value="glueStockFilter.materialCode"
                allow-clear
                class="glue-stock-material-filter"
                placeholder="按胶板料号过滤"
                size="small"
                @press-enter="loadGlueStockRows({ resetPage: true })"
              />
              <Input
                v-model:value="glueStockFilter.model"
                allow-clear
                class="glue-stock-model-filter"
                placeholder="按胶板型号过滤"
                size="small"
                @press-enter="loadGlueStockRows({ resetPage: true })"
              />
              <Input
                v-model:value="glueStockFilter.batchNo"
                allow-clear
                class="glue-stock-batch-filter"
                placeholder="按胶板批号过滤"
                size="small"
                @press-enter="loadGlueStockRows({ resetPage: true })"
              />
              <span class="console-table-count">批次 {{ glueStockPagination.total }} 条</span>
              <Button size="small" :loading="glueStockSelectLoading" @click="loadGlueStockRows({ resetPage: true })">刷新</Button>
            </div>
          </div>
          <div class="glue-board-map-filter-tip" :class="{ warning: glueBoardMapCandidates.length === 0 }">
            <span>{{ glueBoardMapFilterNotice }}</span>
            <template v-if="glueBoardMapCandidates.length > 0">
              <Tag
                v-for="candidate in glueBoardMapCandidates"
                :key="`${candidate.glueBoardMaterialCode || ''}-${candidate.glueBoardModel || ''}`"
                color="processing"
              >
                {{ formatGlueBoardMapCandidate(candidate) || '-' }}
              </Tag>
            </template>
          </div>
          <div class="console-table-body">
            <ATable
              class="rough-check-table console-record-table"
              :columns="glueBoardStockSelectColumns"
              :custom-row="(record) => ({ onDblclick: () => selectGlueBoardStock(record) })"
              :data-source="glueStockRows"
              :loading="glueStockSelectLoading"
              :pagination="false"
              :scroll="{ x: 980, y: 360 }"
              row-key="id"
              size="small"
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

    <AModal v-model:open="aqcVisible" :footer="null" :width="720" class="rough-prototype-modal" title="胶板送检">
        <div class="glue-consume-form">
          <div class="pp-form-grid rough-material-scan-grid">
            <div class="pp-form-item"><label>胶板型号</label><div class="pp-readonly-box">{{ getActualGlueBoardModel() || '-' }}</div></div>
            <div class="pp-form-item"><label>胶板批号</label><div class="pp-readonly-box">{{ aqcForm.glueBoardBatchNo || glueBoard.batchNo || '-' }}</div></div>
            <div class="pp-form-item"><label>当前可用</label><div class="pp-readonly-box">{{ glueBoardAvailableText }}</div></div>
            <div class="pp-form-item"><label>取样起点(m)</label><InputNumber v-model:value="aqcForm.sampleStartPosition" class="w-full" :min="0" :precision="3" /></div>
            <div class="pp-form-item"><label>送检长度(m)</label><InputNumber v-model:value="aqcForm.sampleLength" class="w-full" :min="0" :max="getGlueBoardFaiMaxSampleLength() || undefined" :precision="3" /></div>
            <div class="pp-form-item"><label>FAI状态</label><div class="pp-readonly-box">{{ glueBoardAqcStatusMeta.stampText }} / {{ glueBoardAqcStatusMeta.subText }}</div></div>
          </div>
          <div class="glue-board-message">
            本次送检会创建真实 QMS 胶板检验记录，并同步回写胶板边库最近检验信息；今日已送检且非 NG 时不能重复提交。
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
            <div class="pp-form-item"><label>胶板料号</label><div class="pp-readonly-box">{{ glueBoard.materialCode || '-' }}</div></div>
            <div class="pp-form-item"><label>胶板批号</label><div class="pp-readonly-box">{{ glueBoard.batchNo || '-' }}</div></div>
            <div class="pp-form-item"><label>边库余量</label><div class="pp-readonly-box">{{ glueBoardAvailableText }}</div></div>
            <div class="pp-form-item"><label>已报损耗</label><div class="pp-readonly-box">{{ formatNumber(glueBoard.lossLength) }} m</div></div>
            <div class="pp-form-item"><label>损耗长度(m)</label><InputNumber v-model:value="glueLossForm.lossLength" class="w-full" :min="0" :precision="3" /></div>
            <div class="pp-form-item pp-form-item--span-2"><label>损耗原因</label><Input v-model:value="glueLossForm.lossReason" placeholder="请输入损耗原因" /></div>
          </div>
          <div class="modal-footer">
            <Button @click="glueLossVisible = false">取消</Button>
            <Button type="primary" @click="submitGlueBoardLoss">确认保存</Button>
          </div>
        </div>
      </AModal>

      <AModal
        v-model:open="recordConfirmVisible"
        :footer="null"
        :keyboard="false"
        :mask-closable="false"
        :width="680"
        class="rough-prototype-modal"
        destroy-on-close
        title="粘胶报工扫码确认"
      >
          <div class="record-confirm-panel">
            <div class="record-confirm-summary">
            <span>母卷批次</span><strong>{{ activeRecord ? resolveMotherBatchNo(activeRecord) : '-' }}</strong>
            <span>胶板批号</span><strong>{{ activeRecord?.glueBoardBatchNo || glueBoard.batchNo || '-' }}</strong>
          </div>
          <div v-if="recordConfirmForm.matchedBatchNo" class="record-confirm-matched-row">
            <span>本次确认批次</span>
            <strong>{{ recordConfirmForm.matchedBatchNo }}</strong>
          </div>
          <div class="record-confirm-scan">
            <IconifyIcon icon="lucide:scan-line" />
            <Input
              ref="recordConfirmScanInputRef"
              v-model:value="recordConfirmForm.scannedBatchNo"
              :disabled="isCurrentTaskReadonly"
              allow-clear
              autofocus
              data-adhesive-confirm-scan
              placeholder="请扫描当前母卷+段的批次号，如 W26F035AP，不带 -J1"
              @press-enter="confirmRecordScan"
            />
      </div>
      <div v-if="recordConfirmForm.error" class="record-scan-confirm__error">{{ recordConfirmForm.error }}</div>
      <div v-if="recordConfirmForm.message" class="record-scan-confirm__message">{{ recordConfirmForm.message }}</div>
      <div class="modal-footer">
            <Button @click="recordConfirmVisible = false">取消</Button>
            <Button type="primary" :disabled="isCurrentTaskReadonly" @click="confirmRecordScan">确认扫码</Button>
          </div>
        </div>
      </AModal>

      <AModal
        v-model:open="intermediateVisible"
        :footer="null"
        :title="null"
        width="100vw"
        wrap-class-name="hc-pass-work-modal rough-prep-work-modal"
      >
        <div class="pp-plan-modal adhesive-middle-modal">
          <div class="pp-plan-toolbar">
            <div class="pp-plan-toolbar__title pass-work-dialog-title">
              <span class="pp-plan-toolbar__main">{{ intermediateMode === 'view' ? '查看' : '填写' }}{{ intermediateDisplayName }}</span>
              <div class="toolbar-meta">
                <span>报工批次：{{ intermediateForm.batchNo || '-' }}</span>
                <span>报工产出：{{ formatNumber(intermediateForm.processLength) }} m</span>
                <span>
                  状态：
                  <Tag :color="getIntermediateStatusMeta(intermediateForm.recordStatus).color" class="!m-0">
                    {{ getIntermediateStatusMeta(intermediateForm.recordStatus).text }}
                  </Tag>
                </span>
              </div>
            </div>
            <div class="pp-plan-toolbar__actions">
          <input ref="intermediateImportInputRef" accept=".xls,.xlsx" class="hidden-input" type="file" @change="handleIntermediateImportFile" />
          <Button size="small" @click="handleIntermediateExport">导出Excel</Button>
          <Button v-if="!intermediateReadonly" size="small" @click="addIntermediateDetailRow">
            <IconifyIcon icon="lucide:plus" class="mr-1" />
            增加行
          </Button>
          <Button v-if="!intermediateReadonly" size="small" @click="handleIntermediateImportClick">导入Excel</Button>
              <Button v-if="!intermediateReadonly" size="small" type="primary" :loading="intermediateLoading" @click="saveIntermediateRecord">保存</Button>
              <Button
                v-if="intermediateCanConfirm"
                size="small"
                type="primary"
                :loading="intermediateLoading"
                @click="openIntermediateConfirmAuth"
              >
                确认
              </Button>
          <Button size="small" @click="closeIntermediateRecord()">关闭</Button>
            </div>
          </div>
          <div class="pp-plan-body middle-product-detail-body adhesive-middle-body">
            <fieldset class="pp-fieldset">
              <legend>主表信息</legend>
              <div class="middle-product-header">
                <label>生产日期</label>
                <strong class="middle-product-header-input">
                  <DatePicker
                    v-model:value="intermediateForm.productionDate"
                    :disabled="intermediateReadonly"
                    format="YYYY-MM-DD HH:mm:ss"
                    show-time
                    value-format="YYYY-MM-DD HH:mm:ss"
                    size="small"
                  />
                </strong>
                <label>记录时间</label>
                <strong class="middle-product-header-input">
                  <DatePicker
                    v-model:value="intermediateForm.recordTime"
                    :disabled="intermediateReadonly"
                    format="YYYY-MM-DD HH:mm:ss"
                    show-time
                    value-format="YYYY-MM-DD HH:mm:ss"
                    size="small"
                  />
                </strong>
                <label>型号</label><strong>{{ intermediateForm.modelCode || '-' }}</strong>
                <label>料号</label><strong>{{ intermediateForm.materialCode || '-' }}</strong>
                <label>批号</label><strong>{{ intermediateForm.batchNo || '-' }}</strong>
                <label>加工长度(m)</label>
                <strong class="middle-product-header-input">
                  <InputNumber v-model:value="intermediateForm.processLength" :disabled="intermediateReadonly" :min="0" :precision="3" size="small" />
                </strong>
                <label>产品宽幅(mm)</label>
                <strong class="middle-product-header-input">
                  <InputNumber v-model:value="intermediateForm.productWidthMm" :disabled="intermediateReadonly" :min="0" :precision="3" size="small" />
                </strong>
                <label>开头</label>
                <strong class="middle-product-header-input">
                  <InputNumber v-model:value="intermediateForm.widthStart" :disabled="intermediateReadonly" :min="0" :precision="3" size="small" />
                </strong>
                <label>中间</label>
                <strong class="middle-product-header-input">
                  <InputNumber v-model:value="intermediateForm.widthMiddle" :disabled="intermediateReadonly" :min="0" :precision="3" size="small" />
                </strong>
                <label>结尾</label>
                <strong class="middle-product-header-input">
                  <InputNumber v-model:value="intermediateForm.widthEnd" :disabled="intermediateReadonly" :min="0" :precision="3" size="small" />
                </strong>
                <label>担当</label><strong>{{ intermediateForm.recorderName || '-' }}</strong>
                <label>确认</label><strong>{{ intermediateForm.confirmerName || '-' }}</strong>
              </div>
            </fieldset>

            <fieldset v-if="intermediateNotes.length" class="pp-fieldset">
              <legend>表单说明</legend>
              <div v-for="note in intermediateNotes" :key="note.key" style="white-space: pre-wrap; padding: 4px 8px;">
                <strong>{{ note.label }}：</strong>{{ note.text }}
              </div>
            </fieldset>
            <div class="adhesive-intermediate-attachment">
              <div class="adhesive-intermediate-attachment__title">原始导入附件</div>
              <div v-if="intermediateImportAttachment" class="adhesive-intermediate-attachment__content">
                <button class="adhesive-intermediate-attachment__link" type="button" @click="openIntermediateAttachment(intermediateImportAttachment)">
                  <IconifyIcon icon="lucide:paperclip" />
                  <span>{{ intermediateImportAttachment.name || '原始导入文件' }}</span>
                </button>
                <span>导入时间：{{ intermediateImportAttachment.uploadTime || '-' }}</span>
                <span v-if="formatIntermediateAttachmentSize(intermediateImportAttachment.size)">
                  大小：{{ formatIntermediateAttachmentSize(intermediateImportAttachment.size) }}
                </span>
              </div>
              <span v-else class="adhesive-intermediate-attachment__empty">暂无原始导入附件</span>
            </div>

            <div class="middle-product-detail-table">
              <ATable
                class="rough-check-table"
                :columns="intermediateDetailColumns"
                :data-source="intermediatePagedDetails"
                :loading="intermediateLoading"
                :pagination="false"
                :scroll="{ x: 1180, y: 'calc(100vh - 318px)' }"
                row-key="seq"
                size="small"
              >
                <template #bodyCell="{ column, record }">
                  <template v-if="column.dataIndex === 'lengthMark'">
                    <InputNumber v-model:value="record.lengthMark" :disabled="intermediateReadonly" class="adhesive-intermediate-cell full-input" :min="0" :precision="3" size="small" @keydown="handleIntermediateCellKeydown" />
                  </template>
                  <template v-if="column.dataIndex === 'leftThickness'">
                    <InputNumber v-model:value="record.leftThickness" :disabled="intermediateReadonly" class="adhesive-intermediate-cell full-input" :min="0" :precision="3" size="small" @keydown="handleIntermediateCellKeydown" />
                  </template>
                  <template v-if="column.dataIndex === 'rightThickness'">
                    <InputNumber v-model:value="record.rightThickness" :disabled="intermediateReadonly" class="adhesive-intermediate-cell full-input" :min="0" :precision="3" size="small" @keydown="handleIntermediateCellKeydown" />
                  </template>
                  <template v-if="column.dataIndex === 'remark'">
                    <Input v-model:value="record.remark" :disabled="intermediateReadonly" class="adhesive-intermediate-cell" size="small" @keydown="handleIntermediateCellKeydown" />
                  </template>
                </template>
              </ATable>
              <div class="middle-product-pagination">
                <Pagination
                  v-model:current="intermediatePage"
                  :page-size="INTERMEDIATE_PAGE_SIZE"
                  :show-size-changer="false"
                  :total="intermediateDetails.length"
                  show-less-items
                  size="small"
                />
              </div>
            </div>
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
            <legend>粘胶1工艺参数报工登记</legend>
            <div class="report-toolbar-form">
              <label>粘胶批次</label>
              <strong class="report-toolbar-value report-toolbar-value--batch">{{ reportForm.sourceProductionBatchNo || reportForm.sourceCode || '-' }}</strong>
              <label>胶板本次后</label>
              <strong class="report-toolbar-value">
                本次消耗 {{ formatNumber(glueBoardConsumeLength) }} m，余量 {{ formatNumber(glueBoardAfterStock) }} m，今日使用 {{ formatNumber(glueBoardAfterTodayUsed) }} m
                <Tag :color="glueBoard.materialCode ? 'green' : 'default'">
                  {{ glueBoard.materialCode ? '正常' : '未加载' }}
                </Tag>
              </strong>
            </div>
          </fieldset>
          <Tabs v-model:active-key="activeReportTab" class="rough-report-tabs">
            <TabPane key="scan" tab="来源扫码">
              <div class="rough-material-scan-row">
                <div class="pp-form-item rough-material-scan-row__field">
                  <label>粘胶批次（扫码/输入）</label>
                  <Input
                    v-model:value="reportForm.sourceCode"
                    :disabled="reportFormReadonly"
                    placeholder="扫描第二次磨皮已确认分段批号"
                    @press-enter="scanReportSource"
                  />
                </div>
                <Button size="small" type="primary" :disabled="reportFormReadonly" @click="scanReportSource">查询带出</Button>
              </div>
              <div class="rough-material-scan-hint">
                布局按磨皮报工登记执行：先扫码确认来源，再填写工艺参数，最后提交报工。
              </div>
              <div v-if="reportForm.sourceScanError" class="record-scan-confirm__error">{{ reportForm.sourceScanError }}</div>
              <div v-if="reportForm.sourceScanMessage" class="record-scan-confirm__message">{{ reportForm.sourceScanMessage }}</div>
              <div class="pp-form-grid rough-material-scan-grid">
                <div class="pp-form-item"><label>粘胶批次</label><Input v-model:value="reportForm.sourceProductionBatchNo" disabled /></div>
                <div class="pp-form-item"><label>来源母批</label><Input v-model:value="reportForm.parentBatchNo" disabled /></div>
                <div class="pp-form-item"><label>来源明细ID</label><Input :value="reportForm.sourceGrindingSecondDetailId || '-'" disabled /></div>
              </div>
            </TabPane>
            <TabPane key="dev-process-param" tab="工艺参数点检表">
              <div class="report-tab-stack">
                <fieldset class="erp-fieldset adhesive-dev-param-fieldset">
                  <legend>粘胶1工艺参数点检表</legend>
                  <div class="adhesive-dev-param-toolbar">
                    <div class="adhesive-dev-param-toolbar__meta">
                      <span>型号：{{ currentPlan.modelCode || '-' }}</span>
                      <span>模板：{{ adhesiveDevProcessParamForm?.formName || '按型号自动匹配' }}</span>
                      <span>计划号：{{ getAdhesiveDevExpectedPlanNo() || '-' }}</span>
                      <span>批号：{{ getAdhesiveDevExpectedBatchNo() || '-' }}</span>
                    </div>
                    <div class="adhesive-dev-param-toolbar__actions">
                      <Button size="small" @click="loadAdhesiveDevProcessParamRecords">
                        <IconifyIcon icon="lucide:refresh-cw" class="mr-1" />
                        刷新
                      </Button>
                      <Button size="small" type="primary" :disabled="reportDetailViewOnly || isCurrentTaskReadonly" @click="openAdhesiveDevProcessParamFill">
                        <IconifyIcon icon="lucide:plus" class="mr-1" />
                        新建上报
                      </Button>
                    </div>
                  </div>
                <ATable
                  class="rough-check-table adhesive-dev-param-table"
                  :columns="adhesiveDevProcessParamColumns"
                  :data-source="adhesiveDevProcessParamRecords"
                  :loading="adhesiveDevProcessParamLoading"
                  :pagination="false"
                  row-key="id"
                  size="small"
                >
                  <template #bodyCell="{ column, record }">
                    <template v-if="column.dataIndex === 'recordStatus'">
                      <Tag :color="getAdhesiveDevRecordStatusMeta(record.recordStatus).color">
                        {{ getAdhesiveDevRecordStatusMeta(record.recordStatus).text }}
                      </Tag>
                    </template>
                    <template v-else-if="column.dataIndex === 'fillInfo'">
                      {{ record.fillUserName || record.creator || '-' }}
                      <span class="adhesive-dev-param-fill-time">{{ record.fillTime || record.createTime || '' }}</span>
                    </template>
                    <template v-else-if="column.dataIndex === 'confirmResult'">
                      {{ getAdhesiveDevConfirmResult(record) }}
                    </template>
                    <template v-else-if="column.dataIndex === 'confirmInfo'">
                      {{ getAdhesiveDevConfirmUser(record) }}
                      <span class="adhesive-dev-param-fill-time">{{ getAdhesiveDevConfirmTime(record) }}</span>
                    </template>
                    <template v-else-if="column.dataIndex === 'action'">
                      <Button size="small" type="link" @click="openAdhesiveDevProcessParamDetail(record)">查看</Button>
                      <Button
                        v-if="isAdhesiveDevProcessParamDraft(record)"
                        size="small"
                        type="link"
                        :disabled="reportDetailViewOnly || isCurrentTaskReadonly"
                        @click="openAdhesiveDevProcessParamDetail(record, 'edit')"
                      >
                        修改
                      </Button>
                      <Button
                        v-if="!isAdhesiveDevProcessParamConfirmed(record)"
                        size="small"
                        type="link"
                        :disabled="reportDetailViewOnly || isCurrentTaskReadonly"
                        :loading="adhesiveDevProcessParamConfirming"
                        @click="confirmAdhesiveDevProcessParamRecord(record)"
                      >
                        确认
                      </Button>
                    </template>
                  </template>
                </ATable>
                  <div v-if="!adhesiveDevProcessParamLoading && !adhesiveDevProcessParamRecords.length" class="adhesive-dev-param-empty">
                    暂无粘胶1工艺参数点检表记录，可点击“新建上报”填写动态表单。
                  </div>
                </fieldset>
              </div>
            </TabPane>
            <TabPane key="report" tab="报工收卷">
              <div class="report-tab-stack report-tab-stack--report">
                <div class="report-tab-scroll-body">
                  <fieldset class="erp-fieldset">
                    <legend>报工信息</legend>
                    <Form layout="vertical" class="report-form-grid">
                      <FormItem label="粘胶批次"><Input v-model:value="reportForm.sourceProductionBatchNo" disabled /></FormItem>
                      <FormItem label="起始位置(m)" class="report-required-field"><InputNumber v-model:value="reportForm.startPosition" :disabled="reportFormReadonly" :min="0" :precision="3" class="full-input" /></FormItem>
                      <FormItem label="结束位置(m)" class="report-required-field"><InputNumber v-model:value="reportForm.endPosition" :disabled="reportFormReadonly" :min="0" :precision="3" class="full-input" /></FormItem>
                      <FormItem label="加工米数(m)" class="report-required-field"><InputNumber :value="reportForm.processLength" disabled :precision="3" class="full-input report-calculated-input" /></FormItem>
                      <FormItem>
                        <template #label>
                          固定损耗-<span class="report-loss-sample-emphasis">包含小样条</span>（米）
                        </template>
                        <div class="report-loss-with-abnormal">
                          <InputNumber v-model:value="reportForm.lossLength" :disabled="reportFormReadonly" :min="0" :precision="3" class="full-input" />
                          <Button size="small" :disabled="reportFormReadonly" @click="openReportAbnormalTab">
                            <IconifyIcon icon="lucide:map-pin" class="mr-1" />
                            异常位置
                          </Button>
                          <span>{{ formatReportAbnormalPositionSummary(reportAbnormalRows) }}</span>
                        </div>
                      </FormItem>
                      <FormItem label="送检米数">
                        <div class="nap-sample-field">
                          <InputNumber v-model:value="reportForm.napSampleLength" :disabled="reportFormReadonly" :min="0" :precision="3" class="full-input" />
                          <div class="nap-sample-hint" :class="{ 'nap-sample-hint--warn': reportNapSampleSummary.reportedLength > 0 }">
                            本段已送检 {{ formatNumber(reportNapSampleSummary.reportedLength) }} m
                            <template v-if="reportNapSampleSummary.inspectionLength > 0">
                              ，送检单记录 {{ formatNumber(reportNapSampleSummary.inspectionLength) }} m
                            </template>
                            <template v-if="reportNapSampleSummary.pendingLength > 0">
                              ，待回填 {{ formatNumber(reportNapSampleSummary.pendingLength) }} m
                            </template>
                            <template v-if="reportNapSampleSummary.reportedLength > 0">，后续报工请勿重复填写</template>
                          </div>
                        </div>
                      </FormItem>
                      <FormItem label="产出米数(m)"><InputNumber :value="reportOutputLength" disabled class="full-input" /></FormItem>
                      <FormItem label="胶板消耗米数(m)"><InputNumber :value="glueBoardConsumeLength" disabled class="full-input report-calculated-input" /></FormItem>
                      <FormItem label="胶板料号"><Input v-model:value="reportForm.glueBoardMaterialCode" :disabled="reportFormReadonly" /></FormItem>
                      <FormItem label="胶板批号"><Input v-model:value="reportForm.glueBoardBatchNo" :disabled="reportFormReadonly" /></FormItem>
                      <FormItem label="生产日期">
                        <DatePicker
                          v-model:value="reportForm.reportDate"
                          class="full-input report-time-edit-field"
                          :disabled="reportTimeReadonly"
                          value-format="YYYY-MM-DD"
                        />
                      </FormItem>
                      <FormItem label="开始时间">
                        <DatePicker
                          v-model:value="reportForm.startTime"
                          class="full-input report-time-edit-field"
                          :disabled="!reportTimeEditing"
                          show-time
                          value-format="YYYY-MM-DD HH:mm:ss"
                        />
                      </FormItem>
                      <FormItem label="结束时间">
                        <DatePicker
                          v-model:value="reportForm.endTime"
                          class="full-input report-time-edit-field"
                          :disabled="!reportTimeEditing"
                          show-time
                          value-format="YYYY-MM-DD HH:mm:ss"
                        />
                      </FormItem>
                      <FormItem label="自检">
                        <RadioGroup v-model:value="reportForm.selfCheck" :disabled="reportFormReadonly"><Radio value="OK">OK</Radio><Radio value="NG">NG</Radio></RadioGroup>
                      </FormItem>
                      <FormItem label="不良代码"><Select v-model:value="reportForm.defectCode" :disabled="reportFormReadonly" :options="defectOptions" class="full-input" /></FormItem>
                      <FormItem label="备注" class="report-remark"><Input v-model:value="reportForm.remark" :disabled="reportFormReadonly" /></FormItem>
                    </Form>
                    <div class="report-output-hint">
                      产出米数 = 加工米数 - 固定损耗-<span class="report-loss-sample-emphasis">包含小样条</span>（米） - 送检米数
                    </div>
                  </fieldset>
                  <div class="second-range-visual">
                    <div class="second-range-visual__head">
                      <strong>粘胶区间占用</strong>
                      <span>当前区间：{{ reportRangeText }}</span>
                      <span>来源总米数：{{ formatNumber(reportRangeTotal) }}</span>
                      <span>可加工：{{ formatNumber(findSourceSegment(reportForm.sourceProductionBatchNo)?.availableLength) }} m</span>
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
            <TabPane key="abnormal-position" tab="异常位置">
              <div class="report-tab-stack">
                <fieldset class="erp-fieldset">
                  <legend>固定损耗异常位置</legend>
                  <div class="abnormal-position-dialog">
                    <div class="abnormal-position-toolbar">
                      <span>
                        粘胶批次：{{ reportForm.sourceProductionBatchNo || '-' }}　固定损耗-包含小样条：{{ formatNumber(reportForm.lossLength) }} m
                      </span>
                      <Button v-if="!reportFormReadonly" size="small" type="primary" @click="addReportAbnormalRow">
                        <IconifyIcon icon="lucide:plus" class="mr-1" />
                        新增
                      </Button>
                    </div>
                    <table class="pp-grid abnormal-position-edit-table">
                      <thead>
                        <tr>
                          <th width="64">序号</th>
                          <th>异常位置</th>
                          <th width="160">米数</th>
                          <th>备注</th>
                          <th width="80">操作</th>
                        </tr>
                      </thead>
                      <tbody>
                        <tr v-for="(row, index) in reportAbnormalRows" :key="row.clientKey">
                          <td align="center">{{ index + 1 }}</td>
                          <td>
                            <Input v-model:value="row.positionText" :disabled="reportFormReadonly" placeholder="请输入异常位置" />
                          </td>
                          <td>
                            <InputNumber v-model:value="row.abnormalLength" :disabled="reportFormReadonly" :min="0" :precision="3" class="full-input" />
                          </td>
                          <td>
                            <Input v-model:value="row.remark" :disabled="reportFormReadonly" placeholder="备注" />
                          </td>
                          <td align="center">
                            <Button v-if="!reportFormReadonly" danger size="small" type="link" @click="removeReportAbnormalRow(index)">删除</Button>
                          </td>
                        </tr>
                        <tr v-if="!reportAbnormalRows.length">
                          <td colspan="5" class="pp-empty-cell" align="center">暂无异常位置明细</td>
                        </tr>
                      </tbody>
                    </table>
                  </div>
                </fieldset>
              </div>
            </TabPane>
          </Tabs>
        </div>
        <div class="modal-footer report-action-footer">
          <Button @click="requestCloseReport">{{ reportTimeEditing ? '取消修改' : reportReadonly ? '关闭' : '取消' }}</Button>
          <Button
            v-if="canEditReadonlyReportTime && !reportTimeEditing"
            v-access:code="['mes:sfc:adhesive-console-prototype:report-time:update']"
            type="primary"
            @click="startEditReadonlyReportTime"
          >
            修订开工完工时间
          </Button>
          <Button v-if="reportTimeEditing" type="primary" :loading="reportTimeSaving" @click="saveReadonlyReportTime">
            保存时间
          </Button>
          <Button v-if="!reportReadonly && reportStep === 'SCAN'" type="primary" :disabled="isCurrentTaskReadonly" @click="startReportWork">进入工艺参数点检表</Button>
          <Button v-if="!reportReadonly && reportStep === 'PROCESS'" type="primary" :disabled="isCurrentTaskReadonly" @click="submitProcessItems">进入报工收卷</Button>
          <Button v-if="!reportReadonly && reportStep === 'REPORT'" type="primary" :disabled="isCurrentTaskReadonly" @click="submitAdhesiveReport">提交报工收卷</Button>
        </div>
      </AModal>

      <AModal
        :open="statisticsRevisionVisible"
        :confirm-loading="statisticsRevisionSubmitting"
        :mask-closable="!statisticsRevisionSubmitting"
        :width="620"
        :z-index="3260"
        destroy-on-close
        title="修订粘胶1统计数据"
        @cancel="closeStatisticsRevisionModal"
        @ok="handleStatisticsRevisionConfirm"
      >
        <div class="statistics-revision-head">
          <span>粘胶批次：{{ statisticsRevisionRecord?.sourceProductionBatchNo || statisticsRevisionRecord?.productionBatchNo || '-' }}</span>
          <span>报工批次：{{ statisticsRevisionRecord?.productionBatchNo || '-' }}</span>
        </div>
        <Form layout="vertical" class="statistics-revision-form">
          <FormItem label="投入米数(m)" required>
            <InputNumber v-model:value="statisticsRevisionForm.inputLength" :min="0" :precision="3" class="full-input" />
          </FormItem>
          <FormItem label="固定损耗(m)" required>
            <InputNumber v-model:value="statisticsRevisionForm.lossLength" :min="0" :precision="3" class="full-input" />
          </FormItem>
          <FormItem label="产出米数(m)" required>
            <InputNumber v-model:value="statisticsRevisionForm.outputLength" :min="0" :precision="3" class="full-input" />
          </FormItem>
          <FormItem label="修订原因" required class="statistics-revision-form__reason">
            <Input v-model:value="statisticsRevisionForm.reason" :maxlength="200" placeholder="请填写修订原因" />
          </FormItem>
        </Form>
      </AModal>

      <StationFormRuntimeFillModal
        v-model:open="adhesiveDevProcessParamFillOpen"
        :form="adhesiveDevProcessParamForm"
        :initial-params="adhesiveDevProcessParamInitialParams"
        skip-business-param-step
        @success="handleAdhesiveDevProcessParamSaved"
      />

      <AuthModal
        v-model:visible="dailyRecordAuthVisible"
        :actionName="dailyRecordAuthAction"
        authMode="username"
        :equipment-id="selectedBoardEquipmentId"
        :workstation="selectedBoardEquipmentCode || selectedBoardEquipmentName"
        @cancel="handleDailyRecordAuthCancel"
        @success="handleDailyRecordAuthSuccess"
      />
      <AuthModal
        v-model:visible="segmentTimingAuthVisible"
        :actionName="segmentTimingAuthActionName"
        authMode="username"
        :equipment-id="selectedBoardEquipmentId"
        :workstation="selectedBoardEquipmentCode || selectedBoardEquipmentName || '粘胶1工位'"
        @cancel="handleSegmentTimingAuthCancel"
        @success="handleSegmentTimingAuthSuccess"
      />
      <AuthModal
        v-model:visible="intermediateAuthVisible"
        :actionName="intermediateAuthAction"
        authMode="username"
        :workstation="selectedBoardEquipmentCode || selectedBoardEquipmentName || '粘胶1工位'"
        @success="handleIntermediateAuthSuccess"
      />
      <AuthModal
        v-model:visible="adhesiveDevProcessParamAuthVisible"
        action-name="确认粘胶1工艺参数点检表"
        auth-mode="username"
        :workstation="selectedBoardEquipmentCode || selectedBoardEquipmentName || '粘胶1工位'"
        @success="handleAdhesiveDevProcessParamAuthSuccess"
      />

      <AuthModal
        v-model:visible="segmentCompleteAuthVisible"
        :actionName="segmentCompleteAuthActionName"
        authMode="username"
        title="确认本段是否确认完工？"
        @cancel="handleSegmentCompleteAuthCancel"
        @success="handleSegmentCompleteAuthSuccess"
      />

      <AModal
        :open="adhesiveDevProcessParamViewVisible"
        :footer="null"
        :title="null"
        :z-index="3220"
        width="100vw"
        wrap-class-name="hc-pass-work-modal adhesive-dev-process-param-view-modal"
        @cancel="closeAdhesiveDevProcessParamDetail"
      >
        <div class="adhesive-dev-param-runtime-view">
          <div class="adhesive-dev-param-runtime-view__header">
            <strong>{{ adhesiveDevProcessParamViewMode === 'edit' ? '修改' : '查看' }}粘胶1工艺参数点检表</strong>
            <span v-if="adhesiveDevProcessParamViewRecord" class="adhesive-dev-param-runtime-view__status">
              {{ getAdhesiveDevRecordStatusMeta(adhesiveDevProcessParamViewRecord.recordStatus).text }}
            </span>
            <input
              ref="adhesiveDevProcessParamImportInputRef"
              accept=".xls,.xlsx"
              class="hidden-input"
              type="file"
              @change="handleAdhesiveDevProcessParamImportExcel"
            />
            <Button
              v-if="adhesiveDevProcessParamViewRecord"
              :loading="adhesiveDevProcessParamExcelLoading"
              @click="exportAdhesiveDevProcessParamExcel"
            >
              导出Excel
            </Button>
            <Button
              v-if="canImportAdhesiveDevProcessParamExcel()"
              :loading="adhesiveDevProcessParamExcelLoading"
              @click="triggerAdhesiveDevProcessParamImportExcel"
            >
              导入Excel
            </Button>
            <Button
              v-if="adhesiveDevProcessParamViewMode === 'edit' && adhesiveDevProcessParamViewRecord && isAdhesiveDevProcessParamDraft(adhesiveDevProcessParamViewRecord)"
              :loading="adhesiveDevProcessParamSaving"
              type="primary"
              @click="saveAdhesiveDevProcessParamView()"
            >
              保存
            </Button>
            <Button
              v-if="adhesiveDevProcessParamViewRecord && !isAdhesiveDevProcessParamConfirmed(adhesiveDevProcessParamViewRecord)"
              :loading="adhesiveDevProcessParamConfirming"
              danger
              type="primary"
              @click="confirmAdhesiveDevProcessParamRecord(adhesiveDevProcessParamViewRecord)"
            >
              确认
            </Button>
            <Button @click="closeAdhesiveDevProcessParamDetail">关闭</Button>
          </div>
          <div class="adhesive-dev-param-runtime-view__body">
            <StationFormRuntimeRenderer
              v-if="adhesiveDevProcessParamViewRecord && !adhesiveDevProcessParamViewLoading"
              ref="adhesiveDevProcessParamRuntimeRef"
              v-model:header-data="adhesiveDevProcessParamViewHeaderData"
              form-name="粘胶1工艺参数点检表"
              :items="adhesiveDevProcessParamViewItems"
              :readonly="adhesiveDevProcessParamViewMode !== 'edit' || !isAdhesiveDevProcessParamDraft(adhesiveDevProcessParamViewRecord)"
              :record-meta="adhesiveDevProcessParamViewMetaItems"
              :schema="adhesiveDevProcessParamViewSchema"
            />
            <div v-else class="adhesive-dev-param-runtime-loading">正在加载粘胶1工艺参数点检表详情...</div>
          </div>
        </div>
      </AModal>
    </div>
    <FaiDetailPreviewModal />
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

.adhesive-console.is-source-maximized {
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

.console-meta-action {
  flex: 0 0 auto;
  height: 20px;
  padding: 0 4px;
  margin-left: 6px;
  font-size: 12px;
  font-weight: 900;
  line-height: 20px;
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

.adhesive-status-stamp-slot {
  flex-basis: clamp(288px, 18vw, 342px);
  gap: 8px;
}

.adhesive-status-stamp-slot .inspection-stamp-side {
  width: clamp(136px, 8.2vw, 166px);
}

.adhesive-qtime-stamp {
  cursor: default;
}

.adhesive-qtime-stamp .inspection-stamp-content span {
  letter-spacing: 0.08em;
}

.adhesive-qtime-stamp .inspection-stamp-content strong {
  font-size: 14px;
}

.adhesive-qtime-stamp .inspection-stamp-content em {
  max-width: 118px;
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

.console-tabs--single :deep(.ant-tabs-nav) {
  display: none;
}

.console-tabs :deep(.ant-tabs-tab) {
  color: #334155;
  font-weight: 800;
}

.tab-maximize-button {
  display: inline-flex;
  gap: 4px;
  align-items: center;
  color: #0f5f99;
  font-weight: 700;
  background: #e9f2fa;
  border-color: #8fb3d6;
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

.source-board-list {
  display: grid;
  grid-auto-rows: minmax(300px, 1fr);
  align-content: stretch;
  gap: 14px;
  height: 100%;
  min-height: 0;
  overflow-y: auto;
}

.workbench-blocked-panel {
  position: relative;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.workbench-blocked-panel--blocked .source-board-list {
  pointer-events: none;
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
  background: rgba(241, 245, 249, 0.28);
}

.workbench-blocked-mask__content {
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

.workbench-blocked-mask__content :deep(.iconify),
.workbench-blocked-mask__content :deep(svg) {
  margin-bottom: 4px;
  font-size: 22px;
}

.workbench-blocked-mask__content strong {
  font-size: 16px;
  font-weight: 950;
}

.workbench-blocked-mask__content span {
  margin-top: 6px;
  font-size: 13px;
  font-weight: 800;
  line-height: 1.7;
}

.source-group-board {
  display: grid;
  grid-template-rows: minmax(58px, auto) minmax(150px, 1fr);
  gap: 10px;
  min-height: 0;
  padding: 12px;
  background:
    linear-gradient(90deg, rgba(15, 23, 42, 0.035) 1px, transparent 1px) 0 0 / 22px 22px,
    linear-gradient(180deg, #f5f8fb 0%, #e1e9f1 100%);
  border: 1px solid #7d8b9b;
}

.adhesive-sample-toolbar {
  display: grid;
  grid-template-rows: 1fr;
  gap: 0;
  align-items: stretch;
  min-height: 0;
}

.adhesive-sample-toolbar .second-sample-segment-strip {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  min-height: 0;
  overflow: hidden;
  background:
    radial-gradient(circle at 18px 18px, rgba(15, 23, 42, 0.06) 0 1px, transparent 1.4px) 0 0 / 18px 18px,
    linear-gradient(180deg, #d7e4ee 0%, #9fb9cc 100%);
  border: 1px solid #475569;
  box-shadow:
    inset 0 0 0 1px rgba(255, 255, 255, 0.55),
    inset 0 8px 14px rgba(255, 255, 255, 0.24);
}

.adhesive-sample-toolbar .second-sample-segment-cell {
  position: relative;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  grid-template-rows: repeat(2, minmax(44px, 1fr));
  gap: 9px;
  min-width: 0;
  min-height: 0;
  padding: 7px;
  overflow: hidden;
  background: rgba(226, 232, 240, 0.18);
  border-right: 2px solid rgba(30, 41, 59, 0.72);
}

.adhesive-sample-lock-guard {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  grid-template-rows: repeat(2, minmax(44px, 1fr));
  grid-column: 1 / -1;
  grid-row: 1 / -1;
  gap: 7px;
  width: 100%;
  height: 100%;
  min-width: 0;
  min-height: 0;
}

.adhesive-sample-toolbar .second-sample-segment-cell:last-child {
  border-right: 0;
}

.adhesive-sample-toolbar .second-sample-segment-cell.no-segment {
  background:
    linear-gradient(180deg, rgba(180, 83, 9, 0.3), rgba(245, 158, 11, 0.2)),
    rgba(251, 191, 36, 0.18);
}

.adhesive-sample-toolbar .second-sample-action-button {
  display: flex;
  gap: 5px;
  align-items: center;
  justify-content: center;
  min-width: 0;
  min-height: 44px;
  padding: 6px 10px;
  overflow: hidden;
  color: #0f172a;
  text-align: center;
  cursor: pointer;
  background: linear-gradient(180deg, rgba(248, 250, 252, 0.92) 0%, rgba(226, 232, 240, 0.86) 100%);
  border: 1px solid rgba(71, 85, 105, 0.5);
  border-radius: 8px;
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.72),
    0 1px 2px rgba(15, 23, 42, 0.1);
}

.adhesive-sample-toolbar .second-sample-action-button:disabled {
  cursor: not-allowed;
  opacity: 0.48;
}

.adhesive-sample-toolbar .second-sample-action-button__icon {
  flex: 0 0 auto;
  font-size: 20px;
}

.segment-main-action__copy {
  display: grid;
  gap: 1px;
  min-width: 0;
  text-align: left;
}

.segment-main-action__copy strong {
  font-size: 14px;
  font-weight: 950;
  line-height: 18px;
}

.segment-main-action__copy small {
  overflow: hidden;
  color: #64748b;
  font-size: 11px;
  font-weight: 800;
  line-height: 14px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.adhesive-sample-toolbar .second-sample-action-button > span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.adhesive-sample-toolbar .second-sample-action-button:hover:not(:disabled) {
  background: linear-gradient(180deg, rgba(224, 242, 254, 0.96) 0%, rgba(186, 230, 253, 0.84) 100%);
  border-color: rgba(2, 132, 199, 0.72);
}

.adhesive-sample-toolbar .second-sample-segment-button__text {
  display: block;
  max-width: 100%;
  overflow: hidden;
  color: #0f172a;
  font-size: 14px;
  font-weight: 900;
  line-height: 20px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.adhesive-sample-toolbar .second-sample-action-button.sent .second-sample-segment-button__text,
.adhesive-sample-toolbar .second-sample-action-button.sample-status-ok .second-sample-segment-button__text {
  color: #166534;
}

.adhesive-sample-toolbar .second-sample-action-button.sample-status-waiting .second-sample-segment-button__text {
  color: #075985;
}

.adhesive-sample-toolbar .second-sample-action-button.sample-status-ng .second-sample-segment-button__text {
  color: #be123c;
}

.adhesive-sample-toolbar .second-sample-action-button--middle {
  color: #075985;
  font-size: 14px;
  font-weight: 900;
  line-height: 20px;
  white-space: nowrap;
}

.adhesive-sample-toolbar .second-sample-action-button--report {
  color: #92400e;
  font-size: 14px;
  font-weight: 950;
  line-height: 20px;
  white-space: nowrap;
}

.adhesive-sample-toolbar .second-sample-action-button--start {
  color: #166534;
  background: linear-gradient(145deg, rgba(240, 253, 244, 0.98), rgba(187, 247, 208, 0.88));
  border-color: rgba(22, 163, 74, 0.58);
}

.adhesive-sample-toolbar .second-sample-action-button--end {
  color: #9f1239;
  background: linear-gradient(145deg, rgba(255, 241, 242, 0.98), rgba(254, 205, 211, 0.88));
  border-color: rgba(225, 29, 72, 0.5);
}

.adhesive-sample-toolbar .second-sample-action-button--report {
  background: linear-gradient(145deg, rgba(255, 251, 235, 0.98), rgba(253, 230, 138, 0.86));
  border-color: rgba(217, 119, 6, 0.5);
}

.adhesive-sample-toolbar .second-sample-action-button.recorded {
  background: linear-gradient(145deg, rgba(241, 245, 249, 0.98), rgba(203, 213, 225, 0.9));
  border-color: rgba(71, 85, 105, 0.42);
}

.adhesive-sample-toolbar .second-sample-action-button.recorded .segment-main-action__copy small {
  color: #334155;
}

.adhesive-sample-toolbar .second-sample-action-button--print {
  color: #1d4ed8;
  font-size: 14px;
  font-weight: 950;
  line-height: 20px;
  white-space: nowrap;
}

.adhesive-sample-toolbar .second-sample-action-button.disabled,
.adhesive-sample-toolbar .second-sample-action-button:disabled {
  color: #94a3b8;
  cursor: not-allowed;
  background: rgba(226, 232, 240, 0.72);
  border-color: rgba(148, 163, 184, 0.45);
  box-shadow: none;
  opacity: 1;
}

.adhesive-sample-toolbar .second-sample-action-button.disabled .second-sample-action-button__icon,
.adhesive-sample-toolbar .second-sample-action-button:disabled .second-sample-action-button__icon,
.adhesive-sample-toolbar .second-sample-action-button.disabled > span,
.adhesive-sample-toolbar .second-sample-action-button:disabled > span,
.adhesive-sample-toolbar .second-sample-action-button.disabled .second-sample-segment-button__text,
.adhesive-sample-toolbar .second-sample-action-button:disabled .second-sample-segment-button__text {
  color: #64748b !important;
}

.hidden-input {
  display: none;
}

.segmented-cloth-strip {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
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
  display: grid;
  align-items: stretch;
  justify-content: stretch;
  min-width: 0;
  padding: 26px 12px 52px;
  text-align: left;
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
  cursor: not-allowed;
  opacity: 0.58;
}

.cloth-segment:disabled {
  cursor: not-allowed;
  opacity: 0.58;
}

.cloth-segment.stock {
  background: rgba(15, 23, 42, 0.5);
}

.cloth-segment.inspection-ok {
  background:
    linear-gradient(180deg, rgba(22, 163, 74, 0.36), rgba(14, 165, 233, 0.14)),
    rgba(3, 105, 161, 0.42);
}

.cloth-segment.inspection-ng {
  background:
    linear-gradient(180deg, rgba(220, 38, 38, 0.32), rgba(245, 158, 11, 0.14)),
    rgba(100, 116, 139, 0.36);
}

.cloth-segment-topbar {
  position: absolute;
  top: 6px;
  right: 8px;
  z-index: 4;
  display: inline-flex;
  max-width: calc(100% - 16px);
  gap: 4px;
  align-items: center;
}

.cloth-segment-count {
  display: inline-flex !important;
  align-items: center;
  justify-content: center;
  width: auto !important;
  max-width: 56px !important;
  padding: 1px 6px !important;
  color: #e0f2fe !important;
  font-size: 11px !important;
  font-weight: 900;
  line-height: 16px !important;
  background: rgba(15, 23, 42, 0.82) !important;
  border: 1px solid rgba(226, 232, 240, 0.28);
  border-radius: 999px;
}

.cloth-segment-export {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 20px;
  padding: 0;
  color: #0f172a;
  cursor: pointer;
  background: rgba(248, 250, 252, 0.92);
  border: 1px solid rgba(148, 163, 184, 0.82);
  border-radius: 2px;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.18);
}

.cloth-segment-export:hover {
  color: #075985;
  background: #e0f2fe;
  border-color: #38bdf8;
}

.cloth-segment-export.disabled,
.cloth-segment-export:disabled {
  color: #94a3b8;
  cursor: not-allowed;
  background: rgba(226, 232, 240, 0.82);
  border-color: rgba(148, 163, 184, 0.54);
  box-shadow: none;
}

.cloth-segment-summary {
  display: grid;
  grid-template-columns: minmax(190px, 0.72fr) minmax(0, 1.55fr);
  gap: 14px;
  align-items: stretch;
  width: 100%;
  min-width: 0;
  min-height: 0;
}

.cloth-segment-identity {
  display: flex;
  flex-direction: column;
  justify-content: center;
  min-width: 0;
}

.cloth-segment-identity > strong,
.cloth-segment-identity > em {
  width: fit-content;
  max-width: 100%;
  padding: 3px 10px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  background: rgba(15, 23, 42, 0.72);
}

.cloth-segment-identity > strong {
  font-size: 18px;
}

.cloth-segment-identity > em {
  margin-top: 4px;
  font-size: 12px;
  font-style: normal;
}

.cloth-segment-batch-main {
  color: #ffffff;
  font-family: Consolas, 'Courier New', monospace;
  font-size: 30px !important;
  font-weight: 950;
  line-height: 1.12;
  letter-spacing: 0;
  background:
    linear-gradient(90deg, rgba(14, 165, 233, 0.92), rgba(2, 132, 199, 0.7)),
    rgba(15, 23, 42, 0.82) !important;
  border: 1px solid rgba(186, 230, 253, 0.45);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.24),
    0 2px 6px rgba(15, 23, 42, 0.18);
}

.cloth-segment-label {
  color: #dbeafe;
  font-size: 13px !important;
  font-weight: 900;
}

.cloth-segment-metrics {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(118px, 1fr));
  gap: 8px;
  align-content: center;
  min-width: 0;
}

.cloth-segment-metric {
  display: flex;
  min-width: 0;
  min-height: 46px;
  flex-direction: column;
  justify-content: center;
  padding: 6px 10px;
  overflow: hidden;
  background: rgba(15, 23, 42, 0.62);
  border: 1px solid rgba(226, 232, 240, 0.18);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.12);
}

.cloth-segment-metric b,
.cloth-segment-metric i {
  display: block;
  min-width: 0;
  overflow: hidden;
  line-height: 1.25;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cloth-segment-metric b {
  color: #dbeafe;
  font-size: 11px;
  font-weight: 900;
}

.cloth-segment-metric i {
  margin-top: 3px;
  color: #ffffff;
  font-family: Consolas, 'Courier New', monospace;
  font-size: 14px;
  font-style: normal;
  font-weight: 950;
}

.cloth-segment-inspection {
  margin-top: 4px !important;
  font-size: 12px !important;
  font-weight: 900;
}

.cloth-segment-inspection--ok {
  color: #dcfce7;
  background: rgba(22, 101, 52, 0.86);
}

.cloth-segment-inspection--ng {
  color: #fee2e2;
  background: rgba(153, 27, 27, 0.88);
}

.cloth-segment-inspection--waiting {
  color: #e0f2fe;
  background: rgba(3, 105, 161, 0.86);
}

.cloth-segment-scan-status {
  color: #fef3c7;
  font-size: 12px !important;
  font-weight: 900;
  background: rgba(146, 64, 14, 0.86) !important;
}

.cloth-segment-scan-status.confirmed {
  color: #dcfce7;
  background: rgba(21, 128, 61, 0.86) !important;
}

.cloth-segment-scan-status.empty {
  color: #e2e8f0;
  background: rgba(51, 65, 85, 0.78) !important;
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
  background: repeating-linear-gradient(
    90deg,
    rgba(249, 115, 22, 0.98) 0 8px,
    rgba(234, 88, 12, 0.98) 8px 12px
  );
  border-right: 1px solid rgba(124, 45, 18, 0.72);
}

.adhesive-position-strip i.confirmed {
  background: repeating-linear-gradient(
    90deg,
    rgba(22, 163, 74, 0.96) 0 8px,
    rgba(21, 128, 61, 0.96) 8px 12px
  );
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

.report-loss-sample-emphasis {
  color: #dc2626;
  font-style: normal;
  font-weight: 900;
}

.nap-sample-field {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.nap-sample-hint {
  color: #475569;
  font-size: 11px;
  font-weight: 700;
  line-height: 1.35;
}

.nap-sample-hint--warn {
  color: #b45309;
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

.segment-print-selector {
  height: 430px;
}

.segment-print-selector-tip {
  flex: 0 0 auto;
  padding: 8px 10px;
  color: #075985;
  font-size: 13px;
  font-weight: 800;
  background: #e0f2fe;
  border: 1px solid #7dd3fc;
  border-bottom: 0;
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

.equipment-select-filter {
  width: 220px;
}

.equipment-select-tip {
  padding: 8px 10px;
  color: #334155;
  font-size: 12px;
  font-weight: 700;
  background: #f8fafc;
  border: 1px solid #8794a4;
  border-top: 0;
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

.glue-stock-batch-filter {
  width: 190px;
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
  flex: 0 0 42px;
}

.console-table-count {
  color: #475569;
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
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

.second-inspection-summary {
  display: grid;
  flex: 0 0 42px;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  padding: 6px 8px;
  background: linear-gradient(180deg, #f8fafc 0%, #edf2f7 100%);
  border: 1px solid #8794a4;
  border-top: 0;
}

.second-inspection-summary__item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-width: 0;
  padding: 4px 10px;
  color: #475569;
  background: rgba(255, 255, 255, 0.7);
  border: 1px solid rgba(148, 163, 184, 0.45);
}

.second-inspection-summary__item span {
  font-size: 12px;
  font-weight: 800;
}

.second-inspection-summary__item strong {
  color: #0f172a;
  font-size: 17px;
  font-weight: 950;
  line-height: 20px;
}

.second-inspection-summary__item.ok strong {
  color: #166534;
}

.second-inspection-summary__item.abnormal strong {
  color: #be123c;
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

.adhesive-task-list-table :deep(.ant-table-cell) {
  white-space: nowrap;
}

.adhesive-task-list-table :deep(.ant-table-thead > tr > th),
.adhesive-task-list-table :deep(.ant-table-tbody > tr > td) {
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

.adhesive-dev-param-fieldset {
  display: flex;
  flex: 1 1 0;
  flex-direction: column;
  min-height: 0;
}

.adhesive-dev-param-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 6px 8px;
  background: linear-gradient(180deg, #eef3f8 0%, #dde5ee 100%);
  border: 1px solid #a2adba;
  border-bottom: 0;
}

.adhesive-dev-param-toolbar__meta,
.adhesive-dev-param-toolbar__actions {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.adhesive-dev-param-toolbar__meta span,
.adhesive-dev-param-fill-time {
  color: #475569;
  font-size: 12px;
}

.adhesive-dev-param-table {
  flex: 1 1 0;
  min-height: 0;
}

.adhesive-dev-param-empty,
.adhesive-dev-param-runtime-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 120px;
  color: #64748b;
  font-weight: 700;
}

.adhesive-dev-param-runtime-view {
  display: flex;
  flex-direction: column;
  height: 100vh;
  min-height: 0;
  overflow: hidden;
  background:
    linear-gradient(90deg, rgba(30, 41, 59, 0.04) 1px, transparent 1px) 0 0 / 28px 28px,
    linear-gradient(0deg, rgba(30, 41, 59, 0.04) 1px, transparent 1px) 0 0 / 28px 28px,
    #e8edf3;
}

.adhesive-dev-param-runtime-view__header {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: #dfe7f0;
  border-bottom: 1px solid #8794a4;
}

.adhesive-dev-param-runtime-view__header strong {
  margin-right: auto;
  color: #082f49;
  font-size: 18px;
}

.adhesive-dev-param-runtime-view__status {
  color: #475569;
}

.adhesive-dev-param-runtime-view__body {
  flex: 1 1 0;
  min-height: 0;
  padding: 10px;
  overflow: hidden;
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

.report-tab-scroll-body {
  display: flex;
  flex: 1 1 0;
  flex-direction: column;
  gap: 8px;
  min-height: 0;
  overflow: auto;
}

.report-loss-with-abnormal {
  display: grid;
  grid-template-columns: minmax(0, 1fr) max-content max-content;
  gap: 8px;
  align-items: center;
}

.report-loss-with-abnormal span {
  color: #475569;
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
}

.abnormal-position-dialog {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.abnormal-position-toolbar {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.abnormal-position-toolbar span {
  color: #334155;
  font-weight: 800;
}

.abnormal-position-edit-table :deep(.ant-input),
.abnormal-position-edit-table :deep(.ant-input-number) {
  width: 100%;
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

.adhesive-intermediate-attachment {
  flex: 0 0 auto;
  min-height: 42px;
  padding: 8px 10px;
  color: #475569;
  font-size: 12px;
  background: #fff;
  border: 1px solid #d8e0ea;
}

.adhesive-intermediate-attachment__title {
  color: #1677ff;
  font-size: 14px;
  font-weight: 800;
}

.adhesive-intermediate-attachment__content {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 14px;
  align-items: center;
  margin-top: 6px;
}

.adhesive-intermediate-attachment__link {
  display: inline-flex;
  gap: 4px;
  align-items: center;
  padding: 0;
  color: #1677ff;
  font: inherit;
  font-weight: 800;
  cursor: pointer;
  background: transparent;
  border: 0;
}

.adhesive-intermediate-attachment__empty {
  display: inline-flex;
  margin-top: 6px;
  color: #64748b;
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

.record-action-group {
  display: inline-flex;
  gap: 4px;
  align-items: center;
  white-space: nowrap;
}

.statistics-revision-head {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-bottom: 12px;
  color: #334155;
  font-size: 12px;
  font-weight: 800;
}

.statistics-revision-form {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px 12px;
}

.statistics-revision-form__reason {
  grid-column: 1 / -1;
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
  grid-template-columns: repeat(2, 120px minmax(0, 1fr));
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

.record-confirm-matched-row {
  display: grid;
  grid-template-columns: 120px minmax(0, 1fr);
  gap: 8px;
}

.record-confirm-matched-row span,
.record-confirm-matched-row strong {
  display: flex;
  align-items: center;
  min-height: 34px;
  padding: 6px 8px;
  border: 1px solid #cbd5e1;
}

.record-confirm-matched-row span {
  justify-content: flex-end;
  color: #334155;
  font-weight: 700;
  background: #e2e8f0;
}

.record-confirm-matched-row strong {
  overflow: hidden;
  color: #0f766e;
  font-family: Consolas, 'Courier New', monospace;
  text-overflow: ellipsis;
  white-space: nowrap;
  background: #ecfdf5;
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

.first-inspection-detail p {
  margin: 0 0 8px;
  line-height: 1.6;
}

.first-inspection-detail__actions {
  display: flex;
  gap: 8px;
  margin-top: 12px;
}

.second-sample-length-confirm p {
  margin: 0 0 10px;
  line-height: 1.6;
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

.rough-report-work-modal .modal-footer {
  position: sticky;
  bottom: 0;
  flex: 0 0 auto;
  padding: 8px 10px;
  background: #f5f7fa;
  border-top: 1px solid #cbd5e1;
}

</style>
