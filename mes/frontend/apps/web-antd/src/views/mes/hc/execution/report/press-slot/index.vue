<script lang="ts" setup>
import { computed, h, nextTick, onActivated, onBeforeUnmount, onDeactivated, onMounted, reactive, ref, watch } from 'vue';

import { useAccess } from '@vben/access';
import { Page, useVbenModal } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';
import { useUserStore } from '@vben/stores';
import { downloadFileFromBlobPart } from '@vben/utils';
import { useQRCode } from '@vueuse/integrations/useQRCode';

import {
  Button,
  Badge,
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

import {
  completeAdhesiveConsoleWorkOrder,
  completePressSlotSegment,
  applyPressSlotFai,
  withdrawPressSlotFai,
  cleanPressSlotConsumable,
  correctPressSlotReportAbnormalCategory,
  confirmAdhesiveConsolePassWork,
  confirmAdhesiveConsoleReport,
  confirmPressSlotIntermediate,
  confirmPressSlotProcessParam,
  getAdhesiveConsoleCheckTemplate,
  getAdhesiveConsoleIntermediate,
  getAdhesiveConsolePassWorkList,
  getAdhesiveConsoleReportList,
  getAdhesiveConsoleSourceList,
  getAdhesiveConsoleTaskList,
  getPressSlotConsumableStatus,
  getPressSlotChangeoverLatest,
  getPressSlotChangeoverList,
  getPressSlotActiveAbnormalLock,
  getPressSlotAbnormalLockList,
  getPressSlotFaiList,
  getPressSlotFaiSummary,
  getPressSlotConsumableMaterialOptions,
  getPressSlotProcessCheckFaiList,
  getPressSlotIntermediateList,
  getPressSlotProcessParamList,
  getPressSlotProductionCheckTemplate,
  importPressSlotProcessParams,
  importPressSlotMiddleLedger,
  exportPressSlotProcessParams,
  exportPressSlotMiddleLedger,
  markAdhesiveConsoleReportPrinted,
  replacePressSlotConsumable,
  saveAndConfirmPressSlotReport,
  saveAdhesiveConsoleIntermediate,
  saveAdhesiveConsolePassWork,
  savePressSlotProcessParam,
  savePressSlotChangeover,
  scanAdhesiveConsoleSource,
  setPressSlotReportMiddleType,
  startAdhesiveConsoleWorkOrder,
  switchPressSlotConsoleWorkOrderEquipment,
  validatePressSlotFaiScan,
  type MesHcAdhesiveConsoleApi,
} from '#/api/mes/hc/execution/press-slot-console';
import { exportProcessFormRecordLayout, importProcessFormRecordLayout } from '#/api/mes/hc/processform';
import { getStationFormDetail, type MesHcStationFormApi } from '#/api/mes/hc/stationform';
import StationFormRuntimeRenderer from '#/views/mes/hc/stationform/modules/StationFormRuntimeRenderer.vue';
import { normalizeProductionCheckHeader, productionCheckRows, productionCheckSchema } from '#/views/mes/hc/shared/pressSlotProductionCheck';
import { uploadFile } from '#/api/infra/file';
import { getEquipmentPage, type MesHcEquipmentApi } from '#/api/mes/hc/equipment';
import { getPlanOrderDetail } from '#/api/mes/hc/planorder';
import AuthModal from '#/views/mes/work-order-booking/modules/components/AuthModal.vue';
import FaiDetailModal from '#/views/mes/quality/fai/modules/detail-modal.vue';
import {
  ProductionInstructionMessageTab,
  type ProductionInstructionContext,
} from '#/views/mes/hc/shared/production-instruction';
import { shouldShowPressSlotIntermediateDepthSection } from '#/views/mes/hc/shared/pressSlotRuntimeLayout';
import { getProductionInstructionUnreadCount } from '#/api/mes/hc/production-instruction';
import { useExecutionFullscreenClock } from '../shared/useExecutionFullscreenClock';
import { getReportRequestErrorMessage, resolveSavedReportId } from '../shared/reportSubmitGuard';
import {
  applyPreProcessSelfCheckAttribution,
  getDownstreamPreProcessFeedbackTitle,
  hasDownstreamPreProcessFeedback,
  isPreProcessSelfCheckAbnormal,
} from '../shared/preProcessSelfCheckAttribution';
import {
  buildInspectionTransferTicketPayload,
  buildTransferTicketQrValue,
  resolveTransferTicketQrBusinessNo,
  sendTransferTicketToPrintAgent,
} from '../shared/workOrderTicketPrint';
import { applyPrintFieldTemplate } from '../shared/printFieldTemplate';
import {
  buildSegmentChainSampleLockCandidates,
  ensureProcessingSampleAbnormalUnlocked as ensureSampleAbnormalUnlocked,
} from '../shared/sampleAbnormalLockGuard';

defineOptions({ name: 'MesExecutionPressSlotConsole' });

const PRESS_SLOT_PRE_PROCESS_ATTRIBUTION = {
  processCode: 'SLITTING',
  processName: '分切',
} as const;

const PRESS_SLOT_PRINT_AGENT_URL = 'http://127.0.0.1:17820';
const PRESS_SLOT_LAST_CONSUMABLE_MATERIAL_KEY = 'mes.pressSlot.lastConsumableMaterial';
const ABNORMAL_CATEGORY_CORRECT_PERMISSION = 'mes:sfc:press-slot-report:correct-abnormal-category';
const PRESS_SLOT_PRODUCTION_CHECK_FORM_TYPE = 'PRODUCTION_CHECK';
const PRESS_SLOT_FIRST_INSPECTION_PROCESS_FORM_NAME = '压槽生产点检表';
const PRESS_SLOT_PROCESS_CHECK_PROCESS_FORM_NAME = '压槽加检生产点检表';
const PRESS_SLOT_FIRST_INSPECTION_PROCESS_EXCEL = '11-2-92 CMP软垫（W26P0100）压槽生产点检表.xlsx';

type PressSlotConsumableType = 'BEARING' | 'PRESS_ROLLER';

type DailyRecordMode = 'confirm' | 'edit' | 'view';
type DailyRecordSubmitAction = {
  key: string;
  mode: Exclude<DailyRecordMode, 'view'>;
};
type IntermediateAuthAction = 'confirm' | 'save';
type ProcessParamAuthAction = 'confirm' | 'save';
type FirstInspectionProcessAuthAction = 'confirm' | 'save';

interface PressSlotImportAttachment {
  name: string;
  path?: string;
  size?: number;
  type?: string;
  uid: string;
  uploadTime: string;
  url?: string;
}

interface ConsumableMaterialSelectOption {
  label: string;
  material: MesHcAdhesiveConsoleApi.ConsumableMaterialOption;
  value: string;
}

interface SubmitPressSlotReportOptions {
  closeAfterSave?: boolean;
  openNextAfterSave?: boolean;
  successMessage?: string;
}

interface PressSlotOneClickConfirmTarget {
  group: SourceGroup;
  record?: MesHcAdhesiveConsoleApi.ReportItem;
  segment: SourceSegment;
}

interface SaveFirstInspectionProcessFormOptions {
  closeAfterSave?: boolean;
  silent?: boolean;
}

interface PressSlotInspectionTransferPrintOptions {
  inspectionType: string;
  sampleType: string;
  successContent?: string;
}

interface LoadPressSlotFaiSummaryOptions {
  preserveExistingOnEmpty?: boolean;
}

type FirstInspectionScanMode = 'ABNORMAL_RELEASE' | 'FIRST_INSPECTION' | 'PROCESS_CHECK';

const PRESS_SLOT_FAI_TRIGGER_PROCESS_CHECK_NG_RESTART = 'PROCESS_CHECK_NG_RESTART';
const PRESS_SLOT_FAI_TRIGGER_ADDITIONAL_FIRST_INSPECTION = 'ADDITIONAL_FIRST_INSPECTION';

interface FirstInspectionProcessRow extends Partial<MesHcAdhesiveConsoleApi.ChangeoverInspection>, Partial<MesHcAdhesiveConsoleApi.FaiSummary> {
  firstInspectionSliceNo: string;
  inspectionType: FirstInspectionScanMode;
  inspectionTypeName: string;
  inspectionResultText: string;
  rowKey: string;
  processFormRecord?: MesHcAdhesiveConsoleApi.ProcessParamRecord;
  submitTimeText: string;
}

interface BoardEquipment {
  code: string;
  id?: number;
  name: string;
  workCenterId?: number;
  workCenterName: string;
}

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
  requirements: string;
  sourceBatchNo: string;
  sourceProductionBatchNo: string;
  startTime: string;
  status: string;
  workCenterId?: number;
  workCenterName: string;
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
  recordDate?: string;
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
  extraJson?: string;
  grindingSecondDetailId?: number;
  label: string;
  outputLength: number;
  productQualityStatus?: string;
  qtime?: MesHcAdhesiveConsoleApi.QtimeInfo;
  qualityLockReason?: string;
  reportRanges: SourceReportRange[];
  segmentMark: string;
  selfCheck?: string;
  sourceOperationName?: string;
  sourceProcessStage?: string;
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

interface AdhesiveCheckItem {
  abnormalRemark: string;
  actualValue: string;
  checkResult: 'NG' | 'OK' | string;
  dualLabel1?: string;
  dualLabel2?: string;
  itemCategory: string;
  itemName: string;
  requiredFlag?: boolean;
  sortNo: number;
  standardValue: string;
  valueMode?: string;
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
const TASK_LIST_DEFAULT_PAGE_SIZE = 10;
const FIRST_INSPECTION_REFRESH_INTERVAL_MS = 5 * 60_000;
const currentDateTime = ref(dayjs().format('YYYY-MM-DD HH:mm:ss'));
const currentDateText = computed(() => dayjs(currentDateTime.value).format('YYYY-MM-DD'));
const currentTimeText = computed(() => dayjs(currentDateTime.value).format('HH:mm:ss'));
const { showExecutionClock } = useExecutionFullscreenClock();
const boardLoading = ref(false);
let timer: ReturnType<typeof setInterval> | null = null;
let skipInitialActivation = true;
let firstInspectionRefreshTimer: ReturnType<typeof setInterval> | null = null;
let planScanTimer: ReturnType<typeof setTimeout> | null = null;
let syncingReportPosition = false;
let firstInspectionRefreshInFlight = false;

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
  operationCode: 'WC-GROOVE',
  operationName: '压槽',
  processCode: 'WC-GROOVE',
  processName: '压槽',
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
const planScanInputRef = ref<any>();
const recordConfirmScanInputRef = ref<any>();
const firstInspectionScanInputRef = ref<any>();
const lastAutoScannedPlanNo = ref('');
let pendingScannerPlanNo = '';
let pendingScannerSliceNo = '';
let globalScannerBuffer = '';
let globalScannerLastAt = 0;
let globalScannerTimer: ReturnType<typeof setTimeout> | null = null;
let globalScannerListenerAttached = false;
const PLAN_SCAN_SEPARATOR_REGEXP = /[，,；;]/;
const GLOBAL_SCANNER_MAX_GAP_MS = 80;
const GLOBAL_SCANNER_IDLE_FLUSH_MS = 160;
const hasPlanScanDelimiter = (value?: string) => PLAN_SCAN_SEPARATOR_REGEXP.test(value || '');
const parsePlanScanCode = (value?: string) => {
  const text = String(value || '').trim();
  const separatorIndex = text.search(PLAN_SCAN_SEPARATOR_REGEXP);
  return {
    planNo: separatorIndex >= 0 ? text.slice(0, separatorIndex).trim() : text,
    sliceNo: separatorIndex >= 0 ? text.slice(separatorIndex + 1).trim() : '',
  };
};
const normalizePlanScanNo = (value?: string) => parsePlanScanCode(value).planNo;
const normalizePlanScanSliceNo = (value?: string) => parsePlanScanCode(value).sliceNo;
const normalizeConfirmScanCode = (value?: string) => {
  const text = String(value || '').trim();
  const separatorIndex = text.search(PLAN_SCAN_SEPARATOR_REGEXP);
  return separatorIndex >= 0 ? text.slice(separatorIndex + 1).trim() || text : text;
};
const syncPlanScanNo = (value?: string) => {
  const scannerInput = hasPlanScanDelimiter(value);
  const { planNo, sliceNo } = parsePlanScanCode(value);
  if (scannerInput) {
    pendingScannerPlanNo = planNo;
    pendingScannerSliceNo = sliceNo;
  }
  if (String(value || '').trim() !== planNo && scanPlanNo.value !== planNo) {
    scanPlanNo.value = planNo;
  }
  return planNo;
};
const clearPlanScannerState = () => {
  scanPlanNo.value = '';
  pendingScannerPlanNo = '';
  pendingScannerSliceNo = '';
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
const focusFirstInspectionScanInput = () => focusInputRef(firstInspectionScanInputRef);
const handlePlanScanInput = (value?: string) => {
  const rawValue = String(value || '');
  if (hasPlanScanDelimiter(rawValue)) {
    const { planNo, sliceNo } = parsePlanScanCode(rawValue);
    pendingScannerPlanNo = planNo;
    pendingScannerSliceNo = sliceNo;
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
  isEventTargetInInputRef(event.target, planScanInputRef)
  || isEventTargetInInputRef(event.target, recordConfirmScanInputRef)
  || isEventTargetInInputRef(event.target, firstInspectionScanInputRef);
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
const activeBoardTab = ref('SOURCE');
const activeReportTab = ref('');
const reportVisible = ref(false);
const showExtendedBoardTabs = ref(false);
const reportDialogMode = ref<'confirm' | 'view'>('view');
const reportRecords = ref<MesHcAdhesiveConsoleApi.ReportItem[]>([]);
const reportScanConfirmDate = ref('');
const changeoverInspections = ref<MesHcAdhesiveConsoleApi.ChangeoverInspection[]>([]);
const latestChangeoverInspection = ref<MesHcAdhesiveConsoleApi.ChangeoverInspection | null>(null);
const firstInspection = ref<MesHcAdhesiveConsoleApi.FaiSummary>({ allowReportSubmit: false });
const firstInspectionFaiRows = ref<MesHcAdhesiveConsoleApi.FaiSummary[]>([]);
const firstInspectionApplying = ref(false);
const firstInspectionRefreshing = ref(false);
const processCheckApplying = ref(false);
const reportSourceScanning = ref(false);
const reportSubmitting = ref(false);
const processCheckFaiRows = ref<MesHcAdhesiveConsoleApi.FaiSummary[]>([]);
const withdrawingFaiId = ref<number | null>(null);
const abnormalLockRows = ref<MesHcAdhesiveConsoleApi.PressSlotAbnormalLockRecord[]>([]);
const activeAbnormalLockSummary = ref<MesHcAdhesiveConsoleApi.PressSlotAbnormalLockRecord | null>(null);
let lastAbnormalLockRefreshSignature = '';
const firstInspectionScanLoading = ref(false);
const firstInspectionScanVisible = ref(false);
const firstInspectionScanMode = ref<FirstInspectionScanMode>('FIRST_INSPECTION');
const firstInspectionScanNo = ref('');
const firstInspectionScanError = ref('');
const firstInspectionSource = ref<MesHcAdhesiveConsoleApi.SourceItem | null>(null);
const processCheckScanReport = ref<MesHcAdhesiveConsoleApi.ReportItem | null>(null);
const processParameterRecords = ref<MesHcAdhesiveConsoleApi.ProcessParamRecord[]>([]);
const processParamFileInput = ref<HTMLInputElement | null>(null);
const firstInspectionProcessFileInput = ref<HTMLInputElement | null>(null);
const middleLedgerFileInput = ref<HTMLInputElement | null>(null);
const segmentCompleteVisible = ref(false);
const segmentCompleteSubmitting = ref(false);
const pendingSegmentCompleteBatchNo = ref('');
const segmentCompleteAuthActionName = computed(() => {
  const batchNo = pendingSegmentCompleteBatchNo.value || currentMotherSegmentBatchNo.value || resolveCurrentPlanSourceBatchNo() || '-';
  return `压槽本段完工：${batchNo}（确认后不可再报工）`;
});
const processParamFilters = reactive({
  productionBatchNo: '',
  reportDate: '',
});
const processParamEditVisible = ref(false);
const processParamConfirming = ref(false);
const processParamEditSaving = ref(false);
const processParamImporting = ref(false);
const processParamAuthVisible = ref(false);
const processParamAuthAction = ref('CMP压槽工艺参数表认证');
const pendingProcessParamAuthAction = ref<ProcessParamAuthAction>();
const processParamEditForm = reactive<MesHcAdhesiveConsoleApi.ProcessParamRecord>({
  id: undefined,
  confirmTime: '',
  confirmUserName: '',
  fillTime: '',
  fillUserName: '',
  formName: 'CMP压槽工艺参数表',
  firstInspectionResult: '',
  firstInspectionSliceNo: '',
  importAttachment: undefined,
  inspectionTime: '',
  items: [],
  materialCode: '',
  modelCode: '',
  motherBatchNo: '',
  parentProductionBatchNo: '',
  planId: undefined,
  planNo: '',
  planOperationId: undefined,
  productionBatchNo: '',
  recordStatus: 'SUBMITTED',
  recorderName: '',
  remark: '',
  reportDate: dayjs().format('YYYY-MM-DD'),
  statusName: '',
  temperature1: '',
  temperature2: '',
  temperature3: '',
  temperature4: '',
  temperature5: '',
});
const firstInspectionProcessFormRecords = ref<Record<string, MesHcAdhesiveConsoleApi.ProcessParamRecord>>({});
const firstInspectionProcessFormVisible = ref(false);
const firstInspectionProcessFormLoading = ref(false);
const firstInspectionProcessFormSaving = ref(false);
const firstInspectionProcessFormConfirming = ref(false);
const firstInspectionProcessImporting = ref(false);
const firstInspectionProcessAuthVisible = ref(false);
const firstInspectionProcessAuthAction = ref('压槽生产点检表认证');
const pendingFirstInspectionProcessAuthAction = ref<FirstInspectionProcessAuthAction>();
const currentFirstInspectionProcessRow = ref<FirstInspectionProcessRow | null>(null);
const firstInspectionProcessRecord = reactive<MesHcAdhesiveConsoleApi.ProcessParamRecord>({});
const firstInspectionProcessHeader = reactive<Record<string, any>>({});
const productionCheckRuntime = ref<InstanceType<typeof StationFormRuntimeRenderer>>();
const productionCheckForm = ref<MesHcStationFormApi.StationForm>();
const productionCheckRuntimeSchema = ref<Record<string, any>>({});
const productionCheckRuntimeItems = ref<MesHcStationFormApi.StationFormItem[]>([]);
const middleLedgerImporting = ref(false);
const middleTypeSetting = ref(false);
const middleAutoMarking = ref(false);
const selectedReportRecordKeys = ref<(number | string)[]>([]);
const sourceGroups = ref<SourceGroup[]>([]);
const selectedOneClickPressSlotBatchNo = ref('');
const selectedOneClickPressSlotSegmentKeys = ref<string[]>([]);
const oneClickScanConfirmingBatchNo = ref('');
const visualMaximized = ref(false);
const workbenchRefreshing = ref(false);
const transferPrintSelectionMode = ref(false);
const selectedTransferReportIds = ref<string[]>([]);
const visualFilterForm = reactive({
  batchNo: '',
  reportType: 'ALL',
  status: 'ALL',
});
const checkTemplate = ref<AdhesiveCheckItem[]>([]);
const checkTemplateModelCode = ref('');
const changeoverVisible = ref(false);
const changeoverScanNo = ref('');
const changeoverForm = reactive<MesHcAdhesiveConsoleApi.ChangeoverInspection>({
  checkItems: [],
  currentPlanNo: '',
  feedbackRemark: '',
  feedbackResult: '',
  feedbackTime: '',
  inspectionStatus: 'DRAFT',
  motherSegmentBatchNo: '',
  planId: 0,
  planOperationId: 0,
  pressSlotSliceNo: '',
  productionMaterialCode: '',
  productionModelCode: '',
  recordTime: '',
  recorderName: '',
  submitTime: '',
});
const recordConfirmVisible = ref(false);
const activeRecord = ref<MesHcAdhesiveConsoleApi.ReportItem | null>(null);
const abnormalCategoryCorrectionVisible = ref(false);
const abnormalCategoryCorrectionSaving = ref(false);
const abnormalCategoryCorrectionForm = reactive({
  category: '',
  reason: '',
});
const taskListVisible = ref(false);
const taskListLoading = ref(false);
const taskRows = ref<MesHcAdhesiveConsoleApi.TaskItem[]>([]);
const taskListPage = ref(1);
const taskListPageSize = ref(TASK_LIST_DEFAULT_PAGE_SIZE);
const taskListFilters = reactive({
  batchNo: '',
  modelCode: '',
  planNo: '',
  status: 'UNFINISHED',
});
const boardEquipment = reactive<BoardEquipment>({
  code: '',
  id: undefined,
  name: '',
  workCenterId: undefined,
  workCenterName: '',
});
const equipmentSelectVisible = ref(false);
const equipmentSelectLoading = ref(false);
const equipmentSelectRows = ref<MesHcEquipmentApi.Equipment[]>([]);
const equipmentSelectKeyword = ref('');
const boardEquipmentManualSelected = ref(false);
const openDailyRecordAfterEquipmentSelected = ref(false);
const intermediateLoading = ref(false);
const intermediateDetailVisible = ref(false);
const intermediateDetails = ref<MesHcAdhesiveConsoleApi.IntermediateDetail[]>([]);
const pressSlotIntermediateRecords = ref<MesHcAdhesiveConsoleApi.IntermediateRecord[]>([]);
const selectedIntermediateRecord = ref<MesHcAdhesiveConsoleApi.IntermediateRecord | null>(null);
const intermediateAuthVisible = ref(false);
const intermediateAuthAction = ref('压槽中间品记录表认证');
const pendingIntermediateAuthAction = ref<IntermediateAuthAction>();
const PRESS_SLOT_INTERMEDIATE_DEFAULT_SLOT_DEPTH_STANDARD = '0.705±0.044';
const PRESS_SLOT_INTERMEDIATE_DEFAULT_THICKNESS_STANDARD = '1.183±0.045';
const PRESS_SLOT_INTERMEDIATE_DEFAULT_THICKNESS_COLUMN_COUNT = 10;
const PRESS_SLOT_INTERMEDIATE_DEFAULT_THICKNESS_INTERVAL_CM = 100;
const PRESS_SLOT_INTERMEDIATE_DEPTH_FIELDS = [
  'firstSlotDepthAvg',
  'firstSlotDepthMax',
  'firstSlotDepthMin',
  'firstSlotDepthXAvg',
  'firstSlotDepthXMax',
  'firstSlotDepthXMin',
  'firstSlotDepthYAvg',
  'firstSlotDepthYMax',
  'firstSlotDepthYMin',
] as const;

const intermediateForm = reactive<MesHcAdhesiveConsoleApi.IntermediateRecord>({
  adhesiveReportId: undefined,
  batchNo: '',
  confirmerName: '',
  confirmTime: '',
  createTime: '',
  endSliceNo: '',
  fillTime: '',
  firstSampleSliceNo: '',
  firstSlotDepthAvg: undefined,
  firstSlotDepthXAvg: undefined,
  firstSlotDepthXMax: undefined,
  firstSlotDepthXMin: undefined,
  firstSlotDepthYAvg: undefined,
  firstSlotDepthYMax: undefined,
  firstSlotDepthYMin: undefined,
  firstSlotDepthMax: undefined,
  firstSlotDepthMin: undefined,
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
  slotDepthStandard: PRESS_SLOT_INTERMEDIATE_DEFAULT_SLOT_DEPTH_STANDARD,
  sourceExcel: '',
  sourceSheet: '',
  templateCode: '',
  templateName: '',
  thicknessColumnCount: PRESS_SLOT_INTERMEDIATE_DEFAULT_THICKNESS_COLUMN_COUNT,
  thicknessIntervalCm: PRESS_SLOT_INTERMEDIATE_DEFAULT_THICKNESS_INTERVAL_CM,
  thicknessStandard: PRESS_SLOT_INTERMEDIATE_DEFAULT_THICKNESS_STANDARD,
  updateTime: '',
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
  qualityLockReason: '',
  qualityLockStartPosition: undefined as number | undefined,
  qualityStatus: '',
  receiveLength: 0,
  receiveStartPosition: 0,
  receiveCount: 0,
  stockMeasureMode: 'COUNT',
  stockLength: 0,
  stockCount: 0,
  todayUsedLength: 0,
  todayUsedCount: 0,
  availableStartPosition: 0,
  availableCount: 0,
  aqcSampleLength: 0,
  latestAqcTask: undefined as MesHcAdhesiveConsoleApi.AqcTask | undefined,
  lossLength: 0,
  lossCount: 0,
  lifetimeMode: '',
  lifetimeLimitLength: 0,
  lifetimeLimitCount: 0,
  lifeUsedLength: 0,
  lifeUsedCount: 0,
  lastCleanTime: '',
  useDays: 0,
  limitDays: 60,
  warning: '',
});

const bearingConsumable = reactive({
  alarm: '',
  batchNo: '',
  id: undefined as number | undefined,
  stockId: undefined as number | undefined,
  materialCode: '',
  materialName: '',
  stockCount: 0,
  todayUsedCount: 0,
  lifetimeLimitCount: 0,
  lastReplaceTime: '',
  useDays: 0,
  limitDays: 150,
  warning: '',
});

const activeConsumableType = ref<PressSlotConsumableType>('PRESS_ROLLER');
const glueConsumeVisible = ref(false);
const rollerCleanVisible = ref(false);
const consumableAuthVisible = ref(false);
const consumableAuthAction = ref('');
const pendingConsumableAction = ref<'CLEAN_ROLLER' | 'REPLACE_BEARING' | ''>('');
const glueConsumeForm = reactive({
  batchNo: '',
  materialCode: '',
  materialName: '',
  materialOptionValue: undefined as string | undefined,
  materialScanCode: '',
  initialUseCount: 0,
  replaceReason: '',
  replaceTime: '',
  receiveLength: undefined as number | undefined,
  receiveStartPosition: 0,
  stockAvailableCount: 0,
  stockAvailableLength: 0,
  stockId: undefined as number | undefined,
  stockMeasureMode: 'COUNT',
});
const rollerCleanForm = reactive({
  batchNo: '',
  cleanRemark: '',
  cleanTime: '',
  initialUseCount: 0,
  materialCode: '',
  materialName: '',
  materialOptionValue: undefined as string | undefined,
});
const activeMaterialSelectType = ref<PressSlotConsumableType>('PRESS_ROLLER');
const consumableMaterialLoading = ref(false);
const consumableMaterialOptions = ref<ConsumableMaterialSelectOption[]>([]);

function readLastConsumableMaterialMap() {
  if (typeof window === 'undefined') {
    return {} as Partial<Record<PressSlotConsumableType, MesHcAdhesiveConsoleApi.ConsumableMaterialOption>>;
  }
  try {
    return JSON.parse(window.localStorage.getItem(PRESS_SLOT_LAST_CONSUMABLE_MATERIAL_KEY) || '{}') as Partial<
      Record<PressSlotConsumableType, MesHcAdhesiveConsoleApi.ConsumableMaterialOption>
    >;
  } catch {
    return {} as Partial<Record<PressSlotConsumableType, MesHcAdhesiveConsoleApi.ConsumableMaterialOption>>;
  }
}

const lastConsumableMaterialMap = reactive<Partial<Record<PressSlotConsumableType, MesHcAdhesiveConsoleApi.ConsumableMaterialOption>>>(
  readLastConsumableMaterialMap(),
);

const recordConfirmForm = reactive({
  error: '',
  message: '',
  reportType: 'PRODUCT' as PressSlotReportType,
  scannedBatchNo: '',
});

const showPressSlotScanGateReminder = (content?: string) => {
  const text = String(content || '').trim();
  if (!text) return;
  message.warning(text);
  if (recordConfirmVisible.value) {
    focusRecordConfirmScanInput();
  }
};

const reportForm = reactive({
  defectCode: '',
  endTime: '',
  glueBoardBatchNo: '',
  glueBoardMaterialCode: '',
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
  coaFlag: false,
  reportType: 'PRODUCT' as PressSlotReportType,
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
const visualInspectionItems = ref<VisualItem[]>([]);

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
  sourceBatchNo: { max: 320, min: 142, padding: 32 },
  status: { max: 92, min: 78, padding: 24 },
};
const taskListColumnTextGetters: Record<string, (row: Record<string, any>) => unknown> = {
  action: (row) => getTaskListActionText(row.status),
  availableSourceLength: (row) => formatNumber(row.availableSourceLength),
  status: (row) => getWorkOrderStatusMeta(row.status).text,
};

const equipmentSelectColumns = [
  { dataIndex: 'equipmentCode', title: '设备编码', width: 150 },
  { dataIndex: 'equipmentName', title: '设备名称', width: 180 },
  { dataIndex: 'workCenterName', title: '工作中心', width: 160 },
  { dataIndex: 'workStatus', title: '状态', width: 100 },
  { dataIndex: 'action', fixed: 'right' as const, title: '操作', width: 90 },
];

const taskListStatusOptions = [
  { label: '未完工', value: 'UNFINISHED' },
  { label: '全部', value: 'ALL' },
  { label: '待开工', value: 'PENDING' },
  { label: '生产中', value: 'IN_PROGRESS' },
  { label: '已完工', value: 'COMPLETED' },
  { label: '已暂停', value: 'PAUSED' },
  { label: '已作废', value: 'CANCELLED' },
];

const pressSlotFilterStatusOptions = [
  { label: '全部状态', value: 'ALL' },
  { label: '待确认', value: 'PENDING' },
  { label: '本工序NG', value: 'CURRENT_NG' },
  { label: '分切工序NG', value: 'OTHER_NG' },
  { label: '已确认', value: 'COMPLETED' },
];

const pressSlotIntermediatePositions = [
  { label: '首件', value: 'FIRST' },
  { label: '前段', value: 'FRONT' },
  { label: '中段', value: 'MIDDLE' },
  { label: '后段', value: 'END' },
];

type PressSlotReportType = 'CHANGEOVER' | 'END' | 'FRONT' | 'MIDDLE' | 'PROCESS_CHECK' | 'PRODUCT';
type PressSlotWorkbenchStatus = 'COMPLETED' | 'CURRENT_NG' | 'OTHER_NG' | 'PENDING';

const pressSlotReportTypeOptions: Array<{ label: string; value: PressSlotReportType }> = [
  { label: '首检', value: 'CHANGEOVER' },
  { label: '成品加工', value: 'PRODUCT' },
  { label: '中间品-前段', value: 'FRONT' },
  { label: '中间品-中段', value: 'MIDDLE' },
  { label: '中间品-后段', value: 'END' },
  { label: '过程加检', value: 'PROCESS_CHECK' },
];

const pressSlotReportTypeFilterOptions = [
  { label: '全部作业', value: 'ALL' },
  ...pressSlotReportTypeOptions,
];

const pressSlotVisualItemNames = ['黑点', '蓝点', '黄点', '红点', '针孔', '条纹', '褶皱', '波浪纹', '其他'];
const abnormalCategoryCorrectionOptions = pressSlotVisualItemNames.map((item) => ({ label: item, value: item }));

const pressSlotMiddleCheckItems = ref<AdhesiveCheckItem[]>([
  { abnormalRemark: '', actualValue: '', checkResult: 'OK', itemCategory: '中间品', itemName: '宽幅/mm', sortNo: 101, standardValue: '/' },
  ...Array.from({ length: 10 }, (_, index) => ({
    abnormalRemark: '',
    actualValue: '',
    checkResult: 'OK',
    itemCategory: '中间品',
    itemName: `厚度${index + 1}/mm`,
    sortNo: 102 + index,
    standardValue: PRESS_SLOT_INTERMEDIATE_DEFAULT_THICKNESS_STANDARD,
  })),
]);

const pressSlotProcessCheckItems = ref<AdhesiveCheckItem[]>([
  { abnormalRemark: '', actualValue: buildNowText(), checkResult: 'OK', itemCategory: '过程加检', itemName: '检测时间', sortNo: 201, standardValue: '扫码触发' },
  { abnormalRemark: '', actualValue: '待检', checkResult: 'OK', itemCategory: '过程加检', itemName: '反馈状态', sortNo: 202, standardValue: 'QMS反馈' },
]);

const pressSlotMeasuredPositions = pressSlotIntermediatePositions.filter((item) => item.value !== 'FIRST');

const intermediateSlotDepthStandardText = computed(
  () => String(intermediateForm.slotDepthStandard || '').trim() || PRESS_SLOT_INTERMEDIATE_DEFAULT_SLOT_DEPTH_STANDARD,
);
const showIntermediateSlotDepthSection = computed(() =>
  shouldShowPressSlotIntermediateDepthSection(intermediateForm as Record<string, any>),
);
const intermediateThicknessStandardText = computed(
  () => String(intermediateForm.thicknessStandard || '').trim() || PRESS_SLOT_INTERMEDIATE_DEFAULT_THICKNESS_STANDARD,
);
const intermediateThicknessColumnCount = computed(() => {
  const count = Number(intermediateForm.thicknessColumnCount);
  if (!Number.isFinite(count) || count <= 0) {
    return PRESS_SLOT_INTERMEDIATE_DEFAULT_THICKNESS_COLUMN_COUNT;
  }
  return Math.min(10, Math.trunc(count));
});
const intermediateThicknessIntervalCm = computed(() => {
  const interval = Number(intermediateForm.thicknessIntervalCm);
  if (!Number.isFinite(interval) || interval <= 0) {
    return PRESS_SLOT_INTERMEDIATE_DEFAULT_THICKNESS_INTERVAL_CM;
  }
  return Math.trunc(interval);
});
const intermediateDetailColumns = computed(() => [
  { align: 'center' as const, dataIndex: 'seq', title: '序号', width: 48 },
  { dataIndex: 'samplePositionName', title: '采样段', width: 82 },
  { dataIndex: 'sliceBatchNo', title: '片号', width: 190 },
  { dataIndex: 'widthMm', title: '宽幅/mm', width: 108 },
  ...Array.from({ length: intermediateThicknessColumnCount.value }, (_, index) => ({
    dataIndex: `thickness${index + 1}`,
    title: `${(index + 1) * intermediateThicknessIntervalCm.value}cm厚度/mm(${intermediateThicknessStandardText.value})`,
    width: index === intermediateThicknessColumnCount.value - 1 ? 196 : 190,
  })),
  { dataIndex: 'remark', title: '备注', width: 160 },
]);

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
const taskListFilteredRows = computed(() => {
  const statusFilter = taskListFilters.status;
  return taskRows.value.filter((row) => {
    const rowAny = row as Record<string, any>;
    const status = normalizeTaskListStatusValue(rowAny.status);
    if (statusFilter === 'UNFINISHED' && status === 'COMPLETED') return false;
    if (statusFilter && statusFilter !== 'ALL' && statusFilter !== 'UNFINISHED' && status !== statusFilter) return false;
    if (!containsTaskFilterText([rowAny.planNo, rowAny.id, rowAny.erpOrderNo], taskListFilters.planNo)) return false;
    if (!containsTaskFilterText([rowAny.modelCode, rowAny.modelName, rowAny.productModel, rowAny.materialCode], taskListFilters.modelCode)) return false;
    return containsTaskFilterText(
      [
        rowAny.batchNo,
        rowAny.sourceBatchNo,
        rowAny.motherBatchNo,
        rowAny.parentProductionBatchNo,
        rowAny.sourceProductionBatchNo,
        rowAny.productionBatchNo,
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

const selectedBoardEquipmentId = computed(() => boardEquipment.id ?? currentPlan.equipmentId);
const selectedBoardEquipmentCode = computed(() => boardEquipment.code || currentPlan.equipmentCode || '');
const selectedBoardEquipmentName = computed(() => boardEquipment.name || currentPlan.equipmentName || '');
const selectedBoardWorkCenterName = computed(() => boardEquipment.workCenterName || currentPlan.workCenterName || '');
const pressSlotTaskListTitle = computed(
  () => `压槽待加工列表 - ${selectedBoardEquipmentCode.value || '未绑定'} / ${selectedBoardEquipmentName.value || '-'}`,
);
const activeConsumable = computed(() =>
  activeConsumableType.value === 'BEARING' ? bearingConsumable : glueBoard,
);
const activeConsumableAvailableRange = computed(() => `${formatNumber(activeConsumable.value.stockCount || 0)} 个`);
const PRESS_SLOT_CONSUMABLE_WARNING_RATIO = 0.9;

function normalizeConsumableLimit(value: number | string | undefined, fallback: number) {
  const limit = Number(value || 0);
  return Number.isFinite(limit) && limit > 0 ? limit : fallback;
}

function isConsumableNearLimit(used: number | string | undefined, limit: number) {
  const current = Number(used || 0);
  return Number.isFinite(current) && current >= limit * PRESS_SLOT_CONSUMABLE_WARNING_RATIO;
}

const rollerLifecycleLimits = computed(() => ({
  countLimit: normalizeConsumableLimit(glueBoard.lifetimeLimitCount, 2000),
  dayLimit: normalizeConsumableLimit(glueBoard.limitDays, 60),
}));
const bearingLifecycleLimits = computed(() => ({
  countLimit: normalizeConsumableLimit(bearingConsumable.lifetimeLimitCount, 5000),
  dayLimit: normalizeConsumableLimit(bearingConsumable.limitDays, 150),
}));

function formatPressSlotConsumableProgress(usedCount: number | string | undefined, countLimit: number, useDays: number | string | undefined, dayLimit: number) {
  return [
    countLimit > 0 ? `${formatNumber(usedCount || 0)}/${formatNumber(countLimit)}片` : '',
    dayLimit > 0 ? `${formatNumber(useDays || 0)}/${formatNumber(dayLimit)}天` : '',
  ].filter(Boolean).join('，');
}

function simplifyPressSlotConsumableMessage(messageText: string | undefined, fallbackName: string) {
  const text = String(messageText || '').trim();
  if (!text) return '';
  const name = text.includes('轴承')
    ? '轴承'
    : (text.includes('压辊') || text.includes('压槽辊') ? '压槽辊' : fallbackName);
  const countMatch = text.match(/累计\s*([0-9.]+)\s*\/\s*([0-9.]+)\s*片/);
  const dayMatch = text.match(/使用\s*([0-9.]+)\s*\/\s*([0-9.]+)\s*天/);
  const progress = [
    countMatch ? `${formatNumber(countMatch[1])}/${formatNumber(countMatch[2])}片` : '',
    dayMatch ? `${formatNumber(dayMatch[1])}/${formatNumber(dayMatch[2])}天` : '',
  ].filter(Boolean).join('，');
  if (text.includes('未挂接')) return `${name}未挂接`;
  if (text.includes('已达到清洗上限') || text.includes('已达到更换上限') || text.includes('寿命已达阈值')) {
    return `${name}到限${progress ? `：${progress}` : ''}${name === '轴承' ? '，请更换' : '，请复位'}`;
  }
  if (text.includes('接近寿命阈值')) {
    return `${name}预警${progress ? `：${progress}` : ''}`;
  }
  return text
    .replace(/压辊/g, '压槽辊')
    .replace(/已达到90%预警，请复位。?/g, '90%预警')
    .replace(/请及时更换或复位/g, '请处理');
}

const rollerLifecycleWarning = computed(() => {
  const { countLimit, dayLimit } = rollerLifecycleLimits.value;
  if (glueBoard.alarm) {
    return '';
  }
  if (!isConsumableNearLimit(glueBoard.useDays, dayLimit) && !isConsumableNearLimit(glueBoard.todayUsedCount, countLimit)) {
    return '';
  }
  const progress = formatPressSlotConsumableProgress(glueBoard.todayUsedCount, countLimit, glueBoard.useDays, dayLimit);
  return `压槽辊预警${progress ? `：${progress}` : ''}`;
});
const bearingLifecycleWarning = computed(() => {
  const { countLimit, dayLimit } = bearingLifecycleLimits.value;
  if (bearingConsumable.alarm) {
    return '';
  }
  if (!isConsumableNearLimit(bearingConsumable.useDays, dayLimit) && !isConsumableNearLimit(bearingConsumable.todayUsedCount, countLimit)) {
    return '';
  }
  const progress = formatPressSlotConsumableProgress(bearingConsumable.todayUsedCount, countLimit, bearingConsumable.useDays, dayLimit);
  return `轴承预警${progress ? `：${progress}` : ''}`;
});
const pressSlotConsumableLifecycleMessage = computed(() => {
  const warnings = [rollerLifecycleWarning.value, bearingLifecycleWarning.value].filter(Boolean);
  return warnings.join('；');
});
const hasConsumableLifecycleWarning = computed(() =>
  !!(glueBoard.warning || bearingConsumable.warning || rollerLifecycleWarning.value || bearingLifecycleWarning.value),
);
const glueBoardStatusMeta = computed(() => {
  if (glueBoard.alarm || bearingConsumable.alarm) return { color: 'red', text: '需处理' };
  if (hasConsumableLifecycleWarning.value) return { color: 'orange', text: '预警' };
  if (!glueBoard.id || !bearingConsumable.id) return { color: 'default', text: '未挂接' };
  return { color: 'green', text: '正常' };
});

const changeoverStampMeta = computed(() => {
  if (!currentPlan.planNo) {
    return { stampClass: 'empty', stampText: '待加载', time: '扫码计划后校验首检', timeLabel: '' };
  }
  const motherBatchNo = firstInspection.value?.inspectionScopeBatchNo || currentMotherSegmentBatchNo.value || '-';
  const summary = firstInspection.value;
  if (!summary?.faiId) {
    return { stampClass: 'ng', stampText: '尚未首检', time: `分段批号 ${motherBatchNo}`, timeLabel: '' };
  }
  if (summary.overallQualified) {
    return { stampClass: 'ok', stampText: '整体合格', time: '任一首检已判定合格', timeLabel: '' };
  }
  if (isFirstInspectionSubmitted(summary)) {
    return {
      stampClass: 'ok',
      stampText: '已送检',
      time: displayDateTimeText(summary.faiApplyTime || summary.faiReturnTime),
      timeLabel: '送检时间：',
    };
  }
  if (['CANCELED', 'REJECTED'].includes(String(summary.faiStatus || '').toUpperCase())) {
    return {
      stampClass: 'ng',
      stampText: '首检已失效',
      time: displayDateTimeText(summary.faiReturnTime || summary.faiApplyTime),
      timeLabel: '结果更新时间：',
    };
  }
  return {
    stampClass: 'waiting',
    stampText: summary.displayText || resolveFirstInspectionDisplayText(summary.faiStatus, summary.faiJudgment),
    time: displayDateTimeText(summary.faiApplyTime || summary.faiReturnTime),
    timeLabel: '送检时间：',
  };
});
const pressSlotConsumablesReady = computed(() =>
  !!glueBoard.id
  && !!bearingConsumable.id
  && !glueBoard.alarm
  && !bearingConsumable.alarm,
);
const pressSlotConsumableTitleMessage = computed(() =>
  [...new Set([
    glueBoard.alarm,
    bearingConsumable.alarm,
    pressSlotConsumableLifecycleMessage.value,
    rollerLifecycleWarning.value ? '' : glueBoard.warning,
    bearingLifecycleWarning.value ? '' : bearingConsumable.warning,
  ].filter(Boolean))]
    .join('；'),
);
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
const currentMotherSegmentBatchNo = computed(() => resolveCurrentPlanSourceBatchNo());
const visibleSourceGroups = computed<SourceGroup[]>(() => {
  const selectedBatchNo = currentMotherSegmentBatchNo.value;
  return sourceGroups.value
    .filter((group) => !selectedBatchNo || group.baseBatchNo === selectedBatchNo)
    .map((group) => {
      const segments = group.segments.filter(matchPressSlotSegment);
      return {
        ...group,
        segments,
        totalAvailableLength: segments.reduce((sum, item) => sum + Number(item.availableLength || 0), 0),
        totalOutputLength: segments.reduce((sum, item) => sum + Number(item.outputLength || 0), 0),
        totalUsedLength: segments.reduce((sum, item) => sum + Number(item.usedLength || 0), 0),
      };
    })
    .filter((group) => group.segments.length > 0);
});
const visualDisplaySourceGroups = computed<SourceGroup[]>(() => {
  if (!transferPrintSelectionMode.value) return visibleSourceGroups.value;
  return sourceGroups.value
    .map((group) => {
      const segments = group.segments.filter(matchPressSlotSegment);
      return {
        ...group,
        segments,
        totalAvailableLength: segments.reduce((sum, item) => sum + Number(item.availableLength || 0), 0),
        totalOutputLength: segments.reduce((sum, item) => sum + Number(item.outputLength || 0), 0),
        totalUsedLength: segments.reduce((sum, item) => sum + Number(item.usedLength || 0), 0),
      };
    })
    .filter((group) => group.segments.length > 0);
});
const selectableTransferReports = computed(() => {
  const rows = new Map<string, MesHcAdhesiveConsoleApi.ReportItem>();
  visualDisplaySourceGroups.value.forEach((group) => {
    group.segments.forEach((segment) => {
      const record = getPrintableTransferReportBySegment(segment);
      const key = getTransferReportKey(record);
      if (key) rows.set(key, record as MesHcAdhesiveConsoleApi.ReportItem);
    });
  });
  return Array.from(rows.values());
});
const selectedTransferReports = computed(() =>
  selectedTransferReportIds.value
    .map((id) => reportRecords.value.find((record) => getTransferReportKey(record) === id))
    .filter(isPrintableTransferReport),
);
const selectableTransferReportCount = computed(() => selectableTransferReports.value.length);
const selectedTransferReportCount = computed(() => selectedTransferReports.value.length);
const allTransferReportsSelected = computed(
  () =>
    selectableTransferReportCount.value > 0 &&
    selectableTransferReports.value.every((record) => selectedTransferReportIds.value.includes(getTransferReportKey(record))),
);
const currentMotherSegments = computed(() => {
  const currentMother = currentMotherSegmentBatchNo.value;
  return sourceGroups.value
    .filter((group) => !currentMother || group.baseBatchNo === currentMother)
    .flatMap((group) => group.segments);
});
const visiblePressSlotSliceCount = computed(() => visibleSourceGroups.value.reduce((sum, group) => sum + group.segments.length, 0));
const visiblePressSlotPendingCount = computed(() =>
  visibleSourceGroups.value.reduce((sum, group) => sum + group.segments.filter((segment) => getPressSlotSegmentStatus(segment) === 'PENDING').length, 0),
);
const visiblePressSlotCompletedCount = computed(() =>
  visibleSourceGroups.value.reduce((sum, group) => sum + group.segments.filter((segment) => getPressSlotSegmentStatus(segment) === 'COMPLETED').length, 0),
);
const visiblePressSlotCurrentNgCount = computed(() =>
  visibleSourceGroups.value.reduce((sum, group) => sum + group.segments.filter(hasPressSlotCurrentProcessNg).length, 0),
);
const visiblePressSlotOtherNgCount = computed(() =>
  visibleSourceGroups.value.reduce((sum, group) => sum + group.segments.filter(isPressSlotSlittingNgSegment).length, 0),
);

const changeoverInspectionRows = computed(() =>
  changeoverInspections.value.map((record) => ({
    ...record,
    feedbackResult: record.feedbackResult || '-',
    feedbackTime: displayDateTimeText(record.feedbackTime),
    inspectionStatusText: record.inspectionStatusName || getInspectionStatusText(record.inspectionStatus),
    motherSegmentBatchNo: record.motherSegmentBatchNo || resolveMotherSegmentBatchNo(record.pressSlotSliceNo || ''),
    sliceBatchNo: record.pressSlotSliceNo || '-',
    submitTime: displayDateTimeText(record.submitTime || record.recordTime),
  })),
);
const firstInspectionProcessRows = computed<FirstInspectionProcessRow[]>(() => {
  const currentMother = normalizePressSlotMiddleBatchNo(getCurrentChangeoverMotherBatchNo());
  const sourceRows = [
    ...firstInspectionFaiRows.value.map((record) => ({ inspectionType: 'FIRST_INSPECTION' as FirstInspectionScanMode, record })),
    ...processCheckFaiRows.value.map((record) => ({ inspectionType: 'PROCESS_CHECK' as FirstInspectionScanMode, record })),
  ];
  return sourceRows
    .map(({ inspectionType, record }) => {
      const sliceNo = String(record.productBatchNo || '').trim();
      const motherBatchNo = normalizePressSlotMiddleBatchNo(record.inspectionScopeBatchNo || resolveMotherSegmentBatchNo(sliceNo));
      const rowKey = getFirstInspectionProcessFormKey(sliceNo, inspectionType);
      return {
        ...record,
        inspectionType,
        inspectionTypeName: inspectionType === 'PROCESS_CHECK' ? '过程加检' : '首检',
        planId: currentPlan.planId,
        planNo: currentPlan.planNo,
        planOperationId: currentPlan.planOperationId,
        firstInspectionSliceNo: sliceNo || '-',
        inspectionResultText: getFirstInspectionFaiResultText(record),
        motherSegmentBatchNo: motherBatchNo,
        pressSlotSliceNo: sliceNo,
        productionMaterialCode: currentPlan.materialCode,
        productionModelCode: currentPlan.modelCode,
        processFormRecord: firstInspectionProcessFormRecords.value[rowKey],
        recordTime: record.faiApplyTime,
        rowKey,
        submitTime: record.faiApplyTime,
        submitTimeText: displayDateTimeText(record.faiApplyTime),
      };
    })
    .filter((record) =>
      record.firstInspectionSliceNo !== '-'
      && (!currentMother || normalizePressSlotMiddleBatchNo(record.motherSegmentBatchNo) === currentMother),
    );
});
const firstInspectionRows = computed(() => {
  const rows = firstInspectionFaiRows.value.length ? firstInspectionFaiRows.value : [firstInspection.value || {}];
  return rows.map((summary, index) => ({
    ...summary,
    id: summary.faiId || `PRESS_SLOT_FAI_${index}`,
    applyTimeText: displayDateTimeText(summary.faiApplyTime),
    productBatchNo: summary.productBatchNo || '-',
    returnTimeText: displayDateTimeText(summary.faiReturnTime),
    standardText: summary.faiStandardNo
      ? `${summary.faiStandardNo}${summary.faiStandardVersion ? ` / ${summary.faiStandardVersion}` : ''}`
      : '-',
    statusText: summary.faiId
      ? summary.displayText || resolveFirstInspectionDisplayText(summary.faiStatus, summary.faiJudgment)
      : '未提交',
  }));
});
const processCheckInspectionRows = computed(() =>
  processCheckFaiRows.value.map((record, index) => ({
    ...record,
    id: record.faiId || record.sourceReportId || record.sourceReportNo || `${record.productBatchNo || 'PROCESS'}-${index}`,
    applyTimeText: displayDateTimeText(record.faiApplyTime),
    returnTimeText: displayDateTimeText(record.faiReturnTime),
    standardText: record.faiStandardNo
      ? `${record.faiStandardNo}${record.faiStandardVersion ? ` / ${record.faiStandardVersion}` : ''}`
      : '-',
    statusText: record.displayText || resolveFirstInspectionDisplayText(record.faiStatus, record.faiJudgment),
  })),
);
const latestProcessCheckInspection = computed(() =>
  processCheckInspectionRows.value.find((record) => !!record.faiId) || null,
);
function decorateAbnormalLockRecord(record: MesHcAdhesiveConsoleApi.PressSlotAbnormalLockRecord, index = 0) {
  return {
    ...record,
    abnormalFeedbackTimeText: displayDateTimeText(record.abnormalFeedbackTime),
    abnormalSubmitTimeText: displayDateTimeText(record.abnormalSubmitTime),
    confirmTimeText: displayDateTimeText(record.confirmTime),
    id: record.id || record.reportId || record.lockId || `${record.productionBatchNo || 'LOCK'}-${index}`,
    lockStartTimeText: displayDateTimeText(record.lockStartTime || record.abnormalSubmitTime),
    lockStatusText: record.locked ? '锁定中' : '已放行',
    releaseFeedbackTimeText: displayDateTimeText(record.releaseFeedbackTime),
    releaseSubmitTimeText: displayDateTimeText(record.releaseSubmitTime),
    releaseTimeText: displayDateTimeText(record.releaseTime),
  };
}

function isActivePressSlotAbnormalLock(record?: MesHcAdhesiveConsoleApi.PressSlotAbnormalLockRecord | null) {
  if (!record) return false;
  const lockStatus = String(record.lockStatus || '').toUpperCase();
  if (record.locked === false || lockStatus === 'RELEASED') return false;
  return record.locked === true || lockStatus === 'LOCKED';
}

const abnormalLockTableRows = computed(() =>
  abnormalLockRows.value.map((record, index) => decorateAbnormalLockRecord(record, index)),
);
const activeAbnormalLockRecord = computed(() => {
  const activeSummary = activeAbnormalLockSummary.value;
  if (isActivePressSlotAbnormalLock(activeSummary)) {
    return decorateAbnormalLockRecord(activeSummary, -1);
  }
  return abnormalLockTableRows.value.find((record) => isActivePressSlotAbnormalLock(record)) || null;
});
const activeAbnormalLockStartText = computed(() => normalizeDateTimeText(
  activeAbnormalLockRecord.value?.lockStartTime
  || activeAbnormalLockRecord.value?.abnormalSubmitTime
  || activeAbnormalLockRecord.value?.abnormalFeedbackTime,
));
const hasActivePressSlotAbnormalLock = computed(() => !!activeAbnormalLockRecord.value);
const requiresRestartFirstInspection = computed(() => firstInspection.value?.restartRequired === true);
const isProcessCheckNgRestartFirstInspectionMode = computed(() =>
  firstInspectionScanMode.value === 'FIRST_INSPECTION' && requiresRestartFirstInspection.value,
);
const abnormalLockedSliceNoSet = computed(() => new Set(
  abnormalLockRows.value
    .filter((record) => isActivePressSlotAbnormalLock(record))
    .map((record) => String(record.productionBatchNo || '').trim())
    .filter(Boolean),
));
const abnormalLockRecordMap = computed(() => {
  const result = new Map<string, MesHcAdhesiveConsoleApi.PressSlotAbnormalLockRecord>();
  abnormalLockRows.value.forEach((record) => {
    const batchNo = String(record.productionBatchNo || '').trim();
    if (batchNo) result.set(batchNo, record);
  });
  return result;
});
const currentProcessParamFirstInspectionRecord = computed(() => {
  const currentMother = currentMotherSegmentBatchNo.value;
  return processParameterRecords.value.find((record) =>
    !!record.firstInspectionSliceNo
    && (!currentMother || isSameBatchNo(record.parentProductionBatchNo, currentMother)),
  ) || null;
});

function isProcessCheckFaiSummary(summary?: MesHcAdhesiveConsoleApi.FaiSummary) {
  return String(summary?.inspectionScene || '').toUpperCase() === 'PROCESS_CHECK'
    || String(summary?.sourceReportNo || '').toUpperCase().includes('-PROCESS-CHECK-');
}

const currentFirstInspectionSliceNo = computed(() =>
  (['ABNORMAL_RELEASE', 'PROCESS_CHECK'].includes(firstInspectionScanMode.value) ? processCheckScanReport.value?.productionBatchNo : '')
  || firstInspectionSource.value?.productionBatchNo
  || firstInspectionSource.value?.confirmedBatchNo
  || currentProcessParamFirstInspectionRecord.value?.firstInspectionSliceNo
  || (isProcessCheckFaiSummary(firstInspection.value) ? '' : firstInspection.value?.productBatchNo)
  || firstInspectionScanNo.value
  || '',
);

const firstInspectionSourceStatusMeta = computed(() => {
  if (firstInspectionScanMode.value === 'ABNORMAL_RELEASE') {
    if (!processCheckScanReport.value) return { color: 'default', text: '未扫码放行片' };
    return isAdhesiveRecordConfirmed(processCheckScanReport.value.reportStatus)
      ? { color: 'success', text: '已带入锁定片号' }
      : { color: 'warning', text: '放行片尚未确认' };
  }
  if (firstInspectionScanMode.value === 'PROCESS_CHECK') {
    if (!processCheckScanReport.value) return { color: 'default', text: '未扫码压槽片' };
    return isAdhesiveRecordConfirmed(processCheckScanReport.value.reportStatus)
      ? { color: 'success', text: '已带入已确认压槽片' }
      : { color: 'warning', text: '压槽片尚未确认' };
  }
  const selfCheck = String(firstInspectionSource.value?.selfCheck || '').toUpperCase();
  if (!firstInspectionSource.value) return { color: 'default', text: '未扫码批号' };
  if (selfCheck === 'NG') return { color: 'error', text: '分切NG，可送首检复判' };
  return { color: 'success', text: '分切正常' };
});

const firstInspectionScanDialogTitle = computed(() => {
  if (firstInspectionScanMode.value === 'ABNORMAL_RELEASE') return '压槽风险片槽深复测扫码';
  return firstInspectionScanMode.value === 'PROCESS_CHECK' ? '压槽过程加检扫码' : '压槽首检扫码';
});
const firstInspectionScanDialogBasis = computed(() => {
  if (firstInspectionScanMode.value === 'ABNORMAL_RELEASE') {
    return '仅可从“过程加检送出”至“品质返回NG结果”之间已锁定的风险片中选择一片复测槽深；复测OK后放行该风险区间。';
  }
  return firstInspectionScanMode.value === 'PROCESS_CHECK'
    ? '需要选择一片已确认压槽片送过程加检；加检结果记录在过程加检页签，不影响后续继续扫码报工。'
    : isProcessCheckNgRestartFirstInspectionMode.value
      ? '检验已返回NG，请扫描新片重新首检；送检成功即可继续报工，再次NG时需再次送新首检。'
      : '当前分段尚无有效首检，或最近首检、过程加检、扫码确认不属于当前分段批号时，需要重新首检；同一分段仍需按规则提交过程加检。';
});
const firstInspectionScanSubmitText = computed(() => {
  if (firstInspectionScanMode.value === 'ABNORMAL_RELEASE') return '扫码并提交风险复测';
  return firstInspectionScanMode.value === 'PROCESS_CHECK'
    ? '扫码并提交加检'
    : isProcessCheckNgRestartFirstInspectionMode.value ? '扫码并提交重新首检' : '扫码并提交首检';
});
const firstInspectionScanSummaryLabel = computed(() => {
  if (firstInspectionScanMode.value === 'ABNORMAL_RELEASE') return '复测类型';
  return firstInspectionScanMode.value === 'PROCESS_CHECK'
    ? '加检类型'
    : isProcessCheckNgRestartFirstInspectionMode.value ? '重新首检' : '压槽首检';
});
const firstInspectionScanSummaryText = computed(() => {
  if (firstInspectionScanMode.value === 'ABNORMAL_RELEASE') return '风险片槽深复测';
  return firstInspectionScanMode.value === 'PROCESS_CHECK'
    ? '过程加检'
    : isProcessCheckNgRestartFirstInspectionMode.value ? '过程加检NG后重新首检' : changeoverStampMeta.value.stampText;
});
const firstInspectionScanPlaceholder = computed(() => {
  if (firstInspectionScanMode.value === 'ABNORMAL_RELEASE') return '请扫描已锁定风险片号，也可人工输入';
  return firstInspectionScanMode.value === 'PROCESS_CHECK'
    ? '请扫描已确认压槽片号，也可人工输入'
    : isProcessCheckNgRestartFirstInspectionMode.value
      ? '请扫描未送检的新首检片号，也可人工输入'
      : '请扫描当前分段批号下任一首检批号，也可人工输入';
});
const firstInspectionScanCurrentLabel = computed(() => {
  if (firstInspectionScanMode.value === 'ABNORMAL_RELEASE') return '当前风险复测片号';
  return firstInspectionScanMode.value === 'PROCESS_CHECK'
    ? '当前加检片号'
    : isProcessCheckNgRestartFirstInspectionMode.value ? '当前重新首检批号' : '当前首检批号';
});
const firstInspectionProcessModalTitle = computed(() =>
  `${currentFirstInspectionProcessRow.value?.inspectionTypeName || '首检'}点检 - ${getFirstInspectionProcessFormDisplayName(currentFirstInspectionProcessRow.value || undefined)}`,
);

function isFirstInspectionSubmitted(summary = firstInspection.value) {
  if (summary?.restartRequired) return false;
  const status = String(summary?.faiStatus || '').toUpperCase();
  return summary?.allowReportSubmit === true
    || ['PENDING', 'INSPECTING', 'WAITING_QA', 'COMPLETED'].includes(status);
}

function getInspectionStatusText(status?: string) {
  if (status === 'DRAFT') return '草稿';
  if (status === 'QUALIFIED') return '已检';
  if (status === 'ABNORMAL') return '已检';
  if (status === 'CANCELLED') return '已取消';
  return '待检';
}

function resolveFirstInspectionDisplayText(status?: string, judgment?: string) {
  if (status === 'COMPLETED' && judgment === 'OK') return '已完成';
  if (status === 'REJECTED' || judgment === 'NG') return '已驳回';
  if (status === 'PENDING') return '待检测';
  if (status === 'INSPECTING') return '检测中';
  if (status === 'WAITING_QA') return '待品质复核';
  if (status === 'SUSPENDED') return '已挂起';
  if (status === 'REWORKING') return '调机中';
  if (status === 'CANCELED') return '已取消';
  return '未提交';
}

function getFirstInspectionStatusMeta(status?: string, judgment?: string) {
  if (!firstInspection.value?.faiId) return { color: 'default', text: '未提交' };
  if (status === 'COMPLETED' && judgment === 'OK') return { color: 'success', text: '已完成' };
  if (status === 'REJECTED') return { color: 'error', text: '已驳回' };
  if (judgment === 'NG') return { color: 'error', text: '检验NG' };
  if (status === 'PENDING') return { color: 'default', text: '待检测' };
  if (status === 'INSPECTING') return { color: 'processing', text: '检测中' };
  if (status === 'WAITING_QA') return { color: 'warning', text: '待品质复核' };
  if (status === 'SUSPENDED') return { color: 'warning', text: '已挂起' };
  if (status === 'REWORKING') return { color: 'error', text: '调机中' };
  if (status === 'CANCELED') return { color: 'default', text: '已取消' };
  return { color: 'default', text: '待处理' };
}

const firstInspectionSummaryStatusMeta = computed(() => {
  if (requiresRestartFirstInspection.value) return { color: 'error', text: 'NG，待重新首检' };
  if (firstInspection.value?.overallQualified) return { color: 'success', text: '整体合格' };
  return getFirstInspectionStatusMeta(firstInspection.value?.faiStatus, firstInspection.value?.faiJudgment);
});

function canSubmitFirstInspection() {
  return true;
}

const firstInspectionSubmitText = computed(() => '提交首检申请');

function getInspectionResultText(record?: Partial<MesHcAdhesiveConsoleApi.ChangeoverInspection>) {
  if (!record?.feedbackResult) return '-';
  return record.feedbackResult;
}

const processParameterRows = computed(() =>
  processParameterRecords.value.map((record) => ({
    ...record,
    confirmTime: displayDateTimeText(record.confirmTime),
    fillTime: displayDateTimeText(record.fillTime),
    formName: record.formName || 'CMP压槽工艺参数表',
    itemCount: normalizeProcessParamItems(record.items).length,
    motherBatchNo: normalizePressSlotMiddleBatchNo(record.motherBatchNo || record.parentProductionBatchNo || ''),
    recordStatus: record.recordStatus || record.reportStatus || 'SUBMITTED',
    statusName: record.statusName || getRecordStatusMeta(record.recordStatus || record.reportStatus).text,
  })),
);
const reportInspectionInfo = computed(() => {
  const extra = parseRecordExtra(activeRecord.value || {}) as Record<string, any>;
  return {
    feedbackResult: extra.feedbackResult || extra.inspectionResult || '-',
    feedbackTime: extra.feedbackTime || '',
    submitTime: extra.submitTime || reportForm.startTime || '',
  };
});
const shouldShowVisualInspectionTab = computed(() => reportForm.reportType !== 'CHANGEOVER');
const isVisualInspectionEditable = computed(() => reportDialogMode.value !== 'view' && shouldShowVisualInspectionTab.value);
const hasActiveVisualItems = computed(() => visualInspectionItems.value.some((item) => isVisualItemActive(item)));
const currentReportVisualIssue = computed(() =>
  shouldShowVisualInspectionTab.value
  && (normalizeVisualResult(reportForm.selfCheck) === 'NG' || hasActiveVisualItems.value),
);
const currentReportVisualIssueText = computed(() =>
  currentReportVisualIssue.value ? '外观异常' : '合格',
);
const canCorrectAbnormalCategory = computed(() =>
  reportDialogMode.value === 'view'
  && !!activeRecord.value?.id
  && shouldShowVisualInspectionTab.value
  && hasPressSlotVisualIssue(activeRecord.value)
  && !isPreProcessSelfCheckAbnormal(parseRecordExtra(activeRecord.value))
  && hasAccessByCodes([ABNORMAL_CATEGORY_CORRECT_PERMISSION]),
);

function buildDefaultPressSlotIntermediateDetails(): MesHcAdhesiveConsoleApi.IntermediateDetail[] {
  return pressSlotMeasuredPositions.map((item, index) => ({
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
  if (isPressSlotMiddleReportType(reportForm.reportType)) {
    return ['中间品'];
  }
  const baseItems = reportForm.reportType === 'CHANGEOVER'
    ? ensurePressSlotChangeoverItems(checkTemplate.value)
    : reportForm.reportType === 'PROCESS_CHECK'
      ? getPressSlotProcessCheckItems()
      : getTemperatureCheckItems();
  const categories = Array.from(new Set(
    baseItems
      .map((item) => item.itemCategory || '其他')
      .filter((category) => category !== '压槽半成品' && category !== '工艺参数'),
  ));
  const order = ['环境', '压槽机', '压辊', '工艺参数', '过程加检', '其他'];
  return categories.sort((a, b) => {
    const aIndex = order.includes(a) ? order.indexOf(a) : order.length;
    const bIndex = order.includes(b) ? order.indexOf(b) : order.length;
    return aIndex - bIndex;
  });
});
const reportSubmitButtonText = computed(() => {
  if (shouldShowVisualInspectionTab.value && activeReportTab.value !== 'visual-inspection') {
    return '下一步：外观检验';
  }
  return '保存并扫码确认';
});

const recordConfirmTypeBasis = computed(() => {
  return '扫码确认默认按成品加工登记，确认后直接进入外观检验；中间品前/中/后段请在详情中手动设置。';
});

const recordColumns = [
  { dataIndex: 'parentProductionBatchNo', key: 'motherBatchNo', title: '分段批号', width: 180 },
  { dataIndex: 'productionBatchNo', key: 'productionBatchNo', title: '压槽片号', width: 160 },
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
];

const changeoverInspectionColumns = [
  { dataIndex: 'faiNo', title: '首检单号', width: 180 },
  { dataIndex: 'productBatchNo', title: '压槽片号', width: 170 },
  { dataIndex: 'applyTimeText', title: '申请时间', width: 170 },
  { dataIndex: 'inspectionStatus', title: 'FAI状态', width: 130 },
  { dataIndex: 'returnTimeText', title: '最近回写时间', width: 170 },
  { dataIndex: 'faiRejectReason', title: '驳回/NG说明', width: 220 },
  { dataIndex: 'action', fixed: 'right' as const, title: '操作', width: 290 },
];

const firstInspectionProcessColumns = [
  { dataIndex: 'inspectionTypeName', title: '类型', width: 130 },
  { dataIndex: 'firstInspectionSliceNo', title: '压槽片号', width: 220 },
  { dataIndex: 'submitTimeText', title: '送检时间', width: 200 },
  { dataIndex: 'inspectionResultText', title: '送检结果', width: 140 },
  { dataIndex: 'action', fixed: 'right' as const, title: '操作', width: 220 },
  { dataIndex: 'processFormStatus', fixed: 'right' as const, title: '表单状态', width: 120 },
];

const processCheckInspectionColumns = [
  { dataIndex: 'faiNo', title: '加检单号', width: 180 },
  { dataIndex: 'productBatchNo', title: '压槽片号', width: 170 },
  { dataIndex: 'applyTimeText', title: '申请时间', width: 170 },
  { dataIndex: 'inspectionStatus', title: 'FAI状态', width: 130 },
  { dataIndex: 'standardText', title: '采用标准', width: 190 },
  { dataIndex: 'returnTimeText', title: '最近回写时间', width: 170 },
  { dataIndex: 'faiRejectReason', title: '驳回/NG说明', width: 220 },
  { dataIndex: 'action', fixed: 'right' as const, title: '操作', width: 290 },
];

const abnormalLockColumns = [
  { dataIndex: 'productionBatchNo', fixed: 'left' as const, title: '片号', width: 170 },
  { dataIndex: 'lockStatus', title: '锁定状态', width: 110 },
  { dataIndex: 'lockStartTimeText', title: '锁定起点', width: 170 },
  { dataIndex: 'confirmTimeText', title: '扫码确认时间', width: 170 },
  { dataIndex: 'confirmerName', title: '确认人', width: 110 },
  { dataIndex: 'abnormalSampleBatchNo', title: '异常检验片号', width: 170 },
  { dataIndex: 'abnormalResult', title: '异常结果', width: 110 },
  { dataIndex: 'abnormalSubmitTimeText', title: '异常送检时间', width: 170 },
  { dataIndex: 'abnormalFeedbackTimeText', title: '异常反馈时间', width: 170 },
  { dataIndex: 'abnormalInspectorName', title: '异常检验人', width: 120 },
  { dataIndex: 'releaseSampleBatchNo', title: '复检片号', width: 170 },
  { dataIndex: 'releaseResult', title: '复检结果', width: 110 },
  { dataIndex: 'releaseSubmitTimeText', title: '复检送检时间', width: 170 },
  { dataIndex: 'releaseFeedbackTimeText', title: '复检反馈时间', width: 170 },
  { dataIndex: 'releaseInspectorName', title: '复检检验人', width: 120 },
];

const processParameterColumns = [
  { dataIndex: 'formName', title: '表单名称', width: 180 },
  { dataIndex: 'reportDate', title: '生产日期', width: 120 },
  { dataIndex: 'planNo', title: '计划号', width: 150 },
  { dataIndex: 'motherBatchNo', title: '分段批号', width: 170 },
  { dataIndex: 'modelCode', title: '型号', width: 130 },
  { dataIndex: 'itemCount', title: '明细数', width: 90 },
  { dataIndex: 'recordStatus', title: '状态', width: 100 },
  { dataIndex: 'fillUserName', title: '填写人', width: 120 },
  { dataIndex: 'fillTime', title: '填写时间', width: 170 },
  { dataIndex: 'confirmUserName', title: '确认人', width: 120 },
  { dataIndex: 'confirmTime', title: '确认时间', width: 170 },
  { dataIndex: 'action', fixed: 'right' as const, title: '操作', width: 100 },
];

const processParamDetailColumns = [
  { dataIndex: 'reportDate', title: '日期', width: 110 },
  { dataIndex: 'modelCode', title: '型号', width: 120 },
  { dataIndex: 'motherBatchNo', title: '批号', width: 150 },
  { dataIndex: 'productionBatchNo', title: '压槽片号', width: 170 },
  { dataIndex: 'measuredTemperature1', title: '实测点温度1(℃)', width: 150 },
  { dataIndex: 'measuredTemperature2', title: '实测点温度2(℃)', width: 150 },
  { dataIndex: 'measuredTemperature3', title: '实测点温度3(℃)', width: 150 },
  { dataIndex: 'measuredTemperature4', title: '实测点温度4(℃)', width: 150 },
  { dataIndex: 'measuredTemperature5', title: '实测点温度5(℃)', width: 150 },
  { dataIndex: 'recorderName', title: '记录人', width: 110 },
  { dataIndex: 'remark', title: '备注', width: 180 },
];
const processParamDetailTableWidth = processParamDetailColumns.reduce(
  (total, column) => total + (Number(column.width) || 0),
  0,
);

const pressSlotIntermediateRecordColumns = [
  { dataIndex: 'batchNo', title: '批号', width: 190 },
  { dataIndex: 'recordDate', title: '记录日期', width: 120 },
  { dataIndex: 'modelCode', title: '型号', width: 140 },
  { dataIndex: 'materialCode', title: '料号', width: 150 },
  { dataIndex: 'frontSliceNo', title: '前段片号', width: 170 },
  { dataIndex: 'middleSliceNo', title: '中段片号', width: 170 },
  { dataIndex: 'endSliceNo', title: '后段片号', width: 170 },
  { dataIndex: 'recordStatus', title: '状态', width: 110 },
  { dataIndex: 'recorderName', title: '记录人', width: 120 },
  { dataIndex: 'action', fixed: 'right' as const, title: '操作', width: 130 },
];

const reportProcessColumns = [
  { dataIndex: 'itemCategory', title: '类别', width: 120 },
  { dataIndex: 'itemName', title: '工艺参数项目', width: 170 },
  { dataIndex: 'standardValue', title: '标准', width: 180 },
  { dataIndex: 'actualValue', title: '实测值', width: 200 },
  { dataIndex: 'abnormalRemark', title: '异常备注', width: 180 },
];

const firstInspectionProductionCheckColumns = [
  { dataIndex: 'seq', title: '序号', width: 70 },
  { dataIndex: 'itemCategory', title: '项目类别', width: 120 },
  { dataIndex: 'itemName', title: '点检项目', width: 260 },
  { dataIndex: 'standardValue', title: '标准', width: 240 },
  { dataIndex: 'actualValue', title: '记录值', width: 220 },
  { dataIndex: 'abnormalRemark', title: '异常说明', width: 220 },
];
const firstInspectionProductionCheckTableWidth = firstInspectionProductionCheckColumns.reduce(
  (total, column) => total + (Number(column.width) || 0),
  0,
);

function normalizeRows(payload: any): any[] {
  const source = payload?.data ?? payload;
  if (Array.isArray(source)) return source;
  if (Array.isArray(source?.list)) return source.list;
  if (Array.isArray(source?.rows)) return source.rows;
  if (Array.isArray(source?.records)) return source.records;
  return [];
}

function formatNumber(value?: number | string) {
  const num = Number(value || 0);
  if (!Number.isFinite(num)) return '0';
  return Number(num.toFixed(3)).toString();
}

function getPressSlotQtimeText(qtime?: MesHcAdhesiveConsoleApi.QtimeInfo) {
  if (!qtime) return '-';
  if (qtime.status === 'WAITING_SOURCE_FINISH') return '0分钟（并行开工）';
  if (qtime.status === 'MISSING_SOURCE_TIME') return '上道未完工';
  if (qtime.status === 'NO_RULE') return '未配置';
  const minutes = Math.max(0, Number(qtime.elapsedMinutes || 0));
  return qtime.timeout ? `超时 ${minutes}分钟` : `${minutes}分钟`;
}

function getPressSlotQtimeColor(qtime?: MesHcAdhesiveConsoleApi.QtimeInfo) {
  if (qtime?.timeout) return 'red';
  if (qtime?.status === 'WAITING_SOURCE_FINISH') return 'blue';
  if (qtime?.status === 'NORMAL') return 'green';
  return 'default';
}

function mergePressSlotBatchQtime(
  current?: MesHcAdhesiveConsoleApi.QtimeInfo,
  candidate?: MesHcAdhesiveConsoleApi.QtimeInfo,
) {
  if (!current) return candidate;
  if (!candidate) return current;
  if (!current.targetStarted) return candidate.targetStarted ? candidate : current;
  if (!candidate.targetStarted) return current;
  const currentStart = String(current.targetStartTime || '9999-12-31 23:59:59');
  const candidateStart = String(candidate.targetStartTime || '9999-12-31 23:59:59');
  return candidateStart < currentStart ? candidate : current;
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
    .map(buildReportRange)
    .filter((item): item is SourceReportRange => !!item)
    .sort((a, b) => a.start - b.start || a.end - b.end);
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
  for (const group of sourceGroups.value) {
    const segment = group.segments.find((item) => String(item.batchNo || '').trim().toUpperCase() === scanned);
    if (segment) return { group, segment };
  }
  return undefined;
}

function findReportBySourceBatchNo(batchNo?: string) {
  const sourceBatchNo = String(batchNo || '');
  if (!sourceBatchNo) return undefined;
  return reportRecords.value
    .filter((record) => record.sourceProductionBatchNo === sourceBatchNo || record.productionBatchNo === sourceBatchNo)
    .sort((a, b) => Number(b.id || 0) - Number(a.id || 0))[0];
}

function findReportBySegment(segment?: SourceSegment) {
  return findReportBySourceBatchNo(segment?.batchNo);
}

function getPressSlotSegmentEditBlockedReason(segment: SourceSegment) {
  return String(findReportBySegment(segment)?.editBlockedReason || '').trim();
}

function isPrintableTransferReport(record?: MesHcAdhesiveConsoleApi.ReportItem | null): record is MesHcAdhesiveConsoleApi.ReportItem {
  return Boolean(record?.id && record.productionBatchNo);
}

function getPrintableTransferReportBySegment(segment: SourceSegment) {
  const record = findReportBySegment(segment);
  return isPrintableTransferReport(record) ? record : null;
}

function getTransferReportKey(record?: MesHcAdhesiveConsoleApi.ReportItem | null) {
  return record?.id == null ? '' : String(record.id);
}

function isTransferSegmentSelected(segment: SourceSegment) {
  const record = getPrintableTransferReportBySegment(segment);
  const key = getTransferReportKey(record);
  return !!key && selectedTransferReportIds.value.includes(key);
}

function normalizePressSlotCheckItemName(name?: string) {
  const text = String(name || '');
  const temperatureIndex = text.match(/(?:温度点|温度)(\d+)/)?.[1];
  if (temperatureIndex) return `实测点温度${temperatureIndex}(℃)`;
  return text;
}

function applyReportCheckItems(checkItems?: MesHcAdhesiveConsoleApi.CheckItem[]) {
  const savedItems = Array.isArray(checkItems) ? checkItems : [];
  checkTemplate.value = checkTemplate.value.map((item) => {
    const matched = savedItems.find(
      (saved) => normalizePressSlotCheckItemName(saved.itemName) === normalizePressSlotCheckItemName(item.itemName),
    );
    if (!matched) return item;
    return {
      ...item,
      abnormalRemark: matched.abnormalRemark || '',
      actualValue: matched.actualValue || '',
      checkResult: matched.checkResult || 'OK',
      standardValue: matched.standardValue || item.standardValue,
    };
  });
  pressSlotMiddleCheckItems.value = pressSlotMiddleCheckItems.value.map((item) => {
    const matched = savedItems.find(
      (saved) => saved.itemCategory === item.itemCategory && saved.itemName === item.itemName,
    );
    return matched ? { ...item, ...matched } : item;
  });
  pressSlotProcessCheckItems.value = pressSlotProcessCheckItems.value.map((item) => {
    const matched = savedItems.find(
      (saved) => saved.itemCategory === item.itemCategory && saved.itemName === item.itemName,
    );
    return matched ? { ...item, ...matched } : item;
  });
}

function applySegmentToReportForm(group: SourceGroup, segment: SourceSegment, useAvailableRange = true) {
  const availableRange = useAvailableRange ? findFirstAvailableRange(segment) : {
    end: Number(segment.outputLength || 0),
    length: Number(segment.outputLength || 0),
    start: 0,
  };
  reportForm.sourceCode = segment.batchNo;
  reportForm.sourceGrindingSecondDetailId = segment.grindingSecondDetailId;
  reportForm.sourceProductionBatchNo = segment.batchNo;
  reportForm.parentBatchNo = group.baseBatchNo;
  reportForm.startPosition = roundMeter(availableRange.start);
  reportForm.endPosition = roundMeter(availableRange.end);
  reportForm.processLength = roundMeter(availableRange.length);
  reportForm.outputLength = reportOutputLength.value;
  reportForm.glueBoardStartPosition = Number(glueBoard.availableStartPosition || 0);
  reportForm.glueBoardUseLength = roundMeter(availableRange.length);
  reportForm.productionBatchNo = segment.batchNo;
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
    glueBoardMaterialCode: record.glueBoardMaterialCode || glueBoard.materialCode,
    glueBoardStartPosition: Number(record.glueBoardStartPosition || glueBoard.availableStartPosition || 0),
    glueBoardUsageId: record.glueBoardUsageId,
    glueBoardUseLength: Number(record.glueBoardUseLength || record.inputLength || 0),
    lossLength: Number(record.lossLength || 0),
    napSampleLength: Number(record.napSampleLength || 0),
    outputLength: Number(record.outputLength || 0),
    parentBatchNo: record.parentProductionBatchNo || record.sourceBatchNo || reportForm.parentBatchNo,
    processLength: Number(record.inputLength || record.outputLength || 0),
    productionBatchNo: record.productionBatchNo || reportForm.productionBatchNo,
    preProcessSelfCheckAbnormal: isPreProcessSelfCheckAbnormal(extra),
    remark: record.remark || '',
    reportDate: record.reportDate || reportForm.reportDate,
    coaFlag: extra.coaFlag === true || extra.coaFlag === 'true' || extra.coaFlag === 'Y' || extra.coaFlag === 1,
    reportType: extra.reportType || reportForm.reportType || 'PRODUCT',
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

function getPressSlotGridColumns(_count: number) {
  return 'repeat(auto-fill, 168px)';
}

function hasPressSlotCurrentProcessNg(segment: SourceSegment) {
  return isPressSlotDownstreamFeedbackNg(segment) || (
    getPressSlotSegmentVisualIssue(segment) &&
      !isPressSlotPreProcessAttributedNg(segment)
  ) || getPressSlotSegmentInspectionNg(segment);
}

function isPressSlotSegmentConfirmed(segment: SourceSegment) {
  const existedRecord = findReportBySegment(segment);
  if (existedRecord && isAdhesiveRecordConfirmed(existedRecord.reportStatus)) return true;
  return isFirstInspectionSegment(segment);
}

function isPressSlotFirstInspectionSegment(segment: SourceSegment) {
  return parseJsonObject(segment.extraJson || '').faiFirstInspectionSampleOccupied === true
    || isFirstInspectionSegment(segment)
    || getPressSlotSegmentReportType(segment) === 'CHANGEOVER';
}

function isPressSlotSegmentProcessable(segment: SourceSegment) {
  return !isPressSlotSegmentConfirmed(segment)
    && !isPressSlotFirstInspectionSegment(segment)
    && !hasPressSlotCurrentProcessNg(segment)
    && !isPressSlotSlittingNgSegment(segment)
    && !getPressSlotSegmentCoaFlag(segment);
}

function getPressSlotSegmentStatus(segment: SourceSegment): PressSlotWorkbenchStatus {
  if (hasPressSlotCurrentProcessNg(segment)) return 'CURRENT_NG';
  if (isPressSlotSlittingNgSegment(segment)) return 'OTHER_NG';
  if (isPressSlotSegmentConfirmed(segment)) return 'COMPLETED';
  return 'PENDING';
}

function matchPressSlotStatusFilter(segment: SourceSegment) {
  const filterStatus = visualFilterForm.status;
  if (filterStatus === 'ALL') return true;
  if (filterStatus === 'CURRENT_NG') return hasPressSlotCurrentProcessNg(segment);
  if (filterStatus === 'OTHER_NG') return isPressSlotSlittingNgSegment(segment);
  return getPressSlotSegmentStatus(segment) === filterStatus;
}

function matchPressSlotSegment(segment: SourceSegment) {
  const keyword = visualFilterForm.batchNo.trim().toLowerCase();
  if (keyword) {
    const matched = [segment.batchNo, segment.segmentMark, segment.label]
      .filter(Boolean)
      .some((value) => String(value).toLowerCase().includes(keyword));
    if (!matched) return false;
  }
  if (!matchPressSlotStatusFilter(segment)) return false;
  if (visualFilterForm.reportType !== 'ALL' && getPressSlotSegmentReportType(segment) !== visualFilterForm.reportType) return false;
  return true;
}

function setPressSlotFilterStatus(status: string) {
  visualFilterForm.status = status;
}

function setPressSlotFilterReportType(reportType: string) {
  visualFilterForm.reportType = reportType;
}

function isPressSlotReportType(value?: string): value is PressSlotReportType {
  return pressSlotReportTypeOptions.some((item) => item.value === value);
}

function isPressSlotMiddleReportType(value?: string): value is 'END' | 'FRONT' | 'MIDDLE' {
  return ['END', 'FRONT', 'MIDDLE'].includes(String(value || ''));
}

function isPressSlotInspectionReportType(value?: string) {
  return ['CHANGEOVER', 'PROCESS_CHECK'].includes(String(value || ''));
}

function getTemperatureCheckItems() {
  return checkTemplate.value
    .filter((item) => item.itemName.includes('温度'))
    .map((item, index) => {
      const matchedIndex = item.itemName.match(/(?:温度点|温度)(\d+)/)?.[1];
      const temperatureIndex = matchedIndex || String(index + 1);
      return {
        ...item,
        itemCategory: '工艺参数',
        itemName: `实测点温度${temperatureIndex}(℃)`,
      };
    });
}

function getPressSlotProcessCheckItems() {
  return [
    ...getTemperatureCheckItems(),
    ...pressSlotProcessCheckItems.value,
  ];
}

function ensurePressSlotChangeoverItems(items: AdhesiveCheckItem[]) {
  const rows = items.map((item) => ({ ...item, itemCategory: item.itemCategory || '其他' }));
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
  if (!rows.some((item) => item.itemName.includes('压槽辊编号'))) {
    rows.push({
      abnormalRemark: '',
      actualValue: '',
      checkResult: 'OK',
      itemCategory: '压槽机',
      itemName: '压槽辊编号',
      sortNo: 19,
      standardValue: '①YCG-3*3-003 / ②YCG-3*3-006',
    });
  }
  return rows.sort((a, b) => Number(a.sortNo || 0) - Number(b.sortNo || 0));
}

function getPressSlotSegmentReportType(segment: SourceSegment): PressSlotReportType {
  const existedRecord = findReportBySegment(segment);
  if (!existedRecord) return 'PRODUCT';
  const extra = parseRecordExtra(existedRecord) as Record<string, any>;
  return isPressSlotReportType(extra.reportType) ? extra.reportType : 'PRODUCT';
}

function getPressSlotSegmentCoaFlag(segment: SourceSegment) {
  const existedRecord = findReportBySegment(segment);
  if (!existedRecord) return false;
  const extra = parseRecordExtra(existedRecord) as Record<string, any>;
  return extra.coaFlag === true || extra.coaFlag === 'true' || extra.coaFlag === 'Y' || extra.coaFlag === 1;
}

function getPressSlotSegmentInspectionNg(segment: SourceSegment) {
  const existedRecord = findReportBySegment(segment);
  if (!existedRecord) return false;
  const extra = parseRecordExtra(existedRecord) as Record<string, any>;
  return ['NG', 'ABNORMAL'].includes(String(extra.feedbackResult || extra.inspectionResult || '').toUpperCase());
}

function hasPressSlotVisualIssue(record?: MesHcAdhesiveConsoleApi.ReportItem | Record<string, any> | null) {
  if (!record) return false;
  if (normalizeVisualResult((record as Record<string, any>).selfCheck) === 'NG') return true;
  const extra = parseRecordExtra(record) as Record<string, any>;
  if (normalizeVisualResult(extra.visualInspectionResult || extra.selfCheck) === 'NG') return true;
  return parseVisualItemsFromRecord(record).some((item) => isVisualItemActive(item));
}

function getActiveVisualCategoryItems(record?: MesHcAdhesiveConsoleApi.ReportItem | Record<string, any> | null) {
  const source = (record || activeRecord.value || {}) as Record<string, any>;
  const extra = parseRecordExtra(source) as Record<string, any>;
  const categories = parseVisualItemsFromRecord(source)
    .filter((item) => isVisualItemActive(item))
    .map((item) => item.itemName)
    .filter((itemName) => pressSlotVisualItemNames.includes(itemName));
  const defectCode = String(source.defectCode || extra.defectCode || '').trim();
  if (pressSlotVisualItemNames.includes(defectCode) && !categories.includes(defectCode)) {
    categories.push(defectCode);
  }
  return Array.from(new Set(categories));
}

function getActiveVisualCategoryText(record?: MesHcAdhesiveConsoleApi.ReportItem | Record<string, any> | null) {
  return getActiveVisualCategoryItems(record).join('、') || '-';
}

function getPressSlotSegmentVisualIssue(segment: SourceSegment) {
  return hasPressSlotVisualIssue(findReportBySegment(segment));
}

function isPressSlotDownstreamFeedbackNg(segment: SourceSegment) {
  return hasDownstreamPreProcessFeedback(findReportBySegment(segment));
}

function isPressSlotPreProcessAttributedNg(segment: SourceSegment) {
  const record = findReportBySegment(segment);
  if (!record || !hasPressSlotVisualIssue(record)) return false;
  return isPreProcessSelfCheckAbnormal(parseRecordExtra(record));
}

function isPressSlotSlittingNgSegment(segment: SourceSegment) {
  return (
    String(segment.selfCheck || '').toUpperCase() === 'NG' ||
    isPressSlotPreProcessAttributedNg(segment)
  );
}

function normalizePressSlotCardBatchNo(value?: unknown) {
  return String(value ?? '').trim().toUpperCase();
}

function normalizePressSlotOperationName(value?: unknown) {
  const text = String(value ?? '').trim();
  if (!text) return '';
  const nameMap: Record<string, string> = {
    ADHESIVE1: '粘胶1',
    ADHESIVE2: '粘胶2',
    CUT_ROUND: '裁切',
    GRINDING_FIRST: '一次磨皮',
    GRINDING_SECOND: '二次磨皮',
    PRESS_SLOT: '压槽',
    ROUGH_GRINDING: '磨皮',
    SLITTING: '分切',
  };
  return nameMap[text.toUpperCase()] || text;
}

function getPressSlotOtherNgOperationText(segment: SourceSegment) {
  const record = findReportBySegment(segment);
  if (record && isPressSlotPreProcessAttributedNg(segment)) {
    const extra = parseRecordExtra(record) as Record<string, any>;
    return normalizePressSlotOperationName(
      extra.sourceNgProcessName || extra.ngAttributionProcessName,
    ) || '分切';
  }
  return normalizePressSlotOperationName(segment.sourceOperationName)
    || normalizePressSlotOperationName(segment.sourceProcessStage)
    || '分切';
}

function hasPressSlotCurrentReportRecord(segment: SourceSegment) {
  return !!findReportBySegment(segment);
}

function shouldShowPressSlotSelfCheckLine(segment: SourceSegment) {
  if (getPressSlotSegmentVisualIssue(segment)
    || isPressSlotSlittingNgSegment(segment)
    || isPressSlotDownstreamFeedbackNg(segment)) return true;
  if (getPressSlotSegmentInspectionEntries(segment).length > 0) return false;
  return hasPressSlotCurrentReportRecord(segment) || isPressSlotSlittingNgSegment(segment);
}

function formatPressSlotSelfCheckLabel(processName?: string) {
  const normalized = normalizePressSlotOperationName(processName).replace(/工序$/, '');
  return normalized ? `${normalized}工序自检` : '自检';
}

function getPressSlotSelfCheckLabel(segment: SourceSegment) {
  if (isPressSlotDownstreamFeedbackNg(segment)) return formatPressSlotSelfCheckLabel('压槽');
  if (isPressSlotSlittingNgSegment(segment)) return formatPressSlotSelfCheckLabel(getPressSlotOtherNgOperationText(segment));
  if (getPressSlotSegmentVisualIssue(segment)) return formatPressSlotSelfCheckLabel('压槽');
  return '自检';
}

function getPressSlotNgStatusText(segment: SourceSegment) {
  if (isPressSlotDownstreamFeedbackNg(segment)) return 'NG';
  if (getPressSlotSegmentVisualIssue(segment)) return 'NG';
  if (isPressSlotSlittingNgSegment(segment)) {
    return 'NG';
  }
  return hasPressSlotCurrentReportRecord(segment) ? '正常' : '';
}

function getPressSlotNgLineClass(segment: SourceSegment) {
  if (getPressSlotSegmentVisualIssue(segment)
    || isPressSlotSlittingNgSegment(segment)
    || isPressSlotDownstreamFeedbackNg(segment)) return 'is-ng';
  return 'is-ok';
}

function getPressSlotSelfCheckTitle(segment: SourceSegment) {
  const record = findReportBySegment(segment);
  if (!record) return getPressSlotNgStatusText(segment);
  const extra = parseRecordExtra(record) as Record<string, any>;
  return getDownstreamPreProcessFeedbackTitle(record)
    || String(extra.sourceNgReason || record.remark || getPressSlotNgStatusText(segment));
}

function getPressSlotScanConfirmText(segment: SourceSegment) {
  return isPressSlotSegmentConfirmed(segment) ? '已确认' : '待确认';
}

function getPressSlotScanConfirmLineClass(segment: SourceSegment) {
  return isPressSlotSegmentConfirmed(segment) ? 'is-ok' : 'is-scan-pending';
}

function shouldShowPressSlotScanConfirmLine(segment: SourceSegment) {
  const status = getPressSlotSegmentStatus(segment);
  return status === 'PENDING' || status === 'CURRENT_NG';
}

function getPressSlotRecordInspectionLabel(
  inspectionType?: FirstInspectionScanMode | string,
  record?: Partial<MesHcAdhesiveConsoleApi.FaiSummary> | MesHcAdhesiveConsoleApi.ReportItem | Record<string, any>,
  reportType?: PressSlotReportType,
) {
  const rawRecord = record as Record<string, any> | undefined;
  const extra = parseRecordExtra(rawRecord || {});
  const sceneText = [
    inspectionType,
    reportType,
    rawRecord?.inspectionScene,
    extra.inspectionScene,
    extra.inspectionSampleCategory,
    extra.inspectionSampleCategoryName,
  ].filter(Boolean).join('|').toUpperCase();
  if (sceneText.includes('PROCESS_CHECK') || sceneText.includes('ABNORMAL_RELEASE') || sceneText.includes('加检') || sceneText.includes('异常放行')) return '加检';
  if (sceneText.includes('FIRST_INSPECTION') || sceneText.includes('CHANGEOVER') || sceneText.includes('首检')) return '首检';
  return '';
}

function isPressSlotInspectionSubmitStatus(status?: string) {
  return ['INSPECTING', 'PENDING', 'SUBMITTED', 'WAIT_QA', 'WAITING_QA'].includes(String(status || '').toUpperCase());
}

function hasPressSlotInspectionSubmission(
  record?: Partial<MesHcAdhesiveConsoleApi.FaiSummary> | MesHcAdhesiveConsoleApi.ReportItem | Record<string, any>,
  extra: Record<string, any> = {},
) {
  const rawRecord = record as Record<string, any> | undefined;
  if (!rawRecord) return false;
  if (rawRecord.faiId || rawRecord.faiNo || rawRecord.sourceReportId || rawRecord.sourceReportNo) return true;
  if (rawRecord.faiApplyTime || rawRecord.inspectionDate || rawRecord.submitTime || extra.submitTime) return true;
  if (isPressSlotInspectionSubmitStatus(rawRecord.faiStatus || rawRecord.inspectionStatus || extra.inspectionStatus)) return true;
  return extra.coaFlag === true || extra.coaFlag === 'true' || extra.coaFlag === 'Y' || extra.coaFlag === 1;
}

function getPressSlotInspectionFeedbackText(rawRecord: Record<string, any>, extra: Record<string, any>) {
  const directResult = String(
    rawRecord.faiJudgment
    || rawRecord.inspectionResult
    || rawRecord.feedbackResult
    || extra.feedbackResult
    || extra.inspectionResult
    || '',
  ).trim();
  const normalized = directResult.toUpperCase();
  if (!normalized || ['PENDING', 'WAIT_QA', 'WAITING_QA'].includes(normalized)) return '';
  if (normalized === 'OK' || normalized === 'QUALIFIED') return '正常';
  if (normalized === 'NG' || normalized === 'ABNORMAL' || normalized === 'REJECTED') return 'NG';
  return normalized;
}

function getPressSlotRecordInspectionResultText(record?: Partial<MesHcAdhesiveConsoleApi.FaiSummary> | MesHcAdhesiveConsoleApi.ReportItem | Record<string, any>) {
  const rawRecord = record as Record<string, any> | undefined;
  if (!rawRecord) return '';
  const extra = parseRecordExtra(rawRecord);
  const feedbackText = getPressSlotInspectionFeedbackText(rawRecord, extra);
  if (feedbackText) return feedbackText;

  const status = String(rawRecord.faiStatus || rawRecord.inspectionStatus || extra.inspectionStatus || '').toUpperCase();
  if (['REJECTED', 'ABNORMAL'].includes(status)) return 'NG';
  if (['COMPLETED', 'QUALIFIED'].includes(status)) return '正常';
  if (isPressSlotInspectionSubmitStatus(status)) return '待检';

  const displayText = normalizePressSlotInspectionResultText(rawRecord.displayText);
  if (displayText) return displayText;
  const resolvedText = normalizePressSlotInspectionResultText(
    rawRecord.faiStatus || rawRecord.faiJudgment
      ? resolveFirstInspectionDisplayText(rawRecord.faiStatus, rawRecord.faiJudgment)
      : '',
  );
  if (resolvedText) return resolvedText;
  const inspectionStatusText = normalizePressSlotInspectionResultText(
    rawRecord.inspectionStatus ? getInspectionStatusText(rawRecord.inspectionStatus) : '',
  );
  if (inspectionStatusText) return inspectionStatusText;
  return hasPressSlotInspectionSubmission(rawRecord, extra) ? '待检' : '';
}

function normalizePressSlotInspectionResultText(value?: string) {
  const text = String(value || '').trim();
  const normalized = text.toUpperCase();
  if (!text || text === '-' || text.includes('未提交') || text.includes('草稿') || text.includes('取消')) return '';
  if (normalized === 'OK' || normalized === 'QUALIFIED' || text.includes('完成') || text.includes('合格') || text.includes('正常')) {
    return '正常';
  }
  if (normalized === 'NG' || normalized === 'ABNORMAL' || normalized === 'REJECTED' || text.includes('驳回') || text.includes('异常')) {
    return 'NG';
  }
  if (['PENDING', 'WAIT_QA', 'WAITING_QA', 'INSPECTING'].includes(normalized)
    || text.includes('待') || text.includes('检测') || text.includes('复核')) {
    return '待检';
  }
  return text;
}

function normalizePressSlotInspectionDisplayText(value?: string, fallback?: string) {
  const text = String(value || '').trim();
  if (!fallback) return '';
  if (text.includes('正常放行')) return '正常放行';
  if (text.includes('放行') && fallback === '正常') return '正常放行';
  return fallback;
}

function isPressSlotInspectionReleaseOk(resultText?: string) {
  const text = String(resultText || '').trim();
  return normalizePressSlotInspectionResultText(text) === '正常' && text.includes('放行');
}

function getPressSlotInspectionTone(resultText?: string, label?: string) {
  const text = normalizePressSlotInspectionResultText(resultText);
  if (text === '正常' && label === '首检') return 'is-first-ok';
  if (text === '正常' && label === '加检' && isPressSlotInspectionReleaseOk(resultText)) return 'is-release-ok';
  if (text === 'NG') return 'is-ng';
  if (text === '正常') return 'is-ok';
  if (text === '待检') return 'is-pending';
  return '';
}

function getPressSlotInspectionRecordSortMeta(record?: Partial<MesHcAdhesiveConsoleApi.FaiSummary> | MesHcAdhesiveConsoleApi.ReportItem | Record<string, any>) {
  const rawRecord = record as Record<string, any> | undefined;
  const timeText = normalizeDateTimeText(
    rawRecord?.faiReturnTime
    || rawRecord?.faiApplyTime
    || rawRecord?.submitTime
    || rawRecord?.recordTime
    || rawRecord?.createTime
    || rawRecord?.updateTime
    || '',
  );
  return {
    id: Number(rawRecord?.faiId || rawRecord?.sourceReportId || rawRecord?.id || 0),
    time: timeText ? dayjs(timeText).valueOf() : 0,
  };
}

function isNewerPressSlotInspectionRecord(
  next?: Partial<MesHcAdhesiveConsoleApi.FaiSummary> | MesHcAdhesiveConsoleApi.ReportItem | Record<string, any>,
  current?: Partial<MesHcAdhesiveConsoleApi.FaiSummary> | MesHcAdhesiveConsoleApi.ReportItem | Record<string, any>,
) {
  const nextMeta = getPressSlotInspectionRecordSortMeta(next);
  const currentMeta = getPressSlotInspectionRecordSortMeta(current);
  if (nextMeta.time !== currentMeta.time) return nextMeta.time > currentMeta.time;
  return nextMeta.id >= currentMeta.id;
}

type PressSlotInspectionCardEntry = { label: string; resultText: string; tone: string };

function getPressSlotInspectionLabelPriority(label: string) {
  if (label === 'COA') return 1;
  if (label === '首检') return 2;
  if (label === '加检') return 3;
  return 99;
}

function getPressSlotAbnormalReleaseInspectionRecord(segment: SourceSegment) {
  const batchNo = normalizePressSlotCardBatchNo(segment.batchNo);
  if (!batchNo) return null;
  const lockRecord = getPressSlotAbnormalLockRecord(segment);
  if (!lockRecord) return null;
  if (normalizePressSlotCardBatchNo(lockRecord.releaseSampleBatchNo) !== batchNo) return null;
  if (!lockRecord.releaseFaiId && !lockRecord.releaseFaiNo && !lockRecord.releaseSubmitTime) return null;
  const normalizedResult = normalizePressSlotInspectionResultText(lockRecord.releaseResult);
  return {
    displayText: normalizedResult === '正常' ? '正常放行' : normalizedResult || '待检',
    faiApplyTime: lockRecord.releaseSubmitTime,
    faiId: lockRecord.releaseFaiId,
    faiNo: lockRecord.releaseFaiNo,
    faiReturnTime: lockRecord.releaseFeedbackTime || lockRecord.releaseTime,
    inspectionScene: 'ABNORMAL_RELEASE',
    productBatchNo: lockRecord.releaseSampleBatchNo,
  } as Record<string, any>;
}

function getPressSlotAbnormalProcessCheckInspectionRecord(segment: SourceSegment) {
  const batchNo = normalizePressSlotCardBatchNo(segment.batchNo);
  if (!batchNo) return null;
  const lockRecord = [...abnormalLockRows.value]
    .filter((record) => normalizePressSlotCardBatchNo(record.abnormalSampleBatchNo) === batchNo)
    .sort((a, b) => {
      const aTime = dayjs(normalizeDateTimeText(a.abnormalFeedbackTime || a.abnormalSubmitTime || '') || 0).valueOf();
      const bTime = dayjs(normalizeDateTimeText(b.abnormalFeedbackTime || b.abnormalSubmitTime || '') || 0).valueOf();
      if (aTime !== bTime) return bTime - aTime;
      return Number(b.abnormalFaiId || 0) - Number(a.abnormalFaiId || 0);
    })[0];
  if (!lockRecord) return null;
  if (!lockRecord.abnormalFaiId && !lockRecord.abnormalFaiNo && !lockRecord.abnormalSubmitTime) return null;
  const normalizedResult = normalizePressSlotInspectionResultText(lockRecord.abnormalResult);
  return {
    displayText: normalizedResult || '待检',
    faiApplyTime: lockRecord.abnormalSubmitTime,
    faiId: lockRecord.abnormalFaiId,
    faiNo: lockRecord.abnormalFaiNo,
    faiReturnTime: lockRecord.abnormalFeedbackTime,
    inspectionScene: 'PROCESS_CHECK',
    productBatchNo: lockRecord.abnormalSampleBatchNo,
  } as Record<string, any>;
}

function getPressSlotSegmentInspectionCandidates(segment: SourceSegment) {
  const batchNo = normalizePressSlotCardBatchNo(segment.batchNo);
  const entries: PressSlotInspectionCardEntry[] = [];
  const seen = new Set<string>();
  const pushEntry = (label: string, resultText = '') => {
    if (!label) return;
    const normalizedResult = normalizePressSlotInspectionResultText(resultText);
    if (!normalizedResult) return;
    const displayResult = normalizePressSlotInspectionDisplayText(resultText, normalizedResult);
    const key = `${label}|${displayResult}`;
    if (seen.has(key)) return;
    seen.add(key);
    entries.push({ label, resultText: displayResult, tone: getPressSlotInspectionTone(displayResult, label) });
  };
  const latestInspectionByLabel = new Map<string, {
    record: MesHcAdhesiveConsoleApi.FaiSummary | MesHcAdhesiveConsoleApi.ReportItem;
    resultText: string;
  }>();
  const faiRecords = [
    ...(firstInspection.value?.faiId ? [{ inspectionType: 'FIRST_INSPECTION' as FirstInspectionScanMode, record: firstInspection.value }] : []),
    ...firstInspectionFaiRows.value.map((record) => ({ inspectionType: 'FIRST_INSPECTION' as FirstInspectionScanMode, record })),
    ...processCheckFaiRows.value.map((record) => ({ inspectionType: 'PROCESS_CHECK' as FirstInspectionScanMode, record })),
  ];
  faiRecords.forEach(({ inspectionType, record }) => {
    if (normalizePressSlotCardBatchNo(getFirstInspectionProcessSliceNo(record)) !== batchNo) return;
    const label = getPressSlotRecordInspectionLabel(inspectionType, record);
    if (!label) return;
    const current = latestInspectionByLabel.get(label);
    if (!current || isNewerPressSlotInspectionRecord(record, current.record)) {
      latestInspectionByLabel.set(label, { record, resultText: getFirstInspectionFaiResultText(record) });
    }
  });

  const existedRecord = findReportBySegment(segment);
  if (existedRecord) {
    const reportType = getPressSlotSegmentReportType(segment);
    const label = getPressSlotRecordInspectionLabel(undefined, existedRecord, reportType);
    const current = label ? latestInspectionByLabel.get(label) : undefined;
    if (label && (!current || isNewerPressSlotInspectionRecord(existedRecord, current.record))) {
      latestInspectionByLabel.set(label, { record: existedRecord, resultText: getPressSlotRecordInspectionResultText(existedRecord) });
    }
  }
  const abnormalProcessCheckRecord = getPressSlotAbnormalProcessCheckInspectionRecord(segment);
  if (abnormalProcessCheckRecord) {
    const label = getPressSlotRecordInspectionLabel('PROCESS_CHECK', abnormalProcessCheckRecord);
    const current = label ? latestInspectionByLabel.get(label) : undefined;
    if (label && (!current || isNewerPressSlotInspectionRecord(abnormalProcessCheckRecord, current.record))) {
      latestInspectionByLabel.set(label, { record: abnormalProcessCheckRecord as MesHcAdhesiveConsoleApi.FaiSummary, resultText: String(abnormalProcessCheckRecord.displayText || '') });
    }
  }
  const abnormalReleaseRecord = getPressSlotAbnormalReleaseInspectionRecord(segment);
  if (abnormalReleaseRecord) {
    const label = getPressSlotRecordInspectionLabel('ABNORMAL_RELEASE', abnormalReleaseRecord);
    const current = label ? latestInspectionByLabel.get(label) : undefined;
    if (label && (!current || isNewerPressSlotInspectionRecord(abnormalReleaseRecord, current.record))) {
      latestInspectionByLabel.set(label, { record: abnormalReleaseRecord as MesHcAdhesiveConsoleApi.FaiSummary, resultText: String(abnormalReleaseRecord.displayText || '') });
    }
  }
  ['首检', '加检'].forEach((label) => {
    const latest = latestInspectionByLabel.get(label);
    if (latest) pushEntry(label, latest.resultText);
  });
  if (getPressSlotSegmentCoaFlag(segment) && existedRecord) {
    pushEntry('COA', getPressSlotRecordInspectionResultText(existedRecord));
  }
  return entries.sort((a, b) => getPressSlotInspectionLabelPriority(a.label) - getPressSlotInspectionLabelPriority(b.label));
}

function getPressSlotSegmentInspectionEntries(segment: SourceSegment) {
  return getPressSlotSegmentInspectionCandidates(segment).slice(0, 1);
}

function getPressSlotSegmentInspectionBackgroundClass(segment: SourceSegment) {
  const primary = getPressSlotSegmentInspectionEntries(segment)[0];
  if (!primary) return '';
  const normalizedResult = normalizePressSlotInspectionResultText(primary.resultText);
  if (normalizedResult === '正常' && primary.label === '首检') return 'is-inspection-bg-first-ok';
  if (normalizedResult === '正常' && primary.label === '加检' && isPressSlotInspectionReleaseOk(primary.resultText)) return 'is-inspection-bg-release-ok';
  if (normalizedResult === 'NG') return 'is-inspection-bg-ng';
  if (normalizedResult === '待检') return 'is-inspection-bg-pending';
  if (normalizedResult === '正常') return 'is-inspection-bg-ok';
  return '';
}

function getPressSlotSegmentPositionText(segment: SourceSegment) {
  const reportType = getPressSlotSegmentReportType(segment);
  if (reportType === 'FRONT') return '前段';
  if (reportType === 'MIDDLE') return '中段';
  if (reportType === 'END') return '后段';
  const extra = parseJsonObject(segment.extraJson || '');
  const extraText = String(extra.samplePositionName || extra.segmentPositionName || extra.reportTypeName || '').trim();
  if (extraText.includes('前')) return '前段';
  if (extraText.includes('中')) return '中段';
  if (extraText.includes('后')) return '后段';
  return '';
}

function getFirstInspectionSegmentMeta(segment: SourceSegment) {
  const segmentBatchNo = String(segment.batchNo || '').trim();
  const currentSliceNo = String(currentFirstInspectionSliceNo.value || '').trim();
  if (!segmentBatchNo || !currentSliceNo || segmentBatchNo !== currentSliceNo) {
    return null;
  }
  const summarySliceNo = String(firstInspection.value?.productBatchNo || '').trim();
  if (!summarySliceNo || summarySliceNo !== segmentBatchNo) {
    return { badgeClass: '', sliceClass: '', text: '首检' };
  }
  const status = String(firstInspection.value?.faiStatus || '').toUpperCase();
  const judgment = String(firstInspection.value?.faiJudgment || '').toUpperCase();
  if (judgment === 'NG' || status === 'REJECTED') {
    return { badgeClass: 'is-fai-ng', sliceClass: 'is-fai-ng', text: '首检NG' };
  }
  if (status === 'COMPLETED' && judgment === 'OK') {
    return { badgeClass: 'is-fai-ok', sliceClass: 'is-fai-ok', text: '首检OK' };
  }
  if (['INSPECTING', 'PENDING', 'WAITING_QA'].includes(status)) {
    return { badgeClass: 'is-fai-pending', sliceClass: 'is-fai-pending', text: '首检待检' };
  }
  return { badgeClass: '', sliceClass: '', text: '首检' };
}

function isFirstInspectionSegment(segment: SourceSegment) {
  return !!getFirstInspectionSegmentMeta(segment);
}

function getFirstInspectionSegmentLabel(segment: SourceSegment) {
  return getFirstInspectionSegmentMeta(segment)?.text || '';
}

function getFirstInspectionSegmentBadgeClass(segment: SourceSegment) {
  return getFirstInspectionSegmentMeta(segment)?.badgeClass || '';
}

function getPressSlotReportTypeClass(type?: string) {
  const classMap: Record<PressSlotReportType, string> = {
    CHANGEOVER: 'is-type-changeover',
    END: 'is-type-end',
    FRONT: 'is-type-front',
    MIDDLE: 'is-type-middle',
    PROCESS_CHECK: 'is-type-process',
    PRODUCT: 'is-type-product',
  };
  return isPressSlotReportType(type) ? classMap[type] : classMap.PRODUCT;
}

function getPressSlotSliceClass(segment: SourceSegment) {
  const status = getPressSlotSegmentStatus(segment);
  const typeClass = getPressSlotReportTypeClass(getPressSlotSegmentReportType(segment));
  const firstInspectionMeta = getFirstInspectionSegmentMeta(segment);
  const flags = [
    getPressSlotSegmentCoaFlag(segment) ? 'is-coa' : '',
    isPressSlotSlittingNgSegment(segment) ? 'is-slitting-ng' : '',
    getPressSlotSegmentVisualIssue(segment) ? 'is-visual-ng' : '',
    getPressSlotSegmentInspectionNg(segment) ? 'is-inspection-ng' : '',
    isPressSlotAbnormalLockedSegment(segment) ? 'is-abnormal-locked' : '',
    firstInspectionMeta ? 'is-first-inspection' : '',
    firstInspectionMeta?.sliceClass || '',
    getPressSlotSegmentInspectionBackgroundClass(segment),
    transferPrintSelectionMode.value ? 'is-transfer-selectable' : '',
    transferPrintSelectionMode.value && !getPrintableTransferReportBySegment(segment) ? 'is-transfer-disabled' : '',
    transferPrintSelectionMode.value && isTransferSegmentSelected(segment) ? 'is-transfer-selected' : '',
    selectedOneClickPressSlotBatchNo.value ? 'is-one-click-selectable' : '',
    selectedOneClickPressSlotBatchNo.value &&
    !isPressSlotSegmentProcessable(segment)
      ? 'is-one-click-disabled'
      : '',
    selectedOneClickPressSlotBatchNo.value &&
    isPressSlotOneClickSegmentSelected(segment)
      ? 'is-one-click-selected'
      : '',
  ].filter(Boolean);
  if (status === 'COMPLETED') return ['slice-cell', 'is-confirmed', typeClass, ...flags];
  if (status === 'CURRENT_NG') return ['slice-cell', 'is-current-ng', typeClass, ...flags];
  if (status === 'OTHER_NG') return ['slice-cell', 'is-other-ng', typeClass, ...flags];
  return ['slice-cell', 'is-scan-pending', typeClass, ...flags];
}

function getPressSlotAbnormalLockRecord(segment: SourceSegment) {
  return abnormalLockRecordMap.value.get(String(segment.batchNo || '').trim());
}

function isPressSlotAbnormalLockedSegment(segment: SourceSegment) {
  return abnormalLockedSliceNoSet.value.has(String(segment.batchNo || '').trim());
}

function shouldShowPressSlotAbnormalLockLine(segment: SourceSegment) {
  return isPressSlotAbnormalLockedSegment(segment);
}

function getPressSlotAbnormalLockText(segment: SourceSegment) {
  const record = getPressSlotAbnormalLockRecord(segment);
  if (!record) return '';
  if (isActivePressSlotAbnormalLock(record)) {
    return record.releaseResult
      ? `锁定中 / 放行${record.releaseResult}`
      : '锁定中';
  }
  return record.releaseResult ? `已放行 / 放行${record.releaseResult}` : '已放行';
}

function getPressSlotAbnormalLockLineClass(segment: SourceSegment) {
  return isPressSlotAbnormalLockedSegment(segment) ? 'is-abnormal-lock' : 'is-ok';
}

function getPressSlotSliceStatusText(segment: SourceSegment) {
  const status = getPressSlotSegmentStatus(segment);
  if (status === 'CURRENT_NG') return '本工序NG';
  if (status === 'OTHER_NG') return '分切工序NG';
  if (status === 'COMPLETED') return '已确认';
  return '待确认';
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

function displayTimeText(value?: null | number | string) {
  const normalized = normalizeDateTimeText(value);
  return normalized ? dayjs(normalized).format('HH:mm') : '';
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

function getTaskListActionText(status?: string) {
  return normalizeWorkOrderStatus(status) === 'FINISHED' ? '查看' : '开工';
}

function getEquipmentWorkStatusMeta(status?: null | number | string) {
  const raw = String(status ?? '').trim();
  const normalized = raw.toUpperCase();
  if (!raw) return { color: 'default', text: '未记录' };
  if (['PRODUCING', 'RUNNING', 'IN_PROGRESS', 'WORKING'].includes(normalized) || raw.includes('生产')) {
    return { color: 'processing', text: '生产中' };
  }
  if (['MAINTENANCE', 'REPAIR'].includes(normalized) || raw.includes('检修') || raw.includes('维护')) {
    return { color: 'warning', text: '检修' };
  }
  if (['FAULT', 'ERROR', 'DOWN'].includes(normalized) || raw.includes('故障')) {
    return { color: 'error', text: '故障' };
  }
  if (['DISABLED', 'STOPPED'].includes(normalized) || raw.includes('停用')) {
    return { color: 'default', text: '停用' };
  }
  if (['IDLE', 'FREE', 'AVAILABLE'].includes(normalized) || raw.includes('待机') || raw.includes('空闲')) {
    return { color: 'success', text: '待机' };
  }
  return { color: 'default', text: raw };
}

function normalizeTaskListStatusValue(status?: string) {
  const normalized = normalizeWorkOrderStatus(status);
  if (normalized === 'FINISHED') return 'COMPLETED';
  if (normalized === 'RUNNING') return 'IN_PROGRESS';
  if (normalized === 'PENDING') return 'PENDING';
  return String(status || '').trim().toUpperCase();
}

function containsTaskFilterText(values: unknown[], keyword: string) {
  const text = String(keyword || '').trim().toLowerCase();
  if (!text) return true;
  return values.some((value) => String(value ?? '').toLowerCase().includes(text));
}

const currentOperationStatus = computed(() => normalizeWorkOrderStatus(currentPlan.status));
const isWorkOrderRunning = computed(() => currentOperationStatus.value === 'RUNNING');
const isWorkOrderFinished = computed(() => currentOperationStatus.value === 'FINISHED');
const isWorkOrderPaused = computed(() => currentOperationStatus.value === 'PAUSED');
const isWorkOrderCancelled = computed(() => currentOperationStatus.value === 'CANCELLED');
const isCurrentTaskReadonly = computed(() =>
  isWorkOrderFinished.value || isWorkOrderPaused.value || isWorkOrderCancelled.value,
);
const activeReportReadonlyReason = computed(() => {
  if (reportDialogMode.value !== 'view' || !activeRecord.value) return '';
  if (activeRecord.value.editBlockedReason) return activeRecord.value.editBlockedReason;
  if (String(activeRecord.value.reportStatus || '').toUpperCase() === 'SUBMITTED') {
    return '该压槽片已过站提交，只能查看，不能覆盖修改。';
  }
  if (isWorkOrderFinished.value) return '当前压槽工序已完工，只能查看已有报工。';
  if (isWorkOrderPaused.value) return '当前压槽工序已暂停，只能查看已有报工。';
  if (isWorkOrderCancelled.value) return '当前压槽工序已取消，只能查看已有报工。';
  return '';
});
const dailyPreparationReady = computed(() => dailyRecordRows.value.some((row) => isTodayDailyPreparationRecord(row)));

function ensureSegmentWritable(actionName = '操作') {
  if (isWorkOrderPaused.value) {
    message.warning(`当前压槽工序已暂停，请等待复工后再${actionName}`);
    return false;
  }
  if (isWorkOrderCancelled.value) {
    message.warning(`当前压槽工序已作废取消，不能${actionName}`);
    return false;
  }
  if (isWorkOrderFinished.value) {
    message.warning(`当前压槽分段已完工，不能再${actionName}`);
    return false;
  }
  return true;
}

function buildWorkOrderBlockedReason(status = currentPlan.status) {
  const normalized = normalizeWorkOrderStatus(status);
  const batchNo = currentPlan.sourceProductionBatchNo || currentPlan.batchNo || currentPlan.sourceBatchNo || '-';
  if (normalized === 'PAUSED') {
    return `当前压槽工序已暂停，计划 ${currentPlan.planNo || '-'}，批号 ${batchNo} 暂不能继续报工或提交操作，请等待生产计划复工指令。`;
  }
  if (normalized === 'CANCELLED') {
    return `当前压槽工序已作废取消，计划 ${currentPlan.planNo || '-'}，批号 ${batchNo} 不能继续报工或提交操作。`;
  }
  return '';
}

const workbenchBlockedReason = computed(() => buildWorkOrderBlockedReason());
const workbenchBlockedTitle = computed(() =>
  isWorkOrderCancelled.value ? '当前压槽工序已作废取消' : '当前压槽工序已暂停',
);

function isTodayDailyPreparationRecord(row: DailyRecordRow) {
  if (row.status === 'PENDING') return false;
  const recordDate = String(row.recordDate || '').trim();
  if (recordDate) return dayjs(recordDate).isSame(dayjs(currentDateText.value), 'day');
  const recorderTime = String(row.recorderTime || '').trim();
  return !!recorderTime && dayjs(recorderTime).isSame(dayjs(currentDateText.value), 'day');
}

function showDailyPreparationRequiredWarning() {
  AModal.warning({
    okText: '知道了',
    title: '需要今日点检/清洁记录',
    content: '当前压槽机台今天尚未记录点检/清洁，请双击左上角压槽图标显示点检/清洁记录后填写保存任一点检/清洁记录；暂不要求确认。',
  });
}

function stripSegmentMark(batchNo?: string) {
  const source = String(batchNo || '').trim();
  if (!source) return '';
  if (/[PQRS]\d{3}[A-Z]?$/i.test(source)) return source.replace(/\d{3}[A-Z]?$/i, '');
  if (/-[A-Z]\d+$/i.test(source)) return source.replace(/-[A-Z]\d+$/i, '');
  return /[PQRS]$/.test(source) ? source.slice(0, -1) : source;
}

function resolveMotherSegmentBatchNo(batchNo?: string) {
  const source = String(batchNo || '').trim();
  if (!source) return '';
  if (/[PQRS]\d{3}[A-Z]?$/i.test(source)) return source.replace(/\d{3}[A-Z]?$/i, '');
  if (/[PQRS]-J\d+$/i.test(source)) return source.replace(/-J\d+$/i, '');
  return source;
}

function resolveSelectedSourceBatchNo(row: Record<string, any>) {
  const sourceBatchNo = String(row.sourceBatchNo || '').trim();
  if (sourceBatchNo) return sourceBatchNo;
  const sourceProductionBatchNo = resolveMotherSegmentBatchNo(row.sourceProductionBatchNo);
  if (sourceProductionBatchNo) return sourceProductionBatchNo;
  const parentProductionBatchNo = resolveMotherSegmentBatchNo(row.parentProductionBatchNo);
  if (parentProductionBatchNo) return parentProductionBatchNo;
  const motherBatchNo = String(row.motherBatchNo || '').trim();
  if (motherBatchNo) return motherBatchNo;
  const fallback = row.productionBatchNo || row.batchNo || row.confirmedBatchNo || '';
  return resolveMotherSegmentBatchNo(fallback) || stripSegmentMark(fallback) || '';
}

function resolveSelectedSourceProductionBatchNo(row: Record<string, any>) {
  return String(
    row.sourceProductionBatchNo
    || row.productionBatchNo
    || row.confirmedBatchNo
    || row.batchNo
    || '',
  ).trim();
}

function resolveCurrentPlanSourceBatchNo() {
  return resolveMotherSegmentBatchNo(currentPlan.sourceBatchNo)
    || resolveMotherSegmentBatchNo(currentPlan.sourceProductionBatchNo)
    || resolveMotherSegmentBatchNo(currentPlan.batchNo)
    || '';
}

function resolvePlanScanSourceBatchNo(value?: string) {
  if (!hasPlanScanDelimiter(value)) return '';
  const scannedSource = normalizePlanScanSliceNo(value);
  return resolveMotherSegmentBatchNo(scannedSource) || scannedSource;
}

function isSameBatchNo(left?: string, right?: string) {
  return String(left || '').trim().toUpperCase() === String(right || '').trim().toUpperCase();
}

function matchTaskScanSliceNo(task: Record<string, any>, scanSliceNo?: string) {
  const scannedSliceNo = String(scanSliceNo || '').trim();
  if (!scannedSliceNo) return false;
  const scannedSourceBatchNo = resolveMotherSegmentBatchNo(scannedSliceNo) || scannedSliceNo;
  const sourceProductionBatchNo = resolveSelectedSourceProductionBatchNo(task);
  return [
    sourceProductionBatchNo,
    task.productionBatchNo,
    task.batchNo,
    task.confirmedBatchNo,
  ].some((candidate) => isSameBatchNo(candidate, scannedSliceNo))
    || matchTaskSourceBatchNo(task, scannedSourceBatchNo);
}

function matchTaskSourceBatchNo(task: Record<string, any>, sourceBatchNo?: string) {
  if (!sourceBatchNo) return false;
  const sourceProductionBatchNo = resolveSelectedSourceProductionBatchNo(task);
  return [
    resolveSelectedSourceBatchNo(task),
    sourceProductionBatchNo,
    resolveMotherSegmentBatchNo(sourceProductionBatchNo),
    resolveMotherSegmentBatchNo(task.productionBatchNo || task.batchNo || task.confirmedBatchNo || ''),
  ].some((candidate) => isSameBatchNo(candidate, sourceBatchNo));
}

function countTaskSourceContexts(tasks: Record<string, any>[]) {
  return new Set(
    tasks
      .map((task) => resolveSelectedSourceBatchNo(task) || resolveSelectedSourceProductionBatchNo(task))
      .filter(Boolean)
      .map((value) => String(value).trim().toUpperCase()),
  ).size;
}

function resolveMotherBatchNo(row: Record<string, any>) {
  return resolveSelectedSourceBatchNo(row);
}

function isGlueBoardMaterialItem(itemCategory: string, itemName: string) {
  return itemCategory.trim() === '压辊' && itemName.trim().includes('压辊料号');
}

function isGlueBoardBatchItem(itemCategory: string, itemName: string) {
  const name = itemName.trim();
  return itemCategory.trim() === '压辊' && (name.includes('压辊批号') || name.includes('压槽辊编号') || name.includes('压辊编码'));
}

function getDefaultCheckActualValue(item: Pick<AdhesiveCheckItem, 'itemCategory' | 'itemName'>) {
  const itemCategory = item.itemCategory || '';
  const itemName = item.itemName || '';
  if (isGlueBoardMaterialItem(itemCategory, itemName)) {
    return glueBoard.materialCode || '';
  }
  if (isGlueBoardBatchItem(itemCategory, itemName)) {
    return glueBoard.batchNo || '';
  }
  return '';
}

function applyGlueBoardDefaultsToCheckItems() {
  checkTemplate.value = checkTemplate.value.map((item) => {
    if (isGlueBoardMaterialItem(item.itemCategory || '', item.itemName || '')) {
      return { ...item, actualValue: glueBoard.materialCode || '' };
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
    dualLabel1: item?.dualLabel1 || '',
    dualLabel2: item?.dualLabel2 || '',
    itemCategory,
    itemName,
    requiredFlag: item?.requiredFlag === true,
    sortNo: Number(item?.sortNo || item?.itemSeq || index + 1),
    standardValue: item?.standardValue || item?.standard || '',
    valueMode: item?.valueMode || 'OK_NG',
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
    formCode: record?.formCode || `PRESS_SLOT_DAILY_${index + 1}`,
    key: record?.formCode || record?.id || `press-slot-daily-${index + 1}`,
    name: record?.name || '压槽工作准备记录',
    recordDate: record?.recordDate || '',
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
        { category: '检查项目', item: '模温机', node: '开工前', remark: '', result: 'OK', seq: 1, standard: '导热油液位正常、制热正常、无异常报警', value: '' },
        { category: '检查项目', item: '加热油路系统', node: '开工前', remark: '', result: 'OK', seq: 2, standard: '无漏油、渗油', value: '' },
        { category: '检查项目', item: '旋转接头', node: '开工前', remark: '', result: 'OK', seq: 3, standard: '无漏油、渗油', value: '' },
        { category: '检查项目', item: '齿轮减速机', node: '开工前', remark: '', result: 'OK', seq: 4, standard: '运行正常、无异响', value: '' },
        { category: '检查项目', item: '升降丝杆', node: '开工前', remark: '', result: 'OK', seq: 5, standard: '运行无卡顿、无异响', value: '' },
        { category: '检查项目', item: '压槽辊', node: '开工前', remark: '', result: 'OK', seq: 6, standard: '表面无明显油污粉尘、运行无卡顿、无异响', value: '' },
        { category: '检查项目', item: '橡胶辊', node: '开工前', remark: '', result: 'OK', seq: 7, standard: '表面无明显油污粉尘、运行无卡顿、无异响', value: '' },
      ],
      formCode: 'PRESS_SLOT_STARTUP_CHECK',
      key: 'PRESS_SLOT_STARTUP_CHECK',
      name: 'CMP软垫压槽开机点检表',
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
        { category: '检验项目', item: '压槽辊', node: '开工前', remark: '', result: 'OK', seq: 1, standard: '目视表面洁净且用无尘布擦拭无脏污', value: '' },
        { category: '检验项目', item: '橡胶辊', node: '开工前', remark: '', result: 'OK', seq: 2, standard: '目视表面洁净且用无尘布擦拭无脏污', value: '' },
        { category: '检验项目', item: '导热油管路', node: '开工前', remark: '', result: 'OK', seq: 3, standard: '表面无油污、脏污、异物', value: '' },
        { category: '检验项目', item: '旋转丝杆', node: '开工前', remark: '', result: 'OK', seq: 4, standard: '表面无油污、脏污、异物', value: '' },
        { category: '检验项目', item: '左右电机', node: '开工前', remark: '', result: 'OK', seq: 5, standard: '表面无油污、脏污、异物', value: '' },
        { category: '检验项目', item: '减速机', node: '开工前', remark: '', result: 'OK', seq: 6, standard: '表面无油污、脏污', value: '' },
        { category: '检验项目', item: '油压装置', node: '开工前', remark: '', result: 'OK', seq: 7, standard: '表面无油污、脏污', value: '' },
        { category: '检验项目', item: '模温机', node: '开工前', remark: '', result: 'OK', seq: 8, standard: '表面无油污、脏污', value: '' },
        { category: '检验项目', item: '整机框架及电灯', node: '开工前', remark: '', result: 'OK', seq: 9, standard: '表面无油污、脏污', value: '' },
      ],
      formCode: 'PRESS_SLOT_CLEANING_CHECK',
      key: 'PRESS_SLOT_CLEANING_CHECK',
      name: 'CMP软垫压槽设备清洁点检表',
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
        { category: '检查项目', item: '检查电源电压指示灯是否正常', node: '开工前', remark: '', result: 'OK', seq: 1, standard: '指示灯显示为绿色正常', value: '' },
        { category: '检查项目', item: '检查触发手柄是否正常', node: '开工前', remark: '', result: 'OK', seq: 2, standard: '按键运行流畅为正常', value: '' },
        { category: '检查项目', item: '检查支柱横梁是否正常', node: '开工前', remark: '', result: 'OK', seq: 3, standard: '无松动无脱焊为正常', value: '' },
        { category: '检查项目', item: '检查起吊电机是否正常', node: '开工前', remark: '', result: 'OK', seq: 4, standard: '无噪音，运行自如为正常', value: '' },
        { category: '检查项目', item: '检查行走电机是否正常', node: '开工前', remark: '', result: 'OK', seq: 5, standard: '无噪音，运行自如为正常', value: '' },
        { category: '检查项目', item: '检查钢丝绳是否正常', node: '开工前', remark: '', result: 'OK', seq: 6, standard: '无断丝，无缠绕打结为正常', value: '' },
        { category: '检查项目', item: '检查吊钩是否正常', node: '开工前', remark: '', result: 'OK', seq: 7, standard: '无破损为正常', value: '' },
        { category: '检查项目', item: '检查行程开关是否正常', node: '开工前', remark: '', result: 'OK', seq: 8, standard: '正常闭合为正常', value: '' },
        { category: '检查项目', item: '检查导绳器是否正常', node: '开工前', remark: '', result: 'OK', seq: 9, standard: '导绳顺序正常为正常', value: '' },
      ],
      formCode: 'PRESS_SLOT_HOIST_CHECK',
      key: 'PRESS_SLOT_HOIST_CHECK',
      name: '电动葫芦点检表',
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
    materialName: '',
    modelCode: '',
    planId: undefined,
    planNo: '',
    planOperationId: undefined,
    requirements: '',
    sourceBatchNo: '',
    sourceProductionBatchNo: '',
    startTime: '',
    status: '',
    workCenterId: undefined,
    workCenterName: '',
  });
  reportRecords.value = [];
  changeoverInspections.value = [];
  latestChangeoverInspection.value = null;
  firstInspection.value = { allowReportSubmit: false };
  firstInspectionFaiRows.value = [];
  firstInspectionProcessFormRecords.value = {};
  processCheckFaiRows.value = [];
  abnormalLockRows.value = [];
  activeAbnormalLockSummary.value = null;
  firstInspectionScanMode.value = 'FIRST_INSPECTION';
  firstInspectionScanNo.value = '';
  firstInspectionScanError.value = '';
  firstInspectionSource.value = null;
  processCheckScanReport.value = null;
  processParameterRecords.value = [];
  resetIntermediateForm();
  resetProcessParamFilters(false);
  selectedReportRecordKeys.value = [];
  sourceGroups.value = [];
  Object.assign(glueBoard, {
    alarm: '',
    aqcSampleLength: 0,
    availableCount: 0,
    availableStartPosition: 0,
    batchNo: '',
    id: undefined,
    latestAqcTask: undefined,
    lifeUsedCount: 0,
    lifeUsedLength: 0,
    lifetimeLimitCount: 0,
    lifetimeLimitLength: 0,
    lifetimeMode: '',
    lossCount: 0,
    lossLength: 0,
    materialCode: '',
    qualityLockReason: '',
    qualityLockStartPosition: undefined,
    qualityStatus: '',
    receiveLength: 0,
    receiveStartPosition: 0,
    receiveCount: 0,
    stockCount: 0,
    stockLength: 0,
    stockMeasureMode: 'COUNT',
    todayUsedCount: 0,
    todayUsedLength: 0,
  });
  Object.assign(bearingConsumable, {
    alarm: '',
    batchNo: '',
    id: undefined,
    lastReplaceTime: '',
    lifetimeLimitCount: 0,
    materialCode: '',
    materialName: '',
    stockCount: 0,
    stockId: undefined,
    todayUsedCount: 0,
  });
}

function applyBoardEquipment(equipment: Partial<BoardEquipment>) {
  boardEquipment.id = equipment.id;
  boardEquipment.code = equipment.code || '';
  boardEquipment.name = equipment.name || '';
  boardEquipment.workCenterId = equipment.workCenterId;
  boardEquipment.workCenterName = equipment.workCenterName || '';
}

function applyBoardEquipmentFromTask(task: any, manual = false) {
  const equipmentId = task?.equipmentId;
  const equipmentCode = String(task?.equipmentCode || '').trim();
  if (!equipmentId && !equipmentCode) return;
  if (manual) {
    boardEquipmentManualSelected.value = true;
  }
  applyBoardEquipment({
    code: equipmentCode,
    id: equipmentId,
    name: task?.equipmentName || '',
    workCenterId: task?.workCenterId,
    workCenterName: task?.workCenterName || '',
  });
}

function getEffectiveBoardEquipment(task?: any): BoardEquipment {
  return {
    code: boardEquipment.code || task?.equipmentCode || currentPlan.equipmentCode || '',
    id: boardEquipment.id ?? task?.equipmentId ?? currentPlan.equipmentId,
    name: boardEquipment.name || task?.equipmentName || currentPlan.equipmentName || '',
    workCenterId: boardEquipment.workCenterId ?? task?.workCenterId ?? currentPlan.workCenterId,
    workCenterName: boardEquipment.workCenterName || task?.workCenterName || currentPlan.workCenterName || '',
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

function applyTask(task: any) {
  const planOperationChanged = currentPlan.planOperationId !== task.planOperationId;
  const sourceBatchNo = resolveSelectedSourceBatchNo(task);
  const sourceProductionBatchNo = resolveSelectedSourceProductionBatchNo(task);
  const planBatchNo = resolveMotherSegmentBatchNo(task.batchNo || task.productionBatchNo || task.confirmedBatchNo || '')
    || sourceBatchNo;
  currentPlan.availableSourceLength = Number(task.availableSourceLength || 0);
  currentPlan.batchNo = planBatchNo;
  currentPlan.endTime = task.endTime || '';
  currentPlan.equipmentCode = task.equipmentCode || '';
  currentPlan.equipmentId = task.equipmentId;
  currentPlan.equipmentName = task.equipmentName || '';
  currentPlan.materialCode = task.materialCode || '';
  currentPlan.modelCode = task.modelCode || '';
  currentPlan.planId = task.planId;
  currentPlan.planNo = task.planNo || '';
  currentPlan.planOperationId = task.planOperationId;
  currentPlan.requirements = task.requirements || task.executeRequirement || '';
  currentPlan.sourceBatchNo = sourceBatchNo;
  currentPlan.sourceProductionBatchNo = sourceProductionBatchNo;
  currentPlan.startTime = task.startTime || '';
  currentPlan.status = task.status || '';
  currentPlan.workCenterId = task.workCenterId;
  currentPlan.workCenterName = task.workCenterName || '';
  if (!boardEquipmentManualSelected.value) {
    applyBoardEquipmentFromTask(task);
  }
  syncCurrentPlanEquipmentFromBoard(task);
  sourceGroups.value = [];
  selectedOneClickPressSlotBatchNo.value = '';
  selectedOneClickPressSlotSegmentKeys.value = [];
  if (planOperationChanged) {
    resetProcessParamFilters(false);
  }
}

function getSegmentLabel(segmentMark?: string) {
  return segmentMark ? `${segmentMark}段` : '不分段';
}

function buildSourceGroupsFromSources(sources: MesHcAdhesiveConsoleApi.SourceItem[]) {
  const groups = new Map<string, SourceGroup>();
  sources.forEach((source) => {
    const sourceAny = source as Record<string, any>;
    const batchNo = source.productionBatchNo || source.confirmedBatchNo || sourceAny.sourceProductionBatchNo || '';
    if (!batchNo) return;
    const baseBatchNo = resolveSelectedSourceBatchNo(sourceAny)
      || resolveMotherSegmentBatchNo(batchNo)
      || stripSegmentMark(batchNo);
    if (!baseBatchNo) return;
    const outputLength = 1;
    const reportRanges = getReportRanges(batchNo);
    const segment: SourceSegment = {
      availableLength: outputLength,
      batchNo,
      extraJson: source.extraJson || '',
      grindingSecondDetailId: source.grindingSecondDetailId,
      label: getSegmentLabel(source.segmentMark),
      outputLength,
      productQualityStatus: source.productQualityStatus || '',
      qtime: source.qtime,
      qualityLockReason: source.qualityLockReason || '',
      reportRanges,
      segmentMark: source.segmentMark || '',
      selfCheck: source.selfCheck || 'OK',
      sourceOperationName: source.operationName || source.processName || source.reportProcess || '',
      sourceProcessStage: source.processStage || '',
      usedLength: 0,
    };
    const availableLength = isPressSlotSegmentProcessable(segment) ? outputLength : 0;
    const usedLength = outputLength - availableLength;
    segment.usedLength = usedLength;
    segment.availableLength = availableLength;
    const existed = groups.get(baseBatchNo);
    if (existed) {
      existed.segments.push(segment);
      existed.qtime = mergePressSlotBatchQtime(existed.qtime, segment.qtime);
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
      qtime: segment.qtime,
      segments: [segment],
      totalAvailableLength: segment.availableLength,
      totalOutputLength: outputLength,
      totalUsedLength: usedLength,
    });
  });
  sourceGroups.value = Array.from(groups.values()).map((group) => ({
    ...group,
    segments: group.segments.sort((a, b) => comparePressSlotCardNo(a.batchNo, b.batchNo)),
  }));
  if (
    selectedOneClickPressSlotBatchNo.value
    && !sourceGroups.value.some((group) => isSameBatchNo(group.baseBatchNo, selectedOneClickPressSlotBatchNo.value))
  ) {
    selectedOneClickPressSlotBatchNo.value = '';
    selectedOneClickPressSlotSegmentKeys.value = [];
  } else if (selectedOneClickPressSlotBatchNo.value) {
    const selectedGroup = sourceGroups.value.find((group) =>
      isSameBatchNo(group.baseBatchNo, selectedOneClickPressSlotBatchNo.value),
    );
    const selectableKeys = new Set(
      (selectedGroup?.segments || [])
        .filter((segment) => isPressSlotSegmentProcessable(segment))
        .map((segment) => getPressSlotOneClickSegmentKey(segment)),
    );
    selectedOneClickPressSlotSegmentKeys.value = selectedOneClickPressSlotSegmentKeys.value
      .filter((key) => selectableKeys.has(key));
  }
}

let checkTemplateRequest = 0;
async function loadCheckTemplate(modelCode = currentPlan.modelCode) {
  const request = ++checkTemplateRequest;
  const normalizedModel = String(modelCode || '').trim().toUpperCase();
  const planKey = currentPlan.planOperationId;
  checkTemplate.value = [];
  checkTemplateModelCode.value = '';
  try {
    if (!normalizedModel) {
      throw new Error('当前压槽计划缺少型号，请先选择有效计划后再打开报工点检表');
    }
    const rows = normalizeRows(await getAdhesiveConsoleCheckTemplate(normalizedModel));
    if (request !== checkTemplateRequest || planKey !== currentPlan.planOperationId) {
      throw new Error('压槽计划或模板请求已切换，请重新打开点检表');
    }
    if (!rows.length) {
      throw new Error('压槽生产点检表模板配置没有找到配置，请联系管理员！');
    }
    checkTemplate.value = rows.map(normalizeCheckItem);
    checkTemplateModelCode.value = normalizedModel;
  } catch (error) {
    if (request === checkTemplateRequest) {
      checkTemplate.value = [];
      checkTemplateModelCode.value = '';
      if (!showPressSlotTemplateConfigError(error, '压槽生产点检表模板配置没有找到配置，请联系管理员！')) {
        message.error(getErrorMessage(error) || '压槽生产点检表模板加载失败');
      }
    }
    throw error;
  }
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
      equipmentId: equipment.id,
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
  const rows = normalizeRows(await getAdhesiveConsoleReportList(currentPlan.planOperationId, {
    scanConfirmDate: reportScanConfirmDate.value || undefined,
  }));
  reportRecords.value = rows;
  syncIntermediateCountersFromReports();
  selectedReportRecordKeys.value = selectedReportRecordKeys.value.filter((key) =>
    rows.some((record: any) => record.id === key),
  );
}

function formatReportScanConfirmDate(record?: Partial<MesHcAdhesiveConsoleApi.ReportItem> | Record<string, any>) {
  const value = String((record as Record<string, any> | undefined)?.confirmerTime || '');
  if (!value) return '-';
  const parsed = dayjs(value);
  if (parsed.isValid()) return parsed.format('YYYY-MM-DD');
  return value.length >= 10 ? value.slice(0, 10) : value;
}

function getFirstInspectionProcessFormKey(batchNo?: string, _inspectionType: FirstInspectionScanMode = 'FIRST_INSPECTION') {
  return String(batchNo || '').trim().toUpperCase();
}

function parseJsonObject(source?: string) {
  if (!source) return {};
  try {
    const parsed = JSON.parse(source);
    return parsed && typeof parsed === 'object' ? parsed as Record<string, any> : {};
  } catch {
    return {};
  }
}

function getFirstInspectionProcessRecordType(record?: Partial<MesHcAdhesiveConsoleApi.ProcessParamRecord> | Record<string, any>): FirstInspectionScanMode {
  const rawRecord = record as Record<string, any> | undefined;
  const context = parseJsonObject(String(rawRecord?.contextJson || ''));
  const sceneText = [
    record?.formName,
    record?.remark,
    rawRecord?.inspectionScene,
    context.inspectionScene,
    context.bindType,
  ].filter(Boolean).join('|').toUpperCase();
  return sceneText.includes('PROCESS_CHECK') || sceneText.includes('加检') ? 'PROCESS_CHECK' : 'FIRST_INSPECTION';
}

function isProcessCheckInspectionRow(row?: Partial<FirstInspectionProcessRow>) {
  return row?.inspectionType === 'PROCESS_CHECK'
    || String(row?.inspectionScene || '').toUpperCase() === 'PROCESS_CHECK'
    || String(row?.sourceReportNo || '').toUpperCase().includes('-PROCESS-CHECK-');
}

function getFirstInspectionProcessFormDisplayName(row?: Partial<FirstInspectionProcessRow>) {
  return isProcessCheckInspectionRow(row)
    ? PRESS_SLOT_PROCESS_CHECK_PROCESS_FORM_NAME
    : PRESS_SLOT_FIRST_INSPECTION_PROCESS_FORM_NAME;
}

function getFirstInspectionProcessSliceNo(record?: Partial<FirstInspectionProcessRow>) {
  return String(record?.firstInspectionSliceNo || record?.pressSlotSliceNo || record?.productBatchNo || '').trim();
}

function getFirstInspectionProcessModelCode(row?: Partial<FirstInspectionProcessRow>) {
  return currentPlan.modelCode || row?.productionModelCode || 'W26P0100';
}

function getFirstInspectionFaiResultText(record?: Partial<MesHcAdhesiveConsoleApi.FaiSummary>) {
  const judgment = String(record?.faiJudgment || '').trim().toUpperCase();
  if (judgment && judgment !== 'PENDING' && judgment !== 'WAIT_QA') {
    return judgment;
  }
  return record?.displayText || resolveFirstInspectionDisplayText(record?.faiStatus, record?.faiJudgment) || '-';
}

function getFirstInspectionProcessResultColor(result?: string) {
  const normalized = String(result || '').toUpperCase();
  if (normalized === 'OK') return 'success';
  if (normalized === 'NG' || normalized === 'ABNORMAL') return 'error';
  return 'default';
}

function getFirstInspectionProcessFormStatus(record?: MesHcAdhesiveConsoleApi.ProcessParamRecord) {
  if (!record?.id) return { text: '未保存', color: 'default' };
  const status = String(record.recordStatus || record.reportStatus || '').toUpperCase();
  return status === 'CONFIRMED'
    ? { text: '已确认', color: 'success' }
    : { text: '已保存', color: 'processing' };
}

function getFirstInspectionProcessRecordStatusText(status?: string) {
  const normalized = String(status || 'DRAFT').toUpperCase();
  if (normalized === 'CONFIRMED') return '已确认';
  if (normalized === 'SUBMITTED') return '已提交';
  return '草稿';
}

function getFirstInspectionProcessRecordDate(row?: Partial<FirstInspectionProcessRow>) {
  const timeText = normalizeDateTimeText(row?.submitTime || row?.recordTime || row?.faiApplyTime || '');
  return timeText ? dayjs(timeText).format('YYYY-MM-DD') : currentDateText.value;
}

function getFirstInspectionProcessMotherBatchNo(row?: Partial<FirstInspectionProcessRow>) {
  const sliceNo = getFirstInspectionProcessSliceNo(row);
  return normalizePressSlotMiddleBatchNo(
    row?.motherSegmentBatchNo
      || row?.inspectionScopeBatchNo
      || resolveMotherSegmentBatchNo(sliceNo)
      || getCurrentChangeoverMotherBatchNo()
      || currentMotherSegmentBatchNo.value
      || currentPlan.sourceBatchNo
      || '',
  );
}

function findPressSlotReportBySliceNo(sliceNo?: string) {
  const normalized = String(sliceNo || '').trim().toUpperCase();
  if (!normalized) return undefined;
  return reportRecords.value.find((record) => String(record.productionBatchNo || '').trim().toUpperCase() === normalized);
}

function buildFirstInspectionProductionCheckItem(
  row: FirstInspectionProcessRow,
  templateItem: AdhesiveCheckItem,
  index: number,
  existed?: Partial<MesHcAdhesiveConsoleApi.ProcessParamItem>,
): MesHcAdhesiveConsoleApi.ProcessParamItem {
  const sliceNo = getFirstInspectionProcessSliceNo(row);
  const sourceReport = findPressSlotReportBySliceNo(sliceNo);
  const sortNo = existed?.sortNo || existed?.seq || templateItem.sortNo || index + 1;
  return {
    ...existed,
    abnormalRemark: existed?.abnormalRemark || '',
    actualValue: existed?.actualValue ?? getDefaultCheckActualValue(templateItem),
    checkResult: existed?.checkResult || templateItem.checkResult || 'OK',
    dualLabel1: (existed as any)?.dualLabel1 || templateItem.dualLabel1 || '',
    dualLabel2: (existed as any)?.dualLabel2 || templateItem.dualLabel2 || '',
    itemCategory: existed?.itemCategory || templateItem.itemCategory || '其他',
    itemName: existed?.itemName || templateItem.itemName,
    modelCode: existed?.modelCode || getFirstInspectionProcessModelCode(row),
    motherBatchNo: normalizePressSlotMiddleBatchNo(existed?.motherBatchNo || getFirstInspectionProcessMotherBatchNo(row)),
    productionBatchNo: sliceNo,
    recorderName: existed?.recorderName || currentUserName.value || '',
    remark: existed?.remark || '',
    reportDate: existed?.reportDate || getFirstInspectionProcessRecordDate(row),
    seq: sortNo,
    sortNo,
    sourceReportId: existed?.sourceReportId || (row.sourceReportId as number | undefined) || (sourceReport?.id as number | undefined),
    standardValue: existed?.standardValue || templateItem.standardValue || '',
    valueMode: (existed as any)?.valueMode || templateItem.valueMode || 'OK_NG',
  };
}

function recordContainsFirstInspectionProcessSlice(
  record: Partial<MesHcAdhesiveConsoleApi.ProcessParamRecord>,
  sliceNo?: string,
) {
  const normalized = String(sliceNo || '').trim().toUpperCase();
  if (!normalized) return false;
  return String(record.productionBatchNo || '').trim().toUpperCase() === normalized
    || normalizeProcessParamItems(record.items).some((item) =>
      String(item.productionBatchNo || '').trim().toUpperCase() === normalized,
    );
}

function findFirstInspectionProcessParamRecord(
  row: Partial<FirstInspectionProcessRow>,
  records: MesHcAdhesiveConsoleApi.ProcessParamRecord[],
) {
  const sliceNo = getFirstInspectionProcessSliceNo(row);
  const candidates = records
    .filter((record) => recordContainsFirstInspectionProcessSlice(record, sliceNo))
    .sort((a, b) => Number(b.id || 0) - Number(a.id || 0));
  if (!candidates.length) return undefined;
  if (candidates.length > 1) throw new Error(`片号 ${sliceNo} 存在多份生产点检记录，请核查`);
  const candidate = candidates[0];
  if (candidate?.faiId && row.faiId && candidate.faiId !== row.faiId) throw new Error('点检记录与送检单不一致');
  return candidate;
}

function getPressSlotProductionCheckTemplateItems() {
  return checkTemplate.value
    .map((item, index) => {
      const sortNo = Number(item.sortNo || index + 1);
      return {
        ...item,
        abnormalRemark: '',
        actualValue: '',
        checkResult: item.checkResult || 'OK',
        itemCategory: item.itemCategory || '其他',
        seq: sortNo,
        sortNo,
        valueMode: item.valueMode || 'OK_NG',
      };
    })
    .sort((a, b) => Number(a.sortNo || a.seq || 0) - Number(b.sortNo || b.seq || 0));
}

function normalizeFirstInspectionProcessParamItems(row = currentFirstInspectionProcessRow.value) {
  const defaultDate = getFirstInspectionProcessRecordDate(row || undefined);
  const defaultModel = getFirstInspectionProcessModelCode(row || undefined);
  const defaultMother = getFirstInspectionProcessMotherBatchNo(row || undefined);
  const sliceNo = getFirstInspectionProcessSliceNo(row || undefined);
  const savedItems = (Array.isArray(firstInspectionProcessRecord.items) ? firstInspectionProcessRecord.items : [])
    .filter((item) => !sliceNo || String(item.productionBatchNo || sliceNo).trim().toUpperCase() === sliceNo.toUpperCase());
  const templateItems = checkTemplate.value.length
    ? getPressSlotProductionCheckTemplateItems()
    : savedItems;
  const rows = templateItems.map((templateItem, index) => {
    const targetSeq = Number(templateItem.sortNo || templateItem.seq || index + 1);
    const matched = savedItems.find((item) => Number(item.sortNo || item.seq || 0) === targetSeq)
      || savedItems.find((item) =>
      normalizePressSlotCheckItemName(item.itemName) === normalizePressSlotCheckItemName(templateItem.itemName)
        && String(item.itemCategory || '') === String(templateItem.itemCategory || ''),
      ) || savedItems[index];
    return buildFirstInspectionProductionCheckItem(row as FirstInspectionProcessRow, templateItem as AdhesiveCheckItem, index, matched);
  });
  return rows.map((item, index) => ({
    ...item,
    modelCode: item.modelCode || defaultModel,
    motherBatchNo: normalizePressSlotMiddleBatchNo(item.motherBatchNo || defaultMother),
    productionBatchNo: item.productionBatchNo || sliceNo,
    recorderName: item.recorderName || firstInspectionProcessHeader.recorderName || currentUserName.value || '',
    reportDate: item.reportDate || defaultDate,
    seq: item.seq || item.sortNo || index + 1,
    sortNo: item.sortNo || item.seq || index + 1,
  }));
}

const firstInspectionProcessVisibleItems = computed(() => {
  const items = Array.isArray(firstInspectionProcessRecord.items) ? firstInspectionProcessRecord.items : [];
  return [...items].sort((a, b) => Number(a.sortNo || a.seq || 0) - Number(b.sortNo || b.seq || 0));
});

async function loadFirstInspectionProcessFormRecords() {
  if (!currentPlan.planOperationId) {
    firstInspectionProcessFormRecords.value = {};
    return;
  }
  const currentMother = normalizePressSlotMiddleBatchNo(getCurrentChangeoverMotherBatchNo());
  const firstRows = firstInspectionFaiRows.value.length
    ? firstInspectionFaiRows.value
    : (firstInspection.value?.faiId ? [firstInspection.value] : []);
  const sourceRows = [
    ...firstRows.map((record) => ({ inspectionType: 'FIRST_INSPECTION' as FirstInspectionScanMode, record })),
    ...processCheckFaiRows.value.map((record) => ({ inspectionType: 'PROCESS_CHECK' as FirstInspectionScanMode, record })),
  ];
  const rowRefs = sourceRows
    .filter(({ record }) => {
      const sliceNo = getFirstInspectionProcessSliceNo(record);
      const mother = normalizePressSlotMiddleBatchNo(record.inspectionScopeBatchNo || resolveMotherSegmentBatchNo(sliceNo));
      return sliceNo && (!currentMother || mother === currentMother);
    })
    .map(({ inspectionType, record }) => ({
      inspectionType,
      key: getFirstInspectionProcessFormKey(getFirstInspectionProcessSliceNo(record), inspectionType),
      motherSegmentBatchNo: normalizePressSlotMiddleBatchNo(record.inspectionScopeBatchNo || resolveMotherSegmentBatchNo(getFirstInspectionProcessSliceNo(record))),
      record,
      sliceNo: getFirstInspectionProcessSliceNo(record),
    }));
  const records = normalizeRows(await getPressSlotProcessParamList(currentPlan.planOperationId, {
    formType: PRESS_SLOT_PRODUCTION_CHECK_FORM_TYPE,
    motherBatchNo: currentMother || undefined,
  }));
  const nextMap: Record<string, MesHcAdhesiveConsoleApi.ProcessParamRecord> = {};
  rowRefs.forEach((rowRef) => {
    const matched = findFirstInspectionProcessParamRecord({
      ...(rowRef.record as Record<string, any>),
      firstInspectionSliceNo: rowRef.sliceNo,
      inspectionType: rowRef.inspectionType,
      motherSegmentBatchNo: rowRef.motherSegmentBatchNo,
    } as FirstInspectionProcessRow, records);
    if (matched) {
      nextMap[rowRef.key] = matched;
    }
  });
  firstInspectionProcessFormRecords.value = nextMap;
}

async function loadFirstInspectionFaiRows(query = buildPressSlotFaiSummaryQuery()) {
  if (!currentPlan.planId || !currentPlan.planOperationId) {
    firstInspectionFaiRows.value = [];
    firstInspectionProcessFormRecords.value = {};
    return;
  }
  const queryKey = getPressSlotFaiSummaryQueryKey(query);
  const rows = normalizeRows(await getPressSlotFaiList(query));
  if (queryKey !== getCurrentPressSlotFaiSummaryQueryKey()) {
    return;
  }
  firstInspectionFaiRows.value = rows;
  await loadFirstInspectionProcessFormRecords();
}

async function loadChangeoverInspections() {
  if (!currentPlan.planOperationId) {
    changeoverInspections.value = [];
    latestChangeoverInspection.value = null;
    firstInspectionProcessFormRecords.value = {};
    return;
  }
  changeoverInspections.value = normalizeRows(await getPressSlotChangeoverList(currentPlan.planOperationId));
  latestChangeoverInspection.value = await getPressSlotChangeoverLatest(currentPlan.planOperationId);
  await loadFirstInspectionProcessFormRecords();
}

function clearFirstInspectionProcessRecord() {
  Object.keys(firstInspectionProcessRecord).forEach((key) => delete (firstInspectionProcessRecord as Record<string, any>)[key]);
}

function clearFirstInspectionProcessHeader() {
  Object.keys(firstInspectionProcessHeader).forEach((key) => delete firstInspectionProcessHeader[key]);
}

function buildFirstInspectionProcessHeader(row: FirstInspectionProcessRow) {
  const submitTime = normalizeDateTimeText(row.submitTime || row.recordTime || row.faiApplyTime || '');
  const inspectionScene = isProcessCheckInspectionRow(row) ? 'PROCESS_CHECK' : 'FIRST_INSPECTION';
  return {
    batchNo: row.firstInspectionSliceNo,
    endTime: buildNowText(),
    inspectionScene,
    inspectionTypeName: row.inspectionTypeName || (inspectionScene === 'PROCESS_CHECK' ? '过程加检' : '首检'),
    inspectionResult: row.inspectionResultText,
    materialCode: currentPlan.materialCode || row.productionMaterialCode || '',
    modelCode: getFirstInspectionProcessModelCode(row),
    productionDate: getFirstInspectionProcessRecordDate(row),
    recorderName: currentUserName.value || '',
    sourceExcel: PRESS_SLOT_FIRST_INSPECTION_PROCESS_EXCEL,
    startTime: submitTime,
    submitTime,
  };
}

function resetFirstInspectionProcessHeader(
  row: FirstInspectionProcessRow,
  detail?: MesHcAdhesiveConsoleApi.ProcessParamRecord | null,
) {
  clearFirstInspectionProcessHeader();
  Object.assign(firstInspectionProcessHeader, {
    ...buildFirstInspectionProcessHeader(row),
    materialCode: detail?.materialCode || currentPlan.materialCode || row.productionMaterialCode || '',
    modelCode: detail?.modelCode || getFirstInspectionProcessModelCode(row),
    productionDate: detail?.reportDate || getFirstInspectionProcessRecordDate(row),
    recorderName: detail?.fillUserName || detail?.recorderName || currentUserName.value || '',
  });
}

async function openFirstInspectionProcessForm(row: FirstInspectionProcessRow) {
  if (!currentPlan.planId || !currentPlan.planOperationId) {
    message.warning('请先加载压槽计划');
    return;
  }
  const sliceNo = row.firstInspectionSliceNo;
  if (!sliceNo || sliceNo === '-') {
    message.warning(`压槽片号为空，无法填写${getFirstInspectionProcessFormDisplayName(row)}`);
    return;
  }
  currentFirstInspectionProcessRow.value = row;
  firstInspectionProcessFormVisible.value = true;
  firstInspectionProcessFormLoading.value = true;
  try {
    await loadReports();
    const motherBatchNo = getFirstInspectionProcessMotherBatchNo(row);
    const candidateRecords = normalizeRows(await getPressSlotProcessParamList(currentPlan.planOperationId, {
      formType: PRESS_SLOT_PRODUCTION_CHECK_FORM_TYPE,
      motherBatchNo: motherBatchNo || undefined,
      productionBatchNo: sliceNo,
    }));
    const detail = row.processFormRecord || findFirstInspectionProcessParamRecord(row, candidateRecords);
    const modelCode = getFirstInspectionProcessModelCode(row);
    const template = detail?.runtimeSchema ? undefined : detail?.templateId ? { id: detail.templateId } : await getPressSlotProductionCheckTemplate(modelCode);
    productionCheckForm.value = template?.id ? await getStationFormDetail(template.id) : undefined;
    productionCheckRuntimeSchema.value = productionCheckSchema(detail?.runtimeSchema || parseJsonObject(productionCheckForm.value?.schemaJson));
    productionCheckRuntimeItems.value = productionCheckForm.value?.items || [];
    clearFirstInspectionProcessRecord();
    Object.assign(firstInspectionProcessRecord, {
      ...(detail || {}),
      faiId: row.faiId,
      templateId: detail?.templateId || template?.id,
      confirmTime: detail?.confirmTime || '',
      confirmUserName: detail?.confirmUserName || '',
      fillTime: detail?.fillTime || buildNowText(),
      fillUserName: detail?.fillUserName || currentUserName.value || '',
      formName: getFirstInspectionProcessFormDisplayName(row),
      formType: PRESS_SLOT_PRODUCTION_CHECK_FORM_TYPE,
      formTypeName: '生产点检',
      id: normalizeRecordId(detail?.id),
      importAttachment: detail?.importAttachment,
      inspectionScene: row.inspectionType,
      items: Array.isArray(detail?.items) ? detail.items.map((item, index) => ({ ...item, seq: item.seq || index + 1 })) : [],
      materialCode: detail?.materialCode || currentPlan.materialCode || '',
      modelCode: detail?.modelCode || modelCode,
      motherBatchNo: normalizePressSlotMiddleBatchNo(detail?.motherBatchNo || detail?.parentProductionBatchNo || motherBatchNo),
      parentProductionBatchNo: normalizePressSlotMiddleBatchNo(detail?.parentProductionBatchNo || detail?.motherBatchNo || motherBatchNo),
      planId: currentPlan.planId,
      planNo: currentPlan.planNo,
      planOperationId: currentPlan.planOperationId,
      productionBatchNo: sliceNo,
      recordStatus: detail?.recordStatus || detail?.reportStatus || 'SUBMITTED',
      recorderName: detail?.recorderName || detail?.fillUserName || currentUserName.value || '',
      remark: detail?.remark || `${row.inspectionTypeName || '首检'}压槽片号：${sliceNo}`,
      reportDate: detail?.reportDate || getFirstInspectionProcessRecordDate(row),
      sourceExcel: PRESS_SLOT_FIRST_INSPECTION_PROCESS_EXCEL,
      statusName: detail?.statusName || '',
    } as MesHcAdhesiveConsoleApi.ProcessParamRecord);
    resetFirstInspectionProcessHeader(row, detail);
    const savedRows = (detail?.items || []).map((item) => ({ ...item, standardText: item.standardValue, resultFlag: item.checkResult }));
    const defaults = parseJsonObject(productionCheckForm.value?.presetHeaderDataJson);
    Object.assign(firstInspectionProcessHeader, defaults, buildFirstInspectionProcessHeader(row), {
      planNo: currentPlan.planNo, equipmentCode: currentPlan.equipmentCode,
    }, detail?.headerData || {});
    Object.assign(firstInspectionProcessHeader, normalizeProductionCheckHeader(firstInspectionProcessHeader));
    firstInspectionProcessHeader.batchNo = sliceNo;
    firstInspectionProcessHeader.previewDetails = savedRows.length ? savedRows : productionCheckRows(productionCheckRuntimeItems.value, defaults.previewDetails || []);
    // 历史记录缺少稳定项 ID 时只按名称和原序号显式映射，禁止按数组位置拼接。
    if (detail && !detail.runtimeSchema) {
      for (const item of firstInspectionProcessHeader.previewDetails) {
        const matches = productionCheckRuntimeItems.value.filter((t) => t.itemName === item.itemName && t.itemSeq === (item.sortNo || item.seq));
        if (matches.length !== 1) throw new Error('历史点检明细与当前模板存在差异，请先核查，不能自动套用');
        item.templateItemId = matches[0]!.id;
      }
    }
  } catch (error) {
    firstInspectionProcessFormVisible.value = false;
    if (!showPressSlotTemplateConfigError(error, '压槽生产点检表模板配置没有找到配置，请联系管理员！')) {
      message.warning(getErrorMessage(error) || `打开${getFirstInspectionProcessFormDisplayName(row)}失败`);
    }
  } finally {
    firstInspectionProcessFormLoading.value = false;
  }
}

const productionCheckFillSchema = computed(() => ({
  ...productionCheckRuntimeSchema.value,
  runtimeLayout: {
    ...productionCheckRuntimeSchema.value.runtimeLayout,
    // 全屏填写使用内部滚动，顶部操作和底部签核信息不参与滚动。
    scrollMode: 'body',
    sections: (productionCheckRuntimeSchema.value.runtimeLayout?.sections || []).map((section: any) => ({
      ...section,
      fields: section.fields?.map((field: any) => field.field === 'batchNo' && field.editable !== false ? {
        ...field, component: 'Select',
        options: firstInspectionProcessRows.value.map((row) => ({ label: row.firstInspectionSliceNo, value: row.firstInspectionSliceNo })),
      } : field),
    })),
  },
}));

function updateProductionCheckHeader(value: Record<string, any>) {
  const original = getFirstInspectionProcessSliceNo(currentFirstInspectionProcessRow.value || undefined);
  const selected = String(value.batchNo || '').trim();
  if (original && selected !== original) {
    const target = firstInspectionProcessRows.value.find((row) => row.firstInspectionSliceNo === selected);
    productionCheckRuntime.value?.setHeaderData({ ...firstInspectionProcessHeader, batchNo: original });
    if (!target) { message.warning('请选择当前计划的有效送检片号'); return; }
    AModal.confirm({
      title: '切换送检片号',
      content: '切换后将加载该片对应的送检信息和点检记录，当前未保存内容不会保留。',
      onOk: () => openFirstInspectionProcessForm(target),
    });
    return;
  }
  Object.assign(firstInspectionProcessHeader, value);
}

function buildFirstInspectionProcessFormPayload(): MesHcAdhesiveConsoleApi.ProcessParamRecord {
  const currentRow = currentFirstInspectionProcessRow.value;
  const bindSliceNo = getFirstInspectionProcessSliceNo(currentRow || undefined);
  const reportDate = firstInspectionProcessHeader.productionDate || firstInspectionProcessRecord.reportDate || getFirstInspectionProcessRecordDate(currentRow || undefined);
  const motherBatchNo = normalizePressSlotMiddleBatchNo(
    firstInspectionProcessRecord.motherBatchNo
      || firstInspectionProcessRecord.parentProductionBatchNo
      || getFirstInspectionProcessMotherBatchNo(currentRow || undefined),
  );
  const modelCode = firstInspectionProcessHeader.modelCode || firstInspectionProcessRecord.modelCode || getFirstInspectionProcessModelCode(currentRow || undefined);
  const recorderName = firstInspectionProcessHeader.recorderName || firstInspectionProcessRecord.recorderName || currentUserName.value || '';
  const header = productionCheckRuntime.value?.getHeaderData() || { ...firstInspectionProcessHeader };
  if (String(header.batchNo || '').trim() !== bindSliceNo) throw new Error('片号是送检关联依据，请关闭后选择对应片号的送检记录');
  const scope = String(productionCheckRuntimeSchema.value.modelScope || '').toUpperCase();
  const prefix = String(productionCheckRuntimeSchema.value.modelPrefix || '').trim().toUpperCase();
  const exact = String(productionCheckRuntimeSchema.value.modelCode || '').trim().toUpperCase();
  const selectedModel = String(header.modelCode || '').trim().toUpperCase();
  if ((scope === 'PREFIX' && (!prefix || !selectedModel.startsWith(prefix)))
      || (scope === 'MODEL' && selectedModel !== exact)) throw new Error('型号与当前模板适用范围不匹配');
  const items = (header.previewDetails || []).map((item: any) => ({
    ...item, standardValue: item.standardText, checkResult: item.resultFlag,
    productionBatchNo: bindSliceNo, motherBatchNo, modelCode: header.modelCode, reportDate: header.productionDate,
  }));
  return {
    ...firstInspectionProcessRecord,
    fillUserName: recorderName,
    formName: getFirstInspectionProcessFormDisplayName(currentRow || undefined),
    formType: PRESS_SLOT_PRODUCTION_CHECK_FORM_TYPE,
    formTypeName: '生产点检',
    id: normalizeRecordId(firstInspectionProcessRecord.id),
    inspectionScene: currentRow?.inspectionType || 'FIRST_INSPECTION',
    items,
    headerData: header,
    templateId: firstInspectionProcessRecord.templateId,
    faiId: currentRow?.faiId,
    materialCode: header.materialCode ?? '',
    modelCode: header.modelCode,
    motherBatchNo,
    parentProductionBatchNo: motherBatchNo,
    planId: currentPlan.planId,
    planNo: currentPlan.planNo,
    planOperationId: currentPlan.planOperationId,
    productionBatchNo: bindSliceNo,
    firstInspectionSliceNo: bindSliceNo,
    recorderName,
    remark: firstInspectionProcessRecord.remark || `${currentRow?.inspectionTypeName || '首检'}压槽片号：${bindSliceNo}`,
    reportDate: header.productionDate || reportDate,
    sourceExcel: productionCheckRuntimeSchema.value.sourceExcel || firstInspectionProcessRecord.sourceExcel,
  };
}

function validateFirstInspectionProcessAuthAction(action: FirstInspectionProcessAuthAction) {
  const formName = getFirstInspectionProcessFormDisplayName(currentFirstInspectionProcessRow.value || undefined);
  const actionName = action === 'confirm' ? `确认${formName}` : `保存${formName}`;
  if (!ensureSegmentWritable(actionName)) return false;
  if (!firstInspectionProcessFormVisible.value || !currentFirstInspectionProcessRow.value) {
    message.warning('请先打开压槽生产点检表');
    return false;
  }
  if (isFirstInspectionProcessFormReadonly.value) {
    message.warning('已确认记录不能修改');
    return false;
  }
  return true;
}

function requestSaveFirstInspectionProcessForm() {
  if (!validateFirstInspectionProcessAuthAction('save')) return;
  pendingFirstInspectionProcessAuthAction.value = 'save';
  firstInspectionProcessAuthAction.value = `保存${getFirstInspectionProcessFormDisplayName(currentFirstInspectionProcessRow.value || undefined)}`;
  firstInspectionProcessAuthVisible.value = true;
}

function requestConfirmFirstInspectionProcessForm() {
  if (!validateFirstInspectionProcessAuthAction('confirm')) return;
  pendingFirstInspectionProcessAuthAction.value = 'confirm';
  firstInspectionProcessAuthAction.value = `确认${getFirstInspectionProcessFormDisplayName(currentFirstInspectionProcessRow.value || undefined)}`;
  firstInspectionProcessAuthVisible.value = true;
}

async function handleFirstInspectionProcessAuthSuccess(payload: any) {
  const pending = pendingFirstInspectionProcessAuthAction.value;
  pendingFirstInspectionProcessAuthAction.value = undefined;
  firstInspectionProcessAuthVisible.value = false;
  if (!pending) {
    message.warning('未找到待提交的压槽生产点检表，请重新点击保存或确认');
    return;
  }
  if (!validateFirstInspectionProcessAuthAction(pending)) return;
  const operator = resolveOperationAuthOperator(payload);
  if (!operator) {
    message.warning('请先完成记录人/确认人身份认证');
    return;
  }
  const now = buildNowText();
  if (pending === 'confirm') {
    firstInspectionProcessRecord.confirmUserName = operator;
    firstInspectionProcessRecord.confirmTime = now;
    firstInspectionProcessHeader.recorderName = firstInspectionProcessHeader.recorderName
      || firstInspectionProcessRecord.recorderName
      || firstInspectionProcessRecord.fillUserName
      || operator;
    firstInspectionProcessRecord.recorderName = firstInspectionProcessRecord.recorderName
      || firstInspectionProcessHeader.recorderName
      || operator;
    firstInspectionProcessRecord.fillUserName = firstInspectionProcessRecord.fillUserName
      || firstInspectionProcessRecord.recorderName
      || operator;
    firstInspectionProcessRecord.fillTime = firstInspectionProcessRecord.fillTime || now;
    await confirmFirstInspectionProcessForm();
    return;
  }
  firstInspectionProcessHeader.recorderName = operator;
  firstInspectionProcessRecord.recorderName = operator;
  firstInspectionProcessRecord.fillUserName = operator;
  firstInspectionProcessRecord.fillTime = now;
  await saveFirstInspectionProcessForm();
}

function handleFirstInspectionProcessAuthCancel() {
  pendingFirstInspectionProcessAuthAction.value = undefined;
  firstInspectionProcessAuthVisible.value = false;
}

async function saveFirstInspectionProcessForm(options: SaveFirstInspectionProcessFormOptions = {}) {
  if (!ensureSegmentWritable('保存首检')) return undefined;
  if (!currentFirstInspectionProcessRow.value) return undefined;
  firstInspectionProcessFormSaving.value = true;
  try {
    const payload = buildFirstInspectionProcessFormPayload();
    const saveResult = await savePressSlotProcessParam(payload);
    const id = resolveSavedReportId(saveResult, getFirstInspectionProcessFormDisplayName(currentFirstInspectionProcessRow.value || undefined));
    firstInspectionProcessRecord.id = id;
    Object.assign(firstInspectionProcessRecord, payload, { id });
    if (!options.silent) {
      message.success(`${getFirstInspectionProcessFormDisplayName(currentFirstInspectionProcessRow.value || undefined)}已保存`);
    }
    if (options.closeAfterSave !== false) {
      firstInspectionProcessFormVisible.value = false;
    }
    await Promise.all([loadProcessParameters(), loadFirstInspectionProcessFormRecords()]);
    return id;
  } catch (error) {
    if (!showPressSlotTemplateConfigError(error, '压槽生产点检表模板配置没有找到配置，请联系管理员！')) {
      message.error(getErrorMessage(error) || `${getFirstInspectionProcessFormDisplayName(currentFirstInspectionProcessRow.value || undefined)}保存失败`);
    }
    throw error;
  } finally {
    firstInspectionProcessFormSaving.value = false;
  }
}

async function confirmFirstInspectionProcessForm() {
  if (!ensureSegmentWritable('确认首检')) return;
  if (!currentFirstInspectionProcessRow.value) return;
  firstInspectionProcessFormConfirming.value = true;
  try {
    const confirmResult = await confirmPressSlotProcessParam(buildFirstInspectionProcessFormPayload());
    const id = resolveSavedReportId(confirmResult, getFirstInspectionProcessFormDisplayName(currentFirstInspectionProcessRow.value || undefined));
    Object.assign(firstInspectionProcessRecord, {
      id,
      confirmTime: buildNowText(),
      confirmUserName: currentUserName.value || firstInspectionProcessRecord.confirmUserName || '',
      recordStatus: 'CONFIRMED',
    });
    message.success(`${getFirstInspectionProcessFormDisplayName(currentFirstInspectionProcessRow.value || undefined)}已确认`);
    firstInspectionProcessFormVisible.value = false;
    await Promise.all([loadProcessParameters(), loadFirstInspectionProcessFormRecords()]);
  } catch (error) {
    if (!showPressSlotTemplateConfigError(error, '压槽生产点检表模板配置没有找到配置，请联系管理员！')) {
      message.error(getErrorMessage(error) || `${getFirstInspectionProcessFormDisplayName(currentFirstInspectionProcessRow.value || undefined)}确认失败`);
    }
    throw error;
  } finally {
    firstInspectionProcessFormConfirming.value = false;
  }
}

function buildPressSlotFaiSummaryQuery() {
  return {
    motherBatchNo: currentMotherSegmentBatchNo.value || currentPlan.sourceBatchNo || currentPlan.batchNo || '',
    planId: currentPlan.planId!,
    planOperationId: currentPlan.planOperationId!,
  };
}

function getPressSlotFaiSummaryQueryKey(query: {
  motherBatchNo?: string;
  planId?: number;
  planOperationId?: number;
}) {
  return [
    query.planId || '',
    query.planOperationId || '',
    normalizePressSlotMiddleBatchNo(query.motherBatchNo),
  ].join('|');
}

function getCurrentPressSlotFaiSummaryQueryKey() {
  if (!currentPlan.planId || !currentPlan.planOperationId) return '';
  return getPressSlotFaiSummaryQueryKey(buildPressSlotFaiSummaryQuery());
}

function shouldKeepExistingFirstInspectionOnEmpty(
  summary: MesHcAdhesiveConsoleApi.FaiSummary | null | undefined,
  queryMotherBatchNo?: string,
) {
  if (summary?.faiId || !firstInspection.value?.faiId) return false;
  const previousScope = normalizePressSlotMiddleBatchNo(
    firstInspection.value.inspectionScopeBatchNo || firstInspection.value.productBatchNo || queryMotherBatchNo,
  );
  const nextScope = normalizePressSlotMiddleBatchNo(summary?.inspectionScopeBatchNo || queryMotherBatchNo);
  return !previousScope || !nextScope || previousScope === nextScope;
}

async function loadPressSlotFaiSummary(options: LoadPressSlotFaiSummaryOptions = {}) {
  if (!currentPlan.planId || !currentPlan.planOperationId) {
    firstInspection.value = { allowReportSubmit: false };
    firstInspectionFaiRows.value = [];
    firstInspectionProcessFormRecords.value = {};
    return;
  }
  const query = buildPressSlotFaiSummaryQuery();
  const queryKey = getPressSlotFaiSummaryQueryKey(query);
  const summary = await getPressSlotFaiSummary(query);
  if (queryKey !== getCurrentPressSlotFaiSummaryQueryKey()) {
    return;
  }
  if (options.preserveExistingOnEmpty && shouldKeepExistingFirstInspectionOnEmpty(summary, query.motherBatchNo)) {
    return;
  }
  firstInspection.value = summary || { allowReportSubmit: false };
  await loadFirstInspectionFaiRows(query);
}

async function loadProcessCheckFaiRows() {
  if (!currentPlan.planId || !currentPlan.planOperationId) {
    processCheckFaiRows.value = [];
    return;
  }
  processCheckFaiRows.value = normalizeRows(await getPressSlotProcessCheckFaiList({
    motherBatchNo: currentMotherSegmentBatchNo.value || currentPlan.sourceBatchNo || currentPlan.batchNo || '',
    planId: currentPlan.planId,
    planOperationId: currentPlan.planOperationId,
  }));
  await loadFirstInspectionProcessFormRecords();
}

function buildAbnormalLockRefreshSignature(
  activeLock?: MesHcAdhesiveConsoleApi.PressSlotAbnormalLockRecord | null,
  rows: MesHcAdhesiveConsoleApi.PressSlotAbnormalLockRecord[] = [],
) {
  const rowKeys = rows
    .map((record) => [
      record.lockId || '',
      record.reportId || record.id || '',
      record.productionBatchNo || '',
      record.lockStatus || '',
      record.lockStartTime || '',
      record.abnormalResult || '',
      record.abnormalFeedbackTime || '',
      record.releaseResult || '',
      record.releaseSampleBatchNo || '',
      record.releaseFeedbackTime || '',
      record.releaseTime || '',
    ].join('#'))
    .sort();
  return JSON.stringify({
    activeLockId: activeLock?.lockId || activeLock?.id || '',
    activeLockStartTime: activeLock?.lockStartTime || activeLock?.abnormalSubmitTime || '',
    activeLockStatus: activeLock?.lockStatus || '',
    motherBatchNo: currentMotherSegmentBatchNo.value || currentPlan.sourceBatchNo || currentPlan.batchNo || '',
    planId: currentPlan.planId || '',
    planOperationId: currentPlan.planOperationId || '',
    rowKeys,
  });
}

async function loadPressSlotAbnormalLockRows(options: { refreshWorkbenchOnChange?: boolean } = {}) {
  if (!currentPlan.planId || !currentPlan.planOperationId) {
    abnormalLockRows.value = [];
    activeAbnormalLockSummary.value = null;
    lastAbnormalLockRefreshSignature = '';
    return;
  }
  const query = {
    motherBatchNo: currentMotherSegmentBatchNo.value || currentPlan.sourceBatchNo || currentPlan.batchNo || '',
    planId: currentPlan.planId,
    planOperationId: currentPlan.planOperationId,
  };
  const [activeLock, rows] = await Promise.all([
    getPressSlotActiveAbnormalLock(query),
    getPressSlotAbnormalLockList(query),
  ]);
  const normalizedRows = normalizeRows(rows);
  const nextSignature = buildAbnormalLockRefreshSignature(activeLock || null, normalizedRows);
  const shouldRefreshWorkbench = options.refreshWorkbenchOnChange
    && !!lastAbnormalLockRefreshSignature
    && lastAbnormalLockRefreshSignature !== nextSignature;
  activeAbnormalLockSummary.value = activeLock || null;
  abnormalLockRows.value = normalizedRows;
  lastAbnormalLockRefreshSignature = nextSignature;
  if (shouldRefreshWorkbench) {
    await Promise.all([loadProcessCheckFaiRows(), loadReports(), loadSourceGroups()]);
  }
}

async function refreshFirstInspectionStatus(options: {
  includeChangeover?: boolean;
  includeProcessCheck?: boolean;
  silent?: boolean;
} = {}) {
  if (!currentPlan.planId || !currentPlan.planOperationId) {
    if (!options.silent) {
      message.warning('请先扫码或选择压槽计划');
    }
    return false;
  }
  if (firstInspectionRefreshInFlight) return false;
  const includeChangeover = options.includeChangeover !== false;
  const includeProcessCheck = options.includeProcessCheck !== false;
  firstInspectionRefreshInFlight = true;
  if (!options.silent) {
    firstInspectionRefreshing.value = true;
  }
  try {
    await Promise.all([
      ...(includeChangeover ? [loadChangeoverInspections()] : []),
      loadPressSlotFaiSummary({ preserveExistingOnEmpty: options.silent === true }),
      ...(includeProcessCheck ? [loadProcessCheckFaiRows()] : []),
      loadPressSlotAbnormalLockRows({ refreshWorkbenchOnChange: options.silent === true }),
    ]);
    await loadFirstInspectionProcessFormRecords();
    if (!options.silent) {
      // message.success('首检状态已刷新');
    }
    return true;
  } catch (error) {
    if (options.silent) {
      console.warn('[press-slot] refresh first inspection status failed', error);
    }
    if (!options.silent) {
      message.warning(getErrorMessage(error) || '首检状态刷新失败，请稍后重试');
    }
    return false;
  } finally {
    firstInspectionRefreshInFlight = false;
    if (!options.silent) {
      firstInspectionRefreshing.value = false;
    }
  }
}

function getCurrentChangeoverMotherBatchNo() {
  return currentMotherSegmentBatchNo.value
    || resolveMotherSegmentBatchNo(currentPlan.sourceBatchNo || currentPlan.batchNo || currentPlan.sourceProductionBatchNo || '');
}

function getLatestChangeoverMotherBatchNo(record?: MesHcAdhesiveConsoleApi.ChangeoverInspection | null) {
  return resolveMotherSegmentBatchNo(record?.motherSegmentBatchNo || record?.pressSlotSliceNo || '');
}

function getChangeoverInspectionBlockMessage() {
  const summary = firstInspection.value;
  if (!summary?.faiId) {
    return '当前分段批号尚未提交 FAI 首检，请先扫码批号并提交首检申请。';
  }
  if (summary.restartRequired) {
    return '检验已返回NG，请使用新片重新首检；送检成功即可继续报工。';
  }
  if (!isFirstInspectionSubmitted(summary)) {
    return '当前分段批号最近一次 FAI 首检已失效，请重新扫码批号并提交首检。';
  }
  return '';
}

function showChangeoverInspectionRequiredWarning() {
  AModal.warning({
    title: '需要提交 FAI 首检',
    content: getChangeoverInspectionBlockMessage(),
    onOk: () => {
      if (canSubmitFirstInspection()) {
        openFirstInspectionScanDialog();
      }
    },
  });
}

async function loadProcessParameters() {
  if (!currentPlan.planOperationId) {
    processParameterRecords.value = [];
    return;
  }
  processParameterRecords.value = normalizeRows(await getPressSlotProcessParamList(currentPlan.planOperationId, {
    motherBatchNo: currentMotherSegmentBatchNo.value || undefined,
    productionBatchNo: processParamFilters.productionBatchNo.trim() || undefined,
    reportDate: processParamFilters.reportDate || undefined,
  }));
}

async function loadPressSlotMiddleLedger() {
  if (!currentPlan.planId || !currentPlan.planOperationId) {
    pressSlotIntermediateRecords.value = [];
    selectedIntermediateRecord.value = null;
    resetIntermediateForm();
    return;
  }
  const currentBatchNo = normalizePressSlotMiddleBatchNo(currentMotherSegmentBatchNo.value || '');
  const rows = normalizeRows(await getPressSlotIntermediateList(currentPlan.planOperationId, currentBatchNo || undefined));
  pressSlotIntermediateRecords.value = currentBatchNo
    ? rows.filter((item) => normalizePressSlotMiddleBatchNo(item.batchNo || '') === currentBatchNo)
    : rows;
  if (selectedIntermediateRecord.value?.id) {
    selectedIntermediateRecord.value = pressSlotIntermediateRecords.value.find((item) => item.id === selectedIntermediateRecord.value?.id) || null;
  }
}

async function resetProcessParamFilters(reload = true) {
  processParamFilters.productionBatchNo = '';
  processParamFilters.reportDate = '';
  if (reload) {
    await loadProcessParameters();
  }
}

function triggerProcessParamImport() {
  if (!ensureSegmentWritable('导入工艺参数')) return;
  if (!processParamEditVisible.value) {
    message.warning('请先打开工艺参数记录表');
    return;
  }
  processParamFileInput.value?.click();
}

function triggerFirstInspectionProcessImport() {
  if (!ensureSegmentWritable('导入首检')) return;
  if (!firstInspectionProcessFormVisible.value || !currentFirstInspectionProcessRow.value) {
    message.warning('请先打开压槽生产点检表');
    return;
  }
  if (String(firstInspectionProcessRecord.recordStatus || '').toUpperCase() === 'CONFIRMED') {
    message.warning('已确认记录不能导入');
    return;
  }
  firstInspectionProcessFileInput.value?.click();
}

function triggerMiddleLedgerImport() {
  if (!ensureSegmentWritable('导入中间品')) return;
  middleLedgerFileInput.value?.click();
}

function buildPressSlotImportAttachment(file: File, uploaded: any): PressSlotImportAttachment {
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

function normalizePressSlotImportAttachments(value?: PressSlotImportAttachment | PressSlotImportAttachment[] | unknown) {
  const list = Array.isArray(value) ? value : value ? [value] : [];
  return list
    .map((item, index) => {
      if (!item || typeof item !== 'object') return null;
      const raw = item as PressSlotImportAttachment & Record<string, any>;
      const path = String(raw.path || raw.filePath || '');
      const url = String(raw.url || raw.fileUrl || path || '');
      const name = String(raw.name || raw.fileName || url.split('/').pop() || `附件${index + 1}`);
      if (!url) return null;
      return { name, path: path || undefined, size: Number(raw.size) || undefined, type: raw.type, uploadTime: raw.uploadTime, url };
    })
    .filter(Boolean)
    .slice(-1) as PressSlotImportAttachment[];
}

const intermediateImportAttachment = computed(() => {
  const extra = parseRecordExtra(intermediateForm as Record<string, any>) as Record<string, any>;
  return normalizePressSlotImportAttachments(extra.attachments || extra.importAttachment || extra.processFormExcelAttachments)[0];
});

const processParamImportAttachment = computed(() =>
  normalizePressSlotImportAttachments(processParamEditForm.importAttachment)[0],
);
const firstInspectionProcessImportAttachment = computed(() =>
  normalizePressSlotImportAttachments(firstInspectionProcessRecord.importAttachment)[0],
);
const isFirstInspectionProcessFormReadonly = computed(() =>
  isWorkOrderFinished.value || String(firstInspectionProcessRecord.recordStatus || '').toUpperCase() === 'CONFIRMED',
);
const isProcessParamEditReadonly = computed(() =>
  isWorkOrderFinished.value || String(processParamEditForm.recordStatus || '').toUpperCase() === 'CONFIRMED',
);
const isIntermediateRecordReadonly = computed(() =>
  isWorkOrderFinished.value || String(intermediateForm.recordStatus || '').toUpperCase() === 'CONFIRMED',
);
const intermediateRecordStatusMeta = computed(() => {
  const status = String(intermediateForm.recordStatus || '').toUpperCase();
  if (status === 'CONFIRMED') return { color: 'processing', text: '已确认' };
  if (status === 'RECORDED') return { color: 'success', text: '已保存' };
  return { color: 'default', text: '草稿' };
});

function normalizeRecordId(value: unknown) {
  const id = Number(value);
  return Number.isFinite(id) && id > 0 ? id : undefined;
}

function formatPressSlotAttachmentSize(size?: number) {
  if (!size || size <= 0) return '';
  if (size < 1024) return `${size} B`;
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`;
  return `${(size / 1024 / 1024).toFixed(1)} MB`;
}

function openPressSlotImportAttachment(attachment?: PressSlotImportAttachment) {
  const url = attachment?.url || attachment?.path;
  if (!url) {
    message.warning('附件地址为空，无法下载');
    return;
  }
  window.open(url, '_blank');
}

async function uploadPressSlotImportAttachment(file: File, directory: string) {
  const uploaded = await uploadFile({ directory, file });
  return buildPressSlotImportAttachment(file, uploaded);
}

function normalizeProcessParamItems(items?: MesHcAdhesiveConsoleApi.ProcessParamItem[]) {
  return (Array.isArray(items) ? items : [])
    .map((item, index) => ({
      confirmerName: item.confirmerName || '',
      endTime: item.endTime || '',
      environmentHumidity: item.environmentHumidity || '',
      environmentTemperature: item.environmentTemperature || '',
      measuredTemperature1: item.measuredTemperature1 || '',
      measuredTemperature2: item.measuredTemperature2 || '',
      measuredTemperature3: item.measuredTemperature3 || '',
      measuredTemperature4: item.measuredTemperature4 || '',
      measuredTemperature5: item.measuredTemperature5 || '',
      modelCode: item.modelCode || currentPlan.modelCode || '',
      motherBatchNo: normalizePressSlotMiddleBatchNo(item.motherBatchNo || processParamEditForm.motherBatchNo || currentMotherSegmentBatchNo.value || ''),
      pressSlotOrder: item.pressSlotOrder || '',
      pressSlotSize: item.pressSlotSize || '',
      pressSlotSpeed: item.pressSlotSpeed || '',
      productionBatchNo: String(item.productionBatchNo || '').trim(),
      recorderName: item.recorderName || currentUserName.value || '',
      remark: item.remark || '',
      reportDate: item.reportDate || processParamEditForm.reportDate || dayjs().format('YYYY-MM-DD'),
      rollerGap: item.rollerGap || '',
      seq: item.seq || index + 1,
      setTemperature: item.setTemperature || '',
      sourceReportId: item.sourceReportId,
      startTime: item.startTime || '',
      thickness: item.thickness || '',
      widthM: item.widthM ?? '',
    }))
    .filter((item) => !!item.productionBatchNo)
    .map((item, index) => ({ ...item, seq: index + 1 }));
}

function resolveProcessParamMotherBatchNo(record: Partial<MesHcAdhesiveConsoleApi.ReportItem>) {
  return normalizePressSlotMiddleBatchNo(
    record.sourceProductionBatchNo || record.productionBatchNo || record.parentProductionBatchNo || record.sourceBatchNo || '',
  );
}

function buildInitialProcessParamItems(existingRecords = processParameterRecords.value) {
  const included = new Set(
    existingRecords.flatMap((record) => normalizeProcessParamItems(record.items))
      .map((item) => String(item.productionBatchNo || '').trim().toUpperCase())
      .filter(Boolean),
  );
  const currentMother = normalizePressSlotMiddleBatchNo(currentMotherSegmentBatchNo.value || '');
  return reportRecords.value
    .filter((record) => String(record.reportStatus || '').toUpperCase() === 'CONFIRMED')
    .filter((record) => {
      const mother = resolveProcessParamMotherBatchNo(record);
      return !currentMother || mother === currentMother;
    })
    .filter((record) => {
      const sliceNo = String(record.productionBatchNo || '').trim();
      return sliceNo && !included.has(sliceNo.toUpperCase());
    })
    .map((record, index) => ({
      confirmerName: record.confirmerName || '',
      endTime: displayTimeText(record.endTime),
      modelCode: record.modelCode || currentPlan.modelCode || '',
      motherBatchNo: resolveProcessParamMotherBatchNo(record) || currentMother,
      productionBatchNo: record.productionBatchNo || '',
      recorderName: record.recorderName || currentUserName.value || '',
      reportDate: record.reportDate || dayjs().format('YYYY-MM-DD'),
      seq: index + 1,
      sourceReportId: record.id as number,
      startTime: displayTimeText(record.startTime),
    }));
}

async function handleProcessParamFileChange(event: Event) {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  input.value = '';
  if (!ensureSegmentWritable('导入工艺参数')) return;
  if (!file || !currentPlan.planId || !currentPlan.planOperationId) {
    return;
  }
  if (!processParamEditVisible.value) {
    message.warning('请先打开需要导入的工艺参数记录表');
    return;
  }
  processParamImporting.value = true;
  try {
    if (!normalizeRecordId(processParamEditForm.id)) {
      await saveProcessParamEdit(false, false, true);
    }
    const attachment = await uploadPressSlotImportAttachment(file, 'mes/press-slot-process-param');
    const count = await importPressSlotProcessParams(currentPlan.planId, currentPlan.planOperationId, file, attachment, {
      motherBatchNo: processParamEditForm.motherBatchNo || currentMotherSegmentBatchNo.value || undefined,
      recordDate: processParamEditForm.reportDate,
      recordId: normalizeRecordId(processParamEditForm.id),
    });
    processParamEditForm.importAttachment = attachment;
    await Promise.all([loadReportsAndSourceGroups(), loadProcessParameters()]);
    const latest = processParameterRecords.value.find((item) => item.id === normalizeRecordId(processParamEditForm.id));
    if (latest) openProcessParamEdit(latest);
    message.success(`已导入 ${count || 0} 条压槽工艺参数记录`);
  } finally {
    processParamImporting.value = false;
  }
}

async function handleFirstInspectionProcessFileChange(event: Event) {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  input.value = '';
  if (!file || isFirstInspectionProcessFormReadonly.value || !ensureSegmentWritable('导入生产点检')) return;
  firstInspectionProcessImporting.value = true;
  try {
    const layout = productionCheckRuntime.value?.buildExcelLayout(firstInspectionProcessRecord.formName, 'import');
    if (!layout) return;
    const result = await importProcessFormRecordLayout(file, layout);
    productionCheckRuntime.value?.applyImportedLayout(result);
    firstInspectionProcessRecord.importAttachment = await uploadPressSlotImportAttachment(file, 'mes/press-slot-production-check');
    message.success('已导入当前表单，请核对后保存');
  } catch (error) {
    message.error(getErrorMessage(error) || '导入生产点检失败');
  } finally {
    firstInspectionProcessImporting.value = false;
  }
}

async function exportFirstInspectionProcessExcel() {
  const layout = productionCheckRuntime.value?.buildExcelLayout(firstInspectionProcessRecord.formName, 'export');
  if (!layout) return;
  const data = await exportProcessFormRecordLayout(layout);
  downloadFileFromBlobPart({ fileName: buildFirstInspectionProcessExcelFileName(currentFirstInspectionProcessRow.value || undefined), source: data });
}

function sanitizePressSlotExcelFileName(value?: string) {
  return String(value || 'CMP压槽工艺参数表').replace(/[\\/:*?"<>|]/gu, '_');
}

function buildFirstInspectionProcessExcelFileName(row?: Partial<FirstInspectionProcessRow>) {
  const sliceNo = String(
    firstInspectionProcessRecord.productionBatchNo || getFirstInspectionProcessSliceNo(row) || '',
  ).trim();
  return `${sanitizePressSlotExcelFileName(
    [getFirstInspectionProcessFormDisplayName(row), sliceNo].filter(Boolean).join('_'),
  )}.xlsx`;
}

function buildProcessParamExcelFileName() {
  const motherBatchNo = normalizePressSlotMiddleBatchNo(
    processParamEditForm.motherBatchNo || currentMotherSegmentBatchNo.value || currentPlan.sourceBatchNo || '',
  );
  const motherBatchNoText = motherBatchNo
    ? `${motherBatchNo}${motherBatchNo.endsWith('号') ? '' : '号'}`
    : '';
  return `${sanitizePressSlotExcelFileName(
    ['CMP压槽工艺参数表', motherBatchNoText].filter(Boolean).join('_'),
  )}.xlsx`;
}

async function handleMiddleLedgerFileChange(event: Event) {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  input.value = '';
  if (!ensureSegmentWritable('导入中间品')) return;
  if (!file || !currentPlan.planId || !currentPlan.planOperationId) {
    return;
  }
  if (!intermediateDetailVisible.value || !String(intermediateForm.batchNo || '').trim()) {
    message.warning('请先打开需要导入的压槽中间品记录表');
    return;
  }
  middleLedgerImporting.value = true;
  try {
    const attachment = await uploadPressSlotImportAttachment(file, 'mes/press-slot-middle-ledger');
    const count = await importPressSlotMiddleLedger(currentPlan.planId, currentPlan.planOperationId, file, attachment, {
      batchNo: intermediateForm.batchNo,
      recordDate: intermediateForm.recordDate,
      recordId: selectedIntermediateRecord.value?.id || intermediateForm.id,
    });
    await Promise.all([loadPressSlotMiddleLedger(), loadSourceGroups()]);
    await loadIntermediateRecord(selectedIntermediateRecord.value || undefined);
    message.success(`已导入 ${count || 0} 条压槽中间品记录`);
  } finally {
    middleLedgerImporting.value = false;
  }
}

async function exportProcessParamExcel() {
  if (!currentPlan.planOperationId) {
    message.warning('请先加载压槽计划');
    return;
  }
  const hasRows = normalizeProcessParamItems(processParamEditForm.items).length > 0;
  const recordId = normalizeRecordId(processParamEditForm.id);
  if (!isWorkOrderFinished.value && processParamEditVisible.value && (recordId || hasRows)) {
    await saveProcessParamEdit(false, false, true);
  }
  const data = await exportPressSlotProcessParams(currentPlan.planOperationId, {
    motherBatchNo: processParamEditForm.motherBatchNo || currentMotherSegmentBatchNo.value || undefined,
    recordId: processParamEditVisible.value ? normalizeRecordId(processParamEditForm.id) : undefined,
  });
  downloadFileFromBlobPart({ fileName: buildProcessParamExcelFileName(), source: data });
}

async function exportMiddleLedgerExcel() {
  if (!currentPlan.planOperationId) {
    message.warning('请先加载压槽计划');
    return;
  }
  if (!intermediateDetailVisible.value || !String(intermediateForm.batchNo || '').trim()) {
    message.warning('请先打开需要导出的压槽中间品记录表');
    return;
  }
  const recordId = selectedIntermediateRecord.value?.id || intermediateForm.id;
  if (!recordId) {
    message.warning('请先保存当前压槽中间品记录表，再导出当前批号');
    return;
  }
  const data = await exportPressSlotMiddleLedger(currentPlan.planOperationId, {
    batchNo: intermediateForm.batchNo,
    recordId,
  });
  downloadFileFromBlobPart({ fileName: 'CMP压槽中间品记录表.xlsx', source: data });
}

async function openProcessParamEdit(record?: MesHcAdhesiveConsoleApi.ProcessParamRecord) {
  if (!record?.id && !ensureSegmentWritable('上报工艺参数')) return;
  const existingRecords = record?.id || !currentPlan.planOperationId
    ? processParameterRecords.value
    : normalizeRows(await getPressSlotProcessParamList(currentPlan.planOperationId, {
        motherBatchNo: currentMotherSegmentBatchNo.value || undefined,
      }));
  const items = record?.id
    ? normalizeProcessParamItems(record.items)
    : buildInitialProcessParamItems(existingRecords);
  Object.assign(processParamEditForm, {
    id: record?.id,
    confirmTime: record?.confirmTime || '',
    confirmUserName: record?.confirmUserName || '',
    fillTime: record?.fillTime || '',
    fillUserName: record?.fillUserName || currentUserName.value || '',
    formName: record?.formName || 'CMP压槽工艺参数表',
    importAttachment: record?.importAttachment,
    items,
    materialCode: record?.materialCode || currentPlan.materialCode || '',
    modelCode: record?.modelCode || currentPlan.modelCode || '',
    motherBatchNo: normalizePressSlotMiddleBatchNo(record?.motherBatchNo || record?.parentProductionBatchNo || currentMotherSegmentBatchNo.value || currentPlan.sourceBatchNo || ''),
    parentProductionBatchNo: normalizePressSlotMiddleBatchNo(record?.motherBatchNo || record?.parentProductionBatchNo || currentMotherSegmentBatchNo.value || currentPlan.sourceBatchNo || ''),
    planId: record?.planId || currentPlan.planId,
    planNo: record?.planNo || currentPlan.planNo,
    planOperationId: record?.planOperationId || currentPlan.planOperationId,
    productionBatchNo: record?.productionBatchNo || items[0]?.productionBatchNo || '',
    recordStatus: record?.recordStatus || record?.reportStatus || 'SUBMITTED',
    recorderName: record?.recorderName || currentUserName.value || '',
    remark: record?.remark || '',
    reportDate: record?.reportDate || dayjs().format('YYYY-MM-DD'),
    statusName: record?.statusName || '',
  });
  processParamEditVisible.value = true;
}

function buildProcessParamPayload(): MesHcAdhesiveConsoleApi.ProcessParamRecord {
  return {
    ...processParamEditForm,
    id: normalizeRecordId(processParamEditForm.id),
    items: normalizeProcessParamItems(processParamEditForm.items),
    motherBatchNo: processParamEditForm.motherBatchNo || currentMotherSegmentBatchNo.value || undefined,
    parentProductionBatchNo: processParamEditForm.motherBatchNo || currentMotherSegmentBatchNo.value || undefined,
    planId: currentPlan.planId,
    planOperationId: currentPlan.planOperationId,
    planNo: currentPlan.planNo,
  };
}

function validateProcessParamAuthAction(action: ProcessParamAuthAction) {
  const actionName = action === 'confirm' ? '确认工艺参数' : '保存工艺参数';
  if (!ensureSegmentWritable(actionName)) return false;
  if (!currentPlan.planId || !currentPlan.planOperationId) {
    message.warning('请先加载压槽计划');
    return false;
  }
  if (action === 'save' && !normalizeProcessParamItems(processParamEditForm.items).length) {
    message.warning('工艺参数明细不能为空');
    return false;
  }
  return true;
}

function requestSaveProcessParamEdit() {
  if (!validateProcessParamAuthAction('save')) return;
  pendingProcessParamAuthAction.value = 'save';
  processParamAuthAction.value = '保存CMP压槽工艺参数表';
  processParamAuthVisible.value = true;
}

function requestConfirmProcessParamEdit() {
  if (!validateProcessParamAuthAction('confirm')) return;
  pendingProcessParamAuthAction.value = 'confirm';
  processParamAuthAction.value = '确认CMP压槽工艺参数表';
  processParamAuthVisible.value = true;
}

async function saveProcessParamEdit(closeAfter = true, showMessage = true, allowEmpty = false) {
  if (!ensureSegmentWritable('保存工艺参数')) return undefined;
  if (!currentPlan.planId || !currentPlan.planOperationId) {
    message.warning('请先加载压槽计划');
    return undefined;
  }
  if (!allowEmpty && !normalizeProcessParamItems(processParamEditForm.items).length) {
    message.warning('工艺参数明细不能为空');
    return undefined;
  }
  processParamEditSaving.value = true;
  try {
    const saveResult = await savePressSlotProcessParam(buildProcessParamPayload());
    const id = resolveSavedReportId(saveResult, '压槽工艺参数记录表');
    processParamEditForm.id = id;
    await Promise.all([loadReportsAndSourceGroups(), loadProcessParameters()]);
    if (closeAfter) processParamEditVisible.value = false;
    if (showMessage) message.success('工艺参数记录表已保存');
    return id;
  } catch (error) {
    if (!showPressSlotTemplateConfigError(error, 'CMP压槽工艺参数表模板配置没有找到配置，请联系管理员！')) {
      message.error(getErrorMessage(error) || '工艺参数记录表保存失败');
    }
    throw error;
  } finally {
    processParamEditSaving.value = false;
  }
}

async function handleProcessParamAuthSuccess(payload: any) {
  const pending = pendingProcessParamAuthAction.value;
  pendingProcessParamAuthAction.value = undefined;
  processParamAuthVisible.value = false;
  if (!pending) {
    message.warning('未找到待提交的工艺参数记录，请重新点击保存或确认');
    return;
  }
  const operator = resolveOperationAuthOperator(payload);
  if (!operator) {
    message.warning('请先完成填写人/确认人身份认证');
    return;
  }
  const now = buildNowText();
  if (pending === 'confirm') {
    processParamEditForm.confirmUserName = operator;
    processParamEditForm.confirmTime = now;
    processParamEditForm.fillUserName = processParamEditForm.fillUserName || processParamEditForm.recorderName || operator;
    processParamEditForm.recorderName = processParamEditForm.recorderName || processParamEditForm.fillUserName || operator;
    await confirmProcessParamEdit();
    return;
  }
  processParamEditForm.fillUserName = operator;
  processParamEditForm.recorderName = operator;
  processParamEditForm.fillTime = now;
  await saveProcessParamEdit();
}

function handleProcessParamAuthCancel() {
  pendingProcessParamAuthAction.value = undefined;
  processParamAuthVisible.value = false;
}

async function confirmProcessParamEdit() {
  if (!ensureSegmentWritable('确认工艺参数')) return;
  if (!currentPlan.planId || !currentPlan.planOperationId) {
    message.warning('请先加载压槽计划');
    return;
  }
  processParamConfirming.value = true;
  try {
    const confirmResult = await confirmPressSlotProcessParam(buildProcessParamPayload());
    const id = resolveSavedReportId(confirmResult, '压槽工艺参数记录表');
    processParamEditForm.id = id;
    await Promise.all([loadReportsAndSourceGroups(), loadProcessParameters()]);
    const latest = processParameterRecords.value.find((item) => item.id === id);
    if (latest) openProcessParamEdit(latest);
    message.success('工艺参数记录表已确认');
  } catch (error) {
    if (!showPressSlotTemplateConfigError(error, 'CMP压槽工艺参数表模板配置没有找到配置，请联系管理员！')) {
      message.error(getErrorMessage(error) || '工艺参数记录表确认失败');
    }
    throw error;
  } finally {
    processParamConfirming.value = false;
  }
}

async function loadGlueBoardUsage() {
  if (!currentPlan.planOperationId) {
    return;
  }
  const rows = normalizeRows(await getPressSlotConsumableStatus(currentPlan.planOperationId, currentPlan.equipmentId));
  const roller = rows.find((item: any) => item.consumableType === 'PRESS_ROLLER') || {};
  const bearing = rows.find((item: any) => item.consumableType === 'BEARING') || {};
  const rollerBlocked = ['EMPTY', 'NEED_CLEAN', 'NEED_REPLACE'].includes(String(roller?.status || '').toUpperCase());
  const bearingBlocked = ['EMPTY', 'NEED_CLEAN', 'NEED_REPLACE'].includes(String(bearing?.status || '').toUpperCase());
  const rollerMessage = simplifyPressSlotConsumableMessage(roller?.message || '压辊未挂接或寿命已达阈值，请先维护', '压槽辊');
  const bearingMessage = simplifyPressSlotConsumableMessage(bearing?.message || '轴承未挂接或寿命已达阈值，请先维护', '轴承');
  Object.assign(glueBoard, {
    alarm: rollerBlocked ? rollerMessage : '',
    aqcSampleLength: 0,
    availableCount: Number(roller?.availableCount || 0),
    availableStartPosition: 0,
    batchNo: roller?.batchNo || '',
    id: roller?.stateId,
    latestAqcTask: undefined,
    lifeUsedCount: Number(roller?.useCount || 0),
    lastCleanTime: roller?.lastCleanTime || '',
    useDays: Number(roller?.useDays || 0),
    limitDays: Number(roller?.limitDays || 60),
    lifeUsedLength: 0,
    lifetimeLimitCount: Number(roller?.limitCount || 0),
    lifetimeLimitLength: 0,
    lifetimeMode: 'COUNT',
    lossCount: 0,
    lossLength: 0,
    materialCode: roller?.materialCode || '',
    materialName: roller?.materialName || '',
    qualityLockReason: '',
    qualityLockStartPosition: undefined,
    qualityStatus: '',
    receiveCount: Number(roller?.onlineQuantity || 0),
    receiveLength: 0,
    receiveStartPosition: 0,
    stockCount: Number(roller?.availableCount || 0),
    stockLength: 0,
    stockMeasureMode: 'COUNT',
    stockId: roller?.stockId,
    todayUsedCount: Number(roller?.useCount || 0),
    todayUsedLength: 0,
    warning: !rollerBlocked && Number(roller?.warningFlag || 0) > 0 ? rollerMessage : '',
  });
  Object.assign(bearingConsumable, {
    alarm: bearingBlocked ? bearingMessage : '',
    batchNo: bearing?.batchNo || '',
    id: bearing?.stateId,
    lastReplaceTime: bearing?.lastReplaceTime || '',
    useDays: Number(bearing?.useDays || 0),
    limitDays: Number(bearing?.limitDays || 150),
    lifetimeLimitCount: Number(bearing?.limitCount || 0),
    materialCode: bearing?.materialCode || '',
    materialName: bearing?.materialName || '',
    stockCount: Number(bearing?.availableCount || 0),
    stockId: bearing?.stockId,
    todayUsedCount: Number(bearing?.useCount || 0),
    warning: !bearingBlocked && Number(bearing?.warningFlag || 0) > 0 ? bearingMessage : '',
  });
  applyGlueBoardDefaultsToCheckItems();
}

function resolvePressSlotIntermediateDefaultDate() {
  const planDate = dayjs(currentPlan.startTime || currentPlan.endTime || '');
  return planDate.isValid() ? planDate.format('YYYY-MM-DD') : dayjs().format('YYYY-MM-DD');
}

function resetIntermediateForm() {
  const recordDate = resolvePressSlotIntermediateDefaultDate();
  Object.assign(intermediateForm, {
    adhesiveReportId: undefined,
    batchNo: '',
    confirmerName: '',
    confirmTime: '',
    createTime: '',
    endSliceNo: '',
    fillTime: '',
    firstSampleSliceNo: '',
    firstSlotDepthAvg: undefined,
    firstSlotDepthXAvg: undefined,
    firstSlotDepthXMax: undefined,
    firstSlotDepthXMin: undefined,
    firstSlotDepthYAvg: undefined,
    firstSlotDepthYMax: undefined,
    firstSlotDepthYMin: undefined,
    firstSlotDepthMax: undefined,
    firstSlotDepthMin: undefined,
    frontSliceNo: '',
    id: undefined,
    inputQty: 0,
    materialCode: currentPlan.materialCode || '',
    middleSliceNo: '',
    modelCode: currentPlan.modelCode || '',
    outputQty: 0,
    planId: currentPlan.planId || 0,
    planNo: currentPlan.planNo || '',
    planOperationId: currentPlan.planOperationId || 0,
    processLength: 0,
    productionDate: recordDate,
    recordDate,
    recorderName: currentUserName.value || '',
    recordStatus: 'DRAFT',
    remark: '',
    slotDepthStandard: PRESS_SLOT_INTERMEDIATE_DEFAULT_SLOT_DEPTH_STANDARD,
    sourceExcel: '',
    sourceSheet: '',
    templateCode: '',
    templateName: '',
    thicknessColumnCount: PRESS_SLOT_INTERMEDIATE_DEFAULT_THICKNESS_COLUMN_COUNT,
    thicknessIntervalCm: PRESS_SLOT_INTERMEDIATE_DEFAULT_THICKNESS_INTERVAL_CM,
    thicknessStandard: PRESS_SLOT_INTERMEDIATE_DEFAULT_THICKNESS_STANDARD,
    updateTime: '',
    widthEnd: undefined,
    widthMiddle: undefined,
    widthStart: undefined,
  });
  intermediateDetails.value = buildDefaultPressSlotIntermediateDetails();
}

function normalizePressSlotMiddleBatchNo(batchNo?: string) {
  return String(batchNo || '')
    .trim()
    .replace(/-J\d+$/i, '')
    .replace(/-S\d+$/i, '')
    .replace(/^(.+[PQRS])\d{3}[A-Z]?$/i, '$1');
}

function resolveCurrentIntermediateBatchNo() {
  return normalizePressSlotMiddleBatchNo(
    currentMotherSegmentBatchNo.value
    || currentPlan.sourceBatchNo
    || currentPlan.sourceProductionBatchNo
    || currentPlan.parentProductionBatchNo
    || '',
  );
}

function resolveIntermediateReportMotherBatchNo(record?: Partial<MesHcAdhesiveConsoleApi.ReportItem>) {
  return normalizePressSlotMiddleBatchNo(
    record?.parentProductionBatchNo
    || record?.sourceBatchNo
    || record?.sourceProductionBatchNo
    || record?.productionBatchNo
    || '',
  );
}

function resolveIntermediateBatchNo() {
  const fromCurrent = resolveCurrentIntermediateBatchNo();
  if (fromCurrent) return fromCurrent;
  const fromReport = reportRecords.value
    .map((item) => item.sourceProductionBatchNo || item.productionBatchNo || item.parentProductionBatchNo || item.sourceBatchNo)
    .find(Boolean);
  const fromSource = sourceGroups.value
    .flatMap((group) => group.segments)
    .map((item) => (item as SourceSegment & { parentProductionBatchNo?: string }).parentProductionBatchNo || item.batchNo)
    .find(Boolean);
  return normalizePressSlotMiddleBatchNo(fromReport || fromSource || currentPlan.sourceProductionBatchNo || currentPlan.batchNo);
}

function syncIntermediateCountersFromReports() {
  const currentBatchNo = normalizePressSlotMiddleBatchNo(intermediateForm.batchNo || resolveCurrentIntermediateBatchNo());
  const operationReports = reportRecords.value
    .filter((item) => !currentBatchNo || resolveIntermediateReportMotherBatchNo(item) === currentBatchNo);
  intermediateForm.inputQty = operationReports.reduce((sum, item) => sum + Number(item.inputLength || item.outputLength || 1), 0);
  intermediateForm.outputQty = operationReports
    .filter((item) => isAdhesiveRecordConfirmed(item.reportStatus))
    .reduce((sum, item) => sum + Number(item.outputLength || item.inputLength || 1), 0);
  intermediateForm.batchNo = intermediateForm.batchNo || currentBatchNo || resolveIntermediateBatchNo();
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

function clearIntermediateSlotDepthPayload(payload: Record<string, any>) {
  for (const field of PRESS_SLOT_INTERMEDIATE_DEPTH_FIELDS) {
    payload[field] = undefined;
  }
}

function mapPressSlotIntermediateRecord(data?: MesHcAdhesiveConsoleApi.IntermediateRecord) {
  resetIntermediateForm();
  if (data?.id) {
    selectedIntermediateRecord.value = data;
  }
  Object.assign(intermediateForm, {
    ...data,
    batchNo: data?.batchNo || resolveIntermediateBatchNo(),
    firstSlotDepthAvg: data?.firstSlotDepthAvg ?? data?.firstSlotDepthXAvg ?? data?.firstSlotDepthYAvg,
    firstSlotDepthMax: data?.firstSlotDepthMax ?? data?.firstSlotDepthXMax ?? data?.firstSlotDepthYMax,
    firstSlotDepthMin: data?.firstSlotDepthMin ?? data?.firstSlotDepthXMin ?? data?.firstSlotDepthYMin,
    materialCode: data?.materialCode || currentPlan.materialCode || '',
    modelCode: data?.modelCode || currentPlan.modelCode || '',
    planId: currentPlan.planId || data?.planId || 0,
    planNo: currentPlan.planNo || data?.planNo || '',
    planOperationId: currentPlan.planOperationId || data?.planOperationId || 0,
    productionDate: data?.recordDate || data?.productionDate || dayjs().format('YYYY-MM-DD'),
    recordDate: data?.recordDate || data?.productionDate || dayjs().format('YYYY-MM-DD'),
    recorderName: data?.recorderName || currentUserName.value || '',
    recordStatus: data?.recordStatus || 'DRAFT',
  });
  const backendDetails = Array.isArray(data?.details) ? data.details : [];
  intermediateDetails.value = backendDetails.length > 0
    ? backendDetails.map((row, index) => ({
        ...row,
        samplePosition: row.samplePosition || `ROW-${row.sortNo || index + 1}`,
        samplePositionName: row.samplePositionName || row.samplePosition || `第${index + 1}行`,
        sortNo: row.sortNo || index + 1,
      }))
    : buildDefaultPressSlotIntermediateDetails();
  syncIntermediateSliceNoFieldsFromDetails();
  syncIntermediateCountersFromReports();
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
      recordDate: targetRecord?.recordDate || intermediateForm.recordDate || dayjs().format('YYYY-MM-DD'),
    });
    mapPressSlotIntermediateRecord(data);
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
  if (!ensureSegmentWritable('新建中间品')) return;
  if (!currentPlan.planId || !currentPlan.planOperationId) {
    message.warning('请先扫描或选择压槽计划');
    return;
  }
  const batchNo = resolveCurrentIntermediateBatchNo();
  if (!batchNo) {
    message.warning('当前计划缺少分段批号，不能新建压槽中间品记录表');
    return;
  }
  selectedIntermediateRecord.value = null;
  resetIntermediateForm();
  intermediateForm.batchNo = batchNo;
  intermediateForm.recordDate = dayjs().format('YYYY-MM-DD');
  intermediateForm.productionDate = intermediateForm.recordDate;
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
    activeRecord.value = null;
    return;
  }
  AModal.confirm({
    cancelText: '继续填写',
    content: '当前扫码确认尚未提交，关闭后本次外观检验和扫码确认信息会丢失，确认关闭？',
    okText: '确认关闭',
    okType: 'danger',
    title: '确认关闭报工登记',
    onOk: () => {
      reportVisible.value = false;
      activeRecord.value = null;
    },
  });
}

function clearReportIntermediateDraft() {
  // 压槽中间品已调整为按日业务表，不再使用报工弹窗内的临时草稿。
}

function validateIntermediateAuthAction(action: IntermediateAuthAction) {
  if (!ensureSegmentWritable(action === 'confirm' ? '确认中间品' : '保存中间品')) return false;
  if (!currentPlan.planId || !currentPlan.planOperationId) {
    message.warning('请先扫描或选择压槽计划');
    return false;
  }
  return true;
}

function requestSaveIntermediateRecord() {
  if (!validateIntermediateAuthAction('save')) return;
  pendingIntermediateAuthAction.value = 'save';
  intermediateAuthAction.value = '保存压槽中间品记录表';
  intermediateAuthVisible.value = true;
}

function requestConfirmIntermediateRecord() {
  if (!validateIntermediateAuthAction('confirm')) return;
  pendingIntermediateAuthAction.value = 'confirm';
  intermediateAuthAction.value = '确认压槽中间品记录表';
  intermediateAuthVisible.value = true;
}

async function submitIntermediateRecord(action: IntermediateAuthAction, authPayload: any) {
  if (!validateIntermediateAuthAction(action)) return;
  const operator = resolveOperationAuthOperator(authPayload);
  if (!operator) {
    message.warning('请先完成填写人/确认人身份认证');
    return;
  }
  const now = buildNowText();
  syncIntermediateDetailsFromSliceNoFields();
  syncIntermediateCountersFromReports();
  if (action === 'confirm') {
    intermediateForm.confirmerName = operator;
    intermediateForm.confirmTime = now;
    intermediateForm.recorderName = intermediateForm.recorderName || operator;
    intermediateForm.fillTime = intermediateForm.fillTime || now;
  } else {
    intermediateForm.recorderName = operator;
    intermediateForm.fillTime = now;
  }
  intermediateLoading.value = true;
  try {
    const payload: MesHcAdhesiveConsoleApi.IntermediateRecord = {
      ...intermediateForm,
      batchNo: intermediateForm.batchNo || resolveCurrentIntermediateBatchNo() || resolveIntermediateBatchNo(),
      details: intermediateDetails.value,
      planId: currentPlan.planId,
      planNo: currentPlan.planNo,
      planOperationId: currentPlan.planOperationId,
      recordDate: intermediateForm.recordDate || dayjs().format('YYYY-MM-DD'),
      recordStatus: action === 'confirm' ? 'CONFIRMED' : 'RECORDED',
    };
    if (!showIntermediateSlotDepthSection.value) {
      clearIntermediateSlotDepthPayload(payload as Record<string, any>);
    }
    const submitApi = action === 'confirm' ? confirmPressSlotIntermediate : saveAdhesiveConsoleIntermediate;
    const recordId = await submitApi({
      ...payload,
    });
    intermediateForm.id = Number((recordId as any)?.data ?? recordId ?? intermediateForm.id);
    intermediateForm.recordStatus = payload.recordStatus;
    selectedIntermediateRecord.value = {
      ...(selectedIntermediateRecord.value || {}),
      ...intermediateForm,
      id: intermediateForm.id,
    } as MesHcAdhesiveConsoleApi.IntermediateRecord;
    message.success(action === 'confirm' ? '压槽中间品记录单已确认' : '压槽中间品记录单已保存');
    await loadPressSlotMiddleLedger();
    await loadIntermediateRecord(selectedIntermediateRecord.value || undefined);
  } finally {
    intermediateLoading.value = false;
  }
}

async function handleIntermediateAuthSuccess(payload: any) {
  const pending = pendingIntermediateAuthAction.value;
  pendingIntermediateAuthAction.value = undefined;
  intermediateAuthVisible.value = false;
  if (!pending) {
    message.warning('未找到待提交的中间品记录，请重新点击保存或确认');
    return;
  }
  await submitIntermediateRecord(pending, payload);
}

function handleIntermediateAuthCancel() {
  pendingIntermediateAuthAction.value = undefined;
  intermediateAuthVisible.value = false;
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
  const planNo = syncPlanScanNo(value);
  const scannerInput = hasPlanScanDelimiter(value) || pendingScannerPlanNo === planNo;
  if (!planNo || (!scannerInput && !isPlanNoReadyForAutoScan(planNo)) || (!scannerInput && planNo === lastAutoScannedPlanNo.value)) return;
  planScanTimer = setTimeout(() => {
    if (planNo !== normalizePlanScanNo(scanPlanNo.value)) {
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
      equipmentCode: equipment.id ? undefined : equipment.code || undefined,
      equipmentId: equipment.id,
      taskStatus: 'ALL',
    }));
    ensureBoardEquipmentFromTasks(taskRows.value);
    taskListPage.value = 1;
  } finally {
    taskListLoading.value = false;
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
  const equipment = getEffectiveBoardEquipment();
  return taskRows.value.find((task: any) =>
    task?.planId &&
    task?.planOperationId &&
    normalizeWorkOrderStatus(task.status) !== 'FINISHED' &&
    isSameTaskEquipment(task, equipment),
  ) || taskRows.value.find((task: any) =>
    task?.planId &&
    task?.planOperationId &&
    normalizeWorkOrderStatus(task.status) !== 'FINISHED',
  );
}

async function loadEquipmentSelectRows() {
  equipmentSelectLoading.value = true;
  try {
    const keyword = equipmentSelectKeyword.value.trim();
    const workCenterId = currentPlan.workCenterId || boardEquipment.workCenterId;
    const workCenterName = currentPlan.workCenterName || boardEquipment.workCenterName;
    if (!workCenterId && !workCenterName) {
      equipmentSelectRows.value = [];
      message.warning('当前压槽任务未带出工作中心，不能展示全厂设备；请先维护压槽工序工作中心或机台。');
      return;
    }
    const baseParams: Record<string, any> = {
      pageNo: 1,
      pageSize: 200,
      status: 0,
      ...(workCenterId ? { workCenterId } : { workCenterName }),
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
    if (!rows.length) {
      message.warning('当前压槽工作中心下未找到可用设备，请检查设备台账的工作中心和启用状态。');
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
    message.warning('请先选择压槽设备，再填写清洁点检');
    await openEquipmentSelect();
    return;
  }
  await loadDailyRecords();
  dailyRecordListVisible.value = true;
}

function openReportRecordList() {
  if (!showExtendedBoardTabs.value) {
    message.info('请双击左上角压槽图标显示今日压槽报工记录。');
    return;
  }
  activeBoardTab.value = 'RECORDS';
}

async function selectBoardEquipment(equipment: MesHcEquipmentApi.Equipment) {
  applyBoardEquipment({
    code: equipment.equipmentCode || '',
    id: equipment.id,
    name: equipment.equipmentName || '',
    workCenterId: equipment.workCenterId,
    workCenterName: equipment.workCenterName || '',
  });
  boardEquipmentManualSelected.value = true;
  syncCurrentPlanEquipmentFromBoard(equipment);
  if (currentPlan.planId && currentPlan.planOperationId && isWorkOrderRunning.value) {
    await switchPressSlotConsoleWorkOrderEquipment({
      equipmentCode: boardEquipment.code,
      equipmentId: boardEquipment.id,
      equipmentName: boardEquipment.name,
      planId: currentPlan.planId,
      planOperationId: currentPlan.planOperationId,
    });
    await Promise.all([loadDailyRecords(), loadGlueBoardUsage()]);
  }
  await loadTaskList();
  await loadDailyRecords();
  equipmentSelectVisible.value = false;
  // message.success(`已切换压槽设备：${boardEquipment.code || boardEquipment.name || '-'}`);
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

async function startTaskFromList(task: MesHcAdhesiveConsoleApi.TaskItem) {
  if (!task?.planOperationId) return;
  const taskSourceBatchNo = resolveMotherBatchNo(task as Record<string, any>);
  if (
    normalizeWorkOrderStatus(task.status) !== 'FINISHED' &&
    !(await ensureSampleAbnormalUnlocked(
      buildSegmentChainSampleLockCandidates({
        motherBatchNo: taskSourceBatchNo,
        segmentBatchNo: taskSourceBatchNo,
      }),
      '加载压槽',
    ))
  ) {
    return;
  }
  scanPlanNo.value = task.planNo || '';
  if (!boardEquipmentManualSelected.value) {
    applyBoardEquipmentFromTask(task);
  }
  applyTask(task);
  const equipment = getEffectiveBoardEquipment(task);
  lastAutoScannedPlanNo.value = task.planNo || scanPlanNo.value.trim();
  const taskStatus = normalizeWorkOrderStatus(task.status);
  const readonlyFinishedTask = taskStatus === 'FINISHED';
  const readonlyBlockedTask = taskStatus === 'PAUSED' || taskStatus === 'CANCELLED';
  if (!readonlyFinishedTask && !readonlyBlockedTask && taskStatus !== 'RUNNING') {
    if (!equipment.id && !equipment.code) {
      message.warning('请先选择压槽设备，再执行开工');
      await openEquipmentSelect();
      return;
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
    syncCurrentPlanEquipmentFromBoard(equipment);
  }
  if (readonlyFinishedTask) {
    currentPlan.status = 'COMPLETED';
  }
  await Promise.all([loadDailyRecords(), loadReports(), loadGlueBoardUsage(), loadProcessParameters(), loadPressSlotMiddleLedger()]);
  await loadSourceGroups();
  await Promise.all([loadChangeoverInspections(), loadPressSlotFaiSummary(), loadProcessCheckFaiRows(), loadPressSlotAbnormalLockRows()]);
  await loadIntermediateRecord();
  taskListVisible.value = false;
  if (readonlyFinishedTask) {
    return;
  }
  if (readonlyBlockedTask) {
    return;
  }
  if (!dailyPreparationReady.value) {
    showDailyPreparationRequiredWarning();
    return;
  }
  // message.success('已开工加载压槽计划与可加工来源');
}

function handleWorkOrderCompleteTodo() {
  if (!currentPlan.planId || !currentPlan.planOperationId) {
    message.warning('请先扫码或从待加工列表选择压槽工单');
    return;
  }
  if (isWorkOrderFinished.value) {
    message.warning('当前压槽工单已完工');
    return;
  }
  if (!isWorkOrderRunning.value) {
    message.warning('当前压槽工单未开工，不能执行工单完工');
    return;
  }
  AModal.confirm({
    okText: '确认完工',
    title: '确认压槽工单完工',
    content: '执行后会标识当前压槽工单全部完成，后续不能再新增压槽报工记录。请确认所有报工记录已打印并扫码确认。',
    async onOk() {
      await completeAdhesiveConsoleWorkOrder({
        confirmerName: currentUserName.value || undefined,
        confirmerTime: buildNowText(),
        endTime: buildNowText(),
        planId: currentPlan.planId!,
        planOperationId: currentPlan.planOperationId!,
        recorderName: currentUserName.value || undefined,
        recorderTime: buildNowText(),
        remark: '压槽看板工单完工',
        reportDate: dayjs().format('YYYY-MM-DD'),
      });
      currentPlan.status = 'FINISHED';
      currentPlan.endTime = buildNowText();
      await Promise.all([loadReportsAndSourceGroups(), loadTaskList(), loadChangeoverInspections(), loadPressSlotFaiSummary(), loadProcessCheckFaiRows(), loadPressSlotAbnormalLockRows(), loadProcessParameters(), loadPressSlotMiddleLedger()]);
      await loadIntermediateRecord();
      message.success('压槽工单已完工');
    },
  });
}

function openSegmentCompleteDialog() {
  if (!currentPlan.planId || !currentPlan.planOperationId) {
    message.warning('请先扫码或从待加工列表选择压槽分段');
    return;
  }
  if (isWorkOrderFinished.value) {
    message.warning('当前压槽分段已完工');
    return;
  }
  if (!isWorkOrderRunning.value) {
    message.warning('当前压槽分段未开工，不能执行本段完工');
    return;
  }
  const sourceBatchNo = normalizePressSlotMiddleBatchNo(currentMotherSegmentBatchNo.value || resolveCurrentPlanSourceBatchNo());
  if (!sourceBatchNo) {
    message.warning('当前压槽分段缺少分段批号，不能执行本段完工');
    return;
  }
  pendingSegmentCompleteBatchNo.value = sourceBatchNo;
  segmentCompleteVisible.value = true;
}

async function confirmSegmentComplete(userInfo?: any) {
  if (segmentCompleteSubmitting.value) return;
  const sourceBatchNo = normalizePressSlotMiddleBatchNo(
    pendingSegmentCompleteBatchNo.value || currentMotherSegmentBatchNo.value || resolveCurrentPlanSourceBatchNo(),
  );
  if (!currentPlan.planId || !currentPlan.planOperationId || !sourceBatchNo) {
    message.warning('当前压槽分段信息不完整，不能执行本段完工');
    return;
  }
  const now = buildNowText();
  const confirmerName = userInfo?.empName || userInfo?.nickname || userInfo?.username || currentUserName.value || undefined;
  segmentCompleteSubmitting.value = true;
  try {
    await completePressSlotSegment({
      confirmerName,
      confirmerTime: now,
      endTime: now,
      planId: currentPlan.planId,
      planOperationId: currentPlan.planOperationId,
      recorderName: confirmerName,
      recorderTime: now,
      remark: '压槽本段完工',
      reportDate: dayjs().format('YYYY-MM-DD'),
      sourceBatchNo,
      sourceProductionBatchNo: sourceBatchNo,
    });
    currentPlan.status = 'COMPLETED';
    currentPlan.endTime = now;
    segmentCompleteVisible.value = false;
    pendingSegmentCompleteBatchNo.value = '';
    await Promise.all([loadReportsAndSourceGroups(), loadTaskList(), loadChangeoverInspections(), loadPressSlotFaiSummary(), loadProcessCheckFaiRows(), loadPressSlotAbnormalLockRows(), loadProcessParameters(), loadPressSlotMiddleLedger()]);
    await loadIntermediateRecord();
    message.success(`当前压槽分段已完工，认证人：${confirmerName || '-'}`);
  } catch (error: any) {
    message.error(getErrorMessage(error) || '本段完工失败');
  } finally {
    segmentCompleteSubmitting.value = false;
  }
}

function handleSegmentCompleteAuthCancel() {
  pendingSegmentCompleteBatchNo.value = '';
  segmentCompleteVisible.value = false;
}

function parseRecordExtra(record: MesHcAdhesiveConsoleApi.ReportItem | Record<string, any>) {
  try {
    const extra = record?.extraJson ? JSON.parse(record.extraJson) : {};
    return extra && typeof extra === 'object' ? extra : {};
  } catch {
    return {};
  }
}

function buildDefaultVisualItems(): VisualItem[] {
  return pressSlotVisualItemNames.map((itemName) => ({
    itemName,
    remark: '',
    result: 'OK',
  }));
}

function normalizeVisualResult(value: unknown): 'NG' | 'OK' {
  const text = String(value ?? '').trim().toUpperCase();
  if (['NG', 'N', 'FALSE', '0', 'ABNORMAL', 'FAIL', 'FAILED', '异常', '不合格'].includes(text)) return 'NG';
  return 'OK';
}

function normalizeVisualItems(source: unknown): VisualItem[] {
  let rows: any[] = [];
  if (typeof source === 'string') {
    try {
      const parsed = JSON.parse(source || '[]');
      rows = Array.isArray(parsed)
        ? parsed
        : parsed?.visualItems || parsed?.items || parsed?.details || parsed?.list || [];
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
  return pressSlotVisualItemNames.map((itemName) => {
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
  if (shouldShowVisualInspectionTab.value) return 'visual-inspection';
  const firstCategory = reportCheckCategories.value[0];
  if (firstCategory) return `check-${firstCategory}`;
  return '';
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
  if (normalized === 'SUBMITTED') return { color: 'green', text: '已过站' };
  if (normalized === 'CONFIRMED') return { color: 'blue', text: '已扫码确认' };
  return { color: 'orange', text: '草稿' };
}

function isAdhesiveRecordConfirmed(status?: string) {
  const normalized = String(status || '').toUpperCase();
  return normalized === 'CONFIRMED' || normalized === 'SUBMITTED';
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

function formatPressSlotTicketLength(value?: number) {
  return `${formatNumber(Number(value || 0))} m`;
}

async function buildPressSlotTransferTicketItem(
  record: MesHcAdhesiveConsoleApi.ReportItem,
  now: string,
  planDetail: Record<string, any>,
) {
  const planNo = record.planNo || currentPlan.planNo;
  const materialCode = planDetail?.materialCode || currentPlan.materialCode || record.materialCode || '-';
  const modelCode = planDetail?.modelCode || currentPlan.modelCode || record.modelCode || '-';
  const sourceBatchNo =
    record.sourceProductionBatchNo ||
    record.parentProductionBatchNo ||
    currentPlan.sourceProductionBatchNo ||
    currentPlan.sourceBatchNo ||
    '-';
  const pressSlotBatchNo = record.productionBatchNo || '-';
  const startTime = record.startTime || record.recorderTime || now;
  const endTime = record.endTime || record.confirmerTime || record.recorderTime || now;
  const recorderName = record.recorderName || record.confirmerName || currentUserName.value || '-';
  const meterAndLoss = `产出 ${formatPressSlotTicketLength(record.outputLength || record.inputLength)} / 损耗 ${formatPressSlotTicketLength(record.lossLength)}`;
  const pressRoller = [record.glueBoardMaterialCode, record.glueBoardBatchNo].filter(Boolean).join(' / ') || '-';
  const napSampleLength = formatPressSlotTicketLength(record.napSampleLength);
  const fallbackFields = [
    { label: '料号', value: materialCode },
    { label: '型号', value: modelCode },
    { label: '来源片号', value: sourceBatchNo },
    { label: '压槽片号', value: pressSlotBatchNo },
    { label: '米数/损耗', value: meterAndLoss },
    { label: 'NAP', value: napSampleLength },
    { label: '压辊', value: pressRoller },
    { label: '记录人', value: recorderName },
  ];
  const fields = await applyPrintFieldTemplate('PRESS_SLOT_TRANSFER', fallbackFields, {
    endTime,
    materialCode,
    meterAndLoss,
    modelCode,
    napSampleLength,
    planNo,
    pressRoller,
    pressSlotBatchNo,
    processName: '压槽',
    recorderName,
    sourceBatchNo,
    startTime,
  });
  return {
    endTime,
    fields,
    materialCode,
    modelCode,
    planNo,
    processName: '压槽',
    productionBatchNo: pressSlotBatchNo,
    qrTopText: pressSlotBatchNo,
    qrValue: buildTransferTicketQrValue(planNo, pressSlotBatchNo),
    recorderName,
    startTime,
    ticketId: record.id,
  };
}

async function buildPressSlotTransferTicketPayload(records: MesHcAdhesiveConsoleApi.ReportItem[], planDetail: Record<string, any>) {
  const now = buildNowText();
  const items = await Promise.all(records.map((record) => buildPressSlotTransferTicketItem(record, now, planDetail)));
  const commonPayload = {
    continueOnError: false,
    copies: 1,
    labelGapDots: 24,
    labelGapMm: 3,
    labelHeightMm: 50,
    labelWidthMm: 80,
    offsetXmm: 0,
    offsetYmm: 0,
    printerKey: 'pressSlot',
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

async function sendPressSlotTransferTicketsToPrintAgent(
  records: MesHcAdhesiveConsoleApi.ReportItem[],
  planDetail: Record<string, any>,
) {
  const payload = await buildPressSlotTransferTicketPayload(records, planDetail);
  const endpoint = records.length > 1 ? '/print/transfer-tickets' : '/print/transfer-ticket';
  const response = await fetch(`${PRESS_SLOT_PRINT_AGENT_URL}${endpoint}`, {
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

async function markPressSlotRecordsPrinted(records: MesHcAdhesiveConsoleApi.ReportItem[], printTime: string) {
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

async function printAdhesiveRecords(records: MesHcAdhesiveConsoleApi.ReportItem[]) {
  const validRows = records.filter((record) => !!record?.id && !!record.productionBatchNo);
  if (!validRows.length) {
    AModal.info({ title: '没有可打印记录', content: '请选择已有生产批次号的压槽报工记录。' });
    return false;
  }
  const now = buildNowText();
  const planDetail = currentPlan.planId ? await getPlanOrderDetail(currentPlan.planId as any) : {};
  try {
    const result = await sendPressSlotTransferTicketsToPrintAgent(validRows, planDetail as Record<string, any>);
    try {
      await markPressSlotRecordsPrinted(validRows, now);
      await loadReports();
      message.success(`压槽流转单已发送到 ${result.printerName || '配置打印机'}，数量：${result.printedCount || validRows.length}。`);
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

const printSelectedAdhesiveRecords = () => void printAdhesiveRecords(printableReportRecords.value);
const printAllAdhesiveRecords = () => void printAdhesiveRecords(unprintedReportRecords.value);

function openTransferPrintSelector() {
  selectedOneClickPressSlotBatchNo.value = '';
  selectedOneClickPressSlotSegmentKeys.value = [];
  transferPrintSelectionMode.value = true;
  activeBoardTab.value = 'SOURCE';
  visualMaximized.value = true;
  if (!selectableTransferReportCount.value) {
    // message.warning(reportRecords.value.length > 0 ? '当前过滤条件下暂无可打印压槽流转单' : '请先完成压槽报工后再选择打印流转单');
  }
}

function closeTransferPrintSelector() {
  transferPrintSelectionMode.value = false;
  selectedTransferReportIds.value = [];
}

function toggleTransferSegmentSelection(segment: SourceSegment) {
  const record = getPrintableTransferReportBySegment(segment);
  const key = getTransferReportKey(record);
  if (!record || !key) {
    message.warning('当前压槽片还未形成可打印报工记录');
    return;
  }
  selectedTransferReportIds.value = selectedTransferReportIds.value.includes(key)
    ? selectedTransferReportIds.value.filter((id) => id !== key)
    : [...selectedTransferReportIds.value, key];
}

function getPressSlotSelectionMarkIcon(
  group: SourceGroup,
  segment: SourceSegment,
) {
  if (transferPrintSelectionMode.value) {
    return isTransferSegmentSelected(segment)
      ? 'lucide:check'
      : 'lucide:square';
  }
  return isPressSlotOneClickGroupSelected(group) &&
    isPressSlotOneClickSegmentSelected(segment)
    ? 'lucide:check'
    : 'lucide:square';
}

function handleVisualSegmentClick(group: SourceGroup, segment: SourceSegment) {
  if (transferPrintSelectionMode.value) {
    toggleTransferSegmentSelection(segment);
    return;
  }
  if (isPressSlotOneClickGroupSelected(group)) {
    togglePressSlotOneClickSegmentSelection(group, segment);
    return;
  }
  openReportViewDialog(group, segment);
}

function clearTransferReportSelection() {
  selectedTransferReportIds.value = [];
}

function selectAllTransferReports() {
  selectedTransferReportIds.value = selectableTransferReports.value.map((record) => getTransferReportKey(record));
}

function toggleAllTransferReports() {
  if (allTransferReportsSelected.value) {
    const visibleKeys = new Set(selectableTransferReports.value.map((record) => getTransferReportKey(record)));
    selectedTransferReportIds.value = selectedTransferReportIds.value.filter((id) => !visibleKeys.has(id));
    return;
  }
  selectAllTransferReports();
}

function selectGroupTransferReports(group: SourceGroup) {
  const groupKeys = group.segments
    .map((segment) => getPrintableTransferReportBySegment(segment))
    .filter(isPrintableTransferReport)
    .map((record) => getTransferReportKey(record));
  selectedTransferReportIds.value = Array.from(new Set([...selectedTransferReportIds.value, ...groupKeys]));
}

function isPressSlotOneClickGroupSelected(group: SourceGroup) {
  return isSameBatchNo(
    group.baseBatchNo,
    selectedOneClickPressSlotBatchNo.value,
  );
}

function selectPressSlotOneClickGroup(group: SourceGroup) {
  if (isPressSlotOneClickGroupSelected(group)) {
    selectedOneClickPressSlotBatchNo.value = '';
    selectedOneClickPressSlotSegmentKeys.value = [];
    message.info('已退出选片模式。');
    return;
  }
  selectedOneClickPressSlotBatchNo.value = group.baseBatchNo;
  selectedOneClickPressSlotSegmentKeys.value = [];
  message.success(
    `已进入母批 ${group.baseBatchNo} 的选片模式，请点击需要一键报工的片号。`,
  );
}

function getPressSlotOneClickSegmentKey(segment: SourceSegment) {
  return String(segment.grindingSecondDetailId || segment.batchNo || '');
}

function isPressSlotOneClickSegmentSelected(segment: SourceSegment) {
  const key = getPressSlotOneClickSegmentKey(segment);
  return !!key && selectedOneClickPressSlotSegmentKeys.value.includes(key);
}

function togglePressSlotOneClickSegmentSelection(
  group: SourceGroup,
  segment: SourceSegment,
) {
  if (!isPressSlotOneClickGroupSelected(group)) return;
  if (!isPressSlotSegmentProcessable(segment)) {
    message.warning(
      '当前片号不可一键报工，请选择待确认且未被质量状态锁定的片号。',
    );
    return;
  }
  const key = getPressSlotOneClickSegmentKey(segment);
  if (!key) {
    message.warning('当前片号缺少来源标识，不能加入一键报工选择。');
    return;
  }
  selectedOneClickPressSlotSegmentKeys.value =
    isPressSlotOneClickSegmentSelected(segment)
      ? selectedOneClickPressSlotSegmentKeys.value.filter(
          (item) => item !== key,
        )
      : [...selectedOneClickPressSlotSegmentKeys.value, key];
}

function getPressSlotOneClickSelectedCount(group: SourceGroup) {
  return getPressSlotOneClickConfirmTargets(group).length;
}

function getPressSlotOneClickConfirmTargets(
  group: SourceGroup,
): PressSlotOneClickConfirmTarget[] {
  const sourceGroup = sourceGroups.value.find((item) =>
    isSameBatchNo(item.baseBatchNo, group.baseBatchNo),
  );
  if (!sourceGroup) return [];
  const selectedKeys = new Set(selectedOneClickPressSlotSegmentKeys.value);
  return sourceGroup.segments
    .filter(
      (segment) =>
        selectedKeys.has(getPressSlotOneClickSegmentKey(segment)) &&
        isPressSlotSegmentProcessable(segment) &&
        getPressSlotSegmentStatus(segment) === 'PENDING',
    )
    .map((segment) => ({
      group: sourceGroup,
      record: findReportBySegment(segment),
      segment,
    }));
}

function getPressSlotOneClickConfirmTitle(group: SourceGroup) {
  if (!isPressSlotOneClickGroupSelected(group)) return '请先选中本母批';
  const count = getPressSlotOneClickSelectedCount(group);
  return count ? `确认已选 ${count} 片` : '请点击待确认片号进行选择';
}

function buildPressSlotOneClickCheckItems() {
  return checkTemplate.value.map((item, index) => ({
    abnormalRemark: '',
    actualValue: getDefaultCheckActualValue(item),
    checkResult: 'OK',
    itemCategory: item.itemCategory,
    itemName: item.itemName,
    sortNo: Number(item.sortNo || index + 1),
    standardValue: item.standardValue,
  }));
}

function buildPressSlotOneClickCreatePayload(
  target: PressSlotOneClickConfirmTarget,
) {
  const sourceGrindingSecondDetailId = Number(
    target.segment.grindingSecondDetailId || 0,
  );
  if (!sourceGrindingSecondDetailId) {
    throw new Error(
      `${target.segment.batchNo} 未带出分切来源标识，不能自动创建压槽报工。`,
    );
  }
  if (!currentPlan.planId || !currentPlan.planOperationId) {
    throw new Error('当前压槽计划未加载，不能自动创建压槽报工。');
  }
  const range = findFirstAvailableRange(target.segment);
  const availableRangeLength = Number(range.length);
  const processLength =
    availableRangeLength > 0
      ? availableRangeLength
      : Number(target.segment.outputLength || 0);
  if (processLength <= 0) {
    throw new Error(
      `${target.segment.batchNo} 没有可报工的来源数量，不能自动创建压槽报工。`,
    );
  }
  const now = buildNowText();
  const reportType: PressSlotReportType = 'PRODUCT';
  const extra = applyPreProcessSelfCheckAttribution(
    {
      reportType,
      reportTypeName: getPressSlotReportTypeText(reportType),
      visualInspectionRemark: '',
      visualInspectionResult: 'OK',
      visualItems: normalizeVisualItems([]),
    },
    false,
    PRESS_SLOT_PRE_PROCESS_ATTRIBUTION,
  );
  return {
    aqcStatus: undefined,
    aqcTaskId: undefined,
    checkItems: buildPressSlotOneClickCheckItems(),
    confirmerName: currentUserName.value || 'admin',
    confirmerTime: now,
    defectCode: undefined,
    endPosition: Number(range.end),
    endTime: now,
    extraJson: JSON.stringify(extra),
    glueBoardBatchNo: glueBoard.batchNo || undefined,
    glueBoardMaterialCode: glueBoard.materialCode || undefined,
    glueBoardStartPosition: Number(glueBoard.availableStartPosition || 0),
    glueBoardUsageId: undefined,
    glueBoardUseLength: processLength,
    inputLength: processLength,
    lossLength: 0,
    materialCode: currentPlan.materialCode || undefined,
    modelCode: currentPlan.modelCode || undefined,
    napSampleLength: 0,
    outputLength: processLength,
    parentProductionBatchNo: target.group.baseBatchNo || undefined,
    planId: currentPlan.planId,
    planOperationId: currentPlan.planOperationId,
    productionBatchNo: target.segment.batchNo,
    recorderName: currentUserName.value || undefined,
    recorderTime: now,
    remark: undefined,
    reportDate: dayjs().format('YYYY-MM-DD'),
    scannedBatchNo: target.segment.batchNo,
    selfCheck: 'OK',
    sourceBatchNo: target.group.baseBatchNo || undefined,
    sourceGrindingSecondDetailId,
    sourceProductionBatchNo: target.segment.batchNo,
    sourceType: '扫码母料',
    startPosition: Number(range.start),
    startTime: now,
  };
}

async function refreshPressSlotOneClickConfirmData() {
  await Promise.all([
    loadReports(),
    loadGlueBoardUsage(),
    loadProcessParameters(),
    loadPressSlotFaiSummary(),
    loadPressSlotAbnormalLockRows(),
  ]);
  await loadSourceGroups();
  await loadIntermediateRecord();
}

function getPressSlotOneClickConsumableActionText() {
  if (glueBoard.alarm) return '清洗复位';
  if (bearingConsumable.alarm || !bearingConsumable.id) return '更换轴承';
  return '知道了';
}

async function handlePressSlotOneClickScanConfirm(group: SourceGroup) {
  if (oneClickScanConfirmingBatchNo.value) return;
  if (!isPressSlotOneClickGroupSelected(group)) {
    message.warning('请先点击“选中”，再执行一键扫码确认。');
    return;
  }
  if (!ensureSegmentWritable('一键扫码确认')) return;
  if (!isWorkOrderRunning.value) {
    AModal.warning({
      okText: '去待加工开工',
      title: '当前压槽工单未开工',
      content:
        '请先在右上角“待加工”中选择当前计划并执行开工，再进行一键扫码确认。',
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
  if (!pressSlotConsumablesReady.value) {
    AModal.warning({
      okText: getPressSlotOneClickConsumableActionText(),
      title: '压辊/轴承未就绪',
      content:
        glueBoard.alarm ||
        bearingConsumable.alarm ||
        '压槽报工前请先完成压槽辊清洗复位或轴承更换登记。',
      onOk: () => {
        if (glueBoard.alarm) {
          resetPressSlotRollerClean();
        } else if (bearingConsumable.alarm || !bearingConsumable.id) {
          openGlueConsumeDialog('BEARING');
        }
      },
    });
    return;
  }
  try {
    await loadCheckTemplate(currentPlan.modelCode);
  } catch {
    return;
  }
  const targets = getPressSlotOneClickConfirmTargets(group);
  if (targets.length === 0) {
    message.info('请先点击选择需要一键报工的待确认片号。');
    return;
  }
  const createCount = targets.filter((target) => !target.record?.id).length;
  const draftCount = targets.length - createCount;
  AModal.confirm({
    cancelText: '取消',
    content: `将对母批 ${group.baseBatchNo} 已选择的 ${targets.length} 片执行一键扫码报工：${draftCount} 片已有草稿直接确认，${createCount} 片将自动创建并确认。未填写的点检和外观项目按模板默认 OK 保存；没有默认实测值的字段保持为空。每片仍会执行首检/过程加检、日点检/清洁和耗材校验，任一片失败即停止后续确认。`,
    okText: `报工 ${targets.length} 片`,
    title: '压槽一键扫码报工',
    width: 680,
    onOk: async () => {
      oneClickScanConfirmingBatchNo.value = group.baseBatchNo;
      let confirmedCount = 0;
      let createdCount = 0;
      let failedBatchNo = '';
      let failureMessage = '';
      try {
        for (const target of targets) {
          const scanGatePassed = await validatePressSlotScanGate(
            target.group,
            target.segment,
            'PRODUCT',
            target.segment.batchNo,
            (messageText) => {
              failureMessage = messageText;
            },
          );
          if (!scanGatePassed) {
            failedBatchNo = target.segment.batchNo;
            break;
          }
          if (target.record?.id) {
            await confirmAdhesiveConsoleReport({
              confirmerName: currentUserName.value || 'admin',
              confirmerTime: buildNowText(),
              id: Number(target.record.id),
              scannedBatchNo: target.segment.batchNo,
            });
          } else {
            await saveAndConfirmPressSlotReport(
              buildPressSlotOneClickCreatePayload(target),
            );
            createdCount += 1;
          }
          confirmedCount += 1;
        }
      } catch (error) {
        failedBatchNo =
          failedBatchNo || targets[confirmedCount]?.segment.batchNo || '';
        failureMessage = getReportRequestErrorMessage(
          error,
          '压槽一键扫码报工失败，请检查现场状态后重试。',
        );
      } finally {
        try {
          await refreshPressSlotOneClickConfirmData();
        } catch (refreshError) {
          failureMessage =
            failureMessage ||
            getErrorMessage(refreshError) ||
            '确认结果已提交，但工作台回载失败，请手动刷新后核对。';
        }
        oneClickScanConfirmingBatchNo.value = '';
      }
      if (failedBatchNo || failureMessage) {
        AModal.warning({
          content: `已完成 ${confirmedCount} 片报工（其中自动创建 ${createdCount} 片）。${failedBatchNo ? `片号 ${failedBatchNo} 未报工：` : ''}${failureMessage || '报工流程已停止，请刷新后核对。'}`,
          title: '压槽一键扫码报工已停止',
        });
        return;
      }
      message.success(
        `母批 ${group.baseBatchNo} 已完成已选 ${confirmedCount} 片一键扫码报工，其中自动创建 ${createdCount} 片。`,
      );
    },
  });
}

async function handlePrintSelectedTransferReports() {
  if (!selectedTransferReportCount.value) {
    message.warning('请先选择要打印的压槽片');
    return;
  }
  const printed = await printAdhesiveRecords(selectedTransferReports.value);
  if (printed) closeTransferPrintSelector();
}

async function printChangeoverInspection(record?: MesHcAdhesiveConsoleApi.ChangeoverInspection | Record<string, any>) {
  const sourceRecord = (record || changeoverForm) as MesHcAdhesiveConsoleApi.ChangeoverInspection & Record<string, any>;
  if (!sourceRecord?.id) {
    message.warning('请先保存首检记录后再打印送检单');
    return;
  }
  const now = buildNowText();
  const checkItems = ((sourceRecord.checkItems || changeoverForm.checkItems || []) as AdhesiveCheckItem[]).map((item, index) => ({
    ...item,
    sortNo: item.sortNo || index + 1,
  }));
  if (!['PENDING_QMS', 'QUALIFIED', 'ABNORMAL'].includes(String(sourceRecord.inspectionStatus || ''))) {
    await savePressSlotChangeover({
      ...sourceRecord,
      checkItems,
      currentPlanNo: sourceRecord.currentPlanNo || currentPlan.planNo,
      detailItemsJson: JSON.stringify(checkItems),
      inspectionStatus: 'PENDING_QMS',
      motherSegmentBatchNo: sourceRecord.motherSegmentBatchNo || currentMotherSegmentBatchNo.value,
      planId: currentPlan.planId || sourceRecord.planId,
      planOperationId: currentPlan.planOperationId || sourceRecord.planOperationId,
      productionMaterialCode: currentPlan.materialCode || sourceRecord.productionMaterialCode,
      productionModelCode: currentPlan.modelCode || sourceRecord.productionModelCode,
      recordTime: normalizeDateTimeText(sourceRecord.recordTime) || now,
      submitTime: now,
    });
    sourceRecord.inspectionStatus = 'PENDING_QMS';
    sourceRecord.inspectionStatusName = '待检';
    sourceRecord.submitTime = now;
    changeoverForm.inspectionStatus = 'PENDING_QMS';
    changeoverForm.submitTime = now;
    await loadChangeoverInspections();
  }
  const printWindow = window.open('', '_blank', 'width=980,height=720');
  if (!printWindow) return;
  const qrValue = sourceRecord.pressSlotSliceNo
    || sourceRecord.motherSegmentBatchNo
    || currentMotherSegmentBatchNo.value
    || sourceRecord.currentPlanNo
    || currentPlan.planNo
    || '';
  const qrDataUrl = await createPrintQrDataUrl(qrValue);
  const checkRows = checkItems.map((item, index) => `
    <tr>
      <td>${index + 1}</td>
      <td>${item.itemCategory || ''}</td>
      <td>${item.itemName || ''}</td>
      <td>${item.standardValue || ''}</td>
      <td>${item.actualValue || ''}</td>
      <td>${item.abnormalRemark || ''}</td>
    </tr>
  `).join('');
  printWindow.document.write(`
    <html>
      <head>
        <title>压槽首检送检单</title>
        <style>
          body { font-family: "Microsoft YaHei", Arial, sans-serif; margin: 24px; color: #111827; }
          .title { text-align: center; font-size: 24px; font-weight: 700; margin-bottom: 18px; }
          .head { display: grid; grid-template-columns: repeat(3, 1fr) 120px; border: 1px solid #111827; border-bottom: 0; }
          .cell { min-height: 34px; padding: 8px 10px; border-right: 1px solid #111827; border-bottom: 1px solid #111827; font-size: 14px; }
          .cell strong { margin-right: 6px; }
          .qr { grid-row: span 4; display: flex; align-items: center; justify-content: center; border-right: 0; }
          .qr img { width: 96px; height: 96px; }
          table { width: 100%; border-collapse: collapse; margin-top: 14px; font-size: 13px; }
          th, td { border: 1px solid #111827; padding: 7px 6px; text-align: center; }
          th { background: #eef2f7; }
          .remark { margin-top: 14px; font-size: 13px; }
        </style>
      </head>
      <body>
        <div class="title">压槽首检送检单</div>
        <div class="head">
          <div class="cell"><strong>计划号</strong>${sourceRecord.currentPlanNo || sourceRecord.planNo || currentPlan.planNo || ''}</div>
          <div class="cell"><strong>压槽片号</strong>${sourceRecord.pressSlotSliceNo || ''}</div>
          <div class="cell"><strong>母片/来源片号</strong>${sourceRecord.motherSegmentBatchNo || currentMotherSegmentBatchNo.value || ''}</div>
          <div class="cell qr">${qrDataUrl ? `<img src="${qrDataUrl}" alt="压槽首检二维码" />` : ''}</div>
          <div class="cell"><strong>生产型号</strong>${sourceRecord.productionModelCode || currentPlan.modelCode || ''}</div>
          <div class="cell"><strong>当前料号</strong>${sourceRecord.productionMaterialCode || currentPlan.materialCode || ''}</div>
          <div class="cell"><strong>设备</strong>${currentPlan.equipmentCode || ''} ${currentPlan.equipmentName || ''}</div>
          <div class="cell"><strong>检验人</strong>${sourceRecord.recorderName || currentUserName.value || ''}</div>
          <div class="cell"><strong>记录时间</strong>${displayDateTimeText(sourceRecord.recordTime || sourceRecord.submitTime)}</div>
          <div class="cell"><strong>反馈时间</strong>${displayDateTimeText(sourceRecord.feedbackTime)}</div>
        </div>
        <table>
          <thead>
            <tr><th>序号</th><th>类别</th><th>项目</th><th>标准</th><th>实测值</th><th>异常备注</th></tr>
          </thead>
          <tbody>${checkRows || '<tr><td colspan="6">暂无明细</td></tr>'}</tbody>
        </table>
        <div class="remark">备注：${sourceRecord.remark || ''}</div>
      </body>
    </html>
  `);
  printWindow.document.close();
  printWindow.focus();
  printWindow.print();
}

function openRecordConfirm(record?: MesHcAdhesiveConsoleApi.ReportItem | Record<string, any>) {
  activeRecord.value = (record as MesHcAdhesiveConsoleApi.ReportItem) || null;
  recordConfirmForm.error = '';
  recordConfirmForm.message = '';
  recordConfirmForm.reportType = 'PRODUCT';
  recordConfirmForm.scannedBatchNo = '';
  recordConfirmVisible.value = true;
  focusRecordConfirmScanInput();
}

function openFirstInspectionScanDialog(mode: FirstInspectionScanMode = 'FIRST_INSPECTION') {
  firstInspectionScanMode.value = mode;
  firstInspectionScanNo.value = '';
  firstInspectionScanError.value = '';
  firstInspectionSource.value = null;
  processCheckScanReport.value = null;
  firstInspectionScanVisible.value = true;
  focusFirstInspectionScanInput();
}

function requestFirstInspectionApplication() {
  if (!ensureSegmentWritable('提交首检申请')) return;
  if (!currentPlan.planId || !currentPlan.planOperationId) {
    AModal.warning({ title: '请先加载压槽计划', content: '首检申请需要先扫码或从待加工列表带出当前压槽计划。' });
    return;
  }
  if (!isWorkOrderRunning.value) {
    AModal.warning({ title: '请先执行压槽开工', content: '压槽工序必须先开工并锁定设备后才能提交首检申请。' });
    return;
  }
  openFirstInspectionScanDialog();
}

function requestCumulativeProcessCheckApplication() {
  if (!ensureSegmentWritable('提交过程加检')) return;
  if (!currentPlan.planId || !currentPlan.planOperationId) {
    AModal.warning({ title: '请先加载压槽计划', content: '过程加检需要先扫码或从待加工列表带出当前压槽计划。' });
    return;
  }
  if (!isWorkOrderRunning.value) {
    AModal.warning({ title: '请先执行压槽开工', content: '压槽工序必须先开工并锁定设备后才能提交过程加检。' });
    return;
  }
  openFirstInspectionScanDialog('PROCESS_CHECK');
}

function requestAbnormalReleaseApplication() {
  if (!ensureSegmentWritable('提交异常放行送检')) return;
  if (!currentPlan.planId || !currentPlan.planOperationId) {
    AModal.warning({ title: '请先加载压槽计划', content: '异常放行送检需要先扫码或从待加工列表带出当前压槽计划。' });
    return;
  }
  if (!isWorkOrderRunning.value) {
    AModal.warning({ title: '请先执行压槽开工', content: '压槽工序必须先开工并锁定设备后才能提交异常放行送检。' });
    return;
  }
  if (!hasActivePressSlotAbnormalLock.value) {
    AModal.info({ title: '暂无异常锁定', content: '当前母批没有活动中的加检NG异常锁定记录，不需要提交异常放行送检。' });
    return;
  }
  openFirstInspectionScanDialog('ABNORMAL_RELEASE');
}

function handleFirstInspectionStampClick() {
  if (canSubmitFirstInspection()) {
    requestFirstInspectionApplication();
    return;
  }
  if (firstInspection.value?.faiId) {
    viewFirstInspectionDetail();
  }
}

async function confirmFirstInspectionScanAndSubmit() {
  const scanned = ['ABNORMAL_RELEASE', 'PROCESS_CHECK'].includes(firstInspectionScanMode.value)
    ? await scanProcessCheckSlice(false)
    : await scanFirstInspectionSlice(false);
  if (!scanned) return;
  const submitted = firstInspectionScanMode.value === 'ABNORMAL_RELEASE'
    ? await submitAbnormalReleaseApplication()
    : firstInspectionScanMode.value === 'PROCESS_CHECK'
      ? await submitCumulativeProcessCheckApplication()
      : await submitFirstInspectionApplication();
  if (submitted) {
    firstInspectionScanVisible.value = false;
  }
}

async function submitFirstInspectionApplication() {
  if (!ensureSegmentWritable('提交首检申请')) return false;
  if (!currentPlan.planId || !currentPlan.planOperationId) {
    AModal.warning({ title: '请先加载压槽计划', content: '首检申请需要先扫码或从待加工列表带出当前压槽计划。' });
    return false;
  }
  if (!isWorkOrderRunning.value) {
    AModal.warning({ title: '请先执行压槽开工', content: '压槽工序必须先开工并锁定设备后才能提交首检申请。' });
    return false;
  }
  if (!currentFirstInspectionSliceNo.value || !firstInspectionSource.value) {
    firstInspectionScanError.value = '请先在首检扫码弹窗中扫描批号';
    firstInspectionScanVisible.value = true;
    focusFirstInspectionScanInput();
    return false;
  }
  const processCheckNgRestart = requiresRestartFirstInspection.value;
  const additionalFirstInspection = !processCheckNgRestart
    && [
      firstInspection.value,
      ...firstInspectionFaiRows.value,
    ].some((record) => record?.faiId && String(record.faiStatus || '').toUpperCase() !== 'CANCELED');
  const inspectionTypeName = processCheckNgRestart ? '重新首检' : additionalFirstInspection ? '加检' : '首检';
  firstInspectionApplying.value = true;
  try {
    const productBatchNo = currentFirstInspectionSliceNo.value;
    const summary = await applyPressSlotFai({
      planId: currentPlan.planId,
      planOperationId: currentPlan.planOperationId,
      remark: processCheckNgRestart
        ? '压槽过程加检NG后重新首检申请'
        : additionalFirstInspection
          ? '压槽生产重新送样加检申请'
          : '压槽操作看板首检申请',
      productBatchNo,
      standardMatchMode: 'PRODUCT_MODEL_PROCESS',
      submitterName: currentUserName.value || undefined,
      triggerReason: processCheckNgRestart
        ? PRESS_SLOT_FAI_TRIGGER_PROCESS_CHECK_NG_RESTART
        : additionalFirstInspection
          ? PRESS_SLOT_FAI_TRIGGER_ADDITIONAL_FIRST_INSPECTION
          : undefined,
    });
    const summaryWithBatch = { ...summary, productBatchNo };
    firstInspection.value = summaryWithBatch;
    await loadPressSlotFaiSummary();
    await Promise.all([loadTaskList(), loadReportsAndSourceGroups()]);
    confirmPrintPressSlotInspectionTransferTicket(summaryWithBatch, {
      inspectionType: inspectionTypeName,
      sampleType: '压槽首检片',
      successContent: `${inspectionTypeName}已送检，已生成首件检验单 ${summary.faiNo || '-'}；无需等待检验结果，可继续报工。是否立即打印首检检验流转单？`,
    });
    return true;
  } catch (error: any) {
    AModal.warning({
      content: getErrorMessage(error) || '提交首检申请失败，请检查压槽开工、设备锁定和 FAI 标准配置。',
      title: '提交失败',
    });
    return false;
  } finally {
    firstInspectionApplying.value = false;
  }
}

function canWithdrawPressSlotFai(record?: MesHcAdhesiveConsoleApi.FaiSummary | Record<string, any>) {
  return !!record?.faiId && String(record.faiStatus || '').toUpperCase() === 'PENDING';
}

function requestPressSlotFaiWithdrawal(
  record: MesHcAdhesiveConsoleApi.FaiSummary | Record<string, any>,
  inspectionType: '首检' | '过程加检',
) {
  const faiId = Number(record?.faiId || 0);
  if (!faiId || !currentPlan.planId || !currentPlan.planOperationId) {
    message.warning('未找到当前压槽计划或送检单，无法撤回');
    return;
  }
  const pieceNo = String(record?.productBatchNo || '').trim() || '-';
  AModal.confirm({
    cancelText: '取消',
    content: `将撤回${inspectionType}送检单 ${record?.faiNo || '-'}（片号：${pieceNo}）。仅品质未扫码、未录入、未提交的待检单可以撤回；首检撤回后该片号会恢复为可送检。确认撤回？`,
    okButtonProps: { danger: true },
    okText: '确认撤回',
    title: `撤回${inspectionType}送检单`,
    async onOk() {
      withdrawingFaiId.value = faiId;
      try {
        await withdrawPressSlotFai({
          faiId,
          planId: currentPlan.planId!,
          planOperationId: currentPlan.planOperationId!,
          withdrawReason: `压槽看板人工撤回${inspectionType}送检单`,
        });
        await Promise.all([
          loadPressSlotFaiSummary(),
          loadProcessCheckFaiRows(),
          loadReportsAndSourceGroups(),
          loadTaskList(),
        ]);
        message.success(`${inspectionType}送检单已撤回${inspectionType === '首检' ? '，片号已释放' : '，已保留压槽报工记录'}`);
      } catch (error: any) {
        message.warning(getErrorMessage(error) || `${inspectionType}送检单撤回失败`);
        throw error;
      } finally {
        withdrawingFaiId.value = null;
      }
    },
  });
}

function findPressSlotReportByBatchNo(batchNo?: string) {
  const normalized = String(batchNo || '').trim().toUpperCase();
  if (!normalized) return undefined;
  return reportRecords.value
    .filter((record) => [
      record.productionBatchNo,
      record.sourceProductionBatchNo,
      record.sourceBatchNo,
      (record as Record<string, any>).confirmedBatchNo,
    ].some((candidate) => String(candidate || '').trim().toUpperCase() === normalized))
    .sort((a, b) => Number(b.id || 0) - Number(a.id || 0))[0];
}

async function scanProcessCheckSlice(showSuccess: Event | boolean = true) {
  const shouldShowSuccess = showSuccess !== false;
  const isAbnormalRelease = firstInspectionScanMode.value === 'ABNORMAL_RELEASE';
  const scanLabel = isAbnormalRelease ? '风险复测片号' : '加检片号';
  const normalizedScanNo = resolveTransferTicketQrBusinessNo(normalizeConfirmScanCode(firstInspectionScanNo.value));
  const batchNo = normalizedScanNo || firstInspectionScanNo.value.trim();
  firstInspectionScanError.value = '';
  if (!batchNo) {
    firstInspectionScanError.value = `请先扫码或输入需要${isAbnormalRelease ? '风险片槽深复测' : '过程加检'}的压槽片号`;
    focusFirstInspectionScanInput();
    return false;
  }
  firstInspectionScanNo.value = batchNo;
  firstInspectionScanLoading.value = true;
  try {
    await loadReports();
    const report = findPressSlotReportByBatchNo(batchNo);
    if (!report?.id) {
      if (isAbnormalRelease) {
        processCheckScanReport.value = null;
        firstInspectionScanError.value = '未找到该片号的压槽扫码确认记录，请选择冻结风险区间内的锁定片号';
        return false;
      }
      const inferredMotherBatchNo = normalizePressSlotMiddleBatchNo(resolveMotherSegmentBatchNo(batchNo));
      const currentMotherBatchNo = currentMotherSegmentBatchNo.value;
      if (currentMotherBatchNo && inferredMotherBatchNo && inferredMotherBatchNo !== currentMotherBatchNo) {
        processCheckScanReport.value = null;
        firstInspectionScanError.value = `${scanLabel}不属于当前分段批号，请重新扫码`;
        return false;
      }
      processCheckScanReport.value = {
        materialCode: currentPlan.materialCode,
        modelCode: currentPlan.modelCode,
        parentProductionBatchNo: currentMotherBatchNo || inferredMotherBatchNo,
        planId: currentPlan.planId || 0,
        planNo: currentPlan.planNo,
        planOperationId: currentPlan.planOperationId || 0,
        productionBatchNo: batchNo,
        recorderName: currentUserName.value || undefined,
        recorderTime: dayjs().format('YYYY-MM-DD HH:mm:ss'),
        remark: isAbnormalRelease ? '风险片槽深复测扫码带入' : '过程加检扫码后台补建',
        reportDate: currentDateText.value,
        reportStatus: 'CONFIRMED',
        selfCheck: 'OK',
        sourceBatchNo: currentMotherBatchNo || inferredMotherBatchNo,
        sourceProductionBatchNo: batchNo,
      } as MesHcAdhesiveConsoleApi.ReportItem;
      if (shouldShowSuccess) {
        message.success(isAbnormalRelease ? '已带入风险复测压槽片' : '已带入过程加检压槽片，提交时将后台补建报工记录');
      }
      return true;
    }
    if (!isAdhesiveRecordConfirmed(report.reportStatus)) {
      processCheckScanReport.value = null;
      firstInspectionScanError.value = `该压槽片尚未扫码确认，请先确认后再提交${isAbnormalRelease ? '风险片槽深复测' : '过程加检'}`;
      return false;
    }
    const reportMotherBatchNo = normalizePressSlotMiddleBatchNo(
      report.parentProductionBatchNo || report.sourceProductionBatchNo || report.sourceBatchNo || report.productionBatchNo || '',
    );
    const currentMotherBatchNo = currentMotherSegmentBatchNo.value;
    if (currentMotherBatchNo && reportMotherBatchNo && reportMotherBatchNo !== currentMotherBatchNo) {
      processCheckScanReport.value = null;
      firstInspectionScanError.value = `${scanLabel}不属于当前分段批号，请重新扫码`;
      return false;
    }
    processCheckScanReport.value = report;
    firstInspectionScanNo.value = report.productionBatchNo || batchNo;
    if (shouldShowSuccess) {
      message.success(isAbnormalRelease ? '已带入风险复测压槽片' : '已带入过程加检压槽片');
    }
    return true;
  } catch (error) {
    processCheckScanReport.value = null;
    firstInspectionScanError.value = getErrorMessage(error) || `${scanLabel}校验失败，请检查扫码内容`;
    return false;
  } finally {
    firstInspectionScanLoading.value = false;
  }
}

function buildCumulativeProcessCheckRemark(report?: Partial<MesHcAdhesiveConsoleApi.ReportItem> | null) {
  return [
    '压槽过程加检',
    `分段批号 ${currentMotherSegmentBatchNo.value || report?.parentProductionBatchNo || '-'}`,
    `压槽片号 ${report?.productionBatchNo || firstInspectionScanNo.value || '-'}`,
    `报工ID ${report?.id || '后台补建'}`,
  ].join('；');
}

async function submitCumulativeProcessCheckApplication() {
  if (!ensureSegmentWritable('提交过程加检')) return false;
  if (!currentPlan.planId || !currentPlan.planOperationId) {
    AModal.warning({ title: '请先加载压槽计划', content: '过程加检需要先扫码或从待加工列表带出当前压槽计划。' });
    return false;
  }
  if (!isWorkOrderRunning.value) {
    AModal.warning({ title: '请先执行压槽开工', content: '压槽工序必须先开工并锁定设备后才能提交过程加检。' });
    return false;
  }
  const report = processCheckScanReport.value;
  const productBatchNo = String(report?.productionBatchNo || firstInspectionScanNo.value || '').trim();
  if (!productBatchNo) {
    firstInspectionScanError.value = '请先扫描需要过程加检的压槽片号';
    firstInspectionScanVisible.value = true;
    focusFirstInspectionScanInput();
    return false;
  }
  processCheckApplying.value = true;
  try {
    const sourceReportId = Number(report?.id || 0) || undefined;
    const summary = await applyPressSlotFai({
      inspectionScene: 'PROCESS_CHECK',
      inspectionScopeBatchNo: currentMotherSegmentBatchNo.value || report?.parentProductionBatchNo || undefined,
      planId: currentPlan.planId,
      planOperationId: currentPlan.planOperationId,
      productBatchNo,
      remark: buildCumulativeProcessCheckRemark(report),
      sourceReportId,
      sourceReportNo: productBatchNo,
      standardMatchMode: 'PRODUCT_MODEL_PROCESS',
      submitterName: currentUserName.value || undefined,
      triggerReason: 'NEW_ORDER',
    });
    await Promise.all([loadProcessCheckFaiRows(), loadPressSlotAbnormalLockRows(), loadReportsAndSourceGroups(), loadProcessParameters()]);
    activeBoardTab.value = 'PROCESS_CHECK';
    message.success(`过程加检已送检，FAI单号：${summary.faiNo || '-'}；无需等待检验结果，可继续报工。`);
    confirmPrintPressSlotInspectionTransferTicket(summary, {
      inspectionType: '过程加检',
      sampleType: '压槽过程加检片',
      successContent: `过程加检已提交，已生成过程检验单 ${summary.faiNo || '-'}。是否立即打印检验流转单？`,
    });
    return true;
  } catch (error: any) {
    AModal.warning({
      content: getErrorMessage(error) || '过程加检提交失败，请检查当前计划、压槽片号和 FAI 检验标准。',
      title: '加检提交失败',
    });
    return false;
  } finally {
    processCheckApplying.value = false;
  }
}

function buildAbnormalReleaseRemark(report?: Partial<MesHcAdhesiveConsoleApi.ReportItem> | null) {
  const activeLock = activeAbnormalLockRecord.value;
  return [
    '压槽风险片槽深复测',
    `分段批号 ${currentMotherSegmentBatchNo.value || report?.parentProductionBatchNo || '-'}`,
    `风险复测片号 ${report?.productionBatchNo || firstInspectionScanNo.value || '-'}`,
    `异常加检片号 ${activeLock?.abnormalSampleBatchNo || '-'}`,
    `锁定起点 ${activeLock?.lockStartTimeText || activeLock?.abnormalSubmitTimeText || '-'}`,
    `报工ID ${report?.id || '-'}`,
  ].join('；');
}

async function submitAbnormalReleaseApplication() {
  if (!ensureSegmentWritable('提交风险片槽深复测')) return false;
  if (!currentPlan.planId || !currentPlan.planOperationId) {
    AModal.warning({ title: '请先加载压槽计划', content: '风险片槽深复测需要先扫码或从待加工列表带出当前压槽计划。' });
    return false;
  }
  if (!isWorkOrderRunning.value) {
    AModal.warning({ title: '请先执行压槽开工', content: '压槽工序必须先开工并锁定设备后才能提交风险片槽深复测。' });
    return false;
  }
  if (!hasActivePressSlotAbnormalLock.value) {
    firstInspectionScanError.value = '当前母批暂无加检NG风险锁定记录';
    return false;
  }
  const report = processCheckScanReport.value;
  const productBatchNo = String(report?.productionBatchNo || firstInspectionScanNo.value || '').trim();
  if (!productBatchNo) {
    firstInspectionScanError.value = '请先扫描需要槽深复测的风险片号';
    firstInspectionScanVisible.value = true;
    focusFirstInspectionScanInput();
    return false;
  }
  processCheckApplying.value = true;
  try {
    const sourceReportId = Number(report?.id || 0) || undefined;
    const summary = await applyPressSlotFai({
      inspectionScene: 'ABNORMAL_RELEASE',
      inspectionScopeBatchNo: currentMotherSegmentBatchNo.value || report?.parentProductionBatchNo || undefined,
      planId: currentPlan.planId,
      planOperationId: currentPlan.planOperationId,
      productBatchNo,
      remark: buildAbnormalReleaseRemark(report),
      sourceReportId,
      sourceReportNo: productBatchNo,
      standardMatchMode: 'PRODUCT_MODEL_PROCESS',
      submitterName: currentUserName.value || undefined,
      triggerReason: 'NEW_ORDER',
    });
    await Promise.all([loadPressSlotAbnormalLockRows(), loadProcessCheckFaiRows(), loadReportsAndSourceGroups(), loadProcessParameters()]);
    activeBoardTab.value = 'ABNORMAL_LOCK';
    message.success(`风险片槽深复测已送检，FAI单号：${summary.faiNo || '-'}；无需等待检验结果，可继续报工。`);
    confirmPrintPressSlotInspectionTransferTicket(summary, {
      inspectionType: '风险片槽深复测',
      sampleType: '压槽风险复测片',
      successContent: `风险片槽深复测已提交，已生成检验单 ${summary.faiNo || '-'}。是否立即打印检验流转单？`,
    });
    return true;
  } catch (error: any) {
    AModal.warning({
      content: getErrorMessage(error) || '风险片槽深复测提交失败，请检查当前计划、锁定片号和 FAI 检验标准。',
      title: '风险复测提交失败',
    });
    return false;
  } finally {
    processCheckApplying.value = false;
  }
}

function getErrorMessage(error: unknown) {
  const data = (error as any)?.response?.data || (error as any)?.data || {};
  return String(data.msg || data.message || (error as any)?.msg || (error as any)?.message || '');
}

const pressSlotTemplateConfigErrorShownMessages = new Set<string>();

function showPressSlotTemplateConfigError(error: unknown, fallback: string) {
  const messageText = getErrorMessage(error) || fallback;
  if (!messageText.includes('模板配置没有找到配置')) {
    return false;
  }
  if (!pressSlotTemplateConfigErrorShownMessages.has(messageText)) {
    pressSlotTemplateConfigErrorShownMessages.add(messageText);
    AModal.error({ title: '模板配置缺失', content: messageText });
  }
  return true;
}

function viewFirstInspectionDetail(record: MesHcAdhesiveConsoleApi.FaiSummary = firstInspection.value) {
  if (!record?.faiId) {
    message.warning('暂无 FAI 首检单');
    return;
  }
  faiDetailModalApi.setData({ id: record.faiId }).open();
}

function viewProcessCheckFaiDetail(record: MesHcAdhesiveConsoleApi.FaiSummary) {
  if (!record?.faiId) {
    message.warning('暂无过程加检单');
    return;
  }
  faiDetailModalApi.setData({ id: record.faiId }).open();
}

function viewAbnormalLockInspectionDetail(
  record: MesHcAdhesiveConsoleApi.PressSlotAbnormalLockRecord,
  type: 'abnormal' | 'release',
) {
  const faiId = type === 'abnormal' ? record.abnormalFaiId : record.releaseFaiId;
  if (!faiId) {
    message.warning(type === 'abnormal' ? '暂无异常送检单' : '暂无复检送检单');
    return;
  }
  faiDetailModalApi.setData({ id: faiId }).open();
}

const [FaiDetailPreviewModal, faiDetailModalApi] = useVbenModal({
  connectedComponent: FaiDetailModal,
});

function resolvePressSlotInspectionNo(record?: MesHcAdhesiveConsoleApi.FaiSummary | Record<string, any> | null) {
  return String(record?.faiNo || record?.sourceReportNo || record?.faiId || '').trim();
}

async function buildPressSlotInspectionTransferTicketPayload(
  record: MesHcAdhesiveConsoleApi.FaiSummary | Record<string, any>,
  options: PressSlotInspectionTransferPrintOptions,
) {
  const inspectionNo = resolvePressSlotInspectionNo(record);
  const isFirstInspectionTransfer = String(options.inspectionType || '').includes('首检');
  const productBatchNo = record.productBatchNo
    || (isFirstInspectionTransfer ? currentFirstInspectionSliceNo.value : reportForm.productionBatchNo)
    || record.inspectionScopeBatchNo
    || currentMotherSegmentBatchNo.value
    || currentPlan.batchNo;
  const applyTime = record.faiApplyTime || buildNowText();
  const fallbackFields = [
    { label: '工序', value: '压槽' },
    { label: '送检类型', value: options.inspectionType },
    { label: '产品型号', value: currentPlan.modelCode },
    { label: '产品批次', value: productBatchNo },
    { label: '送检时间', value: applyTime },
    { label: '送检数量', value: '1片' },
  ];
  const fields = await applyPrintFieldTemplate('PRESS_SLOT_INSPECTION', fallbackFields, {
    applyTime,
    inspectionNo,
    inspectionType: options.inspectionType,
    modelCode: currentPlan.modelCode,
    processName: '压槽',
    productBatchNo,
    sampleQty: '1片',
  });
  const payload = buildInspectionTransferTicketPayload({
    applyTime,
    fields,
    inspectionNo,
    inspectionTransferNo: inspectionNo,
    inspectionType: options.inspectionType,
    materialCode: currentPlan.materialCode,
    modelCode: currentPlan.modelCode,
    planNo: currentPlan.planNo,
    processName: '压槽',
    productionBatchNo: productBatchNo,
    sampleType: options.sampleType,
    title: '检验流转单',
  });
  return {
    ...payload,
    qrTopText: inspectionNo,
    qrValue: productBatchNo,
    transferTicketNo: inspectionNo,
  };
}

async function printPressSlotInspectionTransferTicket(
  record?: MesHcAdhesiveConsoleApi.FaiSummary | Record<string, any> | null,
  options: PressSlotInspectionTransferPrintOptions = { inspectionType: 'FAI首检', sampleType: '压槽首检片' },
) {
  const inspectionNo = resolvePressSlotInspectionNo(record);
  if (!record || !inspectionNo) {
    AModal.warning({
      content: '当前检验记录还没有检验单号，无法生成检验流转单二维码。请先完成送检后再打印。',
      title: '无法打印检验流转单',
    });
    return;
  }
  try {
    const result = await sendTransferTicketToPrintAgent(
      await buildPressSlotInspectionTransferTicketPayload(record, options),
      PRESS_SLOT_PRINT_AGENT_URL,
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

function confirmPrintPressSlotInspectionTransferTicket(
  record: MesHcAdhesiveConsoleApi.FaiSummary | Record<string, any>,
  options: PressSlotInspectionTransferPrintOptions,
) {
  const inspectionNo = resolvePressSlotInspectionNo(record);
  if (!inspectionNo) return;
  AModal.confirm({
    cancelText: '暂不打印',
    content: options.successContent || `${options.inspectionType}已生成，检验单号：${inspectionNo}。是否立即打印检验流转单？`,
    okText: '打印',
    onOk: () => printPressSlotInspectionTransferTicket(record, options),
    title: '打印检验流转单',
  });
}

function printFirstInspectionTransferTicket(record: MesHcAdhesiveConsoleApi.FaiSummary | Record<string, any> = firstInspection.value) {
  return printPressSlotInspectionTransferTicket(record, {
    inspectionType: '首检',
    sampleType: '压槽首检片',
  });
}

function printProcessCheckInspectionTransferTicket(record: MesHcAdhesiveConsoleApi.FaiSummary | Record<string, any>) {
  return printPressSlotInspectionTransferTicket(record, {
    inspectionType: '过程加检',
    sampleType: '压槽过程加检片',
  });
}

function printLatestProcessCheckInspectionTransferTicket() {
  return printProcessCheckInspectionTransferTicket(latestProcessCheckInspection.value || {});
}

async function printFirstInspectionSummary() {
  const summary = firstInspection.value;
  if (!summary?.faiId) {
    message.warning('请先提交 FAI 首检申请后再打印送检单');
    return;
  }
  const printWindow = window.open('', '_blank', 'width=980,height=720');
  if (!printWindow) return;
  const qrDataUrl = await createPrintQrDataUrl(summary.faiNo || currentPlan.planNo);
  const statusText = summary.displayText || resolveFirstInspectionDisplayText(summary.faiStatus, summary.faiJudgment);
  printWindow.document.write(`
    <html>
      <head>
        <title>压槽首检送检单</title>
        <style>
          body { font-family: "Microsoft YaHei", Arial, sans-serif; margin: 24px; color: #111827; }
          .header { display: flex; justify-content: space-between; align-items: flex-start; border-bottom: 2px solid #111827; padding-bottom: 12px; }
          .title { font-size: 24px; font-weight: 800; }
          .qr { width: 96px; height: 96px; border: 1px solid #d1d5db; display: flex; align-items: center; justify-content: center; }
          .grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 8px 16px; margin-top: 18px; }
          .cell { border-bottom: 1px solid #e5e7eb; padding: 8px 0; font-size: 14px; }
          .cell strong { display: inline-block; min-width: 110px; color: #4b5563; }
          .remark { margin-top: 18px; padding: 12px; border: 1px solid #d1d5db; min-height: 68px; }
          .footer { margin-top: 28px; display: flex; justify-content: space-between; font-size: 14px; }
        </style>
      </head>
      <body>
        <div class="header">
          <div>
            <div class="title">压槽首检送检单</div>
            <div>首检单号：${summary.faiNo || '-'}</div>
          </div>
          <div class="qr">${qrDataUrl ? `<img src="${qrDataUrl}" width="96" height="96" alt="首检单号二维码" />` : 'QR'}</div>
        </div>
        <div class="grid">
          <div class="cell"><strong>计划号</strong>${currentPlan.planNo || '-'}</div>
          <div class="cell"><strong>首检状态</strong>${statusText}</div>
          <div class="cell"><strong>设备</strong>${currentPlan.equipmentCode || '-'} ${currentPlan.equipmentName || ''}</div>
          <div class="cell"><strong>工位</strong>${currentPlan.workCenterName || '-'}</div>
          <div class="cell"><strong>物料编码</strong>${currentPlan.materialCode || '-'}</div>
          <div class="cell"><strong>型号</strong>${currentPlan.modelCode || '-'}</div>
          <div class="cell"><strong>批号</strong>${currentFirstInspectionSliceNo.value || currentPlan.batchNo || '-'}</div>
          <div class="cell"><strong>申请时间</strong>${displayDateTimeText(summary.faiApplyTime)}</div>
          <div class="cell"><strong>检验标准</strong>${summary.faiStandardNo || '-'}</div>
          <div class="cell"><strong>判定</strong>${summary.faiJudgment || '-'}</div>
        </div>
        <div class="remark">
          <strong>驳回/NG说明：</strong>${summary.faiRejectReason || '-'}
        </div>
        <div class="footer">
          <span>送检人：${currentUserName.value || '-'}</span>
          <span>打印时间：${buildNowText()}</span>
        </div>
      </body>
    </html>
  `);
  printWindow.document.close();
  printWindow.focus();
  printWindow.print();
}

function openSelectedRecordConfirm() {
  if (!ensureSegmentWritable('扫码确认')) return;
  if (!sourceGroups.value.some((group) => group.segments.length > 0)) {
    AModal.info({ title: '暂无工作台片', content: '请先扫码计划号，系统会按已确认分切片号加载工作台。' });
    return;
  }
  openRecordConfirm();
}

function openChangeoverInspectionScan(initialSliceNo: string | Event = '') {
  if (!ensureSegmentWritable('首检')) return;
  const sliceNo = typeof initialSliceNo === 'string' ? initialSliceNo : '';
  if (!currentPlan.planNo) {
    AModal.warning({ title: '请先加载压槽计划', content: '首检需要先扫码或从待加工列表带出当前压槽计划。' });
    return;
  }
  resetChangeoverForm();
  if (sliceNo) {
    changeoverScanNo.value = sliceNo;
    void nextTick(() => scanChangeoverSlice());
  }
  changeoverVisible.value = true;
  nextTick(() => document.querySelector<HTMLInputElement>('[data-changeover-scan] input')?.focus());
}

function resetChangeoverForm(record?: MesHcAdhesiveConsoleApi.ChangeoverInspection) {
  const now = buildNowText();
  const currentMotherBatchNo = currentMotherSegmentBatchNo.value;
  const recordTime = normalizeDateTimeText(record?.recordTime);
  const submitTime = normalizeDateTimeText(record?.submitTime);
  Object.assign(changeoverForm, {
    checkItems: record?.checkItems?.length
      ? record.checkItems.map(normalizeCheckItem)
      : ensurePressSlotChangeoverItems(checkTemplate.value).map((item) => ({
          ...item,
          actualValue: getDefaultCheckActualValue(item),
          abnormalRemark: '',
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
    motherSegmentBatchNo: record?.motherSegmentBatchNo || currentMotherBatchNo,
    planId: currentPlan.planId || record?.planId || 0,
    planOperationId: currentPlan.planOperationId || record?.planOperationId || 0,
    pressSlotSliceNo: record?.pressSlotSliceNo || '',
    previousModelCode: record?.previousModelCode || '',
    productionMaterialCode: record?.productionMaterialCode || currentPlan.materialCode || '',
    productionModelCode: record?.productionModelCode || currentPlan.modelCode || '',
    recordTime: recordTime || now,
    recorderName: record?.recorderName || currentUserName.value || 'admin',
    remark: record?.remark || '',
    sourceSlittingSliceId: record?.sourceSlittingSliceId,
    submitTime: submitTime || recordTime || now,
  });
  changeoverScanNo.value = record?.pressSlotSliceNo || '';
}

function applyFirstInspectionSource(source: MesHcAdhesiveConsoleApi.SourceItem, fallbackBatchNo = '') {
  const sliceNo = source.productionBatchNo || source.confirmedBatchNo || fallbackBatchNo;
  firstInspectionSource.value = source;
  firstInspectionScanNo.value = sliceNo;
  firstInspectionScanError.value = '';
  changeoverForm.sourceSlittingSliceId = source.grindingSecondDetailId;
  changeoverForm.pressSlotSliceNo = sliceNo;
  changeoverForm.currentPlanNo = currentPlan.planNo;
  changeoverForm.planId = currentPlan.planId || 0;
  changeoverForm.planOperationId = currentPlan.planOperationId || 0;
  changeoverForm.productionMaterialCode = currentPlan.materialCode || '';
  changeoverForm.productionModelCode = currentPlan.modelCode || '';
  changeoverForm.motherSegmentBatchNo = resolveMotherSegmentBatchNo(sliceNo)
    || source.parentProductionBatchNo
    || source.motherBatchNo
    || currentMotherSegmentBatchNo.value;
}

async function scanFirstInspectionSlice(showSuccess: Event | boolean = true) {
  const shouldShowSuccess = showSuccess !== false;
  const normalizedScanNo = resolveTransferTicketQrBusinessNo(normalizeConfirmScanCode(firstInspectionScanNo.value));
  const batchNo = normalizedScanNo || firstInspectionScanNo.value.trim();
  firstInspectionScanError.value = '';
  if (!batchNo) {
    firstInspectionScanError.value = '请先扫码或输入首检批号';
    focusFirstInspectionScanInput();
    return false;
  }
  firstInspectionScanNo.value = batchNo;
  firstInspectionScanLoading.value = true;
  try {
    const source = await scanAdhesiveConsoleSource(batchNo, true);
    if (source.grindingPlanId && currentPlan.planId && source.grindingPlanId !== currentPlan.planId) {
      firstInspectionScanError.value = '首检批号不属于当前压槽计划，请重新扫码';
      firstInspectionSource.value = null;
      return false;
    }
    const sourceMotherBatchNo = resolveMotherSegmentBatchNo(source.productionBatchNo || source.confirmedBatchNo || batchNo)
      || source.parentProductionBatchNo
      || source.motherBatchNo;
    const currentMotherBatchNo = currentMotherSegmentBatchNo.value;
    if (currentMotherBatchNo && sourceMotherBatchNo && sourceMotherBatchNo !== currentMotherBatchNo) {
      firstInspectionScanError.value = '首检批号不属于当前分段批号，请重新扫码';
      firstInspectionSource.value = null;
      return false;
    }
    applyFirstInspectionSource(source, batchNo);
    if (shouldShowSuccess) {
      const selfCheck = String(source.selfCheck || 'OK').toUpperCase();
      message.success(selfCheck === 'NG' ? '已带入分切 NG 批号，可提交首检复判' : '已带入首检批号');
    }
    return true;
  } catch (error) {
    firstInspectionSource.value = null;
    firstInspectionScanError.value = getErrorMessage(error) || '未找到已确认分切批号，请检查扫码内容';
    return false;
  } finally {
    firstInspectionScanLoading.value = false;
  }
}

async function scanChangeoverSlice() {
  const batchNo = changeoverScanNo.value.trim();
  if (!batchNo) {
    message.warning('请先扫码或输入压槽片号');
    return;
  }
  const matched = findSourceSegmentWithGroup(batchNo);
  changeoverForm.sourceSlittingSliceId = matched?.segment.grindingSecondDetailId;
  changeoverForm.pressSlotSliceNo = matched?.segment.batchNo || batchNo;
  changeoverForm.currentPlanNo = currentPlan.planNo;
  changeoverForm.planId = currentPlan.planId || 0;
  changeoverForm.planOperationId = currentPlan.planOperationId || 0;
  changeoverForm.productionMaterialCode = currentPlan.materialCode || '';
  changeoverForm.productionModelCode = currentPlan.modelCode || '';
  changeoverForm.motherSegmentBatchNo = currentMotherSegmentBatchNo.value;
  if (!matched) {
    message.warning('未在当前已确认分切片中找到该片号，保存时将按手工首检记录插入。');
  }
}

async function submitChangeoverInspection() {
  if (!ensureSegmentWritable('保存首检')) return;
  if (!changeoverForm.pressSlotSliceNo) {
    await scanChangeoverSlice();
  }
  if (!changeoverForm.pressSlotSliceNo) {
    message.warning('请先扫码首检批号');
    return;
  }
  const now = buildNowText();
  const submitTime = normalizeDateTimeText(changeoverForm.submitTime) || now;
  const recordTime = normalizeDateTimeText(changeoverForm.recordTime) || submitTime;
  changeoverForm.submitTime = submitTime;
  changeoverForm.recordTime = recordTime;
  await savePressSlotChangeover({
    ...changeoverForm,
    currentPlanNo: currentPlan.planNo,
    motherSegmentBatchNo: currentMotherSegmentBatchNo.value || changeoverForm.motherSegmentBatchNo,
    planId: currentPlan.planId || changeoverForm.planId,
    planOperationId: currentPlan.planOperationId || changeoverForm.planOperationId,
    productionMaterialCode: currentPlan.materialCode || changeoverForm.productionMaterialCode,
    productionModelCode: currentPlan.modelCode || changeoverForm.productionModelCode,
    previousModelCode: '',
    inspectionStatus: changeoverForm.inspectionStatus || 'DRAFT',
    recordTime,
    submitTime,
    checkItems: (changeoverForm.checkItems || []).map((item, index) => ({ ...item, sortNo: item.sortNo || index + 1 })),
    detailItemsJson: JSON.stringify(changeoverForm.checkItems || []),
  });
  changeoverVisible.value = false;
  await loadChangeoverInspections();
  message.success('首检记录已保存为草稿，打印后进入待检状态');
}

function viewChangeoverInspection(record: MesHcAdhesiveConsoleApi.ChangeoverInspection | Record<string, any>) {
  resetChangeoverForm(record as MesHcAdhesiveConsoleApi.ChangeoverInspection);
  changeoverVisible.value = true;
}

async function confirmRecordScan() {
  if (recordConfirmForm.reportType === 'CHANGEOVER') {
    const scannedBatchNo = resolveTransferTicketQrBusinessNo(normalizeConfirmScanCode(recordConfirmForm.scannedBatchNo));
    if (!scannedBatchNo) {
      recordConfirmForm.error = '请先扫码或输入首检批号。';
      recordConfirmForm.message = '';
      return;
    }
    firstInspectionScanNo.value = scannedBatchNo;
    const scanned = await scanFirstInspectionSlice(false);
    if (!scanned) {
      recordConfirmForm.error = firstInspectionScanError.value || '未找到首检批号。';
      recordConfirmForm.message = '';
      return;
    }
    recordConfirmVisible.value = false;
    await submitFirstInspectionApplication();
    return;
  }
  const scannedBatchNo = resolveTransferTicketQrBusinessNo(normalizeConfirmScanCode(recordConfirmForm.scannedBatchNo));
  if (!scannedBatchNo) {
    recordConfirmForm.error = '请扫码或输入压槽片号。';
    recordConfirmForm.message = '';
    return;
  }
  const matched = findSourceSegmentWithGroup(scannedBatchNo);
  if (!matched) {
    recordConfirmForm.error = '未在当前工作台中找到该片号，请检查计划或重新扫码。';
    recordConfirmForm.message = '';
    return;
  }
  const selectedReportType: PressSlotReportType = 'PRODUCT';
  recordConfirmForm.reportType = selectedReportType;
  const existedRecord = findReportBySegment(matched.segment);
  const existedStatus = String(existedRecord?.reportStatus || '').toUpperCase();
  if (existedRecord?.editBlockedReason) {
    recordConfirmVisible.value = false;
    openReportViewDialog(matched.group, matched.segment);
    return;
  }
  if (existedStatus === 'SUBMITTED') {
    recordConfirmVisible.value = false;
    openReportViewDialog(matched.group, matched.segment);
    return;
  }
  if (existedStatus !== 'CONFIRMED') {
    const scanGatePassed = await validatePressSlotScanGate(
      matched.group,
      matched.segment,
      selectedReportType,
      scannedBatchNo,
      (messageText) => {
        recordConfirmForm.error = messageText;
        recordConfirmForm.message = '';
      },
    );
    if (!scanGatePassed) return;
  }
  recordConfirmVisible.value = false;
  await openReportConfirmDialog(matched.group, matched.segment, scannedBatchNo, true);
  if (existedStatus === 'CONFIRMED' && reportVisible.value && reportDialogMode.value === 'confirm') {
    message.info('该压槽片已确认，已进入覆盖修改；本次不会重复扣减耗材或生成库存流水。');
  }
}

async function validatePressSlotScanGate(
  group: SourceGroup,
  segment: SourceSegment,
  reportType: PressSlotReportType,
  scannedBatchNo?: string,
  onError?: (messageText: string) => void,
) {
  if (!currentPlan.planId || !currentPlan.planOperationId) {
    const text = '请先扫码或从待加工列表选择压槽计划';
    onError?.(text);
    return false;
  }
  const productionBatchNo = scannedBatchNo || segment.batchNo;
  const motherBatchNo = resolveMotherSegmentBatchNo(productionBatchNo)
    || group.baseBatchNo
    || currentMotherSegmentBatchNo.value;
  try {
    const scanGate = await validatePressSlotFaiScan({
      motherBatchNo,
      planId: currentPlan.planId,
      planOperationId: currentPlan.planOperationId,
      productionBatchNo,
      reportType,
    });
    if (scanGate?.allowScan === false) {
      const text = scanGate.warningMessage || '压槽扫码前首检/过程加检校验未通过';
      if (onError) {
        onError(text);
      } else {
        AModal.warning({
          content: text,
          title: '压槽扫码前校验未通过',
        });
      }
      return false;
    }
    if (scanGate?.warningMessage) {
      showPressSlotScanGateReminder(scanGate.warningMessage);
    }
    return true;
  } catch (error) {
    const text = getErrorMessage(error) || '压槽扫码前首检/过程加检校验未通过';
    if (onError) {
      onError(text);
    } else {
      AModal.warning({
        content: text,
        title: '压槽扫码前校验未通过',
      });
    }
    return false;
  }
}

async function loadSourceGroups() {
  if (!currentPlan.planId || !currentPlan.planOperationId) {
    sourceGroups.value = [];
    return;
  }
  const selectedSourceBatchNo = resolveCurrentPlanSourceBatchNo();
  const rows = normalizeRows(await getAdhesiveConsoleSourceList(currentPlan.planId, currentPlan.planOperationId));
  buildSourceGroupsFromSources(rows);
  const exactGroup = sourceGroups.value.find((group) => isSameBatchNo(group.baseBatchNo, selectedSourceBatchNo));
  const selectedGroup = exactGroup || sourceGroups.value[0];
  if (!exactGroup && selectedSourceBatchNo && selectedGroup) {
    message.warning(`未找到来源分段 ${selectedSourceBatchNo}，已临时切换到 ${selectedGroup.baseBatchNo}，请核对待加工分段`);
  }
  currentPlan.sourceBatchNo = selectedGroup?.baseBatchNo || currentPlan.sourceBatchNo || '';
  if (!currentPlan.sourceProductionBatchNo && selectedGroup?.segments?.[0]?.batchNo) {
    currentPlan.sourceProductionBatchNo = selectedGroup.segments[0].batchNo;
  }
  currentPlan.availableSourceLength = selectedGroup?.totalAvailableLength
    ?? sourceGroups.value.reduce((sum, group) => sum + group.totalAvailableLength, 0);
}

async function loadReportsAndSourceGroups() {
  await loadReports();
  await loadSourceGroups();
  await loadPressSlotAbnormalLockRows();
}

async function refreshPressSlotWorkbench() {
  if (!currentPlan.planId || !currentPlan.planOperationId) {
    message.warning('请先加载压槽计划');
    return;
  }
  if (workbenchRefreshing.value) return;
  workbenchRefreshing.value = true;
  try {
    await Promise.all([loadReports(), loadProcessCheckFaiRows()]);
    await loadSourceGroups();
    await loadPressSlotAbnormalLockRows();
    // message.success('工作台片信息已刷新');
  } catch (error: any) {
    message.warning(getErrorMessage(error) || '工作台片信息刷新失败');
  } finally {
    workbenchRefreshing.value = false;
  }
}

async function consumePlanScan(silent = false) {
  if (planScanTimer) {
    clearTimeout(planScanTimer);
    planScanTimer = null;
  }
  const rawScanValue = scanPlanNo.value;
  const scannerRequiresSlice = !!pendingScannerPlanNo || hasPlanScanDelimiter(rawScanValue);
  const scannedSliceNo = pendingScannerSliceNo || normalizePlanScanSliceNo(rawScanValue);
  const planNo = syncPlanScanNo(rawScanValue);
  const scannedSourceBatchNo = scannedSliceNo
    ? resolveMotherSegmentBatchNo(scannedSliceNo) || scannedSliceNo
    : resolvePlanScanSourceBatchNo(rawScanValue);
  if (!planNo) {
    if (!silent) message.warning('请先扫码或输入计划号');
    focusPlanScanInput();
    return;
  }
  if (scannerRequiresSlice && !scannedSliceNo) {
    resetPlan();
    clearPlanScannerState();
    AModal.warning({
      content: '压槽扫码计划需同时包含计划号和扫码片号，请确认流转单二维码内容后重新扫码。',
      okText: '知道了',
      onOk: focusPlanScanInput,
      title: '扫码计划缺少片号',
    });
    return;
  }
  boardLoading.value = true;
  try {
    const rows = normalizeRows(await getAdhesiveConsoleTaskList({ taskKeyword: planNo, taskStatus: 'ALL' }));
    const planRows = rows.filter((item) => item.planNo === planNo);
    const candidateRows = planRows.length > 0 ? planRows : rows;
    const exactSourceRows = scannedSliceNo
      ? candidateRows.filter((item) => matchTaskScanSliceNo(item, scannedSliceNo))
      : scannedSourceBatchNo
        ? candidateRows.filter((item) => matchTaskSourceBatchNo(item, scannedSourceBatchNo))
        : [];
    if (scannerRequiresSlice && exactSourceRows.length === 0) {
      resetPlan();
      clearPlanScannerState();
      await loadDailyRecords();
      AModal.warning({
        content: `未找到计划号 ${planNo} 且扫码片号 ${scannedSliceNo} 同时匹配的压槽工单，扫码计划框已清空，请确认后重新扫码。`,
        okText: '知道了',
        onOk: focusPlanScanInput,
        title: '扫码计划未找到',
      });
      return;
    }
    const task = exactSourceRows[0] || candidateRows[0];
    if (!task?.planOperationId) {
      resetPlan();
      clearPlanScannerState();
      await loadDailyRecords();
      AModal.warning({
        content: '未找到压槽工单，请确认计划号或工序任务状态，扫码计划框已清空。',
        okText: '知道了',
        onOk: focusPlanScanInput,
        title: '扫码计划未找到',
      });
      return;
    }
    if (scannedSourceBatchNo && exactSourceRows.length === 0 && candidateRows.length > 1) {
      clearPlanScannerState();
      focusPlanScanInput();
      taskListFilters.planNo = planNo;
      taskListFilters.batchNo = scannedSourceBatchNo;
      taskListFilters.status = 'UNFINISHED';
      taskListVisible.value = true;
      await loadTaskList();
      message.warning(`未匹配到扫码片号 ${scannedSliceNo || scannedSourceBatchNo}，请从待加工列表选择具体压槽任务`);
      return;
    }
    if (candidateRows.length > 1 && !scannedSourceBatchNo && countTaskSourceContexts(candidateRows) > 1) {
      clearPlanScannerState();
      focusPlanScanInput();
      taskListFilters.planNo = planNo;
      taskListFilters.batchNo = '';
      taskListFilters.status = 'UNFINISHED';
      taskListVisible.value = true;
      await loadTaskList();
      message.warning('当前计划存在多个待加工压槽来源，请从待加工列表选择具体来源');
      return;
    }
    const taskSourceBatchNo = scannedSourceBatchNo || resolveMotherBatchNo(task as Record<string, any>);
    if (
      normalizeWorkOrderStatus(task.status) !== 'FINISHED' &&
      !(await ensureSampleAbnormalUnlocked(
        buildSegmentChainSampleLockCandidates({
          motherBatchNo: taskSourceBatchNo,
          segmentBatchNo: taskSourceBatchNo,
        }),
        '扫码加载压槽',
      ))
    ) {
      return;
    }
    applyTask(task);
    clearPlanScannerState();
    lastAutoScannedPlanNo.value = task.planNo || planNo;
    await Promise.all([loadDailyRecords(), loadReports(), loadGlueBoardUsage(), loadProcessParameters(), loadPressSlotMiddleLedger()]);
    await loadSourceGroups();
    await Promise.all([loadChangeoverInspections(), loadPressSlotFaiSummary(), loadProcessCheckFaiRows(), loadPressSlotAbnormalLockRows()]);
    await loadIntermediateRecord();
    if (!dailyPreparationReady.value) {
      showDailyPreparationRequiredWarning();
      return;
    }
    focusPlanScanInput();
    // if (!silent) message.success('已带出压槽计划、工作准备和可加工来源');
  } finally {
    boardLoading.value = false;
  }
}

const isGlobalScannerCandidate = (value: string) => {
  const text = value.trim();
  if (!text) return false;
  if (firstInspectionScanVisible.value) return text.length >= 3;
  if (recordConfirmVisible.value) return text.length >= 3;
  return hasPlanScanDelimiter(text) || normalizePlanScanNo(text).length >= PLAN_SCAN_MIN_LENGTH;
};

const routeGlobalScannerInput = (value: string) => {
  const scanned = value.trim();
  if (firstInspectionScanVisible.value) {
    firstInspectionScanNo.value = resolveTransferTicketQrBusinessNo(normalizeConfirmScanCode(scanned)) || scanned;
    focusFirstInspectionScanInput();
    nextTick(() => void confirmFirstInspectionScanAndSubmit());
    return;
  }
  if (recordConfirmVisible.value) {
    recordConfirmForm.scannedBatchNo = scanned;
    focusRecordConfirmScanInput();
    nextTick(() => void confirmRecordScan());
    return;
  }
  handlePlanScanInput(scanned);
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
    event.ctrlKey
    || event.altKey
    || event.metaKey
    || event.isComposing
    || isEventFromScannerInput(event)
    || isEditableEventTarget(event.target)
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

function toDailyRecord(record: Record<string, any>) {
  return record as DailyRecordRow;
}

function toTaskItem(record: Record<string, any>) {
  return record as MesHcAdhesiveConsoleApi.TaskItem;
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

function resolveOperationAuthOperator(authPayload: any) {
  return authPayload?.empName || authPayload?.nickname || authPayload?.username || currentUserName.value || '';
}

function resolveDailyRecordAuthOperator(authPayload: any) {
  return resolveOperationAuthOperator(authPayload);
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
  if (!equipment.id && !equipment.code) {
    message.warning('请先选择压槽设备，再填写清洁点检');
    return;
  }
  if (!context?.planId || !context?.planOperationId) {
    message.warning('请先选择压槽设备并加载该设备对应压槽计划后，再保存今日点检/清洁记录');
    return;
  }
  const existingRecorder = row.recorder === '-' ? operator : row.recorder;
  const recorder = action.mode === 'edit' ? operator : existingRecorder;
  const existingRecorderTime = row.recorderTime === '-' ? now : row.recorderTime;
  const recorderTime = action.mode === 'confirm'
    ? existingRecorderTime
    : now;
  const existingConfirmer = row.confirmer === '-' ? undefined : row.confirmer;
  const confirmer = action.mode === 'confirm' ? operator : existingConfirmer;
  const existingConfirmerTime = row.confirmerTime === '-' ? undefined : row.confirmerTime;
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
      status: 'OK',
    })),
    equipmentCode: equipment.code || undefined,
    equipmentId: equipment.id,
    equipmentName: equipment.name || undefined,
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
  const context = getDailyRecordContext();
  const equipment = getEffectiveBoardEquipment(context || {});
  if (!equipment.id && !equipment.code) {
    message.warning('请先选择压槽设备，再填写清洁点检');
    await openEquipmentSelect();
    return;
  }
  if (!context?.planId || !context?.planOperationId) {
    message.warning('请先选择压槽设备并加载该设备对应压槽计划后，再保存今日点检/清洁记录');
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

function getConsumableMaterialOptionValue(material?: MesHcAdhesiveConsoleApi.ConsumableMaterialOption) {
  if (!material) return '';
  return String(material.materialCode || (material.id ? `ID:${material.id}` : ''));
}

function buildConsumableMaterialLabel(material: MesHcAdhesiveConsoleApi.ConsumableMaterialOption) {
  const code = material.materialCode || '-';
  const name = material.materialName || material.materialShortName || '-';
  const spec = material.specModel ? ` / ${material.specModel}` : '';
  return `${code} / ${name}${spec}`;
}

function buildConsumableMaterialOption(material: MesHcAdhesiveConsoleApi.ConsumableMaterialOption) {
  return {
    label: buildConsumableMaterialLabel(material),
    material,
    value: getConsumableMaterialOptionValue(material),
  };
}

function mergeConsumableMaterialOptions(options: ConsumableMaterialSelectOption[], selected?: MesHcAdhesiveConsoleApi.ConsumableMaterialOption) {
  const map = new Map<string, ConsumableMaterialSelectOption>();
  if (selected && getConsumableMaterialOptionValue(selected)) {
    const option = buildConsumableMaterialOption(selected);
    map.set(option.value, option);
  }
  options.forEach((option) => {
    if (option.value) {
      map.set(option.value, option);
    }
  });
  return Array.from(map.values());
}

function getFormConsumableMaterial(type: PressSlotConsumableType) {
  const form = type === 'BEARING' ? glueConsumeForm : rollerCleanForm;
  if (!form.materialCode && !form.materialName) {
    return undefined;
  }
  return {
    materialCode: form.materialCode,
    materialName: form.materialName,
  } as MesHcAdhesiveConsoleApi.ConsumableMaterialOption;
}

function persistLastConsumableMaterial(type: PressSlotConsumableType, material: MesHcAdhesiveConsoleApi.ConsumableMaterialOption) {
  lastConsumableMaterialMap[type] = material;
  if (typeof window !== 'undefined') {
    window.localStorage.setItem(PRESS_SLOT_LAST_CONSUMABLE_MATERIAL_KEY, JSON.stringify(lastConsumableMaterialMap));
  }
}

function applyConsumableMaterial(type: PressSlotConsumableType, material?: MesHcAdhesiveConsoleApi.ConsumableMaterialOption) {
  const form = type === 'BEARING' ? glueConsumeForm : rollerCleanForm;
  if (!material) {
    form.materialCode = '';
    form.materialName = '';
    form.materialOptionValue = undefined;
    return;
  }
  form.materialCode = material.materialCode || '';
  form.materialName = material.materialName || material.materialShortName || '';
  form.materialOptionValue = getConsumableMaterialOptionValue(material) || undefined;
  if (form.materialOptionValue) {
    consumableMaterialOptions.value = mergeConsumableMaterialOptions(consumableMaterialOptions.value, material);
    persistLastConsumableMaterial(type, material);
  }
}

function prepareConsumableMaterialForm(type: PressSlotConsumableType, current: { materialCode?: string; materialName?: string }) {
  const lastMaterial = lastConsumableMaterialMap[type];
  const currentMaterial = current.materialCode || current.materialName
    ? {
        materialCode: current.materialCode,
        materialName: current.materialName,
      } as MesHcAdhesiveConsoleApi.ConsumableMaterialOption
    : undefined;
  applyConsumableMaterial(type, currentMaterial || lastMaterial);
}

async function loadConsumableMaterialOptions(type: PressSlotConsumableType, keyword = '') {
  activeMaterialSelectType.value = type;
  consumableMaterialLoading.value = true;
  try {
    const rows = normalizeRows(await getPressSlotConsumableMaterialOptions({
      consumableType: type,
      keyword: String(keyword || '').trim() || undefined,
      pageSize: 30,
    })) as MesHcAdhesiveConsoleApi.ConsumableMaterialOption[];
    consumableMaterialOptions.value = mergeConsumableMaterialOptions(
      rows.map(buildConsumableMaterialOption),
      getFormConsumableMaterial(type),
    );
  } finally {
    consumableMaterialLoading.value = false;
  }
}

function handleConsumableMaterialSearch(type: PressSlotConsumableType, keyword: string) {
  void loadConsumableMaterialOptions(type, keyword);
}

function handleConsumableMaterialChange(type: PressSlotConsumableType, value?: any) {
  const option = consumableMaterialOptions.value.find((item) => item.value === String(value || ''));
  applyConsumableMaterial(type, option?.material);
}

function openGlueConsumeDialog(type: PressSlotConsumableType = 'BEARING') {
  activeConsumableType.value = type;
  if (type !== 'BEARING') {
    message.warning('压槽辊不在看板更换，只支持清洗复位。');
    return;
  }
  const current = activeConsumable.value;
  consumableMaterialOptions.value = [];
  glueConsumeForm.stockId = current.stockId;
  glueConsumeForm.materialScanCode = current.batchNo;
  glueConsumeForm.batchNo = current.batchNo;
  glueConsumeForm.materialCode = '';
  glueConsumeForm.materialName = '';
  glueConsumeForm.materialOptionValue = undefined;
  glueConsumeForm.stockMeasureMode = 'COUNT';
  glueConsumeForm.initialUseCount = 0;
  glueConsumeForm.replaceReason = '';
  glueConsumeForm.replaceTime = buildNowText();
  glueConsumeForm.receiveStartPosition = 0;
  glueConsumeForm.receiveLength = undefined;
  glueConsumeForm.stockAvailableCount = Number(current.stockCount || 0);
  glueConsumeForm.stockAvailableLength = 0;
  prepareConsumableMaterialForm('BEARING', current);
  glueConsumeVisible.value = true;
  void loadConsumableMaterialOptions('BEARING');
}

async function confirmGlueConsume() {
  if (!String(glueConsumeForm.batchNo || '').trim()) {
    message.warning('请填写轴承批号');
    return;
  }
  if (!normalizeDateTimeText(glueConsumeForm.replaceTime)) {
    message.warning('请选择上次更换时间');
    return;
  }
  if (Number(glueConsumeForm.initialUseCount || 0) < 0) {
    message.warning('初始化片数不能小于0');
    return;
  }
  if (!String(glueConsumeForm.replaceReason || '').trim()) {
    message.warning('请填写更换原因');
    return;
  }
  pendingConsumableAction.value = 'REPLACE_BEARING';
  consumableAuthAction.value = '确认轴承更换并记录人员';
  consumableAuthVisible.value = true;
}

function resetPressSlotRollerClean() {
  consumableMaterialOptions.value = [];
  rollerCleanForm.cleanRemark = '';
  rollerCleanForm.batchNo = glueBoard.batchNo || '';
  rollerCleanForm.cleanTime = buildNowText();
  rollerCleanForm.initialUseCount = 0;
  rollerCleanForm.materialCode = '';
  rollerCleanForm.materialName = '';
  rollerCleanForm.materialOptionValue = undefined;
  prepareConsumableMaterialForm('PRESS_ROLLER', glueBoard);
  rollerCleanVisible.value = true;
  void loadConsumableMaterialOptions('PRESS_ROLLER');
}

function confirmRollerClean() {
  if (!String(rollerCleanForm.batchNo || '').trim()) {
    message.warning('请填写压槽批号');
    return;
  }
  if (!normalizeDateTimeText(rollerCleanForm.cleanTime)) {
    message.warning('请选择上次清洗时间');
    return;
  }
  if (Number(rollerCleanForm.initialUseCount || 0) < 0) {
    message.warning('初始化片数不能小于0');
    return;
  }
  if (!String(rollerCleanForm.cleanRemark || '').trim()) {
    message.warning('请填写清洗原因');
    return;
  }
  pendingConsumableAction.value = 'CLEAN_ROLLER';
  consumableAuthAction.value = '确认压槽辊清洗复位并记录人员';
  consumableAuthVisible.value = true;
}

async function handleConsumableAuthSuccess(userInfo: any) {
  if (pendingConsumableAction.value === 'CLEAN_ROLLER') {
    await cleanPressSlotConsumable({
      batchNo: rollerCleanForm.batchNo.trim(),
      cleanRemark: rollerCleanForm.cleanRemark.trim(),
      cleanTime: normalizeDateTimeText(rollerCleanForm.cleanTime),
      consumableType: 'PRESS_ROLLER',
      equipmentCode: currentPlan.equipmentCode,
      equipmentId: currentPlan.equipmentId,
      equipmentName: currentPlan.equipmentName,
      initialUseCount: Number(rollerCleanForm.initialUseCount || 0),
      materialCode: rollerCleanForm.materialCode.trim(),
      materialName: rollerCleanForm.materialName,
      operatorId: userInfo?.userId,
      operatorName: userInfo?.empName || currentUserName.value || 'admin',
      planOperationId: currentPlan.planOperationId,
    });
    await loadGlueBoardUsage();
    rollerCleanVisible.value = false;
    message.success('压槽辊清洗已复位');
  } else if (pendingConsumableAction.value === 'REPLACE_BEARING') {
    await replacePressSlotConsumable({
      batchNo: String(glueConsumeForm.batchNo || '').trim(),
      consumableType: 'BEARING',
      equipmentCode: currentPlan.equipmentCode,
      equipmentId: currentPlan.equipmentId,
      equipmentName: currentPlan.equipmentName,
      materialCode: String(glueConsumeForm.materialCode || '').trim(),
      materialName: glueConsumeForm.materialName,
      initialUseCount: Number(glueConsumeForm.initialUseCount || 0),
      operatorId: userInfo?.userId,
      operatorName: userInfo?.empName || currentUserName.value || undefined,
      planId: currentPlan.planId,
      planNo: currentPlan.planNo,
      planOperationId: currentPlan.planOperationId,
      replaceReason: glueConsumeForm.replaceReason || undefined,
      replaceTime: normalizeDateTimeText(glueConsumeForm.replaceTime),
    });
    await loadGlueBoardUsage();
    glueConsumeVisible.value = false;
    message.success('轴承更换记录已保存');
  }
  pendingConsumableAction.value = '';
  consumableAuthAction.value = '';
}

function resetReportForm() {
  Object.assign(reportForm, {
    aqcStatus: '',
    aqcTaskId: undefined,
    defectCode: '',
    endTime: '',
    glueBoardBatchNo: glueBoard.batchNo,
    glueBoardMaterialCode: glueBoard.materialCode,
    glueBoardStartPosition: Number(glueBoard.availableStartPosition || 0),
    glueBoardUsageId: undefined,
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
    coaFlag: false,
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
  resetVisualInspectionItems();
  clearReportIntermediateDraft();
}

async function scanReportSource() {
  if (reportSourceScanning.value) {
    reportForm.sourceScanError = '片号扫码接口正在解析，请稍后再保存。';
    return false;
  }
  const sourceCode = reportForm.sourceCode.trim();
  reportForm.sourceScanError = '';
  reportForm.sourceScanMessage = '';
  if (!sourceCode) {
    reportForm.sourceScanError = '请先扫描或输入分切已确认片号。';
    return false;
  }
  reportSourceScanning.value = true;
  try {
    const source = await scanAdhesiveConsoleSource(sourceCode);
    const pressSlotBatchNo = source.productionBatchNo || source.confirmedBatchNo || sourceCode;
    reportForm.sourceGrindingSecondDetailId = source.grindingSecondDetailId;
    reportForm.sourceProductionBatchNo = pressSlotBatchNo;
    reportForm.parentBatchNo = resolveMotherSegmentBatchNo(pressSlotBatchNo)
      || source.parentProductionBatchNo
      || source.motherBatchNo
      || stripSegmentMark(sourceCode);
    reportForm.productionBatchNo = pressSlotBatchNo;
    const sourceLength = Number(source.outputLength || source.processLength || reportForm.processLength || 0);
    const segment = findSourceSegment(reportForm.sourceProductionBatchNo);
    const availableRange = segment
      ? findFirstAvailableRange(segment)
      : { end: sourceLength, length: sourceLength, start: 0 };
    reportForm.startPosition = availableRange.start;
    reportForm.endPosition = availableRange.end;
    reportForm.processLength = availableRange.length;
    reportForm.outputLength = reportOutputLength.value;
    reportForm.sourceScanMessage = '来源已通过压槽片号扫码接口带出。';
    return true;
  } catch (error) {
    reportForm.sourceScanError = '片号扫码接口未找到该批次，当前只能作为视觉原型预览，不能提交后台。';
    return false;
  } finally {
    reportSourceScanning.value = false;
  }
}

async function ensureReportSourceResolvedBeforeSave() {
  if (reportForm.sourceGrindingSecondDetailId) return true;
  reportForm.sourceCode = reportForm.sourceCode
    || reportForm.productionBatchNo
    || reportForm.sourceProductionBatchNo
    || '';
  const resolved = await scanReportSource();
  if (!resolved) {
    message.warning(reportForm.sourceScanError || '请先通过片号扫码接口确认压槽来源片号');
  }
  return resolved;
}

function getFirstReportTabKey() {
  return getDefaultReportTabKey();
}

function openReportViewDialog(group: SourceGroup, segment: SourceSegment) {
  resetReportForm();
  applyGlueBoardDefaultsToCheckItems();
  applySegmentToReportForm(group, segment, false);
  const existedRecord = findReportBySegment(segment);
  activeRecord.value = existedRecord || null;
  applyReportRecordToForm(existedRecord);
  reportDialogMode.value = 'view';
  activeReportTab.value = getFirstReportTabKey();
  reportVisible.value = true;
}

function getDefaultPressSlotReportType(_segment?: SourceSegment): PressSlotReportType {
  return 'PRODUCT';
}

function getPressSlotReportTypeText(type?: string) {
  return pressSlotReportTypeOptions.find((item) => item.value === type)?.label || '成品加工';
}

async function setActiveReportMiddleType(reportType: 'END' | 'FRONT' | 'MIDDLE') {
  if (!ensureSegmentWritable('设置中间品段位')) return;
  const currentRecord = activeRecord.value;
  if (!currentRecord?.id) {
    message.warning('请先打开已保存的压槽片详情');
    return;
  }
  await loadPressSlotAbnormalLockRows();
  if (isPressSlotMiddleAutoMarkAbnormalLockedRecord(currentRecord)) {
    message.warning(`片号 ${currentRecord.productionBatchNo || '-'} 处于异常锁定中，不能设置为中间品段位`);
    return;
  }
  const reportId = Number(currentRecord.id);
  if (middleTypeSetting.value) return;
  middleTypeSetting.value = true;
  try {
    await setPressSlotReportMiddleType(reportId, reportType);
    const extra = {
      ...(parseRecordExtra(currentRecord) as Record<string, any>),
      reportType,
      reportTypeName: getPressSlotReportTypeText(reportType),
    };
    activeRecord.value = { ...currentRecord, extraJson: JSON.stringify(extra) };
    reportForm.reportType = reportType;
    await Promise.all([loadReportsAndSourceGroups(), loadPressSlotMiddleLedger()]);
    message.success(`已设置为${getPressSlotReportTypeText(reportType)}，新建中间品记录表时会带入该段片号`);
  } catch (error: any) {
    message.error(getErrorMessage(error) || '设置中间品段位失败');
  } finally {
    middleTypeSetting.value = false;
  }
}

function openAbnormalCategoryCorrection() {
  if (!canCorrectAbnormalCategory.value) {
    message.warning('当前账号没有修正异常类别权限，或当前片没有压槽外观异常类别');
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
    message.warning('请先打开已保存的压槽片详情');
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
    const updatedRecord = await correctPressSlotReportAbnormalCategory({
      category: abnormalCategoryCorrectionForm.category,
      id: Number(currentRecord.id),
      reason,
    });
    activeRecord.value = updatedRecord;
    applyReportRecordToForm(updatedRecord);
    await loadReportsAndSourceGroups();
    abnormalCategoryCorrectionVisible.value = false;
    message.success('异常类别已修正，统计归类将按新类别计算');
  } catch (error: any) {
    message.error(getErrorMessage(error) || '修正异常类别失败');
  } finally {
    abnormalCategoryCorrectionSaving.value = false;
  }
}

type PressSlotMiddleAutoMarkType = 'END' | 'FRONT' | 'MIDDLE';
interface PressSlotMiddleAutoMarkTarget {
  batchNo: string;
  index: number;
  record: MesHcAdhesiveConsoleApi.ReportItem;
  reportType: PressSlotMiddleAutoMarkType;
}

interface PressSlotMiddleAutoMarkPreviewRow {
  batchNo: string;
  index: number;
  key: string;
  markText: string;
}

function isPressSlotRecordCoa(record?: MesHcAdhesiveConsoleApi.ReportItem | Record<string, any> | null) {
  if (!record) return false;
  const extra = parseRecordExtra(record) as Record<string, any>;
  const raw = (record as Record<string, any>).coaFlag ?? extra.coaFlag;
  return raw === true || raw === 'true' || raw === 'Y' || raw === 1;
}

function findPressSlotSegmentByReport(record: MesHcAdhesiveConsoleApi.ReportItem) {
  const batchNos = [
    record.productionBatchNo,
    record.sourceProductionBatchNo,
  ].map((item) => String(item || '').trim()).filter(Boolean);
  if (!batchNos.length) return undefined;
  const batchNoSet = new Set(batchNos);
  return sourceGroups.value
    .flatMap((group) => group.segments)
    .find((segment) => batchNoSet.has(segment.batchNo));
}

function isPressSlotMiddleAutoMarkCoaRecord(record: MesHcAdhesiveConsoleApi.ReportItem) {
  if (isPressSlotRecordCoa(record)) return true;
  const segment = findPressSlotSegmentByReport(record);
  return segment ? getPressSlotSegmentCoaFlag(segment) : false;
}

function isPressSlotMiddleAutoMarkNormalFirstInspectionRecord(record: MesHcAdhesiveConsoleApi.ReportItem) {
  const batchNo = normalizePressSlotCardBatchNo(record.productionBatchNo);
  const reportId = Number(record.id || 0);
  const firstInspectionRows = [
    ...(firstInspection.value?.faiId ? [firstInspection.value] : []),
    ...firstInspectionFaiRows.value,
  ];
  return firstInspectionRows.some((inspection) => {
    const inspectionBatchNo = normalizePressSlotCardBatchNo(getFirstInspectionProcessSliceNo(inspection));
    const inspectionSourceReportId = Number((inspection as Record<string, any>).sourceReportId || 0);
    const sameBatchNo = !!batchNo && inspectionBatchNo === batchNo;
    const sameSourceReport = !!reportId && !!inspectionSourceReportId && inspectionSourceReportId === reportId;
    if (!sameBatchNo && !sameSourceReport) return false;
    return normalizePressSlotInspectionResultText(getFirstInspectionFaiResultText(inspection)) === '正常';
  });
}

function isPressSlotMiddleAutoMarkProcessCheckRecord(record: MesHcAdhesiveConsoleApi.ReportItem) {
  const batchNos = [
    record.productionBatchNo,
    record.sourceProductionBatchNo,
  ].map((item) => normalizePressSlotCardBatchNo(item)).filter(Boolean);
  const reportId = Number(record.id || 0);
  const isAbnormalInspectionSlice = abnormalLockRows.value.some((lockRecord) => {
    const abnormalBatchNo = normalizePressSlotCardBatchNo(lockRecord.abnormalSampleBatchNo);
    const releaseBatchNo = normalizePressSlotCardBatchNo(lockRecord.releaseSampleBatchNo);
    return (!!abnormalBatchNo && batchNos.includes(abnormalBatchNo))
      || (!!releaseBatchNo && batchNos.includes(releaseBatchNo));
  });
  if (isAbnormalInspectionSlice) return true;
  return processCheckFaiRows.value.some((inspection) => {
    const inspectionBatchNo = normalizePressSlotCardBatchNo(getFirstInspectionProcessSliceNo(inspection));
    const inspectionSourceReportId = Number((inspection as Record<string, any>).sourceReportId || 0);
    const sameBatchNo = !!inspectionBatchNo && batchNos.includes(inspectionBatchNo);
    const sameSourceReport = !!reportId && !!inspectionSourceReportId && inspectionSourceReportId === reportId;
    return sameBatchNo || sameSourceReport;
  });
}

function isPressSlotMiddleAutoMarkAbnormalLockedRecord(record?: MesHcAdhesiveConsoleApi.ReportItem | null) {
  if (!record) return false;
  const batchNos = [
    record.productionBatchNo,
    record.sourceProductionBatchNo,
  ].map((item) => String(item || '').trim()).filter(Boolean);
  if (batchNos.some((batchNo) => abnormalLockedSliceNoSet.value.has(batchNo))) return true;
  const reportId = Number(record.id || 0);
  return !!reportId && abnormalLockRows.value.some((lockRecord) =>
    isActivePressSlotAbnormalLock(lockRecord)
    && Number(lockRecord.reportId || 0) === reportId,
  );
}

function getPressSlotBatchSerialNo(batchNo?: string) {
  const text = String(batchNo || '').trim();
  if (!text) return Number.MAX_SAFE_INTEGER;
  const normalized = /[AB]$/i.test(text) ? text.slice(0, -1) : text;
  const sortText = normalized.slice(-3);
  return /^\d+$/.test(sortText) ? Number(sortText) : Number.MAX_SAFE_INTEGER;
}

function comparePressSlotCardNo(left?: string, right?: string) {
  const serialDiff = getPressSlotBatchSerialNo(left) - getPressSlotBatchSerialNo(right);
  if (serialDiff !== 0) return serialDiff;
  return String(left || '').localeCompare(String(right || ''), 'zh-Hans-CN', { numeric: true });
}

function sortPressSlotMiddleCandidateRecords(records: MesHcAdhesiveConsoleApi.ReportItem[]) {
  return [...records].sort((a, b) => {
    return comparePressSlotCardNo(a.productionBatchNo, b.productionBatchNo);
  });
}

function getPressSlotMiddleAutoMarkCandidates() {
  const currentMother = normalizePressSlotMiddleBatchNo(resolveCurrentIntermediateBatchNo() || currentMotherSegmentBatchNo.value || '');
  return sortPressSlotMiddleCandidateRecords(
    reportRecords.value
      .filter((record) => !!record.id && !!record.productionBatchNo)
      .filter((record) => isAdhesiveRecordConfirmed(record.reportStatus))
      .filter((record) => !hasPressSlotVisualIssue(record))
      .filter((record) => !isPressSlotMiddleAutoMarkCoaRecord(record))
      .filter((record) => !isPressSlotMiddleAutoMarkNormalFirstInspectionRecord(record))
      .filter((record) => !isPressSlotMiddleAutoMarkProcessCheckRecord(record))
      .filter((record) => !isPressSlotMiddleAutoMarkAbnormalLockedRecord(record))
      .filter((record) => {
        if (!currentMother) return true;
        return resolveIntermediateReportMotherBatchNo(record) === currentMother;
      }),
  );
}

function buildPressSlotMiddleAutoMarkTargets(records: MesHcAdhesiveConsoleApi.ReportItem[]) {
  const middleIndex = Math.floor((records.length - 1) / 2);
  const rawTargets: PressSlotMiddleAutoMarkTarget[] = [
    { batchNo: records[0]?.productionBatchNo || '', index: 1, record: records[0]!, reportType: 'FRONT' },
    { batchNo: records[middleIndex]?.productionBatchNo || '', index: middleIndex + 1, record: records[middleIndex]!, reportType: 'MIDDLE' },
    { batchNo: records[records.length - 1]?.productionBatchNo || '', index: records.length, record: records[records.length - 1]!, reportType: 'END' },
  ];
  const seenRecordIds = new Set<number | string>();
  return rawTargets.filter((target) => {
    const recordId = target.record?.id;
    if (!recordId || seenRecordIds.has(recordId)) return false;
    seenRecordIds.add(recordId);
    return !!target.batchNo;
  });
}

function getPressSlotMiddleAutoMarkTypeText(reportType?: PressSlotMiddleAutoMarkType) {
  if (reportType === 'FRONT') return '前段';
  if (reportType === 'MIDDLE') return '中段';
  if (reportType === 'END') return '后段';
  return '';
}

function buildPressSlotMiddleAutoMarkPreviewRows(
  records: MesHcAdhesiveConsoleApi.ReportItem[],
  targets: PressSlotMiddleAutoMarkTarget[],
): PressSlotMiddleAutoMarkPreviewRow[] {
  const targetTypeMap = new Map<string, PressSlotMiddleAutoMarkType>();
  targets.forEach((target) => {
    if (target.record?.id != null) targetTypeMap.set(String(target.record.id), target.reportType);
  });
  return records.map((record, index) => {
    const reportType = record.id == null ? undefined : targetTypeMap.get(String(record.id));
    return {
      batchNo: record.productionBatchNo || '',
      index: index + 1,
      key: String(record.id || record.productionBatchNo || index),
      markText: getPressSlotMiddleAutoMarkTypeText(reportType),
    };
  });
}

function renderPressSlotMiddleAutoMarkConfirmContent(motherBatchNo: string, rows: PressSlotMiddleAutoMarkPreviewRow[]) {
  return h('div', { class: 'press-slot-auto-middle-confirm' }, [
    h('p', `当前母卷批号 ${motherBatchNo || '-'} 共自检正常且非首检/过程加检/COA/异常锁定 ${rows.length} 片，确认后按前段、中段、后段更新中间品标记。`),
    h(ATable, {
      bordered: true,
      columns: [
        { align: 'center', dataIndex: 'index', key: 'index', title: '顺序号', width: 80 },
        { dataIndex: 'batchNo', key: 'batchNo', title: '卡号', width: 240 },
        { align: 'center', dataIndex: 'markText', key: 'markText', title: '前中尾标记', width: 120 },
      ],
      dataSource: rows,
      pagination: false,
      rowKey: 'key',
      scroll: { x: 520, y: 320 },
      size: 'small',
    }),
  ]);
}

async function openAutoMarkPressSlotMiddleSegments() {
  if (!ensureSegmentWritable('标记中间品')) return;
  if (!currentPlan.planId || !currentPlan.planOperationId) {
    message.warning('请先加载压槽计划');
    return;
  }
  const motherBatchNo = normalizePressSlotMiddleBatchNo(resolveCurrentIntermediateBatchNo() || currentMotherSegmentBatchNo.value || currentPlan.sourceBatchNo || '');
  await loadPressSlotAbnormalLockRows();
  const candidates = getPressSlotMiddleAutoMarkCandidates();
  if (candidates.length < 3) {
    message.warning(`当前母卷批号 ${motherBatchNo || '-'} 已确认、自检正常且非首检/过程加检/COA/异常锁定片数为 ${candidates.length}，至少需要 3 片才能标记前中后段`);
    return;
  }
  const targets = buildPressSlotMiddleAutoMarkTargets(candidates);
  if (targets.length < 3) {
    message.warning('计算出的前段、中段、后段片号不完整，请确认片号流水号后重试');
    return;
  }
  const previewRows = buildPressSlotMiddleAutoMarkPreviewRows(candidates, targets);
  AModal.confirm({
    cancelText: '取消',
    content: renderPressSlotMiddleAutoMarkConfirmContent(motherBatchNo, previewRows),
    okText: '确认标记',
    title: '标记中间品确认',
    width: 680,
    onOk: async () => {
      if (middleAutoMarking.value) return;
      middleAutoMarking.value = true;
      try {
        for (const target of targets) {
          await setPressSlotReportMiddleType(Number(target.record.id), target.reportType);
        }
        await Promise.all([loadReportsAndSourceGroups(), loadPressSlotMiddleLedger()]);
        message.success('中间品前中后段已标记完成');
      } catch (error: any) {
        message.error(getErrorMessage(error) || '标记中间品失败');
        throw error;
      } finally {
        middleAutoMarking.value = false;
      }
    },
  });
}

async function openReportConfirmDialog(
  group: SourceGroup,
  segment: SourceSegment,
  scannedBatchNo?: string,
  scanGateValidated = false,
) {
  if (!ensureSegmentWritable('报工')) return;
  const existedRecord = findReportBySegment(segment);
  const overwriteConfirmed = String(existedRecord?.reportStatus || '').toUpperCase() === 'CONFIRMED';
  if (existedRecord?.editBlockedReason) {
    openReportViewDialog(group, segment);
    return;
  }
  if (!isWorkOrderRunning.value) {
    AModal.warning({
      okText: '去待加工开工',
      title: '当前压槽工单未开工',
      content: '请先在右上角“待加工”中选择当前计划并执行开工，再进行压槽报工登记。',
      onOk: () => {
        void openTaskList();
      },
    });
    return;
  }
  if (!overwriteConfirmed && !dailyPreparationReady.value) {
    showDailyPreparationRequiredWarning();
    return;
  }
  const selectedReportType: PressSlotReportType = 'PRODUCT';
  recordConfirmForm.reportType = selectedReportType;
  if (!overwriteConfirmed && !scanGateValidated) {
    const scanGatePassed = await validatePressSlotScanGate(group, segment, selectedReportType, scannedBatchNo);
    if (!scanGatePassed) return;
  }
  if (!overwriteConfirmed && !pressSlotConsumablesReady.value) {
    AModal.warning({
      okText: glueBoard.alarm ? '清洗复位' : bearingConsumable.alarm || !bearingConsumable.id ? '更换轴承' : '知道了',
      title: '压辊/轴承未就绪',
      content: glueBoard.alarm || bearingConsumable.alarm || '压槽报工前请先完成压槽辊清洗复位或轴承更换登记。',
      onOk: () => {
        if (glueBoard.alarm) {
          resetPressSlotRollerClean();
        } else if (bearingConsumable.alarm || !bearingConsumable.id) {
          openGlueConsumeDialog('BEARING');
        }
      },
    });
    return;
  }
  try {
    await loadCheckTemplate(currentPlan.modelCode);
  } catch {
    return;
  }
  resetReportForm();
  applyGlueBoardDefaultsToCheckItems();
  applySegmentToReportForm(group, segment, true);
  reportForm.reportType = selectedReportType;
  reportForm.coaFlag = false;
  reportForm.sourceCode = scannedBatchNo || segment.batchNo;
  reportForm.startTime = buildNowText();
  reportForm.endTime = buildNowText();
  activeRecord.value = existedRecord || null;
  reportDialogMode.value = 'confirm';
  activeReportTab.value = getFirstReportTabKey();
  reportVisible.value = true;
  const ok = await scanReportSource();
  if (!ok) {
    reportVisible.value = false;
    return;
  }
  applyReportRecordToForm(existedRecord);
  activeReportTab.value = getFirstReportTabKey();
  focusActiveReportCheckCell();
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

function submitProcessItems() {
  if (reportDialogMode.value === 'view') return;
  if (reportForm.reportType === 'PROCESS_CHECK') {
    void saveAndPushProcessCheck();
    return;
  }
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
  void submitAdhesiveReport();
}

function confirmSwitchToProcessCheck() {
  if (reportForm.reportType === 'PROCESS_CHECK') return Promise.resolve(true);
  return new Promise<boolean>((resolve) => {
    AModal.confirm({
      content: `当前作业类型为“${getPressSlotReportTypeText(reportForm.reportType)}”，保存并推送过程加检会把本次报工按“过程加检”保存。是否继续？`,
      okText: '切换并继续',
      cancelText: '取消',
      onCancel: () => resolve(false),
      onOk: () => {
        reportForm.reportType = 'PROCESS_CHECK';
        message.info('已切换为过程加检，请确认过程加检与外观检验。');
        nextTick(() => {
          ensureActiveReportTab();
          focusActiveReportCheckCell();
        });
        resolve(true);
      },
      title: '确认作业类型',
    });
  });
}

function buildProcessCheckFaiRemark(reportId?: number) {
  return [
    '压槽操作看板过程加检',
    `分段批号 ${currentMotherSegmentBatchNo.value || reportForm.parentBatchNo || '-'}`,
    `来源片号 ${reportForm.sourceProductionBatchNo || '-'}`,
    `压槽片号 ${reportForm.productionBatchNo || '-'}`,
    `报工ID ${reportId || '-'}`,
  ].join('；');
}

async function saveAndPushProcessCheck() {
  if (reportDialogMode.value === 'view') return;
  if (!ensureSegmentWritable('推送过程加检')) return;
  const confirmed = await confirmSwitchToProcessCheck();
  if (!confirmed) return;
  if (shouldShowVisualInspectionTab.value && activeReportTab.value !== 'visual-inspection') {
    activeReportTab.value = 'visual-inspection';
    reportForm.selfCheck = hasActiveVisualItems.value ? 'NG' : (reportForm.selfCheck || 'OK');
    message.info('请确认外观检验后，再次点击保存并推送过程加检。');
    return;
  }
  if (processCheckApplying.value) return;
  processCheckApplying.value = true;
  try {
    reportForm.endTime = reportForm.endTime || buildNowText();
    const existingConfirmedReportId = activeRecord.value?.id && isAdhesiveRecordConfirmed(activeRecord.value.reportStatus)
      ? Number(activeRecord.value.id)
      : undefined;
    const reportId = existingConfirmedReportId || (await submitAdhesiveReport({
      closeAfterSave: false,
      openNextAfterSave: false,
      successMessage: '过程加检报工已保存，正在推送 FAI 检验记录',
    }));
    if (!reportId) return;
    const summary = await applyPressSlotFai({
      inspectionScene: 'PROCESS_CHECK',
      inspectionScopeBatchNo: currentMotherSegmentBatchNo.value || reportForm.parentBatchNo || undefined,
      planId: currentPlan.planId!,
      planOperationId: currentPlan.planOperationId!,
      productBatchNo: reportForm.productionBatchNo || undefined,
      remark: buildProcessCheckFaiRemark(reportId),
      sourceReportId: reportId,
      standardMatchMode: 'PRODUCT_MODEL_PROCESS',
      submitterName: currentUserName.value || undefined,
      triggerReason: 'NEW_ORDER',
    });
    await Promise.all([loadProcessCheckFaiRows(), loadPressSlotAbnormalLockRows(), loadReportsAndSourceGroups(), loadProcessParameters()]);
    reportVisible.value = false;
    activeRecord.value = null;
    clearReportIntermediateDraft();
    activeBoardTab.value = 'PROCESS_CHECK';
    message.success(`过程加检已推送，FAI单号：${summary.faiNo || '-'}`);
    confirmPrintPressSlotInspectionTransferTicket(summary, {
      inspectionType: '过程加检',
      sampleType: '压槽过程加检片',
      successContent: `过程加检已推送，FAI单号：${summary.faiNo || '-'}。是否立即打印检验流转单？`,
    });
  } catch (error: any) {
    AModal.warning({
      content: getErrorMessage(error) || '过程加检推送失败，请检查当前计划、压槽片号和 FAI 检验标准。',
      title: '过程加检未推送',
    });
  } finally {
    processCheckApplying.value = false;
  }
}

function getCheckItemsByCategory(category: string) {
  let rows: AdhesiveCheckItem[] = [];
  if (category === '中间品') {
    rows = pressSlotMiddleCheckItems.value;
  } else if (reportForm.reportType === 'CHANGEOVER') {
    rows = ensurePressSlotChangeoverItems(checkTemplate.value).filter((item) => item.itemCategory === category);
  } else if (reportForm.reportType === 'PROCESS_CHECK') {
    rows = getPressSlotProcessCheckItems().filter((item) => item.itemCategory === category);
  } else {
    rows = getTemperatureCheckItems().filter((item) => item.itemCategory === category);
  }
  return rows
    .filter((item) => item.itemCategory === category)
    .sort((a, b) => Number(a.sortNo || 0) - Number(b.sortNo || 0));
}

function getReportCheckItemsForSave() {
  if (isPressSlotMiddleReportType(reportForm.reportType)) {
    return pressSlotMiddleCheckItems.value;
  }
  const baseItems = reportForm.reportType === 'CHANGEOVER'
    ? ensurePressSlotChangeoverItems(checkTemplate.value)
    : reportForm.reportType === 'PROCESS_CHECK'
      ? getPressSlotProcessCheckItems()
      : getTemperatureCheckItems();
  return baseItems.filter((item) => (item.itemCategory || '其他') !== '工艺参数');
}

function getReportPositionValidationMessage() {
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
  const editingReportId = activeRecord.value?.id;
  const overlapRange = (segment?.reportRanges || []).find(
    (range) => (!editingReportId || range.reportId !== editingReportId) && start < range.end && end > range.start,
  );
  if (overlapRange) {
    return `当前位置与已有报工 ${overlapRange.label}（${formatNumber(overlapRange.start)}-${formatNumber(overlapRange.end)}m）重叠`;
  }
  return '';
}

async function submitAdhesiveReport(options: SubmitPressSlotReportOptions = {}) {
  if (reportDialogMode.value === 'view') return;
  if (reportSubmitting.value) {
    message.info('当前压槽片正在保存，请勿重复点击。');
    return;
  }
  if (!currentPlan.planId || !currentPlan.planOperationId) {
    message.warning('请先扫码带出压槽计划');
    return;
  }
  if (!ensureSegmentWritable('报工')) return;
  if (!isWorkOrderRunning.value) {
    AModal.warning({
      okText: '去待加工开工',
      title: '当前压槽工单未开工',
      content: '请先在右上角“待加工”中选择当前计划并执行开工，再进行压槽报工登记。',
      onOk: () => {
        void openTaskList();
      },
    });
    return;
  }
  if (!reportForm.sourceGrindingSecondDetailId && !(await ensureReportSourceResolvedBeforeSave())) {
    message.warning('请先通过片号扫码接口确认第二次磨皮分段批号');
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
  const overwriteConfirmed = String(activeRecord.value?.reportStatus || '').toUpperCase() === 'CONFIRMED';
  if (!overwriteConfirmed && !pressSlotConsumablesReady.value) {
    AModal.warning({
      okText: glueBoard.alarm ? '清洗复位' : bearingConsumable.alarm || !bearingConsumable.id ? '更换轴承' : '知道了',
      title: '压辊/轴承未就绪',
      content: glueBoard.alarm || bearingConsumable.alarm || '请先在当前看板完成压槽辊清洗复位或轴承更换登记',
      onOk: () => {
        if (glueBoard.alarm) {
          resetPressSlotRollerClean();
        } else if (bearingConsumable.alarm || !bearingConsumable.id) {
          openGlueConsumeDialog('BEARING');
        }
      },
    });
    return;
  }
  const outputLength = reportOutputLength.value;
  const visualItemsForSave = normalizeVisualItems(visualInspectionItems.value);
  const reportSelfCheck = hasActiveVisualItems.value ? 'NG' : (reportForm.selfCheck || 'OK');
  reportForm.selfCheck = reportSelfCheck;
  const extra: Record<string, any> = applyPreProcessSelfCheckAttribution(
    {
      ...(parseRecordExtra(activeRecord.value || {}) as Record<string, any>),
      reportType: reportForm.reportType,
      reportTypeName: getPressSlotReportTypeText(reportForm.reportType),
      visualInspectionRemark: reportForm.remark || '',
      visualInspectionResult: reportSelfCheck,
      visualItems: shouldShowVisualInspectionTab.value ? visualItemsForSave : [],
    },
    reportSelfCheck === 'NG' && reportForm.preProcessSelfCheckAbnormal,
    PRESS_SLOT_PRE_PROCESS_ATTRIBUTION,
  );
  if (reportForm.reportType === 'PROCESS_CHECK') {
    extra.inspectionScene = 'PROCESS_CHECK';
    extra.inspectionSampleCategory = 'PROCESS_CHECK';
    extra.inspectionSampleCategoryName = '加检送检';
  } else if (reportForm.reportType === 'CHANGEOVER') {
    extra.inspectionScene = 'FIRST_INSPECTION';
    extra.inspectionSampleCategory = 'FIRST_INSPECTION';
    extra.inspectionSampleCategoryName = '首检送检';
  } else {
    delete extra.inspectionScene;
    delete extra.inspectionSampleCategory;
    delete extra.inspectionSampleCategoryName;
  }
  reportSubmitting.value = true;
  try {
    const saveResult = await saveAndConfirmPressSlotReport({
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
      aqcStatus: reportForm.aqcStatus || undefined,
      aqcTaskId: reportForm.aqcTaskId,
      glueBoardBatchNo: reportForm.glueBoardBatchNo || undefined,
      glueBoardMaterialCode: reportForm.glueBoardMaterialCode || undefined,
      glueBoardStartPosition: Number(reportForm.glueBoardStartPosition || glueBoard.availableStartPosition || 0),
      glueBoardUsageId: undefined,
      glueBoardUseLength: Number(reportForm.glueBoardUseLength || reportForm.processLength || 0),
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
      extraJson: JSON.stringify(extra),
      reportDate: reportForm.reportDate || dayjs().format('YYYY-MM-DD'),
      selfCheck: reportSelfCheck,
      sourceBatchNo: reportForm.parentBatchNo || undefined,
      sourceGrindingSecondDetailId: reportForm.sourceGrindingSecondDetailId,
      sourceProductionBatchNo: reportForm.sourceProductionBatchNo || undefined,
      sourceType: '扫码母料',
      startTime: reportForm.startTime || buildNowText(),
      confirmerName: currentUserName.value || 'admin',
      confirmerTime: buildNowText(),
      scannedBatchNo: reportForm.productionBatchNo || reportForm.sourceProductionBatchNo || '',
    });
    const reportId = resolveSavedReportId(saveResult, '压槽');
    await loadReports();
    await loadProcessParameters();
    await loadGlueBoardUsage();
    await loadSourceGroups();
    await loadPressSlotFaiSummary();
    await loadPressSlotAbnormalLockRows();
    await loadIntermediateRecord();
    if (options.closeAfterSave !== false) {
      reportVisible.value = false;
      activeRecord.value = null;
      clearReportIntermediateDraft();
    }
    message.success(options.successMessage || '压槽片已扫码确认并点亮，可继续扫描下一片');
    if (options.openNextAfterSave !== false) {
      openSelectedRecordConfirm();
    }
    return reportId;
  } catch (error: any) {
    AModal.warning({
      content: getReportRequestErrorMessage(error, '压槽报工保存或确认失败，请检查登录状态后重试。'),
      title: '压槽扫码确认失败',
    });
    return undefined;
  } finally {
    reportSubmitting.value = false;
  }
}

watch(
  () => [reportForm.lossLength, reportForm.napSampleLength],
  () => {
    reportForm.outputLength = reportOutputLength.value;
  },
);

watch(
  () => [reportForm.reportType, reportCheckCategories.value.join('|'), shouldShowVisualInspectionTab.value],
  () => ensureActiveReportTab(),
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
      reportForm.glueBoardUseLength = reportForm.processLength;
      syncingReportPosition = false;
    }
  },
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
    if (taskListPage.value > maxPage) taskListPage.value = maxPage;
  },
);

watch(scanPlanNo, (value) => schedulePlanScan(value));

function toggleExtendedBoardTabs() {
  showExtendedBoardTabs.value = !showExtendedBoardTabs.value;
  if (!showExtendedBoardTabs.value && ['CHECK', 'RECORDS'].includes(activeBoardTab.value)) {
    activeBoardTab.value = 'SOURCE';
  }
  message.info(showExtendedBoardTabs.value ? '已显示点检/清洁和今日压槽报工记录' : '已隐藏扩展记录页签');
}

watch(showExtendedBoardTabs, (visible) => {
  if (!visible && ['CHECK', 'RECORDS'].includes(activeBoardTab.value)) {
    activeBoardTab.value = 'SOURCE';
  }
});

watch(activeBoardTab, async (tab) => {
  if (tab !== 'SOURCE') {
    closeTransferPrintSelector();
  }
  if (tab === 'PROCESS_CHECK' && currentPlan.planId && currentPlan.planOperationId) {
    await loadProcessCheckFaiRows();
  }
  if (tab === 'ABNORMAL_LOCK' && currentPlan.planId && currentPlan.planOperationId) {
    await loadPressSlotAbnormalLockRows();
  }
  if (tab === 'FIRST_INSPECTION_PROCESS' && currentPlan.planId && currentPlan.planOperationId) {
    await Promise.all([loadChangeoverInspections(), loadProcessCheckFaiRows()]);
    await loadFirstInspectionProcessFormRecords();
  }
  if (tab === 'PROCESS_PARAM' && currentPlan.planId && currentPlan.planOperationId) {
    await loadProcessParameters();
  }
  if (tab === 'MIDDLE_LEDGER' && currentPlan.planId && currentPlan.planOperationId) {
    await loadPressSlotMiddleLedger();
  }
  await nextTick();
  window.dispatchEvent(new Event('resize'));
});

watch(reportRecords, () => {
  const existingIds = new Set(reportRecords.value.map((record) => getTransferReportKey(record)).filter(Boolean));
  selectedTransferReportIds.value = selectedTransferReportIds.value.filter((id) => existingIds.has(id));
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
    void refreshPressSlotWorkbench();
  }
});

onMounted(async () => {
  attachGlobalScannerListener();
  timer = setInterval(() => {
    currentDateTime.value = dayjs().format('YYYY-MM-DD HH:mm:ss');
  }, 1000);
  firstInspectionRefreshTimer = setInterval(() => {
    void refreshFirstInspectionStatus({
      includeChangeover: false,
      includeProcessCheck: true,
      silent: true,
    });
  }, FIRST_INSPECTION_REFRESH_INTERVAL_MS);
  // 型号模板在实际报工时加载，页面初始化不依赖生产点检配置。
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
  if (firstInspectionRefreshTimer) clearInterval(firstInspectionRefreshTimer);
  if (planScanTimer) clearTimeout(planScanTimer);
  detachGlobalScannerListener();
});
</script>

<template>
  <Page auto-content-height :loading="boardLoading">
    <input ref="processParamFileInput" accept=".xls,.xlsx" class="hidden" type="file" @change="handleProcessParamFileChange" />
    <input
      ref="firstInspectionProcessFileInput"
      accept=".xls,.xlsx"
      class="hidden"
      type="file"
      @change="handleFirstInspectionProcessFileChange"
    />
    <input ref="middleLedgerFileInput" accept=".xls,.xlsx" class="hidden" type="file" @change="handleMiddleLedgerFileChange" />
    <div class="adhesive-console" :class="{ 'is-visual-maximized': visualMaximized }">
      <div class="prototype-banner shrink-0 bg-white rounded-xl shadow-sm border border-slate-200 flex p-3 relative overflow-hidden">
        <div class="flex flex-1 items-center gap-4 min-w-0 pl-1">
          <div
            class="console-main-icon w-[60px] h-[60px] bg-gradient-to-br from-cyan-500 to-blue-600 rounded-xl shadow-md flex items-center justify-center shrink-0 text-white"
            title="双击显示/隐藏点检清洁和今日压槽报工记录"
            @dblclick="toggleExtendedBoardTabs"
          >
            <IconifyIcon icon="lucide:layers" class="text-[32px]" />
          </div>
          <div class="console-title-block">
            <div class="console-title-row">
              <span class="console-title-text">压槽操作看板</span>
              <Tag color="processing" class="console-title-tag">压槽1</Tag>
            </div>
            <div class="console-meta-row">
              <div class="console-meta-item console-meta-item--machine">
                <span class="console-meta-label">压槽机台</span>
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
        <div class="inspection-stamp-slot adhesive-aqc-stamp-slot">
          <button
            class="inspection-stamp-side"
            :class="`inspection-stamp-side--${changeoverStampMeta.stampClass}`"
            type="button"
            @click="handleFirstInspectionStampClick"
          >
            <span class="inspection-stamp-content">
              <span>首检</span>
              <strong>{{ changeoverStampMeta.stampText }}</strong>
              <em>{{ changeoverStampMeta.time }}</em>
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
            v-if="!isWorkOrderPaused && !isWorkOrderCancelled"
            class="w-[64px] h-[64px] bg-amber-50 border border-amber-200 text-amber-700 rounded-xl flex flex-col items-center justify-center transition-all"
            :class="isWorkOrderFinished ? 'opacity-60 cursor-not-allowed' : 'cursor-pointer hover:bg-amber-100 hover:shadow-md active:scale-95'"
            @click="requestFirstInspectionApplication"
          >
            <IconifyIcon icon="lucide:badge-plus" class="text-[22px] mb-1 opacity-80" />
            <span class="text-[11px] font-bold">{{ hasActivePressSlotAbnormalLock ? '重新首检' : '首检申请' }}</span>
          </div>
          <div
            v-if="!isWorkOrderPaused && !isWorkOrderCancelled && !requiresRestartFirstInspection"
            class="w-[64px] h-[64px] bg-orange-50 border border-orange-200 text-orange-700 rounded-xl flex flex-col items-center justify-center transition-all"
            :class="isWorkOrderFinished ? 'opacity-60 cursor-not-allowed' : 'cursor-pointer hover:bg-orange-100 hover:shadow-md active:scale-95'"
            @click="requestCumulativeProcessCheckApplication"
          >
            <IconifyIcon icon="lucide:badge-alert" class="text-[22px] mb-1 opacity-80" />
            <span class="text-[11px] font-bold">过程加检</span>
          </div>
          <div
            v-if="!isWorkOrderPaused && !isWorkOrderCancelled"
            class="w-[64px] h-[64px] bg-indigo-50 border border-indigo-200 text-indigo-700 rounded-xl flex flex-col items-center justify-center cursor-pointer hover:bg-indigo-100 hover:shadow-md transition-all active:scale-95"
            @click="openTransferPrintSelector"
          >
            <IconifyIcon icon="lucide:printer" class="text-[22px] mb-1 opacity-80" />
            <span class="text-[11px] font-bold">打印流转单</span>
          </div>
          <div
            v-if="!isWorkOrderPaused && !isWorkOrderCancelled"
            class="w-[64px] h-[64px] bg-sky-50 border border-sky-200 text-sky-700 rounded-xl flex flex-col items-center justify-center transition-all"
            :class="isWorkOrderFinished ? 'opacity-60 cursor-not-allowed' : 'cursor-pointer hover:bg-sky-100 hover:shadow-md active:scale-95'"
            @click="openSelectedRecordConfirm"
          >
            <IconifyIcon icon="lucide:scan-line" class="text-[22px] mb-1 opacity-80" />
            <span class="text-[11px] font-bold">扫码确认</span>
          </div>
          <div
            v-if="!isWorkOrderPaused && !isWorkOrderCancelled"
            class="w-[64px] h-[64px] bg-emerald-50 border border-emerald-200 text-emerald-700 rounded-xl flex flex-col items-center justify-center transition-all"
            :class="isWorkOrderFinished ? 'opacity-60 cursor-not-allowed' : 'cursor-pointer hover:bg-emerald-100 hover:shadow-md active:scale-95'"
            @click="openSegmentCompleteDialog"
          >
            <IconifyIcon icon="lucide:circle-check-big" class="text-[22px] mb-1 opacity-80" />
            <span class="text-[11px] font-bold">本段完工</span>
          </div>
          <div
            v-if="!isWorkOrderPaused && !isWorkOrderCancelled"
            class="w-[64px] h-[64px] bg-teal-50 border border-teal-200 text-teal-700 rounded-xl flex flex-col items-center justify-center cursor-pointer hover:bg-teal-100 hover:shadow-md transition-all active:scale-95"
            @click="openReportRecordList"
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
              v-model:value="scanPlanNo"
              allow-clear
              class="scan-input"
              placeholder="请扫码或输入计划号，达到长度后自动查询"
              @press-enter="consumePlanScan(false)"
            />
          </div>
          <label>计划号</label><strong>{{ currentPlan.planNo || '请扫码计划号' }}</strong>
          <label>分段批号</label><strong>{{ currentPlan.sourceBatchNo || currentPlan.batchNo || resolveMotherSegmentBatchNo(currentPlan.sourceProductionBatchNo) || '-' }}</strong>
          <label>产品型号</label><strong>{{ currentPlan.modelCode || '-' }}</strong>
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
          <IconifyIcon icon="lucide:package-search" />
          压辊/轴承状态
          <Tag :color="glueBoardStatusMeta.color">{{ glueBoardStatusMeta.text }}</Tag>
          <Button size="small" danger class="consumable-replace-btn" :disabled="isWorkOrderPaused || isWorkOrderCancelled" @click="resetPressSlotRollerClean">清洗复位</Button>
          <Button size="small" type="primary" class="consumable-replace-btn" :disabled="isWorkOrderPaused || isWorkOrderCancelled" @click="openGlueConsumeDialog('BEARING')">更换轴承</Button>
          <span class="glue-board-title-tip" :class="{ warning: !!glueBoard.alarm || !!bearingConsumable.alarm || hasConsumableLifecycleWarning }">
            {{ pressSlotConsumableTitleMessage }}
          </span>
        </div>
        <div class="glue-board-layout glue-board-layout--plain">
          <div class="glue-board-form-grid">
            <div class="glue-board-field">
              <label>压辊累计片数</label>
              <strong>{{ formatNumber(glueBoard.todayUsedCount) }} / {{ formatNumber(glueBoard.lifetimeLimitCount || 2000) }} 片</strong>
            </div>
            <div class="glue-board-field">
              <label>累计压辊时间</label>
              <strong>{{ formatNumber(glueBoard.useDays) }} / {{ formatNumber(glueBoard.limitDays) }} 天</strong>
            </div>
            <div class="glue-board-field glue-board-field--highlight">
              <label>轴承累计片数</label>
              <strong>{{ formatNumber(bearingConsumable.todayUsedCount) }} / {{ formatNumber(bearingConsumable.lifetimeLimitCount || 5000) }} 片</strong>
            </div>
            <div class="glue-board-field">
              <label>累计轴承时间</label>
              <strong>{{ formatNumber(bearingConsumable.useDays) }} / {{ formatNumber(bearingConsumable.limitDays) }} 天</strong>
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
                  :options="pressSlotFilterStatusOptions"
                  size="small"
                  @change="setPressSlotFilterStatus"
                />
                <Select
                  v-model:value="visualFilterForm.reportType"
                  class="panel-filter-select panel-filter-select--type"
                  :options="pressSlotReportTypeFilterOptions"
                  size="small"
                  @change="setPressSlotFilterReportType"
                />
                <Button
                  class="panel-filter-refresh"
                  size="small"
                  :loading="workbenchRefreshing"
                  @click="refreshPressSlotWorkbench"
                >
                  刷新
                </Button>
                <Button
                  class="panel-filter-refresh"
                  size="small"
                  :disabled="isWorkOrderFinished || !reportRecords.length"
                  :loading="middleAutoMarking"
                  @click="openAutoMarkPressSlotMiddleSegments"
                >
                  标记中间品
                </Button>
              </div>
              <div class="panel-metrics">
                <span>片数 {{ visiblePressSlotSliceCount }}</span>
                <span>待确认 {{ visiblePressSlotPendingCount }}</span>
                <span>本工序NG {{ visiblePressSlotCurrentNgCount }}</span>
                <span>分切工序NG {{ visiblePressSlotOtherNgCount }}</span>
                <span>已确认 {{ visiblePressSlotCompletedCount }}</span>
              </div>
            </div>
            <div v-if="requiresRestartFirstInspection" class="press-slot-abnormal-alert">
              <IconifyIcon icon="lucide:shield-alert" />
              <span>检验已返回 NG，请使用新片重新首检。送检成功即可继续报工；再次 NG 时需再次送首检。</span>
              <Button size="small" danger @click="requestFirstInspectionApplication">重新首检</Button>
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
              {{ currentPlan.planNo ? '暂无工作台片信息，请先完成分切扫码确认后刷新。' : '请先扫描计划号，系统会按后台已确认分切片号加载工作台。' }}
            </div>
            <div v-else-if="!visualDisplaySourceGroups.length" class="empty-hint">
              <IconifyIcon icon="lucide:filter-x" />
              当前过滤条件下没有匹配的工作台片。
            </div>
            <div
              v-else
              class="cloth-list press-slot-cloth-list"
              :class="{
                'is-one-click-select-mode': !!selectedOneClickPressSlotBatchNo,
                'is-print-select-mode': transferPrintSelectionMode,
              }"
            >
              <div v-for="group in visualDisplaySourceGroups" :key="group.baseBatchNo" class="cloth-source press-slot-cloth-source">
                <div class="cloth-source-header">
                  <div>
                    <strong>{{ group.baseBatchNo }}</strong>
                    <span>来源 {{ group.planNo || currentPlan.planNo || '-' }}</span>
                    <Tag v-if="group.qtime" :color="getPressSlotQtimeColor(group.qtime)" :title="group.qtime.message">
                      本批 QTIME {{ getPressSlotQtimeText(group.qtime) }}
                    </Tag>
                  </div>
                  <div class="press-slot-card-legend" aria-label="压槽卡片图示说明">
                    <span><i class="is-ok"></i>正常/确认</span>
                    <span><i class="is-first-ok"></i>首检正常</span>
                    <span><i class="is-release-ok"></i>加检放行</span>
                    <span><i class="is-scan-pending"></i>待确认</span>
                    <span><i class="is-pending"></i>待检</span>
                    <span><i class="is-ng"></i>工序NG</span>
                    <span><i class="is-abnormal-lock"></i>异常锁定</span>
                    <span><i class="is-middle"></i>中间品</span>
                  </div>
                  <div class="source-actions">
                    <template v-if="transferPrintSelectionMode">
                      <Button
                        size="small"
                        @click="selectGroupTransferReports(group)"
                      >
                        全选本组
                      </Button>
                    </template>
                    <template v-else>
                      <span
                        v-if="isPressSlotOneClickGroupSelected(group)"
                        class="source-selection-count"
                      >
                        已选 {{ getPressSlotOneClickSelectedCount(group) }} 片
                      </span>
                      <Button
                        size="small"
                        :type="
                          isPressSlotOneClickGroupSelected(group)
                            ? 'primary'
                            : 'default'
                        "
                        :disabled="!!oneClickScanConfirmingBatchNo"
                        @click="selectPressSlotOneClickGroup(group)"
                      >
                        {{
                          isPressSlotOneClickGroupSelected(group)
                            ? '取消选中'
                            : '选中'
                        }}
                      </Button>
                      <Button
                        size="small"
                        type="primary"
                        :disabled="
                          !isPressSlotOneClickGroupSelected(group) ||
                          getPressSlotOneClickSelectedCount(group) === 0
                        "
                        :loading="
                          isSameBatchNo(
                            oneClickScanConfirmingBatchNo,
                            group.baseBatchNo,
                          )
                        "
                        :title="getPressSlotOneClickConfirmTitle(group)"
                        @click="handlePressSlotOneClickScanConfirm(group)"
                      >
                        <IconifyIcon icon="lucide:scan-line" />
                        一键扫码报工
                      </Button>
                    </template>
                  </div>
                </div>
                <div class="cloth-source-body">
                  <div class="slice-grid press-slot-slice-grid" :style="{ gridTemplateColumns: getPressSlotGridColumns(group.segments.length) }">
                    <div
                      v-for="segment in group.segments"
                      :key="segment.batchNo"
                      :class="getPressSlotSliceClass(segment)"
                      role="button"
                      tabindex="0"
                      @click="handleVisualSegmentClick(group, segment)"
                      @keydown.enter="handleVisualSegmentClick(group, segment)"
                    >
                      <div class="slice-cell__head">
                        <span class="slice-cell__batch-wrap">
                          <span
                            v-if="
                              transferPrintSelectionMode ||
                              isPressSlotOneClickGroupSelected(group)
                            "
                            class="slice-select-mark"
                          >
                            <IconifyIcon
                              :icon="
                                getPressSlotSelectionMarkIcon(group, segment)
                              "
                            />
                          </span>
                          <strong class="slice-cell__batch">{{ segment.batchNo }}</strong>
                          <span
                            v-if="getPressSlotSegmentEditBlockedReason(segment)"
                            class="slice-edit-lock"
                            :title="getPressSlotSegmentEditBlockedReason(segment)"
                            aria-label="后道工序已报工或送检，当前压槽记录不可修改"
                          >
                            <IconifyIcon icon="lucide:lock-keyhole" />
                          </span>
                        </span>
                      </div>
                      <div class="slice-cell__info">
                        <span
                          v-if="shouldShowPressSlotScanConfirmLine(segment)"
                          class="slice-cell__line"
                          :class="getPressSlotScanConfirmLineClass(segment)"
                        >
                          <b>扫码</b><em>{{ getPressSlotScanConfirmText(segment) }}</em>
                        </span>
                        <span
                          v-if="shouldShowPressSlotSelfCheckLine(segment)"
                          class="slice-cell__line"
                          :class="getPressSlotNgLineClass(segment)"
                          :title="getPressSlotSelfCheckTitle(segment)"
                        >
                          <b>{{ getPressSlotSelfCheckLabel(segment) }}</b><em>{{ getPressSlotNgStatusText(segment) }}</em>
                        </span>
                        <span
                          v-for="entry in getPressSlotSegmentInspectionEntries(segment)"
                          :key="`${segment.batchNo}-${entry.label}-${entry.resultText}`"
                          class="slice-cell__line"
                          :class="entry.tone"
                        >
                          <b>{{ entry.label }}</b><em>{{ entry.resultText }}</em>
                        </span>
                        <span
                          v-if="shouldShowPressSlotAbnormalLockLine(segment)"
                          class="slice-cell__line"
                          :class="getPressSlotAbnormalLockLineClass(segment)"
                        >
                          <b>异常</b><em>{{ getPressSlotAbnormalLockText(segment) }}</em>
                        </span>
                        <span v-if="getPressSlotSegmentPositionText(segment)" class="slice-cell__line is-middle">
                          <b>中间品</b><em>{{ getPressSlotSegmentPositionText(segment) }}</em>
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
        <TabPane key="CHANGEOVER" tab="首检送检单">
          <div class="tab-table-content">
            <div class="console-table-shell">
              <div class="rough-grid-toolbar console-record-toolbar">
                <div class="rough-grid-toolbar__title">FAI首检联动</div>
                <div class="first-inspection-toolbar-status">
                  <span>首检状态</span>
                  <Tag :color="firstInspectionSummaryStatusMeta.color">
                    {{ changeoverStampMeta.stampText }}
                  </Tag>
                  <strong>分段批号：{{ firstInspection.inspectionScopeBatchNo || currentMotherSegmentBatchNo || '-' }}</strong>
                  <strong>检验日期：{{ firstInspection.inspectionDate || currentDateText }}</strong>
                  <Button size="small" :loading="firstInspectionRefreshing" @click="refreshFirstInspectionStatus()">刷新</Button>
                  <Button size="small" :disabled="!firstInspection.faiId" @click="printFirstInspectionTransferTicket">打印检验流转单</Button>
                </div>
              </div>
              <div class="console-table-body">
                <ATable
                  class="rough-check-table console-record-table"
                  :columns="changeoverInspectionColumns"
                  :data-source="firstInspectionRows"
                  :pagination="false"
                  :scroll="{ x: 1260, y: 220 }"
                  row-key="id"
                  size="small"
                >
                  <template #bodyCell="{ column, record }">
                    <template v-if="column.dataIndex === 'inspectionStatus'">
                      <Tag :color="getFirstInspectionStatusMeta(record.faiStatus, record.faiJudgment).color">
                        {{ getFirstInspectionStatusMeta(record.faiStatus, record.faiJudgment).text }}
                      </Tag>
                    </template>
                    <template v-if="column.dataIndex === 'action'">
                      <Button size="small" type="link" :disabled="!record.faiId" @click="viewFirstInspectionDetail(record)">查看</Button>
                      <Button size="small" type="link" :disabled="!record.faiId" @click="printFirstInspectionTransferTicket(record)">打印检验流转单</Button>
                      <Button
                        danger
                        size="small"
                        type="link"
                        :disabled="!canWithdrawPressSlotFai(record) || withdrawingFaiId === record.faiId"
                        :loading="withdrawingFaiId === record.faiId"
                        title="仅品质未扫码、未录入、未提交的待检单可撤回"
                        @click="requestPressSlotFaiWithdrawal(record, '首检')"
                      >
                        撤回
                      </Button>
                    </template>
                  </template>
                </ATable>
              </div>
            </div>
          </div>
        </TabPane>
        <TabPane key="FIRST_INSPECTION_PROCESS" tab="生产点检">
          <div class="tab-table-content">
            <div class="console-table-shell">
              <div class="rough-grid-toolbar console-record-toolbar">
                <div class="rough-grid-toolbar__title">生产点检</div>
                <div class="pass-work-actions rough-grid-toolbar__actions">
                  <span class="console-table-count">
                    当前分段批号 {{ getCurrentChangeoverMotherBatchNo() || '-' }} / 记录 {{ firstInspectionProcessRows.length }} 条
                  </span>
                  <Button size="small" :loading="firstInspectionRefreshing" @click="refreshFirstInspectionStatus()">刷新</Button>
                </div>
              </div>
              <div class="console-table-body">
                <ATable
                  class="rough-check-table console-record-table"
                  :columns="firstInspectionProcessColumns"
                  :data-source="firstInspectionProcessRows"
                  :pagination="false"
                  :scroll="{ x: 1150, y: 220 }"
                  row-key="rowKey"
                  size="small"
                >
                  <template #bodyCell="{ column, record }">
                    <template v-if="column.dataIndex === 'inspectionResultText'">
                      <Tag
                        v-if="record.inspectionResultText && record.inspectionResultText !== '-'"
                        :color="getFirstInspectionProcessResultColor(record.inspectionResultText)"
                      >
                        {{ record.inspectionResultText }}
                      </Tag>
                      <span v-else>-</span>
                    </template>
                    <template v-if="column.dataIndex === 'processFormStatus'">
                      <Tag :color="getFirstInspectionProcessFormStatus(record.processFormRecord).color">
                        {{ getFirstInspectionProcessFormStatus(record.processFormRecord).text }}
                      </Tag>
                    </template>
                    <template v-if="column.dataIndex === 'action'">
                      <Button
                        size="small"
                        type="link"
                        :disabled="isWorkOrderFinished && !record.processFormRecord?.id"
                        @click="openFirstInspectionProcessForm(record)"
                      >
                        {{
                          isWorkOrderFinished
                            ? `查看${getFirstInspectionProcessFormDisplayName(record)}`
                            : record.processFormRecord?.id
                              ? `修改${getFirstInspectionProcessFormDisplayName(record)}`
                              : `填写${getFirstInspectionProcessFormDisplayName(record)}`
                        }}
                      </Button>
                    </template>
                  </template>
                </ATable>
              </div>
            </div>
          </div>
        </TabPane>
        <TabPane key="MIDDLE_LEDGER" tab="中间品">
          <div class="tab-table-content">
            <div class="console-table-shell">
              <div class="rough-grid-toolbar console-record-toolbar">
                <div class="rough-grid-toolbar__title">压槽中间品记录表</div>
                <div class="pass-work-actions rough-grid-toolbar__actions">
                  <span class="console-table-count">记录 {{ pressSlotIntermediateRecords.length }} 张</span>
                  <Button size="small" type="primary" :disabled="isWorkOrderFinished" :loading="intermediateLoading" @click="openNewIntermediateRecordDetail">新建</Button>
                  <Button size="small" :loading="intermediateLoading" @click="loadPressSlotMiddleLedger">刷新</Button>
                </div>
              </div>
              <div class="console-table-body">
                <ATable
                  class="rough-check-table console-record-table"
                  :columns="pressSlotIntermediateRecordColumns"
                  :data-source="pressSlotIntermediateRecords"
                  :loading="intermediateLoading"
                  :pagination="false"
                  :scroll="{ x: 1450, y: 220 }"
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
                      <Button size="small" type="link" @click="openIntermediateRecordDetail(record)">
                        {{ isWorkOrderFinished ? '查看' : '查看/填写' }}
                      </Button>
                    </template>
                  </template>
                </ATable>
              </div>
            </div>
          </div>
        </TabPane>
        <TabPane key="PROCESS_PARAM" tab="工艺参数上报">
          <div class="tab-table-content">
            <div class="console-table-shell">
              <div class="rough-grid-toolbar console-record-toolbar">
                <div class="rough-grid-toolbar__title">工艺参数上报</div>
                <div class="pass-work-actions rough-grid-toolbar__actions">
                  <DatePicker
                    v-model:value="processParamFilters.reportDate"
                    allow-clear
                    class="process-param-filter process-param-filter--date"
                    placeholder="日期"
                    size="small"
                    value-format="YYYY-MM-DD"
                    @change="loadProcessParameters"
                  />
                  <Input
                    v-model:value="processParamFilters.productionBatchNo"
                    allow-clear
                    class="process-param-filter process-param-filter--slice"
                    placeholder="压槽片号"
                    size="small"
                    @press-enter="loadProcessParameters"
                  />
                  <Button size="small" @click="loadProcessParameters">查询</Button>
                  <Button size="small" @click="resetProcessParamFilters()">重置</Button>
                  <span class="console-table-count">记录 {{ processParameterRows.length }} 条</span>
                  <Button size="small" type="primary" :disabled="isWorkOrderFinished" @click="openProcessParamEdit()">上报工艺参数</Button>
                </div>
              </div>
              <div class="console-table-body">
                <ATable
                  class="rough-check-table console-record-table"
                  :columns="processParameterColumns"
                  :custom-row="(record) => ({ onDblclick: () => openProcessParamEdit(record) })"
                  :data-source="processParameterRows"
                  :pagination="false"
                  :scroll="{ x: 1580, y: 220 }"
                  row-key="id"
                  size="small"
                >
                  <template #bodyCell="{ column, record }">
                    <template v-if="column.dataIndex === 'recordStatus'">
                      <Tag :color="getRecordStatusMeta(record.recordStatus).color">
                        {{ record.statusName || getRecordStatusMeta(record.recordStatus).text }}
                      </Tag>
                    </template>
                    <template v-if="column.dataIndex === 'action'">
                      <Button size="small" type="link" @click="openProcessParamEdit(record)">查看</Button>
                    </template>
                  </template>
                </ATable>
              </div>
            </div>
          </div>
        </TabPane>
        <TabPane key="PROCESS_CHECK" tab="过程加检">
          <div class="tab-table-content">
            <div class="console-table-shell">
              <div class="rough-grid-toolbar console-record-toolbar">
                <div class="rough-grid-toolbar__title">过程加检记录</div>
                <div class="pass-work-actions rough-grid-toolbar__actions">
                  <span class="console-table-count">记录 {{ processCheckInspectionRows.length }} 条</span>
                  <Button size="small" @click="loadProcessCheckFaiRows">刷新</Button>
                  <Button
                    size="small"
                    :disabled="!latestProcessCheckInspection?.faiId"
                    @click="printLatestProcessCheckInspectionTransferTicket"
                  >
                    打印检验流转单
                  </Button>
                </div>
              </div>
              <div class="console-table-body">
                <ATable
                  class="rough-check-table console-record-table"
                  :columns="processCheckInspectionColumns"
                  :data-source="processCheckInspectionRows"
                  :pagination="false"
                  :scroll="{ x: 1430, y: 220 }"
                  row-key="id"
                  size="small"
                >
                  <template #bodyCell="{ column, record }">
                    <template v-if="column.dataIndex === 'inspectionStatus'">
                      <Tag :color="getFirstInspectionStatusMeta(record.faiStatus, record.faiJudgment).color">
                        {{ getFirstInspectionStatusMeta(record.faiStatus, record.faiJudgment).text }}
                      </Tag>
                    </template>
                    <template v-if="column.dataIndex === 'action'">
                      <Button size="small" type="link" :disabled="!record.faiId" @click="viewProcessCheckFaiDetail(record)">查看</Button>
                      <Button size="small" type="link" :disabled="!record.faiId" @click="printProcessCheckInspectionTransferTicket(record)">打印检验流转单</Button>
                      <Button
                        danger
                        size="small"
                        type="link"
                        :disabled="!canWithdrawPressSlotFai(record) || withdrawingFaiId === record.faiId"
                        :loading="withdrawingFaiId === record.faiId"
                        title="仅品质未扫码、未录入、未提交的待检单可撤回"
                        @click="requestPressSlotFaiWithdrawal(record, '过程加检')"
                      >
                        撤回
                      </Button>
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
                    <Button size="small" type="link" :disabled="!canFillDailyRecord(toDailyRecord(record))" @click="openDailyRecord(toDailyRecord(record), 'edit')">
                      填写
                    </Button>
                    <Button size="small" type="link" :disabled="!canConfirmDailyRecord(toDailyRecord(record))" @click="openDailyRecord(toDailyRecord(record), 'confirm')">
                      确认
                    </Button>
                    <Button size="small" type="link" :disabled="!canViewDailyRecord(toDailyRecord(record))" @click="openDailyRecord(toDailyRecord(record), 'view')">查看</Button>
                  </div>
                </template>
              </template>
            </ATable>
          </div>
    </TabPane>
    <TabPane v-if="showExtendedBoardTabs" key="RECORDS" tab="今日压槽报工记录">
      <div class="tab-table-content">
        <div class="console-table-shell">
          <div class="rough-grid-toolbar console-record-toolbar">
            <div class="rough-grid-toolbar__title">今日压槽报工记录</div>
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
              <Button size="small" :disabled="isWorkOrderFinished || !reportRecords.length" @click="openSelectedRecordConfirm">扫码确认</Button>
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
              :scroll="{ x: 920, y: 176 }"
              row-key="id"
              size="small"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'motherBatchNo'">
                  {{ resolveMotherSegmentBatchNo(record.parentProductionBatchNo || record.sourceBatchNo || record.sourceProductionBatchNo || '') || '-' }}
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
    <TabPane key="INSTRUCTION_MESSAGES">
      <template #tab>
        <Badge :count="productionInstructionUnreadCount" :offset="[8, -4]" :overflow-count="99" size="small">
          <span>指令消息</span>
        </Badge>
      </template>
      <ProductionInstructionMessageTab
        :context="productionInstructionContext"
        title="压槽指令消息"
        @unread-change="handleProductionInstructionUnreadChange"
      />
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
              <div class="rough-grid-toolbar__title">压槽中间品记录表</div>
              <div class="pass-work-actions rough-grid-toolbar__actions">
                <span class="console-table-count">
                  批号 {{ intermediateForm.batchNo || '-' }} / 明细 {{ intermediateTableRows.length }} 行
                </span>
                <Tag :color="intermediateRecordStatusMeta.color">
                  {{ intermediateRecordStatusMeta.text }}
                </Tag>
                <Button size="small" :disabled="isIntermediateRecordReadonly" :loading="middleLedgerImporting" @click="triggerMiddleLedgerImport">导入EXCEL</Button>
                <Button size="small" @click="exportMiddleLedgerExcel">导出EXCEL</Button>
                <Button size="small" :loading="intermediateLoading" @click="loadIntermediateRecord(selectedIntermediateRecord || undefined)">刷新</Button>
                <Button size="small" type="primary" :disabled="isIntermediateRecordReadonly" :loading="intermediateLoading" @click="requestSaveIntermediateRecord">保存</Button>
                <Button size="small" type="primary" :disabled="isIntermediateRecordReadonly" :loading="intermediateLoading" @click="requestConfirmIntermediateRecord">确认</Button>
                <Button size="small" @click="closeIntermediateRecordDetail">关闭</Button>
              </div>
            </div>
            <div
              class="press-slot-middle-body"
              :class="{ 'press-slot-middle-body--no-depth': !showIntermediateSlotDepthSection }"
            >
              <div class="press-slot-middle-sheet">
                <div class="press-slot-section-title">
                  <span>主表与采样片号</span>
                  <em>新建时按当前批号报工详情已设置的前段 / 中段 / 后段带入</em>
                </div>
                <div class="press-slot-middle-form">
                  <label>生产日期</label>
                  <div class="middle-product-header-input">
                    <DatePicker v-model:value="intermediateForm.recordDate" :disabled="isIntermediateRecordReadonly" value-format="YYYY-MM-DD" size="small" />
                  </div>
                  <label>型号</label><strong>{{ intermediateForm.modelCode || '-' }}</strong>
                  <label>料号</label><strong>{{ intermediateForm.materialCode || '-' }}</strong>
                  <label>批号</label><strong>{{ intermediateForm.batchNo || '-' }}</strong>
                  <label>填写人</label>
                  <strong>{{ intermediateForm.recorderName || '-' }}</strong>
                  <label>填写时间</label><strong>{{ displayDateTimeText(intermediateForm.fillTime || intermediateForm.updateTime || intermediateForm.createTime) }}</strong>
                  <label>确认人</label>
                  <strong>{{ intermediateForm.confirmerName || '-' }}</strong>
                  <label>确认时间</label><strong>{{ displayDateTimeText(intermediateForm.confirmTime) }}</strong>
                </div>
              </div>
              <div v-if="showIntermediateSlotDepthSection" class="press-slot-middle-sheet">
                <div class="press-slot-section-title">
                  <span>首件槽深/mm（标准：{{ intermediateSlotDepthStandardText }}）</span>
                  <em>首检批号对应的槽深最小值、最大值、平均值</em>
                </div>
                <div class="press-slot-depth-matrix">
                  <div class="press-slot-depth-cell press-slot-depth-cell--head">XY最小值</div>
                  <div class="press-slot-depth-cell press-slot-depth-cell--head">XY最大值</div>
                  <div class="press-slot-depth-cell press-slot-depth-cell--head">XY平均值</div>
                  <InputNumber v-model:value="intermediateForm.firstSlotDepthMin" class="full-input" :disabled="isIntermediateRecordReadonly" :precision="3" size="small" />
                  <InputNumber v-model:value="intermediateForm.firstSlotDepthMax" class="full-input" :disabled="isIntermediateRecordReadonly" :precision="3" size="small" />
                  <InputNumber v-model:value="intermediateForm.firstSlotDepthAvg" class="full-input" :disabled="isIntermediateRecordReadonly" :precision="3" size="small" />
                </div>
              </div>
              <div class="press-slot-middle-attachment">
                <div class="press-slot-middle-attachment__title">原始导入附件</div>
                <div v-if="intermediateImportAttachment" class="press-slot-middle-attachment__content">
                  <button class="press-slot-middle-attachment__link" type="button" @click="openPressSlotImportAttachment(intermediateImportAttachment)">
                    <IconifyIcon icon="lucide:paperclip" />
                    <span>{{ intermediateImportAttachment.name || '原始导入文件' }}</span>
                  </button>
                  <span>导入时间：{{ intermediateImportAttachment.uploadTime || '-' }}</span>
                  <span v-if="formatPressSlotAttachmentSize(intermediateImportAttachment.size)">
                    大小：{{ formatPressSlotAttachmentSize(intermediateImportAttachment.size) }}
                  </span>
                </div>
                <span v-else class="press-slot-middle-attachment__empty">暂无原始导入附件</span>
              </div>
              <div class="press-slot-middle-sheet press-slot-middle-sheet--table">
                <div class="press-slot-section-title">
                  <span>前段 / 中段 / 后段宽幅与厚度测量</span>
                  <em>每片每 {{ intermediateThicknessIntervalCm }}cm 测量一次厚度，共 {{ intermediateThicknessColumnCount }} 个测量点；厚度标准：{{ intermediateThicknessStandardText }}mm</em>
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
                      :disabled="isIntermediateRecordReadonly"
                      size="small"
                      @change="syncIntermediateSliceNoFieldsFromDetails"
                      @keydown="handleIntermediateCellKeydown"
                    />
                  </template>
                  <template v-if="column.dataIndex === 'widthMm'">
                    <InputNumber v-model:value="record.widthMm" class="adhesive-intermediate-cell full-input" :disabled="isIntermediateRecordReadonly" :min="0" :precision="3" size="small" @keydown="handleIntermediateCellKeydown" />
                  </template>
                  <template v-if="isIntermediateThicknessColumn(column.dataIndex)">
                    <InputNumber
                      :value="getIntermediateNumericValue(record, column.dataIndex)"
                      class="adhesive-intermediate-cell full-input"
                      :disabled="isIntermediateRecordReadonly"
                      :min="0"
                      :precision="3"
                      size="small"
                      @change="(value) => setIntermediateNumericValue(record, column.dataIndex, value as number | null)"
                      @keydown="handleIntermediateCellKeydown"
                    />
                  </template>
                  <template v-if="column.dataIndex === 'remark'">
                    <Input v-model:value="record.remark" class="adhesive-intermediate-cell" :disabled="isIntermediateRecordReadonly" size="small" @keydown="handleIntermediateCellKeydown" />
                  </template>
                </template>
              </ATable>
                </div>
              </div>
            </div>
          </div>
        </div>
      </AModal>

      <AModal v-model:open="taskListVisible" :footer="null" :width="1280" class="rough-prototype-modal" :title="pressSlotTaskListTitle">
        <div class="console-table-shell console-table-shell--modal task-list-table-shell">
          <div class="rough-grid-toolbar console-record-toolbar">
            <div class="rough-grid-toolbar__title">{{ pressSlotTaskListTitle }}</div>
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
                placeholder="请输入分段批号"
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
              row-key="id"
              size="small"
              :custom-row="(record) => ({ onDblclick: () => startTaskFromList(toTaskItem(record)) })"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.dataIndex === 'sourceBatchNo'">
                  {{ record.sourceBatchNo || '-' }}
                </template>
            <template v-if="column.dataIndex === 'availableSourceLength'">
              {{ formatNumber(record.availableSourceLength) }}
            </template>
            <template v-if="column.dataIndex === 'status'">
              <Tag :color="getWorkOrderStatusMeta(record.status).color">{{ getWorkOrderStatusMeta(record.status).text }}</Tag>
                </template>
                <template v-if="column.dataIndex === 'action'">
                  <Button size="small" type="link" @click="startTaskFromList(toTaskItem(record))">
                    {{ getTaskListActionText(record.status) }}
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
        :width="820"
        class="rough-prototype-modal"
        title="选择压槽设备"
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

      <AModal
        v-model:open="firstInspectionProcessFormVisible"
        :body-style="{ height: '100vh', overflow: 'hidden', padding: '12px' }"
        :closable="false"
        :footer="null"
        :title="null"
        width="100vw"
        class="rough-prototype-modal"
        destroy-on-close
        wrap-class-name="first-inspection-process-modal-wrap"
      >
        <div class="first-inspection-process-form">
          <div v-if="firstInspectionProcessFormLoading" class="console-empty compact">正在解析{{ getFirstInspectionProcessFormDisplayName(currentFirstInspectionProcessRow || undefined) }}...</div>
          <template v-else>
            <div class="first-inspection-process-action-bar">
              <div class="first-inspection-process-action-bar__title">{{ firstInspectionProcessModalTitle }}</div>
              <div class="first-inspection-process-action-bar__actions">
                <Button :loading="firstInspectionProcessFormSaving" @click="exportFirstInspectionProcessExcel">导出Excel</Button>
                <Button
                  :disabled="isFirstInspectionProcessFormReadonly"
                  :loading="firstInspectionProcessImporting"
                  @click="triggerFirstInspectionProcessImport"
                >
                  导入Excel
                </Button>
                <Button @click="firstInspectionProcessFormVisible = false">
                  {{ isFirstInspectionProcessFormReadonly ? '关闭' : '取消' }}
                </Button>
                <Button
                  :disabled="isFirstInspectionProcessFormReadonly"
                  :loading="firstInspectionProcessFormSaving"
                  type="primary"
                  @click="requestSaveFirstInspectionProcessForm"
                >
                  保存
                </Button>
                <Button
                  :disabled="isFirstInspectionProcessFormReadonly"
                  :loading="firstInspectionProcessFormConfirming"
                  type="primary"
                  @click="requestConfirmFirstInspectionProcessForm"
                >
                  确认
                </Button>
              </div>
            </div>
            <StationFormRuntimeRenderer
              ref="productionCheckRuntime"
              class="first-inspection-process-runtime"
              :header-data="firstInspectionProcessHeader"
              @update:header-data="updateProductionCheckHeader"
              :schema="productionCheckFillSchema"
              :items="productionCheckRuntimeItems"
              :readonly="isFirstInspectionProcessFormReadonly"
              :form-name="firstInspectionProcessRecord.formName"
            />
            <div class="modal-footer">
              <div class="first-inspection-process-confirm-info">
                <Tag :color="String(firstInspectionProcessRecord.recordStatus || '').toUpperCase() === 'CONFIRMED' ? 'success' : 'default'">
                  {{ getFirstInspectionProcessRecordStatusText(firstInspectionProcessRecord.recordStatus) }}
                </Tag>
                <div class="first-inspection-process-info-item editable">
                  <span>记录人：</span>
                  <Input v-model:value="firstInspectionProcessHeader.recorderName" :disabled="isFirstInspectionProcessFormReadonly" allow-clear size="small" />
                </div>
                <div class="first-inspection-process-info-item">
                  记录时间：{{ displayDateTimeText(firstInspectionProcessRecord.fillTime) }}
                </div>
                <div class="first-inspection-process-info-item">确认人：{{ firstInspectionProcessRecord.confirmUserName || '-' }}</div>
                <div class="first-inspection-process-info-item">确认时间：{{ displayDateTimeText(firstInspectionProcessRecord.confirmTime) }}</div>
              </div>
            </div>
          </template>
        </div>
      </AModal>

      <AModal
        v-model:open="processParamEditVisible"
        :footer="null"
        :title="null"
        width="100vw"
        class="rough-prototype-modal"
        destroy-on-close
        wrap-class-name="hc-pass-work-modal rough-report-work-modal process-param-sheet-modal"
        @cancel="processParamEditVisible = false"
      >
        <div class="process-param-sheet">
          <div class="report-modal-toolbar process-param-sheet__toolbar">
            <div class="process-param-sheet__title">
              {{ normalizeRecordId(processParamEditForm.id) ? '查看CMP压槽工艺参数表' : '填写CMP压槽工艺参数表' }}
            </div>
            <div class="process-param-sheet__meta">
              <span>计划号：{{ processParamEditForm.planNo || '-' }}</span>
              <span>分段批号：{{ processParamEditForm.motherBatchNo || '-' }}</span>
              <span>明细数：{{ normalizeProcessParamItems(processParamEditForm.items).length }}</span>
            </div>
            <div class="process-param-sheet__actions">
              <Button size="small" :loading="processParamEditSaving" @click="exportProcessParamExcel">导出Excel</Button>
              <Button
                size="small"
                :disabled="isProcessParamEditReadonly"
                :loading="processParamImporting"
                @click="triggerProcessParamImport"
              >
                导入Excel
              </Button>
              <Button size="small" @click="processParamEditVisible = false">关闭</Button>
              <Button
                size="small"
                type="primary"
                :disabled="isProcessParamEditReadonly"
                :loading="processParamEditSaving"
                @click="requestSaveProcessParamEdit"
              >
                保存
              </Button>
              <Button
                size="small"
                type="primary"
                :disabled="isProcessParamEditReadonly"
                :loading="processParamConfirming"
                @click="requestConfirmProcessParamEdit"
              >
                确认
              </Button>
            </div>
          </div>

          <fieldset class="erp-fieldset process-param-sheet__section">
            <legend>表单信息</legend>
            <div class="process-param-head-grid">
              <label>生产日期</label>
              <DatePicker
                v-model:value="processParamEditForm.reportDate"
                :disabled="isProcessParamEditReadonly"
                value-format="YYYY-MM-DD"
              />
              <label>计划号</label><strong>{{ processParamEditForm.planNo || '-' }}</strong>
              <label>分段批号</label><strong>{{ processParamEditForm.motherBatchNo || '-' }}</strong>
              <label>填写人</label><strong>{{ processParamEditForm.fillUserName || processParamEditForm.recorderName || '-' }}</strong>
              <label>填写时间</label><strong>{{ displayDateTimeText(processParamEditForm.fillTime) }}</strong>
              <label>确认人</label><strong>{{ processParamEditForm.confirmUserName || '-' }}</strong>
              <label>确认时间</label><strong>{{ displayDateTimeText(processParamEditForm.confirmTime) }}</strong>
            </div>
          </fieldset>

          <fieldset v-if="processParamImportAttachment" class="erp-fieldset process-param-sheet__section">
            <legend>原始导入附件</legend>
            <div class="press-slot-middle-attachment__content">
              <button class="press-slot-middle-attachment__link" type="button" @click="openPressSlotImportAttachment(processParamImportAttachment)">
                <IconifyIcon icon="lucide:paperclip" />
                <span>{{ processParamImportAttachment.name || '原始导入文件' }}</span>
              </button>
              <span>导入时间：{{ processParamImportAttachment.uploadTime || '-' }}</span>
              <span v-if="formatPressSlotAttachmentSize(processParamImportAttachment.size)">
                大小：{{ formatPressSlotAttachmentSize(processParamImportAttachment.size) }}
              </span>
            </div>
          </fieldset>

          <div class="process-param-sheet__table">
            <div class="rough-grid-toolbar__title">工艺参数明细</div>
            <ATable
              class="rough-check-table console-record-table process-param-detail-table"
              :columns="processParamDetailColumns"
              :data-source="processParamEditForm.items"
              :locale="{ emptyText: '暂无工艺参数明细' }"
              :pagination="false"
              :scroll="{ x: processParamDetailTableWidth, y: 430 }"
              row-key="seq"
              size="small"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.dataIndex === 'seq'">
                  <strong>{{ record.seq }}</strong>
                </template>
                <template v-else-if="isProcessParamEditReadonly">
                  <span class="process-param-readonly-cell">{{ record[column.dataIndex] || '-' }}</span>
                </template>
                <template v-else-if="column.dataIndex === 'reportDate'">
                  <DatePicker
                    v-model:value="record.reportDate"
                    size="small"
                    value-format="YYYY-MM-DD"
                  />
                </template>
                <template v-else>
                  <Input
                    v-model:value="record[column.dataIndex]"
                    allow-clear
                    size="small"
                  />
                </template>
              </template>
            </ATable>
          </div>
        </div>
      </AModal>

      <AModal
        v-model:open="changeoverVisible"
        :footer="null"
        :title="null"
        width="100vw"
        class="rough-prototype-modal"
        destroy-on-close
        wrap-class-name="hc-pass-work-modal rough-report-work-modal"
        @cancel="changeoverVisible = false"
      >
        <div class="report-modal-body changeover-modal-body">
          <fieldset class="erp-fieldset report-modal-toolbar">
            <legend>压槽首检记录</legend>
            <div class="changeover-scan-row">
              <label>扫码片号</label>
              <Input
                v-model:value="changeoverScanNo"
                allow-clear
                data-changeover-scan
                :disabled="isWorkOrderFinished"
                placeholder="扫描首检批号，也可人工输入后回车"
                @press-enter="scanChangeoverSlice"
              />
              <Button type="primary" :disabled="isWorkOrderFinished" @click="scanChangeoverSlice">带入片号</Button>
            </div>
            <div class="changeover-form-grid">
              <label>计划号</label><strong>{{ changeoverForm.currentPlanNo || currentPlan.planNo || '-' }}</strong>
              <label>当前型号</label><strong>{{ changeoverForm.productionModelCode || currentPlan.modelCode || '-' }}</strong>
              <label>当前料号</label><strong>{{ changeoverForm.productionMaterialCode || currentPlan.materialCode || '-' }}</strong>
              <label>送检时间</label><strong>{{ displayDateTimeText(changeoverForm.submitTime) }}</strong>
              <label>分段批号</label><strong>{{ changeoverForm.motherSegmentBatchNo || '-' }}</strong>
              <label>压槽片号</label><strong>{{ changeoverForm.pressSlotSliceNo || '-' }}</strong>
              <label>检测状态</label>
              <strong>{{ getInspectionStatusText(changeoverForm.inspectionStatus) }}</strong>
              <label>检测结果</label><strong>{{ getInspectionResultText(changeoverForm) }}</strong>
              <label>备注</label><Input v-model:value="changeoverForm.remark" :disabled="isWorkOrderFinished" class="changeover-remark-input" placeholder="请输入备注" />
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
                    class="adhesive-check-cell"
                    :data-report-check-field="'actualValue'"
                    :disabled="isWorkOrderFinished || String(record.itemName || '').includes('压槽辊编号')"
                    placeholder="填写实际值"
                    @keydown="handleReportCheckKeydown($event, index, 'actualValue')"
                  />
                </template>
                <template v-if="column.dataIndex === 'abnormalRemark'">
                  <Input
                    v-model:value="record.abnormalRemark"
                    class="adhesive-check-cell"
                    :data-report-check-field="'abnormalRemark'"
                    :disabled="isWorkOrderFinished"
                    placeholder="填写异常备注"
                    @keydown="handleReportCheckKeydown($event, index, 'abnormalRemark')"
                  />
                </template>
              </template>
            </ATable>
          </div>
        </div>
        <div class="modal-footer report-action-footer">
          <Button @click="changeoverVisible = false">关闭</Button>
          <Button :disabled="!changeoverForm.id" @click="printChangeoverInspection(changeoverForm)">打印送检单</Button>
          <Button type="primary" :disabled="isWorkOrderFinished" @click="submitChangeoverInspection">保存首检记录</Button>
        </div>
      </AModal>

      <AModal v-model:open="reportRecordListVisible" :footer="null" :width="1180" title="今日压槽报工记录">
        <div class="console-table-shell console-table-shell--modal">
          <div class="rough-grid-toolbar console-record-toolbar">
            <div class="rough-grid-toolbar__title">今日压槽报工记录</div>
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
              <Button size="small" :disabled="isWorkOrderFinished || !reportRecords.length" @click="openSelectedRecordConfirm">扫码确认</Button>
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
              :scroll="{ x: 920, y: 420 }"
              row-key="id"
              size="small"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'motherBatchNo'">
                  {{ resolveMotherSegmentBatchNo(record.parentProductionBatchNo || record.sourceBatchNo || record.sourceProductionBatchNo || '') || '-' }}
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

      <AModal v-model:open="dailyRecordListVisible" :footer="null" :width="760" class="rough-prototype-modal" title="今日压槽点检/清洁记录">
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
              <Button size="small" type="primary" :disabled="!canFillDailyRecord(toDailyRecord(record))" @click="openDailyRecord(toDailyRecord(record), 'edit')">填写</Button>
              <Button size="small" danger :disabled="!canConfirmDailyRecord(toDailyRecord(record))" @click="openDailyRecord(toDailyRecord(record), 'confirm')">确认</Button>
              <Button size="small" :disabled="!canViewDailyRecord(toDailyRecord(record))" @click="openDailyRecord(toDailyRecord(record), 'view')">查看</Button>
            </span>
          </div>
          <div v-if="dailyRecordRows.length === 0" class="console-empty compact">暂无点检清洁记录，请检查压槽动态表单配置。</div>
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
                <span>计划：{{ currentPlan.planNo || '-' }}</span>
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

      <AuthModal
        v-model:visible="dailyRecordAuthVisible"
        auth-mode="username"
        :action-name="dailyRecordAuthAction"
        :equipment-id="selectedBoardEquipmentId"
        :workstation="selectedBoardEquipmentCode || selectedBoardEquipmentName || '压槽工位'"
        @cancel="handleDailyRecordAuthCancel"
        @success="handleDailyRecordAuthSuccess"
      />

      <AuthModal
        v-model:visible="segmentCompleteVisible"
        auth-mode="username"
        :action-name="segmentCompleteAuthActionName"
        title="压槽本段完工认证"
        :workstation="currentPlan.equipmentName || currentPlan.equipmentCode || '压槽工位'"
        @cancel="handleSegmentCompleteAuthCancel"
        @success="confirmSegmentComplete"
      />

      <AuthModal
        v-model:visible="intermediateAuthVisible"
        auth-mode="username"
        :action-name="intermediateAuthAction"
        title="压槽中间品记录认证"
        :workstation="currentPlan.equipmentName || currentPlan.equipmentCode || '压槽工位'"
        @cancel="handleIntermediateAuthCancel"
        @success="handleIntermediateAuthSuccess"
      />

      <AuthModal
        v-model:visible="processParamAuthVisible"
        auth-mode="username"
        :action-name="processParamAuthAction"
        title="压槽工艺参数认证"
        :workstation="currentPlan.equipmentName || currentPlan.equipmentCode || '压槽工位'"
        @cancel="handleProcessParamAuthCancel"
        @success="handleProcessParamAuthSuccess"
      />

      <AuthModal
        v-model:visible="firstInspectionProcessAuthVisible"
        auth-mode="username"
        :action-name="firstInspectionProcessAuthAction"
        title="压槽生产点检表认证"
        :workstation="currentPlan.equipmentName || currentPlan.equipmentCode || '压槽工位'"
        @cancel="handleFirstInspectionProcessAuthCancel"
        @success="handleFirstInspectionProcessAuthSuccess"
      />

      <AModal v-model:open="rollerCleanVisible" :footer="null" :width="680" class="rough-prototype-modal" title="压槽辊清洗复位">
        <div class="glue-consume-form">
          <div class="consumable-info-grid">
            <div class="consumable-reason-form">
              <label>压槽批号</label>
              <Input v-model:value="rollerCleanForm.batchNo" allow-clear placeholder="请输入压槽批号" />
            </div>
            <div class="consumable-reason-form">
              <label>上次清洗时间</label>
              <DatePicker
                v-model:value="rollerCleanForm.cleanTime"
                class="consumable-number-input"
                format="YYYY-MM-DD HH:mm:ss"
                show-time
                value-format="YYYY-MM-DD HH:mm:ss"
              />
            </div>
            <div class="consumable-reason-form">
              <label>初始化片数</label>
              <InputNumber v-model:value="rollerCleanForm.initialUseCount" :min="0" :precision="0" class="consumable-number-input" />
            </div>
          </div>
          <div class="consumable-reason-form">
            <label>清洗原因</label>
            <Input.TextArea v-model:value="rollerCleanForm.cleanRemark" :rows="5" placeholder="请输入清洗原因" />
          </div>
          <div class="modal-footer">
            <Button @click="rollerCleanVisible = false">取消</Button>
            <Button type="primary" @click="confirmRollerClean">确认清洗复位</Button>
          </div>
        </div>
      </AModal>

      <AModal v-model:open="glueConsumeVisible" :footer="null" :width="680" class="rough-prototype-modal" title="轴承更换登记">
        <div class="glue-consume-form">
          <div class="consumable-info-grid">
            <div class="consumable-reason-form">
              <label>轴承批号</label>
              <Input v-model:value="glueConsumeForm.batchNo" allow-clear placeholder="请输入轴承批号" />
            </div>
            <div class="consumable-reason-form">
              <label>上次更换时间</label>
              <DatePicker
                v-model:value="glueConsumeForm.replaceTime"
                class="consumable-number-input"
                format="YYYY-MM-DD HH:mm:ss"
                show-time
                value-format="YYYY-MM-DD HH:mm:ss"
              />
            </div>
            <div class="consumable-reason-form">
              <label>初始化片数</label>
              <InputNumber v-model:value="glueConsumeForm.initialUseCount" :min="0" :precision="0" class="consumable-number-input" />
            </div>
          </div>
          <div class="consumable-reason-form">
            <label>更换原因</label>
            <Input.TextArea v-model:value="glueConsumeForm.replaceReason" :rows="5" placeholder="请输入更换原因" />
          </div>
          <div v-if="activeConsumable.alarm" class="record-scan-confirm__error">{{ activeConsumable.alarm }}</div>
          <div class="modal-footer">
            <Button @click="glueConsumeVisible = false">取消</Button>
            <Button type="primary" @click="confirmGlueConsume">确认更换</Button>
          </div>
        </div>
      </AModal>

      <AuthModal
        v-model:visible="consumableAuthVisible"
        auth-mode="username"
        :action-name="consumableAuthAction"
        :workstation="currentPlan.equipmentName || currentPlan.equipmentCode || '压槽工位'"
        @success="handleConsumableAuthSuccess"
      />

      <AModal
        v-model:open="firstInspectionScanVisible"
        :footer="null"
        :keyboard="false"
        :mask-closable="false"
        :width="860"
        class="rough-prototype-modal"
        destroy-on-close
        :title="firstInspectionScanDialogTitle"
      >
        <div class="record-confirm-panel">
          <div class="record-confirm-summary">
            <span>当前计划</span><strong>{{ currentPlan.planNo || '-' }}</strong>
            <span>分段批号</span><strong>{{ currentMotherSegmentBatchNo || '-' }}</strong>
            <span>{{ firstInspectionScanSummaryLabel }}</span><strong>{{ firstInspectionScanSummaryText }}</strong>
          </div>
          <div class="record-confirm-basis">
            {{ firstInspectionScanDialogBasis }}
          </div>
          <div class="record-confirm-scan">
            <IconifyIcon icon="lucide:scan-line" />
            <Input
              ref="firstInspectionScanInputRef"
              v-model:value="firstInspectionScanNo"
              allow-clear
              data-first-inspection-confirm-scan
              :placeholder="firstInspectionScanPlaceholder"
              @press-enter="confirmFirstInspectionScanAndSubmit"
            />
          </div>
          <div class="first-inspection-dialog-meta">
            <Tag :color="firstInspectionSourceStatusMeta.color">{{ firstInspectionSourceStatusMeta.text }}</Tag>
            <span v-if="currentFirstInspectionSliceNo">{{ firstInspectionScanCurrentLabel }}：{{ currentFirstInspectionSliceNo }}</span>
            <span>检验日期：{{ firstInspection.inspectionDate || currentDateText }}</span>
          </div>
          <div v-if="firstInspectionScanError" class="record-scan-confirm__error">{{ firstInspectionScanError }}</div>
          <div class="modal-footer">
            <Button @click="firstInspectionScanVisible = false">取消</Button>
            <Button
              type="primary"
              :loading="firstInspectionScanLoading || firstInspectionApplying || processCheckApplying"
              @click="confirmFirstInspectionScanAndSubmit"
            >
              {{ firstInspectionScanSubmitText }}
            </Button>
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
        title="压槽片扫码确认"
      >
        <div class="record-confirm-panel">
          <div class="record-confirm-summary">
            <span>当前计划</span><strong>{{ currentPlan.planNo || '-' }}</strong>
            <span>分段批号</span><strong>{{ currentMotherSegmentBatchNo || '-' }}</strong>
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
              data-adhesive-confirm-scan
              placeholder="请扫描流转单上的压槽片号，也可人工输入"
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
          <div v-if="activeReportReadonlyReason" class="report-readonly-reason">
            {{ activeReportReadonlyReason }}
          </div>
          <fieldset class="erp-fieldset report-modal-toolbar">
            <legend>{{ reportDialogMode === 'view' ? '压槽片详情查看' : '压槽扫码确认' }}</legend>
            <div class="press-slot-report-head">
              <div v-if="reportDialogMode === 'view'" class="press-slot-report-head__item press-slot-report-head__item--type">
                <label>作业类型</label>
                <div class="press-slot-report-head__value">
                  <strong class="press-slot-report-type-text">
                    {{ getPressSlotReportTypeText(reportForm.reportType) }}
                  </strong>
                </div>
              </div>
              <div class="press-slot-report-head__item">
                <label>分段批号</label>
                <strong class="press-slot-report-head__value press-slot-report-head__value--batch">{{ reportForm.parentBatchNo || resolveMotherSegmentBatchNo(reportForm.sourceProductionBatchNo) || '-' }}</strong>
              </div>
              <div class="press-slot-report-head__item">
                <label>压槽片号</label>
                <strong class="press-slot-report-head__value press-slot-report-head__value--batch">{{ reportForm.productionBatchNo || '-' }}</strong>
              </div>
              <div class="press-slot-report-head__item">
                <label>COA送检</label>
                <strong class="press-slot-report-head__value" :class="{ warning: reportForm.coaFlag }">{{ reportForm.coaFlag ? '已选为COA送检片' : '未送检' }}</strong>
              </div>
              <div v-if="shouldShowVisualInspectionTab" class="press-slot-report-head__item" :class="{ 'press-slot-report-head__item--visual-warning': currentReportVisualIssue }">
                <label>外观检查</label>
                <strong class="press-slot-report-head__value" :class="{ warning: currentReportVisualIssue }">
                  {{ currentReportVisualIssueText }}
                </strong>
              </div>
              <div v-if="isPressSlotInspectionReportType(reportForm.reportType)" class="press-slot-report-head__item press-slot-report-head__item--full">
                <label>检验信息</label>
                <strong class="press-slot-report-head__value">
                  送检时间：{{ displayDateTimeText(reportInspectionInfo.submitTime) }} / 反馈结果：{{ reportInspectionInfo.feedbackResult }} / 反馈时间：{{ displayDateTimeText(reportInspectionInfo.feedbackTime) }}
                </strong>
              </div>
            </div>
          </fieldset>
          <Tabs v-model:active-key="activeReportTab" class="rough-report-tabs">
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
                        class="adhesive-check-cell"
                        :data-report-check-field="'actualValue'"
                        :disabled="reportDialogMode === 'view'"
                        placeholder="填写实际值"
                        @keydown="handleReportCheckKeydown($event, getCheckItemsByCategory(category).findIndex((item) => item.itemName === record.itemName), 'actualValue')"
                      />
                    </template>
                    <template v-if="column.dataIndex === 'abnormalRemark'">
                      <Input
                        v-model:value="record.abnormalRemark"
                        class="adhesive-check-cell"
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
            <TabPane v-if="shouldShowVisualInspectionTab" key="visual-inspection" tab="外观检验">
              <div class="report-tab-stack press-slot-visual-tab">
                <div class="visual-check-grid">
                  <div v-for="item in visualInspectionItems" :key="item.itemName" :class="getVisualCheckItemClass(item)">
                    <button class="visual-light-button" type="button" :disabled="!isVisualInspectionEditable" @click="toggleVisualItem(item)">
                      <span class="visual-light-dot" :class="{ 'is-active': isVisualItemActive(item) }"></span>
                      <span>{{ item.itemName }}</span>
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
                  <div v-if="currentReportVisualIssue" class="press-slot-visual-field">
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
          </Tabs>
        </div>
        <div class="modal-footer report-action-footer">
          <Button
            v-if="reportDialogMode === 'view' && activeRecord?.id && !isWorkOrderFinished"
            :loading="middleTypeSetting"
            @click="setActiveReportMiddleType('FRONT')"
          >
            设为中间品-前段
          </Button>
          <Button
            v-if="reportDialogMode === 'view' && activeRecord?.id && !isWorkOrderFinished"
            :loading="middleTypeSetting"
            @click="setActiveReportMiddleType('MIDDLE')"
          >
            设为中间品-中段
          </Button>
          <Button
            v-if="reportDialogMode === 'view' && activeRecord?.id && !isWorkOrderFinished"
            :loading="middleTypeSetting"
            @click="setActiveReportMiddleType('END')"
          >
            设为中间品-后段
          </Button>
          <Button
            v-if="canCorrectAbnormalCategory"
            :loading="abnormalCategoryCorrectionSaving"
            @click="openAbnormalCategoryCorrection"
          >
            修正异常类别
          </Button>
          <Button @click="requestCloseReport">{{ reportDialogMode === 'view' ? '关闭' : '取消' }}</Button>
          <Button
            v-if="reportDialogMode === 'confirm'"
            :disabled="reportSourceScanning || reportSubmitting"
            :loading="processCheckApplying || reportSubmitting"
            :type="reportForm.reportType === 'PROCESS_CHECK' ? 'primary' : 'default'"
            @click="saveAndPushProcessCheck"
          >
            保存并推送过程加检
          </Button>
          <Button
            v-if="reportDialogMode === 'confirm' && reportForm.reportType !== 'PROCESS_CHECK'"
            :disabled="reportSourceScanning || reportSubmitting"
            :loading="reportSourceScanning || reportSubmitting"
            type="primary"
            @click="submitProcessItems"
          >
            {{ reportSubmitButtonText }}
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
        title="修正压槽外观异常类别"
        width="560px"
        @cancel="closeAbnormalCategoryCorrection"
        @ok="submitAbnormalCategoryCorrection"
      >
        <Form layout="vertical">
          <FormItem label="压槽片号">
            <Input :value="activeRecord?.productionBatchNo || activeRecord?.sourceProductionBatchNo || '-'" disabled />
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
  flex: 0 0 clamp(270px, 18vw, 330px);
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
  width: clamp(132px, 8.4vw, 162px);
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
  max-width: 118px;
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

.changeover-modal-body {
  gap: 10px;
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
  font-weight: 900;
  background: #dce5ee;
  border: 1px solid #94a3b8;
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
  color: #0f172a;
  font-family: Consolas, 'Microsoft YaHei', monospace;
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap;
  background: #f8fbff;
  border: 1px solid #b6c3d1;
}

.changeover-status-radio {
  display: flex;
  align-items: center;
  min-width: 0;
  height: 32px;
  padding: 0 10px;
  overflow: hidden;
  background: #f8fbff;
  border: 1px solid #b6c3d1;
}

.changeover-status-radio :deep(.ant-radio-wrapper) {
  margin-inline-end: 12px;
  font-weight: 800;
}

.changeover-form-grid :deep(.ant-input) {
  height: 32px;
  border-radius: 0;
}

.process-param-edit-grid {
  grid-template-columns: 110px minmax(0, 1fr) 110px minmax(0, 1fr) 110px minmax(0, 1fr);
}

.process-param-edit-grid :deep(.ant-picker) {
  width: 100%;
  height: 32px;
  border-radius: 0;
}

.process-param-edit-grid .changeover-remark-input {
  grid-column: span 5;
}

.first-inspection-process-form {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  gap: 10px;
}

.first-inspection-process-form :deep(.first-inspection-process-runtime) {
  flex: 1 1 0;
  min-width: 0;
  min-height: 0;
  height: auto;
}

.first-inspection-process-form > .modal-footer {
  flex: 0 0 auto;
}

.first-inspection-process-action-bar {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 12px;
  justify-content: space-between;
  min-height: 44px;
}

.first-inspection-process-action-bar__title {
  flex: 1 1 auto;
  min-width: 0;
  overflow: hidden;
  color: #111827;
  font-size: 18px;
  font-weight: 800;
  line-height: 32px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.first-inspection-process-action-bar__actions {
  display: flex;
  flex: 0 0 auto;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  justify-content: flex-end;
  margin-left: auto;
}

.first-inspection-process-header-grid {
  grid-template-columns: 90px minmax(0, 1fr) 90px minmax(0, 1fr) 90px minmax(0, 1fr) 90px minmax(0, 1fr);
}

.first-inspection-process-header-grid :deep(.ant-picker) {
  width: 100%;
  height: 32px;
  border-radius: 0;
}

.first-inspection-process-value {
  display: flex;
  align-items: center;
  min-width: 0;
  height: 32px;
  padding: 0 10px;
  overflow: hidden;
  background: #f8fbff;
  border: 1px solid #b6c3d1;
}

.first-inspection-process-table-wrap {
  display: flex;
  flex: 1 1 0;
  min-height: 0;
  overflow: hidden;
  background: #f8fafc;
  border: 1px solid #8794a4;
}

.first-inspection-production-check-table {
  flex: 1 1 0;
  min-height: 0;
}

.first-inspection-production-check-table :deep(.ant-spin-nested-loading),
.first-inspection-production-check-table :deep(.ant-spin-container),
.first-inspection-production-check-table :deep(.ant-table) {
  height: 100%;
  min-height: 0;
}

.first-inspection-production-check-table :deep(.ant-table) {
  display: flex;
  flex-direction: column;
}

.first-inspection-production-check-table :deep(.ant-table-container) {
  display: flex;
  flex: 1 1 0;
  flex-direction: column;
  min-height: 0;
}

.first-inspection-production-check-table :deep(.ant-table-body) {
  flex: 1 1 auto;
  min-height: 0;
  overflow: auto !important;
}

.first-inspection-production-check-table :deep(.ant-table-cell) {
  white-space: normal;
}

.first-inspection-production-check-static-cell {
  display: inline-flex;
  align-items: center;
  min-height: 26px;
  color: #111827;
  font-weight: 700;
  white-space: pre-wrap;
}

:global(.first-inspection-process-modal-wrap .ant-modal) {
  top: 0;
  max-width: 100vw;
  height: 100vh;
  padding-bottom: 0;
  margin: 0;
}

:global(.first-inspection-process-modal-wrap .ant-modal-content) {
  display: flex;
  flex-direction: column;
  height: 100vh;
  border-radius: 0;
}

:global(.first-inspection-process-modal-wrap .ant-modal-body) {
  flex: 1;
  min-height: 0;
}

.first-inspection-process-confirm-info {
  display: flex;
  flex-wrap: wrap;
  flex: 1;
  align-items: center;
  gap: 12px;
  min-width: 0;
  color: #334155;
  font-size: 12px;
  font-weight: 800;
}

.first-inspection-process-info-item {
  display: inline-flex;
  align-items: center;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.first-inspection-process-info-item.editable {
  flex: 0 0 190px;
}

.first-inspection-process-info-item.editable span {
  flex: 0 0 auto;
}

.first-inspection-process-info-item :deep(.ant-input) {
  height: 28px;
  min-width: 0;
  border-radius: 2px;
}

.changeover-remark-input {
  grid-column: span 7;
}

.changeover-check-panel {
  flex: 1;
  min-height: 0;
  overflow: hidden;
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
  grid-template-columns: 7fr 18fr 7fr 18fr 7fr 18fr 7fr 18fr;
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

.tab-maximize-button {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  height: 26px;
  margin-top: 5px;
  color: #075985;
  font-weight: 900;
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

.panel-metrics {
  display: inline-flex;
  flex: 1 1 auto;
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

.press-slot-card-legend i.is-first-ok {
  background: #cffafe;
  border-color: #0891b2;
  box-shadow: inset 0 0 0 2px rgb(8 145 178 / 26%);
}

.press-slot-card-legend i.is-release-ok {
  background: #ede9fe;
  border-color: #7c3aed;
  box-shadow: inset 0 0 0 2px rgb(124 58 237 / 24%);
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

.press-slot-card-legend i.is-abnormal-lock {
  background: #fee2e2;
  border-color: #dc2626;
  box-shadow: inset 0 0 0 2px rgb(220 38 38 / 45%);
}

.press-slot-card-legend i.is-middle {
  background: #eef2ff;
  border-color: #4f46e5;
  box-shadow: inset 3px 0 0 #4f46e5;
  border-left-width: 3px;
}

.press-slot-abnormal-alert {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 8px 10px 0;
  padding: 8px 10px;
  color: #7f1d1d;
  font-size: 13px;
  font-weight: 800;
  background: #fee2e2;
  border: 1px solid #ef4444;
  box-shadow: inset 4px 0 0 #dc2626;
}

.press-slot-abnormal-alert span {
  flex: 1;
  min-width: 0;
}

.press-slot-abnormal-alert :deep(.iconify),
.press-slot-abnormal-alert :deep(svg) {
  flex: 0 0 auto;
  color: #b91c1c;
  font-size: 18px;
}

.press-slot-result-link {
  padding: 0;
  margin: 0;
  line-height: 1;
  cursor: pointer;
  background: transparent;
  border: 0;
}

.press-slot-result-link :deep(.ant-tag) {
  margin-right: 0;
}

.press-slot-result-link:hover :deep(.ant-tag) {
  text-decoration: underline;
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

.panel-filter-bar {
  display: flex;
  flex: 0 1 540px;
  align-items: center;
  gap: 6px;
  width: auto;
  min-width: 0;
  max-width: 540px;
  margin-left: 10px;
}

.panel-filter-bar :deep(.ant-input-affix-wrapper) {
  flex: 0 1 145px;
  min-width: 0;
  height: 26px;
  border-radius: 999px;
}

.panel-filter-select {
  flex: 0 0 108px;
  min-width: 0;
}

.panel-filter-select--type {
  flex-basis: 150px;
}

.panel-filter-refresh {
  flex: 0 0 auto;
  height: 26px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 900;
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
  flex: 0 0 270px;
  align-items: center;
  gap: 4px;
  width: 270px;
  min-width: 270px;
  overflow: hidden;
}

.status-pill-group--type {
  flex: 1 1 360px;
  width: auto;
  min-width: 0;
  overflow-x: auto;
  scrollbar-width: thin;
}

.status-pill {
  flex: 0 0 62px;
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

.status-pill-group--type .status-pill {
  flex-basis: 76px;
  font-size: 11px;
}

.status-pill.is-active {
  color: #fff;
  background: linear-gradient(180deg, #0ea5e9 0%, #0369a1 100%);
  border-color: #075985;
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

.cloth-source-header .press-slot-card-legend {
  gap: 7px;
  margin-left: auto;
  padding-left: 10px;
}

.cloth-source-header .press-slot-card-legend span {
  gap: 3px;
  color: #334155;
  font-size: 11px;
}

.source-actions {
  display: inline-flex;
  align-items: center;
  justify-content: flex-end;
  gap: 6px;
}

.source-selection-count {
  color: #1d4ed8;
  font-size: 12px;
  font-weight: 800;
  white-space: nowrap;
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
  display: block;
  width: 100%;
  min-width: 0;
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

.slice-cell__line.is-first-ok {
  color: #164e63;
  background: rgb(207 250 254 / 86%);
  border: 1px solid rgb(8 145 178 / 58%);
  border-left: 4px solid #0891b2;
}

.slice-cell__line.is-first-ok b,
.slice-cell__line.is-first-ok em {
  color: #164e63;
}

.slice-cell__line.is-release-ok {
  color: #4c1d95;
  background: rgb(237 233 254 / 88%);
  border: 1px solid rgb(124 58 237 / 58%);
  border-left: 4px solid #7c3aed;
}

.slice-cell__line.is-release-ok b,
.slice-cell__line.is-release-ok em {
  color: #4c1d95;
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

.slice-cell__line.is-abnormal-lock {
  color: #7f1d1d;
  background: #fee2e2;
  border: 1px solid rgb(220 38 38 / 70%);
  border-left: 4px solid #dc2626;
}

.slice-cell__line.is-abnormal-lock b,
.slice-cell__line.is-abnormal-lock em {
  color: #7f1d1d;
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

.slice-cell__type {
  display: block;
  width: fit-content;
  min-width: 0;
  margin: 0 auto;
  padding: 0 4px;
  color: #0f172a;
  font-size: 10px;
  font-weight: 900;
  line-height: 14px;
  background: rgb(255 255 255 / 36%);
  border: 0;
  border-left: 3px solid currentColor;
  border-radius: 1px;
}

.slice-cell.is-coa {
  border-color: #dc2626;
  box-shadow:
    inset 0 0 0 2px rgb(220 38 38 / 58%),
    inset 0 0 0 5px rgb(254 226 226 / 72%),
    0 0 0 2px rgb(220 38 38 / 25%),
    0 8px 16px rgb(127 29 29 / 24%);
}

.slice-cell__coa {
  position: absolute;
  top: 4px;
  left: 4px;
  z-index: 3;
  display: inline-flex;
  min-width: 38px;
  height: 20px;
  align-items: center;
  justify-content: center;
  padding: 0 6px;
  color: #fff;
  font-family: Arial, 'Microsoft YaHei', sans-serif;
  font-size: 12px;
  font-style: normal;
  font-weight: 950;
  letter-spacing: 0.03em;
  line-height: 20px;
  text-shadow: none;
  background: linear-gradient(135deg, #dc2626 0%, #991b1b 100%);
  border: 1px solid rgb(255 255 255 / 86%);
  border-radius: 1px;
  box-shadow:
    0 2px 8px rgb(127 29 29 / 38%),
    0 0 0 2px rgb(254 226 226 / 70%);
}

.slice-cell__fai {
  position: absolute;
  right: 7px;
  bottom: 6px;
  z-index: 3;
  padding: 1px 5px;
  color: #fff;
  font-size: 11px;
  font-style: normal;
  font-weight: 900;
  line-height: 16px;
  background: #be123c;
  border-radius: 2px;
  box-shadow: 0 1px 4px rgb(127 29 29 / 24%);
}

.slice-cell__fai.is-fai-ok {
  background: #15803d;
  box-shadow: 0 1px 4px rgb(21 128 61 / 24%);
}

.slice-cell__fai.is-fai-ng {
  background: #b91c1c;
  box-shadow: 0 1px 4px rgb(185 28 28 / 28%);
}

.slice-cell__fai.is-fai-pending {
  background: #d97706;
  box-shadow: 0 1px 4px rgb(217 119 6 / 24%);
}

.slice-cell__ng {
  position: absolute;
  top: 5px;
  right: 5px;
  z-index: 4;
  padding: 2px 6px;
  color: #fff;
  font-size: 11px;
  font-style: normal;
  font-weight: 950;
  line-height: 16px;
  background: #b91c1c;
  border: 1px solid rgb(255 255 255 / 74%);
  border-radius: 2px;
  box-shadow: 0 2px 8px rgb(127 29 29 / 34%);
}

.slice-cell__slitting-ng {
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
  background: #c2410c;
  border: 1px solid rgb(255 255 255 / 74%);
  border-radius: 1px;
  box-shadow: none;
}

.slice-cell__coa,
.slice-cell__fai,
.slice-cell__ng,
.slice-cell__slitting-ng {
  position: static;
  display: inline-flex;
  max-width: 100%;
  min-height: 0;
  align-items: center;
  justify-content: center;
  padding: 1px 4px;
  font-size: 10px;
  line-height: 13px;
  overflow-wrap: anywhere;
  text-align: center;
  white-space: normal;
  border-radius: 1px;
  box-shadow: none;
}

.slice-cell__coa {
  height: auto;
  min-width: 34px;
  font-size: 10px;
  line-height: 13px;
}

.slice-cell__fai {
  right: auto;
  bottom: auto;
  z-index: 4;
}

.slice-cell__ng {
  top: auto;
  right: auto;
  z-index: 4;
  line-height: 13px;
}

.report-toolbar-radio {
  align-items: center;
  min-width: 0;
  overflow: hidden;
}

.report-toolbar-radio :deep(.ant-radio-wrapper) {
  margin-inline-end: 8px;
  font-weight: 800;
  white-space: nowrap;
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
  box-shadow:
    inset 0 0 0 2px rgb(249 115 22 / 24%),
    inset 0 -12px 20px rgb(154 75 16 / 12%);
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

.slice-cell.is-one-click-selectable {
  cursor: pointer;
  padding-top: 8px;
}

.slice-cell.is-one-click-disabled {
  cursor: not-allowed;
  opacity: 0.58;
}

.slice-cell.is-one-click-selected {
  border-color: #047857;
  box-shadow:
    inset 0 0 0 3px rgb(5 150 105 / 42%),
    0 0 0 2px rgb(255 255 255 / 88%),
    0 0 16px rgb(5 150 105 / 28%);
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

.slice-cell.is-current-ng,
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

.slice-cell.is-other-ng,
.slice-cell.is-slitting-ng {
  color: #7f1d1d;
  background:
    linear-gradient(135deg, rgb(255 255 255 / 38%) 0%, rgb(255 255 255 / 10%) 34%, transparent 35%),
    repeating-linear-gradient(45deg, rgb(255 255 255 / 16%) 0 1px, transparent 1px 7px),
    repeating-linear-gradient(-45deg, rgb(153 27 27 / 12%) 0 1px, transparent 1px 7px),
    #fecaca;
  border-color: rgb(185 28 28 / 78%);
  box-shadow:
    inset 0 0 0 3px rgb(220 38 38 / 38%),
    0 0 14px rgb(220 38 38 / 22%);
}

.slice-cell.is-current-ng.is-slitting-ng {
  color: #7f1d1d;
  background:
    linear-gradient(135deg, rgb(255 255 255 / 38%) 0%, rgb(255 255 255 / 10%) 34%, transparent 35%),
    repeating-linear-gradient(45deg, rgb(255 255 255 / 16%) 0 1px, transparent 1px 7px),
    #fecaca;
  border-color: rgb(185 28 28 / 76%);
  box-shadow:
    inset 0 0 0 3px rgb(220 38 38 / 42%),
    0 0 14px rgb(220 38 38 / 22%);
}

.slice-cell.is-first-inspection {
  border-color: #be123c;
  box-shadow:
    inset 0 0 0 3px rgb(190 18 60 / 36%),
    0 0 14px rgb(190 18 60 / 22%);
}

.slice-cell.is-first-inspection.is-fai-ok {
  border-color: #15803d;
  box-shadow:
    inset 0 0 0 3px rgb(21 128 61 / 32%),
    0 0 14px rgb(21 128 61 / 20%);
}

.slice-cell.is-first-inspection.is-fai-ng {
  border-color: #b91c1c;
  box-shadow:
    inset 0 0 0 3px rgb(185 28 28 / 36%),
    0 0 14px rgb(185 28 28 / 22%);
}

.slice-cell.is-first-inspection.is-fai-pending {
  border-color: #d97706;
  box-shadow:
    inset 0 0 0 3px rgb(217 119 6 / 30%),
    0 0 14px rgb(217 119 6 / 18%);
}

.slice-cell.is-current-ng,
.slice-cell.is-current-ng.is-fai-pending,
.slice-cell.is-other-ng,
.slice-cell.is-other-ng.is-fai-pending,
.slice-cell.is-slitting-ng,
.slice-cell.is-slitting-ng.is-fai-pending {
  color: #7f1d1d;
  background:
    linear-gradient(135deg, rgb(255 255 255 / 38%) 0%, rgb(255 255 255 / 10%) 34%, transparent 35%),
    repeating-linear-gradient(45deg, rgb(255 255 255 / 16%) 0 1px, transparent 1px 7px),
    repeating-linear-gradient(-45deg, rgb(153 27 27 / 12%) 0 1px, transparent 1px 7px),
    #fecaca;
  border-color: rgb(185 28 28 / 78%);
  box-shadow:
    inset 0 0 0 3px rgb(220 38 38 / 40%),
    0 0 14px rgb(220 38 38 / 22%);
}

.slice-cell.is-abnormal-locked {
  color: #7f1d1d;
  background:
    linear-gradient(135deg, rgb(255 255 255 / 34%) 0%, rgb(255 255 255 / 10%) 34%, transparent 35%),
    repeating-linear-gradient(45deg, rgb(220 38 38 / 10%) 0 1px, transparent 1px 7px),
    #fee2e2;
  border-color: #dc2626;
  box-shadow:
    inset 0 0 0 4px rgb(220 38 38 / 45%),
    0 10px 16px rgb(127 29 29 / 18%);
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

.slice-cell.is-inspection-bg-first-ok {
  color: #164e63;
  background:
    linear-gradient(135deg, rgb(255 255 255 / 46%) 0%, rgb(255 255 255 / 10%) 36%, transparent 37%),
    repeating-linear-gradient(45deg, rgb(255 255 255 / 18%) 0 1px, transparent 1px 7px),
    repeating-linear-gradient(-45deg, rgb(8 145 178 / 10%) 0 1px, transparent 1px 7px),
    #cffafe;
  border-color: rgb(8 145 178 / 78%);
  box-shadow:
    inset 0 0 0 3px rgb(8 145 178 / 30%),
    0 0 14px rgb(6 182 212 / 20%);
}

.slice-cell.is-inspection-bg-release-ok {
  color: #4c1d95;
  background:
    linear-gradient(135deg, rgb(255 255 255 / 46%) 0%, rgb(255 255 255 / 10%) 36%, transparent 37%),
    repeating-linear-gradient(45deg, rgb(255 255 255 / 18%) 0 1px, transparent 1px 7px),
    repeating-linear-gradient(-45deg, rgb(124 58 237 / 10%) 0 1px, transparent 1px 7px),
    #ede9fe;
  border-color: rgb(124 58 237 / 78%);
  box-shadow:
    inset 0 0 0 3px rgb(124 58 237 / 30%),
    0 0 14px rgb(124 58 237 / 18%);
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

.slice-cell.is-abnormal-locked {
  border-color: #dc2626;
  border-width: 4px;
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

.console-table-count {
  color: #475569;
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
}

.process-param-filter {
  flex: 0 0 auto;
}

.process-param-filter--date {
  width: 126px;
}

.process-param-filter--slice {
  width: 168px;
}

.first-inspection-toolbar-status {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  justify-content: flex-end;
  margin-left: auto;
}

.first-inspection-toolbar-status span {
  color: #334155;
  font-size: 12px;
  font-weight: 900;
}

.first-inspection-toolbar-status strong {
  color: #be123c;
  font-size: 12px;
  font-weight: 900;
  white-space: nowrap;
}

.first-inspection-slice-mark {
  color: #be123c;
  font-size: 12px;
  font-weight: 900;
  white-space: nowrap;
}

.first-inspection-scan-error {
  color: #dc2626;
  font-size: 12px;
  font-weight: 800;
}

.first-inspection-dialog-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  margin-top: 10px;
  color: #334155;
  font-size: 12px;
  font-weight: 850;
}

.console-table-body {
  flex: 1 1 0;
  width: 100%;
  max-width: 100%;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
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
  grid-template-rows: max-content max-content max-content minmax(320px, 1fr);
  gap: 8px;
  height: auto;
  min-height: 0;
  padding: 8px;
  overflow: hidden;
}

.press-slot-middle-body--no-depth {
  grid-template-rows: max-content max-content minmax(320px, 1fr);
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

.press-slot-depth-cell--head {
  text-align: center;
}

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
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 6px;
  align-items: center;
  padding: 8px;
}

.press-slot-middle-attachment {
  min-height: 42px;
  padding: 8px 10px;
  color: #475569;
  font-size: 12px;
  background: #fff;
  border: 1px solid #d8e0ea;
}

.press-slot-middle-attachment__title {
  color: #1677ff;
  font-size: 14px;
  font-weight: 800;
}

.press-slot-middle-attachment__content {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 14px;
  align-items: center;
  margin-top: 6px;
}

.press-slot-middle-attachment__link {
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

.press-slot-middle-attachment__empty {
  display: inline-flex;
  margin-top: 6px;
  color: #64748b;
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

.consumable-reason-form {
  display: grid;
  grid-template-columns: 88px minmax(0, 1fr);
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
  grid-template-columns: minmax(320px, 1.15fr) minmax(240px, 0.85fr) minmax(180px, 0.7fr) minmax(190px, 0.7fr);
  gap: 8px;
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

.press-slot-report-head__item--visual-warning {
  border-color: #dc2626;
  box-shadow:
    inset 0 0 0 2px rgb(220 38 38 / 22%),
    0 0 12px rgb(239 68 68 / 18%);
}

.press-slot-report-head__item--visual-warning label {
  color: #7f1d1d;
  background: linear-gradient(180deg, #fecaca 0%, #fca5a5 100%);
  border-right-color: #ef4444;
}

.press-slot-report-head__value--batch {
  color: #075985;
}

.press-slot-report-head__radio {
  font-family: 'Microsoft YaHei', sans-serif;
  font-size: 13px;
  white-space: nowrap;
}

.press-slot-report-head__radio :deep(.ant-radio-group) {
  display: flex;
  flex-wrap: nowrap;
  align-items: center;
  gap: 10px;
}

.press-slot-report-head__radio :deep(.ant-radio-wrapper) {
  margin-inline-end: 12px;
  font-weight: 800;
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

.record-confirm-basis {
  padding: 7px 10px;
  color: #075985;
  font-size: 13px;
  font-weight: 900;
  background: #eff6ff;
  border: 1px solid #bfdbfe;
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

.process-param-sheet {
  display: flex;
  flex-direction: column;
  gap: 10px;
  height: 100vh;
  min-height: 0;
  padding: 10px 12px;
  overflow: hidden;
  background: #f5f7fa;
}

.process-param-sheet__toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 14px;
  align-items: center;
  min-height: 44px;
  padding: 6px 8px;
}

.process-param-sheet__title {
  color: #172033;
  font-size: 18px;
  font-weight: 800;
}

.process-param-sheet__meta,
.process-param-sheet__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 12px;
  align-items: center;
}

.process-param-sheet__meta {
  color: #334155;
}

.process-param-sheet__actions {
  margin-left: auto;
}

.process-param-sheet__section {
  flex-shrink: 0;
  margin: 0;
}

.process-param-head-grid {
  display: grid;
  grid-template-columns: 96px minmax(0, 1fr) 96px minmax(0, 1fr) 96px minmax(0, 1fr);
  gap: 8px 10px;
  align-items: center;
}

.process-param-head-grid label {
  justify-self: end;
  color: #334155;
  font-weight: 700;
}

.process-param-head-grid strong {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.process-param-sheet__table {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
  background: #fff;
  border: 1px solid #d8e0ea;
}

.process-param-sheet__table .rough-grid-toolbar__title {
  flex-shrink: 0;
  padding: 8px 10px;
  background: #eef2f7;
  border-bottom: 1px solid #d8e0ea;
}

.process-param-sheet__table :deep(.process-param-detail-table) {
  display: flex;
  flex: 1 1 0;
  flex-direction: column;
  min-height: 0;
}

.process-param-sheet__table :deep(.ant-spin-nested-loading),
.process-param-sheet__table :deep(.ant-spin-container),
.process-param-sheet__table :deep(.ant-table) {
  display: flex;
  flex: 1 1 0;
  flex-direction: column;
  min-height: 0;
}

.process-param-sheet__table :deep(.ant-table-container) {
  display: flex;
  flex: 1 1 0;
  flex-direction: column;
  min-height: 0;
}

.process-param-sheet__table :deep(.ant-table-header) {
  display: block !important;
  flex: 0 0 auto;
  min-height: 42px;
  overflow: hidden !important;
}

.process-param-sheet__table :deep(.ant-table-thead) {
  display: table-header-group !important;
}

.process-param-sheet__table :deep(.ant-table-thead > tr) {
  display: table-row !important;
}

.process-param-sheet__table :deep(.ant-table-thead > tr > th) {
  display: table-cell !important;
  height: 42px;
  padding: 8px 10px !important;
}

.process-param-sheet__table :deep(.ant-table-body) {
  display: block;
  flex: 1 1 auto;
  min-height: 260px;
  overflow: auto !important;
}

.process-param-sheet__table :deep(.ant-table-placeholder) {
  display: table-row !important;
}

.process-param-readonly-cell {
  display: inline-flex;
  align-items: center;
  min-height: 26px;
  color: #111827;
  font-weight: 700;
  white-space: pre-wrap;
}

.abnormal-category-correction-options {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
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
